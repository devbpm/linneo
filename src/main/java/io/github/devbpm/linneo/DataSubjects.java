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
package io.github.devbpm.linneo;

/**
 * Keys of the data subjects of the {@linkplain Taxonomy#standard() standard taxonomy}.
 * <p>
 * The fideslang keys, names and descriptions come from the fideslang 3.1.4 default taxonomy,
 * &copy; Ethyca, Inc., licensed under CC BY 4.0 (see the {@code NOTICE} file). The keys added by
 * Linneo follow them.
 *
 * @since 0.1.0
 */
public final class DataSubjects {

    // fideslang 3.1.4

    /** Anonymous User: An individual that is unidentifiable to the systems. Note - This should only be applied to truly anonymous users where there is no risk of re-identification */
    public static final String ANONYMOUS_USER = "anonymous_user";

    /** Citizen Voter: An individual registered to voter with a state or authority. */
    public static final String CITIZEN_VOTER = "citizen_voter";

    /** Commuter: An individual that is traveling or transiting in the context of location tracking. */
    public static final String COMMUTER = "commuter";

    /** Consultant: An individual employed in a consultative/temporary capacity by the organization. */
    public static final String CONSULTANT = "consultant";

    /** Customer: An individual or other organization that purchases goods or services from the organization. */
    public static final String CUSTOMER = "customer";

    /** Employee: An individual employed by the organization. */
    public static final String EMPLOYEE = "employee";

    /** Job Applicant: An individual applying for employment to the organization. */
    public static final String JOB_APPLICANT = "job_applicant";

    /** Next of Kin: A relative of any other individual subject where such a relationship is known. */
    public static final String NEXT_OF_KIN = "next_of_kin";

    /** Passenger: An individual traveling on some means of provided transport. */
    public static final String PASSENGER = "passenger";

    /** Patient: An individual identified for the purposes of any medical care. */
    public static final String PATIENT = "patient";

    /** Prospect: An individual or organization to whom an organization is selling goods or services. */
    public static final String PROSPECT = "prospect";

    /** Shareholder: An individual or organization that holds equity in the organization. */
    public static final String SHAREHOLDER = "shareholder";

    /** Supplier/Vendor: An individual or organization that provides services or goods to the organization. */
    public static final String SUPPLIER_VENDOR = "supplier_vendor";

    /** Trainee: An individual undergoing training by the organization. */
    public static final String TRAINEE = "trainee";

    /** Visitor: An individual visiting a location. */
    public static final String VISITOR = "visitor";

    // Linneo extensions

    /** Student: An individual enrolled in a program or course of the organization. */
    public static final String STUDENT = "student";

    /** Applicant: An individual who applies for admission to a program of the organization. */
    public static final String APPLICANT = "applicant";

    /** Alumnus: A former student of the organization. */
    public static final String ALUMNUS = "alumnus";

    /** Faculty Member: An individual who teaches or does research for the organization. */
    public static final String FACULTY_MEMBER = "faculty_member";

    /** Staff Member: An individual in an administrative, technical or service role of the organization. */
    public static final String STAFF_MEMBER = "staff_member";

    /** Legal Representative: An individual who acts on behalf of a legal entity or of another person. */
    public static final String LEGAL_REPRESENTATIVE = "legal_representative";

    private DataSubjects() {
    }
}
