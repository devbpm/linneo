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
-->

# Extending the taxonomy

Applications add terms, and tighten the protection of existing ones, either directly on a `TaxonomyBuilder` or
through a `TaxonomyExtension` that `Taxonomy.load()` discovers with `ServiceLoader`. Linneo also ships optional
extensions for a sector, such as the [university extension](extensions/university.md): a complete example of a
`TaxonomyExtension` with its key constants (see [Extensions](extensions/README.md)).

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
| `TaxonomyBuilder extend(TaxonomyExtension extension)` | Applies one extension: calls `contribute(this)` with the extension's `name()` as origin. Applying the same extension **class** again has **no effect**, so an extension can be applied explicitly and also registered as a service. Throws `TaxonomyException` if the name is `null`, blank, `"fideslang"` or `"linneo"`, or if a **different** class with the same name was already applied (`The extensions <first class> and <second class> are both named '<name>'`); `NullPointerException` if `extension` is `null`. |
| `TaxonomyBuilder origin(String origin)` | Sets the origin recorded for terms added from now on (default `"application"`). `NullPointerException` if `null`. |
| `TaxonomyBuilder category(String key, String name, String description)` | Adds a data category that inherits all its protection from its parent. |
| `TaxonomyBuilder category(String key, String name, String description, Consumer<CategoryDraft> customizer)` | Adds a data category and declares its facets, attributes and lifecycle on a [`CategoryDraft`](#categorydraft). |
| `TaxonomyBuilder refineCategory(String key, Consumer<CategoryDraft> customizer)` | Makes an existing category's protection stricter, or adds attributes or a deprecation to it. A facet the category already **declares** can only be raised (checked immediately). Throws `TaxonomyException` if the category does not exist. |
| `TaxonomyBuilder dataUse(String key, String name, String description)` | Adds a data use. |
| `TaxonomyBuilder dataUse(String key, String name, String description, Consumer<TermDraft> customizer)` | Adds a data use and declares its lifecycle on a [`TermDraft`](#termdraft). |
| `TaxonomyBuilder dataSubject(String key, String name, String description)` | Adds a data subject. |
| `TaxonomyBuilder dataSubject(String key, String name, String description, Consumer<TermDraft> customizer)` | Adds a data subject and declares its lifecycle. |
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
3. A child data category inherits every facet it does not declare; a declared facet may not be weaker than the
   parent's: `nature` must be admitted by the parent's (`LegalNature.admits`), `confidentiality` and `integrity`
   must not be lower.
4. A category whose nature is `SPECIAL_CATEGORY` or `CRIMINAL_OFFENCE` (`LegalNature.requiresMaximumProtection()`)
   must resolve, declared or inherited, to `HIGH` confidentiality and `HIGH` integrity.
5. `secret` is OR-ed down the tree; attributes are inherited and overridden by the child's own.
6. Every `replacedBy` exists in the same hierarchy, and replacements do not form a cycle (for data categories, data
   uses and data subjects).

See [Protection levels](protection-levels.md#inheritance-rule).

### Errors

All are `TaxonomyException` (messages as produced by the source):

| When | Message |
|---|---|
| `category`/`dataUse`/`dataSubject`, bad key | `Invalid key '<key>': use lower-case segments separated by dots` |
| `category`/`dataUse`/`dataSubject`, duplicate | `Duplicate key '<key>'` |
| `refineCategory`, unknown key | `Cannot refine unknown data category '<key>'` |
| `extend`, reserved or blank name | `Invalid extension name '<name>' in <class>` |
| `refineCategory`, nature not admitted by the declared one | `Cannot change the nature of '<key>' from <old> to <new>` |
| `refineCategory`, lower confidentiality | `Cannot lower the confidentiality of '<key>' from <old> to <new>` |
| `refineCategory`, lower integrity | `Cannot lower the integrity of '<key>' from <old> to <new>` |
| `build`, missing parent | `The parent '<parent>' of data category '<key>' does not exist` (or `data use`, `data subject`) |
| `build`, incomplete root | `Root data category '<key>' must declare its nature, confidentiality and integrity` |
| `build`, nature not admitted | `Data category '<key>' declares nature <n>, which its parent '<parent>' (<pn>) does not admit` |
| `build`, lower confidentiality | `Data category '<key>' declares confidentiality <c>, lower than its parent's <pc>` |
| `build`, lower integrity | `Data category '<key>' declares integrity <i>, lower than its parent's <pi>` |
| `build`, special category or criminal offence below `HIGH` | `Data category '<key>' is <nature> data and must have HIGH confidentiality and integrity, not <c> and <i>` |
| `build`, missing replacement | `The replacement '<r>' of data category '<key>' does not exist` |
| `build`, replacement cycle | `The replacements of data category '<key>' form a cycle` |

### Example

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .origin("my-app")
        .category("user.education.lab_notebook", "Lab Notebook", "Laboratory notebooks kept by the subject.",
                c -> c.attribute("retention", "permanent"))
        .refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
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
| `String name()` | Recorded as the `origin` of the terms the extension adds. A short, stable name; never `"fideslang"` or `"linneo"`. Also the sort key when extensions are loaded, and the identity used to apply an extension only once: two different classes cannot share a name. |
| `void contribute(TaxonomyBuilder builder)` | Adds or refines terms. The builder already holds the standard terms (when started with `withStandard()`) and the extensions applied before this one. |

An extension may only add children to existing terms and make protection stricter; the rules are enforced when the
taxonomy is built. Pick a name and keys that no other extension you apply uses: `extend` throws a `TaxonomyException`
if a different extension class with the same name has already been applied, and a key added twice fails with
`Duplicate key`. The name `"university"` and the
keys of the [university extension](extensions/university.md) are taken by the extension shipped with Linneo.

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

## CategoryDraft

`io.github.devbpm.linneo.CategoryDraft` — `public final class CategoryDraft extends TermDraft`, `@since 0.1.0`

The declared properties of a data category while the builder is open, passed to the customizers of `category` and
`refineCategory`. Every facet left undeclared is inherited from the parent at `build()`. All methods return the same
draft for chaining.

| Method | Description |
|---|---|
| `CategoryDraft nature(LegalNature nature)` | Declares the legal nature; must be admitted by the parent's. When refining, throws `TaxonomyException` if the category already declares a nature that does not admit the new one. |
| `CategoryDraft confidentiality(Confidentiality confidentiality)` | Declares the confidentiality; never lower than the parent's. When refining, throws `TaxonomyException` if lower than the level already declared. |
| `CategoryDraft integrity(Integrity integrity)` | Declares the integrity; never lower than the parent's. When refining, throws `TaxonomyException` if lower than the level already declared. |
| `CategoryDraft secret()` | Marks the category, and therefore all its children, as a secret. It cannot be undone. |
| `CategoryDraft attribute(String name, String value)` | Sets an application-defined attribute, inherited by children that do not set their own value. |
| `CategoryDraft since(String version)` | Overrides `TermDraft.since`, returning `CategoryDraft`. |
| `CategoryDraft deprecated(String sinceVersion, String replacedBy)` | Overrides `TermDraft.deprecated`, returning `CategoryDraft`. |

`nature`, `confidentiality`, `integrity` and both arguments of `attribute` throw `NullPointerException` on `null`.
The immediate checks while refining compare with what the category **declares**; a facet it only inherits is
compared with the parent's at `build()`.

## TermDraft

`io.github.devbpm.linneo.TermDraft` — `public sealed class TermDraft permits CategoryDraft`, `@since 0.1.0`

The declared lifecycle of a term while the builder is open, passed to the customizers of `dataUse` and
`dataSubject`.

| Method | Description |
|---|---|
| `TermDraft since(String version)` | Declares the version of its origin that introduced the term, such as `1.2.0`. |
| `TermDraft deprecated(String sinceVersion, String replacedBy)` | Deprecates the term. `sinceVersion` is required (`NullPointerException` if `null`); `replacedBy` is the key of the replacing term of the same hierarchy, or `null`. Declarations that still use the term keep working and are resolved to the replacement. |

Blank `since`, `sinceVersion` or `replacedBy` values are recorded as `null` in `TermInfo`.

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard()
        .dataUse("education.tutoring", "Tutoring", "Personal tutoring of students.", u -> u.since("1.0.0"))
        .category("user.education.old_grade", "Old grade", "Superseded.",
                c -> c.deprecated("1.1.0", Categories.USER_EDUCATION_GRADE))
        .build();
```

See also: [Taxonomy](taxonomy.md) · [Protection levels](protection-levels.md) · [Standard taxonomy](standard-taxonomy.md) ·
[Terms](terms.md) · [Extensions](extensions/README.md) · course: [Extending the taxonomy](../course/06-extending-the-taxonomy.md)
