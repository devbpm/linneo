---
title: "@CategoryByRow"
parent: "Anotaciones"
grand_parent: "Referencia"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/CategoryByRow.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @CategoryByRow

`io.github.devbpm.linneo.annotation.CategoryByRow` —
`@Documented @Retention(RUNTIME) @Target({FIELD, RECORD_COMPONENT})`, `@since 0.1.0`

Declara un campo cuya categoría depende de cada fila, como el contenido de un documento que en una fila puede ser un
certificado y en la siguiente un informe médico. Donde hace falta una sola categoría para todo el campo (un catálogo,
una etiqueta de exportación) se usa el `ceiling`: la categoría más protectora que puede tener cualquier fila.

## Elementos

| Elemento | Tipo | Por defecto | Significado |
|---|---|---|---|
| `declaredBy` | `String` | obligatorio | Qué decide la categoría de cada fila, para quienes revisan la clasificación. No puede estar en blanco. |
| `ceiling` | `String` | obligatorio | Clave de la categoría más protectora que puede tener una fila. Puede ser personal. |

No hay elementos de nivel ni de identificabilidad: el campo toma los niveles del techo.

## Reglas que aplica el Classifier

| Regla | Violación | Gravedad | ¿Se clasifica el campo? |
|---|---|---|---|
| `declaredBy` no puede estar en blanco | `MISSING_REASON` (`@CategoryByRow needs declaredBy`) | error | **sí** (el error se informa igualmente) |
| `ceiling` debe ser una clave de la taxonomía | `UNKNOWN_CATEGORY` (`unknown data category '<key>'`) | error | no |
| Un `ceiling` obsoleto se sustituye por su sustituta vigente | `DEPRECATED_CATEGORY` | aviso | sí, con la sustituta |
| No combinarla con `@Category` ni con `@Exempt` | `CONFLICTING_ANNOTATIONS` | error | no |

## FieldClassification resultante

| Componente | Valor |
|---|---|
| `category` | el techo resuelto |
| `basis` | `BY_ROW` |
| `reason` | `declaredBy` |
| `confidentiality` / `integrity` | los niveles del techo |
| `identifiability` | `IDENTIFIED` si el techo es personal; si no, `null` |

## Ejemplo

```java
import io.github.devbpm.linneo.annotation.CategoryByRow;
import static io.github.devbpm.linneo.Categories.*;

public class Document {

    @CategoryByRow(declaredBy = "the category of the document", ceiling = USER_HEALTH_AND_MEDICAL)
    private byte[] content;   // BY_ROW, SPECIAL_CATEGORY, HIGH / HIGH, IDENTIFIED
}
```

Véase también: [Visión general de las anotaciones](README.md) · [`@Category`](category.md) ·
[Resultados](../classify/results.md) · [Violaciones](../classify/violations.md) ·
curso: [Anotar modelos](../../course/03-annotating-models.md)
