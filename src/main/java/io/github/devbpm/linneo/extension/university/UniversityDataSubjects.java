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
 * Keys of the data subjects added by the {@link UniversityTaxonomy}. Like fideslang's data subjects, they are flat:
 * none has a parent.
 *
 * @since 0.1.0
 */
public final class UniversityDataSubjects {

    /** Doctoral Candidate: A student enrolled in a doctoral programme. */
    public static final String DOCTORAL_CANDIDATE = "doctoral_candidate";

    /** Donor: An individual who gives or pledges gifts to the institution. */
    public static final String DONOR = "donor";

    /** Emergency Contact: A person named by a student or employee to be contacted in an emergency. */
    public static final String EMERGENCY_CONTACT = "emergency_contact";

    /** Exchange Student: A student on an exchange, received from or sent to another institution. */
    public static final String EXCHANGE_STUDENT = "exchange_student";

    /** External Examiner: An assessor from outside the institution who examines students or theses. */
    public static final String EXTERNAL_EXAMINER = "external_examiner";

    /** Guest: An individual temporarily affiliated with the institution without student or employee status. */
    public static final String GUEST = "guest";

    /** Mentor: An individual, often an alumnus or volunteer, who mentors students. */
    public static final String MENTOR = "mentor";

    /** Philanthropic Prospect: An individual identified as a potential donor. */
    public static final String PHILANTHROPIC_PROSPECT = "philanthropic_prospect";

    /** Placement Supervisor: The person who supervises a student at a host organisation or institution. */
    public static final String PLACEMENT_SUPERVISOR = "placement_supervisor";

    /** Prospective Student: An individual who has shown interest in studying at the institution. */
    public static final String PROSPECTIVE_STUDENT = "prospective_student";

    /** Referee: An individual who writes a reference letter about an applicant. */
    public static final String REFEREE = "referee";

    /** Trustee: A member of the institution's or its foundation's governing board. */
    public static final String TRUSTEE = "trustee";

    /** Volunteer: An individual who volunteers time for the institution. */
    public static final String VOLUNTEER = "volunteer";

    private UniversityDataSubjects() {
    }
}
