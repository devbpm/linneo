---
title: "@DerivedFrom"
parent: "Anotaciones"
grand_parent: "Referencia"
nav_order: 5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/DerivedFrom.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @DerivedFrom

`io.github.devbpm.linneo.annotation.DerivedFrom` —
`@Documented @Retention(RUNTIME) @Target({FIELD, RECORD_COMPONENT})`, `@since 0.1.0`

Declara un campo cuyo valor se calcula a partir de otros campos. Un valor derivado de datos es esos datos, o los
revela: la edad calculada a partir de una fecha de nacimiento es un dato personal, y también lo es el nombre completo
construido con un nombre y unos apellidos. Usa esta anotación para esos valores, nunca [`@Exempt`](exempt.md).

## Elementos

| Elemento | Tipo | Por defecto | Significado |
|---|---|---|---|
| `value` | `String[]` | obligatorio | Las rutas de los campos a partir de los que se calcula el valor. Al menos una, o el campo tiene un error `UNKNOWN_SOURCE`. |
| `type` | `Class<?>` | `void.class` | La clase que declara las fuentes, cuando no es la clase que se clasifica: una vista, un objeto de transferencia de datos o una fila de informe construidos a partir de una entidad. `void.class` significa la clase que se clasifica. |

## Fuentes

Una fuente es una ruta de campo:

- el nombre de un campo de la clase, **campos heredados incluidos**, en cualquier orden de declaración (`"birthDate"`);
- la ruta de un campo de un [`@Composite`](composite.md) que contiene la clase (`"home.city"`);
- la ruta del propio campo compuesto (`"home"`), que representa **todos** los campos que cuelgan de él.

Un campo derivado puede ser la fuente de otro, siempre que ninguna derivación dependa de sí misma. Una fuente debe ser
un campo *clasificado*: una ruta que no existe, un campo [`@Exempt`](exempt.md) y un campo con un error son todos
`UNKNOWN_SOURCE`.

Con `type`, las rutas nombran campos de esa clase, clasificados como los clasifica esa clase. Indicar como `type` la
clase que se clasifica equivale a omitirlo. Así una vista se mantiene coherente con la entidad que muestra: sin
`@Category` sigue la clasificación de la entidad, y con `@Category` debe estar a la altura si más adelante se da al
campo de la entidad una categoría más estricta.

Las fuentes se resuelven campo a campo, así que dos clases pueden derivar de los *otros* campos de la otra:
`Order.customerName` desde `Customer.name` y `Customer.lastOrderTotal` desde `Order.total` no es un ciclo, y un
[`@Composite`](composite.md) puede derivar de una clase que lo contiene. Solo son un ciclo los campos derivados que
dependen unos de otros, dentro de una clase, entre clases o a través de las partes de un compuesto. El resultado no
depende del orden en que se clasifiquen las clases.

## Qué obtiene el campo

| | Sin `@Category` | Con `@Category` |
|---|---|---|
| Clasificación | la de su fuente: categoría, niveles e identificabilidad | la declarada, con las comprobaciones habituales de [`@Category`](category.md) |
| Varias fuentes | todas deben coincidir en categoría, confidencialidad, integridad e identificabilidad, o el campo debe declarar su categoría | la categoría declarada debe proteger al menos tanto como **cada** fuente |
| Proteger menos que una fuente | no es posible | un error, salvo que se declare el valor anonimizado o agregado |

«Al menos tanto» es [`Protection.covers`](../protection-levels.md#protection): se cubre cada naturaleza legal de la
fuente, los niveles no son inferiores y un secreto sigue siendo secreto. La única forma explícita de proteger menos es
declarar el valor anonimizado o agregado: una categoría **personal** con una `identifiability` de `ANONYMIZED` o
`AGGREGATED`, como un promedio sobre muchas personas, algo que una revisión puede ver. Declarar una categoría no
personal para un valor derivado de datos personales es un error.

## Reglas que aplica el Classifier

| Regla | Violación | Gravedad | ¿Se clasifica el campo? |
|---|---|---|---|
| Al menos una fuente | `UNKNOWN_SOURCE` (`@DerivedFrom needs at least one source`) | error | no |
| Cada fuente es un campo clasificado | `UNKNOWN_SOURCE` (`the source '<s>' is not a classified field of <Class>` / `the source '<s>' has an error` / `the source '<Class>.<s>' has an error` para un campo de otra clase) | error | no |
| Sin `@Category`, las varias fuentes coinciden | `AMBIGUOUS_DERIVATION` (`its sources [<s>, ...] have different classifications; declare the category of the derived value with @Category`) | error | no |
| Con `@Category`, protege al menos tanto como cada fuente, salvo que sea anonimizado o agregado | `WEAKER_THAN_SOURCE` (`'<key>' protects less than its source(s) [<s>, ...]; a value derived from data needs at least its protection, unless it is declared anonymized or aggregated`) | error | no |
| Ningún campo derivado depende de sí mismo, dentro de una clase o entre clases | `CYCLE` (`is derived from itself through other derived fields`) | error | no |
| Las comprobaciones habituales de `@Category`, cuando está presente | como en [`@Category`](category.md) | | |
| No combinarla con `@Exempt` ni con `@CategoryByRow` | `CONFLICTING_ANNOTATIONS` | error | no |

Se informa de cada campo derivado que forma parte de un ciclo. Un campo que deriva de un campo con error, incluso uno
en un ciclo, sin estar él mismo en el ciclo, es un `UNKNOWN_SOURCE`. Una fuente de otra clase (`type`) que tiene un
error en su propia clase es desconocida aquí.

## FieldClassification resultante

| Componente | Valor |
|---|---|
| `category` | la categoría de la fuente, o la declarada |
| `basis` | `DERIVED` |
| `reason` | `null` |
| `confidentiality` / `integrity` | los niveles de la fuente, o los declarados |
| `identifiability` | la de la fuente, o la declarada |
| `derivedFrom` | las fuentes: rutas de campos de la misma clase (una ruta de compuesto se expande a sus campos) o `ClassName#path` para un campo de otra clase, con el nombre binario de la clase |

## Ejemplo

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.DerivedFrom;
import io.github.devbpm.linneo.Identifiability;
import java.time.LocalDate;
import static io.github.devbpm.linneo.Categories.*;

public class Student {

    @Category(USER_NAME_FIRST)
    private String firstName;

    @Category(USER_NAME_LAST)
    private String lastName;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    @Category(USER_DEMOGRAPHIC_DATE_OF_BIRTH)
    private LocalDate birthDate;

    @DerivedFrom("birthDate")                 // DERIVED: user.demographic.date_of_birth, IDENTIFIED
    private int age;

    @Category(USER_NAME)                      // DERIVED: declarada, protege al menos tanto como las dos fuentes
    @DerivedFrom({"firstName", "lastName"})
    private String fullName;

    @Category(value = USER_DEMOGRAPHIC_DATE_OF_BIRTH, identifiability = Identifiability.AGGREGATED)
    @DerivedFrom("birthDate")                 // DERIVED: un resumen de muchas personas, no es dato personal
    private double averageAge;
}

public record StudentView(                    // una vista de Student: sus campos se clasifican como los de Student
        @DerivedFrom(type = Student.class, value = "email") String email,
        @DerivedFrom(type = Student.class, value = "firstName") String firstName) {
}
```

Para `StudentView.email`, `derivedFrom()` es `["<paquete>.Student#email"]`.

Declaraciones erróneas:

```java
@Category(USER_NAME) @DerivedFrom("condition")   // WEAKER_THAN_SOURCE: condition es un dato de salud
private String summary;

@DerivedFrom({"firstName", "email"})             // AMBIGUOUS_DERIVATION: categorías distintas y sin @Category
private String contact;

@DerivedFrom("missing")                          // UNKNOWN_SOURCE
private String unknown;
```

Véase también: [Visión general de las anotaciones](README.md) · [`@Category`](category.md) ·
[`@Composite`](composite.md) · [`@Exempt`](exempt.md) · [Resultados](../classify/results.md) ·
[Violaciones](../classify/violations.md) · [Niveles de protección](../protection-levels.md) ·
curso: [Anotar modelos](../../course/03-annotating-models.md)
