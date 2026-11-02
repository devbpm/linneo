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

import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.Trait;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.classify.InventoryField;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The severity of a personal data breach and what the GDPR asks of the controller, following the methodology of
 * ENISA, the European Union Agency for Cybersecurity ("Recommendations for a methodology of the assessment of severity
 * of personal data breaches", v1.0, 2013):
 * <pre>
 * severity = data processing context &times; ease of identification + circumstances of the breach
 * </pre>
 * Linneo scores the first two from the classification of the affected fields and the
 * {@linkplain io.github.devbpm.linneo.Trait traits} of their categories, and says which field sets each score: the
 * context is 4 for special category or criminal offence data, 3 for {@link Trait#FINANCIAL financial} data, 2 for
 * {@link Trait#BEHAVIOURAL behavioural} data, 1 for other personal data; the ease of identification is maximum when a
 * field is a {@link Trait#DIRECT_IDENTIFIER direct identifier} (a name, an identity document, biometric data, a
 * picture), significant with a {@link Trait#CONTACT_IDENTIFIER contact identifier} (contact or account data), limited
 * with an {@link Trait#INDIRECT_IDENTIFIER indirect identifier}, a pseudonymized value or any other personal data,
 * held about someone, and negligible when no field is personal data: a field declared anonymized or aggregated is not
 * personal data and counts for neither score. Both scores can be adjusted, as
 * the methodology foresees, when the context of the data calls for it. The circumstances (how confidentiality,
 * integrity and availability were lost, and malicious intent) are declared by whoever handles the breach.
 * <pre>{@code
 * BreachAssessment assessment = BreachAssessment.builder()
 *         .affected(classifier.classify(Student.class).fields())
 *         .confidentiality(Exposure.KNOWN_RECIPIENTS).individuals(340)
 *         .build();
 * assessment.severity();               // VERY_HIGH for a Student with health data: 4 x 1 + 0.25
 * assessment.notifyAuthority();        // true: within 72 hours (GDPR article 33.1)
 * assessment.communicateToSubjects();  // true: a high risk (article 34.1)
 * }</pre>
 * A low severity is read as a breach unlikely to result in a risk (article 33.1), which is recorded (article 33.5)
 * but not notified; a high or very high severity as a high risk, which is also communicated to the data subjects
 * (article 34.1) unless the data was unintelligible (article 34.3.a). A breach of confidentiality alone of data that
 * was unintelligible, such as encrypted with a key that was not compromised, is unlikely to result in a risk and is
 * not notified either (EDPB Guidelines 9/2022, and 01/2021 case 1). It is an orientation for the first hours, not a
 * decision: the controller assesses the risk of each breach. Instances are immutable.
 *
 * @since 0.1.0
 */
public final class BreachAssessment {

    /**
     * How easily the affected data identifies the people it is about, with ENISA's scores.
     *
     * @since 0.1.0
     */
    public enum Identification {

        /**
         * Extremely difficult to match the data to a person: 0.25.
         */
        NEGLIGIBLE(0.25),

        /**
         * Possible with some effort or access to other data: 0.5.
         */
        LIMITED(0.5),

        /**
         * Possible with little effort, through identifiers that reach the person: 0.75.
         */
        SIGNIFICANT(0.75),

        /**
         * Immediate: the data names the person: 1.
         */
        MAXIMUM(1.0);

        private final double score;

        Identification(double score) {
            this.score = score;
        }

        /**
         * The score in the methodology.
         *
         * @return 0.25 to 1
         */
        public double score() {
            return score;
        }
    }

    /**
     * How confidentiality was lost.
     *
     * @since 0.1.0
     */
    public enum Exposure {

        /**
         * Not lost, or exposed with no evidence of unlawful processing, such as a lost encrypted laptop: 0.
         */
        NONE(0),

        /**
         * Disclosed to a number of known recipients, such as an email sent to the wrong people: +0.25.
         */
        KNOWN_RECIPIENTS(0.25),

        /**
         * Disclosed to an unknown number of recipients, such as data published on the internet: +0.5.
         */
        UNKNOWN_RECIPIENTS(0.5);

        private final double score;

        Exposure(double score) {
            this.score = score;
        }

        /**
         * The points it adds.
         *
         * @return 0 to 0.5
         */
        public double score() {
            return score;
        }
    }

    /**
     * How integrity was lost.
     *
     * @since 0.1.0
     */
    public enum Alteration {

        /**
         * Not altered, or altered with no incorrect or unlawful use: 0.
         */
        NONE(0),

        /**
         * Altered and possibly used wrongly, but recoverable: +0.25.
         */
        RECOVERABLE(0.25),

        /**
         * Altered and possibly used wrongly, and the original cannot be recovered: +0.5.
         */
        UNRECOVERABLE(0.5);

        private final double score;

        Alteration(double score) {
            this.score = score;
        }

        /**
         * The points it adds.
         *
         * @return 0 to 0.5
         */
        public double score() {
            return score;
        }
    }

    /**
     * How availability was lost.
     *
     * @since 0.1.0
     */
    public enum Unavailability {

        /**
         * Not lost, or recoverable without difficulty from other copies: 0.
         */
        NONE(0),

        /**
         * Temporarily unavailable, recoverable with some work or from the person: +0.25.
         */
        TEMPORARY(0.25),

        /**
         * Lost for good, with no backup and no other source: +0.5.
         */
        PERMANENT(0.5);

        private final double score;

        Unavailability(double score) {
            this.score = score;
        }

        /**
         * The points it adds.
         *
         * @return 0 to 0.5
         */
        public double score() {
            return score;
        }
    }

    /**
     * The severity levels of the methodology.
     *
     * @since 0.1.0
     */
    public enum Severity {

        /**
         * Below 2: no effect, or a few inconveniences that people overcome without any problem.
         */
        LOW,

        /**
         * From 2 to 3: significant inconveniences that people overcome despite a few difficulties.
         */
        MEDIUM,

        /**
         * From 3 to 4: significant consequences that people overcome with serious difficulties.
         */
        HIGH,

        /**
         * 4 or more: significant or irreversible consequences that people may not overcome.
         */
        VERY_HIGH
    }

    private final int context;
    private final String contextReason;
    private final Identification identification;
    private final String identificationReason;
    private final Exposure exposure;
    private final Alteration alteration;
    private final Unavailability unavailability;
    private final boolean malicious;
    private final long individuals;
    private final boolean unintelligible;

    private BreachAssessment(Builder builder) {
        Scored scored = builder.score();
        this.context = scored.context();
        this.contextReason = scored.contextReason();
        this.identification = builder.identification != null ? builder.identification : scored.identification();
        this.identificationReason = builder.identification != null ? builder.identificationReason
                : scored.identificationReason();
        this.exposure = builder.exposure;
        this.alteration = builder.alteration;
        this.unavailability = builder.unavailability;
        this.malicious = builder.malicious;
        this.individuals = builder.individuals;
        this.unintelligible = builder.unintelligible;
    }

    /**
     * Starts an assessment.
     *
     * @return a builder with no affected field and no loss declared
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The data processing context: how critical the affected data is.
     *
     * @return 1 (simple data) to 4 (sensitive data), after any adjustment; 0 when no affected field holds personal
     *         data, which is then not a personal data breach
     */
    public int dataProcessingContext() {
        return context;
    }

    /**
     * Why the context has its score.
     *
     * @return the data type and the field that sets it, in English
     */
    public String contextReason() {
        return contextReason;
    }

    /**
     * How easily the affected data identifies people.
     *
     * @return the level
     */
    public Identification identification() {
        return identification;
    }

    /**
     * Why the ease of identification has its level.
     *
     * @return the identifiers found or the reason given, in English
     */
    public String identificationReason() {
        return identificationReason;
    }

    /**
     * The circumstances of the breach: the points the losses and malicious intent add.
     *
     * @return 0 to 2
     */
    public double circumstances() {
        return exposure.score() + alteration.score() + unavailability.score() + (malicious ? 0.5 : 0);
    }

    /**
     * The severity score.
     *
     * @return context &times; identification + circumstances, or 0 when no affected field holds personal data,
     *         which is then not a personal data breach
     */
    public double score() {
        return context == 0 ? 0 : context * identification.score() + circumstances();
    }

    /**
     * The severity level of the score.
     *
     * @return the level
     */
    public Severity severity() {
        double score = score();
        return score < 2 ? Severity.LOW : score < 3 ? Severity.MEDIUM : score < 4 ? Severity.HIGH
                : Severity.VERY_HIGH;
    }

    /**
     * Whether more than 100 people are affected, a flag of the methodology that weighs on the assessment without
     * changing the score.
     *
     * @return {@code true} above 100 individuals
     */
    public boolean manyIndividuals() {
        return individuals > 100;
    }

    /**
     * Whether the supervisory authority should be notified, within 72 hours of becoming aware of the breach (GDPR
     * article 33.1).
     *
     * @return {@code true} unless no personal data is affected, the severity is low, or only the confidentiality of
     *         unintelligible data was lost
     */
    public boolean notifyAuthority() {
        return context > 0 && severity() != Severity.LOW && !unintelligibleOnly();
    }

    // Only the confidentiality of data no one can read was lost: unlikely to result in a risk (EDPB Guidelines
    // 9/2022 and 01/2021, case 1), whatever the score of the data.
    private boolean unintelligibleOnly() {
        return unintelligible && alteration == Alteration.NONE && unavailability == Unavailability.NONE;
    }

    /**
     * Whether the data subjects should be told, without undue delay (GDPR article 34.1).
     *
     * @return {@code true} for a high or very high severity, unless only the confidentiality of unintelligible data
     *         was lost (article 34.3.a: encryption protects what was read, not what was lost or altered)
     */
    public boolean communicateToSubjects() {
        return context > 0 && !unintelligibleOnly()
                && (severity() == Severity.HIGH || severity() == Severity.VERY_HIGH);
    }

    /**
     * The assessment, its reasons and what to do, in English, one statement per line.
     *
     * @return the explanation
     */
    public String explanation() {
        List<String> lines = new ArrayList<>();
        if (context == 0) {
            lines.add("No affected field holds personal data: this is not a personal data breach (GDPR art. 4.12).");
            return String.join("\n", lines);
        }
        lines.add("Severity " + severity() + " (" + number(score()) + " = " + context + " x "
                + number(identification.score()) + " + " + number(circumstances()) + "), following ENISA's"
                + " methodology (2013).");
        lines.add("Data processing context " + context + ": " + contextReason + ".");
        lines.add("Ease of identification " + identification.name().toLowerCase(Locale.ROOT) + " ("
                + number(identification.score()) + "): " + identificationReason + ".");
        lines.add("Circumstances +" + number(circumstances()) + ": confidentiality " + exposure + ", integrity "
                + alteration + ", availability " + unavailability + (malicious ? ", malicious intent" : "") + ".");
        if (manyIndividuals()) {
            lines.add("More than 100 people are affected (" + individuals + ").");
        }
        lines.add(notifyAuthority()
                ? "Notify the supervisory authority within 72 hours of becoming aware of the breach (GDPR art. 33.1)."
                : (unintelligibleOnly() && severity() != Severity.LOW ? "Only the confidentiality of unintelligible"
                + " data was lost (EDPB Guidelines 9/2022). " : "") + "The breach is unlikely to result in a risk:"
                + " record it (GDPR art. 33.5); notification is not required (art. 33.1).");
        if (communicateToSubjects()) {
            lines.add("Tell the data subjects without undue delay (GDPR art. 34.1).");
        } else if (unintelligibleOnly() && (severity() == Severity.HIGH || severity() == Severity.VERY_HIGH)) {
            lines.add("The data was unintelligible to anyone without access to it, so the data subjects need not be"
                    + " told (GDPR art. 34.3.a).");
        } else {
            lines.add("Telling the data subjects is required only for a high risk (GDPR art. 34.1).");
        }
        lines.add("This is an orientation: the controller assesses the risk of each breach.");
        return String.join("\n", lines);
    }

    @Override
    public String toString() {
        return "BreachAssessment[" + severity() + ", " + number(score()) + "]";
    }

    private static String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private record Scored(int context, String contextReason, Identification identification,
                          String identificationReason) {
    }

    private record Affected(DataCategory category, boolean personal, Identifiability identifiability) {
    }

    /**
     * Builds a {@link BreachAssessment}.
     *
     * @since 0.1.0
     */
    public static final class Builder {

        private final List<Affected> affected = new ArrayList<>();
        private int adjustment;
        private String adjustmentReason;
        private Identification identification;
        private String identificationReason;
        private Exposure exposure = Exposure.NONE;
        private Alteration alteration = Alteration.NONE;
        private Unavailability unavailability = Unavailability.NONE;
        private boolean malicious;
        private long individuals;
        private boolean unintelligible;

        private Builder() {
        }

        /**
         * Adds affected fields.
         *
         * @param fields the classifications of the fields whose data was breached
         * @return this builder
         */
        public Builder affected(Collection<FieldClassification> fields) {
            fields.forEach(f -> affected.add(new Affected(f.category(), f.isPersonal(), f.identifiability())));
            return this;
        }

        /**
         * Adds affected fields from a data inventory.
         *
         * @param fields the stored fields whose data was breached
         * @param taxonomy the taxonomy to resolve their keys with
         * @return this builder
         * @throws TaxonomyException if the taxonomy does not contain a key
         */
        public Builder affected(Collection<InventoryField> fields, Taxonomy taxonomy) {
            for (InventoryField field : fields) {
                if (!field.isExempt()) {
                    affected.add(new Affected(field.category(taxonomy), field.isPersonal(taxonomy),
                            field.identifiability(taxonomy)));
                }
            }
            return this;
        }

        /**
         * Adjusts the data processing context, as the methodology foresees: up when the volume of data or the
         * vulnerability of the people makes it more critical, down when the data is public anyway.
         *
         * @param delta the points to add, between -3 and 3; the result stays between 1 and 4
         * @param reason why, in a few words
         * @return this builder
         */
        public Builder adjustContext(int delta, String reason) {
            if (delta < -3 || delta > 3) {
                throw new IllegalArgumentException("A context adjustment is between -3 and 3: " + delta);
            }
            this.adjustment = delta;
            this.adjustmentReason = Objects.requireNonNull(reason, "reason");
            return this;
        }

        /**
         * Sets the ease of identification instead of reading it from the affected fields.
         *
         * @param level the level
         * @param reason why, in a few words
         * @return this builder
         */
        public Builder identification(Identification level, String reason) {
            this.identification = Objects.requireNonNull(level, "level");
            this.identificationReason = Objects.requireNonNull(reason, "reason");
            return this;
        }

        /**
         * Declares how confidentiality was lost.
         *
         * @param exposure the exposure
         * @return this builder
         */
        public Builder confidentiality(Exposure exposure) {
            this.exposure = Objects.requireNonNull(exposure, "exposure");
            return this;
        }

        /**
         * Declares how integrity was lost.
         *
         * @param alteration the alteration
         * @return this builder
         */
        public Builder integrity(Alteration alteration) {
            this.alteration = Objects.requireNonNull(alteration, "alteration");
            return this;
        }

        /**
         * Declares how availability was lost.
         *
         * @param unavailability the unavailability
         * @return this builder
         */
        public Builder availability(Unavailability unavailability) {
            this.unavailability = Objects.requireNonNull(unavailability, "unavailability");
            return this;
        }

        /**
         * Declares that the breach was intentional, to harm the people or the controller: +0.5.
         *
         * @param malicious whether it was
         * @return this builder
         */
        public Builder malicious(boolean malicious) {
            this.malicious = malicious;
            return this;
        }

        /**
         * Declares how many people are affected.
         *
         * @param count the number of people
         * @return this builder
         */
        public Builder individuals(long count) {
            this.individuals = count;
            return this;
        }

        /**
         * Declares that the data was unintelligible to anyone without access to it, such as strongly encrypted with
         * a key that was not compromised (GDPR article 34.3.a).
         *
         * @param unintelligible whether it was
         * @return this builder
         */
        public Builder unintelligible(boolean unintelligible) {
            this.unintelligible = unintelligible;
            return this;
        }

        /**
         * Creates the assessment.
         *
         * @return the assessment
         */
        public BreachAssessment build() {
            return new BreachAssessment(this);
        }

        private Scored score() {
            int context = 0;
            String contextReason = "no personal data";
            Identification level = Identification.NEGLIGIBLE;
            String levelReason = "no identifier among the affected fields";
            // On equal scores the smallest key gives the reason, so the reasons do not depend on the order of fields.
            String contextKey = null;
            String levelKey = null;
            for (Affected field : affected) {
                String key = field.category().key();
                if (field.personal()) {
                    int score = contextOf(field.category());
                    if (score > context || score == context && key.compareTo(contextKey) < 0) {
                        context = score;
                        contextKey = key;
                        contextReason = typeOf(score) + " " + key;
                    }
                }
                Identification identifies = identificationOf(field);
                int order = identifies.compareTo(level);
                if (order > 0 || order == 0 && levelKey != null && key.compareTo(levelKey) < 0) {
                    level = identifies;
                    levelKey = key;
                    levelReason = reasonOf(field, identifies) + key;
                }
            }
            if (context > 0 && adjustment != 0) {
                int adjusted = Math.max(1, Math.min(4, context + adjustment));
                contextReason += ", adjusted " + (adjustment > 0 ? "+" : "") + adjustment + " (" + adjustmentReason
                        + ")";
                context = adjusted;
            }
            return new Scored(context, contextReason, level, levelReason);
        }

        private static int contextOf(DataCategory category) {
            if (category.nature() == LegalNature.SPECIAL_CATEGORY
                    || category.nature() == LegalNature.CRIMINAL_OFFENCE) {
                return 4;
            }
            if (category.has(Trait.FINANCIAL)) {
                return 3;
            }
            return category.has(Trait.BEHAVIOURAL) ? 2 : 1;
        }

        private static String typeOf(int score) {
            return switch (score) {
                case 4 -> "sensitive data";
                case 3 -> "financial data";
                case 2 -> "behavioural data";
                default -> "simple data";
            };
        }

        private static String reasonOf(Affected field, Identification identifies) {
            if (identifies != Identification.LIMITED || field.category().has(Trait.INDIRECT_IDENTIFIER)) {
                return "identifier ";
            }
            return field.identifiability() == Identifiability.PSEUDONYMIZED ? "pseudonymized "
                    : "personal data held about a person, ";
        }

        // A value that is not personal data, an anonymized or aggregated one included, identifies no one; personal
        // data held about someone, without an identifier among the fields, identifies them with some effort.
        private static Identification identificationOf(Affected field) {
            if (!field.personal()) {
                return Identification.NEGLIGIBLE;
            }
            DataCategory category = field.category();
            boolean pseudonymized = field.identifiability() == Identifiability.PSEUDONYMIZED;
            if (category.has(Trait.DIRECT_IDENTIFIER)) {
                return pseudonymized ? Identification.LIMITED : Identification.MAXIMUM;
            }
            if (category.has(Trait.CONTACT_IDENTIFIER)) {
                return pseudonymized ? Identification.LIMITED : Identification.SIGNIFICANT;
            }
            return Identification.LIMITED;
        }
    }
}
