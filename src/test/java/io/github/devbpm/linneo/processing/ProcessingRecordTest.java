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

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.DataSubjects;
import io.github.devbpm.linneo.DataUses;
import io.github.devbpm.linneo.Taxonomy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingRecordTest {

    private final ProcessingChecker checker = new ProcessingChecker(Taxonomy.standard());

    private final ProcessingActivity admissions = ProcessingActivity.builder("admissions", "Admissions | intake")
            .description("Receives and assesses\napplications.")
            .purposes(DataUses.EDUCATION_ADMISSION)
            .dataSubjects(DataSubjects.APPLICANT)
            .dataCategories(Categories.USER_NAME, Categories.USER_HEALTH_AND_MEDICAL_DISABILITY)
            .legalBasis(LegalBasis.PUBLIC_INTEREST)
            .specialCategoryCondition(SpecialCategoryCondition.EXPLICIT_CONSENT)
            .recipients("Admissions board")
            .transfer("United States", TransferSafeguard.STANDARD_CONTRACTUAL_CLAUSES)
            .build();

    @Test
    @DisplayName("the Markdown record has one table per activity, its open points, and escapes table syntax")
    void markdown() {
        String markdown = checker.verify(admissions).toMarkdown("Example University");
        assertAll(
                () -> assertTrue(markdown.startsWith("# Example University\n\n## Admissions \\| intake\n\n"
                        + "`admissions`: Receives and assesses applications.\n\n| Record item | Content |\n|---|---|\n")),
                () -> assertTrue(markdown.contains("| Purposes (art. 30.1.b) | Admission (`education.admission`) |\n")),
                () -> assertTrue(markdown.contains("| Lawful basis (art. 6.1) | Art. 6.1.e: Public interest |\n")),
                () -> assertTrue(markdown.contains("| Special categories (art. 9.2) | Art. 9.2.a: Explicit consent |\n")),
                () -> assertTrue(markdown.contains("| Personal data (art. 30.1.c) | Disability "
                        + "(`user.health_and_medical.disability`, special category); ")),
                () -> assertTrue(markdown.contains("| Transfers (art. 30.1.e) | United States (art. 46.2.c: SCCs) |\n")),
                () -> assertTrue(markdown.contains("| Retention (art. 30.1.f) | Not declared |\n")),
                () -> assertTrue(markdown.contains("| Required protection (art. 30.1.g) | Confidentiality High, "
                        + "integrity High |\n")),
                () -> assertTrue(markdown.contains("\nOpen points:\n\n- GDPR article(s) 6.1.e rest on Union or Member "
                        + "State law; name it as the legal reference\n")),
                () -> assertTrue(!markdown.contains("| Legitimate interest |")));
    }

    @Test
    @DisplayName("the JSON record carries keys, bases with articles, protection and open points")
    void json() {
        String json = checker.verify(admissions).toJson();
        assertAll(
                () -> assertTrue(json.startsWith("{\"activities\":[{\"key\":\"admissions\",\"name\":\"Admissions | intake\","
                        + "\"description\":\"Receives and assesses\\napplications.\",\"purposes\":[\"education.admission\"],"
                        + "\"dataSubjects\":[\"applicant\"],\"dataCategories\":[\"user.health_and_medical.disability\","
                        + "\"user.name\"],\"legalBasis\":{\"code\":\"PUBLIC_INTEREST\",\"article\":\"6.1.e\","
                        + "\"fideslang\":\"Public interest\"},\"specialCategoryCondition\":{\"code\":\"EXPLICIT_CONSENT\","
                        + "\"article\":\"9.2.a\",\"fideslang\":\"Explicit consent\"},\"recipients\":[\"Admissions board\"],"
                        + "\"transfers\":[{\"destination\":\"United States\",\"safeguard\":{\"code\":"
                        + "\"STANDARD_CONTRACTUAL_CLAUSES\",\"article\":\"46.2.c\",\"fideslang\":\"SCCs\"}}],"
                        + "\"protection\":{\"natures\":[\"ORDINARY\",\"SPECIAL_CATEGORY\"],\"confidentiality\":\"HIGH\","
                        + "\"integrity\":\"HIGH\",\"secret\":false},\"openPoints\":[")),
                () -> assertTrue(json.endsWith("]}]}")),
                () -> assertEquals(3, json.split("\"openPoints\":\\[")[1].split("\",\"").length));
    }
}
