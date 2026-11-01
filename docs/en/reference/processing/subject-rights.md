---
title: "SubjectRights"
parent: "Processing"
grand_parent: "Reference"
nav_order: 5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/SubjectRights.java
-->

# SubjectRights

`io.github.devbpm.linneo.processing.SubjectRights` — `public final class`, `@since 0.1.0`

What the [record of processing activities](record.md) says to someone of one category of data subjects who exercises
their rights (GDPR articles 15 to 21): the activities that process their data, what each one processes and why, where
that data is stored, and which rights each activity's lawful basis gives them. Instances are immutable; there is no
public constructor.

It is the **starting point of the answer, not the answer**: the data protection officer decides each request, since the
conditions of each article depend on the case. Linneo reads the record; locating, giving, correcting or erasing the
data is the application's.

```java
SubjectRights rights = SubjectRights.of(record, DataSubjects.STUDENT, Jurisdiction.SPAIN);
Map<String, List<InventoryField>> where = rights.storedFields(inventory, taxonomy);   // where to look for the data
for (SubjectRights.ActivityRights activity : rights.activities()) {
    SubjectRights.Entitlement erasure = activity.entitlement(SubjectRights.Right.ERASURE);
    System.out.println(activity.activity().activity().key() + ": " + erasure.availability());
}
```

## Members

| Member | Returns | Description |
|---|---|---|
| `static SubjectRights of(ProcessingRecord record, String dataSubject)` | rights | The rights under the GDPR alone (`Jurisdiction.EU`). |
| `static SubjectRights of(ProcessingRecord record, String dataSubject, Jurisdiction jurisdiction)` | rights | The rights in a [jurisdiction](checker.md#jurisdiction), over every activity of the record whose data subjects include the category `dataSubject` (a key such as `student`). `NullPointerException` if an argument is `null`. |
| `String dataSubject()` | `String` | The key of the category. |
| `List<ActivityRights> activities()` | immutable list | The activities that process data of the category, with the rights over each, in the order of the record. |
| `Optional<ActivityRights> activity(String activity)` | `Optional` | The rights over one activity, or empty if the activity does not process data of the category. |
| `boolean isEmpty()` | `boolean` | `true` if no activity processes data of the category: there is nothing to answer but that. |
| `List<DataCategory> dataCategories()` | immutable list | The personal data categories the activities process, in key order. |
| `Set<String> objects()` | immutable set | The objects of a data inventory that the activities name, sorted: where the person's data is stored. |
| `List<Class<?>> entities()` | immutable list | The entity classes the activities name, each once, in the order of the activities. |
| `Map<String, List<InventoryField>> storedFields(DataInventory inventory, Taxonomy taxonomy)` | immutable map | The fields that hold personal data in the `objects()`: where to look for the person's data to give access to it, export it, rectify it or erase it. Objects sorted, fields in the order of the inventory; an object the inventory does not hold, or without personal fields, has no entry. `TaxonomyException` if the taxonomy does not contain a key of the inventory. |

A category that no activity names gives an empty result, not an error. `entities()` and `objects()` are what the
activities declare; the inventory gives the fields of the objects.

## Right

`SubjectRights.Right` — nested `enum`, `@since 0.1.0`: a right of the data subject that depends on the processing.
`String article()` is the GDPR article number.

| Value | `article()` | Right |
|---|---|---|
| `ACCESS` | `15` | Access to the data and to the information about its processing. |
| `RECTIFICATION` | `16` | Rectification of inaccurate data. |
| `ERASURE` | `17` | Erasure. |
| `RESTRICTION` | `18` | Restriction of processing. |
| `PORTABILITY` | `20` | Data portability. |
| `OBJECTION` | `21` | Objection. |

## Availability

`SubjectRights.Availability` — nested `enum`, `@since 0.1.0`: whether a right applies to an activity.

| Value | Meaning |
|---|---|
| `YES` | It applies. |
| `CONDITIONAL` | It applies in the cases the article lists; the explanation says which. |
| `NO` | It does not apply to this activity, as the explanation says. |

## Entitlement and ActivityRights

```java
public record Entitlement(Right right, Availability availability, String explanation)
```

A right as it applies to one activity. `explanation` is a text in English that cites the provisions. The constructor
throws `NullPointerException` if a component is `null`.

```java
public record ActivityRights(CheckedActivity activity, List<Entitlement> entitlements)
```

The rights a data subject has over one activity: `activity` is the [`CheckedActivity`](checker.md#checkedactivity) of
the record, and `entitlements` holds one `Entitlement` per `Right`, in the order of the rights (an immutable list).
`Entitlement entitlement(Right right)` returns the one of a right.

## The rules

The availability follows the lawful basis of the activity (`governance().legalBasis()`):

| Right | Consent | Contract | Legal obligation | Vital interests | Public interest | Legitimate interests |
|---|---|---|---|---|---|---|
| `ACCESS` (15) | `YES` | `YES` | `YES` | `YES` | `YES` | `YES` |
| `RECTIFICATION` (16) | `YES` | `YES` | `YES` | `YES` | `YES` | `YES` |
| `ERASURE` (17) | `YES` (17.1.b) | `CONDITIONAL` (17.1.a) | `NO` (17.3.b) | `CONDITIONAL` (17.1.a) | `NO` (17.3.b) | `CONDITIONAL` (17.1.c) |
| `RESTRICTION` (18) | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` |
| `PORTABILITY` (20) | `YES` | `YES` | `NO` | `NO` | `NO` | `NO` |
| `OBJECTION` (21) | `NO` | `NO` | `NO` | `NO` | `CONDITIONAL` (21.1) | `CONDITIONAL` (21.1) |

- **Erasure** is not available while a legal obligation or a task in the public interest needs the data (article
  17.3.b), and the explanation then carries the retention the activity declares (`not declared` if it has none). With
  consent it follows the withdrawal of consent (17.1.b), with a contract or vital interests once the data is no longer
  needed (17.1.a), with legitimate interests after an objection unless there are overriding legitimate grounds (17.1.c).
- **Portability** applies to processing based on consent or a contract (article 20.1).
- **Objection** applies to processing based on a task in the public interest or legitimate interests (article 21.1).
- **Restriction** is `CONDITIONAL` in every activity: it applies while the accuracy of the data or an objection is
  being verified, when the processing is unlawful and the person prefers restriction, or when the person needs the data
  for legal claims (article 18.1).
- **Direct marketing.** If a purpose of the activity is `marketing` or under `marketing.*`, whatever the basis,
  `ERASURE` is `YES` (articles 17.1.c and 21.2: the person may object at any time and the data is then erased) and
  `OBJECTION` is `YES` (articles 21.2 and 21.3: the processing stops).
- **Access** is `YES` in every activity, and its explanation lists the purposes of the activity by name. The
  explanations of the other rights cite the articles above.
- **`Jurisdiction.SPAIN`.** The explanations of `RECTIFICATION` and `ERASURE` add that the data is blocked, not
  deleted, while liabilities may arise from the processing, and deleted afterwards (LOPDGDD article 32). With
  `Jurisdiction.EU` they do not.

`SubjectRights` does not check whether a particular request meets the conditions of an article. A `CONDITIONAL` right
and the explanation of a `NO` are for the officer to apply to the case.

See also: [ProcessingRecord](record.md) · [ProcessingChecker](checker.md) · [Legal bases](bases.md) ·
[Data inventory](../classify/inventory.md) · course:
[Processing activities and the record](../../course/09-processing-activities.md)
