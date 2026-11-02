---
title: "Taxonomy"
parent: "Reference"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/Taxonomy.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyException.java
  - src/main/java/io/github/devbpm/linneo/Trait.java
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

`toString()` reports the sizes, for example `Taxonomy[126 data categories, 63 data uses, 22 data subjects]` for the
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

`TaxonomyBuilder.build()` guarantees that every replacement exists in the same hierarchy and that replacements form no
cycle, so the chain always ends, and that the replacement of a deprecated data category does not protect less (nature,
confidentiality, integrity, secret) than the category it replaces. The [`Classifier`](classify/classifier.md) uses
`resolveCategory` for deprecated keys and reports a `DEPRECATED_CATEGORY` warning.

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
| `DataUse resolveDataUse(String key)` | `DataUse` | The data use **to use** for a key: the use itself or, if it is deprecated with a replacement, the current use that replaces it. Throws `TaxonomyException` if the key, or a replacement on the chain, is absent. |
| `List<DataUse> childrenOf(DataUse use)` | immutable list | The direct children, ordered by key. |

## Data subjects

| Method | Returns | Description |
|---|---|---|
| `List<DataSubject> dataSubjects()` | immutable list | All data subjects, ordered by key. |
| `Optional<DataSubject> dataSubject(String key)` | `Optional` | A data subject by key; empty if absent. |
| `DataSubject requireDataSubject(String key)` | `DataSubject` | A data subject by key. Throws `TaxonomyException` (`Unknown data subject '<key>'`) if absent. |
| `DataSubject resolveDataSubject(String key)` | `DataSubject` | The data subject **to use** for a key: the subject itself or, if it is deprecated with a replacement, the current subject that replaces it. Throws `TaxonomyException` if the key, or a replacement on the chain, is absent. |

`resolveDataUse` and `resolveDataSubject` follow `replacedBy` as `resolveCategory` does, so a key stored before its
term was deprecated still reaches the current term: [`SubjectRights`](processing/subject-rights.md) resolves the data
subject with the latter. `parentOf` and `ancestorsOf` exist only for data categories; for data uses and data subjects
use `info().parent()` and the lookups above. Data uses and data subjects describe processing activities, not fields,
so no annotation refers to them.

## The taxonomy as data

A taxonomy can be written as text and read back. An application that stores keys keeps the taxonomy it applied, so that
on the next start it can tell what changed with [`TaxonomyDiff`](taxonomy-diff.md), and an auditor can reproduce the
classification of the past with the taxonomy of the past.

| Method | Returns | Description |
|---|---|---|
| `String toTsv()` | `String` | The taxonomy as tab-separated text: every term with its lifecycle and, for data categories, their **effective** protection and attributes, and the [traits](terms.md#trait) of every term, in key order. Deterministic: the same taxonomy always gives the same text. |
| `static Taxonomy fromTsv(String text)` | `Taxonomy` | Reads a text written by `toTsv()`. The terms go through a `TaxonomyBuilder`, so the text must follow the same rules as any other taxonomy. Throws `TaxonomyException` if the text is not a taxonomy of a known format version, a row is malformed, or the terms break a rule of the builder. |
| `String fingerprint()` | `String` | The SHA-256 of `toTsv()` (UTF-8), as 64 lower-case hexadecimal digits. Equal taxonomies have the same fingerprint; computed once and kept. |
| `boolean equals(Object)`, `int hashCode()` | | Two taxonomies are equal when they have the same data categories, data uses and data subjects: same keys, names, descriptions, origins, lifecycles, protection and attributes. |

The fingerprint says that something changed, not what: any change of text, even a description, changes it. Use
[`TaxonomyDiff`](taxonomy-diff.md) to find out what.

### Text format

The first line is `#linneo-taxonomy`, a tab and the format version, `1`; `fromTsv` refuses any other first line. Blank
lines and lines starting with `#` after it are comments; `toTsv` writes four that name the columns. Every other line
is a row of cells separated by tabs, and the first cell says which row it is:

| Row | Cells |
|---|---|
| `category` | `category`, key, name, description, origin, version that introduced it, version that deprecated it, replacement, nature, confidentiality, integrity, secret (`true` or `false`): 12 cells |
| `use`, `subject` | the kind, key, name, description, origin, version that introduced it, version that deprecated it, replacement: 8 cells |
| `attribute` | `attribute`, category key, name, value: 4 cells. One row for each attribute of a category, those inherited from its ancestors included |
| `trait` | `trait`, the kind of term (`category`, `use` or `subject`), key, trait: 4 cells. One row for each trait of a term, those inherited from its ancestors included |

Rows are written in this order: data categories, data uses and data subjects, each by key, then the attributes by
category key and name, then the traits of the categories, the uses and the subjects. An empty cell is an absent value.
A backslash, tab, line feed and carriage return inside a cell are written `\\`, `\t`, `\n` and `\r`, so a row is always
one line; a lone UTF-16 surrogate, which UTF-8 cannot encode, is written as a backslash, `u` and its four hexadecimal
digits, so that two different taxonomies never have the same bytes or fingerprint. The nature, confidentiality and
integrity are the names of the constants of [`LegalNature`, `Confidentiality` and `Integrity`](protection-levels.md),
and the trait is the name of a [`Trait`](terms.md#trait). `fromTsv` reads the rows in any order: parents are checked
when the taxonomy is built, and the attribute and trait rows are applied after all the terms.

```java
Taxonomy taxonomy = Taxonomy.builder()
        .origin("my-app")
        .category("data", "Data", "Any data.",
                c -> c.nature(LegalNature.ORDINARY).confidentiality(Confidentiality.LOW).integrity(Integrity.LOW))
        .category("data.note", "Note", "A free text note.", c -> c.attribute("retention", "1y"))
        .build();

String text = taxonomy.toTsv();
Taxonomy read = Taxonomy.fromTsv(text);
read.equals(taxonomy);                       // true
read.fingerprint().equals(taxonomy.fingerprint());   // true
```

`text` is, with `→` standing for a tab:

```text
#linneo-taxonomy→1
# category key name description origin since deprecated_since replaced_by nature confidentiality integrity secret
# use|subject key name description origin since deprecated_since replaced_by
# attribute category_key name value
# trait category|use|subject key trait
category→data→Data→Any data.→my-app→→→→ORDINARY→LOW→LOW→false
category→data.note→Note→A free text note.→my-app→→→→ORDINARY→LOW→LOW→false
attribute→data.note→retention→1y
```

## TaxonomyException

`io.github.devbpm.linneo.TaxonomyException` — `public class TaxonomyException extends RuntimeException`,
`@since 0.1.0`

Thrown when a taxonomy cannot be built or a key is not part of it. The message names the key involved.

| Constructor | Description |
|---|---|
| `TaxonomyException(String message)` | Creates the exception. |
| `TaxonomyException(String message, Throwable cause)` | Creates the exception with its cause. |

Thrown by:

- `Taxonomy.requireCategory`, `requireDataUse`, `requireDataSubject` and the three `resolve` methods for an unknown
  key;
- `Taxonomy.load()` and the `TaxonomyBuilder` methods when a rule is broken (messages listed in
  [Extending the taxonomy](extending.md#errors));
- `Taxonomy.fromTsv` when the text is malformed or its terms break a rule of the builder;
- [`TaxonomyDiff.requireCompatible()`](taxonomy-diff.md#taxonomydiff) when a change is incompatible;
- the standard taxonomy loader when a bundled resource is missing or unreadable.

See also: [Terms](terms.md) · [Protection levels](protection-levels.md) · [Standard taxonomy](standard-taxonomy.md) ·
[Extending the taxonomy](extending.md) · [Taxonomy diff](taxonomy-diff.md) · course:
[The taxonomy](../course/02-the-taxonomy.md)
