/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.devbpm.linneo.extension.humanresources;

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyExtension;

import static io.github.devbpm.linneo.extension.humanresources.HumanResourcesCategories.*;
import static io.github.devbpm.linneo.extension.humanresources.HumanResourcesDataSubjects.*;
import static io.github.devbpm.linneo.extension.humanresources.HumanResourcesDataUses.*;

/**
 * Terms for human resources: the employment relationship from recruitment to termination, pay, working time,
 * leave, performance, training, workplace monitoring and internal reporting, the human resources purposes and the
 * people involved.
 * <p>
 * The terms are informed by the human resources and payroll areas of the Common Data Model (CDM), copyright Microsoft
 * Corporation, whose documentation and other content are licensed under the CC BY 4.0 licence; no CDM material is
 * reproduced. The extension adds {@link HumanResourcesCategories#USER_EMPLOYMENT user.employment} for the
 * employment relationship managed by human resources, next to the standard {@code user.workplace}, which says where
 * and in which team, unit or role the subject works. What CDM records about a worker that the standard taxonomy
 * already covers keeps its standard key: names, addresses and contacts, the emergency contact
 * ({@code user.contact.emergency_contact}), the bank account for pay ({@code user.financial.bank_account}), the
 * social security number, the job title, the position ({@code user.workplace.role}), ethnic origin
 * ({@code user.demographic.race_ethnicity}) and trade union membership, including union dues deducted from pay
 * ({@code user.demographic.trade_union_membership}).
 * <p>
 * Every term extends an existing class: the categories extend {@code user} and {@code user.health_and_medical}, the
 * data uses sit below the standard {@code employment} use, and the data subjects are flat as in fideslang. No term
 * carries version information, which fideslang reserves for its default taxonomy.
 * <p>
 * Protection follows the standard policy and only raises it:
 * <ul>
 *   <li>sick leave and occupational health are health data, special categories with the maximum protection: the
 *       fact of being on sick leave already concerns health, and the Spanish occupational risk prevention law
 *       (Ley 31/1995, article 22) keeps health surveillance results confidential;</li>
 *   <li>a workplace adjustment reveals a disability, a health condition or a pregnancy, so it is a special category
 *       of data, with the maximum protection;</li>
 *   <li>compensation and payroll have high confidentiality and integrity, and a whistleblowing report too: the law
 *       on the protection of reporting persons (Ley 2/2023) requires the identity of the reporting person to be kept
 *       confidential and the reports to be recorded;</li>
 *   <li>background checks, candidate assessments, performance evaluations, disciplinary records, terminations and
 *       workplace monitoring have high confidentiality; LOPDGDD articles 87 to 91 limit the monitoring of workers'
 *       devices, video surveillance and geolocation;</li>
 *   <li>the employment contract and the working time record have high integrity: the Spanish Workers' Statute
 *       (article 34.9) requires the daily record of working time to be kept for four years;</li>
 *   <li>the other categories inherit the medium confidentiality of {@code user.employment}.</li>
 * </ul>
 * Data that reveals more than its category belongs in a stricter key: the medical reason for a leave in
 * {@code user.health_and_medical.sick_leave}, the disability degree declared for tax withholding in
 * {@code user.health_and_medical.disability}, health insurance numbers in
 * {@code user.health_and_medical.insurance_beneficiary_id}, a certificate of no criminal or sexual offence
 * convictions, or a dismissal for a criminal offence, in {@code user.criminal_history}, and a sanction imposed by
 * an authority rather than by the employer in {@code user.administrative_sanction}.
 * <p>
 * Like the other extensions shipped with Linneo, it is not registered with {@link java.util.ServiceLoader}, so
 * {@link io.github.devbpm.linneo.Taxonomy#load()} does not apply it. Apply it explicitly, alone or with others:
 * <pre>{@code
 * Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new HumanResourcesTaxonomy()).build();
 * }</pre>
 * or register it as a service of the application, as
 * {@link io.github.devbpm.linneo.extension.university.UniversityTaxonomy} explains. The keys are in
 * {@link HumanResourcesCategories}, {@link HumanResourcesDataUses} and {@link HumanResourcesDataSubjects}.
 *
 * @since 0.1.0
 */
public final class HumanResourcesTaxonomy implements TaxonomyExtension {

    /**
     * The extension's name, recorded as the origin of its terms.
     */
    @SuppressWarnings("java:S1845") // NAME is the value name() returns; the shared name is intended
    public static final String NAME = "human_resources";

    /**
     * Creates the extension; it holds no state.
     */
    public HumanResourcesTaxonomy() {
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void contribute(TaxonomyBuilder builder) {
        categories(builder);
        dataUses(builder);
        dataSubjects(builder);
    }

    private static void categories(TaxonomyBuilder builder) {
        builder.category(USER_EMPLOYMENT, "Employment Data",
                        "Data about the subject's employment or engagement with the organization, as managed by"
                                + " human resources.",
                        c -> c.confidentiality(Confidentiality.MEDIUM))
                .category(USER_EMPLOYMENT_BACKGROUND_CHECK, "Background Check",
                        "Pre-employment verification of the subject's identity, qualifications, work history and"
                                + " references.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EMPLOYMENT_BENEFIT, "Benefit",
                        "Enrolment in benefit plans, such as pensions, insurance, meal vouchers or flexible"
                                + " remuneration, and the beneficiaries named.")
                .category(USER_EMPLOYMENT_COMPENSATION, "Compensation",
                        "Salary, pay grade, allowances, bonuses, salary reviews and pay-for-performance awards.",
                        c -> c.confidentiality(Confidentiality.HIGH).integrity(Integrity.HIGH))
                .category(USER_EMPLOYMENT_CONTRACT, "Employment Contract",
                        "Contract type, start and end dates, working hours, probation and the changes to these"
                                + " terms.",
                        c -> c.integrity(Integrity.HIGH))
                .category(USER_EMPLOYMENT_DISCIPLINARY_RECORD, "Disciplinary Record",
                        "Warnings, disciplinary proceedings and sanctions concerning the subject.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EMPLOYMENT_LEAVE, "Leave and Absence",
                        "Holidays, leave requests, balances and absences, without their medical reasons.")
                .category(USER_EMPLOYMENT_MONITORING, "Workplace Monitoring",
                        "Data from monitoring the subject at work: use of work devices and email, video surveillance"
                                + " and geolocation.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EMPLOYMENT_PAYROLL, "Payroll",
                        "Pay statements, earnings, deductions, tax withholdings and social security contributions"
                                + " for each pay period.",
                        c -> c.confidentiality(Confidentiality.HIGH).integrity(Integrity.HIGH))
                .category(USER_EMPLOYMENT_PAYROLL_GARNISHMENT, "Wage Garnishment",
                        "Court or administrative orders to withhold part of the subject's pay, and the amounts"
                                + " withheld.")
                .category(USER_EMPLOYMENT_PAYROLL_TAX_WITHHOLDING, "Tax Withholding Data",
                        "Personal and family situation the subject declares to compute income tax withholding, such"
                                + " as dependants or pension payments.")
                .category(USER_EMPLOYMENT_PERFORMANCE, "Performance Evaluation",
                        "Goals, reviews, ratings, feedback and calibration of the subject's performance.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EMPLOYMENT_RECRUITMENT, "Recruitment",
                        "The subject's job applications: CV, positions applied for, interviews and their status.")
                .category(USER_EMPLOYMENT_RECRUITMENT_ASSESSMENT, "Candidate Assessment",
                        "Interview notes, test results, scores and the hiring decision about the subject.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EMPLOYMENT_TERMINATION, "Termination",
                        "End of the subject's employment: reason, notice, dismissal letter, settlement and"
                                + " severance.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EMPLOYMENT_TRAINING, "Training",
                        "Courses, attendance, results and certifications the subject obtains through the"
                                + " organization.")
                .category(USER_EMPLOYMENT_WHISTLEBLOWING, "Whistleblowing Report",
                        "Reports made through the internal reporting channel by or about the subject, and their"
                                + " investigation.",
                        c -> c.confidentiality(Confidentiality.HIGH).integrity(Integrity.HIGH))
                .category(USER_EMPLOYMENT_WORKING_TIME, "Working Time Record",
                        "Daily start and end of the subject's working day, breaks and overtime.",
                        c -> c.integrity(Integrity.HIGH))
                .category(USER_EMPLOYMENT_WORKPLACE_ADJUSTMENT, "Workplace Adjustment",
                        "Adjustments made to the subject's job, schedule or workplace for a disability, a health"
                                + " condition or a pregnancy, and the needs behind them.",
                        c -> c.nature(LegalNature.SPECIAL_CATEGORY).confidentiality(Confidentiality.HIGH)
                                .integrity(Integrity.HIGH))
                .category(USER_HEALTH_AND_MEDICAL_OCCUPATIONAL_HEALTH, "Occupational Health",
                        "Health surveillance, fitness-for-work assessments, workplace accidents and occupational"
                                + " diseases of the subject.")
                .category(USER_HEALTH_AND_MEDICAL_SICK_LEAVE, "Sick Leave",
                        "Periods of temporary incapacity for work, medical certificates and return-to-work"
                                + " reports.");
    }

    private static void dataUses(TaxonomyBuilder builder) {
        builder.dataUse(EMPLOYMENT_COMPENSATION, "Compensation and Benefits",
                        "Sets pay, reviews salaries, awards bonuses and administers benefit plans.")
                .dataUse(EMPLOYMENT_DISCIPLINE, "Discipline", "Investigates and sanctions breaches of workplace rules.")
                .dataUse(EMPLOYMENT_EQUALITY, "Equality and Diversity",
                        "Prepares equality plans, pay registers and diversity reports.")
                .dataUse(EMPLOYMENT_INTERNAL_REPORTING, "Internal Reporting Channel",
                        "Receives and investigates reports of breaches and protects the people who report them.")
                .dataUse(EMPLOYMENT_LEARNING, "Learning and Development",
                        "Plans, delivers and records training and career development.")
                .dataUse(EMPLOYMENT_LEAVE, "Leave Management", "Manages holidays, leave and absences.")
                .dataUse(EMPLOYMENT_MONITORING, "Workplace Monitoring",
                        "Monitors work devices, premises and vehicles within the limits the law sets.")
                .dataUse(EMPLOYMENT_OCCUPATIONAL_SAFETY, "Occupational Health and Safety",
                        "Prevents occupational risks, surveys workers' health and investigates accidents.")
                .dataUse(EMPLOYMENT_OFFBOARDING, "Offboarding",
                        "Ends employment: notice, settlement, certificates and the withdrawal of access and"
                                + " equipment.")
                .dataUse(EMPLOYMENT_ONBOARDING, "Onboarding",
                        "Prepares the start of employment: contract, registration, accounts, equipment and"
                                + " induction.")
                .dataUse(EMPLOYMENT_PAYROLL, "Payroll",
                        "Calculates and pays salaries, withholds taxes and contributions and files the related"
                                + " returns.")
                .dataUse(EMPLOYMENT_PERFORMANCE, "Performance Management",
                        "Sets goals, evaluates performance and plans development.")
                .dataUse(EMPLOYMENT_RECRUITMENT_BACKGROUND_CHECK, "Background Check",
                        "Verifies candidates' identity, qualifications and history before hiring.")
                .dataUse(EMPLOYMENT_TIME_AND_ATTENDANCE, "Time and Attendance",
                        "Records working time, shifts and overtime as the law requires.")
                .dataUse(EMPLOYMENT_WORKFORCE_PLANNING, "Workforce Planning",
                        "Designs the organization, positions and headcount and plans succession.");
    }

    private static void dataSubjects(TaxonomyBuilder builder) {
        builder.dataSubject(DEPENDANT, "Dependant",
                        "A person who depends on a worker and is covered by a benefit or declared for tax purposes.")
                .dataSubject(FORMER_EMPLOYEE, "Former Employee",
                        "An individual whose employment with the organization has ended.")
                .dataSubject(REPORTING_PERSON, "Reporting Person",
                        "An individual who reports breaches through the organization's internal reporting channel.")
                .dataSubject(RETIREE, "Retiree",
                        "A former employee who receives a pension or another retirement benefit from the"
                                + " organization.")
                .dataSubject(TEMPORARY_AGENCY_WORKER, "Temporary Agency Worker",
                        "A worker hired by a temporary work agency and assigned to the organization.")
                .dataSubject(WORKER_REPRESENTATIVE, "Worker Representative",
                        "A worker elected or appointed to represent the workforce, such as a works council member.");
    }
}
