---
title: "ProcessingChecker"
parent: "Tratamientos"
grand_parent: "Referencia"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingChecker.java
  - src/main/java/io/github/devbpm/linneo/processing/CheckedActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingViolation.java
  - src/main/java/io/github/devbpm/linneo/processing/Jurisdiction.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingException.java
-->

# ProcessingChecker

`io.github.devbpm.linneo.processing.ProcessingChecker` — `public final class`, `@since 0.1.0`

Comprueba [actividades de tratamiento](activity.md) frente a una [`Taxonomy`](../taxonomy.md) y la clasificación de
sus entidades, y construye el [registro de actividades de tratamiento](record.md). Aplica el fallo seguro a lo que
haría ilícita una actividad tal como está declarada (errores) y notifica las lagunas del registro como avisos;
consulta [Errores y avisos](README.md#errores-y-avisos). Las instancias son inmutables y seguras entre hilos.

## Métodos

| Miembro | Devuelve | Descripción |
|---|---|---|
| `ProcessingChecker(Taxonomy taxonomy)` | — | Crea un comprobador; las claves y las entidades deben pertenecer a `taxonomy`. Crea su propio [`Classifier`](../classify/classifier.md) para esa taxonomía. `NullPointerException` si `taxonomy` es `null`. Aplica solo el RGPD, como `Jurisdiction.EU`. |
| `ProcessingChecker(Taxonomy taxonomy, Jurisdiction jurisdiction)` | — | Lo mismo, aplicando además las normas nacionales de `jurisdiction` (véase [Jurisdiction](#jurisdiction)). `NullPointerException` si algún argumento es `null`. |
| `Jurisdiction jurisdiction()` | [`Jurisdiction`](#jurisdiction) | El derecho nacional que este comprobador aplica además del RGPD. |
| `CheckedActivity check(ProcessingActivity activity)` | [`CheckedActivity`](#checkedactivity) | Comprueba una actividad: sus términos resueltos, sus datos personales, la protección que requieren y cada infracción encontrada. Nunca lanza excepciones por un problema de la actividad. `NullPointerException` si `activity` es `null`. |
| `List<CheckedActivity> checkAll(Collection<ProcessingActivity> activities)` | lista | Un resultado por actividad, en el orden dado. Una actividad cuya clave ya usó otra anterior recibe además el error `DUPLICATE_ACTIVITY`; la primera no. |
| `ProcessingRecord verify(Collection<ProcessingActivity> activities)` | [`ProcessingRecord`](record.md) | Ejecuta `checkAll` y lanza una [`ProcessingException`](#processingexception) con **todos los errores de todas las actividades** si hay alguno. Los avisos no hacen fallar. |
| `ProcessingRecord verify(ProcessingActivity... activities)` | [`ProcessingRecord`](record.md) | Lo mismo, con las actividades una a una. |

```java
@Test
void recordOfProcessingActivities() {
    ProcessingRecord record = new ProcessingChecker(Taxonomy.load()).verify(UniversityActivities.all());
}
```

Usa `Taxonomy.load()` si tus claves o entidades usan términos de una [`TaxonomyExtension`](../extending.md); con
`Taxonomy.standard()` serían desconocidos.

## Qué se comprueba

Para cada actividad, en este orden (las infracciones de una `CheckedActivity` lo siguen):

1. **Finalidades** y después **interesados**: cada clave se busca en la taxonomía. Una clave desconocida es el error
   `UNKNOWN_KEY` y se descarta. Una clave obsoleta es el aviso `DEPRECATED_KEY` y se sustituye por su término
   `replaced_by`, siguiendo la cadena de sustituciones; una clave obsoleta sin sustituto se conserva, con el aviso. Los
   duplicados tras la sustitución se conservan una vez, en el orden de declaración. Una lista vacía es el error
   `MISSING_PURPOSE` o `MISSING_DATA_SUBJECTS`.
2. **Datos personales** (consulta [más abajo](#cómo-se-reúnen-los-datos-personales)). Si no hay ninguno y nada quedó
   sin resolver por una clave de categoría desconocida o una entidad con errores de clasificación, el error es
   `MISSING_PERSONAL_DATA`.
3. **Licitud**, solo cuando la actividad trata datos personales:
   - sin `legalBasis`: error `MISSING_LEGAL_BASIS`;
   - `LEGITIMATE_INTERESTS` sin `legitimateInterest`: aviso `MISSING_LEGITIMATE_INTEREST`; un `legitimateInterest`
     con otra base: aviso `UNNEEDED_CONDITION`;
   - datos de categorías especiales sin `specialCategoryCondition`: error `MISSING_SPECIAL_CATEGORY_CONDITION`; una
     condición sin datos de categorías especiales: aviso `UNNEEDED_CONDITION`;
   - datos penales sin `criminalDataBasis`: error `MISSING_CRIMINAL_DATA_BASIS`; una base sin datos penales: aviso
     `UNNEEDED_CONDITION`;
   - con `Jurisdiction.SPAIN`, `EXPLICIT_CONSENT` como condición del artículo 9.2 de una actividad que trata ideología,
     afiliación sindical, religión, orientación sexual, creencias u origen racial o étnico: error
     `CONSENT_NOT_SUFFICIENT`;
   - bases que [descansan en una ley](bases.md#la-referencia-legal) sin `legalReference`: aviso
     `MISSING_LEGAL_REFERENCE`;
   - sin `retention`: aviso `MISSING_RETENTION`;
   - datos de categorías especiales, datos penales o una finalidad de elaboración de perfiles, sin
     `impactAssessment`: aviso `IMPACT_ASSESSMENT_RECOMMENDED`.

Una finalidad cuenta como **elaboración de perfiles** cuando un segmento de su clave (ya resuelta) es `profiling`,
`profiled` o `targeted`. En la taxonomía estándar son `marketing.advertising.profiling`,
`marketing.advertising.first_party.targeted`, `marketing.advertising.third_party.targeted`,
`personalize.content.profiling`, `personalize.content.profiled` y la obsoleta `personalize.profiling` (comprobada como
`personalize.content.profiling`). Los usos de datos de una extensión solo cuentan si sus claves usan esos segmentos.

El comprobador no examina `description`, `recipients` ni `transfers`.

## Cómo se reúnen los datos personales

Los datos personales de una actividad son la unión de dos fuentes:

- **Claves de categoría** (`dataCategories`). Una clave desconocida es el error `UNKNOWN_KEY`. Una clave obsoleta es siempre el
  aviso `DEPRECATED_KEY`; con sustituto se resuelve a él, sin sustituto se usa tal cual. Una categoría que no es dato personal (naturaleza jurídica `NONE`) es el aviso `NOT_PERSONAL_DATA` y queda
  fuera del registro. Cada categoría restante aporta sus propios niveles.
- **Entidades** (`entities`). Cada clase se clasifica con el [`Classifier`](../classify/classifier.md). Si la
  clasificación tiene errores, la actividad recibe un error `ENTITY_NOT_CLASSIFIED` con su número (ejecuta el test
  del clasificador para verlos); los campos que sí pudieron clasificarse siguen contando. La entidad aporta sus
  [campos personales](../classify/results.md): los campos cuya categoría es personal y cuya identificabilidad no es
  `AGGREGATED` ni `ANONYMIZED`, porque los datos agregados y anonimizados no son datos personales. El clasificador ya
  resuelve las claves obsoletas de las anotaciones a su sustituto, y sus avisos no se repiten aquí. Cada campo aporta
  sus niveles **efectivos**, de modo que cuenta una confidencialidad o integridad elevada en el campo.

Las categorías se guardan una sola vez cada una, ordenadas por clave, en `CheckedActivity.dataCategories()`. La
protección que requiere la actividad es la confidencialidad y la integridad más altas de todas esas aportaciones
(`PUBLIC` y `LOW` si no hay ninguna), e incluye secretos si alguna es un secreto. Sus naturalezas jurídicas deciden si
la actividad trata categorías especiales (artículo 9) o datos penales (artículo 10).

Los campos personales de una entidad no siempre son de los interesados de la actividad. En el modelo universitario del
curso, todas las entidades heredan de una superclase de auditoría cuyo campo `createdBy` guarda el nombre de usuario
de un miembro del personal, así que incluir `Enrollment` añade `user.account.username` a una actividad sobre
estudiantes. Declara las categorías por clave cuando una entidad contiene más de lo que trata la actividad.

## CheckedActivity

`io.github.devbpm.linneo.processing.CheckedActivity` — `public record`, `@since 0.1.0`

```java
public record CheckedActivity(ProcessingActivity activity, List<DataUse> purposes, List<DataSubject> dataSubjects,
                              List<DataCategory> dataCategories, Confidentiality confidentiality, Integrity integrity,
                              boolean secret, List<ProcessingViolation> violations)
```

| Componente | Descripción |
|---|---|
| `activity` | La actividad tal como se declaró. |
| `purposes` | Los usos de datos, sin las claves desconocidas y con las obsoletas sustituidas. |
| `dataSubjects` | Los sujetos de datos, del mismo modo. |
| `dataCategories` | Las categorías de datos personales tratadas, de las claves declaradas y de los campos personales de las entidades, en orden de clave. |
| `confidentiality` | La confidencialidad más alta de esos datos, incluidas las elevaciones en los campos; `PUBLIC` si no hay ninguno. |
| `integrity` | La integridad más alta de esos datos, incluidas las elevaciones en los campos; `LOW` si no hay ninguno. |
| `secret` | Si alguno de esos datos es un secreto. |
| `violations` | Todos los problemas encontrados, errores y avisos. |

El constructor canónico copia las listas (inmutables) y lanza `NullPointerException` si `activity`,
`confidentiality`, `integrity` o una lista es `null`.

| Método | Descripción |
|---|---|
| `boolean isValid()` | `true` si no hay ningún error; los avisos se admiten. |
| `List<ProcessingViolation> errors()` | Las infracciones de gravedad `ERROR`. |
| `List<ProcessingViolation> warnings()` | Las infracciones de gravedad `WARNING`: los puntos abiertos del registro. |
| `Set<LegalNature> natures()` | Las naturalezas jurídicas de `dataCategories` (nunca `NONE`). |
| `boolean processesSpecialCategories()` | `true` si alguna categoría es `SPECIAL_CATEGORY` (artículo 9). |
| `boolean processesCriminalOffenceData()` | `true` si alguna categoría es `CRIMINAL_OFFENCE` (artículo 10). |

```java
CheckedActivity checked = checker.check(admissions);
checked.isValid();                      // true
checked.natures();                      // [ORDINARY, SPECIAL_CATEGORY]
checked.confidentiality();              // HIGH
checked.warnings();                     // []
```

## ProcessingViolation

`io.github.devbpm.linneo.processing.ProcessingViolation` — `public record`, `@since 0.1.0`

```java
public record ProcessingViolation(String activity, Severity severity, Code code, String message)
```

| Componente | Descripción |
|---|---|
| `activity` | La clave de la actividad. |
| `severity` | Si hace fallar la verificación. |
| `code` | Qué tipo de problema es. |
| `message` | Una descripción que nombra la clave o la disposición implicada. |

El constructor canónico lanza `NullPointerException` si algún componente es `null`.

| Método | Descripción |
|---|---|
| `boolean isError()` | `true` para la gravedad `ERROR`. |
| `String toString()` | `<SEVERITY> <CODE> <activity>: <message>`, por ejemplo `ERROR MISSING_LEGAL_BASIS newsletter: Personal data needs a lawful basis (GDPR article 6.1)`. |

### Severity

`ProcessingViolation.Severity`

| Constante | Significado |
|---|---|
| `ERROR` | La actividad, tal como está declarada, sería ilícita o no se puede comprobar; `verify` falla e `isValid()` es `false`. |
| `WARNING` | El registro de la actividad está incompleto o debe revisarse; la verificación pasa y el mensaje es un punto abierto del registro. |

### Códigos

`ProcessingViolation.Code`: todos los valores, primero los errores, con el mensaje que les da el comprobador (`<k>`
para una clave, `<a>` para un artículo). Los mensajes están en inglés.

| Código | Gravedad | Cuándo se produce | Mensaje |
|---|---|---|---|
| `UNKNOWN_KEY` | error | Una clave de finalidad, interesado o categoría de datos no está en la taxonomía. | `Unknown data use '<k>'` / `Unknown data subject '<k>'` / `Unknown data category '<k>'` |
| `MISSING_PURPOSE` | error | `purposes` está vacía. | `No data use declared` |
| `MISSING_DATA_SUBJECTS` | error | `dataSubjects` está vacía. | `No data subject declared` |
| `MISSING_PERSONAL_DATA` | error | Ninguna categoría personal ni entidad con campos personales, y ninguna clave de categoría desconocida ni error de entidad que lo explique. | `The activity processes no personal data` |
| `ENTITY_NOT_CLASSIFIED` | error | Una entidad de la lista tiene errores de clasificación. | `<nombre binario de la clase> has <n> classification error(s)` |
| `MISSING_LEGAL_BASIS` | error | Datos personales sin `legalBasis`. | `Personal data needs a lawful basis (GDPR article 6.1)` |
| `MISSING_SPECIAL_CATEGORY_CONDITION` | error | Datos de categorías especiales sin `specialCategoryCondition`. | `Special categories of data (<keys>) need a condition of GDPR article 9.2` |
| `MISSING_CRIMINAL_DATA_BASIS` | error | Datos penales sin `criminalDataBasis`. | `Criminal offence data (<keys>) needs a basis of GDPR article 10` |
| `CONSENT_NOT_SUFFICIENT` | error | Solo con `Jurisdiction.SPAIN`: la actividad trata `user.demographic.political_opinion`, `philosophical_belief`, `trade_union_membership`, `religious_belief`, `sexual_orientation` o `race_ethnicity` (o un descendiente) y su única condición del artículo 9.2 es `EXPLICIT_CONSENT` (LOPDGDD, artículo 9.1). | `Explicit consent alone does not lift the prohibition of processing <keys> (LOPDGDD article 9.1); declare another condition of GDPR article 9.2` |
| `DUPLICATE_ACTIVITY` | error | Solo en `checkAll`/`verify`: una actividad anterior tiene la misma clave. | `Another activity of the record has the key '<k>'` |
| `DEPRECATED_KEY` | **aviso** | Una clave de finalidad, interesado o categoría está obsoleta; la actividad se comprueba con su sustituta. | `The data use '<k>' is deprecated; checked as '<r>'` (igual con `data subject` y `data category`; sin la parte `; checked as` cuando la clave no tiene sustituto) |
| `NOT_PERSONAL_DATA` | **aviso** | Una categoría declarada no es dato personal; queda fuera del registro. | `The data category '<k>' is not personal data and is left out of the record` |
| `UNNEEDED_CONDITION` | **aviso** | Se declara una condición del artículo 9.2 o una base del artículo 10 para datos que la actividad no trata, o se indica un interés legítimo con otra base jurídica. | `Article <a> is declared but the activity processes no special category of data` / `Article 10 is declared but the activity processes no criminal offence data` / `A legitimate interest is stated but the lawful basis is article <a>` |
| `MISSING_LEGAL_REFERENCE` | **aviso** | Se aplica una base que descansa en una ley y no se indica `legalReference`. | `GDPR article(s) <a>, <a> rest on Union or Member State law; name it as the legal reference`; con `Jurisdiction.SPAIN` y una condición `SUBSTANTIAL_PUBLIC_INTEREST`, `HEALTH_OR_SOCIAL_CARE` o `PUBLIC_HEALTH`, termina con ` (LOPDGDD article 9.2: a law with the rank of statute)` |
| `MISSING_LEGITIMATE_INTEREST` | **aviso** | La base es `LEGITIMATE_INTERESTS` y no se indica `legitimateInterest`. | `State the legitimate interest pursued (GDPR article 6.1.f)` |
| `MISSING_RETENTION` | **aviso** | Datos personales sin `retention`. | `Declare the envisaged time limit for erasure (GDPR article 30.1.f)` |
| `IMPACT_ASSESSMENT_RECOMMENDED` | **aviso** | Datos de categorías especiales, datos penales o una finalidad de elaboración de perfiles, sin `impactAssessment`. | `The activity involves <reasons>; GDPR article 35 may require an impact assessment`, donde `<reasons>` une `special categories of data`, `criminal offence data` y `profiling` con `and` |

Salida real para una actividad a la que le faltan ambas condiciones:

```text
ERROR MISSING_SPECIAL_CATEGORY_CONDITION screening: Special categories of data (user.health_and_medical.disability) need a condition of GDPR article 9.2
ERROR MISSING_CRIMINAL_DATA_BASIS screening: Criminal offence data (user.criminal_history) needs a basis of GDPR article 10
WARNING MISSING_LEGAL_REFERENCE screening: GDPR article(s) 6.1.c rest on Union or Member State law; name it as the legal reference
WARNING MISSING_RETENTION screening: Declare the envisaged time limit for erasure (GDPR article 30.1.f)
WARNING IMPACT_ASSESSMENT_RECOMMENDED screening: The activity involves special categories of data and criminal offence data; GDPR article 35 may require an impact assessment
```

y para una con tres declaraciones innecesarias (`legitimateInterest(...)` seguido de `legalBasis(CONSENT)`, más
`EXPLICIT_CONSENT` y `OFFICIAL_AUTHORITY` en una actividad que solo trata `user.name`):

```text
WARNING UNNEEDED_CONDITION newsletter: A legitimate interest is stated but the lawful basis is article 6.1.a
WARNING UNNEEDED_CONDITION newsletter: Article 9.2.a is declared but the activity processes no special category of data
WARNING UNNEEDED_CONDITION newsletter: Article 10 is declared but the activity processes no criminal offence data
```

## Jurisdiction

`io.github.devbpm.linneo.processing.Jurisdiction` — `public enum`, `@since 0.1.0`

El derecho nacional de protección de datos que un comprobador aplica además del RGPD.

| Constante | Significado |
|---|---|
| `EU` | Solo el RGPD, sin normas nacionales. Es el valor de `new ProcessingChecker(taxonomy)`. |
| `SPAIN` | El RGPD y la Ley Orgánica 3/2018 (LOPDGDD). Añade las dos reglas siguientes. |

Con `SPAIN`:

- **El consentimiento no basta** (LOPDGDD, artículo 9.1). Si la actividad trata `user.demographic.political_opinion`,
  `philosophical_belief`, `trade_union_membership`, `religious_belief`, `sexual_orientation` o `race_ethnicity`
  (o un descendiente) y su única condición del artículo 9.2 es `EXPLICIT_CONSENT`, el error
  `CONSENT_NOT_SUFFICIENT` nombra esas claves: declara otra condición del artículo 9.2.
- **Norma con rango de ley** (LOPDGDD, artículo 9.2). Cuando la condición es `SUBSTANTIAL_PUBLIC_INTEREST`,
  `HEALTH_OR_SOCIAL_CARE` o `PUBLIC_HEALTH` y no hay `legalReference`, el aviso `MISSING_LEGAL_REFERENCE` añade que
  la norma debe tener rango de ley.

```java
ProcessingChecker checker = new ProcessingChecker(Taxonomy.load(), Jurisdiction.SPAIN);
checker.jurisdiction();   // SPAIN
```

## ProcessingException

`io.github.devbpm.linneo.processing.ProcessingException` — `public class ProcessingException extends
RuntimeException`, `@since 0.1.0`

La lanza `ProcessingChecker.verify` cuando una actividad está incompleta o sería ilícita tal como está declarada.

| Miembro | Descripción |
|---|---|
| `ProcessingException(List<ProcessingViolation> errors)` | Crea la excepción a partir de los errores encontrados (al menos uno). La lista se copia. |
| `List<ProcessingViolation> errors()` | Los errores, todos de gravedad `ERROR`, de todas las actividades verificadas, en el orden de las actividades. |

El mensaje es el número de errores seguido de una línea por error, con el formato de
[`ProcessingViolation.toString()`](#processingviolation). Verificar dos veces la misma actividad más una vacía da:

```text
4 processing error(s):
 - ERROR DUPLICATE_ACTIVITY newsletter: Another activity of the record has the key 'newsletter'
 - ERROR MISSING_PURPOSE empty: No data use declared
 - ERROR MISSING_DATA_SUBJECTS empty: No data subject declared
 - ERROR MISSING_PERSONAL_DATA empty: The activity processes no personal data
```

La lista `errors` se guarda en un campo `transient`: no se conserva si la excepción se serializa.

Véase también: [ProcessingActivity](activity.md) · [Bases jurídicas](bases.md) · [ProcessingRecord](record.md) ·
[Classifier](../classify/classifier.md) · curso:
[Actividades de tratamiento y el registro](../../course/09-processing-activities.md)
