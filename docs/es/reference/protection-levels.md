---
title: "Niveles de protección"
parent: "Referencia"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/LegalNature.java
  - src/main/java/io/github/devbpm/linneo/Confidentiality.java
  - src/main/java/io/github/devbpm/linneo/Integrity.java
  - src/main/java/io/github/devbpm/linneo/Identifiability.java
  - src/main/java/io/github/devbpm/linneo/DataCategory.java
  - src/main/java/io/github/devbpm/linneo/Protection.java
-->

# Niveles de protección

Una [`DataCategory`](terms.md#datacategory) lleva cuatro facetas de protección: `nature`, `confidentiality`,
`integrity` y `secret`. [`Protection`](#protection) las agrupa en un único valor que se puede comparar y combinar.
`Identifiability` no es una faceta de la categoría: pertenece al campo (ver
[`@Category`](annotations/category.md)).

| Faceta | Tipo | Valores (de menos a más protección) | Dónde |
|---|---|---|---|
| naturaleza | `LegalNature` | `NONE`, `ORDINARY`, `SPECIAL_CATEGORY` / `CRIMINAL_OFFENCE` | categoría |
| confidencialidad | `Confidentiality` | `PUBLIC`, `LOW`, `MEDIUM`, `HIGH` | categoría; un campo puede elevarla |
| integridad | `Integrity` | `LOW`, `MEDIUM`, `HIGH` | categoría; un campo puede elevarla |
| secreto | `boolean` | `false`, `true` | categoría |
| identificabilidad | `Identifiability` | `IDENTIFIED`, `PSEUDONYMIZED`, `ANONYMIZED`, `AGGREGATED` | solo el campo |

Los cuatro enumerados, y el registro `Protection`, son `@since 0.1.0`.

## LegalNature

`io.github.devbpm.linneo.LegalNature` — qué es una categoría de datos según la normativa de protección de datos: Reglamento (UE) 2016/679 (RGPD, con las
correcciones de DOUE L 127, 23.5.2018) y, para España, Ley Orgánica 3/2018 (LOPDGDD, texto consolidado de 27 de
diciembre de 2025).

| Constante | Significado |
|---|---|
| `NONE` | No son datos personales: datos del sistema, de la organización o de referencia. |
| `ORDINARY` | Datos personales (artículo 4.1 del RGPD). |
| `SPECIAL_CATEGORY` | Categoría especial de datos personales (artículo 9 del RGPD): origen étnico o racial, opiniones políticas, convicciones religiosas o filosóficas, afiliación sindical, datos genéticos, datos biométricos tratados para identificar de manera unívoca a una persona, salud, vida sexual y orientación sexual. En España, el consentimiento por sí solo no levanta la prohibición cuando la finalidad principal es identificar ideología, afiliación sindical, religión, orientación sexual, creencias u origen racial o étnico (artículo 9.1 de la LOPDGDD), y el tratamiento amparado en el artículo 9.2 g), h) e i) del RGPD requiere una norma con rango de ley (artículo 9.2 de la LOPDGDD). |
| `CRIMINAL_OFFENCE` | Datos personales relativos a condenas e infracciones penales o medidas de seguridad conexas (artículo 10 del RGPD y de la LOPDGDD). Los datos de infracciones y sanciones administrativas no son de esta naturaleza: son `ORDINARY` con confidencialidad alta (artículo 27 de la LOPDGDD). |

| Método | Descripción |
|---|---|
| `boolean isPersonal()` | `false` solo para `NONE`. |
| `boolean admits(LegalNature child)` | Si una categoría hija puede tener la naturaleza `child` cuando su padre tiene esta. |
| `boolean covers(LegalNature other)` | Si los datos de esta naturaleza necesitan al menos la protección que necesitan los de `other`: `true` si esta naturaleza es `other` o más estricta que ella. Es un orden parcial, ver más abajo. |
| `boolean requiresMaximumProtection()` | `true` para `SPECIAL_CATEGORY` y `CRIMINAL_OFFENCE`: los datos de esa naturaleza deben tener confidencialidad `HIGH` e integridad `HIGH`. |

`admits` es la regla que aplica el constructor a la naturaleza de una categoría hija:

| Naturaleza del padre | Naturalezas admitidas en la hija |
|---|---|
| `NONE` | `NONE` |
| `ORDINARY` | `ORDINARY`, `SPECIAL_CATEGORY`, `CRIMINAL_OFFENCE` |
| `SPECIAL_CATEGORY` | `SPECIAL_CATEGORY` |
| `CRIMINAL_OFFENCE` | `CRIMINAL_OFFENCE` |

Así, la naturaleza la fija la raíz de la jerarquía: una jerarquía `NONE` nunca contiene datos personales, una
categoría `ORDINARY` puede tener hijas de categoría especial o de infracciones penales, y estas conservan su
naturaleza en todo el subárbol.

`requiresMaximumProtection()` es la regla que fija los niveles: `TaxonomyBuilder.build()` lanza `TaxonomyException`
si una categoría `SPECIAL_CATEGORY` o `CRIMINAL_OFFENCE` no resuelve (declarada o heredada) a confidencialidad `HIGH`
e integridad `HIGH`.

`covers` ordena las naturalezas según cuánta protección necesitan sus datos. [`Protection.covers`](#protection)
compara protecciones con ese orden:

| Naturaleza | Cubre |
|---|---|
| `NONE` | `NONE` |
| `ORDINARY` | `NONE`, `ORDINARY` |
| `SPECIAL_CATEGORY` | `NONE`, `ORDINARY`, `SPECIAL_CATEGORY` |
| `CRIMINAL_OFFENCE` | `NONE`, `ORDINARY`, `CRIMINAL_OFFENCE` |

El orden es parcial: `SPECIAL_CATEGORY` y `CRIMINAL_OFFENCE` son más estrictas que `ORDINARY`, pero ninguna cubre a la
otra, porque cada una trae obligaciones que la otra no tiene. `admits` es más restrictiva que `covers`: una jerarquía
también conserva si contiene datos personales, así que una categoría `NONE` nunca tiene una hija personal.

```java
LegalNature.ORDINARY.admits(LegalNature.SPECIAL_CATEGORY);           // true
LegalNature.SPECIAL_CATEGORY.admits(LegalNature.CRIMINAL_OFFENCE);   // false
LegalNature.NONE.admits(LegalNature.ORDINARY);                       // false

LegalNature.SPECIAL_CATEGORY.covers(LegalNature.ORDINARY);           // true
LegalNature.SPECIAL_CATEGORY.covers(LegalNature.CRIMINAL_OFFENCE);   // false
LegalNature.ORDINARY.covers(LegalNature.NONE);                       // true
```

## Confidentiality

`io.github.devbpm.linneo.Confidentiality` — cuánto daño causaría divulgar los datos. `LOW`, `MEDIUM` y `HIGH`
siguen las escalas habituales de tres niveles de los marcos de seguridad de la información (por ejemplo, el ENS o
FIPS 199); `PUBLIC` es para datos destinados a ser públicos.

| Constante | Significado |
|---|---|
| `PUBLIC` | Datos públicos: divulgarlos no causa daño. |
| `LOW` | La divulgación causaría un daño limitado. |
| `MEDIUM` | La divulgación causaría un daño grave. |
| `HIGH` | La divulgación causaría un daño muy grave. Es siempre el nivel de los datos de categoría especial y de condenas e infracciones penales. |

Las constantes se declaran en orden creciente, así que `compareTo` las ordena por exigencia.

| Método | Descripción |
|---|---|
| `Confidentiality max(Confidentiality other)` | El más exigente entre este nivel y `other`. |

## Integrity

`io.github.devbpm.linneo.Integrity` — cuánto daño causaría un cambio no autorizado o no detectado en los datos. La
integridad es independiente de la confidencialidad: un registro de auditoría o una matriz de permisos pueden ser
inofensivos de divulgar y críticos de mantener inalterados.

| Constante | Significado |
|---|---|
| `LOW` | Un cambio no deseado causaría un daño limitado. |
| `MEDIUM` | Un cambio no deseado causaría un daño grave. |
| `HIGH` | Un cambio no deseado causaría un daño muy grave. |

Las constantes se declaran en orden creciente, así que `compareTo` las ordena por exigencia.

| Método | Descripción |
|---|---|
| `Integrity max(Integrity other)` | El más exigente entre este nivel y `other`. |

```java
Confidentiality.LOW.max(Confidentiality.HIGH);   // HIGH
Integrity.HIGH.compareTo(Integrity.MEDIUM) > 0;  // true
```

## Identifiability

`io.github.devbpm.linneo.Identifiability` — hasta qué punto un valor almacenado de datos personales apunta
directamente a una persona. Pertenece al campo, no a la categoría: la misma categoría puede guardarse identificada en
una tabla y seudonimizada en otra.

| Constante | Significado | ¿Dato personal? |
|---|---|---|
| `IDENTIFIED` | El valor identifica a la persona directamente o mediante otros datos que tiene el responsable. | sí |
| `PSEUDONYMIZED` | El valor solo puede vincularse a la persona con información adicional guardada por separado (artículo 4.5 del RGPD). | sí (considerando 26 del RGPD) |
| `ANONYMIZED` | El valor ya no puede vincularse a la persona. | no, mientras nadie pueda reidentificarla |
| `AGGREGATED` | El valor resume a varias personas y no describe a ninguna de ellas. | no, mientras nadie pueda reidentificarla |

`FieldClassification.isPersonal()` aplica la última columna: un campo es dato personal solo cuando su categoría es
personal y su identificabilidad es `IDENTIFIED` o `PSEUDONYMIZED` (ver [Resultados](classify/results.md)). Un campo
con categoría personal y sin identificabilidad declarada es `IDENTIFIED`; un campo con categoría no personal no tiene
identificabilidad.

## Regla de herencia

`TaxonomyBuilder.build()` resuelve las facetas empezando por el padre:

- Una categoría **raíz** debe declarar `nature`, `confidentiality` e `integrity`.
- Una categoría **hija** hereda toda faceta que no declara. Una faceta que declara solo puede ser **igual o más
  estricta**:
  - `nature`: debe admitirla la naturaleza del padre (`parent.nature().admits(child)`);
  - `confidentiality`, `integrity`: no pueden ser inferiores a las del padre (`compareTo`).
  En caso contrario, `build()` lanza `TaxonomyException`.
- Una categoría cuya naturaleza efectiva cumple `requiresMaximumProtection()` (`SPECIAL_CATEGORY` o
  `CRIMINAL_OFFENCE`) debe acabar con confidencialidad `HIGH` e integridad `HIGH`, declaradas o heredadas. Una hija
  que pasa a ser categoría especial bajo un padre con niveles inferiores debe, por tanto, subirlos también.
- `secret` se combina con **OR** hacia abajo: una categoría es secreta si lo declara o si su padre es secreto. Una
  categoría secreta hace secretas a todas sus descendientes, y nada puede revertirlo.
- `attributes` se heredan: la hija recibe los atributos del padre, sobrescritos por los que declara.

La `DataCategory` resultante contiene los valores efectivos, así que `DataCategory.confidentiality()` ya incluye lo
heredado. En la taxonomía estándar, por ejemplo:

| Categoría | Declara | Efectivo |
|---|---|---|
| `user` | `ORDINARY`, `LOW`, `MEDIUM`, no secreta | `ORDINARY`, `LOW`, `MEDIUM`, no secreta |
| `user.authorization` | confidencialidad `HIGH`, integridad `HIGH` | `ORDINARY`, `HIGH`, `HIGH`, no secreta |
| `user.authorization.biometric` | naturaleza `SPECIAL_CATEGORY`, secreta | `SPECIAL_CATEGORY`, `HIGH`, `HIGH`, secreta |
| `user.authorization.password` | secreta | `ORDINARY`, `HIGH`, `HIGH`, secreta |

A nivel de campo, [`@Category`](annotations/category.md) aplica la misma dirección: un campo puede elevar la
confidencialidad o la integridad por encima de las de su categoría, nunca rebajarlas (`LOWERED_LEVEL`).

## Protection

`io.github.devbpm.linneo.Protection` — `public record Protection(...)`, `@since 0.1.0`

La protección que requieren unos datos, como un único valor: las cuatro facetas de una categoría. Linneo la usa allí
donde se comparan protecciones: una categoría y su sustituta, los campos de una clase
([`FieldClassification.protection()`](classify/results.md)), los datos personales de una actividad de tratamiento
([`CheckedActivity.protection()`](processing/checker.md)) o la misma clasificación en dos versiones de un modelo.

```java
public record Protection(Set<LegalNature> natures, Confidentiality confidentiality,
                         Integrity integrity, boolean secret)
```

| Componente | Descripción |
|---|---|
| `natures` | Las naturalezas legales más estrictas de los datos, nunca vacío: solo `LegalNature.NONE` si los datos no son personales. |
| `confidentiality` | El nivel de [confidencialidad](#confidentiality). |
| `integrity` | El nivel de [integridad](#integrity). |
| `secret` | Si los datos dan acceso y no deben leerse de vuelta, exportarse ni publicarse nunca. |

El componente es un conjunto porque las naturalezas solo están parcialmente ordenadas: la protección de un registro que
contiene datos de salud y antecedentes penales conserva ambas naturalezas y no cubre ninguna de las dos por sí sola. El
constructor canónico descarta toda naturaleza que cubra otra del conjunto (`{ORDINARY, SPECIAL_CATEGORY}` pasa a ser
`{SPECIAL_CATEGORY}`) y hace el conjunto inmutable, de modo que protecciones iguales son registros iguales. Lanza
`NullPointerException` si un componente es `null` e `IllegalArgumentException` si `natures` está vacío.

| Miembro | Descripción |
|---|---|
| `static Protection of(LegalNature nature, Confidentiality confidentiality, Integrity integrity, boolean secret)` | La protección de datos de una sola naturaleza. |
| `static final Protection LOWEST` | No personal, `PUBLIC`, integridad `LOW`, no secreto. Toda protección la cubre, y combinarla no cambia nada. |
| `boolean covers(Protection other)` | Si esta protección es al menos tan estricta como `other` en todas las facetas: cada naturaleza de la otra está cubierta por alguna de estas ([`LegalNature.covers`](#legalnature)), los niveles no son inferiores y un secreto sigue siéndolo. |
| `Direction comparedTo(Protection before)` | Cómo se compara esta protección con una anterior, como la del mismo campo en la versión previa de un modelo. Ver [`Direction`](#direction). |
| `Protection join(Protection other)` | La menor protección que cubre a ambas: las naturalezas más estrictas de las dos, los niveles más altos y secreto si lo es alguna. |
| `static Protection joinAll(Collection<Protection> protections)` | La combinación de todas, o `LOWEST` si no hay ninguna. |
| `boolean isPersonal()` | `true` si alguna naturaleza es personal. |
| `boolean requiresMaximumProtection()` | `true` si alguna naturaleza es `SPECIAL_CATEGORY` o `CRIMINAL_OFFENCE`. |

`covers` es un orden parcial: cuando una protección es más estricta en una faceta y más débil en otra, ninguna cubre a
la otra. `join` es la menor cota superior de ese orden, así que la protección de unos datos que contienen varios tipos
de datos es la combinación de sus protecciones.

### Direction

`Protection.Direction` (`@since 0.1.0`) es el resultado de `comparedTo`.

| Constante | Significado |
|---|---|
| `SAME` | La protección es la misma. |
| `TIGHTENED` | La protección nueva cubre a la antigua: más estricta en al menos una faceta y no más débil en ninguna. |
| `LOOSENED` | La protección antigua cubre a la nueva: más débil en al menos una faceta y no más estricta en ninguna. |
| `MIXED` | Más estricta en una faceta y más débil en otra, o una naturaleza no comparable con la antigua, como unos datos de categoría especial que pasan a ser de condenas e infracciones penales. Merece la misma revisión que un debilitamiento. |

| Método | Descripción |
|---|---|
| `boolean weakens()` | `true` para `LOOSENED` y `MIXED`: alguna protección baja, así que alguien debe revisar el cambio. |
| `Direction and(Direction other)` | Combina esta dirección con la de otra faceta del mismo cambio: `SAME` da la otra, dos direcciones iguales se mantienen y cualquier otro par es `MIXED`. |

```java
Protection contact = Protection.of(LegalNature.ORDINARY, Confidentiality.MEDIUM, Integrity.LOW, false);
Protection health = Protection.of(LegalNature.SPECIAL_CATEGORY, Confidentiality.HIGH, Integrity.HIGH, false);
Protection criminal = Protection.of(LegalNature.CRIMINAL_OFFENCE, Confidentiality.HIGH, Integrity.HIGH, false);

health.covers(contact);                      // true
health.covers(criminal);                     // false: categoría especial y condenas penales no son comparables

Protection both = health.join(criminal);     // naturalezas SPECIAL_CATEGORY y CRIMINAL_OFFENCE, HIGH, HIGH
both.covers(health);                         // true
both.covers(criminal);                       // true

health.comparedTo(contact);                  // TIGHTENED
contact.comparedTo(health);                  // LOOSENED
criminal.comparedTo(health);                 // MIXED, y MIXED.weakens() es true
Protection.joinAll(List.of());               // Protection.LOWEST

DataCategory category = Taxonomy.standard().requireCategory(Categories.USER_HEALTH_AND_MEDICAL);
category.protection().covers(contact);       // true
```

Véase también: [Términos](terms.md) · [Taxonomía estándar](standard-taxonomy.md) ·
[Extender la taxonomía](extending.md) · [`@Category`](annotations/category.md) ·
curso: [La taxonomía](../course/02-the-taxonomy.md)
