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
package io.github.devbpm.linneo.export.odcs;

import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.InventoryField;
import io.github.devbpm.linneo.internal.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The classification of a data inventory as the {@code schema} of an
 * <a href="https://bitol-io.github.io/open-data-contract-standard/">Open Data Contract Standard</a> (ODCS v3) data
 * contract: one schema object per object of the inventory, one property per classified field, so a data contract
 * says what each property holds and how it must be protected.
 * <p>
 * Each property gets a {@code classification} on the usual four-level scale, from its confidentiality:
 * {@code public}, {@code internal} ({@code LOW}), {@code confidential} ({@code MEDIUM}) and {@code restricted}
 * ({@code HIGH}, and every secret). Its {@code tags} are the category key and {@code personal-data},
 * {@code special-category}, {@code criminal-offence-data} and {@code secret} where they apply, and its
 * {@code customProperties} carry the category key, the levels and the identifiability under the prefix
 * {@code linneo.}. Exempted fields hold no data and are left out.
 * <p>
 * The result is JSON, which is also valid YAML: merge it into the contract, whose servers, types and owners Linneo
 * does not know. Property names are the field paths ({@code home.street} for a composite part); map them when the
 * contract uses other names. Instances are immutable and thread-safe.
 *
 * @since 0.1.0
 */
public final class OdcsSchema {

    private final Taxonomy taxonomy;

    private OdcsSchema(Taxonomy taxonomy) {
        this.taxonomy = Objects.requireNonNull(taxonomy, "taxonomy");
    }

    /**
     * The schema writer for a taxonomy, which resolves the stored keys.
     *
     * @param taxonomy the taxonomy
     * @return the writer
     */
    public static OdcsSchema of(Taxonomy taxonomy) {
        return new OdcsSchema(taxonomy);
    }

    /**
     * The classification label of a protection.
     *
     * @param protection the protection a field requires
     * @return {@code public}, {@code internal}, {@code confidential} or {@code restricted}
     */
    public static String classification(Protection protection) {
        if (protection.secret()) {
            return "restricted";
        }
        return switch (protection.confidentiality()) {
            case PUBLIC -> "public";
            case LOW -> "internal";
            case MEDIUM -> "confidential";
            case HIGH -> "restricted";
        };
    }

    /**
     * The {@code schema} of a contract for every object of an inventory.
     *
     * @param inventory the inventory
     * @return a JSON array of schema objects, in the order of the objects
     * @throws TaxonomyException if the taxonomy does not contain a key of the inventory
     */
    public String schemaJson(DataInventory inventory) {
        List<String> objects = new ArrayList<>();
        for (String object : inventory.objects()) {
            objects.add(objectJson(inventory, object));
        }
        return Json.array(objects);
    }

    /**
     * The schema object of one object of an inventory.
     *
     * @param inventory the inventory
     * @param object the key of the object
     * @return a JSON object with {@code name}, {@code logicalType} and {@code properties}
     * @throws TaxonomyException if the taxonomy does not contain a key of the object
     */
    public String objectJson(DataInventory inventory, String object) {
        List<String> properties = new ArrayList<>();
        for (InventoryField field : inventory.fields(object)) {
            if (!field.isExempt()) {
                properties.add(propertyJson(field));
            }
        }
        return new Json().field("name", object).field("logicalType", "object")
                .raw("properties", Json.array(properties)).end();
    }

    /**
     * The schema property of one stored field.
     *
     * @param field a classified field of an inventory
     * @return a JSON object with {@code name}, {@code classification}, {@code tags} and {@code customProperties}
     * @throws IllegalArgumentException if the field is exempted
     * @throws TaxonomyException if the taxonomy does not contain the field's key
     */
    public String propertyJson(InventoryField field) {
        if (field.isExempt()) {
            throw new IllegalArgumentException(field + " is exempted and holds no data");
        }
        DataCategory category = field.category(taxonomy);
        Protection protection = field.protection(taxonomy);
        boolean personal = field.isPersonal(taxonomy);
        List<String> tags = new ArrayList<>();
        tags.add(category.key());
        if (personal) {
            tags.add("personal-data");
            if (category.nature() == LegalNature.SPECIAL_CATEGORY) {
                tags.add("special-category");
            } else if (category.nature() == LegalNature.CRIMINAL_OFFENCE) {
                tags.add("criminal-offence-data");
            }
        }
        if (protection.secret()) {
            tags.add("secret");
        }
        List<String> custom = new ArrayList<>();
        custom.add(custom("linneo.category", category.key()));
        custom.add(custom("linneo.confidentiality", protection.confidentiality().name()));
        custom.add(custom("linneo.integrity", protection.integrity().name()));
        if (personal) {
            custom.add(custom("linneo.identifiability", field.identifiability(taxonomy).name()));
        }
        return new Json().field("name", field.path()).field("classification", classification(protection))
                .field("tags", tags).raw("customProperties", Json.array(custom)).end();
    }

    private static String custom(String property, String value) {
        return new Json().field("property", property).field("value", value).end();
    }
}
