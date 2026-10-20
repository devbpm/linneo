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
 * Keys of the data categories of the {@linkplain Taxonomy#standard() standard taxonomy}.
 * <p>
 * The fideslang keys, names and descriptions come from the fideslang 3.1.4 default taxonomy,
 * &copy; Ethyca, Inc., licensed under CC BY 4.0 (see the {@code NOTICE} file). The keys added by
 * Linneo follow them.
 *
 * @since 0.1.0
 */
public final class Categories {

    // fideslang 3.1.4

    /** User Data: Data related to the user of the system, either provided directly or derived based on their usage. */
    public static final String USER = "user";

    /** Account Information: Account creation or registration information. */
    public static final String USER_ACCOUNT = "user.account";

    /** Account Settings: Account preferences and settings. */
    public static final String USER_ACCOUNT_SETTINGS = "user.account.settings";

    /** Account Username: Username associated with account. */
    public static final String USER_ACCOUNT_USERNAME = "user.account.username";

    /** Authorization Information: Scope of permissions and access to a system. */
    public static final String USER_AUTHORIZATION = "user.authorization";

    /** Account password: Authentication credentials to a system. */
    public static final String USER_AUTHORIZATION_CREDENTIALS = "user.authorization.credentials";

    /** Biometric Credentials: Credentials for system authentication. */
    public static final String USER_AUTHORIZATION_BIOMETRIC = "user.authorization.biometric";

    /** Password: Password for system authentication. */
    public static final String USER_AUTHORIZATION_PASSWORD = "user.authorization.password";

    /** Observed Behavior: Behavioral data about the subject. */
    public static final String USER_BEHAVIOR = "user.behavior";

    /** Browsing History: Content browsing history of a user. */
    public static final String USER_BEHAVIOR_BROWSING_HISTORY = "user.behavior.browsing_history";

    /** Media Consumption: Content consumption history of the subject. */
    public static final String USER_BEHAVIOR_MEDIA_CONSUMPTION = "user.behavior.media_consumption";

    /** Purchase History: Purchase history of the subject. */
    public static final String USER_BEHAVIOR_PURCHASE_HISTORY = "user.behavior.purchase_history";

    /** Search History: Search history of the subject. */
    public static final String USER_BEHAVIOR_SEARCH_HISTORY = "user.behavior.search_history";

    /** Biometric Data: Encoded characteristics provided by a user. */
    public static final String USER_BIOMETRIC = "user.biometric";

    /** Fingerprint: Fingerprint encoded data about a subject. */
    public static final String USER_BIOMETRIC_FINGERPRINT = "user.biometric.fingerprint";

    /** Retina Scan: Retinal data about a subject. */
    public static final String USER_BIOMETRIC_RETINAL = "user.biometric.retinal";

    /** Voice Recording: Voice encoded data about a subject. */
    public static final String USER_BIOMETRIC_VOICE = "user.biometric.voice";

    /** Biometric Health Data: Encoded characteristic collected about a user. */
    public static final String USER_BIOMETRIC_HEALTH = "user.biometric.health";

    /** Children's Data: Data relating to children. */
    public static final String USER_CHILDRENS = "user.childrens";

    /** Contact Data: Contact data collected about a user. */
    public static final String USER_CONTACT = "user.contact";

    /** User Contact Address: Contact address data collected about a user. */
    public static final String USER_CONTACT_ADDRESS = "user.contact.address";

    /** User Contact City: User's city level address data. */
    public static final String USER_CONTACT_ADDRESS_CITY = "user.contact.address.city";

    /** User Contact Country: User's country level address data. */
    public static final String USER_CONTACT_ADDRESS_COUNTRY = "user.contact.address.country";

    /** User Contact Email: User's contact email address. */
    public static final String USER_CONTACT_EMAIL = "user.contact.email";

    /** User Contact Phone Number: User's phone number. */
    public static final String USER_CONTACT_PHONE_NUMBER = "user.contact.phone_number";

    /** User Website: Subject's websites or links to social and personal profiles. */
    public static final String USER_CONTACT_URL = "user.contact.url";

    /** Fax Number: Data Subject's fax number. */
    public static final String USER_CONTACT_FAX_NUMBER = "user.contact.fax_number";

    /** User Contact Postal Code: User's postal code. */
    public static final String USER_CONTACT_ADDRESS_POSTAL_CODE = "user.contact.address.postal_code";

    /** User Contact State: User's state level address data. */
    public static final String USER_CONTACT_ADDRESS_STATE = "user.contact.address.state";

    /** User Contact Street: User's street level address data. */
    public static final String USER_CONTACT_ADDRESS_STREET = "user.contact.address.street";

    /** Organization: Data Subject's Organization. */
    public static final String USER_CONTACT_ORGANIZATION = "user.contact.organization";

    /** User Content: Content related to, or created by the subject. */
    public static final String USER_CONTENT = "user.content";

    /** Private Content: Private content related to, or created by the subject, not publicly available. */
    public static final String USER_CONTENT_PRIVATE = "user.content.private";

    /** Public Content: Publicly shared Content related to, or created by the subject. */
    public static final String USER_CONTENT_PUBLIC = "user.content.public";

    /** User Image: Photograph or image in which subject is whole or partially recognized. */
    public static final String USER_CONTENT_SELF_IMAGE = "user.content.self_image";

    /** Demographic Data: Demographic data about a user. */
    public static final String USER_DEMOGRAPHIC = "user.demographic";

    /** Age Range: Non specific age or age-range of data subject. */
    public static final String USER_DEMOGRAPHIC_AGE_RANGE = "user.demographic.age_range";

    /** Birth Date: Date of birth of data subject. */
    public static final String USER_DEMOGRAPHIC_DATE_OF_BIRTH = "user.demographic.date_of_birth";

    /** Gender: Gender of data subject. */
    public static final String USER_DEMOGRAPHIC_GENDER = "user.demographic.gender";

    /** Language: Spoken or written language of subject. */
    public static final String USER_DEMOGRAPHIC_LANGUAGE = "user.demographic.language";

    /** Marital Status: Marital status of data subject. */
    public static final String USER_DEMOGRAPHIC_MARITAL_STATUS = "user.demographic.marital_status";

    /** Political Opinion: Political opinion or belief of data subject. */
    public static final String USER_DEMOGRAPHIC_POLITICAL_OPINION = "user.demographic.political_opinion";

    /** User Profile Data: Profile or preference information about the data subject. */
    public static final String USER_DEMOGRAPHIC_PROFILE = "user.demographic.profile";

    /** Race: Race or ethnicity of data subject. */
    public static final String USER_DEMOGRAPHIC_RACE_ETHNICITY = "user.demographic.race_ethnicity";

    /** Religion: Religion or religious beliefs of the data subject. */
    public static final String USER_DEMOGRAPHIC_RELIGIOUS_BELIEF = "user.demographic.religious_belief";

    /** Sexual Orientation: Sexual orientation of data subject. */
    public static final String USER_DEMOGRAPHIC_SEXUAL_ORIENTATION = "user.demographic.sexual_orientation";

    /** Location Data: Records of the location of a user. */
    public static final String USER_LOCATION = "user.location";

    /** Imprecise Subject Location: Imprecise location derived from sensors (more than 500M). */
    public static final String USER_LOCATION_IMPRECISE = "user.location.imprecise";

    /** Precise Subject Location: Precise location derived from sensors (less than 500M). */
    public static final String USER_LOCATION_PRECISE = "user.location.precise";

    /** Device Data: Data related to a user's device, configuration and setting. */
    public static final String USER_DEVICE = "user.device";

    /** Device Cookie: Data related to a subject, stored within a cookie. */
    public static final String USER_DEVICE_COOKIE = "user.device.cookie";

    /** Cookie ID: Cookie unique identification number. */
    public static final String USER_DEVICE_COOKIE_ID = "user.device.cookie_id";

    /** Device ID: Device unique identification number. */
    public static final String USER_DEVICE_DEVICE_ID = "user.device.device_id";

    /** IP Address: Unique identifier related to device connection. */
    public static final String USER_DEVICE_IP_ADDRESS = "user.device.ip_address";

    /** Payment Data: Payment data related to user. */
    public static final String USER_PAYMENT = "user.payment";

    /** Social Data: Social activity and interaction data. */
    public static final String USER_SOCIAL = "user.social";

    /** Unique ID: Unique identifier for a user assigned through system use. */
    public static final String USER_UNIQUE_ID = "user.unique_id";

    /** Pseudonymous User ID: A pseudonymous, or probabilistic identifier generated from other subject or device data belonging to the subject. */
    public static final String USER_UNIQUE_ID_PSEUDONYMOUS = "user.unique_id.pseudonymous";

    /** Telemetry Data: User identifiable measurement data from system sensors and monitoring. */
    public static final String USER_TELEMETRY = "user.telemetry";

    /** User Sensor Data: Measurement data about a user's environment through system use. */
    public static final String USER_USER_SENSOR = "user.user_sensor";

    /** Workplace: Organization of employment. */
    public static final String USER_WORKPLACE = "user.workplace";

    /** Sensor Data: Measurement data from sensors and monitoring systems. */
    public static final String USER_SENSOR = "user.sensor";

    /** Financial Data: Payment data and financial history. */
    public static final String USER_FINANCIAL = "user.financial";

    /** Bank Account Information: Bank account information belonging to the subject. */
    public static final String USER_FINANCIAL_BANK_ACCOUNT = "user.financial.bank_account";

    /** Credit Card Information: Credit card information belonging to the subject. */
    public static final String USER_FINANCIAL_CREDIT_CARD = "user.financial.credit_card";

    /** Government ID: State provided identification data. */
    public static final String USER_GOVERNMENT_ID = "user.government_id";

    /** Birth Certificate: State issued certificate of birth. */
    public static final String USER_GOVERNMENT_ID_BIRTH_CERTIFICATE = "user.government_id.birth_certificate";

    /** Driver's License Number: State issued driving identification number. */
    public static final String USER_GOVERNMENT_ID_DRIVERS_LICENSE_NUMBER = "user.government_id.drivers_license_number";

    /** Immigration ID: State issued immigration or residency data. */
    public static final String USER_GOVERNMENT_ID_IMMIGRATION = "user.government_id.immigration";

    /** National Identification Number: State issued personal identification number. */
    public static final String USER_GOVERNMENT_ID_NATIONAL_IDENTIFICATION_NUMBER = "user.government_id.national_identification_number";

    /** Passport Number: State issued passport data. */
    public static final String USER_GOVERNMENT_ID_PASSPORT_NUMBER = "user.government_id.passport_number";

    /** Vehicle Registration: State issued license plate or vehicle registration data. */
    public static final String USER_GOVERNMENT_ID_VEHICLE_REGISTRATION = "user.government_id.vehicle_registration";

    /** Health and Medical Data: Health records or individual's personal medical information. */
    public static final String USER_HEALTH_AND_MEDICAL = "user.health_and_medical";

    /** User's Genetic Data: Data about the genetic makeup provided by the subject. */
    public static final String USER_HEALTH_AND_MEDICAL_GENETIC = "user.health_and_medical.genetic";

    /** Medical Insurance ID: Health insurance beneficiary number of the subject. */
    public static final String USER_HEALTH_AND_MEDICAL_INSURANCE_BENEFICIARY_ID = "user.health_and_medical.insurance_beneficiary_id";

    /** Medical Record ID: Medical record identifiers belonging to a subject. */
    public static final String USER_HEALTH_AND_MEDICAL_RECORD_ID = "user.health_and_medical.record_id";

    /** Name: User's real name. */
    public static final String USER_NAME = "user.name";

    /** First Name: Subject's first name. */
    public static final String USER_NAME_FIRST = "user.name.first";

    /** Last Name: Subject's last, or family, name. */
    public static final String USER_NAME_LAST = "user.name.last";

    /** Criminal History: Criminal records or information about the data subject. */
    public static final String USER_CRIMINAL_HISTORY = "user.criminal_history";

    /** Privacy Preferences: Privacy preferences or settings set by the subject. */
    public static final String USER_PRIVACY_PREFERENCES = "user.privacy_preferences";

    /** Job Title: Professional data. */
    public static final String USER_JOB_TITLE = "user.job_title";

    /** System Data: Data unique to, and under control of the system. */
    public static final String SYSTEM = "system";

    /** Authentication Data: Data used to manage access to the system. */
    public static final String SYSTEM_AUTHENTICATION = "system.authentication";

    /** Operations Data: Data used for system operations. */
    public static final String SYSTEM_OPERATIONS = "system.operations";

    // Linneo extensions

    /** Record Reference: Key, label or link that points to a record about the subject in this or another system. */
    public static final String USER_UNIQUE_ID_RECORD_REFERENCE = "user.unique_id.record_reference";

    /** Social Security Number: Number assigned to the subject by a social security system. */
    public static final String USER_GOVERNMENT_ID_SOCIAL_SECURITY_NUMBER = "user.government_id.social_security_number";

    /** User Agent: Browser or client identification string sent by the subject's device. */
    public static final String USER_DEVICE_USER_AGENT = "user.device.user_agent";

    /** Session ID: Identifier of an authenticated or anonymous session of the subject; while the session is valid it grants access like a credential. */
    public static final String USER_DEVICE_SESSION_ID = "user.device.session_id";

    /** Team: Team or working group the subject belongs to within the organization. */
    public static final String USER_WORKPLACE_TEAM = "user.workplace.team";

    /** Organizational Unit: Department, service or other unit the subject belongs to. */
    public static final String USER_WORKPLACE_UNIT = "user.workplace.unit";

    /** Role: Position or function the subject holds within the organization, such as head of unit or coordinator. */
    public static final String USER_WORKPLACE_ROLE = "user.workplace.role";

    /** Assignment Period: Start and end of the subject's assignment to a team, unit or role. */
    public static final String USER_WORKPLACE_ASSIGNMENT_PERIOD = "user.workplace.assignment_period";

    /** Nationality: Nationality or citizenship of the subject. */
    public static final String USER_DEMOGRAPHIC_NATIONALITY = "user.demographic.nationality";

    /** Family Relationship: Family or legal relationship between the subject and another person. */
    public static final String USER_DEMOGRAPHIC_FAMILY = "user.demographic.family";

    /** Philosophical Belief: Philosophical or ethical convictions of the subject that are not religious, such as conscientious objection. */
    public static final String USER_DEMOGRAPHIC_PHILOSOPHICAL_BELIEF = "user.demographic.philosophical_belief";

    /** Trade Union Membership: Membership of the subject in a trade union, including union dues deducted from pay. */
    public static final String USER_DEMOGRAPHIC_TRADE_UNION_MEMBERSHIP = "user.demographic.trade_union_membership";

    /** Sex Life: Data concerning the sex life of the subject. */
    public static final String USER_DEMOGRAPHIC_SEX_LIFE = "user.demographic.sex_life";

    /** Education Data: Data about the subject's studies and academic history, as a student or applicant. */
    public static final String USER_EDUCATION = "user.education";

    /** Program: Degree, program or qualification the subject is studying or has studied. */
    public static final String USER_EDUCATION_PROGRAM = "user.education.program";

    /** Enrollment: Registration of the subject in a program, course or academic period. */
    public static final String USER_EDUCATION_ENROLLMENT = "user.education.enrollment";

    /** Course History: Courses the subject has taken, with their periods and outcomes. */
    public static final String USER_EDUCATION_COURSE_HISTORY = "user.education.course_history";

    /** Grade: Marks, grades or assessment results obtained by the subject. */
    public static final String USER_EDUCATION_GRADE = "user.education.grade";

    /** Test Score: Results of admission or standardized tests taken by the subject. */
    public static final String USER_EDUCATION_TEST_SCORE = "user.education.test_score";

    /** Previous Education: Studies completed by the subject before the current program. */
    public static final String USER_EDUCATION_PREVIOUS_EDUCATION = "user.education.previous_education";

    /** Academic Status: Standing of the subject in a program: active, on leave, graduated, withdrawn. */
    public static final String USER_EDUCATION_STATUS = "user.education.status";

    /** Accomplishment: Awards, honours, certificates and other merits of the subject. */
    public static final String USER_EDUCATION_ACCOMPLISHMENT = "user.education.accomplishment";

    /** Scholarship: Scholarships, grants and financial aid applied for or awarded to the subject. */
    public static final String USER_FINANCIAL_SCHOLARSHIP = "user.financial.scholarship";

    /** Fee: Tuition, fees and charges owed or paid by the subject. */
    public static final String USER_FINANCIAL_FEE = "user.financial.fee";

    /** Disability: Type or degree of a disability of the subject. */
    public static final String USER_HEALTH_AND_MEDICAL_DISABILITY = "user.health_and_medical.disability";

    /** Activity Log: Record of the actions the subject performed in a system: who did what and when. */
    public static final String USER_BEHAVIOR_ACTIVITY_LOG = "user.behavior.activity_log";

    /** Secret: Secret, or a hash of a secret, that grants access: API keys, tokens, signing keys. */
    public static final String SYSTEM_AUTHENTICATION_SECRET = "system.authentication.secret";

    /** Security Data: Access control definitions that do not identify a person: roles, permission matrices, scopes. */
    public static final String SYSTEM_SECURITY = "system.security";

    /** Integrity Data: Evidence that protects the integrity of other data: hashes, chain anchors, checkpoints. */
    public static final String SYSTEM_INTEGRITY = "system.integrity";

    /** Configuration Data: Settings and definitions that drive the system: menus, objects, parameters, rules. */
    public static final String SYSTEM_CONFIGURATION = "system.configuration";

    /** Organization Data: Data about the organization and about third parties that are not natural persons. */
    public static final String ORG = "org";

    /** Organizational Structure: Units, teams and their hierarchy. */
    public static final String ORG_STRUCTURE = "org.structure";

    /** Legal Entity: Organizations and other legal persons: names, registration numbers, public registry data. */
    public static final String ORG_LEGAL_ENTITY = "org.legal_entity";

    /** Location: Sites, buildings, rooms and other places of the organization. */
    public static final String ORG_LOCATION = "org.location";

    /** Process Definition: Definitions, documentation and management data of the organization's processes. */
    public static final String ORG_PROCESS = "org.process";

    /** Business Records: Business records that do not relate to a natural person. */
    public static final String ORG_BUSINESS_RECORDS = "org.business_records";

    /** Reference Data: Master data from public or standard sources. */
    public static final String REFERENCE = "reference";

    /** Public Reference Data: Public code lists and registers: countries, currencies, languages, territorial codes, holidays. */
    public static final String REFERENCE_PUBLIC = "reference.public";

    private Categories() {
    }
}
