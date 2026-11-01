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

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.InventoryField;
import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliTest {

    @TempDir
    Path work;

    private record Result(int code, String out, String err) {
    }

    private static Result run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = Cli.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Result(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    private Path write(String name, String text) throws IOException {
        return Files.writeString(work.resolve(name), text, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("the taxonomy, with shipped extensions, is written as the text Taxonomy.fromTsv reads")
    void taxonomy() {
        Result standard = run("taxonomy");
        Result university = run("taxonomy", "--with", "university");
        assertAll(
                () -> assertEquals(0, standard.code()),
                () -> assertEquals(Taxonomy.standard(), Taxonomy.fromTsv(standard.out())),
                () -> assertEquals(Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build(),
                        Taxonomy.fromTsv(university.out())),
                () -> assertEquals(2, run("taxonomy", "--with", "unknown").code()));
    }

    @Test
    @DisplayName("a taxonomy diff gives the required version change and fails on incompatible changes when asked")
    void taxonomyDiff() throws IOException {
        Path standard = write("standard.tsv", Taxonomy.standard().toTsv());
        Path extended = write("extended.tsv", Taxonomy.builder().withStandard().extend(new UniversityTaxonomy())
                .build().toTsv());
        Result added = run("taxonomy-diff", standard.toString(), extended.toString(), "--require-compatible");
        Result removed = run("taxonomy-diff", extended.toString(), standard.toString(), "--require-compatible");
        assertAll(
                () -> assertEquals(0, added.code(), added::err),
                () -> assertTrue(added.out().contains("required version change: MINOR"), added::out),
                () -> assertEquals(1, removed.code()),
                () -> assertTrue(removed.out().contains("Incompatible:"), removed::out),
                () -> assertEquals(0, run("taxonomy-diff", extended.toString(), standard.toString()).code()));
    }

    @Test
    @DisplayName("files with a byte order mark, as Windows tools write them, are read in the encoding it names")
    void byteOrderMarks() throws IOException {
        String text = Taxonomy.standard().toTsv();
        Path utf8 = Files.write(work.resolve("utf8.tsv"), concat(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                text.getBytes(StandardCharsets.UTF_8)));
        Path utf16 = Files.write(work.resolve("utf16.tsv"), concat(new byte[] {(byte) 0xFF, (byte) 0xFE},
                text.getBytes(StandardCharsets.UTF_16LE)));
        Result result = run("taxonomy-diff", utf8.toString(), utf16.toString());
        assertAll(
                () -> assertEquals(0, result.code(), result::err),
                () -> assertTrue(result.out().startsWith("0 change(s)"), result::out));
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] all = new byte[first.length + second.length];
        System.arraycopy(first, 0, all, 0, first.length);
        System.arraycopy(second, 0, all, first.length, second.length);
        return all;
    }

    @Test
    @DisplayName("an inventory diff marks the changes that weaken protection and fails on them when asked")
    void inventoryDiff() throws IOException {
        Path taxonomy = write("taxonomy.tsv", Taxonomy.standard().toTsv());
        Path before = write("before.tsv", DataInventory.builder()
                .field("students", "email", Categories.USER_CONTACT_EMAIL, Basis.DECLARED).build().toTsv());
        Path after = write("after.tsv", DataInventory.builder()
                .field(InventoryField.exempt("students", "email", "always empty")).build().toTsv());
        Result result = run("inventory-diff", before.toString(), after.toString(), "--taxonomy", taxonomy.toString(),
                "--fail-on-weakening");
        assertAll(
                () -> assertEquals(1, result.code(), result::err),
                () -> assertTrue(result.out().contains("<- weakens protection"), result::out),
                () -> assertTrue(result.out().endsWith("1 field change(s), 1 weakening" + System.lineSeparator())),
                () -> assertEquals(2, run("inventory-diff", before.toString(), after.toString()).code()));
    }

    @Test
    @DisplayName("the catalog requests are written as JSON, and bad input is a usage or input error")
    void catalogAndErrors() {
        assertAll(
                () -> assertTrue(run("catalog", "openmetadata").out().startsWith("{\"classifications\":[")),
                () -> assertTrue(run("catalog", "datahub").out().startsWith("[{")),
                () -> assertTrue(run("catalog", "atlas").out().startsWith("{\"classificationDefs\":[")),
                () -> assertEquals(2, run("catalog", "unknown").code()),
                () -> assertEquals(2, run().code()),
                () -> assertEquals(2, run("nonsense").code()),
                () -> assertEquals(0, run("help").code()),
                () -> assertTrue(run("taxonomy-diff", "missing.tsv", "other.tsv").err().startsWith("Error: ")));
    }
}
