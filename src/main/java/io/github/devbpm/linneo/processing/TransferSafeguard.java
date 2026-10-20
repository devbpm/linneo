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

/**
 * What allows a transfer of personal data to a third country or an international organisation (GDPR chapter V).
 * The names match fideslang's {@code legal_basis_for_transfers}.
 *
 * @since 0.1.0
 */
public enum TransferSafeguard {

    /**
     * The Commission has decided that the destination ensures an adequate level of protection.
     */
    ADEQUACY_DECISION("45", "Adequacy Decision"),

    /**
     * Standard data protection clauses adopted by the Commission.
     */
    STANDARD_CONTRACTUAL_CLAUSES("46.2.c", "SCCs"),

    /**
     * Binding corporate rules approved by a supervisory authority.
     */
    BINDING_CORPORATE_RULES("47", "BCRs"),

    /**
     * A derogation for a specific situation, such as explicit consent or the performance of a contract.
     */
    DEROGATION("49", "Other");

    private final String article;
    private final String fideslangName;

    TransferSafeguard(String article, String fideslangName) {
        this.article = article;
        this.fideslangName = fideslangName;
    }

    /**
     * The GDPR provision.
     *
     * @return an article reference such as {@code 46.2.c}
     */
    public String article() {
        return article;
    }

    /**
     * The value of fideslang's {@code legal_basis_for_transfers}.
     *
     * @return a name such as {@code SCCs}
     */
    public String fideslangName() {
        return fideslangName;
    }
}
