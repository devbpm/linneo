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

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.DataSubject;
import io.github.devbpm.linneo.DataUse;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A processing activity checked against the taxonomy: its terms resolved, the personal data it processes and the
 * protection that data requires, plus the problems found.
 *
 * @param activity the activity as declared
 * @param purposes the data uses, deprecated keys replaced
 * @param dataSubjects the data subjects, deprecated keys replaced
 * @param dataCategories the personal data categories processed, from the declared keys and the personal fields of
 *        the entities, in key order
 * @param confidentiality the highest confidentiality of that data, field-level raises included; {@code PUBLIC}
 *        when there is none
 * @param integrity the highest integrity of that data, field-level raises included; {@code LOW} when there is none
 * @param secret whether any of that data is a secret
 * @param violations the problems found
 * @since 0.1.0
 */
public record CheckedActivity(ProcessingActivity activity, List<DataUse> purposes, List<DataSubject> dataSubjects,
                              List<DataCategory> dataCategories, Confidentiality confidentiality, Integrity integrity,
                              boolean secret, List<ProcessingViolation> violations) {

    /**
     * Checks the components and makes the lists immutable.
     *
     * @param activity the activity
     * @param purposes the data uses
     * @param dataSubjects the data subjects
     * @param dataCategories the personal data categories
     * @param confidentiality the highest confidentiality
     * @param integrity the highest integrity
     * @param secret whether any data is a secret
     * @param violations the problems found
     */
    public CheckedActivity {
        Objects.requireNonNull(activity, "activity");
        purposes = List.copyOf(purposes);
        dataSubjects = List.copyOf(dataSubjects);
        dataCategories = List.copyOf(dataCategories);
        Objects.requireNonNull(confidentiality, "confidentiality");
        Objects.requireNonNull(integrity, "integrity");
        violations = List.copyOf(violations);
    }

    /**
     * Whether the activity can go in the record.
     *
     * @return {@code true} if there is no {@linkplain ProcessingViolation#isError() error}; warnings are allowed
     */
    public boolean isValid() {
        return violations.stream().noneMatch(ProcessingViolation::isError);
    }

    /**
     * The errors found.
     *
     * @return the violations of severity {@link ProcessingViolation.Severity#ERROR}
     */
    public List<ProcessingViolation> errors() {
        return violations.stream().filter(ProcessingViolation::isError).toList();
    }

    /**
     * The warnings found: the open points of the record.
     *
     * @return the violations of severity {@link ProcessingViolation.Severity#WARNING}
     */
    public List<ProcessingViolation> warnings() {
        return violations.stream().filter(v -> !v.isError()).toList();
    }

    /**
     * The legal natures of the personal data processed.
     *
     * @return the natures, without {@link LegalNature#NONE}
     */
    public Set<LegalNature> natures() {
        Set<LegalNature> natures = EnumSet.noneOf(LegalNature.class);
        dataCategories.forEach(category -> natures.add(category.nature()));
        return natures;
    }

    /**
     * Whether the activity processes special categories of personal data (GDPR article 9).
     *
     * @return {@code true} if a category is {@link LegalNature#SPECIAL_CATEGORY}
     */
    public boolean processesSpecialCategories() {
        return natures().contains(LegalNature.SPECIAL_CATEGORY);
    }

    /**
     * Whether the activity processes personal data relating to criminal convictions and offences (GDPR article 10).
     *
     * @return {@code true} if a category is {@link LegalNature#CRIMINAL_OFFENCE}
     */
    public boolean processesCriminalOffenceData() {
        return natures().contains(LegalNature.CRIMINAL_OFFENCE);
    }
}
