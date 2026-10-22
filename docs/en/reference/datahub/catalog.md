---
title: "DataHub catalog"
parent: "Reference"
nav_order: 15
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DataHubCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DhProposal.java
  - src/main/java/io/github/devbpm/linneo/internal/Json.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# DataHub catalog

The package `io.github.devbpm.linneo.catalog.datahub` translates a [`Taxonomy`](../taxonomy.md) into a
[DataHub](https://datahub.com/) **business glossary** (glossary nodes and glossary terms) and computes the terms of
each classified field. It makes **no network calls**: send each proposal with the HTTP client you already use to
`POST /aspects?action=ingestProposal` of the DataHub metadata service (GMS), then add the terms to the fields of
each dataset. The aspects follow DataHub's `glossaryNodeInfo`, `glossaryTermInfo` and `glossaryRelatedTerms`
schemas. Send every proposal with the header `X-RestLi-Protocol-Version: 2.0.0`. They were checked against DataHub
v1.7.0.1, and the aspects use only fields present since v0.9.2.

The repository has an integration test, `DataHubIT`, that sends the proposals to a real GMS; see "Integration tests
against catalog servers" in `CONTRIBUTING.md`.

How this exporter compares with the OpenMetadata and Atlas ones: see
[Data catalogues compared](../README.md#data-catalogues-compared).

## DataHubCatalog

`io.github.devbpm.linneo.catalog.datahub.DataHubCatalog` — `public final class`, `@since 0.1.0`

Immutable and thread-safe. There is no public constructor.

| Method | Returns | Description |
|---|---|---|
| `static DataHubCatalog of(Taxonomy taxonomy)` | catalog | With the [default node names](mapping.md) (`DataHubMapping.defaults()`). |
| `static DataHubCatalog of(Taxonomy taxonomy, DataHubMapping mapping)` | catalog | With the given node names. `NullPointerException` if either argument is `null`. |
| `List<DhProposal> proposals()` | new mutable list | The proposals that create the glossary, in the order to send them (see [Proposal order](#proposal-order)). |
| `String termOf(DataCategory category)` | `String` | The URN of the category's term, such as `urn:li:glossaryTerm:DataCategory.user.contact.email`. |
| `List<String> termsOf(DataCategory category)` | list | The category's own term followed by the terms of the protection it implies (its `relatedTerms`). |
| `List<String> termsOf(FieldClassification field)` | list | The terms of a classified field: its category, its legal nature when it is personal data, and the field's effective protection. |

```java
Taxonomy taxonomy = Taxonomy.load();
DataHubCatalog catalog = DataHubCatalog.of(taxonomy);

for (DhProposal proposal : catalog.proposals()) {
    String body = proposal.toJson();   // POST /aspects?action=ingestProposal, in this order
}

ClassClassification person = new Classifier(taxonomy).classify(Person.class);
for (FieldClassification field : person.fields()) {
    List<String> fieldTerms = catalog.termsOf(field);   // add to the dataset field mapped to the field
}
```

## Glossary structure

With the default mapping the glossary has six root nodes:

| Node | `name` | `definition` | Holds |
|---|---|---|---|
| `PersonalData` | `Personal Data` | Legal nature of personal data under the GDPR. | the terms `Personal`, `SpecialCategory`, `CriminalOffence` |
| `Confidentiality` | `Confidentiality` | Harm that disclosing the data would cause. | the terms `Public`, `Low`, `Medium`, `High` |
| `Integrity` | `Integrity` | Harm that an unwanted change to the data would cause. | the terms `Low`, `Medium`, `High` |
| `Identifiability` | `Identifiability` | How directly a personal data value points to the person. | the terms `Identified`, `Pseudonymized`, `Anonymized`, `Aggregated` |
| `DataHandling` | `Data Handling` | Handling rules that apply to the data. | the term `Secret` |
| `DataCategory` | `Data Category` | What a field holds, from the Linneo data category taxonomy (fideslang default taxonomy plus extensions). | the category tree |

### Categories: nodes and terms that mirror the tree

**Every category is a glossary term** whose URN is the node name plus the key: `user.contact.email` is
`urn:li:glossaryTerm:DataCategory.user.contact.email`.

**Every category with children also gets a glossary node** of the same name and definition, with the same id:
`urn:li:glossaryNode:DataCategory.user.contact`. That node holds the category's **own term** and the nodes and terms
of its children, so the glossary browses like the taxonomy tree. A category without children (a leaf) is just a term
in its parent's node, and a root category's node sits in the `DataCategory` node:

```text
DataCategory                                  (node)
└── DataCategory.user                         (node "User Data")
    ├── DataCategory.user                     (term "User Data")
    └── DataCategory.user.contact             (node "Contact Data")
        ├── DataCategory.user.contact         (term "Contact Data")
        ├── DataCategory.user.contact.email   (term "User Contact Email")
        ├── DataCategory.user.contact.address (node "User Contact Address")
        │   └── …
        └── …
```

A node and a term may share an id because they are different entity types (`glossaryNode` and `glossaryTerm`).
For the standard taxonomy, 22 of the 125 categories have children, so the glossary has 6 + 22 = 28 nodes and
15 + 125 = 140 terms.

### Relations: `isRelatedTerms` and `relatedTerms`

Each category term gets a `glossaryRelatedTerms` aspect:

- `isRelatedTerms` (DataHub's *Inherits* / "is a" relation): the term of its **parent category**. Root categories
  (`user`, `system`, …) have none, and the member is then omitted.
- `relatedTerms` (DataHub's *Related to* relation): the terms of the **protection the category implies**, in this
  order:
  1. `PersonalData.Personal`, `PersonalData.SpecialCategory` or `PersonalData.CriminalOffence` — only if the
     category is personal;
  2. `Confidentiality.<level>`;
  3. `Integrity.<level>`;
  4. `DataHandling.Secret` — only if the category is secret.

```json
{"isRelatedTerms":["urn:li:glossaryTerm:DataCategory.user.contact"],"relatedTerms":["urn:li:glossaryTerm:PersonalData.Personal","urn:li:glossaryTerm:Confidentiality.Low","urn:li:glossaryTerm:Integrity.Medium"]}
```

These relations document the taxonomy in the glossary; they do not tag anything. The terms of a field are always
listed explicitly by [`termsOf(field)`](#terms-of-a-field).

### Term info: definition, custom properties and source

The `glossaryTermInfo` of a **category term** holds:

| Member | Value |
|---|---|
| `customProperties` | `linneo.key`, `linneo.origin`, `linneo.nature`, `linneo.confidentiality`, `linneo.integrity` (enum names, such as `MEDIUM`), `linneo.secret` (`"true"` or `"false"`), then the category's [attributes](../terms.md#datacategory) sorted by key (an attribute with one of the `linneo.*` names is ignored: it cannot override that value), so the output is the same on every run |
| `name` | the category's name |
| `definition` | the category's description, plus ` Deprecated.` or ` Deprecated: use <key>.` for a deprecated category |
| `parentNode` | the category's own node if it has children; otherwise its parent's node (`DataCategory` for a root leaf) |
| `termSource` | `EXTERNAL` for a term whose origin is `fideslang`, with `sourceRef` `fideslang` and `sourceUrl` `https://github.com/ethyca/fideslang`; `INTERNAL` for every other origin (`linneo`, an extension name, `application`) |

```json
{"customProperties":{"linneo.key":"user.contact.email","linneo.origin":"fideslang","linneo.nature":"ORDINARY","linneo.confidentiality":"LOW","linneo.integrity":"MEDIUM","linneo.secret":"false"},"name":"User Contact Email","definition":"User's contact email address.","parentNode":"urn:li:glossaryNode:DataCategory.user.contact","termSource":"EXTERNAL","sourceRef":"fideslang","sourceUrl":"https://github.com/ethyca/fideslang"}
```

```json
{"customProperties":{"linneo.key":"user.education.grade","linneo.origin":"linneo","linneo.nature":"ORDINARY","linneo.confidentiality":"MEDIUM","linneo.integrity":"HIGH","linneo.secret":"false"},"name":"Grade","definition":"Marks, grades or assessment results obtained by the subject.","parentNode":"urn:li:glossaryNode:DataCategory.user.education","termSource":"INTERNAL"}
```

A **level term** (`Confidentiality.High`, `PersonalData.SpecialCategory`, …) has empty `customProperties`, the
level as `name`, `termSource` `INTERNAL` and no relations:

| Terms | Node | `definition` |
|---|---|---|
| `Personal`, `SpecialCategory`, `CriminalOffence` | `PersonalData` | `Personal data (GDPR article 4.1).`, `Special category of personal data (GDPR article 9).`, `Personal data relating to criminal convictions and offences (GDPR article 10).` |
| `Public`, `Low`, `Medium`, `High` | `Confidentiality` | `Confidentiality level <Level>.` |
| `Low`, `Medium`, `High` | `Integrity` | `Integrity level <Level>.` |
| `Identified`, `Pseudonymized`, `Anonymized`, `Aggregated` | `Identifiability` | `Personal data that is <value>.` |
| `Secret` | `DataHandling` | `A secret that grants access: it must never be read back, exported or published, only verified.` |

A **node** has empty `customProperties`, a `name`, a `definition` and, except for the six root nodes, a
`parentNode`:

```json
{"customProperties":{},"name":"Contact Data","definition":"Contact data collected about a user.","parentNode":"urn:li:glossaryNode:DataCategory.user"}
```

### Proposal order

`proposals()` returns, in this order (293 proposals for the standard taxonomy):

1. a `glossaryNodeInfo` for each of the six root nodes, then for each category with children, in key order, so
   that every node comes after its parent node (28);
2. a `glossaryTermInfo` for each level term (15), then for each category, in key order (125);
3. a `glossaryRelatedTerms` for each category, in key order (125).

Send them in this order: every node exists before the nodes and terms it holds, and every term exists before a
relation points to it. Each proposal is an `UPSERT`, so sending the list again updates the glossary in place — and
overwrites the whole aspect, including any change made by hand in the DataHub UI to those terms.

## Terms of a category

`termsOf(DataCategory)` is the category's term followed by its `relatedTerms`. For example
`user.authorization.password` gives
`[urn:li:glossaryTerm:DataCategory.user.authorization.password, urn:li:glossaryTerm:PersonalData.Personal,
urn:li:glossaryTerm:Confidentiality.High, urn:li:glossaryTerm:Integrity.High, urn:li:glossaryTerm:DataHandling.Secret]`.

## Terms of a field

`termsOf(FieldClassification)` uses the **field's** effective levels and identifiability:

1. `DataCategory.<key>`;
2. the `PersonalData` term of the nature — only if `field.isPersonal()` (personal category stored `IDENTIFIED` or
   `PSEUDONYMIZED`; an anonymized or aggregated personal field gets none);
3. `Confidentiality.<field level>`;
4. `Integrity.<field level>`;
5. `Identifiability.<value>` — only if the field has an identifiability (personal category);
6. `DataHandling.Secret` — only if the field is secret.

```java
@Category(value = Categories.USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED,
          confidentiality = Confidentiality.MEDIUM)
private String reference;
// termsOf → [urn:li:glossaryTerm:DataCategory.user.unique_id.pseudonymous, urn:li:glossaryTerm:PersonalData.Personal,
//            urn:li:glossaryTerm:Confidentiality.Medium, urn:li:glossaryTerm:Integrity.Medium,
//            urn:li:glossaryTerm:Identifiability.Pseudonymized]
```

### Applying the terms to dataset fields

**Linneo does not do this step.** Map each field to its dataset and field path (your persistence naming rules), then
add the terms with the DataHub API you already use. With GraphQL, the `addTerms` mutation adds terms to a field
without removing the others; the field path goes in `subResource`:

```graphql
mutation {
  addTerms(input: {
    termUrns: ["urn:li:glossaryTerm:DataCategory.user.contact.email",
               "urn:li:glossaryTerm:PersonalData.Personal",
               "urn:li:glossaryTerm:Confidentiality.Low",
               "urn:li:glossaryTerm:Integrity.Medium",
               "urn:li:glossaryTerm:Identifiability.Identified"],
    resourceUrn: "urn:li:dataset:(urn:li:dataPlatform:postgres,university.public.student,PROD)",
    subResourceType: DATASET_FIELD,
    subResource: "email"
  })
}
```

The field path is the one DataHub shows for the field (for relational sources usually the column name).

## DhProposal

`io.github.devbpm.linneo.catalog.datahub.DhProposal` — `public record`, `@since 0.1.0`

```java
public record DhProposal(String entityType, String entityUrn, String aspectName, String aspect)
```

A DataHub metadata change proposal that upserts one aspect of one entity: the body of
`POST /aspects?action=ingestProposal`.

| Component | Description |
|---|---|
| `entityType` | `glossaryNode` or `glossaryTerm`. |
| `entityUrn` | The entity's URN, such as `urn:li:glossaryTerm:DataCategory.user.contact.email`. |
| `aspectName` | `glossaryNodeInfo`, `glossaryTermInfo` or `glossaryRelatedTerms`. |
| `aspect` | The aspect as a JSON object (see [Glossary structure](#glossary-structure)). |

The constructor throws `NullPointerException` if any component is `null`.

| Method | Description |
|---|---|
| `String toJson()` | The JSON body of an `ingestProposal` request, with `changeType` `UPSERT`. The aspect travels **JSON-encoded as a string** in `aspect.value`, with `contentType` `application/json`, as the API requires. |

```json
{"proposal":{"entityType":"glossaryTerm","entityUrn":"urn:li:glossaryTerm:Confidentiality.High","changeType":"UPSERT","aspectName":"glossaryTermInfo","aspect":{"value":"{\"customProperties\":{},\"name\":\"High\",\"definition\":\"Confidentiality level High.\",\"parentNode\":\"urn:li:glossaryNode:Confidentiality\",\"termSource\":\"INTERNAL\"}","contentType":"application/json"}}}
```

Send it with `Content-Type: application/json` and, for the Rest.li endpoint of GMS, the header
`X-RestLi-Protocol-Version: 2.0.0`, plus your access token. Behind the DataHub frontend the same endpoint is usually
`/api/gms/aspects?action=ingestProposal`.

## JSON output

The aspects and `toJson()` are compact JSON (no white space) written by the internal writer
`io.github.devbpm.linneo.internal.Json`, shared by all the exporters (the package is not exported):

- **Member order** — `glossaryNodeInfo`: `customProperties`, `name`, `definition`, `parentNode`.
  `glossaryTermInfo`: `customProperties`, `name`, `definition`, `parentNode`, `termSource`, `sourceRef`,
  `sourceUrl`. `glossaryRelatedTerms`: `isRelatedTerms`, `relatedTerms`.
- **Omitted members** — `parentNode` on the six root nodes; `sourceRef` and `sourceUrl` on `INTERNAL` terms;
  `isRelatedTerms` on root categories.
- **Escaping** — as in the [OpenMetadata catalog](../openmetadata/catalog.md#json-output). The aspect string inside
  `toJson()` is escaped a second time.

See also: [Mapping](mapping.md) · [Results](../classify/results.md) · [Protection levels](../protection-levels.md) ·
[Data catalogues compared](../README.md#data-catalogues-compared) · course:
[DataHub and Apache Atlas](../../course/08-datahub-and-atlas.md)
