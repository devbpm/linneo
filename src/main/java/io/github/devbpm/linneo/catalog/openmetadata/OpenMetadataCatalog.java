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
package io.github.devbpm.linneo.catalog.openmetadata;

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.FieldClassification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The taxonomy translated to OpenMetadata: the classifications and tags to create, and the tags of each column.
 * <p>
 * The category hierarchy becomes nested tags of one mutually exclusive classification, so
 * {@code user.contact.email} is the tag {@code DataCategory.user.contact.email}. OpenMetadata does not keep links
 * between tags, so the protection a category implies (the built-in {@code PersonalData} and {@code PII} tags plus
 * the confidentiality, integrity and secret tags) goes on each column, together with its category tag:
 * {@link #tagsOf(FieldClassification)}. Special categories and criminal offence data are both tagged
 * {@code PersonalData.SpecialCategory}, the only OpenMetadata tag for data with reinforced protection.
 * <p>
 * Checked against a running OpenMetadata 2.0.6 server; the request bodies use only fields present since 0.13.2.
 * <p>
 * Instances are immutable and thread-safe.
 *
 * @since 0.1.0
 */
public final class OpenMetadataCatalog {

    private static final String PERSONAL = "Personal";
    private static final String SPECIAL_CATEGORY = "SpecialCategory";
    private static final String PII_NONE = "None";
    private static final String PII_NON_SENSITIVE = "NonSensitive";
    private static final String PII_SENSITIVE = "Sensitive";
    private static final String SECRET = "Secret";

    private final Taxonomy taxonomy;
    private final OpenMetadataMapping mapping;

    private OpenMetadataCatalog(Taxonomy taxonomy, OpenMetadataMapping mapping) {
        this.taxonomy = Objects.requireNonNull(taxonomy, "taxonomy");
        this.mapping = Objects.requireNonNull(mapping, "mapping");
    }

    /**
     * The catalog of a taxonomy with the {@linkplain OpenMetadataMapping#defaults() default names}.
     *
     * @param taxonomy the taxonomy
     * @return the catalog
     */
    public static OpenMetadataCatalog of(Taxonomy taxonomy) {
        return of(taxonomy, OpenMetadataMapping.defaults());
    }

    /**
     * The catalog of a taxonomy with the given names.
     *
     * @param taxonomy the taxonomy
     * @param mapping the classification names and the sensitivity threshold
     * @return the catalog
     */
    public static OpenMetadataCatalog of(Taxonomy taxonomy, OpenMetadataMapping mapping) {
        return new OpenMetadataCatalog(taxonomy, mapping);
    }

    /**
     * The classifications to create. The built-in {@code PersonalData} and {@code PII} classifications are not
     * included.
     *
     * @return the classifications, in creation order
     */
    public List<OmClassification> classifications() {
        return List.of(
                new OmClassification(mapping.categories(), "What a column holds, from the Linneo data category "
                        + "taxonomy (fideslang default taxonomy plus extensions).", true),
                new OmClassification(mapping.confidentiality(), "Harm that disclosing the data would cause.", true),
                new OmClassification(mapping.integrity(), "Harm that an unwanted change to the data would cause.",
                        true),
                new OmClassification(mapping.identifiability(), "How directly a personal data value points to the "
                        + "person.", true),
                new OmClassification(mapping.handling(), "Handling rules that apply to the data.", false));
    }

    /**
     * The tags to create: the level tags first, then the category tags with every parent before its children.
     *
     * @return the tags, in creation order
     */
    public List<OmTag> tags() {
        List<OmTag> tags = new ArrayList<>();
        for (Confidentiality level : Confidentiality.values()) {
            tags.add(levelTag(mapping.confidentiality(), level, "Confidentiality level " + label(level) + "."));
        }
        for (Integrity level : Integrity.values()) {
            tags.add(levelTag(mapping.integrity(), level, "Integrity level " + label(level) + "."));
        }
        for (Identifiability level : Identifiability.values()) {
            tags.add(levelTag(mapping.identifiability(), level, "Personal data that is " + label(level)
                    .toLowerCase(Locale.ROOT) + "."));
        }
        tags.add(new OmTag(mapping.handling(), null, SECRET, SECRET, "A secret that grants access: it must never "
                + "be read back, exported or published, only verified."));
        for (DataCategory category : taxonomy.categories()) {
            tags.add(categoryTag(category));
        }
        return tags;
    }

    /**
     * The fully qualified name of a category's tag.
     *
     * @param category a category of the taxonomy
     * @return a name such as {@code DataCategory.user.contact.email}
     */
    public String tagOf(DataCategory category) {
        return mapping.categories() + "." + category.key();
    }

    /**
     * The tags of a category: its own tag followed by the tags it implies.
     *
     * @param category a category of the taxonomy
     * @return fully qualified tag names
     */
    public List<String> tagsOf(DataCategory category) {
        List<String> tags = new ArrayList<>();
        tags.add(tagOf(category));
        tags.addAll(impliedBy(category));
        return tags;
    }

    /**
     * The tags of a classified column: its category, whether and how it is personal data, and the effective
     * protection of the field.
     *
     * @param field the classification of the field mapped to the column
     * @return fully qualified tag names
     */
    public List<String> tagsOf(FieldClassification field) {
        DataCategory category = field.category();
        List<String> tags = new ArrayList<>();
        tags.add(tagOf(category));
        if (field.isPersonal()) {
            tags.add(personalDataTag(category.nature()));
            tags.add(piiTag(true, field.confidentiality()));
        } else {
            tags.add(piiTag(false, field.confidentiality()));
        }
        tags.add(mapping.confidentiality() + "." + label(field.confidentiality()));
        tags.add(mapping.integrity() + "." + label(field.integrity()));
        if (field.identifiability() != null) {
            tags.add(mapping.identifiability() + "." + label(field.identifiability()));
        }
        if (field.isSecret()) {
            tags.add(mapping.handling() + "." + SECRET);
        }
        return tags;
    }

    private OmTag categoryTag(DataCategory category) {
        String parent = category.info().parent().map(p -> mapping.categories() + "." + p).orElse(null);
        String segment = category.key().substring(category.key().lastIndexOf('.') + 1);
        String description = category.info().description();
        if (category.info().isDeprecated()) {
            description += " Deprecated" + (category.info().replacedBy() == null ? "."
                    : ": use " + category.info().replacedBy() + ".");
        }
        return new OmTag(mapping.categories(), parent, segment, category.name(), description);
    }

    private List<String> impliedBy(DataCategory category) {
        List<String> tags = new ArrayList<>();
        if (category.isPersonal()) {
            tags.add(personalDataTag(category.nature()));
        }
        tags.add(piiTag(category.isPersonal(), category.confidentiality()));
        tags.add(mapping.confidentiality() + "." + label(category.confidentiality()));
        tags.add(mapping.integrity() + "." + label(category.integrity()));
        if (category.secret()) {
            tags.add(mapping.handling() + "." + SECRET);
        }
        return tags;
    }

    private String personalDataTag(LegalNature nature) {
        return mapping.personalData() + "." + (nature == LegalNature.ORDINARY ? PERSONAL : SPECIAL_CATEGORY);
    }

    private String piiTag(boolean personal, Confidentiality confidentiality) {
        String tag;
        if (!personal) {
            tag = PII_NONE;
        } else if (confidentiality.compareTo(mapping.sensitiveFrom()) >= 0) {
            tag = PII_SENSITIVE;
        } else {
            tag = PII_NON_SENSITIVE;
        }
        return mapping.pii() + "." + tag;
    }

    private static OmTag levelTag(String classification, Enum<?> level, String description) {
        return new OmTag(classification, null, label(level), label(level), description);
    }

    private static String label(Enum<?> level) {
        String name = level.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
