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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The part of a processing activity that the controller's data protection function decides and keeps up to date:
 * why the processing is lawful, how long the data is kept, who receives it, where it goes and the impact assessment.
 * <p>
 * It is separate from what the code of the application knows for certain, the scope of the
 * {@link ProcessingActivity} (its purposes, data subjects and personal data), because the two have different owners.
 * An application can declare initial values in code, keep the governance its data protection officer edits in its
 * own storage, and combine both with {@link ProcessingActivity#withGovernance(Governance)} before
 * {@linkplain ProcessingChecker#check(ProcessingActivity) checking} the activity, for example when the officer saves
 * a change. The problems the checker finds in this part are those whose
 * {@link ProcessingViolation.Element#isGovernance() element is governance}.
 *
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
 * @param riskFactors what the officer knows of the activity that the code cannot, for the
 *        {@linkplain ImpactScreening screening} of the need for an impact assessment
 * @since 0.1.0
 */
public record Governance(LegalBasis legalBasis, String legitimateInterest,
                         SpecialCategoryCondition specialCategoryCondition, CriminalDataBasis criminalDataBasis,
                         String legalReference, String retention, List<String> recipients, List<Transfer> transfers,
                         String impactAssessment, Set<ImpactScreening.Factor> riskFactors) {

    /**
     * Governance with nothing declared yet.
     */
    public static final Governance NONE = builder().build();

    /**
     * Makes the lists immutable and turns blank texts into {@code null}.
     *
     * @param legalBasis the lawful basis
     * @param legitimateInterest the legitimate interest
     * @param specialCategoryCondition the article 9.2 condition
     * @param criminalDataBasis the article 10 basis
     * @param legalReference the law
     * @param retention the retention
     * @param recipients the recipients
     * @param transfers the transfers
     * @param impactAssessment the impact assessment reference
     * @param riskFactors the declared risk factors; {@code null} means none
     */
    public Governance {
        legitimateInterest = blankToNull(legitimateInterest);
        legalReference = blankToNull(legalReference);
        retention = blankToNull(retention);
        recipients = recipients == null ? List.of() : List.copyOf(recipients);
        transfers = transfers == null ? List.of() : List.copyOf(transfers);
        impactAssessment = blankToNull(impactAssessment);
        riskFactors = riskFactors == null || riskFactors.isEmpty() ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(riskFactors));
    }

    /**
     * Starts declaring governance.
     *
     * @return an empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder that starts from this governance, to change part of it.
     *
     * @return a builder with these values
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.legalBasis = legalBasis;
        builder.legitimateInterest = legitimateInterest;
        builder.specialCategoryCondition = specialCategoryCondition;
        builder.criminalDataBasis = criminalDataBasis;
        builder.legalReference = legalReference;
        builder.retention = retention;
        builder.recipients.addAll(recipients);
        builder.transfers.addAll(transfers);
        builder.impactAssessment = impactAssessment;
        builder.riskFactors.addAll(riskFactors);
        return builder;
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /**
     * Declares a {@link Governance} step by step. List methods add to what was given before.
     *
     * @since 0.1.0
     */
    public static final class Builder {

        private LegalBasis legalBasis;
        private String legitimateInterest;
        private SpecialCategoryCondition specialCategoryCondition;
        private CriminalDataBasis criminalDataBasis;
        private String legalReference;
        private String retention;
        private final List<String> recipients = new ArrayList<>();
        private final List<Transfer> transfers = new ArrayList<>();
        private String impactAssessment;
        private final Set<ImpactScreening.Factor> riskFactors = EnumSet.noneOf(ImpactScreening.Factor.class);

        private Builder() {
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
         * Adds risk factors the officer knows of, for the screening of the need for an impact assessment.
         *
         * @param factors the factors, such as {@link ImpactScreening.Factor#LARGE_SCALE}
         * @return this builder
         */
        public Builder riskFactors(ImpactScreening.Factor... factors) {
            this.riskFactors.addAll(Arrays.asList(factors));
            return this;
        }

        /**
         * Creates the governance.
         *
         * @return the governance
         */
        public Governance build() {
            return new Governance(legalBasis, legitimateInterest, specialCategoryCondition, criminalDataBasis,
                    legalReference, retention, recipients, transfers, impactAssessment, riskFactors);
        }
    }
}
