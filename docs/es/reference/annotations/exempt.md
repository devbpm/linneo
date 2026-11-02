---
title: "@Exempt"
parent: "Anotaciones"
grand_parent: "Referencia"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/Exempt.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @Exempt

`io.github.devbpm.linneo.annotation.Exempt` —
`@Documented @Retention(RUNTIME) @Target({FIELD, RECORD_COMPONENT})`, `@since 0.1.0`

Excluye de la clasificación un campo que no contiene datos del modelo: una caché, un manejador técnico o un estado que
mantiene un framework.

Un valor calculado a partir de otros campos **no** es un campo así: la edad calculada a partir de una fecha de
nacimiento es un dato personal. Se clasifica con [`@DerivedFrom`](derived-from.md) o [`@Category`](category.md),
nunca con `@Exempt`.

Una relación con un registro que contiene datos personales tampoco es un campo así: una referencia al registro de una
persona es en sí misma un dato personal, una clave seudónima. Declara lo que guarda el campo, normalmente la clave del
registro, con [`@DerivedFrom`](derived-from.md) (véase el ejemplo más abajo). Una referencia a un registro que no
contiene datos personales, como un elemento de un catálogo, no contiene datos del modelo.

Los campos estáticos, sintéticos y `$$_...` (los añade Hibernate) se excluyen **siempre** y no necesitan anotación. Un
`@Exempt` en uno de esos campos se ignora por completo (ni siquiera se comprueba su motivo). Un campo `transient` se
excluye también, **salvo que lleve una de las anotaciones de campo de Linneo** (`@Category`, `@CategoryByRow`,
`@DerivedFrom` o `@Exempt`): entonces se clasifica como cualquier otro campo, y un `@Exempt` en él se comprueba y se lista
en `exempted()`.

## Elementos

| Elemento | Tipo | Por defecto | Significado |
|---|---|---|---|
| `value` | `String` | obligatorio | Por qué el campo no contiene datos, para quienes revisan la clasificación. No puede estar en blanco. |

## Reglas que aplica el Classifier

| Regla | Violación | Gravedad |
|---|---|---|
| `value` no puede estar en blanco | `MISSING_REASON` (`@Exempt needs a reason`) | error |
| No combinarla con `@Category`, `@CategoryByRow` ni `@DerivedFrom` | `CONFLICTING_ANNOTATIONS` (`@Exempt cannot be combined with @Category, @CategoryByRow or @DerivedFrom`) | error |
| No ponerla en un campo que contiene [compuestos](composite.md) | `CONFLICTING_ANNOTATIONS` (`@Exempt is for a field that holds no data of the model, and this one holds [<Composite>]: classify it as one value with @Category or @CategoryByRow`) | error |
| No ponerla en un campo que se refiere a un registro con datos personales | `CONFLICTING_ANNOTATIONS` (`@Exempt is for a field that holds no data of the model, and this one refers to <Record> that holds personal data, and a reference to a person's record is personal data: declare what the field stores with @Category, or take the classification of the record's key with @DerivedFrom(type = <Record>.class, value = "id")`) | error |

Un campo exento nunca aparece en `ClassClassification.fields()`, tenga o no violaciones; ambas comprobaciones pueden
informar sobre el mismo campo. Un campo exento se lista, con su motivo, en
[`ClassClassification.exempted()`](../classify/results.md#exemption), para que se note cuando se excluye un campo que
antes estaba clasificado; la excepción es un campo que combina `@Exempt` con `@Category`, `@CategoryByRow` o
`@DerivedFrom`, o que contiene compuestos, que es un error y no se lista.

Un campo exento no puede ser la fuente de un [`@DerivedFrom`](derived-from.md): la fuente debe ser un campo
clasificado (`UNKNOWN_SOURCE`). Un campo exento dentro de un [`@Composite`](composite.md) se lista en `exempted()`
con la ruta `campo.parte`.

## Ejemplo

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.DerivedFrom;
import io.github.devbpm.linneo.annotation.Exempt;
import static io.github.devbpm.linneo.Categories.*;

public class Person {

    @Category(USER_NAME)
    private String fullName;

    @DerivedFrom("fullName")        // calculado a partir de fullName: es un dato, no va con @Exempt
    private String initials;

    @Exempt("handle the persistence framework keeps")
    private Object handle;          // técnico: no contiene datos del modelo

    private transient Object cache; // excluido sin anotación
}
```

Una relación con registros que contienen datos personales, aquí los `Attachment` de una asociación uno a muchos, no se
exime: el campo guarda sus claves, y eso es lo que declara.

```java
@DerivedFrom(type = Attachment.class, value = "id")   // la clave de cada Attachment: dato personal, no se exime
private List<Attachment> attachments;
```

Véase también: [Visión general de las anotaciones](README.md) · [`@DerivedFrom`](derived-from.md) ·
[Classifier](../classify/classifier.md) · [Violaciones](../classify/violations.md) · curso:
[Anotar modelos](../../course/03-annotating-models.md)
