---
title: "3. Anotar los modelos"
parent: "Curso"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/Category.java
  - src/main/java/io/github/devbpm/linneo/annotation/DefaultCategory.java
  - src/main/java/io/github/devbpm/linneo/annotation/CategoryByRow.java
  - src/main/java/io/github/devbpm/linneo/annotation/Exempt.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-category-policy.tsv
-->

# Módulo 3 — Anotar los modelos

## Objetivos de aprendizaje

Al terminar este módulo serás capaz de:

- declarar la categoría de un campo con `@Category` y subir su confidencialidad o integridad para ese campo;
- declarar la identificabilidad de un valor almacenado;
- indicar una sola vez, con un motivo, que una clase no contiene datos personales, mediante `@DefaultCategory`;
- clasificar un campo cuya categoría cambia de una fila a otra con `@CategoryByRow`;
- excluir con `@Exempt` los campos que no contienen datos, y saber qué campos se excluyen automáticamente;
- anotar correctamente records y jerarquías de clases;
- elegir la clave adecuada para un campo y justificarla.

## Las cuatro anotaciones de un vistazo

| Anotación | Dónde | Dice |
|---|---|---|
| `@Category(clave)` | campo, componente de record | «este campo contiene datos de esta categoría» |
| `@DefaultCategory(value = clave, reason = "...")` | clase | «todos los campos de esta clase sin anotación propia contienen estos datos **no personales**, porque…» |
| `@CategoryByRow(declaredBy = "...", ceiling = clave)` | campo, componente de record | «la categoría depende de cada fila; esto es lo que la decide y esto es lo más protegida que puede ser» |
| `@Exempt("motivo")` | campo, componente de record | «este campo no contiene datos del modelo, porque…» |

Las cuatro se leen por reflexión en tiempo de ejecución (`RetentionPolicy.RUNTIME`). Las lee el clasificador
(módulo 4), que nunca lee los valores de los campos.

## `@Category`: un campo, una categoría

Empecemos el modelo de la universidad con la entidad `Student`:

```java
package edu.example.university;

import io.github.devbpm.linneo.annotation.Category;
import java.time.LocalDate;

import static io.github.devbpm.linneo.Categories.*;

public class Student {

    @Category(USER_UNIQUE_ID)
    private Long id;

    @Category(USER_GOVERNMENT_ID_NATIONAL_IDENTIFICATION_NUMBER)
    private String nationalId;

    @Category(USER_NAME_FIRST)
    private String firstName;

    @Category(USER_NAME_LAST)
    private String lastName;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    @Category(USER_DEMOGRAPHIC_DATE_OF_BIRTH)
    private LocalDate dateOfBirth;

    @Category(USER_HEALTH_AND_MEDICAL_DISABILITY)
    private Integer disabilityDegree;

    @Category(USER_EDUCATION_STATUS)
    private String academicStatus;
}
```

Cada campo recibe la protección de su categoría: `disabilityDegree` es automáticamente un dato de categoría especial
con confidencialidad e integridad `HIGH`, y `email` es un dato personal ordinario con confidencialidad `LOW`
(módulo 2).

### Elementos opcionales, y por qué aparece `value =`

`@Category` tiene un elemento obligatorio, `value` (la clave), y tres opcionales:

| Elemento | Tipo | Por defecto | Significado |
|---|---|---|---|
| `identifiability` | `Identifiability[]` | `{}` → `IDENTIFIED` | cuán directamente apunta a la persona el valor almacenado; **solo para categorías personales** |
| `confidentiality` | `Confidentiality[]` | `{}` → el nivel de la categoría | una confidencialidad más estricta, solo para este campo |
| `integrity` | `Integrity[]` | `{}` → el nivel de la categoría | una integridad más estricta, solo para este campo |

Hay dos detalles de Java importantes:

1. **`value =` es obligatorio en cuanto añades otro elemento.** Java solo permite la forma abreviada
   `@Category(USER_NAME)` cuando `value` es el *único* elemento indicado. Con dos elementos hay que nombrar ambos:
   `@Category(value = USER_NAME, confidentiality = Confidentiality.HIGH)`.
2. **Los elementos opcionales son arrays** para poder distinguir «no indicado» (`{}`) de un valor. Indica como mucho
   un valor: `confidentiality = Confidentiality.HIGH`. Dos valores (`integrity = {Integrity.LOW, Integrity.HIGH}`)
   se notifican como error `AMBIGUOUS_VALUE`.

### Subir un nivel en un campo

Los niveles de una categoría son un suelo. Un campo concreto puede necesitar más. En la universidad, el servicio de
orientación guarda notas en texto libre sobre los estudiantes. El texto libre escrito por la persona o sobre ella es
`user.content.private` (confidencialidad `MEDIUM`), pero estas notas suelen contener contexto muy sensible, así que la
universidad las sube a `HIGH`:

```java
@Category(value = USER_CONTENT_PRIVATE, confidentiality = Confidentiality.HIGH)
private String counsellingNotes;
```

Los niveles solo pueden **subir**. Declarar un nivel inferior al de la categoría es un error `LOWERED_LEVEL`:

```java
@Category(value = USER_HEALTH_AND_MEDICAL_DISABILITY, confidentiality = Confidentiality.MEDIUM)  // error
private Integer disabilityDegree;
```

Declarar el *mismo* nivel que la categoría está permitido, pero no sirve de nada.

### Identificabilidad

Por defecto, un campo personal es `IDENTIFIED`. Si el valor almacenado está seudonimizado, anonimizado o agregado,
indícalo:

```java
@Category(value = USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
private String analyticsId;
```

Un campo seudonimizado sigue siendo un dato personal; uno `ANONYMIZED` o `AGGREGATED` no (módulo 1). Así es como las
tablas de estadísticas conservan su categoría real sin contar como datos personales:

```java
public record CourseStatistics(
        @Category(REFERENCE_PUBLIC) String courseCode,
        @Category(value = USER_EDUCATION_GRADE, identifiability = Identifiability.AGGREGATED) BigDecimal averageMark,
        @Category(value = USER_EDUCATION_GRADE, identifiability = Identifiability.AGGREGATED) int passed) {
}
```

La identificabilidad no tiene sentido en datos no personales: `identifiability` en una clave `system.*`, `org.*` o
`reference.*` es un error `IDENTIFIABILITY_NOT_APPLICABLE`.

## `@DefaultCategory`: una clase sin datos personales

Muchas clases no contienen ningún dato personal: configuración, listas de códigos, tablas técnicas. Anotar cada
campo sería ruido. `@DefaultCategory` hace una única **afirmación positiva**, con un motivo que un revisor puede
comprobar:

```java
@DefaultCategory(value = SYSTEM_CONFIGURATION, reason = "configuración de menús del portal del estudiante")
public class MenuGroup {

    private Long id;
    private String code;
    private String label;
    private int position;

    @Category(SYSTEM_SECURITY)          // un campo puede seguir declarando su propia categoría
    private String requiredRole;
}
```

Reglas:

- **La categoría no puede ser personal.** `@DefaultCategory(USER_CONTACT, ...)` es un error `PERSONAL_DEFAULT`: los
  datos personales siempre se declaran campo a campo, de modo que añadir una columna personal a una clase nunca
  pueda quedar cubierto en silencio por un valor por defecto.
- **El motivo no puede estar vacío** (`MISSING_REASON`).
- **La anotación propia del campo prevalece** sobre el valor por defecto (arriba, `requiredRole` es
  `system.security`).
- **No la heredan las subclases.** Cubre los campos que declara la clase anotada y —cuando esa clase es la que se
  clasifica— los campos sin anotar heredados de superclases que no tienen valor por defecto propio.

## Herencia

Los modelos JPA suelen tener una superclase común para las columnas de auditoría. En la universidad:

```java
@DefaultCategory(value = SYSTEM_OPERATIONS, reason = "marcas de tiempo técnicas de auditoría y versión de bloqueo optimista")
public abstract class AuditedEntity {

    @Category(USER_ACCOUNT_USERNAME)    // quién creó la fila: el nombre de usuario de una persona
    private String createdBy;

    private Instant createdAt;
    private Instant updatedAt;
    private long version;
}

public class Student extends AuditedEntity {
    // ... campos como antes
}
```

Al clasificar `Student`, el clasificador recorre la jerarquía (primero los campos de la superclase) y, para cada
campo sin anotación propia, busca un valor por defecto en este orden:

1. la `@DefaultCategory` de la clase que **declara** el campo (`AuditedEntity` en el caso de `createdAt`);
2. la `@DefaultCategory` de la clase **que se clasifica** (`Student`);
3. ninguna: error `UNCLASSIFIED_FIELD`.

Así, `createdAt`, `updatedAt` y `version` son `system.operations` en todas las subclases, `createdBy` mantiene su
propia `@Category`, y los campos propios de `Student` **no** quedan cubiertos por el valor por defecto de
`AuditedEntity`: cada uno necesita su anotación (o un valor por defecto en `Student`, que solo sería posible si
`Student` no contuviera datos personales).

La precedencia completa que aplica el clasificador a cada campo:

1. `@Exempt` → el campo se omite.
2. `@Category`, o el techo de `@CategoryByRow`.
3. la `@DefaultCategory` de la clase que declara el campo.
4. la `@DefaultCategory` de la clase que se clasifica.
5. si no, `UNCLASSIFIED_FIELD`.

## `@CategoryByRow`: cuando cada fila es distinta

La universidad permite a los estudiantes subir documentos: un certificado de matrícula en una fila, un informe
médico que justifica una ausencia en la siguiente, un formulario de beca en otra. La columna `content` no puede tener
una única categoría honesta. `@CategoryByRow` indica qué decide la categoría de cada fila y el **techo**
(`ceiling`): la categoría más protegida que puede contener una fila.

```java
public class Attachment extends AuditedEntity {

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)
    private Long id;

    @Category(USER_CONTENT_PRIVATE)
    private String fileName;

    @CategoryByRow(declaredBy = "el documentType del Document al que pertenece", ceiling = USER_HEALTH_AND_MEDICAL)
    private byte[] content;

    @Category(SYSTEM_INTEGRITY)         // SHA-256 del contenido, para detectar manipulaciones
    private String sha256;
}
```

Allí donde se necesita una única categoría para todo el campo —un inventario de datos, una etiqueta de
OpenMetadata— se usa el techo. Por tanto, el campo recibe la protección del techo (`SPECIAL_CATEGORY`, `HIGH`,
`HIGH`) y la identificabilidad `IDENTIFIED` cuando el techo es personal. `@CategoryByRow` no tiene elementos
`confidentiality`, `integrity` ni `identifiability`: si necesitas más protección, elige un techo más estricto.

`declaredBy` no puede estar vacío (`MISSING_REASON`). `@CategoryByRow` y `@Category` en el mismo campo es un error
`CONFLICTING_ANNOTATIONS`.

## `@Exempt`: campos que no contienen datos

Algunos campos no contienen datos del modelo: un valor calculado a partir de otros campos, una caché, un manejador
técnico, una relación con otra entidad. `@Exempt` los excluye, con un motivo:

```java
@Exempt("calculado a partir de firstName y lastName, nunca se guarda")
private String displayName;
```

```java
public class Document extends AuditedEntity {

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)
    private Long id;

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)
    private Long studentId;

    @Category(USER_CONTENT_PRIVATE)
    private String title;

    @Category(USER_CONTENT_PRIVATE)
    private String documentType;

    @Exempt("relación: cada Attachment se clasifica por separado")
    private List<Attachment> attachments;
}
```

Se excluyen automáticamente, sin anotación: los campos **`static`**, los **sintéticos** (generados por el
compilador) y los que llevan el modificador Java **`transient`**.

> **Nota sobre JPA.** `@jakarta.persistence.Transient` *no* es el modificador Java `transient`. Un campo anotado solo
> con `@Transient` de JPA sigue siendo visible para el clasificador y necesita `@Exempt` (o una categoría).

`@Exempt` no puede combinarse con `@Category` ni con `@CategoryByRow` (`CONFLICTING_ANNOTATIONS`), y su motivo no
puede estar vacío (`MISSING_REASON`).

## Records

Todas las anotaciones de campo admiten también componentes de record. En un componente de record la anotación se
aplica además al campo generado, que es lo que lee el clasificador. Mira `CourseStatistics` más arriba: no hace falta
nada más.

## Elegir la clave adecuada

Algunas pautas que funcionan bien en la práctica:

- **Clasifica lo que es el valor, no dónde está.** El correo de un empleado en la tabla `Staff` es
  `USER_CONTACT_EMAIL`, igual que el de un estudiante.
- **Usa la clave más específica que sea cierta para todas las filas.** `USER_NAME_FIRST` para el nombre de pila;
  `USER_NAME` para el nombre completo.
- **Identificadores.** La clave primaria de una tabla *sobre una persona* identifica a la persona: `USER_UNIQUE_ID`.
  Una clave ajena o el identificador de un registro sobre una persona (una matrícula, un documento) es
  `USER_UNIQUE_ID_RECORD_REFERENCE`. Los identificadores de tablas no personales los cubre el valor por defecto de la
  clase.
- **Texto libre.** Los comentarios y notas escritos por una persona o sobre ella son `USER_CONTENT_PRIVATE`; sube la
  confidencialidad si suelen contener contexto sensible.
- **Secretos.** La contraseña o el hash de contraseña de una persona es `USER_AUTHORIZATION_PASSWORD`; la clave de
  API o de firma de un sistema es `SYSTEM_AUTHENTICATION_SECRET`. Ambas son secretos.
- **Ante la duda entre dos claves, elige la más protectora**, o usa `@CategoryByRow` si de verdad cambia por fila.
- **Si nada encaja,** no fuerces una clave incorrecta: añade una con una `TaxonomyExtension` (módulo 6).

## El modelo hasta ahora

Además de `Student`, `MenuGroup`, `AuditedEntity`, `Document`, `Attachment` y `CourseStatistics`, el modelo de la
universidad tiene `Enrollment`, `Grade` y `Staff`. Los anotarás en los ejercicios. El `Student` completo:

```java
public class Student extends AuditedEntity {

    @Category(USER_UNIQUE_ID)
    private Long id;

    @Category(USER_GOVERNMENT_ID_NATIONAL_IDENTIFICATION_NUMBER)
    private String nationalId;

    @Category(USER_NAME_FIRST)
    private String firstName;

    @Category(USER_NAME_LAST)
    private String lastName;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    @Category(USER_DEMOGRAPHIC_DATE_OF_BIRTH)
    private LocalDate dateOfBirth;

    @Category(USER_HEALTH_AND_MEDICAL_DISABILITY)
    private Integer disabilityDegree;

    @Category(value = USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
    private String analyticsId;

    @Category(USER_EDUCATION_STATUS)
    private String academicStatus;

    @Category(value = USER_CONTENT_PRIVATE, confidentiality = Confidentiality.HIGH)
    private String counsellingNotes;

    @Exempt("calculado a partir de firstName y lastName, nunca se guarda")
    private String displayName;
}
```

## Errores frecuentes

- **Olvidar `value =`.** `@Category(USER_NAME, confidentiality = Confidentiality.HIGH)` no compila.
- **Poner una clave personal en `@DefaultCategory`** «para ahorrar tiempo». Se rechaza (`PERSONAL_DEFAULT`) a
  propósito.
- **Esperar que `@DefaultCategory` en una superclase cubra los campos de las subclases.** Solo cubre los campos que
  declara la clase anotada.
- **Confiar en `@Transient` de JPA.** Solo el modificador Java `transient` excluye un campo automáticamente.
- **Bajar un nivel «porque nuestros datos no son tan sensibles».** En un campo los niveles solo suben; si crees que
  el nivel estándar no es adecuado para tu organización, súbelo para todos con una extensión (módulo 6); bajarlo no es
  posible.
- **Usar `identifiability` con claves no personales** (`IDENTIFIABILITY_NOT_APPLICABLE`).
- **Eximir campos que sí contienen datos** para que pase la compilación. `@Exempt` es para campos que *no* contienen
  datos del modelo; el motivo se revisa.
- **Dejar sin anotar los campos `@Embedded` de JPA.** El clasificador no entra en los objetos embebidos: marca el
  campo embebido con `@Exempt("embebido: Address se clasifica por separado")` y clasifica la propia clase embebible
  (el módulo 4 muestra cómo incluir los embebibles).

## Ejercicios

**Ejercicio 3.1 — Enrollment y Grade.** Anota estas dos clases. Ambas extienden `AuditedEntity`.

```java
public class Enrollment extends AuditedEntity {
    private Long id;
    private Long studentId;
    private String programCode;      // p. ej. "GII", Grado en Ingeniería Informática
    private String academicYear;     // p. ej. "2026/27"
    private BigDecimal tuitionFee;
    private String scholarshipType;
}

public class Grade extends AuditedEntity {
    private Long enrollmentId;
    private String courseCode;
    private BigDecimal mark;
}
```

**Ejercicio 3.2 — Staff.** Anota `Staff`. La universidad necesita la confirmación de que el personal que trabaja con
menores no tiene condenas por delitos sexuales, y el personal inicia sesión con una contraseña local.

```java
public class Staff extends AuditedEntity {
    private Long id;
    private String fullName;
    private String workEmail;
    private String jobTitle;
    private String department;
    private String socialSecurityNumber;
    private String iban;
    private Boolean sexOffenceCertificateClear;
    private String passwordHash;
}
```

¿Qué campos son de categoría especial o datos penales? ¿Cuál es un secreto?

**Ejercicio 3.3 — Encuentra los fallos.** Cada campo tiene un problema. Identifícalo y corrígelo.

```java
@DefaultCategory(value = USER_CONTACT, reason = "")
public class ContactPreferences {

    @Category(USER_CONTACT_EMAIL, confidentiality = Confidentiality.HIGH)
    private String email;

    @Category(value = SYSTEM_CONFIGURATION, identifiability = Identifiability.PSEUDONYMIZED)
    private String theme;

    @Category(value = USER_NAME, integrity = {Integrity.MEDIUM, Integrity.HIGH})
    private String name;

    @jakarta.persistence.Transient
    private String cachedGreeting;
}
```

**Ejercicio 3.4 — ¿Por fila o no?** Para cada campo, elige entre `@Category`, `@CategoryByRow` y `@Exempt`, e indica
la clave o el motivo: (a) el cuerpo de un mensaje que los estudiantes envían a secretaría; (b) un campo de fichero en
una entidad genérica `Form` que guarda un formulario de beca o un certificado de discapacidad; (c) un `Map` usado
como caché en memoria, con el modificador Java `transient`; (d) el número de adjuntos de un documento, calculado en
el getter a partir de la lista.

## Cuestionario de autoevaluación

1. ¿Por qué hay que escribir `value =` en `@Category(value = USER_NAME, integrity = Integrity.HIGH)`?
2. ¿Qué ocurre si un campo declara `confidentiality = Confidentiality.LOW` para una categoría `MEDIUM`?
3. ¿Puede `@DefaultCategory` indicar `USER_CONTACT_EMAIL`?
4. ¿Qué protección recibe un campo `@CategoryByRow`?
5. ¿Qué campos se excluyen sin necesidad de anotación?
6. Una superclase tiene `@DefaultCategory`. ¿Cubre los campos declarados en sus subclases?

## Para saber más

- [Referencia — Anotaciones](../reference/annotations/README.md):
  [`@Category`](../reference/annotations/category.md),
  [`@DefaultCategory`](../reference/annotations/default-category.md),
  [`@CategoryByRow`](../reference/annotations/category-by-row.md),
  [`@Exempt`](../reference/annotations/exempt.md)
- [Referencia — Clasificador](../reference/classify/classifier.md): orden de resolución y exclusiones

---

← Anterior: [Módulo 2 — La taxonomía](02-the-taxonomy.md) · Siguiente:
[Módulo 4 — Verificar en los tests](04-verifying-in-tests.md) →

<details markdown="block">
<summary><strong>Soluciones</strong></summary>

**Ejercicio 3.1**

```java
public class Enrollment extends AuditedEntity {

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)     // identificador de un registro sobre un estudiante
    private Long id;

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)
    private Long studentId;

    @Category(USER_EDUCATION_PROGRAM)
    private String programCode;

    @Category(USER_EDUCATION_ENROLLMENT)
    private String academicYear;

    @Category(USER_FINANCIAL_FEE)
    private BigDecimal tuitionFee;

    @Category(USER_FINANCIAL_SCHOLARSHIP)
    private String scholarshipType;
}

public class Grade extends AuditedEntity {

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)
    private Long enrollmentId;

    @Category(USER_EDUCATION_COURSE_HISTORY)
    private String courseCode;

    @Category(USER_EDUCATION_GRADE)
    private BigDecimal mark;
}
```

Un código de titulación aislado es un dato público de referencia, pero en `Enrollment` dice *qué titulación estudia
este estudiante*: es `USER_EDUCATION_PROGRAM`. El mismo razonamiento vale para `courseCode` en `Grade`.

**Ejercicio 3.2**

```java
public class Staff extends AuditedEntity {

    @Category(USER_UNIQUE_ID)
    private Long id;

    @Category(USER_NAME)
    private String fullName;

    @Category(USER_CONTACT_EMAIL)
    private String workEmail;

    @Category(USER_JOB_TITLE)
    private String jobTitle;

    @Category(USER_WORKPLACE_UNIT)
    private String department;

    @Category(USER_GOVERNMENT_ID_SOCIAL_SECURITY_NUMBER)
    private String socialSecurityNumber;

    @Category(USER_FINANCIAL_BANK_ACCOUNT)
    private String iban;

    @Category(USER_CRIMINAL_HISTORY)
    private Boolean sexOffenceCertificateClear;

    @Category(USER_AUTHORIZATION_PASSWORD)
    private String passwordHash;
}
```

`sexOffenceCertificateClear` es un dato penal (`CRIMINAL_OFFENCE`, art. 10 RGPD): incluso un booleano derivado del
certificado dice algo sobre condenas. Ningún campo es de categoría especial. `passwordHash` es un secreto (la
política estándar marca `user.authorization.password` como secreta).

**Ejercicio 3.3**

- Clase: `USER_CONTACT` es personal (`PERSONAL_DEFAULT`) y el motivo está vacío (`MISSING_REASON`). Quita el valor
  por defecto y anota los campos personales uno a uno.
- `email`: no compila; necesita `value = USER_CONTACT_EMAIL`.
- `theme`: identificabilidad en una clave no personal (`IDENTIFIABILITY_NOT_APPLICABLE`); quita `identifiability`.
- `name`: dos valores de integridad (`AMBIGUOUS_VALUE`); deja uno, p. ej. `integrity = Integrity.HIGH`.
- `cachedGreeting`: el `@Transient` de JPA no lo excluye y, sin valor por defecto, es `UNCLASSIFIED_FIELD`. Añade
  `@Exempt("saludo calculado, nunca se guarda")` o hazlo `transient` de Java.

**Ejercicio 3.4**

(a) `@Category(USER_CONTENT_PRIVATE)` (sube la confidencialidad si los mensajes suelen contener detalles sensibles).
(b) `@CategoryByRow(declaredBy = "el tipo de formulario", ceiling = USER_HEALTH_AND_MEDICAL)`: un certificado de
discapacidad es un dato de salud. (c) Nada: los campos `transient` de Java se excluyen automáticamente. (d) Si es un
campo, `@Exempt("recuento derivado de attachments")`; si solo es un getter sin campo, no hay nada que clasificar: el
clasificador lee campos, no métodos.

**Cuestionario**

1. Java solo permite la forma abreviada de un elemento cuando `value` es el único elemento indicado.
2. Un error `LOWERED_LEVEL`; los niveles solo pueden subir.
3. No: es personal (`PERSONAL_DEFAULT`).
4. La protección de su techo, e `IDENTIFIED` si el techo es personal.
5. Los campos `static`, los sintéticos y los `transient` de Java.
6. No. Cubre los campos que declara la clase anotada (y, cuando esa clase es la que se clasifica, los campos sin
   anotar heredados de superclases sin valor por defecto propio).

</details>
