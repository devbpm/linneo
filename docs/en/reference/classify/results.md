---
title: "Classification results"
parent: "Reference"
nav_order: 10
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/ClassClassification.java
  - src/main/java/io/github/devbpm/linneo/classify/FieldClassification.java
-->

# Classification results

What [`Classifier`](classifier.md) returns. Both types are records in `io.github.devbpm.linneo.classify`,
`@since 0.1.0`.

## ClassClassification

```java
public record ClassClassification(Class<?> type, List<FieldClassification> fields, List<Exemption> exempted,
                                  List<Violation> violations)
```

The classification of a class: one entry per classified field, including inherited fields, the fields excluded with
`@Exempt`, and the problems found.

| Component | Description |
|---|---|
| `type` | The classified class. |
| `fields` | The classified fields, superclass fields first and, within a class, in the order the JVM reports, which is usually the declaration order but is not guaranteed. Identify a field by its `path`, never by its position. Exempt and excluded fields are absent; a field with an error may be absent. |
| `exempted` | The fields excluded with [`@Exempt`](../annotations/exempt.md), with their reasons (see [`Exemption`](#exemption)). |
| `violations` | Every [`Violation`](violations.md) found, errors and warnings. |

The canonical constructor throws `NullPointerException` if `type` is `null` and copies the three lists into immutable
lists.

| Method | Returns | Description |
|---|---|---|
| `boolean isValid()` | `boolean` | `true` if there is no error; warnings are allowed. |
| `List<Violation> errors()` | list | The violations of severity `ERROR`. |
| `List<FieldClassification> personalFields()` | list | The fields whose `isPersonal()` is `true`. |
| `boolean holdsPersonalData()` | `boolean` | `true` if any field is personal. |
| `Optional<FieldClassification> field(String path)` | `Optional` | The classification of a field by path; empty if the field is unknown, excluded, exempt or was dropped because of an error. A field that hides a superclass field of the same name is not classified (`DUPLICATE_FIELD`), so a path is never ambiguous. |
| `Protection ceiling()` | [`Protection`](../protection-levels.md#protection) | The protection a record of the class requires as a whole: the join of the protection of its fields, or `Protection.LOWEST` if there are none. It is what an export of whole records, or a column that stores the record serialized, needs. |
| `Protection ceilingOf(String path)` | [`Protection`](../protection-levels.md#protection) | The protection a field requires together with everything below it: the field itself or, for a field that holds a [`@Composite`](../annotations/composite.md), every field of the composite. It is the join of the field and of every field whose path starts with its path followed by a dot, or `Protection.LOWEST` if there is none. `NullPointerException` if `path` is `null`. |

```java
Classifier classifier = new Classifier(Taxonomy.standard());
ClassClassification person = classifier.classify(Person.class);
person.isValid();
person.personalFields();
DataCategory category = person.field("email").orElseThrow().category();
Protection whole = person.ceiling();                 // what a whole Person record requires
Protection email = person.ceilingOf("email");        // the field and any field below it
Protection home = person.ceilingOf("home");          // a composite field: all of home.street, home.city, ...
```

### Exemption

`ClassClassification.Exemption` — `public record Exemption(Class<?> declaringClass, String path, String reason)`,
`@since 0.1.0`

A field excluded from classification with `@Exempt`. It is listed so that excluding a field that used to be
classified is visible.

| Component | Description |
|---|---|
| `declaringClass` | The class that declares the field. |
| `path` | The field path: its name, or `field.part` for an exempt field of a [`@Composite`](../annotations/composite.md) the class holds. |
| `reason` | Why the field holds no data of the model: the value of `@Exempt`. |

The canonical constructor throws `NullPointerException` if a component is `null`. An `@Exempt` combined with
`@Category`, `@CategoryByRow` or `@DerivedFrom` is a `CONFLICTING_ANNOTATIONS` error and is not listed.

## FieldClassification

```java
public record FieldClassification(Class<?> declaringClass, String path, DataCategory category, Basis basis,
                                  String reason, Confidentiality confidentiality, Integrity integrity,
                                  Identifiability identifiability, List<String> derivedFrom)
```

The classification of one field: its category and the effective protection of the field.

| Component | Nullable | Description |
|---|---|---|
| `declaringClass` | no | The class that declares the field (a superclass for inherited fields, the composite for a field of a [`@Composite`](../annotations/composite.md)). |
| `path` | no | Identifies the field within the classified class: its name, or `field.part` for a field of a composite it holds (`home.street`). |
| `category` | no | The **current** data category: a deprecated key is already resolved to its replacement. |
| `basis` | no | Where the category comes from (see `Basis`). |
| `reason` | yes | The `reason` of a class default or the `declaredBy` of a per-row category; `null` for `DECLARED`. |
| `confidentiality` | no | The category's level, or the stricter one the field declares. |
| `integrity` | no | The category's level, or the stricter one the field declares. |
| `identifiability` | yes | How the value points to the person; `null` when the category is not personal (and for class defaults, which are never personal). |
| `derivedFrom` | no | For a [`DERIVED`](#basis) field, its sources: field paths of the same class (a composite path is expanded to its fields), or `ClassName#path`, with the binary name of the class, for a field of another class. Empty for any other field. |

The canonical constructor throws `NullPointerException` if `declaringClass`, `path`, `category`, `basis`,
`confidentiality` or `integrity` is `null`. A `null` `derivedFrom` means none; the list is copied into an immutable
one.

### Basis

`FieldClassification.Basis` — where the category of a field comes from.

| Constant | Source | `reason` |
|---|---|---|
| `DECLARED` | a [`@Category`](../annotations/category.md) on the field | `null` |
| `CLASS_DEFAULT` | the [`@DefaultCategory`](../annotations/default-category.md) of a class | the default's `reason` |
| `BY_ROW` | the ceiling of a [`@CategoryByRow`](../annotations/category-by-row.md) | `declaredBy` |
| `DERIVED` | a [`@DerivedFrom`](../annotations/derived-from.md) field: the classification of its source, or the category it declares, which protects at least as much as its sources | `null` |

The fields of a [`@Composite`](../annotations/composite.md) have the basis their own class gives them, not a basis of
their own.

### Methods

| Method | Description |
|---|---|
| `boolean isPersonal()` | Whether the stored values are personal data: the category is personal **and** the identifiability is `IDENTIFIED` or `PSEUDONYMIZED`. Anonymized or aggregated values of a personal category are not. |
| `boolean isSecret()` | The category's `secret` flag: the field holds a secret that must never be read back, exported or published. |
| `String fieldName()` | The name of the field: the last segment of its `path`, as declared in `declaringClass()`. |
| `Protection protection()` | The [protection](../protection-levels.md#protection) the stored values require: the category's nature when the values are personal data (`isPersonal()`) and `LegalNature.NONE` when they are anonymized or aggregated, with the field's effective `confidentiality` and `integrity` and the category's secret flag. |
| `boolean covers(DataCategory category)` | Whether the field protects data of `category` at least as much as that category requires: `protection().covers(category.protection())`. It answers two questions an application asks at run time, see [Checking against a category](#checking-against-a-category). `NullPointerException` if `category` is `null`. |
| `String toString()` | `<path> = <category key> (<BASIS>)`, for example `email = user.contact.email (DECLARED)`. |

```java
for (FieldClassification field : person.fields()) {
    if (field.isSecret()) {
        // never export
    } else if (field.isPersonal() && field.confidentiality().compareTo(Confidentiality.MEDIUM) >= 0) {
        // mask in logs
    }
}
```

### Checking against a category

`covers(category)` compares the [protection](../protection-levels.md#protection) of the field with the one the category
requires, with [`Protection.covers`](../protection-levels.md#protection): every nature of the category is covered by one
of the field, the levels are not lower and a secret stays a secret. Two questions use it:

- **A row of a [`@CategoryByRow`](../annotations/category-by-row.md) field.** The field takes the protection of its
  ceiling, so `covers` tells whether the category chosen for a row stays below the ceiling, that is, whether the field
  may hold data of that category.
- **A classification the data protection officer raises.** Called on the raised classification of a field with the
  category the code declared, it tells whether the raised one still covers what the code declared.

```java
// Attachment.content: BY_ROW, ceiling user.health_and_medical (special category, HIGH / HIGH)
FieldClassification content = classifier.classify(Attachment.class).field("content").orElseThrow();
Taxonomy taxonomy = Taxonomy.load();

content.covers(taxonomy.requireCategory(USER_HEALTH_AND_MEDICAL_DISABILITY));   // true: at the ceiling or below it
content.covers(taxonomy.requireCategory(USER_CONTACT_EMAIL));                   // true: ordinary data
content.covers(taxonomy.requireCategory(USER_CRIMINAL_HISTORY));                // false: criminal offence data is not
                                                                                // below a special category ceiling
```

The legal natures are compared with a partial order: `NONE` is below every nature, `ORDINARY` below
`SPECIAL_CATEGORY` and `CRIMINAL_OFFENCE`, and those two are not comparable.

See also: [Classifier](classifier.md) · [Violations](violations.md) · [Protection levels](../protection-levels.md) ·
[OpenMetadata catalog](../openmetadata/catalog.md) · course:
[Using the classification](../../course/05-using-the-classification.md)
