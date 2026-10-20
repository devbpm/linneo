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

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Integrity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the data category a field holds.
 * <pre>{@code
 * @Category(Categories.USER_CONTACT_EMAIL)
 * private String email;
 *
 * @Category(value = Categories.USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
 * private String resource;
 *
 * @Category(value = Categories.USER_CONTENT_PRIVATE, confidentiality = Confidentiality.HIGH)
 * private String withdrawalReason;
 * }</pre>
 * The optional elements are arrays so that "not set" can be told apart from a value: leave them empty to take the
 * category's level, or give exactly one value. A level can only be raised above the category's, never lowered.
 * On a record component the annotation also applies to the component's field.
 *
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface Category {

    /**
     * The key of the data category, preferably one of the constants of {@link Categories}.
     *
     * @return the category key
     */
    String value();

    /**
     * How directly the stored value points to the person; only for personal categories.
     *
     * @return empty for {@link Identifiability#IDENTIFIED}, or exactly one value
     */
    Identifiability[] identifiability() default {};

    /**
     * A confidentiality stricter than the category's, for this field only.
     *
     * @return empty for the category's level, or exactly one value
     */
    Confidentiality[] confidentiality() default {};

    /**
     * An integrity stricter than the category's, for this field only.
     *
     * @return empty for the category's level, or exactly one value
     */
    Integrity[] integrity() default {};
}
