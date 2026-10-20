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
 * What a data category is under data protection law (GDPR).
 * <p>
 * The nature is fixed by the root of a hierarchy and can only become stricter down the tree: a child of an
 * {@link #ORDINARY} category may be a {@link #SPECIAL_CATEGORY} or a {@link #CRIMINAL_OFFENCE}, and the children of
 * those keep the same nature. A {@link #NONE} hierarchy never contains personal data.
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
     * Special category of personal data (GDPR article 9): racial or ethnic origin, political opinions, religious or
     * philosophical beliefs, trade union membership, genetic data, biometric data, health, sex life and sexual
     * orientation.
     */
    SPECIAL_CATEGORY,

    /**
     * Personal data relating to criminal convictions and offences (GDPR article 10).
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
