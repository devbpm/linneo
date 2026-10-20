---
title: "5. Usar la clasificación"
parent: "Curso"
nav_order: 5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/ClassClassification.java
  - src/main/java/io/github/devbpm/linneo/classify/FieldClassification.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - src/main/java/io/github/devbpm/linneo/processing/package-info.java
-->

# Módulo 5 — Usar la clasificación

## Objetivos de aprendizaje

Al terminar este módulo serás capaz de:

- obtener la clasificación de una clase con `classify` y leer una `ClassClassification`;
- leer una `FieldClassification`: categoría, origen, niveles efectivos, identificabilidad y marcas de dato personal y
  de secreto;
- listar los campos personales de un modelo y construir un inventario de datos;
- montar un informe al estilo de un registro de actividades de tratamiento (RAT) a partir de tu propia descripción de
  los procesos y de la clasificación;
- basar decisiones de enmascaramiento (por ejemplo, en los logs) en la clasificación, con fallo seguro.

## De las anotaciones a los datos

El test del módulo 4 garantiza que el modelo está completamente clasificado. El mismo `Classifier` te da también el
resultado como datos:

```java
Classifier classifier = new Classifier(Taxonomy.load());

ClassClassification student = classifier.classify(Student.class);       // una clase
List<ClassClassification> all = classifier.classifyAll(entities);      // varias, sin excepción
List<ClassClassification> checked = classifier.verify(entities);      // varias, lanza si hay errores
```

En el código de la aplicación es preferible `verify` (o comprobar `isValid()`): los campos con un error, en general,
**faltan** en el resultado, así que un informe construido a partir de una clasificación inválida los omitiría sin
avisar.

## `ClassClassification`

| Miembro | Devuelve |
|---|---|
| `type()` | la clase |
| `fields()` | una `FieldClassification` por campo clasificado, **primero los de la superclase**, en orden de declaración |
| `violations()` | todos los problemas encontrados, errores y avisos |
| `isValid()` | `true` si no hay errores (los avisos se permiten) |
| `errors()` | solo los errores |
| `personalFields()` | los campos cuyo `isPersonal()` es `true` |
| `holdsPersonalData()` | si algún campo es personal |
| `field(name)` | la clasificación de un campo, o vacío si no existe, está excluido (`@Exempt`, `static`, `transient`) o tiene un error |

```java
student.holdsPersonalData();                              // true
student.field("displayName");                             // Optional.empty: @Exempt
student.field("email").orElseThrow().category().key();    // "user.contact.email"
```

## `FieldClassification`

| Componente | Significado |
|---|---|
| `declaringClass()` | la clase que declara el campo (`AuditedEntity` en el caso de `createdBy`) |
| `field()` | el nombre del campo |
| `category()` | la `DataCategory`, ya resuelta en su sustituta si la clave era obsoleta |
| `basis()` | de dónde viene la categoría: `DECLARED` (`@Category`), `CLASS_DEFAULT` (`@DefaultCategory`), `BY_ROW` (techo de `@CategoryByRow`) |
| `reason()` | el motivo de un valor por defecto de clase, o el `declaredBy` de un campo por fila; `null` en `DECLARED` |
| `confidentiality()`, `integrity()` | los niveles **efectivos**: los de la categoría o los más estrictos que declare el campo |
| `identifiability()` | `IDENTIFIED`, `PSEUDONYMIZED`, `ANONYMIZED` o `AGGREGATED`; `null` si la categoría no es personal |
| `isPersonal()` | categoría personal **y** almacenada `IDENTIFIED` o `PSEUDONYMIZED` |
| `isSecret()` | la marca de secreto de la categoría |

`toString()` produce una línea compacta, útil en logs y aserciones:

```java
student.fields().forEach(System.out::println);
```

```
AuditedEntity.createdBy = user.account.username (DECLARED)
AuditedEntity.createdAt = system.operations (CLASS_DEFAULT)
AuditedEntity.updatedAt = system.operations (CLASS_DEFAULT)
AuditedEntity.version = system.operations (CLASS_DEFAULT)
Student.id = user.unique_id (DECLARED)
Student.nationalId = user.government_id.national_identification_number (DECLARED)
Student.firstName = user.name.first (DECLARED)
Student.lastName = user.name.last (DECLARED)
Student.email = user.contact.email (DECLARED)
Student.dateOfBirth = user.demographic.date_of_birth (DECLARED)
Student.disabilityDegree = user.health_and_medical.disability (DECLARED)
Student.analyticsId = user.unique_id.pseudonymous (DECLARED)
Student.academicStatus = user.education.status (DECLARED)
Student.counsellingNotes = user.content.private (DECLARED)
```

Algunos valores que conviene observar:

- `counsellingNotes`: `confidentiality()` es `HIGH` (subida en el campo), aunque `user.content.private` es `MEDIUM`.
- `analyticsId`: `identifiability()` es `PSEUDONYMIZED` e `isPersonal()` es `true`.
- `CourseStatistics.averageMark`: categoría `user.education.grade` (personal), pero `identifiability()` es
  `AGGREGATED`, así que `isPersonal()` es `false` y el campo no está en `personalFields()`.
- `Attachment.content`: `basis()` es `BY_ROW`, `reason()` es `"el documentType del Document al que pertenece"` y la
  categoría es `user.health_and_medical` (el techo).
- `Staff.passwordHash`: `isSecret()` es `true`.

## Un inventario de datos

Un inventario de datos responde a «dónde tenemos datos personales, de qué tipo y con qué protección». Con la
clasificación, es un bucle:

```java
Classifier classifier = new Classifier(Taxonomy.load());
List<ClassClassification> model = classifier.verify(List.of(
        MenuGroup.class, Student.class, Enrollment.class, Grade.class,
        Staff.class, Document.class, Attachment.class, CourseStatistics.class));

System.out.println("entity;field;category;nature;confidentiality;integrity;identifiability;secret");
for (ClassClassification entity : model) {
    for (FieldClassification field : entity.personalFields()) {
        DataCategory category = field.category();
        System.out.println(String.join(";",
                entity.type().getSimpleName(),
                field.field(),
                category.key(),
                category.nature().name(),
                field.confidentiality().name(),
                field.integrity().name(),
                field.identifiability().name(),
                Boolean.toString(field.isSecret())));
    }
}

model.stream()
        .filter(c -> c.personalFields().stream()
                .anyMatch(f -> f.category().nature() != LegalNature.ORDINARY))
        .forEach(c -> System.out.println("Protección reforzada (art. 9/10): " + c.type().getSimpleName()));
```

Un extracto de la salida:

```
entity;field;category;nature;confidentiality;integrity;identifiability;secret
Student;createdBy;user.account.username;ORDINARY;LOW;MEDIUM;IDENTIFIED;false
Student;id;user.unique_id;ORDINARY;LOW;MEDIUM;IDENTIFIED;false
...
Student;disabilityDegree;user.health_and_medical.disability;SPECIAL_CATEGORY;HIGH;HIGH;IDENTIFIED;false
Student;analyticsId;user.unique_id.pseudonymous;ORDINARY;LOW;MEDIUM;PSEUDONYMIZED;false
...
Staff;sexOffenceCertificateClear;user.criminal_history;CRIMINAL_OFFENCE;HIGH;HIGH;IDENTIFIED;false
Staff;passwordHash;user.authorization.password;ORDINARY;HIGH;HIGH;IDENTIFIED;true
...
Attachment;content;user.health_and_medical;SPECIAL_CATEGORY;HIGH;HIGH;IDENTIFIED;false
Protección reforzada (art. 9/10): Student
Protección reforzada (art. 9/10): Staff
Protección reforzada (art. 9/10): Attachment
```

Aquí `identifiability()` nunca es `null`, porque `personalFields()` solo contiene categorías personales.
`CourseStatistics` no aparece: sus campos son datos de referencia o agregados.

Los nombres de campo no son nombres de columna: tradúcelos con las reglas de nombrado de tu capa de persistencia si el
inventario debe hablar de tablas y columnas.

## Un informe tipo RAT

El art. 30 RGPD obliga a los responsables a llevar un registro de las actividades de tratamiento: para cada
actividad, entre otras cosas, sus fines, las categorías de interesados y las categorías de datos personales. **Los
usos de datos y los interesados describen procesos, no campos**, por lo que no hay anotaciones para ellos. Linneo
modela las actividades de tratamiento en el paquete `io.github.devbpm.linneo.processing`, al que se dedica el
[módulo 9](09-processing-activities.md). Aquí, para ver cómo encajan las piezas, esbozas las actividades en tu propio
código con ambos vocabularios (`DataUses`, `DataSubjects`) y dejas que la clasificación complete las categorías:

```java
record ActivitySketch(String name, String purpose, List<String> subjects, List<Class<?>> entities) {
}

Taxonomy taxonomy = Taxonomy.load();
Classifier classifier = new Classifier(taxonomy);
List<ActivitySketch> activities = List.of(
        new ActivitySketch("Evaluación y calificación", DataUses.EDUCATION_ASSESSMENT,
                List.of(DataSubjects.STUDENT, DataSubjects.FACULTY_MEMBER),
                List.of(Enrollment.class, Grade.class)),
        new ActivitySketch("Administración de personal", DataUses.EMPLOYMENT,
                List.of(DataSubjects.EMPLOYEE, DataSubjects.STAFF_MEMBER),
                List.of(Staff.class)));

for (ActivitySketch activity : activities) {
    System.out.println("## " + activity.name());
    System.out.println("Finalidad: " + taxonomy.requireDataUse(activity.purpose()).name());
    activity.subjects().forEach(s -> System.out.println("Interesado: " + taxonomy.requireDataSubject(s).name()));
    TreeSet<String> categories = new TreeSet<>();
    for (Class<?> entity : activity.entities()) {
        classifier.classify(entity).personalFields().stream()
                .map(FieldClassification::category)
                .forEach(c -> categories.add(c.key() + " (" + c.nature() + ")"));
    }
    categories.forEach(c -> System.out.println("Categoría: " + c));
}
```

La salida de la segunda actividad:

```
## Administración de personal
Finalidad: Employment
Interesado: Employee
Interesado: Staff Member
Categoría: user.account.username (ORDINARY)
Categoría: user.authorization.password (ORDINARY)
Categoría: user.contact.email (ORDINARY)
Categoría: user.criminal_history (CRIMINAL_OFFENCE)
Categoría: user.financial.bank_account (ORDINARY)
Categoría: user.government_id.social_security_number (ORDINARY)
Categoría: user.job_title (ORDINARY)
Categoría: user.name (ORDINARY)
Categoría: user.unique_id (ORDINARY)
Categoría: user.workplace.unit (ORDINARY)
```

(Los nombres de los términos, como `Employment` o `Staff Member`, vienen de la taxonomía y están en inglés.)

`requireDataUse` y `requireDataSubject` lanzan una `TaxonomyException` ante una clave desconocida, así que una errata
en la lista de actividades se detecta en cuanto se ejecuta el informe. El resultado es materia prima para el delegado
de protección de datos, no un documento jurídico: faltan las bases jurídicas, los destinatarios, los plazos de
conservación y las transferencias. El módulo 9 los declara con `ProcessingActivity`, los comprueba con
`ProcessingChecker` y genera el registro a partir de esas mismas declaraciones.

## Decisiones de enmascaramiento en los logs

Los logs son una fuga clásica: el `toString()` de una entidad acaba en una traza de error con el grado de discapacidad
de un estudiante. La clasificación permite decidir, campo a campo, qué puede aparecer en claro. El diseño seguro es una
**lista de permitidos**: calcula los campos que *pueden* registrarse y enmascara todo lo demás, incluidos los campos
que no conoces.

```java
/** Decide qué campos pueden aparecer en claro en los logs de la aplicación. Todo lo demás se enmascara. */
public final class LogMasking {

    private final Classifier classifier;
    private final Map<Class<?>, Set<String>> loggable = new ConcurrentHashMap<>();

    public LogMasking(Classifier classifier) {
        this.classifier = classifier;
    }

    /** Los campos de una clase que pueden registrarse en claro: una lista de permitidos, así que lo desconocido queda enmascarado. */
    public Set<String> loggableFields(Class<?> type) {
        return loggable.computeIfAbsent(type, t -> classifier.classify(t).fields().stream()
                .filter(LogMasking::mayBeLogged)
                .map(FieldClassification::field)
                .collect(Collectors.toUnmodifiableSet()));
    }

    static boolean mayBeLogged(FieldClassification field) {
        if (field.isSecret()) {
            return false;                                   // nunca, ni siquiera enmascarado
        }
        if (!field.isPersonal()) {
            return true;                                    // no personal: datos del sistema, agregados
        }
        return field.identifiability() == Identifiability.PSEUDONYMIZED
                && field.confidentiality() == Confidentiality.LOW;  // por ejemplo, un id de correlación
    }
}
```

Para `Student`, `loggableFields` devuelve `[analyticsId, createdAt, updatedAt, version]`; para `Staff`,
`[createdAt, updatedAt, version]`. La política en sí (qué permite tu organización en los logs) es tuya; la
clasificación la hace explícita, comprobable y aplicada automáticamente a los campos nuevos. Como un campo sin
clasificar, o con una clave desconocida o un nivel incorrecto, falta en `fields()`, nunca está en la lista de
permitidos: el diseño falla de forma segura.

Tu código de logging lee después los valores de los campos permitidos (con getters, un filtro del serializador, etc.)
e imprime un marcador para el resto. Linneo nunca lee valores.

## Errores frecuentes

- **Construir informes con `classify` sin comprobar `isValid()`.** Faltan los campos con errores; usa `verify` o
  compruébalo antes.
- **Usar `category().isPersonal()` en lugar de `isPersonal()`.** El primero ignora la identificabilidad: una media
  agregada contaría como personal. Usa `FieldClassification.isPersonal()` para saber si «este valor almacenado es un
  dato personal».
- **Usar los niveles de la categoría en lugar de los del campo.** `field.confidentiality()` incluye la subida
  declarada en el campo; `field.category().confidentiality()` no.
- **Una lista de denegados para enmascarar** («enmascara estos campos»). Los campos personales nuevos se filtran hasta
  que alguien actualice la lista.
- **Esperar que Linneo conozca los fines del tratamiento.** Los usos de datos y los interesados son vocabularios para
  que los uses donde describes tus procesos.

## Ejercicios

**Ejercicio 5.1 — Protección reforzada.** Imprime todos los campos del modelo cuya categoría sea `SPECIAL_CATEGORY` o
`CRIMINAL_OFFENCE` y cuyo valor sea personal, con el formato `Clase.campo (clave)`.

**Ejercicio 5.2 — Inventario por categoría.** Genera, para cada clave de categoría usada por algún campo personal del
modelo, la lista de `Clase.campo` que la usan, ordenada por clave.

**Ejercicio 5.3 — Tres tratamientos.** Sustituye `loggableFields` por un método
`Map<String, Treatment> treatments(Class<?>)`, donde `Treatment` es `CLEAR`, `MASK` u `OMIT`: los secretos se omiten
(no aparece ni el nombre del campo), los campos personales se enmascaran y el resto va en claro. ¿Qué debe hacer tu
código de logging con un campo que **no** está en el mapa?

**Ejercicio 5.4 — Explica.** Para `CourseStatistics`, ¿qué devuelven `holdsPersonalData()`, `personalFields()` y
`field("averageMark").orElseThrow().identifiability()`? ¿Por qué?

## Cuestionario de autoevaluación

1. ¿En qué orden enumera `fields()` los campos de `Student`?
2. ¿Qué devuelven `basis()` y `reason()` para `Student.createdAt`?
3. ¿Cuándo es `null` `identifiability()`?
4. ¿Es personal según `isPersonal()` un campo `PSEUDONYMIZED`?
5. ¿Por qué es más segura una lista de permitidos que una de denegados para enmascarar?

## Para saber más

- [Referencia — Resultados](../reference/classify/results.md): `ClassClassification` y `FieldClassification`
- [Referencia — Clasificador](../reference/classify/classifier.md)
- [Referencia — Términos](../reference/terms.md): usos de datos e interesados

---

← Anterior: [Módulo 4 — Verificar en los tests](04-verifying-in-tests.md) · Siguiente:
[Módulo 6 — Extender la taxonomía](06-extending-the-taxonomy.md) →

<details markdown="block">
<summary><strong>Soluciones</strong></summary>

**Ejercicio 5.1**

```java
for (ClassClassification c : model) {
    c.personalFields().stream()
            .filter(f -> f.category().nature() != LegalNature.ORDINARY)
            .forEach(f -> System.out.println(
                    c.type().getSimpleName() + "." + f.field() + " (" + f.category().key() + ")"));
}
```

```
Student.disabilityDegree (user.health_and_medical.disability)
Staff.sexOffenceCertificateClear (user.criminal_history)
Attachment.content (user.health_and_medical)
```

(`personalFields()` solo contiene categorías personales, así que `!= ORDINARY` significa categoría especial o dato
penal.)

**Ejercicio 5.2**

```java
Map<String, List<String>> byCategory = new TreeMap<>();
for (ClassClassification c : model) {
    for (FieldClassification f : c.personalFields()) {
        byCategory.computeIfAbsent(f.category().key(), k -> new ArrayList<>())
                .add(c.type().getSimpleName() + "." + f.field());
    }
}
byCategory.forEach((key, fields) -> System.out.println(key + " -> " + fields));
```

Observa que `user.account.username` enumera `createdBy` una vez por cada entidad que extiende `AuditedEntity`: los
campos heredados pertenecen a cada clase concreta.

**Ejercicio 5.3**

```java
enum Treatment { CLEAR, MASK, OMIT }

Map<String, Treatment> treatments(Class<?> type) {
    return classifier.classify(type).fields().stream().collect(Collectors.toUnmodifiableMap(
            FieldClassification::field,
            f -> f.isSecret() ? Treatment.OMIT : f.isPersonal() ? Treatment.MASK : Treatment.CLEAR));
}
```

Un campo que no está en el mapa debe tratarse como `MASK` (u `OMIT`): puede ser un campo con errores o uno añadido
después de calcular el mapa. Nunca debe tratarse por defecto como `CLEAR`.

**Ejercicio 5.4**

`holdsPersonalData()` es `false`, `personalFields()` está vacía y la identificabilidad es `AGGREGATED`. La categoría
de calificaciones es personal, pero los valores almacenados resumen a muchos estudiantes, así que
`FieldClassification.isPersonal()` es `false`.

**Cuestionario**

1. Primero los campos de la superclase (los de `AuditedEntity`) y después los de `Student`, cada uno en orden de
   declaración.
2. `CLASS_DEFAULT`, con el motivo de la `@DefaultCategory` de `AuditedEntity`:
   `"marcas de tiempo técnicas de auditoría y versión de bloqueo optimista"`.
3. Cuando la categoría no es personal.
4. Sí. Solo `ANONYMIZED` y `AGGREGATED` no lo son.
5. Todo lo que no está permitido explícitamente —incluidos los campos nuevos y los que tienen errores— queda
   enmascarado.

</details>
