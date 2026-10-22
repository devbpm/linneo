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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenMetadataCatalogTest {

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final OpenMetadataCatalog catalog = OpenMetadataCatalog.of(taxonomy);

    @Test
    @DisplayName("creates one nested tag per category, every parent before its children")
    void nestedTags() {
        List<OmTag> tags = catalog.tags();
        Set<String> created = new HashSet<>();
        for (OmTag tag : tags) {
            assertTrue(tag.parent() == null || created.contains(tag.parent()), tag::fullyQualifiedName);
            created.add(tag.fullyQualifiedName());
        }
        assertEquals(4 + 3 + 4 + 1 + taxonomy.categories().size(), tags.size());
        assertTrue(created.contains("DataCategory.user.contact.email"));
    }

    @Test
    @DisplayName("the tags of a category are its own tag plus the protection it implies")
    void impliedTags() {
        OmTag health = catalog.tags().stream()
                .filter(t -> t.fullyQualifiedName().equals("DataCategory.user.health_and_medical"))
                .findFirst().orElseThrow();
        assertAll(
                () -> assertEquals("DataCategory.user", health.parent()),
                () -> assertEquals(List.of("DataCategory.user.health_and_medical", "PersonalData.SpecialCategory",
                        "PII.Sensitive", "Confidentiality.High", "Integrity.High"),
                        catalog.tagsOf(taxonomy.requireCategory(Categories.USER_HEALTH_AND_MEDICAL))),
                () -> assertEquals(List.of("DataCategory.system.authentication.secret", "PII.None",
                        "Confidentiality.High", "Integrity.High", "DataHandling.Secret"),
                        catalog.tagsOf(taxonomy.requireCategory(Categories.SYSTEM_AUTHENTICATION_SECRET))));
    }

    @Test
    @DisplayName("a column gets its category, personal data, PII, levels and identifiability tags")
    void columnTags() {
        @SuppressWarnings("unused")
        class Row {
            @Category(Categories.USER_CONTACT_EMAIL)
            private String email;
            @Category(value = Categories.USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED,
                    confidentiality = Confidentiality.MEDIUM)
            private String reference;
        }
        List<FieldClassification> fields = new Classifier(taxonomy).classify(Row.class).fields();
        assertEquals(List.of("DataCategory.user.contact.email", "PersonalData.Personal", "PII.NonSensitive",
                "Confidentiality.Low", "Integrity.Medium", "Identifiability.Identified"), catalog.tagsOf(fields.get(0)));
        assertEquals(List.of("DataCategory.user.unique_id.pseudonymous", "PersonalData.Personal", "PII.Sensitive",
                "Confidentiality.Medium", "Integrity.Medium", "Identifiability.Pseudonymized"),
                catalog.tagsOf(fields.get(1)));
    }

    @Test
    @DisplayName("creates the five Linneo classifications; only DataHandling allows several tags per column")
    void classifications() {
        List<OmClassification> classifications = catalog.classifications();
        assertAll(
                () -> assertEquals(List.of("DataCategory", "Confidentiality", "Integrity", "Identifiability",
                        "DataHandling"), classifications.stream().map(OmClassification::name).toList()),
                () -> assertEquals(List.of(true, true, true, true, false),
                        classifications.stream().map(OmClassification::mutuallyExclusive).toList()),
                () -> assertTrue(classifications.stream().noneMatch(c -> c.name().equals("PII")
                        || c.name().equals("PersonalData"))));
    }

    @Test
    @DisplayName("a non-personal secret column gets PII.None and DataHandling.Secret, without identifiability")
    void secretColumn() {
        @SuppressWarnings("unused")
        class Credentials {
            @Category(Categories.SYSTEM_AUTHENTICATION_SECRET)
            private String apiKey;
        }
        FieldClassification field = new Classifier(taxonomy).classify(Credentials.class).fields().get(0);
        assertEquals(List.of("DataCategory.system.authentication.secret", "PII.None", "Confidentiality.High",
                "Integrity.High", "DataHandling.Secret"), catalog.tagsOf(field));
    }

    @Test
    @DisplayName("the tag of a deprecated category says so in its description, with the replacement")
    void deprecatedTag() {
        Taxonomy withDeprecated = Taxonomy.builder().withStandard()
                .category("user.contact.pager", "Pager", "Pager number.", c -> c.deprecated("0.2.0",
                        Categories.USER_CONTACT_PHONE_NUMBER))
                .category("user.contact.telex", "Telex", "Telex number.", c -> c.deprecated("0.2.0", null))
                .build();
        List<OmTag> tags = OpenMetadataCatalog.of(withDeprecated).tags();
        assertAll(
                () -> assertTrue(tags.stream().anyMatch(t -> t.fullyQualifiedName().equals("DataCategory.user.contact.pager")
                        && t.description().equals("Pager number. Deprecated: use user.contact.phone_number."))),
                () -> assertTrue(tags.stream().anyMatch(t -> t.fullyQualifiedName().equals("DataCategory.user.contact.telex")
                        && t.description().equals("Telex number. Deprecated."))));
    }

    @Test
    @DisplayName("the mapping renames classifications and moves the sensitivity threshold")
    void mapping() {
        OpenMetadataCatalog custom = OpenMetadataCatalog.of(taxonomy, OpenMetadataMapping.defaults()
                .withCategories("Categoria").withConfidentiality("Confidencialidad")
                .withSensitiveFrom(Confidentiality.LOW));
        assertEquals(List.of("Categoria.user.name", "PersonalData.Personal", "PII.Sensitive", "Confidencialidad.Low",
                "Integrity.Medium"), custom.tagsOf(taxonomy.requireCategory(Categories.USER_NAME)));
        assertThrows(IllegalArgumentException.class, () -> OpenMetadataMapping.defaults().withCategories("a.b"));
        assertThrows(IllegalArgumentException.class, () -> OpenMetadataMapping.defaults().withCategories("PII"));
    }

    @Test
    @DisplayName("request bodies are valid, escaped JSON")
    void json() {
        OmTag tag = new OmTag("DataCategory", "DataCategory.user", "x", "Say \"hi\"", "line\nbreak\\");
        assertEquals("{\"classification\":\"DataCategory\",\"parent\":\"DataCategory.user\",\"name\":\"x\","
                + "\"displayName\":\"Say \\\"hi\\\"\",\"description\":\"line\\nbreak\\\\\"}", tag.toJson());
        assertEquals("{\"name\":\"Integrity\",\"description\":\"d\",\"mutuallyExclusive\":true}",
                new OmClassification("Integrity", "d", true).toJson());
    }
}
