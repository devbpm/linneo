# Changelog

All notable changes to this project are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Standard taxonomy: the fideslang 3.1.4 default taxonomy (85 data categories, 56 data uses, 15 data subjects)
  plus Linneo extensions for education data (`user.education.*`, `education.*`, `student`, `applicant`…),
  workplace detail, nationality, disability, activity logs, the GDPR article 9 categories fideslang lacks
  (philosophical belief, trade union membership, sex life), and non-personal data (`system.security`,
  `system.integrity`, `system.configuration`, `system.authentication.secret`, `org.*`, `reference.*`).
- Protection facets on every data category: legal nature, confidentiality, integrity, secret and inherited
  attributes, with Linneo's default policy based on the GDPR (as corrected in 2018) and the Spanish LOPDGDD
  (consolidated text of 27 December 2025). Special category and criminal offence data always have `HIGH`
  confidentiality and integrity (`LegalNature.requiresMaximumProtection()`, enforced by `TaxonomyBuilder`); data of
  vulnerable data subjects (`user.childrens`, `user.government_id.immigration`) has `HIGH` confidentiality.
- `TaxonomyBuilder` refuses a deprecated data category whose replacement protects less (a nature it does not
  admit, lower confidentiality or integrity, or not secret), since every use of the deprecated key is checked with
  the replacement.
- Integration tests against real servers, skipped in the normal build: `OpenMetadataIT` (checked with
  OpenMetadata 2.0.6), `DataHubIT` (DataHub v1.7.0.1) and `AtlasIT` (Apache Atlas 2.3.0). Each publishes the
  standard taxonomy with the university extension, reads it back, and checks the tags, terms or classifications
  stored on columns.
- Linear-time handling of long inputs: Markdown table cells no longer use a backtracking regular expression, and
  key validation uses possessive quantifiers, so very long text or keys cannot slow down or overflow the stack.
- `Classifier` ignores fields that bytecode tools add (names starting with `$$_`, such as Hibernate's enhancement
  fields) and classifies a Hibernate proxy class as the entity it extends.
- Data categories `user.administrative_sanction` (administrative infringements and sanctions, LOPDGDD article 27)
  and `user.victim_status` (victims of gender-based violence, sexual violence, terrorism and other offences):
  ordinary personal data with `HIGH` confidentiality and integrity.
- `Jurisdiction` and `ProcessingChecker(Taxonomy, Jurisdiction)`: with `Jurisdiction.SPAIN` the checker applies the
  LOPDGDD on top of the GDPR. Explicit consent as the only condition for ideology, trade union membership, religion,
  sexual orientation, beliefs or racial or ethnic origin is an error (`CONSENT_NOT_SUFFICIENT`, article 9.1), and
  the warning for a missing legal reference under GDPR article 9.2 g), h) or i) asks for a law with the rank of
  statute (article 9.2).
- `TaxonomyBuilder` and `TaxonomyExtension` (loaded with `ServiceLoader`) to add terms and make protection stricter.
- Annotations `@Category`, `@DefaultCategory`, `@CategoryByRow` and `@Exempt`, and the `Classifier` that reads and
  verifies them.
- `OpenMetadataCatalog`: classifications, nested tags, and the tags of each column. Tags carry no
  `associatedTags`, which OpenMetadata accepts but does not store: the protection goes on each column.
- `UniversityTaxonomy`: an optional taxonomy extension for higher education (32 data categories focused on the
  student record, 27 data uses including the new roots `advancement` and `research`, 12 data subjects), informed by
  the Open Education API (OEAPI) and following fideslang's rules for custom labels. Not applied by default; keys in
  `UniversityCategories`, `UniversityDataUses` and `UniversityDataSubjects`.
- Optional business extensions informed by the Common Data Model (CDM) schemas, each in its own exported package
  and not applied by default: `FinanceTaxonomy` (19 data categories, 13 data uses, 3 data subjects),
  `SupplyChainTaxonomy` (16, 12, 3), `ProjectOperationsTaxonomy` (11, 6, 2) and `HumanResourcesTaxonomy` (21, 15,
  6, with the new branch `user.employment` and sick leave, occupational health and workplace adjustments as special
  categories). Records about organizations extend `org.business_records`, records about people extend `user.*`; the
  extensions share no key and can be applied together.
- `user.contact.emergency_contact` and the data subject `emergency_contact` are standard terms, available to every
  extension (they were part of the university extension during development).
- `ProcessingChecker`, `ProcessingActivity` and `ProcessingRecord`: GDPR processing activities (purposes, data
  subjects, personal data from category keys and annotated entities, lawful basis, article 9.2 and 10 conditions,
  retention, recipients, transfers, impact assessment) checked against the taxonomy and the classification, and the
  article 30 record of processing activities as Markdown or JSON. Vocabularies aligned with fideslang's privacy
  declarations.
- `DataHubCatalog`: the taxonomy as a DataHub business glossary (`ingestProposal` bodies for glossary nodes and
  terms, with related terms for the parent and the implied protection), and the glossary terms of each field.
- `Protection`: the protection some data requires (its strictest legal natures, confidentiality, integrity and
  secret flag), partially ordered by `covers`, with `join` and `comparedTo` (`Protection.Direction`: `SAME`,
  `TIGHTENED`, `LOOSENED`, `MIXED`). It is the one comparison the library uses for protection. `LegalNature.covers`
  orders the natures; special category and criminal offence data are both stricter than ordinary personal data and
  not comparable with each other. Available as `DataCategory.protection()`, `FieldClassification.protection()`
  (anonymized and aggregated values have the nature `NONE`), `ClassClassification.ceiling()` and `ceilingOf(path)`,
  and `CheckedActivity.protection()`.
- Fields are identified by their path (`FieldClassification.path()`, `Violation.path()`): a field that hides a
  superclass field of the same name is a `DUPLICATE_FIELD` error, and `ClassClassification.exempted()` lists the
  fields excluded with `@Exempt`, so excluding a field is visible.
- `@DerivedFrom` for values computed from other fields, of the same class or of another (a view or data transfer
  object built from an entity): without `@Category` the field takes the classification of its source; with it, the
  category must protect at least as much as every source (`WEAKER_THAN_SOURCE`), unless the value is declared
  anonymized or aggregated. Errors `UNKNOWN_SOURCE`, `AMBIGUOUS_DERIVATION` and `CYCLE`; basis `DERIVED` with
  `FieldClassification.derivedFrom()`. `@Exempt` is only for technical fields: a derived value is data.
- `@Composite` for classes that are part of other records, such as an embedded address: an unannotated field of a
  composite type, or an array, collection or `Optional` of one, is classified by the composite's fields with paths
  such as `home.street`, under the composite's own default. A composite stored as one value must protect at least
  as much as its fields (`WEAKER_THAN_PARTS`); a composite that contains itself is a `CYCLE`. `Classifier` reads each
  class once per call and stays immutable.
- `ClassificationException` and `ProcessingException` list the first 50 errors and count all of them by code, so
  the message of a large model stays readable.
- `AtlasCatalog`: the taxonomy as Apache Atlas classification types (category tree as super types, protection
  levels as separate classifications), and the classifications of each column.
