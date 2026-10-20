---
title: "Taxonomía estándar"
parent: "Referencia"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/internal/StandardTaxonomy.java
  - src/main/resources/io/github/devbpm/linneo/internal/fideslang-data-categories.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/fideslang-data-uses.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/fideslang-data-subjects.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-categories.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-uses.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-data-subjects.tsv
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-category-policy.tsv
  - tools/fideslang/update.py
-->

# Taxonomía estándar

Es la taxonomía que devuelve `Taxonomy.standard()` (y la base de `Taxonomy.load()` y de
`TaxonomyBuilder.withStandard()`). La construye `io.github.devbpm.linneo.internal.StandardTaxonomy`, que está en un
paquete no exportado y no forma parte de la API pública, a partir de recursos separados por tabuladores incluidos en
el JAR bajo `io/github/devbpm/linneo/internal/`.

## Contenido

| | fideslang 3.1.4 | Linneo | Total |
|---|---|---|---|
| Categorías de datos | 85 | 38 | 123 |
| Usos de datos | 56 | 7 | 63 |
| Sujetos de datos | 15 | 6 | 21 |

La versión de fideslang es `StandardTaxonomy.FIDESLANG_VERSION` (`"3.1.4"`). Los términos de fideslang se incluyen
sin cambios (licencia CC BY 4.0, © Ethyca, Inc.; ver `NOTICE`) y tienen origen `fideslang`; los de Linneo tienen
origen `linneo` y nunca redefinen una clave de fideslang. fideslang no define niveles de protección: todos proceden
del fichero de política de Linneo.

Para consultar todas las claves, usa las constantes de [`Categories`, `DataUses` y `DataSubjects`](key-constants.md)
o recorre `Taxonomy.categories()`, `dataUses()` y `dataSubjects()`.

## Raíces de las categorías de datos

Cada raíz declara las cuatro facetas en `linneo-category-policy.tsv`:

| Raíz | Origen | Naturaleza | Confidencialidad | Integridad | Secreta | Abarca |
|---|---|---|---|---|---|---|
| `user` | fideslang | `ORDINARY` | `LOW` | `MEDIUM` | no | Datos sobre una persona física |
| `system` | fideslang | `NONE` | `LOW` | `LOW` | no | Datos del sistema: autenticación, operación, seguridad, configuración |
| `org` | Linneo | `NONE` | `PUBLIC` | `LOW` | no | La organización y terceros que no son personas físicas |
| `reference` | Linneo | `NONE` | `PUBLIC` | `MEDIUM` | no | Datos maestros de fuentes públicas o estándar |

## Política de protección

Por debajo de las raíces, la política eleva las facetas donde los datos lo requieren; una clave que no aparece hereda
de su padre (ver [Niveles de protección](protection-levels.md#regla-de-herencia)). Las reglas principales:

| Categorías | Política |
|---|---|
| `user.health_and_medical`, `user.biometric`, `user.demographic.philosophical_belief`, `.political_opinion`, `.race_ethnicity`, `.religious_belief`, `.sex_life`, `.sexual_orientation`, `.trade_union_membership` | `SPECIAL_CATEGORY`, confidencialidad `HIGH`, integridad `HIGH` |
| `user.criminal_history` | `CRIMINAL_OFFENCE`, `HIGH`, `HIGH` |
| `user.authorization` (alcance de los permisos) | confidencialidad `HIGH`, integridad `HIGH`; `user.authorization.credentials`, `.password` y `.biometric` son **secretas**, y `user.authorization.biometric` además es `SPECIAL_CATEGORY` |
| `user.device.session_id` | confidencialidad `HIGH`, **secreta** |
| `system.authentication` | confidencialidad `HIGH`, integridad `HIGH`; `system.authentication.secret` es **secreta** |
| `user.financial`, `user.payment`, `user.education` | confidencialidad `MEDIUM`, integridad `HIGH`; `user.financial.credit_card` confidencialidad `HIGH` |
| `user.behavior`, `user.childrens`, `user.government_id`, `user.location.precise`, `user.content.private`, `user.content.self_image`, varias `user.demographic.*` (fecha de nacimiento, género, estado civil, perfil, familia, nacionalidad) | confidencialidad `MEDIUM` |
| `user.behavior.activity_log`, `system.security`, `system.integrity` | integridad `HIGH` |
| `system.configuration` | integridad `MEDIUM` |
| `org.structure`, `org.location`, `org.process` | confidencialidad `LOW`, integridad `MEDIUM` |
| `org.legal_entity` | integridad `MEDIUM` |
| `org.business_records` | confidencialidad `MEDIUM`, integridad `MEDIUM` |

Las hijas de una categoría especial son también categoría especial (por ejemplo,
`user.health_and_medical.disability`), y toda descendiente de una categoría secreta es secreta.

## Añadidos de Linneo

**Categorías de datos** (38, todas con `since` `0.1.0`):

| Padre | Hijas añadidas |
|---|---|
| `user` | `user.education` con `program`, `enrollment`, `course_history`, `grade`, `test_score`, `previous_education`, `status`, `accomplishment` |
| `user.unique_id` | `record_reference` |
| `user.government_id` | `social_security_number` |
| `user.device` | `user_agent`, `session_id` |
| `user.workplace` | `team`, `unit`, `role`, `assignment_period` |
| `user.demographic` | `nationality`, `family`, `philosophical_belief`, `trade_union_membership`, `sex_life` |
| `user.financial` | `scholarship`, `fee` |
| `user.health_and_medical` | `disability` |
| `user.behavior` | `activity_log` |
| `system` | `system.security`, `system.integrity`, `system.configuration` |
| `system.authentication` | `secret` |
| (raíz) | `org` con `structure`, `legal_entity`, `location`, `process`, `business_records` |
| (raíz) | `reference` con `public` |

**Usos de datos** (7): la raíz `education` ("Provides educational services") con `education.admission`,
`education.enrollment`, `education.teaching`, `education.assessment`, `education.academic_records` y
`education.financial_aid`.

**Sujetos de datos** (6, todos raíces): `student`, `applicant`, `alumnus`, `faculty_member`, `staff_member`,
`legal_representative`.

## Raíces de fideslang

- Categorías de datos: `user`, `system`.
- Usos de datos: `analytics`, `collect`, `employment`, `essential`, `finance`, `functional`, `marketing`,
  `operations`, `personalize`, `sales`, `third_party_sharing`, `train_ai_system`.
- Sujetos de datos: 15 términos planos, como `customer`, `employee`, `job_applicant`, `patient`, `visitor`.

fideslang 3.1.4 tiene un único término obsoleto: el uso de datos `personalize.profiling` (obsoleto desde `2.1.1`,
sustituido por `personalize.content.profiling`). Ninguna categoría de datos estándar está obsoleta.

## Ficheros de recursos y formatos

| Fichero | Origen | Columnas |
|---|---|---|
| `fideslang-data-categories.tsv`, `fideslang-data-uses.tsv`, `fideslang-data-subjects.tsv` | `fideslang` | formato de términos (7) |
| `linneo-data-categories.tsv`, `linneo-data-uses.tsv`, `linneo-data-subjects.tsv` | `linneo` | formato de términos (7) |
| `linneo-category-policy.tsv` | — | formato de política (5) |

Reglas comunes (UTF-8): se ignoran las líneas en blanco y las que empiezan por `#`; la primera línea restante es la
cabecera de columnas y se salta; las celdas se separan con un tabulador y se recortan los espacios de los extremos;
las celdas finales que faltan se consideran vacías; una línea con más celdas que columnas se rechaza
(`TaxonomyException`: `<file>: too many columns in line '...'`). Si falta un recurso se lanza `TaxonomyException`
(`Missing taxonomy resource <file>`).

**Formato de términos**: `key`, `parent_key`, `name`, `description`, `version_added`, `version_deprecated`,
`replaced_by`.

- `parent_key` debe ser la clave sin su último segmento (vacío para una raíz); si no, `TaxonomyException`
  (`<file>: '<key>' declares parent '<p>' but its key implies '<q>'`).
- Un `version_added` no vacío pasa a ser `TermInfo.since`.
- Un `version_deprecated` no vacío declara obsoleto el término, con `replaced_by` como sustituto (o ninguno si está
  vacío). `replaced_by` se ignora cuando `version_deprecated` está vacío.
- En los ficheros de fideslang, un `version_added` vacío se escribe como `2.0.0`, el valor que asigna el
  `default_factory` de fideslang.

**Formato de política**: `key`, `nature`, `confidentiality`, `integrity`, `secret`.

- Celda vacía = heredar. Los valores no vacíos son nombres de constantes de los enumerados (`ORDINARY`, `HIGH`…);
  `secret` es `true` o `false`.
- Cada fila se aplica con `TaxonomyBuilder.refineCategory` una vez cargados todos los términos, así que la clave debe
  existir.

Orden de carga: categorías, usos y sujetos de fideslang (origen `fideslang`), luego categorías, usos y sujetos de
Linneo (origen `linneo`) y, por último, la política. Después, `TaxonomyBuilder.build()` comprueba la jerarquía y
resuelve las facetas heredadas.

## Regenerar desde fideslang

Los ficheros `fideslang-*.tsv` y las clases de constantes son generados; no los edites a mano. Desde la raíz del
repositorio:

```bash
python -I tools/fideslang/update.py 3.1.4
```

El script (solo usa la biblioteca estándar de Python) descarga de PyPI la distribución de fuentes de esa versión,
lee `src/fideslang/default_taxonomy/data_{categories,uses,subjects}.py` y escribe:

- `src/main/resources/io/github/devbpm/linneo/internal/fideslang-data-{categories,uses,subjects}.tsv`, con un
  comentario de cabecera que indica la fuente, la versión y la licencia;
- `src/main/java/io/github/devbpm/linneo/{Categories,DataUses,DataSubjects}.java`: las claves de fideslang y después
  las de los ficheros `linneo-*.tsv` (ver [Constantes de claves](key-constants.md)).

Tras añadir un término de Linneo a un fichero `linneo-*.tsv`, vuelve a ejecutar el script para regenerar las
constantes. Al pasar a otra versión de fideslang, actualiza también `StandardTaxonomy.FIDESLANG_VERSION`, `NOTICE` y
los recuentos de términos que comprueba `StandardTaxonomyTest`, y asegúrate de que cada nueva categoría raíz tiene
una fila completa en `linneo-category-policy.tsv` (si no, la taxonomía no se construye).

Véase también: [Taxonomía](taxonomy.md) · [Niveles de protección](protection-levels.md) ·
[Constantes de claves](key-constants.md) · [Extender la taxonomía](extending.md) ·
curso: [La taxonomía](../course/02-the-taxonomy.md)
