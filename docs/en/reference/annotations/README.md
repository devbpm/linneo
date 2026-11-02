---
title: "Annotations"
parent: "Reference"
nav_order: 8
permalink: "/en/reference/annotations/"
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/Category.java
  - src/main/java/io/github/devbpm/linneo/annotation/DefaultCategory.java
  - src/main/java/io/github/devbpm/linneo/annotation/CategoryByRow.java
  - src/main/java/io/github/devbpm/linneo/annotation/DerivedFrom.java
  - src/main/java/io/github/devbpm/linneo/annotation/Composite.java
  - src/main/java/io/github/devbpm/linneo/annotation/Exempt.java
  - src/main/java/io/github/devbpm/linneo/annotation/package-info.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# Annotations

Six annotations in `io.github.devbpm.linneo.annotation` declare what a data model holds. All are `@Documented`,
`@Retention(RUNTIME)` and `@since 0.1.0`. They are read by the [`Classifier`](../classify/classifier.md), which never
reads field values.

| Annotation | Target | Declares | Page |
|---|---|---|---|
| `@Category` | field, record component | the data category of one field, optionally with stricter levels and its identifiability | [category.md](category.md) |
| `@DefaultCategory` | type | the non-personal category of every unannotated field of a class, with the reason | [default-category.md](default-category.md) |
| `@CategoryByRow` | field, record component | a field whose category depends on each row, with the most protective category a row may hold | [category-by-row.md](category-by-row.md) |
| `@DerivedFrom` | field, record component | a field whose value is computed from other fields: it takes their classification, or declares one that protects at least as much | [derived-from.md](derived-from.md) |
| `@Composite` | type | a class that is part of another record, such as an embedded address: the fields that hold it are classified by its own fields | [composite.md](composite.md) |
| `@Exempt` | field, record component | a field that holds no data of the model (cache, handle or framework state), with the reason | [exempt.md](exempt.md) |

On a record component, the annotations also apply to the component's field, which is what the classifier reads.

## Which fields are classified

The classifier walks the class and its superclasses (stopping before `Object`, `Record` and `Enum`) and considers every
field declared by them except **static** and **synthetic** fields (and those named `$$_...`, added by Hibernate), which
are always skipped without any annotation, and **transient** fields, which are skipped unless they carry one of Linneo's
field annotations. See [Classifier](../classify/classifier.md#which-fields-are-scanned).

A field without an annotation of its own whose type is a concrete [`@Composite`](composite.md) class (or an array,
collection or `Optional` of one, at any depth) is not classified as one field: each field of the composite is, with
the path `field.part`.

## Resolution order

For each considered field, the first rule that applies wins:

| # | Rule | Result (`FieldClassification.basis()`) |
|---|---|---|
| 1 | The field has `@Exempt` | skipped: no classification |
| 2 | The field has `@Category` | the declared category (`DECLARED`; `DERIVED` if it also has `@DerivedFrom`) |
| 2 | The field has `@CategoryByRow` | the ceiling category (`BY_ROW`) |
| 3 | The field has `@DerivedFrom` and no `@Category` | the classification of its source (`DERIVED`) |
| 4 | The type of the field is a concrete `@Composite` class, or an array, collection or `Optional` of one, at any depth | one classification per field of the composite, with the path `field.part`, each as its own class resolves it |
| 5 | The class that **declares** the field has `@DefaultCategory`, and the field cannot hold data the classifier does not see and refers to no record with personal data or with errors | that default (`CLASS_DEFAULT`) |
| 6 | The class **being classified** has `@DefaultCategory`, and the same holds | that default (`CLASS_DEFAULT`) |
| 7 | none of the above | error `UNCLASSIFIED_FIELD` |

Consequences:

- An annotation on a field always overrides a class default.
- The fields of a composite are resolved with the composite's own annotations and `@DefaultCategory`, never with the
  default of the class that holds it.
- A class default never applies to a field that may hold data the classifier cannot see (composites in a map or that
  cannot be split into fields, an abstract composite, `Object`, `Serializable`, an unbound type variable or a raw
  container): such a field needs its own annotation, or it is an `UNCLASSIFIED_FIELD`. See
  [`@Composite`](composite.md#what-the-classifier-does).
- A field that refers to another record of the model takes the class default only when that record holds no personal
  data, as a catalogue entry does. A reference to a record with personal data needs its own annotation, typically
  `@DerivedFrom(type = Customer.class, value = "id")`, and cannot be exempted. The same holds for a reference to a
  record whose fields have errors, since whether it holds personal data is then unknown. See
  [References to other records](composite.md#references-to-other-records).
- A superclass's `@DefaultCategory` covers the fields that superclass declares, even when you classify a subclass.
- A subclass's `@DefaultCategory` covers the fields it declares and the unannotated fields inherited from superclasses
  that have no default of their own.
- `@DefaultCategory` is not inherited: a subclass without its own default does not get its superclass's default for
  the fields the subclass declares.
- A deprecated key in any annotation still classifies the field, with the current replacement, and produces a
  `DEPRECATED_CATEGORY` warning.

## Conflicts

| Combination on one field | Violation | Effect |
|---|---|---|
| `@Exempt` + `@Category`, `@CategoryByRow` and/or `@DerivedFrom` | `CONFLICTING_ANNOTATIONS`: `@Exempt cannot be combined with @Category, @CategoryByRow or @DerivedFrom` | field skipped (the `@Exempt` reason is still checked) |
| `@Exempt` on a field that holds composites, or that refers to a record with personal data or with errors | `CONFLICTING_ANNOTATIONS`: `@Exempt is for a field that holds no data of the model, and this one ...` | error reported; see [`@Exempt`](exempt.md#rules-enforced-by-the-classifier) |
| `@CategoryByRow` + `@Category` and/or `@DerivedFrom` | `CONFLICTING_ANNOTATIONS`: `@CategoryByRow cannot be combined with @Category or @DerivedFrom` | field not classified |
| `@Category` + `@DerivedFrom` | none | the field declares its category and must protect at least as much as its sources |
| `@Category`, `@CategoryByRow` or `@DerivedFrom` + class `@DefaultCategory` | none | the field annotation wins |

The conflicts are errors. See [Violations](../classify/violations.md) for every code.

## Example

```java
import io.github.devbpm.linneo.annotation.*;
import static io.github.devbpm.linneo.Categories.*;

@DefaultCategory(value = SYSTEM_CONFIGURATION, reason = "menu configuration")
public class MenuGroup {
    private String code;                      // CLASS_DEFAULT: system.configuration
    private String label;                     // CLASS_DEFAULT: system.configuration

    @Category(USER_UNIQUE_ID)                 // DECLARED: overrides the class default
    private Long createdBy;

    @Exempt("cache of the rendered menu")
    private String renderedMenu;              // skipped

    private transient String cache;           // skipped: transient
}
```

See also: [`@Category`](category.md) · [`@DefaultCategory`](default-category.md) ·
[`@CategoryByRow`](category-by-row.md) · [`@DerivedFrom`](derived-from.md) · [`@Composite`](composite.md) ·
[`@Exempt`](exempt.md) · [Classifier](../classify/classifier.md) ·
course: [Annotating models](../../course/03-annotating-models.md)
