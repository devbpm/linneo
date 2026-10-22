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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {

    private static String value(String text) {
        return new Json().field("v", text).end();
    }

    @Test
    @DisplayName("escapes quotes, backslashes and every control character (RFC 8259, section 7)")
    void escapesWhatJsonRequires() {
        assertAll(
                () -> assertEquals("{\"v\":\"say \\\"hi\\\" \\\\ ok\"}", value("say \"hi\" \\ ok")),
                () -> assertEquals("{\"v\":\"\\n\\r\\t\\b\\f\"}", value("\n\r\t\b\f")),
                () -> assertEquals("{\"v\":\"\\u0000\\u0001\\u001f\"}", value("\u0000\u0001\u001f")),
                () -> assertEquals("{\"v\":\"\u007f\"}", value("\u007f")),
                () -> assertEquals("{\"\\\"k\\\"\":\"x\"}", new Json().field("\"k\"", "x").end()));
    }

    @Test
    @DisplayName("keeps non-ASCII text as is, including characters outside the Basic Multilingual Plane")
    void keepsUnicode() {
        String text = "Protección de datos · ñ ·   · 🔒 · 𠀋";
        assertAll(
                () -> assertEquals("{\"v\":\"" + text + "\"}", value(text)),
                () -> assertEquals(2, "🔒".length()),
                () -> assertEquals(text.codePointCount(0, text.length()) + 8,
                        value(text).codePointCount(0, value(text).length())));
    }

    @Test
    @DisplayName("writes arrays, booleans and nested objects in the order given")
    void structures() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("b", "2");
        properties.put("a", "1");
        String json = new Json().field("name", "x").field("flag", false).field("list", List.of("p", "q"))
                .field("props", properties).field("empty", List.of()).end();
        assertAll(
                () -> assertEquals("{\"name\":\"x\",\"flag\":false,\"list\":[\"p\",\"q\"],"
                        + "\"props\":{\"b\":\"2\",\"a\":\"1\"},\"empty\":[]}", json),
                () -> assertEquals("[{},[]]", Json.array(List.of("{}", "[]"))),
                () -> assertEquals("{}", new Json().end()));
    }
}
