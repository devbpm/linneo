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
package io.github.devbpm.linneo.catalog.datahub;

import io.github.devbpm.linneo.internal.Json;

import java.util.Objects;

/**
 * A DataHub metadata change proposal that upserts one aspect of one entity: the body of
 * {@code POST /aspects?action=ingestProposal}.
 *
 * @param entityType the entity type, {@code glossaryNode} or {@code glossaryTerm}
 * @param entityUrn the URN of the entity
 * @param aspectName the aspect written, such as {@code glossaryTermInfo}
 * @param aspect the aspect as a JSON object
 * @since 0.1.0
 */
public record DhProposal(String entityType, String entityUrn, String aspectName, String aspect) {

    /**
     * Checks the mandatory components.
     *
     * @param entityType the entity type
     * @param entityUrn the entity URN
     * @param aspectName the aspect name
     * @param aspect the aspect
     */
    public DhProposal {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(entityUrn, "entityUrn");
        Objects.requireNonNull(aspectName, "aspectName");
        Objects.requireNonNull(aspect, "aspect");
    }

    /**
     * The JSON body of an {@code ingestProposal} request. The aspect travels JSON-encoded as a string, as the API
     * requires.
     *
     * @return a JSON object
     */
    public String toJson() {
        String serialized = new Json().field("value", aspect).field("contentType", "application/json").end();
        String proposal = new Json().field("entityType", entityType).field("entityUrn", entityUrn)
                .field("changeType", "UPSERT").field("aspectName", aspectName).raw("aspect", serialized).end();
        return new Json().raw("proposal", proposal).end();
    }
}
