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
package io.github.devbpm.linneo.ens;

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.classify.ClassClassification;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.classify.InventoryField;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The security category of a system under the Spanish Esquema Nacional de Seguridad (Real Decreto 311/2022, annex I),
 * with the valuation of its information taken from the classification of the data it handles.
 * <p>
 * The ENS values information and services in five dimensions, each {@linkplain Level#LOW low},
 * {@linkplain Level#MEDIUM medium} or {@linkplain Level#HIGH high}, and the category of the system is the highest
 * level of any dimension. Linneo knows two of them for the information: the confidentiality and the integrity of every
 * field, defined on the same three-level scale (limited, serious and severe harm), so {@link Confidentiality#LOW},
 * {@code MEDIUM} and {@code HIGH} are the ENS levels and {@link Confidentiality#PUBLIC} sets no requirement. The
 * system owner declares the other three, authenticity, traceability and availability, and may raise confidentiality
 * or integrity, never lower them: the code sets the floor.
 * <pre>{@code
 * EnsCategorization ens = EnsCategorization.builder()
 *         .information(classifier.classify(Student.class))
 *         .declare(Dimension.AVAILABILITY, Level.MEDIUM, "enrollment closes on fixed dates")
 *         .declare(Dimension.AUTHENTICITY, Level.MEDIUM, "students sign in")
 *         .declare(Dimension.TRACEABILITY, Level.MEDIUM, "changes to records are audited")
 *         .build();
 * ens.category();   // HIGH when the system handles special category data
 * }</pre>
 * Only a classification without errors gives information, since a field with an error has no level and leaving it
 * out would understate the category. While no information is given or a dimension is missing, the category is only a
 * lower bound ({@link #isComplete()}). The correspondence of
 * levels is Linneo's; review the valuation against guide CCN-STIC 803 before relying on it. Instances are
 * immutable.
 *
 * @since 0.1.0
 */
public final class EnsCategorization {

    /**
     * The security dimensions of the ENS, with their codes.
     *
     * @since 0.1.0
     */
    public enum Dimension {

        /**
         * Confidentiality [C].
         */
        CONFIDENTIALITY("C"),

        /**
         * Integrity [I].
         */
        INTEGRITY("I"),

        /**
         * Traceability [T].
         */
        TRACEABILITY("T"),

        /**
         * Authenticity [A].
         */
        AUTHENTICITY("A"),

        /**
         * Availability [D].
         */
        AVAILABILITY("D");

        private final String code;

        Dimension(String code) {
            this.code = code;
        }

        /**
         * The letter the ENS uses.
         *
         * @return such as {@code C}
         */
        public String code() {
            return code;
        }
    }

    /**
     * The level of a dimension.
     *
     * @since 0.1.0
     */
    public enum Level {

        /**
         * BAJO: limited harm.
         */
        LOW("BAJO"),

        /**
         * MEDIO: serious harm.
         */
        MEDIUM("MEDIO"),

        /**
         * ALTO: very serious harm.
         */
        HIGH("ALTO");

        private final String spanishName;

        Level(String spanishName) {
            this.spanishName = spanishName;
        }

        /**
         * The name in the ENS.
         *
         * @return such as {@code MEDIO}
         */
        public String spanishName() {
            return spanishName;
        }
    }

    /**
     * The category of a system.
     *
     * @since 0.1.0
     */
    public enum SystemCategory {

        /**
         * BÁSICA: no dimension above low.
         */
        BASIC("BÁSICA"),

        /**
         * MEDIA: a dimension medium, none high.
         */
        MEDIUM("MEDIA"),

        /**
         * ALTA: a dimension high.
         */
        HIGH("ALTA");

        private final String spanishName;

        SystemCategory(String spanishName) {
            this.spanishName = spanishName;
        }

        /**
         * The name in the ENS.
         *
         * @return such as {@code MEDIA}
         */
        public String spanishName() {
            return spanishName;
        }
    }

    /**
     * The level of a dimension and why.
     *
     * @param dimension the dimension
     * @param level the level
     * @param reason the field that sets it, or what the system owner declared, in English
     * @since 0.1.0
     */
    public record Valuation(Dimension dimension, Level level, String reason) {

        /**
         * Checks the components.
         *
         * @param dimension the dimension
         * @param level the level
         * @param reason the reason
         */
        public Valuation {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(level, "level");
            Objects.requireNonNull(reason, "reason");
        }
    }

    private final Map<Dimension, Valuation> valuations;
    private final boolean informed;

    private EnsCategorization(Map<Dimension, Valuation> valuations, boolean informed) {
        this.valuations = new EnumMap<>(valuations);
        this.informed = informed;
    }

    /**
     * Starts a categorization.
     *
     * @return a builder with no information and nothing declared
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The valuation of a dimension.
     *
     * @param dimension the dimension
     * @return its valuation, or empty if the information sets no requirement and nothing was declared
     */
    public Optional<Valuation> valuation(Dimension dimension) {
        return Optional.ofNullable(valuations.get(dimension));
    }

    /**
     * Every valuation, in the order of the dimensions.
     *
     * @return an immutable list
     */
    public List<Valuation> valuations() {
        return List.copyOf(valuations.values());
    }

    /**
     * The category of the system: the highest level of any dimension.
     *
     * @return the category; {@link SystemCategory#BASIC} when no dimension has a level
     */
    public SystemCategory category() {
        Level highest = valuations.values().stream().map(Valuation::level).max(Enum::compareTo).orElse(Level.LOW);
        return switch (highest) {
            case LOW -> SystemCategory.BASIC;
            case MEDIUM -> SystemCategory.MEDIUM;
            case HIGH -> SystemCategory.HIGH;
        };
    }

    /**
     * Whether the information was given and the system owner declared the three dimensions the classification cannot
     * give, so that the category is final rather than a lower bound.
     *
     * @return {@code true} if information was given and authenticity, traceability and availability are valued
     */
    public boolean isComplete() {
        return informed && missing().isEmpty();
    }

    /**
     * The dimensions without a valuation.
     *
     * @return in the order of the dimensions
     */
    public Set<Dimension> missing() {
        Set<Dimension> missing = EnumSet.allOf(Dimension.class);
        missing.removeAll(valuations.keySet());
        missing.remove(Dimension.CONFIDENTIALITY);
        missing.remove(Dimension.INTEGRITY);
        return missing;
    }

    /**
     * A one-line summary, in English, with the ENS names.
     *
     * @return such as {@code Category MEDIUM (MEDIA): C MEDIUM, I HIGH; not declared: T, A, D}
     */
    public String summary() {
        List<String> parts = new ArrayList<>();
        valuations.values().forEach(v -> parts.add(v.dimension().code() + " " + v.level()));
        String text = "Category " + category() + " (" + category().spanishName() + "): "
                + (parts.isEmpty() ? "no dimension valued" : String.join(", ", parts));
        if (!informed) {
            text += "; no information given (lower bound)";
        } else if (!isComplete()) {
            text += "; not declared: " + String.join(", ", missing().stream().map(Dimension::code).toList())
                    + " (lower bound)";
        }
        return text;
    }

    @Override
    public String toString() {
        return "EnsCategorization[" + summary() + "]";
    }

    /**
     * Builds an {@link EnsCategorization}.
     *
     * @since 0.1.0
     */
    public static final class Builder {

        private final Map<Dimension, Valuation> valuations = new EnumMap<>(Dimension.class);
        private boolean informed;

        private Builder() {
        }

        /**
         * Adds the fields of a class of the information the system handles.
         *
         * @param classification the classification of the class
         * @return this builder
         * @throws IllegalArgumentException if the classification has errors: a field with an error has no level,
         *         and leaving it out would understate the category
         */
        public Builder information(ClassClassification classification) {
            if (!classification.isValid()) {
                throw new IllegalArgumentException(classification.type().getName() + " has classification errors, so"
                        + " some fields have no level: " + classification.errors());
            }
            informed = true;
            for (FieldClassification field : classification.fields()) {
                add(field.category(), field.confidentiality(), field.integrity(),
                        classification.type().getSimpleName() + "." + field.path());
            }
            return this;
        }

        /**
         * Adds the stored fields of every object of a data inventory, as the information the system handles. An
         * inventory without objects gives no information, so the category stays a lower bound.
         *
         * @param inventory the inventory of the system
         * @param taxonomy the taxonomy to resolve its keys with
         * @return this builder
         * @throws TaxonomyException if the taxonomy does not contain a key
         */
        public Builder information(DataInventory inventory, Taxonomy taxonomy) {
            for (String object : inventory.objects()) {
                information(inventory, object, taxonomy);
            }
            return this;
        }

        /**
         * Adds the stored fields of one object of a data inventory, as information the system handles.
         *
         * @param inventory the inventory
         * @param object the key of the object
         * @param taxonomy the taxonomy to resolve its keys with
         * @return this builder
         * @throws IllegalArgumentException if the inventory has no such object, which would leave its information
         *         out and understate the category
         * @throws TaxonomyException if the taxonomy does not contain a key
         */
        public Builder information(DataInventory inventory, String object, Taxonomy taxonomy) {
            if (!inventory.objects().contains(Objects.requireNonNull(object, "object"))) {
                throw new IllegalArgumentException("The inventory has no object '" + object + "'");
            }
            informed = true;
            for (InventoryField field : inventory.fields(object)) {
                if (!field.isExempt()) {
                    add(field.category(taxonomy), field.protection(taxonomy).confidentiality(),
                            field.protection(taxonomy).integrity(), field.object() + "." + field.path());
                }
            }
            return this;
        }

        /**
         * Declares the level of a dimension. For confidentiality and integrity a declaration can only raise what the
         * information sets.
         *
         * @param dimension the dimension
         * @param level the level
         * @param reason why, in a few words
         * @return this builder
         */
        public Builder declare(Dimension dimension, Level level, String reason) {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(level, "level");
            Objects.requireNonNull(reason, "reason");
            raise(new Valuation(dimension, level, "declared: " + reason));
            return this;
        }

        /**
         * Creates the categorization.
         *
         * @return the categorization
         */
        public EnsCategorization build() {
            return new EnsCategorization(valuations, informed);
        }

        private void add(DataCategory category, Confidentiality confidentiality, Integrity integrity, String field) {
            if (confidentiality != Confidentiality.PUBLIC) {
                raise(new Valuation(Dimension.CONFIDENTIALITY, Level.valueOf(confidentiality.name()),
                        "field " + field + " (" + category.key() + ")"));
            }
            raise(new Valuation(Dimension.INTEGRITY, Level.valueOf(integrity.name()),
                    "field " + field + " (" + category.key() + ")"));
        }

        private void raise(Valuation valuation) {
            valuations.merge(valuation.dimension(), valuation,
                    (current, candidate) -> candidate.level().compareTo(current.level()) > 0 ? candidate : current);
        }
    }
}
