---
title: "Reference"
nav_order: 2
permalink: "/en/reference/"
---
<!--
doc-sources:
  - src/main/java/module-info.java
  - src/main/java/io/github/devbpm/linneo/package-info.java
  - src/main/java/io/github/devbpm/linneo/annotation/package-info.java
  - src/main/java/io/github/devbpm/linneo/classify/package-info.java
  - src/main/java/io/github/devbpm/linneo/processing/package-info.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/package-info.java
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/package-info.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/package-info.java
  - src/main/java/io/github/devbpm/linneo/ens/package-info.java
  - src/main/java/io/github/devbpm/linneo/export/odcs/package-info.java
  - src/main/java/io/github/devbpm/linneo/export/openapi/package-info.java
  - src/main/java/io/github/devbpm/linneo/export/postgresql/package-info.java
  - src/main/java/io/github/devbpm/linneo/internal/Cli.java
  - src/main/java/io/github/devbpm/linneo/extension/university/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/finance/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/package-info.java
-->

# Linneo reference

Linneo is a Java 21+ library with no runtime dependencies that bundles a data classification taxonomy and applies it
to a data model. A `Taxonomy` holds three hierarchies of terms keyed with dot notation: **data categories** (what a
field holds), **data uses** (why it is processed) and **data subjects** (whose data it is). The standard taxonomy is
the fideslang default taxonomy plus Linneo's extensions for non-personal and education data. Every data category
carries the protection it requires (legal nature, confidentiality, integrity, secret), which children inherit and may
only make stricter. Models declare what they hold with annotations, a `Classifier` checks those annotations in tests,
processing activities tie purposes, data subjects and personal data to their lawful bases and produce the GDPR record
of processing activities (and screens each activity for the need of an impact assessment), and three exporters
translate the taxonomy for a data catalogue: `OpenMetadataCatalog` (OpenMetadata classifications and tags),
`DataHubCatalog` (a DataHub business glossary) and `AtlasCatalog` (Apache Atlas classifications). Three more exporters
publish the classification of a model's fields to the tools beside the code: an OpenAPI extension, masking rules for
PostgreSQL Anonymizer and the schema of an Open Data Contract Standard data contract. Optional helpers orient the
severity of a personal data breach and the security category of a system under the Spanish ENS, and the JAR has a
command line.
Applications add their own terms through a `TaxonomyExtension`, and Linneo ships five optional ones (universities,
finance, supply chain, project operations and human resources) that are applied only on request.

This reference describes every public type. For a guided introduction, see the [course](../course/README.md).

## Module and packages

The JAR is the named JPMS module `io.github.devbpm.linneo`:

```java
module io.github.devbpm.linneo {
    exports io.github.devbpm.linneo;
    exports io.github.devbpm.linneo.annotation;
    exports io.github.devbpm.linneo.classify;
    exports io.github.devbpm.linneo.processing;
    exports io.github.devbpm.linneo.catalog.openmetadata;
    exports io.github.devbpm.linneo.catalog.datahub;
    exports io.github.devbpm.linneo.catalog.atlas;
    exports io.github.devbpm.linneo.ens;
    exports io.github.devbpm.linneo.export.odcs;
    exports io.github.devbpm.linneo.export.openapi;
    exports io.github.devbpm.linneo.export.postgresql;
    exports io.github.devbpm.linneo.extension.university;
    exports io.github.devbpm.linneo.extension.finance;
    exports io.github.devbpm.linneo.extension.supplychain;
    exports io.github.devbpm.linneo.extension.projectoperations;
    exports io.github.devbpm.linneo.extension.humanresources;

    uses io.github.devbpm.linneo.TaxonomyExtension;
}
```

| Package | Exported | Contents |
|---|---|---|
| `io.github.devbpm.linneo` | yes | The taxonomy (`Taxonomy`, `TaxonomyBuilder`, `TaxonomyExtension`), the differences between two versions of it (`TaxonomyDiff`, `TermChange`), its terms (`Term`, `TermInfo`, `DataCategory`, `DataUse`, `DataSubject`), the protection enums and `Protection`, and the key constants (`Categories`, `DataUses`, `DataSubjects`) |
| `io.github.devbpm.linneo.annotation` | yes | The annotations that declare what each field of a model holds: `@Category`, `@DefaultCategory`, `@CategoryByRow`, `@DerivedFrom`, `@Composite`, `@Exempt` |
| `io.github.devbpm.linneo.classify` | yes | Reading and verifying the annotations of a model: `Classifier`, `ClassClassification`, `FieldClassification`, `Violation`, `ClassificationException`, `ModelClasses`; what may leave the system for a field: `DisclosurePolicy`; and the classification kept as data that an application can store and compare: `DataInventory`, `InventoryField`, `InventoryDiff`, `FieldChange` |
| `io.github.devbpm.linneo.processing` | yes | Processing activities and the GDPR record of processing activities: `ProcessingActivity`, `Governance`, `Transfer`, `LegalBasis`, `SpecialCategoryCondition`, `CriminalDataBasis`, `TransferSafeguard`, `ProcessingChecker`, `CheckedActivity`, `ProcessingViolation`, `ProcessingException`, `ProcessingRecord`, `RecordDiff`, `ActivityChange`, `SubjectRights`; the screening of the need for an impact assessment, `ImpactScreening`; and the severity of a personal data breach, `BreachAssessment` |
| `io.github.devbpm.linneo.catalog.openmetadata` | yes | The taxonomy as OpenMetadata classifications and tags: `OpenMetadataCatalog`, `OpenMetadataMapping`, `OmClassification`, `OmTag`. No network calls |
| `io.github.devbpm.linneo.catalog.datahub` | yes | The taxonomy as a DataHub business glossary: `DataHubCatalog`, `DataHubMapping`, `DhProposal`. No network calls |
| `io.github.devbpm.linneo.catalog.atlas` | yes | The taxonomy as Apache Atlas classifications: `AtlasCatalog`, `AtlasMapping`, `AtlasClassificationDef`. No network calls |
| `io.github.devbpm.linneo.ens` | yes | The security category of a system under the Spanish ENS (Real Decreto 311/2022): `EnsCategorization` |
| `io.github.devbpm.linneo.export.openapi` | yes | The classification of an API's data as the `x-linneo` OpenAPI extension: `OpenApiExtension` |
| `io.github.devbpm.linneo.export.postgresql` | yes | Masking rules for PostgreSQL Anonymizer that follow a disclosure policy: `PostgresAnonymizer`. It connects to no database |
| `io.github.devbpm.linneo.export.odcs` | yes | The classification of a data inventory as the `schema` of an Open Data Contract Standard data contract: `OdcsSchema` |
| `io.github.devbpm.linneo.extension.university` | yes | An optional extension for universities, not applied by default: `UniversityTaxonomy` and its key constants `UniversityCategories`, `UniversityDataUses`, `UniversityDataSubjects` |
| `io.github.devbpm.linneo.extension.finance` | yes | An optional extension for finance and accounting, not applied by default: `FinanceTaxonomy` and its key constants `FinanceCategories`, `FinanceDataUses`, `FinanceDataSubjects` |
| `io.github.devbpm.linneo.extension.supplychain` | yes | An optional extension for supply chain management, not applied by default: `SupplyChainTaxonomy` and its key constants `SupplyChainCategories`, `SupplyChainDataUses`, `SupplyChainDataSubjects` |
| `io.github.devbpm.linneo.extension.projectoperations` | yes | An optional extension for project operations, not applied by default: `ProjectOperationsTaxonomy` and its key constants `ProjectOperationsCategories`, `ProjectOperationsDataUses`, `ProjectOperationsDataSubjects` |
| `io.github.devbpm.linneo.extension.humanresources` | yes | An optional extension for human resources and payroll, not applied by default: `HumanResourcesTaxonomy` and its key constants `HumanResourcesCategories`, `HumanResourcesDataUses`, `HumanResourcesDataSubjects` |
| `io.github.devbpm.linneo.internal` | no | `StandardTaxonomy`, which loads the bundled TSV resources, `Json`, the minimal JSON writer shared by the exporters, and the [command line](command-line.md) of the JAR. Not part of the public API |

The `uses` clause lets `Taxonomy.load()` and `TaxonomyBuilder.withExtensions()` find extensions with
`java.util.ServiceLoader`; see [Extending the taxonomy](extending.md). The module has no `provides` clause: the
extensions it ships are applied explicitly; see [Extensions](extensions/README.md).

To use the library from a modular application:

```java
module com.example.app {
    requires io.github.devbpm.linneo;
}
```

## Pages

**Taxonomy model** (`io.github.devbpm.linneo`)

- [Taxonomy](taxonomy.md) — `Taxonomy` (`standard`, `load`, `builder`, lookups, deprecation, navigation, text form and
  fingerprint) and `TaxonomyException`.
- [Terms](terms.md) — `Term`, `TermInfo`, `DataCategory`, `DataUse`, `DataSubject`.
- [Protection levels](protection-levels.md) — `LegalNature`, `Confidentiality`, `Integrity`, `Identifiability`,
  `Protection` and the inheritance rule.
- [Standard taxonomy](standard-taxonomy.md) — the bundled fideslang taxonomy, Linneo's additions and protection
  policy, the TSV formats.
- [Key constants](key-constants.md) — `Categories`, `DataUses`, `DataSubjects`.
- [Extending the taxonomy](extending.md) — `TaxonomyBuilder`, `TaxonomyExtension`, `CategoryDraft`, `TermDraft`.
- [Taxonomy diff](taxonomy-diff.md) — `TaxonomyDiff`, `TermChange`: what changed between two versions of a taxonomy,
  and the versioning rules that grade it.

**Extensions** (`io.github.devbpm.linneo.extension.university`, `.finance`, `.supplychain`, `.projectoperations`,
`.humanresources`)

- [Extensions overview](extensions/README.md) — the optional extensions shipped with Linneo, how to apply or
  register them and how they combine with yours.
- [University](extensions/university.md) — `UniversityTaxonomy`, `UniversityCategories`, `UniversityDataUses`,
  `UniversityDataSubjects`: every term with its protection, the standard keys to use, routing rules and sources.
- [Finance](extensions/finance.md) — `FinanceTaxonomy`, `FinanceCategories`, `FinanceDataUses`, `FinanceDataSubjects`.
- [Supply chain](extensions/supply-chain.md) — `SupplyChainTaxonomy`, `SupplyChainCategories`,
  `SupplyChainDataUses`, `SupplyChainDataSubjects`.
- [Project operations](extensions/project-operations.md) — `ProjectOperationsTaxonomy`,
  `ProjectOperationsCategories`, `ProjectOperationsDataUses`, `ProjectOperationsDataSubjects`.
- [Human resources](extensions/human-resources.md) — `HumanResourcesTaxonomy`, `HumanResourcesCategories`,
  `HumanResourcesDataUses`, `HumanResourcesDataSubjects`.

**Annotations** (`io.github.devbpm.linneo.annotation`)

- [Annotations overview](annotations/README.md) — precedence and conflicts.
- [`@Category`](annotations/category.md)
- [`@DefaultCategory`](annotations/default-category.md)
- [`@CategoryByRow`](annotations/category-by-row.md)
- [`@DerivedFrom`](annotations/derived-from.md)
- [`@Composite`](annotations/composite.md)
- [`@Exempt`](annotations/exempt.md)

**Classification** (`io.github.devbpm.linneo.classify`)

- [Classifier](classify/classifier.md) — `Classifier` (including `inventory`), `ClassificationException`,
  `ModelClasses`.
- [Results](classify/results.md) — `ClassClassification` (with `Exemption`), `FieldClassification`.
- [Violations](classify/violations.md) — `Violation` and every `Code`.
- [Disclosure policy](classify/disclosure.md) — `DisclosurePolicy` and its `Treatment`: whether the value of a field
  is shown, masked or left out in a log, an export or an API; [`PostgresAnonymizer`](export/postgresql.md) turns it
  into database masking rules.
- [Data inventory](classify/inventory.md) — `DataInventory`, `InventoryField`, `InventoryDiff`, `FieldChange`: the
  classification as data, its text form and fingerprints, and the comparison of two inventories.

**Processing activities** (`io.github.devbpm.linneo.processing`)

- [Processing activities overview](processing/README.md) — how activities relate to the taxonomy and the
  classifier, errors and warnings.
- [ProcessingActivity](processing/activity.md) — `ProcessingActivity`, its `Builder`, `Governance`, `Transfer`.
- [Legal bases](processing/bases.md) — `LegalBasis`, `SpecialCategoryCondition`, `CriminalDataBasis`,
  `TransferSafeguard`.
- [ProcessingChecker](processing/checker.md) — `ProcessingChecker`, `CheckedActivity`, `ProcessingViolation` with
  every `Code` and `Element`, `ProcessingException`.
- [ProcessingRecord](processing/record.md) — the record as Markdown and JSON, its fingerprint, `RecordDiff` and
  `ActivityChange`.
- [SubjectRights](processing/subject-rights.md) — `SubjectRights`: the activities that process the data of a category
  of data subjects and the rights their lawful basis gives them.
- [ImpactScreening](processing/impact-screening.md) — `ImpactScreening`, `Criterion`, `Factor`, `Outcome`: whether an
  activity needs a data protection impact assessment, from the criteria of the EDPB guidelines and article 35.3.
- [BreachAssessment](processing/breach-assessment.md) — `BreachAssessment`: the severity of a personal data breach under
  ENISA's methodology, and what the GDPR asks of the controller.

**Spanish ENS** (`io.github.devbpm.linneo.ens`)

- [ENS categorization](ens.md) — `EnsCategorization`: the security category of a system from the classification of its
  data and the dimensions its owner declares.

**OpenMetadata** (`io.github.devbpm.linneo.catalog.openmetadata`)

- [Catalog](openmetadata/catalog.md) — `OpenMetadataCatalog`, `OmClassification`, `OmTag`, JSON bodies, tag mapping.
- [Mapping](openmetadata/mapping.md) — `OpenMetadataMapping`.

**DataHub** (`io.github.devbpm.linneo.catalog.datahub`)

- [Catalog](datahub/catalog.md) — `DataHubCatalog`, `DhProposal`, glossary nodes and terms, `ingestProposal` bodies,
  field terms.
- [Mapping](datahub/mapping.md) — `DataHubMapping`.

**Apache Atlas** (`io.github.devbpm.linneo.catalog.atlas`)

- [Catalog](atlas/catalog.md) — `AtlasCatalog`, `AtlasClassificationDef`, classification types, typedefs and
  classification bodies.
- [Mapping](atlas/mapping.md) — `AtlasMapping`.

**Export** (`io.github.devbpm.linneo.export.openapi`, `.postgresql`, `.odcs`)

- [Export overview](export/README.md) — what the three exporters have in common.
- [OpenAPI extension](export/openapi.md) — `OpenApiExtension`: the `x-linneo` extension of each property of an API
  description.
- [PostgreSQL Anonymizer](export/postgresql.md) — `PostgresAnonymizer`: the `SECURITY LABEL` statements that mask or
  null each column, following a disclosure policy.
- [Open Data Contract Standard](export/odcs.md) — `OdcsSchema`: the `schema` of a data contract from a data inventory.

**Command line**

- [Command line](command-line.md) — `java -jar linneo.jar`: write the taxonomy, compare taxonomies and inventories and
  write the catalog requests, with exit codes for a pipeline.

## Data catalogues compared

The three exporters publish the same taxonomy and compute the labels of each classified field from the same
`FieldClassification`; they differ in what the catalogue offers. None of them makes network calls or applies the
labels to your tables: they build request bodies and names.

| | OpenMetadata | DataHub | Apache Atlas |
|---|---|---|---|
| Entry point | [`OpenMetadataCatalog`](openmetadata/catalog.md) | [`DataHubCatalog`](datahub/catalog.md) | [`AtlasCatalog`](atlas/catalog.md) |
| A category becomes | a nested tag `DataCategory.<key>` in a mutually exclusive classification | a glossary term `urn:li:glossaryTerm:DataCategory.<key>`, plus a glossary node if it has children | a classification type `DataCategory.<key>` |
| The tree becomes | tag `parent` | nodes that mirror the tree, and `isRelatedTerms` to the parent's term | `superTypes`: a search for a parent finds its sub-classifications |
| Protection implied by a category | not stored in the tag; goes on each column with `tagsOf(field)` (`tagsOf(category)` lists it) | `relatedTerms` of its term | not stored in the type; `classificationsOf(category)` lists it |
| Legal nature | built-in `PersonalData.Personal` / `SpecialCategory` (criminal offence data is `SpecialCategory`) | `PersonalData.Personal` / `SpecialCategory` / `CriminalOffence` | `PersonalData.Personal` / `SpecialCategory` / `CriminalOffence` |
| Sensitivity | built-in `PII.None` / `NonSensitive` / `Sensitive`, threshold `sensitiveFrom` | — | — |
| Levels and secret | `Confidentiality.*`, `Integrity.*`, `Identifiability.*`, `DataHandling.Secret` | the same, as glossary terms | the same, as classifications (not super types of the categories) |
| Created for the standard taxonomy | 5 classifications and 138 tags | 295 proposals: 28 nodes, 141 terms, 126 relation aspects | 147 classification types |
| Requests | `PUT /api/v1/classifications`, `PUT /api/v1/tags`, one per body | `POST /aspects?action=ingestProposal`, one per proposal | one `POST /api/atlas/v2/types/typedefs` |
| Sending again | create or update | `UPSERT` (replaces each aspect) | `POST` answers 409 if the types exist; update with `PUT` |
| Labels of a field | `tagsOf(field)`: tag names | `termsOf(field)`: term URNs | `classificationsOf(field)`: names; `classificationsJson(field)`: request body |
| Applying them (your code) | the table API you already use | for example GraphQL `addTerms` with the field path as `subResource` | `POST /api/atlas/v2/entity/guid/{guid}/classifications` |
| Names | [`OpenMetadataMapping`](openmetadata/mapping.md): seven names and a threshold | [`DataHubMapping`](datahub/mapping.md): six node names | [`AtlasMapping`](atlas/mapping.md): six root names |

See also: [course](../course/README.md) · [Español](../../es/reference/README.md)
