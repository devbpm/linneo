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
import io.github.devbpm.linneo.TermChange.Aspect;
import io.github.devbpm.linneo.TermChange.Hierarchy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The differences between two versions of a taxonomy, term by term.
 * <p>
 * Applications use it when the taxonomy they run with is not the one they applied last: they keep the applied one
 * with {@link Taxonomy#toTsv()}, read it back with {@link Taxonomy#fromTsv(String)} and compare. Each
 * {@link TermChange} says what changed and how the protection of a data category moved, so an application can
 * migrate the keys it stores (a deprecated key to its replacement), record one audit entry per changed term instead
 * of one per field, and refuse to start on a change it cannot accept. The same comparison enforces Linneo's own
 * versioning rules ({@link #requiredBump()}).
 * <pre>{@code
 * TaxonomyDiff diff = TaxonomyDiff.between(Taxonomy.fromTsv(applied), taxonomy);
 * diff.requireCompatible();                 // fails on a removed term or weaker protection
 * for (TermChange change : diff.changes()) { ... }
 * }</pre>
 * Instances are immutable.
 *
 * @since 0.1.0
 */
public final class TaxonomyDiff {

    private final Taxonomy before;
    private final Taxonomy after;
    private final List<TermChange> changes;

    private TaxonomyDiff(Taxonomy before, Taxonomy after, List<TermChange> changes) {
        this.before = before;
        this.after = after;
        this.changes = List.copyOf(changes);
    }

    /**
     * Compares two versions of a taxonomy.
     *
     * @param before the earlier version, such as the one an application applied last
     * @param after the later version, such as the one it runs with now
     * @return the differences, data categories first, then data uses and data subjects, each by key
     */
    public static TaxonomyDiff between(Taxonomy before, Taxonomy after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        List<TermChange> changes = new ArrayList<>();
        compare(Hierarchy.DATA_CATEGORY, before.categories(), after.categories(), changes);
        compare(Hierarchy.DATA_USE, before.dataUses(), after.dataUses(), changes);
        compare(Hierarchy.DATA_SUBJECT, before.dataSubjects(), after.dataSubjects(), changes);
        return new TaxonomyDiff(before, after, changes);
    }

    /**
     * The earlier version compared.
     *
     * @return the taxonomy given as {@code before}
     */
    public Taxonomy before() {
        return before;
    }

    /**
     * The later version compared.
     *
     * @return the taxonomy given as {@code after}
     */
    public Taxonomy after() {
        return after;
    }

    /**
     * The changes, data categories first, then data uses and data subjects, each by key.
     *
     * @return an immutable list
     */
    public List<TermChange> changes() {
        return changes;
    }

    /**
     * Whether the two versions have the same terms.
     *
     * @return {@code true} if there is no change
     */
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /**
     * The change of a term.
     *
     * @param hierarchy the hierarchy of the term
     * @param key the key
     * @return the change, or {@code null} if the term did not change
     */
    public TermChange change(Hierarchy hierarchy, String key) {
        return changes.stream().filter(c -> c.hierarchy() == hierarchy && c.key().equals(key)).findFirst()
                .orElse(null);
    }

    /**
     * The changes that are incompatible for applications that store or publish keys.
     *
     * @return the {@linkplain TermChange#isBreaking() breaking} changes
     */
    public List<TermChange> breakingChanges() {
        return changes.stream().filter(TermChange::isBreaking).toList();
    }

    /**
     * The version change the differences require: the strictest {@link TermChange#bump()}.
     *
     * @return {@link Bump#NONE} when there is no change
     */
    public Bump requiredBump() {
        return changes.stream().map(TermChange::bump).max(Enum::compareTo).orElse(Bump.NONE);
    }

    /**
     * Fails if a change is incompatible.
     *
     * @return this diff, when it has no breaking change
     * @throws TaxonomyException listing every breaking change
     */
    public TaxonomyDiff requireCompatible() {
        List<TermChange> breaking = breakingChanges();
        if (!breaking.isEmpty()) {
            throw new TaxonomyException(breaking.size() + " incompatible taxonomy change(s):" + breaking.stream()
                    .map(c -> "\n - " + c).collect(Collectors.joining()));
        }
        return this;
    }

    @Override
    public String toString() {
        return "TaxonomyDiff[" + changes.size() + " change(s), " + requiredBump() + "]";
    }

    private static <T extends Term> void compare(Hierarchy hierarchy, List<T> before, List<T> after,
                                                 List<TermChange> changes) {
        Map<String, T> old = index(before);
        Map<String, T> current = index(after);
        Set<String> keys = new TreeSet<>(old.keySet());
        keys.addAll(current.keySet());
        for (String key : keys) {
            T was = old.get(key);
            T is = current.get(key);
            if (was == null) {
                changes.add(new TermChange(hierarchy, key, null, is, EnumSet.of(Aspect.ADDED), Direction.SAME));
            } else if (is == null) {
                changes.add(new TermChange(hierarchy, key, was, null, EnumSet.of(Aspect.REMOVED), Direction.SAME));
            } else if (!was.equals(is)) {
                Set<Aspect> aspects = aspects(was, is);
                Direction direction = was instanceof DataCategory a && is instanceof DataCategory b
                        ? b.protection().comparedTo(a.protection()) : Direction.SAME;
                if (!aspects.isEmpty()) {
                    changes.add(new TermChange(hierarchy, key, was, is, aspects, direction));
                }
            }
        }
    }

    private static Set<Aspect> aspects(Term was, Term is) {
        TermInfo a = was.info();
        TermInfo b = is.info();
        Set<Aspect> aspects = EnumSet.noneOf(Aspect.class);
        if (was instanceof DataCategory c && is instanceof DataCategory d) {
            if (!c.protection().equals(d.protection())) {
                aspects.add(Aspect.PROTECTION);
            }
            if (!c.attributes().equals(d.attributes())) {
                aspects.add(Aspect.ATTRIBUTES);
            }
        }
        if (!was.traits().equals(is.traits())) {
            aspects.add(Aspect.TRAITS);
        }
        if (a.isDeprecated() != b.isDeprecated()) {
            aspects.add(Aspect.DEPRECATION);
        }
        if (!Objects.equals(a.replacedBy(), b.replacedBy())) {
            aspects.add(Aspect.REPLACEMENT);
        }
        if (!a.name().equals(b.name())) {
            aspects.add(Aspect.NAME);
        }
        if (!a.description().equals(b.description())) {
            aspects.add(Aspect.DESCRIPTION);
        }
        if (!a.origin().equals(b.origin())) {
            aspects.add(Aspect.ORIGIN);
        }
        if (!Objects.equals(a.since(), b.since()) || a.isDeprecated() == b.isDeprecated()
                && !Objects.equals(a.deprecatedSince(), b.deprecatedSince())) {
            aspects.add(Aspect.VERSIONS);
        }
        return aspects;
    }

    private static <T extends Term> Map<String, T> index(List<T> terms) {
        return terms.stream().collect(Collectors.toMap(Term::key, Function.identity(), (x, y) -> x, TreeMap::new));
    }

    /**
     * A version change under semantic versioning, as Linneo applies it to taxonomies.
     *
     * @since 0.1.0
     */
    public enum Bump {

        /**
         * No change.
         */
        NONE,

        /**
         * Descriptive changes only: names, descriptions, attributes, origins, lifecycle versions.
         */
        PATCH,

        /**
         * Compatible changes: added terms, tighter protection, deprecations, a replacement for a deprecated term
         * that had none.
         */
        MINOR,

        /**
         * Incompatible changes: removed terms, weaker or mixed protection, a deprecated term that now resolves to
         * another replacement.
         */
        MAJOR
    }
}
