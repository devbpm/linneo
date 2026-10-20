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

import java.util.Objects;

/**
 * A transfer of personal data to a third country or an international organisation (GDPR article 30.1.e).
 *
 * @param destination the third country or international organisation
 * @param safeguard what allows the transfer
 * @since 0.1.0
 */
public record Transfer(String destination, TransferSafeguard safeguard) {

    /**
     * Checks the components.
     *
     * @param destination the destination, not blank
     * @param safeguard the safeguard
     */
    public Transfer {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(safeguard, "safeguard");
        if (destination.isBlank()) {
            throw new IllegalArgumentException("A transfer needs a destination");
        }
    }
}
