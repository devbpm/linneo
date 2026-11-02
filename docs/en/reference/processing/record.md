---
title: "ProcessingRecord"
parent: "Processing"
grand_parent: "Reference"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingRecord.java
  - src/main/java/io/github/devbpm/linneo/processing/RecordDiff.java
  - src/main/java/io/github/devbpm/linneo/processing/ActivityChange.java
  - src/main/java/io/github/devbpm/linneo/processing/ImpactScreening.java
-->

# ProcessingRecord

`io.github.devbpm.linneo.processing.ProcessingRecord` — `public record`, `@since 0.1.0`

```java
public record ProcessingRecord(List<CheckedActivity> activities)
```

The record of processing activities of a controller (GDPR article 30.1). [`ProcessingChecker.verify`](checker.md)
builds it from activities without errors, so every activity in it has a lawful basis and the conditions its data
needs. The warnings of each activity travel with it as **open points**. The record renders as Markdown for people
and as JSON for tools. Its [fingerprint](#fingerprint) tells whether it is still the record someone approved, and
[`RecordDiff`](#recorddiff) what changed since.

**The texts the record adds are in English** (row labels, `None declared`, `Open points:`, the messages of the open
points), whatever the language of the application. The names of purposes, data subjects and categories come from
the taxonomy (the standard taxonomy's names are English too), and the remaining texts — activity names,
descriptions, interests, references, retention, recipients, destinations — are printed as declared.

## Members

| Member | Description |
|---|---|
| `ProcessingRecord(List<CheckedActivity> activities)` | Copies the list (immutable). Throws `IllegalArgumentException` if any activity has an error, with the message `The processing activity '<key>' has errors and cannot go in the record: [...]`, so a record only holds valid activities (warnings are allowed). It also throws `IllegalArgumentException` if two activities have the same key (`Two processing activities of the record have the key '<key>'`), so each key is in the record once. Normally you get a record from `verify`, which reports every error. |
| `List<CheckedActivity> activities()` | The checked activities, in declaration order. |
| `Optional<CheckedActivity> activity(String key)` | The activity with that key, or empty. |
| `String toMarkdown(String title)` | The record as a Markdown document whose level-1 heading is `title` (for example the controller's name). `NullPointerException` if `title` is `null`. |
| `String toJson()` | The record as a JSON object. |
| `String fingerprint()` | The SHA-256 of the content of the record, as 64 hexadecimal digits; see [Fingerprint](#fingerprint). |

## Markdown

`toMarkdown(title)` writes the title, then for each activity a level-2 heading with its name, a line with its key
and description, a two-column table and, if the activity has warnings, an `Open points:` list with their messages.

| Row | Shown | Content |
|---|---|---|
| `Purposes (art. 30.1.b)` | always | ``<name> (`<key>`)`` for each purpose, in key order, separated by `; `, or `None` |
| `Lawful basis (art. 6.1)` | when declared | `Art. <article>: <fideslang name>` |
| `Legitimate interest` | only when the basis is `LEGITIMATE_INTERESTS` and an interest is stated | the interest |
| `Special categories (art. 9.2)` | when declared | `Art. <article>: <fideslang name>` for each condition, separated by `; ` |
| `Criminal offence data (art. 10)` | when declared | `Art. 10: Official authority` or `Art. 10: Authorised by law` |
| `Legal reference` | when declared | the reference |
| `Data subjects (art. 30.1.c)` | always | as purposes |
| `Personal data (art. 30.1.c)` | always | as purposes, with `, special category` or `, criminal offence data` after the key of such categories |
| `Recipients (art. 30.1.d)` | always | the recipients separated by `; `, or `None declared` |
| `Transfers (art. 30.1.e)` | always | `<destination> (art. <article>: <fideslang name>)` for each, or `None declared` |
| `Retention (art. 30.1.f)` | always | the retention, or `Not declared` |
| `Required protection (art. 30.1.g)` | always | `Confidentiality <level>, integrity <level>`, plus `, includes secrets` if any data is a secret |
| `Impact assessment (art. 35)` | always | the reference, or `None` |
| `Impact assessment screening` | always | the [screening](impact-screening.md) of the activity as `ImpactScreening.summary()` gives it: `Not indicated`, or the outcome with the criteria met and what shows each |

The record states the protection the data **requires** (the highest levels of its categories and fields), as input
for the description of security measures of article 30.1.g; it does not describe the measures themselves.

Every text written into the document — title, names, description and cells — is written as text, so that a value can
neither break the table nor become a link or HTML where the Markdown is rendered: `\`, `|`, `[` and `]` are escaped
with a backslash (`\|`), `&`, `<` and `>` become `&amp;`, `&lt;` and `&gt;`, and line breaks (`\n` or `\r`, with the
white space around them) collapse to one space. The activity key is written as code, unescaped.

### Example

```java
ProcessingActivity admissions = ProcessingActivity.builder("admissions", "Admissions")
        .description("Receives and assesses applications,\nincluding requests for the disability quota.")
        .purposes(DataUses.EDUCATION_ADMISSION)
        .dataSubjects(DataSubjects.APPLICANT)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL, USER_HEALTH_AND_MEDICAL_DISABILITY)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .specialCategoryConditions(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
        .recipients("Admissions board")
        .impactAssessment("DPIA-2026-01")
        .build();
ProcessingActivity newsletter = ProcessingActivity.builder("alumni-newsletter", "Alumni newsletter")
        .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL)
        .dataSubjects(DataSubjects.ALUMNUS)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL)
        .legitimateInterest("Keeping former students informed about university events")
        .retention("Until the alumnus unsubscribes")
        .recipients("Email delivery provider (processor)")
        .transfer("United States", TransferSafeguard.STANDARD_CONTRACTUAL_CLAUSES)
        .build();

ProcessingRecord record = new ProcessingChecker(Taxonomy.standard()).verify(admissions, newsletter);
String markdown = record.toMarkdown("Example University | Records");
```

`markdown` is:

```markdown
# Example University \| Records

## Admissions

`admissions`: Receives and assesses applications, including requests for the disability quota.

| Record item | Content |
|---|---|
| Purposes (art. 30.1.b) | Admission (`education.admission`) |
| Lawful basis (art. 6.1) | Art. 6.1.e: Public interest |
| Special categories (art. 9.2) | Art. 9.2.g: Reasons of substantial public interest (with a basis in law) |
| Data subjects (art. 30.1.c) | Applicant (`applicant`) |
| Personal data (art. 30.1.c) | User Contact Email (`user.contact.email`); Disability (`user.health_and_medical.disability`, special category); Name (`user.name`) |
| Recipients (art. 30.1.d) | Admissions board |
| Transfers (art. 30.1.e) | None declared |
| Retention (art. 30.1.f) | Not declared |
| Required protection (art. 30.1.g) | Confidentiality High, integrity High |
| Impact assessment (art. 35) | DPIA-2026-01 |
| Impact assessment screening | Possible (1 criterion of WP248): 4. sensitive data: special category data user.health_and_medical.disability |

Open points:

- GDPR article(s) 6.1.e, 9.2.g rest on Union or Member State law; name it as the legal reference
- Declare the envisaged time limit for erasure (GDPR article 30.1.f)

## Alumni newsletter

`alumni-newsletter`

| Record item | Content |
|---|---|
| Purposes (art. 30.1.b) | Marketing Email Communications (`marketing.communications.email`) |
| Lawful basis (art. 6.1) | Art. 6.1.f: Legitimate interests |
| Legitimate interest | Keeping former students informed about university events |
| Data subjects (art. 30.1.c) | Alumnus (`alumnus`) |
| Personal data (art. 30.1.c) | User Contact Email (`user.contact.email`); Name (`user.name`) |
| Recipients (art. 30.1.d) | Email delivery provider (processor) |
| Transfers (art. 30.1.e) | United States (art. 46.2.c: SCCs) |
| Retention (art. 30.1.f) | Until the alumnus unsubscribes |
| Required protection (art. 30.1.g) | Confidentiality Low, integrity Medium |
| Impact assessment (art. 35) | None |
| Impact assessment screening | Not indicated |
```

The screening is shown whether or not the activity refers to an impact assessment, so the officer sees what the criteria
say next to what was declared.

## JSON

`toJson()` returns an object with one member, `activities`, an array with one object per activity in declaration
order. Optional members are left out when the activity does not declare them; lists are always present.

| Member | Present | Value |
|---|---|---|
| `key`, `name` | always | strings |
| `description` | when declared | string (line breaks kept) |
| `purposes`, `dataSubjects`, `dataCategories` | always | arrays of keys in key order, after resolution (deprecated keys replaced, non-personal categories left out) |
| `legalBasis` | when declared | `{"code": <constant>, "article": <article>, "fideslang": <fideslang name>}` |
| `legitimateInterest` | only when the basis is `LEGITIMATE_INTERESTS` and an interest is stated | string |
| `specialCategoryConditions` | when declared | array of objects as `legalBasis`, one for each condition |
| `criminalDataBasis` | when declared | `{"code": <constant>, "article": "10"}`, without `fideslang` |
| `legalReference` | when declared | string |
| `recipients` | always | array of strings |
| `transfers` | always | array of `{"destination": <string>, "safeguard": {"code", "article", "fideslang"}}` |
| `retention`, `impactAssessment` | when declared | strings |
| `screening` | always | `{"outcome": <Outcome>, "criteria": [<Criterion>...], "mandatoryCase": <string>}`: the names of the constants; `mandatoryCase` (such as `"35.3.b"`) only when the screening finds a case of article 35.3 |
| `protection` | always | `{"natures": [<LegalNature>...], "confidentiality": <level>, "integrity": <level>, "secret": <boolean>}` |
| `openPoints` | always | array with the messages of the warnings |

The JSON of the record above (one line):

```json
{"activities":[{"key":"admissions","name":"Admissions","description":"Receives and assesses applications,\nincluding requests for the disability quota.","purposes":["education.admission"],"dataSubjects":["applicant"],"dataCategories":["user.contact.email","user.health_and_medical.disability","user.name"],"legalBasis":{"code":"PUBLIC_INTEREST","article":"6.1.e","fideslang":"Public interest"},"specialCategoryConditions":[{"code":"SUBSTANTIAL_PUBLIC_INTEREST","article":"9.2.g","fideslang":"Reasons of substantial public interest (with a basis in law)"}],"recipients":["Admissions board"],"transfers":[],"impactAssessment":"DPIA-2026-01","screening":{"outcome":"POSSIBLE","criteria":["SENSITIVE_DATA"]},"protection":{"natures":["ORDINARY","SPECIAL_CATEGORY"],"confidentiality":"HIGH","integrity":"HIGH","secret":false},"openPoints":["GDPR article(s) 6.1.e, 9.2.g rest on Union or Member State law; name it as the legal reference","Declare the envisaged time limit for erasure (GDPR article 30.1.f)"]},{"key":"alumni-newsletter","name":"Alumni newsletter","purposes":["marketing.communications.email"],"dataSubjects":["alumnus"],"dataCategories":["user.contact.email","user.name"],"legalBasis":{"code":"LEGITIMATE_INTERESTS","article":"6.1.f","fideslang":"Legitimate interests"},"legitimateInterest":"Keeping former students informed about university events","recipients":["Email delivery provider (processor)"],"transfers":[{"destination":"United States","safeguard":{"code":"STANDARD_CONTRACTUAL_CLAUSES","article":"46.2.c","fideslang":"SCCs"}}],"retention":"Until the alumnus unsubscribes","screening":{"outcome":"NOT_INDICATED","criteria":[]},"protection":{"natures":["ORDINARY"],"confidentiality":"LOW","integrity":"MEDIUM","secret":false},"openPoints":[]}]}
```

The `code` members are the enum constant names, so a tool can map them back with `LegalBasis.valueOf(...)` and the
like; the `fideslang` members can fill the matching fields of a fideslang privacy declaration.

## Comparing records

A record is a value the application computes, so it can compare two of them: the record the data protection officer
approved and the one that applies now. `fingerprint()` tells at once whether anything changed, and two types of
`io.github.devbpm.linneo.processing`, `RecordDiff` and `ActivityChange`, say what.

### Fingerprint

`fingerprint()` is the SHA-256 (UTF-8, lower-case hexadecimal, 64 digits) of the content of the record, activity by
activity in key order and aspect by aspect as `RecordDiff` compares them. Two records have the same fingerprint exactly
when `RecordDiff` finds no change between them, whatever the order of their activities. The screening of the need for an
impact assessment is part of the content, so a change in the traits of a term that changes the screening changes the
fingerprint, although no declaration changed. It says that something changed, not what. An application keeps it with
the record the officer approved, to know whether the record still is that one. The text that is hashed writes a lone
UTF-16 surrogate, which UTF-8 cannot encode, as a backslash, `u` and its four hexadecimal digits, so two records that
differ only in such a character never have the same fingerprint.

### RecordDiff

`io.github.devbpm.linneo.processing.RecordDiff` — `public final class`, `@since 0.1.0`. Immutable; there is no public
constructor.

| Member | Description |
|---|---|
| `static RecordDiff between(ProcessingRecord before, ProcessingRecord after)` | Compares two records: `before` is the earlier one, such as the approved one. `NullPointerException` if either is `null`. |
| `List<ActivityChange> changes()` | The changes, one per activity added, removed or changed, by activity key. Immutable. |
| `Optional<ActivityChange> change(String activity)` | The change of one activity, or empty if the activity is the same in both records or in neither. |
| `boolean isEmpty()` | `true` if no activity was added, removed or changed. |

An activity only `after` has is `ADDED`, one only `before` has is `REMOVED`; for the others the diff reports the aspects
whose content differs. The order of the elements of a list (purposes, recipients, open points...) is not part of the
content.

Both records are computed, so the earlier one is rebuilt from what the application stored: the governance the data
protection function keeps, and the [data inventory](../classify/inventory.md) and the taxonomy of the record that was
approved. Checked against what applies now, the same activities show what a new taxonomy, a change of the data model or
an edit of the governance does to the record, before anyone signs it:

```java
ProcessingRecord approved = new ProcessingChecker(appliedTaxonomy, Jurisdiction.SPAIN, storedInventory)
        .verify(activities);
ProcessingRecord current = new ProcessingChecker(taxonomy, Jurisdiction.SPAIN, declared).verify(activities);

RecordDiff diff = RecordDiff.between(approved, current);
for (ActivityChange change : diff.changes()) {
    System.out.println(change);   // activity 'contact': [PERSONAL_DATA, PROTECTION] TIGHTENED
}
```

### ActivityChange

`io.github.devbpm.linneo.processing.ActivityChange` — `public record`, `@since 0.1.0`

```java
public record ActivityChange(String activity, CheckedActivity before, CheckedActivity after, Set<Aspect> aspects,
                             Direction direction)
```

How one activity differs between the two records.

| Component | Nullable | Description |
|---|---|---|
| `activity` | no | The key of the activity. |
| `before` | yes | The activity in the earlier record, or `null` if it was added. |
| `after` | yes | The activity in the later record, or `null` if it was removed. |
| `aspects` | no | What changed, never empty: an immutable set of [`Aspect`](#aspect). |
| `direction` | no | How the protection the personal data of the activity requires changed, as [`Protection.Direction`](../protection-levels.md#direction): `TIGHTENED` means the activity now processes data that needs more protection, which may call for other bases or an impact assessment. `SAME` for an added or removed activity. |

The canonical constructor throws `NullPointerException` if `activity` or `direction` is `null`, and
`IllegalArgumentException` if `aspects` is `null` or empty, or if `before` and `after` are both `null`.

| Member | Description |
|---|---|
| `boolean isGovernance()` | `true` if an aspect of the [governance](activity.md#governance) changed and nothing else did but the open points and the screening: a change the data protection function made, with the scope the code declares and the data it reaches unchanged. The open points and the screening that follow from it may change too. |

`toString()` is `activity '<key>': <aspects>`, followed by the direction unless it is `SAME`.

### Aspect

`ActivityChange.Aspect` — nested `enum`, `@since 0.1.0`: what changed in an activity. `boolean isGovernance()` tells
whether the aspect is part of the governance rather than of the scope declared in code or the data it reaches.

| Aspect | `isGovernance()` | What changed |
|---|---|---|
| `ADDED` | `false` | The activity is new in the record. |
| `REMOVED` | `false` | The activity is no longer in the record. |
| `NAME` | `false` | The name. |
| `DESCRIPTION` | `false` | The description. |
| `PURPOSES` | `false` | The purposes, deprecated keys replaced. |
| `DATA_SUBJECTS` | `false` | The categories of data subjects, deprecated keys replaced. |
| `PERSONAL_DATA` | `false` | The personal data categories processed, whether declared or reached through entities and objects. |
| `PROTECTION` | `false` | The protection the personal data requires; see `direction`. |
| `LEGAL_BASIS` | `true` | The lawful basis. |
| `LEGITIMATE_INTEREST` | `true` | The legitimate interest pursued. |
| `SPECIAL_CATEGORY_CONDITION` | `true` | The article 9.2 conditions. |
| `CRIMINAL_DATA_BASIS` | `true` | The article 10 basis. |
| `LEGAL_REFERENCE` | `true` | The law the bases rest on. |
| `RECIPIENTS` | `true` | The recipients. |
| `TRANSFERS` | `true` | The transfers to third countries or international organisations. |
| `RETENTION` | `true` | The time limit for erasure. |
| `IMPACT_ASSESSMENT` | `true` | The impact assessment. |
| `RISK_FACTORS` | `true` | The [risk factors](impact-screening.md#declaring-risk-factors) the officer declares in the governance for the screening of the need for an impact assessment. |
| `DECLARED_RISK_FACTORS` | `false` | The risk factors the code declares (`ProcessingActivity.riskFactors()`): part of the scope. |
| `SCREENING` | `false` | The [screening](impact-screening.md) of the need for an impact assessment: its outcome, its mandatory case and its criteria. It changes with the traits of the terms even when no declaration changes. |
| `OPEN_POINTS` | `false` | The open points: the warnings of the activity, compared by code and element only, so a change of the wording of the library is not a change of the record. |

An edit of the retention or the recipients is a governance change, and it usually closes an open point too. An edit of
the risk factors in the governance is one as well: the screening names the criteria that are met, so it changes
`RISK_FACTORS` and `SCREENING` and, when the warning about the impact assessment appears or goes away, `OPEN_POINTS`;
`isGovernance()` ignores `SCREENING` and `OPEN_POINTS`. A change in the data the activity reaches, such as a new field
in a listed entity or a stricter category, shows in `PERSONAL_DATA` or `PROTECTION` and is not a governance change.

See also: [ProcessingChecker](checker.md) · [ProcessingActivity](activity.md) · [Legal bases](bases.md) ·
[Processing activities](README.md)
