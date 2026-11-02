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
  - src/main/java/io/github/devbpm/linneo/Trait.java
-->

# Términos

Cada nodo de una taxonomía es un `Term`. La clave de un término es una ruta separada por puntos, como
`user.contact.email`; su padre es la clave sin el último segmento (`user.contact`). Los tres tipos de término son
records:

| Tipo | Jerarquía | Describe | Componentes |
|---|---|---|---|
| [`DataCategory`](#datacategory) | categorías de datos | qué contiene un campo | `info` + facetas de protección + atributos + rasgos |
| [`DataUse`](#datause) | usos de datos | para qué se tratan los datos (finalidad) | `info` + rasgos |
| [`DataSubject`](#datasubject) | sujetos de datos | de quién son los datos | `info` + rasgos |

Los términos los crea [`TaxonomyBuilder`](extending.md); se obtienen de una [`Taxonomy`](taxonomy.md).

## Term

`io.github.devbpm.linneo.Term` — `public sealed interface Term permits DataCategory, DataUse, DataSubject`,
`@since 0.1.0`

| Método | Devuelve | Descripción |
|---|---|---|
| `TermInfo info()` | `TermInfo` | Clave, padre, nombre, descripción, origen y ciclo de vida. |
| `Set<Trait> traits()` | `Set<Trait>` | Los [rasgos](#trait) del término, declarados en él o heredados de sus ancestros, que leen las reglas jurídicas de Linneo. Inmutable, posiblemente vacío. |
| `default boolean has(Trait trait)` | `boolean` | `true` si `traits()` contiene el rasgo. |
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
                           Integrity integrity, boolean secret, Map<String, String> attributes,
                           Set<Trait> traits) implements Term
```

| Componente | Descripción |
|---|---|
| `info` | Clave, padre, nombre, descripción, origen y ciclo de vida. |
| `nature` | Qué es el dato según la normativa de protección de datos ([`LegalNature`](protection-levels.md#legalnature)). |
| `confidentiality` | El daño que causaría su divulgación ([`Confidentiality`](protection-levels.md#confidentiality)). |
| `integrity` | El daño que causaría un cambio no deseado ([`Integrity`](protection-levels.md#integrity)). |
| `secret` | Si el dato da acceso (contraseñas, claves, tokens) y no debe leerse de vuelta, exportarse ni publicarse nunca, solo verificarse. |
| `attributes` | Propiedades definidas por la aplicación, como una clase de retención o el grupo de un registro de actividades de tratamiento. Las hijas las heredan salvo que fijen su propio valor. |
| `traits` | Los [rasgos](#trait) que la categoría declara o hereda, que leen las reglas jurídicas de Linneo. |

Las facetas son las **efectivas**: los valores que declara la categoría o, donde no declara ninguno, los heredados de
su padre. Una `Taxonomy` garantiza que nunca son más débiles que los del padre (ver
[Niveles de protección](protection-levels.md#regla-de-herencia)).

El constructor canónico lanza `NullPointerException` si `info`, `nature`, `confidentiality` o `integrity` son
`null`; un `attributes` `null` se convierte en un mapa vacío, y el mapa se copia en uno inmutable; un `traits` `null`
se convierte en un conjunto vacío, y `IllegalArgumentException` indica un rasgo que no se aplica a las categorías de
datos. Lanza también `IllegalArgumentException` si la `nature` es de categoría especial o de datos de condenas e
infracciones penales (`LegalNature.requiresMaximumProtection()`) y la confidencialidad y la integridad no son ambas
`HIGH`, como exige `TaxonomyBuilder.build()` a toda categoría que construye.

| Método | Devuelve | Descripción |
|---|---|---|
| `boolean isPersonal()` | `boolean` | `true` salvo que `nature` sea `LegalNature.NONE`. |
| `Protection protection()` | [`Protection`](protection-levels.md#protection) | La naturaleza, la confidencialidad, la integridad y el indicador de secreto de la categoría como un único valor, para compararlo con otras protecciones. |
| `String attribute(String name)` | `String` | El valor del atributo, o `null` si ni esta categoría ni ningún ancestro lo fija. |

```java
DataCategory health = Taxonomy.standard().requireCategory(Categories.USER_HEALTH_AND_MEDICAL);
health.nature();            // SPECIAL_CATEGORY
health.confidentiality();   // HIGH
health.isPersonal();        // true
health.secret();            // false
health.protection();        // las cuatro facetas anteriores como una Protection
```

## DataUse

`io.github.devbpm.linneo.DataUse` — `public record DataUse(TermInfo info, Set<Trait> traits) implements Term`,
`@since 0.1.0`

Para qué se tratan los datos: la finalidad de una actividad de tratamiento, como
`essential.service.authentication`. Una finalidad pertenece a un proceso o a un sistema, no a un campo, por lo que
ninguna anotación se refiere a ella; usa estos términos donde describas tus actividades de tratamiento. El
constructor lanza `NullPointerException` si `info` es `null`; un `traits` `null` se convierte en un conjunto vacío, y
`IllegalArgumentException` indica un rasgo que no se aplica a los usos de datos.

## DataSubject

`io.github.devbpm.linneo.DataSubject` — `public record DataSubject(TermInfo info, Set<Trait> traits) implements Term`,
`@since 0.1.0`

De quién son los datos tratados: un tipo de persona, como `employee` o `student`. Igual que `DataUse`, describe una
actividad de tratamiento y no un campo. El constructor lanza `NullPointerException` si `info` es `null`; `traits` se
comprueba como en `DataUse`.

```java
Taxonomy taxonomy = Taxonomy.standard();
DataUse assessment = taxonomy.requireDataUse(DataUses.EDUCATION_ASSESSMENT);
DataSubject student = taxonomy.requireDataSubject(DataSubjects.STUDENT);
```

## Trait

`io.github.devbpm.linneo.Trait` — `public enum Trait`, `@since 0.1.0`

Una propiedad de un término que leen las reglas jurídicas de Linneo, para que las reglas sigan a la taxonomía y no a
listas de claves: un término de una aplicación o de una extensión pesa tanto como uno estándar en cuanto declara sus
rasgos. Un rasgo se declara en un término (`TermDraft.traits(Trait...)`) y lo **heredan** todos los términos que
cuelgan de él. Los términos propios de Linneo declaran los suyos en el recurso `linneo-traits.tsv` de la
[taxonomía estándar](standard-taxonomy.md#rasgos-de-los-términos-estándar), y cada [extensión](extensions/README.md)
incluida declara los de sus términos. Cada rasgo se aplica solo a algunas jerarquías; declararlo en otra es un error.

| Rasgo | Se aplica a | Marca |
|---|---|---|
| `FINANCIAL` | categorías de datos | Datos bancarios y de tarjetas, ingresos, remuneración, deudas, transacciones y solvencia. |
| `BEHAVIOURAL` | categorías de datos | Hábitos, preferencias, actividad, ubicación y datos de tráfico. |
| `PRIVATE_LIFE` | categorías de datos | El contenido de las comunicaciones, los documentos privados y las actividades del hogar. |
| `DIRECT_IDENTIFIER` | categorías de datos | Datos que identifican a una persona por sí solos: un nombre, un documento de identidad, un rostro, una plantilla biométrica. |
| `CONTACT_IDENTIFIER` | categorías de datos | Datos mediante los que se puede localizar a una persona y, por tanto, identificarla: datos de contacto, una cuenta. |
| `INDIRECT_IDENTIFIER` | categorías de datos | Datos que singularizan a una persona dentro de un sistema: un identificador interno o de dispositivo. |
| `BIOMETRIC` | categorías de datos | Datos biométricos como los define el artículo 4.14 del RGPD: características físicas, fisiológicas o conductuales que permiten o confirman la identificación única de una persona, como las imágenes faciales o las huellas dactilares. |
| `GENETIC` | categorías de datos | Datos genéticos como los define el artículo 4.13 del RGPD: características genéticas heredadas o adquiridas, tal como las da el análisis de una muestra biológica. |
| `MONITORING` | categorías de datos, usos de datos | Datos obtenidos observando a las personas, o una finalidad que las observa, rastrea o vigila de forma sistemática. |
| `VULNERABLE` | categorías de datos, sujetos de datos | Personas en una posición más débil que el responsable, como niños, empleados y pacientes, o datos que tratan de ellas por su naturaleza. |
| `EVALUATION` | usos de datos | Una finalidad que evalúa o puntúa a las personas, elabora perfiles de ellas o predice su comportamiento, rendimiento o situación. |
| `DIRECT_MARKETING` | usos de datos | Mercadotecnia directa, incluidas las campañas de captación de fondos y la elaboración de perfiles relacionada, a la que el interesado puede oponerse en todo momento (artículo 21.2 del RGPD). |
| `RESEARCH` | usos de datos | Una finalidad de investigación científica o histórica o de estadística (artículo 89.1 del RGPD), a la que los derechos de supresión y oposición se aplican con sus propios límites (artículos 17.3.d y 21.6). |

| Método | Descripción |
|---|---|
| `boolean appliesTo(TermChange.Hierarchy hierarchy)` | Si el rasgo puede declararse en los términos de una jerarquía. |
| `boolean isIdentifier()` | `true` para `DIRECT_IDENTIFIER`, `CONTACT_IDENTIFIER` e `INDIRECT_IDENTIFIER`: los rasgos que indican cómo un valor identifica a una persona. |
| `boolean isHighlyPersonal()` | `true` para `FINANCIAL`, `BEHAVIOURAL` y `PRIVATE_LIFE`: datos de naturaleza altamente personal en el sentido de las directrices sobre evaluaciones de impacto. |

Cuatro reglas los leen:

- [`ImpactScreening`](processing/impact-screening.md) lee `FINANCIAL`, `BEHAVIOURAL` y `PRIVATE_LIFE` como datos de
  naturaleza altamente personal, y `EVALUATION`, `MONITORING` y `VULNERABLE` como los criterios del mismo nombre
  (WP248 rev.01, III.B.a);
- [`BreachAssessment`](processing/breach-assessment.md) lee `FINANCIAL` y `BEHAVIOURAL` como el contexto del
  tratamiento de los datos y los rasgos de identificador como la facilidad de identificación (ENISA, 2013);
- [`SubjectRights`](processing/subject-rights.md) lee `DIRECT_MARKETING` para el derecho de oposición absoluto
  (artículo 21.2 y 21.3 del RGPD) y `RESEARCH` para el derecho de oposición a la investigación (artículo 21.6) y el
  límite a la supresión (artículo 17.3.d);
- [`ProcessingChecker`](processing/checker.md) lee `BIOMETRIC` y `GENETIC` con `Jurisdiction.SPAIN`, donde la lista de
  la AEPD del artículo 35.4 los cuenta como criterios propios.

Añadir un rasgo a un término endurece esas reglas, así que es un cambio menor de una taxonomía; quitarlo puede levantar
una obligación, así que es un cambio mayor ([`TermChange.Aspect.TRAITS`](taxonomy-diff.md#aspect)). `Taxonomy.toTsv()`
los escribe como filas `trait`.

Una sustituta lleva todos los rasgos del término que sustituye, en las tres jerarquías: las declaraciones que aún
nombran la clave obsoleta se comprueban con la sustituta, así que una sin el rasgo levantaría la regla en silencio.
`TaxonomyBuilder.build()` lanza `TaxonomyException` (`The replacement '<r>' of data category '<key>' lacks its traits
[...]`) en otro caso. Los rasgos son una ayuda: permiten que las reglas vean lo que una clave no dice, y el delegado de
protección de datos sigue revisando cada resultado.

```java
Taxonomy taxonomy = Taxonomy.standard();
DataCategory email = taxonomy.requireCategory(Categories.USER_CONTACT_EMAIL);
email.has(Trait.CONTACT_IDENTIFIER);                                      // true, heredado de user.contact
taxonomy.requireDataUse(DataUses.PERSONALIZE_PROFILING).traits();         // [EVALUATION]
taxonomy.requireDataSubject(DataSubjects.EMPLOYEE).has(Trait.VULNERABLE); // true
```

Véase también: [Taxonomía](taxonomy.md) · [Niveles de protección](protection-levels.md) ·
[Constantes de claves](key-constants.md) · [Extender la taxonomía](extending.md) ·
curso: [La taxonomía](../course/02-the-taxonomy.md)
