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
  - src/main/java/io/github/devbpm/linneo/annotation/Exempt.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# Annotations

Four annotations in `io.github.devbpm.linneo.annotation` declare what a data model holds. All are `@Documented`,
`@Retention(RUNTIME)` and `@since 0.1.0`. They are read by the [`Classifier`](../classify/classifier.md), which never
reads field values.

| Annotation | Target | Declares | Page |
|---|---|---|---|
| `@Category` | field, record component | the data category of one field, optionally with stricter levels and its identifiability | [category.md](category.md) |
| `@DefaultCategory` | type | the non-personal category of every unannotated field of a class, with the reason | [default-category.md](default-category.md) |
| `@CategoryByRow` | field, record component | a field whose category depends on each row, with the most protective category a row may hold | [category-by-row.md](category-by-row.md) |
| `@Exempt` | field, record component | a field that holds no data of the model, with the reason | [exempt.md](exempt.md) |

On a record component, the annotations also apply to the component's field, which is what the classifier reads.

## Which fields are classified

The classifier walks the class and its superclasses (stopping before `Object`, `Record` and `Enum`) and considers
every field declared by them except **static**, **transient** and **synthetic** fields (and those named `$$_...`, added by Hibernate), which are always skipped
without any annotation. See [Classifier](../classify/classifier.md#which-fields-are-scanned).

## Resolution order

For each considered field, the first rule that applies wins:

| # | Rule | Result (`FieldClassification.basis()`) |
|---|---|---|
| 1 | The field has `@Exempt` | skipped: no classification |
| 2 | The field has `@Category` | the declared category (`DECLARED`) |
| 2 | The field has `@CategoryByRow` | the ceiling category (`BY_ROW`) |
| 3 | The class that **declares** the field has `@DefaultCategory` | that default (`CLASS_DEFAULT`) |
| 4 | The class **being classified** has `@DefaultCategory` | that default (`CLASS_DEFAULT`) |
| 5 | none of the above | error `UNCLASSIFIED_FIELD` |

Consequences:

- An annotation on a field always overrides a class default.
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
| `@Exempt` + `@Category` and/or `@CategoryByRow` | `CONFLICTING_ANNOTATIONS`: `@Exempt cannot be combined with @Category or @CategoryByRow` | field skipped (the `@Exempt` reason is still checked) |
| `@Category` + `@CategoryByRow` | `CONFLICTING_ANNOTATIONS`: `@Category and @CategoryByRow cannot be used together` | field not classified |
| `@Category` or `@CategoryByRow` + class `@DefaultCategory` | none | the field annotation wins |

All conflicts are errors. See [Violations](../classify/violations.md) for every code.

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

    @Exempt("computed from code")
    private String displayKey;                // skipped

    private transient String cache;           // skipped: transient
}
```

See also: [`@Category`](category.md) · [`@DefaultCategory`](default-category.md) ·
[`@CategoryByRow`](category-by-row.md) · [`@Exempt`](exempt.md) · [Classifier](../classify/classifier.md) ·
course: [Annotating models](../../course/03-annotating-models.md)
