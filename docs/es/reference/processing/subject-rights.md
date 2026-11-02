---
title: "SubjectRights"
parent: "Tratamientos"
grand_parent: "Referencia"
nav_order: 5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/SubjectRights.java
  - src/main/java/io/github/devbpm/linneo/Trait.java
-->

# SubjectRights

`io.github.devbpm.linneo.processing.SubjectRights` — `public final class`, `@since 0.1.0`

Lo que el [registro de actividades de tratamiento](record.md) dice a alguien de una categoría de interesados que ejerce
sus derechos (RGPD, artículos 15 a 22): las actividades que tratan sus datos, qué trata cada una y por qué, dónde se
guardan esos datos y qué derechos le da la base jurídica de cada actividad. Las instancias son inmutables; no hay
constructor público.

Es el **punto de partida de la respuesta, no la respuesta**: el delegado de protección de datos decide cada solicitud,
porque las condiciones de cada artículo dependen del caso. Linneo lee el registro; localizar, entregar, corregir o
suprimir los datos corresponde a la aplicación.

```java
SubjectRights rights = SubjectRights.of(record, taxonomy, DataSubjects.STUDENT, Jurisdiction.SPAIN);
Map<String, List<InventoryField>> where = rights.storedFields(inventory, taxonomy);   // dónde buscar los datos
for (SubjectRights.ActivityRights activity : rights.activities()) {
    SubjectRights.Entitlement erasure = activity.entitlement(SubjectRights.Right.ERASURE);
    System.out.println(activity.activity().activity().key() + ": " + erasure.availability());
}
```

## Miembros

| Miembro | Devuelve | Descripción |
|---|---|---|
| `static SubjectRights of(ProcessingRecord record, Taxonomy taxonomy, String dataSubject)` | derechos | Los derechos solo bajo el RGPD (`Jurisdiction.EU`). |
| `static SubjectRights of(ProcessingRecord record, Taxonomy taxonomy, String dataSubject, Jurisdiction jurisdiction)` | derechos | Los derechos en una [jurisdicción](checker.md#jurisdiction), sobre cada actividad del registro cuyos interesados incluyen la categoría `dataSubject` (una clave como `student`). La clave se resuelve con `taxonomy`, la misma con que se comprobó el registro: una clave obsoleta equivale a su sustituta. `NullPointerException` si algún argumento es `null`; `TaxonomyException` si la taxonomía no tiene ese sujeto de datos; `IllegalArgumentException` si una actividad del registro no tiene base jurídica (construye el registro con `ProcessingChecker.verify`). |
| `String dataSubject()` | `String` | La clave de la categoría, resuelta: la sustituta si la clave indicada estaba obsoleta. |
| `List<ActivityRights> activities()` | lista inmutable | Las actividades que tratan datos de la categoría, con los derechos sobre cada una, en el orden del registro. |
| `Optional<ActivityRights> activity(String activity)` | `Optional` | Los derechos sobre una actividad, o vacío si la actividad no trata datos de la categoría. |
| `boolean isEmpty()` | `boolean` | `true` si ninguna actividad trata datos de la categoría: no hay más que responder. |
| `List<DataCategory> dataCategories()` | lista inmutable | Las categorías de datos personales que tratan las actividades, en orden de clave. |
| `Set<String> objects()` | conjunto inmutable | Los objetos de un inventario de datos que nombran las actividades, ordenados: dónde se guardan los datos de la persona. |
| `List<Class<?>> entities()` | lista inmutable | Las clases de entidad que nombran las actividades, cada una una vez, en el orden de las actividades. |
| `Map<String, List<InventoryField>> storedFields(DataInventory inventory, Taxonomy taxonomy)` | mapa inmutable | Los campos que contienen datos personales en los `objects()`: dónde buscar los datos de la persona para darle acceso, exportarlos, rectificarlos o suprimirlos. Objetos ordenados y campos en el orden del inventario; un objeto que el inventario no contiene, o sin campos personales, no tiene entrada. `TaxonomyException` si la taxonomía no contiene una clave del inventario. |

Una categoría que ninguna actividad nombra da un resultado vacío, pero una clave que la taxonomía no tiene es un error,
para que una errata no se lea como "no hay datos". `entities()` y `objects()` son lo que declaran las actividades; el
inventario da los campos de los objetos.

## Right

`SubjectRights.Right` — `enum` anidado, `@since 0.1.0`: un derecho del interesado que depende del tratamiento.
`String article()` es el número de artículo del RGPD.

| Valor | `article()` | Derecho |
|---|---|---|
| `ACCESS` | `15` | Acceso a los datos y a la información sobre su tratamiento. |
| `RECTIFICATION` | `16` | Rectificación de datos inexactos. |
| `ERASURE` | `17` | Supresión. |
| `RESTRICTION` | `18` | Limitación del tratamiento. |
| `PORTABILITY` | `20` | Portabilidad de los datos. |
| `OBJECTION` | `21` | Oposición. |
| `AUTOMATED_DECISION` | `22` | No ser objeto de una decisión basada únicamente en un tratamiento automatizado. |

## Availability

`SubjectRights.Availability` — `enum` anidado, `@since 0.1.0`: si un derecho se aplica a una actividad.

| Valor | Significado |
|---|---|
| `YES` | Se aplica. |
| `CONDITIONAL` | Se aplica en los casos que enumera el artículo; la explicación dice cuáles. |
| `NO` | No se aplica a esta actividad, como dice la explicación. |

## Entitlement y ActivityRights

```java
public record Entitlement(Right right, Availability availability, String explanation)
```

Un derecho tal como se aplica a una actividad. `explanation` es un texto en inglés que cita las disposiciones. El
constructor lanza `NullPointerException` si algún componente es `null`.

```java
public record ActivityRights(CheckedActivity activity, List<Entitlement> entitlements)
```

Los derechos que tiene un interesado sobre una actividad: `activity` es la
[`CheckedActivity`](checker.md#checkedactivity) del registro y `entitlements` contiene un `Entitlement` por cada
`Right`, en el orden de los derechos (una lista inmutable). `Entitlement entitlement(Right right)` devuelve el de un
derecho.

## Las reglas

La disponibilidad sigue la base jurídica de la actividad (`governance().legalBasis()`), que toda actividad de un
registro construido con `ProcessingChecker.verify` tiene:

| Derecho | Consentimiento (`CONSENT`) | Contrato (`CONTRACT`) | Obligación legal (`LEGAL_OBLIGATION`) | Intereses vitales (`VITAL_INTERESTS`) | Interés público (`PUBLIC_INTEREST`) | Intereses legítimos (`LEGITIMATE_INTERESTS`) |
|---|---|---|---|---|---|---|
| `ACCESS` (15) | `YES` | `YES` | `YES` | `YES` | `YES` | `YES` |
| `RECTIFICATION` (16) | `YES` | `YES` | `YES` | `YES` | `YES` | `YES` |
| `ERASURE` (17) | `YES` (17.1.b) | `CONDITIONAL` (17.1.a) | `NO` (17.3.b) | `CONDITIONAL` (17.1.a) | `NO` (17.3.b) | `CONDITIONAL` (17.1.c) |
| `RESTRICTION` (18) | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` | `CONDITIONAL` |
| `PORTABILITY` (20) | `YES` | `YES` | `NO` | `NO` | `NO` | `NO` |
| `OBJECTION` (21) | `NO` | `NO` | `NO` | `NO` | `CONDITIONAL` (21.1) | `CONDITIONAL` (21.1) |

La tabla no tiene fila para `AUTOMATED_DECISION`: no depende de la base jurídica (véase más abajo).

- **La supresión** no está disponible mientras una obligación legal o una misión de interés público necesita los datos
  (artículo 17.3.b), y la explicación incluye entonces la conservación que declara la actividad (`not declared` si no
  tiene). Con consentimiento sigue a la retirada del consentimiento (17.1.b), con un contrato o intereses vitales a
  que los datos dejen de ser necesarios (17.1.a), con intereses legítimos a una oposición salvo que haya motivos
  legítimos imperiosos (17.1.c).
- **La portabilidad** se aplica al tratamiento basado en el consentimiento o en un contrato (artículo 20.1), y a los
  datos de categoría especial tratados con consentimiento explícito (artículo 20.1.a; véase más abajo). No se aplica al
  tratamiento necesario para una misión de interés público (artículo 20.3), ni siquiera con consentimiento explícito.
- **La oposición** se aplica al tratamiento basado en una misión de interés público o en intereses legítimos
  (artículo 21.1).
- **Las decisiones automatizadas** (artículo 22) no dependen de la base sino de lo que declara la actividad. Si la
  actividad declara `Factor.AUTOMATED_DECISIONS`, en el alcance o en la gobernanza, el derecho es `CONDITIONAL`: la
  persona no es objeto de una decisión basada únicamente en un tratamiento automatizado con efectos jurídicos o
  similares, salvo que sea necesaria para un contrato, esté autorizada por ley o se base en el consentimiento
  explícito, y entonces puede obtener intervención humana, expresar su punto de vista e impugnar la decisión (artículo
  22.3). La explicación de `ACCESS` añade las decisiones automatizadas, su lógica y sus consecuencias (artículo
  15.1.h). Sin el factor, el derecho es `NO`.
- **La limitación** es `CONDITIONAL` en toda actividad: se aplica mientras se verifica la exactitud de los datos o una
  oposición, cuando el tratamiento es ilícito y la persona prefiere la limitación, o cuando la persona necesita los
  datos para reclamaciones (artículo 18.1).
- **Mercadotecnia directa.** Si una finalidad de la actividad tiene el [rasgo `DIRECT_MARKETING`](../terms.md#trait)
  (los usos estándar `marketing` y `sales` y todo lo que cuelga de ellos, los usos de relaciones con antiguos alumnos,
  captación de fondos e investigación de posibles donantes de la extensión universitaria, o un término tuyo que lo
  declare), sea cual sea la base, `OBJECTION` es `YES` (artículos 21.2 y 21.3: el tratamiento cesa) y
  `ERASURE` es `YES` (artículos 17.1.c y 21.2: la persona puede oponerse en cualquier momento y los datos se suprimen
  entonces). Cuando la base es una obligación legal o una misión de interés público, `ERASURE` es en cambio
  `CONDITIONAL` (artículos 17.1.c y 21.3): los datos se suprimen salvo lo que conserva el artículo 17.3.b, y la
  explicación incluye la conservación. `ERASURE` es también `CONDITIONAL` cuando la mercadotecnia directa es una
  finalidad junto a otras: los datos se suprimen salvo lo que aún necesitan las demás finalidades de la actividad.
- **Investigación.** Si una finalidad de la actividad tiene el rasgo `RESEARCH` (el uso `research.participation` de la
  extensión universitaria, o un término tuyo que lo declare), `OBJECTION` es `CONDITIONAL` (artículo 21.6: por motivos
  relacionados con la situación particular de la persona) o `NO` cuando la base es una misión de interés público, y un
  `ERASURE` que no es `NO` pasa a ser `CONDITIONAL`, con una explicación que señala que puede denegarse cuando haría
  imposible o perjudicaría gravemente la investigación (artículo 17.3.d). La mercadotecnia directa va primero para
  `OBJECTION`.
- **Consentimiento explícito.** Si la actividad declara el consentimiento explícito como condición para datos de
  categoría especial (artículo 9.2.a, en [`specialCategoryConditions`](activity.md#governance)): cuando su base
  jurídica no es el consentimiento, un contrato ni una misión de interés público, `PORTABILITY` es `CONDITIONAL` en
  lugar de `NO` (artículo 20.1.a: abarca los datos de categoría especial, no el resto), mientras que con una misión de
  interés público sigue siendo `NO` (artículo 20.3); y cuando su base no es el consentimiento, `ERASURE` es
  `CONDITIONAL` salvo que ya sea `YES`, y la explicación añade que los datos de categoría especial se suprimen cuando se
  retira el consentimiento (artículos 17.1.b y 9.2.a), salvo que lo cubra otra condición del artículo 9.2 declarada para
  la actividad, cuando la actividad declara otras.
- **El acceso** es `YES` en toda actividad y su explicación enumera por su nombre las finalidades de la actividad. Las
  explicaciones de los demás derechos citan los artículos anteriores.
- **`Jurisdiction.SPAIN`.** Las explicaciones de `RECTIFICATION` y `ERASURE` añaden que los datos se bloquean, no se
  borran, mientras puedan surgir responsabilidades del tratamiento, y se borran después (LOPDGDD, artículo 32). Con
  `Jurisdiction.EU` no lo añaden.

`SubjectRights` no comprueba si una solicitud concreta cumple las condiciones de un artículo. Un derecho `CONDITIONAL`
y la explicación de un `NO` son para que el delegado los aplique al caso.

Véase también: [ProcessingRecord](record.md) · [ProcessingChecker](checker.md) · [Bases jurídicas](bases.md) ·
[Inventario de datos](../classify/inventory.md) · curso:
[Actividades de tratamiento y el registro](../../course/09-processing-activities.md)
