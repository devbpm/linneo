---
title: "University"
parent: "Extensions"
grand_parent: "Reference"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/extension/university/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityCategories.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityDataUses.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityDataSubjects.java
-->

# University extension

Package `io.github.devbpm.linneo.extension.university` — exported, `@since 0.1.0`

Terms for universities and other higher education institutions: the data they hold about students, applicants,
staff, alumni and supporters beyond the standard `user.education.*` terms, the purposes they process them for and the
people involved. The extension is optional and not applied by default; see [Extensions](README.md).

It gives the finest detail to the student academic record: enrolment changes and transfers, assessment attempts, final
grades and averages, credit recognition, programme progress, degrees, certificates and language proficiency,
internships and mobility, means-tested aid and fee exemptions.

| Type | Description |
|---|---|
| `UniversityTaxonomy` | `public final class implements TaxonomyExtension`. `name()` returns `NAME`, `public static final String NAME = "university"`, recorded as the origin of its terms. Public no-argument constructor; no state. `contribute(builder)` adds the terms listed below. |
| `UniversityCategories` | `public final class`, private constructor: one `public static final String` per data category the extension adds (33). |
| `UniversityDataUses` | The same for its data uses (27). |
| `UniversityDataSubjects` | The same for its data subjects (13). |

The constants follow the [naming rule](../key-constants.md#naming-rule) of `Categories`: `user.education.attendance`
is `UniversityCategories.USER_EDUCATION_ATTENDANCE`, `essential.legal_obligation.statutory_reporting` is
`UniversityDataUses.ESSENTIAL_LEGAL_OBLIGATION_STATUTORY_REPORTING`. They are compile-time constants, valid in
annotations once the extension is applied.

## Applying it

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();
// Taxonomy[156 data categories, 90 data uses, 34 data subjects]
```

`Taxonomy.load()` applies it only if the application registers it; see
[Registering a shipped extension](README.md#registering-a-shipped-extension).

A model then uses the extension's keys next to the standard ones:

```java
package com.example.app;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.extension.university.UniversityCategories;

import java.time.LocalDate;

public class Absence {
    @Category(Categories.USER_UNIQUE_ID)
    private String studentNumber;

    @Category(UniversityCategories.USER_EDUCATION_ATTENDANCE)
    private LocalDate date;

    @Category(UniversityCategories.USER_EDUCATION_ATTENDANCE)
    private boolean excused;

    @Category(Categories.USER_HEALTH_AND_MEDICAL)   // the medical reason is health data, not attendance
    private String medicalReason;
}
```

```java
new Classifier(taxonomy).verify(List.of(Absence.class)).get(0).fields().forEach(f ->
        System.out.println(f.field() + ": " + f.category().key() + " " + f.category().nature() + " "
                + f.confidentiality() + " " + f.integrity()));
```

```text
studentNumber: user.unique_id ORDINARY LOW MEDIUM
date: user.education.attendance ORDINARY MEDIUM HIGH
excused: user.education.attendance ORDINARY MEDIUM HIGH
medicalReason: user.health_and_medical SPECIAL_CATEGORY HIGH HIGH
```

## Fideslang rules for custom labels

The extension follows the rules fideslang sets for custom labels, so its terms stay interoperable with other tools:

- **Extend existing classes.** Every term is a child of an existing term. The only exceptions are the two roots of
  data uses `advancement` and `research`, whose purposes no fideslang use describes.
- **The parent is the prefix of the key.** `user.education.grade.final` is a child of `user.education.grade`,
  `education.placement.career_services` of `education.placement`.
- **Data subjects are flat.** fideslang data subjects have no hierarchy, so every subject of the extension is a root:
  a doctoral candidate is `doctoral_candidate`, not a child of `student`.
- **No version information.** fideslang allows version information only in its default taxonomy ("Non-default items
  can't have version information"), so no term of the extension carries any; its origin is recorded as
  `university`.

## Data categories

The extension arranges the categories by the record they belong to: the student record, admission, support and
conduct, placements and mobility, and finance. The finest detail is in the student record.

The protection shown is the effective one in the built taxonomy. Values in **bold** are declared by the extension;
the rest are inherited from the parent (`user.education` and `user.financial` are `ORDINARY`, `MEDIUM`, `HIGH`;
`user.contact` is `ORDINARY`, `LOW`, `MEDIUM`). No category is secret, and all are personal data.

### The student record

Enrolment changes and record transfers below `user.education.enrollment`, assessment attempts, ratified final grades
and averages below `user.education.grade`, credit recognition below `user.education.course_history`, progress below
`user.education.program` and degrees, certificates and language proficiency below `user.education.accomplishment`.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `user.education.enrollment.modification` | Enrollment Change | `ORDINARY` | `MEDIUM` | `HIGH` | Changes to an enrolment after registration: courses added or withdrawn, cancellation, partial or full withdrawal. |
| `user.education.enrollment.transfer` | Record Transfer | `ORDINARY` | `MEDIUM` | `HIGH` | Transfer of the subject's record to or from another institution, with the documents exchanged. |
| `user.education.academic_standing` | Academic Standing | `ORDINARY` | `MEDIUM` | `HIGH` | Standing earned by performance: good standing, warning, probation, dismissal, honours. |
| `user.education.attendance` | Attendance | `ORDINARY` | `MEDIUM` | `HIGH` | Presence, absence and lateness of the subject in classes, exams or other sessions. |
| `user.education.coursework` | Coursework | `ORDINARY` | `MEDIUM` | `HIGH` | Work produced by the subject for assessment: submitted assignments, exam answers. |
| `user.education.grade.attempt` | Assessment Attempt | `ORDINARY` | `MEDIUM` | `HIGH` | Each sitting of an assessment: date, attempt number, result and incidents. |
| `user.education.grade.final` | Final Grade | `ORDINARY` | `MEDIUM` | `HIGH` | Final course results ratified by the examining board, as recorded in the transcript. |
| `user.education.grade.average` | Grade Average | `ORDINARY` | `MEDIUM` | `HIGH` | Grade point averages and rankings computed from the subject's results. |
| `user.education.course_history.recognition` | Credit Recognition | `ORDINARY` | `MEDIUM` | `HIGH` | Credits recognised or transferred from previous studies, work experience or other institutions, with the decision. |
| `user.education.program.progress` | Programme Progress | `ORDINARY` | `MEDIUM` | `HIGH` | Credits earned, requirements met and progress towards completing the programme. |
| `user.education.hold` | Academic Hold | `ORDINARY` | `MEDIUM` | `HIGH` | Administrative restrictions on the subject's record, such as registration or transcript holds. |
| `user.education.thesis` | Thesis | `ORDINARY` | `MEDIUM` | `HIGH` | Master's or doctoral thesis or final project: title, supervisors, submission and defence. |
| `user.education.accomplishment.degree` | Degree | `ORDINARY` | `MEDIUM` | `HIGH` | Degrees and official qualifications awarded: title, date, final grade, honours and registration number. |
| `user.education.accomplishment.certificate` | Academic Certificate | `ORDINARY` | `MEDIUM` | `HIGH` | Certificates issued about the subject's record, such as transcripts of records or diploma supplements, with their verification codes. |
| `user.education.accomplishment.language_proficiency` | Language Proficiency | `ORDINARY` | `MEDIUM` | `HIGH` | Accredited language proficiency levels, such as a certified B2 level. |

### Admission

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `user.education.application` | Admission Application | `ORDINARY` | `MEDIUM` | `HIGH` | The subject's application: programme and period applied to, status and answers. |
| `user.education.application.evaluation` | Admission Evaluation | `ORDINARY` | **`HIGH`** | `HIGH` | Reviewer scores, comments, conditions and the decision on an application. |
| `user.education.application.reference` | Reference Letter | `ORDINARY` | **`HIGH`** | `HIGH` | Recommendation or reference letters written about the subject by third parties. |

### Support, conduct and equity

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `user.education.accommodation` | Academic Accommodation | **`SPECIAL_CATEGORY`** | **`HIGH`** | `HIGH` | Adjustments granted for study or assessment, such as extra time or assistive tools, and the needs behind them. |
| `user.education.advising` | Advising | `ORDINARY` | **`HIGH`** | `HIGH` | Advisor assignments, advising notes, success plans and referrals to support services. |
| `user.education.disciplinary_record` | Disciplinary Record | `ORDINARY` | **`HIGH`** | `HIGH` | Conduct and academic integrity proceedings and sanctions concerning the subject, such as plagiarism findings or suspensions. |
| `user.education.equity_indicator` | Equity Indicator | `ORDINARY` | **`HIGH`** | `HIGH` | Flags for widening participation or equity reporting, such as first-generation student, care leaver or economic disadvantage. |
| `user.education.risk_indicator` | Academic Risk Indicator | `ORDINARY` | **`HIGH`** | `HIGH` | Early alerts, watchlists, risk scores and predictions about the subject's progress or retention. |
| `user.education.extracurricular` | Extracurricular Activity | `ORDINARY` | `MEDIUM` | `HIGH` | Clubs, societies, sports and other non-curricular involvement of the subject. |

### Placements and mobility

Internships and mobility are told apart below `user.education.placement`.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `user.education.placement` | Placement | `ORDINARY` | `MEDIUM` | `HIGH` | Internships, work placements, exchanges and other mobility undertaken during studies. |
| `user.education.placement.internship` | Internship | `ORDINARY` | `MEDIUM` | `HIGH` | A work placement: host organisation, supervisor, agreement, schedule and tasks. |
| `user.education.placement.internship.evaluation` | Internship Evaluation | `ORDINARY` | **`HIGH`** | `HIGH` | The host supervisor's assessment of the subject's performance in a placement. |
| `user.education.placement.mobility` | Mobility | `ORDINARY` | `MEDIUM` | `HIGH` | An exchange or study stay at another institution: host institution, learning agreement, recognition and mobility grant. |

### Finance and contact

Means-tested aid and fee exemptions sit below the standard financial terms `user.financial.scholarship` and
`user.financial.fee`.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `user.financial.scholarship.means_assessment` | Means Assessment | `ORDINARY` | **`HIGH`** | `HIGH` | Household income, assets and composition assessed to award means-tested aid. |
| `user.financial.fee.exemption` | Fee Exemption | `ORDINARY` | **`HIGH`** | `HIGH` | Exemptions and reductions of fees and the grounds for them, such as a large family or academic merit. |
| `user.financial.donation` | Donation | `ORDINARY` | `MEDIUM` | `HIGH` | Gifts, pledges and recurring donations made by the subject to the institution. |
| `user.financial.wealth_assessment` | Wealth Assessment | `ORDINARY` | **`HIGH`** | `HIGH` | Estimates of the subject's wealth, income and giving capacity from prospect research. |
| `user.contact.emergency_contact` | Emergency Contact | `ORDINARY` | **`MEDIUM`** | `MEDIUM` | Name, phone number and relationship of the person to contact in an emergency. |

### Why these levels

- An **academic accommodation** such as extra time in exams reveals a disability or a health condition, so it is a
  special category of data even when the record holds only the adjustment and not the diagnosis.
- **Disciplinary records, advising notes, risk indicators, admission evaluations, reference letters, internship
  evaluations, equity indicators, means assessments, fee exemptions and wealth assessments** have high
  confidentiality: they are evaluations, predictions or vulnerability data about the subject, often written by others,
  or they reveal the household's income or circumstances.
- The **emergency contact** is raised to medium confidentiality, above the rest of `user.contact`: it reveals who
  the subject's close relations are.
- The other categories inherit the medium confidentiality and high integrity of `user.education` (or
  `user.financial`).

## Where stricter data goes

A record of an extension category may contain details that reveal more than the category. Classify those fields
with the stricter standard key, not with the extension's:

| The field holds | Classify it as | Protection |
|---|---|---|
| The medical reason for an absence | `user.health_and_medical` (not `user.education.attendance`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| A disciplinary case about a criminal offence | `user.criminal_history` (not `user.education.disciplinary_record`) | `CRIMINAL_OFFENCE`, `HIGH`, `HIGH` |
| Membership of a religious or political society | `user.demographic.religious_belief` or `user.demographic.political_opinion` (not `user.education.extracurricular`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| A fee exemption or a grade adjustment granted for a disability | `user.health_and_medical.disability` (not `user.financial.fee.exemption` or `user.education.grade`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |

The same reasoning applies to other special categories, for example a society linked to a trade union
(`user.demographic.trade_union_membership`) or to sexual orientation (`user.demographic.sexual_orientation`).

## Data uses

The extension adds two roots, `advancement` and `research`, because fideslang has no purpose for fundraising or for
scientific research; every other use is a child of an existing one, including the new children of the standard
`education.academic_records`, `education.assessment` and `education.financial_aid`. Data uses have no protection
facets.

| Key | Name | Parent | Purpose |
|---|---|---|---|
| `advancement` | Advancement | (root) | Builds the institution's relationships with alumni, donors and supporters. |
| `advancement.alumni_relations` | Alumni Relations | `advancement` | Keeps in touch with former students: communications, events, networks. |
| `advancement.fundraising` | Fundraising | `advancement` | Solicits, records and acknowledges gifts and pledges. |
| `advancement.prospect_research` | Prospect Research | `advancement` | Identifies potential donors and estimates their giving capacity. |
| `education.academic_records.certification` | Certification | `education.academic_records` | Issues certificates, transcripts, diploma supplements and degree certificates. |
| `education.academic_records.credit_recognition` | Credit Recognition | `education.academic_records` | Recognises and transfers credits from previous studies or experience. |
| `education.academic_records.verification` | Qualification Verification | `education.academic_records` | Confirms a subject's qualifications to employers or other institutions on request. |
| `education.accessibility` | Accessibility | `education` | Assesses needs and provides accommodations and disability support. |
| `education.advising` | Advising | `education` | Provides academic advising, success planning and mentoring. |
| `education.assessment.examination` | Examination | `education.assessment` | Schedules and administers exam sittings, including invigilation and special arrangements. |
| `education.attendance` | Attendance | `education` | Records and monitors attendance and follows up on absences. |
| `education.discipline` | Discipline | `education` | Investigates and sanctions breaches of conduct and of academic integrity. |
| `education.financial_aid.means_assessment` | Means Assessment | `education.financial_aid` | Assesses household income and assets to award means-tested aid. |
| `education.learning_analytics` | Learning Analytics | `education` | Analyses learning data to improve teaching and student outcomes. |
| `education.placement` | Placement | `education` | Arranges internships, work placements, career services, exchanges and mobility. |
| `education.placement.career_services` | Career Services | `education.placement` | Helps students and graduates find employment: job boards, guidance, employer events. |
| `education.placement.internship` | Internships | `education.placement` | Arranges, supervises and assesses work placements with host organisations. |
| `education.placement.mobility` | Mobility | `education.placement` | Organises exchanges and study stays at other institutions, in the country or abroad. |
| `education.quality_assurance` | Quality Assurance | `education` | Evaluates and accredits programmes and teaching, including student surveys. |
| `education.recruitment` | Recruitment | `education` | Attracts prospective students: enquiries, events and interest tracking. |
| `education.student_life` | Student Life | `education` | Organises extracurricular activities, sports, societies and campus residence. |
| `education.student_support` | Student Support | `education` | Identifies students at risk and provides interventions and wellbeing support. |
| `education.thesis_supervision` | Thesis Supervision | `education` | Supervises master's and doctoral theses and manages their submission and defence. |
| `essential.legal_obligation.statutory_reporting` | Statutory Reporting | `essential.legal_obligation` | Compiles the statistics and returns that education authorities require by law. |
| `research` | Research | (root) | Carries out and manages scientific research. |
| `research.administration` | Research Administration | `research` | Manages research projects, grants, researchers and their outputs. |
| `research.participation` | Research Participation | `research` | Processes data about the participants of research studies. |

The standard taxonomy already has the core academic purposes: `education.admission`, `education.enrollment`,
`education.teaching`, `education.assessment`, `education.academic_records` and `education.financial_aid`; staff
recruitment is `employment.recruitment`.

## Data subjects

All the data subjects of the extension are flat, as fideslang data subjects have no hierarchy.

| Key | Name | Who |
|---|---|---|
| `doctoral_candidate` | Doctoral Candidate | A student enrolled in a doctoral programme. |
| `donor` | Donor | An individual who gives or pledges gifts to the institution. |
| `emergency_contact` | Emergency Contact | A person named by a student or employee to be contacted in an emergency. |
| `exchange_student` | Exchange Student | A student on an exchange, received from or sent to another institution. |
| `external_examiner` | External Examiner | An assessor from outside the institution who examines students or theses. |
| `guest` | Guest | An individual temporarily affiliated with the institution without student or employee status. |
| `mentor` | Mentor | An individual, often an alumnus or volunteer, who mentors students. |
| `philanthropic_prospect` | Philanthropic Prospect | An individual identified as a potential donor. |
| `placement_supervisor` | Placement Supervisor | The person who supervises a student at a host organisation or institution. |
| `prospective_student` | Prospective Student | An individual who has shown interest in studying at the institution. |
| `referee` | Referee | An individual who writes a reference letter about an applicant. |
| `trustee` | Trustee | A member of the institution's or its foundation's governing board. |
| `volunteer` | Volunteer | An individual who volunteers time for the institution. |

The standard subjects `student`, `applicant`, `alumnus`, `faculty_member`, `staff_member` and
`legal_representative` (Linneo) and `employee`, `next_of_kin` and `visitor` (fideslang) cover the rest. Some
extension subjects sit next to a fideslang one that is narrower: `prospect` is someone the organisation sells goods or
services to, `next_of_kin` is a relative (an emergency contact may be a friend or a neighbour) and `visitor` is
someone visiting a location (a guest is affiliated with the institution for a while). `doctoral_candidate` and
`exchange_student` are kinds of student, but as data subjects are flat they are not children of `student`: use them
when the processing is specific to them.

### Why teaching roles are not data subjects

Lecturer, teaching assistant, course coordinator, invigilator or assessor are not data subjects. The models the
extension draws on treat them as **roles a person holds in a course offering**, not as kinds of person: the person
is a `faculty_member` or a `staff_member`, and a teaching assistant is often a `student`. Classify the field that
records the role with `user.workplace.role` and the person with the subject they are. The exception is the
`external_examiner`, who belongs to no other subject of the institution.

## Standard keys for everything else

The extension adds only what has no standard key. Attributes that the education models share with the standard
taxonomy use the standard keys. Some of them have children in the extension, which describe a narrower kind of
data: `user.education.grade` (`.final`, `.average`, `.attempt`), `user.education.enrollment` (`.modification`,
`.transfer`), `user.education.course_history` (`.recognition`), `user.education.program` (`.progress`),
`user.education.accomplishment` (`.degree`, `.certificate`, `.language_proficiency`), `user.financial.scholarship`
(`.means_assessment`) and `user.financial.fee` (`.exemption`). Use the standard key for what the child does not
describe:

| Attribute | Standard key | Protection |
|---|---|---|
| Grades, GPA, scores, results | `user.education.grade` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Course and term enrolments, waiting lists | `user.education.enrollment` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Courses taken | `user.education.course_history` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Programme, major, minor | `user.education.program` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Active, on leave, graduated, withdrawn | `user.education.status` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Admission tests and language tests | `user.education.test_score` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Previous schools, class rank | `user.education.previous_education` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Degrees, certificates, badges, awards | `user.education.accomplishment` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Scholarships, grants, financial aid | `user.financial.scholarship` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Tuition and other fees | `user.financial.fee` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Disability type, degree and verification | `user.health_and_medical.disability` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Race or ethnicity | `user.demographic.race_ethnicity` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Religion | `user.demographic.religious_belief` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Gender, pronouns | `user.demographic.gender` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| Home or preferred language | `user.demographic.language` | `ORDINARY`, `LOW`, `MEDIUM` |
| Nationality, citizenship, country of birth | `user.demographic.nationality` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| Residence permit, citizenship status | `user.government_id.immigration` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| National identity and social security numbers | `user.government_id.national_identification_number`, `user.government_id.social_security_number` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| Student number, European Student Identifier, ORCID | `user.unique_id` | `ORDINARY`, `LOW`, `MEDIUM` |
| Photographs | `user.content.self_image` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| Directory and disclosure preferences | `user.privacy_preferences` | `ORDINARY`, `LOW`, `MEDIUM` |
| Job while studying, teaching licences | `user.workplace` and its children, `user.job_title` | `ORDINARY`, `LOW`, `MEDIUM` |
| Access logs of learning platforms | `user.behavior.activity_log` | `ORDINARY`, `MEDIUM`, `HIGH` |

## Sources

The terms are informed by a public model of the higher education domain, read in October 2026. The extension takes
concepts from it, not its texts or its structure.

- **Open Education API (OEAPI) v6** — the specification of persons, offerings, associations, attempts, results and
  their enumerations (attendance, personal needs, association roles, affiliations):
  <https://github.com/open-education-api/specification/tree/v6.0>.
  Copyright (c) 2017-2026 SURF U.A. Cooperative (<https://surf.nl>), licensed under the
  [European Union Public Licence v. 1.2 (EUPL-1.2)](https://github.com/open-education-api/specification/blob/master/LICENSE.md).

No OEAPI material (schemas, texts or enumerations) is reproduced in Linneo, so the EUPL does not apply to Linneo's
code; OEAPI is cited as the source of the concepts. Linneo is not affiliated with or endorsed by SURF or the OEAPI
project.

What it contributed: attendance and personal needs (accommodations). Teaching roles come from the OEAPI association
roles.

See also: [Extensions](README.md) · [Standard taxonomy](../standard-taxonomy.md) ·
[Key constants](../key-constants.md) · [Protection levels](../protection-levels.md)
