---
title: "3. Annotating models"
parent: "Course"
nav_order: 3
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/Category.java
  - src/main/java/io/github/devbpm/linneo/annotation/DefaultCategory.java
  - src/main/java/io/github/devbpm/linneo/annotation/CategoryByRow.java
  - src/main/java/io/github/devbpm/linneo/annotation/DerivedFrom.java
  - src/main/java/io/github/devbpm/linneo/annotation/Composite.java
  - src/main/java/io/github/devbpm/linneo/annotation/Exempt.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-category-policy.tsv
-->

# Module 3 — Annotating models

## Learning objectives

After this module you will be able to:

- declare the category of a field with `@Category`, and raise its confidentiality or integrity for that field;
- declare the identifiability of a stored value;
- state once, with a reason, that a class holds no personal data, using `@DefaultCategory`;
- classify a field whose category changes from row to row with `@CategoryByRow`;
- classify a value computed from other fields with `@DerivedFrom`, and an embedded value object with `@Composite`;
- exclude technical fields with `@Exempt`, and know which fields are excluded automatically;
- annotate records and class hierarchies correctly;
- choose the right key for a field and justify it.

## The six annotations at a glance

| Annotation | Where | Says |
|---|---|---|
| `@Category(key)` | field, record component | "this field holds data of this category" |
| `@DefaultCategory(value = key, reason = "...")` | class | "every field of this class without its own annotation holds this **non-personal** data, because…" |
| `@CategoryByRow(declaredBy = "...", ceiling = key)` | field, record component | "the category depends on each row; this is what decides it, and this is the most protective it can be" |
| `@DerivedFrom("source")` | field, record component | "this value is computed from other fields, so it is that data: it takes their classification" |
| `@Composite` | class | "instances of this class are part of other records: the fields that hold it are classified by its own fields" |
| `@Exempt("reason")` | field, record component | "this is a technical field (cache, handle, framework state) that holds no data of the model, because…" |

All six are read by reflection at run time (`RetentionPolicy.RUNTIME`). The classifier (module 4) reads them; it
never reads field values.

## `@Category`: one field, one category

Let us start the university model with the `Student` entity:

```java
package edu.example.university;

import io.github.devbpm.linneo.annotation.Category;
import java.time.LocalDate;

import static io.github.devbpm.linneo.Categories.*;

public class Student {

    @Category(USER_UNIQUE_ID)
    private Long id;

    @Category(USER_GOVERNMENT_ID_NATIONAL_IDENTIFICATION_NUMBER)
    private String nationalId;

    @Category(USER_NAME_FIRST)
    private String firstName;

    @Category(USER_NAME_LAST)
    private String lastName;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    @Category(USER_DEMOGRAPHIC_DATE_OF_BIRTH)
    private LocalDate dateOfBirth;

    @Category(USER_HEALTH_AND_MEDICAL_DISABILITY)
    private Integer disabilityDegree;

    @Category(USER_EDUCATION_STATUS)
    private String academicStatus;
}
```

Each field gets the protection of its category: `disabilityDegree` is automatically special category data with
`HIGH` confidentiality and integrity, `email` is ordinary personal data with `LOW` confidentiality (module 2).

### Optional elements, and why `value =` appears

`@Category` has one mandatory element, `value` (the key), and three optional ones:

| Element | Type | Default | Meaning |
|---|---|---|---|
| `identifiability` | `Identifiability[]` | `{}` → `IDENTIFIED` | how directly the stored value points to the person; **only for personal categories** |
| `confidentiality` | `Confidentiality[]` | `{}` → the category's level | a stricter confidentiality for this field only |
| `integrity` | `Integrity[]` | `{}` → the category's level | a stricter integrity for this field only |

Two Java details matter here:

1. **`value =` is required as soon as you add another element.** Java only allows the shorthand
   `@Category(USER_NAME)` when `value` is the *only* element given. With two elements you must name both:
   `@Category(value = USER_NAME, confidentiality = Confidentiality.HIGH)`.
2. **The optional elements are arrays** so that "not set" (`{}`) can be told apart from a value. Give at most one
   value: `confidentiality = Confidentiality.HIGH`. Two values (`integrity = {Integrity.LOW, Integrity.HIGH}`) are
   reported as an `AMBIGUOUS_VALUE` error.

### Raising a level for one field

A category's levels are a floor. A specific field may need more. In the university, counsellors keep free-text
notes about students. Free text written by or about the person is `user.content.private` (confidentiality
`MEDIUM`), but these notes often contain very sensitive context, so the university raises them to `HIGH`:

```java
@Category(value = USER_CONTENT_PRIVATE, confidentiality = Confidentiality.HIGH)
private String counsellingNotes;
```

Levels can only go **up**. Declaring a level lower than the category's is a `LOWERED_LEVEL` error:

```java
@Category(value = USER_HEALTH_AND_MEDICAL_DISABILITY, confidentiality = Confidentiality.MEDIUM)  // error
private Integer disabilityDegree;
```

Declaring the *same* level as the category is allowed but pointless.

### Identifiability

By default a personal field is `IDENTIFIED`. When the stored value is pseudonymised, anonymised or aggregated, say
so:

```java
@Category(value = USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
private String analyticsId;
```

A pseudonymised field is still personal data; an `ANONYMIZED` or `AGGREGATED` one is not (module 1). That is how
statistics tables keep their real category without counting as personal data:

```java
public record CourseStatistics(
        @Category(REFERENCE_PUBLIC) String courseCode,
        @Category(value = USER_EDUCATION_GRADE, identifiability = Identifiability.AGGREGATED) BigDecimal averageMark,
        @Category(value = USER_EDUCATION_GRADE, identifiability = Identifiability.AGGREGATED) int passed) {
}
```

Identifiability makes no sense for non-personal data: `identifiability` on a `system.*`, `org.*` or `reference.*`
key is an `IDENTIFIABILITY_NOT_APPLICABLE` error.

## `@DefaultCategory`: a class with no personal data

Many classes hold no personal data at all: configuration, code lists, technical tables. Annotating each field would
be noise. `@DefaultCategory` makes a single **positive statement**, with a reason a reviewer can check:

```java
@DefaultCategory(value = SYSTEM_CONFIGURATION, reason = "menu configuration of the student portal")
public class MenuGroup {

    private Long id;
    private String code;
    private String label;
    private int position;

    @Category(SYSTEM_SECURITY)          // a field may still declare its own category
    private String requiredRole;
}
```

Rules:

- **The category must not be personal.** `@DefaultCategory(USER_CONTACT, ...)` is a `PERSONAL_DEFAULT` error:
  personal data is always declared field by field, so that adding a personal column to a class can never be covered
  silently by a default.
- **The reason must not be blank** (`MISSING_REASON`).
- **A field's own annotation wins** over the default (`requiredRole` above is `system.security`).
- **It is not inherited by subclasses.** It covers the fields the annotated class declares, and — when that class is
  the one being classified — unannotated fields inherited from superclasses that have no default of their own.

## Inheritance

JPA models often have a common superclass for audit columns. In the university:

```java
@DefaultCategory(value = SYSTEM_OPERATIONS, reason = "technical audit timestamps and optimistic-lock version")
public abstract class AuditedEntity {

    @Category(USER_ACCOUNT_USERNAME)    // who created the row: a person's username
    private String createdBy;

    private Instant createdAt;
    private Instant updatedAt;
    private long version;
}

public class Student extends AuditedEntity {
    // ... fields as above
}
```

When `Student` is classified, the classifier walks the hierarchy (superclass fields first) and, for each field
without its own annotation, looks for a default in this order:

1. the `@DefaultCategory` of the class that **declares** the field (`AuditedEntity` for `createdAt`);
2. the `@DefaultCategory` of the class **being classified** (`Student`);
3. none: an `UNCLASSIFIED_FIELD` error.

So `createdAt`, `updatedAt` and `version` are `system.operations` in every subclass, `createdBy` keeps its own
`@Category`, and `Student`'s own fields are **not** covered by `AuditedEntity`'s default: each needs an annotation
(or a default on `Student`, which would only be possible if `Student` held no personal data).

A subclass must not reuse the name of a superclass field: the classifier identifies a field by its name, so the
field that hides the other is reported as a `DUPLICATE_FIELD` error. Rename one of the two.

The complete precedence the classifier applies to each field (`@DerivedFrom` and `@Composite` are introduced below):

1. `@Exempt` → the field is skipped.
2. `@Category`, or the ceiling of `@CategoryByRow`.
3. `@DerivedFrom` without a category → the classification of its source.
4. a field of a `@Composite` type → the fields of the composite.
5. the `@DefaultCategory` of the declaring class.
6. the `@DefaultCategory` of the class being classified.
7. otherwise `UNCLASSIFIED_FIELD`.

## `@CategoryByRow`: when each row is different

The university lets students upload documents: an enrollment certificate in one row, a medical report justifying
an absence in the next, a scholarship form in a third. The `content` column cannot have a single honest category.
`@CategoryByRow` says what decides the category of each row and the **ceiling**: the most protective category any
row may hold.

```java
public class Attachment extends AuditedEntity {

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)
    private Long id;

    @Category(USER_CONTENT_PRIVATE)
    private String fileName;

    @CategoryByRow(declaredBy = "the documentType of the owning Document", ceiling = USER_HEALTH_AND_MEDICAL)
    private byte[] content;

    @Category(SYSTEM_INTEGRITY)         // SHA-256 of the content, to detect tampering
    private String sha256;
}
```

Wherever a single category is needed for the whole field — a data inventory, an OpenMetadata tag — the ceiling is
used. The field therefore gets the ceiling's protection (`SPECIAL_CATEGORY`, `HIGH`, `HIGH`), and identifiability
`IDENTIFIED` when the ceiling is personal. `@CategoryByRow` has no `confidentiality`, `integrity` or
`identifiability` elements: if you need more protection, choose a stricter ceiling.

`declaredBy` must not be blank (`MISSING_REASON`). `@CategoryByRow` and `@Category` on the same field is a
`CONFLICTING_ANNOTATIONS` error.

The classifier never reads values, so it cannot check the rows. When your code writes one, it can check that the
category it chose stays below the ceiling with `FieldClassification.covers` (module 5).

## `@DerivedFrom`: values computed from other fields

Some fields are not captured but computed: an age from a date of birth, a full name from a first and a last name.
A value computed from data is that data, or reveals it: the age of a student is personal data. It must not be
exempted. `@DerivedFrom` names the fields it is computed from, and the classifier keeps the two consistent:

```java
public class StudentCard {                    // a card printed for the student; not part of the model so far

    @Category(USER_NAME_FIRST)
    private String firstName;

    @Category(USER_NAME_LAST)
    private String lastName;

    @Category(USER_DEMOGRAPHIC_DATE_OF_BIRTH)
    private LocalDate dateOfBirth;

    @DerivedFrom("dateOfBirth")               // takes the classification of its source
    private int age;

    @Category(USER_NAME)                      // declares its own, which must protect at least as much
    @DerivedFrom({"firstName", "lastName"})
    private String fullName;
}
```

- **Without `@Category`** the field takes the classification of its source: `age` is `user.demographic.date_of_birth`,
  identified, personal data. Its `basis()` is `DERIVED` and `derivedFrom()` lists `["dateOfBirth"]` (module 5).
- **Several sources must agree.** `firstName` and `lastName` have different categories, so `fullName` has to declare
  its own with `@Category`, here `user.name`. Without it the field is an `AMBIGUOUS_DERIVATION` error.
- **With `@Category`, the field must protect at least as much as every source.** `@Category(SYSTEM_OPERATIONS)` on
  `age` is a `WEAKER_THAN_SOURCE` error: computing a value from personal data does not make it less personal.
- **The one explicit way to protect less** is to declare the value anonymized or aggregated, which a reviewer can see.
  A statistics row can derive its average from the field of another class with `type`:

  ```java
  public record CourseSummary(
          @Category(REFERENCE_PUBLIC) String courseCode,
          @Category(value = USER_EDUCATION_GRADE, identifiability = Identifiability.AGGREGATED)
          @DerivedFrom(type = Grade.class, value = "mark") BigDecimal averageMark) {
  }
  ```

  Here `averageMark` is not personal data (module 1), and if `Grade.mark` is ever given another category the
  summary is checked against it. `Grade` is the class of exercise 3.1.
- **A source must be a classified field** of the class (or of `type`): a path that does not exist, an `@Exempt` field
  or a field with an error is `UNKNOWN_SOURCE`. It may be an inherited field, a part of a composite (`"home.city"`) or
  another derived field, as long as no derivation depends on itself (`CYCLE`).
- `@Exempt` and `@CategoryByRow` cannot be combined with `@DerivedFrom` (`CONFLICTING_ANNOTATIONS`).

## `@Composite`: classes that are part of other records

JPA models embed small value objects, such as an address, in several entities (`@Embeddable`). The classifier reads
the fields of the class it is given; for an `Address` field to be classified by what the address holds, mark the
value class `@Composite`:

```java
@Composite                                         // with JPA, next to @Embeddable
public class Address {

    @Category(USER_CONTACT_ADDRESS_STREET)
    private String street;

    @Category(USER_CONTACT_ADDRESS_CITY)
    private String city;

    @Category(USER_CONTACT_ADDRESS_POSTAL_CODE)
    private String postalCode;
}

public class ScholarshipApplication {              // not part of the model so far

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)
    private Long id;

    private Address home;                          // home.street, home.city, home.postalCode

    private List<Address> previous;                // previous.street, previous.city, previous.postalCode

    @Category(USER_CONTACT_ADDRESS)                // stored as one value, for example as JSON
    private Address billing;
}
```

An unannotated field whose type is a composite (or an array, a collection or an `Optional` of one) is classified by
the composite's fields, with the path `field.part`: `home.street`, `previous.city`. A `Map` is never entered.

- The composite is resolved with **its own** annotations and `@DefaultCategory`, never with the holder's: a default
  on `ScholarshipApplication` does not reach `Address`.
- **A composite stored as one value** (a JSON column, say) declares its category on the field, as `billing` does.
  That category must protect at least as much as the fields of the composite: `USER_CONTACT_ADDRESS` is enough for
  the address, but `@Category(SYSTEM_OPERATIONS)` would be a `WEAKER_THAN_PARTS` error.
- A composite that contains itself (a `Node` with a list of `Node`) cannot be expanded: the field that closes the
  loop is a `CYCLE` error; classify that field as one value.
- **Forgetting `@Composite` is safe.** Without it, `home` is an ordinary field with no category, and the classifier
  reports `UNCLASSIFIED_FIELD`. You do not need to pass the embeddable classes to `verify` separately.

## `@Exempt`: technical fields

Some fields hold no data of the model: a cache, a technical handle, state that a framework keeps, or a collection of
other records that are classified on their own, such as the children of a one-to-many association. `@Exempt`
excludes them, with a reason:

```java
@Exempt("cache of the rendered menu, rebuilt on demand")
private String renderedMenu;
```

A value computed from other fields is **not** such a field: it is data, and uses `@DerivedFrom` or `@Category`. If you
find yourself writing "computed from..." as a reason, use `@DerivedFrom`.

```java
public class Document extends AuditedEntity {

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)
    private Long id;

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)
    private Long studentId;

    @Category(USER_CONTENT_PRIVATE)
    private String title;

    @Category(USER_CONTENT_PRIVATE)
    private String documentType;

    @Exempt("relationship: each Attachment is classified on its own")
    private List<Attachment> attachments;
}
```

Excluded automatically, without any annotation: **`static`** fields, **synthetic** fields (generated by the
compiler) and fields with the Java **`transient`** modifier.

> **JPA note.** `@jakarta.persistence.Transient` is *not* the Java `transient` modifier. A field annotated only with
> JPA's `@Transient` is still seen by the classifier and needs `@Exempt` (or a category).

`@Exempt` cannot be combined with `@Category`, `@CategoryByRow` or `@DerivedFrom` (`CONFLICTING_ANNOTATIONS`), and its
reason must not be blank (`MISSING_REASON`). The classification keeps the exempt fields and their reasons
(`ClassClassification.exempted()`, module 5), so a reviewer can see what was left out.

## Records

All field annotations also target record components. On a record component the annotation also applies to the
generated field, which is what the classifier reads. See `CourseStatistics` above: nothing else is needed.

## Choosing the right key

Some guidelines that work well in practice:

- **Classify what the value is, not where it is.** A staff member's email in the `Staff` table is
  `USER_CONTACT_EMAIL`, just like a student's.
- **Use the most specific key that is true for every row.** `USER_NAME_FIRST` for a first name; `USER_NAME` for a
  full name.
- **Identifiers.** The primary key of a table *about a person* identifies the person: `USER_UNIQUE_ID`. A foreign
  key or an id of a record about a person (an enrollment, a document) is `USER_UNIQUE_ID_RECORD_REFERENCE`. Ids of
  non-personal tables are covered by the class default.
- **Free text.** Comments and notes written by or about a person are `USER_CONTENT_PRIVATE`; raise confidentiality
  if they routinely hold sensitive context.
- **Secrets.** A person's password or password hash is `USER_AUTHORIZATION_PASSWORD`; a system's API key or signing
  key is `SYSTEM_AUTHENTICATION_SECRET`. Both are secrets.
- **When in doubt between two keys, take the more protective one** — or use `@CategoryByRow` if it really changes
  per row.
- **When nothing fits,** do not force a wrong key: add one with a `TaxonomyExtension` (module 6).

## The model so far

Besides `Student`, `MenuGroup`, `AuditedEntity`, `Document`, `Attachment` and `CourseStatistics` shown above, the
university model has `Enrollment`, `Grade` and `Staff`. You will annotate them in the exercises. The complete
`Student`:

```java
public class Student extends AuditedEntity {

    @Category(USER_UNIQUE_ID)
    private Long id;

    @Category(USER_GOVERNMENT_ID_NATIONAL_IDENTIFICATION_NUMBER)
    private String nationalId;

    @Category(USER_NAME_FIRST)
    private String firstName;

    @Category(USER_NAME_LAST)
    private String lastName;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    @Category(USER_DEMOGRAPHIC_DATE_OF_BIRTH)
    private LocalDate dateOfBirth;

    @Category(USER_HEALTH_AND_MEDICAL_DISABILITY)
    private Integer disabilityDegree;

    @Category(value = USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
    private String analyticsId;

    @Category(USER_EDUCATION_STATUS)
    private String academicStatus;

    @Category(value = USER_CONTENT_PRIVATE, confidentiality = Confidentiality.HIGH)
    private String counsellingNotes;
}
```

## Common mistakes

- **Forgetting `value =`.** `@Category(USER_NAME, confidentiality = Confidentiality.HIGH)` does not compile.
- **Putting a personal key in `@DefaultCategory`** to "save time". It is rejected (`PERSONAL_DEFAULT`) on purpose.
- **Expecting `@DefaultCategory` on a superclass to cover subclass fields.** It covers only the fields the annotated
  class declares.
- **Relying on JPA's `@Transient`.** Only the Java `transient` modifier excludes a field automatically.
- **Lowering a level "because our data is not that sensitive".** Levels only go up on a field; if you think the
  standard level is wrong for your organisation everywhere, raise it for everyone with an extension (module 6) — it
  cannot be lowered.
- **Using `identifiability` on non-personal keys** (`IDENTIFIABILITY_NOT_APPLICABLE`).
- **Exempting fields that do hold data** to make the build pass. `@Exempt` is for technical fields that hold *no* data
  of the model; the reason is reviewed.
- **Exempting a computed value** ("computed from firstName and lastName"). It is data: use `@DerivedFrom`, or
  `@Category` if you know its category.
- **Leaving an embedded class without `@Composite`.** A JPA `@Embedded` address is then one field with no category
  (`UNCLASSIFIED_FIELD`). Mark the embeddable class `@Composite` and the fields that hold it are classified by its
  fields; do not `@Exempt` the embedded field.

## Exercises

**Exercise 3.1 — Enrollment and Grade.** Annotate these two classes. Both extend `AuditedEntity`.

```java
public class Enrollment extends AuditedEntity {
    private Long id;
    private Long studentId;
    private String programCode;      // e.g. "GII" for the Computer Engineering degree
    private String academicYear;     // e.g. "2026/27"
    private BigDecimal tuitionFee;
    private String scholarshipType;
}

public class Grade extends AuditedEntity {
    private Long enrollmentId;
    private String courseCode;
    private BigDecimal mark;
}
```

**Exercise 3.2 — Staff.** Annotate `Staff`. The university needs a confirmation that staff working with minors have
no sexual offence convictions, and staff log in with a local password.

```java
public class Staff extends AuditedEntity {
    private Long id;
    private String fullName;
    private String workEmail;
    private String jobTitle;
    private String department;
    private String socialSecurityNumber;
    private String iban;
    private Boolean sexOffenceCertificateClear;
    private String passwordHash;
}
```

Which fields are special category or criminal offence data? Which one is a secret?

**Exercise 3.3 — Spot the bugs.** Each field below has a problem. Name it and fix it.

<!-- unchecked: an exercise with deliberate mistakes -->
```java
@DefaultCategory(value = USER_CONTACT, reason = "")
public class ContactPreferences {

    @Category(USER_CONTACT_EMAIL, confidentiality = Confidentiality.HIGH)
    private String email;

    @Category(value = SYSTEM_CONFIGURATION, identifiability = Identifiability.PSEUDONYMIZED)
    private String theme;

    @Category(value = USER_NAME, integrity = {Integrity.MEDIUM, Integrity.HIGH})
    private String name;

    @jakarta.persistence.Transient
    private String renderCache;
}
```

**Exercise 3.4 — Per row or not?** For each field, decide between `@Category`, `@CategoryByRow`, `@DerivedFrom` and
`@Exempt`, and give the key, the source or the reason: (a) the body of a message students send to the secretariat;
(b) a file field in a generic `Form` entity that stores either a scholarship form or a disability certificate; (c) a
`Map` used as an in-memory cache, with the Java `transient` modifier; (d) the age of a student, kept in an `age`
field and computed from `dateOfBirth`.

**Exercise 3.5 — Computed and embedded.** `Address` is the `@Composite` class of this module. Annotate `Contact`, and
say which paths the classifier reports for `office`:

```java
public class Contact {
    @Category(USER_NAME)
    private String name;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    private Address office;
    private String officeCity;       // the city of the office, kept for searches
    private String initials;         // computed from name
    private String emailDomain;      // computed from email
}
```

Could `emailDomain` be declared `@Category(SYSTEM_OPERATIONS)` because "a domain is not personal"?

## Self-check quiz

1. Why must you write `value =` in `@Category(value = USER_NAME, integrity = Integrity.HIGH)`?
2. What happens if a field declares `confidentiality = Confidentiality.LOW` for a `MEDIUM` category?
3. Can `@DefaultCategory` name `USER_CONTACT_EMAIL`?
4. Which protection does a `@CategoryByRow` field get?
5. Which fields are excluded without any annotation?
6. A superclass has `@DefaultCategory`. Does it cover the fields declared in its subclasses?
7. A field holds the age computed from a date of birth. Which annotation does it take, and what classification does
   it get?
8. What does the classifier report for a field of type `Address` when `Address` is not marked `@Composite`?

## Further reading

- [Reference — Annotations](../reference/annotations/README.md):
  [`@Category`](../reference/annotations/category.md),
  [`@DefaultCategory`](../reference/annotations/default-category.md),
  [`@CategoryByRow`](../reference/annotations/category-by-row.md),
  [`@DerivedFrom`](../reference/annotations/derived-from.md),
  [`@Composite`](../reference/annotations/composite.md),
  [`@Exempt`](../reference/annotations/exempt.md)
- [Reference — Classifier](../reference/classify/classifier.md): resolution order and exclusions

---

← Previous: [Module 2 — The taxonomy](02-the-taxonomy.md) · Next:
[Module 4 — Verifying in tests](04-verifying-in-tests.md) →

<details markdown="block">
<summary><strong>Solutions</strong></summary>

**Exercise 3.1**

```java
public class Enrollment extends AuditedEntity {

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)     // id of a record about a student
    private Long id;

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)
    private Long studentId;

    @Category(USER_EDUCATION_PROGRAM)
    private String programCode;

    @Category(USER_EDUCATION_ENROLLMENT)
    private String academicYear;

    @Category(USER_FINANCIAL_FEE)
    private BigDecimal tuitionFee;

    @Category(USER_FINANCIAL_SCHOLARSHIP)
    private String scholarshipType;
}

public class Grade extends AuditedEntity {

    @Category(USER_UNIQUE_ID_RECORD_REFERENCE)
    private Long enrollmentId;

    @Category(USER_EDUCATION_COURSE_HISTORY)
    private String courseCode;

    @Category(USER_EDUCATION_GRADE)
    private BigDecimal mark;
}
```

A programme code on its own is public reference data, but in `Enrollment` it says *which programme this student
studies*: it is `USER_EDUCATION_PROGRAM`. The same reasoning applies to `courseCode` in `Grade`.

**Exercise 3.2**

```java
public class Staff extends AuditedEntity {

    @Category(USER_UNIQUE_ID)
    private Long id;

    @Category(USER_NAME)
    private String fullName;

    @Category(USER_CONTACT_EMAIL)
    private String workEmail;

    @Category(USER_JOB_TITLE)
    private String jobTitle;

    @Category(USER_WORKPLACE_UNIT)
    private String department;

    @Category(USER_GOVERNMENT_ID_SOCIAL_SECURITY_NUMBER)
    private String socialSecurityNumber;

    @Category(USER_FINANCIAL_BANK_ACCOUNT)
    private String iban;

    @Category(USER_CRIMINAL_HISTORY)
    private Boolean sexOffenceCertificateClear;

    @Category(USER_AUTHORIZATION_PASSWORD)
    private String passwordHash;
}
```

`sexOffenceCertificateClear` is criminal offence data (`CRIMINAL_OFFENCE`, GDPR art. 10) — even a boolean derived
from the certificate says something about convictions. No field is special category. `passwordHash` is a secret
(the standard policy marks `user.authorization.password` as secret).

**Exercise 3.3**

- Class: `USER_CONTACT` is personal (`PERSONAL_DEFAULT`) and the reason is blank (`MISSING_REASON`). Remove the
  default and annotate the personal fields one by one.
- `email`: does not compile; it needs `value = USER_CONTACT_EMAIL`.
- `theme`: identifiability on a non-personal key (`IDENTIFIABILITY_NOT_APPLICABLE`); remove `identifiability`.
- `name`: two integrity values (`AMBIGUOUS_VALUE`); keep one, e.g. `integrity = Integrity.HIGH`.
- `renderCache`: JPA's `@Transient` does not exclude it, and without a default it is `UNCLASSIFIED_FIELD`. Add
  `@Exempt("rendering cache, rebuilt on demand")` or make it Java `transient`.

**Exercise 3.4**

(a) `@Category(USER_CONTENT_PRIVATE)` (raise confidentiality if messages routinely contain sensitive details).
(b) `@CategoryByRow(declaredBy = "the form type", ceiling = USER_HEALTH_AND_MEDICAL)` — a disability certificate is
health data. (c) Nothing: Java `transient` fields are excluded automatically. (d) `@DerivedFrom("dateOfBirth")`: the
age is personal data like the date of birth, so it takes `user.demographic.date_of_birth`; `@Exempt` would hide it
from every review. If it is only a getter with no field, there is nothing to classify — the classifier reads fields,
not methods.

**Exercise 3.5**

```java
public class Contact {
    @Category(USER_NAME)
    private String name;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    private Address office;                              // office.street, office.city, office.postalCode

    @DerivedFrom("office.city")                          // user.contact.address.city
    private String officeCity;

    @DerivedFrom("name")                                 // user.name
    private String initials;

    @DerivedFrom("email")                                // user.contact.email
    private String emailDomain;
}
```

`office` is not a field of its own: the classifier reports `office.street`, `office.city` and `office.postalCode`.
`emailDomain` cannot be `@Category(SYSTEM_OPERATIONS)`: a value computed from personal data must protect at least as
much as its source (`WEAKER_THAN_SOURCE`). Only a value declared anonymized or aggregated, such as an average over
many people, may protect less.

**Quiz

1. Java allows the single-element shorthand only when `value` is the only element given.
2. A `LOWERED_LEVEL` error; levels can only be raised.
3. No: it is personal (`PERSONAL_DEFAULT`).
4. The protection of its ceiling, and `IDENTIFIED` if the ceiling is personal.
5. `static`, synthetic and Java `transient` fields.
6. No. It covers the fields the annotated class declares (and, when that class is the one classified, unannotated
   fields inherited from superclasses without their own default).
7. `@DerivedFrom("dateOfBirth")`. It takes the classification of its source: `user.demographic.date_of_birth`,
   identified, personal data, with basis `DERIVED`.
8. `UNCLASSIFIED_FIELD` for that field, unless a class default covers it as one value: without `@Composite` it is an
   ordinary field.

</details>
