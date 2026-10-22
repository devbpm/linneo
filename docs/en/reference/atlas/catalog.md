---
title: "Apache Atlas catalog"
parent: "Reference"
nav_order: 17
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasClassificationDef.java
  - src/main/java/io/github/devbpm/linneo/internal/Json.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# Apache Atlas catalog

The package `io.github.devbpm.linneo.catalog.atlas` translates a [`Taxonomy`](../taxonomy.md) into
[Apache Atlas](https://atlas.apache.org/) **classification types** and computes the classifications of each
classified column. It makes **no network calls**: send the type definitions with the HTTP client you already use to
`POST /api/atlas/v2/types/typedefs`, then add the classifications to the column entities with
`POST /api/atlas/v2/entity/guid/{guid}/classifications`. The bodies were checked against Apache Atlas 2.3.0;
classification names with dots are valid since Atlas 1.0 and the API is unchanged up to 2.5.0.

The repository has an integration test, `AtlasIT`, that sends these bodies to a real server; see "Integration tests
against catalog servers" in `CONTRIBUTING.md`.

How this exporter compares with the OpenMetadata and DataHub ones: see
[Data catalogues compared](../README.md#data-catalogues-compared).

## AtlasCatalog

`io.github.devbpm.linneo.catalog.atlas.AtlasCatalog` — `public final class`, `@since 0.1.0`

Immutable and thread-safe. There is no public constructor.

| Method | Returns | Description |
|---|---|---|
| `static AtlasCatalog of(Taxonomy taxonomy)` | catalog | With the [default names](mapping.md) (`AtlasMapping.defaults()`). |
| `static AtlasCatalog of(Taxonomy taxonomy, AtlasMapping mapping)` | catalog | With the given root names. `NullPointerException` if either argument is `null`. |
| `List<AtlasClassificationDef> classificationDefs()` | new mutable list | The classification types to create, in creation order (see [Classification types](#classification-types)). |
| `String typeDefsJson()` | `String` | The body of `POST /api/atlas/v2/types/typedefs` that creates every classification type at once: `{"classificationDefs":[…]}`. |
| `String classificationOf(DataCategory category)` | `String` | The name of the category's classification, such as `DataCategory.user.contact.email`. |
| `List<String> classificationsOf(DataCategory category)` | list | The category's own classification followed by those of the protection it implies. |
| `List<String> classificationsOf(FieldClassification field)` | list | The classifications of a classified column: its category, its legal nature when it is personal data, and the field's effective protection. |
| `String classificationsJson(FieldClassification field)` | `String` | The body of `POST /api/atlas/v2/entity/guid/{guid}/classifications` for that column: a JSON array of `{"typeName": …}` objects. |

```java
Taxonomy taxonomy = Taxonomy.load();
AtlasCatalog catalog = AtlasCatalog.of(taxonomy);

String typeDefs = catalog.typeDefsJson();   // POST /api/atlas/v2/types/typedefs

ClassClassification person = new Classifier(taxonomy).classify(Person.class);
for (FieldClassification field : person.fields()) {
    String body = catalog.classificationsJson(field);
    // POST /api/atlas/v2/entity/guid/{guid}/classifications, with the guid of the column of the field
}
```

## Classification types

### The category tree as super types

Every category is a classification named after the root plus the key, whose **super type is its parent's
classification**; root categories have the super type `DataCategory`, a root classification of its own:

```text
DataCategory
└── DataCategory.user                      superTypes: [DataCategory]
    └── DataCategory.user.contact          superTypes: [DataCategory.user]
        └── DataCategory.user.contact.email   superTypes: [DataCategory.user.contact]
```

In Atlas an entity with a classification also matches its super types, so a search for
`DataCategory.user.health_and_medical` finds the columns classified `DataCategory.user.health_and_medical.disability`
too, and a search for `DataCategory.user` finds every column holding user data.

### Protection levels as separate classifications

The protection levels are classifications of their own, each a sub-type of its root:

| Classifications | Super type | `description` |
|---|---|---|
| `PersonalData.Personal`, `.SpecialCategory`, `.CriminalOffence` | `PersonalData` | `Personal data (GDPR article 4.1).`, `Special category of personal data (GDPR article 9).`, `Personal data relating to criminal convictions and offences (GDPR article 10).` |
| `Confidentiality.Public`, `.Low`, `.Medium`, `.High` | `Confidentiality` | `Confidentiality level <Level>.` |
| `Integrity.Low`, `.Medium`, `.High` | `Integrity` | `Integrity level <Level>.` |
| `Identifiability.Identified`, `.Pseudonymized`, `.Anonymized`, `.Aggregated` | `Identifiability` | `Personal data that is <value>.` |
| `DataHandling.Secret` | `DataHandling` | `A secret that grants access: it must never be read back, exported or published, only verified.` |

A column carries them **next to** its category classification. They are deliberately **not super types of the
categories**, for two reasons:

- **A child category may raise a level its parent implies.** `user` is `Confidentiality.Low`, but
  `user.health_and_medical` is `High`. If `DataCategory.user` had the super type `Confidentiality.Low`, every
  category below it would inherit `Low`, and a stricter child would be both `Low` and `High`. Atlas inheritance
  cannot be undone further down; Linneo's protection inheritance can be tightened.
- **Tag-based policies act on them directly.** Policy engines that read Atlas classifications, such as Apache
  Ranger's tag-based policies, match the classifications an entity carries. A policy on `Confidentiality.High` or
  `PersonalData.SpecialCategory` then covers every column that carries it, whatever its category.

### Creation order and descriptions

`classificationDefs()` returns, in this order (146 types for the standard taxonomy):

1. the six roots, without super types: `PersonalData`, `Confidentiality`, `Integrity`, `Identifiability`,
   `DataHandling`, `DataCategory` (6);
2. the level classifications of the table above (15);
3. one classification per category, in key order, every parent before its children (125).

Every super type is defined before the types that name it. A category's description is its name, a colon and its
description, plus ` Deprecated.` or ` Deprecated: use <key>.` for a deprecated category (Atlas classifications have
no separate display name):

```json
{"name":"DataCategory.user.contact.email","description":"User Contact Email: User's contact email address.","superTypes":["DataCategory.user.contact"],"attributeDefs":[]}
```

The roots' descriptions are those of the [DataHub nodes](../datahub/catalog.md#glossary-structure), except
`DataCategory`: `What a column holds, from the Linneo data category taxonomy (fideslang default taxonomy plus
extensions).`

### Sending the type definitions

`typeDefsJson()` wraps every definition in one body:

```json
{"classificationDefs":[{"name":"PersonalData","description":"Legal nature of personal data under the GDPR.","superTypes":[],"attributeDefs":[]},{"name":"Confidentiality","description":"Harm that disclosing the data would cause.","superTypes":[],"attributeDefs":[]}, …]}
```

`POST /api/atlas/v2/types/typedefs` **creates** types and answers 409 when they already exist. On later runs —
for instance after adding terms to the taxonomy — send the same body with `PUT /api/atlas/v2/types/typedefs` to
update them. To send only what is new, list the existing types at `GET /api/atlas/v2/types/typedefs/headers` and
build the bodies from `classificationDefs()`.

## Classifications of a category

`classificationsOf(DataCategory)` is the category's classification followed by, in this order:

1. `PersonalData.Personal`, `PersonalData.SpecialCategory` or `PersonalData.CriminalOffence` — only if the
   category is personal;
2. `Confidentiality.<level>`;
3. `Integrity.<level>`;
4. `DataHandling.Secret` — only if the category is secret.

For example `user.authorization.password` gives `[DataCategory.user.authorization.password, PersonalData.Personal,
Confidentiality.High, Integrity.High, DataHandling.Secret]`.

## Classifications of a column

`classificationsOf(FieldClassification)` uses the **field's** effective levels and identifiability:

1. `DataCategory.<key>`;
2. the `PersonalData` classification of the nature — only if `field.isPersonal()` (personal category stored
   `IDENTIFIED` or `PSEUDONYMIZED`; an anonymized or aggregated personal field gets none);
3. `Confidentiality.<field level>`;
4. `Integrity.<field level>`;
5. `Identifiability.<value>` — only if the field has an identifiability (personal category);
6. `DataHandling.Secret` — only if the field is secret.

`classificationsJson(field)` writes the same list as the body that adds them to the column entity:

```json
[{"typeName":"DataCategory.user.contact.email"},{"typeName":"PersonalData.Personal"},{"typeName":"Confidentiality.Low"},{"typeName":"Integrity.Medium"},{"typeName":"Identifiability.Identified"}]
```

**Linneo does not find the column entity.** Map each field to its column (your persistence naming rules) and look up
its `guid` in Atlas, for instance with `GET /api/atlas/v2/entity/uniqueAttribute/type/<column type>` and the
column's `qualifiedName`; then `POST` the body to `/api/atlas/v2/entity/guid/{guid}/classifications`. Atlas rejects
a classification the entity already has directly, so on later runs remove or update the existing ones first.

### Propagation

The body sets no `propagate` flag, so **Atlas applies its default propagation**: in a stock installation
classifications propagate along lineage to the entities derived from the column. Review what that means for your
lineage: a column computed from personal columns (an average, a count) inherits their `PersonalData.*` and
`Identifiability.Identified` classifications next to its own. If you need another behaviour, add
`"propagate": false` (or the propagation options your Atlas version supports) to the objects before sending them.

## AtlasClassificationDef

`io.github.devbpm.linneo.catalog.atlas.AtlasClassificationDef` — `public record`, `@since 0.1.0`

```java
public record AtlasClassificationDef(String name, String description, List<String> superTypes)
```

An Atlas classification type to create: one element of `classificationDefs` in `POST /api/atlas/v2/types/typedefs`.

| Component | Nullable | Description |
|---|---|---|
| `name` | no | The classification name, such as `DataCategory.user.contact.email`. |
| `description` | no | What it means. |
| `superTypes` | yes | The classifications it specializes; an entity with this classification also matches them. A `null` argument becomes an empty list; the list is copied into an immutable one. |

The constructor throws `NullPointerException` if `name` or `description` is `null`.

| Method | Description |
|---|---|
| `String toJson()` | The JSON of the classification definition, with an empty `attributeDefs`. |

```json
{"name":"Confidentiality.High","description":"Confidentiality level High.","superTypes":["Confidentiality"],"attributeDefs":[]}
```

## JSON output

The bodies are compact JSON (no white space) written by the internal writer `io.github.devbpm.linneo.internal.Json`,
shared by all the exporters (the package is not exported):

- **Member order** — a definition: `name`, `description`, `superTypes`, `attributeDefs`. A classification:
  `typeName`.
- **Omitted members** — none: `superTypes` and `attributeDefs` are written as `[]` when empty.
- **Escaping** — as in the [OpenMetadata catalog](../openmetadata/catalog.md#json-output).

See also: [Mapping](mapping.md) · [Results](../classify/results.md) · [Protection levels](../protection-levels.md) ·
[Data catalogues compared](../README.md#data-catalogues-compared) · course:
[DataHub and Apache Atlas](../../course/08-datahub-and-atlas.md)
