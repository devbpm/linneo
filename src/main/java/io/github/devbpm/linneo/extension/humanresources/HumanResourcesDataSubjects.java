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
package io.github.devbpm.linneo.extension.humanresources;

/**
 * Keys of the data subjects added by the {@link HumanResourcesTaxonomy}. Like fideslang's data subjects, they are
 * flat: none has a parent.
 *
 * @since 0.1.0
 */
public final class HumanResourcesDataSubjects {

    /** Dependant: A person who depends on a worker and is covered by a benefit or declared for tax purposes. */
    public static final String DEPENDANT = "dependant";

    /** Former Employee: An individual whose employment with the organization has ended. */
    public static final String FORMER_EMPLOYEE = "former_employee";

    /** Reporting Person: An individual who reports breaches through the organization's internal reporting channel. */
    public static final String REPORTING_PERSON = "reporting_person";

    /** Retiree: A former employee who receives a pension or another retirement benefit from the organization. */
    public static final String RETIREE = "retiree";

    /** Temporary Agency Worker: A worker hired by a temporary work agency and assigned to the organization. */
    public static final String TEMPORARY_AGENCY_WORKER = "temporary_agency_worker";

    /** Worker Representative: A worker elected or appointed to represent the workforce, such as on a works council. */
    public static final String WORKER_REPRESENTATIVE = "worker_representative";

    private HumanResourcesDataSubjects() {
    }
}
