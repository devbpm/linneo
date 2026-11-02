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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The declared properties of a data category while a {@link TaxonomyBuilder} is open.
 * <p>
 * Every declared facet is a minimum: declaring a level twice, as two extensions refining the same category do
 * ({@link TaxonomyBuilder#refineCategory}), keeps the stricter one, whatever their order, and a facet left undeclared
 * is inherited from the parent when the taxonomy is built.
 *
 * @since 0.1.0
 */
public final class CategoryDraft extends TermDraft {

    private LegalNature nature;
    private Confidentiality confidentiality;
    private Integrity integrity;
    private boolean secret;
    private final Map<String, String> attributes = new LinkedHashMap<>();

    CategoryDraft(TaxonomyBuilder builder, String key, String name, String description, String origin) {
        super(builder, TermChange.Hierarchy.DATA_CATEGORY, key, name, description, origin);
    }

    /**
     * Declares the legal nature, at least. A nature the declared one already covers, such as ordinary personal data
     * for a special category, changes nothing.
     *
     * @param nature the nature; it must be admitted by the parent's ({@link LegalNature#admits})
     * @return this draft
     * @throws TaxonomyException if a nature is already declared and the two cannot be reconciled: one is personal
     *         data and the other is not, or they are special category and criminal offence data
     */
    public CategoryDraft nature(LegalNature nature) {
        checkOpen();
        Objects.requireNonNull(nature, "nature");
        if (this.nature == null || this.nature.admits(nature)) {
            this.nature = nature;
        } else if (this.nature.isPersonal() != nature.isPersonal() || !this.nature.covers(nature)) {
            throw new TaxonomyException("Cannot change the nature of '" + key() + "' from " + this.nature + " to "
                    + nature);
        }
        return this;
    }

    /**
     * Declares the confidentiality level, at least: the stricter of this and an already declared level is kept.
     *
     * @param confidentiality the level
     * @return this draft
     */
    public CategoryDraft confidentiality(Confidentiality confidentiality) {
        checkOpen();
        Objects.requireNonNull(confidentiality, "confidentiality");
        this.confidentiality = this.confidentiality == null ? confidentiality
                : this.confidentiality.max(confidentiality);
        return this;
    }

    /**
     * Declares the integrity level, at least: the stricter of this and an already declared level is kept.
     *
     * @param integrity the level
     * @return this draft
     */
    public CategoryDraft integrity(Integrity integrity) {
        checkOpen();
        Objects.requireNonNull(integrity, "integrity");
        this.integrity = this.integrity == null ? integrity : this.integrity.max(integrity);
        return this;
    }

    /**
     * Marks the category, and therefore all its children, as a secret.
     *
     * @return this draft
     */
    public CategoryDraft secret() {
        checkOpen();
        this.secret = true;
        return this;
    }

    /**
     * Sets an application-defined attribute, inherited by the children that do not set their own value.
     *
     * @param name the attribute name
     * @param value the attribute value
     * @return this draft
     */
    public CategoryDraft attribute(String name, String value) {
        checkOpen();
        if (Objects.requireNonNull(name, "name").isBlank()) {
            throw new TaxonomyException("An attribute of '" + key() + "' has a blank name");
        }
        attributes.put(name, Objects.requireNonNull(value, "value"));
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @throws TaxonomyException if an extension refines a category it did not add
     */
    @Override
    public CategoryDraft since(String version) {
        super.since(version);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @throws TaxonomyException if a trait does not apply to data categories
     */
    @Override
    public CategoryDraft traits(Trait... traits) {
        super.traits(traits);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @throws TaxonomyException if the version is blank, or an extension refines a category it did not add
     */
    @Override
    public CategoryDraft deprecated(String sinceVersion, String replacedBy) {
        super.deprecated(sinceVersion, replacedBy);
        return this;
    }

    LegalNature declaredNature() {
        return nature;
    }

    Confidentiality declaredConfidentiality() {
        return confidentiality;
    }

    Integrity declaredIntegrity() {
        return integrity;
    }

    boolean declaredSecret() {
        return secret;
    }

    Map<String, String> declaredAttributes() {
        return attributes;
    }

}
