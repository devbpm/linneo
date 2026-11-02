---
title: "10. Mantener al día los registros guardados"
parent: "Curso"
nav_order: 10
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/DataInventory.java
  - src/main/java/io/github/devbpm/linneo/classify/InventoryField.java
  - src/main/java/io/github/devbpm/linneo/classify/InventoryDiff.java
  - src/main/java/io/github/devbpm/linneo/classify/FieldChange.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - src/main/java/io/github/devbpm/linneo/Taxonomy.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyDiff.java
  - src/main/java/io/github/devbpm/linneo/TermChange.java
  - src/main/java/io/github/devbpm/linneo/Protection.java
  - src/main/java/io/github/devbpm/linneo/processing/Governance.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingChecker.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingRecord.java
  - src/main/java/io/github/devbpm/linneo/processing/RecordDiff.java
  - src/main/java/io/github/devbpm/linneo/processing/ActivityChange.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OpenMetadataCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DataHubCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasCatalog.java
-->

# Módulo 10 — Mantener al día los registros guardados

## Objetivos de aprendizaje

Al terminar este módulo sabrás:

- explicar por qué una aplicación guarda su propia copia de la clasificación y cómo esa copia se aleja del código;
- construir el inventario de datos de un modelo con `Classifier.inventory` y elegir claves de objeto que sobrevivan a
  una refactorización;
- comparar lo guardado con lo que declara el código (`InventoryDiff`), aplicar el resultado y llevar a revisión cada
  cambio que rebaje la protección;
- guardar la taxonomía que aplicó la aplicación y reaccionar a una nueva versión de Linneo con `TaxonomyDiff`;
- mantener las etiquetas de un catálogo de datos al día con la copia guardada, columna a columna;
- comprobar actividades de tratamiento a partir de registros guardados, con el gobierno que mantiene el delegado de
  protección de datos;
- saber qué ha cambiado en el registro de actividades de tratamiento desde que el delegado lo aprobó (`RecordDiff`).

Los módulos 3, 4 y 9 son necesarios.

## La copia que se desvía

Los módulos 3 a 9 usaron la clasificación donde vive: en el código, leída por el `Classifier` en un test o en
ejecución. Las aplicaciones reales también guardan una **copia**. Una pantalla lista los campos de cada objeto de
negocio con su categoría; un motor de permisos oculta las columnas de categorías especiales; un catálogo de datos
muestra las etiquetas de cada columna; el delegado edita el registro de actividades de tratamiento en un back office.
Todos leen tablas, no anotaciones.

Una copia se desvía. Un desarrollador añade `Student.phone` con `@Category(USER_CONTACT_PHONE_NUMBER)`; la build pasa,
porque la clasificación es correcta, pero a la tabla de campos le falta la fila y la pantalla que debería enmascarar el
teléfono lo muestra en claro. Los ficheros de semillas escritos a mano se desvían igual.

El remedio habitual es un **reconciliador**: en cada arranque, la aplicación compara la copia con el código y corrige
la copia. Linneo no trae uno, porque un reconciliador es sobre todo cosa de la aplicación: sus tablas, sus
transacciones, sus cerrojos entre nodos, su auditoría, las columnas que editan sus administradores. Lo que Linneo te
da es la parte que es igual en todas las aplicaciones:

| Paso | Quién | Con |
|---|---|---|
| Saber qué declara el código | Linneo | `Classifier.inventory(...)` |
| Leer lo guardado | la aplicación | sus propias consultas, y después `DataInventory.builder()` |
| Comparar y decir cómo se movió la protección | Linneo | `InventoryDiff.between(...)`, `FieldChange.direction()` |
| Escribir los cambios, en una transacción | la aplicación | su propia persistencia |
| Auditar y pedir revisión de los cambios que rebajan | la aplicación | `InventoryDiff.weakening()` |
| Detectar una taxonomía nueva y a qué alcanza | Linneo | `TaxonomyDiff`, `DataInventory.affectedBy(...)` |

Tres reglas hacen seguro un reconciliador, y la API las sigue:

1. **El código manda en la clasificación.** Claves, niveles que eleva un campo, identificabilidad: diga lo que diga la
   copia, el siguiente arranque la reescribe como dice el código.
2. **Nunca borrar, retirar.** Un campo que el código ya no declara conserva su fila, marcada como retirada, porque los
   permisos, la auditoría y los informes apuntan a ella. Si el código lo vuelve a declarar, la fila vuelve como estaba.
3. **Guardar claves, no niveles.** Una fila guarda `user.contact.email`, no «confidencialidad LOW». La protección se
   resuelve con la taxonomía cuando hace falta, así que una taxonomía más estricta protege al instante las filas
   antiguas.

## El inventario del código

```java
Classifier classifier = new Classifier(Taxonomy.load());
DataInventory declared = classifier.inventory(entities,
        type -> type.getAnnotation(jakarta.persistence.Table.class).name());
```

`inventory` clasifica las clases, falla con una `ClassificationException` si alguna tiene un error y devuelve un
**objeto** por clase con sus campos. El segundo argumento da a cada objeto su clave, y pesa más de lo que parece: las
filas guardadas se emparejan por `(objeto, ruta)`. Una clave tomada del nombre de la clase convierte una clase
renombrada o movida en un objeto eliminado y otro nuevo, con todos sus campos retirados e insertados otra vez. Tómala
de algo que ya identifique el objeto en tu aplicación y no cambie con una refactorización: el nombre de la tabla, una
anotación de objeto de negocio, un registro.

Cada `InventoryField` guarda lo que el código **declara**, no lo que una taxonomía hace con ello:

| Componente | `Student.email` con `@Category(value = USER_CONTACT_EMAIL, confidentiality = HIGH)` |
|---|---|
| `object`, `path` | `"students"`, `"email"` |
| `key` | `"user.contact.email"`, aunque una taxonomía posterior la deje obsoleta |
| `basis` | `DECLARED` (o `CLASS_DEFAULT`, `BY_ROW`, `DERIVED`) |
| `confidentiality`, `integrity` | `HIGH`, `null`: solo los niveles que eleva el propio campo |
| `identifiability` | `null`: el campo no declara ninguna; una categoría personal se lee como `IDENTIFIED`, que nunca se guarda |
| `reason` | el motivo de un valor por defecto de clase o lo que decide una categoría por fila; aquí `null` |

Un campo compuesto aparece una vez por parte (`home.street`), un campo derivado con la clave de su origen, y un campo
exento como `InventoryField.exempt(object, path, reason)`, sin clave, para que eximir un campo que antes estaba
clasificado sea un cambio visible. `field.protection(taxonomy)` resuelve la protección cuando la necesitas, igual que
lo haría `FieldClassification.protection()`.

## Lo guardado y el plan

Lee tu tabla y construye con ella el inventario guardado. Solo entran las columnas que manda el código: etiquetas,
órdenes y el interruptor de «activo» que editan tus administradores se quedan fuera, así que la comparación nunca los
toca.

```java
import io.github.devbpm.linneo.classify.FieldClassification.Basis;

DataInventory.Builder stored = DataInventory.builder();
for (FieldRow row : fieldTable.activeRows()) {               // la consulta de la aplicación, una por tabla
    stored.field(new InventoryField(row.object(), row.path(), row.categoryKey(), Basis.valueOf(row.basis()),
            row.raisedConfidentiality(), row.raisedIntegrity(), row.identifiability(), row.reason()));
}
InventoryDiff diff = InventoryDiff.between(stored.build(), declared, taxonomy);
```

`InventoryDiff` es el plan. Cada `FieldChange` tiene un `kind`:

- `ADDED`: inserta la fila con los valores iniciales que luego serán de tus administradores, o revive la fila retirada
  del mismo objeto y ruta;
- `REMOVED`: retira la fila;
- `CHANGED`: reescribe las columnas que manda el código. Pasar a exento, o dejar de estarlo, también es un cambio.

y una `direction` calculada con la taxonomía que pasas, con el orden de `Protection` del módulo 5 más la
identificabilidad: `TIGHTENED`, `LOOSENED`, `MIXED` o `SAME`. `diff.weakening()` lista los cambios que rebajan alguna
protección: una categoría más débil, un campo que pasó a exento, un valor ahora declarado seudonimizado o anonimizado.
Aplícalos como los demás, porque el código manda, y pon cada uno delante del delegado. Los campos añadidos y
eliminados no tienen dirección; un campo personal nuevo son datos personales nuevos, que quizá deba recoger el registro
de actividades de tratamiento.

```java
for (FieldChange change : diff.changes()) {
    switch (change.kind()) {
        case ADDED -> fieldTable.insertOrRevive(change.after());
        case REMOVED -> fieldTable.retire(change.object(), change.path());
        case CHANGED -> fieldTable.rewriteCodeColumns(change.after());
    }
}
diff.weakening().forEach(change -> audit.reviewNeeded(change.toString()));
```

### Ahorrarse el trabajo

La mayoría de los arranques no cambian nada. Guarda `declared.fingerprint()` junto a tu tabla; si coincide con la
guardada, sáltate la comparación. Cuando difiere, `declared.fingerprint(object)` dice qué objetos volver a leer: guarda
también una huella por objeto y reconcilia solo los objetos cuya huella se movió. Las huellas son SHA-256 de un texto
determinista (`toTsv()`), así que no dependen de la JVM, del orden de las clases ni del orden de los campos.

La huella solo dice si cambió el **código**. Una edición a mano de una columna que manda el código no se ve hasta la
siguiente comparación: haz una comparación completa con cada versión nueva de la aplicación, o impide esas
actualizaciones en la base de datos (privilegios por columna que solo dejen al usuario de la aplicación actualizar las
columnas del administrador).

## Una taxonomía nueva

Una versión nueva de Linneo, o de tu propia `TaxonomyExtension`, puede dejar obsoleta una clave que guardaste o hacer
más estricta una categoría. Nada de eso cambia el inventario: el código sigue declarando las mismas claves. Guarda la
taxonomía que aplicaste y compara:

```java
String applied = settings.read("linneo.taxonomy");             // lo que guardó el último arranque
TaxonomyDiff changes = TaxonomyDiff.between(Taxonomy.fromTsv(applied), taxonomy);
changes.requireCompatible();                                  // un término eliminado o una protección más débil
for (var entry : declared.affectedBy(changes).entrySet()) {
    TermChange change = entry.getKey();                         // una entrada de auditoría por término, no por campo
    if (change.aspects().contains(TermChange.Aspect.REPLACEMENT)) {
        fieldTable.migrateKey(change.key(), change.after().info().replacedBy());
    }
    audit.taxonomyChange(change, entry.getValue());
}
settings.write("linneo.taxonomy", taxonomy.toTsv());
```

`Taxonomy.toTsv()` escribe cada término con su protección efectiva, unos 36 KB para la taxonomía estándar y 70 KB con
las cinco extensiones incluidas; `fromTsv` lo lee pasando por el `TaxonomyBuilder`, así que un texto dañado se
rechaza. El mismo texto permite a un auditor reproducir, años después, la protección que tenía un campo un día
concreto.
`TaxonomyDiff` también gradúa los cambios con las reglas de versionado de Linneo: `requiredBump()` es `MAJOR` para un
término eliminado, una protección más débil o mixta, o una clave obsoleta que ahora se resuelve a otra.

Como las filas guardan claves, una categoría más estricta no necesita ninguna actualización: la siguiente lectura
resuelve el nivel más estricto. Es la regla 3 en acción.

## Catálogos que siguen a la copia guardada

Un catálogo de datos es una copia más: los módulos 7 y 8 ponen etiquetas en cada columna. Las etiquetas de una columna
dependen de dos cosas, el campo y la taxonomía, así que una columna necesita etiquetas nuevas cuando cambió su campo
(`InventoryDiff`) o cuando la taxonomía cambió una categoría que usa (`affectedBy`). Los catálogos aceptan un
`InventoryField` guardado además de un `FieldClassification`, y lo resuelven con su propia taxonomía, de modo que las
etiquetas que tenía la columna son las del campo guardado con la taxonomía que aplicaste, y las que necesita son las
del campo declarado con la actual:

```java
OpenMetadataCatalog was = OpenMetadataCatalog.of(Taxonomy.fromTsv(applied));  // la del último arranque
OpenMetadataCatalog now = OpenMetadataCatalog.of(taxonomy);
for (FieldChange change : diff.changes()) {
    List<String> before = change.before() == null ? List.of() : was.tagsOf(change.before());
    List<String> after = change.after() == null ? List.of() : now.tagsOf(change.after());
    List<String> remove = new ArrayList<>(before);
    remove.removeAll(after);
    List<String> add = new ArrayList<>(after);
    add.removeAll(before);
    catalogClient.patchColumnTags(change.object(), change.path(), remove, add);   // una petición por columna
}
```

Haz lo mismo con cada campo que lista `declared.affectedBy(changes)`, con el mismo campo antes y después. Importan
tres detalles:

- **Quita y añade en una sola petición.** La mayoría de estas clasificaciones son mutuamente excluyentes en
  OpenMetadata (`Confidentiality.Low` y `Confidentiality.High` no pueden convivir), así que añadir primero se rechaza.
- **No toques las demás etiquetas.** Solo se quita la diferencia entre las dos listas de Linneo; las etiquetas que
  la gente añade a mano no aparecen en ninguna de las dos.
- **Un campo exento no tiene etiquetas.** Un campo que pasa a exento pierde todas las de Linneo; uno retirado también
  (`after` es `null`).

`DataHubCatalog.termsOf(InventoryField)` y `AtlasCatalog.classificationsOf(InventoryField)` funcionan igual. Las
definiciones del catálogo (las clasificaciones y etiquetas de la propia taxonomía) son peticiones idempotentes:
envíalas de nuevo tras un cambio de taxonomía, antes que las columnas.

## Actividades de tratamiento desde registros guardados

El módulo 9 declaró las actividades en código. En muchas organizaciones el **alcance** de una actividad lo conocen los
desarrolladores (finalidades, interesados, los objetos que lee) y su **gobierno** lo decide el delegado (la base
jurídica, la conservación, los destinatarios). Linneo modela esa separación:

```java
ProcessingActivity scope = ProcessingActivity.builder("enrollment", "Student enrollment")
        .purposes(DataUses.EDUCATION_ENROLLMENT)
        .dataSubjects(DataSubjects.STUDENT)
        .objects("students", "enrollments")                    // claves del inventario
        .build();

Governance governance = governanceTable.read("enrollment");   // lo que mantiene el delegado
ProcessingChecker checker = new ProcessingChecker(taxonomy, Jurisdiction.SPAIN, declared);
CheckedActivity checked = checker.check(scope.withGovernance(governance));
checked.violations().forEach(v -> {
    if (v.element().isGovernance()) {
        form.error(v.element(), v.message());                   // se muestra donde lo edita el delegado
    } else {
        throw new IllegalStateException(v.toString());          // un error del código
    }
});
```

`objects(...)` nombra objetos de un inventario de datos en lugar de clases, así que la misma comprobación funciona en
un back office que solo tiene el inventario guardado y nunca ve las clases de las entidades.

## El registro que firmó el delegado

El registro de actividades de tratamiento también es una copia, y la que lleva una firma. El delegado aprueba una
versión; después, un campo nuevo, una taxonomía más estricta o un cambio del gobierno pueden alterar lo que dice.
Guarda la `fingerprint()` del registro aprobado: si el registro actual tiene la misma, nada ha cambiado, sea cual sea
el orden de las actividades. Si no, reconstruye el registro aprobado con lo que guardaste al aprobarlo (el inventario,
la taxonomía y el gobierno) y compara:

```java
ProcessingRecord approved = new ProcessingChecker(Taxonomy.fromTsv(approvedTaxonomy), Jurisdiction.SPAIN,
        approvedInventory).verify(approvedActivities);
ProcessingRecord current = new ProcessingChecker(taxonomy, Jurisdiction.SPAIN, declared).verify(activities);
if (!current.fingerprint().equals(approval.fingerprint())) {
    for (ActivityChange change : RecordDiff.between(approved, current).changes()) {
        officer.review(change);                     // añadida, eliminada, o los aspectos que cambiaron
    }
}
```

Cada `ActivityChange` nombra la actividad y sus `aspects`: `ADDED` o `REMOVED`, el alcance (`PURPOSES`,
`DATA_SUBJECTS`), los datos (`PERSONAL_DATA`, `PROTECTION`), cada elemento del gobierno (`LEGAL_BASIS`, `RETENTION`,
`RECIPIENTS`…), el cribado de la evaluación de impacto `SCREENING` (que sigue los rasgos de los términos, así que una
taxonomía que añade un rasgo puede cambiarlo sin que cambie ninguna declaración) y los `OPEN_POINTS`, comparados por
código y elemento. Dos de sus respuestas orientan la revisión:

- `isGovernance()`: solo cambió el gobierno (con los puntos abiertos y el cribado que se siguen de él), así que el
  delegado está viendo su propio cambio;
- `direction()`: `TIGHTENED` indica que la actividad trata ahora datos que necesitan más protección, justo el caso en
  que la base jurídica, una condición del artículo 9 o una evaluación de impacto pueden dejar de bastar.

Un cambio del alcance en el código se detecta mejor antes, en la pull request: guarda `record.toMarkdown(title)` bajo
control de versiones, igual que la última sección guarda el inventario.

## Rendimiento

`tools/bench/InventoryBench.java` genera un modelo de 800 entidades, 40 de ellas con 400 campos, 39 000 campos
clasificados en total, y mide cada paso:

```text
java -cp target/classes tools/bench/InventoryBench.java
```

En un portátil de desarrollo la clasificación tarda unos 0,3 s la primera vez, el inventario 0,2 s, su huella 0,1 s y
la comparación con el 1 % de los campos cambiados menos de 0,1 s: la parte de Linneo de una reconciliación queda muy
por debajo de un segundo y crece linealmente con el número de campos. El tiempo que importa es el de la aplicación:

- lee cada tabla con **una consulta que devuelva las columnas que manda el código**, no una consulta por objeto, y no
  como entidades JPA gestionadas, cuya comprobación de cambios en cada volcado automático crece con las filas cargadas;
- escribe solo las filas que cambian, por lotes; con claves `IDENTITY` Hibernate no puede agrupar inserciones, así que
  el primer arranque de un modelo grande inserta decenas de miles de filas una a una;
- audita por causa: una entrada por término cambiado de `affectedBy`, no una por campo, y un resumen en el primer
  arranque;
- mantén la transacción, y el cerrojo que impide que dos nodos reconcilien a la vez, tan corta como eso permita.

## Una revisión que enseña cada cambio de clasificación

El inventario como texto es un buen fichero para tener bajo control de versiones: una línea por campo, ordenada, con
su clave, base, niveles elevados e identificabilidad. Un test puede compararlo con el código, de modo que cada cambio
de la clasificación aparezca en la pull request, donde lo ve quien revisa o el delegado:

```java
@Test
void classificationIsReviewed() throws IOException {
    String current = classifier.inventory(ENTITIES, Entities::key).toTsv();
    String reviewed = Files.readString(Path.of("src/test/resources/inventory.tsv"));
    assertEquals(reviewed, current, "the classification changed: review it and update inventory.tsv");
}
```

## Errores frecuentes

- **Claves de objeto a partir del nombre de la clase.** Una refactorización parece entonces la eliminación de un
  objeto entero.
- **Guardar los niveles de protección en cada fila.** Una taxonomía más estricta necesita entonces miles de
  actualizaciones, y una que falte deja un campo menos protegido de lo debido. Guarda la clave y los niveles que eleva
  el propio campo.
- **Borrar las filas de los campos eliminados.** Los permisos y la auditoría pierden su sentido; retíralas.
- **Meter las columnas del administrador en el inventario guardado.** La comparación reescribiría sus cambios en cada
  arranque.
- **Ignorar `weakening()` porque el código manda.** El código gana, pero alguien debe confirmar que la protección más
  débil era intencionada.
- **Saltarse la comparación por la huella para siempre.** Las ediciones a mano de columnas que manda el código solo se
  reparan con una comparación completa.
- **Añadir las etiquetas nuevas de una columna antes de quitar las viejas.** Las clasificaciones mutuamente
  excluyentes rechazan el segundo nivel; envía las dos cosas en una petición.
- **Comparar el registro actual con uno reconstruido con los datos actuales.** El diff sale vacío por construcción:
  reconstruye el registro aprobado con el inventario, la taxonomía y el gobierno guardados al aprobarlo.

## Ejercicios

**Ejercicio 10.1 — Predice.** El inventario guardado tiene `students.phone` con `user.contact.phone_number`. El código
cambia el campo a `@Exempt("legacy column, always empty")`. ¿Qué `FieldChange` informa la comparación, con qué
dirección, y está en `weakening()`?

**Ejercicio 10.2 — Claves.** Dos entidades, `legacy.Student` y `core.Student`, usan `Class::getSimpleName` como clave de
objeto. ¿Qué hace `inventory` y cómo elegirías las claves?

**Ejercicio 10.3 — Una taxonomía nueva.** La taxonomía que aplicó la aplicación tenía `user.contact.fax`; la nueva la
deja obsoleta con el sustituto `user.contact.phone_number`. ¿Qué aspectos de `TermChange` aparecen, qué devuelve
`requiredBump()` y qué filas guardadas deben cambiar?

**Ejercicio 10.4 — Fichero de referencia.** Añade el test de la última sección al modelo de la universidad y haz un
cambio de clasificación. Lee el mensaje del fallo y la diferencia de `inventory.tsv`.

**Ejercicio 10.5 — Las etiquetas de una columna.** `students.notes` se guardó con `user.content.private`; el código
declara ahora `user.health_and_medical`. Con los nombres por defecto de OpenMetadata, ¿qué etiquetas pierde la
columna y cuáles gana? ¿Podrías enviarlas en dos peticiones, primero las nuevas?

**Ejercicio 10.6 — El registro firmado.** El delegado aprobó el registro. Desde entonces, una desarrolladora añadió
`@Category(USER_HEALTH_AND_MEDICAL_DISABILITY)` a un campo de un objeto que lee la actividad `enrollment`, y el
delegado añadió la condición del artículo 9. ¿Qué aspectos tiene el `ActivityChange` de `enrollment`, cuál es su
dirección y es `isGovernance()` verdadero?

## Autoevaluación

1. ¿Qué pasos de una reconciliación hace Linneo y cuáles la aplicación?
2. ¿Por qué el inventario guarda los niveles que eleva un campo pero no los de su categoría?
3. ¿Qué significa `FieldChange.direction()` para un campo añadido?
4. Una categoría se vuelve más estricta en una taxonomía nueva. ¿Cuál de `InventoryDiff` y `affectedBy` lo informa?
5. ¿Qué rechaza `requireCompatible()`?
6. ¿Por qué un back office puede comprobar actividades de tratamiento sin las clases de las entidades?
7. ¿Qué dos catálogos necesita el cambio de una columna, y por qué dos?
8. ¿Cuándo tienen dos registros la misma huella?

## Para saber más

- [Referencia — Inventario de datos](../reference/classify/inventory.md)
- [Referencia — La taxonomía como datos](../reference/taxonomy.md)
- [Referencia — Diferencias entre taxonomías](../reference/taxonomy-diff.md)
- [Referencia — Niveles de protección](../reference/protection-levels.md)
- [Referencia — ProcessingChecker](../reference/processing/checker.md)
- [Referencia — El registro de actividades de tratamiento](../reference/processing/record.md)
- [Referencia — Catálogo de OpenMetadata](../reference/openmetadata/catalog.md)

---

← Anterior: [Módulo 9 — Actividades de tratamiento y el registro](09-processing-activities.md) · Volver a la
[presentación del curso](README.md)

<details markdown="block">
<summary><strong>Soluciones</strong></summary>

**Ejercicio 10.1.** Un cambio `CHANGED` para `students.phone`: antes, un campo clasificado con
`user.contact.phone_number`; después, un campo exento. Un campo exento tiene la protección más baja, así que la
dirección es `LOOSENED` y el cambio está en `weakening()`. Aplícalo (retira o reescribe la fila según decida tu
aplicación) y pide revisión: una columna «siempre vacía» que sigue existiendo puede llenarse mañana.

**Ejercicio 10.2.** Las dos clases reciben la clave `Student` e `inventory` falla con una `IllegalArgumentException`
que nombra las dos clases. Toma la clave del nombre de la tabla, de una anotación de objeto de negocio o de un
registro: algo que nombre el objeto, no la clase.

**Ejercicio 10.3.** El cambio de `user.contact.fax` tiene los aspectos `DEPRECATION` y `REPLACEMENT` (y `VERSIONS` solo
si cambió la versión de introducción). Dejar obsoleta una clave con un primer sustituto es compatible:
`requiredBump()` es `MINOR`. Las filas que guardan `user.contact.fax` son los campos que `affectedBy` lista para ese
cambio; mígralas a `user.contact.phone_number`, o consérvalas y deja que la taxonomía resuelva el sustituto al leerlas.

**Ejercicio 10.4.** El mensaje del fallo nombra el test, y la diferencia del fichero muestra la línea cambiada, con la
clave, base, niveles o identificabilidad de antes y de ahora, que es lo que necesita quien revisa.

**Ejercicio 10.5.** Pierde `DataCategory.user.content.private`, `PersonalData.Personal`, `Confidentiality.Medium` e
`Integrity.Medium`, y gana `DataCategory.user.health_and_medical`, `PersonalData.SpecialCategory`,
`Confidentiality.High` e `Integrity.High`; `PII.Sensitive` e `Identifiability.Identified` se quedan. No en dos
peticiones con las nuevas primero: las clasificaciones de datos personales, confidencialidad e integridad son
mutuamente excluyentes, así que el servidor rechaza un segundo nivel mientras el primero siga en la columna.

**Ejercicio 10.6.** `PERSONAL_DATA` (una categoría nueva), `PROTECTION` y `SPECIAL_CATEGORY_CONDITION`, más
`OPEN_POINTS` solo si aparece o desaparece un aviso. La recomendación de evaluación de impacto no es uno de ellos aquí:
el cribado cuenta los datos de categoría especial y los financieros como un solo criterio, los datos sensibles, así
que la discapacidad no añade ningún criterio a una actividad que ya trata la tasa, y `SCREENING` tampoco está entre los
aspectos. La dirección es `TIGHTENED`: la
actividad trata ahora datos de categoría especial. `isGovernance()` es falso, porque también cambiaron los datos; el
cambio del delegado forma parte del mismo cambio. Fíjate en el orden: hasta que se añadió la condición, el registro
actual ni siquiera podía construirse, porque datos de categoría especial sin condición del artículo 9 son un error.

**Autoevaluación.** 1: Linneo declara, compara y dice la dirección; la aplicación lee, escribe, bloquea y audita.
2: los niveles de la categoría son de la taxonomía, que puede cambiar sin que cambie el código; guardar solo los que
eleva el campo separa las dos causas. 3: nada, es `SAME`; mira `after().isPersonal(taxonomy)` para detectar datos
personales nuevos. 4: `affectedBy`; `InventoryDiff` compara con una sola taxonomía y solo ve cambios del código. 5: un
término eliminado, una protección más débil o mixta, y una clave obsoleta que se resuelve a un sustituto distinto del
de antes. 6: las actividades pueden nombrar objetos de un `DataInventory` guardado, y el comprobador los resuelve con
la taxonomía. 7: uno con la taxonomía aplicada antes, para las etiquetas que tiene la columna, y otro con la actual,
para las que necesita; las etiquetas dependen del campo y de la taxonomía. 8: exactamente cuando `RecordDiff` no
encuentra ningún cambio entre ellos, sea cual sea el orden de sus actividades.

</details>
