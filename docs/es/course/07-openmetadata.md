---
title: "7. OpenMetadata"
parent: "Curso"
nav_order: 7
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OpenMetadataCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OpenMetadataMapping.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OmTag.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OmClassification.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# Módulo 7 — OpenMetadata

## Objetivos de aprendizaje

Al terminar este módulo serás capaz de:

- explicar cómo se corresponde la taxonomía de Linneo con las clasificaciones y etiquetas de OpenMetadata,
  incluidas las clasificaciones predefinidas `PersonalData` y `PII`;
- generar los cuerpos de las peticiones para crear las clasificaciones y etiquetas, y enviarlos con tu propio
  cliente HTTP;
- calcular las etiquetas de cada columna a partir de su `FieldClassification`;
- renombrar las clasificaciones y mover el umbral de sensibilidad con `OpenMetadataMapping`.

## Por qué un catálogo

La clasificación vive en el código; los datos viven en bases de datos, almacenes y cuadros de mando que los analistas
consultan en un catálogo de datos. [OpenMetadata](https://open-metadata.org) es un catálogo de código abierto que
organiza las etiquetas en **clasificaciones**. Publicar ahí la taxonomía de Linneo significa que quien encuentre la
tabla `student` en el catálogo verá, en la columna `disability_degree`, que es un dato de categoría especial con
confidencialidad alta: la misma respuesta que da el código.

OpenMetadata incluye dos clasificaciones predefinidas que Linneo **reutiliza y nunca crea**:

- `PersonalData`, con las etiquetas `Personal` y `SpecialCategory`;
- `PII`, con las etiquetas `None`, `NonSensitive` y `Sensitive`.

## La correspondencia

| Linneo | OpenMetadata |
|---|---|
| Categoría de datos `user.contact.email` | Etiqueta anidada `DataCategory.user.contact.email` en la clasificación `DataCategory` (mutuamente excluyente) |
| Naturaleza `ORDINARY` | `PersonalData.Personal` |
| Naturaleza `SPECIAL_CATEGORY` o `CRIMINAL_OFFENCE` | `PersonalData.SpecialCategory`, la única etiqueta de OpenMetadata para datos con protección reforzada |
| Dato personal con confidencialidad inferior a `MEDIUM` | `PII.NonSensitive` |
| Dato personal con confidencialidad `MEDIUM` o superior | `PII.Sensitive` (umbral configurable) |
| No personal | `PII.None` |
| Confidencialidad, integridad, identificabilidad | `Confidentiality.*`, `Integrity.*`, `Identifiability.*` (por ejemplo, `Confidentiality.High`) |
| Secreto | `DataHandling.Secret` |
| Protección implícita en una categoría | `associatedTags` de la etiqueta de la categoría |

## El catálogo

```java
OpenMetadataCatalog catalog = OpenMetadataCatalog.of(Taxonomy.load());
```

`OpenMetadataCatalog` es inmutable y seguro entre hilos. Usa la misma taxonomía que el resto de la aplicación para que
también se publiquen los términos y niveles más estrictos de tu extensión (módulo 6).

### Clasificaciones

`classifications()` devuelve las cinco clasificaciones que crea Linneo, en orden de creación. `toJson()` da el cuerpo
de una petición `CreateClassification` (`PUT /api/v1/classifications`):

```json
{"name":"DataCategory","description":"What a column holds, from the Linneo data category taxonomy (fideslang default taxonomy plus extensions).","mutuallyExclusive":true}
{"name":"Confidentiality","description":"Harm that disclosing the data would cause.","mutuallyExclusive":true}
{"name":"Integrity","description":"Harm that an unwanted change to the data would cause.","mutuallyExclusive":true}
{"name":"Identifiability","description":"How directly a personal data value points to the person.","mutuallyExclusive":true}
{"name":"DataHandling","description":"Handling rules that apply to the data.","mutuallyExclusive":false}
```

(Las descripciones las genera Linneo en inglés.)

### Etiquetas

`tags()` devuelve las etiquetas que hay que crear, en un orden que puedes enviar tal cual: primero las etiquetas de
nivel (cuatro de confidencialidad, tres de integridad, cuatro de identificabilidad), después `DataHandling.Secret` y
por último una etiqueta por categoría de datos, con **cada madre antes que sus hijas**. Para la taxonomía estándar son
4 + 3 + 4 + 1 + 125 = 137 etiquetas.

Cada `OmTag` tiene una `classification`, un `parent` (el nombre completo de la etiqueta madre, o `null` en la raíz),
un `name` (un único segmento, el último de la clave), un `displayName` (el nombre de la categoría), una `description`
y sus `associatedTags`. `fullyQualifiedName()` da el nombre con el que se aplica y `toJson()` el cuerpo de una
petición `CreateTag` (`PUT /api/v1/tags`). La etiqueta de `user.education.grade`:

```json
{"classification":"DataCategory","parent":"DataCategory.user.education","name":"grade","displayName":"Grade","description":"Marks, grades or assessment results obtained by the subject.","associatedTags":["PersonalData.Personal","PII.Sensitive","Confidentiality.Medium","Integrity.High"]}
```

Las `associatedTags` llevan la protección que implica la categoría, para que quien aplique a mano la etiqueta de la
categoría en la interfaz de OpenMetadata obtenga también las etiquetas de protección adecuadas. En una categoría
obsoleta, la descripción termina con `Deprecated: use <sustituta>.` (o solo `Deprecated.`).

## Enviar los cuerpos

**Linneo no hace llamadas HTTP.** Produce los cuerpos; tú los envías con el cliente que ya uses. Una versión mínima
con el `HttpClient` del JDK:

```java
package edu.example.university;

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.catalog.openmetadata.OmClassification;
import io.github.devbpm.linneo.catalog.openmetadata.OmTag;
import io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataCatalog;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class PublishToOpenMetadata {

    public static void main(String[] args) throws IOException, InterruptedException {
        URI api = URI.create("https://openmetadata.example.edu/api/v1/");
        String token = System.getenv("OPENMETADATA_TOKEN");
        HttpClient http = HttpClient.newHttpClient();
        OpenMetadataCatalog catalog = OpenMetadataCatalog.of(Taxonomy.load());

        for (OmClassification classification : catalog.classifications()) {
            put(http, api.resolve("classifications"), token, classification.toJson());
        }
        for (OmTag tag : catalog.tags()) {              // las madres siempre van antes que sus hijas
            put(http, api.resolve("tags"), token, tag.toJson());
        }
    }

    private static void put(HttpClient http, URI uri, String token, String json)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException("PUT " + uri + " falló: " + response.statusCode() + " " + response.body());
        }
    }
}
```

Los cuerpos siguen los esquemas `CreateClassification` y `CreateTag` de OpenMetadata, incluidos los campos `parent`
y `associatedTags`; compruébalos con la versión de OpenMetadata que uses. Las clasificaciones deben existir antes que
sus etiquetas, y las etiquetas madre antes que sus hijas: el orden de `classifications()` y `tags()` garantiza ambas
cosas.

## Las etiquetas de una columna

`tagsOf(FieldClassification)` da las etiquetas, con nombre completo, de una columna, calculadas a partir de la
clasificación efectiva del **campo**: su categoría, si el valor almacenado es personal, los niveles propios del
campo, su identificabilidad y la marca de secreto. Para `Student`:

```java
Taxonomy taxonomy = Taxonomy.load();
OpenMetadataCatalog catalog = OpenMetadataCatalog.of(taxonomy);
ClassClassification student = new Classifier(taxonomy).classify(Student.class);

Map<String, List<String>> tagsByField = student.fields().stream()
        .collect(Collectors.toMap(FieldClassification::field, catalog::tagsOf));
```

| Campo | Etiquetas |
|---|---|
| `email` | `DataCategory.user.contact.email`, `PersonalData.Personal`, `PII.NonSensitive`, `Confidentiality.Low`, `Integrity.Medium`, `Identifiability.Identified` |
| `disabilityDegree` | `DataCategory.user.health_and_medical.disability`, `PersonalData.SpecialCategory`, `PII.Sensitive`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified` |
| `analyticsId` | `DataCategory.user.unique_id.pseudonymous`, `PersonalData.Personal`, `PII.NonSensitive`, `Confidentiality.Low`, `Integrity.Medium`, `Identifiability.Pseudonymized` |
| `counsellingNotes` | `DataCategory.user.content.private`, `PersonalData.Personal`, `PII.Sensitive`, `Confidentiality.High`, `Integrity.Medium`, `Identifiability.Identified` |

Y en otras partes del modelo:

| Campo | Etiquetas |
|---|---|
| `Staff.passwordHash` | `DataCategory.user.authorization.password`, `PersonalData.Personal`, `PII.Sensitive`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified`, `DataHandling.Secret` |
| `Staff.sexOffenceCertificateClear` | `DataCategory.user.criminal_history`, `PersonalData.SpecialCategory`, `PII.Sensitive`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified` |
| `CourseStatistics.averageMark` | `DataCategory.user.education.grade`, `PII.None`, `Confidentiality.Medium`, `Integrity.High`, `Identifiability.Aggregated` |
| `MenuGroup.code` | `DataCategory.system.configuration`, `PII.None`, `Confidentiality.Low`, `Integrity.Medium` |

Observa que:

- `counsellingNotes` es `PII.Sensitive` y `Confidentiality.High` por el nivel subido **en el campo**; la categoría por
  sí sola daría `Medium`.
- `averageMark` conserva la etiqueta de su categoría, pero es `PII.None` y no recibe etiqueta `PersonalData`: los
  valores agregados no son datos personales.
- Los campos no personales no tienen etiqueta `Identifiability`.

Existe también `tagsOf(DataCategory)`: la etiqueta de la categoría seguida de las que implica (la misma lista que sus
`associatedTags`, sin identificabilidad). Úsala cuando solo conozcas la categoría y no el campo.

Aplicar las etiquetas a las columnas de tus tablas es tarea de tu integración: relaciona cada campo con su tabla y su
columna (según las reglas de nombrado de tu persistencia) y usa la API de OpenMetadata que ya utilices para los
metadatos de tablas.

## Personalizar los nombres

`OpenMetadataMapping` contiene los nombres de las clasificaciones y el umbral de sensibilidad. `defaults()` es
`DataCategory`, `PersonalData`, `PII`, `Confidentiality`, `Integrity`, `Identifiability`, `DataHandling`, con los
datos personales sensibles a partir de `MEDIUM`. Tres métodos `with...` cubren los cambios habituales:

```java
OpenMetadataMapping spanish = OpenMetadataMapping.defaults()
        .withCategories("CategoriaDato")
        .withConfidentiality("Confidencialidad")
        .withSensitiveFrom(Confidentiality.LOW);
OpenMetadataCatalog custom = OpenMetadataCatalog.of(taxonomy, spanish);

custom.tagsOf(student.field("email").orElseThrow());
// [CategoriaDato.user.contact.email, PersonalData.Personal, PII.Sensitive, Confidencialidad.Low,
//  Integrity.Medium, Identifiability.Identified]
```

Con `sensitiveFrom` bajado a `LOW`, el correo del estudiante pasa a ser `PII.Sensitive`. Para los demás nombres, usa
el constructor del record:

```java
OpenMetadataMapping full = new OpenMetadataMapping("CategoriaDato", "PersonalData", "PII",
        "Confidencialidad", "Integridad", "Identificabilidad", "Tratamiento", Confidentiality.MEDIUM);
```

`personalData` y `pii` nombran las clasificaciones **predefinidas** de OpenMetadata: cámbialos solo si tu instalación
de OpenMetadata usa otros nombres, porque Linneo nunca las crea. Todos los nombres deben ser no vacíos y no contener
puntos; si no, el mapping lanza `IllegalArgumentException: Invalid OpenMetadata classification name 'Data.Category'`.

## Errores frecuentes

- **Esperar que Linneo llame a OpenMetadata.** Solo construye los cuerpos y los nombres de las etiquetas.
- **Enviar las etiquetas en otro orden** (por ejemplo, ordenadas alfabéticamente por nombre completo): una hija antes
  que su madre falla.
- **Publicar `Taxonomy.standard()`** cuando la aplicación usa extensiones; al catálogo le faltarán tus términos.
- **Calcular las etiquetas de columna a partir de la categoría en lugar del campo** (`tagsOf(field.category())`):
  pierdes los niveles subidos en el campo, su identificabilidad y la distinción agregado/anonimizado.
- **Renombrar `PersonalData` o `PII`** en el mapping sin tener esas clasificaciones en OpenMetadata.
- **Leer `PersonalData.SpecialCategory` como «solo art. 9».** Los datos penales (art. 10) también se etiquetan
  `SpecialCategory`; la etiqueta `DataCategory` los distingue.

## Ejercicios

**Ejercicio 7.1 — Predice.** Sin ejecutar código, escribe las etiquetas de `Staff.iban` y `Attachment.content`.
Después compruébalo con `tagsOf`.

**Ejercicio 7.2 — Revisar antes de publicar.** En lugar de enviar los cuerpos, escribe cada uno en un fichero
(`classifications.jsonl` y `tags.jsonl`, un objeto JSON por línea) para que el delegado de protección de datos pueda
revisarlos en una pull request.

**Ejercicio 7.3 — Umbral.** Con el mapping por defecto, ¿qué campos personales de `Student` son `PII.NonSensitive`?
¿Y con `withSensitiveFrom(Confidentiality.HIGH)`?

**Ejercicio 7.4 — La extensión en el catálogo.** Con la `CampusTaxonomy` del módulo 6 registrada, ¿cuántas
etiquetas devuelve `catalog.tags()`? ¿Cuál es la `description` de la etiqueta
`DataCategory.user.education.lab_journal`?

## Cuestionario de autoevaluación

1. ¿Qué dos clasificaciones de OpenMetadata reutiliza Linneo sin crearlas nunca?
2. ¿Qué etiqueta `PersonalData` reciben los datos penales?
3. ¿Qué contiene `associatedTags` en la etiqueta de una categoría?
4. ¿Por qué se crean las etiquetas de nivel antes que las de categoría?
5. ¿Qué etiqueta `PII` recibe, con el mapping por defecto, un campo seudonimizado con confidencialidad `LOW`?

## Para saber más

- [Referencia — Catálogo de OpenMetadata](../reference/openmetadata/catalog.md)
- [Referencia — Mapping de OpenMetadata](../reference/openmetadata/mapping.md)

---

← Anterior: [Módulo 6 — Extender la taxonomía](06-extending-the-taxonomy.md) · Siguiente:
[Módulo 8 — DataHub y Apache Atlas](08-datahub-and-atlas.md) →

<details markdown="block">
<summary><strong>Soluciones</strong></summary>

**Ejercicio 7.1**

`Staff.iban` (`user.financial.bank_account`: `ORDINARY`, `MEDIUM`, `HIGH`):
`DataCategory.user.financial.bank_account`, `PersonalData.Personal`, `PII.Sensitive`, `Confidentiality.Medium`,
`Integrity.High`, `Identifiability.Identified`.

`Attachment.content` (por fila, techo `user.health_and_medical`):
`DataCategory.user.health_and_medical`, `PersonalData.SpecialCategory`, `PII.Sensitive`, `Confidentiality.High`,
`Integrity.High`, `Identifiability.Identified`.

**Ejercicio 7.2**

```java
OpenMetadataCatalog catalog = OpenMetadataCatalog.of(Taxonomy.load());
Files.write(Path.of("classifications.jsonl"),
        catalog.classifications().stream().map(OmClassification::toJson).toList());
Files.write(Path.of("tags.jsonl"),
        catalog.tags().stream().map(OmTag::toJson).toList());
```

**Ejercicio 7.3**

Por defecto (`MEDIUM`): `createdBy`, `id`, `firstName`, `lastName`, `email` y `analyticsId` son `PII.NonSensitive`
(confidencialidad `LOW`); el resto son `PII.Sensitive`. Con `HIGH`: solo `disabilityDegree` y `counsellingNotes` son
`PII.Sensitive`; los demás campos personales son `PII.NonSensitive`.

**Ejercicio 7.4**

La extensión añade dos categorías (`user.education.lab_notebook` y `user.education.lab_journal`), así que son
137 + 2 = 139 etiquetas. (Los interesados no se publican como etiquetas.) La descripción es
`Nombre anterior de la clave del cuaderno de laboratorio. Deprecated: use user.education.lab_notebook.`

**Cuestionario**

1. `PersonalData` y `PII`.
2. `PersonalData.SpecialCategory`.
3. La protección que implica la categoría: `PersonalData.*` (si es personal), `PII.*`, `Confidentiality.*`,
   `Integrity.*` y, en los secretos, `DataHandling.Secret`.
4. Porque las etiquetas de categoría las referencian en `associatedTags`, y las madres deben existir antes que sus
   hijas.
5. `PII.NonSensitive` (personal, por debajo de `MEDIUM`).

</details>
