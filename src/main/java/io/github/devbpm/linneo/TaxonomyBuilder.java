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

import io.github.devbpm.linneo.internal.StandardTaxonomy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Builds a {@link Taxonomy}: the standard terms, the {@linkplain TaxonomyExtension extensions} and any terms added
 * directly.
 * <p>
 * Terms can be added in any order; the hierarchy is checked when {@link #build()} is called. A key is a
 * dot-separated path of lower-case segments ({@code [a-z][a-z0-9_]*}); its parent is the key without its last
 * segment, and must exist in the same hierarchy. When the taxonomy is built:
 * <ul>
 *   <li>a root data category must declare its nature, confidentiality and integrity;</li>
 *   <li>a child inherits every facet it does not declare, and may not declare a weaker one;</li>
 *   <li>a secret category makes all its children secret;</li>
 *   <li>a replacement must exist in the same hierarchy, and replacements must not form a cycle.</li>
 * </ul>
 * A builder is not thread-safe and is meant to be used once.
 *
 * @since 0.1.0
 */
public final class TaxonomyBuilder {

    /**
     * Origin recorded for terms added outside any extension.
     */
    public static final String DEFAULT_ORIGIN = "application";

    private static final Pattern KEY = Pattern.compile("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*");

    private final Map<String, CategoryDraft> categories = new LinkedHashMap<>();
    private final Map<String, TermDraft> uses = new LinkedHashMap<>();
    private final Map<String, TermDraft> subjects = new LinkedHashMap<>();
    private final Map<String, Class<?>> appliedExtensions = new LinkedHashMap<>();
    private String origin = DEFAULT_ORIGIN;
    private boolean standardApplied;

    TaxonomyBuilder() {
    }

    /**
     * Adds the standard taxonomy: the fideslang default taxonomy and Linneo's extensions with their protection
     * policy. Calling it again has no effect.
     *
     * @return this builder
     */
    public TaxonomyBuilder withStandard() {
        if (!standardApplied) {
            standardApplied = true;
            String previous = origin;
            StandardTaxonomy.contribute(this);
            origin = previous;
        }
        return this;
    }

    /**
     * Applies every {@link TaxonomyExtension} found by {@link ServiceLoader} with the thread context class loader,
     * or with this class's loader when there is none.
     *
     * @return this builder
     */
    public TaxonomyBuilder withExtensions() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return withExtensions(loader != null ? loader : TaxonomyBuilder.class.getClassLoader());
    }

    /**
     * Applies every {@link TaxonomyExtension} found by {@link ServiceLoader}, in order of their names so the result
     * does not depend on the class path order.
     *
     * @param classLoader where to look for extensions
     * @return this builder
     */
    public TaxonomyBuilder withExtensions(ClassLoader classLoader) {
        List<TaxonomyExtension> found = new ArrayList<>();
        ServiceLoader.load(TaxonomyExtension.class, classLoader).forEach(found::add);
        found.sort(Comparator.comparing(TaxonomyExtension::name));
        found.forEach(this::extend);
        return this;
    }

    /**
     * Applies an extension. Its terms are recorded with the extension's {@linkplain TaxonomyExtension#name() name}
     * as their origin. Applying the same extension class again has no effect, so an extension can be both applied
     * explicitly and registered as a service.
     *
     * @param extension the extension
     * @return this builder
     * @throws TaxonomyException if the extension uses a reserved name, or a different extension class with the same
     *         name was already applied
     */
    public TaxonomyBuilder extend(TaxonomyExtension extension) {
        String name = Objects.requireNonNull(extension, "extension").name();
        if (name == null || name.isBlank() || TermInfo.FIDESLANG.equals(name) || TermInfo.LINNEO.equals(name)) {
            throw new TaxonomyException("Invalid extension name '" + name + "' in " + extension.getClass().getName());
        }
        Class<?> applied = appliedExtensions.putIfAbsent(name, extension.getClass());
        if (applied != null && applied != extension.getClass()) {
            throw new TaxonomyException("The extensions " + applied.getName() + " and "
                    + extension.getClass().getName() + " are both named '" + name + "'");
        }
        if (applied == null) {
            String previous = origin;
            origin = name;
            try {
                extension.contribute(this);
            } finally {
                origin = previous;
            }
        }
        return this;
    }

    /**
     * Sets the origin recorded for the terms added from now on.
     *
     * @param origin the origin, such as the name of the application
     * @return this builder
     */
    public TaxonomyBuilder origin(String origin) {
        this.origin = Objects.requireNonNull(origin, "origin");
        return this;
    }

    /**
     * Adds a data category that inherits all its protection from its parent.
     *
     * @param key the unique key
     * @param name the human-readable name
     * @param description what the category covers
     * @return this builder
     * @throws TaxonomyException if the key is invalid or already present
     */
    public TaxonomyBuilder category(String key, String name, String description) {
        return category(key, name, description, draft -> {
        });
    }

    /**
     * Adds a data category and declares its properties.
     *
     * @param key the unique key
     * @param name the human-readable name
     * @param description what the category covers
     * @param customizer declares facets, attributes and lifecycle on the new category
     * @return this builder
     * @throws TaxonomyException if the key is invalid or already present
     */
    public TaxonomyBuilder category(String key, String name, String description, Consumer<CategoryDraft> customizer) {
        CategoryDraft draft = new CategoryDraft(checkKey(key, categories), name, description, origin);
        customizer.accept(draft);
        categories.put(key, draft);
        return this;
    }

    /**
     * Makes the protection of an existing data category stricter, or adds attributes or a deprecation to it.
     * A facet it already declares can only be raised.
     *
     * @param key the key of a category already added
     * @param customizer declares the stricter facets, attributes or lifecycle
     * @return this builder
     * @throws TaxonomyException if the category does not exist or the customizer lowers a declared facet
     */
    public TaxonomyBuilder refineCategory(String key, Consumer<CategoryDraft> customizer) {
        CategoryDraft draft = categories.get(key);
        if (draft == null) {
            throw new TaxonomyException("Cannot refine unknown data category '" + key + "'");
        }
        draft.refining(true);
        try {
            customizer.accept(draft);
        } finally {
            draft.refining(false);
        }
        return this;
    }

    /**
     * Adds a data use.
     *
     * @param key the unique key
     * @param name the human-readable name
     * @param description what the purpose covers
     * @return this builder
     * @throws TaxonomyException if the key is invalid or already present
     */
    public TaxonomyBuilder dataUse(String key, String name, String description) {
        return dataUse(key, name, description, draft -> {
        });
    }

    /**
     * Adds a data use and declares its lifecycle.
     *
     * @param key the unique key
     * @param name the human-readable name
     * @param description what the purpose covers
     * @param customizer declares the lifecycle
     * @return this builder
     * @throws TaxonomyException if the key is invalid or already present
     */
    public TaxonomyBuilder dataUse(String key, String name, String description, Consumer<TermDraft> customizer) {
        TermDraft draft = new TermDraft(checkKey(key, uses), name, description, origin);
        customizer.accept(draft);
        uses.put(key, draft);
        return this;
    }

    /**
     * Adds a data subject.
     *
     * @param key the unique key
     * @param name the human-readable name
     * @param description who the subject is
     * @return this builder
     * @throws TaxonomyException if the key is invalid or already present
     */
    public TaxonomyBuilder dataSubject(String key, String name, String description) {
        return dataSubject(key, name, description, draft -> {
        });
    }

    /**
     * Adds a data subject and declares its lifecycle.
     *
     * @param key the unique key
     * @param name the human-readable name
     * @param description who the subject is
     * @param customizer declares the lifecycle
     * @return this builder
     * @throws TaxonomyException if the key is invalid or already present
     */
    public TaxonomyBuilder dataSubject(String key, String name, String description, Consumer<TermDraft> customizer) {
        TermDraft draft = new TermDraft(checkKey(key, subjects), name, description, origin);
        customizer.accept(draft);
        subjects.put(key, draft);
        return this;
    }

    /**
     * Checks the hierarchies and resolves the inherited facets.
     *
     * @return the immutable taxonomy
     * @throws TaxonomyException if a parent or replacement is missing, a root category lacks a facet, a category
     *         weakens its parent's protection, or replacements form a cycle
     */
    public Taxonomy build() {
        Map<String, DataCategory> builtCategories = new LinkedHashMap<>();
        for (CategoryDraft draft : byDepth(categories)) {
            String parentKey = parentOf(draft.key(), categories, "data category");
            DataCategory parent = parentKey == null ? null : builtCategories.get(parentKey);
            builtCategories.put(draft.key(), resolve(draft, parentKey, parent));
        }
        checkReplacements(categories, "data category");
        checkReplacements(uses, "data use");
        checkReplacements(subjects, "data subject");
        return new Taxonomy(builtCategories.values(), buildTerms(uses, "data use", DataUse::new),
                buildTerms(subjects, "data subject", DataSubject::new));
    }

    private static DataCategory resolve(CategoryDraft draft, String parentKey, DataCategory parent) {
        String key = draft.key();
        LegalNature nature = draft.declaredNature();
        Confidentiality confidentiality = draft.declaredConfidentiality();
        Integrity integrity = draft.declaredIntegrity();
        Map<String, String> attributes = new LinkedHashMap<>();
        if (parent == null) {
            if (nature == null || confidentiality == null || integrity == null) {
                throw new TaxonomyException("Root data category '" + key
                        + "' must declare its nature, confidentiality and integrity");
            }
            attributes.putAll(draft.declaredAttributes());
            return new DataCategory(draft.info(null), nature, confidentiality, integrity, draft.declaredSecret(),
                    attributes);
        }
        if (nature != null && !parent.nature().admits(nature)) {
            throw new TaxonomyException("Data category '" + key + "' declares nature " + nature
                    + ", which its parent '" + parentKey + "' (" + parent.nature() + ") does not admit");
        }
        if (confidentiality != null && confidentiality.compareTo(parent.confidentiality()) < 0) {
            throw new TaxonomyException("Data category '" + key + "' declares confidentiality " + confidentiality
                    + ", lower than its parent's " + parent.confidentiality());
        }
        if (integrity != null && integrity.compareTo(parent.integrity()) < 0) {
            throw new TaxonomyException("Data category '" + key + "' declares integrity " + integrity
                    + ", lower than its parent's " + parent.integrity());
        }
        attributes.putAll(parent.attributes());
        attributes.putAll(draft.declaredAttributes());
        return new DataCategory(draft.info(parentKey),
                nature != null ? nature : parent.nature(),
                confidentiality != null ? confidentiality : parent.confidentiality(),
                integrity != null ? integrity : parent.integrity(),
                parent.secret() || draft.declaredSecret(),
                attributes);
    }

    private static <T extends Term> List<T> buildTerms(Map<String, TermDraft> drafts, String kind,
                                                      Function<TermInfo, T> factory) {
        List<T> terms = new ArrayList<>();
        for (TermDraft draft : byDepth(drafts)) {
            terms.add(factory.apply(draft.info(parentOf(draft.key(), drafts, kind))));
        }
        return terms;
    }

    private static <D extends TermDraft> List<D> byDepth(Map<String, D> drafts) {
        List<D> ordered = new ArrayList<>(drafts.values());
        ordered.sort(Comparator.comparingInt((D d) -> depth(d.key())).thenComparing(TermDraft::key));
        return ordered;
    }

    private static int depth(String key) {
        return (int) key.chars().filter(c -> c == '.').count();
    }

    private static String parentOf(String key, Map<String, ? extends TermDraft> drafts, String kind) {
        int dot = key.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        String parentKey = key.substring(0, dot);
        if (!drafts.containsKey(parentKey)) {
            throw new TaxonomyException("The parent '" + parentKey + "' of " + kind + " '" + key + "' does not exist");
        }
        return parentKey;
    }

    private static void checkReplacements(Map<String, ? extends TermDraft> drafts, String kind) {
        for (TermDraft draft : drafts.values()) {
            Set<String> seen = new HashSet<>();
            TermDraft current = draft;
            while (current.replacedBy() != null) {
                if (!seen.add(current.key())) {
                    throw new TaxonomyException("The replacements of " + kind + " '" + draft.key() + "' form a cycle");
                }
                TermDraft next = drafts.get(current.replacedBy());
                if (next == null) {
                    throw new TaxonomyException("The replacement '" + current.replacedBy() + "' of " + kind + " '"
                            + current.key() + "' does not exist");
                }
                current = next;
            }
        }
    }

    private static String checkKey(String key, Map<String, ?> existing) {
        Objects.requireNonNull(key, "key");
        if (!KEY.matcher(key).matches()) {
            throw new TaxonomyException("Invalid key '" + key + "': use lower-case segments separated by dots");
        }
        if (existing.containsKey(key)) {
            throw new TaxonomyException("Duplicate key '" + key + "'");
        }
        return key;
    }
}
