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
package io.github.devbpm.linneo.extension.projectoperations;

/**
 * Keys of the data categories added by the {@link ProjectOperationsTaxonomy}, for use in annotations.
 *
 * @since 0.1.0
 */
public final class ProjectOperationsCategories {

    /** Project: Projects, work breakdown structures, tasks, dependencies, milestones and status. */
    public static final String ORG_BUSINESS_RECORDS_PROJECT = "org.business_records.project";

    /** Project Actual: Cost, revenue and hours posted to a project. */
    public static final String ORG_BUSINESS_RECORDS_PROJECT_ACTUAL = "org.business_records.project.actual";

    /** Project Contract: Contracts agreed with a customer for a project, with billing methods and invoice schedules. */
    public static final String ORG_BUSINESS_RECORDS_PROJECT_CONTRACT = "org.business_records.project.contract";

    /** Project Estimate: Estimates, quotations, budgets and forecasts of a project's cost and revenue. */
    public static final String ORG_BUSINESS_RECORDS_PROJECT_ESTIMATE = "org.business_records.project.estimate";

    /** Project Funding: Funding sources, grants, funding rules and their limits. */
    public static final String ORG_BUSINESS_RECORDS_PROJECT_FUNDING = "org.business_records.project.funding";

    /** Project Rate: Cost and sales prices of roles, categories and transaction types used to price projects. */
    public static final String ORG_BUSINESS_RECORDS_PROJECT_RATE = "org.business_records.project.rate";

    /** Project Assignment: Projects, tasks and roles the subject is assigned to, with bookings and planned hours. */
    public static final String USER_WORKPLACE_PROJECT_ASSIGNMENT = "user.workplace.project_assignment";

    /** Resource Rate: Cost and billing rates set for the subject as a project resource. */
    public static final String USER_WORKPLACE_RESOURCE_RATE = "user.workplace.resource_rate";

    /** Skill: Skills, certifications and proficiency ratings used to match the subject to work. */
    public static final String USER_WORKPLACE_SKILL = "user.workplace.skill";

    /** Time Entry: Hours the subject reports on projects and tasks, with descriptions and approvals. */
    public static final String USER_WORKPLACE_TIME_ENTRY = "user.workplace.time_entry";

    /** Utilization: Billable and productive hours of the subject compared with capacity and targets. */
    public static final String USER_WORKPLACE_UTILIZATION = "user.workplace.utilization";

    private ProjectOperationsCategories() {
    }
}
