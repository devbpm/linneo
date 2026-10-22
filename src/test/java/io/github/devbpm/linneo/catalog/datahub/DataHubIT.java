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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.devbpm.linneo.catalog.ItHttp.difference;
import static io.github.devbpm.linneo.catalog.ItHttp.encode;
import static io.github.devbpm.linneo.catalog.ItHttp.strings;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Publishes the taxonomy to a running DataHub metadata service (GMS) and reads back what it stored. Skipped unless
 * the system property {@code linneo.it.datahub} gives the GMS URL. With the official quickstart compose file of a
 * DataHub release ({@code docker/quickstart/docker-compose.quickstart-profile.yml}):
 * <pre>
 * DATAHUB_VERSION=v1.7.0.1 docker compose -p linneo-dh --profile quickstart-backend up -d
 * ./mvnw test -Dtest=DataHubIT -Dsurefire.failIfNoSpecifiedTests=false -Dlinneo.it.datahub=http://localhost:8080
 * </pre>
 * The quickstart runs without authentication; for a server that requires it, pass a personal access token in
 * {@code linneo.it.datahub.token}.
 */
@EnabledIfSystemProperty(named = "linneo.it.datahub", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataHubIT {

    private static final Pattern GLOSSARY_TERM = Pattern.compile("urn:li:glossaryTerm:[^\"\\\\]+");
    private static final Taxonomy TAXONOMY = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();
    private static final DataHubCatalog CATALOG = DataHubCatalog.of(TAXONOMY);

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
        http = new ItHttp(System.getProperty("linneo.it.datahub")).header("X-RestLi-Protocol-Version", "2.0.0");
        String token = System.getProperty("linneo.it.datahub.token", "");
        if (!token.isBlank()) {
            http.header("Authorization", "Bearer " + token);
        }
    }

    @Test
    @Order(1)
    @DisplayName("stores every node and term with its definition and parent, and publishing twice is idempotent")
    void publishesGlossary() {
        List<DhProposal> proposals = CATALOG.proposals();
        for (int round = 0; round < 2; round++) {
            for (DhProposal proposal : proposals) {
                ingest(proposal);
            }
        }
        for (DhProposal proposal : proposals) {
            String stored = aspect(proposal.entityUrn(), proposal.aspectName());
            if (proposal.aspectName().equals("glossaryRelatedTerms")) {
                assertEquals(Map.of(), difference(urns(proposal.aspect()), urns(stored)), proposal.entityUrn());
                continue;
            }
            String definition = strings(proposal.aspect(), "definition").get(0);
            List<String> parents = strings(proposal.aspect(), "parentNode");
            assertAll(proposal.entityUrn(),
                    () -> assertTrue(stored.contains("\"definition\":\"" + definition + "\""), stored),
                    () -> assertTrue(parents.isEmpty() || stored.contains("\"parentNode\":\"" + parents.get(0) + "\""),
                            stored));
        }
    }

    @Test
    @Order(2)
    @DisplayName("every term a category is a kind of, or is related to, exists in the glossary")
    void relatedTermsExist() {
        Set<String> targets = new LinkedHashSet<>();
        for (DhProposal proposal : CATALOG.proposals()) {
            if (proposal.aspectName().equals("glossaryRelatedTerms")) {
                targets.addAll(urns(proposal.aspect()));
            }
        }
        for (var category : TAXONOMY.categories()) {
            category.info().parent().ifPresent(parent -> assertTrue(
                    targets.contains(CATALOG.termOf(TAXONOMY.requireCategory(parent))), category.key()));
        }
        for (String target : targets) {
            assertTrue(aspect(target, "glossaryTermInfo").contains("\"definition\""), target);
        }
    }

    @Test
    @Order(3)
    @DisplayName("each dataset field stores exactly the terms of termsOf(field)")
    void tagsFields() {
        List<FieldClassification> fields = new Classifier(TAXONOMY).classify(Student.class).fields();
        assertEquals(7, fields.size());
        for (FieldClassification field : fields) {
            List<String> expected = CATALOG.termsOf(field);
            String urn = "urn:li:dataset:(urn:li:dataPlatform:mysql,linneo_it.t_"
                    + field.field().toLowerCase(Locale.ROOT) + ",PROD)";
            ingest(new DhProposal("dataset", urn, "schemaMetadata", schema(field.field())));
            ingest(new DhProposal("dataset", urn, "editableSchemaMetadata", fieldTerms(field.field(), expected)));
            assertEquals(Map.of(), difference(expected, urns(aspect(urn, "editableSchemaMetadata"))), field.field());
        }
    }

    private static void ingest(DhProposal proposal) {
        http.send("POST", "/aspects?action=ingestProposal", proposal.toJson());
    }

    private static String aspect(String urn, String aspect) {
        return http.send("GET", "/aspects/" + encode(urn) + "?aspect=" + aspect + "&version=0", null);
    }

    // Every distinct glossary term URN in a JSON text, whatever member holds it.
    private static List<String> urns(String json) {
        Matcher matcher = GLOSSARY_TERM.matcher(json);
        Set<String> found = new LinkedHashSet<>();
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return List.copyOf(found);
    }

    private static String schema(String field) {
        String type = new Json().raw("type", "{\"com.linkedin.schema.StringType\":{}}").end();
        String column = new Json().field("fieldPath", field).field("nativeDataType", "VARCHAR(255)")
                .raw("type", type).end();
        return new Json().field("schemaName", field).field("platform", "urn:li:dataPlatform:mysql")
                .raw("version", "0").field("hash", "")
                .raw("platformSchema", "{\"com.linkedin.schema.MySqlDDL\":{\"tableSchema\":\"\"}}")
                .raw("fields", Json.array(List.of(column))).end();
    }

    private static String fieldTerms(String field, List<String> terms) {
        String audit = "{\"time\":0,\"actor\":\"urn:li:corpuser:datahub\"}";
        String glossaryTerms = new Json().raw("terms", Json.array(terms.stream()
                .map(t -> new Json().field("urn", t).end()).toList())).raw("auditStamp", audit).end();
        String info = new Json().field("fieldPath", field).raw("glossaryTerms", glossaryTerms).end();
        return new Json().raw("editableSchemaFieldInfo", Json.array(List.of(info))).end();
    }
}
