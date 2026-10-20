---
title: "2. La taxonomía"
parent: "Curso"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/Taxonomy.java
  - src/main/java/io/github/devbpm/linneo/DataCategory.java
  - src/main/java/io/github/devbpm/linneo/internal/StandardTaxonomy.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-category-policy.tsv
  - src/main/java/io/github/devbpm/linneo/Categories.java
-->

# Módulo 2 — La taxonomía

## Objetivos de aprendizaje

Al terminar este módulo serás capaz de:

- describir de dónde procede la taxonomía de Linneo (fideslang más las extensiones de Linneo) y sus tres
  jerarquías;
- leer una clave con puntos y encontrar su madre y sus antepasadas;
- explicar cómo se heredan las facetas de protección y por qué una hija solo puede hacerlas más estrictas;
- usar la API `Taxonomy` para buscar, recorrer y listar categorías;
- reconocer las claves obsoletas y cómo se resuelven en su sustituta.

## De dónde vienen los términos

Linneo no inventa un vocabulario desde cero. Incluye la taxonomía por defecto completa de
[fideslang](https://github.com/ethyca/fideslang), **versión 3.1.4**, una taxonomía abierta para la ingeniería de la
privacidad, y añade términos propios donde fideslang se queda corta, sobre todo en datos no personales y datos
académicos, que son importantes para una universidad.

| | fideslang 3.1.4 | Añadidos por Linneo | Taxonomía estándar |
|---|---|---|---|
| Categorías de datos (*qué* contiene un campo) | 85 | 38 | 123 |
| Usos de datos (*para qué* se tratan) | 56 | 7 (`education.*`) | 63 |
| Interesados (*de quién* son los datos) | 15 | 6 (`student`, `applicant`, `alumnus`, `faculty_member`, `staff_member`, `legal_representative`) | 21 |

Puedes comprobar esas cifras tú mismo:

```java
System.out.println(Taxonomy.standard());
// Taxonomy[123 data categories, 63 data uses, 21 data subjects]
```

Los términos se cargan desde ficheros separados por tabuladores incluidos en el JAR. Los de fideslang se conservan
literalmente; los añadidos de Linneo están en ficheros aparte y **nunca redefinen una clave de fideslang**. Cada
término registra su **origen** (`TermInfo.origin()`): `"fideslang"`, `"linneo"` o el nombre de una extensión
(módulo 6).

Los usos de datos y los interesados describen *actividades de tratamiento*, no campos, así que no hay anotaciones
para ellos. Este curso se centra en las categorías de datos; el módulo 5 muestra cómo encajan usos e interesados en
un registro de actividades de tratamiento.

## Claves con puntos

Cada término tiene una clave en notación de puntos. La madre de una clave es la clave sin su último segmento:

```
user                                   raíz
user.contact                           madre: user
user.contact.email                     madre: user.contact
user.education.grade                   madre: user.education   (añadida por Linneo)
```

Las cuatro raíces del árbol de categorías son:

| Raíz | Origen | Naturaleza | Qué abarca |
|---|---|---|---|
| `user` | fideslang | `ORDINARY` | Datos de una persona física: nombre, contacto, identificadores, datos demográficos, salud, datos financieros, comportamiento, más los añadidos de Linneo `user.education.*`, `user.workplace.team/unit/role/assignment_period`, `user.demographic.nationality/family`, el resto del art. 9 RGPD (`user.demographic.philosophical_belief/trade_union_membership/sex_life`), `user.health_and_medical.disability`, `user.behavior.activity_log`... |
| `system` | fideslang | `NONE` | `system.authentication`, `system.operations`, más los añadidos de Linneo `system.authentication.secret`, `system.security`, `system.integrity`, `system.configuration` |
| `org` | Linneo | `NONE` | La organización y las personas jurídicas: estructura, entidades, ubicaciones, procesos, registros de negocio |
| `reference` | Linneo | `NONE` | Listas de códigos y registros públicos (`reference.public`) |

`Term.isA(ancestorKey)` indica si un término es una clave dada o una de sus descendientes. Compara segmentos
completos: `user.contact.email` *es un* `user.contact`, pero no un `user.con`.

## Constantes de claves

Rara vez escribirás las claves como cadenas. `Categories` contiene una constante por cada categoría de la taxonomía
estándar (`DataUses` y `DataSubjects` hacen lo mismo con las otras dos jerarquías):

```java
Categories.USER_CONTACT_EMAIL                  // "user.contact.email"
Categories.USER_EDUCATION_GRADE                // "user.education.grade"      (Linneo)
Categories.USER_HEALTH_AND_MEDICAL_DISABILITY  // "user.health_and_medical.disability" (Linneo)
Categories.SYSTEM_CONFIGURATION                // "system.configuration"      (Linneo)
```

Son **constantes de compilación** `public static final String`, así que pueden usarse dentro de anotaciones, y una
errata es un error de compilación y no una sorpresa en tiempo de ejecución. La clase se genera a partir de los
ficheros de la taxonomía: toda clave de la taxonomía estándar tiene constante y toda constante es una clave. La
lista completa está en la [referencia de constantes](../reference/key-constants.md).

## La protección viaja con la categoría

fideslang define claves, nombres y descripciones, pero no niveles de protección. Linneo los añade en su política de
protección (`linneo-category-policy.tsv`). Toda categoría de datos tiene cuatro facetas de protección, más atributos
libres:

| Faceta | Valores | Regla |
|---|---|---|
| `nature` | `NONE`, `ORDINARY`, `SPECIAL_CATEGORY`, `CRIMINAL_OFFENCE` | la fija la raíz; solo puede hacerse más estricta (`LegalNature.admits`, módulo 1) |
| `confidentiality` | `PUBLIC` < `LOW` < `MEDIUM` < `HIGH` | se hereda; una hija puede subirla, nunca bajarla |
| `integrity` | `LOW` < `MEDIUM` < `HIGH` | se hereda; una hija puede subirla, nunca bajarla |
| `secret` | `true` / `false` | si una categoría es secreta, todas sus hijas lo son |
| `attributes` | pares clave/valor | se heredan salvo que la hija fije su propio valor (las usan las extensiones, módulo 6) |

La política solo enumera las claves que *cambian* algo; las demás heredan de su madre. Un extracto:

```
key                               nature            confidentiality  integrity  secret
user                              ORDINARY          LOW              MEDIUM     false
user.authorization                                  HIGH             HIGH
user.authorization.password                                                     true
user.behavior                                       MEDIUM
user.criminal_history             CRIMINAL_OFFENCE  HIGH             HIGH
user.device.session_id                              HIGH                        true
user.education                                      MEDIUM           HIGH
user.financial                                      MEDIUM           HIGH
user.financial.credit_card                          HIGH
user.government_id                                  MEDIUM
user.health_and_medical           SPECIAL_CATEGORY  HIGH             HIGH
system                            NONE              LOW              LOW        false
system.authentication                               HIGH             HIGH
system.authentication.secret                                                    true
system.configuration                                                 MEDIUM
system.security                                                      HIGH
org                               NONE              PUBLIC           LOW        false
reference                         NONE              PUBLIC           MEDIUM     false
```

(La [referencia de la taxonomía estándar](../reference/standard-taxonomy.md) enumera todas las claves con sus niveles
efectivos.)

Resolvamos a mano algunas claves del modelo de la universidad:

- `user.contact.email` no declara nada; `user.contact` tampoco; así que toma las facetas de `user`:
  **ORDINARY, LOW, MEDIUM**, no secreta.
- `user.education.grade` no declara nada; `user.education` sube la confidencialidad a `MEDIUM` y la integridad a
  `HIGH`: **ORDINARY, MEDIUM, HIGH**.
- `user.health_and_medical.disability` hereda de `user.health_and_medical`: **SPECIAL_CATEGORY, HIGH, HIGH**.
- `user.authorization.password` hereda `HIGH`/`HIGH` de `user.authorization` y se declara secreta:
  **ORDINARY, HIGH, HIGH, secreta**. `user.authorization` en sí (el alcance de los permisos de una persona) no es
  secreta.
- `user.financial.credit_card` sube la confidencialidad a `HIGH` y hereda la integridad `HIGH` de `user.financial`.

Los valores que obtienes de una `DataCategory` son siempre estas facetas **efectivas**: lo que declara la categoría
o, si no declara nada, lo que hereda. La taxonomía garantiza que nunca son más débiles que las de la madre; en caso
contrario se niega a construirse (el módulo 6 muestra los mensajes de error).

```java
DataCategory grade = Taxonomy.standard().requireCategory(Categories.USER_EDUCATION_GRADE);
grade.nature();            // ORDINARY
grade.isPersonal();        // true
grade.confidentiality();   // MEDIUM
grade.integrity();         // HIGH
grade.secret();            // false
grade.attribute("retention");  // null: la taxonomía estándar no fija atributos
```

## Explorar con la API `Taxonomy`

Una `Taxonomy` es inmutable y segura entre hilos; construye una por aplicación y compártela.

| Método | Sirve para… |
|---|---|
| `Taxonomy.standard()` | obtener la taxonomía estándar (fideslang + Linneo), creada una vez y compartida |
| `Taxonomy.load()` | obtener la taxonomía estándar **más todas las extensiones registradas** (módulo 6); construye una instancia nueva en cada llamada, así que guárdala |
| `categories()` | listar todas las categorías ordenadas por clave (una madre siempre va antes que sus descendientes) |
| `category(key)` | buscar una clave; `Optional` vacío si no existe |
| `requireCategory(key)` | buscar una clave; lanza `TaxonomyException("Unknown data category '...'")` si no existe |
| `resolveCategory(key)` | buscar una clave y seguir las sustituciones de las obsoletas hasta la categoría vigente |
| `parentOf(c)`, `childrenOf(c)`, `ancestorsOf(c)` | recorrer el árbol |
| `dataUses()`, `dataUse(key)`, `requireDataUse(key)`, `childrenOf(use)` | lo mismo para los usos de datos |
| `dataSubjects()`, `dataSubject(key)`, `requireDataSubject(key)` | lo mismo para los interesados |

Explorar el subárbol académico:

```java
Taxonomy taxonomy = Taxonomy.standard();
DataCategory education = taxonomy.requireCategory(Categories.USER_EDUCATION);

for (DataCategory c : taxonomy.childrenOf(education)) {
    System.out.printf("%-36s %-9s %-7s %-7s %s%n",
            c.key(), c.nature(), c.confidentiality(), c.integrity(), c.info().origin());
}
```

```
user.education.accomplishment        ORDINARY  MEDIUM  HIGH    linneo
user.education.course_history        ORDINARY  MEDIUM  HIGH    linneo
user.education.enrollment            ORDINARY  MEDIUM  HIGH    linneo
user.education.grade                 ORDINARY  MEDIUM  HIGH    linneo
user.education.previous_education    ORDINARY  MEDIUM  HIGH    linneo
user.education.program               ORDINARY  MEDIUM  HIGH    linneo
user.education.status                ORDINARY  MEDIUM  HIGH    linneo
user.education.test_score            ORDINARY  MEDIUM  HIGH    linneo
```

Subir por el árbol:

```java
DataCategory grade = taxonomy.requireCategory(Categories.USER_EDUCATION_GRADE);
taxonomy.ancestorsOf(grade).stream().map(DataCategory::key).toList();   // [user.education, user]: la más cercana primero
```

Encontrar todas las categorías con protección reforzada:

```java
taxonomy.categories().stream()
        .filter(c -> c.nature() == LegalNature.SPECIAL_CATEGORY || c.nature() == LegalNature.CRIMINAL_OFFENCE)
        .map(DataCategory::key)
        .forEach(System.out::println);
```

## Claves obsoletas

Las aplicaciones guardan y publican las claves, así que una clave nunca se borra ni se reutiliza con otro
significado. En su lugar se marca como **obsoleta**, opcionalmente con una sustituta. `TermInfo` contiene el ciclo
de vida:

- `since()`: la versión de su origen que introdujo el término (`"2.0.0"` en los de fideslang, `"0.1.0"` en los de
  Linneo);
- `deprecatedSince()` y `replacedBy()`: presentes en un término obsoleto;
- `isDeprecated()`: `true` si alguno de los dos está presente.

`category(key)` y `requireCategory(key)` devuelven la categoría obsoleta tal cual; `resolveCategory(key)` sigue
`replacedBy` (aunque haya varios saltos) hasta la vigente.

En la taxonomía estándar de Linneo 0.1.0 **no hay ninguna categoría de datos obsoleta**. El único término obsoleto es
un uso de datos de fideslang:

```java
TermInfo info = taxonomy.requireDataUse("personalize.profiling").info();
info.isDeprecated();       // true
info.deprecatedSince();    // "2.1.1"
info.replacedBy();         // "personalize.content.profiling"
```

Las categorías obsoletas aparecen cuando una extensión declara obsoletos sus propios términos (módulo 6) o en
futuras versiones de la taxonomía. El módulo 4 muestra cómo las notifica el clasificador: como avisos, no como
errores.

## Errores frecuentes

- **Usar `Taxonomy.standard()` en un sitio y `Taxonomy.load()` en otro.** `standard()` ignora tus extensiones; si
  tu modelo usa claves de una extensión, todo debe usar `load()` (o el mismo builder).
- **Llamar a `Taxonomy.load()` una y otra vez.** Construye una taxonomía nueva en cada llamada. Constrúyela una vez y
  compártela.
- **Buscar los niveles en la clave.** `user.contact.email` no dice nada de la confidencialidad; los niveles salen de
  la política y la herencia. Léelos siempre de la `DataCategory`.
- **Suponer que cada hija declara sus niveles.** La mayoría los hereda. El nivel *efectivo* de una categoría puede
  venir de cualquier antepasada.
- **Comparar claves con `startsWith`.** `"user.contact".startsWith("user.con")` es cierto; usa `isA`, que compara
  segmentos completos.

## Ejercicios

**Ejercicio 2.1 — Niveles efectivos.** Sin ejecutar código, indica naturaleza, confidencialidad, integridad y marca
de secreto de: (a) `user.contact.address.city`; (b) `user.demographic.date_of_birth`;
(c) `user.authorization.biometric`; (d) `reference.public`; (e) `system.authentication.secret`. Después
compruébalo con `requireCategory`.

**Ejercicio 2.2 — Elegir claves.** Con la [referencia de constantes](../reference/key-constants.md) o con
`taxonomy.categories()`, elige la clave más específica para estos campos de la universidad: (a) el DNI del
estudiante; (b) el número de la Seguridad Social de un empleado; (c) la titulación en la que está matriculado un
estudiante; (d) el departamento al que pertenece un empleado; (e) la lista de países de los desplegables; (f) una
beca concedida a un estudiante.

**Ejercicio 2.3 — Categorías especiales.** Escribe el código que imprime todas las claves cuya naturaleza es
`SPECIAL_CATEGORY` o `CRIMINAL_OFFENCE`, con su origen. ¿Cuántas hay y cuáles añadió Linneo?

**Ejercicio 2.4 — Secretos.** Escribe código que liste todas las categorías secretas e indique si son personales.
Explica por qué `user.authorization.password` y `system.authentication.secret` tienen naturalezas distintas.

## Cuestionario de autoevaluación

1. ¿Cuál es la madre de `user.contact.address.city`?
2. ¿Cuántas categorías de datos tiene la taxonomía estándar y cuántas proceden de fideslang?
3. Una categoría declara confidencialidad `MEDIUM`. ¿Puede su hija declarar `LOW`?
4. ¿Qué diferencia hay entre `requireCategory` y `resolveCategory`?
5. ¿Por qué puede usarse `Categories.USER_NAME` dentro de una anotación?

## Para saber más

- [Referencia — Taxonomía](../reference/taxonomy.md) y [Términos](../reference/terms.md)
- [Referencia — Taxonomía estándar](../reference/standard-taxonomy.md): todas las claves con sus niveles efectivos
- [Referencia — Constantes de claves](../reference/key-constants.md)
- [Referencia — Niveles de protección](../reference/protection-levels.md)

---

← Anterior: [Módulo 1 — Por qué clasificar los datos](01-why-classify.md) · Siguiente:
[Módulo 3 — Anotar los modelos](03-annotating-models.md) →

<details markdown="block">
<summary><strong>Soluciones</strong></summary>

**Ejercicio 2.1**

(a) `user.contact.address.city`: nada declarado hasta llegar a `user` → ORDINARY, LOW, MEDIUM, no secreta.
(b) `user.demographic.date_of_birth`: declara confidencialidad `MEDIUM` → ORDINARY, MEDIUM, MEDIUM, no secreta.
(c) `user.authorization.biometric`: declara `SPECIAL_CATEGORY` y la marca de secreto, y hereda HIGH/HIGH de
`user.authorization` → SPECIAL_CATEGORY, HIGH, HIGH, secreta.
(d) `reference.public`: hereda de `reference` → NONE, PUBLIC, MEDIUM, no secreta.
(e) `system.authentication.secret`: hereda HIGH/HIGH de `system.authentication` y se declara secreta → NONE, HIGH,
HIGH, secreta.

**Ejercicio 2.2**

(a) `USER_GOVERNMENT_ID_NATIONAL_IDENTIFICATION_NUMBER`; (b) `USER_GOVERNMENT_ID_SOCIAL_SECURITY_NUMBER` (Linneo);
(c) `USER_EDUCATION_PROGRAM`; (d) `USER_WORKPLACE_UNIT`; (e) `REFERENCE_PUBLIC`; (f) `USER_FINANCIAL_SCHOLARSHIP`.

**Ejercicio 2.3**

```java
Taxonomy taxonomy = Taxonomy.standard();
taxonomy.categories().stream()
        .filter(c -> c.nature() == LegalNature.SPECIAL_CATEGORY || c.nature() == LegalNature.CRIMINAL_OFFENCE)
        .forEach(c -> System.out.println(c.key() + " " + c.nature() + " " + c.info().origin()));
```

Hay 19: `user.authorization.biometric`, `user.biometric` y sus cuatro hijas (`fingerprint`, `health`, `retinal`,
`voice`), `user.criminal_history` (la única `CRIMINAL_OFFENCE`), las siete categorías demográficas especiales
(`philosophical_belief`, `political_opinion`, `race_ethnicity`, `religious_belief`, `sex_life`, `sexual_orientation`,
`trade_union_membership`) y `user.health_and_medical` con `genetic`, `insurance_beneficiary_id`, `record_id` y la
`disability` de Linneo. Cuatro tienen origen `linneo`: `user.demographic.philosophical_belief`, `.sex_life` y
`.trade_union_membership`, que completan la lista del artículo 9 del RGPD, y `user.health_and_medical.disability`.

**Ejercicio 2.4**

```java
taxonomy.categories().stream()
        .filter(DataCategory::secret)
        .forEach(c -> System.out.println(c.key() + " personal=" + c.isPersonal()));
```

```
system.authentication.secret personal=false
user.authorization.biometric personal=true
user.authorization.credentials personal=true
user.authorization.password personal=true
user.device.session_id personal=true
```

Una contraseña pertenece a una persona (está bajo `user`, dato personal ordinario); una clave de API o de firma de
un sistema no pertenece a nadie (está bajo `system`, no personal). Ambas son secretos. `user.authorization` no está
en la lista: es el alcance de los permisos de una persona, confidencial pero no secreto. Un identificador de sesión
sí lo está, porque mientras la sesión es válida da acceso como una credencial.

**Cuestionario**

1. `user.contact.address`.
2. 123, de las que 85 proceden de fideslang.
3. No: una hija solo puede subir un nivel. La taxonomía se niega a construirse.
4. `requireCategory` devuelve la categoría de esa clave exacta, obsoleta o no; `resolveCategory` sigue `replacedBy`
   hasta la categoría vigente.
5. Porque es una constante de compilación (`public static final String` inicializada con un literal).

</details>
