---
title: "ProcessingActivity"
parent: "Processing"
grand_parent: "Reference"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/Governance.java
  - src/main/java/io/github/devbpm/linneo/processing/Transfer.java
  - src/main/java/io/github/devbpm/linneo/processing/ImpactScreening.java
-->

# ProcessingActivity

`io.github.devbpm.linneo.processing.ProcessingActivity` — `public record`, `@since 0.1.0`

```java
public record ProcessingActivity(String key, String name, String description, List<String> purposes,
                                 List<String> dataSubjects, List<String> dataCategories, List<Class<?>> entities,
                                 List<String> objects, Set<ImpactScreening.Factor> riskFactors,
                                 Governance governance)
```

A processing activity of the controller, as declared in its record of processing activities (GDPR article 30.1).
The activity only **holds what was declared**: its constructor does not look keys up in a taxonomy, classify the
entities or check the bases. That is the job of the [`ProcessingChecker`](checker.md). Build one with
[`builder(key, name)`](#builder).

An activity has two parts with different owners:

- its **scope** is what the code of the application knows for certain: the purposes it serves, the data subjects and
  the personal data it processes (`purposes`, `dataSubjects`, `dataCategories`, `entities`, `objects`), and the risk
  factors the code knows of (`riskFactors`);
- its [**governance**](#governance) is what the controller's data protection function decides and keeps up to date:
  the lawful bases, the legal reference, the retention, the recipients, the transfers, the impact assessment and the
  risk factors it declares for the [screening](impact-screening.md) of the need for one.

Code can give initial values for the governance with the builder. An application whose data protection officer edits
them keeps the governance in its own storage and combines it with the scope declared in code using
[`withGovernance`](#withgovernance).

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
| `objects` | 30.1.c | empty | Keys of objects of a [data inventory](../classify/inventory.md); the categories of their personal fields are processed by the activity. The [`ProcessingChecker`](checker.md#methods) must be created with that inventory. |
| `riskFactors` | 35 | empty | What the code knows the activity does that the [screening](impact-screening.md) cannot see from its terms, such as `Factor.AUTOMATED_DECISIONS`: a floor the governance adds to and cannot remove. `null` is an empty set. |
| `governance` | 6.1, 6.3, 9.2, 10, 30.1.d–f, 35 | `Governance.NONE` | The lawful bases, legal reference, retention, recipients, transfers, impact assessment and the risk factors the officer declares; see [Governance](#governance). `null` means `Governance.NONE`. |

The governance components are read through it: `activity.governance().legalBasis()`,
`activity.governance().retention()`, and so on. The screening reads the risk factors of both, `allRiskFactors()`.

### Canonical constructor

- `NullPointerException` if `key` or `name` is `null`.
- `IllegalArgumentException` if the `key` does not match `[A-Za-z0-9][A-Za-z0-9._-]*` (so a blank key, or one with
  spaces or other symbols, is rejected), with the message `Invalid processing activity key '<key>': use letters, digits,
  dots, underscores and hyphens, starting with a letter or digit`. The key is not stripped.
- `IllegalArgumentException` with the message `The processing activity '<key>' needs a name` if the `name` is blank.
  A valid name is **stripped** of leading and trailing white space.
- The lists are copied with `List.copyOf`: the result is immutable, and a `null` list or a `null` element throws
  `NullPointerException`. The elements are kept as given; a blank key in a list is reported by the checker as an
  unknown key. The exception is `objects`: a `null` list is an empty one.
- A blank `description` becomes `null` and any other text is stripped of leading and trailing white space.
- `riskFactors` is copied into an immutable set; `null` is an empty one.
- A `null` `governance` becomes [`Governance.NONE`](#governance).

```java
ProcessingActivity activity = ProcessingActivity.builder("newsletter", "Alumni newsletter")
        .description("  Monthly news for former students.  ")
        .retention("   ")
        .build();
activity.description();               // "Monthly news for former students."
activity.governance().retention();    // null
```

### withGovernance

`ProcessingActivity withGovernance(Governance governance)` returns the same activity with another governance, such as
the one the data protection officer keeps up to date. The scope declared in code is kept as is, its `riskFactors`
included, and the original activity is not changed. The governance simply **replaces** the current one, with no
merging: what the code says the activity does, such as automated decisions, is part of the scope, and the screening
reads it together with the factors of the governance (`allRiskFactors()`), so a governance saved before the code
changed never lifts an impact assessment. `NullPointerException` if `governance` is `null`; to clear it, pass
`Governance.NONE` (the factors the code declares stay).

`Set<ImpactScreening.Factor> allRiskFactors()` returns the risk factors the screening reads: the union of those of the
scope and those of the governance, immutable.

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
| `Builder objects(String... keys)` | **Adds** keys of objects of a [data inventory](../classify/inventory.md): the categories of their personal fields are processed by the activity. The checker must be created with that inventory. |
| `Builder governance(Governance governance)` | **Replaces** the governance declared so far with `governance`; the governance methods below that follow it start from it. `NullPointerException` if `governance` is `null`. |
| `Builder legalBasis(LegalBasis basis)` | Sets the lawful basis; `null` clears it. |
| `Builder legitimateInterest(String interest)` | Sets the basis to `LEGITIMATE_INTERESTS` **and** the interest pursued. |
| `Builder specialCategoryConditions(SpecialCategoryCondition... conditions)` | **Adds** article 9.2 conditions: one for each kind of special data the activity processes under a different condition. |
| `Builder criminalDataBasis(CriminalDataBasis basis)` | Sets the article 10 basis. |
| `Builder legalReference(String reference)` | Sets the law the bases rest on. |
| `Builder retention(String retention)` | Sets the retention period or rule. |
| `Builder recipients(String... recipients)` | **Adds** categories of recipients. |
| `Builder transfer(String destination, TransferSafeguard safeguard)` | **Adds** a `Transfer`. A blank destination throws `IllegalArgumentException` at once, not in `build()`. |
| `Builder impactAssessment(String reference)` | Sets the reference to the impact assessment. |
| `Builder riskFactors(ImpactScreening.Factor... factors)` | **Adds** risk factors the code knows of, such as `Factor.AUTOMATED_DECISIONS` when the code decides on its own, for the [screening](impact-screening.md) of the need for an impact assessment. They are part of the scope: the governance adds its own and cannot remove these. |
| `ProcessingActivity build()` | Creates the activity with the canonical constructor. |

The methods from `legalBasis` to `impactAssessment` are those of the [`Governance.Builder`](#governancebuilder): they
give the initial values of the governance of the activity. `riskFactors` is not one of them: it feeds the scope.

Every list method, `specialCategoryConditions` and `riskFactors` add to what was given before
(`purposes("a").purposes("b", "c")` gives `[a, b, c]`); every other method replaces the previous value, so the last
call wins. `governance(...)` replaces
everything declared by the governance methods before it, and the ones after it change the governance it gave:

```java
ProcessingActivity.builder("a", "A")
        .retention("One year")
        .governance(stored)          // retention of the activity: that of `stored`, not "One year"
        .recipients("Auditors")      // added to the recipients of `stored`
        .build();
```

`build()` can be called more than once, and a builder can be kept to derive variants of an activity.

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
        .specialCategoryConditions(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
        .legalReference("Higher Education Act, arts. 12 and 31")
        .retention("Until the end of the admission cycle; admitted applicants move to the student record")
        .recipients("Admissions board")
        .impactAssessment("DPIA-2026-01")
        .build();
```

(`USER_NAME` and the other category constants come from `import static io.github.devbpm.linneo.Categories.*;`.)

## Governance

`io.github.devbpm.linneo.processing.Governance` — `public record`, `@since 0.1.0`

```java
public record Governance(LegalBasis legalBasis, String legitimateInterest,
                         Set<SpecialCategoryCondition> specialCategoryConditions,
                         CriminalDataBasis criminalDataBasis,
                         String legalReference, String retention, List<String> recipients, List<Transfer> transfers,
                         String impactAssessment, Set<ImpactScreening.Factor> riskFactors)
```

The part of a processing activity that the controller's data protection function decides and keeps up to date: why
the processing is lawful, how long the data is kept, who receives it, where it goes, the impact assessment and what
the officer knows of the risk that the code cannot. It is separate from the scope of the activity (purposes, data
subjects and personal data) because the two have different owners. The problems the [checker](checker.md) finds in
this part are those whose [element](checker.md#element) `isGovernance()`.

| Component | Article | Default | Description |
|---|---|---|---|
| `legalBasis` | 6.1 | `null` | The lawful basis; see [`LegalBasis`](bases.md#legalbasis). `null` means not declared. |
| `legitimateInterest` | 6.1.f | `null` | The interest pursued, when the basis is `LEGITIMATE_INTERESTS`. |
| `specialCategoryConditions` | 9.2 | empty | The conditions for special categories of data, one for each kind of special data processed under a different condition, such as explicit consent for health data and an employment law obligation for trade union membership; see [`SpecialCategoryCondition`](bases.md#specialcategorycondition). |
| `criminalDataBasis` | 10 | `null` | What allows processing criminal offence data; see [`CriminalDataBasis`](bases.md#criminaldatabasis). |
| `legalReference` | 6.3 | `null` | The Union or Member State law the bases rest on, as free text. One text covers every basis of the activity. |
| `retention` | 30.1.f | `null` | The envisaged time limit for erasure, as free text. |
| `recipients` | 30.1.d | empty | The categories of recipients, as free text. |
| `transfers` | 30.1.e | empty | Transfers to third countries or international organisations; see [`Transfer`](#transfer). |
| `impactAssessment` | 35 | `null` | A reference to the data protection impact assessment, such as its identifier or location. |
| `riskFactors` | 35 | empty | What the officer knows of the activity that the code cannot, such as its scale, for the [screening](impact-screening.md#declaring-risk-factors) of the need for an impact assessment. |

The canonical constructor **normalizes** the optional texts `legitimateInterest`, `legalReference`, `retention` and
`impactAssessment`: a blank text becomes `null` and any other text is stripped of leading and trailing white space.
The texts of `recipients` are not normalized. The lists are copied (immutable); a `null` list is an empty one and a
`null` element throws `NullPointerException`. `specialCategoryConditions` and `riskFactors` are copied into immutable
sets; `null` is an empty one.

| Member | Description |
|---|---|
| `static final Governance NONE` | Governance with nothing declared yet: every component `null` or empty. It is the governance of an activity that was given none. |
| `static Builder builder()` | Starts declaring governance from nothing. |
| `Builder toBuilder()` | A builder that starts from these values, to change part of them: list methods, `specialCategoryConditions` and `riskFactors` add to what is there, the other methods replace the value. The governance itself is not changed. |

### Governance.Builder

`Governance.Builder` — `public static final class`, `@since 0.1.0`. It has the same methods, with the same effect, as
the governance methods of the [`ProcessingActivity.Builder`](#builder): `legalBasis`, `legitimateInterest`,
`specialCategoryConditions(SpecialCategoryCondition...)`, `criminalDataBasis`, `legalReference`, `retention`,
`recipients`, `transfer` and `impactAssessment`, plus `Governance build()`. Its own
`riskFactors(ImpactScreening.Factor...)` declares the risk factors of the governance, those the officer knows of, as
opposed to the ones the code declares. List methods, `specialCategoryConditions` and `riskFactors` add to what was
given before and every other method replaces the previous value. `build()` can be called more than once.

### Scope in code, governance from storage

An application can declare the scope in code and read the governance from where its data protection officer edits it,
then combine both before checking, for example each time the officer saves a change:

```java
// In code: what the application knows for certain.
ProcessingActivity support = ProcessingActivity.builder("learning-support", "Learning support")
        .purposes(DataUses.EDUCATION_TEACHING)
        .dataSubjects(DataSubjects.STUDENT)
        .dataCategories(USER_NAME, USER_HEALTH_AND_MEDICAL_DISABILITY)
        .build();

// In the officer's storage: what the data protection function decides.
Governance stored = Governance.builder()
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .specialCategoryConditions(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
        .legalReference("Higher Education Act, arts. 12 and 31")
        .retention("Until the end of the studies")
        .recipients("Teaching staff")
        .impactAssessment("DPIA-2026-07")
        .build();

ProcessingChecker checker = new ProcessingChecker(Taxonomy.load());
checker.check(support).violations();                          // 3: no basis, no condition, no retention
checker.check(support.withGovernance(stored)).violations();   // []
```

The three violations of the first check are all about governance: their elements are `LEGAL_BASIS`,
`SPECIAL_CATEGORY_CONDITION` and `RETENTION`. There is none about the impact assessment: special category data is one
criterion of its [screening](impact-screening.md), and one criterion only is not enough to ask for an assessment. See
[what to do with the element of a violation](checker.md#element).

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
