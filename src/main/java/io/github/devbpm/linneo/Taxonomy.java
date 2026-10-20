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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * An immutable set of terms in three hierarchies: data categories, data uses and data subjects.
 * <p>
 * Obtain one with {@link #standard()}, {@link #load()} or {@link #builder()}. Instances are thread-safe; build one
 * per application and share it. Lists are ordered by key, so a parent always comes before its descendants.
 *
 * @since 0.1.0
 */
public final class Taxonomy {

    private final Map<String, DataCategory> categories;
    private final Map<String, DataUse> uses;
    private final Map<String, DataSubject> subjects;

    Taxonomy(Collection<DataCategory> categories, Collection<DataUse> uses, Collection<DataSubject> subjects) {
        this.categories = index(categories);
        this.uses = index(uses);
        this.subjects = index(subjects);
    }

    /**
     * The standard taxonomy: the fideslang default taxonomy plus Linneo's extensions, without application
     * extensions. The instance is created once and shared.
     *
     * @return the standard taxonomy
     */
    public static Taxonomy standard() {
        return StandardHolder.INSTANCE;
    }

    /**
     * Builds the standard taxonomy plus every {@link TaxonomyExtension} registered with {@link java.util.ServiceLoader}.
     * Each call builds a new instance; keep the result.
     *
     * @return the standard taxonomy with the registered extensions
     * @throws TaxonomyException if an extension breaks the rules of {@link TaxonomyBuilder}
     */
    public static Taxonomy load() {
        return builder().withStandard().withExtensions().build();
    }

    /**
     * A builder that starts empty; call {@link TaxonomyBuilder#withStandard()} to start from the standard terms.
     *
     * @return a new builder
     */
    public static TaxonomyBuilder builder() {
        return new TaxonomyBuilder();
    }

    /**
     * All data categories, ordered by key.
     *
     * @return an immutable list
     */
    public List<DataCategory> categories() {
        return List.copyOf(categories.values());
    }

    /**
     * A data category by key, deprecated or not.
     *
     * @param key the key
     * @return the category, or empty if the taxonomy does not contain it
     */
    public Optional<DataCategory> category(String key) {
        return Optional.ofNullable(categories.get(key));
    }

    /**
     * A data category by key, deprecated or not.
     *
     * @param key the key
     * @return the category
     * @throws TaxonomyException if the taxonomy does not contain it
     */
    public DataCategory requireCategory(String key) {
        return require(categories, key, "data category");
    }

    /**
     * The data category to use for a key: the category itself or, if it is deprecated with a replacement, the
     * current category that replaces it.
     *
     * @param key the key
     * @return the current category
     * @throws TaxonomyException if the taxonomy does not contain the key
     */
    public DataCategory resolveCategory(String key) {
        DataCategory category = requireCategory(key);
        while (category.info().replacedBy() != null) {
            category = requireCategory(category.info().replacedBy());
        }
        return category;
    }

    /**
     * The parent of a data category.
     *
     * @param category a category of this taxonomy
     * @return its parent, or empty for a root
     */
    public Optional<DataCategory> parentOf(DataCategory category) {
        return category.info().parent().map(categories::get);
    }

    /**
     * The direct children of a data category.
     *
     * @param category a category of this taxonomy
     * @return its children, ordered by key
     */
    public List<DataCategory> childrenOf(DataCategory category) {
        return childrenOf(categories, category.key());
    }

    /**
     * The ancestors of a data category, from its parent up to its root.
     *
     * @param category a category of this taxonomy
     * @return its ancestors, nearest first
     */
    public List<DataCategory> ancestorsOf(DataCategory category) {
        List<DataCategory> ancestors = new ArrayList<>();
        Optional<DataCategory> parent = parentOf(category);
        while (parent.isPresent()) {
            ancestors.add(parent.get());
            parent = parentOf(parent.get());
        }
        return Collections.unmodifiableList(ancestors);
    }

    /**
     * All data uses, ordered by key.
     *
     * @return an immutable list
     */
    public List<DataUse> dataUses() {
        return List.copyOf(uses.values());
    }

    /**
     * A data use by key.
     *
     * @param key the key
     * @return the data use, or empty if the taxonomy does not contain it
     */
    public Optional<DataUse> dataUse(String key) {
        return Optional.ofNullable(uses.get(key));
    }

    /**
     * A data use by key.
     *
     * @param key the key
     * @return the data use
     * @throws TaxonomyException if the taxonomy does not contain it
     */
    public DataUse requireDataUse(String key) {
        return require(uses, key, "data use");
    }

    /**
     * The direct children of a data use.
     *
     * @param use a data use of this taxonomy
     * @return its children, ordered by key
     */
    public List<DataUse> childrenOf(DataUse use) {
        return childrenOf(uses, use.key());
    }

    /**
     * All data subjects, ordered by key.
     *
     * @return an immutable list
     */
    public List<DataSubject> dataSubjects() {
        return List.copyOf(subjects.values());
    }

    /**
     * A data subject by key.
     *
     * @param key the key
     * @return the data subject, or empty if the taxonomy does not contain it
     */
    public Optional<DataSubject> dataSubject(String key) {
        return Optional.ofNullable(subjects.get(key));
    }

    /**
     * A data subject by key.
     *
     * @param key the key
     * @return the data subject
     * @throws TaxonomyException if the taxonomy does not contain it
     */
    public DataSubject requireDataSubject(String key) {
        return require(subjects, key, "data subject");
    }

    @Override
    public String toString() {
        return "Taxonomy[" + categories.size() + " data categories, " + uses.size() + " data uses, "
                + subjects.size() + " data subjects]";
    }

    private static <T extends Term> Map<String, T> index(Collection<T> terms) {
        Map<String, T> map = new TreeMap<>();
        terms.forEach(term -> map.put(term.key(), term));
        return Collections.unmodifiableMap(map);
    }

    private static <T extends Term> T require(Map<String, T> terms, String key, String kind) {
        T term = terms.get(key);
        if (term == null) {
            throw new TaxonomyException("Unknown " + kind + " '" + key + "'");
        }
        return term;
    }

    private static <T extends Term> List<T> childrenOf(Map<String, T> terms, String key) {
        return terms.values().stream().filter(t -> key.equals(t.info().parentKey())).toList();
    }

    private static final class StandardHolder {
        private static final Taxonomy INSTANCE = builder().withStandard().build();
    }
}
