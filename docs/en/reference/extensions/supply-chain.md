---
title: "Supply chain"
parent: "Extensions"
grand_parent: "Reference"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/SupplyChainTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/SupplyChainCategories.java
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/SupplyChainDataUses.java
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/SupplyChainDataSubjects.java
-->

# Supply chain extension

Package `io.github.devbpm.linneo.extension.supplychain` — exported, `@since 0.1.0`

Terms for supply chain management: products and their formulas and costs, procurement, inventory, production, quality,
planning, sales orders, transport and after-sales service, the personal data these processes touch, the purposes they
serve and the people involved. The extension is optional and not applied by default; see [Extensions](README.md).

It is informed by the supply chain business areas of the Common Data Model (CDM) (see [Sources](#sources)); no CDM
material is reproduced.

Most supply chain data is about organizations, products and goods and extends `org.business_records`, which is not
personal data. Warehouses and sites are the standard `org.location`, and suppliers and customers that are organizations
the standard `org.legal_entity`. Personal data appears where a process touches people: a consumer's orders are the
standard `user.behavior.purchase_history`, a delivery to a person is `user.contact.delivery` (delivery details), and the
workers who drive, pick and produce leave a driving record (`user.workplace.driving_record`) and labour tracking
(`user.workplace.labor_tracking`) data.

| Type | Description |
|---|---|
| `SupplyChainTaxonomy` | `public final class implements TaxonomyExtension`. `name()` returns `NAME`, `public static final String NAME = "supply_chain"`, recorded as the origin of its terms. Public no-argument constructor; no state. `contribute(builder)` adds the terms listed below. |
| `SupplyChainCategories` | `public final class`, private constructor: one `public static final String` per data category the extension adds (16). |
| `SupplyChainDataUses` | The same for its data uses (12). |
| `SupplyChainDataSubjects` | The same for its data subjects (3). |

The constants follow the [naming rule](../key-constants.md#naming-rule) of `Categories`:
`org.business_records.inventory` is `SupplyChainCategories.ORG_BUSINESS_RECORDS_INVENTORY`,
`operations.logistics.fleet_management` is `SupplyChainDataUses.OPERATIONS_LOGISTICS_FLEET_MANAGEMENT`. They are
compile-time constants, valid in annotations once the extension is applied.

## Applying it

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new SupplyChainTaxonomy()).build();
// Taxonomy[142 data categories, 75 data uses, 25 data subjects]
```

The extension shares no key with the other shipped extensions, so it can be applied with any of them, in the same
builder:

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new SupplyChainTaxonomy())
        .extend(new UniversityTaxonomy())   // io.github.devbpm.linneo.extension.university.UniversityTaxonomy
        .build();
```

`Taxonomy.load()` applies it only if the application registers it; see [Registering a shipped
extension](README.md#registering-a-shipped-extension).

A model then uses the extension's keys next to the standard ones:

```java
package com.example.app;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.extension.supplychain.SupplyChainCategories;

public class Delivery {
    @Category(Categories.USER_NAME)
    private String recipientName;

    @Category(SupplyChainCategories.USER_CONTACT_DELIVERY)
    private String address;

    @Category(SupplyChainCategories.USER_WORKPLACE_DRIVING_RECORD)
    private String driverTachograph;

    @Category(Categories.ORG_BUSINESS_RECORDS)
    private String orderReference;
}
```

```java
new Classifier(taxonomy).verify(List.of(Delivery.class)).get(0).fields().forEach(f ->
        System.out.println(f.path() + ": " + f.category().key() + " " + f.category().nature() + " "
                + f.confidentiality() + " " + f.integrity()));
```

```text
recipientName: user.name ORDINARY LOW MEDIUM
address: user.contact.delivery ORDINARY MEDIUM MEDIUM
driverTachograph: user.workplace.driving_record ORDINARY MEDIUM HIGH
orderReference: org.business_records NONE MEDIUM MEDIUM
```

## Fideslang rules for custom labels

The extension follows the rules fideslang sets for custom labels, so its terms stay interoperable with other tools:

- **Extend existing classes.** Every term is a child of an existing term; every use is a child of the standard
  `operations` or `sales` use.
- **The parent is the prefix of the key.** `org.business_records.inventory` is a child of `org.business_records`.
- **Data subjects are flat.** fideslang data subjects have no hierarchy, so every subject of the extension is a root.
- **No version information.** fideslang allows version information only in its default taxonomy, so no term of the
  extension carries any; its origin is recorded as `supply_chain`.

## Data categories

The protection shown is the effective one in the built taxonomy. Values in **bold** are declared by the extension; the
rest are inherited from the parent (`org.business_records` is `NONE`, `MEDIUM`, `MEDIUM`; `user.contact` is `ORDINARY`,
`LOW`, `MEDIUM`; `user.workplace` is `ORDINARY`, `LOW`, `MEDIUM`). The categories below `org.business_records` are not
personal data; the rest are. No category is secret.

### Products, procurement, production and logistics

Records about organizations, products and goods, below `org.business_records`: they are not personal data.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `org.business_records.inventory` | Inventory | `NONE` | `MEDIUM` | **`HIGH`** | Stock on hand, warehouses and locations, stock movements, counts, batches and serial numbers. |
| `org.business_records.planning` | Supply Planning | `NONE` | `MEDIUM` | `MEDIUM` | Demand forecasts, master plans, planned orders and capacity plans. |
| `org.business_records.price_agreement` | Price Agreement | `NONE` | `MEDIUM` | `MEDIUM` | Price lists, trade agreements, discounts and rebates agreed with customers and suppliers. |
| `org.business_records.procurement` | Procurement | `NONE` | `MEDIUM` | `MEDIUM` | Purchase requisitions, requests for quotation, purchase orders, receipts and supplier agreements. |
| `org.business_records.procurement.bid` | Supplier Bid | `NONE` | **`HIGH`** | `MEDIUM` | Quotations and bids received from suppliers, and their evaluation, before the award. |
| `org.business_records.product` | Product | `NONE` | `MEDIUM` | `MEDIUM` | Products and their variants, specifications, units, classifications and attributes. |
| `org.business_records.product.cost` | Product Cost | `NONE` | **`HIGH`** | `MEDIUM` | Standard costs, cost roll-ups and margins of products. |
| `org.business_records.product.formula` | Formula | `NONE` | **`HIGH`** | `MEDIUM` | Bills of materials, formulas, recipes and routes, which are often trade secrets. |
| `org.business_records.production` | Production | `NONE` | `MEDIUM` | `MEDIUM` | Production orders, routes, resources, capacity and shop floor results. |
| `org.business_records.quality` | Quality | `NONE` | `MEDIUM` | **`HIGH`** | Quality orders, tests, non-conformances and certificates of analysis. |
| `org.business_records.sales_order` | Sales Order | `NONE` | `MEDIUM` | `MEDIUM` | Quotations, orders, confirmations, deliveries and returns with customers that are organizations. |
| `org.business_records.service_order` | Service Order | `NONE` | `MEDIUM` | `MEDIUM` | Service agreements, service orders and repairs of the products sold. |
| `org.business_records.shipment` | Shipment | `NONE` | `MEDIUM` | `MEDIUM` | Loads, shipments, carriers, routes, freight and customs documents. |

### People in the supply chain

Personal data of recipients and workers, below `user.contact` and `user.workplace`.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `user.contact.delivery` | Delivery Details | `ORDINARY` | **`MEDIUM`** | `MEDIUM` | Recipient, delivery address, instructions, time slot and proof of delivery, such as a signature or a photo. |
| `user.workplace.driving_record` | Driving Record | `ORDINARY` | **`MEDIUM`** | **`HIGH`** | Tachograph data, driving and rest times, routes and vehicle geolocation of a driver. |
| `user.workplace.labor_tracking` | Labour Tracking | `ORDINARY` | **`MEDIUM`** | `MEDIUM` | Tasks, scans and units handled by a warehouse or production worker, and the productivity measured from them. |

### Why these levels

- **Formulas, product costs and supplier bids** have high confidentiality: formulas and costs are often trade secrets,
  and bids must stay confidential until the award.
- **Inventory and quality records** have high integrity, since stock values feed the books and quality and batch records
  support recalls and product safety obligations.
- A **driving record** has medium confidentiality and high integrity: tachograph records must be kept intact, and the
  geolocation of workers is restricted by LOPDGDD article 90.
- **Delivery details and labour tracking** have medium confidentiality, since they reveal where a person is and how a
  worker performs (LOPDGDD articles 87 to 91 limit the monitoring of workers).
- The other categories inherit the medium confidentiality of `org.business_records`.

## Where stricter data goes

A record of an extension category may contain details that reveal more than the category. Classify those fields with the
stricter key, not with the extension's:

| The field holds | Classify it as | Protection |
|---|---|---|
| An order or delivery of medicines or medical devices for a person | `user.health_and_medical` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| A driver's medical fitness | `user.health_and_medical` (not `user.workplace.driving_record`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |

## Data uses

Every use is a child of the standard `operations` or `sales` use. Data uses have no protection facets, only
[traits](#traits).

| Key | Name | Parent | Purpose |
|---|---|---|---|
| `operations.after_sales_service` | After-Sales Service | `operations` | Provides warranties, repairs and field service for the products sold. |
| `operations.inventory` | Inventory Management | `operations` | Receives, stores, counts and moves stock. |
| `operations.logistics` | Logistics | `operations` | Plans and carries out transport and deliveries. |
| `operations.logistics.fleet_management` | Fleet Management | `operations.logistics` | Manages vehicles and drivers, including driving times and vehicle geolocation. |
| `operations.planning` | Supply Planning | `operations` | Forecasts demand and plans supply, production and capacity. |
| `operations.procurement` | Procurement | `operations` | Sources, orders and receives goods and services from suppliers. |
| `operations.product_management` | Product Information Management | `operations` | Defines products, their specifications, formulas and costs. |
| `operations.production` | Production | `operations` | Plans and runs production and records its results. |
| `operations.quality` | Quality Management | `operations` | Inspects and tests products and materials and handles non-conformances. |
| `operations.supplier_management` | Supplier Management | `operations` | Qualifies, evaluates and audits suppliers. |
| `operations.traceability` | Traceability | `operations` | Tracks batches and serial numbers through the supply chain for recalls and regulators. |
| `sales.order_management` | Order Management | `sales` | Takes, confirms, fulfils and invoices customer orders and handles returns. |

## Data subjects

All the data subjects of the extension are flat, as fideslang data subjects have no hierarchy.

| Key | Name | Who |
|---|---|---|
| `delivery_recipient` | Delivery Recipient | A person who receives or signs for a delivery, who may not be the customer. |
| `driver` | Driver | A person who drives a vehicle carrying the organization's goods, as an employee or for a carrier. |
| `supplier_contact` | Supplier Contact | A person who acts for a supplier organization, such as a sales representative. |

## Traits

The extension declares the [traits](../terms.md#trait) that Linneo's legal rules read, so its terms weigh in the
[impact screening](../processing/impact-screening.md), the [breach assessment](../processing/breach-assessment.md) and
the [subject rights](../processing/subject-rights.md) like the standard ones:

| Terms | Trait | Why |
|---|---|---|
| Data use `operations.logistics.fleet_management` | `MONITORING` | Fleet management tracks the times and the location of drivers (WP248 criterion 3) |
| Data subject `driver` | `VULNERABLE` | Drivers work for the organization or for its carriers, in a weaker position than the controller (WP248 criterion 7) |

## Standard keys for everything else

The extension adds only what has no standard key. What the standard taxonomy already covers keeps its standard key:

| Attribute | Standard key | Protection |
|---|---|---|
| Warehouses and sites | `org.location` | `NONE`, `LOW`, `MEDIUM` |
| Suppliers and customers that are organizations | `org.legal_entity` | `NONE`, `PUBLIC`, `MEDIUM` |
| A consumer's orders | `user.behavior.purchase_history` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| Other data about products and goods | `org.business_records` and its children | `NONE`, `MEDIUM`, `MEDIUM` |

## Sources

The terms are informed by the supply chain business areas of the **Common Data Model (CDM)** schemas:
<https://github.com/microsoft/CDM> (documentation: <https://learn.microsoft.com/common-data-model/>). Copyright (c)
Microsoft Corporation. The documentation and other content of CDM are licensed under [Creative Commons Attribution 4.0
International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/); its code is under the MIT licence.

No CDM material (schemas, entity or attribute definitions, texts or enumerations) is reproduced in Linneo; CDM is cited
as the source of the concepts, and the names and descriptions of the terms are Linneo's. Linneo is not affiliated with
or endorsed by Microsoft. See also `NOTICE` and the "Licence and attribution" section of the `README`.

See also: [Extensions](README.md) · [Standard taxonomy](../standard-taxonomy.md) · [Key constants](../key-constants.md)
· [Protection levels](../protection-levels.md)
