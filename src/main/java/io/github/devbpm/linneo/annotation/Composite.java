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
 * Marks a class whose instances are part of another record, such as a JPA {@code @Embeddable} address or a value
 * object, so that the fields that hold it are classified by its own fields.
 * <pre>{@code
 * @Composite
 * public class Address {
 *     @Category(Categories.USER_CONTACT_ADDRESS_STREET) private String street;
 *     @Category(Categories.USER_CONTACT_ADDRESS_CITY) private String city;
 * }
 *
 * public class Student {
 *     private Address home;                                   // home.street, home.city
 *     private List<Address> previous;                         // previous.street, previous.city
 *     @Category(Categories.USER_CONTACT_ADDRESS)              // stored as one value: must cover street and city
 *     private Address shipping;
 * }
 * }</pre>
 * A field without an annotation of its own whose type is a composite class, or an array, collection or
 * {@link java.util.Optional} of one, gets one classification per field of the composite, with the path
 * {@code field.part}. The composite's own {@link DefaultCategory @DefaultCategory} applies to its fields; the default
 * of the class that holds it does not. A field that stores the composite as one value, for example as JSON,
 * declares its category with {@link Category @Category} or {@link CategoryByRow @CategoryByRow}, which must protect
 * at least as much as every field of the composite. A {@link java.util.Map} is never descended into.
 * <p>
 * The marker is Linneo's own rather than a framework annotation, so that the library needs no dependency. Forgetting
 * it is safe: the field then has no category and fails verification.
 *
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Composite {
}
