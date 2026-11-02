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

import io.github.devbpm.linneo.TaxonomyDiff.Bump;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReleasedTaxonomyTest {

    @Test
    @DisplayName("the taxonomy changed since its release line's last release no more than the version did")
    void versioning() throws IOException {
        String current = System.getProperty("linneo.version");
        assumeTrue(current != null, "run through Maven, which passes linneo.version");
        Optional<Path> baseline = ReleasedTaxonomy.baseline(ReleasedTaxonomy.releases(), current);
        assumeTrue(baseline.isPresent(), "no release recorded yet");
        String released = baseline.get().getFileName().toString().replace(".tsv", "");
        TaxonomyDiff diff = TaxonomyDiff.between(Taxonomy.fromTsv(Files.readString(baseline.get())),
                ReleasedTaxonomy.shipped());
        Bump allowed = ReleasedTaxonomy.allowed(released, current);
        assertTrue(diff.requiredBump().compareTo(allowed) <= 0, () -> "From " + released + " to " + current
                + " the version allows a " + allowed + " change, but the taxonomy needs a " + diff.requiredBump()
                + ": " + (diff.requiredBump() == Bump.MAJOR ? diff.breakingChanges() : diff.changes()));
    }

    @Test
    @DisplayName("a key removed in any earlier release is never used again")
    void removedKeysStayRemoved() throws IOException {
        String current = System.getProperty("linneo.version");
        assumeTrue(current != null, "run through Maven, which passes linneo.version");
        List<Path> releases = ReleasedTaxonomy.releases();
        Optional<Path> baseline = ReleasedTaxonomy.baseline(releases, current);
        assumeTrue(baseline.isPresent(), "no release recorded yet");
        // The keys each release removed from the one before, up to this line's baseline.
        Set<String> removed = new TreeSet<>();
        Set<String> previous = null;
        for (Path release : releases) {
            if (Arrays.compare(ReleasedTaxonomy.version(release), ReleasedTaxonomy.version(baseline.get())) > 0) {
                break;
            }
            Set<String> keys = ReleasedTaxonomy.keys(Taxonomy.fromTsv(Files.readString(release)));
            if (previous != null) {
                previous.stream().filter(key -> !keys.contains(key)).forEach(removed::add);
                assertEquals(Set.of(), intersection(removed, keys), "keys removed earlier are back in " + release);
            }
            previous = keys;
        }
        assertEquals(Set.of(), intersection(removed, ReleasedTaxonomy.keys(ReleasedTaxonomy.shipped())),
                "keys removed by an earlier release are back");
    }

    private static Set<String> intersection(Set<String> a, Set<String> b) {
        Set<String> both = new TreeSet<>(a);
        both.retainAll(b);
        return both;
    }

    @Test
    @DisplayName("each version is compared with the highest release not above it")
    void baseline() {
        List<Path> releases = List.of(Path.of("0.1.0.tsv"), Path.of("0.2.0.tsv"), Path.of("1.0.0.tsv"));
        assertAll(
                () -> assertEquals(Optional.of(Path.of("0.1.0.tsv")),
                        ReleasedTaxonomy.baseline(releases, "0.1.1-SNAPSHOT")),
                () -> assertEquals(Optional.of(Path.of("1.0.0.tsv")),
                        ReleasedTaxonomy.baseline(releases, "1.1.0-SNAPSHOT")),
                () -> assertEquals(Optional.empty(), ReleasedTaxonomy.baseline(releases, "0.0.9")),
                () -> assertEquals(Optional.of(Path.of("1.0.0.tsv")),
                        ReleasedTaxonomy.baseline(releases, "1.0.1+build.7")));
    }

    @Test
    @DisplayName("semantic versioning allows a change as large as the first number that differs")
    void allowed() {
        assertAll(
                () -> assertEquals(Bump.NONE, ReleasedTaxonomy.allowed("0.1.0", "0.1.0")),
                () -> assertEquals(Bump.PATCH, ReleasedTaxonomy.allowed("0.1.0", "0.1.1-SNAPSHOT")),
                () -> assertEquals(Bump.MINOR, ReleasedTaxonomy.allowed("0.1.3", "0.2.0-SNAPSHOT")),
                () -> assertEquals(Bump.MAJOR, ReleasedTaxonomy.allowed("1.4.0", "2.0.0")));
    }

    @Test
    @DisplayName("the shipped taxonomy, every extension included, survives its text form")
    void shipped() {
        Taxonomy shipped = ReleasedTaxonomy.shipped();
        assertEquals(shipped, Taxonomy.fromTsv(shipped.toTsv()));
    }
}
