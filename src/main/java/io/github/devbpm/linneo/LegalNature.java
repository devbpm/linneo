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
package io.github.devbpm.linneo;

/**
 * What a data category is under data protection law: Regulation (EU) 2016/679 (GDPR, text corrected by the
 * corrigendum of OJ L 127, 23.5.2018) and, for Spain, Ley Orgánica 3/2018 (LOPDGDD, consolidated text of
 * 27 December 2025).
 * <p>
 * The nature is fixed by the root of a hierarchy and can only become stricter down the tree: a child of an
 * {@link #ORDINARY} category may be a {@link #SPECIAL_CATEGORY} or a {@link #CRIMINAL_OFFENCE}, and the children of
 * those keep the same nature. A {@link #NONE} hierarchy never contains personal data. Special category and criminal
 * offence data always get the maximum protection ({@link #requiresMaximumProtection()}).
 *
 * @since 0.1.0
 */
public enum LegalNature {

    /**
     * Not personal data: system, organization or reference data.
     */
    NONE,

    /**
     * Personal data (GDPR article 4.1).
     */
    ORDINARY,

    /**
     * Special category of personal data (GDPR article 9.1): racial or ethnic origin, political opinions, religious or
     * philosophical beliefs, trade union membership, genetic data, biometric data processed to uniquely identify a
     * person, health, sex life and sexual orientation.
     * <p>
     * In Spain, consent alone does not lift the prohibition when the main purpose is to identify ideology, trade
     * union membership, religion, sexual orientation, beliefs or racial or ethnic origin (LOPDGDD article 9.1), and
     * processing under GDPR article 9.2 g), h) and i) needs a law (LOPDGDD article 9.2).
     */
    SPECIAL_CATEGORY,

    /**
     * Personal data relating to criminal convictions and offences or related security measures (GDPR article 10,
     * LOPDGDD article 10).
     * <p>
     * Data about administrative infringements and sanctions is not of this nature, although LOPDGDD article 27
     * restricts its processing; it is {@link #ORDINARY} data with high confidentiality.
     */
    CRIMINAL_OFFENCE;

    /**
     * Whether data of this nature is personal data.
     *
     * @return {@code false} only for {@link #NONE}
     */
    public boolean isPersonal() {
        return this != NONE;
    }

    /**
     * Whether data of this nature must have {@link Confidentiality#HIGH HIGH} confidentiality and
     * {@link Integrity#HIGH HIGH} integrity. {@link TaxonomyBuilder#build()} refuses a category of this nature
     * with a lower level.
     *
     * @return {@code true} for {@link #SPECIAL_CATEGORY} and {@link #CRIMINAL_OFFENCE}
     */
    public boolean requiresMaximumProtection() {
        return this == SPECIAL_CATEGORY || this == CRIMINAL_OFFENCE;
    }

    /**
     * Whether a child category may have the given nature when its parent has this one.
     *
     * @param child the nature declared by the child
     * @return {@code true} if {@code child} keeps or strengthens this nature
     */
    public boolean admits(LegalNature child) {
        return switch (this) {
            case NONE -> child == NONE;
            case ORDINARY -> child != NONE;
            case SPECIAL_CATEGORY, CRIMINAL_OFFENCE -> child == this;
        };
    }
}
