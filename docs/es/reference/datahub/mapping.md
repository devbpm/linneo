---
title: "Mapeo de DataHub"
parent: "Referencia"
nav_order: 16
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DataHubMapping.java
-->

# Mapeo de DataHub

`io.github.devbpm.linneo.catalog.datahub.DataHubMapping` — `public record`, `@since 0.1.0`

Los nombres de los nodos del glosario de DataHub bajo los que se publica la taxonomía. Cada nombre es el primer
segmento de los URN que cuelgan de él: con los valores por defecto, `user.contact.email` es el término
`urn:li:glossaryTerm:DataCategory.user.contact.email`. Pásalo a
[`DataHubCatalog.of(taxonomy, mapping)`](catalog.md).

```java
public record DataHubMapping(String categories, String personalData, String confidentiality, String integrity,
                             String identifiability, String handling)
```

## Componentes y valores por defecto

| Componente | Por defecto | Nodo de | Términos que contiene |
|---|---|---|---|
| `categories` | `DataCategory` | la jerarquía de categorías | `<nombre>.<key>` para cada categoría, con un subnodo `<nombre>.<key>` para cada categoría con hijas |
| `personalData` | `PersonalData` | la naturaleza jurídica | `Personal`, `SpecialCategory`, `CriminalOffence` |
| `confidentiality` | `Confidentiality` | los niveles de confidencialidad | `Public`, `Low`, `Medium`, `High` |
| `integrity` | `Integrity` | los niveles de integridad | `Low`, `Medium`, `High` |
| `identifiability` | `Identifiability` | la identificabilidad de los campos personales | `Identified`, `Pseudonymized`, `Anonymized`, `Aggregated` |
| `handling` | `DataHandling` | las reglas de tratamiento | `Secret` |

Linneo crea los seis nodos; DataHub no tiene ningún glosario predefinido que se reutilice. Los nombres de los
términos de cada nodo son fijos, y también los nombres visibles de los nodos raíz (`Data Category`, `Personal Data`,
`Data Handling`…): el mapeo cambia los identificadores y los URN, no el `name` que se muestra en la interfaz.

## Validación

El constructor canónico comprueba cada nombre:

| Problema | Excepción |
|---|---|
| Un nombre es `null` | `NullPointerException` (`glossary node name`) |
| Un nombre no cumple `[A-Za-z][A-Za-z0-9_]*` (está vacío, empieza por un dígito o contiene un punto, un espacio, una coma…) | `IllegalArgumentException` (`Invalid DataHub glossary node name '<name>'`) |
| Dos de los seis nombres son iguales | `IllegalArgumentException` (`Duplicate DataHub glossary node name '<name>'`) |

La regla garantiza que cada nombre sea un único segmento del URN.

## Métodos

| Método | Descripción |
|---|---|
| `static DataHubMapping defaults()` | Los valores por defecto anteriores. |
| `DataHubMapping withCategories(String name)` | Una copia con otro nombre para el nodo de categorías. |
| `DataHubMapping withPersonalData(String name)` | Una copia con otro nombre para el nodo de naturaleza jurídica. |
| `DataHubMapping withConfidentiality(String name)` | Una copia con otro nombre para el nodo de confidencialidad. |
| `DataHubMapping withIntegrity(String name)` | Una copia con otro nombre para el nodo de integridad. |
| `DataHubMapping withIdentifiability(String name)` | Una copia con otro nombre para el nodo de identificabilidad. |
| `DataHubMapping withHandling(String name)` | Una copia con otro nombre para el nodo de tratamiento. |

Los métodos `with...` validan igual que el constructor.

## Ejemplos

```java
DataHubMapping mapping = DataHubMapping.defaults()
        .withCategories("CategoriaDato")
        .withConfidentiality("Confidencialidad");

DataHubCatalog catalog = DataHubCatalog.of(Taxonomy.standard(), mapping);
catalog.termsOf(Taxonomy.standard().requireCategory(Categories.USER_NAME));
// [urn:li:glossaryTerm:CategoriaDato.user.name, urn:li:glossaryTerm:PersonalData.Personal,
//  urn:li:glossaryTerm:Confidencialidad.Low, urn:li:glossaryTerm:Integrity.Medium]
```

Para renombrar todos los nodos:

```java
DataHubMapping mapping = new DataHubMapping("CategoriaDato", "DatoPersonal", "Confidencialidad", "Integridad",
        "Identificabilidad", "Tratamiento");
```

```java
DataHubMapping.defaults().withHandling("Data Handling");
// IllegalArgumentException: Invalid DataHub glossary node name 'Data Handling'
```

Véase también: [Catálogo de DataHub](catalog.md) · [Niveles de protección](../protection-levels.md) · curso:
[DataHub y Apache Atlas](../../course/08-datahub-and-atlas.md)
