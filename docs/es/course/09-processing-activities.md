---
title: "9. Actividades de tratamiento y el registro"
parent: "Curso"
nav_order: 9
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/LegalBasis.java
  - src/main/java/io/github/devbpm/linneo/processing/SpecialCategoryCondition.java
  - src/main/java/io/github/devbpm/linneo/processing/CriminalDataBasis.java
  - src/main/java/io/github/devbpm/linneo/processing/TransferSafeguard.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingChecker.java
  - src/main/java/io/github/devbpm/linneo/processing/CheckedActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingViolation.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingException.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingRecord.java
-->

# Módulo 9 — Actividades de tratamiento y el registro

## Objetivos de aprendizaje

Al terminar este módulo serás capaz de:

- declarar las actividades de tratamiento de una organización con `ProcessingActivity`: finalidades, interesados,
  datos personales, base jurídica, condiciones para categorías especiales y datos penales, conservación,
  destinatarios y transferencias;
- elegir entre indicar los datos personales como claves de categoría o como entidades clasificadas, y saber qué
  aporta cada opción;
- leer las infracciones de un `ProcessingChecker` y distinguir un error (ilícito tal como está declarado) de un aviso
  (una laguna del registro);
- verificar las actividades en un test y generar el registro de actividades de tratamiento en Markdown y JSON.

Los módulos 3 y 4 son necesarios: las actividades reutilizan el modelo clasificado de la universidad.

## De los campos a los procesos

El módulo 5 montó a mano un informe tipo RAT y terminó con una lista de lo que aún faltaba: bases jurídicas,
destinatarios, plazos de conservación y transferencias. Son propiedades de los **procesos**, no de los campos. El
mismo `Grade.mark` se trata para evaluar a los estudiantes, para expedir certificados y para calcular estadísticas,
cada vez con una finalidad distinta y quizá con una base distinta.

El paquete `io.github.devbpm.linneo.processing` modela esos procesos. Una `ProcessingActivity` reúne las tres
dimensiones de la taxonomía que presentó el módulo 2:

| Taxonomía | En la actividad | Registro del RGPD (art. 30.1) |
|---|---|---|
| usos de datos | `purposes` | b — fines del tratamiento |
| sujetos de datos | `dataSubjects` | c — categorías de interesados |
| categorías de datos | `dataCategories` y `entities` | c — categorías de datos personales |

y añade lo que la taxonomía no puede saber: la base jurídica (art. 6.1), la condición para las categorías especiales
(art. 9.2), la base para los datos penales (art. 10), la ley en la que descansan esas bases, la conservación, los
destinatarios, las transferencias y la evaluación de impacto.

> **Nota de protección de datos.** Linneo comprueba *declaraciones*: que se nombra una base donde el RGPD la exige,
> que las claves existen, que las entidades están clasificadas. Si una base es la adecuada, si un interés prevalece
> sobre los derechos de los antiguos alumnos o si una ley ampara realmente el tratamiento lo deciden el responsable y
> su delegado de protección de datos. Las leyes que aparecen en este módulo («Higher Education Act», «Child Protection
> Act») son ficticias.

En este módulo, los nombres, descripciones y demás textos de las actividades están en inglés, como los textos que
añade el registro (lo verás más adelante): así el registro queda en un solo idioma. Las salidas que se muestran son
las reales de ese código.

## Una primera actividad

La universidad envía un boletín a sus antiguos alumnos. Un primer borrador:

```java
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.processing.*;   // ProcessingActivity, ProcessingChecker, LegalBasis…

import static io.github.devbpm.linneo.Categories.*;

ProcessingChecker checker = new ProcessingChecker(Taxonomy.load());

ProcessingActivity draft = ProcessingActivity.builder("alumni-newsletter", "Alumni newsletter")
        .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL)
        .dataSubjects(DataSubjects.ALUMNUS)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL)
        .build();

CheckedActivity checked = checker.check(draft);
checked.violations().forEach(System.out::println);
```

```text
ERROR MISSING_LEGAL_BASIS alumni-newsletter: Personal data needs a lawful basis (GDPR article 6.1)
WARNING MISSING_RETENTION alumni-newsletter: Declare the envisaged time limit for erasure (GDPR article 30.1.f)
```

`check` nunca lanza excepciones por un problema de la actividad: devuelve una `CheckedActivity` con los términos
resueltos, los datos personales, la protección que requieren y todas las infracciones. Aquí `checked.isValid()` es
`false`.

Las dos líneas muestran la regla de todo el paquete:

- un **error** es algo que haría la actividad **ilícita tal como está declarada**, o imposible de comprobar. Tratar
  datos personales sin base jurídica es el caso más claro;
- un **aviso** es una **laguna del registro**: la actividad puede ser lícita, pero el registro está incompleto o hay
  algo que revisar. La falta de plazo de conservación es uno.

La universidad se basa en su interés legítimo en mantener el contacto con sus antiguos alumnos. Con solo
`legalBasis(LegalBasis.LEGITIMATE_INTERESTS)` el error desaparece, pero aparece un aviso nuevo:

```text
WARNING MISSING_LEGITIMATE_INTEREST alumni-newsletter: State the legitimate interest pursued (GDPR article 6.1.f)
WARNING MISSING_RETENTION alumni-newsletter: Declare the envisaged time limit for erasure (GDPR article 30.1.f)
```

`legitimateInterest(...)` fija la base **e** indica el interés en una sola llamada. La actividad completa no tiene
infracciones:

```java
ProcessingActivity newsletter = ProcessingActivity.builder("alumni-newsletter", "Alumni newsletter")
        .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL)
        .dataSubjects(DataSubjects.ALUMNUS)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL)
        .legitimateInterest("Keeping former students informed about university events and alumni services")
        .retention("Until the alumnus unsubscribes")
        .recipients("Email delivery provider (processor)")
        .transfer("United States", TransferSafeguard.STANDARD_CONTRACTUAL_CLAUSES)
        .build();
```

El proveedor de correo guarda la lista en un tercer país, así que la actividad declara una **transferencia** con su
garantía, aquí cláusulas contractuales tipo. El comprobador no juzga las transferencias; el registro las enumera.

## Categorías especiales: la admisión

Los solicitantes pueden pedir las plazas reservadas a estudiantes con discapacidad. La admisión trata entonces
`user.health_and_medical.disability`, una **categoría especial** (módulo 1). El artículo 9 prohíbe tratarla salvo que
se aplique una de las condiciones del artículo 9.2, **además** de la base jurídica. Sin ninguna:

```java
ProcessingActivity admissions = ProcessingActivity.builder("admissions", "Admissions")
        .purposes(DataUses.EDUCATION_ADMISSION)
        .dataSubjects(DataSubjects.APPLICANT)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL, USER_EDUCATION_PREVIOUS_EDUCATION,
                USER_EDUCATION_TEST_SCORE, USER_HEALTH_AND_MEDICAL_DISABILITY)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .legalReference("Higher Education Act, art. 12")
        .retention("Until the end of the admission cycle")
        .build();
```

```text
ERROR MISSING_SPECIAL_CATEGORY_CONDITION admissions: Special categories of data (user.health_and_medical.disability) need a condition of GDPR article 9.2
WARNING IMPACT_ASSESSMENT_RECOMMENDED admissions: The activity involves special categories of data; GDPR article 35 may require an impact assessment
```

El error nombra la categoría que lo provoca. La solución es una `SpecialCategoryCondition` —la ley de admisión de la
universidad reserva plazas para estudiantes con discapacidad, así que `SUBSTANTIAL_PUBLIC_INTEREST` (art. 9.2.g)— y
una referencia a la evaluación de impacto que ha hecho el delegado de protección de datos:

```java
        .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
        .legalReference("Higher Education Act, arts. 12 and 31")
        .impactAssessment("DPIA-2026-01")
```

El artículo 9.2.g, como la base de interés público del artículo 6.1.e, debe descansar en una ley:
`requiresLegalReference()` es `true` para ambos. Sin `legalReference`, el comprobador añadiría el aviso
`MISSING_LEGAL_REFERENCE` con los dos artículos. Un solo texto de `legalReference` cubre todas las bases de la
actividad, así que nombra en él todas las leyes implicadas.

Por defecto, el comprobador aplica solo el RGPD. Para una institución española, créalo con una jurisdicción:
`new ProcessingChecker(Taxonomy.load(), Jurisdiction.SPAIN)`. Entonces informa también del error
`CONSENT_NOT_SUFFICIENT` cuando el consentimiento explícito es la única condición para creencias, afiliación
sindical u otros datos del artículo 9.1 de la LOPDGDD, y avisa en `MISSING_LEGAL_REFERENCE` de que los artículos
9.2.g, h e i exigen una norma con rango de ley.

La página de referencia [Bases jurídicas](../reference/processing/bases.md) enumera cada constante con su artículo y
su nombre en fideslang.

## Datos penales: la verificación de antecedentes del personal

Antes de contratar a alguien para un puesto en contacto con menores (campamentos de verano, actividades con colegios),
la universidad comprueba que el candidato no tiene condenas por delitos sexuales. `user.criminal_history` son **datos
penales** (art. 10), que necesitan su propia base:

```text
ERROR MISSING_CRIMINAL_DATA_BASIS staff-screening: Criminal offence data (user.criminal_history) needs a basis of GDPR article 10
WARNING MISSING_LEGAL_REFERENCE staff-screening: GDPR article(s) 6.1.c rest on Union or Member State law; name it as the legal reference
WARNING IMPACT_ASSESSMENT_RECOMMENDED staff-screening: The activity involves criminal offence data; GDPR article 35 may require an impact assessment
```

`CriminalDataBasis` tiene dos valores: `OFFICIAL_AUTHORITY` y `AUTHORISED_BY_LAW`. La verificación la exige la
(ficticia) Child Protection Act, así que la base es `AUTHORISED_BY_LAW` y esa ley es la referencia legal.

## Claves o entidades: la matrícula

Los datos personales de una actividad pueden indicarse como claves de categoría (`dataCategories`), como clases del
modelo clasificadas (`entities`) o de ambas formas. Para una entidad, el comprobador ejecuta el `Classifier` del
módulo 4 y toma las categorías de sus **campos personales**, con los niveles declarados en cada campo:

- si se añade un campo personal a `Enrollment`, todas las actividades que incluyen `Enrollment` pasan a tratarlo, sin
  tocar las actividades;
- una entidad con errores de clasificación hace fallar la actividad (`ENTITY_NOT_CLASSIFIED`);
- los campos agregados y anonimizados, como los de `CourseStatistics`, no son datos personales y se descartan;
- un campo con la confidencialidad elevada (`Student.counsellingNotes`) eleva la protección de la actividad.

La otra cara es que una entidad aporta **todos** sus campos personales. Si la matrícula incluyera `Student.class`:

```java
ProcessingActivity enrollment = ProcessingActivity.builder("enrollment", "Student enrollment")
        .purposes(DataUses.EDUCATION_ENROLLMENT)
        .dataSubjects(DataSubjects.STUDENT)
        .entities(Student.class, Enrollment.class)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .legalReference("Higher Education Act, art. 12")
        .retention("Six years after the student leaves")
        .build();
```

```text
ERROR MISSING_SPECIAL_CATEGORY_CONDITION enrollment: Special categories of data (user.health_and_medical.disability) need a condition of GDPR article 9.2
WARNING IMPACT_ASSESSMENT_RECOMMENDED enrollment: The activity involves special categories of data; GDPR article 35 may require an impact assessment
```

`Student.disabilityDegree` ha entrado con la entidad, junto con las notas de orientación y el identificador de
analítica. La matrícula no los usa, así que la universidad incluye `Enrollment` y declara como claves los pocos campos
del estudiante que sí usa. Observa también que `Enrollment` hereda de `AuditedEntity`, cuyo `createdBy` es el nombre de
usuario del miembro del personal que creó la fila: añade `user.account.username` a la actividad. Una entidad contiene
lo que contiene, no solo datos de los interesados de la actividad; revisa las categorías que notifica el comprobador.

## Las actividades de la universidad

Las cuatro actividades en una clase, junto al modelo:

```java
package edu.example.university;

import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.processing.CriminalDataBasis;
import io.github.devbpm.linneo.processing.LegalBasis;
import io.github.devbpm.linneo.processing.ProcessingActivity;
import io.github.devbpm.linneo.processing.SpecialCategoryCondition;
import io.github.devbpm.linneo.processing.TransferSafeguard;
import java.util.List;

import static io.github.devbpm.linneo.Categories.*;

/** Las actividades de tratamiento de la universidad. */
public final class UniversityActivities {

    private UniversityActivities() {
    }

    public static final ProcessingActivity ENROLLMENT = ProcessingActivity.builder("enrollment", "Student enrollment")
            .description("Registers admitted students in a programme and an academic year, and charges the fees.")
            .purposes(DataUses.EDUCATION_ENROLLMENT)
            .dataSubjects(DataSubjects.STUDENT)
            .entities(Enrollment.class)
            .dataCategories(USER_NAME, USER_GOVERNMENT_ID_NATIONAL_IDENTIFICATION_NUMBER, USER_CONTACT_EMAIL)
            .legalBasis(LegalBasis.PUBLIC_INTEREST)
            .legalReference("Higher Education Act, art. 12")
            .recipients("Bank that collects the tuition fees")
            .build();

    public static final ProcessingActivity ADMISSIONS = ProcessingActivity.builder("admissions", "Admissions")
            .description("Receives and assesses applications, including requests for the disability quota.")
            .purposes(DataUses.EDUCATION_ADMISSION)
            .dataSubjects(DataSubjects.APPLICANT)
            .dataCategories(USER_NAME, USER_CONTACT_EMAIL, USER_EDUCATION_PREVIOUS_EDUCATION,
                    USER_EDUCATION_TEST_SCORE, USER_HEALTH_AND_MEDICAL_DISABILITY)
            .legalBasis(LegalBasis.PUBLIC_INTEREST)
            .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
            .legalReference("Higher Education Act, arts. 12 and 31")
            .retention("Until the end of the admission cycle; admitted applicants move to the student record")
            .recipients("Admissions board")
            .impactAssessment("DPIA-2026-01")
            .build();

    public static final ProcessingActivity STAFF_SCREENING = ProcessingActivity.builder("staff-screening",
                    "Staff screening")
            .description("Checks that candidates for posts in contact with minors have no sexual offence convictions.")
            .purposes(DataUses.EMPLOYMENT_RECRUITMENT)
            .dataSubjects(DataSubjects.JOB_APPLICANT)
            .dataCategories(USER_NAME, USER_GOVERNMENT_ID_NATIONAL_IDENTIFICATION_NUMBER, USER_CRIMINAL_HISTORY)
            .legalBasis(LegalBasis.LEGAL_OBLIGATION)
            .criminalDataBasis(CriminalDataBasis.AUTHORISED_BY_LAW)
            .legalReference("Child Protection Act, art. 13")
            .retention("Until the hiring decision; afterwards only whether the certificate was clear")
            .impactAssessment("DPIA-2026-02")
            .build();

    public static final ProcessingActivity ALUMNI_NEWSLETTER = ProcessingActivity.builder("alumni-newsletter",
                    "Alumni newsletter")
            .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL)
            .dataSubjects(DataSubjects.ALUMNUS)
            .dataCategories(USER_NAME, USER_CONTACT_EMAIL)
            .legitimateInterest("Keeping former students informed about university events and alumni services")
            .retention("Until the alumnus unsubscribes")
            .recipients("Email delivery provider (processor)")
            .transfer("United States", TransferSafeguard.STANDARD_CONTRACTUAL_CLAUSES)
            .build();

    public static List<ProcessingActivity> all() {
        return List.of(ENROLLMENT, ADMISSIONS, STAFF_SCREENING, ALUMNI_NEWSLETTER);
    }
}
```

Lo que el comprobador obtiene de cada una:

| Actividad | Datos personales | Naturalezas | Protección requerida | Infracciones |
|---|---|---|---|---|
| `enrollment` | 9 categorías, 6 de ellas de `Enrollment` | `ORDINARY` | confidencialidad `MEDIUM`, integridad `HIGH` | aviso `MISSING_RETENTION` |
| `admissions` | 5 categorías | `ORDINARY`, `SPECIAL_CATEGORY` | `HIGH`, `HIGH` | ninguna |
| `staff-screening` | 3 categorías | `ORDINARY`, `CRIMINAL_OFFENCE` | `HIGH`, `HIGH` | ninguna |
| `alumni-newsletter` | 2 categorías | `ORDINARY` | `LOW`, `MEDIUM` | ninguna |

La actividad de matrícula se ha dejado a propósito sin plazo de conservación: un aviso no detiene el registro.

## Verificar en un test

Como con el clasificador, lo importante es ejecutar la comprobación en cada compilación. `verify` comprueba todas las
actividades y lanza una `ProcessingException` con **todos los errores de todas las actividades**; si no hay ninguno,
devuelve el `ProcessingRecord`:

```java
package edu.example.university;

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.processing.ProcessingChecker;
import io.github.devbpm.linneo.processing.ProcessingRecord;
import io.github.devbpm.linneo.processing.ProcessingViolation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class ProcessingRecordTest {

    @Test
    void recordOfProcessingActivities() throws IOException {
        ProcessingRecord record = new ProcessingChecker(Taxonomy.load()).verify(UniversityActivities.all());

        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/record-of-processing.md"), record.toMarkdown("Example University"));
        Files.writeString(Path.of("target/record-of-processing.json"), record.toJson());

        List<ProcessingViolation> openPoints = record.activities().stream()
                .flatMap(activity -> activity.warnings().stream())
                .toList();
        openPoints.forEach(System.out::println);
    }
}
```

Con los borradores anteriores de la admisión y de la verificación de antecedentes (sin condición del artículo 9.2 ni
base del artículo 10) en la lista, el test falla con:

```text
io.github.devbpm.linneo.processing.ProcessingException: 2 processing error(s):
 - ERROR MISSING_SPECIAL_CATEGORY_CONDITION admissions: Special categories of data (user.health_and_medical.disability) need a condition of GDPR article 9.2
 - ERROR MISSING_CRIMINAL_DATA_BASIS staff-screening: Criminal offence data (user.criminal_history) needs a basis of GDPR article 10
```

Cada línea es `<SEVERITY> <CODE> <clave de la actividad>: <mensaje>`. Solo se enumeran los errores; los avisos de esas
actividades siguen en su `CheckedActivity`. Dos actividades con la misma clave también son un error
(`DUPLICATE_ACTIVITY`), porque la clave identifica la actividad en el registro. Usa `Taxonomy.load()`, como en el
módulo 4, para que se conozcan los términos de tu extensión.

La [tabla de códigos](../reference/processing/checker.md#códigos) enumera cada código con su gravedad y su mensaje.

## El registro

Con las cuatro actividades definitivas el test pasa y escribe el registro. **Sus textos están en inglés** —las
etiquetas de las filas, `None declared`, `Open points:` y los mensajes—, sea cual sea el idioma de tu aplicación; los
nombres de los términos vienen de la taxonomía y el resto se escribe tal como lo declaraste. El principio de
`record-of-processing.md`:

```markdown
# Example University

## Student enrollment

`enrollment`: Registers admitted students in a programme and an academic year, and charges the fees.

| Record item | Content |
|---|---|
| Purposes (art. 30.1.b) | Enrollment (`education.enrollment`) |
| Lawful basis (art. 6.1) | Art. 6.1.e: Public interest |
| Legal reference | Higher Education Act, art. 12 |
| Data subjects (art. 30.1.c) | Student (`student`) |
| Personal data (art. 30.1.c) | Account Username (`user.account.username`); User Contact Email (`user.contact.email`); Enrollment (`user.education.enrollment`); Program (`user.education.program`); Fee (`user.financial.fee`); Scholarship (`user.financial.scholarship`); National Identification Number (`user.government_id.national_identification_number`); Name (`user.name`); Record Reference (`user.unique_id.record_reference`) |
| Recipients (art. 30.1.d) | Bank that collects the tuition fees |
| Transfers (art. 30.1.e) | None declared |
| Retention (art. 30.1.f) | Not declared |
| Required protection (art. 30.1.g) | Confidentiality Medium, integrity High |
| Impact assessment (art. 35) | None |

Open points:

- Declare the envisaged time limit for erasure (GDPR article 30.1.f)

## Admissions
…
```

y la sección de la verificación de antecedentes:

```markdown
## Staff screening

`staff-screening`: Checks that candidates for posts in contact with minors have no sexual offence convictions.

| Record item | Content |
|---|---|
| Purposes (art. 30.1.b) | Employment Recruitment (`employment.recruitment`) |
| Lawful basis (art. 6.1) | Art. 6.1.c: Legal obligations |
| Criminal offence data (art. 10) | Art. 10: Authorised by law |
| Legal reference | Child Protection Act, art. 13 |
| Data subjects (art. 30.1.c) | Job Applicant (`job_applicant`) |
| Personal data (art. 30.1.c) | Criminal History (`user.criminal_history`, criminal offence data); National Identification Number (`user.government_id.national_identification_number`); Name (`user.name`) |
| Recipients (art. 30.1.d) | None declared |
| Transfers (art. 30.1.e) | None declared |
| Retention (art. 30.1.f) | Until the hiring decision; afterwards only whether the certificate was clear |
| Required protection (art. 30.1.g) | Confidentiality High, integrity High |
| Impact assessment (art. 35) | DPIA-2026-02 |
```

La fila «Lawful basis» muestra el nombre fideslang de la base (`Legal obligations`), y la fila «Required protection»
los niveles más altos de los datos, como punto de partida para la descripción de las medidas de seguridad del
artículo 30.1.g. Cada fila se explica en la página de referencia [ProcessingRecord](../reference/processing/record.md).

`toJson()` da el mismo contenido para las herramientas. Solo el boletín, con `checker.verify(ALUMNI_NEWSLETTER).toJson()`:

```json
{"activities":[{"key":"alumni-newsletter","name":"Alumni newsletter","purposes":["marketing.communications.email"],"dataSubjects":["alumnus"],"dataCategories":["user.contact.email","user.name"],"legalBasis":{"code":"LEGITIMATE_INTERESTS","article":"6.1.f","fideslang":"Legitimate interests"},"legitimateInterest":"Keeping former students informed about university events and alumni services","recipients":["Email delivery provider (processor)"],"transfers":[{"destination":"United States","safeguard":{"code":"STANDARD_CONTRACTUAL_CLAUSES","article":"46.2.c","fideslang":"SCCs"}}],"retention":"Until the alumnus unsubscribes","protection":{"natures":["ORDINARY"],"confidentiality":"LOW","integrity":"MEDIUM","secret":false},"openPoints":[]}]}
```

El registro es un punto de partida para la delegada de protección de datos, generado a partir de las mismas
declaraciones que comprueba la compilación. No sustituye su revisión: ella sigue decidiendo si las bases son las
adecuadas y completa lo que el registro no puede saber, como los datos de contacto del responsable.

## Errores frecuentes

- **Esperar que Linneo decida la base jurídica.** Comprueba que hay una declarada y que están las condiciones que
  necesitan los datos, no que sean las adecuadas.
- **Incluir una entidad que contiene más de lo que trata la actividad.** `Student.class` en la matrícula arrastra el
  grado de discapacidad y las notas de orientación. Declara las categorías por clave o divide la entidad.
- **Llamar a `legalBasis(...)` después de `legitimateInterest(...)`.** Gana la última llamada: la base cambia y el
  interés se queda, lo que el comprobador notifica como `UNNEEDED_CONDITION` y el registro omite.
- **Declarar una condición «por si acaso».** `EXPLICIT_CONSENT` en una actividad sin categorías especiales es el aviso
  `UNNEEDED_CONDITION`: el registro debe reflejar lo que se aplica, no todo lo que podría aplicarse.
- **Tratar los avisos como ruido.** Son los puntos abiertos del registro; cada uno es algo por lo que preguntará la
  delegada de protección de datos.
- **Comprobar con `Taxonomy.standard()` cuando las actividades usan términos de una extensión** → `UNKNOWN_KEY`.
- **Reutilizar una clave para una segunda actividad** → `DUPLICATE_ACTIVITY`. Las claves identifican las actividades
  en el registro y en las herramientas que leen el JSON; mantenlas estables.

## Ejercicios

**Ejercicio 9.1 — Predice.** La universidad añade una actividad de evaluación:

```java
ProcessingActivity grading = ProcessingActivity.builder("grading", "Assessment and grading")
        .purposes(DataUses.EDUCATION_ASSESSMENT)
        .dataSubjects(DataSubjects.STUDENT, DataSubjects.FACULTY_MEMBER)
        .entities(Enrollment.class, Grade.class)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .retention("Permanently, as part of the academic record")
        .build();
```

Sin ejecutarla, di qué infracciones notifica `check`, si la actividad es válida y qué confidencialidad e integridad
requiere. Después ejecútala.

**Ejercicio 9.2 — Lee y corrige.** La rama de un compañero hace fallar el test con este mensaje. Para cada línea, di
qué está mal y cómo corregirlo.

```text
3 processing error(s):
 - ERROR UNKNOWN_KEY staff-screening: Unknown data subject 'job_applicants'
 - ERROR MISSING_CRIMINAL_DATA_BASIS staff-screening: Criminal offence data (user.criminal_history) needs a basis of GDPR article 10
 - ERROR DUPLICATE_ACTIVITY enrollment: Another activity of the record has the key 'enrollment'
```

(La tercera línea viene de una actividad nueva, «Student enrollment (fees)», que trata el cobro de las tasas de
matrícula.)

**Ejercicio 9.3 — Del interés legítimo al consentimiento.** La universidad decide pedir el consentimiento de los
antiguos alumnos antes de enviarles el boletín. Un compañero añade `.legalBasis(LegalBasis.CONSENT)` después de la
llamada a `legitimateInterest(...)` en `ALUMNI_NEWSLETTER`. ¿Qué notifica `check` y qué muestra el registro sobre el
interés legítimo? ¿Cuál es el cambio correcto?

**Ejercicio 9.4 — Cierra los puntos abiertos.** Da un plazo de conservación a la actividad de matrícula y añade un
test que falle cuando el registro tenga algún punto abierto, con un mensaje que los enumere.

**Ejercicio 9.5 — Administración de personal.** Declara una actividad «Staff administration» (finalidad
`DataUses.EMPLOYMENT`, interesados `EMPLOYEE` y `STAFF_MEMBER`, entidad `Staff`, base `CONTRACT`, un plazo de
conservación). Predice qué notifica el comprobador, teniendo en cuenta la clase `Staff` del módulo 3, y decide cómo
corregirlo.

## Cuestionario de autoevaluación

1. ¿La falta de plazo de conservación hace que `verify` lance una excepción?
2. Una actividad trata `user.health_and_medical.disability` con base en el consentimiento. ¿Qué más debe declarar?
3. ¿Qué hace `legitimateInterest("…")` con la base jurídica?
4. ¿Por qué falla con `MISSING_PERSONAL_DATA` una actividad cuya única entidad es `CourseStatistics`?
5. ¿Qué bases hacen que el comprobador pida una `legalReference`?
6. ¿En qué idioma están los textos del registro en Markdown?
7. ¿Un registro válido significa que el tratamiento es lícito?

## Lecturas recomendadas

- [Referencia — Actividades de tratamiento](../reference/processing/README.md)
- [Referencia — ProcessingActivity](../reference/processing/activity.md)
- [Referencia — Bases jurídicas](../reference/processing/bases.md)
- [Referencia — ProcessingChecker](../reference/processing/checker.md): cada código con su mensaje
- [Referencia — ProcessingRecord](../reference/processing/record.md)

---

← Anterior: [Módulo 8 — DataHub y Apache Atlas](08-datahub-and-atlas.md) · Volver a la
[presentación del curso](README.md)

<details markdown="block">
<summary><strong>Soluciones</strong></summary>

**Ejercicio 9.1**

Un aviso, y la actividad es válida:

```text
WARNING MISSING_LEGAL_REFERENCE grading: GDPR article(s) 6.1.e rest on Union or Member State law; name it as the legal reference
```

La base de interés público descansa en una ley y la actividad no nombra ninguna. Los datos personales vienen de las
dos entidades: `user.account.username` (de `AuditedEntity.createdBy`), `user.education.course_history`,
`user.education.enrollment`, `user.education.grade`, `user.education.program`, `user.financial.fee`,
`user.financial.scholarship` y `user.unique_id.record_reference`, todas ordinarias, así que no hace falta ninguna
condición ni se sugiere evaluación de impacto. La protección requerida es confidencialidad `MEDIUM` e integridad
`HIGH` (la integridad de las calificaciones). Añade `.legalReference("Higher Education Act, art. 12")`.

**Ejercicio 9.2**

- `job_applicants` no es una clave: el interesado es `job_applicant`. Usa `DataSubjects.JOB_APPLICANT` y el
  compilador detectará las erratas.
- La verificación ha perdido su `.criminalDataBasis(CriminalDataBasis.AUTHORISED_BY_LAW)`. Sin ella, tratar datos
  penales es ilícito tal como está declarado.
- La nueva actividad de tasas reutiliza la clave `enrollment`. Dale su propia clave, por ejemplo `tuition-fees`. El
  error se notifica en la segunda actividad con esa clave; la primera no se toca.

**Ejercicio 9.3**

La comprobación notifica un aviso:

```text
WARNING UNNEEDED_CONDITION alumni-newsletter: A legitimate interest is stated but the lawful basis is article 6.1.a
```

La base es ahora `CONSENT`, porque gana la última llamada, y el registro muestra `Art. 6.1.a: Consent` sin fila
«Legitimate interest»: el registro solo muestra el interés cuando la base es el interés legítimo. El cambio correcto es
sustituir `.legitimateInterest(...)` por `.legalBasis(LegalBasis.CONSENT)` y conservar la prueba del consentimiento
donde se guardan las suscripciones al boletín.

**Ejercicio 9.4**

Añade `.retention("Six years after the student leaves the university")` a `ENROLLMENT`, y:

```java
@Test
void noOpenPoints() {
    ProcessingRecord record = new ProcessingChecker(Taxonomy.load()).verify(UniversityActivities.all());
    List<ProcessingViolation> openPoints = record.activities().stream()
            .flatMap(activity -> activity.warnings().stream())
            .toList();
    assertTrue(openPoints.isEmpty(), () -> "Close the open points of the record: " + openPoints);
}
```

Con el plazo añadido, `openPoints` queda vacía.

**Ejercicio 9.5**

```text
ERROR MISSING_CRIMINAL_DATA_BASIS staff-administration: Criminal offence data (user.criminal_history) needs a basis of GDPR article 10
WARNING IMPACT_ASSESSMENT_RECOMMENDED staff-administration: The activity involves criminal offence data; GDPR article 35 may require an impact assessment
```

`Staff.sexOffenceCertificateClear` son datos penales, así que incluir `Staff` los lleva a la administración de
personal. La actividad requiere además confidencialidad `HIGH`, integridad `HIGH` e incluye secretos (`passwordHash`).
Hay dos soluciones posibles: si la administración de personal guarda de verdad el resultado de la verificación,
declara `.criminalDataBasis(CriminalDataBasis.AUTHORISED_BY_LAW)`, la referencia legal y la evaluación de impacto; si
no, declara por clave las categorías que sí trata en lugar de incluir `Staff` y deja los datos penales a la actividad
de verificación. La segunda limita cada actividad a lo que necesita.

**Cuestionario**

1. No. `MISSING_RETENTION` es un aviso: el registro muestra `Not declared` y lo enumera como punto abierto.
2. Una `SpecialCategoryCondition`, por ejemplo `EXPLICIT_CONSENT` (art. 9.2.a). Sin ella, `check` notifica
   `MISSING_SPECIAL_CATEGORY_CONDITION`.
3. Fija la base en `LEGITIMATE_INTERESTS` e indica el interés.
4. Sus campos son datos de referencia o calificaciones agregadas, y los datos agregados no son datos personales, así
   que la actividad no trata ningún dato personal.
5. `LEGAL_OBLIGATION` y `PUBLIC_INTEREST`; las condiciones b, g, h, i y j del artículo 9.2 cuando hay categorías
   especiales; `AUTHORISED_BY_LAW` cuando hay datos penales.
6. En inglés, sea cual sea el idioma de la aplicación. Los nombres de los términos vienen de la taxonomía y el resto
   se escribe tal como se declaró.
7. No. Significa que cada actividad declara lo que pide el RGPD y que sus claves y entidades son correctas. Si las
   declaraciones son ciertas y las bases adecuadas es la evaluación del responsable.

</details>
