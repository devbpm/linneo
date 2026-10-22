---
title: "6. Extending the taxonomy"
parent: "Course"
nav_order: 6
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/TaxonomyBuilder.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyExtension.java
  - src/main/java/io/github/devbpm/linneo/CategoryDraft.java
  - src/main/java/io/github/devbpm/linneo/TermDraft.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityTaxonomy.java
-->

# Module 6 — Extending the taxonomy

## Learning objectives

After this module you will be able to:

- decide when an extension is the right answer (and when a standard key is enough);
- write a `TaxonomyExtension` that adds categories, data uses and data subjects;
- make existing categories stricter with `refineCategory`, add attributes and deprecate your own terms;
- register the extension with `ServiceLoader`, on the class path or the module path;
- apply the university extension shipped with Linneo and combine it with your own;
- predict which extensions the builder rejects, and read its error messages.

## When to extend

The standard taxonomy covers a lot, but every organisation has its own data. The university needs:

- a key for **laboratory notebooks** — nothing in `user.education.*` fits;
- a **retention** attribute on lab notebooks, which must be kept permanently;
- stricter protection for **government identifiers** across the whole university: its security policy rates
  disclosure of national identity numbers as severe (`HIGH`), not `MEDIUM`;
- a data subject for **visiting researchers**.

None of this needs a change to Linneo. A `TaxonomyExtension` contributes it to the taxonomy at build time.

Before extending, check the [standard taxonomy](../reference/standard-taxonomy.md): an existing key that is true for
the data is always better than a new one, because other tools and organisations already understand it. For a
university, also check the [university extension](../reference/extensions/university.md) shipped with Linneo (see
[below](#the-university-extension-shipped-with-linneo)): it already has keys for theses, attendance, disciplinary
records and much more.

## Writing the extension

```java
package edu.example.university;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyExtension;

public final class CampusTaxonomy implements TaxonomyExtension {

    @Override
    public String name() {
        return "campus";
    }

    @Override
    public void contribute(TaxonomyBuilder builder) {
        builder.category("user.education.lab_notebook", "Lab Notebook",
                        "Laboratory notebooks kept by the subject: experiments, measurements and results.",
                        c -> c.attribute("retention", "permanent"))
               .category("user.education.lab_journal", "Lab Journal", "Former name of the lab notebook key.",
                        c -> c.deprecated("2.0", "user.education.lab_notebook"))
               .refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
               .dataSubject("visiting_researcher", "Visiting Researcher", "A researcher from another institution.");
    }
}
```

### `name()`

A short, stable name. It is recorded as the **origin** of every term the extension adds, so reports can tell
"campus" terms from "fideslang" and "linneo" ones. Those two names are reserved: an extension called
`"fideslang"` or `"linneo"` (or with a blank name) is refused with
`Invalid extension name 'linneo' in edu.example.university.CampusTaxonomy`. Applying the same extension class twice
has no effect the second time, but two different classes with the same name are refused (see below). The name `"university"` is taken by the
[extension shipped with Linneo](#the-university-extension-shipped-with-linneo); do not reuse it.

### Adding categories

`category(key, name, description)` adds a category that **inherits all its protection** from its parent.
`category(key, name, description, customizer)` lets you declare things on a `CategoryDraft`:

| `CategoryDraft` method | Declares |
|---|---|
| `nature(LegalNature)` | the legal nature; must be admitted by the parent's (`LegalNature.admits`) |
| `confidentiality(Confidentiality)` | the confidentiality; never lower than the parent's |
| `integrity(Integrity)` | the integrity; never lower than the parent's |
| `secret()` | marks the category, and therefore all its children, as secret |
| `attribute(name, value)` | an application-defined attribute, inherited by children that do not set their own |
| `since(version)` | the version of your extension that introduced the term |
| `deprecated(sinceVersion, replacedBy)` | deprecates the term; `replacedBy` may be `null` |

With this extension, `user.education.lab_notebook` is:

```
nature=ORDINARY, confidentiality=MEDIUM, integrity=HIGH, secret=false,
attributes={retention=permanent}, origin=campus
```

The levels come from `user.education`; the attribute is the extension's. Any child you later add under
`user.education.lab_notebook` inherits `retention=permanent` unless it sets its own value. In module 5's inventory you can
print it with `field.category().attribute("retention")`.

The key must be lower-case segments separated by dots (`[a-z][a-z0-9_]*`), and its parent must exist. Add terms as
**children of existing terms**: that keeps them understandable to anyone who knows the standard tree.

### Making existing categories stricter

`refineCategory(key, customizer)` modifies a category that is already in the builder — standard or added by an
earlier extension. It can raise levels, change an `ORDINARY` nature into a stricter one, mark it secret, add
attributes or deprecate it. It can never weaken:

```java
.refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
```

After this, `user.government_id.passport_number`, `...national_identification_number`,
`...social_security_number` and every other child inherit `HIGH`. Refining does not change the term's origin:
`user.government_id` is still `fideslang`.

### Data uses and data subjects

`dataUse(key, name, description)` and `dataSubject(key, name, description)` (each with an optional `TermDraft`
customizer for `since` and `deprecated`) add terms to the other two hierarchies. They have no protection facets.

### Deprecating your own terms

Suppose the university used `user.education.lab_journal` in a first version and later renamed it to
`user.education.lab_notebook`. Never delete or reuse a key that applications may have persisted; deprecate it:

```java
.category("user.education.lab_journal", "Lab Journal", "Former name of the lab notebook key.",
        c -> c.deprecated("2.0", "user.education.lab_notebook"))
```

Fields still annotated with the old key keep working, are classified with `user.education.lab_notebook`, and
produce the warning you saw in module 4:

```
WARNING DEPRECATED_CATEGORY edu.example.university.LabNotebook.file: data category 'user.education.lab_journal' is deprecated; use 'user.education.lab_notebook'
```

### Constants for your keys

`Categories` only covers the standard taxonomy. Give your keys compile-time constants too, so they work in
annotations and typos do not compile:

```java
public final class CampusCategories {
    public static final String USER_EDUCATION_LAB_NOTEBOOK = "user.education.lab_notebook";

    private CampusCategories() {
    }
}
```

```java
@Category(CampusCategories.USER_EDUCATION_LAB_NOTEBOOK)
private String notebookTitle;
```

## Registering the extension

`Taxonomy.load()` applies every extension found by `java.util.ServiceLoader`. Register yours in one of two ways.

**Class path** — a file named `META-INF/services/io.github.devbpm.linneo.TaxonomyExtension` (for Maven, under
`src/main/resources`) containing the fully qualified class name:

```
edu.example.university.CampusTaxonomy
```

**Module path** — a `provides` clause in your `module-info.java`:

```java
module edu.example.university {
    requires io.github.devbpm.linneo;

    provides io.github.devbpm.linneo.TaxonomyExtension with edu.example.university.CampusTaxonomy;
}
```

Then, everywhere (application and tests):

```java
Taxonomy taxonomy = Taxonomy.load();   // standard + every registered extension
```

`load()` looks up extensions with the thread context class loader (or Linneo's own loader when there is none) and
applies them **sorted by name**, so the result does not depend on class path order. If you need another class
loader, use `Taxonomy.builder().withStandard().withExtensions(classLoader).build()`.

You can also apply an extension explicitly, without `ServiceLoader` — handy in a unit test of the extension itself:

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new CampusTaxonomy()).build();
```

## The university extension shipped with Linneo

Linneo ships one optional extension for higher education, `UniversityTaxonomy`, in the package
`io.github.devbpm.linneo.extension.university`. It adds 33 data categories (attendance, academic standing,
accommodations, admission applications and evaluations, final grades, credit recognition, degrees, internships,
means assessments, fee exemptions, donations…), 27 data uses (including two new roots, `advancement` and `research`)
and 13 data subjects (prospective student, doctoral candidate, exchange student, donor, referee…). The [reference](../reference/extensions/university.md) lists every term with its
protection.

It is **not registered** with `ServiceLoader`, so `Taxonomy.load()` never applies it. Apply it next to your own
extension:

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new UniversityTaxonomy())   // io.github.devbpm.linneo.extension.university
        .extend(new CampusTaxonomy())
        .build();
```

`taxonomy.toString()` prints `Taxonomy[160 data categories, 90 data uses, 35 data subjects]`: the 125 standard
categories, 33 from `university` and 2 from `campus`. Its keys have constants in `UniversityCategories`,
`UniversityDataUses` and `UniversityDataSubjects`, so they work in annotations like the standard ones:

```java
@Category(UniversityCategories.USER_EDUCATION_ATTENDANCE)
private int absences;
```

Two extensions combine safely when:

- **their names differ.** The name is the origin of each term (`university`, `campus`) and the identity of the
  extension: `extend` throws a `TaxonomyException` when a different class with the name of one already applied
  arrives: `The extensions io.github.devbpm.linneo.extension.university.UniversityTaxonomy and
  edu.example.university.CampusTaxonomy are both named 'university'`. (Applying the very same class twice is
  harmless, which is why an extension can be applied explicitly and also registered as a service.)
- **they add different keys.** Adding a key the other extension already added fails with
  `Duplicate key 'user.education.thesis'`. That is why `CampusTaxonomy` adds lab notebooks and not theses.
- **refinements come after the term they refine.** Your extension can make a term of `UniversityTaxonomy` stricter
  only if it is applied after it; otherwise `refineCategory` fails with
  `Cannot refine unknown data category 'user.education.attendance'`. With `extend` you choose the order. `load()`
  sorts by name, so to apply the shipped extension first and then every registered one, write
  `Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).withExtensions().build()`.

To have `Taxonomy.load()` include the shipped extension, register it in your application; the
[Extensions](../reference/extensions/README.md#registering-a-shipped-extension) reference shows how on the class path
and on the module path.

## What `build()` rejects

The builder is where the rules of module 2 are enforced. Some problems are detected as soon as you call a builder
method, the rest when the taxonomy is built; either way `Taxonomy.load()` throws a `TaxonomyException`, so a broken
extension stops the application (or the test) instead of producing a weaker taxonomy. The messages are:

| Problem | Example message | When |
|---|---|---|
| Invalid key | `Invalid key 'user.education.LabNotebook': use lower-case segments separated by dots` | on `category(...)` |
| Duplicate key | `Duplicate key 'user.education.grade'` | on `category(...)` |
| Refining a key that is not there (yet) | `Cannot refine unknown data category 'user.education.lab_notebook'` | on `refineCategory(...)` |
| Refine lowers a level the category declares | `Cannot lower the confidentiality of 'user.education' from MEDIUM to LOW` | on `refineCategory(...)` |
| Refine changes a nature to one it does not admit | `Cannot change the nature of 'user.health_and_medical' from SPECIAL_CATEGORY to ORDINARY` | on `refineCategory(...)` |
| Missing parent | `The parent 'user.education.lab_notebook' of data category 'user.education.lab_notebook.entry' does not exist` | `build()` |
| Root without all facets | `Root data category 'research' must declare its nature, confidentiality and integrity` | `build()` |
| Nature not admitted by the parent | `Data category 'user.education.stats' declares nature NONE, which its parent 'user.education' (ORDINARY) does not admit` | `build()` |
| Level lower than the parent's | `Data category 'user.education.grade' declares confidentiality LOW, lower than its parent's MEDIUM` | `build()` |
| Replacement does not exist | `The replacement 'user.education.new' of data category 'user.education.old' does not exist` | `build()` |
| Replacement protects less | `The replacement 'user.education.new' of data category 'user.education.old' has weaker protection: ORDINARY, LOW, LOW instead of ORDINARY, MEDIUM, HIGH` | `build()` |
| Replacements form a cycle | `The replacements of data category 'user.education.a' form a cycle` | `build()` |

Two cases deserve attention:

- **Refining a category that only inherits a level.** `user.education.grade` declares no confidentiality of its
  own, so `refineCategory(USER_EDUCATION_GRADE, c -> c.confidentiality(LOW))` is accepted by `refineCategory`, but
  `build()` then compares it with the parent and fails ("declares confidentiality LOW, lower than its parent's
  MEDIUM"). Either way, it cannot be lowered.
- **Raising a parent above what a child declares.** `refineCategory(USER_DEMOGRAPHIC, c ->
  c.confidentiality(HIGH))` fails at build time with `Data category 'user.demographic.date_of_birth' declares
  confidentiality MEDIUM, lower than its parent's HIGH`, because several children of `user.demographic` declare
  `MEDIUM` explicitly. Refine those children as well, or refine only the specific keys you care about.

Order matters for `refineCategory`: the key must already be in the builder. The standard terms always are;
terms of another extension are only there if that extension's name sorts before yours.

## Common mistakes

- **Forgetting to register the extension** (or registering it only in the test resources). The application then
  fails with `UNKNOWN_CATEGORY`, or worse, uses `Taxonomy.standard()` and never sees the stricter levels.
- **Using `Taxonomy.standard()`** anywhere the extension is needed: it never includes extensions.
- **Trying to lower a level** for the whole organisation. Not possible by design; levels only become stricter.
- **Inventing new roots.** Add children of existing terms. (The builder does accept a new root that declares
  nature, confidentiality and integrity, but nobody else will understand it.)
- **Renaming a key** instead of deprecating it with a replacement.
- **Using `"linneo"` or `"fideslang"` as the extension name**, or `"university"`, which the shipped extension uses.
- **Adding a key the shipped university extension already has** instead of applying it.

## Exercises

**Exercise 6.1 — Library loans.** Add `user.education.library_loan` ("Library Loan", "Books and other materials the
subject has borrowed from the library.") to `CampusTaxonomy` without declaring any facet. What are its effective nature, confidentiality,
integrity and origin? Check with `requireCategory`.

**Exercise 6.2 — Postal addresses.** The university decides that postal addresses of students and staff are
`MEDIUM` confidentiality. Add the refinement. Will `build()` succeed? Which keys change?

**Exercise 6.3 — Predict.** For each extension body, predict whether `Taxonomy.builder().withStandard().extend(...)
.build()` succeeds and, if not, the message:

1. `builder.category("user.education.lab_notebook.entry", "Entry", "An entry of a lab notebook.");` (alone)
2. `builder.category("user.education.special_needs", "Special Needs", "Adaptations for disabled students.", c -> c.nature(LegalNature.SPECIAL_CATEGORY));`
3. `builder.refineCategory(Categories.USER_HEALTH_AND_MEDICAL, c -> c.nature(LegalNature.ORDINARY));`
4. `builder.refineCategory(Categories.USER_EDUCATION_TEST_SCORE, CategoryDraft::secret);`

**Exercise 6.4 — Register and test.** Register `CampusTaxonomy` with `ServiceLoader` and write a test that
asserts that `Taxonomy.load()` contains `user.education.lab_notebook` with origin `"campus"` and that
`user.government_id.passport_number` has confidentiality `HIGH`.

**Exercise 6.5 — Combine.** Build a taxonomy with the standard terms, the shipped `UniversityTaxonomy` and then
`CampusTaxonomy`.

1. What does `toString()` print?
2. What is the origin of `user.education.thesis`, and of `user.education.lab_notebook`?
3. What happens if `CampusTaxonomy` also adds `user.education.thesis`?
4. And if `CampusTaxonomy.name()` returned `"university"`?

## Self-check quiz

1. What is recorded as the origin of a term added by an extension?
2. Does `refineCategory` change a term's origin?
3. In which order does `Taxonomy.load()` apply extensions?
4. Can an extension make `user.contact.email` `PUBLIC`?
5. Where do you put the `ServiceLoader` registration on the class path?
6. Does `Taxonomy.load()` include the university extension shipped with Linneo?

## Further reading

- [Reference — Extending the taxonomy](../reference/extending.md)
- [Reference — Extensions](../reference/extensions/README.md) and the
  [university extension](../reference/extensions/university.md)
- [Reference — Taxonomy](../reference/taxonomy.md)
- [Reference — Standard taxonomy](../reference/standard-taxonomy.md)

---

← Previous: [Module 5 — Using the classification](05-using-the-classification.md) · Next:
[Module 7 — OpenMetadata](07-openmetadata.md) →

<details markdown="block">
<summary><strong>Solutions</strong></summary>

**Exercise 6.1**

```java
builder.category("user.education.library_loan", "Library Loan",
        "Books and other materials the subject has borrowed from the library.");
```

Effective: `ORDINARY`, `MEDIUM`, `HIGH` (from `user.education`), not secret, origin `"campus"`.

**Exercise 6.2**

```java
.refineCategory(Categories.USER_CONTACT_ADDRESS, c -> c.confidentiality(Confidentiality.MEDIUM))
```

It builds: `user.contact.address` and its children (`city`, `country`, `postal_code`, `state`, `street`) declare no
confidentiality, so they all inherit `MEDIUM`. `user.contact.email` and the other contact keys are unchanged.

**Exercise 6.3**

1. Fails at build: `The parent 'user.education.lab_notebook' of data category 'user.education.lab_notebook.entry'
   does not exist` (unless `user.education.lab_notebook` is also added).
2. Fails at build: `ORDINARY` admits `SPECIAL_CATEGORY`, but special category data must have maximum protection and
   the key inherits `MEDIUM` confidentiality from `user.education`: `Data category 'user.education.special_needs' is
   SPECIAL_CATEGORY data and must have HIGH confidentiality and integrity, not MEDIUM and HIGH`. Declaring
   `c.nature(LegalNature.SPECIAL_CATEGORY).confidentiality(Confidentiality.HIGH)` makes it succeed.
3. Fails on `refineCategory`: `Cannot change the nature of 'user.health_and_medical' from SPECIAL_CATEGORY to
   ORDINARY`.
4. Succeeds: test scores become secret (an odd choice, but stricter, so allowed).

**Exercise 6.4**

`src/main/resources/META-INF/services/io.github.devbpm.linneo.TaxonomyExtension`:

```
edu.example.university.CampusTaxonomy
```

(or `provides io.github.devbpm.linneo.TaxonomyExtension with edu.example.university.CampusTaxonomy;` in
`module-info.java`), and:

```java
@Test
void campusExtensionIsLoaded() {
    Taxonomy taxonomy = Taxonomy.load();
    assertEquals("campus", taxonomy.requireCategory("user.education.lab_notebook").info().origin());
    assertEquals(Confidentiality.HIGH,
            taxonomy.requireCategory(Categories.USER_GOVERNMENT_ID_PASSPORT_NUMBER).confidentiality());
}
```

**Exercise 6.5**

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new UniversityTaxonomy())
        .extend(new CampusTaxonomy())
        .build();
```

1. `Taxonomy[160 data categories, 90 data uses, 35 data subjects]`: 125 + 33 + 2 categories, 63 + 27 uses and
   21 + 13 + 1 subjects. Applying the two extensions in the other order gives the same taxonomy.
2. `university` and `campus`.
3. `extend(new CampusTaxonomy())` throws `TaxonomyException` with `Duplicate key 'user.education.thesis'`.
4. `extend(new CampusTaxonomy())` throws `TaxonomyException` with `The extensions
   io.github.devbpm.linneo.extension.university.UniversityTaxonomy and edu.example.university.CampusTaxonomy are both
   named 'university'`: two different classes cannot share a name, and `CampusTaxonomy.contribute` never runs.

**Quiz**

1. The extension's `name()`.
2. No; it keeps the origin of whoever added the term.
3. Sorted by name.
4. No. `user.contact.email` inherits `LOW` from `user`; `PUBLIC` is lower, so `build()` fails.
5. In `META-INF/services/io.github.devbpm.linneo.TaxonomyExtension` (in Maven, under `src/main/resources`).
6. No. Linneo's module provides no extension as a service; apply it with `extend(new UniversityTaxonomy())` or
   register it in your application.

</details>
