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
import io.github.devbpm.linneo.processing.ActivityChange.Aspect;
import io.github.devbpm.linneo.processing.ImpactScreening.Criterion;
import io.github.devbpm.linneo.processing.ImpactScreening.Factor;
import io.github.devbpm.linneo.processing.ImpactScreening.Outcome;
import io.github.devbpm.linneo.processing.ProcessingViolation.Code;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpactScreeningTest {

    private final ProcessingChecker checker = new ProcessingChecker(Taxonomy.standard());

    private static ProcessingActivity.Builder newsletter() {
        return ProcessingActivity.builder("newsletter", "Newsletter")
                .purposes(DataUses.MARKETING_COMMUNICATIONS_EMAIL).dataSubjects(DataSubjects.ALUMNUS)
                .dataCategories(Categories.USER_NAME, Categories.USER_CONTACT_EMAIL)
                .legalBasis(LegalBasis.CONSENT).retention("Until consent is withdrawn");
    }

    private static ProcessingActivity.Builder payroll() {
        return ProcessingActivity.builder("payroll", "Payroll")
                .purposes(DataUses.EMPLOYMENT).dataSubjects(DataSubjects.EMPLOYEE)
                .dataCategories(Categories.USER_NAME, Categories.USER_FINANCIAL_BANK_ACCOUNT)
                .legalBasis(LegalBasis.CONTRACT).retention("Four years");
    }

    private static ProcessingActivity.Builder care() {
        return ProcessingActivity.builder("care", "Student health care")
                .purposes(DataUses.EDUCATION).dataSubjects(DataSubjects.STUDENT)
                .dataCategories(Categories.USER_HEALTH_AND_MEDICAL)
                .legalBasis(LegalBasis.PUBLIC_INTEREST).legalReference("Health Act, art. 3")
                .specialCategoryConditions(SpecialCategoryCondition.HEALTH_OR_SOCIAL_CARE).retention("Five years");
    }

    private static Set<Code> codes(CheckedActivity checked) {
        return Set.copyOf(checked.violations().stream().map(ProcessingViolation::code).toList());
    }

    @Test
    @DisplayName("each criterion the code can see is found with its evidence, and the count gives the outcome")
    void criteria() {
        CheckedActivity newsletter = checker.check(newsletter().build());
        CheckedActivity payroll = checker.check(payroll().build());
        CheckedActivity care = checker.check(care().build());
        assertAll(
                () -> assertEquals(Outcome.NOT_INDICATED, newsletter.screening().outcome()),
                () -> assertEquals("Not indicated", newsletter.screening().summary()),
                () -> assertEquals(Outcome.POSSIBLE, care.screening().outcome()),
                () -> assertEquals(Outcome.LIKELY, payroll.screening().outcome()),
                () -> assertEquals(EnumSet.of(Criterion.SENSITIVE_DATA, Criterion.VULNERABLE_SUBJECTS),
                        payroll.screening().criteria()),
                () -> assertEquals(List.of("highly personal data user.financial.bank_account",
                                "data subjects employee"),
                        payroll.screening().findings().stream().map(ImpactScreening.Finding::evidence).toList()),
                () -> assertNull(payroll.screening().mandatoryCase()));
    }

    @Test
    @DisplayName("an assessment likely needed is a warning, a case of article 35.3 without one an error")
    void checkerFollowsTheScreening() {
        CheckedActivity likely = checker.check(payroll().build());
        CheckedActivity largeScale = checker.check(care().riskFactors(Factor.LARGE_SCALE).build());
        CheckedActivity assessed = checker.check(care().riskFactors(Factor.LARGE_SCALE)
                .impactAssessment("DPIA-2026-03").build());
        CheckedActivity possible = checker.check(care().build());
        assertAll(
                () -> assertEquals(Set.of(Code.IMPACT_ASSESSMENT_RECOMMENDED), codes(likely)),
                () -> assertEquals(Set.of(Code.IMPACT_ASSESSMENT_REQUIRED), codes(largeScale)),
                () -> assertTrue(largeScale.errors().getFirst().message().startsWith("GDPR article 35.3.b requires"),
                        () -> largeScale.errors().toString()),
                () -> assertEquals("35.3.b", assessed.screening().mandatoryCase()),
                () -> assertEquals(Set.of(), codes(assessed)),
                () -> assertEquals(Set.of(), codes(possible)));
    }

    @Test
    @DisplayName("the three cases of article 35.3 need the facts only the data protection officer can declare")
    void mandatoryCases() {
        CheckedActivity scoring = checker.check(newsletter().purposes(DataUses.PERSONALIZE_CONTENT_PROFILING)
                .riskFactors(Factor.AUTOMATED_DECISIONS).impactAssessment("DPIA-1").build());
        CheckedActivity cctv = checker.check(ProcessingActivity.builder("cctv", "Video surveillance")
                .purposes(DataUses.OPERATIONS).dataSubjects(DataSubjects.VISITOR)
                .dataCategories(Categories.USER_CONTENT_SELF_IMAGE).legalBasis(LegalBasis.PUBLIC_INTEREST)
                .legalReference("Private Security Act").retention("One month").impactAssessment("DPIA-2")
                .riskFactors(Factor.PUBLICLY_ACCESSIBLE_AREA, Factor.LARGE_SCALE).build());
        assertAll(
                () -> assertEquals("35.3.a", scoring.screening().mandatoryCase()),
                () -> assertEquals("35.3.c", cctv.screening().mandatoryCase()),
                () -> assertTrue(cctv.screening().criteria().containsAll(EnumSet.of(Criterion.SYSTEMATIC_MONITORING,
                        Criterion.LARGE_SCALE))),
                () -> assertTrue(cctv.isValid(), () -> cctv.violations().toString()));
    }

    @Test
    @DisplayName("the record shows the screening of each activity; risk factors change the governance or the scope")
    void recordAndDiff() {
        ProcessingActivity declared = payroll().build();
        ProcessingRecord before = checker.verify(declared);
        ProcessingRecord after = checker.verify(declared.withGovernance(declared.governance().toBuilder()
                .riskFactors(Factor.DATASET_MATCHING).build()));
        ProcessingRecord inCode = checker.verify(payroll().riskFactors(Factor.DATASET_MATCHING).build());
        ActivityChange change = RecordDiff.between(before, after).change("payroll").orElseThrow();
        ActivityChange codeChange = RecordDiff.between(before, inCode).change("payroll").orElseThrow();
        assertAll(
                () -> assertTrue(before.toMarkdown("U").contains("| Impact assessment screening | Likely required (2"
                        + " criteria of WP248): 4. sensitive data:"), () -> before.toMarkdown("U")),
                () -> assertTrue(before.toJson().contains("\"screening\":{\"outcome\":\"LIKELY\",\"criteria\":["
                        + "\"SENSITIVE_DATA\",\"VULNERABLE_SUBJECTS\"]}"), before::toJson),
                // The screening is part of what the record says, so a new criterion changes it.
                () -> assertEquals(EnumSet.of(Aspect.RISK_FACTORS, Aspect.SCREENING), change.aspects()),
                () -> assertTrue(change.isGovernance()),
                () -> assertEquals(EnumSet.of(Aspect.DECLARED_RISK_FACTORS, Aspect.SCREENING), codeChange.aspects()),
                () -> assertFalse(codeChange.isGovernance()),
                () -> assertFalse(after.activities().getFirst().screening().criteria().isEmpty()));
    }
}
