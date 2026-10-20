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

import java.util.Map;
import java.util.Objects;

/**
 * What a field holds, with the protection that kind of data requires.
 * <p>
 * The protection facets are the effective ones: the values the category declares or, where it declares none,
 * those inherited from its parent. A {@link Taxonomy} guarantees they are never weaker than the parent's.
 *
 * @param info the key, parent, name, description, origin and lifecycle
 * @param nature what the data is under data protection law
 * @param confidentiality the harm disclosure would cause
 * @param integrity the harm an unwanted change would cause
 * @param secret whether the data grants access (passwords, keys, tokens) and must never be read back, exported or
 *        published, only verified
 * @param attributes application-defined properties, inherited by children unless they set their own value; for
 *        example the group of a record of processing activities or a retention class
 * @since 0.1.0
 */
public record DataCategory(TermInfo info, LegalNature nature, Confidentiality confidentiality, Integrity integrity,
                           boolean secret, Map<String, String> attributes) implements Term {

    /**
     * Checks the components and makes the attributes immutable.
     *
     * @param info the term information
     * @param nature the legal nature
     * @param confidentiality the confidentiality level
     * @param integrity the integrity level
     * @param secret whether it is a secret
     * @param attributes the application-defined attributes
     */
    public DataCategory {
        Objects.requireNonNull(info, "info");
        Objects.requireNonNull(nature, "nature");
        Objects.requireNonNull(confidentiality, "confidentiality");
        Objects.requireNonNull(integrity, "integrity");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * Whether the category is personal data.
     *
     * @return {@code true} unless the nature is {@link LegalNature#NONE}
     */
    public boolean isPersonal() {
        return nature.isPersonal();
    }

    /**
     * An application-defined attribute.
     *
     * @param name the attribute name
     * @return its value, or {@code null} if neither this category nor an ancestor sets it
     */
    public String attribute(String name) {
        return attributes.get(name);
    }
}
