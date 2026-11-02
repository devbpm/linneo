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

import java.util.Objects;
import java.util.Set;

/**
 * Why data is processed: the purpose of a processing activity, such as {@code essential.service.authentication}.
 * <p>
 * A purpose belongs to a process or a system, not to a field, so there is no annotation for it; applications use
 * these terms where they describe their processing activities.
 *
 * @param info the key, parent, name, description, origin and lifecycle
 * @param traits the traits the term declares or inherits, which Linneo's legal rules read
 * @since 0.1.0
 */
public record DataUse(TermInfo info, Set<Trait> traits) implements Term {

    /**
     * Checks the components and makes the traits immutable.
     *
     * @param info the term information
     * @param traits the traits, or {@code null} for none
     * @throws IllegalArgumentException if a trait does not apply to the hierarchy
     */
    public DataUse {
        Objects.requireNonNull(info, "info");
        traits = Trait.checked(traits, TermChange.Hierarchy.DATA_USE, info.key());
    }
}
