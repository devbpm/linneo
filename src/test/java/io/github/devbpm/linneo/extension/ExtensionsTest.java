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
package io.github.devbpm.linneo.extension;

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyExtension;
import io.github.devbpm.linneo.Term;
import io.github.devbpm.linneo.catalog.atlas.AtlasCatalog;
import io.github.devbpm.linneo.catalog.datahub.DataHubCatalog;
import io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataCatalog;
import io.github.devbpm.linneo.extension.finance.FinanceCategories;
import io.github.devbpm.linneo.extension.finance.FinanceDataSubjects;
import io.github.devbpm.linneo.extension.finance.FinanceDataUses;
import io.github.devbpm.linneo.extension.finance.FinanceTaxonomy;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesCategories;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesDataSubjects;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesDataUses;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesTaxonomy;
import io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsCategories;
import io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsDataSubjects;
import io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsDataUses;
import io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsTaxonomy;
import io.github.devbpm.linneo.extension.supplychain.SupplyChainCategories;
import io.github.devbpm.linneo.extension.supplychain.SupplyChainDataSubjects;
import io.github.devbpm.linneo.extension.supplychain.SupplyChainDataUses;
import io.github.devbpm.linneo.extension.supplychain.SupplyChainTaxonomy;
import io.github.devbpm.linneo.extension.university.UniversityCategories;
import io.github.devbpm.linneo.extension.university.UniversityDataSubjects;
import io.github.devbpm.linneo.extension.university.UniversityDataUses;
import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static io.github.devbpm.linneo.extension.ExtensionTerms.added;
import static io.github.devbpm.linneo.extension.ExtensionTerms.constants;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The conventions every extension shipped with Linneo follows, and the extensions applied together.
 */
class ExtensionsTest {

    private static final Taxonomy STANDARD = Taxonomy.standard();

    record Shipped(TaxonomyExtension extension, Class<?> categories, Class<?> uses, Class<?> subjects,
                   Set<String> useRoots) {

        Taxonomy taxonomy() {
            return Taxonomy.builder().withStandard().extend(extension).build();
        }

        @Override
        public String toString() {
            return extension.name();
        }
    }

    static Stream<Shipped> shipped() {
        return Stream.of(
                new Shipped(new UniversityTaxonomy(), UniversityCategories.class, UniversityDataUses.class,
                        UniversityDataSubjects.class,
                        Set.of(UniversityDataUses.ADVANCEMENT, UniversityDataUses.RESEARCH)),
                new Shipped(new FinanceTaxonomy(), FinanceCategories.class, FinanceDataUses.class,
                        FinanceDataSubjects.class, Set.of()),
                new Shipped(new SupplyChainTaxonomy(), SupplyChainCategories.class, SupplyChainDataUses.class,
                        SupplyChainDataSubjects.class, Set.of()),
                new Shipped(new ProjectOperationsTaxonomy(), ProjectOperationsCategories.class,
                        ProjectOperationsDataUses.class, ProjectOperationsDataSubjects.class, Set.of()),
                new Shipped(new HumanResourcesTaxonomy(), HumanResourcesCategories.class, HumanResourcesDataUses.class,
                        HumanResourcesDataSubjects.class, Set.of()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shipped")
    @DisplayName("the constants name exactly the terms the extension adds, all recorded with its origin")
    void constantsMatchTerms(Shipped shipped) {
        Taxonomy taxonomy = shipped.taxonomy();
        String origin = shipped.extension().name();
        assertAll(
                () -> assertEquals(constants(shipped.categories()), added(taxonomy.categories(),
                        STANDARD.categories())),
                () -> assertEquals(constants(shipped.uses()), added(taxonomy.dataUses(), STANDARD.dataUses())),
                () -> assertEquals(constants(shipped.subjects()), added(taxonomy.dataSubjects(),
                        STANDARD.dataSubjects())),
                () -> assertTrue(addedTerms(shipped, taxonomy).stream()
                        .allMatch(term -> term.info().origin().equals(origin)), "origin"),
                () -> assertTrue(constants(shipped.extension().getClass()).contains(origin), "NAME"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shipped")
    @DisplayName("it is not applied by Taxonomy.load(), since the module provides no service")
    void notRegistered(Shipped shipped) {
        Taxonomy loaded = Taxonomy.load();
        assertTrue(constants(shipped.categories()).stream().noneMatch(k -> loaded.category(k).isPresent()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shipped")
    @DisplayName("follows fideslang's rules for custom labels")
    void fideslangRules(Shipped shipped) {
        Taxonomy taxonomy = shipped.taxonomy();
        assertAll(
                // categories extend existing classes; data uses only add the documented roots
                () -> assertTrue(constants(shipped.categories()).stream()
                        .allMatch(k -> taxonomy.requireCategory(k).info().parent().isPresent())),
                () -> assertEquals(shipped.useRoots(), new TreeSet<>(constants(shipped.uses()).stream()
                        .filter(k -> taxonomy.requireDataUse(k).info().parent().isEmpty()).toList())),
                // data subjects are flat, as in fideslang
                () -> assertTrue(constants(shipped.subjects()).stream()
                        .allMatch(k -> taxonomy.requireDataSubject(k).info().parent().isEmpty())),
                // custom labels carry no version information
                () -> assertTrue(addedTerms(shipped, taxonomy).stream().allMatch(term -> term.info().since() == null
                        && !term.info().isDeprecated()), "version information on a custom label"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shipped")
    @DisplayName("data about people is personal and below user; records about organizations are not")
    void personalData(Shipped shipped) {
        Taxonomy taxonomy = shipped.taxonomy();
        assertAll(constants(shipped.categories()).stream().map(taxonomy::requireCategory).map(c -> () -> {
            assertTrue(c.key().startsWith("user.") || c.key().startsWith("org."), c.key());
            assertEquals(c.key().startsWith("user."), c.isPersonal(), c.key());
            assertEquals(c.key().startsWith("org."), c.nature() == LegalNature.NONE, c.key());
        }));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shipped")
    @DisplayName("special categories and criminal data have the maximum protection")
    void maximumProtection(Shipped shipped) {
        Taxonomy taxonomy = shipped.taxonomy();
        assertTrue(constants(shipped.categories()).stream().map(taxonomy::requireCategory)
                .filter(c -> c.nature().requiresMaximumProtection())
                .allMatch(c -> c.confidentiality() == Confidentiality.HIGH && c.integrity() == Integrity.HIGH));
    }

    @Test
    @DisplayName("all the extensions apply together, and the catalogs export the result")
    void together() {
        TaxonomyBuilder builder = Taxonomy.builder().withStandard();
        shipped().forEach(s -> builder.extend(s.extension()));
        Taxonomy all = builder.build();
        int categories = STANDARD.categories().size() + shipped().mapToInt(s -> constants(s.categories()).size()).sum();
        int uses = STANDARD.dataUses().size() + shipped().mapToInt(s -> constants(s.uses()).size()).sum();
        int subjects = STANDARD.dataSubjects().size() + shipped().mapToInt(s -> constants(s.subjects()).size()).sum();
        assertAll(
                () -> assertEquals(categories, all.categories().size()),
                () -> assertEquals(uses, all.dataUses().size()),
                () -> assertEquals(subjects, all.dataSubjects().size()),
                () -> assertEquals(4 + 3 + 4 + 1 + categories, OpenMetadataCatalog.of(all).tags().size()),
                () -> assertEquals(6 + 3 + 4 + 3 + 4 + 1 + categories,
                        AtlasCatalog.of(all).classificationDefs().size()),
                () -> assertFalse(DataHubCatalog.of(all).proposals().isEmpty()));
    }

    @Test
    @DisplayName("the extensions never share a key, so any of them can be applied with any other")
    void disjoint() {
        List<String> keys = new ArrayList<>();
        shipped().forEach(s -> {
            keys.addAll(constants(s.categories()));
            keys.addAll(constants(s.uses()));
            keys.addAll(constants(s.subjects()));
        });
        assertEquals(keys.size(), new TreeSet<>(keys).size());
    }

    private static List<Term> addedTerms(Shipped shipped, Taxonomy taxonomy) {
        List<Term> terms = new ArrayList<>();
        constants(shipped.categories()).forEach(k -> terms.add(taxonomy.requireCategory(k)));
        constants(shipped.uses()).forEach(k -> terms.add(taxonomy.requireDataUse(k)));
        constants(shipped.subjects()).forEach(k -> terms.add(taxonomy.requireDataSubject(k)));
        return terms;
    }
}
