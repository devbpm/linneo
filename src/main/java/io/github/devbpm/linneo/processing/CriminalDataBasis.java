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
 * What allows a processing activity to handle personal data relating to criminal convictions and offences (GDPR
 * article 10).
 *
 * @since 0.1.0
 */
public enum CriminalDataBasis {

    /**
     * The processing is carried out under the control of official authority.
     */
    OFFICIAL_AUTHORITY,

    /**
     * The processing is authorised by Union or Member State law that provides appropriate safeguards; a processing
     * activity should name that law.
     */
    AUTHORISED_BY_LAW;

    /**
     * The GDPR provision.
     *
     * @return {@code 10}
     */
    public String article() {
        return "10";
    }

    /**
     * Whether the basis rests on a law that a processing activity should name.
     *
     * @return {@code true} for {@link #AUTHORISED_BY_LAW}
     */
    public boolean requiresLegalReference() {
        return this == AUTHORISED_BY_LAW;
    }
}
