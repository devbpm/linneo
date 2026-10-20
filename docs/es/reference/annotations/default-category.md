---
title: "@DefaultCategory"
parent: "Anotaciones"
grand_parent: "Referencia"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/DefaultCategory.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @DefaultCategory

`io.github.devbpm.linneo.annotation.DefaultCategory` — `@Documented @Retention(RUNTIME) @Target(TYPE)`, `@since 0.1.0`

Declara la categoría de todos los campos de una clase que no tienen anotación propia: una afirmación explícita de que
esos campos **no contienen datos personales**, con el motivo. Los datos personales se declaran siempre campo a campo
con [`@Category`](category.md).

## Elementos

| Elemento | Tipo | Por defecto | Significado |
|---|---|---|---|
| `value` | `String` | obligatorio | Clave de una categoría **no personal**, preferiblemente una constante de [`Categories`](../key-constants.md). |
| `reason` | `String` | obligatorio | Por qué la clase no contiene datos personales, para quienes revisan la clasificación. No puede estar en blanco. |

## Alcance

- Se aplica a los campos sin anotar que **declara** la clase anotada.
- Cuando la clase anotada es la que se clasifica, se aplica también a los campos heredados sin anotar de superclases
  que no declaran un valor por defecto propio.
- **No se hereda**: una subclase sin su propio `@DefaultCategory` no usa el de su superclase para los campos que
  declara ella.
- Las anotaciones de campo (`@Category`, `@CategoryByRow`, `@Exempt`) siempre tienen prioridad.

Consulta el [orden de resolución](README.md#orden-de-resolución).

## Reglas que aplica el Classifier

El clasificador comprueba el `@DefaultCategory` de la clase clasificada y el de cada una de sus superclases. Estas
violaciones no tienen campo (`Violation.field()` es `null`) y su mensaje empieza por
`@DefaultCategory of <SimpleName>`.

| Regla | Violación | Gravedad |
|---|---|---|
| `reason` no puede estar en blanco | `MISSING_REASON` (`@DefaultCategory of <Class> needs a reason`) | error |
| `value` debe ser una clave de la taxonomía | `UNKNOWN_CATEGORY` (`unknown data category '<key>'`) | error |
| Un `value` obsoleto se sustituye por su sustituta vigente | `DEPRECATED_CATEGORY` | aviso |
| La categoría (resuelta) no puede ser personal | `PERSONAL_DEFAULT` (`@DefaultCategory of <Class> names '<key>', which is personal data: declare personal fields one by one with @Category`) | error |

Si el valor por defecto es desconocido o personal, los campos que cubriría quedan fuera de
`ClassClassification.fields()`; la violación de clase es el único aviso (no se añade `UNCLASSIFIED_FIELD`). Un
`reason` en blanco, por sí solo, no impide que los campos se clasifiquen.

## FieldClassification resultante

| Componente | Valor |
|---|---|
| `declaringClass` | la clase que declara el campo |
| `category` | la categoría por defecto resuelta |
| `basis` | `CLASS_DEFAULT` |
| `reason` | el `reason` de la anotación |
| `confidentiality` / `integrity` | los niveles de la categoría |
| `identifiability` | `null` |

## Ejemplos

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import static io.github.devbpm.linneo.Categories.*;

public abstract class Audited {
    @Category(USER_UNIQUE_ID)
    private Long createdBy;                 // DECLARED
    private java.time.Instant createdAt;    // lo cubre el valor por defecto de la subclase clasificada
}

@DefaultCategory(value = SYSTEM_CONFIGURATION, reason = "menu configuration")
public class MenuGroup extends Audited {
    private String code;                    // CLASS_DEFAULT: system.configuration
}
```

Al clasificar `MenuGroup` se obtienen los campos `createdBy`, `createdAt` y `code`. Al clasificar otra subclase de
`Audited` sin valor por defecto se obtiene `UNCLASSIFIED_FIELD` para `createdAt`.

```java
@DefaultCategory(value = USER_CONTACT, reason = "")    // PERSONAL_DEFAULT y MISSING_REASON
public class WrongDefault {
    private String value;
}
```

Véase también: [Visión general de las anotaciones](README.md) · [`@Category`](category.md) ·
[Violaciones](../classify/violations.md) · [Taxonomía estándar](../standard-taxonomy.md) ·
curso: [Anotar modelos](../../course/03-annotating-models.md)
