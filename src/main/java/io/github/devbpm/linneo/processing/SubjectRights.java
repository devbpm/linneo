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
 * rights (GDPR articles 15 to 21): the activities that process their data, what each one processes and why, where
 * that data is stored, and which rights each activity's lawful basis gives them.
 * <pre>{@code
 * SubjectRights rights = SubjectRights.of(record, DataSubjects.STUDENT, Jurisdiction.SPAIN);
 * rights.storedFields(inventory, taxonomy);   // where to look for the person's data, object by object
 * rights.activities().forEach(activity -> activity.entitlement(Right.ERASURE));
 * }</pre>
 * The rights follow what the GDPR provides for each lawful basis: erasure is not available while a legal obligation
 * or a public task needs the data (article 17.3.b), portability applies to processing based on consent or a
 * contract (article 20.1), objection to processing based on a public task or legitimate interests (article 21.1) and
 * always to direct marketing (article 21.2). It is the starting point of the answer, not the answer: the data
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
        OBJECTION("21");

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
     * @param dataSubject the key of the data subject category, such as {@code student}
     * @return the rights, over every activity whose data subjects include that category
     */
    public static SubjectRights of(ProcessingRecord record, String dataSubject) {
        return of(record, dataSubject, Jurisdiction.EU);
    }

    /**
     * The rights of a category of data subjects in a jurisdiction.
     *
     * @param record the record of processing activities
     * @param dataSubject the key of the data subject category, such as {@code student}
     * @param jurisdiction the law that applies besides the GDPR
     * @return the rights, over every activity whose data subjects include that category
     */
    public static SubjectRights of(ProcessingRecord record, String dataSubject, Jurisdiction jurisdiction) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(dataSubject, "dataSubject");
        Objects.requireNonNull(jurisdiction, "jurisdiction");
        List<ActivityRights> activities = new ArrayList<>();
        for (CheckedActivity checked : record.activities()) {
            if (checked.dataSubjects().stream().anyMatch(subject -> subject.key().equals(dataSubject))) {
                activities.add(new ActivityRights(checked, entitlements(checked, jurisdiction)));
            }
        }
        return new SubjectRights(dataSubject, activities);
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
        boolean marketing = checked.purposes().stream().map(DataUse::key)
                .anyMatch(key -> key.equals("marketing") || key.startsWith("marketing."));
        String blocking = jurisdiction == Jurisdiction.SPAIN
                ? " Under Spanish law the data is blocked, not deleted, while liabilities may arise from the"
                + " processing, and deleted afterwards (LOPDGDD art. 32)." : "";
        String retention = governance.retention() == null ? "not declared" : governance.retention();
        List<Entitlement> entitlements = new ArrayList<>();
        entitlements.add(new Entitlement(Right.ACCESS, Availability.YES, "Art. 15: confirmation, a copy of the data"
                + " and the purposes, categories, recipients, transfers and retention of this activity ("
                + terms(checked.purposes()) + ")."));
        entitlements.add(new Entitlement(Right.RECTIFICATION, Availability.YES,
                "Art. 16: inaccurate data is rectified and incomplete data completed." + blocking));
        entitlements.add(erasure(basis, marketing, retention, blocking));
        entitlements.add(new Entitlement(Right.RESTRICTION, Availability.CONDITIONAL, "Art. 18.1: while the accuracy"
                + " of the data or an objection is being verified, when the processing is unlawful and the person"
                + " prefers restriction, or when the data is no longer needed but the person needs it for legal"
                + " claims."));
        entitlements.add(basis == LegalBasis.CONSENT || basis == LegalBasis.CONTRACT
                ? new Entitlement(Right.PORTABILITY, Availability.YES, "Art. 20.1: the processing is based on "
                + label(basis) + "; the person may receive the data they provided, processed by automated means,"
                + " in a structured, machine-readable format, or have it sent to another controller.")
                : new Entitlement(Right.PORTABILITY, Availability.NO, "Art. 20.1: portability applies to processing"
                + " based on consent or a contract; this activity rests on " + label(basis) + "."));
        entitlements.add(objection(basis, marketing));
        return entitlements;
    }

    private static Entitlement erasure(LegalBasis basis, boolean marketing, String retention, String blocking) {
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

    private static Entitlement objection(LegalBasis basis, boolean marketing) {
        if (marketing) {
            return new Entitlement(Right.OBJECTION, Availability.YES, "Art. 21.2 and 21.3: the person may object at"
                    + " any time to processing for direct marketing, which then stops.");
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
