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
 * Keys of the data uses added by the {@link HumanResourcesTaxonomy}, all below the standard {@code employment} use.
 *
 * @since 0.1.0
 */
public final class HumanResourcesDataUses {

    /** Compensation and Benefits: Sets pay, reviews salaries, awards bonuses and administers benefit plans. */
    public static final String EMPLOYMENT_COMPENSATION = "employment.compensation";

    /** Discipline: Investigates and sanctions breaches of workplace rules. */
    public static final String EMPLOYMENT_DISCIPLINE = "employment.discipline";

    /** Equality and Diversity: Prepares equality plans, pay registers and diversity reports. */
    public static final String EMPLOYMENT_EQUALITY = "employment.equality";

    /** Internal Reporting Channel: Receives and investigates reports of breaches and protects the people reporting. */
    public static final String EMPLOYMENT_INTERNAL_REPORTING = "employment.internal_reporting";

    /** Learning and Development: Plans, delivers and records training and career development. */
    public static final String EMPLOYMENT_LEARNING = "employment.learning";

    /** Leave Management: Manages holidays, leave and absences. */
    public static final String EMPLOYMENT_LEAVE = "employment.leave";

    /** Workplace Monitoring: Monitors work devices, premises and vehicles within the limits the law sets. */
    public static final String EMPLOYMENT_MONITORING = "employment.monitoring";

    /** Occupational Health and Safety: Prevents occupational risks, surveys health and investigates accidents. */
    public static final String EMPLOYMENT_OCCUPATIONAL_SAFETY = "employment.occupational_safety";

    /** Offboarding: Ends employment: notice, settlement, certificates and the withdrawal of access and equipment. */
    public static final String EMPLOYMENT_OFFBOARDING = "employment.offboarding";

    /** Onboarding: Prepares the start of employment: contract, registration, accounts, equipment and induction. */
    public static final String EMPLOYMENT_ONBOARDING = "employment.onboarding";

    /** Payroll: Calculates and pays salaries, withholds taxes and contributions and files the related returns. */
    public static final String EMPLOYMENT_PAYROLL = "employment.payroll";

    /** Performance Management: Sets goals, evaluates performance and plans development. */
    public static final String EMPLOYMENT_PERFORMANCE = "employment.performance";

    /** Background Check: Verifies candidates' identity, qualifications and history before hiring. */
    public static final String EMPLOYMENT_RECRUITMENT_BACKGROUND_CHECK = "employment.recruitment.background_check";

    /** Time and Attendance: Records working time, shifts and overtime as the law requires. */
    public static final String EMPLOYMENT_TIME_AND_ATTENDANCE = "employment.time_and_attendance";

    /** Workforce Planning: Designs the organization, positions and headcount and plans succession. */
    public static final String EMPLOYMENT_WORKFORCE_PLANNING = "employment.workforce_planning";

    private HumanResourcesDataUses() {
    }
}
