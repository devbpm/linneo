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
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUse;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.processing.ProcessingViolation.Code;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingCheckerTest {

    @SuppressWarnings("unused")
    static class Student {
        @Category(Categories.USER_NAME)
        private String name;
        @Category(Categories.USER_CONTACT_EMAIL)
        private String email;
        @Category(Categories.USER_HEALTH_AND_MEDICAL_DISABILITY)
        private Integer disabilityDegree;
        @Category(value = Categories.USER_EDUCATION_GRADE, identifiability = Identifiability.AGGREGATED)
        private Double averageGrade;
    }

    @SuppressWarnings("unused")
    static class Note {
        @Category(value = Categories.USER_CONTENT_PRIVATE, confidentiality = Confidentiality.HIGH)
        private String text;
    }

    @SuppressWarnings("unused")
    static class Unclassified {
        private String anything;
    }

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final ProcessingChecker checker = new ProcessingChecker(taxonomy);

    private static ProcessingActivity.Builder enrollment() {
        return ProcessingActivity.builder("enrollment", "Student enrollment")
                .purposes(DataUses.EDUCATION_ENROLLMENT)
                .dataSubjects(DataSubjects.STUDENT)
                .entities(Student.class)
                .legalBasis(LegalBasis.PUBLIC_INTEREST)
                .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
                .legalReference("Higher Education Act, art. 12")
                .retention("Five years after the student leaves")
                .impactAssessment("DPIA-2026-01");
    }

    private static Set<Code> codes(CheckedActivity checked) {
        return Set.copyOf(checked.violations().stream().map(ProcessingViolation::code).toList());
    }

    @Test
    @DisplayName("a complete activity resolves its terms and the personal data of its entities")
    void lawfulActivity() {
        CheckedActivity checked = checker.check(enrollment().build());
        assertAll(
                () -> assertEquals(List.of(), checked.violations()),
                () -> assertEquals(List.of(DataUses.EDUCATION_ENROLLMENT),
                        checked.purposes().stream().map(DataUse::key).toList()),
                () -> assertEquals(List.of(Categories.USER_CONTACT_EMAIL, Categories.USER_HEALTH_AND_MEDICAL_DISABILITY,
                        Categories.USER_NAME), checked.dataCategories().stream().map(DataCategory::key).toList()),
                () -> assertEquals(Set.of(LegalNature.ORDINARY, LegalNature.SPECIAL_CATEGORY), checked.natures()),
                () -> assertTrue(checked.processesSpecialCategories()),
                () -> assertFalse(checked.processesCriminalOffenceData()),
                () -> assertEquals(Confidentiality.HIGH, checked.confidentiality()),
                () -> assertEquals(Integrity.HIGH, checked.integrity()),
                () -> assertTrue(checked.isValid()));
    }

    @Test
    @DisplayName("aggregated fields are not personal data, and field-level raises count for the protection")
    void fieldLevel() {
        CheckedActivity checked = checker.check(ProcessingActivity.builder("notes", "Tutoring notes")
                .purposes(DataUses.EDUCATION_TEACHING).dataSubjects(DataSubjects.STUDENT).entities(Note.class)
                .legalBasis(LegalBasis.CONTRACT).retention("End of the course").build());
        assertAll(
                () -> assertEquals(List.of(), checked.violations()),
                () -> assertEquals(Confidentiality.HIGH, checked.confidentiality()),
                () -> assertEquals(Confidentiality.MEDIUM,
                        taxonomy.requireCategory(Categories.USER_CONTENT_PRIVATE).confidentiality()),
                () -> assertFalse(checker.check(enrollment().build()).dataCategories().stream()
                        .anyMatch(c -> c.key().equals(Categories.USER_EDUCATION_GRADE))));
    }

    @Test
    @DisplayName("special category data without an article 9.2 condition is an error")
    void specialCategory() {
        CheckedActivity checked = checker.check(enrollment().specialCategoryCondition(null).impactAssessment(null)
                .build());
        assertAll(
                () -> assertEquals(Set.of(Code.MISSING_SPECIAL_CATEGORY_CONDITION, Code.IMPACT_ASSESSMENT_RECOMMENDED),
                        codes(checked)),
                () -> assertTrue(checked.errors().get(0).message()
                        .contains(Categories.USER_HEALTH_AND_MEDICAL_DISABILITY)),
                () -> assertFalse(checked.isValid()));
    }

    @Test
    @DisplayName("in Spain explicit consent alone does not lift the prohibition for the LOPDGDD article 9.1 categories")
    void spanishConsentRule() {
        ProcessingActivity.Builder chaplaincy = ProcessingActivity.builder("chaplaincy", "Chaplaincy")
                .purposes(DataUses.EDUCATION_TEACHING).dataSubjects(DataSubjects.STUDENT)
                .dataCategories(Categories.USER_DEMOGRAPHIC_RELIGIOUS_BELIEF, Categories.USER_CONTACT_EMAIL)
                .legalBasis(LegalBasis.CONSENT).specialCategoryCondition(SpecialCategoryCondition.EXPLICIT_CONSENT)
                .retention("While the student is enrolled").impactAssessment("DPIA-2026-03");
        ProcessingChecker spain = new ProcessingChecker(taxonomy, Jurisdiction.SPAIN);
        CheckedActivity inSpain = spain.check(chaplaincy.build());
        CheckedActivity health = spain.check(enrollment()
                .specialCategoryCondition(SpecialCategoryCondition.EXPLICIT_CONSENT).build());
        CheckedActivity unnamedLaw = spain.check(enrollment().legalReference(null).build());
        assertAll(
                () -> assertEquals(Jurisdiction.EU, checker.jurisdiction()),
                () -> assertEquals(List.of(), checker.check(chaplaincy.build()).violations()),
                () -> assertEquals(Set.of(Code.CONSENT_NOT_SUFFICIENT), codes(inSpain)),
                () -> assertTrue(inSpain.errors().get(0).message()
                        .contains(Categories.USER_DEMOGRAPHIC_RELIGIOUS_BELIEF)),
                () -> assertFalse(inSpain.errors().get(0).message().contains(Categories.USER_CONTACT_EMAIL)),
                () -> assertEquals(List.of(), health.violations()),
                () -> assertEquals(Set.of(Code.MISSING_LEGAL_REFERENCE), codes(unnamedLaw)),
                () -> assertTrue(unnamedLaw.warnings().get(0).message().contains("LOPDGDD article 9.2")));
    }

    @Test
    @DisplayName("criminal offence data needs an article 10 basis, and a basis in law needs its reference")
    void criminalOffenceData() {
        ProcessingActivity.Builder screening = ProcessingActivity.builder("screening", "Staff screening")
                .purposes(DataUses.EMPLOYMENT_RECRUITMENT).dataSubjects(DataSubjects.JOB_APPLICANT)
                .dataCategories(Categories.USER_CRIMINAL_HISTORY).legalBasis(LegalBasis.LEGAL_OBLIGATION)
                .retention("Until hired").impactAssessment("DPIA-2026-02");
        CheckedActivity missing = checker.check(screening.build());
        CheckedActivity unnamed = checker.check(screening.criminalDataBasis(CriminalDataBasis.AUTHORISED_BY_LAW)
                .build());
        CheckedActivity complete = checker.check(screening.legalReference("Child Protection Act, art. 13").build());
        assertAll(
                () -> assertEquals(Set.of(Code.MISSING_CRIMINAL_DATA_BASIS, Code.MISSING_LEGAL_REFERENCE),
                        codes(missing)),
                () -> assertEquals(Set.of(Code.MISSING_LEGAL_REFERENCE), codes(unnamed)),
                () -> assertTrue(unnamed.warnings().get(0).message().contains("6.1.c, 10")),
                () -> assertTrue(unnamed.isValid()),
                () -> assertEquals(List.of(), complete.violations()));
    }

    @Test
    @DisplayName("personal data without a lawful basis is an error")
    void missingLegalBasis() {
        CheckedActivity checked = checker.check(enrollment().legalBasis(null).build());
        assertEquals(Set.of(Code.MISSING_LEGAL_BASIS), codes(checked));
    }

    @Test
    @DisplayName("purposes, data subjects and personal data are required")
    void requiredParts() {
        CheckedActivity empty = checker.check(ProcessingActivity.builder("empty", "Empty").build());
        CheckedActivity onlySystem = checker.check(ProcessingActivity.builder("menus", "Menus")
                .purposes(DataUses.ESSENTIAL_SERVICE).dataSubjects(DataSubjects.STUDENT)
                .dataCategories(Categories.SYSTEM_CONFIGURATION).build());
        assertAll(
                () -> assertEquals(Set.of(Code.MISSING_PURPOSE, Code.MISSING_DATA_SUBJECTS, Code.MISSING_PERSONAL_DATA),
                        codes(empty)),
                () -> assertEquals(Set.of(Code.NOT_PERSONAL_DATA, Code.MISSING_PERSONAL_DATA), codes(onlySystem)));
    }

    @Test
    @DisplayName("unknown keys are errors and do not also count as missing data")
    void unknownKeys() {
        CheckedActivity checked = checker.check(ProcessingActivity.builder("typos", "Typos")
                .purposes("education.enrolment").dataSubjects("studnet").dataCategories("user.nmae")
                .legalBasis(LegalBasis.CONSENT).build());
        assertAll(
                () -> assertEquals(Set.of(Code.UNKNOWN_KEY), codes(checked)),
                () -> assertEquals(3, checked.errors().size()));
    }

    @Test
    @DisplayName("an entity with classification errors is an error")
    void unclassifiedEntity() {
        CheckedActivity checked = checker.check(enrollment().entities(Unclassified.class).build());
        assertAll(
                () -> assertEquals(Set.of(Code.ENTITY_NOT_CLASSIFIED), codes(checked)),
                () -> assertTrue(checked.errors().get(0).message().startsWith(Unclassified.class.getName())));
    }

    @Test
    @DisplayName("a deprecated purpose is resolved to its replacement and profiling suggests an impact assessment")
    void deprecatedProfiling() {
        CheckedActivity checked = checker.check(enrollment().purposes(DataUses.PERSONALIZE_PROFILING)
                .impactAssessment(null).build());
        assertAll(
                () -> assertEquals(Set.of(Code.DEPRECATED_KEY, Code.IMPACT_ASSESSMENT_RECOMMENDED), codes(checked)),
                () -> assertEquals(List.of(DataUses.EDUCATION_ENROLLMENT, DataUses.PERSONALIZE_CONTENT_PROFILING),
                        checked.purposes().stream().map(DataUse::key).toList()),
                () -> assertTrue(checked.warnings().get(1).message().contains("special categories of data and"
                        + " profiling")));
    }

    @Test
    @DisplayName("a deprecated category without replacement is still reported")
    void deprecatedCategoryWithoutReplacement() {
        Taxonomy extended = Taxonomy.builder().withStandard()
                .category("user.contact.pager", "Pager", "Pager number.", c -> c.deprecated("0.2.0", null))
                .build();
        CheckedActivity checked = new ProcessingChecker(extended).check(ProcessingActivity
                .builder("paging", "Paging").purposes(DataUses.ESSENTIAL_SERVICE).dataSubjects(DataSubjects.STUDENT)
                .dataCategories("user.contact.pager").legalBasis(LegalBasis.CONTRACT).retention("One year").build());
        assertAll(
                () -> assertEquals(Set.of(Code.DEPRECATED_KEY), codes(checked)),
                () -> assertEquals("The data category 'user.contact.pager' is deprecated",
                        checked.warnings().get(0).message()));
    }

    @Test
    @DisplayName("documentation gaps are warnings: interest, retention, unneeded condition")
    void warnings() {
        CheckedActivity checked = checker.check(ProcessingActivity.builder("newsletter", "Alumni newsletter")
                .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL).dataSubjects(DataSubjects.ALUMNUS)
                .dataCategories(Categories.USER_NAME, Categories.USER_CONTACT_EMAIL)
                .legalBasis(LegalBasis.LEGITIMATE_INTERESTS)
                .specialCategoryCondition(SpecialCategoryCondition.EXPLICIT_CONSENT).build());
        CheckedActivity stated = checker.check(ProcessingActivity.builder("newsletter", "Alumni newsletter")
                .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL).dataSubjects(DataSubjects.ALUMNUS)
                .dataCategories(Categories.USER_NAME).legitimateInterest("Keeping in touch with former students")
                .retention("Until the alumnus unsubscribes").build());
        assertAll(
                () -> assertEquals(Set.of(Code.MISSING_LEGITIMATE_INTEREST, Code.MISSING_RETENTION,
                        Code.UNNEEDED_CONDITION), codes(checked)),
                () -> assertTrue(checked.isValid()),
                () -> assertEquals(LegalBasis.LEGITIMATE_INTERESTS, stated.activity().legalBasis()),
                () -> assertEquals(List.of(), stated.violations()),
                () -> assertEquals(Set.of(Code.UNNEEDED_CONDITION), codes(checker.check(ProcessingActivity
                        .builder("newsletter", "Alumni newsletter").purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL)
                        .dataSubjects(DataSubjects.ALUMNUS).dataCategories(Categories.USER_NAME)
                        .legitimateInterest("Keeping in touch").legalBasis(LegalBasis.CONSENT)
                        .retention("Until consent is withdrawn").build()))));
    }

    @Test
    @DisplayName("verify builds the record, or lists every error including duplicate keys")
    void verify() {
        ProcessingRecord record = checker.verify(enrollment().build());
        ProcessingException exception = assertThrows(ProcessingException.class,
                () -> checker.verify(List.of(enrollment().build(), enrollment().legalBasis(null).build())));
        assertAll(
                () -> assertEquals(1, record.activities().size()),
                () -> assertTrue(record.activity("enrollment").isPresent()),
                () -> assertEquals(List.of(Code.MISSING_LEGAL_BASIS, Code.DUPLICATE_ACTIVITY),
                        exception.errors().stream().map(ProcessingViolation::code).toList()),
                () -> assertTrue(exception.getMessage().startsWith("2 processing error(s):\n - ERROR "
                        + "MISSING_LEGAL_BASIS enrollment: ")));
    }

    @Test
    @DisplayName("the bases carry their GDPR article and fideslang name")
    void vocabularies() {
        assertAll(
                () -> assertEquals("6.1.f", LegalBasis.LEGITIMATE_INTERESTS.article()),
                () -> assertEquals("Legal obligations", LegalBasis.LEGAL_OBLIGATION.fideslangName()),
                () -> assertEquals(10, SpecialCategoryCondition.values().length),
                () -> assertEquals("Archiving, research and statistics (with a basis in law)",
                        SpecialCategoryCondition.ARCHIVING_RESEARCH_AND_STATISTICS.fideslangName()),
                () -> assertEquals("SCCs", TransferSafeguard.STANDARD_CONTRACTUAL_CLAUSES.fideslangName()),
                () -> assertThrows(IllegalArgumentException.class, () -> ProcessingActivity.builder(" ", "x").build()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ProcessingActivity.builder(" enrollment", "x").build()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ProcessingActivity.builder("a|b", "x").build()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ProcessingActivity.builder("a`b", "x").build()),
                () -> assertEquals("Enrollment", ProcessingActivity.builder("enrollment-2026.v1", " Enrollment ")
                        .build().name()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ProcessingRecord(List.of(checker.check(enrollment().legalBasis(null).build())))),
                () -> assertThrows(IllegalArgumentException.class, () -> new Transfer(" ",
                        TransferSafeguard.ADEQUACY_DECISION)));
    }
}
