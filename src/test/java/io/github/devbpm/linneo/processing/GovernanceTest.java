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
package io.github.devbpm.linneo.processing;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.processing.ProcessingViolation.Code;
import io.github.devbpm.linneo.processing.ProcessingViolation.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GovernanceTest {

    private final ProcessingChecker checker = new ProcessingChecker(Taxonomy.standard(), Jurisdiction.SPAIN);

    // The scope, as the code of the application declares it: no governance yet.
    private static ProcessingActivity scope() {
        return ProcessingActivity.builder("support", "Learning support")
                .purposes(DataUses.EDUCATION_TEACHING)
                .dataSubjects(DataSubjects.STUDENT)
                .dataCategories(Categories.USER_HEALTH_AND_MEDICAL_DISABILITY, Categories.USER_NAME)
                .build();
    }

    // What the data protection officer keeps, as an application would read it from its own storage.
    private static Governance stored() {
        return Governance.builder()
                .legalBasis(LegalBasis.PUBLIC_INTEREST)
                .specialCategoryCondition(SpecialCategoryCondition.SUBSTANTIAL_PUBLIC_INTEREST)
                .legalReference("Ley Orgánica 2/2023, del Sistema Universitario, art. 37")
                .retention("Until the end of the studies")
                .recipients("Teaching staff")
                .impactAssessment("DPIA-2026-07")
                .build();
    }

    @Test
    @DisplayName("an activity declared in code has no governance; its gaps are governance problems, not code ones")
    void scopeOnly() {
        CheckedActivity checked = checker.check(scope());
        Map<Element, List<Code>> errors = checked.errors().stream().collect(Collectors.groupingBy(
                ProcessingViolation::element, Collectors.mapping(ProcessingViolation::code, Collectors.toList())));
        assertAll(
                () -> assertEquals(Governance.NONE, scope().governance()),
                () -> assertEquals(Map.of(Element.LEGAL_BASIS, List.of(Code.MISSING_LEGAL_BASIS),
                        Element.SPECIAL_CATEGORY_CONDITION, List.of(Code.MISSING_SPECIAL_CATEGORY_CONDITION)), errors),
                () -> assertTrue(checked.errors().stream().allMatch(v -> v.element().isGovernance())));
    }

    @Test
    @DisplayName("the stored governance completes the activity without touching its scope")
    void withGovernance() {
        ProcessingActivity complete = scope().withGovernance(stored());
        CheckedActivity checked = checker.check(complete);
        assertAll(
                () -> assertEquals(List.of(), checked.violations()),
                () -> assertEquals(scope().purposes(), complete.purposes()),
                () -> assertEquals(stored(), complete.governance()),
                () -> assertEquals(List.of("Teaching staff"), complete.governance().recipients()));
    }

    @Test
    @DisplayName("a change by the officer is checked like any other, and code problems are told apart")
    void officerChange() {
        Governance consentOnly = stored().toBuilder()
                .specialCategoryCondition(SpecialCategoryCondition.EXPLICIT_CONSENT).build();
        ProcessingActivity unknownPurpose = ProcessingActivity.builder("x", "X").purposes("education.unknown")
                .dataSubjects(DataSubjects.STUDENT).dataCategories(Categories.USER_NAME).governance(stored()).build();
        CheckedActivity changed = checker.check(scope().withGovernance(consentOnly));
        CheckedActivity wrongCode = checker.check(unknownPurpose);
        assertAll(
                // explicit consent is a valid condition for disability data, so nothing blocks; the record is fine
                () -> assertTrue(changed.isValid(), () -> changed.violations().toString()),
                () -> assertEquals(List.of(Element.PURPOSES), wrongCode.errors().stream()
                        .map(ProcessingViolation::element).toList()),
                () -> assertFalse(wrongCode.errors().getFirst().element().isGovernance()));
    }

    @Test
    @DisplayName("the builder's governance methods give initial values that governance() replaces as a whole")
    void builder() {
        ProcessingActivity replaced = ProcessingActivity.builder("a", "A").retention("One year")
                .governance(Governance.builder().legalBasis(LegalBasis.CONTRACT).build()).build();
        ProcessingActivity added = ProcessingActivity.builder("a", "A").governance(stored()).recipients("Auditors")
                .build();
        assertAll(
                () -> assertNull(replaced.governance().retention()),
                () -> assertEquals(LegalBasis.CONTRACT, replaced.governance().legalBasis()),
                () -> assertEquals(List.of("Teaching staff", "Auditors"), added.governance().recipients()),
                () -> assertNull(Governance.builder().retention("  ").build().retention()));
    }
}
