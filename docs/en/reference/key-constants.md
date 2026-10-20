---
title: "Key constants"
parent: "Reference"
nav_order: 5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/Categories.java
  - src/main/java/io/github/devbpm/linneo/DataUses.java
  - src/main/java/io/github/devbpm/linneo/DataSubjects.java
  - tools/fideslang/update.py
-->

# Key constants

`io.github.devbpm.linneo.Categories`, `DataUses` and `DataSubjects` — `public final class`es with a private
constructor, `@since 0.1.0`.

Each holds one `public static final String` per key of the [standard taxonomy](standard-taxonomy.md):

| Class | Keys of | Count |
|---|---|---|
| `Categories` | data categories | 123 |
| `DataUses` | data uses (purposes of processing) | 63 |
| `DataSubjects` | data subjects | 21 |

## Why constants

- They are **compile-time constants**, so they can be used as annotation values:
  `@Category(Categories.USER_CONTACT_EMAIL)`.
- A typo does not compile, while a mistyped string literal is only caught by the
  [`Classifier`](classify/classifier.md) (`UNKNOWN_CATEGORY`).
- The IDE completes them, and each constant's Javadoc shows the term's name and description.

Plain strings are equally valid wherever a key is expected; use them for keys your own
[extensions](extending.md) add, which have no constant. The extensions shipped with Linneo bring their own constants:
the [university extension](extensions/university.md) has `UniversityCategories`, `UniversityDataUses` and
`UniversityDataSubjects`.

```java
import io.github.devbpm.linneo.annotation.Category;

import static io.github.devbpm.linneo.Categories.*;

public class Person {
    @Category(USER_NAME)
    private String fullName;

    @Category("user.education.lab_notebook")   // added by an application extension: no constant
    private String notebookTitle;
}
```

## Naming rule

The constant name is the key in upper case with every dot replaced by an underscore:

| Key | Constant |
|---|---|
| `user` | `Categories.USER` |
| `user.contact.email` | `Categories.USER_CONTACT_EMAIL` |
| `user.health_and_medical.disability` | `Categories.USER_HEALTH_AND_MEDICAL_DISABILITY` |
| `essential.service.authentication` | `DataUses.ESSENTIAL_SERVICE_AUTHENTICATION` |
| `third_party_sharing` | `DataUses.THIRD_PARTY_SHARING` |
| `job_applicant` | `DataSubjects.JOB_APPLICANT` |

Each class lists the fideslang keys first (in the order of the fideslang source), then the keys added by Linneo.
Deprecated terms keep their constant (for example `DataUses.PERSONALIZE_PROFILING`).

## Generated code

The three classes are **generated** by `tools/fideslang/update.py` from the `fideslang-*.tsv` and `linneo-*.tsv`
resources; do not edit them by hand. After adding a term to a `linneo-*.tsv` file, rerun the script:

```bash
python -I tools/fideslang/update.py 3.1.4
```

A test of the library checks that every key of the standard taxonomy has a constant and every constant a key. See
[Standard taxonomy](standard-taxonomy.md#regenerating-from-fideslang).

See also: [Standard taxonomy](standard-taxonomy.md) · [`@Category`](annotations/category.md) ·
[Taxonomy](taxonomy.md) · course: [Annotating models](../course/03-annotating-models.md)
