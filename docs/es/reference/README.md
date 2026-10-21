---
title: "Referencia"
nav_order: 4
permalink: "/es/reference/"
---
<!--
doc-sources:
  - src/main/java/module-info.java
  - src/main/java/io/github/devbpm/linneo/package-info.java
  - src/main/java/io/github/devbpm/linneo/annotation/package-info.java
  - src/main/java/io/github/devbpm/linneo/classify/package-info.java
  - src/main/java/io/github/devbpm/linneo/processing/package-info.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/package-info.java
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/package-info.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/university/package-info.java
-->

# Referencia de Linneo

Linneo es una biblioteca para Java 21 o superior, sin dependencias en tiempo de ejecución, que incluye una taxonomía
de clasificación de datos y la aplica a un modelo de datos. Una `Taxonomy` contiene tres jerarquías de términos con
claves en notación de puntos: **categorías de datos** (qué contiene un campo), **usos de datos** (para qué se tratan)
y **sujetos de datos** (de quién son). La taxonomía estándar es la taxonomía por defecto de fideslang más las
extensiones de Linneo para datos no personales y datos educativos. Cada categoría de datos lleva la protección que
requiere (naturaleza jurídica, confidencialidad, integridad, secreto), que sus hijas heredan y solo pueden endurecer.
Los modelos declaran lo que contienen con anotaciones, un `Classifier` comprueba esas anotaciones en los tests, las
actividades de tratamiento vinculan finalidades, interesados y datos personales con sus bases jurídicas y generan el
registro de actividades de tratamiento del RGPD, y tres exportadores traducen la taxonomía para un catálogo de
datos: `OpenMetadataCatalog` (clasificaciones y etiquetas de OpenMetadata), `DataHubCatalog` (un glosario de negocio
de DataHub) y `AtlasCatalog` (clasificaciones de Apache Atlas). Las aplicaciones añaden sus propios términos mediante una `TaxonomyExtension`, y Linneo incluye una opcional para
universidades que solo se aplica si se pide.

Esta referencia describe todos los tipos públicos. Para una introducción guiada, consulta el
[curso](../course/README.md).

## Módulo y paquetes

El JAR es el módulo JPMS con nombre `io.github.devbpm.linneo`:

```java
module io.github.devbpm.linneo {
    exports io.github.devbpm.linneo;
    exports io.github.devbpm.linneo.annotation;
    exports io.github.devbpm.linneo.classify;
    exports io.github.devbpm.linneo.processing;
    exports io.github.devbpm.linneo.catalog.openmetadata;
    exports io.github.devbpm.linneo.catalog.datahub;
    exports io.github.devbpm.linneo.catalog.atlas;
    exports io.github.devbpm.linneo.extension.university;

    uses io.github.devbpm.linneo.TaxonomyExtension;
}
```

| Paquete | Exportado | Contenido |
|---|---|---|
| `io.github.devbpm.linneo` | sí | La taxonomía (`Taxonomy`, `TaxonomyBuilder`, `TaxonomyExtension`), sus términos (`Term`, `TermInfo`, `DataCategory`, `DataUse`, `DataSubject`), los enumerados de protección y las constantes de claves (`Categories`, `DataUses`, `DataSubjects`) |
| `io.github.devbpm.linneo.annotation` | sí | Las anotaciones que declaran qué contiene cada campo de un modelo: `@Category`, `@DefaultCategory`, `@CategoryByRow`, `@Exempt` |
| `io.github.devbpm.linneo.classify` | sí | Lectura y verificación de las anotaciones de un modelo: `Classifier`, `ClassClassification`, `FieldClassification`, `Violation`, `ClassificationException` |
| `io.github.devbpm.linneo.processing` | sí | Las actividades de tratamiento y el registro de actividades de tratamiento del RGPD: `ProcessingActivity`, `Transfer`, `LegalBasis`, `SpecialCategoryCondition`, `CriminalDataBasis`, `TransferSafeguard`, `ProcessingChecker`, `CheckedActivity`, `ProcessingViolation`, `ProcessingException`, `ProcessingRecord` |
| `io.github.devbpm.linneo.catalog.openmetadata` | sí | La taxonomía como clasificaciones y etiquetas de OpenMetadata: `OpenMetadataCatalog`, `OpenMetadataMapping`, `OmClassification`, `OmTag`. Sin llamadas de red |
| `io.github.devbpm.linneo.catalog.datahub` | sí | La taxonomía como glosario de negocio de DataHub: `DataHubCatalog`, `DataHubMapping`, `DhProposal`. Sin llamadas de red |
| `io.github.devbpm.linneo.catalog.atlas` | sí | La taxonomía como clasificaciones de Apache Atlas: `AtlasCatalog`, `AtlasMapping`, `AtlasClassificationDef`. Sin llamadas de red |
| `io.github.devbpm.linneo.extension.university` | sí | Una extensión opcional para universidades, que no se aplica por defecto: `UniversityTaxonomy` y sus constantes de claves `UniversityCategories`, `UniversityDataUses`, `UniversityDataSubjects` |
| `io.github.devbpm.linneo.internal` | no | `StandardTaxonomy`, que carga los recursos TSV incluidos, y `Json`, el escritor JSON mínimo que comparten los tres exportadores. No forma parte de la API pública |

La cláusula `uses` permite que `Taxonomy.load()` y `TaxonomyBuilder.withExtensions()` encuentren extensiones con
`java.util.ServiceLoader`; consulta [Extender la taxonomía](extending.md). El módulo no tiene cláusula `provides`:
las extensiones que incluye se aplican de forma explícita; consulta [Extensiones](extensions/README.md).

Para usar la biblioteca desde una aplicación modular:

```java
module com.example.app {
    requires io.github.devbpm.linneo;
}
```

## Páginas

**Modelo de la taxonomía** (`io.github.devbpm.linneo`)

- [Taxonomía](taxonomy.md) — `Taxonomy` (`standard`, `load`, `builder`, búsquedas, obsolescencia, navegación) y
  `TaxonomyException`.
- [Términos](terms.md) — `Term`, `TermInfo`, `DataCategory`, `DataUse`, `DataSubject`.
- [Niveles de protección](protection-levels.md) — `LegalNature`, `Confidentiality`, `Integrity`, `Identifiability`
  y la regla de herencia.
- [Taxonomía estándar](standard-taxonomy.md) — la taxonomía de fideslang incluida, los añadidos y la política de
  protección de Linneo, los formatos TSV.
- [Constantes de claves](key-constants.md) — `Categories`, `DataUses`, `DataSubjects`.
- [Extender la taxonomía](extending.md) — `TaxonomyBuilder`, `TaxonomyExtension`, `CategoryDraft`, `TermDraft`.

**Extensiones** (`io.github.devbpm.linneo.extension.university`)

- [Visión general de las extensiones](extensions/README.md) — las extensiones opcionales que incluye Linneo, cómo
  aplicarlas o registrarlas y cómo se combinan con las tuyas.
- [Universidad](extensions/university.md) — `UniversityTaxonomy`, `UniversityCategories`, `UniversityDataUses`,
  `UniversityDataSubjects`: todos los términos con su protección, las claves estándar que conviene usar, las reglas
  de derivación y las fuentes.

**Anotaciones** (`io.github.devbpm.linneo.annotation`)

- [Visión general de las anotaciones](annotations/README.md) — precedencia y conflictos.
- [`@Category`](annotations/category.md)
- [`@DefaultCategory`](annotations/default-category.md)
- [`@CategoryByRow`](annotations/category-by-row.md)
- [`@Exempt`](annotations/exempt.md)

**Clasificación** (`io.github.devbpm.linneo.classify`)

- [Classifier](classify/classifier.md) — `Classifier`, `ClassificationException`.
- [Resultados](classify/results.md) — `ClassClassification`, `FieldClassification`.
- [Violaciones](classify/violations.md) — `Violation` y cada `Code`.

**Actividades de tratamiento** (`io.github.devbpm.linneo.processing`)

- [Visión general de los tratamientos](processing/README.md) — cómo se relacionan las actividades con la taxonomía y
  el clasificador, errores y avisos.
- [ProcessingActivity](processing/activity.md) — `ProcessingActivity`, su `Builder`, `Transfer`.
- [Bases jurídicas](processing/bases.md) — `LegalBasis`, `SpecialCategoryCondition`, `CriminalDataBasis`,
  `TransferSafeguard`.
- [ProcessingChecker](processing/checker.md) — `ProcessingChecker`, `CheckedActivity`, `ProcessingViolation` y
  cada `Code`, `ProcessingException`.
- [ProcessingRecord](processing/record.md) — el registro en Markdown y en JSON.

**OpenMetadata** (`io.github.devbpm.linneo.catalog.openmetadata`)

- [Catálogo](openmetadata/catalog.md) — `OpenMetadataCatalog`, `OmClassification`, `OmTag`, cuerpos JSON,
  correspondencia de etiquetas.
- [Mapeo](openmetadata/mapping.md) — `OpenMetadataMapping`.

**DataHub** (`io.github.devbpm.linneo.catalog.datahub`)

- [Catálogo](datahub/catalog.md) — `DataHubCatalog`, `DhProposal`, nodos y términos del glosario, cuerpos de
  `ingestProposal`, términos de cada campo.
- [Mapeo](datahub/mapping.md) — `DataHubMapping`.

**Apache Atlas** (`io.github.devbpm.linneo.catalog.atlas`)

- [Catálogo](atlas/catalog.md) — `AtlasCatalog`, `AtlasClassificationDef`, tipos de clasificación, cuerpos de
  typedefs y de clasificaciones.
- [Mapeo](atlas/mapping.md) — `AtlasMapping`.

## Comparativa de catálogos de datos

Los tres exportadores publican la misma taxonomía y calculan las etiquetas de cada campo clasificado a partir del
mismo `FieldClassification`; se diferencian en lo que ofrece cada catálogo. Ninguno hace llamadas de red ni aplica
las etiquetas a tus tablas: construyen cuerpos de petición y nombres.

| | OpenMetadata | DataHub | Apache Atlas |
|---|---|---|---|
| Punto de entrada | [`OpenMetadataCatalog`](openmetadata/catalog.md) | [`DataHubCatalog`](datahub/catalog.md) | [`AtlasCatalog`](atlas/catalog.md) |
| Una categoría se convierte en | una etiqueta anidada `DataCategory.<key>` en una clasificación mutuamente excluyente | un término del glosario `urn:li:glossaryTerm:DataCategory.<key>`, más un nodo del glosario si tiene hijas | un tipo de clasificación `DataCategory.<key>` |
| El árbol se convierte en | el `parent` de la etiqueta | nodos que reflejan el árbol, e `isRelatedTerms` hacia el término del padre | `superTypes`: una búsqueda del padre encuentra sus subclasificaciones |
| Protección implícita en una categoría | `associatedTags` de su etiqueta | `relatedTerms` de su término | no se guarda en el tipo; `classificationsOf(category)` la enumera |
| Naturaleza jurídica | `PersonalData.Personal` / `SpecialCategory` predefinidas (los datos penales son `SpecialCategory`) | `PersonalData.Personal` / `SpecialCategory` / `CriminalOffence` | `PersonalData.Personal` / `SpecialCategory` / `CriminalOffence` |
| Sensibilidad | `PII.None` / `NonSensitive` / `Sensitive` predefinidas, umbral `sensitiveFrom` | — | — |
| Niveles y secreto | `Confidentiality.*`, `Integrity.*`, `Identifiability.*`, `DataHandling.Secret` | los mismos, como términos del glosario | los mismos, como clasificaciones (no son supertipos de las categorías) |
| Se crea con la taxonomía estándar | 5 clasificaciones y 137 etiquetas | 293 propuestas: 28 nodos, 140 términos y 125 aspectos de relaciones | 146 tipos de clasificación |
| Peticiones | `PUT /api/v1/classifications`, `PUT /api/v1/tags`, una por cuerpo | `POST /aspects?action=ingestProposal`, una por propuesta | un único `POST /api/atlas/v2/types/typedefs` |
| Volver a enviar | crea o actualiza | `UPSERT` (sustituye cada aspecto) | `POST` rechaza los tipos existentes; se actualizan con `PUT` |
| Etiquetas de un campo | `tagsOf(field)`: nombres de etiqueta | `termsOf(field)`: URN de términos | `classificationsOf(field)`: nombres; `classificationsJson(field)`: cuerpo de la petición |
| Aplicarlas (tu código) | la API de tablas que ya uses | por ejemplo, la mutación GraphQL `addTerms` con la ruta del campo como `subResource` | `POST /api/atlas/v2/entity/guid/{guid}/classifications` |
| Nombres | [`OpenMetadataMapping`](openmetadata/mapping.md): siete nombres y un umbral | [`DataHubMapping`](datahub/mapping.md): seis nombres de nodo | [`AtlasMapping`](atlas/mapping.md): seis nombres de raíz |

Véase también: [curso](../course/README.md) · [English](../../en/reference/README.md)
