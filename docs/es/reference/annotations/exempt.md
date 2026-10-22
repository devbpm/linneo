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

Excluye de la clasificación un campo que no contiene datos del modelo: un valor calculado a partir de otros campos,
una caché o un manejador técnico.

Los campos estáticos, sintéticos, `transient` y `$$_...` (los añade Hibernate) se excluyen **siempre** y no necesitan anotación. Un `@Exempt` en uno
de esos campos se ignora por completo (ni siquiera se comprueba su motivo).

## Elementos

| Elemento | Tipo | Por defecto | Significado |
|---|---|---|---|
| `value` | `String` | obligatorio | Por qué el campo no contiene datos, para quienes revisan la clasificación. No puede estar en blanco. |

## Reglas que aplica el Classifier

| Regla | Violación | Gravedad |
|---|---|---|
| `value` no puede estar en blanco | `MISSING_REASON` (`@Exempt needs a reason`) | error |
| No combinarla con `@Category` ni con `@CategoryByRow` | `CONFLICTING_ANNOTATIONS` (`@Exempt cannot be combined with @Category or @CategoryByRow`) | error |

Un campo exento nunca aparece en `ClassClassification.fields()`, tenga o no violaciones; ambas comprobaciones pueden
informar sobre el mismo campo.

## Ejemplo

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.Exempt;
import static io.github.devbpm.linneo.Categories.*;

public class Person {

    @Category(USER_NAME)
    private String fullName;

    @Exempt("computed from fullName")
    private String initials;

    private transient Object cache;     // excluido sin anotación
}
```

Véase también: [Visión general de las anotaciones](README.md) · [Classifier](../classify/classifier.md) ·
[Violaciones](../classify/violations.md) · curso: [Anotar modelos](../../course/03-annotating-models.md)
