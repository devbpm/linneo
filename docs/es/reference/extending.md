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
-->

# Extender la taxonomía

Las aplicaciones añaden términos, y endurecen la protección de los existentes, directamente sobre un
`TaxonomyBuilder` o mediante una `TaxonomyExtension` que `Taxonomy.load()` descubre con `ServiceLoader`. Linneo
incluye además extensiones opcionales para un sector, como la [extensión universitaria](extensions/university.md):
un ejemplo completo de `TaxonomyExtension` con sus constantes de claves (ver [Extensiones](extensions/README.md)).

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
| `TaxonomyBuilder extend(TaxonomyExtension extension)` | Aplica una extensión: llama a `contribute(this)` con su `name()` como origen. Aplicar de nuevo la misma **clase** de extensión **no tiene efecto**, de modo que una extensión puede aplicarse de forma explícita y estar además registrada como servicio. Lanza `TaxonomyException` si el nombre es `null`, está en blanco, o es `"fideslang"` o `"linneo"`, o si ya se aplicó una clase **distinta** con el mismo nombre (`The extensions <primera clase> and <segunda clase> are both named '<nombre>'`); `NullPointerException` si `extension` es `null`. |
| `TaxonomyBuilder origin(String origin)` | Fija el origen que se registra para los términos que se añadan a partir de ahora (por defecto `"application"`). `NullPointerException` si es `null`. |
| `TaxonomyBuilder category(String key, String name, String description)` | Añade una categoría de datos que hereda toda su protección de su padre. |
| `TaxonomyBuilder category(String key, String name, String description, Consumer<CategoryDraft> customizer)` | Añade una categoría y declara sus facetas, atributos y ciclo de vida en un [`CategoryDraft`](#categorydraft). |
| `TaxonomyBuilder refineCategory(String key, Consumer<CategoryDraft> customizer)` | Endurece la protección de una categoría existente, o le añade atributos o una obsolescencia. Una faceta que la categoría ya **declara** solo puede elevarse (se comprueba en el momento). Lanza `TaxonomyException` si la categoría no existe. |
| `TaxonomyBuilder dataUse(String key, String name, String description)` | Añade un uso de datos. |
| `TaxonomyBuilder dataUse(String key, String name, String description, Consumer<TermDraft> customizer)` | Añade un uso de datos y declara su ciclo de vida en un [`TermDraft`](#termdraft). |
| `TaxonomyBuilder dataSubject(String key, String name, String description)` | Añade un sujeto de datos. |
| `TaxonomyBuilder dataSubject(String key, String name, String description, Consumer<TermDraft> customizer)` | Añade un sujeto de datos y declara su ciclo de vida. |
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
3. Que una categoría hija herede toda faceta que no declara y que una faceta declarada no sea más débil que la del
   padre: la naturaleza del padre debe admitir `nature` (`LegalNature.admits`), y `confidentiality` e `integrity` no
   pueden ser inferiores.
4. `secret` se combina con OR hacia abajo; los atributos se heredan y los de la hija prevalecen.
5. Que todo `replacedBy` exista en la misma jerarquía y que las sustituciones no formen un ciclo (para categorías,
   usos y sujetos).

Consulta [Niveles de protección](protection-levels.md#regla-de-herencia).

### Errores

Todos son `TaxonomyException` (mensajes tal como los produce el código):

| Cuándo | Mensaje |
|---|---|
| `category`/`dataUse`/`dataSubject`, clave no válida | `Invalid key '<key>': use lower-case segments separated by dots` |
| `category`/`dataUse`/`dataSubject`, clave duplicada | `Duplicate key '<key>'` |
| `refineCategory`, clave desconocida | `Cannot refine unknown data category '<key>'` |
| `extend`, nombre reservado o en blanco | `Invalid extension name '<name>' in <class>` |
| `refineCategory`, naturaleza no admitida por la declarada | `Cannot change the nature of '<key>' from <old> to <new>` |
| `refineCategory`, confidencialidad inferior | `Cannot lower the confidentiality of '<key>' from <old> to <new>` |
| `refineCategory`, integridad inferior | `Cannot lower the integrity of '<key>' from <old> to <new>` |
| `build`, falta el padre | `The parent '<parent>' of data category '<key>' does not exist` (o `data use`, `data subject`) |
| `build`, raíz incompleta | `Root data category '<key>' must declare its nature, confidentiality and integrity` |
| `build`, naturaleza no admitida | `Data category '<key>' declares nature <n>, which its parent '<parent>' (<pn>) does not admit` |
| `build`, confidencialidad inferior | `Data category '<key>' declares confidentiality <c>, lower than its parent's <pc>` |
| `build`, integridad inferior | `Data category '<key>' declares integrity <i>, lower than its parent's <pi>` |
| `build`, falta la sustituta | `The replacement '<r>' of data category '<key>' does not exist` |
| `build`, ciclo de sustituciones | `The replacements of data category '<key>' form a cycle` |

### Ejemplo

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .origin("my-app")
        .category("user.education.lab_notebook", "Lab Notebook", "Laboratory notebooks kept by the subject.",
                c -> c.attribute("retention", "permanent"))
        .refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
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
| `String name()` | Se registra como `origin` de los términos que añade la extensión. Un nombre corto y estable; nunca `"fideslang"` ni `"linneo"`. También es la clave de ordenación al cargar extensiones y la identidad que garantiza que una extensión se aplique una sola vez: dos clases distintas no pueden compartir nombre. |
| `void contribute(TaxonomyBuilder builder)` | Añade o refina términos. El constructor ya contiene los términos estándar (si se empezó con `withStandard()`) y las extensiones aplicadas antes que esta. |

Una extensión solo puede añadir hijos a términos existentes y endurecer la protección; las reglas se aplican al
construir la taxonomía. Elige un nombre y unas claves que no use ninguna otra extensión que apliques: `extend` lanza
una `TaxonomyException` si ya se aplicó una clase de extensión distinta con el mismo nombre, y una clave añadida dos
veces falla con `Duplicate key`. El nombre
`"university"` y las claves de la [extensión universitaria](extensions/university.md) los usa la extensión incluida en
Linneo.

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

## CategoryDraft

`io.github.devbpm.linneo.CategoryDraft` — `public final class CategoryDraft extends TermDraft`, `@since 0.1.0`

Las propiedades declaradas de una categoría mientras el constructor está abierto; es lo que reciben los
personalizadores de `category` y `refineCategory`. Toda faceta no declarada se hereda del padre en `build()`. Todos
los métodos devuelven el mismo borrador para encadenar llamadas.

| Método | Descripción |
|---|---|
| `CategoryDraft nature(LegalNature nature)` | Declara la naturaleza jurídica; debe admitirla la del padre. Al refinar, lanza `TaxonomyException` si la categoría ya declara una naturaleza que no admite la nueva. |
| `CategoryDraft confidentiality(Confidentiality confidentiality)` | Declara la confidencialidad; nunca inferior a la del padre. Al refinar, lanza `TaxonomyException` si es inferior al nivel ya declarado. |
| `CategoryDraft integrity(Integrity integrity)` | Declara la integridad; nunca inferior a la del padre. Al refinar, lanza `TaxonomyException` si es inferior al nivel ya declarado. |
| `CategoryDraft secret()` | Marca la categoría, y por tanto todas sus hijas, como secreta. No se puede deshacer. |
| `CategoryDraft attribute(String name, String value)` | Fija un atributo definido por la aplicación, que heredan las hijas que no fijen su propio valor. |
| `CategoryDraft since(String version)` | Sobrescribe `TermDraft.since` devolviendo `CategoryDraft`. |
| `CategoryDraft deprecated(String sinceVersion, String replacedBy)` | Sobrescribe `TermDraft.deprecated` devolviendo `CategoryDraft`. |

`nature`, `confidentiality`, `integrity` y los dos argumentos de `attribute` lanzan `NullPointerException` ante
`null`. Las comprobaciones inmediatas al refinar comparan con lo que la categoría **declara**; una faceta solo
heredada se compara con la del padre en `build()`.

## TermDraft

`io.github.devbpm.linneo.TermDraft` — `public sealed class TermDraft permits CategoryDraft`, `@since 0.1.0`

El ciclo de vida declarado de un término mientras el constructor está abierto; es lo que reciben los
personalizadores de `dataUse` y `dataSubject`.

| Método | Descripción |
|---|---|
| `TermDraft since(String version)` | Declara la versión de su origen que introdujo el término, como `1.2.0`. |
| `TermDraft deprecated(String sinceVersion, String replacedBy)` | Declara obsoleto el término. `sinceVersion` es obligatorio (`NullPointerException` si es `null`); `replacedBy` es la clave del término sustituto de la misma jerarquía, o `null`. Las declaraciones que aún lo usan siguen funcionando y se resuelven a la sustituta. |

Los valores en blanco de `since`, `sinceVersion` o `replacedBy` se registran como `null` en `TermInfo`.

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard()
        .dataUse("education.tutoring", "Tutoring", "Personal tutoring of students.", u -> u.since("1.0.0"))
        .category("user.education.old_grade", "Old grade", "Superseded.",
                c -> c.deprecated("1.1.0", Categories.USER_EDUCATION_GRADE))
        .build();
```

Véase también: [Taxonomía](taxonomy.md) · [Niveles de protección](protection-levels.md) ·
[Taxonomía estándar](standard-taxonomy.md) · [Términos](terms.md) · [Extensiones](extensions/README.md) ·
curso: [Extender la taxonomía](../course/06-extending-the-taxonomy.md)
