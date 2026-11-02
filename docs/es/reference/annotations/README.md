---
title: "Anotaciones"
parent: "Referencia"
nav_order: 8
permalink: "/es/reference/annotations/"
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/Category.java
  - src/main/java/io/github/devbpm/linneo/annotation/DefaultCategory.java
  - src/main/java/io/github/devbpm/linneo/annotation/CategoryByRow.java
  - src/main/java/io/github/devbpm/linneo/annotation/DerivedFrom.java
  - src/main/java/io/github/devbpm/linneo/annotation/Composite.java
  - src/main/java/io/github/devbpm/linneo/annotation/Exempt.java
  - src/main/java/io/github/devbpm/linneo/annotation/package-info.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# Anotaciones

Seis anotaciones de `io.github.devbpm.linneo.annotation` declaran qué contiene un modelo de datos. Todas son
`@Documented`, `@Retention(RUNTIME)` y `@since 0.1.0`. Las lee el [`Classifier`](../classify/classifier.md), que
nunca lee los valores de los campos.

| Anotación | Destino | Declara | Página |
|---|---|---|---|
| `@Category` | campo, componente de record | la categoría de un campo, opcionalmente con niveles más estrictos y su identificabilidad | [category.md](category.md) |
| `@DefaultCategory` | tipo | la categoría no personal de todos los campos sin anotar de una clase, con el motivo | [default-category.md](default-category.md) |
| `@CategoryByRow` | campo, componente de record | un campo cuya categoría depende de cada fila, con la categoría más protectora que puede tener una fila | [category-by-row.md](category-by-row.md) |
| `@DerivedFrom` | campo, componente de record | un campo cuyo valor se calcula a partir de otros campos: toma su clasificación, o declara una que protege al menos tanto | [derived-from.md](derived-from.md) |
| `@Composite` | tipo | una clase que forma parte de otro registro, como una dirección embebida: los campos que la contienen se clasifican por los campos de la propia clase | [composite.md](composite.md) |
| `@Exempt` | campo, componente de record | un campo que no contiene datos del modelo (caché, manejador o estado de un framework), con el motivo | [exempt.md](exempt.md) |

En un componente de record, las anotaciones se aplican también al campo del componente, que es lo que lee el
clasificador.

## Qué campos se clasifican

El clasificador recorre la clase y sus superclases (se detiene antes de `Object`, `Record` y `Enum`) y considera todos
los campos que declaran salvo los **estáticos** y **sintéticos** (y los llamados `$$_...`, que añade Hibernate), que se
omiten siempre sin necesidad de anotación, y los **transient**, que se omiten salvo que lleven una de las anotaciones de
campo de Linneo. Consulta [Classifier](../classify/classifier.md#qué-campos-se-examinan).

Un campo sin anotación propia cuyo tipo es una clase [`@Composite`](composite.md) concreta (o un array, una colección o
un `Optional` de ella, a cualquier profundidad) no se clasifica como un solo campo: se clasifica cada campo del
compuesto, con la ruta `campo.parte`.

## Orden de resolución

Para cada campo considerado, gana la primera regla que se cumple:

| # | Regla | Resultado (`FieldClassification.basis()`) |
|---|---|---|
| 1 | El campo tiene `@Exempt` | se omite: sin clasificación |
| 2 | El campo tiene `@Category` | la categoría declarada (`DECLARED`; `DERIVED` si además tiene `@DerivedFrom`) |
| 2 | El campo tiene `@CategoryByRow` | la categoría techo (`BY_ROW`) |
| 3 | El campo tiene `@DerivedFrom` y no tiene `@Category` | la clasificación de su fuente (`DERIVED`) |
| 4 | El tipo del campo es una clase `@Composite` concreta, o un array, una colección o un `Optional` de ella, a cualquier profundidad | una clasificación por cada campo del compuesto, con la ruta `campo.parte`, cada una como la resuelve su propia clase |
| 5 | La clase que **declara** el campo tiene `@DefaultCategory`, y el campo no puede contener datos que el clasificador no ve ni se refiere a un registro con datos personales | ese valor por defecto (`CLASS_DEFAULT`) |
| 6 | La clase **que se clasifica** tiene `@DefaultCategory`, y se cumple lo mismo | ese valor por defecto (`CLASS_DEFAULT`) |
| 7 | ninguna de las anteriores | error `UNCLASSIFIED_FIELD` |

Consecuencias:

- Una anotación en el campo siempre prevalece sobre el valor por defecto de la clase.
- Un valor por defecto de la clase nunca se aplica a un campo que puede contener datos que el clasificador no ve
  (compuestos en un mapa o que no se pueden dividir en campos, un compuesto abstracto, `Object`, `Serializable`, una
  variable de tipo sin enlazar o un contenedor sin parametrizar): ese campo necesita su propia anotación, o es un
  `UNCLASSIFIED_FIELD`. Consulta [`@Composite`](composite.md#qué-hace-el-clasificador).
- Un campo que se refiere a otro registro del modelo toma el valor por defecto de la clase solo cuando ese registro no
  contiene datos personales, como un elemento de un catálogo. Una referencia a un registro con datos personales necesita
  su propia anotación, normalmente `@DerivedFrom(type = Customer.class, value = "id")`, y no se puede eximir. Consulta
  [Referencias a otros registros](composite.md#referencias-a-otros-registros).
- Los campos de un compuesto se resuelven con las anotaciones y el `@DefaultCategory` del propio compuesto, nunca
  con el valor por defecto de la clase que lo contiene.
- El `@DefaultCategory` de una superclase cubre los campos que declara esa superclase, aunque se clasifique una
  subclase.
- El `@DefaultCategory` de una subclase cubre los campos que declara y los campos heredados sin anotar de
  superclases que no tienen valor por defecto propio.
- `@DefaultCategory` no se hereda: una subclase sin valor por defecto propio no usa el de su superclase para los
  campos que declara ella.
- Una clave obsoleta en cualquier anotación sigue clasificando el campo, con la sustituta vigente, y produce un aviso
  `DEPRECATED_CATEGORY`.

## Conflictos

| Combinación en un campo | Violación | Efecto |
|---|---|---|
| `@Exempt` + `@Category`, `@CategoryByRow` y/o `@DerivedFrom` | `CONFLICTING_ANNOTATIONS`: `@Exempt cannot be combined with @Category, @CategoryByRow or @DerivedFrom` | se omite el campo (el motivo de `@Exempt` se sigue comprobando) |
| `@Exempt` en un campo que contiene compuestos, o que se refiere a un registro con datos personales | `CONFLICTING_ANNOTATIONS`: `@Exempt is for a field that holds no data of the model, and this one ...` | se informa el error; consulta [`@Exempt`](exempt.md#reglas-que-aplica-el-classifier) |
| `@CategoryByRow` + `@Category` y/o `@DerivedFrom` | `CONFLICTING_ANNOTATIONS`: `@CategoryByRow cannot be combined with @Category or @DerivedFrom` | el campo no se clasifica |
| `@Category` + `@DerivedFrom` | ninguna | el campo declara su categoría y debe proteger al menos tanto como sus fuentes |
| `@Category`, `@CategoryByRow` o `@DerivedFrom` + `@DefaultCategory` de la clase | ninguna | prevalece la anotación del campo |

Los conflictos son errores. Consulta [Violaciones](../classify/violations.md) para ver todos los códigos.

## Ejemplo

```java
import io.github.devbpm.linneo.annotation.*;
import static io.github.devbpm.linneo.Categories.*;

@DefaultCategory(value = SYSTEM_CONFIGURATION, reason = "menu configuration")
public class MenuGroup {
    private String code;                      // CLASS_DEFAULT: system.configuration
    private String label;                     // CLASS_DEFAULT: system.configuration

    @Category(USER_UNIQUE_ID)                 // DECLARED: prevalece sobre el valor por defecto
    private Long createdBy;

    @Exempt("cache of the rendered menu")
    private String renderedMenu;              // se omite

    private transient String cache;           // se omite: transient
}
```

Véase también: [`@Category`](category.md) · [`@DefaultCategory`](default-category.md) ·
[`@CategoryByRow`](category-by-row.md) · [`@DerivedFrom`](derived-from.md) · [`@Composite`](composite.md) ·
[`@Exempt`](exempt.md) · [Classifier](../classify/classifier.md) ·
curso: [Anotar modelos](../../course/03-annotating-models.md)
