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
package io.github.devbpm.linneo.catalog.openmetadata;

import io.github.devbpm.linneo.internal.Json;

import java.util.Objects;

/**
 * An OpenMetadata classification to create: the body of {@code PUT /api/v1/classifications}.
 *
 * @param name the classification name
 * @param description what it classifies
 * @param mutuallyExclusive whether an entity can carry only one of its tags
 * @since 0.1.0
 */
public record OmClassification(String name, String description, boolean mutuallyExclusive) {

    /**
     * Checks the mandatory components.
     *
     * @param name the name
     * @param description the description
     * @param mutuallyExclusive whether its tags exclude each other
     */
    public OmClassification {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
    }

    /**
     * The JSON body of a {@code CreateClassification} request.
     *
     * @return a JSON object
     */
    public String toJson() {
        return new Json().field("name", name).field("description", description)
                .field("mutuallyExclusive", mutuallyExclusive).end();
    }
}
