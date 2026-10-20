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

/**
 * A term of a taxonomy: a node of one of its three hierarchies.
 *
 * @since 0.1.0
 */
public sealed interface Term permits DataCategory, DataUse, DataSubject {

    /**
     * What the term has in common with every other term.
     *
     * @return the key, parent, name, description, origin and lifecycle of the term
     */
    TermInfo info();

    /**
     * Shortcut for {@code info().key()}.
     *
     * @return the unique key in dot notation
     */
    default String key() {
        return info().key();
    }

    /**
     * Shortcut for {@code info().name()}.
     *
     * @return the human-readable name
     */
    default String name() {
        return info().name();
    }

    /**
     * Whether this term is {@code ancestorKey} or one of its descendants.
     *
     * @param ancestorKey a key of the same hierarchy
     * @return {@code true} if this key equals {@code ancestorKey} or starts with it followed by a dot
     */
    default boolean isA(String ancestorKey) {
        String key = key();
        return key.equals(ancestorKey) || key.startsWith(ancestorKey + ".");
    }
}
