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

import io.github.devbpm.linneo.CategoryDraft;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.TermDraft;
import io.github.devbpm.linneo.TermInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Loads the standard taxonomy from the tab-separated resources bundled next to this class.
 * <p>
 * The {@code fideslang-*.tsv} files hold the fideslang default taxonomy unchanged (see their header and the
 * {@code NOTICE} file); the {@code linneo-*.tsv} files hold Linneo's additions and its protection policy. Not part
 * of the public API.
 */
public final class StandardTaxonomy {

    /**
     * The fideslang version the bundled terms come from.
     */
    public static final String FIDESLANG_VERSION = "3.1.4";

    private static final int COLUMNS = 7;

    private StandardTaxonomy() {
    }

    /**
     * Adds the standard terms and policy to a builder.
     *
     * @param builder the builder
     */
    public static void contribute(TaxonomyBuilder builder) {
        builder.origin(TermInfo.FIDESLANG);
        terms("fideslang-data-categories.tsv", (row, lifecycle) ->
                builder.category(row.get(0), row.get(2), row.get(3), lifecycle::accept));
        terms("fideslang-data-uses.tsv", (row, lifecycle) ->
                builder.dataUse(row.get(0), row.get(2), row.get(3), lifecycle));
        terms("fideslang-data-subjects.tsv", (row, lifecycle) ->
                builder.dataSubject(row.get(0), row.get(2), row.get(3), lifecycle));
        builder.origin(TermInfo.LINNEO);
        terms("linneo-data-categories.tsv", (row, lifecycle) ->
                builder.category(row.get(0), row.get(2), row.get(3), lifecycle::accept));
        terms("linneo-data-uses.tsv", (row, lifecycle) ->
                builder.dataUse(row.get(0), row.get(2), row.get(3), lifecycle));
        terms("linneo-data-subjects.tsv", (row, lifecycle) ->
                builder.dataSubject(row.get(0), row.get(2), row.get(3), lifecycle));
        for (List<String> row : read("linneo-category-policy.tsv", 5)) {
            builder.refineCategory(row.get(0), draft -> applyPolicy(draft, row));
        }
    }

    private interface TermRow {
        void add(List<String> row, Consumer<TermDraft> lifecycle);
    }

    private static void terms(String resource, TermRow adder) {
        for (List<String> row : read(resource, COLUMNS)) {
            String key = row.get(0);
            String declaredParent = row.get(1);
            int dot = key.lastIndexOf('.');
            String derivedParent = dot < 0 ? "" : key.substring(0, dot);
            if (!declaredParent.equals(derivedParent)) {
                throw new TaxonomyException(resource + ": '" + key + "' declares parent '" + declaredParent
                        + "' but its key implies '" + derivedParent + "'");
            }
            adder.add(row, draft -> {
                if (!row.get(4).isEmpty()) {
                    draft.since(row.get(4));
                }
                if (!row.get(5).isEmpty()) {
                    draft.deprecated(row.get(5), row.get(6).isEmpty() ? null : row.get(6));
                }
            });
        }
    }

    private static void applyPolicy(CategoryDraft draft, List<String> row) {
        if (!row.get(1).isEmpty()) {
            draft.nature(LegalNature.valueOf(row.get(1)));
        }
        if (!row.get(2).isEmpty()) {
            draft.confidentiality(Confidentiality.valueOf(row.get(2)));
        }
        if (!row.get(3).isEmpty()) {
            draft.integrity(Integrity.valueOf(row.get(3)));
        }
        if (Boolean.parseBoolean(row.get(4))) {
            draft.secret();
        }
    }

    private static List<List<String>> read(String resource, int columns) {
        InputStream in = StandardTaxonomy.class.getResourceAsStream(resource);
        if (in == null) {
            throw new TaxonomyException("Missing taxonomy resource " + resource);
        }
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            boolean header = true;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                if (header) {
                    header = false;
                    continue;
                }
                String[] cells = line.split("\t", -1);
                if (cells.length > columns) {
                    throw new TaxonomyException(resource + ": too many columns in line '" + line + "'");
                }
                List<String> row = new ArrayList<>(columns);
                for (int i = 0; i < columns; i++) {
                    row.add(i < cells.length ? cells[i].strip() : "");
                }
                rows.add(List.copyOf(row));
            }
        } catch (IOException e) {
            throw new TaxonomyException("Cannot read taxonomy resource " + resource, e);
        }
        return rows;
    }
}
