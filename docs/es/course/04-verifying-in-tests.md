---
title: "4. Verificar en los tests"
parent: "Curso"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - src/main/java/io/github/devbpm/linneo/classify/ClassificationException.java
  - src/main/java/io/github/devbpm/linneo/classify/Violation.java
  - src/main/java/io/github/devbpm/linneo/classify/ModelClasses.java
-->

# Módulo 4 — Verificar en los tests

## Objetivos de aprendizaje

Al terminar este módulo serás capaz de:

- escribir un test JUnit que haga fallar la compilación cuando el modelo de datos no esté clasificado por completo y
  correctamente;
- leer una `ClassificationException` y localizar cada problema;
- reconocer cada tipo de `Violation` y corregirlo;
- gestionar los avisos por claves obsoletas;
- alimentar el clasificador con las clases de entidad de una aplicación JPA, o con todas las clases de un paquete para
  que no se olvide ninguna.

## Por qué un test

Las anotaciones por sí solas son documentación. Lo que las convierte en un control es un test que se ejecuta en cada
compilación: cuando alguien añade `mobilePhone` a `Student` y se olvida de clasificarlo, la compilación falla y la
pull request muestra una corrección de una línea que revisar. Es el principio de fallo seguro del módulo 1, aplicado
por la integración continua.

## El test

`Classifier` recibe una `Taxonomy` y comprueba clases contra ella. `verify` clasifica una colección de clases y lanza
una `ClassificationException` si **cualquiera** de ellas tiene **algún** error:

```java
package edu.example.university;

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.ClassClassification;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

class DataClassificationTest {

    private static final Taxonomy TAXONOMY = Taxonomy.load();

    @Test
    void everyEntityIsClassified() {
        List<ClassClassification> classifications = new Classifier(TAXONOMY).verify(List.of(
                MenuGroup.class, Student.class, Enrollment.class, Grade.class,
                Staff.class, Document.class, Attachment.class));

        // verify() solo lanza por errores; los avisos (claves obsoletas) siguen en el resultado.
        classifications.stream()
                .flatMap(c -> c.violations().stream())
                .map(Violation::toString)
                .forEach(System.out::println);
    }
}
```

Puntos a tener en cuenta:

- Usa **`Taxonomy.load()`**, para que el test vea las mismas extensiones (módulo 6) que la aplicación. Con
  `Taxonomy.standard()`, las claves añadidas por tu extensión se notificarían como desconocidas.
- `verify` notifica a la vez los errores de **todas** las clases, no solo los de la primera, así que una única
  ejecución te da la lista completa de tareas.
- Si no hay errores, `verify` devuelve la `ClassClassification` de cada clase, en el orden recibido. El módulo 5 usa
  ese resultado.
- `Classifier` es inmutable y seguro entre hilos; puedes guardarlo en un campo estático. Solo lee anotaciones a través
  de `Class.getDeclaredFields()`, nunca valores de campos, así que no necesita una base de datos en marcha ni
  `setAccessible`.
- La lista de este test está escrita a mano, así que una clase que nadie añada a ella no se verifica nunca. «No
  olvidar una clase», más abajo, toma las clases del propio modelo.

Si prefieres examinar una sola clase sin que se lance nada, usa `classify(Class)` y sus métodos `isValid()`,
`errors()` y `violations()` (módulo 5).

## Leer la excepción

Supón que alguien añade dos campos a `Student`:

```java
@Category("user.contact.mail")          // errata
private String personalEmail;

private String mobilePhone;             // olvidado
```

El test falla con:

```
io.github.devbpm.linneo.classify.ClassificationException: 2 classification error(s):
 - ERROR UNKNOWN_CATEGORY edu.example.university.Student.personalEmail: unknown data category 'user.contact.mail'
 - ERROR UNCLASSIFIED_FIELD edu.example.university.Student.mobilePhone: no @Category on the field and no @DefaultCategory on Student
```

Cada línea es una `Violation`, impresa como:

```
<GRAVEDAD> <CÓDIGO> <clase con nombre completo>[.<path>]: <mensaje>
```

- La clase es la que **se está clasificando**, aunque el campo esté declarado en una superclase.
- La ruta es el nombre del campo (`campo.parte` para un campo de un `@Composite` que contiene la clase, módulo 3).
  Falta cuando el problema está en una anotación de clase (`@DefaultCategory`).
- El mensaje nombra la clave o la anotación implicada.

El mensaje lista los 50 primeros errores. Si hay más, una última línea los cuenta todos por código
(`... and 70 more; all of them by code: LOWERED_LEVEL 12, UNCLASSIFIED_FIELD 108`), de modo que un modelo grande no
entierre el log de la compilación. `ClassificationException.errors()` siempre contiene todos los errores.

Desde código, `ClassificationException.errors()` devuelve esas mismas `Violation`, cada una con `type()`, `path()`,
`severity()`, `code()` y `message()`. Es útil para agruparlas o contarlas:

```java
try {
    classifier.verify(entities);
} catch (ClassificationException e) {
    Map<Violation.Code, Long> byCode = e.errors().stream()
            .collect(Collectors.groupingBy(Violation::code, TreeMap::new, Collectors.counting()));
    System.out.println(byCode);
    throw e;
}
```

## Cada tipo de infracción y cómo corregirla

Los mensajes de abajo son exactamente los que produce Linneo (con `X` para la clase y `k` para una clave).

| Código | Gravedad | Mensaje | Corrección |
|---|---|---|---|
| `UNCLASSIFIED_FIELD` | error | `no @Category on the field and no @DefaultCategory on X` (o `on Superclase or X` para un campo heredado) | Añade `@Category` (o `@CategoryByRow`/`@DerivedFrom`/`@Exempt`), o una `@DefaultCategory` no personal si la clase no contiene datos personales; en una clase embebida, márcala `@Composite` |
| `UNKNOWN_CATEGORY` | error | `unknown data category 'k'` | Corrige la clave (usa las constantes de `Categories`) o asegúrate de que se carga la extensión que la define (`Taxonomy.load()`) |
| `PERSONAL_DEFAULT` | error | `@DefaultCategory of X names 'k', which is personal data: declare personal fields one by one with @Category` | Quita el valor por defecto y anota los campos personales uno a uno |
| `MISSING_REASON` | error | `@DefaultCategory of X needs a reason`, `@Exempt needs a reason` o `@CategoryByRow needs declaredBy` | Escribe el motivo para el revisor |
| `CONFLICTING_ANNOTATIONS` | error | `@Exempt cannot be combined with @Category, @CategoryByRow or @DerivedFrom` o `@CategoryByRow cannot be combined with @Category or @DerivedFrom` | Deja solo una (`@Category` con `@DerivedFrom` está permitido) |
| `LOWERED_LEVEL` | error | `confidentiality LOW is lower than the MEDIUM of 'k'` (o `integrity ...`) | Quita el elemento o súbelo; los niveles solo suben |
| `AMBIGUOUS_VALUE` | error | `confidentiality must have at most one value` (o `integrity`, `identifiability`) | Indica un solo valor |
| `IDENTIFIABILITY_NOT_APPLICABLE` | error | `'k' is not personal data and has no identifiability` | Quita `identifiability` o comprueba que la clave es la correcta |
| `DUPLICATE_FIELD` | error | `hides the field of the same name declared by Superclase; rename one of them` | Renombra uno de los dos campos: un campo se identifica por su nombre, así que un campo de una subclase no puede reutilizar el nombre de uno de la superclase |
| `UNKNOWN_SOURCE` | error | `@DerivedFrom needs at least one source` o `the source 's' is not a classified field of X` o `the source 's' has an error` | Nombra un campo clasificado como fuente; un campo `@Exempt` o con un error no puede ser fuente |
| `AMBIGUOUS_DERIVATION` | error | `its sources [...] have different classifications; declare the category of the derived value with @Category` | Añade `@Category` al campo derivado |
| `WEAKER_THAN_SOURCE` | error | `'k' protects less than its source(s) [...]; a value derived from data needs at least its protection, unless it is declared anonymized or aggregated` | Declara una categoría que proteja al menos tanto, o quita `@Category` para tomar la de la fuente |
| `WEAKER_THAN_PARTS` | error | `'k' protects less than the fields of X need (...)` | Declara una categoría que cubra todos los campos del compuesto guardado como un solo valor |
| `CYCLE` | error | `the composite X contains itself; classify the field as one value with @Category or @CategoryByRow` o `is derived from itself through other derived fields` | Clasifica como un solo valor el campo que cierra el bucle, o rompe la derivación circular |
| `DEPRECATED_CATEGORY` | **aviso** | `data category 'k' is deprecated; use 'r'` | Sustituye la clave por la sugerida |

Esta es una clase rota a propósito y la salida real de `verify`:

```java
public class BadFields {
    @Category("user.contact.mail") private String email;
    @Category(value = USER_HEALTH_AND_MEDICAL_DISABILITY, confidentiality = Confidentiality.MEDIUM)
    private Integer disabilityDegree;
    @Category(value = SYSTEM_CONFIGURATION, identifiability = Identifiability.PSEUDONYMIZED) private String theme;
    @Category(value = USER_NAME, integrity = {Integrity.MEDIUM, Integrity.HIGH}) private String name;
    @Exempt("") @Category(USER_CONTACT_EMAIL) private String email2;
    @Category(USER_NAME) @CategoryByRow(declaredBy = "el tipo", ceiling = USER_HEALTH_AND_MEDICAL) private String both;
    @CategoryByRow(declaredBy = "", ceiling = USER_HEALTH_AND_MEDICAL) private byte[] blob;
}
```

```
8 classification error(s):
 - ERROR UNKNOWN_CATEGORY edu.example.university.BadFields.email: unknown data category 'user.contact.mail'
 - ERROR LOWERED_LEVEL edu.example.university.BadFields.disabilityDegree: confidentiality MEDIUM is lower than the HIGH of 'user.health_and_medical.disability'
 - ERROR IDENTIFIABILITY_NOT_APPLICABLE edu.example.university.BadFields.theme: 'system.configuration' is not personal data and has no identifiability
 - ERROR AMBIGUOUS_VALUE edu.example.university.BadFields.name: integrity must have at most one value
 - ERROR CONFLICTING_ANNOTATIONS edu.example.university.BadFields.email2: @Exempt cannot be combined with @Category, @CategoryByRow or @DerivedFrom
 - ERROR MISSING_REASON edu.example.university.BadFields.email2: @Exempt needs a reason
 - ERROR CONFLICTING_ANNOTATIONS edu.example.university.BadFields.both: @CategoryByRow cannot be combined with @Category or @DerivedFrom
 - ERROR MISSING_REASON edu.example.university.BadFields.blob: @CategoryByRow needs declaredBy
```

Y un problema a nivel de clase:

```java
@DefaultCategory(value = USER_CONTACT, reason = "")
public class ContactPreferences {
    private String channel;
}
```

```
2 classification error(s):
 - ERROR MISSING_REASON edu.example.university.ContactPreferences: @DefaultCategory of ContactPreferences needs a reason
 - ERROR PERSONAL_DEFAULT edu.example.university.ContactPreferences: @DefaultCategory of ContactPreferences names 'user.contact', which is personal data: declare personal fields one by one with @Category
```

Fíjate en que `channel` no se notifica por separado: el valor por defecto de la clase es incorrecto, así que el campo
queda sin clasificar y el problema se notifica una vez, en la clase.

### Campos heredados

Si `AuditedEntity` no tuviera `@DefaultCategory`, su campo `createdAt` se notificaría en **cada** subclase,
nombrando ambas clases:

```
 - ERROR UNCLASSIFIED_FIELD edu.example.university.Student.createdAt: no @Category on the field and no @DefaultCategory on AuditedEntity or Student
```

La corrección va en la superclase (anotar el campo o dar a `AuditedEntity` un valor por defecto), no en cada subclase.

## Avisos: claves obsoletas

Una clave obsoleta sigue funcionando: el campo se clasifica con su **sustituta** y se registra un `WARNING`. Los avisos
no hacen que `verify` lance la excepción. La taxonomía estándar de Linneo 0.1.0 no tiene categorías de datos
obsoletas, así que lo verás cuando tu propia extensión declare obsoleto un término (módulo 6) o cuando lo haga una
versión futura de la taxonomía. Por ejemplo, con la extensión del campus del módulo 6, en la que
`user.education.lab_journal` se sustituye por `user.education.lab_notebook`:

```
WARNING DEPRECATED_CATEGORY edu.example.university.LabNotebook.file: data category 'user.education.lab_journal' is deprecated; use 'user.education.lab_notebook'
```

Para que los avisos no pasen desapercibidos, imprímelos (como en el test de arriba). Para tratarlos como errores en tu
compilación, comprueba que no hay ninguno:

```java
List<Violation> warnings = classifications.stream().flatMap(c -> c.violations().stream()).toList();
assertTrue(warnings.isEmpty(), () -> "Sustituye las claves obsoletas: " + warnings);
```

## Obtener las clases desde JPA

Enumerar las clases a mano funciona en un modelo pequeño, pero es fácil olvidar alguna. Con JPA, tómalas del
metamodelo:

```java
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.Classifier;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import java.util.List;

static void verifyAllEntities(EntityManagerFactory entityManagerFactory) {
    List<Class<?>> entities = entityManagerFactory.getMetamodel().getEntities().stream()
            .<Class<?>>map(EntityType::getJavaType)
            .toList();
    new Classifier(Taxonomy.load()).verify(entities);
}
```

Llámalo desde un test de integración que ya tenga un `EntityManagerFactory` (por ejemplo, un test de Spring Boot con
un `EntityManagerFactory` inyectado). No hace falta enumerar las superclases mapeadas como `AuditedEntity`: sus campos
se clasifican como parte de cada entidad. Tampoco los **embebibles**, siempre que estén marcados con `@Composite`
(módulo 3): el clasificador entra en ellos desde cada entidad que contiene uno e informa de sus problemas en rutas
como `home.street`. Un embebible que no está marcado `@Composite` aparece como un campo sin clasificar de la entidad
(`UNCLASSIFIED_FIELD`).

Sin JPA vale cualquier fuente de clases: una lista fija, un escaneo del class path, o `ModelClasses`, más abajo.

## No olvidar una clase

Una lista escrita a mano tiene un punto débil: la clase que un desarrollador añada el mes que viene no está en ella, así
que es la que nadie verifica. `ModelClasses` lista las clases de un paquete, leídas desde donde se cargó una clase
ancla, para que el test verifique todo lo que contiene el modelo:

```java
@Test
void everyClassOfTheModelIsClassified() {
    new Classifier(Taxonomy.load()).verify(ModelClasses.inPackageTree(Student.class));
}
```

`inPackageTree` toma el paquete de `Student` y los paquetes que cuelgan de él; `inPackage` toma solo el paquete.
Devuelve clases y records, incluidos los anidados y los abstractos, ordenados por nombre, y deja fuera las interfaces,
los enumerados, las anotaciones, las clases anónimas y las clases locales. Una clase nueva con un campo sin clasificar
hace ahora fallar la compilación con `UNCLASSIFIED_FIELD`, sin que nadie la haya listado. Con Maven o Gradle, la propia
clase de test no está en la lista, aunque esté en el mismo paquete: está en otra ubicación (las clases de test).

Si el paquete contiene además clases que no son datos, como un servicio con campos, filtra la lista antes de
verificarla:

```java
List<Class<?>> model = ModelClasses.inPackageTree(Student.class).stream()
        .filter(type -> !type.getSimpleName().endsWith("Service"))
        .toList();
new Classifier(Taxonomy.load()).verify(model);
```

`ModelClasses` también es fail-closed: una ubicación que no puede listar o una clase que no puede cargar es una
`IllegalArgumentException`, no una clase que se omite en silencio. Entonces pasa las clases de forma explícita.

## Errores frecuentes

- **Usar `Taxonomy.standard()` en el test cuando la aplicación usa claves de una extensión** → `UNKNOWN_CATEGORY` en
  todas las claves de la extensión.
- **Capturar la excepción y limitarse a registrarla en el log.** Lo que se busca es que falle la compilación.
- **Verificar solo algunas entidades.** Tómalas del metamodelo, o con `ModelClasses`, para que las nuevas queden
  cubiertas automáticamente.
- **Corregir una infracción añadiendo `@Exempt`** a un campo que contiene datos. Revisa las exenciones como código.
- **Olvidar `@Composite` en un embebible.** El campo embebido no tiene categoría y se informa como
  `UNCLASSIFIED_FIELD`; la solución es el marcador en la clase embebible, no un `@Exempt` en el campo.
- **Ignorar los avisos para siempre.** Una clave obsoleta sigue funcionando, pero la sustituta es la que usarán el
  catálogo y los informes.

## Ejercicios

**Ejercicio 4.1 — El test.** Escribe `DataClassificationTest` para el modelo construido en el módulo 3 (`MenuGroup`,
`Student`, `Enrollment`, `Grade`, `Staff`, `Document`, `Attachment`, `CourseStatistics`). Ejecútalo y haz que pase.

**Ejercicio 4.2 — Leer y corregir.** La rama de un compañero produce esta salida. Para cada línea, indica qué está mal
y escribe la corrección.

```
4 classification error(s):
 - ERROR UNCLASSIFIED_FIELD edu.example.university.Staff.officePhone: no @Category on the field and no @DefaultCategory on Staff
 - ERROR LOWERED_LEVEL edu.example.university.Grade.mark: integrity MEDIUM is lower than the HIGH of 'user.education.grade'
 - ERROR PERSONAL_DEFAULT edu.example.university.Building: @DefaultCategory of Building names 'user.workplace', which is personal data: declare personal fields one by one with @Category
 - ERROR CONFLICTING_ANNOTATIONS edu.example.university.Document.attachments: @Exempt cannot be combined with @Category, @CategoryByRow or @DerivedFrom
```

**Ejercicio 4.3 — Modo estricto.** Amplía tu test para que falle también si hay algún aviso, con un mensaje que los
enumere.

**Ejercicio 4.4 — Resumen.** Cambia el test para que, cuando falle la verificación, imprima cuántos errores hay de cada
`Violation.Code` antes de volver a lanzar la excepción.

## Cuestionario de autoevaluación

1. ¿Se detiene `verify` en la primera clase con errores?
2. ¿Hace una infracción `DEPRECATED_CATEGORY` que `verify` lance la excepción?
3. En `ERROR UNCLASSIFIED_FIELD edu.example.university.Student.createdAt: ...`, ¿qué clase declara `createdAt`?
4. ¿Por qué debe usar el test `Taxonomy.load()`?
5. ¿Por qué no hace falta enumerar los embebibles de JPA en las clases verificadas, y qué deben llevar en su lugar?

## Para saber más

- [Referencia — Clasificador](../reference/classify/classifier.md)
- [Referencia — Infracciones](../reference/classify/violations.md): todos los códigos con sus mensajes
- [Referencia — Resultados](../reference/classify/results.md)

---

← Anterior: [Módulo 3 — Anotar los modelos](03-annotating-models.md) · Siguiente:
[Módulo 5 — Usar la clasificación](05-using-the-classification.md) →

<details markdown="block">
<summary><strong>Soluciones</strong></summary>

**Ejercicio 4.1**

El test de este módulo con `CourseStatistics.class` añadido a la lista. Pasa con las anotaciones de las soluciones del
módulo 3 y no imprime nada (no hay avisos).

**Ejercicio 4.2**

- `Staff.officePhone`: sin anotar. Añade `@Category(USER_CONTACT_PHONE_NUMBER)`.
- `Grade.mark`: declara `integrity = Integrity.MEDIUM`, inferior al `HIGH` de la categoría. Quita el elemento.
- `Building`: `user.workplace` es personal. Un edificio es un dato de la organización: usa
  `@DefaultCategory(value = ORG_LOCATION, reason = "edificios y aulas de la universidad")` y anota con su propia
  `@Category` cualquier campo personal (por ejemplo, el nombre del responsable del edificio).
- `Document.attachments`: lleva `@Exempt` junto con otra anotación de clasificación. Un campo de relación no contiene
  datos por sí mismo: deja solo `@Exempt("relación: cada Attachment se clasifica por separado")`.

**Ejercicio 4.3**

```java
List<Violation> warnings = classifications.stream().flatMap(c -> c.violations().stream()).toList();
assertTrue(warnings.isEmpty(), () -> "Sustituye las claves obsoletas: " + warnings);
```

(Cuando `verify` devuelve el resultado, todas las infracciones que quedan son avisos.)

**Ejercicio 4.4**

```java
try {
    classifications = classifier.verify(entities);
} catch (ClassificationException e) {
    Map<Violation.Code, Long> byCode = e.errors().stream()
            .collect(Collectors.groupingBy(Violation::code, TreeMap::new, Collectors.counting()));
    System.out.println(byCode);
    throw e;
}
```

**Cuestionario**

1. No: clasifica todas las clases y notifica todos los errores juntos.
2. No, es un aviso; el campo se clasifica con la sustituta.
3. La salida no lo dice: nombra la clase que se clasifica (`Student`). `createdAt` puede estar declarado en una
   superclase (en nuestro modelo, `AuditedEntity`).
4. Para que se apliquen las extensiones registradas en la aplicación y se conozcan sus claves.
5. El clasificador entra en una clase `@Composite` desde cada clase que la contiene, así que los campos del embebible
   se comprueban como parte de cada entidad. Deben llevar `@Composite`; sin él, el campo embebido es
   `UNCLASSIFIED_FIELD`.

</details>
