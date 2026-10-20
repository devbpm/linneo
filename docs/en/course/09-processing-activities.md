---
title: "9. Processing activities and the record"
parent: "Course"
nav_order: 9
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/LegalBasis.java
  - src/main/java/io/github/devbpm/linneo/processing/SpecialCategoryCondition.java
  - src/main/java/io/github/devbpm/linneo/processing/CriminalDataBasis.java
  - src/main/java/io/github/devbpm/linneo/processing/TransferSafeguard.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingChecker.java
  - src/main/java/io/github/devbpm/linneo/processing/CheckedActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingViolation.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingException.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingRecord.java
-->

# Module 9 — Processing activities and the record

## Learning objectives

After this module you will be able to:

- declare the processing activities of an organisation with `ProcessingActivity`: purposes, data subjects, personal
  data, lawful basis, conditions for special categories and criminal offence data, retention, recipients and
  transfers;
- choose between listing the personal data as category keys or as classified entities, and know what each brings in;
- read the violations of a `ProcessingChecker` and tell an error (unlawful as declared) from a warning (a gap in the
  record);
- verify the activities in a test and produce the record of processing activities as Markdown and JSON.

Modules 3 and 4 are required: the activities reuse the classified model of the university.

## From fields to processes

Module 5 built a RoPA-style report by hand and ended with a list of what was still missing: legal bases, recipients,
retention periods and transfers. Those are properties of **processes**, not of fields. The same `Grade.mark` is
processed to assess students, to issue certificates and to compute statistics, each time for a different purpose and
possibly on a different basis.

The `io.github.devbpm.linneo.processing` package models those processes. A `ProcessingActivity` brings together the
three dimensions of the taxonomy that module 2 introduced:

| Taxonomy | In the activity | GDPR record (art. 30.1) |
|---|---|---|
| data uses | `purposes` | b — purposes |
| data subjects | `dataSubjects` | c — categories of data subjects |
| data categories | `dataCategories` and `entities` | c — categories of personal data |

and adds what the taxonomy cannot know: the lawful basis (art. 6.1), the condition for special categories (art.
9.2), the basis for criminal offence data (art. 10), the law those bases rest on, the retention, the recipients, the
transfers and the impact assessment.

> **Data protection note.** Linneo checks *declarations*: that a basis is named where the GDPR requires one, that the
> keys exist, that the entities are classified. Whether a basis is the right one, whether an interest outweighs the
> rights of the alumni or whether a law really covers the processing is for the controller and its data protection
> officer to decide. The laws named in this module ("Higher Education Act", "Child Protection Act") are fictional.

## A first activity

The university sends a newsletter to its alumni. A first draft:

```java
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.processing.*;   // ProcessingActivity, ProcessingChecker, LegalBasis...

import static io.github.devbpm.linneo.Categories.*;

ProcessingChecker checker = new ProcessingChecker(Taxonomy.load());

ProcessingActivity draft = ProcessingActivity.builder("alumni-newsletter", "Alumni newsletter")
        .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL)
        .dataSubjects(DataSubjects.ALUMNUS)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL)
        .build();

CheckedActivity checked = checker.check(draft);
checked.violations().forEach(System.out::println);
```

```text
ERROR MISSING_LEGAL_BASIS alumni-newsletter: Personal data needs a lawful basis (GDPR article 6.1)
WARNING MISSING_RETENTION alumni-newsletter: Declare the envisaged time limit for erasure (GDPR article 30.1.f)
```

`check` never throws for a problem in the activity: it returns a `CheckedActivity` with the resolved terms, the
personal data, the protection it requires and every violation. Here `checked.isValid()` is `false`.

The two lines show the rule of the whole package:

- an **error** is something that would make the activity **unlawful as declared**, or impossible to check. Personal
  data without a lawful basis is the clearest case;
- a **warning** is a **gap in the record**: the activity may be lawful, but the record is incomplete or something
  should be reviewed. A missing retention period is one.

The university relies on its legitimate interest in keeping in touch with former students. With
`legalBasis(LegalBasis.LEGITIMATE_INTERESTS)` alone the error goes away, but a new warning appears:

```text
WARNING MISSING_LEGITIMATE_INTEREST alumni-newsletter: State the legitimate interest pursued (GDPR article 6.1.f)
WARNING MISSING_RETENTION alumni-newsletter: Declare the envisaged time limit for erasure (GDPR article 30.1.f)
```

`legitimateInterest(...)` sets the basis **and** states the interest in one call. The complete activity has no
violations:

```java
ProcessingActivity newsletter = ProcessingActivity.builder("alumni-newsletter", "Alumni newsletter")
        .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL)
        .dataSubjects(DataSubjects.ALUMNUS)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL)
        .legitimateInterest("Keeping former students informed about university events and alumni services")
        .retention("Until the alumnus unsubscribes")
        .recipients("Email delivery provider (processor)")
        .transfer("United States", TransferSafeguard.STANDARD_CONTRACTUAL_CLAUSES)
        .build();
```

The email provider stores the list in a third country, so the activity declares a **transfer** with its safeguard,
here standard contractual clauses. The checker does not judge transfers; the record lists them.

## Special categories: admissions

Applicants may ask for the places reserved for students with a disability. Admissions then processes
`user.health_and_medical.disability`, a **special category** (module 1). Article 9 prohibits processing it unless one
of the conditions of article 9.2 applies, **in addition** to the lawful basis. Without one:

```java
ProcessingActivity admissions = ProcessingActivity.builder("admissions", "Admissions")
        .purposes(DataUses.EDUCATION_ADMISSION)
        .dataSubjects(DataSubjects.APPLICANT)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL, USER_EDUCATION_PREVIOUS_EDUCATION,
                USER_EDUCATION_TEST_SCORE, USER_HEALTH_AND_MEDICAL_DISABILITY)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .legalReference("Higher Education Act, art. 12")
        .retention("Until the end of the admission cycle")
        .build();
```

```text
ERROR MISSING_SPECIAL_CATEGORY_CONDITION admissions: Special categories of data (user.health_and_medical.disability) need a condition of GDPR article 9.2
WARNING IMPACT_ASSESSMENT_RECOMMENDED admissions: The activity involves special categories of data; GDPR article 35 may require an impact assessment
```

The error names the category that triggered it. The fix is a `SpecialCategoryCondition` — the university's
admissions law sets aside places for students with a disability, so `SUBSTANTIAL_PUBLIC_INTEREST` (art. 9.2.g) — and
a reference to the impact assessment the data protection officer carried out:

```java
        .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
        .legalReference("Higher Education Act, arts. 12 and 31")
        .impactAssessment("DPIA-2026-01")
```

Article 9.2.g, like the public interest basis of article 6.1.e, must rest on law: `requiresLegalReference()` is
`true` for both. Without `legalReference` the checker would add the warning `MISSING_LEGAL_REFERENCE` naming both
articles. One `legalReference` text covers every basis of the activity, so name all the laws involved.

The reference page [Legal bases](../reference/processing/bases.md) lists every constant with its article and its
fideslang name.

## Criminal offence data: staff screening

Before hiring someone for a post in contact with minors (summer camps, school outreach), the university
checks that the candidate has no convictions for sexual offences. `user.criminal_history` is **criminal offence
data** (art. 10), which needs its own basis:

```text
ERROR MISSING_CRIMINAL_DATA_BASIS staff-screening: Criminal offence data (user.criminal_history) needs a basis of GDPR article 10
WARNING MISSING_LEGAL_REFERENCE staff-screening: GDPR article(s) 6.1.c rest on Union or Member State law; name it as the legal reference
WARNING IMPACT_ASSESSMENT_RECOMMENDED staff-screening: The activity involves criminal offence data; GDPR article 35 may require an impact assessment
```

`CriminalDataBasis` has two values: `OFFICIAL_AUTHORITY` and `AUTHORISED_BY_LAW`. The screening is required by the
(fictional) Child Protection Act, so the basis is `AUTHORISED_BY_LAW` and the act is the legal reference.

## Keys or entities: enrollment

The personal data of an activity can be listed as category keys (`dataCategories`), as classified model classes
(`entities`), or both. For an entity, the checker runs the `Classifier` of module 4 and takes the categories of its
**personal fields**, with the levels declared on each field:

- add a personal field to `Enrollment` and every activity that lists `Enrollment` processes it, without touching the
  activities;
- an entity with classification errors makes the activity fail (`ENTITY_NOT_CLASSIFIED`);
- aggregated and anonymized fields, such as those of `CourseStatistics`, are not personal data and are left out;
- a field whose confidentiality was raised (`Student.counsellingNotes`) raises the protection of the activity.

The flip side is that an entity brings **all** its personal fields. If enrollment listed `Student.class`:

```java
ProcessingActivity enrollment = ProcessingActivity.builder("enrollment", "Student enrollment")
        .purposes(DataUses.EDUCATION_ENROLLMENT)
        .dataSubjects(DataSubjects.STUDENT)
        .entities(Student.class, Enrollment.class)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .legalReference("Higher Education Act, art. 12")
        .retention("Six years after the student leaves")
        .build();
```

```text
ERROR MISSING_SPECIAL_CATEGORY_CONDITION enrollment: Special categories of data (user.health_and_medical.disability) need a condition of GDPR article 9.2
WARNING IMPACT_ASSESSMENT_RECOMMENDED enrollment: The activity involves special categories of data; GDPR article 35 may require an impact assessment
```

`Student.disabilityDegree` came in with the entity, together with the counselling notes and the analytics id.
Enrollment does not use them, so the university lists `Enrollment` and declares the few student fields it does use
as keys. Note also that `Enrollment` extends `AuditedEntity`, whose `createdBy` is the username of the staff member
who created the row: it adds `user.account.username` to the activity. An entity holds what it holds, not only data
about the activity's subjects; review the categories the checker reports.

## The university's activities

The four activities in one class, next to the model:

```java
package edu.example.university;

import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.processing.CriminalDataBasis;
import io.github.devbpm.linneo.processing.LegalBasis;
import io.github.devbpm.linneo.processing.ProcessingActivity;
import io.github.devbpm.linneo.processing.SpecialCategoryCondition;
import io.github.devbpm.linneo.processing.TransferSafeguard;
import java.util.List;

import static io.github.devbpm.linneo.Categories.*;

/** The processing activities of the university. */
public final class UniversityActivities {

    private UniversityActivities() {
    }

    public static final ProcessingActivity ENROLLMENT = ProcessingActivity.builder("enrollment", "Student enrollment")
            .description("Registers admitted students in a programme and an academic year, and charges the fees.")
            .purposes(DataUses.EDUCATION_ENROLLMENT)
            .dataSubjects(DataSubjects.STUDENT)
            .entities(Enrollment.class)
            .dataCategories(USER_NAME, USER_GOVERNMENT_ID_NATIONAL_IDENTIFICATION_NUMBER, USER_CONTACT_EMAIL)
            .legalBasis(LegalBasis.PUBLIC_INTEREST)
            .legalReference("Higher Education Act, art. 12")
            .recipients("Bank that collects the tuition fees")
            .build();

    public static final ProcessingActivity ADMISSIONS = ProcessingActivity.builder("admissions", "Admissions")
            .description("Receives and assesses applications, including requests for the disability quota.")
            .purposes(DataUses.EDUCATION_ADMISSION)
            .dataSubjects(DataSubjects.APPLICANT)
            .dataCategories(USER_NAME, USER_CONTACT_EMAIL, USER_EDUCATION_PREVIOUS_EDUCATION,
                    USER_EDUCATION_TEST_SCORE, USER_HEALTH_AND_MEDICAL_DISABILITY)
            .legalBasis(LegalBasis.PUBLIC_INTEREST)
            .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
            .legalReference("Higher Education Act, arts. 12 and 31")
            .retention("Until the end of the admission cycle; admitted applicants move to the student record")
            .recipients("Admissions board")
            .impactAssessment("DPIA-2026-01")
            .build();

    public static final ProcessingActivity STAFF_SCREENING = ProcessingActivity.builder("staff-screening",
                    "Staff screening")
            .description("Checks that candidates for posts in contact with minors have no sexual offence convictions.")
            .purposes(DataUses.EMPLOYMENT_RECRUITMENT)
            .dataSubjects(DataSubjects.JOB_APPLICANT)
            .dataCategories(USER_NAME, USER_GOVERNMENT_ID_NATIONAL_IDENTIFICATION_NUMBER, USER_CRIMINAL_HISTORY)
            .legalBasis(LegalBasis.LEGAL_OBLIGATION)
            .criminalDataBasis(CriminalDataBasis.AUTHORISED_BY_LAW)
            .legalReference("Child Protection Act, art. 13")
            .retention("Until the hiring decision; afterwards only whether the certificate was clear")
            .impactAssessment("DPIA-2026-02")
            .build();

    public static final ProcessingActivity ALUMNI_NEWSLETTER = ProcessingActivity.builder("alumni-newsletter",
                    "Alumni newsletter")
            .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL)
            .dataSubjects(DataSubjects.ALUMNUS)
            .dataCategories(USER_NAME, USER_CONTACT_EMAIL)
            .legitimateInterest("Keeping former students informed about university events and alumni services")
            .retention("Until the alumnus unsubscribes")
            .recipients("Email delivery provider (processor)")
            .transfer("United States", TransferSafeguard.STANDARD_CONTRACTUAL_CLAUSES)
            .build();

    public static List<ProcessingActivity> all() {
        return List.of(ENROLLMENT, ADMISSIONS, STAFF_SCREENING, ALUMNI_NEWSLETTER);
    }
}
```

What the checker makes of each one:

| Activity | Personal data | Natures | Protection required | Violations |
|---|---|---|---|---|
| `enrollment` | 9 categories, 6 of them from `Enrollment` | `ORDINARY` | confidentiality `MEDIUM`, integrity `HIGH` | warning `MISSING_RETENTION` |
| `admissions` | 5 categories | `ORDINARY`, `SPECIAL_CATEGORY` | `HIGH`, `HIGH` | none |
| `staff-screening` | 3 categories | `ORDINARY`, `CRIMINAL_OFFENCE` | `HIGH`, `HIGH` | none |
| `alumni-newsletter` | 2 categories | `ORDINARY` | `LOW`, `MEDIUM` | none |

The enrollment activity has been left without a retention period on purpose: a warning does not stop the record.

## Verifying in a test

As with the classifier, the point is to run the check on every build. `verify` checks all the activities and throws
a `ProcessingException` listing **every error of every activity**; when there is none it returns the
`ProcessingRecord`:

```java
package edu.example.university;

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.processing.ProcessingChecker;
import io.github.devbpm.linneo.processing.ProcessingRecord;
import io.github.devbpm.linneo.processing.ProcessingViolation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class ProcessingRecordTest {

    @Test
    void recordOfProcessingActivities() throws IOException {
        ProcessingRecord record = new ProcessingChecker(Taxonomy.load()).verify(UniversityActivities.all());

        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/record-of-processing.md"), record.toMarkdown("Example University"));
        Files.writeString(Path.of("target/record-of-processing.json"), record.toJson());

        List<ProcessingViolation> openPoints = record.activities().stream()
                .flatMap(activity -> activity.warnings().stream())
                .toList();
        openPoints.forEach(System.out::println);
    }
}
```

With the drafts of admissions and staff screening above (no article 9.2 condition, no article 10 basis) in the list,
the test fails with:

```text
io.github.devbpm.linneo.processing.ProcessingException: 2 processing error(s):
 - ERROR MISSING_SPECIAL_CATEGORY_CONDITION admissions: Special categories of data (user.health_and_medical.disability) need a condition of GDPR article 9.2
 - ERROR MISSING_CRIMINAL_DATA_BASIS staff-screening: Criminal offence data (user.criminal_history) needs a basis of GDPR article 10
```

Each line is `<SEVERITY> <CODE> <activity key>: <message>`. Only errors are listed; the warnings of those activities
stay in their `CheckedActivity`. Two activities with the same key are an error too (`DUPLICATE_ACTIVITY`), because
the key identifies the activity in the record. Use `Taxonomy.load()`, as in module 4, so that terms of your
extension are known.

The [codes table](../reference/processing/checker.md#codes) lists every code with its severity and message.

## The record

With the four final activities the test passes and writes the record. **Its texts are in English** — the row labels,
`None declared`, `Open points:` and the messages — whatever the language of your application; the names of the terms
come from the taxonomy and the rest is printed as you declared it. The start of `record-of-processing.md`:

```markdown
# Example University

## Student enrollment

`enrollment`: Registers admitted students in a programme and an academic year, and charges the fees.

| Record item | Content |
|---|---|
| Purposes (art. 30.1.b) | Enrollment (`education.enrollment`) |
| Lawful basis (art. 6.1) | Art. 6.1.e: Public interest |
| Legal reference | Higher Education Act, art. 12 |
| Data subjects (art. 30.1.c) | Student (`student`) |
| Personal data (art. 30.1.c) | Account Username (`user.account.username`); User Contact Email (`user.contact.email`); Enrollment (`user.education.enrollment`); Program (`user.education.program`); Fee (`user.financial.fee`); Scholarship (`user.financial.scholarship`); National Identification Number (`user.government_id.national_identification_number`); Name (`user.name`); Record Reference (`user.unique_id.record_reference`) |
| Recipients (art. 30.1.d) | Bank that collects the tuition fees |
| Transfers (art. 30.1.e) | None declared |
| Retention (art. 30.1.f) | Not declared |
| Required protection (art. 30.1.g) | Confidentiality Medium, integrity High |
| Impact assessment (art. 35) | None |

Open points:

- Declare the envisaged time limit for erasure (GDPR article 30.1.f)

## Admissions
…
```

and the staff screening section:

```markdown
## Staff screening

`staff-screening`: Checks that candidates for posts in contact with minors have no sexual offence convictions.

| Record item | Content |
|---|---|
| Purposes (art. 30.1.b) | Employment Recruitment (`employment.recruitment`) |
| Lawful basis (art. 6.1) | Art. 6.1.c: Legal obligations |
| Criminal offence data (art. 10) | Art. 10: Authorised by law |
| Legal reference | Child Protection Act, art. 13 |
| Data subjects (art. 30.1.c) | Job Applicant (`job_applicant`) |
| Personal data (art. 30.1.c) | Criminal History (`user.criminal_history`, criminal offence data); National Identification Number (`user.government_id.national_identification_number`); Name (`user.name`) |
| Recipients (art. 30.1.d) | None declared |
| Transfers (art. 30.1.e) | None declared |
| Retention (art. 30.1.f) | Until the hiring decision; afterwards only whether the certificate was clear |
| Required protection (art. 30.1.g) | Confidentiality High, integrity High |
| Impact assessment (art. 35) | DPIA-2026-02 |
```

The "Lawful basis" row shows the fideslang name of the basis (`Legal obligations`), the "Required protection" row
the highest levels of the data, as input for the description of security measures of article 30.1.g. Every row is
explained in the reference page [ProcessingRecord](../reference/processing/record.md).

`toJson()` gives the same content for tools. The newsletter alone, as `checker.verify(ALUMNI_NEWSLETTER).toJson()`:

```json
{"activities":[{"key":"alumni-newsletter","name":"Alumni newsletter","purposes":["marketing.communications.email"],"dataSubjects":["alumnus"],"dataCategories":["user.contact.email","user.name"],"legalBasis":{"code":"LEGITIMATE_INTERESTS","article":"6.1.f","fideslang":"Legitimate interests"},"legitimateInterest":"Keeping former students informed about university events and alumni services","recipients":["Email delivery provider (processor)"],"transfers":[{"destination":"United States","safeguard":{"code":"STANDARD_CONTRACTUAL_CLAUSES","article":"46.2.c","fideslang":"SCCs"}}],"retention":"Until the alumnus unsubscribes","protection":{"natures":["ORDINARY"],"confidentiality":"LOW","integrity":"MEDIUM","secret":false},"openPoints":[]}]}
```

The record is a starting point for the data protection officer, generated from the same declarations the build
checks. It does not replace her review: she still decides whether the bases are right and completes what the record
cannot know, such as the controller's contact details.

## Common mistakes

- **Expecting Linneo to decide the lawful basis.** It checks that one is declared and that the conditions the data
  needs are there, not that they are the right ones.
- **Listing an entity that holds more than the activity processes.** `Student.class` in enrollment drags in the
  disability degree and the counselling notes. Declare the categories by key, or split the entity.
- **Calling `legalBasis(...)` after `legitimateInterest(...)`.** The last call wins: the basis changes and the
  interest stays, which the checker reports as `UNNEEDED_CONDITION` and the record leaves out.
- **Declaring a condition "just in case".** `EXPLICIT_CONSENT` on an activity without special category data is the
  warning `UNNEEDED_CONDITION`: the record should state what applies, not everything that might.
- **Treating warnings as noise.** They are the open points of the record; each one is something the data protection
  officer will ask about.
- **Checking with `Taxonomy.standard()` when the activities use extension terms** → `UNKNOWN_KEY`.
- **Reusing a key for a second activity** → `DUPLICATE_ACTIVITY`. Keys identify activities in the record and in the
  tools that read the JSON; keep them stable.

## Exercises

**Exercise 9.1 — Predict.** The university adds an assessment activity:

```java
ProcessingActivity grading = ProcessingActivity.builder("grading", "Assessment and grading")
        .purposes(DataUses.EDUCATION_ASSESSMENT)
        .dataSubjects(DataSubjects.STUDENT, DataSubjects.FACULTY_MEMBER)
        .entities(Enrollment.class, Grade.class)
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .retention("Permanently, as part of the academic record")
        .build();
```

Without running it, say which violations `check` reports, whether the activity is valid, and the confidentiality
and integrity it requires. Then run it.

**Exercise 9.2 — Read and fix.** A colleague's branch makes the test fail with this message. For each line, say what
is wrong and how to fix it.

```text
3 processing error(s):
 - ERROR UNKNOWN_KEY staff-screening: Unknown data subject 'job_applicants'
 - ERROR MISSING_CRIMINAL_DATA_BASIS staff-screening: Criminal offence data (user.criminal_history) needs a basis of GDPR article 10
 - ERROR DUPLICATE_ACTIVITY enrollment: Another activity of the record has the key 'enrollment'
```

(The third line comes from a new activity, "Student enrollment (fees)", that processes the tuition fee payments.)

**Exercise 9.3 — From legitimate interests to consent.** The university decides to ask alumni for consent before
sending the newsletter. A colleague adds `.legalBasis(LegalBasis.CONSENT)` after the `legitimateInterest(...)` call
in `ALUMNI_NEWSLETTER`. What does `check` report, and what does the record show about the legitimate interest? What
is the right change?

**Exercise 9.4 — Close the open points.** Give the enrollment activity a retention period and add a test that fails
when the record has any open point, with a message that lists them.

**Exercise 9.5 — Staff administration.** Declare a "Staff administration" activity (purpose `DataUses.EMPLOYMENT`,
subjects `EMPLOYEE` and `STAFF_MEMBER`, entity `Staff`, basis `CONTRACT`, a retention). Predict what the checker
reports, given the `Staff` class of module 3, and decide how to fix it.

## Self-check quiz

1. Does a missing retention period make `verify` throw?
2. An activity processes `user.health_and_medical.disability` on the basis of consent. What else must it declare?
3. What does `legitimateInterest("…")` do to the lawful basis?
4. Why does an activity that lists `CourseStatistics` as its only entity fail with `MISSING_PERSONAL_DATA`?
5. Which bases make the checker ask for a `legalReference`?
6. In which language are the texts of the Markdown record?
7. Does a valid record mean that the processing is lawful?

## Further reading

- [Reference — Processing activities](../reference/processing/README.md)
- [Reference — ProcessingActivity](../reference/processing/activity.md)
- [Reference — Legal bases](../reference/processing/bases.md)
- [Reference — ProcessingChecker](../reference/processing/checker.md): every code with its message
- [Reference — ProcessingRecord](../reference/processing/record.md)

---

← Previous: [Module 8 — DataHub and Apache Atlas](08-datahub-and-atlas.md) · Back to the [course overview](README.md)

<details markdown="block">
<summary><strong>Solutions</strong></summary>

**Exercise 9.1**

One warning, and the activity is valid:

```text
WARNING MISSING_LEGAL_REFERENCE grading: GDPR article(s) 6.1.e rest on Union or Member State law; name it as the legal reference
```

The public interest basis rests on law, and the activity names none. The personal data comes from the two entities:
`user.account.username` (from `AuditedEntity.createdBy`), `user.education.course_history`,
`user.education.enrollment`, `user.education.grade`, `user.education.program`, `user.financial.fee`,
`user.financial.scholarship` and `user.unique_id.record_reference`, all ordinary, so no condition is needed and no
impact assessment is suggested. The protection required is confidentiality `MEDIUM` and integrity `HIGH` (the
integrity of grades). Add `.legalReference("Higher Education Act, art. 12")`.

**Exercise 9.2**

- `job_applicants` is not a key: the data subject is `job_applicant`. Use `DataSubjects.JOB_APPLICANT` and the
  compiler catches typos.
- The screening lost its `.criminalDataBasis(CriminalDataBasis.AUTHORISED_BY_LAW)`. Without it, processing criminal
  offence data is unlawful as declared.
- The new fees activity reuses the key `enrollment`. Give it its own key, for example `tuition-fees`. The error is
  reported on the second activity with the key; the first one is left alone.

**Exercise 9.3**

The check reports one warning:

```text
WARNING UNNEEDED_CONDITION alumni-newsletter: A legitimate interest is stated but the lawful basis is article 6.1.a
```

The basis is now `CONSENT`, because the last call wins, and the record shows `Art. 6.1.a: Consent` with no
"Legitimate interest" row: the record only shows the interest when the basis is legitimate interests. The right
change is to replace `.legitimateInterest(...)` with `.legalBasis(LegalBasis.CONSENT)`, and to keep the proof of
consent where the newsletter subscriptions are stored.

**Exercise 9.4**

Add `.retention("Six years after the student leaves the university")` to `ENROLLMENT`, and:

```java
@Test
void noOpenPoints() {
    ProcessingRecord record = new ProcessingChecker(Taxonomy.load()).verify(UniversityActivities.all());
    List<ProcessingViolation> openPoints = record.activities().stream()
            .flatMap(activity -> activity.warnings().stream())
            .toList();
    assertTrue(openPoints.isEmpty(), () -> "Close the open points of the record: " + openPoints);
}
```

With the retention added, `openPoints` is empty.

**Exercise 9.5**

```text
ERROR MISSING_CRIMINAL_DATA_BASIS staff-administration: Criminal offence data (user.criminal_history) needs a basis of GDPR article 10
WARNING IMPACT_ASSESSMENT_RECOMMENDED staff-administration: The activity involves criminal offence data; GDPR article 35 may require an impact assessment
```

`Staff.sexOffenceCertificateClear` is criminal offence data, so listing `Staff` brings it into staff administration.
The activity also requires confidentiality `HIGH`, integrity `HIGH` and includes secrets (`passwordHash`). Two fixes
are possible: if staff administration really keeps the result of the screening, declare
`.criminalDataBasis(CriminalDataBasis.AUTHORISED_BY_LAW)`, the legal reference and the impact assessment; if it does
not, declare the categories it does process by key instead of listing `Staff`, and leave criminal offence data to the
screening activity. The second keeps each activity to what it needs.

**Quiz**

1. No. `MISSING_RETENTION` is a warning: the record shows `Not declared` and lists it as an open point.
2. A `SpecialCategoryCondition`, for example `EXPLICIT_CONSENT` (art. 9.2.a). Without it, `check` reports
   `MISSING_SPECIAL_CATEGORY_CONDITION`.
3. It sets the basis to `LEGITIMATE_INTERESTS` and states the interest.
4. Its fields are reference data or aggregated grades, and aggregated data is not personal data, so the activity
   processes no personal data at all.
5. `LEGAL_OBLIGATION` and `PUBLIC_INTEREST`; the article 9.2 conditions b, g, h, i and j when there is special
   category data; `AUTHORISED_BY_LAW` when there is criminal offence data.
6. English, whatever the language of the application. Term names come from the taxonomy and the rest is printed as
   declared.
7. No. It means every activity declares what the GDPR asks for and that its keys and entities check out. Whether
   the declarations are true and the bases appropriate is the controller's assessment.

</details>
