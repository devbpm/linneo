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

/**
 * Keys of the data categories added by the {@link SupplyChainTaxonomy}, for use in annotations.
 *
 * @since 0.1.0
 */
public final class SupplyChainCategories {

    /** Inventory: Stock on hand, warehouses and locations, stock movements, counts, batches and serial numbers. */
    public static final String ORG_BUSINESS_RECORDS_INVENTORY = "org.business_records.inventory";

    /** Supply Planning: Demand forecasts, master plans, planned orders and capacity plans. */
    public static final String ORG_BUSINESS_RECORDS_PLANNING = "org.business_records.planning";

    /** Price Agreement: Price lists, trade agreements, discounts and rebates agreed with customers and suppliers. */
    public static final String ORG_BUSINESS_RECORDS_PRICE_AGREEMENT = "org.business_records.price_agreement";

    /** Procurement: Purchase requisitions, requests for quotation, purchase orders, receipts, supplier agreements. */
    public static final String ORG_BUSINESS_RECORDS_PROCUREMENT = "org.business_records.procurement";

    /** Supplier Bid: Quotations and bids received from suppliers, and their evaluation, before the award. */
    public static final String ORG_BUSINESS_RECORDS_PROCUREMENT_BID = "org.business_records.procurement.bid";

    /** Product: Products and their variants, specifications, units, classifications and attributes. */
    public static final String ORG_BUSINESS_RECORDS_PRODUCT = "org.business_records.product";

    /** Product Cost: Standard costs, cost roll-ups and margins of products. */
    public static final String ORG_BUSINESS_RECORDS_PRODUCT_COST = "org.business_records.product.cost";

    /** Formula: Bills of materials, formulas, recipes and routes, which are often trade secrets. */
    public static final String ORG_BUSINESS_RECORDS_PRODUCT_FORMULA = "org.business_records.product.formula";

    /** Production: Production orders, routes, resources, capacity and shop floor results. */
    public static final String ORG_BUSINESS_RECORDS_PRODUCTION = "org.business_records.production";

    /** Quality: Quality orders, tests, non-conformances and certificates of analysis. */
    public static final String ORG_BUSINESS_RECORDS_QUALITY = "org.business_records.quality";

    /** Sales Order: Quotations, orders, confirmations, deliveries and returns with customers that are organizations. */
    public static final String ORG_BUSINESS_RECORDS_SALES_ORDER = "org.business_records.sales_order";

    /** Service Order: Service agreements, service orders and repairs of the products sold. */
    public static final String ORG_BUSINESS_RECORDS_SERVICE_ORDER = "org.business_records.service_order";

    /** Shipment: Loads, shipments, carriers, routes, freight and customs documents. */
    public static final String ORG_BUSINESS_RECORDS_SHIPMENT = "org.business_records.shipment";

    /** Delivery Details: Recipient, address, instructions, time slot and proof of delivery, such as a signature. */
    public static final String USER_CONTACT_DELIVERY = "user.contact.delivery";

    /** Driving Record: Tachograph data, driving and rest times, routes and vehicle geolocation of a driver. */
    public static final String USER_WORKPLACE_DRIVING_RECORD = "user.workplace.driving_record";

    /** Labour Tracking: Tasks, scans and units handled by a worker, and the productivity measured from them. */
    public static final String USER_WORKPLACE_LABOR_TRACKING = "user.workplace.labor_tracking";

    private SupplyChainCategories() {
    }
}
