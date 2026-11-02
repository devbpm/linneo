---
title: "Extending the taxonomy"
parent: "Reference"
nav_order: 6
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/TaxonomyBuilder.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyExtension.java
  - src/main/java/io/github/devbpm/linneo/CategoryDraft.java
  - src/main/java/io/github/devbpm/linneo/TermDraft.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyDiff.java
  - src/main/java/io/github/devbpm/linneo/Trait.java
-->

# Extending the taxonomy

Applications add terms, and tighten the protection of existing ones, either directly on a `TaxonomyBuilder` or
through a `TaxonomyExtension` that `Taxonomy.load()` discovers with `ServiceLoader`. Linneo also ships optional
extensions for a sector, such as the [university extension](extensions/university.md): a complete example of a
`TaxonomyExtension` with its key constants (see [Extensions](extensions/README.md), which lists the five shipped ones).

## TaxonomyBuilder

`io.github.devbpm.linneo.TaxonomyBuilder` — `public final class`, `@since 0.1.0`

The single place where taxonomies are assembled and validated. The constructor is package-private: obtain a builder
with `Taxonomy.builder()` (empty). A builder is not thread-safe and is meant to be used once.

| Member | Description |
|---|---|
| `static final String DEFAULT_ORIGIN = "application"` | Origin recorded for terms added outside any extension. |
| `TaxonomyBuilder withStandard()` | Adds the [standard taxonomy](standard-taxonomy.md) with its protection policy (origins `fideslang` and `linneo`). Calling it again has no effect. Restores the current origin afterwards. |
| `TaxonomyBuilder withExtensions()` | Applies every `TaxonomyExtension` found by `ServiceLoader` with the thread context class loader, or with `TaxonomyBuilder`'s own loader when there is none. |
| `TaxonomyBuilder withExtensions(ClassLoader classLoader)` | Applies every extension found by `ServiceLoader` in `classLoader`, **sorted by `name()`**, so the result does not depend on class path order. |
| `TaxonomyBuilder extend(TaxonomyExtension extension)` | Applies one extension: calls `contribute(this)` with the extension's `name()` as origin. Applying the same extension **class** again has **no effect**, so an extension can be applied explicitly and also registered as a service. While it contributes, the builder applies the [rules for extensions](#what-an-extension-may-do). Throws `TaxonomyException` if the name is `null`, blank, or one of the reserved `"fideslang"`, `"linneo"` and `"application"` (the origin of terms added outside an extension), or if a **different** class with the same name was already applied (`The extensions <first class> and <second class> are both named '<name>'`); `NullPointerException` if `extension` is `null`. |
| `TaxonomyBuilder origin(String origin)` | Sets the origin recorded for terms added from now on (default `"application"`). `NullPointerException` if `null`; `TaxonomyException` if blank, or if an extension calls it (the terms an extension adds carry its name). |
| `TaxonomyBuilder category(String key, String name, String description)` | Adds a data category that inherits all its protection from its parent. |
| `TaxonomyBuilder category(String key, String name, String description, Consumer<CategoryDraft> customizer)` | Adds a data category and declares its facets, traits, attributes and lifecycle on a [`CategoryDraft`](#categorydraft). Throws `TaxonomyException` if an extension adds a root category. |
| `TaxonomyBuilder refineCategory(String key, Consumer<CategoryDraft> customizer)` | Raises the protection of an existing category, and of the categories below it, or adds traits, attributes or a deprecation to it. Every facet is a **minimum**: a level lower than the declared one changes nothing, and two refinements of one category give the stricter level in either order. Throws `TaxonomyException` if the category does not exist, if a nature cannot be reconciled with the declared one (personal and non-personal, or special category and criminal offence), or if an extension changes the lifecycle of a category it did not add. |
| `TaxonomyBuilder refineDataUse(String key, Consumer<TermDraft> customizer)` | Adds traits or a deprecation to an existing data use. Throws `TaxonomyException` if it does not exist, or if an extension changes the lifecycle of a use it did not add. |
| `TaxonomyBuilder refineDataSubject(String key, Consumer<TermDraft> customizer)` | The same for an existing data subject. |
| `TaxonomyBuilder dataUse(String key, String name, String description)` | Adds a data use. |
| `TaxonomyBuilder dataUse(String key, String name, String description, Consumer<TermDraft> customizer)` | Adds a data use and declares its traits and lifecycle on a [`TermDraft`](#termdraft). |
| `TaxonomyBuilder dataSubject(String key, String name, String description)` | Adds a data subject. |
| `TaxonomyBuilder dataSubject(String key, String name, String description, Consumer<TermDraft> customizer)` | Adds a data subject and declares its traits and lifecycle. |
| `Taxonomy build()` | Checks the hierarchies, resolves inherited facets and returns the immutable `Taxonomy`. |

### Keys

A key is a dot-separated path of lower-case segments:

```text
[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*
```

`category`, `dataUse` and `dataSubject` check the key **immediately**: a `null` key throws `NullPointerException`,
an invalid key or a key already present in the same hierarchy throws `TaxonomyException`. `name` must not be `null`;
a `null` description is stored as `""`. The parent is the key without its last segment and must exist in the same
hierarchy, but terms may be added in any order: parents are checked by `build()`.

### Validation at `build()`

`build()` processes categories parent first (by depth, then key) and enforces:

1. Every non-root term's parent exists in the same hierarchy.
2. A root data category declares `nature`, `confidentiality` and `integrity`.
3. A child data category inherits every facet it does not declare, and every declared facet is a **minimum**: a
   `confidentiality` or `integrity` below the parent's is raised to the parent's, so raising a category raises
   everything below it. The `nature` is the parent's, or narrows ordinary personal data to special category or
   criminal offence data (`LegalNature.admits`); ordinary data declared below a special category parent takes the
   parent's nature. A nature that changes whether the data is personal, or that switches between special category
   and criminal offence data, is an error.
4. A category whose nature is `SPECIAL_CATEGORY` or `CRIMINAL_OFFENCE` (`LegalNature.requiresMaximumProtection()`)
   must resolve, declared or inherited, to `HIGH` confidentiality and `HIGH` integrity. A `DataCategory` record built
   by hand is held to the same rule: its constructor throws `IllegalArgumentException` otherwise.
5. `secret` is OR-ed down the tree; attributes are inherited and overridden by the child's own; traits are inherited
   and added to the child's own.
6. Every `replacedBy` exists in the same hierarchy, and replacements do not form a cycle (for data categories, data
   uses and data subjects).
7. The replacement of a deprecated data category protects at least as much: a nature the deprecated category admits,
   confidentiality and integrity not lower, and `secret` if the deprecated category is secret. A declaration that
   names the deprecated key is classified with its replacement, so a weaker one would silently lower the protection.
8. The replacement of a deprecated term carries every trait of the term it replaces, for data categories, data uses
   and data subjects, since declarations that still name the deprecated key are checked with the replacement: a
   replacement without the trait would lift a rule that read it.

See [Protection levels](protection-levels.md#inheritance-rule).

### What an extension may do

An extension adds data categories **below** existing ones, data uses and data subjects (these two may start a new
root), raises the protection of any category, and adds traits and attributes. While its `contribute` runs, the builder
refuses what would let one extension change what another, or the standard taxonomy, defines:

- adding a **root** data category: a new root is a new domain of data, which needs its own protection policy;
- calling `origin(...)`: the terms an extension adds carry its name;
- calling `since` or `deprecated` on a term it did not add (one whose origin is another): it deprecates only its own
  terms. This is checked when the call is made, against the extension contributing then.

Raising is always allowed, and combining is independent of order: two extensions that raise the same category give the
stricter of their levels, whichever is applied first. An application that uses the builder directly is not bound by
these three rules.

### Errors

All are `TaxonomyException` (messages as produced by the source):

| When | Message |
|---|---|
| `category`/`dataUse`/`dataSubject`, bad key | `Invalid key '<key>': use lower-case segments separated by dots` |
| `category`/`dataUse`/`dataSubject`, duplicate | `Duplicate key '<key>'` |
| `refineCategory`/`refineDataUse`/`refineDataSubject`, unknown key | `Cannot refine unknown data category '<key>'` (or `data use`, `data subject`) |
| `extend`, reserved (`fideslang`, `linneo`, `application`) or blank name | `Invalid extension name '<name>' in <class>` |
| `origin`, blank | `The origin of terms must not be blank` |
| `origin`, called by an extension | `The extension '<name>' cannot change the origin of its terms` |
| `category`, root added by an extension | `The extension '<name>' cannot add the root data category '<key>': an extension adds data categories below existing ones` |
| `attribute`, blank name | `An attribute of '<key>' has a blank name` |
| `nature`, cannot be reconciled with the declared one | `Cannot change the nature of '<key>' from <old> to <new>` |
| `traits`, not applicable to the hierarchy | `The trait <trait> does not apply to the <hierarchy> '<key>'` |
| `deprecated`, blank version | `The deprecation of '<key>' needs the version that deprecated it` |
| `since`/`deprecated`, by an extension on a term it did not add | `The extension '<name>' cannot set the version that introduced '<key>', which comes from '<origin>'` (or `deprecate`) |
| any method of a draft, called after its customizer returned | `The draft of '<key>' can only be changed inside the customizer that received it` |
| `build`, missing parent | `The parent '<parent>' of data category '<key>' does not exist` (or `data use`, `data subject`) |
| `build`, incomplete root | `Root data category '<key>' must declare its nature, confidentiality and integrity` |
| `build`, nature not admitted | `Data category '<key>' declares nature <n>, which its parent '<parent>' (<pn>) does not admit` |
| `build`, special category or criminal offence below `HIGH` | `Data category '<key>' is <nature> data and must have HIGH confidentiality and integrity, not <c> and <i>` |
| `build`, missing replacement | `The replacement '<r>' of data category '<key>' does not exist` |
| `build`, replacement cycle | `The replacements of data category '<key>' form a cycle` |
| `build`, replacement protects less | `The replacement '<r>' of data category '<key>' has weaker protection: <nature>, <c>, <i>[, secret] instead of <nature>, <c>, <i>[, secret]` |
| `build`, replacement lacks a trait | `The replacement '<r>' of data category '<key>' lacks its traits [<trait>, ...]` (or `data use`, `data subject`) |

### Example

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .origin("my-app")
        .category("user.education.lab_notebook", "Lab Notebook", "Laboratory notebooks kept by the subject.",
                c -> c.attribute("retention", "permanent"))
        .refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
        .dataUse("education.scoring", "Applicant scoring", "Ranking of applicants.", u -> u.traits(Trait.EVALUATION))
        .dataSubject("visiting_researcher", "Visiting Researcher", "A researcher from another institution.")
        .build();

taxonomy.requireCategory("user.education.lab_notebook").info().origin();   // "my-app"
taxonomy.requireCategory(Categories.USER_GOVERNMENT_ID_PASSPORT_NUMBER).confidentiality();   // HIGH (inherited)
```

## TaxonomyExtension

`io.github.devbpm.linneo.TaxonomyExtension` — `public interface`, `@since 0.1.0`

Terms an application or a sector adds to the taxonomy, and the stricter protection it requires on existing ones.

| Method | Description |
|---|---|
| `String name()` | Recorded as the `origin` of the terms the extension adds. A short, stable name; never `"fideslang"`, `"linneo"` or `"application"`. Also the sort key when extensions are loaded, and the identity used to apply an extension only once: two different classes cannot share a name. |
| `void contribute(TaxonomyBuilder builder)` | Adds or refines terms. The builder already holds the standard terms (when started with `withStandard()`) and the extensions applied before this one. |

An extension adds data categories below existing ones, data uses and data subjects, raises protection, adds traits and
attributes, and deprecates only the terms it adds: the builder refuses the rest while the extension contributes (see
[What an extension may do](#what-an-extension-may-do)), and the protection rules are enforced when the taxonomy is
built. Pick a name and keys that no other extension you apply uses: `extend` throws a `TaxonomyException` if
a different extension class with the same name has already been applied, and a key added twice fails with
`Duplicate key`. The names `"university"`, `"finance"`, `"supply_chain"`, `"project_operations"` and `"human_resources"`
and the keys of the [shipped extensions](extensions/README.md) are taken by the extensions shipped with Linneo.

```java
package com.example.taxonomy;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyExtension;

public final class CampusTaxonomy implements TaxonomyExtension {

    @Override
    public String name() {
        return "campus";
    }

    @Override
    public void contribute(TaxonomyBuilder builder) {
        builder.category("user.education.lab_notebook", "Lab Notebook", "Laboratory notebooks kept by the subject.",
                        c -> c.attribute("retention", "permanent"))
               .refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
               .dataSubject("visiting_researcher", "Visiting Researcher", "A researcher from another institution.");
    }
}
```

### Registration

`Taxonomy.load()` (and `TaxonomyBuilder.withExtensions()`) applies every extension registered with
`java.util.ServiceLoader`. Register yours in one or both ways:

- **Module path** — in the extension's `module-info.java`:

  ```java
  module com.example.taxonomy {
      requires io.github.devbpm.linneo;
      provides io.github.devbpm.linneo.TaxonomyExtension with com.example.taxonomy.CampusTaxonomy;
  }
  ```

- **Class path** — a file `META-INF/services/io.github.devbpm.linneo.TaxonomyExtension` containing the fully
  qualified class name:

  ```text
  com.example.taxonomy.CampusTaxonomy
  ```

The Linneo module declares `uses io.github.devbpm.linneo.TaxonomyExtension`. As usual for `ServiceLoader`, the
provider class must be public with a public no-argument constructor (or, on the module path, a public static
`provider()` method).

You can also apply an extension explicitly, without `ServiceLoader`:

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new CampusTaxonomy()).build();
```

The extensions shipped with Linneo are not registered; see
[Extensions](extensions/README.md#registering-a-shipped-extension) to apply or register them.

### Keeping your extension compatible

Applications persist the keys of your extension, so it should follow the rules of the standard taxonomy: add terms,
tighten protection and deprecate, but never remove a key or weaken protection. [`TaxonomyDiff`](taxonomy-diff.md)
checks it. Keep the taxonomy of your last release as text, written with `Taxonomy.toTsv()`, and compare it with the
current one in a test:

```java
Taxonomy released = Taxonomy.fromTsv(Files.readString(Path.of("src/test/resources/taxonomy-1.0.0.tsv")));
Taxonomy current = Taxonomy.builder().withStandard().extend(new CampusTaxonomy()).build();
TaxonomyDiff diff = TaxonomyDiff.between(released, current);
diff.requireCompatible();   // TaxonomyException on a removed term or trait, weaker protection or a changed replacement
diff.requiredBump();        // the version step the changes need: NONE, PATCH, MINOR or MAJOR
```

The diff covers the whole taxonomy, so the changes of the standard taxonomy in a newer version of Linneo appear in it
too.

## CategoryDraft

`io.github.devbpm.linneo.CategoryDraft` — `public final class CategoryDraft extends TermDraft`, `@since 0.1.0`

The declared properties of a data category while the builder is open, passed to the customizers of `category` and
`refineCategory`. Every declared facet is a **minimum**, and every facet left undeclared is inherited from the parent at
`build()`. All methods return the same draft for chaining. A draft can only be changed inside the customizer that
received it: keeping it and calling a method later throws `TaxonomyException`.

| Method | Description |
|---|---|
| `CategoryDraft nature(LegalNature nature)` | Declares the legal nature, at least; it must be admitted by the parent's. If a nature is already declared, as when refining, a narrower one replaces it (ordinary personal data to special category or criminal offence data) and one the declared nature already covers changes nothing. Throws `TaxonomyException` if the two cannot be reconciled: personal and non-personal, or special category and criminal offence. |
| `CategoryDraft confidentiality(Confidentiality confidentiality)` | Declares the confidentiality, at least: the stricter of this and an already declared level is kept, whatever the order. A level below the parent's is raised to it at `build()`. |
| `CategoryDraft integrity(Integrity integrity)` | The same for the integrity. |
| `CategoryDraft secret()` | Marks the category, and therefore all its children, as a secret. It cannot be undone. |
| `CategoryDraft attribute(String name, String value)` | Sets an application-defined attribute, inherited by children that do not set their own value. `TaxonomyException` if the name is blank. |
| `CategoryDraft traits(Trait... traits)` | Declares [traits](terms.md#trait) of the category, which its descendants inherit. They only add up. `TaxonomyException` if a trait does not apply to data categories. |
| `CategoryDraft since(String version)` | Overrides `TermDraft.since`, returning `CategoryDraft`. |
| `CategoryDraft deprecated(String sinceVersion, String replacedBy)` | Overrides `TermDraft.deprecated`, returning `CategoryDraft`. |

`nature`, `confidentiality`, `integrity`, `traits` and both arguments of `attribute` throw `NullPointerException` on
`null`. Only what a draft declares is compared when refining; a facet the category only inherits is compared with
the parent's at `build()`.

## TermDraft

`io.github.devbpm.linneo.TermDraft` — `public sealed class TermDraft permits CategoryDraft`, `@since 0.1.0`

The declared traits and lifecycle of a term while the builder is open, passed to the customizers of `dataUse`,
`dataSubject`, `refineDataUse` and `refineDataSubject`. As with a `CategoryDraft`, its methods throw
`TaxonomyException` once the customizer that received the draft has returned.

| Method | Description |
|---|---|
| `TermDraft since(String version)` | Declares the version of its origin that introduced the term, such as `1.2.0`. `TaxonomyException` if an extension sets it on a term it did not add. |
| `TermDraft traits(Trait... traits)` | Declares [traits](terms.md#trait) of the term, which its descendants inherit. Declaring some again, as two extensions may, keeps all of them. `TaxonomyException` if a trait does not apply to the term's hierarchy. |
| `TermDraft deprecated(String sinceVersion, String replacedBy)` | Deprecates the term. `sinceVersion` is required (`NullPointerException` if `null`, `TaxonomyException` if blank, which would leave the term current); `replacedBy` is the key of the replacing term of the same hierarchy, or `null`. Declarations that still use the term keep working and are resolved to the replacement. `TaxonomyException` if an extension deprecates a term it did not add. |

A blank `since` or `replacedBy` is recorded as `null` in `TermInfo`.

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard()
        .dataUse("education.tutoring", "Tutoring", "Personal tutoring of students.", u -> u.since("1.0.0"))
        .category("user.education.old_grade", "Old grade", "Superseded.",
                c -> c.deprecated("1.1.0", Categories.USER_EDUCATION_GRADE))
        .build();
```

See also: [Taxonomy](taxonomy.md) · [Protection levels](protection-levels.md) ·
[Standard taxonomy](standard-taxonomy.md) · [Terms](terms.md) · [Taxonomy diff](taxonomy-diff.md) ·
[Extensions](extensions/README.md) · course: [Extending the taxonomy](../course/06-extending-the-taxonomy.md)
