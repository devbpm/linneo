/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.devbpm.linneo.classify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** The types the classifier creates when it resolves type variables follow the contracts of the JDK's own. */
class SubstitutedTypesTest {

    @SuppressWarnings("unused")
    private List<String> list;
    @SuppressWarnings("unused")
    private List<String>[] lists;
    @SuppressWarnings("unused")
    private List<? extends Number> upper;
    @SuppressWarnings("unused")
    private List<? super Integer> lower;
    @SuppressWarnings("unused")
    private List<?> any;

    private static Type type(String field) throws NoSuchFieldException {
        return SubstitutedTypesTest.class.getDeclaredField(field).getGenericType();
    }

    private static Type argument(String field) throws NoSuchFieldException {
        return ((ParameterizedType) type(field)).getActualTypeArguments()[0];
    }

    @Test
    @DisplayName("they are equal to the JDK's types of the same content, both ways, with the same hash code and text")
    void contracts() throws NoSuchFieldException {
        Type[] none = {};
        List<Type[]> pairs = List.of(
                new Type[] {type("list"), new Classifier.Parameterized(List.class, null, new Type[] {String.class})},
                new Type[] {type("lists"), new Classifier.GenericArray(type("list"))},
                new Type[] {argument("upper"), new Classifier.Wildcard(new Type[] {Number.class}, none)},
                new Type[] {argument("lower"), new Classifier.Wildcard(new Type[] {Object.class},
                        new Type[] {Integer.class})},
                new Type[] {argument("any"), new Classifier.Wildcard(new Type[] {Object.class}, none)});
        assertAll(pairs.stream().map(pair -> () -> assertAll(
                () -> assertEquals(pair[0], pair[1]),
                () -> assertEquals(pair[1], pair[0]),
                () -> assertEquals(pair[0].hashCode(), pair[1].hashCode()),
                () -> assertEquals(pair[0].toString(), pair[1].toString()))));
        assertAll(
                () -> assertNotEquals(type("list"), new Classifier.Parameterized(List.class, null,
                        new Type[] {Integer.class})),
                () -> assertNotEquals(argument("upper"), new Classifier.Wildcard(new Type[] {Integer.class}, none)),
                () -> assertNotEquals(type("lists"), new Classifier.GenericArray(String.class)));
    }
}
