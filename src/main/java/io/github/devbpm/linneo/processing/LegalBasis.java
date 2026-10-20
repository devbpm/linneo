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
 * The lawful basis of a processing activity (GDPR article 6.1). The names match fideslang's
 * {@code legal_basis_for_processing}.
 *
 * @since 0.1.0
 */
public enum LegalBasis {

    /**
     * The data subject has given consent for one or more specific purposes.
     */
    CONSENT("6.1.a", "Consent", false),

    /**
     * Necessary for a contract with the data subject, or to take steps at their request before entering into one.
     */
    CONTRACT("6.1.b", "Contract", false),

    /**
     * Necessary to comply with a legal obligation of the controller; the obligation is laid down by law.
     */
    LEGAL_OBLIGATION("6.1.c", "Legal obligations", true),

    /**
     * Necessary to protect the vital interests of the data subject or of another natural person.
     */
    VITAL_INTERESTS("6.1.d", "Vital interests", false),

    /**
     * Necessary for a task carried out in the public interest or in the exercise of official authority; the task
     * is laid down by law.
     */
    PUBLIC_INTEREST("6.1.e", "Public interest", true),

    /**
     * Necessary for the legitimate interests of the controller or a third party, balanced against the data
     * subject's rights.
     */
    LEGITIMATE_INTERESTS("6.1.f", "Legitimate interests", false);

    private final String article;
    private final String fideslangName;
    private final boolean requiresLegalReference;

    LegalBasis(String article, String fideslangName, boolean requiresLegalReference) {
        this.article = article;
        this.fideslangName = fideslangName;
        this.requiresLegalReference = requiresLegalReference;
    }

    /**
     * The GDPR provision.
     *
     * @return an article reference such as {@code 6.1.f}
     */
    public String article() {
        return article;
    }

    /**
     * The value of fideslang's {@code legal_basis_for_processing}.
     *
     * @return a name such as {@code Legitimate interests}
     */
    public String fideslangName() {
        return fideslangName;
    }

    /**
     * Whether the basis must be laid down by Union or Member State law (GDPR article 6.3), so a processing
     * activity should name that law.
     *
     * @return {@code true} for {@link #LEGAL_OBLIGATION} and {@link #PUBLIC_INTEREST}
     */
    public boolean requiresLegalReference() {
        return requiresLegalReference;
    }
}
