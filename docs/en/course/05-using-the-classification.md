---
title: "5. Using the classification"
parent: "Course"
nav_order: 5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/ClassClassification.java
  - src/main/java/io/github/devbpm/linneo/classify/FieldClassification.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - src/main/java/io/github/devbpm/linneo/processing/package-info.java
-->

# Module 5 — Using the classification

## Learning objectives

After this module you will be able to:

- obtain the classification of a class with `classify` and read a `ClassClassification`;
- read a `FieldClassification`: category, basis, effective levels, identifiability, personal and secret flags;
- list the personal fields of a model and build a data inventory;
- assemble a report in the style of a record of processing activities (RoPA) from your own description of
  processes plus the classification;
- base masking decisions (for example in logs) on the classification, failing closed.

## From annotations to data

The test from module 4 guarantees that the model is fully classified. The same `Classifier` also gives you the
result as data:

```java
Classifier classifier = new Classifier(Taxonomy.load());

ClassClassification student = classifier.classify(Student.class);       // one class
List<ClassClassification> all = classifier.classifyAll(entities);      // several, no exception
List<ClassClassification> checked = classifier.verify(entities);      // several, throws on errors
```

In application code prefer `verify` (or check `isValid()`): fields with an error are, in general, **missing** from the
result, so a report built from an invalid classification would silently leave them out.

## `ClassClassification`

| Member | Returns |
|---|---|
| `type()` | the class |
| `fields()` | one `FieldClassification` per classified field, **superclass fields first**, in declaration order |
| `violations()` | every problem found, errors and warnings |
| `isValid()` | `true` if there is no error (warnings allowed) |
| `errors()` | only the errors |
| `personalFields()` | the fields whose `isPersonal()` is `true` |
| `holdsPersonalData()` | whether any field is personal |
| `field(name)` | the classification of one field, or empty if unknown, excluded (`@Exempt`, `static`, `transient`) or in error |

```java
student.holdsPersonalData();                              // true
student.field("displayName");                             // Optional.empty: @Exempt
student.field("email").orElseThrow().category().key();    // "user.contact.email"
```

## `FieldClassification`

| Component | Meaning |
|---|---|
| `declaringClass()` | the class that declares the field (`AuditedEntity` for `createdBy`) |
| `field()` | the field name |
| `category()` | the `DataCategory`, already resolved to its replacement if the key was deprecated |
| `basis()` | where the category comes from: `DECLARED` (`@Category`), `CLASS_DEFAULT` (`@DefaultCategory`), `BY_ROW` (ceiling of `@CategoryByRow`) |
| `reason()` | the reason of a class default, or the `declaredBy` of a per-row field; `null` for `DECLARED` |
| `confidentiality()`, `integrity()` | the **effective** levels: the category's, or the stricter ones the field declares |
| `identifiability()` | `IDENTIFIED`, `PSEUDONYMIZED`, `ANONYMIZED` or `AGGREGATED`; `null` when the category is not personal |
| `isPersonal()` | personal category **and** stored `IDENTIFIED` or `PSEUDONYMIZED` |
| `isSecret()` | the category's secret flag |

`toString()` gives a compact line, handy in logs and assertions:

```java
student.fields().forEach(System.out::println);
```

```
AuditedEntity.createdBy = user.account.username (DECLARED)
AuditedEntity.createdAt = system.operations (CLASS_DEFAULT)
AuditedEntity.updatedAt = system.operations (CLASS_DEFAULT)
AuditedEntity.version = system.operations (CLASS_DEFAULT)
Student.id = user.unique_id (DECLARED)
Student.nationalId = user.government_id.national_identification_number (DECLARED)
Student.firstName = user.name.first (DECLARED)
Student.lastName = user.name.last (DECLARED)
Student.email = user.contact.email (DECLARED)
Student.dateOfBirth = user.demographic.date_of_birth (DECLARED)
Student.disabilityDegree = user.health_and_medical.disability (DECLARED)
Student.analyticsId = user.unique_id.pseudonymous (DECLARED)
Student.academicStatus = user.education.status (DECLARED)
Student.counsellingNotes = user.content.private (DECLARED)
```

A few values worth noticing:

- `counsellingNotes`: `confidentiality()` is `HIGH` (raised on the field), although `user.content.private` is
  `MEDIUM`.
- `analyticsId`: `identifiability()` is `PSEUDONYMIZED`, `isPersonal()` is `true`.
- `CourseStatistics.averageMark`: category `user.education.grade` (personal), but `identifiability()` is
  `AGGREGATED`, so `isPersonal()` is `false` and the field is not in `personalFields()`.
- `Attachment.content`: `basis()` is `BY_ROW`, `reason()` is `"the documentType of the owning Document"`, category
  `user.health_and_medical` (the ceiling).
- `Staff.passwordHash`: `isSecret()` is `true`.

## A data inventory

A data inventory answers "where do we hold personal data, of which kind, and how protected is it". With the
classification, it is a loop:

```java
Classifier classifier = new Classifier(Taxonomy.load());
List<ClassClassification> model = classifier.verify(List.of(
        MenuGroup.class, Student.class, Enrollment.class, Grade.class,
        Staff.class, Document.class, Attachment.class, CourseStatistics.class));

System.out.println("entity;field;category;nature;confidentiality;integrity;identifiability;secret");
for (ClassClassification entity : model) {
    for (FieldClassification field : entity.personalFields()) {
        DataCategory category = field.category();
        System.out.println(String.join(";",
                entity.type().getSimpleName(),
                field.field(),
                category.key(),
                category.nature().name(),
                field.confidentiality().name(),
                field.integrity().name(),
                field.identifiability().name(),
                Boolean.toString(field.isSecret())));
    }
}

model.stream()
        .filter(c -> c.personalFields().stream()
                .anyMatch(f -> f.category().nature() != LegalNature.ORDINARY))
        .forEach(c -> System.out.println("Reinforced protection (art. 9/10): " + c.type().getSimpleName()));
```

An extract of the output:

```
entity;field;category;nature;confidentiality;integrity;identifiability;secret
Student;createdBy;user.account.username;ORDINARY;LOW;MEDIUM;IDENTIFIED;false
Student;id;user.unique_id;ORDINARY;LOW;MEDIUM;IDENTIFIED;false
...
Student;disabilityDegree;user.health_and_medical.disability;SPECIAL_CATEGORY;HIGH;HIGH;IDENTIFIED;false
Student;analyticsId;user.unique_id.pseudonymous;ORDINARY;LOW;MEDIUM;PSEUDONYMIZED;false
...
Staff;sexOffenceCertificateClear;user.criminal_history;CRIMINAL_OFFENCE;HIGH;HIGH;IDENTIFIED;false
Staff;passwordHash;user.authorization.password;ORDINARY;HIGH;HIGH;IDENTIFIED;true
...
Attachment;content;user.health_and_medical;SPECIAL_CATEGORY;HIGH;HIGH;IDENTIFIED;false
Reinforced protection (art. 9/10): Student
Reinforced protection (art. 9/10): Staff
Reinforced protection (art. 9/10): Attachment
```

`identifiability()` is never `null` here, because `personalFields()` only contains personal categories.
`CourseStatistics` does not appear at all: its fields are reference data or aggregated.

Field names are not column names: map them with your persistence layer's naming rules if the inventory must speak
in tables and columns.

## A RoPA-style report

GDPR art. 30 asks controllers to keep a record of processing activities: for each activity, its purposes, the
categories of data subjects and the categories of personal data, among other things. **Data uses and data subjects
describe processes, not fields**, so there are no annotations for them. Linneo models processing activities in the
`io.github.devbpm.linneo.processing` package, the subject of [module 9](09-processing-activities.md). Here, to see
how the pieces fit, you sketch the activities in your own code with both vocabularies (`DataUses`, `DataSubjects`)
and let the classification fill in the categories:

```java
record ActivitySketch(String name, String purpose, List<String> subjects, List<Class<?>> entities) {
}

Taxonomy taxonomy = Taxonomy.load();
Classifier classifier = new Classifier(taxonomy);
List<ActivitySketch> activities = List.of(
        new ActivitySketch("Assessment and grading", DataUses.EDUCATION_ASSESSMENT,
                List.of(DataSubjects.STUDENT, DataSubjects.FACULTY_MEMBER),
                List.of(Enrollment.class, Grade.class)),
        new ActivitySketch("Staff administration", DataUses.EMPLOYMENT,
                List.of(DataSubjects.EMPLOYEE, DataSubjects.STAFF_MEMBER),
                List.of(Staff.class)));

for (ActivitySketch activity : activities) {
    System.out.println("## " + activity.name());
    System.out.println("Purpose: " + taxonomy.requireDataUse(activity.purpose()).name());
    activity.subjects().forEach(s -> System.out.println("Subject: " + taxonomy.requireDataSubject(s).name()));
    TreeSet<String> categories = new TreeSet<>();
    for (Class<?> entity : activity.entities()) {
        classifier.classify(entity).personalFields().stream()
                .map(FieldClassification::category)
                .forEach(c -> categories.add(c.key() + " (" + c.nature() + ")"));
    }
    categories.forEach(c -> System.out.println("Category: " + c));
}
```

The output for the second activity:

```
## Staff administration
Purpose: Employment
Subject: Employee
Subject: Staff Member
Category: user.account.username (ORDINARY)
Category: user.authorization.password (ORDINARY)
Category: user.contact.email (ORDINARY)
Category: user.criminal_history (CRIMINAL_OFFENCE)
Category: user.financial.bank_account (ORDINARY)
Category: user.government_id.social_security_number (ORDINARY)
Category: user.job_title (ORDINARY)
Category: user.name (ORDINARY)
Category: user.unique_id (ORDINARY)
Category: user.workplace.unit (ORDINARY)
```

`requireDataUse` and `requireDataSubject` throw a `TaxonomyException` for an unknown key, so a typo in the activity
list is caught as soon as the report runs. The result is raw material for the data protection officer, not a legal
document: legal bases, recipients, retention periods and transfers are missing. Module 9 declares them with
`ProcessingActivity`, checks them with `ProcessingChecker` and produces the record from the same declarations.

## Masking decisions in logs

Logs are a classic leak: an entity's `toString()` ends up in an error trace with a student's disability degree in
it. The classification lets you decide, per field, what may appear in clear. The safe design is an **allow-list**:
compute the fields that *may* be logged, and mask everything else — including fields you do not know about.

```java
/** Decides which fields may appear in clear in application logs. Everything else is masked. */
public final class LogMasking {

    private final Classifier classifier;
    private final Map<Class<?>, Set<String>> loggable = new ConcurrentHashMap<>();

    public LogMasking(Classifier classifier) {
        this.classifier = classifier;
    }

    /** The fields of a class that may be logged in clear: an allow-list, so unknown fields stay masked. */
    public Set<String> loggableFields(Class<?> type) {
        return loggable.computeIfAbsent(type, t -> classifier.classify(t).fields().stream()
                .filter(LogMasking::mayBeLogged)
                .map(FieldClassification::field)
                .collect(Collectors.toUnmodifiableSet()));
    }

    static boolean mayBeLogged(FieldClassification field) {
        if (field.isSecret()) {
            return false;                                   // never, not even masked
        }
        if (!field.isPersonal()) {
            return true;                                    // not personal: system data, aggregates
        }
        return field.identifiability() == Identifiability.PSEUDONYMIZED
                && field.confidentiality() == Confidentiality.LOW;  // a correlation id, for example
    }
}
```

For `Student`, `loggableFields` returns `[analyticsId, createdAt, updatedAt, version]`; for `Staff`,
`[createdAt, updatedAt, version]`. The policy itself (what your organisation allows in logs) is yours; the
classification makes it explicit, testable and automatically applied to new fields. Because an unclassified field,
or one with an unknown key or a wrong level, is missing from `fields()`, it is never in the allow-list: the design
fails closed.

Your logging code then reads the values of the allowed fields (through getters, a serializer filter, etc.) and
prints a placeholder for the rest. Linneo itself never reads values.

## Common mistakes

- **Building reports from `classify` without checking `isValid()`.** Fields in error are missing; use `verify` or
  check first.
- **Using `category().isPersonal()` instead of `isPersonal()`.** The first ignores identifiability: an aggregated
  average would count as personal. Use `FieldClassification.isPersonal()` for "is this stored value personal data".
- **Using the category's levels instead of the field's.** `field.confidentiality()` includes the raise declared on
  the field; `field.category().confidentiality()` does not.
- **A deny-list for masking** ("mask these fields"). New personal fields leak until someone updates the list.
- **Expecting Linneo to know the purposes of processing.** Data uses and subjects are vocabularies for you to use
  where you describe processes.

## Exercises

**Exercise 5.1 — Reinforced protection.** Print every field of the model whose category is `SPECIAL_CATEGORY` or
`CRIMINAL_OFFENCE` and whose value is personal, as `Class.field (key)`.

**Exercise 5.2 — Inventory by category.** Produce, for each category key used by a personal field anywhere in the
model, the list of `Class.field` that use it, sorted by key.

**Exercise 5.3 — Three treatments.** Replace `loggableFields` with a method `Map<String, Treatment> treatments(Class<?>)`
where `Treatment` is `CLEAR`, `MASK` or `OMIT`: secrets are omitted (not even the field name appears), personal
fields are masked, the rest is clear. What should your logging code do with a field that is **not** in the map?

**Exercise 5.4 — Explain.** For `CourseStatistics`, what do `holdsPersonalData()`, `personalFields()` and
`field("averageMark").orElseThrow().identifiability()` return? Why?

## Self-check quiz

1. In which order does `fields()` list the fields of `Student`?
2. What is `basis()` for `Student.createdAt`, and what is `reason()`?
3. When is `identifiability()` `null`?
4. Is a `PSEUDONYMIZED` field personal according to `isPersonal()`?
5. Why is an allow-list safer than a deny-list for masking?

## Further reading

- [Reference — Results](../reference/classify/results.md): `ClassClassification` and `FieldClassification`
- [Reference — Classifier](../reference/classify/classifier.md)
- [Reference — Terms](../reference/terms.md): data uses and data subjects

---

← Previous: [Module 4 — Verifying in tests](04-verifying-in-tests.md) · Next:
[Module 6 — Extending the taxonomy](06-extending-the-taxonomy.md) →

<details markdown="block">
<summary><strong>Solutions</strong></summary>

**Exercise 5.1**

```java
for (ClassClassification c : model) {
    c.personalFields().stream()
            .filter(f -> f.category().nature() != LegalNature.ORDINARY)
            .forEach(f -> System.out.println(
                    c.type().getSimpleName() + "." + f.field() + " (" + f.category().key() + ")"));
}
```

```
Student.disabilityDegree (user.health_and_medical.disability)
Staff.sexOffenceCertificateClear (user.criminal_history)
Attachment.content (user.health_and_medical)
```

(`personalFields()` only holds personal categories, so `!= ORDINARY` means special category or criminal offence.)

**Exercise 5.2**

```java
Map<String, List<String>> byCategory = new TreeMap<>();
for (ClassClassification c : model) {
    for (FieldClassification f : c.personalFields()) {
        byCategory.computeIfAbsent(f.category().key(), k -> new ArrayList<>())
                .add(c.type().getSimpleName() + "." + f.field());
    }
}
byCategory.forEach((key, fields) -> System.out.println(key + " -> " + fields));
```

Note that `user.account.username` lists `createdBy` once per entity that extends `AuditedEntity`: inherited fields
belong to every concrete class.

**Exercise 5.3**

```java
enum Treatment { CLEAR, MASK, OMIT }

Map<String, Treatment> treatments(Class<?> type) {
    return classifier.classify(type).fields().stream().collect(Collectors.toUnmodifiableMap(
            FieldClassification::field,
            f -> f.isSecret() ? Treatment.OMIT : f.isPersonal() ? Treatment.MASK : Treatment.CLEAR));
}
```

A field that is not in the map must be treated as `MASK` (or `OMIT`): it may be a field in error or one added
after the map was computed. Never default to `CLEAR`.

**Exercise 5.4**

`holdsPersonalData()` is `false`, `personalFields()` is empty and the identifiability is `AGGREGATED`. The grade
category is personal, but the stored values summarise many students, so `FieldClassification.isPersonal()` is
`false`.

**Quiz**

1. Superclass fields first (`AuditedEntity`'s), then `Student`'s, each in declaration order.
2. `CLASS_DEFAULT`, with the reason of `AuditedEntity`'s `@DefaultCategory`:
   `"technical audit timestamps and optimistic-lock version"`.
3. When the category is not personal.
4. Yes. Only `ANONYMIZED` and `AGGREGATED` are not.
5. Anything not explicitly allowed — including new fields and fields in error — stays masked.

</details>
