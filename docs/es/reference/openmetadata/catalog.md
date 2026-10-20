---
title: "Catálogo de OpenMetadata"
parent: "Referencia"
nav_order: 13
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OpenMetadataCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OmClassification.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OmTag.java
  - src/main/java/io/github/devbpm/linneo/internal/Json.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# Catálogo de OpenMetadata

El paquete `io.github.devbpm.linneo.catalog.openmetadata` traduce una [`Taxonomy`](../taxonomy.md) a clasificaciones y
etiquetas de [OpenMetadata](https://open-metadata.org/) y calcula las etiquetas de cada columna clasificada. **No
hace llamadas de red**: envía los cuerpos JSON con el cliente HTTP que ya uses a `PUT /api/v1/classifications` y
`PUT /api/v1/tags`, y después añade las etiquetas a las columnas de cada tabla. Los cuerpos siguen los esquemas
`CreateClassification` y `CreateTag` de OpenMetadata, incluidos `parent` y `associatedTags`; compruébalos con la
versión de OpenMetadata que utilices.

## OpenMetadataCatalog

`io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataCatalog` — `public final class`, `@since 0.1.0`

Inmutable y seguro entre hilos. No tiene constructor público.

| Método | Devuelve | Descripción |
|---|---|---|
| `static OpenMetadataCatalog of(Taxonomy taxonomy)` | catálogo | Con los [nombres por defecto](mapping.md) (`OpenMetadataMapping.defaults()`). |
| `static OpenMetadataCatalog of(Taxonomy taxonomy, OpenMetadataMapping mapping)` | catálogo | Con los nombres y el umbral de sensibilidad indicados. `NullPointerException` si algún argumento es `null`. |
| `List<OmClassification> classifications()` | lista inmutable | Las clasificaciones que hay que crear, en orden de creación. **No** incluye las clasificaciones predefinidas `PersonalData` y `PII`: se reutilizan. |
| `List<OmTag> tags()` | lista | Las etiquetas que hay que crear, en orden de creación: primero las de niveles y después una por categoría, cada padre antes que sus hijas. |
| `String tagOf(DataCategory category)` | `String` | El nombre completo de la etiqueta de la categoría, como `DataCategory.user.contact.email`. |
| `List<String> tagsOf(DataCategory category)` | lista | La etiqueta propia de la categoría seguida de las que implica (sus `associatedTags`). |
| `List<String> tagsOf(FieldClassification field)` | lista | Las etiquetas de una columna clasificada: su categoría, si es dato personal y de qué tipo, y la protección efectiva del campo. |

```java
Taxonomy taxonomy = Taxonomy.load();
OpenMetadataCatalog catalog = OpenMetadataCatalog.of(taxonomy);

for (OmClassification c : catalog.classifications()) {
    String body = c.toJson();          // PUT /api/v1/classifications
}
for (OmTag t : catalog.tags()) {
    String body = t.toJson();          // PUT /api/v1/tags, en este orden
}

ClassClassification person = new Classifier(taxonomy).classify(Person.class);
for (FieldClassification field : person.fields()) {
    List<String> columnTags = catalog.tagsOf(field);   // para la columna que corresponde al campo
}
```

### Clasificaciones creadas

Con el mapeo por defecto, `classifications()` devuelve:

| `name` | `mutuallyExclusive` | `description` |
|---|---|---|
| `DataCategory` | `true` | What a column holds, from the Linneo data category taxonomy (fideslang default taxonomy plus extensions). |
| `Confidentiality` | `true` | Harm that disclosing the data would cause. |
| `Integrity` | `true` | Harm that an unwanted change to the data would cause. |
| `Identifiability` | `true` | How directly a personal data value points to the person. |
| `DataHandling` | `false` | Handling rules that apply to the data. |

Las descripciones se generan en inglés.

### Etiquetas creadas

`tags()` devuelve, en este orden (12 más una por categoría; 135 con la taxonomía estándar):

| Etiquetas | Clasificación | `name` / `displayName` | `description` |
|---|---|---|---|
| `Public`, `Low`, `Medium`, `High` | `Confidentiality` | el nivel, con mayúscula inicial | `Confidentiality level <Level>.` |
| `Low`, `Medium`, `High` | `Integrity` | el nivel, con mayúscula inicial | `Integrity level <Level>.` |
| `Identified`, `Pseudonymized`, `Anonymized`, `Aggregated` | `Identifiability` | el valor, con mayúscula inicial | `Personal data that is <value>.` |
| `Secret` | `DataHandling` | `Secret` | `A secret that grants access: it must never be read back, exported or published, only verified.` |
| una por categoría, en orden de clave | `DataCategory` | `name`: último segmento de la clave; `displayName`: nombre de la categoría | la descripción de la categoría, más ` Deprecated.` o ` Deprecated: use <key>.` si está obsoleta |

Una etiqueta de categoría tiene `parent` = `DataCategory.<clave del padre>` (ninguno en una raíz), así que la
jerarquía se convierte en etiquetas anidadas: `user.contact.email` es la etiqueta `DataCategory.user.contact.email`.
Las etiquetas de nivel no tienen padre ni etiquetas asociadas.

## Correspondencia de etiquetas

| Linneo | Etiqueta de OpenMetadata (nombres por defecto) |
|---|---|
| Categoría `<key>` | `DataCategory.<key>` |
| Naturaleza `ORDINARY` | `PersonalData.Personal` (predefinida) |
| Naturaleza `SPECIAL_CATEGORY` o `CRIMINAL_OFFENCE` | `PersonalData.SpecialCategory` (predefinida; la única etiqueta de OpenMetadata para datos con protección reforzada) |
| Naturaleza `NONE` | ninguna etiqueta `PersonalData` |
| Personal, confidencialidad ≥ `sensitiveFrom` (por defecto `MEDIUM`) | `PII.Sensitive` (predefinida) |
| Personal, confidencialidad < `sensitiveFrom` | `PII.NonSensitive` (predefinida) |
| No personal | `PII.None` (predefinida) |
| Confidencialidad | `Confidentiality.Public` / `.Low` / `.Medium` / `.High` |
| Integridad | `Integrity.Low` / `.Medium` / `.High` |
| Identificabilidad (solo campos) | `Identifiability.Identified` / `.Pseudonymized` / `.Anonymized` / `.Aggregated` |
| Secreto | `DataHandling.Secret` |

Los nombres de etiqueta `Personal`, `SpecialCategory`, `None`, `NonSensitive`, `Sensitive` y `Secret` son fijos; los
nombres de las clasificaciones se configuran con [`OpenMetadataMapping`](mapping.md).

### Etiquetas de una categoría

`tagsOf(DataCategory)` y las `associatedTags` de una etiqueta de categoría se calculan a partir de sus facetas, en
este orden:

1. `PersonalData.Personal` o `PersonalData.SpecialCategory` — solo si la categoría es personal;
2. `PII.None`, `PII.NonSensitive` o `PII.Sensitive`;
3. `Confidentiality.<nivel>`;
4. `Integrity.<nivel>`;
5. `DataHandling.Secret` — solo si la categoría es secreta.

`tagsOf(DataCategory)` antepone la etiqueta propia de la categoría. Por ejemplo, `user.authorization.password` da
`[DataCategory.user.authorization.password, PersonalData.Personal, PII.Sensitive, Confidentiality.High,
Integrity.High, DataHandling.Secret]`.

### Etiquetas de una columna

`tagsOf(FieldClassification)` usa los niveles efectivos y la identificabilidad **del campo**:

1. `DataCategory.<key>`;
2. si `field.isPersonal()` (categoría personal guardada como `IDENTIFIED` o `PSEUDONYMIZED`): la etiqueta
   `PersonalData` de la naturaleza y después `PII.Sensitive` o `PII.NonSensitive` según la confidencialidad del
   campo; si no, `PII.None` (un campo personal anonimizado o agregado recibe `PII.None` y ninguna etiqueta
   `PersonalData`);
3. `Confidentiality.<nivel del campo>`;
4. `Integrity.<nivel del campo>`;
5. `Identifiability.<valor>` — solo si el campo tiene identificabilidad (categoría personal);
6. `DataHandling.Secret` — solo si la categoría es secreta.

```java
@Category(value = Categories.USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED,
          confidentiality = Confidentiality.MEDIUM)
private String reference;
// tagsOf → [DataCategory.user.unique_id.pseudonymous, PersonalData.Personal, PII.Sensitive,
//           Confidentiality.Medium, Integrity.Medium, Identifiability.Pseudonymized]
```

## OmClassification

`io.github.devbpm.linneo.catalog.openmetadata.OmClassification` — `public record`, `@since 0.1.0`

```java
public record OmClassification(String name, String description, boolean mutuallyExclusive)
```

Una clasificación de OpenMetadata que hay que crear: el cuerpo de `PUT /api/v1/classifications`.
`mutuallyExclusive` indica que una entidad solo puede llevar una de sus etiquetas. El constructor lanza
`NullPointerException` si `name` o `description` son `null`.

| Método | Descripción |
|---|---|
| `String toJson()` | El cuerpo JSON de una petición `CreateClassification`. |

```json
{"name":"DataCategory","description":"What a column holds, from the Linneo data category taxonomy (fideslang default taxonomy plus extensions).","mutuallyExclusive":true}
```

## OmTag

`io.github.devbpm.linneo.catalog.openmetadata.OmTag` — `public record`, `@since 0.1.0`

```java
public record OmTag(String classification, String parent, String name, String displayName, String description,
                    List<String> associatedTags)
```

Una etiqueta de OpenMetadata que hay que crear: el cuerpo de `PUT /api/v1/tags`.

| Componente | Admite `null` | Descripción |
|---|---|---|
| `classification` | no | Nombre de la clasificación a la que pertenece. |
| `parent` | sí | Nombre completo de la etiqueta padre; `null` en la raíz de la clasificación. |
| `name` | no | El nombre de la etiqueta, un único segmento sin puntos. |
| `displayName` | no | El nombre legible. |
| `description` | no | Qué significa. |
| `associatedTags` | no | Nombres completos de las etiquetas que la acompañan, para que quien la aplique a mano reciba también esas. Un argumento `null` se convierte en una lista vacía; la lista se copia en una inmutable. |

El constructor lanza `NullPointerException` si `classification`, `name`, `displayName` o `description` son `null`.

| Método | Descripción |
|---|---|
| `String fullyQualifiedName()` | `parent` (o `classification` si no hay padre) + `.` + `name`, como `DataCategory.user.contact.email`. |
| `String toJson()` | El cuerpo JSON de una petición `CreateTag`. |

```json
{"classification":"DataCategory","parent":"DataCategory.user.contact","name":"email","displayName":"User Contact Email","description":"User's contact email address.","associatedTags":["PersonalData.Personal","PII.NonSensitive","Confidentiality.Low","Integrity.Medium"]}
```

## Salida JSON

`toJson()` escribe un objeto JSON compacto (sin espacios) con el escritor interno
`io.github.devbpm.linneo.internal.Json`, compartido con los exportadores de DataHub y Atlas (el paquete no se exporta),
para que la biblioteca no necesite ninguna dependencia JSON:

- **Orden de los campos** — `OmClassification`: `name`, `description`, `mutuallyExclusive`. `OmTag`:
  `classification`, `parent`, `name`, `displayName`, `description`, `associatedTags`.
- **Campos omitidos** — `OmTag` omite `parent` cuando es `null` y `associatedTags` cuando la lista está vacía.
- **Tipos** — cadenas, un booleano (`mutuallyExclusive`) y un array de cadenas (`associatedTags`).
- **Escapado** — `"` → `\"`, `\` → `\\`, salto de línea → `\n`, retorno de carro → `\r`, tabulador → `\t`, retroceso
  → `\b`, avance de página → `\f`, otros caracteres de control por debajo de U+0020 → `\u00XX`. El resto de
  caracteres, incluidos los no ASCII, se escriben tal cual (el resultado es un `String` de Java; codifícalo en UTF-8
  al enviarlo).

Véase también: [Mapeo](mapping.md) · [Resultados](../classify/results.md) ·
[Niveles de protección](../protection-levels.md) ·
[Comparativa de catálogos de datos](../README.md#comparativa-de-catálogos-de-datos) · curso:
[OpenMetadata](../../course/07-openmetadata.md)
