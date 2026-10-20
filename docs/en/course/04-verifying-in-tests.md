---
title: "4. Verifying in tests"
parent: "Course"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - src/main/java/io/github/devbpm/linneo/classify/ClassificationException.java
  - src/main/java/io/github/devbpm/linneo/classify/Violation.java
-->

# Module 4 — Verifying in tests

## Learning objectives

After this module you will be able to:

- write a JUnit test that fails the build when the data model is not fully and correctly classified;
- read a `ClassificationException` and locate each problem;
- recognise every kind of `Violation` and fix it;
- handle warnings for deprecated keys;
- feed the classifier with the entity classes of a JPA application.

## Why a test

Annotations alone are documentation. What turns them into a control is a test that runs on every build: when a
developer adds `mobilePhone` to `Student` and forgets to classify it, the build fails and the pull request shows a
one-line fix to review. This is the fail-closed principle from module 1, enforced by CI.

## The test

`Classifier` takes a `Taxonomy` and checks classes against it. `verify` classifies a collection of classes and
throws a `ClassificationException` if **any** of them has **any** error:

```java
package edu.example.university;

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.ClassClassification;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

class DataClassificationTest {

    private static final Taxonomy TAXONOMY = Taxonomy.load();

    @Test
    void everyEntityIsClassified() {
        List<ClassClassification> classifications = new Classifier(TAXONOMY).verify(List.of(
                MenuGroup.class, Student.class, Enrollment.class, Grade.class,
                Staff.class, Document.class, Attachment.class));

        // verify() only throws for errors; warnings (deprecated keys) are still in the result.
        classifications.stream()
                .flatMap(c -> c.violations().stream())
                .map(Violation::toString)
                .forEach(System.out::println);
    }
}
```

Points to note:

- Use **`Taxonomy.load()`**, so that the test sees the same extensions (module 6) as the application. With
  `Taxonomy.standard()`, keys added by your extension would be reported as unknown.
- `verify` reports the errors of **every** class at once, not just the first one, so a single run gives you the
  whole to-do list.
- When there is no error, `verify` returns the `ClassClassification` of each class, in the given order. Module 5 uses
  that result.
- `Classifier` is immutable and thread-safe; you can keep one in a static field. It only reads annotations through
  `Class.getDeclaredFields()`, never field values, so it needs no running database and no `setAccessible`.

If you prefer to inspect a single class without throwing, use `classify(Class)` and its `isValid()`, `errors()` and
`violations()` methods (module 5).

## Reading the exception

Suppose a developer adds two fields to `Student`:

```java
@Category("user.contact.mail")          // typo
private String personalEmail;

private String mobilePhone;             // forgotten
```

The test fails with:

```
io.github.devbpm.linneo.classify.ClassificationException: 2 classification error(s):
 - ERROR UNKNOWN_CATEGORY edu.example.university.Student.personalEmail: unknown data category 'user.contact.mail'
 - ERROR UNCLASSIFIED_FIELD edu.example.university.Student.mobilePhone: no @Category on the field and no @DefaultCategory on Student
```

Each line is a `Violation`, printed as:

```
<SEVERITY> <CODE> <fully qualified class>[.<field>]: <message>
```

- The class is the one **being classified**, even when the field is declared in a superclass.
- The field part is missing when the problem is on a class annotation (`@DefaultCategory`).
- The message names the key or annotation involved.

Programmatically, `ClassificationException.errors()` returns the same `Violation`s, each with `type()`, `field()`,
`severity()`, `code()` and `message()`. That is useful to group or count them:

```java
try {
    classifier.verify(entities);
} catch (ClassificationException e) {
    Map<Violation.Code, Long> byCode = e.errors().stream()
            .collect(Collectors.groupingBy(Violation::code, TreeMap::new, Collectors.counting()));
    System.out.println(byCode);
    throw e;
}
```

## Every kind of violation, and how to fix it

The messages below are the exact ones Linneo produces (with `X` for the class, `k` for a key).

| Code | Severity | Message | Fix |
|---|---|---|---|
| `UNCLASSIFIED_FIELD` | error | `no @Category on the field and no @DefaultCategory on X` (or `on Superclass or X` for an inherited field) | Add `@Category` (or `@CategoryByRow`/`@Exempt`), or a non-personal `@DefaultCategory` if the class holds no personal data |
| `UNKNOWN_CATEGORY` | error | `unknown data category 'k'` | Fix the key (use the `Categories` constants), or make sure the extension that defines it is loaded (`Taxonomy.load()`) |
| `PERSONAL_DEFAULT` | error | `@DefaultCategory of X names 'k', which is personal data: declare personal fields one by one with @Category` | Remove the default and annotate personal fields individually |
| `MISSING_REASON` | error | `@DefaultCategory of X needs a reason`, `@Exempt needs a reason` or `@CategoryByRow needs declaredBy` | Write the reason for the reviewer |
| `CONFLICTING_ANNOTATIONS` | error | `@Exempt cannot be combined with @Category or @CategoryByRow` or `@Category and @CategoryByRow cannot be used together` | Keep only one |
| `LOWERED_LEVEL` | error | `confidentiality LOW is lower than the MEDIUM of 'k'` (or `integrity ...`) | Remove the element or raise it; levels only go up |
| `AMBIGUOUS_VALUE` | error | `confidentiality must have at most one value` (or `integrity`, `identifiability`) | Give one value |
| `IDENTIFIABILITY_NOT_APPLICABLE` | error | `'k' is not personal data and has no identifiability` | Remove `identifiability`, or check that the key is right |
| `DEPRECATED_CATEGORY` | **warning** | `data category 'k' is deprecated; use 'r'` | Replace the key with the one suggested |

Here is a deliberately broken class and the real output of `verify`:

```java
public class BadFields {
    @Category("user.contact.mail") private String email;
    @Category(value = USER_HEALTH_AND_MEDICAL_DISABILITY, confidentiality = Confidentiality.MEDIUM)
    private Integer disabilityDegree;
    @Category(value = SYSTEM_CONFIGURATION, identifiability = Identifiability.PSEUDONYMIZED) private String theme;
    @Category(value = USER_NAME, integrity = {Integrity.MEDIUM, Integrity.HIGH}) private String name;
    @Exempt("") @Category(USER_CONTACT_EMAIL) private String email2;
    @Category(USER_NAME) @CategoryByRow(declaredBy = "the type", ceiling = USER_HEALTH_AND_MEDICAL) private String both;
    @CategoryByRow(declaredBy = "", ceiling = USER_HEALTH_AND_MEDICAL) private byte[] blob;
}
```

```
8 classification error(s):
 - ERROR UNKNOWN_CATEGORY edu.example.university.BadFields.email: unknown data category 'user.contact.mail'
 - ERROR LOWERED_LEVEL edu.example.university.BadFields.disabilityDegree: confidentiality MEDIUM is lower than the HIGH of 'user.health_and_medical.disability'
 - ERROR IDENTIFIABILITY_NOT_APPLICABLE edu.example.university.BadFields.theme: 'system.configuration' is not personal data and has no identifiability
 - ERROR AMBIGUOUS_VALUE edu.example.university.BadFields.name: integrity must have at most one value
 - ERROR CONFLICTING_ANNOTATIONS edu.example.university.BadFields.email2: @Exempt cannot be combined with @Category or @CategoryByRow
 - ERROR MISSING_REASON edu.example.university.BadFields.email2: @Exempt needs a reason
 - ERROR CONFLICTING_ANNOTATIONS edu.example.university.BadFields.both: @Category and @CategoryByRow cannot be used together
 - ERROR MISSING_REASON edu.example.university.BadFields.blob: @CategoryByRow needs declaredBy
```

And a class-level problem:

```java
@DefaultCategory(value = USER_CONTACT, reason = "")
public class ContactPreferences {
    private String channel;
}
```

```
2 classification error(s):
 - ERROR MISSING_REASON edu.example.university.ContactPreferences: @DefaultCategory of ContactPreferences needs a reason
 - ERROR PERSONAL_DEFAULT edu.example.university.ContactPreferences: @DefaultCategory of ContactPreferences names 'user.contact', which is personal data: declare personal fields one by one with @Category
```

Note that `channel` is not reported separately: the class default is wrong, so the field is left unclassified and
the problem is reported once, on the class.

### Inherited fields

If `AuditedEntity` had no `@DefaultCategory`, its `createdAt` field would be reported in **every** subclass, naming
both classes:

```
 - ERROR UNCLASSIFIED_FIELD edu.example.university.Student.createdAt: no @Category on the field and no @DefaultCategory on AuditedEntity or Student
```

The fix belongs in the superclass (annotate the field or give `AuditedEntity` a default), not in each subclass.

## Warnings: deprecated keys

A deprecated key keeps working: the field is classified with its **replacement**, and a `WARNING` is recorded.
Warnings do not make `verify` throw. Linneo 0.1.0's standard taxonomy has no deprecated data category, so you will
see this when your own extension deprecates a term (module 6) or when a future version of the taxonomy does. For
example, with the campus extension of module 6, where `user.education.lab_journal` is replaced by
`user.education.lab_notebook`:

```
WARNING DEPRECATED_CATEGORY edu.example.university.LabNotebook.file: data category 'user.education.lab_journal' is deprecated; use 'user.education.lab_notebook'
```

To keep warnings visible, print them (as in the test above). To treat them as errors in your build, assert there
are none:

```java
List<Violation> warnings = classifications.stream().flatMap(c -> c.violations().stream()).toList();
assertTrue(warnings.isEmpty(), () -> "Replace deprecated keys: " + warnings);
```

## Getting the classes from JPA

Listing classes by hand works for a small model, but it is easy to forget one. With JPA, take them from the
metamodel. Include the **embeddables** too, since the classifier does not descend into `@Embedded` fields:

```java
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.Classifier;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import java.util.List;
import java.util.stream.Stream;

static void verifyAllEntities(EntityManagerFactory entityManagerFactory) {
    Metamodel metamodel = entityManagerFactory.getMetamodel();
    List<Class<?>> classes = Stream.concat(metamodel.getEntities().stream(), metamodel.getEmbeddables().stream())
            .<Class<?>>map(ManagedType::getJavaType)
            .toList();
    new Classifier(Taxonomy.load()).verify(classes);
}
```

Call it from an integration test that already has an `EntityManagerFactory` (for example a Spring Boot test with
an injected `EntityManagerFactory`). Mapped superclasses such as `AuditedEntity` do not need to be listed: their
fields are classified as part of each entity.

Without JPA, any source of classes works: a fixed list, a class path scan, the records of a package.

## Common mistakes

- **Using `Taxonomy.standard()` in the test while the application uses extension keys** → `UNKNOWN_CATEGORY` for
  every extension key.
- **Catching the exception and only logging it.** The point is to fail the build.
- **Verifying only some entities.** Take them from the metamodel so new entities are covered automatically.
- **Fixing a violation by adding `@Exempt`** to a field that holds data. Review exemptions like code.
- **Forgetting embeddables.** Their fields are invisible unless the embeddable class is verified too.
- **Ignoring warnings forever.** A deprecated key still works, but the replacement is what the catalogue and
  reports will use.

## Exercises

**Exercise 4.1 — The test.** Write `DataClassificationTest` for the model built in module 3 (`MenuGroup`,
`Student`, `Enrollment`, `Grade`, `Staff`, `Document`, `Attachment`, `CourseStatistics`). Run it and make it pass.

**Exercise 4.2 — Read and fix.** A colleague's branch produces this output. For each line, say what is wrong and
write the fix.

```
4 classification error(s):
 - ERROR UNCLASSIFIED_FIELD edu.example.university.Staff.officePhone: no @Category on the field and no @DefaultCategory on Staff
 - ERROR LOWERED_LEVEL edu.example.university.Grade.mark: integrity MEDIUM is lower than the HIGH of 'user.education.grade'
 - ERROR PERSONAL_DEFAULT edu.example.university.Building: @DefaultCategory of Building names 'user.workplace', which is personal data: declare personal fields one by one with @Category
 - ERROR CONFLICTING_ANNOTATIONS edu.example.university.Document.attachments: @Exempt cannot be combined with @Category or @CategoryByRow
```

**Exercise 4.3 — Strict mode.** Extend your test so that it also fails when there is any warning, with a message
that lists them.

**Exercise 4.4 — Summary.** Change the test so that, when verification fails, it prints how many errors there are of
each `Violation.Code` before rethrowing.

## Self-check quiz

1. Does `verify` stop at the first class with an error?
2. Does a `DEPRECATED_CATEGORY` violation make `verify` throw?
3. In `ERROR UNCLASSIFIED_FIELD edu.example.university.Student.createdAt: ...`, which class declares `createdAt`?
4. Why should the test use `Taxonomy.load()`?
5. Why include JPA embeddables in the verified classes?

## Further reading

- [Reference — Classifier](../reference/classify/classifier.md)
- [Reference — Violations](../reference/classify/violations.md): every code with its messages
- [Reference — Results](../reference/classify/results.md)

---

← Previous: [Module 3 — Annotating models](03-annotating-models.md) · Next:
[Module 5 — Using the classification](05-using-the-classification.md) →

<details markdown="block">
<summary><strong>Solutions</strong></summary>

**Exercise 4.1**

The test shown in this module with `CourseStatistics.class` added to the list. It passes with the annotations from
the solutions of module 3 and prints nothing (no warnings).

**Exercise 4.2**

- `Staff.officePhone`: unannotated. Add `@Category(USER_CONTACT_PHONE_NUMBER)`.
- `Grade.mark`: declares `integrity = Integrity.MEDIUM`, lower than the category's `HIGH`. Remove the element.
- `Building`: `user.workplace` is personal. A building is organisation data: use
  `@DefaultCategory(value = ORG_LOCATION, reason = "university buildings and rooms")`, and annotate any personal
  field (say, the name of the building manager) with its own `@Category`.
- `Document.attachments`: carries both `@Exempt` and a category. A relationship field holds no data itself: keep
  only `@Exempt("relationship: each Attachment is classified on its own")`.

**Exercise 4.3**

```java
List<Violation> warnings = classifications.stream().flatMap(c -> c.violations().stream()).toList();
assertTrue(warnings.isEmpty(), () -> "Replace deprecated keys: " + warnings);
```

(When `verify` returns, every remaining violation is a warning.)

**Exercise 4.4**

```java
try {
    classifications = classifier.verify(entities);
} catch (ClassificationException e) {
    Map<Violation.Code, Long> byCode = e.errors().stream()
            .collect(Collectors.groupingBy(Violation::code, TreeMap::new, Collectors.counting()));
    System.out.println(byCode);
    throw e;
}
```

**Quiz**

1. No: it classifies every class and reports all errors together.
2. No, it is a warning; the field is classified with the replacement.
3. The output does not say: it names the class being classified (`Student`). `createdAt` may be declared in a
   superclass (in our model, `AuditedEntity`).
4. So that the extensions registered in the application are applied and their keys are known.
5. The classifier does not descend into `@Embedded` fields; the embeddable's own fields are checked only if the
   embeddable class is verified.

</details>
