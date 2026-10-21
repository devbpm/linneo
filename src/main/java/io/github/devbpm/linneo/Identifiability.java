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
 * How directly a stored value of personal data points to a person.
 * <p>
 * It belongs to the field, not to the category: the same category can be stored identified in one table and
 * pseudonymized in another. Pseudonymized data is still personal data (GDPR recital 26); anonymized and aggregated
 * data is not, as long as no one can re-identify the person.
 *
 * @since 0.1.0
 */
public enum Identifiability {

    /**
     * The value identifies the person directly or through other data the controller holds.
     */
    IDENTIFIED,

    /**
     * The value can be linked to the person only with additional information kept separately (GDPR article 4.5).
     */
    PSEUDONYMIZED,

    /**
     * The value can no longer be linked to the person.
     */
    ANONYMIZED,

    /**
     * The value summarizes several people and does not describe any one of them.
     */
    AGGREGATED
}
