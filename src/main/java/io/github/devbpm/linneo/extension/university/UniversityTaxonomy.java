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

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyExtension;

import static io.github.devbpm.linneo.extension.university.UniversityCategories.*;
import static io.github.devbpm.linneo.extension.university.UniversityDataSubjects.*;
import static io.github.devbpm.linneo.extension.university.UniversityDataUses.*;

/**
 * Terms for higher education institutions: the data a university holds about students, applicants, staff, alumni
 * and supporters beyond the standard {@code user.education.*} terms, the purposes it processes them for and the
 * people involved.
 * <p>
 * The terms are informed by three models of the domain: the Open Education API (OEAPI v6), the higher education
 * entities of Microsoft's Common Data Model and Education Accelerator, and Salesforce Education Cloud with its
 * Education Data Architecture. Attributes those models share with the standard taxonomy (grades, enrollments, test
 * scores, previous education, scholarships, fees, disability, nationality…) use the standard keys; only what has no
 * standard key is added. Teaching roles such as lecturer or invigilator are not data subjects: the models treat
 * them as roles a staff member or student holds in a course offering.
 * <p>
 * The student record gets the finest detail: enrolment changes and record transfers below
 * {@code user.education.enrollment}, assessment attempts, ratified final grades and averages below
 * {@code user.education.grade}, credit recognition below {@code user.education.course_history}, programme progress
 * below {@code user.education.program}, and degrees, certificates and language proficiency below
 * {@code user.education.accomplishment}. Internships and mobility are told apart below
 * {@code user.education.placement}, and means-tested aid and fee exemptions below the standard financial terms.
 * <p>
 * The extension follows fideslang's rules for custom labels: every term extends an existing class (fideslang
 * recommends it for interoperability), its parent is the prefix of its key, data subjects are flat as in fideslang,
 * and no term carries version information, which fideslang reserves for its default taxonomy.
 * <p>
 * Protection follows the standard policy and only raises it:
 * <ul>
 *   <li>an academic accommodation reveals a disability or health condition, so it is a special category of data;</li>
 *   <li>disciplinary records, advising notes, risk indicators, admission evaluations, reference letters, internship
 *       evaluations, equity indicators, means assessments, fee exemptions and wealth assessments have high
 *       confidentiality;</li>
 *   <li>the emergency contact has medium confidentiality, above the rest of {@code user.contact};</li>
 *   <li>the other categories inherit the medium confidentiality and high integrity of {@code user.education}.</li>
 * </ul>
 * Data that reveals more than its category belongs in a stricter key: the medical reason for an absence in
 * {@code user.health_and_medical}, a disciplinary case about a criminal offence in {@code user.criminal_history},
 * membership of a religious or political society in the matching special category, a fee exemption or grade
 * adjustment granted for a disability in {@code user.health_and_medical.disability}.
 * <p>
 * The only departure from extending existing classes is two roots of data uses that fideslang lacks,
 * {@link UniversityDataUses#ADVANCEMENT advancement} and {@link UniversityDataUses#RESEARCH research}, whose purposes
 * no fideslang use describes. It is not
 * registered with {@link java.util.ServiceLoader}, so {@link io.github.devbpm.linneo.Taxonomy#load()} does not apply
 * it. Apply it explicitly, which works on the class path and the module path alike:
 * <pre>{@code
 * Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();
 * }</pre>
 * To have {@code Taxonomy.load()} apply it instead, register it as a service of the application:
 * <ul>
 *   <li>on the class path, list {@code io.github.devbpm.linneo.extension.university.UniversityTaxonomy} in the
 *       application's {@code META-INF/services/io.github.devbpm.linneo.TaxonomyExtension} file;</li>
 *   <li>on the module path, a module can only provide its own classes, so declare a class in the application's
 *       module with a {@code public static TaxonomyExtension provider()} method that returns
 *       {@code new UniversityTaxonomy()}, and name that class in {@code provides
 *       io.github.devbpm.linneo.TaxonomyExtension with ...}.</li>
 * </ul>
 * Applying it both ways is harmless. The keys are in {@link UniversityCategories}, {@link UniversityDataUses} and
 * {@link UniversityDataSubjects}.
 *
 * @since 0.1.0
 */
public final class UniversityTaxonomy implements TaxonomyExtension {

    /**
     * The extension's name, recorded as the origin of its terms.
     */
    public static final String NAME = "university";

    /**
     * Creates the extension; it holds no state.
     */
    public UniversityTaxonomy() {
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
        builder.category(USER_CONTACT_EMERGENCY_CONTACT, "Emergency Contact",
                        "Name, phone number and relationship of the person to contact in an emergency.",
                        c -> c.confidentiality(Confidentiality.MEDIUM))
                .category(USER_EDUCATION_ACADEMIC_STANDING, "Academic Standing",
                        "Standing earned by performance: good standing, warning, probation, dismissal, honours.")
                .category(USER_EDUCATION_ACCOMMODATION, "Academic Accommodation",
                        "Adjustments granted for study or assessment, such as extra time or assistive tools, and the"
                                + " needs behind them.",
                        c -> c.nature(LegalNature.SPECIAL_CATEGORY).confidentiality(Confidentiality.HIGH)
                                .integrity(Integrity.HIGH))
                .category(USER_EDUCATION_ACCOMPLISHMENT_CERTIFICATE, "Academic Certificate",
                        "Certificates issued about the subject's record, such as transcripts of records or diploma"
                                + " supplements, with their verification codes.")
                .category(USER_EDUCATION_ACCOMPLISHMENT_DEGREE, "Degree",
                        "Degrees and official qualifications awarded: title, date, final grade, honours and"
                                + " registration number.")
                .category(USER_EDUCATION_ACCOMPLISHMENT_LANGUAGE_PROFICIENCY, "Language Proficiency",
                        "Accredited language proficiency levels, such as a certified B2 level.")
                .category(USER_EDUCATION_ADVISING, "Advising",
                        "Advisor assignments, advising notes, success plans and referrals to support services.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EDUCATION_APPLICATION, "Admission Application",
                        "The subject's application: programme and period applied to, status and answers.")
                .category(USER_EDUCATION_APPLICATION_EVALUATION, "Admission Evaluation",
                        "Reviewer scores, comments, conditions and the decision on an application.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EDUCATION_APPLICATION_REFERENCE, "Reference Letter",
                        "Recommendation or reference letters written about the subject by third parties.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EDUCATION_ATTENDANCE, "Attendance",
                        "Presence, absence and lateness of the subject in classes, exams or other sessions.")
                .category(USER_EDUCATION_COURSE_HISTORY_RECOGNITION, "Credit Recognition",
                        "Credits recognised or transferred from previous studies, work experience or other"
                                + " institutions, with the decision.")
                .category(USER_EDUCATION_COURSEWORK, "Coursework",
                        "Work produced by the subject for assessment: submitted assignments, exam answers.")
                .category(USER_EDUCATION_DISCIPLINARY_RECORD, "Disciplinary Record",
                        "Conduct and academic integrity proceedings and sanctions concerning the subject, such as"
                                + " plagiarism findings or suspensions.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EDUCATION_ENROLLMENT_MODIFICATION, "Enrollment Change",
                        "Changes to an enrolment after registration: courses added or withdrawn, cancellation, partial"
                                + " or full withdrawal.")
                .category(USER_EDUCATION_ENROLLMENT_TRANSFER, "Record Transfer",
                        "Transfer of the subject's record to or from another institution, with the documents"
                                + " exchanged.")
                .category(USER_EDUCATION_EQUITY_INDICATOR, "Equity Indicator",
                        "Flags for widening participation or equity reporting, such as first-generation student,"
                                + " care leaver or economic disadvantage.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EDUCATION_EXTRACURRICULAR, "Extracurricular Activity",
                        "Clubs, societies, sports and other non-curricular involvement of the subject.")
                .category(USER_EDUCATION_GRADE_ATTEMPT, "Assessment Attempt",
                        "Each sitting of an assessment: date, attempt number, result and incidents.")
                .category(USER_EDUCATION_GRADE_AVERAGE, "Grade Average",
                        "Grade point averages and rankings computed from the subject's results.")
                .category(USER_EDUCATION_GRADE_FINAL, "Final Grade",
                        "Final course results ratified by the examining board, as recorded in the transcript.")
                .category(USER_EDUCATION_HOLD, "Academic Hold",
                        "Administrative restrictions on the subject's record, such as registration or transcript"
                                + " holds.")
                .category(USER_EDUCATION_PLACEMENT, "Placement",
                        "Internships, work placements, exchanges and other mobility undertaken during studies.")
                .category(USER_EDUCATION_PLACEMENT_INTERNSHIP, "Internship",
                        "A work placement: host organisation, supervisor, agreement, schedule and tasks.")
                .category(USER_EDUCATION_PLACEMENT_INTERNSHIP_EVALUATION, "Internship Evaluation",
                        "The host supervisor's assessment of the subject's performance in a placement.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EDUCATION_PLACEMENT_MOBILITY, "Mobility",
                        "An exchange or study stay at another institution: host institution, learning agreement,"
                                + " recognition and mobility grant.")
                .category(USER_EDUCATION_PROGRAM_PROGRESS, "Programme Progress",
                        "Credits earned, requirements met and progress towards completing the programme.")
                .category(USER_EDUCATION_RISK_INDICATOR, "Academic Risk Indicator",
                        "Early alerts, watchlists, risk scores and predictions about the subject's progress or"
                                + " retention.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_EDUCATION_THESIS, "Thesis",
                        "Master's or doctoral thesis or final project: title, supervisors, submission and defence.")
                .category(USER_FINANCIAL_DONATION, "Donation",
                        "Gifts, pledges and recurring donations made by the subject to the institution.")
                .category(USER_FINANCIAL_FEE_EXEMPTION, "Fee Exemption",
                        "Exemptions and reductions of fees and the grounds for them, such as a large family or"
                                + " academic merit.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_FINANCIAL_SCHOLARSHIP_MEANS_ASSESSMENT, "Means Assessment",
                        "Household income, assets and composition assessed to award means-tested aid.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_FINANCIAL_WEALTH_ASSESSMENT, "Wealth Assessment",
                        "Estimates of the subject's wealth, income and giving capacity from prospect research.",
                        c -> c.confidentiality(Confidentiality.HIGH));
    }

    private static void dataUses(TaxonomyBuilder builder) {
        builder.dataUse(ADVANCEMENT, "Advancement",
                        "Builds the institution's relationships with alumni, donors and supporters.")
                .dataUse(ADVANCEMENT_ALUMNI_RELATIONS, "Alumni Relations",
                        "Keeps in touch with former students: communications, events, networks.")
                .dataUse(ADVANCEMENT_FUNDRAISING, "Fundraising",
                        "Solicits, records and acknowledges gifts and pledges.")
                .dataUse(ADVANCEMENT_PROSPECT_RESEARCH, "Prospect Research",
                        "Identifies potential donors and estimates their giving capacity.")
                .dataUse(EDUCATION_ACADEMIC_RECORDS_CERTIFICATION, "Certification",
                        "Issues certificates, transcripts, diploma supplements and degree certificates.")
                .dataUse(EDUCATION_ACADEMIC_RECORDS_CREDIT_RECOGNITION, "Credit Recognition",
                        "Recognises and transfers credits from previous studies or experience.")
                .dataUse(EDUCATION_ACADEMIC_RECORDS_VERIFICATION, "Qualification Verification",
                        "Confirms a subject's qualifications to employers or other institutions on request.")
                .dataUse(EDUCATION_ACCESSIBILITY, "Accessibility",
                        "Assesses needs and provides accommodations and disability support.")
                .dataUse(EDUCATION_ADVISING, "Advising",
                        "Provides academic advising, success planning and mentoring.")
                .dataUse(EDUCATION_ASSESSMENT_EXAMINATION, "Examination",
                        "Schedules and administers exam sittings, including invigilation and special arrangements.")
                .dataUse(EDUCATION_ATTENDANCE, "Attendance",
                        "Records and monitors attendance and follows up on absences.")
                .dataUse(EDUCATION_DISCIPLINE, "Discipline",
                        "Investigates and sanctions breaches of conduct and of academic integrity.")
                .dataUse(EDUCATION_FINANCIAL_AID_MEANS_ASSESSMENT, "Means Assessment",
                        "Assesses household income and assets to award means-tested aid.")
                .dataUse(EDUCATION_LEARNING_ANALYTICS, "Learning Analytics",
                        "Analyses learning data to improve teaching and student outcomes.")
                .dataUse(EDUCATION_PLACEMENT, "Placement",
                        "Arranges internships, work placements, career services, exchanges and mobility.")
                .dataUse(EDUCATION_PLACEMENT_CAREER_SERVICES, "Career Services",
                        "Helps students and graduates find employment: job boards, guidance, employer events.")
                .dataUse(EDUCATION_PLACEMENT_INTERNSHIP, "Internships",
                        "Arranges, supervises and assesses work placements with host organisations.")
                .dataUse(EDUCATION_PLACEMENT_MOBILITY, "Mobility",
                        "Organises exchanges and study stays at other institutions, in the country or abroad.")
                .dataUse(EDUCATION_QUALITY_ASSURANCE, "Quality Assurance",
                        "Evaluates and accredits programmes and teaching, including student surveys.")
                .dataUse(EDUCATION_RECRUITMENT, "Recruitment",
                        "Attracts prospective students: enquiries, events and interest tracking.")
                .dataUse(EDUCATION_STUDENT_LIFE, "Student Life",
                        "Organises extracurricular activities, sports, societies and campus residence.")
                .dataUse(EDUCATION_STUDENT_SUPPORT, "Student Support",
                        "Identifies students at risk and provides interventions and wellbeing support.")
                .dataUse(EDUCATION_THESIS_SUPERVISION, "Thesis Supervision",
                        "Supervises master's and doctoral theses and manages their submission and defence.")
                .dataUse(ESSENTIAL_LEGAL_OBLIGATION_STATUTORY_REPORTING, "Statutory Reporting",
                        "Compiles the statistics and returns that education authorities require by law.")
                .dataUse(RESEARCH, "Research", "Carries out and manages scientific research.")
                .dataUse(RESEARCH_ADMINISTRATION, "Research Administration",
                        "Manages research projects, grants, researchers and their outputs.")
                .dataUse(RESEARCH_PARTICIPATION, "Research Participation",
                        "Processes data about the participants of research studies.");
    }

    private static void dataSubjects(TaxonomyBuilder builder) {
        builder.dataSubject(DOCTORAL_CANDIDATE, "Doctoral Candidate", "A student enrolled in a doctoral programme.")
                .dataSubject(DONOR, "Donor", "An individual who gives or pledges gifts to the institution.")
                .dataSubject(EMERGENCY_CONTACT, "Emergency Contact",
                        "A person named by a student or employee to be contacted in an emergency.")
                .dataSubject(EXCHANGE_STUDENT, "Exchange Student",
                        "A student on an exchange, received from or sent to another institution.")
                .dataSubject(EXTERNAL_EXAMINER, "External Examiner",
                        "An assessor from outside the institution who examines students or theses.")
                .dataSubject(GUEST, "Guest",
                        "An individual temporarily affiliated with the institution without student or employee"
                                + " status.")
                .dataSubject(MENTOR, "Mentor", "An individual, often an alumnus or volunteer, who mentors students.")
                .dataSubject(PHILANTHROPIC_PROSPECT, "Philanthropic Prospect",
                        "An individual identified as a potential donor.")
                .dataSubject(PLACEMENT_SUPERVISOR, "Placement Supervisor",
                        "The person who supervises a student at a host organisation or institution.")
                .dataSubject(PROSPECTIVE_STUDENT, "Prospective Student",
                        "An individual who has shown interest in studying at the institution.")
                .dataSubject(REFEREE, "Referee", "An individual who writes a reference letter about an applicant.")
                .dataSubject(TRUSTEE, "Trustee",
                        "A member of the institution's or its foundation's governing board.")
                .dataSubject(VOLUNTEER, "Volunteer", "An individual who volunteers time for the institution.");
    }
}
