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
package io.github.devbpm.linneo.classify;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.util.stream.Stream;

/**
 * Finds the classes of a model, so that a test verifies every class of the model rather than the ones someone
 * remembered to list: a class added to the model without a classification then fails the build, as it should.
 * <pre>{@code
 * new Classifier(Taxonomy.load()).verify(ModelClasses.inPackageTree(Student.class));
 * }</pre>
 * The classes are read from where an <em>anchor</em> class was loaded from, a directory or a JAR file, on the class
 * path or the module path, so classes of the same package elsewhere are not included. Classes and records are
 * returned, nested and abstract ones included; interfaces, annotations, enums, anonymous and local classes are left
 * out. They are loaded without being initialized and sorted by name, so the result is the same on every run. Filter
 * the list further when the package also holds classes that are not data, such as repositories.
 * <p>
 * A location that cannot be listed, such as a JAR nested in another, classes generated at run time or a source file
 * run by the {@code java} launcher, and a class of
 * the package that cannot be loaded, for instance because an optional dependency is missing, are reported with an
 * {@link IllegalArgumentException} rather than skipped, so that no class of the model goes unverified: pass the
 * classes explicitly then.
 *
 * @since 0.1.0
 */
public final class ModelClasses {

    private static final String SUFFIX = ".class";

    private ModelClasses() {
    }

    /**
     * The classes of the anchor's package.
     *
     * @param anchor a class of the package, such as one of its entities
     * @return the classes, sorted by name
     * @throws IllegalArgumentException if the anchor is in the unnamed package, its location cannot be listed, or a
     *         class of the package cannot be loaded
     * @throws UncheckedIOException if the location cannot be read
     */
    public static List<Class<?>> inPackage(Class<?> anchor) {
        return find(anchor, false);
    }

    /**
     * The classes of the anchor's package and of the packages below it.
     *
     * @param anchor a class of the top package, such as one of its entities
     * @return the classes, sorted by name
     * @throws IllegalArgumentException if the anchor is in the unnamed package, its location cannot be listed, or a
     *         class of the packages cannot be loaded
     * @throws UncheckedIOException if the location cannot be read
     */
    public static List<Class<?>> inPackageTree(Class<?> anchor) {
        return find(anchor, true);
    }

    private static List<Class<?>> find(Class<?> anchor, boolean below) {
        Objects.requireNonNull(anchor, "anchor");
        if (anchor.getPackageName().isEmpty()) {
            throw new IllegalArgumentException(anchor.getName() + " is in the unnamed package, which cannot be listed");
        }
        String prefix = anchor.getPackageName().replace('.', '/') + "/";
        Path location = location(anchor);
        List<String> entries;
        try {
            entries = Files.isDirectory(location) ? inDirectory(location, prefix, below)
                    : inJar(location, prefix, below);
        } catch (ZipException e) {
            throw new IllegalArgumentException("Cannot list classes from " + location + ", where " + anchor.getName()
                    + " was loaded from: it is neither a directory nor a JAR file; pass the classes explicitly", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list the classes of " + location, e);
        }
        List<Class<?>> classes = new ArrayList<>();
        for (String entry : entries) {
            String name = entry.substring(0, entry.length() - SUFFIX.length()).replace('/', '.');
            if (name.endsWith(".package-info") || name.endsWith(".module-info")) {
                continue;
            }
            Class<?> type;
            try {
                type = Class.forName(name, false, anchor.getClassLoader());
            } catch (ClassNotFoundException | LinkageError e) {
                throw new IllegalArgumentException("Cannot load " + name + " from " + location, e);
            }
            if (!type.isInterface() && !type.isEnum() && !type.isAnonymousClass() && !type.isLocalClass()
                    && !type.isSynthetic()) {
                classes.add(type);
            }
        }
        classes.sort(Comparator.comparing(Class::getName));
        return List.copyOf(classes);
    }

    private static List<String> inDirectory(Path root, String prefix, boolean below) throws IOException {
        Path directory = root.resolve(prefix);
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = below ? Files.walk(directory) : Files.list(directory)) {
            return files.filter(file -> Files.isRegularFile(file) && file.toString().endsWith(SUFFIX))
                    .map(file -> root.relativize(file).toString().replace(File.separatorChar, '/'))
                    .toList();
        }
    }

    private static List<String> inJar(Path jar, String prefix, boolean below) throws IOException {
        try (JarFile file = new JarFile(jar.toFile())) {
            return file.stream().map(JarEntry::getName)
                    .filter(name -> name.startsWith(prefix) && name.endsWith(SUFFIX))
                    .filter(name -> below || name.indexOf('/', prefix.length()) < 0)
                    .toList();
        }
    }

    private static Path location(Class<?> anchor) {
        CodeSource source = anchor.getProtectionDomain().getCodeSource();
        URL url = source == null ? null : source.getLocation();
        if (url == null) {
            throw new IllegalArgumentException(anchor.getName() + " has no location to list classes from; pass the"
                    + " classes explicitly");
        }
        Path location;
        try {
            location = Path.of(url.toURI());
        } catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException e) {
            throw new IllegalArgumentException("Cannot list classes from " + url + ", where " + anchor.getName()
                    + " was loaded from; pass the classes explicitly", e);
        }
        if (!Files.exists(location)) {
            throw new IllegalArgumentException("The location of " + anchor.getName() + " does not exist: " + url
                    + "; pass the classes explicitly");
        }
        return location;
    }
}
