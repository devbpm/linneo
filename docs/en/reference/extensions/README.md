---
title: "Extensions"
parent: "Reference"
nav_order: 7
permalink: "/en/reference/extensions/"
---
<!--
doc-sources:
  - src/main/java/module-info.java
  - src/main/java/io/github/devbpm/linneo/extension/university/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyBuilder.java
-->

# Extensions

Linneo ships, besides the [standard taxonomy](../standard-taxonomy.md), **optional extensions** for a sector: terms
that most applications do not need, packaged as a [`TaxonomyExtension`](../extending.md#taxonomyextension) in the
Linneo JAR, each in its own exported package `io.github.devbpm.linneo.extension.<sector>`, with compile-time
constants for its keys.

| Extension | Class | `name()` | Adds | Page |
|---|---|---|---|---|
| Higher education | `io.github.devbpm.linneo.extension.university.UniversityTaxonomy` | `"university"` | 33 data categories, 27 data uses, 13 data subjects | [University](university.md) |

## Not applied by default

A shipped extension is never part of the taxonomy unless the application asks for it:

- `Taxonomy.standard()` never includes extensions.
- `Taxonomy.load()` and `TaxonomyBuilder.withExtensions()` apply the extensions registered with `ServiceLoader`, and
  the Linneo module registers none: its `module-info.java` has a `uses` clause but no `provides` clause, and the JAR
  has no `META-INF/services` file.

A shipped extension follows the same rules as yours: it adds children of existing terms (a shipped extension may add
a new root of data uses where fideslang has none for the sector, as the university extension does) and it only makes
protection stricter.

## Applying a shipped extension

Apply it on the builder, after the standard terms:

```java
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;

Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();
```

`taxonomy.toString()` prints `Taxonomy[158 data categories, 90 data uses, 34 data subjects]`, against
`Taxonomy[125 data categories, 63 data uses, 21 data subjects]` for the standard taxonomy. Keep the result and use it
everywhere: in the [`Classifier`](../classify/classifier.md), the
[`ProcessingChecker`](../processing/checker.md) and the catalogue exporters. A `Classifier` built on
`Taxonomy.standard()` reports every key of the extension as `UNKNOWN_CATEGORY`.

## Registering a shipped extension

To have `Taxonomy.load()` apply a shipped extension, the **application** registers it with `ServiceLoader`. How
depends on where Linneo is:

- **Class path** (Linneo and the application on the class path): list the extension's class in the application's
  `META-INF/services/io.github.devbpm.linneo.TaxonomyExtension` (in Maven, under `src/main/resources`), next to
  your own extensions if any:

  ```text
  io.github.devbpm.linneo.extension.university.UniversityTaxonomy
  ```

  When Linneo is on the module path, `ServiceLoader` ignores a `META-INF/services` entry that names a class of a
  named module, so this file has no effect there.

- **Module path**: a `provides` clause can only name a class of the module that declares it, so
  `provides io.github.devbpm.linneo.TaxonomyExtension with io.github.devbpm.linneo.extension.university.UniversityTaxonomy`
  in your `module-info.java` does not compile (`service implementation must be defined in the same module as the
  provides directive`). Provide it through a class of your module with a public static `provider()` method:

  ```java
  package com.example.app;

  import io.github.devbpm.linneo.TaxonomyExtension;
  import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;

  public final class UniversityProvider {

      private UniversityProvider() {
      }

      public static TaxonomyExtension provider() {
          return new UniversityTaxonomy();
      }
  }
  ```

  ```java
  module com.example.app {
      requires io.github.devbpm.linneo;

      provides io.github.devbpm.linneo.TaxonomyExtension with com.example.app.UniversityProvider;
  }
  ```

The explicit `extend` call above works in both cases and needs no registration.

## Shipped extensions and yours

A shipped extension and your own extensions are applied to the same builder and combine like any two extensions:

- **Names.** Each term records the name of the extension that added it as its origin (`university`, or yours).
  `extend` applies the same extension class only once (so a shipped extension can be applied explicitly and also be
  registered as a service), but throws a `TaxonomyException` if a different class with the same name has already
  been applied: `The extensions io.github.devbpm.linneo.extension.university.UniversityTaxonomy and
  com.example.app.MyExtension are both named 'university'`. Do not give your own extension the name of a shipped
  one.
- **Keys.** Check the shipped extension before adding a term: if it already has the key, use it. Adding a key twice
  fails with `Duplicate key '<key>'`.
- **Order.** Your extension can make a term of a shipped extension stricter with `refineCategory` only if the
  shipped extension is applied first; otherwise `refineCategory` fails with
  `Cannot refine unknown data category '<key>'`. With `extend` the order is the order of the calls. `load()` applies
  the registered extensions sorted by name. To apply a shipped extension first and then every registered extension,
  whatever their names:

  ```java
  Taxonomy taxonomy = Taxonomy.builder()
          .withStandard()
          .extend(new UniversityTaxonomy())
          .withExtensions()
          .build();
  ```

- **Constants.** Use the shipped extension's constant classes (for example `UniversityCategories`) in annotations,
  and keep your own constants in your own classes.

See also: [University extension](university.md) · [Extending the taxonomy](../extending.md) ·
[Standard taxonomy](../standard-taxonomy.md) · course: [Extending the taxonomy](../../course/06-extending-the-taxonomy.md)
