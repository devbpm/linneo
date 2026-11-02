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

El marcador es propio de Linneo y no una anotación de un framework, para que la biblioteca no necesite ninguna
dependencia. Olvidarlo es seguro: un campo que contiene una clase cuyos campos están clasificados, sin que la clase sea
un compuesto, no toma el valor por defecto de la clase, así que no pasa la verificación (`UNCLASSIFIED_FIELD`) hasta
que se marque la clase o el campo declare su propia categoría.

## Qué hace el clasificador

Un campo **sin anotación propia** cuyo tipo es una clase compuesta, o un array, una colección, un `Optional` o un
**valor** de `Map` de ella, a cualquier profundidad, recibe una clasificación por cada campo del compuesto, con la ruta
`campo.parte`:

| Campo | Se clasifica como |
|---|---|
| `Address home` | `home.street`, `home.city` |
| `List<Address> previous`, `Address[] previous`, `Optional<Address> previous` | `previous.street`, `previous.city` |
| `Map<String, Address> byType`, `Map<String, List<Address>> byType` | `byType.street`, `byType.city`: se entra en los valores de un mapa y en los contenedores anidados a cualquier profundidad |
| un campo que puede contener datos que el clasificador no puede dividir en campos (véase más abajo) | `UNCLASSIFIED_FIELD`, salvo que declare su propia categoría |
| un campo con `@Category`, `@CategoryByRow` o `@DerivedFrom` | una sola entrada: el compuesto se guarda como un solo valor (véase más abajo) |
| un campo con `@Exempt` | se omite, y se lista en `exempted()` |

- **Las variables de tipo** se resuelven a través de las superclases de la clase que se clasifica: un `T value` de una
  clase base genérica, clasificado para `Box extends Base<Address>`, es un `Address`.
- **Un valor por defecto de la clase nunca se aplica a un campo que puede contener datos que el clasificador no ve**,
  así que envolver un compuesto nunca le da un valor por defecto no personal. Estos campos son un error
  `UNCLASSIFIED_FIELD` salvo que estén anotados: una **clave** de mapa que es un compuesto; un compuesto como argumento
  de tipo de una clase que no es una colección, como `Wrapper<Address>`; varios compuestos en un mismo tipo, como
  `Map<Address, Address>`; un valor tipado `Object`, una variable de tipo sin enlazar o un contenedor sin parametrizar;
  y otra clase con campos clasificados que **no** es un compuesto, como una entidad referenciada (márcala `@Composite`
  para clasificar aquí sus campos, o declara lo que guarda el campo, como la clave del registro).
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

## Un compuesto guardado como un solo valor

Un campo que guarda el compuesto como un solo valor, por ejemplo como JSON en una única columna, o que lo contiene donde
sus campos no pueden ser rutas (una clave de mapa, o un argumento de tipo de una clase que no es una colección), declara
su categoría con [`@Category`](category.md) o [`@CategoryByRow`](category-by-row.md). El campo es entonces una sola
entrada, y su protección debe cubrir la protección de **todos los campos de los compuestos que contiene**: si no, el
valor quedaría clasificado por debajo de lo que contiene. Lo mismo vale para un campo que guarda compuestos como un solo
valor y toma su categoría de una fuente de [`@DerivedFrom`](derived-from.md). La comprobación abarca también un
compuesto que se contiene a sí mismo y compuestos que se contienen entre sí, sea cual sea el orden en que se clasifican
las clases.

| Regla | Violación | Gravedad | ¿Se clasifica el campo? |
|---|---|---|---|
| La categoría declarada protege al menos tanto como los campos de cada compuesto que contiene el campo | `WEAKER_THAN_PARTS` (`'<key>' protects less than the fields of [<Composite>, ...] need (<natures>, <confidentiality>, <integrity>[, secret])`) | error | **sí** (el error se informa igualmente) |
| El compuesto no se contiene a sí mismo, directamente ni a través de otros compuestos | `CYCLE` (`the composite <Composite> contains itself; classify the field as one value with @Category or @CategoryByRow`) | error | no (el campo que cierra el bucle) |

Un compuesto que se contiene a sí mismo (`Node` con un `List<Node> children`) no se puede expandir: el error se
informa en el campo que cierra el bucle (`tree.children`), y los demás campos del compuesto se clasifican con
normalidad. Clasifica ese campo como un solo valor para eliminar el error.

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
