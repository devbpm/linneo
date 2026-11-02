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
  - src/main/java/io/github/devbpm/linneo/Trait.java
-->

# Terms

Every node of a taxonomy is a `Term`. A term's key is a dot-separated path such as `user.contact.email`; its parent
is the key without its last segment (`user.contact`). The three kinds of term are records:

| Type | Hierarchy | Describes | Components |
|---|---|---|---|
| [`DataCategory`](#datacategory) | data categories | what a field holds | `info` + protection facets + attributes + traits |
| [`DataUse`](#datause) | data uses | why data is processed (purpose) | `info` + traits |
| [`DataSubject`](#datasubject) | data subjects | whose data it is | `info` + traits |

Terms are created by [`TaxonomyBuilder`](extending.md); you obtain them from a [`Taxonomy`](taxonomy.md).

## Term

`io.github.devbpm.linneo.Term` — `public sealed interface Term permits DataCategory, DataUse, DataSubject`,
`@since 0.1.0`

| Method | Returns | Description |
|---|---|---|
| `TermInfo info()` | `TermInfo` | Key, parent, name, description, origin and lifecycle. |
| `default String key()` | `String` | Shortcut for `info().key()`. |
| `Set<Trait> traits()` | `Set<Trait>` | The [traits](#trait) of the term, declared on it or inherited from its ancestors, which Linneo's legal rules read. Immutable, possibly empty. |
| `default boolean has(Trait trait)` | `boolean` | `true` if `traits()` contains the trait. |
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
                           Integrity integrity, boolean secret, Map<String, String> attributes,
                           Set<Trait> traits) implements Term
```

| Component | Description |
|---|---|
| `info` | Key, parent, name, description, origin and lifecycle. |
| `nature` | What the data is under data protection law ([`LegalNature`](protection-levels.md#legalnature)). |
| `confidentiality` | The harm disclosure would cause ([`Confidentiality`](protection-levels.md#confidentiality)). |
| `integrity` | The harm an unwanted change would cause ([`Integrity`](protection-levels.md#integrity)). |
| `secret` | Whether the data grants access (passwords, keys, tokens) and must never be read back, exported or published, only verified. |
| `attributes` | Application-defined properties, such as a retention class or the group of a record of processing activities. Inherited by children unless they set their own value. |
| `traits` | The [traits](#trait) the category declares or inherits, which Linneo's legal rules read. |

The facets are the **effective** ones: the values the category declares or, where it declares none, those inherited
from its parent. A `Taxonomy` guarantees they are never weaker than the parent's (see
[Protection levels](protection-levels.md#inheritance-rule)).

The canonical constructor throws `NullPointerException` if `info`, `nature`, `confidentiality` or `integrity` is
`null`; a `null` `attributes` becomes an empty map, and the map is copied into an immutable one; a `null` `traits`
becomes an empty set, and `IllegalArgumentException` reports a trait that does not apply to data categories.

| Method | Returns | Description |
|---|---|---|
| `boolean isPersonal()` | `boolean` | `true` unless `nature` is `LegalNature.NONE`. |
| `Protection protection()` | [`Protection`](protection-levels.md#protection) | The nature, confidentiality, integrity and secret flag of the category as one value, to compare with other protections. |
| `String attribute(String name)` | `String` | The attribute value, or `null` if neither this category nor an ancestor sets it. |

```java
DataCategory health = Taxonomy.standard().requireCategory(Categories.USER_HEALTH_AND_MEDICAL);
health.nature();            // SPECIAL_CATEGORY
health.confidentiality();   // HIGH
health.isPersonal();        // true
health.secret();            // false
health.protection();        // the four facets above as one Protection
```

## DataUse

`io.github.devbpm.linneo.DataUse` — `public record DataUse(TermInfo info, Set<Trait> traits) implements Term`,
`@since 0.1.0`

Why data is processed: the purpose of a processing activity, such as `essential.service.authentication`. A purpose
belongs to a process or a system, not to a field, so no annotation refers to it; use these terms where you describe
your processing activities. The constructor throws `NullPointerException` if `info` is `null`; a `null` `traits`
becomes an empty set, and `IllegalArgumentException` reports a trait that does not apply to data uses.

## DataSubject

`io.github.devbpm.linneo.DataSubject` — `public record DataSubject(TermInfo info, Set<Trait> traits) implements Term`,
`@since 0.1.0`

Whose data is processed: a kind of individual, such as `employee` or `student`. Like a `DataUse`, it describes a
processing activity rather than a field. The constructor throws `NullPointerException` if `info` is `null`; `traits`
is checked as in `DataUse`.

```java
Taxonomy taxonomy = Taxonomy.standard();
DataUse assessment = taxonomy.requireDataUse(DataUses.EDUCATION_ASSESSMENT);
DataSubject student = taxonomy.requireDataSubject(DataSubjects.STUDENT);
```

## Trait

`io.github.devbpm.linneo.Trait` — `public enum Trait`, `@since 0.1.0`

A property of a term that Linneo's legal rules read, so that the rules follow the taxonomy rather than lists of keys:
a term of an application or of an extension weighs as much as a standard one once it declares its traits. A trait is
declared on a term (`TermDraft.traits(Trait...)`) and **inherited** by every term below it. Linneo's own terms declare
theirs in the resource `linneo-traits.tsv` of the
[standard taxonomy](standard-taxonomy.md#traits-of-the-standard-terms), and each shipped
[extension](extensions/README.md) declares those of its terms. Each trait applies to some hierarchies only; declaring it
on another is an error.

| Trait | Applies to | Marks |
|---|---|---|
| `FINANCIAL` | data categories | Bank and card details, income, pay, debts, transactions and financial standing. |
| `BEHAVIOURAL` | data categories | Habits, preferences, activity, location and traffic data. |
| `PRIVATE_LIFE` | data categories | The contents of communications, private documents and household activities. |
| `DIRECT_IDENTIFIER` | data categories | Data that identifies a person on its own: a name, an identity document, a face, a biometric template. |
| `CONTACT_IDENTIFIER` | data categories | Data through which a person can be reached, and so identified: contact details, an account. |
| `INDIRECT_IDENTIFIER` | data categories | Data that singles out a person within a system: an internal or a device identifier. |
| `MONITORING` | data categories, data uses | Data obtained by observing people, or a purpose that observes, tracks or monitors them systematically. |
| `VULNERABLE` | data categories, data subjects | People in a weaker position than the controller, such as children, employees and patients, or data that is about them by its nature. |
| `EVALUATION` | data uses | A purpose that evaluates or scores people, profiles them or predicts their behaviour, performance or situation. |
| `DIRECT_MARKETING` | data uses | Direct marketing, fundraising appeals and the profiling related to them included, to which a data subject may object at any time (GDPR article 21.2). |

| Method | Description |
|---|---|
| `boolean appliesTo(TermChange.Hierarchy hierarchy)` | Whether the trait can be declared on the terms of a hierarchy. |
| `boolean isIdentifier()` | `true` for `DIRECT_IDENTIFIER`, `CONTACT_IDENTIFIER` and `INDIRECT_IDENTIFIER`: the traits that tell how a value identifies a person. |
| `boolean isHighlyPersonal()` | `true` for `FINANCIAL`, `BEHAVIOURAL` and `PRIVATE_LIFE`: data of a highly personal nature in the sense of the impact assessment guidelines. |

Three rules read them:

- [`ImpactScreening`](processing/impact-screening.md) reads `FINANCIAL`, `BEHAVIOURAL` and `PRIVATE_LIFE` as data of a
  highly personal nature, and `EVALUATION`, `MONITORING` and `VULNERABLE` as the criteria of the same names (WP248
  rev.01, III.B.a);
- [`BreachAssessment`](processing/breach-assessment.md) reads `FINANCIAL` and `BEHAVIOURAL` as the data processing
  context and the identifier traits as the ease of identification (ENISA, 2013);
- [`SubjectRights`](processing/subject-rights.md) reads `DIRECT_MARKETING` for the absolute right to object (GDPR
  article 21.2 and 21.3).

Adding a trait to a term makes those rules stricter, so it is a minor change of a taxonomy; removing one may lift an
obligation, so it is a major one ([`TermChange.Aspect.TRAITS`](taxonomy-diff.md#aspect)). `Taxonomy.toTsv()` writes
them as `trait` rows.

```java
Taxonomy taxonomy = Taxonomy.standard();
DataCategory email = taxonomy.requireCategory(Categories.USER_CONTACT_EMAIL);
email.has(Trait.CONTACT_IDENTIFIER);                                      // true, inherited from user.contact
taxonomy.requireDataUse(DataUses.PERSONALIZE_PROFILING).traits();         // [EVALUATION]
taxonomy.requireDataSubject(DataSubjects.EMPLOYEE).has(Trait.VULNERABLE); // true
```

See also: [Taxonomy](taxonomy.md) · [Protection levels](protection-levels.md) · [Key constants](key-constants.md) ·
[Extending the taxonomy](extending.md) · course: [The taxonomy](../course/02-the-taxonomy.md)
