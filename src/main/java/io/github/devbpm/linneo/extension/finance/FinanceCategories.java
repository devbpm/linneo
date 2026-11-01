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

/**
 * Keys of the data categories added by the {@link FinanceTaxonomy}, for use in annotations.
 *
 * @since 0.1.0
 */
public final class FinanceCategories {

    /** Budget: Budget plans, allocations, revisions and the controls applied to them. */
    public static final String ORG_BUSINESS_RECORDS_BUDGET = "org.business_records.budget";

    /** Cost Accounting: Cost centres, cost elements, allocations and the results they measure. */
    public static final String ORG_BUSINESS_RECORDS_COST_ACCOUNTING = "org.business_records.cost_accounting";

    /** Financial Statement: Balance sheets, income and cash flow statements, period closing and consolidation. */
    public static final String ORG_BUSINESS_RECORDS_FINANCIAL_STATEMENT = "org.business_records.financial_statement";

    /** Fixed Asset: The asset register: acquisitions, depreciation, revaluations, transfers and disposals. */
    public static final String ORG_BUSINESS_RECORDS_FIXED_ASSET = "org.business_records.fixed_asset";

    /** Invoice: Invoices issued to and received from organizations, with the content tax law requires. */
    public static final String ORG_BUSINESS_RECORDS_INVOICE = "org.business_records.invoice";

    /** Ledger: Chart of accounts, journal entries, balances and financial dimensions of the books. */
    public static final String ORG_BUSINESS_RECORDS_LEDGER = "org.business_records.ledger";

    /** Payable: Amounts owed to suppliers that are organizations: open items, payment proposals and payments. */
    public static final String ORG_BUSINESS_RECORDS_PAYABLE = "org.business_records.payable";

    /** Receivable: Amounts owed by customers that are organizations: open items, payments received, settlements. */
    public static final String ORG_BUSINESS_RECORDS_RECEIVABLE = "org.business_records.receivable";

    /** Tax Record: Tax codes, calculations, returns and settlements of the organization's own taxes. */
    public static final String ORG_BUSINESS_RECORDS_TAX = "org.business_records.tax";

    /** Treasury: The organization's bank accounts, statements, reconciliations, cash position and forecasts. */
    public static final String ORG_BUSINESS_RECORDS_TREASURY = "org.business_records.treasury";

    /** Account Balance: Open amounts, payments, settlements and payment terms of the subject as a trading party. */
    public static final String USER_FINANCIAL_ACCOUNT_BALANCE = "user.financial.account_balance";

    /** Creditworthiness: Credit limits, credit scores, solvency checks and payment default history of the subject. */
    public static final String USER_FINANCIAL_CREDIT = "user.financial.credit";

    /** Debt Collection: Reminders, claims, payment plans, referrals and write-offs of amounts the subject owes. */
    public static final String USER_FINANCIAL_DEBT_COLLECTION = "user.financial.debt_collection";

    /** Due Diligence: Identification, beneficial ownership, PEP status and sanctions screening under AML law. */
    public static final String USER_FINANCIAL_DUE_DILIGENCE = "user.financial.due_diligence";

    /** Expense Claim: Travel and expense reports, receipts, mileage, per diems, advances and corporate card use. */
    public static final String USER_FINANCIAL_EXPENSE = "user.financial.expense";

    /** Expense Policy Violation: Breaches of the expense policy found in the subject's claims, with justifications. */
    public static final String USER_FINANCIAL_EXPENSE_POLICY_VIOLATION = "user.financial.expense.policy_violation";

    /** Personal Invoice: Invoices issued to or by the subject, such as a consumer or a self-employed supplier. */
    public static final String USER_FINANCIAL_INVOICE = "user.financial.invoice";

    /** Tax Status: Tax residence, VAT or self-employed status, withholdings and the information returns about them. */
    public static final String USER_FINANCIAL_TAX_STATUS = "user.financial.tax_status";

    /** Signing Authority: Approval and spending limits delegated to the subject, and their revocation. */
    public static final String USER_WORKPLACE_SIGNING_AUTHORITY = "user.workplace.signing_authority";

    private FinanceCategories() {
    }
}
