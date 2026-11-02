---
title: "Taxonomía"
parent: "Referencia"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/Taxonomy.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyException.java
  - src/main/java/io/github/devbpm/linneo/Trait.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
-->

# Taxonomía

`io.github.devbpm.linneo.Taxonomy` — `public final class`, `@since 0.1.0`

Un conjunto inmutable de términos en tres jerarquías: categorías de datos ([`DataCategory`](terms.md#datacategory)),
usos de datos ([`DataUse`](terms.md#datause)) y sujetos de datos ([`DataSubject`](terms.md#datasubject)). Las
instancias son seguras entre hilos: construye una por aplicación y compártela. Todas las listas están ordenadas por
clave, de modo que un padre siempre aparece antes que sus descendientes.

No hay constructor público. Se obtiene una taxonomía con uno de los métodos de fábrica.

## Obtener una taxonomía

| Método | Devuelve | Descripción |
|---|---|---|
| `static Taxonomy standard()` | `Taxonomy` | La [taxonomía estándar](standard-taxonomy.md): la taxonomía por defecto de fideslang más las extensiones de Linneo, **sin** extensiones de la aplicación. Se crea una sola vez y se comparte: cada llamada devuelve la misma instancia. |
| `static Taxonomy load()` | `Taxonomy` | La taxonomía estándar más todas las [`TaxonomyExtension`](extending.md#taxonomyextension) registradas con `ServiceLoader`. Equivale a `builder().withStandard().withExtensions().build()`. Construye una **instancia nueva en cada llamada**: guarda el resultado. Lanza `TaxonomyException` si una extensión incumple las reglas de `TaxonomyBuilder`. |
| `static TaxonomyBuilder builder()` | `TaxonomyBuilder` | Un constructor nuevo y **vacío**. Llama a `withStandard()` para partir de los términos estándar. Consulta [Extender la taxonomía](extending.md). |

```java
Taxonomy standard = Taxonomy.standard();          // compartida, sin extensiones
Taxonomy taxonomy = Taxonomy.load();              // estándar + extensiones de ServiceLoader, instancia nueva
Taxonomy custom = Taxonomy.builder()
        .withStandard()
        .category("user.education.lab_notebook", "Lab Notebook", "Laboratory notebooks kept by the subject.")
        .build();
```

`toString()` informa de los tamaños; por ejemplo, `Taxonomy[126 data categories, 63 data uses, 22 data subjects]`
para la taxonomía estándar.

## Categorías de datos

| Método | Devuelve | Descripción |
|---|---|---|
| `List<DataCategory> categories()` | lista inmutable | Todas las categorías, ordenadas por clave. |
| `Optional<DataCategory> category(String key)` | `Optional` | Una categoría por clave, obsoleta o no; vacío si la taxonomía no la contiene. |
| `DataCategory requireCategory(String key)` | `DataCategory` | Una categoría por clave, obsoleta o no. Lanza `TaxonomyException` (`Unknown data category '<key>'`) si no existe. |
| `DataCategory resolveCategory(String key)` | `DataCategory` | La categoría **que se debe usar** para una clave: la propia categoría o, si está obsoleta y tiene sustituta, la categoría vigente que la sustituye (ver abajo). Lanza `TaxonomyException` si no existe la clave o alguna sustituta de la cadena. |
| `Optional<DataCategory> parentOf(DataCategory category)` | `Optional` | El padre; vacío para una raíz. |
| `List<DataCategory> childrenOf(DataCategory category)` | lista inmutable | Las hijas directas, ordenadas por clave. |
| `List<DataCategory> ancestorsOf(DataCategory category)` | lista no modificable | Los ancestros desde el padre hasta la raíz, **el más cercano primero**. Vacía para una raíz. |

### Obsolescencia y `resolveCategory`

Un término está obsoleto cuando su [`TermInfo`](terms.md#terminfo) tiene `deprecatedSince` o `replacedBy`.
`category` y `requireCategory` devuelven las categorías obsoletas sin cambios. `resolveCategory` sigue `replacedBy`
hasta llegar a una categoría sin sustituta:

- no obsoleta → la propia categoría;
- obsoleta con `replacedBy` → el final de la cadena de sustituciones (una sustituta puede estar a su vez obsoleta y
  sustituida);
- obsoleta sin `replacedBy` → la propia categoría obsoleta.

`TaxonomyBuilder.build()` garantiza que toda sustituta existe en la misma jerarquía y que las sustituciones no forman
ciclos, así que la cadena siempre termina, y que la sustituta de una categoría obsoleta no protege menos (naturaleza,
confidencialidad, integridad, secreto) que la categoría a la que sustituye. El [`Classifier`](classify/classifier.md)
usa `resolveCategory` con las claves obsoletas y emite un aviso `DEPRECATED_CATEGORY`.

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard()
        .category("user.contact.old_email", "Old email", "",
                c -> c.deprecated("1.0.0", Categories.USER_CONTACT_EMAIL))
        .build();
taxonomy.requireCategory("user.contact.old_email").info().isDeprecated();   // true
taxonomy.resolveCategory("user.contact.old_email").key();                   // "user.contact.email"
```

### Navegación

```java
DataCategory email = taxonomy.requireCategory(Categories.USER_CONTACT_EMAIL);
taxonomy.parentOf(email);        // la categoría user.contact
taxonomy.ancestorsOf(email);     // las categorías user.contact, user
taxonomy.childrenOf(taxonomy.requireCategory(Categories.USER_CONTACT));   // incluye user.contact.email
email.isA(Categories.USER_CONTACT);   // true  (Term.isA: misma clave o descendiente)
email.isA("user.con");                // false (solo segmentos completos)
```

## Usos de datos

| Método | Devuelve | Descripción |
|---|---|---|
| `List<DataUse> dataUses()` | lista inmutable | Todos los usos de datos, ordenados por clave. |
| `Optional<DataUse> dataUse(String key)` | `Optional` | Un uso por clave; vacío si no existe. |
| `DataUse requireDataUse(String key)` | `DataUse` | Un uso por clave. Lanza `TaxonomyException` (`Unknown data use '<key>'`) si no existe. |
| `DataUse resolveDataUse(String key)` | `DataUse` | El uso de datos **que hay que usar** para una clave: el propio uso o, si está obsoleto con una sustituta, el uso vigente que lo sustituye. Lanza `TaxonomyException` si la clave, o una sustituta de la cadena, no existe. |
| `List<DataUse> childrenOf(DataUse use)` | lista inmutable | Los hijos directos, ordenados por clave. |

## Sujetos de datos

| Método | Devuelve | Descripción |
|---|---|---|
| `List<DataSubject> dataSubjects()` | lista inmutable | Todos los sujetos de datos, ordenados por clave. |
| `Optional<DataSubject> dataSubject(String key)` | `Optional` | Un sujeto por clave; vacío si no existe. |
| `DataSubject requireDataSubject(String key)` | `DataSubject` | Un sujeto por clave. Lanza `TaxonomyException` (`Unknown data subject '<key>'`) si no existe. |
| `DataSubject resolveDataSubject(String key)` | `DataSubject` | El sujeto de datos **que hay que usar** para una clave: el propio sujeto o, si está obsoleto con una sustituta, el sujeto vigente que lo sustituye. Lanza `TaxonomyException` si la clave, o una sustituta de la cadena, no existe. |

`resolveDataUse` y `resolveDataSubject` siguen `replacedBy` igual que `resolveCategory`, así que una clave guardada
antes de que su término quedara obsoleto sigue llegando al término vigente: [`SubjectRights`](processing/subject-rights.md)
resuelve el sujeto de datos con el segundo. `parentOf` y `ancestorsOf` solo existen para categorías de datos; para usos
y sujetos usa `info().parent()` y las búsquedas anteriores. Los usos y los sujetos describen actividades de
tratamiento, no campos, por lo que ninguna anotación se refiere a ellos.

## La taxonomía como datos

Una taxonomía se puede escribir como texto y volver a leer. Una aplicación que guarda claves conserva la taxonomía que
aplicó, de modo que en el siguiente arranque puede saber qué cambió con [`TaxonomyDiff`](taxonomy-diff.md), y un
auditor puede reproducir la clasificación del pasado con la taxonomía del pasado.

| Método | Devuelve | Descripción |
|---|---|---|
| `String toTsv()` | `String` | La taxonomía como texto separado por tabuladores: cada término con su ciclo de vida y, en las categorías de datos, su protección y sus atributos **efectivos**, y los [rasgos](terms.md#trait) de cada término,
por orden de clave. Determinista: la misma taxonomía da siempre el mismo texto. |
| `static Taxonomy fromTsv(String text)` | `Taxonomy` | Lee un texto escrito por `toTsv()`. Los términos pasan por un `TaxonomyBuilder`, así que el texto debe cumplir las mismas reglas que cualquier otra taxonomía. Lanza `TaxonomyException` si el texto no es una taxonomía de una versión de formato conocida, una fila está mal formada o los términos incumplen una regla del constructor. |
| `String fingerprint()` | `String` | El SHA-256 de `toTsv()` (UTF-8), como 64 dígitos hexadecimales en minúscula. Las taxonomías iguales tienen la misma huella; se calcula una vez y se conserva. |
| `boolean equals(Object)`, `int hashCode()` | | Dos taxonomías son iguales cuando tienen las mismas categorías, usos y sujetos de datos: mismas claves, nombres, descripciones, orígenes, ciclos de vida, protección y atributos. |

La huella dice que algo cambió, no qué: cualquier cambio de texto, incluso una descripción, la modifica. Para saber
qué cambió usa [`TaxonomyDiff`](taxonomy-diff.md).

### Formato del texto

La primera línea es `#linneo-taxonomy`, un tabulador y la versión del formato, `1`; `fromTsv` rechaza cualquier otra
primera línea. Las líneas en blanco y las que empiezan por `#` a continuación son comentarios; `toTsv` escribe cuatro
que nombran las columnas. Cualquier otra línea es una fila de celdas separadas por tabuladores, y la primera celda dice de
qué fila se trata:

| Fila | Celdas |
|---|---|
| `category` | `category`, clave, nombre, descripción, origen, versión que la introdujo, versión que la declaró obsoleta, sustituta, naturaleza, confidencialidad, integridad, secreto (`true` o `false`): 12 celdas |
| `use`, `subject` | el tipo, clave, nombre, descripción, origen, versión que lo introdujo, versión que lo declaró obsoleto, sustituto: 8 celdas |
| `attribute` | `attribute`, clave de la categoría, nombre, valor: 4 celdas. Una fila por cada atributo de una categoría, incluidos los heredados de sus ancestros |
| `trait` | `trait`, el tipo de término (`category`, `use` o `subject`), clave, rasgo: 4 celdas. Una fila por cada rasgo de un término, incluidos los heredados de sus ancestros |

Las filas se escriben en este orden: categorías, usos y sujetos de datos, cada grupo por clave, después los atributos
por clave de categoría y nombre, y después los rasgos de las categorías, de los usos y de los sujetos. Una celda vacía
es un valor ausente. Una barra invertida, un tabulador, un salto de línea y un retorno de carro dentro de una celda se
escriben `\\`, `\t`, `\n` y `\r`, de modo que una fila es siempre una línea; un sustituto UTF-16 suelto, que UTF-8 no
puede codificar, se escribe como una barra invertida, `u` y sus cuatro dígitos hexadecimales, para que dos taxonomías
distintas nunca tengan los mismos bytes ni la misma huella. La naturaleza, la confidencialidad y la integridad son los
nombres de las constantes de [`LegalNature`, `Confidentiality` e `Integrity`](protection-levels.md), y el rasgo es el
nombre de un [`Trait`](terms.md#trait). `fromTsv` lee las filas en cualquier orden: los padres se comprueban al
construir la taxonomía y las filas de atributos y de rasgos se aplican después de todos los términos.

```java
Taxonomy taxonomy = Taxonomy.builder()
        .origin("my-app")
        .category("data", "Data", "Any data.",
                c -> c.nature(LegalNature.ORDINARY).confidentiality(Confidentiality.LOW).integrity(Integrity.LOW))
        .category("data.note", "Note", "A free text note.", c -> c.attribute("retention", "1y"))
        .build();

String text = taxonomy.toTsv();
Taxonomy read = Taxonomy.fromTsv(text);
read.equals(taxonomy);                       // true
read.fingerprint().equals(taxonomy.fingerprint());   // true
```

`text` es, con `→` en lugar de un tabulador:

```text
#linneo-taxonomy→1
# category key name description origin since deprecated_since replaced_by nature confidentiality integrity secret
# use|subject key name description origin since deprecated_since replaced_by
# attribute category_key name value
# trait category|use|subject key trait
category→data→Data→Any data.→my-app→→→→ORDINARY→LOW→LOW→false
category→data.note→Note→A free text note.→my-app→→→→ORDINARY→LOW→LOW→false
attribute→data.note→retention→1y
```

## TaxonomyException

`io.github.devbpm.linneo.TaxonomyException` — `public class TaxonomyException extends RuntimeException`,
`@since 0.1.0`

Se lanza cuando no se puede construir una taxonomía o una clave no forma parte de ella. El mensaje nombra la clave
implicada.

| Constructor | Descripción |
|---|---|
| `TaxonomyException(String message)` | Crea la excepción. |
| `TaxonomyException(String message, Throwable cause)` | Crea la excepción con su causa. |

La lanzan:

- `Taxonomy.requireCategory`, `requireDataUse`, `requireDataSubject` y los tres métodos `resolve` ante una clave
  desconocida;
- `Taxonomy.load()` y los métodos de `TaxonomyBuilder` cuando se incumple una regla (mensajes en
  [Extender la taxonomía](extending.md#errores));
- `Taxonomy.fromTsv` cuando el texto está mal formado o sus términos incumplen una regla del constructor;
- [`TaxonomyDiff.requireCompatible()`](taxonomy-diff.md#taxonomydiff) cuando un cambio es incompatible;
- el cargador de la taxonomía estándar cuando falta un recurso incluido o no se puede leer.

Véase también: [Términos](terms.md) · [Niveles de protección](protection-levels.md) ·
[Taxonomía estándar](standard-taxonomy.md) · [Extender la taxonomía](extending.md) ·
[Diferencias entre taxonomías](taxonomy-diff.md) · curso: [La taxonomía](../course/02-the-taxonomy.md)
