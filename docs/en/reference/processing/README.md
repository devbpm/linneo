---
title: "Processing"
parent: "Reference"
nav_order: 12
permalink: "/en/reference/processing/"
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/package-info.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingChecker.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingViolation.java
-->

# Processing activities

Package `io.github.devbpm.linneo.processing` — exported, `@since 0.1.0`

The annotations and the [`Classifier`](../classify/classifier.md) say what each **field** holds. A GDPR record of
processing activities (article 30) is about **processes**: why the data is processed, whose data it is, on which
lawful basis, for how long and who receives it. This package describes those processes, checks them against the
taxonomy and the classification of the model, and renders the record.

| Type | Kind | Role | Page |
|---|---|---|---|
| `ProcessingActivity` | record, with a `Builder` | One processing activity as declared: purposes, data subjects, personal data, bases, retention, recipients, transfers | [ProcessingActivity](activity.md) |
| `Transfer` | record | A transfer to a third country or an international organisation | [ProcessingActivity](activity.md#transfer) |
| `LegalBasis` | enum | The lawful basis (article 6.1) | [Legal bases](bases.md#legalbasis) |
| `SpecialCategoryCondition` | enum | The exception for special categories of data (article 9.2) | [Legal bases](bases.md#specialcategorycondition) |
| `CriminalDataBasis` | enum | What allows criminal offence data (article 10) | [Legal bases](bases.md#criminaldatabasis) |
| `TransferSafeguard` | enum | What allows a transfer (chapter V) | [Legal bases](bases.md#transfersafeguard) |
| `ProcessingChecker` | class | Checks activities and builds the record | [ProcessingChecker](checker.md) |
| `CheckedActivity` | record | An activity with its terms resolved, its personal data, the protection it requires and the problems found | [ProcessingChecker](checker.md#checkedactivity) |
| `ProcessingViolation` | record, with `Severity` and `Code` | A problem found in an activity | [ProcessingChecker](checker.md#processingviolation) |
| `ProcessingException` | exception | Thrown by `verify` with every error | [ProcessingChecker](checker.md#processingexception) |
| `ProcessingRecord` | record | The record of processing activities, as Markdown or JSON | [ProcessingRecord](record.md) |

## How it relates to the taxonomy

A `Taxonomy` has three dimensions. The annotations use only the first one; a processing activity uses all three:

| Taxonomy dimension | In a `ProcessingActivity` | Record item |
|---|---|---|
| **Data uses** (`DataUses`) | `purposes`: why the data is processed | purposes (art. 30.1.b) |
| **Data subjects** (`DataSubjects`) | `dataSubjects`: whose data it is | categories of data subjects (art. 30.1.c) |
| **Data categories** (`Categories`) | `dataCategories` (keys) and `entities` (classified model classes) | categories of personal data (art. 30.1.c) |

The personal data of an activity can be given as category keys, as model classes, or both. For a class, the
checker runs the [`Classifier`](../classify/classifier.md) and takes the categories of its personal fields, with the
levels declared on each field, so the record follows the model: add a personal field to an entity and every activity
that lists the entity processes it. An entity that is not fully and correctly classified is an error of the activity.
The legal nature of the categories (ordinary, special category, criminal offence data; see
[Protection levels](../protection-levels.md)) decides which bases the activity needs.

The vocabularies of the bases follow fideslang's privacy declarations: `LegalBasis`, `SpecialCategoryCondition` and
`TransferSafeguard` carry the values of `legal_basis_for_processing`, `special_category_legal_basis` and
`legal_basis_for_transfers` in `fideslangName()`.

## Errors and warnings

The [`ProcessingChecker`](checker.md) follows the same fail-closed approach as the classifier, with one rule for the
severity:

- **Error**: the activity, as declared, would be unlawful or cannot be checked. Personal data without a lawful basis,
  special categories without an article 9.2 condition, criminal offence data without an article 10 basis, an
  unknown key, an activity with no purpose, no data subjects or no personal data, an entity with classification
  errors, two activities with the same key. `verify` throws a `ProcessingException` and no record is produced.
- **Warning**: the record is incomplete or should be reviewed. A deprecated key, a non-personal category, a basis
  resting on law without naming the law, legitimate interests without the interest pursued, no retention, a
  condition declared for data the activity does not process, and special category, criminal offence or profiling
  activities with no impact assessment. Verification passes and the warnings appear in the record as **open
  points**.

The full list is in the [codes table](checker.md#codes).

## What Linneo does not do

Linneo checks **declarations**: that each activity names a basis where the GDPR requires one, that its keys exist and
that its entities are classified. It does not decide whether consent is valid, whether an interest outweighs the
rights of the data subjects or whether a law really covers the processing, and it does not give legal advice. The
record it produces supports, and does not replace, the controller's own assessment and the advice of its data
protection officer. The laws named in the examples of this documentation ("Higher Education Act", "Child Protection
Act") are fictional.

## Example

```java
ProcessingActivity newsletter = ProcessingActivity.builder("alumni-newsletter", "Alumni newsletter")
        .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL)
        .dataSubjects(DataSubjects.ALUMNUS)
        .dataCategories(Categories.USER_NAME, Categories.USER_CONTACT_EMAIL)
        .legitimateInterest("Keeping former students informed about university events")
        .retention("Until the alumnus unsubscribes")
        .build();

ProcessingRecord record = new ProcessingChecker(Taxonomy.load()).verify(newsletter);
String markdown = record.toMarkdown("Example University");
```

See also: [ProcessingActivity](activity.md) · [Legal bases](bases.md) · [ProcessingChecker](checker.md) ·
[ProcessingRecord](record.md) · course: [Processing activities and the record](../../course/09-processing-activities.md)
