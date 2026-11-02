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
  - src/main/java/io/github/devbpm/linneo/processing/ImpactScreening.java
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
| `ProcessingChecker(Taxonomy taxonomy, Jurisdiction jurisdiction, DataInventory inventory)` | — | Lo mismo, y además resuelve los [`objects`](activity.md#componentes) de las actividades en `inventory`, un [inventario de datos](../classify/inventory.md), de modo que se puedan comprobar actividades sin las clases del modelo, por ejemplo a partir de registros que una aplicación guarda en su base de datos. `inventory` puede ser `null` si no hay ninguno; entonces una actividad que incluye objetos recibe un error. `NullPointerException` si `taxonomy` o `jurisdiction` son `null`. |
| `Jurisdiction jurisdiction()` | [`Jurisdiction`](#jurisdiction) | El derecho nacional que este comprobador aplica además del RGPD. |
| `CheckedActivity check(ProcessingActivity activity)` | [`CheckedActivity`](#checkedactivity) | Comprueba una actividad: sus términos resueltos, sus datos personales, la protección que requieren y cada infracción encontrada. Nunca lanza excepciones por un problema de la actividad. `NullPointerException` si `activity` es `null`. |
| `List<CheckedActivity> checkAll(Collection<ProcessingActivity> activities)` | lista | Un resultado por actividad, en el orden dado. Una actividad cuya clave ya usó otra anterior recibe además el error `DUPLICATE_ACTIVITY`; la primera no. Cada entidad se clasifica una sola vez para todas las actividades, por muchas que la incluyan. |
| `ProcessingRecord verify(Collection<ProcessingActivity> activities)` | [`ProcessingRecord`](record.md) | Ejecuta `checkAll` y lanza una [`ProcessingException`](#processingexception) con **todos los errores de todas las actividades**, también los de gobernanza, si hay alguno (su mensaje lista los 50 primeros). Los avisos no hacen fallar. |
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
   sin resolver por una clave de categoría desconocida, una entidad con errores de clasificación, o un objeto o un
   campo de un objeto desconocidos, el error es `MISSING_PERSONAL_DATA`.
3. **Licitud**, solo cuando la actividad trata datos personales. Comprueba la
   [gobernanza](activity.md#governance) de la actividad, y cada problema que encuentra se notifica en el
   [elemento](#element) de la gobernanza al que se refiere:
   - sin `legalBasis`: error `MISSING_LEGAL_BASIS`;
   - `LEGITIMATE_INTERESTS` sin `legitimateInterest`: aviso `MISSING_LEGITIMATE_INTEREST`; un `legitimateInterest`
     con otra base: aviso `UNNEEDED_CONDITION`;
   - datos de categorías especiales sin `specialCategoryConditions`: error `MISSING_SPECIAL_CATEGORY_CONDITION`;
     condiciones sin datos de categorías especiales: aviso `UNNEEDED_CONDITION`;
   - datos penales sin `criminalDataBasis`: error `MISSING_CRIMINAL_DATA_BASIS`; una base sin datos penales: aviso
     `UNNEEDED_CONDITION`;
   - con `Jurisdiction.SPAIN`, `EXPLICIT_CONSENT` como **única** condición del artículo 9.2 de una actividad que trata
     ideología, afiliación sindical, religión, orientación sexual, creencias u origen racial o étnico: error
     `CONSENT_NOT_SUFFICIENT` (otra condición declarada junto a ella cubre los datos que el consentimiento no puede);
   - bases que [descansan en una ley](bases.md#la-referencia-legal) sin `legalReference`: aviso
     `MISSING_LEGAL_REFERENCE`. Con `Jurisdiction.SPAIN` el mensaje añade, cuando procede,
     `LOPDGDD article(s) 8, 9.2, 10: a law with the rank of statute`, con las disposiciones que la exigen: la 8 para una
     obligación legal o una misión de interés público, la 9.2 para las condiciones de los artículos 9.2.g, h e i, y la 10
     para datos penales autorizados por ley;
   - con `Jurisdiction.SPAIN`, datos sobre infracciones y sanciones administrativas (`user.administrative_sanction` o
     descendientes), una base distinta del consentimiento y sin `legalReference`: aviso `MISSING_LEGAL_REFERENCE`
     (artículo 27 de la LOPDGDD: solo pueden tratarlos los órganos competentes, con el consentimiento de la persona o
     cuando una norma con rango de ley lo permite);
   - sin `retention`: aviso `MISSING_RETENTION`;
   - sin `impactAssessment`, y el [cribado](impact-screening.md) de la actividad encuentra un caso del artículo 35.3 del
     RGPD (`REQUIRED`): error `IMPACT_ASSESSMENT_REQUIRED`; o encuentra dos criterios o más (`LIKELY`): aviso
     `IMPACT_ASSESSMENT_RECOMMENDED`. Un criterio (`POSSIBLE`) o ninguno no genera nada.

El cribado es el que devuelve `CheckedActivity.screening()`. No toma por sí solos los datos de categorías especiales,
los datos penales ni la elaboración de perfiles como motivo: cada uno es un único criterio de las directrices del
Comité Europeo de Protección de Datos, y con dos una evaluación pasa a ser probable. Lo que el código no puede ver, como
la escala del tratamiento, lo declara el delegado como factores de riesgo en la [gobernanza](activity.md#governance), y
un `Factor.LARGE_SCALE` declarado convierte los datos de categorías especiales en un caso del artículo 35.3.b. Los
criterios, cómo se encuentra cada uno y los tres casos están en [ImpactScreening](impact-screening.md).

El comprobador no examina `description`, `recipients` ni `transfers`.

## Cómo se reúnen los datos personales

Los datos personales de una actividad son la unión de tres fuentes:

- **Claves de categoría** (`dataCategories`). Una clave desconocida es el error `UNKNOWN_KEY`. Una clave obsoleta es
  siempre el aviso `DEPRECATED_KEY`; con sustituto se resuelve a él, sin sustituto se usa tal cual. Una categoría que no
  es dato personal (naturaleza jurídica `NONE`) es el aviso `NOT_PERSONAL_DATA` y queda fuera del registro. Cada
  categoría restante aporta sus propios niveles.
- **Entidades** (`entities`). Cada clase se clasifica con el [`Classifier`](../classify/classifier.md). Si la
  clasificación tiene errores, la actividad recibe un error `ENTITY_NOT_CLASSIFIED` con su número (ejecuta el test
  del clasificador para verlos); los campos que sí pudieron clasificarse siguen contando. La entidad aporta sus
  [campos personales](../classify/results.md): los campos cuya categoría es personal y cuya identificabilidad no es
  `AGGREGATED` ni `ANONYMIZED`, porque los datos agregados y anonimizados no son datos personales. El clasificador ya
  resuelve las claves obsoletas de las anotaciones a su sustituto, y sus avisos no se repiten aquí. Cada campo aporta
  sus niveles **efectivos**, de modo que cuenta una confidencialidad o integridad elevada en el campo.
  `checkAll` y `verify` clasifican cada entidad una sola vez para todas las actividades que la incluyen; `check`
  clasifica las entidades de la única actividad que recibe.
- **Objetos** (`objects`), que se resuelven en el [inventario de datos](../classify/inventory.md) con el que se creó el
  comprobador. Un objeto que el inventario no contiene, o cualquier objeto cuando el comprobador no tiene inventario, es
  el error `UNKNOWN_KEY`. Un campo del objeto cuya clave no está en la taxonomía también es `UNKNOWN_KEY`, sea o no
  personal; los campos exentos se omiten. Un campo cuya clave está obsoleta es el aviso `DEPRECATED_KEY`, porque el
  inventario guarda las claves tal como están escritas (y por eso, a diferencia de los avisos del propio clasificador
  en una entidad, se notifica aquí), sea o no personal el campo; el campo se comprueba con la sustituta. Cada uno de los
  demás campos aporta como el campo de una entidad: su categoría, resuelta a la sustituta de una clave obsoleta, y su
  protección **efectiva**, con los niveles elevados en el campo, pero solo si es personal: una categoría personal no
  declarada `ANONYMIZED` ni `AGGREGATED`. Con el inventario que `Classifier.inventory` construye
  para una clase, una actividad que incluye su objeto obtiene las mismas categorías y la misma protección que una que
  incluye la clase.

Las categorías se guardan una sola vez cada una, ordenadas por clave, en `CheckedActivity.dataCategories()`. La
protección que requiere la actividad, `CheckedActivity.protection()`, es la
[combinación](../protection-levels.md#protection) de todas esas aportaciones: la confidencialidad y la integridad más
altas (`PUBLIC` y `LOW` si no hay ninguna), secreto si alguna es un secreto y la más estricta de sus naturalezas
jurídicas (solo `NONE` si no hay ninguna). Las naturalezas jurídicas de las categorías deciden si la actividad trata
categorías especiales (artículo 9) o datos penales (artículo 10).

Los campos personales de una entidad no siempre son de los interesados de la actividad. En el modelo universitario del
curso, todas las entidades heredan de una superclase de auditoría cuyo campo `createdBy` guarda el nombre de usuario
de un miembro del personal, así que incluir `Enrollment` añade `user.account.username` a una actividad sobre
estudiantes. Declara las categorías por clave cuando una entidad contiene más de lo que trata la actividad.

## CheckedActivity

`io.github.devbpm.linneo.processing.CheckedActivity` — `public record`, `@since 0.1.0`

```java
public record CheckedActivity(ProcessingActivity activity, List<DataUse> purposes, List<DataSubject> dataSubjects,
                              List<DataCategory> dataCategories, Protection protection,
                              List<ProcessingViolation> violations)
```

| Componente | Descripción |
|---|---|
| `activity` | La actividad tal como se declaró. |
| `purposes` | Los usos de datos, sin las claves desconocidas y con las obsoletas sustituidas. |
| `dataSubjects` | Los sujetos de datos, del mismo modo. |
| `dataCategories` | Las categorías de datos personales tratadas, de las claves declaradas y de los campos personales de las entidades y los objetos, en orden de clave. |
| `protection` | La menor [`Protection`](../protection-levels.md#protection) que cubre todos esos datos, incluidas las elevaciones en los campos; `Protection.LOWEST` si no hay ninguno. Su `confidentiality()`, `integrity()` y `secret()` son lo que el registro muestra como protección requerida. |
| `violations` | Todos los problemas encontrados, errores y avisos. |

El constructor canónico copia las listas (inmutables) y lanza `NullPointerException` si `activity`, `protection` o una
lista es `null`.

| Método | Descripción |
|---|---|
| `boolean isValid()` | `true` si no hay ningún error; los avisos se admiten. |
| `List<ProcessingViolation> errors()` | Las infracciones de gravedad `ERROR`. |
| `List<ProcessingViolation> warnings()` | Las infracciones de gravedad `WARNING`: los puntos abiertos del registro. |
| `Set<LegalNature> natures()` | Las naturalezas jurídicas de `dataCategories` (nunca `NONE`), todas ellas; `protection().natures()` conserva solo las más estrictas. |
| `boolean processesSpecialCategories()` | `true` si alguna categoría es `SPECIAL_CATEGORY` (artículo 9). |
| `boolean processesCriminalOffenceData()` | `true` si alguna categoría es `CRIMINAL_OFFENCE` (artículo 10). |
| `ImpactScreening screening()` | Si la actividad necesita una evaluación de impacto relativa a la protección de datos (artículo 35): su [cribado](impact-screening.md) frente al artículo 35.3 y los criterios de las directrices del CEPD. Se calcula a partir de la actividad cada vez; el registro lo muestra. |

```java
CheckedActivity checked = checker.check(admissions);
checked.isValid();                      // true
checked.natures();                      // [ORDINARY, SPECIAL_CATEGORY]
checked.protection().confidentiality(); // HIGH
checked.warnings();                     // []
checked.screening().outcome();          // POSSIBLE: los datos de categorías especiales son un criterio
```

## ProcessingViolation

`io.github.devbpm.linneo.processing.ProcessingViolation` — `public record`, `@since 0.1.0`

```java
public record ProcessingViolation(String activity, Severity severity, Code code, Element element, String message)
```

| Componente | Descripción |
|---|---|
| `activity` | La clave de la actividad. |
| `severity` | Si hace fallar la verificación. |
| `code` | Qué tipo de problema es. |
| `element` | La parte de la actividad en la que está el problema, que dice quién debe corregirlo; consulta [`Element`](#element). |
| `message` | Una descripción que nombra la clave o la disposición implicada. |

El constructor canónico lanza `NullPointerException` si algún componente es `null`.

| Método | Descripción |
|---|---|
| `boolean isError()` | `true` para la gravedad `ERROR`. |
| `String toString()` | `<SEVERITY> <CODE> <activity>: <message>`, por ejemplo `ERROR MISSING_LEGAL_BASIS newsletter: Personal data needs a lawful basis (GDPR article 6.1)`. No muestra el elemento. |

### Severity

`ProcessingViolation.Severity`

| Constante | Significado |
|---|---|
| `ERROR` | La actividad, tal como está declarada, sería ilícita o no se puede comprobar; `verify` falla e `isValid()` es `false`. |
| `WARNING` | El registro de la actividad está incompleto o debe revisarse; la verificación pasa y el mensaje es un punto abierto del registro. |

### Códigos

`ProcessingViolation.Code`: todos los valores, primero los errores, con el [elemento](#element) en el que se notifican y
el mensaje que les da el comprobador (`<k>` para una clave, `<a>` para un artículo). El elemento de un código que se
aplica a varias partes depende de la parte. Los mensajes están en inglés.

| Código | Gravedad | Elemento | Cuándo se produce | Mensaje |
|---|---|---|---|---|
| `UNKNOWN_KEY` | error | `PURPOSES`, `DATA_SUBJECTS` o `PERSONAL_DATA`, según el tipo de clave | Una clave de finalidad, interesado o categoría de datos no está en la taxonomía; un objeto de `objects` no está en el inventario de datos (o el comprobador no tiene ninguno); un campo de ese objeto declara una clave de categoría de datos que no está en la taxonomía. | `Unknown data use '<k>'` / `Unknown data subject '<k>'` / `Unknown data category '<k>'` / `Unknown object '<o>'` (seguido de `: the checker was given no data inventory` cuando no tiene ninguno) / `The field <o>.<path> has the unknown data category '<k>'` |
| `MISSING_PURPOSE` | error | `PURPOSES` | `purposes` está vacía. | `No data use declared` |
| `MISSING_DATA_SUBJECTS` | error | `DATA_SUBJECTS` | `dataSubjects` está vacía. | `No data subject declared` |
| `MISSING_PERSONAL_DATA` | error | `PERSONAL_DATA` | Ninguna categoría personal ni entidad u objeto con campos personales, y ninguna clave de categoría desconocida, error de entidad ni objeto desconocido que lo explique. | `The activity processes no personal data` |
| `ENTITY_NOT_CLASSIFIED` | error | `PERSONAL_DATA` | Una entidad de la lista tiene errores de clasificación. | `<nombre binario de la clase> has <n> classification error(s)` |
| `MISSING_LEGAL_BASIS` | error | `LEGAL_BASIS` | Datos personales sin `legalBasis`. | `Personal data needs a lawful basis (GDPR article 6.1)` |
| `MISSING_SPECIAL_CATEGORY_CONDITION` | error | `SPECIAL_CATEGORY_CONDITION` | Datos de categorías especiales sin `specialCategoryConditions`. | `Special categories of data (<keys>) need a condition of GDPR article 9.2` |
| `MISSING_CRIMINAL_DATA_BASIS` | error | `CRIMINAL_DATA_BASIS` | Datos penales sin `criminalDataBasis`. | `Criminal offence data (<keys>) needs a basis of GDPR article 10` |
| `CONSENT_NOT_SUFFICIENT` | error | `SPECIAL_CATEGORY_CONDITION` | Solo con `Jurisdiction.SPAIN`: la actividad trata `user.demographic.political_opinion`, `philosophical_belief`, `trade_union_membership`, `religious_belief`, `sexual_orientation` o `race_ethnicity` (o un descendiente) y su única condición del artículo 9.2 es `EXPLICIT_CONSENT` (LOPDGDD, artículo 9.1). | `Explicit consent alone does not lift the prohibition of processing <keys> (LOPDGDD article 9.1); declare another condition of GDPR article 9.2` |
| `IMPACT_ASSESSMENT_REQUIRED` | error | `IMPACT_ASSESSMENT` | El [cribado](impact-screening.md) de la actividad encuentra un caso del artículo 35.3 del RGPD y no hay `impactAssessment`. | `GDPR article <a> requires an impact assessment; refer to it. <summary>`, donde `<a>` es `35.3.a`, `35.3.b` o `35.3.c` y `<summary>` es `ImpactScreening.summary()` |
| `DUPLICATE_ACTIVITY` | error | `KEY` | Solo en `checkAll`/`verify`: una actividad anterior tiene la misma clave. | `Another activity of the record has the key '<k>'` |
| `DEPRECATED_KEY` | **aviso** | `PURPOSES`, `DATA_SUBJECTS` o `PERSONAL_DATA`, según el tipo de clave | Una clave de finalidad, interesado o categoría está obsoleta, o un campo de un objeto de `objects` declara una clave de categoría de datos obsoleta; la actividad se comprueba con su sustituta. | `The data use '<k>' is deprecated; checked as '<r>'` (igual con `data subject` y `data category`; sin la parte `; checked as` cuando la clave no tiene sustituto) / `The field <o>.<path> has the deprecated data category '<k>'; checked as '<r>'` (misma omisión) |
| `NOT_PERSONAL_DATA` | **aviso** | `PERSONAL_DATA` | Una categoría declarada no es dato personal; queda fuera del registro. | `The data category '<k>' is not personal data and is left out of the record` |
| `UNNEEDED_CONDITION` | **aviso** | `SPECIAL_CATEGORY_CONDITION` para una condición del artículo 9.2, `CRIMINAL_DATA_BASIS` para una base del artículo 10, `LEGITIMATE_INTEREST` para un interés legítimo | Se declara una condición del artículo 9.2 o una base del artículo 10 para datos que la actividad no trata, o se indica un interés legítimo con otra base jurídica. | `Article(s) <a>, <a> declared but the activity processes no special category of data` / `Article 10 is declared but the activity processes no criminal offence data` / `A legitimate interest is stated but the lawful basis is article <a>` |
| `MISSING_LEGAL_REFERENCE` | **aviso** | `LEGAL_REFERENCE` | Se aplica una base que descansa en una ley y no se indica `legalReference`; o, con `Jurisdiction.SPAIN`, la actividad trata datos sobre infracciones y sanciones administrativas (`user.administrative_sanction` o descendientes) con una base distinta del consentimiento. | `GDPR article(s) <a>, <a> rest on Union or Member State law; name it as the legal reference`; con `Jurisdiction.SPAIN` termina con ` (LOPDGDD article(s) <a>: a law with the rank of statute)` con las disposiciones que se aplican: `8` para una obligación legal o una misión de interés público, `9.2` para una condición `SUBSTANTIAL_PUBLIC_INTEREST`, `HEALTH_OR_SOCIAL_CARE` o `PUBLIC_HEALTH`, `10` para datos penales autorizados por ley. Para las sanciones administrativas: `Data on administrative infringements and sanctions may only be processed by the bodies competent for the sanctioning procedure, with the consent of the person, or as a law with the rank of statute allows (LOPDGDD article 27); name that law as the legal reference` |
| `MISSING_LEGITIMATE_INTEREST` | **aviso** | `LEGITIMATE_INTEREST` | La base es `LEGITIMATE_INTERESTS` y no se indica `legitimateInterest`. | `State the legitimate interest pursued (GDPR article 6.1.f)` |
| `MISSING_RETENTION` | **aviso** | `RETENTION` | Datos personales sin `retention`. | `Declare the envisaged time limit for erasure (GDPR article 30.1.f)` |
| `IMPACT_ASSESSMENT_RECOMMENDED` | **aviso** | `IMPACT_ASSESSMENT` | El [cribado](impact-screening.md) de la actividad cumple dos criterios o más, así que el artículo 35 probablemente exige una evaluación de impacto, y no hay `impactAssessment`. | `GDPR article 35 likely requires an impact assessment. <summary>`, donde `<summary>` es `ImpactScreening.summary()`; con `Jurisdiction.SPAIN`, `GDPR article 35 likely requires an impact assessment (the AEPD's list under article 35.4 requires one in most cases that meet two criteria or more). <summary>` |

Salida real para una actividad de estudiantes que trata `user.health_and_medical.disability` y `user.criminal_history` y
a la que le faltan ambas condiciones. Ambos son datos sensibles, un solo criterio, así que el cribado es `POSSIBLE` y
no genera nada:

```text
ERROR MISSING_SPECIAL_CATEGORY_CONDITION screening: Special categories of data (user.health_and_medical.disability) need a condition of GDPR article 9.2
ERROR MISSING_CRIMINAL_DATA_BASIS screening: Criminal offence data (user.criminal_history) needs a basis of GDPR article 10
WARNING MISSING_LEGAL_REFERENCE screening: GDPR article(s) 6.1.c rest on Union or Member State law; name it as the legal reference
WARNING MISSING_RETENTION screening: Declare the envisaged time limit for erasure (GDPR article 30.1.f)
```

y para la nómina de empleados, cuya cuenta bancaria son datos de naturaleza muy personal (criterio 4) y que son
interesados vulnerables (criterio 7):

```text
WARNING IMPACT_ASSESSMENT_RECOMMENDED payroll: GDPR article 35 likely requires an impact assessment. Likely required (2 criteria of WP248): 4. sensitive data: highly personal data user.financial.bank_account; 7. vulnerable subjects: data subjects employee
```

Con `riskFactors(Factor.LARGE_SCALE)` en la actividad anterior que trata datos de categorías especiales y datos penales,
el cribado es un caso del artículo 35.3.b y el comprobador añade un error:

```text
ERROR IMPACT_ASSESSMENT_REQUIRED screening: GDPR article 35.3.b requires an impact assessment; refer to it. Required (GDPR art. 35.3.b): 4. sensitive data: criminal offence data user.criminal_history; special category data user.health_and_medical.disability; 5. large scale: declared LARGE_SCALE
```

y para una con tres declaraciones innecesarias (`legitimateInterest(...)` seguido de `legalBasis(CONSENT)`, más
`EXPLICIT_CONSENT` y `OFFICIAL_AUTHORITY` en una actividad que solo trata `user.name`):

```text
WARNING UNNEEDED_CONDITION newsletter: A legitimate interest is stated but the lawful basis is article 6.1.a
WARNING UNNEEDED_CONDITION newsletter: Article(s) 9.2.a declared but the activity processes no special category of data
WARNING UNNEEDED_CONDITION newsletter: Article 10 is declared but the activity processes no criminal offence data
```

### Element

`ProcessingViolation.Element` — `public enum`, `@since 0.1.0`

La parte de una actividad de tratamiento en la que está un problema. El **alcance** de la actividad (`KEY`, `PURPOSES`,
`DATA_SUBJECTS`, `PERSONAL_DATA`) se declara en el código, así que un problema ahí es para los desarrolladores y debe
hacer fallar la compilación. El resto es la [gobernanza](activity.md#governance) que mantiene la función de protección
de datos, así que un problema ahí corresponde a la pantalla donde se edita.

| Constante | `isGovernance()` | La parte |
|---|---|---|
| `KEY` | `false` | La clave que identifica la actividad en el registro. |
| `PURPOSES` | `false` | Las finalidades (`ProcessingActivity.purposes()`). |
| `DATA_SUBJECTS` | `false` | Los interesados (`ProcessingActivity.dataSubjects()`). |
| `PERSONAL_DATA` | `false` | Los datos personales: categorías de datos, entidades y objetos. |
| `LEGAL_BASIS` | `true` | La base jurídica (`Governance.legalBasis()`). |
| `LEGITIMATE_INTEREST` | `true` | El interés legítimo perseguido (`Governance.legitimateInterest()`). |
| `SPECIAL_CATEGORY_CONDITION` | `true` | Las condiciones del artículo 9.2 (`Governance.specialCategoryConditions()`). |
| `CRIMINAL_DATA_BASIS` | `true` | La base del artículo 10 (`Governance.criminalDataBasis()`). |
| `LEGAL_REFERENCE` | `true` | La norma en la que descansan las bases (`Governance.legalReference()`). |
| `RETENTION` | `true` | El plazo previsto para la supresión (`Governance.retention()`). |
| `IMPACT_ASSESSMENT` | `true` | La evaluación de impacto (`Governance.impactAssessment()`). |

`boolean isGovernance()` es `true` para los elementos de [`Governance`](activity.md#governance) y `false` para los del
alcance. No hay elemento para los destinatarios ni para las transferencias, porque el comprobador no los examina. El
elemento de cada código está en la [tabla de arriba](#códigos).

Úsalo para enviar cada problema a quien puede corregirlo: haz fallar la compilación con los errores del alcance y
muestra las lagunas de la gobernanza donde las edita el delegado. `verify` falla con **todos** los errores, también los
de gobernanza, así que una comprobación de compilación que deba fallar solo por el alcance usa `checkAll` y filtra por
elemento:

```java
import io.github.devbpm.linneo.processing.ProcessingViolation.Element;

// En un test: el alcance se declara en el código, así que sus errores son de los desarrolladores.
List<ProcessingViolation> scopeErrors = checker.checkAll(activities).stream()
        .flatMap(checked -> checked.errors().stream())
        .filter(violation -> !violation.element().isGovernance())
        .toList();
assertTrue(scopeErrors.isEmpty(), () -> "Fix the scope of the activities: " + scopeErrors);

// Donde el delegado edita la gobernanza de una actividad: cada laguna junto a su campo.
CheckedActivity edited = checker.check(activity.withGovernance(stored));
Map<Element, List<ProcessingViolation>> gaps = edited.violations().stream()
        .filter(violation -> violation.element().isGovernance())
        .collect(Collectors.groupingBy(ProcessingViolation::element));
List<ProcessingViolation> beside = gaps.getOrDefault(Element.RETENTION, List.of());
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
[`ProcessingViolation.toString()`](#processingviolation). Lista los 50 primeros errores; si hay más, una última línea
indica cuántos quedaron fuera y cuenta todos los errores por código, en orden alfabético, como en
`... and 70 more; all of them by code: MISSING_PURPOSE 60, UNKNOWN_KEY 60`. `errors()` siempre contiene todos.
Verificar dos veces la misma actividad más una vacía da:

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
