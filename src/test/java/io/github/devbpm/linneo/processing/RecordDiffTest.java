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
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Protection.Direction;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.InventoryField;
import io.github.devbpm.linneo.processing.ActivityChange.Aspect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordDiffTest {

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final DataInventory stored = DataInventory.builder()
            .field("student", "email", Categories.USER_CONTACT_EMAIL, Basis.DECLARED).build();

    private static ProcessingActivity contact() {
        return ProcessingActivity.builder("contact", "Contacting students")
                .purposes(DataUses.EDUCATION_TEACHING).dataSubjects(DataSubjects.STUDENT)
                .legalBasis(LegalBasis.CONTRACT).retention("End of studies").objects("student").build();
    }

    private static ProcessingActivity alumni() {
        return ProcessingActivity.builder("alumni", "Alumni newsletter")
                .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL).dataSubjects(DataSubjects.ALUMNUS)
                .dataCategories(Categories.USER_NAME, Categories.USER_CONTACT_EMAIL)
                .legalBasis(LegalBasis.CONSENT).retention("Until consent is withdrawn").build();
    }

    private ProcessingRecord record(DataInventory inventory, ProcessingActivity... activities) {
        return new ProcessingChecker(taxonomy, Jurisdiction.EU, inventory).verify(List.of(activities));
    }

    @Test
    @DisplayName("the same activities in another order are the same record, with the same fingerprint")
    void sameRecord() {
        ProcessingRecord one = record(stored, contact(), alumni());
        ProcessingRecord other = record(stored, alumni(), contact());
        assertAll(
                () -> assertTrue(RecordDiff.between(one, other).isEmpty()),
                () -> assertEquals(one.fingerprint(), other.fingerprint()),
                () -> assertTrue(one.fingerprint().matches("[0-9a-f]{64}")));
    }

    @Test
    @DisplayName("an edit of the governance is a governance change, with the open points it closes")
    void governanceEdit() {
        ProcessingActivity open = contact().withGovernance(contact().governance().toBuilder().retention(null).build());
        ProcessingActivity edited = contact().withGovernance(contact().governance().toBuilder()
                .retention("Six years after the student leaves").recipients("Ministry of Education").build());
        ProcessingRecord before = record(stored, open);
        ProcessingRecord after = record(stored, edited);
        ActivityChange change = RecordDiff.between(before, after).change("contact").orElseThrow();
        assertAll(
                () -> assertEquals(EnumSet.of(Aspect.RETENTION, Aspect.RECIPIENTS, Aspect.OPEN_POINTS),
                        change.aspects()),
                () -> assertTrue(change.isGovernance()),
                () -> assertEquals(Direction.SAME, change.direction()),
                () -> assertNotEquals(before.fingerprint(), after.fingerprint()));
    }

    @Test
    @DisplayName("a change of the data an activity reaches says how its protection moved")
    void dataChange() {
        DataInventory declared = DataInventory.builder()
                .field(new InventoryField("student", "email", Categories.USER_CONTACT_EMAIL, Basis.DECLARED,
                        Confidentiality.HIGH, null, null, null))
                .field("student", "name", Categories.USER_NAME, Basis.DECLARED).build();
        ActivityChange change = RecordDiff.between(record(stored, contact()), record(declared, contact()))
                .change("contact").orElseThrow();
        assertAll(
                () -> assertEquals(EnumSet.of(Aspect.PERSONAL_DATA, Aspect.PROTECTION), change.aspects()),
                () -> assertEquals(Direction.TIGHTENED, change.direction()),
                () -> assertFalse(change.isGovernance()));
    }

    @Test
    @DisplayName("activities only one record has are added or removed, by key")
    void addedAndRemoved() {
        List<ActivityChange> changes = RecordDiff.between(record(stored, contact()), record(stored, alumni()))
                .changes();
        assertAll(
                () -> assertEquals(List.of("alumni", "contact"), changes.stream().map(ActivityChange::activity)
                        .toList()),
                () -> assertEquals(EnumSet.of(Aspect.ADDED), changes.getFirst().aspects()),
                () -> assertNull(changes.getFirst().before()),
                () -> assertEquals(EnumSet.of(Aspect.REMOVED), changes.get(1).aspects()),
                () -> assertNull(changes.get(1).after()),
                () -> assertTrue(RecordDiff.between(record(stored, contact()), record(stored, contact()))
                        .change("contact").isEmpty()));
    }

    @Test
    @DisplayName("a record holds each activity key once, and a change always says what changed")
    void invariants() {
        CheckedActivity checked = new ProcessingChecker(taxonomy, Jurisdiction.EU, stored).check(contact());
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ProcessingRecord(List.of(checked, checked))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ActivityChange("contact", checked, checked, EnumSet.noneOf(Aspect.class),
                                Direction.SAME)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ActivityChange("contact", null, null, EnumSet.of(Aspect.NAME), Direction.SAME)));
    }
}
