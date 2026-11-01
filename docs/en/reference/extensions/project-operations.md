---
title: "Project operations"
parent: "Extensions"
grand_parent: "Reference"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/ProjectOperationsTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/ProjectOperationsCategories.java
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/ProjectOperationsDataUses.java
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/ProjectOperationsDataSubjects.java
-->

# Project operations extension

Package `io.github.devbpm.linneo.extension.projectoperations` — exported, `@since 0.1.0`

Terms for project operations, the management of project-based services: projects and their contracts, estimates,
actuals, funding and rates, the data about the people who staff them, the purposes they serve and the people involved.
The extension is optional and not applied by default; see [Extensions](README.md).

It is informed by the project management and accounting and the project service automation areas of the Common Data
Model (CDM) (see [Sources](#sources)); no CDM material is reproduced.

Project records extend `org.business_records`, which is not personal data; what describes a person as a project resource
extends `user.workplace`: the assignments and bookings (`user.workplace.project_assignment`), the time entries
(`user.workplace.time_entry`), the skills used to match people to work (`user.workplace.skill`), their utilization
(`user.workplace.utilization`) and their rates (`user.workplace.resource_rate`). The project team role is the standard
`user.workplace.role`.

The extension does not depend on the finance extension: an expense charged to a project is an expense claim of the
[finance extension](finance.md) where it is applied, and otherwise the standard `user.financial`.

| Type | Description |
|---|---|
| `ProjectOperationsTaxonomy` | `public final class implements TaxonomyExtension`. `name()` returns `NAME`, `public static final String NAME = "project_operations"`, recorded as the origin of its terms. Public no-argument constructor; no state. `contribute(builder)` adds the terms listed below. |
| `ProjectOperationsCategories` | `public final class`, private constructor: one `public static final String` per data category the extension adds (11). |
| `ProjectOperationsDataUses` | The same for its data uses (6). |
| `ProjectOperationsDataSubjects` | The same for its data subjects (2). |

The constants follow the [naming rule](../key-constants.md#naming-rule) of `Categories`: `user.workplace.time_entry` is
`ProjectOperationsCategories.USER_WORKPLACE_TIME_ENTRY`, `operations.project_management.billing` is
`ProjectOperationsDataUses.OPERATIONS_PROJECT_MANAGEMENT_BILLING`. They are compile-time constants, valid in annotations
once the extension is applied.

## Applying it

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new ProjectOperationsTaxonomy()).build();
// Taxonomy[137 data categories, 69 data uses, 24 data subjects]
```

The extension shares no key with the other shipped extensions, so it can be applied with any of them, in the same
builder:

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new ProjectOperationsTaxonomy())
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
import io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsCategories;

import java.math.BigDecimal;

public class TimeEntry {
    @Category(Categories.USER_UNIQUE_ID)
    private String employeeNumber;

    @Category(ProjectOperationsCategories.USER_WORKPLACE_TIME_ENTRY)
    private BigDecimal hours;

    @Category(ProjectOperationsCategories.USER_WORKPLACE_RESOURCE_RATE)
    private BigDecimal costRate;
}
```

```java
new Classifier(taxonomy).verify(List.of(TimeEntry.class)).get(0).fields().forEach(f ->
        System.out.println(f.field() + ": " + f.category().key() + " " + f.category().nature() + " "
                + f.confidentiality() + " " + f.integrity()));
```

```text
employeeNumber: user.unique_id ORDINARY LOW MEDIUM
hours: user.workplace.time_entry ORDINARY LOW HIGH
costRate: user.workplace.resource_rate ORDINARY HIGH MEDIUM
```

## Fideslang rules for custom labels

The extension follows the rules fideslang sets for custom labels, so its terms stay interoperable with other tools:

- **Extend existing classes.** Every term is a child of an existing term; every use is a child of the standard
  `operations` use; `operations.project_management` is new and the others sit below it.
- **The parent is the prefix of the key.** `user.workplace.time_entry` is a child of `user.workplace`.
- **Data subjects are flat.** fideslang data subjects have no hierarchy, so every subject of the extension is a root.
- **No version information.** fideslang allows version information only in its default taxonomy, so no term of the
  extension carries any; its origin is recorded as `project_operations`.

## Data categories

The protection shown is the effective one in the built taxonomy. Values in **bold** are declared by the extension; the
rest are inherited from the parent (`org.business_records` is `NONE`, `MEDIUM`, `MEDIUM`; `user.workplace` is
`ORDINARY`, `LOW`, `MEDIUM`). The categories below `org.business_records` are not personal data; the rest are. No
category is secret.

### Projects

Records about projects, below `org.business_records.project`: they are not personal data.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `org.business_records.project` | Project | `NONE` | `MEDIUM` | `MEDIUM` | Projects, work breakdown structures, tasks, dependencies, milestones and status. |
| `org.business_records.project.actual` | Project Actual | `NONE` | `MEDIUM` | **`HIGH`** | Cost, revenue and hours posted to a project. |
| `org.business_records.project.contract` | Project Contract | `NONE` | `MEDIUM` | `MEDIUM` | Contracts and contract lines agreed with a customer for a project, with their billing methods and invoice schedules. |
| `org.business_records.project.estimate` | Project Estimate | `NONE` | `MEDIUM` | `MEDIUM` | Estimates, quotations, budgets and forecasts of a project's cost and revenue. |
| `org.business_records.project.funding` | Project Funding | `NONE` | `MEDIUM` | `MEDIUM` | Funding sources, grants, funding rules and their limits. |
| `org.business_records.project.rate` | Project Rate | `NONE` | **`HIGH`** | `MEDIUM` | Cost and sales prices of roles, categories and transaction types used to price projects. |

### People as project resources

What describes a person on a project, below `user.workplace`.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `user.workplace.project_assignment` | Project Assignment | `ORDINARY` | `LOW` | `MEDIUM` | Projects, tasks and roles the subject is assigned to, with bookings and planned hours. |
| `user.workplace.resource_rate` | Resource Rate | `ORDINARY` | **`HIGH`** | `MEDIUM` | Cost and billing rates set for the subject as a project resource. |
| `user.workplace.skill` | Skill | `ORDINARY` | `LOW` | `MEDIUM` | Skills, certifications and proficiency ratings used to match the subject to work. |
| `user.workplace.time_entry` | Time Entry | `ORDINARY` | `LOW` | **`HIGH`** | Hours the subject reports on projects and tasks, with their descriptions and approvals. |
| `user.workplace.utilization` | Utilization | `ORDINARY` | **`MEDIUM`** | `MEDIUM` | Billable and productive hours of the subject compared with capacity and targets. |

### Why these levels

- **Project rates and resource rates** have high confidentiality: they are commercially sensitive, and the cost rate of
  a person reveals what the organization pays them.
- **Time entries and project actuals** have high integrity, since customers are billed and revenue is recognised from
  them.
- **Utilization** has medium confidentiality, since it measures how a worker performs and LOPDGDD articles 87 to 91
  limit the monitoring of workers.
- The other categories inherit the medium confidentiality of `org.business_records` and the low confidentiality of
  `user.workplace`.

## Where stricter data goes

A record of an extension category may contain details that reveal more than the category. Classify those fields with the
stricter key, not with the extension's:

| The field holds | Classify it as | Protection |
|---|---|---|
| The medical reason why a resource is not available | `user.health_and_medical` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| A time entry used to evaluate a worker's performance | `user.employment.performance` of the [human resources extension](human-resources.md), where it is applied | `ORDINARY`, `HIGH`, `MEDIUM` |

## Data uses

Every use is a child of the standard `operations` use; `operations.project_management` is new and the others sit below
it. Data uses have no protection facets.

| Key | Name | Parent | Purpose |
|---|---|---|---|
| `operations.project_management` | Project Management | `operations` | Plans, executes and controls projects. |
| `operations.project_management.accounting` | Project Accounting | `operations.project_management` | Records project cost and revenue, recognises revenue and reports profitability. |
| `operations.project_management.billing` | Project Billing | `operations.project_management` | Invoices customers for project work as the contract provides. |
| `operations.project_management.estimating` | Project Estimating | `operations.project_management` | Prepares estimates and quotations for project work. |
| `operations.project_management.resource_management` | Resource Management | `operations.project_management` | Requests, schedules and books people for projects according to their skills and availability. |
| `operations.project_management.time_and_expense` | Time and Expense | `operations.project_management` | Records and approves the time and expenses charged to projects. |

## Data subjects

All the data subjects of the extension are flat, as fideslang data subjects have no hierarchy.

| Key | Name | Who |
|---|---|---|
| `project_stakeholder` | Project Stakeholder | A person at the customer or a partner who is involved in a project, such as a sponsor or an approver. |
| `subcontractor` | Subcontractor | An individual who works on the organization's projects under a contract with the organization or a supplier, rather than an employment. |

## Standard keys for everything else

The extension adds only what has no standard key. What the standard taxonomy already covers keeps its standard key:

| Attribute | Standard key | Protection |
|---|---|---|
| The team role of a person on a project | `user.workplace.role` | `ORDINARY`, `LOW`, `MEDIUM` |
| An expense charged to a project | `user.financial` (or `user.financial.expense` where the [finance extension](finance.md) is applied) | `ORDINARY`, `MEDIUM`, `HIGH` |

## Sources

The terms are informed by the project management and accounting and the project service automation areas of the **Common
Data Model (CDM)** schemas: <https://github.com/microsoft/CDM> (documentation:
<https://learn.microsoft.com/common-data-model/>). Copyright (c) Microsoft Corporation. The documentation and other
content of CDM are licensed under [Creative Commons Attribution 4.0 International (CC BY
4.0)](https://creativecommons.org/licenses/by/4.0/); its code is under the MIT licence.

No CDM material (schemas, entity or attribute definitions, texts or enumerations) is reproduced in Linneo; CDM is cited
as the source of the concepts, and the names and descriptions of the terms are Linneo's. Linneo is not affiliated with
or endorsed by Microsoft. See also `NOTICE` and the "Licence and attribution" section of the `README`.

See also: [Extensions](README.md) · [Standard taxonomy](../standard-taxonomy.md) · [Key constants](../key-constants.md)
· [Protection levels](../protection-levels.md)
