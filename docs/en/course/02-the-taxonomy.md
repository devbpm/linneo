---
title: "2. The taxonomy"
parent: "Course"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/Taxonomy.java
  - src/main/java/io/github/devbpm/linneo/DataCategory.java
  - src/main/java/io/github/devbpm/linneo/internal/StandardTaxonomy.java
  - src/main/resources/io/github/devbpm/linneo/internal/linneo-category-policy.tsv
  - src/main/java/io/github/devbpm/linneo/Categories.java
-->

# Module 2 — The taxonomy

## Learning objectives

After this module you will be able to:

- describe where Linneo's taxonomy comes from (fideslang plus Linneo's extensions) and its three hierarchies;
- read a dotted key and find its parent and ancestors;
- explain how protection facets are inherited and why a child can only make them stricter;
- use the `Taxonomy` API to look up, navigate and list categories;
- recognise deprecated keys and how they resolve to their replacement.

## Where the terms come from

Linneo does not invent a vocabulary from scratch. It bundles the complete
[fideslang](https://github.com/ethyca/fideslang) default taxonomy, **version 3.1.4**, an open taxonomy for privacy
engineering, and adds its own terms where fideslang has gaps — in particular non-personal data and education data,
which matter for a university.

| | fideslang 3.1.4 | Added by Linneo | Standard taxonomy |
|---|---|---|---|
| Data categories (*what* a field holds) | 85 | 38 | 123 |
| Data uses (*why* data is processed) | 56 | 7 (`education.*`) | 63 |
| Data subjects (*whose* data it is) | 15 | 6 (`student`, `applicant`, `alumnus`, `faculty_member`, `staff_member`, `legal_representative`) | 21 |

You can check those numbers yourself:

```java
System.out.println(Taxonomy.standard());
// Taxonomy[123 data categories, 63 data uses, 21 data subjects]
```

The terms are loaded from tab-separated files bundled in the JAR. fideslang's files are kept verbatim; Linneo's
additions live in separate files and **never redefine a fideslang key**. Every term records its **origin**
(`TermInfo.origin()`): `"fideslang"`, `"linneo"`, or the name of an extension (module 6).

Data uses and data subjects describe *processing activities*, not fields, so there are no annotations for them.
This course concentrates on data categories; module 5 shows how uses and subjects fit into a record of processing
activities.

## Dotted keys

Every term has a key in dot notation. The parent of a key is the key without its last segment:

```
user                                   root
user.contact                           parent: user
user.contact.email                     parent: user.contact
user.education.grade                   parent: user.education   (added by Linneo)
```

The four roots of the data category tree are:

| Root | Origin | Nature | What it covers |
|---|---|---|---|
| `user` | fideslang | `ORDINARY` | Data about a natural person: name, contact, identifiers, demographics, health, financial, behaviour, plus Linneo's `user.education.*`, `user.workplace.team/unit/role/assignment_period`, `user.demographic.nationality/family`, the rest of GDPR art. 9 (`user.demographic.philosophical_belief/trade_union_membership/sex_life`), `user.health_and_medical.disability`, `user.behavior.activity_log`... |
| `system` | fideslang | `NONE` | `system.authentication`, `system.operations`, plus Linneo's `system.authentication.secret`, `system.security`, `system.integrity`, `system.configuration` |
| `org` | Linneo | `NONE` | The organisation and legal persons: structure, legal entities, locations, processes, business records |
| `reference` | Linneo | `NONE` | Public code lists and registers (`reference.public`) |

`Term.isA(ancestorKey)` tells you whether a term is a given key or one of its descendants. It compares whole
segments, so `user.contact.email` *is a* `user.contact` but not a `user.con`.

## Key constants

You will rarely type keys as strings. `Categories` holds one constant per data category of the standard taxonomy
(`DataUses` and `DataSubjects` do the same for the other two hierarchies):

```java
Categories.USER_CONTACT_EMAIL                  // "user.contact.email"
Categories.USER_EDUCATION_GRADE                // "user.education.grade"      (Linneo)
Categories.USER_HEALTH_AND_MEDICAL_DISABILITY  // "user.health_and_medical.disability" (Linneo)
Categories.SYSTEM_CONFIGURATION                // "system.configuration"      (Linneo)
```

They are `public static final String` **compile-time constants**, so they can be used inside annotations, and a
typo is a compilation error instead of a run-time surprise. The class is generated from the taxonomy files; every
key of the standard taxonomy has a constant and every constant is a key. The full list is in the
[key constants reference](../reference/key-constants.md).

## Protection travels with the category

fideslang defines keys, names and descriptions, but no protection levels. Linneo adds them in its protection policy
(`linneo-category-policy.tsv`). Every data category has four protection facets, plus free attributes:

| Facet | Values | Rule |
|---|---|---|
| `nature` | `NONE`, `ORDINARY`, `SPECIAL_CATEGORY`, `CRIMINAL_OFFENCE` | fixed by the root; may only become stricter (`LegalNature.admits`, module 1) |
| `confidentiality` | `PUBLIC` < `LOW` < `MEDIUM` < `HIGH` | inherited; a child may raise it, never lower it |
| `integrity` | `LOW` < `MEDIUM` < `HIGH` | inherited; a child may raise it, never lower it |
| `secret` | `true` / `false` | once a category is secret, all its children are |
| `attributes` | key/value pairs | inherited unless a child sets its own value (used by extensions, module 6) |

The policy only lists the keys that *change* something; every other key inherits from its parent. An extract:

```
key                               nature            confidentiality  integrity  secret
user                              ORDINARY          LOW              MEDIUM     false
user.authorization                                  HIGH             HIGH
user.authorization.password                                                     true
user.behavior                                       MEDIUM
user.criminal_history             CRIMINAL_OFFENCE  HIGH             HIGH
user.device.session_id                              HIGH                        true
user.education                                      MEDIUM           HIGH
user.financial                                      MEDIUM           HIGH
user.financial.credit_card                          HIGH
user.government_id                                  MEDIUM
user.health_and_medical           SPECIAL_CATEGORY  HIGH             HIGH
system                            NONE              LOW              LOW        false
system.authentication                               HIGH             HIGH
system.authentication.secret                                                    true
system.configuration                                                 MEDIUM
system.security                                                      HIGH
org                               NONE              PUBLIC           LOW        false
reference                         NONE              PUBLIC           MEDIUM     false
```

(The [standard taxonomy reference](../reference/standard-taxonomy.md) lists every key with its effective levels.)

Let us resolve a few keys of the university model by hand:

- `user.contact.email` declares nothing; `user.contact` declares nothing; so it takes `user`'s facets:
  **ORDINARY, LOW, MEDIUM**, not secret.
- `user.education.grade` declares nothing; `user.education` raises confidentiality to `MEDIUM` and integrity to
  `HIGH`: **ORDINARY, MEDIUM, HIGH**.
- `user.health_and_medical.disability` inherits from `user.health_and_medical`: **SPECIAL_CATEGORY, HIGH, HIGH**.
- `user.authorization.password` inherits `HIGH`/`HIGH` from `user.authorization` and declares itself secret:
  **ORDINARY, HIGH, HIGH, secret**. `user.authorization` itself (the scope of a person's permissions) is not secret.
- `user.financial.credit_card` raises confidentiality to `HIGH` and inherits integrity `HIGH` from `user.financial`.

The values you get from a `DataCategory` are always these **effective** facets: what the category declares or,
where it declares nothing, what it inherits. The taxonomy guarantees they are never weaker than the parent's —
it refuses to build otherwise (module 6 shows the error messages).

```java
DataCategory grade = Taxonomy.standard().requireCategory(Categories.USER_EDUCATION_GRADE);
grade.nature();            // ORDINARY
grade.isPersonal();        // true
grade.confidentiality();   // MEDIUM
grade.integrity();         // HIGH
grade.secret();            // false
grade.attribute("retention");  // null: no attribute set in the standard taxonomy
```

## Exploring with the `Taxonomy` API

A `Taxonomy` is immutable and thread-safe; build one per application and share it.

| Method | Use it to… |
|---|---|
| `Taxonomy.standard()` | get the standard taxonomy (fideslang + Linneo), created once and shared |
| `Taxonomy.load()` | get the standard taxonomy **plus every registered extension** (module 6); builds a new instance on each call, so keep it |
| `categories()` | list every category, ordered by key (a parent always comes before its descendants) |
| `category(key)` | look up a key; empty `Optional` if unknown |
| `requireCategory(key)` | look up a key; throws `TaxonomyException("Unknown data category '...'")` if unknown |
| `resolveCategory(key)` | look up a key and follow deprecation replacements to the current category |
| `parentOf(c)`, `childrenOf(c)`, `ancestorsOf(c)` | navigate the tree |
| `dataUses()`, `dataUse(key)`, `requireDataUse(key)`, `childrenOf(use)` | the same for data uses |
| `dataSubjects()`, `dataSubject(key)`, `requireDataSubject(key)` | the same for data subjects |

Exploring the education subtree:

```java
Taxonomy taxonomy = Taxonomy.standard();
DataCategory education = taxonomy.requireCategory(Categories.USER_EDUCATION);

for (DataCategory c : taxonomy.childrenOf(education)) {
    System.out.printf("%-36s %-9s %-7s %-7s %s%n",
            c.key(), c.nature(), c.confidentiality(), c.integrity(), c.info().origin());
}
```

```
user.education.accomplishment        ORDINARY  MEDIUM  HIGH    linneo
user.education.course_history        ORDINARY  MEDIUM  HIGH    linneo
user.education.enrollment            ORDINARY  MEDIUM  HIGH    linneo
user.education.grade                 ORDINARY  MEDIUM  HIGH    linneo
user.education.previous_education    ORDINARY  MEDIUM  HIGH    linneo
user.education.program               ORDINARY  MEDIUM  HIGH    linneo
user.education.status                ORDINARY  MEDIUM  HIGH    linneo
user.education.test_score            ORDINARY  MEDIUM  HIGH    linneo
```

Walking up the tree:

```java
DataCategory grade = taxonomy.requireCategory(Categories.USER_EDUCATION_GRADE);
taxonomy.ancestorsOf(grade).stream().map(DataCategory::key).toList();   // [user.education, user] — nearest first
```

Finding every category with reinforced protection:

```java
taxonomy.categories().stream()
        .filter(c -> c.nature() == LegalNature.SPECIAL_CATEGORY || c.nature() == LegalNature.CRIMINAL_OFFENCE)
        .map(DataCategory::key)
        .forEach(System.out::println);
```

## Deprecated keys

Applications persist and publish keys, so a key is never deleted or reused for another meaning. Instead it is
**deprecated**, optionally with a replacement. `TermInfo` carries the lifecycle:

- `since()` — the version of its origin that introduced the term (`"2.0.0"` for fideslang terms, `"0.1.0"` for
  Linneo's);
- `deprecatedSince()` and `replacedBy()` — set for a deprecated term;
- `isDeprecated()` — `true` if either is set.

`category(key)` and `requireCategory(key)` return a deprecated category as it is; `resolveCategory(key)` follows
`replacedBy` (even through several hops) to the current one.

In the standard taxonomy of Linneo 0.1.0 **no data category is deprecated**. The only deprecated term is a fideslang
data use:

```java
TermInfo info = taxonomy.requireDataUse("personalize.profiling").info();
info.isDeprecated();       // true
info.deprecatedSince();    // "2.1.1"
info.replacedBy();         // "personalize.content.profiling"
```

Deprecated data categories appear when an extension deprecates its own terms (module 6) or in future versions of
the taxonomy. Module 4 shows how the classifier reports them: as warnings, not errors.

## Common mistakes

- **Using `Taxonomy.standard()` in one place and `Taxonomy.load()` in another.** `standard()` ignores your
  extensions; if your model uses extension keys, everything must use `load()` (or the same builder).
- **Calling `Taxonomy.load()` repeatedly.** It builds a new taxonomy each time. Build it once and share it.
- **Looking for levels in the key.** `user.contact.email` says nothing about confidentiality; the levels come from
  the policy and inheritance. Always read them from the `DataCategory`.
- **Assuming every child declares its levels.** Most inherit. A category's *effective* level can come from any
  ancestor.
- **Matching keys with `startsWith`.** `"user.contact".startsWith("user.con")` is true; use `isA`, which compares
  whole segments.

## Exercises

**Exercise 2.1 — Effective levels.** Without running code, give nature, confidentiality, integrity and secret flag
for: (a) `user.contact.address.city`; (b) `user.demographic.date_of_birth`; (c) `user.authorization.biometric`;
(d) `reference.public`; (e) `system.authentication.secret`. Then check with `requireCategory`.

**Exercise 2.2 — Choose keys.** Using the [key constants reference](../reference/key-constants.md) or
`taxonomy.categories()`, pick the most specific key for these university fields: (a) the student's national
identity number (DNI); (b) the staff member's social security number; (c) the degree programme a student is
enrolled in; (d) the department a staff member belongs to; (e) the list of countries used in drop-downs; (f) a
scholarship awarded to a student.

**Exercise 2.3 — Special categories.** Write the code that prints every key whose nature is `SPECIAL_CATEGORY` or
`CRIMINAL_OFFENCE`, with its origin. How many are there, and which of them were added by Linneo?

**Exercise 2.4 — Secrets.** Write code that lists every secret category and whether it is personal. Explain why
`user.authorization.password` and `system.authentication.secret` have different natures.

## Self-check quiz

1. What is the parent of `user.contact.address.city`?
2. How many data categories does the standard taxonomy have, and how many come from fideslang?
3. A category declares confidentiality `MEDIUM`. May its child declare `LOW`?
4. What is the difference between `requireCategory` and `resolveCategory`?
5. Why can `Categories.USER_NAME` be used inside an annotation?

## Further reading

- [Reference — Taxonomy](../reference/taxonomy.md) and [Terms](../reference/terms.md)
- [Reference — Standard taxonomy](../reference/standard-taxonomy.md): every key with its effective levels
- [Reference — Key constants](../reference/key-constants.md)
- [Reference — Protection levels](../reference/protection-levels.md)

---

← Previous: [Module 1 — Why classify data](01-why-classify.md) · Next:
[Module 3 — Annotating models](03-annotating-models.md) →

<details markdown="block">
<summary><strong>Solutions</strong></summary>

**Exercise 2.1**

(a) `user.contact.address.city`: nothing declared on the way up to `user` → ORDINARY, LOW, MEDIUM, not secret.
(b) `user.demographic.date_of_birth`: declares confidentiality `MEDIUM` → ORDINARY, MEDIUM, MEDIUM, not secret.
(c) `user.authorization.biometric`: declares `SPECIAL_CATEGORY` and secret, inherits HIGH/HIGH from
`user.authorization` → SPECIAL_CATEGORY, HIGH, HIGH, secret.
(d) `reference.public`: inherits from `reference` → NONE, PUBLIC, MEDIUM, not secret.
(e) `system.authentication.secret`: inherits HIGH/HIGH from `system.authentication`, declares secret → NONE, HIGH,
HIGH, secret.

**Exercise 2.2**

(a) `USER_GOVERNMENT_ID_NATIONAL_IDENTIFICATION_NUMBER`; (b) `USER_GOVERNMENT_ID_SOCIAL_SECURITY_NUMBER` (Linneo);
(c) `USER_EDUCATION_PROGRAM`; (d) `USER_WORKPLACE_UNIT`; (e) `REFERENCE_PUBLIC`; (f) `USER_FINANCIAL_SCHOLARSHIP`.

**Exercise 2.3**

```java
Taxonomy taxonomy = Taxonomy.standard();
taxonomy.categories().stream()
        .filter(c -> c.nature() == LegalNature.SPECIAL_CATEGORY || c.nature() == LegalNature.CRIMINAL_OFFENCE)
        .forEach(c -> System.out.println(c.key() + " " + c.nature() + " " + c.info().origin()));
```

There are 19: `user.authorization.biometric`, `user.biometric` and its four children (`fingerprint`, `health`,
`retinal`, `voice`), `user.criminal_history` (the only `CRIMINAL_OFFENCE`), the seven special demographic categories
(`philosophical_belief`, `political_opinion`, `race_ethnicity`, `religious_belief`, `sex_life`, `sexual_orientation`,
`trade_union_membership`), and `user.health_and_medical` with `genetic`, `insurance_beneficiary_id`, `record_id` and
Linneo's `disability`. Four have origin `linneo`: `user.demographic.philosophical_belief`, `.sex_life` and
`.trade_union_membership`, which complete the GDPR article 9 list, and `user.health_and_medical.disability`.

**Exercise 2.4**

```java
taxonomy.categories().stream()
        .filter(DataCategory::secret)
        .forEach(c -> System.out.println(c.key() + " personal=" + c.isPersonal()));
```

```
system.authentication.secret personal=false
user.authorization.biometric personal=true
user.authorization.credentials personal=true
user.authorization.password personal=true
user.device.session_id personal=true
```

A password belongs to a person (it is under `user`, ordinary personal data); an API key or a signing key of a
system belongs to no one (it is under `system`, not personal). Both are secrets. `user.authorization` is not in the
list: it is the scope of a person's permissions, confidential but not a secret. A session ID is, because while the
session is valid it grants access like a credential.

**Quiz**

1. `user.contact.address`.
2. 123, of which 85 come from fideslang.
3. No: a child may only raise a level. The taxonomy refuses to build.
4. `requireCategory` returns the category for that exact key, deprecated or not; `resolveCategory` follows
   `replacedBy` to the current category.
5. It is a compile-time constant (`public static final String` initialised with a literal).

</details>
