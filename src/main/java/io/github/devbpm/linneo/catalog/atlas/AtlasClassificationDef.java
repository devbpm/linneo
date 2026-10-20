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
package io.github.devbpm.linneo.catalog.atlas;

import io.github.devbpm.linneo.internal.Json;

import java.util.List;
import java.util.Objects;

/**
 * An Apache Atlas classification type to create: one element of {@code classificationDefs} in
 * {@code POST /api/atlas/v2/types/typedefs}.
 *
 * @param name the classification name, such as {@code DataCategory.user.contact.email}
 * @param description what it means
 * @param superTypes the classifications it specializes; an entity with this classification also matches them
 * @since 0.1.0
 */
public record AtlasClassificationDef(String name, String description, List<String> superTypes) {

    /**
     * Checks the mandatory components and makes the list immutable.
     *
     * @param name the name
     * @param description the description
     * @param superTypes the super types
     */
    public AtlasClassificationDef {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        superTypes = superTypes == null ? List.of() : List.copyOf(superTypes);
    }

    /**
     * The JSON of the classification definition.
     *
     * @return a JSON object
     */
    public String toJson() {
        return new Json().field("name", name).field("description", description).field("superTypes", superTypes)
                .raw("attributeDefs", "[]").end();
    }
}
