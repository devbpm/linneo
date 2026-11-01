---
title: "Disclosure policy"
parent: "Reference"
nav_order: 11.2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/DisclosurePolicy.java
-->

# Disclosure policy

`io.github.devbpm.linneo.classify.DisclosurePolicy` — `public final class`, `@since 0.1.0`

How the values of classified fields may leave the system through one channel, such as a log, an export, a copy for a
test environment or an API: shown as they are, **masked**, or **left out**. Linneo decides; the application masks or
omits the values, since Linneo never reads them. Instances are immutable and thread-safe.

## Treatment

`DisclosurePolicy.Treatment` — nested `enum`, `@since 0.1.0`: what happens to a value, from the most open to the
strictest.

| Value | Meaning |
|---|---|
| `CLEAR` | Shown as it is. |
| `MASK` | Shown in a form that does not reveal it: replaced by asterisks, truncated, hashed or replaced by a pseudonym, as the application decides. |
| `OMIT` | Left out. |

## How a policy decides

A policy is a set of **thresholds** in the [partial order of protection](../protection-levels.md#protection): a field
whose protection [covers](../protection-levels.md#protection) a threshold gets at least that threshold's treatment, and
the strictest treatment that applies wins.

- A policy has any number of `mask` and `omit` thresholds. A value that needs a protection that covers none of them is
  `CLEAR`.
- **A field that holds a secret is `OMIT` in every policy**, even one with no thresholds: a secret is never read back,
  exported or published.
- **More protection never gets a laxer treatment.** Whatever thresholds a policy has, if the protection of a field
  covers that of another, its treatment is at least as strict. A hand-written rule on a few flags, such as "secrets
  omitted, personal data masked, the rest clear", gives no such guarantee: a field that needs more protection in a
  way the rule does not look at, such as a higher confidentiality, is treated like one that needs less.

The protection that is compared is that of the field: [`FieldClassification.protection()`](results.md#methods) for a
classified field, [`InventoryField.protection(taxonomy)`](inventory.md#inventoryfield) for a stored one. The nature of
a value that is not personal data (an anonymized or aggregated value of a personal category) is `NONE`, so it follows
its confidentiality.

## Members

| Member | Returns | Description |
|---|---|---|
| `static DisclosurePolicy forLogs()` | policy | The policy for logs, traces and error reports; see [For logs](#for-logs). |
| `static Builder builder()` | [`Builder`](#builder) | Starts a policy with no threshold: only secrets are left out. |
| `List<Protection> masked()` | immutable list | The thresholds from which a value is masked. |
| `List<Protection> omitted()` | immutable list | The thresholds from which a value is left out, besides secrets. |
| `Treatment treatment(Protection protection)` | `Treatment` | The strictest treatment whose threshold the protection covers; `OMIT` for a secret. `NullPointerException` if `protection` is `null`. |
| `Treatment treatment(FieldClassification field)` | `Treatment` | The treatment of the field's [`protection()`](results.md#methods). |
| `Treatment treatment(InventoryField field, Taxonomy taxonomy)` | `Treatment` | The treatment of a stored field, resolved with the taxonomy; `CLEAR` for an exempted field, which holds no data. `TaxonomyException` if the taxonomy does not contain the key. |
| `Map<String, Treatment> treatments(ClassClassification classification)` | immutable map | The treatment of every field of a class by path: the classified fields in order, then the exempted ones, which hold no data and are `CLEAR`. `IllegalArgumentException` if the class has errors. |

`treatments` throws for a class with errors because a field with an error is not in `fields()` and has no treatment:
leaving it out of the map would let it through. Build the map once per class and keep it.

`toString()` is `DisclosurePolicy[mask from <thresholds>, omit from <thresholds> and secrets]`.

## Builder

`DisclosurePolicy.Builder` — `public static final class`, `@since 0.1.0`. Created by `DisclosurePolicy.builder()`; it
has no public constructor.

| Member | Description |
|---|---|
| `Builder mask(Protection atLeast)` | Masks every value whose protection covers `atLeast`. `NullPointerException` if `null`. |
| `Builder omit(Protection atLeast)` | Leaves out every value whose protection covers `atLeast`. `NullPointerException` if `null`. |
| `DisclosurePolicy build()` | Builds the immutable policy. |

A threshold is a `Protection`, such as `Protection.of(LegalNature.ORDINARY, Confidentiality.PUBLIC, Integrity.LOW,
false)` for any personal data. For example, a policy for an export that masks personal data and leaves out special
category data:

```java
DisclosurePolicy export = DisclosurePolicy.builder()
        .mask(Protection.of(LegalNature.ORDINARY, Confidentiality.PUBLIC, Integrity.LOW, false))
        .omit(Protection.of(LegalNature.SPECIAL_CATEGORY, Confidentiality.PUBLIC, Integrity.LOW, false))
        .build();
```

## For logs

`DisclosurePolicy.forLogs()` is a policy for logs, traces and error reports:

| The field holds | Treatment |
|---|---|
| personal data (a threshold of `ORDINARY`) | `MASK` |
| data of `HIGH` confidentiality, personal or not | `MASK` |
| special category data | `OMIT` |
| criminal offence data | `OMIT` |
| a secret | `OMIT` |
| anything else | `CLEAR` |

The strictest applies: special category data of any level is `OMIT`, not `MASK`.

## Using it

```java
DisclosurePolicy logs = DisclosurePolicy.forLogs();
Map<String, DisclosurePolicy.Treatment> treatments = logs.treatments(classifier.classify(Student.class));

for (Map.Entry<String, DisclosurePolicy.Treatment> entry : treatments.entrySet()) {
    switch (entry.getValue()) {
        case CLEAR -> System.out.println(entry.getKey() + " shown");
        case MASK -> System.out.println(entry.getKey() + " masked");
        case OMIT -> System.out.println(entry.getKey() + " left out");
    }
}

DisclosurePolicy.Treatment stored = logs.treatment(inventoryField, taxonomy);   // a field of a data inventory
```

The application reads the values (through getters, a serializer filter or a logging converter) and applies the
treatment of each path; Linneo never touches them. What `MASK` looks like is also the application's decision.

See also: [Results](results.md) · [Data inventory](inventory.md) · [Protection levels](../protection-levels.md) ·
course: [Using the classification](../../course/05-using-the-classification.md)
