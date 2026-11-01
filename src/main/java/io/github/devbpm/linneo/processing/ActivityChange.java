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

import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Protection.Direction;
import io.github.devbpm.linneo.Term;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * How one processing activity differs between two {@linkplain ProcessingRecord records}, as found by
 * {@link RecordDiff}.
 *
 * @param activity the key of the activity
 * @param before the activity in the earlier record, or {@code null} if it was added
 * @param after the activity in the later record, or {@code null} if it was removed
 * @param aspects what changed in the record, never empty
 * @param direction how the protection the activity's personal data requires changed: {@link Direction#TIGHTENED}
 *        means the activity now processes data that needs more protection, which may call for other bases or an
 *        impact assessment; {@link Direction#SAME} for an added or removed activity
 * @since 0.1.0
 */
public record ActivityChange(String activity, CheckedActivity before, CheckedActivity after, Set<Aspect> aspects,
                             Direction direction) {

    /**
     * Checks the components and makes the aspects immutable.
     *
     * @param activity the activity key
     * @param before the earlier activity or {@code null}
     * @param after the later activity or {@code null}
     * @param aspects what changed
     * @param direction how the protection changed
     */
    public ActivityChange {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(direction, "direction");
        if (aspects == null || aspects.isEmpty()) {
            throw new IllegalArgumentException("A change of the activity '" + activity + "' needs an aspect");
        }
        if (before == null && after == null) {
            throw new IllegalArgumentException("A change of the activity '" + activity
                    + "' needs the activity before or after it");
        }
        aspects = Collections.unmodifiableSet(EnumSet.copyOf(aspects));
    }

    /**
     * Whether the change comes from the {@link Governance}: a change the data protection function made, with the
     * scope the code declares and the data it reaches unchanged. The open points that follow from it may change too.
     *
     * @return {@code true} if an aspect of the governance changed and nothing else did but the open points
     */
    public boolean isGovernance() {
        return aspects.stream().anyMatch(Aspect::isGovernance)
                && aspects.stream().allMatch(aspect -> aspect.isGovernance() || aspect == Aspect.OPEN_POINTS);
    }

    @Override
    public String toString() {
        return "activity '" + activity + "': " + aspects + (direction == Direction.SAME ? "" : " " + direction);
    }

    /**
     * What changed in an activity of the record.
     *
     * @since 0.1.0
     */
    public enum Aspect {

        /**
         * The activity is new in the record.
         */
        ADDED(false),

        /**
         * The activity is no longer in the record.
         */
        REMOVED(false),

        /**
         * The name.
         */
        NAME(false),

        /**
         * The description.
         */
        DESCRIPTION(false),

        /**
         * The purposes, deprecated keys replaced.
         */
        PURPOSES(false),

        /**
         * The categories of data subjects, deprecated keys replaced.
         */
        DATA_SUBJECTS(false),

        /**
         * The personal data categories processed, whether declared or reached through entities and objects.
         */
        PERSONAL_DATA(false),

        /**
         * The protection the personal data requires; see {@link ActivityChange#direction()}.
         */
        PROTECTION(false),

        /**
         * The lawful basis.
         */
        LEGAL_BASIS(true),

        /**
         * The legitimate interest pursued.
         */
        LEGITIMATE_INTEREST(true),

        /**
         * The article 9.2 condition.
         */
        SPECIAL_CATEGORY_CONDITION(true),

        /**
         * The article 10 basis.
         */
        CRIMINAL_DATA_BASIS(true),

        /**
         * The law the bases rest on.
         */
        LEGAL_REFERENCE(true),

        /**
         * The recipients.
         */
        RECIPIENTS(true),

        /**
         * The transfers to third countries or international organisations.
         */
        TRANSFERS(true),

        /**
         * The time limit for erasure.
         */
        RETENTION(true),

        /**
         * The impact assessment.
         */
        IMPACT_ASSESSMENT(true),

        /**
         * The risk factors the officer declares for the screening of the need for an impact assessment.
         */
        RISK_FACTORS(true),

        /**
         * The open points: the warnings of the activity.
         */
        OPEN_POINTS(false);

        private final boolean governance;

        Aspect(boolean governance) {
            this.governance = governance;
        }

        /**
         * Whether the aspect is part of the {@link Governance} rather than of the scope declared in code or the data
         * it reaches.
         *
         * @return {@code true} for the elements of {@link Governance}
         */
        public boolean isGovernance() {
            return governance;
        }
    }

    /**
     * The content of an activity in the record, aspect by aspect, each as a sorted list of texts: what
     * {@link RecordDiff} compares and {@link ProcessingRecord#fingerprint()} hashes, so that the two always agree.
     * Lists are sorted because their order is not part of the record. ADDED and REMOVED have no content.
     */
    static Map<Aspect, List<String>> content(CheckedActivity checked) {
        ProcessingActivity activity = checked.activity();
        Governance governance = activity.governance();
        Protection protection = checked.protection();
        Map<Aspect, List<String>> content = new EnumMap<>(Aspect.class);
        content.put(Aspect.NAME, value(activity.name()));
        content.put(Aspect.DESCRIPTION, value(activity.description()));
        content.put(Aspect.PURPOSES, sorted(checked.purposes().stream().map(Term::key)));
        content.put(Aspect.DATA_SUBJECTS, sorted(checked.dataSubjects().stream().map(Term::key)));
        content.put(Aspect.PERSONAL_DATA, sorted(checked.dataCategories().stream().map(Term::key)));
        List<String> levels = new ArrayList<>(sorted(protection.natures().stream().map(Enum::name)));
        levels.add("confidentiality " + protection.confidentiality());
        levels.add("integrity " + protection.integrity());
        levels.add("secret " + protection.secret());
        content.put(Aspect.PROTECTION, levels);
        content.put(Aspect.LEGAL_BASIS, value(governance.legalBasis()));
        content.put(Aspect.LEGITIMATE_INTEREST, value(governance.legitimateInterest()));
        content.put(Aspect.SPECIAL_CATEGORY_CONDITION, value(governance.specialCategoryCondition()));
        content.put(Aspect.CRIMINAL_DATA_BASIS, value(governance.criminalDataBasis()));
        content.put(Aspect.LEGAL_REFERENCE, value(governance.legalReference()));
        content.put(Aspect.RECIPIENTS, sorted(governance.recipients().stream()));
        content.put(Aspect.TRANSFERS, sorted(governance.transfers().stream()
                .map(transfer -> transfer.safeguard().name() + " " + transfer.destination())));
        content.put(Aspect.RETENTION, value(governance.retention()));
        content.put(Aspect.IMPACT_ASSESSMENT, value(governance.impactAssessment()));
        content.put(Aspect.RISK_FACTORS, sorted(governance.riskFactors().stream().map(Enum::name)));
        content.put(Aspect.OPEN_POINTS, sorted(checked.warnings().stream()
                .map(warning -> warning.code() + " " + warning.element() + " " + warning.message())));
        return content;
    }

    private static List<String> value(Object value) {
        return value == null ? List.of() : List.of(value instanceof Enum<?> e ? e.name() : value.toString());
    }

    private static List<String> sorted(Stream<String> values) {
        return values.sorted().toList();
    }
}
