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
same `Term` model). Eight exported packages plus one internal one (not exported in `module-info.java`):

- **`io.github.devbpm.linneo`** — the taxonomy model, protection enums, key constants, `TaxonomyBuilder`, the
  drafts and `TaxonomyExtension`. Terms are `DataCategory`, `DataUse`, `DataSubject` (all `Term`s), keyed by
  dotted strings whose parent is the key prefix (`user.contact.email` → `user.contact`). The builder and drafts
  must stay in this package: they rely on package-private access (the `Taxonomy` constructor, `TermDraft.info()`,
  `CategoryDraft.declared*()`) so that only `build()` can create a validated `Taxonomy`.
- **`annotation`** — `@Category`, `@DefaultCategory`, `@CategoryByRow`, `@Exempt`.
- **`internal.StandardTaxonomy`** — loads the bundled TSV resources from
  `src/main/resources/io/github/devbpm/linneo/internal/` into a `TaxonomyBuilder`:
  - `fideslang-data-{categories,uses,subjects}.tsv` — fideslang verbatim (version in `FIDESLANG_VERSION`).
  - `linneo-data-{categories,uses,subjects}.tsv` — Linneo's added terms (never redefine fideslang keys).
  - `linneo-category-policy.tsv` — protection facets (nature, confidentiality, integrity, secret) per category key;
    fideslang itself defines no protection levels.
- **`TaxonomyBuilder`** — the single place where taxonomies are assembled and validated. `Taxonomy.standard()` =
  builder + standard; `Taxonomy.load()` additionally applies `TaxonomyExtension`s found via `ServiceLoader`
  (sorted by name for determinism). `build()` resolves categories parent-first: roots must declare all facets,
  children inherit unset facets and may only make them **stricter** (a lower level or an inadmissible `LegalNature`
  throws `TaxonomyException`); `secret` is OR-ed down the tree. `replaced_by` targets of deprecated terms are
  checked here too. The built `Taxonomy` is immutable.
- **`classify`** — `Classifier` reflects over a class hierarchy's fields (never reads values), resolves each field
  through its annotations or a class-level `@DefaultCategory`, and collects `Violation`s. `classify` returns a
  `ClassClassification`; `verify` throws `ClassificationException` listing all errors. Design is fail-closed:
  unclassified fields, unknown keys, weakened levels, personal categories used as class defaults and conflicting
  annotations are errors; deprecated keys are warnings resolved to their replacement.
- **`processing`** — GDPR processing activities. `ProcessingActivity` (builder) declares purposes (data use keys),
  data subjects, personal data (category keys and/or annotated entity classes) and the lawful bases
  (`LegalBasis`, `SpecialCategoryCondition`, `CriminalDataBasis`, `TransferSafeguard`, with fideslang's values).
  `ProcessingChecker` resolves them against the taxonomy, runs the `Classifier` on the entities and reports
  `ProcessingViolation`s with the same fail-closed split as `classify`: unlawful-as-declared is an error, a gap in
  the record is a warning. `verify` returns a `ProcessingRecord` (article 30 record, `toMarkdown`/`toJson`).
- **`extension.university`** — `UniversityTaxonomy`, an optional `TaxonomyExtension` for higher education with its
  key constants (`UniversityCategories`, `UniversityDataUses`, `UniversityDataSubjects`). Deliberately **not**
  declared as a `provides` service in `module-info.java`, so `Taxonomy.load()` never applies it; users call
  `builder.extend(new UniversityTaxonomy())` or register it themselves. Its keys are hand-written constants (not
  generated); `UniversityTaxonomyTest` checks they match exactly the terms the extension adds. If a future standard
  term reuses one of its keys the build fails with a duplicate key: move the term to the standard and drop it here.
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
that stamp matches the current library diff; afterwards `git add docs/` and retry.

Taxonomy rules (from `CONTRIBUTING.md`; applications persist and publish these keys):

- Never change the meaning of a key or reuse a removed one.
- Add terms as children of existing terms; new roots need discussion.
- Protection may only become stricter down the tree (enforced by `TaxonomyBuilder`).
- Deprecate rather than delete: set `version_deprecated` and, where applicable, `replaced_by`.
- Versioning: adding a term, tightening protection or deprecating is minor; removing a term or lowering protection
  is major.
