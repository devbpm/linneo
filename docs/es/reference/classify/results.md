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
public record ClassClassification(Class<?> type, List<FieldClassification> fields, List<Violation> violations)
```

La clasificación de una clase: una entrada por campo clasificado, incluidos los heredados, más los problemas
encontrados.

| Componente | Descripción |
|---|---|
| `type` | La clase clasificada. |
| `fields` | Los campos clasificados, primero los de las superclases, en orden de declaración. No incluye los campos exentos ni excluidos; un campo con error puede faltar. |
| `violations` | Todas las [`Violation`](violations.md) encontradas, errores y avisos. |

El constructor canónico lanza `NullPointerException` si `type` es `null` y copia ambas listas en listas inmutables.

| Método | Devuelve | Descripción |
|---|---|---|
| `boolean isValid()` | `boolean` | `true` si no hay errores; los avisos se permiten. |
| `List<Violation> errors()` | lista | Las violaciones de gravedad `ERROR`. |
| `List<FieldClassification> personalFields()` | lista | Los campos cuyo `isPersonal()` es `true`. |
| `boolean holdsPersonalData()` | `boolean` | `true` si algún campo es personal. |
| `Optional<FieldClassification> field(String name)` | `Optional` | La clasificación de un campo por nombre; vacío si el campo no existe, está excluido, está exento o se descartó por un error. Si un campo de una subclase oculta otro del mismo nombre de una superclase, se devuelve el de la superclase (aparece antes). |

```java
Classifier classifier = new Classifier(Taxonomy.standard());
ClassClassification person = classifier.classify(Person.class);
person.isValid();
person.personalFields();
DataCategory category = person.field("email").orElseThrow().category();
```

## FieldClassification

```java
public record FieldClassification(Class<?> declaringClass, String field, DataCategory category, Basis basis,
                                  String reason, Confidentiality confidentiality, Integrity integrity,
                                  Identifiability identifiability)
```

La clasificación de un campo: su categoría y la protección efectiva del campo.

| Componente | Admite `null` | Descripción |
|---|---|---|
| `declaringClass` | no | La clase que declara el campo (una superclase en los campos heredados). |
| `field` | no | El nombre del campo. |
| `category` | no | La categoría **vigente**: una clave obsoleta ya está resuelta a su sustituta. |
| `basis` | no | De dónde viene la categoría (ver `Basis`). |
| `reason` | sí | El `reason` de un valor por defecto de clase o el `declaredBy` de una categoría por fila; `null` para `DECLARED`. |
| `confidentiality` | no | El nivel de la categoría, o el más estricto que declara el campo. |
| `integrity` | no | El nivel de la categoría, o el más estricto que declara el campo. |
| `identifiability` | sí | Cómo apunta el valor a la persona; `null` si la categoría no es personal (y en los valores por defecto de clase, que nunca son personales). |

El constructor canónico lanza `NullPointerException` si `declaringClass`, `field`, `category`, `basis`,
`confidentiality` o `integrity` son `null`.

### Basis

`FieldClassification.Basis` — de dónde viene la categoría de un campo.

| Constante | Origen | `reason` |
|---|---|---|
| `DECLARED` | un [`@Category`](../annotations/category.md) en el campo | `null` |
| `CLASS_DEFAULT` | el [`@DefaultCategory`](../annotations/default-category.md) de una clase | el `reason` del valor por defecto |
| `BY_ROW` | el techo de un [`@CategoryByRow`](../annotations/category-by-row.md) | `declaredBy` |

### Métodos

| Método | Descripción |
|---|---|
| `boolean isPersonal()` | Si los valores almacenados son datos personales: la categoría es personal **y** la identificabilidad es `IDENTIFIED` o `PSEUDONYMIZED`. Los valores anonimizados o agregados de una categoría personal no lo son. |
| `boolean isSecret()` | La marca `secret` de la categoría: el campo guarda un secreto que no debe leerse de vuelta, exportarse ni publicarse nunca. |
| `String toString()` | `<DeclaringSimpleName>.<field> = <clave de la categoría> (<BASIS>)`, por ejemplo `Person.email = user.contact.email (DECLARED)`. |

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
