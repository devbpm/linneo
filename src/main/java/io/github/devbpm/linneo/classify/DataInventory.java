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
package io.github.devbpm.linneo.classify;

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyDiff;
import io.github.devbpm.linneo.TermChange;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.internal.Tsv;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * The classification of an application's model as data: for each object, identified by a key the application
 * chooses, the fields it holds with what the code declares for them. It holds no classes and no taxonomy, so it can
 * be stored, read back and compared.
 * <p>
 * Applications use it to keep their own records of the classification, such as a field table or a data catalog, in
 * step with the code. Build the inventory of the code with
 * {@link Classifier#inventory(java.util.Collection, java.util.function.Function)}, build the inventory of what is
 * stored with {@link #builder()}, and compare them with {@link InventoryDiff}. The {@linkplain #fingerprint()
 * fingerprints}, one for the whole inventory and one per object, tell whether anything changed without comparing
 * field by field, so a start without changes can skip the comparison, and a start with changes can compare only the
 * objects whose fingerprint moved.
 * <p>
 * The inventory records what the code declares, not the protection a taxonomy gives it: a field keeps its key, even
 * a deprecated one, and only the levels it raises itself. A change of the taxonomy therefore never changes the
 * inventory; {@link #affectedBy(TaxonomyDiff)} lists the fields such a change reaches. Instances are immutable.
 *
 * @since 0.1.0
 */
public final class DataInventory {

    private static final String FORMAT = "linneo-inventory";
    private static final int VERSION = 1;
    private static final String OBJECT = "object";
    private static final String FIELD = "field";
    private static final String EXEMPT = "exempt";

    // object key -> path -> field, both sorted
    private final Map<String, Map<String, InventoryField>> objects;
    private volatile String fingerprint;

    private DataInventory(Map<String, Map<String, InventoryField>> objects) {
        Map<String, Map<String, InventoryField>> copy = new TreeMap<>();
        objects.forEach((object, fields) -> copy.put(object, Collections.unmodifiableMap(new TreeMap<>(fields))));
        this.objects = Collections.unmodifiableMap(copy);
    }

    /**
     * Starts an inventory, typically from what an application stored.
     *
     * @return an empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The keys of the objects.
     *
     * @return the keys, sorted
     */
    public Set<String> objects() {
        return objects.keySet();
    }

    /**
     * All the fields, exempted ones included.
     *
     * @return the fields, by object and then by path
     */
    public List<InventoryField> fields() {
        return objects.values().stream().flatMap(fields -> fields.values().stream()).toList();
    }

    /**
     * The fields of an object, exempted ones included.
     *
     * @param object the object key
     * @return the fields by path; empty for an unknown object
     */
    public List<InventoryField> fields(String object) {
        return List.copyOf(objects.getOrDefault(object, Map.of()).values());
    }

    /**
     * A field of an object.
     *
     * @param object the object key
     * @param path the field path
     * @return the field, or empty if the inventory does not hold it
     */
    public Optional<InventoryField> field(String object, String path) {
        return Optional.ofNullable(objects.getOrDefault(object, Map.of()).get(path));
    }

    /**
     * The classified fields that declare a data category key.
     *
     * @param key the key
     * @return the fields, by object and then by path
     */
    public List<InventoryField> fieldsWith(String key) {
        return fields().stream().filter(field -> key.equals(field.key())).toList();
    }

    /**
     * The fields each change of a taxonomy reaches: for each changed data category, the fields that declare its key
     * and those whose deprecated key is replaced by it, in either version, since such a field takes the protection
     * of the replacement. An application uses it to migrate the keys it stores when a category is deprecated, and to
     * record one entry per changed category, with the fields it affects, instead of one entry per field.
     *
     * @param diff the changes of the taxonomy
     * @return the data category changes that reach at least one field, in the order of the diff, with their fields
     */
    public Map<TermChange, List<InventoryField>> affectedBy(TaxonomyDiff diff) {
        Map<TermChange, List<InventoryField>> affected = new LinkedHashMap<>();
        for (TermChange change : diff.changes()) {
            if (change.hierarchy() == TermChange.Hierarchy.DATA_CATEGORY) {
                List<InventoryField> fields = fields().stream().filter(field -> !field.isExempt()
                        && (field.key().equals(change.key()) || resolvesTo(diff.before(), field.key(), change.key())
                        || resolvesTo(diff.after(), field.key(), change.key()))).toList();
                if (!fields.isEmpty()) {
                    affected.put(change, fields);
                }
            }
        }
        return Collections.unmodifiableMap(affected);
    }

    private static boolean resolvesTo(Taxonomy taxonomy, String key, String target) {
        return taxonomy.category(key).isPresent() && taxonomy.resolveCategory(key).key().equals(target);
    }

    /**
     * The fingerprint of the whole inventory: the SHA-256 of {@link #toTsv()}, in hexadecimal.
     *
     * @return 64 lower-case hexadecimal digits
     */
    public String fingerprint() {
        String result = fingerprint;
        if (result == null) {
            result = Tsv.fingerprint(toTsv());
            fingerprint = result;
        }
        return result;
    }

    /**
     * The fingerprint of one object: the SHA-256 of its rows of {@link #toTsv()}, in hexadecimal. It changes only
     * when that object changes.
     *
     * @param object the object key
     * @return 64 lower-case hexadecimal digits
     * @throws IllegalArgumentException if the inventory does not hold the object
     */
    public String fingerprint(String object) {
        Map<String, InventoryField> fields = objects.get(object);
        if (fields == null) {
            throw new IllegalArgumentException("The inventory holds no object '" + object + "'");
        }
        Tsv tsv = new Tsv(FORMAT, VERSION);
        write(tsv, object, fields);
        return Tsv.fingerprint(tsv.text());
    }

    /**
     * The inventory as tab-separated text, deterministic and readable by {@link #fromTsv(String)}.
     * <p>
     * The first line is {@code #linneo-inventory}, a tab and the format version, {@code 1}. Each object is a row
     * {@code object}, key, followed by its fields: {@code field}, object, path, key, basis, confidentiality,
     * integrity, identifiability, reason; or {@code exempt}, object, path, reason. Cells are escaped as in
     * {@link io.github.devbpm.linneo.Taxonomy#toTsv()}. Kept under version control, the text shows every change of the
     * classification in a review.
     *
     * @return the text
     */
    public String toTsv() {
        Tsv tsv = new Tsv(FORMAT, VERSION)
                .comment("object key")
                .comment("field object path key basis confidentiality integrity identifiability reason")
                .comment("exempt object path reason");
        objects.forEach((object, fields) -> write(tsv, object, fields));
        return tsv.text();
    }

    /**
     * Reads an inventory written by {@link #toTsv()}.
     *
     * @param text the text
     * @return the inventory
     * @throws IllegalArgumentException if the text is not an inventory of a known format version or a row is
     *         malformed
     */
    public static DataInventory fromTsv(String text) {
        Builder builder = builder();
        for (List<String> row : Tsv.read(text, FORMAT, VERSION)) {
            String kind = row.getFirst() == null ? "" : row.getFirst();
            switch (kind) {
                case OBJECT -> builder.object(cell(row, 2, 1));
                case FIELD -> builder.field(new InventoryField(cell(row, 9, 1), cell(row, 9, 2), cell(row, 9, 3),
                        constant(Basis.class, cell(row, 9, 4)), constant(Confidentiality.class, row.get(5)),
                        constant(Integrity.class, row.get(6)), constant(Identifiability.class, row.get(7)),
                        row.get(8)));
                case EXEMPT -> builder.field(InventoryField.exempt(cell(row, 4, 1), cell(row, 4, 2), row.get(3)));
                default -> throw new IllegalArgumentException("Unknown row '" + kind + "' in an inventory text");
            }
        }
        return builder.build();
    }

    /**
     * Whether another object is an inventory with the same objects and fields.
     *
     * @param other the other object
     * @return {@code true} if they are equal
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof DataInventory inventory && objects.equals(inventory.objects);
    }

    @Override
    public int hashCode() {
        return objects.hashCode();
    }

    @Override
    public String toString() {
        return "DataInventory[" + objects.size() + " objects, " + fields().size() + " fields]";
    }

    private static void write(Tsv tsv, String object, Map<String, InventoryField> fields) {
        tsv.row(OBJECT, object);
        for (InventoryField field : fields.values()) {
            if (field.isExempt()) {
                tsv.row(EXEMPT, object, field.path(), field.reason());
            } else {
                tsv.row(FIELD, object, field.path(), field.key(), field.basis(), field.confidentiality(),
                        field.integrity(), field.identifiability(), field.reason());
            }
        }
    }

    private static String cell(List<String> row, int count, int index) {
        if (row.size() != count) {
            throw new IllegalArgumentException("A '" + row.getFirst() + "' row needs " + count + " cells, not "
                    + row.size() + ": " + row);
        }
        String value = row.get(index);
        if (value == null) {
            throw new IllegalArgumentException("Cell " + (index + 1) + " of " + row + " must not be empty");
        }
        return value;
    }

    private static <E extends Enum<E>> E constant(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    /**
     * Builds a {@link DataInventory}, for example from the rows an application stored.
     *
     * @since 0.1.0
     */
    public static final class Builder {

        private final Map<String, Map<String, InventoryField>> objects = new TreeMap<>();

        private Builder() {
        }

        /**
         * Adds an object, even one without fields.
         *
         * @param key the object key
         * @return this builder
         */
        public Builder object(String key) {
            Objects.requireNonNull(key, "key");
            if (key.isBlank()) {
                throw new IllegalArgumentException("An object needs a key");
            }
            objects.computeIfAbsent(key, k -> new TreeMap<>());
            return this;
        }

        /**
         * Adds a field, and its object if it is new.
         *
         * @param field the field
         * @return this builder
         * @throws IllegalArgumentException if the object already has a field with that path
         */
        public Builder field(InventoryField field) {
            Objects.requireNonNull(field, "field");
            object(field.object());
            if (objects.get(field.object()).putIfAbsent(field.path(), field) != null) {
                throw new IllegalArgumentException("Duplicate field " + field.object() + "." + field.path());
            }
            return this;
        }

        /**
         * Adds a classified field that takes the levels of its category.
         *
         * @param object the object key
         * @param path the field path
         * @param key the data category key
         * @param basis where the category comes from
         * @return this builder
         */
        public Builder field(String object, String path, String key, Basis basis) {
            return field(new InventoryField(object, path, Objects.requireNonNull(key, "key"),
                    Objects.requireNonNull(basis, "basis"), null, null, null, null));
        }

        /**
         * Adds a field excluded from classification.
         *
         * @param object the object key
         * @param path the field path
         * @param reason why the field holds no data
         * @return this builder
         */
        public Builder exempt(String object, String path, String reason) {
            return field(InventoryField.exempt(object, path, reason));
        }

        /**
         * Creates the inventory.
         *
         * @return the inventory
         */
        public DataInventory build() {
            Map<String, Map<String, InventoryField>> copy = new TreeMap<>();
            objects.forEach((object, fields) -> copy.put(object, new TreeMap<>(fields)));
            return new DataInventory(copy);
        }
    }

    // Used by Classifier.inventory, which has classified every field.
    static DataInventory of(List<InventoryField> fields, List<String> objectKeys) {
        Builder builder = builder();
        objectKeys.forEach(builder::object);
        fields.forEach(builder::field);
        return builder.build();
    }
}
