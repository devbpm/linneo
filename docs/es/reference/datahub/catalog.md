---
title: "Catálogo de DataHub"
parent: "Referencia"
nav_order: 15
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DataHubCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DhProposal.java
  - src/main/java/io/github/devbpm/linneo/internal/Json.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# Catálogo de DataHub

El paquete `io.github.devbpm.linneo.catalog.datahub` traduce una [`Taxonomy`](../taxonomy.md) a un **glosario de
negocio** de [DataHub](https://datahub.com/) (nodos y términos del glosario) y calcula los términos de cada campo clasificado.
**No hace llamadas de red**: envía cada propuesta con el cliente HTTP que ya uses a
`POST /aspects?action=ingestProposal` del servicio de metadatos de DataHub (GMS) y después añade los términos a los
campos de cada dataset. Los aspectos siguen los esquemas `glossaryNodeInfo`, `glossaryTermInfo` y
`glossaryRelatedTerms` de DataHub; compruébalos con la versión de DataHub que utilices.

Cómo se compara este exportador con los de OpenMetadata y Atlas: consulta la
[comparativa de catálogos de datos](../README.md#comparativa-de-catálogos-de-datos).

## DataHubCatalog

`io.github.devbpm.linneo.catalog.datahub.DataHubCatalog` — `public final class`, `@since 0.1.0`

Inmutable y seguro entre hilos. No tiene constructor público.

| Método | Devuelve | Descripción |
|---|---|---|
| `static DataHubCatalog of(Taxonomy taxonomy)` | catálogo | Con los [nombres de nodo por defecto](mapping.md) (`DataHubMapping.defaults()`). |
| `static DataHubCatalog of(Taxonomy taxonomy, DataHubMapping mapping)` | catálogo | Con los nombres de nodo indicados. `NullPointerException` si algún argumento es `null`. |
| `List<DhProposal> proposals()` | lista nueva y modificable | Las propuestas que crean el glosario, en el orden en que hay que enviarlas (consulta el [orden de las propuestas](#orden-de-las-propuestas)). |
| `String termOf(DataCategory category)` | `String` | El URN del término de la categoría, como `urn:li:glossaryTerm:DataCategory.user.contact.email`. |
| `List<String> termsOf(DataCategory category)` | lista | El término propio de la categoría seguido de los términos de la protección que implica (sus `relatedTerms`). |
| `List<String> termsOf(FieldClassification field)` | lista | Los términos de un campo clasificado: su categoría, su naturaleza jurídica si es dato personal y la protección efectiva del campo. |

```java
Taxonomy taxonomy = Taxonomy.load();
DataHubCatalog catalog = DataHubCatalog.of(taxonomy);

for (DhProposal proposal : catalog.proposals()) {
    String body = proposal.toJson();   // POST /aspects?action=ingestProposal, en este orden
}

ClassClassification person = new Classifier(taxonomy).classify(Person.class);
for (FieldClassification field : person.fields()) {
    List<String> fieldTerms = catalog.termsOf(field);   // para el campo del dataset que corresponde al campo
}
```

## Estructura del glosario

Con el mapeo por defecto, el glosario tiene seis nodos raíz:

| Nodo | `name` | `definition` | Contiene |
|---|---|---|---|
| `PersonalData` | `Personal Data` | Legal nature of personal data under the GDPR. | los términos `Personal`, `SpecialCategory`, `CriminalOffence` |
| `Confidentiality` | `Confidentiality` | Harm that disclosing the data would cause. | los términos `Public`, `Low`, `Medium`, `High` |
| `Integrity` | `Integrity` | Harm that an unwanted change to the data would cause. | los términos `Low`, `Medium`, `High` |
| `Identifiability` | `Identifiability` | How directly a personal data value points to the person. | los términos `Identified`, `Pseudonymized`, `Anonymized`, `Aggregated` |
| `DataHandling` | `Data Handling` | Handling rules that apply to the data. | el término `Secret` |
| `DataCategory` | `Data Category` | What a field holds, from the Linneo data category taxonomy (fideslang default taxonomy plus extensions). | el árbol de categorías |

Los nombres y las definiciones se generan en inglés.

### Categorías: nodos y términos que reflejan el árbol

**Cada categoría es un término del glosario** cuyo URN es el nombre del nodo más la clave: `user.contact.email` es
`urn:li:glossaryTerm:DataCategory.user.contact.email`.

**Cada categoría con hijas recibe además un nodo del glosario** con el mismo nombre, la misma definición y el mismo
identificador: `urn:li:glossaryNode:DataCategory.user.contact`. Ese nodo contiene el **término propio** de la
categoría y los nodos y términos de sus hijas, de modo que el glosario se recorre como el árbol de la taxonomía. Una
categoría sin hijas (una hoja) es solo un término dentro del nodo de su padre, y el nodo de una categoría raíz está
dentro del nodo `DataCategory`:

```text
DataCategory                                  (nodo)
└── DataCategory.user                         (nodo "User Data")
    ├── DataCategory.user                     (término "User Data")
    └── DataCategory.user.contact             (nodo "Contact Data")
        ├── DataCategory.user.contact         (término "Contact Data")
        ├── DataCategory.user.contact.email   (término "User Contact Email")
        ├── DataCategory.user.contact.address (nodo "User Contact Address")
        │   └── …
        └── …
```

Un nodo y un término pueden compartir identificador porque son tipos de entidad distintos (`glossaryNode` y
`glossaryTerm`). En la taxonomía estándar, 22 de las 123 categorías tienen hijas, así que el glosario tiene
6 + 22 = 28 nodos y 15 + 123 = 138 términos.

### Relaciones: `isRelatedTerms` y `relatedTerms`

Cada término de categoría recibe un aspecto `glossaryRelatedTerms`:

- `isRelatedTerms` (la relación *Inherits*, «es un», de DataHub): el término de su **categoría padre**. Las
  categorías raíz (`user`, `system`…) no tienen, y entonces el miembro se omite.
- `relatedTerms` (la relación *Related to* de DataHub): los términos de la **protección que implica la categoría**,
  en este orden:
  1. `PersonalData.Personal`, `PersonalData.SpecialCategory` o `PersonalData.CriminalOffence` — solo si la
     categoría es personal;
  2. `Confidentiality.<nivel>`;
  3. `Integrity.<nivel>`;
  4. `DataHandling.Secret` — solo si la categoría es secreta.

```json
{"isRelatedTerms":["urn:li:glossaryTerm:DataCategory.user.contact"],"relatedTerms":["urn:li:glossaryTerm:PersonalData.Personal","urn:li:glossaryTerm:Confidentiality.Low","urn:li:glossaryTerm:Integrity.Medium"]}
```

Estas relaciones documentan la taxonomía en el glosario; no etiquetan nada. Los términos de un campo los enumera
siempre de forma explícita [`termsOf(field)`](#términos-de-un-campo).

### Información del término: definición, propiedades personalizadas y origen

El `glossaryTermInfo` de un **término de categoría** contiene:

| Miembro | Valor |
|---|---|
| `customProperties` | `linneo.key`, `linneo.origin`, `linneo.nature`, `linneo.confidentiality`, `linneo.integrity` (nombres del enumerado, como `MEDIUM`), `linneo.secret` (`"true"` o `"false"`) y después los [atributos](../terms.md#datacategory) de la categoría ordenados por clave (un atributo con uno de los nombres `linneo.*` se ignora: no puede sustituir ese valor), de modo que la salida es la misma en cada ejecución |
| `name` | el nombre de la categoría |
| `definition` | la descripción de la categoría, más ` Deprecated.` o ` Deprecated: use <key>.` si está obsoleta |
| `parentNode` | el nodo propio de la categoría si tiene hijas; si no, el nodo de su padre (`DataCategory` para una hoja raíz) |
| `termSource` | `EXTERNAL` para un término cuyo origen es `fideslang`, con `sourceRef` `fideslang` y `sourceUrl` `https://github.com/ethyca/fideslang`; `INTERNAL` para cualquier otro origen (`linneo`, el nombre de una extensión, `application`) |

```json
{"customProperties":{"linneo.key":"user.contact.email","linneo.origin":"fideslang","linneo.nature":"ORDINARY","linneo.confidentiality":"LOW","linneo.integrity":"MEDIUM","linneo.secret":"false"},"name":"User Contact Email","definition":"User's contact email address.","parentNode":"urn:li:glossaryNode:DataCategory.user.contact","termSource":"EXTERNAL","sourceRef":"fideslang","sourceUrl":"https://github.com/ethyca/fideslang"}
```

```json
{"customProperties":{"linneo.key":"user.education.grade","linneo.origin":"linneo","linneo.nature":"ORDINARY","linneo.confidentiality":"MEDIUM","linneo.integrity":"HIGH","linneo.secret":"false"},"name":"Grade","definition":"Marks, grades or assessment results obtained by the subject.","parentNode":"urn:li:glossaryNode:DataCategory.user.education","termSource":"INTERNAL"}
```

Un **término de nivel** (`Confidentiality.High`, `PersonalData.SpecialCategory`…) tiene `customProperties` vacías,
el nivel como `name`, `termSource` `INTERNAL` y ninguna relación:

| Términos | Nodo | `definition` |
|---|---|---|
| `Personal`, `SpecialCategory`, `CriminalOffence` | `PersonalData` | `Personal data (GDPR article 4.1).`, `Special category of personal data (GDPR article 9).`, `Personal data relating to criminal convictions and offences (GDPR article 10).` |
| `Public`, `Low`, `Medium`, `High` | `Confidentiality` | `Confidentiality level <Level>.` |
| `Low`, `Medium`, `High` | `Integrity` | `Integrity level <Level>.` |
| `Identified`, `Pseudonymized`, `Anonymized`, `Aggregated` | `Identifiability` | `Personal data that is <value>.` |
| `Secret` | `DataHandling` | `A secret that grants access: it must never be read back, exported or published, only verified.` |

Un **nodo** tiene `customProperties` vacías, un `name`, una `definition` y, salvo los seis nodos raíz, un
`parentNode`:

```json
{"customProperties":{},"name":"Contact Data","definition":"Contact data collected about a user.","parentNode":"urn:li:glossaryNode:DataCategory.user"}
```

### Orden de las propuestas

`proposals()` devuelve, en este orden (289 propuestas con la taxonomía estándar):

1. un `glossaryNodeInfo` para cada uno de los seis nodos raíz y después para cada categoría con hijas, en orden de
   clave, de modo que cada nodo va detrás de su nodo padre (28);
2. un `glossaryTermInfo` para cada término de nivel (15) y después para cada categoría, en orden de clave (123);
3. un `glossaryRelatedTerms` para cada categoría, en orden de clave (123).

Envíalas en este orden: cada nodo existe antes que los nodos y términos que contiene, y cada término existe antes de
que una relación apunte a él. Cada propuesta es un `UPSERT`, así que volver a enviar la lista actualiza el glosario en
su sitio… y sobrescribe el aspecto completo, incluido cualquier cambio hecho a mano en la interfaz de DataHub sobre
esos términos.

## Términos de una categoría

`termsOf(DataCategory)` es el término de la categoría seguido de sus `relatedTerms`. Por ejemplo,
`user.authorization.password` da
`[urn:li:glossaryTerm:DataCategory.user.authorization.password, urn:li:glossaryTerm:PersonalData.Personal,
urn:li:glossaryTerm:Confidentiality.High, urn:li:glossaryTerm:Integrity.High, urn:li:glossaryTerm:DataHandling.Secret]`.

## Términos de un campo

`termsOf(FieldClassification)` usa los niveles efectivos y la identificabilidad **del campo**:

1. `DataCategory.<key>`;
2. el término `PersonalData` de la naturaleza — solo si `field.isPersonal()` (categoría personal guardada como
   `IDENTIFIED` o `PSEUDONYMIZED`; un campo personal anonimizado o agregado no recibe ninguno);
3. `Confidentiality.<nivel del campo>`;
4. `Integrity.<nivel del campo>`;
5. `Identifiability.<valor>` — solo si el campo tiene identificabilidad (categoría personal);
6. `DataHandling.Secret` — solo si el campo es secreto.

```java
@Category(value = Categories.USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED,
          confidentiality = Confidentiality.MEDIUM)
private String reference;
// termsOf → [urn:li:glossaryTerm:DataCategory.user.unique_id.pseudonymous, urn:li:glossaryTerm:PersonalData.Personal,
//            urn:li:glossaryTerm:Confidentiality.Medium, urn:li:glossaryTerm:Integrity.Medium,
//            urn:li:glossaryTerm:Identifiability.Pseudonymized]
```

### Aplicar los términos a los campos de un dataset

**Linneo no hace este paso.** Relaciona cada campo con su dataset y su ruta de campo (según tus reglas de nombres de
persistencia) y añade los términos con la API de DataHub que ya uses. Con GraphQL, la mutación `addTerms` añade
términos a un campo sin quitar los demás; la ruta del campo va en `subResource`:

```graphql
mutation {
  addTerms(input: {
    termUrns: ["urn:li:glossaryTerm:DataCategory.user.contact.email",
               "urn:li:glossaryTerm:PersonalData.Personal",
               "urn:li:glossaryTerm:Confidentiality.Low",
               "urn:li:glossaryTerm:Integrity.Medium",
               "urn:li:glossaryTerm:Identifiability.Identified"],
    resourceUrn: "urn:li:dataset:(urn:li:dataPlatform:postgres,university.public.student,PROD)",
    subResourceType: DATASET_FIELD,
    subResource: "email"
  })
}
```

La ruta del campo es la que muestra DataHub para ese campo (en fuentes relacionales, normalmente el nombre de la
columna).

## DhProposal

`io.github.devbpm.linneo.catalog.datahub.DhProposal` — `public record`, `@since 0.1.0`

```java
public record DhProposal(String entityType, String entityUrn, String aspectName, String aspect)
```

Una propuesta de cambio de metadatos de DataHub que inserta o actualiza un aspecto de una entidad: el cuerpo de
`POST /aspects?action=ingestProposal`.

| Componente | Descripción |
|---|---|
| `entityType` | `glossaryNode` o `glossaryTerm`. |
| `entityUrn` | El URN de la entidad, como `urn:li:glossaryTerm:DataCategory.user.contact.email`. |
| `aspectName` | `glossaryNodeInfo`, `glossaryTermInfo` o `glossaryRelatedTerms`. |
| `aspect` | El aspecto como objeto JSON (consulta la [estructura del glosario](#estructura-del-glosario)). |

El constructor lanza `NullPointerException` si algún componente es `null`.

| Método | Descripción |
|---|---|
| `String toJson()` | El cuerpo JSON de una petición `ingestProposal`, con `changeType` `UPSERT`. El aspecto viaja **codificado en JSON como cadena** en `aspect.value`, con `contentType` `application/json`, como exige la API. |

```json
{"proposal":{"entityType":"glossaryTerm","entityUrn":"urn:li:glossaryTerm:Confidentiality.High","changeType":"UPSERT","aspectName":"glossaryTermInfo","aspect":{"value":"{\"customProperties\":{},\"name\":\"High\",\"definition\":\"Confidentiality level High.\",\"parentNode\":\"urn:li:glossaryNode:Confidentiality\",\"termSource\":\"INTERNAL\"}","contentType":"application/json"}}}
```

Envíalo con `Content-Type: application/json` y, para el endpoint Rest.li de GMS, la cabecera
`X-RestLi-Protocol-Version: 2.0.0`, además de tu token de acceso. Detrás del frontend de DataHub el mismo endpoint
suele ser `/api/gms/aspects?action=ingestProposal`.

## Salida JSON

Los aspectos y `toJson()` son JSON compacto (sin espacios) escrito por el escritor interno
`io.github.devbpm.linneo.internal.Json`, que comparten todos los exportadores (el paquete no se exporta):

- **Orden de los miembros** — `glossaryNodeInfo`: `customProperties`, `name`, `definition`, `parentNode`.
  `glossaryTermInfo`: `customProperties`, `name`, `definition`, `parentNode`, `termSource`, `sourceRef`,
  `sourceUrl`. `glossaryRelatedTerms`: `isRelatedTerms`, `relatedTerms`.
- **Miembros omitidos** — `parentNode` en los seis nodos raíz; `sourceRef` y `sourceUrl` en los términos
  `INTERNAL`; `isRelatedTerms` en las categorías raíz.
- **Escapado** — como en el [catálogo de OpenMetadata](../openmetadata/catalog.md#salida-json). La cadena del aspecto
  dentro de `toJson()` se escapa una segunda vez.

Véase también: [Mapeo](mapping.md) · [Resultados](../classify/results.md) ·
[Niveles de protección](../protection-levels.md) ·
[Comparativa de catálogos de datos](../README.md#comparativa-de-catálogos-de-datos) · curso:
[DataHub y Apache Atlas](../../course/08-datahub-and-atlas.md)
