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
 * Keys of the data uses added by the {@link SupplyChainTaxonomy}, below the standard {@code operations} and
 * {@code sales} uses.
 *
 * @since 0.1.0
 */
public final class SupplyChainDataUses {

    /** After-Sales Service: Provides warranties, repairs and field service for the products sold. */
    public static final String OPERATIONS_AFTER_SALES_SERVICE = "operations.after_sales_service";

    /** Inventory Management: Receives, stores, counts and moves stock. */
    public static final String OPERATIONS_INVENTORY = "operations.inventory";

    /** Logistics: Plans and carries out transport and deliveries. */
    public static final String OPERATIONS_LOGISTICS = "operations.logistics";

    /** Fleet Management: Manages vehicles and drivers, including driving times and vehicle geolocation. */
    public static final String OPERATIONS_LOGISTICS_FLEET_MANAGEMENT = "operations.logistics.fleet_management";

    /** Supply Planning: Forecasts demand and plans supply, production and capacity. */
    public static final String OPERATIONS_PLANNING = "operations.planning";

    /** Procurement: Sources, orders and receives goods and services from suppliers. */
    public static final String OPERATIONS_PROCUREMENT = "operations.procurement";

    /** Product Information Management: Defines products, their specifications, formulas and costs. */
    public static final String OPERATIONS_PRODUCT_MANAGEMENT = "operations.product_management";

    /** Production: Plans and runs production and records its results. */
    public static final String OPERATIONS_PRODUCTION = "operations.production";

    /** Quality Management: Inspects and tests products and materials and handles non-conformances. */
    public static final String OPERATIONS_QUALITY = "operations.quality";

    /** Supplier Management: Qualifies, evaluates and audits suppliers. */
    public static final String OPERATIONS_SUPPLIER_MANAGEMENT = "operations.supplier_management";

    /** Traceability: Tracks batches and serial numbers through the supply chain for recalls and regulators. */
    public static final String OPERATIONS_TRACEABILITY = "operations.traceability";

    /** Order Management: Takes, confirms, fulfils and invoices customer orders and handles returns. */
    public static final String SALES_ORDER_MANAGEMENT = "sales.order_management";

    private SupplyChainDataUses() {
    }
}
