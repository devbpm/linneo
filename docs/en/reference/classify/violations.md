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
public record Violation(Class<?> type, String path, Severity severity, Code code, String message)
```

A problem found while classifying a class.

| Component | Nullable | Description |
|---|---|---|
| `type` | no | The class being classified (not necessarily the class that declares the field or annotation). |
| `path` | yes | The path of the field involved, which identifies it within `type`: its name, or `field.part` for a field of a [`@Composite`](../annotations/composite.md); `null` for a problem with a class annotation (`@DefaultCategory`). |
| `severity` | no | Whether it fails verification. |
| `code` | no | What kind of problem it is. |
| `message` | no | A description naming the key or annotation involved. |

The canonical constructor throws `NullPointerException` if `type`, `severity`, `code` or `message` is `null`.

| Method | Description |
|---|---|
| `boolean isError()` | `true` for severity `ERROR`. |
| `String toString()` | `<SEVERITY> <CODE> <type's binary name>[.<path>]: <message>`, for example `ERROR UNCLASSIFIED_FIELD com.example.Person.undecided: no @Category on the field and no @DefaultCategory on Person`. |

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
| `UNCLASSIFIED_FIELD` | error | A field has no `@Category`, `@CategoryByRow`, `@DerivedFrom` or `@Exempt`, is not of a [`@Composite`](../annotations/composite.md) type, and neither its declaring class nor the classified class has a `@DefaultCategory`. A `Map` of composites counts as an ordinary field. | `no @Category on the field and no @DefaultCategory on <Declaring>[ or <Type>]` | no |
| `UNKNOWN_CATEGORY` | error | The key of `@Category`, the `ceiling` of `@CategoryByRow` or the `value` of `@DefaultCategory` is not in the taxonomy. | `unknown data category '<key>'` | no (for a default: the fields it would cover are absent) |
| `DEPRECATED_CATEGORY` | **warning** | A key in any annotation is deprecated. | `data category '<key>' is deprecated; use '<replacement>'` (without the `; use` part when there is no replacement) | yes, with the current replacement |
| `PERSONAL_DEFAULT` | error | The (resolved) category of a `@DefaultCategory` is personal. `path` is `null`. | `@DefaultCategory of <Class> names '<key>', which is personal data: declare personal fields one by one with @Category` | no (fields it would cover are absent) |
| `MISSING_REASON` | error | `@Exempt` has a blank value, `@CategoryByRow` a blank `declaredBy`, or `@DefaultCategory` a blank `reason` (`path` is `null`). | `@Exempt needs a reason` / `@CategoryByRow needs declaredBy` / `@DefaultCategory of <Class> needs a reason` | exempt: never; by row and default: yes |
| `CONFLICTING_ANNOTATIONS` | error | `@Exempt` with `@Category`, `@CategoryByRow` or `@DerivedFrom`, or `@CategoryByRow` with `@Category` or `@DerivedFrom`. (`@Category` with `@DerivedFrom` is allowed.) | `@Exempt cannot be combined with @Category, @CategoryByRow or @DerivedFrom` / `@CategoryByRow cannot be combined with @Category or @DerivedFrom` | no |
| `LOWERED_LEVEL` | error | `@Category` declares a confidentiality or integrity lower than the category's. One violation per facet. | `confidentiality <c> is lower than the <cc> of '<key>'` / `integrity <i> is lower than the <ci> of '<key>'` | no |
| `AMBIGUOUS_VALUE` | error | An optional element of `@Category` (`confidentiality`, `integrity`, or `identifiability` on a personal category) has more than one value. | `<element> must have at most one value` | no |
| `IDENTIFIABILITY_NOT_APPLICABLE` | error | `@Category` declares an `identifiability` for a category that is not personal. | `'<key>' is not personal data and has no identifiability` | no |
| `DUPLICATE_FIELD` | error | A field has the name of a field of a superclass, so its path would not identify it. Raised for the field that hides, not for the superclass field. | `hides the field of the same name declared by <Superclass>; rename one of them` | no (the superclass field is) |
| `UNKNOWN_SOURCE` | error | A [`@DerivedFrom`](../annotations/derived-from.md) field names no source, or a source that is not a classified field of the class given by `type` (or of the classified class): it does not exist, is exempted or has an error, a field in a cycle included. | `@DerivedFrom needs at least one source` / `the source '<s>' is not a classified field of <Class>` / `the source '<s>' has an error` / `the source '<Class>.<s>' has an error` (a field of another class) | no |
| `AMBIGUOUS_DERIVATION` | error | A derived field without `@Category` has sources whose classifications differ in category, confidentiality, integrity or identifiability. | `its sources [<s>, ...] have different classifications; declare the category of the derived value with @Category` | no |
| `WEAKER_THAN_SOURCE` | error | A derived field declares a category that protects less than one of its sources, without declaring the value anonymized or aggregated. | `'<key>' protects less than its source(s) [<s>, ...]; a value derived from data needs at least its protection, unless it is declared anonymized or aggregated` | no |
| `WEAKER_THAN_PARTS` | error | A field that stores a [`@Composite`](../annotations/composite.md) as one value declares, with `@Category` or `@CategoryByRow`, a category that protects less than the fields of the composite. | `'<key>' protects less than the fields of <Composite> need (<natures>, <confidentiality>, <integrity>[, secret])` | **yes** (the error is still reported) |
| `CYCLE` | error | A composite contains itself, or derived fields depend on each other, within one class or across classes, also through the parts of a composite. | `the composite <Composite> contains itself; classify the field as one value with @Category or @CategoryByRow` / `is derived from itself through other derived fields` | no (for a composite, the field that closes the loop) |

In `AMBIGUOUS_DERIVATION` and `WEAKER_THAN_SOURCE` the sources `[<s>, ...]` are written as in
`FieldClassification.derivedFrom()`: paths of the same class (a composite expanded to its fields), or `ClassName#path`.
In `UNKNOWN_SOURCE`, `<s>` is the path as written in the annotation.

Classes that derive from each other's other fields, and a composite that derives from a class that holds it, are not a
`CYCLE`: only derived fields that depend on each other are, and each of them gets the second message. A field that
derives from one of them without being in the cycle is an `UNKNOWN_SOURCE`. A field of a composite in a cycle is
reported with the composite and by each class that holds it, at `field.part`.

The same fields have errors, with the same codes and messages, whatever the order the classes are classified in.

Violations about a `@DefaultCategory` are raised for the classified class and for each superclass that carries one,
and are reported in the `ClassClassification` of the class being classified (so a faulty superclass default appears
in the result of every subclass).

Violations found inside a [`@Composite`](../annotations/composite.md) are reported in the `ClassClassification` of the
class that holds it, with the holder as `type` and the path `field.part`; one without a path of its own (a faulty
`@DefaultCategory` of the composite) gets the path of the holding field. A composite with an error therefore appears
in the result of every class that holds it.

```java
ClassClassification result = new Classifier(Taxonomy.standard()).classify(WrongFields.class);
for (Violation v : result.violations()) {
    System.out.println(v.severity() + " " + v.code() + " " + v.path() + ": " + v.message());
}
List<Violation.Code> codes = result.errors().stream().map(Violation::code).toList();
```

See also: [Classifier](classifier.md) · [Results](results.md) · [Annotations](../annotations/README.md) ·
course: [Verifying in tests](../../course/04-verifying-in-tests.md)
