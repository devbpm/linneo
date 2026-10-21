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
     *       religion, sexual orientation, beliefs or racial or ethnic origin (article 9.1), an error;</li>
     *   <li>processing under GDPR article 9.2 g), h) and i) must rest on a law with the rank of statute (article
     *       9.2), which the warning about a missing legal reference says.</li>
     * </ul>
     */
    SPAIN
}
