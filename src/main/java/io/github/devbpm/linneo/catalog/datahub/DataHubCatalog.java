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
package io.github.devbpm.linneo.catalog.datahub;

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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * The taxonomy translated to a DataHub business glossary: the glossary nodes and terms to create, and the terms of
 * each field.
 * <p>
 * Every category is a glossary term, such as {@code urn:li:glossaryTerm:DataCategory.user.contact.email}. A
 * category with children also gets a glossary node of the same name that holds its own term and its children, so
 * the glossary browses like the taxonomy tree. Each term <em>is a</em> ({@code isRelatedTerms}) term of its parent
 * category and <em>is related to</em> ({@code relatedTerms}) the terms of the protection it implies; its custom
 * properties record the key, origin and protection facets ({@code linneo.*}) followed by the category's attributes
 * in key order, which cannot override them. The protection levels are terms of their own nodes:
 * {@code PersonalData}, {@code Confidentiality}, {@code Integrity}, {@code Identifiability} and
 * {@code DataHandling}.
 * <p>
 * Instances are immutable and thread-safe.
 *
 * @since 0.1.0
 */
public final class DataHubCatalog {

    private static final String NODE = "glossaryNode";
    private static final String TERM = "glossaryTerm";
    private static final String SECRET = "Secret";
    private static final String FIDESLANG_URL = "https://github.com/ethyca/fideslang";

    private final Taxonomy taxonomy;
    private final DataHubMapping mapping;
    private final Set<String> parents = new HashSet<>();

    private DataHubCatalog(Taxonomy taxonomy, DataHubMapping mapping) {
        this.taxonomy = Objects.requireNonNull(taxonomy, "taxonomy");
        this.mapping = Objects.requireNonNull(mapping, "mapping");
        for (DataCategory category : taxonomy.categories()) {
            category.info().parent().ifPresent(parents::add);
        }
    }

    /**
     * The catalog of a taxonomy with the {@linkplain DataHubMapping#defaults() default names}.
     *
     * @param taxonomy the taxonomy
     * @return the catalog
     */
    public static DataHubCatalog of(Taxonomy taxonomy) {
        return of(taxonomy, DataHubMapping.defaults());
    }

    /**
     * The catalog of a taxonomy with the given names.
     *
     * @param taxonomy the taxonomy
     * @param mapping the glossary node names
     * @return the catalog
     */
    public static DataHubCatalog of(Taxonomy taxonomy, DataHubMapping mapping) {
        return new DataHubCatalog(taxonomy, mapping);
    }

    /**
     * The proposals that create the glossary: every node (parents first), then every term with its
     * {@code glossaryTermInfo}, then the {@code glossaryRelatedTerms} of the category terms. Send them in this
     * order.
     *
     * @return the proposals, in creation order
     */
    public List<DhProposal> proposals() {
        List<DhProposal> proposals = new ArrayList<>();
        proposals.add(node(mapping.personalData(), "Personal Data", "Legal nature of personal data under the GDPR.",
                null));
        proposals.add(node(mapping.confidentiality(), "Confidentiality", "Harm that disclosing the data would cause.",
                null));
        proposals.add(node(mapping.integrity(), "Integrity", "Harm that an unwanted change to the data would cause.",
                null));
        proposals.add(node(mapping.identifiability(), "Identifiability", "How directly a personal data value points "
                + "to the person.", null));
        proposals.add(node(mapping.handling(), "Data Handling", "Handling rules that apply to the data.", null));
        proposals.add(node(mapping.categories(), "Data Category", "What a field holds, from the Linneo data category "
                + "taxonomy (fideslang default taxonomy plus extensions).", null));
        for (DataCategory category : taxonomy.categories()) {
            if (parents.contains(category.key())) {
                proposals.add(node(id(category.key()), category.name(), category.info().description(),
                        nodeOf(category.info().parentKey())));
            }
        }

        for (LegalNature nature : LegalNature.values()) {
            if (nature.isPersonal()) {
                proposals.add(levelTerm(mapping.personalData(), natureLabel(nature), natureDescription(nature)));
            }
        }
        for (Confidentiality level : Confidentiality.values()) {
            proposals.add(levelTerm(mapping.confidentiality(), label(level), "Confidentiality level " + label(level)
                    + "."));
        }
        for (Integrity level : Integrity.values()) {
            proposals.add(levelTerm(mapping.integrity(), label(level), "Integrity level " + label(level) + "."));
        }
        for (Identifiability level : Identifiability.values()) {
            proposals.add(levelTerm(mapping.identifiability(), label(level), "Personal data that is "
                    + label(level).toLowerCase(Locale.ROOT) + "."));
        }
        proposals.add(levelTerm(mapping.handling(), SECRET, "A secret that grants access: it must never be read "
                + "back, exported or published, only verified."));
        for (DataCategory category : taxonomy.categories()) {
            proposals.add(categoryTerm(category));
        }

        for (DataCategory category : taxonomy.categories()) {
            Json related = new Json();
            category.info().parent().ifPresent(parent -> related.field("isRelatedTerms", List.of(termUrn(id(parent)))));
            related.field("relatedTerms", impliedBy(category));
            proposals.add(new DhProposal(TERM, termOf(category), "glossaryRelatedTerms", related.end()));
        }
        return proposals;
    }

    /**
     * The URN of a category's term.
     *
     * @param category a category of the taxonomy
     * @return a URN such as {@code urn:li:glossaryTerm:DataCategory.user.contact.email}
     */
    public String termOf(DataCategory category) {
        return termUrn(id(category.key()));
    }

    /**
     * The terms of a category: its own term followed by the terms of the protection it implies.
     *
     * @param category a category of the taxonomy
     * @return glossary term URNs
     */
    public List<String> termsOf(DataCategory category) {
        List<String> terms = new ArrayList<>();
        terms.add(termOf(category));
        terms.addAll(impliedBy(category));
        return terms;
    }

    /**
     * The terms of a classified field: its category, its legal nature when it is personal data, and the effective
     * protection of the field. Add them to the field of the dataset, for instance with the GraphQL mutation
     * {@code addTerms} and the field path as sub-resource.
     *
     * @param field the classification of the field
     * @return glossary term URNs
     */
    public List<String> termsOf(FieldClassification field) {
        DataCategory category = field.category();
        List<String> terms = new ArrayList<>();
        terms.add(termOf(category));
        if (field.isPersonal()) {
            terms.add(levelUrn(mapping.personalData(), natureLabel(category.nature())));
        }
        terms.add(levelUrn(mapping.confidentiality(), label(field.confidentiality())));
        terms.add(levelUrn(mapping.integrity(), label(field.integrity())));
        if (field.identifiability() != null) {
            terms.add(levelUrn(mapping.identifiability(), label(field.identifiability())));
        }
        if (field.isSecret()) {
            terms.add(levelUrn(mapping.handling(), SECRET));
        }
        return terms;
    }

    private DhProposal node(String id, String name, String definition, String parentNode) {
        Json info = new Json().field("customProperties", Map.of()).field("name", name)
                .field("definition", definition);
        if (parentNode != null) {
            info.field("parentNode", parentNode);
        }
        return new DhProposal(NODE, nodeUrn(id), "glossaryNodeInfo", info.end());
    }

    private DhProposal levelTerm(String node, String name, String definition) {
        String info = new Json().field("customProperties", Map.of()).field("name", name)
                .field("definition", definition).field("parentNode", nodeUrn(node)).field("termSource", "INTERNAL")
                .end();
        return new DhProposal(TERM, levelUrn(node, name), "glossaryTermInfo", info);
    }

    private DhProposal categoryTerm(DataCategory category) {
        TermInfo info = category.info();
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("linneo.key", category.key());
        properties.put("linneo.origin", info.origin());
        properties.put("linneo.nature", category.nature().name());
        properties.put("linneo.confidentiality", category.confidentiality().name());
        properties.put("linneo.integrity", category.integrity().name());
        properties.put("linneo.secret", Boolean.toString(category.secret()));
        new TreeMap<>(category.attributes()).forEach(properties::putIfAbsent);
        String definition = info.description();
        if (info.isDeprecated()) {
            definition += " Deprecated" + (info.replacedBy() == null ? "." : ": use " + info.replacedBy() + ".");
        }
        String parentNode = parents.contains(category.key()) ? nodeUrn(id(category.key()))
                : nodeOf(info.parentKey());
        Json json = new Json().field("customProperties", properties).field("name", category.name())
                .field("definition", definition).field("parentNode", parentNode);
        if (TermInfo.FIDESLANG.equals(info.origin())) {
            json.field("termSource", "EXTERNAL").field("sourceRef", "fideslang").field("sourceUrl", FIDESLANG_URL);
        } else {
            json.field("termSource", "INTERNAL");
        }
        return new DhProposal(TERM, termOf(category), "glossaryTermInfo", json.end());
    }

    private List<String> impliedBy(DataCategory category) {
        List<String> terms = new ArrayList<>();
        if (category.isPersonal()) {
            terms.add(levelUrn(mapping.personalData(), natureLabel(category.nature())));
        }
        terms.add(levelUrn(mapping.confidentiality(), label(category.confidentiality())));
        terms.add(levelUrn(mapping.integrity(), label(category.integrity())));
        if (category.secret()) {
            terms.add(levelUrn(mapping.handling(), SECRET));
        }
        return terms;
    }

    private String nodeOf(String parentKey) {
        return nodeUrn(parentKey == null ? mapping.categories() : id(parentKey));
    }

    private String id(String key) {
        return mapping.categories() + "." + key;
    }

    private static String levelUrn(String node, String name) {
        return termUrn(node + "." + name);
    }

    private static String nodeUrn(String id) {
        return "urn:li:" + NODE + ":" + id;
    }

    private static String termUrn(String id) {
        return "urn:li:" + TERM + ":" + id;
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
