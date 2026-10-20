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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The names of the DataHub glossary nodes under which the taxonomy is published. Each name is the first segment of
 * the URNs below it: with the defaults, {@code user.contact.email} is the term
 * {@code urn:li:glossaryTerm:DataCategory.user.contact.email}.
 * <p>
 * Every name must start with a letter and contain only letters, digits and underscores.
 *
 * @param categories the node that holds the category hierarchy
 * @param personalData the node of the legal nature terms {@code Personal}, {@code SpecialCategory} and
 *        {@code CriminalOffence}
 * @param confidentiality the node of the confidentiality levels
 * @param integrity the node of the integrity levels
 * @param identifiability the node of the identifiability of personal fields
 * @param handling the node that holds the {@code Secret} term
 * @since 0.1.0
 */
public record DataHubMapping(String categories, String personalData, String confidentiality, String integrity,
                             String identifiability, String handling) {

    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    /**
     * Checks that every name is present and valid, and that no two names are equal.
     *
     * @param categories the categories node
     * @param personalData the legal nature node
     * @param confidentiality the confidentiality node
     * @param integrity the integrity node
     * @param identifiability the identifiability node
     * @param handling the handling node
     */
    public DataHubMapping {
        Set<String> names = new HashSet<>();
        for (String name : new String[] {categories, personalData, confidentiality, integrity, identifiability,
                handling}) {
            Objects.requireNonNull(name, "glossary node name");
            if (!NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Invalid DataHub glossary node name '" + name + "'");
            }
            if (!names.add(name)) {
                throw new IllegalArgumentException("Duplicate DataHub glossary node name '" + name + "'");
            }
        }
    }

    /**
     * The default names: {@code DataCategory}, {@code PersonalData}, {@code Confidentiality}, {@code Integrity},
     * {@code Identifiability} and {@code DataHandling}.
     *
     * @return the default mapping
     */
    public static DataHubMapping defaults() {
        return new DataHubMapping("DataCategory", "PersonalData", "Confidentiality", "Integrity", "Identifiability",
                "DataHandling");
    }

    /**
     * A copy with another name for the categories node.
     *
     * @param name the new name
     * @return the new mapping
     */
    public DataHubMapping withCategories(String name) {
        return new DataHubMapping(name, personalData, confidentiality, integrity, identifiability, handling);
    }

    /**
     * A copy with another name for the legal nature node.
     *
     * @param name the new name
     * @return the new mapping
     */
    public DataHubMapping withPersonalData(String name) {
        return new DataHubMapping(categories, name, confidentiality, integrity, identifiability, handling);
    }

    /**
     * A copy with another name for the confidentiality node.
     *
     * @param name the new name
     * @return the new mapping
     */
    public DataHubMapping withConfidentiality(String name) {
        return new DataHubMapping(categories, personalData, name, integrity, identifiability, handling);
    }

    /**
     * A copy with another name for the integrity node.
     *
     * @param name the new name
     * @return the new mapping
     */
    public DataHubMapping withIntegrity(String name) {
        return new DataHubMapping(categories, personalData, confidentiality, name, identifiability, handling);
    }

    /**
     * A copy with another name for the identifiability node.
     *
     * @param name the new name
     * @return the new mapping
     */
    public DataHubMapping withIdentifiability(String name) {
        return new DataHubMapping(categories, personalData, confidentiality, integrity, name, handling);
    }

    /**
     * A copy with another name for the handling node.
     *
     * @param name the new name
     * @return the new mapping
     */
    public DataHubMapping withHandling(String name) {
        return new DataHubMapping(categories, personalData, confidentiality, integrity, identifiability, name);
    }
}
