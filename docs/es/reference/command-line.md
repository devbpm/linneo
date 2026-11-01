---
title: "Línea de comandos"
parent: "Referencia"
nav_order: 20
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/internal/Cli.java
  - pom.xml
-->

# Línea de comandos

El JAR de Linneo se ejecuta como un programa: `java -jar linneo.jar <command>`. Trabaja con las formas de texto que
escribe Linneo (una [taxonomía](taxonomy.md#la-taxonomía-como-datos), un
[inventario de datos](classify/inventory.md#formato-del-texto)) y con las exportaciones de catálogo, de modo que una
cadena de compilación o un delegado de protección de datos puedan usarlas sin escribir Java. Solo necesita un entorno
de ejecución de Java 21.

La línea de comandos **no forma parte de la API pública**: es una herramienta, sin punto de entrada Java al que
llamar, y esta página documenta los comandos, no el código que hay detrás.

```text
java -jar linneo.jar <command> [options]
```

La sintaxis es la que muestra `help`, que está en inglés; en ella `BEFORE.tsv` y `AFTER.tsv` son los dos ficheros que se
comparan, el anterior y el posterior, y `TAXONOMY.tsv` una taxonomía escrita por `taxonomy`.

| Comando | Qué hace |
|---|---|
| [`taxonomy`](#taxonomy) | Escribe la taxonomía estándar, con las extensiones incluidas que nombres, como texto. |
| [`taxonomy-diff`](#taxonomy-diff) | Lista los cambios entre dos taxonomías y el cambio de versión que necesitan. |
| [`inventory-diff`](#inventory-diff) | Lista los cambios de campos entre dos inventarios de datos. |
| [`catalog`](#catalog) | Escribe, como JSON, las peticiones que crean la taxonomía en OpenMetadata, DataHub o Apache Atlas. |
| `help` | Muestra el uso. `--help` y `-h` hacen lo mismo. |

## Códigos de salida y entrada

| Código | Significado |
|---|---|
| `0` | El comando tuvo éxito. Las diferencias se listan y siguen saliendo con `0` salvo que pidieras una comprobación. |
| `1` | Falló una comprobación que pediste: `--require-compatible` encontró un cambio incompatible, o `--fail-on-weakening` encontró un cambio que debilita la protección. |
| `2` | Un error de uso o de entrada: ningún comando, un comando, una extensión o una opción desconocidos, un número de argumentos incorrecto, falta `--taxonomy`, un fichero que no se puede leer o un texto que no es una taxonomía ni un inventario. El mensaje va a la salida de error, como `Error: ...` en un error de entrada, y tras un error de uso le sigue el uso. |

Los resultados van a la salida estándar en UTF-8. Los ficheros que pasas se leen como UTF-8, o en la codificación que
nombra su marca de orden de bytes: UTF-8 con marca, como lo escriben muchas herramientas de Windows, o UTF-16 (en
cualquier orden), que es lo que escribe la redirección de Windows PowerShell 5. Un fichero sin marca es UTF-8.

## taxonomy

```text
java -jar linneo.jar taxonomy [--with EXTENSION,...]
```

Escribe la taxonomía estándar en su forma de texto, la que lee `Taxonomy.fromTsv`. Con `--with` se aplican encima las
[extensiones](extensions/README.md) incluidas que nombres, separadas por comas: `university`, `finance`, `supply_chain`,
`project_operations` y `human_resources`. Un nombre desconocido es un error de uso (salida `2`) que lista los conocidos.

```bash
java -jar linneo.jar taxonomy > taxonomy.tsv
java -jar linneo.jar taxonomy --with university,finance > taxonomy-with-extensions.tsv
```

El texto es lo que los demás comandos toman como taxonomía: conserva la que aplicaste y compárala con una posterior.

## taxonomy-diff

```text
java -jar linneo.jar taxonomy-diff BEFORE.tsv AFTER.tsv [--require-compatible]
```

Compara dos taxonomías término a término, como hace [`TaxonomyDiff`](taxonomy-diff.md), y escribe una línea por cada
término cambiado y una línea de resumen con el número de cambios y el cambio de versión que necesitan según las reglas
de Linneo (`NONE`, `PATCH`, `MINOR` o `MAJOR`). Con `--require-compatible` sale con `1` cuando un cambio es incompatible
(un término eliminado, una protección más débil o una sustitución que cambió de sitio) y añade una línea que lo
dice. Sin la opción nada es un fallo.

```bash
java -jar linneo.jar taxonomy > current.tsv
java -jar linneo.jar taxonomy-diff applied.tsv current.tsv --require-compatible
```

```text
DATA_CATEGORY 'user.contact.address.state': [REMOVED]
1 change(s); required version change: MAJOR
Incompatible: 1 change(s) remove a term, weaken protection or move a replacement
```

(código de salida `1`.) Añadir a la taxonomía estándar los términos de la extensión `university` escribe una línea
`[ADDED]` por cada uno y termina con `71 change(s); required version change: MINOR`, código de salida `0`. Los mismos
dos ficheros dan `0 change(s); required version change: NONE`.

## inventory-diff

```text
java -jar linneo.jar inventory-diff BEFORE.tsv AFTER.tsv --taxonomy TAXONOMY.tsv [--fail-on-weakening]
```

Compara dos [inventarios de datos](classify/inventory.md), como hace
[`InventoryDiff`](classify/inventory.md#inventorydiff), bajo la taxonomía de `TAXONOMY.tsv`, que dice cómo se movió la
protección de cada campo: `--taxonomy` es obligatoria.
Escribe una línea por cada campo cambiado, marca los que debilitan la protección y termina con el número de cambios y
de debilitamientos. Con `--fail-on-weakening` sale con `1` si algún cambio debilita la protección, también un campo
clasificado que pasó a estar exento.

```bash
java -jar linneo.jar inventory-diff applied-inventory.tsv inventory.tsv --taxonomy taxonomy.tsv --fail-on-weakening
```

```text
CHANGED students.email LOOSENED: students.email exempt: always empty  <- weakens protection
ADDED students.phone: students.phone = user.contact.phone_number (DECLARED)
2 field change(s), 1 weakening
```

(código de salida `1`.)

## catalog

```text
java -jar linneo.jar catalog openmetadata|datahub|atlas [--taxonomy TAXONOMY.tsv]
```

Escribe, como JSON en la salida estándar, las peticiones que crean la taxonomía en un catálogo de datos, a partir de la
taxonomía estándar o de la de `TAXONOMY.tsv`. No hace llamadas de red: envía el JSON con el cliente HTTP que uses.

| Argumento | Escribe | Véase |
|---|---|---|
| `openmetadata` | Un objeto `{"classifications": [...], "tags": [...]}`, con los cuerpos de `PUT /api/v1/classifications` y `PUT /api/v1/tags`, primero los padres. | [Catálogo de OpenMetadata](openmetadata/catalog.md) |
| `datahub` | Un array con los cuerpos `ingestProposal` de los nodos y términos del glosario. | [Catálogo de DataHub](datahub/catalog.md) |
| `atlas` | El cuerpo del único `POST /api/atlas/v2/types/typedefs`, `{"classificationDefs": [...]}`. | [Catálogo de Apache Atlas](atlas/catalog.md) |

```bash
java -jar linneo.jar catalog atlas --taxonomy taxonomy.tsv > atlas-typedefs.json
```

## En una compilación

Los códigos de salida permiten que una cadena falle por lo que importa. Guarda en el repositorio la taxonomía y el
inventario que se aplicaron la última vez y compáralos con los actuales en un paso de la compilación:

```bash
java -jar linneo.jar taxonomy-diff released-taxonomy.tsv taxonomy.tsv --require-compatible
java -jar linneo.jar inventory-diff released-inventory.tsv inventory.tsv --taxonomy taxonomy.tsv --fail-on-weakening
```

El primero falla cuando la taxonomía perdió un término o debilitó una protección, el segundo cuando un campo del
inventario está menos protegido que antes. El fichero del inventario es lo que escribe
[`DataInventory.toTsv()`](classify/inventory.md#formato-del-texto); la biblioteca lo escribe a partir del código y el
comando solo lo lee.

Véase también: [Diferencias entre taxonomías](taxonomy-diff.md) · [Inventario de datos](classify/inventory.md) ·
[Referencia](README.md)
