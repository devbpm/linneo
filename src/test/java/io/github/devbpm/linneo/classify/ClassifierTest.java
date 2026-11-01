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
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.CategoryByRow;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.annotation.Exempt;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.Violation.Code;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassifierTest {

    private final Classifier classifier = new Classifier(Taxonomy.standard());

    @SuppressWarnings("unused")
    abstract static class Audited {
        @Category(Categories.USER_UNIQUE_ID)
        private Long createdBy;
        private java.time.Instant createdAt;
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_CONFIGURATION, reason = "menu configuration")
    static class MenuGroup extends Audited {
        private static final String CONSTANT = "x";
        private String code;
        private transient String cache;
        @Exempt("cache of the rendered menu")
        private String renderCache;
    }

    @SuppressWarnings("unused")
    static class Person {
        @Category(Categories.USER_NAME)
        private String name;
        @Category(value = Categories.USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
        private String reference;
        @Category(value = Categories.USER_CONTENT_PRIVATE, confidentiality = Confidentiality.HIGH)
        private String notes;
        @CategoryByRow(declaredBy = "the document category", ceiling = Categories.USER_HEALTH_AND_MEDICAL)
        private byte[] attachment;
        private String undecided;
    }

    // An entity after Hibernate's bytecode enhancement, and the proxy subclass Hibernate generates for it.
    @SuppressWarnings("unused")
    static class Enhanced {
        @Category(Categories.USER_NAME)
        private String name;
        private Object $$_hibernate_entityEntryHolder;
    }

    @SuppressWarnings("unused")
    static class Enhanced$HibernateProxy$x9Kq extends Enhanced {
        private Object handler;
    }

    record Contact(@Category(Categories.USER_CONTACT_EMAIL) String email,
                   @Category(value = Categories.USER_CONTACT, identifiability = Identifiability.AGGREGATED)
                   String stats) {
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.USER_CONTACT, reason = "")
    static class WrongDefault {
        private String value;
    }

    @SuppressWarnings("unused")
    static class WrongFields {
        @Category("user.unknown")
        private String unknown;
        @Category(value = Categories.USER_HEALTH_AND_MEDICAL, confidentiality = Confidentiality.LOW)
        private String lowered;
        @Category(value = Categories.SYSTEM_CONFIGURATION, identifiability = Identifiability.IDENTIFIED)
        private String notPersonal;
        @Category(value = Categories.USER_NAME, integrity = {Integrity.LOW, Integrity.HIGH})
        private String ambiguous;
        @Exempt("")
        @Category(Categories.USER_NAME)
        private String conflicting;
    }

    @Test
    @DisplayName("ignores fields added by bytecode enhancement and classifies a Hibernate proxy as its entity")
    void generatedCode() {
        ClassClassification enhanced = classifier.classify(Enhanced.class);
        ClassClassification proxy = classifier.classify(Enhanced$HibernateProxy$x9Kq.class);
        assertAll(
                () -> assertTrue(enhanced.isValid(), () -> enhanced.violations().toString()),
                () -> assertEquals(List.of("name"), enhanced.fields().stream().map(FieldClassification::path).toList()),
                () -> assertEquals(Enhanced.class, proxy.type()),
                () -> assertTrue(proxy.isValid(), () -> proxy.violations().toString()));
    }

    @Test
    @DisplayName("a class default covers its own fields; inherited fields keep their own annotation")
    void classDefault() {
        ClassClassification menu = classifier.classify(MenuGroup.class);
        assertTrue(menu.isValid(), () -> menu.violations().toString());
        assertEquals(List.of("createdBy", "createdAt", "code"), menu.fields().stream().map(FieldClassification::path)
                .toList());
        FieldClassification code = menu.field("code").orElseThrow();
        assertAll(
                () -> assertEquals(Basis.CLASS_DEFAULT, code.basis()),
                () -> assertEquals("menu configuration", code.reason()),
                () -> assertNull(code.identifiability()),
                () -> assertEquals(Categories.SYSTEM_CONFIGURATION,
                        menu.field("createdAt").orElseThrow().category().key()),
                () -> assertTrue(menu.field("createdBy").orElseThrow().isPersonal()),
                () -> assertTrue(menu.holdsPersonalData()));
    }

    @Test
    @DisplayName("a superclass field without annotation is unclassified when no class gives a default")
    void unclassifiedInheritedField() {
        class Plain extends Audited {
        }
        ClassClassification plain = classifier.classify(Plain.class);
        assertEquals(List.of(Code.UNCLASSIFIED_FIELD), plain.errors().stream().map(Violation::code).toList());
        assertEquals("createdAt", plain.errors().getFirst().path());
    }

    @Test
    @DisplayName("declared fields take their category, levels and identifiability")
    void declaredFields() {
        ClassClassification person = classifier.classify(Person.class);
        FieldClassification reference = person.field("reference").orElseThrow();
        FieldClassification notes = person.field("notes").orElseThrow();
        FieldClassification attachment = person.field("attachment").orElseThrow();
        assertAll(
                () -> assertEquals(Identifiability.PSEUDONYMIZED, reference.identifiability()),
                () -> assertTrue(reference.isPersonal()),
                () -> assertEquals(Confidentiality.HIGH, notes.confidentiality()),
                () -> assertEquals(Basis.BY_ROW, attachment.basis()),
                () -> assertEquals("the document category", attachment.reason()),
                () -> assertEquals(Confidentiality.HIGH, attachment.confidentiality()),
                () -> assertEquals(List.of("undecided"), person.errors().stream().map(Violation::path).toList()));
    }

    @Test
    @DisplayName("record components are classified, and aggregated values are not personal")
    void records() {
        ClassClassification contact = classifier.classify(Contact.class);
        assertTrue(contact.isValid(), () -> contact.violations().toString());
        assertTrue(contact.field("email").orElseThrow().isPersonal());
        assertFalse(contact.field("stats").orElseThrow().isPersonal());
    }

    @Test
    @DisplayName("a class default must be non-personal and give a reason")
    void wrongDefault() {
        List<Code> codes = classifier.classify(WrongDefault.class).errors().stream().map(Violation::code).toList();
        assertTrue(codes.containsAll(List.of(Code.MISSING_REASON, Code.PERSONAL_DEFAULT)), codes::toString);
    }

    @Test
    @DisplayName("reports unknown keys, lowered levels, misplaced identifiability, ambiguity and conflicts")
    void wrongFields() {
        List<Code> codes = classifier.classify(WrongFields.class).errors().stream().map(Violation::code).toList();
        assertTrue(codes.containsAll(List.of(Code.UNKNOWN_CATEGORY, Code.LOWERED_LEVEL,
                Code.IDENTIFIABILITY_NOT_APPLICABLE, Code.AMBIGUOUS_VALUE, Code.CONFLICTING_ANNOTATIONS,
                Code.MISSING_REASON)), codes::toString);
    }

    @Test
    @DisplayName("a deprecated key is a warning and classifies with its replacement")
    void deprecatedKey() {
        Taxonomy taxonomy = Taxonomy.builder().withStandard()
                .category("user.contact.old_email", "Old email", "",
                        c -> c.deprecated("1.0.0", Categories.USER_CONTACT_EMAIL))
                .build();
        @SuppressWarnings("unused")
        class Legacy {
            @Category("user.contact.old_email")
            private String email;
        }
        ClassClassification legacy = new Classifier(taxonomy).classify(Legacy.class);
        assertTrue(legacy.isValid());
        assertEquals(Code.DEPRECATED_CATEGORY, legacy.violations().getFirst().code());
        assertEquals(Categories.USER_CONTACT_EMAIL, legacy.field("email").orElseThrow().category().key());
    }

    @Test
    @DisplayName("verify lists every error of every class")
    void verify() {
        ClassificationException failure = assertThrows(ClassificationException.class,
                () -> classifier.verify(List.of(MenuGroup.class, Person.class, WrongDefault.class)));
        assertEquals(3, failure.errors().size(), failure::getMessage);
        assertTrue(failure.getMessage().contains("Person.undecided"));
        assertEquals(2, classifier.verify(List.of(MenuGroup.class, Contact.class)).size());
    }
    @Test
    @DisplayName("exempted fields are listed with their reason")
    void exempted() {
        ClassClassification menu = classifier.classify(MenuGroup.class);
        assertEquals(List.of(new ClassClassification.Exemption(MenuGroup.class, "renderCache",
                "cache of the rendered menu")), menu.exempted());
        assertTrue(menu.field("renderCache").isEmpty());
    }

    @Test
    @DisplayName("a field that hides a superclass field of the same name is an error, since paths identify fields")
    void duplicateField() {
        @SuppressWarnings("unused")
        @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
        class Hiding extends Audited {
            @Category(Categories.USER_UNIQUE_ID)
            private Long createdBy;
        }
        ClassClassification hiding = classifier.classify(Hiding.class);
        assertEquals(List.of(Code.DUPLICATE_FIELD), hiding.errors().stream().map(Violation::code).toList());
        assertEquals("createdBy", hiding.errors().getFirst().path());
        assertEquals(Audited.class, hiding.field("createdBy").orElseThrow().declaringClass());
    }

    @Test
    @DisplayName("a field's protection folds its identifiability into the nature; the ceiling joins the fields")
    void protection() {
        ClassClassification contact = classifier.classify(Contact.class);
        ClassClassification person = classifier.classify(Person.class);
        FieldClassification stats = contact.field("stats").orElseThrow();
        FieldClassification notes = person.field("notes").orElseThrow();
        assertAll(
                () -> assertEquals(Set.of(LegalNature.NONE), stats.protection().natures()),
                () -> assertEquals(Set.of(LegalNature.ORDINARY), contact.field("email").orElseThrow()
                        .protection().natures()),
                () -> assertEquals(Confidentiality.HIGH, notes.protection().confidentiality()),
                () -> assertEquals("notes", notes.fieldName()),
                () -> assertEquals(Set.of(LegalNature.SPECIAL_CATEGORY), person.ceiling().natures()),
                () -> assertEquals(notes.protection(), person.ceilingOf("notes")),
                () -> assertEquals(Protection.LOWEST, person.ceilingOf("missing")),
                () -> assertTrue(person.ceiling().covers(contact.ceiling())));
    }

    @Test
    @DisplayName("a by-row field accepts categories below its ceiling; a raised classification covers the code's")
    void coversAtRunTime() {
        Taxonomy taxonomy = Taxonomy.standard();
        ClassClassification person = classifier.classify(Person.class);
        FieldClassification attachment = person.field("attachment").orElseThrow();
        FieldClassification name = person.field("name").orElseThrow();
        assertAll(
                () -> assertTrue(attachment.covers(taxonomy.requireCategory(Categories.USER_CONTACT_EMAIL))),
                () -> assertTrue(attachment.covers(taxonomy.requireCategory(Categories.USER_HEALTH_AND_MEDICAL))),
                () -> assertFalse(attachment.covers(taxonomy.requireCategory(Categories.USER_CRIMINAL_HISTORY))),
                // the officer may raise the name to a stricter category, never lower it
                () -> assertTrue(taxonomy.requireCategory(Categories.USER_HEALTH_AND_MEDICAL).protection()
                        .covers(name.protection())),
                () -> assertFalse(taxonomy.requireCategory(Categories.SYSTEM_OPERATIONS).protection()
                        .covers(name.protection())));
    }

    @Test
    @DisplayName("the message of a long list of errors lists the first ones and counts them all by code")
    void longErrorList() {
        List<Violation> errors = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            errors.add(new Violation(Person.class, "f" + i, Violation.Severity.ERROR,
                    i < 45 ? Code.UNCLASSIFIED_FIELD : Code.UNKNOWN_CATEGORY, "problem " + i));
        }
        String message = new ClassificationException(errors).getMessage();
        assertAll(
                () -> assertTrue(message.startsWith("60 classification error(s):")),
                () -> assertTrue(message.contains("problem 49")),
                () -> assertFalse(message.contains("problem 50")),
                () -> assertTrue(message.endsWith("... and 10 more; all of them by code: UNCLASSIFIED_FIELD 45,"
                        + " UNKNOWN_CATEGORY 15"), message));
    }
}
