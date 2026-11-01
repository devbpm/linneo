---
title: "Human resources"
parent: "Extensions"
grand_parent: "Reference"
nav_order: 5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/HumanResourcesTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/HumanResourcesCategories.java
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/HumanResourcesDataUses.java
  - src/main/java/io/github/devbpm/linneo/extension/humanresources/HumanResourcesDataSubjects.java
-->

# Human resources extension

Package `io.github.devbpm.linneo.extension.humanresources` — exported, `@since 0.1.0`

Terms for human resources: the employment relationship from recruitment to termination, pay, working time, leave,
performance, training, workplace monitoring and internal reporting, the human resources purposes and the people
involved. The extension is optional and not applied by default; see [Extensions](README.md).

It is informed by the human resources and payroll areas of the Common Data Model (CDM) (see [Sources](#sources)); no CDM
material is reproduced.

The extension adds `user.employment` for the employment relationship managed by human resources, next to the standard
`user.workplace`, which says where and in which team, unit or role the subject works. Every category extends `user` or
`user.health_and_medical`, so the `Classifier` treats all of them as personal data.

| Type | Description |
|---|---|
| `HumanResourcesTaxonomy` | `public final class implements TaxonomyExtension`. `name()` returns `NAME`, `public static final String NAME = "human_resources"`, recorded as the origin of its terms. Public no-argument constructor; no state. `contribute(builder)` adds the terms listed below. |
| `HumanResourcesCategories` | `public final class`, private constructor: one `public static final String` per data category the extension adds (21). |
| `HumanResourcesDataUses` | The same for its data uses (15). |
| `HumanResourcesDataSubjects` | The same for its data subjects (6). |

The constants follow the [naming rule](../key-constants.md#naming-rule) of `Categories`: `user.employment.payroll` is
`HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL`, `employment.payroll` is `HumanResourcesDataUses.EMPLOYMENT_PAYROLL`.
They are compile-time constants, valid in annotations once the extension is applied.

## Applying it

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new HumanResourcesTaxonomy()).build();
// Taxonomy[147 data categories, 78 data uses, 28 data subjects]
```

The extension shares no key with the other shipped extensions, so it can be applied with any of them, in the same
builder:

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new HumanResourcesTaxonomy())
        .extend(new ProjectOperationsTaxonomy())   // io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsTaxonomy
        .build();
```

`Taxonomy.load()` applies it only if the application registers it; see [Registering a shipped
extension](README.md#registering-a-shipped-extension).

A model then uses the extension's keys next to the standard ones:

```java
package com.example.app;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.extension.humanresources.HumanResourcesCategories;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SickLeave {
    @Category(Categories.USER_UNIQUE_ID)
    private String employeeNumber;

    @Category(HumanResourcesCategories.USER_HEALTH_AND_MEDICAL_SICK_LEAVE)
    private LocalDate from;

    @Category(HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL)
    private BigDecimal subsidy;
}
```

```java
new Classifier(taxonomy).verify(List.of(SickLeave.class)).get(0).fields().forEach(f ->
        System.out.println(f.path() + ": " + f.category().key() + " " + f.category().nature() + " "
                + f.confidentiality() + " " + f.integrity()));
```

```text
employeeNumber: user.unique_id ORDINARY LOW MEDIUM
from: user.health_and_medical.sick_leave SPECIAL_CATEGORY HIGH HIGH
subsidy: user.employment.payroll ORDINARY HIGH HIGH
```

## Fideslang rules for custom labels

The extension follows the rules fideslang sets for custom labels, so its terms stay interoperable with other tools:

- **Extend existing classes.** Every term is a child of an existing term; every use is a child of the standard
  `employment` use (`employment.recruitment.background_check` sits below the standard `employment.recruitment`).
- **The parent is the prefix of the key.** `user.employment.payroll` is a child of `user.employment`.
- **Data subjects are flat.** fideslang data subjects have no hierarchy, so every subject of the extension is a root.
- **No version information.** fideslang allows version information only in its default taxonomy, so no term of the
  extension carries any; its origin is recorded as `human_resources`.

## Data categories

The protection shown is the effective one in the built taxonomy. Values in **bold** are declared by the extension; the
rest are inherited from the parent (`user` is `ORDINARY`, `LOW`, `MEDIUM`; `user.employment` is `ORDINARY`, `MEDIUM`,
`MEDIUM`; `user.health_and_medical` is `SPECIAL_CATEGORY`, `HIGH`, `HIGH`). `user.employment.payroll.garnishment` and
`user.employment.payroll.tax_withholding` inherit `HIGH` and `HIGH` from `user.employment.payroll`. No category is
secret, and all are personal data.

### The employment relationship

The new branch `user.employment`, with the recruitment, contract, pay, working time, leave, performance and termination
records.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `user.employment` | Employment Data | `ORDINARY` | **`MEDIUM`** | `MEDIUM` | Data about the subject's employment or engagement with the organization, as managed by human resources. |
| `user.employment.background_check` | Background Check | `ORDINARY` | **`HIGH`** | `MEDIUM` | Pre-employment verification of the subject's identity, qualifications, work history and references. |
| `user.employment.benefit` | Benefit | `ORDINARY` | `MEDIUM` | `MEDIUM` | Enrolment in benefit plans, such as pensions, insurance, meal vouchers or flexible remuneration, and the beneficiaries named. |
| `user.employment.compensation` | Compensation | `ORDINARY` | **`HIGH`** | **`HIGH`** | Salary, pay grade, allowances, bonuses, salary reviews and pay-for-performance awards. |
| `user.employment.contract` | Employment Contract | `ORDINARY` | `MEDIUM` | **`HIGH`** | Contract type, start and end dates, working hours, probation and the changes to these terms. |
| `user.employment.disciplinary_record` | Disciplinary Record | `ORDINARY` | **`HIGH`** | `MEDIUM` | Warnings, disciplinary proceedings and sanctions concerning the subject. |
| `user.employment.leave` | Leave and Absence | `ORDINARY` | `MEDIUM` | `MEDIUM` | Holidays, leave requests, balances and absences, without their medical reasons. |
| `user.employment.monitoring` | Workplace Monitoring | `ORDINARY` | **`HIGH`** | `MEDIUM` | Data from monitoring the subject at work: use of work devices and email, video surveillance and geolocation. |
| `user.employment.payroll` | Payroll | `ORDINARY` | **`HIGH`** | **`HIGH`** | Pay statements, earnings, deductions, tax withholdings and social security contributions for each pay period. |
| `user.employment.payroll.garnishment` | Wage Garnishment | `ORDINARY` | `HIGH` | `HIGH` | Court or administrative orders to withhold part of the subject's pay, and the amounts withheld. |
| `user.employment.payroll.tax_withholding` | Tax Withholding Data | `ORDINARY` | `HIGH` | `HIGH` | Personal and family situation the subject declares to compute income tax withholding, such as dependants or pension payments. |
| `user.employment.performance` | Performance Evaluation | `ORDINARY` | **`HIGH`** | `MEDIUM` | Goals, reviews, ratings, feedback and calibration of the subject's performance. |
| `user.employment.recruitment` | Recruitment | `ORDINARY` | `MEDIUM` | `MEDIUM` | The subject's job applications: CV, positions applied for, interviews and their status. |
| `user.employment.recruitment.assessment` | Candidate Assessment | `ORDINARY` | **`HIGH`** | `MEDIUM` | Interview notes, test results, scores and the hiring decision about the subject. |
| `user.employment.termination` | Termination | `ORDINARY` | **`HIGH`** | `MEDIUM` | End of the subject's employment: reason, notice, dismissal letter, settlement and severance. |
| `user.employment.training` | Training | `ORDINARY` | `MEDIUM` | `MEDIUM` | Courses, attendance, results and certifications the subject obtains through the organization. |
| `user.employment.whistleblowing` | Whistleblowing Report | `ORDINARY` | **`HIGH`** | **`HIGH`** | Reports made through the internal reporting channel by or about the subject, and their investigation. |
| `user.employment.working_time` | Working Time Record | `ORDINARY` | `MEDIUM` | **`HIGH`** | Daily start and end of the subject's working day, breaks and overtime. |
| `user.employment.workplace_adjustment` | Workplace Adjustment | **`SPECIAL_CATEGORY`** | **`HIGH`** | **`HIGH`** | Adjustments made to the subject's job, schedule or workplace for a disability, a health condition or a pregnancy, and the needs behind them. |

### Health at work

Two children of the standard `user.health_and_medical`.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `user.health_and_medical.occupational_health` | Occupational Health | `SPECIAL_CATEGORY` | `HIGH` | `HIGH` | Health surveillance, fitness-for-work assessments, workplace accidents and occupational diseases of the subject. |
| `user.health_and_medical.sick_leave` | Sick Leave | `SPECIAL_CATEGORY` | `HIGH` | `HIGH` | Periods of temporary incapacity for work, medical certificates and return-to-work reports. |

### Why these levels

- **Sick leave and occupational health** are health data, special categories with the maximum protection: the fact of
  being on sick leave already concerns health, and the Spanish occupational risk prevention law (Ley 31/1995, article
  22) keeps health surveillance results confidential.
- A **workplace adjustment** reveals a disability, a health condition or a pregnancy, so it is a special category of
  data, with the maximum protection.
- **Compensation and payroll** have high confidentiality and integrity, and a **whistleblowing report** too: the law on
  the protection of reporting persons (Ley 2/2023) requires the identity of the reporting person to be kept confidential
  and the reports to be recorded.
- **Background checks, candidate assessments, performance evaluations, disciplinary records, terminations and workplace
  monitoring** have high confidentiality; LOPDGDD articles 87 to 91 limit the monitoring of workers' devices, video
  surveillance and geolocation.
- The **employment contract and the working time record** have high integrity: the Spanish Workers' Statute (article
  34.9) requires the daily record of working time to be kept for four years.
- The other categories inherit the medium confidentiality of `user.employment`.

## Where stricter data goes

A record of an extension category may contain details that reveal more than the category. Classify those fields with the
stricter key, not with the extension's:

| The field holds | Classify it as | Protection |
|---|---|---|
| The medical reason for a leave | `user.health_and_medical.sick_leave` (not `user.employment.leave`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| The disability degree declared for tax withholding | `user.health_and_medical.disability` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Health insurance numbers | `user.health_and_medical.insurance_beneficiary_id` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| A certificate of no criminal or sexual offence convictions, or a dismissal for a criminal offence | `user.criminal_history` | `CRIMINAL_OFFENCE`, `HIGH`, `HIGH` |
| A sanction imposed by an authority rather than by the employer | `user.administrative_sanction` (not `user.employment.disciplinary_record`) | `ORDINARY`, `HIGH`, `HIGH` |

## Data uses

Every use is a child of the standard `employment` use (`employment.recruitment.background_check` sits below the standard
`employment.recruitment`). Data uses have no protection facets.

| Key | Name | Parent | Purpose |
|---|---|---|---|
| `employment.compensation` | Compensation and Benefits | `employment` | Sets pay, reviews salaries, awards bonuses and administers benefit plans. |
| `employment.discipline` | Discipline | `employment` | Investigates and sanctions breaches of workplace rules. |
| `employment.equality` | Equality and Diversity | `employment` | Prepares equality plans, pay registers and diversity reports. |
| `employment.internal_reporting` | Internal Reporting Channel | `employment` | Receives and investigates reports of breaches and protects the people who report them. |
| `employment.learning` | Learning and Development | `employment` | Plans, delivers and records training and career development. |
| `employment.leave` | Leave Management | `employment` | Manages holidays, leave and absences. |
| `employment.monitoring` | Workplace Monitoring | `employment` | Monitors work devices, premises and vehicles within the limits the law sets. |
| `employment.occupational_safety` | Occupational Health and Safety | `employment` | Prevents occupational risks, surveys workers' health and investigates accidents. |
| `employment.offboarding` | Offboarding | `employment` | Ends employment: notice, settlement, certificates and the withdrawal of access and equipment. |
| `employment.onboarding` | Onboarding | `employment` | Prepares the start of employment: contract, registration, accounts, equipment and induction. |
| `employment.payroll` | Payroll | `employment` | Calculates and pays salaries, withholds taxes and contributions and files the related returns. |
| `employment.performance` | Performance Management | `employment` | Sets goals, evaluates performance and plans development. |
| `employment.recruitment.background_check` | Background Check | `employment.recruitment` | Verifies candidates' identity, qualifications and history before hiring. |
| `employment.time_and_attendance` | Time and Attendance | `employment` | Records working time, shifts and overtime as the law requires. |
| `employment.workforce_planning` | Workforce Planning | `employment` | Designs the organization, positions and headcount and plans succession. |

## Data subjects

All the data subjects of the extension are flat, as fideslang data subjects have no hierarchy.

| Key | Name | Who |
|---|---|---|
| `dependant` | Dependant | A person who depends on a worker and is covered by a benefit or declared for tax purposes. |
| `former_employee` | Former Employee | An individual whose employment with the organization has ended. |
| `reporting_person` | Reporting Person | An individual who reports breaches through the organization's internal reporting channel. |
| `retiree` | Retiree | A former employee who receives a pension or another retirement benefit from the organization. |
| `temporary_agency_worker` | Temporary Agency Worker | A worker hired by a temporary work agency and assigned to the organization. |
| `worker_representative` | Worker Representative | A worker elected or appointed to represent the workforce, such as a works council member. |

## Standard keys for everything else

The extension adds only what has no standard key. What the standard taxonomy already covers keeps its standard key:

| Attribute | Standard key | Protection |
|---|---|---|
| Names, addresses and contacts | `user.name`, `user.contact` and its children | `ORDINARY`, `LOW`, `MEDIUM` |
| The emergency contact | `user.contact.emergency_contact` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| The bank account for pay | `user.financial.bank_account` | `ORDINARY`, `MEDIUM`, `HIGH` |
| The social security number | `user.government_id.social_security_number` | `ORDINARY`, `MEDIUM`, `MEDIUM` |
| The job title | `user.job_title` | `ORDINARY`, `LOW`, `MEDIUM` |
| The position | `user.workplace.role` | `ORDINARY`, `LOW`, `MEDIUM` |
| Ethnic origin | `user.demographic.race_ethnicity` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| Trade union membership, including union dues deducted from pay | `user.demographic.trade_union_membership` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |

## Sources

The terms are informed by the human resources and payroll areas of the **Common Data Model (CDM)** schemas:
<https://github.com/microsoft/CDM> (documentation: <https://learn.microsoft.com/common-data-model/>). Copyright (c)
Microsoft Corporation. The documentation and other content of CDM are licensed under [Creative Commons Attribution 4.0
International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/); its code is under the MIT licence.

No CDM material (schemas, entity or attribute definitions, texts or enumerations) is reproduced in Linneo; CDM is cited
as the source of the concepts, and the names and descriptions of the terms are Linneo's. Linneo is not affiliated with
or endorsed by Microsoft. See also `NOTICE` and the "Licence and attribution" section of the `README`.

See also: [Extensions](README.md) · [Standard taxonomy](../standard-taxonomy.md) · [Key constants](../key-constants.md)
· [Protection levels](../protection-levels.md)
