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

/**
 * Processing activities and the GDPR record of processing activities (article 30).
 * <p>
 * A {@link io.github.devbpm.linneo.processing.ProcessingActivity} ties together what the taxonomy describes
 * separately: the purposes ({@link io.github.devbpm.linneo.DataUse data uses}), the
 * {@link io.github.devbpm.linneo.DataSubject data subjects} and the personal data, given as category keys or as
 * classified model classes. It also declares the lawful basis
 * ({@link io.github.devbpm.linneo.processing.LegalBasis}), the conditions for special categories and criminal
 * offence data, retention, recipients and transfers. The vocabularies follow fideslang's privacy declarations.
 * <p>
 * {@link io.github.devbpm.linneo.processing.ProcessingChecker} checks the activities against the taxonomy and the
 * classification of their entities and fails, like the classifier, on what would make them unlawful as declared;
 * {@link io.github.devbpm.linneo.processing.ProcessingRecord} renders the result as Markdown or JSON. Linneo checks
 * declarations, not legal advice: the record supports, and does not replace, the controller's own assessment.
 */
package io.github.devbpm.linneo.processing;
