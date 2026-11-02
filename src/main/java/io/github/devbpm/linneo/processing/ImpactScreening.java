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
import io.github.devbpm.linneo.DataSubject;
import io.github.devbpm.linneo.DataUse;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Trait;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Whether a processing activity needs a data protection impact assessment (GDPR article 35), screened against the
 * nine criteria of the European Data Protection Board's guidelines (WP248 rev.01) and the cases article 35.3 makes
 * mandatory. It is a screening, not the assessment: it says which criteria apply and why.
 * <p>
 * The criteria the code can see are found from the {@linkplain io.github.devbpm.linneo.Trait traits} of the
 * activity's terms, so an extension's terms count as soon as they declare theirs: special category and criminal
 * offence data, and data of a highly personal nature ({@link io.github.devbpm.linneo.Trait#FINANCIAL},
 * {@link io.github.devbpm.linneo.Trait#BEHAVIOURAL}, {@link io.github.devbpm.linneo.Trait#PRIVATE_LIFE}) are
 * {@link Criterion#SENSITIVE_DATA}; purposes with the {@link io.github.devbpm.linneo.Trait#EVALUATION} trait are
 * {@link Criterion#EVALUATION}; purposes and data categories with the
 * {@link io.github.devbpm.linneo.Trait#MONITORING} trait are {@link Criterion#SYSTEMATIC_MONITORING}; data subjects
 * and data categories with the {@link io.github.devbpm.linneo.Trait#VULNERABLE} trait are
 * {@link Criterion#VULNERABLE_SUBJECTS}. The rest only the data protection officer knows, such as the scale of the
 * processing, and declares as {@linkplain Governance#riskFactors() risk factors}, as the code declares those it
 * knows ({@link ProcessingActivity#riskFactors()}). The evidence of each criterion is
 * sorted, so the screening does not depend on the order in which the activity lists its terms.
 * <p>
 * The outcome follows the guidelines: a case of article 35.3 is {@link Outcome#REQUIRED}; two criteria or more make
 * an assessment {@link Outcome#LIKELY} to be required; one makes it {@link Outcome#POSSIBLE}. The
 * {@link ProcessingChecker} reports a required assessment the activity does not refer to as an error, and a likely one
 * as a warning. Review the result against the list of the supervisory authority (article 35.4).
 *
 * @param outcome whether an assessment is needed
 * @param findings the criteria met, in the order of the guidelines, each with what shows it
 * @param mandatoryCase the provision of article 35.3 that makes the assessment mandatory, such as {@code 35.3.b},
 *        or {@code null}
 * @since 0.1.0
 */
public record ImpactScreening(Outcome outcome, List<Finding> findings, String mandatoryCase) {

    /**
     * The criteria of the guidelines (WP248 rev.01, section III.B.a).
     *
     * @since 0.1.0
     */
    public enum Criterion {

        /**
         * 1. Evaluation or scoring, including profiling and predicting.
         */
        EVALUATION,

        /**
         * 2. Automated decision-making with legal or similarly significant effect.
         */
        AUTOMATED_DECISIONS,

        /**
         * 3. Systematic monitoring.
         */
        SYSTEMATIC_MONITORING,

        /**
         * 4. Sensitive data or data of a highly personal nature.
         */
        SENSITIVE_DATA,

        /**
         * 5. Data processed on a large scale.
         */
        LARGE_SCALE,

        /**
         * 6. Matching or combining datasets.
         */
        DATASET_MATCHING,

        /**
         * 7. Data concerning vulnerable data subjects.
         */
        VULNERABLE_SUBJECTS,

        /**
         * 8. Innovative use or applying new technological or organisational solutions.
         */
        INNOVATIVE_TECHNOLOGY,

        /**
         * 9. Processing that prevents data subjects from exercising a right or using a service or a contract.
         */
        PREVENTS_RIGHTS;

        /**
         * The number of the criterion in the guidelines.
         *
         * @return 1 to 9
         */
        public int number() {
            return ordinal() + 1;
        }
    }

    /**
     * What the data protection officer declares about an activity because the code cannot know it.
     *
     * @since 0.1.0
     */
    public enum Factor {

        /**
         * Profiles or rates people beyond what the purposes show.
         */
        EVALUATION(Criterion.EVALUATION),

        /**
         * Decides automatically with legal or similarly significant effects on people.
         */
        AUTOMATED_DECISIONS(Criterion.AUTOMATED_DECISIONS),

        /**
         * Observes, monitors or controls people systematically.
         */
        SYSTEMATIC_MONITORING(Criterion.SYSTEMATIC_MONITORING),

        /**
         * Monitors a publicly accessible area systematically, which on a large scale is article 35.3.c.
         */
        PUBLICLY_ACCESSIBLE_AREA(Criterion.SYSTEMATIC_MONITORING),

        /**
         * Processes data on a large scale: many people, much data, a long time or a wide area.
         */
        LARGE_SCALE(Criterion.LARGE_SCALE),

        /**
         * Matches or combines datasets from different processing operations or controllers.
         */
        DATASET_MATCHING(Criterion.DATASET_MATCHING),

        /**
         * Concerns vulnerable people the data subjects of the activity do not show, such as minors among students.
         */
        VULNERABLE_SUBJECTS(Criterion.VULNERABLE_SUBJECTS),

        /**
         * Uses new technology or organisational solutions, such as biometric access or connected devices.
         */
        INNOVATIVE_TECHNOLOGY(Criterion.INNOVATIVE_TECHNOLOGY),

        /**
         * Can prevent people from exercising a right or using a service or a contract.
         */
        PREVENTS_RIGHTS(Criterion.PREVENTS_RIGHTS);

        private final Criterion criterion;

        Factor(Criterion criterion) {
            this.criterion = criterion;
        }

        /**
         * The criterion the factor meets.
         *
         * @return the criterion
         */
        public Criterion criterion() {
            return criterion;
        }
    }

    /**
     * Whether an impact assessment is needed, from the least to the most.
     *
     * @since 0.1.0
     */
    public enum Outcome {

        /**
         * No criterion is met.
         */
        NOT_INDICATED,

        /**
         * One criterion is met: an assessment may be needed, depending on the case.
         */
        POSSIBLE,

        /**
         * Two criteria or more are met: in most cases an assessment is required.
         */
        LIKELY,

        /**
         * A case of article 35.3: an assessment is required.
         */
        REQUIRED
    }

    /**
     * A criterion met, with what shows it.
     *
     * @param criterion the criterion
     * @param evidence the categories, purposes, data subjects or declared factors that meet it, in English
     * @since 0.1.0
     */
    public record Finding(Criterion criterion, String evidence) {

        /**
         * Checks the components.
         *
         * @param criterion the criterion
         * @param evidence the evidence
         */
        public Finding {
            Objects.requireNonNull(criterion, "criterion");
            Objects.requireNonNull(evidence, "evidence");
        }
    }

    /**
     * Checks the components and makes the findings immutable.
     *
     * @param outcome the outcome
     * @param findings the findings
     * @param mandatoryCase the article 35.3 provision or {@code null}
     */
    public ImpactScreening {
        Objects.requireNonNull(outcome, "outcome");
        findings = List.copyOf(findings);
    }

    /**
     * Screens a checked activity.
     *
     * @param activity the activity, as checked against the taxonomy
     * @return the screening
     */
    public static ImpactScreening of(CheckedActivity activity) {
        return of(activity.purposes(), activity.dataSubjects(), activity.dataCategories(),
                activity.activity().allRiskFactors());
    }

    static ImpactScreening of(Collection<DataUse> purposes, Collection<DataSubject> subjects,
                              Collection<DataCategory> categories, Set<Factor> factors) {
        Map<Criterion, Set<String>> evidence = new EnumMap<>(Criterion.class);
        for (DataUse use : purposes) {
            if (use.has(Trait.EVALUATION)) {
                add(evidence, Criterion.EVALUATION, "purpose " + use.key());
            }
            if (use.has(Trait.MONITORING)) {
                add(evidence, Criterion.SYSTEMATIC_MONITORING, "purpose " + use.key());
            }
        }
        boolean special = false;
        for (DataCategory category : categories) {
            if (category.nature() == LegalNature.SPECIAL_CATEGORY
                    || category.nature() == LegalNature.CRIMINAL_OFFENCE) {
                special = true;
                add(evidence, Criterion.SENSITIVE_DATA, (category.nature() == LegalNature.SPECIAL_CATEGORY
                        ? "special category data " : "criminal offence data ") + category.key());
            } else if (category.traits().stream().anyMatch(Trait::isHighlyPersonal)) {
                add(evidence, Criterion.SENSITIVE_DATA, "highly personal data " + category.key());
            }
            if (category.has(Trait.MONITORING)) {
                add(evidence, Criterion.SYSTEMATIC_MONITORING, "data category " + category.key());
            }
            if (category.has(Trait.VULNERABLE)) {
                add(evidence, Criterion.VULNERABLE_SUBJECTS, "data category " + category.key());
            }
        }
        for (DataSubject subject : subjects) {
            if (subject.has(Trait.VULNERABLE)) {
                add(evidence, Criterion.VULNERABLE_SUBJECTS, "data subjects " + subject.key());
            }
        }
        for (Factor factor : factors) {
            add(evidence, factor.criterion(), "declared " + factor.name());
        }
        String mandatory = null;
        if (evidence.containsKey(Criterion.EVALUATION) && factors.contains(Factor.AUTOMATED_DECISIONS)) {
            mandatory = "35.3.a";
        } else if (special && factors.contains(Factor.LARGE_SCALE)) {
            mandatory = "35.3.b";
        } else if (factors.contains(Factor.PUBLICLY_ACCESSIBLE_AREA) && factors.contains(Factor.LARGE_SCALE)) {
            mandatory = "35.3.c";
        }
        List<Finding> findings = new ArrayList<>();
        evidence.forEach((criterion, shown) -> findings.add(new Finding(criterion, String.join("; ", shown))));
        Outcome outcome = mandatory != null ? Outcome.REQUIRED
                : findings.size() >= 2 ? Outcome.LIKELY
                : findings.size() == 1 ? Outcome.POSSIBLE : Outcome.NOT_INDICATED;
        return new ImpactScreening(outcome, findings, mandatory);
    }

    /**
     * The criteria met.
     *
     * @return in the order of the guidelines
     */
    public Set<Criterion> criteria() {
        Set<Criterion> criteria = EnumSet.noneOf(Criterion.class);
        findings.forEach(finding -> criteria.add(finding.criterion()));
        return Collections.unmodifiableSet(criteria);
    }

    /**
     * A one-line explanation, in English.
     *
     * @return the outcome, the provision or the number of criteria, and the findings
     */
    public String summary() {
        String head = switch (outcome) {
            case REQUIRED -> "Required (GDPR art. " + mandatoryCase + ")";
            case LIKELY -> "Likely required (" + findings.size() + " criteria of WP248)";
            case POSSIBLE -> "Possible (1 criterion of WP248)";
            case NOT_INDICATED -> "Not indicated";
        };
        if (findings.isEmpty()) {
            return head;
        }
        List<String> parts = new ArrayList<>();
        findings.forEach(f -> parts.add(f.criterion().number() + ". " + f.criterion().name().toLowerCase(Locale.ROOT)
                .replace('_', ' ') + ": " + f.evidence()));
        return head + ": " + String.join("; ", parts);
    }

    private static void add(Map<Criterion, Set<String>> evidence, Criterion criterion, String shown) {
        evidence.computeIfAbsent(criterion, c -> new TreeSet<>()).add(shown);
    }
}
