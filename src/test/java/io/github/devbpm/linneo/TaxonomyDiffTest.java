/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.devbpm.linneo;

import io.github.devbpm.linneo.Protection.Direction;
import io.github.devbpm.linneo.TaxonomyDiff.Bump;
import io.github.devbpm.linneo.TermChange.Aspect;
import io.github.devbpm.linneo.TermChange.Hierarchy;
import io.github.devbpm.linneo.extension.finance.FinanceTaxonomy;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesTaxonomy;
import io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsTaxonomy;
import io.github.devbpm.linneo.extension.supplychain.SupplyChainTaxonomy;
import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxonomyDiffTest {

    private static TaxonomyBuilder root() {
        return Taxonomy.builder().category("data", "Data", "Data.",
                c -> c.nature(LegalNature.ORDINARY).confidentiality(Confidentiality.LOW).integrity(Integrity.LOW));
    }

    @Test
    @DisplayName("a taxonomy written as text reads back equal, with the same fingerprint")
    void roundTrip() {
        Taxonomy all = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).extend(new FinanceTaxonomy())
                .extend(new SupplyChainTaxonomy()).extend(new ProjectOperationsTaxonomy())
                .extend(new HumanResourcesTaxonomy()).build();
        String text = all.toTsv();
        Taxonomy read = Taxonomy.fromTsv(text);
        assertAll(
                () -> assertEquals(all, read),
                () -> assertEquals(all.hashCode(), read.hashCode()),
                () -> assertEquals(text, read.toTsv()),
                () -> assertEquals(all.fingerprint(), read.fingerprint()),
                () -> assertEquals(64, all.fingerprint().length()),
                () -> assertTrue(text.startsWith("#linneo-taxonomy\t1\n")),
                () -> assertTrue(TaxonomyDiff.between(all, read).isEmpty()),
                () -> assertNotEquals(all.fingerprint(), Taxonomy.standard().fingerprint()));
    }

    @Test
    @DisplayName("texts with tabs, line breaks and backslashes survive; malformed texts are refused")
    void text() {
        Taxonomy odd = root().category("data.odd", "Odd\tname", "Line one\nline two \\ end",
                c -> c.attribute("group", "a\\tb")).build();
        assertEquals(odd, Taxonomy.fromTsv(odd.toTsv()));
        assertAll(
                () -> assertThrows(TaxonomyException.class, () -> Taxonomy.fromTsv("")),
                () -> assertThrows(TaxonomyException.class, () -> Taxonomy.fromTsv("#linneo-taxonomy\t2\n")),
                () -> assertThrows(TaxonomyException.class, () -> Taxonomy.fromTsv("#linneo-taxonomy\t1\n"
                        + "category\tdata\tData\t\tapp\t\t\t\tORDINARY\tLOW\tLOW\tmaybe\n")),
                () -> assertThrows(TaxonomyException.class, () -> Taxonomy.fromTsv("#linneo-taxonomy\t1\n"
                        + "term\tdata\n")),
                // a text that breaks the rules of the builder is refused like any other taxonomy
                () -> assertThrows(TaxonomyException.class, () -> Taxonomy.fromTsv("#linneo-taxonomy\t1\n"
                        + "category\tdata.child\tChild\t\tapp\t\t\t\tORDINARY\tLOW\tLOW\tfalse\n")));
    }

    @Test
    @DisplayName("the same taxonomy has no differences")
    void same() {
        TaxonomyDiff diff = TaxonomyDiff.between(Taxonomy.standard(), Taxonomy.builder().withStandard().build());
        assertAll(
                () -> assertTrue(diff.isEmpty()),
                () -> assertEquals(Bump.NONE, diff.requiredBump()),
                () -> assertTrue(diff.breakingChanges().isEmpty()));
    }

    @Test
    @DisplayName("added terms, tighter protection and deprecations are compatible")
    void compatible() {
        Taxonomy before = root().category("data.a", "A", "").category("data.b", "B", "").build();
        Taxonomy after = root()
                .category("data.a", "A", "", c -> c.confidentiality(Confidentiality.HIGH))
                .category("data.b", "B", "", c -> c.deprecated("2.0", "data.c"))
                .category("data.c", "C", "")
                .dataUse("use", "Use", "")
                .build();
        TaxonomyDiff diff = TaxonomyDiff.between(before, after);
        TermChange tightened = diff.change(Hierarchy.DATA_CATEGORY, "data.a");
        assertAll(
                () -> assertEquals(Set.of(Aspect.PROTECTION), tightened.aspects()),
                () -> assertEquals(Direction.TIGHTENED, tightened.direction()),
                () -> assertEquals(Set.of(Aspect.DEPRECATION, Aspect.REPLACEMENT),
                        diff.change(Hierarchy.DATA_CATEGORY, "data.b").aspects()),
                () -> assertEquals(Set.of(Aspect.ADDED), diff.change(Hierarchy.DATA_CATEGORY, "data.c").aspects()),
                () -> assertNull(diff.change(Hierarchy.DATA_CATEGORY, "data.c").before()),
                () -> assertEquals(Set.of(Aspect.ADDED), diff.change(Hierarchy.DATA_USE, "use").aspects()),
                () -> assertNull(diff.change(Hierarchy.DATA_CATEGORY, "data")),
                () -> assertEquals(Bump.MINOR, diff.requiredBump()),
                () -> assertEquals(diff, diff.requireCompatible()));
    }

    @Test
    @DisplayName("removed terms, weaker or mixed protection and a changed replacement are breaking")
    void breaking() {
        Taxonomy before = root()
                .category("data.a", "A", "", c -> c.confidentiality(Confidentiality.HIGH))
                .category("data.b", "B", "", c -> c.integrity(Integrity.HIGH))
                .category("data.c", "C", "", c -> c.deprecated("2.0", "data.d"))
                .category("data.d", "D", "")
                .category("data.e", "E", "")
                .category("data.gone", "Gone", "")
                .build();
        Taxonomy after = root()
                .category("data.a", "A", "")
                .category("data.b", "B", "", c -> c.confidentiality(Confidentiality.HIGH))
                .category("data.c", "C", "", c -> c.deprecated("2.0", "data.e"))
                .category("data.d", "D", "")
                .category("data.e", "E", "")
                .build();
        TaxonomyDiff diff = TaxonomyDiff.between(before, after);
        assertAll(
                () -> assertEquals(Direction.LOOSENED, diff.change(Hierarchy.DATA_CATEGORY, "data.a").direction()),
                () -> assertEquals(Direction.MIXED, diff.change(Hierarchy.DATA_CATEGORY, "data.b").direction()),
                () -> assertEquals(Set.of(Aspect.REPLACEMENT),
                        diff.change(Hierarchy.DATA_CATEGORY, "data.c").aspects()),
                () -> assertEquals(Set.of(Aspect.REMOVED), diff.change(Hierarchy.DATA_CATEGORY, "data.gone").aspects()),
                () -> assertEquals(4, diff.breakingChanges().size()),
                () -> assertEquals(Bump.MAJOR, diff.requiredBump()));
        TaxonomyException failure = assertThrows(TaxonomyException.class, diff::requireCompatible);
        assertTrue(failure.getMessage().startsWith("4 incompatible taxonomy change(s):"), failure::getMessage);
    }

    @Test
    @DisplayName("names, descriptions, attributes and origins are descriptive changes")
    void descriptive() {
        Taxonomy before = root().category("data.a", "A", "Old.", c -> c.attribute("group", "x")).build();
        Taxonomy after = root().origin("other").category("data.a", "Alpha", "New.", c -> c.attribute("group", "y"))
                .build();
        TermChange change = TaxonomyDiff.between(before, after).change(Hierarchy.DATA_CATEGORY, "data.a");
        assertAll(
                () -> assertEquals(Set.of(Aspect.NAME, Aspect.DESCRIPTION, Aspect.ATTRIBUTES, Aspect.ORIGIN),
                        change.aspects()),
                () -> assertEquals(Bump.PATCH, change.bump()),
                () -> assertFalse(change.isBreaking()));
    }

    @Test
    @DisplayName("dropping an extension removes its terms; a term standard in both versions does not change")
    void movedTerm() {
        Taxonomy withExtension = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();
        TermChange change = TaxonomyDiff.between(withExtension, Taxonomy.standard())
                .change(Hierarchy.DATA_CATEGORY, Categories.USER_CONTACT_EMERGENCY_CONTACT);
        assertNull(change, "the emergency contact is standard in both");
        assertEquals(Bump.MAJOR, TaxonomyDiff.between(withExtension, Taxonomy.standard()).requiredBump(),
                "dropping an extension removes its terms");
    }
}
