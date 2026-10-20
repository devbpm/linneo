---
title: "Terms"
parent: "Reference"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/Term.java
  - src/main/java/io/github/devbpm/linneo/TermInfo.java
  - src/main/java/io/github/devbpm/linneo/DataCategory.java
  - src/main/java/io/github/devbpm/linneo/DataUse.java
  - src/main/java/io/github/devbpm/linneo/DataSubject.java
-->

# Terms

Every node of a taxonomy is a `Term`. A term's key is a dot-separated path such as `user.contact.email`; its parent
is the key without its last segment (`user.contact`). The three kinds of term are records:

| Type | Hierarchy | Describes | Components |
|---|---|---|---|
| [`DataCategory`](#datacategory) | data categories | what a field holds | `info` + protection facets + attributes |
| [`DataUse`](#datause) | data uses | why data is processed (purpose) | `info` only |
| [`DataSubject`](#datasubject) | data subjects | whose data it is | `info` only |

Terms are created by [`TaxonomyBuilder`](extending.md); you obtain them from a [`Taxonomy`](taxonomy.md).

## Term

`io.github.devbpm.linneo.Term` — `public sealed interface Term permits DataCategory, DataUse, DataSubject`,
`@since 0.1.0`

| Method | Returns | Description |
|---|---|---|
| `TermInfo info()` | `TermInfo` | Key, parent, name, description, origin and lifecycle. |
| `default String key()` | `String` | Shortcut for `info().key()`. |
| `default String name()` | `String` | Shortcut for `info().name()`. |
| `default boolean isA(String ancestorKey)` | `boolean` | `true` if the key equals `ancestorKey` or starts with `ancestorKey + "."`. Compares whole segments: `user.contact.email` is a `user.contact` but not a `user.con`. Does not consult the taxonomy. |

```java
DataCategory email = Taxonomy.standard().requireCategory(Categories.USER_CONTACT_EMAIL);
email.isA(Categories.USER);            // true
email.isA(Categories.USER_CONTACT);    // true
email.isA(Categories.SYSTEM);          // false
```

## TermInfo

`io.github.devbpm.linneo.TermInfo` — `public record`, `@since 0.1.0`

What every term has, whatever its hierarchy.

```java
public record TermInfo(String key, String parentKey, String name, String description, String origin,
                       String since, String deprecatedSince, String replacedBy)
```

| Component | Nullable | Description |
|---|---|---|
| `key` | no | Unique key in dot notation, such as `user.contact.email`. |
| `parentKey` | yes | Key of the parent term; `null` for a root. |
| `name` | no | Short human-readable name. |
| `description` | no | What the term covers. A `null` argument is stored as `""`. |
| `origin` | no | Who defined the term: `"fideslang"`, `"linneo"`, the name of a `TaxonomyExtension`, or the origin set on the builder (default `"application"`; see [`TaxonomyBuilder.origin`](extending.md#taxonomybuilder)). |
| `since` | yes | Version of its origin that introduced the term; `null` if unknown. |
| `deprecatedSince` | yes | Version of its origin that deprecated the term; `null` if current. |
| `replacedBy` | yes | Key of the term that replaces a deprecated one; `null` if none. |

The canonical constructor throws `NullPointerException` if `key`, `name` or `origin` is `null`.

| Member | Description |
|---|---|
| `static final String FIDESLANG = "fideslang"` | Origin of the terms taken from the fideslang default taxonomy. |
| `static final String LINNEO = "linneo"` | Origin of the terms added by Linneo. |
| `Optional<String> parent()` | The parent key; empty for a root. |
| `boolean isDeprecated()` | `true` if `deprecatedSince` **or** `replacedBy` is set. Deprecated terms should no longer be used in new declarations. |

```java
TermInfo profiling = Taxonomy.standard().requireDataUse(DataUses.PERSONALIZE_PROFILING).info();
profiling.origin();            // "fideslang"
profiling.since();             // "2.0.0"
profiling.isDeprecated();      // true
profiling.deprecatedSince();   // "2.1.1"
profiling.replacedBy();        // "personalize.content.profiling"
```

## DataCategory

`io.github.devbpm.linneo.DataCategory` — `public record DataCategory(...) implements Term`, `@since 0.1.0`

What a field holds, with the protection that kind of data requires.

```java
public record DataCategory(TermInfo info, LegalNature nature, Confidentiality confidentiality,
                           Integrity integrity, boolean secret, Map<String, String> attributes) implements Term
```

| Component | Description |
|---|---|
| `info` | Key, parent, name, description, origin and lifecycle. |
| `nature` | What the data is under data protection law ([`LegalNature`](protection-levels.md#legalnature)). |
| `confidentiality` | The harm disclosure would cause ([`Confidentiality`](protection-levels.md#confidentiality)). |
| `integrity` | The harm an unwanted change would cause ([`Integrity`](protection-levels.md#integrity)). |
| `secret` | Whether the data grants access (passwords, keys, tokens) and must never be read back, exported or published, only verified. |
| `attributes` | Application-defined properties, such as a retention class or the group of a record of processing activities. Inherited by children unless they set their own value. |

The facets are the **effective** ones: the values the category declares or, where it declares none, those inherited
from its parent. A `Taxonomy` guarantees they are never weaker than the parent's (see
[Protection levels](protection-levels.md#inheritance-rule)).

The canonical constructor throws `NullPointerException` if `info`, `nature`, `confidentiality` or `integrity` is
`null`; a `null` `attributes` becomes an empty map, and the map is copied into an immutable one.

| Method | Returns | Description |
|---|---|---|
| `boolean isPersonal()` | `boolean` | `true` unless `nature` is `LegalNature.NONE`. |
| `String attribute(String name)` | `String` | The attribute value, or `null` if neither this category nor an ancestor sets it. |

```java
DataCategory health = Taxonomy.standard().requireCategory(Categories.USER_HEALTH_AND_MEDICAL);
health.nature();            // SPECIAL_CATEGORY
health.confidentiality();   // HIGH
health.isPersonal();        // true
health.secret();            // false
```

## DataUse

`io.github.devbpm.linneo.DataUse` — `public record DataUse(TermInfo info) implements Term`, `@since 0.1.0`

Why data is processed: the purpose of a processing activity, such as `essential.service.authentication`. A purpose
belongs to a process or a system, not to a field, so no annotation refers to it; use these terms where you describe
your processing activities. The constructor throws `NullPointerException` if `info` is `null`.

## DataSubject

`io.github.devbpm.linneo.DataSubject` — `public record DataSubject(TermInfo info) implements Term`, `@since 0.1.0`

Whose data is processed: a kind of individual, such as `employee` or `student`. Like a `DataUse`, it describes a
processing activity rather than a field. The constructor throws `NullPointerException` if `info` is `null`.

```java
Taxonomy taxonomy = Taxonomy.standard();
DataUse assessment = taxonomy.requireDataUse(DataUses.EDUCATION_ASSESSMENT);
DataSubject student = taxonomy.requireDataSubject(DataSubjects.STUDENT);
```

See also: [Taxonomy](taxonomy.md) · [Protection levels](protection-levels.md) · [Key constants](key-constants.md) ·
[Extending the taxonomy](extending.md) · course: [The taxonomy](../course/02-the-taxonomy.md)
