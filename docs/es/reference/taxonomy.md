---
title: "Taxonomía"
parent: "Referencia"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/Taxonomy.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyException.java
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

`toString()` informa de los tamaños; por ejemplo, `Taxonomy[125 data categories, 63 data uses, 21 data subjects]`
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
ciclos, así que la cadena siempre termina, y que la sustituta de una categoría obsoleta no protege menos
(naturaleza, confidencialidad, integridad, secreto) que la categoría a la que sustituye. El [`Classifier`](classify/classifier.md) usa `resolveCategory` con las
claves obsoletas y emite un aviso `DEPRECATED_CATEGORY`.

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
| `List<DataUse> childrenOf(DataUse use)` | lista inmutable | Los hijos directos, ordenados por clave. |

## Sujetos de datos

| Método | Devuelve | Descripción |
|---|---|---|
| `List<DataSubject> dataSubjects()` | lista inmutable | Todos los sujetos de datos, ordenados por clave. |
| `Optional<DataSubject> dataSubject(String key)` | `Optional` | Un sujeto por clave; vacío si no existe. |
| `DataSubject requireDataSubject(String key)` | `DataSubject` | Un sujeto por clave. Lanza `TaxonomyException` (`Unknown data subject '<key>'`) si no existe. |

`parentOf`, `ancestorsOf` y `resolveCategory` solo existen para categorías de datos; para usos y sujetos usa
`info().parent()` y las búsquedas anteriores. Los usos y los sujetos describen actividades de tratamiento, no campos,
por lo que ninguna anotación se refiere a ellos.

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

- `Taxonomy.requireCategory`, `requireDataUse`, `requireDataSubject` y `resolveCategory` ante una clave
  desconocida;
- `Taxonomy.load()` y los métodos de `TaxonomyBuilder` cuando se incumple una regla (mensajes en
  [Extender la taxonomía](extending.md#errores));
- el cargador de la taxonomía estándar cuando falta un recurso incluido o no se puede leer.

Véase también: [Términos](terms.md) · [Niveles de protección](protection-levels.md) ·
[Taxonomía estándar](standard-taxonomy.md) · [Extender la taxonomía](extending.md) ·
curso: [La taxonomía](../course/02-the-taxonomy.md)
