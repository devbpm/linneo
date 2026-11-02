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
package io.github.devbpm.linneo.export.postgresql;

import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.classify.ClassClassification;
import io.github.devbpm.linneo.classify.ClassClassification.Exemption;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.DisclosurePolicy;
import io.github.devbpm.linneo.classify.DisclosurePolicy.Treatment;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.classify.InventoryField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The masking rules of <a href="https://postgresql-anonymizer.readthedocs.io/">PostgreSQL Anonymizer</a> for the
 * columns of classified fields: the {@code SECURITY LABEL FOR anon} statements that make the database's masked views
 * and anonymized dumps follow a {@link DisclosurePolicy}, typically for the copies of production data that test
 * environments use.
 * <pre>{@code
 * PostgresAnonymizer anonymizer = PostgresAnonymizer.of(DisclosurePolicy.forLogs())
 *         .withValue(Categories.SYSTEM_AUTHENTICATION_SECRET, "'*'");      // a NOT NULL column
 * List<String> statements = anonymizer.labels(classifier.classify(Student.class),
 *         path -> "students." + path.replace('.', '_'));
 * }</pre>
 * A field the policy leaves {@linkplain Treatment#OMIT out} is {@code MASKED WITH VALUE NULL}, or the value declared
 * for its category with {@link #withValue(String, String)}, which a {@code NOT NULL} column needs. A field it
 * {@linkplain Treatment#MASK masks} is replaced by a fake value of the same kind when Linneo knows a masking function
 * for its category or an ancestor ({@link #masks()}, such as {@code anon.dummy_safe_email()} for
 * {@code user.contact.email}, from the {@code dummy_*} functions version 2 recommends); otherwise it is left out in the
 * same way, because Linneo does not know the type of the column and leaving the value out is the stricter choice.
 * Declare the masks of your other columns with {@link #withMask(String, String)}. A field shown as it is gets
 * {@code IS NULL}, which removes any rule, so running the statements again after a change of the classification
 * brings every column in line.
 * <p>
 * When several fields are stored in one column, such as a composite kept as JSON, the column gets one statement: the
 * rule of its fields when they all have the same, and otherwise {@code MASKED WITH VALUE NULL}, since a fake value of
 * one field's kind does not fit a column that holds several. Column names are written quoted, an unquoted part folded
 * to lower case as PostgreSQL does, so reserved words such as {@code user} work, and the labels are string constants
 * that read the same whatever {@code standard_conforming_strings} is.
 * <p>
 * Linneo only writes the statements; the application chooses the column of each field and runs them. Instances are
 * immutable and thread-safe.
 *
 * @since 0.1.0
 */
public final class PostgresAnonymizer {

    private static final Map<String, String> STANDARD_MASKS = Map.ofEntries(
            Map.entry("user.account.username", "anon.dummy_username()"),
            Map.entry("user.contact.address.city", "anon.dummy_city_name()"),
            Map.entry("user.contact.address.country", "anon.dummy_country_name()"),
            Map.entry("user.contact.address.postal_code", "anon.dummy_post_code()"),
            Map.entry("user.contact.address.state", "anon.dummy_state_name()"),
            Map.entry("user.contact.address.street", "anon.dummy_street_name()"),
            Map.entry("user.contact.email", "anon.dummy_safe_email()"),
            Map.entry("user.contact.phone_number", "anon.dummy_phone_number()"),
            Map.entry("user.device.ip_address", "anon.dummy_ip()"),
            Map.entry("user.name", "anon.dummy_name()"),
            Map.entry("user.name.first", "anon.dummy_first_name()"),
            Map.entry("user.name.last", "anon.dummy_last_name()"));
    private static final String NULL_VALUE = "MASKED WITH VALUE NULL";
    private static final String PART = "([A-Za-z_][A-Za-z0-9_$]*|\"([^\"]|\"\")+\")";
    private static final Pattern PART_PATTERN = Pattern.compile(PART);
    private static final Pattern COLUMN = Pattern.compile(PART + "(\\." + PART + "){1,2}");

    private final DisclosurePolicy policy;
    private final Map<String, String> masks;
    private final Map<String, String> values;

    private PostgresAnonymizer(DisclosurePolicy policy, Map<String, String> masks, Map<String, String> values) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.masks = Collections.unmodifiableMap(new TreeMap<>(masks));
        this.values = Collections.unmodifiableMap(new TreeMap<>(values));
    }

    /**
     * The rules that follow a policy, with Linneo's masking functions.
     *
     * @param policy the policy that decides which values are shown, masked or left out
     * @return the anonymizer
     */
    public static PostgresAnonymizer of(DisclosurePolicy policy) {
        return new PostgresAnonymizer(policy, STANDARD_MASKS, Map.of());
    }

    /**
     * The same rules with a masking function for the fields of a category and of the categories below it, unless a
     * more specific key has its own.
     *
     * @param categoryKey a data category key
     * @param function the SQL that follows {@code MASKED WITH FUNCTION}, such as
     *        {@code anon.random_int_between(0, 10)} or {@code anon.partial(iban, 4, '******', 2)}
     * @return a new anonymizer
     */
    public PostgresAnonymizer withMask(String categoryKey, String function) {
        Map<String, String> changed = new TreeMap<>(masks);
        changed.put(Objects.requireNonNull(categoryKey, "categoryKey"), sql(categoryKey, function));
        return new PostgresAnonymizer(policy, changed, values);
    }

    /**
     * The same rules with the value that replaces a left-out value of a category and of the categories below it,
     * unless a more specific key has its own, instead of {@code NULL}: what a {@code NOT NULL} column, such as a
     * password hash, needs. The value should tell nothing about the original, as a constant does.
     *
     * @param categoryKey a data category key
     * @param value the SQL that follows {@code MASKED WITH VALUE}, such as {@code '*'} or {@code 0}
     * @return a new anonymizer
     */
    public PostgresAnonymizer withValue(String categoryKey, String value) {
        Map<String, String> changed = new TreeMap<>(values);
        changed.put(Objects.requireNonNull(categoryKey, "categoryKey"), sql(categoryKey, value));
        return new PostgresAnonymizer(policy, masks, changed);
    }

    /**
     * The masking functions by category key.
     *
     * @return an immutable map sorted by key
     */
    public Map<String, String> masks() {
        return masks;
    }

    /**
     * The values that replace left-out values, by category key; a category without one is {@code NULL}.
     *
     * @return an immutable map sorted by key
     */
    public Map<String, String> values() {
        return values;
    }

    /**
     * The masking rule of a field: the text of its security label.
     *
     * @param field the classification of the field
     * @return {@code MASKED WITH FUNCTION ...} or {@code MASKED WITH VALUE ...}, or {@code null} for a value the
     *         policy shows as it is
     */
    public String rule(FieldClassification field) {
        return rule(policy.treatment(field), field.category());
    }

    /**
     * The masking rule of a stored field, resolved with a taxonomy.
     *
     * @param field a field of a {@link DataInventory}
     * @param taxonomy the taxonomy to resolve its key with
     * @return the rule, as {@link #rule(FieldClassification)} gives it; {@code null} for an exempted field
     * @throws TaxonomyException if the taxonomy does not contain the field's key
     */
    public String rule(InventoryField field, Taxonomy taxonomy) {
        Treatment treatment = policy.treatment(field, taxonomy);
        return treatment == Treatment.CLEAR ? null : rule(treatment, field.category(taxonomy));
    }

    /**
     * The statement that sets the rule of a field on its column.
     *
     * @param column the column, as {@code table.column} or {@code schema.table.column}, quoted where needed
     * @param field the classification of the field stored in the column
     * @return a {@code SECURITY LABEL FOR anon ON COLUMN ...} statement
     * @throws IllegalArgumentException if the column is not a qualified SQL identifier
     */
    public String label(String column, FieldClassification field) {
        return statement(column(column), rule(field));
    }

    /**
     * The statement that sets the rule of a stored field on its column.
     *
     * @param column the column, as {@code table.column} or {@code schema.table.column}, quoted where needed
     * @param field a field of a {@link DataInventory}
     * @param taxonomy the taxonomy to resolve its key with
     * @return a {@code SECURITY LABEL FOR anon ON COLUMN ...} statement
     * @throws IllegalArgumentException if the column is not a qualified SQL identifier
     * @throws TaxonomyException if the taxonomy does not contain the field's key
     */
    public String label(String column, InventoryField field, Taxonomy taxonomy) {
        return statement(column(column), rule(field, taxonomy));
    }

    /**
     * The statements for every field of a class, the exempted ones included, one per column.
     *
     * @param classification the classification of the class, without errors
     * @param columns the column of each field path, as {@code table.column}; {@code null} for a field with no column
     * @return the statements, in the order of the first field of each column
     * @throws IllegalArgumentException if the classification has errors, so that some fields have no rule, or a
     *         column is not a qualified SQL identifier
     */
    public List<String> labels(ClassClassification classification, Function<String, String> columns) {
        if (!classification.isValid()) {
            throw new IllegalArgumentException(classification.type().getName() + " has classification errors, so"
                    + " some fields have no rule: " + classification.errors());
        }
        Map<String, Set<String>> rules = new LinkedHashMap<>();
        for (FieldClassification field : classification.fields()) {
            add(rules, columns.apply(field.path()), rule(field));
        }
        for (Exemption exemption : classification.exempted()) {
            add(rules, columns.apply(exemption.path()), null);
        }
        return statements(rules);
    }

    /**
     * The statements for every field of an object of a data inventory, the exempted ones included, one per column.
     *
     * @param inventory the inventory
     * @param object the key of the object
     * @param taxonomy the taxonomy to resolve the stored keys with
     * @param columns the column of each field path, as {@code table.column}; {@code null} for a field with no column
     * @return the statements, in the order of the first field of each column
     * @throws IllegalArgumentException if the inventory has no such object, or a column is not a qualified SQL
     *         identifier
     * @throws TaxonomyException if the taxonomy does not contain a key of the object
     */
    public List<String> labels(DataInventory inventory, String object, Taxonomy taxonomy,
                               Function<String, String> columns) {
        if (!inventory.objects().contains(Objects.requireNonNull(object, "object"))) {
            throw new IllegalArgumentException("The inventory has no object '" + object + "'");
        }
        Map<String, Set<String>> rules = new LinkedHashMap<>();
        for (InventoryField field : inventory.fields(object)) {
            add(rules, columns.apply(field.path()), rule(field, taxonomy));
        }
        return statements(rules);
    }

    @Override
    public String toString() {
        return "PostgresAnonymizer[" + policy + ", " + masks.size() + " masks, " + values.size() + " values]";
    }

    private String rule(Treatment treatment, DataCategory category) {
        return switch (treatment) {
            case CLEAR -> null;
            case OMIT -> leftOut(category);
            case MASK -> {
                String function = nearest(masks, category);
                yield function == null ? leftOut(category) : "MASKED WITH FUNCTION " + function;
            }
        };
    }

    private String leftOut(DataCategory category) {
        String value = nearest(values, category);
        return value == null ? NULL_VALUE : "MASKED WITH VALUE " + value;
    }

    // The entry of the category, or of its nearest ancestor that has one.
    private static String nearest(Map<String, String> byKey, DataCategory category) {
        for (String key = category.key(); key != null; key = parent(key)) {
            String found = byKey.get(key);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static String parent(String key) {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? null : key.substring(0, dot);
    }

    private static String sql(String categoryKey, String text) {
        if (Objects.requireNonNull(text, "SQL").isBlank()) {
            throw new IllegalArgumentException("The SQL of '" + categoryKey + "' is blank");
        }
        return text.strip();
    }

    private static void add(Map<String, Set<String>> rules, String column, String rule) {
        if (column != null) {
            rules.computeIfAbsent(column(column), c -> new HashSet<>()).add(rule);
        }
    }

    // One statement per column: the rule its fields share, or the value left out when they differ.
    private static List<String> statements(Map<String, Set<String>> rules) {
        List<String> statements = new ArrayList<>();
        rules.forEach((column, columnRules) -> statements.add(statement(column,
                columnRules.size() == 1 ? columnRules.iterator().next() : NULL_VALUE)));
        return List.copyOf(statements);
    }

    // The column with every part quoted; an unquoted part is folded to lower case, as PostgreSQL reads it.
    private static String column(String column) {
        Objects.requireNonNull(column, "column");
        if (!COLUMN.matcher(column).matches()) {
            throw new IllegalArgumentException("Not a qualified column name (table.column): " + column);
        }
        List<String> parts = new ArrayList<>();
        Matcher part = PART_PATTERN.matcher(column);
        for (int at = 0; at < column.length(); at = part.end() + 1) {
            part.region(at, column.length()).lookingAt();
            String text = part.group();
            parts.add(text.startsWith("\"") ? text : "\"" + text.toLowerCase(Locale.ROOT) + "\"");
        }
        return String.join(".", parts);
    }

    private static String statement(String column, String rule) {
        return "SECURITY LABEL FOR anon ON COLUMN " + column + " IS " + (rule == null ? "NULL" : literal(rule)) + ";";
    }

    // A string constant: an escape string when the text has a backslash, which a plain one reads differently
    // depending on standard_conforming_strings.
    private static String literal(String text) {
        String quoted = text.replace("'", "''");
        return text.contains("\\") ? "E'" + quoted.replace("\\", "\\\\") + "'" : "'" + quoted + "'";
    }
}
