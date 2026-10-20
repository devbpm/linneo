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
package io.github.devbpm.linneo.catalog.atlas;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The names of the root Apache Atlas classifications under which the taxonomy is published. Each name is the
 * first segment of the classifications below it: with the defaults, {@code user.contact.email} is the
 * classification {@code DataCategory.user.contact.email}.
 * <p>
 * Every name must start with a letter and contain only letters, digits and underscores.
 *
 * @param categories the root classification of the category hierarchy
 * @param personalData the root of the legal nature classifications {@code Personal}, {@code SpecialCategory} and
 *        {@code CriminalOffence}
 * @param confidentiality the root of the confidentiality levels
 * @param integrity the root of the integrity levels
 * @param identifiability the root of the identifiability of personal columns
 * @param handling the root of the {@code Secret} classification
 * @since 0.1.0
 */
public record AtlasMapping(String categories, String personalData, String confidentiality, String integrity,
                           String identifiability, String handling) {

    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    /**
     * Checks that every name is present and valid, and that no two names are equal.
     *
     * @param categories the categories root
     * @param personalData the legal nature root
     * @param confidentiality the confidentiality root
     * @param integrity the integrity root
     * @param identifiability the identifiability root
     * @param handling the handling root
     */
    public AtlasMapping {
        Set<String> names = new HashSet<>();
        for (String name : new String[] {categories, personalData, confidentiality, integrity, identifiability,
                handling}) {
            Objects.requireNonNull(name, "classification name");
            if (!NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Invalid Atlas classification name '" + name + "'");
            }
            if (!names.add(name)) {
                throw new IllegalArgumentException("Duplicate Atlas classification name '" + name + "'");
            }
        }
    }

    /**
     * The default names: {@code DataCategory}, {@code PersonalData}, {@code Confidentiality}, {@code Integrity},
     * {@code Identifiability} and {@code DataHandling}.
     *
     * @return the default mapping
     */
    public static AtlasMapping defaults() {
        return new AtlasMapping("DataCategory", "PersonalData", "Confidentiality", "Integrity", "Identifiability",
                "DataHandling");
    }

    /**
     * A copy with another name for the categories root.
     *
     * @param name the new name
     * @return the new mapping
     */
    public AtlasMapping withCategories(String name) {
        return new AtlasMapping(name, personalData, confidentiality, integrity, identifiability, handling);
    }

    /**
     * A copy with another name for the legal nature root.
     *
     * @param name the new name
     * @return the new mapping
     */
    public AtlasMapping withPersonalData(String name) {
        return new AtlasMapping(categories, name, confidentiality, integrity, identifiability, handling);
    }

    /**
     * A copy with another name for the confidentiality root.
     *
     * @param name the new name
     * @return the new mapping
     */
    public AtlasMapping withConfidentiality(String name) {
        return new AtlasMapping(categories, personalData, name, integrity, identifiability, handling);
    }

    /**
     * A copy with another name for the integrity root.
     *
     * @param name the new name
     * @return the new mapping
     */
    public AtlasMapping withIntegrity(String name) {
        return new AtlasMapping(categories, personalData, confidentiality, name, identifiability, handling);
    }

    /**
     * A copy with another name for the identifiability root.
     *
     * @param name the new name
     * @return the new mapping
     */
    public AtlasMapping withIdentifiability(String name) {
        return new AtlasMapping(categories, personalData, confidentiality, integrity, name, handling);
    }

    /**
     * A copy with another name for the handling root.
     *
     * @param name the new name
     * @return the new mapping
     */
    public AtlasMapping withHandling(String name) {
        return new AtlasMapping(categories, personalData, confidentiality, integrity, identifiability, name);
    }
}
