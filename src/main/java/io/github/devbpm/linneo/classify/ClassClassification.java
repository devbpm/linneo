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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The classification of a class: one entry per field it holds, including inherited fields, plus the problems found.
 *
 * @param type the class
 * @param fields the classified fields, superclass fields first, in declaration order
 * @param violations the problems found; a field with an error may be missing from {@code fields}
 * @since 0.1.0
 */
public record ClassClassification(Class<?> type, List<FieldClassification> fields, List<Violation> violations) {

    /**
     * Checks the components and makes the lists immutable.
     *
     * @param type the class
     * @param fields the classified fields
     * @param violations the problems found
     */
    public ClassClassification {
        Objects.requireNonNull(type, "type");
        fields = List.copyOf(fields);
        violations = List.copyOf(violations);
    }

    /**
     * Whether the class is fully and correctly classified.
     *
     * @return {@code true} if there is no {@linkplain Violation#isError() error}; warnings are allowed
     */
    public boolean isValid() {
        return violations.stream().noneMatch(Violation::isError);
    }

    /**
     * The errors found.
     *
     * @return the violations of severity {@link Violation.Severity#ERROR}
     */
    public List<Violation> errors() {
        return violations.stream().filter(Violation::isError).toList();
    }

    /**
     * The fields that hold personal data.
     *
     * @return the fields whose {@link FieldClassification#isPersonal()} is {@code true}
     */
    public List<FieldClassification> personalFields() {
        return fields.stream().filter(FieldClassification::isPersonal).toList();
    }

    /**
     * Whether any field holds personal data.
     *
     * @return {@code true} if {@link #personalFields()} is not empty
     */
    public boolean holdsPersonalData() {
        return fields.stream().anyMatch(FieldClassification::isPersonal);
    }

    /**
     * The classification of one field.
     *
     * @param name the field name
     * @return the classification, or empty if the field is unknown, excluded or has an error
     */
    public Optional<FieldClassification> field(String name) {
        return fields.stream().filter(f -> f.field().equals(name)).findFirst();
    }
}
