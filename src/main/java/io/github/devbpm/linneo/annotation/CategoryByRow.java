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
package io.github.devbpm.linneo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a field whose data category depends on each row, such as the content of a document that may be a
 * certificate in one row and a medical report in the next.
 * <pre>{@code
 * @CategoryByRow(declaredBy = "the category of the document", ceiling = Categories.USER_HEALTH_AND_MEDICAL)
 * private byte[] content;
 * }</pre>
 * Wherever a single category is needed for the whole field (a catalogue, an export label) the {@link #ceiling()}
 * is used: the most protective category any row may hold.
 *
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface CategoryByRow {

    /**
     * What decides the category of each row, for the people who review the classification.
     *
     * @return a non-blank description
     */
    String declaredBy();

    /**
     * The key of the most protective data category a row may hold.
     *
     * @return the category key
     */
    String ceiling();
}
