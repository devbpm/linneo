---
title: "1. Why classify data"
parent: "Course"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/LegalNature.java
  - src/main/java/io/github/devbpm/linneo/Confidentiality.java
  - src/main/java/io/github/devbpm/linneo/Integrity.java
  - src/main/java/io/github/devbpm/linneo/Identifiability.java
-->

# Module 1 — Why classify data

## Learning objectives

After this module you will be able to:

- explain why an organisation needs to know, field by field, what its data model holds;
- tell apart the four legal natures Linneo uses: ordinary personal data, special category data (GDPR art. 9),
  criminal offence data (GDPR art. 10) and data that is not personal;
- describe confidentiality and integrity as two independent scales, and identifiability as a property of how a
  value is stored;
- recognise secrets and explain why they need special handling;
- explain what "fail closed" means for a classification and why Linneo is designed that way.

## The problem

Our running example is a university. Its student information system has a `Student` table, enrollments, grades,
staff records, uploaded documents and a lot of configuration. Sooner or later someone asks questions like:

- *"Which tables hold health data?"* (a data protection impact assessment, a breach notification)
- *"Can we send this table to the analytics team?"* (purpose limitation, minimisation)
- *"What must the record of processing activities say about the grading process?"* (GDPR art. 30)
- *"Which columns must be encrypted, masked in logs or excluded from the test database dump?"* (GDPR art. 32)

If the answer lives in a spreadsheet maintained by hand, it is out of date the day after a developer adds a column.
Linneo puts the answer **next to the field**, in the source code, and checks it in the build:

```java
@Category(USER_HEALTH_AND_MEDICAL_DISABILITY)
private Integer disabilityDegree;
```

The annotation says *what* the field holds. Everything else — whether it is personal data, how sensitive it is,
how it must be protected — follows from the category, as the rest of this module explains.

## Legal nature: what the data is under data protection law

Every data category in Linneo has a `LegalNature`, an enum with four values:

| `LegalNature` | Meaning | University examples |
|---|---|---|
| `NONE` | Not personal data: system, organisation or reference data | menu configuration, list of countries, building names |
| `ORDINARY` | Personal data (GDPR art. 4.1): any information relating to an identified or identifiable natural person | student name, email, enrollment, grade, IBAN of a staff member |
| `SPECIAL_CATEGORY` | Special categories of personal data (GDPR art. 9): racial or ethnic origin, political opinions, religious or philosophical beliefs, trade union membership, genetic, biometric, health, sex life and sexual orientation | degree of disability, a medical certificate, a fingerprint template |
| `CRIMINAL_OFFENCE` | Personal data relating to criminal convictions and offences (GDPR art. 10) | the certificate that staff working with minors have no sexual offence convictions |

`LegalNature.isPersonal()` returns `false` only for `NONE`. Special category and criminal offence data are
personal data with *reinforced* protection: processing them needs an additional legal basis, and a breach is
treated more seriously.

### Natures only get stricter down the tree

Categories form a tree (module 2). A child category cannot be *less* personal than its parent. The rule is
encoded in `LegalNature.admits(child)`:

| Parent nature | Child may be |
|---|---|
| `NONE` | only `NONE` |
| `ORDINARY` | `ORDINARY`, `SPECIAL_CATEGORY` or `CRIMINAL_OFFENCE` (anything but `NONE`) |
| `SPECIAL_CATEGORY` | only `SPECIAL_CATEGORY` |
| `CRIMINAL_OFFENCE` | only `CRIMINAL_OFFENCE` |

```java
LegalNature.ORDINARY.admits(LegalNature.SPECIAL_CATEGORY);   // true: user -> user.health_and_medical
LegalNature.SPECIAL_CATEGORY.admits(LegalNature.ORDINARY);   // false: health data never becomes ordinary
LegalNature.NONE.admits(LegalNature.ORDINARY);                // false: a system subtree never holds personal data
```

This is why, under `user` (ordinary personal data), you find `user.health_and_medical` (special category) and
`user.criminal_history` (criminal offence), and why nothing under them can be "downgraded".

## Confidentiality: the harm of disclosure

`Confidentiality` measures how much harm disclosing the data would cause. It has four levels, declared from least
to most strict, so `compareTo` orders them by strictness:

| Level | Meaning |
|---|---|
| `PUBLIC` | Meant to be public; disclosure causes no harm |
| `LOW` | Disclosure would cause limited harm |
| `MEDIUM` | Disclosure would cause serious harm |
| `HIGH` | Disclosure would cause severe harm |

`LOW`, `MEDIUM` and `HIGH` follow the usual three-level scales of information security frameworks, such as the
Spanish ENS or FIPS 199. `PUBLIC` is for data that is meant to be published — the public course catalogue, the
list of countries.

`Confidentiality.max(other)` returns the stricter of two levels, which is handy when you combine several fields:

```java
Confidentiality.LOW.max(Confidentiality.HIGH);   // HIGH
```

## Integrity: the harm of an unwanted change

`Integrity` measures how much harm an unauthorised or undetected change would cause: `LOW`, `MEDIUM`, `HIGH`, again
in increasing order and with a `max` method.

Integrity is **independent** of confidentiality. Two university examples:

- A **grade** is moderately confidential, but its integrity is critical: a silently altered mark changes someone's
  academic record. In Linneo's standard policy `user.education.grade` is confidentiality `MEDIUM`, integrity `HIGH`.
- A **permission matrix** (`system.security`) may be harmless to disclose, but an unwanted change grants access to
  the wrong people: confidentiality `LOW`, integrity `HIGH`.

Keeping both scales lets the classification drive both kinds of control: access restrictions and encryption for
confidentiality; audit trails, signatures and change approval for integrity.

## Identifiability: a property of the stored value

The same kind of data can be stored in very different ways. A grade stored next to the student's id identifies the
student; the average grade of a course with 120 students does not describe any one of them. `Identifiability`
captures this, and it belongs to the **field**, not to the category:

| `Identifiability` | Meaning | Personal data? |
|---|---|---|
| `IDENTIFIED` | The value identifies the person directly or through other data the controller holds | yes |
| `PSEUDONYMIZED` | The value can be linked to the person only with additional information kept separately (GDPR art. 4.5) | **yes** (GDPR recital 26) |
| `ANONYMIZED` | The value can no longer be linked to the person | no |
| `AGGREGATED` | The value summarises several people and does not describe any one of them | no |

The values follow the identifiability traits of the Microsoft Common Data Model. Note the common misconception:
**pseudonymised data is still personal data**. Only anonymised and aggregated values leave the scope of the GDPR,
and only as long as nobody can re-identify the person.

In module 3 you will declare identifiability on a field:

```java
@Category(value = USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
private String analyticsId;
```

## Secrets

Some data does not just *describe* someone: it *grants access*. Passwords, password hashes, API keys, signing keys,
tokens. Linneo marks these categories as **secret**. A secret must never be read back, exported or published — only
verified. In the standard taxonomy a person's credentials (`user.authorization.credentials`, `.password`,
`.biometric`), session IDs (`user.device.session_id`, which grant access while the session is valid) and
`system.authentication.secret` are secrets. `user.authorization` itself, the scope of a person's permissions, is
confidential but not secret. Module 2 shows how the flag is inherited, and module 5 uses it to keep secrets out of
logs.

## Fail closed

The most important design decision in Linneo is that it **fails closed**:

- a field with no classification is an **error**, not "probably fine";
- a key that does not exist in the taxonomy is an error, not ignored;
- a field that declares a *lower* protection than its category is an error;
- a class-wide default may only name *non-personal* data; personal data is always declared field by field.

The opposite approach — classify what you remember to classify, assume the rest is harmless — is how health data
ends up in a debug log. With Linneo, a developer who adds a column without classifying it breaks the build, and the
fix is a one-line annotation reviewed together with the column (module 4).

## Common mistakes

- **"Pseudonymised means anonymous."** It does not. A pseudonymous identifier is personal data; only `ANONYMIZED`
  and `AGGREGATED` values are not.
- **Treating confidentiality and integrity as one "sensitivity" score.** A grade and a permission matrix need very
  different controls; keep the two scales separate.
- **Thinking criminal offence data is a special category.** GDPR art. 10 is a separate regime from art. 9, which is
  why `CRIMINAL_OFFENCE` is its own nature. (When exported to OpenMetadata both map to the same built-in tag; see
  module 7.)
- **Classifying by table instead of by field.** A `Student` table holds ordinary data (name), special category data
  (disability) and pseudonymous identifiers side by side. The classification is per field.
- **Assuming "not personal" means "not sensitive".** A signing key is not personal data and is a secret with high
  confidentiality.

## Exercises

**Exercise 1.1 — Nature.** For each item held by the university, give its `LegalNature`:

1. A student's email address.
2. The label of a menu entry in the student portal.
3. A medical certificate justifying an exam absence.
4. The negative certificate of sexual offences required for staff working with minors.
5. The number of students who passed *Algebra I* last year.
6. A student's religion, recorded to organise chaplaincy services.
7. The ISO code of a country.

**Exercise 1.2 — Two scales.** For each item, say whether *confidentiality* or *integrity* is the main concern, and
why: (a) the final mark of a student; (b) the hash chain that protects the audit log; (c) a staff member's home
address; (d) the role that grants access to the grading module.

**Exercise 1.3 — Identifiability.** Choose the `Identifiability` of each stored value and say whether it is personal
data: (a) a student id in the enrollment table; (b) a random analytics id whose mapping to students is kept in a
separate, restricted table; (c) the average mark of a course; (d) a survey answer whose link to the respondent was
irreversibly destroyed.

**Exercise 1.4 — Code.** Without running it, predict the output, then run it to check:

```java
System.out.println(LegalNature.CRIMINAL_OFFENCE.isPersonal());
System.out.println(LegalNature.ORDINARY.admits(LegalNature.CRIMINAL_OFFENCE));
System.out.println(LegalNature.CRIMINAL_OFFENCE.admits(LegalNature.SPECIAL_CATEGORY));
System.out.println(Confidentiality.PUBLIC.compareTo(Confidentiality.LOW) < 0);
System.out.println(Integrity.MEDIUM.max(Integrity.LOW));
```

## Self-check quiz

1. Which `LegalNature` values are personal data?
2. May a child of a `SPECIAL_CATEGORY` category be `ORDINARY`?
3. Is a pseudonymised value personal data?
4. Does identifiability belong to the category or to the field?
5. What does "fail closed" mean for a field that nobody classified?

## Further reading

- [Reference — Protection levels](../reference/protection-levels.md): `LegalNature`, `Confidentiality`,
  `Integrity`, `Identifiability` in detail.

---

← [Course overview](README.md) · Next: [Module 2 — The taxonomy](02-the-taxonomy.md) →

<details markdown="block">
<summary><strong>Solutions</strong></summary>

**Exercise 1.1**

1. `ORDINARY`. 2. `NONE`. 3. `SPECIAL_CATEGORY` (health). 4. `CRIMINAL_OFFENCE` (GDPR art. 10). 5. The category is
about grades (personal), but the stored value is aggregated, so it is not personal data — in module 3 you will
classify it as a grade with `identifiability = AGGREGATED`. 6. `SPECIAL_CATEGORY` (religious beliefs). 7. `NONE`.

**Exercise 1.2**

(a) Both matter, but integrity is critical: an altered mark changes the academic record (the standard policy gives
grades confidentiality `MEDIUM`, integrity `HIGH`). (b) Integrity: the hash chain is the evidence that the log was
not altered; disclosing it is harmless. (c) Confidentiality: disclosure exposes the person. (d) Integrity: changing
who holds the role grants access to the wrong people.

**Exercise 1.3**

(a) `IDENTIFIED`, personal. (b) `PSEUDONYMIZED`, personal (the mapping exists, even if kept apart). (c) `AGGREGATED`,
not personal. (d) `ANONYMIZED`, not personal, as long as nobody can re-identify the respondent.

**Exercise 1.4**

```
true
true
false
true
MEDIUM
```

**Quiz**

1. `ORDINARY`, `SPECIAL_CATEGORY` and `CRIMINAL_OFFENCE` (everything but `NONE`).
2. No. `SPECIAL_CATEGORY.admits(...)` only accepts `SPECIAL_CATEGORY`.
3. Yes (GDPR art. 4.5 and recital 26).
4. To the field: the same category can be stored identified in one table and aggregated in another.
5. It is an error that fails verification, not an implicit "not personal".

</details>
