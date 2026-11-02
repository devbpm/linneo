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

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyExtension;
import io.github.devbpm.linneo.Trait;

import static io.github.devbpm.linneo.extension.finance.FinanceCategories.*;
import static io.github.devbpm.linneo.extension.finance.FinanceDataSubjects.*;
import static io.github.devbpm.linneo.extension.finance.FinanceDataUses.*;

/**
 * Terms for finance: the organization's books, invoices, payables, receivables, treasury, budgets, fixed assets and
 * taxes, the financial data it holds about customers, suppliers, employees and other people, the finance purposes it
 * processes them for and the people involved.
 * <p>
 * The terms are informed by the finance business areas of the Common Data Model (CDM), copyright Microsoft
 * Corporation, whose documentation and other content are licensed under the CC BY 4.0 licence; no CDM material is
 * reproduced. CDM entities become data categories by what they reveal, not one by one: the general ledger, journals
 * and financial dimensions are the {@linkplain FinanceCategories#ORG_BUSINESS_RECORDS_LEDGER ledger}, travel and
 * expense entities are {@linkplain FinanceCategories#USER_FINANCIAL_EXPENSE expense claims}, and a customer's or
 * supplier's bank account is the standard {@code user.financial.bank_account} when the customer or supplier is a
 * natural person.
 * <p>
 * Records about organizations extend {@code org.business_records}; records about natural persons, including
 * self-employed suppliers and consumers, extend {@code user.financial} or {@code user.workplace}, so that the
 * {@link io.github.devbpm.linneo.classify.Classifier} treats them as personal data. Every term extends an existing
 * class: the data uses sit below the standard {@code finance} use, and the data subjects are flat as in fideslang.
 * No term carries version information, which fideslang reserves for its default taxonomy.
 * <p>
 * Protection follows the standard policy and only raises it:
 * <ul>
 *   <li>creditworthiness, debt collection, tax status and expense policy violations have high confidentiality:
 *       solvency and default data can exclude the subject from credit (LOPDGDD article 20 regulates the files that
 *       share it), tax data is confidential, and a policy violation can lead to disciplinary action;</li>
 *   <li>anti-money laundering due diligence has high confidentiality and integrity: it records whether the subject is
 *       a politically exposed person or appears in sanctions lists, and the law that requires it also requires that
 *       its records be kept intact;</li>
 *   <li>the ledger, financial statements, invoices, taxes, treasury and signing authority have high integrity, since
 *       commercial and tax law require accurate books and invoices and a forged limit authorizes payments;</li>
 *   <li>the other categories inherit the medium confidentiality of {@code org.business_records} and the medium
 *       confidentiality and high integrity of {@code user.financial}.</li>
 * </ul>
 * Data that reveals more than its category belongs in a stricter key: a deduction or payment that reveals trade union
 * membership in {@code user.demographic.trade_union_membership}, an expense that reveals a health condition in
 * {@code user.health_and_medical}, and a suspicious transaction report or a conviction found during due diligence in
 * {@code user.criminal_history}.
 * <p>
 * Like the other extensions shipped with Linneo, it is not registered with {@link java.util.ServiceLoader}, so
 * {@link io.github.devbpm.linneo.Taxonomy#load()} does not apply it. Apply it explicitly, alone or with others:
 * <pre>{@code
 * Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new FinanceTaxonomy()).build();
 * }</pre>
 * or register it as a service of the application, as
 * {@link io.github.devbpm.linneo.extension.university.UniversityTaxonomy} explains. The keys are in
 * {@link FinanceCategories}, {@link FinanceDataUses} and {@link FinanceDataSubjects}.
 *
 * @since 0.1.0
 */
public final class FinanceTaxonomy implements TaxonomyExtension {

    /**
     * The extension's name, recorded as the origin of its terms.
     */
    @SuppressWarnings("java:S1845") // NAME is the value name() returns; the shared name is intended
    public static final String NAME = "finance";

    /**
     * Creates the extension; it holds no state.
     */
    public FinanceTaxonomy() {
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
        traits(builder);
    }

    // The traits Linneo's legal rules read (see Trait): the personal financial categories inherit FINANCIAL from
    // user.financial; credit assessment and anti-money laundering screening are evaluation or scoring (WP248).
    private static void traits(TaxonomyBuilder builder) {
        builder.refineDataUse(FINANCE_CREDIT_MANAGEMENT, u -> u.traits(Trait.EVALUATION))
                .refineDataUse(FINANCE_ANTI_MONEY_LAUNDERING, u -> u.traits(Trait.EVALUATION));
    }

    private static void categories(TaxonomyBuilder builder) {
        builder.category(ORG_BUSINESS_RECORDS_BUDGET, "Budget",
                        "Budget plans, allocations, revisions and the controls applied to them.")
                .category(ORG_BUSINESS_RECORDS_COST_ACCOUNTING, "Cost Accounting",
                        "Cost centres, cost elements, allocations and the costs and results they measure.")
                .category(ORG_BUSINESS_RECORDS_FINANCIAL_STATEMENT, "Financial Statement",
                        "Balance sheets, income statements, cash flow statements, period closing and consolidation.",
                        c -> c.integrity(Integrity.HIGH))
                .category(ORG_BUSINESS_RECORDS_FIXED_ASSET, "Fixed Asset",
                        "The asset register: acquisitions, depreciation, revaluations, transfers and disposals.")
                .category(ORG_BUSINESS_RECORDS_INVOICE, "Invoice",
                        "Invoices issued to and received from organizations, with the content that tax law requires.",
                        c -> c.integrity(Integrity.HIGH))
                .category(ORG_BUSINESS_RECORDS_LEDGER, "Ledger",
                        "Chart of accounts, journal entries, balances and financial dimensions of the organization's"
                                + " books.",
                        c -> c.integrity(Integrity.HIGH))
                .category(ORG_BUSINESS_RECORDS_PAYABLE, "Payable",
                        "Amounts owed to suppliers that are organizations: open items, payment proposals and"
                                + " payments.")
                .category(ORG_BUSINESS_RECORDS_RECEIVABLE, "Receivable",
                        "Amounts owed by customers that are organizations: open items, payments received and"
                                + " settlements.")
                .category(ORG_BUSINESS_RECORDS_TAX, "Tax Record",
                        "Tax codes, calculations, returns and settlements of the organization's own taxes.",
                        c -> c.integrity(Integrity.HIGH))
                .category(ORG_BUSINESS_RECORDS_TREASURY, "Treasury",
                        "The organization's bank accounts, statements, reconciliations, cash position and"
                                + " forecasts.",
                        c -> c.integrity(Integrity.HIGH))
                .category(USER_FINANCIAL_ACCOUNT_BALANCE, "Account Balance",
                        "Open amounts, payments, settlements and payment terms of the subject as a customer or"
                                + " supplier.")
                .category(USER_FINANCIAL_CREDIT, "Creditworthiness",
                        "Credit limits, credit scores, solvency checks and payment default history of the subject.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_FINANCIAL_DEBT_COLLECTION, "Debt Collection",
                        "Reminders, claims, payment plans, collection agency referrals and write-offs of amounts the"
                                + " subject owes.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_FINANCIAL_DUE_DILIGENCE, "Due Diligence",
                        "Identification of the subject, beneficial ownership, politically exposed person status and"
                                + " sanctions screening under anti-money laundering law.",
                        c -> c.confidentiality(Confidentiality.HIGH).integrity(Integrity.HIGH))
                .category(USER_FINANCIAL_EXPENSE, "Expense Claim",
                        "Travel and expense reports, receipts, mileage, per diems, cash advances and corporate card"
                                + " transactions of the subject.")
                .category(USER_FINANCIAL_EXPENSE_POLICY_VIOLATION, "Expense Policy Violation",
                        "Breaches of the expense policy found in the subject's claims, with the justifications given.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_FINANCIAL_INVOICE, "Personal Invoice",
                        "Invoices issued to or by the subject, such as a consumer or a self-employed supplier.")
                .category(USER_FINANCIAL_TAX_STATUS, "Tax Status",
                        "Tax residence, VAT or self-employed status, withholdings on payments to the subject and the"
                                + " information returns that report them.",
                        c -> c.confidentiality(Confidentiality.HIGH))
                .category(USER_WORKPLACE_SIGNING_AUTHORITY, "Signing Authority",
                        "Approval and spending limits delegated to the subject, and their revocation.",
                        c -> c.integrity(Integrity.HIGH));
    }

    private static void dataUses(TaxonomyBuilder builder) {
        builder.dataUse(FINANCE_ACCOUNTING, "Accounting",
                        "Keeps the books and prepares the financial statements that commercial law requires.")
                .dataUse(FINANCE_ACCOUNTS_PAYABLE, "Accounts Payable", "Records, approves and pays supplier invoices.")
                .dataUse(FINANCE_ACCOUNTS_RECEIVABLE, "Accounts Receivable",
                        "Invoices customers and records and reconciles their payments.")
                .dataUse(FINANCE_ANTI_MONEY_LAUNDERING, "Anti-Money Laundering",
                        "Identifies customers and their beneficial owners, screens them against sanctions lists and"
                                + " reports suspicious transactions.")
                .dataUse(FINANCE_AUDIT, "Audit",
                        "Supports internal and external audits of the accounts and of financial controls.")
                .dataUse(FINANCE_BUDGETING, "Budgeting",
                        "Plans, allocates and controls budgets and financial forecasts.")
                .dataUse(FINANCE_COST_ACCOUNTING, "Cost Accounting",
                        "Allocates costs to cost centres, products and activities to measure their results.")
                .dataUse(FINANCE_CREDIT_MANAGEMENT, "Credit Management",
                        "Assesses the creditworthiness of customers and sets and monitors their credit limits.")
                .dataUse(FINANCE_DEBT_COLLECTION, "Debt Collection",
                        "Pursues overdue amounts through reminders, payment plans, collection agencies and claims.")
                .dataUse(FINANCE_EXPENSE_MANAGEMENT, "Expense Management",
                        "Processes travel and expense claims, cash advances and corporate cards, and checks them"
                                + " against policy.")
                .dataUse(FINANCE_FIXED_ASSETS, "Fixed Asset Management",
                        "Records, depreciates and disposes of the organization's assets.")
                .dataUse(FINANCE_TAX, "Tax Compliance",
                        "Calculates, files and pays taxes and keeps the records that tax law requires.")
                .dataUse(FINANCE_TREASURY, "Treasury", "Manages bank accounts, payments, cash and liquidity.");
    }

    private static void dataSubjects(TaxonomyBuilder builder) {
        builder.dataSubject(BENEFICIAL_OWNER, "Beneficial Owner",
                        "An individual who ultimately owns or controls a customer, supplier or other legal entity.")
                .dataSubject(GUARANTOR, "Guarantor",
                        "An individual who guarantees the obligations of a customer or other debtor.")
                .dataSubject(PAYEE, "Payee",
                        "An individual paid by the organization who is not its customer, supplier or employee, such"
                                + " as a claimant or a prize winner.");
    }
}
