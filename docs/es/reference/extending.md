---
title: "Extender la taxonomía"
parent: "Referencia"
nav_order: 6
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/TaxonomyBuilder.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyExtension.java
  - src/main/java/io/github/devbpm/linneo/CategoryDraft.java
  - src/main/java/io/github/devbpm/linneo/TermDraft.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyDiff.java
  - src/main/java/io/github/devbpm/linneo/Trait.java
-->

# Extender la taxonomía

Las aplicaciones añaden términos, y endurecen la protección de los existentes, directamente sobre un `TaxonomyBuilder` o
mediante una `TaxonomyExtension` que `Taxonomy.load()` descubre con `ServiceLoader`. Linneo incluye además extensiones
opcionales para un sector, como la [extensión universitaria](extensions/university.md): un ejemplo completo de
`TaxonomyExtension` con sus constantes de claves (ver [Extensiones](extensions/README.md), que enumera las cinco
incluidas).

## TaxonomyBuilder

`io.github.devbpm.linneo.TaxonomyBuilder` — `public final class`, `@since 0.1.0`

El único lugar donde se montan y validan las taxonomías. El constructor es de paquete: obtén un constructor con
`Taxonomy.builder()` (vacío). No es seguro entre hilos y está pensado para usarse una sola vez.

| Miembro | Descripción |
|---|---|
| `static final String DEFAULT_ORIGIN = "application"` | Origen que se registra para los términos añadidos fuera de cualquier extensión. |
| `TaxonomyBuilder withStandard()` | Añade la [taxonomía estándar](standard-taxonomy.md) con su política de protección (orígenes `fideslang` y `linneo`). Llamarlo de nuevo no tiene efecto. Restaura después el origen vigente. |
| `TaxonomyBuilder withExtensions()` | Aplica todas las `TaxonomyExtension` que encuentra `ServiceLoader` con el cargador de clases de contexto del hilo, o con el cargador de `TaxonomyBuilder` si no hay ninguno. |
| `TaxonomyBuilder withExtensions(ClassLoader classLoader)` | Aplica todas las extensiones que `ServiceLoader` encuentra en `classLoader`, **ordenadas por `name()`**, para que el resultado no dependa del orden del class path. |
| `TaxonomyBuilder extend(TaxonomyExtension extension)` | Aplica una extensión: llama a `contribute(this)` con su `name()` como origen. Aplicar de nuevo la misma **clase** de extensión **no tiene efecto**, de modo que una extensión puede aplicarse de forma explícita y estar además registrada como servicio. Mientras contribuye, el constructor aplica las [reglas para extensiones](#qué-puede-hacer-una-extensión). Lanza `TaxonomyException` si el nombre es `null`, está en blanco, o es uno de los reservados `"fideslang"`, `"linneo"` y `"application"` (el origen de los términos añadidos fuera de una extensión), o si ya se aplicó una clase **distinta** con el mismo nombre (`The extensions <primera clase> and <segunda clase> are both named '<nombre>'`); `NullPointerException` si `extension` es `null`. |
| `TaxonomyBuilder origin(String origin)` | Fija el origen que se registra para los términos que se añadan a partir de ahora (por defecto `"application"`). `NullPointerException` si es `null`; `TaxonomyException` si está en blanco, o si lo llama una extensión (los términos que añade una extensión llevan su nombre). |
| `TaxonomyBuilder category(String key, String name, String description)` | Añade una categoría de datos que hereda toda su protección de su padre. |
| `TaxonomyBuilder category(String key, String name, String description, Consumer<CategoryDraft> customizer)` | Añade una categoría y declara sus facetas, rasgos, atributos y ciclo de vida en un [`CategoryDraft`](#categorydraft). Lanza `TaxonomyException` si una extensión añade una categoría raíz. |
| `TaxonomyBuilder refineCategory(String key, Consumer<CategoryDraft> customizer)` | Eleva la protección de una categoría existente, y la de las categorías que cuelgan de ella, o le añade rasgos, atributos o una obsolescencia. Toda faceta es un **mínimo**: un nivel inferior al declarado no cambia nada, y dos refinamientos de una misma categoría dan el nivel más estricto en cualquier orden. Lanza `TaxonomyException` si la categoría no existe, si una naturaleza no puede conciliarse con la declarada (personal y no personal, o categoría especial e infracciones penales), o si una extensión cambia el ciclo de vida de una categoría que no añadió. |
| `TaxonomyBuilder refineDataUse(String key, Consumer<TermDraft> customizer)` | Añade rasgos o una obsolescencia a un uso de datos existente. Lanza `TaxonomyException` si no existe, o si una extensión cambia el ciclo de vida de un uso que no añadió. |
| `TaxonomyBuilder refineDataSubject(String key, Consumer<TermDraft> customizer)` | Lo mismo para un sujeto de datos existente. |
| `TaxonomyBuilder dataUse(String key, String name, String description)` | Añade un uso de datos. |
| `TaxonomyBuilder dataUse(String key, String name, String description, Consumer<TermDraft> customizer)` | Añade un uso de datos y declara sus rasgos y su ciclo de vida en un [`TermDraft`](#termdraft). |
| `TaxonomyBuilder dataSubject(String key, String name, String description)` | Añade un sujeto de datos. |
| `TaxonomyBuilder dataSubject(String key, String name, String description, Consumer<TermDraft> customizer)` | Añade un sujeto de datos y declara sus rasgos y su ciclo de vida. |
| `Taxonomy build()` | Comprueba las jerarquías, resuelve las facetas heredadas y devuelve la `Taxonomy` inmutable. |

### Claves

Una clave es una ruta de segmentos en minúsculas separados por puntos:

```text
[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*
```

`category`, `dataUse` y `dataSubject` comprueban la clave **en el momento**: una clave `null` lanza
`NullPointerException`; una clave no válida, o ya presente en la misma jerarquía, lanza `TaxonomyException`. `name`
no puede ser `null`; una descripción `null` se guarda como `""`. El padre es la clave sin su último segmento y debe
existir en la misma jerarquía, pero los términos pueden añadirse en cualquier orden: los padres se comprueban en
`build()`.

### Validación en `build()`

`build()` procesa las categorías empezando por los padres (por profundidad y luego por clave) y exige:

1. Que el padre de todo término no raíz exista en la misma jerarquía.
2. Que una categoría raíz declare `nature`, `confidentiality` e `integrity`.
3. Que una categoría hija herede toda faceta que no declara, y que toda faceta declarada sea un **mínimo**: una
   `confidentiality` o una `integrity` inferior a la del padre se eleva a la del padre, así que elevar una categoría
   eleva todo lo que cuelga de ella. La `nature` es la del padre, o precisa datos personales ordinarios como categoría
   especial o infracciones penales (`LegalNature.admits`); un dato ordinario declarado bajo un padre de categoría
   especial toma la naturaleza del padre. Una naturaleza que cambia si el dato es personal, o que pasa de categoría
   especial a infracciones penales o al revés, es un error.
4. Que una categoría de naturaleza `SPECIAL_CATEGORY` o `CRIMINAL_OFFENCE`
   (`LegalNature.requiresMaximumProtection()`) resuelva, declarada o heredada, a confidencialidad `HIGH` e integridad
   `HIGH`. Un record `DataCategory` construido a mano está sujeto a la misma regla: su constructor lanza
   `IllegalArgumentException` en otro caso.
5. `secret` se combina con OR hacia abajo; los atributos se heredan y los de la hija prevalecen; los rasgos se heredan
   y se suman a los propios de la hija.
6. Que todo `replacedBy` exista en la misma jerarquía y que las sustituciones no formen un ciclo (para categorías,
   usos y sujetos).
7. Que la sustituta de una categoría obsoleta proteja al menos tanto: una naturaleza que la obsoleta admita,
   confidencialidad e integridad no inferiores, y `secret` si la obsoleta es secreta. Una declaración que nombra la
   clave obsoleta se clasifica con su sustituta, así que una más débil rebajaría la protección sin avisar.
8. Que la sustituta de un término obsoleto lleve todos los rasgos del término que sustituye, para categorías de datos,
   usos de datos y sujetos de datos, ya que las declaraciones que aún nombran la clave obsoleta se comprueban con la
   sustituta: una sustituta sin el rasgo levantaría una regla que lo leía.

Consulta [Niveles de protección](protection-levels.md#regla-de-herencia).

### Qué puede hacer una extensión

Una extensión añade categorías de datos **por debajo** de las existentes, usos de datos y sujetos de datos (estos dos
pueden abrir una raíz nueva), eleva la protección de cualquier categoría y añade rasgos y atributos. Mientras se
ejecuta su `contribute`, el constructor rechaza lo que permitiría que una extensión cambiara lo que define otra, o la
taxonomía estándar:

- añadir una categoría de datos **raíz**: una raíz nueva es un dominio nuevo de datos, que necesita su propia política
  de protección;
- llamar a `origin(...)`: los términos que añade una extensión llevan su nombre;
- llamar a `since` o `deprecated` sobre un término que no añadió (uno cuyo origen es otro): solo deprecia sus propios
  términos. Se comprueba al hacer la llamada, frente a la extensión que contribuye entonces.

Elevar siempre está permitido, y combinar no depende del orden: dos extensiones que elevan la misma categoría dan el
más estricto de sus niveles, se aplique primero la que se aplique. Una aplicación que usa el constructor directamente
no está sujeta a estas tres reglas.

### Errores

Todos son `TaxonomyException` (mensajes tal como los produce el código):

| Cuándo | Mensaje |
|---|---|
| `category`/`dataUse`/`dataSubject`, clave no válida | `Invalid key '<key>': use lower-case segments separated by dots` |
| `category`/`dataUse`/`dataSubject`, clave duplicada | `Duplicate key '<key>'` |
| `refineCategory`/`refineDataUse`/`refineDataSubject`, clave desconocida | `Cannot refine unknown data category '<key>'` (o `data use`, `data subject`) |
| `extend`, nombre reservado (`fideslang`, `linneo`, `application`) o en blanco | `Invalid extension name '<name>' in <class>` |
| `origin`, en blanco | `The origin of terms must not be blank` |
| `origin`, llamado por una extensión | `The extension '<name>' cannot change the origin of its terms` |
| `category`, raíz añadida por una extensión | `The extension '<name>' cannot add the root data category '<key>': an extension adds data categories below existing ones` |
| `attribute`, nombre en blanco | `An attribute of '<key>' has a blank name` |
| `nature`, no se concilia con la declarada | `Cannot change the nature of '<key>' from <old> to <new>` |
| `traits`, no aplicable a la jerarquía | `The trait <trait> does not apply to the <hierarchy> '<key>'` |
| `deprecated`, versión en blanco | `The deprecation of '<key>' needs the version that deprecated it` |
| `since`/`deprecated`, de una extensión sobre un término que no añadió | `The extension '<name>' cannot set the version that introduced '<key>', which comes from '<origin>'` (o `deprecate`) |
| cualquier método de un borrador, llamado después de que su customizer haya terminado | `The draft of '<key>' can only be changed inside the customizer that received it` |
| `build`, falta el padre | `The parent '<parent>' of data category '<key>' does not exist` (o `data use`, `data subject`) |
| `build`, raíz incompleta | `Root data category '<key>' must declare its nature, confidentiality and integrity` |
| `build`, naturaleza no admitida | `Data category '<key>' declares nature <n>, which its parent '<parent>' (<pn>) does not admit` |
| `build`, categoría especial o de infracciones penales por debajo de `HIGH` | `Data category '<key>' is <nature> data and must have HIGH confidentiality and integrity, not <c> and <i>` |
| `build`, falta la sustituta | `The replacement '<r>' of data category '<key>' does not exist` |
| `build`, ciclo de sustituciones | `The replacements of data category '<key>' form a cycle` |
| `build`, la sustituta protege menos | `The replacement '<r>' of data category '<key>' has weaker protection: <nature>, <c>, <i>[, secret] instead of <nature>, <c>, <i>[, secret]` |
| `build`, a la sustituta le falta un rasgo | `The replacement '<r>' of data category '<key>' lacks its traits [<trait>, ...]` (o `data use`, `data subject`) |

### Ejemplo

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .origin("my-app")
        .category("user.education.lab_notebook", "Lab Notebook", "Laboratory notebooks kept by the subject.",
                c -> c.attribute("retention", "permanent"))
        .refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
        .dataUse("education.scoring", "Applicant scoring", "Ranking of applicants.", u -> u.traits(Trait.EVALUATION))
        .dataSubject("visiting_researcher", "Visiting Researcher", "A researcher from another institution.")
        .build();

taxonomy.requireCategory("user.education.lab_notebook").info().origin();   // "my-app"
taxonomy.requireCategory(Categories.USER_GOVERNMENT_ID_PASSPORT_NUMBER).confidentiality();   // HIGH (heredada)
```

## TaxonomyExtension

`io.github.devbpm.linneo.TaxonomyExtension` — `public interface`, `@since 0.1.0`

Términos que una aplicación o un sector añaden a la taxonomía, y la protección más estricta que exigen a los
existentes.

| Método | Descripción |
|---|---|
| `String name()` | Se registra como `origin` de los términos que añade la extensión. Un nombre corto y estable; nunca `"fideslang"`, `"linneo"` ni `"application"`. También es la clave de ordenación al cargar extensiones y la identidad que garantiza que una extensión se aplique una sola vez: dos clases distintas no pueden compartir nombre. |
| `void contribute(TaxonomyBuilder builder)` | Añade o refina términos. El constructor ya contiene los términos estándar (si se empezó con `withStandard()`) y las extensiones aplicadas antes que esta. |

Una extensión añade categorías de datos por debajo de las existentes, usos de datos y sujetos de datos, eleva la
protección, añade rasgos y atributos, y depreca solo los términos que añade: el constructor rechaza el resto mientras la
extensión contribuye (consulta [Qué puede hacer una extensión](#qué-puede-hacer-una-extensión)), y las reglas de
protección se aplican al construir la taxonomía. Elige un nombre y unas claves que no use ninguna otra extensión que
apliques: `extend` lanza una `TaxonomyException` si ya se aplicó una clase de extensión distinta con el mismo nombre, y
una clave añadida dos veces falla con `Duplicate key`. Los nombres
`"university"`, `"finance"`, `"supply_chain"`, `"project_operations"` y `"human_resources"` y las claves de las
[extensiones incluidas](extensions/README.md) los usan las extensiones incluidas en Linneo.

```java
package com.example.taxonomy;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyExtension;

public final class CampusTaxonomy implements TaxonomyExtension {

    @Override
    public String name() {
        return "campus";
    }

    @Override
    public void contribute(TaxonomyBuilder builder) {
        builder.category("user.education.lab_notebook", "Lab Notebook", "Laboratory notebooks kept by the subject.",
                        c -> c.attribute("retention", "permanent"))
               .refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
               .dataSubject("visiting_researcher", "Visiting Researcher", "A researcher from another institution.");
    }
}
```

### Registro

`Taxonomy.load()` (y `TaxonomyBuilder.withExtensions()`) aplica todas las extensiones registradas con
`java.util.ServiceLoader`. Registra la tuya de una de estas formas, o de ambas:

- **Module path** — en el `module-info.java` de la extensión:

  ```java
  module com.example.taxonomy {
      requires io.github.devbpm.linneo;
      provides io.github.devbpm.linneo.TaxonomyExtension with com.example.taxonomy.CampusTaxonomy;
  }
  ```

- **Class path** — un fichero `META-INF/services/io.github.devbpm.linneo.TaxonomyExtension` con el nombre
  completo de la clase:

  ```text
  com.example.taxonomy.CampusTaxonomy
  ```

El módulo de Linneo declara `uses io.github.devbpm.linneo.TaxonomyExtension`. Como siempre con `ServiceLoader`, la
clase proveedora debe ser pública y tener un constructor público sin argumentos (o, en el module path, un método
público estático `provider()`).

También puedes aplicar una extensión explícitamente, sin `ServiceLoader`:

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new CampusTaxonomy()).build();
```

Las extensiones incluidas en Linneo no están registradas; consulta
[Extensiones](extensions/README.md#registrar-una-extensión-incluida) para aplicarlas o registrarlas.

### Mantener compatible tu extensión

Las aplicaciones guardan las claves de tu extensión, así que debe seguir las reglas de la taxonomía estándar: añadir
términos, endurecer la protección y declarar obsoletos, pero nunca eliminar una clave ni debilitar la protección.
[`TaxonomyDiff`](taxonomy-diff.md) lo comprueba. Conserva la taxonomía de tu última versión publicada como texto,
escrita con `Taxonomy.toTsv()`, y compárala con la actual en un test:

```java
Taxonomy released = Taxonomy.fromTsv(Files.readString(Path.of("src/test/resources/taxonomy-1.0.0.tsv")));
Taxonomy current = Taxonomy.builder().withStandard().extend(new CampusTaxonomy()).build();
TaxonomyDiff diff = TaxonomyDiff.between(released, current);
diff.requireCompatible();   // TaxonomyException ante un término o rasgo eliminado, protección más débil o una sustituta cambiada
diff.requiredBump();        // el salto de versión que necesitan los cambios: NONE, PATCH, MINOR o MAJOR
```

El diff abarca toda la taxonomía, así que en él aparecen también los cambios de la taxonomía estándar de una versión
más reciente de Linneo.

## CategoryDraft

`io.github.devbpm.linneo.CategoryDraft` — `public final class CategoryDraft extends TermDraft`, `@since 0.1.0`

Las propiedades declaradas de una categoría mientras el constructor está abierto; es lo que reciben los
personalizadores de `category` y `refineCategory`. Toda faceta declarada es un **mínimo**, y toda faceta no declarada
se hereda del padre en `build()`. Todos los métodos devuelven el mismo borrador para encadenar llamadas. Un borrador
solo se puede cambiar dentro del personalizador que lo recibió: guardarlo y llamar a un método después lanza
`TaxonomyException`.

| Método | Descripción |
|---|---|
| `CategoryDraft nature(LegalNature nature)` | Declara la naturaleza jurídica, como mínimo; debe admitirla la del padre. Si ya hay una naturaleza declarada, como al refinar, una más precisa la sustituye (datos personales ordinarios a categoría especial o infracciones penales) y una que la declarada ya cubre no cambia nada. Lanza `TaxonomyException` si no pueden conciliarse: personal y no personal, o categoría especial e infracciones penales. |
| `CategoryDraft confidentiality(Confidentiality confidentiality)` | Declara la confidencialidad, como mínimo: se conserva la más estricta entre esta y un nivel ya declarado, en cualquier orden. Un nivel inferior al del padre se eleva al del padre en `build()`. |
| `CategoryDraft integrity(Integrity integrity)` | Lo mismo para la integridad. |
| `CategoryDraft secret()` | Marca la categoría, y por tanto todas sus hijas, como secreta. No se puede deshacer. |
| `CategoryDraft attribute(String name, String value)` | Fija un atributo definido por la aplicación, que heredan las hijas que no fijen su propio valor. `TaxonomyException` si el nombre está en blanco. |
| `CategoryDraft traits(Trait... traits)` | Declara [rasgos](terms.md#trait) de la categoría, que heredan sus descendientes. Solo se suman. `TaxonomyException` si un rasgo no se aplica a las categorías de datos. |
| `CategoryDraft since(String version)` | Sobrescribe `TermDraft.since` devolviendo `CategoryDraft`. |
| `CategoryDraft deprecated(String sinceVersion, String replacedBy)` | Sobrescribe `TermDraft.deprecated` devolviendo `CategoryDraft`. |

`nature`, `confidentiality`, `integrity`, `traits` y los dos argumentos de `attribute` lanzan `NullPointerException`
ante `null`. Al refinar solo se compara lo que declara el borrador; una faceta solo heredada se compara con la del
padre en `build()`.

## TermDraft

`io.github.devbpm.linneo.TermDraft` — `public sealed class TermDraft permits CategoryDraft`, `@since 0.1.0`

Los rasgos y el ciclo de vida declarados de un término mientras el constructor está abierto; es lo que reciben los
personalizadores de `dataUse`, `dataSubject`, `refineDataUse` y `refineDataSubject`. Igual que con un `CategoryDraft`,
sus métodos lanzan `TaxonomyException` cuando el personalizador que recibió el borrador ya ha terminado.

| Método | Descripción |
|---|---|
| `TermDraft since(String version)` | Declara la versión de su origen que introdujo el término, como `1.2.0`. `TaxonomyException` si una extensión la fija en un término que no añadió. |
| `TermDraft traits(Trait... traits)` | Declara [rasgos](terms.md#trait) del término, que heredan sus descendientes. Declarar de nuevo algunos, como pueden hacer dos extensiones, conserva todos. `TaxonomyException` si un rasgo no se aplica a la jerarquía del término. |
| `TermDraft deprecated(String sinceVersion, String replacedBy)` | Declara obsoleto el término. `sinceVersion` es obligatorio (`NullPointerException` si es `null`, `TaxonomyException` si está en blanco, lo que dejaría el término vigente); `replacedBy` es la clave del término sustituto de la misma jerarquía, o `null`. Las declaraciones que aún lo usan siguen funcionando y se resuelven a la sustituta. `TaxonomyException` si una extensión depreca un término que no añadió. |

Un `since` o un `replacedBy` en blanco se registra como `null` en `TermInfo`.

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard()
        .dataUse("education.tutoring", "Tutoring", "Personal tutoring of students.", u -> u.since("1.0.0"))
        .category("user.education.old_grade", "Old grade", "Superseded.",
                c -> c.deprecated("1.1.0", Categories.USER_EDUCATION_GRADE))
        .build();
```

Véase también: [Taxonomía](taxonomy.md) · [Niveles de protección](protection-levels.md) ·
[Taxonomía estándar](standard-taxonomy.md) · [Términos](terms.md) ·
[Diferencias entre taxonomías](taxonomy-diff.md) · [Extensiones](extensions/README.md) ·
curso: [Extender la taxonomía](../course/06-extending-the-taxonomy.md)
