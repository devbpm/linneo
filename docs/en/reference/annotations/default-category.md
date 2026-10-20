---
title: "@DefaultCategory"
parent: "Annotations"
grand_parent: "Reference"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/DefaultCategory.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @DefaultCategory

`io.github.devbpm.linneo.annotation.DefaultCategory` — `@Documented @Retention(RUNTIME) @Target(TYPE)`, `@since 0.1.0`

Declares the data category of every field of a class that has no annotation of its own: a positive statement that
those fields hold **no personal data**, with the reason. Personal data is always declared field by field with
[`@Category`](category.md).

## Elements

| Element | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String` | required | Key of a **non-personal** data category, preferably a constant of [`Categories`](../key-constants.md). |
| `reason` | `String` | required | Why the class holds no personal data, for the people who review the classification. Must not be blank. |

## Scope

- It applies to the unannotated fields the annotated class **declares**.
- When the annotated class is the class being classified, it also applies to unannotated fields inherited from
  superclasses that declare no default of their own.
- It is **not inherited**: a subclass without its own `@DefaultCategory` does not use its superclass's default for
  the fields the subclass declares.
- Field annotations (`@Category`, `@CategoryByRow`, `@Exempt`) always take precedence.

See the [resolution order](README.md#resolution-order).

## Rules enforced by the Classifier

The classifier checks the `@DefaultCategory` of the classified class and of each of its superclasses. These
violations have no field (`Violation.field()` is `null`) and their message starts with
`@DefaultCategory of <SimpleName>`.

| Rule | Violation | Severity |
|---|---|---|
| `reason` must not be blank | `MISSING_REASON` (`@DefaultCategory of <Class> needs a reason`) | error |
| `value` must be a key of the taxonomy | `UNKNOWN_CATEGORY` (`unknown data category '<key>'`) | error |
| A deprecated `value` is replaced by its current replacement | `DEPRECATED_CATEGORY` | warning |
| The (resolved) category must not be personal | `PERSONAL_DEFAULT` (`@DefaultCategory of <Class> names '<key>', which is personal data: declare personal fields one by one with @Category`) | error |

When the default is unknown or personal, the fields it would cover are left out of `ClassClassification.fields()`;
the class-level violation is the only report (no extra `UNCLASSIFIED_FIELD`). A blank `reason` alone does not stop
the fields from being classified.

## Resulting FieldClassification

| Component | Value |
|---|---|
| `declaringClass` | the class that declares the field |
| `category` | the resolved default category |
| `basis` | `CLASS_DEFAULT` |
| `reason` | the annotation's `reason` |
| `confidentiality` / `integrity` | the category's levels |
| `identifiability` | `null` |

## Examples

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import static io.github.devbpm.linneo.Categories.*;

public abstract class Audited {
    @Category(USER_UNIQUE_ID)
    private Long createdBy;                 // DECLARED
    private java.time.Instant createdAt;    // covered by the default of the classified subclass
}

@DefaultCategory(value = SYSTEM_CONFIGURATION, reason = "menu configuration")
public class MenuGroup extends Audited {
    private String code;                    // CLASS_DEFAULT: system.configuration
}
```

Classifying `MenuGroup` gives the fields `createdBy`, `createdAt` and `code`. Classifying another subclass of
`Audited` without a default reports `UNCLASSIFIED_FIELD` for `createdAt`.

```java
@DefaultCategory(value = USER_CONTACT, reason = "")    // PERSONAL_DEFAULT and MISSING_REASON
public class WrongDefault {
    private String value;
}
```

See also: [Annotations overview](README.md) · [`@Category`](category.md) · [Violations](../classify/violations.md) ·
[Standard taxonomy](../standard-taxonomy.md) · course: [Annotating models](../../course/03-annotating-models.md)
