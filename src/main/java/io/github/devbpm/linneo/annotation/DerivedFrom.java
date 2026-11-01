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
 * Declares a field whose value is computed from other fields. A value derived from data is that data, or reveals
 * it: the age computed from a date of birth is personal data, and so is the full name built from a first and last
 * name. Use this annotation for such values, never {@link Exempt @Exempt}.
 * <pre>{@code
 * @DerivedFrom("birthDate")                                   // takes the classification of its source
 * private int age;
 *
 * @Category(Categories.USER_NAME)                             // declares its own, at least as protective
 * @DerivedFrom({"firstName", "lastName"})
 * private String fullName;
 *
 * @Category(value = Categories.USER_DEMOGRAPHIC_DATE_OF_BIRTH, identifiability = Identifiability.AGGREGATED)
 * @DerivedFrom("birthDate")                                   // a summary of many people: no longer personal
 * private double averageAge;
 *
 * @DerivedFrom(type = Student.class, value = "email")         // a view of another class
 * private String email;
 * }</pre>
 * Without {@link Category @Category} the field takes the classification of its source; with several sources they
 * must agree, or the field must declare its category. With {@code @Category} the field must protect at least as much
 * as each source ({@link io.github.devbpm.linneo.Protection#covers(io.github.devbpm.linneo.Protection)}). The one
 * explicit way to protect less is to declare the value anonymized or aggregated, with the
 * {@link Category#identifiability()} of a personal category, which a review can see.
 * <p>
 * A source is a field path: the name of a field of the class, inherited ones included, or the path of a field of a
 * {@link Composite @Composite} it holds, such as {@code "home.city"}; the path of a composite field stands for all
 * the fields below it. A derived field may be the source of another, as long as no derivation depends on itself.
 *
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface DerivedFrom {

    /**
     * The paths of the fields the value is computed from.
     *
     * @return one path or more
     */
    String[] value();

    /**
     * The class that declares the sources, when it is not the class being classified: for a view, a data transfer
     * object or a report row built from an entity. {@code void.class}, the default, means the class being
     * classified.
     *
     * @return the class of the sources
     */
    Class<?> type() default void.class;
}
