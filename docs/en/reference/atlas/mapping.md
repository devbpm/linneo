---
title: "Apache Atlas mapping"
parent: "Reference"
nav_order: 18
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasMapping.java
-->

# Apache Atlas mapping

`io.github.devbpm.linneo.catalog.atlas.AtlasMapping` — `public record`, `@since 0.1.0`

The names of the root Atlas classifications under which the taxonomy is published. Each name is the first segment of
the classifications below it: with the defaults, `user.contact.email` is the classification
`DataCategory.user.contact.email`. Pass it to [`AtlasCatalog.of(taxonomy, mapping)`](catalog.md).

```java
public record AtlasMapping(String categories, String personalData, String confidentiality, String integrity,
                           String identifiability, String handling)
```

## Components and defaults

| Component | Default | Root of | Classifications below it |
|---|---|---|---|
| `categories` | `DataCategory` | the category hierarchy | `<name>.<key>` for every category, each a sub-type of its parent's |
| `personalData` | `PersonalData` | the legal nature | `<name>.Personal`, `<name>.SpecialCategory`, `<name>.CriminalOffence` |
| `confidentiality` | `Confidentiality` | the confidentiality levels | `<name>.Public`, `.Low`, `.Medium`, `.High` |
| `integrity` | `Integrity` | the integrity levels | `<name>.Low`, `.Medium`, `.High` |
| `identifiability` | `Identifiability` | the identifiability of personal columns | `<name>.Identified`, `.Pseudonymized`, `.Anonymized`, `.Aggregated` |
| `handling` | `DataHandling` | handling rules | `<name>.Secret` |

Linneo creates all six roots. The names after the root are fixed.

## Validation

The canonical constructor checks every name:

| Problem | Exception |
|---|---|
| A name is `null` | `NullPointerException` (`classification name`) |
| A name does not match `[A-Za-z][A-Za-z0-9_]*` (it is empty, starts with a digit or contains a dot, a space…) | `IllegalArgumentException` (`Invalid Atlas classification name '<name>'`) |
| Two of the six names are equal | `IllegalArgumentException` (`Duplicate Atlas classification name '<name>'`) |

Atlas itself accepts dots and spaces in classification names; Linneo forbids them in the roots so that the first
segment of every name is the root.

## Methods

| Method | Description |
|---|---|
| `static AtlasMapping defaults()` | The defaults above. |
| `AtlasMapping withCategories(String name)` | A copy with another name for the categories root. |
| `AtlasMapping withPersonalData(String name)` | A copy with another name for the legal nature root. |
| `AtlasMapping withConfidentiality(String name)` | A copy with another name for the confidentiality root. |
| `AtlasMapping withIntegrity(String name)` | A copy with another name for the integrity root. |
| `AtlasMapping withIdentifiability(String name)` | A copy with another name for the identifiability root. |
| `AtlasMapping withHandling(String name)` | A copy with another name for the handling root. |

The withers validate like the constructor.

## Examples

```java
AtlasMapping mapping = AtlasMapping.defaults()
        .withCategories("CategoriaDato")
        .withConfidentiality("Confidencialidad");

AtlasCatalog catalog = AtlasCatalog.of(Taxonomy.standard(), mapping);
catalog.classificationsOf(Taxonomy.standard().requireCategory(Categories.USER_NAME));
// [CategoriaDato.user.name, PersonalData.Personal, Confidencialidad.Low, Integrity.Medium]
```

Renaming every root:

```java
AtlasMapping mapping = new AtlasMapping("CategoriaDato", "DatoPersonal", "Confidencialidad", "Integridad",
        "Identificabilidad", "Tratamiento");
```

```java
AtlasMapping.defaults().withCategories("Data.Category");
// IllegalArgumentException: Invalid Atlas classification name 'Data.Category'
```

See also: [Apache Atlas catalog](catalog.md) · [Protection levels](../protection-levels.md) · course:
[DataHub and Apache Atlas](../../course/08-datahub-and-atlas.md)
