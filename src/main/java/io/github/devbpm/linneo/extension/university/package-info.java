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
 * An optional taxonomy extension for universities and other higher education institutions.
 * <p>
 * {@link io.github.devbpm.linneo.extension.university.UniversityTaxonomy} adds data categories, data uses and data
 * subjects for the domain. It ships with Linneo but is not applied by default; apply it with
 * {@link io.github.devbpm.linneo.TaxonomyBuilder#extend(io.github.devbpm.linneo.TaxonomyExtension)} or register it
 * as a service of the application, as its class documentation explains. Its keys are in {@link io.github.devbpm.linneo.extension.university.UniversityCategories},
 * {@link io.github.devbpm.linneo.extension.university.UniversityDataUses} and
 * {@link io.github.devbpm.linneo.extension.university.UniversityDataSubjects}.
 */
package io.github.devbpm.linneo.extension.university;
