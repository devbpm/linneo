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
package io.github.devbpm.linneo.classify.scan;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.annotation.Category;

/**
 * A model class for {@code ModelClassesTest}, with a nested part and an anonymous class that are found or skipped.
 */
@SuppressWarnings("unused")
public class Customer {

    @Category(Categories.USER_NAME)
    private String name;

    private static final Runnable NOTHING = new Runnable() {
        @Override
        public void run() {
        }
    };

    /**
     * A nested model class.
     */
    public static class Address {
        @Category(Categories.USER_CONTACT_ADDRESS_CITY)
        private String city;
    }

    /**
     * Not a model class: an interface.
     */
    public interface Repository {
    }

    /**
     * Not a model class: an enum.
     */
    public enum Kind { PERSON, COMPANY }
}
