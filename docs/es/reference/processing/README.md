---
title: "Tratamientos"
parent: "Referencia"
nav_order: 12
permalink: "/es/reference/processing/"
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/package-info.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingChecker.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingViolation.java
  - src/main/java/io/github/devbpm/linneo/processing/Jurisdiction.java
-->

# Actividades de tratamiento

Paquete `io.github.devbpm.linneo.processing` — exportado, `@since 0.1.0`

Las anotaciones y el [`Classifier`](../classify/classifier.md) dicen qué contiene cada **campo**. El registro de
actividades de tratamiento del RGPD (artículo 30) trata de **procesos**: para qué se tratan los datos, de quién son,
con qué base jurídica, durante cuánto tiempo y quién los recibe. Este paquete describe esos procesos, los comprueba
frente a la taxonomía y la clasificación del modelo, y genera el registro.

| Tipo | Declaración | Papel | Página |
|---|---|---|---|
| `ProcessingActivity` | record, con un `Builder` | Una actividad de tratamiento tal como se declara: finalidades, interesados, datos personales, bases, conservación, destinatarios, transferencias | [ProcessingActivity](activity.md) |
| `Transfer` | record | Una transferencia a un tercer país o a una organización internacional | [ProcessingActivity](activity.md#transfer) |
| `LegalBasis` | enum | La base jurídica (artículo 6.1) | [Bases jurídicas](bases.md#legalbasis) |
| `SpecialCategoryCondition` | enum | La excepción para categorías especiales de datos (artículo 9.2) | [Bases jurídicas](bases.md#specialcategorycondition) |
| `CriminalDataBasis` | enum | Lo que permite tratar datos penales (artículo 10) | [Bases jurídicas](bases.md#criminaldatabasis) |
| `TransferSafeguard` | enum | Lo que permite una transferencia (capítulo V) | [Bases jurídicas](bases.md#transfersafeguard) |
| `ProcessingChecker` | clase | Comprueba las actividades y construye el registro | [ProcessingChecker](checker.md) |
| `CheckedActivity` | record | Una actividad con sus términos resueltos, sus datos personales, la protección que requieren y los problemas encontrados | [ProcessingChecker](checker.md#checkedactivity) |
| `Jurisdiction` | enum | El derecho nacional que un comprobador aplica además del RGPD (`EU`, `SPAIN`) | [ProcessingChecker](checker.md#jurisdiction) |
| `ProcessingViolation` | record, con `Severity` y `Code` | Un problema encontrado en una actividad | [ProcessingChecker](checker.md#processingviolation) |
| `ProcessingException` | excepción | La lanza `verify` con todos los errores | [ProcessingChecker](checker.md#processingexception) |
| `ProcessingRecord` | record | El registro de actividades de tratamiento, en Markdown o JSON | [ProcessingRecord](record.md) |

## Relación con la taxonomía

Una `Taxonomy` tiene tres dimensiones. Las anotaciones solo usan la primera; una actividad de tratamiento usa las
tres:

| Dimensión de la taxonomía | En una `ProcessingActivity` | Contenido del registro |
|---|---|---|
| **Usos de datos** (`DataUses`) | `purposes`: para qué se tratan los datos | fines del tratamiento (art. 30.1.b) |
| **Sujetos de datos** (`DataSubjects`) | `dataSubjects`: de quién son los datos | categorías de interesados (art. 30.1.c) |
| **Categorías de datos** (`Categories`) | `dataCategories` (claves) y `entities` (clases del modelo clasificadas) | categorías de datos personales (art. 30.1.c) |

Los datos personales de una actividad pueden indicarse como claves de categoría, como clases del modelo o de ambas
formas. Para una clase, el comprobador ejecuta el [`Classifier`](../classify/classifier.md) y toma las categorías de
sus campos personales, con los niveles declarados en cada campo, de modo que el registro sigue al modelo: si se añade
un campo personal a una entidad, todas las actividades que la incluyen pasan a tratarlo. Una entidad que no esté
clasificada por completo y correctamente es un error de la actividad. La naturaleza jurídica de las categorías
(ordinaria, categoría especial, datos penales; consulta [Niveles de protección](../protection-levels.md)) decide qué
bases necesita la actividad.

Los vocabularios de las bases siguen las declaraciones de privacidad de fideslang: `LegalBasis`,
`SpecialCategoryCondition` y `TransferSafeguard` llevan en `fideslangName()` los valores de
`legal_basis_for_processing`, `special_category_legal_basis` y `legal_basis_for_transfers`.

## Errores y avisos

El [`ProcessingChecker`](checker.md) sigue el mismo enfoque de fallo seguro que el clasificador, con una regla para
la gravedad:

- **Error**: la actividad, tal como está declarada, sería ilícita o no se puede comprobar. Datos personales sin base
  jurídica, categorías especiales sin una condición del artículo 9.2, datos penales sin una base del artículo 10, una
  clave desconocida, una actividad sin finalidad, sin interesados o sin datos personales, una entidad con errores de
  clasificación, dos actividades con la misma clave. Con `Jurisdiction.SPAIN`, también el consentimiento explícito como única
  condición para las categorías especiales del artículo 9.1 de la LOPDGDD. `verify` lanza una `ProcessingException` y no se genera ningún
  registro.
- **Aviso**: el registro está incompleto o algo debe revisarse. Una clave obsoleta, una categoría no personal, una
  base que descansa en una ley sin nombrarla, el interés legítimo sin el interés perseguido, la falta de plazo de
  conservación, una condición declarada para datos que la actividad no trata, y actividades con categorías especiales,
  datos penales o elaboración de perfiles sin evaluación de impacto. La verificación pasa y los avisos aparecen en el
  registro como **puntos abiertos**.

La lista completa está en la [tabla de códigos](checker.md#códigos).

## Lo que Linneo no hace

Linneo comprueba **declaraciones**: que cada actividad nombra una base donde el RGPD la exige, que sus claves existen
y que sus entidades están clasificadas. No decide si el consentimiento es válido, si un interés prevalece sobre los
derechos de los interesados o si una ley ampara realmente el tratamiento, y no ofrece asesoramiento jurídico. El
registro que genera sirve de apoyo a la evaluación del propio responsable del tratamiento y al consejo de su delegado
de protección de datos, y no los sustituye. Las leyes que aparecen en los ejemplos de esta documentación («Higher
Education Act», «Child Protection Act») son ficticias.

## Ejemplo

```java
ProcessingActivity newsletter = ProcessingActivity.builder("alumni-newsletter", "Alumni newsletter")
        .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL)
        .dataSubjects(DataSubjects.ALUMNUS)
        .dataCategories(Categories.USER_NAME, Categories.USER_CONTACT_EMAIL)
        .legitimateInterest("Keeping former students informed about university events")
        .retention("Until the alumnus unsubscribes")
        .build();

ProcessingRecord record = new ProcessingChecker(Taxonomy.load()).verify(newsletter);
String markdown = record.toMarkdown("Example University");
```

Véase también: [ProcessingActivity](activity.md) · [Bases jurídicas](bases.md) · [ProcessingChecker](checker.md) ·
[ProcessingRecord](record.md) · curso:
[Actividades de tratamiento y el registro](../../course/09-processing-activities.md)
