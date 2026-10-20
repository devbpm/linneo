---
title: "Extensiones"
parent: "Referencia"
nav_order: 7
permalink: "/es/reference/extensions/"
---
<!--
doc-sources:
  - src/main/java/module-info.java
  - src/main/java/io/github/devbpm/linneo/extension/university/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyBuilder.java
-->

# Extensiones

Además de la [taxonomía estándar](../standard-taxonomy.md), Linneo incluye **extensiones opcionales** para un
sector: términos que la mayoría de las aplicaciones no necesitan, empaquetados como una
[`TaxonomyExtension`](../extending.md#taxonomyextension) en el JAR de Linneo, cada una en su propio paquete exportado
`io.github.devbpm.linneo.extension.<sector>`, con constantes de compilación para sus claves.

| Extensión | Clase | `name()` | Añade | Página |
|---|---|---|---|---|
| Educación superior | `io.github.devbpm.linneo.extension.university.UniversityTaxonomy` | `"university"` | 33 categorías de datos, 27 usos de datos, 13 interesados | [Universidad](university.md) |

## No se aplican por defecto

Una extensión incluida nunca forma parte de la taxonomía salvo que la aplicación la pida:

- `Taxonomy.standard()` nunca incluye extensiones.
- `Taxonomy.load()` y `TaxonomyBuilder.withExtensions()` aplican las extensiones registradas con `ServiceLoader`, y el
  módulo de Linneo no registra ninguna: su `module-info.java` tiene una cláusula `uses` pero ninguna `provides`, y el
  JAR no tiene fichero `META-INF/services`.

Una extensión incluida sigue las mismas reglas que las tuyas: añade hijos de términos existentes (una extensión
incluida puede añadir una raíz nueva de usos de datos cuando fideslang no tiene ninguna para el sector, como hace la
extensión universitaria) y solo endurece la protección.

## Aplicar una extensión incluida

Aplícala en el builder, después de los términos estándar:

```java
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;

Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();
```

`taxonomy.toString()` imprime `Taxonomy[156 data categories, 90 data uses, 34 data subjects]`, frente a
`Taxonomy[123 data categories, 63 data uses, 21 data subjects]` de la taxonomía estándar. Guarda el resultado y úsalo
en todas partes: en el [`Classifier`](../classify/classifier.md), en el
[`ProcessingChecker`](../processing/checker.md) y en los exportadores a catálogos. Un `Classifier` construido sobre
`Taxonomy.standard()` informa de cada clave de la extensión como `UNKNOWN_CATEGORY`.

## Registrar una extensión incluida

Para que `Taxonomy.load()` aplique una extensión incluida, la **aplicación** la registra con `ServiceLoader`. Cómo
hacerlo depende de dónde esté Linneo:

- **Class path** (Linneo y la aplicación en el class path): escribe la clase de la extensión en el fichero
  `META-INF/services/io.github.devbpm.linneo.TaxonomyExtension` de la aplicación (en Maven, dentro de
  `src/main/resources`), junto a tus propias extensiones si las tienes:

  ```text
  io.github.devbpm.linneo.extension.university.UniversityTaxonomy
  ```

  Cuando Linneo está en el module path, `ServiceLoader` ignora una entrada de `META-INF/services` que nombra una clase
  de un módulo con nombre, así que allí este fichero no tiene efecto.

- **Module path**: una cláusula `provides` solo puede nombrar una clase del módulo que la declara, así que
  `provides io.github.devbpm.linneo.TaxonomyExtension with io.github.devbpm.linneo.extension.university.UniversityTaxonomy`
  en tu `module-info.java` no compila (`service implementation must be defined in the same module as the provides
  directive`). Ofrécela mediante una clase de tu módulo con un método público estático `provider()`:

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

La llamada explícita a `extend` de la sección anterior funciona en ambos casos y no necesita registro.

## Las extensiones incluidas y las tuyas

Una extensión incluida y tus propias extensiones se aplican al mismo builder y se combinan como dos extensiones
cualesquiera:

- **Nombres.** Cada término registra como origen el nombre de la extensión que lo añadió (`university`, o el de la
  tuya). `extend` aplica una misma clase de extensión una sola vez (así una extensión incluida puede aplicarse de
  forma explícita y estar además registrada como servicio), pero lanza una `TaxonomyException` si ya se aplicó una
  clase distinta con el mismo nombre: `The extensions io.github.devbpm.linneo.extension.university.UniversityTaxonomy
  and com.example.app.MyExtension are both named 'university'`. No le pongas a tu extensión el nombre de una
  incluida.
- **Claves.** Revisa la extensión incluida antes de añadir un término: si ya tiene la clave, úsala. Añadir dos veces
  una clave falla con `Duplicate key '<key>'`.
- **Orden.** Tu extensión solo puede hacer más estricto un término de una extensión incluida con `refineCategory` si
  la incluida se aplica antes; si no, `refineCategory` falla con `Cannot refine unknown data category '<key>'`. Con
  `extend`, el orden es el de las llamadas. `load()` aplica las extensiones registradas ordenadas por nombre. Para
  aplicar primero una extensión incluida y después todas las registradas, se llamen como se llamen:

  ```java
  Taxonomy taxonomy = Taxonomy.builder()
          .withStandard()
          .extend(new UniversityTaxonomy())
          .withExtensions()
          .build();
  ```

- **Constantes.** Usa las clases de constantes de la extensión incluida (por ejemplo `UniversityCategories`) en las
  anotaciones, y guarda tus propias constantes en tus propias clases.

Véase también: [Extensión universitaria](university.md) · [Extender la taxonomía](../extending.md) ·
[Taxonomía estándar](../standard-taxonomy.md) · curso: [Extender la taxonomía](../../course/06-extending-the-taxonomy.md)
