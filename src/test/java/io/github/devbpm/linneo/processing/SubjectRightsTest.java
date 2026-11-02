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
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.InventoryField;
import io.github.devbpm.linneo.processing.SubjectRights.Availability;
import io.github.devbpm.linneo.processing.SubjectRights.Right;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubjectRightsTest {

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final DataInventory inventory = DataInventory.builder()
            .field("students", "email", Categories.USER_CONTACT_EMAIL, Basis.DECLARED)
            .field("students", "name", Categories.USER_NAME, Basis.DECLARED)
            .field("students", "version", Categories.SYSTEM_OPERATIONS, Basis.CLASS_DEFAULT)
            .field(InventoryField.exempt("students", "cache", "rendering cache"))
            .field("grades", "mark", Categories.USER_EDUCATION_GRADE, Basis.DECLARED)
            .build();

    private ProcessingRecord record() {
        ProcessingActivity enrollment = ProcessingActivity.builder("enrollment", "Enrollment")
                .purposes(DataUses.EDUCATION_ENROLLMENT).dataSubjects(DataSubjects.STUDENT).objects("students")
                .legalBasis(LegalBasis.CONTRACT).retention("Six years after the student leaves").build();
        ProcessingActivity grades = ProcessingActivity.builder("grades", "Academic record")
                .purposes(DataUses.EDUCATION_ASSESSMENT).dataSubjects(DataSubjects.STUDENT)
                .objects("students", "grades").legalBasis(LegalBasis.LEGAL_OBLIGATION)
                .legalReference("Royal Decree 822/2021").retention("Permanent").build();
        ProcessingActivity newsletter = ProcessingActivity.builder("newsletter", "Newsletter")
                .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL).dataSubjects(DataSubjects.STUDENT)
                .dataCategories(Categories.USER_CONTACT_EMAIL).legalBasis(LegalBasis.CONSENT)
                .retention("Until consent is withdrawn").build();
        ProcessingActivity payroll = ProcessingActivity.builder("payroll", "Payroll")
                .purposes(DataUses.EMPLOYMENT).dataSubjects(DataSubjects.EMPLOYEE)
                .dataCategories(Categories.USER_NAME).legalBasis(LegalBasis.LEGITIMATE_INTERESTS)
                .legitimateInterest("Paying the staff on time").retention("Five years").build();
        return new ProcessingChecker(taxonomy, Jurisdiction.SPAIN, inventory)
                .verify(List.of(enrollment, grades, newsletter, payroll));
    }

    @Test
    @DisplayName("the activities, categories and stored fields of one category of data subjects")
    void scope() {
        SubjectRights students = SubjectRights.of(record(), taxonomy, DataSubjects.STUDENT);
        Map<String, List<InventoryField>> stored = students.storedFields(inventory, taxonomy);
        assertAll(
                () -> assertEquals(List.of("enrollment", "grades", "newsletter"), students.activities().stream()
                        .map(a -> a.activity().activity().key()).toList()),
                () -> assertEquals(List.of(Categories.USER_CONTACT_EMAIL, Categories.USER_EDUCATION_GRADE,
                        Categories.USER_NAME), students.dataCategories().stream().map(DataCategory::key).toList()),
                () -> assertEquals(Set.of("grades", "students"), students.objects()),
                () -> assertEquals(List.of("grades", "students"), List.copyOf(stored.keySet())),
                () -> assertEquals(List.of("email", "name"), stored.get("students").stream()
                        .map(InventoryField::path).toList()),
                () -> assertTrue(SubjectRights.of(record(), taxonomy, DataSubjects.APPLICANT).isEmpty()));
    }

    @Test
    @DisplayName("each lawful basis gives the rights the GDPR provides for it")
    void rightsByBasis() {
        SubjectRights students = SubjectRights.of(record(), taxonomy, DataSubjects.STUDENT);
        SubjectRights employees = SubjectRights.of(record(), taxonomy, DataSubjects.EMPLOYEE);
        assertAll(
                () -> assertEquals(Availability.CONDITIONAL, availability(students, "enrollment", Right.ERASURE)),
                () -> assertEquals(Availability.YES, availability(students, "enrollment", Right.PORTABILITY)),
                () -> assertEquals(Availability.NO, availability(students, "enrollment", Right.OBJECTION)),
                () -> assertEquals(Availability.NO, availability(students, "grades", Right.ERASURE)),
                () -> assertTrue(students.activity("grades").orElseThrow().entitlement(Right.ERASURE).explanation()
                        .contains("retention: Permanent")),
                () -> assertEquals(Availability.NO, availability(students, "grades", Right.PORTABILITY)),
                () -> assertEquals(Availability.YES, availability(students, "newsletter", Right.ERASURE)),
                () -> assertEquals(Availability.YES, availability(students, "newsletter", Right.OBJECTION)),
                () -> assertEquals(Availability.CONDITIONAL, availability(employees, "payroll", Right.OBJECTION)),
                () -> assertEquals(Availability.CONDITIONAL, availability(employees, "payroll", Right.ERASURE)),
                () -> assertEquals(Availability.NO, availability(employees, "payroll", Right.PORTABILITY)),
                () -> assertTrue(students.activities().stream().allMatch(a -> a.entitlements().stream()
                        .map(SubjectRights.Entitlement::right).toList().equals(List.of(Right.values())))));
    }

    @Test
    @DisplayName("under Spanish law, erased or rectified data is blocked first")
    void blocking() {
        String spain = SubjectRights.of(record(), taxonomy, DataSubjects.STUDENT, Jurisdiction.SPAIN).activity("enrollment")
                .orElseThrow().entitlement(Right.ERASURE).explanation();
        String eu = SubjectRights.of(record(), taxonomy, DataSubjects.STUDENT).activity("enrollment").orElseThrow()
                .entitlement(Right.ERASURE).explanation();
        assertAll(
                () -> assertTrue(spain.contains("LOPDGDD art. 32"), spain),
                () -> assertFalse(eu.contains("LOPDGDD"), eu));
    }

    private static Availability availability(SubjectRights rights, String activity, Right right) {
        return rights.activity(activity).orElseThrow().entitlement(right).availability();
    }
}
