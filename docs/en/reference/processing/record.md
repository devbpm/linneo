---
title: "ProcessingRecord"
parent: "Processing"
grand_parent: "Reference"
nav_order: 4
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingRecord.java
-->

# ProcessingRecord

`io.github.devbpm.linneo.processing.ProcessingRecord` — `public record`, `@since 0.1.0`

```java
public record ProcessingRecord(List<CheckedActivity> activities)
```

The record of processing activities of a controller (GDPR article 30.1). [`ProcessingChecker.verify`](checker.md)
builds it from activities without errors, so every activity in it has a lawful basis and the conditions its data
needs. The warnings of each activity travel with it as **open points**. The record renders as Markdown for people
and as JSON for tools.

**The texts the record adds are in English** (row labels, `None declared`, `Open points:`, the messages of the open
points), whatever the language of the application. The names of purposes, data subjects and categories come from
the taxonomy (the standard taxonomy's names are English too), and the remaining texts — activity names,
descriptions, interests, references, retention, recipients, destinations — are printed as declared.

## Members

| Member | Description |
|---|---|
| `ProcessingRecord(List<CheckedActivity> activities)` | Copies the list (immutable). Throws `IllegalArgumentException` if any activity has an error, with the message `The processing activity '<key>' has errors and cannot go in the record: [...]`, so a record only holds valid activities (warnings are allowed). It does not check for duplicate keys; normally you get a record from `verify`, which reports every error. |
| `List<CheckedActivity> activities()` | The checked activities, in declaration order. |
| `Optional<CheckedActivity> activity(String key)` | The first activity with that key, or empty. |
| `String toMarkdown(String title)` | The record as a Markdown document whose level-1 heading is `title` (for example the controller's name). `NullPointerException` if `title` is `null`. |
| `String toJson()` | The record as a JSON object. |

## Markdown

`toMarkdown(title)` writes the title, then for each activity a level-2 heading with its name, a line with its key
and description, a two-column table and, if the activity has warnings, an `Open points:` list with their messages.

| Row | Shown | Content |
|---|---|---|
| `Purposes (art. 30.1.b)` | always | ``<name> (`<key>`)`` for each purpose, separated by `; `, or `None` |
| `Lawful basis (art. 6.1)` | when declared | `Art. <article>: <fideslang name>` |
| `Legitimate interest` | only when the basis is `LEGITIMATE_INTERESTS` and an interest is stated | the interest |
| `Special categories (art. 9.2)` | when declared | `Art. <article>: <fideslang name>` |
| `Criminal offence data (art. 10)` | when declared | `Art. 10: Official authority` or `Art. 10: Authorised by law` |
| `Legal reference` | when declared | the reference |
| `Data subjects (art. 30.1.c)` | always | as purposes |
| `Personal data (art. 30.1.c)` | always | as purposes, with `, special category` or `, criminal offence data` after the key of such categories |
| `Recipients (art. 30.1.d)` | always | the recipients separated by `; `, or `None declared` |
| `Transfers (art. 30.1.e)` | always | `<destination> (art. <article>: <fideslang name>)` for each, or `None declared` |
| `Retention (art. 30.1.f)` | always | the retention, or `Not declared` |
| `Required protection (art. 30.1.g)` | always | `Confidentiality <level>, integrity <level>`, plus `, includes secrets` if any data is a secret |
| `Impact assessment (art. 35)` | always | the reference, or `None` |

The record states the protection the data **requires** (the highest levels of its categories and fields), as input
for the description of security measures of article 30.1.g; it does not describe the measures themselves.

Every text written into the document — title, names, description and cells — has `|` escaped as `\|` and line
breaks (with the white space around them) collapsed to one space, so that a value cannot break the table. The
activity key is written as code, unescaped.

### Example

```java
ProcessingActivity admissions = ProcessingActivity.builder("admissions", "Admissions")
        .description("Receives and assesses applications,\nincluding requests for the disability quota.")
        .purposes(DataUses.EDUCATION_ADMISSION)
        .dataSubjects(DataSubjects.APPLICANT)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL, USER_HEALTH_AND_MEDICAL_DISABILITY)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
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
```

## JSON

`toJson()` returns an object with one member, `activities`, an array with one object per activity in declaration
order. Optional members are left out when the activity does not declare them; lists are always present.

| Member | Present | Value |
|---|---|---|
| `key`, `name` | always | strings |
| `description` | when declared | string (line breaks kept) |
| `purposes`, `dataSubjects`, `dataCategories` | always | arrays of keys, after resolution (deprecated keys replaced, non-personal categories left out) |
| `legalBasis` | when declared | `{"code": <constant>, "article": <article>, "fideslang": <fideslang name>}` |
| `legitimateInterest` | only when the basis is `LEGITIMATE_INTERESTS` and an interest is stated | string |
| `specialCategoryCondition` | when declared | as `legalBasis` |
| `criminalDataBasis` | when declared | `{"code": <constant>, "article": "10"}`, without `fideslang` |
| `legalReference` | when declared | string |
| `recipients` | always | array of strings |
| `transfers` | always | array of `{"destination": <string>, "safeguard": {"code", "article", "fideslang"}}` |
| `retention`, `impactAssessment` | when declared | strings |
| `protection` | always | `{"natures": [<LegalNature>...], "confidentiality": <level>, "integrity": <level>, "secret": <boolean>}` |
| `openPoints` | always | array with the messages of the warnings |

The JSON of the record above (one line):

```json
{"activities":[{"key":"admissions","name":"Admissions","description":"Receives and assesses applications,\nincluding requests for the disability quota.","purposes":["education.admission"],"dataSubjects":["applicant"],"dataCategories":["user.contact.email","user.health_and_medical.disability","user.name"],"legalBasis":{"code":"PUBLIC_INTEREST","article":"6.1.e","fideslang":"Public interest"},"specialCategoryCondition":{"code":"SUBSTANTIAL_PUBLIC_INTEREST","article":"9.2.g","fideslang":"Reasons of substantial public interest (with a basis in law)"},"recipients":["Admissions board"],"transfers":[],"impactAssessment":"DPIA-2026-01","protection":{"natures":["ORDINARY","SPECIAL_CATEGORY"],"confidentiality":"HIGH","integrity":"HIGH","secret":false},"openPoints":["GDPR article(s) 6.1.e, 9.2.g rest on Union or Member State law; name it as the legal reference","Declare the envisaged time limit for erasure (GDPR article 30.1.f)"]},{"key":"alumni-newsletter","name":"Alumni newsletter","purposes":["marketing.communications.email"],"dataSubjects":["alumnus"],"dataCategories":["user.contact.email","user.name"],"legalBasis":{"code":"LEGITIMATE_INTERESTS","article":"6.1.f","fideslang":"Legitimate interests"},"legitimateInterest":"Keeping former students informed about university events","recipients":["Email delivery provider (processor)"],"transfers":[{"destination":"United States","safeguard":{"code":"STANDARD_CONTRACTUAL_CLAUSES","article":"46.2.c","fideslang":"SCCs"}}],"retention":"Until the alumnus unsubscribes","protection":{"natures":["ORDINARY"],"confidentiality":"LOW","integrity":"MEDIUM","secret":false},"openPoints":[]}]}
```

The `code` members are the enum constant names, so a tool can map them back with `LegalBasis.valueOf(...)` and the
like; the `fideslang` members can fill the matching fields of a fideslang privacy declaration.

See also: [ProcessingChecker](checker.md) · [ProcessingActivity](activity.md) · [Legal bases](bases.md) ·
[Processing activities](README.md)
