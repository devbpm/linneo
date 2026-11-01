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
 * Excludes a field that holds no data of the model from classification: a cache, a technical handle or state that
 * a framework keeps. Static, synthetic and {@code transient} fields are always excluded.
 * <p>
 * A value computed from other fields is data, not an exemption: the age computed from a date of birth is personal
 * data. Classify it with {@link DerivedFrom @DerivedFrom} or {@link Category @Category}. Exempted fields are listed by
 * {@link io.github.devbpm.linneo.classify.ClassClassification#exempted()}, so a review can see each exemption and
 * notice when a field that used to be classified becomes exempt.
 *
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface Exempt {

    /**
     * Why the field holds no data, for the people who review the classification.
     *
     * @return a non-blank reason
     */
    String value();
}
