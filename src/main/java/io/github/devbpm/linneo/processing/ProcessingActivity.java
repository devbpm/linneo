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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A processing activity of the controller, as declared in its record of processing activities (GDPR article 30.1).
 * <p>
 * Purposes, data subjects and data categories are keys of the taxonomy. The personal data processed can be listed
 * as category keys, as classified model classes ({@link #entities()}), whose personal fields contribute their
 * categories, or both. Build one with {@link #builder(String, String)} and check it with a
 * {@link ProcessingChecker}; the activity itself only holds what was declared.
 *
 * @param key a stable identifier, unique within the record: letters, digits, dots, underscores and hyphens, starting
 *        with a letter or digit
 * @param name a human-readable name
 * @param description what the activity does, or {@code null}
 * @param purposes keys of the data uses it serves (article 30.1.b)
 * @param dataSubjects keys of the categories of data subjects (article 30.1.c)
 * @param dataCategories keys of the categories of personal data it processes beyond those of {@code entities}
 * @param entities classified model classes whose personal fields it processes
 * @param legalBasis the lawful basis (article 6.1), or {@code null} if not declared
 * @param legitimateInterest the interest pursued, when the basis is {@link LegalBasis#LEGITIMATE_INTERESTS}, or
 *        {@code null}
 * @param specialCategoryCondition the article 9.2 condition for special categories of data, or {@code null}
 * @param criminalDataBasis what allows processing criminal offence data (article 10), or {@code null}
 * @param legalReference the Union or Member State law the bases rest on, or {@code null}
 * @param retention the envisaged time limit for erasure (article 30.1.f), or {@code null}
 * @param recipients the categories of recipients (article 30.1.d)
 * @param transfers the transfers to third countries or international organisations (article 30.1.e)
 * @param impactAssessment a reference to the data protection impact assessment (article 35), or {@code null}
 * @since 0.1.0
 */
public record ProcessingActivity(String key, String name, String description, List<String> purposes,
                                 List<String> dataSubjects, List<String> dataCategories, List<Class<?>> entities,
                                 LegalBasis legalBasis, String legitimateInterest,
                                 SpecialCategoryCondition specialCategoryCondition,
                                 CriminalDataBasis criminalDataBasis, String legalReference, String retention,
                                 List<String> recipients, List<Transfer> transfers, String impactAssessment) {

    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    /**
     * Checks the identity, makes the lists immutable and turns blank texts into {@code null}.
     *
     * @param key the key
     * @param name the name
     * @param description the description
     * @param purposes the data use keys
     * @param dataSubjects the data subject keys
     * @param dataCategories the data category keys
     * @param entities the model classes
     * @param legalBasis the lawful basis
     * @param legitimateInterest the legitimate interest
     * @param specialCategoryCondition the article 9.2 condition
     * @param criminalDataBasis the article 10 basis
     * @param legalReference the law
     * @param retention the retention
     * @param recipients the recipients
     * @param transfers the transfers
     * @param impactAssessment the impact assessment reference
     */
    public ProcessingActivity {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(name, "name");
        if (!KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid processing activity key '" + key + "': use letters, digits,"
                    + " dots, underscores and hyphens, starting with a letter or digit");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("The processing activity '" + key + "' needs a name");
        }
        name = name.strip();
        description = blankToNull(description);
        purposes = List.copyOf(purposes);
        dataSubjects = List.copyOf(dataSubjects);
        dataCategories = List.copyOf(dataCategories);
        entities = List.copyOf(entities);
        legitimateInterest = blankToNull(legitimateInterest);
        legalReference = blankToNull(legalReference);
        retention = blankToNull(retention);
        recipients = List.copyOf(recipients);
        transfers = List.copyOf(transfers);
        impactAssessment = blankToNull(impactAssessment);
    }

    /**
     * Starts the declaration of a processing activity.
     *
     * @param key a stable identifier, unique within the record
     * @param name a human-readable name
     * @return a builder
     */
    public static Builder builder(String key, String name) {
        return new Builder(key, name);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /**
     * Declares a {@link ProcessingActivity} step by step. List methods add to what was given before.
     *
     * @since 0.1.0
     */
    public static final class Builder {

        private final String key;
        private final String name;
        private String description;
        private final List<String> purposes = new ArrayList<>();
        private final List<String> dataSubjects = new ArrayList<>();
        private final List<String> dataCategories = new ArrayList<>();
        private final List<Class<?>> entities = new ArrayList<>();
        private LegalBasis legalBasis;
        private String legitimateInterest;
        private SpecialCategoryCondition specialCategoryCondition;
        private CriminalDataBasis criminalDataBasis;
        private String legalReference;
        private String retention;
        private final List<String> recipients = new ArrayList<>();
        private final List<Transfer> transfers = new ArrayList<>();
        private String impactAssessment;

        private Builder(String key, String name) {
            this.key = key;
            this.name = name;
        }

        /**
         * Describes what the activity does.
         *
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Adds purposes, preferably constants of {@link io.github.devbpm.linneo.DataUses}.
         *
         * @param keys data use keys
         * @return this builder
         */
        public Builder purposes(String... keys) {
            purposes.addAll(Arrays.asList(keys));
            return this;
        }

        /**
         * Adds categories of data subjects, preferably constants of {@link io.github.devbpm.linneo.DataSubjects}.
         *
         * @param keys data subject keys
         * @return this builder
         */
        public Builder dataSubjects(String... keys) {
            dataSubjects.addAll(Arrays.asList(keys));
            return this;
        }

        /**
         * Adds categories of personal data that no listed entity declares, preferably constants of
         * {@link io.github.devbpm.linneo.Categories}.
         *
         * @param keys data category keys
         * @return this builder
         */
        public Builder dataCategories(String... keys) {
            dataCategories.addAll(Arrays.asList(keys));
            return this;
        }

        /**
         * Adds classified model classes; the categories of their personal fields are processed by the activity.
         *
         * @param types annotated classes
         * @return this builder
         */
        public Builder entities(Class<?>... types) {
            entities.addAll(Arrays.asList(types));
            return this;
        }

        /**
         * Declares the lawful basis.
         *
         * @param basis the basis
         * @return this builder
         */
        public Builder legalBasis(LegalBasis basis) {
            this.legalBasis = basis;
            return this;
        }

        /**
         * Declares legitimate interests as the lawful basis, with the interest pursued.
         *
         * @param interest the interest of the controller or third party
         * @return this builder
         */
        public Builder legitimateInterest(String interest) {
            this.legalBasis = LegalBasis.LEGITIMATE_INTERESTS;
            this.legitimateInterest = interest;
            return this;
        }

        /**
         * Declares the article 9.2 condition for special categories of data.
         *
         * @param condition the condition
         * @return this builder
         */
        public Builder specialCategoryCondition(SpecialCategoryCondition condition) {
            this.specialCategoryCondition = condition;
            return this;
        }

        /**
         * Declares what allows processing criminal offence data.
         *
         * @param basis the article 10 basis
         * @return this builder
         */
        public Builder criminalDataBasis(CriminalDataBasis basis) {
            this.criminalDataBasis = basis;
            return this;
        }

        /**
         * Names the Union or Member State law the bases rest on.
         *
         * @param reference the law and its provision
         * @return this builder
         */
        public Builder legalReference(String reference) {
            this.legalReference = reference;
            return this;
        }

        /**
         * Declares the envisaged time limit for erasure.
         *
         * @param retention the retention period or rule
         * @return this builder
         */
        public Builder retention(String retention) {
            this.retention = retention;
            return this;
        }

        /**
         * Adds categories of recipients.
         *
         * @param recipients the recipients
         * @return this builder
         */
        public Builder recipients(String... recipients) {
            this.recipients.addAll(Arrays.asList(recipients));
            return this;
        }

        /**
         * Adds a transfer to a third country or an international organisation.
         *
         * @param destination the destination
         * @param safeguard what allows the transfer
         * @return this builder
         */
        public Builder transfer(String destination, TransferSafeguard safeguard) {
            transfers.add(new Transfer(destination, safeguard));
            return this;
        }

        /**
         * Refers to the data protection impact assessment.
         *
         * @param reference where the assessment is, or its identifier
         * @return this builder
         */
        public Builder impactAssessment(String reference) {
            this.impactAssessment = reference;
            return this;
        }

        /**
         * Creates the activity.
         *
         * @return the activity
         */
        public ProcessingActivity build() {
            return new ProcessingActivity(key, name, description, purposes, dataSubjects, dataCategories, entities,
                    legalBasis, legitimateInterest, specialCategoryCondition, criminalDataBasis, legalReference,
                    retention, recipients, transfers, impactAssessment);
        }
    }
}
