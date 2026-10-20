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

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.FieldClassification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasCatalogTest {

    /** The pattern Apache Atlas validates classification names with. */
    private static final Pattern ATLAS_CLASSIFICATION_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9_ .]*");

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final AtlasCatalog catalog = AtlasCatalog.of(taxonomy);

    @Test
    @DisplayName("defines every super type before its sub-classifications, with names Atlas accepts")
    void creationOrder() {
        List<AtlasClassificationDef> defs = catalog.classificationDefs();
        Set<String> defined = new HashSet<>();
        for (AtlasClassificationDef def : defs) {
            assertTrue(ATLAS_CLASSIFICATION_NAME.matcher(def.name()).matches(), def::name);
            assertTrue(defined.containsAll(def.superTypes()), def::name);
            assertTrue(defined.add(def.name()), def::name);
        }
        assertEquals(6 + 3 + 4 + 3 + 4 + 1 + taxonomy.categories().size(), defs.size());
    }

    @Test
    @DisplayName("the category tree becomes classification inheritance")
    void inheritance() {
        assertAll(
                () -> assertEquals(List.of("DataCategory.user.contact"), def("DataCategory.user.contact.email")
                        .superTypes()),
                () -> assertEquals(List.of("DataCategory"), def("DataCategory.user").superTypes()),
                () -> assertEquals(List.of("Confidentiality"), def("Confidentiality.High").superTypes()),
                () -> assertTrue(def("DataCategory.user.contact.email").description()
                        .startsWith("User Contact Email: ")));
    }

    @Test
    @DisplayName("a category implies its legal nature, levels and secrecy as separate classifications")
    void impliedClassifications() {
        assertAll(
                () -> assertEquals(List.of("DataCategory.user.health_and_medical", "PersonalData.SpecialCategory",
                        "Confidentiality.High", "Integrity.High"),
                        catalog.classificationsOf(taxonomy.requireCategory(Categories.USER_HEALTH_AND_MEDICAL))),
                () -> assertEquals(List.of("DataCategory.system.authentication.secret", "Confidentiality.High",
                        "Integrity.High", "DataHandling.Secret"),
                        catalog.classificationsOf(taxonomy.requireCategory(Categories.SYSTEM_AUTHENTICATION_SECRET))));
    }

    @Test
    @DisplayName("a column gets its category, legal nature, levels and identifiability classifications")
    void columnClassifications() {
        @SuppressWarnings("unused")
        class Row {
            @Category(Categories.USER_CONTACT_EMAIL)
            private String email;
            @Category(value = Categories.USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED,
                    confidentiality = Confidentiality.MEDIUM)
            private String reference;
        }
        List<FieldClassification> fields = new Classifier(taxonomy).classify(Row.class).fields();
        assertEquals(List.of("DataCategory.user.contact.email", "PersonalData.Personal", "Confidentiality.Low",
                "Integrity.Medium", "Identifiability.Identified"), catalog.classificationsOf(fields.get(0)));
        assertEquals(List.of("DataCategory.user.unique_id.pseudonymous", "PersonalData.Personal",
                "Confidentiality.Medium", "Integrity.Medium", "Identifiability.Pseudonymized"),
                catalog.classificationsOf(fields.get(1)));
        assertEquals("[{\"typeName\":\"DataCategory.user.contact.email\"},{\"typeName\":\"PersonalData.Personal\"},"
                + "{\"typeName\":\"Confidentiality.Low\"},{\"typeName\":\"Integrity.Medium\"},"
                + "{\"typeName\":\"Identifiability.Identified\"}]", catalog.classificationsJson(fields.get(0)));
    }

    @Test
    @DisplayName("the mapping renames the roots and rejects names with dots")
    void mapping() {
        AtlasCatalog custom = AtlasCatalog.of(taxonomy, AtlasMapping.defaults().withCategories("Categoria"));
        assertEquals("Categoria.user.name", custom.classificationOf(taxonomy.requireCategory(Categories.USER_NAME)));
        assertThrows(IllegalArgumentException.class, () -> AtlasMapping.defaults().withPersonalData("a.b"));
        assertThrows(IllegalArgumentException.class, () -> AtlasMapping.defaults().withHandling("DataCategory"));
    }

    @Test
    @DisplayName("the typedefs body holds every classification definition")
    void json() {
        assertEquals("{\"name\":\"Confidentiality.High\",\"description\":\"Say \\\"hi\\\"\","
                + "\"superTypes\":[\"Confidentiality\"],\"attributeDefs\":[]}",
                new AtlasClassificationDef("Confidentiality.High", "Say \"hi\"", List.of("Confidentiality")).toJson());
        String typeDefs = catalog.typeDefsJson();
        assertTrue(typeDefs.startsWith("{\"classificationDefs\":[{\"name\":\"PersonalData\","));
        assertEquals(catalog.classificationDefs().size(), typeDefs.split("\"superTypes\"").length - 1);
    }

    private AtlasClassificationDef def(String name) {
        return catalog.classificationDefs().stream().filter(d -> d.name().equals(name)).findFirst().orElseThrow();
    }
}
