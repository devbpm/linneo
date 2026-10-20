---
title: "Universidad"
parent: "Extensiones"
grand_parent: "Referencia"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/extension/university/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityCategories.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityDataUses.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityDataSubjects.java
-->

# Extensión universitaria

Paquete `io.github.devbpm.linneo.extension.university` — exportado, `@since 0.1.0`

Términos para universidades y otras instituciones de educación superior: los datos que guardan sobre estudiantes,
solicitantes, personal, antiguos alumnos y benefactores más allá de los términos estándar `user.education.*`, las
finalidades para las que los tratan y las personas implicadas. La extensión es opcional y no se aplica por defecto;
consulta [Extensiones](README.md).

Da el mayor detalle al expediente académico del estudiante: cambios de matrícula y traslados, convocatorias de
evaluación, notas finales y medias, reconocimiento de créditos, avance en la titulación, títulos, certificados y
competencia lingüística, prácticas y movilidad, ayudas sujetas a nivel de renta y exenciones de tasas.

| Tipo | Descripción |
|---|---|
| `UniversityTaxonomy` | `public final class implements TaxonomyExtension`. `name()` devuelve `NAME`, `public static final String NAME = "university"`, que se registra como origen de sus términos. Constructor público sin argumentos; sin estado. `contribute(builder)` añade los términos que se enumeran abajo. |
| `UniversityCategories` | `public final class` con constructor privado: un `public static final String` por cada categoría de datos que añade la extensión (33). |
| `UniversityDataUses` | Lo mismo para sus usos de datos (27). |
| `UniversityDataSubjects` | Lo mismo para sus interesados (13). |

Las constantes siguen la [regla de nombres](../key-constants.md#regla-de-nombres) de `Categories`:
`user.education.attendance` es `UniversityCategories.USER_EDUCATION_ATTENDANCE` y
`essential.legal_obligation.statutory_reporting` es
`UniversityDataUses.ESSENTIAL_LEGAL_OBLIGATION_STATUTORY_REPORTING`. Son constantes de compilación, válidas en las
anotaciones una vez aplicada la extensión.

## Aplicarla

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();
// Taxonomy[156 data categories, 90 data uses, 34 data subjects]
```

`Taxonomy.load()` solo la aplica si la aplicación la registra; consulta
[Registrar una extensión incluida](README.md#registrar-una-extensión-incluida).

Un modelo usa entonces las claves de la extensión junto a las estándar:

```java
package com.example.app;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.extension.university.UniversityCategories;

import java.time.LocalDate;

public class Absence {
    @Category(Categories.USER_UNIQUE_ID)
    private String studentNumber;

    @Category(UniversityCategories.USER_EDUCATION_ATTENDANCE)
    private LocalDate date;

    @Category(UniversityCategories.USER_EDUCATION_ATTENDANCE)
    private boolean excused;

    @Category(Categories.USER_HEALTH_AND_MEDICAL)   // the medical reason is health data, not attendance
    private String medicalReason;
}
```

```java
new Classifier(taxonomy).verify(List.of(Absence.class)).get(0).fields().forEach(f ->
        System.out.println(f.field() + ": " + f.category().key() + " " + f.category().nature() + " "
                + f.confidentiality() + " " + f.integrity()));
```

```text
studentNumber: user.unique_id ORDINARY LOW MEDIUM
date: user.education.attendance ORDINARY MEDIUM HIGH
excused: user.education.attendance ORDINARY MEDIUM HIGH
medicalReason: user.health_and_medical SPECIAL_CATEGORY HIGH HIGH
```

## Reglas de fideslang para las etiquetas propias

La extensión sigue las reglas que fideslang fija para las etiquetas propias, de modo que sus términos sigan siendo
interoperables con otras herramientas:

- **Ampliar clases existentes.** Todos los términos son hijos de un término existente. Las únicas excepciones son las
  dos raíces de usos de datos `advancement` y `research`, cuyas finalidades no describe ningún uso de fideslang.
- **La madre es el prefijo de la clave.** `user.education.grade.final` es hija de `user.education.grade`, y
  `education.placement.career_services`, de `education.placement`.
- **Los interesados son planos.** Los interesados de fideslang no tienen jerarquía, así que todos los de la
  extensión son raíces: un doctorando es `doctoral_candidate`, no un hijo de `student`.
- **Sin información de versión.** fideslang solo permite información de versión en su taxonomía por defecto
  («Non-default items can't have version information»), así que ningún término de la extensión la lleva; su origen se
  registra como `university`.

## Categorías de datos

La extensión organiza las categorías según el registro al que pertenecen: el expediente del estudiante, la admisión,
el apoyo y la convivencia, las prácticas y la movilidad, y las finanzas. El mayor detalle está en el expediente del
estudiante.

La protección que se muestra es la efectiva en la taxonomía construida. Los valores en **negrita** los declara la
extensión; el resto se heredan de la madre (`user.education` y `user.financial` son `ORDINARY`, `MEDIUM`, `HIGH`;
`user.contact` es `ORDINARY`, `LOW`, `MEDIUM`). Ninguna categoría es secreta y todas son datos personales.

### El expediente del estudiante

Cambios de matrícula y traslados de expediente bajo `user.education.enrollment`; convocatorias, notas finales
ratificadas y notas medias bajo `user.education.grade`; reconocimiento de créditos bajo
`user.education.course_history`; avance bajo `user.education.program`; y títulos, certificados y competencia
lingüística bajo `user.education.accomplishment`.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `user.education.enrollment.modification` | Enrollment Change | `ORDINARY` | `MEDIUM` | `HIGH` | Cambios en una matrícula después de formalizarla: asignaturas añadidas o retiradas, anulación, renuncia parcial o total. |
| `user.education.enrollment.transfer` | Record Transfer | `ORDINARY` | `MEDIUM` | `HIGH` | Traslado del expediente del interesado desde o hacia otra institución, con los documentos intercambiados. |
| `user.education.academic_standing` | Academic Standing | `ORDINARY` | `MEDIUM` | `HIGH` | Situación académica según el rendimiento: normal, advertencia, periodo de prueba, expulsión académica, matrícula de honor. |
| `user.education.attendance` | Attendance | `ORDINARY` | `MEDIUM` | `HIGH` | Asistencia, ausencias y retrasos del interesado en clases, exámenes u otras sesiones. |
| `user.education.coursework` | Coursework | `ORDINARY` | `MEDIUM` | `HIGH` | Trabajos del interesado para su evaluación: tareas entregadas, respuestas de examen. |
| `user.education.grade.attempt` | Assessment Attempt | `ORDINARY` | `MEDIUM` | `HIGH` | Cada convocatoria de una evaluación: fecha, número de intento, resultado e incidencias. |
| `user.education.grade.final` | Final Grade | `ORDINARY` | `MEDIUM` | `HIGH` | Resultados finales de las asignaturas ratificados por el tribunal o la comisión de evaluación, tal como constan en el expediente. |
| `user.education.grade.average` | Grade Average | `ORDINARY` | `MEDIUM` | `HIGH` | Notas medias y clasificaciones calculadas a partir de los resultados del interesado. |
| `user.education.course_history.recognition` | Credit Recognition | `ORDINARY` | `MEDIUM` | `HIGH` | Créditos reconocidos o transferidos de estudios previos, experiencia laboral u otras instituciones, con la resolución. |
| `user.education.program.progress` | Programme Progress | `ORDINARY` | `MEDIUM` | `HIGH` | Créditos superados, requisitos cumplidos y avance hacia la finalización de la titulación. |
| `user.education.hold` | Academic Hold | `ORDINARY` | `MEDIUM` | `HIGH` | Bloqueos administrativos del expediente, como los que impiden matricularse o pedir un certificado. |
| `user.education.thesis` | Thesis | `ORDINARY` | `MEDIUM` | `HIGH` | Tesis doctoral, trabajo fin de máster o trabajo fin de estudios: título, directores, depósito y defensa. |
| `user.education.accomplishment.degree` | Degree | `ORDINARY` | `MEDIUM` | `HIGH` | Títulos y cualificaciones oficiales obtenidos: denominación, fecha, nota final, menciones y número de registro. |
| `user.education.accomplishment.certificate` | Academic Certificate | `ORDINARY` | `MEDIUM` | `HIGH` | Certificados emitidos sobre el expediente del interesado, como certificaciones académicas o suplementos al título, con sus códigos de verificación. |
| `user.education.accomplishment.language_proficiency` | Language Proficiency | `ORDINARY` | `MEDIUM` | `HIGH` | Niveles acreditados de competencia lingüística, como un nivel B2 certificado. |

### Admisión

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `user.education.application` | Admission Application | `ORDINARY` | `MEDIUM` | `HIGH` | La solicitud del interesado: titulación y periodo solicitados, estado y respuestas. |
| `user.education.application.evaluation` | Admission Evaluation | `ORDINARY` | **`HIGH`** | `HIGH` | Puntuaciones, comentarios y condiciones de los evaluadores y la resolución de una solicitud. |
| `user.education.application.reference` | Reference Letter | `ORDINARY` | **`HIGH`** | `HIGH` | Cartas de recomendación o de referencia escritas por terceros sobre el interesado. |

### Apoyo, convivencia y equidad

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `user.education.accommodation` | Academic Accommodation | **`SPECIAL_CATEGORY`** | **`HIGH`** | `HIGH` | Adaptaciones concedidas para el estudio o la evaluación, como tiempo adicional o herramientas de apoyo, y las necesidades que las motivan. |
| `user.education.advising` | Advising | `ORDINARY` | **`HIGH`** | `HIGH` | Asignación de tutores, notas de tutoría, planes de seguimiento y derivaciones a servicios de apoyo. |
| `user.education.disciplinary_record` | Disciplinary Record | `ORDINARY` | **`HIGH`** | `HIGH` | Procedimientos y sanciones de convivencia o de integridad académica sobre el interesado, como plagios probados o suspensiones. |
| `user.education.equity_indicator` | Equity Indicator | `ORDINARY` | **`HIGH`** | `HIGH` | Indicadores para políticas de acceso o informes de equidad, como primera generación universitaria, tutela pública o desventaja económica. |
| `user.education.risk_indicator` | Academic Risk Indicator | `ORDINARY` | **`HIGH`** | `HIGH` | Alertas tempranas, listas de seguimiento, puntuaciones de riesgo y predicciones sobre el progreso o el abandono del interesado. |
| `user.education.extracurricular` | Extracurricular Activity | `ORDINARY` | `MEDIUM` | `HIGH` | Clubes, asociaciones, deportes y otras actividades no curriculares del interesado. |

### Prácticas y movilidad

Las prácticas y la movilidad se distinguen bajo `user.education.placement`.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `user.education.placement` | Placement | `ORDINARY` | `MEDIUM` | `HIGH` | Prácticas, estancias en empresas, intercambios y otras movilidades durante los estudios. |
| `user.education.placement.internship` | Internship | `ORDINARY` | `MEDIUM` | `HIGH` | Unas prácticas: organización de acogida, tutor, convenio, calendario y tareas. |
| `user.education.placement.internship.evaluation` | Internship Evaluation | `ORDINARY` | **`HIGH`** | `HIGH` | La valoración del tutor de la organización de acogida sobre el desempeño del interesado en las prácticas. |
| `user.education.placement.mobility` | Mobility | `ORDINARY` | `MEDIUM` | `HIGH` | Un intercambio o una estancia de estudios en otra institución: institución de destino, acuerdo de estudios, reconocimiento y ayuda de movilidad. |

### Finanzas y contacto

Las ayudas sujetas a nivel de renta y las exenciones de tasas cuelgan de los términos financieros estándar
`user.financial.scholarship` y `user.financial.fee`.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `user.financial.scholarship.means_assessment` | Means Assessment | `ORDINARY` | **`HIGH`** | `HIGH` | Ingresos, patrimonio y composición del hogar evaluados para conceder ayudas sujetas a nivel de renta. |
| `user.financial.fee.exemption` | Fee Exemption | `ORDINARY` | **`HIGH`** | `HIGH` | Exenciones y reducciones de tasas y sus motivos, como familia numerosa o mérito académico. |
| `user.financial.donation` | Donation | `ORDINARY` | `MEDIUM` | `HIGH` | Donativos, promesas de donación y donaciones periódicas del interesado a la institución. |
| `user.financial.wealth_assessment` | Wealth Assessment | `ORDINARY` | **`HIGH`** | `HIGH` | Estimaciones del patrimonio, los ingresos y la capacidad de donación del interesado obtenidas en la investigación de donantes. |
| `user.contact.emergency_contact` | Emergency Contact | `ORDINARY` | **`MEDIUM`** | `MEDIUM` | Nombre, teléfono y relación de la persona a la que avisar en caso de emergencia. |

### Por qué estos niveles

- Una **adaptación académica**, como el tiempo adicional en los exámenes, revela una discapacidad o un problema de
  salud, así que es una categoría especial de datos aunque el registro solo guarde la adaptación y no el diagnóstico.
- Los **expedientes disciplinarios, las notas de tutoría, los indicadores de riesgo, las evaluaciones de admisión, las
  cartas de referencia, las evaluaciones de prácticas, los indicadores de equidad, las valoraciones de renta, las
  exenciones de tasas y las estimaciones de patrimonio** tienen confidencialidad alta: son valoraciones, predicciones o
  datos de vulnerabilidad sobre el interesado, a menudo escritos por otros, o revelan los ingresos o la situación del
  hogar.
- El **contacto de emergencia** sube a confidencialidad media, por encima del resto de `user.contact`: revela quiénes
  son las personas cercanas al interesado.
- Las demás categorías heredan la confidencialidad media y la integridad alta de `user.education` (o de
  `user.financial`).

## Dónde van los datos más sensibles

Un registro de una categoría de la extensión puede contener detalles que revelan más que la categoría. Clasifica esos
campos con la clave estándar más estricta, no con la de la extensión:

| El campo contiene | Clasifícalo como | Protección |
|---|---|---|
| El motivo médico de una ausencia | `user.health_and_medical` (no `user.education.attendance`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Un expediente disciplinario por un delito | `user.criminal_history` (no `user.education.disciplinary_record`) | `CRIMINAL_OFFENCE`, `HIGH`, `HIGH` |
| La pertenencia a una asociación religiosa o política | `user.demographic.religious_belief` o `user.demographic.political_opinion` (no `user.education.extracurricular`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Una exención de tasas o un ajuste de calificación concedidos por una discapacidad | `user.health_and_medical.disability` (no `user.financial.fee.exemption` ni `user.education.grade`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |

El mismo razonamiento vale para otras categorías especiales, por ejemplo una asociación vinculada a un sindicato
(`user.demographic.trade_union_membership`) o a la orientación sexual (`user.demographic.sexual_orientation`).

## Usos de datos

La extensión añade dos raíces, `advancement` y `research`, porque fideslang no tiene ninguna finalidad para la
captación de fondos ni para la investigación científica; todos los demás usos son hijos de uno existente, incluidos
los nuevos hijos de los estándar `education.academic_records`, `education.assessment` y `education.financial_aid`. Los
usos de datos no tienen facetas de protección.

| Clave | Nombre | Madre | Finalidad |
|---|---|---|---|
| `advancement` | Advancement | (raíz) | Cultiva la relación de la institución con antiguos alumnos, donantes y benefactores. |
| `advancement.alumni_relations` | Alumni Relations | `advancement` | Mantiene el contacto con los antiguos alumnos: comunicaciones, eventos, redes. |
| `advancement.fundraising` | Fundraising | `advancement` | Solicita, registra y agradece donativos y promesas de donación. |
| `advancement.prospect_research` | Prospect Research | `advancement` | Identifica posibles donantes y estima su capacidad de donación. |
| `education.academic_records.certification` | Certification | `education.academic_records` | Emite certificados, certificaciones académicas, suplementos al título y certificados de títulos. |
| `education.academic_records.credit_recognition` | Credit Recognition | `education.academic_records` | Reconoce y transfiere créditos de estudios previos o de experiencia. |
| `education.academic_records.verification` | Qualification Verification | `education.academic_records` | Confirma las cualificaciones del interesado a empleadores u otras instituciones cuando lo solicitan. |
| `education.accessibility` | Accessibility | `education` | Valora necesidades y presta adaptaciones y apoyo a la discapacidad. |
| `education.advising` | Advising | `education` | Ofrece tutoría académica, planes de seguimiento y mentoría. |
| `education.assessment.examination` | Examination | `education.assessment` | Programa y administra las convocatorias de examen, incluidas la vigilancia y las adaptaciones. |
| `education.attendance` | Attendance | `education` | Registra y controla la asistencia y hace seguimiento de las ausencias. |
| `education.discipline` | Discipline | `education` | Investiga y sanciona las infracciones de las normas de convivencia y de integridad académica. |
| `education.financial_aid.means_assessment` | Means Assessment | `education.financial_aid` | Valora los ingresos y el patrimonio del hogar para conceder ayudas sujetas a nivel de renta. |
| `education.learning_analytics` | Learning Analytics | `education` | Analiza datos de aprendizaje para mejorar la docencia y los resultados de los estudiantes. |
| `education.placement` | Placement | `education` | Organiza prácticas, estancias en empresas, servicios de empleo, intercambios y movilidad. |
| `education.placement.career_services` | Career Services | `education.placement` | Ayuda a estudiantes y titulados a encontrar empleo: bolsas de trabajo, orientación, eventos con empleadores. |
| `education.placement.internship` | Internships | `education.placement` | Organiza, supervisa y evalúa prácticas en organizaciones de acogida. |
| `education.placement.mobility` | Mobility | `education.placement` | Organiza intercambios y estancias de estudio en otras instituciones, en el país o en el extranjero. |
| `education.quality_assurance` | Quality Assurance | `education` | Evalúa y acredita titulaciones y docencia, incluidas las encuestas a estudiantes. |
| `education.recruitment` | Recruitment | `education` | Atrae a futuros estudiantes: consultas, eventos y seguimiento del interés. |
| `education.student_life` | Student Life | `education` | Organiza actividades extracurriculares, deportes, asociaciones y residencias en el campus. |
| `education.student_support` | Student Support | `education` | Identifica a los estudiantes en riesgo y les ofrece intervenciones y apoyo al bienestar. |
| `education.thesis_supervision` | Thesis Supervision | `education` | Dirige tesis de máster y doctorales y gestiona su depósito y defensa. |
| `essential.legal_obligation.statutory_reporting` | Statutory Reporting | `essential.legal_obligation` | Elabora las estadísticas e informes que las autoridades educativas exigen por ley. |
| `research` | Research | (raíz) | Realiza y gestiona investigación científica. |
| `research.administration` | Research Administration | `research` | Gestiona proyectos, ayudas, investigadores y resultados de investigación. |
| `research.participation` | Research Participation | `research` | Trata datos de los participantes en estudios de investigación. |

La taxonomía estándar ya tiene las finalidades académicas básicas: `education.admission`, `education.enrollment`,
`education.teaching`, `education.assessment`, `education.academic_records` y `education.financial_aid`; la selección
de personal es `employment.recruitment`.

## Interesados

Todos los interesados de la extensión son planos, porque los interesados de fideslang no tienen jerarquía.

| Clave | Nombre | Quién |
|---|---|---|
| `doctoral_candidate` | Doctoral Candidate | Un estudiante matriculado en un programa de doctorado. |
| `donor` | Donor | Una persona que hace o promete donativos a la institución. |
| `emergency_contact` | Emergency Contact | Una persona designada por un estudiante o un empleado para avisarla en caso de emergencia. |
| `exchange_student` | Exchange Student | Un estudiante de intercambio, recibido de otra institución o enviado a ella. |
| `external_examiner` | External Examiner | Un evaluador ajeno a la institución que examina a estudiantes o evalúa tesis. |
| `guest` | Guest | Una persona vinculada temporalmente a la institución sin ser estudiante ni empleado. |
| `mentor` | Mentor | Una persona, a menudo un antiguo alumno o un voluntario, que hace de mentor de estudiantes. |
| `philanthropic_prospect` | Philanthropic Prospect | Una persona identificada como posible donante. |
| `placement_supervisor` | Placement Supervisor | La persona que supervisa a un estudiante en una organización o institución de acogida. |
| `prospective_student` | Prospective Student | Una persona que ha mostrado interés por estudiar en la institución. |
| `referee` | Referee | Una persona que escribe una carta de referencia sobre un solicitante. |
| `trustee` | Trustee | Un miembro del órgano de gobierno de la institución o de su fundación. |
| `volunteer` | Volunteer | Una persona que dedica tiempo como voluntaria a la institución. |

Los interesados estándar `student`, `applicant`, `alumnus`, `faculty_member`, `staff_member` y
`legal_representative` (Linneo) y `employee`, `next_of_kin` y `visitor` (fideslang) cubren el resto. Algunos
interesados de la extensión están junto a uno de fideslang más restringido: `prospect` es alguien a quien la
organización vende bienes o servicios, `next_of_kin` es un familiar (un contacto de emergencia puede ser un amigo o un
vecino) y `visitor` es alguien que visita un lugar (un invitado está vinculado a la institución durante un tiempo). `doctoral_candidate` y
`exchange_student` son tipos de estudiante, pero, como los interesados son planos, no son hijos de `student`: úsalos
cuando el tratamiento sea específico de ellos.

### Por qué los roles docentes no son interesados

Profesor, profesor ayudante, coordinador de asignatura, vigilante de examen o evaluador no son interesados. Los
modelos en los que se basa la extensión los tratan como **roles que una persona desempeña en una oferta docente**, no
como tipos de persona: la persona es un `faculty_member` o un `staff_member`, y un profesor ayudante es a menudo un
`student`. Clasifica el campo que registra el rol con `user.workplace.role` y a la persona con el interesado que sea.
La excepción es el `external_examiner`, que no pertenece a ningún otro grupo de interesados de la institución.

## Claves estándar para todo lo demás

La extensión solo añade lo que no tiene clave estándar. Los atributos que los modelos educativos comparten con la
taxonomía estándar usan las claves estándar. Algunas tienen hijas en la extensión, que describen un tipo de dato más
concreto: `user.education.grade` (`.final`, `.average`, `.attempt`), `user.education.enrollment` (`.modification`,
`.transfer`), `user.education.course_history` (`.recognition`), `user.education.program` (`.progress`),
`user.education.accomplishment` (`.degree`, `.certificate`, `.language_proficiency`), `user.financial.scholarship`
(`.means_assessment`) y `user.financial.fee` (`.exemption`). Usa la clave estándar para lo que la hija no describe:

| Atributo | Clave estándar | Protección |
|---|---|---|
| Calificaciones, nota media, puntuaciones, resultados | `user.education.grade` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Matrículas en asignaturas y periodos, listas de espera | `user.education.enrollment` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Asignaturas cursadas | `user.education.course_history` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Titulación, especialidad, mención | `user.education.program` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Activo, con permiso, titulado, baja | `user.education.status` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Pruebas de acceso y de idiomas | `user.education.test_score` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Centros anteriores, posición en la promoción | `user.education.previous_education` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Títulos, certificados, insignias, premios | `user.education.accomplishment` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Becas, ayudas, ayuda financiera | `user.financial.scholarship` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Matrícula y otras tasas | `user.financial.fee` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Tipo, grado y acreditación de la discapacidad | `user.health_and_medical.disability` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Raza u origen étnico | `user.demographic.race_ethnicity` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Religión | `user.demographic.religious_belief` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Género, pronombres | `user.demographic.gender` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| Lengua materna o preferida | `user.demographic.language` | `ORDINARY`, `LOW`, `MEDIUM` |
| Nacionalidad, ciudadanía, país de nacimiento | `user.demographic.nationality` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| Permiso de residencia, situación de ciudadanía | `user.government_id.immigration` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| Números de identidad y de la seguridad social | `user.government_id.national_identification_number`, `user.government_id.social_security_number` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| Número de estudiante, European Student Identifier, ORCID | `user.unique_id` | `ORDINARY`, `LOW`, `MEDIUM` |
| Fotografías | `user.content.self_image` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| Preferencias de directorio y de comunicación de datos | `user.privacy_preferences` | `ORDINARY`, `LOW`, `MEDIUM` |
| Trabajo durante los estudios, habilitaciones docentes | `user.workplace` y sus hijas, `user.job_title` | `ORDINARY`, `LOW`, `MEDIUM` |
| Registros de acceso a plataformas de aprendizaje | `user.behavior.activity_log` | `ORDINARY`, `MEDIUM`, `HIGH` |

## Fuentes

Los términos se basan en tres modelos públicos del dominio de la educación superior, consultados en octubre de 2026.
La extensión toma de ellos conceptos, no sus textos ni su estructura.

- **Open Education API (OEAPI) v6**: la especificación de personas, ofertas, asociaciones, intentos, resultados y sus
  enumeraciones (asistencia, necesidades personales, roles en una asociación, afiliaciones):
  <https://github.com/open-education-api/specification/tree/v6.0>.
- **Microsoft Common Data Model, educación superior**: las entidades `higherEducation` del CDM (versión 1.4):
  <https://github.com/microsoft/CDM/tree/master/schemaDocuments/core/applicationCommon/foundationCommon/crmCommon/accelerators/education/higherEducation>,
  y los metadatos del Dynamics 365 Education Accelerator (asistencia, incidentes de conducta, intervenciones,
  características del estudiante; el repositorio está archivado): <https://github.com/microsoft/Industry-Accelerator-Education>.
- **Salesforce Education Cloud**: los objetos estándar de la *Education Cloud Developer Guide* (versión 68.0):
  <https://developer.salesforce.com/docs/atlas.en-us.edu_cloud_dev_guide.meta/edu_cloud_dev_guide/edu_cloud_standard_objects.htm>;
  y el paquete **Education Data Architecture (EDA)**: <https://github.com/SalesforceFoundation/EDA>.

Qué aportó cada uno: todos modelan la asistencia y las adaptaciones o necesidades personales; el acelerador de
Microsoft y EDA modelan incidentes de conducta y características del estudiante; Salesforce modela la admisión
(solicitudes, evaluaciones, recomendaciones), la situación académica, los bloqueos, las listas de seguimiento, las
prácticas y la captación de fondos con investigación de donantes. Los roles docentes vienen de los roles de asociación
de OEAPI y de los participantes en una oferta docente de Salesforce.

Véase también: [Extensiones](README.md) · [Taxonomía estándar](../standard-taxonomy.md) ·
[Constantes de claves](../key-constants.md) · [Niveles de protección](../protection-levels.md)
