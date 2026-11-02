---
title: "SubjectRights"
parent: "Processing"
grand_parent: "Reference"
nav_order: 5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/SubjectRights.java
  - src/main/java/io/github/devbpm/linneo/Trait.java
-->

# SubjectRights

`io.github.devbpm.linneo.processing.SubjectRights` — `public final class`, `@since 0.1.0`

What the [record of processing activities](record.md) says to someone of one category of data subjects who exercises
their rights (GDPR articles 15 to 22): the activities that process their data, what each one processes and why, where
that data is stored, and which rights each activity's lawful basis gives them. Instances are immutable; there is no
public constructor.

It is the **starting point of the answer, not the answer**: the data protection officer decides each request, since the
conditions of each article depend on the case. Linneo reads the record; locating, giving, correcting or erasing the
data is the application's.

```java
SubjectRights rights = SubjectRights.of(record, taxonomy, DataSubjects.STUDENT, Jurisdiction.SPAIN);
Map<String, List<InventoryField>> where = rights.storedFields(inventory, taxonomy);   // where to look for the data
for (SubjectRights.ActivityRights activity : rights.activities()) {
    SubjectRights.Entitlement erasure = activity.entitlement(SubjectRights.Right.ERASURE);
    System.out.println(activity.activity().activity().key() + ": " + erasure.availability());
}
```

## Members

| Member | Returns | Description |
|---|---|---|
| `static SubjectRights of(ProcessingRecord record, Taxonomy taxonomy, String dataSubject)` | rights | The rights under the GDPR alone (`Jurisdiction.EU`). |
| `static SubjectRights of(ProcessingRecord record, Taxonomy taxonomy, String dataSubject, Jurisdiction jurisdiction)` | rights | The rights in a [jurisdiction](checker.md#jurisdiction), over every activity of the record whose data subjects include the category `dataSubject` (a key such as `student`). The key is resolved with `taxonomy`, the one the record was checked with: a deprecated key stands for its replacement. `NullPointerException` if an argument is `null`; `TaxonomyException` if the taxonomy has no such data subject; `IllegalArgumentException` if an activity of the record has no lawful basis (build the record with `ProcessingChecker.verify`). |
| `String dataSubject()` | `String` | The key of the category, resolved: the replacement if the key given was deprecated. |
| `List<ActivityRights> activities()` | immutable list | The activities that process data of the category, with the rights over each, in the order of the record. |
| `Optional<ActivityRights> activity(String activity)` | `Optional` | The rights over one activity, or empty if the activity does not process data of the category. |
| `boolean isEmpty()` | `boolean` | `true` if no activity processes data of the category: there is nothing to answer but that. |
| `List<DataCategory> dataCategories()` | immutable list | The personal data categories the activities process, in key order. |
| `Set<String> objects()` | immutable set | The objects of a data inventory that the activities name, sorted: where the person's data is stored. |
| `List<Class<?>> entities()` | immutable list | The entity classes the activities name, each once, in the order of the activities. |
| `Map<String, List<InventoryField>> storedFields(DataInventory inventory, Taxonomy taxonomy)` | immutable map | The fields that hold personal data in the `objects()`: where to look for the person's data to give access to it, export it, rectify it or erase it. Objects sorted, fields in the order of the inventory; an object the inventory does not hold, or without personal fields, has no entry. `TaxonomyException` if the taxonomy does not contain a key of the inventory. |

A category that no activity names gives an empty result, but a key the taxonomy does not have is an error, so a typo is
not read as "no data". `entities()` and `objects()` are what the activities declare; the inventory gives the fields of
the objects.

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
| `AUTOMATED_DECISION` | `22` | Not to be subject to a decision based solely on automated processing. |

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

The availability follows the lawful basis of the activity (`governance().legalBasis()`), which every activity of a
record built with `ProcessingChecker.verify` has:

| Right | Consent | Contract | Legal obligation | Vital interests | Public interest | Legitimate interests |
|---|---|---|---|---|---|---|
| `ACCESS` (15) | `YES` | `YES` | `YES` | `YES` | `YES` | `YES` |
| `RECTIFICATION` (16) | `YES` | `YES` | `YES` | `YES` | `YES` | `YES` |
| `ERASURE` (17) | `YES` (17.1.b) | `CONDITIONAL` (17.1.a) | `NO` (17.3.b) | `CONDITIONAL` (17.1.a) | `NO` (17.3.b) | `CONDITIONAL` (17.1.c) |
| `RESTRICTION` (18) | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` |
| `PORTABILITY` (20) | `YES` | `YES` | `NO` | `NO` | `NO` | `NO` |
| `OBJECTION` (21) | `NO` | `NO` | `NO` | `NO` | `CONDITIONAL` (21.1) | `CONDITIONAL` (21.1) |

The table has no row for `AUTOMATED_DECISION`: it does not depend on the lawful basis (see below).

- **Erasure** is not available while a legal obligation or a task in the public interest needs the data (article
  17.3.b), and the explanation then carries the retention the activity declares (`not declared` if it has none). With
  consent it follows the withdrawal of consent (17.1.b), with a contract or vital interests once the data is no longer
  needed (17.1.a), with legitimate interests after an objection unless there are overriding legitimate grounds (17.1.c).
- **Portability** applies to processing based on consent or a contract (article 20.1), and to the special category data
  processed with explicit consent (article 20.1.a; see below). It does not apply to processing necessary for a task in
  the public interest (article 20.3), even with explicit consent.
- **Objection** applies to processing based on a task in the public interest or legitimate interests (article 21.1).
- **Automated decisions** (article 22) do not depend on the basis but on what the activity declares. If the activity
  declares `Factor.AUTOMATED_DECISIONS`, in the scope or in the governance, the right is `CONDITIONAL`: the person is
  not subject to a decision based solely on automated processing with legal or similarly significant effects, unless
  it is necessary for a contract, authorised by law or based on explicit consent, and then may obtain human
  intervention, express their view and contest the decision (article 22.3). The explanation of `ACCESS` adds the
  automated decisions, their logic and consequences (article 15.1.h). Without the factor the right is `NO`.
- **Restriction** is `CONDITIONAL` in every activity: it applies while the accuracy of the data or an objection is
  being verified, when the processing is unlawful and the person prefers restriction, or when the person needs the data
  for legal claims (article 18.1).
- **Direct marketing.** If a purpose of the activity has the [`DIRECT_MARKETING` trait](../terms.md#trait) (the
  standard `marketing` and `sales` uses and everything below them, the alumni relations, fundraising and prospect
  research uses of the university extension, or a term of yours that declares it), whatever the basis, `OBJECTION` is
  `YES` (articles 21.2 and 21.3: the processing stops) and `ERASURE` is `YES` (articles 17.1.c and 21.2: the person may
  object at any time and the data is then erased). When the basis
  is a legal obligation or a task in the public interest, `ERASURE` is `CONDITIONAL` instead (articles 17.1.c and
  21.3): the data is erased except what article 17.3.b keeps, and the explanation carries the retention. `ERASURE` is
  also `CONDITIONAL` when direct marketing is one purpose beside others: the data is erased except what the activity's
  other purposes still need.
- **Research.** If a purpose of the activity has the `RESEARCH` trait (the `research.participation` use of the university
  extension, or a term of yours that declares it), `OBJECTION` is `CONDITIONAL` (article 21.6: on grounds relating to the person's
  particular situation) or `NO` when the basis is a task in the public interest, and an `ERASURE` that is not `NO`
  becomes `CONDITIONAL`, with an explanation that notes that it may be refused where it would make the research
  impossible or seriously impair it (article 17.3.d). Direct marketing comes first for `OBJECTION`.
- **Explicit consent.** If the activity declares explicit consent as a condition for special category data (article
  9.2.a, in [`specialCategoryConditions`](activity.md#governance)): when its lawful basis is none of consent, a contract
  or a task in the public interest, `PORTABILITY` is `CONDITIONAL` instead of `NO` (article 20.1.a: it covers the
  special category data, not the rest), whereas for a task in the public interest it stays `NO` (article 20.3); and
  when its basis is not consent, `ERASURE` is `CONDITIONAL` unless it is already `YES`, and the explanation adds that
  the special category data is erased when the consent is withdrawn (articles 17.1.b and 9.2.a), unless another
  condition of article 9.2 declared for the activity covers it, when the activity declares others.
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
