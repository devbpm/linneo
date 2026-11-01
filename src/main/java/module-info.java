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
 * Linneo: a data classification taxonomy for Java data models.
 * <ul>
 *   <li>{@link io.github.devbpm.linneo} &mdash; the taxonomy, its terms, the key constants and the builder;</li>
 *   <li>{@link io.github.devbpm.linneo.annotation} &mdash; the annotations that classify the fields of a model;</li>
 *   <li>{@link io.github.devbpm.linneo.classify} &mdash; reading and verifying the annotations of a model;</li>
 *   <li>{@link io.github.devbpm.linneo.processing} &mdash; processing activities and the GDPR record of processing
 *       activities;</li>
 *   <li>{@link io.github.devbpm.linneo.catalog.openmetadata} &mdash; the taxonomy as OpenMetadata classifications and tags;</li>
 *   <li>{@link io.github.devbpm.linneo.catalog.datahub} &mdash; the taxonomy as a DataHub business glossary;</li>
 *   <li>{@link io.github.devbpm.linneo.catalog.atlas} &mdash; the taxonomy as Apache Atlas classifications;</li>
 *   <li>{@link io.github.devbpm.linneo.extension.university} &mdash; an optional extension for universities;</li>
 *   <li>{@link io.github.devbpm.linneo.extension.finance} &mdash; an optional extension for finance and
 *       accounting;</li>
 *   <li>{@link io.github.devbpm.linneo.extension.supplychain} &mdash; an optional extension for supply chain
 *       management;</li>
 *   <li>{@link io.github.devbpm.linneo.extension.projectoperations} &mdash; an optional extension for project
 *       operations;</li>
 *   <li>{@link io.github.devbpm.linneo.extension.humanresources} &mdash; an optional extension for human resources
 *       and payroll.</li>
 * </ul>
 * Applications contribute their own terms through {@link io.github.devbpm.linneo.TaxonomyExtension}. The module
 * provides no extension as a service: the optional extensions are not applied by default, but explicitly, alone or
 * together.
 */
module io.github.devbpm.linneo {
    exports io.github.devbpm.linneo;
    exports io.github.devbpm.linneo.annotation;
    exports io.github.devbpm.linneo.classify;
    exports io.github.devbpm.linneo.processing;
    exports io.github.devbpm.linneo.catalog.openmetadata;
    exports io.github.devbpm.linneo.catalog.datahub;
    exports io.github.devbpm.linneo.catalog.atlas;
    exports io.github.devbpm.linneo.extension.university;
    exports io.github.devbpm.linneo.extension.finance;
    exports io.github.devbpm.linneo.extension.supplychain;
    exports io.github.devbpm.linneo.extension.projectoperations;
    exports io.github.devbpm.linneo.extension.humanresources;

    uses io.github.devbpm.linneo.TaxonomyExtension;
}
