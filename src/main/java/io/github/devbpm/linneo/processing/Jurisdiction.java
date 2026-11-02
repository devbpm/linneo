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
 * The national data protection law a {@link ProcessingChecker} applies on top of the GDPR.
 *
 * @since 0.1.0
 */
public enum Jurisdiction {

    /**
     * The GDPR alone, with no national rules.
     */
    EU,

    /**
     * The GDPR and Spain's Ley Orgánica 3/2018 (LOPDGDD, consolidated text of 27 December 2025):
     * <ul>
     *   <li>explicit consent alone does not lift the prohibition of processing ideology, trade union membership,
     *       religion, sexual orientation, beliefs or racial or ethnic origin (article 9.1): an error when it is the
     *       only condition, a warning to confirm another condition covers them otherwise;</li>
     *   <li>a legal obligation or a task in the public interest (article 8), GDPR article 9.2 g), h) and i)
     *       (article 9.2) and criminal offence data authorised by law (article 10) rest on a law with the rank of
     *       statute, which the warning about a missing legal reference says;</li>
     *   <li>data on administrative infringements and sanctions needs consent or such a law, unless the controller is
     *       the competent body (article 27), a warning;</li>
     *   <li>erased or rectified data is blocked before it is deleted (article 32), which
     *       {@link SubjectRights} says;</li>
     *   <li>the AEPD's list under GDPR article 35.4 counts biometric data for identification and genetic data as
     *       criteria of their own, so they make an impact assessment likely, a warning.</li>
     * </ul>
     */
    SPAIN
}
