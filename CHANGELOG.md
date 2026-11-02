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
  the warning for a missing legal reference asks for a law with the rank of statute where the LOPDGDD requires one
  (article 8 for a legal obligation or a task in the public interest, 9.2 for GDPR article 9.2 g), h) or i), 10 for
  criminal offence data authorised by law). Data on administrative sanctions without consent or a named law is a
  warning (article 27), and the impact assessment warning cites the AEPD's list under GDPR article 35.4.
- `TaxonomyBuilder` and `TaxonomyExtension` (loaded with `ServiceLoader`) to add terms and make protection stricter.
  Every declared facet is a minimum: two refinements of a category combine into the stricter one in either order,
  and raising a category raises everything below it. An extension adds data categories below existing ones and
  changes the lifecycle of its own terms only, checked when each change is made; a draft changes only inside the
  customizer that received it. The builder refuses what the text form could not hold (a blank origin, attribute name
  or deprecating version), so every taxonomy reads back from its text.
- `Trait`: properties of terms that the legal rules read instead of lists of keys (financial, behavioural and
  private-life data; direct, contact and indirect identifiers; biometric and genetic data; monitoring; vulnerable data
  subjects; evaluation, direct marketing and research purposes). Declared in a resource of the standard taxonomy and
  by each extension (`TermDraft.traits`, `refineDataUse`, `refineDataSubject`), inherited down the tree, carried by
  every replacement, kept in the text form and graded by `TaxonomyDiff` (adding one is minor, removing one major), so
  an application's own terms weigh like standard ones. A test makes every standard term without traits a reviewed
  decision, so a new fideslang term cannot weaken a rule unnoticed. `Taxonomy.resolveDataUse` and `resolveDataSubject` follow replacements as `resolveCategory` does.
- Annotations `@Category`, `@DefaultCategory`, `@CategoryByRow` and `@Exempt`, and the `Classifier` that reads and
  verifies them.
- `OpenMetadataCatalog`: classifications, nested tags, and the tags of each column. Tags carry no
  `associatedTags`, which OpenMetadata accepts but does not store: the protection goes on each column.
- `UniversityTaxonomy`: an optional taxonomy extension for higher education (32 data categories focused on the
  student record, 28 data uses including the new roots `advancement` and `research`, 12 data subjects), informed by
  the Open Education API (OEAPI) and following fideslang's rules for custom labels. Not applied by default; keys in
  `UniversityCategories`, `UniversityDataUses` and `UniversityDataSubjects`.
- Optional business extensions informed by the Common Data Model (CDM) schemas, each in its own exported package
  and not applied by default: `FinanceTaxonomy` (19 data categories, 13 data uses, 3 data subjects),
  `SupplyChainTaxonomy` (16, 12, 3), `ProjectOperationsTaxonomy` (11, 6, 2) and `HumanResourcesTaxonomy` (21, 15,
  6, with the new branch `user.employment` and sick leave, occupational health and workplace adjustments as special
  categories). Records about organizations extend `org.business_records`, records about people extend `user.*`; the
  extensions share no key and can be applied together. A term is placed where the traits it inherits hold: order
  management runs the business (`operations.order_management`), unlike the outreach of `sales`, and only the
  participation in research studies is research, not the administration of projects and grants.
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
  category must protect at least as much as every source (`WEAKER_THAN_SOURCE`); a value declared anonymized or
  aggregated may drop only the personal nature of its sources, never their levels or secrecy. Errors `UNKNOWN_SOURCE`, `AMBIGUOUS_DERIVATION` and `CYCLE`; basis `DERIVED` with
  `FieldClassification.derivedFrom()`. `@Exempt` is only for technical fields: a derived value is data. Sources are
  resolved field by field, so classes may derive from each other's other fields and a composite from a class that
  holds it; only derived fields that depend on each other are a `CYCLE` (found exactly, with Tarjan's algorithm), and
  the result does not depend on the order in which classes are classified.
- `@Composite` for classes that are part of other records, such as an embedded address: an unannotated field of a
  concrete composite type, or an array, collection or `Optional` of one at any depth, is classified by the
  composite's fields with paths such as `home.street`, under the composite's own default. The marker is inherited,
  and type variables are resolved through the superclasses of the class being classified. A field that may hold data
  the classifier cannot see (composites in a map or that it cannot split, an abstract composite, a value typed
  `Object`, `Serializable` or by an unbound type variable) never takes a class default. A field whose type is another
  record of the model takes the class default only when that record holds no personal data and its fields have no
  errors; a reference to a person's record declares what it stores, typically with
  `@DerivedFrom(type = …, value = "id")`, and cannot be exempted. A field that stores composites as one value, declared or derived, must protect at least as much as
  their fields (`WEAKER_THAN_PARTS`), and a composite with an error leaves nothing to compare with; a composite that
  contains itself is a `CYCLE`. A `transient` field is classified when it carries an annotation, and Lombok's lock
  fields are ignored. `Classifier` reads each class once per call and stays immutable.
- `Governance`: the part of a processing activity the data protection function decides (lawful bases, legal
  reference, retention, recipients, transfers, impact assessment), separate from the scope the code declares
  (purposes, data subjects, personal data). `ProcessingActivity.governance()` and `withGovernance(...)` combine what
  an officer keeps in the application's storage with the scope in code; the builder's governance methods give initial
  values. `ProcessingViolation.element()` tells which part a problem is in and `Element.isGovernance()` who should fix
  it, so an application can fail the build on scope errors and show governance gaps where they are edited. The risk
  factors the code knows of are part of the scope (`ProcessingActivity.riskFactors`), joined with those of the
  governance, and an activity may rest on several article 9.2 conditions (`specialCategoryConditions`).
- `ProcessingChecker.checkAll` and `verify` classify each entity once for all the activities.
- `FieldClassification.covers(DataCategory)`: whether a row of a `@CategoryByRow` field may hold a category, and
  whether a classification raised at run time still covers the code's.
- The taxonomy as data: `Taxonomy.toTsv()` writes every term with its effective protection as deterministic
  tab-separated text, `Taxonomy.fromTsv(String)` reads it back through the builder, `fingerprint()` identifies it, and
  two taxonomies with the same terms are equal. `TaxonomyDiff.between(before, after)` lists the `TermChange`s (added,
  removed, protection with its direction, traits, deprecation, replacement, name, description, attributes, origin,
  versions), grades them with `requiredBump()` and refuses incompatible ones with `requireCompatible()`.
  `ReleasedTaxonomyTest` checks Linneo's own taxonomy against the copy kept for the last release of its line and
  that no key removed by any release comes back.
- `DataInventory`: the classification of a model as data, one object per class under a key the application chooses
  (`Classifier.inventory(types, objectKey)`), with what each field declares (`InventoryField`: key, basis, raised
  levels, declared identifiability, reason; exempted fields included), per-object and global fingerprints and a
  deterministic text form, in which lone UTF-16 surrogates are escaped so that different texts never share a
  fingerprint. `InventoryDiff.between(stored, declared, taxonomy)` lists the `FieldChange`s to apply to a stored copy
  and how each moves protection; `weakening()` lists the ones to review. `DataInventory.affectedBy(TaxonomyDiff)`
  gives the fields each changed category reaches, those whose deprecated key it replaces included. Processing
  activities can name inventory objects (`ProcessingActivity.objects`) and be checked with `ProcessingChecker(taxonomy, jurisdiction, inventory)` without
  the model classes. `tools/bench/InventoryBench.java` measures all of it on a model of 800 entities.
- Catalogs from stored records: `OpenMetadataCatalog.tagsOf(InventoryField)`, `DataHubCatalog.termsOf(InventoryField)`
  and `AtlasCatalog.classificationsOf(InventoryField)` give what the `FieldClassification` overloads give for the same
  field, resolved with the catalog's taxonomy, so an application can publish from its inventory and update a column
  with the difference between its earlier and current tags. `InventoryField.identifiability(Taxonomy)`.
- `ProcessingRecord.fingerprint()` and `RecordDiff.between(before, after)`: the `ActivityChange`s between two records
  of processing activities (added, removed, and per aspect: scope, personal data, protection with its direction,
  each governance element, the impact assessment screening, open points by code), so a record rebuilt from what was
  approved can be compared with the current one before it is signed again. A record holds each activity key once and
  lists purposes and data subjects in key order.
- `ModelClasses.inPackage(anchor)` and `inPackageTree(anchor)`: the classes and records of a model package, read
  from where the anchor class was loaded (directory or JAR, class path or module path), so a test verifies every
  class of the model, including the one nobody listed. A location or class that cannot be read is an error, never
  skipped, and says why: a location that does not exist, or a file that is neither a directory nor a JAR, such as a
  source file the `java` launcher runs.
- `DisclosurePolicy`: whether the values of a field may be shown, must be masked or left out (`Treatment`) in a
  channel such as logs, exports or test copies, from thresholds on the `Protection` order, so a field that needs more
  protection never gets a laxer treatment and secrets are always left out; `forLogs()` as a starting point.
- `SubjectRights.of(record, taxonomy, dataSubject, jurisdiction)`: for a request of a data subject, the activities
  that process their data, the categories, the stored fields to search (`storedFields(inventory, taxonomy)`) and, per
  activity, whether access, rectification, erasure, restriction, portability, objection and the safeguards on automated
  decisions apply under its lawful basis (GDPR articles 15 to 22), with the provisions: objection to direct marketing
  (purposes with that trait) is absolute and erases what no legal obligation or other purpose keeps, research has its
  own limits (articles 17.3.d and 21.6), portability excludes tasks in the public interest (article 20.3), and explicit
  consent to special category data gives portability and erasure on withdrawal. The key is resolved with the taxonomy, so a misspelt one is an error. Under
  Spanish law, erased or rectified data is blocked first (LOPDGDD article 32).
- `ImpactScreening`: whether an activity needs a data protection impact assessment, screened against the nine
  criteria of the EDPB guidelines (WP248) and the mandatory cases of GDPR article 35.3, with the evidence of each
  criterion. The criteria the code can see (sensitive or highly personal data, vulnerable data subjects, evaluation
  and monitoring purposes) are found from the traits of the terms, with sorted evidence; the rest the code and the
  data protection officer declare as risk factors. In Spain, biometric and genetic data count as a criterion of their
  own (the AEPD's list). The
  checker reports a mandatory case without an assessment as the error `IMPACT_ASSESSMENT_REQUIRED` and two criteria
  or more without one as the warning `IMPACT_ASSESSMENT_RECOMMENDED`; a single criterion, such as special category
  data alone, no longer raises a warning. The record shows the screening of each activity.
- `BreachAssessment`: the severity of a personal data breach with ENISA's methodology (context × ease of
  identification + circumstances), scored from the classification of the affected fields and the traits of their
  categories (anonymized and aggregated values identify no one, other personal data identifies with effort), and
  whether to notify the supervisory authority (GDPR article 33) and tell the data subjects (article 34), neither of
  which applies when only the confidentiality of unintelligible data was lost. Without personal data the score is 0:
  it is not a personal data breach.
- `EnsCategorization` (package `ens`): the security category of a system under the Spanish Esquema Nacional de
  Seguridad, with confidentiality and integrity valued from the classification and the other dimensions declared.
  Only a classification without errors, or an object a data inventory holds, gives information (an empty inventory
  gives none), and the category
  (`SystemCategory`) is final only once information is given and every other dimension declared.
- Exports to more tools, in the `export` packages: `PostgresAnonymizer` (masking rules for PostgreSQL Anonymizer
  that follow a `DisclosurePolicy`: one statement per column, the value left out in a column shared by fields that
  are not all shown, removals before masks, `withValue` for `NOT NULL` columns, quoted identifiers of at most 63
  bytes), `OpenApiExtension` (the `x-linneo`
  extension of each API property) and `OdcsSchema` (the schema of an Open Data Contract Standard contract from a data
  inventory). An object the inventory does not hold is an error, never an empty output.
- The Java samples of the README and the documentation site are compiled against the library in the build
  (`DocumentationSamplesTest`), type signatures are compared with the real types and module declarations with
  `module-info.java`, and a type no sample declares or imports, such as a renamed one of the library, fails, used as
  a type or as a qualifier. A Javadoc warning fails the build.
- `ClassificationException` and `ProcessingException` list the first 50 errors and count all of them by code, so
  the message of a large model stays readable.
- `AtlasCatalog`: the taxonomy as Apache Atlas classification types (category tree as super types, protection
  levels as separate classifications), and the classifications of each column. `createJson` and `updateJson` publish
  again after the taxonomy gains terms, since Atlas creates a request's types all or none.
