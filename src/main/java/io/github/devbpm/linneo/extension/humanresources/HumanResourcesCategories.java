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

/**
 * Keys of the data categories added by the {@link HumanResourcesTaxonomy}, for use in annotations.
 *
 * @since 0.1.0
 */
public final class HumanResourcesCategories {

    /** Employment Data: Data about the subject's employment or engagement, as managed by human resources. */
    public static final String USER_EMPLOYMENT = "user.employment";

    /** Background Check: Pre-employment verification of identity, qualifications, work history and references. */
    public static final String USER_EMPLOYMENT_BACKGROUND_CHECK = "user.employment.background_check";

    /** Benefit: Enrolment in benefit plans, such as pensions, insurance or meal vouchers, and beneficiaries named. */
    public static final String USER_EMPLOYMENT_BENEFIT = "user.employment.benefit";

    /** Compensation: Salary, pay grade, allowances, bonuses, salary reviews and pay-for-performance awards. */
    public static final String USER_EMPLOYMENT_COMPENSATION = "user.employment.compensation";

    /** Employment Contract: Contract type, start and end dates, working hours, probation and changes to the terms. */
    public static final String USER_EMPLOYMENT_CONTRACT = "user.employment.contract";

    /** Disciplinary Record: Warnings, disciplinary proceedings and sanctions concerning the subject. */
    public static final String USER_EMPLOYMENT_DISCIPLINARY_RECORD = "user.employment.disciplinary_record";

    /** Leave and Absence: Holidays, leave requests, balances and absences, without their medical reasons. */
    public static final String USER_EMPLOYMENT_LEAVE = "user.employment.leave";

    /** Workplace Monitoring: Monitoring of work devices and email, video surveillance and geolocation at work. */
    public static final String USER_EMPLOYMENT_MONITORING = "user.employment.monitoring";

    /** Payroll: Pay statements, earnings, deductions, tax withholdings and social security contributions. */
    public static final String USER_EMPLOYMENT_PAYROLL = "user.employment.payroll";

    /** Wage Garnishment: Court or administrative orders to withhold part of the subject's pay, and amounts withheld. */
    public static final String USER_EMPLOYMENT_PAYROLL_GARNISHMENT = "user.employment.payroll.garnishment";

    /** Tax Withholding Data: Personal and family situation the subject declares to compute income tax withholding. */
    public static final String USER_EMPLOYMENT_PAYROLL_TAX_WITHHOLDING = "user.employment.payroll.tax_withholding";

    /** Performance Evaluation: Goals, reviews, ratings, feedback and calibration of the subject's performance. */
    public static final String USER_EMPLOYMENT_PERFORMANCE = "user.employment.performance";

    /** Recruitment: The subject's job applications: CV, positions applied for, interviews and their status. */
    public static final String USER_EMPLOYMENT_RECRUITMENT = "user.employment.recruitment";

    /** Candidate Assessment: Interview notes, test results, scores and the hiring decision about the subject. */
    public static final String USER_EMPLOYMENT_RECRUITMENT_ASSESSMENT = "user.employment.recruitment.assessment";

    /** Termination: End of the subject's employment: reason, notice, dismissal letter, settlement and severance. */
    public static final String USER_EMPLOYMENT_TERMINATION = "user.employment.termination";

    /** Training: Courses, attendance, results and certifications the subject obtains through the organization. */
    public static final String USER_EMPLOYMENT_TRAINING = "user.employment.training";

    /** Whistleblowing Report: Reports made through the internal reporting channel by or about the subject. */
    public static final String USER_EMPLOYMENT_WHISTLEBLOWING = "user.employment.whistleblowing";

    /** Working Time Record: Daily start and end of the subject's working day, breaks and overtime. */
    public static final String USER_EMPLOYMENT_WORKING_TIME = "user.employment.working_time";

    /** Workplace Adjustment: Adjustments to the job or workplace for a disability or health condition. */
    public static final String USER_EMPLOYMENT_WORKPLACE_ADJUSTMENT = "user.employment.workplace_adjustment";

    /** Occupational Health: Health surveillance, fitness for work, workplace accidents and occupational diseases. */
    public static final String USER_HEALTH_AND_MEDICAL_OCCUPATIONAL_HEALTH =
            "user.health_and_medical.occupational_health";

    /** Sick Leave: Periods of temporary incapacity for work, medical certificates and return-to-work reports. */
    public static final String USER_HEALTH_AND_MEDICAL_SICK_LEAVE = "user.health_and_medical.sick_leave";

    private HumanResourcesCategories() {
    }
}
