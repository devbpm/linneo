---
title: "Data inventory"
parent: "Reference"
nav_order: 11.5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/DataInventory.java
  - src/main/java/io/github/devbpm/linneo/classify/InventoryField.java
  - src/main/java/io/github/devbpm/linneo/classify/InventoryDiff.java
  - src/main/java/io/github/devbpm/linneo/classify/FieldChange.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
  - tools/bench/InventoryBench.java
-->

# Data inventory

`io.github.devbpm.linneo.classify.DataInventory` — `public final class`, `@since 0.1.0`

`io.github.devbpm.linneo.classify.InventoryField` — `public record`, `@since 0.1.0`

`io.github.devbpm.linneo.classify.InventoryDiff` — `public final class`, `@since 0.1.0`

`io.github.devbpm.linneo.classify.FieldChange` — `public record`, `@since 0.1.0`

The classification of a model as **data**: for each object, identified by a key the application chooses, the fields it
holds with what the code declares for them. It holds no classes and no taxonomy, so an application can store it, read
it back and compare it. Applications use it to keep their own records of the classification, such as a field table or
a data catalog, in step with the code:

- [`Classifier.inventory`](classifier.md#inventory) builds the inventory of the code;
- [`DataInventory.builder()`](#builder) (or [`fromTsv`](#text-format)) builds the inventory of what the application
  stored;
- [`InventoryDiff.between`](#inventorydiff) compares them and says what to insert, retire or update, and which changes
  weaken protection;
- the [fingerprints](#fingerprints) tell whether anything changed without comparing field by field.

Instances of the four types are immutable.

## What it records

The inventory records what the code **declares**, never what the taxonomy gives a field. A classified field keeps:

- its data category **key as written**, even a deprecated one (it is not replaced by its successor);
- the **basis**: where the category comes from ([`Basis`](results.md#basis));
- the `confidentiality` and `integrity` the field raises explicitly with `@Category`, or `null` when it takes the
  category's;
- the **identifiability** as the field declares it (`PSEUDONYMIZED`, `ANONYMIZED`, `AGGREGATED`), or `null` when it
  declares none or `IDENTIFIED`, which a personal category takes anyway: equal declarations give equal fields;
- the **reason**: that of a class default, or the `declaredBy` of a per-row category.

A field excluded with [`@Exempt`](../annotations/exempt.md) is kept too, with only its reason, so that excluding a field
that used to be classified shows up as a change.

Because the taxonomy's levels are not copied into the inventory, **a change of the taxonomy never changes an
inventory**. Tighten a category and the inventories of the code and of the stored copy stay equal; what changes is the
protection you get when you resolve their fields against the taxonomy of the day, with
[`InventoryField.protection(taxonomy)`](#inventoryfield). [`affectedBy`](#changes-of-the-taxonomy) lists the fields a
taxonomy change reaches.

### Object keys

An object is any group of fields the application keeps a record for: a business object, a table, an entity. Its key is
**chosen by the application** and must be **stable**: stored records are matched by it. Do not use the class name: if
the class is renamed or moved, every field of the old name is a removed field and every field of the new one an added
field. Take the key from what already identifies the object in the application, such as its business object or table
name.

## DataInventory

| Member | Returns | Description |
|---|---|---|
| `static Builder builder()` | `Builder` | Starts an inventory, typically from what an application stored. |
| `Set<String> objects()` | set | The keys of the objects, sorted. |
| `List<InventoryField> fields()` | immutable list | All the fields, exempted ones included, by object and then by path. |
| `List<InventoryField> fields(String object)` | immutable list | The fields of an object by path; empty for an unknown object. |
| `Optional<InventoryField> field(String object, String path)` | `Optional` | One field, or empty if the inventory does not hold it. |
| `List<InventoryField> fieldsWith(String key)` | immutable list | The classified fields that declare exactly that data category key, by object and then by path. |
| `Map<TermChange, List<InventoryField>> affectedBy(TaxonomyDiff diff)` | immutable map | The fields each change of a taxonomy reaches; see [Changes of the taxonomy](#changes-of-the-taxonomy). |
| `String fingerprint()` | `String` | The fingerprint of the whole inventory; see [Fingerprints](#fingerprints). |
| `String fingerprint(String object)` | `String` | The fingerprint of one object. `IllegalArgumentException` if the inventory does not hold it. |
| `String toTsv()` | `String` | The inventory as tab-separated text, deterministic; see [Text format](#text-format). |
| `static DataInventory fromTsv(String text)` | `DataInventory` | Reads a text written by `toTsv()`. `IllegalArgumentException` if the text is not an inventory of a known format version, or a row is malformed. |
| `boolean equals(Object)`, `int hashCode()` | | Two inventories are equal when they have the same objects and the same fields. |

`toString()` reports the size, for example `DataInventory[2 objects, 7 fields]`.

### Builder

`DataInventory.Builder` — `public static final class`, `@since 0.1.0`. It is how an application turns the rows it
stored into an inventory. It has no public constructor; use `DataInventory.builder()`.

| Method | Effect |
|---|---|
| `Builder object(String key)` | Adds an object, even one without fields. `NullPointerException` if `key` is `null`; `IllegalArgumentException` if it is blank. |
| `Builder field(InventoryField field)` | Adds a field, and its object if it is new. `IllegalArgumentException` if the object already has a field with that path. |
| `Builder field(String object, String path, String key, Basis basis)` | Adds a classified field that takes the levels of its category: no raised levels, no identifiability and no reason. |
| `Builder exempt(String object, String path, String reason)` | Adds a field excluded from classification. |
| `DataInventory build()` | Creates the inventory. The builder can be kept and used again. |

```java
import io.github.devbpm.linneo.classify.FieldClassification.Basis;

DataInventory stored = DataInventory.builder()
        .field("student", "email", Categories.USER_CONTACT_EMAIL, Basis.DECLARED)
        .field(new InventoryField("student", "version", Categories.SYSTEM_OPERATIONS, Basis.CLASS_DEFAULT,
                null, null, null, "technical columns"))
        .exempt("student", "card", "cache of the rendered card")
        .build();
```

(`Basis` is `FieldClassification.Basis`.) A personal field built without an identifiability is read as `IDENTIFIED`, as
the classifier does.

## InventoryField

```java
public record InventoryField(String object, String path, String key, Basis basis, Confidentiality confidentiality,
                             Integrity integrity, Identifiability identifiability, String reason)
```

One field of an inventory: what the code declares for it, independent of any taxonomy, so that it can be stored,
compared and resolved later against the taxonomy of the day.

| Component | Nullable | Description |
|---|---|---|
| `object` | no | The key of the object the field belongs to, chosen by the application. |
| `path` | no | The field path within the object: its name, or `field.part` for a field of a [`@Composite`](../annotations/composite.md). |
| `key` | yes | The data category key as declared, possibly deprecated; `null` for an exempted field. |
| `basis` | yes | Where the category comes from: `DECLARED`, `CLASS_DEFAULT`, `BY_ROW` or `DERIVED`; `null` for an exempted field. |
| `confidentiality` | yes | The confidentiality the field declares above its category's, or `null` when it takes the category's. |
| `integrity` | yes | The integrity the field declares above its category's, or `null` when it takes the category's. |
| `identifiability` | yes | How the value points to the person, as the field declares it; `null` when it declares none, and read as `IDENTIFIED` for a personal category. `IDENTIFIED` itself is stored as `null`, so that equal declarations give equal fields. |
| `reason` | yes | The reason of an exemption or of a class default, or what decides a per-row category (`declaredBy`); `null` otherwise. A blank reason of a classified field is stored as `null`. |

The canonical constructor throws `NullPointerException` if `object` or `path` is `null`, and `IllegalArgumentException`
if either is blank, if only one of `key` and `basis` is given, or if an exempted field (no key) has levels, an
identifiability or no reason.

| Member | Description |
|---|---|
| `static InventoryField exempt(String object, String path, String reason)` | A field excluded from classification. |
| `boolean isExempt()` | `true` if the field has no key. |
| `DataCategory category(Taxonomy taxonomy)` | The current data category of the field: its key, or the replacement of a deprecated key. `TaxonomyException` if the field is exempted or the taxonomy does not contain its key. |
| `boolean isPersonal(Taxonomy taxonomy)` | Whether the stored values are personal data, as [`FieldClassification.isPersonal()`](results.md#methods) tells: a personal category that is not declared `ANONYMIZED` or `AGGREGATED`. `false` for an exempted field. `TaxonomyException` if the taxonomy does not contain the key. |
| `Identifiability identifiability(Taxonomy taxonomy)` | How the stored values point to the person, as `FieldClassification.identifiability()` gives it: the declared identifiability, or `IDENTIFIED` when the field declares none. `null` for an exempted field or a category that is not personal. `TaxonomyException` if the taxonomy does not contain the key. |
| `Protection protection(Taxonomy taxonomy)` | The [protection](../protection-levels.md#protection) the field requires in the taxonomy, as `FieldClassification.protection()` computes it; `Protection.LOWEST` for an exempted field. `TaxonomyException` if the taxonomy does not contain the key. |

`toString()` is `<object>.<path> = <key> (<BASIS>)`, or `<object>.<path> exempt: <reason>`.

Resolved against the taxonomy, an inventory field gives what the classifier gives the same field: for every field of a
class, `protection(taxonomy)`, `isPersonal(taxonomy)`, `identifiability(taxonomy)` and `category(taxonomy)` equal those
of its `FieldClassification`.

## Building it from the code

[`Classifier.inventory(types, objectKey)`](classifier.md#inventory) verifies the classes, so it fails with a
`ClassificationException` if any has an error, and records every field of every class under the key
`objectKey` gives for it. A class listed twice counts once; a null or blank key, or the same key for two classes, is an
`IllegalArgumentException`.

```java
Map<Class<?>, String> objectKeys = Map.of(Student.class, "student", Setting.class, "setting");
DataInventory declared = new Classifier(taxonomy).inventory(List.of(Student.class, Setting.class), objectKeys::get);
```

What each field records, by how the classifier resolved it:

| Basis | `key` | Levels | `reason` |
|---|---|---|---|
| `DECLARED` | the `value` of `@Category` | those `@Category` sets explicitly | `null` |
| `CLASS_DEFAULT` | the `value` of `@DefaultCategory` | `null` | the default's `reason` |
| `BY_ROW` | the `ceiling` of `@CategoryByRow` | `null` | `declaredBy` |
| `DERIVED` | the key of its own `@Category`, or that of its source | those of its own `@Category`, or those its source declares | `null` |

An exempted field is recorded with its reason from `@Exempt`. The fields of a `@Composite` have the path `field.part`.

## Text format

`toTsv()` writes the inventory as text and `fromTsv` reads it back, so an application can keep it in a file under
version control, where a review shows every change of the classification, or exchange it with another system.
`DataInventory.fromTsv(inventory.toTsv())` is equal to `inventory`.

The first line is `#linneo-inventory`, a tab and the format version, `1`; `fromTsv` refuses any other first line.
Blank lines and lines starting with `#` after it are comments; `toTsv` writes three that name the rows. Every other
line is a row of cells separated by tabs, and the first cell says which row it is:

| Row | Cells |
|---|---|
| `object` | `object`, key: 2 cells. It declares an object, so an object without fields is kept |
| `field` | `field`, object, path, key, basis, confidentiality, integrity, identifiability, reason: 9 cells. The confidentiality, integrity, identifiability and reason may be empty |
| `exempt` | `exempt`, object, path, reason: 4 cells |

Each object is a row `object` followed by its `field` and `exempt` rows by path, and the objects are by key, so the
same inventory always gives the same text. An empty cell is an absent value. A backslash, tab, line feed and carriage
return inside a cell are written `\\`, `\t`, `\n` and `\r`, as in the
[text of a taxonomy](../taxonomy.md#text-format), so a row is always one line; a lone UTF-16 surrogate, which UTF-8
cannot encode, is written as a backslash, `u` and its four hexadecimal digits, so two different inventories never have
the same bytes or fingerprint. The basis, confidentiality, integrity and
identifiability are the names of their constants. `fromTsv` throws `IllegalArgumentException` for another first line,
a row with another number of cells, an unknown kind of row or constant, an empty cell that must have a value, or two
rows for the same field.

For `Student` with a personal `email` that raises its confidentiality, a class default for the rest and an exempted
`card`, with `→` standing for a tab:

```text
#linneo-inventory→1
# object key
# field object path key basis confidentiality integrity identifiability reason
# exempt object path reason
object→student
exempt→student→card→cache of the rendered card
field→student→email→user.contact.email→DECLARED→HIGH→→→
field→student→version→system.operations→CLASS_DEFAULT→→→→technical columns
```

## Fingerprints

| Method | Description |
|---|---|
| `fingerprint()` | The SHA-256 of `toTsv()` (UTF-8), as 64 lower-case hexadecimal digits. Equal inventories have the same fingerprint; it is computed once and kept. |
| `fingerprint(String object)` | The SHA-256 of the text of that object alone: the first line and the rows of the object, as 64 lower-case hexadecimal digits. It changes only when that object changes. |

A fingerprint says that something changed, not what. Keep the fingerprint of what you stored: at the start, if the
fingerprint of the code is the same, there is nothing to reconcile and the comparison can be skipped; if it differs,
compare only the objects whose own fingerprint moved.

## InventoryDiff

The differences between two inventories, field by field, such as what an application stored and what its code declares
now. It is the plan of a reconciliation: Linneo compares, the application applies.

| Member | Returns | Description |
|---|---|---|
| `static InventoryDiff between(DataInventory before, DataInventory after, Taxonomy taxonomy)` | `InventoryDiff` | Compares two inventories: `before` is the earlier one, such as what the application stored, and `after` the later one, such as what its code declares now. The `taxonomy` resolves the keys to tell how protection changed. `NullPointerException` if an argument is `null`. |
| `List<FieldChange> changes()` | immutable list | The field changes, by object and then by path. A field that did not change is not listed. |
| `List<FieldChange> weakening()` | immutable list | The changes that [`weaken`](#fieldchange) protection and deserve a review. |
| `List<String> addedObjects()` | immutable list | The objects only `after` holds, sorted. |
| `List<String> removedObjects()` | immutable list | The objects only `before` holds, sorted. |
| `Set<String> changedObjects()` | set | The objects with any change, added and removed ones included, sorted: the ones to reconcile. |
| `boolean isEmpty()` | `boolean` | `true` if no object or field differs: the inventories are equal. |

`toString()` reports the size, for example `InventoryDiff[3 field change(s), 2 weakening]`.

Both inventories are compared **under the same taxonomy**, so the changes come from the code; the changes that come from
the taxonomy are found with [`affectedBy`](#changes-of-the-taxonomy). The fields of an added object are `ADDED` changes
and those of a removed object `REMOVED` changes, besides the object being in `addedObjects()` or `removedObjects()`.

### FieldChange

```java
public record FieldChange(String object, String path, Kind kind, InventoryField before, InventoryField after,
                          Direction direction)
```

How one field differs between the two inventories.

| Component | Nullable | Description |
|---|---|---|
| `object` | no | The object key. |
| `path` | no | The field path. |
| `kind` | no | Whether the field was added, removed or changed (see below). |
| `before` | yes | The field in the earlier inventory; `null` if it was added. |
| `after` | yes | The field in the later inventory; `null` if it was removed. |
| `direction` | no | How the protection of the field moved, with its identifiability: a [`Protection.Direction`](../protection-levels.md#direction). |

The canonical constructor throws `NullPointerException` if `object`, `path`, `kind` or `direction` is `null`, and
`IllegalArgumentException` if `before` and `after` do not match the kind (`before` is `null` exactly for `ADDED`,
`after` exactly for `REMOVED`).

| Method | Description |
|---|---|
| `boolean weakens()` | Whether the change lowers some protection and deserves a review: `direction().weakens()`, that is, `LOOSENED` or `MIXED`. It includes a classified field that became exempt and a value declared anonymized. |

`toString()` is `<KIND> <object>.<path>`, then the direction when it is not `SAME`, then `: <after>` or, for a removed
field, `: was <before>`; for example `REMOVED student.fax: was student.fax = user.contact.phone_number (DECLARED)`.

`FieldChange.Kind` — `public enum`, `@since 0.1.0`:

| Constant | Meaning |
|---|---|
| `ADDED` | The field is new in the later inventory. A new personal field means new personal data to record. |
| `REMOVED` | The field is no longer in the later inventory, not even as exempted. |
| `CHANGED` | The field is in both and something it declares changed, including becoming exempt or no longer being exempt. |

### Direction

`direction` is computed for `CHANGED` fields by resolving both sides against the taxonomy given to `between`:

| Situation | `direction` |
|---|---|
| The field was `ADDED` or `REMOVED` | `SAME` |
| `CHANGED`, with the same protection and identifiability, for example a different basis or reason | `SAME` |
| The later protection covers the earlier one: stricter in at least one facet, weaker in none | `TIGHTENED` |
| The earlier protection covers the later one | `LOOSENED` |
| Stricter in one facet and weaker in another, or natures that cannot be compared | `MIXED` |
| The key of either side is not in the taxonomy | `MIXED` |

The protection is that of [`InventoryField.protection(taxonomy)`](#inventoryfield), compared with
[`Protection.comparedTo`](../protection-levels.md#protection). A classified field that becomes exempt is `LOOSENED`
(unless it already required the lowest protection), and an exempted one that becomes classified is `TIGHTENED`; a
personal value declared `ANONYMIZED` or `AGGREGATED` loses its legal nature and is `LOOSENED`.

When the protection is personal on both sides, the **identifiability** is compared as well, with `PSEUDONYMIZED` below
`IDENTIFIED`, and the two directions are combined with [`Direction.and`](../protection-levels.md#direction): a `SAME`
yields the other, equal directions stay, and any other pair is `MIXED`. Pseudonymizing a field is therefore `LOOSENED`,
identifying it again is `TIGHTENED`, and pseudonymizing a field while raising its confidentiality is `MIXED`.

## Changes of the taxonomy

`affectedBy(TaxonomyDiff diff)` answers the other question: the code did not change but the taxonomy did. For each
changed **data category** in the [`TaxonomyDiff`](../taxonomy-diff.md) that at least one field declares, it returns the
change with the fields that declare its key, in the order of the diff, and with the fields whose **deprecated key is
replaced** by it in either version, since such a field takes the protection of the replacement (exempted fields are
never returned). Data uses and data subjects are not in an inventory, so their changes are never returned.

An application uses it to migrate the keys it stores when a category is deprecated, and to record **one entry per
changed category**, with the fields it affects, instead of one entry per field. The inventory itself does not change:
the field keeps the key it declared.

```java
Taxonomy applied = Taxonomy.fromTsv(Files.readString(appliedFile));       // the taxonomy applied last
TaxonomyDiff taxonomyDiff = TaxonomyDiff.between(applied, taxonomy);
for (Map.Entry<TermChange, List<InventoryField>> entry : declared.affectedBy(taxonomyDiff).entrySet()) {
    TermChange change = entry.getKey();                   // one audit entry for the category
    for (InventoryField field : entry.getValue()) {
        // field.object(), field.path(): the fields that declare change.key()
    }
}
```

## Using it

Reconcile a stored inventory with the code at start:

```java
static void reconcile(Classifier classifier, Taxonomy taxonomy, List<Class<?>> entities,
                      Map<Class<?>, String> objectKeys, DataInventory stored) {
    DataInventory declared = classifier.inventory(entities, objectKeys::get);   // ClassificationException if any error
    if (declared.fingerprint().equals(stored.fingerprint())) {
        return;                                                // nothing changed
    }
    InventoryDiff diff = InventoryDiff.between(stored, declared, taxonomy);
    for (FieldChange change : diff.changes()) {
        switch (change.kind()) {
            case ADDED -> { /* insert change.after() */ }
            case REMOVED -> { /* retire change.before(), do not delete it */ }
            case CHANGED -> { /* rewrite the row with change.after() */ }
        }
    }
    for (FieldChange change : diff.weakening()) {
        // ask for a review before applying: protection went down, or became mixed
    }
}
```

After applying the changes, keep `declared.fingerprint()` (and, to limit the next comparison to the objects that moved,
`declared.fingerprint(object)` for each object) next to the stored rows. `diff.changedObjects()` lists the objects to
reconcile now.

## What Linneo does and what the application does

Linneo **compares**; the application **reads, writes, locks and audits**. The inventory types do not touch a file or a
database, do not lock anything while you reconcile, keep no history and do not decide what a review accepts. Your
application chooses the object keys, stores the inventory (a table, a file, a catalog), applies the plan of
`InventoryDiff` in a transaction, prevents two nodes from reconciling at once, and records who approved each weakening
change.

## Performance

`tools/bench/InventoryBench.java` generates a large model, compiles it and times each step; it is not part of the
build (see [Contributing](https://github.com/devbpm/linneo/blob/main/CONTRIBUTING.md#performance)). On a developer
laptop, for 800 entities and 39 000 fields:

| Step | Time |
|---|---|
| first `verify` | about 0.3 s |
| `inventory` | about 0.2 s |
| `fingerprint` | about 0.1 s |
| `InventoryDiff.between`, with 1% of the fields changed | about 0.08 s |
| `affectedBy` | about 0.02 s |

Timings depend on the machine; run the tool on yours.

See also: [Classifier](classifier.md) · [Results](results.md) · [Violations](violations.md) ·
[Taxonomy diff](../taxonomy-diff.md) · [Protection levels](../protection-levels.md#direction) ·
[ProcessingChecker](../processing/checker.md) · course:
[Using the classification](../../course/05-using-the-classification.md)
