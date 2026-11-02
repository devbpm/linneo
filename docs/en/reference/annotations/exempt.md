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

Excludes from classification a field that holds no data of the model: a cache, a technical handle, state that a
framework keeps, or a collection of other records that are classified on their own, such as the children of a
one-to-many association.

A value computed from other fields is **not** such a field: the age computed from a date of birth is personal data.
Classify it with [`@DerivedFrom`](derived-from.md) or [`@Category`](category.md), never with `@Exempt`.

Static, synthetic and `$$_...` fields (added by Hibernate) are **always** excluded and need no annotation. An `@Exempt`
on such a field is ignored entirely (not even its reason is checked). A `transient` field is excluded too, **unless it
carries one of Linneo's field annotations** (`@Category`, `@CategoryByRow`, `@DerivedFrom` or `@Exempt`): then it is
classified like any other field, and an `@Exempt` on it is checked and listed in `exempted()`.

## Elements

| Element | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String` | required | Why the field holds no data, for the people who review the classification. Must not be blank. |

## Rules enforced by the Classifier

| Rule | Violation | Severity |
|---|---|---|
| `value` must not be blank | `MISSING_REASON` (`@Exempt needs a reason`) | error |
| Not combined with `@Category`, `@CategoryByRow` or `@DerivedFrom` | `CONFLICTING_ANNOTATIONS` (`@Exempt cannot be combined with @Category, @CategoryByRow or @DerivedFrom`) | error |

An exempt field never appears in `ClassClassification.fields()`, whether or not it has violations; both checks may
report on the same field. An exempt field is listed, with its reason, in
[`ClassClassification.exempted()`](../classify/results.md#exemption), so that excluding a field that used to be
classified is visible; the exception is a field that combines `@Exempt` with `@Category`, `@CategoryByRow` or
`@DerivedFrom`, which is an error and is not listed.

An exempt field cannot be the source of a [`@DerivedFrom`](derived-from.md): the source must be a classified field
(`UNKNOWN_SOURCE`). An exempt field inside a [`@Composite`](composite.md) is listed in `exempted()` with the path
`field.part`.

## Example

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.DerivedFrom;
import io.github.devbpm.linneo.annotation.Exempt;
import static io.github.devbpm.linneo.Categories.*;

public class Person {

    @Category(USER_NAME)
    private String fullName;

    @DerivedFrom("fullName")        // computed from fullName: data, so not @Exempt
    private String initials;

    @Exempt("handle the persistence framework keeps")
    private Object handle;          // technical: holds no data of the model

    private transient Object cache; // excluded without annotation
}
```

Exempting a collection of other records is right when each of them is classified on its own, with its own class (here
`Attachment`):

```java
@Exempt("relationship: each Attachment is classified on its own")
private List<Attachment> attachments;   // the children of a one-to-many association
```

See also: [Annotations overview](README.md) · [`@DerivedFrom`](derived-from.md) ·
[Classifier](../classify/classifier.md) · [Violations](../classify/violations.md) · course:
[Annotating models](../../course/03-annotating-models.md)
