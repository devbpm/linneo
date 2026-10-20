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
 * The exception to the prohibition of processing special categories of personal data that applies to a processing
 * activity (GDPR article 9.2). The names match fideslang's {@code special_category_legal_basis}.
 *
 * @since 0.1.0
 */
public enum SpecialCategoryCondition {

    /**
     * The data subject has given explicit consent.
     */
    EXPLICIT_CONSENT("9.2.a", "Explicit consent", false),

    /**
     * Necessary for obligations and rights in employment, social security and social protection law.
     */
    EMPLOYMENT_AND_SOCIAL_SECURITY("9.2.b", "Employment, social security and social protection", true),

    /**
     * Necessary to protect the vital interests of a person who cannot give consent.
     */
    VITAL_INTERESTS("9.2.c", "Vital interests", false),

    /**
     * Carried out by a not-for-profit body with a political, philosophical, religious or trade union aim, about its
     * members or regular contacts.
     */
    NOT_FOR_PROFIT_BODIES("9.2.d", "Not-for-profit bodies", false),

    /**
     * The data has been manifestly made public by the data subject.
     */
    MADE_PUBLIC_BY_THE_DATA_SUBJECT("9.2.e", "Made public by the data subject", false),

    /**
     * Necessary for legal claims or whenever courts are acting in their judicial capacity.
     */
    LEGAL_CLAIMS("9.2.f", "Legal claims or judicial acts", false),

    /**
     * Necessary for reasons of substantial public interest, on the basis of law.
     */
    SUBSTANTIAL_PUBLIC_INTEREST("9.2.g", "Reasons of substantial public interest (with a basis in law)", true),

    /**
     * Necessary for preventive or occupational medicine, medical diagnosis, or health or social care, on the basis
     * of law.
     */
    HEALTH_OR_SOCIAL_CARE("9.2.h", "Health or social care (with a basis in law)", true),

    /**
     * Necessary for reasons of public interest in public health, on the basis of law.
     */
    PUBLIC_HEALTH("9.2.i", "Public health (with a basis in law)", true),

    /**
     * Necessary for archiving in the public interest, scientific or historical research or statistics, on the
     * basis of law.
     */
    ARCHIVING_RESEARCH_AND_STATISTICS("9.2.j", "Archiving, research and statistics (with a basis in law)", true);

    private final String article;
    private final String fideslangName;
    private final boolean requiresLegalReference;

    SpecialCategoryCondition(String article, String fideslangName, boolean requiresLegalReference) {
        this.article = article;
        this.fideslangName = fideslangName;
        this.requiresLegalReference = requiresLegalReference;
    }

    /**
     * The GDPR provision.
     *
     * @return an article reference such as {@code 9.2.a}
     */
    public String article() {
        return article;
    }

    /**
     * The value of fideslang's {@code special_category_legal_basis}.
     *
     * @return a name such as {@code Explicit consent}
     */
    public String fideslangName() {
        return fideslangName;
    }

    /**
     * Whether the condition rests on Union or Member State law, so a processing activity should name that law.
     *
     * @return {@code true} for the conditions of article 9.2 points b, g, h, i and j
     */
    public boolean requiresLegalReference() {
        return requiresLegalReference;
    }
}
