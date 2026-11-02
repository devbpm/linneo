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
package io.github.devbpm.linneo.ens;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.ens.EnsCategorization.Dimension;
import io.github.devbpm.linneo.ens.EnsCategorization.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnsCategorizationTest {

    private final Taxonomy taxonomy = Taxonomy.standard();
    private final Classifier classifier = new Classifier(taxonomy);

    @SuppressWarnings("unused")
    static class Student {
        @Category(Categories.USER_NAME)
        private String name;
        @Category(Categories.USER_HEALTH_AND_MEDICAL_DISABILITY)
        private Integer disability;
    }

    @SuppressWarnings("unused")
    static class Course {
        @Category(Categories.REFERENCE_PUBLIC)
        private String title;
    }

    @Test
    @DisplayName("the information sets confidentiality and integrity, and the category is the highest level")
    void fromInformation() {
        EnsCategorization ens = EnsCategorization.builder()
                .information(classifier.classify(Student.class)).build();
        assertAll(
                () -> assertEquals(Level.HIGH, ens.valuation(Dimension.CONFIDENTIALITY).orElseThrow().level()),
                () -> assertTrue(ens.valuation(Dimension.CONFIDENTIALITY).orElseThrow().reason()
                        .contains(Categories.USER_HEALTH_AND_MEDICAL_DISABILITY)),
                () -> assertEquals(Level.HIGH, ens.valuation(Dimension.INTEGRITY).orElseThrow().level()),
                () -> assertEquals(EnsCategorization.SystemCategory.HIGH, ens.category()),
                () -> assertEquals("ALTA", ens.category().spanishName()),
                () -> assertFalse(ens.isComplete()),
                () -> assertEquals(EnumSet.of(Dimension.TRACEABILITY, Dimension.AUTHENTICITY, Dimension.AVAILABILITY),
                        ens.missing()),
                () -> assertTrue(ens.summary().endsWith("not declared: T, A, D (lower bound)"), ens::summary));
    }

    @Test
    @DisplayName("public data sets no confidentiality requirement, and the owner completes the other dimensions")
    void declared() {
        EnsCategorization ens = EnsCategorization.builder()
                .information(classifier.classify(Course.class))
                .declare(Dimension.AVAILABILITY, Level.MEDIUM, "enrollment closes on fixed dates")
                .declare(Dimension.AUTHENTICITY, Level.LOW, "public catalogue")
                .declare(Dimension.TRACEABILITY, Level.LOW, "no audit needed")
                .build();
        assertAll(
                () -> assertTrue(ens.valuation(Dimension.CONFIDENTIALITY).isEmpty()),
                () -> assertTrue(ens.isComplete()),
                () -> assertEquals(EnsCategorization.SystemCategory.MEDIUM, ens.category()),
                () -> assertEquals("declared: enrollment closes on fixed dates",
                        ens.valuation(Dimension.AVAILABILITY).orElseThrow().reason()));
    }

    @Test
    @DisplayName("a declaration raises the level the code sets but never lowers it, and stored fields count alike")
    void floorAndInventory() {
        EnsCategorization lowered = EnsCategorization.builder()
                .information(classifier.classify(Student.class))
                .declare(Dimension.CONFIDENTIALITY, Level.LOW, "the officer thinks it is not sensitive").build();
        EnsCategorization raised = EnsCategorization.builder()
                .information(classifier.classify(Course.class))
                .declare(Dimension.INTEGRITY, Level.HIGH, "official catalogue").build();
        DataInventory inventory = classifier.inventory(List.of(Student.class), type -> "students");
        EnsCategorization stored = EnsCategorization.builder()
                .information(inventory, "students", taxonomy).build();
        assertAll(
                () -> assertEquals(Level.HIGH, lowered.valuation(Dimension.CONFIDENTIALITY).orElseThrow().level()),
                () -> assertTrue(lowered.valuation(Dimension.CONFIDENTIALITY).orElseThrow().reason()
                        .startsWith("field ")),
                () -> assertEquals(Level.HIGH, raised.valuation(Dimension.INTEGRITY).orElseThrow().level()),
                () -> assertEquals(EnsCategorization.builder().information(classifier.classify(Student.class))
                        .build().category(), stored.category()),
                () -> assertEquals(Level.HIGH, stored.valuation(Dimension.CONFIDENTIALITY).orElseThrow().level()));
    }

    @SuppressWarnings("unused")
    static class Broken {
        @Category(Categories.USER_HEALTH_AND_MEDICAL)
        private String condition;
        private String unclassified;
    }

    @Test
    @DisplayName("a classification with errors or an empty inventory gives no information, and none given leaves the"
            + " category a lower bound")
    void failsClosed() {
        EnsCategorization empty = EnsCategorization.builder()
                .declare(Dimension.AUTHENTICITY, Level.LOW, "x").declare(Dimension.TRACEABILITY, Level.LOW, "x")
                .declare(Dimension.AVAILABILITY, Level.LOW, "x").build();
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> EnsCategorization.builder().information(classifier.classify(Broken.class))),
                () -> assertFalse(empty.isComplete()),
                () -> assertTrue(empty.summary().contains("no information given")),
                () -> assertEquals("Category BASIC (BÁSICA): no dimension valued; no information given (lower bound)",
                        EnsCategorization.builder().information(DataInventory.builder().build(), taxonomy).build()
                                .summary()),
                () -> assertThrows(IllegalArgumentException.class, () -> EnsCategorization.builder()
                        .information(DataInventory.builder().object("students").build(), "studnet", taxonomy)));
    }
}
