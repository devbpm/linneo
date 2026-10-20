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
package io.github.devbpm.linneo;

/**
 * Terms an application or a sector adds to the taxonomy, and the stricter protection it requires on existing ones.
 * <p>
 * Register an implementation with {@link java.util.ServiceLoader} (a {@code provides} clause in
 * {@code module-info.java}, or {@code META-INF/services/io.github.devbpm.linneo.TaxonomyExtension} on the class
 * path) to have {@link Taxonomy#load()} apply it, or pass it to {@link TaxonomyBuilder#extend(TaxonomyExtension)}.
 * <pre>{@code
 * public final class UniversityTaxonomy implements TaxonomyExtension {
 *
 *     public String name() {
 *         return "university";
 *     }
 *
 *     public void contribute(TaxonomyBuilder builder) {
 *         builder.category("user.education.thesis", "Thesis", "Theses and dissertations of the subject.")
 *                .refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH));
 *     }
 * }
 * }</pre>
 * An extension may only add children to existing terms and make protection stricter; the rules are enforced when
 * the taxonomy is {@linkplain TaxonomyBuilder#build() built}.
 *
 * @since 0.1.0
 */
public interface TaxonomyExtension {

    /**
     * The name recorded as the {@linkplain TermInfo#origin() origin} of the terms this extension adds.
     *
     * @return a short, stable name; never {@value TermInfo#FIDESLANG} or {@value TermInfo#LINNEO}
     */
    String name();

    /**
     * Adds terms to, or refines terms of, the taxonomy being built.
     *
     * @param builder the builder, already holding the standard terms and the extensions applied before this one
     */
    void contribute(TaxonomyBuilder builder);
}
