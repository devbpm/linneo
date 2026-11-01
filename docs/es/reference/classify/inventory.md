---
title: "Inventario de datos"
parent: "Referencia"
nav_order: 11.5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/DataInventory.java
  - src/main/java/io/github/devbpm/linneo/classify/InventoryField.java
  - src/main/java/io/github/devbpm/linneo/classify/InventoryDiff.java
  - src/main/java/io/github/devbpm/linneo/classify/FieldChange.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - tools/bench/InventoryBench.java
-->

# Inventario de datos

`io.github.devbpm.linneo.classify.DataInventory` — `public final class`, `@since 0.1.0`

`io.github.devbpm.linneo.classify.InventoryField` — `public record`, `@since 0.1.0`

`io.github.devbpm.linneo.classify.InventoryDiff` — `public final class`, `@since 0.1.0`

`io.github.devbpm.linneo.classify.FieldChange` — `public record`, `@since 0.1.0`

La clasificación de un modelo como **datos**: para cada objeto, identificado por una clave que elige la aplicación, los
campos que contiene con lo que el código declara para ellos. No contiene clases ni taxonomía, así que una aplicación
puede guardarlo, volver a leerlo y compararlo. Las aplicaciones lo usan para mantener sus propios registros de la
clasificación, como una tabla de campos o un catálogo de datos, al día con el código:

- [`Classifier.inventory`](classifier.md#inventario) construye el inventario del código;
- [`DataInventory.builder()`](#builder) (o [`fromTsv`](#formato-del-texto)) construye el inventario de lo que la
  aplicación guardó;
- [`InventoryDiff.between`](#inventorydiff) los compara y dice qué insertar, retirar o actualizar, y qué cambios
  debilitan la protección;
- las [huellas](#huellas) dicen si algo cambió sin comparar campo a campo.

Las instancias de los cuatro tipos son inmutables.

## Qué registra

El inventario registra lo que el código **declara**, nunca lo que la taxonomía da a un campo. Un campo clasificado
conserva:

- la **clave** de su categoría de datos **tal como está escrita**, incluso si es obsoleta (no se sustituye por su
  sucesora);
- la **base**: de dónde viene la categoría ([`Basis`](results.md#basis));
- la `confidentiality` y la `integrity` que el campo eleva explícitamente con `@Category`, o `null` si toma las de la
  categoría;
- la **identificabilidad**, en una categoría personal;
- el **motivo**: el de un valor por defecto de clase, o el `declaredBy` de una categoría por fila.

Un campo excluido con [`@Exempt`](../annotations/exempt.md) también se conserva, solo con su motivo, de modo que excluir
un campo que antes estaba clasificado se vea como un cambio.

Como los niveles de la taxonomía no se copian en el inventario, **un cambio de la taxonomía nunca cambia un
inventario**. Endurece una categoría y los inventarios del código y de la copia guardada siguen siendo iguales; lo que
cambia es la protección que obtienes al resolver sus campos contra la taxonomía del día, con
[`InventoryField.protection(taxonomy)`](#inventoryfield). [`affectedBy`](#cambios-de-la-taxonomía) lista los campos a
los que llega un cambio de la taxonomía.

### Claves de objeto

Un objeto es cualquier grupo de campos del que la aplicación lleva un registro: un objeto de negocio, una tabla, una
entidad. Su clave la **elige la aplicación** y debe ser **estable**: los registros guardados se emparejan con ella. No
uses el nombre de la clase: si se renombra o se mueve la clase, cada campo del nombre antiguo es un campo eliminado y
cada campo del nuevo, un campo añadido. Toma la clave de lo que ya identifica el objeto en la aplicación, como su objeto
de negocio o el nombre de su tabla.

## DataInventory

| Miembro | Devuelve | Descripción |
|---|---|---|
| `static Builder builder()` | `Builder` | Empieza un inventario, normalmente a partir de lo que una aplicación guardó. |
| `Set<String> objects()` | conjunto | Las claves de los objetos, ordenadas. |
| `List<InventoryField> fields()` | lista inmutable | Todos los campos, incluidos los exentos, por objeto y después por ruta. |
| `List<InventoryField> fields(String object)` | lista inmutable | Los campos de un objeto por ruta; vacía si el objeto no existe. |
| `Optional<InventoryField> field(String object, String path)` | `Optional` | Un campo, o vacío si el inventario no lo contiene. |
| `List<InventoryField> fieldsWith(String key)` | lista inmutable | Los campos clasificados que declaran exactamente esa clave de categoría de datos, por objeto y después por ruta. |
| `Map<TermChange, List<InventoryField>> affectedBy(TaxonomyDiff diff)` | mapa inmutable | Los campos a los que llega cada cambio de una taxonomía; véase [Cambios de la taxonomía](#cambios-de-la-taxonomía). |
| `String fingerprint()` | `String` | La huella de todo el inventario; véase [Huellas](#huellas). |
| `String fingerprint(String object)` | `String` | La huella de un objeto. `IllegalArgumentException` si el inventario no lo contiene. |
| `String toTsv()` | `String` | El inventario como texto separado por tabuladores, determinista; véase [Formato del texto](#formato-del-texto). |
| `static DataInventory fromTsv(String text)` | `DataInventory` | Lee un texto escrito por `toTsv()`. `IllegalArgumentException` si el texto no es un inventario de una versión de formato conocida o una fila está mal formada. |
| `boolean equals(Object)`, `int hashCode()` | | Dos inventarios son iguales cuando tienen los mismos objetos y los mismos campos. |

`toString()` informa del tamaño, por ejemplo `DataInventory[2 objects, 7 fields]`.

### Builder

`DataInventory.Builder` — `public static final class`, `@since 0.1.0`. Es la forma de convertir en inventario las filas
que una aplicación guardó. No tiene constructor público; usa `DataInventory.builder()`.

| Método | Efecto |
|---|---|
| `Builder object(String key)` | Añade un objeto, aunque no tenga campos. `NullPointerException` si `key` es `null`; `IllegalArgumentException` si está en blanco. |
| `Builder field(InventoryField field)` | Añade un campo y, si es nuevo, su objeto. `IllegalArgumentException` si el objeto ya tiene un campo con esa ruta. |
| `Builder field(String object, String path, String key, Basis basis)` | Añade un campo clasificado que toma los niveles de su categoría: sin niveles elevados, sin identificabilidad y sin motivo. |
| `Builder exempt(String object, String path, String reason)` | Añade un campo excluido de la clasificación. |
| `DataInventory build()` | Crea el inventario. El constructor se puede conservar y volver a usar. |

```java
DataInventory stored = DataInventory.builder()
        .field("student", "email", Categories.USER_CONTACT_EMAIL, Basis.DECLARED)
        .field(new InventoryField("student", "version", Categories.SYSTEM_OPERATIONS, Basis.CLASS_DEFAULT,
                null, null, null, "technical columns"))
        .exempt("student", "card", "cache of the rendered card")
        .build();
```

(`Basis` es `FieldClassification.Basis`.) Un campo personal construido sin identificabilidad se lee como `IDENTIFIED`,
igual que hace el clasificador.

## InventoryField

```java
public record InventoryField(String object, String path, String key, Basis basis, Confidentiality confidentiality,
                             Integrity integrity, Identifiability identifiability, String reason)
```

Un campo de un inventario: lo que el código declara para él, independiente de cualquier taxonomía, de modo que se pueda
guardar, comparar y resolver más tarde contra la taxonomía del día.

| Componente | Admite `null` | Descripción |
|---|---|---|
| `object` | no | La clave del objeto al que pertenece el campo, elegida por la aplicación. |
| `path` | no | La ruta del campo dentro del objeto: su nombre, o `campo.parte` para un campo de un [`@Composite`](../annotations/composite.md). |
| `key` | sí | La clave de la categoría de datos tal como se declaró, posiblemente obsoleta; `null` en un campo exento. |
| `basis` | sí | De dónde viene la categoría: `DECLARED`, `CLASS_DEFAULT`, `BY_ROW` o `DERIVED`; `null` en un campo exento. |
| `confidentiality` | sí | La confidencialidad que el campo declara por encima de la de su categoría, o `null` si toma la de la categoría. |
| `integrity` | sí | La integridad que el campo declara por encima de la de su categoría, o `null` si toma la de la categoría. |
| `identifiability` | sí | Cómo apunta el valor a la persona; `null` si la categoría no es personal, y se lee como `IDENTIFIED` cuando falta en una categoría personal. |
| `reason` | sí | El motivo de una exención o de un valor por defecto de clase, o lo que decide una categoría por fila (`declaredBy`); `null` en los demás casos. |

El constructor canónico lanza `NullPointerException` si `object` o `path` son `null`, e `IllegalArgumentException` si
alguno está en blanco, si solo se da uno de `key` y `basis`, o si un campo exento (sin clave) tiene niveles, una
identificabilidad o ningún motivo.

| Miembro | Descripción |
|---|---|
| `static InventoryField exempt(String object, String path, String reason)` | Un campo excluido de la clasificación. |
| `boolean isExempt()` | `true` si el campo no tiene clave. |
| `DataCategory category(Taxonomy taxonomy)` | La categoría de datos vigente del campo: su clave, o la sustituta de una clave obsoleta. `TaxonomyException` si el campo está exento o la taxonomía no contiene su clave. |
| `boolean isPersonal(Taxonomy taxonomy)` | Si los valores almacenados son datos personales, como dice [`FieldClassification.isPersonal()`](results.md#métodos): una categoría personal que no se declara `ANONYMIZED` ni `AGGREGATED`. `false` en un campo exento. `TaxonomyException` si la taxonomía no contiene la clave. |
| `Protection protection(Taxonomy taxonomy)` | La [protección](../protection-levels.md#protection) que el campo requiere en la taxonomía, tal como la calcula `FieldClassification.protection()`; `Protection.LOWEST` en un campo exento. `TaxonomyException` si la taxonomía no contiene la clave. |

`toString()` es `<object>.<path> = <key> (<BASIS>)`, o `<object>.<path> exempt: <reason>`.

Resuelto contra la taxonomía, un campo del inventario da lo mismo que el clasificador da al mismo campo: en cada campo de
una clase, `protection(taxonomy)`, `isPersonal(taxonomy)` y `category(taxonomy)` son iguales a los de su
`FieldClassification`.

## Construirlo desde el código

[`Classifier.inventory(types, objectKey)`](classifier.md#inventario) verifica las clases, así que falla con una
`ClassificationException` si alguna tiene un error, y registra cada campo de cada clase bajo la clave que `objectKey`
da para ella. Una clase repetida cuenta una sola vez; una clave `null` o en blanco, o la misma clave para dos clases,
es una `IllegalArgumentException`.

```java
Map<Class<?>, String> objectKeys = Map.of(Student.class, "student", Setting.class, "setting");
DataInventory declared = new Classifier(taxonomy).inventory(List.of(Student.class, Setting.class), objectKeys::get);
```

Qué registra cada campo, según cómo lo resolvió el clasificador:

| Base | `key` | Niveles | `reason` |
|---|---|---|---|
| `DECLARED` | el `value` de `@Category` | los que `@Category` fija explícitamente | `null` |
| `CLASS_DEFAULT` | el `value` de `@DefaultCategory` | `null` | el `reason` del valor por defecto |
| `BY_ROW` | el `ceiling` de `@CategoryByRow` | `null` | `declaredBy` |
| `DERIVED` | la clave de su propio `@Category`, o la de su fuente | los de su propio `@Category`, o los que declara su fuente | `null` |

Un campo exento se registra con su motivo de `@Exempt`. Los campos de un `@Composite` tienen la ruta `campo.parte`.

## Formato del texto

`toTsv()` escribe el inventario como texto y `fromTsv` lo vuelve a leer, de modo que una aplicación puede conservarlo en
un archivo bajo control de versiones, donde una revisión muestra cada cambio de la clasificación, o intercambiarlo con
otro sistema. `DataInventory.fromTsv(inventory.toTsv())` es igual a `inventory`.

La primera línea es `#linneo-inventory`, un tabulador y la versión del formato, `1`; `fromTsv` rechaza cualquier otra
primera línea. Las líneas en blanco y las que empiezan por `#` a continuación son comentarios; `toTsv` escribe tres que
nombran las filas. Cualquier otra línea es una fila de celdas separadas por tabuladores, y la primera celda dice de qué
fila se trata:

| Fila | Celdas |
|---|---|
| `object` | `object`, clave: 2 celdas. Declara un objeto, así que se conserva un objeto sin campos |
| `field` | `field`, objeto, ruta, clave, base, confidencialidad, integridad, identificabilidad, motivo: 9 celdas. La confidencialidad, la integridad, la identificabilidad y el motivo pueden estar vacíos |
| `exempt` | `exempt`, objeto, ruta, motivo: 4 celdas |

Cada objeto es una fila `object` seguida de sus filas `field` y `exempt` por ruta, y los objetos van por clave, de modo
que el mismo inventario da siempre el mismo texto. Una celda vacía es un valor ausente. Una barra invertida, un
tabulador, un salto de línea y un retorno de carro dentro de una celda se escriben `\\`, `\t`, `\n` y `\r`, como en el
[texto de una taxonomía](../taxonomy.md#formato-del-texto), de modo que una fila es siempre una línea. La base, la
confidencialidad, la integridad y la identificabilidad son los nombres de sus constantes. `fromTsv` lanza
`IllegalArgumentException` ante otra primera línea, una fila con otro número de celdas, un tipo de fila o una constante
desconocidos, una celda vacía que debe tener valor, o dos filas del mismo campo.

Para `Student`, con un `email` personal que eleva su confidencialidad, un valor por defecto de clase para el resto y un
`card` exento, con `→` en lugar de un tabulador:

```text
#linneo-inventory→1
# object key
# field object path key basis confidentiality integrity identifiability reason
# exempt object path reason
object→student
exempt→student→card→cache of the rendered card
field→student→email→user.contact.email→DECLARED→HIGH→→IDENTIFIED→
field→student→version→system.operations→CLASS_DEFAULT→→→→technical columns
```

## Huellas

| Método | Descripción |
|---|---|
| `fingerprint()` | El SHA-256 de `toTsv()` (UTF-8), como 64 dígitos hexadecimales en minúscula. Los inventarios iguales tienen la misma huella; se calcula una vez y se conserva. |
| `fingerprint(String object)` | El SHA-256 del texto de ese objeto solo: la primera línea y las filas del objeto, como 64 dígitos hexadecimales en minúscula. Cambia únicamente cuando cambia ese objeto. |

Una huella dice que algo cambió, no qué. Conserva la huella de lo que guardaste: al arrancar, si la huella del código es
la misma, no hay nada que reconciliar y se puede omitir la comparación; si difiere, compara solo los objetos cuya
propia huella se movió.

## InventoryDiff

Las diferencias entre dos inventarios, campo a campo, como lo que una aplicación guardó y lo que su código declara
ahora. Es el plan de una reconciliación: Linneo compara, la aplicación aplica.

| Miembro | Devuelve | Descripción |
|---|---|---|
| `static InventoryDiff between(DataInventory before, DataInventory after, Taxonomy taxonomy)` | `InventoryDiff` | Compara dos inventarios: `before` es el anterior, como lo que la aplicación guardó, y `after` el posterior, como lo que su código declara ahora. La `taxonomy` resuelve las claves para saber cómo cambió la protección. `NullPointerException` si algún argumento es `null`. |
| `List<FieldChange> changes()` | lista inmutable | Los cambios de campo, por objeto y después por ruta. Un campo que no cambió no aparece. |
| `List<FieldChange> weakening()` | lista inmutable | Los cambios que [debilitan](#fieldchange) la protección (`weakens`) y merecen una revisión. |
| `List<String> addedObjects()` | lista inmutable | Los objetos que solo contiene `after`, ordenados. |
| `List<String> removedObjects()` | lista inmutable | Los objetos que solo contiene `before`, ordenados. |
| `Set<String> changedObjects()` | conjunto | Los objetos con algún cambio, incluidos los añadidos y los eliminados, ordenados: los que hay que reconciliar. |
| `boolean isEmpty()` | `boolean` | `true` si ningún objeto ni campo difiere: los inventarios son iguales. |

`toString()` informa del tamaño, por ejemplo `InventoryDiff[3 field change(s), 2 weakening]`.

Los dos inventarios se comparan **con la misma taxonomía**, así que los cambios vienen del código; los cambios que
vienen de la taxonomía se encuentran con [`affectedBy`](#cambios-de-la-taxonomía). Los campos de un objeto añadido son
cambios `ADDED` y los de un objeto eliminado, cambios `REMOVED`, además de que el objeto figura en `addedObjects()` o
en `removedObjects()`.

### FieldChange

```java
public record FieldChange(String object, String path, Kind kind, InventoryField before, InventoryField after,
                          Direction direction)
```

Cómo difiere un campo entre los dos inventarios.

| Componente | Admite `null` | Descripción |
|---|---|---|
| `object` | no | La clave del objeto. |
| `path` | no | La ruta del campo. |
| `kind` | no | Si el campo se añadió, se eliminó o cambió (véase más abajo). |
| `before` | sí | El campo en el inventario anterior; `null` si se añadió. |
| `after` | sí | El campo en el inventario posterior; `null` si se eliminó. |
| `direction` | no | Cómo se movió la protección del campo, con su identificabilidad: una [`Protection.Direction`](../protection-levels.md#direction). |

El constructor canónico lanza `NullPointerException` si `object`, `path`, `kind` o `direction` son `null`, e
`IllegalArgumentException` si `before` y `after` no corresponden al tipo (`before` es `null` exactamente en `ADDED`,
`after` exactamente en `REMOVED`).

| Método | Descripción |
|---|---|
| `boolean weakens()` | Si el cambio baja alguna protección y merece una revisión: `direction().weakens()`, es decir, `LOOSENED` o `MIXED`. Incluye un campo clasificado que pasó a estar exento y un valor declarado anonimizado. |

`toString()` es `<KIND> <object>.<path>`, después la dirección cuando no es `SAME` y después `: <after>` o, en un campo
eliminado, `: was <before>`; por ejemplo `REMOVED student.fax: was student.fax = user.contact.phone_number (DECLARED)`.

`FieldChange.Kind` — `public enum`, `@since 0.1.0`:

| Constante | Significado |
|---|---|
| `ADDED` | El campo es nuevo en el inventario posterior. Un campo personal nuevo significa datos personales nuevos que registrar. |
| `REMOVED` | El campo ya no está en el inventario posterior, ni siquiera como exento. |
| `CHANGED` | El campo está en ambos y cambió algo de lo que declara, incluido pasar a estar exento o dejar de estarlo. |

### Dirección

`direction` se calcula en los campos `CHANGED` resolviendo ambos lados contra la taxonomía dada a `between`:

| Situación | `direction` |
|---|---|
| El campo se añadió (`ADDED`) o se eliminó (`REMOVED`) | `SAME` |
| `CHANGED`, con la misma protección e identificabilidad, por ejemplo otra base u otro motivo | `SAME` |
| La protección posterior cubre a la anterior: más estricta en al menos una faceta, más débil en ninguna | `TIGHTENED` |
| La protección anterior cubre a la posterior | `LOOSENED` |
| Más estricta en una faceta y más débil en otra, o naturalezas que no se pueden comparar | `MIXED` |
| La clave de alguno de los dos lados no está en la taxonomía | `MIXED` |

La protección es la de [`InventoryField.protection(taxonomy)`](#inventoryfield), comparada con
[`Protection.comparedTo`](../protection-levels.md#protection). Un campo clasificado que pasa a estar exento es
`LOOSENED` (salvo que ya requiriera la protección más baja), y uno exento que pasa a estar clasificado es `TIGHTENED`;
un valor personal declarado `ANONYMIZED` o `AGGREGATED` pierde su naturaleza legal y es `LOOSENED`.

Cuando la protección es personal en ambos lados, también se compara la **identificabilidad**, con `PSEUDONYMIZED` por
debajo de `IDENTIFIED`, y las dos direcciones se combinan con
[`Direction.and`](../protection-levels.md#direction): `SAME` da la otra, dos direcciones iguales se mantienen y cualquier
otro par es `MIXED`. Por tanto, seudonimizar un campo es `LOOSENED`, volver a identificarlo es `TIGHTENED`, y
seudonimizar un campo mientras se eleva su confidencialidad es `MIXED`.

## Cambios de la taxonomía

`affectedBy(TaxonomyDiff diff)` responde a la otra pregunta: el código no cambió pero la taxonomía sí. Por cada
**categoría de datos** modificada en el [`TaxonomyDiff`](../taxonomy-diff.md) que declara al menos un campo, devuelve el
cambio con los campos que declaran su clave, en el orden del diff. Los usos y los sujetos de datos no están en un
inventario, así que sus cambios nunca se devuelven.

Una aplicación lo usa para migrar las claves que guarda cuando una categoría pasa a ser obsoleta, y para registrar
**una entrada por categoría modificada**, con los campos a los que afecta, en lugar de una entrada por campo. El
inventario en sí no cambia: el campo conserva la clave que declaró.

```java
Taxonomy applied = Taxonomy.fromTsv(Files.readString(appliedFile));       // la taxonomía aplicada la última vez
TaxonomyDiff taxonomyDiff = TaxonomyDiff.between(applied, taxonomy);
for (Map.Entry<TermChange, List<InventoryField>> entry : declared.affectedBy(taxonomyDiff).entrySet()) {
    TermChange change = entry.getKey();                   // una entrada de auditoría para la categoría
    for (InventoryField field : entry.getValue()) {
        // field.object(), field.path(): los campos que declaran change.key()
    }
}
```

## Uso

Reconciliar al arrancar un inventario guardado con el código:

```java
static void reconcile(Classifier classifier, Taxonomy taxonomy, List<Class<?>> entities,
                      Map<Class<?>, String> objectKeys, DataInventory stored) {
    DataInventory declared = classifier.inventory(entities, objectKeys::get);   // ClassificationException si hay algún error
    if (declared.fingerprint().equals(stored.fingerprint())) {
        return;                                                // nada cambió
    }
    InventoryDiff diff = InventoryDiff.between(stored, declared, taxonomy);
    for (FieldChange change : diff.changes()) {
        switch (change.kind()) {
            case ADDED -> { /* inserta change.after() */ }
            case REMOVED -> { /* retira change.before(), no lo borres */ }
            case CHANGED -> { /* reescribe la fila con change.after() */ }
        }
    }
    for (FieldChange change : diff.weakening()) {
        // pide una revisión antes de aplicar: la protección bajó o quedó mezclada
    }
}
```

Tras aplicar los cambios, conserva `declared.fingerprint()` (y, para limitar la próxima comparación a los objetos que se
movieron, `declared.fingerprint(object)` de cada objeto) junto a las filas guardadas. `diff.changedObjects()` lista los
objetos que hay que reconciliar ahora.

## Qué hace Linneo y qué hace la aplicación

Linneo **compara**; la aplicación **lee, escribe, bloquea y audita**. Los tipos del inventario no tocan un archivo ni
una base de datos, no bloquean nada mientras reconcilias, no guardan historial y no deciden qué acepta una revisión. Tu
aplicación elige las claves de objeto, guarda el inventario (una tabla, un archivo, un catálogo), aplica el plan de
`InventoryDiff` en una transacción, evita que dos nodos reconcilien a la vez y registra quién aprobó cada cambio que
debilita.

## Rendimiento

`tools/bench/InventoryBench.java` genera un modelo grande, lo compila y mide cada paso; no forma parte de la
compilación (véase [Contributing](https://github.com/devbpm/linneo/blob/main/CONTRIBUTING.md#performance)). En un
portátil de desarrollo, con 800 entidades y 39 000 campos:

| Paso | Tiempo |
|---|---|
| primer `verify` | unos 0,3 s |
| `inventory` | unos 0,2 s |
| `fingerprint` | unos 0,1 s |
| `InventoryDiff.between`, con el 1 % de los campos modificados | unos 0,08 s |
| `affectedBy` | unos 0,02 s |

Los tiempos dependen de la máquina; ejecuta la herramienta en la tuya.

Véase también: [Classifier](classifier.md) · [Resultados](results.md) · [Violaciones](violations.md) ·
[Diferencias entre taxonomías](../taxonomy-diff.md) · [Niveles de protección](../protection-levels.md#direction) ·
[ProcessingChecker](../processing/checker.md) · curso:
[Usar la clasificación](../../course/05-using-the-classification.md)
