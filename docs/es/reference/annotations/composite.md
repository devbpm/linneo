---
title: "@Composite"
parent: "Anotaciones"
grand_parent: "Referencia"
nav_order: 6
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/Composite.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @Composite

`io.github.devbpm.linneo.annotation.Composite` — `@Documented @Inherited @Retention(RUNTIME) @Target(TYPE)`,
`@since 0.1.0`

Marca una clase cuyas instancias forman parte de otro registro, como una dirección `@Embeddable` de JPA o un objeto de
valor, de modo que los campos que la contienen se clasifican por los campos de la propia clase. Es un marcador sin
elementos. Se **hereda**: una subclase de un compuesto es un compuesto.

Marca clases concretas y records. El clasificador no ve los campos de las implementaciones de una interfaz o de una
clase abstracta, así que un campo de ese tipo no toma el valor por defecto de la clase, y los campos de una subclase de
un compuesto concreto son invisibles a través de un campo del tipo de la superclase.

El marcador es propio de Linneo y no una anotación de un framework, para que la biblioteca no necesite ninguna
dependencia. Olvidarlo es seguro donde importa: un campo que contiene una clase cuyos campos están clasificados, sin que
la clase sea un compuesto, es una [referencia a otro registro](#referencias-a-otros-registros), que no toma un valor por
defecto no personal de la clase cuando ese registro contiene datos personales, así que no pasa la verificación
(`UNCLASSIFIED_FIELD`) hasta que se marque la clase o el campo declare su propia categoría.

## Qué hace el clasificador

Un campo **sin anotación propia** cuyo tipo es una clase compuesta concreta, o un array, una colección o un `Optional`
de ella, a cualquier profundidad, recibe una clasificación por cada campo del compuesto, con la ruta `campo.parte`:

| Campo | Se clasifica como |
|---|---|
| `Address home` | `home.street`, `home.city` |
| `List<Address> previous`, `Address[] previous`, `Optional<Address> previous`, `List<List<Address>> history` | `previous.street`, `previous.city`: se entra en los contenedores anidados a cualquier profundidad |
| un campo que puede contener datos que el clasificador no puede dividir en campos, como `Map<String, Address> byType` (véase más abajo) | `UNCLASSIFIED_FIELD`, salvo que declare su propia categoría |
| un campo con `@Category`, `@CategoryByRow` o `@DerivedFrom` | una sola entrada: el compuesto se guarda como un solo valor (véase más abajo) |
| un campo con `@Exempt` | se omite, y se lista en `exempted()`, salvo que contenga compuestos (`CONFLICTING_ANNOTATIONS`) |

- **Las variables de tipo** se resuelven a través de las superclases de la clase que se clasifica: un `T value` de una
  clase base genérica, clasificado para `Box extends Base<Address>`, es un `Address`.
- **Un valor por defecto de la clase nunca se aplica a un campo que puede contener datos que el clasificador no ve**,
  así que envolver un compuesto nunca le da un valor por defecto no personal. Estos campos son un error
  `UNCLASSIFIED_FIELD` salvo que estén anotados:
  - compuestos en un `Map` o un `Map.Entry`, en el que **nunca se entra** porque sus claves también son datos:
    `Map<String, Address>` declara su categoría con `@Category` o `@CategoryByRow`, y se comprueba entonces contra los
    campos de los compuestos (véase más abajo);
  - un compuesto como argumento de tipo de una clase que no es una colección, como `Wrapper<Address>`, o de un tipo
    propietario (`Outer<Address>.Inner`), y varios compuestos en un mismo tipo, como `Map<Address, Address>`. Un
    compuesto genérico como `Box<Address>` menciona dos compuestos, él mismo y `Address`, así que se declara como un
    solo valor;
  - una interfaz o una clase abstracta marcada `@Composite`, que no se incrusta: sus implementaciones pueden tener otros
    campos;
  - un valor tipado `Object`, `Serializable`, `Comparable` o `Cloneable`, una variable de tipo que la clase no enlaza o
    un contenedor sin parametrizar.

  Un campo que se refiere a otro registro del modelo es un caso aparte; véase
  [Referencias a otros registros](#referencias-a-otros-registros).

- **Las rutas** se anidan: un compuesto dentro de otro da `home.geo.country`. En un campo así,
  `FieldClassification.fieldName()` es el último segmento y `declaringClass()` es la clase compuesta.
- **Valores por defecto.** Los campos del compuesto se resuelven con las anotaciones del propio compuesto y su propio
  [`@DefaultCategory`](default-category.md). El valor por defecto de la clase que lo contiene **no** se les aplica
  (sigue aplicándose a los campos de la propia clase contenedora).
- **Los problemas** dentro del compuesto se informan en la clasificación de la clase que lo contiene, en su ruta
  (`broken.unclassified`), con la clase contenedora como `Violation.type()`. Un problema con una anotación de clase
  del compuesto (un `@DefaultCategory` defectuoso), que no tiene ruta propia, se informa en la ruta del campo que lo
  contiene. Los campos `@Exempt` del compuesto se listan en `exempted()` con la ruta `campo.parte`.
- **Protección.** `ClassClassification.ceilingOf("home")` es la protección que cubre todo lo que contiene el
  compuesto; `ceiling()` hace lo mismo para toda la clase.
- **Fuentes de derivaciones.** Un [`@DerivedFrom`](derived-from.md) puede nombrar una parte (`"home.city"`) o el
  compuesto entero (`"home"`). Un campo del compuesto puede a su vez derivar de una clase que contiene el compuesto:
  solo es un `CYCLE` si campos derivados dependen unos de otros, también a través de las partes del compuesto. Un campo
  del compuesto que está en un ciclo se informa con el compuesto y, en `campo.parte`, con la clase contenedora.
- Las clases compuestas no hace falta pasarlas por separado a `Classifier.verify`: se clasifican como parte de cada
  clase que las contiene. Clasificar una directamente también funciona, como con cualquier clase.
- **Independencia del orden.** Los problemas encontrados no dependen del orden en que se clasifican las clases; solo
  puede depender el campo en el que se informa un ciclo de contención de compuestos. Cada clase informa de sus propios
  problemas, así que verifica todas las clases del modelo (véase
  [`ModelClasses`](../classify/classifier.md#modelclasses)), no solo algunas.

## Un compuesto guardado como un solo valor

Un campo que guarda el compuesto como un solo valor, por ejemplo como JSON en una única columna, o que lo contiene donde
sus campos no pueden ser rutas (en un `Map`, cuyas claves también son datos, o como argumento de tipo de una clase que
no es una colección), declara su categoría con [`@Category`](category.md) o [`@CategoryByRow`](category-by-row.md). El
campo es entonces una sola entrada, y su protección debe cubrir la protección de **todos los campos de los compuestos
que contiene**: si no, el valor quedaría clasificado por debajo de lo que contiene. Lo mismo vale para un campo que
guarda compuestos como un solo valor y toma su categoría de una fuente de [`@DerivedFrom`](derived-from.md). Un
compuesto cuyos propios campos tienen errores no deja nada con lo que comparar, lo que también es un error. La
comprobación abarca también un compuesto que se contiene a sí mismo y compuestos que se contienen entre sí, sea cual
sea el orden en que se clasifican las clases.

| Regla | Violación | Gravedad | ¿Se clasifica el campo? |
|---|---|---|---|
| La categoría declarada protege al menos tanto como los campos de cada compuesto que contiene el campo | `WEAKER_THAN_PARTS` (`'<key>' protects less than the fields of [<Composite>, ...] need (<natures>, <confidentiality>, <integrity>[, secret])`; o, cuando los campos de un compuesto tienen errores, `the fields of [<Composite>, ...] have errors, so what '<key>' must protect is unknown`) | error | **sí** (el error se informa igualmente) |
| El compuesto no se contiene a sí mismo, directamente ni a través de otros compuestos | `CYCLE` (`the composite <Composite> contains itself; classify the field as one value with @Category or @CategoryByRow`) | error | no (el campo que cierra el bucle) |

Un compuesto que se contiene a sí mismo (`Node` con un `List<Node> children`) no se puede expandir: el error se
informa en el campo que cierra el bucle (`tree.children`), y los demás campos del compuesto se clasifican con
normalidad. Clasifica ese campo como un solo valor para eliminar el error.

## Referencias a otros registros

Un campo sin anotación propia cuyo tipo es otro registro del modelo, una clase que tiene un
[`@DefaultCategory`](default-category.md), un campo anotado o un campo de un tipo compuesto, **se refiere** a ese
registro. Un tipo sellado se lee a través de sus subclases permitidas, y también se leen los argumentos de tipo
(`List<Country>`).

- Si el registro **no contiene datos personales**, como un elemento de un catálogo tipo `Country`, el campo toma el
  valor por defecto de la clase como cualquier otro.
- Una referencia a un registro que **contiene datos personales** es en sí misma un dato personal, una clave seudónima,
  así que se rechaza el valor por defecto de la clase: el campo es un error `UNCLASSIFIED_FIELD` que aconseja declarar
  lo que guarda, normalmente la clave del registro, con `@DerivedFrom(type = Customer.class, value = "id")`.
- [`@Exempt`](exempt.md) en un campo que contiene compuestos, o que se refiere a un registro con datos personales, es un
  error `CONFLICTING_ANNOTATIONS`: una relación no es un campo que no contiene datos.

```java
@DefaultCategory(value = REFERENCE_PUBLIC, reason = "lista de códigos de países")
public class Country {
    private String code;
}

@DefaultCategory(value = SYSTEM_OPERATIONS, reason = "columnas técnicas de la factura")
public class Invoice {

    @DerivedFrom(type = Customer.class, value = "id")   // lo que guarda la referencia: la clave de un Customer
    private Customer customer;

    private Country country;                            // elemento de catálogo, sin datos personales: valor por defecto

    private Long number;                                // valor por defecto de la clase
}
```

## Ejemplo

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.Composite;
import java.util.List;
import static io.github.devbpm.linneo.Categories.*;

@Composite
public class Address {                                  // con JPA, junto a @Embeddable
    @Category(USER_CONTACT_ADDRESS_STREET)
    private String street;

    @Category(USER_CONTACT_ADDRESS_CITY)
    private String city;
}

public class Applicant {

    @Category(USER_NAME)
    private String name;

    private Address home;                               // home.street, home.city

    private List<Address> previous;                     // previous.street, previous.city

    @Category(USER_CONTACT_ADDRESS)                     // un solo valor: debe cubrir street y city
    private Address shipping;
}
```

Al clasificar `Applicant` se obtiene:

```text
name = user.name (DECLARED)
home.street = user.contact.address.street (DECLARED)
home.city = user.contact.address.city (DECLARED)
previous.street = user.contact.address.street (DECLARED)
previous.city = user.contact.address.city (DECLARED)
shipping = user.contact.address (DECLARED)
```

Una declaración más débil para el valor guardado es un error, y el campo se clasifica igualmente:

```java
@Composite
public class Medical {
    @Category(USER_HEALTH_AND_MEDICAL)
    private String condition;
}

@Category(USER_CONTACT)                                 // WEAKER_THAN_PARTS: condition es un dato de salud
private Medical record;
```

Véase también: [Visión general de las anotaciones](README.md) · [`@Category`](category.md) ·
[`@DefaultCategory`](default-category.md) · [`@DerivedFrom`](derived-from.md) · [Classifier](../classify/classifier.md)
· [Resultados](../classify/results.md) · [Violaciones](../classify/violations.md) · curso:
[Anotar modelos](../../course/03-annotating-models.md)
