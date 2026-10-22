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
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.github.devbpm.linneo.catalog.ItHttp.difference;
import static io.github.devbpm.linneo.catalog.ItHttp.encode;
import static io.github.devbpm.linneo.catalog.ItHttp.string;
import static io.github.devbpm.linneo.catalog.ItHttp.strings;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Publishes the taxonomy to a running OpenMetadata server and reads back what the server stored. Skipped unless the
 * system property {@code linneo.it.openmetadata} gives the server URL. With the official {@code docker-compose.yml}
 * of an OpenMetadata release:
 * <pre>
 * docker compose -p linneo-om up -d openmetadata-server
 * ./mvnw test -Dtest=OpenMetadataIT -Dsurefire.failIfNoSpecifiedTests=false \
 *     -Dlinneo.it.openmetadata=http://localhost:8585
 * </pre>
 * The user and password default to the server's {@code admin@open-metadata.org} / {@code admin}
 * ({@code linneo.it.openmetadata.user}, {@code linneo.it.openmetadata.password}). The test leaves the
 * classifications in place, since publishing them is idempotent, and deletes the database service it creates
 * unless {@code linneo.it.keep} is {@code true}, which leaves the tagged tables to look at in the UI.
 */
@EnabledIfSystemProperty(named = "linneo.it.openmetadata", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OpenMetadataIT {

    private static final String SERVICE = "linneo_it";
    private static final String SCHEMA = SERVICE + ".linneo.public";

    private static final Taxonomy TAXONOMY = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();
    private static final OpenMetadataCatalog CATALOG = OpenMetadataCatalog.of(TAXONOMY);

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
    static void login() {
        http = new ItHttp(System.getProperty("linneo.it.openmetadata") + "/api/v1");
        String user = System.getProperty("linneo.it.openmetadata.user", "admin@open-metadata.org");
        String password = System.getProperty("linneo.it.openmetadata.password", "admin");
        String body = new Json().field("email", user)
                .field("password", Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8))).end();
        http.header("Authorization", "Bearer " + string(http.send("POST", "/users/login", body), "accessToken"));
    }

    @AfterAll
    static void deleteService() {
        if (Boolean.getBoolean("linneo.it.keep")) {
            return;
        }
        ItHttp.Response service = http.request("GET", "/services/databaseServices/name/" + SERVICE, null);
        if (service.status() == 200) {
            http.send("DELETE", "/services/databaseServices/" + string(service.body(), "id")
                    + "?hardDelete=true&recursive=true", null);
        }
    }

    @Test
    @Order(1)
    @DisplayName("stores every classification and tag, each tag under its parent, and publishing twice is idempotent")
    void publishesTaxonomy() {
        for (OmClassification classification : CATALOG.classifications()) {
            http.send("PUT", "/classifications", classification.toJson());
        }
        List<OmTag> tags = CATALOG.tags();
        for (int round = 0; round < 2; round++) {
            for (OmTag tag : tags) {
                http.send("PUT", "/tags", tag.toJson());
            }
        }
        for (OmTag tag : tags) {
            String stored = http.send("GET", "/tags/name/" + encode(tag.fullyQualifiedName()), null);
            List<String> names = strings(stored, "fullyQualifiedName");
            assertAll(tag.fullyQualifiedName(),
                    () -> assertTrue(names.contains(tag.fullyQualifiedName()), stored),
                    () -> assertTrue(stored.contains("\"displayName\":\"" + tag.displayName() + "\""), stored),
                    () -> assertTrue(tag.parent() == null || names.contains(tag.parent()), stored));
        }
        assertEquals(TAXONOMY.categories().size(),
                tags.stream().filter(t -> t.classification().equals("DataCategory")).count());
    }

    @Test
    @Order(2)
    @DisplayName("each column stores exactly the tags of tagsOf(field), within the mutually exclusive classifications")
    void tagsColumns() {
        http.send("PUT", "/services/databaseServices", "{\"name\":\"" + SERVICE + "\",\"serviceType\":"
                + "\"CustomDatabase\",\"connection\":{\"config\":{\"type\":\"CustomDatabase\",\"sourcePythonClass\":"
                + "\"linneo.It\"}}}");
        http.send("PUT", "/databases", new Json().field("name", "linneo").field("service", SERVICE).end());
        http.send("PUT", "/databaseSchemas", new Json().field("name", "public")
                .field("database", SERVICE + ".linneo").end());
        List<FieldClassification> fields = new Classifier(TAXONOMY).classify(Student.class).fields();
        assertEquals(7, fields.size());
        for (FieldClassification field : fields) {
            List<String> expected = CATALOG.tagsOf(field);
            String table = "t_" + field.field().toLowerCase(Locale.ROOT);
            http.send("PUT", "/tables", tableBody(table, field.field(), expected));
            String stored = http.send("GET", "/tables/name/" + encode(SCHEMA + "." + table) + "?fields=columns,tags",
                    null);
            assertEquals(Map.of(), difference(expected, strings(stored, "tagFQN")), field.field());
        }
    }

    @Test
    @Order(3)
    @DisplayName("the server refuses two tags of one mutually exclusive classification on a column")
    void mutualExclusion() {
        ItHttp.Response response = http.request("PUT", "/tables", tableBody("t_conflict", "x",
                List.of("Confidentiality.Low", "Confidentiality.High")));
        assertEquals(400, response.status(), response.body());
    }

    private static String tableBody(String table, String column, List<String> tags) {
        List<String> labels = tags.stream().map(tag -> new Json().field("tagFQN", tag).field("source", "Classification")
                .field("labelType", "Manual").field("state", "Confirmed").end()).toList();
        String columnJson = new Json().field("name", column).field("dataType", "VARCHAR").raw("dataLength", "255")
                .raw("tags", Json.array(labels)).end();
        return new Json().field("name", table).field("databaseSchema", SCHEMA)
                .raw("columns", Json.array(List.of(columnJson))).end();
    }
}
