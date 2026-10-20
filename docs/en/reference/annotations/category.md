---
title: "@Category"
parent: "Annotations"
grand_parent: "Reference"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/Category.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @Category

`io.github.devbpm.linneo.annotation.Category` —
`@Documented @Retention(RUNTIME) @Target({FIELD, RECORD_COMPONENT})`, `@since 0.1.0`

Declares the data category a field holds. On a record component the annotation also applies to the component's
field.

## Elements

| Element | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String` | required | Key of the data category, preferably a constant of [`Categories`](../key-constants.md). |
| `identifiability` | `Identifiability[]` | `{}` | How directly the stored value points to the person; only for personal categories. Empty means `IDENTIFIED`. |
| `confidentiality` | `Confidentiality[]` | `{}` | A confidentiality stricter than the category's, for this field only. Empty means the category's level. |
| `integrity` | `Integrity[]` | `{}` | An integrity stricter than the category's, for this field only. Empty means the category's level. |

The optional elements are arrays so that "not set" can be told apart from a value: leave them empty or give exactly
one value.

## Rules enforced by the Classifier

| Rule | Violation | Severity | Field classified? |
|---|---|---|---|
| `value` must be a key of the taxonomy | `UNKNOWN_CATEGORY` (`unknown data category '<key>'`) | error | no |
| A deprecated `value` is replaced by its current replacement (`Taxonomy.resolveCategory`) | `DEPRECATED_CATEGORY` (`data category '<key>' is deprecated; use '<replacement>'`) | warning | yes, with the replacement |
| At most one value per optional element | `AMBIGUOUS_VALUE` (`<element> must have at most one value`) | error | no |
| `confidentiality` not lower than the category's | `LOWERED_LEVEL` (`confidentiality <c> is lower than the <cc> of '<key>'`) | error | no |
| `integrity` not lower than the category's | `LOWERED_LEVEL` (`integrity <i> is lower than the <ci> of '<key>'`) | error | no |
| `identifiability` only on personal categories | `IDENTIFIABILITY_NOT_APPLICABLE` (`'<key>' is not personal data and has no identifiability`) | error | no |
| Not combined with `@CategoryByRow` or `@Exempt` | `CONFLICTING_ANNOTATIONS` | error | no |

Notes:

- The level checks compare with the levels of the resolved (current) category.
- With several values, the first one is still compared, so `integrity = {LOW, HIGH}` on a `MEDIUM` category reports
  both `AMBIGUOUS_VALUE` and `LOWERED_LEVEL`.
- The `identifiability` array is only checked for ambiguity on personal categories; on a non-personal category any
  value produces `IDENTIFIABILITY_NOT_APPLICABLE`.

## Resulting FieldClassification

| Component | Value |
|---|---|
| `category` | the resolved category |
| `basis` | `DECLARED` |
| `reason` | `null` |
| `confidentiality` / `integrity` | the declared value, or the category's |
| `identifiability` | the declared value, or `IDENTIFIED`, for a personal category; `null` for a non-personal one |

## Examples

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Identifiability;
import static io.github.devbpm.linneo.Categories.*;

public class Person {

    @Category(USER_CONTACT_EMAIL)
    private String email;                 // user.contact.email, IDENTIFIED, LOW / MEDIUM

    @Category(value = USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
    private String analyticsId;           // still personal data

    @Category(value = USER_CONTENT_PRIVATE, confidentiality = Confidentiality.HIGH)
    private String notes;                 // raised from MEDIUM to HIGH

    @Category(value = USER_CONTACT, identifiability = Identifiability.AGGREGATED)
    private Integer contactsCount;        // not personal data: isPersonal() is false
}

public record Contact(@Category(USER_CONTACT_EMAIL) String email) {
}
```

Wrong declarations:

```java
@Category("user.unknown")                                                   // UNKNOWN_CATEGORY
private String a;

@Category(value = USER_HEALTH_AND_MEDICAL, confidentiality = Confidentiality.LOW)   // LOWERED_LEVEL
private String b;

@Category(value = SYSTEM_CONFIGURATION, identifiability = Identifiability.IDENTIFIED) // IDENTIFIABILITY_NOT_APPLICABLE
private String c;

@Category(value = USER_NAME, integrity = {Integrity.LOW, Integrity.HIGH})            // AMBIGUOUS_VALUE (+ LOWERED_LEVEL)
private String d;
```

See also: [Annotations overview](README.md) · [Protection levels](../protection-levels.md) ·
[Key constants](../key-constants.md) · [Violations](../classify/violations.md) ·
course: [Annotating models](../../course/03-annotating-models.md)
