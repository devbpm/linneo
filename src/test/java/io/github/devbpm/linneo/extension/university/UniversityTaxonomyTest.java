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
package io.github.devbpm.linneo.extension.university;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.Term;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.catalog.openmetadata.OpenMetadataCatalog;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.processing.LegalBasis;
import io.github.devbpm.linneo.processing.ProcessingActivity;
import io.github.devbpm.linneo.processing.ProcessingChecker;
import io.github.devbpm.linneo.processing.ProcessingViolation;
import io.github.devbpm.linneo.processing.SpecialCategoryCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversityTaxonomyTest {

    private final Taxonomy standard = Taxonomy.standard();
    private final Taxonomy university = Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).build();

    @Test
    @DisplayName("the constants name exactly the terms the extension adds, all recorded with its origin")
    void constantsMatchTerms() {
        assertAll(
                () -> assertEquals(constants(UniversityCategories.class), added(university.categories(),
                        standard.categories())),
                () -> assertEquals(constants(UniversityDataUses.class), added(university.dataUses(),
                        standard.dataUses())),
                () -> assertEquals(constants(UniversityDataSubjects.class), added(university.dataSubjects(),
                        standard.dataSubjects())),
                () -> assertEquals(33, constants(UniversityCategories.class).size()),
                () -> assertEquals(27, constants(UniversityDataUses.class).size()),
                () -> assertEquals(13, constants(UniversityDataSubjects.class).size()),
                () -> assertTrue(university.categories().stream()
                        .filter(c -> constants(UniversityCategories.class).contains(c.key()))
                        .allMatch(c -> c.info().origin().equals(UniversityTaxonomy.NAME))));
    }

    @Test
    @DisplayName("it is not applied by Taxonomy.load(), since the module provides no service")
    void notRegistered() {
        assertFalse(Taxonomy.load().category(UniversityCategories.USER_EDUCATION_ATTENDANCE).isPresent());
    }

    @Test
    @DisplayName("an accommodation is a special category; sensitive records have high confidentiality")
    void protection() {
        DataCategory accommodation = university.requireCategory(UniversityCategories.USER_EDUCATION_ACCOMMODATION);
        assertAll(
                () -> assertEquals(LegalNature.SPECIAL_CATEGORY, accommodation.nature()),
                () -> assertEquals(Confidentiality.HIGH, accommodation.confidentiality()),
                () -> assertEquals(Integrity.HIGH, accommodation.integrity()),
                () -> assertEquals(Set.of(UniversityCategories.USER_EDUCATION_ADVISING,
                                UniversityCategories.USER_EDUCATION_APPLICATION_EVALUATION,
                                UniversityCategories.USER_EDUCATION_APPLICATION_REFERENCE,
                                UniversityCategories.USER_EDUCATION_DISCIPLINARY_RECORD,
                                UniversityCategories.USER_EDUCATION_EQUITY_INDICATOR,
                                UniversityCategories.USER_EDUCATION_RISK_INDICATOR,
                                UniversityCategories.USER_FINANCIAL_WEALTH_ASSESSMENT,
                                UniversityCategories.USER_EDUCATION_PLACEMENT_INTERNSHIP_EVALUATION,
                                UniversityCategories.USER_FINANCIAL_FEE_EXEMPTION,
                                UniversityCategories.USER_FINANCIAL_SCHOLARSHIP_MEANS_ASSESSMENT,
                                UniversityCategories.USER_EDUCATION_ACCOMMODATION),
                        constants(UniversityCategories.class).stream()
                                .filter(k -> university.requireCategory(k).confidentiality() == Confidentiality.HIGH)
                                .collect(Collectors.toSet())),
                () -> assertEquals(Confidentiality.MEDIUM,
                        university.requireCategory(UniversityCategories.USER_CONTACT_EMERGENCY_CONTACT)
                                .confidentiality()),
                () -> assertEquals(Confidentiality.LOW, university.requireCategory(Categories.USER_CONTACT)
                        .confidentiality()),
                () -> assertEquals(Integrity.HIGH, university.requireCategory(
                        UniversityCategories.USER_EDUCATION_ATTENDANCE).integrity()),
                () -> assertTrue(constants(UniversityCategories.class).stream()
                        .map(university::requireCategory).allMatch(DataCategory::isPersonal)));
    }

    @Test
    @DisplayName("follows fideslang's rules for custom labels")
    void fideslangRules() {
        List<Term> added = new ArrayList<>();
        constants(UniversityCategories.class).forEach(k -> added.add(university.requireCategory(k)));
        constants(UniversityDataUses.class).forEach(k -> added.add(university.requireDataUse(k)));
        constants(UniversityDataSubjects.class).forEach(k -> added.add(university.requireDataSubject(k)));
        assertAll(
                // categories extend existing classes; only two new roots of data uses
                () -> assertTrue(constants(UniversityCategories.class).stream()
                        .allMatch(k -> university.requireCategory(k).info().parent().isPresent())),
                () -> assertEquals(List.of(UniversityDataUses.ADVANCEMENT, UniversityDataUses.RESEARCH),
                        constants(UniversityDataUses.class).stream()
                                .filter(k -> university.requireDataUse(k).info().parent().isEmpty()).toList()),
                // data subjects are flat, as in fideslang
                () -> assertTrue(constants(UniversityDataSubjects.class).stream()
                        .allMatch(k -> university.requireDataSubject(k).info().parent().isEmpty())),
                // custom labels carry no version information
                () -> assertTrue(added.stream().allMatch(term -> term.info().since() == null
                        && !term.info().isDeprecated()), "version information on a custom label"));
    }

    @Test
    @DisplayName("the student record, placements and aid refine the standard terms")
    void hierarchy() {
        assertAll(
                () -> assertEquals(Categories.USER_EDUCATION_GRADE, university.requireCategory(
                        UniversityCategories.USER_EDUCATION_GRADE_ATTEMPT).info().parentKey()),
                () -> assertEquals(Categories.USER_EDUCATION_ACCOMPLISHMENT, university.requireCategory(
                        UniversityCategories.USER_EDUCATION_ACCOMPLISHMENT_DEGREE).info().parentKey()),
                () -> assertEquals(Categories.USER_EDUCATION_COURSE_HISTORY, university.requireCategory(
                        UniversityCategories.USER_EDUCATION_COURSE_HISTORY_RECOGNITION).info().parentKey()),
                () -> assertEquals(UniversityCategories.USER_EDUCATION_PLACEMENT_INTERNSHIP, university.requireCategory(
                        UniversityCategories.USER_EDUCATION_PLACEMENT_INTERNSHIP_EVALUATION).info().parentKey()),
                () -> assertEquals(Categories.USER_FINANCIAL_SCHOLARSHIP, university.requireCategory(
                        UniversityCategories.USER_FINANCIAL_SCHOLARSHIP_MEANS_ASSESSMENT).info().parentKey()),
                () -> assertEquals(UniversityCategories.USER_EDUCATION_APPLICATION, university.requireCategory(
                        UniversityCategories.USER_EDUCATION_APPLICATION_EVALUATION).info().parentKey()),
                () -> assertEquals(Integrity.HIGH, university.requireCategory(
                        UniversityCategories.USER_EDUCATION_ACCOMPLISHMENT_DEGREE).integrity()));
    }

    @Test
    @DisplayName("the keys work in annotations, processing activities and catalogs")
    void integration() {
        @SuppressWarnings("unused")
        class ExamArrangement {
            @Category(UniversityCategories.USER_EDUCATION_ACCOMMODATION)
            private String extraTime;
            @Category(Categories.USER_NAME)
            private String student;
        }
        List<FieldClassification> fields = new Classifier(university).verify(List.of(ExamArrangement.class)).get(0)
                .fields();
        List<ProcessingViolation> violations = new ProcessingChecker(university).check(ProcessingActivity
                .builder("accessibility", "Exam accommodations")
                .purposes(UniversityDataUses.EDUCATION_ACCESSIBILITY)
                .dataSubjects(DataSubjects.STUDENT)
                .entities(ExamArrangement.class)
                .legalBasis(LegalBasis.LEGAL_OBLIGATION)
                .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
                .legalReference("Equal Opportunities Act, art. 3")
                .retention("Until the student graduates")
                .impactAssessment("DPIA-2026-07")
                .build()).violations();
        assertAll(
                () -> assertEquals(LegalNature.SPECIAL_CATEGORY, fields.get(0).category().nature()),
                () -> assertEquals(List.of(), violations),
                () -> assertTrue(OpenMetadataCatalog.of(university).tagsOf(fields.get(0))
                        .contains("PersonalData.SpecialCategory")));
    }

    private static Set<String> added(List<? extends Term> all, List<? extends Term> standard) {
        Set<String> keys = all.stream().map(Term::key).collect(Collectors.toCollection(TreeSet::new));
        standard.forEach(term -> keys.remove(term.key()));
        return keys;
    }

    private static Set<String> constants(Class<?> type) {
        return Arrays.stream(type.getFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                .map(UniversityTaxonomyTest::value)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static String value(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
