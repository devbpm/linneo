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
package io.github.devbpm.linneo.internal;

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyDiff;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.TaxonomyExtension;
import io.github.devbpm.linneo.catalog.atlas.AtlasCatalog;
import io.github.devbpm.linneo.catalog.datahub.DataHubCatalog;
import io.github.devbpm.linneo.catalog.datahub.DhProposal;
import io.github.devbpm.linneo.catalog.openmetadata.OmClassification;
import io.github.devbpm.linneo.catalog.openmetadata.OmTag;
import io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataCatalog;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.InventoryDiff;
import io.github.devbpm.linneo.extension.finance.FinanceTaxonomy;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesTaxonomy;
import io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsTaxonomy;
import io.github.devbpm.linneo.extension.supplychain.SupplyChainTaxonomy;
import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The command line of the Linneo JAR: {@code java -jar linneo.jar <command>}. It works on the text forms Linneo writes
 * (a taxonomy, a data inventory) and on the catalog exports, so that a build pipeline or a data protection officer can
 * use them without writing Java. Not part of the public API.
 * <p>
 * Exit codes: 0 when the command succeeds, 1 when a check it was asked for fails, 2 for a usage or input error.
 */
public final class Cli {

    private static final String USAGE = String.join("\n",
            "Usage: java -jar linneo.jar <command> [options]",
            "",
            "Commands:",
            "  taxonomy [--with EXTENSION,...]",
            "      Writes the standard taxonomy, with the shipped extensions named, as text.",
            "      Extensions: university, finance, supply_chain, project_operations, human_resources.",
            "  taxonomy-diff BEFORE.tsv AFTER.tsv [--require-compatible]",
            "      Lists the changes between two taxonomies and the version change they need.",
            "      With --require-compatible, exits with 1 when a change is incompatible.",
            "  inventory-diff BEFORE.tsv AFTER.tsv --taxonomy TAXONOMY.tsv [--fail-on-weakening]",
            "      Lists the field changes between two data inventories.",
            "      With --fail-on-weakening, exits with 1 when a change weakens protection.",
            "  catalog openmetadata|datahub|atlas [--taxonomy TAXONOMY.tsv]",
            "      Writes the requests that create the taxonomy in a data catalog, as JSON.",
            "  help",
            "      Shows this text.");

    private static final Map<String, Supplier<TaxonomyExtension>> EXTENSIONS = new LinkedHashMap<>();

    static {
        EXTENSIONS.put("university", UniversityTaxonomy::new);
        EXTENSIONS.put("finance", FinanceTaxonomy::new);
        EXTENSIONS.put("supply_chain", SupplyChainTaxonomy::new);
        EXTENSIONS.put("project_operations", ProjectOperationsTaxonomy::new);
        EXTENSIONS.put("human_resources", HumanResourcesTaxonomy::new);
    }

    private Cli() {
    }

    /**
     * Runs a command and exits with its code.
     *
     * @param args the command and its options
     */
    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        System.exit(run(args, out, err));
    }

    /**
     * Runs a command.
     *
     * @param args the command and its options
     * @param out where results go
     * @param err where errors go
     * @return the exit code
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            err.println(USAGE);
            return 2;
        }
        List<String> rest = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
        try {
            return switch (args[0]) {
                case "taxonomy" -> taxonomy(rest, out);
                case "taxonomy-diff" -> taxonomyDiff(rest, out);
                case "inventory-diff" -> inventoryDiff(rest, out);
                case "catalog" -> catalog(rest, out);
                case "help", "--help", "-h" -> {
                    out.println(USAGE);
                    yield 0;
                }
                default -> throw new Usage("Unknown command '" + args[0] + "'");
            };
        } catch (Usage e) {
            err.println(e.getMessage());
            err.println();
            err.println(USAGE);
            return 2;
        } catch (IOException | IllegalArgumentException | TaxonomyException e) {
            err.println("Error: " + e.getMessage());
            return 2;
        }
    }

    private static int taxonomy(List<String> args, PrintStream out) {
        String with = option(args, "--with");
        noMore(args);
        TaxonomyBuilder builder = Taxonomy.builder().withStandard();
        if (with != null) {
            for (String name : with.split(",")) {
                Supplier<TaxonomyExtension> extension = EXTENSIONS.get(name.strip());
                if (extension == null) {
                    throw new Usage("Unknown extension '" + name.strip() + "'; known: "
                            + String.join(", ", EXTENSIONS.keySet()));
                }
                builder.extend(extension.get());
            }
        }
        out.print(builder.build().toTsv());
        return 0;
    }

    private static int taxonomyDiff(List<String> args, PrintStream out) throws IOException {
        boolean requireCompatible = flag(args, "--require-compatible");
        List<Path> files = files(args, 2);
        TaxonomyDiff diff = TaxonomyDiff.between(Taxonomy.fromTsv(read(files.get(0))),
                Taxonomy.fromTsv(read(files.get(1))));
        diff.changes().forEach(out::println);
        out.println(diff.changes().size() + " change(s); required version change: " + diff.requiredBump());
        if (requireCompatible && !diff.breakingChanges().isEmpty()) {
            out.println("Incompatible: " + diff.breakingChanges().size() + " change(s) remove a term, weaken"
                    + " protection or move a replacement");
            return 1;
        }
        return 0;
    }

    private static int inventoryDiff(List<String> args, PrintStream out) throws IOException {
        boolean failOnWeakening = flag(args, "--fail-on-weakening");
        String taxonomyFile = option(args, "--taxonomy");
        if (taxonomyFile == null) {
            throw new Usage("inventory-diff needs --taxonomy TAXONOMY.tsv to tell how protection moved");
        }
        List<Path> files = files(args, 2);
        InventoryDiff diff = InventoryDiff.between(DataInventory.fromTsv(read(files.get(0))),
                DataInventory.fromTsv(read(files.get(1))), Taxonomy.fromTsv(read(Path.of(taxonomyFile))));
        diff.changes().forEach(change -> out.println(change + (change.weakens() ? "  <- weakens protection" : "")));
        out.println(diff.changes().size() + " field change(s), " + diff.weakening().size() + " weakening");
        return failOnWeakening && !diff.weakening().isEmpty() ? 1 : 0;
    }

    private static int catalog(List<String> args, PrintStream out) throws IOException {
        String taxonomyFile = option(args, "--taxonomy");
        if (args.size() != 1) {
            throw new Usage("catalog needs one of openmetadata, datahub or atlas");
        }
        Taxonomy taxonomy = taxonomyFile == null ? Taxonomy.standard() : Taxonomy.fromTsv(read(Path.of(taxonomyFile)));
        String json = switch (args.getFirst()) {
            case "openmetadata" -> {
                OpenMetadataCatalog catalog = OpenMetadataCatalog.of(taxonomy);
                yield new Json()
                        .raw("classifications", Json.array(catalog.classifications().stream()
                                .map(OmClassification::toJson).toList()))
                        .raw("tags", Json.array(catalog.tags().stream().map(OmTag::toJson).toList()))
                        .end();
            }
            case "datahub" -> Json.array(DataHubCatalog.of(taxonomy).proposals().stream().map(DhProposal::toJson)
                    .toList());
            case "atlas" -> AtlasCatalog.of(taxonomy).typeDefsJson();
            default -> throw new Usage("Unknown catalog '" + args.getFirst() + "'");
        };
        out.println(json);
        return 0;
    }

    private static String option(List<String> args, String name) {
        int at = args.indexOf(name);
        if (at < 0) {
            return null;
        }
        if (at + 1 >= args.size()) {
            throw new Usage(name + " needs a value");
        }
        String value = args.get(at + 1);
        args.subList(at, at + 2).clear();
        return value;
    }

    private static boolean flag(List<String> args, String name) {
        return args.remove(name);
    }

    private static List<Path> files(List<String> args, int count) {
        if (args.size() != count || args.stream().anyMatch(a -> a.startsWith("--"))) {
            throw new Usage("Expected " + count + " files, got " + args);
        }
        return args.stream().map(Path::of).toList();
    }

    private static void noMore(List<String> args) {
        if (!args.isEmpty()) {
            throw new Usage("Unexpected arguments " + args);
        }
    }

    // UTF-8, or the encoding a byte order mark names: Windows tools write UTF-8 with a mark, and the redirection of
    // Windows PowerShell 5 writes UTF-16.
    private static String read(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** A command line that cannot be run as written. */
    private static final class Usage extends RuntimeException {

        private static final long serialVersionUID = 1L;

        Usage(String message) {
            super(message);
        }
    }
}
