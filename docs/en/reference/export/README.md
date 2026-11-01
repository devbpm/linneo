---
title: "Export"
parent: "Reference"
nav_order: 19
permalink: "/en/reference/export/"
---
<!--
doc-sources:
  - src/main/java/module-info.java
  - src/main/java/io/github/devbpm/linneo/export/openapi/package-info.java
  - src/main/java/io/github/devbpm/linneo/export/postgresql/package-info.java
  - src/main/java/io/github/devbpm/linneo/export/odcs/package-info.java
-->

# Export

Packages `io.github.devbpm.linneo.export.openapi`, `.postgresql` and `.odcs` — exported, `@since 0.1.0`

The [catalog exporters](../README.md#data-catalogues-compared) publish the **taxonomy**. These three publish the
**classification of the fields of a model** to the tools that sit next to the code: the description of an API, the
database that holds the data and the data contract that other teams read. Each one takes what the
[`Classifier`](../classify/classifier.md) knows about a field, or what a [data inventory](../classify/inventory.md) has
stored about it, and writes the text the other tool expects.

| Package | Entry point | Writes | Page |
|---|---|---|---|
| `io.github.devbpm.linneo.export.openapi` | `OpenApiExtension` | The `x-linneo` specification extension of each property of an API description, as maps or JSON | [OpenAPI extension](openapi.md) |
| `io.github.devbpm.linneo.export.postgresql` | `PostgresAnonymizer` | The `SECURITY LABEL` statements of PostgreSQL Anonymizer that mask or null each column, following a [`DisclosurePolicy`](../classify/disclosure.md) | [PostgreSQL Anonymizer](postgresql.md) |
| `io.github.devbpm.linneo.export.odcs` | `OdcsSchema` | The `schema` section of an Open Data Contract Standard (v3) data contract, from a data inventory, as JSON | [Open Data Contract Standard](odcs.md) |

What they have in common:

- **They write text and nothing else.** None of them reads or writes your API description, connects to a database or
  edits a contract, and none makes network calls. The application chooses the property, column or contract each field
  belongs to, and applies the result.
- **The protection comes from the classification**, never from the values: a field is described by its category, its
  levels (those of the category, raised on the field if the code says so) and whether it holds a secret.
- **Two inputs.** A `ClassClassification` (or its `FieldClassification`s) is what the code declares now, for a build
  or a start-up. An `InventoryField` of a [`DataInventory`](../classify/inventory.md), resolved with a `Taxonomy`, is
  what an application has stored, so the same exporter can run in a back office that has no model classes.
  `OpenApiExtension` and `PostgresAnonymizer` accept both; `OdcsSchema` reads the inventory.
- **A class with classification errors is refused.** Where an exporter takes a `ClassClassification`, it throws
  `IllegalArgumentException` if the class has errors, because a field with an error has no classification and would be
  left without protection. Fix it with the [classifier test](../../course/04-verifying-in-tests.md).
- **Exempted fields hold no data.** `PostgresAnonymizer` gives them an `IS NULL` label (no rule), `OpenApiExtension`
  describes them with no extension and `OdcsSchema` leaves them out.

```java
import io.github.devbpm.linneo.export.openapi.OpenApiExtension;
import io.github.devbpm.linneo.export.postgresql.PostgresAnonymizer;

ClassClassification student = classifier.classify(Student.class);

// For the description of the API: one extension per classified field.
Map<String, Map<String, Object>> api = OpenApiExtension.of(student);

// For the test database: one statement per field.
List<String> sql = PostgresAnonymizer.of(DisclosurePolicy.forLogs())
        .labels(student, path -> "students." + path);
```

See also: [Data inventory](../classify/inventory.md) · [Disclosure policy](../classify/disclosure.md) ·
[Reference](../README.md)
