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
package io.github.devbpm.linneo.processing;

import java.util.Objects;

/**
 * A problem found while checking a processing activity.
 *
 * @param activity the key of the activity
 * @param severity whether it fails verification
 * @param code what kind of problem it is
 * @param element the part of the activity the problem is in, which tells who should fix it
 * @param message a description naming the key or provision involved
 * @since 0.1.0
 */
public record ProcessingViolation(String activity, Severity severity, Code code, Element element,
                                  String message) {

    /**
     * Whether a violation fails {@link ProcessingChecker#verify(java.util.Collection)}.
     */
    public enum Severity {

        /**
         * The activity, as declared, would be unlawful or cannot be checked; verification fails.
         */
        ERROR,

        /**
         * The record of the activity is incomplete or should be reviewed; verification passes.
         */
        WARNING
    }

    /**
     * The kinds of problem. Errors first, then warnings.
     */
    public enum Code {

        /**
         * A purpose, data subject or data category key is not part of the taxonomy.
         */
        UNKNOWN_KEY,

        /**
         * The activity declares no purpose.
         */
        MISSING_PURPOSE,

        /**
         * The activity declares no category of data subjects.
         */
        MISSING_DATA_SUBJECTS,

        /**
         * The activity processes no personal data: no personal category and no entity with personal fields.
         */
        MISSING_PERSONAL_DATA,

        /**
         * A listed entity is not fully and correctly classified.
         */
        ENTITY_NOT_CLASSIFIED,

        /**
         * The activity processes personal data and declares no lawful basis (article 6.1).
         */
        MISSING_LEGAL_BASIS,

        /**
         * The activity processes special categories of data and declares no article 9.2 condition.
         */
        MISSING_SPECIAL_CATEGORY_CONDITION,

        /**
         * The activity processes criminal offence data and declares no article 10 basis.
         */
        MISSING_CRIMINAL_DATA_BASIS,

        /**
         * Under {@linkplain Jurisdiction#SPAIN Spanish law}, explicit consent is the only article 9.2 condition of
         * an activity that processes ideology, trade union membership, religion, sexual orientation, beliefs or
         * racial or ethnic origin, and consent alone does not lift the prohibition (LOPDGDD article 9.1).
         */
        CONSENT_NOT_SUFFICIENT,

        /**
         * The activity is a case of GDPR article 35.3, as its {@linkplain ImpactScreening screening} shows, and
         * refers to no impact assessment.
         */
        IMPACT_ASSESSMENT_REQUIRED,

        /**
         * Two activities of a record have the same key.
         */
        DUPLICATE_ACTIVITY,

        /**
         * A key is deprecated; the activity is checked with its replacement. A warning.
         */
        DEPRECATED_KEY,

        /**
         * A declared data category is not personal data and is left out of the record. A warning.
         */
        NOT_PERSONAL_DATA,

        /**
         * An article 9.2 condition or article 10 basis is declared for data the activity does not process, or a
         * legitimate interest is stated with another lawful basis. A warning.
         */
        UNNEEDED_CONDITION,

        /**
         * A basis that rests on Union or Member State law is declared without naming the law. A warning.
         */
        MISSING_LEGAL_REFERENCE,

        /**
         * Legitimate interests is the basis and the interest pursued is not stated. A warning.
         */
        MISSING_LEGITIMATE_INTEREST,

        /**
         * No time limit for erasure is declared (article 30.1.f). A warning.
         */
        MISSING_RETENTION,

        /**
         * The {@linkplain ImpactScreening screening} of the activity meets two criteria or more, so article 35
         * likely requires an impact assessment, and the activity refers to none. A warning.
         */
        IMPACT_ASSESSMENT_RECOMMENDED
    }

    /**
     * The part of a processing activity a problem is in. The scope ({@link #KEY}, {@link #PURPOSES},
     * {@link #DATA_SUBJECTS}, {@link #PERSONAL_DATA}) is declared in code, so a problem there is for developers and
     * should fail the build; the rest is the {@link Governance} the data protection function keeps, so a problem there
     * belongs on the screen where it is edited.
     *
     * @since 0.1.0
     */
    public enum Element {

        /**
         * The key that identifies the activity in the record.
         */
        KEY(false),

        /**
         * The purposes ({@link ProcessingActivity#purposes()}).
         */
        PURPOSES(false),

        /**
         * The data subjects ({@link ProcessingActivity#dataSubjects()}).
         */
        DATA_SUBJECTS(false),

        /**
         * The personal data: data categories and entities.
         */
        PERSONAL_DATA(false),

        /**
         * The lawful basis ({@link Governance#legalBasis()}).
         */
        LEGAL_BASIS(true),

        /**
         * The legitimate interest pursued ({@link Governance#legitimateInterest()}).
         */
        LEGITIMATE_INTEREST(true),

        /**
         * The article 9.2 conditions ({@link Governance#specialCategoryConditions()}).
         */
        SPECIAL_CATEGORY_CONDITION(true),

        /**
         * The article 10 basis ({@link Governance#criminalDataBasis()}).
         */
        CRIMINAL_DATA_BASIS(true),

        /**
         * The law the bases rest on ({@link Governance#legalReference()}).
         */
        LEGAL_REFERENCE(true),

        /**
         * The time limit for erasure ({@link Governance#retention()}).
         */
        RETENTION(true),

        /**
         * The impact assessment ({@link Governance#impactAssessment()}).
         */
        IMPACT_ASSESSMENT(true);

        private final boolean governance;

        Element(boolean governance) {
            this.governance = governance;
        }

        /**
         * Whether the element belongs to the {@link Governance} rather than to the scope declared in code.
         *
         * @return {@code true} for the elements of {@link Governance}
         */
        public boolean isGovernance() {
            return governance;
        }
    }

    /**
     * Checks the components.
     *
     * @param activity the activity key
     * @param severity the severity
     * @param code the code
     * @param element the element
     * @param message the message
     */
    public ProcessingViolation {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(element, "element");
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
        return severity + " " + code + " " + activity + ": " + message;
    }
}
