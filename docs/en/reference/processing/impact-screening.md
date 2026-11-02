---
title: "ImpactScreening"
parent: "Processing"
grand_parent: "Reference"
nav_order: 6
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ImpactScreening.java
  - src/main/java/io/github/devbpm/linneo/processing/Governance.java
  - src/main/java/io/github/devbpm/linneo/processing/CheckedActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingChecker.java
  - src/main/java/io/github/devbpm/linneo/Trait.java
-->

# ImpactScreening

`io.github.devbpm.linneo.processing.ImpactScreening` — `public record`, `@since 0.1.0`

```java
public record ImpactScreening(Outcome outcome, List<Finding> findings, String mandatoryCase)
```

Whether a [processing activity](activity.md) needs a data protection impact assessment (GDPR article 35), screened
against the nine criteria of the European Data Protection Board's guidelines (WP248 rev.01) and the cases that article
35.3 makes mandatory. It is a **screening, not the assessment**: it says which criteria apply and why, so that the data
protection officer knows where to look. Review the result against the list of the supervisory authority (article 35.4).

| Component | Description |
|---|---|
| `outcome` | Whether an assessment is needed; see [Outcome](#outcome). |
| `findings` | The criteria met, in the order of the guidelines, each with what shows it. |
| `mandatoryCase` | The provision of article 35.3 that makes the assessment mandatory, such as `35.3.b`, or `null`. |

The canonical constructor throws `NullPointerException` if `outcome` or `findings` is `null`, and copies the list
(immutable). Every [`CheckedActivity`](checker.md#checkedactivity) has one, from `screening()`; the
[`ProcessingChecker`](checker.md) uses it to report the need of an assessment the activity does not refer to, and the
[record](record.md) shows it.

## What the code can see and what only the officer knows

Some criteria show in what the activity declares. The others are facts that the terms do not tell, such as the scale of
the processing or automated decisions: the code declares those it knows and the officer the rest, as **risk factors**.

The criteria the code can see are read from the [traits](../terms.md#trait) of the activity's terms, not from lists of
keys, so a term of an extension or of your own counts as soon as it declares its traits.

| Criterion | Found from the activity | Declared with |
|---|---|---|
| 1. `EVALUATION`: evaluation or scoring, including profiling | a purpose with the `EVALUATION` trait | `Factor.EVALUATION` |
| 2. `AUTOMATED_DECISIONS`: automated decisions with legal or similarly significant effect | not visible | `Factor.AUTOMATED_DECISIONS` |
| 3. `SYSTEMATIC_MONITORING`: systematic monitoring | a purpose or a data category with the `MONITORING` trait | `Factor.SYSTEMATIC_MONITORING`, `Factor.PUBLICLY_ACCESSIBLE_AREA` |
| 4. `SENSITIVE_DATA`: sensitive data or data of a highly personal nature | a special category or criminal offence data category, or a category with the `FINANCIAL`, `BEHAVIOURAL` or `PRIVATE_LIFE` trait | — |
| 5. `LARGE_SCALE`: data processed on a large scale | not visible | `Factor.LARGE_SCALE` |
| 6. `DATASET_MATCHING`: matching or combining datasets | not visible | `Factor.DATASET_MATCHING` |
| 7. `VULNERABLE_SUBJECTS`: data concerning vulnerable data subjects | a data subject or a data category with the `VULNERABLE` trait | `Factor.VULNERABLE_SUBJECTS` |
| 8. `INNOVATIVE_TECHNOLOGY`: innovative use or new technological or organisational solutions | not visible | `Factor.INNOVATIVE_TECHNOLOGY` |
| 9. `PREVENTS_RIGHTS`: processing that prevents people from exercising a right or using a service or a contract | not visible | `Factor.PREVENTS_RIGHTS` |

The purposes, data subjects and data categories are those the checker resolved: deprecated keys are replaced, and only
personal data counts. A criterion is met once, however many things show it; the findings list them all in its evidence,
**sorted**, so the screening does not depend on the order in which the activity lists its terms.

The standard taxonomy declares the traits in [`linneo-traits.tsv`](../standard-taxonomy.md#traits-of-the-standard-terms):
the purposes with `EVALUATION` are `marketing.advertising.profiling`, `marketing.advertising.first_party.targeted`,
`marketing.advertising.third_party.targeted`, `personalize.content.profiling`, `personalize.content.profiled`,
`essential.fraud_detection` and the deprecated `personalize.profiling` (screened as `personalize.content.profiling`);
`employee`, `staff_member`, `faculty_member`, `trainee`, `patient` and `job_applicant` are `VULNERABLE`, like the
categories `user.childrens`, `user.victim_status` and `user.government_id.immigration`; `user.financial` and
`user.payment` are `FINANCIAL`, `user.behavior` and `user.location` are `BEHAVIOURAL`, and `user.content.private` is
`PRIVATE_LIFE`. The standard taxonomy has two `MONITORING` categories, `user.telemetry` and `user.sensor`, and no
`MONITORING` purpose: those come from the [extensions](../extensions/README.md#traits), such as `employment.monitoring`
and `user.employment.monitoring` of the [human resources extension](../extensions/human-resources.md); with the
standard taxonomy alone no purpose meets criterion 3. `education.financial_aid.means_assessment` is an `EVALUATION`
purpose of the [university extension](../extensions/university.md). An extension or an application that declares the
traits of its own terms is read like the standard ones. In Spain, `BIOMETRIC` and `GENETIC` data count as a criterion
of their own for the checker; see [In the checker and the record](#in-the-checker-and-the-record).

`Criterion.number()` is the number of the criterion in the guidelines, 1 to 9. A `Factor` has a `criterion()`: the one
it meets.

### Declaring risk factors

Risk factors have two homes, both a `Set<ImpactScreening.Factor>` set with a `riskFactors(...)` method that **adds** the
factors it is given to those declared before. What the code knows the activity does, such as
`Factor.AUTOMATED_DECISIONS` when the code decides on its own, is part of its scope: the `riskFactors` component of the
[`ProcessingActivity`](activity.md#components), set with its builder. What only the officer knows, such as the scale of
the processing, belongs to the [governance](activity.md#governance) she keeps: `Governance.riskFactors()`.

```java
Governance declared = Governance.builder()
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .riskFactors(ImpactScreening.Factor.LARGE_SCALE, ImpactScreening.Factor.DATASET_MATCHING)
        .build();
declared.riskFactors();   // [LARGE_SCALE, DATASET_MATCHING]

ProcessingActivity scoring = ProcessingActivity.builder("scoring", "Applicant scoring")
        .riskFactors(ImpactScreening.Factor.AUTOMATED_DECISIONS)
        .build();
scoring.riskFactors();                                // [AUTOMATED_DECISIONS]
scoring.withGovernance(declared).allRiskFactors();    // [AUTOMATED_DECISIONS, LARGE_SCALE, DATASET_MATCHING]
```

The screening reads the union, `ProcessingActivity.allRiskFactors()`. `withGovernance` only replaces the governance, so
a governance saved before the code changed never lifts what the code declares. A change of the risk factors between
two records is the governance aspect `RISK_FACTORS` of an [`ActivityChange`](record.md#aspect) when it is the
governance that changes, and the scope aspect `DECLARED_RISK_FACTORS` when it is the code.

## Outcome

`ImpactScreening.Outcome` — nested `enum`, `@since 0.1.0`: whether an assessment is needed, from the least to the most.

| Value | When | Meaning |
|---|---|---|
| `NOT_INDICATED` | no criterion is met | Nothing in the activity points to an assessment. |
| `POSSIBLE` | one criterion is met | An assessment may be needed, depending on the case. |
| `LIKELY` | two criteria or more are met | In most cases an assessment is required. |
| `REQUIRED` | a case of article 35.3 applies | An assessment is required. |

### The cases of article 35.3

The outcome is `REQUIRED` and `mandatoryCase()` names the provision when one of these applies, checked in this order:

| `mandatoryCase` | Article | Linneo finds it when |
|---|---|---|
| `35.3.a` | Systematic and extensive evaluation of personal aspects, with automated decisions that have legal or similar effects | criterion 1 (`EVALUATION`) is met, by a purpose or by a declared factor, **and** `Factor.AUTOMATED_DECISIONS` is declared |
| `35.3.b` | Processing on a large scale of special categories of data or of criminal offence data | the activity processes special category or criminal offence data **and** `Factor.LARGE_SCALE` is declared |
| `35.3.c` | Systematic monitoring of a publicly accessible area on a large scale | `Factor.PUBLICLY_ACCESSIBLE_AREA` **and** `Factor.LARGE_SCALE` are declared |

Highly personal data (financial, location, behaviour, private content) meets criterion 4, but it is not a case of
article 35.3.b: only special category and criminal offence data are.

## Finding

`ImpactScreening.Finding` — nested `record`, `@since 0.1.0`

```java
public record Finding(Criterion criterion, String evidence)
```

A criterion met, with what shows it. `evidence` is in English and joins, with `; `, everything that meets the criterion:
`purpose <key>`, `special category data <key>`, `criminal offence data <key>`, `highly personal data <key>`,
`data category <key>`, `data subjects <key>` and `declared <FACTOR>`. The constructor throws `NullPointerException` if a
component is `null`.

## Methods

| Member | Returns | Description |
|---|---|---|
| `static ImpactScreening of(CheckedActivity activity)` | screening | Screens a checked activity, as [`CheckedActivity.screening()`](checker.md#checkedactivity) does. |
| `Set<Criterion> criteria()` | immutable set | The criteria met, in the order of the guidelines. |
| `String summary()` | `String` | A one-line explanation, in English: the outcome, the provision or the number of criteria, and the findings. |

`summary()` is the head `Not indicated`, `Possible (1 criterion of WP248)`, `Likely required (<n> criteria of WP248)` or
`Required (GDPR art. <case>)`, followed, when there are findings, by `: ` and each finding as
`<number>. <criterion in lower case>: <evidence>`, separated by `; `.

## In the checker and the record

When an activity processes personal data and refers to **no** impact assessment (`Governance.impactAssessment()`), the
[`ProcessingChecker`](checker.md) reports what the screening finds:

| Outcome | Violation | Message |
|---|---|---|
| `REQUIRED` | **error** `IMPACT_ASSESSMENT_REQUIRED` | `GDPR article <case> requires an impact assessment; refer to it. <summary>` |
| `LIKELY` | **warning** `IMPACT_ASSESSMENT_RECOMMENDED` | `GDPR article 35 likely requires an impact assessment. <summary>` |
| `POSSIBLE`, `NOT_INDICATED` | none | |

Under `Jurisdiction.SPAIN` the warning cites the list of the Spanish supervisory authority (article 35.4) and reads
`GDPR article 35 likely requires an impact assessment (the AEPD's list under article 35.4 requires one in most cases
that meet two criteria or more). <summary>`.

Under `Jurisdiction.SPAIN` the checker adds one more case. The AEPD's list under article 35.4 counts biometric data for
identification and genetic data, the categories with the `BIOMETRIC` or `GENETIC` trait, as criteria of their own,
besides special category data. When the screening is not already `LIKELY` or `REQUIRED` and the activity refers to no
assessment, it raises the warning `IMPACT_ASSESSMENT_RECOMMENDED`: `The AEPD's list under GDPR article 35.4 counts
biometric data for identification and genetic data as a criterion besides special category data, so an impact
assessment is required in most cases: <keys>. <summary>`. The screening itself, and so the record, shows the criteria
of the guidelines as before.

The error and the warnings are reported on the element `IMPACT_ASSESSMENT`, which belongs to the governance, and the
error makes `verify` fail like any other. An activity that refers to an assessment gets no violation, whatever the
outcome. The [record](record.md) shows the screening of every activity either way, in a row of the Markdown and in a
`screening` object of the JSON.

## Example

An activity that scores applicants with automated decisions is a case of article 35.3.a, found from the purpose and the
declared factor:

```java
ProcessingActivity scoring = ProcessingActivity.builder("applicant-scoring", "Applicant scoring")
        .purposes(DataUses.PERSONALIZE_CONTENT_PROFILING)
        .dataSubjects(DataSubjects.APPLICANT)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL)
        .legalBasis(LegalBasis.CONSENT)
        .retention("Until the end of the admission cycle")
        .riskFactors(ImpactScreening.Factor.AUTOMATED_DECISIONS)
        .build();

CheckedActivity checked = checker.check(scoring);
ImpactScreening screening = checked.screening();
screening.outcome();         // REQUIRED
screening.mandatoryCase();   // "35.3.a"
screening.criteria();        // [EVALUATION, AUTOMATED_DECISIONS]
checked.isValid();           // false
```

```text
ERROR IMPACT_ASSESSMENT_REQUIRED applicant-scoring: GDPR article 35.3.a requires an impact assessment; refer to it. Required (GDPR art. 35.3.a): 1. evaluation: purpose personalize.content.profiling; 2. automated decisions: declared AUTOMATED_DECISIONS
```

With `.impactAssessment("DPIA-2026-04")` there is no violation, and the screening is the same. An activity with one
criterion only is `POSSIBLE` and raises nothing: special category data alone, as in an admissions activity, is one
criterion, and the guidelines ask for an assessment in most cases only from two. Declare `Factor.LARGE_SCALE` for it
and it is a case of article 35.3.b: the outcome is `REQUIRED`.

See also: [ProcessingChecker](checker.md) · [ProcessingActivity](activity.md#governance) ·
[ProcessingRecord](record.md) · [BreachAssessment](breach-assessment.md) · [Processing activities](README.md)
