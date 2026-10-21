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
-->

# Niveles de protección

Una [`DataCategory`](terms.md#datacategory) lleva cuatro facetas de protección: `nature`, `confidentiality`,
`integrity` y `secret`. `Identifiability` no es una faceta de la categoría: pertenece al campo (ver
[`@Category`](annotations/category.md)).

| Faceta | Tipo | Valores (de menos a más protección) | Dónde |
|---|---|---|---|
| naturaleza | `LegalNature` | `NONE`, `ORDINARY`, `SPECIAL_CATEGORY` / `CRIMINAL_OFFENCE` | categoría |
| confidencialidad | `Confidentiality` | `PUBLIC`, `LOW`, `MEDIUM`, `HIGH` | categoría; un campo puede elevarla |
| integridad | `Integrity` | `LOW`, `MEDIUM`, `HIGH` | categoría; un campo puede elevarla |
| secreto | `boolean` | `false`, `true` | categoría |
| identificabilidad | `Identifiability` | `IDENTIFIED`, `PSEUDONYMIZED`, `ANONYMIZED`, `AGGREGATED` | solo el campo |

Los cuatro enumerados son `@since 0.1.0`.

## LegalNature

`io.github.devbpm.linneo.LegalNature` — qué es una categoría de datos según la normativa de protección de datos
(RGPD).

| Constante | Significado |
|---|---|
| `NONE` | No son datos personales: datos del sistema, de la organización o de referencia. |
| `ORDINARY` | Datos personales (artículo 4.1 del RGPD). |
| `SPECIAL_CATEGORY` | Categoría especial de datos personales (artículo 9 del RGPD): origen étnico o racial, opiniones políticas, convicciones religiosas o filosóficas, afiliación sindical, datos genéticos, datos biométricos, salud, vida sexual y orientación sexual. |
| `CRIMINAL_OFFENCE` | Datos personales relativos a condenas e infracciones penales (artículo 10 del RGPD). |

| Método | Descripción |
|---|---|
| `boolean isPersonal()` | `false` solo para `NONE`. |
| `boolean admits(LegalNature child)` | Si una categoría hija puede tener la naturaleza `child` cuando su padre tiene esta. |

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

```java
LegalNature.ORDINARY.admits(LegalNature.SPECIAL_CATEGORY);           // true
LegalNature.SPECIAL_CATEGORY.admits(LegalNature.CRIMINAL_OFFENCE);   // false
LegalNature.NONE.admits(LegalNature.ORDINARY);                       // false
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
| `HIGH` | La divulgación causaría un daño muy grave. |

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

Véase también: [Términos](terms.md) · [Taxonomía estándar](standard-taxonomy.md) ·
[Extender la taxonomía](extending.md) · [`@Category`](annotations/category.md) ·
curso: [La taxonomía](../course/02-the-taxonomy.md)
