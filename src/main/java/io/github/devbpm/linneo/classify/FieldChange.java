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

import io.github.devbpm.linneo.Protection.Direction;

import java.util.Objects;

/**
 * How one field differs between two {@linkplain DataInventory inventories}, as found by {@link InventoryDiff}.
 *
 * @param object the object key
 * @param path the field path
 * @param kind whether the field was added, removed or changed
 * @param before the field in the earlier inventory, or {@code null} if it was added
 * @param after the field in the later inventory, or {@code null} if it was removed
 * @param direction how the protection of the field changed, with its identifiability; {@link Direction#SAME} for
 *        added and removed fields, and {@link Direction#MIXED} when a key cannot be resolved
 * @since 0.1.0
 */
public record FieldChange(String object, String path, Kind kind, InventoryField before, InventoryField after,
                          Direction direction) {

    /**
     * Checks the components.
     *
     * @param object the object key
     * @param path the field path
     * @param kind the kind of change
     * @param before the earlier field or {@code null}
     * @param after the later field or {@code null}
     * @param direction how the protection changed
     */
    public FieldChange {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(direction, "direction");
        if ((before == null) != (kind == Kind.ADDED) || (after == null) != (kind == Kind.REMOVED)) {
            throw new IllegalArgumentException(object + "." + path + ": " + kind
                    + " does not match the fields before and after");
        }
    }

    /**
     * Whether the change lowers some protection and deserves a review: a weaker or mixed classification, including
     * a classified field that became exempt or a value declared anonymized.
     *
     * @return {@code true} if {@link #direction()} {@linkplain Direction#weakens() weakens}
     */
    public boolean weakens() {
        return direction.weakens();
    }

    @Override
    public String toString() {
        return kind + " " + object + "." + path + (direction == Direction.SAME ? "" : " " + direction)
                + (after != null ? ": " + after : before != null ? ": was " + before : "");
    }

    /**
     * What happened to a field.
     *
     * @since 0.1.0
     */
    public enum Kind {

        /**
         * The field is new in the later inventory. A new personal field means new personal data to record.
         */
        ADDED,

        /**
         * The field is no longer in the later inventory, not even as exempted.
         */
        REMOVED,

        /**
         * The field is in both and something it declares changed, including becoming exempt or no longer being
         * exempt.
         */
        CHANGED
    }
}
