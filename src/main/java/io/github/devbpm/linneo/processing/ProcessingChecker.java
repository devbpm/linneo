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

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.DataSubject;
import io.github.devbpm.linneo.DataUse;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.Term;
import io.github.devbpm.linneo.classify.ClassClassification;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.processing.ProcessingViolation.Code;
import io.github.devbpm.linneo.processing.ProcessingViolation.Severity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Checks processing activities against a taxonomy and the classification of their entities, and builds the record
 * of processing activities.
 * <p>
 * The check is fail-closed for what would make the processing unlawful as declared, which are errors:
 * <ul>
 *   <li>an unknown purpose, data subject or data category key;</li>
 *   <li>no purpose, no data subjects, or no personal data at all;</li>
 *   <li>an entity that is not fully and correctly {@linkplain Classifier classified};</li>
 *   <li>personal data without a lawful basis (GDPR article 6.1);</li>
 *   <li>special categories of data without an article 9.2 condition, or criminal offence data without an article
 *       10 basis.</li>
 * </ul>
 * Gaps in the record are warnings: a deprecated key, a non-personal category, a condition that is not needed, a
 * basis resting on law without naming the law, legitimate interests without the interest pursued, no retention,
 * and special category, criminal offence or profiling activities that refer to no impact assessment.
 * <p>
 * The personal data of an activity is the union of its personal category keys and the personal fields of its
 * entities; anonymized and aggregated fields are not personal data. Instances are immutable and thread-safe.
 *
 * @since 0.1.0
 */
public final class ProcessingChecker {

    private static final Set<String> PROFILING_SEGMENTS = Set.of("profiling", "profiled", "targeted");

    private final Taxonomy taxonomy;
    private final Classifier classifier;

    /**
     * Creates a checker.
     *
     * @param taxonomy the taxonomy the keys and entities must belong to
     */
    public ProcessingChecker(Taxonomy taxonomy) {
        this.taxonomy = Objects.requireNonNull(taxonomy, "taxonomy");
        this.classifier = new Classifier(taxonomy);
    }

    /**
     * Checks one activity.
     *
     * @param activity the activity
     * @return the resolved activity and the problems found
     */
    public CheckedActivity check(ProcessingActivity activity) {
        Objects.requireNonNull(activity, "activity");
        Check check = new Check(activity);
        List<DataUse> purposes = check.terms(activity.purposes(), "data use", Code.MISSING_PURPOSE,
                taxonomy::dataUse, taxonomy::requireDataUse);
        List<DataSubject> subjects = check.terms(activity.dataSubjects(), "data subject", Code.MISSING_DATA_SUBJECTS,
                taxonomy::dataSubject, taxonomy::requireDataSubject);
        check.personalData();
        check.lawfulness(purposes);
        return new CheckedActivity(activity, purposes, subjects, List.copyOf(check.categories.values()),
                check.confidentiality, check.integrity, check.secret, check.violations);
    }

    /**
     * Checks several activities.
     *
     * @param activities the activities
     * @return one result per activity, in the same order
     */
    public List<CheckedActivity> checkAll(Collection<ProcessingActivity> activities) {
        List<CheckedActivity> checked = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (ProcessingActivity activity : activities) {
            CheckedActivity result = check(activity);
            if (!keys.add(activity.key())) {
                List<ProcessingViolation> violations = new ArrayList<>(result.violations());
                violations.add(new ProcessingViolation(activity.key(), Severity.ERROR, Code.DUPLICATE_ACTIVITY,
                        "Another activity of the record has the key '" + activity.key() + "'"));
                result = new CheckedActivity(result.activity(), result.purposes(), result.dataSubjects(),
                        result.dataCategories(), result.confidentiality(), result.integrity(), result.secret(),
                        violations);
            }
            checked.add(result);
        }
        return checked;
    }

    /**
     * Checks activities and builds the record of processing activities.
     *
     * @param activities the activities of the record
     * @return the record, when there is no error
     * @throws ProcessingException listing every error found in any of the activities
     */
    public ProcessingRecord verify(Collection<ProcessingActivity> activities) {
        List<CheckedActivity> checked = checkAll(activities);
        List<ProcessingViolation> errors = checked.stream().flatMap(c -> c.errors().stream()).toList();
        if (!errors.isEmpty()) {
            throw new ProcessingException(errors);
        }
        return new ProcessingRecord(checked);
    }

    /**
     * Checks activities and builds the record of processing activities.
     *
     * @param activities the activities of the record
     * @return the record, when there is no error
     * @throws ProcessingException listing every error found in any of the activities
     */
    public ProcessingRecord verify(ProcessingActivity... activities) {
        return verify(Arrays.asList(activities));
    }

    private static boolean profiles(DataUse use) {
        return Arrays.stream(use.key().split("\\.")).anyMatch(PROFILING_SEGMENTS::contains);
    }

    /** The state of the check of one activity. */
    private final class Check {

        private final ProcessingActivity activity;
        private final List<ProcessingViolation> violations = new ArrayList<>();
        private final Map<String, DataCategory> categories = new TreeMap<>();
        private Confidentiality confidentiality = Confidentiality.PUBLIC;
        private Integrity integrity = Integrity.LOW;
        private boolean secret;
        private boolean dataUnresolved;

        Check(ProcessingActivity activity) {
            this.activity = activity;
        }

        <T extends Term> List<T> terms(List<String> keys, String kind, Code missing,
                                       Function<String, Optional<T>> lookup, Function<String, T> require) {
            Map<String, T> resolved = new LinkedHashMap<>();
            for (String key : keys) {
                Optional<T> found = lookup.apply(key);
                if (found.isEmpty()) {
                    error(Code.UNKNOWN_KEY, "Unknown " + kind + " '" + key + "'");
                    continue;
                }
                T term = found.get();
                if (term.info().isDeprecated()) {
                    String replacement = term.info().replacedBy();
                    warning(Code.DEPRECATED_KEY, "The " + kind + " '" + key + "' is deprecated"
                            + (replacement == null ? "" : "; checked as '" + replacement + "'"));
                    while (term.info().replacedBy() != null) {
                        term = require.apply(term.info().replacedBy());
                    }
                }
                resolved.putIfAbsent(term.key(), term);
            }
            if (keys.isEmpty()) {
                error(missing, "No " + kind + " declared");
            }
            return List.copyOf(resolved.values());
        }

        void personalData() {
            for (String key : activity.dataCategories()) {
                if (taxonomy.category(key).isEmpty()) {
                    error(Code.UNKNOWN_KEY, "Unknown data category '" + key + "'");
                    dataUnresolved = true;
                    continue;
                }
                DataCategory category = taxonomy.resolveCategory(key);
                if (taxonomy.requireCategory(key).info().isDeprecated()) {
                    warning(Code.DEPRECATED_KEY, "The data category '" + key + "' is deprecated"
                            + (category.key().equals(key) ? "" : "; checked as '" + category.key() + "'"));
                }
                if (!category.isPersonal()) {
                    warning(Code.NOT_PERSONAL_DATA, "The data category '" + category.key()
                            + "' is not personal data and is left out of the record");
                    continue;
                }
                add(category, category.confidentiality(), category.integrity(), category.secret());
            }
            for (Class<?> entity : activity.entities()) {
                ClassClassification classification = classifier.classify(entity);
                if (!classification.isValid()) {
                    error(Code.ENTITY_NOT_CLASSIFIED, entity.getName() + " has " + classification.errors().size()
                            + " classification error(s)");
                    dataUnresolved = true;
                }
                for (FieldClassification field : classification.personalFields()) {
                    add(field.category(), field.confidentiality(), field.integrity(), field.isSecret());
                }
            }
            if (categories.isEmpty() && !dataUnresolved) {
                error(Code.MISSING_PERSONAL_DATA, "The activity processes no personal data");
            }
        }

        void lawfulness(List<DataUse> purposes) {
            if (categories.isEmpty()) {
                return;
            }
            List<String> resting = new ArrayList<>();
            LegalBasis basis = activity.legalBasis();
            if (basis == null) {
                error(Code.MISSING_LEGAL_BASIS, "Personal data needs a lawful basis (GDPR article 6.1)");
            } else {
                if (basis.requiresLegalReference()) {
                    resting.add(basis.article());
                }
                if (basis == LegalBasis.LEGITIMATE_INTERESTS && activity.legitimateInterest() == null) {
                    warning(Code.MISSING_LEGITIMATE_INTEREST, "State the legitimate interest pursued (GDPR article"
                            + " 6.1.f)");
                } else if (basis != LegalBasis.LEGITIMATE_INTERESTS && activity.legitimateInterest() != null) {
                    warning(Code.UNNEEDED_CONDITION, "A legitimate interest is stated but the lawful basis is article "
                            + basis.article());
                }
            }

            boolean special = categories.values().stream().anyMatch(c -> c.nature() == LegalNature.SPECIAL_CATEGORY);
            SpecialCategoryCondition condition = activity.specialCategoryCondition();
            if (special && condition == null) {
                error(Code.MISSING_SPECIAL_CATEGORY_CONDITION, "Special categories of data ("
                        + keysOf(LegalNature.SPECIAL_CATEGORY) + ") need a condition of GDPR article 9.2");
            } else if (!special && condition != null) {
                warning(Code.UNNEEDED_CONDITION, "Article " + condition.article()
                        + " is declared but the activity processes no special category of data");
            } else if (condition != null && condition.requiresLegalReference()) {
                resting.add(condition.article());
            }

            boolean criminal = categories.values().stream().anyMatch(c -> c.nature() == LegalNature.CRIMINAL_OFFENCE);
            CriminalDataBasis criminalBasis = activity.criminalDataBasis();
            if (criminal && criminalBasis == null) {
                error(Code.MISSING_CRIMINAL_DATA_BASIS, "Criminal offence data (" + keysOf(LegalNature.CRIMINAL_OFFENCE)
                        + ") needs a basis of GDPR article 10");
            } else if (!criminal && criminalBasis != null) {
                warning(Code.UNNEEDED_CONDITION, "Article 10 is declared but the activity processes no criminal"
                        + " offence data");
            } else if (criminalBasis != null && criminalBasis.requiresLegalReference()) {
                resting.add(criminalBasis.article());
            }

            if (!resting.isEmpty() && activity.legalReference() == null) {
                warning(Code.MISSING_LEGAL_REFERENCE, "GDPR article(s) " + String.join(", ", resting)
                        + " rest on Union or Member State law; name it as the legal reference");
            }
            if (activity.retention() == null) {
                warning(Code.MISSING_RETENTION, "Declare the envisaged time limit for erasure (GDPR article"
                        + " 30.1.f)");
            }
            List<String> reasons = new ArrayList<>();
            if (special) {
                reasons.add("special categories of data");
            }
            if (criminal) {
                reasons.add("criminal offence data");
            }
            if (purposes.stream().anyMatch(ProcessingChecker::profiles)) {
                reasons.add("profiling");
            }
            if (!reasons.isEmpty() && activity.impactAssessment() == null) {
                warning(Code.IMPACT_ASSESSMENT_RECOMMENDED, "The activity involves " + String.join(" and ", reasons)
                        + "; GDPR article 35 may require an impact assessment");
            }
        }

        private void add(DataCategory category, Confidentiality fieldConfidentiality, Integrity fieldIntegrity,
                         boolean fieldSecret) {
            categories.putIfAbsent(category.key(), category);
            if (fieldConfidentiality.compareTo(confidentiality) > 0) {
                confidentiality = fieldConfidentiality;
            }
            if (fieldIntegrity.compareTo(integrity) > 0) {
                integrity = fieldIntegrity;
            }
            secret |= fieldSecret;
        }

        private String keysOf(LegalNature nature) {
            return String.join(", ", categories.values().stream()
                    .filter(c -> c.nature() == nature).map(DataCategory::key).toList());
        }

        private void error(Code code, String message) {
            violations.add(new ProcessingViolation(activity.key(), Severity.ERROR, code, message));
        }

        private void warning(Code code, String message) {
            violations.add(new ProcessingViolation(activity.key(), Severity.WARNING, code, message));
        }
    }
}
