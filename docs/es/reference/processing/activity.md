---
title: "ProcessingActivity"
parent: "Tratamientos"
grand_parent: "Referencia"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/Governance.java
  - src/main/java/io/github/devbpm/linneo/processing/Transfer.java
  - src/main/java/io/github/devbpm/linneo/processing/ImpactScreening.java
-->

# ProcessingActivity

`io.github.devbpm.linneo.processing.ProcessingActivity` — `public record`, `@since 0.1.0`

```java
public record ProcessingActivity(String key, String name, String description, List<String> purposes,
                                 List<String> dataSubjects, List<String> dataCategories, List<Class<?>> entities,
                                 List<String> objects, Set<ImpactScreening.Factor> riskFactors,
                                 Governance governance)
```

Una actividad de tratamiento del responsable, tal como se declara en su registro de actividades de tratamiento (RGPD,
artículo 30.1). La actividad solo **guarda lo declarado**: su constructor no busca las claves en una taxonomía, no
clasifica las entidades ni comprueba las bases. Esa es la tarea del [`ProcessingChecker`](checker.md). Se construye
con [`builder(key, name)`](#builder).

Una actividad tiene dos partes con responsables distintos:

- su **alcance** es lo que el código de la aplicación sabe con certeza: las finalidades a las que sirve, los
  interesados y los datos personales que trata (`purposes`, `dataSubjects`, `dataCategories`, `entities`, `objects`), y
  los factores de riesgo que el código conoce (`riskFactors`);
- su [**gobernanza**](#governance) es lo que decide y mantiene al día la función de protección de datos del
  responsable: las bases jurídicas, la referencia legal, el plazo de conservación, los destinatarios, las
  transferencias, la evaluación de impacto y los factores de riesgo que declara para el
  [cribado](impact-screening.md) de la necesidad de una.

El código puede dar valores iniciales para la gobernanza con el builder. Una aplicación cuyo delegado de protección de
datos los edita guarda la gobernanza en su propio almacenamiento y la combina con el alcance declarado en el código
mediante [`withGovernance`](#withgovernance).

## Componentes

| Componente | Artículo | Valor por defecto del builder | Descripción |
|---|---|---|---|
| `key` | — | obligatorio | Un identificador estable, único dentro del registro: letras, dígitos, puntos, guiones bajos y guiones, empezando por una letra o un dígito (`[A-Za-z0-9][A-Za-z0-9._-]*`). Dos actividades con la misma clave son un error `DUPLICATE_ACTIVITY`. |
| `name` | — | obligatorio | Un nombre legible, el encabezado de la actividad en el registro. No puede estar en blanco; se le quitan los espacios sobrantes. |
| `description` | — | `null` | Qué hace la actividad. |
| `purposes` | 30.1.b | vacía | Claves de los usos de datos a los que sirve la actividad, preferiblemente constantes de `DataUses`. |
| `dataSubjects` | 30.1.c | vacía | Claves de las categorías de interesados, preferiblemente constantes de `DataSubjects`. |
| `dataCategories` | 30.1.c | vacía | Claves de categorías de datos personales tratadas además de las de `entities`, preferiblemente constantes de `Categories`. |
| `entities` | 30.1.c | vacía | Clases del modelo clasificadas; la actividad trata las categorías de sus campos personales. |
| `objects` | 30.1.c | vacía | Claves de objetos de un [inventario de datos](../classify/inventory.md); la actividad trata las categorías de sus campos personales. El [`ProcessingChecker`](checker.md#métodos) debe crearse con ese inventario. |
| `riskFactors` | 35 | vacío | Lo que el código sabe que hace la actividad y que el [cribado](impact-screening.md) no puede ver por sus términos, como `Factor.AUTOMATED_DECISIONS`: un mínimo al que la gobernanza añade y que no puede quitar. `null` es un conjunto vacío. |
| `governance` | 6.1, 6.3, 9.2, 10, 30.1.d–f, 35 | `Governance.NONE` | Las bases jurídicas, la referencia legal, el plazo de conservación, los destinatarios, las transferencias, la evaluación de impacto y los factores de riesgo que declara el delegado; consulta [Governance](#governance). `null` significa `Governance.NONE`. |

Los componentes de la gobernanza se leen a través de ella: `activity.governance().legalBasis()`,
`activity.governance().retention()`, etcétera. El cribado lee los factores de riesgo de ambas, `allRiskFactors()`.

### Constructor canónico

- `NullPointerException` si `key` o `name` es `null`.
- `IllegalArgumentException` si la `key` no cumple `[A-Za-z0-9][A-Za-z0-9._-]*` (por tanto se rechaza una clave en
  blanco, o con espacios u otros símbolos), con el mensaje `Invalid processing activity key '<key>': use letters,
  digits, dots, underscores and hyphens, starting with a letter or digit`. A la clave no se le quitan los espacios.
- `IllegalArgumentException` con el mensaje `The processing activity '<key>' needs a name` si el `name` está en
  blanco. A un nombre válido se le **quitan** los espacios iniciales y finales.
- Las listas se copian con `List.copyOf`: el resultado es inmutable, y una lista `null` o un elemento `null` lanzan
  `NullPointerException`. Los elementos se guardan tal cual; el comprobador notifica una clave en blanco dentro de una
  lista como clave desconocida. La excepción es `objects`: una lista `null` es una lista vacía.
- Una `description` en blanco pasa a ser `null` y a cualquier otro texto se le quitan los espacios iniciales y
  finales.
- `riskFactors` se copia en un conjunto inmutable; `null` es uno vacío.
- Una `governance` `null` pasa a ser [`Governance.NONE`](#governance).

```java
ProcessingActivity activity = ProcessingActivity.builder("newsletter", "Alumni newsletter")
        .description("  Monthly news for former students.  ")
        .retention("   ")
        .build();
activity.description();               // "Monthly news for former students."
activity.governance().retention();    // null
```

### withGovernance

`ProcessingActivity withGovernance(Governance governance)` devuelve la misma actividad con otra gobernanza, como la que
mantiene al día el delegado de protección de datos. El alcance declarado en el código se conserva tal cual, con sus
`riskFactors`, y la actividad original no cambia. La gobernanza simplemente **sustituye** a la actual, sin combinarse:
lo que el código dice que hace la actividad, como las decisiones automatizadas, forma parte del alcance, y el cribado lo
lee junto con los factores de la gobernanza (`allRiskFactors()`), así que una gobernanza guardada antes de que cambiara
el código nunca levanta una evaluación de impacto. `NullPointerException` si `governance` es `null`; para vaciarla, pasa
`Governance.NONE` (los factores que declara el código se mantienen).

`Set<ImpactScreening.Factor> allRiskFactors()` devuelve los factores de riesgo que lee el cribado: la unión de los del
alcance y los de la gobernanza, inmutable.

## Builder

`ProcessingActivity.Builder` — `public static final class`, `@since 0.1.0`

| Miembro | Efecto |
|---|---|
| `static Builder builder(String key, String name)` | Inicia una declaración. La clave y el nombre se comprueban al llamar a `build()`. |
| `Builder description(String description)` | Fija la descripción. |
| `Builder purposes(String... keys)` | **Añade** claves de usos de datos. |
| `Builder dataSubjects(String... keys)` | **Añade** claves de sujetos de datos. |
| `Builder dataCategories(String... keys)` | **Añade** claves de categorías de datos, para datos personales que no declara ninguna entidad de la lista. |
| `Builder entities(Class<?>... types)` | **Añade** clases del modelo clasificadas. |
| `Builder objects(String... keys)` | **Añade** claves de objetos de un [inventario de datos](../classify/inventory.md): la actividad trata las categorías de sus campos personales. El comprobador debe crearse con ese inventario. |
| `Builder governance(Governance governance)` | **Sustituye** la gobernanza declarada hasta ahora por `governance`; los métodos de gobernanza que la siguen parten de ella. `NullPointerException` si `governance` es `null`. |
| `Builder legalBasis(LegalBasis basis)` | Fija la base jurídica; `null` la borra. |
| `Builder legitimateInterest(String interest)` | Fija la base en `LEGITIMATE_INTERESTS` **y** el interés perseguido. |
| `Builder specialCategoryConditions(SpecialCategoryCondition... conditions)` | **Añade** condiciones del artículo 9.2: una por cada tipo de dato especial que la actividad trata con una condición distinta. |
| `Builder criminalDataBasis(CriminalDataBasis basis)` | Fija la base del artículo 10. |
| `Builder legalReference(String reference)` | Fija la norma en la que descansan las bases. |
| `Builder retention(String retention)` | Fija el plazo o la regla de conservación. |
| `Builder recipients(String... recipients)` | **Añade** categorías de destinatarios. |
| `Builder transfer(String destination, TransferSafeguard safeguard)` | **Añade** una `Transfer`. Un destino en blanco lanza `IllegalArgumentException` en ese momento, no en `build()`. |
| `Builder impactAssessment(String reference)` | Fija la referencia a la evaluación de impacto. |
| `Builder riskFactors(ImpactScreening.Factor... factors)` | **Añade** factores de riesgo que conoce el código, como `Factor.AUTOMATED_DECISIONS` cuando el código decide por sí solo, para el [cribado](impact-screening.md) de la necesidad de una evaluación de impacto. Forman parte del alcance: la gobernanza añade los suyos y no puede quitar estos. |
| `ProcessingActivity build()` | Crea la actividad con el constructor canónico. |

Los métodos de `legalBasis` a `impactAssessment` son los del [`Governance.Builder`](#governancebuilder): dan los
valores iniciales de la gobernanza de la actividad. `riskFactors` no es uno de ellos: alimenta el alcance.

Cada método de lista, `specialCategoryConditions` y `riskFactors` añaden a lo indicado antes
(`purposes("a").purposes("b", "c")` da `[a, b, c]`); los demás métodos sustituyen el valor anterior, así que gana la
última llamada. `governance(...)` sustituye todo lo que
declararon antes los métodos de gobernanza, y los que van después cambian la gobernanza que dio:

```java
ProcessingActivity.builder("a", "A")
        .retention("One year")
        .governance(stored)          // conservación de la actividad: la de `stored`, no "One year"
        .recipients("Auditors")      // se añade a los destinatarios de `stored`
        .build();
```

`build()` puede llamarse más de una vez, y un builder puede conservarse para derivar variantes de una actividad.

`legitimateInterest(interest)` es la forma habitual de declarar el interés legítimo: fija ambos componentes. Como gana
la última llamada, un `legalBasis(...)` posterior sustituye la base pero **conserva el interés**:

```java
ProcessingActivity.builder("newsletter", "Alumni newsletter")
        .legitimateInterest("Keeping in touch with former students")
        .legalBasis(LegalBasis.CONSENT)        // basis CONSENT, the interest is still set
        .build();
```

El comprobador lo notifica con el aviso `UNNEEDED_CONDITION` (`A legitimate interest is stated but the lawful basis
is article 6.1.a`), y el [registro](record.md) omite el interés, porque solo muestra el interés legítimo cuando la
base es `LEGITIMATE_INTERESTS`. Llamar a `legalBasis(LegalBasis.LEGITIMATE_INTERESTS)` sin indicar el interés es el
aviso `MISSING_LEGITIMATE_INTEREST`.

Una declaración completa:

```java
ProcessingActivity admissions = ProcessingActivity.builder("admissions", "Admissions")
        .description("Receives and assesses applications, including requests for the disability quota.")
        .purposes(DataUses.EDUCATION_ADMISSION)
        .dataSubjects(DataSubjects.APPLICANT)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL, USER_EDUCATION_PREVIOUS_EDUCATION,
                USER_EDUCATION_TEST_SCORE, USER_HEALTH_AND_MEDICAL_DISABILITY)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .specialCategoryConditions(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
        .legalReference("Higher Education Act, arts. 12 and 31")
        .retention("Until the end of the admission cycle; admitted applicants move to the student record")
        .recipients("Admissions board")
        .impactAssessment("DPIA-2026-01")
        .build();
```

(`USER_NAME` y las demás constantes de categoría vienen de `import static io.github.devbpm.linneo.Categories.*;`.)

## Governance

`io.github.devbpm.linneo.processing.Governance` — `public record`, `@since 0.1.0`

```java
public record Governance(LegalBasis legalBasis, String legitimateInterest,
                         Set<SpecialCategoryCondition> specialCategoryConditions,
                         CriminalDataBasis criminalDataBasis,
                         String legalReference, String retention, List<String> recipients, List<Transfer> transfers,
                         String impactAssessment, Set<ImpactScreening.Factor> riskFactors)
```

La parte de una actividad de tratamiento que decide y mantiene al día la función de protección de datos del
responsable: por qué es lícito el tratamiento, cuánto tiempo se conservan los datos, quién los recibe, adónde van, la
evaluación de impacto y lo que el delegado sabe del riesgo y el código no. Está separada del alcance de la actividad
(finalidades, interesados y datos personales) porque ambos tienen responsables distintos. Los problemas que el
[comprobador](checker.md) encuentra en esta parte son los de los [elementos](checker.md#element) cuyo `isGovernance()`
es `true`.

| Componente | Artículo | Por defecto | Descripción |
|---|---|---|---|
| `legalBasis` | 6.1 | `null` | La base jurídica; consulta [`LegalBasis`](bases.md#legalbasis). `null` significa que no se ha declarado. |
| `legitimateInterest` | 6.1.f | `null` | El interés perseguido, cuando la base es `LEGITIMATE_INTERESTS`. |
| `specialCategoryConditions` | 9.2 | vacío | Las condiciones para las categorías especiales de datos, una por cada tipo de dato especial tratado con una condición distinta, como el consentimiento explícito para datos de salud y una obligación de derecho laboral para la afiliación sindical; consulta [`SpecialCategoryCondition`](bases.md#specialcategorycondition). |
| `criminalDataBasis` | 10 | `null` | Lo que permite tratar datos penales; consulta [`CriminalDataBasis`](bases.md#criminaldatabasis). |
| `legalReference` | 6.3 | `null` | La norma de la Unión o del Estado miembro en la que descansan las bases, como texto libre. Un solo texto cubre todas las bases de la actividad. |
| `retention` | 30.1.f | `null` | El plazo previsto para la supresión, como texto libre. |
| `recipients` | 30.1.d | vacía | Las categorías de destinatarios, como texto libre. |
| `transfers` | 30.1.e | vacía | Transferencias a terceros países u organizaciones internacionales; consulta [`Transfer`](#transfer). |
| `impactAssessment` | 35 | `null` | Una referencia a la evaluación de impacto relativa a la protección de datos, como su identificador o su ubicación. |
| `riskFactors` | 35 | vacío | Lo que el delegado sabe de la actividad y el código no, como su escala, para el [cribado](impact-screening.md#declarar-factores-de-riesgo) de la necesidad de una evaluación de impacto. |

El constructor canónico **normaliza** los textos opcionales `legitimateInterest`, `legalReference`, `retention` e
`impactAssessment`: un texto en blanco pasa a ser `null` y a cualquier otro se le quitan los espacios iniciales y
finales. Los textos de `recipients` no se normalizan. Las listas se copian (inmutables); una lista `null` es una lista
vacía y un elemento `null` lanza `NullPointerException`. `specialCategoryConditions` y `riskFactors` se copian en
conjuntos inmutables; `null` es un conjunto vacío.

| Miembro | Descripción |
|---|---|
| `static final Governance NONE` | Gobernanza sin nada declarado todavía: todos los componentes `null` o vacíos. Es la gobernanza de una actividad a la que no se dio ninguna. |
| `static Builder builder()` | Empieza a declarar una gobernanza desde cero. |
| `Builder toBuilder()` | Un builder que parte de estos valores, para cambiar una parte: los métodos de lista, `specialCategoryConditions` y `riskFactors` añaden a lo que hay y los demás métodos sustituyen el valor. La gobernanza en sí no cambia. |

### Governance.Builder

`Governance.Builder` — `public static final class`, `@since 0.1.0`. Tiene los mismos métodos, con el mismo efecto, que
los métodos de gobernanza del [`ProcessingActivity.Builder`](#builder): `legalBasis`, `legitimateInterest`,
`specialCategoryConditions(SpecialCategoryCondition...)`, `criminalDataBasis`, `legalReference`, `retention`,
`recipients`, `transfer` e `impactAssessment`, más `Governance build()`. Su propio
`riskFactors(ImpactScreening.Factor...)` declara los factores de riesgo de la gobernanza, los que conoce el delegado, a
diferencia de los que declara el código. Los métodos de lista, `specialCategoryConditions` y `riskFactors` añaden a lo
indicado antes y los demás métodos sustituyen el valor anterior. `build()` puede llamarse más de una vez.

### Alcance en el código, gobernanza desde el almacenamiento

Una aplicación puede declarar el alcance en el código y leer la gobernanza de donde la edita su delegado de protección
de datos, y combinar ambos antes de comprobar, por ejemplo cada vez que el delegado guarda un cambio:

```java
// En el código: lo que la aplicación sabe con certeza.
ProcessingActivity support = ProcessingActivity.builder("learning-support", "Learning support")
        .purposes(DataUses.EDUCATION_TEACHING)
        .dataSubjects(DataSubjects.STUDENT)
        .dataCategories(USER_NAME, USER_HEALTH_AND_MEDICAL_DISABILITY)
        .build();

// En el almacenamiento del delegado: lo que decide la función de protección de datos.
Governance stored = Governance.builder()
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .specialCategoryConditions(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
        .legalReference("Higher Education Act, arts. 12 and 31")
        .retention("Until the end of the studies")
        .recipients("Teaching staff")
        .impactAssessment("DPIA-2026-07")
        .build();

ProcessingChecker checker = new ProcessingChecker(Taxonomy.load());
checker.check(support).violations();                          // 3: sin base, sin condición, sin conservación
checker.check(support.withGovernance(stored)).violations();   // []
```

Las tres infracciones de la primera comprobación son todas de gobernanza: sus elementos son `LEGAL_BASIS`,
`SPECIAL_CATEGORY_CONDITION` y `RETENTION`. No hay ninguna sobre la evaluación de impacto: los datos de categorías
especiales son un criterio de su [cribado](impact-screening.md), y un solo criterio no basta para pedir una evaluación.
Consulta [qué hacer con el elemento de una infracción](checker.md#element).

## Transfer

`io.github.devbpm.linneo.processing.Transfer` — `public record`, `@since 0.1.0`

```java
public record Transfer(String destination, TransferSafeguard safeguard)
```

Una transferencia de datos personales a un tercer país o a una organización internacional (RGPD, artículo 30.1.e).

| Componente | Descripción |
|---|---|
| `destination` | El tercer país o la organización internacional, como texto libre. No se le quitan los espacios. |
| `safeguard` | Lo que permite la transferencia; consulta [`TransferSafeguard`](bases.md#transfersafeguard). |

El constructor lanza `NullPointerException` si algún componente es `null`, e `IllegalArgumentException` con el
mensaje `A transfer needs a destination` si `destination` está en blanco. El comprobador no examina las
transferencias; el registro las enumera con el artículo y el nombre fideslang de su garantía.

Véase también: [Bases jurídicas](bases.md) · [ProcessingChecker](checker.md) · [ProcessingRecord](record.md) ·
[Actividades de tratamiento](README.md)
