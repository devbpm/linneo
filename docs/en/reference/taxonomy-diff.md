---
title: "Taxonomy diff"
parent: "Reference"
nav_order: 6.5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/TaxonomyDiff.java
  - src/main/java/io/github/devbpm/linneo/TermChange.java
  - pom.xml
-->

# Taxonomy diff

`io.github.devbpm.linneo.TaxonomyDiff` — `public final class`, `@since 0.1.0`

`io.github.devbpm.linneo.TermChange` — `public record`, `@since 0.1.0`

The differences between two versions of a taxonomy, term by term. Applications use them when the taxonomy they run with
is not the one they applied last: they keep the applied one with [`Taxonomy.toTsv()`](taxonomy.md#the-taxonomy-as-data),
read it back with `Taxonomy.fromTsv(String)` and compare. Each `TermChange` says what changed in a term and, for a data
category, how its protection moved. With it an application can:

- **migrate** the keys it stores, such as a deprecated key to its replacement;
- **record one audit entry per changed term** instead of one per field;
- **refuse to start** on a change it cannot accept, with `requireCompatible()`.

The same comparison grades the changes of Linneo's own taxonomy under its [versioning rules](#versioning-rules).
Instances of both types are immutable.

## TaxonomyDiff

| Member | Returns | Description |
|---|---|---|
| `static TaxonomyDiff between(Taxonomy before, Taxonomy after)` | `TaxonomyDiff` | Compares two versions: `before` is the earlier one, such as the one an application applied last, and `after` the later one, such as the one it runs with now. Terms are matched by key within each hierarchy. `NullPointerException` if either is `null`. |
| `List<TermChange> changes()` | immutable list | The changes: data categories first, then data uses and data subjects, each by key. A term that did not change is not listed. |
| `boolean isEmpty()` | `boolean` | `true` if the two versions have the same terms. |
| `TermChange change(TermChange.Hierarchy hierarchy, String key)` | `TermChange` | The change of one term, or `null` if the term did not change (or is in neither version). |
| `List<TermChange> breakingChanges()` | immutable list | The changes that are [`isBreaking()`](#termchange): incompatible for applications that store or publish keys. |
| `TaxonomyDiff.Bump requiredBump()` | `Bump` | The version change the differences require: the strictest `bump()` of the changes, `Bump.NONE` when there is none. |
| `TaxonomyDiff requireCompatible()` | `TaxonomyDiff` | Returns this diff if it has no breaking change. Otherwise throws `TaxonomyException` listing every one: `<n> incompatible taxonomy change(s):` followed by a line ` - <change>` for each. |

`toString()` reports the size and the bump, for example `TaxonomyDiff[3 change(s), MINOR]`.

### Bump

`TaxonomyDiff.Bump` — `public enum`, `@since 0.1.0`. A version change under semantic versioning, as Linneo applies it
to taxonomies. The constants are declared from the smallest to the largest, so `compareTo` orders them.

| Constant | Meaning |
|---|---|
| `NONE` | No change. |
| `PATCH` | Descriptive changes only: names, descriptions, attributes, origins, lifecycle versions. |
| `MINOR` | Compatible changes: added terms, tighter protection, deprecations, a replacement for a deprecated term that had none. |
| `MAJOR` | Incompatible changes: removed terms, weaker or mixed protection, a deprecated term that now resolves to another replacement. |

## TermChange

How one term differs between two versions. The record components are:

| Component | Description |
|---|---|
| `Hierarchy hierarchy()` | The hierarchy of the term. |
| `String key()` | The key of the term. |
| `Term before()` | The term in the earlier version, or `null` if it was added. |
| `Term after()` | The term in the later version, or `null` if it was removed. |
| `Set<Aspect> aspects()` | What changed, never empty. Immutable. |
| `Protection.Direction direction()` | How the protection of a data category changed: [`Direction`](protection-levels.md#direction) of the later protection compared with the earlier one. `SAME` for data uses, data subjects and for added or removed terms. |

The canonical constructor is public and checks its arguments: `NullPointerException` for a `null` hierarchy, key or
direction; `IllegalArgumentException` for empty aspects, or if both `before` and `after` are `null`. It copies the
aspects into an immutable set.

| Method | Description |
|---|---|
| `TaxonomyDiff.Bump bump()` | The version change this change requires: the strictest bump of its aspects (table below). |
| `boolean isBreaking()` | Whether the change is incompatible for applications that store or publish the key: `bump()` is `MAJOR`. |

`toString()` names the hierarchy, the key, the aspects and, when it is not `SAME`, the direction, for example
`DATA_CATEGORY 'data.a': [PROTECTION] LOOSENED`.

### Hierarchy

`TermChange.Hierarchy` — `public enum`, `@since 0.1.0`: `DATA_CATEGORY` ([`DataCategory`](terms.md#datacategory)),
`DATA_USE` ([`DataUse`](terms.md#datause)) and `DATA_SUBJECT` ([`DataSubject`](terms.md#datasubject)).

### Aspect

`TermChange.Aspect` — `public enum`, `@since 0.1.0`. What changed in a term. A change has one or more aspects, and its
`bump()` is the strictest of theirs.

| Aspect | Set when | Bump |
|---|---|---|
| `ADDED` | The term exists only in the later version. | `MINOR` |
| `REMOVED` | The term exists only in the earlier version. Applications that stored its key can no longer resolve it. | `MAJOR` |
| `PROTECTION` | The protection of a data category differs: its nature, confidentiality, integrity or secret flag, as resolved at build time. `direction()` says how. | `MAJOR` if the direction is `LOOSENED` or `MIXED` (`Direction.weakens()`), `MINOR` if it is `TIGHTENED` |
| `DEPRECATION` | The term was deprecated, or is no longer deprecated. | `MINOR` |
| `REPLACEMENT` | The key that replaces a deprecated term was set or changed, so the term resolves to another one. | `MAJOR` if the term already had a replacement, `MINOR` if it had none |
| `NAME` | The name differs. | `PATCH` |
| `DESCRIPTION` | The description differs. It is only text, but a description that says something else may hide a change of meaning, which is never allowed: review it. | `PATCH` |
| `ATTRIBUTES` | The attributes of a data category differ. | `PATCH` |
| `ORIGIN` | The origin differs, for example when a term moves from an extension to the standard taxonomy. | `PATCH` |
| `VERSIONS` | The version that introduced the term differs, or the version that deprecated it differs without a change of deprecation status. | `PATCH` |

A mixed change deserves the review of a loosening: stricter in one facet and weaker in another, or a nature that is not
comparable with the old one, such as special category data becoming criminal offence data.

## Using it

Keep the taxonomy the application applied, and compare it on the next start:

```java
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyDiff;
import io.github.devbpm.linneo.TermChange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

static Taxonomy start(Path appliedFile) throws IOException {
    Taxonomy taxonomy = Taxonomy.load();
    if (Files.exists(appliedFile)) {
        Taxonomy applied = Taxonomy.fromTsv(Files.readString(appliedFile));
        TaxonomyDiff diff = TaxonomyDiff.between(applied, taxonomy);
        diff.requireCompatible();                  // TaxonomyException: removed term, weaker protection, ...
        for (TermChange change : diff.changes()) {
            System.out.println("Taxonomy change: " + change);   // one audit entry per changed term
        }
    }
    Files.writeString(appliedFile, taxonomy.toTsv());   // the taxonomy applied from now on
    return taxonomy;
}
```

A key that was deprecated since the last start is a `DEPRECATION` change of its category. Stored values that still
hold it can be migrated to the category it resolves to:

```java
import io.github.devbpm.linneo.TermChange.Aspect;
import io.github.devbpm.linneo.TermChange.Hierarchy;

for (TermChange change : diff.changes()) {
    if (change.hierarchy() == Hierarchy.DATA_CATEGORY && change.aspects().contains(Aspect.DEPRECATION)
            && change.after().info().isDeprecated()) {
        String current = taxonomy.resolveCategory(change.key()).key();
        if (!current.equals(change.key())) {
            // update the stored rows that hold change.key() to current
        }
    }
}
```

`change.after()` is never `null` here: a `DEPRECATION` change exists only for a term that is in both versions. Because
a replacement never protects less than the deprecated category it replaces (see
[Extending the taxonomy](extending.md#validation-at-build)), migrating a key never lowers the protection of the data.

A taxonomy you extend yourself can use the same check to stay compatible; see
[Extending the taxonomy](extending.md#keeping-your-extension-compatible).

## Versioning rules

Applications persist and publish the keys of the taxonomy, so Linneo versions it under semantic versioning with these
rules, which `requiredBump()` applies:

- **Major**: removing a term, weakening the protection of a category in any facet, or changing what a deprecated key
  resolves to.
- **Minor**: adding a term, tightening protection, deprecating a term, or giving a deprecated term its first
  replacement.
- **Patch**: descriptive changes (names, descriptions, attributes, origins, lifecycle versions).

Two rules are not checked by a program: never change the meaning of a key, and never reuse a removed key. A
`DESCRIPTION` change is reported so that someone reviews that the meaning stays.

Linneo's own build checks these rules. Each release keeps a copy of the taxonomy it ships, the standard taxonomy with
every shipped extension, in `src/test/resources/io/github/devbpm/linneo/released/<version>.tsv`, written with
`toTsv()`. `ReleasedTaxonomyTest`, which receives the project version from Maven as the system property
`linneo.version`, compares the current taxonomy with the copy of the last release and fails when `requiredBump()` is
larger than the version step allows: the first of major, minor and patch that differs between the released and the
current version. For example, a removed term fails the build in a minor or patch release. While no copy exists, the
test is skipped.

See also: [Taxonomy](taxonomy.md#the-taxonomy-as-data) · [Protection levels](protection-levels.md#direction) ·
[Extending the taxonomy](extending.md) · [Standard taxonomy](standard-taxonomy.md)
