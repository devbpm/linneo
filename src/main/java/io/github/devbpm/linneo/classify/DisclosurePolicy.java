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

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.classify.ClassClassification.Exemption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * How the values of classified fields may leave the system through one channel, such as a log, an export, a copy
 * for a test environment or an API: shown as they are, masked, or left out. Linneo decides; the application masks or
 * omits the values, since Linneo never reads them.
 * <p>
 * A policy is a set of thresholds in the {@linkplain Protection partial order of protection}: a field whose
 * protection {@linkplain Protection#covers(Protection) covers} a threshold gets at least that threshold's treatment,
 * and the strictest treatment applies. A field that needs more protection therefore never gets a laxer treatment
 * than one that needs less, whatever thresholds a policy has, and a field that holds a secret is left out by every
 * policy, because a secret is never read back, exported or published.
 * <pre>{@code
 * DisclosurePolicy export = DisclosurePolicy.builder()
 *         .mask(Protection.of(LegalNature.ORDINARY, Confidentiality.PUBLIC, Integrity.LOW, false))  // personal data
 *         .omit(Protection.of(LegalNature.SPECIAL_CATEGORY, Confidentiality.PUBLIC, Integrity.LOW, false))
 *         .build();
 * Map<String, Treatment> inLogs = DisclosurePolicy.forLogs().treatments(classifier.classify(Student.class));
 * }</pre>
 * Instances are immutable and thread-safe.
 *
 * @since 0.1.0
 */
public final class DisclosurePolicy {

    /**
     * What happens to a value, from the most open to the strictest.
     *
     * @since 0.1.0
     */
    public enum Treatment {

        /**
         * Shown as it is.
         */
        CLEAR,

        /**
         * Shown in a form that does not reveal it: replaced by asterisks, truncated, hashed or replaced by a pseudonym,
         * as the application decides.
         */
        MASK,

        /**
         * Left out.
         */
        OMIT
    }

    private static final Protection PERSONAL = threshold(LegalNature.ORDINARY, Confidentiality.PUBLIC);
    private static final DisclosurePolicy LOGS = builder()
            .mask(PERSONAL)
            .mask(threshold(LegalNature.NONE, Confidentiality.HIGH))
            .omit(threshold(LegalNature.SPECIAL_CATEGORY, Confidentiality.PUBLIC))
            .omit(threshold(LegalNature.CRIMINAL_OFFENCE, Confidentiality.PUBLIC))
            .build();

    private final List<Protection> masked;
    private final List<Protection> omitted;

    private DisclosurePolicy(List<Protection> masked, List<Protection> omitted) {
        this.masked = List.copyOf(masked);
        this.omitted = List.copyOf(omitted);
    }

    /**
     * A policy for logs, traces and error reports: personal data and data of {@code HIGH} confidentiality masked;
     * special category data, criminal offence data and secrets left out. Anonymized and aggregated values of a
     * personal category are not personal data and follow their confidentiality.
     *
     * @return the policy
     */
    public static DisclosurePolicy forLogs() {
        return LOGS;
    }

    /**
     * Starts a policy with no threshold: only secrets are left out.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The thresholds from which a value is masked.
     *
     * @return an immutable list
     */
    public List<Protection> masked() {
        return masked;
    }

    /**
     * The thresholds from which a value is left out, besides secrets.
     *
     * @return an immutable list
     */
    public List<Protection> omitted() {
        return omitted;
    }

    /**
     * The treatment of a value that requires a protection.
     *
     * @param protection the protection
     * @return the strictest treatment whose threshold the protection covers; {@link Treatment#OMIT} for a secret
     */
    public Treatment treatment(Protection protection) {
        Objects.requireNonNull(protection, "protection");
        if (protection.secret() || omitted.stream().anyMatch(protection::covers)) {
            return Treatment.OMIT;
        }
        return masked.stream().anyMatch(protection::covers) ? Treatment.MASK : Treatment.CLEAR;
    }

    /**
     * The treatment of the values of a field.
     *
     * @param field the classification of the field
     * @return the treatment of its {@linkplain FieldClassification#protection() protection}
     */
    public Treatment treatment(FieldClassification field) {
        return treatment(field.protection());
    }

    /**
     * The treatment of the values of a stored field, resolved with a taxonomy.
     *
     * @param field a field of a {@link DataInventory}
     * @param taxonomy the taxonomy to resolve its key with
     * @return the treatment of its {@linkplain InventoryField#protection(Taxonomy) protection}; {@link Treatment#CLEAR}
     *         for an exempted field, which holds no data
     * @throws TaxonomyException if the taxonomy does not contain the field's key
     */
    public Treatment treatment(InventoryField field, Taxonomy taxonomy) {
        return field.isExempt() ? Treatment.CLEAR : treatment(field.protection(taxonomy));
    }

    /**
     * The treatment of every field of a class, as an application builds a masker for the class once.
     *
     * @param classification the classification of the class, without errors
     * @return the treatments by path: the classified fields in order, then the exempted ones, which hold no data and
     *         are {@link Treatment#CLEAR}
     * @throws IllegalArgumentException if the classification has errors: a field with an error has no treatment, and
     *         leaving it out of the map would let it through
     */
    public Map<String, Treatment> treatments(ClassClassification classification) {
        if (!classification.isValid()) {
            throw new IllegalArgumentException(classification.type().getName() + " has classification errors, so"
                    + " some fields have no treatment: " + classification.errors());
        }
        Map<String, Treatment> treatments = new LinkedHashMap<>();
        for (FieldClassification field : classification.fields()) {
            treatments.put(field.path(), treatment(field));
        }
        for (Exemption exemption : classification.exempted()) {
            treatments.put(exemption.path(), Treatment.CLEAR);
        }
        return Collections.unmodifiableMap(treatments);
    }

    @Override
    public String toString() {
        return "DisclosurePolicy[mask from " + masked + ", omit from " + omitted + " and secrets]";
    }

    private static Protection threshold(LegalNature nature, Confidentiality confidentiality) {
        return Protection.of(nature, confidentiality, Integrity.LOW, false);
    }

    /**
     * Builds a {@link DisclosurePolicy}.
     *
     * @since 0.1.0
     */
    public static final class Builder {

        private final List<Protection> masked = new ArrayList<>();
        private final List<Protection> omitted = new ArrayList<>();

        private Builder() {
        }

        /**
         * Masks every value whose protection covers a threshold.
         *
         * @param atLeast the threshold, such as {@code Protection.of(LegalNature.ORDINARY, Confidentiality.PUBLIC,
         *        Integrity.LOW, false)} for any personal data
         * @return this builder
         */
        public Builder mask(Protection atLeast) {
            masked.add(Objects.requireNonNull(atLeast, "atLeast"));
            return this;
        }

        /**
         * Leaves out every value whose protection covers a threshold.
         *
         * @param atLeast the threshold, such as {@code Protection.of(LegalNature.SPECIAL_CATEGORY,
         *        Confidentiality.PUBLIC, Integrity.LOW, false)} for special category data
         * @return this builder
         */
        public Builder omit(Protection atLeast) {
            omitted.add(Objects.requireNonNull(atLeast, "atLeast"));
            return this;
        }

        /**
         * Builds the policy.
         *
         * @return an immutable policy
         */
        public DisclosurePolicy build() {
            return new DisclosurePolicy(masked, omitted);
        }
    }
}
