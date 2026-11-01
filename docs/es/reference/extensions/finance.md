---
title: "Finanzas"
parent: "Extensiones"
grand_parent: "Referencia"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/extension/finance/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/finance/FinanceTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/finance/FinanceCategories.java
  - src/main/java/io/github/devbpm/linneo/extension/finance/FinanceDataUses.java
  - src/main/java/io/github/devbpm/linneo/extension/finance/FinanceDataSubjects.java
-->

# Extensión de finanzas

Paquete `io.github.devbpm.linneo.extension.finance` — exportado, `@since 0.1.0`

Términos para finanzas: los libros de la organización, facturas, cuentas a pagar y a cobrar, tesorería, presupuestos,
inmovilizado e impuestos, los datos financieros que guarda sobre clientes, proveedores, empleados y otras personas, las
finalidades de finanzas para las que los trata y las personas implicadas. La extensión es opcional y no se aplica por
defecto; consulta [Extensiones](README.md).

Se basa en las áreas de negocio de finanzas del Common Data Model (CDM) (consulta [Fuentes](#fuentes)); no se reproduce
material de CDM.

Las entidades de CDM se convierten en categorías de datos por lo que revelan, no una por una: el libro mayor, los
asientos y las dimensiones financieras son la categoría `org.business_records.ledger`, las entidades de viajes y gastos
son `user.financial.expense` (notas de gastos), y la cuenta bancaria de un cliente o proveedor es la estándar
`user.financial.bank_account` cuando el cliente o proveedor es una persona física.

Los registros sobre organizaciones cuelgan de `org.business_records`, que no son datos personales; los registros sobre
personas físicas, incluidos los proveedores autónomos y los consumidores, cuelgan de `user.financial` o
`user.workplace`, de modo que el `Classifier` los trata como datos personales.

| Tipo | Descripción |
|---|---|
| `FinanceTaxonomy` | `public final class implements TaxonomyExtension`. `name()` devuelve `NAME`, `public static final String NAME = "finance"`, que se registra como origen de sus términos. Constructor público sin argumentos; sin estado. `contribute(builder)` añade los términos que se enumeran abajo. |
| `FinanceCategories` | `public final class` con constructor privado: un `public static final String` por cada categoría de datos que añade la extensión (19). |
| `FinanceDataUses` | Lo mismo para sus usos de datos (13). |
| `FinanceDataSubjects` | Lo mismo para sus interesados (3). |

Las constantes siguen la [regla de nombres](../key-constants.md#regla-de-nombres) de `Categories`:
`user.financial.expense` es `FinanceCategories.USER_FINANCIAL_EXPENSE` y `finance.accounts_payable` es
`FinanceDataUses.FINANCE_ACCOUNTS_PAYABLE`. Son constantes de compilación, válidas en las anotaciones una vez aplicada
la extensión.

## Aplicarla

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new FinanceTaxonomy()).build();
// Taxonomy[145 data categories, 76 data uses, 25 data subjects]
```

La extensión no comparte ninguna clave con las demás extensiones incluidas, así que puede aplicarse con cualquiera de
ellas, en el mismo builder:

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new FinanceTaxonomy())
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
import io.github.devbpm.linneo.extension.finance.FinanceCategories;

import java.math.BigDecimal;

public class ExpenseClaim {
    @Category(Categories.USER_UNIQUE_ID)
    private String employeeNumber;

    @Category(FinanceCategories.USER_FINANCIAL_EXPENSE)
    private BigDecimal amount;

    @Category(FinanceCategories.USER_FINANCIAL_EXPENSE_POLICY_VIOLATION)
    private String justification;

    @Category(Categories.USER_HEALTH_AND_MEDICAL)   // a claim that reveals a health condition
    private String treatment;
}
```

```java
new Classifier(taxonomy).verify(List.of(ExpenseClaim.class)).get(0).fields().forEach(f ->
        System.out.println(f.field() + ": " + f.category().key() + " " + f.category().nature() + " "
                + f.confidentiality() + " " + f.integrity()));
```

```text
employeeNumber: user.unique_id ORDINARY LOW MEDIUM
amount: user.financial.expense ORDINARY MEDIUM HIGH
justification: user.financial.expense.policy_violation ORDINARY HIGH HIGH
treatment: user.health_and_medical SPECIAL_CATEGORY HIGH HIGH
```

## Reglas de fideslang para las etiquetas propias

La extensión sigue las reglas que fideslang fija para las etiquetas propias, de modo que sus términos sigan siendo
interoperables con otras herramientas:

- **Ampliar clases existentes.** Todos los términos son hijos de un término existente; todos los usos son hijos del uso
  estándar `finance`.
- **La madre es el prefijo de la clave.** `user.financial.expense` es hija de `user.financial`.
- **Los interesados son planos.** Los interesados de fideslang no tienen jerarquía, así que todos los de la extensión
  son raíces.
- **Sin información de versión.** fideslang solo permite información de versión en su taxonomía por defecto, así que
  ningún término de la extensión la lleva; su origen se registra como `finance`.

## Categorías de datos

La protección que se muestra es la efectiva en la taxonomía construida. Los valores en **negrita** los declara la
extensión; el resto se heredan de la madre (`org.business_records` es `NONE`, `MEDIUM`, `MEDIUM`; `user.financial` es
`ORDINARY`, `MEDIUM`, `HIGH`; `user.workplace` es `ORDINARY`, `LOW`, `MEDIUM`). Las categorías bajo
`org.business_records` no son datos personales; las demás sí. Ninguna categoría es secreta.

### Los libros de la organización

Registros sobre la propia organización, bajo `org.business_records`: no son datos personales.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `org.business_records.budget` | Budget | `NONE` | `MEDIUM` | `MEDIUM` | Planes presupuestarios, asignaciones, revisiones y los controles que se les aplican. |
| `org.business_records.cost_accounting` | Cost Accounting | `NONE` | `MEDIUM` | `MEDIUM` | Centros de coste, elementos de coste, asignaciones y los costes y resultados que miden. |
| `org.business_records.financial_statement` | Financial Statement | `NONE` | `MEDIUM` | **`HIGH`** | Balances, cuentas de pérdidas y ganancias, estados de flujos de efectivo, cierre de periodo y consolidación. |
| `org.business_records.fixed_asset` | Fixed Asset | `NONE` | `MEDIUM` | `MEDIUM` | El registro de activos: adquisiciones, amortización, revalorizaciones, traspasos y bajas. |
| `org.business_records.invoice` | Invoice | `NONE` | `MEDIUM` | **`HIGH`** | Facturas emitidas a organizaciones y recibidas de ellas, con el contenido que exige la ley fiscal. |
| `org.business_records.ledger` | Ledger | `NONE` | `MEDIUM` | **`HIGH`** | Plan de cuentas, asientos, saldos y dimensiones financieras de los libros de la organización. |
| `org.business_records.payable` | Payable | `NONE` | `MEDIUM` | `MEDIUM` | Importes que se deben a proveedores que son organizaciones: partidas abiertas, propuestas de pago y pagos. |
| `org.business_records.receivable` | Receivable | `NONE` | `MEDIUM` | `MEDIUM` | Importes que deben los clientes que son organizaciones: partidas abiertas, cobros recibidos y liquidaciones. |
| `org.business_records.tax` | Tax Record | `NONE` | `MEDIUM` | **`HIGH`** | Códigos de impuestos, cálculos, declaraciones y liquidaciones de los impuestos de la propia organización. |
| `org.business_records.treasury` | Treasury | `NONE` | `MEDIUM` | **`HIGH`** | Las cuentas bancarias de la organización, extractos, conciliaciones, posición de tesorería y previsiones. |

### Clientes, proveedores y otras personas

Datos financieros de personas físicas, bajo `user.financial`.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `user.financial.account_balance` | Account Balance | `ORDINARY` | `MEDIUM` | `HIGH` | Importes pendientes, pagos, liquidaciones y condiciones de pago del interesado como cliente o proveedor. |
| `user.financial.credit` | Creditworthiness | `ORDINARY` | **`HIGH`** | `HIGH` | Límites de crédito, puntuaciones de crédito, comprobaciones de solvencia e historial de impagos del interesado. |
| `user.financial.debt_collection` | Debt Collection | `ORDINARY` | **`HIGH`** | `HIGH` | Recordatorios, reclamaciones, planes de pago, derivaciones a agencias de cobro y bajas de importes que debe el interesado. |
| `user.financial.due_diligence` | Due Diligence | `ORDINARY` | **`HIGH`** | `HIGH` | Identificación del interesado, titularidad real, condición de persona con responsabilidad pública y cribado de sanciones conforme a la normativa contra el blanqueo de capitales. |
| `user.financial.expense` | Expense Claim | `ORDINARY` | `MEDIUM` | `HIGH` | Informes de viajes y gastos, justificantes, kilometraje, dietas, anticipos de efectivo y operaciones con tarjeta corporativa del interesado. |
| `user.financial.expense.policy_violation` | Expense Policy Violation | `ORDINARY` | **`HIGH`** | `HIGH` | Incumplimientos de la política de gastos detectados en las notas de gastos del interesado, con las justificaciones aportadas. |
| `user.financial.invoice` | Personal Invoice | `ORDINARY` | `MEDIUM` | `HIGH` | Facturas emitidas al interesado o por él, como un consumidor o un proveedor autónomo. |
| `user.financial.tax_status` | Tax Status | `ORDINARY` | **`HIGH`** | `HIGH` | Residencia fiscal, condición de sujeto del IVA o de autónomo, retenciones sobre los pagos al interesado y las declaraciones informativas que las comunican. |

### Facultades de firma

Los límites delegados en una persona, bajo `user.workplace`.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `user.workplace.signing_authority` | Signing Authority | `ORDINARY` | `LOW` | **`HIGH`** | Límites de aprobación y de gasto delegados en el interesado, y su revocación. |

### Por qué estos niveles

- La **solvencia, el cobro de deudas, la situación fiscal y los incumplimientos de la política de gastos** tienen
  confidencialidad alta: los datos de solvencia e impago pueden excluir al interesado del crédito (el artículo 20 de la
  LOPDGDD regula los ficheros que los comparten), los datos fiscales son confidenciales y un incumplimiento de la
  política puede dar lugar a una medida disciplinaria.
- La **diligencia debida contra el blanqueo de capitales** (`user.financial.due_diligence`) tiene confidencialidad e
  integridad altas: registra si el interesado es una persona con responsabilidad pública o figura en listas de
  sanciones, y la norma que la exige exige también que sus registros se conserven íntegros.
- El **libro mayor, los estados financieros, las facturas, los impuestos, la tesorería y las facultades de firma**
  tienen integridad alta, porque la ley mercantil y la fiscal exigen libros y facturas exactos y un límite falsificado
  autoriza pagos.
- Las demás categorías heredan la confidencialidad media de `org.business_records` y la confidencialidad media e
  integridad alta de `user.financial`.

## Dónde van los datos más sensibles

Un registro de una categoría de la extensión puede contener datos que revelan más que la categoría. Clasifica esos
campos con la clave más estricta, no con la de la extensión:

| El campo contiene | Clasifícalo como | Protección |
|---|---|---|
| Una deducción o un pago que revela la afiliación sindical | `user.demographic.trade_union_membership` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Un gasto que revela un problema de salud | `user.health_and_medical` (no `user.financial.expense`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Una comunicación de operación sospechosa, o una condena detectada en la diligencia debida | `user.criminal_history` (no `user.financial.due_diligence`) | `CRIMINAL_OFFENCE`, `HIGH`, `HIGH` |

## Usos de datos

Todos los usos son hijos del uso estándar `finance`. Los usos de datos no tienen facetas de protección.

| Clave | Nombre | Madre | Finalidad |
|---|---|---|---|
| `finance.accounting` | Accounting | `finance` | Lleva los libros y prepara los estados financieros que exige la ley mercantil. |
| `finance.accounts_payable` | Accounts Payable | `finance` | Registra, aprueba y paga las facturas de proveedores. |
| `finance.accounts_receivable` | Accounts Receivable | `finance` | Factura a los clientes y registra y concilia sus pagos. |
| `finance.anti_money_laundering` | Anti-Money Laundering | `finance` | Identifica a los clientes y sus titulares reales, los coteja con las listas de sanciones y comunica las operaciones sospechosas. |
| `finance.audit` | Audit | `finance` | Apoya las auditorías internas y externas de las cuentas y de los controles financieros. |
| `finance.budgeting` | Budgeting | `finance` | Planifica, asigna y controla presupuestos y previsiones financieras. |
| `finance.cost_accounting` | Cost Accounting | `finance` | Imputa costes a centros de coste, productos y actividades para medir sus resultados. |
| `finance.credit_management` | Credit Management | `finance` | Evalúa la solvencia de los clientes y fija y vigila sus límites de crédito. |
| `finance.debt_collection` | Debt Collection | `finance` | Persigue los importes vencidos mediante recordatorios, planes de pago, agencias de cobro y reclamaciones. |
| `finance.expense_management` | Expense Management | `finance` | Tramita las notas de viajes y gastos, los anticipos y las tarjetas corporativas, y los comprueba con la política. |
| `finance.fixed_assets` | Fixed Asset Management | `finance` | Registra, amortiza y da de baja los activos de la organización. |
| `finance.tax` | Tax Compliance | `finance` | Calcula, presenta y paga impuestos y conserva los registros que exige la ley fiscal. |
| `finance.treasury` | Treasury | `finance` | Gestiona cuentas bancarias, pagos, efectivo y liquidez. |

## Interesados

Todos los interesados de la extensión son planos, como los de fideslang, que no tienen jerarquía.

| Clave | Nombre | Quién |
|---|---|---|
| `beneficial_owner` | Beneficial Owner | Una persona que posee o controla en última instancia a un cliente, proveedor u otra persona jurídica. |
| `guarantor` | Guarantor | Una persona que garantiza las obligaciones de un cliente o de otro deudor. |
| `payee` | Payee | Una persona a la que paga la organización y que no es su cliente, proveedor ni empleado, como un reclamante o el ganador de un premio. |

## Claves estándar para todo lo demás

La extensión añade solo lo que no tiene clave estándar. Lo que la taxonomía estándar ya cubre conserva su clave
estándar:

| Atributo | Clave estándar | Protección |
|---|---|---|
| La cuenta bancaria de un cliente o proveedor que es persona física | `user.financial.bank_account` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Otros datos financieros de una persona física | `user.financial` y sus hijas | `ORDINARY`, `MEDIUM`, `HIGH` |
| Otros datos sobre una organización | `org.business_records` y sus hijas | `NONE`, `MEDIUM`, `MEDIUM` |
| Datos de puesto de trabajo de una persona | `user.workplace` y sus hijas | `ORDINARY`, `LOW`, `MEDIUM` |

## Fuentes

Los términos se basan en las áreas de negocio de finanzas de los esquemas del **Common Data Model (CDM)**:
<https://github.com/microsoft/CDM> (documentación: <https://learn.microsoft.com/common-data-model/>). Copyright (c)
Microsoft Corporation. La documentación y demás contenido de CDM tienen licencia [Creative Commons Attribution 4.0
International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/); su código está bajo la licencia MIT.

Linneo no reproduce material de CDM (esquemas, definiciones de entidades o atributos, textos ni enumeraciones); CDM se
cita como fuente de los conceptos, y los nombres y descripciones de los términos son de Linneo. Linneo no está afiliado
a Microsoft ni cuenta con su respaldo. Véanse también `NOTICE` y la sección «Licence and attribution» del `README`.

Véase también: [Extensiones](README.md) · [Taxonomía estándar](../standard-taxonomy.md) · [Constantes de
claves](../key-constants.md) · [Niveles de protección](../protection-levels.md)
