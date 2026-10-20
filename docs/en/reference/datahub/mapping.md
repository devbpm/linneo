---
title: "DataHub mapping"
parent: "Reference"
nav_order: 16
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DataHubMapping.java
-->

# DataHub mapping

`io.github.devbpm.linneo.catalog.datahub.DataHubMapping` — `public record`, `@since 0.1.0`

The names of the DataHub glossary nodes under which the taxonomy is published. Each name is the first segment of the
URNs below it: with the defaults, `user.contact.email` is the term
`urn:li:glossaryTerm:DataCategory.user.contact.email`. Pass it to
[`DataHubCatalog.of(taxonomy, mapping)`](catalog.md).

```java
public record DataHubMapping(String categories, String personalData, String confidentiality, String integrity,
                             String identifiability, String handling)
```

## Components and defaults

| Component | Default | Node of | Terms below it |
|---|---|---|---|
| `categories` | `DataCategory` | the category hierarchy | `<name>.<key>` for every category, with a sub-node `<name>.<key>` for every category with children |
| `personalData` | `PersonalData` | the legal nature | `Personal`, `SpecialCategory`, `CriminalOffence` |
| `confidentiality` | `Confidentiality` | the confidentiality levels | `Public`, `Low`, `Medium`, `High` |
| `integrity` | `Integrity` | the integrity levels | `Low`, `Medium`, `High` |
| `identifiability` | `Identifiability` | the identifiability of personal fields | `Identified`, `Pseudonymized`, `Anonymized`, `Aggregated` |
| `handling` | `DataHandling` | handling rules | `Secret` |

Linneo creates all six nodes; DataHub has no built-in glossary that it reuses. The term names inside each node are
fixed, and so are the display names of the root nodes (`Data Category`, `Personal Data`, `Data Handling`, …): the
mapping changes the ids and URNs, not the `name` shown in the UI.

## Validation

The canonical constructor checks every name:

| Problem | Exception |
|---|---|
| A name is `null` | `NullPointerException` (`glossary node name`) |
| A name does not match `[A-Za-z][A-Za-z0-9_]*` (it is empty, starts with a digit or contains a dot, a space, a comma…) | `IllegalArgumentException` (`Invalid DataHub glossary node name '<name>'`) |
| Two of the six names are equal | `IllegalArgumentException` (`Duplicate DataHub glossary node name '<name>'`) |

The rule keeps every name a single URN segment.

## Methods

| Method | Description |
|---|---|
| `static DataHubMapping defaults()` | The defaults above. |
| `DataHubMapping withCategories(String name)` | A copy with another name for the categories node. |
| `DataHubMapping withPersonalData(String name)` | A copy with another name for the legal nature node. |
| `DataHubMapping withConfidentiality(String name)` | A copy with another name for the confidentiality node. |
| `DataHubMapping withIntegrity(String name)` | A copy with another name for the integrity node. |
| `DataHubMapping withIdentifiability(String name)` | A copy with another name for the identifiability node. |
| `DataHubMapping withHandling(String name)` | A copy with another name for the handling node. |

The withers validate like the constructor.

## Examples

```java
DataHubMapping mapping = DataHubMapping.defaults()
        .withCategories("CategoriaDato")
        .withConfidentiality("Confidencialidad");

DataHubCatalog catalog = DataHubCatalog.of(Taxonomy.standard(), mapping);
catalog.termsOf(Taxonomy.standard().requireCategory(Categories.USER_NAME));
// [urn:li:glossaryTerm:CategoriaDato.user.name, urn:li:glossaryTerm:PersonalData.Personal,
//  urn:li:glossaryTerm:Confidencialidad.Low, urn:li:glossaryTerm:Integrity.Medium]
```

Renaming every node:

```java
DataHubMapping mapping = new DataHubMapping("CategoriaDato", "DatoPersonal", "Confidencialidad", "Integridad",
        "Identificabilidad", "Tratamiento");
```

```java
DataHubMapping.defaults().withHandling("Data Handling");
// IllegalArgumentException: Invalid DataHub glossary node name 'Data Handling'
```

See also: [DataHub catalog](catalog.md) · [Protection levels](../protection-levels.md) · course:
[DataHub and Apache Atlas](../../course/08-datahub-and-atlas.md)
