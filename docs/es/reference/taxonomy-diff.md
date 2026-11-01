---
title: "Diferencias entre taxonomías"
parent: "Referencia"
nav_order: 6.5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/TaxonomyDiff.java
  - src/main/java/io/github/devbpm/linneo/TermChange.java
  - pom.xml
-->

# Diferencias entre taxonomías

`io.github.devbpm.linneo.TaxonomyDiff` — `public final class`, `@since 0.1.0`

`io.github.devbpm.linneo.TermChange` — `public record`, `@since 0.1.0`

Las diferencias entre dos versiones de una taxonomía, término a término. Las aplicaciones las usan cuando la taxonomía
con la que se ejecutan no es la que aplicaron por última vez: guardan la aplicada con
[`Taxonomy.toTsv()`](taxonomy.md#la-taxonomía-como-datos), la leen con `Taxonomy.fromTsv(String)` y comparan. Cada
`TermChange` dice qué cambió en un término y, en una categoría de datos, cómo se movió su protección. Con ello una
aplicación puede:

- **migrar** las claves que guarda, por ejemplo una clave obsoleta a su sustituta;
- **registrar una entrada de auditoría por término modificado**, en lugar de una por campo;
- **negarse a arrancar** ante un cambio que no puede aceptar, con `requireCompatible()`.

La misma comparación gradúa los cambios de la propia taxonomía de Linneo según sus
[reglas de versionado](#reglas-de-versionado). Las instancias de ambos tipos son inmutables.

## TaxonomyDiff

| Miembro | Devuelve | Descripción |
|---|---|---|
| `static TaxonomyDiff between(Taxonomy before, Taxonomy after)` | `TaxonomyDiff` | Compara dos versiones: `before` es la anterior, como la que una aplicación aplicó por última vez, y `after` la posterior, como con la que se ejecuta ahora. Los términos se emparejan por clave dentro de cada jerarquía. `NullPointerException` si alguna es `null`. |
| `List<TermChange> changes()` | lista inmutable | Los cambios: primero las categorías de datos, después los usos y los sujetos de datos, cada grupo por clave. Un término que no cambió no aparece. |
| `boolean isEmpty()` | `boolean` | `true` si las dos versiones tienen los mismos términos. |
| `TermChange change(TermChange.Hierarchy hierarchy, String key)` | `TermChange` | El cambio de un término, o `null` si el término no cambió (o no está en ninguna de las dos versiones). |
| `List<TermChange> breakingChanges()` | lista inmutable | Los cambios que son [`isBreaking()`](#termchange): incompatibles para las aplicaciones que guardan o publican claves. |
| `TaxonomyDiff.Bump requiredBump()` | `Bump` | El cambio de versión que exigen las diferencias: el `bump()` más estricto de los cambios, `Bump.NONE` si no hay ninguno. |
| `TaxonomyDiff requireCompatible()` | `TaxonomyDiff` | Devuelve este diff si no tiene ningún cambio incompatible. En caso contrario lanza `TaxonomyException` con todos ellos: `<n> incompatible taxonomy change(s):` seguido de una línea ` - <change>` por cada uno. |

`toString()` informa del tamaño y del salto de versión, por ejemplo `TaxonomyDiff[3 change(s), MINOR]`.

### Bump

`TaxonomyDiff.Bump` — `public enum`, `@since 0.1.0`. Un cambio de versión según el versionado semántico, tal como
Linneo lo aplica a las taxonomías. Las constantes se declaran de menor a mayor, así que `compareTo` las ordena.

| Constante | Significado |
|---|---|
| `NONE` | Ningún cambio. |
| `PATCH` | Solo cambios descriptivos: nombres, descripciones, atributos, orígenes, versiones del ciclo de vida. |
| `MINOR` | Cambios compatibles: términos añadidos, protección más estricta, declaraciones de obsolescencia, una sustituta para un término obsoleto que no tenía. |
| `MAJOR` | Cambios incompatibles: términos eliminados, protección más débil o mixta, un término obsoleto que ahora se resuelve a otra sustituta. |

## TermChange

Cómo difiere un término entre dos versiones. Los componentes del registro son:

| Componente | Descripción |
|---|---|
| `Hierarchy hierarchy()` | La jerarquía del término. |
| `String key()` | La clave del término. |
| `Term before()` | El término en la versión anterior, o `null` si se añadió. |
| `Term after()` | El término en la versión posterior, o `null` si se eliminó. |
| `Set<Aspect> aspects()` | Qué cambió, nunca vacío. Inmutable. |
| `Protection.Direction direction()` | Cómo cambió la protección de una categoría de datos: la [`Direction`](protection-levels.md#direction) de la protección posterior respecto de la anterior. `SAME` para usos y sujetos de datos y para términos añadidos o eliminados. |

El constructor canónico es público y comprueba sus argumentos: `NullPointerException` si la jerarquía, la clave o la
dirección son `null`; `IllegalArgumentException` si los aspectos están vacíos o si `before` y `after` son ambos `null`.
Copia los aspectos en un conjunto inmutable.

| Método | Descripción |
|---|---|
| `TaxonomyDiff.Bump bump()` | El cambio de versión que exige este cambio: el salto más estricto de sus aspectos (tabla siguiente). |
| `boolean isBreaking()` | Si el cambio es incompatible para las aplicaciones que guardan o publican la clave: `bump()` es `MAJOR`. |

`toString()` nombra la jerarquía, la clave, los aspectos y, cuando no es `SAME`, la dirección; por ejemplo
`DATA_CATEGORY 'data.a': [PROTECTION] LOOSENED`.

### Hierarchy

`TermChange.Hierarchy` — `public enum`, `@since 0.1.0`: `DATA_CATEGORY` ([`DataCategory`](terms.md#datacategory)),
`DATA_USE` ([`DataUse`](terms.md#datause)) y `DATA_SUBJECT` ([`DataSubject`](terms.md#datasubject)).

### Aspect

`TermChange.Aspect` — `public enum`, `@since 0.1.0`. Qué cambió en un término. Un cambio tiene uno o más aspectos, y su
`bump()` es el más estricto de los suyos.

| Aspecto | Se da cuando | Salto |
|---|---|---|
| `ADDED` | El término existe solo en la versión posterior. | `MINOR` |
| `REMOVED` | El término existe solo en la versión anterior. Las aplicaciones que guardaron su clave ya no pueden resolverla. | `MAJOR` |
| `PROTECTION` | Difiere la protección de una categoría de datos: su naturaleza, confidencialidad, integridad o marca de secreto, tal como se resuelven al construir. `direction()` dice cómo. | `MAJOR` si la dirección es `LOOSENED` o `MIXED` (`Direction.weakens()`), `MINOR` si es `TIGHTENED` |
| `DEPRECATION` | El término se declaró obsoleto, o ya no lo está. | `MINOR` |
| `REPLACEMENT` | Se fijó o cambió la clave que sustituye a un término obsoleto, de modo que el término se resuelve a otro. | `MAJOR` si el término ya tenía sustituta, `MINOR` si no tenía |
| `NAME` | Difiere el nombre. | `PATCH` |
| `DESCRIPTION` | Difiere la descripción. Es solo texto, pero una descripción que dice otra cosa puede ocultar un cambio de significado, lo que nunca se permite: revísala. | `PATCH` |
| `ATTRIBUTES` | Difieren los atributos de una categoría de datos. | `PATCH` |
| `ORIGIN` | Difiere el origen, por ejemplo cuando un término pasa de una extensión a la taxonomía estándar. | `PATCH` |
| `VERSIONS` | Difiere la versión que introdujo el término, o la versión que lo declaró obsoleto sin que cambie el estado de obsolescencia. | `PATCH` |

Un cambio mixto merece la revisión de un debilitamiento: más estricto en una faceta y más débil en otra, o una
naturaleza que no es comparable con la anterior, como unos datos de categoría especial que pasan a ser datos de
condenas e infracciones penales.

## Uso

Conserva la taxonomía que aplicó la aplicación y compárala en el siguiente arranque:

```java
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyDiff;
import io.github.devbpm.linneo.TermChange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

static Taxonomy start(Path appliedFile) throws IOException {
    Taxonomy taxonomy = Taxonomy.load();
    if (Files.exists(appliedFile)) {
        Taxonomy applied = Taxonomy.fromTsv(Files.readString(appliedFile));
        TaxonomyDiff diff = TaxonomyDiff.between(applied, taxonomy);
        diff.requireCompatible();                  // TaxonomyException: término eliminado, protección más débil, ...
        for (TermChange change : diff.changes()) {
            System.out.println("Taxonomy change: " + change);   // una entrada de auditoría por término modificado
        }
    }
    Files.writeString(appliedFile, taxonomy.toTsv());   // la taxonomía aplicada a partir de ahora
    return taxonomy;
}
```

Una clave que se declaró obsoleta desde el último arranque es un cambio `DEPRECATION` de su categoría. Los valores
guardados que todavía la contienen se pueden migrar a la categoría a la que se resuelve:

```java
import io.github.devbpm.linneo.TermChange.Aspect;
import io.github.devbpm.linneo.TermChange.Hierarchy;

for (TermChange change : diff.changes()) {
    if (change.hierarchy() == Hierarchy.DATA_CATEGORY && change.aspects().contains(Aspect.DEPRECATION)
            && change.after().info().isDeprecated()) {
        String current = taxonomy.resolveCategory(change.key()).key();
        if (!current.equals(change.key())) {
            // actualiza las filas guardadas que contienen change.key() a current
        }
    }
}
```

Aquí `change.after()` nunca es `null`: un cambio `DEPRECATION` solo existe para un término que está en las dos
versiones. Como una sustituta nunca protege menos que la categoría obsoleta a la que sustituye (ver
[Extender la taxonomía](extending.md#validación-en-build)), migrar una clave nunca baja la protección de los datos.

Una taxonomía que extiendes tú puede usar la misma comprobación para seguir siendo compatible; consulta
[Extender la taxonomía](extending.md#mantener-compatible-tu-extensión).

## Reglas de versionado

Las aplicaciones guardan y publican las claves de la taxonomía, así que Linneo la versiona con versionado semántico y
estas reglas, que aplica `requiredBump()`:

- **Mayor** (`MAJOR`): eliminar un término, debilitar la protección de una categoría en cualquier faceta, o cambiar a
  qué se resuelve una clave obsoleta.
- **Menor** (`MINOR`): añadir un término, endurecer la protección, declarar obsoleto un término, o dar a un término
  obsoleto su primera sustituta.
- **Parche** (`PATCH`): cambios descriptivos (nombres, descripciones, atributos, orígenes, versiones del ciclo de vida).

Dos reglas no las comprueba ningún programa: nunca cambiar el significado de una clave, y nunca reutilizar una clave
eliminada. Un cambio `DESCRIPTION` se informa para que alguien revise que el significado se mantiene.

La propia compilación de Linneo comprueba estas reglas. Cada versión publicada conserva una copia de la taxonomía que
distribuye, la taxonomía estándar con todas las extensiones incluidas, en
`src/test/resources/io/github/devbpm/linneo/released/<version>.tsv`, escrita con `toTsv()`. `ReleasedTaxonomyTest`,
que recibe de Maven la versión del proyecto como propiedad del sistema `linneo.version`, compara la taxonomía actual
con la copia de la última versión publicada y falla cuando `requiredBump()` es mayor de lo que permite el salto de
versión: el primero de mayor, menor y parche que difiere entre la versión publicada y la actual. Por ejemplo, un
término eliminado hace fallar la compilación en una versión menor o de parche. Mientras no exista ninguna copia, la
prueba se omite.

Véase también: [Taxonomía](taxonomy.md#la-taxonomía-como-datos) · [Niveles de protección](protection-levels.md#direction) ·
[Extender la taxonomía](extending.md) · [Taxonomía estándar](standard-taxonomy.md)
