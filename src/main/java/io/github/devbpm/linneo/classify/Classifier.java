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

import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.CategoryByRow;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.annotation.Exempt;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.Violation.Code;
import io.github.devbpm.linneo.classify.Violation.Severity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads the classification annotations of classes and checks them against a {@link Taxonomy}.
 * <p>
 * For each non-static, non-synthetic, non-{@code transient} field of a class and its superclasses, the category
 * is, in order of precedence:
 * <ol>
 *   <li>the field's {@link Category @Category}, or the ceiling of its {@link CategoryByRow @CategoryByRow};</li>
 *   <li>the {@link DefaultCategory @DefaultCategory} of the class that declares the field;</li>
 *   <li>the {@link DefaultCategory @DefaultCategory} of the class being classified;</li>
 *   <li>none: an {@link Code#UNCLASSIFIED_FIELD} error.</li>
 * </ol>
 * Fields marked {@link Exempt @Exempt} are skipped. A deprecated key still classifies the field, with its
 * replacement, and is reported as a warning. The classifier only reads annotations, never field values, so it needs
 * no access to the classes beyond what {@link Class#getDeclaredFields()} requires.
 * <p>
 * Typical use, in a test of the application:
 * <pre>{@code
 * new Classifier(Taxonomy.load()).verify(List.of(Customer.class, Invoice.class, MenuGroup.class));
 * }</pre>
 * Instances are immutable and thread-safe.
 *
 * @since 0.1.0
 */
public final class Classifier {

    private final Taxonomy taxonomy;

    /**
     * Creates a classifier for a taxonomy.
     *
     * @param taxonomy the taxonomy the keys must belong to
     */
    public Classifier(Taxonomy taxonomy) {
        this.taxonomy = Objects.requireNonNull(taxonomy, "taxonomy");
    }

    /**
     * Classifies one class.
     *
     * @param type the class
     * @return its field classifications and the problems found
     */
    public ClassClassification classify(Class<?> type) {
        Objects.requireNonNull(type, "type");
        List<FieldClassification> fields = new ArrayList<>();
        List<Violation> violations = new ArrayList<>();
        List<Class<?>> hierarchy = hierarchyOf(type);
        for (Class<?> declaring : hierarchy) {
            checkDefault(type, declaring, violations);
        }
        for (Class<?> declaring : hierarchy) {
            for (Field field : declaring.getDeclaredFields()) {
                if (isExcluded(field)) {
                    continue;
                }
                classifyField(type, field, violations).ifPresent(fields::add);
            }
        }
        return new ClassClassification(type, fields, violations);
    }

    /**
     * Classifies several classes.
     *
     * @param types the classes
     * @return one classification per class, in the given order
     */
    public List<ClassClassification> classifyAll(Collection<? extends Class<?>> types) {
        return types.stream().map(this::classify).toList();
    }

    /**
     * Classifies several classes and fails if any of them has an error.
     *
     * @param types the classes, typically every entity of an application
     * @return the classifications, when there is no error
     * @throws ClassificationException listing every error found in any of the classes
     */
    public List<ClassClassification> verify(Collection<? extends Class<?>> types) {
        List<ClassClassification> classifications = classifyAll(types);
        List<Violation> errors = classifications.stream().flatMap(c -> c.errors().stream()).toList();
        if (!errors.isEmpty()) {
            throw new ClassificationException(errors);
        }
        return classifications;
    }

    private Optional<FieldClassification> classifyField(Class<?> type, Field field, List<Violation> violations) {
        Category category = field.getDeclaredAnnotation(Category.class);
        CategoryByRow byRow = field.getDeclaredAnnotation(CategoryByRow.class);
        Exempt exempt = field.getDeclaredAnnotation(Exempt.class);
        String name = field.getName();
        if (exempt != null) {
            if (category != null || byRow != null) {
                violations.add(error(type, name, Code.CONFLICTING_ANNOTATIONS,
                        "@Exempt cannot be combined with @Category or @CategoryByRow"));
            }
            if (exempt.value().isBlank()) {
                violations.add(error(type, name, Code.MISSING_REASON, "@Exempt needs a reason"));
            }
            return Optional.empty();
        }
        if (category != null && byRow != null) {
            violations.add(error(type, name, Code.CONFLICTING_ANNOTATIONS,
                    "@Category and @CategoryByRow cannot be used together"));
            return Optional.empty();
        }
        if (category != null) {
            return declared(type, field, category, violations);
        }
        if (byRow != null) {
            return perRow(type, field, byRow, violations);
        }
        return byDefault(type, field, violations);
    }

    private Optional<FieldClassification> declared(Class<?> type, Field field, Category annotation,
                                                   List<Violation> violations) {
        String name = field.getName();
        Optional<DataCategory> resolved = resolve(type, name, annotation.value(), violations);
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        DataCategory category = resolved.get();
        int before = violations.size();
        Confidentiality confidentiality = single(type, name, "confidentiality", annotation.confidentiality(),
                category.confidentiality(), violations);
        Integrity integrity = single(type, name, "integrity", annotation.integrity(), category.integrity(),
                violations);
        if (confidentiality.compareTo(category.confidentiality()) < 0) {
            violations.add(error(type, name, Code.LOWERED_LEVEL, "confidentiality " + confidentiality
                    + " is lower than the " + category.confidentiality() + " of '" + category.key() + "'"));
        }
        if (integrity.compareTo(category.integrity()) < 0) {
            violations.add(error(type, name, Code.LOWERED_LEVEL, "integrity " + integrity
                    + " is lower than the " + category.integrity() + " of '" + category.key() + "'"));
        }
        Identifiability identifiability = null;
        if (category.isPersonal()) {
            identifiability = single(type, name, "identifiability", annotation.identifiability(),
                    Identifiability.IDENTIFIED, violations);
        } else if (annotation.identifiability().length > 0) {
            violations.add(error(type, name, Code.IDENTIFIABILITY_NOT_APPLICABLE,
                    "'" + category.key() + "' is not personal data and has no identifiability"));
        }
        if (hasNewErrors(violations, before)) {
            return Optional.empty();
        }
        return Optional.of(new FieldClassification(field.getDeclaringClass(), name, category, Basis.DECLARED, null,
                confidentiality, integrity, identifiability));
    }

    private Optional<FieldClassification> perRow(Class<?> type, Field field, CategoryByRow annotation,
                                                 List<Violation> violations) {
        String name = field.getName();
        if (annotation.declaredBy().isBlank()) {
            violations.add(error(type, name, Code.MISSING_REASON, "@CategoryByRow needs declaredBy"));
        }
        return resolve(type, name, annotation.ceiling(), violations).map(category ->
                new FieldClassification(field.getDeclaringClass(), name, category, Basis.BY_ROW,
                        annotation.declaredBy(), category.confidentiality(), category.integrity(),
                        category.isPersonal() ? Identifiability.IDENTIFIED : null));
    }

    private Optional<FieldClassification> byDefault(Class<?> type, Field field, List<Violation> violations) {
        Class<?> declaring = field.getDeclaringClass();
        DefaultCategory annotation = declaring.getDeclaredAnnotation(DefaultCategory.class);
        if (annotation == null) {
            annotation = type.getDeclaredAnnotation(DefaultCategory.class);
        }
        if (annotation == null) {
            violations.add(error(type, field.getName(), Code.UNCLASSIFIED_FIELD,
                    "no @Category on the field and no @DefaultCategory on " + declaring.getSimpleName()
                            + (declaring == type ? "" : " or " + type.getSimpleName())));
            return Optional.empty();
        }
        DefaultCategory found = annotation;
        // The default itself was checked by checkDefault; an invalid one leaves the field unclassified silently.
        return taxonomy.category(found.value())
                .map(c -> taxonomy.resolveCategory(c.key()))
                .filter(c -> !c.isPersonal())
                .map(c -> new FieldClassification(declaring, field.getName(), c, Basis.CLASS_DEFAULT, found.reason(),
                        c.confidentiality(), c.integrity(), null));
    }

    private void checkDefault(Class<?> type, Class<?> declaring, List<Violation> violations) {
        DefaultCategory annotation = declaring.getDeclaredAnnotation(DefaultCategory.class);
        if (annotation == null) {
            return;
        }
        String where = "@DefaultCategory of " + declaring.getSimpleName();
        if (annotation.reason().isBlank()) {
            violations.add(error(type, null, Code.MISSING_REASON, where + " needs a reason"));
        }
        resolve(type, null, annotation.value(), violations)
                .filter(DataCategory::isPersonal)
                .ifPresent(c -> violations.add(error(type, null, Code.PERSONAL_DEFAULT, where + " names '"
                        + c.key() + "', which is personal data: declare personal fields one by one with @Category")));
    }

    private Optional<DataCategory> resolve(Class<?> type, String field, String key, List<Violation> violations) {
        Optional<DataCategory> category = taxonomy.category(key);
        if (category.isEmpty()) {
            violations.add(error(type, field, Code.UNKNOWN_CATEGORY, "unknown data category '" + key + "'"));
            return Optional.empty();
        }
        if (category.get().info().isDeprecated()) {
            DataCategory current = taxonomy.resolveCategory(key);
            violations.add(new Violation(type, field, Severity.WARNING, Code.DEPRECATED_CATEGORY,
                    "data category '" + key + "' is deprecated"
                            + (current.key().equals(key) ? "" : "; use '" + current.key() + "'")));
            return Optional.of(current);
        }
        return category;
    }

    private static <T> T single(Class<?> type, String field, String element, T[] values, T fallback,
                                List<Violation> violations) {
        if (values.length > 1) {
            violations.add(error(type, field, Code.AMBIGUOUS_VALUE, element + " must have at most one value"));
        }
        return values.length == 0 ? fallback : values[0];
    }

    private static boolean hasNewErrors(List<Violation> violations, int from) {
        return violations.subList(from, violations.size()).stream().anyMatch(Violation::isError);
    }

    private static boolean isExcluded(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic();
    }

    private static List<Class<?>> hierarchyOf(Class<?> type) {
        List<Class<?>> hierarchy = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class && c != Record.class && c != Enum.class;
             c = c.getSuperclass()) {
            hierarchy.add(c);
        }
        Collections.reverse(hierarchy);
        return hierarchy;
    }

    private static Violation error(Class<?> type, String field, Code code, String message) {
        return new Violation(type, field, Severity.ERROR, code, message);
    }
}
