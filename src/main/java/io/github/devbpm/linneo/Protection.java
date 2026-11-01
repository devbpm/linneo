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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * The protection some data requires: its legal natures, its confidentiality and integrity, and whether it is a
 * secret. It is the one way Linneo compares protection, whether between a category and its replacement, a field and
 * the fields it is derived from, a classification decided in code and one raised later by the data protection
 * officer, or two versions of the same classification.
 * <p>
 * Protections are partially ordered by {@link #covers(Protection)}: one covers another when it is at least as strict
 * in every facet. The natures are ordered by {@link LegalNature#covers(LegalNature)}, where special category data
 * and criminal offence data are both stricter than ordinary personal data but not comparable with each other. That
 * is why a protection holds a set of natures: the protection of a record with health data and criminal records
 * keeps both, and covers neither one alone. The set is kept to its strictest natures, so equal protections are
 * equal records.
 *
 * @param natures the strictest legal natures of the data, never empty; {@link LegalNature#NONE} alone for data that
 *        is not personal
 * @param confidentiality the confidentiality level
 * @param integrity the integrity level
 * @param secret whether the data grants access and must never be read back, exported or published
 * @since 0.1.0
 */
public record Protection(Set<LegalNature> natures, Confidentiality confidentiality, Integrity integrity,
                         boolean secret) {

    /**
     * The lowest protection: data that is not personal, public, of low integrity and not a secret. Every protection
     * covers it, and {@linkplain #join(Protection) joining} it changes nothing.
     */
    public static final Protection LOWEST = of(LegalNature.NONE, Confidentiality.PUBLIC, Integrity.LOW, false);

    /**
     * Keeps the strictest natures and makes the set immutable.
     *
     * @param natures the legal natures, not empty
     * @param confidentiality the confidentiality level
     * @param integrity the integrity level
     * @param secret whether the data is a secret
     * @throws IllegalArgumentException if {@code natures} is empty
     */
    public Protection {
        Objects.requireNonNull(natures, "natures");
        Objects.requireNonNull(confidentiality, "confidentiality");
        Objects.requireNonNull(integrity, "integrity");
        if (natures.isEmpty()) {
            throw new IllegalArgumentException("A protection needs at least one legal nature");
        }
        natures = strictest(natures);
    }

    /**
     * The protection of data of a single nature.
     *
     * @param nature the legal nature
     * @param confidentiality the confidentiality level
     * @param integrity the integrity level
     * @param secret whether the data is a secret
     * @return the protection
     */
    public static Protection of(LegalNature nature, Confidentiality confidentiality, Integrity integrity,
                                boolean secret) {
        return new Protection(EnumSet.of(Objects.requireNonNull(nature, "nature")), confidentiality, integrity,
                secret);
    }

    /**
     * Whether the data is personal data.
     *
     * @return {@code true} if a nature is {@linkplain LegalNature#isPersonal() personal}
     */
    public boolean isPersonal() {
        return natures.stream().anyMatch(LegalNature::isPersonal);
    }

    /**
     * Whether the data includes special category or criminal offence data.
     *
     * @return {@code true} if a nature {@linkplain LegalNature#requiresMaximumProtection() requires the maximum
     *         protection}
     */
    public boolean requiresMaximumProtection() {
        return natures.stream().anyMatch(LegalNature::requiresMaximumProtection);
    }

    /**
     * Whether this protection is at least as strict as another in every facet: each of the other's natures is
     * covered by one of these, the levels are not lower, and a secret stays a secret.
     *
     * @param other another protection
     * @return {@code true} if this protection covers {@code other}
     */
    public boolean covers(Protection other) {
        Objects.requireNonNull(other, "other");
        return other.natures.stream().allMatch(n -> natures.stream().anyMatch(m -> m.covers(n)))
                && confidentiality.compareTo(other.confidentiality) >= 0
                && integrity.compareTo(other.integrity) >= 0
                && (secret || !other.secret);
    }

    /**
     * How this protection compares with an earlier one, such as the protection of the same field or category in
     * the previous version.
     *
     * @param before the earlier protection
     * @return {@link Direction#SAME}, {@link Direction#TIGHTENED} if this one covers it, {@link Direction#LOOSENED}
     *         if it covers this one, or {@link Direction#MIXED} if neither covers the other
     */
    public Direction comparedTo(Protection before) {
        boolean tighter = covers(before);
        boolean looser = before.covers(this);
        if (tighter && looser) {
            return Direction.SAME;
        }
        if (tighter) {
            return Direction.TIGHTENED;
        }
        return looser ? Direction.LOOSENED : Direction.MIXED;
    }

    /**
     * The least protection that covers both this one and another: the protection of data that holds both.
     *
     * @param other another protection
     * @return the strictest natures of both, the higher levels and secret if either is
     */
    public Protection join(Protection other) {
        Objects.requireNonNull(other, "other");
        Set<LegalNature> union = EnumSet.copyOf(natures);
        union.addAll(other.natures);
        return new Protection(union, confidentiality.max(other.confidentiality), integrity.max(other.integrity),
                secret || other.secret);
    }

    /**
     * The least protection that covers all of some protections.
     *
     * @param protections the protections
     * @return their join, or {@link #LOWEST} if there are none
     */
    public static Protection joinAll(Collection<Protection> protections) {
        Protection result = LOWEST;
        for (Protection protection : protections) {
            result = result.join(protection);
        }
        return result;
    }

    private static Set<LegalNature> strictest(Set<LegalNature> natures) {
        Set<LegalNature> result = EnumSet.noneOf(LegalNature.class);
        for (LegalNature nature : natures) {
            boolean dominated = natures.stream().anyMatch(other -> other != nature && other.covers(nature));
            if (!dominated) {
                result.add(Objects.requireNonNull(nature, "nature"));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * How a protection changed between two versions.
     *
     * @since 0.1.0
     */
    public enum Direction {

        /**
         * The protection is the same.
         */
        SAME,

        /**
         * The new protection covers the old one: it is stricter in at least one facet and no weaker in any.
         */
        TIGHTENED,

        /**
         * The old protection covers the new one: it is weaker in at least one facet and no stricter in any.
         */
        LOOSENED,

        /**
         * Stricter in one facet and weaker in another, or a legal nature that is not comparable with the old one,
         * such as special category data becoming criminal offence data. It always deserves the review a loosening
         * does.
         */
        MIXED;

        /**
         * Whether the change lowers some protection, so that someone should review it.
         *
         * @return {@code true} for {@link #LOOSENED} and {@link #MIXED}
         */
        public boolean weakens() {
            return this == LOOSENED || this == MIXED;
        }

        /**
         * Combines this direction with that of another facet of the same change.
         *
         * @param other the direction of the other facet
         * @return the direction of the whole change
         */
        public Direction and(Direction other) {
            Objects.requireNonNull(other, "other");
            if (this == SAME || this == other) {
                return other;
            }
            return other == SAME ? this : MIXED;
        }
    }
}
