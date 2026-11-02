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
  - src/main/java/io/github/devbpm/linneo/extension/finance/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/finance/FinanceTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/SupplyChainTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/ProjectOperationsTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/HumanResourcesTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyBuilder.java
  - src/main/java/io/github/devbpm/linneo/Trait.java
-->

# Extensiones

Además de la [taxonomía estándar](../standard-taxonomy.md), Linneo incluye **extensiones opcionales** para un
sector: términos que la mayoría de las aplicaciones no necesitan, empaquetados como una
[`TaxonomyExtension`](../extending.md#taxonomyextension) en el JAR de Linneo, cada una en su propio paquete exportado
`io.github.devbpm.linneo.extension.<sector>`, con constantes de compilación para sus claves.

| Extensión | Clase | `name()` | Añade | Página |
|---|---|---|---|---|
| Educación superior | `io.github.devbpm.linneo.extension.university.UniversityTaxonomy` | `"university"` | 32 categorías de datos, 27 usos de datos, 12 interesados | [Universidad](university.md) |
| Finanzas y contabilidad | `io.github.devbpm.linneo.extension.finance.FinanceTaxonomy` | `"finance"` | 19 categorías de datos, 13 usos de datos, 3 interesados | [Finanzas](finance.md) |
| Gestión de la cadena de suministro | `io.github.devbpm.linneo.extension.supplychain.SupplyChainTaxonomy` | `"supply_chain"` | 16 categorías de datos, 12 usos de datos, 3 interesados | [Cadena de suministro](supply-chain.md) |
| Operaciones de proyecto | `io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsTaxonomy` | `"project_operations"` | 11 categorías de datos, 6 usos de datos, 2 interesados | [Operaciones de proyecto](project-operations.md) |
| Recursos humanos y nómina | `io.github.devbpm.linneo.extension.humanresources.HumanResourcesTaxonomy` | `"human_resources"` | 21 categorías de datos, 15 usos de datos, 6 interesados | [Recursos humanos](human-resources.md) |

Las extensiones de finanzas, cadena de suministro, operaciones de proyecto y recursos humanos se basan en las áreas de
negocio del Common Data Model (CDM); consulta la sección *Fuentes* de cada página. La extensión universitaria se basa en
la Open Education API. Las cinco no comparten ninguna clave, así que cualquiera puede aplicarse con cualquier otra y con
las tuyas.

## No se aplican por defecto

Una extensión incluida nunca forma parte de la taxonomía salvo que la aplicación la pida:

- `Taxonomy.standard()` nunca incluye extensiones.
- `Taxonomy.load()` y `TaxonomyBuilder.withExtensions()` aplican las extensiones registradas con `ServiceLoader`, y el
  módulo de Linneo no registra ninguna: su `module-info.java` tiene una cláusula `uses` pero ninguna `provides`, y el
  JAR no tiene fichero `META-INF/services`.

Una extensión incluida sigue las [mismas reglas que las tuyas](../extending.md#qué-puede-hacer-una-extensión): añade
categorías de datos por debajo de las existentes, nunca una raíz (una rama nueva, como hace la extensión de recursos
humanos con `user.employment`), usos de datos y sujetos de datos (puede añadir una raíz nueva de usos de datos cuando
fideslang no tiene ninguna para el sector, como hace la extensión universitaria), y solo eleva la protección y añade
rasgos.

## Aplicar una extensión incluida

Aplícala en el builder, después de los términos estándar:

```java
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.extension.university.UniversityTaxonomy;

Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();
```

`taxonomy.toString()` imprime `Taxonomy[158 data categories, 90 data uses, 34 data subjects]`, frente a
`Taxonomy[126 data categories, 63 data uses, 22 data subjects]` de la taxonomía estándar. Con las cinco extensiones
incluidas aplicadas imprime `Taxonomy[225 data categories, 136 data uses, 48 data subjects]`. Guarda el resultado y
úsalo en todas partes: en el [`Classifier`](../classify/classifier.md), en el
[`ProcessingChecker`](../processing/checker.md) y en los exportadores a catálogos. Un `Classifier` construido sobre
`Taxonomy.standard()` informa de cada clave de la extensión como `UNKNOWN_CATEGORY`.

## Rasgos

Cada extensión incluida declara los [rasgos](../terms.md#trait) de sus términos, para que el
[cribado de impacto](../processing/impact-screening.md), la [evaluación de violaciones](../processing/breach-assessment.md)
y los [derechos del interesado](../processing/subject-rights.md) los lean igual que los estándar. Las categorías
personales que cuelgan de `user.financial` o de `user.contact` heredan esos rasgos; las extensiones añaden:

| Extensión | Rasgos que declara |
|---|---|
| Universidad | usos de datos: `advancement` `DIRECT_MARKETING`; `advancement.prospect_research` y `education.financial_aid.means_assessment` `EVALUATION`; `education.learning_analytics` `EVALUATION` y `MONITORING` |
| Finanzas | usos de datos: `finance.credit_management` y `finance.anti_money_laundering` `EVALUATION` |
| Cadena de suministro | uso de datos: `operations.logistics.fleet_management` `MONITORING`; sujeto de datos: `driver` `VULNERABLE` |
| Recursos humanos | categorías: `user.employment.compensation`, `user.employment.benefit` y `user.employment.payroll` `FINANCIAL`, `user.employment.monitoring` `MONITORING`; usos de datos: `employment.monitoring` `MONITORING`, `employment.performance` y `employment.recruitment.background_check` `EVALUATION`; sujetos de datos: `dependant`, `reporting_person`, `temporary_agency_worker` y `worker_representative` `VULNERABLE` |
| Operaciones de proyecto | ninguno |

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
  en tu `module-info.java` no compila
  (`service implementation must be defined in the same module as the provides directive`). Ofrécela mediante una clase
  de tu módulo con un método público estático `provider()`:

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

- **Nombres.** Cada término registra como origen el nombre de la extensión que lo añadió (`university`, `finance`,
  `supply_chain`, `project_operations`, `human_resources`, o el de la tuya). `extend` aplica una misma clase de
  extensión una sola vez (así una extensión incluida puede aplicarse de forma explícita y estar además registrada como
  servicio), pero lanza una `TaxonomyException` si ya se aplicó una clase distinta con el mismo nombre:
  `The extensions io.github.devbpm.linneo.extension.university.UniversityTaxonomy and com.example.app.MyExtension are both named 'university'`.
  No le pongas a tu extensión el nombre de una incluida.
- **Claves.** Revisa la extensión incluida antes de añadir un término: si ya tiene la clave, úsala. Añadir dos veces
  una clave falla con `Duplicate key '<key>'`.
- **Orden.** Tu extensión solo puede hacer más estricto un término de una extensión incluida con `refineCategory` si
  la incluida se aplica antes; si no, `refineCategory` falla con `Cannot refine unknown data category '<key>'`. Elevar
  no depende del orden: si dos extensiones elevan la misma categoría, el resultado es el nivel más estricto se aplique
  primero la que se aplique. Una extensión no puede deprecar ni fijar el `since` de un término que no añadió. Con
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

Véase también: [Universidad](university.md) · [Finanzas](finance.md) · [Cadena de suministro](supply-chain.md) ·
[Operaciones de proyecto](project-operations.md) · [Recursos humanos](human-resources.md) ·
[Extender la taxonomía](../extending.md) · [Taxonomía estándar](../standard-taxonomy.md) · curso:
[Extender la taxonomía](../../course/06-extending-the-taxonomy.md)
