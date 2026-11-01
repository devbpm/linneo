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
 * Keys of the data subjects added by the {@link ProjectOperationsTaxonomy}. Like fideslang's data subjects, they are
 * flat: none has a parent.
 *
 * @since 0.1.0
 */
public final class ProjectOperationsDataSubjects {

    /** Project Stakeholder: A person at the customer or a partner involved in a project, such as a sponsor. */
    public static final String PROJECT_STAKEHOLDER = "project_stakeholder";

    /** Subcontractor: An individual who works on the organization's projects under a contract, not an employment. */
    public static final String SUBCONTRACTOR = "subcontractor";

    private ProjectOperationsDataSubjects() {
    }
}
