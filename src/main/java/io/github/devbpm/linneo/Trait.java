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

import io.github.devbpm.linneo.TermChange.Hierarchy;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A property of a term that Linneo's legal rules read, so that the rules follow the taxonomy rather than lists of keys:
 * an application's or an extension's own term carries the same weight as a standard one once it declares its traits.
 * <p>
 * Traits are declared on terms ({@link TermDraft#traits(Trait...)}) and inherited down the tree: a child has every
 * trait of its parent. Linneo's own terms declare theirs in a resource of the standard taxonomy, and each shipped
 * extension declares the traits of its terms. Each trait applies to some hierarchies only, and declaring it on another
 * is an error. Adding a trait to a term makes the rules stricter, a minor change of a taxonomy; removing one is a
 * major change ({@link TermChange.Aspect#TRAITS}).
 * <ul>
 *   <li>{@link io.github.devbpm.linneo.processing.ImpactScreening} reads {@link #FINANCIAL}, {@link #BEHAVIOURAL}
 *       and {@link #PRIVATE_LIFE} as data of a highly personal nature, and {@link #EVALUATION},
 *       {@link #MONITORING} and {@link #VULNERABLE} as the criteria of the same names (WP248 rev.01, III.B.a).</li>
 *   <li>{@link io.github.devbpm.linneo.processing.BreachAssessment} reads {@link #FINANCIAL} and
 *       {@link #BEHAVIOURAL} as the data processing context and the identifier traits as the ease of identification
 *       (ENISA, 2013).</li>
 *   <li>{@link io.github.devbpm.linneo.processing.SubjectRights} reads {@link #DIRECT_MARKETING} for the absolute
 *       right to object (GDPR article 21.2 and 21.3).</li>
 * </ul>
 *
 * @since 0.1.0
 */
public enum Trait {

    /**
     * Financial data: bank and card details, income, pay, debts, transactions and financial standing.
     */
    FINANCIAL(Hierarchy.DATA_CATEGORY),

    /**
     * Data about behaviour: habits, preferences, activity, location and traffic data.
     */
    BEHAVIOURAL(Hierarchy.DATA_CATEGORY),

    /**
     * Data about private life: the contents of communications, private documents and household activities.
     */
    PRIVATE_LIFE(Hierarchy.DATA_CATEGORY),

    /**
     * Data that identifies a person on its own, such as a name, an identity document, a face or a biometric template.
     */
    DIRECT_IDENTIFIER(Hierarchy.DATA_CATEGORY),

    /**
     * Data through which a person can be reached and so identified, such as contact details or an account.
     */
    CONTACT_IDENTIFIER(Hierarchy.DATA_CATEGORY),

    /**
     * Data that singles out a person within a system, such as an internal identifier or a device identifier.
     */
    INDIRECT_IDENTIFIER(Hierarchy.DATA_CATEGORY),

    /**
     * For a data category, data obtained by observing people; for a data use, a purpose that observes, tracks or
     * monitors them systematically.
     */
    MONITORING(Hierarchy.DATA_CATEGORY, Hierarchy.DATA_USE),

    /**
     * For a data subject, people in a weaker position than the controller, such as children, employees and patients;
     * for a data category, data that is about such people by its nature.
     */
    VULNERABLE(Hierarchy.DATA_CATEGORY, Hierarchy.DATA_SUBJECT),

    /**
     * A purpose that evaluates or scores people, profiles them or predicts their behaviour, performance or situation.
     */
    EVALUATION(Hierarchy.DATA_USE),

    /**
     * A purpose of direct marketing, fundraising appeals and the profiling related to them included, to which a data
     * subject may object at any time (GDPR article 21.2).
     */
    DIRECT_MARKETING(Hierarchy.DATA_USE);

    private final Set<Hierarchy> hierarchies;

    Trait(Hierarchy first, Hierarchy... rest) {
        this.hierarchies = EnumSet.of(first, rest);
    }

    /**
     * Whether the trait can be declared on the terms of a hierarchy.
     *
     * @param hierarchy a hierarchy
     * @return {@code true} if it applies to the hierarchy's terms
     */
    public boolean appliesTo(Hierarchy hierarchy) {
        return hierarchies.contains(hierarchy);
    }

    /**
     * Whether the trait tells how a value identifies a person.
     *
     * @return {@code true} for {@link #DIRECT_IDENTIFIER}, {@link #CONTACT_IDENTIFIER} and
     *         {@link #INDIRECT_IDENTIFIER}
     */
    public boolean isIdentifier() {
        return this == DIRECT_IDENTIFIER || this == CONTACT_IDENTIFIER || this == INDIRECT_IDENTIFIER;
    }

    /**
     * Whether the trait marks data of a highly personal nature in the sense of the impact assessment guidelines.
     *
     * @return {@code true} for {@link #FINANCIAL}, {@link #BEHAVIOURAL} and {@link #PRIVATE_LIFE}
     */
    public boolean isHighlyPersonal() {
        return this == FINANCIAL || this == BEHAVIOURAL || this == PRIVATE_LIFE;
    }

    // The traits of a term of a hierarchy, checked and immutable.
    static Set<Trait> checked(Set<Trait> traits, Hierarchy hierarchy, String key) {
        if (traits == null || traits.isEmpty()) {
            return Set.of();
        }
        for (Trait trait : traits) {
            if (!Objects.requireNonNull(trait, "trait").appliesTo(hierarchy)) {
                throw new IllegalArgumentException("The trait " + trait + " does not apply to the " + hierarchy
                        + " '" + key + "'");
            }
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(traits));
    }
}
