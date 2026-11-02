---
title: "Categorización del ENS"
parent: "Referencia"
nav_order: 12.5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/ens/EnsCategorization.java
  - src/main/java/io/github/devbpm/linneo/ens/package-info.java
-->

# Categorización del ENS

`io.github.devbpm.linneo.ens.EnsCategorization` — `public final class`, `@since 0.1.0`

La categoría de seguridad de un sistema según el **Esquema Nacional de Seguridad** (ENS, Real Decreto 311/2022, anexo
I), con la valoración de su información tomada de la clasificación de los datos que maneja. Es opcional y nacional: se
aplica al sector público español y a quienes le prestan servicios. Las instancias son inmutables; construye una con
[`EnsCategorization.builder()`](#builder).

> **Revisa la correspondencia.** La correspondencia entre los niveles de Linneo y los del ENS es propia de Linneo.
> Revisa la valoración con la guía CCN-STIC 803 antes de fiarte de ella.

## El ENS en breve

El ENS valora la información y los servicios de un sistema en cinco **dimensiones**, cada una en un **nivel** `BAJO`,
`MEDIO` o `ALTO`, y la **categoría** del sistema es el nivel más alto de cualquier dimensión: `BÁSICA` si ninguna supera
el nivel bajo, `MEDIA` si una es media y ninguna alta, `ALTA` si una es alta.

| `Dimension` | `code()` | Quién la valora |
|---|---|---|
| `CONFIDENTIALITY` | `C` | Linneo, a partir de la clasificación; el responsable del sistema puede elevarla |
| `INTEGRITY` | `I` | Linneo, a partir de la clasificación; el responsable del sistema puede elevarla |
| `TRACEABILITY` | `T` | El responsable del sistema |
| `AUTHENTICITY` | `A` | El responsable del sistema |
| `AVAILABILITY` | `D` | El responsable del sistema |

| `Level` | `spanishName()` | Significado |
|---|---|---|
| `LOW` | `BAJO` | Perjuicio limitado. |
| `MEDIUM` | `MEDIO` | Perjuicio grave. |
| `HIGH` | `ALTO` | Perjuicio muy grave. |

| `SystemCategory` | `spanishName()` | Cuándo |
|---|---|---|
| `BASIC` | `BÁSICA` | Ninguna dimensión supera el nivel bajo. |
| `MEDIUM` | `MEDIA` | Una dimensión es media, ninguna es alta. |
| `HIGH` | `ALTA` | Una dimensión es alta. |

`EnsCategorization.SystemCategory` es el enum anidado de la categoría del sistema; no se llama `Category` para que no
choque con la anotación `@Category`.

## Qué da la clasificación

Linneo conoce dos dimensiones de la información: la confidencialidad y la integridad de cada campo. El ENS define sus
niveles con los mismos tres grados de perjuicio (limitado, grave y muy grave), así que los niveles de Linneo son los
niveles del ENS:

| Linneo | Nivel del ENS |
|---|---|
| `Confidentiality.PUBLIC` | ningún requisito: no fija valoración de confidencialidad |
| `Confidentiality.LOW`, `Integrity.LOW` | `LOW` (`BAJO`) |
| `Confidentiality.MEDIUM`, `Integrity.MEDIUM` | `MEDIUM` (`MEDIO`) |
| `Confidentiality.HIGH`, `Integrity.HIGH` | `HIGH` (`ALTO`) |

Los niveles son los del campo: los de la categoría, con la elevación que declare el campo. La valoración de cada
dimensión es la más alta entre los campos, y su motivo nombra el primer campo que la fija (`field <path> (<key>)`, con
la ruta como `<clase>.<campo>` en una clase clasificada y `<objeto>.<ruta>` en una guardada). Un sistema que maneja
datos de categorías especiales, que son `HIGH` en ambas, es de categoría `ALTA`.

Solo una clasificación **sin errores** da información: un campo con un error no tiene nivel, y omitirlo infravaloraría
la categoría, así que `information(ClassClassification)` lanza `IllegalArgumentException` ante una.

El responsable del sistema declara las otras tres dimensiones, y también puede elevar la confidencialidad o la
integridad, **nunca bajarlas**: el código fija el suelo. Declarar un nivel inferior al que necesitan los campos deja la
valoración como la fijan los campos, y una dimensión declarada dos veces conserva el nivel más alto. El motivo de una
declaración es `declared: <tu motivo>`.

## Completa o cota inferior

Mientras no se haya dado información, o no se declare la autenticidad, la trazabilidad o la disponibilidad, la categoría
es solo una **cota inferior**: el sistema podría estar en una superior. `isComplete()` es `true` cuando se dio
información **y** las tres dimensiones están valoradas, y `missing()` lista las que no. Sin información, `summary()`
termina con `; no information given (lower bound)`. Un inventario sin objetos tampoco da información, así que también
deja la categoría como cota inferior. Cuando ninguna dimensión está valorada, el resumen lo dice:
`Category BASIC (BÁSICA): no dimension valued; no information given (lower bound)`.

## Miembros

| Miembro | Devuelve | Descripción |
|---|---|---|
| `static Builder builder()` | [`Builder`](#builder) | Inicia una categorización sin información y sin nada declarado. |
| `Optional<Valuation> valuation(Dimension dimension)` | optional | La valoración de una dimensión, o vacío si la información no fija ningún requisito y no se declaró nada. |
| `List<Valuation> valuations()` | lista inmutable | Todas las valoraciones, en el orden de las dimensiones. |
| `SystemCategory category()` | categoría | El nivel más alto de cualquier dimensión; `BASIC` cuando ninguna dimensión tiene nivel. |
| `boolean isComplete()` | `boolean` | Si se dio información y la autenticidad, la trazabilidad y la disponibilidad están valoradas, de modo que la categoría es definitiva y no una cota inferior. |
| `Set<Dimension> missing()` | conjunto | Las dimensiones de autenticidad, trazabilidad y disponibilidad sin valoración, en el orden de las dimensiones. La confidencialidad y la integridad nunca se listan. |
| `String summary()` | `String` | Un resumen de una línea en inglés con los nombres del ENS, como `Category MEDIUM (MEDIA): C MEDIUM, I HIGH; not declared: T, A, D (lower bound)`. |

`toString()` es `EnsCategorization[<resumen>]`.

### Valuation

`EnsCategorization.Valuation` — `record` anidado, `@since 0.1.0`

```java
public record Valuation(Dimension dimension, Level level, String reason)
```

El nivel de una dimensión y por qué: el campo que lo fija, o lo que declaró el responsable del sistema, en inglés. El
constructor lanza `NullPointerException` si un componente es `null`.

## Builder

`EnsCategorization.Builder` — `public static final class`, `@since 0.1.0`. Lo crea `EnsCategorization.builder()`; no
tiene constructor público.

| Miembro | Efecto |
|---|---|
| `Builder information(ClassClassification classification)` | **Añade** los campos de una clase de la información que maneja el sistema; el motivo nombra `<clase>.<ruta>`. `IllegalArgumentException` si la clasificación tiene errores (llámalo una vez por cada clase). |
| `Builder information(DataInventory inventory, Taxonomy taxonomy)` | **Añade** los campos guardados de **todos los objetos** de un [inventario de datos](classify/inventory.md), resueltos con la taxonomía; los campos exentos se omiten, y el motivo nombra `<object>.<path>`. `TaxonomyException` si la taxonomía no contiene una clave. |
| `Builder information(DataInventory inventory, String object, Taxonomy taxonomy)` | Lo mismo para un objeto del inventario. `IllegalArgumentException` si el inventario no tiene ese objeto, porque una clave mal escrita dejaría fuera su información y subestimaría la categoría; `TaxonomyException` si la taxonomía no contiene una clave. |
| `Builder declare(Dimension dimension, Level level, String reason)` | Declara el nivel de una dimensión y por qué. En confidencialidad e integridad solo puede elevar lo que fija la información. `NullPointerException` si un argumento es `null`. |
| `EnsCategorization build()` | Crea la categorización. |

## Ejemplo

```java
import io.github.devbpm.linneo.ens.EnsCategorization;
import io.github.devbpm.linneo.ens.EnsCategorization.Dimension;
import io.github.devbpm.linneo.ens.EnsCategorization.Level;

ClassClassification student = classifier.classify(Student.class);

EnsCategorization ens = EnsCategorization.builder()
        .information(student)
        .build();
ens.summary();      // Category HIGH (ALTA): C HIGH, I HIGH; not declared: T, A, D (lower bound)
ens.isComplete();   // false
ens.missing();      // [TRACEABILITY, AUTHENTICITY, AVAILABILITY]
```

Para un `Student` que maneja datos de categorías especiales, la información sola sitúa el sistema en la categoría
`ALTA`, pero las otras tres dimensiones aún no están declaradas. El responsable del sistema las completa:

```java
import io.github.devbpm.linneo.ens.EnsCategorization;
import io.github.devbpm.linneo.ens.EnsCategorization.Dimension;
import io.github.devbpm.linneo.ens.EnsCategorization.Level;

ClassClassification student = classifier.classify(Student.class);

EnsCategorization complete = EnsCategorization.builder()
        .information(student)
        .declare(Dimension.AVAILABILITY, Level.MEDIUM, "enrollment closes on fixed dates")
        .declare(Dimension.AUTHENTICITY, Level.MEDIUM, "students sign in")
        .declare(Dimension.TRACEABILITY, Level.MEDIUM, "changes to records are audited")
        .build();
complete.summary();    // Category HIGH (ALTA): C HIGH, I HIGH, T MEDIUM, A MEDIUM, D MEDIUM
complete.isComplete(); // true
```

Un sistema cuya información está guardada, como un back office que tiene el inventario de datos y no las clases del
modelo, da sus campos desde el inventario:

```java
DataInventory inventory = classifier.inventory(List.of(Student.class), type -> "students");

EnsCategorization stored = EnsCategorization.builder()
        .information(inventory, taxonomy)                        // todos los objetos del inventario
        .build();
EnsCategorization students = EnsCategorization.builder()
        .information(inventory, "students", taxonomy)            // un objeto; una clave desconocida es un error
        .build();
```

Un sistema cuyos campos son todos datos `PUBLIC` no tiene valoración de confidencialidad, así que su categoría sale de
su integridad y de lo que declare el responsable.

Véase también: [Classifier](classify/classifier.md) · [Inventario de datos](classify/inventory.md) ·
[Niveles de protección](protection-levels.md)
