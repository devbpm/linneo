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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the data category of every field of a class that has no annotation of its own: a positive statement
 * that those fields hold no personal data, with the reason.
 * <pre>{@code
 * @DefaultCategory(value = Categories.SYSTEM_CONFIGURATION, reason = "menu configuration")
 * public class MenuGroup { ... }
 * }</pre>
 * The category must not be personal: personal data is always declared field by field with {@link Category}.
 * It applies to the fields the class declares and, when the class is the one being classified, to unannotated
 * fields inherited from superclasses that declare no default of their own. It is not inherited by subclasses.
 *
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultCategory {

    /**
     * The key of a non-personal data category, preferably one of the constants of {@link Categories}.
     *
     * @return the category key
     */
    String value();

    /**
     * Why the class holds no personal data, for the people who review the classification.
     *
     * @return a non-blank reason
     */
    String reason();
}
