---
title: "Resultados de la clasificación"
parent: "Referencia"
nav_order: 10
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/ClassClassification.java
  - src/main/java/io/github/devbpm/linneo/classify/FieldClassification.java
-->

# Resultados de la clasificación

Lo que devuelve el [`Classifier`](classifier.md). Ambos tipos son records de `io.github.devbpm.linneo.classify`,
`@since 0.1.0`.

## ClassClassification

```java
public record ClassClassification(Class<?> type, List<FieldClassification> fields, List<Exemption> exempted,
                                  List<Violation> violations)
```

La clasificación de una clase: una entrada por campo clasificado, incluidos los heredados, los campos excluidos con
`@Exempt` y los problemas encontrados.

| Componente | Descripción |
|---|---|
| `type` | La clase clasificada. |
| `fields` | Los campos clasificados, primero los de las superclases y, dentro de una clase, en el orden que informa la JVM, que normalmente es el de declaración pero no está garantizado. Identifica un campo por su `path`, nunca por su posición. No incluye los campos exentos ni excluidos; un campo con error puede faltar. |
| `exempted` | Los campos excluidos con [`@Exempt`](../annotations/exempt.md), con sus motivos (ver [`Exemption`](#exemption)). |
| `violations` | Todas las [`Violation`](violations.md) encontradas, errores y avisos. |

El constructor canónico lanza `NullPointerException` si `type` es `null` y copia las tres listas en listas
inmutables.

| Método | Devuelve | Descripción |
|---|---|---|
| `boolean isValid()` | `boolean` | `true` si no hay errores; los avisos se permiten. |
| `List<Violation> errors()` | lista | Las violaciones de gravedad `ERROR`. |
| `List<FieldClassification> personalFields()` | lista | Los campos cuyo `isPersonal()` es `true`. |
| `boolean holdsPersonalData()` | `boolean` | `true` si algún campo es personal. |
| `Optional<FieldClassification> field(String path)` | `Optional` | La clasificación de un campo por ruta; vacío si el campo no existe, está excluido, está exento o se descartó por un error. Un campo que oculta otro del mismo nombre de una superclase no se clasifica (`DUPLICATE_FIELD`), así que una ruta nunca es ambigua. |
| `Protection ceiling()` | [`Protection`](../protection-levels.md#protection) | La protección que requiere un registro de la clase en conjunto: la combinación de la protección de sus campos, o `Protection.LOWEST` si no hay ninguno. Es lo que necesita una exportación de registros completos o una columna que guarda el registro serializado. |
| `Protection ceilingOf(String path)` | [`Protection`](../protection-levels.md#protection) | La protección que requiere un campo junto con todo lo que cuelga de él: el propio campo o, en un campo que contiene un [`@Composite`](../annotations/composite.md), todos los campos del compuesto. Es la combinación del campo y de cada campo cuya ruta empieza por la suya seguida de un punto, o `Protection.LOWEST` si no hay ninguno. `NullPointerException` si `path` es `null`. |

```java
Classifier classifier = new Classifier(Taxonomy.standard());
ClassClassification person = classifier.classify(Person.class);
person.isValid();
person.personalFields();
DataCategory category = person.field("email").orElseThrow().category();
Protection whole = person.ceiling();                 // lo que requiere un registro Person completo
Protection email = person.ceilingOf("email");        // el campo y cualquier campo que cuelgue de él
Protection home = person.ceilingOf("home");          // un campo compuesto: todos los home.street, home.city, ...
```

### Exemption

`ClassClassification.Exemption` — `public record Exemption(Class<?> declaringClass, String path, String reason)`,
`@since 0.1.0`

Un campo excluido de la clasificación con `@Exempt`. Se lista para que se note cuando se excluye un campo que antes
estaba clasificado.

| Componente | Descripción |
|---|---|
| `declaringClass` | La clase que declara el campo. |
| `path` | La ruta del campo: su nombre, o `campo.parte` para un campo exento de un [`@Composite`](../annotations/composite.md) que contiene la clase. |
| `reason` | Por qué el campo no contiene datos del modelo: el valor de `@Exempt`. |

El constructor canónico lanza `NullPointerException` si un componente es `null`. Un `@Exempt` combinado con
`@Category`, `@CategoryByRow` o `@DerivedFrom` es un error `CONFLICTING_ANNOTATIONS` y no se lista.

## FieldClassification

```java
public record FieldClassification(Class<?> declaringClass, String path, DataCategory category, Basis basis,
                                  String reason, Confidentiality confidentiality, Integrity integrity,
                                  Identifiability identifiability, List<String> derivedFrom)
```

La clasificación de un campo: su categoría y la protección efectiva del campo.

| Componente | Admite `null` | Descripción |
|---|---|---|
| `declaringClass` | no | La clase que declara el campo (una superclase en los campos heredados, el compuesto en un campo de un [`@Composite`](../annotations/composite.md)). |
| `path` | no | Identifica el campo dentro de la clase clasificada: su nombre, o `campo.parte` para un campo de un compuesto que contiene (`home.street`). |
| `category` | no | La categoría **vigente**: una clave obsoleta ya está resuelta a su sustituta. |
| `basis` | no | De dónde viene la categoría (ver `Basis`). |
| `reason` | sí | El `reason` de un valor por defecto de clase o el `declaredBy` de una categoría por fila; `null` para `DECLARED`. |
| `confidentiality` | no | El nivel de la categoría, o el más estricto que declara el campo. |
| `integrity` | no | El nivel de la categoría, o el más estricto que declara el campo. |
| `identifiability` | sí | Cómo apunta el valor a la persona; `null` si la categoría no es personal (y en los valores por defecto de clase, que nunca son personales). |
| `derivedFrom` | no | En un campo [`DERIVED`](#basis), sus fuentes: rutas de campos de la misma clase (una ruta de compuesto se expande a sus campos), o `ClassName#path`, con el nombre binario de la clase, para un campo de otra clase. Vacía en cualquier otro campo. |

El constructor canónico lanza `NullPointerException` si `declaringClass`, `path`, `category`, `basis`,
`confidentiality` o `integrity` son `null`. Un `derivedFrom` `null` significa ninguna; la lista se copia en una
inmutable.

### Basis

`FieldClassification.Basis` — de dónde viene la categoría de un campo.

| Constante | Origen | `reason` |
|---|---|---|
| `DECLARED` | un [`@Category`](../annotations/category.md) en el campo | `null` |
| `CLASS_DEFAULT` | el [`@DefaultCategory`](../annotations/default-category.md) de una clase | el `reason` del valor por defecto |
| `BY_ROW` | el techo de un [`@CategoryByRow`](../annotations/category-by-row.md) | `declaredBy` |
| `DERIVED` | un campo [`@DerivedFrom`](../annotations/derived-from.md): la clasificación de su fuente, o la categoría que declara, que protege al menos tanto como sus fuentes | `null` |

Los campos de un [`@Composite`](../annotations/composite.md) tienen la base que les da su propia clase, no una base
propia.

### Métodos

| Método | Descripción |
|---|---|
| `boolean isPersonal()` | Si los valores almacenados son datos personales: la categoría es personal **y** la identificabilidad es `IDENTIFIED` o `PSEUDONYMIZED`. Los valores anonimizados o agregados de una categoría personal no lo son. |
| `boolean isSecret()` | La marca `secret` de la categoría: el campo guarda un secreto que no debe leerse de vuelta, exportarse ni publicarse nunca. |
| `String fieldName()` | El nombre del campo: el último segmento de su `path`, tal como se declara en `declaringClass()`. |
| `Protection protection()` | La [protección](../protection-levels.md#protection) que requieren los valores almacenados: la naturaleza de la categoría cuando los valores son datos personales (`isPersonal()`) y `LegalNature.NONE` cuando están anonimizados o agregados, con la `confidentiality` y la `integrity` efectivas del campo y la marca de secreto de la categoría. |
| `String toString()` | `<path> = <clave de la categoría> (<BASIS>)`, por ejemplo `email = user.contact.email (DECLARED)`. |

```java
for (FieldClassification field : person.fields()) {
    if (field.isSecret()) {
        // no exportar nunca
    } else if (field.isPersonal() && field.confidentiality().compareTo(Confidentiality.MEDIUM) >= 0) {
        // enmascarar en los logs
    }
}
```

Véase también: [Classifier](classifier.md) · [Violaciones](violations.md) ·
[Niveles de protección](../protection-levels.md) · [Catálogo de OpenMetadata](../openmetadata/catalog.md) ·
curso: [Usar la clasificación](../../course/05-using-the-classification.md)
