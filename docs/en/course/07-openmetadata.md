---
title: "7. OpenMetadata"
parent: "Course"
nav_order: 7
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OpenMetadataCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OpenMetadataMapping.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OmTag.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OmClassification.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# Module 7 — OpenMetadata

## Learning objectives

After this module you will be able to:

- explain how Linneo's taxonomy maps onto OpenMetadata classifications and tags, including the built-in
  `PersonalData` and `PII` classifications;
- generate the request bodies to create the classifications and tags, and send them with your own HTTP client;
- compute the tags of each column from its `FieldClassification`;
- rename the classifications and move the sensitivity threshold with `OpenMetadataMapping`.

## Why a catalogue

The classification lives in the code; the data lives in databases, warehouses and dashboards that analysts browse in
a data catalogue. [OpenMetadata](https://open-metadata.org) is an open source catalogue that organises tags into
**classifications**. Publishing Linneo's taxonomy there means that the person who finds the `student` table in the
catalogue sees, on the `disability_degree` column, that it is special category data with high confidentiality —
the same answer the code gives.

OpenMetadata ships two built-in classifications that Linneo **reuses and never creates**:

- `PersonalData`, with the tags `Personal` and `SpecialCategory`;
- `PII`, with the tags `None`, `NonSensitive` and `Sensitive`.

## The mapping

| Linneo | OpenMetadata |
|---|---|
| Data category `user.contact.email` | Nested tag `DataCategory.user.contact.email` in the classification `DataCategory` (mutually exclusive) |
| Nature `ORDINARY` | `PersonalData.Personal` |
| Nature `SPECIAL_CATEGORY` or `CRIMINAL_OFFENCE` | `PersonalData.SpecialCategory` — the only OpenMetadata tag for data with reinforced protection |
| Personal data with confidentiality below `MEDIUM` | `PII.NonSensitive` |
| Personal data with confidentiality `MEDIUM` or higher | `PII.Sensitive` (threshold configurable) |
| Not personal | `PII.None` |
| Confidentiality, integrity, identifiability | `Confidentiality.*`, `Integrity.*`, `Identifiability.*` (for example `Confidentiality.High`) |
| Secret | `DataHandling.Secret` |
| Protection implied by a category | goes on each column: `tagsOf(field)` (and `tagsOf(category)` for the category alone) |

## The catalog

```java
OpenMetadataCatalog catalog = OpenMetadataCatalog.of(Taxonomy.load());
```

`OpenMetadataCatalog` is immutable and thread-safe. Use the same taxonomy as the rest of the application, so that
your extension's terms and stricter levels (module 6) are published too.

### Classifications

`classifications()` returns the five classifications Linneo creates, in creation order. `toJson()` gives the body
of a `CreateClassification` request (`PUT /api/v1/classifications`):

```json
{"name":"DataCategory","description":"What a column holds, from the Linneo data category taxonomy (fideslang default taxonomy plus extensions).","mutuallyExclusive":true}
{"name":"Confidentiality","description":"Harm that disclosing the data would cause.","mutuallyExclusive":true}
{"name":"Integrity","description":"Harm that an unwanted change to the data would cause.","mutuallyExclusive":true}
{"name":"Identifiability","description":"How directly a personal data value points to the person.","mutuallyExclusive":true}
{"name":"DataHandling","description":"Handling rules that apply to the data.","mutuallyExclusive":false}
```

### Tags

`tags()` returns the tags to create, in an order you can send as is: first the level tags (four confidentiality,
three integrity, four identifiability), then `DataHandling.Secret`, then one tag per data category with **every
parent before its children**. For the standard taxonomy that is 4 + 3 + 4 + 1 + 125 = 137 tags.

Each `OmTag` has a `classification`, a `parent` (the fully qualified name of the parent tag, or `null` at the root),
a `name` (one segment, the last of the key), a `displayName` (the category's name), and a `description`.
`fullyQualifiedName()` gives the name used to apply it, and `toJson()` the body of a `CreateTag`
request (`PUT /api/v1/tags`). The tag of `user.education.grade`:

```json
{"classification":"DataCategory","parent":"DataCategory.user.education","name":"grade","displayName":"Grade","description":"Marks, grades or assessment results obtained by the subject."}
```

The tag carries no `associatedTags`: OpenMetadata accepts that field but never stores it, so the protection a category
implies goes on each column instead (see [The tags of a column](#the-tags-of-a-column)). For a deprecated category,
the description ends with `Deprecated: use <replacement>.` (or just `Deprecated.`).

## Sending the bodies

**Linneo makes no HTTP calls.** It produces bodies; you send them with the client you already use. A minimal
version with the JDK's `HttpClient`:

```java
package edu.example.university;

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.catalog.openmetadata.OmClassification;
import io.github.devbpm.linneo.catalog.openmetadata.OmTag;
import io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataCatalog;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class PublishToOpenMetadata {

    public static void main(String[] args) throws IOException, InterruptedException {
        URI api = URI.create("https://openmetadata.example.edu/api/v1/");
        String token = System.getenv("OPENMETADATA_TOKEN");
        HttpClient http = HttpClient.newHttpClient();
        OpenMetadataCatalog catalog = OpenMetadataCatalog.of(Taxonomy.load());

        for (OmClassification classification : catalog.classifications()) {
            put(http, api.resolve("classifications"), token, classification.toJson());
        }
        for (OmTag tag : catalog.tags()) {              // parents always come before their children
            put(http, api.resolve("tags"), token, tag.toJson());
        }
    }

    private static void put(HttpClient http, URI uri, String token, String json)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException("PUT " + uri + " failed: " + response.statusCode() + " " + response.body());
        }
    }
}
```

The bodies follow OpenMetadata's `CreateClassification` and `CreateTag` schemas, including the `parent` field. They
were checked against OpenMetadata 2.0.6 and use only fields present since 0.13.2. Classifications must exist before
their tags, and parent tags before their children: the order of `classifications()` and `tags()` guarantees both.

## The tags of a column

`tagsOf(FieldClassification)` gives the fully qualified tags of a column, computed from the **field's** effective
classification: its category, whether the stored value is personal, the field's own levels, its identifiability
and the secret flag. For `Student`:

```java
Taxonomy taxonomy = Taxonomy.load();
OpenMetadataCatalog catalog = OpenMetadataCatalog.of(taxonomy);
ClassClassification student = new Classifier(taxonomy).classify(Student.class);

Map<String, List<String>> tagsByField = student.fields().stream()
        .collect(Collectors.toMap(FieldClassification::field, catalog::tagsOf));
```

| Field | Tags |
|---|---|
| `email` | `DataCategory.user.contact.email`, `PersonalData.Personal`, `PII.NonSensitive`, `Confidentiality.Low`, `Integrity.Medium`, `Identifiability.Identified` |
| `disabilityDegree` | `DataCategory.user.health_and_medical.disability`, `PersonalData.SpecialCategory`, `PII.Sensitive`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified` |
| `analyticsId` | `DataCategory.user.unique_id.pseudonymous`, `PersonalData.Personal`, `PII.NonSensitive`, `Confidentiality.Low`, `Integrity.Medium`, `Identifiability.Pseudonymized` |
| `counsellingNotes` | `DataCategory.user.content.private`, `PersonalData.Personal`, `PII.Sensitive`, `Confidentiality.High`, `Integrity.Medium`, `Identifiability.Identified` |

And elsewhere in the model:

| Field | Tags |
|---|---|
| `Staff.passwordHash` | `DataCategory.user.authorization.password`, `PersonalData.Personal`, `PII.Sensitive`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified`, `DataHandling.Secret` |
| `Staff.sexOffenceCertificateClear` | `DataCategory.user.criminal_history`, `PersonalData.SpecialCategory`, `PII.Sensitive`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified` |
| `CourseStatistics.averageMark` | `DataCategory.user.education.grade`, `PII.None`, `Confidentiality.Medium`, `Integrity.High`, `Identifiability.Aggregated` |
| `MenuGroup.code` | `DataCategory.system.configuration`, `PII.None`, `Confidentiality.Low`, `Integrity.Medium` |

Notice:

- `counsellingNotes` is `PII.Sensitive` and `Confidentiality.High` because of the level raised **on the field**;
  the category alone would give `Medium`.
- `averageMark` keeps its category tag but is `PII.None` and gets no `PersonalData` tag: aggregated values are not
  personal data.
- Non-personal fields have no `Identifiability` tag.

There is also `tagsOf(DataCategory)`: the category's tag followed by the tags it implies (the implied protection,
without identifiability). Use it when you only know the category, not the field.

Applying the tags to the columns of your tables is your integration's job: map each field to its table and column
(your persistence naming rules) and use the OpenMetadata API you already use for table metadata.

## Customising the names

`OpenMetadataMapping` holds the classification names and the sensitivity threshold. `defaults()` is `DataCategory`,
`PersonalData`, `PII`, `Confidentiality`, `Integrity`, `Identifiability`, `DataHandling`, with personal data
sensitive from `MEDIUM`. Three `with...` methods cover the common changes:

```java
OpenMetadataMapping spanish = OpenMetadataMapping.defaults()
        .withCategories("CategoriaDato")
        .withConfidentiality("Confidencialidad")
        .withSensitiveFrom(Confidentiality.LOW);
OpenMetadataCatalog custom = OpenMetadataCatalog.of(taxonomy, spanish);

custom.tagsOf(student.field("email").orElseThrow());
// [CategoriaDato.user.contact.email, PersonalData.Personal, PII.Sensitive, Confidencialidad.Low,
//  Integrity.Medium, Identifiability.Identified]
```

With `sensitiveFrom` lowered to `LOW`, the student's email is now `PII.Sensitive`. For the other names, use the
record's constructor:

```java
OpenMetadataMapping full = new OpenMetadataMapping("CategoriaDato", "PersonalData", "PII",
        "Confidencialidad", "Integridad", "Identificabilidad", "Tratamiento", Confidentiality.MEDIUM);
```

`personalData` and `pii` name OpenMetadata's **built-in** classifications: change them only if your OpenMetadata
installation uses other names, because Linneo never creates them. Every name must be non-blank and contain no dot;
otherwise the mapping throws `IllegalArgumentException: Invalid OpenMetadata classification name 'Data.Category'`.

## Common mistakes

- **Expecting Linneo to call OpenMetadata.** It only builds bodies and tag names.
- **Sending tags in another order** (for example sorted alphabetically by FQN) — a child before its parent fails.
- **Publishing `Taxonomy.standard()`** while the application uses extensions; the catalogue then lacks your terms.
- **Computing column tags from the category instead of the field** (`tagsOf(field.category())`): you lose the
  field's raised levels, its identifiability and the aggregated/anonymised distinction.
- **Renaming `PersonalData` or `PII`** in the mapping without having those classifications in OpenMetadata.
- **Reading `PersonalData.SpecialCategory` as "art. 9 only".** Criminal offence data (art. 10) is also tagged
  `SpecialCategory`; the `DataCategory` tag tells them apart.

## Exercises

**Exercise 7.1 — Predict.** Without running code, write the tags of `Staff.iban` and `Attachment.content`. Then
check with `tagsOf`.

**Exercise 7.2 — Review before publishing.** Instead of sending the bodies, write each one to a file
(`classifications.jsonl` and `tags.jsonl`, one JSON object per line) so that the data protection officer can review
them in a pull request.

**Exercise 7.3 — Threshold.** With the default mapping, which personal fields of `Student` are `PII.NonSensitive`?
And with `withSensitiveFrom(Confidentiality.HIGH)`?

**Exercise 7.4 — Extension in the catalogue.** With the `CampusTaxonomy` of module 6 registered, how many tags
does `catalog.tags()` return? What is the `description` of the tag `DataCategory.user.education.lab_journal`?

## Self-check quiz

1. Which two OpenMetadata classifications does Linneo reuse and never create?
2. Which `PersonalData` tag does criminal offence data get?
3. What does `tagsOf(category)` return, and where does the implied protection go when you tag a column?
4. Why are the level tags created before the category tags?
5. Which `PII` tag does a pseudonymised field with confidentiality `LOW` get with the default mapping?

## Further reading

- [Reference — OpenMetadata catalog](../reference/openmetadata/catalog.md)
- [Reference — OpenMetadata mapping](../reference/openmetadata/mapping.md)

---

← Previous: [Module 6 — Extending the taxonomy](06-extending-the-taxonomy.md) · Next:
[Module 8 — DataHub and Apache Atlas](08-datahub-and-atlas.md) →

<details markdown="block">
<summary><strong>Solutions</strong></summary>

**Exercise 7.1**

`Staff.iban` (`user.financial.bank_account`: `ORDINARY`, `MEDIUM`, `HIGH`):
`DataCategory.user.financial.bank_account`, `PersonalData.Personal`, `PII.Sensitive`, `Confidentiality.Medium`,
`Integrity.High`, `Identifiability.Identified`.

`Attachment.content` (per row, ceiling `user.health_and_medical`):
`DataCategory.user.health_and_medical`, `PersonalData.SpecialCategory`, `PII.Sensitive`, `Confidentiality.High`,
`Integrity.High`, `Identifiability.Identified`.

**Exercise 7.2**

```java
OpenMetadataCatalog catalog = OpenMetadataCatalog.of(Taxonomy.load());
Files.write(Path.of("classifications.jsonl"),
        catalog.classifications().stream().map(OmClassification::toJson).toList());
Files.write(Path.of("tags.jsonl"),
        catalog.tags().stream().map(OmTag::toJson).toList());
```

**Exercise 7.3**

Default (`MEDIUM`): `createdBy`, `id`, `firstName`, `lastName`, `email` and `analyticsId` are `PII.NonSensitive`
(confidentiality `LOW`); the rest are `PII.Sensitive`. With `HIGH`: only `disabilityDegree` and `counsellingNotes`
are `PII.Sensitive`; every other personal field is `PII.NonSensitive`.

**Exercise 7.4**

The extension adds two categories (`user.education.lab_notebook` and `user.education.lab_journal`), so 137 + 2 =
139 tags. (Data subjects are not published as tags.) The description is
`Former name of the lab notebook key. Deprecated: use user.education.lab_notebook.`

**Quiz**

1. `PersonalData` and `PII`.
2. `PersonalData.SpecialCategory`.
3. The category tag followed by the protection the category implies: `PersonalData.*` (if personal), `PII.*`,
   `Confidentiality.*`, `Integrity.*` and, for secrets, `DataHandling.Secret`. The implied protection goes on each
   column, because OpenMetadata does not store `associatedTags`.
4. Because the tags a column gets refer to them, and parents must exist before children.
5. `PII.NonSensitive` (personal, below `MEDIUM`).

</details>
