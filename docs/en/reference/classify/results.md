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
public record ClassClassification(Class<?> type, List<FieldClassification> fields, List<Violation> violations)
```

The classification of a class: one entry per classified field, including inherited fields, plus the problems found.

| Component | Description |
|---|---|
| `type` | The classified class. |
| `fields` | The classified fields, superclass fields first, in declaration order. Exempt and excluded fields are absent; a field with an error may be absent. |
| `violations` | Every [`Violation`](violations.md) found, errors and warnings. |

The canonical constructor throws `NullPointerException` if `type` is `null` and copies both lists into immutable
lists.

| Method | Returns | Description |
|---|---|---|
| `boolean isValid()` | `boolean` | `true` if there is no error; warnings are allowed. |
| `List<Violation> errors()` | list | The violations of severity `ERROR`. |
| `List<FieldClassification> personalFields()` | list | The fields whose `isPersonal()` is `true`. |
| `boolean holdsPersonalData()` | `boolean` | `true` if any field is personal. |
| `Optional<FieldClassification> field(String name)` | `Optional` | The classification of a field by name; empty if the field is unknown, excluded, exempt or was dropped because of an error. If a subclass field hides a superclass field with the same name, the superclass one is returned (it comes first). |

```java
Classifier classifier = new Classifier(Taxonomy.standard());
ClassClassification person = classifier.classify(Person.class);
person.isValid();
person.personalFields();
DataCategory category = person.field("email").orElseThrow().category();
```

## FieldClassification

```java
public record FieldClassification(Class<?> declaringClass, String field, DataCategory category, Basis basis,
                                  String reason, Confidentiality confidentiality, Integrity integrity,
                                  Identifiability identifiability)
```

The classification of one field: its category and the effective protection of the field.

| Component | Nullable | Description |
|---|---|---|
| `declaringClass` | no | The class that declares the field (a superclass for inherited fields). |
| `field` | no | The field name. |
| `category` | no | The **current** data category: a deprecated key is already resolved to its replacement. |
| `basis` | no | Where the category comes from (see `Basis`). |
| `reason` | yes | The `reason` of a class default or the `declaredBy` of a per-row category; `null` for `DECLARED`. |
| `confidentiality` | no | The category's level, or the stricter one the field declares. |
| `integrity` | no | The category's level, or the stricter one the field declares. |
| `identifiability` | yes | How the value points to the person; `null` when the category is not personal (and for class defaults, which are never personal). |

The canonical constructor throws `NullPointerException` if `declaringClass`, `field`, `category`, `basis`,
`confidentiality` or `integrity` is `null`.

### Basis

`FieldClassification.Basis` — where the category of a field comes from.

| Constant | Source | `reason` |
|---|---|---|
| `DECLARED` | a [`@Category`](../annotations/category.md) on the field | `null` |
| `CLASS_DEFAULT` | the [`@DefaultCategory`](../annotations/default-category.md) of a class | the default's `reason` |
| `BY_ROW` | the ceiling of a [`@CategoryByRow`](../annotations/category-by-row.md) | `declaredBy` |

### Methods

| Method | Description |
|---|---|
| `boolean isPersonal()` | Whether the stored values are personal data: the category is personal **and** the identifiability is `IDENTIFIED` or `PSEUDONYMIZED`. Anonymized or aggregated values of a personal category are not. |
| `boolean isSecret()` | The category's `secret` flag: the field holds a secret that must never be read back, exported or published. |
| `String toString()` | `<DeclaringSimpleName>.<field> = <category key> (<BASIS>)`, for example `Person.email = user.contact.email (DECLARED)`. |

```java
for (FieldClassification field : person.fields()) {
    if (field.isSecret()) {
        // never export
    } else if (field.isPersonal() && field.confidentiality().compareTo(Confidentiality.MEDIUM) >= 0) {
        // mask in logs
    }
}
```

See also: [Classifier](classifier.md) · [Violations](violations.md) · [Protection levels](../protection-levels.md) ·
[OpenMetadata catalog](../openmetadata/catalog.md) · course: [Using the classification](../../course/05-using-the-classification.md)
