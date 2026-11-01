---
title: "Exportación"
parent: "Referencia"
nav_order: 19
permalink: "/es/reference/export/"
---
<!--
doc-sources:
  - src/main/java/module-info.java
  - src/main/java/io/github/devbpm/linneo/export/openapi/package-info.java
  - src/main/java/io/github/devbpm/linneo/export/postgresql/package-info.java
  - src/main/java/io/github/devbpm/linneo/export/odcs/package-info.java
-->

# Exportación

Paquetes `io.github.devbpm.linneo.export.openapi`, `.postgresql` y `.odcs` — exportados, `@since 0.1.0`

Los [exportadores de catálogo](../README.md#comparativa-de-catálogos-de-datos) publican la **taxonomía**. Estos tres
publican la **clasificación de los campos de un modelo** en las herramientas que acompañan al código: la descripción de
una API, la base de datos que guarda los datos y el contrato de datos que leen otros equipos. Cada uno toma lo que el
[`Classifier`](../classify/classifier.md) sabe de un campo, o lo que un [inventario de datos](../classify/inventory.md)
ha guardado de él, y escribe el texto que espera la otra herramienta.

| Paquete | Punto de entrada | Escribe | Página |
|---|---|---|---|
| `io.github.devbpm.linneo.export.openapi` | `OpenApiExtension` | La extensión de la especificación `x-linneo` de cada propiedad de la descripción de una API, como mapas o JSON | [Extensión OpenAPI](openapi.md) |
| `io.github.devbpm.linneo.export.postgresql` | `PostgresAnonymizer` | Las sentencias `SECURITY LABEL` de PostgreSQL Anonymizer que enmascaran o anulan cada columna, según una [`DisclosurePolicy`](../classify/disclosure.md) | [PostgreSQL Anonymizer](postgresql.md) |
| `io.github.devbpm.linneo.export.odcs` | `OdcsSchema` | La sección `schema` de un contrato de datos del Open Data Contract Standard (v3), a partir de un inventario de datos, como JSON | [Open Data Contract Standard](odcs.md) |

Lo que tienen en común:

- **Escriben texto y nada más.** Ninguno lee ni escribe la descripción de tu API, se conecta a una base de datos o
  edita un contrato, y ninguno hace llamadas de red. La aplicación elige a qué propiedad, columna o contrato pertenece
  cada campo, y aplica el resultado.
- **La protección sale de la clasificación**, nunca de los valores: un campo se describe por su categoría, sus niveles
  (los de la categoría, elevados en el campo si el código lo dice) y si contiene un secreto.
- **Dos entradas.** Una `ClassClassification` (o sus `FieldClassification`) es lo que declara el código ahora, para una
  compilación o un arranque. Un `InventoryField` de un [`DataInventory`](../classify/inventory.md), resuelto con una
  `Taxonomy`, es lo que una aplicación ha guardado, de modo que el mismo exportador puede ejecutarse en un back office
  sin las clases del modelo. `OpenApiExtension` y `PostgresAnonymizer` aceptan ambas; `OdcsSchema` lee el inventario.
- **Una clase con errores de clasificación se rechaza.** Cuando un exportador toma una `ClassClassification`, lanza
  `IllegalArgumentException` si la clase tiene errores, porque un campo con un error no tiene clasificación y quedaría
  sin protección. Corrígela con el [test del clasificador](../../course/04-verifying-in-tests.md).
- **Los campos exentos no contienen datos.** `PostgresAnonymizer` les da una etiqueta `IS NULL` (sin regla),
  `OpenApiExtension` los describe sin ninguna extensión y `OdcsSchema` los omite.

```java
import io.github.devbpm.linneo.export.openapi.OpenApiExtension;
import io.github.devbpm.linneo.export.postgresql.PostgresAnonymizer;

ClassClassification student = classifier.classify(Student.class);

// Para la descripción de la API: una extensión por cada campo clasificado.
Map<String, Map<String, Object>> api = OpenApiExtension.of(student);

// Para la base de datos de pruebas: una sentencia por campo.
List<String> sql = PostgresAnonymizer.of(DisclosurePolicy.forLogs())
        .labels(student, path -> "students." + path);
```

Véase también: [Inventario de datos](../classify/inventory.md) · [Política de divulgación](../classify/disclosure.md) ·
[Referencia](../README.md)
