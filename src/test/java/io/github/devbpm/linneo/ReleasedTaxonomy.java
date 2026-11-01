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
import io.github.devbpm.linneo.extension.finance.FinanceTaxonomy;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesTaxonomy;
import io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsTaxonomy;
import io.github.devbpm.linneo.extension.supplychain.SupplyChainTaxonomy;
import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The taxonomy Linneo ships, the standard taxonomy with every extension, and the copies of it kept for each release
 * under {@link #DIRECTORY}, against which {@code ReleasedTaxonomyTest} checks Linneo's versioning rules.
 * <p>
 * After a release, record its taxonomy from the project directory:
 * <pre>
 * java -cp target/classes:target/test-classes io.github.devbpm.linneo.ReleasedTaxonomy 0.1.0
 * </pre>
 * (use {@code ;} instead of {@code :} on Windows) and commit the new file.
 */
public final class ReleasedTaxonomy {

    /**
     * Where the released copies are, relative to the project directory.
     */
    public static final Path DIRECTORY = Path.of("src", "test", "resources", "io", "github", "devbpm", "linneo",
            "released");

    private ReleasedTaxonomy() {
    }

    /**
     * Writes the shipped taxonomy as the copy of a release.
     *
     * @param args the released version, such as {@code 0.1.0}
     * @throws IOException if the file cannot be written
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1 || !args[0].matches("\\d+\\.\\d+\\.\\d+")) {
            throw new IllegalArgumentException("Give the released version, such as 0.1.0");
        }
        Files.createDirectories(DIRECTORY);
        Path file = DIRECTORY.resolve(args[0] + ".tsv");
        Files.writeString(file, shipped().toTsv());
        System.out.println("Wrote " + file);
    }

    static Taxonomy shipped() {
        return Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).extend(new FinanceTaxonomy())
                .extend(new SupplyChainTaxonomy()).extend(new ProjectOperationsTaxonomy())
                .extend(new HumanResourcesTaxonomy()).build();
    }

    static Optional<Path> latest() throws IOException {
        if (!Files.isDirectory(DIRECTORY)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(DIRECTORY)) {
            return files.filter(f -> f.getFileName().toString().matches("\\d+\\.\\d+\\.\\d+\\.tsv"))
                    .max(Comparator.comparing(f -> version(f.getFileName().toString().replace(".tsv", "")),
                            Arrays::compare));
        }
    }

    // The largest change semantic versioning allows from one version to another.
    static Bump allowed(String released, String current) {
        int[] from = version(released);
        int[] to = version(current);
        if (to[0] != from[0]) {
            return Bump.MAJOR;
        }
        if (to[1] != from[1]) {
            return Bump.MINOR;
        }
        return to[2] != from[2] ? Bump.PATCH : Bump.NONE;
    }

    static int[] version(String version) {
        String plain = version.replaceFirst("-.*$", "");
        return Arrays.stream(plain.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }
}
