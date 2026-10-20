---
title: "ProcessingActivity"
parent: "Processing"
grand_parent: "Reference"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/Transfer.java
-->

# ProcessingActivity

`io.github.devbpm.linneo.processing.ProcessingActivity` — `public record`, `@since 0.1.0`

```java
public record ProcessingActivity(String key, String name, String description, List<String> purposes,
                                 List<String> dataSubjects, List<String> dataCategories, List<Class<?>> entities,
                                 LegalBasis legalBasis, String legitimateInterest,
                                 SpecialCategoryCondition specialCategoryCondition,
                                 CriminalDataBasis criminalDataBasis, String legalReference, String retention,
                                 List<String> recipients, List<Transfer> transfers, String impactAssessment)
```

A processing activity of the controller, as declared in its record of processing activities (GDPR article 30.1).
The activity only **holds what was declared**: its constructor does not look keys up in a taxonomy, classify the
entities or check the bases. That is the job of the [`ProcessingChecker`](checker.md). Build one with
[`builder(key, name)`](#builder).

## Components

| Component | Article | Builder default | Description |
|---|---|---|---|
| `key` | — | required | A stable identifier, unique within the record: letters, digits, dots, underscores and hyphens, starting with a letter or digit (`[A-Za-z0-9][A-Za-z0-9._-]*`). Two activities with the same key are a `DUPLICATE_ACTIVITY` error. |
| `name` | — | required | A human-readable name, the heading of the activity in the record. It must not be blank; it is stripped. |
| `description` | — | `null` | What the activity does. |
| `purposes` | 30.1.b | empty | Keys of the data uses the activity serves, preferably `DataUses` constants. |
| `dataSubjects` | 30.1.c | empty | Keys of the categories of data subjects, preferably `DataSubjects` constants. |
| `dataCategories` | 30.1.c | empty | Keys of personal data categories processed beyond those of `entities`, preferably `Categories` constants. |
| `entities` | 30.1.c | empty | Classified model classes; the categories of their personal fields are processed by the activity. |
| `legalBasis` | 6.1 | `null` | The lawful basis; see [`LegalBasis`](bases.md#legalbasis). `null` means not declared. |
| `legitimateInterest` | 6.1.f | `null` | The interest pursued, when the basis is `LEGITIMATE_INTERESTS`. |
| `specialCategoryCondition` | 9.2 | `null` | The condition for special categories of data; see [`SpecialCategoryCondition`](bases.md#specialcategorycondition). |
| `criminalDataBasis` | 10 | `null` | What allows processing criminal offence data; see [`CriminalDataBasis`](bases.md#criminaldatabasis). |
| `legalReference` | 6.3 | `null` | The Union or Member State law the bases rest on, as free text. One text covers every basis of the activity. |
| `retention` | 30.1.f | `null` | The envisaged time limit for erasure, as free text. |
| `recipients` | 30.1.d | empty | The categories of recipients, as free text. |
| `transfers` | 30.1.e | empty | Transfers to third countries or international organisations; see [`Transfer`](#transfer). |
| `impactAssessment` | 35 | `null` | A reference to the data protection impact assessment, such as its identifier or location. |

### Canonical constructor

- `NullPointerException` if `key` or `name` is `null`.
- `IllegalArgumentException` if the `key` does not match `[A-Za-z0-9][A-Za-z0-9._-]*` (so a blank key, or one with
  spaces or other symbols, is rejected), with the message `Invalid processing activity key '<key>': use letters, digits,
  dots, underscores and hyphens, starting with a letter or digit`. The key is not stripped.
- `IllegalArgumentException` with the message `The processing activity '<key>' needs a name` if the `name` is blank.
  A valid name is **stripped** of leading and trailing white space.
- The lists are copied with `List.copyOf`: the result is immutable, and a `null` list or a `null` element throws
  `NullPointerException`. The elements are kept as given; a blank key in a list is reported by the checker as an
  unknown key.
- The optional texts `description`, `legitimateInterest`, `legalReference`, `retention` and `impactAssessment` are
  **normalized**: a blank text becomes `null` and any other text is stripped of leading and trailing white space.
  The texts of `recipients` are not normalized.

```java
ProcessingActivity activity = ProcessingActivity.builder("newsletter", "Alumni newsletter")
        .description("  Monthly news for former students.  ")
        .retention("   ")
        .build();
activity.description();   // "Monthly news for former students."
activity.retention();     // null
```

## Builder

`ProcessingActivity.Builder` — `public static final class`, `@since 0.1.0`

| Member | Effect |
|---|---|
| `static Builder builder(String key, String name)` | Starts a declaration. The key and name are checked when `build()` is called. |
| `Builder description(String description)` | Sets the description. |
| `Builder purposes(String... keys)` | **Adds** data use keys. |
| `Builder dataSubjects(String... keys)` | **Adds** data subject keys. |
| `Builder dataCategories(String... keys)` | **Adds** data category keys, for personal data that no listed entity declares. |
| `Builder entities(Class<?>... types)` | **Adds** classified model classes. |
| `Builder legalBasis(LegalBasis basis)` | Sets the lawful basis; `null` clears it. |
| `Builder legitimateInterest(String interest)` | Sets the basis to `LEGITIMATE_INTERESTS` **and** the interest pursued. |
| `Builder specialCategoryCondition(SpecialCategoryCondition condition)` | Sets the article 9.2 condition. |
| `Builder criminalDataBasis(CriminalDataBasis basis)` | Sets the article 10 basis. |
| `Builder legalReference(String reference)` | Sets the law the bases rest on. |
| `Builder retention(String retention)` | Sets the retention period or rule. |
| `Builder recipients(String... recipients)` | **Adds** categories of recipients. |
| `Builder transfer(String destination, TransferSafeguard safeguard)` | **Adds** a `Transfer`. A blank destination throws `IllegalArgumentException` at once, not in `build()`. |
| `Builder impactAssessment(String reference)` | Sets the reference to the impact assessment. |
| `ProcessingActivity build()` | Creates the activity with the canonical constructor. |

Every list method adds to what was given before (`purposes("a").purposes("b", "c")` gives `[a, b, c]`); every other
method replaces the previous value, so the last call wins. `build()` can be called more than once, and a builder can
be kept to derive variants of an activity.

`legitimateInterest(interest)` is the usual way to declare legitimate interests: it sets both components. Because
the last call wins, a later `legalBasis(...)` replaces the basis but **keeps the interest**:

```java
ProcessingActivity.builder("newsletter", "Alumni newsletter")
        .legitimateInterest("Keeping in touch with former students")
        .legalBasis(LegalBasis.CONSENT)        // basis CONSENT, the interest is still set
        .build();
```

The checker reports that as the warning `UNNEEDED_CONDITION` (`A legitimate interest is stated but the lawful basis
is article 6.1.a`), and the [record](record.md) leaves the interest out, because it shows the legitimate interest
only when the basis is `LEGITIMATE_INTERESTS`. Calling `legalBasis(LegalBasis.LEGITIMATE_INTERESTS)` without an
interest is the warning `MISSING_LEGITIMATE_INTEREST`.

A complete declaration:

```java
ProcessingActivity admissions = ProcessingActivity.builder("admissions", "Admissions")
        .description("Receives and assesses applications, including requests for the disability quota.")
        .purposes(DataUses.EDUCATION_ADMISSION)
        .dataSubjects(DataSubjects.APPLICANT)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL, USER_EDUCATION_PREVIOUS_EDUCATION,
                USER_EDUCATION_TEST_SCORE, USER_HEALTH_AND_MEDICAL_DISABILITY)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
        .legalReference("Higher Education Act, arts. 12 and 31")
        .retention("Until the end of the admission cycle; admitted applicants move to the student record")
        .recipients("Admissions board")
        .impactAssessment("DPIA-2026-01")
        .build();
```

(`USER_NAME` and the other category constants come from `import static io.github.devbpm.linneo.Categories.*;`.)

## Transfer

`io.github.devbpm.linneo.processing.Transfer` — `public record`, `@since 0.1.0`

```java
public record Transfer(String destination, TransferSafeguard safeguard)
```

A transfer of personal data to a third country or an international organisation (GDPR article 30.1.e).

| Component | Description |
|---|---|
| `destination` | The third country or international organisation, as free text. Not stripped. |
| `safeguard` | What allows the transfer; see [`TransferSafeguard`](bases.md#transfersafeguard). |

The constructor throws `NullPointerException` if a component is `null`, and `IllegalArgumentException` with the
message `A transfer needs a destination` if `destination` is blank. The checker does not inspect transfers; the
record lists them with the article and fideslang name of their safeguard.

See also: [Legal bases](bases.md) · [ProcessingChecker](checker.md) · [ProcessingRecord](record.md) ·
[Processing activities](README.md)
