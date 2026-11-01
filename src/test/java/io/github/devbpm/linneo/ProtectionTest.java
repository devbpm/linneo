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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.github.devbpm.linneo.LegalNature.CRIMINAL_OFFENCE;
import static io.github.devbpm.linneo.LegalNature.NONE;
import static io.github.devbpm.linneo.LegalNature.ORDINARY;
import static io.github.devbpm.linneo.LegalNature.SPECIAL_CATEGORY;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectionTest {

    /** Every distinct protection: 5 sets of strictest natures, 4 confidentiality and 3 integrity levels, 2 flags. */
    private static final List<Protection> ALL = all();

    private static List<Protection> all() {
        Set<Protection> protections = new LinkedHashSet<>();
        LegalNature[] natures = LegalNature.values();
        for (int mask = 1; mask < 1 << natures.length; mask++) {
            Set<LegalNature> set = EnumSet.noneOf(LegalNature.class);
            for (int i = 0; i < natures.length; i++) {
                if ((mask & 1 << i) != 0) {
                    set.add(natures[i]);
                }
            }
            for (Confidentiality c : Confidentiality.values()) {
                for (Integrity i : Integrity.values()) {
                    protections.add(new Protection(set, c, i, false));
                    protections.add(new Protection(set, c, i, true));
                }
            }
        }
        return new ArrayList<>(protections);
    }

    @Test
    @DisplayName("the natures are ordered partially: special category and criminal offence data are not comparable")
    void natureOrder() {
        assertAll(
                () -> assertTrue(ORDINARY.covers(NONE)),
                () -> assertFalse(NONE.covers(ORDINARY)),
                () -> assertTrue(SPECIAL_CATEGORY.covers(ORDINARY)),
                () -> assertTrue(CRIMINAL_OFFENCE.covers(ORDINARY)),
                () -> assertTrue(CRIMINAL_OFFENCE.covers(NONE)),
                () -> assertFalse(SPECIAL_CATEGORY.covers(CRIMINAL_OFFENCE)),
                () -> assertFalse(CRIMINAL_OFFENCE.covers(SPECIAL_CATEGORY)),
                () -> assertFalse(ORDINARY.covers(SPECIAL_CATEGORY)));
    }

    @Test
    @DisplayName("a protection keeps only its strictest natures, so equal protections are equal records")
    void normalized() {
        assertAll(
                () -> assertEquals(120, ALL.size()),
                () -> assertEquals(Set.of(SPECIAL_CATEGORY), new Protection(EnumSet.of(ORDINARY, SPECIAL_CATEGORY),
                        Confidentiality.HIGH, Integrity.HIGH, false).natures()),
                () -> assertEquals(Set.of(SPECIAL_CATEGORY, CRIMINAL_OFFENCE), new Protection(EnumSet.allOf(
                        LegalNature.class), Confidentiality.HIGH, Integrity.HIGH, false).natures()),
                () -> assertThrows(IllegalArgumentException.class, () -> new Protection(Set.of(),
                        Confidentiality.LOW, Integrity.LOW, false)),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> Protection.LOWEST.natures().add(ORDINARY)));
    }

    @Test
    @DisplayName("covers is a partial order: reflexive, antisymmetric and transitive")
    void partialOrder() {
        for (Protection p : ALL) {
            assertTrue(p.covers(p), p::toString);
            assertTrue(p.covers(Protection.LOWEST), p::toString);
            for (Protection q : ALL) {
                if (p.covers(q) && q.covers(p)) {
                    assertEquals(p, q);
                }
                if (!p.covers(q)) {
                    continue;
                }
                for (Protection r : ALL) {
                    if (q.covers(r)) {
                        assertTrue(p.covers(r), () -> p + " " + q + " " + r);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("join is the least protection that covers both")
    void join() {
        for (Protection p : ALL) {
            assertEquals(p, p.join(Protection.LOWEST));
            for (Protection q : ALL) {
                Protection j = p.join(q);
                assertEquals(j, q.join(p));
                assertTrue(j.covers(p) && j.covers(q), () -> p + " " + q);
                for (Protection r : ALL) {
                    if (r.covers(p) && r.covers(q)) {
                        assertTrue(r.covers(j), () -> p + " " + q + " " + r);
                    }
                }
            }
        }
        assertEquals(Protection.LOWEST, Protection.joinAll(List.of()));
    }

    @Test
    @DisplayName("comparedTo tells a tightening, a loosening and a mixed change apart")
    void direction() {
        Protection ordinary = Protection.of(ORDINARY, Confidentiality.MEDIUM, Integrity.MEDIUM, false);
        Protection health = Protection.of(SPECIAL_CATEGORY, Confidentiality.HIGH, Integrity.HIGH, false);
        Protection criminal = Protection.of(CRIMINAL_OFFENCE, Confidentiality.HIGH, Integrity.HIGH, false);
        Protection moreConfidentialLessIntegral = Protection.of(ORDINARY, Confidentiality.HIGH, Integrity.LOW, false);
        assertAll(
                () -> assertEquals(Direction.SAME, ordinary.comparedTo(ordinary)),
                () -> assertEquals(Direction.TIGHTENED, health.comparedTo(ordinary)),
                () -> assertEquals(Direction.LOOSENED, ordinary.comparedTo(health)),
                () -> assertEquals(Direction.MIXED, criminal.comparedTo(health)),
                () -> assertEquals(Direction.MIXED, moreConfidentialLessIntegral.comparedTo(ordinary)),
                () -> assertTrue(Direction.MIXED.weakens() && Direction.LOOSENED.weakens()),
                () -> assertFalse(Direction.SAME.weakens() || Direction.TIGHTENED.weakens()));
        for (Protection p : ALL) {
            for (Protection q : ALL) {
                Direction d = p.comparedTo(q);
                assertEquals(d == Direction.SAME || d == Direction.TIGHTENED, p.covers(q));
                assertEquals(d == Direction.SAME || d == Direction.LOOSENED, q.covers(p));
            }
        }
    }

    @Test
    @DisplayName("the directions of two facets of a change combine")
    void combine() {
        for (Direction d : Direction.values()) {
            assertEquals(d, Direction.SAME.and(d));
            assertEquals(d, d.and(Direction.SAME));
            assertEquals(d, d.and(d));
        }
        assertAll(
                () -> assertEquals(Direction.MIXED, Direction.TIGHTENED.and(Direction.LOOSENED)),
                () -> assertEquals(Direction.MIXED, Direction.LOOSENED.and(Direction.TIGHTENED)),
                () -> assertEquals(Direction.MIXED, Direction.MIXED.and(Direction.TIGHTENED)));
    }

    @Test
    @DisplayName("a category's protection is its nature and effective facets")
    void category() {
        Taxonomy taxonomy = Taxonomy.standard();
        DataCategory password = taxonomy.requireCategory(Categories.USER_AUTHORIZATION_PASSWORD);
        DataCategory health = taxonomy.requireCategory(Categories.USER_HEALTH_AND_MEDICAL);
        assertAll(
                () -> assertTrue(password.protection().secret()),
                () -> assertEquals(Set.of(SPECIAL_CATEGORY), health.protection().natures()),
                () -> assertTrue(health.protection().requiresMaximumProtection()),
                () -> assertTrue(health.protection().covers(taxonomy.requireCategory(Categories.USER_NAME)
                        .protection())));
    }
}
