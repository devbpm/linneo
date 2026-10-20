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

import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown by {@link ProcessingChecker#verify(java.util.Collection)} when a processing activity is incomplete or
 * would be unlawful as declared. The message lists every error, one per line.
 *
 * @since 0.1.0
 */
public class ProcessingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<ProcessingViolation> errors;

    /**
     * Creates the exception.
     *
     * @param errors the errors found, at least one
     */
    public ProcessingException(List<ProcessingViolation> errors) {
        super(errors.size() + " processing error(s):" + errors.stream().map(v -> "\n - " + v)
                .collect(Collectors.joining()));
        this.errors = List.copyOf(errors);
    }

    /**
     * The errors found.
     *
     * @return the violations, all of severity {@link ProcessingViolation.Severity#ERROR}
     */
    public List<ProcessingViolation> errors() {
        return errors;
    }
}
