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
package io.github.devbpm.linneo.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * A minimal JSON-over-HTTP client for the integration tests against real catalog servers. It uses only
 * {@code java.base}, so the tests need no extra module or dependency.
 */
public final class ItHttp {

    /**
     * A response.
     *
     * @param status the HTTP status
     * @param body the body, empty when there is none
     */
    public record Response(int status, String body) {
    }

    private final String base;
    private final Map<String, String> headers = new LinkedHashMap<>();

    /**
     * Creates a client.
     *
     * @param base the URL every path is appended to
     */
    public ItHttp(String base) {
        this.base = base.replaceAll("/+$", "");
    }

    /**
     * Adds a header to every request.
     *
     * @param name the header name
     * @param value the value
     * @return this client
     */
    public ItHttp header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    /**
     * Sends a request and fails the test unless the status is 2xx.
     *
     * @param method the HTTP method
     * @param path the path after the base URL
     * @param body a JSON body, or {@code null}
     * @return the response body
     */
    public String send(String method, String path, String body) {
        Response response = request(method, path, body);
        if (response.status() / 100 != 2) {
            fail(method + " " + path + " -> " + response.status() + ": " + response.body()
                    + (body == null ? "" : "\nrequest: " + body));
        }
        return response.body();
    }

    /**
     * Sends a request.
     *
     * @param method the HTTP method
     * @param path the path after the base URL
     * @param body a JSON body, or {@code null}
     * @return the response, whatever its status
     */
    public Response request(String method, String path, String body) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(base + path).toURL().openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(30_000);
            connection.setReadTimeout(120_000);
            connection.setRequestProperty("Accept", "application/json");
            headers.forEach(connection::setRequestProperty);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String text = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            connection.disconnect();
            return new Response(status, text);
        } catch (IOException e) {
            throw new UncheckedIOException(method + " " + path, e);
        }
    }

    /**
     * The first string value of a member, wherever it is in the JSON text.
     *
     * @param json JSON text
     * @param member the member name
     * @return the value
     */
    public static String string(String json, String member) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(member) + "\":\"([^\"]*)\"").matcher(json);
        if (!matcher.find()) {
            fail("No member '" + member + "' in " + json);
        }
        return matcher.group(1);
    }

    /**
     * Every string value of a member, in order.
     *
     * @param json JSON text
     * @param member the member name
     * @return the values
     */
    public static List<String> strings(String json, String member) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(member) + "\":\"([^\"]*)\"").matcher(json);
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    /**
     * Encodes a value for a URL path segment or query parameter.
     *
     * @param value the value
     * @return the encoded value
     */
    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * The tags missing from a stored list and the ones the server added.
     *
     * @param expected the tags sent
     * @param stored the tags read back
     * @return an empty map when both lists hold the same tags
     */
    public static Map<String, List<String>> difference(List<String> expected, List<String> stored) {
        List<String> missing = expected.stream().filter(t -> !stored.contains(t)).toList();
        List<String> extra = stored.stream().filter(t -> !expected.contains(t)).toList();
        return missing.isEmpty() && extra.isEmpty() ? Map.of() : Map.of("missing", missing, "extra", extra);
    }
}
