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
 * Keys of the data subjects added by the {@link FinanceTaxonomy}. Like fideslang's data subjects, they are flat:
 * none has a parent.
 *
 * @since 0.1.0
 */
public final class FinanceDataSubjects {

    /** Beneficial Owner: An individual who ultimately owns or controls a customer, supplier or other legal entity. */
    public static final String BENEFICIAL_OWNER = "beneficial_owner";

    /** Guarantor: An individual who guarantees the obligations of a customer or other debtor. */
    public static final String GUARANTOR = "guarantor";

    /** Payee: An individual paid by the organization who is not its customer, supplier or employee. */
    public static final String PAYEE = "payee";

    private FinanceDataSubjects() {
    }
}
