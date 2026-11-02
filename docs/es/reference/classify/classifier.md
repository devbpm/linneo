---
title: "Classifier"
parent: "Referencia"
nav_order: 9
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - src/main/java/io/github/devbpm/linneo/classify/ClassificationException.java
  - src/main/java/io/github/devbpm/linneo/classify/ModelClasses.java
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

Sin JPA, toma las clases de un paquete con [`ModelClasses`](#modelclasses).

## ModelClasses

`io.github.devbpm.linneo.classify.ModelClasses` — `public final class`, `@since 0.1.0`

Encuentra las clases de un modelo, para que una prueba verifique todas las clases del modelo y no solo las que alguien
se acordó de listar: una clase añadida al modelo sin clasificar hace entonces que falle la compilación. No tiene
constructor público y solo tiene métodos estáticos.

| Miembro | Descripción |
|---|---|
| `static List<Class<?>> inPackage(Class<?> anchor)` | Las clases del paquete del ancla. |
| `static List<Class<?>> inPackageTree(Class<?> anchor)` | Las clases del paquete del ancla y de los paquetes que cuelgan de él. |

```java
@Test
void everyClassOfTheModelIsClassified() {
    new Classifier(Taxonomy.load()).verify(ModelClasses.inPackageTree(Student.class));
}
```

- **De dónde salen las clases.** De la ubicación desde la que se cargó la clase ancla: un directorio o un archivo JAR,
  en el class path o en el module path. Las clases del mismo paquete que estén en otra ubicación no se incluyen.
- **Qué clases.** Clases y records, incluidos los anidados y los abstractos. Se dejan fuera las interfaces, las
  anotaciones, los enumerados, las clases anónimas y las clases locales, y también las clases sintéticas,
  `package-info` y `module-info`. Se cargan sin inicializarlas y se ordenan por nombre, así que el resultado es el
  mismo en cada ejecución.
- **Se verifica todo.** Un paquete que además contiene clases que no son datos, como servicios o repositorios
  implementados como clases, hace que `verify` informe de sus campos sin clasificar: filtra antes la lista, por
  ejemplo por nombre.
- **Fail-closed.** `IllegalArgumentException` si el ancla está en el paquete sin nombre, si su ubicación no se puede
  listar (un JAR dentro de otro, clases generadas en tiempo de ejecución, una clase sin ubicación como las del JDK) o
  no existe, o si no se puede cargar una clase del paquete, por ejemplo porque falta una dependencia opcional. No se
  omite nada, así que ninguna clase queda sin verificar; pasa entonces las clases de forma explícita.
  `UncheckedIOException` si no se puede leer la ubicación; `NullPointerException` si `anchor` es `null`.

## Qué campos se examinan

- La clase y sus superclases, desde la superclase más alta hasta la propia clase. El recorrido se detiene antes de
  `Object`, `Record` y `Enum`; las interfaces no se visitan.
- De cada clase, los campos que devuelve `getDeclaredFields()`. La lista `fields()` resultante contiene primero los
  campos de las superclases y, dentro de una clase, el orden que informa la JVM, que normalmente es el de declaración
  pero no está garantizado. Un campo se identifica por su `path`, que es su nombre, nunca por su posición.
- Como un campo se identifica por su nombre, un campo que **oculta** otro del mismo nombre de una superclase es un
  error `DUPLICATE_FIELD`: solo se clasifica el campo de la superclase.
- Se **excluyen** sin necesidad de anotación los campos `static` y sintéticos (como la referencia a la instancia
  contenedora en una clase interna), los campos cuyo nombre empieza por `$$_` (los añaden herramientas de bytecode, por
  ejemplo la mejora de Hibernate: `$$_hibernate_interceptor`), los campos de bloqueo de `@Synchronized` de Lombok
  (`$lock`, `$LOCK`) y los campos `transient`. Un campo `transient` no se guarda, así que se omite, **salvo que lleve
  una de las anotaciones de campo de Linneo** (`@Category`, `@CategoryByRow`, `@DerivedFrom` o `@Exempt`): una anotación
  dice que contiene datos que clasificar, y se clasifica como cualquier otro campo.
- **Proxies de Hibernate.** Una clase cuyo nombre contiene `$HibernateProxy$` se clasifica como la entidad que
  extiende: el `type()` de su [`ClassClassification`](results.md#classclassification) es la entidad, no el proxy.
- **Se entra en los compuestos.** Un campo sin anotación propia cuyo tipo es una clase
  [`@Composite`](../annotations/composite.md) concreta (lo es también una subclase de un compuesto: la anotación se
  hereda), o un array, una colección o un `Optional` de ella, a cualquier profundidad, se clasifica por los campos del
  compuesto: una entrada por campo, con la ruta `campo.parte` (`home.street`), con las anotaciones y el
  `@DefaultCategory` del propio compuesto, no los de la clase contenedora. Las variables de tipo se resuelven a través
  de las superclases de la clase que se clasifica. Nunca se entra en un `Map`: sus claves también son datos.
- **Datos que el clasificador no ve.** Un campo que puede contener datos que el clasificador no puede dividir en campos
  nunca toma un valor por defecto de la clase y es un error `UNCLASSIFIED_FIELD` salvo que esté anotado: compuestos en
  un `Map` o un `Map.Entry` (declara la categoría con `@Category` o `@CategoryByRow`; se comprueba entonces contra los
  campos de los compuestos), un compuesto como argumento de tipo de una clase que no es una colección o de un tipo
  propietario, varios compuestos en un mismo tipo (un compuesto genérico como `Box<Address>` menciona dos, así que se
  declara como un solo valor), una interfaz o una clase abstracta marcada `@Composite` (no se incrusta: sus
  implementaciones pueden tener otros campos), un valor tipado `Object`, `Serializable`, `Comparable` o `Cloneable`, una
  variable de tipo sin enlazar o un contenedor sin parametrizar. Un valor por defecto de la clase clasificaría, si no,
  como no personal lo que envuelve datos personales.
- **Referencias a otros registros.** Un campo cuyo tipo es otro registro del modelo (una clase con un
  `@DefaultCategory`, un campo anotado o un campo de tipo compuesto; un tipo sellado se lee a través de sus subclases
  permitidas) toma el valor por defecto de la clase solo cuando ese registro no contiene datos personales, como un
  elemento de un catálogo tipo `Country`. Una referencia a un registro que contiene datos personales, como una
  asociación JPA con un `Customer` o una clase `@Embedded` no marcada `@Composite`, es en sí misma un dato personal, una
  clave seudónima: el campo es un error `UNCLASSIFIED_FIELD` que aconseja declarar lo que guarda, normalmente con
  `@DerivedFrom(type = Customer.class, value = "id")`.
- **Campos derivados.** Un campo marcado con [`@DerivedFrom`](../annotations/derived-from.md) toma la clasificación de
  su fuente, o declara una que protege al menos tanto. Sus fuentes pueden estar declaradas después de él, ser a su vez
  campos derivados, ser partes de un compuesto o pertenecer a otra clase (`type`), que el clasificador también lee.
  Las fuentes se resuelven campo a campo, así que dos clases pueden derivar de los *otros* campos de la otra, y un
  compuesto de una clase que lo contiene. Los problemas encontrados no dependen del orden en que se clasifiquen las
  clases; solo puede depender el campo en el que se informa un ciclo de contención de compuestos.
- Los campos marcados con [`@Exempt`](../annotations/exempt.md) no se clasifican tras comprobar su anotación, y
  `ClassClassification.exempted()` los lista con sus motivos. `@Exempt` es para campos técnicos; un valor calculado a
  partir de otros campos es un dato y usa `@DerivedFrom` o `@Category`, y un campo que contiene compuestos, o que se
  refiere a un registro con datos personales, es un error `CONFLICTING_ANNOTATIONS`.
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

La protección nunca baja en silencio: un campo derivado debe proteger al menos tanto como cada una de sus fuentes, y un
valor que declara anonimizado o agregado puede perder solo la **naturaleza personal** de sus fuentes, nunca su
confidencialidad, su integridad ni su carácter secreto. Un campo que guarda compuestos como un solo valor, declarado o
derivado, debe proteger al menos tanto como los campos de los compuestos, incluidos un compuesto que se contiene a sí
mismo y dos que se contienen entre sí, sea cual sea el orden de las clases; un compuesto cuyos campos tienen errores no
deja nada con lo que comparar, lo que también es un error (`WEAKER_THAN_PARTS`: `the fields of [X] have errors`). Un
compuesto que se contiene a sí mismo, y campos derivados que dependen unos de otros (dentro de una clase o entre
clases, también a través de las partes de un compuesto), son un `CYCLE`. Las clases que derivan de los otros campos de
la otra no lo son. Un campo que deriva de un campo con error, incluso uno en un ciclo, pero que no está él mismo en el
ciclo, es un `UNKNOWN_SOURCE`.

Cada clase informa de sus propios problemas, como un campo guardado como un solo valor que protege menos que su
compuesto o una referencia a un registro con datos personales, así que verifica todas las clases del modelo, con
[`ModelClasses`](#modelclasses), y no solo algunas.

## Comportamiento cerrado ante fallos (fail-closed)

Nada se clasifica por defecto. Son **errores** (hacen fallar `verify` y ponen `isValid()` a `false`):

- un campo sin anotación y sin valor por defecto de clase aplicable, o que se refiere a un registro con datos
  personales (`UNCLASSIFIED_FIELD`);
- una clave que no está en la taxonomía (`UNKNOWN_CATEGORY`);
- una categoría personal usada como valor por defecto de clase (`PERSONAL_DEFAULT`);
- un motivo o un `declaredBy` en blanco (`MISSING_REASON`);
- anotaciones que se excluyen entre sí, o `@Exempt` en un campo que contiene compuestos o se refiere a un registro con
  datos personales (`CONFLICTING_ANNOTATIONS`);
- un nivel inferior al de la categoría (`LOWERED_LEVEL`);
- más de un valor en un elemento opcional de `@Category` (`AMBIGUOUS_VALUE`);
- una identificabilidad en una categoría no personal (`IDENTIFIABILITY_NOT_APPLICABLE`);
- un campo que oculta otro del mismo nombre de una superclase (`DUPLICATE_FIELD`);
- un campo derivado cuya fuente no existe, está exenta o tiene un error, o que no tiene ninguna (`UNKNOWN_SOURCE`);
- un campo derivado sin categoría cuyas fuentes se clasifican de forma distinta (`AMBIGUOUS_DERIVATION`);
- un campo derivado que protege menos que una fuente, o que se declara anonimizado o agregado y pierde más que la
  naturaleza personal (`WEAKER_THAN_SOURCE`);
- un campo que guarda compuestos como un solo valor y protege menos que sus campos, o cuyos compuestos tienen errores en
  sus campos (`WEAKER_THAN_PARTS`);
- un compuesto que se contiene a sí mismo, o campos derivados que dependen unos de otros (`CYCLE`).

Una clave obsoleta es el único **aviso** (`DEPRECATED_CATEGORY`): el campo se clasifica con la sustituta vigente. Un
campo con error normalmente no aparece en `fields()`; consulta [Violaciones](violations.md) para ver el efecto exacto
de cada código.

## Inventario

`inventory(types, objectKey)` devuelve las clases como un [`DataInventory`](inventory.md): por cada clase, un objeto
bajo la clave que `objectKey` da para ella, con cada campo tal como el código lo declara (la clave tal como está
escrita, los niveles que el propio campo eleva, la identificabilidad, el motivo) y los campos exentos con sus motivos.
Es la forma de la clasificación que una aplicación guarda y compara, en lugar de las clases. La identificabilidad es la
que el campo **declara**, no la que le da la taxonomía, de modo que un inventario nunca depende de la taxonomía con la
que se construyó: un campo que no declara ninguna, o `IDENTIFIED`, que una categoría personal toma de todos modos, no
guarda ninguna.

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
