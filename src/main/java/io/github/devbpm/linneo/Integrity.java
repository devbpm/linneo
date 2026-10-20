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
 * How much harm an unauthorized or undetected change to the data would cause, from least to most.
 * <p>
 * Integrity is independent of confidentiality: an audit trail or a permission matrix may be harmless to disclose
 * and critical to keep unaltered. Constants are declared in increasing order, so {@link #compareTo(Enum)} orders
 * them by strictness.
 *
 * @since 0.1.0
 */
public enum Integrity {

    /**
     * An unwanted change would cause limited harm.
     */
    LOW,

    /**
     * An unwanted change would cause serious harm.
     */
    MEDIUM,

    /**
     * An unwanted change would cause severe harm.
     */
    HIGH;

    /**
     * The stricter of this level and another.
     *
     * @param other another level
     * @return the stricter of the two
     */
    public Integrity max(Integrity other) {
        return other.compareTo(this) > 0 ? other : this;
    }
}
