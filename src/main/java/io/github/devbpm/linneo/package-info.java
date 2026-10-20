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
 * The taxonomy: its terms, their protection, the key constants and the builder.
 * <p>
 * A {@link io.github.devbpm.linneo.Taxonomy} holds three hierarchies of {@link io.github.devbpm.linneo.Term terms}
 * keyed with dot notation: {@link io.github.devbpm.linneo.DataCategory data categories} (what a field holds),
 * {@link io.github.devbpm.linneo.DataUse data uses} (why it is processed) and
 * {@link io.github.devbpm.linneo.DataSubject data subjects} (whose data it is). The
 * {@linkplain io.github.devbpm.linneo.Taxonomy#standard() standard taxonomy} is the fideslang default taxonomy
 * plus Linneo's extensions for non-personal and education data.
 * <p>
 * Every data category carries the protection it requires: its {@link io.github.devbpm.linneo.LegalNature},
 * {@link io.github.devbpm.linneo.Confidentiality}, {@link io.github.devbpm.linneo.Integrity} and whether it is a
 * secret. A child inherits them from its parent and may only make them stricter.
 * <p>
 * The constants of {@link io.github.devbpm.linneo.Categories}, {@link io.github.devbpm.linneo.DataUses} and
 * {@link io.github.devbpm.linneo.DataSubjects} name every standard key. Applications build their own taxonomy
 * with {@link io.github.devbpm.linneo.TaxonomyBuilder} or contribute terms through a
 * {@link io.github.devbpm.linneo.TaxonomyExtension}. The annotations that apply the taxonomy to a data model are
 * in {@link io.github.devbpm.linneo.annotation}.
 */
package io.github.devbpm.linneo;
