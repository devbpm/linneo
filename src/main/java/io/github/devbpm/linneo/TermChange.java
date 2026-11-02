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
package io.github.devbpm.linneo;

import io.github.devbpm.linneo.Protection.Direction;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * How one term differs between two versions of a taxonomy, as found by {@link TaxonomyDiff}.
 *
 * @param hierarchy the hierarchy of the term
 * @param key the key of the term
 * @param before the term in the earlier version, or {@code null} if it was added
 * @param after the term in the later version, or {@code null} if it was removed
 * @param aspects what changed, never empty
 * @param direction how the protection of a data category changed; {@link Direction#SAME} for other terms and for
 *        added or removed ones
 * @since 0.1.0
 */
public record TermChange(Hierarchy hierarchy, String key, Term before, Term after, Set<Aspect> aspects,
                         Direction direction) {

    /**
     * Checks the components and makes the aspects immutable.
     *
     * @param hierarchy the hierarchy
     * @param key the key
     * @param before the earlier term or {@code null}
     * @param after the later term or {@code null}
     * @param aspects what changed
     * @param direction how the protection changed
     */
    public TermChange {
        Objects.requireNonNull(hierarchy, "hierarchy");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(direction, "direction");
        if (aspects == null || aspects.isEmpty()) {
            throw new IllegalArgumentException("A change of '" + key + "' needs at least one aspect");
        }
        if (before == null && after == null) {
            throw new IllegalArgumentException("A change of '" + key + "' needs the term before or after it");
        }
        aspects = Collections.unmodifiableSet(EnumSet.copyOf(aspects));
    }

    /**
     * The version change this change requires under Linneo's rules: removing a term or a trait, weakening the
     * protection of a category or changing what a deprecated key resolves to is incompatible; adding a term or a
     * trait, tightening protection and deprecating are compatible additions; the rest is descriptive.
     *
     * @return the strictest bump any of its aspects requires
     */
    public TaxonomyDiff.Bump bump() {
        TaxonomyDiff.Bump bump = TaxonomyDiff.Bump.NONE;
        for (Aspect aspect : aspects) {
            TaxonomyDiff.Bump needed = switch (aspect) {
                case REMOVED -> TaxonomyDiff.Bump.MAJOR;
                case PROTECTION -> direction.weakens() ? TaxonomyDiff.Bump.MAJOR : TaxonomyDiff.Bump.MINOR;
                case REPLACEMENT -> before.info().replacedBy() != null ? TaxonomyDiff.Bump.MAJOR
                        : TaxonomyDiff.Bump.MINOR;
                case TRAITS -> after.traits().containsAll(before.traits()) ? TaxonomyDiff.Bump.MINOR
                        : TaxonomyDiff.Bump.MAJOR;
                case ADDED, DEPRECATION -> TaxonomyDiff.Bump.MINOR;
                case NAME, DESCRIPTION, ATTRIBUTES, ORIGIN, VERSIONS -> TaxonomyDiff.Bump.PATCH;
            };
            if (needed.compareTo(bump) > 0) {
                bump = needed;
            }
        }
        return bump;
    }

    /**
     * Whether the change is incompatible for applications that store or publish the key.
     *
     * @return {@code true} if it requires a {@linkplain TaxonomyDiff.Bump#MAJOR major} version
     */
    public boolean isBreaking() {
        return bump() == TaxonomyDiff.Bump.MAJOR;
    }

    @Override
    public String toString() {
        return hierarchy + " '" + key + "': " + aspects + (direction == Direction.SAME ? "" : " " + direction);
    }

    /**
     * The three hierarchies of a taxonomy.
     *
     * @since 0.1.0
     */
    public enum Hierarchy {

        /**
         * {@link DataCategory} terms.
         */
        DATA_CATEGORY,

        /**
         * {@link DataUse} terms.
         */
        DATA_USE,

        /**
         * {@link DataSubject} terms.
         */
        DATA_SUBJECT
    }

    /**
     * What changed in a term.
     *
     * @since 0.1.0
     */
    public enum Aspect {

        /**
         * The term is new.
         */
        ADDED,

        /**
         * The term no longer exists. Applications that stored its key can no longer resolve it.
         */
        REMOVED,

        /**
         * The protection of a data category: its nature, confidentiality, integrity or secret flag. See
         * {@link TermChange#direction()}.
         */
        PROTECTION,

        /**
         * The term was deprecated, or is no longer deprecated.
         */
        DEPRECATION,

        /**
         * The key that replaces a deprecated term was set or changed, so the term resolves to another one.
         */
        REPLACEMENT,

        /**
         * The human-readable name.
         */
        NAME,

        /**
         * The description. It is only text, but a description that says something else may hide a change of
         * meaning, which is never allowed: review it.
         */
        DESCRIPTION,

        /**
         * The {@linkplain Trait traits} of the term, which Linneo's legal rules read: adding one makes them stricter,
         * removing one may lift an obligation.
         */
        TRAITS,

        /**
         * The application-defined attributes of a data category.
         */
        ATTRIBUTES,

        /**
         * Who defines the term, for example when a term moves from an extension to the standard taxonomy.
         */
        ORIGIN,

        /**
         * The versions recorded in the term's lifecycle, without a change of status.
         */
        VERSIONS
    }
}
