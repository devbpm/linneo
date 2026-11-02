---
title: "Open Data Contract Standard"
parent: "Export"
grand_parent: "Reference"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/export/odcs/OdcsSchema.java
  - src/main/java/io/github/devbpm/linneo/export/odcs/package-info.java
  - src/main/java/io/github/devbpm/linneo/internal/Json.java
-->

# Open Data Contract Standard

`io.github.devbpm.linneo.export.odcs.OdcsSchema` — `public final class`, `@since 0.1.0`

The classification of a [data inventory](../classify/inventory.md) as the `schema` of an
[Open Data Contract Standard](https://bitol-io.github.io/open-data-contract-standard/) (ODCS v3) data contract: one
schema object per object of the inventory and one property per classified field, so that a data contract says what each
property holds and how it must be protected. Instances are immutable and thread-safe.

The result is JSON, which is also valid YAML: merge it into the contract, whose servers, types and owners Linneo does
not know. Linneo reads and writes no contract. The names of the properties are the field paths (`home.street` for a
part of a [`@Composite`](../annotations/composite.md)); map them when the contract uses other names.

## What a property says

| Member | Value |
|---|---|
| `name` | The field path. |
| `classification` | `public`, `internal`, `confidential` or `restricted`, from the confidentiality of the field; see below. |
| `tags` | The key of the category, then `personal-data` for personal data, `special-category` or `criminal-offence-data` for those natures, and `secret` for a secret. |
| `customProperties` | `linneo.category` (the key), `linneo.confidentiality` and `linneo.integrity` (the names of the levels) and, for personal data only, `linneo.identifiability`; each as `{"property": ..., "value": ...}`. |

The classification uses the usual four-level scale:

| Confidentiality | `classification` |
|---|---|
| `PUBLIC` | `public` |
| `LOW` | `internal` |
| `MEDIUM` | `confidential` |
| `HIGH` | `restricted` |
| any, with a secret | `restricted` |

The levels are the effective ones: those of the category, with any raise declared on the field. Exempted fields hold no
data and are left out of the schema.

## Members

| Member | Returns | Description |
|---|---|---|
| `static OdcsSchema of(Taxonomy taxonomy)` | writer | The schema writer for a taxonomy, which resolves the keys the inventory stores. `NullPointerException` if `taxonomy` is `null`. |
| `static String classification(Protection protection)` | `String` | The classification label of a protection: `public`, `internal`, `confidential` or `restricted`. |
| `String schemaJson(DataInventory inventory)` | `String` | The `schema` of a contract: a JSON array with one schema object per object of the inventory, in its order. `TaxonomyException` if the taxonomy does not contain a key of the inventory. |
| `String objectJson(DataInventory inventory, String object)` | `String` | The schema object of one object: a JSON object with `name`, `logicalType` (`object`) and `properties`. `IllegalArgumentException` if the inventory has no such object. |
| `String propertyJson(InventoryField field)` | `String` | The schema property of one classified field: a JSON object with `name`, `classification`, `tags` and `customProperties`. `IllegalArgumentException` if the field is exempted. `TaxonomyException` if the taxonomy does not contain its key. |

## Using it

```java
import io.github.devbpm.linneo.export.odcs.OdcsSchema;

DataInventory inventory = classifier.inventory(List.of(Student.class), type -> "students");
String schema = OdcsSchema.of(taxonomy).schemaJson(inventory);
```

For a `Student` with, among others, a `disabilityDegree` of `user.health_and_medical.disability` and a `passwordHash` of
`user.authorization.password`, `schema` is, formatted here for reading (Linneo writes it on one line):

```json
[
  {
    "name": "students",
    "logicalType": "object",
    "properties": [
      {
        "name": "disabilityDegree",
        "classification": "restricted",
        "tags": ["user.health_and_medical.disability", "personal-data", "special-category"],
        "customProperties": [
          {"property": "linneo.category", "value": "user.health_and_medical.disability"},
          {"property": "linneo.confidentiality", "value": "HIGH"},
          {"property": "linneo.integrity", "value": "HIGH"},
          {"property": "linneo.identifiability", "value": "IDENTIFIED"}
        ]
      },
      {
        "name": "passwordHash",
        "classification": "restricted",
        "tags": ["user.authorization.password", "personal-data", "secret"],
        "customProperties": [
          {"property": "linneo.category", "value": "user.authorization.password"},
          {"property": "linneo.confidentiality", "value": "HIGH"},
          {"property": "linneo.integrity", "value": "HIGH"},
          {"property": "linneo.identifiability", "value": "IDENTIFIED"}
        ]
      }
    ]
  }
]
```

Paste the array under `schema:` in the contract, or write it to a file the contract's build merges. Because it comes
from the inventory, the schema follows the classification: regenerate it when the
[inventory changes](../classify/inventory.md#inventorydiff), and a review sees the change of a classification as a
change of the contract.

See also: [Export](README.md) · [Data inventory](../classify/inventory.md) ·
[Protection levels](../protection-levels.md)
