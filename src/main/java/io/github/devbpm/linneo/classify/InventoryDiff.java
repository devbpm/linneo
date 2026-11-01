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

import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Protection.Direction;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.classify.FieldChange.Kind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The differences between two {@linkplain DataInventory inventories}, field by field, such as what an application
 * stored and what its code declares now.
 * <p>
 * It is the plan of a reconciliation: Linneo compares, the application applies. Each {@link FieldChange} says what
 * happened to a field and how its protection moved under the given taxonomy, so the application can insert the new
 * fields, retire the removed ones instead of deleting them, rewrite the changed ones, and ask for a review of every
 * change that {@linkplain FieldChange#weakens() weakens} protection.
 * <pre>{@code
 * DataInventory declared = classifier.inventory(entities, type -> keyOf(type));
 * if (!declared.fingerprint().equals(storedFingerprint)) {
 *     InventoryDiff diff = InventoryDiff.between(storedInventory(), declared, taxonomy);
 *     for (FieldChange change : diff.changes()) { ... }
 * }
 * }</pre>
 * Both inventories are compared under the same taxonomy, so the changes come from the code; the changes that come
 * from the taxonomy are found with {@link DataInventory#affectedBy(io.github.devbpm.linneo.TaxonomyDiff)}. An empty
 * diff means the inventories are equal. Instances are immutable.
 *
 * @since 0.1.0
 */
public final class InventoryDiff {

    private final List<FieldChange> changes;
    private final List<String> addedObjects;
    private final List<String> removedObjects;

    private InventoryDiff(List<FieldChange> changes, List<String> addedObjects, List<String> removedObjects) {
        this.changes = List.copyOf(changes);
        this.addedObjects = List.copyOf(addedObjects);
        this.removedObjects = List.copyOf(removedObjects);
    }

    /**
     * Compares two inventories.
     *
     * @param before the earlier inventory, such as what an application stored
     * @param after the later inventory, such as what its code declares now
     * @param taxonomy the taxonomy to resolve keys with when telling how protection changed
     * @return the differences, by object and then by path
     */
    public static InventoryDiff between(DataInventory before, DataInventory after, Taxonomy taxonomy) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(taxonomy, "taxonomy");
        Set<String> objects = new TreeSet<>(before.objects());
        objects.addAll(after.objects());
        List<FieldChange> changes = new ArrayList<>();
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        for (String object : objects) {
            if (!before.objects().contains(object)) {
                added.add(object);
            } else if (!after.objects().contains(object)) {
                removed.add(object);
            }
            Set<String> paths = new TreeSet<>();
            before.fields(object).forEach(field -> paths.add(field.path()));
            after.fields(object).forEach(field -> paths.add(field.path()));
            for (String path : paths) {
                InventoryField was = before.field(object, path).orElse(null);
                InventoryField is = after.field(object, path).orElse(null);
                if (was == null) {
                    changes.add(new FieldChange(object, path, Kind.ADDED, null, is, Direction.SAME));
                } else if (is == null) {
                    changes.add(new FieldChange(object, path, Kind.REMOVED, was, null, Direction.SAME));
                } else if (!was.equals(is)) {
                    changes.add(new FieldChange(object, path, Kind.CHANGED, was, is, direction(was, is, taxonomy)));
                }
            }
        }
        return new InventoryDiff(changes, added, removed);
    }

    /**
     * The field changes, by object and then by path.
     *
     * @return an immutable list
     */
    public List<FieldChange> changes() {
        return changes;
    }

    /**
     * The changes that lower some protection and deserve a review.
     *
     * @return the changes that {@linkplain FieldChange#weakens() weaken} protection
     */
    public List<FieldChange> weakening() {
        return changes.stream().filter(FieldChange::weakens).toList();
    }

    /**
     * The objects only the later inventory holds.
     *
     * @return their keys, sorted
     */
    public List<String> addedObjects() {
        return addedObjects;
    }

    /**
     * The objects only the earlier inventory holds.
     *
     * @return their keys, sorted
     */
    public List<String> removedObjects() {
        return removedObjects;
    }

    /**
     * The objects with any change, added and removed ones included: the ones to reconcile.
     *
     * @return their keys, sorted
     */
    public Set<String> changedObjects() {
        Set<String> objects = new TreeSet<>(addedObjects);
        objects.addAll(removedObjects);
        changes.forEach(change -> objects.add(change.object()));
        return objects;
    }

    /**
     * Whether the inventories are equal.
     *
     * @return {@code true} if no object or field differs
     */
    public boolean isEmpty() {
        return changes.isEmpty() && addedObjects.isEmpty() && removedObjects.isEmpty();
    }

    @Override
    public String toString() {
        return "InventoryDiff[" + changes.size() + " field change(s), " + weakening().size() + " weakening]";
    }

    // The protection, with the identifiability between personal values, compared under one taxonomy.
    private static Direction direction(InventoryField was, InventoryField is, Taxonomy taxonomy) {
        Protection before;
        Protection after;
        try {
            before = was.protection(taxonomy);
            after = is.protection(taxonomy);
        } catch (TaxonomyException e) {
            return Direction.MIXED;
        }
        Direction direction = after.comparedTo(before);
        if (before.isPersonal() && after.isPersonal()) {
            int change = Integer.compare(rank(is.identifiability()), rank(was.identifiability()));
            direction = direction.and(change > 0 ? Direction.TIGHTENED
                    : change < 0 ? Direction.LOOSENED : Direction.SAME);
        }
        return direction;
    }

    private static int rank(Identifiability identifiability) {
        return identifiability == Identifiability.PSEUDONYMIZED ? 0 : 1;
    }
}
