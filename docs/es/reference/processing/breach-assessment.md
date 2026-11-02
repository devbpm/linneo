---
title: "BreachAssessment"
parent: "Tratamientos"
grand_parent: "Referencia"
nav_order: 7
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/BreachAssessment.java
  - src/main/java/io/github/devbpm/linneo/Trait.java
-->

# BreachAssessment

`io.github.devbpm.linneo.processing.BreachAssessment` — `public final class`, `@since 0.1.0`

La gravedad de una violación de seguridad de los datos personales y lo que el RGPD pide al responsable, según la
metodología de ENISA, la Agencia de la Unión Europea para la Ciberseguridad («Recommendations for a methodology of the
assessment of severity of personal data breaches», v1.0, 2013):

```text
gravedad = contexto del tratamiento de datos × facilidad de identificación + circunstancias de la violación
```

Linneo puntúa los dos primeros factores a partir de la clasificación de los campos afectados y de los
[rasgos](../terms.md#trait) de sus categorías, y dice qué campos fijan cada puntuación. Las circunstancias (cómo se
perdieron la confidencialidad, la integridad y la disponibilidad, y si hubo intención maliciosa) las declara quien
gestiona la violación. Las instancias son inmutables; construye una con [`BreachAssessment.builder()`](#builder).

Es una **orientación para las primeras horas, no una decisión**: el responsable evalúa el riesgo de cada violación, y el
delegado de protección de datos decide si hay que notificar. Una gravedad baja se lee como una violación que
probablemente no entraña un riesgo (artículo 33.1), que se documenta (artículo 33.5) pero no se notifica; una alta o muy
alta, como un riesgo alto, que además se comunica a los interesados (artículo 34.1) salvo que los datos fueran
ininteligibles (artículo 34.3.a). Una violación solo de la confidencialidad de datos que eran ininteligibles, como los
cifrados con una clave que no quedó comprometida, tampoco entraña probablemente un riesgo y no se notifica, sea cual
sea su puntuación (Directrices 9/2022 del CEPD, y 01/2021, caso 1).

## Cómo se puntúa

**Contexto del tratamiento de datos** (1 a 4): cuán críticos son los datos afectados. Es el del campo personal más
crítico entre los afectados.

| Contexto | Los datos afectados |
|---|---|
| 4 | datos de categorías especiales o datos penales |
| 3 | datos financieros: una categoría con el rasgo `FINANCIAL` (`user.financial` y descendientes en la taxonomía estándar) |
| 2 | datos de comportamiento: una categoría con el rasgo `BEHAVIOURAL` (`user.behavior` y `user.location` y descendientes) |
| 1 | otros datos personales |
| 0 | ningún campo afectado contiene datos personales: no es una violación de seguridad de datos personales (RGPD, artículo 4.12) |

Un campo declarado anonimizado o agregado no es un dato personal, así que no cuenta ni para el contexto ni para la
facilidad de identificación.

**Facilidad de identificación**: con qué facilidad identifican los datos afectados a las personas a las que se refieren.
Es el más alto de los niveles de los campos personales afectados, leído de los rasgos de identificador de sus
categorías.

| Nivel | Puntuación | Los campos afectados |
|---|---|---|
| `MAXIMUM` | 1 | incluyen un `DIRECT_IDENTIFIER`: un nombre (`user.name`), un documento de identidad (`user.government_id`), datos biométricos (`user.biometric`) o una imagen (`user.content.self_image`) |
| `SIGNIFICANT` | 0,75 | incluyen un `CONTACT_IDENTIFIER`: datos de contacto (`user.contact`) o de cuenta (`user.account`) |
| `LIMITED` | 0,5 | incluyen un `INDIRECT_IDENTIFIER`: identificadores internos (`user.unique_id`) o de dispositivo (`user.device`); o cualquier valor declarado seudonimizado; un identificador directo o de contacto seudonimizado también es `LIMITED` |
| `NEGLIGIBLE` | 0,25 | no tienen ninguno de ellos |

Ambos pueden ajustarse, como prevé la metodología, cuando el contexto de los datos lo pide: véanse
[`adjustContext`](#builder) e [`identification`](#builder).

Las **circunstancias** (0 a 2) suman lo que declara quien gestiona la violación:

| Se declara con | Valor | Puntos |
|---|---|---|
| `confidentiality(Exposure)` | `NONE`: no se perdió, o se expuso sin indicios de tratamiento ilícito, como un portátil cifrado perdido | 0 |
| | `KNOWN_RECIPIENTS`: revelados a un número de destinatarios conocidos, como un correo enviado a las personas equivocadas | +0,25 |
| | `UNKNOWN_RECIPIENTS`: revelados a un número desconocido de destinatarios, como datos publicados en internet | +0,5 |
| `integrity(Alteration)` | `NONE`: no se alteraron, o se alteraron sin uso incorrecto o ilícito | 0 |
| | `RECOVERABLE`: alterados y posiblemente usados de forma incorrecta, pero recuperables | +0,25 |
| | `UNRECOVERABLE`: alterados y posiblemente usados de forma incorrecta, y el original no se puede recuperar | +0,5 |
| `availability(Unavailability)` | `NONE`: no se perdió, o se recupera sin dificultad de otras copias | 0 |
| | `TEMPORARY`: no disponibles temporalmente, recuperables con algo de trabajo o de la propia persona | +0,25 |
| | `PERMANENT`: perdidos para siempre, sin copia de seguridad ni otra fuente | +0,5 |
| `malicious(true)` | La violación fue intencionada, para dañar a las personas o al responsable | +0,5 |

La **gravedad** es el nivel de la puntuación (`contexto × identificación + circunstancias`):

| `Severity` | Puntuación | Significado |
|---|---|---|
| `LOW` | menos de 2 | Ningún efecto, o unas pocas molestias que las personas superan sin problema. |
| `MEDIUM` | de 2 a menos de 3 | Molestias significativas que las personas superan pese a algunas dificultades. |
| `HIGH` | de 3 a menos de 4 | Consecuencias significativas que las personas superan con serias dificultades. |
| `VERY_HIGH` | 4 o más | Consecuencias significativas o irreversibles que las personas pueden no superar. |

## Lo que pide el RGPD

| Método | `true` cuando | Artículo |
|---|---|---|
| `notifyAuthority()` | hay datos personales afectados, la gravedad no es `LOW` y **no** se perdió solo la confidencialidad de datos ininteligibles (`unintelligible(true)` sin pérdida de integridad ni de disponibilidad declarada) | 33.1: notificar a la autoridad de control en un plazo de 72 horas desde que se tiene constancia de la violación. Una violación `LOW`, y una violación solo de la confidencialidad de datos ininteligibles (Directrices 9/2022 del CEPD y 01/2021, caso 1), solo se documenta (33.5). |
| `communicateToSubjects()` | hay datos personales afectados, la gravedad es `HIGH` o `VERY_HIGH` y los datos **no** se declararon ininteligibles | 34.1: informar a los interesados sin dilación indebida. 34.3.a: no hace falta si los datos eran ininteligibles para cualquiera sin acceso a ellos, por ejemplo cifrados con una clave robusta que no se vio comprometida (`unintelligible(true)`). |
| `manyIndividuals()` | se declaró que se vieron afectadas más de 100 personas | Un indicador de la metodología que pesa en la evaluación sin cambiar la puntuación. |

## Miembros

| Miembro | Devuelve | Descripción |
|---|---|---|
| `static Builder builder()` | [`Builder`](#builder) | Inicia una evaluación sin ningún campo afectado ni ninguna pérdida declarada. |
| `int dataProcessingContext()` | `int` | El contexto, de 1 a 4 tras cualquier ajuste; 0 cuando ningún campo afectado contiene datos personales. |
| `String contextReason()` | `String` | Por qué el contexto tiene su puntuación: el tipo de datos y el campo que lo fija, en inglés, con el ajuste si lo hay. |
| `Identification identification()` | nivel | La facilidad de identificación. Cada `Identification` tiene un `score()`: 0,25, 0,5, 0,75 o 1. |
| `String identificationReason()` | `String` | Los identificadores encontrados o el motivo dado, en inglés. |
| `double circumstances()` | `double` | Los puntos de las circunstancias, de 0 a 2. |
| `double score()` | `double` | `contexto × identificación + circunstancias`. |
| `Severity severity()` | nivel | El nivel de la puntuación. |
| `boolean manyIndividuals()` | `boolean` | Si se ven afectadas más de 100 personas. |
| `boolean notifyAuthority()` | `boolean` | Véase arriba. |
| `boolean communicateToSubjects()` | `boolean` | Véase arriba. |
| `String explanation()` | `String` | La evaluación, sus motivos y qué hacer, en inglés, una afirmación por línea. |

`toString()` es `BreachAssessment[<gravedad>, <puntuación>]`. Los enumerados anidados `Exposure`, `Alteration` y
`Unavailability` también tienen un `score()`, los puntos de la tabla anterior.

## Builder

`BreachAssessment.Builder` — `public static final class`, `@since 0.1.0`. Lo crea `BreachAssessment.builder()`; no tiene
constructor público.

| Miembro | Efecto |
|---|---|
| `Builder affected(Collection<FieldClassification> fields)` | **Añade** las clasificaciones de los campos cuyos datos se vieron afectados. |
| `Builder affected(Collection<InventoryField> fields, Taxonomy taxonomy)` | **Añade** campos guardados de un [inventario de datos](../classify/inventory.md), resueltos con la taxonomía; los campos exentos se omiten. `TaxonomyException` si la taxonomía no contiene una clave. |
| `Builder adjustContext(int delta, String reason)` | Ajusta el contexto, al alza cuando el volumen de datos o la vulnerabilidad de las personas lo hace más crítico, a la baja cuando los datos son públicos de todos modos. `delta` está entre -3 y 3 (`IllegalArgumentException` si no) y el resultado se mantiene entre 1 y 4; no convierte en una violación de datos personales la que no tiene datos personales. El motivo se añade a `contextReason()`. |
| `Builder identification(Identification level, String reason)` | Fija la facilidad de identificación en lugar de leerla de los campos, por ejemplo cuando el nombre de un fichero lleva el nombre de la persona. |
| `Builder confidentiality(Exposure exposure)` | Cómo se perdió la confidencialidad; por defecto `NONE`. |
| `Builder integrity(Alteration alteration)` | Cómo se perdió la integridad; por defecto `NONE`. |
| `Builder availability(Unavailability unavailability)` | Cómo se perdió la disponibilidad; por defecto `NONE`. |
| `Builder malicious(boolean malicious)` | Si la violación fue intencionada; por defecto `false`. |
| `Builder individuals(long count)` | Cuántas personas se ven afectadas. |
| `Builder unintelligible(boolean unintelligible)` | Si los datos eran ininteligibles para cualquiera sin acceso a ellos. |
| `BreachAssessment build()` | Crea la evaluación. |

Los métodos de las circunstancias sustituyen el valor anterior; un argumento `null` lanza `NullPointerException`.

## Ejemplo

```java
ClassClassification student = classifier.classify(Student.class);

BreachAssessment assessment = BreachAssessment.builder()
        .affected(student.fields())
        .confidentiality(BreachAssessment.Exposure.KNOWN_RECIPIENTS)
        .individuals(340)
        .build();

assessment.severity();               // VERY_HIGH
assessment.notifyAuthority();        // true: en 72 horas (RGPD, artículo 33.1)
assessment.communicateToSubjects();  // true: RGPD, artículo 34.1
System.out.println(assessment.explanation());
```

Para un `Student` cuyos campos incluyen un nombre, un correo y un grado de discapacidad, la explicación es:

```text
Severity VERY_HIGH (4.25 = 4 x 1 + 0.25), following ENISA's methodology (2013).
Data processing context 4: sensitive data user.health_and_medical.disability.
Ease of identification maximum (1): identifier user.name.first.
Circumstances +0.25: confidentiality KNOWN_RECIPIENTS, integrity NONE, availability NONE.
More than 100 people are affected (340).
Notify the supervisory authority within 72 hours of becoming aware of the breach (GDPR art. 33.1).
Tell the data subjects without undue delay (GDPR art. 34.1).
This is an orientation: the controller assesses the risk of each breach.
```

La puntuación es `4 × 1 + 0,25`: datos sensibles que nombran a las personas, enviados a destinatarios conocidos. Si esos
mismos datos se hubieran declarado `unintelligible(true)`, solo se habría perdido su confidencialidad, así que tampoco
se notificaría a la autoridad: la explicación diría que solo se perdió la confidencialidad de datos ininteligibles
(Directrices 9/2022 del CEPD) y que la violación se documenta, no se notifica. Con una pérdida de integridad o de
disponibilidad declarada, la autoridad se notificaría y la penúltima línea diría que no hace falta informar a los
interesados (artículo 34.3.a). Una violación que solo afecta a calificaciones e identificadores internos, sin que se
haya revelado nada, puntúa 0,5 (`LOW`): documéntala, no la notifiques.

Véase también: [ImpactScreening](impact-screening.md) · [ProcessingChecker](checker.md) ·
[Classifier](../classify/classifier.md) · [Actividades de tratamiento](README.md)
