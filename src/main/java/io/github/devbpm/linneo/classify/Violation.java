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

import java.util.Objects;

/**
 * A problem found while classifying a class.
 *
 * @param type the class being classified
 * @param field the field involved, or {@code null} for a problem with a class annotation
 * @param severity whether it fails verification
 * @param code what kind of problem it is
 * @param message a description naming the key or annotation involved
 * @since 0.1.0
 */
public record Violation(Class<?> type, String field, Severity severity, Code code, String message) {

    /**
     * Whether a violation fails {@link Classifier#verify(java.util.Collection)}.
     */
    public enum Severity {

        /**
         * The classification is wrong or missing; verification fails.
         */
        ERROR,

        /**
         * The classification works but should be updated; verification passes.
         */
        WARNING
    }

    /**
     * The kinds of problem.
     */
    public enum Code {

        /**
         * A field has no category: no annotation of its own and no class default.
         */
        UNCLASSIFIED_FIELD,

        /**
         * A key is not part of the taxonomy.
         */
        UNKNOWN_CATEGORY,

        /**
         * A key is deprecated; the field is classified with its replacement. A warning.
         */
        DEPRECATED_CATEGORY,

        /**
         * A class default names a personal category; personal data must be declared field by field.
         */
        PERSONAL_DEFAULT,

        /**
         * A reason, or what decides a per-row category, is blank.
         */
        MISSING_REASON,

        /**
         * A field carries annotations that exclude each other.
         */
        CONFLICTING_ANNOTATIONS,

        /**
         * A field declares a level lower than its category's.
         */
        LOWERED_LEVEL,

        /**
         * An optional element of {@link io.github.devbpm.linneo.annotation.Category @Category} has more than one value.
         */
        AMBIGUOUS_VALUE,

        /**
         * A field declares an identifiability for a category that is not personal.
         */
        IDENTIFIABILITY_NOT_APPLICABLE
    }

    /**
     * Checks the mandatory components.
     *
     * @param type the class
     * @param field the field or {@code null}
     * @param severity the severity
     * @param code the code
     * @param message the message
     */
    public Violation {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }

    /**
     * Whether the violation fails verification.
     *
     * @return {@code true} for an {@link Severity#ERROR}
     */
    public boolean isError() {
        return severity == Severity.ERROR;
    }

    @Override
    public String toString() {
        return severity + " " + code + " " + type.getName() + (field == null ? "" : "." + field) + ": " + message;
    }
}
