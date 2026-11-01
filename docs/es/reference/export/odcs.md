---
title: "Open Data Contract Standard"
parent: "Exportación"
grand_parent: "Referencia"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/export/odcs/OdcsSchema.java
  - src/main/java/io/github/devbpm/linneo/export/odcs/package-info.java
  - src/main/java/io/github/devbpm/linneo/internal/Json.java
-->

# Open Data Contract Standard

`io.github.devbpm.linneo.export.odcs.OdcsSchema` — `public final class`, `@since 0.1.0`

La clasificación de un [inventario de datos](../classify/inventory.md) como el `schema` de un contrato de datos del
[Open Data Contract Standard](https://bitol-io.github.io/open-data-contract-standard/) (ODCS v3): un objeto de esquema
por cada objeto del inventario y una propiedad por cada campo clasificado, de modo que un contrato de datos diga qué
contiene cada propiedad y cómo debe protegerse. Las instancias son inmutables y seguras entre hilos.

El resultado es JSON, que también es YAML válido: incorpóralo al contrato, cuyos servidores, tipos y responsables
Linneo no conoce. Linneo no lee ni escribe contratos. Los nombres de las propiedades son las rutas de los campos
(`home.street` para una parte de un [`@Composite`](../annotations/composite.md)); asígnalos a otros nombres cuando el
contrato use otros.

## Qué dice una propiedad

| Miembro | Valor |
|---|---|
| `name` | La ruta del campo. |
| `classification` | `public`, `internal`, `confidential` o `restricted`, según la confidencialidad del campo; véase más abajo. |
| `tags` | La clave de la categoría, luego `personal-data` en datos personales, `special-category` o `criminal-offence-data` para esas naturalezas, y `secret` para un secreto. |
| `customProperties` | `linneo.category` (la clave), `linneo.confidentiality` y `linneo.integrity` (los nombres de los niveles) y, solo en datos personales, `linneo.identifiability`; cada una como `{"property": ..., "value": ...}`. |

La clasificación usa la escala habitual de cuatro niveles:

| Confidencialidad | `classification` |
|---|---|
| `PUBLIC` | `public` |
| `LOW` | `internal` |
| `MEDIUM` | `confidential` |
| `HIGH` | `restricted` |
| cualquiera, con un secreto | `restricted` |

Los niveles son los efectivos: los de la categoría, con la elevación que declare el campo. Los campos exentos no
contienen datos y quedan fuera del esquema.

## Miembros

| Miembro | Devuelve | Descripción |
|---|---|---|
| `static OdcsSchema of(Taxonomy taxonomy)` | escritor | El escritor de esquemas para una taxonomía, que resuelve las claves que guarda el inventario. `NullPointerException` si `taxonomy` es `null`. |
| `static String classification(Protection protection)` | `String` | La etiqueta de clasificación de una protección: `public`, `internal`, `confidential` o `restricted`. |
| `String schemaJson(DataInventory inventory)` | `String` | El `schema` de un contrato: un array JSON con un objeto de esquema por cada objeto del inventario, en su orden. `TaxonomyException` si la taxonomía no contiene una clave del inventario. |
| `String objectJson(DataInventory inventory, String object)` | `String` | El objeto de esquema de un objeto: un objeto JSON con `name`, `logicalType` (`object`) y `properties`. |
| `String propertyJson(InventoryField field)` | `String` | La propiedad de esquema de un campo clasificado: un objeto JSON con `name`, `classification`, `tags` y `customProperties`. `IllegalArgumentException` si el campo está exento. `TaxonomyException` si la taxonomía no contiene su clave. |

## Uso

```java
import io.github.devbpm.linneo.export.odcs.OdcsSchema;

DataInventory inventory = classifier.inventory(List.of(Student.class), type -> "students");
String schema = OdcsSchema.of(taxonomy).schemaJson(inventory);
```

Para un `Student` con, entre otros, un `disabilityDegree` de `user.health_and_medical.disability` y un `passwordHash` de
`user.authorization.password`, `schema` es, con formato para leerlo (Linneo lo escribe en una sola línea):

```json
[
  {
    "name": "students",
    "logicalType": "object",
    "properties": [
      {
        "name": "disabilityDegree",
        "classification": "restricted",
        "tags": ["user.health_and_medical.disability", "personal-data", "special-category"],
        "customProperties": [
          {"property": "linneo.category", "value": "user.health_and_medical.disability"},
          {"property": "linneo.confidentiality", "value": "HIGH"},
          {"property": "linneo.integrity", "value": "HIGH"},
          {"property": "linneo.identifiability", "value": "IDENTIFIED"}
        ]
      },
      {
        "name": "passwordHash",
        "classification": "restricted",
        "tags": ["user.authorization.password", "personal-data", "secret"],
        "customProperties": [
          {"property": "linneo.category", "value": "user.authorization.password"},
          {"property": "linneo.confidentiality", "value": "HIGH"},
          {"property": "linneo.integrity", "value": "HIGH"},
          {"property": "linneo.identifiability", "value": "IDENTIFIED"}
        ]
      }
    ]
  }
]
```

Pega el array bajo `schema:` en el contrato, o escríbelo en un fichero que la compilación del contrato incorpore. Como
sale del inventario, el esquema sigue a la clasificación: regenéralo cuando
[cambie el inventario](../classify/inventory.md#inventorydiff), y una revisión verá el cambio de una clasificación como
un cambio del contrato.

Véase también: [Exportación](README.md) · [Inventario de datos](../classify/inventory.md) ·
[Niveles de protección](../protection-levels.md)
