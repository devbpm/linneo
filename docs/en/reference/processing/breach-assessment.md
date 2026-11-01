---
title: "BreachAssessment"
parent: "Processing"
grand_parent: "Reference"
nav_order: 7
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/BreachAssessment.java
-->

# BreachAssessment

`io.github.devbpm.linneo.processing.BreachAssessment` — `public final class`, `@since 0.1.0`

The severity of a personal data breach and what the GDPR asks of the controller, following the methodology of ENISA, the
European Union Agency for Cybersecurity ("Recommendations for a methodology of the assessment of severity of personal
data breaches", v1.0, 2013):

```text
severity = data processing context × ease of identification + circumstances of the breach
```

Linneo scores the first two from the classification of the affected fields, and says which fields set each score. The
circumstances (how confidentiality, integrity and availability were lost, and malicious intent) are declared by whoever
handles the breach. Instances are immutable; build one with [`BreachAssessment.builder()`](#builder).

It is an **orientation for the first hours, not a decision**: the controller assesses the risk of each breach, and the
data protection officer decides whether to notify. A low severity is read as a breach unlikely to result in a risk
(article 33.1), which is recorded (article 33.5) but not notified; a high or very high one as a high risk, which is
also communicated to the data subjects (article 34.1) unless the data was unintelligible (article 34.3.a).

## How it is scored

**Data processing context** (1 to 4): how critical the affected data is. It is that of the most critical personal field
among those affected.

| Context | The affected data |
|---|---|
| 4 | special category data or criminal offence data |
| 3 | financial data: a category `user.financial` or below |
| 2 | behavioural data: `user.behavior` or `user.location` or below |
| 1 | other personal data |
| 0 | no affected field holds personal data: this is not a personal data breach (GDPR article 4.12) |

**Ease of identification**: how easily the affected data identifies the people it is about. It is the highest of the
levels of the affected fields.

| Level | Score | The affected fields |
|---|---|---|
| `MAXIMUM` | 1 | include a name (`user.name`), an identity document (`user.government_id`), biometric data (`user.biometric`) or a picture (`user.content.self_image`) |
| `SIGNIFICANT` | 0.75 | include contact data (`user.contact`) or account data (`user.account`) |
| `LIMITED` | 0.5 | include internal identifiers (`user.unique_id`), device identifiers (`user.device`) or any value declared pseudonymized; a pseudonymized name, contact or account is also `LIMITED` |
| `NEGLIGIBLE` | 0.25 | have none of those |

Both can be adjusted, as the methodology foresees, when the context of the data calls for it: see
[`adjustContext`](#builder) and [`identification`](#builder).

**Circumstances** (0 to 2) add up what the person who handles the breach declares:

| Declared with | Value | Points |
|---|---|---|
| `confidentiality(Exposure)` | `NONE`: not lost, or exposed with no evidence of unlawful processing, such as a lost encrypted laptop | 0 |
| | `KNOWN_RECIPIENTS`: disclosed to a number of known recipients, such as an email to the wrong people | +0.25 |
| | `UNKNOWN_RECIPIENTS`: disclosed to an unknown number of recipients, such as data published on the internet | +0.5 |
| `integrity(Alteration)` | `NONE`: not altered, or altered with no incorrect or unlawful use | 0 |
| | `RECOVERABLE`: altered and possibly used wrongly, but recoverable | +0.25 |
| | `UNRECOVERABLE`: altered and possibly used wrongly, and the original cannot be recovered | +0.5 |
| `availability(Unavailability)` | `NONE`: not lost, or recoverable without difficulty from other copies | 0 |
| | `TEMPORARY`: temporarily unavailable, recoverable with some work or from the person | +0.25 |
| | `PERMANENT`: lost for good, with no backup and no other source | +0.5 |
| `malicious(true)` | The breach was intentional, to harm the people or the controller | +0.5 |

**Severity** is the level of the score (`context × identification + circumstances`):

| `Severity` | Score | Meaning |
|---|---|---|
| `LOW` | below 2 | No effect, or a few inconveniences that people overcome without any problem. |
| `MEDIUM` | 2 to below 3 | Significant inconveniences that people overcome despite a few difficulties. |
| `HIGH` | 3 to below 4 | Significant consequences that people overcome with serious difficulties. |
| `VERY_HIGH` | 4 or more | Significant or irreversible consequences that people may not overcome. |

## What the GDPR asks

| Method | `true` when | Article |
|---|---|---|
| `notifyAuthority()` | personal data is affected and the severity is not `LOW` | 33.1: notify the supervisory authority within 72 hours of becoming aware of the breach. A `LOW` breach is only recorded (33.5). |
| `communicateToSubjects()` | personal data is affected, the severity is `HIGH` or `VERY_HIGH` and the data was **not** declared unintelligible | 34.1: tell the data subjects without undue delay. 34.3.a: not needed if the data was unintelligible to anyone without access to it, such as strongly encrypted with a key that was not compromised (`unintelligible(true)`). |
| `manyIndividuals()` | more than 100 people were declared affected | A flag of the methodology that weighs on the assessment without changing the score. |

## Members

| Member | Returns | Description |
|---|---|---|
| `static Builder builder()` | [`Builder`](#builder) | Starts an assessment with no affected field and no loss declared. |
| `int dataProcessingContext()` | `int` | The context, 1 to 4 after any adjustment; 0 when no affected field holds personal data. |
| `String contextReason()` | `String` | Why the context has its score: the kind of data and the field that sets it, in English, with the adjustment if any. |
| `Identification identification()` | level | The ease of identification. Each `Identification` has a `score()`: 0.25, 0.5, 0.75 or 1. |
| `String identificationReason()` | `String` | The identifiers found or the reason given, in English. |
| `double circumstances()` | `double` | The points of the circumstances, 0 to 2. |
| `double score()` | `double` | `context × identification + circumstances`. |
| `Severity severity()` | level | The level of the score. |
| `boolean manyIndividuals()` | `boolean` | Whether more than 100 people are affected. |
| `boolean notifyAuthority()` | `boolean` | See above. |
| `boolean communicateToSubjects()` | `boolean` | See above. |
| `String explanation()` | `String` | The assessment, its reasons and what to do, in English, one statement per line. |

`toString()` is `BreachAssessment[<severity>, <score>]`. The nested enums `Exposure`, `Alteration` and `Unavailability`
also have a `score()`, the points in the table above.

## Builder

`BreachAssessment.Builder` — `public static final class`, `@since 0.1.0`. Created by `BreachAssessment.builder()`; it
has no public constructor.

| Member | Effect |
|---|---|
| `Builder affected(Collection<FieldClassification> fields)` | **Adds** the classifications of the fields whose data was breached. |
| `Builder affected(Collection<InventoryField> fields, Taxonomy taxonomy)` | **Adds** stored fields of a [data inventory](../classify/inventory.md), resolved with the taxonomy; exempted fields are skipped. `TaxonomyException` if the taxonomy does not contain a key. |
| `Builder adjustContext(int delta, String reason)` | Adjusts the context, up when the volume of data or the vulnerability of the people makes it more critical, down when the data is public anyway. `delta` is between -3 and 3 (`IllegalArgumentException` otherwise) and the result stays between 1 and 4; it does not turn a breach with no personal data into one. The reason is added to `contextReason()`. |
| `Builder identification(Identification level, String reason)` | Sets the ease of identification instead of reading it from the fields, for example when a file name carries the person's name. |
| `Builder confidentiality(Exposure exposure)` | How confidentiality was lost; default `NONE`. |
| `Builder integrity(Alteration alteration)` | How integrity was lost; default `NONE`. |
| `Builder availability(Unavailability unavailability)` | How availability was lost; default `NONE`. |
| `Builder malicious(boolean malicious)` | Whether the breach was intentional; default `false`. |
| `Builder individuals(long count)` | How many people are affected. |
| `Builder unintelligible(boolean unintelligible)` | Whether the data was unintelligible to anyone without access to it. |
| `BreachAssessment build()` | Creates the assessment. |

The setters of the circumstances replace the previous value; `null` arguments throw `NullPointerException`.

## Example

```java
ClassClassification student = classifier.classify(Student.class);

BreachAssessment assessment = BreachAssessment.builder()
        .affected(student.fields())
        .confidentiality(BreachAssessment.Exposure.KNOWN_RECIPIENTS)
        .individuals(340)
        .build();

assessment.severity();               // VERY_HIGH
assessment.notifyAuthority();        // true: within 72 hours (GDPR article 33.1)
assessment.communicateToSubjects();  // true: GDPR article 34.1
System.out.println(assessment.explanation());
```

For a `Student` whose fields include a first name, an email and a disability degree, the explanation is:

```text
Severity VERY_HIGH (4.25 = 4 x 1 + 0.25), following ENISA's methodology (2013).
Data processing context 4: sensitive data user.health_and_medical.disability.
Ease of identification maximum (1): identifier user.name.first.
Circumstances +0.25: confidentiality KNOWN_RECIPIENTS, integrity NONE, availability NONE.
More than 100 people are affected (340).
Notify the supervisory authority within 72 hours of becoming aware of the breach (GDPR art. 33.1).
Tell the data subjects without undue delay (GDPR art. 34.1).
This is an orientation: the controller assesses the risk of each breach.
```

The score is `4 × 1 + 0.25`: sensitive data that names the people, sent to known recipients. If the same data had been
declared `unintelligible(true)`, the authority would still be notified but the line before the last would say that the
data subjects need not be told (article 34.3.a). A breach of grades and internal identifiers only, with nothing
disclosed, scores 0.5 (`LOW`): record it, do not notify.

See also: [ImpactScreening](impact-screening.md) · [ProcessingChecker](checker.md) ·
[Classifier](../classify/classifier.md) · [Processing activities](README.md)
