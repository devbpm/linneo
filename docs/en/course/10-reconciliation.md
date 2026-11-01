---
title: "10. Keeping stored records in step"
parent: "Course"
nav_order: 10
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/DataInventory.java
  - src/main/java/io/github/devbpm/linneo/classify/InventoryField.java
  - src/main/java/io/github/devbpm/linneo/classify/InventoryDiff.java
  - src/main/java/io/github/devbpm/linneo/classify/FieldChange.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - src/main/java/io/github/devbpm/linneo/Taxonomy.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyDiff.java
  - src/main/java/io/github/devbpm/linneo/TermChange.java
  - src/main/java/io/github/devbpm/linneo/Protection.java
  - src/main/java/io/github/devbpm/linneo/processing/Governance.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingChecker.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingRecord.java
  - src/main/java/io/github/devbpm/linneo/processing/RecordDiff.java
  - src/main/java/io/github/devbpm/linneo/processing/ActivityChange.java
  - src/main/java/io/github/devbpm/linneo/catalog/openmetadata/OpenMetadataCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/datahub/DataHubCatalog.java
  - src/main/java/io/github/devbpm/linneo/catalog/atlas/AtlasCatalog.java
-->

# Module 10 — Keeping stored records in step

## Learning objectives

After this module you will be able to:

- explain why an application keeps its own copy of the classification and how that copy drifts from the code;
- build the data inventory of a model with `Classifier.inventory` and choose object keys that survive refactoring;
- compare what is stored with what the code declares (`InventoryDiff`), apply the result and send every change that
  weakens protection to a review;
- keep the taxonomy the application applied and react to a new version of Linneo with `TaxonomyDiff`;
- keep the tags of a data catalog in step with the stored copy, column by column;
- check processing activities from stored records, with the governance the data protection officer keeps;
- tell what changed in the record of processing activities since the officer approved it (`RecordDiff`).

Modules 3, 4 and 9 are required.

## The copy that drifts

Modules 3 to 9 used the classification where it lives: in the code, read by the `Classifier` in a test or at run
time. Real applications also keep a **copy** of it. A screen lists the fields of each business object with their
category; a permission engine hides special category columns; a data catalog shows the tags of each column; the
record of processing activities is edited by the data protection officer in a back office. All of them read tables,
not annotations.

A copy drifts. A developer adds `Student.phone` with `@Category(USER_CONTACT_PHONE_NUMBER)`; the build passes,
because the classification is right, but the field table still lacks the row, and the screen that should mask the
phone shows it in clear. Seed files written by hand drift the same way.

The usual cure is a **reconciler**: at every start, the application compares the copy with the code and fixes the
copy. Linneo does not ship one, because a reconciler is mostly the application's business: its tables, its
transactions, its locks between nodes, its audit trail, the columns its administrators edit. What Linneo gives you is
the part that is the same in every application:

| Step | Who | With |
|---|---|---|
| Know what the code declares | Linneo | `Classifier.inventory(...)` |
| Read what is stored | the application | its own queries, then `DataInventory.builder()` |
| Compare, and tell how protection moved | Linneo | `InventoryDiff.between(...)`, `FieldChange.direction()` |
| Write the changes, in a transaction | the application | its own persistence |
| Audit, and ask for a review of weakening changes | the application | `InventoryDiff.weakening()` |
| Notice a new taxonomy, and what it reaches | Linneo | `TaxonomyDiff`, `DataInventory.affectedBy(...)` |

Three rules make reconcilers safe, and the API follows them:

1. **The code owns the classification.** Keys, levels raised by a field, identifiability: whatever the stored copy
   says, the next start rewrites it as the code says.
2. **Never delete, retire.** A field the code no longer declares keeps its row, marked retired, because permissions,
   audit entries and reports point at it. If the code declares it again, the row comes back as it was.
3. **Store keys, not levels.** A row stores `user.contact.email`, not "confidentiality LOW". The protection is
   resolved from the taxonomy when it is needed, so a stricter taxonomy protects old rows at once.

## The inventory of the code

```java
Classifier classifier = new Classifier(Taxonomy.load());
DataInventory declared = classifier.inventory(entities,
        type -> type.getAnnotation(jakarta.persistence.Table.class).name());
```

`inventory` classifies the classes, fails with a `ClassificationException` if any has an error, and returns one
**object** per class with its fields. The second argument gives each object its key, and it matters more than it
looks: stored rows are matched by `(object, path)`. A key taken from the class name turns a renamed or moved class
into a removed object and a new one, with every field retired and inserted again. Take it from something that
already identifies the object in your application and does not change with a refactoring: the table name, a
business object annotation, a registry.

Each `InventoryField` records what the code **declares**, not what a taxonomy makes of it:

| Component | `Student.email` with `@Category(value = USER_CONTACT_EMAIL, confidentiality = HIGH)` |
|---|---|
| `object`, `path` | `"students"`, `"email"` |
| `key` | `"user.contact.email"`, even if a later taxonomy deprecates it |
| `basis` | `DECLARED` (or `CLASS_DEFAULT`, `BY_ROW`, `DERIVED`) |
| `confidentiality`, `integrity` | `HIGH`, `null`: only the levels the field raises itself |
| `identifiability` | `IDENTIFIED` |
| `reason` | the reason of a class default or what decides a per-row category; `null` here |

A composite field appears once per part (`home.street`), a derived field with the key of its source, and an
exempted field as `InventoryField.exempt(object, path, reason)` with no key, so that exempting a field that used to
be classified is a visible change. `field.protection(taxonomy)` resolves the protection when you need it, exactly as
`FieldClassification.protection()` would.

## What is stored, and the plan

Read your table and build the stored inventory from it. Only the columns the code owns go in: labels, sort orders and
the "active" switch your administrators edit stay out, so the comparison never touches them.

```java
DataInventory.Builder stored = DataInventory.builder();
for (FieldRow row : fieldTable.activeRows()) {               // the application's own query, one per table
    stored.field(new InventoryField(row.object(), row.path(), row.categoryKey(), Basis.valueOf(row.basis()),
            row.raisedConfidentiality(), row.raisedIntegrity(), row.identifiability(), row.reason()));
}
InventoryDiff diff = InventoryDiff.between(stored.build(), declared, taxonomy);
```

`InventoryDiff` is the plan. Each `FieldChange` has a `kind`:

- `ADDED`: insert the row with the initial values your administrators will own later, or revive the retired row of
  the same object and path;
- `REMOVED`: retire the row;
- `CHANGED`: rewrite the code-owned columns. Becoming exempt, or no longer exempt, is a change too.

and a `direction` computed under the taxonomy you pass, with the `Protection` order of module 5 plus the
identifiability: `TIGHTENED`, `LOOSENED`, `MIXED` or `SAME`. `diff.weakening()` lists the changes that lower some
protection: a weaker category, a field that became exempt, a value now declared pseudonymized or anonymized. Apply
them like the others, the code is right by definition, and put each one in front of the data protection officer.
Added and removed fields have no direction; a new personal field is new personal data, which the record of
processing activities may need to mention.

```java
for (FieldChange change : diff.changes()) {
    switch (change.kind()) {
        case ADDED -> fieldTable.insertOrRevive(change.after());
        case REMOVED -> fieldTable.retire(change.object(), change.path());
        case CHANGED -> fieldTable.rewriteCodeColumns(change.after());
    }
}
diff.weakening().forEach(change -> audit.reviewNeeded(change.toString()));
```

### Skipping the work

Most starts change nothing. Store `declared.fingerprint()` next to your table; if it equals the stored one, skip the
comparison. When it differs, `declared.fingerprint(object)` per object tells which objects to read again: store one
fingerprint per object too, and reconcile only the objects whose fingerprint moved. The fingerprints are SHA-256
hashes of a deterministic text (`toTsv()`), so they do not depend on the JVM, the order of the classes or the order
of the fields.

The fingerprint only tells whether the **code** changed. Someone editing a code-owned column by hand is not seen until
the next comparison: run a full comparison on every new version of the application, or forbid those updates in the
database (column privileges that let the application user update only the administrator's columns).

## A new taxonomy

A new version of Linneo, or of your own `TaxonomyExtension`, may deprecate a key you stored or tighten a category.
None of that changes the inventory: the code still declares the same keys. Keep the taxonomy you applied, and compare:

```java
String applied = settings.read("linneo.taxonomy");             // what the last start stored
TaxonomyDiff changes = TaxonomyDiff.between(Taxonomy.fromTsv(applied), taxonomy);
changes.requireCompatible();                                  // a removed term or weaker protection stops here
for (var entry : declared.affectedBy(changes).entrySet()) {
    TermChange change = entry.getKey();                         // one audit entry per term, not per field
    if (change.aspects().contains(TermChange.Aspect.REPLACEMENT)) {
        fieldTable.migrateKey(change.key(), change.after().info().replacedBy());
    }
    audit.taxonomyChange(change, entry.getValue());
}
settings.write("linneo.taxonomy", taxonomy.toTsv());
```

`Taxonomy.toTsv()` writes every term with its effective protection, about 30 KB for the standard taxonomy and 60 KB
with the five shipped extensions; `fromTsv` reads it back through the `TaxonomyBuilder`, so a damaged text is refused.
The
same text lets an auditor reproduce, years later, the protection a field had on a given day. `TaxonomyDiff` also
grades the changes under Linneo's versioning rules: `requiredBump()` is `MAJOR` for a removed term, a weaker or mixed
protection, or a deprecated key that now resolves elsewhere.

Because the rows store keys, a tightened category needs no update at all: the next read resolves the stricter level.
That is rule 3 at work.

## Catalogs that follow the stored copy

A data catalog is one more copy: modules 7 and 8 put tags on each column. The tags of a column depend on two things,
the field and the taxonomy, so a column needs new tags when its field changed (`InventoryDiff`) or when the taxonomy
changed a category it uses (`affectedBy`). The catalogs accept a stored `InventoryField` as well as a
`FieldClassification`, and resolve it with their own taxonomy, so the tags a column had are those of the stored field
under the taxonomy you applied, and the tags it needs are those of the declared field under the current one:

```java
OpenMetadataCatalog was = OpenMetadataCatalog.of(Taxonomy.fromTsv(applied));  // as of the last start
OpenMetadataCatalog now = OpenMetadataCatalog.of(taxonomy);
for (FieldChange change : diff.changes()) {
    List<String> before = change.before() == null ? List.of() : was.tagsOf(change.before());
    List<String> after = change.after() == null ? List.of() : now.tagsOf(change.after());
    List<String> remove = new ArrayList<>(before);
    remove.removeAll(after);
    List<String> add = new ArrayList<>(after);
    add.removeAll(before);
    catalogClient.patchColumnTags(change.object(), change.path(), remove, add);   // one request per column
}
```

Do the same for every field `declared.affectedBy(changes)` lists, with the same field before and after. Three details
matter:

- **Remove and add in one request.** Most of these classifications are mutually exclusive in OpenMetadata
  (`Confidentiality.Low` and `Confidentiality.High` cannot coexist), so adding first is refused.
- **Leave the other tags alone.** Only the difference between the two Linneo lists is removed; the tags people add
  by hand never appear in either list.
- **An exempted field has no tags.** A field that became exempt loses every Linneo tag; one that was retired too
  (`after` is `null`).

`DataHubCatalog.termsOf(InventoryField)` and `AtlasCatalog.classificationsOf(InventoryField)` work the same way. The
catalog's definitions (the classifications and tags of the taxonomy itself) are idempotent requests: send them again
after a taxonomy change, before the columns.

## Processing activities from stored records

Module 9 declared activities in code. In many organisations the **scope** of an activity is known to the developers
(purposes, data subjects, the objects it reads) and its **governance** is decided by the data protection officer (the
lawful basis, the retention, the recipients). Linneo models the split:

```java
ProcessingActivity scope = ProcessingActivity.builder("enrollment", "Student enrollment")
        .purposes(DataUses.EDUCATION_ENROLLMENT)
        .dataSubjects(DataSubjects.STUDENT)
        .objects("students", "enrollments")                    // keys of the inventory
        .build();

Governance governance = governanceTable.read("enrollment");   // what the officer keeps
ProcessingChecker checker = new ProcessingChecker(taxonomy, Jurisdiction.SPAIN, declared);
CheckedActivity checked = checker.check(scope.withGovernance(governance));
checked.violations().forEach(v -> {
    if (v.element().isGovernance()) {
        form.error(v.element(), v.message());                   // shown where the officer edits it
    } else {
        throw new IllegalStateException(v.toString());          // a bug in the code
    }
});
```

`objects(...)` names objects of a data inventory instead of classes, so the same check runs in a back office that
has only the stored inventory and never sees the entity classes.

## The record the officer signed

The record of processing activities is a copy too, and the one with a signature on it. The officer approves a
version; afterwards a new field, a stricter taxonomy or an edit of the governance can change what it says. Keep the
`fingerprint()` of the record that was approved: if the current record has the same one, nothing changed, whatever
the order of the activities. If not, rebuild the approved record from what you stored when it was approved, the
inventory, the taxonomy and the governance, and compare:

```java
ProcessingRecord approved = new ProcessingChecker(Taxonomy.fromTsv(approvedTaxonomy), Jurisdiction.SPAIN,
        approvedInventory).verify(approvedActivities);
ProcessingRecord current = new ProcessingChecker(taxonomy, Jurisdiction.SPAIN, declared).verify(activities);
if (!current.fingerprint().equals(approval.fingerprint())) {
    for (ActivityChange change : RecordDiff.between(approved, current).changes()) {
        officer.review(change);                     // added, removed, or the aspects that changed
    }
}
```

Each `ActivityChange` names the activity and its `aspects`: `ADDED` or `REMOVED`, the scope (`PURPOSES`,
`DATA_SUBJECTS`), the data (`PERSONAL_DATA`, `PROTECTION`), each element of the governance (`LEGAL_BASIS`,
`RETENTION`, `RECIPIENTS`…) and the `OPEN_POINTS`. Two of its answers route the review:

- `isGovernance()`: only the governance changed (with the open points that follow from it), so the officer is
  looking at their own edit;
- `direction()`: `TIGHTENED` means the activity now processes data that needs more protection, the case where the
  lawful basis, an article 9 condition or an impact assessment may no longer be enough.

A change to the scope in code is better caught earlier, in the pull request: keep `record.toMarkdown(title)` under
version control the way the last section keeps the inventory.

## Performance

`tools/bench/InventoryBench.java` generates a model of 800 entities, 40 of them with 400 fields, 39 000 classified
fields in all, and times every step:

```text
java -cp target/classes tools/bench/InventoryBench.java
```

On a developer laptop the classification takes about 0.3 s the first time, the inventory 0.2 s, its fingerprint
0.1 s and the comparison with 1 % of the fields changed less than 0.1 s: Linneo's part of a reconciliation stays well
under a second and grows linearly with the number of fields. The time that matters is the application's:

- read each table with **one query that returns the code-owned columns**, not one query per object, and not as
  managed JPA entities, whose dirty checking on every automatic flush grows with the number of rows loaded;
- write only the rows that changed, in batches; with `IDENTITY` keys Hibernate cannot batch inserts, so the first
  start of a large model inserts tens of thousands of rows one by one;
- audit by cause: one entry per changed term from `affectedBy`, not one per field, and a summary for the first start;
- keep the transaction, and the lock that stops two nodes from reconciling at once, as short as that allows.

## A review that shows every classification change

The inventory as text is a good file to keep under version control: one line per field, sorted, with its key,
basis, raised levels and identifiability. A test can compare it with the code, so that every change to the
classification shows up in the pull request where a reviewer, or the data protection officer, sees it:

```java
@Test
void classificationIsReviewed() throws IOException {
    String current = classifier.inventory(ENTITIES, Entities::key).toTsv();
    String reviewed = Files.readString(Path.of("src/test/resources/inventory.tsv"));
    assertEquals(reviewed, current, "the classification changed: review it and update inventory.tsv");
}
```

## Common mistakes

- **Object keys from class names.** A refactoring then looks like the removal of a whole object.
- **Storing the protection levels in every row.** A stricter taxonomy then needs thousands of updates, and a missed
  update leaves a field less protected than it should be. Store the key and the levels the field raises itself.
- **Deleting the rows of removed fields.** Permissions and audit entries lose their meaning; retire them.
- **Putting the administrator's columns in the stored inventory.** The comparison would rewrite their edits at every
  start.
- **Ignoring `weakening()` because the code is right.** The code wins, but someone must confirm the weaker
  protection was intended.
- **Skipping on the fingerprint forever.** Hand edits of code-owned columns are only repaired by a full comparison.
- **Adding a column's new tags before removing the old ones.** Mutually exclusive classifications refuse the second
  level; send both in one request.
- **Comparing the current record with a record rebuilt from current data.** The diff is then empty by construction:
  rebuild the approved record from the inventory, taxonomy and governance stored when it was approved.

## Exercises

**Exercise 10.1 — Predict.** The stored inventory has `students.phone` with `user.contact.phone_number`. The code
changes the field to `@Exempt("legacy column, always empty")`. What `FieldChange` does the comparison report, with
which direction, and is it in `weakening()`?

**Exercise 10.2 — Keys.** Two entities, `legacy.Student` and `core.Student`, both use `Class::getSimpleName` as the
object key. What does `inventory` do, and how would you choose the keys instead?

**Exercise 10.3 — A new taxonomy.** The taxonomy the application applied had `user.contact.fax`; the new one
deprecates it with the replacement `user.contact.phone_number`. Which `TermChange` aspects appear, what does
`requiredBump()` return, and which stored rows must change?

**Exercise 10.4 — Golden file.** Add the test of the last section to the university model and make one
classification change. Read the failure message and the diff of `inventory.tsv`.

**Exercise 10.5 — A column's tags.** `students.notes` was stored with `user.content.private`; the code now declares
`user.health_and_medical`. With the default OpenMetadata names, which tags does the column lose and which does it
gain? Could you send them as two requests, additions first?

**Exercise 10.6 — The signed record.** The officer approved the record. Since then a developer added
`@Category(USER_HEALTH_AND_MEDICAL_DISABILITY)` to a field of an object the activity `enrollment` reads, and the
officer added the article 9 condition. Which aspects does the `ActivityChange` of `enrollment` have, what is its
direction, and is `isGovernance()` true?

## Self-check quiz

1. Which steps of a reconciliation does Linneo do, and which does the application do?
2. Why does the inventory keep the levels a field raises, but not the levels of its category?
3. What does `FieldChange.direction()` mean for an added field?
4. A category becomes stricter in a new taxonomy. Which of `InventoryDiff` and `affectedBy` reports it?
5. What does `requireCompatible()` refuse?
6. Why can a back office check processing activities without the entity classes?
7. Which two catalogs does a column change need, and why two?
8. When do two records have the same fingerprint?

## Further reading

- [Reference — Data inventory](../reference/classify/inventory.md)
- [Reference — The taxonomy as data](../reference/taxonomy.md)
- [Reference — Taxonomy diff](../reference/taxonomy-diff.md)
- [Reference — Protection levels](../reference/protection-levels.md)
- [Reference — ProcessingChecker](../reference/processing/checker.md)
- [Reference — The record of processing activities](../reference/processing/record.md)
- [Reference — OpenMetadata catalog](../reference/openmetadata/catalog.md)

---

← Previous: [Module 9 — Processing activities and the record](09-processing-activities.md) · Back to the
[course overview](README.md)

<details markdown="block">
<summary><strong>Solutions</strong></summary>

**Exercise 10.1.** One `CHANGED` change for `students.phone`: before, a classified field with
`user.contact.phone_number`; after, an exempted field. An exempted field has the lowest protection, so the direction
is `LOOSENED` and the change is in `weakening()`. Apply it (retire or rewrite the row as your application decides) and
ask for a review: an "always empty" column that still exists can be filled tomorrow.

**Exercise 10.2.** Both classes get the key `Student`, and `inventory` fails with an `IllegalArgumentException` that
names both classes. Take the key from the table name, a business object annotation or a registry: something that
names the object, not the class.

**Exercise 10.3.** The change of `user.contact.fax` has the aspects `DEPRECATION` and `REPLACEMENT` (and `VERSIONS`
only if the introducing version changed). Deprecating with a first replacement is compatible: `requiredBump()` is
`MINOR`. The rows that store `user.contact.fax` are the fields `affectedBy` lists for that change; migrate them to
`user.contact.phone_number`, or keep them and let the taxonomy resolve the replacement when it reads them.

**Exercise 10.4.** The failure message names the test, and the diff of the file shows the changed line, with the
old and new key, basis, levels or identifiability, which is what the reviewer needs.

**Exercise 10.5.** It loses `DataCategory.user.content.private`, `PersonalData.Personal`, `Confidentiality.Medium`
and `Integrity.Medium`, and gains `DataCategory.user.health_and_medical`, `PersonalData.SpecialCategory`,
`Confidentiality.High` and `Integrity.High`; `PII.Sensitive` and `Identifiability.Identified` stay. Not as two
requests with additions first: the personal data, confidentiality and integrity classifications are mutually
exclusive, so the server refuses a second level while the first is still on the column.

**Exercise 10.6.** `PERSONAL_DATA` (a new category), `PROTECTION` and `SPECIAL_CATEGORY_CONDITION`, plus
`OPEN_POINTS` if a warning appears, such as the impact assessment recommendation when no assessment is referenced. The
direction is `TIGHTENED`: the activity now processes special category data. `isGovernance()` is false, because the
data changed too; the officer's edit is part of the same change. Note the order of events: until the condition was
added, the current record could not be built at all, because special category data without an article 9 condition is
an error.

**Quiz.** 1: Linneo declares, compares and tells the direction; the application reads, writes, locks and audits.
2: the category's levels belong to the taxonomy, which can change without the code changing; keeping only the
field's own raises separates the two causes. 3: nothing, it is `SAME`; check `after().isPersonal(taxonomy)` for new
personal data. 4: `affectedBy`; `InventoryDiff` compares under one taxonomy and sees only code changes. 5: a removed
term, a weaker or mixed protection, and a deprecated key that resolves to another replacement than before. 6:
activities can name objects of a stored `DataInventory`, and the checker resolves them with the taxonomy. 7: one of
the taxonomy applied before, for the tags the column has, and one of the current taxonomy, for the tags it needs;
tags depend on the field and on the taxonomy. 8: exactly when `RecordDiff` finds no change between them, whatever the
order of their activities.

</details>
