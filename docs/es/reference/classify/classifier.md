---
title: "Classifier"
parent: "Referencia"
nav_order: 9
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - src/main/java/io/github/devbpm/linneo/classify/ClassificationException.java
-->

# Classifier

`io.github.devbpm.linneo.classify.Classifier` — `public final class`, `@since 0.1.0`

Lee las anotaciones de clasificación de las clases y las comprueba contra una [`Taxonomy`](../taxonomy.md). Funciona
igual con clases normales, records y entidades JPA, y no necesita ninguna biblioteca de persistencia. Solo lee
anotaciones, nunca los valores de los campos, así que no necesita más acceso a las clases que el que exige
`Class.getDeclaredFields()`. Las instancias son inmutables y seguras entre hilos: cada llamada lee una sola vez cada
clase que necesita (la clase, las clases [`@Composite`](../annotations/composite.md) que contiene y las clases cuyos
campos nombran sus campos [`@DerivedFrom`](../annotations/derived-from.md)), detecta ciclos y no conserva nada entre
llamadas.

## Métodos

| Miembro | Devuelve | Descripción |
|---|---|---|
| `Classifier(Taxonomy taxonomy)` | — | Crea un clasificador para una taxonomía; las claves deben pertenecer a ella. `NullPointerException` si `taxonomy` es `null`. |
| `ClassClassification classify(Class<?> type)` | [`ClassClassification`](results.md#classclassification) | Clasifica una clase: las clasificaciones de sus campos y todas las violaciones encontradas. No lanza excepciones por problemas de clasificación. `NullPointerException` si `type` es `null`. |
| `List<ClassClassification> classifyAll(Collection<? extends Class<?>> types)` | lista inmutable | Una clasificación por clase, en el orden recibido. Una clase que usan varias de ellas, como compuesto o como fuente de campos derivados, se lee una sola vez. |
| `List<ClassClassification> verify(Collection<? extends Class<?>> types)` | lista inmutable | Clasifica las clases y, si hay algún error, lanza `ClassificationException` con **todos los errores de todas las clases** (su mensaje lista los 50 primeros). Los avisos no hacen fallar. Si no hay errores, devuelve las clasificaciones. |
| `DataInventory inventory(Collection<? extends Class<?>> types, Function<? super Class<?>, String> objectKey)` | [`DataInventory`](inventory.md) | La clasificación de las clases como datos, bajo las claves de objeto que da `objectKey`; véase [Inventario](#inventario). Verifica antes las clases. |

```java
@Test
void everyEntityIsClassified() {
    new Classifier(Taxonomy.load()).verify(List.of(Customer.class, Invoice.class, MenuGroup.class));
}
```

Con JPA, toma las clases del metamodelo:

```java
List<Class<?>> entities = entityManagerFactory.getMetamodel().getEntities().stream()
        .<Class<?>>map(EntityType::getJavaType)
        .toList();
new Classifier(Taxonomy.load()).verify(entities);
```

## Qué campos se examinan

- La clase y sus superclases, desde la superclase más alta hasta la propia clase. El recorrido se detiene antes de
  `Object`, `Record` y `Enum`; las interfaces no se visitan.
- De cada clase, los campos que devuelve `getDeclaredFields()`. La lista `fields()` resultante contiene primero los
  campos de las superclases y, dentro de una clase, el orden que informa la JVM, que normalmente es el de declaración
  pero no está garantizado. Un campo se identifica por su `path`, que es su nombre, nunca por su posición.
- Como un campo se identifica por su nombre, un campo que **oculta** otro del mismo nombre de una superclase es un
  error `DUPLICATE_FIELD`: solo se clasifica el campo de la superclase.
- Se **excluyen** sin necesidad de anotación los campos `static`, `transient` y sintéticos (como la referencia a la
  instancia contenedora en una clase interna) y los campos cuyo nombre empieza por `$$_` (los añaden herramientas de
  bytecode, por ejemplo la mejora de Hibernate: `$$_hibernate_interceptor`).
- **Proxies de Hibernate.** Una clase cuyo nombre contiene `$HibernateProxy$` se clasifica como la entidad que
  extiende: el `type()` de su [`ClassClassification`](results.md#classclassification) es la entidad, no el proxy.
- **Se entra en los compuestos.** Un campo sin anotación propia cuyo tipo es una clase
  [`@Composite`](../annotations/composite.md), o un array, una colección o un `Optional` de ella, se clasifica por los
  campos del compuesto: una entrada por campo, con la ruta `campo.parte` (`home.street`), con las anotaciones y el
  `@DefaultCategory` del propio compuesto, no los de la clase contenedora. Un `Map` nunca se recorre. Una clase que no
  está marcada `@Composite`, como una clase `@Embedded` de JPA, es un campo ordinario que necesita una categoría
  propia (o un valor por defecto de la clase): marca la clase `@Composite` en lugar de pasarla a `verify` por
  separado.
- **Campos derivados.** Un campo marcado con [`@DerivedFrom`](../annotations/derived-from.md) toma la clasificación de
  su fuente, o declara una que protege al menos tanto. Sus fuentes pueden estar declaradas después de él, ser a su vez
  campos derivados, ser partes de un compuesto o pertenecer a otra clase (`type`), que el clasificador también lee.
  Las fuentes se resuelven campo a campo, así que dos clases pueden derivar de los *otros* campos de la otra, y un
  compuesto de una clase que lo contiene. El resultado no depende del orden en que se clasifiquen las clases.
- Los campos marcados con [`@Exempt`](../annotations/exempt.md) no se clasifican tras comprobar su anotación, y
  `ClassClassification.exempted()` los lista con sus motivos. `@Exempt` es para campos técnicos; un valor calculado a
  partir de otros campos es un dato y usa `@DerivedFrom` o `@Category`.
- Las anotaciones se leen del propio campo (`getDeclaredAnnotation`), así que en un record deben aplicarse al campo
  del componente, cosa que hacen `@Category`, `@CategoryByRow`, `@DerivedFrom` y `@Exempt`.

Cada campo restante se resuelve según el [orden de resolución](../annotations/README.md#orden-de-resolución): su
propio `@Category` o `@CategoryByRow`; para un campo `@DerivedFrom` sin categoría, la clasificación de su fuente; para
un campo de tipo compuesto, los campos del compuesto; después el `@DefaultCategory` de la clase que lo declara, después
el `@DefaultCategory` de la clase clasificada y, si no, `UNCLASSIFIED_FIELD`.

Orden de las violaciones en una `ClassClassification`: los problemas de una clase se listan en el orden de sus campos.
Los que no tienen ruta van primero, como las comprobaciones de cada `@DefaultCategory` de la jerarquía (de arriba
abajo), y los problemas de un compuesto están en la posición del campo que lo contiene (ruta `campo.parte`). El orden
no depende del orden en que se clasifiquen las clases.

La protección nunca baja en silencio: un campo derivado debe proteger al menos tanto como cada una de sus fuentes,
salvo que declare un valor anonimizado o agregado, y un compuesto guardado como un solo valor debe proteger al menos
tanto como los campos del compuesto. Un compuesto que se contiene a sí mismo, y campos derivados que dependen unos de
otros (dentro de una clase o entre clases, también a través de las partes de un compuesto), son un `CYCLE`. Las clases
que derivan de los otros campos de la otra no lo son. Un campo que deriva de un campo con error, incluso uno en un
ciclo, pero que no está él mismo en el ciclo, es un `UNKNOWN_SOURCE`.

## Comportamiento cerrado ante fallos (fail-closed)

Nada se clasifica por defecto. Son **errores** (hacen fallar `verify` y ponen `isValid()` a `false`):

- un campo sin anotación y sin valor por defecto de clase aplicable (`UNCLASSIFIED_FIELD`);
- una clave que no está en la taxonomía (`UNKNOWN_CATEGORY`);
- una categoría personal usada como valor por defecto de clase (`PERSONAL_DEFAULT`);
- un motivo o un `declaredBy` en blanco (`MISSING_REASON`);
- anotaciones que se excluyen entre sí (`CONFLICTING_ANNOTATIONS`);
- un nivel inferior al de la categoría (`LOWERED_LEVEL`);
- más de un valor en un elemento opcional de `@Category` (`AMBIGUOUS_VALUE`);
- una identificabilidad en una categoría no personal (`IDENTIFIABILITY_NOT_APPLICABLE`);
- un campo que oculta otro del mismo nombre de una superclase (`DUPLICATE_FIELD`);
- un campo derivado cuya fuente no existe, está exenta o tiene un error, o que no tiene ninguna (`UNKNOWN_SOURCE`);
- un campo derivado sin categoría cuyas fuentes se clasifican de forma distinta (`AMBIGUOUS_DERIVATION`);
- un campo derivado que protege menos que una fuente y no se declara anonimizado o agregado (`WEAKER_THAN_SOURCE`);
- un compuesto guardado como un solo valor que protege menos que sus campos (`WEAKER_THAN_PARTS`);
- un compuesto que se contiene a sí mismo, o campos derivados que dependen unos de otros (`CYCLE`).

Una clave obsoleta es el único **aviso** (`DEPRECATED_CATEGORY`): el campo se clasifica con la sustituta vigente. Un
campo con error normalmente no aparece en `fields()`; consulta [Violaciones](violations.md) para ver el efecto exacto
de cada código.

## Inventario

`inventory(types, objectKey)` devuelve las clases como un [`DataInventory`](inventory.md): por cada clase, un objeto
bajo la clave que `objectKey` da para ella, con cada campo tal como el código lo declara (la clave tal como está
escrita, los niveles que el propio campo eleva, la identificabilidad, el motivo) y los campos exentos con sus motivos. Es
la forma de la clasificación que una aplicación guarda y compara, en lugar de las clases.

```java
Map<Class<?>, String> objectKeys = Map.of(Student.class, "student", Invoice.class, "invoice");
DataInventory inventory = new Classifier(Taxonomy.load()).inventory(List.of(Student.class, Invoice.class), objectKeys::get);
```

- **Verifica** las clases como hace `verify`: si alguna tiene un error lanza `ClassificationException` con todos los
  errores, y los avisos no hacen fallar.
- Una clase repetida cuenta una sola vez.
- `NullPointerException` si `objectKey` o una clase son `null`. `IllegalArgumentException` si `objectKey` da una clave
  `null` o en blanco para una clase, o la misma clave para dos clases.
- La clave de objeto debe ser **estable**, ya que los registros y los catálogos guardados se emparejan con ella: una
  clave que cambia al renombrar o mover una clase convierte cada campo de la clase en un campo eliminado y uno nuevo.
  Tómala de lo que ya identifica la clase en la aplicación, como su objeto de negocio o el nombre de su tabla, no del
  nombre de la clase.

Consulta [Inventario de datos](inventory.md) para el inventario, su forma de texto, sus huellas y cómo comparar dos.

## ClassificationException

`io.github.devbpm.linneo.classify.ClassificationException` — `public class ClassificationException extends
RuntimeException`, `@since 0.1.0`

La lanzan `Classifier.verify` y `Classifier.inventory` cuando un modelo no está clasificado completa y correctamente.

| Miembro | Descripción |
|---|---|
| `ClassificationException(List<Violation> errors)` | Crea la excepción con los errores encontrados (al menos uno). La lista se copia. |
| `List<Violation> errors()` | Los errores, todos de gravedad `ERROR`, de todas las clases verificadas. |

El mensaje es el número de errores seguido de una línea por error, cada una con el formato de
[`Violation.toString()`](violations.md#violation). Lista los 50 primeros errores; si hay más, una última línea indica
cuántos quedaron fuera y cuenta todos los errores por código. `errors()` siempre contiene todos.

```text
3 classification error(s):
 - ERROR UNCLASSIFIED_FIELD com.example.Person.undecided: no @Category on the field and no @DefaultCategory on Person
 - ERROR MISSING_REASON com.example.WrongDefault: @DefaultCategory of WrongDefault needs a reason
 - ERROR PERSONAL_DEFAULT com.example.WrongDefault: @DefaultCategory of WrongDefault names 'user.contact', which is personal data: declare personal fields one by one with @Category
```

Con 120 errores, el mensaje lista los 50 primeros y termina con una línea como esta, con los códigos en orden
alfabético:

```text
 ... and 70 more; all of them by code: LOWERED_LEVEL 12, UNCLASSIFIED_FIELD 108
```

La lista `errors` se guarda en un campo `transient`: no se conserva si la excepción se serializa.

Véase también: [Resultados](results.md) · [Violaciones](violations.md) · [Inventario de datos](inventory.md) ·
[Anotaciones](../annotations/README.md) ·
curso: [Verificar en los tests](../../course/04-verifying-in-tests.md)
