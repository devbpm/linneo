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
package io.github.devbpm.linneo.classify;

import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown by {@link Classifier#verify(java.util.Collection)} when a model is not fully and correctly classified.
 * The message lists every error, one per line.
 *
 * @since 0.1.0
 */
public class ClassificationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<Violation> errors;

    /**
     * Creates the exception.
     *
     * @param errors the errors found, at least one
     */
    public ClassificationException(List<Violation> errors) {
        super(errors.size() + " classification error(s):" + errors.stream().map(v -> "\n - " + v)
                .collect(Collectors.joining()));
        this.errors = List.copyOf(errors);
    }

    /**
     * The errors found.
     *
     * @return the violations, all of severity {@link Violation.Severity#ERROR}
     */
    public List<Violation> errors() {
        return errors;
    }
}
