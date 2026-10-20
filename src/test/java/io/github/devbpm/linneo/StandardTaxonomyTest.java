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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardTaxonomyTest {

    private final Taxonomy taxonomy = Taxonomy.standard();

    @Test
    @DisplayName("bundles the whole fideslang 3.1.4 default taxonomy")
    void bundlesFideslang() {
        assertAll(
                () -> assertEquals(85, count(taxonomy.categories(), TermInfo.FIDESLANG)),
                () -> assertEquals(56, count(taxonomy.dataUses(), TermInfo.FIDESLANG)),
                () -> assertEquals(15, count(taxonomy.dataSubjects(), TermInfo.FIDESLANG)));
    }

    @Test
    @DisplayName("keeps fideslang deprecations and their replacements")
    void keepsDeprecations() {
        TermInfo profiling = taxonomy.requireDataUse("personalize.profiling").info();
        assertTrue(profiling.isDeprecated());
        assertEquals("2.1.1", profiling.deprecatedSince());
        assertEquals("personalize.content.profiling", profiling.replacedBy());
    }

    @Test
    @DisplayName("is built once and shared")
    void isShared() {
        assertSame(Taxonomy.standard(), Taxonomy.standard());
    }

    @Test
    @DisplayName("every key has a constant and every constant a key")
    void constantsMatchKeys() {
        assertAll(
                () -> assertEquals(keys(taxonomy.categories()), constants(Categories.class)),
                () -> assertEquals(keys(taxonomy.dataUses()), constants(DataUses.class)),
                () -> assertEquals(keys(taxonomy.dataSubjects()), constants(DataSubjects.class)));
    }

    @Test
    @DisplayName("no category is less protected than its parent")
    void protectionNeverWeakensDownTheTree() {
        for (DataCategory category : taxonomy.categories()) {
            taxonomy.parentOf(category).ifPresent(parent -> assertAll(category.key(),
                    () -> assertTrue(parent.nature().admits(category.nature())),
                    () -> assertTrue(category.confidentiality().compareTo(parent.confidentiality()) >= 0),
                    () -> assertTrue(category.integrity().compareTo(parent.integrity()) >= 0),
                    () -> assertTrue(!parent.secret() || category.secret())));
        }
    }

    @Test
    @DisplayName("applies Linneo's policy to the fideslang categories")
    void appliesPolicy() {
        DataCategory email = taxonomy.requireCategory(Categories.USER_CONTACT_EMAIL);
        DataCategory religion = taxonomy.requireCategory(Categories.USER_DEMOGRAPHIC_RELIGIOUS_BELIEF);
        DataCategory genetic = taxonomy.requireCategory(Categories.USER_HEALTH_AND_MEDICAL_GENETIC);
        DataCategory password = taxonomy.requireCategory(Categories.USER_AUTHORIZATION_PASSWORD);
        DataCategory criminal = taxonomy.requireCategory(Categories.USER_CRIMINAL_HISTORY);
        assertAll(
                () -> assertEquals(LegalNature.ORDINARY, email.nature()),
                () -> assertEquals(Confidentiality.LOW, email.confidentiality()),
                () -> assertEquals(LegalNature.SPECIAL_CATEGORY, religion.nature()),
                () -> assertEquals(LegalNature.SPECIAL_CATEGORY, genetic.nature()),
                () -> assertEquals(Confidentiality.HIGH, genetic.confidentiality()),
                () -> assertTrue(password.secret()),
                () -> assertEquals(LegalNature.CRIMINAL_OFFENCE, criminal.nature()));
    }

    @Test
    @DisplayName("covers every special category of GDPR article 9")
    void coversEverySpecialCategory() {
        for (String key : List.of(Categories.USER_DEMOGRAPHIC_RACE_ETHNICITY,
                Categories.USER_DEMOGRAPHIC_POLITICAL_OPINION, Categories.USER_DEMOGRAPHIC_RELIGIOUS_BELIEF,
                Categories.USER_DEMOGRAPHIC_PHILOSOPHICAL_BELIEF, Categories.USER_DEMOGRAPHIC_TRADE_UNION_MEMBERSHIP,
                Categories.USER_HEALTH_AND_MEDICAL_GENETIC, Categories.USER_BIOMETRIC,
                Categories.USER_HEALTH_AND_MEDICAL, Categories.USER_DEMOGRAPHIC_SEX_LIFE,
                Categories.USER_DEMOGRAPHIC_SEXUAL_ORIENTATION)) {
            DataCategory category = taxonomy.requireCategory(key);
            assertAll(key,
                    () -> assertEquals(LegalNature.SPECIAL_CATEGORY, category.nature()),
                    () -> assertEquals(Confidentiality.HIGH, category.confidentiality()));
        }
    }

    @Test
    @DisplayName("marks as secret what grants access, not the permissions themselves")
    void secretsAreWhatGrantsAccess() {
        assertAll(
                () -> assertFalse(taxonomy.requireCategory(Categories.USER_AUTHORIZATION).secret()),
                () -> assertTrue(taxonomy.requireCategory(Categories.USER_AUTHORIZATION_CREDENTIALS).secret()),
                () -> assertTrue(taxonomy.requireCategory(Categories.USER_AUTHORIZATION_PASSWORD).secret()),
                () -> assertTrue(taxonomy.requireCategory(Categories.USER_AUTHORIZATION_BIOMETRIC).secret()),
                () -> assertTrue(taxonomy.requireCategory(Categories.USER_DEVICE_SESSION_ID).secret()),
                () -> assertEquals(Confidentiality.HIGH,
                        taxonomy.requireCategory(Categories.USER_DEVICE_SESSION_ID).confidentiality()));
    }

    @Test
    @DisplayName("Linneo's extensions add education, non-personal and public data")
    void addsLinneoExtensions() {
        DataCategory disability = taxonomy.requireCategory(Categories.USER_HEALTH_AND_MEDICAL_DISABILITY);
        DataCategory grade = taxonomy.requireCategory(Categories.USER_EDUCATION_GRADE);
        DataCategory secret = taxonomy.requireCategory(Categories.SYSTEM_AUTHENTICATION_SECRET);
        DataCategory registry = taxonomy.requireCategory(Categories.REFERENCE_PUBLIC);
        DataCategory evidence = taxonomy.requireCategory(Categories.SYSTEM_INTEGRITY);
        assertAll(
                () -> assertEquals(TermInfo.LINNEO, disability.info().origin()),
                () -> assertEquals(LegalNature.SPECIAL_CATEGORY, disability.nature()),
                () -> assertEquals(Confidentiality.MEDIUM, grade.confidentiality()),
                () -> assertEquals(Integrity.HIGH, grade.integrity()),
                () -> assertTrue(secret.secret()),
                () -> assertFalse(secret.isPersonal()),
                () -> assertEquals(Confidentiality.PUBLIC, registry.confidentiality()),
                () -> assertEquals(Integrity.HIGH, evidence.integrity()));
    }

    @Test
    @DisplayName("navigates the hierarchy")
    void navigates() {
        DataCategory email = taxonomy.requireCategory(Categories.USER_CONTACT_EMAIL);
        assertEquals(List.of(Categories.USER_CONTACT, Categories.USER),
                taxonomy.ancestorsOf(email).stream().map(DataCategory::key).toList());
        assertTrue(email.isA(Categories.USER_CONTACT));
        assertFalse(email.isA("user.con"));
        assertTrue(taxonomy.childrenOf(taxonomy.requireCategory(Categories.USER_CONTACT)).contains(email));
    }

    private static long count(List<? extends Term> terms, String origin) {
        return terms.stream().filter(t -> t.info().origin().equals(origin)).count();
    }

    private static Set<String> keys(List<? extends Term> terms) {
        return terms.stream().map(Term::key).collect(Collectors.toSet());
    }

    private static Set<String> constants(Class<?> holder) {
        return Arrays.stream(holder.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                .map(StandardTaxonomyTest::value)
                .collect(Collectors.toSet());
    }

    private static String value(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
