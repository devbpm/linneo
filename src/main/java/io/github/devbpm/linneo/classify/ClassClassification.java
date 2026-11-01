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

import io.github.devbpm.linneo.Protection;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The classification of a class: one entry per field it holds, including inherited fields, the fields excluded with
 * {@link io.github.devbpm.linneo.annotation.Exempt @Exempt}, and the problems found.
 * <p>
 * Fields are listed superclass first and, within a class, in the order the JVM reports them, which is usually the
 * declaration order but is not guaranteed. A field is identified by its {@linkplain FieldClassification#path() path},
 * never by its position.
 *
 * @param type the class
 * @param fields the classified fields
 * @param exempted the fields excluded from classification, with their reasons
 * @param violations the problems found; a field with an error may be missing from {@code fields}
 * @since 0.1.0
 */
public record ClassClassification(Class<?> type, List<FieldClassification> fields, List<Exemption> exempted,
                                  List<Violation> violations) {

    /**
     * Checks the components and makes the lists immutable.
     *
     * @param type the class
     * @param fields the classified fields
     * @param exempted the exempted fields
     * @param violations the problems found
     */
    public ClassClassification {
        Objects.requireNonNull(type, "type");
        fields = List.copyOf(fields);
        exempted = List.copyOf(exempted);
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
     * @param path the field path
     * @return the classification, or empty if the field is unknown, excluded or has an error
     */
    public Optional<FieldClassification> field(String path) {
        return fields.stream().filter(f -> f.path().equals(path)).findFirst();
    }

    /**
     * The protection a record of the class requires as a whole: the join of the protection of its fields. It is
     * what an export of whole records, or a column that stores the record serialized, needs.
     *
     * @return the least protection that covers every field, or {@link Protection#LOWEST} if there is none
     */
    public Protection ceiling() {
        return Protection.joinAll(fields.stream().map(FieldClassification::protection).toList());
    }

    /**
     * The protection a field requires together with everything below it: the field itself and every field whose
     * path starts with its path.
     *
     * @param path a field path
     * @return the least protection that covers the field and the fields below it, or {@link Protection#LOWEST} if
     *         there is none
     */
    public Protection ceilingOf(String path) {
        Objects.requireNonNull(path, "path");
        String prefix = path + ".";
        return Protection.joinAll(fields.stream()
                .filter(f -> f.path().equals(path) || f.path().startsWith(prefix))
                .map(FieldClassification::protection).toList());
    }

    /**
     * A field excluded from classification with {@link io.github.devbpm.linneo.annotation.Exempt @Exempt}. It is
     * listed so that excluding a field that used to be classified is visible.
     *
     * @param declaringClass the class that declares the field
     * @param path the field path
     * @param reason why the field holds no data of the model
     * @since 0.1.0
     */
    public record Exemption(Class<?> declaringClass, String path, String reason) {

        /**
         * Checks the components.
         *
         * @param declaringClass the declaring class
         * @param path the field path
         * @param reason the reason
         */
        public Exemption {
            Objects.requireNonNull(declaringClass, "declaringClass");
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(reason, "reason");
        }
    }
}
