---
title: "Mapeo de OpenMetadata"
parent: "Referencia"
nav_order: 14
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OpenMetadataMapping.java
-->

# Mapeo de OpenMetadata

`io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataMapping` — `public record`, `@since 0.1.0`

Los nombres con los que se publica la taxonomía en OpenMetadata, y a partir de qué nivel una columna personal se
considera sensible. Se pasa a [`OpenMetadataCatalog.of(taxonomy, mapping)`](catalog.md).

```java
public record OpenMetadataMapping(String categories, String personalData, String pii, String confidentiality,
                                  String integrity, String identifiability, String handling,
                                  Confidentiality sensitiveFrom)
```

## Componentes y valores por defecto

| Componente | Por defecto | ¿Lo crea Linneo? | Significado |
|---|---|---|---|
| `categories` | `DataCategory` | sí | Clasificación que contiene la jerarquía de categorías como etiquetas anidadas. |
| `personalData` | `PersonalData` | no (predefinida) | Clasificación predefinida con las etiquetas `Personal` y `SpecialCategory`. |
| `pii` | `PII` | no (predefinida) | Clasificación predefinida con las etiquetas `None`, `NonSensitive` y `Sensitive`. |
| `confidentiality` | `Confidentiality` | sí | Clasificación de los niveles de confidencialidad. |
| `integrity` | `Integrity` | sí | Clasificación de los niveles de integridad. |
| `identifiability` | `Identifiability` | sí | Clasificación de la identificabilidad de las columnas personales. |
| `handling` | `DataHandling` | sí | Clasificación que contiene la etiqueta `Secret`. |
| `sensitiveFrom` | `Confidentiality.MEDIUM` | — | Confidencialidad mínima de los datos personales que se etiquetan `PII.Sensitive`; los niveles inferiores son `PII.NonSensitive`. |

`personalData` y `pii` nombran clasificaciones predefinidas de OpenMetadata: se reutilizan y
`OpenMetadataCatalog.classifications()` nunca las devuelve. Los nombres de etiqueta dentro de cada clasificación son
fijos.

## Validación

El constructor canónico comprueba todos los componentes:

| Problema | Excepción |
|---|---|
| Un nombre es `null` | `NullPointerException` (`classification name`) |
| Un nombre está en blanco o contiene un punto | `IllegalArgumentException` (`Invalid OpenMetadata classification name '<name>'`) |
| Dos de los siete nombres son iguales | `IllegalArgumentException` (`Duplicate OpenMetadata classification name '<name>'`) |
| `sensitiveFrom` es `null` | `NullPointerException` (`sensitiveFrom`) |

Además, cada nombre debe ser un nombre de entidad válido en OpenMetadata; Linneo no lo comprueba más allá de lo
anterior.

## Métodos

| Método | Descripción |
|---|---|
| `static OpenMetadataMapping defaults()` | Los valores por defecto anteriores. |
| `OpenMetadataMapping withCategories(String name)` | Una copia con otro nombre para la clasificación de categorías. |
| `OpenMetadataMapping withConfidentiality(String name)` | Una copia con otro nombre para la clasificación de confidencialidad. |
| `OpenMetadataMapping withSensitiveFrom(Confidentiality level)` | Una copia con otro umbral para `PII.Sensitive`. |

Los métodos `with…` validan igual que el constructor. No hay métodos `with…` para `personalData`, `pii`,
`integrity`, `identifiability` ni `handling`: usa el constructor canónico.

## Ejemplos

```java
OpenMetadataMapping mapping = OpenMetadataMapping.defaults()
        .withCategories("Categoria")
        .withConfidentiality("Confidencialidad")
        .withSensitiveFrom(Confidentiality.LOW);

OpenMetadataCatalog catalog = OpenMetadataCatalog.of(Taxonomy.standard(), mapping);
catalog.tagsOf(Taxonomy.standard().requireCategory(Categories.USER_NAME));
// [Categoria.user.name, PersonalData.Personal, PII.Sensitive, Confidencialidad.Low, Integrity.Medium]
```

Para renombrar todas las clasificaciones de Linneo:

```java
OpenMetadataMapping mapping = new OpenMetadataMapping("Categoria", "PersonalData", "PII", "Confidencialidad",
        "Integridad", "Identificabilidad", "Tratamiento", Confidentiality.MEDIUM);
```

Véase también: [Catálogo de OpenMetadata](catalog.md) · [Niveles de protección](../protection-levels.md) ·
curso: [OpenMetadata](../../course/07-openmetadata.md)
