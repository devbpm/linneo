---
title: "Finance"
parent: "Extensions"
grand_parent: "Reference"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/extension/finance/package-info.java
  - src/main/java/io/github/devbpm/linneo/extension/finance/FinanceTaxonomy.java
  - src/main/java/io/github/devbpm/linneo/extension/finance/FinanceCategories.java
  - src/main/java/io/github/devbpm/linneo/extension/finance/FinanceDataUses.java
  - src/main/java/io/github/devbpm/linneo/extension/finance/FinanceDataSubjects.java
-->

# Finance extension

Package `io.github.devbpm.linneo.extension.finance` — exported, `@since 0.1.0`

Terms for finance: the organization's books, invoices, payables, receivables, treasury, budgets, fixed assets and taxes,
the financial data it holds about customers, suppliers, employees and other people, the finance purposes it processes
them for and the people involved. The extension is optional and not applied by default; see [Extensions](README.md).

It is informed by the finance business areas of the Common Data Model (CDM) (see [Sources](#sources)); no CDM material
is reproduced.

CDM entities become data categories by what they reveal, not one by one: the general ledger, journals and financial
dimensions are the `org.business_records.ledger` category, travel and expense entities are `user.financial.expense`
(expense claims), and a customer's or supplier's bank account is the standard `user.financial.bank_account` when the
customer or supplier is a natural person.

Records about organizations extend `org.business_records`, which is not personal data; records about natural persons,
including self-employed suppliers and consumers, extend `user.financial` or `user.workplace`, so that the `Classifier`
treats them as personal data.

| Type | Description |
|---|---|
| `FinanceTaxonomy` | `public final class implements TaxonomyExtension`. `name()` returns `NAME`, `public static final String NAME = "finance"`, recorded as the origin of its terms. Public no-argument constructor; no state. `contribute(builder)` adds the terms listed below. |
| `FinanceCategories` | `public final class`, private constructor: one `public static final String` per data category the extension adds (19). |
| `FinanceDataUses` | The same for its data uses (13). |
| `FinanceDataSubjects` | The same for its data subjects (3). |

The constants follow the [naming rule](../key-constants.md#naming-rule) of `Categories`: `user.financial.expense` is
`FinanceCategories.USER_FINANCIAL_EXPENSE`, `finance.accounts_payable` is `FinanceDataUses.FINANCE_ACCOUNTS_PAYABLE`.
They are compile-time constants, valid in annotations once the extension is applied.

## Applying it

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new FinanceTaxonomy()).build();
// Taxonomy[145 data categories, 76 data uses, 25 data subjects]
```

The extension shares no key with the other shipped extensions, so it can be applied with any of them, in the same
builder:

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new FinanceTaxonomy())
        .extend(new UniversityTaxonomy())   // io.github.devbpm.linneo.extension.university.UniversityTaxonomy
        .build();
```

`Taxonomy.load()` applies it only if the application registers it; see [Registering a shipped
extension](README.md#registering-a-shipped-extension).

A model then uses the extension's keys next to the standard ones:

```java
package com.example.app;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.extension.finance.FinanceCategories;

import java.math.BigDecimal;

public class ExpenseClaim {
    @Category(Categories.USER_UNIQUE_ID)
    private String employeeNumber;

    @Category(FinanceCategories.USER_FINANCIAL_EXPENSE)
    private BigDecimal amount;

    @Category(FinanceCategories.USER_FINANCIAL_EXPENSE_POLICY_VIOLATION)
    private String justification;

    @Category(Categories.USER_HEALTH_AND_MEDICAL)   // a claim that reveals a health condition
    private String treatment;
}
```

```java
new Classifier(taxonomy).verify(List.of(ExpenseClaim.class)).get(0).fields().forEach(f ->
        System.out.println(f.path() + ": " + f.category().key() + " " + f.category().nature() + " "
                + f.confidentiality() + " " + f.integrity()));
```

```text
employeeNumber: user.unique_id ORDINARY LOW MEDIUM
amount: user.financial.expense ORDINARY MEDIUM HIGH
justification: user.financial.expense.policy_violation ORDINARY HIGH HIGH
treatment: user.health_and_medical SPECIAL_CATEGORY HIGH HIGH
```

## Fideslang rules for custom labels

The extension follows the rules fideslang sets for custom labels, so its terms stay interoperable with other tools:

- **Extend existing classes.** Every term is a child of an existing term; every use is a child of the standard `finance`
  use.
- **The parent is the prefix of the key.** `user.financial.expense` is a child of `user.financial`.
- **Data subjects are flat.** fideslang data subjects have no hierarchy, so every subject of the extension is a root.
- **No version information.** fideslang allows version information only in its default taxonomy, so no term of the
  extension carries any; its origin is recorded as `finance`.

## Data categories

The protection shown is the effective one in the built taxonomy. Values in **bold** are declared by the extension; the
rest are inherited from the parent (`org.business_records` is `NONE`, `MEDIUM`, `MEDIUM`; `user.financial` is
`ORDINARY`, `MEDIUM`, `HIGH`; `user.workplace` is `ORDINARY`, `LOW`, `MEDIUM`). The categories below
`org.business_records` are not personal data; the rest are. No category is secret.

### The organization's books

Records about the organization itself, below `org.business_records`: they are not personal data.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `org.business_records.budget` | Budget | `NONE` | `MEDIUM` | `MEDIUM` | Budget plans, allocations, revisions and the controls applied to them. |
| `org.business_records.cost_accounting` | Cost Accounting | `NONE` | `MEDIUM` | `MEDIUM` | Cost centres, cost elements, allocations and the costs and results they measure. |
| `org.business_records.financial_statement` | Financial Statement | `NONE` | `MEDIUM` | **`HIGH`** | Balance sheets, income statements, cash flow statements, period closing and consolidation. |
| `org.business_records.fixed_asset` | Fixed Asset | `NONE` | `MEDIUM` | `MEDIUM` | The asset register: acquisitions, depreciation, revaluations, transfers and disposals. |
| `org.business_records.invoice` | Invoice | `NONE` | `MEDIUM` | **`HIGH`** | Invoices issued to and received from organizations, with the content that tax law requires. |
| `org.business_records.ledger` | Ledger | `NONE` | `MEDIUM` | **`HIGH`** | Chart of accounts, journal entries, balances and financial dimensions of the organization's books. |
| `org.business_records.payable` | Payable | `NONE` | `MEDIUM` | `MEDIUM` | Amounts owed to suppliers that are organizations: open items, payment proposals and payments. |
| `org.business_records.receivable` | Receivable | `NONE` | `MEDIUM` | `MEDIUM` | Amounts owed by customers that are organizations: open items, payments received and settlements. |
| `org.business_records.tax` | Tax Record | `NONE` | `MEDIUM` | **`HIGH`** | Tax codes, calculations, returns and settlements of the organization's own taxes. |
| `org.business_records.treasury` | Treasury | `NONE` | `MEDIUM` | **`HIGH`** | The organization's bank accounts, statements, reconciliations, cash position and forecasts. |

### Customers, suppliers and other people

Financial data about natural persons, below `user.financial`.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `user.financial.account_balance` | Account Balance | `ORDINARY` | `MEDIUM` | `HIGH` | Open amounts, payments, settlements and payment terms of the subject as a customer or supplier. |
| `user.financial.credit` | Creditworthiness | `ORDINARY` | **`HIGH`** | `HIGH` | Credit limits, credit scores, solvency checks and payment default history of the subject. |
| `user.financial.debt_collection` | Debt Collection | `ORDINARY` | **`HIGH`** | `HIGH` | Reminders, claims, payment plans, collection agency referrals and write-offs of amounts the subject owes. |
| `user.financial.due_diligence` | Due Diligence | `ORDINARY` | **`HIGH`** | `HIGH` | Identification of the subject, beneficial ownership, politically exposed person status and sanctions screening under anti-money laundering law. |
| `user.financial.expense` | Expense Claim | `ORDINARY` | `MEDIUM` | `HIGH` | Travel and expense reports, receipts, mileage, per diems, cash advances and corporate card transactions of the subject. |
| `user.financial.expense.policy_violation` | Expense Policy Violation | `ORDINARY` | **`HIGH`** | `HIGH` | Breaches of the expense policy found in the subject's claims, with the justifications given. |
| `user.financial.invoice` | Personal Invoice | `ORDINARY` | `MEDIUM` | `HIGH` | Invoices issued to or by the subject, such as a consumer or a self-employed supplier. |
| `user.financial.tax_status` | Tax Status | `ORDINARY` | **`HIGH`** | `HIGH` | Tax residence, VAT or self-employed status, withholdings on payments to the subject and the information returns that report them. |

### Signing authority

The limits delegated to a person, below `user.workplace`.

| Key | Name | Nature | Confidentiality | Integrity | Holds |
|---|---|---|---|---|---|
| `user.workplace.signing_authority` | Signing Authority | `ORDINARY` | `LOW` | **`HIGH`** | Approval and spending limits delegated to the subject, and their revocation. |

### Why these levels

- **Creditworthiness, debt collection, tax status and expense policy violations** have high confidentiality: solvency
  and default data can exclude the subject from credit (LOPDGDD article 20 regulates the files that share it), tax data
  is confidential, and a policy violation can lead to disciplinary action.
- **Anti-money laundering due diligence** (`user.financial.due_diligence`) has high confidentiality and integrity: it
  records whether the subject is a politically exposed person or appears in sanctions lists, and the law that requires
  it also requires that its records be kept intact.
- The **ledger, financial statements, invoices, taxes, treasury and signing authority** have high integrity, since
  commercial and tax law require accurate books and invoices and a forged limit authorizes payments.
- The other categories inherit the medium confidentiality of `org.business_records` and the medium confidentiality and
  high integrity of `user.financial`.

## Where stricter data goes

A record of an extension category may contain details that reveal more than the category. Classify those fields with the
stricter key, not with the extension's:

| The field holds | Classify it as | Protection |
|---|---|---|
| A deduction or payment that reveals trade union membership | `user.demographic.trade_union_membership` | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| An expense that reveals a health condition | `user.health_and_medical` (not `user.financial.expense`) | `SPECIAL_CATEGORY`, `HIGH`, `HIGH` |
| A suspicious transaction report, or a conviction found during due diligence | `user.criminal_history` (not `user.financial.due_diligence`) | `CRIMINAL_OFFENCE`, `HIGH`, `HIGH` |

## Data uses

Every use is a child of the standard `finance` use. Data uses have no protection facets.

| Key | Name | Parent | Purpose |
|---|---|---|---|
| `finance.accounting` | Accounting | `finance` | Keeps the books and prepares the financial statements that commercial law requires. |
| `finance.accounts_payable` | Accounts Payable | `finance` | Records, approves and pays supplier invoices. |
| `finance.accounts_receivable` | Accounts Receivable | `finance` | Invoices customers and records and reconciles their payments. |
| `finance.anti_money_laundering` | Anti-Money Laundering | `finance` | Identifies customers and their beneficial owners, screens them against sanctions lists and reports suspicious transactions. |
| `finance.audit` | Audit | `finance` | Supports internal and external audits of the accounts and of financial controls. |
| `finance.budgeting` | Budgeting | `finance` | Plans, allocates and controls budgets and financial forecasts. |
| `finance.cost_accounting` | Cost Accounting | `finance` | Allocates costs to cost centres, products and activities to measure their results. |
| `finance.credit_management` | Credit Management | `finance` | Assesses the creditworthiness of customers and sets and monitors their credit limits. |
| `finance.debt_collection` | Debt Collection | `finance` | Pursues overdue amounts through reminders, payment plans, collection agencies and claims. |
| `finance.expense_management` | Expense Management | `finance` | Processes travel and expense claims, cash advances and corporate cards, and checks them against policy. |
| `finance.fixed_assets` | Fixed Asset Management | `finance` | Records, depreciates and disposes of the organization's assets. |
| `finance.tax` | Tax Compliance | `finance` | Calculates, files and pays taxes and keeps the records that tax law requires. |
| `finance.treasury` | Treasury | `finance` | Manages bank accounts, payments, cash and liquidity. |

## Data subjects

All the data subjects of the extension are flat, as fideslang data subjects have no hierarchy.

| Key | Name | Who |
|---|---|---|
| `beneficial_owner` | Beneficial Owner | An individual who ultimately owns or controls a customer, supplier or other legal entity. |
| `guarantor` | Guarantor | An individual who guarantees the obligations of a customer or other debtor. |
| `payee` | Payee | An individual paid by the organization who is not its customer, supplier or employee, such as a claimant or a prize winner. |

## Standard keys for everything else

The extension adds only what has no standard key. What the standard taxonomy already covers keeps its standard key:

| Attribute | Standard key | Protection |
|---|---|---|
| The bank account of a customer or supplier that is a natural person | `user.financial.bank_account` | `ORDINARY`, `MEDIUM`, `HIGH` |
| Other financial data about a natural person | `user.financial` and its children | `ORDINARY`, `MEDIUM`, `HIGH` |
| Other data about an organization | `org.business_records` and its children | `NONE`, `MEDIUM`, `MEDIUM` |
| Workplace data of a person | `user.workplace` and its children | `ORDINARY`, `LOW`, `MEDIUM` |

## Sources

The terms are informed by the finance business areas of the **Common Data Model (CDM)** schemas:
<https://github.com/microsoft/CDM> (documentation: <https://learn.microsoft.com/common-data-model/>). Copyright (c)
Microsoft Corporation. The documentation and other content of CDM are licensed under [Creative Commons Attribution 4.0
International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/); its code is under the MIT licence.

No CDM material (schemas, entity or attribute definitions, texts or enumerations) is reproduced in Linneo; CDM is cited
as the source of the concepts, and the names and descriptions of the terms are Linneo's. Linneo is not affiliated with
or endorsed by Microsoft. See also `NOTICE` and the "Licence and attribution" section of the `README`.

See also: [Extensions](README.md) · [Standard taxonomy](../standard-taxonomy.md) · [Key constants](../key-constants.md)
· [Protection levels](../protection-levels.md)
