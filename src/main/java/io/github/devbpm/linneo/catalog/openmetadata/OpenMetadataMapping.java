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

import io.github.devbpm.linneo.Confidentiality;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The names under which the taxonomy is published in OpenMetadata, and when a column counts as sensitive.
 * <p>
 * {@link #personalData()} and {@link #pii()} name OpenMetadata's built-in classifications, which are reused and
 * never created; the others are created by Linneo. Every name must be a valid OpenMetadata entity name without
 * dots.
 *
 * @param categories the classification that holds the category hierarchy as nested tags
 * @param personalData the built-in classification with the tags {@code Personal} and {@code SpecialCategory}
 * @param pii the built-in classification with the tags {@code None}, {@code NonSensitive} and {@code Sensitive}
 * @param confidentiality the classification of the confidentiality levels
 * @param integrity the classification of the integrity levels
 * @param identifiability the classification of the identifiability of personal columns
 * @param handling the classification that holds the {@code Secret} tag
 * @param sensitiveFrom the lowest confidentiality of personal data tagged {@code PII.Sensitive}; lower levels are
 *        {@code PII.NonSensitive}
 * @since 0.1.0
 */
public record OpenMetadataMapping(String categories, String personalData, String pii, String confidentiality,
                                  String integrity, String identifiability, String handling,
                                  Confidentiality sensitiveFrom) {

    /**
     * Checks that no component is missing and no name contains a dot, and that no two names are equal.
     *
     * @param categories the categories classification
     * @param personalData the built-in personal data classification
     * @param pii the built-in PII classification
     * @param confidentiality the confidentiality classification
     * @param integrity the integrity classification
     * @param identifiability the identifiability classification
     * @param handling the handling classification
     * @param sensitiveFrom the threshold for {@code PII.Sensitive}
     */
    public OpenMetadataMapping {
        Set<String> names = new HashSet<>();
        for (String name : new String[] {categories, personalData, pii, confidentiality, integrity, identifiability,
                handling}) {
            Objects.requireNonNull(name, "classification name");
            if (name.isBlank() || name.contains(".")) {
                throw new IllegalArgumentException("Invalid OpenMetadata classification name '" + name + "'");
            }
            if (!names.add(name)) {
                throw new IllegalArgumentException("Duplicate OpenMetadata classification name '" + name + "'");
            }
        }
        Objects.requireNonNull(sensitiveFrom, "sensitiveFrom");
    }

    /**
     * The default names: {@code DataCategory}, {@code PersonalData}, {@code PII}, {@code Confidentiality},
     * {@code Integrity}, {@code Identifiability} and {@code DataHandling}; personal data is sensitive from
     * {@link Confidentiality#MEDIUM}.
     *
     * @return the default mapping
     */
    public static OpenMetadataMapping defaults() {
        return new OpenMetadataMapping("DataCategory", "PersonalData", "PII", "Confidentiality", "Integrity",
                "Identifiability", "DataHandling", Confidentiality.MEDIUM);
    }

    /**
     * A copy with another name for the category classification.
     *
     * @param name the new name
     * @return the new mapping
     */
    public OpenMetadataMapping withCategories(String name) {
        return new OpenMetadataMapping(name, personalData, pii, confidentiality, integrity, identifiability, handling,
                sensitiveFrom);
    }

    /**
     * A copy with another name for the confidentiality classification.
     *
     * @param name the new name
     * @return the new mapping
     */
    public OpenMetadataMapping withConfidentiality(String name) {
        return new OpenMetadataMapping(categories, personalData, pii, name, integrity, identifiability, handling,
                sensitiveFrom);
    }

    /**
     * A copy with another threshold for {@code PII.Sensitive}.
     *
     * @param level the lowest confidentiality of personal data that is sensitive
     * @return the new mapping
     */
    public OpenMetadataMapping withSensitiveFrom(Confidentiality level) {
        return new OpenMetadataMapping(categories, personalData, pii, confidentiality, integrity, identifiability,
                handling, level);
    }
}
