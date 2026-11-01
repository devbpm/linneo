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
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.CategoryByRow;
import io.github.devbpm.linneo.annotation.Composite;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.annotation.DerivedFrom;
import io.github.devbpm.linneo.annotation.Exempt;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.Violation.Code;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassifierDerivedTest {

    private final Classifier classifier = new Classifier(Taxonomy.standard());

    @SuppressWarnings("unused")
    static class Student {
        @Category(Categories.USER_NAME_FIRST)
        private String firstName;
        @Category(Categories.USER_NAME_LAST)
        private String lastName;
        @Category(Categories.USER_CONTACT_EMAIL)
        private String email;
        @Category(Categories.USER_DEMOGRAPHIC_DATE_OF_BIRTH)
        private LocalDate birthDate;
        @Category(Categories.USER_HEALTH_AND_MEDICAL)
        private String condition;
        @DerivedFrom("birthDate")
        private int age;
        @Category(Categories.USER_NAME)
        @DerivedFrom({"firstName", "lastName"})
        private String fullName;
        @Category(value = Categories.USER_DEMOGRAPHIC_DATE_OF_BIRTH, identifiability = Identifiability.AGGREGATED)
        @DerivedFrom("birthDate")
        private double cohortAverageAge;
        @DerivedFrom("nickname")
        private String greeting;
        @Category(Categories.USER_NAME)
        private String nickname;
        @DerivedFrom("age")
        private boolean adult;
    }

    @Test
    @DisplayName("a derived field without a category inherits the classification of its single source")
    void inherits() {
        ClassClassification student = classifier.classify(Student.class);
        assertTrue(student.isValid(), () -> student.violations().toString());
        FieldClassification age = student.field("age").orElseThrow();
        assertAll(
                () -> assertEquals(Basis.DERIVED, age.basis()),
                () -> assertEquals(Categories.USER_DEMOGRAPHIC_DATE_OF_BIRTH, age.category().key()),
                () -> assertEquals(List.of("birthDate"), age.derivedFrom()),
                () -> assertEquals(Identifiability.IDENTIFIED, age.identifiability()),
                () -> assertTrue(age.isPersonal()),
                () -> assertNull(age.reason()),
                // a source declared later in the class, and a chain of derivations
                () -> assertEquals(Categories.USER_NAME, student.field("greeting").orElseThrow().category().key()),
                () -> assertEquals(Categories.USER_DEMOGRAPHIC_DATE_OF_BIRTH,
                        student.field("adult").orElseThrow().category().key()),
                () -> assertEquals(List.of("age"), student.field("adult").orElseThrow().derivedFrom()));
    }

    @Test
    @DisplayName("a derived field may declare a category that protects at least as much as all its sources")
    void declared() {
        ClassClassification student = classifier.classify(Student.class);
        FieldClassification fullName = student.field("fullName").orElseThrow();
        assertAll(
                () -> assertEquals(Basis.DERIVED, fullName.basis()),
                () -> assertEquals(List.of("firstName", "lastName"), fullName.derivedFrom()),
                () -> assertEquals(Categories.USER_NAME, fullName.category().key()));
    }

    @Test
    @DisplayName("declaring an anonymized or aggregated value is the one explicit way to protect less than a source")
    void aggregated() {
        FieldClassification average = classifier.classify(Student.class).field("cohortAverageAge").orElseThrow();
        assertAll(
                () -> assertEquals(Basis.DERIVED, average.basis()),
                () -> assertFalse(average.isPersonal()));
    }

    @Test
    @DisplayName("a derived field cannot protect less than a source, nor turn personal data into non-personal data")
    void weaker() {
        @SuppressWarnings("unused")
        class Weaker {
            @Category(Categories.USER_HEALTH_AND_MEDICAL)
            private String condition;
            @Category(Categories.USER_CONTACT_EMAIL)
            private String email;
            @Category(Categories.USER_NAME)
            @DerivedFrom("condition")
            private String summary;
            @Category(Categories.SYSTEM_OPERATIONS)
            @DerivedFrom("email")
            private String domain;
            @Category(value = Categories.USER_CONTACT_EMAIL, confidentiality = Confidentiality.HIGH)
            @DerivedFrom("email")
            private String masked;
        }
        ClassClassification weaker = classifier.classify(Weaker.class);
        assertEquals(List.of("summary", "domain"), weaker.errors().stream()
                .filter(v -> v.code() == Code.WEAKER_THAN_SOURCE).map(Violation::path).toList());
        assertTrue(weaker.field("masked").isPresent());
    }

    @Test
    @DisplayName("several sources need a declared category unless they agree")
    void severalSources() {
        @SuppressWarnings("unused")
        class Sources {
            @Category(Categories.USER_NAME_FIRST)
            private String first;
            @Category(Categories.USER_NAME_LAST)
            private String last;
            @Category(Categories.USER_CONTACT_EMAIL)
            private String home;
            @Category(Categories.USER_CONTACT_EMAIL)
            private String work;
            @DerivedFrom({"first", "last"})
            private String full;
            @DerivedFrom({"home", "work"})
            private String preferred;
        }
        ClassClassification sources = classifier.classify(Sources.class);
        assertEquals(List.of(Code.AMBIGUOUS_DERIVATION), sources.errors().stream().map(Violation::code).toList());
        assertEquals("full", sources.errors().getFirst().path());
        assertEquals(Categories.USER_CONTACT_EMAIL, sources.field("preferred").orElseThrow().category().key());
    }

    @Test
    @DisplayName("a source must be a classified field of the class; derivations must not form a cycle")
    void wrongSources() {
        @SuppressWarnings("unused")
        @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
        class Wrong {
            @Exempt("cache")
            private String cache;
            @DerivedFrom("missing")
            private String unknown;
            @DerivedFrom("cache")
            private String fromExempt;
            @DerivedFrom({})
            private String none;
            @DerivedFrom("b")
            private String a;
            @DerivedFrom("a")
            private String b;
            @Exempt("x")
            @DerivedFrom("cache")
            private String conflicting;
            @CategoryByRow(declaredBy = "the type", ceiling = Categories.USER_HEALTH_AND_MEDICAL)
            @DerivedFrom("cache")
            private String byRow;
        }
        List<Violation> errors = classifier.classify(Wrong.class).errors();
        assertAll(
                () -> assertEquals(List.of("unknown", "fromExempt", "none"), errors.stream()
                        .filter(v -> v.code() == Code.UNKNOWN_SOURCE).map(Violation::path).toList()),
                () -> assertEquals(List.of("a", "b"), errors.stream()
                        .filter(v -> v.code() == Code.CYCLE).map(Violation::path).sorted().toList()),
                () -> assertEquals(List.of("conflicting", "byRow"), errors.stream()
                        .filter(v -> v.code() == Code.CONFLICTING_ANNOTATIONS).map(Violation::path).toList()));
    }

    @SuppressWarnings("unused")
    record StudentView(@DerivedFrom(type = Student.class, value = "email") String email,
                       @DerivedFrom(type = Student.class, value = "fullName") String name,
                       @Category(Categories.USER_NAME)
                       @DerivedFrom(type = Student.class, value = "condition") String weaker) {
    }

    @SuppressWarnings("unused")
    static class Ping {
        @DerivedFrom(type = Pong.class, value = "value")
        private String value;
    }

    @SuppressWarnings("unused")
    static class Pong {
        @DerivedFrom(type = Ping.class, value = "value")
        private String value;
    }

    @Test
    @DisplayName("a view of another class derives from that class's fields, which keeps the two consistent")
    void otherClass() {
        ClassClassification view = classifier.classify(StudentView.class);
        FieldClassification email = view.field("email").orElseThrow();
        assertAll(
                () -> assertEquals(Categories.USER_CONTACT_EMAIL, email.category().key()),
                () -> assertEquals(List.of(Student.class.getName() + "#email"), email.derivedFrom()),
                () -> assertEquals(Categories.USER_NAME, view.field("name").orElseThrow().category().key()),
                () -> assertEquals(List.of(Code.WEAKER_THAN_SOURCE),
                        view.errors().stream().map(Violation::code).toList()),
                () -> assertEquals(List.of(Code.CYCLE),
                        classifier.classify(Ping.class).errors().stream().map(Violation::code).toList()));
    }
    @SuppressWarnings("unused")
    static class Order {
        @Category(Categories.USER_FINANCIAL_FEE)
        private BigDecimal total;
        @DerivedFrom(type = Customer.class, value = "name")
        private String customerName;
        // A derived source in a class that derives from this one: a chain, not a cycle.
        @DerivedFrom(type = Customer.class, value = "lastOrderTotal")
        private BigDecimal echo;
        // Depends on a cycle without being part of it.
        @DerivedFrom(type = Invoice.class, value = "reference")
        private String invoice;
    }

    @SuppressWarnings("unused")
    static class Customer {
        @Category(Categories.USER_NAME)
        private String name;
        @DerivedFrom(type = Order.class, value = "total")
        private BigDecimal lastOrderTotal;
        @DerivedFrom(type = Invoice.class, value = "reference")
        private String lastInvoice;
    }

    @SuppressWarnings("unused")
    static class Invoice {
        @DerivedFrom(type = Customer.class, value = "lastInvoice")
        private String reference;
    }

    @Test
    @DisplayName("classes may derive from each other's fields; only fields that depend on each other are a cycle")
    void classesDerivingFromEachOther() {
        List<ClassClassification> forward = classifier.classifyAll(List.of(Order.class, Customer.class,
                Invoice.class));
        List<ClassClassification> backward = classifier.classifyAll(List.of(Invoice.class, Customer.class,
                Order.class));
        ClassClassification order = forward.getFirst();
        ClassClassification customer = forward.get(1);
        assertAll(
                () -> assertEquals(List.of("invoice"), order.errors().stream().map(Violation::path).toList()),
                () -> assertEquals(Code.UNKNOWN_SOURCE, order.errors().getFirst().code()),
                () -> assertEquals(Categories.USER_NAME, order.field("customerName").orElseThrow().category().key()),
                () -> assertEquals(Categories.USER_FINANCIAL_FEE, order.field("echo").orElseThrow().category().key()),
                () -> assertEquals(Categories.USER_FINANCIAL_FEE,
                        customer.field("lastOrderTotal").orElseThrow().category().key()),
                () -> assertEquals(List.of("lastInvoice"), customer.errors().stream()
                        .filter(v -> v.code() == Code.CYCLE).map(Violation::path).toList()),
                () -> assertEquals(List.of("reference"), forward.get(2).errors().stream()
                        .filter(v -> v.code() == Code.CYCLE).map(Violation::path).toList()),
                () -> assertEquals(forward, backward.reversed()),
                () -> assertEquals(forward.get(1), classifier.classify(Customer.class)),
                () -> assertEquals(forward.get(2), classifier.classify(Invoice.class)));
    }

    // Two cycles that share fields, across three classes: a -> b -> c -> a and a -> d -> b. Every field is in a
    // cycle, which a search that only marks the path back to a field met again misses for d when it starts at a.
    @SuppressWarnings("unused")
    static class Tangle1 {
        @DerivedFrom(value = {"b", "d"}, type = Tangle2.class)
        private String a;
    }

    @SuppressWarnings("unused")
    static class Tangle2 {
        @DerivedFrom(type = Tangle3.class, value = "c")
        private String b;
        @DerivedFrom("b")
        private String d;
    }

    @SuppressWarnings("unused")
    static class Tangle3 {
        @DerivedFrom(type = Tangle1.class, value = "a")
        private String c;
        @DerivedFrom(type = Tangle2.class, value = "d")
        private String afterD;
    }

    @Test
    @DisplayName("every field of a tangle of cycles is a CYCLE, and a field depending on one has an unknown source")
    void tangledCycles() {
        List<Class<?>> classes = List.of(Tangle1.class, Tangle2.class, Tangle3.class);
        for (List<Class<?>> order : List.of(classes, classes.reversed(),
                List.of(Tangle2.class, Tangle1.class, Tangle3.class))) {
            List<ClassClassification> result = classifier.classifyAll(order);
            List<String> cycles = result.stream().flatMap(c -> c.errors().stream())
                    .filter(v -> v.code() == Code.CYCLE).map(v -> v.type().getSimpleName() + "." + v.path())
                    .sorted().toList();
            List<String> unknown = result.stream().flatMap(c -> c.errors().stream())
                    .filter(v -> v.code() == Code.UNKNOWN_SOURCE).map(v -> v.type().getSimpleName() + "." + v.path())
                    .toList();
            assertAll(order.toString(),
                    () -> assertEquals(List.of("Tangle1.a", "Tangle2.b", "Tangle2.d", "Tangle3.c"), cycles),
                    () -> assertEquals(List.of("Tangle3.afterD"), unknown));
        }
    }

    @SuppressWarnings("unused")
    @Composite
    static class Label {
        @DerivedFrom(type = Labelled.class, value = "name")
        private String text;
    }

    @SuppressWarnings("unused")
    static class Labelled {
        @Category(Categories.USER_NAME)
        private String name;
        private Label label;
    }

    // A chain through a composite: Stamp.by <- Author.signature <- Article.writer, and Article holds a Stamp.
    @SuppressWarnings("unused")
    @Composite
    static class Stamp {
        @DerivedFrom(type = Author.class, value = "signature")
        private String by;
    }

    @SuppressWarnings("unused")
    static class Author {
        @DerivedFrom(type = Article.class, value = "writer")
        private String signature;
    }

    @SuppressWarnings("unused")
    static class Article {
        @Category(Categories.USER_NAME)
        private String writer;
        private Stamp stamp;
    }

    // A real cycle through a composite: Ring.x <- ring.link, and Link.back <- Ring.x.
    @SuppressWarnings("unused")
    @Composite
    static class Link {
        @DerivedFrom(type = Ring.class, value = "x")
        private String back;
    }

    @SuppressWarnings("unused")
    static class Ring {
        @DerivedFrom("link.back")
        private String x;
        private Link link;
    }

    @Test
    @DisplayName("a composite may derive from a class that holds it; only fields that depend on each other cycle")
    void compositesAndDerivations() {
        List<List<Class<?>>> orders = List.of(List.of(Label.class, Labelled.class), List.of(Labelled.class,
                Label.class), List.of(Stamp.class, Author.class, Article.class), List.of(Article.class, Author.class,
                Stamp.class), List.of(Link.class, Ring.class), List.of(Ring.class, Link.class));
        for (List<Class<?>> order : orders) {
            for (ClassClassification result : classifier.classifyAll(order)) {
                Class<?> type = result.type();
                if (type == Ring.class) {
                    assertEquals(List.of("x", "link.back"), result.errors().stream().map(Violation::path).toList(),
                            order::toString);
                    assertTrue(result.errors().stream().allMatch(v -> v.code() == Code.CYCLE), order::toString);
                } else if (type == Link.class) {
                    assertEquals(List.of(Code.CYCLE), result.errors().stream().map(Violation::code).toList(),
                            order::toString);
                } else {
                    assertEquals(List.of(), result.errors(), () -> order + ": " + result);
                }
            }
        }
        assertAll(
                () -> assertEquals(Categories.USER_NAME, classifier.classify(Labelled.class).field("label.text")
                        .orElseThrow().category().key()),
                () -> assertEquals(Categories.USER_NAME, classifier.classify(Article.class).field("stamp.by")
                        .orElseThrow().category().key()),
                () -> assertEquals(List.of(Author.class.getName() + "#signature"),
                        classifier.classify(Article.class).field("stamp.by").orElseThrow().derivedFrom()));
    }

    @Test
    @DisplayName("naming the class being classified as the type of the sources is the same as omitting it")
    void ownType() {
        FieldClassification greeting = classifier.classify(Greeting.class).field("greeting").orElseThrow();
        assertAll(
                () -> assertEquals(Categories.USER_NAME_FIRST, greeting.category().key()),
                () -> assertEquals(List.of("first"), greeting.derivedFrom()));
    }

    @SuppressWarnings("unused")
    static class Greeting {
        @Category(Categories.USER_NAME_FIRST)
        private String first;
        @DerivedFrom(type = Greeting.class, value = "first")
        private String greeting;
    }
}
