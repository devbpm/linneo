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
 * Every facet left undeclared is inherited from the parent when the taxonomy is built. When refining an existing
 * category ({@link TaxonomyBuilder#refineCategory}), a facet can only be made stricter than what it already
 * declares.
 *
 * @since 0.1.0
 */
public final class CategoryDraft extends TermDraft {

    private boolean refining;
    private LegalNature nature;
    private Confidentiality confidentiality;
    private Integrity integrity;
    private boolean secret;
    private final Map<String, String> attributes = new LinkedHashMap<>();

    CategoryDraft(String key, String name, String description, String origin) {
        super(key, name, description, origin);
    }

    /**
     * Declares the legal nature.
     *
     * @param nature the nature; it must be admitted by the parent's ({@link LegalNature#admits})
     * @return this draft
     * @throws TaxonomyException if refining would change an already declared nature into one it does not admit
     */
    public CategoryDraft nature(LegalNature nature) {
        Objects.requireNonNull(nature, "nature");
        if (refining && this.nature != null && !this.nature.admits(nature)) {
            throw new TaxonomyException("Cannot change the nature of '" + key() + "' from " + this.nature + " to " + nature);
        }
        this.nature = nature;
        return this;
    }

    /**
     * Declares the confidentiality level.
     *
     * @param confidentiality the level; never lower than the parent's
     * @return this draft
     * @throws TaxonomyException if refining would lower an already declared level
     */
    public CategoryDraft confidentiality(Confidentiality confidentiality) {
        Objects.requireNonNull(confidentiality, "confidentiality");
        if (refining && this.confidentiality != null && confidentiality.compareTo(this.confidentiality) < 0) {
            throw new TaxonomyException("Cannot lower the confidentiality of '" + key() + "' from "
                    + this.confidentiality + " to " + confidentiality);
        }
        this.confidentiality = confidentiality;
        return this;
    }

    /**
     * Declares the integrity level.
     *
     * @param integrity the level; never lower than the parent's
     * @return this draft
     * @throws TaxonomyException if refining would lower an already declared level
     */
    public CategoryDraft integrity(Integrity integrity) {
        Objects.requireNonNull(integrity, "integrity");
        if (refining && this.integrity != null && integrity.compareTo(this.integrity) < 0) {
            throw new TaxonomyException("Cannot lower the integrity of '" + key() + "' from " + this.integrity + " to "
                    + integrity);
        }
        this.integrity = integrity;
        return this;
    }

    /**
     * Marks the category, and therefore all its children, as a secret.
     *
     * @return this draft
     */
    public CategoryDraft secret() {
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
        attributes.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
        return this;
    }

    @Override
    public CategoryDraft since(String version) {
        super.since(version);
        return this;
    }

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

    void refining(boolean refining) {
        this.refining = refining;
    }
}
