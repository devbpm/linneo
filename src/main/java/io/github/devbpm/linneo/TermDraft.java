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

import io.github.devbpm.linneo.TermChange.Hierarchy;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * The declared properties of a term while a {@link TaxonomyBuilder} is open: its lifecycle and its
 * {@linkplain Trait traits}. Data categories add their protection facets in {@link CategoryDraft}.
 *
 * @since 0.1.0
 */
public sealed class TermDraft permits CategoryDraft {

    private final Hierarchy hierarchy;
    private final String key;
    private final String name;
    private final String description;
    private final String origin;
    private final Set<Trait> traits = EnumSet.noneOf(Trait.class);
    private String since;
    private String deprecatedSince;
    private String replacedBy;
    // The builder, which tells which extension, if any, is contributing when the draft is changed.
    private final TaxonomyBuilder builder;
    // A draft only changes inside the customizer that received it.
    private boolean open;

    TermDraft(TaxonomyBuilder builder, Hierarchy hierarchy, String key, String name, String description,
              String origin) {
        this.builder = Objects.requireNonNull(builder, "builder");
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
        this.key = Objects.requireNonNull(key, "key");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description == null ? "" : description;
        this.origin = Objects.requireNonNull(origin, "origin");
    }

    /**
     * Declares the version of its origin that introduced the term.
     *
     * @param version the version, such as {@code 1.2.0}
     * @return this draft
     * @throws TaxonomyException if an extension refines a term it did not add, or the customizer that received the
     *         draft has returned
     */
    public TermDraft since(String version) {
        checkLifecycle("set the version that introduced");
        this.since = version;
        return this;
    }

    /**
     * Declares traits of the term, which its descendants inherit. Traits only add up: declaring some again, as two
     * extensions may, keeps all of them.
     *
     * @param traits traits that apply to the term's hierarchy
     * @return this draft
     * @throws TaxonomyException if a trait does not apply to the term's hierarchy, or the customizer that received the
     *         draft has returned
     */
    public TermDraft traits(Trait... traits) {
        checkOpen();
        for (Trait trait : traits) {
            if (!Objects.requireNonNull(trait, "trait").appliesTo(hierarchy)) {
                throw new TaxonomyException("The trait " + trait + " does not apply to the " + hierarchy + " '"
                        + key + "'");
            }
            this.traits.add(trait);
        }
        return this;
    }

    /**
     * Deprecates the term. Declarations that still use it keep working and are resolved to its replacement.
     *
     * @param sinceVersion the version of the term's origin that deprecated it
     * @param replacedBy the key of the replacing term of the same hierarchy, or {@code null} if there is none
     * @return this draft
     * @throws TaxonomyException if the version is blank, which would leave the term current, an extension refines a
     *         term it did not add, or the customizer that received the draft has returned
     */
    public TermDraft deprecated(String sinceVersion, String replacedBy) {
        checkLifecycle("deprecate");
        if (Objects.requireNonNull(sinceVersion, "sinceVersion").isBlank()) {
            throw new TaxonomyException("The deprecation of '" + key + "' needs the version that deprecated it");
        }
        this.deprecatedSince = sinceVersion;
        this.replacedBy = replacedBy;
        return this;
    }

    final String key() {
        return key;
    }

    final Set<Trait> declaredTraits() {
        return Collections.unmodifiableSet(traits);
    }

    // Opens or closes the draft for its customizer and returns whether it was open, for nested customizers.
    final boolean open(boolean open) {
        boolean was = this.open;
        this.open = open;
        return was;
    }

    final void checkOpen() {
        if (!open) {
            throw new TaxonomyException("The draft of '" + key + "' can only be changed inside the customizer that"
                    + " received it");
        }
    }

    // Checked when the change is made, against the extension contributing then.
    private void checkLifecycle(String action) {
        checkOpen();
        String extension = builder.extending();
        if (extension != null && !extension.equals(origin)) {
            throw new TaxonomyException("The extension '" + extension + "' cannot " + action + " '" + key
                    + "', which comes from '" + origin + "'");
        }
    }

    final String name() {
        return name;
    }

    final String description() {
        return description;
    }

    final String origin() {
        return origin;
    }

    final TermInfo info(String parentKey) {
        return new TermInfo(key, parentKey, name, description, origin, blankToNull(since),
                blankToNull(deprecatedSince), blankToNull(replacedBy));
    }

    final String replacedBy() {
        return blankToNull(replacedBy);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
