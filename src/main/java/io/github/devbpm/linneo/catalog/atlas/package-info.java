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

/**
 * The taxonomy as <a href="https://atlas.apache.org/">Apache Atlas</a> classifications.
 * <p>
 * {@link io.github.devbpm.linneo.catalog.atlas.AtlasCatalog} computes the classification types to create and which
 * classifications each classified column gets; it makes no network calls. Send its
 * {@link io.github.devbpm.linneo.catalog.atlas.AtlasCatalog#typeDefsJson() type definitions} with any HTTP client to
 * {@code POST /api/atlas/v2/types/typedefs}, then add the
 * {@link io.github.devbpm.linneo.catalog.atlas.AtlasCatalog#classificationsJson(io.github.devbpm.linneo.classify.FieldClassification)
 * classifications} to the column entities.
 */
package io.github.devbpm.linneo.catalog.atlas;
