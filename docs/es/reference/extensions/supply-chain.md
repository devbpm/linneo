---
title: "Cadena de suministro"
parent: "Extensiones"
grand_parent: "Referencia"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/SupplyChainTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/SupplyChainCategories.java
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/SupplyChainDataUses.java
  - src/main/java/io/github/devbpm/linneo/extension/supplychain/SupplyChainDataSubjects.java
-->

# Extensión de cadena de suministro

Paquete `io.github.devbpm.linneo.extension.supplychain` — exportado, `@since 0.1.0`

Términos para la gestión de la cadena de suministro: productos y sus fórmulas y costes, compras, inventario, producción,
calidad, planificación, pedidos de venta, transporte y servicio posventa, los datos personales que tocan estos procesos,
las finalidades a las que sirven y las personas implicadas. La extensión es opcional y no se aplica por defecto;
consulta [Extensiones](README.md).

Se basa en las áreas de negocio de la cadena de suministro del Common Data Model (CDM) (consulta [Fuentes](#fuentes));
no se reproduce material de CDM.

La mayor parte de los datos de la cadena de suministro trata de organizaciones, productos y mercancías y cuelga de
`org.business_records`, que no son datos personales. Los almacenes y centros son la estándar `org.location`, y los
proveedores y clientes que son organizaciones la estándar `org.legal_entity`. Aparecen datos personales donde un proceso
toca a personas: los pedidos de un consumidor son la estándar `user.behavior.purchase_history`, una entrega a una
persona es `user.contact.delivery` (datos de entrega), y los trabajadores que conducen, preparan pedidos y producen
dejan datos de registro de conducción (`user.workplace.driving_record`) y de seguimiento laboral
(`user.workplace.labor_tracking`).

| Tipo | Descripción |
|---|---|
| `SupplyChainTaxonomy` | `public final class implements TaxonomyExtension`. `name()` devuelve `NAME`, `public static final String NAME = "supply_chain"`, que se registra como origen de sus términos. Constructor público sin argumentos; sin estado. `contribute(builder)` añade los términos que se enumeran abajo. |
| `SupplyChainCategories` | `public final class` con constructor privado: un `public static final String` por cada categoría de datos que añade la extensión (16). |
| `SupplyChainDataUses` | Lo mismo para sus usos de datos (12). |
| `SupplyChainDataSubjects` | Lo mismo para sus interesados (3). |

Las constantes siguen la [regla de nombres](../key-constants.md#regla-de-nombres) de `Categories`:
`org.business_records.inventory` es `SupplyChainCategories.ORG_BUSINESS_RECORDS_INVENTORY` y
`operations.logistics.fleet_management` es `SupplyChainDataUses.OPERATIONS_LOGISTICS_FLEET_MANAGEMENT`. Son constantes
de compilación, válidas en las anotaciones una vez aplicada la extensión.

## Aplicarla

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new SupplyChainTaxonomy()).build();
// Taxonomy[142 data categories, 75 data uses, 25 data subjects]
```

La extensión no comparte ninguna clave con las demás extensiones incluidas, así que puede aplicarse con cualquiera de
ellas, en el mismo builder:

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new SupplyChainTaxonomy())
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
import io.github.devbpm.linneo.extension.supplychain.SupplyChainCategories;

public class Delivery {
    @Category(Categories.USER_NAME)
    private String recipientName;

    @Category(SupplyChainCategories.USER_CONTACT_DELIVERY)
    private String address;

    @Category(SupplyChainCategories.USER_WORKPLACE_DRIVING_RECORD)
    private String driverTachograph;

    @Category(Categories.ORG_BUSINESS_RECORDS)
    private String orderReference;
}
```

```java
new Classifier(taxonomy).verify(List.of(Delivery.class)).get(0).fields().forEach(f ->
        System.out.println(f.field() + ": " + f.category().key() + " " + f.category().nature() + " "
                + f.confidentiality() + " " + f.integrity()));
```

```text
recipientName: user.name ORDINARY LOW MEDIUM
address: user.contact.delivery ORDINARY MEDIUM MEDIUM
driverTachograph: user.workplace.driving_record ORDINARY MEDIUM HIGH
orderReference: org.business_records NONE MEDIUM MEDIUM
```

## Reglas de fideslang para las etiquetas propias

La extensión sigue las reglas que fideslang fija para las etiquetas propias, de modo que sus términos sigan siendo
interoperables con otras herramientas:

- **Ampliar clases existentes.** Todos los términos son hijos de un término existente; todos los usos son hijos de los
  usos estándar `operations` o `sales`.
- **La madre es el prefijo de la clave.** `org.business_records.inventory` es hija de `org.business_records`.
- **Los interesados son planos.** Los interesados de fideslang no tienen jerarquía, así que todos los de la extensión
  son raíces.
- **Sin información de versión.** fideslang solo permite información de versión en su taxonomía por defecto, así que
  ningún término de la extensión la lleva; su origen se registra como `supply_chain`.

## Categorías de datos

La protección que se muestra es la efectiva en la taxonomía construida. Los valores en **negrita** los declara la
extensión; el resto se heredan de la madre (`org.business_records` es `NONE`, `MEDIUM`, `MEDIUM`; `user.contact` es
`ORDINARY`, `LOW`, `MEDIUM`; `user.workplace` es `ORDINARY`, `LOW`, `MEDIUM`). Las categorías bajo
`org.business_records` no son datos personales; las demás sí. Ninguna categoría es secreta.

### Productos, compras, producción y logística

Registros sobre organizaciones, productos y mercancías, bajo `org.business_records`: no son datos personales.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `org.business_records.inventory` | Inventory | `NONE` | `MEDIUM` | **`HIGH`** | Existencias, almacenes y ubicaciones, movimientos de stock, recuentos, lotes y números de serie. |
| `org.business_records.planning` | Supply Planning | `NONE` | `MEDIUM` | `MEDIUM` | Previsiones de demanda, planes maestros, pedidos planificados y planes de capacidad. |
| `org.business_records.price_agreement` | Price Agreement | `NONE` | `MEDIUM` | `MEDIUM` | Tarifas, acuerdos comerciales, descuentos y rappels pactados con clientes y proveedores. |
| `org.business_records.procurement` | Procurement | `NONE` | `MEDIUM` | `MEDIUM` | Solicitudes de compra, peticiones de oferta, pedidos de compra, recepciones y acuerdos con proveedores. |
| `org.business_records.procurement.bid` | Supplier Bid | `NONE` | **`HIGH`** | `MEDIUM` | Presupuestos y ofertas recibidos de proveedores, y su evaluación, antes de la adjudicación. |
| `org.business_records.product` | Product | `NONE` | `MEDIUM` | `MEDIUM` | Productos y sus variantes, especificaciones, unidades, clasificaciones y atributos. |
| `org.business_records.product.cost` | Product Cost | `NONE` | **`HIGH`** | `MEDIUM` | Costes estándar, escandallos de coste y márgenes de los productos. |
| `org.business_records.product.formula` | Formula | `NONE` | **`HIGH`** | `MEDIUM` | Listas de materiales, fórmulas, recetas y rutas, que a menudo son secretos comerciales. |
| `org.business_records.production` | Production | `NONE` | `MEDIUM` | `MEDIUM` | Órdenes de producción, rutas, recursos, capacidad y resultados de planta. |
| `org.business_records.quality` | Quality | `NONE` | `MEDIUM` | **`HIGH`** | Órdenes de calidad, ensayos, no conformidades y certificados de análisis. |
| `org.business_records.sales_order` | Sales Order | `NONE` | `MEDIUM` | `MEDIUM` | Presupuestos, pedidos, confirmaciones, entregas y devoluciones con clientes que son organizaciones. |
| `org.business_records.service_order` | Service Order | `NONE` | `MEDIUM` | `MEDIUM` | Contratos de servicio, órdenes de servicio y reparaciones de los productos vendidos. |
| `org.business_records.shipment` | Shipment | `NONE` | `MEDIUM` | `MEDIUM` | Cargas, envíos, transportistas, rutas, fletes y documentos de aduanas. |

### Personas en la cadena de suministro

Datos personales de destinatarios y trabajadores, bajo `user.contact` y `user.workplace`.

| Clave | Nombre | Naturaleza | Confidencialidad | Integridad | Contiene |
|---|---|---|---|---|---|
| `user.contact.delivery` | Delivery Details | `ORDINARY` | **`MEDIUM`** | `MEDIUM` | Destinatario, dirección de entrega, instrucciones, franja horaria y prueba de entrega, como una firma o una foto. |
| `user.workplace.driving_record` | Driving Record | `ORDINARY` | **`MEDIUM`** | **`HIGH`** | Datos del tacógrafo, tiempos de conducción y descanso, rutas y geolocalización del vehículo de un conductor. |
| `user.workplace.labor_tracking` | Labour Tracking | `ORDINARY` | **`MEDIUM`** | `MEDIUM` | Tareas, lecturas y unidades manejadas por un trabajador de almacén o de producción, y la productividad que se mide con ellas. |

### Por qué estos niveles

- Las **fórmulas, los costes de producto y las ofertas de proveedores** tienen confidencialidad alta: las fórmulas y los
  costes suelen ser secretos comerciales, y las ofertas deben mantenerse confidenciales hasta la adjudicación.
- Los **registros de inventario y de calidad** tienen integridad alta, porque el valor de las existencias alimenta la
  contabilidad y los registros de calidad y de lotes sustentan las retiradas de producto y las obligaciones de seguridad
  de los productos.
- Un **registro de conducción** tiene confidencialidad media e integridad alta: los registros del tacógrafo deben
  conservarse íntegros, y la geolocalización de los trabajadores está restringida por el artículo 90 de la LOPDGDD.
- Los **datos de entrega y el seguimiento laboral** tienen confidencialidad media, porque revelan dónde está una persona
  y cómo rinde un trabajador (los artículos 87 a 91 de la LOPDGDD limitan el control de los trabajadores).
- Las demás categorías heredan la confidencialidad media de `org.business_records`.

## Dónde van los datos más sensibles

Un registro de una categoría de la extensión puede contener datos que revelan más que la categoría. Clasifica esos
campos con la clave más estricta, no con la de la extensión:

| El campo contiene | Clasifícalo como | Protección |
|---|---|---|
| Un pedido o una entrega de medicamentos o productos sanitarios para una persona | `user.health_and_medical` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| La aptitud médica de un conductor | `user.health_and_medical` (no `user.workplace.driving_record`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |

## Usos de datos

Todos los usos son hijos de los usos estándar `operations` o `sales`. Los usos de datos no tienen facetas de protección.

| Clave | Nombre | Madre | Finalidad |
|---|---|---|---|
| `operations.after_sales_service` | After-Sales Service | `operations` | Presta garantías, reparaciones y servicio en campo para los productos vendidos. |
| `operations.inventory` | Inventory Management | `operations` | Recibe, almacena, cuenta y mueve existencias. |
| `operations.logistics` | Logistics | `operations` | Planifica y realiza el transporte y las entregas. |
| `operations.logistics.fleet_management` | Fleet Management | `operations.logistics` | Gestiona vehículos y conductores, incluidos los tiempos de conducción y la geolocalización de los vehículos. |
| `operations.planning` | Supply Planning | `operations` | Prevé la demanda y planifica el suministro, la producción y la capacidad. |
| `operations.procurement` | Procurement | `operations` | Busca, pide y recibe bienes y servicios de proveedores. |
| `operations.product_management` | Product Information Management | `operations` | Define los productos, sus especificaciones, fórmulas y costes. |
| `operations.production` | Production | `operations` | Planifica y ejecuta la producción y registra sus resultados. |
| `operations.quality` | Quality Management | `operations` | Inspecciona y ensaya productos y materiales y gestiona las no conformidades. |
| `operations.supplier_management` | Supplier Management | `operations` | Homologa, evalúa y audita a los proveedores. |
| `operations.traceability` | Traceability | `operations` | Sigue lotes y números de serie a lo largo de la cadena de suministro para retiradas de producto y reguladores. |
| `sales.order_management` | Order Management | `sales` | Recibe, confirma, sirve y factura los pedidos de clientes y gestiona las devoluciones. |

## Interesados

Todos los interesados de la extensión son planos, como los de fideslang, que no tienen jerarquía.

| Clave | Nombre | Quién |
|---|---|---|
| `delivery_recipient` | Delivery Recipient | Una persona que recibe o firma una entrega y que puede no ser el cliente. |
| `driver` | Driver | Una persona que conduce un vehículo con mercancías de la organización, como empleado o por cuenta de un transportista. |
| `supplier_contact` | Supplier Contact | Una persona que actúa en nombre de una organización proveedora, como un representante comercial. |

## Claves estándar para todo lo demás

La extensión añade solo lo que no tiene clave estándar. Lo que la taxonomía estándar ya cubre conserva su clave
estándar:

| Atributo | Clave estándar | Protección |
|---|---|---|
| Almacenes y centros | `org.location` | `NONE`, `LOW`, `MEDIUM` |
| Proveedores y clientes que son organizaciones | `org.legal_entity` | `NONE`, `PUBLIC`, `MEDIUM` |
| Los pedidos de un consumidor | `user.behavior.purchase_history` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| Otros datos sobre productos y mercancías | `org.business_records` y sus hijas | `NONE`, `MEDIUM`, `MEDIUM` |

## Fuentes

Los términos se basan en las áreas de negocio de la cadena de suministro de los esquemas del **Common Data Model
(CDM)**: <https://github.com/microsoft/CDM> (documentación: <https://learn.microsoft.com/common-data-model/>). Copyright
(c) Microsoft Corporation. La documentación y demás contenido de CDM tienen licencia [Creative Commons Attribution 4.0
International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/); su código está bajo la licencia MIT.

Linneo no reproduce material de CDM (esquemas, definiciones de entidades o atributos, textos ni enumeraciones); CDM se
cita como fuente de los conceptos, y los nombres y descripciones de los términos son de Linneo. Linneo no está afiliado
a Microsoft ni cuenta con su respaldo. Véanse también `NOTICE` y la sección «Licence and attribution» del `README`.

Véase también: [Extensiones](README.md) · [Taxonomía estándar](../standard-taxonomy.md) · [Constantes de
claves](../key-constants.md) · [Niveles de protección](../protection-levels.md)
