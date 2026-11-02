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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Every standard term the legal rules may read has its traits decided: either it has some, declared or inherited, or
 * it is listed in {@code traits-reviewed.txt} as reviewed without any. A term a new fideslang release adds fails here
 * until someone decides, so a missing trait never makes a rule silently laxer.
 */
class TraitCoverageTest {

    @Test
    @DisplayName("each personal data category, data use and data subject has traits or was reviewed without them")
    void everyTermReviewed() throws IOException {
        Taxonomy taxonomy = Taxonomy.standard();
        Set<String> withoutTraits = Stream.of(
                        taxonomy.categories().stream().filter(DataCategory::isPersonal)
                                .filter(c -> c.traits().isEmpty()).map(c -> "category " + c.key()),
                        taxonomy.dataUses().stream().filter(u -> u.traits().isEmpty()).map(u -> "use " + u.key()),
                        taxonomy.dataSubjects().stream().filter(s -> s.traits().isEmpty())
                                .map(s -> "subject " + s.key()))
                .flatMap(s -> s).collect(Collectors.toCollection(TreeSet::new));
        Set<String> reviewed;
        try (InputStream in = TraitCoverageTest.class.getResourceAsStream("traits-reviewed.txt")) {
            assertNotNull(in, "traits-reviewed.txt is missing");
            reviewed = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
        Set<String> unreviewed = new TreeSet<>(withoutTraits);
        unreviewed.removeAll(reviewed);
        Set<String> stale = new TreeSet<>(reviewed);
        stale.removeAll(withoutTraits);
        assertEquals(Set.of(), unreviewed, "give these terms traits in linneo-traits.tsv, or list them in"
                + " traits-reviewed.txt once reviewed");
        assertEquals(Set.of(), stale, "these terms have traits now, or no longer exist: remove them from"
                + " traits-reviewed.txt");
    }
}
