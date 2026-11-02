---
title: "ProcessingRecord"
parent: "Tratamientos"
grand_parent: "Referencia"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingRecord.java
  - src/main/java/io/github/devbpm/linneo/processing/RecordDiff.java
  - src/main/java/io/github/devbpm/linneo/processing/ActivityChange.java
  - src/main/java/io/github/devbpm/linneo/processing/ImpactScreening.java
-->

# ProcessingRecord

`io.github.devbpm.linneo.processing.ProcessingRecord` — `public record`, `@since 0.1.0`

```java
public record ProcessingRecord(List<CheckedActivity> activities)
```

El registro de actividades de tratamiento de un responsable (RGPD, artículo 30.1).
[`ProcessingChecker.verify`](checker.md) lo construye a partir de actividades sin errores, de modo que cada actividad
del registro tiene una base jurídica y las condiciones que necesitan sus datos. Los avisos de cada actividad viajan con
ella como **puntos abiertos**. El registro se genera en Markdown para las personas y en JSON para las herramientas. Su
[huella](#huella) dice si sigue siendo el registro que alguien aprobó, y [`RecordDiff`](#recorddiff) qué ha cambiado
desde entonces.

**Los textos que añade el registro están en inglés** (las etiquetas de las filas, `None declared`, `Open points:`, los
mensajes de los puntos abiertos), sea cual sea el idioma de la aplicación. Los nombres de las finalidades, los
interesados y las categorías vienen de la taxonomía (los de la taxonomía estándar también están en inglés), y el resto
de textos —nombres y descripciones de las actividades, intereses, referencias, conservación, destinatarios, destinos—
se escriben tal como se declararon. Por eso los ejemplos declaran esos textos en inglés: así el registro queda en un
solo idioma.

## Miembros

| Miembro | Descripción |
|---|---|
| `ProcessingRecord(List<CheckedActivity> activities)` | Copia la lista (inmutable). Lanza `IllegalArgumentException` si alguna actividad tiene un error, con el mensaje `The processing activity '<key>' has errors and cannot go in the record: [...]`, de modo que un registro solo contiene actividades válidas (los avisos se admiten). También lanza `IllegalArgumentException` si dos actividades tienen la misma clave (`Two processing activities of the record have the key '<key>'`), de modo que cada clave está una sola vez en el registro. Lo normal es obtener el registro de `verify`, que notifica todos los errores. |
| `List<CheckedActivity> activities()` | Las actividades comprobadas, en orden de declaración. |
| `Optional<CheckedActivity> activity(String key)` | La actividad con esa clave, o vacío. |
| `String toMarkdown(String title)` | El registro como documento Markdown cuyo encabezado de nivel 1 es `title` (por ejemplo, el nombre del responsable). `NullPointerException` si `title` es `null`. |
| `String toJson()` | El registro como objeto JSON. |
| `String fingerprint()` | El SHA-256 del contenido del registro, como 64 dígitos hexadecimales; véase [Huella](#huella). |

## Markdown

`toMarkdown(title)` escribe el título y, para cada actividad, un encabezado de nivel 2 con su nombre, una línea con su
clave y su descripción, una tabla de dos columnas y, si la actividad tiene avisos, una lista `Open points:` con sus
mensajes.

| Fila | Se muestra | Contenido |
|---|---|---|
| `Purposes (art. 30.1.b)` | siempre | ``<name> (`<key>`)`` de cada finalidad, separadas por `; `, o `None` |
| `Lawful basis (art. 6.1)` | si se declara | `Art. <article>: <fideslang name>` |
| `Legitimate interest` | solo si la base es `LEGITIMATE_INTERESTS` y se indica un interés | el interés |
| `Special categories (art. 9.2)` | si se declara | `Art. <article>: <fideslang name>` por cada condición, separadas por `; ` |
| `Criminal offence data (art. 10)` | si se declara | `Art. 10: Official authority` o `Art. 10: Authorised by law` |
| `Legal reference` | si se declara | la referencia |
| `Data subjects (art. 30.1.c)` | siempre | como las finalidades |
| `Personal data (art. 30.1.c)` | siempre | como las finalidades, con `, special category` o `, criminal offence data` tras la clave de esas categorías |
| `Recipients (art. 30.1.d)` | siempre | los destinatarios separados por `; `, o `None declared` |
| `Transfers (art. 30.1.e)` | siempre | `<destination> (art. <article>: <fideslang name>)` para cada una, o `None declared` |
| `Retention (art. 30.1.f)` | siempre | la conservación, o `Not declared` |
| `Required protection (art. 30.1.g)` | siempre | `Confidentiality <level>, integrity <level>`, más `, includes secrets` si algún dato es un secreto |
| `Impact assessment (art. 35)` | siempre | la referencia, o `None` |
| `Impact assessment screening` | siempre | el [cribado](impact-screening.md) de la actividad tal como lo da `ImpactScreening.summary()`: `Not indicated`, o el resultado con los criterios que se cumplen y lo que muestra cada uno |

El registro indica la protección que los datos **requieren** (los niveles más altos de sus categorías y campos), como
punto de partida para la descripción de las medidas de seguridad del artículo 30.1.g; no describe las medidas.

Todo texto que se escribe en el documento —título, nombres, descripción y celdas— se escribe como texto, para que un
valor no pueda romper la tabla ni convertirse en un enlace o en HTML donde se renderiza el Markdown: `\`, `|`, `[` y
`]` se escapan con una barra invertida (`\|`), `&`, `<` y `>` pasan a `&amp;`, `&lt;` y `&gt;`, y los saltos de línea
(`\n` o `\r`, con los espacios que los rodean) se reducen a un espacio. La clave de la actividad se escribe como código,
sin escapar.

### Ejemplo

```java
ProcessingActivity admissions = ProcessingActivity.builder("admissions", "Admissions")
        .description("Receives and assesses applications,\nincluding requests for the disability quota.")
        .purposes(DataUses.EDUCATION_ADMISSION)
        .dataSubjects(DataSubjects.APPLICANT)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL, USER_HEALTH_AND_MEDICAL_DISABILITY)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .specialCategoryConditions(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
        .recipients("Admissions board")
        .impactAssessment("DPIA-2026-01")
        .build();
ProcessingActivity newsletter = ProcessingActivity.builder("alumni-newsletter", "Alumni newsletter")
        .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL)
        .dataSubjects(DataSubjects.ALUMNUS)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL)
        .legitimateInterest("Keeping former students informed about university events")
        .retention("Until the alumnus unsubscribes")
        .recipients("Email delivery provider (processor)")
        .transfer("United States", TransferSafeguard.STANDARD_CONTRACTUAL_CLAUSES)
        .build();

ProcessingRecord record = new ProcessingChecker(Taxonomy.standard()).verify(admissions, newsletter);
String markdown = record.toMarkdown("Example University | Records");
```

`markdown` es:

```markdown
# Example University \| Records

## Admissions

`admissions`: Receives and assesses applications, including requests for the disability quota.

| Record item | Content |
|---|---|
| Purposes (art. 30.1.b) | Admission (`education.admission`) |
| Lawful basis (art. 6.1) | Art. 6.1.e: Public interest |
| Special categories (art. 9.2) | Art. 9.2.g: Reasons of substantial public interest (with a basis in law) |
| Data subjects (art. 30.1.c) | Applicant (`applicant`) |
| Personal data (art. 30.1.c) | User Contact Email (`user.contact.email`); Disability (`user.health_and_medical.disability`, special category); Name (`user.name`) |
| Recipients (art. 30.1.d) | Admissions board |
| Transfers (art. 30.1.e) | None declared |
| Retention (art. 30.1.f) | Not declared |
| Required protection (art. 30.1.g) | Confidentiality High, integrity High |
| Impact assessment (art. 35) | DPIA-2026-01 |
| Impact assessment screening | Possible (1 criterion of WP248): 4. sensitive data: special category data user.health_and_medical.disability |

Open points:

- GDPR article(s) 6.1.e, 9.2.g rest on Union or Member State law; name it as the legal reference
- Declare the envisaged time limit for erasure (GDPR article 30.1.f)

## Alumni newsletter

`alumni-newsletter`

| Record item | Content |
|---|---|
| Purposes (art. 30.1.b) | Marketing Email Communications (`marketing.communications.email`) |
| Lawful basis (art. 6.1) | Art. 6.1.f: Legitimate interests |
| Legitimate interest | Keeping former students informed about university events |
| Data subjects (art. 30.1.c) | Alumnus (`alumnus`) |
| Personal data (art. 30.1.c) | User Contact Email (`user.contact.email`); Name (`user.name`) |
| Recipients (art. 30.1.d) | Email delivery provider (processor) |
| Transfers (art. 30.1.e) | United States (art. 46.2.c: SCCs) |
| Retention (art. 30.1.f) | Until the alumnus unsubscribes |
| Required protection (art. 30.1.g) | Confidentiality Low, integrity Medium |
| Impact assessment (art. 35) | None |
| Impact assessment screening | Not indicated |
```

El cribado se muestra tanto si la actividad cita una evaluación de impacto como si no, de modo que el delegado ve lo
que dicen los criterios junto a lo declarado.

## JSON

`toJson()` devuelve un objeto con un único miembro, `activities`, un array con un objeto por actividad en orden de
declaración. Los miembros opcionales se omiten cuando la actividad no los declara; las listas siempre están presentes.

| Miembro | Presente | Valor |
|---|---|---|
| `key`, `name` | siempre | cadenas |
| `description` | si se declara | cadena (conserva los saltos de línea) |
| `purposes`, `dataSubjects`, `dataCategories` | siempre | arrays de claves, ya resueltas (claves obsoletas sustituidas, categorías no personales descartadas) |
| `legalBasis` | si se declara | `{"code": <constant>, "article": <article>, "fideslang": <fideslang name>}` |
| `legitimateInterest` | solo si la base es `LEGITIMATE_INTERESTS` y se indica un interés | cadena |
| `specialCategoryConditions` | si se declara | array de objetos como `legalBasis`, uno por cada condición |
| `criminalDataBasis` | si se declara | `{"code": <constant>, "article": "10"}`, sin `fideslang` |
| `legalReference` | si se declara | cadena |
| `recipients` | siempre | array de cadenas |
| `transfers` | siempre | array de `{"destination": <string>, "safeguard": {"code", "article", "fideslang"}}` |
| `retention`, `impactAssessment` | si se declaran | cadenas |
| `screening` | siempre | `{"outcome": <Outcome>, "criteria": [<Criterion>...], "mandatoryCase": <string>}`: los nombres de las constantes; `mandatoryCase` (como `"35.3.b"`) solo cuando el cribado encuentra un caso del artículo 35.3 |
| `protection` | siempre | `{"natures": [<LegalNature>...], "confidentiality": <level>, "integrity": <level>, "secret": <boolean>}` |
| `openPoints` | siempre | array con los mensajes de los avisos |

El JSON del registro anterior (en una sola línea):

```json
{"activities":[{"key":"admissions","name":"Admissions","description":"Receives and assesses applications,\nincluding requests for the disability quota.","purposes":["education.admission"],"dataSubjects":["applicant"],"dataCategories":["user.contact.email","user.health_and_medical.disability","user.name"],"legalBasis":{"code":"PUBLIC_INTEREST","article":"6.1.e","fideslang":"Public interest"},"specialCategoryConditions":[{"code":"SUBSTANTIAL_PUBLIC_INTEREST","article":"9.2.g","fideslang":"Reasons of substantial public interest (with a basis in law)"}],"recipients":["Admissions board"],"transfers":[],"impactAssessment":"DPIA-2026-01","screening":{"outcome":"POSSIBLE","criteria":["SENSITIVE_DATA"]},"protection":{"natures":["ORDINARY","SPECIAL_CATEGORY"],"confidentiality":"HIGH","integrity":"HIGH","secret":false},"openPoints":["GDPR article(s) 6.1.e, 9.2.g rest on Union or Member State law; name it as the legal reference","Declare the envisaged time limit for erasure (GDPR article 30.1.f)"]},{"key":"alumni-newsletter","name":"Alumni newsletter","purposes":["marketing.communications.email"],"dataSubjects":["alumnus"],"dataCategories":["user.contact.email","user.name"],"legalBasis":{"code":"LEGITIMATE_INTERESTS","article":"6.1.f","fideslang":"Legitimate interests"},"legitimateInterest":"Keeping former students informed about university events","recipients":["Email delivery provider (processor)"],"transfers":[{"destination":"United States","safeguard":{"code":"STANDARD_CONTRACTUAL_CLAUSES","article":"46.2.c","fideslang":"SCCs"}}],"retention":"Until the alumnus unsubscribes","screening":{"outcome":"NOT_INDICATED","criteria":[]},"protection":{"natures":["ORDINARY"],"confidentiality":"LOW","integrity":"MEDIUM","secret":false},"openPoints":[]}]}
```

Los miembros `code` son los nombres de las constantes del enumerado, así que una herramienta puede recuperarlas con
`LegalBasis.valueOf(...)` y similares; los miembros `fideslang` pueden rellenar los campos correspondientes de una
declaración de privacidad de fideslang.

## Comparar registros

Un registro es un valor que calcula la aplicación, así que puede comparar dos: el que aprobó el delegado de protección
de datos y el que rige ahora. `fingerprint()` dice de una vez si algo cambió, y dos tipos de
`io.github.devbpm.linneo.processing`, `RecordDiff` y `ActivityChange`, dicen qué.

### Huella

`fingerprint()` es el SHA-256 (UTF-8, hexadecimal en minúscula, 64 dígitos) del contenido del registro, actividad a
actividad por orden de clave y aspecto a aspecto como los compara `RecordDiff`. Dos registros tienen la misma huella
exactamente cuando `RecordDiff` no encuentra ningún cambio entre ellos, sea cual sea el orden de sus actividades. Dice
que algo cambió, no qué. Una aplicación la guarda junto al registro que aprobó el delegado, para saber si el registro
sigue siendo ese. El texto que se resume escribe un sustituto UTF-16 suelto, que UTF-8 no puede codificar, como una
barra invertida, `u` y sus cuatro dígitos hexadecimales, de modo que dos registros que solo difieren en un carácter así
nunca tienen la misma huella.

### RecordDiff

`io.github.devbpm.linneo.processing.RecordDiff` — `public final class`, `@since 0.1.0`. Inmutable; no tiene
constructor público.

| Miembro | Descripción |
|---|---|
| `static RecordDiff between(ProcessingRecord before, ProcessingRecord after)` | Compara dos registros: `before` es el anterior, como el aprobado. `NullPointerException` si alguno es `null`. |
| `List<ActivityChange> changes()` | Los cambios, uno por cada actividad añadida, eliminada o modificada, por clave de actividad. Inmutable. |
| `Optional<ActivityChange> change(String activity)` | El cambio de una actividad, o vacío si la actividad es la misma en los dos registros o no está en ninguno. |
| `boolean isEmpty()` | `true` si no se añadió, eliminó ni modificó ninguna actividad. |

Una actividad que solo tiene `after` es `ADDED`, una que solo tiene `before` es `REMOVED`; en las demás, la diferencia
da los aspectos cuyo contenido difiere. El orden de los elementos de una lista (finalidades, destinatarios, puntos
abiertos...) no forma parte del contenido.

Los dos registros se calculan, así que el anterior se reconstruye a partir de lo que la aplicación guardó: la gobernanza
que mantiene la función de protección de datos, y el [inventario de datos](../classify/inventory.md) y la taxonomía del
registro que se aprobó. Comprobadas con lo que rige ahora, las mismas actividades muestran qué hace al registro una
taxonomía nueva, un cambio del modelo de datos o una edición de la gobernanza, antes de que nadie lo firme:

```java
ProcessingRecord approved = new ProcessingChecker(appliedTaxonomy, Jurisdiction.SPAIN, storedInventory)
        .verify(activities);
ProcessingRecord current = new ProcessingChecker(taxonomy, Jurisdiction.SPAIN, declared).verify(activities);

RecordDiff diff = RecordDiff.between(approved, current);
for (ActivityChange change : diff.changes()) {
    System.out.println(change);   // activity 'contact': [PERSONAL_DATA, PROTECTION] TIGHTENED
}
```

### ActivityChange

`io.github.devbpm.linneo.processing.ActivityChange` — `public record`, `@since 0.1.0`

```java
public record ActivityChange(String activity, CheckedActivity before, CheckedActivity after, Set<Aspect> aspects,
                             Direction direction)
```

Cómo difiere una actividad entre los dos registros.

| Componente | Nulable | Descripción |
|---|---|---|
| `activity` | no | La clave de la actividad. |
| `before` | sí | La actividad en el registro anterior, o `null` si se añadió. |
| `after` | sí | La actividad en el registro posterior, o `null` si se eliminó. |
| `aspects` | no | Qué cambió, nunca vacío: un conjunto inmutable de [`Aspect`](#aspect). |
| `direction` | no | Cómo cambió la protección que requieren los datos personales de la actividad, como [`Protection.Direction`](../protection-levels.md#direction): `TIGHTENED` significa que la actividad ahora trata datos que necesitan más protección, lo que puede exigir otras bases o una evaluación de impacto. `SAME` en una actividad añadida o eliminada. |

El constructor canónico lanza `NullPointerException` si `activity` o `direction` son `null`, e
`IllegalArgumentException` si `aspects` es `null` o está vacío, o si `before` y `after` son ambos `null`.

| Miembro | Descripción |
|---|---|
| `boolean isGovernance()` | `true` si cambió un aspecto de la [gobernanza](activity.md#governance) y nada más salvo los puntos abiertos: un cambio que hizo la función de protección de datos, con el alcance que declara el código y los datos que alcanza sin cambios. Los puntos abiertos que se derivan de él pueden cambiar también. |

`toString()` es `activity '<key>': <aspects>`, seguido de la dirección salvo que sea `SAME`.

### Aspect

`ActivityChange.Aspect` — `enum` anidado, `@since 0.1.0`: qué cambió en una actividad. `boolean isGovernance()` dice si
el aspecto forma parte de la gobernanza y no del alcance declarado en el código ni de los datos que alcanza.

| Aspecto | `isGovernance()` | Qué cambió |
|---|---|---|
| `ADDED` | `false` | La actividad es nueva en el registro. |
| `REMOVED` | `false` | La actividad ya no está en el registro. |
| `NAME` | `false` | El nombre. |
| `DESCRIPTION` | `false` | La descripción. |
| `PURPOSES` | `false` | Las finalidades, con las claves obsoletas sustituidas. |
| `DATA_SUBJECTS` | `false` | Las categorías de interesados, con las claves obsoletas sustituidas. |
| `PERSONAL_DATA` | `false` | Las categorías de datos personales tratadas, declaradas o alcanzadas a través de entidades y objetos. |
| `PROTECTION` | `false` | La protección que requieren los datos personales; véase `direction`. |
| `LEGAL_BASIS` | `true` | La base jurídica. |
| `LEGITIMATE_INTEREST` | `true` | El interés legítimo perseguido. |
| `SPECIAL_CATEGORY_CONDITION` | `true` | Las condiciones del artículo 9.2. |
| `CRIMINAL_DATA_BASIS` | `true` | La base del artículo 10. |
| `LEGAL_REFERENCE` | `true` | La ley en la que se apoyan las bases. |
| `RECIPIENTS` | `true` | Los destinatarios. |
| `TRANSFERS` | `true` | Las transferencias a terceros países u organizaciones internacionales. |
| `RETENTION` | `true` | El plazo de supresión. |
| `IMPACT_ASSESSMENT` | `true` | La evaluación de impacto. |
| `RISK_FACTORS` | `true` | Los [factores de riesgo](impact-screening.md#declarar-factores-de-riesgo) que declara el delegado para el cribado de la necesidad de una evaluación de impacto. |
| `OPEN_POINTS` | `false` | Los puntos abiertos: los avisos de la actividad. |

Editar la conservación o los destinatarios es un cambio de gobernanza, y suele cerrar también un punto abierto. Editar
los factores de riesgo lo es también: el aviso sobre la evaluación de impacto nombra los criterios que se cumplen, así
que cambia `RISK_FACTORS` y, cuando el aviso cambia con ellos, `OPEN_POINTS`. Un cambio en los datos que alcanza la
actividad, como un campo nuevo en una entidad de la lista o una categoría más estricta, se ve en `PERSONAL_DATA` o en
`PROTECTION` y no lo es.

Véase también: [ProcessingChecker](checker.md) · [ProcessingActivity](activity.md) · [Bases jurídicas](bases.md) ·
[Actividades de tratamiento](README.md)
