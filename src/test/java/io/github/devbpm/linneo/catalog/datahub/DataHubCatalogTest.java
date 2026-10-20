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
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.FieldClassification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataHubCatalogTest {

    private static final Pattern PARENT_NODE = Pattern.compile("\"parentNode\":\"([^\"]+)\"");

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final DataHubCatalog catalog = DataHubCatalog.of(taxonomy);

    @Test
    @DisplayName("creates every node before the nodes and terms it holds, and every term before its relations")
    void creationOrder() {
        List<DhProposal> proposals = catalog.proposals();
        Set<String> nodes = new HashSet<>();
        Set<String> terms = new HashSet<>();
        int related = 0;
        for (DhProposal proposal : proposals) {
            Matcher parent = PARENT_NODE.matcher(proposal.aspect());
            if (parent.find()) {
                assertTrue(nodes.contains(parent.group(1)), proposal::entityUrn);
            }
            switch (proposal.aspectName()) {
                case "glossaryNodeInfo" -> nodes.add(proposal.entityUrn());
                case "glossaryTermInfo" -> terms.add(proposal.entityUrn());
                default -> {
                    assertEquals("glossaryRelatedTerms", proposal.aspectName());
                    assertTrue(terms.contains(proposal.entityUrn()), proposal::entityUrn);
                    related++;
                }
            }
        }
        long withChildren = taxonomy.categories().stream()
                .filter(c -> !taxonomy.childrenOf(c).isEmpty()).count();
        int categories = taxonomy.categories().size();
        assertAll(
                () -> assertEquals(6 + withChildren, nodes.size()),
                () -> assertEquals(3 + 4 + 3 + 4 + 1 + categories, terms.size()),
                () -> assertEquals(categories, terms.size() - 15),
                () -> assertTrue(nodes.contains("urn:li:glossaryNode:DataCategory.user.contact")),
                () -> assertTrue(terms.contains("urn:li:glossaryTerm:DataCategory.user.contact.email")));
        assertEquals(categories, related);
    }

    @Test
    @DisplayName("a category with children holds its own term; a leaf sits in its parent's node")
    void termPlacement() {
        assertAll(
                () -> assertEquals("urn:li:glossaryNode:DataCategory.user.health_and_medical",
                        parentNode("urn:li:glossaryTerm:DataCategory.user.health_and_medical")),
                () -> assertEquals("urn:li:glossaryNode:DataCategory.user.contact",
                        parentNode("urn:li:glossaryTerm:DataCategory.user.contact.email")),
                () -> assertEquals("urn:li:glossaryNode:DataCategory",
                        parentNode("urn:li:glossaryNode:DataCategory.user")));
    }

    @Test
    @DisplayName("a category term is a term of its parent and is related to the protection it implies")
    void relatedTerms() {
        String health = aspect("urn:li:glossaryTerm:DataCategory.user.health_and_medical", "glossaryRelatedTerms");
        assertEquals("{\"isRelatedTerms\":[\"urn:li:glossaryTerm:DataCategory.user\"],\"relatedTerms\":["
                + "\"urn:li:glossaryTerm:PersonalData.SpecialCategory\",\"urn:li:glossaryTerm:Confidentiality.High\","
                + "\"urn:li:glossaryTerm:Integrity.High\"]}", health);
        DataCategory secret = taxonomy.requireCategory(Categories.SYSTEM_AUTHENTICATION_SECRET);
        assertEquals(List.of("urn:li:glossaryTerm:DataCategory.system.authentication.secret",
                "urn:li:glossaryTerm:Confidentiality.High", "urn:li:glossaryTerm:Integrity.High",
                "urn:li:glossaryTerm:DataHandling.Secret"), catalog.termsOf(secret));
    }

    @Test
    @DisplayName("a term records its key, origin and protection, and fideslang terms point to their source")
    void termInfo() {
        String email = aspect("urn:li:glossaryTerm:DataCategory.user.contact.email", "glossaryTermInfo");
        String grade = aspect("urn:li:glossaryTerm:DataCategory.user.education.grade", "glossaryTermInfo");
        assertAll(
                () -> assertTrue(email.contains("\"linneo.key\":\"user.contact.email\"")),
                () -> assertTrue(email.contains("\"linneo.confidentiality\":\"LOW\"")),
                () -> assertTrue(email.contains("\"termSource\":\"EXTERNAL\",\"sourceRef\":\"fideslang\"")),
                () -> assertTrue(grade.contains("\"linneo.origin\":\"linneo\"")),
                () -> assertTrue(grade.endsWith("\"termSource\":\"INTERNAL\"}")));
    }

    @Test
    @DisplayName("attributes follow the computed properties in key order and cannot override them")
    void attributes() {
        Taxonomy extended = Taxonomy.builder().withStandard()
                .category("user.contact.alias", "Alias", "Another name.", c -> c.attribute("zeta", "1")
                        .attribute("alpha", "2").attribute("linneo.secret", "true"))
                .build();
        String info = DataHubCatalog.of(extended).proposals().stream()
                .filter(p -> p.entityUrn().equals("urn:li:glossaryTerm:DataCategory.user.contact.alias")
                        && p.aspectName().equals("glossaryTermInfo"))
                .findFirst().orElseThrow().aspect();
        assertTrue(info.contains("\"linneo.secret\":\"false\",\"alpha\":\"2\",\"zeta\":\"1\"}"), info);
    }

    @Test
    @DisplayName("a field gets its category, legal nature, levels and identifiability terms")
    void fieldTerms() {
        @SuppressWarnings("unused")
        class Row {
            @Category(Categories.USER_CONTACT_EMAIL)
            private String email;
            @Category(value = Categories.USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED,
                    confidentiality = Confidentiality.MEDIUM)
            private String reference;
            @Category(Categories.USER_CRIMINAL_HISTORY)
            private String record;
        }
        List<FieldClassification> fields = new Classifier(taxonomy).classify(Row.class).fields();
        assertAll(
                () -> assertEquals(List.of("urn:li:glossaryTerm:DataCategory.user.contact.email",
                        "urn:li:glossaryTerm:PersonalData.Personal", "urn:li:glossaryTerm:Confidentiality.Low",
                        "urn:li:glossaryTerm:Integrity.Medium", "urn:li:glossaryTerm:Identifiability.Identified"),
                        catalog.termsOf(fields.get(0))),
                () -> assertEquals(List.of("urn:li:glossaryTerm:DataCategory.user.unique_id.pseudonymous",
                        "urn:li:glossaryTerm:PersonalData.Personal", "urn:li:glossaryTerm:Confidentiality.Medium",
                        "urn:li:glossaryTerm:Integrity.Medium", "urn:li:glossaryTerm:Identifiability.Pseudonymized"),
                        catalog.termsOf(fields.get(1))),
                () -> assertEquals("urn:li:glossaryTerm:PersonalData.CriminalOffence",
                        catalog.termsOf(fields.get(2)).get(1)));
    }

    @Test
    @DisplayName("the mapping renames the nodes and rejects names that would break a URN")
    void mapping() {
        DataHubCatalog custom = DataHubCatalog.of(taxonomy, DataHubMapping.defaults()
                .withCategories("Categoria").withConfidentiality("Confidencialidad"));
        assertEquals(List.of("urn:li:glossaryTerm:Categoria.user.name", "urn:li:glossaryTerm:PersonalData.Personal",
                "urn:li:glossaryTerm:Confidencialidad.Low", "urn:li:glossaryTerm:Integrity.Medium"),
                custom.termsOf(taxonomy.requireCategory(Categories.USER_NAME)));
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> DataHubMapping.defaults().withCategories("a.b")),
                () -> assertThrows(IllegalArgumentException.class, () -> DataHubMapping.defaults().withHandling("a,b")),
                () -> assertThrows(IllegalArgumentException.class, () -> DataHubMapping.defaults().withIntegrity("")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> DataHubMapping.defaults().withIntegrity("Confidentiality")));
    }

    @Test
    @DisplayName("a proposal is an ingestProposal body with the aspect JSON-encoded")
    void json() {
        DhProposal proposal = new DhProposal("glossaryTerm", "urn:li:glossaryTerm:X", "glossaryTermInfo",
                "{\"definition\":\"Say \\\"hi\\\"\"}");
        assertEquals("{\"proposal\":{\"entityType\":\"glossaryTerm\",\"entityUrn\":\"urn:li:glossaryTerm:X\","
                + "\"changeType\":\"UPSERT\",\"aspectName\":\"glossaryTermInfo\",\"aspect\":{\"value\":"
                + "\"{\\\"definition\\\":\\\"Say \\\\\\\"hi\\\\\\\"\\\"}\",\"contentType\":\"application/json\"}}}",
                proposal.toJson());
    }

    private String aspect(String urn, String aspectName) {
        return catalog.proposals().stream()
                .filter(p -> p.entityUrn().equals(urn) && p.aspectName().equals(aspectName))
                .findFirst().orElseThrow().aspect();
    }

    private String parentNode(String urn) {
        String aspectName = urn.startsWith("urn:li:glossaryNode:") ? "glossaryNodeInfo" : "glossaryTermInfo";
        Matcher matcher = PARENT_NODE.matcher(aspect(urn, aspectName));
        assertTrue(matcher.find(), urn);
        return matcher.group(1);
    }
}
