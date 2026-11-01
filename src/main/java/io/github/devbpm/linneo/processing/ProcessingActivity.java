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
 * An activity has two parts with different owners. Its scope is what the code of the application knows for
 * certain: the purposes it serves, the data subjects and the personal data it processes. Purposes, data subjects and
 * data categories are keys of the taxonomy; the personal data can be listed as category keys, as classified model
 * classes ({@link #entities()}), whose personal fields contribute their categories, as the keys of objects of a
 * {@linkplain io.github.devbpm.linneo.classify.DataInventory data inventory} ({@link #objects()}), or all of them. Its
 * {@linkplain Governance governance} is what the data protection function decides: the lawful bases, retention,
 * recipients, transfers and impact assessment. Code may give initial values for the governance with the builder;
 * an application that lets its data protection officer edit them combines the stored values with
 * {@link #withGovernance(Governance)}.
 * <p>
 * Build one with {@link #builder(String, String)} and check it with a {@link ProcessingChecker}; the activity itself
 * only holds what was declared.
 *
 * @param key a stable identifier, unique within the record: letters, digits, dots, underscores and hyphens, starting
 *        with a letter or digit
 * @param name a human-readable name
 * @param description what the activity does, or {@code null}
 * @param purposes keys of the data uses it serves (article 30.1.b)
 * @param dataSubjects keys of the categories of data subjects (article 30.1.c)
 * @param dataCategories keys of the categories of personal data it processes beyond those of {@code entities}
 * @param entities classified model classes whose personal fields it processes
 * @param objects keys of objects of the data inventory the checker is given, whose personal fields it processes
 * @param governance the lawful bases, retention, recipients, transfers and impact assessment
 * @since 0.1.0
 */
public record ProcessingActivity(String key, String name, String description, List<String> purposes,
                                 List<String> dataSubjects, List<String> dataCategories, List<Class<?>> entities,
                                 List<String> objects, Governance governance) {

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*+");

    /**
     * Checks the identity, makes the lists immutable and turns a blank description into {@code null}.
     *
     * @param key the key
     * @param name the name
     * @param description the description
     * @param purposes the data use keys
     * @param dataSubjects the data subject keys
     * @param dataCategories the data category keys
     * @param entities the model classes
     * @param objects the object keys
     * @param governance the governance; {@code null} means {@link Governance#NONE}
     */
    public ProcessingActivity {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(name, "name");
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid processing activity key '" + key + "': use letters, digits,"
                    + " dots, underscores and hyphens, starting with a letter or digit");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("The processing activity '" + key + "' needs a name");
        }
        name = name.strip();
        description = Governance.blankToNull(description);
        purposes = List.copyOf(purposes);
        dataSubjects = List.copyOf(dataSubjects);
        dataCategories = List.copyOf(dataCategories);
        entities = List.copyOf(entities);
        objects = objects == null ? List.of() : List.copyOf(objects);
        governance = governance == null ? Governance.NONE : governance;
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

    /**
     * The same activity with another governance, such as the one the data protection officer keeps up to date. The
     * scope declared in code is kept as is.
     *
     * @param governance the governance that replaces the current one
     * @return a new activity
     */
    public ProcessingActivity withGovernance(Governance governance) {
        return new ProcessingActivity(key, name, description, purposes, dataSubjects, dataCategories, entities,
                objects, Objects.requireNonNull(governance, "governance"));
    }

    /**
     * Declares a {@link ProcessingActivity} step by step. List methods add to what was given before. The governance
     * methods give initial values that {@link #governance(Governance)} or
     * {@link ProcessingActivity#withGovernance(Governance)} can replace.
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
        private final List<String> objects = new ArrayList<>();
        private Governance.Builder governance = Governance.builder();

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
         * Adds objects of a data inventory, by key; the categories of their personal fields are processed by the
         * activity. The {@link ProcessingChecker} must be created with that inventory.
         *
         * @param keys object keys
         * @return this builder
         */
        public Builder objects(String... keys) {
            objects.addAll(Arrays.asList(keys));
            return this;
        }

        /**
         * Replaces the governance declared so far.
         *
         * @param governance the governance
         * @return this builder
         */
        public Builder governance(Governance governance) {
            this.governance = Objects.requireNonNull(governance, "governance").toBuilder();
            return this;
        }

        /**
         * Declares the lawful basis; see {@link Governance.Builder#legalBasis(LegalBasis)}.
         *
         * @param basis the basis
         * @return this builder
         */
        public Builder legalBasis(LegalBasis basis) {
            governance.legalBasis(basis);
            return this;
        }

        /**
         * Declares legitimate interests as the lawful basis, with the interest pursued; see
         * {@link Governance.Builder#legitimateInterest(String)}.
         *
         * @param interest the interest of the controller or third party
         * @return this builder
         */
        public Builder legitimateInterest(String interest) {
            governance.legitimateInterest(interest);
            return this;
        }

        /**
         * Declares the article 9.2 condition for special categories of data.
         *
         * @param condition the condition
         * @return this builder
         */
        public Builder specialCategoryCondition(SpecialCategoryCondition condition) {
            governance.specialCategoryCondition(condition);
            return this;
        }

        /**
         * Declares what allows processing criminal offence data.
         *
         * @param basis the article 10 basis
         * @return this builder
         */
        public Builder criminalDataBasis(CriminalDataBasis basis) {
            governance.criminalDataBasis(basis);
            return this;
        }

        /**
         * Names the Union or Member State law the bases rest on.
         *
         * @param reference the law and its provision
         * @return this builder
         */
        public Builder legalReference(String reference) {
            governance.legalReference(reference);
            return this;
        }

        /**
         * Declares the envisaged time limit for erasure.
         *
         * @param retention the retention period or rule
         * @return this builder
         */
        public Builder retention(String retention) {
            governance.retention(retention);
            return this;
        }

        /**
         * Adds categories of recipients.
         *
         * @param recipients the recipients
         * @return this builder
         */
        public Builder recipients(String... recipients) {
            governance.recipients(recipients);
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
            governance.transfer(destination, safeguard);
            return this;
        }

        /**
         * Refers to the data protection impact assessment.
         *
         * @param reference where the assessment is, or its identifier
         * @return this builder
         */
        public Builder impactAssessment(String reference) {
            governance.impactAssessment(reference);
            return this;
        }

        /**
         * Adds initial risk factors of the governance; see {@link Governance.Builder#riskFactors}.
         *
         * @param factors the factors
         * @return this builder
         */
        public Builder riskFactors(ImpactScreening.Factor... factors) {
            governance.riskFactors(factors);
            return this;
        }

        /**
         * Creates the activity.
         *
         * @return the activity
         */
        public ProcessingActivity build() {
            return new ProcessingActivity(key, name, description, purposes, dataSubjects, dataCategories, entities,
                    objects, governance.build());
        }
    }
}
