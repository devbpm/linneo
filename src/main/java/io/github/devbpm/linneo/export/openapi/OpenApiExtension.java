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
package io.github.devbpm.linneo.export.openapi;

import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.classify.ClassClassification;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.classify.InventoryField;
import io.github.devbpm.linneo.internal.Json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The classification of the fields of an API's data as an OpenAPI specification extension, so that the description
 * of an API says what personal data each property carries and how much protection it needs. The data an API returns
 * is the data that leaves the system, and its transfer objects are classified like any class, often with
 * {@link io.github.devbpm.linneo.annotation.DerivedFrom @DerivedFrom}.
 * <p>
 * Each property gets one extension, {@value #NAME}, whose value is an object such as:
 * <pre>{@code
 * "x-linneo": {"category": "user.contact.email", "personal": true, "nature": "ORDINARY",
 *              "confidentiality": "LOW", "integrity": "LOW", "identifiability": "IDENTIFIED", "secret": false}
 * }</pre>
 * {@code nature} and {@code identifiability} are present only for personal data. The values are maps, strings and
 * booleans that any OpenAPI library can add to a schema property, for instance with a springdoc or swagger-core
 * customizer: {@code property.addExtension(OpenApiExtension.NAME, OpenApiExtension.of(field))}. The extension is
 * also valid in a JSON Schema, which ignores unknown keywords. Linneo does not read or write the API description:
 * the application maps a field path to its property, which may be renamed or nested.
 *
 * @since 0.1.0
 */
public final class OpenApiExtension {

    /**
     * The name of the extension.
     */
    public static final String NAME = "x-linneo";

    private OpenApiExtension() {
    }

    /**
     * The extension of a property that carries a field.
     *
     * @param field the classification of the field
     * @return an immutable map in a stable order
     */
    public static Map<String, Object> of(FieldClassification field) {
        return extension(field.category(), field.protection(), field.isPersonal(), field.identifiability());
    }

    /**
     * The extension of a property that carries a stored field, resolved with a taxonomy.
     *
     * @param field a field of a {@link io.github.devbpm.linneo.classify.DataInventory}
     * @param taxonomy the taxonomy to resolve its key with
     * @return an immutable map in a stable order; empty for an exempted field, which carries no data
     * @throws TaxonomyException if the taxonomy does not contain the field's key
     */
    public static Map<String, Object> of(InventoryField field, Taxonomy taxonomy) {
        if (field.isExempt()) {
            return Map.of();
        }
        return extension(field.category(taxonomy), field.protection(taxonomy), field.isPersonal(taxonomy),
                field.identifiability(taxonomy));
    }

    /**
     * The extensions of every classified field of a class.
     *
     * @param classification the classification of the class, without errors
     * @return the extensions by field path, in the order of the fields; exempted fields have none
     * @throws IllegalArgumentException if the classification has errors, so that some fields would be described
     *         without their classification
     */
    public static Map<String, Map<String, Object>> of(ClassClassification classification) {
        if (!classification.isValid()) {
            throw new IllegalArgumentException(classification.type().getName() + " has classification errors: "
                    + classification.errors());
        }
        Map<String, Map<String, Object>> extensions = new LinkedHashMap<>();
        for (FieldClassification field : classification.fields()) {
            extensions.put(field.path(), of(field));
        }
        return Collections.unmodifiableMap(extensions);
    }

    /**
     * The extensions of every classified field of a class as JSON, for tools that are not written in Java.
     *
     * @param classification the classification of the class, without errors
     * @return a JSON object whose members are the field paths and whose values are the extensions
     * @throws IllegalArgumentException if the classification has errors
     */
    public static String toJson(ClassClassification classification) {
        Json json = new Json();
        of(classification).forEach((path, extension) -> json.raw(path, toJson(extension)));
        return json.end();
    }

    private static String toJson(Map<String, Object> extension) {
        Json json = new Json();
        extension.forEach((name, value) -> {
            if (value instanceof Boolean flag) {
                json.field(name, flag);
            } else {
                json.field(name, value.toString());
            }
        });
        return json.end();
    }

    private static Map<String, Object> extension(DataCategory category, Protection protection, boolean personal,
                                                 Identifiability identifiability) {
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("category", category.key());
        extension.put("personal", personal);
        if (personal) {
            extension.put("nature", category.nature().name());
        }
        extension.put("confidentiality", protection.confidentiality().name());
        extension.put("integrity", protection.integrity().name());
        if (personal && identifiability != null) {
            extension.put("identifiability", identifiability.name());
        }
        extension.put("secret", protection.secret());
        return Collections.unmodifiableMap(extension);
    }
}
