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
package io.github.devbpm.linneo.extension.university;

/**
 * Keys of the data categories added by the {@link UniversityTaxonomy}. Use them in
 * {@link io.github.devbpm.linneo.annotation.Category @Category} once the extension is applied.
 *
 * @since 0.1.0
 */
public final class UniversityCategories {

    /** Emergency Contact: Name, phone number and relationship of the person to contact in an emergency. */
    public static final String USER_CONTACT_EMERGENCY_CONTACT = "user.contact.emergency_contact";

    /** Academic Standing: Standing earned by performance: good standing, warning, probation, dismissal, honours. */
    public static final String USER_EDUCATION_ACADEMIC_STANDING = "user.education.academic_standing";

    /** Academic Accommodation: Adjustments granted for study or assessment, such as extra time or assistive tools. */
    public static final String USER_EDUCATION_ACCOMMODATION = "user.education.accommodation";

    /** Language Proficiency: Accredited language proficiency levels, such as a certified B2 level. */
    public static final String USER_EDUCATION_ACCOMPLISHMENT_LANGUAGE_PROFICIENCY =
            "user.education.accomplishment.language_proficiency";

    /** Academic Certificate: Certificates issued about the record, such as transcripts or diploma supplements. */
    public static final String USER_EDUCATION_ACCOMPLISHMENT_CERTIFICATE = "user.education.accomplishment.certificate";

    /** Degree: Degrees and official qualifications awarded: title, date, final grade, honours, registration number. */
    public static final String USER_EDUCATION_ACCOMPLISHMENT_DEGREE = "user.education.accomplishment.degree";

    /** Advising: Advisor assignments, advising notes, success plans and referrals to support services. */
    public static final String USER_EDUCATION_ADVISING = "user.education.advising";

    /** Admission Application: The subject's application: programme and period applied to, status and answers. */
    public static final String USER_EDUCATION_APPLICATION = "user.education.application";

    /** Admission Evaluation: Reviewer scores, comments, conditions and the decision on an application. */
    public static final String USER_EDUCATION_APPLICATION_EVALUATION = "user.education.application.evaluation";

    /** Reference Letter: Recommendation or reference letters written about the subject by third parties. */
    public static final String USER_EDUCATION_APPLICATION_REFERENCE = "user.education.application.reference";

    /** Attendance: Presence, absence and lateness of the subject in classes, exams or other sessions. */
    public static final String USER_EDUCATION_ATTENDANCE = "user.education.attendance";

    /** Credit Recognition: Credits recognised or transferred from previous studies, experience or institutions. */
    public static final String USER_EDUCATION_COURSE_HISTORY_RECOGNITION = "user.education.course_history.recognition";

    /** Coursework: Work produced by the subject for assessment: submitted assignments, exam answers. */
    public static final String USER_EDUCATION_COURSEWORK = "user.education.coursework";

    /** Disciplinary Record: Conduct and academic integrity proceedings and sanctions concerning the subject. */
    public static final String USER_EDUCATION_DISCIPLINARY_RECORD = "user.education.disciplinary_record";

    /** Enrollment Change: Changes after registration: courses added or withdrawn, cancellation, withdrawal. */
    public static final String USER_EDUCATION_ENROLLMENT_MODIFICATION = "user.education.enrollment.modification";

    /** Record Transfer: Transfer of the subject's record to or from another institution. */
    public static final String USER_EDUCATION_ENROLLMENT_TRANSFER = "user.education.enrollment.transfer";

    /** Equity Indicator: Flags for widening participation or equity reporting, such as first-generation student. */
    public static final String USER_EDUCATION_EQUITY_INDICATOR = "user.education.equity_indicator";

    /** Extracurricular Activity: Clubs, societies, sports and other non-curricular involvement of the subject. */
    public static final String USER_EDUCATION_EXTRACURRICULAR = "user.education.extracurricular";

    /** Assessment Attempt: Each sitting of an assessment: date, attempt number, result and incidents. */
    public static final String USER_EDUCATION_GRADE_ATTEMPT = "user.education.grade.attempt";

    /** Grade Average: Grade point averages and rankings computed from the subject's results. */
    public static final String USER_EDUCATION_GRADE_AVERAGE = "user.education.grade.average";

    /** Final Grade: Final course results ratified by the examining board, as recorded in the transcript. */
    public static final String USER_EDUCATION_GRADE_FINAL = "user.education.grade.final";

    /** Academic Hold: Administrative restrictions on the subject's record, such as registration holds. */
    public static final String USER_EDUCATION_HOLD = "user.education.hold";

    /** Placement: Internships, work placements, exchanges and other mobility undertaken during studies. */
    public static final String USER_EDUCATION_PLACEMENT = "user.education.placement";

    /** Internship: A work placement: host organisation, supervisor, agreement, schedule and tasks. */
    public static final String USER_EDUCATION_PLACEMENT_INTERNSHIP = "user.education.placement.internship";

    /** Internship Evaluation: The host supervisor's assessment of the subject's performance in a placement. */
    public static final String USER_EDUCATION_PLACEMENT_INTERNSHIP_EVALUATION =
            "user.education.placement.internship.evaluation";

    /** Mobility: An exchange or study stay: host institution, learning agreement, recognition and mobility grant. */
    public static final String USER_EDUCATION_PLACEMENT_MOBILITY = "user.education.placement.mobility";

    /** Programme Progress: Credits earned, requirements met and progress towards completing the programme. */
    public static final String USER_EDUCATION_PROGRAM_PROGRESS = "user.education.program.progress";

    /** Academic Risk Indicator: Early alerts, watchlists, risk scores and predictions about progress or retention. */
    public static final String USER_EDUCATION_RISK_INDICATOR = "user.education.risk_indicator";

    /** Thesis: Master's or doctoral thesis or final project: title, supervisors, submission and defence. */
    public static final String USER_EDUCATION_THESIS = "user.education.thesis";

    /** Donation: Gifts, pledges and recurring donations made by the subject to the institution. */
    public static final String USER_FINANCIAL_DONATION = "user.financial.donation";

    /** Fee Exemption: Exemptions and reductions of fees and the grounds for them, such as a large family. */
    public static final String USER_FINANCIAL_FEE_EXEMPTION = "user.financial.fee.exemption";

    /** Means Assessment: Household income, assets and composition assessed to award means-tested aid. */
    public static final String USER_FINANCIAL_SCHOLARSHIP_MEANS_ASSESSMENT =
            "user.financial.scholarship.means_assessment";

    /** Wealth Assessment: Estimates of the subject's wealth, income and giving capacity from prospect research. */
    public static final String USER_FINANCIAL_WEALTH_ASSESSMENT = "user.financial.wealth_assessment";

    private UniversityCategories() {
    }
}
