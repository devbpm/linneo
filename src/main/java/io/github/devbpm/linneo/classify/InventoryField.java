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
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyException;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;

import java.util.Objects;

/**
 * One field of a {@link DataInventory}: what the code declares for it, independent of any taxonomy, so that it can
 * be stored, compared and resolved later against the taxonomy of the day.
 * <p>
 * A classified field has a {@code key} and a {@code basis}; a field excluded with
 * {@link io.github.devbpm.linneo.annotation.Exempt @Exempt} has neither, only its reason ({@link #isExempt()}).
 *
 * @param object the key of the object the field belongs to, chosen by the application
 * @param path the field path within the object
 * @param key the data category key as declared, possibly deprecated; {@code null} for an exempted field
 * @param basis where the category comes from; {@code null} for an exempted field
 * @param confidentiality the confidentiality the field declares above its category's, or {@code null} when it takes
 *        the category's
 * @param integrity the integrity the field declares above its category's, or {@code null} when it takes the
 *        category's
 * @param identifiability how the value points to the person; {@code null} when the category is not personal, and
 *        read as {@link Identifiability#IDENTIFIED} when absent for a personal category
 * @param reason the reason of an exemption or a class default, or what decides a per-row category; {@code null}
 *        otherwise
 * @since 0.1.0
 */
public record InventoryField(String object, String path, String key, Basis basis, Confidentiality confidentiality,
                             Integrity integrity, Identifiability identifiability, String reason) {

    /**
     * Checks the components.
     *
     * @param object the object key
     * @param path the field path
     * @param key the category key or {@code null}
     * @param basis the basis or {@code null}
     * @param confidentiality the raised confidentiality or {@code null}
     * @param integrity the raised integrity or {@code null}
     * @param identifiability the identifiability or {@code null}
     * @param reason the reason or {@code null}
     * @throws IllegalArgumentException if the object or path is blank, only one of key and basis is given, or an
     *         exempted field has levels, an identifiability or no reason
     */
    public InventoryField {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(path, "path");
        if (object.isBlank() || path.isBlank()) {
            throw new IllegalArgumentException("An inventory field needs an object and a path");
        }
        if ((key == null) != (basis == null)) {
            throw new IllegalArgumentException(object + "." + path + ": a classified field needs both a key and a"
                    + " basis, an exempted field neither");
        }
        if (key == null && (confidentiality != null || integrity != null || identifiability != null
                || reason == null || reason.isBlank())) {
            throw new IllegalArgumentException(object + "." + path + ": an exempted field has only a reason");
        }
    }

    /**
     * A field excluded from classification.
     *
     * @param object the object key
     * @param path the field path
     * @param reason why the field holds no data
     * @return the field
     */
    public static InventoryField exempt(String object, String path, String reason) {
        return new InventoryField(object, path, null, null, null, null, null, reason);
    }

    /**
     * Whether the field is excluded from classification.
     *
     * @return {@code true} if it has no key
     */
    public boolean isExempt() {
        return key == null;
    }

    /**
     * The current data category of the field in a taxonomy: its key, or the replacement of a deprecated key.
     *
     * @param taxonomy the taxonomy
     * @return the category
     * @throws TaxonomyException if the field is exempted or the taxonomy does not contain its key
     */
    public DataCategory category(Taxonomy taxonomy) {
        if (key == null) {
            throw new TaxonomyException(object + "." + path + " is exempted and has no data category");
        }
        return taxonomy.resolveCategory(key);
    }

    /**
     * Whether the stored values are personal data, as {@link FieldClassification#isPersonal()} tells.
     *
     * @param taxonomy the taxonomy to resolve the key with
     * @return {@code false} for an exempted field
     * @throws TaxonomyException if the taxonomy does not contain the key
     */
    public boolean isPersonal(Taxonomy taxonomy) {
        return !isExempt() && category(taxonomy).isPersonal() && identifiability != Identifiability.ANONYMIZED
                && identifiability != Identifiability.AGGREGATED;
    }

    /**
     * How the stored values point to the person in a taxonomy, as {@link FieldClassification#identifiability()}
     * gives it: the declared identifiability, or {@link Identifiability#IDENTIFIED} when the field declares none.
     *
     * @param taxonomy the taxonomy to resolve the key with
     * @return the identifiability; {@code null} for an exempted field or a category that is not personal
     * @throws TaxonomyException if the taxonomy does not contain the key
     */
    public Identifiability identifiability(Taxonomy taxonomy) {
        if (isExempt() || !category(taxonomy).isPersonal()) {
            return null;
        }
        return identifiability == null ? Identifiability.IDENTIFIED : identifiability;
    }

    /**
     * The protection the field requires in a taxonomy, as {@link FieldClassification#protection()} computes it.
     *
     * @param taxonomy the taxonomy to resolve the key with
     * @return the protection; {@link Protection#LOWEST} for an exempted field
     * @throws TaxonomyException if the taxonomy does not contain the key
     */
    public Protection protection(Taxonomy taxonomy) {
        if (isExempt()) {
            return Protection.LOWEST;
        }
        DataCategory category = category(taxonomy);
        return Protection.of(isPersonal(taxonomy) ? category.nature() : LegalNature.NONE,
                confidentiality == null ? category.confidentiality() : category.confidentiality().max(confidentiality),
                integrity == null ? category.integrity() : category.integrity().max(integrity), category.secret());
    }

    @Override
    public String toString() {
        return object + "." + path + (isExempt() ? " exempt: " + reason : " = " + key + " (" + basis + ")");
    }
}
