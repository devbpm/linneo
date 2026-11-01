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

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.InventoryField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OdcsSchemaTest {

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final OdcsSchema schema = OdcsSchema.of(taxonomy);
    private final DataInventory inventory = DataInventory.builder()
            .field("students", "disability", Categories.USER_HEALTH_AND_MEDICAL_DISABILITY, Basis.DECLARED)
            .field("students", "password", Categories.USER_AUTHORIZATION_PASSWORD, Basis.DECLARED)
            .field("students", "version", Categories.SYSTEM_OPERATIONS, Basis.CLASS_DEFAULT)
            .field(InventoryField.exempt("students", "cache", "rendering cache"))
            .build();

    @Test
    @DisplayName("each object is a schema object and each classified field a property with its classification")
    void schema() {
        String json = schema.schemaJson(inventory);
        assertAll(
                () -> assertTrue(json.startsWith("[{\"name\":\"students\",\"logicalType\":\"object\",\"properties\":["),
                        json),
                () -> assertTrue(json.contains("{\"name\":\"disability\",\"classification\":\"restricted\",\"tags\":"
                        + "[\"user.health_and_medical.disability\",\"personal-data\",\"special-category\"],"
                        + "\"customProperties\":[{\"property\":\"linneo.category\",\"value\":"
                        + "\"user.health_and_medical.disability\"},{\"property\":\"linneo.confidentiality\","
                        + "\"value\":\"HIGH\"},{\"property\":\"linneo.integrity\",\"value\":\"HIGH\"},"
                        + "{\"property\":\"linneo.identifiability\",\"value\":\"IDENTIFIED\"}]}"), json),
                () -> assertTrue(json.contains("\"name\":\"password\",\"classification\":\"restricted\""), json),
                () -> assertTrue(json.contains("\"secret\"]"), json),
                () -> assertTrue(json.contains("{\"name\":\"version\",\"classification\":"), json),
                () -> assertFalse(json.contains("\"cache\""), json),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> schema.propertyJson(InventoryField.exempt("s", "cache", "c"))));
    }

    @Test
    @DisplayName("the classification follows confidentiality on the usual four-level scale, and secrets are restricted")
    void classification() {
        assertAll(
                () -> assertEquals("public", OdcsSchema.classification(protection(Confidentiality.PUBLIC, false))),
                () -> assertEquals("internal", OdcsSchema.classification(protection(Confidentiality.LOW, false))),
                () -> assertEquals("confidential", OdcsSchema.classification(protection(Confidentiality.MEDIUM,
                        false))),
                () -> assertEquals("restricted", OdcsSchema.classification(protection(Confidentiality.HIGH, false))),
                () -> assertEquals("restricted", OdcsSchema.classification(protection(Confidentiality.LOW, true))));
    }

    private static Protection protection(Confidentiality confidentiality, boolean secret) {
        return Protection.of(LegalNature.NONE, confidentiality, Integrity.LOW, secret);
    }
}
