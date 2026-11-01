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
| `ProcessingChecker(Taxonomy taxonomy, Jurisdiction jurisdiction, DataInventory inventory)` | — | The same, and it also resolves the [`objects`](activity.md#components) of the activities in `inventory`, a [data inventory](../classify/inventory.md), so that activities can be checked without the model classes, for example from records an application keeps in its database. `inventory` may be `null` for none; an activity that lists objects then gets an error. `NullPointerException` if `taxonomy` or `jurisdiction` is `null`. |
| `Jurisdiction jurisdiction()` | [`Jurisdiction`](#jurisdiction) | The national law this checker applies on top of the GDPR. |
| `CheckedActivity check(ProcessingActivity activity)` | [`CheckedActivity`](#checkedactivity) | Checks one activity: its terms resolved, its personal data, the protection that data requires and every violation found. Never throws for a problem in the activity. `NullPointerException` if `activity` is `null`. |
| `List<CheckedActivity> checkAll(Collection<ProcessingActivity> activities)` | list | One result per activity, in the given order. An activity whose key was already used by an earlier one also gets the error `DUPLICATE_ACTIVITY`; the first one does not. Each entity is classified once for all the activities, however many list it. |
| `ProcessingRecord verify(Collection<ProcessingActivity> activities)` | [`ProcessingRecord`](record.md) | Runs `checkAll` and throws a [`ProcessingException`](#processingexception) carrying **every error of every activity**, governance ones included, if there is any (its message lists the first 50). Warnings do not fail. |
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
   unresolved by an unknown category key, an entity with classification errors, or an unknown object or object field,
   the error is `MISSING_PERSONAL_DATA`.
3. **Lawfulness**, only when the activity processes personal data. It checks the activity's
   [governance](activity.md#governance), and every problem it finds is reported on the
   [element](#element) of the governance it is about:
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

The personal data of an activity is the union of three sources:

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
  `checkAll` and `verify` classify each entity once for all the activities that list it; `check` classifies the
  entities of the one activity it is given.
- **Objects** (`objects`), resolved in the [data inventory](../classify/inventory.md) the checker was created with. An
  object the inventory does not hold, or any object when the checker has no inventory, is the error `UNKNOWN_KEY`. A
  field of the object whose key is not in the taxonomy is also `UNKNOWN_KEY`, whether or not it would be personal;
  exempted fields are skipped. A field whose key is deprecated is the warning `DEPRECATED_KEY`, because the inventory
  holds the keys as written (and so, unlike the classifier's own warnings for an entity, it is reported here), whether or
  not the field is personal; the field is checked with the replacement. Each other field contributes as a field of an
  entity does: its category, resolved to the replacement of a deprecated key, and its **effective** protection, with
  the levels raised on the field, but only if it is personal: a personal category not declared `ANONYMIZED` or
  `AGGREGATED`. With the inventory that `Classifier.inventory` builds for a class, an activity
  that lists its object gets the same categories and protection as one that lists the class.

The categories are kept once each, ordered by key, in `CheckedActivity.dataCategories()`. The protection the
activity requires, `CheckedActivity.protection()`, is the [join](../protection-levels.md#protection) of all those
contributions: the highest confidentiality and integrity (`PUBLIC` and `LOW` when there is none), secret if any of
them is a secret, and the strictest of their legal natures (`NONE` alone when there is none). The legal natures of the
categories decide whether the activity processes special category data (article 9) or criminal offence data
(article 10).

An entity's personal fields are not always about the activity's data subjects. In the course's university model,
every entity extends an audit superclass whose `createdBy` field holds a staff member's username, so listing
`Enrollment` adds `user.account.username` to an activity about students. Declare the categories by key when an
entity holds more than the activity processes.

## CheckedActivity

`io.github.devbpm.linneo.processing.CheckedActivity` — `public record`, `@since 0.1.0`

```java
public record CheckedActivity(ProcessingActivity activity, List<DataUse> purposes, List<DataSubject> dataSubjects,
                              List<DataCategory> dataCategories, Protection protection,
                              List<ProcessingViolation> violations)
```

| Component | Description |
|---|---|
| `activity` | The activity as declared. |
| `purposes` | The data uses, unknown keys left out and deprecated keys replaced. |
| `dataSubjects` | The data subjects, likewise. |
| `dataCategories` | The personal data categories processed, from the declared keys and the personal fields of the entities and objects, in key order. |
| `protection` | The least [`Protection`](../protection-levels.md#protection) that covers all that data, field-level raises included; `Protection.LOWEST` when there is none. Its `confidentiality()`, `integrity()` and `secret()` are what the record shows as the required protection. |
| `violations` | Every problem found, errors and warnings. |

The canonical constructor copies the lists (immutable) and throws `NullPointerException` if `activity`,
`protection` or a list is `null`.

| Method | Description |
|---|---|
| `boolean isValid()` | `true` if there is no error; warnings are allowed. |
| `List<ProcessingViolation> errors()` | The violations of severity `ERROR`. |
| `List<ProcessingViolation> warnings()` | The violations of severity `WARNING`: the open points of the record. |
| `Set<LegalNature> natures()` | The legal natures of `dataCategories` (never `NONE`), all of them; `protection().natures()` keeps only the strictest. |
| `boolean processesSpecialCategories()` | `true` if a category is `SPECIAL_CATEGORY` (article 9). |
| `boolean processesCriminalOffenceData()` | `true` if a category is `CRIMINAL_OFFENCE` (article 10). |

```java
CheckedActivity checked = checker.check(admissions);
checked.isValid();                      // true
checked.natures();                      // [ORDINARY, SPECIAL_CATEGORY]
checked.protection().confidentiality(); // HIGH
checked.warnings();                     // []
```

## ProcessingViolation

`io.github.devbpm.linneo.processing.ProcessingViolation` — `public record`, `@since 0.1.0`

```java
public record ProcessingViolation(String activity, Severity severity, Code code, Element element, String message)
```

| Component | Description |
|---|---|
| `activity` | The key of the activity. |
| `severity` | Whether it fails verification. |
| `code` | What kind of problem it is. |
| `element` | The part of the activity the problem is in, which tells who should fix it; see [`Element`](#element). |
| `message` | A description naming the key or provision involved. |

The canonical constructor throws `NullPointerException` if any component is `null`.

| Method | Description |
|---|---|
| `boolean isError()` | `true` for severity `ERROR`. |
| `String toString()` | `<SEVERITY> <CODE> <activity>: <message>`, for example `ERROR MISSING_LEGAL_BASIS newsletter: Personal data needs a lawful basis (GDPR article 6.1)`. It does not show the element. |

### Severity

`ProcessingViolation.Severity`

| Constant | Meaning |
|---|---|
| `ERROR` | The activity, as declared, would be unlawful or cannot be checked; `verify` fails and `isValid()` is `false`. |
| `WARNING` | The record of the activity is incomplete or should be reviewed; verification passes and the message is an open point of the record. |

### Codes

`ProcessingViolation.Code` — every value, errors first, with the [element](#element) it is reported on and the
message the checker gives it (`<k>` for a key, `<a>` for an article). The element of a code that applies to several
parts depends on the part.

| Code | Severity | Element | Raised when | Message |
|---|---|---|---|---|
| `UNKNOWN_KEY` | error | `PURPOSES`, `DATA_SUBJECTS` or `PERSONAL_DATA`, by the kind of key | A purpose, data subject or data category key is not in the taxonomy; an object of `objects` is not in the data inventory (or the checker has none); a field of such an object declares a data category key that is not in the taxonomy. | `Unknown data use '<k>'` / `Unknown data subject '<k>'` / `Unknown data category '<k>'` / `Unknown object '<o>'` (followed by `: the checker was given no data inventory` when it has none) / `The field <o>.<path> has the unknown data category '<k>'` |
| `MISSING_PURPOSE` | error | `PURPOSES` | `purposes` is empty. | `No data use declared` |
| `MISSING_DATA_SUBJECTS` | error | `DATA_SUBJECTS` | `dataSubjects` is empty. | `No data subject declared` |
| `MISSING_PERSONAL_DATA` | error | `PERSONAL_DATA` | No personal category and no entity or object with personal fields, and no unknown category key, entity error or unknown object that could explain it. | `The activity processes no personal data` |
| `ENTITY_NOT_CLASSIFIED` | error | `PERSONAL_DATA` | A listed entity has classification errors. | `<binary class name> has <n> classification error(s)` |
| `MISSING_LEGAL_BASIS` | error | `LEGAL_BASIS` | Personal data and no `legalBasis`. | `Personal data needs a lawful basis (GDPR article 6.1)` |
| `MISSING_SPECIAL_CATEGORY_CONDITION` | error | `SPECIAL_CATEGORY_CONDITION` | Special category data and no `specialCategoryCondition`. | `Special categories of data (<keys>) need a condition of GDPR article 9.2` |
| `MISSING_CRIMINAL_DATA_BASIS` | error | `CRIMINAL_DATA_BASIS` | Criminal offence data and no `criminalDataBasis`. | `Criminal offence data (<keys>) needs a basis of GDPR article 10` |
| `CONSENT_NOT_SUFFICIENT` | error | `SPECIAL_CATEGORY_CONDITION` | `Jurisdiction.SPAIN` only: the activity processes `user.demographic.political_opinion`, `philosophical_belief`, `trade_union_membership`, `religious_belief`, `sexual_orientation` or `race_ethnicity` (or a descendant) and its only article 9.2 condition is `EXPLICIT_CONSENT` (LOPDGDD article 9.1). | `Explicit consent alone does not lift the prohibition of processing <keys> (LOPDGDD article 9.1); declare another condition of GDPR article 9.2` |
| `DUPLICATE_ACTIVITY` | error | `KEY` | `checkAll`/`verify` only: an earlier activity has the same key. | `Another activity of the record has the key '<k>'` |
| `DEPRECATED_KEY` | **warning** | `PURPOSES`, `DATA_SUBJECTS` or `PERSONAL_DATA`, by the kind of key | A purpose, data subject or data category key is deprecated, or a field of an object of `objects` declares a deprecated data category key; the activity is checked with its replacement. | `The data use '<k>' is deprecated; checked as '<r>'` (likewise `data subject`, `data category`; without the `; checked as` part when the key has no replacement) / `The field <o>.<path> has the deprecated data category '<k>'; checked as '<r>'` (same omission) |
| `NOT_PERSONAL_DATA` | **warning** | `PERSONAL_DATA` | A declared category is not personal data; it is left out of the record. | `The data category '<k>' is not personal data and is left out of the record` |
| `UNNEEDED_CONDITION` | **warning** | `SPECIAL_CATEGORY_CONDITION` for an article 9.2 condition, `CRIMINAL_DATA_BASIS` for an article 10 basis, `LEGITIMATE_INTEREST` for a legitimate interest | An article 9.2 condition or article 10 basis is declared for data the activity does not process, or a legitimate interest is stated with another lawful basis. | `Article <a> is declared but the activity processes no special category of data` / `Article 10 is declared but the activity processes no criminal offence data` / `A legitimate interest is stated but the lawful basis is article <a>` |
| `MISSING_LEGAL_REFERENCE` | **warning** | `LEGAL_REFERENCE` | A basis that rests on law applies and `legalReference` is not set. | `GDPR article(s) <a>, <a> rest on Union or Member State law; name it as the legal reference`; with `Jurisdiction.SPAIN` and a condition of `SUBSTANTIAL_PUBLIC_INTEREST`, `HEALTH_OR_SOCIAL_CARE` or `PUBLIC_HEALTH`, it ends with ` (LOPDGDD article 9.2: a law with the rank of statute)` |
| `MISSING_LEGITIMATE_INTEREST` | **warning** | `LEGITIMATE_INTEREST` | The basis is `LEGITIMATE_INTERESTS` and `legitimateInterest` is not set. | `State the legitimate interest pursued (GDPR article 6.1.f)` |
| `MISSING_RETENTION` | **warning** | `RETENTION` | Personal data and no `retention`. | `Declare the envisaged time limit for erasure (GDPR article 30.1.f)` |
| `IMPACT_ASSESSMENT_RECOMMENDED` | **warning** | `IMPACT_ASSESSMENT` | Special category data, criminal offence data or a profiling purpose, and no `impactAssessment`. | `The activity involves <reasons>; GDPR article 35 may require an impact assessment`, where `<reasons>` joins `special categories of data`, `criminal offence data` and `profiling` with `and` |

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

### Element

`ProcessingViolation.Element` — `public enum`, `@since 0.1.0`

The part of a processing activity a problem is in. The **scope** of the activity (`KEY`, `PURPOSES`, `DATA_SUBJECTS`,
`PERSONAL_DATA`) is declared in code, so a problem there is for developers and should fail the build. The rest is the
[governance](activity.md#governance) the data protection function keeps, so a problem there belongs on the screen
where it is edited.

| Constant | `isGovernance()` | The part |
|---|---|---|
| `KEY` | `false` | The key that identifies the activity in the record. |
| `PURPOSES` | `false` | The purposes (`ProcessingActivity.purposes()`). |
| `DATA_SUBJECTS` | `false` | The data subjects (`ProcessingActivity.dataSubjects()`). |
| `PERSONAL_DATA` | `false` | The personal data: data categories, entities and objects. |
| `LEGAL_BASIS` | `true` | The lawful basis (`Governance.legalBasis()`). |
| `LEGITIMATE_INTEREST` | `true` | The legitimate interest pursued (`Governance.legitimateInterest()`). |
| `SPECIAL_CATEGORY_CONDITION` | `true` | The article 9.2 condition (`Governance.specialCategoryCondition()`). |
| `CRIMINAL_DATA_BASIS` | `true` | The article 10 basis (`Governance.criminalDataBasis()`). |
| `LEGAL_REFERENCE` | `true` | The law the bases rest on (`Governance.legalReference()`). |
| `RETENTION` | `true` | The time limit for erasure (`Governance.retention()`). |
| `IMPACT_ASSESSMENT` | `true` | The impact assessment (`Governance.impactAssessment()`). |

`boolean isGovernance()` is `true` for the elements of [`Governance`](activity.md#governance) and `false` for those of
the scope. There is no element for the recipients or the transfers, because the checker does not inspect them. The
element of each code is in the [table above](#codes).

Use it to send each problem to whoever can fix it: fail the build on the errors of the scope, and show the gaps of
the governance where the officer edits them. `verify` fails on **every** error, governance ones included, so a build
check that should fail only on the scope uses `checkAll` and filters by element:

```java
import io.github.devbpm.linneo.processing.ProcessingViolation.Element;

// In a test: the scope is declared in code, so its errors are the developers' to fix.
List<ProcessingViolation> scopeErrors = checker.checkAll(activities).stream()
        .flatMap(checked -> checked.errors().stream())
        .filter(violation -> !violation.element().isGovernance())
        .toList();
assertTrue(scopeErrors.isEmpty(), () -> "Fix the scope of the activities: " + scopeErrors);

// Where the officer edits the governance of one activity: each gap beside its field.
CheckedActivity edited = checker.check(activity.withGovernance(stored));
Map<Element, List<ProcessingViolation>> gaps = edited.violations().stream()
        .filter(violation -> violation.element().isGovernance())
        .collect(Collectors.groupingBy(ProcessingViolation::element));
List<ProcessingViolation> beside = gaps.getOrDefault(Element.RETENTION, List.of());
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
[`ProcessingViolation.toString()`](#processingviolation). It lists the first 50 errors; if there are more, a last line
says how many were left out and counts all the errors by code, in alphabetical order, as in
`... and 70 more; all of them by code: MISSING_PURPOSE 60, UNKNOWN_KEY 60`. `errors()` always has every error.
Verifying an activity twice plus an empty one gives:

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
