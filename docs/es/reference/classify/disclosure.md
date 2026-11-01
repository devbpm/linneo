---
title: "Política de divulgación"
parent: "Referencia"
nav_order: 11.2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/DisclosurePolicy.java
-->

# Política de divulgación

`io.github.devbpm.linneo.classify.DisclosurePolicy` — `public final class`, `@since 0.1.0`

Cómo pueden salir del sistema los valores de los campos clasificados por un canal, como un log, una exportación, una
copia para un entorno de pruebas o una API: tal cual, **enmascarados** o **omitidos**. Linneo decide; la aplicación
enmascara u omite los valores, porque Linneo nunca los lee. Las instancias son inmutables y seguras entre hilos.

## Treatment

`DisclosurePolicy.Treatment` — `enum` anidado, `@since 0.1.0`: qué ocurre con un valor, del más abierto al más
estricto.

| Valor | Significado |
|---|---|
| `CLEAR` | Se muestra tal cual. |
| `MASK` | Se muestra de una forma que no lo revela: sustituido por asteriscos, truncado, con un hash o sustituido por un seudónimo, como decida la aplicación. |
| `OMIT` | Se omite. |

## Cómo decide una política

Una política es un conjunto de **umbrales** en el [orden parcial de la protección](../protection-levels.md#protection):
un campo cuya protección [cubre](../protection-levels.md#protection) un umbral recibe al menos el tratamiento de ese
umbral, y se aplica el tratamiento más estricto.

- Una política tiene cualquier número de umbrales `mask` y `omit`. Un valor que necesita una protección que no cubre
  ninguno es `CLEAR`.
- **Un campo que contiene un secreto es `OMIT` en toda política**, incluso en una sin umbrales: un secreto nunca se
  lee de vuelta, ni se exporta ni se publica.
- **Más protección nunca recibe un tratamiento más laxo.** Sean cuales sean los umbrales de una política, si la
  protección de un campo cubre la de otro, su tratamiento es al menos igual de estricto. Una regla escrita a mano con
  unos pocos indicadores, como «secretos omitidos, datos personales enmascarados, el resto en claro», no da esa
  garantía: un campo que necesita más protección en algo que la regla no mira, como una confidencialidad más alta, se
  trata igual que uno que necesita menos.

La protección que se compara es la del campo: [`FieldClassification.protection()`](results.md#métodos) en un campo
clasificado, [`InventoryField.protection(taxonomy)`](inventory.md#inventoryfield) en uno guardado. La naturaleza de un
valor que no es dato personal (un valor anonimizado o agregado de una categoría personal) es `NONE`, así que sigue a
su confidencialidad.

## Miembros

| Miembro | Devuelve | Descripción |
|---|---|---|
| `static DisclosurePolicy forLogs()` | política | La política para logs, trazas e informes de error; véase [Para logs](#para-logs). |
| `static Builder builder()` | [`Builder`](#builder) | Empieza una política sin ningún umbral: solo se omiten los secretos. |
| `List<Protection> masked()` | lista inmutable | Los umbrales desde los que se enmascara un valor. |
| `List<Protection> omitted()` | lista inmutable | Los umbrales desde los que se omite un valor, además de los secretos. |
| `Treatment treatment(Protection protection)` | `Treatment` | El tratamiento más estricto cuyo umbral cubre la protección; `OMIT` para un secreto. `NullPointerException` si `protection` es `null`. |
| `Treatment treatment(FieldClassification field)` | `Treatment` | El tratamiento de la [`protection()`](results.md#métodos) del campo. |
| `Treatment treatment(InventoryField field, Taxonomy taxonomy)` | `Treatment` | El tratamiento de un campo guardado, resuelto con la taxonomía; `CLEAR` en un campo exento, que no contiene datos. `TaxonomyException` si la taxonomía no contiene la clave. |
| `Map<String, Treatment> treatments(ClassClassification classification)` | mapa inmutable | El tratamiento de cada campo de una clase por ruta: los campos clasificados en orden y luego los exentos, que no contienen datos y son `CLEAR`. `IllegalArgumentException` si la clase tiene errores. |

`treatments` lanza una excepción en una clase con errores porque un campo con error no está en `fields()` y no tiene
tratamiento: dejarlo fuera del mapa lo dejaría pasar. Construye el mapa una vez por clase y consérvalo.

`toString()` es `DisclosurePolicy[mask from <umbrales>, omit from <umbrales> and secrets]`.

## Builder

`DisclosurePolicy.Builder` — `public static final class`, `@since 0.1.0`. Lo crea `DisclosurePolicy.builder()`; no
tiene constructor público.

| Miembro | Descripción |
|---|---|
| `Builder mask(Protection atLeast)` | Enmascara todo valor cuya protección cubre `atLeast`. `NullPointerException` si es `null`. |
| `Builder omit(Protection atLeast)` | Omite todo valor cuya protección cubre `atLeast`. `NullPointerException` si es `null`. |
| `DisclosurePolicy build()` | Construye la política inmutable. |

Un umbral es una `Protection`, como `Protection.of(LegalNature.ORDINARY, Confidentiality.PUBLIC, Integrity.LOW,
false)` para cualquier dato personal. Por ejemplo, una política para una exportación que enmascara los datos
personales y omite los de categoría especial:

```java
DisclosurePolicy export = DisclosurePolicy.builder()
        .mask(Protection.of(LegalNature.ORDINARY, Confidentiality.PUBLIC, Integrity.LOW, false))
        .omit(Protection.of(LegalNature.SPECIAL_CATEGORY, Confidentiality.PUBLIC, Integrity.LOW, false))
        .build();
```

## Para logs

`DisclosurePolicy.forLogs()` es una política para logs, trazas e informes de error:

| El campo contiene | Tratamiento |
|---|---|
| datos personales (un umbral de `ORDINARY`) | `MASK` |
| datos de confidencialidad `HIGH`, personales o no | `MASK` |
| datos de categoría especial | `OMIT` |
| datos de condenas e infracciones penales | `OMIT` |
| un secreto | `OMIT` |
| cualquier otra cosa | `CLEAR` |

Se aplica el más estricto: los datos de categoría especial, de cualquier nivel, son `OMIT` y no `MASK`.

## Uso

```java
DisclosurePolicy logs = DisclosurePolicy.forLogs();
Map<String, DisclosurePolicy.Treatment> treatments = logs.treatments(classifier.classify(Student.class));

for (Map.Entry<String, DisclosurePolicy.Treatment> entry : treatments.entrySet()) {
    switch (entry.getValue()) {
        case CLEAR -> System.out.println(entry.getKey() + " shown");
        case MASK -> System.out.println(entry.getKey() + " masked");
        case OMIT -> System.out.println(entry.getKey() + " left out");
    }
}

DisclosurePolicy.Treatment stored = logs.treatment(inventoryField, taxonomy);   // un campo de un inventario de datos
```

La aplicación lee los valores (con getters, un filtro del serializador o un conversor del logging) y aplica el
tratamiento de cada ruta; Linneo nunca los toca. Cómo se ve un valor `MASK` también lo decide la aplicación.

Véase también: [Resultados](results.md) · [Inventario de datos](inventory.md) ·
[Niveles de protección](../protection-levels.md) · curso:
[Usar la clasificación](../../course/05-using-the-classification.md)
