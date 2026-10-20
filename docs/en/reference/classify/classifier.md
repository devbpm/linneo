---
title: "Classifier"
parent: "Reference"
nav_order: 9
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - src/main/java/io/github/devbpm/linneo/classify/ClassificationException.java
-->

# Classifier

`io.github.devbpm.linneo.classify.Classifier` — `public final class`, `@since 0.1.0`

Reads the classification annotations of classes and checks them against a [`Taxonomy`](../taxonomy.md). It works on
plain classes, records and JPA entities alike and needs no persistence library. It only reads annotations, never
field values, so it needs no access to the classes beyond what `Class.getDeclaredFields()` requires. Instances are
immutable and thread-safe.

## Methods

| Member | Returns | Description |
|---|---|---|
| `Classifier(Taxonomy taxonomy)` | — | Creates a classifier for a taxonomy; keys must belong to it. `NullPointerException` if `taxonomy` is `null`. |
| `ClassClassification classify(Class<?> type)` | [`ClassClassification`](results.md#classclassification) | Classifies one class: its field classifications and every violation found. Never throws for classification problems. `NullPointerException` if `type` is `null`. |
| `List<ClassClassification> classifyAll(Collection<? extends Class<?>> types)` | immutable list | One classification per class, in the given order. |
| `List<ClassClassification> verify(Collection<? extends Class<?>> types)` | immutable list | Classifies the classes and throws `ClassificationException` listing **every error of every class** if there is any. Warnings do not fail. Returns the classifications when there is no error. |

```java
@Test
void everyEntityIsClassified() {
    new Classifier(Taxonomy.load()).verify(List.of(Customer.class, Invoice.class, MenuGroup.class));
}
```

With JPA, take the classes from the metamodel:

```java
List<Class<?>> entities = entityManagerFactory.getMetamodel().getEntities().stream()
        .<Class<?>>map(EntityType::getJavaType)
        .toList();
new Classifier(Taxonomy.load()).verify(entities);
```

## Which fields are scanned

- The class and its superclasses, from the topmost superclass down to the class itself. The walk stops before
  `Object`, `Record` and `Enum`; interfaces are not visited.
- For each class, the fields returned by `getDeclaredFields()`. The resulting `fields()` list holds superclass fields
  first, in declaration order.
- **Excluded** without any annotation: `static`, `transient` and synthetic fields (such as the reference to the
  enclosing instance in an inner class).
- Fields marked [`@Exempt`](../annotations/exempt.md) are skipped after their annotation is checked.
- Annotations are read from the field itself (`getDeclaredAnnotation`), so on a record they must target the
  component's field, which `@Category`, `@CategoryByRow` and `@Exempt` do.

Each remaining field is resolved by the [resolution order](../annotations/README.md#resolution-order): its own
`@Category` or `@CategoryByRow`, then the `@DefaultCategory` of the declaring class, then the `@DefaultCategory` of
the classified class, otherwise `UNCLASSIFIED_FIELD`.

Order of violations in a `ClassClassification`: first the checks of each `@DefaultCategory` in the hierarchy (top
down), then the field checks in field order.

## Fail-closed behaviour

Nothing is classified by default. These are **errors** (they fail `verify` and make `isValid()` false):

- a field with no annotation and no applicable class default (`UNCLASSIFIED_FIELD`);
- a key that is not in the taxonomy (`UNKNOWN_CATEGORY`);
- a personal category used as a class default (`PERSONAL_DEFAULT`);
- a blank reason or `declaredBy` (`MISSING_REASON`);
- annotations that exclude each other (`CONFLICTING_ANNOTATIONS`);
- a level lower than the category's (`LOWERED_LEVEL`);
- more than one value in an optional element of `@Category` (`AMBIGUOUS_VALUE`);
- an identifiability on a non-personal category (`IDENTIFIABILITY_NOT_APPLICABLE`).

A deprecated key is the only **warning** (`DEPRECATED_CATEGORY`): the field is classified with the current
replacement. A field with an error is usually missing from `fields()`; see [Violations](violations.md) for the exact
effect of each code.

## ClassificationException

`io.github.devbpm.linneo.classify.ClassificationException` — `public class ClassificationException extends
RuntimeException`, `@since 0.1.0`

Thrown by `Classifier.verify` when a model is not fully and correctly classified.

| Member | Description |
|---|---|
| `ClassificationException(List<Violation> errors)` | Creates the exception from the errors found (at least one). The list is copied. |
| `List<Violation> errors()` | The errors, all of severity `ERROR`, from every verified class. |

The message is the number of errors followed by one line per error, each in the format of
[`Violation.toString()`](violations.md#violation):

```text
3 classification error(s):
 - ERROR UNCLASSIFIED_FIELD com.example.Person.undecided: no @Category on the field and no @DefaultCategory on Person
 - ERROR MISSING_REASON com.example.WrongDefault: @DefaultCategory of WrongDefault needs a reason
 - ERROR PERSONAL_DEFAULT com.example.WrongDefault: @DefaultCategory of WrongDefault names 'user.contact', which is personal data: declare personal fields one by one with @Category
```

The `errors` list is held in a `transient` field: it is not preserved if the exception is serialized.

See also: [Results](results.md) · [Violations](violations.md) · [Annotations](../annotations/README.md) ·
course: [Verifying in tests](../../course/04-verifying-in-tests.md)
