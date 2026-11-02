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
package io.github.devbpm.linneo.processing;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.InventoryField;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesCategories;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesTaxonomy;
import io.github.devbpm.linneo.extension.university.UniversityDataSubjects;
import io.github.devbpm.linneo.extension.university.UniversityDataUses;
import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;
import io.github.devbpm.linneo.processing.BreachAssessment.Exposure;
import io.github.devbpm.linneo.processing.BreachAssessment.Identification;
import io.github.devbpm.linneo.processing.SubjectRights.Availability;
import io.github.devbpm.linneo.processing.SubjectRights.Right;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The legal rules follow the traits of the taxonomy, so an extension's terms count like standard ones, and they give
 * the answers the GDPR and the LOPDGDD give in the cases the review found wrong.
 */
class LegalRulesTest {

    private final Taxonomy standard = Taxonomy.standard();

    @Test
    @DisplayName("pay is financial data in a breach, as a bank account is")
    void payroll() {
        Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new HumanResourcesTaxonomy()).build();
        InventoryField salary = new InventoryField("staff", "salary", HumanResourcesCategories.USER_EMPLOYMENT_COMPENSATION,
                Basis.DECLARED, null, null, null, null);
        BreachAssessment assessment = BreachAssessment.builder().affected(List.of(salary), taxonomy)
                .confidentiality(Exposure.UNKNOWN_RECIPIENTS).build();
        assertEquals(3, assessment.dataProcessingContext());
    }

    @Test
    @DisplayName("an anonymized or aggregated field identifies no one and is no personal data in a breach")
    void anonymizedBreach() {
        InventoryField anonymized = new InventoryField("o", "name", Categories.USER_NAME, Basis.DECLARED, null, null,
                Identifiability.ANONYMIZED, null);
        InventoryField email = new InventoryField("o", "email", Categories.USER_CONTACT_EMAIL, Basis.DECLARED, null,
                null, null, null);
        BreachAssessment alone = BreachAssessment.builder().affected(List.of(anonymized), standard)
                .confidentiality(Exposure.UNKNOWN_RECIPIENTS).build();
        BreachAssessment mixed = BreachAssessment.builder().affected(List.of(anonymized, email), standard)
                .confidentiality(Exposure.UNKNOWN_RECIPIENTS).build();
        assertAll(
                () -> assertEquals(0, alone.dataProcessingContext()),
                () -> assertFalse(alone.notifyAuthority()),
                () -> assertEquals(Identification.SIGNIFICANT, mixed.identification()));
    }

    @Test
    @DisplayName("fundraising is direct marketing: the objection to it is absolute")
    void fundraising() {
        Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();
        ProcessingActivity activity = ProcessingActivity.builder("fundraising", "Alumni fundraising")
                .purposes(UniversityDataUses.ADVANCEMENT_FUNDRAISING).dataSubjects(DataSubjects.ALUMNUS)
                .dataCategories(Categories.USER_NAME, Categories.USER_CONTACT_EMAIL)
                .legitimateInterest("Raising funds for scholarships").retention("Five years").build();
        ProcessingRecord record = new ProcessingChecker(taxonomy, Jurisdiction.SPAIN).verify(List.of(activity));
        SubjectRights.ActivityRights rights = SubjectRights.of(record, taxonomy, DataSubjects.ALUMNUS)
                .activities().getFirst();
        assertAll(
                () -> assertEquals(Availability.YES, rights.entitlement(Right.OBJECTION).availability()),
                () -> assertTrue(rights.entitlement(Right.OBJECTION).explanation().contains("21.2")),
                () -> assertEquals(Availability.YES, rights.entitlement(Right.ERASURE).availability()),
                () -> assertTrue(UniversityDataSubjects.DONOR.length() > 0));
    }

    @Test
    @DisplayName("objecting to marketing stops it, but data a legal obligation needs is kept")
    void marketingUnderObligation() {
        ProcessingActivity activity = ProcessingActivity.builder("notices", "Notices")
                .purposes(DataUses.MARKETING_COMMUNICATIONS).dataSubjects(DataSubjects.CUSTOMER)
                .dataCategories(Categories.USER_CONTACT_EMAIL).legalBasis(LegalBasis.LEGAL_OBLIGATION)
                .legalReference("Law 34/2002, article 21").retention("Four years").build();
        ProcessingRecord record = new ProcessingChecker(standard).verify(List.of(activity));
        SubjectRights.ActivityRights rights = SubjectRights.of(record, standard, DataSubjects.CUSTOMER)
                .activities().getFirst();
        assertAll(
                () -> assertEquals(Availability.YES, rights.entitlement(Right.OBJECTION).availability()),
                () -> assertEquals(Availability.CONDITIONAL, rights.entitlement(Right.ERASURE).availability()),
                () -> assertTrue(rights.entitlement(Right.ERASURE).explanation().contains("17.3.b")));
    }

    @Test
    @DisplayName("explicit consent to special category data gives portability and erasure on withdrawal")
    void explicitConsent() {
        ProcessingActivity activity = ProcessingActivity.builder("support", "Disability support")
                .purposes(DataUses.ESSENTIAL_SERVICE).dataSubjects(DataSubjects.STUDENT)
                .dataCategories(Categories.USER_HEALTH_AND_MEDICAL, Categories.USER_NAME)
                .legalBasis(LegalBasis.PUBLIC_INTEREST).legalReference("Law 1/2013")
                .specialCategoryConditions(SpecialCategoryCondition.EXPLICIT_CONSENT)
                .retention("While enrolled").impactAssessment("DPIA-7").build();
        ProcessingRecord record = new ProcessingChecker(standard).verify(List.of(activity));
        SubjectRights.ActivityRights rights = SubjectRights.of(record, standard, DataSubjects.STUDENT)
                .activities().getFirst();
        assertAll(
                () -> assertEquals(Availability.CONDITIONAL, rights.entitlement(Right.PORTABILITY).availability()),
                () -> assertTrue(rights.entitlement(Right.PORTABILITY).explanation().contains("20.1.a")),
                () -> assertEquals(Availability.CONDITIONAL, rights.entitlement(Right.ERASURE).availability()),
                () -> assertTrue(rights.entitlement(Right.ERASURE).explanation().contains("17.1.b")));
    }

    @Test
    @DisplayName("an unknown data subject key is an error, not an empty answer")
    void unknownSubject() {
        ProcessingRecord record = new ProcessingRecord(List.of());
        assertAll(
                () -> assertThrows(TaxonomyException.class, () -> SubjectRights.of(record, standard, "studnet")),
                () -> assertTrue(SubjectRights.of(record, standard, DataSubjects.VISITOR).isEmpty()));
    }

    @Test
    @DisplayName("a stored governance keeps the risk factors the code declares")
    void riskFactorsAreAFloor() {
        ProcessingActivity declared = ProcessingActivity.builder("scoring", "Scoring")
                .purposes(DataUses.PERSONALIZE_PROFILING).dataSubjects(DataSubjects.CUSTOMER)
                .dataCategories(Categories.USER_CONTACT_EMAIL).legalBasis(LegalBasis.CONTRACT).retention("1 year")
                .riskFactors(ImpactScreening.Factor.AUTOMATED_DECISIONS).build();
        ProcessingActivity stored = declared.withGovernance(Governance.builder().legalBasis(LegalBasis.CONTRACT)
                .retention("1 year").riskFactors(ImpactScreening.Factor.LARGE_SCALE).build());
        CheckedActivity checked = new ProcessingChecker(standard).check(stored);
        assertAll(
                () -> assertTrue(stored.governance().riskFactors().contains(ImpactScreening.Factor.AUTOMATED_DECISIONS)),
                () -> assertTrue(stored.governance().riskFactors().contains(ImpactScreening.Factor.LARGE_SCALE)),
                () -> assertEquals("35.3.a", checked.screening().mandatoryCase()),
                () -> assertFalse(checked.isValid()));
    }

    @Test
    @DisplayName("the order in which an activity lists its terms changes neither the record nor its fingerprint")
    void orderIndependent() {
        ProcessingActivity.Builder first = ProcessingActivity.builder("ads", "Ads").dataSubjects(DataSubjects.CUSTOMER)
                .purposes(DataUses.PERSONALIZE_PROFILING, DataUses.MARKETING_ADVERTISING_PROFILING)
                .dataCategories(Categories.USER_LOCATION, Categories.USER_FINANCIAL)
                .legalBasis(LegalBasis.CONSENT).retention("1 year").impactAssessment("DPIA-1");
        ProcessingActivity.Builder second = ProcessingActivity.builder("ads", "Ads")
                .dataSubjects(DataSubjects.CUSTOMER)
                .purposes(DataUses.MARKETING_ADVERTISING_PROFILING, DataUses.PERSONALIZE_PROFILING)
                .dataCategories(Categories.USER_FINANCIAL, Categories.USER_LOCATION)
                .legalBasis(LegalBasis.CONSENT).retention("1 year").impactAssessment("DPIA-1");
        ProcessingChecker checker = new ProcessingChecker(standard);
        ProcessingRecord a = checker.verify(List.of(first.build()));
        ProcessingRecord b = checker.verify(List.of(second.build()));
        assertAll(
                () -> assertEquals(a.fingerprint(), b.fingerprint()),
                () -> assertTrue(RecordDiff.between(a, b).isEmpty()));
    }

    @Test
    @DisplayName("in Spain, administrative sanctions need a law with the rank of statute unless the person consents")
    void administrativeSanctions() {
        ProcessingActivity activity = ProcessingActivity.builder("sanctions", "Sanctions")
                .purposes(DataUses.ESSENTIAL_SERVICE).dataSubjects(DataSubjects.CUSTOMER)
                .dataCategories(Categories.USER_ADMINISTRATIVE_SANCTION).legalBasis(LegalBasis.CONTRACT)
                .retention("Five years").build();
        CheckedActivity spain = new ProcessingChecker(standard, Jurisdiction.SPAIN).check(activity);
        CheckedActivity eu = new ProcessingChecker(standard).check(activity);
        assertAll(
                () -> assertTrue(spain.warnings().stream().anyMatch(w -> w.message().contains("LOPDGDD article 27"))),
                () -> assertTrue(eu.warnings().stream().noneMatch(w -> w.message().contains("LOPDGDD"))));
    }

    @Test
    @DisplayName("text from the activities is shown as text in the Markdown record")
    void markdownEscaping() {
        ProcessingActivity activity = ProcessingActivity.builder("x", "X <b>bold</b>")
                .description("See [here](http://evil) | a\\ path").purposes(DataUses.ESSENTIAL_SERVICE)
                .dataSubjects(DataSubjects.CUSTOMER).dataCategories(Categories.USER_NAME)
                .legalBasis(LegalBasis.CONTRACT).retention("1 year").build();
        String markdown = new ProcessingChecker(standard).verify(List.of(activity)).toMarkdown("Record");
        assertAll(
                () -> assertTrue(markdown.contains("X &lt;b&gt;bold&lt;/b&gt;")),
                () -> assertTrue(markdown.contains("See \\[here\\](http://evil) \\| a\\\\ path")),
                () -> assertFalse(markdown.contains("<b>")));
    }

    @Test
    @DisplayName("text with a lone surrogate has its own fingerprint and reads back exactly")
    void loneSurrogates() {
        DataInventory lone = DataInventory.builder().exempt("o", "f", "lock \uD800").build();
        DataInventory question = DataInventory.builder().exempt("o", "f", "lock ?").build();
        assertAll(
                () -> assertFalse(lone.fingerprint().equals(question.fingerprint())),
                () -> assertEquals(lone, DataInventory.fromTsv(lone.toTsv())),
                () -> assertTrue(lone.toTsv().contains("lock \\ud800")));
    }
}
