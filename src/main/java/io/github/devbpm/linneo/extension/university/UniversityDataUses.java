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
 * Keys of the data uses added by the {@link UniversityTaxonomy}: the purposes of a university's processing
 * activities.
 *
 * @since 0.1.0
 */
public final class UniversityDataUses {

    /** Advancement: Builds the institution's relationships with alumni, donors and supporters. */
    public static final String ADVANCEMENT = "advancement";

    /** Alumni Relations: Keeps in touch with former students: communications, events, networks. */
    public static final String ADVANCEMENT_ALUMNI_RELATIONS = "advancement.alumni_relations";

    /** Fundraising: Solicits, records and acknowledges gifts and pledges. */
    public static final String ADVANCEMENT_FUNDRAISING = "advancement.fundraising";

    /** Prospect Research: Identifies potential donors and estimates their giving capacity. */
    public static final String ADVANCEMENT_PROSPECT_RESEARCH = "advancement.prospect_research";

    /** Certification: Issues certificates, transcripts, diploma supplements and degree certificates. */
    public static final String EDUCATION_ACADEMIC_RECORDS_CERTIFICATION = "education.academic_records.certification";

    /** Credit Recognition: Recognises and transfers credits from previous studies or experience. */
    public static final String EDUCATION_ACADEMIC_RECORDS_CREDIT_RECOGNITION =
            "education.academic_records.credit_recognition";

    /** Qualification Verification: Confirms a subject's qualifications to employers or institutions on request. */
    public static final String EDUCATION_ACADEMIC_RECORDS_VERIFICATION = "education.academic_records.verification";

    /** Accessibility: Assesses needs and provides accommodations and disability support. */
    public static final String EDUCATION_ACCESSIBILITY = "education.accessibility";

    /** Advising: Provides academic advising, success planning and mentoring. */
    public static final String EDUCATION_ADVISING = "education.advising";

    /** Examination: Schedules and administers exam sittings, including invigilation and special arrangements. */
    public static final String EDUCATION_ASSESSMENT_EXAMINATION = "education.assessment.examination";

    /** Attendance: Records and monitors attendance and follows up on absences. */
    public static final String EDUCATION_ATTENDANCE = "education.attendance";

    /** Discipline: Investigates and sanctions breaches of conduct and of academic integrity. */
    public static final String EDUCATION_DISCIPLINE = "education.discipline";

    /** Means Assessment: Assesses household income and assets to award means-tested aid. */
    public static final String EDUCATION_FINANCIAL_AID_MEANS_ASSESSMENT = "education.financial_aid.means_assessment";

    /** Learning Analytics: Analyses learning data to improve teaching and student outcomes. */
    public static final String EDUCATION_LEARNING_ANALYTICS = "education.learning_analytics";

    /** Placement: Arranges internships, work placements, career services, exchanges and mobility. */
    public static final String EDUCATION_PLACEMENT = "education.placement";

    /** Career Services: Helps students and graduates find employment: job boards, guidance, employer events. */
    public static final String EDUCATION_PLACEMENT_CAREER_SERVICES = "education.placement.career_services";

    /** Internships: Arranges, supervises and assesses work placements with host organisations. */
    public static final String EDUCATION_PLACEMENT_INTERNSHIP = "education.placement.internship";

    /** Mobility: Organises exchanges and study stays at other institutions, in the country or abroad. */
    public static final String EDUCATION_PLACEMENT_MOBILITY = "education.placement.mobility";

    /** Quality Assurance: Evaluates and accredits programmes and teaching, including student surveys. */
    public static final String EDUCATION_QUALITY_ASSURANCE = "education.quality_assurance";

    /** Recruitment: Attracts prospective students: enquiries, events and interest tracking. */
    public static final String EDUCATION_RECRUITMENT = "education.recruitment";

    /** Student Life: Organises extracurricular activities, sports, societies and campus residence. */
    public static final String EDUCATION_STUDENT_LIFE = "education.student_life";

    /** Student Support: Identifies students at risk and provides interventions and wellbeing support. */
    public static final String EDUCATION_STUDENT_SUPPORT = "education.student_support";

    /** Thesis Supervision: Supervises master's and doctoral theses and manages their submission and defence. */
    public static final String EDUCATION_THESIS_SUPERVISION = "education.thesis_supervision";

    /** Statutory Reporting: Compiles the statistics and returns that education authorities require by law. */
    public static final String ESSENTIAL_LEGAL_OBLIGATION_STATUTORY_REPORTING =
            "essential.legal_obligation.statutory_reporting";

    /** Research: Carries out and manages scientific research. */
    public static final String RESEARCH = "research";

    /** Research Administration: Manages research projects, grants, researchers and their outputs. */
    public static final String RESEARCH_ADMINISTRATION = "research.administration";

    /** Research Participation: Processes data about the participants of research studies. */
    public static final String RESEARCH_PARTICIPATION = "research.participation";

    private UniversityDataUses() {
    }
}
