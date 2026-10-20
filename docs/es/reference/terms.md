---
title: "Términos"
parent: "Referencia"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/Term.java
  - src/main/java/io/github/devbpm/linneo/TermInfo.java
  - src/main/java/io/github/devbpm/linneo/DataCategory.java
  - src/main/java/io/github/devbpm/linneo/DataUse.java
  - src/main/java/io/github/devbpm/linneo/DataSubject.java
-->

# Términos

Cada nodo de una taxonomía es un `Term`. La clave de un término es una ruta separada por puntos, como
`user.contact.email`; su padre es la clave sin el último segmento (`user.contact`). Los tres tipos de término son
records:

| Tipo | Jerarquía | Describe | Componentes |
|---|---|---|---|
| [`DataCategory`](#datacategory) | categorías de datos | qué contiene un campo | `info` + facetas de protección + atributos |
| [`DataUse`](#datause) | usos de datos | para qué se tratan los datos (finalidad) | solo `info` |
| [`DataSubject`](#datasubject) | sujetos de datos | de quién son los datos | solo `info` |

Los términos los crea [`TaxonomyBuilder`](extending.md); se obtienen de una [`Taxonomy`](taxonomy.md).

## Term

`io.github.devbpm.linneo.Term` — `public sealed interface Term permits DataCategory, DataUse, DataSubject`,
`@since 0.1.0`

| Método | Devuelve | Descripción |
|---|---|---|
| `TermInfo info()` | `TermInfo` | Clave, padre, nombre, descripción, origen y ciclo de vida. |
| `default String key()` | `String` | Atajo de `info().key()`. |
| `default String name()` | `String` | Atajo de `info().name()`. |
| `default boolean isA(String ancestorKey)` | `boolean` | `true` si la clave es igual a `ancestorKey` o empieza por `ancestorKey + "."`. Compara segmentos completos: `user.contact.email` es un `user.contact` pero no un `user.con`. No consulta la taxonomía. |

```java
DataCategory email = Taxonomy.standard().requireCategory(Categories.USER_CONTACT_EMAIL);
email.isA(Categories.USER);            // true
email.isA(Categories.USER_CONTACT);    // true
email.isA(Categories.SYSTEM);          // false
```

## TermInfo

`io.github.devbpm.linneo.TermInfo` — `public record`, `@since 0.1.0`

Lo que tiene todo término, sea cual sea su jerarquía.

```java
public record TermInfo(String key, String parentKey, String name, String description, String origin,
                       String since, String deprecatedSince, String replacedBy)
```

| Componente | Admite `null` | Descripción |
|---|---|---|
| `key` | no | Clave única en notación de puntos, como `user.contact.email`. |
| `parentKey` | sí | Clave del término padre; `null` para una raíz. |
| `name` | no | Nombre corto legible. |
| `description` | no | Qué abarca el término. Un argumento `null` se guarda como `""`. |
| `origin` | no | Quién definió el término: `"fideslang"`, `"linneo"`, el nombre de una `TaxonomyExtension` o el origen fijado en el constructor (por defecto `"application"`; ver [`TaxonomyBuilder.origin`](extending.md#taxonomybuilder)). |
| `since` | sí | Versión de su origen que introdujo el término; `null` si se desconoce. |
| `deprecatedSince` | sí | Versión de su origen que lo declaró obsoleto; `null` si está vigente. |
| `replacedBy` | sí | Clave del término que sustituye a uno obsoleto; `null` si no hay. |

El constructor canónico lanza `NullPointerException` si `key`, `name` u `origin` son `null`.

| Miembro | Descripción |
|---|---|
| `static final String FIDESLANG = "fideslang"` | Origen de los términos tomados de la taxonomía por defecto de fideslang. |
| `static final String LINNEO = "linneo"` | Origen de los términos añadidos por Linneo. |
| `Optional<String> parent()` | La clave del padre; vacío para una raíz. |
| `boolean isDeprecated()` | `true` si hay `deprecatedSince` **o** `replacedBy`. Los términos obsoletos no deberían usarse en declaraciones nuevas. |

```java
TermInfo profiling = Taxonomy.standard().requireDataUse(DataUses.PERSONALIZE_PROFILING).info();
profiling.origin();            // "fideslang"
profiling.since();             // "2.0.0"
profiling.isDeprecated();      // true
profiling.deprecatedSince();   // "2.1.1"
profiling.replacedBy();        // "personalize.content.profiling"
```

## DataCategory

`io.github.devbpm.linneo.DataCategory` — `public record DataCategory(...) implements Term`, `@since 0.1.0`

Qué contiene un campo, con la protección que requiere ese tipo de dato.

```java
public record DataCategory(TermInfo info, LegalNature nature, Confidentiality confidentiality,
                           Integrity integrity, boolean secret, Map<String, String> attributes) implements Term
```

| Componente | Descripción |
|---|---|
| `info` | Clave, padre, nombre, descripción, origen y ciclo de vida. |
| `nature` | Qué es el dato según la normativa de protección de datos ([`LegalNature`](protection-levels.md#legalnature)). |
| `confidentiality` | El daño que causaría su divulgación ([`Confidentiality`](protection-levels.md#confidentiality)). |
| `integrity` | El daño que causaría un cambio no deseado ([`Integrity`](protection-levels.md#integrity)). |
| `secret` | Si el dato da acceso (contraseñas, claves, tokens) y no debe leerse de vuelta, exportarse ni publicarse nunca, solo verificarse. |
| `attributes` | Propiedades definidas por la aplicación, como una clase de retención o el grupo de un registro de actividades de tratamiento. Las hijas las heredan salvo que fijen su propio valor. |

Las facetas son las **efectivas**: los valores que declara la categoría o, donde no declara ninguno, los heredados de
su padre. Una `Taxonomy` garantiza que nunca son más débiles que los del padre (ver
[Niveles de protección](protection-levels.md#regla-de-herencia)).

El constructor canónico lanza `NullPointerException` si `info`, `nature`, `confidentiality` o `integrity` son
`null`; un `attributes` `null` se convierte en un mapa vacío, y el mapa se copia en uno inmutable.

| Método | Devuelve | Descripción |
|---|---|---|
| `boolean isPersonal()` | `boolean` | `true` salvo que `nature` sea `LegalNature.NONE`. |
| `String attribute(String name)` | `String` | El valor del atributo, o `null` si ni esta categoría ni ningún ancestro lo fija. |

```java
DataCategory health = Taxonomy.standard().requireCategory(Categories.USER_HEALTH_AND_MEDICAL);
health.nature();            // SPECIAL_CATEGORY
health.confidentiality();   // HIGH
health.isPersonal();        // true
health.secret();            // false
```

## DataUse

`io.github.devbpm.linneo.DataUse` — `public record DataUse(TermInfo info) implements Term`, `@since 0.1.0`

Para qué se tratan los datos: la finalidad de una actividad de tratamiento, como
`essential.service.authentication`. Una finalidad pertenece a un proceso o a un sistema, no a un campo, por lo que
ninguna anotación se refiere a ella; usa estos términos donde describas tus actividades de tratamiento. El
constructor lanza `NullPointerException` si `info` es `null`.

## DataSubject

`io.github.devbpm.linneo.DataSubject` — `public record DataSubject(TermInfo info) implements Term`, `@since 0.1.0`

De quién son los datos tratados: un tipo de persona, como `employee` o `student`. Igual que `DataUse`, describe una
actividad de tratamiento y no un campo. El constructor lanza `NullPointerException` si `info` es `null`.

```java
Taxonomy taxonomy = Taxonomy.standard();
DataUse assessment = taxonomy.requireDataUse(DataUses.EDUCATION_ASSESSMENT);
DataSubject student = taxonomy.requireDataSubject(DataSubjects.STUDENT);
```

Véase también: [Taxonomía](taxonomy.md) · [Niveles de protección](protection-levels.md) ·
[Constantes de claves](key-constants.md) · [Extender la taxonomía](extending.md) ·
curso: [La taxonomía](../course/02-the-taxonomy.md)
