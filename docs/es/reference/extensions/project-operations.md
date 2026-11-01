---
title: "Operaciones de proyecto"
parent: "Extensiones"
grand_parent: "Referencia"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/ProjectOperationsTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/ProjectOperationsCategories.java
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/ProjectOperationsDataUses.java
  - src/main/java/io/github/devbpm/linneo/extension/projectoperations/ProjectOperationsDataSubjects.java
-->

# Extensión de operaciones de proyecto

Paquete `io.github.devbpm.linneo.extension.projectoperations` — exportado, `@since 0.1.0`

Términos para las operaciones de proyecto, la gestión de servicios basados en proyectos: los proyectos y sus contratos,
estimaciones, costes e ingresos reales, financiación y tarifas, los datos de las personas que los dotan de personal, las
finalidades a las que sirven y las personas implicadas. La extensión es opcional y no se aplica por defecto; consulta
[Extensiones](README.md).

Se basa en las áreas de gestión y contabilidad de proyectos y de automatización de servicios de proyecto del Common Data
Model (CDM) (consulta [Fuentes](#fuentes)); no se reproduce material de CDM.

Los registros de proyecto cuelgan de `org.business_records`, que no son datos personales; lo que describe a una persona
como recurso de proyecto cuelga de `user.workplace`: las asignaciones y reservas (`user.workplace.project_assignment`),
las imputaciones de horas (`user.workplace.time_entry`), las competencias con las que se asigna a las personas al
trabajo (`user.workplace.skill`), su utilización (`user.workplace.utilization`) y sus tarifas
(`user.workplace.resource_rate`). El rol en el equipo del proyecto es la estándar `user.workplace.role`.

La extensión no depende de la extensión de finanzas: un gasto imputado a un proyecto es una nota de gastos de la
[extensión de finanzas](finance.md) si está aplicada y, si no, la estándar `user.financial`.

| Tipo | Descripción |
|---|---|
| `ProjectOperationsTaxonomy` | `public final class implements TaxonomyExtension`. `name()` devuelve `NAME`, `public static final String NAME = "project_operations"`, que se registra como origen de sus términos. Constructor público sin argumentos; sin estado. `contribute(builder)` añade los términos que se enumeran abajo. |
| `ProjectOperationsCategories` | `public final class` con constructor privado: un `public static final String` por cada categoría de datos que añade la extensión (11). |
| `ProjectOperationsDataUses` | Lo mismo para sus usos de datos (6). |
| `ProjectOperationsDataSubjects` | Lo mismo para sus interesados (2). |

Las constantes siguen la [regla de nombres](../key-constants.md#regla-de-nombres) de `Categories`:
`user.workplace.time_entry` es `ProjectOperationsCategories.USER_WORKPLACE_TIME_ENTRY` y
`operations.project_management.billing` es `ProjectOperationsDataUses.OPERATIONS_PROJECT_MANAGEMENT_BILLING`. Son
constantes de compilación, válidas en las anotaciones una vez aplicada la extensión.

## Aplicarla

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new ProjectOperationsTaxonomy()).build();
// Taxonomy[137 data categories, 69 data uses, 24 data subjects]
```

La extensión no comparte ninguna clave con las demás extensiones incluidas, así que puede aplicarse con cualquiera de
ellas, en el mismo builder:

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new ProjectOperationsTaxonomy())
        .extend(new UniversityTaxonomy())   // io.github.devbpm.linneo.extension.university.UniversityTaxonomy
        .build();
```

`Taxonomy.load()` solo la aplica si la aplicación la registra; consulta [Registrar una extensión
incluida](README.md#registrar-una-extensión-incluida).

Un modelo usa entonces las claves de la extensión junto a las estándar:

```java
package com.example.app;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsCategories;

import java.math.BigDecimal;

public class TimeEntry {
    @Category(Categories.USER_UNIQUE_ID)
    private String employeeNumber;

    @Category(ProjectOperationsCategories.USER_WORKPLACE_TIME_ENTRY)
    private BigDecimal hours;

    @Category(ProjectOperationsCategories.USER_WORKPLACE_RESOURCE_RATE)
    private BigDecimal costRate;
}
```

```java
new Classifier(taxonomy).verify(List.of(TimeEntry.class)).get(0).fields().forEach(f ->
        System.out.println(f.path() + ": " + f.category().key() + " " + f.category().nature() + " "
                + f.confidentiality() + " " + f.integrity()));
```

```text
employeeNumber: user.unique_id ORDINARY LOW MEDIUM
hours: user.workplace.time_entry ORDINARY LOW HIGH
costRate: user.workplace.resource_rate ORDINARY HIGH MEDIUM
```

## Reglas de fideslang para las etiquetas propias

La extensión sigue las reglas que fideslang fija para las etiquetas propias, de modo que sus términos sigan siendo
interoperables con otras herramientas:

- **Ampliar clases existentes.** Todos los términos son hijos de un término existente; todos los usos son hijos del uso
  estándar `operations`; `operations.project_management` es nuevo y los demás cuelgan de él.
- **La madre es el prefijo de la clave.** `user.workplace.time_entry` es hija de `user.workplace`.
- **Los interesados son planos.** Los interesados de fideslang no tienen jerarquía, así que todos los de la extensión
  son raíces.
- **Sin información de versión.** fideslang solo permite información de versión en su taxonomía por defecto, así que
  ningún término de la extensión la lleva; su origen se registra como `project_operations`.

## Categorías de datos

La protección que se muestra es la efectiva en la taxonomía construida. Los valores en **negrita** los declara la
extensión; el resto se heredan de la madre (`org.business_records` es `NONE`, `MEDIUM`, `MEDIUM`; `user.workplace` es
`ORDINARY`, `LOW`, `MEDIUM`). Las categorías bajo `org.business_records` no son datos personales; las demás sí. Ninguna
categoría es secreta.

### Proyectos

Registros sobre proyectos, bajo `org.business_records.project`: no son datos personales.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `org.business_records.project` | Project | `NONE` | `MEDIUM` | `MEDIUM` | Proyectos, estructuras de desglose del trabajo, tareas, dependencias, hitos y estado. |
| `org.business_records.project.actual` | Project Actual | `NONE` | `MEDIUM` | **`HIGH`** | Costes, ingresos y horas contabilizados en un proyecto. |
| `org.business_records.project.contract` | Project Contract | `NONE` | `MEDIUM` | `MEDIUM` | Contratos y líneas de contrato pactados con un cliente para un proyecto, con sus métodos de facturación y calendarios de facturas. |
| `org.business_records.project.estimate` | Project Estimate | `NONE` | `MEDIUM` | `MEDIUM` | Estimaciones, presupuestos y previsiones del coste y los ingresos de un proyecto. |
| `org.business_records.project.funding` | Project Funding | `NONE` | `MEDIUM` | `MEDIUM` | Fuentes de financiación, subvenciones, reglas de financiación y sus límites. |
| `org.business_records.project.rate` | Project Rate | `NONE` | **`HIGH`** | `MEDIUM` | Precios de coste y de venta de roles, categorías y tipos de transacción que se usan para presupuestar proyectos. |

### Personas como recursos de proyecto

Lo que describe a una persona en un proyecto, bajo `user.workplace`.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `user.workplace.project_assignment` | Project Assignment | `ORDINARY` | `LOW` | `MEDIUM` | Proyectos, tareas y roles a los que está asignado el interesado, con reservas y horas planificadas. |
| `user.workplace.resource_rate` | Resource Rate | `ORDINARY` | **`HIGH`** | `MEDIUM` | Tarifas de coste y de facturación fijadas para el interesado como recurso de proyecto. |
| `user.workplace.skill` | Skill | `ORDINARY` | `LOW` | `MEDIUM` | Competencias, certificaciones y niveles de dominio que se usan para asignar al interesado a un trabajo. |
| `user.workplace.time_entry` | Time Entry | `ORDINARY` | `LOW` | **`HIGH`** | Horas que el interesado imputa a proyectos y tareas, con sus descripciones y aprobaciones. |
| `user.workplace.utilization` | Utilization | `ORDINARY` | **`MEDIUM`** | `MEDIUM` | Horas facturables y productivas del interesado frente a su capacidad y a los objetivos. |

### Por qué estos niveles

- Las **tarifas de proyecto y las tarifas de recurso** tienen confidencialidad alta: son comercialmente sensibles, y la
  tarifa de coste de una persona revela lo que la organización le paga.
- Las **imputaciones de horas y los reales de proyecto** tienen integridad alta, porque con ellos se factura a los
  clientes y se reconocen los ingresos.
- La **utilización** tiene confidencialidad media, porque mide cómo rinde un trabajador y los artículos 87 a 91 de la
  LOPDGDD limitan el control de los trabajadores.
- Las demás categorías heredan la confidencialidad media de `org.business_records` y la confidencialidad baja de
  `user.workplace`.

## Dónde van los datos más sensibles

Un registro de una categoría de la extensión puede contener datos que revelan más que la categoría. Clasifica esos
campos con la clave más estricta, no con la de la extensión:

| El campo contiene | Clasifícalo como | Protección |
|---|---|---|
| El motivo médico por el que un recurso no está disponible | `user.health_and_medical` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Una imputación de horas usada para evaluar el rendimiento de un trabajador | `user.employment.performance` de la [extensión de recursos humanos](human-resources.md), si está aplicada | `ORDINARY`, `HIGH`, `MEDIUM` |

## Usos de datos

Todos los usos son hijos del uso estándar `operations`; `operations.project_management` es nuevo y los demás cuelgan de
él. Los usos de datos no tienen facetas de protección.

| Clave | Nombre | Madre | Finalidad |
|---|---|---|---|
| `operations.project_management` | Project Management | `operations` | Planifica, ejecuta y controla proyectos. |
| `operations.project_management.accounting` | Project Accounting | `operations.project_management` | Registra los costes e ingresos del proyecto, reconoce los ingresos e informa de la rentabilidad. |
| `operations.project_management.billing` | Project Billing | `operations.project_management` | Factura a los clientes el trabajo del proyecto según prevé el contrato. |
| `operations.project_management.estimating` | Project Estimating | `operations.project_management` | Prepara estimaciones y presupuestos del trabajo de un proyecto. |
| `operations.project_management.resource_management` | Resource Management | `operations.project_management` | Solicita, programa y reserva personas para proyectos según sus competencias y disponibilidad. |
| `operations.project_management.time_and_expense` | Time and Expense | `operations.project_management` | Registra y aprueba el tiempo y los gastos imputados a proyectos. |

## Interesados

Todos los interesados de la extensión son planos, como los de fideslang, que no tienen jerarquía.

| Clave | Nombre | Quién |
|---|---|---|
| `project_stakeholder` | Project Stakeholder | Una persona del cliente o de un socio que participa en un proyecto, como un patrocinador o un aprobador. |
| `subcontractor` | Subcontractor | Una persona que trabaja en los proyectos de la organización con un contrato con la organización o con un proveedor, y no con una relación laboral. |

## Claves estándar para todo lo demás

La extensión añade solo lo que no tiene clave estándar. Lo que la taxonomía estándar ya cubre conserva su clave
estándar:

| Atributo | Clave estándar | Protección |
|---|---|---|
| El rol de una persona en el equipo del proyecto | `user.workplace.role` | `ORDINARY`, `LOW`, `MEDIUM` |
| Un gasto imputado a un proyecto | `user.financial` (o `user.financial.expense` si está aplicada la [extensión de finanzas](finance.md)) | `ORDINARY`, `MEDIUM`, `HIGH` |

## Fuentes

Los términos se basan en las áreas de gestión y contabilidad de proyectos y de automatización de servicios de proyecto
de los esquemas del **Common Data Model (CDM)**: <https://github.com/microsoft/CDM> (documentación:
<https://learn.microsoft.com/common-data-model/>). Copyright (c) Microsoft Corporation. La documentación y demás
contenido de CDM tienen licencia [Creative Commons Attribution 4.0 International (CC BY
4.0)](https://creativecommons.org/licenses/by/4.0/); su código está bajo la licencia MIT.

Linneo no reproduce material de CDM (esquemas, definiciones de entidades o atributos, textos ni enumeraciones); CDM se
cita como fuente de los conceptos, y los nombres y descripciones de los términos son de Linneo. Linneo no está afiliado
a Microsoft ni cuenta con su respaldo. Véanse también `NOTICE` y la sección «Licence and attribution» del `README`.

Véase también: [Extensiones](README.md) · [Taxonomía estándar](../standard-taxonomy.md) · [Constantes de
claves](../key-constants.md) · [Niveles de protección](../protection-levels.md)
