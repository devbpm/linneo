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
package io.github.devbpm.linneo.extension.supplychain;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataCatalog;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.processing.LegalBasis;
import io.github.devbpm.linneo.processing.ProcessingActivity;
import io.github.devbpm.linneo.processing.ProcessingChecker;
import io.github.devbpm.linneo.processing.ProcessingViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.devbpm.linneo.extension.ExtensionTerms.constants;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplyChainTaxonomyTest {

    private final Taxonomy supplyChain = Taxonomy.builder().withStandard().extend(new SupplyChainTaxonomy()).build();

    @Test
    @DisplayName("adds 16 categories, 12 data uses and 3 data subjects")
    void counts() {
        assertAll(
                () -> assertEquals(16, constants(SupplyChainCategories.class).size()),
                () -> assertEquals(12, constants(SupplyChainDataUses.class).size()),
                () -> assertEquals(3, constants(SupplyChainDataSubjects.class).size()));
    }

    @Test
    @DisplayName("trade secrets and bids are highly confidential; stock, quality and tachograph records keep integrity")
    void protection() {
        assertAll(
                () -> assertEquals(Set.of(SupplyChainCategories.ORG_BUSINESS_RECORDS_PRODUCT_COST,
                                SupplyChainCategories.ORG_BUSINESS_RECORDS_PRODUCT_FORMULA,
                                SupplyChainCategories.ORG_BUSINESS_RECORDS_PROCUREMENT_BID),
                        keys(c -> c.confidentiality() == Confidentiality.HIGH)),
                () -> assertEquals(Set.of(SupplyChainCategories.ORG_BUSINESS_RECORDS_INVENTORY,
                                SupplyChainCategories.ORG_BUSINESS_RECORDS_QUALITY,
                                SupplyChainCategories.USER_WORKPLACE_DRIVING_RECORD),
                        keys(c -> c.integrity() == Integrity.HIGH)),
                () -> assertEquals(Confidentiality.MEDIUM, supplyChain.requireCategory(
                        SupplyChainCategories.USER_CONTACT_DELIVERY).confidentiality()),
                () -> assertEquals(Confidentiality.LOW, supplyChain.requireCategory(Categories.USER_CONTACT)
                        .confidentiality()),
                () -> assertEquals(Confidentiality.MEDIUM, supplyChain.requireCategory(
                        SupplyChainCategories.USER_WORKPLACE_LABOR_TRACKING).confidentiality()));
    }

    @Test
    @DisplayName("refines products and procurement and places the data uses below operations and sales")
    void hierarchy() {
        assertAll(
                () -> assertEquals(SupplyChainCategories.ORG_BUSINESS_RECORDS_PRODUCT, supplyChain.requireCategory(
                        SupplyChainCategories.ORG_BUSINESS_RECORDS_PRODUCT_FORMULA).info().parentKey()),
                () -> assertEquals(SupplyChainCategories.ORG_BUSINESS_RECORDS_PROCUREMENT, supplyChain
                        .requireCategory(SupplyChainCategories.ORG_BUSINESS_RECORDS_PROCUREMENT_BID).info()
                        .parentKey()),
                () -> assertEquals(SupplyChainDataUses.OPERATIONS_LOGISTICS, supplyChain.requireDataUse(
                        SupplyChainDataUses.OPERATIONS_LOGISTICS_FLEET_MANAGEMENT).info().parentKey()),
                () -> assertEquals(DataUses.OPERATIONS, supplyChain.requireDataUse(
                        SupplyChainDataUses.OPERATIONS_ORDER_MANAGEMENT).info().parentKey()));
    }

    @Test
    @DisplayName("a delivery to a consumer is personal data, tagged as such in the catalogs")
    void integration() {
        @SuppressWarnings("unused")
        class Delivery {
            @Category(SupplyChainCategories.ORG_BUSINESS_RECORDS_SHIPMENT)
            private String shipmentNumber;
            @Category(SupplyChainCategories.USER_CONTACT_DELIVERY)
            private String address;
            @Category(SupplyChainCategories.USER_CONTACT_DELIVERY)
            private byte[] signature;
        }
        List<FieldClassification> fields = new Classifier(supplyChain).verify(List.of(Delivery.class)).get(0)
                .fields();
        List<ProcessingViolation> violations = new ProcessingChecker(supplyChain).check(ProcessingActivity
                .builder("delivery", "Home deliveries")
                .purposes(SupplyChainDataUses.OPERATIONS_LOGISTICS)
                .dataSubjects(SupplyChainDataSubjects.DELIVERY_RECIPIENT)
                .entities(Delivery.class)
                .legalBasis(LegalBasis.CONTRACT)
                .retention("Until the return period ends")
                .build()).violations();
        assertAll(
                () -> assertEquals(List.of(), violations),
                () -> assertTrue(fields.get(1).category().isPersonal()),
                () -> assertTrue(OpenMetadataCatalog.of(supplyChain).tagsOf(fields.get(1))
                        .contains("Confidentiality.Medium")));
    }

    private Set<String> keys(Predicate<DataCategory> filter) {
        return constants(SupplyChainCategories.class).stream().map(supplyChain::requireCategory).filter(filter)
                .map(DataCategory::key).collect(Collectors.toSet());
    }
}
