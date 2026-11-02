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
import io.github.devbpm.linneo.internal.Tsv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The record of processing activities of a controller (GDPR article 30.1), built by
 * {@link ProcessingChecker#verify(java.util.Collection)} from activities without errors.
 * <p>
 * It renders as Markdown for people, with one section per activity and its open points (the warnings), and as
 * JSON for tools. The texts it adds are in English; the names come from the taxonomy and the activities. Its
 * {@linkplain #fingerprint() fingerprint} tells whether it is still the record someone approved, and
 * {@link RecordDiff} what changed since.
 *
 * @param activities the checked activities, in declaration order, each key once
 * @since 0.1.0
 */
public record ProcessingRecord(List<CheckedActivity> activities) {

    private static final String FORMAT = "linneo-record";
    private static final int VERSION = 1;

    /**
     * Checks that every activity is valid and has its own key, and makes the list immutable.
     *
     * @param activities the checked activities, none with an error
     * @throws IllegalArgumentException if an activity has an error, or two have the same key; use
     *         {@link ProcessingChecker#verify(java.util.Collection)}, which reports them all
     */
    public ProcessingRecord {
        activities = List.copyOf(activities);
        Set<String> keys = new HashSet<>();
        for (CheckedActivity activity : activities) {
            if (!activity.isValid()) {
                throw new IllegalArgumentException("The processing activity '" + activity.activity().key()
                        + "' has errors and cannot go in the record: " + activity.errors());
            }
            if (!keys.add(activity.activity().key())) {
                throw new IllegalArgumentException("Two processing activities of the record have the key '"
                        + activity.activity().key() + "'");
            }
        }
    }

    /**
     * The fingerprint of the record: the SHA-256, in hexadecimal, of its content, activity by activity in key order
     * and aspect by aspect as {@link RecordDiff} compares them. Two records have the same fingerprint exactly when
     * {@code RecordDiff} finds no change between them, whatever the order of their activities. An application keeps
     * it with the record the data protection officer approved, to know whether the record still is that one.
     *
     * @return 64 hexadecimal digits
     */
    public String fingerprint() {
        Tsv tsv = new Tsv(FORMAT, VERSION);
        activities.stream().sorted(Comparator.comparing(checked -> checked.activity().key())).forEach(checked ->
                ActivityChange.content(checked).forEach((aspect, content) -> {
                    List<String> row = new ArrayList<>(List.of(checked.activity().key(), aspect.name()));
                    row.addAll(content);
                    tsv.row(row.toArray());
                }));
        return Tsv.fingerprint(tsv.text());
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
            row(out, "Special categories (art. 9.2)", governance.specialCategoryConditions().isEmpty() ? null
                    : String.join("; ", governance.specialCategoryConditions().stream()
                    .map(condition -> "Art. " + condition.article() + ": " + condition.fideslangName()).toList()));
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
            row(out, "Impact assessment screening", checked.screening().summary());
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
            if (!governance.specialCategoryConditions().isEmpty()) {
                json.raw("specialCategoryConditions", Json.array(governance.specialCategoryConditions().stream()
                        .map(condition -> basis(condition.name(), condition.article(), condition.fideslangName()))
                        .toList()));
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
            ImpactScreening screening = checked.screening();
            Json screened = new Json().field("outcome", screening.outcome().name())
                    .field("criteria", screening.criteria().stream().map(Enum::name).toList());
            if (screening.mandatoryCase() != null) {
                screened.field("mandatoryCase", screening.mandatoryCase());
            }
            json.raw("screening", screened.end());
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
    // without a regular expression, so the time stays linear for any text. Text from the activities is shown as
    // text: a backslash, a cell separator, brackets and HTML characters are escaped, so a description can neither
    // break the table nor become a link or HTML where the Markdown is rendered.
    private static String cell(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\', '|', '[', ']' -> out.append('\\').append(c);
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                default -> out.append(c);
            }
        }
        String escaped = out.toString();
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
