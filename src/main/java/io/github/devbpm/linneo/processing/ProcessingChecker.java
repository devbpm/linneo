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

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.DataSubject;
import io.github.devbpm.linneo.DataUse;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.Term;
import io.github.devbpm.linneo.classify.ClassClassification;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.InventoryField;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.processing.ProcessingViolation.Code;
import io.github.devbpm.linneo.processing.ProcessingViolation.Element;
import io.github.devbpm.linneo.processing.ProcessingViolation.Severity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
 *       10 basis;</li>
 *   <li>under {@linkplain Jurisdiction#SPAIN Spanish law}, explicit consent as the only condition for the special
 *       categories of LOPDGDD article 9.1.</li>
 * </ul>
 * Gaps in the record are warnings: a deprecated key, a non-personal category, a condition that is not needed, a
 * basis resting on law without naming the law, legitimate interests without the interest pursued, no retention,
 * and an activity whose {@linkplain ImpactScreening screening} makes an impact assessment likely and that refers to
 * none. A case of GDPR article 35.3 that refers to no impact assessment is an error.
 * <p>
 * A checker created for a {@link Jurisdiction} other than {@link Jurisdiction#EU EU} also applies that country's
 * rules; see {@link Jurisdiction#SPAIN}.
 * <p>
 * The personal data of an activity is the union of its personal category keys and the personal fields of its
 * entities; anonymized and aggregated fields are not personal data. Instances are immutable and thread-safe.
 *
 * @since 0.1.0
 */
public final class ProcessingChecker {

    // LOPDGDD article 9.1: ideology, trade union membership, religion, sexual orientation, beliefs and racial or
    // ethnic origin, for which consent alone does not lift the prohibition.
    private static final List<String> CONSENT_INSUFFICIENT_IN_SPAIN = List.of(
            Categories.USER_DEMOGRAPHIC_POLITICAL_OPINION, Categories.USER_DEMOGRAPHIC_PHILOSOPHICAL_BELIEF,
            Categories.USER_DEMOGRAPHIC_TRADE_UNION_MEMBERSHIP, Categories.USER_DEMOGRAPHIC_RELIGIOUS_BELIEF,
            Categories.USER_DEMOGRAPHIC_SEXUAL_ORIENTATION, Categories.USER_DEMOGRAPHIC_RACE_ETHNICITY);

    // LOPDGDD article 9.2: these conditions must rest on a law with the rank of statute.
    private static final Set<SpecialCategoryCondition> STATUTE_REQUIRED_IN_SPAIN = Set.of(
            SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST, SpecialCategoryCondition.HEALTH_OR_SOCIAL_CARE,
            SpecialCategoryCondition.PUBLIC_HEALTH);

    // The provisions of the LOPDGDD that require a law with the rank of statute ("norma con rango de ley") for the
    // bases the activity declares: article 8 for a legal obligation or a task in the public interest, article 9.2
    // for some special category conditions, article 10 for criminal offence data authorised by law.
    private static List<String> statuteArticles(Governance governance, boolean special, boolean criminal) {
        List<String> articles = new ArrayList<>();
        if (governance.legalBasis() == LegalBasis.LEGAL_OBLIGATION
                || governance.legalBasis() == LegalBasis.PUBLIC_INTEREST) {
            articles.add("8");
        }
        if (special && governance.specialCategoryConditions().stream().anyMatch(STATUTE_REQUIRED_IN_SPAIN::contains)) {
            articles.add("9.2");
        }
        if (criminal && governance.criminalDataBasis() == CriminalDataBasis.AUTHORISED_BY_LAW) {
            articles.add("10");
        }
        return articles;
    }

    private final Taxonomy taxonomy;
    private final Classifier classifier;
    private final Jurisdiction jurisdiction;
    private final DataInventory inventory;

    /**
     * Creates a checker that applies the GDPR alone ({@link Jurisdiction#EU}).
     *
     * @param taxonomy the taxonomy the keys and entities must belong to
     */
    public ProcessingChecker(Taxonomy taxonomy) {
        this(taxonomy, Jurisdiction.EU);
    }

    /**
     * Creates a checker that applies the GDPR and the national rules of a jurisdiction.
     *
     * @param taxonomy the taxonomy the keys and entities must belong to
     * @param jurisdiction the national law to apply on top of the GDPR
     */
    public ProcessingChecker(Taxonomy taxonomy, Jurisdiction jurisdiction) {
        this(taxonomy, jurisdiction, null);
    }

    /**
     * Creates a checker that also resolves the {@linkplain ProcessingActivity#objects() objects} of the activities
     * in a data inventory, so that activities can be checked without the model classes, for example from records an
     * application keeps in its database.
     *
     * @param taxonomy the taxonomy the keys and entities must belong to
     * @param jurisdiction the national law to apply on top of the GDPR
     * @param inventory the data inventory the object keys refer to, or {@code null} for none
     */
    public ProcessingChecker(Taxonomy taxonomy, Jurisdiction jurisdiction, DataInventory inventory) {
        this.taxonomy = Objects.requireNonNull(taxonomy, "taxonomy");
        this.jurisdiction = Objects.requireNonNull(jurisdiction, "jurisdiction");
        this.classifier = new Classifier(taxonomy);
        this.inventory = inventory;
    }

    /**
     * The national law this checker applies on top of the GDPR.
     *
     * @return the jurisdiction
     */
    public Jurisdiction jurisdiction() {
        return jurisdiction;
    }

    /**
     * Checks one activity.
     *
     * @param activity the activity
     * @return the resolved activity and the problems found
     */
    public CheckedActivity check(ProcessingActivity activity) {
        return check(activity, new HashMap<>());
    }

    private CheckedActivity check(ProcessingActivity activity, Map<Class<?>, ClassClassification> classified) {
        Objects.requireNonNull(activity, "activity");
        Check check = new Check(activity, classified);
        List<DataUse> purposes = check.terms(activity.purposes(), "data use", Element.PURPOSES,
                Code.MISSING_PURPOSE,
                taxonomy::dataUse, taxonomy::requireDataUse);
        List<DataSubject> subjects = check.terms(activity.dataSubjects(), "data subject", Element.DATA_SUBJECTS,
                Code.MISSING_DATA_SUBJECTS,
                taxonomy::dataSubject, taxonomy::requireDataSubject);
        check.personalData();
        check.lawfulness(purposes, subjects);
        return new CheckedActivity(activity, purposes, subjects, List.copyOf(check.categories.values()),
                check.protection, check.violations);
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
        Map<Class<?>, ClassClassification> classified = new HashMap<>();
        for (ProcessingActivity activity : activities) {
            CheckedActivity result = check(activity, classified);
            if (!keys.add(activity.key())) {
                List<ProcessingViolation> violations = new ArrayList<>(result.violations());
                violations.add(new ProcessingViolation(activity.key(), Severity.ERROR, Code.DUPLICATE_ACTIVITY,
                        Element.KEY, "Another activity of the record has the key '" + activity.key() + "'"));
                result = new CheckedActivity(result.activity(), result.purposes(), result.dataSubjects(),
                        result.dataCategories(), result.protection(), violations);
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

    /** The state of the check of one activity. */
    private final class Check {

        private final ProcessingActivity activity;
        private final Map<Class<?>, ClassClassification> classified;
        private final List<ProcessingViolation> violations = new ArrayList<>();
        private final Map<String, DataCategory> categories = new TreeMap<>();
        private Protection protection = Protection.LOWEST;
        private boolean dataUnresolved;

        Check(ProcessingActivity activity, Map<Class<?>, ClassClassification> classified) {
            this.activity = activity;
            this.classified = classified;
        }

        <T extends Term> List<T> terms(List<String> keys, String kind, Element element, Code missing,
                                       Function<String, Optional<T>> lookup, Function<String, T> require) {
            Map<String, T> resolved = new LinkedHashMap<>();
            for (String key : keys) {
                Optional<T> found = lookup.apply(key);
                if (found.isEmpty()) {
                    error(element, Code.UNKNOWN_KEY, "Unknown " + kind + " '" + key + "'");
                    continue;
                }
                T term = found.get();
                if (term.info().isDeprecated()) {
                    String replacement = term.info().replacedBy();
                    warning(element, Code.DEPRECATED_KEY, "The " + kind + " '" + key + "' is deprecated"
                            + (replacement == null ? "" : "; checked as '" + replacement + "'"));
                    while (term.info().replacedBy() != null) {
                        term = require.apply(term.info().replacedBy());
                    }
                }
                resolved.putIfAbsent(term.key(), term);
            }
            if (keys.isEmpty()) {
                error(element, missing, "No " + kind + " declared");
            }
            return List.copyOf(resolved.values());
        }

        void personalData() {
            for (String key : activity.dataCategories()) {
                if (taxonomy.category(key).isEmpty()) {
                    error(Element.PERSONAL_DATA, Code.UNKNOWN_KEY, "Unknown data category '" + key + "'");
                    dataUnresolved = true;
                    continue;
                }
                DataCategory category = taxonomy.resolveCategory(key);
                if (taxonomy.requireCategory(key).info().isDeprecated()) {
                    warning(Element.PERSONAL_DATA, Code.DEPRECATED_KEY, "The data category '" + key
                            + "' is deprecated"
                            + (category.key().equals(key) ? "" : "; checked as '" + category.key() + "'"));
                }
                if (!category.isPersonal()) {
                    warning(Element.PERSONAL_DATA, Code.NOT_PERSONAL_DATA, "The data category '" + category.key()
                            + "' is not personal data and is left out of the record");
                    continue;
                }
                add(category, category.protection());
            }
            for (Class<?> entity : activity.entities()) {
                ClassClassification classification = classified.computeIfAbsent(entity, classifier::classify);
                if (!classification.isValid()) {
                    error(Element.PERSONAL_DATA, Code.ENTITY_NOT_CLASSIFIED, entity.getName() + " has "
                            + classification.errors().size()
                            + " classification error(s)");
                    dataUnresolved = true;
                }
                for (FieldClassification field : classification.personalFields()) {
                    add(field.category(), field.protection());
                }
            }
            for (String object : activity.objects()) {
                if (inventory == null || !inventory.objects().contains(object)) {
                    error(Element.PERSONAL_DATA, Code.UNKNOWN_KEY, "Unknown object '" + object + "'"
                            + (inventory == null ? ": the checker was given no data inventory" : ""));
                    dataUnresolved = true;
                    continue;
                }
                for (InventoryField field : inventory.fields(object)) {
                    if (field.isExempt()) {
                        continue;
                    }
                    Optional<DataCategory> declared = taxonomy.category(field.key());
                    if (declared.isEmpty()) {
                        error(Element.PERSONAL_DATA, Code.UNKNOWN_KEY, "The field " + object + "." + field.path()
                                + " has the unknown data category '" + field.key() + "'");
                        dataUnresolved = true;
                        continue;
                    }
                    if (declared.get().info().isDeprecated()) {
                        DataCategory current = field.category(taxonomy);
                        warning(Element.PERSONAL_DATA, Code.DEPRECATED_KEY, "The field " + object + "."
                                + field.path() + " has the deprecated data category '" + field.key() + "'"
                                + (current.key().equals(field.key()) ? "" : "; checked as '" + current.key() + "'"));
                    }
                    if (field.isPersonal(taxonomy)) {
                        add(field.category(taxonomy), field.protection(taxonomy));
                    }
                }
            }
            if (categories.isEmpty() && !dataUnresolved) {
                error(Element.PERSONAL_DATA, Code.MISSING_PERSONAL_DATA, "The activity processes no personal data");
            }
        }

        void lawfulness(List<DataUse> purposes, List<DataSubject> subjects) {
            if (categories.isEmpty()) {
                return;
            }
            List<String> resting = new ArrayList<>();
            Governance governance = activity.governance();
            LegalBasis basis = governance.legalBasis();
            if (basis == null) {
                error(Element.LEGAL_BASIS, Code.MISSING_LEGAL_BASIS,
                        "Personal data needs a lawful basis (GDPR article 6.1)");
            } else {
                if (basis.requiresLegalReference()) {
                    resting.add(basis.article());
                }
                if (basis == LegalBasis.LEGITIMATE_INTERESTS && governance.legitimateInterest() == null) {
                    warning(Element.LEGITIMATE_INTEREST, Code.MISSING_LEGITIMATE_INTEREST,
                            "State the legitimate interest pursued (GDPR article"
                            + " 6.1.f)");
                } else if (basis != LegalBasis.LEGITIMATE_INTERESTS && governance.legitimateInterest() != null) {
                    warning(Element.LEGITIMATE_INTEREST, Code.UNNEEDED_CONDITION,
                            "A legitimate interest is stated but the lawful basis is article "
                            + basis.article());
                }
            }

            boolean special = categories.values().stream().anyMatch(c -> c.nature() == LegalNature.SPECIAL_CATEGORY);
            Set<SpecialCategoryCondition> conditions = governance.specialCategoryConditions();
            if (special && conditions.isEmpty()) {
                error(Element.SPECIAL_CATEGORY_CONDITION, Code.MISSING_SPECIAL_CATEGORY_CONDITION,
                        "Special categories of data ("
                        + keysOf(LegalNature.SPECIAL_CATEGORY) + ") need a condition of GDPR article 9.2");
            } else if (!special && !conditions.isEmpty()) {
                warning(Element.SPECIAL_CATEGORY_CONDITION, Code.UNNEEDED_CONDITION, "Article(s) "
                        + String.join(", ", conditions.stream().map(SpecialCategoryCondition::article).toList())
                        + " declared but the activity processes no special category of data");
            } else {
                conditions.stream().filter(SpecialCategoryCondition::requiresLegalReference)
                        .forEach(condition -> resting.add(condition.article()));
            }
            // With explicit consent alone; another condition declared beside it covers the data consent cannot.
            if (special && conditions.equals(Set.of(SpecialCategoryCondition.EXPLICIT_CONSENT))
                    && jurisdiction == Jurisdiction.SPAIN) {
                String keys = String.join(", ", categories.keySet().stream()
                        .filter(key -> CONSENT_INSUFFICIENT_IN_SPAIN.stream()
                                .anyMatch(k -> key.equals(k) || key.startsWith(k + ".")))
                        .toList());
                if (!keys.isEmpty()) {
                    error(Element.SPECIAL_CATEGORY_CONDITION, Code.CONSENT_NOT_SUFFICIENT,
                            "Explicit consent alone does not lift the prohibition of"
                            + " processing " + keys + " (LOPDGDD article 9.1); declare another condition of GDPR"
                            + " article 9.2");
                }
            }

            boolean criminal = categories.values().stream().anyMatch(c -> c.nature() == LegalNature.CRIMINAL_OFFENCE);
            CriminalDataBasis criminalBasis = governance.criminalDataBasis();
            if (criminal && criminalBasis == null) {
                error(Element.CRIMINAL_DATA_BASIS, Code.MISSING_CRIMINAL_DATA_BASIS, "Criminal offence data ("
                        + keysOf(LegalNature.CRIMINAL_OFFENCE)
                        + ") needs a basis of GDPR article 10");
            } else if (!criminal && criminalBasis != null) {
                warning(Element.CRIMINAL_DATA_BASIS, Code.UNNEEDED_CONDITION,
                        "Article 10 is declared but the activity processes no criminal"
                        + " offence data");
            } else if (criminalBasis != null && criminalBasis.requiresLegalReference()) {
                resting.add(criminalBasis.article());
            }

            if (!resting.isEmpty() && governance.legalReference() == null) {
                List<String> statute = jurisdiction == Jurisdiction.SPAIN
                        ? statuteArticles(governance, special, criminal) : List.of();
                warning(Element.LEGAL_REFERENCE, Code.MISSING_LEGAL_REFERENCE, "GDPR article(s) "
                        + String.join(", ", resting)
                        + " rest on Union or Member State law; name it as the legal reference"
                        + (statute.isEmpty() ? "" : " (LOPDGDD article(s) " + String.join(", ", statute)
                        + ": a law with the rank of statute)"));
            }
            if (jurisdiction == Jurisdiction.SPAIN && governance.legalReference() == null
                    && governance.legalBasis() != LegalBasis.CONSENT
                    && categories.keySet().stream().anyMatch(key -> key.equals(Categories.USER_ADMINISTRATIVE_SANCTION)
                    || key.startsWith(Categories.USER_ADMINISTRATIVE_SANCTION + "."))) {
                warning(Element.LEGAL_REFERENCE, Code.MISSING_LEGAL_REFERENCE, "Data on administrative"
                        + " infringements and sanctions may only be processed by the bodies competent for the"
                        + " sanctioning procedure, with the consent of the person, or as a law with the rank of statute"
                        + " allows (LOPDGDD article 27); name that law as the legal reference");
            }
            if (governance.retention() == null) {
                warning(Element.RETENTION, Code.MISSING_RETENTION,
                        "Declare the envisaged time limit for erasure (GDPR article"
                        + " 30.1.f)");
            }
            ImpactScreening screening = ImpactScreening.of(purposes, subjects, categories.values(), governance);
            if (governance.impactAssessment() == null) {
                if (screening.outcome() == ImpactScreening.Outcome.REQUIRED) {
                    error(Element.IMPACT_ASSESSMENT, Code.IMPACT_ASSESSMENT_REQUIRED, "GDPR article "
                            + screening.mandatoryCase() + " requires an impact assessment; refer to it. "
                            + screening.summary());
                } else if (screening.outcome() == ImpactScreening.Outcome.LIKELY) {
                    warning(Element.IMPACT_ASSESSMENT, Code.IMPACT_ASSESSMENT_RECOMMENDED, "GDPR article 35 likely"
                            + " requires an impact assessment" + (jurisdiction == Jurisdiction.SPAIN ? " (the AEPD's"
                            + " list under article 35.4 requires one in most cases that meet two criteria or more)"
                            : "") + ". " + screening.summary());
                }
            }
        }

        private void add(DataCategory category, Protection dataProtection) {
            categories.putIfAbsent(category.key(), category);
            protection = protection.join(dataProtection);
        }

        private String keysOf(LegalNature nature) {
            return String.join(", ", categories.values().stream()
                    .filter(c -> c.nature() == nature).map(DataCategory::key).toList());
        }

        private void error(Element element, Code code, String message) {
            violations.add(new ProcessingViolation(activity.key(), Severity.ERROR, code, element, message));
        }

        private void warning(Element element, Code code, String message) {
            violations.add(new ProcessingViolation(activity.key(), Severity.WARNING, code, element, message));
        }
    }
}
