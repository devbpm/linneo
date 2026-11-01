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
package io.github.devbpm.linneo.export.openapi;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.annotation.DerivedFrom;
import io.github.devbpm.linneo.annotation.Exempt;
import io.github.devbpm.linneo.classify.ClassClassification;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.FieldClassification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiExtensionTest {

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final Classifier classifier = new Classifier(taxonomy);

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class StudentView {
        @Category(Categories.USER_CONTACT_EMAIL)
        private String email;
        @Category(value = Categories.USER_HEALTH_AND_MEDICAL, identifiability = Identifiability.PSEUDONYMIZED)
        private String condition;
        @DerivedFrom("email")
        private String domain;
        private int page;
        @Exempt("hypermedia links")
        private String links;
    }

    @Test
    @DisplayName("a property gets its category, whether it is personal data and the protection it needs")
    void extension() {
        ClassClassification view = classifier.classify(StudentView.class);
        FieldClassification email = view.field("email").orElseThrow();
        Map<String, Object> extension = OpenApiExtension.of(email);
        assertAll(
                () -> assertEquals(List.of("category", "personal", "nature", "confidentiality", "integrity",
                        "identifiability", "secret"), List.copyOf(extension.keySet())),
                () -> assertEquals(Categories.USER_CONTACT_EMAIL, extension.get("category")),
                () -> assertEquals(true, extension.get("personal")),
                () -> assertEquals(email.confidentiality().name(), extension.get("confidentiality")),
                () -> assertEquals("SPECIAL_CATEGORY", OpenApiExtension.of(view.field("condition").orElseThrow())
                        .get("nature")),
                () -> assertEquals("PSEUDONYMIZED", OpenApiExtension.of(view.field("condition").orElseThrow())
                        .get("identifiability")),
                () -> assertEquals(List.of("category", "personal", "confidentiality", "integrity", "secret"),
                        List.copyOf(OpenApiExtension.of(view.field("page").orElseThrow()).keySet())));
    }

    @Test
    @DisplayName("a class gives one extension per classified field, as maps or as JSON, like its stored fields")
    void classAndInventory() {
        ClassClassification view = classifier.classify(StudentView.class);
        DataInventory inventory = classifier.inventory(List.of(StudentView.class), type -> "students");
        Map<String, Map<String, Object>> extensions = OpenApiExtension.of(view);
        assertAll(
                () -> assertEquals(List.of("email", "condition", "domain", "page"), List.copyOf(extensions.keySet())),
                () -> assertEquals(extensions.get("email"), extensions.get("domain")),
                () -> assertTrue(OpenApiExtension.toJson(view).startsWith("{\"email\":{\"category\":"
                        + "\"user.contact.email\",\"personal\":true,"), OpenApiExtension.toJson(view)),
                () -> {
                    for (FieldClassification field : view.fields()) {
                        assertEquals(OpenApiExtension.of(field), OpenApiExtension.of(inventory.field("students",
                                field.path()).orElseThrow(), taxonomy), field.path());
                    }
                },
                () -> assertEquals(Map.of(), OpenApiExtension.of(inventory.field("students", "links").orElseThrow(),
                        taxonomy)));
    }

    @Test
    @DisplayName("a class with classification errors is not described")
    void errors() {
        @SuppressWarnings("unused")
        class Broken {
            private String unclassified;
        }
        assertThrows(IllegalArgumentException.class, () -> OpenApiExtension.of(classifier.classify(Broken.class)));
    }
}
