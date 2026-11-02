# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Linneo is a zero-runtime-dependency Java 21+ library (JPMS module `io.github.devbpm.linneo`) that bundles a data
classification taxonomy (the fideslang default taxonomy plus Linneo's own terms), lets applications annotate model
fields with categories, verifies those annotations in tests, checks GDPR processing activities and builds the
article 30 record, and exports the taxonomy to OpenMetadata, DataHub and Apache Atlas. See `README.md` for the user-facing API.

## Build and test

```bash
./mvnw verify                                   # full build: compile, tests, javadoc (what CI runs on JDK 21 and 25)
./mvnw test -Dtest=ClassifierTest               # one test class
./mvnw test -Dtest=ClassifierTest#methodName    # one test method
```

On Windows use `mvnw.cmd`. The build is strict, so "it compiles in the IDE" is not enough:

- `maven-compiler-plugin` runs `-Xlint:all` with `failOnWarning` — any lint warning fails the build.
- `maven-javadoc-plugin` runs with `doclint` `all` during `verify` — every public type/member needs complete Javadoc
  (new public types also get `@since`).
- The enforcer requires JDK 21+ and Maven 3.9+.
- The only dependency is JUnit Jupiter (test scope). Do not add runtime dependencies; JSON output uses the internal
  minimal writer `internal/Json.java`.
- Every source file starts with the Apache License header.

## Architecture

Packages are organised by role and audience, not by taxonomy dimension (categories, uses and subjects share the
same `Term` model). Sixteen exported packages plus one internal one (not exported in `module-info.java`):

- **`io.github.devbpm.linneo`** — the taxonomy model, protection enums, key constants, `TaxonomyBuilder`, the
  drafts and `TaxonomyExtension`. `Protection` is the single partial order for comparing protection (`covers`,
  `join`, `comparedTo`); use it instead of comparing natures and levels by hand. `Trait` holds the properties of terms
  the legal rules read (financial, behavioural, identifiers, biometric, genetic, monitoring, vulnerable, evaluation,
  direct marketing, research): rules test `term.has(Trait.X)`, never lists of key prefixes, so an extension's terms
  count once they declare traits; a replacement carries every trait of the term it replaces. A `Taxonomy` is also
  data:
  `toTsv`/`fromTsv` (versioned format, read back through the builder), `fingerprint`, value equality, and
  `TaxonomyDiff`/`TermChange` grade the changes between two versions (`requiredBump`). `ReleasedTaxonomyTest`
  compares the shipped taxonomy with the copy of the last release in `src/test/resources/.../released/`. Terms are `DataCategory`, `DataUse`, `DataSubject` (all `Term`s), keyed by
  dotted strings whose parent is the key prefix (`user.contact.email` → `user.contact`). The builder and drafts
  must stay in this package: they rely on package-private access (the `Taxonomy` constructor, `TermDraft.info()`,
  `CategoryDraft.declared*()`) so that only `build()` can create a validated `Taxonomy`.
- **`annotation`** — `@Category`, `@DefaultCategory`, `@CategoryByRow`, `@DerivedFrom`, `@Composite`, `@Exempt`.
- **`internal.StandardTaxonomy`** — loads the bundled TSV resources from
  `src/main/resources/io/github/devbpm/linneo/internal/` into a `TaxonomyBuilder`:
  - `fideslang-data-{categories,uses,subjects}.tsv` — fideslang verbatim (version in `FIDESLANG_VERSION`).
  - `linneo-data-{categories,uses,subjects}.tsv` — Linneo's added terms (never redefine fideslang keys).
  - `linneo-category-policy.tsv` — protection facets (nature, confidentiality, integrity, secret) per category key;
    fideslang itself defines no protection levels.
  - `linneo-traits.tsv` — the `Trait`s of standard terms (kind, key, traits), with the legal sources in its header;
    each shipped extension declares its own terms' traits in a `traits(builder)` method. `TraitCoverageTest` makes
    every standard term without traits a decision listed in `src/test/resources/.../traits-reviewed.txt`.
- **`TaxonomyBuilder`** — the single place where taxonomies are assembled and validated. `Taxonomy.standard()` =
  builder + standard; `Taxonomy.load()` additionally applies `TaxonomyExtension`s found via `ServiceLoader`
  (sorted by name for determinism). Every declared facet is a **minimum**: refinements keep the stricter value in
  any order, and `build()` resolves categories parent-first: roots must declare all facets, children inherit unset
  facets and a lower declared level is raised to the parent's (`TaxonomyBuilderTest` checks that the bundled terms
  declare none); a nature that cannot be reconciled (personal vs not, special vs criminal) throws
  `TaxonomyException`; `secret` and traits are OR-ed down the tree. While an extension contributes it cannot add a
  root category, change the origin, or change the lifecycle of terms it did not add. `replaced_by` targets of
  deprecated terms are checked here too. The built `Taxonomy` is immutable.
- **`classify`** — `Classifier` reflects over a class hierarchy's fields (never reads values), resolves each field
  through its annotations, a `@Composite` type (one entry per part, path `field.part`), a `@DerivedFrom` source or a
  class-level `@DefaultCategory`, and collects `Violation`s. Each call runs a private `Run` that collects every
  class once and derives a derived field only when something needs it, across classes (an embedded composite links
  its derived parts instead of deriving them). All derivations of a run are one depth-first search where Tarjan's
  algorithm marks exactly the fields in a cycle, so results do not depend on the order of the classes, and problems
  are sorted in field order. The `Classifier` itself stays immutable; it also records what each field declares
  (`Classifier.Declared`) for the data inventory. `Classifier.inventory` builds a `DataInventory` (objects under keys
  the application chooses, `InventoryField`s with the declared key and raised levels, never the taxonomy's levels,
  so a taxonomy change never changes an inventory); `InventoryDiff` compares two inventories under one taxonomy and
  `DataInventory.affectedBy(TaxonomyDiff)` gives the fields a taxonomy change reaches; the catalogs' `InventoryField`
  overloads publish a stored field exactly as its `FieldClassification` (`InventoryPublishingTest`). Linneo
  compares and never persists: reading, writing, locking and auditing a stored copy belong to the application.
  `ModelClasses` lists the classes of a model package from the anchor class's location (fails rather than skips);
  `DisclosurePolicy` maps a field's `Protection` to CLEAR/MASK/OMIT through thresholds, monotone by construction
  (`DisclosurePolicyTest` checks it over every protection), secrets always OMIT.
  `tools/bench/InventoryBench.java` measures it on 800 entities. `classify` returns a
  `ClassClassification`; `verify` throws `ClassificationException` listing all errors. Design is fail-closed:
  unclassified fields, unknown keys, weakened levels, personal categories used as class defaults and conflicting
  annotations are errors; deprecated keys are warnings resolved to their replacement. Composites are found through
  arrays, collections and optionals at any depth, subclasses and type variables bound by superclasses; a field that
  may hold data the classifier cannot see (composites in a map or that cannot be split, an abstract composite,
  `Object`, `Serializable`, an unbound type variable) never takes a class default. A field whose type is another
  record of the model (`Known.CLASSIFIED`, memoized per class) is a reference: the class default stands for it only
  when that record holds no personal data, checked at the end like a composite stored whole. The library is a
  classifier and an aid: rules close the holes a correct model could fall into, without demanding annotations for
  cases that only misuse reaches.
- **`processing`** — GDPR processing activities. `ProcessingActivity` (builder) declares purposes (data use keys),
  data subjects, personal data (category keys and/or annotated entity classes) and the lawful bases
  (`LegalBasis`, `SpecialCategoryCondition`, `CriminalDataBasis`, `TransferSafeguard`, with fideslang's values).
  The bases, retention, recipients, transfers and impact assessment form the activity's `Governance`, owned by the
  data protection function and combined with the code's scope through `withGovernance`; every
  `ProcessingViolation` names its `Element`, which says whether the problem is in the scope or in the governance.
  `ProcessingChecker` resolves them against the taxonomy, runs the `Classifier` on the entities and reports
  `ProcessingViolation`s with the same fail-closed split as `classify`: unlawful-as-declared is an error, a gap in
  the record is a warning. `verify` returns a `ProcessingRecord` (article 30 record, `toMarkdown`/`toJson`, unique
  activity keys). `RecordDiff` compares two records into `ActivityChange`s; it and `ProcessingRecord.fingerprint()`
  both use `ActivityChange.content`, so equal fingerprints always mean an empty diff. `SubjectRights` derives, for
  one data subject category, the activities, stored fields and the rights of articles 15-21 per lawful basis; its
  legal rules live in `SubjectRights.entitlements` and cite the articles in their explanations. `ImpactScreening`
  (WP248 criteria from the traits of the activity's terms plus the risk factors of the scope,
  `ProcessingActivity.riskFactors`, and of the governance) drives the checker's impact assessment error and warning; `BreachAssessment` follows ENISA's 2013
  severity methodology. Legal rules are fixed and cited, not
  configurable: what the code cannot see is declared, never guessed.
- **`ens`** — `EnsCategorization`, the Spanish ENS category of a system: confidentiality and integrity from the
  classification (only raised by declarations), authenticity, traceability and availability declared.
- **`export.postgresql`**, **`export.openapi`**, **`export.odcs`** — `PostgresAnonymizer`, `OpenApiExtension` and
  `OdcsSchema` write text for other tools (masking rules, an API extension, a data contract schema), like the
  catalogs, and make no connection.
- **`extension.university`** — `UniversityTaxonomy`, an optional `TaxonomyExtension` for higher education with its
  key constants (`UniversityCategories`, `UniversityDataUses`, `UniversityDataSubjects`). Deliberately **not**
  declared as a `provides` service in `module-info.java`, so `Taxonomy.load()` never applies it; users call
  `builder.extend(new UniversityTaxonomy())` or register it themselves. Its keys are hand-written constants (not
  generated); `UniversityTaxonomyTest` checks they match exactly the terms the extension adds. If a future standard
  term reuses one of its keys the build fails with a duplicate key: move the term to the standard and drop it here.
- **`extension.finance`**, **`extension.supplychain`**, **`extension.projectoperations`**,
  **`extension.humanresources`** — four more optional extensions built the same way (`<Domain>Taxonomy` plus
  `<Domain>Categories`/`DataUses`/`DataSubjects`), informed by the Common Data Model business areas. Organization
  records extend `org.business_records`, personal data extends `user.*`. They must stay independent: no extension
  may use another extension's key as a parent, and no two extensions may add the same key (`ExtensionsTest` applies
  all of them together and checks the shared conventions; a term two extensions need belongs in the standard, as
  `user.contact.emergency_contact` does).
- **`catalog.openmetadata`** — `OpenMetadataCatalog` maps a `Taxonomy` to OpenMetadata `CreateClassification`/`CreateTag`
  bodies (`OmClassification`, `OmTag`, parents first) and computes per-column tags, reusing OpenMetadata's built-in
  `PersonalData` and `PII` classifications. Names are configurable via `OpenMetadataMapping`. No HTTP calls.
- **`catalog.datahub`** and **`catalog.atlas`** — the same idea for DataHub (`DataHubCatalog`: glossary nodes/terms as
  `ingestProposal` bodies, `termsOf(field)`) and Apache Atlas (`AtlasCatalog`: classification typedefs with the
  category tree as `superTypes`, `classificationsOf(field)`). All three exporters share `internal.Json`, a minimal
  JSON writer, and make no HTTP calls.

## Generated code and taxonomy changes

`Categories.java`, `DataUses.java` and `DataSubjects.java` (the compile-time key constants used in annotations) are
**generated** by `tools/fideslang/update.py`, together with the `fideslang-*.tsv` files. Do not hand-edit them or the
`fideslang-*.tsv` files:

```bash
python -I tools/fideslang/update.py 3.1.4   # downloads that fideslang release from PyPI, regenerates TSVs + constants
```

The constants include the keys from the `linneo-*.tsv` files, so after adding a Linneo term to those files, rerun
the script to regenerate the constants. When moving to another fideslang version also update
`StandardTaxonomy.FIDESLANG_VERSION`, `NOTICE` and the term counts asserted in `StandardTaxonomyTest`.

## Documentation

`docs/` is a GitHub Pages site (Jekyll + Just the Docs, `docs/_config.yml`, deployed by `.github/workflows/pages.yml`;
rules in the "Documentation" section of `CONTRIBUTING.md`). `docs/en/` and `docs/es/` (mirrored file by file) hold a
`reference/` and a training `course/`. Each page starts with Just the Docs front matter followed by a
`<!-- doc-sources: ... -->` header listing the source files it documents. Before committing changes to `src/main`,
`tools/fideslang` or `pom.xml`, run the `docs-updater` subagent: it updates only the pages whose sources changed, in
both languages, and stamps the review. The `PreToolUse` hook `.claude/hooks/docs-gate.sh` blocks `git commit` until
that stamp matches the current library diff; afterwards `git add docs/` and retry. The hook's fingerprint uses
`git diff`, so a new file under `src/main` counts only once it is staged: `git add` it before running the subagent.

`DocumentationSamplesTest` compiles every ```` ```java ```` block of `README.md` and `docs/` against the library
(errors about names a page takes from elsewhere are ignored; errors naming the library, syntax errors and unknown
key constants fail), compares body-less type signatures with the real types and module declarations with
`module-info.java`. Mark a deliberately broken sample with `<!-- unchecked: reason -->` on the line before it.

Taxonomy rules (from `CONTRIBUTING.md`; applications persist and publish these keys):

- Never change the meaning of a key or reuse a removed one.
- Add terms as children of existing terms; new roots need discussion.
- Protection may only become stricter down the tree (enforced by `TaxonomyBuilder`, which raises a lower declared
  level; Linneo's own terms must not rely on that).
- Traits are part of a term's meaning: adding one is minor, removing one is major.
- Deprecate rather than delete: set `version_deprecated` and, where applicable, `replaced_by`.
- Versioning: adding a term, tightening protection or deprecating is minor; removing a term or lowering protection
  is major.
