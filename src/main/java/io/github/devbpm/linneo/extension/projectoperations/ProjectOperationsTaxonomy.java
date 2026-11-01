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

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyExtension;

import static io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsCategories.*;
import static io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsDataSubjects.*;
import static io.github.devbpm.linneo.extension.projectoperations.ProjectOperationsDataUses.*;

/**
 * Terms for project operations, the management of project-based services: projects and their contracts, estimates,
 * actuals, funding and rates, the data about the people who staff them, the purposes they serve and the people
 * involved.
 * <p>
 * The terms are informed by the project management and accounting and the project service automation areas of the
 * Common Data Model (CDM), copyright Microsoft Corporation, whose documentation and other content are licensed under
 * the CC BY 4.0 licence; no CDM material is reproduced. Project records extend {@code org.business_records}; what
 * describes a person as a project resource extends {@code user.workplace}: the
 * {@linkplain ProjectOperationsCategories#USER_WORKPLACE_PROJECT_ASSIGNMENT assignments} and bookings, the
 * {@linkplain ProjectOperationsCategories#USER_WORKPLACE_TIME_ENTRY time entries}, the
 * {@linkplain ProjectOperationsCategories#USER_WORKPLACE_SKILL skills} used to match people to work, their
 * {@linkplain ProjectOperationsCategories#USER_WORKPLACE_UTILIZATION utilization} and their
 * {@linkplain ProjectOperationsCategories#USER_WORKPLACE_RESOURCE_RATE rates}. The project team role is the standard
 * {@code user.workplace.role}.
 * <p>
 * Every term extends an existing class: the data uses sit below the standard {@code operations} use, and the data
 * subjects are flat as in fideslang. No term carries version information, which fideslang reserves for its default
 * taxonomy. The extension does not depend on the finance extension: an expense charged to a project is an expense
 * claim of the finance extension where it is applied, and otherwise the standard {@code user.financial}.
 * <p>
 * Protection follows the standard policy and only raises it:
 * <ul>
 *   <li>project rates and resource rates have high confidentiality: they are commercially sensitive, and the cost
 *       rate of a person reveals what the organization pays them;</li>
 *   <li>time entries and project actuals have high integrity, since customers are billed and revenue is recognised
 *       from them;</li>
 *   <li>utilization has medium confidentiality, since it measures how a worker performs and LOPDGDD articles 87 to
 *       91 limit the monitoring of workers;</li>
 *   <li>the other categories inherit the medium confidentiality of {@code org.business_records} and the low
 *       confidentiality of {@code user.workplace}.</li>
 * </ul>
 * Data that reveals more than its category belongs in a stricter key: the medical reason why a resource is not
 * available in {@code user.health_and_medical}, and a time entry used to evaluate a worker's performance in the
 * performance evaluation of the human resources extension where it is applied.
 * <p>
 * Like the other extensions shipped with Linneo, it is not registered with {@link java.util.ServiceLoader}, so
 * {@link io.github.devbpm.linneo.Taxonomy#load()} does not apply it. Apply it explicitly, alone or with others:
 * <pre>{@code
 * Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new ProjectOperationsTaxonomy()).build();
 * }</pre>
 * or register it as a service of the application, as
 * {@link io.github.devbpm.linneo.extension.university.UniversityTaxonomy} explains. The keys are in
 * {@link ProjectOperationsCategories}, {@link ProjectOperationsDataUses} and {@link ProjectOperationsDataSubjects}.
 *
 * @since 0.1.0
 */
public final class ProjectOperationsTaxonomy implements TaxonomyExtension {

    /**
     * The extension's name, recorded as the origin of its terms.
     */
    @SuppressWarnings("java:S1845") // NAME is the value name() returns; the shared name is intended
    public static final String NAME = "project_operations";

    /**
     * Creates the extension; it holds no state.
     */
    public ProjectOperationsTaxonomy() {
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
        builder.category(ORG_BUSINESS_RECORDS_PROJECT, "Project",
                        "Projects, work breakdown structures, tasks, dependencies, milestones and status.")
                .category(ORG_BUSINESS_RECORDS_PROJECT_ACTUAL, "Project Actual",
                        "Cost, revenue and hours posted to a project.",
                        c -> c.integrity(Integrity.HIGH))
                .category(ORG_BUSINESS_RECORDS_PROJECT_CONTRACT, "Project Contract",
                        "Contracts and contract lines agreed with a customer for a project, with their billing"
                                + " methods and invoice schedules.")
                .category(ORG_BUSINESS_RECORDS_PROJECT_ESTIMATE, "Project Estimate",
                        "Estimates, quotations, budgets and forecasts of a project's cost and revenue.")
                .category(ORG_BUSINESS_RECORDS_PROJECT_FUNDING, "Project Funding",
                        "Funding sources, grants, funding rules and their limits.")
                .category(ORG_BUSINESS_RECORDS_PROJECT_RATE, "Project Rate",
                        "Cost and sales prices of roles, categories and transaction types used to price projects.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_WORKPLACE_PROJECT_ASSIGNMENT, "Project Assignment",
                        "Projects, tasks and roles the subject is assigned to, with bookings and planned hours.")
                .category(USER_WORKPLACE_RESOURCE_RATE, "Resource Rate",
                        "Cost and billing rates set for the subject as a project resource.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_WORKPLACE_SKILL, "Skill",
                        "Skills, certifications and proficiency ratings used to match the subject to work.")
                .category(USER_WORKPLACE_TIME_ENTRY, "Time Entry",
                        "Hours the subject reports on projects and tasks, with their descriptions and approvals.",
                        c -> c.integrity(Integrity.HIGH))
                .category(USER_WORKPLACE_UTILIZATION, "Utilization",
                        "Billable and productive hours of the subject compared with capacity and targets.",
                        c -> c.confidentiality(Confidentiality.MEDIUM));
    }

    private static void dataUses(TaxonomyBuilder builder) {
        builder.dataUse(OPERATIONS_PROJECT_MANAGEMENT, "Project Management", "Plans, executes and controls projects.")
                .dataUse(OPERATIONS_PROJECT_MANAGEMENT_ACCOUNTING, "Project Accounting",
                        "Records project cost and revenue, recognises revenue and reports profitability.")
                .dataUse(OPERATIONS_PROJECT_MANAGEMENT_BILLING, "Project Billing",
                        "Invoices customers for project work as the contract provides.")
                .dataUse(OPERATIONS_PROJECT_MANAGEMENT_ESTIMATING, "Project Estimating",
                        "Prepares estimates and quotations for project work.")
                .dataUse(OPERATIONS_PROJECT_MANAGEMENT_RESOURCE_MANAGEMENT, "Resource Management",
                        "Requests, schedules and books people for projects according to their skills and"
                                + " availability.")
                .dataUse(OPERATIONS_PROJECT_MANAGEMENT_TIME_AND_EXPENSE, "Time and Expense",
                        "Records and approves the time and expenses charged to projects.");
    }

    private static void dataSubjects(TaxonomyBuilder builder) {
        builder.dataSubject(PROJECT_STAKEHOLDER, "Project Stakeholder",
                        "A person at the customer or a partner who is involved in a project, such as a sponsor or an"
                                + " approver.")
                .dataSubject(SUBCONTRACTOR, "Subcontractor",
                        "An individual who works on the organization's projects under a contract with the"
                                + " organization or a supplier, rather than an employment.");
    }
}
