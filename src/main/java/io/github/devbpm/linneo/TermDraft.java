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

import java.util.Objects;

/**
 * The declared properties of a term while a {@link TaxonomyBuilder} is open: its lifecycle. Data categories add
 * their protection facets in {@link CategoryDraft}.
 *
 * @since 0.1.0
 */
public sealed class TermDraft permits CategoryDraft {

    private final String key;
    private final String name;
    private final String description;
    private final String origin;
    private String since;
    private String deprecatedSince;
    private String replacedBy;

    TermDraft(String key, String name, String description, String origin) {
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
     */
    public TermDraft since(String version) {
        this.since = version;
        return this;
    }

    /**
     * Deprecates the term. Declarations that still use it keep working and are resolved to its replacement.
     *
     * @param sinceVersion the version of the term's origin that deprecated it
     * @param replacedBy the key of the replacing term of the same hierarchy, or {@code null} if there is none
     * @return this draft
     */
    public TermDraft deprecated(String sinceVersion, String replacedBy) {
        this.deprecatedSince = Objects.requireNonNull(sinceVersion, "sinceVersion");
        this.replacedBy = replacedBy;
        return this;
    }

    final String key() {
        return key;
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
