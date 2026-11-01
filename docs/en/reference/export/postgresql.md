---
title: "PostgreSQL Anonymizer"
parent: "Export"
grand_parent: "Reference"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/export/postgresql/PostgresAnonymizer.java
  - src/main/java/io/github/devbpm/linneo/export/postgresql/package-info.java
-->

# PostgreSQL Anonymizer

`io.github.devbpm.linneo.export.postgresql.PostgresAnonymizer` — `public final class`, `@since 0.1.0`

The masking rules of [PostgreSQL Anonymizer](https://postgresql-anonymizer.readthedocs.io/) for the columns of
classified fields: the `SECURITY LABEL FOR anon` statements that make the database's masked views and anonymized dumps
follow a [`DisclosurePolicy`](../classify/disclosure.md), typically for the copies of production data that test
environments use. Linneo only writes the statements: the application chooses the column of each field and runs them,
and Linneo connects to no database. Instances are immutable and thread-safe.

## The rule of each treatment

The policy decides what happens to the value of each field ([`Treatment`](../classify/disclosure.md#treatment)), and the
anonymizer turns that into a rule:

| Treatment | Rule | Statement |
|---|---|---|
| `OMIT` | `MASKED WITH VALUE NULL` | `SECURITY LABEL FOR anon ON COLUMN <column> IS 'MASKED WITH VALUE NULL';` |
| `MASK` | `MASKED WITH FUNCTION <function>` when Linneo knows a function for the category or an ancestor; otherwise `MASKED WITH VALUE NULL` | `... IS 'MASKED WITH FUNCTION anon.dummy_safe_email()';` |
| `CLEAR` | none | `SECURITY LABEL FOR anon ON COLUMN <column> IS NULL;` |

- A masked field is replaced by a fake value of the same kind when a function is known for it. Otherwise it is also
  `NULL`: Linneo does not know the type of the column, and leaving the value out is the stricter choice. Declare the
  masks of your other columns with [`withMask`](#members).
- A field the policy shows as it is gets `IS NULL`, which **removes** any rule the column had. Running the statements
  again after a change of the classification, or of the policy, brings every column in line.
- A function is looked up by the key of the category, then by that of its parent, and so on up the tree: a mask
  declared for `user.education` serves `user.education.grade` unless a more specific key has its own.

## Members

| Member | Returns | Description |
|---|---|---|
| `static PostgresAnonymizer of(DisclosurePolicy policy)` | anonymizer | The rules that follow a policy, with Linneo's masking functions. `NullPointerException` if `policy` is `null`. |
| `PostgresAnonymizer withMask(String categoryKey, String function)` | new anonymizer | The same rules with a masking function for the fields of a category and of the categories below it, unless a more specific key has its own. `function` is the SQL that follows `MASKED WITH FUNCTION`, such as `anon.random_int_between(0, 10)` or `anon.partial(iban, 4, '******', 2)`; it is stripped. `IllegalArgumentException` if it is blank. |
| `Map<String, String> masks()` | immutable map | The masking functions by category key, sorted by key. |
| `String rule(FieldClassification field)` | `String` | The text of the label of a field: `MASKED WITH FUNCTION ...` or `MASKED WITH VALUE NULL`, or `null` for a value the policy shows as it is. |
| `String rule(InventoryField field, Taxonomy taxonomy)` | `String` | The same for a stored field, resolved with the taxonomy; `null` for an exempted field. `TaxonomyException` if the taxonomy does not contain the key. |
| `String label(String column, FieldClassification field)` | `String` | The `SECURITY LABEL FOR anon ON COLUMN ...` statement of a field. `IllegalArgumentException` if `column` is not a qualified SQL identifier. |
| `String label(String column, InventoryField field, Taxonomy taxonomy)` | `String` | The statement of a stored field. |
| `List<String> labels(ClassClassification classification, Function<String, String> columns)` | immutable list | The statements of every field of a class, in the order of the fields, followed by the exempted ones (`IS NULL`). `columns` gives the column of a field path; `null` leaves the field out. `IllegalArgumentException` if the class has classification errors, because some fields would have no rule, or a column is not a qualified identifier. |
| `List<String> labels(DataInventory inventory, String object, Taxonomy taxonomy, Function<String, String> columns)` | immutable list | The statements of every field of an object of a data inventory, exempted ones included, in the order of the inventory. |

`toString()` is `PostgresAnonymizer[<policy>, <n> masks]`.

### The column

A column is written `table.column` or `schema.table.column`, and each part is a plain identifier or a double-quoted one
(`"Students"."e-mail"`). Anything else, such as `email` alone or `students.email; DROP TABLE students`, is refused with
`IllegalArgumentException`, so that a statement cannot carry more than the label. Quotes inside the rule are doubled in
the statement.

### Standard masks

The functions Linneo knows are the `dummy_*` functions that version 2 of PostgreSQL Anonymizer recommends:

| Category | Function |
|---|---|
| `user.account.username` | `anon.dummy_username()` |
| `user.contact.address.city` | `anon.dummy_city_name()` |
| `user.contact.address.country` | `anon.dummy_country_name()` |
| `user.contact.address.postal_code` | `anon.dummy_post_code()` |
| `user.contact.address.state` | `anon.dummy_state_name()` |
| `user.contact.address.street` | `anon.dummy_street_name()` |
| `user.contact.email` | `anon.dummy_safe_email()` |
| `user.contact.phone_number` | `anon.dummy_phone_number()` |
| `user.device.ip_address` | `anon.dummy_ip()` |
| `user.name` | `anon.dummy_name()` |
| `user.name.first` | `anon.dummy_first_name()` |
| `user.name.last` | `anon.dummy_last_name()` |

## Using it

The fields of a class, with the policy for logs: personal data is masked, special category data and secrets are left
out, the rest is shown.

```java
@DefaultCategory(value = SYSTEM_OPERATIONS, reason = "technical")
public class Student {

    @Category(USER_NAME_FIRST)
    private String firstName;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    @Category(USER_HEALTH_AND_MEDICAL_DISABILITY)
    private Integer disabilityDegree;

    @Category(USER_EDUCATION_GRADE)
    private BigDecimal mark;

    @Category(USER_AUTHORIZATION_PASSWORD)
    private String passwordHash;

    private long version;

    @Exempt("rendering cache")
    private String cache;
}
```

```java
import io.github.devbpm.linneo.export.postgresql.PostgresAnonymizer;

ClassClassification student = classifier.classify(Student.class);

PostgresAnonymizer anonymizer = PostgresAnonymizer.of(DisclosurePolicy.forLogs());
List<String> statements = anonymizer.labels(student, path -> "students." + path);
statements.forEach(System.out::println);
```

```text
SECURITY LABEL FOR anon ON COLUMN students.firstName IS 'MASKED WITH FUNCTION anon.dummy_first_name()';
SECURITY LABEL FOR anon ON COLUMN students.email IS 'MASKED WITH FUNCTION anon.dummy_safe_email()';
SECURITY LABEL FOR anon ON COLUMN students.disabilityDegree IS 'MASKED WITH VALUE NULL';
SECURITY LABEL FOR anon ON COLUMN students.mark IS 'MASKED WITH VALUE NULL';
SECURITY LABEL FOR anon ON COLUMN students.passwordHash IS 'MASKED WITH VALUE NULL';
SECURITY LABEL FOR anon ON COLUMN students.version IS NULL;
SECURITY LABEL FOR anon ON COLUMN students.cache IS NULL;
```

`mark` is personal data, so the policy masks it, but Linneo knows no function for `user.education.grade` and masks it
with `NULL`. Declare one for your column and the rule changes; a mask for a category serves its descendants:

```java
import io.github.devbpm.linneo.export.postgresql.PostgresAnonymizer;

PostgresAnonymizer custom = PostgresAnonymizer.of(DisclosurePolicy.forLogs())
        .withMask(USER_EDUCATION, "anon.random_int_between(0, 10)")
        .withMask(USER_CONTACT_EMAIL, "anon.partial(email, 1, '***', 4)");

ClassClassification student = classifier.classify(Student.class);
FieldClassification mark = student.field("mark").orElseThrow();
String statement = custom.label("public.students.mark", mark);
// SECURITY LABEL FOR anon ON COLUMN public.students.mark IS 'MASKED WITH FUNCTION anon.random_int_between(0, 10)';
```

The statements of a stored classification are the same, which lets a back office that has no model classes write them
from a [data inventory](../classify/inventory.md):

```java
import io.github.devbpm.linneo.export.postgresql.PostgresAnonymizer;

DataInventory inventory = classifier.inventory(List.of(Student.class), type -> "students");

PostgresAnonymizer anonymizer = PostgresAnonymizer.of(DisclosurePolicy.forLogs());
List<String> statements = anonymizer.labels(inventory, "students", taxonomy, path -> "students." + path);
```

Run the statements with the database tool of the application, for example in the script that prepares the copy of
production data. A policy for another channel (an export, an API) gives other rules: build it with
[`DisclosurePolicy.builder()`](../classify/disclosure.md#builder).

See also: [Export](README.md) · [Disclosure policy](../classify/disclosure.md) ·
[Data inventory](../classify/inventory.md)
