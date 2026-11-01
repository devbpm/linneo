---
title: "Recursos humanos"
parent: "Extensiones"
grand_parent: "Referencia"
nav_order: 5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/HumanResourcesTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/HumanResourcesCategories.java
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/HumanResourcesDataUses.java
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/HumanResourcesDataSubjects.java
-->

# Extensión de recursos humanos

Paquete `io.github.devbpm.linneo.extension.humanresources` — exportado, `@since 0.1.0`

Términos para recursos humanos: la relación laboral desde la selección hasta la extinción, la retribución, el tiempo de
trabajo, los permisos, el desempeño, la formación, el control en el lugar de trabajo y la comunicación interna de
infracciones, las finalidades de recursos humanos y las personas implicadas. La extensión es opcional y no se aplica por
defecto; consulta [Extensiones](README.md).

Se basa en las áreas de recursos humanos y nómina del Common Data Model (CDM) (consulta [Fuentes](#fuentes)); no se
reproduce material de CDM.

La extensión añade `user.employment` para la relación laboral que gestiona recursos humanos, junto a la estándar
`user.workplace`, que dice dónde y en qué equipo, unidad o rol trabaja el interesado. Todas las categorías cuelgan de
`user` o de `user.health_and_medical`, de modo que el `Classifier` las trata como datos personales.

| Tipo | Descripción |
|---|---|
| `HumanResourcesTaxonomy` | `public final class implements TaxonomyExtension`. `name()` devuelve `NAME`, `public static final String NAME = "human_resources"`, que se registra como origen de sus términos. Constructor público sin argumentos; sin estado. `contribute(builder)` añade los términos que se enumeran abajo. |
| `HumanResourcesCategories` | `public final class` con constructor privado: un `public static final String` por cada categoría de datos que añade la extensión (21). |
| `HumanResourcesDataUses` | Lo mismo para sus usos de datos (15). |
| `HumanResourcesDataSubjects` | Lo mismo para sus interesados (6). |

Las constantes siguen la [regla de nombres](../key-constants.md#regla-de-nombres) de `Categories`:
`user.employment.payroll` es `HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL` y `employment.payroll` es
`HumanResourcesDataUses.EMPLOYMENT_PAYROLL`. Son constantes de compilación, válidas en las anotaciones una vez aplicada
la extensión.

## Aplicarla

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new HumanResourcesTaxonomy()).build();
// Taxonomy[147 data categories, 78 data uses, 28 data subjects]
```

La extensión no comparte ninguna clave con las demás extensiones incluidas, así que puede aplicarse con cualquiera de
ellas, en el mismo builder:

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new HumanResourcesTaxonomy())
        .extend(new ProjectOperationsTaxonomy())   // io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsTaxonomy
        .build();
```

`Taxonomy.load()` solo la aplica si la aplicación la registra; consulta [Registrar una extensión
incluida](README.md#registrar-una-extensión-incluida).

Un modelo usa entonces las claves de la extensión junto a las estándar:

```java
package com.example.app;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesCategories;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SickLeave {
    @Category(Categories.USER_UNIQUE_ID)
    private String employeeNumber;

    @Category(HumanResourcesCategories.USER_HEALTH_AND_MEDICAL_SICK_LEAVE)
    private LocalDate from;

    @Category(HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL)
    private BigDecimal subsidy;
}
```

```java
new Classifier(taxonomy).verify(List.of(SickLeave.class)).get(0).fields().forEach(f ->
        System.out.println(f.field() + ": " + f.category().key() + " " + f.category().nature() + " "
                + f.confidentiality() + " " + f.integrity()));
```

```text
employeeNumber: user.unique_id ORDINARY LOW MEDIUM
from: user.health_and_medical.sick_leave SPECIAL_CATEGORY HIGH HIGH
subsidy: user.employment.payroll ORDINARY HIGH HIGH
```

## Reglas de fideslang para las etiquetas propias

La extensión sigue las reglas que fideslang fija para las etiquetas propias, de modo que sus términos sigan siendo
interoperables con otras herramientas:

- **Ampliar clases existentes.** Todos los términos son hijos de un término existente; todos los usos son hijos del uso
  estándar `employment` (`employment.recruitment.background_check` cuelga del estándar `employment.recruitment`).
- **La madre es el prefijo de la clave.** `user.employment.payroll` es hija de `user.employment`.
- **Los interesados son planos.** Los interesados de fideslang no tienen jerarquía, así que todos los de la extensión
  son raíces.
- **Sin información de versión.** fideslang solo permite información de versión en su taxonomía por defecto, así que
  ningún término de la extensión la lleva; su origen se registra como `human_resources`.

## Categorías de datos

La protección que se muestra es la efectiva en la taxonomía construida. Los valores en **negrita** los declara la
extensión; el resto se heredan de la madre (`user` es `ORDINARY`, `LOW`, `MEDIUM`; `user.employment` es `ORDINARY`,
`MEDIUM`, `MEDIUM`; `user.health_and_medical` es `SPECIAL_CATEGORY`, `HIGH`, `HIGH`).
`user.employment.payroll.garnishment` y `user.employment.payroll.tax_withholding` heredan `HIGH` y `HIGH` de
`user.employment.payroll`. Ninguna categoría es secreta y todas son datos personales.

### La relación laboral

La nueva rama `user.employment`, con los registros de selección, contrato, retribución, tiempo de trabajo, permisos,
desempeño y extinción.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `user.employment` | Employment Data | `ORDINARY` | **`MEDIUM`** | `MEDIUM` | Datos sobre el empleo o la vinculación del interesado con la organización, tal como los gestiona recursos humanos. |
| `user.employment.background_check` | Background Check | `ORDINARY` | **`HIGH`** | `MEDIUM` | Verificación previa a la contratación de la identidad, la titulación, la trayectoria laboral y las referencias del interesado. |
| `user.employment.benefit` | Benefit | `ORDINARY` | `MEDIUM` | `MEDIUM` | Adhesión a planes de beneficios, como pensiones, seguros, tickets de comida o retribución flexible, y los beneficiarios designados. |
| `user.employment.compensation` | Compensation | `ORDINARY` | **`HIGH`** | **`HIGH`** | Salario, categoría salarial, complementos, bonus, revisiones salariales y premios por desempeño. |
| `user.employment.contract` | Employment Contract | `ORDINARY` | `MEDIUM` | **`HIGH`** | Tipo de contrato, fechas de inicio y fin, jornada, periodo de prueba y las modificaciones de estas condiciones. |
| `user.employment.disciplinary_record` | Disciplinary Record | `ORDINARY` | **`HIGH`** | `MEDIUM` | Amonestaciones, expedientes disciplinarios y sanciones que afectan al interesado. |
| `user.employment.leave` | Leave and Absence | `ORDINARY` | `MEDIUM` | `MEDIUM` | Vacaciones, solicitudes de permiso, saldos y ausencias, sin sus motivos médicos. |
| `user.employment.monitoring` | Workplace Monitoring | `ORDINARY` | **`HIGH`** | `MEDIUM` | Datos del control del interesado en el trabajo: uso de dispositivos y correo electrónico de la empresa, videovigilancia y geolocalización. |
| `user.employment.payroll` | Payroll | `ORDINARY` | **`HIGH`** | **`HIGH`** | Recibos de salarios, devengos, deducciones, retenciones fiscales y cotizaciones a la seguridad social de cada periodo de pago. |
| `user.employment.payroll.garnishment` | Wage Garnishment | `ORDINARY` | `HIGH` | `HIGH` | Órdenes judiciales o administrativas de retener parte del salario del interesado, y los importes retenidos. |
| `user.employment.payroll.tax_withholding` | Tax Withholding Data | `ORDINARY` | `HIGH` | `HIGH` | Situación personal y familiar que declara el interesado para calcular la retención del impuesto sobre la renta, como personas a su cargo o pensiones abonadas. |
| `user.employment.performance` | Performance Evaluation | `ORDINARY` | **`HIGH`** | `MEDIUM` | Objetivos, revisiones, valoraciones, comentarios y calibración del desempeño del interesado. |
| `user.employment.recruitment` | Recruitment | `ORDINARY` | `MEDIUM` | `MEDIUM` | Las candidaturas del interesado: currículum, puestos solicitados, entrevistas y su estado. |
| `user.employment.recruitment.assessment` | Candidate Assessment | `ORDINARY` | **`HIGH`** | `MEDIUM` | Notas de entrevista, resultados de pruebas, puntuaciones y la decisión de contratación sobre el interesado. |
| `user.employment.termination` | Termination | `ORDINARY` | **`HIGH`** | `MEDIUM` | Fin del empleo del interesado: motivo, preaviso, carta de despido, finiquito e indemnización. |
| `user.employment.training` | Training | `ORDINARY` | `MEDIUM` | `MEDIUM` | Cursos, asistencia, resultados y certificaciones que obtiene el interesado a través de la organización. |
| `user.employment.whistleblowing` | Whistleblowing Report | `ORDINARY` | **`HIGH`** | **`HIGH`** | Comunicaciones hechas por el interesado o sobre él en el canal interno de información, y su investigación. |
| `user.employment.working_time` | Working Time Record | `ORDINARY` | `MEDIUM` | **`HIGH`** | Inicio y fin diarios de la jornada del interesado, pausas y horas extraordinarias. |
| `user.employment.workplace_adjustment` | Workplace Adjustment | **`SPECIAL_CATEGORY`** | **`HIGH`** | **`HIGH`** | Adaptaciones del puesto, el horario o el lugar de trabajo del interesado por una discapacidad, un problema de salud o un embarazo, y las necesidades que las motivan. |

### Salud en el trabajo

Dos hijas de la estándar `user.health_and_medical`.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `user.health_and_medical.occupational_health` | Occupational Health | `SPECIAL_CATEGORY` | `HIGH` | `HIGH` | Vigilancia de la salud, valoraciones de aptitud para el trabajo, accidentes laborales y enfermedades profesionales del interesado. |
| `user.health_and_medical.sick_leave` | Sick Leave | `SPECIAL_CATEGORY` | `HIGH` | `HIGH` | Periodos de incapacidad temporal para el trabajo, partes médicos e informes de reincorporación. |

### Por qué estos niveles

- La **baja por enfermedad y la salud laboral** son datos de salud, categorías especiales con la máxima protección: el
  hecho de estar de baja ya concierne a la salud, y la ley española de prevención de riesgos laborales (Ley 31/1995,
  artículo 22) mantiene confidenciales los resultados de la vigilancia de la salud.
- Una **adaptación del puesto de trabajo** revela una discapacidad, un problema de salud o un embarazo, así que es una
  categoría especial de datos, con la máxima protección.
- La **retribución y la nómina** tienen confidencialidad e integridad altas, y también la **denuncia en el canal
  interno**: la ley de protección de las personas informantes (Ley 2/2023) exige mantener confidencial la identidad de
  la persona informante y registrar las comunicaciones.
- Las **comprobaciones de antecedentes, las evaluaciones de candidatos, las evaluaciones de desempeño, los expedientes
  disciplinarios, las extinciones y el control en el lugar de trabajo** tienen confidencialidad alta; los artículos 87 a
  91 de la LOPDGDD limitan el control de los dispositivos de los trabajadores, la videovigilancia y la geolocalización.
- El **contrato de trabajo y el registro de la jornada** tienen integridad alta: el Estatuto de los Trabajadores
  (artículo 34.9) exige conservar durante cuatro años el registro diario de la jornada.
- Las demás categorías heredan la confidencialidad media de `user.employment`.

## Dónde van los datos más sensibles

Un registro de una categoría de la extensión puede contener datos que revelan más que la categoría. Clasifica esos
campos con la clave más estricta, no con la de la extensión:

| El campo contiene | Clasifícalo como | Protección |
|---|---|---|
| El motivo médico de un permiso o ausencia | `user.health_and_medical.sick_leave` (no `user.employment.leave`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| El grado de discapacidad declarado para la retención | `user.health_and_medical.disability` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Los números de seguro de salud | `user.health_and_medical.insurance_beneficiary_id` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Un certificado de antecedentes penales o de delitos sexuales, o un despido por un delito | `user.criminal_history` | `CRIMINAL_OFFENCE`, `HIGH`, `HIGH` |
| Una sanción impuesta por una autoridad y no por el empleador | `user.administrative_sanction` (no `user.employment.disciplinary_record`) | `ORDINARY`, `HIGH`, `HIGH` |

## Usos de datos

Todos los usos son hijos del uso estándar `employment` (`employment.recruitment.background_check` cuelga del estándar
`employment.recruitment`). Los usos de datos no tienen facetas de protección.

| Clave | Nombre | Madre | Finalidad |
|---|---|---|---|
| `employment.compensation` | Compensation and Benefits | `employment` | Fija la retribución, revisa salarios, concede bonus y administra los planes de beneficios. |
| `employment.discipline` | Discipline | `employment` | Investiga y sanciona los incumplimientos de las normas del lugar de trabajo. |
| `employment.equality` | Equality and Diversity | `employment` | Elabora planes de igualdad, registros retributivos e informes de diversidad. |
| `employment.internal_reporting` | Internal Reporting Channel | `employment` | Recibe e investiga comunicaciones de infracciones y protege a las personas que las comunican. |
| `employment.learning` | Learning and Development | `employment` | Planifica, imparte y registra la formación y el desarrollo de la carrera. |
| `employment.leave` | Leave Management | `employment` | Gestiona vacaciones, permisos y ausencias. |
| `employment.monitoring` | Workplace Monitoring | `employment` | Controla dispositivos, instalaciones y vehículos de trabajo dentro de los límites que fija la ley. |
| `employment.occupational_safety` | Occupational Health and Safety | `employment` | Previene los riesgos laborales, vigila la salud de los trabajadores e investiga los accidentes. |
| `employment.offboarding` | Offboarding | `employment` | Pone fin al empleo: preaviso, finiquito, certificados y retirada de accesos y equipos. |
| `employment.onboarding` | Onboarding | `employment` | Prepara el inicio del empleo: contrato, alta, cuentas, equipos y acogida. |
| `employment.payroll` | Payroll | `employment` | Calcula y paga los salarios, retiene impuestos y cotizaciones y presenta las declaraciones correspondientes. |
| `employment.performance` | Performance Management | `employment` | Fija objetivos, evalúa el desempeño y planifica el desarrollo. |
| `employment.recruitment.background_check` | Background Check | `employment.recruitment` | Verifica la identidad, la titulación y la trayectoria de los candidatos antes de contratarlos. |
| `employment.time_and_attendance` | Time and Attendance | `employment` | Registra la jornada, los turnos y las horas extraordinarias como exige la ley. |
| `employment.workforce_planning` | Workforce Planning | `employment` | Diseña la organización, los puestos y la plantilla y planifica la sucesión. |

## Interesados

Todos los interesados de la extensión son planos, como los de fideslang, que no tienen jerarquía.

| Clave | Nombre | Quién |
|---|---|---|
| `dependant` | Dependant | Una persona que depende de un trabajador y está cubierta por un beneficio o declarada a efectos fiscales. |
| `former_employee` | Former Employee | Una persona cuyo empleo en la organización ha terminado. |
| `reporting_person` | Reporting Person | Una persona que comunica infracciones por el canal interno de información de la organización. |
| `retiree` | Retiree | Un antiguo empleado que recibe de la organización una pensión u otra prestación de jubilación. |
| `temporary_agency_worker` | Temporary Agency Worker | Un trabajador contratado por una empresa de trabajo temporal y destinado a la organización. |
| `worker_representative` | Worker Representative | Un trabajador elegido o designado para representar a la plantilla, como un miembro del comité de empresa. |

## Claves estándar para todo lo demás

La extensión añade solo lo que no tiene clave estándar. Lo que la taxonomía estándar ya cubre conserva su clave
estándar:

| Atributo | Clave estándar | Protección |
|---|---|---|
| Nombres, direcciones y contactos | `user.name`, `user.contact` y sus hijas | `ORDINARY`, `LOW`, `MEDIUM` |
| El contacto de emergencia | `user.contact.emergency_contact` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| La cuenta bancaria para el pago de la nómina | `user.financial.bank_account` | `ORDINARY`, `MEDIUM`, `HIGH` |
| El número de la seguridad social | `user.government_id.social_security_number` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| El cargo | `user.job_title` | `ORDINARY`, `LOW`, `MEDIUM` |
| El puesto | `user.workplace.role` | `ORDINARY`, `LOW`, `MEDIUM` |
| El origen étnico | `user.demographic.race_ethnicity` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| La afiliación sindical, incluidas las cuotas sindicales descontadas de la nómina | `user.demographic.trade_union_membership` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |

## Fuentes

Los términos se basan en las áreas de recursos humanos y nómina de los esquemas del **Common Data Model (CDM)**:
<https://github.com/microsoft/CDM> (documentación: <https://learn.microsoft.com/common-data-model/>). Copyright (c)
Microsoft Corporation. La documentación y demás contenido de CDM tienen licencia [Creative Commons Attribution 4.0
International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/); su código está bajo la licencia MIT.

Linneo no reproduce material de CDM (esquemas, definiciones de entidades o atributos, textos ni enumeraciones); CDM se
cita como fuente de los conceptos, y los nombres y descripciones de los términos son de Linneo. Linneo no está afiliado
a Microsoft ni cuenta con su respaldo. Véanse también `NOTICE` y la sección «Licence and attribution» del `README`.

Véase también: [Extensiones](README.md) · [Taxonomía estándar](../standard-taxonomy.md) · [Constantes de
claves](../key-constants.md) · [Niveles de protección](../protection-levels.md)
