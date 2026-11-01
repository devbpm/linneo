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
package io.github.devbpm.linneo.internal;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the messages of the exceptions that report many errors at once. Not part of the public API.
 */
public final class Messages {

    /**
     * How many errors a message lists one by one; the rest are only counted, by code.
     */
    public static final int LISTED = 50;

    private Messages() {
    }

    /**
     * A message that lists the errors, one per line, up to {@link #LISTED}, then counts all of them by code.
     *
     * @param kind what kind of errors they are, such as {@code "classification"}
     * @param errors the errors, at least one
     * @param code the code of an error
     * @param <T> the type of the errors
     * @return the message
     */
    public static <T> String errors(String kind, List<T> errors, Function<T, ? extends Enum<?>> code) {
        StringBuilder out = new StringBuilder().append(errors.size()).append(' ').append(kind).append(" error(s):");
        errors.stream().limit(LISTED).forEach(error -> out.append("\n - ").append(error));
        if (errors.size() > LISTED) {
            Map<String, Long> byCode = errors.stream().collect(Collectors.groupingBy(e -> code.apply(e).name(),
                    TreeMap::new, Collectors.counting()));
            out.append("\n ... and ").append(errors.size() - LISTED).append(" more; all of them by code: ")
                    .append(byCode.entrySet().stream().map(e -> e.getKey() + " " + e.getValue())
                            .collect(Collectors.joining(", ")));
        }
        return out.toString();
    }
}
