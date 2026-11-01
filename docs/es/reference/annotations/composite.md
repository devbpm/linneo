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

`io.github.devbpm.linneo.annotation.Composite` — `@Documented @Retention(RUNTIME) @Target(TYPE)`, `@since 0.1.0`

Marca una clase cuyas instancias forman parte de otro registro, como una dirección `@Embeddable` de JPA o un objeto de
valor, de modo que los campos que la contienen se clasifican por los campos de la propia clase. Es un marcador sin
elementos.

El marcador es propio de Linneo y no una anotación de un framework, para que la biblioteca no necesite ninguna
dependencia. Olvidarlo es seguro: el campo que contiene la clase se queda sin categoría y no pasa la verificación.

## Qué hace el clasificador

Un campo **sin anotación propia** cuyo tipo es una clase compuesta, o un array, una colección o un `Optional` de ella,
recibe una clasificación por cada campo del compuesto, con la ruta `campo.parte`:

| Campo | Se clasifica como |
|---|---|
| `Address home` | `home.street`, `home.city` |
| `List<Address> previous`, `Address[] previous`, `Optional<Address> previous` | `previous.street`, `previous.city` |
| `Map<String, Address> byType` | no se entra en él: un campo ordinario, que necesita su propia categoría o un valor por defecto de la clase |
| un campo con `@Category`, `@CategoryByRow` o `@DerivedFrom` | una sola entrada: el compuesto se guarda como un solo valor (véase más abajo) |
| un campo con `@Exempt` | se omite, y se lista en `exempted()` |

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
  compuesto entero (`"home"`).
- Las clases compuestas no hace falta pasarlas por separado a `Classifier.verify`: se clasifican como parte de cada
  clase que las contiene. Clasificar una directamente también funciona, como con cualquier clase.

## Un compuesto guardado como un solo valor

Un campo que guarda el compuesto como un solo valor, por ejemplo como JSON en una única columna, declara su categoría
con [`@Category`](category.md) o [`@CategoryByRow`](category-by-row.md). El campo es entonces una sola entrada, y su
protección debe cubrir la protección de **todos los campos del compuesto**: si no, el valor quedaría clasificado por
debajo de lo que contiene.

| Regla | Violación | Gravedad | ¿Se clasifica el campo? |
|---|---|---|---|
| La categoría declarada protege al menos tanto como el `ceiling()` del compuesto | `WEAKER_THAN_PARTS` (`'<key>' protects less than the fields of <Composite> need (<natures>, <confidentiality>, <integrity>[, secret])`) | error | **sí** (el error se informa igualmente) |
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
[`@DefaultCategory`](default-category.md) · [`@DerivedFrom`](derived-from.md) · [Classifier](../classify/classifier.md) ·
[Resultados](../classify/results.md) · [Violaciones](../classify/violations.md) ·
curso: [Anotar modelos](../../course/03-annotating-models.md)
