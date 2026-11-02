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
package io.github.devbpm.linneo.extension.finance;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
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
import java.util.stream.Collectors;

import static io.github.devbpm.linneo.extension.ExtensionTerms.constants;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceTaxonomyTest {

    private final Taxonomy finance = Taxonomy.builder().withStandard().extend(new FinanceTaxonomy()).build();

    @Test
    @DisplayName("adds 19 categories, 13 data uses and 3 data subjects")
    void counts() {
        assertAll(
                () -> assertEquals(19, constants(FinanceCategories.class).size()),
                () -> assertEquals(13, constants(FinanceDataUses.class).size()),
                () -> assertEquals(3, constants(FinanceDataSubjects.class).size()));
    }

    @Test
    @DisplayName("solvency, collection, tax and AML data are highly confidential; the books have high integrity")
    void protection() {
        assertAll(
                () -> assertEquals(Set.of(FinanceCategories.USER_FINANCIAL_CREDIT,
                                FinanceCategories.USER_FINANCIAL_DEBT_COLLECTION,
                                FinanceCategories.USER_FINANCIAL_DUE_DILIGENCE,
                                FinanceCategories.USER_FINANCIAL_EXPENSE_POLICY_VIOLATION,
                                FinanceCategories.USER_FINANCIAL_TAX_STATUS),
                        keysWith(Confidentiality.HIGH, null)),
                () -> assertEquals(Set.of(FinanceCategories.ORG_BUSINESS_RECORDS_FINANCIAL_STATEMENT,
                                FinanceCategories.ORG_BUSINESS_RECORDS_INVOICE,
                                FinanceCategories.ORG_BUSINESS_RECORDS_LEDGER,
                                FinanceCategories.ORG_BUSINESS_RECORDS_TAX,
                                FinanceCategories.ORG_BUSINESS_RECORDS_TREASURY,
                                FinanceCategories.USER_WORKPLACE_SIGNING_AUTHORITY),
                        keysWith(null, Integrity.HIGH).stream().filter(k -> !k.startsWith("user.financial."))
                                .collect(Collectors.toSet())),
                () -> assertEquals(Integrity.HIGH, finance.requireCategory(
                        FinanceCategories.USER_FINANCIAL_DUE_DILIGENCE).integrity()),
                () -> assertEquals(Confidentiality.MEDIUM, finance.requireCategory(
                        FinanceCategories.ORG_BUSINESS_RECORDS_LEDGER).confidentiality()),
                () -> assertTrue(constants(FinanceCategories.class).stream().map(finance::requireCategory)
                        .noneMatch(c -> c.nature().requiresMaximumProtection())));
    }

    @Test
    @DisplayName("refines the standard financial terms and the expense claim")
    void hierarchy() {
        assertAll(
                () -> assertEquals(Categories.USER_FINANCIAL, finance.requireCategory(
                        FinanceCategories.USER_FINANCIAL_CREDIT).info().parentKey()),
                () -> assertEquals(FinanceCategories.USER_FINANCIAL_EXPENSE, finance.requireCategory(
                        FinanceCategories.USER_FINANCIAL_EXPENSE_POLICY_VIOLATION).info().parentKey()),
                () -> assertEquals(Categories.ORG_BUSINESS_RECORDS, finance.requireCategory(
                        FinanceCategories.ORG_BUSINESS_RECORDS_LEDGER).info().parentKey()),
                () -> assertEquals(Categories.USER_WORKPLACE, finance.requireCategory(
                        FinanceCategories.USER_WORKPLACE_SIGNING_AUTHORITY).info().parentKey()),
                () -> assertTrue(constants(FinanceDataUses.class).stream()
                        .allMatch(k -> finance.requireDataUse(k).info().parentKey().equals(DataUses.FINANCE))));
    }

    @Test
    @DisplayName("a customer's ledger is checked as personal data and its organization records as non-personal")
    void integration() {
        @SuppressWarnings("unused")
        class CustomerAccount {
            @Category(Categories.USER_NAME)
            private String holder;
            @Category(FinanceCategories.USER_FINANCIAL_ACCOUNT_BALANCE)
            private String openAmount;
            @Category(FinanceCategories.USER_FINANCIAL_CREDIT)
            private Integer creditScore;
            @Category(FinanceCategories.ORG_BUSINESS_RECORDS_LEDGER)
            private String ledgerAccount;
        }
        List<FieldClassification> fields = new Classifier(finance).verify(List.of(CustomerAccount.class)).get(0)
                .fields();
        List<ProcessingViolation> violations = new ProcessingChecker(finance).check(ProcessingActivity
                .builder("credit", "Customer credit management")
                .purposes(FinanceDataUses.FINANCE_CREDIT_MANAGEMENT, FinanceDataUses.FINANCE_ACCOUNTS_RECEIVABLE)
                .dataSubjects(DataSubjects.CUSTOMER, FinanceDataSubjects.GUARANTOR)
                .entities(CustomerAccount.class)
                .legalBasis(LegalBasis.CONTRACT)
                .retention("Six years after the end of the relationship")
                .impactAssessment("DPIA-2026-07: credit scoring of customers")
                .build()).violations();
        assertAll(
                () -> assertEquals(List.of(), violations),
                () -> assertEquals(Confidentiality.HIGH, fields.get(2).category().confidentiality()),
                () -> assertFalse(fields.get(3).category().isPersonal()),
                () -> assertEquals(LegalNature.NONE, fields.get(3).category().nature()));
    }

    // The added categories whose resolved confidentiality and integrity match the given values (null matches any).
    private Set<String> keysWith(Confidentiality confidentiality, Integrity integrity) {
        return constants(FinanceCategories.class).stream().map(finance::requireCategory)
                .filter(c -> confidentiality == null || c.confidentiality() == confidentiality)
                .filter(c -> integrity == null || c.integrity() == integrity)
                .map(c -> c.key()).collect(Collectors.toSet());
    }
}
