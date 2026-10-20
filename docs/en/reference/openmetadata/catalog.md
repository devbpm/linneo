---
title: "OpenMetadata catalog"
parent: "Reference"
nav_order: 13
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OpenMetadataCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OmClassification.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OmTag.java
  - src/main/java/io/github/devbpm/linneo/internal/Json.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# OpenMetadata catalog

The package `io.github.devbpm.linneo.catalog.openmetadata` translates a [`Taxonomy`](../taxonomy.md) into
[OpenMetadata](https://open-metadata.org/) classifications and tags, and computes the tags of each classified column.
It makes **no network calls**: send the JSON bodies with the HTTP client you already use to
`PUT /api/v1/classifications` and `PUT /api/v1/tags`, then add the tags to the columns of each table. The bodies
follow OpenMetadata's `CreateClassification` and `CreateTag` schemas, including `parent` and `associatedTags`;
check them against the OpenMetadata version you run.

## OpenMetadataCatalog

`io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataCatalog` — `public final class`, `@since 0.1.0`

Immutable and thread-safe. There is no public constructor.

| Method | Returns | Description |
|---|---|---|
| `static OpenMetadataCatalog of(Taxonomy taxonomy)` | catalog | With the [default names](mapping.md) (`OpenMetadataMapping.defaults()`). |
| `static OpenMetadataCatalog of(Taxonomy taxonomy, OpenMetadataMapping mapping)` | catalog | With the given names and sensitivity threshold. `NullPointerException` if either argument is `null`. |
| `List<OmClassification> classifications()` | immutable list | The classifications to create, in creation order. The built-in `PersonalData` and `PII` classifications are **not** included: they are reused. |
| `List<OmTag> tags()` | list | The tags to create, in creation order: the level tags first, then one tag per data category, every parent before its children. |
| `String tagOf(DataCategory category)` | `String` | The fully qualified name of the category's tag, such as `DataCategory.user.contact.email`. |
| `List<String> tagsOf(DataCategory category)` | list | The category's own tag followed by the tags it implies (its `associatedTags`). |
| `List<String> tagsOf(FieldClassification field)` | list | The tags of a classified column: its category, whether and how it is personal data, and the field's effective protection. |

```java
Taxonomy taxonomy = Taxonomy.load();
OpenMetadataCatalog catalog = OpenMetadataCatalog.of(taxonomy);

for (OmClassification c : catalog.classifications()) {
    String body = c.toJson();          // PUT /api/v1/classifications
}
for (OmTag t : catalog.tags()) {
    String body = t.toJson();          // PUT /api/v1/tags, in this order
}

ClassClassification person = new Classifier(taxonomy).classify(Person.class);
for (FieldClassification field : person.fields()) {
    List<String> columnTags = catalog.tagsOf(field);   // add to the column mapped to the field
}
```

### Classifications created

With the default mapping, `classifications()` returns:

| `name` | `mutuallyExclusive` | `description` |
|---|---|---|
| `DataCategory` | `true` | What a column holds, from the Linneo data category taxonomy (fideslang default taxonomy plus extensions). |
| `Confidentiality` | `true` | Harm that disclosing the data would cause. |
| `Integrity` | `true` | Harm that an unwanted change to the data would cause. |
| `Identifiability` | `true` | How directly a personal data value points to the person. |
| `DataHandling` | `false` | Handling rules that apply to the data. |

### Tags created

`tags()` returns, in this order (12 + one per category; 135 for the standard taxonomy):

| Tags | Classification | `name` / `displayName` | `description` |
|---|---|---|---|
| `Public`, `Low`, `Medium`, `High` | `Confidentiality` | the level, capitalized | `Confidentiality level <Level>.` |
| `Low`, `Medium`, `High` | `Integrity` | the level, capitalized | `Integrity level <Level>.` |
| `Identified`, `Pseudonymized`, `Anonymized`, `Aggregated` | `Identifiability` | the value, capitalized | `Personal data that is <value>.` |
| `Secret` | `DataHandling` | `Secret` | `A secret that grants access: it must never be read back, exported or published, only verified.` |
| one per data category, in key order | `DataCategory` | `name`: last key segment; `displayName`: the category's name | the category's description, plus ` Deprecated.` or ` Deprecated: use <key>.` for a deprecated category |

A category tag has `parent` = `DataCategory.<parent key>` (none for a root), so the hierarchy becomes nested tags:
`user.contact.email` is the tag `DataCategory.user.contact.email`. Level tags have no parent and no associated tags.

## How tags map

| Linneo | OpenMetadata tag (default names) |
|---|---|
| Data category `<key>` | `DataCategory.<key>` |
| Nature `ORDINARY` | `PersonalData.Personal` (built-in) |
| Nature `SPECIAL_CATEGORY` or `CRIMINAL_OFFENCE` | `PersonalData.SpecialCategory` (built-in; the only OpenMetadata tag for data with reinforced protection) |
| Nature `NONE` | no `PersonalData` tag |
| Personal, confidentiality ≥ `sensitiveFrom` (default `MEDIUM`) | `PII.Sensitive` (built-in) |
| Personal, confidentiality < `sensitiveFrom` | `PII.NonSensitive` (built-in) |
| Not personal | `PII.None` (built-in) |
| Confidentiality | `Confidentiality.Public` / `.Low` / `.Medium` / `.High` |
| Integrity | `Integrity.Low` / `.Medium` / `.High` |
| Identifiability (fields only) | `Identifiability.Identified` / `.Pseudonymized` / `.Anonymized` / `.Aggregated` |
| Secret | `DataHandling.Secret` |

The tag names `Personal`, `SpecialCategory`, `None`, `NonSensitive`, `Sensitive` and `Secret` are fixed; the
classification names are configurable with [`OpenMetadataMapping`](mapping.md).

### Tags of a category

`tagsOf(DataCategory)` and a category tag's `associatedTags` are computed from the category's facets, in this order:

1. `PersonalData.Personal` or `PersonalData.SpecialCategory` — only if the category is personal;
2. `PII.None`, `PII.NonSensitive` or `PII.Sensitive`;
3. `Confidentiality.<level>`;
4. `Integrity.<level>`;
5. `DataHandling.Secret` — only if the category is secret.

`tagsOf(DataCategory)` prepends the category's own tag. For example `user.authorization.password` gives
`[DataCategory.user.authorization.password, PersonalData.Personal, PII.Sensitive, Confidentiality.High,
Integrity.High, DataHandling.Secret]`.

### Tags of a column

`tagsOf(FieldClassification)` uses the **field's** effective levels and identifiability:

1. `DataCategory.<key>`;
2. if `field.isPersonal()` (personal category stored `IDENTIFIED` or `PSEUDONYMIZED`): the `PersonalData` tag of the
   nature, then `PII.Sensitive` or `PII.NonSensitive` from the field's confidentiality; otherwise `PII.None`
   (an anonymized or aggregated personal field gets `PII.None` and no `PersonalData` tag);
3. `Confidentiality.<field level>`;
4. `Integrity.<field level>`;
5. `Identifiability.<value>` — only if the field has an identifiability (personal category);
6. `DataHandling.Secret` — only if the category is secret.

```java
@Category(value = Categories.USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED,
          confidentiality = Confidentiality.MEDIUM)
private String reference;
// tagsOf → [DataCategory.user.unique_id.pseudonymous, PersonalData.Personal, PII.Sensitive,
//           Confidentiality.Medium, Integrity.Medium, Identifiability.Pseudonymized]
```

## OmClassification

`io.github.devbpm.linneo.catalog.openmetadata.OmClassification` — `public record`, `@since 0.1.0`

```java
public record OmClassification(String name, String description, boolean mutuallyExclusive)
```

An OpenMetadata classification to create: the body of `PUT /api/v1/classifications`. `mutuallyExclusive` means an
entity can carry only one of its tags. The constructor throws `NullPointerException` if `name` or `description` is
`null`.

| Method | Description |
|---|---|
| `String toJson()` | The JSON body of a `CreateClassification` request. |

```json
{"name":"DataCategory","description":"What a column holds, from the Linneo data category taxonomy (fideslang default taxonomy plus extensions).","mutuallyExclusive":true}
```

## OmTag

`io.github.devbpm.linneo.catalog.openmetadata.OmTag` — `public record`, `@since 0.1.0`

```java
public record OmTag(String classification, String parent, String name, String displayName, String description,
                    List<String> associatedTags)
```

An OpenMetadata tag to create: the body of `PUT /api/v1/tags`.

| Component | Nullable | Description |
|---|---|---|
| `classification` | no | Name of the classification it belongs to. |
| `parent` | yes | Fully qualified name of the parent tag; `null` at the root of the classification. |
| `name` | no | The tag name, one segment without dots. |
| `displayName` | no | The human-readable name. |
| `description` | no | What it means. |
| `associatedTags` | no | Fully qualified names of tags that go with it, so whoever applies it by hand gets them too. A `null` argument becomes an empty list; the list is copied into an immutable one. |

The constructor throws `NullPointerException` if `classification`, `name`, `displayName` or `description` is `null`.

| Method | Description |
|---|---|
| `String fullyQualifiedName()` | `parent` (or `classification` when there is no parent) + `.` + `name`, such as `DataCategory.user.contact.email`. |
| `String toJson()` | The JSON body of a `CreateTag` request. |

```json
{"classification":"DataCategory","parent":"DataCategory.user.contact","name":"email","displayName":"User Contact Email","description":"User's contact email address.","associatedTags":["PersonalData.Personal","PII.NonSensitive","Confidentiality.Low","Integrity.Medium"]}
```

## JSON output

`toJson()` writes a compact JSON object (no white space) with the internal writer
`io.github.devbpm.linneo.internal.Json`, shared with the DataHub and Atlas exporters (the package is not exported), so
the library needs no JSON dependency:

- **Field order** — `OmClassification`: `name`, `description`, `mutuallyExclusive`. `OmTag`: `classification`,
  `parent`, `name`, `displayName`, `description`, `associatedTags`.
- **Omitted fields** — `OmTag` omits `parent` when it is `null` and `associatedTags` when the list is empty.
- **Types** — strings, a boolean (`mutuallyExclusive`) and an array of strings (`associatedTags`).
- **Escaping** — `"` → `\"`, `\` → `\\`, newline → `\n`, carriage return → `\r`, tab → `\t`, backspace → `\b`, form
  feed → `\f`, other control characters below U+0020 → `\u00XX`. All other characters, including non-ASCII ones, are
  written as they are (the output is a Java `String`; encode it as UTF-8 when sending it).

See also: [Mapping](mapping.md) · [Results](../classify/results.md) · [Protection levels](../protection-levels.md) ·
[Data catalogues compared](../README.md#data-catalogues-compared) · course:
[OpenMetadata](../../course/07-openmetadata.md)
