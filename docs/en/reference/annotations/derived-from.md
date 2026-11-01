---
title: "@DerivedFrom"
parent: "Annotations"
grand_parent: "Reference"
nav_order: 5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/DerivedFrom.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @DerivedFrom

`io.github.devbpm.linneo.annotation.DerivedFrom` —
`@Documented @Retention(RUNTIME) @Target({FIELD, RECORD_COMPONENT})`, `@since 0.1.0`

Declares a field whose value is computed from other fields. A value derived from data is that data, or reveals it:
the age computed from a date of birth is personal data, and so is the full name built from a first and a last name.
Use this annotation for such values, never [`@Exempt`](exempt.md).

## Elements

| Element | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String[]` | required | The paths of the fields the value is computed from. At least one, or the field has an `UNKNOWN_SOURCE` error. |
| `type` | `Class<?>` | `void.class` | The class that declares the sources, when it is not the class being classified: a view, a data transfer object or a report row built from an entity. `void.class` means the class being classified. |

## Sources

A source is a field path:

- the name of a field of the class, **inherited fields included**, in any order of declaration (`"birthDate"`);
- the path of a field of a [`@Composite`](composite.md) the class holds (`"home.city"`);
- the path of a composite field itself (`"home"`), which stands for **all** the fields below it.

A derived field may be the source of another one, as long as no derivation depends on itself. A source must be a
*classified* field: a missing path, an [`@Exempt`](exempt.md) field and a field with an error are all
`UNKNOWN_SOURCE`.

With `type`, the paths name fields of that class, classified as that class is. Giving the class being classified as
`type` is the same as leaving it out. A view is thereby kept consistent with the entity it shows: without
`@Category` it follows the entity's classification, and with `@Category` it must keep up if the entity's field is
later given a stricter one.

Sources are resolved field by field, so two classes may derive from each other's *other* fields: `Order.customerName`
from `Customer.name` and `Customer.lastOrderTotal` from `Order.total` is not a cycle, and a
[`@Composite`](composite.md) may derive from a class that holds it. Only derived fields that depend on each other are a
cycle, whether in one class, across classes or through the parts of a composite. The result does not depend on the
order the classes are classified in.

## What the field gets

| | Without `@Category` | With `@Category` |
|---|---|---|
| Classification | that of its source: category, levels and identifiability | the declared one, with the usual checks of [`@Category`](category.md) |
| Several sources | they must all agree on category, confidentiality, integrity and identifiability, or the field must declare its category | the declared category must protect at least as much as **each** source |
| Protecting less than a source | not possible | an error, unless the value is declared anonymized or aggregated |

"At least as much" is [`Protection.covers`](../protection-levels.md#protection): each legal nature of the source is
covered, the levels are not lower, and a secret stays a secret. The one explicit way to protect less is to declare
the value anonymized or aggregated: a **personal** category with an `identifiability` of `ANONYMIZED` or `AGGREGATED`,
such as an average over many people, which a review can see. Declaring a non-personal category for a value derived
from personal data is an error.

## Rules enforced by the Classifier

| Rule | Violation | Severity | Field classified? |
|---|---|---|---|
| At least one source | `UNKNOWN_SOURCE` (`@DerivedFrom needs at least one source`) | error | no |
| Each source is a classified field | `UNKNOWN_SOURCE` (`the source '<s>' is not a classified field of <Class>` / `the source '<s>' has an error` / `the source '<Class>.<s>' has an error` for a field of another class) | error | no |
| Without `@Category`, several sources agree | `AMBIGUOUS_DERIVATION` (`its sources [<s>, ...] have different classifications; declare the category of the derived value with @Category`) | error | no |
| With `@Category`, it protects at least as much as every source, unless anonymized or aggregated | `WEAKER_THAN_SOURCE` (`'<key>' protects less than its source(s) [<s>, ...]; a value derived from data needs at least its protection, unless it is declared anonymized or aggregated`) | error | no |
| No derived field depends on itself, within a class or across classes | `CYCLE` (`is derived from itself through other derived fields`) | error | no |
| The usual checks of `@Category`, when it is present | as in [`@Category`](category.md) | | |
| Not combined with `@Exempt` or `@CategoryByRow` | `CONFLICTING_ANNOTATIONS` | error | no |

Every derived field in a cycle is reported. A field that derives from a field with an error, a field in a cycle
included, without being in the cycle itself, is an `UNKNOWN_SOURCE`. A source of another class (`type`) that has an
error in its own class is unknown here.

## Resulting FieldClassification

| Component | Value |
|---|---|
| `category` | the source's category, or the declared one |
| `basis` | `DERIVED` |
| `reason` | `null` |
| `confidentiality` / `integrity` | the source's levels, or the declared ones |
| `identifiability` | the source's, or the declared one |
| `derivedFrom` | the sources: field paths of the same class (a composite path is expanded to its fields) or `ClassName#path` for a field of another class, with the binary name of the class |

## Example

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.DerivedFrom;
import io.github.devbpm.linneo.Identifiability;
import java.time.LocalDate;
import static io.github.devbpm.linneo.Categories.*;

public class Student {

    @Category(USER_NAME_FIRST)
    private String firstName;

    @Category(USER_NAME_LAST)
    private String lastName;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    @Category(USER_DEMOGRAPHIC_DATE_OF_BIRTH)
    private LocalDate birthDate;

    @DerivedFrom("birthDate")                 // DERIVED: user.demographic.date_of_birth, IDENTIFIED
    private int age;

    @Category(USER_NAME)                      // DERIVED: declared, protects at least as much as both sources
    @DerivedFrom({"firstName", "lastName"})
    private String fullName;

    @Category(value = USER_DEMOGRAPHIC_DATE_OF_BIRTH, identifiability = Identifiability.AGGREGATED)
    @DerivedFrom("birthDate")                 // DERIVED: a summary of many people, not personal data
    private double averageAge;
}

public record StudentView(                    // a view of Student: its fields are classified as Student's
        @DerivedFrom(type = Student.class, value = "email") String email,
        @DerivedFrom(type = Student.class, value = "firstName") String firstName) {
}
```

For `StudentView.email`, `derivedFrom()` is `["<package>.Student#email"]`.

Wrong declarations:

```java
@Category(USER_NAME) @DerivedFrom("condition")   // WEAKER_THAN_SOURCE: condition is health data
private String summary;

@DerivedFrom({"firstName", "email"})             // AMBIGUOUS_DERIVATION: different categories, no @Category
private String contact;

@DerivedFrom("missing")                          // UNKNOWN_SOURCE
private String unknown;
```

See also: [Annotations overview](README.md) · [`@Category`](category.md) · [`@Composite`](composite.md) ·
[`@Exempt`](exempt.md) · [Results](../classify/results.md) · [Violations](../classify/violations.md) ·
[Protection levels](../protection-levels.md) · course: [Annotating models](../../course/03-annotating-models.md)
