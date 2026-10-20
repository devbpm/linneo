---
title: "Standard taxonomy"
parent: "Reference"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/internal/StandardTaxonomy.java
  - src/main/resources/io/github/devbpm/linneo/internal/fideslang-data-categories.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/fideslang-data-uses.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/fideslang-data-subjects.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-uses.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-subjects.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-category-policy.tsv
  - tools/fideslang/update.py
-->

# Standard taxonomy

The taxonomy returned by `Taxonomy.standard()` (and the base of `Taxonomy.load()` and
`TaxonomyBuilder.withStandard()`). It is assembled by `io.github.devbpm.linneo.internal.StandardTaxonomy`, which is in
a non-exported package and not part of the public API, from tab-separated resources bundled in the JAR under
`io/github/devbpm/linneo/internal/`.

## Contents

| | fideslang 3.1.4 | Linneo | Total |
|---|---|---|---|
| Data categories | 85 | 38 | 123 |
| Data uses | 56 | 7 | 63 |
| Data subjects | 15 | 6 | 21 |

The fideslang version is `StandardTaxonomy.FIDESLANG_VERSION` (`"3.1.4"`). The fideslang terms are bundled
unchanged (license CC BY 4.0, © Ethyca, Inc.; see `NOTICE`) and have origin `fideslang`; Linneo's terms have
origin `linneo` and never redefine a fideslang key. fideslang defines no protection levels: all of them come from
Linneo's policy file.

For every key, use the constants in [`Categories`, `DataUses` and `DataSubjects`](key-constants.md) or iterate
`Taxonomy.categories()`, `dataUses()` and `dataSubjects()`.

## Data category roots

Every root declares all four facets in `linneo-category-policy.tsv`:

| Root | Origin | Nature | Confidentiality | Integrity | Secret | Covers |
|---|---|---|---|---|---|---|
| `user` | fideslang | `ORDINARY` | `LOW` | `MEDIUM` | no | Data about a natural person |
| `system` | fideslang | `NONE` | `LOW` | `LOW` | no | Data of the system: authentication, operations, security, configuration |
| `org` | Linneo | `NONE` | `PUBLIC` | `LOW` | no | The organization and third parties that are not natural persons |
| `reference` | Linneo | `NONE` | `PUBLIC` | `MEDIUM` | no | Master data from public or standard sources |

## Protection policy

Below the roots, the policy raises facets where the data requires it; any key not listed inherits from its parent
(see [Protection levels](protection-levels.md#inheritance-rule)). The main rules:

| Categories | Policy |
|---|---|
| `user.health_and_medical`, `user.biometric`, `user.demographic.philosophical_belief`, `.political_opinion`, `.race_ethnicity`, `.religious_belief`, `.sex_life`, `.sexual_orientation`, `.trade_union_membership` | `SPECIAL_CATEGORY`, confidentiality `HIGH`, integrity `HIGH` |
| `user.criminal_history` | `CRIMINAL_OFFENCE`, `HIGH`, `HIGH` |
| `user.authorization` (scope of permissions) | confidentiality `HIGH`, integrity `HIGH`; `user.authorization.credentials`, `.password` and `.biometric` are **secret**, and `user.authorization.biometric` is also `SPECIAL_CATEGORY` |
| `user.device.session_id` | confidentiality `HIGH`, **secret** |
| `system.authentication` | confidentiality `HIGH`, integrity `HIGH`; `system.authentication.secret` is **secret** |
| `user.financial`, `user.payment`, `user.education` | confidentiality `MEDIUM`, integrity `HIGH`; `user.financial.credit_card` confidentiality `HIGH` |
| `user.behavior`, `user.childrens`, `user.government_id`, `user.location.precise`, `user.content.private`, `user.content.self_image`, several `user.demographic.*` (date of birth, gender, marital status, profile, family, nationality) | confidentiality `MEDIUM` |
| `user.behavior.activity_log`, `system.security`, `system.integrity` | integrity `HIGH` |
| `system.configuration` | integrity `MEDIUM` |
| `org.structure`, `org.location`, `org.process` | confidentiality `LOW`, integrity `MEDIUM` |
| `org.legal_entity` | integrity `MEDIUM` |
| `org.business_records` | confidentiality `MEDIUM`, integrity `MEDIUM` |

Children of a special-category category are special category too (for example
`user.health_and_medical.disability`), and every descendant of a secret category is secret.

## Linneo's additions

**Data categories** (38, all `since` `0.1.0`):

| Parent | Added children |
|---|---|
| `user` | `user.education` with `program`, `enrollment`, `course_history`, `grade`, `test_score`, `previous_education`, `status`, `accomplishment` |
| `user.unique_id` | `record_reference` |
| `user.government_id` | `social_security_number` |
| `user.device` | `user_agent`, `session_id` |
| `user.workplace` | `team`, `unit`, `role`, `assignment_period` |
| `user.demographic` | `nationality`, `family`, `philosophical_belief`, `trade_union_membership`, `sex_life` |
| `user.financial` | `scholarship`, `fee` |
| `user.health_and_medical` | `disability` |
| `user.behavior` | `activity_log` |
| `system` | `system.security`, `system.integrity`, `system.configuration` |
| `system.authentication` | `secret` |
| (root) | `org` with `structure`, `legal_entity`, `location`, `process`, `business_records` |
| (root) | `reference` with `public` |

**Data uses** (7): the root `education` ("Provides educational services") with `education.admission`,
`education.enrollment`, `education.teaching`, `education.assessment`, `education.academic_records` and
`education.financial_aid`.

**Data subjects** (6, all roots): `student`, `applicant`, `alumnus`, `faculty_member`, `staff_member`,
`legal_representative`.

## fideslang roots

- Data categories: `user`, `system`.
- Data uses: `analytics`, `collect`, `employment`, `essential`, `finance`, `functional`, `marketing`, `operations`,
  `personalize`, `sales`, `third_party_sharing`, `train_ai_system`.
- Data subjects: 15 flat terms, such as `customer`, `employee`, `job_applicant`, `patient`, `visitor`.

fideslang 3.1.4 has one deprecated term: the data use `personalize.profiling` (deprecated in `2.1.1`, replaced by
`personalize.content.profiling`). No standard data category is deprecated.

## Resource files and formats

| File | Origin | Columns |
|---|---|---|
| `fideslang-data-categories.tsv`, `fideslang-data-uses.tsv`, `fideslang-data-subjects.tsv` | `fideslang` | term format (7) |
| `linneo-data-categories.tsv`, `linneo-data-uses.tsv`, `linneo-data-subjects.tsv` | `linneo` | term format (7) |
| `linneo-category-policy.tsv` | — | policy format (5) |

Common rules (UTF-8): blank lines and lines starting with `#` are ignored; the first remaining line is the column
header and is skipped; cells are separated by a tab and stripped of surrounding white space; missing trailing cells
are empty; a line with more cells than columns is rejected (`TaxonomyException`: `<file>: too many columns in line
'...'`). A missing resource throws `TaxonomyException` (`Missing taxonomy resource <file>`).

**Term format**: `key`, `parent_key`, `name`, `description`, `version_added`, `version_deprecated`, `replaced_by`.

- `parent_key` must equal the key without its last segment (empty for a root); otherwise `TaxonomyException`
  (`<file>: '<key>' declares parent '<p>' but its key implies '<q>'`).
- A non-empty `version_added` becomes `TermInfo.since`.
- A non-empty `version_deprecated` deprecates the term, with `replaced_by` as replacement (or none if empty).
  `replaced_by` is ignored when `version_deprecated` is empty.
- In the fideslang files an empty `version_added` is written as `2.0.0`, the value fideslang's `default_factory`
  assigns.

**Policy format**: `key`, `nature`, `confidentiality`, `integrity`, `secret`.

- Empty cell = inherit. Non-empty values are enum constant names (`ORDINARY`, `HIGH`…); `secret` is `true` or
  `false`.
- Each row is applied with `TaxonomyBuilder.refineCategory` after all terms are loaded, so the key must exist.

Loading order: fideslang categories, uses and subjects (origin `fideslang`), then Linneo's categories, uses and
subjects (origin `linneo`), then the policy. `TaxonomyBuilder.build()` then checks the hierarchy and resolves the
inherited facets.

## Regenerating from fideslang

The `fideslang-*.tsv` files and the constant classes are generated; do not edit them by hand. From the repository
root:

```bash
python -I tools/fideslang/update.py 3.1.4
```

The script (Python standard library only) downloads the release's source distribution from PyPI, reads
`src/fideslang/default_taxonomy/data_{categories,uses,subjects}.py` and writes:

- `src/main/resources/io/github/devbpm/linneo/internal/fideslang-data-{categories,uses,subjects}.tsv`, with a
  header comment naming the source, version and licence;
- `src/main/java/io/github/devbpm/linneo/{Categories,DataUses,DataSubjects}.java`: the fideslang keys, then the
  keys of the `linneo-*.tsv` files (see [Key constants](key-constants.md)).

After adding a Linneo term to a `linneo-*.tsv` file, rerun the script to regenerate the constants. When moving to
another fideslang version, also update `StandardTaxonomy.FIDESLANG_VERSION`, `NOTICE` and the term counts asserted
in `StandardTaxonomyTest`, and make sure every new root category has a full row in `linneo-category-policy.tsv`
(otherwise the taxonomy fails to build).

See also: [Taxonomy](taxonomy.md) · [Protection levels](protection-levels.md) · [Key constants](key-constants.md) ·
[Extending the taxonomy](extending.md) · course: [The taxonomy](../course/02-the-taxonomy.md)
