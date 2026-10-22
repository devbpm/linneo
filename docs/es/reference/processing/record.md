---
title: "ProcessingRecord"
parent: "Tratamientos"
grand_parent: "Referencia"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingRecord.java
-->

# ProcessingRecord

`io.github.devbpm.linneo.processing.ProcessingRecord` — `public record`, `@since 0.1.0`

```java
public record ProcessingRecord(List<CheckedActivity> activities)
```

El registro de actividades de tratamiento de un responsable (RGPD, artículo 30.1). [`ProcessingChecker.verify`](checker.md)
lo construye a partir de actividades sin errores, de modo que cada actividad del registro tiene una base jurídica y
las condiciones que necesitan sus datos. Los avisos de cada actividad viajan con ella como **puntos abiertos**. El
registro se genera en Markdown para las personas y en JSON para las herramientas.

**Los textos que añade el registro están en inglés** (las etiquetas de las filas, `None declared`, `Open points:`, los
mensajes de los puntos abiertos), sea cual sea el idioma de la aplicación. Los nombres de las finalidades, los
interesados y las categorías vienen de la taxonomía (los de la taxonomía estándar también están en inglés), y el resto
de textos —nombres y descripciones de las actividades, intereses, referencias, conservación, destinatarios, destinos—
se escriben tal como se declararon. Por eso los ejemplos declaran esos textos en inglés: así el registro queda en un
solo idioma.

## Miembros

| Miembro | Descripción |
|---|---|
| `ProcessingRecord(List<CheckedActivity> activities)` | Copia la lista (inmutable). Lanza `IllegalArgumentException` si alguna actividad tiene un error, con el mensaje `The processing activity '<key>' has errors and cannot go in the record: [...]`, de modo que un registro solo contiene actividades válidas (los avisos se admiten). No comprueba claves duplicadas; lo normal es obtener el registro de `verify`, que notifica todos los errores. |
| `List<CheckedActivity> activities()` | Las actividades comprobadas, en orden de declaración. |
| `Optional<CheckedActivity> activity(String key)` | La primera actividad con esa clave, o vacío. |
| `String toMarkdown(String title)` | El registro como documento Markdown cuyo encabezado de nivel 1 es `title` (por ejemplo, el nombre del responsable). `NullPointerException` si `title` es `null`. |
| `String toJson()` | El registro como objeto JSON. |

## Markdown

`toMarkdown(title)` escribe el título y, para cada actividad, un encabezado de nivel 2 con su nombre, una línea con su
clave y su descripción, una tabla de dos columnas y, si la actividad tiene avisos, una lista `Open points:` con sus
mensajes.

| Fila | Se muestra | Contenido |
|---|---|---|
| `Purposes (art. 30.1.b)` | siempre | ``<name> (`<key>`)`` de cada finalidad, separadas por `; `, o `None` |
| `Lawful basis (art. 6.1)` | si se declara | `Art. <article>: <fideslang name>` |
| `Legitimate interest` | solo si la base es `LEGITIMATE_INTERESTS` y se indica un interés | el interés |
| `Special categories (art. 9.2)` | si se declara | `Art. <article>: <fideslang name>` |
| `Criminal offence data (art. 10)` | si se declara | `Art. 10: Official authority` o `Art. 10: Authorised by law` |
| `Legal reference` | si se declara | la referencia |
| `Data subjects (art. 30.1.c)` | siempre | como las finalidades |
| `Personal data (art. 30.1.c)` | siempre | como las finalidades, con `, special category` o `, criminal offence data` tras la clave de esas categorías |
| `Recipients (art. 30.1.d)` | siempre | los destinatarios separados por `; `, o `None declared` |
| `Transfers (art. 30.1.e)` | siempre | `<destination> (art. <article>: <fideslang name>)` para cada una, o `None declared` |
| `Retention (art. 30.1.f)` | siempre | la conservación, o `Not declared` |
| `Required protection (art. 30.1.g)` | siempre | `Confidentiality <level>, integrity <level>`, más `, includes secrets` si algún dato es un secreto |
| `Impact assessment (art. 35)` | siempre | la referencia, o `None` |

El registro indica la protección que los datos **requieren** (los niveles más altos de sus categorías y campos), como
punto de partida para la descripción de las medidas de seguridad del artículo 30.1.g; no describe las medidas.

En todo texto que se escribe en el documento —título, nombres, descripción y celdas— `|` se escapa como `\|` y los
saltos de línea (`\n` o `\r`, con los espacios que los rodean) se reducen a un espacio, para que ningún valor rompa la tabla. La
clave de la actividad se escribe como código, sin escapar.

### Ejemplo

```java
ProcessingActivity admissions = ProcessingActivity.builder("admissions", "Admissions")
        .description("Receives and assesses applications,\nincluding requests for the disability quota.")
        .purposes(DataUses.EDUCATION_ADMISSION)
        .dataSubjects(DataSubjects.APPLICANT)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL, USER_HEALTH_AND_MEDICAL_DISABILITY)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
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
```

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
| `specialCategoryCondition` | si se declara | como `legalBasis` |
| `criminalDataBasis` | si se declara | `{"code": <constant>, "article": "10"}`, sin `fideslang` |
| `legalReference` | si se declara | cadena |
| `recipients` | siempre | array de cadenas |
| `transfers` | siempre | array de `{"destination": <string>, "safeguard": {"code", "article", "fideslang"}}` |
| `retention`, `impactAssessment` | si se declaran | cadenas |
| `protection` | siempre | `{"natures": [<LegalNature>...], "confidentiality": <level>, "integrity": <level>, "secret": <boolean>}` |
| `openPoints` | siempre | array con los mensajes de los avisos |

El JSON del registro anterior (en una sola línea):

```json
{"activities":[{"key":"admissions","name":"Admissions","description":"Receives and assesses applications,\nincluding requests for the disability quota.","purposes":["education.admission"],"dataSubjects":["applicant"],"dataCategories":["user.contact.email","user.health_and_medical.disability","user.name"],"legalBasis":{"code":"PUBLIC_INTEREST","article":"6.1.e","fideslang":"Public interest"},"specialCategoryCondition":{"code":"SUBSTANTIAL_PUBLIC_INTEREST","article":"9.2.g","fideslang":"Reasons of substantial public interest (with a basis in law)"},"recipients":["Admissions board"],"transfers":[],"impactAssessment":"DPIA-2026-01","protection":{"natures":["ORDINARY","SPECIAL_CATEGORY"],"confidentiality":"HIGH","integrity":"HIGH","secret":false},"openPoints":["GDPR article(s) 6.1.e, 9.2.g rest on Union or Member State law; name it as the legal reference","Declare the envisaged time limit for erasure (GDPR article 30.1.f)"]},{"key":"alumni-newsletter","name":"Alumni newsletter","purposes":["marketing.communications.email"],"dataSubjects":["alumnus"],"dataCategories":["user.contact.email","user.name"],"legalBasis":{"code":"LEGITIMATE_INTERESTS","article":"6.1.f","fideslang":"Legitimate interests"},"legitimateInterest":"Keeping former students informed about university events","recipients":["Email delivery provider (processor)"],"transfers":[{"destination":"United States","safeguard":{"code":"STANDARD_CONTRACTUAL_CLAUSES","article":"46.2.c","fideslang":"SCCs"}}],"retention":"Until the alumnus unsubscribes","protection":{"natures":["ORDINARY"],"confidentiality":"LOW","integrity":"MEDIUM","secret":false},"openPoints":[]}]}
```

Los miembros `code` son los nombres de las constantes del enumerado, así que una herramienta puede recuperarlas con
`LegalBasis.valueOf(...)` y similares; los miembros `fideslang` pueden rellenar los campos correspondientes de una
declaración de privacidad de fideslang.

Véase también: [ProcessingChecker](checker.md) · [ProcessingActivity](activity.md) · [Bases jurídicas](bases.md) ·
[Actividades de tratamiento](README.md)
