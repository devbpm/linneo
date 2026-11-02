---
title: "@Composite"
parent: "Annotations"
grand_parent: "Reference"
nav_order: 6
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/Composite.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @Composite

`io.github.devbpm.linneo.annotation.Composite` — `@Documented @Inherited @Retention(RUNTIME) @Target(TYPE)`,
`@since 0.1.0`

Marks a class whose instances are part of another record, such as a JPA `@Embeddable` address or a value object, so
that the fields that hold it are classified by its own fields. It is a marker with no elements. It is **inherited**: a
subclass of a composite is a composite.

Mark concrete classes and records. The classifier cannot see the fields of the implementations of an interface or an
abstract class, so a field of such a type takes no class default, and the fields of a subclass of a concrete composite
are invisible through a field of the superclass type.

The marker is Linneo's own rather than a framework annotation, so that the library needs no dependency. Forgetting it
is safe where it matters: a field that holds a class whose fields are classified, without the class being a composite,
is a [reference to another record](#references-to-other-records), which takes no non-personal class default when that
record holds personal data or its fields have errors, so it fails verification (`UNCLASSIFIED_FIELD`) until the class
is marked or the field declares its own category.

## What the classifier does

A field with **no annotation of its own** whose type is a concrete composite class, or an array, a collection or an
`Optional` of one, at any depth, gets one classification per field of the composite, with the path `field.part`:

| Field | Classified as |
|---|---|
| `Address home` | `home.street`, `home.city` |
| `List<Address> previous`, `Address[] previous`, `Optional<Address> previous`, `List<List<Address>> history` | `previous.street`, `previous.city`: containers nested at any depth are descended into |
| a field that may hold data the classifier cannot split into fields, such as `Map<String, Address> byType` (see below) | `UNCLASSIFIED_FIELD`, unless it declares its own category |
| a field with `@Category`, `@CategoryByRow` or `@DerivedFrom` | one entry: the composite is stored as one value (see below) |
| a field with `@Exempt` | skipped, and listed in `exempted()`, unless it holds composites (`CONFLICTING_ANNOTATIONS`) |

- **Type variables** are resolved through the superclasses of the class being classified: a `T value` of a generic
  base class classified for `Box extends Base<Address>` is an `Address`.
- **A class default never applies to a field that may hold data the classifier cannot see**, so wrapping a composite
  never gives it a non-personal default. These fields are an `UNCLASSIFIED_FIELD` error unless they are annotated:
  - composites in a `Map` or a `Map.Entry`, which is **never descended into** because its keys are data too:
    `Map<String, Address>` declares its category with `@Category` or `@CategoryByRow`, and is then checked against the
    composites' fields (see below);
  - a composite as the type argument of a class that is not a collection, such as `Wrapper<Address>`, or of an owner
    type (`Outer<Address>.Inner`), and several composites in one type, such as `Map<Address, Address>`. A generic
    composite such as `Box<Address>` mentions two composites, itself and `Address`, so it is declared as one value;
  - an interface or an abstract class marked `@Composite`, which is not embedded: its implementations may have other
    fields;
  - a value typed `Object`, `Serializable`, `Comparable` or `Cloneable`, a type variable the class does not bind, or a
    raw container.

  A field that refers to another record of the model is a separate case; see
  [References to other records](#references-to-other-records).

- **Paths** nest: a composite inside a composite gives `home.geo.country`. For such a field,
  `FieldClassification.fieldName()` is the last segment and `declaringClass()` is the composite class.
- **Defaults.** The fields of the composite are resolved by the composite's own annotations and its own
  [`@DefaultCategory`](default-category.md). The default of the class that holds it does **not** apply to them (it
  still applies to the holder's own fields).
- **Problems** inside the composite are reported in the classification of the class that holds it, at their path
  (`broken.unclassified`), with the holder as `Violation.type()`. A problem with a class annotation of the composite
  (a faulty `@DefaultCategory`), which has no path of its own, is reported at the path of the holding field.
  `@Exempt` fields of the composite are listed in `exempted()` with the path `field.part`.
- **Protection.** `ClassClassification.ceilingOf("home")` is the protection that covers everything the composite
  holds; so does `ceiling()` for the whole class.
- **Sources of derivations.** A [`@DerivedFrom`](derived-from.md) can name a part (`"home.city"`) or the whole
  composite (`"home"`). A field of the composite may itself derive from a class that holds the composite: that is a
  `CYCLE` only if derived fields depend on each other, through the parts of the composite too. A field of the
  composite in a cycle is reported with the composite and, at `field.part`, with the holder.
- The composite classes need not be passed to `Classifier.verify` separately: they are classified as part of every
  class that holds them. Classifying one directly works too, as for any class.
- **Order independence.** The problems found do not depend on the order in which classes are classified; only the field
  a composite containment cycle is reported on may. Each class reports its own problems, so verify every class of the
  model (see [`ModelClasses`](../classify/classifier.md#modelclasses)), not some of them.

## A composite stored as one value

A field that stores the composite as one value, for example as JSON in a single column, or that holds it where its
fields cannot be paths (in a `Map`, whose keys are data too, or as a type argument of a class that is not a
collection), declares its category with [`@Category`](category.md) or [`@CategoryByRow`](category-by-row.md). The
field is then one entry, and its protection must cover the protection of **every field of the composites it holds**:
otherwise the value would be classified below what it contains. The same holds for a field that stores composites as
one value and takes its category from a [`@DerivedFrom`](derived-from.md) source. A composite whose own fields have
errors leaves nothing to compare with, which is an error too. The check also covers a composite that holds itself and
composites that hold each other, whatever the order in which the classes are classified.

| Rule | Violation | Severity | Field classified? |
|---|---|---|---|
| The declared category protects at least as much as the fields of every composite the field holds | `WEAKER_THAN_PARTS` (`'<key>' protects less than the fields of [<Composite>, ...] need (<natures>, <confidentiality>, <integrity>[, secret])`; or, when the fields of a composite have errors, `the fields of [<Composite>, ...] have errors, so what '<key>' must protect is unknown`) | error | **yes** (the error is still reported) |
| The composite does not contain itself, directly or through other composites | `CYCLE` (`the composite <Composite> contains itself; classify the field as one value with @Category or @CategoryByRow`) | error | no (the field that closes the loop) |

A composite that contains itself (`Node` with a `List<Node> children`) cannot be expanded: the error is reported at
the field that closes the loop (`tree.children`), and the other fields of the composite are classified normally.
Classify that field as one value to remove the error.

## References to other records

A field with no annotation of its own whose type is another record of the model, a class that has a
[`@DefaultCategory`](default-category.md), an annotated field or a field of a composite type, **refers** to that
record. A sealed type is read through its permitted subclasses, and type arguments are read too (`List<Country>`).

- If the record holds **no personal data**, as a catalogue entry such as a `Country` does, the field takes the class
  default like any other.
- A reference to a record that **holds personal data** is personal data itself, a pseudonymous key, so the class
  default is refused: the field is an `UNCLASSIFIED_FIELD` error that advises declaring what it stores, typically the
  record's key, with `@DerivedFrom(type = Customer.class, value = "id")`.
- If the **fields of the record have errors**, whether it holds personal data is unknown, so the class default is
  refused too: the field is an `UNCLASSIFIED_FIELD` error that advises fixing them, or declaring what the field stores.
- [`@Exempt`](exempt.md) on a field that holds composites, or that refers to a record with personal data or with
  errors, is a `CONFLICTING_ANNOTATIONS` error: a relationship is not a field that holds no data.

```java
@DefaultCategory(value = REFERENCE_PUBLIC, reason = "code list of countries")
public class Country {
    private String code;
}

@DefaultCategory(value = SYSTEM_OPERATIONS, reason = "technical columns of the invoice")
public class Invoice {

    @DerivedFrom(type = Customer.class, value = "id")   // what the reference stores: the key of a Customer
    private Customer customer;

    private Country country;                            // a catalogue entry holds no personal data: class default

    private Long number;                                // class default
}
```

## Example

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.Composite;
import java.util.List;
import static io.github.devbpm.linneo.Categories.*;

@Composite
public class Address {                                  // with JPA, next to @Embeddable
    @Category(USER_CONTACT_ADDRESS_STREET)
    private String street;

    @Category(USER_CONTACT_ADDRESS_CITY)
    private String city;
}

public class Applicant {

    @Category(USER_NAME)
    private String name;

    private Address home;                               // home.street, home.city

    private List<Address> previous;                     // previous.street, previous.city

    @Category(USER_CONTACT_ADDRESS)                     // one value: must cover street and city
    private Address shipping;
}
```

Classifying `Applicant` gives:

```text
name = user.name (DECLARED)
home.street = user.contact.address.street (DECLARED)
home.city = user.contact.address.city (DECLARED)
previous.street = user.contact.address.street (DECLARED)
previous.city = user.contact.address.city (DECLARED)
shipping = user.contact.address (DECLARED)
```

A weaker declaration for the stored value is an error, and the field is still classified:

```java
@Composite
public class Medical {
    @Category(USER_HEALTH_AND_MEDICAL)
    private String condition;
}

@Category(USER_CONTACT)                                 // WEAKER_THAN_PARTS: condition is health data
private Medical record;
```

See also: [Annotations overview](README.md) · [`@Category`](category.md) · [`@DefaultCategory`](default-category.md) ·
[`@DerivedFrom`](derived-from.md) · [Classifier](../classify/classifier.md) · [Results](../classify/results.md) ·
[Violations](../classify/violations.md) · course: [Annotating models](../../course/03-annotating-models.md)
