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
 * Keys of the data uses added by the {@link FinanceTaxonomy}, all below the standard {@code finance} use.
 *
 * @since 0.1.0
 */
public final class FinanceDataUses {

    /** Accounting: Keeps the books and prepares the financial statements that commercial law requires. */
    public static final String FINANCE_ACCOUNTING = "finance.accounting";

    /** Accounts Payable: Records, approves and pays supplier invoices. */
    public static final String FINANCE_ACCOUNTS_PAYABLE = "finance.accounts_payable";

    /** Accounts Receivable: Invoices customers and records and reconciles their payments. */
    public static final String FINANCE_ACCOUNTS_RECEIVABLE = "finance.accounts_receivable";

    /** Anti-Money Laundering: Identifies customers and beneficial owners, screens them, reports suspicious activity. */
    public static final String FINANCE_ANTI_MONEY_LAUNDERING = "finance.anti_money_laundering";

    /** Audit: Supports internal and external audits of the accounts and of financial controls. */
    public static final String FINANCE_AUDIT = "finance.audit";

    /** Budgeting: Plans, allocates and controls budgets and financial forecasts. */
    public static final String FINANCE_BUDGETING = "finance.budgeting";

    /** Cost Accounting: Allocates costs to cost centres, products and activities to measure their results. */
    public static final String FINANCE_COST_ACCOUNTING = "finance.cost_accounting";

    /** Credit Management: Assesses the creditworthiness of customers and sets and monitors their credit limits. */
    public static final String FINANCE_CREDIT_MANAGEMENT = "finance.credit_management";

    /** Debt Collection: Pursues overdue amounts through reminders, payment plans, collection agencies and claims. */
    public static final String FINANCE_DEBT_COLLECTION = "finance.debt_collection";

    /** Expense Management: Processes expense claims, cash advances and corporate cards, and checks them. */
    public static final String FINANCE_EXPENSE_MANAGEMENT = "finance.expense_management";

    /** Fixed Asset Management: Records, depreciates and disposes of the organization's assets. */
    public static final String FINANCE_FIXED_ASSETS = "finance.fixed_assets";

    /** Tax Compliance: Calculates, files and pays taxes and keeps the records that tax law requires. */
    public static final String FINANCE_TAX = "finance.tax";

    /** Treasury: Manages bank accounts, payments, cash and liquidity. */
    public static final String FINANCE_TREASURY = "finance.treasury";

    private FinanceDataUses() {
    }
}
