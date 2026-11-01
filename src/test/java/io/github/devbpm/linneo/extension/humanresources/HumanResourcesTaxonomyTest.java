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

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataCatalog;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.processing.Jurisdiction;
import io.github.devbpm.linneo.processing.LegalBasis;
import io.github.devbpm.linneo.processing.ProcessingActivity;
import io.github.devbpm.linneo.processing.ProcessingChecker;
import io.github.devbpm.linneo.processing.ProcessingViolation;
import io.github.devbpm.linneo.processing.SpecialCategoryCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.devbpm.linneo.extension.ExtensionTerms.constants;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanResourcesTaxonomyTest {

    private final Taxonomy hr = Taxonomy.builder().withStandard().extend(new HumanResourcesTaxonomy()).build();

    @Test
    @DisplayName("adds 21 categories, 15 data uses and 6 data subjects")
    void counts() {
        assertAll(
                () -> assertEquals(21, constants(HumanResourcesCategories.class).size()),
                () -> assertEquals(15, constants(HumanResourcesDataUses.class).size()),
                () -> assertEquals(6, constants(HumanResourcesDataSubjects.class).size()));
    }

    @Test
    @DisplayName("health data and workplace adjustments are special categories with the maximum protection")
    void maximumProtection() {
        assertAll(
                () -> assertEquals(Set.of(HumanResourcesCategories.USER_EMPLOYMENT_WORKPLACE_ADJUSTMENT,
                                HumanResourcesCategories.USER_HEALTH_AND_MEDICAL_OCCUPATIONAL_HEALTH,
                                HumanResourcesCategories.USER_HEALTH_AND_MEDICAL_SICK_LEAVE),
                        keys(c -> c.nature() == LegalNature.SPECIAL_CATEGORY)),
                () -> assertTrue(keys(c -> c.nature() == LegalNature.SPECIAL_CATEGORY).stream()
                        .map(hr::requireCategory).allMatch(c -> c.confidentiality() == Confidentiality.HIGH
                                && c.integrity() == Integrity.HIGH)));
    }

    @Test
    @DisplayName("pay, evaluations, sanctions, monitoring and reports are highly confidential")
    void protection() {
        assertAll(
                () -> assertEquals(Set.of(HumanResourcesCategories.USER_EMPLOYMENT_BACKGROUND_CHECK,
                                HumanResourcesCategories.USER_EMPLOYMENT_COMPENSATION,
                                HumanResourcesCategories.USER_EMPLOYMENT_DISCIPLINARY_RECORD,
                                HumanResourcesCategories.USER_EMPLOYMENT_MONITORING,
                                HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL,
                                HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL_GARNISHMENT,
                                HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL_TAX_WITHHOLDING,
                                HumanResourcesCategories.USER_EMPLOYMENT_PERFORMANCE,
                                HumanResourcesCategories.USER_EMPLOYMENT_RECRUITMENT_ASSESSMENT,
                                HumanResourcesCategories.USER_EMPLOYMENT_TERMINATION,
                                HumanResourcesCategories.USER_EMPLOYMENT_WHISTLEBLOWING),
                        keys(c -> c.confidentiality() == Confidentiality.HIGH
                                && c.nature() != LegalNature.SPECIAL_CATEGORY)),
                () -> assertEquals(Set.of(HumanResourcesCategories.USER_EMPLOYMENT_COMPENSATION,
                                HumanResourcesCategories.USER_EMPLOYMENT_CONTRACT,
                                HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL,
                                HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL_GARNISHMENT,
                                HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL_TAX_WITHHOLDING,
                                HumanResourcesCategories.USER_EMPLOYMENT_WHISTLEBLOWING,
                                HumanResourcesCategories.USER_EMPLOYMENT_WORKING_TIME),
                        keys(c -> c.integrity() == Integrity.HIGH && c.nature() != LegalNature.SPECIAL_CATEGORY)),
                () -> assertEquals(Confidentiality.MEDIUM, hr.requireCategory(
                        HumanResourcesCategories.USER_EMPLOYMENT_LEAVE).confidentiality()));
    }

    @Test
    @DisplayName("employment data is a new branch of user; health terms refine the standard health data")
    void hierarchy() {
        assertAll(
                () -> assertEquals(Categories.USER, hr.requireCategory(HumanResourcesCategories.USER_EMPLOYMENT)
                        .info().parentKey()),
                () -> assertEquals(Categories.USER_HEALTH_AND_MEDICAL, hr.requireCategory(
                        HumanResourcesCategories.USER_HEALTH_AND_MEDICAL_SICK_LEAVE).info().parentKey()),
                () -> assertEquals(DataUses.EMPLOYMENT_RECRUITMENT, hr.requireDataUse(
                        HumanResourcesDataUses.EMPLOYMENT_RECRUITMENT_BACKGROUND_CHECK).info().parentKey()),
                () -> assertTrue(constants(HumanResourcesDataUses.class).stream()
                        .allMatch(k -> k.startsWith(DataUses.EMPLOYMENT + "."))));
    }

    @Test
    @DisplayName("a payslip with sick leave needs a special category condition, and Spain a law behind it")
    void integration() {
        @SuppressWarnings("unused")
        class Payslip {
            @Category(Categories.USER_NAME)
            private String employee;
            @Category(HumanResourcesCategories.USER_EMPLOYMENT_PAYROLL)
            private String earnings;
            @Category(HumanResourcesCategories.USER_HEALTH_AND_MEDICAL_SICK_LEAVE)
            private Integer sickLeaveDays;
        }
        List<FieldClassification> fields = new Classifier(hr).verify(List.of(Payslip.class)).get(0).fields();
        ProcessingActivity.Builder payroll = ProcessingActivity.builder("payroll", "Payroll")
                .purposes(HumanResourcesDataUses.EMPLOYMENT_PAYROLL)
                .dataSubjects(DataSubjects.EMPLOYEE)
                .entities(Payslip.class)
                .legalBasis(LegalBasis.LEGAL_OBLIGATION)
                .retention("Four years after the end of the tax year")
                .impactAssessment("DPIA-2026-11");
        List<ProcessingViolation> withoutCondition = new ProcessingChecker(hr).check(payroll.build()).violations();
        List<ProcessingViolation> spain = new ProcessingChecker(hr, Jurisdiction.SPAIN).check(payroll
                .specialCategoryCondition(SpecialCategoryCondition.EMPLOYMENT_AND_SOCIAL_SECURITY)
                .legalReference("Workers' Statute, art. 29; General Social Security Act, art. 169")
                .build()).violations();
        assertAll(
                () -> assertTrue(withoutCondition.stream().anyMatch(ProcessingViolation::isError),
                        withoutCondition::toString),
                () -> assertEquals(List.of(), spain),
                () -> assertTrue(OpenMetadataCatalog.of(hr).tagsOf(fields.get(2))
                        .contains("PersonalData.SpecialCategory")));
    }

    private Set<String> keys(Predicate<DataCategory> filter) {
        return constants(HumanResourcesCategories.class).stream().map(hr::requireCategory).filter(filter)
                .map(DataCategory::key).collect(Collectors.toSet());
    }
}
