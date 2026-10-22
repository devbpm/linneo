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
- `UniversityTaxonomy`: an optional taxonomy extension for higher education (33 data categories focused on the
  student record, 27 data uses including the new roots `advancement` and `research`, 13 data subjects), informed by
  the Open Education API (OEAPI) and following fideslang's rules for custom labels. Not applied by default; keys in
  `UniversityCategories`, `UniversityDataUses` and `UniversityDataSubjects`.
- `ProcessingChecker`, `ProcessingActivity` and `ProcessingRecord`: GDPR processing activities (purposes, data
  subjects, personal data from category keys and annotated entities, lawful basis, article 9.2 and 10 conditions,
  retention, recipients, transfers, impact assessment) checked against the taxonomy and the classification, and the
  article 30 record of processing activities as Markdown or JSON. Vocabularies aligned with fideslang's privacy
  declarations.
- `DataHubCatalog`: the taxonomy as a DataHub business glossary (`ingestProposal` bodies for glossary nodes and
  terms, with related terms for the parent and the implied protection), and the glossary terms of each field.
- `AtlasCatalog`: the taxonomy as Apache Atlas classification types (category tree as super types, protection
  levels as separate classifications), and the classifications of each column.
