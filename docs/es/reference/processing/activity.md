---
title: "ProcessingActivity"
parent: "Tratamientos"
grand_parent: "Referencia"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/Transfer.java
-->

# ProcessingActivity

`io.github.devbpm.linneo.processing.ProcessingActivity` — `public record`, `@since 0.1.0`

```java
public record ProcessingActivity(String key, String name, String description, List<String> purposes,
                                 List<String> dataSubjects, List<String> dataCategories, List<Class<?>> entities,
                                 LegalBasis legalBasis, String legitimateInterest,
                                 SpecialCategoryCondition specialCategoryCondition,
                                 CriminalDataBasis criminalDataBasis, String legalReference, String retention,
                                 List<String> recipients, List<Transfer> transfers, String impactAssessment)
```

Una actividad de tratamiento del responsable, tal como se declara en su registro de actividades de tratamiento (RGPD,
artículo 30.1). La actividad solo **guarda lo declarado**: su constructor no busca las claves en una taxonomía, no
clasifica las entidades ni comprueba las bases. Esa es la tarea del [`ProcessingChecker`](checker.md). Se construye
con [`builder(key, name)`](#builder).

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
| `legalBasis` | 6.1 | `null` | La base jurídica; consulta [`LegalBasis`](bases.md#legalbasis). `null` significa que no se ha declarado. |
| `legitimateInterest` | 6.1.f | `null` | El interés perseguido, cuando la base es `LEGITIMATE_INTERESTS`. |
| `specialCategoryCondition` | 9.2 | `null` | La condición para las categorías especiales de datos; consulta [`SpecialCategoryCondition`](bases.md#specialcategorycondition). |
| `criminalDataBasis` | 10 | `null` | Lo que permite tratar datos penales; consulta [`CriminalDataBasis`](bases.md#criminaldatabasis). |
| `legalReference` | 6.3 | `null` | La norma de la Unión o del Estado miembro en la que descansan las bases, como texto libre. Un solo texto cubre todas las bases de la actividad. |
| `retention` | 30.1.f | `null` | El plazo previsto para la supresión, como texto libre. |
| `recipients` | 30.1.d | vacía | Las categorías de destinatarios, como texto libre. |
| `transfers` | 30.1.e | vacía | Transferencias a terceros países u organizaciones internacionales; consulta [`Transfer`](#transfer). |
| `impactAssessment` | 35 | `null` | Una referencia a la evaluación de impacto relativa a la protección de datos, como su identificador o su ubicación. |

### Constructor canónico

- `NullPointerException` si `key` o `name` es `null`.
- `IllegalArgumentException` si la `key` no cumple `[A-Za-z0-9][A-Za-z0-9._-]*` (por tanto se rechaza una clave en
  blanco, o con espacios u otros símbolos), con el mensaje `Invalid processing activity key '<key>': use letters,
  digits, dots, underscores and hyphens, starting with a letter or digit`. A la clave no se le quitan los espacios.
- `IllegalArgumentException` con el mensaje `The processing activity '<key>' needs a name` si el `name` está en
  blanco. A un nombre válido se le **quitan** los espacios iniciales y finales.
- Las listas se copian con `List.copyOf`: el resultado es inmutable, y una lista `null` o un elemento `null` lanzan
  `NullPointerException`. Los elementos se guardan tal cual; el comprobador notifica una clave en blanco dentro de una
  lista como clave desconocida.
- Los textos opcionales `description`, `legitimateInterest`, `legalReference`, `retention` e `impactAssessment` se
  **normalizan**: un texto en blanco pasa a ser `null` y a cualquier otro se le quitan los espacios iniciales y
  finales. Los textos de `recipients` no se normalizan.

```java
ProcessingActivity activity = ProcessingActivity.builder("newsletter", "Alumni newsletter")
        .description("  Monthly news for former students.  ")
        .retention("   ")
        .build();
activity.description();   // "Monthly news for former students."
activity.retention();     // null
```

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
| `Builder legalBasis(LegalBasis basis)` | Fija la base jurídica; `null` la borra. |
| `Builder legitimateInterest(String interest)` | Fija la base en `LEGITIMATE_INTERESTS` **y** el interés perseguido. |
| `Builder specialCategoryCondition(SpecialCategoryCondition condition)` | Fija la condición del artículo 9.2. |
| `Builder criminalDataBasis(CriminalDataBasis basis)` | Fija la base del artículo 10. |
| `Builder legalReference(String reference)` | Fija la norma en la que descansan las bases. |
| `Builder retention(String retention)` | Fija el plazo o la regla de conservación. |
| `Builder recipients(String... recipients)` | **Añade** categorías de destinatarios. |
| `Builder transfer(String destination, TransferSafeguard safeguard)` | **Añade** una `Transfer`. Un destino en blanco lanza `IllegalArgumentException` en ese momento, no en `build()`. |
| `Builder impactAssessment(String reference)` | Fija la referencia a la evaluación de impacto. |
| `ProcessingActivity build()` | Crea la actividad con el constructor canónico. |

Cada método de lista añade a lo indicado antes (`purposes("a").purposes("b", "c")` da `[a, b, c]`); los demás
métodos sustituyen el valor anterior, así que gana la última llamada. `build()` puede llamarse más de una vez, y un
builder puede conservarse para derivar variantes de una actividad.

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
        .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
        .legalReference("Higher Education Act, arts. 12 and 31")
        .retention("Until the end of the admission cycle; admitted applicants move to the student record")
        .recipients("Admissions board")
        .impactAssessment("DPIA-2026-01")
        .build();
```

(`USER_NAME` y las demás constantes de categoría vienen de `import static io.github.devbpm.linneo.Categories.*;`.)

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
