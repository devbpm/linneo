---
title: "Violations"
parent: "Reference"
nav_order: 11
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/Violation.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# Violations

## Violation

`io.github.devbpm.linneo.classify.Violation` — `public record`, `@since 0.1.0`

```java
public record Violation(Class<?> type, String field, Severity severity, Code code, String message)
```

A problem found while classifying a class.

| Component | Nullable | Description |
|---|---|---|
| `type` | no | The class being classified (not necessarily the class that declares the field or annotation). |
| `field` | yes | The field involved; `null` for a problem with a class annotation (`@DefaultCategory`). |
| `severity` | no | Whether it fails verification. |
| `code` | no | What kind of problem it is. |
| `message` | no | A description naming the key or annotation involved. |

The canonical constructor throws `NullPointerException` if `type`, `severity`, `code` or `message` is `null`.

| Method | Description |
|---|---|
| `boolean isError()` | `true` for severity `ERROR`. |
| `String toString()` | `<SEVERITY> <CODE> <type's binary name>[.<field>]: <message>`, for example `ERROR UNCLASSIFIED_FIELD com.example.Person.undecided: no @Category on the field and no @DefaultCategory on Person`. |

## Severity

`Violation.Severity`

| Constant | Meaning |
|---|---|
| `ERROR` | The classification is wrong or missing; `Classifier.verify` fails and `ClassClassification.isValid()` is `false`. |
| `WARNING` | The classification works but should be updated; verification passes. |

## Codes

`Violation.Code` — every value, with the severity the [`Classifier`](classifier.md) gives it, when it is raised, the
message, and whether the field still appears in `ClassClassification.fields()`.

| Code | Severity | Raised when | Message | Field classified? |
|---|---|---|---|---|
| `UNCLASSIFIED_FIELD` | error | A field has no `@Category`, `@CategoryByRow` or `@Exempt`, and neither its declaring class nor the classified class has a `@DefaultCategory`. | `no @Category on the field and no @DefaultCategory on <Declaring>[ or <Type>]` | no |
| `UNKNOWN_CATEGORY` | error | The key of `@Category`, the `ceiling` of `@CategoryByRow` or the `value` of `@DefaultCategory` is not in the taxonomy. | `unknown data category '<key>'` | no (for a default: the fields it would cover are absent) |
| `DEPRECATED_CATEGORY` | **warning** | A key in any annotation is deprecated. | `data category '<key>' is deprecated; use '<replacement>'` (without the `; use` part when there is no replacement) | yes, with the current replacement |
| `PERSONAL_DEFAULT` | error | The (resolved) category of a `@DefaultCategory` is personal. `field` is `null`. | `@DefaultCategory of <Class> names '<key>', which is personal data: declare personal fields one by one with @Category` | no (fields it would cover are absent) |
| `MISSING_REASON` | error | `@Exempt` has a blank value, `@CategoryByRow` a blank `declaredBy`, or `@DefaultCategory` a blank `reason` (`field` is `null`). | `@Exempt needs a reason` / `@CategoryByRow needs declaredBy` / `@DefaultCategory of <Class> needs a reason` | exempt: never; by row and default: yes |
| `CONFLICTING_ANNOTATIONS` | error | `@Exempt` with `@Category` or `@CategoryByRow`, or `@Category` with `@CategoryByRow`. | `@Exempt cannot be combined with @Category or @CategoryByRow` / `@Category and @CategoryByRow cannot be used together` | no |
| `LOWERED_LEVEL` | error | `@Category` declares a confidentiality or integrity lower than the category's. One violation per facet. | `confidentiality <c> is lower than the <cc> of '<key>'` / `integrity <i> is lower than the <ci> of '<key>'` | no |
| `AMBIGUOUS_VALUE` | error | An optional element of `@Category` (`confidentiality`, `integrity`, or `identifiability` on a personal category) has more than one value. | `<element> must have at most one value` | no |
| `IDENTIFIABILITY_NOT_APPLICABLE` | error | `@Category` declares an `identifiability` for a category that is not personal. | `'<key>' is not personal data and has no identifiability` | no |

Violations about a `@DefaultCategory` are raised for the classified class and for each superclass that carries one,
and are reported in the `ClassClassification` of the class being classified (so a faulty superclass default appears
in the result of every subclass).

```java
ClassClassification result = new Classifier(Taxonomy.standard()).classify(WrongFields.class);
for (Violation v : result.violations()) {
    System.out.println(v.severity() + " " + v.code() + " " + v.field() + ": " + v.message());
}
List<Violation.Code> codes = result.errors().stream().map(Violation::code).toList();
```

See also: [Classifier](classifier.md) · [Results](results.md) · [Annotations](../annotations/README.md) ·
course: [Verifying in tests](../../course/04-verifying-in-tests.md)
