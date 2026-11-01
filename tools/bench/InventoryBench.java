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

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.TaxonomyDiff;
import io.github.devbpm.linneo.classify.Classifier;
import io.github.devbpm.linneo.classify.DataInventory;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.InventoryDiff;
import io.github.devbpm.linneo.classify.InventoryField;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Measures the classification and the data inventory of a large synthetic model: by default 800 entities, 40 of
 * them with 400 fields and the rest with 25, all extending an audited superclass. It generates and compiles the
 * entities, so it needs a JDK and no other dependency. Not part of the build or of CI, since timings vary.
 * <pre>
 * ./mvnw -q compile
 * java -cp target/classes tools/bench/InventoryBench.java [entities] [heavy entities]
 * </pre>
 */
public class InventoryBench {

    private static final String[] PERSONAL = {Categories.USER_CONTACT_EMAIL, Categories.USER_NAME,
            Categories.USER_FINANCIAL_BANK_ACCOUNT, Categories.USER_DEMOGRAPHIC_DATE_OF_BIRTH};

    public static void main(String[] args) throws Exception {
        int entities = args.length > 0 ? Integer.parseInt(args[0]) : 800;
        int heavy = args.length > 1 ? Integer.parseInt(args[1]) : 40;
        Path dir = Files.createTempDirectory("linneo-bench");
        List<Path> sources = generate(dir, entities, heavy);
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        List<String> options = new ArrayList<>(List.of("-proc:none", "-d", dir.toString(), "-cp",
                System.getProperty("java.class.path")));
        sources.forEach(source -> options.add(source.toString()));
        if (javac.run(null, null, null, options.toArray(String[]::new)) != 0) {
            throw new IllegalStateException("The generated entities do not compile");
        }
        try (URLClassLoader loader = new URLClassLoader(new URL[]{dir.toUri().toURL()},
                InventoryBench.class.getClassLoader())) {
            List<Class<?>> types = new ArrayList<>();
            for (int e = 0; e < entities; e++) {
                types.add(loader.loadClass("bench.E" + e));
            }
            run(types);
        }
    }

    private static void run(List<Class<?>> types) {
        Taxonomy taxonomy = time("taxonomy", Taxonomy::standard);
        Classifier classifier = new Classifier(taxonomy);
        int fields = time("verify (first)", () -> classifier.verify(types)).stream()
                .mapToInt(c -> c.fields().size()).sum();
        time("verify (again)", () -> classifier.verify(types));
        DataInventory declared = time("inventory", () -> classifier.inventory(types, Class::getSimpleName));
        String fingerprint = time("fingerprint", declared::fingerprint);
        String text = time("toTsv", declared::toTsv);
        DataInventory read = time("fromTsv", () -> DataInventory.fromTsv(text));
        DataInventory.Builder stored = DataInventory.builder();
        int i = 0;
        for (InventoryField field : read.fields()) {
            stored.field(i++ % 100 == 0 && !field.isExempt()
                    ? new InventoryField(field.object(), field.path(), Categories.USER_NAME, Basis.DECLARED, null,
                    null, null, null) : field);
        }
        DataInventory before = stored.build();
        InventoryDiff diff = time("diff (1% changed)", () -> InventoryDiff.between(before, declared, taxonomy));
        Taxonomy tightened = Taxonomy.builder().withStandard().refineCategory(Categories.USER_CONTACT_EMAIL,
                c -> c.confidentiality(Confidentiality.HIGH)).build();
        int affected = time("affectedBy (email tightened)",
                () -> declared.affectedBy(TaxonomyDiff.between(taxonomy, tightened))).values().stream()
                .mapToInt(List::size).sum();
        System.out.printf("%n%d entities, %d classified fields, %d KB of text, fingerprint %s...%n", types.size(),
                fields, text.length() / 1024, fingerprint.substring(0, 12));
        System.out.printf("%d field changes, %d weakening; %d fields reached by the taxonomy change%n",
                diff.changes().size(), diff.weakening().size(), affected);
    }

    private static <T> T time(String step, Supplier<T> work) {
        long start = System.nanoTime();
        T result = work.get();
        System.out.printf("%-30s %8.1f ms%n", step, (System.nanoTime() - start) / 1e6);
        return result;
    }

    private static List<Path> generate(Path dir, int entities, int heavy) throws Exception {
        Random random = new Random(7);
        String header = "package bench;\nimport io.github.devbpm.linneo.annotation.*;\n"
                + "import io.github.devbpm.linneo.Confidentiality;\n";
        List<Path> sources = new ArrayList<>();
        Path pkg = Files.createDirectories(dir.resolve("bench"));
        sources.add(Files.writeString(pkg.resolve("Audited.java"), header
                + "@DefaultCategory(value = \"system.operations\", reason = \"audit columns\")\n"
                + "public abstract class Audited {\n private java.time.Instant createdAt;\n"
                + " private java.time.Instant updatedAt;\n private Long version;\n"
                + " @Category(\"user.unique_id\") private Long createdBy;\n"
                + " @Category(\"user.unique_id\") private Long updatedBy;\n}\n"));
        for (int e = 0; e < entities; e++) {
            int count = e < heavy ? 400 : 25;
            StringBuilder body = new StringBuilder();
            for (int f = 0; f < count; f++) {
                double r = random.nextDouble();
                if (r < 0.5) {
                    body.append(" private String f").append(f).append(";\n");
                } else if (r < 0.9) {
                    body.append(" @Category(\"").append(PERSONAL[random.nextInt(PERSONAL.length)])
                            .append("\") private String f").append(f).append(";\n");
                } else if (r < 0.97) {
                    body.append(" @Category(value = \"user.contact.email\", confidentiality = Confidentiality.HIGH)")
                            .append(" private String f").append(f).append(";\n");
                } else {
                    body.append(" @CategoryByRow(declaredBy = \"type\", ceiling = \"user.health_and_medical\")")
                            .append(" private byte[] f").append(f).append(";\n");
                }
            }
            sources.add(Files.writeString(pkg.resolve("E" + e + ".java"), header
                    + "@DefaultCategory(value = \"system.operations\", reason = \"technical\")\n"
                    + "public class E" + e + " extends Audited {\n" + body + "}\n"));
        }
        return sources;
    }
}
