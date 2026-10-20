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
import java.util.Optional;

/**
 * What every term of a taxonomy has, whatever its hierarchy.
 *
 * @param key the unique key in dot notation, such as {@code user.contact.email}
 * @param parentKey the key of the parent term, or {@code null} for a root
 * @param name a short human-readable name
 * @param description what the term covers
 * @param origin who defined the term: {@value #FIDESLANG}, {@value #LINNEO} or the name of a
 *        {@link TaxonomyExtension}
 * @param since the version of its origin that introduced the term, or {@code null} if unknown
 * @param deprecatedSince the version of its origin that deprecated the term, or {@code null} if it is current
 * @param replacedBy the key of the term that replaces a deprecated one, or {@code null}
 * @since 0.1.0
 */
public record TermInfo(String key, String parentKey, String name, String description, String origin, String since,
                       String deprecatedSince, String replacedBy) {

    /**
     * Origin of the terms taken from the fideslang default taxonomy.
     */
    public static final String FIDESLANG = "fideslang";

    /**
     * Origin of the terms added by Linneo.
     */
    public static final String LINNEO = "linneo";

    /**
     * Checks the mandatory components.
     *
     * @param key the unique key
     * @param parentKey the parent key or {@code null}
     * @param name the name
     * @param description the description
     * @param origin the origin
     * @param since the introducing version or {@code null}
     * @param deprecatedSince the deprecating version or {@code null}
     * @param replacedBy the replacing key or {@code null}
     */
    public TermInfo {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(origin, "origin");
        description = description == null ? "" : description;
    }

    /**
     * The key of the parent term.
     *
     * @return the parent key, empty for a root
     */
    public Optional<String> parent() {
        return Optional.ofNullable(parentKey);
    }

    /**
     * Whether the term is deprecated and should no longer be used in new declarations.
     *
     * @return {@code true} if a deprecation version or a replacement is set
     */
    public boolean isDeprecated() {
        return deprecatedSince != null || replacedBy != null;
    }
}
