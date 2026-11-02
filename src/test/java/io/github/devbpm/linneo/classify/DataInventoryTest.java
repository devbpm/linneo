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
package io.github.devbpm.linneo.classify;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.CategoryDraft;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Protection.Direction;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyDiff;
import io.github.devbpm.linneo.TermChange;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.Composite;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.annotation.DerivedFrom;
import io.github.devbpm.linneo.annotation.Exempt;
import io.github.devbpm.linneo.classify.FieldChange.Kind;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataInventoryTest {

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final Classifier classifier = new Classifier(taxonomy);

    @SuppressWarnings("unused")
    @Composite
    static class Address {
        @Category(Categories.USER_CONTACT_ADDRESS_CITY)
        private String city;
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical columns")
    static class Student {
        private Long version;
        @Category(value = Categories.USER_CONTACT_EMAIL, confidentiality = Confidentiality.HIGH)
        private String email;
        @Category(Categories.USER_DEMOGRAPHIC_DATE_OF_BIRTH)
        private LocalDate birthDate;
        @DerivedFrom("birthDate")
        private int age;
        private Address home;
        @Exempt("cache of the rendered card")
        private String card;
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_CONFIGURATION, reason = "configuration")
    static class Setting {
        private String name;
    }

    private DataInventory declared() {
        return classifier.inventory(List.of(Student.class, Setting.class),
                type -> type == Student.class ? "student" : "setting");
    }

    @Test
    @DisplayName("the inventory of the code records what each field declares, exemptions included")
    void fromCode() {
        DataInventory inventory = declared();
        assertAll(
                () -> assertEquals(List.of("setting", "student"), List.copyOf(inventory.objects())),
                () -> assertEquals(List.of("age", "birthDate", "card", "email", "home.city", "version"),
                        inventory.fields("student").stream().map(InventoryField::path).toList()),
                () -> assertEquals(new InventoryField("student", "email", Categories.USER_CONTACT_EMAIL,
                        Basis.DECLARED, Confidentiality.HIGH, null, Identifiability.IDENTIFIED, null),
                        inventory.field("student", "email").orElseThrow()),
                () -> assertEquals(new InventoryField("student", "age", Categories.USER_DEMOGRAPHIC_DATE_OF_BIRTH,
                        Basis.DERIVED, null, null, Identifiability.IDENTIFIED, null),
                        inventory.field("student", "age").orElseThrow()),
                () -> assertEquals(InventoryField.exempt("student", "card", "cache of the rendered card"),
                        inventory.field("student", "card").orElseThrow()),
                () -> assertEquals("technical columns", inventory.field("student", "version").orElseThrow().reason()),
                () -> assertEquals(List.of("student"), inventory.fieldsWith(Categories.USER_CONTACT_EMAIL).stream()
                        .map(InventoryField::object).toList()));
    }

    @Test
    @DisplayName("resolved against the taxonomy, every field has the protection the classifier gives it")
    void sameProtection() {
        DataInventory inventory = declared();
        ClassClassification student = classifier.classify(Student.class);
        for (FieldClassification field : student.fields()) {
            InventoryField entry = inventory.field("student", field.path()).orElseThrow();
            assertEquals(field.protection(), entry.protection(taxonomy), field::toString);
            assertEquals(field.isPersonal(), entry.isPersonal(taxonomy), field::toString);
            assertEquals(field.category(), entry.category(taxonomy), field::toString);
        }
        assertEquals(Protection.LOWEST, inventory.field("student", "card").orElseThrow().protection(taxonomy));
    }

    @Test
    @DisplayName("as text it reads back equal; the fingerprints are stable and per object")
    void text() {
        DataInventory inventory = declared();
        DataInventory read = DataInventory.fromTsv(inventory.toTsv());
        DataInventory otherSetting = DataInventory.builder().field("setting", "name", Categories.SYSTEM_OPERATIONS,
                Basis.CLASS_DEFAULT).build();
        assertAll(
                () -> assertEquals(inventory, read),
                () -> assertEquals(inventory.toTsv(), read.toTsv()),
                () -> assertEquals(inventory.fingerprint(), declared().fingerprint()),
                () -> assertEquals(64, inventory.fingerprint("student").length()),
                () -> assertNotEquals(inventory.fingerprint("setting"), otherSetting.fingerprint("setting")),
                () -> assertTrue(inventory.toTsv().startsWith("#linneo-inventory\t1\n")),
                () -> assertThrows(IllegalArgumentException.class, () -> inventory.fingerprint("unknown")),
                () -> assertThrows(IllegalArgumentException.class, () -> DataInventory.fromTsv("#linneo-inventory\t1\n"
                        + "field\tstudent\temail\n")),
                () -> assertThrows(IllegalArgumentException.class, () -> DataInventory.fromTsv("#other\t1\n")));
    }

    @Test
    @DisplayName("an inventory compared with itself has no differences")
    void same() {
        InventoryDiff diff = InventoryDiff.between(declared(), declared(), taxonomy);
        assertTrue(diff.isEmpty());
        assertEquals(List.of(), diff.changes());
    }

    @Test
    @DisplayName("the diff from the stored copy to the code tells added, removed and changed fields and their"
            + " direction")
    void diff() {
        DataInventory stored = DataInventory.builder()
                .field("student", "email", Categories.USER_CONTACT_EMAIL, Basis.DECLARED)
                .field(new InventoryField("student", "birthDate", Categories.USER_DEMOGRAPHIC_DATE_OF_BIRTH,
                        Basis.DECLARED, null, null, Identifiability.IDENTIFIED, null))
                .field("student", "card", Categories.USER_NAME, Basis.DECLARED)
                .field("student", "fax", Categories.USER_CONTACT_PHONE_NUMBER, Basis.DECLARED)
                .field(new InventoryField("student", "version", Categories.SYSTEM_OPERATIONS, Basis.CLASS_DEFAULT,
                        null, null, null, "technical columns"))
                .field(new InventoryField("student", "age", Categories.USER_DEMOGRAPHIC_DATE_OF_BIRTH, Basis.DERIVED,
                        null, null, Identifiability.IDENTIFIED, null))
                .field("student", "home.city", "user.unknown", Basis.DECLARED)
                .field("course", "title", Categories.SYSTEM_OPERATIONS, Basis.CLASS_DEFAULT)
                .build();
        InventoryDiff diff = InventoryDiff.between(stored, declared(), taxonomy);
        Map<String, FieldChange> byPath = diff.changes().stream()
                .filter(c -> c.object().equals("student"))
                .collect(java.util.stream.Collectors.toMap(FieldChange::path, c -> c));
        assertAll(
                () -> assertEquals(Kind.CHANGED, byPath.get("email").kind()),
                () -> assertEquals(Direction.TIGHTENED, byPath.get("email").direction()),
                () -> assertEquals(Kind.CHANGED, byPath.get("card").kind()),
                () -> assertTrue(byPath.get("card").after().isExempt()),
                () -> assertEquals(Direction.LOOSENED, byPath.get("card").direction()),
                () -> assertEquals(Kind.REMOVED, byPath.get("fax").kind()),
                () -> assertEquals(Direction.MIXED, byPath.get("home.city").direction()),
                () -> assertFalse(byPath.containsKey("birthDate")),
                () -> assertFalse(byPath.containsKey("age")),
                () -> assertEquals(List.of("setting"), diff.addedObjects()),
                () -> assertEquals(List.of("course"), diff.removedObjects()),
                () -> assertEquals(List.of("course", "setting", "student"), List.copyOf(diff.changedObjects())),
                () -> assertEquals(List.of("card", "home.city"), diff.weakening().stream()
                        .map(FieldChange::path).toList()),
                () -> assertNull(byPath.get("fax").after()));
    }

    @Test
    @DisplayName("pseudonymizing a field loosens it; declaring it anonymized loosens it too")
    void identifiability() {
        InventoryField identified = new InventoryField("o", "f", Categories.USER_NAME, Basis.DECLARED, null, null,
                Identifiability.IDENTIFIED, null);
        DataInventory before = DataInventory.builder().field(identified).build();
        DataInventory pseudonymized = DataInventory.builder().field(new InventoryField("o", "f", Categories.USER_NAME,
                Basis.DECLARED, null, null, Identifiability.PSEUDONYMIZED, null)).build();
        DataInventory anonymized = DataInventory.builder().field(new InventoryField("o", "f", Categories.USER_NAME,
                Basis.DECLARED, null, null, Identifiability.ANONYMIZED, null)).build();
        assertAll(
                () -> assertEquals(Direction.LOOSENED, InventoryDiff.between(before, pseudonymized, taxonomy)
                        .changes().getFirst().direction()),
                () -> assertEquals(Direction.TIGHTENED, InventoryDiff.between(pseudonymized, before, taxonomy)
                        .changes().getFirst().direction()),
                () -> assertTrue(InventoryDiff.between(before, anonymized, taxonomy).changes().getFirst().weakens()));
    }

    @Test
    @DisplayName("a change of the taxonomy leaves the inventory unchanged and reaches the fields that use the key")
    void taxonomyChange() {
        Taxonomy tightened = Taxonomy.builder().withStandard().refineCategory(Categories.USER_DEMOGRAPHIC_DATE_OF_BIRTH,
                c -> c.confidentiality(Confidentiality.HIGH)).build();
        DataInventory inventory = declared();
        Map<TermChange, List<InventoryField>> affected = inventory.affectedBy(TaxonomyDiff.between(taxonomy,
                tightened));
        assertAll(
                () -> assertTrue(InventoryDiff.between(inventory, inventory, tightened).isEmpty()),
                () -> assertEquals(1, affected.size()),
                () -> assertEquals(List.of("age", "birthDate"), affected.values().iterator().next().stream()
                        .map(InventoryField::path).toList()),
                () -> assertEquals(Direction.TIGHTENED, affected.keySet().iterator().next().direction()));
    }

    @Test
    @DisplayName("an inventory needs valid objects and fields, and keys the application gives")
    void validation() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> DataInventory.builder()
                        .field("o", "f", Categories.USER_NAME, Basis.DECLARED)
                        .exempt("o", "f", "cache")),
                () -> assertThrows(IllegalArgumentException.class, () -> new InventoryField("o", "f", null, null,
                        Confidentiality.HIGH, null, null, "cache")),
                () -> assertThrows(IllegalArgumentException.class, () -> new InventoryField("o", "f",
                        Categories.USER_NAME, null, null, null, null, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> DataInventory.builder().object(" ")),
                () -> assertThrows(IllegalArgumentException.class, () -> classifier.inventory(
                        List.of(Student.class, Setting.class), type -> "same")),
                () -> assertThrows(IllegalArgumentException.class, () -> classifier.inventory(
                        List.of(Student.class), type -> null)),
                () -> assertThrows(ClassificationException.class, () -> classifier.inventory(
                        List.of(Broken.class), type -> "broken")),
                () -> assertEquals(declared(), classifier.inventory(List.of(Student.class, Setting.class,
                        Student.class), type -> type == Student.class ? "student" : "setting")));
    }

    @SuppressWarnings("unused")
    static class Broken {
        private String undecided;
    }

    @Test
    @DisplayName("a change of a replacement reaches the fields that still declare the deprecated key")
    void replacementChange() {
        String legacy = Categories.USER_CONTACT + ".legacy";
        Taxonomy before = Taxonomy.builder().withStandard().category(legacy, "Legacy name", "Replaced",
                c -> c.deprecated("0.1.0", Categories.USER_CONTACT_EMAIL)).build();
        Taxonomy after = Taxonomy.builder().withStandard().category(legacy, "Legacy name", "Replaced",
                c -> c.deprecated("0.1.0", Categories.USER_CONTACT_EMAIL))
                .refineCategory(Categories.USER_CONTACT_EMAIL, CategoryDraft::secret).build();
        DataInventory inventory = DataInventory.builder().field("o", "old", legacy, Basis.DECLARED)
                .field("o", "email", Categories.USER_CONTACT_EMAIL, Basis.DECLARED).build();
        Map<TermChange, List<InventoryField>> affected = inventory.affectedBy(TaxonomyDiff.between(before, after));
        assertEquals(List.of("email", "old"), affected.values().stream().flatMap(List::stream)
                .map(InventoryField::path).toList());
    }

    @Test
    @DisplayName("blank reasons and an explicit IDENTIFIED are stored as absent, so equal declarations are equal")
    void canonical() {
        assertAll(
                () -> assertEquals(new InventoryField("o", "f", Categories.USER_NAME, Basis.DECLARED, null, null, null,
                        null), new InventoryField("o", "f", Categories.USER_NAME, Basis.DECLARED, null, null,
                        Identifiability.IDENTIFIED, " ")),
                () -> assertEquals(DataInventory.builder().field(new InventoryField("o", "f", Categories.USER_NAME,
                        Basis.DECLARED, null, null, null, "")).build().toTsv(), DataInventory.fromTsv(
                        DataInventory.builder().field(new InventoryField("o", "f", Categories.USER_NAME,
                                Basis.DECLARED, null, null, null, "")).build().toTsv()).toTsv()));
    }
}
