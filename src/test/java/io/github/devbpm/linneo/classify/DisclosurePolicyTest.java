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

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.DisclosurePolicy.Treatment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisclosurePolicyTest {

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final Classifier classifier = new Classifier(taxonomy);

    @Test
    @DisplayName("in logs, personal data is masked and special category data, criminal data and secrets left out")
    void logs() {
        Map<String, Treatment> treatments = DisclosurePolicy.forLogs()
                .treatments(classifier.classify(InventoryPublishingTest.Person.class));
        assertAll(
                () -> assertEquals(List.of("email", "analyticsId", "sickDays", "disability", "certificateClear",
                        "passwordHash", "content", "emailDomain", "home.street", "home.city", "version", "card"),
                        List.copyOf(treatments.keySet())),
                () -> assertEquals(Treatment.MASK, treatments.get("email")),
                () -> assertEquals(Treatment.MASK, treatments.get("analyticsId")),
                () -> assertEquals(Treatment.MASK, treatments.get("emailDomain")),
                () -> assertEquals(Treatment.MASK, treatments.get("home.city")),
                () -> assertEquals(Treatment.OMIT, treatments.get("disability")),
                () -> assertEquals(Treatment.OMIT, treatments.get("certificateClear")),
                () -> assertEquals(Treatment.OMIT, treatments.get("passwordHash")),
                () -> assertEquals(Treatment.OMIT, treatments.get("content")),
                () -> assertEquals(Treatment.CLEAR, treatments.get("version")),
                () -> assertEquals(Treatment.CLEAR, treatments.get("card")));
    }

    @Test
    @DisplayName("whatever the thresholds, more protection never gets a laxer treatment, and secrets are left out")
    void monotone() {
        List<Protection> protections = protections();
        List<DisclosurePolicy> policies = List.of(DisclosurePolicy.forLogs(), DisclosurePolicy.builder().build(),
                DisclosurePolicy.builder()
                        .mask(Protection.of(LegalNature.NONE, Confidentiality.MEDIUM, Integrity.LOW, false))
                        .omit(Protection.of(LegalNature.ORDINARY, Confidentiality.LOW, Integrity.HIGH, false))
                        .build());
        for (DisclosurePolicy policy : policies) {
            for (Protection stricter : protections) {
                for (Protection laxer : protections) {
                    if (stricter.covers(laxer)) {
                        assertTrue(policy.treatment(stricter).compareTo(policy.treatment(laxer)) >= 0,
                                () -> policy + ": " + stricter + " gets less than " + laxer);
                    }
                }
                if (stricter.secret()) {
                    assertEquals(Treatment.OMIT, policy.treatment(stricter));
                }
            }
        }
    }

    @Test
    @DisplayName("a stored field gets the treatment of its classification")
    void inventory() {
        ClassClassification person = classifier.classify(InventoryPublishingTest.Person.class);
        DataInventory inventory = classifier.inventory(List.of(InventoryPublishingTest.Person.class), type -> "p");
        DisclosurePolicy policy = DisclosurePolicy.forLogs();
        for (FieldClassification field : person.fields()) {
            assertEquals(policy.treatment(field), policy.treatment(inventory.field("p", field.path()).orElseThrow(),
                    taxonomy), field.path());
        }
        assertEquals(Treatment.CLEAR, policy.treatment(InventoryField.exempt("p", "card", "cache"), taxonomy));
    }

    @Test
    @DisplayName("a class with errors has no complete treatments, so none are given")
    void errors() {
        @SuppressWarnings("unused")
        class Broken {
            private String unclassified;
        }
        assertThrows(IllegalArgumentException.class,
                () -> DisclosurePolicy.forLogs().treatments(classifier.classify(Broken.class)));
    }

    private static List<Protection> protections() {
        List<Set<LegalNature>> natures = List.of(EnumSet.of(LegalNature.NONE), EnumSet.of(LegalNature.ORDINARY),
                EnumSet.of(LegalNature.SPECIAL_CATEGORY), EnumSet.of(LegalNature.CRIMINAL_OFFENCE),
                EnumSet.of(LegalNature.SPECIAL_CATEGORY, LegalNature.CRIMINAL_OFFENCE));
        List<Protection> protections = new ArrayList<>();
        for (Set<LegalNature> nature : natures) {
            for (Confidentiality confidentiality : Confidentiality.values()) {
                for (Integrity integrity : Integrity.values()) {
                    protections.add(new Protection(nature, confidentiality, integrity, false));
                    protections.add(new Protection(nature, confidentiality, integrity, true));
                }
            }
        }
        return protections;
    }
}
