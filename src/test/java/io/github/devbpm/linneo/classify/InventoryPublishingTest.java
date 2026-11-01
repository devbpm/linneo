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
package io.github.devbpm.linneo.classify;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.CategoryByRow;
import io.github.devbpm.linneo.annotation.Composite;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.annotation.DerivedFrom;
import io.github.devbpm.linneo.annotation.Exempt;
import io.github.devbpm.linneo.catalog.atlas.AtlasCatalog;
import io.github.devbpm.linneo.catalog.datahub.DataHubCatalog;
import io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataCatalog;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A stored field of a data inventory publishes to every catalog exactly like the classification of the same field,
 * so an application can keep catalogs in step from the inventory alone.
 */
class InventoryPublishingTest {

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final OpenMetadataCatalog openMetadata = OpenMetadataCatalog.of(taxonomy);
    private final DataHubCatalog dataHub = DataHubCatalog.of(taxonomy);
    private final AtlasCatalog atlas = AtlasCatalog.of(taxonomy);

    @SuppressWarnings("unused")
    @Composite
    static class Address {
        @Category(Categories.USER_CONTACT_ADDRESS_STREET)
        private String street;
        @Category(Categories.USER_CONTACT_ADDRESS_CITY)
        private String city;
    }

    @SuppressWarnings("unused")
    @DefaultCategory(value = Categories.SYSTEM_OPERATIONS, reason = "technical")
    static class Person {
        @Category(value = Categories.USER_CONTACT_EMAIL, confidentiality = Confidentiality.HIGH)
        private String email;
        @Category(value = Categories.USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
        private String analyticsId;
        @Category(value = Categories.USER_HEALTH_AND_MEDICAL, identifiability = Identifiability.AGGREGATED)
        private Double sickDays;
        @Category(Categories.USER_HEALTH_AND_MEDICAL_DISABILITY)
        private Integer disability;
        @Category(Categories.USER_CRIMINAL_HISTORY)
        private Boolean certificateClear;
        @Category(Categories.USER_AUTHORIZATION_PASSWORD)
        private String passwordHash;
        @CategoryByRow(declaredBy = "the document type", ceiling = Categories.USER_HEALTH_AND_MEDICAL)
        private byte[] content;
        @DerivedFrom("email")
        private String emailDomain;
        private Address home;
        private long version;
        @Exempt("cache of the rendered card")
        private String card;
    }

    @Test
    @DisplayName("every stored field gets the tags, terms and classifications of its classification")
    void sameAsClassification() {
        Classifier classifier = new Classifier(taxonomy);
        ClassClassification classified = classifier.classify(Person.class);
        DataInventory inventory = classifier.inventory(List.of(Person.class), type -> "person");
        assertEquals(List.of(), classified.errors());
        assertEquals(11, classified.fields().size(), "every way of classifying a field is compared");
        List<Executable> checks = new ArrayList<>();
        for (FieldClassification field : classified.fields()) {
            InventoryField stored = inventory.field("person", field.path()).orElseThrow();
            checks.add(() -> assertEquals(openMetadata.tagsOf(field), openMetadata.tagsOf(stored), field.path()));
            checks.add(() -> assertEquals(dataHub.termsOf(field), dataHub.termsOf(stored), field.path()));
            checks.add(() -> assertEquals(atlas.classificationsOf(field), atlas.classificationsOf(stored),
                    field.path()));
            checks.add(() -> assertEquals(atlas.classificationsJson(field), atlas.classificationsJson(stored),
                    field.path()));
            checks.add(() -> assertEquals(field.identifiability(), stored.identifiability(taxonomy), field.path()));
        }
        assertAll(checks);
    }

    @Test
    @DisplayName("an exempted field carries nothing, and a deprecated key publishes as its replacement")
    void exemptAndDeprecated() {
        InventoryField card = InventoryField.exempt("person", "card", "cache");
        Taxonomy renamed = Taxonomy.builder().withStandard().category("user.contact.old_email", "Old email", "",
                c -> c.deprecated("1.0.0", Categories.USER_CONTACT_EMAIL)).build();
        InventoryField old = new InventoryField("person", "email", "user.contact.old_email", Basis.DECLARED, null,
                null, null, null);
        InventoryField current = new InventoryField("person", "email", Categories.USER_CONTACT_EMAIL,
                Basis.DECLARED, null, null, null, null);
        assertAll(
                () -> assertEquals(List.of(), openMetadata.tagsOf(card)),
                () -> assertEquals(List.of(), dataHub.termsOf(card)),
                () -> assertEquals(List.of(), atlas.classificationsOf(card)),
                () -> assertEquals("[]", atlas.classificationsJson(card)),
                () -> assertEquals(OpenMetadataCatalog.of(renamed).tagsOf(current),
                        OpenMetadataCatalog.of(renamed).tagsOf(old)),
                () -> assertEquals(Identifiability.IDENTIFIED, current.identifiability(taxonomy)),
                () -> assertNull(card.identifiability(taxonomy)));
    }

    @Test
    @DisplayName("the tags of a column before and after a change tell what to remove and what to add")
    void columnDelta() {
        InventoryField before = new InventoryField("person", "notes", Categories.USER_CONTENT_PRIVATE,
                Basis.DECLARED, null, null, null, null);
        InventoryField after = new InventoryField("person", "notes", Categories.USER_HEALTH_AND_MEDICAL,
                Basis.DECLARED, null, null, null, null);
        List<String> removed = new ArrayList<>(openMetadata.tagsOf(before));
        removed.removeAll(openMetadata.tagsOf(after));
        List<String> added = new ArrayList<>(openMetadata.tagsOf(after));
        added.removeAll(openMetadata.tagsOf(before));
        assertAll(
                () -> assertTrue(removed.contains("DataCategory." + Categories.USER_CONTENT_PRIVATE), removed::toString),
                () -> assertTrue(added.contains("DataCategory." + Categories.USER_HEALTH_AND_MEDICAL), added::toString),
                () -> assertTrue(added.contains("PersonalData.SpecialCategory"), added::toString),
                () -> assertTrue(removed.contains("PersonalData.Personal"), removed::toString));
    }
}
