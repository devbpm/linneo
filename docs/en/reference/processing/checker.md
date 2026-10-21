---
title: "ProcessingChecker"
parent: "Processing"
grand_parent: "Reference"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingChecker.java
  - src/main/java/io/github/devbpm/linneo/processing/CheckedActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingViolation.java
  - src/main/java/io/github/devbpm/linneo/processing/Jurisdiction.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingException.java
-->

# ProcessingChecker

`io.github.devbpm.linneo.processing.ProcessingChecker` — `public final class`, `@since 0.1.0`

Checks [processing activities](activity.md) against a [`Taxonomy`](../taxonomy.md) and the classification of their
entities, and builds the [record of processing activities](record.md). It is fail-closed for what would make an
activity unlawful as declared (errors) and reports gaps in the record as warnings; see
[Errors and warnings](README.md#errors-and-warnings). Instances are immutable and thread-safe.

## Methods

| Member | Returns | Description |
|---|---|---|
| `ProcessingChecker(Taxonomy taxonomy)` | — | Creates a checker; keys and entities must belong to `taxonomy`. It creates its own [`Classifier`](../classify/classifier.md) for that taxonomy. `NullPointerException` if `taxonomy` is `null`. It applies the GDPR alone, as `Jurisdiction.EU`. |
| `ProcessingChecker(Taxonomy taxonomy, Jurisdiction jurisdiction)` | — | The same, applying also the national rules of `jurisdiction` (see [Jurisdiction](#jurisdiction)). `NullPointerException` if either argument is `null`. |
| `Jurisdiction jurisdiction()` | [`Jurisdiction`](#jurisdiction) | The national law this checker applies on top of the GDPR. |
| `CheckedActivity check(ProcessingActivity activity)` | [`CheckedActivity`](#checkedactivity) | Checks one activity: its terms resolved, its personal data, the protection that data requires and every violation found. Never throws for a problem in the activity. `NullPointerException` if `activity` is `null`. |
| `List<CheckedActivity> checkAll(Collection<ProcessingActivity> activities)` | list | One result per activity, in the given order. An activity whose key was already used by an earlier one also gets the error `DUPLICATE_ACTIVITY`; the first one does not. |
| `ProcessingRecord verify(Collection<ProcessingActivity> activities)` | [`ProcessingRecord`](record.md) | Runs `checkAll` and throws a [`ProcessingException`](#processingexception) listing **every error of every activity** if there is any. Warnings do not fail. |
| `ProcessingRecord verify(ProcessingActivity... activities)` | [`ProcessingRecord`](record.md) | The same, for activities given one by one. |

```java
@Test
void recordOfProcessingActivities() {
    ProcessingRecord record = new ProcessingChecker(Taxonomy.load()).verify(UniversityActivities.all());
}
```

Use `Taxonomy.load()` if your keys or entities use terms of a [`TaxonomyExtension`](../extending.md); with
`Taxonomy.standard()` they would be unknown.

## What is checked

For each activity, in this order (the violations of a `CheckedActivity` follow it):

1. **Purposes**, then **data subjects**: each key is looked up in the taxonomy. An unknown key is the error
   `UNKNOWN_KEY` and is left out. A deprecated key is the warning `DEPRECATED_KEY` and is replaced by its
   `replaced_by` term, following the chain of replacements; a deprecated key without a replacement is kept, with the
   warning. Duplicates after replacement are kept once, in declaration order. An empty list is the error
   `MISSING_PURPOSE` or `MISSING_DATA_SUBJECTS`.
2. **Personal data** (see [below](#how-the-personal-data-is-gathered)). If there is none, and nothing was left
   unresolved by an unknown category key or an entity with classification errors, the error is
   `MISSING_PERSONAL_DATA`.
3. **Lawfulness**, only when the activity processes personal data:
   - no `legalBasis`: error `MISSING_LEGAL_BASIS`;
   - `LEGITIMATE_INTERESTS` without `legitimateInterest`: warning `MISSING_LEGITIMATE_INTEREST`; a
     `legitimateInterest` with another basis: warning `UNNEEDED_CONDITION`;
   - special category data and no `specialCategoryCondition`: error `MISSING_SPECIAL_CATEGORY_CONDITION`; a
     condition without special category data: warning `UNNEEDED_CONDITION`;
   - criminal offence data and no `criminalDataBasis`: error `MISSING_CRIMINAL_DATA_BASIS`; a basis without
     criminal offence data: warning `UNNEEDED_CONDITION`;
   - with `Jurisdiction.SPAIN`, `EXPLICIT_CONSENT` as the article 9.2 condition of an activity that processes ideology,
     trade union membership, religion, sexual orientation, beliefs or racial or ethnic origin: error
     `CONSENT_NOT_SUFFICIENT`;
   - bases that [rest on law](bases.md#the-legal-reference) and no `legalReference`: warning
     `MISSING_LEGAL_REFERENCE`;
   - no `retention`: warning `MISSING_RETENTION`;
   - special category data, criminal offence data or a profiling purpose, and no `impactAssessment`: warning
     `IMPACT_ASSESSMENT_RECOMMENDED`.

A purpose counts as **profiling** when a segment of its (resolved) key is `profiling`, `profiled` or `targeted`. In
the standard taxonomy that is `marketing.advertising.profiling`, `marketing.advertising.first_party.targeted`,
`marketing.advertising.third_party.targeted`, `personalize.content.profiling`, `personalize.content.profiled` and the
deprecated `personalize.profiling` (checked as `personalize.content.profiling`). Data uses of an extension count only
if their keys use those segments.

The checker does not inspect `description`, `recipients` or `transfers`.

## How the personal data is gathered

The personal data of an activity is the union of two sources:

- **Category keys** (`dataCategories`). An unknown key is the error `UNKNOWN_KEY`. A deprecated key is always the
  warning `DEPRECATED_KEY`; with a replacement it is resolved to it, without one it is used as is. A category that is not personal data (legal nature `NONE`) is the warning
  `NOT_PERSONAL_DATA` and is left out of the record. Each remaining category contributes its own levels.
- **Entities** (`entities`). Each class is classified with the [`Classifier`](../classify/classifier.md). If the
  classification has errors, the activity gets one `ENTITY_NOT_CLASSIFIED` error with their number (run the classifier
  test to see them); the fields that could be classified still count. The entity contributes its
  [personal fields](../classify/results.md): fields whose category is personal and whose identifiability is not
  `AGGREGATED` or `ANONYMIZED` — aggregated and anonymized data is not personal data. Deprecated keys in the
  annotations are already resolved to their replacement by the classifier, and its warnings are not repeated here.
  Each field contributes its **effective** levels, so a confidentiality or integrity raised on the field counts.

The categories are kept once each, ordered by key, in `CheckedActivity.dataCategories()`. The protection the
activity requires is the highest confidentiality and integrity of all those contributions (`PUBLIC` and `LOW` when
there is none), and it includes secrets if any of them is a secret. Their legal natures decide whether the activity
processes special category data (article 9) or criminal offence data (article 10).

An entity's personal fields are not always about the activity's data subjects. In the course's university model,
every entity extends an audit superclass whose `createdBy` field holds a staff member's username, so listing
`Enrollment` adds `user.account.username` to an activity about students. Declare the categories by key when an
entity holds more than the activity processes.

## CheckedActivity

`io.github.devbpm.linneo.processing.CheckedActivity` — `public record`, `@since 0.1.0`

```java
public record CheckedActivity(ProcessingActivity activity, List<DataUse> purposes, List<DataSubject> dataSubjects,
                              List<DataCategory> dataCategories, Confidentiality confidentiality, Integrity integrity,
                              boolean secret, List<ProcessingViolation> violations)
```

| Component | Description |
|---|---|
| `activity` | The activity as declared. |
| `purposes` | The data uses, unknown keys left out and deprecated keys replaced. |
| `dataSubjects` | The data subjects, likewise. |
| `dataCategories` | The personal data categories processed, from the declared keys and the personal fields of the entities, in key order. |
| `confidentiality` | The highest confidentiality of that data, field-level raises included; `PUBLIC` when there is none. |
| `integrity` | The highest integrity of that data, field-level raises included; `LOW` when there is none. |
| `secret` | Whether any of that data is a secret. |
| `violations` | Every problem found, errors and warnings. |

The canonical constructor copies the lists (immutable) and throws `NullPointerException` if `activity`,
`confidentiality`, `integrity` or a list is `null`.

| Method | Description |
|---|---|
| `boolean isValid()` | `true` if there is no error; warnings are allowed. |
| `List<ProcessingViolation> errors()` | The violations of severity `ERROR`. |
| `List<ProcessingViolation> warnings()` | The violations of severity `WARNING`: the open points of the record. |
| `Set<LegalNature> natures()` | The legal natures of `dataCategories` (never `NONE`). |
| `boolean processesSpecialCategories()` | `true` if a category is `SPECIAL_CATEGORY` (article 9). |
| `boolean processesCriminalOffenceData()` | `true` if a category is `CRIMINAL_OFFENCE` (article 10). |

```java
CheckedActivity checked = checker.check(admissions);
checked.isValid();                      // true
checked.natures();                      // [ORDINARY, SPECIAL_CATEGORY]
checked.confidentiality();              // HIGH
checked.warnings();                     // []
```

## ProcessingViolation

`io.github.devbpm.linneo.processing.ProcessingViolation` — `public record`, `@since 0.1.0`

```java
public record ProcessingViolation(String activity, Severity severity, Code code, String message)
```

| Component | Description |
|---|---|
| `activity` | The key of the activity. |
| `severity` | Whether it fails verification. |
| `code` | What kind of problem it is. |
| `message` | A description naming the key or provision involved. |

The canonical constructor throws `NullPointerException` if any component is `null`.

| Method | Description |
|---|---|
| `boolean isError()` | `true` for severity `ERROR`. |
| `String toString()` | `<SEVERITY> <CODE> <activity>: <message>`, for example `ERROR MISSING_LEGAL_BASIS newsletter: Personal data needs a lawful basis (GDPR article 6.1)`. |

### Severity

`ProcessingViolation.Severity`

| Constant | Meaning |
|---|---|
| `ERROR` | The activity, as declared, would be unlawful or cannot be checked; `verify` fails and `isValid()` is `false`. |
| `WARNING` | The record of the activity is incomplete or should be reviewed; verification passes and the message is an open point of the record. |

### Codes

`ProcessingViolation.Code` — every value, errors first, with the message the checker gives it (`<k>` for a key,
`<a>` for an article).

| Code | Severity | Raised when | Message |
|---|---|---|---|
| `UNKNOWN_KEY` | error | A purpose, data subject or data category key is not in the taxonomy. | `Unknown data use '<k>'` / `Unknown data subject '<k>'` / `Unknown data category '<k>'` |
| `MISSING_PURPOSE` | error | `purposes` is empty. | `No data use declared` |
| `MISSING_DATA_SUBJECTS` | error | `dataSubjects` is empty. | `No data subject declared` |
| `MISSING_PERSONAL_DATA` | error | No personal category and no entity with personal fields, and no unknown category key or entity error that could explain it. | `The activity processes no personal data` |
| `ENTITY_NOT_CLASSIFIED` | error | A listed entity has classification errors. | `<binary class name> has <n> classification error(s)` |
| `MISSING_LEGAL_BASIS` | error | Personal data and no `legalBasis`. | `Personal data needs a lawful basis (GDPR article 6.1)` |
| `MISSING_SPECIAL_CATEGORY_CONDITION` | error | Special category data and no `specialCategoryCondition`. | `Special categories of data (<keys>) need a condition of GDPR article 9.2` |
| `MISSING_CRIMINAL_DATA_BASIS` | error | Criminal offence data and no `criminalDataBasis`. | `Criminal offence data (<keys>) needs a basis of GDPR article 10` |
| `CONSENT_NOT_SUFFICIENT` | error | `Jurisdiction.SPAIN` only: the activity processes `user.demographic.political_opinion`, `philosophical_belief`, `trade_union_membership`, `religious_belief`, `sexual_orientation` or `race_ethnicity` (or a descendant) and its only article 9.2 condition is `EXPLICIT_CONSENT` (LOPDGDD article 9.1). | `Explicit consent alone does not lift the prohibition of processing <keys> (LOPDGDD article 9.1); declare another condition of GDPR article 9.2` |
| `DUPLICATE_ACTIVITY` | error | `checkAll`/`verify` only: an earlier activity has the same key. | `Another activity of the record has the key '<k>'` |
| `DEPRECATED_KEY` | **warning** | A purpose, data subject or data category key is deprecated; the activity is checked with its replacement. | `The data use '<k>' is deprecated; checked as '<r>'` (likewise `data subject`, `data category`; without the `; checked as` part when the key has no replacement) |
| `NOT_PERSONAL_DATA` | **warning** | A declared category is not personal data; it is left out of the record. | `The data category '<k>' is not personal data and is left out of the record` |
| `UNNEEDED_CONDITION` | **warning** | An article 9.2 condition or article 10 basis is declared for data the activity does not process, or a legitimate interest is stated with another lawful basis. | `Article <a> is declared but the activity processes no special category of data` / `Article 10 is declared but the activity processes no criminal offence data` / `A legitimate interest is stated but the lawful basis is article <a>` |
| `MISSING_LEGAL_REFERENCE` | **warning** | A basis that rests on law applies and `legalReference` is not set. | `GDPR article(s) <a>, <a> rest on Union or Member State law; name it as the legal reference`; with `Jurisdiction.SPAIN` and a condition of `SUBSTANTIAL_PUBLIC_INTEREST`, `HEALTH_OR_SOCIAL_CARE` or `PUBLIC_HEALTH`, it ends with ` (LOPDGDD article 9.2: a law with the rank of statute)` |
| `MISSING_LEGITIMATE_INTEREST` | **warning** | The basis is `LEGITIMATE_INTERESTS` and `legitimateInterest` is not set. | `State the legitimate interest pursued (GDPR article 6.1.f)` |
| `MISSING_RETENTION` | **warning** | Personal data and no `retention`. | `Declare the envisaged time limit for erasure (GDPR article 30.1.f)` |
| `IMPACT_ASSESSMENT_RECOMMENDED` | **warning** | Special category data, criminal offence data or a profiling purpose, and no `impactAssessment`. | `The activity involves <reasons>; GDPR article 35 may require an impact assessment`, where `<reasons>` joins `special categories of data`, `criminal offence data` and `profiling` with `and` |

Real output for an activity that misses both conditions:

```text
ERROR MISSING_SPECIAL_CATEGORY_CONDITION screening: Special categories of data (user.health_and_medical.disability) need a condition of GDPR article 9.2
ERROR MISSING_CRIMINAL_DATA_BASIS screening: Criminal offence data (user.criminal_history) needs a basis of GDPR article 10
WARNING MISSING_LEGAL_REFERENCE screening: GDPR article(s) 6.1.c rest on Union or Member State law; name it as the legal reference
WARNING MISSING_RETENTION screening: Declare the envisaged time limit for erasure (GDPR article 30.1.f)
WARNING IMPACT_ASSESSMENT_RECOMMENDED screening: The activity involves special categories of data and criminal offence data; GDPR article 35 may require an impact assessment
```

and for one with three unneeded declarations (`legitimateInterest(...)` followed by `legalBasis(CONSENT)`, plus
`EXPLICIT_CONSENT` and `OFFICIAL_AUTHORITY` for an activity that only processes `user.name`):

```text
WARNING UNNEEDED_CONDITION newsletter: A legitimate interest is stated but the lawful basis is article 6.1.a
WARNING UNNEEDED_CONDITION newsletter: Article 9.2.a is declared but the activity processes no special category of data
WARNING UNNEEDED_CONDITION newsletter: Article 10 is declared but the activity processes no criminal offence data
```

## Jurisdiction

`io.github.devbpm.linneo.processing.Jurisdiction` — `public enum`, `@since 0.1.0`

The national data protection law a checker applies on top of the GDPR.

| Constant | Meaning |
|---|---|
| `EU` | The GDPR alone, with no national rules. The default of `new ProcessingChecker(taxonomy)`. |
| `SPAIN` | The GDPR and Ley Orgánica 3/2018 (LOPDGDD). Two rules are added, below. |

With `SPAIN`:

- **Consent is not enough** (LOPDGDD article 9.1). If the activity processes `user.demographic.political_opinion`,
  `philosophical_belief`, `trade_union_membership`, `religious_belief`, `sexual_orientation` or `race_ethnicity`
  (or a descendant of them) and its only article 9.2 condition is `EXPLICIT_CONSENT`, the error
  `CONSENT_NOT_SUFFICIENT` names those keys: declare another condition of article 9.2.
- **Statute** (LOPDGDD article 9.2). When the condition is `SUBSTANTIAL_PUBLIC_INTEREST`, `HEALTH_OR_SOCIAL_CARE`
  or `PUBLIC_HEALTH` and there is no `legalReference`, the warning `MISSING_LEGAL_REFERENCE` adds that the law must
  have the rank of statute.

```java
ProcessingChecker checker = new ProcessingChecker(Taxonomy.load(), Jurisdiction.SPAIN);
checker.jurisdiction();   // SPAIN
```

## ProcessingException

`io.github.devbpm.linneo.processing.ProcessingException` — `public class ProcessingException extends
RuntimeException`, `@since 0.1.0`

Thrown by `ProcessingChecker.verify` when an activity is incomplete or would be unlawful as declared.

| Member | Description |
|---|---|
| `ProcessingException(List<ProcessingViolation> errors)` | Creates the exception from the errors found (at least one). The list is copied. |
| `List<ProcessingViolation> errors()` | The errors, all of severity `ERROR`, from every verified activity, in activity order. |

The message is the number of errors followed by one line per error, in the format of
[`ProcessingViolation.toString()`](#processingviolation). Verifying an activity twice plus an empty one gives:

```text
4 processing error(s):
 - ERROR DUPLICATE_ACTIVITY newsletter: Another activity of the record has the key 'newsletter'
 - ERROR MISSING_PURPOSE empty: No data use declared
 - ERROR MISSING_DATA_SUBJECTS empty: No data subject declared
 - ERROR MISSING_PERSONAL_DATA empty: The activity processes no personal data
```

The `errors` list is held in a `transient` field: it is not preserved if the exception is serialized.

See also: [ProcessingActivity](activity.md) · [Legal bases](bases.md) · [ProcessingRecord](record.md) ·
[Classifier](../classify/classifier.md) · course:
[Processing activities and the record](../../course/09-processing-activities.md)
