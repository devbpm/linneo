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

import java.util.List;
import java.util.Objects;

/**
 * An OpenMetadata tag to create: the body of {@code PUT /api/v1/tags}.
 *
 * @param classification the name of the classification it belongs to
 * @param parent the fully qualified name of its parent tag, or {@code null} at the root of the classification
 * @param name the tag name, one segment without dots
 * @param displayName the human-readable name
 * @param description what it means
 * @param associatedTags fully qualified names of tags that go with it, so that whoever applies it by hand gets them
 *        too
 * @since 0.1.0
 */
public record OmTag(String classification, String parent, String name, String displayName, String description,
                    List<String> associatedTags) {

    /**
     * Checks the mandatory components and makes the list immutable.
     *
     * @param classification the classification
     * @param parent the parent's fully qualified name or {@code null}
     * @param name the name
     * @param displayName the display name
     * @param description the description
     * @param associatedTags the associated tags
     */
    public OmTag {
        Objects.requireNonNull(classification, "classification");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(description, "description");
        associatedTags = associatedTags == null ? List.of() : List.copyOf(associatedTags);
    }

    /**
     * The fully qualified name: the parent's (or the classification's) followed by a dot and the name.
     *
     * @return the name used to apply the tag, such as {@code DataCategory.user.contact.email}
     */
    public String fullyQualifiedName() {
        return (parent == null ? classification : parent) + "." + name;
    }

    /**
     * The JSON body of a {@code CreateTag} request.
     *
     * @return a JSON object
     */
    public String toJson() {
        Json json = new Json().field("classification", classification);
        if (parent != null) {
            json.field("parent", parent);
        }
        json.field("name", name).field("displayName", displayName).field("description", description);
        if (!associatedTags.isEmpty()) {
            json.field("associatedTags", associatedTags);
        }
        return json.end();
    }
}
