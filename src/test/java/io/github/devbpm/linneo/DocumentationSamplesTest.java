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
package io.github.devbpm.linneo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the Java samples of the documentation ({@code README.md} and every page under {@code docs/}: the
 * {@code ```java} blocks of Markdown pages and the {@code <pre><code class="language-java">} blocks of HTML pages)
 * against the library, so that a sample using a type, method, constant or annotation element the library does not
 * have fails the build.
 * <p>
 * Samples are rarely complete programs: they use variables, entities and helpers their page introduces elsewhere.
 * Each sample is therefore wrapped by its shape (a compilation unit, class members, statements, or the continuation
 * of a builder chain) and compiled with the library's packages imported, and two kinds of error fail the test: an
 * error that names an element of the library, and an error that is not about a missing name at all (a syntax error,
 * an incompatible type). A name the sample takes from its page ({@code student}, {@code Enrollment}) is context.
 * Type signatures of the reference ({@code public record Protection(...)}) are compared with the library's types,
 * and module declarations with {@code module-info.java}. A sample that is not meant to compile, such as an exercise
 * with deliberate mistakes, is preceded by {@code <!-- unchecked: the reason -->}.
 */
class DocumentationSamplesTest {

    private static final String API = "io.github.devbpm.linneo";
    private static final String MODIFIERS = "((public|protected|private|abstract|static|final|sealed|non-sealed"
            + "|strictfp)\\s+)*";
    private static final Pattern FENCE = Pattern.compile("^(\\s*)```java\\s*$");
    private static final Pattern UNCHECKED = Pattern.compile("^\\s*<!--\\s*unchecked:\\s*\\S.*-->\\s*$");
    /** A Java block of an HTML page; the code starts right after the tags, as a blank first line would show. */
    private static final Pattern HTML_BLOCK = Pattern.compile("^\\s*<pre\\b[^>]*><code class=\"language-java\">(.*)$");
    private static final String HTML_BLOCK_END = "</code></pre>";
    private static final Pattern IMPORT = Pattern.compile("^(import\\s+(static\\s+)?[\\w.]+(\\.\\*)?\\s*;).*$");
    private static final Pattern PACKAGE = Pattern.compile("^package\\s+[\\w.]+\\s*;.*$");
    private static final Pattern MODULE = Pattern.compile("^(open\\s+)?module\\s+([\\w.]+)\\s*\\{");
    // Annotations, with arguments nested one level deep, never backtrack: each one is followed by whitespace.
    private static final Pattern TYPE = Pattern.compile("^(?:@(?!interface\\b)\\w++"
            + "(?:\\([^()]*+(?:\\([^()]*+\\)[^()]*+)*+\\))?+\\s++)*+" + MODIFIERS
            + "(?<kind>class|interface|enum|record|@interface)\\s+(?<name>\\w+)");
    private static final Pattern PUBLIC_TYPE = Pattern.compile("(?m)^public\\s+(?=" + MODIFIERS
            + "(class|interface|enum|record|@interface)\\s)");
    /** The words a line that declares a method does not start with. */
    private static final Pattern NOT_A_METHOD = Pattern.compile(
            "(return|new|else|throw|class|interface|enum|record)\\b");
    /** The return type and modifiers of a method, such as {@code public static Map<String, List<X>>}. */
    private static final Pattern RETURN_TYPE = Pattern.compile("\\w[\\w<>\\[\\], ?]*+");
    /** What follows the parameters of a method declared on one line. */
    private static final Pattern AFTER_PARAMETERS = Pattern.compile("\\s*+(throws\\s++[\\w., ]++)?+\\s*+\\{\\s*+");
    /** Statements that look like a method declaration: {@code if (x) {}}. */
    private static final Set<String> CONTROL = Set.of("if", "for", "while", "switch", "catch", "synchronized");
    private static final Pattern MEMBER_MODIFIER = Pattern.compile("^(private|protected|public|static|abstract)\\s");
    private static final Pattern DIAGNOSTIC = Pattern.compile("^S(\\d+)_(\\d+)\\.java:(\\d+):\\d+: "
            + "(compiler\\.err\\.[\\w.]++):?\\s?(.*)$");
    private static final Pattern DECLARED_TYPE = Pattern.compile(
            "\\b(class|interface|enum|record|@interface)\\s+([A-Z]\\w*)");
    // A capitalized name with a lower-case letter: a type, not a constant such as ENTITIES.
    private static final Pattern NESTED_IMPORT = Pattern.compile(
            "import\\s+(?:static\\s+)?(io\\.github\\.devbpm\\.linneo[\\w.]*\\.[A-Z]\\w*)\\.([A-Z]\\w*|\\*)\\s*;");
    private static final Pattern MISSING_TYPE = Pattern.compile(
            "kindname\\.(class|variable), ([A-Z][A-Z0-9_]*+[a-z]\\w*+),");
    /**
     * Types the pages use without declaring them: frameworks' and the JDK's types the samples do not import, and
     * application types a page only names.
     */
    private static final Set<String> CONTEXT_TYPES = Set.of("Bean", "ConcurrentHashMap", "Customer", "Entities",
            "EntityManagerFactory", "EntityType", "FieldRow", "Invoice", "OpenApiCustomizer", "Schema", "Setting",
            "WrongFields");
    private static final Pattern ANONYMOUS = Pattern.compile("\\$\\d");
    private static final Pattern DIRECTIVE = Pattern.compile("(requires|exports|opens|uses|provides)\\s++([^;]+);");
    /**
     * A missing name shaped like a key constant ({@code USER_CONTACT_EMAIL}): every key constant is imported, so it
     * names a key the taxonomy does not have. Constants a page declares for itself are better named otherwise.
     */
    private static final Pattern KEY_CONSTANT = Pattern.compile("^kindname\\.variable, [A-Z][A-Z0-9]*(_[A-Z0-9]+)+,");

    /** What every sample may use without importing it, as the pages assume. */
    private static final String IMPORTS = Stream.of(
                    API, API + ".annotation", API + ".classify", API + ".processing", API + ".catalog.openmetadata",
                    API + ".catalog.datahub", API + ".catalog.atlas", API + ".ens", API + ".export.odcs",
                    API + ".export.openapi", API + ".export.postgresql", API + ".extension.university",
                    API + ".extension.finance", API + ".extension.supplychain", API + ".extension.projectoperations",
                    API + ".extension.humanresources", "java.util", "java.util.function", "java.util.stream",
                    "java.time", "java.math", "java.io", "java.nio.file", "org.junit.jupiter.api")
            .map(p -> "import " + p + ".*;").collect(Collectors.joining(" "))
            + " import static " + API + ".Categories.*; import static " + API + ".DataUses.*;"
            + " import static " + API + ".DataSubjects.*; import static org.junit.jupiter.api.Assertions.*;";

    /**
     * The variables most pages share, as fields of a superclass so that a sample declaring its own, as a field or as
     * a local variable, hides them.
     */
    private static final String CONTEXT = "package docs; " + IMPORTS + " public abstract class Context {"
            + " protected Taxonomy taxonomy; protected Classifier classifier; protected ProcessingChecker checker; }";

    /** The types a sample that starts with {@code .method(...)} may continue. */
    private static final List<String> RECEIVERS = List.of("TaxonomyBuilder", "ProcessingActivity.Builder",
            "Governance.Builder", "DataInventory.Builder");

    /** Samples that prove the check works: these must fail... */
    private static final List<Sample> CANARIES_FAILING = List.of(
            new Sample("canary", 1, List.of("Taxonomy.standard().noSuchMethod();")),
            new Sample("canary", 2, List.of("@Category(USER_NO_SUCH_KEY) private String field;")),
            new Sample("canary", 3, List.of("taxonomy.requireCategory(1);")),
            new Sample("canary", 5, List.of("OpenMetadataExporter exporter = new OpenMetadataExporter(taxonomy);")),
            new Sample("canary", 6, List.of("PostgresMasker.of(DisclosurePolicy.forLogs());")),
            new Sample("canary", 7, List.of("OldPolicy.Treatment treatment = null;")),
            new Sample("canary", 8, List.of("@Categry(USER_CONTACT_EMAIL) private String field;")));
    /** ...and this one, which only uses names of its page, must pass. */
    private static final Sample CANARY_PASSING = new Sample("canary", 4,
            List.of("undefinedVariable.anything(Customer.CONSTANT, ENTITIES, student.email());"));

    record Sample(String page, int line, List<String> code) {

        String text() {
            return String.join("\n", code);
        }

        String at(int offset) {
            return page + ":" + (line + offset);
        }
    }

    /** An error that fails the test, on a line of a compilation unit. */
    record Problem(int line, String message) {
    }

    @TempDir
    Path work;

    @Test
    @DisplayName("every Java sample of the documentation compiles against the library")
    void samplesCompile() throws IOException, URISyntaxException {
        List<Sample> samples = new ArrayList<>();
        int marked = 0;
        for (Path page : pages()) {
            marked += read(page, samples);
        }
        int unchecked = marked;
        assertTrue(samples.size() > 300, "expected the documentation samples, found " + samples.size());
        int checked = samples.size();
        samples.addAll(CANARIES_FAILING);
        samples.add(CANARY_PASSING);

        Map<String, Sample> units = new LinkedHashMap<>();
        List<Path> files = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        Path context = work.resolve("src").resolve("docs").resolve("Context.java");
        Files.createDirectories(context.getParent());
        files.add(Files.writeString(context, CONTEXT));
        Map<String, List<Class<?>>> types = libraryTypes();
        for (int i = 0; i < samples.size(); i++) {
            Sample sample = samples.get(i);
            String code = sample.text().replaceAll("//[^\n]*", "").strip();
            if (MODULE.matcher(code).find()) {
                failures.addAll(checkModule(sample));
                continue;
            }
            if (TYPE.matcher(code).find() && code.indexOf('{') < 0 && code.indexOf(';') < 0) {
                failures.addAll(checkSignature(sample, types));
                continue;
            }
            List<String> variants = sources(sample);
            for (int v = 0; v < variants.size(); v++) {
                String name = "S" + i + "_" + v;
                Path file = work.resolve("src").resolve("s" + i + "v" + v).resolve(name + ".java");
                Files.createDirectories(file.getParent());
                Files.writeString(file, variants.get(v).replace("%PACKAGE%", "docs.s" + i + "v" + v)
                        .replace("%CLASS%", name));
                files.add(file);
                units.put(name, sample);
            }
        }

        Set<String> declared = declaredTypes(samples, types);
        Map<String, List<Problem>> errors = compile(files, declared);
        Map<Sample, List<List<Problem>>> byVariant = new LinkedHashMap<>();
        units.forEach((name, sample) -> byVariant.computeIfAbsent(sample, s -> new ArrayList<>())
                .add(errors.getOrDefault(name, List.of())));
        byVariant.forEach((sample, variants) -> {
            if (variants.stream().noneMatch(List::isEmpty)) {
                variants.stream().min(Comparator.comparingInt(List::size)).orElseThrow()
                        .forEach(error -> failures.add(sample.at(error.line() - 2) + ": " + error.message()));
            }
        });

        for (Sample canary : CANARIES_FAILING) {
            assertTrue(failures.removeIf(f -> f.startsWith(canary.at(0) + ":")),
                    () -> "the check is broken, this sample passed: " + canary.text());
        }
        assertTrue(failures.stream().noneMatch(f -> f.startsWith(CANARY_PASSING.page() + ":")),
                () -> "the check is broken, a sample using only names of its page failed: " + failures);
        assertEquals(List.of(), failures, () -> failures.size() + " errors in the documentation samples ("
                + checked + " checked, " + unchecked + " marked unchecked):\n" + String.join("\n", failures));
    }

    @Test
    @DisplayName("the Java blocks of an HTML page are read without their tags and character references")
    void htmlBlocks() {
        List<Sample> samples = new ArrayList<>();
        int unchecked = readHtml("page.html", List.of("<p>Text</p>",
                "<pre class=\"codigo\" tabindex=\"0\"><code class=\"language-java\">"
                        + "List&lt;String&gt; a = List.of(&quot;&amp;&quot;);",
                "a.size();",
                "</code></pre>",
                "<!-- unchecked: an exercise -->",
                "<pre class=\"codigo\" tabindex=\"0\"><code class=\"language-java\">broken(</code></pre>",
                "<pre class=\"codigo\" tabindex=\"0\"><code class=\"language-text\">not Java</code></pre>",
                "<pre class=\"codigo\" tabindex=\"0\"><code class=\"language-java\">int b = 1;</code></pre>"), samples);
        assertEquals(1, unchecked);
        assertEquals(List.of(new Sample("page.html", 2, List.of("List<String> a = List.of(\"&\");", "a.size();", "")),
                new Sample("page.html", 8, List.of("int b = 1;"))), samples);
    }

    private static List<Path> pages() throws IOException {
        Path docs = Path.of("docs");
        assertTrue(Files.isDirectory(docs), "run from the project directory, which holds docs/");
        try (Stream<Path> walk = Files.walk(docs)) {
            List<Path> pages = new ArrayList<>(walk
                    .filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".html")).sorted().toList());
            pages.addFirst(Path.of("README.md"));
            return pages;
        }
    }

    /** Adds the samples of a page and returns how many it marks unchecked. */
    private static int read(Path page, List<Sample> samples) throws IOException {
        List<String> lines = Files.readAllLines(page);
        String name = page.toString().replace('\\', '/');
        if (name.endsWith(".html")) {
            return readHtml(name, lines, samples);
        }
        int unchecked = 0;
        String previous = "";
        for (int i = 0; i < lines.size(); i++) {
            Matcher fence = FENCE.matcher(lines.get(i));
            if (!fence.matches()) {
                if (!lines.get(i).isBlank()) {
                    previous = lines.get(i);
                }
                continue;
            }
            int indent = fence.group(1).length();
            int end = i + 1;
            while (!lines.get(end).strip().startsWith("```")) {
                end++;
            }
            if (UNCHECKED.matcher(previous).matches()) {
                unchecked++;
            } else {
                samples.add(new Sample(name, i + 2, lines.subList(i + 1, end).stream()
                        .map(l -> l.length() >= indent && l.substring(0, indent).isBlank() ? l.substring(indent) : l)
                        .toList()));
            }
            previous = "";
            i = end;
        }
        return unchecked;
    }

    /**
     * Adds the samples of an HTML page, the {@code <pre><code class="language-java">} blocks whose code starts on the
     * line of the tags, and returns how many it marks unchecked.
     */
    private static int readHtml(String name, List<String> lines, List<Sample> samples) {
        int unchecked = 0;
        String previous = "";
        for (int i = 0; i < lines.size(); i++) {
            Matcher block = HTML_BLOCK.matcher(lines.get(i));
            if (!block.matches()) {
                if (!lines.get(i).isBlank()) {
                    previous = lines.get(i);
                }
                continue;
            }
            List<String> code = new ArrayList<>();
            String line = block.group(1);
            int end = i;
            while (!line.contains(HTML_BLOCK_END)) {
                code.add(line);
                line = lines.get(++end);
            }
            code.add(line.substring(0, line.indexOf(HTML_BLOCK_END)));
            if (UNCHECKED.matcher(previous).matches()) {
                unchecked++;
            } else {
                samples.add(new Sample(name, i + 1, code.stream().map(DocumentationSamplesTest::unescape).toList()));
            }
            previous = "";
            i = end;
        }
        return unchecked;
    }

    /** The text of an HTML line without its character references. */
    private static String unescape(String html) {
        return html.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&amp;", "&");
    }

    /**
     * The compilation units for a sample, all with the sample's first line on line 2 so that a diagnostic maps
     * back to the page. A builder continuation has one unit per receiver; any of them may compile.
     */
    private static List<String> sources(Sample sample) {
        List<String> imports = new ArrayList<>();
        List<String> body = new ArrayList<>();
        for (String line : sample.code()) {
            Matcher importLine = IMPORT.matcher(line);
            if (importLine.matches()) {
                imports.add(importLine.group(1));
            }
            body.add(importLine.matches() || PACKAGE.matcher(line).matches() ? "" : line);
        }
        String head = "package %PACKAGE%; " + IMPORTS + " " + String.join(" ", imports);
        List<String> topLevel = topLevel(body);
        // Without public, several types fit in one file of any name, or nest in a class or a method.
        String code = PUBLIC_TYPE.matcher(String.join("\n", body)).replaceAll("");
        if (topLevel.stream().allMatch(l -> TYPE.matcher(l).find() || l.startsWith("@") || l.startsWith("}"))) {
            return List.of(head + "\n" + code + "\n");
        }
        String type = head + " class %CLASS% extends docs.Context {";
        if (code.strip().startsWith(".")) {
            return RECEIVERS.stream().map(receiver -> type + " void run(" + receiver + " receiver) throws Exception {"
                    + " receiver\n" + code + "\n; } }\n").toList();
        }
        if (topLevel.getFirst().startsWith("@")
                || topLevel.stream().anyMatch(l -> MEMBER_MODIFIER.matcher(l).find() || declaresMethod(l))) {
            return List.of(type + "\n" + code + "\n}\n");
        }
        return List.of(type + " void run() throws Exception {\n" + code + "\n} }\n");
    }

    /**
     * Whether a line declares a method: a return type, whitespace, a name that is not a statement keyword, the
     * parameters up to the last parenthesis, maybe a {@code throws} clause, and an opening brace. The parameters start
     * at the first parenthesis, since a type has none, so the name is the word right before it.
     */
    private static boolean declaresMethod(String line) {
        int open = line.indexOf('(');
        int close = line.lastIndexOf(')');
        if (open < 0 || close < open || NOT_A_METHOD.matcher(line).lookingAt()) {
            return false;
        }
        String head = line.substring(0, open).stripTrailing();
        int name = head.length();
        while (name > 0 && isWordCharacter(head.charAt(name - 1))) {
            name--;
        }
        String type = head.substring(0, name);
        return name < head.length() && !type.isBlank() && Character.isWhitespace(type.charAt(type.length() - 1))
                && RETURN_TYPE.matcher(type.stripTrailing()).matches() && !CONTROL.contains(head.substring(name))
                && AFTER_PARAMETERS.matcher(line.substring(close + 1)).matches();
    }

    /** A character of {@code \w}: an ASCII letter, a digit or an underscore. */
    private static boolean isWordCharacter(char c) {
        return c < 128 && (Character.isLetterOrDigit(c) || c == '_');
    }

    /** The lines of a sample that start at its first column, without blank lines and comments. */
    private static List<String> topLevel(List<String> lines) {
        List<String> topLevel = lines.stream()
                .filter(l -> !l.isBlank() && !Character.isWhitespace(l.charAt(0)))
                .filter(l -> !l.startsWith("//") && !l.startsWith("/*") && !l.startsWith("*"))
                .toList();
        return topLevel.isEmpty() ? List.of("") : topLevel;
    }

    /** Compiles the units in one run and returns the errors that fail the test, by unit name. */
    private Map<String, List<Problem>> compile(List<Path> files, Set<String> declared) throws URISyntaxException {
        ToolProvider javac = ToolProvider.findFirst("javac")
                .orElseThrow(() -> new AssertionError("the documentation samples need a JDK with javac"));
        // Raw diagnostics are stable and not translated; stopping after flow analysis writes no class files.
        List<String> arguments = new ArrayList<>(List.of("-XDrawDiagnostics", "-XDshould-stop.ifError=FLOW",
                "-XDshould-stop.at=FLOW", "-Xmaxerrs", "100000", "-nowarn", "-proc:none", "-implicit:none",
                "-encoding", "UTF-8", "-d", work.resolve("classes").toString(),
                "--module-path", location(Taxonomy.class), "--add-modules", API,
                "-cp", location(Test.class) + File.pathSeparator + location(AssertionFailedError.class)));
        files.forEach(file -> arguments.add(file.toString()));
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        javac.run(writer, writer, arguments.toArray(String[]::new));
        writer.flush();
        Map<String, List<Problem>> errors = new LinkedHashMap<>();
        for (String line : out.toString().lines().toList()) {
            Matcher diagnostic = DIAGNOSTIC.matcher(line);
            if (!diagnostic.matches()) {
                // An error outside the samples (an option, the shared context) would hide every sample's errors.
                assertTrue(!line.contains("compiler.err"), () -> "javac failed: " + out);
                continue;
            }
            if (fails(diagnostic.group(4), diagnostic.group(5)) || undeclaredType(diagnostic.group(4),
                    diagnostic.group(5), declared)) {
                errors.computeIfAbsent("S" + diagnostic.group(1) + "_" + diagnostic.group(2), k -> new ArrayList<>())
                        .add(new Problem(Integer.parseInt(diagnostic.group(3)),
                                diagnostic.group(4) + " " + diagnostic.group(5)));
            }
        }
        return errors;
    }

    /**
     * Whether a compiler error is a fault of the sample rather than context it takes from its page: an error that
     * names an element of the library, or one that is not about a missing name.
     */
    private static boolean fails(String code, String arguments) {
        if (code.startsWith("compiler.err.cant.resolve")) {
            int location = arguments.indexOf("compiler.misc.location");
            return location >= 0 && arguments.indexOf(API, location) >= 0 || KEY_CONSTANT.matcher(arguments).find();
        }
        if (code.equals("compiler.err.doesnt.exist")) {
            return arguments.startsWith(API);
        }
        return true;
    }

    /**
     * The types the samples declare ({@code class Student}, {@code record Enrollment}) and those of the shared
     * context: the names a sample may take from its page.
     */
    private static Set<String> declaredTypes(List<Sample> samples, Map<String, List<Class<?>>> types) {
        Set<String> declared = new TreeSet<>(CONTEXT_TYPES);
        // The top-level types of the library, imported by every sample.
        types.forEach((name, classes) -> {
            if (classes.stream().anyMatch(type -> type.getEnclosingClass() == null)) {
                declared.add(name);
            }
        });
        // A nested type, such as FieldClassification.Basis, only where some sample imports it, as a page does once.
        Map<String, Class<?>> topLevel = new HashMap<>();
        types.values().stream().flatMap(List::stream).filter(type -> type.getEnclosingClass() == null)
                .forEach(type -> topLevel.put(type.getName(), type));
        for (Sample sample : samples) {
            Matcher imported = NESTED_IMPORT.matcher(sample.text());
            while (imported.find()) {
                Class<?> outer = topLevel.get(imported.group(1));
                if (outer != null) {
                    java.util.Arrays.stream(outer.getClasses())
                            .filter(nested -> imported.group(2).equals("*")
                                    || nested.getSimpleName().equals(imported.group(2)))
                            .forEach(nested -> declared.add(nested.getSimpleName()));
                }
            }
        }
        Matcher context = DECLARED_TYPE.matcher(CONTEXT);
        while (context.find()) {
            declared.add(context.group(2));
        }
        for (Sample sample : samples) {
            Matcher type = DECLARED_TYPE.matcher(sample.text());
            while (type.find()) {
                declared.add(type.group(2));
            }
        }
        return declared;
    }

    /**
     * Whether a missing name is a type no sample declares: a type of the library that was renamed or removed, since
     * the samples import every package of the library. A capitalized name is a type even where javac reads it as a
     * variable, as the qualifier of a static call.
     */
    private static boolean undeclaredType(String code, String arguments, Set<String> declared) {
        Matcher missing = MISSING_TYPE.matcher(arguments);
        if (code.startsWith("compiler.err.cant.resolve") && missing.lookingAt()) {
            return !declared.contains(missing.group(2));
        }
        // OldName.Nested: javac reports the package OldName that does not exist.
        return code.equals("compiler.err.doesnt.exist") && arguments.matches("[A-Z][A-Z0-9_]*+[a-z]\\w*+")
                && !declared.contains(arguments);
    }

    /** Checks a module declaration: the library's own against module-info.java, others' references to it. */
    private static List<String> checkModule(Sample sample) throws IOException {
        Matcher module = MODULE.matcher(sample.text().replaceAll("//[^\n]*", "").strip());
        assertTrue(module.find());
        Set<String> directives = directives(sample.text());
        List<String> failures = new ArrayList<>();
        if (module.group(2).equals(API)) {
            Set<String> actual = directives(Files.readString(Path.of("src/main/java/module-info.java")));
            if (!actual.equals(directives)) {
                Set<String> missing = new TreeSet<>(actual);
                missing.removeAll(directives);
                Set<String> extra = new TreeSet<>(directives);
                extra.removeAll(actual);
                failures.add(sample.at(0) + ": differs from module-info.java, missing " + missing + ", extra "
                        + extra);
            }
            return failures;
        }
        for (String directive : directives) {
            String[] words = directive.split(" ");
            String last = words[words.length - 1];
            if (words[0].equals("requires") && last.startsWith(API) && !last.equals(API)) {
                failures.add(sample.at(0) + ": no module " + last);
            }
            if ((words[0].equals("uses") || words[0].equals("provides")) && words[1].startsWith(API)) {
                try {
                    Class.forName(words[1]);
                } catch (ClassNotFoundException e) {
                    failures.add(sample.at(0) + ": no service type " + words[1]);
                }
            }
        }
        return failures;
    }

    private static Set<String> directives(String declaration) {
        String code = declaration.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
        Set<String> directives = new TreeSet<>();
        Matcher matcher = DIRECTIVE.matcher(code);
        while (matcher.find()) {
            directives.add(matcher.group(1) + " " + String.join(" ", matcher.group(2).strip().split("\\s+")));
        }
        return directives;
    }

    /**
     * Checks the signature of a type as the reference shows it: the type exists with that kind, and a record has
     * those components, in that order, with those types.
     */
    private static List<String> checkSignature(Sample sample, Map<String, List<Class<?>>> types) {
        String code = sample.text().replaceAll("//[^\n]*", "").replaceAll("\\s+", " ").strip();
        Matcher signature = TYPE.matcher(code);
        assertTrue(signature.find());
        String kind = signature.group("kind");
        String name = signature.group("name");
        List<Class<?>> candidates = types.getOrDefault(name, List.of());
        if (candidates.isEmpty()) {
            return List.of(sample.at(0) + ": the library has no public type " + name);
        }
        List<String> problems = new ArrayList<>();
        for (Class<?> type : candidates) {
            problems = new ArrayList<>();
            if (!kind.equals(kindOf(type))) {
                problems.add(sample.at(0) + ": " + type.getName() + " is " + kindOf(type) + ", not " + kind);
            } else if (type.isRecord()) {
                int open = code.indexOf('(', signature.end());
                List<String> documented = components(code.substring(open + 1, closing(code, open)));
                List<String> actual = Arrays.stream(type.getRecordComponents())
                        .map(c -> c.getType().getSimpleName() + " " + c.getName()).toList();
                if (!documented.equals(actual)) {
                    problems.add(sample.at(0) + ": the components of " + type.getName() + " are " + actual
                            + ", not " + documented);
                }
            }
            if (problems.isEmpty()) {
                return problems;
            }
        }
        return problems;
    }

    private static String kindOf(Class<?> type) {
        if (type.isAnnotation()) {
            return "@interface";
        }
        if (type.isInterface()) {
            return "interface";
        }
        return type.isEnum() ? "enum" : type.isRecord() ? "record" : "class";
    }

    private static int closing(String code, int open) {
        int depth = 0;
        for (int i = open; i < code.length(); i++) {
            depth += code.charAt(i) == '(' ? 1 : code.charAt(i) == ')' ? -1 : 0;
            if (depth == 0) {
                return i;
            }
        }
        return code.length();
    }

    /** The record components of a signature as {@code SimpleType name}, generics and qualifiers removed. */
    private static List<String> components(String declaration) {
        List<String> components = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i <= declaration.length(); i++) {
            char c = i < declaration.length() ? declaration.charAt(i) : ',';
            depth += c == '<' ? 1 : c == '>' ? -1 : 0;
            if (c == ',' && depth == 0) {
                String component = declaration.substring(start, i).replaceAll("@\\w+(\\([^)]*\\))?", "").strip();
                if (!component.isEmpty()) {
                    String erased = component;
                    while (erased.contains("<")) {
                        erased = erased.replaceAll("<[^<>]*>", "");
                    }
                    int space = erased.lastIndexOf(' ');
                    String type = erased.substring(0, space).strip().replace("...", "[]");
                    components.add(type.substring(type.lastIndexOf('.') + 1) + " " + erased.substring(space + 1));
                }
                start = i + 1;
            }
        }
        return components;
    }

    /** The public types of the library's exported packages, nested ones included, by simple name. */
    private static Map<String, List<Class<?>>> libraryTypes() throws IOException, URISyntaxException {
        Path classes = Path.of(location(Taxonomy.class));
        Set<String> exported = Taxonomy.class.getModule().isNamed()
                ? Taxonomy.class.getModule().getDescriptor().exports().stream()
                        .map(e -> e.source()).collect(Collectors.toSet())
                : null;
        Map<String, List<Class<?>>> types = new HashMap<>();
        try (Stream<Path> walk = Files.walk(classes)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".class")).toList()) {
                String name = classes.relativize(file).toString().replace(File.separatorChar, '.')
                        .replaceAll("\\.class$", "");
                if (name.endsWith("module-info") || ANONYMOUS.matcher(name).find()) {
                    continue;
                }
                Class<?> type;
                try {
                    type = Class.forName(name, false, Taxonomy.class.getClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new AssertionError(e);
                }
                boolean visible = Modifier.isPublic(type.getModifiers())
                        && (exported == null ? !type.getPackageName().contains(".internal")
                                : exported.contains(type.getPackageName()));
                if (visible) {
                    types.computeIfAbsent(type.getSimpleName(), k -> new ArrayList<>()).add(type);
                }
            }
        }
        return types;
    }

    private static String location(Class<?> type) throws URISyntaxException {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    }
}
