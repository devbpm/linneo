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

import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.Violation.Code;
import io.github.devbpm.linneo.classify.scan.Customer;
import io.github.devbpm.linneo.classify.scan.Invoice;
import io.github.devbpm.linneo.classify.scan.sub.Unclassified;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelClassesTest {

    @Test
    @DisplayName("a package gives its classes and records, nested ones included, sorted by name")
    void inPackage() {
        assertEquals(List.of(Customer.class, Customer.Address.class, Invoice.class),
                ModelClasses.inPackage(Customer.class));
    }

    @Test
    @DisplayName("a package tree adds the classes of the packages below")
    void inPackageTree() {
        assertEquals(List.of(Customer.class, Customer.Address.class, Invoice.class, Unclassified.class),
                ModelClasses.inPackageTree(Invoice.class));
    }

    @Test
    @DisplayName("a class nobody listed is verified too, so its unclassified field fails the build")
    void forgottenClass() {
        Classifier classifier = new Classifier(Taxonomy.standard());
        List<Class<?>> model = ModelClasses.inPackageTree(Customer.class);
        ClassificationException failure = assertThrows(ClassificationException.class, () -> classifier.verify(model));
        assertAll(
                () -> assertEquals(List.of(Code.UNCLASSIFIED_FIELD), failure.errors().stream()
                        .map(Violation::code).toList()),
                () -> assertEquals(Unclassified.class, failure.errors().getFirst().type()));
    }

    @Test
    @DisplayName("classes are read from a JAR file as well, and a class without a location is refused")
    void jarAndNoLocation() {
        List<Class<?>> fromJar = ModelClasses.inPackage(AssertionFailedError.class);
        assertAll(
                () -> assertTrue(fromJar.contains(AssertionFailedError.class), fromJar::toString),
                () -> assertTrue(fromJar.stream().allMatch(type -> type.getPackageName().equals("org.opentest4j")
                        && !type.isInterface() && !type.isEnum()), fromJar::toString),
                () -> assertThrows(IllegalArgumentException.class, () -> ModelClasses.inPackage(String.class)));
    }
}
