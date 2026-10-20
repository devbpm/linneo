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
 * The annotations that declare what each field of a data model holds.
 * <p>
 * A model declares what it holds with {@link io.github.devbpm.linneo.annotation.Category @Category} on each field,
 * {@link io.github.devbpm.linneo.annotation.DefaultCategory @DefaultCategory} on a class whose fields hold no
 * personal data, {@link io.github.devbpm.linneo.annotation.CategoryByRow @CategoryByRow} when the category depends
 * on each row, and {@link io.github.devbpm.linneo.annotation.Exempt @Exempt} on fields that hold no data at all.
 * Use the constants of {@link io.github.devbpm.linneo.Categories} as keys, and verify the annotations with
 * {@link io.github.devbpm.linneo.classify.Classifier}.
 */
package io.github.devbpm.linneo.annotation;
