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
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.classify.ClassClassification;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.processing.BreachAssessment.Alteration;
import io.github.devbpm.linneo.processing.BreachAssessment.Exposure;
import io.github.devbpm.linneo.processing.BreachAssessment.Identification;
import io.github.devbpm.linneo.processing.BreachAssessment.Severity;
import io.github.devbpm.linneo.processing.BreachAssessment.Unavailability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreachAssessmentTest {

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final Classifier classifier = new Classifier(taxonomy);

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Student {
        @Category(Categories.USER_NAME)
        private String name;
        @Category(Categories.USER_CONTACT_EMAIL)
        private String email;
        @Category(Categories.USER_HEALTH_AND_MEDICAL_DISABILITY)
        private Integer disability;
        @Category(Categories.USER_EDUCATION_GRADE)
        private Double grade;
        @Category(Categories.USER_UNIQUE_ID_RECORD_REFERENCE)
        private Long id;
        @Category(Categories.USER_FINANCIAL_BANK_ACCOUNT)
        private String iban;
        @Category(value = Categories.USER_NAME, identifiability = Identifiability.PSEUDONYMIZED)
        private String alias;
        private long version;
    }

    private List<FieldClassification> fields(String... paths) {
        ClassClassification student = classifier.classify(Student.class);
        return Arrays.stream(paths).map(p -> student.field(p).orElseThrow()).toList();
    }

    @Test
    @DisplayName("sensitive data with names, sent to known recipients, is very high: notify and tell the people")
    void veryHigh() {
        BreachAssessment assessment = BreachAssessment.builder().affected(fields("name", "email", "disability"))
                .confidentiality(Exposure.KNOWN_RECIPIENTS).individuals(340).build();
        assertAll(
                () -> assertEquals(4, assessment.dataProcessingContext()),
                () -> assertEquals("sensitive data " + Categories.USER_HEALTH_AND_MEDICAL_DISABILITY,
                        assessment.contextReason()),
                () -> assertEquals(Identification.MAXIMUM, assessment.identification()),
                () -> assertEquals(4.25, assessment.score()),
                () -> assertEquals(Severity.VERY_HIGH, assessment.severity()),
                () -> assertTrue(assessment.manyIndividuals()),
                () -> assertTrue(assessment.notifyAuthority()),
                () -> assertTrue(assessment.communicateToSubjects()),
                () -> assertTrue(assessment.explanation().startsWith("Severity VERY_HIGH (4.25 = 4 x 1 + 0.25)"),
                        assessment::explanation));
    }

    @Test
    @DisplayName("each level of the methodology, from the data, the identifiers and the circumstances")
    void levels() {
        BreachAssessment grades = BreachAssessment.builder().affected(fields("grade", "id")).build();
        BreachAssessment bank = BreachAssessment.builder().affected(fields("iban", "email"))
                .confidentiality(Exposure.UNKNOWN_RECIPIENTS).build();
        BreachAssessment pseudonymous = BreachAssessment.builder().affected(fields("alias", "disability")).build();
        BreachAssessment worst = BreachAssessment.builder().affected(fields("grade"))
                .confidentiality(Exposure.UNKNOWN_RECIPIENTS).integrity(Alteration.UNRECOVERABLE)
                .availability(Unavailability.PERMANENT).malicious(true).build();
        assertAll(
                () -> assertEquals(0.5, grades.score()),
                () -> assertEquals(Severity.LOW, grades.severity()),
                () -> assertFalse(grades.notifyAuthority()),
                () -> assertTrue(grades.explanation().contains("art. 33.5")),
                () -> assertEquals(2.75, bank.score()),
                () -> assertEquals(Severity.MEDIUM, bank.severity()),
                () -> assertTrue(bank.notifyAuthority()),
                () -> assertFalse(bank.communicateToSubjects()),
                () -> assertEquals(Identification.LIMITED, pseudonymous.identification()),
                () -> assertEquals(2.0, pseudonymous.score()),
                () -> assertEquals(2.0, worst.circumstances()),
                () -> assertEquals(Severity.MEDIUM, worst.severity()));
    }

    @Test
    @DisplayName("unintelligible data spares telling the people and, lost only in confidentiality, notifying the"
            + " authority; data that is not personal is no personal breach")
    void exceptions() {
        BreachAssessment encrypted = BreachAssessment.builder().affected(fields("name", "disability"))
                .confidentiality(Exposure.UNKNOWN_RECIPIENTS).unintelligible(true).build();
        BreachAssessment encryptedAndLost = BreachAssessment.builder().affected(fields("name", "disability"))
                .confidentiality(Exposure.UNKNOWN_RECIPIENTS).availability(Unavailability.TEMPORARY)
                .unintelligible(true).build();
        BreachAssessment technical = BreachAssessment.builder().affected(fields("version"))
                .confidentiality(Exposure.UNKNOWN_RECIPIENTS).build();
        assertAll(
                () -> assertFalse(encrypted.notifyAuthority()),
                () -> assertTrue(encrypted.explanation().contains("EDPB Guidelines 9/2022")),
                () -> assertTrue(encryptedAndLost.notifyAuthority()),
                () -> assertTrue(encryptedAndLost.communicateToSubjects()),
                () -> assertFalse(encrypted.communicateToSubjects()),
                () -> assertTrue(encrypted.explanation().contains("art. 34.3.a")),
                () -> assertEquals(0, technical.dataProcessingContext()),
                () -> assertFalse(technical.notifyAuthority()),
                () -> assertTrue(technical.explanation().contains("not a personal data breach")));
    }

    @Test
    @DisplayName("the context and the identification can be adjusted, and stored fields give the same result")
    void adjustmentsAndInventory() {
        BreachAssessment minors = BreachAssessment.builder().affected(fields("grade", "name"))
                .adjustContext(3, "the students are minors").build();
        BreachAssessment named = BreachAssessment.builder().affected(fields("grade"))
                .identification(Identification.MAXIMUM, "the file name carries the student's name").build();
        DataInventory inventory = classifier.inventory(List.of(Student.class), type -> "students");
        BreachAssessment stored = BreachAssessment.builder()
                .affected(List.of(inventory.field("students", "iban").orElseThrow(),
                        inventory.field("students", "email").orElseThrow()), taxonomy)
                .confidentiality(Exposure.UNKNOWN_RECIPIENTS).build();
        assertAll(
                () -> assertEquals(4, minors.dataProcessingContext()),
                () -> assertTrue(minors.contextReason().endsWith("adjusted +3 (the students are minors)")),
                () -> assertEquals(Identification.MAXIMUM, named.identification()),
                () -> assertEquals(2.75, stored.score()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> BreachAssessment.builder().adjustContext(4, "too much")));
    }
}
