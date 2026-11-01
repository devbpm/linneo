---
title: "Protection levels"
parent: "Reference"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/LegalNature.java
  - src/main/java/io/github/devbpm/linneo/Confidentiality.java
  - src/main/java/io/github/devbpm/linneo/Integrity.java
  - src/main/java/io/github/devbpm/linneo/Identifiability.java
  - src/main/java/io/github/devbpm/linneo/DataCategory.java
  - src/main/java/io/github/devbpm/linneo/Protection.java
-->

# Protection levels

A [`DataCategory`](terms.md#datacategory) carries four protection facets: `nature`, `confidentiality`,
`integrity` and `secret`. [`Protection`](#protection) groups them in one value that can be compared and joined.
`Identifiability` is not a facet of the category: it belongs to the field (see
[`@Category`](annotations/category.md)).

| Facet | Type | Values (least → most protective) | Where |
|---|---|---|---|
| nature | `LegalNature` | `NONE`, `ORDINARY`, `SPECIAL_CATEGORY` / `CRIMINAL_OFFENCE` | category |
| confidentiality | `Confidentiality` | `PUBLIC`, `LOW`, `MEDIUM`, `HIGH` | category; a field may raise it |
| integrity | `Integrity` | `LOW`, `MEDIUM`, `HIGH` | category; a field may raise it |
| secret | `boolean` | `false`, `true` | category |
| identifiability | `Identifiability` | `IDENTIFIED`, `PSEUDONYMIZED`, `ANONYMIZED`, `AGGREGATED` | field only |

All four enums, and the `Protection` record, are `@since 0.1.0`.

## LegalNature

`io.github.devbpm.linneo.LegalNature` — what a data category is under data protection law: Regulation (EU) 2016/679 (GDPR, as corrected in OJ L 127,
23.5.2018) and, for Spain, Ley Orgánica 3/2018 (LOPDGDD, consolidated text of 27 December 2025).

| Constant | Meaning |
|---|---|
| `NONE` | Not personal data: system, organization or reference data. |
| `ORDINARY` | Personal data (GDPR article 4.1). |
| `SPECIAL_CATEGORY` | Special category of personal data (GDPR article 9): racial or ethnic origin, political opinions, religious or philosophical beliefs, trade union membership, genetic data, biometric data processed to uniquely identify a person, health, sex life and sexual orientation. In Spain, consent alone does not lift the prohibition when the main purpose is to identify ideology, trade union membership, religion, sexual orientation, beliefs or racial or ethnic origin (LOPDGDD article 9.1), and processing under GDPR article 9.2 g), h) and i) needs a law (LOPDGDD article 9.2). |
| `CRIMINAL_OFFENCE` | Personal data relating to criminal convictions and offences or related security measures (GDPR article 10, LOPDGDD article 10). Data about administrative infringements and sanctions is not of this nature: it is `ORDINARY` with high confidentiality (LOPDGDD article 27). |

| Method | Description |
|---|---|
| `boolean isPersonal()` | `false` only for `NONE`. |
| `boolean admits(LegalNature child)` | Whether a child category may have nature `child` when its parent has this one. |
| `boolean covers(LegalNature other)` | Whether data of this nature needs at least the protection that data of `other` needs: `true` if this nature is `other` or stricter than it. A partial order, see below. |
| `boolean requiresMaximumProtection()` | `true` for `SPECIAL_CATEGORY` and `CRIMINAL_OFFENCE`: data of that nature must have `HIGH` confidentiality and `HIGH` integrity. |

`admits` is the rule the builder uses for the nature of a child category:

| Parent nature | Admitted child natures |
|---|---|
| `NONE` | `NONE` |
| `ORDINARY` | `ORDINARY`, `SPECIAL_CATEGORY`, `CRIMINAL_OFFENCE` |
| `SPECIAL_CATEGORY` | `SPECIAL_CATEGORY` |
| `CRIMINAL_OFFENCE` | `CRIMINAL_OFFENCE` |

So the nature is fixed by the root of a hierarchy: a `NONE` hierarchy never contains personal data, an `ORDINARY`
category may have special-category or criminal-offence children, and those keep their nature down the tree.

`requiresMaximumProtection()` is the rule behind the levels: `TaxonomyBuilder.build()` throws `TaxonomyException` for
a `SPECIAL_CATEGORY` or `CRIMINAL_OFFENCE` category that does not resolve (declared or inherited) to `HIGH`
confidentiality and `HIGH` integrity.

`covers` orders the natures by how much protection their data needs. [`Protection.covers`](#protection) compares
protections with it:

| Nature | Covers |
|---|---|
| `NONE` | `NONE` |
| `ORDINARY` | `NONE`, `ORDINARY` |
| `SPECIAL_CATEGORY` | `NONE`, `ORDINARY`, `SPECIAL_CATEGORY` |
| `CRIMINAL_OFFENCE` | `NONE`, `ORDINARY`, `CRIMINAL_OFFENCE` |

The order is partial: `SPECIAL_CATEGORY` and `CRIMINAL_OFFENCE` are both stricter than `ORDINARY` but neither covers
the other, since each brings obligations the other does not. `admits` is narrower than `covers`: a hierarchy also
keeps whether it holds personal data, so a `NONE` category never has a personal child.

```java
LegalNature.ORDINARY.admits(LegalNature.SPECIAL_CATEGORY);           // true
LegalNature.SPECIAL_CATEGORY.admits(LegalNature.CRIMINAL_OFFENCE);   // false
LegalNature.NONE.admits(LegalNature.ORDINARY);                       // false

LegalNature.SPECIAL_CATEGORY.covers(LegalNature.ORDINARY);           // true
LegalNature.SPECIAL_CATEGORY.covers(LegalNature.CRIMINAL_OFFENCE);   // false
LegalNature.ORDINARY.covers(LegalNature.NONE);                       // true
```

## Confidentiality

`io.github.devbpm.linneo.Confidentiality` — how much harm disclosing the data would cause. `LOW`, `MEDIUM` and
`HIGH` follow the usual three-level scales of information security frameworks (for example the Spanish ENS or
FIPS 199); `PUBLIC` is for data meant to be public.

| Constant | Meaning |
|---|---|
| `PUBLIC` | Public data: disclosing it causes no harm. |
| `LOW` | Disclosure would cause limited harm. |
| `MEDIUM` | Disclosure would cause serious harm. |
| `HIGH` | Disclosure would cause severe harm. Always the level of special category and criminal offence data. |

Constants are declared in increasing order, so `compareTo` orders them by strictness.

| Method | Description |
|---|---|
| `Confidentiality max(Confidentiality other)` | The stricter of this level and `other`. |

## Integrity

`io.github.devbpm.linneo.Integrity` — how much harm an unauthorized or undetected change to the data would cause.
Integrity is independent of confidentiality: an audit trail or a permission matrix may be harmless to disclose and
critical to keep unaltered.

| Constant | Meaning |
|---|---|
| `LOW` | An unwanted change would cause limited harm. |
| `MEDIUM` | An unwanted change would cause serious harm. |
| `HIGH` | An unwanted change would cause severe harm. |

Constants are declared in increasing order, so `compareTo` orders them by strictness.

| Method | Description |
|---|---|
| `Integrity max(Integrity other)` | The stricter of this level and `other`. |

```java
Confidentiality.LOW.max(Confidentiality.HIGH);   // HIGH
Integrity.HIGH.compareTo(Integrity.MEDIUM) > 0;  // true
```

## Identifiability

`io.github.devbpm.linneo.Identifiability` — how directly a stored value of personal data points to a person. It
belongs to the field, not to the category: the same category can be stored identified in one table and pseudonymized in
another.

| Constant | Meaning | Personal data? |
|---|---|---|
| `IDENTIFIED` | The value identifies the person directly or through other data the controller holds. | yes |
| `PSEUDONYMIZED` | The value can be linked to the person only with additional information kept separately (GDPR article 4.5). | yes (GDPR recital 26) |
| `ANONYMIZED` | The value can no longer be linked to the person. | no, as long as no one can re-identify the person |
| `AGGREGATED` | The value summarizes several people and does not describe any one of them. | no, as long as no one can re-identify the person |

`FieldClassification.isPersonal()` applies the last column: a field is personal data only when its category is
personal and its identifiability is `IDENTIFIED` or `PSEUDONYMIZED` (see [Results](classify/results.md)). A field
with a personal category and no declared identifiability is `IDENTIFIED`; a field with a non-personal category has no
identifiability.

## Inheritance rule

Facets are resolved by `TaxonomyBuilder.build()`, parent first:

- A **root** category must declare `nature`, `confidentiality` and `integrity`.
- A **child** inherits every facet it does not declare. A facet it declares may only be **stricter or equal**:
  - `nature`: must be admitted by the parent's nature (`parent.nature().admits(child)`);
  - `confidentiality`, `integrity`: must not be lower than the parent's (`compareTo`).
  Otherwise `build()` throws `TaxonomyException`.
- A category whose effective nature `requiresMaximumProtection()` (`SPECIAL_CATEGORY` or `CRIMINAL_OFFENCE`) must end
  up with `HIGH` confidentiality and `HIGH` integrity, whether declared or inherited. A child that becomes special
  category under a parent with lower levels must therefore also raise them.
- `secret` is **OR-ed** down the tree: a category is secret if it declares it or its parent is secret. A secret
  category makes all its descendants secret, and nothing can make them non-secret.
- `attributes` are inherited: a child gets its parent's attributes, overridden by those it declares.

The resulting `DataCategory` holds the effective values, so `DataCategory.confidentiality()` already includes what
was inherited. In the standard taxonomy, for example:

| Category | Declares | Effective |
|---|---|---|
| `user` | `ORDINARY`, `LOW`, `MEDIUM`, not secret | `ORDINARY`, `LOW`, `MEDIUM`, not secret |
| `user.authorization` | confidentiality `HIGH`, integrity `HIGH` | `ORDINARY`, `HIGH`, `HIGH`, not secret |
| `user.authorization.biometric` | nature `SPECIAL_CATEGORY`, secret | `SPECIAL_CATEGORY`, `HIGH`, `HIGH`, secret |
| `user.authorization.password` | secret | `ORDINARY`, `HIGH`, `HIGH`, secret |

At field level, [`@Category`](annotations/category.md) applies the same direction: a field may raise
confidentiality or integrity above its category's, never lower them (`LOWERED_LEVEL`).

## Protection

`io.github.devbpm.linneo.Protection` — `public record Protection(...)`, `@since 0.1.0`

The protection some data requires, as one value: the four facets of a category. Linneo uses it wherever protections
are compared: a category and its replacement, the fields of a class
([`FieldClassification.protection()`](classify/results.md)), the personal data of a processing activity
([`CheckedActivity.protection()`](processing/checker.md)), or the same classification in two versions of a model.

```java
public record Protection(Set<LegalNature> natures, Confidentiality confidentiality,
                         Integrity integrity, boolean secret)
```

| Component | Description |
|---|---|
| `natures` | The strictest legal natures of the data, never empty: `LegalNature.NONE` alone for data that is not personal. |
| `confidentiality` | The [confidentiality](#confidentiality) level. |
| `integrity` | The [integrity](#integrity) level. |
| `secret` | Whether the data grants access and must never be read back, exported or published. |

The component is a set because the natures are only partially ordered: the protection of a record that holds health
data and criminal records keeps both natures, and covers neither one alone. The canonical constructor drops every
nature that another one in the set covers (`{ORDINARY, SPECIAL_CATEGORY}` becomes `{SPECIAL_CATEGORY}`) and makes the
set immutable, so equal protections are equal records. It throws `NullPointerException` for a `null` component and
`IllegalArgumentException` if `natures` is empty.

| Member | Description |
|---|---|
| `static Protection of(LegalNature nature, Confidentiality confidentiality, Integrity integrity, boolean secret)` | The protection of data of a single nature. |
| `static final Protection LOWEST` | Not personal, `PUBLIC`, `LOW` integrity, not secret. Every protection covers it, and joining it changes nothing. |
| `boolean covers(Protection other)` | Whether this protection is at least as strict as `other` in every facet: each of the other's natures is covered by one of these ([`LegalNature.covers`](#legalnature)), the levels are not lower, and a secret stays a secret. |
| `Direction comparedTo(Protection before)` | How this protection compares with an earlier one, such as that of the same field in the previous version of a model. See [`Direction`](#direction). |
| `Protection join(Protection other)` | The least protection that covers both: the strictest natures of both, the higher levels, and secret if either is. |
| `static Protection joinAll(Collection<Protection> protections)` | The join of all of them, or `LOWEST` if there are none. |
| `boolean isPersonal()` | `true` if a nature is personal. |
| `boolean requiresMaximumProtection()` | `true` if a nature is `SPECIAL_CATEGORY` or `CRIMINAL_OFFENCE`. |

`covers` is a partial order: when one protection is stricter in a facet and weaker in another, neither covers the
other. `join` is the least upper bound of that order, so the protection of data that holds several kinds of data is
the join of their protections.

### Direction

`Protection.Direction` (`@since 0.1.0`) is the result of `comparedTo`.

| Constant | Meaning |
|---|---|
| `SAME` | The protection is the same. |
| `TIGHTENED` | The new protection covers the old one: stricter in at least one facet and no weaker in any. |
| `LOOSENED` | The old protection covers the new one: weaker in at least one facet and no stricter in any. |
| `MIXED` | Stricter in one facet and weaker in another, or a nature that is not comparable with the old one, such as special category data becoming criminal offence data. It deserves the same review as a loosening. |

| Method | Description |
|---|---|
| `boolean weakens()` | `true` for `LOOSENED` and `MIXED`: some protection is lower, so someone should review the change. |
| `Direction and(Direction other)` | Combines this direction with that of another facet of the same change: `SAME` yields the other one, equal directions stay, and any other pair is `MIXED`. |

```java
Protection contact = Protection.of(LegalNature.ORDINARY, Confidentiality.MEDIUM, Integrity.LOW, false);
Protection health = Protection.of(LegalNature.SPECIAL_CATEGORY, Confidentiality.HIGH, Integrity.HIGH, false);
Protection criminal = Protection.of(LegalNature.CRIMINAL_OFFENCE, Confidentiality.HIGH, Integrity.HIGH, false);

health.covers(contact);                      // true
health.covers(criminal);                     // false: special category and criminal offence are not comparable

Protection both = health.join(criminal);     // natures SPECIAL_CATEGORY and CRIMINAL_OFFENCE, HIGH, HIGH
both.covers(health);                         // true
both.covers(criminal);                       // true

health.comparedTo(contact);                  // TIGHTENED
contact.comparedTo(health);                  // LOOSENED
criminal.comparedTo(health);                 // MIXED, and MIXED.weakens() is true
Protection.joinAll(List.of());               // Protection.LOWEST

DataCategory category = Taxonomy.standard().requireCategory(Categories.USER_HEALTH_AND_MEDICAL);
category.protection().covers(contact);       // true
```

See also: [Terms](terms.md) · [Standard taxonomy](standard-taxonomy.md) · [Extending the taxonomy](extending.md) ·
[`@Category`](annotations/category.md) · course: [The taxonomy](../course/02-the-taxonomy.md)
