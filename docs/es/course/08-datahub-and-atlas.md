---
title: "8. DataHub y Apache Atlas"
parent: "Curso"
nav_order: 8
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DataHubCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DataHubMapping.java
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DhProposal.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasMapping.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasClassificationDef.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# Módulo 8 — DataHub y Apache Atlas

## Objetivos de aprendizaje

Al terminar este módulo serás capaz de:

- explicar cómo la taxonomía de Linneo se convierte en un glosario de negocio de DataHub (nodos, términos y sus
  relaciones) y en tipos de clasificación de Apache Atlas (supertipos y niveles de protección separados);
- generar las propuestas de DataHub y las definiciones de tipos de Atlas, y enviarlas con tu propio cliente HTTP;
- calcular los términos y las clasificaciones de cada columna a partir de su `FieldClassification`, y saber cómo
  aplicarlos;
- distinguir qué representa de forma distinta cada uno de los tres catálogos, y renombrar las raíces con
  `DataHubMapping` y `AtlasMapping`.

El módulo 7 no es imprescindible, pero este módulo lo usa a menudo como referencia.

## Una taxonomía, tres catálogos

Puede que la universidad no use OpenMetadata. [DataHub](https://datahub.com/) y
[Apache Atlas](https://atlas.apache.org/) son otros dos catálogos de código abierto, y Linneo tiene un exportador para
cada uno, junto al de OpenMetadata:

| Catálogo | En qué se convierte una categoría | Paquete |
|---|---|---|
| OpenMetadata | una etiqueta dentro de una clasificación | `io.github.devbpm.linneo.catalog.openmetadata` |
| DataHub | un **término del glosario** dentro de un árbol de **nodos del glosario** | `io.github.devbpm.linneo.catalog.datahub` |
| Apache Atlas | un **tipo de clasificación** que hereda del de su padre | `io.github.devbpm.linneo.catalog.atlas` |

Los tres funcionan igual: un objeto de catálogo inmutable construido a partir de una `Taxonomy`, una lista de lo que
hay que crear en orden de creación y un método que calcula las etiquetas de una columna a partir de su
`FieldClassification`. Ninguno hace llamadas de red. La referencia incluye una
[tabla comparativa](../reference/README.md#comparativa-de-catálogos-de-datos) completa.

Dos diferencias importan desde el principio:

- DataHub y Atlas no tienen clasificaciones `PersonalData` ni `PII` predefinidas que reutilizar, así que Linneo crea
  su propia raíz `PersonalData` y mantiene separadas las tres naturalezas: `Personal`, `SpecialCategory` y
  **`CriminalOffence`**. (OpenMetadata solo tiene `SpecialCategory` para los datos del art. 9 y del art. 10.) No hay
  ninguna etiqueta de sensibilidad `PII.*`.
- Los nombres de los niveles son los mismos en todos: `Confidentiality.High`, `Integrity.Medium`,
  `Identifiability.Aggregated`, `DataHandling.Secret`.

## DataHub: un glosario de negocio

### Nodos y términos

DataHub organiza un glosario en **nodos** (carpetas) que contienen **términos**. Linneo crea seis nodos raíz
—`PersonalData`, `Confidentiality`, `Integrity`, `Identifiability`, `DataHandling` y `DataCategory`— con los términos
de nivel en los cinco primeros y el árbol de categorías en el último.

Cada categoría es un término: `user.education.grade` es `urn:li:glossaryTerm:DataCategory.user.education.grade`. Una
categoría **con hijas** recibe además un nodo con el mismo identificador, y ese nodo contiene el **término propio**
de la categoría junto a sus hijas, de modo que el glosario se recorre como la taxonomía:

```text
DataCategory                                   (nodo)
└── DataCategory.user                          (nodo "User Data")
    ├── DataCategory.user                      (término "User Data")
    ├── DataCategory.user.education            (nodo "Education Data")
    │   ├── DataCategory.user.education        (término "Education Data")
    │   ├── DataCategory.user.education.grade  (término "Grade")
    │   ├── DataCategory.user.education.status (término "Academic Status")
    │   └── …
    └── …
```

Cada término de categoría está además **relacionado** con otros términos:

- `isRelatedTerms` (la relación «hereda de» de DataHub) apunta al término de su categoría padre: `Grade` hereda de
  `Education Data`;
- `relatedTerms` apunta a la protección que implica: `Grade` está relacionado con `PersonalData.Personal`,
  `Confidentiality.Medium` e `Integrity.High`.

Y cada término de categoría guarda su clave, su origen y su protección en **propiedades personalizadas**, para que en
DataHub se vean los datos que hay detrás del término. Los términos que vienen de fideslang se marcan como externos,
con fideslang como fuente; los de Linneo y los tuyos son internos.

### Las propuestas

```java
DataHubCatalog catalog = DataHubCatalog.of(Taxonomy.load());
List<DhProposal> proposals = catalog.proposals();
```

En DataHub se escribe **aspecto** a aspecto. Una `DhProposal` inserta o actualiza un aspecto de una entidad:
`entityType` (`glossaryNode` o `glossaryTerm`), `entityUrn`, `aspectName` y el `aspect` en JSON. Con la taxonomía
estándar, `proposals()` devuelve 289, en un orden que puedes enviar tal cual:

1. 28 `glossaryNodeInfo`: las seis raíces y después las 22 categorías con hijas, cada nodo detrás de su padre;
2. 138 `glossaryTermInfo`: los 15 términos de nivel y después las 123 categorías;
3. 123 `glossaryRelatedTerms`: las relaciones de cada término de categoría.

El `glossaryTermInfo` de `Grade`:

```json
{"customProperties":{"linneo.key":"user.education.grade","linneo.origin":"linneo","linneo.nature":"ORDINARY","linneo.confidentiality":"MEDIUM","linneo.integrity":"HIGH","linneo.secret":"false"},"name":"Grade","definition":"Marks, grades or assessment results obtained by the subject.","parentNode":"urn:li:glossaryNode:DataCategory.user.education","termSource":"INTERNAL"}
```

y su `glossaryRelatedTerms`:

```json
{"isRelatedTerms":["urn:li:glossaryTerm:DataCategory.user.education"],"relatedTerms":["urn:li:glossaryTerm:PersonalData.Personal","urn:li:glossaryTerm:Confidentiality.Medium","urn:li:glossaryTerm:Integrity.High"]}
```

`toJson()` envuelve el aspecto en el cuerpo de la acción `ingestProposal` de DataHub. Fíjate en que el aspecto viaja
como **cadena codificada en JSON** dentro de `aspect.value`, que es lo que espera la API:

```json
{"proposal":{"entityType":"glossaryTerm","entityUrn":"urn:li:glossaryTerm:Confidentiality.High","changeType":"UPSERT","aspectName":"glossaryTermInfo","aspect":{"value":"{\"customProperties\":{},\"name\":\"High\",\"definition\":\"Confidentiality level High.\",\"parentNode\":\"urn:li:glossaryNode:Confidentiality\",\"termSource\":\"INTERNAL\"}","contentType":"application/json"}}}
```

### Enviar las propuestas

Como con OpenMetadata, **Linneo no hace llamadas HTTP**. Un emisor mínimo con el `HttpClient` del JDK:

```java
package edu.example.university;

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.catalog.datahub.DataHubCatalog;
import io.github.devbpm.linneo.catalog.datahub.DhProposal;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class PublishToDataHub {

    public static void main(String[] args) throws IOException, InterruptedException {
        URI ingest = URI.create("https://datahub.example.edu:8080/aspects?action=ingestProposal");
        String token = System.getenv("DATAHUB_TOKEN");
        HttpClient http = HttpClient.newHttpClient();
        DataHubCatalog catalog = DataHubCatalog.of(Taxonomy.load());

        for (DhProposal proposal : catalog.proposals()) {   // nodos, luego términos, luego relaciones
            HttpRequest request = HttpRequest.newBuilder(ingest)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("X-RestLi-Protocol-Version", "2.0.0")
                    .POST(HttpRequest.BodyPublishers.ofString(proposal.toJson()))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IOException(proposal.entityUrn() + " " + proposal.aspectName() + " failed: "
                        + response.statusCode() + " " + response.body());
            }
        }
    }
}
```

La URL es la del servicio de metadatos de DataHub (GMS); detrás del frontend de DataHub suele ser
`/api/gms/aspects?action=ingestProposal`. Cada propuesta es un `UPSERT`, así que puedes volver a ejecutar el emisor
después de cambiar la taxonomía… pero cada vez sustituye el aspecto completo, así que no retoques a mano los términos
de Linneo en la interfaz de DataHub.

### Los términos de una columna

`termsOf(FieldClassification)` da los URN de los términos de una columna a partir de la clasificación efectiva **del
campo**:

```java
Taxonomy taxonomy = Taxonomy.load();
DataHubCatalog catalog = DataHubCatalog.of(taxonomy);
ClassClassification student = new Classifier(taxonomy).classify(Student.class);

Map<String, List<String>> termsByField = student.fields().stream()
        .collect(Collectors.toMap(FieldClassification::field, catalog::termsOf));
```

En la tabla cada URN aparece abreviado: todos empiezan por `urn:li:glossaryTerm:`.

| Campo | Términos |
|---|---|
| `Student.email` | `DataCategory.user.contact.email`, `PersonalData.Personal`, `Confidentiality.Low`, `Integrity.Medium`, `Identifiability.Identified` |
| `Student.disabilityDegree` | `DataCategory.user.health_and_medical.disability`, `PersonalData.SpecialCategory`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified` |
| `Student.analyticsId` | `DataCategory.user.unique_id.pseudonymous`, `PersonalData.Personal`, `Confidentiality.Low`, `Integrity.Medium`, `Identifiability.Pseudonymized` |
| `Student.counsellingNotes` | `DataCategory.user.content.private`, `PersonalData.Personal`, `Confidentiality.High`, `Integrity.Medium`, `Identifiability.Identified` |
| `Staff.passwordHash` | `DataCategory.user.authorization.password`, `PersonalData.Personal`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified`, `DataHandling.Secret` |
| `Staff.sexOffenceCertificateClear` | `DataCategory.user.criminal_history`, `PersonalData.CriminalOffence`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified` |
| `CourseStatistics.averageMark` | `DataCategory.user.education.grade`, `Confidentiality.Medium`, `Integrity.High`, `Identifiability.Aggregated` |
| `MenuGroup.code` | `DataCategory.system.configuration`, `Confidentiality.Low`, `Integrity.Medium` |

Las reglas son las del módulo 7 sin las etiquetas `PII.*`: `counsellingNotes` es `High` por el nivel subido en el
campo, `averageMark` no recibe ningún término `PersonalData` porque los valores agregados no son datos personales, y
los campos no personales no tienen término `Identifiability`.

**Asignar los términos a los campos de los datasets es tarea de tu integración.** Relaciona cada campo con su dataset
y su ruta de campo, y usa la API de DataHub que ya utilices. Con GraphQL, `addTerms` añade términos a un campo de un
dataset, indicado en `subResource`, sin quitar los demás:

```graphql
mutation {
  addTerms(input: {
    termUrns: ["urn:li:glossaryTerm:DataCategory.user.health_and_medical.disability",
               "urn:li:glossaryTerm:PersonalData.SpecialCategory",
               "urn:li:glossaryTerm:Confidentiality.High",
               "urn:li:glossaryTerm:Integrity.High",
               "urn:li:glossaryTerm:Identifiability.Identified"],
    resourceUrn: "urn:li:dataset:(urn:li:dataPlatform:postgres,university.public.student,PROD)",
    subResourceType: DATASET_FIELD,
    subResource: "disability_degree"
  })
}
```

## Apache Atlas: tipos de clasificación

### El árbol como herencia

Atlas tiene **clasificaciones**: tipos que se definen una vez y después se asignan a entidades como las columnas. Un
tipo de clasificación puede tener **supertipos**, y una entidad con una clasificación también coincide con sus
supertipos. Linneo convierte el árbol de categorías en esa herencia: `DataCategory.user.education.grade` tiene como
supertipo `DataCategory.user.education`, que tiene `DataCategory.user`, que tiene la raíz `DataCategory`. Por eso una
búsqueda de `DataCategory.user.education` encuentra todas las columnas con cualquier dato educativo.

Los niveles de protección son **clasificaciones separadas** (`PersonalData.SpecialCategory`, `Confidentiality.High`,
`DataHandling.Secret`…) que una columna lleva **junto a** su categoría. ¿Por qué no hacer de `Confidentiality.Medium`
un supertipo de `DataCategory.user.education`? Porque una categoría hija puede subir un nivel que implica su padre
—`user` es `Low`, `user.health_and_medical` es `High`— y la herencia de Atlas no se puede retirar más abajo en el
árbol. Mantener los niveles aparte también permite que las políticas de acceso basadas en etiquetas, como las de
Apache Ranger, actúen directamente sobre `Confidentiality.High` o `PersonalData.SpecialCategory`, sea cual sea la
categoría.

### Las definiciones de tipos

```java
AtlasCatalog atlas = AtlasCatalog.of(Taxonomy.load());
String typeDefs = atlas.typeDefsJson();
```

`classificationDefs()` enumera 144 tipos con la taxonomía estándar —seis raíces, 15 niveles, 123 categorías, cada
supertipo primero— y `typeDefsJson()` los reúne en un único cuerpo para `POST /api/atlas/v2/types/typedefs`. La
definición de `Grade`:

```json
{"name":"DataCategory.user.education.grade","description":"Grade: Marks, grades or assessment results obtained by the subject.","superTypes":["DataCategory.user.education"],"attributeDefs":[]}
```

Las clasificaciones de Atlas no tienen nombre visible, así que la descripción empieza por el nombre de la categoría.

### Las clasificaciones de una columna

`classificationsOf(FieldClassification)` da las mismas etiquetas que DataHub, como nombres simples: los
identificadores de los términos de DataHub sin `urn:li:glossaryTerm:`. `classificationsJson(field)` las escribe como
el cuerpo que las asigna a una columna:

```java
String body = atlas.classificationsJson(student.field("disabilityDegree").orElseThrow());
```

```json
[{"typeName":"DataCategory.user.health_and_medical.disability"},{"typeName":"PersonalData.SpecialCategory"},{"typeName":"Confidentiality.High"},{"typeName":"Integrity.High"},{"typeName":"Identifiability.Identified"}]
```

Un emisor para las dos peticiones:

```java
package edu.example.university;

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.catalog.atlas.AtlasCatalog;
import io.github.devbpm.linneo.classify.FieldClassification;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class PublishToAtlas {

    private static final URI API = URI.create("https://atlas.example.edu:21000/api/atlas/v2/");

    private final HttpClient http = HttpClient.newHttpClient();
    private final String authorization = "Basic " + Base64.getEncoder().encodeToString(
            (System.getenv("ATLAS_USER") + ":" + System.getenv("ATLAS_PASSWORD")).getBytes(StandardCharsets.UTF_8));
    private final AtlasCatalog catalog = AtlasCatalog.of(Taxonomy.load());

    /** Creates every classification type in one request. */
    void createTypes() throws IOException, InterruptedException {
        post(API.resolve("types/typedefs"), catalog.typeDefsJson());
    }

    /** Adds the classifications of a field to the column entity with the given guid. */
    void classify(String columnGuid, FieldClassification field) throws IOException, InterruptedException {
        post(API.resolve("entity/guid/" + columnGuid + "/classifications"), catalog.classificationsJson(field));
    }

    private void post(URI uri, String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException("POST " + uri + " failed: " + response.statusCode() + " " + response.body());
        }
    }
}
```

Tres cosas quedan para tu integración:

- **Encontrar la columna.** Linneo no conoce el `guid` de la entidad de la columna; búscalo en Atlas a partir del
  nombre cualificado de la columna.
- **Volver a ejecutarlo.** `POST /types/typedefs` crea tipos y rechaza los que ya existen: en ejecuciones posteriores
  envía solo las definiciones nuevas y actualiza las que hayan cambiado con `PUT`. Del mismo modo, Atlas rechaza una
  clasificación que la columna ya tiene.
- **La propagación.** El cuerpo no indica ningún `propagate`, así que Atlas aplica su valor por defecto: en una
  instalación estándar las clasificaciones fluyen por el linaje hacia las columnas derivadas. Una columna de
  estadísticas calculada a partir de `Grade.mark` llevaría entonces `PersonalData.Personal` e
  `Identifiability.Identified` junto a su propia `Identifiability.Aggregated`. Decide qué necesita tu linaje y añade
  `"propagate": false` a los objetos si la propagación no te conviene.

## Personalizar los nombres

`DataHubMapping` y `AtlasMapping` guardan los seis nombres raíz —`DataCategory`, `PersonalData`, `Confidentiality`,
`Integrity`, `Identifiability`, `DataHandling`— y tienen un método `with...` para cada uno:

```java
DataHubMapping spanish = DataHubMapping.defaults()
        .withCategories("CategoriaDato")
        .withConfidentiality("Confidencialidad");
DataHubCatalog custom = DataHubCatalog.of(taxonomy, spanish);

custom.termsOf(student.field("email").orElseThrow());
// [urn:li:glossaryTerm:CategoriaDato.user.contact.email, urn:li:glossaryTerm:PersonalData.Personal,
//  urn:li:glossaryTerm:Confidencialidad.Low, urn:li:glossaryTerm:Integrity.Medium,
//  urn:li:glossaryTerm:Identifiability.Identified]
```

`AtlasMapping` funciona igual y da `[CategoriaDato.user.contact.email, PersonalData.Personal, Confidencialidad.Low,
Integrity.Medium, Identifiability.Identified]`. Cada nombre debe empezar por una letra y contener solo letras,
dígitos y guiones bajos, para que siga siendo un único segmento de cada URN o nombre de clasificación; si no, el
mapeo lanza `IllegalArgumentException: Invalid DataHub glossary node name 'Data Handling'` (o
`Invalid Atlas classification name '…'`). A diferencia de `PersonalData` y `PII` en OpenMetadata, Linneo crea todas
las raíces, así que puedes renombrarlas todas libremente.

## Errores frecuentes

- **Esperar que Linneo llame a DataHub o a Atlas, o que asigne las etiquetas a las columnas.** Construye cuerpos y
  nombres; tu integración los envía y localiza el campo del dataset o la entidad de la columna.
- **Reordenar las propuestas de DataHub** (por ejemplo, ordenarlas por URN): un término puede llegar entonces antes
  que su nodo, o una relación antes que el término al que apunta. Envía `proposals()` en su orden.
- **Construir a mano el cuerpo de `ingestProposal` con el aspecto como objeto JSON.** La API lo quiere como cadena en
  `aspect.value`; `toJson()` ya lo hace.
- **Editar los términos de Linneo en la interfaz de DataHub.** La siguiente ejecución del emisor sustituye esos
  aspectos.
- **Asignar en Atlas solo la clasificación de la categoría.** `DataCategory.user.health_and_medical.disability` no
  lleva `Confidentiality.High` a través de sus supertipos; envía `classificationsJson(field)`, que enumera todas las
  etiquetas.
- **Enviar dos veces sin cambios los typedefs de Atlas.** Los tipos existentes se rechazan; envía solo los nuevos.
- **Olvidar la propagación de Atlas.** Las columnas derivadas pueden recibir las clasificaciones de datos personales
  de sus orígenes.
- **Calcular las etiquetas a partir de la categoría en lugar del campo** (`termsOf(field.category())`): pierdes los
  niveles subidos, la identificabilidad y la distinción agregado/anonimizado, como en el módulo 7.

## Ejercicios

**Ejercicio 8.1 — Predice.** Sin ejecutar código, escribe los términos de DataHub y las clasificaciones de Atlas de
`Staff.iban` y `Attachment.content`. Después compruébalo con `termsOf` y `classificationsOf`.

**Ejercicio 8.2 — Revisar antes de publicar.** Escribe las propuestas de DataHub en `datahub-proposals.jsonl` (un
`toJson()` por línea) y el cuerpo de Atlas en `atlas-typedefs.json`, para que el delegado de protección de datos
pueda revisarlos en una pull request. ¿Cuántas líneas tiene el primer fichero?

**Ejercicio 8.3 — La extensión en el glosario.** Con la `CampusTaxonomy` del módulo 6 registrada, ¿cuántas
propuestas devuelve `proposals()` y cuántos tipos devuelve `classificationDefs()`? ¿Aparece algún nodo nuevo del
glosario? ¿Qué propiedades personalizadas lleva el término `DataCategory.user.education.lab_notebook`?

**Ejercicio 8.4 — Categorías especiales en tres catálogos.** Entre los campos de `Student`, `Staff` y `Attachment`,
¿cuáles llevan `PersonalData.SpecialCategory` en OpenMetadata y cuáles en DataHub y Atlas? Explica la diferencia.

**Ejercicio 8.5 — Buscar en Atlas.** El delegado de protección de datos busca en Atlas la clasificación
`DataCategory.user.health_and_medical`. ¿Qué columnas del modelo de la universidad encuentra, y por qué?

## Cuestionario de autoevaluación

1. En DataHub, ¿en qué se convierte la categoría `user.contact`?
2. ¿Qué relación de DataHub une el término de `user.contact.email` con el de `user.contact`, y cuál lo une con
   `Confidentiality.Low`?
3. ¿Por qué el aspecto es una cadena en el cuerpo que escribe `DhProposal.toJson()`?
4. ¿Por qué los niveles de protección de Atlas no son supertipos de las clasificaciones de categoría?
5. ¿Qué etiqueta da `user.criminal_history` en Atlas, y qué etiqueta en OpenMetadata?
6. ¿Quién asigna los términos de DataHub a los campos de un dataset?

## Para saber más

- [Referencia — Catálogo de DataHub](../reference/datahub/catalog.md)
- [Referencia — Mapeo de DataHub](../reference/datahub/mapping.md)
- [Referencia — Catálogo de Apache Atlas](../reference/atlas/catalog.md)
- [Referencia — Mapeo de Apache Atlas](../reference/atlas/mapping.md)
- [Referencia — Comparativa de catálogos de datos](../reference/README.md#comparativa-de-catálogos-de-datos)

---

← Anterior: [Módulo 7 — OpenMetadata](07-openmetadata.md) · Siguiente:
[Módulo 9 — Actividades de tratamiento y el registro](09-processing-activities.md) →

<details markdown="block">
<summary><strong>Soluciones</strong></summary>

**Ejercicio 8.1**

`Staff.iban` (`user.financial.bank_account`: `ORDINARY`, `MEDIUM`, `HIGH`): los términos de DataHub
`urn:li:glossaryTerm:` + `DataCategory.user.financial.bank_account`, `PersonalData.Personal`, `Confidentiality.Medium`,
`Integrity.High`, `Identifiability.Identified`; las clasificaciones de Atlas son los mismos nombres sin el prefijo.

`Attachment.content` (por fila, con techo `user.health_and_medical`): `DataCategory.user.health_and_medical`,
`PersonalData.SpecialCategory`, `Confidentiality.High`, `Integrity.High`, `Identifiability.Identified`, con el
prefijo `urn:li:glossaryTerm:` en DataHub.

**Ejercicio 8.2**

```java
DataHubCatalog dataHub = DataHubCatalog.of(Taxonomy.load());
Files.write(Path.of("datahub-proposals.jsonl"),
        dataHub.proposals().stream().map(DhProposal::toJson).toList());
Files.writeString(Path.of("atlas-typedefs.json"), AtlasCatalog.of(Taxonomy.load()).typeDefsJson());
```

Con la taxonomía estándar, el primer fichero tiene 289 líneas: 28 nodos, 138 términos y 123 aspectos de relaciones.

**Ejercicio 8.3**

293 propuestas y 146 tipos. La extensión añade dos categorías, `user.education.lab_notebook` y
`user.education.lab_journal`: cada una aporta un `glossaryTermInfo` y un `glossaryRelatedTerms` (2 × 2 = 4
propuestas más) y un tipo de clasificación. No aparece ningún nodo: las dos son hojas, y `user.education` ya tenía
nodo porque ya tenía hijas. El término del cuaderno de laboratorio lleva `linneo.key`
`user.education.lab_notebook`, `linneo.origin` `campus`, `linneo.nature` `ORDINARY`, `linneo.confidentiality` `MEDIUM`, `linneo.integrity` `HIGH`,
`linneo.secret` `false` y el atributo de la extensión `retention` `permanent`. Su `termSource` es `INTERNAL`.

**Ejercicio 8.4**

OpenMetadata: `Student.disabilityDegree`, `Staff.sexOffenceCertificateClear` y `Attachment.content`. DataHub y Atlas:
solo `Student.disabilityDegree` y `Attachment.content`; `Staff.sexOffenceCertificateClear` lleva
`PersonalData.CriminalOffence`. La clasificación predefinida `PersonalData` de OpenMetadata no tiene etiqueta para
los datos penales, así que allí Linneo los asigna a `SpecialCategory`; en DataHub y Atlas Linneo crea él mismo las
etiquetas `PersonalData` y mantiene separadas las tres naturalezas.

**Ejercicio 8.5**

`Student.disabilityDegree` y `Attachment.content`. `Attachment.content` lleva directamente
`DataCategory.user.health_and_medical` (su techo); `disabilityDegree` lleva
`DataCategory.user.health_and_medical.disability`, cuyo supertipo es `DataCategory.user.health_and_medical`, así que
la búsqueda también la encuentra.

**Cuestionario**

1. En un nodo del glosario `urn:li:glossaryNode:DataCategory.user.contact` (porque tiene hijas) y un término
   `urn:li:glossaryTerm:DataCategory.user.contact` dentro de ese nodo.
2. `isRelatedTerms` hacia el padre; `relatedTerms` hacia los términos de protección.
3. Porque la API `ingestProposal` recibe el aspecto codificado en JSON en `aspect.value`, con su `contentType`.
4. Porque una categoría hija puede subir un nivel que implica su padre, y la herencia de Atlas no se puede deshacer
   más abajo; y para que las políticas basadas en etiquetas puedan actuar directamente sobre los niveles.
5. `PersonalData.CriminalOffence` en Atlas; `PersonalData.SpecialCategory` en OpenMetadata.
6. Tu integración (por ejemplo, con la mutación GraphQL `addTerms`); Linneo solo calcula los términos.

</details>
