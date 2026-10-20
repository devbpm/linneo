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

import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxonomyBuilderTest {

    private static TaxonomyBuilder root() {
        return Taxonomy.builder().category("data", "Data", "Root.", c -> c.nature(LegalNature.ORDINARY)
                .confidentiality(Confidentiality.LOW).integrity(Integrity.LOW).attribute("group", "general"));
    }

    @Test
    @DisplayName("a child inherits every facet and attribute it does not declare")
    void inherits() {
        Taxonomy taxonomy = root().category("data.child", "Child", "", c -> c.integrity(Integrity.HIGH)).build();
        DataCategory child = taxonomy.requireCategory("data.child");
        assertAll(
                () -> assertEquals(LegalNature.ORDINARY, child.nature()),
                () -> assertEquals(Confidentiality.LOW, child.confidentiality()),
                () -> assertEquals(Integrity.HIGH, child.integrity()),
                () -> assertEquals("general", child.attribute("group")),
                () -> assertEquals("data", child.info().parentKey()));
    }

    @Test
    @DisplayName("a root must declare nature, confidentiality and integrity")
    void rootDeclaresEverything() {
        TaxonomyBuilder builder = Taxonomy.builder().category("data", "Data", "", c -> c.nature(LegalNature.NONE));
        assertThrows(TaxonomyException.class, builder::build);
    }

    @Test
    @DisplayName("a child cannot weaken its parent's protection")
    void childCannotWeaken() {
        TaxonomyBuilder lowered = root().category("data.child", "Child", "",
                c -> c.confidentiality(Confidentiality.PUBLIC));
        TaxonomyBuilder notPersonal = root().category("data.child", "Child", "", c -> c.nature(LegalNature.NONE));
        TaxonomyBuilder special = root()
                .category("data.health", "Health", "", c -> c.nature(LegalNature.SPECIAL_CATEGORY))
                .category("data.health.x", "X", "", c -> c.nature(LegalNature.CRIMINAL_OFFENCE));
        assertAll(
                () -> assertThrows(TaxonomyException.class, lowered::build),
                () -> assertThrows(TaxonomyException.class, notPersonal::build),
                () -> assertThrows(TaxonomyException.class, special::build));
    }

    @Test
    @DisplayName("refining can raise a declared facet but never lower it")
    void refineOnlyRaises() {
        Taxonomy taxonomy = Taxonomy.builder().withStandard()
                .refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
                .build();
        assertEquals(Confidentiality.HIGH,
                taxonomy.requireCategory(Categories.USER_GOVERNMENT_ID_PASSPORT_NUMBER).confidentiality());
        TaxonomyBuilder builder = Taxonomy.builder().withStandard();
        assertThrows(TaxonomyException.class, () -> builder.refineCategory(Categories.USER_GOVERNMENT_ID,
                c -> c.confidentiality(Confidentiality.LOW)));
        assertThrows(TaxonomyException.class, () -> builder.refineCategory("user.unknown", c -> c.secret()));
    }

    @Test
    @DisplayName("rejects invalid, duplicate and orphan keys")
    void rejectsBadKeys() {
        assertAll(
                () -> assertThrows(TaxonomyException.class, () -> root().category("Data.Upper", "x", "")),
                () -> assertThrows(TaxonomyException.class, () -> root().category("data..x", "x", "")),
                () -> assertThrows(TaxonomyException.class, () -> root().category("data", "x", "")),
                () -> assertThrows(TaxonomyException.class, root().category("other.child", "x", "")::build));
    }

    @Test
    @DisplayName("replacements must exist and must not form a cycle")
    void checksReplacements() {
        TaxonomyBuilder missing = root().category("data.old", "Old", "", c -> c.deprecated("2.0", "data.none"));
        TaxonomyBuilder cycle = root()
                .category("data.a", "A", "", c -> c.deprecated("2.0", "data.b"))
                .category("data.b", "B", "", c -> c.deprecated("2.0", "data.a"));
        assertAll(
                () -> assertThrows(TaxonomyException.class, missing::build),
                () -> assertThrows(TaxonomyException.class, cycle::build));
    }

    @Test
    @DisplayName("resolves a deprecated category to its current replacement")
    void resolvesReplacements() {
        Taxonomy taxonomy = root()
                .category("data.a", "A", "", c -> c.deprecated("2.0", "data.b"))
                .category("data.b", "B", "", c -> c.deprecated("3.0", "data.c"))
                .category("data.c", "C", "")
                .build();
        assertEquals("data.c", taxonomy.resolveCategory("data.a").key());
    }

    @Test
    @DisplayName("an extension's terms carry its name as origin; reserved and shared names are refused")
    void extensions() {
        TaxonomyExtension university = new TaxonomyExtension() {
            @Override
            public String name() {
                return "university";
            }

            @Override
            public void contribute(TaxonomyBuilder builder) {
                builder.category("user.education.thesis", "Thesis", "Theses of the subject.");
            }
        };
        Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(university).extend(university).build();
        DataCategory thesis = taxonomy.requireCategory("user.education.thesis");
        assertEquals("university", thesis.info().origin());
        assertTrue(thesis.isPersonal());

        TaxonomyExtension reserved = new TaxonomyExtension() {
            @Override
            public String name() {
                return TermInfo.FIDESLANG;
            }

            @Override
            public void contribute(TaxonomyBuilder builder) {
            }
        };
        assertThrows(TaxonomyException.class, () -> Taxonomy.builder().extend(reserved));

        TaxonomyException clash = assertThrows(TaxonomyException.class, () -> Taxonomy.builder().withStandard()
                .extend(university).extend(new UniversityTaxonomy()));
        assertTrue(clash.getMessage().endsWith(UniversityTaxonomy.class.getName() + " are both named 'university'"));
    }
}
