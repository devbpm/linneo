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
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.CategoryByRow;
import io.github.devbpm.linneo.annotation.Composite;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.annotation.DerivedFrom;
import io.github.devbpm.linneo.annotation.Exempt;
import io.github.devbpm.linneo.classify.Violation.Code;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassifierCompositeTest {

    private final Classifier classifier = new Classifier(Taxonomy.standard());

    @SuppressWarnings("unused")
    @Composite
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Address {
        @Category(Categories.USER_CONTACT_ADDRESS_STREET)
        private String street;
        @Category(Categories.USER_CONTACT_ADDRESS_CITY)
        private String city;
        private int version;
    }

    @SuppressWarnings("unused")
    @Composite
    record Phone(@Category(Categories.USER_CONTACT_PHONE_NUMBER) String number) {
    }

    @SuppressWarnings("unused")
    @Composite
    static class Medical {
        @Category(Categories.USER_HEALTH_AND_MEDICAL)
        private String condition;
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_CONFIGURATION, reason = "the entity's own technical columns")
    static class Person {
        @Category(Categories.USER_NAME)
        private String name;
        private Address home;
        private List<Address> previous;
        private Phone[] phones;
        private Optional<Medical> medical;
        private String technical;
        @Category(Categories.USER_CONTACT_ADDRESS)
        private Address serialized;
        @DerivedFrom("home.city")
        private String city;
        @Exempt("monitor for synchronization")
        private Object lock;
    }

    @Test
    @DisplayName("an unannotated field of a composite type is classified by the composite's own fields")
    void descends() {
        ClassClassification person = classifier.classify(Person.class);
        assertTrue(person.isValid(), () -> person.violations().toString());
        assertAll(
                () -> assertEquals(List.of("name", "home.street", "home.city", "home.version", "previous.street",
                        "previous.city", "previous.version", "phones.number", "medical.condition", "technical",
                        "serialized", "city"), person.fields().stream().map(FieldClassification::path).toList()),
                () -> assertEquals(Address.class, person.field("home.street").orElseThrow().declaringClass()),
                () -> assertEquals("street", person.field("home.street").orElseThrow().fieldName()),
                // the composite's default applies to its fields, not the default of the class that embeds it
                () -> assertEquals(Categories.SYSTEM_OPERATIONS,
                        person.field("home.version").orElseThrow().category().key()),
                () -> assertEquals(Categories.SYSTEM_CONFIGURATION,
                        person.field("technical").orElseThrow().category().key()),
                () -> assertEquals(Categories.USER_CONTACT_ADDRESS_CITY,
                        person.field("city").orElseThrow().category().key()),
                () -> assertEquals(List.of(new ClassClassification.Exemption(Person.class, "lock",
                        "monitor for synchronization")), person.exempted()));
    }

    @Test
    @DisplayName("the ceiling of a composite field covers everything it contains")
    void ceiling() {
        ClassClassification person = classifier.classify(Person.class);
        assertAll(
                () -> assertEquals(Set.of(LegalNature.SPECIAL_CATEGORY), person.ceilingOf("medical").natures()),
                () -> assertEquals(Set.of(LegalNature.ORDINARY), person.ceilingOf("home").natures()),
                () -> assertEquals(Set.of(LegalNature.NONE), person.ceilingOf("technical").natures()),
                () -> assertEquals(Protection.LOWEST, person.ceilingOf("hom")));
    }

    @Test
    @DisplayName("a composite stored as one value must protect at least as much as its parts")
    void storedAsOneValue() {
        @SuppressWarnings("unused")
        class Stored {
            @Category(Categories.USER_CONTACT)
            private Medical weak;
            @CategoryByRow(declaredBy = "the record type", ceiling = Categories.USER_NAME)
            private Medical weakByRow;
            @Category(Categories.USER_HEALTH_AND_MEDICAL)
            private Medical strong;
        }
        ClassClassification stored = classifier.classify(Stored.class);
        assertEquals(List.of("weak", "weakByRow"), stored.errors().stream()
                .filter(v -> v.code() == Code.WEAKER_THAN_PARTS).map(Violation::path).toList());
        assertTrue(stored.field("strong").isPresent());
    }

    @SuppressWarnings("unused")
    @Composite
    static class Node {
        @Category(Categories.USER_NAME)
        private String label;
        private List<Node> children;
    }

    @SuppressWarnings("unused")
    @Composite
    static class Broken {
        private String unclassified;
    }

    @Test
    @DisplayName("problems inside a composite are reported at their path; a composite cannot contain itself")
    void problems() {
        @SuppressWarnings("unused")
        class Holder {
            @Category(Categories.USER_NAME)
            private String name;
            private Broken broken;
            private Node tree;
            private Map<String, Address> byType;
            private Map<Address, String> byAddress;
        }
        ClassClassification holder = classifier.classify(Holder.class);
        assertAll(
                () -> assertTrue(holder.field("byType.city").isEmpty()),
                () -> assertEquals(List.of("broken.unclassified", "byType", "byAddress"), holder.errors().stream()
                        .filter(v -> v.code() == Code.UNCLASSIFIED_FIELD).map(Violation::path).toList()),
                () -> assertEquals(List.of("tree.children"), holder.errors().stream()
                        .filter(v -> v.code() == Code.CYCLE).map(Violation::path).toList()),
                () -> assertEquals(Holder.class, holder.errors().getFirst().type()),
                () -> assertTrue(holder.field("tree.label").isPresent()));
    }
    @Test
    @DisplayName("deriving from a composite field derives from all its fields")
    void derivedFromComposite() {
        @SuppressWarnings("unused")
        class Letter {
            private Address address;
            @DerivedFrom("address")
            private String undecided;
            @Category(Categories.USER_CONTACT_ADDRESS)
            @DerivedFrom("address")
            private String formatted;
        }
        ClassClassification letter = classifier.classify(Letter.class);
        assertAll(
                () -> assertEquals(List.of(Code.AMBIGUOUS_DERIVATION),
                        letter.errors().stream().map(Violation::code).toList()),
                () -> assertEquals(List.of("address.street", "address.city", "address.version"),
                        letter.field("formatted").orElseThrow().derivedFrom()));
    }
}
