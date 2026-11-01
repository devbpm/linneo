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
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Protection;

import java.util.List;
import java.util.Objects;

/**
 * The classification of one field: its category and the effective protection of the field.
 *
 * @param declaringClass the class that declares the field
 * @param path identifies the field within the classified class: its name, or {@code field.part} for a field of a
 *        {@linkplain io.github.devbpm.linneo.annotation.Composite composite} it holds
 * @param category the current data category (a deprecated key is already resolved to its replacement)
 * @param basis where the category comes from
 * @param reason the reason of a class default or what decides a per-row category; {@code null} for a declared one
 * @param confidentiality the category's level, or the stricter one the field declares
 * @param integrity the category's level, or the stricter one the field declares
 * @param identifiability how the value points to the person; {@code null} when the category is not personal
 * @param derivedFrom for a {@linkplain Basis#DERIVED derived} field, its sources: field paths of the same class, or
 *        {@code ClassName#path} for a field of another class; empty otherwise
 * @since 0.1.0
 */
public record FieldClassification(Class<?> declaringClass, String path, DataCategory category, Basis basis,
                                  String reason, Confidentiality confidentiality, Integrity integrity,
                                  Identifiability identifiability, List<String> derivedFrom) {

    /**
     * Where the category of a field comes from.
     */
    public enum Basis {

        /**
         * A {@link io.github.devbpm.linneo.annotation.Category @Category} on the field.
         */
        DECLARED,

        /**
         * The {@link io.github.devbpm.linneo.annotation.DefaultCategory @DefaultCategory} of a class.
         */
        CLASS_DEFAULT,

        /**
         * The ceiling of a {@link io.github.devbpm.linneo.annotation.CategoryByRow @CategoryByRow}.
         */
        BY_ROW,

        /**
         * A {@link io.github.devbpm.linneo.annotation.DerivedFrom @DerivedFrom} field: the classification of its
         * source, or the category it declares, which protects at least as much as its sources.
         */
        DERIVED
    }

    /**
     * Checks the mandatory components.
     *
     * @param declaringClass the declaring class
     * @param path the field path
     * @param category the data category
     * @param basis the basis
     * @param reason the reason or {@code null}
     * @param confidentiality the effective confidentiality
     * @param integrity the effective integrity
     * @param identifiability the identifiability or {@code null}
     * @param derivedFrom the sources of a derived field; {@code null} means none
     */
    public FieldClassification {
        Objects.requireNonNull(declaringClass, "declaringClass");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(basis, "basis");
        Objects.requireNonNull(confidentiality, "confidentiality");
        Objects.requireNonNull(integrity, "integrity");
        derivedFrom = derivedFrom == null ? List.of() : List.copyOf(derivedFrom);
    }

    /**
     * The name of the field: the last segment of its {@linkplain #path() path}.
     *
     * @return the field name as declared in {@link #declaringClass()}
     */
    public String fieldName() {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    /**
     * Whether the stored values are personal data: a personal category stored identified or pseudonymized.
     * Anonymized or aggregated values of a personal category are not.
     *
     * @return {@code true} if the field holds personal data
     */
    public boolean isPersonal() {
        return category.isPersonal()
                && (identifiability == Identifiability.IDENTIFIED || identifiability == Identifiability.PSEUDONYMIZED);
    }

    /**
     * Whether the field holds a secret that must never be read back, exported or published.
     *
     * @return the category's {@link DataCategory#secret() secret} flag
     */
    public boolean isSecret() {
        return category.secret();
    }

    /**
     * The protection the stored values require: the category's nature when they are {@linkplain #isPersonal()
     * personal data} and {@link LegalNature#NONE} when they are anonymized or aggregated, with the field's effective
     * levels and the category's secret flag.
     *
     * @return the protection of the field
     */
    public Protection protection() {
        return Protection.of(isPersonal() ? category.nature() : LegalNature.NONE, confidentiality, integrity,
                isSecret());
    }

    @Override
    public String toString() {
        return path + " = " + category.key() + " (" + basis + ")";
    }
}
