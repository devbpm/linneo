---
title: "@Exempt"
parent: "Annotations"
grand_parent: "Reference"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/Exempt.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @Exempt

`io.github.devbpm.linneo.annotation.Exempt` —
`@Documented @Retention(RUNTIME) @Target({FIELD, RECORD_COMPONENT})`, `@since 0.1.0`

Excludes from classification a field that holds no data of the model: a value computed from other fields, a cache or
a technical handle.

Static, synthetic, `transient` and `$$_...` fields (added by Hibernate) are **always** excluded and need no annotation. An `@Exempt` on such a field
is ignored entirely (not even its reason is checked).

## Elements

| Element | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String` | required | Why the field holds no data, for the people who review the classification. Must not be blank. |

## Rules enforced by the Classifier

| Rule | Violation | Severity |
|---|---|---|
| `value` must not be blank | `MISSING_REASON` (`@Exempt needs a reason`) | error |
| Not combined with `@Category` or `@CategoryByRow` | `CONFLICTING_ANNOTATIONS` (`@Exempt cannot be combined with @Category or @CategoryByRow`) | error |

An exempt field never appears in `ClassClassification.fields()`, whether or not it has violations; both checks may
report on the same field.

## Example

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.Exempt;
import static io.github.devbpm.linneo.Categories.*;

public class Person {

    @Category(USER_NAME)
    private String fullName;

    @Exempt("computed from fullName")
    private String initials;

    private transient Object cache;     // excluded without annotation
}
```

See also: [Annotations overview](README.md) · [Classifier](../classify/classifier.md) ·
[Violations](../classify/violations.md) · course: [Annotating models](../../course/03-annotating-models.md)
