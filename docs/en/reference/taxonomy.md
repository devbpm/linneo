---
title: "Taxonomy"
parent: "Reference"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/Taxonomy.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyException.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# Taxonomy

`io.github.devbpm.linneo.Taxonomy` — `public final class`, `@since 0.1.0`

An immutable set of terms in three hierarchies: data categories ([`DataCategory`](terms.md#datacategory)), data uses
([`DataUse`](terms.md#datause)) and data subjects ([`DataSubject`](terms.md#datasubject)). Instances are thread-safe:
build one per application and share it. Every list is ordered by key, so a parent always comes before its
descendants.

There is no public constructor. Obtain a taxonomy with one of the factory methods.

## Obtaining a taxonomy

| Method | Returns | Description |
|---|---|---|
| `static Taxonomy standard()` | `Taxonomy` | The [standard taxonomy](standard-taxonomy.md): fideslang default taxonomy plus Linneo's extensions, **without** application extensions. Created once and shared: every call returns the same instance. |
| `static Taxonomy load()` | `Taxonomy` | The standard taxonomy plus every [`TaxonomyExtension`](extending.md#taxonomyextension) registered with `ServiceLoader`. Equivalent to `builder().withStandard().withExtensions().build()`. Builds a **new instance on every call**: keep the result. Throws `TaxonomyException` if an extension breaks the rules of `TaxonomyBuilder`. |
| `static TaxonomyBuilder builder()` | `TaxonomyBuilder` | A new, **empty** builder. Call `withStandard()` on it to start from the standard terms. See [Extending the taxonomy](extending.md). |

```java
Taxonomy standard = Taxonomy.standard();          // shared, no extensions
Taxonomy taxonomy = Taxonomy.load();              // standard + ServiceLoader extensions, new instance
Taxonomy custom = Taxonomy.builder()
        .withStandard()
        .category("user.education.lab_notebook", "Lab Notebook", "Laboratory notebooks kept by the subject.")
        .build();
```

`toString()` reports the sizes, for example `Taxonomy[125 data categories, 63 data uses, 21 data subjects]` for the
standard taxonomy.

## Data categories

| Method | Returns | Description |
|---|---|---|
| `List<DataCategory> categories()` | immutable list | All data categories, ordered by key. |
| `Optional<DataCategory> category(String key)` | `Optional` | A category by key, deprecated or not; empty if the taxonomy does not contain it. |
| `DataCategory requireCategory(String key)` | `DataCategory` | A category by key, deprecated or not. Throws `TaxonomyException` (`Unknown data category '<key>'`) if absent. |
| `DataCategory resolveCategory(String key)` | `DataCategory` | The category **to use** for a key: the category itself or, if it is deprecated with a replacement, the current category that replaces it (see below). Throws `TaxonomyException` if the key, or a replacement on the chain, is absent. |
| `Optional<DataCategory> parentOf(DataCategory category)` | `Optional` | The parent; empty for a root. |
| `List<DataCategory> childrenOf(DataCategory category)` | immutable list | The direct children, ordered by key. |
| `List<DataCategory> ancestorsOf(DataCategory category)` | unmodifiable list | The ancestors from the parent up to the root, **nearest first**. Empty for a root. |

### Deprecation and `resolveCategory`

A term is deprecated when its [`TermInfo`](terms.md#terminfo) has a `deprecatedSince` or a `replacedBy`.
`category` and `requireCategory` return deprecated categories unchanged. `resolveCategory` follows `replacedBy`
until it reaches a category without a replacement:

- not deprecated → the category itself;
- deprecated with `replacedBy` → the end of the replacement chain (a replacement may itself be deprecated and
  replaced);
- deprecated without `replacedBy` → the deprecated category itself.

`TaxonomyBuilder.build()` guarantees that every replacement exists in the same hierarchy and that replacements form
no cycle, so the chain always ends, and that the replacement of a deprecated data category does not protect less
(nature, confidentiality, integrity, secret) than the category it replaces. The [`Classifier`](classify/classifier.md) uses `resolveCategory` for deprecated
keys and reports a `DEPRECATED_CATEGORY` warning.

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard()
        .category("user.contact.old_email", "Old email", "",
                c -> c.deprecated("1.0.0", Categories.USER_CONTACT_EMAIL))
        .build();
taxonomy.requireCategory("user.contact.old_email").info().isDeprecated();   // true
taxonomy.resolveCategory("user.contact.old_email").key();                   // "user.contact.email"
```

### Navigation

```java
DataCategory email = taxonomy.requireCategory(Categories.USER_CONTACT_EMAIL);
taxonomy.parentOf(email);        // the category user.contact
taxonomy.ancestorsOf(email);     // the categories user.contact, user
taxonomy.childrenOf(taxonomy.requireCategory(Categories.USER_CONTACT));   // includes user.contact.email
email.isA(Categories.USER_CONTACT);   // true  (Term.isA: same key or a descendant)
email.isA("user.con");                // false (whole segments only)
```

## Data uses

| Method | Returns | Description |
|---|---|---|
| `List<DataUse> dataUses()` | immutable list | All data uses, ordered by key. |
| `Optional<DataUse> dataUse(String key)` | `Optional` | A data use by key; empty if absent. |
| `DataUse requireDataUse(String key)` | `DataUse` | A data use by key. Throws `TaxonomyException` (`Unknown data use '<key>'`) if absent. |
| `List<DataUse> childrenOf(DataUse use)` | immutable list | The direct children, ordered by key. |

## Data subjects

| Method | Returns | Description |
|---|---|---|
| `List<DataSubject> dataSubjects()` | immutable list | All data subjects, ordered by key. |
| `Optional<DataSubject> dataSubject(String key)` | `Optional` | A data subject by key; empty if absent. |
| `DataSubject requireDataSubject(String key)` | `DataSubject` | A data subject by key. Throws `TaxonomyException` (`Unknown data subject '<key>'`) if absent. |

`parentOf`, `ancestorsOf` and `resolveCategory` exist only for data categories; for data uses and data subjects use
`info().parent()` and the lookups above. Data uses and data subjects describe processing activities, not fields, so
no annotation refers to them.

## TaxonomyException

`io.github.devbpm.linneo.TaxonomyException` — `public class TaxonomyException extends RuntimeException`,
`@since 0.1.0`

Thrown when a taxonomy cannot be built or a key is not part of it. The message names the key involved.

| Constructor | Description |
|---|---|
| `TaxonomyException(String message)` | Creates the exception. |
| `TaxonomyException(String message, Throwable cause)` | Creates the exception with its cause. |

Thrown by:

- `Taxonomy.requireCategory`, `requireDataUse`, `requireDataSubject` and `resolveCategory` for an unknown key;
- `Taxonomy.load()` and the `TaxonomyBuilder` methods when a rule is broken (messages listed in
  [Extending the taxonomy](extending.md#errors));
- the standard taxonomy loader when a bundled resource is missing or unreadable.

See also: [Terms](terms.md) · [Protection levels](protection-levels.md) · [Standard taxonomy](standard-taxonomy.md) ·
[Extending the taxonomy](extending.md) · course: [The taxonomy](../course/02-the-taxonomy.md)
