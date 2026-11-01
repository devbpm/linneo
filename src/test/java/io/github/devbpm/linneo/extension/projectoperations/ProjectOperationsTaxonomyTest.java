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

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.processing.LegalBasis;
import io.github.devbpm.linneo.processing.ProcessingActivity;
import io.github.devbpm.linneo.processing.ProcessingChecker;
import io.github.devbpm.linneo.processing.ProcessingViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.devbpm.linneo.extension.ExtensionTerms.constants;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectOperationsTaxonomyTest {

    private final Taxonomy projects = Taxonomy.builder().withStandard().extend(new ProjectOperationsTaxonomy())
            .build();

    @Test
    @DisplayName("adds 11 categories, 6 data uses and 2 data subjects")
    void counts() {
        assertAll(
                () -> assertEquals(11, constants(ProjectOperationsCategories.class).size()),
                () -> assertEquals(6, constants(ProjectOperationsDataUses.class).size()),
                () -> assertEquals(2, constants(ProjectOperationsDataSubjects.class).size()));
    }

    @Test
    @DisplayName("rates are highly confidential; time entries and actuals keep integrity")
    void protection() {
        assertAll(
                () -> assertEquals(Set.of(ProjectOperationsCategories.ORG_BUSINESS_RECORDS_PROJECT_RATE,
                                ProjectOperationsCategories.USER_WORKPLACE_RESOURCE_RATE),
                        keys(c -> c.confidentiality() == Confidentiality.HIGH)),
                () -> assertEquals(Set.of(ProjectOperationsCategories.ORG_BUSINESS_RECORDS_PROJECT_ACTUAL,
                                ProjectOperationsCategories.USER_WORKPLACE_TIME_ENTRY),
                        keys(c -> c.integrity() == Integrity.HIGH)),
                () -> assertEquals(Confidentiality.MEDIUM, projects.requireCategory(
                        ProjectOperationsCategories.USER_WORKPLACE_UTILIZATION).confidentiality()),
                () -> assertEquals(Confidentiality.LOW, projects.requireCategory(
                        ProjectOperationsCategories.USER_WORKPLACE_SKILL).confidentiality()));
    }

    @Test
    @DisplayName("project records refine org.business_records and resources refine user.workplace")
    void hierarchy() {
        assertAll(
                () -> assertEquals(ProjectOperationsCategories.ORG_BUSINESS_RECORDS_PROJECT, projects.requireCategory(
                        ProjectOperationsCategories.ORG_BUSINESS_RECORDS_PROJECT_CONTRACT).info().parentKey()),
                () -> assertEquals(Categories.USER_WORKPLACE, projects.requireCategory(
                        ProjectOperationsCategories.USER_WORKPLACE_TIME_ENTRY).info().parentKey()),
                () -> assertEquals(DataUses.OPERATIONS, projects.requireDataUse(
                        ProjectOperationsDataUses.OPERATIONS_PROJECT_MANAGEMENT).info().parentKey()));
    }

    @Test
    @DisplayName("a timesheet is personal data with high integrity")
    void integration() {
        @SuppressWarnings("unused")
        class TimesheetLine {
            @Category(Categories.USER_NAME)
            private String resource;
            @Category(ProjectOperationsCategories.ORG_BUSINESS_RECORDS_PROJECT)
            private String project;
            @Category(ProjectOperationsCategories.USER_WORKPLACE_TIME_ENTRY)
            private Double hours;
        }
        List<FieldClassification> fields = new Classifier(projects).verify(List.of(TimesheetLine.class)).get(0)
                .fields();
        List<ProcessingViolation> violations = new ProcessingChecker(projects).check(ProcessingActivity
                .builder("timesheets", "Project time recording")
                .purposes(ProjectOperationsDataUses.OPERATIONS_PROJECT_MANAGEMENT_TIME_AND_EXPENSE,
                        ProjectOperationsDataUses.OPERATIONS_PROJECT_MANAGEMENT_BILLING)
                .dataSubjects(DataSubjects.EMPLOYEE, ProjectOperationsDataSubjects.SUBCONTRACTOR)
                .entities(TimesheetLine.class)
                .legalBasis(LegalBasis.CONTRACT)
                .retention("Six years, with the invoices they support")
                .build()).violations();
        assertAll(
                () -> assertEquals(List.of(), violations),
                () -> assertEquals(Integrity.HIGH, fields.get(2).category().integrity()));
    }

    private Set<String> keys(Predicate<DataCategory> filter) {
        return constants(ProjectOperationsCategories.class).stream().map(projects::requireCategory).filter(filter)
                .map(DataCategory::key).collect(Collectors.toSet());
    }
}
