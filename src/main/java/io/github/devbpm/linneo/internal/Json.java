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
package io.github.devbpm.linneo.internal;

import java.util.List;
import java.util.Map;

/**
 * Writes the JSON objects of the catalog request bodies, so the library needs no JSON dependency. Not part of the
 * public API.
 */
public final class Json {

    private final StringBuilder out = new StringBuilder("{");
    private boolean first = true;

    /**
     * Adds a string member.
     *
     * @param name the member name
     * @param value the value
     * @return this writer
     */
    public Json field(String name, String value) {
        key(name);
        string(out, value);
        return this;
    }

    /**
     * Adds a boolean member.
     *
     * @param name the member name
     * @param value the value
     * @return this writer
     */
    public Json field(String name, boolean value) {
        key(name);
        out.append(value);
        return this;
    }

    /**
     * Adds an array of strings.
     *
     * @param name the member name
     * @param values the values
     * @return this writer
     */
    public Json field(String name, List<String> values) {
        key(name);
        out.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            string(out, values.get(i));
        }
        out.append(']');
        return this;
    }

    /**
     * Adds an object whose members are all strings.
     *
     * @param name the member name
     * @param values the members, in iteration order
     * @return this writer
     */
    public Json field(String name, Map<String, String> values) {
        Json object = new Json();
        values.forEach(object::field);
        return raw(name, object.end());
    }

    /**
     * Adds a member whose value is already JSON.
     *
     * @param name the member name
     * @param json a JSON value
     * @return this writer
     */
    public Json raw(String name, String json) {
        key(name);
        out.append(json);
        return this;
    }

    /**
     * Closes the object.
     *
     * @return the JSON text
     */
    public String end() {
        return out.append('}').toString();
    }

    /**
     * A JSON array of values that are already JSON.
     *
     * @param values JSON values
     * @return the JSON text
     */
    public static String array(List<String> values) {
        return "[" + String.join(",", values) + "]";
    }

    private void key(String name) {
        if (!first) {
            out.append(',');
        }
        first = false;
        string(out, name);
        out.append(':');
    }

    private static void string(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
