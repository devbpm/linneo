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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Writes and reads the tab-separated text Linneo exchanges with applications (a taxonomy, a data inventory), and
 * computes its fingerprint. Not part of the public API.
 * <p>
 * The first line names the format and its version, separated by a tab. Other lines starting with {@code #} are
 * comments. Each remaining line is a row of cells separated by tabs; in a cell a backslash, a tab, a line feed and a
 * carriage return are written {@code \\}, {@code \t}, {@code \n} and {@code \r}, so a row is always one line, and a
 * lone UTF-16 surrogate, which UTF-8 cannot encode, is written as a backslash, {@code u} and its four hexadecimal
 * digits, so two
 * different texts never have the same UTF-8 bytes or fingerprint. An empty cell is an absent value.
 */
public final class Tsv {

    private final StringBuilder out = new StringBuilder();

    /**
     * Starts a text of a format.
     *
     * @param format the format name
     * @param version the format version
     */
    public Tsv(String format, int version) {
        out.append('#').append(format).append('\t').append(version).append('\n');
    }

    /**
     * Adds a comment line.
     *
     * @param comment the comment, on one line
     * @return this writer
     */
    public Tsv comment(String comment) {
        out.append("# ").append(comment).append('\n');
        return this;
    }

    /**
     * Adds a row.
     *
     * @param cells the cells; {@code null} is written as an empty cell
     * @return this writer
     */
    public Tsv row(Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                out.append('\t');
            }
            escape(cells[i] == null ? "" : cells[i].toString());
        }
        out.append('\n');
        return this;
    }

    /**
     * The text written so far.
     *
     * @return the text
     */
    public String text() {
        return out.toString();
    }

    /**
     * Reads a text of a format.
     *
     * @param text the text
     * @param format the expected format name
     * @param version the version this reader understands
     * @return the rows, each a list of cells, with absent values as {@code null}
     * @throws IllegalArgumentException if the first line does not name the format and version, or a cell has an
     *         invalid escape
     */
    public static List<List<String>> read(String text, String format, int version) {
        Objects.requireNonNull(text, "text");
        List<String> lines = text.lines().toList();
        String expected = "#" + format + "\t" + version;
        if (lines.isEmpty() || !lines.getFirst().equals(expected)) {
            throw new IllegalArgumentException("Not a " + format + " text of version " + version
                    + ": the first line must be '" + expected.replace("\t", "<tab>") + "'");
        }
        List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            List<String> row = new ArrayList<>();
            for (String cell : line.split("\t", -1)) {
                row.add(cell.isEmpty() ? null : unescape(cell, i + 1));
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * The SHA-256 fingerprint of a text, as lower-case hexadecimal.
     *
     * @param text the text, encoded as UTF-8
     * @return 64 hexadecimal digits
     */
    public static String fingerprint(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java platform provides SHA-256", e);
        }
    }

    private void escape(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '\t' -> out.append("\\t");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                default -> {
                    if (Character.isHighSurrogate(c) && i + 1 < value.length()
                            && Character.isLowSurrogate(value.charAt(i + 1))) {
                        out.append(c).append(value.charAt(++i));
                    } else if (Character.isSurrogate(c)) {
                        out.append("\\u").append(HexFormat.of().toHexDigits(c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
    }

    private static String unescape(String cell, int line) {
        if (cell.indexOf('\\') < 0) {
            return cell;
        }
        StringBuilder value = new StringBuilder(cell.length());
        for (int i = 0; i < cell.length(); i++) {
            char c = cell.charAt(i);
            if (c != '\\') {
                value.append(c);
                continue;
            }
            if (++i == cell.length()) {
                throw new IllegalArgumentException("Line " + line + ": a cell ends with a lone backslash");
            }
            switch (cell.charAt(i)) {
                case '\\' -> value.append('\\');
                case 't' -> value.append('\t');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 'u' -> {
                    if (i + 4 >= cell.length()) {
                        throw new IllegalArgumentException("Line " + line + ": '\\u' needs four hexadecimal digits");
                    }
                    try {
                        value.append((char) HexFormat.fromHexDigits(cell, i + 1, i + 5));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Line " + line + ": '\\u' needs four hexadecimal digits",
                                e);
                    }
                    i += 4;
                }
                default -> throw new IllegalArgumentException("Line " + line + ": invalid escape '\\"
                        + cell.charAt(i) + "'");
            }
        }
        return value.toString();
    }
}
