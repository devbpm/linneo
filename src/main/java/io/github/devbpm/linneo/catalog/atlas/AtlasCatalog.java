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
package io.github.devbpm.linneo.catalog.atlas;

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TermInfo;
import io.github.devbpm.linneo.classify.FieldClassification;
import io.github.devbpm.linneo.internal.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The taxonomy translated to Apache Atlas: the classification types to create, and the classifications of each
 * column.
 * <p>
 * Every category is a classification whose super type is its parent's, under the root {@code DataCategory}, so
 * {@code user.contact.email} is {@code DataCategory.user.contact.email}, a sub-classification of
 * {@code DataCategory.user.contact}. A search for a classification also finds the entities of its
 * sub-classifications. The protection levels are separate classifications ({@code PersonalData.SpecialCategory},
 * {@code Confidentiality.High}, {@code DataHandling.Secret}…) that a column carries next to its category, so that
 * tag-based policies, such as those of Apache Ranger, can act on them directly. They are not super types of the
 * categories, because a child category may raise a level its parent implies.
 * <p>
 * Instances are immutable and thread-safe.
 *
 * @since 0.1.0
 */
public final class AtlasCatalog {

    private static final String SECRET = "Secret";

    private final Taxonomy taxonomy;
    private final AtlasMapping mapping;

    private AtlasCatalog(Taxonomy taxonomy, AtlasMapping mapping) {
        this.taxonomy = Objects.requireNonNull(taxonomy, "taxonomy");
        this.mapping = Objects.requireNonNull(mapping, "mapping");
    }

    /**
     * The catalog of a taxonomy with the {@linkplain AtlasMapping#defaults() default names}.
     *
     * @param taxonomy the taxonomy
     * @return the catalog
     */
    public static AtlasCatalog of(Taxonomy taxonomy) {
        return of(taxonomy, AtlasMapping.defaults());
    }

    /**
     * The catalog of a taxonomy with the given names.
     *
     * @param taxonomy the taxonomy
     * @param mapping the root classification names
     * @return the catalog
     */
    public static AtlasCatalog of(Taxonomy taxonomy, AtlasMapping mapping) {
        return new AtlasCatalog(taxonomy, mapping);
    }

    /**
     * The classification types to create: the roots, the protection levels, then the categories with every parent
     * before its children.
     *
     * @return the classification definitions, in creation order
     */
    public List<AtlasClassificationDef> classificationDefs() {
        List<AtlasClassificationDef> defs = new ArrayList<>();
        defs.add(root(mapping.personalData(), "Legal nature of personal data under the GDPR."));
        defs.add(root(mapping.confidentiality(), "Harm that disclosing the data would cause."));
        defs.add(root(mapping.integrity(), "Harm that an unwanted change to the data would cause."));
        defs.add(root(mapping.identifiability(), "How directly a personal data value points to the person."));
        defs.add(root(mapping.handling(), "Handling rules that apply to the data."));
        defs.add(root(mapping.categories(), "What a column holds, from the Linneo data category taxonomy "
                + "(fideslang default taxonomy plus extensions)."));
        for (LegalNature nature : LegalNature.values()) {
            if (nature.isPersonal()) {
                defs.add(level(mapping.personalData(), natureLabel(nature), natureDescription(nature)));
            }
        }
        for (Confidentiality level : Confidentiality.values()) {
            defs.add(level(mapping.confidentiality(), label(level), "Confidentiality level " + label(level) + "."));
        }
        for (Integrity level : Integrity.values()) {
            defs.add(level(mapping.integrity(), label(level), "Integrity level " + label(level) + "."));
        }
        for (Identifiability level : Identifiability.values()) {
            defs.add(level(mapping.identifiability(), label(level), "Personal data that is "
                    + label(level).toLowerCase(Locale.ROOT) + "."));
        }
        defs.add(level(mapping.handling(), SECRET, "A secret that grants access: it must never be read back, "
                + "exported or published, only verified."));
        for (DataCategory category : taxonomy.categories()) {
            defs.add(categoryDef(category));
        }
        return defs;
    }

    /**
     * The body of {@code POST /api/atlas/v2/types/typedefs} that creates every classification type at once.
     *
     * @return a JSON object with the member {@code classificationDefs}
     */
    public String typeDefsJson() {
        return new Json().raw("classificationDefs", Json.array(classificationDefs().stream()
                .map(AtlasClassificationDef::toJson).toList())).end();
    }

    /**
     * The name of a category's classification.
     *
     * @param category a category of the taxonomy
     * @return a name such as {@code DataCategory.user.contact.email}
     */
    public String classificationOf(DataCategory category) {
        return mapping.categories() + "." + category.key();
    }

    /**
     * The classifications of a category: its own followed by those of the protection it implies.
     *
     * @param category a category of the taxonomy
     * @return classification names
     */
    public List<String> classificationsOf(DataCategory category) {
        List<String> names = new ArrayList<>();
        names.add(classificationOf(category));
        if (category.isPersonal()) {
            names.add(mapping.personalData() + "." + natureLabel(category.nature()));
        }
        names.add(mapping.confidentiality() + "." + label(category.confidentiality()));
        names.add(mapping.integrity() + "." + label(category.integrity()));
        if (category.secret()) {
            names.add(mapping.handling() + "." + SECRET);
        }
        return names;
    }

    /**
     * The classifications of a classified column: its category, its legal nature when it is personal data, and
     * the effective protection of the field.
     *
     * @param field the classification of the field mapped to the column
     * @return classification names
     */
    public List<String> classificationsOf(FieldClassification field) {
        DataCategory category = field.category();
        List<String> names = new ArrayList<>();
        names.add(classificationOf(category));
        if (field.isPersonal()) {
            names.add(mapping.personalData() + "." + natureLabel(category.nature()));
        }
        names.add(mapping.confidentiality() + "." + label(field.confidentiality()));
        names.add(mapping.integrity() + "." + label(field.integrity()));
        if (field.identifiability() != null) {
            names.add(mapping.identifiability() + "." + label(field.identifiability()));
        }
        if (field.isSecret()) {
            names.add(mapping.handling() + "." + SECRET);
        }
        return names;
    }

    /**
     * The body of {@code POST /api/atlas/v2/entity/guid/{guid}/classifications} that adds the classifications of
     * a column to its entity. Atlas applies its default propagation.
     *
     * @param field the classification of the field mapped to the column
     * @return a JSON array of classifications
     */
    public String classificationsJson(FieldClassification field) {
        return Json.array(classificationsOf(field).stream()
                .map(name -> new Json().field("typeName", name).end()).toList());
    }

    private AtlasClassificationDef categoryDef(DataCategory category) {
        TermInfo info = category.info();
        String description = category.name() + ": " + info.description();
        if (info.isDeprecated()) {
            description += " Deprecated" + (info.replacedBy() == null ? "." : ": use " + info.replacedBy() + ".");
        }
        String superType = info.parent().map(p -> mapping.categories() + "." + p).orElse(mapping.categories());
        return new AtlasClassificationDef(classificationOf(category), description, List.of(superType));
    }

    private static AtlasClassificationDef root(String name, String description) {
        return new AtlasClassificationDef(name, description, List.of());
    }

    private static AtlasClassificationDef level(String root, String name, String description) {
        return new AtlasClassificationDef(root + "." + name, description, List.of(root));
    }

    private static String natureLabel(LegalNature nature) {
        return switch (nature) {
            case ORDINARY -> "Personal";
            case SPECIAL_CATEGORY -> "SpecialCategory";
            case CRIMINAL_OFFENCE -> "CriminalOffence";
            case NONE -> throw new IllegalArgumentException("Not personal data");
        };
    }

    private static String natureDescription(LegalNature nature) {
        return switch (nature) {
            case ORDINARY -> "Personal data (GDPR article 4.1).";
            case SPECIAL_CATEGORY -> "Special category of personal data (GDPR article 9).";
            case CRIMINAL_OFFENCE -> "Personal data relating to criminal convictions and offences (GDPR article 10).";
            case NONE -> throw new IllegalArgumentException("Not personal data");
        };
    }

    private static String label(Enum<?> level) {
        String name = level.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
