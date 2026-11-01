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
immutable and thread-safe: each call reads every class it needs once (the class, the [`@Composite`](../annotations/composite.md)
classes it holds and the classes whose fields its [`@DerivedFrom`](../annotations/derived-from.md) fields name), detects
cycles, and keeps nothing between calls.

## Methods

| Member | Returns | Description |
|---|---|---|
| `Classifier(Taxonomy taxonomy)` | — | Creates a classifier for a taxonomy; keys must belong to it. `NullPointerException` if `taxonomy` is `null`. |
| `ClassClassification classify(Class<?> type)` | [`ClassClassification`](results.md#classclassification) | Classifies one class: its field classifications and every violation found. Never throws for classification problems. `NullPointerException` if `type` is `null`. |
| `List<ClassClassification> classifyAll(Collection<? extends Class<?>> types)` | immutable list | One classification per class, in the given order. A class used by several of them, as a composite or as the source of derived fields, is read once. |
| `List<ClassClassification> verify(Collection<? extends Class<?>> types)` | immutable list | Classifies the classes and throws `ClassificationException` carrying **every error of every class** if there is any (its message lists the first 50). Warnings do not fail. Returns the classifications when there is no error. |

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
  first and, within a class, the order the JVM reports, which is usually the declaration order but is not guaranteed.
  A field is identified by its `path`, which is its name, never by its position.
- Because a field is identified by its name, a field that **hides** a superclass field of the same name is a
  `DUPLICATE_FIELD` error: only the superclass field is classified.
- **Excluded** without any annotation: `static`, `transient` and synthetic fields (such as the reference to the
  enclosing instance in an inner class), and fields whose name starts with `$$_` (added by bytecode tools, for
  example Hibernate's enhancement: `$$_hibernate_interceptor`).
- **Hibernate proxies.** A class whose name contains `$HibernateProxy$` is classified as the entity it extends: the
  `type()` of its [`ClassClassification`](results.md#classclassification) is the entity, not the proxy.
- **Composites are entered.** A field with no annotation of its own whose type is a
  [`@Composite`](../annotations/composite.md) class, or an array, a collection or an `Optional` of one, is classified
  by the composite's fields: one entry per field, with the path `field.part` (`home.street`), under the composite's
  own annotations and `@DefaultCategory`, not the holder's. A `Map` is never descended into. A class that is not
  marked `@Composite`, such as a JPA `@Embedded` class, is one ordinary field that needs a category of its own (or a class default): mark the
  class `@Composite` rather than passing it to `verify` separately.
- **Derived fields.** A field marked [`@DerivedFrom`](../annotations/derived-from.md) takes the classification of its
  source, or declares one that protects at least as much. Its sources may be declared after it, be derived fields
  themselves, be parts of a composite, or belong to another class (`type`), which the classifier reads as well.
- Fields marked [`@Exempt`](../annotations/exempt.md) are not classified after their annotation is checked, and
  `ClassClassification.exempted()` lists them with their reasons. `@Exempt` is for technical fields; a value computed
  from other fields is data and uses `@DerivedFrom` or `@Category`.
- Annotations are read from the field itself (`getDeclaredAnnotation`), so on a record they must target the
  component's field, which `@Category`, `@CategoryByRow`, `@DerivedFrom` and `@Exempt` do.

Each remaining field is resolved by the [resolution order](../annotations/README.md#resolution-order): its own
`@Category` or `@CategoryByRow`; for a `@DerivedFrom` field without a category, the classification of its source; for
a field of a composite type, the fields of the composite; then the `@DefaultCategory` of the declaring class, then the
`@DefaultCategory` of the classified class, otherwise `UNCLASSIFIED_FIELD`.

Order of violations in a `ClassClassification`: first the checks of each `@DefaultCategory` in the hierarchy (top
down), then the field checks in the order of the fields (the problems of a composite at the position of the field that
holds it), and last the checks of the derived fields, which wait for their sources.

Protection never drops silently: a derived field must protect at least as much as each of its sources unless it
declares an anonymized or aggregated value, and a composite stored as one value must protect at least as much as the
fields of the composite. A composite that contains itself, or derivations that depend on each other, are a `CYCLE`.

## Fail-closed behaviour

Nothing is classified by default. These are **errors** (they fail `verify` and make `isValid()` false):

- a field with no annotation and no applicable class default (`UNCLASSIFIED_FIELD`);
- a key that is not in the taxonomy (`UNKNOWN_CATEGORY`);
- a personal category used as a class default (`PERSONAL_DEFAULT`);
- a blank reason or `declaredBy` (`MISSING_REASON`);
- annotations that exclude each other (`CONFLICTING_ANNOTATIONS`);
- a level lower than the category's (`LOWERED_LEVEL`);
- more than one value in an optional element of `@Category` (`AMBIGUOUS_VALUE`);
- an identifiability on a non-personal category (`IDENTIFIABILITY_NOT_APPLICABLE`);
- a field that hides a superclass field of the same name (`DUPLICATE_FIELD`);
- a derived field whose source is missing, exempted or in error, or that has none (`UNKNOWN_SOURCE`);
- a derived field without a category whose sources are classified differently (`AMBIGUOUS_DERIVATION`);
- a derived field that protects less than a source and is not declared anonymized or aggregated (`WEAKER_THAN_SOURCE`);
- a composite stored as one value that protects less than its fields (`WEAKER_THAN_PARTS`);
- a composite that contains itself, or derived fields that depend on each other (`CYCLE`).

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
[`Violation.toString()`](violations.md#violation). It lists the first 50 errors; if there are more, a last line says
how many were left out and counts all the errors by code. `errors()` always has every error.

```text
3 classification error(s):
 - ERROR UNCLASSIFIED_FIELD com.example.Person.undecided: no @Category on the field and no @DefaultCategory on Person
 - ERROR MISSING_REASON com.example.WrongDefault: @DefaultCategory of WrongDefault needs a reason
 - ERROR PERSONAL_DEFAULT com.example.WrongDefault: @DefaultCategory of WrongDefault names 'user.contact', which is personal data: declare personal fields one by one with @Category
```

With 120 errors the message lists the first 50 and ends with a line like this, where the codes are in alphabetical
order:

```text
 ... and 70 more; all of them by code: LOWERED_LEVEL 12, UNCLASSIFIED_FIELD 108
```

The `errors` list is held in a `transient` field: it is not preserved if the exception is serialized.

See also: [Results](results.md) · [Violations](violations.md) · [Annotations](../annotations/README.md) ·
course: [Verifying in tests](../../course/04-verifying-in-tests.md)
