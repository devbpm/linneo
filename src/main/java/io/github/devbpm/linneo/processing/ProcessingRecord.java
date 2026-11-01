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
package io.github.devbpm.linneo.processing;

import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Term;
import io.github.devbpm.linneo.internal.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The record of processing activities of a controller (GDPR article 30.1), built by
 * {@link ProcessingChecker#verify(java.util.Collection)} from activities without errors.
 * <p>
 * It renders as Markdown for people, with one section per activity and its open points (the warnings), and as
 * JSON for tools. The texts it adds are in English; the names come from the taxonomy and the activities.
 *
 * @param activities the checked activities, in declaration order
 * @since 0.1.0
 */
public record ProcessingRecord(List<CheckedActivity> activities) {

    /**
     * Checks that every activity is valid and makes the list immutable.
     *
     * @param activities the checked activities, none with an error
     * @throws IllegalArgumentException if an activity has an error; use
     *         {@link ProcessingChecker#verify(java.util.Collection)}, which reports them all
     */
    public ProcessingRecord {
        activities = List.copyOf(activities);
        for (CheckedActivity activity : activities) {
            if (!activity.isValid()) {
                throw new IllegalArgumentException("The processing activity '" + activity.activity().key()
                        + "' has errors and cannot go in the record: " + activity.errors());
            }
        }
    }

    /**
     * The activity with a key.
     *
     * @param key the activity key
     * @return the checked activity, or empty if the record has none with that key
     */
    public Optional<CheckedActivity> activity(String key) {
        return activities.stream().filter(a -> a.activity().key().equals(key)).findFirst();
    }

    /**
     * The record as a Markdown document.
     *
     * @param title the document title, such as the controller's name
     * @return Markdown text
     */
    public String toMarkdown(String title) {
        Objects.requireNonNull(title, "title");
        StringBuilder out = new StringBuilder("# ").append(cell(title)).append("\n");
        for (CheckedActivity checked : activities) {
            ProcessingActivity activity = checked.activity();
            Governance governance = activity.governance();
            out.append("\n## ").append(cell(activity.name())).append("\n\n`").append(activity.key()).append('`');
            if (activity.description() != null) {
                out.append(": ").append(cell(activity.description()));
            }
            out.append("\n\n| Record item | Content |\n|---|---|\n");
            row(out, "Purposes (art. 30.1.b)", terms(checked.purposes()));
            row(out, "Lawful basis (art. 6.1)", governance.legalBasis() == null ? null
                    : "Art. " + governance.legalBasis().article() + ": " + governance.legalBasis().fideslangName());
            row(out, "Legitimate interest", governance.legalBasis() == LegalBasis.LEGITIMATE_INTERESTS
                    ? governance.legitimateInterest() : null);
            row(out, "Special categories (art. 9.2)", governance.specialCategoryCondition() == null ? null
                    : "Art. " + governance.specialCategoryCondition().article() + ": "
                    + governance.specialCategoryCondition().fideslangName());
            row(out, "Criminal offence data (art. 10)", governance.criminalDataBasis() == null ? null
                    : "Art. 10: " + label(governance.criminalDataBasis()));
            row(out, "Legal reference", governance.legalReference());
            row(out, "Data subjects (art. 30.1.c)", terms(checked.dataSubjects()));
            row(out, "Personal data (art. 30.1.c)", categories(checked.dataCategories()));
            row(out, "Recipients (art. 30.1.d)", governance.recipients().isEmpty() ? "None declared"
                    : String.join("; ", governance.recipients()));
            row(out, "Transfers (art. 30.1.e)", governance.transfers().isEmpty() ? "None declared"
                    : String.join("; ", governance.transfers().stream().map(t -> t.destination() + " (art. "
                    + t.safeguard().article() + ": " + t.safeguard().fideslangName() + ")").toList()));
            row(out, "Retention (art. 30.1.f)", governance.retention() == null ? "Not declared"
                    : governance.retention());
            Protection protection = checked.protection();
            row(out, "Required protection (art. 30.1.g)", "Confidentiality " + label(protection.confidentiality())
                    + ", integrity " + label(protection.integrity())
                    + (protection.secret() ? ", includes secrets" : ""));
            row(out, "Impact assessment (art. 35)", governance.impactAssessment() == null ? "None"
                    : governance.impactAssessment());
            if (!checked.warnings().isEmpty()) {
                out.append("\nOpen points:\n\n");
                checked.warnings().forEach(w -> out.append("- ").append(cell(w.message())).append('\n'));
            }
        }
        return out.toString();
    }

    /**
     * The record as JSON: an object with the member {@code activities}, one object per activity with its keys,
     * bases (with their article and fideslang name), required protection and open points.
     *
     * @return a JSON object
     */
    public String toJson() {
        List<String> items = new ArrayList<>();
        for (CheckedActivity checked : activities) {
            ProcessingActivity activity = checked.activity();
            Governance governance = activity.governance();
            Json json = new Json().field("key", activity.key()).field("name", activity.name());
            if (activity.description() != null) {
                json.field("description", activity.description());
            }
            json.field("purposes", keys(checked.purposes())).field("dataSubjects", keys(checked.dataSubjects()))
                    .field("dataCategories", keys(checked.dataCategories()));
            if (governance.legalBasis() != null) {
                json.raw("legalBasis", basis(governance.legalBasis().name(), governance.legalBasis().article(),
                        governance.legalBasis().fideslangName()));
            }
            if (governance.legalBasis() == LegalBasis.LEGITIMATE_INTERESTS && governance.legitimateInterest() != null) {
                json.field("legitimateInterest", governance.legitimateInterest());
            }
            if (governance.specialCategoryCondition() != null) {
                SpecialCategoryCondition condition = governance.specialCategoryCondition();
                json.raw("specialCategoryCondition", basis(condition.name(), condition.article(),
                        condition.fideslangName()));
            }
            if (governance.criminalDataBasis() != null) {
                json.raw("criminalDataBasis", basis(governance.criminalDataBasis().name(), "10", null));
            }
            if (governance.legalReference() != null) {
                json.field("legalReference", governance.legalReference());
            }
            json.field("recipients", governance.recipients());
            json.raw("transfers", Json.array(governance.transfers().stream()
                    .map(t -> new Json().field("destination", t.destination())
                            .raw("safeguard", basis(t.safeguard().name(), t.safeguard().article(),
                                    t.safeguard().fideslangName())).end()).toList()));
            if (governance.retention() != null) {
                json.field("retention", governance.retention());
            }
            if (governance.impactAssessment() != null) {
                json.field("impactAssessment", governance.impactAssessment());
            }
            json.raw("protection", new Json()
                    .field("natures", checked.natures().stream().map(LegalNature::name).toList())
                    .field("confidentiality", checked.protection().confidentiality().name())
                    .field("integrity", checked.protection().integrity().name())
                    .field("secret", checked.protection().secret()).end());
            json.field("openPoints", checked.warnings().stream().map(ProcessingViolation::message).toList());
            items.add(json.end());
        }
        return new Json().raw("activities", Json.array(items)).end();
    }

    private static String basis(String code, String article, String fideslangName) {
        Json json = new Json().field("code", code).field("article", article);
        if (fideslangName != null) {
            json.field("fideslang", fideslangName);
        }
        return json.end();
    }

    private static List<String> keys(List<? extends Term> terms) {
        return terms.stream().map(Term::key).toList();
    }

    private static String terms(List<? extends Term> terms) {
        return terms.isEmpty() ? "None" : String.join("; ", terms.stream()
                .map(t -> t.name() + " (`" + t.key() + "`)").toList());
    }

    private static String categories(List<DataCategory> categories) {
        return categories.isEmpty() ? "None" : String.join("; ", categories.stream().map(c -> c.name() + " (`"
                + c.key() + "`" + switch (c.nature()) {
                    case SPECIAL_CATEGORY -> ", special category";
                    case CRIMINAL_OFFENCE -> ", criminal offence data";
                    default -> "";
                } + ")").toList());
    }

    private static void row(StringBuilder out, String item, String content) {
        if (content != null) {
            out.append("| ").append(item).append(" | ").append(cell(content)).append(" |\n");
        }
    }

    // A table cell is one line: line breaks and the blanks around them become a single space. Done line by line,
    // without a regular expression, so the time stays linear for any text.
    private static String cell(String text) {
        String escaped = text.replace("|", "\\|");
        if (escaped.indexOf('\n') < 0 && escaped.indexOf('\r') < 0) {
            return escaped;
        }
        return String.join(" ", escaped.lines().map(String::strip).filter(line -> !line.isEmpty()).toList());
    }

    private static String label(Enum<?> value) {
        String name = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
