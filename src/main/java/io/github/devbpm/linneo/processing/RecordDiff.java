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

import io.github.devbpm.linneo.Protection.Direction;
import io.github.devbpm.linneo.processing.ActivityChange.Aspect;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The differences between two {@linkplain ProcessingRecord records of processing activities}, activity by activity.
 * <p>
 * Both records are values an application computes, so the earlier one is rebuilt from what it stored: the governance
 * the data protection function keeps, and the {@link io.github.devbpm.linneo.classify.DataInventory} and
 * {@link io.github.devbpm.linneo.Taxonomy} of the record that was approved. Checked against what applies now, the
 * same activities show what a new taxonomy, a change of the data model or an edit of the governance does to the
 * record, before anyone signs it:
 * <pre>{@code
 * ProcessingRecord approved = new ProcessingChecker(appliedTaxonomy, Jurisdiction.SPAIN, storedInventory)
 *         .verify(activities);
 * ProcessingRecord current = new ProcessingChecker(taxonomy, Jurisdiction.SPAIN, declared).verify(activities);
 * for (ActivityChange change : RecordDiff.between(approved, current).changes()) { ... }
 * }</pre>
 * An empty diff means the records have the same {@linkplain ProcessingRecord#fingerprint() fingerprint}. Instances
 * are immutable.
 *
 * @since 0.1.0
 */
public final class RecordDiff {

    private final List<ActivityChange> changes;

    private RecordDiff(List<ActivityChange> changes) {
        this.changes = List.copyOf(changes);
    }

    /**
     * Compares two records.
     *
     * @param before the earlier record, such as the one the data protection officer approved
     * @param after the later record
     * @return the differences, by activity key
     */
    public static RecordDiff between(ProcessingRecord before, ProcessingRecord after) {
        Map<String, CheckedActivity> was = byKey(Objects.requireNonNull(before, "before"));
        Map<String, CheckedActivity> is = byKey(Objects.requireNonNull(after, "after"));
        Set<String> keys = new TreeSet<>(was.keySet());
        keys.addAll(is.keySet());
        List<ActivityChange> changes = new ArrayList<>();
        for (String key : keys) {
            CheckedActivity earlier = was.get(key);
            CheckedActivity later = is.get(key);
            if (earlier == null) {
                changes.add(new ActivityChange(key, null, later, EnumSet.of(Aspect.ADDED), Direction.SAME));
            } else if (later == null) {
                changes.add(new ActivityChange(key, earlier, null, EnumSet.of(Aspect.REMOVED), Direction.SAME));
            } else {
                Map<Aspect, List<String>> then = ActivityChange.content(earlier);
                Map<Aspect, List<String>> now = ActivityChange.content(later);
                Set<Aspect> aspects = EnumSet.noneOf(Aspect.class);
                then.forEach((aspect, content) -> {
                    if (!content.equals(now.get(aspect))) {
                        aspects.add(aspect);
                    }
                });
                if (!aspects.isEmpty()) {
                    changes.add(new ActivityChange(key, earlier, later, aspects,
                            later.protection().comparedTo(earlier.protection())));
                }
            }
        }
        return new RecordDiff(changes);
    }

    /**
     * The activity changes, by activity key.
     *
     * @return an immutable list
     */
    public List<ActivityChange> changes() {
        return changes;
    }

    /**
     * The change of one activity.
     *
     * @param activity the activity key
     * @return the change, or empty if the activity is the same in both records or in neither
     */
    public Optional<ActivityChange> change(String activity) {
        return changes.stream().filter(change -> change.activity().equals(activity)).findFirst();
    }

    /**
     * Whether the records are the same.
     *
     * @return {@code true} if no activity was added, removed or changed
     */
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    @Override
    public String toString() {
        return "RecordDiff" + changes;
    }

    private static Map<String, CheckedActivity> byKey(ProcessingRecord record) {
        Map<String, CheckedActivity> activities = new TreeMap<>();
        record.activities().forEach(checked -> activities.put(checked.activity().key(), checked));
        return activities;
    }
}
