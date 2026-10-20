---
title: "8. DataHub and Apache Atlas"
parent: "Course"
nav_order: 8
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DataHubCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DataHubMapping.java
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DhProposal.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasMapping.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasClassificationDef.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# Module 8 — DataHub and Apache Atlas

## Learning objectives

After this module you will be able to:

- explain how Linneo's taxonomy becomes a DataHub business glossary (nodes, terms and their relations) and Apache
  Atlas classification types (super types and separate protection levels);
- generate the DataHub proposals and the Atlas type definitions, and send them with your own HTTP client;
- compute the terms and classifications of each column from its `FieldClassification`, and know how to apply them;
- tell what each of the three catalogues represents differently, and rename the roots with `DataHubMapping` and
  `AtlasMapping`.

Module 7 is not required, but this module compares against it often.

## One taxonomy, three catalogues

The university may not run OpenMetadata. [DataHub](https://datahub.com/) and [Apache Atlas](https://atlas.apache.org/)
are two other open source catalogues, and Linneo has an exporter for each, next to the OpenMetadata one:

| Catalogue | What a category becomes | Package |
|---|---|---|
| OpenMetadata | a tag in a classification | `io.github.devbpm.linneo.catalog.openmetadata` |
| DataHub | a **glossary term** in a tree of **glossary nodes** | `io.github.devbpm.linneo.catalog.datahub` |
| Apache Atlas | a **classification type** that inherits from its parent's | `io.github.devbpm.linneo.catalog.atlas` |

The three work the same way: an immutable catalog object built from a `Taxonomy`, a list of things to create in
creation order, and a method that computes the labels of a column from its `FieldClassification`. None of them makes
network calls. The reference has a full [comparison table](../reference/README.md#data-catalogues-compared).

Two differences matter from the start:

- DataHub and Atlas have no built-in `PersonalData` or `PII` classifications to reuse, so Linneo creates its own
  `PersonalData` root, and it keeps the three natures apart: `Personal`, `SpecialCategory` and **`CriminalOffence`**.
  (OpenMetadata only has `SpecialCategory` for both art. 9 and art. 10 data.) There is no `PII.*` sensitivity label.
- The level names are the same everywhere: `Confidentiality.High`, `Integrity.Medium`, `Identifiability.Aggregated`,
  `DataHandling.Secret`.

## DataHub: a business glossary

### Nodes and terms

DataHub organises a glossary into **nodes** (folders) holding **terms**. Linneo creates six root nodes —
`PersonalData`, `Confidentiality`, `Integrity`, `Identifiability`, `DataHandling` and `DataCategory` — with the level
terms in the first five and the category tree in the last one.

Every category is a term: `user.education.grade` is `urn:li:glossaryTerm:DataCategory.user.education.grade`. A
category **with children** also gets a node with the same id, and that node holds the category's **own term** next to
its children, so the glossary browses like the taxonomy:

```text
DataCategory                                   (node)
└── DataCategory.user                          (node "User Data")
    ├── DataCategory.user                      (term "User Data")
    ├── DataCategory.user.education            (node "Education Data")
    │   ├── DataCategory.user.education        (term "Education Data")
    │   ├── DataCategory.user.education.grade  (term "Grade")
    │   ├── DataCategory.user.education.status (term "Academic Status")
    │   └── …
    └── …
```

Each category term is also **related** to other terms:

- `isRelatedTerms` (DataHub's "inherits") points to the term of its parent category: `Grade` inherits from
  `Education Data`;
- `relatedTerms` points to the protection it implies: `Grade` is related to `PersonalData.Personal`,
  `Confidentiality.Medium` and `Integrity.High`.

And each category term records its key, origin and protection in **custom properties**, so the facts behind the term
are visible in DataHub. Terms that come from fideslang are marked as external, with fideslang as the source; Linneo's
and your own are internal.

### The proposals

```java
DataHubCatalog catalog = DataHubCatalog.of(Taxonomy.load());
List<DhProposal> proposals = catalog.proposals();
```

DataHub is written one **aspect** at a time. A `DhProposal` upserts one aspect of one entity: `entityType`
(`glossaryNode` or `glossaryTerm`), `entityUrn`, `aspectName` and the `aspect` as JSON. For the standard taxonomy
`proposals()` returns 289 of them, in an order you can send as is:

1. 28 `glossaryNodeInfo` — the six roots, then the 22 categories with children, every node after its parent;
2. 138 `glossaryTermInfo` — the 15 level terms, then the 123 categories;
3. 123 `glossaryRelatedTerms` — the relations of every category term.

The `glossaryTermInfo` of `Grade`:

```json
{"customProperties":{"linneo.key":"user.education.grade","linneo.origin":"linneo","linneo.nature":"ORDINARY","linneo.confidentiality":"MEDIUM","linneo.integrity":"HIGH","linneo.secret":"false"},"name":"Grade","definition":"Marks, grades or assessment results obtained by the subject.","parentNode":"urn:li:glossaryNode:DataCategory.user.education","termSource":"INTERNAL"}
```

and its `glossaryRelatedTerms`:

```json
{"isRelatedTerms":["urn:li:glossaryTerm:DataCategory.user.education"],"relatedTerms":["urn:li:glossaryTerm:PersonalData.Personal","urn:li:glossaryTerm:Confidentiality.Medium","urn:li:glossaryTerm:Integrity.High"]}
```

`toJson()` wraps the aspect in the body of DataHub's `ingestProposal` action. Notice that the aspect travels as a
**JSON-encoded string** inside `aspect.value`, which is what the API expects:

```json
{"proposal":{"entityType":"glossaryTerm","entityUrn":"urn:li:glossaryTerm:Confidentiality.High","changeType":"UPSERT","aspectName":"glossaryTermInfo","aspect":{"value":"{\"customProperties\":{},\"name\":\"High\",\"definition\":\"Confidentiality level High.\",\"parentNode\":\"urn:li:glossaryNode:Confidentiality\",\"termSource\":\"INTERNAL\"}","contentType":"application/json"}}}
```

### Sending the proposals

As with OpenMetadata, **Linneo makes no HTTP calls**. A minimal sender with the JDK's `HttpClient`:

```java
package edu.example.university;

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.catalog.datahub.DataHubCatalog;
import io.github.devbpm.linneo.catalog.datahub.DhProposal;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class PublishToDataHub {

    public static void main(String[] args) throws IOException, InterruptedException {
        URI ingest = URI.create("https://datahub.example.edu:8080/aspects?action=ingestProposal");
        String token = System.getenv("DATAHUB_TOKEN");
        HttpClient http = HttpClient.newHttpClient();
        DataHubCatalog catalog = DataHubCatalog.of(Taxonomy.load());

        for (DhProposal proposal : catalog.proposals()) {   // nodes, then terms, then relations
            HttpRequest request = HttpRequest.newBuilder(ingest)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("X-RestLi-Protocol-Version", "2.0.0")
                    .POST(HttpRequest.BodyPublishers.ofString(proposal.toJson()))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IOException(proposal.entityUrn() + " " + proposal.aspectName() + " failed: "
                        + response.statusCode() + " " + response.body());
            }
        }
    }
}
```

The URL is the DataHub metadata service (GMS); behind the DataHub frontend it is usually
`/api/gms/aspects?action=ingestProposal`. Every proposal is an `UPSERT`, so you can run the publisher again after
changing the taxonomy — but it replaces the whole aspect each time, so do not curate Linneo's terms by hand in the
DataHub UI.

### The terms of a column

`termsOf(FieldClassification)` gives the term URNs of a column, from the **field's** effective classification:

```java
Taxonomy taxonomy = Taxonomy.load();
DataHubCatalog catalog = DataHubCatalog.of(taxonomy);
ClassClassification student = new Classifier(taxonomy).classify(Student.class);

Map<String, List<String>> termsByField = student.fields().stream()
        .collect(Collectors.toMap(FieldClassification::field, catalog::termsOf));
```

In the table each URN is shortened: they all start with `urn:li:glossaryTerm:`.

| Field | Terms |
|---|---|
| `Student.email` | `DataCategory.user.contact.email`, `PersonalData.Personal`, `Confidentiality.Low`, `Integrity.Medium`, `Identifiability.Identified` |
| `Student.disabilityDegree` | `DataCategory.user.health_and_medical.disability`, `PersonalData.SpecialCategory`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified` |
| `Student.analyticsId` | `DataCategory.user.unique_id.pseudonymous`, `PersonalData.Personal`, `Confidentiality.Low`, `Integrity.Medium`, `Identifiability.Pseudonymized` |
| `Student.counsellingNotes` | `DataCategory.user.content.private`, `PersonalData.Personal`, `Confidentiality.High`, `Integrity.Medium`, `Identifiability.Identified` |
| `Staff.passwordHash` | `DataCategory.user.authorization.password`, `PersonalData.Personal`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified`, `DataHandling.Secret` |
| `Staff.sexOffenceCertificateClear` | `DataCategory.user.criminal_history`, `PersonalData.CriminalOffence`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified` |
| `CourseStatistics.averageMark` | `DataCategory.user.education.grade`, `Confidentiality.Medium`, `Integrity.High`, `Identifiability.Aggregated` |
| `MenuGroup.code` | `DataCategory.system.configuration`, `Confidentiality.Low`, `Integrity.Medium` |

The rules are those of module 7 without the `PII.*` tags: `counsellingNotes` is `High` because of the level raised on
the field, `averageMark` gets no `PersonalData` term because aggregated values are not personal data, and
non-personal fields have no `Identifiability` term.

**Attaching the terms to the dataset fields is your integration's job.** Map each field to its dataset and field
path, then use the DataHub API you already use. With GraphQL, `addTerms` adds terms to one field of a dataset, given
as `subResource`, without removing the others:

```graphql
mutation {
  addTerms(input: {
    termUrns: ["urn:li:glossaryTerm:DataCategory.user.health_and_medical.disability",
               "urn:li:glossaryTerm:PersonalData.SpecialCategory",
               "urn:li:glossaryTerm:Confidentiality.High",
               "urn:li:glossaryTerm:Integrity.High",
               "urn:li:glossaryTerm:Identifiability.Identified"],
    resourceUrn: "urn:li:dataset:(urn:li:dataPlatform:postgres,university.public.student,PROD)",
    subResourceType: DATASET_FIELD,
    subResource: "disability_degree"
  })
}
```

## Apache Atlas: classification types

### The tree as inheritance

Atlas has **classifications**: types that you define once and then attach to entities such as columns. A
classification type can have **super types**, and an entity with a classification also matches its super types.
Linneo turns the category tree into that inheritance: `DataCategory.user.education.grade` has the super type
`DataCategory.user.education`, which has `DataCategory.user`, which has the root `DataCategory`. A search for
`DataCategory.user.education` therefore finds every column with any education data.

The protection levels are **separate classifications** (`PersonalData.SpecialCategory`, `Confidentiality.High`,
`DataHandling.Secret`…) that a column carries **next to** its category. Why not make `Confidentiality.Medium` a super
type of `DataCategory.user.education`? Because a child category may raise a level its parent implies — `user` is
`Low`, `user.health_and_medical` is `High` — and Atlas inheritance cannot be taken back further down the tree. Keeping
the levels apart also lets tag-based access policies, such as those of Apache Ranger, act on
`Confidentiality.High` or `PersonalData.SpecialCategory` directly, whatever the category.

### The type definitions

```java
AtlasCatalog atlas = AtlasCatalog.of(Taxonomy.load());
String typeDefs = atlas.typeDefsJson();
```

`classificationDefs()` lists 144 types for the standard taxonomy — six roots, 15 levels, 123 categories, every super
type first — and `typeDefsJson()` puts them all in one body for `POST /api/atlas/v2/types/typedefs`. The definition
of `Grade`:

```json
{"name":"DataCategory.user.education.grade","description":"Grade: Marks, grades or assessment results obtained by the subject.","superTypes":["DataCategory.user.education"],"attributeDefs":[]}
```

Atlas classifications have no display name, so the description starts with the category's name.

### The classifications of a column

`classificationsOf(FieldClassification)` gives the same labels as DataHub, as plain names — the DataHub term ids
without `urn:li:glossaryTerm:`. `classificationsJson(field)` writes them as the body that attaches them to a column:

```java
String body = atlas.classificationsJson(student.field("disabilityDegree").orElseThrow());
```

```json
[{"typeName":"DataCategory.user.health_and_medical.disability"},{"typeName":"PersonalData.SpecialCategory"},{"typeName":"Confidentiality.High"},{"typeName":"Integrity.High"},{"typeName":"Identifiability.Identified"}]
```

A sender for both requests:

```java
package edu.example.university;

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.catalog.atlas.AtlasCatalog;
import io.github.devbpm.linneo.classify.FieldClassification;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class PublishToAtlas {

    private static final URI API = URI.create("https://atlas.example.edu:21000/api/atlas/v2/");

    private final HttpClient http = HttpClient.newHttpClient();
    private final String authorization = "Basic " + Base64.getEncoder().encodeToString(
            (System.getenv("ATLAS_USER") + ":" + System.getenv("ATLAS_PASSWORD")).getBytes(StandardCharsets.UTF_8));
    private final AtlasCatalog catalog = AtlasCatalog.of(Taxonomy.load());

    /** Creates every classification type in one request. */
    void createTypes() throws IOException, InterruptedException {
        post(API.resolve("types/typedefs"), catalog.typeDefsJson());
    }

    /** Adds the classifications of a field to the column entity with the given guid. */
    void classify(String columnGuid, FieldClassification field) throws IOException, InterruptedException {
        post(API.resolve("entity/guid/" + columnGuid + "/classifications"), catalog.classificationsJson(field));
    }

    private void post(URI uri, String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException("POST " + uri + " failed: " + response.statusCode() + " " + response.body());
        }
    }
}
```

Three things are left to your integration:

- **Finding the column.** Linneo does not know the `guid` of the column entity; look it up in Atlas from the
  column's qualified name.
- **Running it again.** `POST /types/typedefs` creates types and rejects those that already exist: on later runs send
  only the new definitions, and update changed ones with `PUT`. Likewise, Atlas rejects a classification a column
  already has.
- **Propagation.** The body sets no `propagate` flag, so Atlas applies its default: in a stock installation the
  classifications flow along lineage to derived columns. A statistics column computed from `Grade.mark` would then carry
  `PersonalData.Personal` and `Identifiability.Identified` next to its own `Identifiability.Aggregated`. Decide what
  your lineage needs, and add `"propagate": false` to the objects if propagation does not suit you.

## Customising the names

`DataHubMapping` and `AtlasMapping` hold the six root names — `DataCategory`, `PersonalData`, `Confidentiality`,
`Integrity`, `Identifiability`, `DataHandling` — and have a `with...` method for each one:

```java
DataHubMapping spanish = DataHubMapping.defaults()
        .withCategories("CategoriaDato")
        .withConfidentiality("Confidencialidad");
DataHubCatalog custom = DataHubCatalog.of(taxonomy, spanish);

custom.termsOf(student.field("email").orElseThrow());
// [urn:li:glossaryTerm:CategoriaDato.user.contact.email, urn:li:glossaryTerm:PersonalData.Personal,
//  urn:li:glossaryTerm:Confidencialidad.Low, urn:li:glossaryTerm:Integrity.Medium,
//  urn:li:glossaryTerm:Identifiability.Identified]
```

`AtlasMapping` works the same way and gives `[CategoriaDato.user.contact.email, PersonalData.Personal,
Confidencialidad.Low, Integrity.Medium, Identifiability.Identified]`. Each name must start with a letter and contain
only letters, digits and underscores, so that it stays one segment of every URN or classification name; otherwise
the mapping throws `IllegalArgumentException: Invalid DataHub glossary node name 'Data Handling'` (or
`Invalid Atlas classification name '…'`). Unlike OpenMetadata's `PersonalData` and `PII`, every root is created by
Linneo, so you can rename all of them freely.

## Common mistakes

- **Expecting Linneo to call DataHub or Atlas, or to attach labels to columns.** It builds bodies and names; your
  integration sends them and finds the dataset field or column entity.
- **Reordering the DataHub proposals** (for example sorting them by URN): a term can then arrive before its node, or
  a relation before the term it points to. Send `proposals()` in its order.
- **Building the `ingestProposal` body by hand with the aspect as a JSON object.** The API wants it as a string in
  `aspect.value`; `toJson()` does that.
- **Editing Linneo's terms in the DataHub UI.** The next run of the publisher replaces those aspects.
- **Attaching only the category classification in Atlas.** `DataCategory.user.health_and_medical.disability` does not
  carry `Confidentiality.High` through its super types; send `classificationsJson(field)`, which lists every label.
- **Posting the Atlas typedefs a second time unchanged.** Existing types are rejected; send only new ones.
- **Ignoring Atlas propagation.** Derived columns may receive the personal-data classifications of their sources.
- **Computing labels from the category instead of the field** (`termsOf(field.category())`): you lose the raised
  levels, the identifiability and the aggregated/anonymised distinction, as in module 7.

## Exercises

**Exercise 8.1 — Predict.** Without running code, write the DataHub terms and the Atlas classifications of
`Staff.iban` and `Attachment.content`. Then check with `termsOf` and `classificationsOf`.

**Exercise 8.2 — Review before publishing.** Write the DataHub proposals to `datahub-proposals.jsonl` (one
`toJson()` per line) and the Atlas body to `atlas-typedefs.json`, so that the data protection officer can review them
in a pull request. How many lines does the first file have?

**Exercise 8.3 — Extension in the glossary.** With the `CampusTaxonomy` of module 6 registered, how many
proposals does `proposals()` return, and how many types does `classificationDefs()` return? Does any new glossary
node appear? Which custom properties does the term `DataCategory.user.education.lab_notebook` carry?

**Exercise 8.4 — Special categories in three catalogues.** Among the fields of `Student`, `Staff` and `Attachment`,
which carry `PersonalData.SpecialCategory` in OpenMetadata, and which in DataHub and Atlas? Explain the difference.

**Exercise 8.5 — Searching Atlas.** The data protection officer searches Atlas for the classification
`DataCategory.user.health_and_medical`. Which columns of the university model does she find, and why?

## Self-check quiz

1. In DataHub, what does the category `user.contact` become?
2. Which DataHub relation links the term of `user.contact.email` to the term of `user.contact`, and which links it to
   `Confidentiality.Low`?
3. Why is the aspect a string in the body that `DhProposal.toJson()` writes?
4. Why are the Atlas protection levels not super types of the category classifications?
5. Which label does `user.criminal_history` give in Atlas, and which tag in OpenMetadata?
6. Who attaches the DataHub terms to the fields of a dataset?

## Further reading

- [Reference — DataHub catalog](../reference/datahub/catalog.md)
- [Reference — DataHub mapping](../reference/datahub/mapping.md)
- [Reference — Apache Atlas catalog](../reference/atlas/catalog.md)
- [Reference — Apache Atlas mapping](../reference/atlas/mapping.md)
- [Reference — Data catalogues compared](../reference/README.md#data-catalogues-compared)

---

← Previous: [Module 7 — OpenMetadata](07-openmetadata.md) · Next:
[Module 9 — Processing activities and the record](09-processing-activities.md) →

<details markdown="block">
<summary><strong>Solutions</strong></summary>

**Exercise 8.1**

`Staff.iban` (`user.financial.bank_account`: `ORDINARY`, `MEDIUM`, `HIGH`): the DataHub terms
`urn:li:glossaryTerm:` + `DataCategory.user.financial.bank_account`, `PersonalData.Personal`, `Confidentiality.Medium`,
`Integrity.High`, `Identifiability.Identified`; the Atlas classifications are the same names without the prefix.

`Attachment.content` (per row, ceiling `user.health_and_medical`): `DataCategory.user.health_and_medical`,
`PersonalData.SpecialCategory`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified` — with the
`urn:li:glossaryTerm:` prefix in DataHub.

**Exercise 8.2**

```java
DataHubCatalog dataHub = DataHubCatalog.of(Taxonomy.load());
Files.write(Path.of("datahub-proposals.jsonl"),
        dataHub.proposals().stream().map(DhProposal::toJson).toList());
Files.writeString(Path.of("atlas-typedefs.json"), AtlasCatalog.of(Taxonomy.load()).typeDefsJson());
```

With the standard taxonomy the first file has 289 lines: 28 nodes, 138 terms and 123 relation aspects.

**Exercise 8.3**

293 proposals and 146 types. The extension adds two categories, `user.education.lab_notebook` and
`user.education.lab_journal`: each brings a `glossaryTermInfo` and a `glossaryRelatedTerms` (2 × 2 = 4 more
proposals) and one classification type. No node appears: both are leaves, and `user.education` already had a node
because it already had children. The lab notebook term carries `linneo.key` `user.education.lab_notebook`,
`linneo.origin` `campus`, `linneo.nature` `ORDINARY`, `linneo.confidentiality` `MEDIUM`, `linneo.integrity` `HIGH`,
`linneo.secret` `false` and the extension's attribute `retention` `permanent`. Its `termSource` is `INTERNAL`.

**Exercise 8.4**

OpenMetadata: `Student.disabilityDegree`, `Staff.sexOffenceCertificateClear` and `Attachment.content`. DataHub and
Atlas: only `Student.disabilityDegree` and `Attachment.content`; `Staff.sexOffenceCertificateClear` carries
`PersonalData.CriminalOffence`. OpenMetadata's built-in `PersonalData` classification has no tag for criminal
offence data, so Linneo maps it to `SpecialCategory` there; in DataHub and Atlas Linneo creates the `PersonalData`
labels itself and keeps the three natures apart.

**Exercise 8.5**

`Student.disabilityDegree` and `Attachment.content`. `Attachment.content` carries
`DataCategory.user.health_and_medical` itself (its ceiling); `disabilityDegree` carries
`DataCategory.user.health_and_medical.disability`, whose super type is `DataCategory.user.health_and_medical`, so the
search matches it too.

**Quiz**

1. A glossary node `urn:li:glossaryNode:DataCategory.user.contact` (because it has children) and a term
   `urn:li:glossaryTerm:DataCategory.user.contact` inside that node.
2. `isRelatedTerms` to the parent; `relatedTerms` to the protection terms.
3. Because the `ingestProposal` API takes the aspect JSON-encoded in `aspect.value`, with its `contentType`.
4. Because a child category may raise a level its parent implies, and Atlas inheritance cannot be undone further
   down; and so that tag-based policies can act on the levels directly.
5. `PersonalData.CriminalOffence` in Atlas; `PersonalData.SpecialCategory` in OpenMetadata.
6. Your integration (for example with the GraphQL `addTerms` mutation); Linneo only computes the terms.

</details>
