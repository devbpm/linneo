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
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.Composite;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.annotation.DerivedFrom;
import io.github.devbpm.linneo.annotation.Exempt;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.Violation.Code;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The classifier fails closed where the declared type of a field is richer than what it can embed: a field that may
 * hold data it cannot see never takes a class default, and protection is checked whichever way a value is stored.
 */
class ClassifierFailClosedTest {

    private final Classifier classifier = new Classifier(Taxonomy.standard());

    @SuppressWarnings("unused")
    @Composite
    static class Address {
        @Category(Categories.USER_CONTACT_ADDRESS_CITY)
        private String city;
    }

    @SuppressWarnings("unused")
    static class HomeAddress extends Address {
        @Category(Categories.USER_CONTACT_ADDRESS_STREET)
        private String street;
    }

    @SuppressWarnings("unused")
    static class Base<T> {
        private T payload;
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Envelope extends Base<Address> {
        private long id;
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Middle<X> extends Base<List<X>> {
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Deep extends Middle<Address> {
    }

    /** A list subclass whose element type comes from its supertype. */
    static class AddressList extends ArrayList<Address> {
        private static final long serialVersionUID = 1L;
    }

    /** Another record, with classified fields, that is not a composite. */
    @SuppressWarnings("unused")
    static class Person {
        @Category(Categories.USER_NAME)
        private String name;
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Holder {
        private List<List<Address>> history;
        private Optional<List<Address>> previous;
        private HomeAddress home;
        private AddressList moves;
        private Address[][] grid;
        private List<? extends Address> bounded;
        private List<String> tags;
        private Class<?> kind;
        private Map<String, Integer> counters;
    }

    @Test
    @DisplayName("composites behind maps, nested containers, subclasses and supertypes are embedded")
    void embedded() {
        ClassClassification holder = classifier.classify(Holder.class);
        assertAll(
                () -> assertTrue(holder.isValid(), holder::toString),
                () -> assertEquals(List.of("history.city", "previous.city", "home.city",
                        "home.street", "moves.city", "grid.city", "bounded.city"), holder.personalFields().stream()
                        .map(FieldClassification::path).toList()),
                () -> assertEquals(Basis.CLASS_DEFAULT, holder.field("tags").orElseThrow().basis()),
                () -> assertEquals(Basis.CLASS_DEFAULT, holder.field("kind").orElseThrow().basis()),
                () -> assertEquals(Basis.CLASS_DEFAULT, holder.field("counters").orElseThrow().basis()));
    }

    @Test
    @DisplayName("a type variable is resolved through the superclasses of the class being classified")
    void typeVariables() {
        assertAll(
                () -> assertTrue(classifier.classify(Envelope.class).field("payload.city").isPresent()),
                () -> assertTrue(classifier.classify(Deep.class).field("payload.city").isPresent()));
    }

    @SuppressWarnings({"unused", "rawtypes"})
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Opaque<T> {
        private Object anything;
        private T unbound;
        private List raw;
        private List<?> wildcard;
        private Map<Address, String> byAddress;
        private Supplier<Address> later;
        private Map<String, Object> attributes;
        private Person owner;
        private List<Person> people;
        private List<? super Address> lower;
    }

    @Test
    @DisplayName("a field that may hold data the classifier cannot see takes no class default")
    void opaque() {
        ClassClassification opaque = classifier.classify(Opaque.class);
        assertAll(
                () -> assertEquals(List.of("anything", "unbound", "raw", "wildcard", "byAddress", "later",
                        "attributes", "owner", "people", "lower"), opaque.errors().stream()
                        .filter(v -> v.code() == Code.UNCLASSIFIED_FIELD).map(Violation::path).toList()),
                () -> assertTrue(opaque.errors().stream().anyMatch(v -> v.path().equals("raw")
                        && v.message().contains("values typed Object")), opaque::toString),
                () -> assertTrue(opaque.fields().isEmpty()));
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Stored {
        @Category(Categories.SYSTEM_OPERATIONS)
        private Map<Address, String> byAddress;
        @Category(Categories.USER_CONTACT_ADDRESS)
        private Supplier<Address> later;
    }

    @Test
    @DisplayName("a field that stores composites it cannot split must cover their fields")
    void storedWhole() {
        ClassClassification stored = classifier.classify(Stored.class);
        assertEquals(List.of("byAddress"), stored.errors().stream()
                .filter(v -> v.code() == Code.WEAKER_THAN_PARTS).map(Violation::path).toList());
    }

    @SuppressWarnings("unused")
    static class Secrets {
        @Category(Categories.SYSTEM_AUTHENTICATION_SECRET)
        private String apiKey;
        @Category(Categories.USER_NAME)
        private String name;
        @DerivedFrom("apiKey")
        @Category(value = Categories.USER_NAME, identifiability = Identifiability.ANONYMIZED)
        private String apiKeyHint;
        @DerivedFrom("name")
        @Category(value = Categories.USER_NAME, identifiability = Identifiability.ANONYMIZED)
        private String nameHash;
    }

    @Test
    @DisplayName("an anonymized or aggregated value drops the personal nature of its sources, not their secrecy")
    void summarized() {
        ClassClassification secrets = classifier.classify(Secrets.class);
        assertAll(
                () -> assertEquals(List.of("apiKeyHint"), secrets.errors().stream()
                        .filter(v -> v.code() == Code.WEAKER_THAN_SOURCE).map(Violation::path).toList()),
                () -> assertFalse(secrets.field("nameHash").orElseThrow().isPersonal()));
    }

    @SuppressWarnings("unused")
    @Composite
    static class Node {
        @Category(Categories.USER_NAME)
        private String name;
        @Category(Categories.SYSTEM_OPERATIONS)
        private List<Node> replies;
    }

    @SuppressWarnings("unused")
    @Composite
    static class Left {
        @Category(Categories.USER_NAME)
        private String name;
        @Category(Categories.SYSTEM_OPERATIONS)
        private Right right;
    }

    @SuppressWarnings("unused")
    @Composite
    static class Right {
        @Category(Categories.USER_CONTACT_EMAIL)
        private String email;
        @Category(Categories.SYSTEM_OPERATIONS)
        private Left left;
    }

    @Test
    @DisplayName("a composite that stores itself, or two that store each other, are checked against their parts")
    void selfStored() {
        assertAll(
                () -> assertEquals(List.of("replies"), classifier.classify(Node.class).errors().stream()
                        .filter(v -> v.code() == Code.WEAKER_THAN_PARTS).map(Violation::path).toList()),
                () -> assertEquals(List.of("right", "left"), weakerThanParts(List.of(Left.class, Right.class))),
                () -> assertEquals(List.of("left", "right"), weakerThanParts(List.of(Right.class, Left.class))));
    }

    private List<String> weakerThanParts(Collection<Class<?>> types) {
        return classifier.classifyAll(types).stream().flatMap(c -> c.errors().stream())
                .filter(v -> v.code() == Code.WEAKER_THAN_PARTS).map(Violation::path).toList();
    }

    @SuppressWarnings("unused")
    static class DerivedWhole {
        @Category(Categories.SYSTEM_OPERATIONS)
        private String label;
        @DerivedFrom("label")
        @Category(Categories.SYSTEM_OPERATIONS)
        private Address copy;
        @DerivedFrom("label")
        private Address inherited;
    }

    @Test
    @DisplayName("a derived field that stores a composite as one value is checked against its parts")
    void derivedWhole() {
        assertEquals(List.of("copy", "inherited"), classifier.classify(DerivedWhole.class).errors().stream()
                .filter(v -> v.code() == Code.WEAKER_THAN_PARTS).map(Violation::path).toList());
    }

    @SuppressWarnings("unused")
    static class Transients {
        @Category(Categories.USER_NAME)
        private transient String displayName;
        private transient Object cache;
    }

    @Test
    @DisplayName("a transient field is classified when an annotation says so, and skipped otherwise")
    void transients() {
        ClassClassification transients = classifier.classify(Transients.class);
        assertAll(
                () -> assertTrue(transients.isValid()),
                () -> assertEquals(List.of("displayName"), transients.fields().stream()
                        .map(FieldClassification::path).toList()));
    }

    @SuppressWarnings("unused")
    static class Declarations {
        @Category(Categories.USER_NAME)
        private String plain;
        @Category(value = Categories.USER_NAME, identifiability = Identifiability.IDENTIFIED)
        private String explicit;
        @Category(value = Categories.USER_NAME, identifiability = Identifiability.PSEUDONYMIZED)
        private String pseudonym;
    }

    @Test
    @DisplayName("the inventory records the identifiability a field declares, never the resolved one")
    void declaredIdentifiability() {
        DataInventory inventory = classifier.inventory(List.of(Declarations.class), type -> "declarations");
        DataInventory built = DataInventory.builder()
                .field("declarations", "plain", Categories.USER_NAME, Basis.DECLARED)
                .field("declarations", "explicit", Categories.USER_NAME, Basis.DECLARED)
                .field(new InventoryField("declarations", "pseudonym", Categories.USER_NAME, Basis.DECLARED, null,
                        null, Identifiability.PSEUDONYMIZED, null))
                .build();
        assertAll(
                () -> assertNull(inventory.fields("declarations").getFirst().identifiability()),
                () -> assertEquals(built, inventory),
                () -> assertEquals(built.fingerprint(), inventory.fingerprint()));
    }

    @Composite
    interface Contact {
    }

    @SuppressWarnings("unused")
    @Composite
    abstract static class Place {
        @Category(Categories.USER_CONTACT_ADDRESS_COUNTRY)
        private String country;
    }

    @SuppressWarnings("unused")
    @Composite
    static class Box<T> {
        @Category(Categories.SYSTEM_OPERATIONS)
        private T value;
    }

    /** Holds personal data only through composites. */
    @SuppressWarnings("unused")
    static class Resident {
        private Address home;
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "catalogue")
    static class Country {
        private String code;
    }

    @SuppressWarnings("unused")
    sealed interface Payment permits Card {
    }

    @SuppressWarnings("unused")
    record Card(@Category(Categories.USER_FINANCIAL_CREDIT_CARD) String number) implements Payment {
    }

    @SuppressWarnings("unused")
    @Composite
    static class Leaky {
        private String unclassified;
    }

    static class Outer<T> {
        class Inner {
        }
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Shapes<B extends Comparable<B>> {
        private Contact contact;
        private Place place;
        private Box<Address> box;
        private Map<String, Address> byEmail;
        private Resident resident;
        private Country country;
        private List<Country> visited;
        private Payment payment;
        private java.util.concurrent.atomic.AtomicReference<Resident> later;
        private java.util.function.Supplier<Object> anything;
        private java.io.Serializable blob;
        private B bounded;
        private Outer<Address>.Inner inner;
        @Category(Categories.SYSTEM_OPERATIONS)
        private Leaky leaky;
        @Exempt("lazy collection mapped by the resident")
        private List<Resident> residents;
        @Exempt("lazy collection mapped by the country")
        private List<Country> countries;
        private Object $lock;
    }

    @Test
    @DisplayName("abstract and generic composites, maps, references and opaque types take no class default")
    void shapes() {
        ClassClassification shapes = classifier.classify(Shapes.class);
        assertAll(
                () -> assertEquals(List.of("contact", "place", "box", "byEmail", "resident", "payment", "later",
                        "anything", "blob", "bounded", "inner"), shapes.errors().stream()
                        .filter(v -> v.code() == Code.UNCLASSIFIED_FIELD).map(Violation::path).toList()),
                () -> assertEquals(List.of("leaky"), shapes.errors().stream()
                        .filter(v -> v.code() == Code.WEAKER_THAN_PARTS).map(Violation::path).toList()),
                () -> assertEquals(List.of("residents"), shapes.errors().stream()
                        .filter(v -> v.code() == Code.CONFLICTING_ANNOTATIONS).map(Violation::path).toList()),
                () -> assertTrue(shapes.errors().stream().anyMatch(v -> v.path().equals("resident")
                        && v.message().contains("@DerivedFrom(type = Resident.class"))),
                () -> assertEquals(Basis.CLASS_DEFAULT, shapes.field("country").orElseThrow().basis()),
                () -> assertEquals(Basis.CLASS_DEFAULT, shapes.field("visited").orElseThrow().basis()),
                () -> assertEquals(List.of("residents", "countries"), shapes.exempted().stream()
                        .map(ClassClassification.Exemption::path).toList()),
                () -> assertTrue(shapes.field("$lock").isEmpty()));
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Invoice {
        @DerivedFrom(type = Resident.class, value = "home.city")
        private String city;
        @DerivedFrom(type = Country.class, value = "code")
        private String country;
    }

    @Test
    @DisplayName("a reference declared with @DerivedFrom takes the classification of the record's field")
    void declaredReferences() {
        ClassClassification invoice = classifier.classify(Invoice.class);
        assertAll(
                () -> assertTrue(invoice.isValid(), invoice::toString),
                () -> assertTrue(invoice.field("city").orElseThrow().isPersonal()));
    }

    @SuppressWarnings("unused")
    static class Building {
        @Category(Categories.SYSTEM_OPERATIONS)
        private String code;
        private String manager;
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Room {
        private Building building;
        @Exempt("lazy collection mapped by the building")
        private List<Building> annexes;
    }

    @SuppressWarnings("unused")
    static class Hall {
        @Category(Categories.SYSTEM_OPERATIONS)
        private String code;
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Seat {
        private Hall hall;
        @Exempt("lazy collection mapped by the hall")
        private List<Hall> halls;
    }

    @Test
    @DisplayName("a reference to a record whose fields have errors takes no class default and cannot be exempted")
    void referencesWithErrors() {
        ClassClassification room = classifier.classify(Room.class);
        ClassClassification seat = classifier.classify(Seat.class);
        assertAll(
                () -> assertEquals(List.of("building"), room.errors().stream()
                        .filter(v -> v.code() == Code.UNCLASSIFIED_FIELD).map(Violation::path).toList()),
                () -> assertEquals(List.of("annexes"), room.errors().stream()
                        .filter(v -> v.code() == Code.CONFLICTING_ANNOTATIONS).map(Violation::path).toList()),
                () -> assertTrue(room.errors().stream().allMatch(v -> v.message().contains(
                        "whose fields have errors, so whether it holds personal data is unknown")), room::toString),
                // once the referenced record is classified without errors and holds no personal data, both stand
                () -> assertTrue(seat.isValid(), seat::toString),
                () -> assertEquals(Basis.CLASS_DEFAULT, seat.field("hall").orElseThrow().basis()));
    }

    @Test
    @DisplayName("the problems of a class do not depend on the order in which classes are classified")
    void orderIndependent() {
        List<Class<?>> classes = List.of(Holder.class, Opaque.class, Stored.class, Secrets.class, Node.class,
                Left.class, Right.class, DerivedWhole.class, Envelope.class, Shapes.class, Resident.class,
                Country.class, Room.class, Building.class);
        Map<Class<?>, List<Violation>> expected = new java.util.HashMap<>();
        for (List<Class<?>> order : permutations(classes, 40)) {
            for (ClassClassification classification : classifier.classifyAll(order)) {
                List<Violation> before = expected.putIfAbsent(classification.type(), classification.violations());
                if (before != null) {
                    assertEquals(before, classification.violations(), () -> "order " + order);
                }
            }
        }
    }

    // Some orders of a list: rotations of it and of its reverse, and a few swaps of each.
    private static List<List<Class<?>>> permutations(List<Class<?>> classes, int limit) {
        List<List<Class<?>>> orders = new ArrayList<>();
        for (List<Class<?>> base : List.of(classes, classes.reversed())) {
            for (int shift = 0; shift < base.size(); shift++) {
                List<Class<?>> rotated = new ArrayList<>(base.subList(shift, base.size()));
                rotated.addAll(base.subList(0, shift));
                orders.add(rotated);
                List<Class<?>> swapped = new ArrayList<>(rotated);
                java.util.Collections.swap(swapped, 0, swapped.size() / 2);
                orders.add(swapped);
            }
        }
        return orders.subList(0, Math.min(limit, orders.size()));
    }
}
