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
import io.github.devbpm.linneo.DataUse;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.Term;
import io.github.devbpm.linneo.Trait;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.InventoryField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What the record of processing activities says to someone of one category of data subjects who exercises their
 * rights (GDPR articles 15 to 22): the activities that process their data, what each one processes and why, where
 * that data is stored, and which rights each activity's lawful basis gives them.
 * <pre>{@code
 * SubjectRights rights = SubjectRights.of(record, taxonomy, DataSubjects.STUDENT, Jurisdiction.SPAIN);
 * rights.storedFields(inventory, taxonomy);   // where to look for the person's data, object by object
 * rights.activities().forEach(activity -> activity.entitlement(Right.ERASURE));
 * }</pre>
 * The rights follow what the GDPR provides for each lawful basis: erasure is not available while a legal obligation
 * or a public task needs the data (article 17.3.b), portability applies to processing based on consent, explicit
 * consent to special category data or a contract (article 20.1), objection to processing based on a public task or
 * legitimate interests (article 21.1) and always to direct marketing, the purposes with the
 * {@link Trait#DIRECT_MARKETING} trait (article 21.2), after which the marketing stops and its data is erased except
 * what a legal obligation or a public task still needs. The data subject key is resolved with the taxonomy, so a key
 * that does not exist is an error rather than an empty answer. It is the starting point of the answer, not the
 * answer: the data
 * protection officer decides each request, since the conditions of each article depend on the case. Under
 * {@link Jurisdiction#SPAIN} the data is blocked rather than deleted when it is erased or rectified (LOPDGDD article
 * 32). Instances are immutable.
 *
 * @since 0.1.0
 */
public final class SubjectRights {

    /**
     * A right of the data subject that depends on the processing.
     *
     * @since 0.1.0
     */
    public enum Right {

        /**
         * Access to the data and to the information about its processing (article 15).
         */
        ACCESS("15"),

        /**
         * Rectification of inaccurate data (article 16).
         */
        RECTIFICATION("16"),

        /**
         * Erasure (article 17).
         */
        ERASURE("17"),

        /**
         * Restriction of processing (article 18).
         */
        RESTRICTION("18"),

        /**
         * Data portability (article 20).
         */
        PORTABILITY("20"),

        /**
         * Objection (article 21).
         */
        OBJECTION("21"),

        /**
         * Not to be subject to a decision based solely on automated processing (article 22).
         */
        AUTOMATED_DECISION("22");

        private final String article;

        Right(String article) {
            this.article = article;
        }

        /**
         * The GDPR article.
         *
         * @return an article number such as {@code 17}
         */
        public String article() {
            return article;
        }
    }

    /**
     * Whether a right applies to an activity.
     *
     * @since 0.1.0
     */
    public enum Availability {

        /**
         * It applies.
         */
        YES,

        /**
         * It applies in the cases the article lists; the explanation says which.
         */
        CONDITIONAL,

        /**
         * It does not apply to this activity, as the explanation says.
         */
        NO
    }

    /**
     * A right as it applies to one activity.
     *
     * @param right the right
     * @param availability whether it applies
     * @param explanation why, with the provisions, in English
     * @since 0.1.0
     */
    public record Entitlement(Right right, Availability availability, String explanation) {

        /**
         * Checks the components.
         *
         * @param right the right
         * @param availability whether it applies
         * @param explanation why
         */
        public Entitlement {
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(availability, "availability");
            Objects.requireNonNull(explanation, "explanation");
        }
    }

    /**
     * The rights a data subject has over one activity.
     *
     * @param activity the activity, as checked in the record
     * @param entitlements one entitlement per {@link Right}, in the order of the rights
     * @since 0.1.0
     */
    public record ActivityRights(CheckedActivity activity, List<Entitlement> entitlements) {

        /**
         * Checks the components and makes the list immutable.
         *
         * @param activity the activity
         * @param entitlements the entitlements
         */
        public ActivityRights {
            Objects.requireNonNull(activity, "activity");
            entitlements = List.copyOf(entitlements);
        }

        /**
         * The entitlement of one right.
         *
         * @param right the right
         * @return its entitlement for this activity
         */
        public Entitlement entitlement(Right right) {
            return entitlements.stream().filter(e -> e.right() == right).findFirst().orElseThrow();
        }
    }

    private final String dataSubject;
    private final List<ActivityRights> activities;

    private SubjectRights(String dataSubject, List<ActivityRights> activities) {
        this.dataSubject = dataSubject;
        this.activities = List.copyOf(activities);
    }

    /**
     * The rights of a category of data subjects under the GDPR alone.
     *
     * @param record the record of processing activities
     * @param taxonomy the taxonomy the record was checked with, to resolve the key
     * @param dataSubject the key of the data subject category, such as {@code student}; a deprecated key stands for
     *        its replacement
     * @return the rights, over every activity whose data subjects include that category
     * @throws TaxonomyException if the taxonomy has no such data subject
     * @throws IllegalArgumentException if an activity of the record has no lawful basis
     */
    public static SubjectRights of(ProcessingRecord record, Taxonomy taxonomy, String dataSubject) {
        return of(record, taxonomy, dataSubject, Jurisdiction.EU);
    }

    /**
     * The rights of a category of data subjects in a jurisdiction.
     *
     * @param record the record of processing activities
     * @param taxonomy the taxonomy the record was checked with, to resolve the key
     * @param dataSubject the key of the data subject category, such as {@code student}; a deprecated key stands for
     *        its replacement
     * @param jurisdiction the law that applies besides the GDPR
     * @return the rights, over every activity whose data subjects include that category
     * @throws TaxonomyException if the taxonomy has no such data subject
     * @throws IllegalArgumentException if an activity of the record has no lawful basis
     */
    public static SubjectRights of(ProcessingRecord record, Taxonomy taxonomy, String dataSubject,
                                   Jurisdiction jurisdiction) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(jurisdiction, "jurisdiction");
        String key = Objects.requireNonNull(taxonomy, "taxonomy")
                .resolveDataSubject(Objects.requireNonNull(dataSubject, "dataSubject")).key();
        List<ActivityRights> activities = new ArrayList<>();
        for (CheckedActivity checked : record.activities()) {
            if (checked.dataSubjects().stream().anyMatch(subject -> subject.key().equals(key))) {
                activities.add(new ActivityRights(checked, entitlements(checked, jurisdiction)));
            }
        }
        return new SubjectRights(key, activities);
    }

    /**
     * The data subject category.
     *
     * @return its key
     */
    public String dataSubject() {
        return dataSubject;
    }

    /**
     * The activities that process data of the category, with the rights over each.
     *
     * @return in the order of the record
     */
    public List<ActivityRights> activities() {
        return activities;
    }

    /**
     * The rights over one activity.
     *
     * @param activity the activity key
     * @return its rights, or empty if the activity does not process data of the category
     */
    public Optional<ActivityRights> activity(String activity) {
        return activities.stream().filter(a -> a.activity().activity().key().equals(activity)).findFirst();
    }

    /**
     * Whether no activity processes data of the category.
     *
     * @return {@code true} if there is nothing to answer but that
     */
    public boolean isEmpty() {
        return activities.isEmpty();
    }

    /**
     * The personal data categories processed, across the activities.
     *
     * @return in key order
     */
    public List<DataCategory> dataCategories() {
        Map<String, DataCategory> categories = new TreeMap<>();
        activities.forEach(a -> a.activity().dataCategories().forEach(c -> categories.put(c.key(), c)));
        return List.copyOf(categories.values());
    }

    /**
     * The objects of a data inventory the activities name, where the person's data is stored.
     *
     * @return their keys, sorted
     */
    public Set<String> objects() {
        Set<String> objects = new TreeSet<>();
        activities.forEach(a -> objects.addAll(a.activity().activity().objects()));
        return Collections.unmodifiableSet(objects);
    }

    /**
     * The entity classes the activities name, where the person's data is stored.
     *
     * @return in the order of the activities, each once
     */
    public List<Class<?>> entities() {
        Set<Class<?>> entities = new LinkedHashSet<>();
        activities.forEach(a -> entities.addAll(a.activity().activity().entities()));
        return List.copyOf(entities);
    }

    /**
     * The fields that hold personal data in the {@linkplain #objects() objects} of the activities: where to look
     * for the person's data to give access to it, export it, rectify it or erase it.
     *
     * @param inventory the data inventory the activities name objects of
     * @param taxonomy the taxonomy to resolve the stored keys with
     * @return the personal fields by object, objects sorted and fields in the order of the inventory; an object the
     *         inventory does not hold has no entry
     * @throws TaxonomyException if the taxonomy does not contain a key of the inventory
     */
    public Map<String, List<InventoryField>> storedFields(DataInventory inventory, Taxonomy taxonomy) {
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(taxonomy, "taxonomy");
        Map<String, List<InventoryField>> fields = new LinkedHashMap<>();
        for (String object : objects()) {
            List<InventoryField> personal = inventory.fields(object).stream()
                    .filter(field -> field.isPersonal(taxonomy)).toList();
            if (!personal.isEmpty()) {
                fields.put(object, personal);
            }
        }
        return Collections.unmodifiableMap(fields);
    }

    @Override
    public String toString() {
        return "SubjectRights[" + dataSubject + ": " + activities.stream()
                .map(a -> a.activity().activity().key()).toList() + "]";
    }

    private static List<Entitlement> entitlements(CheckedActivity checked, Jurisdiction jurisdiction) {
        Governance governance = checked.activity().governance();
        LegalBasis basis = governance.legalBasis();
        if (basis == null) {
            throw new IllegalArgumentException("The processing activity '" + checked.activity().key() + "' has no"
                    + " lawful basis; build the record with ProcessingChecker.verify");
        }
        boolean marketing = checked.purposes().stream().anyMatch(use -> use.has(Trait.DIRECT_MARKETING));
        boolean onlyMarketing = marketing && checked.purposes().stream().allMatch(u -> u.has(Trait.DIRECT_MARKETING));
        boolean research = checked.purposes().stream().anyMatch(use -> use.has(Trait.RESEARCH));
        boolean automated = checked.activity().allRiskFactors().contains(ImpactScreening.Factor.AUTOMATED_DECISIONS);
        boolean explicitConsent = governance.specialCategoryConditions()
                .contains(SpecialCategoryCondition.EXPLICIT_CONSENT);
        boolean otherConditions = governance.specialCategoryConditions().size() > (explicitConsent ? 1 : 0);
        String blocking = jurisdiction == Jurisdiction.SPAIN
                ? " Under Spanish law the data is blocked, not deleted, while liabilities may arise from the"
                + " processing, and deleted afterwards (LOPDGDD art. 32)." : "";
        String retention = governance.retention() == null ? "not declared" : governance.retention();
        List<Entitlement> entitlements = new ArrayList<>();
        entitlements.add(new Entitlement(Right.ACCESS, Availability.YES, "Art. 15: confirmation, a copy of the data"
                + " and the purposes, categories, recipients, transfers and retention of this activity ("
                + terms(checked.purposes()) + ")" + (automated ? ", and the automated decisions it makes, their"
                + " logic and consequences (art. 15.1.h)" : "") + "."));
        entitlements.add(new Entitlement(Right.RECTIFICATION, Availability.YES,
                "Art. 16: inaccurate data is rectified and incomplete data completed." + blocking));
        entitlements.add(erasure(basis, marketing, onlyMarketing, research, explicitConsent, otherConditions,
                retention, blocking));
        entitlements.add(new Entitlement(Right.RESTRICTION, Availability.CONDITIONAL, "Art. 18.1: while the accuracy"
                + " of the data or an objection is being verified, when the processing is unlawful and the person"
                + " prefers restriction, or when the data is no longer needed but the person needs it for legal"
                + " claims."));
        String portable = " the person may receive the data they provided, processed by automated means, in a"
                + " structured, machine-readable format, or have it sent to another controller.";
        if (basis == LegalBasis.CONSENT || basis == LegalBasis.CONTRACT) {
            entitlements.add(new Entitlement(Right.PORTABILITY, Availability.YES, "Art. 20.1: the processing is"
                    + " based on " + label(basis) + ";" + portable));
        } else if (basis == LegalBasis.PUBLIC_INTEREST) {
            entitlements.add(new Entitlement(Right.PORTABILITY, Availability.NO, "Art. 20.3: portability does not"
                    + " apply to processing necessary for a task in the public interest."));
        } else if (explicitConsent) {
            entitlements.add(new Entitlement(Right.PORTABILITY, Availability.CONDITIONAL, "Art. 20.1.a: the special"
                    + " category data is processed with explicit consent (art. 9.2.a);" + portable + " The rest rests"
                    + " on " + label(basis) + ", to which portability does not apply."));
        } else {
            entitlements.add(new Entitlement(Right.PORTABILITY, Availability.NO, "Art. 20.1: portability applies to"
                    + " processing based on consent or a contract; this activity rests on " + label(basis) + "."));
        }
        entitlements.add(objection(basis, marketing, research));
        entitlements.add(automated ? new Entitlement(Right.AUTOMATED_DECISION, Availability.CONDITIONAL, "Art. 22:"
                + " the person is not subject to a decision based solely on automated processing with legal or"
                + " similarly significant effects, unless it is necessary for a contract, authorised by law or based"
                + " on explicit consent; then the person may obtain human intervention, express their view and"
                + " contest the decision (art. 22.3).")
                : new Entitlement(Right.AUTOMATED_DECISION, Availability.NO, "Art. 22: the activity declares no"
                + " decision based solely on automated processing."));
        return entitlements;
    }

    private static Entitlement erasure(LegalBasis basis, boolean marketing, boolean onlyMarketing, boolean research,
                                       boolean explicitConsent, boolean otherConditions, String retention,
                                       String blocking) {
        Entitlement entitlement = erasure(basis, marketing, onlyMarketing, retention, blocking);
        if (research && entitlement.availability() != Availability.NO) {
            entitlement = new Entitlement(Right.ERASURE, Availability.CONDITIONAL, entitlement.explanation() + " For"
                    + " research, erasure may be refused where it would make the research impossible or seriously"
                    + " impair it (art. 17.3.d).");
        }
        if (!explicitConsent || basis == LegalBasis.CONSENT || entitlement.availability() == Availability.YES) {
            return entitlement;
        }
        return new Entitlement(Right.ERASURE, Availability.CONDITIONAL, entitlement.explanation() + " The special"
                + " category data, processed with explicit consent, is erased when the consent is withdrawn"
                + (otherConditions ? ", unless another condition of article 9.2 declared for the activity covers it"
                : "") + " (art. 17.1.b with 9.2.a).");
    }

    private static Entitlement erasure(LegalBasis basis, boolean marketing, boolean onlyMarketing, String retention,
                                       String blocking) {
        boolean kept = basis == LegalBasis.LEGAL_OBLIGATION || basis == LegalBasis.PUBLIC_INTEREST;
        if (marketing && (kept || !onlyMarketing)) {
            return new Entitlement(Right.ERASURE, Availability.CONDITIONAL, "Art. 17.1.c and 21.3: after an objection"
                    + " to direct marketing the data is no longer processed for it and is erased, except what "
                    + (kept ? (basis == LegalBasis.LEGAL_OBLIGATION ? "the legal obligation" : "the task in the"
                    + " public interest") + " still requires (art. 17.3.b; retention: " + retention + ")"
                    : "the activity's other purposes still need") + "." + blocking);
        }
        if (marketing) {
            return new Entitlement(Right.ERASURE, Availability.YES, "Art. 17.1.c and 21.2: the person may object to"
                    + " direct marketing at any time, and the data is then erased." + blocking);
        }
        return switch (basis) {
            case CONSENT -> new Entitlement(Right.ERASURE, Availability.YES, "Art. 17.1.b: on withdrawal of"
                    + " consent, unless another lawful basis applies." + blocking);
            case LEGAL_OBLIGATION, PUBLIC_INTEREST -> new Entitlement(Right.ERASURE, Availability.NO,
                    "Art. 17.3.b: the data is kept while " + (basis == LegalBasis.LEGAL_OBLIGATION
                    ? "the legal obligation" : "the task in the public interest") + " requires it (retention: "
                    + retention + "), and erased afterwards." + blocking);
            case LEGITIMATE_INTERESTS -> new Entitlement(Right.ERASURE, Availability.CONDITIONAL, "Art. 17.1.c:"
                    + " after an objection, unless there are overriding legitimate grounds; and once the data is no"
                    + " longer needed (retention: " + retention + ")." + blocking);
            case CONTRACT, VITAL_INTERESTS -> new Entitlement(Right.ERASURE, Availability.CONDITIONAL,
                    "Art. 17.1.a: once the data is no longer needed for the purposes (retention: " + retention + ")."
                    + blocking);
        };
    }

    private static Entitlement objection(LegalBasis basis, boolean marketing, boolean research) {
        if (marketing) {
            return new Entitlement(Right.OBJECTION, Availability.YES, "Art. 21.2 and 21.3: the person may object at"
                    + " any time to processing for direct marketing, which then stops.");
        }
        if (research) {
            return basis == LegalBasis.PUBLIC_INTEREST
                    ? new Entitlement(Right.OBJECTION, Availability.NO, "Art. 21.6: objection to research does not"
                    + " apply where the research is necessary for a task in the public interest.")
                    : new Entitlement(Right.OBJECTION, Availability.CONDITIONAL, "Art. 21.6: on grounds relating to"
                    + " the person's particular situation, the person may object to processing for research or"
                    + " statistics.");
        }
        if (basis == LegalBasis.PUBLIC_INTEREST || basis == LegalBasis.LEGITIMATE_INTERESTS) {
            return new Entitlement(Right.OBJECTION, Availability.CONDITIONAL, "Art. 21.1: on grounds relating to the"
                    + " person's particular situation; the processing stops unless the controller shows compelling"
                    + " legitimate grounds or needs the data for legal claims.");
        }
        return new Entitlement(Right.OBJECTION, Availability.NO, "Art. 21.1: objection applies to processing based"
                + " on a task in the public interest or legitimate interests; this activity rests on " + label(basis)
                + ".");
    }

    private static String terms(List<? extends Term> terms) {
        return String.join("; ", terms.stream().sorted(Comparator.comparing(Term::key)).map(Term::name).toList());
    }

    private static String label(LegalBasis basis) {
        return basis.fideslangName().toLowerCase(Locale.ROOT) + " (art. " + basis.article() + ")";
    }
}
