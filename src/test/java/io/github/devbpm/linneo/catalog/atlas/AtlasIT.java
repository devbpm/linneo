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
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.catalog.ItHttp;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.extension.university.UniversityCategories;
import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;
import io.github.devbpm.linneo.internal.Json;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static io.github.devbpm.linneo.catalog.ItHttp.difference;
import static io.github.devbpm.linneo.catalog.ItHttp.encode;
import static io.github.devbpm.linneo.catalog.ItHttp.string;
import static io.github.devbpm.linneo.catalog.ItHttp.strings;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Publishes the taxonomy to a running Apache Atlas server and reads back what it stored. Skipped unless the system
 * property {@code linneo.it.atlas} gives the server URL, for example with a standalone Atlas image:
 * <pre>
 * docker run -d --name linneo-atlas -p 21000:21000 sburn/apache-atlas:2.3.0
 * ./mvnw test -Dtest=AtlasIT -Dsurefire.failIfNoSpecifiedTests=false -Dlinneo.it.atlas=http://localhost:21000
 * </pre>
 * The user and password default to {@code admin} / {@code admin} ({@code linneo.it.atlas.user},
 * {@code linneo.it.atlas.password}). The test defines an entity type of its own to classify, and deletes the
 * entities it creates unless {@code linneo.it.keep} is {@code true}.
 */
@EnabledIfSystemProperty(named = "linneo.it.atlas", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AtlasIT {

    private static final String COLUMN_TYPE = "linneo_it_column";

    private static final Taxonomy TAXONOMY = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();
    private static final AtlasCatalog CATALOG = AtlasCatalog.of(TAXONOMY);

    private static final List<String> CREATED = new ArrayList<>();
    private static ItHttp http;

    @SuppressWarnings("unused")
    static class Student {
        @Category(Categories.USER_CONTACT_EMAIL)
        private String email;
        @Category(Categories.USER_HEALTH_AND_MEDICAL_DISABILITY)
        private Integer disabilityDegree;
        @Category(value = Categories.USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
        private String analyticsId;
        @Category(Categories.SYSTEM_AUTHENTICATION_SECRET)
        private String apiKey;
        @Category(UniversityCategories.USER_EDUCATION_ACCOMMODATION)
        private String examArrangement;
        @Category(UniversityCategories.USER_EDUCATION_GRADE_FINAL)
        private Double finalGrade;
        @Category(Categories.USER_CRIMINAL_HISTORY)
        private String criminalRecordCertificate;
    }

    @BeforeAll
    static void connect() {
        String user = System.getProperty("linneo.it.atlas.user", "admin");
        String password = System.getProperty("linneo.it.atlas.password", "admin");
        http = new ItHttp(System.getProperty("linneo.it.atlas") + "/api/atlas/v2").header("Authorization", "Basic "
                + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }

    @AfterAll
    static void deleteEntities() {
        if (Boolean.getBoolean("linneo.it.keep")) {
            return;
        }
        for (String guid : CREATED) {
            http.request("DELETE", "/entity/guid/" + guid, null);
        }
    }

    @Test
    @Order(1)
    @DisplayName("creates every classification type with its super type; publishing again updates them in place")
    void publishesTypes() {
        String typeDefs = CATALOG.typeDefsJson();
        ItHttp.Response created = http.request("POST", "/types/typedefs", typeDefs);
        assertTrue(created.status() == 200 || created.status() == 409, () -> created.status() + ": " + created.body());
        http.send("PUT", "/types/typedefs", typeDefs);
        for (AtlasClassificationDef def : CATALOG.classificationDefs()) {
            String stored = http.send("GET", "/types/classificationdef/name/" + encode(def.name()), null);
            assertAll(def.name(),
                    () -> assertEquals(def.name(), string(stored, "name")),
                    () -> assertEquals(def.superTypes(), quoted(superTypes(stored))));
        }
    }

    @Test
    @Order(2)
    @DisplayName("each entity stores exactly the classifications of classificationsOf(field)")
    void classifiesEntities() {
        String entityDef = "{\"entityDefs\":[{\"name\":\"" + COLUMN_TYPE + "\",\"superTypes\":[\"DataSet\"],"
                + "\"attributeDefs\":[]}]}";
        if (http.request("GET", "/types/entitydef/name/" + COLUMN_TYPE, null).status() != 200) {
            http.send("POST", "/types/typedefs", entityDef);
        }
        List<FieldClassification> fields = new Classifier(TAXONOMY).classify(Student.class).fields();
        assertEquals(7, fields.size());
        for (FieldClassification field : fields) {
            String attributes = new Json().field("qualifiedName", "linneo_it.student." + field.field() + "@it")
                    .field("name", field.field()).end();
            String entity = new Json().field("typeName", COLUMN_TYPE).raw("attributes", attributes).end();
            String response = http.send("POST", "/entity", new Json().raw("entity", entity).end());
            String guid = string(response, "guid");
            CREATED.add(guid);
            http.send("POST", "/entity/guid/" + guid + "/classifications", CATALOG.classificationsJson(field));
            String stored = http.send("GET", "/entity/guid/" + guid + "/classifications", null);
            assertEquals(Map.of(), difference(CATALOG.classificationsOf(field), strings(stored, "typeName")),
                    field.field());
        }
    }

    // The JSON array of the superTypes member, as text.
    private static String superTypes(String json) {
        int start = json.indexOf("\"superTypes\":[");
        return start < 0 ? "[]" : json.substring(start + 13, json.indexOf(']', start) + 1);
    }

    private static List<String> quoted(String array) {
        List<String> values = new ArrayList<>();
        for (String part : array.substring(1, array.length() - 1).split(",")) {
            if (!part.isBlank()) {
                values.add(part.strip().replace("\"", ""));
            }
        }
        return values;
    }
}
