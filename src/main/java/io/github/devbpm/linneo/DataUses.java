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
 * Keys of the data uses (purposes of processing) of the {@linkplain Taxonomy#standard() standard taxonomy}.
 * <p>
 * The fideslang keys, names and descriptions come from the fideslang 3.1.4 default taxonomy,
 * &copy; Ethyca, Inc., licensed under CC BY 4.0 (see the {@code NOTICE} file). The keys added by
 * Linneo follow them.
 *
 * @since 0.1.0
 */
public final class DataUses {

    // fideslang 3.1.4

    /** Analytics: Provides analytics for activities such as system and advertising performance reporting, insights and fraud detection. */
    public static final String ANALYTICS = "analytics";

    /** Analytics for Reporting: Provides analytics for general reporting such as system and advertising performance. */
    public static final String ANALYTICS_REPORTING = "analytics.reporting";

    /** Analytics for Advertising Performance: Provides analytics for reporting of advertising performance. */
    public static final String ANALYTICS_REPORTING_AD_PERFORMANCE = "analytics.reporting.ad_performance";

    /** Analytics for Content Performance: Analytics for reporting on content performance. */
    public static final String ANALYTICS_REPORTING_CONTENT_PERFORMANCE = "analytics.reporting.content_performance";

    /** Analytics for Insights: Provides analytics for reporting of campaign insights related to advertising and promotion activities. */
    public static final String ANALYTICS_REPORTING_CAMPAIGN_INSIGHTS = "analytics.reporting.campaign_insights";

    /** Analytics for System Activity: Provides analytics for reporting on system activity. */
    public static final String ANALYTICS_REPORTING_SYSTEM = "analytics.reporting.system";

    /** Analytics for System Performance: Provides analytics for reporting on system performance. */
    public static final String ANALYTICS_REPORTING_SYSTEM_PERFORMANCE = "analytics.reporting.system.performance";

    /** Collect: Collects or stores data in order to use it for another purpose which has not yet been expressly defined. */
    public static final String COLLECT = "collect";

    /** Employment: Processes data for the purpose of recruitment or employment and human resources (HR) related activities. */
    public static final String EMPLOYMENT = "employment";

    /** Employment Recruitment: Processes data of prospective employees for the purpose of recruitment. */
    public static final String EMPLOYMENT_RECRUITMENT = "employment.recruitment";

    /** Essential: Operates the service or product, including legal obligations, support and basic system operations. */
    public static final String ESSENTIAL = "essential";

    /** Essential Fraud Detection: Detects possible fraud or misuse of the product, service, application or system. */
    public static final String ESSENTIAL_FRAUD_DETECTION = "essential.fraud_detection";

    /** Essential Legal Obligation: Provides service to meet a legal or compliance obligation such as consent management. */
    public static final String ESSENTIAL_LEGAL_OBLIGATION = "essential.legal_obligation";

    /** Essential for Service: Provides the essential product, service, application or system, without which the product/service would not be possible. */
    public static final String ESSENTIAL_SERVICE = "essential.service";

    /** Essential Service Authentication: Authenticate users to the product, service, application or system. */
    public static final String ESSENTIAL_SERVICE_AUTHENTICATION = "essential.service.authentication";

    /** Essential Service Notifications: Sends notifications about the product, service, application or system. */
    public static final String ESSENTIAL_SERVICE_NOTIFICATIONS = "essential.service.notifications";

    /** Essential Email Service Notifications: Sends email notifications about the product, service, application or system. */
    public static final String ESSENTIAL_SERVICE_NOTIFICATIONS_EMAIL = "essential.service.notifications.email";

    /** Essential SMS Service Notifications: Sends SMS notifications about the product, service, application or system. */
    public static final String ESSENTIAL_SERVICE_NOTIFICATIONS_SMS = "essential.service.notifications.sms";

    /** Essential for Operations: Essential to ensure the operation of the product, service, application or system. */
    public static final String ESSENTIAL_SERVICE_OPERATIONS = "essential.service.operations";

    /** Essential for Operations Support: Provides support for the product, service, application or system. */
    public static final String ESSENTIAL_SERVICE_OPERATIONS_SUPPORT = "essential.service.operations.support";

    /** Essential for Support Improvement: Essential to optimize and improve support for the product, service, application or system. */
    public static final String ESSENTIAL_SERVICE_OPERATIONS_IMPROVE = "essential.service.operations.improve";

    /** Essential for Payment Processing: Essential to processes payments for the product, service, application or system. */
    public static final String ESSENTIAL_SERVICE_PAYMENT_PROCESSING = "essential.service.payment_processing";

    /** Essential for Security: Essential to provide security for the product, service, application or system */
    public static final String ESSENTIAL_SERVICE_SECURITY = "essential.service.security";

    /** Essential for Service Upgrades: Provides timely system upgrade information options. */
    public static final String ESSENTIAL_SERVICE_UPGRADES = "essential.service.upgrades";

    /** Finance: Enables finance and accounting activities such as audits and tax reporting. */
    public static final String FINANCE = "finance";

    /** Functional: Used for specific, necessary, and legitimate purposes */
    public static final String FUNCTIONAL = "functional";

    /** Local Data Storage: Stores or accesses information from the device as needed when using a product, service, application, or system */
    public static final String FUNCTIONAL_STORAGE = "functional.storage";

    /** Local Data Storage for Privacy Preferences: Stores or accesses privacy preferences information from the device as needed when using a product, service, application, or system */
    public static final String FUNCTIONAL_STORAGE_PRIVACY_PREFERENCES = "functional.storage.privacy_preferences";

    /** Service: Functions relating to provided services, products, applications or systems. */
    public static final String FUNCTIONAL_SERVICE = "functional.service";

    /** Improve Service: Improves the specific product, service, application or system. */
    public static final String FUNCTIONAL_SERVICE_IMPROVE = "functional.service.improve";

    /** Marketing: Enables marketing, promotion, advertising and sales activities for the product, service, application or system. */
    public static final String MARKETING = "marketing";

    /** Advertising, Marketing or Promotion: Advertises or promotes the product, service, application or system and associated services. */
    public static final String MARKETING_ADVERTISING = "marketing.advertising";

    /** First Party Advertising: Serves advertisements based on first party data collected or derived about the user. */
    public static final String MARKETING_ADVERTISING_FIRST_PARTY = "marketing.advertising.first_party";

    /** First Party Contextual Advertising: Serves advertisements based on current content being viewed by the user of the system or service. */
    public static final String MARKETING_ADVERTISING_FIRST_PARTY_CONTEXTUAL = "marketing.advertising.first_party.contextual";

    /** First Party Personalized Advertising: Targets advertisements based on data collected or derived about the user from use of the system. */
    public static final String MARKETING_ADVERTISING_FIRST_PARTY_TARGETED = "marketing.advertising.first_party.targeted";

    /** Frequency Capping: Restricts the number of times a specific advertisement is shown to an individual. */
    public static final String MARKETING_ADVERTISING_FREQUENCY_CAPPING = "marketing.advertising.frequency_capping";

    /** Negative Targeting: Enforces rules used to ensure a certain audience or group is not targeted by advertising. */
    public static final String MARKETING_ADVERTISING_NEGATIVE_TARGETING = "marketing.advertising.negative_targeting";

    /** Profiling for Advertising: Creates audience profiles for the purpose of targeted advertising */
    public static final String MARKETING_ADVERTISING_PROFILING = "marketing.advertising.profiling";

    /** Essential for Serving Ads: Essential to the delivery of advertising and content. */
    public static final String MARKETING_ADVERTISING_SERVING = "marketing.advertising.serving";

    /** Third Party Advertising: Serves advertisements based on data within the system or joined with data provided by 3rd parties. */
    public static final String MARKETING_ADVERTISING_THIRD_PARTY = "marketing.advertising.third_party";

    /** Third Party Targeted Advertising: Targets advertisements based on data within the system or joined with data provided by 3rd parties. */
    public static final String MARKETING_ADVERTISING_THIRD_PARTY_TARGETED = "marketing.advertising.third_party.targeted";

    /** Marketing Communications: Uses combined channels to message and market to a customer, user or prospect. */
    public static final String MARKETING_COMMUNICATIONS = "marketing.communications";

    /** Marketing Email Communications: Sends email marketing communications. */
    public static final String MARKETING_COMMUNICATIONS_EMAIL = "marketing.communications.email";

    /** Marketing SMS Communications: Sends SMS marketing communications. */
    public static final String MARKETING_COMMUNICATIONS_SMS = "marketing.communications.sms";

    /** Operations: Supports business processes necessary to the organization's operation. */
    public static final String OPERATIONS = "operations";

    /** Personalize: Personalizes the product, service, application or system. */
    public static final String PERSONALIZE = "personalize";

    /** Content Personalization: Personalizes the content of the product, service, application or system. */
    public static final String PERSONALIZE_CONTENT = "personalize.content";

    /** Personalized Profiling: Creates profiles for the purpose of serving content. */
    public static final String PERSONALIZE_PROFILING = "personalize.profiling";

    /** Limited Content Personalization: Uses limited data for the purpose of serving content. */
    public static final String PERSONALIZE_CONTENT_LIMITED = "personalize.content.limited";

    /** Profiling for Personalization: Creates profiles for the purpose of serving content. */
    public static final String PERSONALIZE_CONTENT_PROFILING = "personalize.content.profiling";

    /** Targeted Content Personalization: Uses profiles for the purpose of serving content. */
    public static final String PERSONALIZE_CONTENT_PROFILED = "personalize.content.profiled";

    /** System Personalization: Personalizes the system. */
    public static final String PERSONALIZE_SYSTEM = "personalize.system";

    /** Sales: Supports sales activities such as communications and outreach. */
    public static final String SALES = "sales";

    /** Third Party Sharing: Transfers data to third parties outside of the system or service's scope. */
    public static final String THIRD_PARTY_SHARING = "third_party_sharing";

    /** Sharing for Legal Obligation: Shares data for legal obligations, including contracts, applicable laws or regulations. */
    public static final String THIRD_PARTY_SHARING_LEGAL_OBLIGATION = "third_party_sharing.legal_obligation";

    /** Train AI System: Trains an AI system or data model for machine learning. */
    public static final String TRAIN_AI_SYSTEM = "train_ai_system";

    // Linneo extensions

    /** Education: Provides educational services: admission, enrollment, teaching, assessment and academic records. */
    public static final String EDUCATION = "education";

    /** Admission: Processes applications and admits applicants to programs. */
    public static final String EDUCATION_ADMISSION = "education.admission";

    /** Enrollment: Registers students in programs, courses and academic periods. */
    public static final String EDUCATION_ENROLLMENT = "education.enrollment";

    /** Teaching: Delivers courses and supports students' learning. */
    public static final String EDUCATION_TEACHING = "education.teaching";

    /** Assessment: Evaluates students and records their grades. */
    public static final String EDUCATION_ASSESSMENT = "education.assessment";

    /** Academic Records: Keeps students' academic records and issues certificates and degrees. */
    public static final String EDUCATION_ACADEMIC_RECORDS = "education.academic_records";

    /** Financial Aid: Manages scholarships, grants and other financial aid for students. */
    public static final String EDUCATION_FINANCIAL_AID = "education.financial_aid";

    private DataUses() {
    }
}
