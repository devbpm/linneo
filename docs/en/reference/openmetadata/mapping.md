---
title: "OpenMetadata mapping"
parent: "Reference"
nav_order: 14
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OpenMetadataMapping.java
-->

# OpenMetadata mapping

`io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataMapping` — `public record`, `@since 0.1.0`

The names under which the taxonomy is published in OpenMetadata, and when a personal column counts as sensitive.
Pass it to [`OpenMetadataCatalog.of(taxonomy, mapping)`](catalog.md).

```java
public record OpenMetadataMapping(String categories, String personalData, String pii, String confidentiality,
                                  String integrity, String identifiability, String handling,
                                  Confidentiality sensitiveFrom)
```

## Components and defaults

| Component | Default | Created by Linneo? | Meaning |
|---|---|---|---|
| `categories` | `DataCategory` | yes | Classification that holds the category hierarchy as nested tags. |
| `personalData` | `PersonalData` | no (built-in) | Built-in classification with the tags `Personal` and `SpecialCategory`. |
| `pii` | `PII` | no (built-in) | Built-in classification with the tags `None`, `NonSensitive` and `Sensitive`. |
| `confidentiality` | `Confidentiality` | yes | Classification of the confidentiality levels. |
| `integrity` | `Integrity` | yes | Classification of the integrity levels. |
| `identifiability` | `Identifiability` | yes | Classification of the identifiability of personal columns. |
| `handling` | `DataHandling` | yes | Classification that holds the `Secret` tag. |
| `sensitiveFrom` | `Confidentiality.MEDIUM` | — | Lowest confidentiality of personal data tagged `PII.Sensitive`; lower levels are `PII.NonSensitive`. |

`personalData` and `pii` name OpenMetadata's built-in classifications: they are reused and never returned by
`OpenMetadataCatalog.classifications()`. The tag names inside each classification are fixed.

## Validation

The canonical constructor checks every component:

| Problem | Exception |
|---|---|
| A name is `null` | `NullPointerException` (`classification name`) |
| A name is blank or contains a dot | `IllegalArgumentException` (`Invalid OpenMetadata classification name '<name>'`) |
| Two of the seven names are equal | `IllegalArgumentException` (`Duplicate OpenMetadata classification name '<name>'`) |
| `sensitiveFrom` is `null` | `NullPointerException` (`sensitiveFrom`) |

Every name must also be a valid OpenMetadata entity name; Linneo does not check that further.

## Methods

| Method | Description |
|---|---|
| `static OpenMetadataMapping defaults()` | The defaults above. |
| `OpenMetadataMapping withCategories(String name)` | A copy with another name for the category classification. |
| `OpenMetadataMapping withConfidentiality(String name)` | A copy with another name for the confidentiality classification. |
| `OpenMetadataMapping withSensitiveFrom(Confidentiality level)` | A copy with another threshold for `PII.Sensitive`. |

The withers validate like the constructor. There are no withers for `personalData`, `pii`, `integrity`,
`identifiability` and `handling`: use the canonical constructor.

## Examples

```java
OpenMetadataMapping mapping = OpenMetadataMapping.defaults()
        .withCategories("Categoria")
        .withConfidentiality("Confidencialidad")
        .withSensitiveFrom(Confidentiality.LOW);

OpenMetadataCatalog catalog = OpenMetadataCatalog.of(Taxonomy.standard(), mapping);
catalog.tagsOf(Taxonomy.standard().requireCategory(Categories.USER_NAME));
// [Categoria.user.name, PersonalData.Personal, PII.Sensitive, Confidencialidad.Low, Integrity.Medium]
```

Renaming every Linneo classification:

```java
OpenMetadataMapping mapping = new OpenMetadataMapping("Categoria", "PersonalData", "PII", "Confidencialidad",
        "Integridad", "Identificabilidad", "Tratamiento", Confidentiality.MEDIUM);
```

See also: [OpenMetadata catalog](catalog.md) · [Protection levels](../protection-levels.md) ·
course: [OpenMetadata](../../course/07-openmetadata.md)
