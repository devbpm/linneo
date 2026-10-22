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
  - src/main/java/io/github/devbpm/linneo/annotation/Exempt.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# Anotaciones

Cuatro anotaciones de `io.github.devbpm.linneo.annotation` declaran qué contiene un modelo de datos. Todas son
`@Documented`, `@Retention(RUNTIME)` y `@since 0.1.0`. Las lee el [`Classifier`](../classify/classifier.md), que
nunca lee los valores de los campos.

| Anotación | Destino | Declara | Página |
|---|---|---|---|
| `@Category` | campo, componente de record | la categoría de un campo, opcionalmente con niveles más estrictos y su identificabilidad | [category.md](category.md) |
| `@DefaultCategory` | tipo | la categoría no personal de todos los campos sin anotar de una clase, con el motivo | [default-category.md](default-category.md) |
| `@CategoryByRow` | campo, componente de record | un campo cuya categoría depende de cada fila, con la categoría más protectora que puede tener una fila | [category-by-row.md](category-by-row.md) |
| `@Exempt` | campo, componente de record | un campo que no contiene datos del modelo, con el motivo | [exempt.md](exempt.md) |

En un componente de record, las anotaciones se aplican también al campo del componente, que es lo que lee el
clasificador.

## Qué campos se clasifican

El clasificador recorre la clase y sus superclases (se detiene antes de `Object`, `Record` y `Enum`) y considera
todos los campos que declaran salvo los **estáticos**, **transient** y **sintéticos** (y los llamados `$$_...`, que añade Hibernate), que se omiten siempre sin
necesidad de anotación. Consulta [Classifier](../classify/classifier.md#qué-campos-se-examinan).

## Orden de resolución

Para cada campo considerado, gana la primera regla que se cumple:

| # | Regla | Resultado (`FieldClassification.basis()`) |
|---|---|---|
| 1 | El campo tiene `@Exempt` | se omite: sin clasificación |
| 2 | El campo tiene `@Category` | la categoría declarada (`DECLARED`) |
| 2 | El campo tiene `@CategoryByRow` | la categoría techo (`BY_ROW`) |
| 3 | La clase que **declara** el campo tiene `@DefaultCategory` | ese valor por defecto (`CLASS_DEFAULT`) |
| 4 | La clase **que se clasifica** tiene `@DefaultCategory` | ese valor por defecto (`CLASS_DEFAULT`) |
| 5 | ninguna de las anteriores | error `UNCLASSIFIED_FIELD` |

Consecuencias:

- Una anotación en el campo siempre prevalece sobre el valor por defecto de la clase.
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
| `@Exempt` + `@Category` y/o `@CategoryByRow` | `CONFLICTING_ANNOTATIONS`: `@Exempt cannot be combined with @Category or @CategoryByRow` | se omite el campo (el motivo de `@Exempt` se sigue comprobando) |
| `@Category` + `@CategoryByRow` | `CONFLICTING_ANNOTATIONS`: `@Category and @CategoryByRow cannot be used together` | el campo no se clasifica |
| `@Category` o `@CategoryByRow` + `@DefaultCategory` de la clase | ninguna | prevalece la anotación del campo |

Todos los conflictos son errores. Consulta [Violaciones](../classify/violations.md) para ver todos los códigos.

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

    @Exempt("computed from code")
    private String displayKey;                // se omite

    private transient String cache;           // se omite: transient
}
```

Véase también: [`@Category`](category.md) · [`@DefaultCategory`](default-category.md) ·
[`@CategoryByRow`](category-by-row.md) · [`@Exempt`](exempt.md) · [Classifier](../classify/classifier.md) ·
curso: [Anotar modelos](../../course/03-annotating-models.md)
