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
 * Keys of the data uses added by the {@link ProjectOperationsTaxonomy}, below the standard {@code operations} use.
 *
 * @since 0.1.0
 */
public final class ProjectOperationsDataUses {

    /** Project Management: Plans, executes and controls projects. */
    public static final String OPERATIONS_PROJECT_MANAGEMENT = "operations.project_management";

    /** Project Accounting: Records project cost and revenue, recognises revenue and reports profitability. */
    public static final String OPERATIONS_PROJECT_MANAGEMENT_ACCOUNTING = "operations.project_management.accounting";

    /** Project Billing: Invoices customers for project work as the contract provides. */
    public static final String OPERATIONS_PROJECT_MANAGEMENT_BILLING = "operations.project_management.billing";

    /** Project Estimating: Prepares estimates and quotations for project work. */
    public static final String OPERATIONS_PROJECT_MANAGEMENT_ESTIMATING = "operations.project_management.estimating";

    /** Resource Management: Requests, schedules and books people for projects by their skills and availability. */
    public static final String OPERATIONS_PROJECT_MANAGEMENT_RESOURCE_MANAGEMENT =
            "operations.project_management.resource_management";

    /** Time and Expense: Records and approves the time and expenses charged to projects. */
    public static final String OPERATIONS_PROJECT_MANAGEMENT_TIME_AND_EXPENSE =
            "operations.project_management.time_and_expense";

    private ProjectOperationsDataUses() {
    }
}
