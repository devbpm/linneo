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
package io.github.devbpm.linneo.export.postgresql;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.annotation.Exempt;
import io.github.devbpm.linneo.classify.ClassClassification;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.DisclosurePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresAnonymizerTest {

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final Classifier classifier = new Classifier(taxonomy);
    private final PostgresAnonymizer anonymizer = PostgresAnonymizer.of(DisclosurePolicy.forLogs());

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Student {
        @Category(Categories.USER_CONTACT_EMAIL)
        private String email;
        @Category(Categories.USER_NAME_FIRST)
        private String firstName;
        @Category(Categories.USER_HEALTH_AND_MEDICAL)
        private String condition;
        @Category(Categories.USER_EDUCATION_GRADE)
        private BigDecimal mark;
        @Category(Categories.USER_AUTHORIZATION_PASSWORD)
        private String password;
        private long version;
        @Exempt("rendering cache")
        private String cache;
    }

    @Test
    @DisplayName("masked fields get a fake value of their kind or none, omitted ones none, the others no rule")
    void labels() {
        assertEquals(List.of(
                        "SECURITY LABEL FOR anon ON COLUMN students.email IS 'MASKED WITH FUNCTION"
                                + " anon.dummy_safe_email()';",
                        "SECURITY LABEL FOR anon ON COLUMN students.firstName IS 'MASKED WITH FUNCTION"
                                + " anon.dummy_first_name()';",
                        "SECURITY LABEL FOR anon ON COLUMN students.condition IS 'MASKED WITH VALUE NULL';",
                        "SECURITY LABEL FOR anon ON COLUMN students.mark IS 'MASKED WITH VALUE NULL';",
                        "SECURITY LABEL FOR anon ON COLUMN students.password IS 'MASKED WITH VALUE NULL';",
                        "SECURITY LABEL FOR anon ON COLUMN students.version IS NULL;",
                        "SECURITY LABEL FOR anon ON COLUMN students.cache IS NULL;"),
                anonymizer.labels(classifier.classify(Student.class), path -> "students." + path));
    }

    @Test
    @DisplayName("a mask declared for a category serves the categories below it, and quotes are escaped")
    void declaredMasks() {
        PostgresAnonymizer custom = anonymizer.withMask(Categories.USER_EDUCATION, "anon.random_int_between(0, 10)")
                .withMask(Categories.USER_CONTACT_EMAIL, "anon.partial(email, 1, '***', 4)");
        ClassClassification student = classifier.classify(Student.class);
        assertAll(
                () -> assertEquals("MASKED WITH FUNCTION anon.random_int_between(0, 10)",
                        custom.rule(student.field("mark").orElseThrow())),
                () -> assertEquals("SECURITY LABEL FOR anon ON COLUMN public.students.email IS 'MASKED WITH FUNCTION"
                        + " anon.partial(email, 1, ''***'', 4)';",
                        custom.label("public.students.email", student.field("email").orElseThrow())),
                () -> assertTrue(anonymizer.masks().containsKey(Categories.USER_NAME)),
                () -> assertEquals(null, anonymizer.rule(student.field("version").orElseThrow())));
    }

    @Test
    @DisplayName("stored fields get the same rules, and a column that is not a qualified name is refused")
    void inventoryAndColumns() {
        DataInventory inventory = classifier.inventory(List.of(Student.class), type -> "students");
        ClassClassification student = classifier.classify(Student.class);
        assertAll(
                () -> assertEquals(Set.copyOf(anonymizer.labels(student, path -> "students." + path)),
                        Set.copyOf(anonymizer.labels(inventory, "students", taxonomy, path -> "students." + path))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> anonymizer.label("email", student.field("email").orElseThrow())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> anonymizer.label("students.email; DROP TABLE students", student.field("email")
                                .orElseThrow())),
                () -> assertEquals("SECURITY LABEL FOR anon ON COLUMN \"Students\".\"e-mail\" IS 'MASKED WITH"
                        + " FUNCTION anon.dummy_safe_email()';",
                        anonymizer.label("\"Students\".\"e-mail\"", student.field("email").orElseThrow())));
    }
}
