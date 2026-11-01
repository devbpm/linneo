# Linneo

[![CI](https://github.com/devbpm/linneo/actions/workflows/ci.yml/badge.svg)](https://github.com/devbpm/linneo/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.devbpm/linneo)](https://central.sonatype.com/artifact/io.github.devbpm/linneo)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

**Declare what every field of your data model holds, and the protection it needs. Let the build check it.**

> The name comes from Carl Linnaeus (Carl von Linné), whose taxonomy classified living beings into a nested hierarchy
> of ranks, each identified by a name. Linneo does the same for data: every category sits under its parent in a tree,
> and its dotted key (`user.contact.email`) names the path from the root.

Linneo is a small Java library, with no runtime dependencies, that brings a data classification taxonomy into your
code. You annotate each field with a category such as `user.contact.email` or `user.health_and_medical`, and Linneo
tells you what that means: whether it is personal data, whether it is a special category under the GDPR, how
confidential it is, whether it is a secret. A test fails as soon as someone adds a field without classifying it.

```java
@Category(USER_HEALTH_AND_MEDICAL_DISABILITY)   // special category, confidentiality HIGH, integrity HIGH
private Integer disabilityDegree;
```

**[Documentation](https://devbpm.github.io/linneo/)** · [Reference](docs/en/reference/README.md) ·
[Course](docs/en/course/README.md) · [Documentación en español](docs/es/reference/README.md) ·
[Changelog](CHANGELOG.md)

## Contents

- [Why Linneo](#why-linneo)
- [Features](#features)
- [Installation](#installation)
- [Quick start](#quick-start)
- [The taxonomy](#the-taxonomy)
- [Extending the taxonomy](#extending-the-taxonomy)
- [Processing activities (GDPR)](#processing-activities-gdpr)
- [Keeping stored records in step](#keeping-stored-records-in-step)
- [Data catalogs](#data-catalogs)
- [Design principles](#design-principles)
- [Versioning](#versioning)
- [Building from source](#building-from-source)
- [Contributing](#contributing)
- [Licence and attribution](#licence-and-attribution)

## Why Linneo

Knowing which columns hold personal data, and how sensitive they are, is the starting point for a GDPR record of
processing, for masking logs, for access control and for a data catalogue. That knowledge usually lives in a
spreadsheet that goes out of date with the next migration.

Linneo keeps it next to the code it describes, uses a public, shared vocabulary instead of an ad-hoc one, and makes
the build fail when the model and its classification drift apart.

## Features

- **One taxonomy, keyed like a tree.** The complete [fideslang](https://github.com/ethyca/fideslang) default
  taxonomy, plus Linneo's extensions for every GDPR article 9 category, education data (`user.education.*`) and
  non-personal data (`system.*`, `org.*`, `reference.*`): 126 data categories, 63 data uses and 22 data subjects.
- **Protection travels with the category.** Each category carries its GDPR nature (ordinary, special category,
  criminal offence or not personal), its confidentiality, its integrity and whether it is a secret. Children inherit
  them and can only make them stricter.
- **Annotations on the model.** `@Category`, `@DefaultCategory`, `@CategoryByRow`, `@DerivedFrom` for values
  computed from other fields, `@Composite` for embedded parts and `@Exempt` for technical fields, with compile-time
  constants for every key, so a typo does not compile.
- **One comparison of protection.** `Protection` orders natures and levels partially (special category and criminal
  offence data are not comparable), so a derived value, a stored composite or a stricter classification set at run
  time is never allowed to protect less than its source.
- **A classifier for your tests.** Fails the build when a field has no classification, a key is unknown or a level
  is weakened. `ModelClasses` finds the classes of the model package, so a class nobody listed is verified too.
- **Safe outputs.** `DisclosurePolicy` tells, field by field, whether a value may be shown, must be masked or left
  out of a log, an export or a test copy; a field that needs more protection never gets a laxer treatment.
- **GDPR record of processing activities.** Declare each processing activity (purposes, data subjects, the
  annotated entities, lawful basis, article 9.2 and 10 conditions, retention, transfers) and get it checked against
  the classification: special category data without an article 9.2 condition fails the build. The article 30 record
  comes out as Markdown or JSON.
- **Stored copies kept in step.** The classification of the whole model as data (`DataInventory`), compared with
  what an application stored (`InventoryDiff`), with fingerprints to skip unchanged starts; the taxonomy as text
  (`Taxonomy.toTsv()`) and its changes between versions (`TaxonomyDiff`), so applications can migrate keys and
  review every change that weakens protection. Catalog tags come from stored records too, and two records of
  processing activities can be compared (`RecordDiff`).
- **Data catalog ready.** The taxonomy, and the tags of each column, for [OpenMetadata](https://open-metadata.org/)
  (classifications and nested tags), [DataHub](https://datahub.com/) (a business glossary) and
  [Apache Atlas](https://atlas.apache.org/) (classification types, usable by Apache Ranger tag policies).
- **Extensible.** Add your own terms or tighten existing ones through a `TaxonomyExtension`, loaded with
  `ServiceLoader`.

Java 21 or later. A single JAR with no dependencies, published as the named JPMS module `io.github.devbpm.linneo`.

## Installation

> [!NOTE]
> Linneo has not been released yet. Until `0.1.0` is on Maven Central, [build it from source](#building-from-source)
> and run `./mvnw install` to use `0.1.0-SNAPSHOT` from your local repository.

Maven:

```xml
<dependency>
    <groupId>io.github.devbpm</groupId>
    <artifactId>linneo</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle:

```kotlin
implementation("io.github.devbpm:linneo:0.1.0")
```

With JPMS, add `requires io.github.devbpm.linneo;` to your `module-info.java`.

## Quick start

### 1. Annotate the model

```java
import static io.github.devbpm.linneo.Categories.*;

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.CategoryByRow;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.annotation.Exempt;

// A class with no personal data says so once, with the reason.
@DefaultCategory(value = SYSTEM_CONFIGURATION, reason = "menu configuration")
public class MenuGroup {
    private String code;
    private String label;
}

// Personal data is declared field by field.
public class Person {

    @Category(USER_NAME)
    private String fullName;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    @Category(USER_HEALTH_AND_MEDICAL_DISABILITY)
    private Integer disabilityDegree;

    @Category(value = USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
    private String analyticsId;

    @Category(value = USER_CONTENT_PRIVATE, confidentiality = Confidentiality.HIGH)   // stricter, never weaker
    private String notes;

    @CategoryByRow(declaredBy = "the category of the document", ceiling = USER_HEALTH_AND_MEDICAL)
    private byte[] attachment;

    @Exempt("computed from fullName")
    private String initials;
}
```

A field can raise the confidentiality or integrity of its category, never lower it. When an annotation sets more
than the key, Java requires the key to be written as `value = ...`.

### 2. Verify it in a test

```java
@Test
void everyEntityIsClassified() {
    new Classifier(Taxonomy.load()).verify(List.of(MenuGroup.class, Person.class /* , ... */));
}
```

`verify` throws a `ClassificationException` listing every problem: unclassified fields, unknown keys, a personal
category used as a class default, a lowered level, conflicting annotations. Deprecated keys are reported as
warnings and resolved to their replacement.

A list written by hand misses the next class someone adds. Take the classes from the model package instead,
`ModelClasses.inPackageTree(Person.class)`, or with JPA from the metamodel:
`entityManagerFactory.getMetamodel().getEntities().stream().map(EntityType::getJavaType).toList()`.

### 3. Use the classification

```java
Classifier classifier = new Classifier(Taxonomy.load());
ClassClassification person = classifier.classify(Person.class);

person.personalFields();                          // the fields that hold personal data
DataCategory email = person.field("email").orElseThrow().category();
email.nature();                                   // ORDINARY
email.confidentiality();                          // LOW
```

### 4. Publish to a data catalog

```java
OpenMetadataCatalog catalog = OpenMetadataCatalog.of(Taxonomy.load());
catalog.classifications();                               // bodies for PUT /api/v1/classifications
catalog.tags();                                          // bodies for PUT /api/v1/tags, parents first
catalog.tagsOf(person.field("email").orElseThrow());     // the tags of a column
```

Linneo makes no HTTP calls: send each body's `toJson()` with the client you already use. DataHub and Apache Atlas
work the same way; see [Data catalogs](#data-catalogs).

For a guided tour with exercises, follow the [course](docs/en/course/README.md).

## The taxonomy

| Root | Origin | Nature | What it covers |
|---|---|---|---|
| `user` | fideslang | personal | Data about a natural person: name, contact, identifiers, demographics, health, financial, behaviour… plus Linneo's `user.education.*`, `user.workplace.*`, the GDPR article 9 categories fideslang lacks (`user.demographic.philosophical_belief`, `trade_union_membership`, `sex_life`), `user.health_and_medical.disability`, `user.behavior.activity_log`, `user.device.session_id` and more |
| `system` | fideslang | not personal | `system.authentication`, `system.operations`, plus Linneo's `system.authentication.secret`, `system.security`, `system.integrity`, `system.configuration` |
| `org` | Linneo | not personal | The organization and legal persons: structure, legal entities, locations, processes, business records |
| `reference` | Linneo | not personal | Public code lists and registers |

fideslang defines no protection levels; Linneo's defaults are in
[`linneo-category-policy.tsv`](src/main/resources/io/github/devbpm/linneo/internal/linneo-category-policy.tsv).
For example:

- every GDPR article 9 category (ethnic origin, political opinion, religious or philosophical belief, trade union
  membership, genetic, biometric, health, sex life and sexual orientation) is a special category with high
  confidentiality and integrity (the builder refuses a special category or criminal offence category below `HIGH`);
- criminal history is criminal offence data;
- passwords, credentials and session identifiers are secrets, while a user's permissions (`user.authorization`)
  are confidential but not secret;
- public reference data is `PUBLIC`.

Data uses and data subjects describe processing activities rather than fields, so there are no annotations for
them; they are the purposes and data subjects of a [processing activity](#processing-activities-gdpr).

The [standard taxonomy reference](docs/en/reference/standard-taxonomy.md) lists every root, Linneo addition and
default level.

### Facets of a data category

| Facet | Values | Rule |
|---|---|---|
| `nature` | `NONE`, `ORDINARY`, `SPECIAL_CATEGORY`, `CRIMINAL_OFFENCE` | fixed by the root; `ORDINARY` may become stricter, the others never change |
| `confidentiality` | `PUBLIC`, `LOW`, `MEDIUM`, `HIGH` | inherited; a child or a field may raise it, never lower it |
| `integrity` | `LOW`, `MEDIUM`, `HIGH` | inherited; a child or a field may raise it, never lower it |
| `secret` | `true` / `false` | once a category is secret, all its children are |
| `attributes` | free key/value pairs | inherited unless overridden; for your own metadata, such as a retention class |

`Identifiability` (`IDENTIFIED`, `PSEUDONYMIZED`, `ANONYMIZED`, `AGGREGATED`) belongs to the field, not to the
category. Anonymized and aggregated values of a personal category do not count as personal data.

## Extending the taxonomy

```java
public final class CampusTaxonomy implements TaxonomyExtension {

    @Override
    public String name() {
        return "campus";
    }

    @Override
    public void contribute(TaxonomyBuilder builder) {
        builder.category("user.education.library_loan", "Library Loan", "Books and resources the subject borrowed.",
                        c -> c.attribute("retention", "one year"))
               .refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
               .dataSubject("visiting_researcher", "Visiting Researcher", "A researcher from another institution.");
    }
}
```

Register it in `module-info.java` (`provides io.github.devbpm.linneo.TaxonomyExtension with ...`) or in
`META-INF/services/io.github.devbpm.linneo.TaxonomyExtension`; `Taxonomy.load()` applies every registered extension.
An extension adds terms and makes protection stricter, never weaker: the taxonomy refuses to build if a term
lowers a level inherited from its parent or leaves special category or criminal offence data below `HIGH`
confidentiality and integrity, and a new root must declare its nature, confidentiality and integrity.
See [Extending the taxonomy](docs/en/reference/extending.md).

### The university extension

Linneo ships an optional extension for universities and other higher education institutions, informed by the
[Open Education API (OEAPI)](https://github.com/open-education-api/specification). It adds 32 data categories, with the finest detail on the student record: enrolment changes and record
transfers, assessment attempts, final grades and averages, credit recognition, programme progress, degrees,
certificates and language proficiency. It also covers attendance, academic accommodations, discipline, advising,
admissions, internships and their evaluation, international mobility, means-tested aid, fee exemptions and
advancement. It adds 27 data uses (certification, credit recognition, examination, internships, mobility, career
services, means assessment, statutory reporting, and the new roots `advancement` and `research`) and 12 data
subjects (prospective student, exchange student, doctoral candidate, placement supervisor, external examiner,
donor…). An academic accommodation reveals a disability, so it is a special category of data. The extension follows
fideslang's rules for custom labels: it extends existing classes, keeps data subjects flat and carries no version
information.

It is not applied by default. Apply it explicitly, alone or with your own extensions:

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard()
        .extend(new UniversityTaxonomy())
        .extend(new CampusTaxonomy())
        .build();

@Category(UniversityCategories.USER_EDUCATION_ACCOMMODATION)   // special category, confidentiality HIGH
private String examArrangement;
```

or register it as a service of your application so that `Taxonomy.load()` applies it: on the class path, list the
class in `META-INF/services/io.github.devbpm.linneo.TaxonomyExtension`; on the module path, provide it through a
class of your own module with a static `provider()` method. See the
[university extension reference](docs/en/reference/extensions/university.md).

### Business extensions: finance, supply chain, project operations and human resources

Four more optional extensions cover the business areas of an ERP, informed by the
[Common Data Model (CDM)](https://github.com/microsoft/CDM) schemas for finance and operations. Records about
organizations extend `org.business_records`; records about people extend `user.*`, so the `Classifier` and the
`ProcessingChecker` treat them as personal data. The four never share a key, so any of them can be applied with any
other and with the university extension.

| Extension | Adds | Highlights |
|---|---|---|
| `FinanceTaxonomy` (`"finance"`) | 19 categories, 13 data uses below `finance`, 3 data subjects | ledger, invoices, treasury and taxes with `HIGH` integrity; creditworthiness, debt collection, tax status and AML due diligence with `HIGH` confidentiality; expense claims; signing authority |
| `SupplyChainTaxonomy` (`"supply_chain"`) | 16 categories, 12 data uses below `operations` and `sales`, 3 data subjects | products, formulas and costs (trade secrets, `HIGH` confidentiality), procurement and supplier bids, inventory and quality (`HIGH` integrity), shipments; delivery details, drivers' driving records, labour tracking |
| `ProjectOperationsTaxonomy` (`"project_operations"`) | 11 categories, 6 data uses below `operations.project_management`, 2 data subjects | projects, contracts, estimates, actuals, funding and rates; project assignments, time entries (`HIGH` integrity), skills, utilization and resource rates |
| `HumanResourcesTaxonomy` (`"human_resources"`) | 21 categories, 15 data uses below `employment`, 6 data subjects | the new branch `user.employment` (contract, compensation, payroll, working time record, leave, performance, recruitment, discipline, termination, monitoring, whistleblowing); sick leave, occupational health and workplace adjustments as special categories with the maximum protection |

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard()
        .extend(new HumanResourcesTaxonomy())
        .extend(new FinanceTaxonomy())
        .build();

@Category(HumanResourcesCategories.USER_HEALTH_AND_MEDICAL_SICK_LEAVE)   // special category, HIGH / HIGH
private Integer sickLeaveDays;
@Category(FinanceCategories.USER_FINANCIAL_EXPENSE)                      // expense claim, MEDIUM / HIGH
private BigDecimal mileage;
```

The emergency contact (`user.contact.emergency_contact`, data subject `emergency_contact`) is part of the standard
taxonomy, since both universities and employers record it. See the
[extensions reference](docs/en/reference/extensions/README.md).

## Processing activities (GDPR)

The annotations say what each field holds; a processing activity says why it is processed, about whom and on what
lawful basis. `ProcessingChecker` checks activities against the taxonomy and the classification of the entities
they use, and builds the record of processing activities of GDPR article 30.

```java
ProcessingActivity enrollment = ProcessingActivity.builder("enrollment", "Student enrollment")
        .purposes(DataUses.EDUCATION_ENROLLMENT, DataUses.EDUCATION_ACADEMIC_RECORDS)
        .dataSubjects(DataSubjects.STUDENT)
        .entities(Person.class)                          // its personal fields are the data processed
        .legalBasis(LegalBasis.PUBLIC_INTEREST)          // article 6.1.e
        .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)   // article 9.2.g
        .legalReference("Higher Education Act, art. 12")
        .retention("Five years after the student leaves")
        .build();

ProcessingRecord record = new ProcessingChecker(Taxonomy.load()).verify(enrollment);
record.toMarkdown("Example University");         // the article 30 record, one table per activity
record.toJson();
```

`verify` throws a `ProcessingException` when an activity would be unlawful as declared: personal data without a
lawful basis, special category data without an article 9.2 condition, criminal offence data without an article 10
basis, an unknown key or an entity that is not fully classified. Gaps in the record are warnings listed as open
points: no retention, a basis resting on law without naming it, legitimate interests without the interest, or an
activity with special category data, criminal offence data or profiling that refers to no impact assessment. The
bases use the same values as fideslang's privacy declarations.

`new ProcessingChecker(Taxonomy.load(), Jurisdiction.SPAIN)` also applies Spain's LOPDGDD: explicit consent alone is
an error for the special categories of article 9.1 (ideology, religion, trade union membership, sexual orientation,
beliefs, racial or ethnic origin), and the missing legal reference warning notes that article 9.2 needs a statute.

The bases, retention, recipients, transfers and impact assessment form the activity's `Governance`. An application
whose data protection officer edits them keeps them in its own storage and checks `activity.withGovernance(stored)`;
each `ProcessingViolation` names its `element()`, and `element().isGovernance()` tells a gap for the officer from a
mistake in the code.

Linneo checks what you declare; it is not legal advice and does not replace your own assessment.

## Keeping stored records in step

Applications keep copies of the classification: a field table behind their screens, permissions, a data catalog.
Linneo does not write them, it tells the application what to write. `Classifier.inventory` turns the model into a
`DataInventory` under object keys the application chooses; `InventoryDiff` compares it with what was stored and says,
for each field, whether it was added, removed or changed and whether its protection tightened or weakened:

```java
DataInventory declared = classifier.inventory(entities, type -> tableNameOf(type));
if (!declared.fingerprint().equals(storedFingerprint)) {
    InventoryDiff diff = InventoryDiff.between(storedInventory, declared, taxonomy);
    diff.changes().forEach(change -> apply(change));           // insert, retire, rewrite: the application's job
    diff.weakening().forEach(change -> requestReview(change));  // weaker protection is applied and reviewed
}
```

The taxonomy is data too: keep `taxonomy.toTsv()` when you apply it, and on the next version compare with
`TaxonomyDiff.between(Taxonomy.fromTsv(applied), taxonomy)`. `requireCompatible()` refuses removed terms and weaker
protection, and `declared.affectedBy(diff)` lists the fields each changed category reaches. Since rows store keys and
not levels, a stricter category protects them at once.

The other copies follow the same way. Each catalog gives the tags of a stored `InventoryField`, resolved with the
catalog's taxonomy, exactly as it gives those of the field's classification, so the tags a column had (the stored
field, the taxonomy applied before) and the tags it needs (the declared field, the current taxonomy) tell what to
remove and what to add.
The record of processing activities has a `fingerprint()`, and `RecordDiff.between(approved, current)` lists the
`ActivityChange`s since the data protection officer approved it: which activities, which aspects, and whether they
now process data that needs more protection. The [course module](docs/en/course/10-reconciliation.md) walks through a
whole reconciliation, with its performance on a model of 800 entities.

## Data catalogs

Linneo publishes the taxonomy to a data catalog and tells you which tags, terms or classifications each column
gets. It only builds the request bodies: it makes no network calls and adds no dependency. You still decide which
table column a Java field maps to.

| | OpenMetadata | DataHub | Apache Atlas |
|---|---|---|---|
| Package | `io.github.devbpm.linneo.catalog.openmetadata` | `io.github.devbpm.linneo.catalog.datahub` | `io.github.devbpm.linneo.catalog.atlas` |
| Category tree | Nested tags of the `DataCategory` classification | Glossary nodes and terms; each term *is a* term of its parent | Classification types whose super type is the parent's |
| `user.contact.email` | `DataCategory.user.contact.email` | `urn:li:glossaryTerm:DataCategory.user.contact.email` | `DataCategory.user.contact.email` |
| Protection | Built-in `PersonalData` and `PII`, plus `Confidentiality`, `Integrity`, `Identifiability`, `DataHandling` | Terms of the `PersonalData`, `Confidentiality`, `Integrity`, `Identifiability` and `DataHandling` nodes | Classifications of the same names |
| Create | `PUT /api/v1/classifications`, `PUT /api/v1/tags` | `POST /aspects?action=ingestProposal` | `POST /api/atlas/v2/types/typedefs` (`PUT` to update) |
| Per column | `tagsOf(field)` | `termsOf(field)` | `classificationsOf(field)`, `classificationsJson(field)` |
| `field` is | a `FieldClassification`, or a stored `InventoryField` | the same | the same |

```java
FieldClassification field = person.field("email").orElseThrow();   // from the quick start

DataHubCatalog datahub = DataHubCatalog.of(Taxonomy.load());
datahub.proposals();                          // ingestProposal bodies: nodes, terms, related terms
datahub.termsOf(field);                       // glossary term URNs of a column

AtlasCatalog atlas = AtlasCatalog.of(Taxonomy.load());
atlas.typeDefsJson();                         // body for POST /api/atlas/v2/types/typedefs
atlas.classificationsJson(field);             // body for POST /api/atlas/v2/entity/guid/{guid}/classifications
```

In Atlas a search for `DataCategory.user` also finds the columns of every sub-classification, and the protection
classifications (`Confidentiality.High`, `DataHandling.Secret`…) can drive Apache Ranger tag-based policies.

The names of the root classifications, nodes and tags are configurable with `OpenMetadataMapping`,
`DataHubMapping` and `AtlasMapping`. The request bodies follow the published APIs of each catalog and were checked
against real servers: OpenMetadata 2.0.6 (only fields present since 0.13.2), DataHub v1.7.0.1 (aspects valid since
v0.9.2; send each proposal with the header `X-RestLi-Protocol-Version: 2.0.0`) and Apache Atlas 2.3.0 (dotted
classification names valid since 1.0, API unchanged to 2.5.0; `POST /types/typedefs` answers 409 when the types exist,
send the same body with `PUT` to update them).

### OpenMetadata

| Linneo | OpenMetadata |
|---|---|
| Data category `user.contact.email` | Nested tag `DataCategory.user.contact.email` (classification `DataCategory`, mutually exclusive) |
| Nature | Built-in `PersonalData.Personal` / `PersonalData.SpecialCategory` (special category and criminal offence data) |
| Personal data by confidentiality | Built-in `PII.NonSensitive` below `MEDIUM`, `PII.Sensitive` from `MEDIUM`; `PII.None` when not personal (threshold configurable) |
| Confidentiality, integrity, identifiability | `Confidentiality.*`, `Integrity.*`, `Identifiability.*` |
| Secret | `DataHandling.Secret` |
| Protection implied by a category | On each column, with `tagsOf(field)`; OpenMetadata does not store `associatedTags` |

The PII sensitivity threshold is configurable with
[`OpenMetadataMapping`](docs/en/reference/openmetadata/mapping.md). The request bodies follow the
`CreateClassification` and `CreateTag` schemas of OpenMetadata, including the `parent` field.

## Design principles

- **No runtime dependencies.** The taxonomy is bundled as tab-separated resources and read with the JDK; JSON is
  written by a minimal internal writer.
- **Immutable and thread-safe.** `Taxonomy`, `Classifier` and the catalogs can be shared.
- **Persistence-agnostic.** The classifier reads annotations by reflection and never reads field values; it works
  with JPA entities, records or plain classes.
- **Fail closed.** An unclassified field, an unknown key or a weakened level is an error, not a default.
- **Compare, never persist.** Linneo computes what the code declares and how it differs from a stored copy; reading,
  writing, locking and auditing stay in the application.

## Versioning

Linneo follows [Semantic Versioning](https://semver.org/). For the taxonomy:

- adding a term, or making the protection of a term stricter, is a minor change;
- deprecating a term (with `replacedBy`) is a minor change: code that uses it keeps working;
- removing a term or lowering a protection level is a major change.

A key is never reused for a different meaning. `TaxonomyDiff.requiredBump()` grades the changes between two
taxonomies with these rules, and the build checks them against the taxonomy of the last release. Changes are listed
in the [changelog](CHANGELOG.md).

## Building from source

```bash
git clone https://github.com/devbpm/linneo.git
cd linneo
./mvnw verify      # mvnw.cmd on Windows
./mvnw install     # to use the snapshot from your local Maven repository
```

Requires JDK 21 or later; the Maven wrapper downloads Maven. The build treats every compiler warning as an error and
checks the Javadoc. To refresh the bundled fideslang taxonomy, run `python -I tools/fideslang/update.py <version>`
(see the script's header).

## Contributing

Issues and pull requests are welcome. Please open an issue before changing the taxonomy or the public API, since
applications store and publish its keys. Read [CONTRIBUTING.md](CONTRIBUTING.md) for the rules.

A protection level that is too low for a kind of data is a security issue: report it privately as described in
[SECURITY.md](SECURITY.md), not in a public issue.

## Licence and attribution

Linneo is licensed under the [Apache License, Version 2.0](LICENSE).

It includes the fideslang default taxonomy, version 3.1.4, by Ethyca, Inc.
([github.com/ethyca/fideslang](https://github.com/ethyca/fideslang)), licensed under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), converted to tab-separated values. Linneo's own terms and
the protection levels are not part of fideslang. See [NOTICE](NOTICE) for the full attribution and the list of
changes. Linneo is not affiliated with or endorsed by Ethyca, Inc. or IAB Tech Lab.

The university extension is informed by the concepts of the
[Open Education API (OEAPI) specification](https://github.com/open-education-api/specification), version 6,
Copyright (c) 2017-2026 SURF U.A. Cooperative ([surf.nl](https://surf.nl)), licensed under the
[EUPL-1.2](https://github.com/open-education-api/specification/blob/master/LICENSE.md). No OEAPI material is
reproduced. Linneo is not affiliated with or endorsed by SURF or the OEAPI project.

The finance, supply chain, project operations and human resources extensions are informed by the business areas of
the [Common Data Model](https://github.com/microsoft/CDM) schemas, Copyright (c) Microsoft Corporation, whose
documentation and other content are licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). No CDM
material is reproduced; the names and descriptions of the terms are Linneo's. Linneo is not affiliated with or
endorsed by Microsoft.
