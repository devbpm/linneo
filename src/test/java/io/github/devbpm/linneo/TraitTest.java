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

import io.github.devbpm.linneo.TaxonomyDiff.Bump;
import io.github.devbpm.linneo.TermChange.Aspect;
import io.github.devbpm.linneo.TermChange.Hierarchy;
import io.github.devbpm.linneo.extension.finance.FinanceDataUses;
import io.github.devbpm.linneo.extension.finance.FinanceTaxonomy;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesCategories;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesDataSubjects;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesTaxonomy;
import io.github.devbpm.linneo.extension.university.UniversityDataUses;
import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitTest {

    private final Taxonomy standard = Taxonomy.standard();

    @Test
    @DisplayName("the standard terms carry their traits, and children inherit them")
    void standardTraits() {
        assertAll(
                () -> assertTrue(standard.requireCategory(Categories.USER_FINANCIAL_BANK_ACCOUNT)
                        .has(Trait.FINANCIAL)),
                () -> assertTrue(standard.requireCategory(Categories.USER_CONTACT_EMAIL)
                        .has(Trait.CONTACT_IDENTIFIER)),
                () -> assertTrue(standard.requireDataUse(DataUses.MARKETING_ADVERTISING)
                        .has(Trait.DIRECT_MARKETING)),
                () -> assertTrue(standard.requireDataUse(DataUses.PERSONALIZE_PROFILING).has(Trait.EVALUATION)),
                () -> assertTrue(standard.requireDataSubject(DataSubjects.EMPLOYEE).has(Trait.VULNERABLE)),
                () -> assertTrue(standard.requireDataSubject(DataSubjects.JOB_APPLICANT).has(Trait.VULNERABLE)),
                () -> assertFalse(standard.requireDataSubject(DataSubjects.CUSTOMER).has(Trait.VULNERABLE)),
                () -> assertEquals(Set.of(), standard.requireCategory(Categories.SYSTEM_OPERATIONS).traits()));
    }

    @Test
    @DisplayName("the shipped extensions declare the traits of their terms")
    void extensionTraits() {
        Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new HumanResourcesTaxonomy())
                .extend(new UniversityTaxonomy()).extend(new FinanceTaxonomy()).build();
        assertAll(
                () -> assertTrue(taxonomy.requireCategory(HumanResourcesCategories.USER_EMPLOYMENT_COMPENSATION)
                        .has(Trait.FINANCIAL)),
                () -> assertTrue(taxonomy.requireCategory(HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL_GARNISHMENT)
                        .has(Trait.FINANCIAL)),
                () -> assertTrue(taxonomy.requireDataSubject(HumanResourcesDataSubjects.REPORTING_PERSON)
                        .has(Trait.VULNERABLE)),
                () -> assertTrue(taxonomy.requireDataUse(UniversityDataUses.ADVANCEMENT_FUNDRAISING)
                        .has(Trait.DIRECT_MARKETING)),
                () -> assertTrue(taxonomy.requireDataUse(UniversityDataUses.ADVANCEMENT_PROSPECT_RESEARCH)
                        .traits().containsAll(Set.of(Trait.DIRECT_MARKETING, Trait.EVALUATION))),
                () -> assertTrue(taxonomy.requireDataUse(FinanceDataUses.FINANCE_CREDIT_MANAGEMENT)
                        .has(Trait.EVALUATION)));
    }

    @Test
    @DisplayName("a trait applies to some hierarchies only")
    void applicability() {
        assertAll(
                () -> assertThrows(TaxonomyException.class, () -> Taxonomy.builder().withStandard()
                        .refineDataUse(DataUses.MARKETING, u -> u.traits(Trait.FINANCIAL))),
                () -> assertThrows(TaxonomyException.class, () -> Taxonomy.builder().withStandard()
                        .refineDataSubject(DataSubjects.CUSTOMER, s -> s.traits(Trait.EVALUATION))),
                () -> assertThrows(TaxonomyException.class, () -> Taxonomy.builder().withStandard()
                        .refineDataSubject("nobody", s -> s.traits(Trait.VULNERABLE))),
                () -> assertTrue(Trait.MONITORING.appliesTo(Hierarchy.DATA_USE)),
                () -> assertFalse(Trait.MONITORING.appliesTo(Hierarchy.DATA_SUBJECT)));
    }

    @Test
    @DisplayName("an application marks its own terms, which then weigh like standard ones, and the text keeps them")
    void declaredAndStored() {
        Taxonomy taxonomy = Taxonomy.builder().withStandard()
                .dataSubject("minor_patient", "Minor patient", "", s -> s.traits(Trait.VULNERABLE))
                .refineDataSubject(DataSubjects.CUSTOMER, s -> s.traits(Trait.VULNERABLE))
                .build();
        Taxonomy read = Taxonomy.fromTsv(taxonomy.toTsv());
        assertAll(
                () -> assertTrue(taxonomy.requireDataSubject("minor_patient").has(Trait.VULNERABLE)),
                () -> assertEquals(taxonomy, read),
                () -> assertTrue(read.requireDataSubject(DataSubjects.CUSTOMER).has(Trait.VULNERABLE)),
                () -> assertTrue(taxonomy.toTsv().contains("trait\tsubject\tcustomer\tVULNERABLE")));
    }

    @Test
    @DisplayName("adding a trait is a minor change, removing one a major change")
    void versioning() {
        Taxonomy marked = Taxonomy.builder().withStandard()
                .refineDataSubject(DataSubjects.CUSTOMER, s -> s.traits(Trait.VULNERABLE)).build();
        TermChange added = TaxonomyDiff.between(standard, marked).change(Hierarchy.DATA_SUBJECT,
                DataSubjects.CUSTOMER);
        TermChange removed = TaxonomyDiff.between(marked, standard).change(Hierarchy.DATA_SUBJECT,
                DataSubjects.CUSTOMER);
        assertAll(
                () -> assertEquals(Set.of(Aspect.TRAITS), added.aspects()),
                () -> assertEquals(Bump.MINOR, added.bump()),
                () -> assertEquals(Bump.MAJOR, removed.bump()));
    }
}
