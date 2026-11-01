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
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.InventoryField;
import io.github.devbpm.linneo.processing.ProcessingViolation.Code;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingInventoryTest {

    private final Taxonomy taxonomy = Taxonomy.standard();

    @SuppressWarnings("unused")
    static class Student {
        @Category(value = Categories.USER_CONTACT_EMAIL, confidentiality = Confidentiality.HIGH)
        private String email;
        @Category(value = Categories.USER_HEALTH_AND_MEDICAL, identifiability = Identifiability.AGGREGATED)
        private Double sickDays;
    }

    private static ProcessingActivity.Builder activity() {
        return ProcessingActivity.builder("contact", "Contacting students")
                .purposes(DataUses.EDUCATION_TEACHING).dataSubjects(DataSubjects.STUDENT)
                .legalBasis(LegalBasis.CONTRACT).retention("End of studies");
    }

    @Test
    @DisplayName("objects of a data inventory give the same personal data and protection as their classes")
    void sameAsEntities() {
        DataInventory inventory = new Classifier(taxonomy).inventory(List.of(Student.class), type -> "student");
        ProcessingChecker checker = new ProcessingChecker(taxonomy, Jurisdiction.EU, inventory);
        CheckedActivity byObject = checker.check(activity().objects("student").build());
        CheckedActivity byEntity = checker.check(activity().entities(Student.class).build());
        assertAll(
                () -> assertEquals(List.of(), byObject.violations()),
                () -> assertEquals(byEntity.dataCategories(), byObject.dataCategories()),
                () -> assertEquals(byEntity.protection(), byObject.protection()),
                () -> assertEquals(List.of(Categories.USER_CONTACT_EMAIL),
                        byObject.dataCategories().stream().map(DataCategory::key).toList()),
                () -> assertEquals(Confidentiality.HIGH, byObject.protection().confidentiality()));
    }

    @Test
    @DisplayName("an object the inventory does not hold, or no inventory at all, is an error")
    void unknownObject() {
        DataInventory stored = DataInventory.builder()
                .field(new InventoryField("legacy", "code", "user.removed", Basis.DECLARED, null, null, null, null))
                .build();
        CheckedActivity noInventory = new ProcessingChecker(taxonomy).check(activity().objects("student").build());
        CheckedActivity unknown = new ProcessingChecker(taxonomy, Jurisdiction.EU, stored)
                .check(activity().objects("student", "legacy").build());
        assertAll(
                () -> assertEquals(List.of(Code.UNKNOWN_KEY), noInventory.errors().stream()
                        .map(ProcessingViolation::code).toList()),
                () -> assertTrue(noInventory.errors().getFirst().message().contains("no data inventory")),
                () -> assertEquals(2, unknown.errors().size()),
                () -> assertTrue(unknown.errors().stream()
                        .allMatch(v -> v.element() == ProcessingViolation.Element.PERSONAL_DATA)));
    }

    @Test
    @DisplayName("a stored field with a deprecated key is checked with its replacement, with a warning")
    void deprecatedKey() {
        Taxonomy renamed = Taxonomy.builder().withStandard().category("user.contact.old_email", "Old email", "",
                c -> c.deprecated("1.0.0", Categories.USER_CONTACT_EMAIL)).build();
        DataInventory stored = DataInventory.builder()
                .field("student", "email", "user.contact.old_email", Basis.DECLARED).build();
        CheckedActivity checked = new ProcessingChecker(renamed, Jurisdiction.EU, stored)
                .check(activity().objects("student").build());
        assertAll(
                () -> assertTrue(checked.isValid(), () -> checked.violations().toString()),
                () -> assertEquals(List.of(Code.DEPRECATED_KEY), checked.warnings().stream()
                        .map(ProcessingViolation::code).toList()),
                () -> assertTrue(checked.warnings().getFirst().message().endsWith("checked as 'user.contact.email'")),
                () -> assertEquals(List.of(Categories.USER_CONTACT_EMAIL),
                        checked.dataCategories().stream().map(DataCategory::key).toList()));
    }
}
