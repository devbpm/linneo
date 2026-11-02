---
title: "ENS categorization"
parent: "Reference"
nav_order: 12.5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/ens/EnsCategorization.java
  - src/main/java/io/github/devbpm/linneo/ens/package-info.java
-->

# ENS categorization

`io.github.devbpm.linneo.ens.EnsCategorization` — `public final class`, `@since 0.1.0`

The security category of a system under the Spanish **Esquema Nacional de Seguridad** (ENS, Real Decreto 311/2022,
annex I), with the valuation of its information taken from the classification of the data it handles. It is optional
and national: it applies to the Spanish public sector and to those who provide it with services. Instances are
immutable; build one with [`EnsCategorization.builder()`](#builder).

> **Review the correspondence.** The match between Linneo's levels and those of the ENS is Linneo's own. Review the
> valuation against guide CCN-STIC 803 before relying on it.

## The ENS in short

The ENS values the information and the services of a system in five **dimensions**, each at a **level** of `BAJO`
(low), `MEDIO` (medium) or `ALTO` (high), and the **category** of the system is the highest level of any dimension:
`BÁSICA` if none is above low, `MEDIA` if one is medium and none high, `ALTA` if one is high.

| `Dimension` | `code()` | Who values it |
|---|---|---|
| `CONFIDENTIALITY` | `C` | Linneo, from the classification; the system owner may raise it |
| `INTEGRITY` | `I` | Linneo, from the classification; the system owner may raise it |
| `TRACEABILITY` | `T` | The system owner |
| `AUTHENTICITY` | `A` | The system owner |
| `AVAILABILITY` | `D` | The system owner |

| `Level` | `spanishName()` | Meaning |
|---|---|---|
| `LOW` | `BAJO` | Limited harm. |
| `MEDIUM` | `MEDIO` | Serious harm. |
| `HIGH` | `ALTO` | Very serious harm. |

| `SystemCategory` | `spanishName()` | When |
|---|---|---|
| `BASIC` | `BÁSICA` | No dimension is above low. |
| `MEDIUM` | `MEDIA` | A dimension is medium, none is high. |
| `HIGH` | `ALTA` | A dimension is high. |

`EnsCategorization.SystemCategory` is the nested enum of the category of the system; it is not called `Category` so that
it does not clash with the `@Category` annotation.

## What the classification gives

Linneo knows two dimensions for the information: the confidentiality and the integrity of every field. The ENS defines
its levels with the same three degrees of harm (limited, serious and severe), so Linneo's levels are the ENS levels:

| Linneo | ENS level |
|---|---|
| `Confidentiality.PUBLIC` | no requirement: it sets no valuation of confidentiality |
| `Confidentiality.LOW`, `Integrity.LOW` | `LOW` (`BAJO`) |
| `Confidentiality.MEDIUM`, `Integrity.MEDIUM` | `MEDIUM` (`MEDIO`) |
| `Confidentiality.HIGH`, `Integrity.HIGH` | `HIGH` (`ALTO`) |

The levels are those of the field: the category's, with any raise declared on the field. The valuation of each
dimension is the highest among the fields, and its reason names the first field that sets it (`field <path> (<key>)`,
with the path as `<class>.<field>` for a classified class and `<object>.<path>` for a stored one). A system that handles
special category data, which is `HIGH` in both, is category `ALTA`.

Only a classification **without errors** gives information: a field with an error has no level, and leaving it out
would understate the category, so `information(ClassClassification)` throws `IllegalArgumentException` for one.

The system owner declares the other three dimensions, and may also raise confidentiality or integrity, **never lower
them**: the code sets the floor. Declaring a level that is lower than what the fields need leaves the valuation as the
fields set it, and a dimension declared twice keeps the higher level. The reason of a declaration is `declared: <your
reason>`.

## Complete or lower bound

While no information was given, or authenticity, traceability or availability is not declared, the category is only a
**lower bound**: the system could be in a higher one. `isComplete()` is `true` when information was given **and** the
three dimensions are valued, and `missing()` lists those that are not. Without information, `summary()` ends with `; no
information given (lower bound)`. An inventory without objects gives no information, so it leaves the category a lower
bound too. When no dimension is valued, the summary says so:
`Category BASIC (BÁSICA): no dimension valued; no information given (lower bound)`.

## Members

| Member | Returns | Description |
|---|---|---|
| `static Builder builder()` | [`Builder`](#builder) | Starts a categorization with no information and nothing declared. |
| `Optional<Valuation> valuation(Dimension dimension)` | optional | The valuation of a dimension, or empty if the information sets no requirement and nothing was declared. |
| `List<Valuation> valuations()` | immutable list | Every valuation, in the order of the dimensions. |
| `SystemCategory category()` | category | The highest level of any dimension; `BASIC` when no dimension has a level. |
| `boolean isComplete()` | `boolean` | Whether information was given and authenticity, traceability and availability are valued, so that the category is final rather than a lower bound. |
| `Set<Dimension> missing()` | set | The dimensions of authenticity, traceability and availability without a valuation, in the order of the dimensions. Confidentiality and integrity are never listed. |
| `String summary()` | `String` | A one-line summary in English with the ENS names, such as `Category MEDIUM (MEDIA): C MEDIUM, I HIGH; not declared: T, A, D (lower bound)`. |

`toString()` is `EnsCategorization[<summary>]`.

### Valuation

`EnsCategorization.Valuation` — nested `record`, `@since 0.1.0`

```java
public record Valuation(Dimension dimension, Level level, String reason)
```

The level of a dimension and why: the field that sets it, or what the system owner declared, in English. The
constructor throws `NullPointerException` if a component is `null`.

## Builder

`EnsCategorization.Builder` — `public static final class`, `@since 0.1.0`. Created by `EnsCategorization.builder()`; it
has no public constructor.

| Member | Effect |
|---|---|
| `Builder information(ClassClassification classification)` | **Adds** the fields of a class of the information the system handles; the reason names `<class>.<path>`. `IllegalArgumentException` if the classification has errors (call it once for each class). |
| `Builder information(DataInventory inventory, Taxonomy taxonomy)` | **Adds** the stored fields of **every object** of a [data inventory](classify/inventory.md), resolved with the taxonomy; exempted fields are skipped, and the reason names `<object>.<path>`. `TaxonomyException` if the taxonomy does not contain a key. |
| `Builder information(DataInventory inventory, String object, Taxonomy taxonomy)` | The same for one object of the inventory. `IllegalArgumentException` if the inventory has no such object, because a mistyped key would leave its information out and understate the category; `TaxonomyException` if the taxonomy does not contain a key. |
| `Builder declare(Dimension dimension, Level level, String reason)` | Declares the level of a dimension and why. For confidentiality and integrity it can only raise what the information sets. `NullPointerException` if an argument is `null`. |
| `EnsCategorization build()` | Creates the categorization. |

## Example

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

For a `Student` that holds special category data, the information alone puts the system in category `ALTA`, but the
three other dimensions are not declared yet. The system owner completes them:

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

A system whose information is stored, such as a back office that has the data inventory and not the model classes, gives
its fields from the inventory:

```java
DataInventory inventory = classifier.inventory(List.of(Student.class), type -> "students");

EnsCategorization stored = EnsCategorization.builder()
        .information(inventory, taxonomy)                        // every object of the inventory
        .build();
EnsCategorization students = EnsCategorization.builder()
        .information(inventory, "students", taxonomy)            // one object; an unknown key is an error
        .build();
```

A system whose fields are all `PUBLIC` data has no valuation of confidentiality, so its category comes from its
integrity and from what the owner declares.

See also: [Classifier](classify/classifier.md) · [Data inventory](classify/inventory.md) ·
[Protection levels](protection-levels.md)
