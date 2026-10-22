---
title: "Catálogo de Apache Atlas"
parent: "Referencia"
nav_order: 17
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasClassificationDef.java
  - src/main/java/io/github/devbpm/linneo/internal/Json.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# Catálogo de Apache Atlas

El paquete `io.github.devbpm.linneo.catalog.atlas` traduce una [`Taxonomy`](../taxonomy.md) a **tipos de
clasificación** de [Apache Atlas](https://atlas.apache.org/) y calcula las clasificaciones de cada columna clasificada. **No hace
llamadas de red**: envía las definiciones de tipos con el cliente HTTP que ya uses a
`POST /api/atlas/v2/types/typedefs` y después añade las clasificaciones a las entidades de columna con
`POST /api/atlas/v2/entity/guid/{guid}/classifications`. Los cuerpos se comprobaron con Apache Atlas 2.3.0; los
nombres de clasificación con puntos son válidos desde Atlas 1.0 y la API no cambia hasta la 2.5.0.

El repositorio incluye una prueba de integración, `AtlasIT`, que envía estos cuerpos a un servidor real; véase
«Integration tests against catalog servers» en `CONTRIBUTING.md`.

Cómo se compara este exportador con los de OpenMetadata y DataHub: consulta la
[comparativa de catálogos de datos](../README.md#comparativa-de-catálogos-de-datos).

## AtlasCatalog

`io.github.devbpm.linneo.catalog.atlas.AtlasCatalog` — `public final class`, `@since 0.1.0`

Inmutable y seguro entre hilos. No tiene constructor público.

| Método | Devuelve | Descripción |
|---|---|---|
| `static AtlasCatalog of(Taxonomy taxonomy)` | catálogo | Con los [nombres por defecto](mapping.md) (`AtlasMapping.defaults()`). |
| `static AtlasCatalog of(Taxonomy taxonomy, AtlasMapping mapping)` | catálogo | Con los nombres de raíz indicados. `NullPointerException` si algún argumento es `null`. |
| `List<AtlasClassificationDef> classificationDefs()` | lista nueva y modificable | Los tipos de clasificación que hay que crear, en orden de creación (consulta [Tipos de clasificación](#tipos-de-clasificación)). |
| `String typeDefsJson()` | `String` | El cuerpo de `POST /api/atlas/v2/types/typedefs` que crea todos los tipos de clasificación a la vez: `{"classificationDefs":[…]}`. |
| `String classificationOf(DataCategory category)` | `String` | El nombre de la clasificación de la categoría, como `DataCategory.user.contact.email`. |
| `List<String> classificationsOf(DataCategory category)` | lista | La clasificación propia de la categoría seguida de las de la protección que implica. |
| `List<String> classificationsOf(FieldClassification field)` | lista | Las clasificaciones de una columna clasificada: su categoría, su naturaleza jurídica si es dato personal y la protección efectiva del campo. |
| `String classificationsJson(FieldClassification field)` | `String` | El cuerpo de `POST /api/atlas/v2/entity/guid/{guid}/classifications` para esa columna: un array JSON de objetos `{"typeName": …}`. |

```java
Taxonomy taxonomy = Taxonomy.load();
AtlasCatalog catalog = AtlasCatalog.of(taxonomy);

String typeDefs = catalog.typeDefsJson();   // POST /api/atlas/v2/types/typedefs

ClassClassification person = new Classifier(taxonomy).classify(Person.class);
for (FieldClassification field : person.fields()) {
    String body = catalog.classificationsJson(field);
    // POST /api/atlas/v2/entity/guid/{guid}/classifications, con el guid de la columna del campo
}
```

## Tipos de clasificación

### El árbol de categorías como supertipos

Cada categoría es una clasificación con el nombre de la raíz más la clave, cuyo **supertipo es la clasificación de
su padre**; las categorías raíz tienen como supertipo `DataCategory`, una clasificación raíz propia:

```text
DataCategory
└── DataCategory.user                      superTypes: [DataCategory]
    └── DataCategory.user.contact          superTypes: [DataCategory.user]
        └── DataCategory.user.contact.email   superTypes: [DataCategory.user.contact]
```

En Atlas, una entidad con una clasificación también coincide con sus supertipos, así que una búsqueda de
`DataCategory.user.health_and_medical` encuentra también las columnas clasificadas como
`DataCategory.user.health_and_medical.disability`, y una búsqueda de `DataCategory.user` encuentra todas las columnas
que contienen datos de usuario.

### Los niveles de protección como clasificaciones separadas

Los niveles de protección son clasificaciones propias, cada una subtipo de su raíz:

| Clasificaciones | Supertipo | `description` |
|---|---|---|
| `PersonalData.Personal`, `.SpecialCategory`, `.CriminalOffence` | `PersonalData` | `Personal data (GDPR article 4.1).`, `Special category of personal data (GDPR article 9).`, `Personal data relating to criminal convictions and offences (GDPR article 10).` |
| `Confidentiality.Public`, `.Low`, `.Medium`, `.High` | `Confidentiality` | `Confidentiality level <Level>.` |
| `Integrity.Low`, `.Medium`, `.High` | `Integrity` | `Integrity level <Level>.` |
| `Identifiability.Identified`, `.Pseudonymized`, `.Anonymized`, `.Aggregated` | `Identifiability` | `Personal data that is <value>.` |
| `DataHandling.Secret` | `DataHandling` | `A secret that grants access: it must never be read back, exported or published, only verified.` |

Una columna las lleva **junto a** la clasificación de su categoría. A propósito **no son supertipos de las
categorías**, por dos motivos:

- **Una categoría hija puede subir un nivel que implica su padre.** `user` es `Confidentiality.Low`, pero
  `user.health_and_medical` es `High`. Si `DataCategory.user` tuviera como supertipo `Confidentiality.Low`, todas las
  categorías por debajo heredarían `Low`, y una hija más estricta sería a la vez `Low` y `High`. La herencia de Atlas
  no se puede deshacer más abajo; la herencia de protección de Linneo sí se puede endurecer.
- **Las políticas basadas en etiquetas actúan directamente sobre ellos.** Los motores de políticas que leen las
  clasificaciones de Atlas, como las políticas basadas en etiquetas de Apache Ranger, se aplican a las clasificaciones
  que lleva una entidad. Una política sobre `Confidentiality.High` o `PersonalData.SpecialCategory` cubre entonces
  cualquier columna que la lleve, sea cual sea su categoría.

### Orden de creación y descripciones

`classificationDefs()` devuelve, en este orden (146 tipos con la taxonomía estándar):

1. las seis raíces, sin supertipos: `PersonalData`, `Confidentiality`, `Integrity`, `Identifiability`,
   `DataHandling`, `DataCategory` (6);
2. las clasificaciones de nivel de la tabla anterior (15);
3. una clasificación por categoría, en orden de clave, cada padre antes que sus hijas (125).

Cada supertipo se define antes que los tipos que lo nombran. La descripción de una categoría es su nombre, dos puntos
y su descripción, más ` Deprecated.` o ` Deprecated: use <key>.` si está obsoleta (las clasificaciones de Atlas no
tienen un nombre visible aparte):

```json
{"name":"DataCategory.user.contact.email","description":"User Contact Email: User's contact email address.","superTypes":["DataCategory.user.contact"],"attributeDefs":[]}
```

Las descripciones de las raíces son las de los [nodos de DataHub](../datahub/catalog.md#estructura-del-glosario),
salvo la de `DataCategory`: `What a column holds, from the Linneo data category taxonomy (fideslang default taxonomy
plus extensions).` Las descripciones se generan en inglés.

### Enviar las definiciones de tipos

`typeDefsJson()` reúne todas las definiciones en un único cuerpo:

```json
{"classificationDefs":[{"name":"PersonalData","description":"Legal nature of personal data under the GDPR.","superTypes":[],"attributeDefs":[]},{"name":"Confidentiality","description":"Harm that disclosing the data would cause.","superTypes":[],"attributeDefs":[]}, …]}
```

`POST /api/atlas/v2/types/typedefs` **crea** tipos y responde 409 si ya existen. En ejecuciones posteriores
—por ejemplo, después de añadir términos a la taxonomía— envía el mismo cuerpo con `PUT /api/atlas/v2/types/typedefs`
para actualizarlos. Para enviar solo lo nuevo, enumera los tipos existentes en
`GET /api/atlas/v2/types/typedefs/headers` y construye los cuerpos a partir de `classificationDefs()`.

## Clasificaciones de una categoría

`classificationsOf(DataCategory)` es la clasificación de la categoría seguida de, en este orden:

1. `PersonalData.Personal`, `PersonalData.SpecialCategory` o `PersonalData.CriminalOffence` — solo si la categoría
   es personal;
2. `Confidentiality.<nivel>`;
3. `Integrity.<nivel>`;
4. `DataHandling.Secret` — solo si la categoría es secreta.

Por ejemplo, `user.authorization.password` da `[DataCategory.user.authorization.password, PersonalData.Personal,
Confidentiality.High, Integrity.High, DataHandling.Secret]`.

## Clasificaciones de una columna

`classificationsOf(FieldClassification)` usa los niveles efectivos y la identificabilidad **del campo**:

1. `DataCategory.<key>`;
2. la clasificación `PersonalData` de la naturaleza — solo si `field.isPersonal()` (categoría personal guardada como
   `IDENTIFIED` o `PSEUDONYMIZED`; un campo personal anonimizado o agregado no recibe ninguna);
3. `Confidentiality.<nivel del campo>`;
4. `Integrity.<nivel del campo>`;
5. `Identifiability.<valor>` — solo si el campo tiene identificabilidad (categoría personal);
6. `DataHandling.Secret` — solo si el campo es secreto.

`classificationsJson(field)` escribe la misma lista como el cuerpo que las añade a la entidad de la columna:

```json
[{"typeName":"DataCategory.user.contact.email"},{"typeName":"PersonalData.Personal"},{"typeName":"Confidentiality.Low"},{"typeName":"Integrity.Medium"},{"typeName":"Identifiability.Identified"}]
```

**Linneo no busca la entidad de la columna.** Relaciona cada campo con su columna (según tus reglas de nombres de
persistencia) y busca su `guid` en Atlas, por ejemplo con `GET /api/atlas/v2/entity/uniqueAttribute/type/<tipo de
columna>` y el `qualifiedName` de la columna; después envía el cuerpo con `POST` a
`/api/atlas/v2/entity/guid/{guid}/classifications`. Atlas rechaza una clasificación que la entidad ya tiene
directamente, así que en ejecuciones posteriores elimina o actualiza antes las existentes.

### Propagación

El cuerpo no indica ningún `propagate`, así que **Atlas aplica su propagación por defecto**: en una instalación
estándar las clasificaciones se propagan por el linaje a las entidades derivadas de la columna. Revisa lo que eso
supone para tu linaje: una columna calculada a partir de columnas personales (una media, un recuento) hereda sus
clasificaciones `PersonalData.*` e `Identifiability.Identified` junto a las suyas propias. Si necesitas otro
comportamiento, añade `"propagate": false` (o las opciones de propagación que admita tu versión de Atlas) a los
objetos antes de enviarlos.

## AtlasClassificationDef

`io.github.devbpm.linneo.catalog.atlas.AtlasClassificationDef` — `public record`, `@since 0.1.0`

```java
public record AtlasClassificationDef(String name, String description, List<String> superTypes)
```

Un tipo de clasificación de Atlas que hay que crear: un elemento de `classificationDefs` en
`POST /api/atlas/v2/types/typedefs`.

| Componente | Admite `null` | Descripción |
|---|---|---|
| `name` | no | El nombre de la clasificación, como `DataCategory.user.contact.email`. |
| `description` | no | Qué significa. |
| `superTypes` | sí | Las clasificaciones que especializa; una entidad con esta clasificación también coincide con ellas. Un argumento `null` se convierte en una lista vacía; la lista se copia en una inmutable. |

El constructor lanza `NullPointerException` si `name` o `description` son `null`.

| Método | Descripción |
|---|---|
| `String toJson()` | El JSON de la definición de la clasificación, con `attributeDefs` vacío. |

```json
{"name":"Confidentiality.High","description":"Confidentiality level High.","superTypes":["Confidentiality"],"attributeDefs":[]}
```

## Salida JSON

Los cuerpos son JSON compacto (sin espacios) escrito por el escritor interno `io.github.devbpm.linneo.internal.Json`,
que comparten todos los exportadores (el paquete no se exporta):

- **Orden de los miembros** — una definición: `name`, `description`, `superTypes`, `attributeDefs`. Una
  clasificación: `typeName`.
- **Miembros omitidos** — ninguno: `superTypes` y `attributeDefs` se escriben como `[]` cuando están vacíos.
- **Escapado** — como en el [catálogo de OpenMetadata](../openmetadata/catalog.md#salida-json).

Véase también: [Mapeo](mapping.md) · [Resultados](../classify/results.md) ·
[Niveles de protección](../protection-levels.md) ·
[Comparativa de catálogos de datos](../README.md#comparativa-de-catálogos-de-datos) · curso:
[DataHub y Apache Atlas](../../course/08-datahub-and-atlas.md)
