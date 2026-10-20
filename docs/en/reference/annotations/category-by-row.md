---
title: "@CategoryByRow"
parent: "Annotations"
grand_parent: "Reference"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/CategoryByRow.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @CategoryByRow

`io.github.devbpm.linneo.annotation.CategoryByRow` —
`@Documented @Retention(RUNTIME) @Target({FIELD, RECORD_COMPONENT})`, `@since 0.1.0`

Declares a field whose data category depends on each row, such as the content of a document that may be a
certificate in one row and a medical report in the next. Wherever a single category is needed for the whole field (a
catalogue, an export label) the `ceiling` is used: the most protective category any row may hold.

## Elements

| Element | Type | Default | Meaning |
|---|---|---|---|
| `declaredBy` | `String` | required | What decides the category of each row, for the people who review the classification. Must not be blank. |
| `ceiling` | `String` | required | Key of the most protective data category a row may hold. May be personal. |

There are no level or identifiability elements: the field takes the ceiling's levels.

## Rules enforced by the Classifier

| Rule | Violation | Severity | Field classified? |
|---|---|---|---|
| `declaredBy` must not be blank | `MISSING_REASON` (`@CategoryByRow needs declaredBy`) | error | **yes** (the error is still reported) |
| `ceiling` must be a key of the taxonomy | `UNKNOWN_CATEGORY` (`unknown data category '<key>'`) | error | no |
| A deprecated `ceiling` is replaced by its current replacement | `DEPRECATED_CATEGORY` | warning | yes, with the replacement |
| Not combined with `@Category` or `@Exempt` | `CONFLICTING_ANNOTATIONS` | error | no |

## Resulting FieldClassification

| Component | Value |
|---|---|
| `category` | the resolved ceiling |
| `basis` | `BY_ROW` |
| `reason` | `declaredBy` |
| `confidentiality` / `integrity` | the ceiling's levels |
| `identifiability` | `IDENTIFIED` if the ceiling is personal, otherwise `null` |

## Example

```java
import io.github.devbpm.linneo.annotation.CategoryByRow;
import static io.github.devbpm.linneo.Categories.*;

public class Document {

    @CategoryByRow(declaredBy = "the category of the document", ceiling = USER_HEALTH_AND_MEDICAL)
    private byte[] content;   // BY_ROW, SPECIAL_CATEGORY, HIGH / HIGH, IDENTIFIED
}
```

See also: [Annotations overview](README.md) · [`@Category`](category.md) · [Results](../classify/results.md) ·
[Violations](../classify/violations.md) · course: [Annotating models](../../course/03-annotating-models.md)
