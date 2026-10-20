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
`Class.getDeclaredFields()`. Las instancias son inmutables y seguras entre hilos.

## Métodos

| Miembro | Devuelve | Descripción |
|---|---|---|
| `Classifier(Taxonomy taxonomy)` | — | Crea un clasificador para una taxonomía; las claves deben pertenecer a ella. `NullPointerException` si `taxonomy` es `null`. |
| `ClassClassification classify(Class<?> type)` | [`ClassClassification`](results.md#classclassification) | Clasifica una clase: las clasificaciones de sus campos y todas las violaciones encontradas. No lanza excepciones por problemas de clasificación. `NullPointerException` si `type` es `null`. |
| `List<ClassClassification> classifyAll(Collection<? extends Class<?>> types)` | lista inmutable | Una clasificación por clase, en el orden recibido. |
| `List<ClassClassification> verify(Collection<? extends Class<?>> types)` | lista inmutable | Clasifica las clases y, si hay algún error, lanza `ClassificationException` con **todos los errores de todas las clases**. Los avisos no hacen fallar. Si no hay errores, devuelve las clasificaciones. |

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
  campos de las superclases, en orden de declaración.
- Se **excluyen** sin necesidad de anotación los campos `static`, `transient` y sintéticos (como la referencia a la
  instancia contenedora en una clase interna).
- Los campos marcados con [`@Exempt`](../annotations/exempt.md) se omiten tras comprobar su anotación.
- Las anotaciones se leen del propio campo (`getDeclaredAnnotation`), así que en un record deben aplicarse al campo
  del componente, cosa que hacen `@Category`, `@CategoryByRow` y `@Exempt`.

Cada campo restante se resuelve según el [orden de resolución](../annotations/README.md#orden-de-resolución): su
propio `@Category` o `@CategoryByRow`, después el `@DefaultCategory` de la clase que lo declara, después el
`@DefaultCategory` de la clase clasificada y, si no, `UNCLASSIFIED_FIELD`.

Orden de las violaciones en una `ClassClassification`: primero las comprobaciones de cada `@DefaultCategory` de la
jerarquía (de arriba abajo) y después las de los campos, en el orden de los campos.

## Comportamiento cerrado ante fallos (fail-closed)

Nada se clasifica por defecto. Son **errores** (hacen fallar `verify` y ponen `isValid()` a `false`):

- un campo sin anotación y sin valor por defecto de clase aplicable (`UNCLASSIFIED_FIELD`);
- una clave que no está en la taxonomía (`UNKNOWN_CATEGORY`);
- una categoría personal usada como valor por defecto de clase (`PERSONAL_DEFAULT`);
- un motivo o un `declaredBy` en blanco (`MISSING_REASON`);
- anotaciones que se excluyen entre sí (`CONFLICTING_ANNOTATIONS`);
- un nivel inferior al de la categoría (`LOWERED_LEVEL`);
- más de un valor en un elemento opcional de `@Category` (`AMBIGUOUS_VALUE`);
- una identificabilidad en una categoría no personal (`IDENTIFIABILITY_NOT_APPLICABLE`).

Una clave obsoleta es el único **aviso** (`DEPRECATED_CATEGORY`): el campo se clasifica con la sustituta vigente. Un
campo con error normalmente no aparece en `fields()`; consulta [Violaciones](violations.md) para ver el efecto exacto
de cada código.

## ClassificationException

`io.github.devbpm.linneo.classify.ClassificationException` — `public class ClassificationException extends
RuntimeException`, `@since 0.1.0`

La lanza `Classifier.verify` cuando un modelo no está clasificado completa y correctamente.

| Miembro | Descripción |
|---|---|
| `ClassificationException(List<Violation> errors)` | Crea la excepción con los errores encontrados (al menos uno). La lista se copia. |
| `List<Violation> errors()` | Los errores, todos de gravedad `ERROR`, de todas las clases verificadas. |

El mensaje es el número de errores seguido de una línea por error, cada una con el formato de
[`Violation.toString()`](violations.md#violation):

```text
3 classification error(s):
 - ERROR UNCLASSIFIED_FIELD com.example.Person.undecided: no @Category on the field and no @DefaultCategory on Person
 - ERROR MISSING_REASON com.example.WrongDefault: @DefaultCategory of WrongDefault needs a reason
 - ERROR PERSONAL_DEFAULT com.example.WrongDefault: @DefaultCategory of WrongDefault names 'user.contact', which is personal data: declare personal fields one by one with @Category
```

La lista `errors` se guarda en un campo `transient`: no se conserva si la excepción se serializa.

Véase también: [Resultados](results.md) · [Violaciones](violations.md) · [Anotaciones](../annotations/README.md) ·
curso: [Verificar en los tests](../../course/04-verifying-in-tests.md)
