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

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyExtension;

import static io.github.devbpm.linneo.extension.supplychain.SupplyChainCategories.*;
import static io.github.devbpm.linneo.extension.supplychain.SupplyChainDataSubjects.*;
import static io.github.devbpm.linneo.extension.supplychain.SupplyChainDataUses.*;

/**
 * Terms for supply chain management: products and their formulas and costs, procurement, inventory, production,
 * quality, planning, sales orders, transport and after-sales service, the personal data these processes touch, the
 * purposes they serve and the people involved.
 * <p>
 * The terms are informed by the supply chain business areas of the Common Data Model (CDM), copyright Microsoft
 * Corporation, whose documentation and other content are licensed under the CC BY 4.0 licence; no CDM material is
 * reproduced. Most supply chain data is about organizations, products and goods and extends
 * {@code org.business_records}; warehouses and sites are the standard {@code org.location}, and suppliers and
 * customers that are organizations the standard {@code org.legal_entity}. Personal data appears where a process
 * touches people: a consumer's orders are the standard {@code user.behavior.purchase_history}, a delivery to a person
 * is {@linkplain SupplyChainCategories#USER_CONTACT_DELIVERY delivery details}, and the workers who drive, pick and
 * produce leave a {@linkplain SupplyChainCategories#USER_WORKPLACE_DRIVING_RECORD driving record} and
 * {@linkplain SupplyChainCategories#USER_WORKPLACE_LABOR_TRACKING labour tracking} data.
 * <p>
 * Every term extends an existing class: the data uses sit below the standard {@code operations} and {@code sales}
 * uses, and the data subjects are flat as in fideslang. No term carries version information, which fideslang
 * reserves for its default taxonomy.
 * <p>
 * Protection follows the standard policy and only raises it:
 * <ul>
 *   <li>formulas, product costs and supplier bids have high confidentiality: formulas and costs are often trade
 *       secrets, and bids must stay confidential until the award;</li>
 *   <li>inventory and quality records have high integrity, since stock values feed the books and quality and batch
 *       records support recalls and product safety obligations;</li>
 *   <li>a driving record has medium confidentiality and high integrity: tachograph records must be kept intact, and
 *       the geolocation of workers is restricted by LOPDGDD article 90;</li>
 *   <li>delivery details and labour tracking have medium confidentiality, since they reveal where a person is and how
 *       a worker performs (LOPDGDD articles 87 to 91 limit the monitoring of workers);</li>
 *   <li>the other categories inherit the medium confidentiality of {@code org.business_records}.</li>
 * </ul>
 * Data that reveals more than its category belongs in a stricter key: an order or delivery of medicines or medical
 * devices for a person in {@code user.health_and_medical}, and a driver's medical fitness in
 * {@code user.health_and_medical}.
 * <p>
 * Like the other extensions shipped with Linneo, it is not registered with {@link java.util.ServiceLoader}, so
 * {@link io.github.devbpm.linneo.Taxonomy#load()} does not apply it. Apply it explicitly, alone or with others:
 * <pre>{@code
 * Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new SupplyChainTaxonomy()).build();
 * }</pre>
 * or register it as a service of the application, as
 * {@link io.github.devbpm.linneo.extension.university.UniversityTaxonomy} explains. The keys are in
 * {@link SupplyChainCategories}, {@link SupplyChainDataUses} and {@link SupplyChainDataSubjects}.
 *
 * @since 0.1.0
 */
public final class SupplyChainTaxonomy implements TaxonomyExtension {

    /**
     * The extension's name, recorded as the origin of its terms.
     */
    @SuppressWarnings("java:S1845") // NAME is the value name() returns; the shared name is intended
    public static final String NAME = "supply_chain";

    /**
     * Creates the extension; it holds no state.
     */
    public SupplyChainTaxonomy() {
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void contribute(TaxonomyBuilder builder) {
        categories(builder);
        dataUses(builder);
        dataSubjects(builder);
    }

    private static void categories(TaxonomyBuilder builder) {
        builder.category(ORG_BUSINESS_RECORDS_INVENTORY, "Inventory",
                        "Stock on hand, warehouses and locations, stock movements, counts, batches and serial"
                                + " numbers.",
                        c -> c.integrity(Integrity.HIGH))
                .category(ORG_BUSINESS_RECORDS_PLANNING, "Supply Planning",
                        "Demand forecasts, master plans, planned orders and capacity plans.")
                .category(ORG_BUSINESS_RECORDS_PRICE_AGREEMENT, "Price Agreement",
                        "Price lists, trade agreements, discounts and rebates agreed with customers and suppliers.")
                .category(ORG_BUSINESS_RECORDS_PROCUREMENT, "Procurement",
                        "Purchase requisitions, requests for quotation, purchase orders, receipts and supplier"
                                + " agreements.")
                .category(ORG_BUSINESS_RECORDS_PROCUREMENT_BID, "Supplier Bid",
                        "Quotations and bids received from suppliers, and their evaluation, before the award.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(ORG_BUSINESS_RECORDS_PRODUCT, "Product",
                        "Products and their variants, specifications, units, classifications and attributes.")
                .category(ORG_BUSINESS_RECORDS_PRODUCT_COST, "Product Cost",
                        "Standard costs, cost roll-ups and margins of products.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(ORG_BUSINESS_RECORDS_PRODUCT_FORMULA, "Formula",
                        "Bills of materials, formulas, recipes and routes, which are often trade secrets.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(ORG_BUSINESS_RECORDS_PRODUCTION, "Production",
                        "Production orders, routes, resources, capacity and shop floor results.")
                .category(ORG_BUSINESS_RECORDS_QUALITY, "Quality",
                        "Quality orders, tests, non-conformances and certificates of analysis.",
                        c -> c.integrity(Integrity.HIGH))
                .category(ORG_BUSINESS_RECORDS_SALES_ORDER, "Sales Order",
                        "Quotations, orders, confirmations, deliveries and returns with customers that are"
                                + " organizations.")
                .category(ORG_BUSINESS_RECORDS_SERVICE_ORDER, "Service Order",
                        "Service agreements, service orders and repairs of the products sold.")
                .category(ORG_BUSINESS_RECORDS_SHIPMENT, "Shipment",
                        "Loads, shipments, carriers, routes, freight and customs documents.")
                .category(USER_CONTACT_DELIVERY, "Delivery Details",
                        "Recipient, delivery address, instructions, time slot and proof of delivery, such as a"
                                + " signature or a photo.",
                        c -> c.confidentiality(Confidentiality.MEDIUM))
                .category(USER_WORKPLACE_DRIVING_RECORD, "Driving Record",
                        "Tachograph data, driving and rest times, routes and vehicle geolocation of a driver.",
                        c -> c.confidentiality(Confidentiality.MEDIUM).integrity(Integrity.HIGH))
                .category(USER_WORKPLACE_LABOR_TRACKING, "Labour Tracking",
                        "Tasks, scans and units handled by a warehouse or production worker, and the productivity"
                                + " measured from them.",
                        c -> c.confidentiality(Confidentiality.MEDIUM));
    }

    private static void dataUses(TaxonomyBuilder builder) {
        builder.dataUse(OPERATIONS_AFTER_SALES_SERVICE, "After-Sales Service",
                        "Provides warranties, repairs and field service for the products sold.")
                .dataUse(OPERATIONS_INVENTORY, "Inventory Management", "Receives, stores, counts and moves stock.")
                .dataUse(OPERATIONS_LOGISTICS, "Logistics", "Plans and carries out transport and deliveries.")
                .dataUse(OPERATIONS_LOGISTICS_FLEET_MANAGEMENT, "Fleet Management",
                        "Manages vehicles and drivers, including driving times and vehicle geolocation.")
                .dataUse(OPERATIONS_PLANNING, "Supply Planning",
                        "Forecasts demand and plans supply, production and capacity.")
                .dataUse(OPERATIONS_PROCUREMENT, "Procurement",
                        "Sources, orders and receives goods and services from suppliers.")
                .dataUse(OPERATIONS_PRODUCT_MANAGEMENT, "Product Information Management",
                        "Defines products, their specifications, formulas and costs.")
                .dataUse(OPERATIONS_PRODUCTION, "Production", "Plans and runs production and records its results.")
                .dataUse(OPERATIONS_QUALITY, "Quality Management",
                        "Inspects and tests products and materials and handles non-conformances.")
                .dataUse(OPERATIONS_SUPPLIER_MANAGEMENT, "Supplier Management",
                        "Qualifies, evaluates and audits suppliers.")
                .dataUse(OPERATIONS_TRACEABILITY, "Traceability",
                        "Tracks batches and serial numbers through the supply chain for recalls and regulators.")
                .dataUse(SALES_ORDER_MANAGEMENT, "Order Management",
                        "Takes, confirms, fulfils and invoices customer orders and handles returns.");
    }

    private static void dataSubjects(TaxonomyBuilder builder) {
        builder.dataSubject(DELIVERY_RECIPIENT, "Delivery Recipient",
                        "A person who receives or signs for a delivery, who may not be the customer.")
                .dataSubject(DRIVER, "Driver",
                        "A person who drives a vehicle carrying the organization's goods, as an employee or for a"
                                + " carrier.")
                .dataSubject(SUPPLIER_CONTACT, "Supplier Contact",
                        "A person who acts for a supplier organization, such as a sales representative.");
    }
}
