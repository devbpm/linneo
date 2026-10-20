---
title: "Mapeo de Apache Atlas"
parent: "Referencia"
nav_order: 18
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasMapping.java
-->

# Mapeo de Apache Atlas

`io.github.devbpm.linneo.catalog.atlas.AtlasMapping` — `public record`, `@since 0.1.0`

Los nombres de las clasificaciones raíz de Atlas bajo las que se publica la taxonomía. Cada nombre es el primer
segmento de las clasificaciones que cuelgan de él: con los valores por defecto, `user.contact.email` es la
clasificación `DataCategory.user.contact.email`. Pásalo a [`AtlasCatalog.of(taxonomy, mapping)`](catalog.md).

```java
public record AtlasMapping(String categories, String personalData, String confidentiality, String integrity,
                           String identifiability, String handling)
```

## Componentes y valores por defecto

| Componente | Por defecto | Raíz de | Clasificaciones que cuelgan de ella |
|---|---|---|---|
| `categories` | `DataCategory` | la jerarquía de categorías | `<nombre>.<key>` para cada categoría, cada una subtipo de la de su padre |
| `personalData` | `PersonalData` | la naturaleza jurídica | `<nombre>.Personal`, `<nombre>.SpecialCategory`, `<nombre>.CriminalOffence` |
| `confidentiality` | `Confidentiality` | los niveles de confidencialidad | `<nombre>.Public`, `.Low`, `.Medium`, `.High` |
| `integrity` | `Integrity` | los niveles de integridad | `<nombre>.Low`, `.Medium`, `.High` |
| `identifiability` | `Identifiability` | la identificabilidad de las columnas personales | `<nombre>.Identified`, `.Pseudonymized`, `.Anonymized`, `.Aggregated` |
| `handling` | `DataHandling` | las reglas de tratamiento | `<nombre>.Secret` |

Linneo crea las seis raíces. Los nombres que siguen a la raíz son fijos.

## Validación

El constructor canónico comprueba cada nombre:

| Problema | Excepción |
|---|---|
| Un nombre es `null` | `NullPointerException` (`classification name`) |
| Un nombre no cumple `[A-Za-z][A-Za-z0-9_]*` (está vacío, empieza por un dígito o contiene un punto, un espacio…) | `IllegalArgumentException` (`Invalid Atlas classification name '<name>'`) |
| Dos de los seis nombres son iguales | `IllegalArgumentException` (`Duplicate Atlas classification name '<name>'`) |

Atlas admite puntos y espacios en los nombres de clasificación; Linneo los prohíbe en las raíces para que el primer
segmento de cada nombre sea la raíz.

## Métodos

| Método | Descripción |
|---|---|
| `static AtlasMapping defaults()` | Los valores por defecto anteriores. |
| `AtlasMapping withCategories(String name)` | Una copia con otro nombre para la raíz de categorías. |
| `AtlasMapping withPersonalData(String name)` | Una copia con otro nombre para la raíz de naturaleza jurídica. |
| `AtlasMapping withConfidentiality(String name)` | Una copia con otro nombre para la raíz de confidencialidad. |
| `AtlasMapping withIntegrity(String name)` | Una copia con otro nombre para la raíz de integridad. |
| `AtlasMapping withIdentifiability(String name)` | Una copia con otro nombre para la raíz de identificabilidad. |
| `AtlasMapping withHandling(String name)` | Una copia con otro nombre para la raíz de tratamiento. |

Los métodos `with...` validan igual que el constructor.

## Ejemplos

```java
AtlasMapping mapping = AtlasMapping.defaults()
        .withCategories("CategoriaDato")
        .withConfidentiality("Confidencialidad");

AtlasCatalog catalog = AtlasCatalog.of(Taxonomy.standard(), mapping);
catalog.classificationsOf(Taxonomy.standard().requireCategory(Categories.USER_NAME));
// [CategoriaDato.user.name, PersonalData.Personal, Confidencialidad.Low, Integrity.Medium]
```

Para renombrar todas las raíces:

```java
AtlasMapping mapping = new AtlasMapping("CategoriaDato", "DatoPersonal", "Confidencialidad", "Integridad",
        "Identificabilidad", "Tratamiento");
```

```java
AtlasMapping.defaults().withCategories("Data.Category");
// IllegalArgumentException: Invalid Atlas classification name 'Data.Category'
```

Véase también: [Catálogo de Apache Atlas](catalog.md) · [Niveles de protección](../protection-levels.md) · curso:
[DataHub y Apache Atlas](../../course/08-datahub-and-atlas.md)
