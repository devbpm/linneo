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
 * The taxonomy as a <a href="https://datahub.com/">DataHub</a> business glossary.
 * <p>
 * {@link io.github.devbpm.linneo.catalog.datahub.DataHubCatalog} computes the glossary nodes and terms to create and which
 * terms each classified field gets; it makes no network calls. Send its
 * {@link io.github.devbpm.linneo.catalog.datahub.DhProposal#toJson() proposals} with any HTTP client to
 * {@code POST /aspects?action=ingestProposal} of the DataHub metadata service, then add the terms to the fields of
 * each dataset.
 */
package io.github.devbpm.linneo.catalog.datahub;
