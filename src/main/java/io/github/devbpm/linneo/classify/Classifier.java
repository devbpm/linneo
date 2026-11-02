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

import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.DataCategory;
import io.github.devbpm.linneo.Identifiability;
import io.github.devbpm.linneo.Integrity;
import io.github.devbpm.linneo.LegalNature;
import io.github.devbpm.linneo.Protection;
import io.github.devbpm.linneo.Taxonomy;
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.annotation.CategoryByRow;
import io.github.devbpm.linneo.annotation.Composite;
import io.github.devbpm.linneo.annotation.DefaultCategory;
import io.github.devbpm.linneo.annotation.DerivedFrom;
import io.github.devbpm.linneo.annotation.Exempt;
import io.github.devbpm.linneo.classify.ClassClassification.Exemption;
import io.github.devbpm.linneo.classify.FieldClassification.Basis;
import io.github.devbpm.linneo.classify.Violation.Code;
import io.github.devbpm.linneo.classify.Violation.Severity;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Reads the classification annotations of classes and checks them against a {@link Taxonomy}.
 * <p>
 * For each non-static, non-synthetic field of a class and its superclasses, the category is, in order of precedence:
 * <ol>
 *   <li>the field's {@link Category @Category}, or the ceiling of its {@link CategoryByRow @CategoryByRow};</li>
 *   <li>for a field marked {@link DerivedFrom @DerivedFrom} without a category, the classification of its
 *       source;</li>
 *   <li>for a field whose type, or the element type of whose collection, array, {@link Optional} or map values, at
 *       any depth, is a {@link Composite @Composite} class, the classification of that class's fields, one entry per
 *       field with the path {@code field.part};</li>
 *   <li>the {@link DefaultCategory @DefaultCategory} of the class that declares the field;</li>
 *   <li>the {@link DefaultCategory @DefaultCategory} of the class being classified;</li>
 *   <li>none: an {@link Code#UNCLASSIFIED_FIELD} error.</li>
 * </ol>
 * A class default never applies to a field that may hold data the classifier cannot see: a composite it cannot
 * split into fields (a map key, a type argument of a class that is not a collection, or two composites), or a value
 * typed {@code Object} or by a type variable that the class does not bind. Such a field without a category is an
 * {@link Code#UNCLASSIFIED_FIELD} error, so a composite never takes a non-personal default by being wrapped. Type
 * variables are resolved through the superclasses of the class being classified, and a subclass of a composite is a
 * composite. A {@code transient} field is not classified unless it carries one of Linneo's field annotations.
 * Fields marked {@link Exempt @Exempt} are not classified, and {@link ClassClassification#exempted()} lists them.
 * A field is identified by its path, so a field that hides a superclass field of the same name is an
 * {@link Code#DUPLICATE_FIELD} error. A deprecated key still classifies the field, with its replacement, and is
 * reported as a warning. The classifier only reads annotations, never field values, so it needs no access to the
 * classes beyond what {@link Class#getDeclaredFields()} requires.
 * <p>
 * Protection never drops silently: a derived field must protect at least as much as each of its sources
 * ({@link Code#WEAKER_THAN_SOURCE}); a value declared anonymized or aggregated may drop the personal nature of its
 * sources, never their levels or secrecy. A field that stores composites as one value, declared or derived, must
 * protect at least as much as everything the composites contain ({@link Code#WEAKER_THAN_PARTS}). A composite that contains itself, and derived fields that depend on each other,
 * in one class or across classes, are a {@link Code#CYCLE}; a field that derives from a field with an error is an
 * {@link Code#UNKNOWN_SOURCE}. Sources are resolved field by field, so classes may derive from each other's other
 * fields and a composite from a class that holds it, and the result does not depend on the order in which classes
 * are classified.
 * <p>
 * Fields that bytecode tools generate are ignored: synthetic fields and fields whose name starts with {@code $$_}, as
 * Hibernate's enhancement adds. A Hibernate proxy class ({@code Entity$HibernateProxy$...}) is classified as the
 * entity it extends.
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

    // Prefix of the fields that bytecode tools add to classes, such as Hibernate's $$_hibernate_interceptor or
    // $$_hibernate_entityEntryHolder; source code does not use it.
    private static final String GENERATED_FIELD_PREFIX = "$$_";
    private static final String HIBERNATE_PROXY = "$HibernateProxy$";

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
        return new Run().unit(unproxied(Objects.requireNonNull(type, "type"))).classification();
    }

    /**
     * Classifies several classes. A class used by several of them, as a composite or as the source of derived
     * fields, is read once.
     *
     * @param types the classes
     * @return one classification per class, in the given order
     */
    public List<ClassClassification> classifyAll(Collection<? extends Class<?>> types) {
        Run run = new Run();
        return types.stream().map(type -> run.unit(unproxied(Objects.requireNonNull(type, "type")))
                .classification()).toList();
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

    /**
     * The inventory of a model: every field of the classes, with what the code declares for it, under object keys the
     * application chooses. The keys must be stable, since stored records and catalogs are matched by them: a key
     * that changes when a class is renamed or moved turns every field of the class into a removed field and a new
     * one. Take them from what already identifies the class in the application, such as its business object or
     * table name, not from the class name.
     *
     * @param types the classes, typically every entity; a class listed twice counts once
     * @param objectKey the key of the object each class holds
     * @return the inventory
     * @throws ClassificationException if a class has an error
     * @throws IllegalArgumentException if a key is null or blank, or two classes have the same key
     */
    public DataInventory inventory(Collection<? extends Class<?>> types, Function<? super Class<?>, String> objectKey) {
        Objects.requireNonNull(objectKey, "objectKey");
        Run run = new Run();
        List<Unit> units = new LinkedHashSet<>(types).stream()
                .map(type -> run.unit(unproxied(Objects.requireNonNull(type, "type")))).distinct().toList();
        List<Violation> errors = units.stream().flatMap(unit -> unit.classification().errors().stream()).toList();
        if (!errors.isEmpty()) {
            throw new ClassificationException(errors);
        }
        DataInventory.Builder inventory = DataInventory.builder();
        Map<String, Class<?>> keys = new HashMap<>();
        for (Unit unit : units) {
            Class<?> type = unit.classification().type();
            String key = objectKey.apply(type);
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("No object key for " + type.getName());
            }
            Class<?> other = keys.putIfAbsent(key, type);
            if (other != null) {
                throw new IllegalArgumentException(other.getName() + " and " + type.getName()
                        + " have the same object key '" + key + "'");
            }
            inventory.object(key);
            for (FieldClassification field : unit.classification().fields()) {
                Declared declared = unit.declared().get(field.path());
                inventory.field(new InventoryField(key, field.path(), declared.key(), field.basis(),
                        declared.confidentiality(), declared.integrity(), declared.identifiability(),
                        field.reason()));
            }
            for (Exemption exemption : unit.classification().exempted()) {
                inventory.exempt(key, exemption.path(), exemption.reason());
            }
        }
        return inventory.build();
    }

    /**
     * What the code declares for a field before the taxonomy resolves it: the key as written, the levels the field
     * raises explicitly ({@code null} when it takes the category's) and the identifiability it declares
     * ({@code null} when it declares none), so that an inventory never depends on the taxonomy it was built with.
     */
    record Declared(String key, Confidentiality confidentiality, Integrity integrity,
                    Identifiability identifiability) {
    }

    /**
     * The classification of a class, with what each classified field declares, by path.
     */
    record Unit(ClassClassification classification, Map<String, Declared> declared) {
    }

    /**
     * One classification request. Each class is read once, in two steps: its fields are collected (derived fields
     * wait), and a derived field is resolved when something needs it, so a source in another class resolves only the
     * fields it depends on. The derived fields being resolved, across classes, form one depth-first search, in which
     * Tarjan's algorithm finds the fields that are in a cycle.
     */
    final class Run {

        private final Map<Class<?>, ClassRun> runs = new HashMap<>();
        // The derived fields being resolved, innermost last.
        private final List<Slot> derivations = new ArrayList<>();
        // The fields of the search not yet assigned to a strongly connected component.
        private final List<Slot> component = new ArrayList<>();
        private int counter;

        /**
         * The run of a class with all its fields collected.
         *
         * @param type the class, not a proxy
         * @return the run, or {@code null} while the class is still collecting its fields: it holds the class that
         *         asks, through composites
         */
        ClassRun collected(Class<?> type) {
            ClassRun classRun = runs.get(type);
            if (classRun == null) {
                classRun = new ClassRun(this, type);
                runs.put(type, classRun);
                classRun.collect();
            }
            return classRun.collected ? classRun : null;
        }

        /**
         * The unit of a class, every derived field resolved. Only a request asks for it, never a class, so no class
         * is collecting its fields or finishing then.
         *
         * @param type the class, not a proxy
         * @return the unit
         */
        Unit unit(Class<?> type) {
            return collectedNow(type).finish();
        }

        // The run of a class outside any collection: collecting completes before anything is derived or checked.
        ClassRun collectedNow(Class<?> type) {
            ClassRun classRun = collected(type);
            if (classRun == null) {
                throw new IllegalStateException(type.getName() + " is collecting its fields during a derivation");
            }
            return classRun;
        }
    }

    private enum State { PENDING, VISITING, DONE }

    /**
     * A classified field; a derived field waiting for its sources; or a part of an embedded composite that is still
     * to be derived, linked to its slot in the composite's run.
     */
    private static final class Slot {
        private final Field field;
        private final ClassRun link;
        private final String linkPath;
        private State state;
        private FieldClassification result;
        private Declared declared;
        private boolean inCycle;
        // Tarjan's bookkeeping for a derived field: order of discovery, lowest discovery order it reaches, and
        // whether it still waits on the component stack.
        private int index;
        private int low;
        private boolean onComponentStack;

        private Slot(Field field, ClassRun link, String linkPath, State state, FieldClassification result,
                     Declared declared) {
            this.field = field;
            this.link = link;
            this.linkPath = linkPath;
            this.state = state;
            this.result = result;
            this.declared = declared;
        }

        static Slot done(FieldClassification result, Declared declared) {
            return new Slot(null, null, null, State.DONE, result, declared);
        }

        static Slot pending(Field field) {
            return new Slot(field, null, null, State.PENDING, null, null);
        }

        static Slot linked(ClassRun composite, String path) {
            return new Slot(null, composite, path, State.PENDING, null, null);
        }
    }

    private record Classified(FieldClassification classification, Declared declared) {
    }

    /** A composite embedded under a field: its problems join those of the class when the class is finished. */
    private record Embedded(String name, ClassRun run) {
    }

    /** A field that stores composites as one value, checked against the composites' fields at the end. */
    private record Whole(String name, Set<Class<?>> composites) {
    }

    /** The classification of one class. */
    private final class ClassRun {

        private final Run run;
        private final Class<?> type;
        private final Map<String, Slot> slots = new LinkedHashMap<>();
        private final List<Exemption> exempted = new ArrayList<>();
        private final List<Violation> violations = new ArrayList<>();
        private final List<Embedded> embedded = new ArrayList<>();
        private final List<Whole> wholes = new ArrayList<>();
        // The position of each field name, to report problems in the order of the fields.
        private final Map<String, Integer> positions = new HashMap<>();
        // Every field has a slot: another class may resolve a source here.
        private boolean collected;
        // Resolving every derived field, to build the unit.
        private boolean finishing;
        private Unit unit;

        // What the type variables of the superclasses stand for in this class.
        private final Map<TypeVariable<?>, Type> bindings;

        ClassRun(Run run, Class<?> type) {
            this.run = run;
            this.type = type;
            this.bindings = bindingsOf(type);
        }

        // Reads every field once; nothing is derived yet, so collecting never needs another class's derivations.
        void collect() {
            List<Class<?>> hierarchy = hierarchyOf(type);
            for (Class<?> declaring : hierarchy) {
                checkDefault(declaring);
            }
            Map<String, Class<?>> declaredBy = new HashMap<>();
            for (Class<?> declaring : hierarchy) {
                for (Field field : declaring.getDeclaredFields()) {
                    if (isExcluded(field)) {
                        continue;
                    }
                    positions.putIfAbsent(field.getName(), positions.size());
                    Class<?> earlier = declaredBy.putIfAbsent(field.getName(), declaring);
                    if (earlier != null) {
                        error(field.getName(), Code.DUPLICATE_FIELD, "hides the field of the same name declared by "
                                + earlier.getSimpleName() + "; rename one of them");
                        continue;
                    }
                    classifyField(field);
                }
            }
            collected = true;
        }

        // The unit, once every derived field is resolved, every composite stored as one value checked and the
        // problems of the embedded composites taken over; null while that is under way.
        // Embedded composites form no cycle (collecting reports one), and checking parts derives instead of finishing,
        // so a class is never finished while it is finishing.
        Unit finish() {
            if (unit != null) {
                return unit;
            }
            if (finishing) {
                throw new IllegalStateException(type.getName() + " is finished while it is finishing");
            }
            finishing = true;
            for (Map.Entry<String, Slot> entry : List.copyOf(slots.entrySet())) {
                if (entry.getValue().state != State.DONE) {
                    derive(entry.getKey());
                }
            }
            for (Whole whole : wholes) {
                checkParts(whole);
            }
            for (Embedded part : embedded) {
                Unit inner = part.run().finish();
                for (Violation violation : inner.classification().violations()) {
                    violations.add(new Violation(type, violation.path() == null ? part.name()
                            : part.name() + "." + violation.path(), violation.severity(), violation.code(),
                            violation.message()));
                }
            }
            // Problems in the order of the fields, whichever order the derivations of the run took.
            violations.sort(Comparator.comparingInt(violation -> position(violation.path())));
            List<FieldClassification> fields = new ArrayList<>();
            Map<String, Declared> declared = new HashMap<>();
            slots.forEach((path, slot) -> {
                if (slot.result != null) {
                    fields.add(slot.result);
                    declared.put(path, slot.declared);
                }
            });
            unit = new Unit(new ClassClassification(type, fields, exempted, violations),
                    Collections.unmodifiableMap(declared));
            finishing = false;
            return unit;
        }

        private int position(String path) {
            if (path == null) {
                return -1;
            }
            int dot = path.indexOf('.');
            return positions.getOrDefault(dot < 0 ? path : path.substring(0, dot), Integer.MAX_VALUE);
        }

        private void classifyField(Field field) {
            Category category = field.getDeclaredAnnotation(Category.class);
            CategoryByRow byRow = field.getDeclaredAnnotation(CategoryByRow.class);
            Exempt exempt = field.getDeclaredAnnotation(Exempt.class);
            DerivedFrom derived = field.getDeclaredAnnotation(DerivedFrom.class);
            String name = field.getName();
            if (exempt != null) {
                if (category != null || byRow != null || derived != null) {
                    error(name, Code.CONFLICTING_ANNOTATIONS,
                            "@Exempt cannot be combined with @Category, @CategoryByRow or @DerivedFrom");
                } else {
                    exempted.add(new Exemption(field.getDeclaringClass(), name, exempt.value()));
                }
                if (exempt.value().isBlank()) {
                    error(name, Code.MISSING_REASON, "@Exempt needs a reason");
                }
                return;
            }
            if (byRow != null && (category != null || derived != null)) {
                error(name, Code.CONFLICTING_ANNOTATIONS,
                        "@CategoryByRow cannot be combined with @Category or @DerivedFrom");
                return;
            }
            Holding holding = new Holding(bindings).of(field.getGenericType());
            if (derived != null) {
                slots.put(name, Slot.pending(field));
                stores(name, holding);
                return;
            }
            Optional<Classified> classified;
            if (category != null) {
                classified = declared(field, category);
            } else if (byRow != null) {
                classified = perRow(field, byRow);
            } else {
                Class<?> composite = holding.embeddable();
                if (composite != null) {
                    embed(name, composite);
                    return;
                }
                if (holding.hidesData()) {
                    error(name, Code.UNCLASSIFIED_FIELD, holding.explain() + ", so no class default applies;"
                            + " declare its category with @Category or @CategoryByRow");
                    return;
                }
                classified = byDefault(field);
            }
            classified.ifPresent(c -> slots.put(name, Slot.done(c.classification(), c.declared())));
            stores(name, holding);
        }

        // A field classified as one value whose type holds composites: it must protect what they contain.
        private void stores(String name, Holding holding) {
            if (!holding.composites.isEmpty()) {
                wholes.add(new Whole(name, holding.composites));
            }
        }

        private Optional<Classified> declared(Field field, Category annotation) {
            String name = field.getName();
            Optional<DataCategory> resolved = resolve(name, annotation.value());
            if (resolved.isEmpty()) {
                return Optional.empty();
            }
            DataCategory category = resolved.get();
            int before = violations.size();
            Confidentiality confidentiality = single(name, "confidentiality", annotation.confidentiality(),
                    category.confidentiality());
            Integrity integrity = single(name, "integrity", annotation.integrity(), category.integrity());
            if (confidentiality.compareTo(category.confidentiality()) < 0) {
                error(name, Code.LOWERED_LEVEL, "confidentiality " + confidentiality + " is lower than the "
                        + category.confidentiality() + " of '" + category.key() + "'");
            }
            if (integrity.compareTo(category.integrity()) < 0) {
                error(name, Code.LOWERED_LEVEL, "integrity " + integrity + " is lower than the "
                        + category.integrity() + " of '" + category.key() + "'");
            }
            Identifiability identifiability = null;
            if (category.isPersonal()) {
                identifiability = single(name, "identifiability", annotation.identifiability(),
                        Identifiability.IDENTIFIED);
            } else if (annotation.identifiability().length > 0) {
                error(name, Code.IDENTIFIABILITY_NOT_APPLICABLE,
                        "'" + category.key() + "' is not personal data and has no identifiability");
            }
            if (hasNewErrors(before)) {
                return Optional.empty();
            }
            Declared declared = new Declared(annotation.value(),
                    annotation.confidentiality().length == 0 ? null : confidentiality,
                    annotation.integrity().length == 0 ? null : integrity,
                    annotation.identifiability().length == 0 ? null : identifiability);
            return Optional.of(new Classified(new FieldClassification(field.getDeclaringClass(), name, category,
                    Basis.DECLARED, null, confidentiality, integrity, identifiability, List.of()), declared));
        }

        private Optional<Classified> perRow(Field field, CategoryByRow annotation) {
            String name = field.getName();
            if (annotation.declaredBy().isBlank()) {
                error(name, Code.MISSING_REASON, "@CategoryByRow needs declaredBy");
            }
            return resolve(name, annotation.ceiling()).map(category -> new Classified(
                    new FieldClassification(field.getDeclaringClass(), name, category, Basis.BY_ROW,
                            annotation.declaredBy(), category.confidentiality(), category.integrity(),
                            category.isPersonal() ? Identifiability.IDENTIFIED : null, List.of()),
                    new Declared(annotation.ceiling(), null, null, null)));
        }

        private Optional<Classified> byDefault(Field field) {
            Class<?> declaring = field.getDeclaringClass();
            DefaultCategory annotation = declaring.getDeclaredAnnotation(DefaultCategory.class);
            if (annotation == null) {
                annotation = type.getDeclaredAnnotation(DefaultCategory.class);
            }
            if (annotation == null) {
                error(field.getName(), Code.UNCLASSIFIED_FIELD, "no @Category on the field and no @DefaultCategory on "
                        + declaring.getSimpleName() + (declaring == type ? "" : " or " + type.getSimpleName()));
                return Optional.empty();
            }
            DefaultCategory found = annotation;
            // The default itself was checked by checkDefault; an invalid one leaves the field unclassified silently.
            return taxonomy.category(found.value())
                    .map(c -> taxonomy.resolveCategory(c.key()))
                    .filter(c -> !c.isPersonal())
                    .map(c -> new Classified(new FieldClassification(declaring, field.getName(), c,
                            Basis.CLASS_DEFAULT, found.reason(), c.confidentiality(), c.integrity(), null, List.of()),
                            new Declared(found.value(), null, null, null)));
        }

        // A field of a composite type without an annotation of its own: one entry per field of the composite. The
        // composite's derived fields are linked, not derived, so embedding never needs a derivation: a part is
        // derived when this class, or a source lookup, needs it.
        private void embed(String name, Class<?> composite) {
            ClassRun inner = run.collected(composite);
            if (inner == null) {
                error(name, Code.CYCLE, "the composite " + composite.getSimpleName() + " contains itself; classify"
                        + " the field as one value with @Category or @CategoryByRow");
                return;
            }
            String prefix = name + ".";
            inner.slots.forEach((path, slot) -> {
                if (slot.state != State.DONE) {
                    slots.put(prefix + path, Slot.linked(inner, path));
                } else if (slot.result != null) {
                    slots.put(prefix + path, Slot.done(prefixed(prefix, slot.result), slot.declared));
                }
            });
            for (Exemption exemption : inner.exempted) {
                exempted.add(new Exemption(exemption.declaringClass(), prefix + exemption.path(), exemption.reason()));
            }
            embedded.add(new Embedded(name, inner));
        }

        // A field that stores composites as one value: its classification must cover the composites' fields. Their
        // fields are derived, not finished, so a composite that holds itself, and two that hold each other, are
        // checked too, whichever class is classified first.
        private void checkParts(Whole whole) {
            Slot slot = slots.get(whole.name());
            FieldClassification classification = slot == null ? null : slot.result;
            if (classification == null) {
                return;
            }
            List<Protection> parts = new ArrayList<>();
            for (Class<?> composite : whole.composites()) {
                ClassRun inner = run.collectedNow(composite);
                for (String path : List.copyOf(inner.slots.keySet())) {
                    FieldClassification part = inner.derive(path);
                    if (part != null) {
                        parts.add(part.protection());
                    }
                }
            }
            Protection ceiling = Protection.joinAll(parts);
            if (!classification.protection().covers(ceiling)) {
                error(whole.name(), Code.WEAKER_THAN_PARTS, "'" + classification.category().key()
                        + "' protects less than the fields of " + whole.composites().stream()
                        .map(Class::getSimpleName).sorted().toList() + " need (" + describe(ceiling) + ")");
            }
        }

        // Resolves a derived field after its sources; a source may be a derived field itself, here or in another
        // class. The derivations of a run are one depth-first search with Tarjan's bookkeeping: a field is in a cycle
        // exactly when it shares a strongly connected component with another field or depends on itself, so the
        // same fields are found whichever field the search starts from.
        private FieldClassification derive(String path) {
            Slot slot = slots.get(path);
            Slot caller = run.derivations.isEmpty() ? null : run.derivations.getLast();
            if (slot.state != State.PENDING) {
                if (caller != null && slot.onComponentStack) {
                    caller.low = Math.min(caller.low, slot.index);
                    slot.inCycle |= caller == slot;
                }
                return slot.result;
            }
            slot.state = State.VISITING;
            slot.index = run.counter++;
            slot.low = slot.index;
            slot.onComponentStack = true;
            run.component.add(slot);
            run.derivations.add(slot);
            List<Source> sources = null;
            FieldClassification part = null;
            if (slot.link != null) {
                part = slot.link.derive(slot.linkPath);
            } else {
                sources = sources(path, slot.field.getDeclaredAnnotation(DerivedFrom.class));
            }
            run.derivations.removeLast();
            slot.state = State.DONE;
            if (caller != null) {
                caller.low = Math.min(caller.low, slot.low);
            }
            if (slot.low < slot.index) {
                slot.inCycle = true;
            } else {
                // The root of a component: the fields above it on the component stack belong to it.
                Slot member;
                do {
                    member = run.component.removeLast();
                    member.onComponentStack = false;
                    slot.inCycle |= member != slot;
                } while (member != slot);
            }
            if (slot.link != null) {
                // A part of an embedded composite: its problems, a cycle included, are the composite's own and join
                // this class's when it is finished.
                if (part != null) {
                    slot.result = prefixed(path.substring(0, path.length() - slot.linkPath.length()), part);
                    slot.declared = slot.link.slots.get(slot.linkPath).declared;
                }
                return slot.result;
            }
            if (slot.inCycle) {
                error(path, Code.CYCLE, "is derived from itself through other derived fields");
                return null;
            }
            if (sources == null) {
                return null;
            }
            Optional<Classified> classified = derived(slot.field, sources);
            classified.ifPresent(c -> {
                slot.result = c.classification();
                slot.declared = c.declared();
            });
            return slot.result;
        }

        private record Source(String reference, FieldClassification classification, Declared declared) {
        }

        // The classified sources of a derived field, or null after reporting why they cannot be found.
        private List<Source> sources(String path, DerivedFrom annotation) {
            if (annotation.value().length == 0) {
                error(path, Code.UNKNOWN_SOURCE, "@DerivedFrom needs at least one source");
                return null;
            }
            List<Source> sources = new ArrayList<>();
            boolean failed = false;
            for (String source : annotation.value()) {
                List<Source> found = annotation.type() == void.class ? local(path, source)
                        : foreign(path, annotation.type(), source);
                if (found == null) {
                    failed = true;
                } else {
                    sources.addAll(found);
                }
            }
            return failed ? null : sources;
        }

        // The classified fields of this class at a path: the field, derived first if needed, or the parts of a
        // composite. Empty if there is none or it has an error.
        private List<Source> classifiedAt(String source, String reference) {
            Slot slot = slots.get(source);
            if (slot != null) {
                FieldClassification classification = derive(source);
                return classification == null ? List.of()
                        : List.of(new Source(reference + source, classification, slot.declared));
            }
            List<Source> parts = new ArrayList<>();
            for (Map.Entry<String, Slot> entry : List.copyOf(slots.entrySet())) {
                if (entry.getKey().startsWith(source + ".")) {
                    FieldClassification classification = derive(entry.getKey());
                    if (classification != null) {
                        parts.add(new Source(reference + entry.getKey(), classification, entry.getValue().declared));
                    }
                }
            }
            return parts;
        }

        private List<Source> local(String path, String source) {
            List<Source> found = classifiedAt(source, "");
            if (!found.isEmpty()) {
                return found;
            }
            if (!slots.containsKey(source)) {
                error(path, Code.UNKNOWN_SOURCE, "the source '" + source + "' is not a classified field of "
                        + type.getSimpleName());
            } else if (!inOpenComponent(source)) {
                error(path, Code.UNKNOWN_SOURCE, "the source '" + source + "' has an error");
            }
            return null;
        }

        // Whether a source is in the component the search is in: then it is in a cycle with the field that asks,
        // which reports the CYCLE, and its own error says nothing more.
        private boolean inOpenComponent(String source) {
            Slot slot = slots.get(source);
            return slot != null && slot.onComponentStack;
        }

        // A source in another class: only that field, and what it depends on, is resolved there, so classes may
        // derive from each other's fields and only fields that depend on each other are a cycle.
        private List<Source> foreign(String path, Class<?> sourceType, String source) {
            Class<?> other = unproxied(sourceType);
            if (other == type) {
                return local(path, source);
            }
            ClassRun owner = run.collected(other);
            if (owner == null) {
                // Collecting derives nothing, so no derivation runs while a class collects its fields.
                throw new IllegalStateException(other.getName() + " is collecting its fields during a derivation");
            }
            List<Source> found = owner.classifiedAt(source, sourceType.getName() + "#");
            if (found.isEmpty() && !owner.inOpenComponent(source)) {
                error(path, Code.UNKNOWN_SOURCE, owner.slots.containsKey(source)
                        ? "the source '" + other.getSimpleName() + "." + source + "' has an error"
                        : "the source '" + source + "' is not a classified field of " + other.getSimpleName());
            }
            return found.isEmpty() ? null : found;
        }

        private Optional<Classified> derived(Field field, List<Source> sources) {
            String name = field.getName();
            List<String> references = sources.stream().map(Source::reference).toList();
            Category category = field.getDeclaredAnnotation(Category.class);
            if (category == null) {
                FieldClassification first = sources.getFirst().classification();
                boolean agree = sources.stream().map(Source::classification).allMatch(s ->
                        s.category().equals(first.category()) && s.confidentiality() == first.confidentiality()
                                && s.integrity() == first.integrity()
                                && s.identifiability() == first.identifiability());
                if (!agree) {
                    error(name, Code.AMBIGUOUS_DERIVATION, "its sources " + references + " have different"
                            + " classifications; declare the category of the derived value with @Category");
                    return Optional.empty();
                }
                return Optional.of(new Classified(new FieldClassification(field.getDeclaringClass(), name,
                        first.category(), Basis.DERIVED, null, first.confidentiality(), first.integrity(),
                        first.identifiability(), references), sources.getFirst().declared()));
            }
            Optional<Classified> declared = declared(field, category);
            if (declared.isEmpty()) {
                return Optional.empty();
            }
            FieldClassification own = declared.get().classification();
            FieldClassification result = new FieldClassification(own.declaringClass(), name, own.category(),
                    Basis.DERIVED, null, own.confidentiality(), own.integrity(), own.identifiability(), references);
            // An anonymized or aggregated value no longer points to a person, so it may drop the personal nature of
            // its sources; it still needs their confidentiality, integrity and secrecy.
            boolean summarized = own.category().isPersonal() && !own.isPersonal();
            List<String> weaker = sources.stream()
                    .filter(s -> !result.protection().covers(summarized
                            ? withoutNature(s.classification().protection()) : s.classification().protection()))
                    .map(Source::reference).toList();
            if (!weaker.isEmpty()) {
                error(name, Code.WEAKER_THAN_SOURCE, "'" + own.category().key() + "' protects less than its"
                        + " source(s) " + weaker + "; a value derived from data needs at least its protection,"
                        + " and an anonymized or aggregated value may drop only the personal nature");
                return Optional.empty();
            }
            return Optional.of(new Classified(result, declared.get().declared()));
        }

        private void checkDefault(Class<?> declaring) {
            DefaultCategory annotation = declaring.getDeclaredAnnotation(DefaultCategory.class);
            if (annotation == null) {
                return;
            }
            String where = "@DefaultCategory of " + declaring.getSimpleName();
            if (annotation.reason().isBlank()) {
                error(null, Code.MISSING_REASON, where + " needs a reason");
            }
            resolve(null, annotation.value())
                    .filter(DataCategory::isPersonal)
                    .ifPresent(c -> error(null, Code.PERSONAL_DEFAULT, where + " names '" + c.key()
                            + "', which is personal data: declare personal fields one by one with @Category"));
        }

        private Optional<DataCategory> resolve(String field, String key) {
            Optional<DataCategory> category = taxonomy.category(key);
            if (category.isEmpty()) {
                error(field, Code.UNKNOWN_CATEGORY, "unknown data category '" + key + "'");
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

        private <T> T single(String field, String element, T[] values, T fallback) {
            if (values.length > 1) {
                error(field, Code.AMBIGUOUS_VALUE, element + " must have at most one value");
            }
            return values.length == 0 ? fallback : values[0];
        }

        private boolean hasNewErrors(int from) {
            return violations.subList(from, violations.size()).stream().anyMatch(Violation::isError);
        }

        private void error(String field, Code code, String message) {
            violations.add(new Violation(type, field, Severity.ERROR, code, message));
        }
    }

    // The field as part of a composite embedded under a prefix: its path and its local sources move below it.
    private static FieldClassification prefixed(String prefix, FieldClassification field) {
        List<String> sources = field.derivedFrom().stream()
                .map(source -> source.contains("#") ? source : prefix + source).toList();
        return new FieldClassification(field.declaringClass(), prefix + field.path(), field.category(),
                field.basis(), field.reason(), field.confidentiality(), field.integrity(), field.identifiability(),
                sources);
    }

    private static Protection withoutNature(Protection protection) {
        return Protection.of(LegalNature.NONE, protection.confidentiality(), protection.integrity(),
                protection.secret());
    }

    /** Where a type appears in the declared type of a field. */
    private enum Position {
        /** The value itself, or an element of an array, collection, optional or map value: its parts can be paths. */
        ELEMENT,
        /** A map key: a value the field holds, whose parts cannot be paths. */
        KEY,
        /** A type argument of a class that is not a container: the field may or may not hold such a value. */
        ARGUMENT
    }

    /**
     * What the declared type of a field may hold, as far as the classifier can tell: the composite classes it
     * mentions, whether each is reached only as an element (so that it can be embedded), and whether a value it holds
     * is typed {@code Object} or by an unbound type variable, which may be anything.
     */
    private static final class Holding {

        private final Map<TypeVariable<?>, Type> bindings;
        private final Set<Class<?>> composites = new LinkedHashSet<>();
        // Classes with classified fields that are not composites, such as another entity or a forgotten marker.
        private final Set<Class<?>> classified = new LinkedHashSet<>();
        private final Set<TypeVariable<?>> visiting = new HashSet<>();
        private boolean embeddable = true;
        private boolean unknown;

        Holding(Map<TypeVariable<?>, Type> bindings) {
            this.bindings = bindings;
        }

        Holding of(Type type) {
            walk(type, Position.ELEMENT);
            return this;
        }

        // The composite whose fields the field holds, when it holds exactly one, only as elements, and nothing unknown.
        Class<?> embeddable() {
            return composites.size() == 1 && embeddable && !unknown ? composites.iterator().next() : null;
        }

        boolean hidesData() {
            return unknown || !composites.isEmpty() || !classified.isEmpty();
        }

        String explain() {
            if (unknown) {
                return "its type holds values typed Object or by a type variable, which may be personal data";
            }
            if (!classified.isEmpty()) {
                return "it holds " + classified.stream().map(Class::getSimpleName).sorted().toList() + ", whose"
                        + " fields are classified: mark the class @Composite to classify its fields here or, for a"
                        + " reference to another record, declare what the field stores (such as the record's key)";
            }
            return "it holds " + composites.stream().map(Class::getSimpleName).sorted().toList()
                    + " in a way that cannot be split into the composite's fields (a map key, a type argument of a"
                    + " class that is not a collection, or several composites)";
        }

        private void walk(Type type, Position position) {
            switch (type) {
                case Class<?> c when c.isArray() -> walk(c.getComponentType(), position);
                case Class<?> c -> walkClass(c, c, position);
                case ParameterizedType p -> walkClass((Class<?>) p.getRawType(), p, position);
                case GenericArrayType g -> walk(g.getGenericComponentType(), position);
                case WildcardType w -> {
                    Arrays.stream(w.getUpperBounds()).forEach(bound -> walk(bound, position));
                    Arrays.stream(w.getLowerBounds()).forEach(bound -> walk(bound, position));
                }
                case TypeVariable<?> v -> {
                    Type bound = bindings.get(v);
                    if (bound != null) {
                        walk(bound, position);
                    } else if (visiting.add(v)) {
                        Arrays.stream(v.getBounds()).forEach(b -> walk(b, position));
                        visiting.remove(v);
                    }
                }
                default -> unknown |= position != Position.ARGUMENT;
            }
        }

        private void walkClass(Class<?> raw, Type type, Position position) {
            if (raw.isAnnotationPresent(Composite.class)) {
                composites.add(raw);
                embeddable &= position == Position.ELEMENT;
            } else if (raw == Object.class) {
                unknown |= position != Position.ARGUMENT;
            } else if (Map.class.isAssignableFrom(raw) || raw == Map.Entry.class) {
                Class<?> container = raw == Map.Entry.class ? Map.Entry.class : Map.class;
                walk(argumentOf(type, container, 0), position == Position.ARGUMENT ? position : Position.KEY);
                walk(argumentOf(type, container, 1), position);
            } else if (Iterable.class.isAssignableFrom(raw)) {
                walk(argumentOf(type, Iterable.class, 0), position);
            } else if (raw == Optional.class) {
                walk(argumentOf(type, Optional.class, 0), position);
            } else {
                if (position != Position.ARGUMENT && isClassified(raw)) {
                    classified.add(raw);
                }
                if (type instanceof ParameterizedType p) {
                    for (Type argument : p.getActualTypeArguments()) {
                        walk(argument, Position.ARGUMENT);
                    }
                }
            }
        }
    }

    // Whether a class declares a classification of its own: a class default or an annotated field.
    private static boolean isClassified(Class<?> type) {
        for (Class<?> c : hierarchyOf(type)) {
            if (c.isAnnotationPresent(DefaultCategory.class) || Arrays.stream(c.getDeclaredFields())
                    .anyMatch(f -> FIELD_ANNOTATIONS.stream().anyMatch(f::isAnnotationPresent))) {
                return true;
            }
        }
        return false;
    }

    // What the type variables of the superclasses of a class stand for in it. A subclass binds before its superclass,
    // so an argument that names the subclass's own type variable is already resolved when the superclass binds it.
    private static Map<TypeVariable<?>, Type> bindingsOf(Class<?> type) {
        Map<TypeVariable<?>, Type> bindings = new HashMap<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (c.getGenericSuperclass() instanceof ParameterizedType p) {
                TypeVariable<?>[] parameters = ((Class<?>) p.getRawType()).getTypeParameters();
                Type[] arguments = p.getActualTypeArguments();
                for (int i = 0; i < parameters.length; i++) {
                    bindings.put(parameters[i], substitute(arguments[i], bindings));
                }
            }
        }
        return bindings;
    }

    // The type argument a type gives to a type parameter of one of its supertypes, such as the element type of a
    // List subclass seen as an Iterable; Object when the type does not say (a raw type).
    private static Type argumentOf(Type type, Class<?> target, int index) {
        Map<TypeVariable<?>, Type> bindings = new HashMap<>();
        Class<?> raw;
        if (type instanceof ParameterizedType p) {
            raw = (Class<?>) p.getRawType();
            TypeVariable<?>[] parameters = raw.getTypeParameters();
            for (int i = 0; i < parameters.length; i++) {
                bindings.put(parameters[i], p.getActualTypeArguments()[i]);
            }
        } else {
            raw = (Class<?>) type;
        }
        return argumentOf(raw, bindings, target, index);
    }

    private static Type argumentOf(Class<?> raw, Map<TypeVariable<?>, Type> bindings, Class<?> target, int index) {
        if (raw == target) {
            Type argument = substitute(target.getTypeParameters()[index], bindings);
            // A type parameter that stays unbound is a raw use of the type: its values may be anything.
            return argument instanceof TypeVariable<?> v && v.getGenericDeclaration() == raw ? Object.class
                    : argument;
        }
        List<Type> supertypes = new ArrayList<>(Arrays.asList(raw.getGenericInterfaces()));
        if (raw.getGenericSuperclass() != null) {
            supertypes.add(raw.getGenericSuperclass());
        }
        for (Type supertype : supertypes) {
            Class<?> superRaw = supertype instanceof ParameterizedType p ? (Class<?>) p.getRawType()
                    : (Class<?>) supertype;
            if (!target.isAssignableFrom(superRaw)) {
                continue;
            }
            Map<TypeVariable<?>, Type> inner = new HashMap<>();
            if (supertype instanceof ParameterizedType p) {
                TypeVariable<?>[] parameters = superRaw.getTypeParameters();
                for (int i = 0; i < parameters.length; i++) {
                    inner.put(parameters[i], substitute(p.getActualTypeArguments()[i], bindings));
                }
            }
            return argumentOf(superRaw, inner, target, index);
        }
        return Object.class;
    }

    // A type with the bound type variables replaced, at any depth.
    private static Type substitute(Type type, Map<TypeVariable<?>, Type> bindings) {
        return switch (type) {
            case TypeVariable<?> v -> bindings.getOrDefault(v, v);
            case ParameterizedType p -> new Parameterized((Class<?>) p.getRawType(), p.getOwnerType(),
                    Arrays.stream(p.getActualTypeArguments()).map(a -> substitute(a, bindings)).toArray(Type[]::new));
            case GenericArrayType g -> new GenericArray(substitute(g.getGenericComponentType(), bindings));
            case WildcardType w -> new Wildcard(
                    Arrays.stream(w.getUpperBounds()).map(b -> substitute(b, bindings)).toArray(Type[]::new),
                    Arrays.stream(w.getLowerBounds()).map(b -> substitute(b, bindings)).toArray(Type[]::new));
            default -> type;
        };
    }

    private record Parameterized(Class<?> raw, Type owner, Type[] arguments) implements ParameterizedType {
        @Override
        public Type[] getActualTypeArguments() {
            return arguments.clone();
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return owner;
        }
    }

    private record GenericArray(Type component) implements GenericArrayType {
        @Override
        public Type getGenericComponentType() {
            return component;
        }
    }

    private record Wildcard(Type[] upper, Type[] lower) implements WildcardType {
        @Override
        public Type[] getUpperBounds() {
            return upper.clone();
        }

        @Override
        public Type[] getLowerBounds() {
            return lower.clone();
        }
    }

    private static String describe(Protection protection) {
        return protection.natures() + ", " + protection.confidentiality() + ", " + protection.integrity()
                + (protection.secret() ? ", secret" : "");
    }

    private static final List<Class<? extends Annotation>> FIELD_ANNOTATIONS =
            List.of(Category.class, CategoryByRow.class, DerivedFrom.class, Exempt.class);

    // A transient field is not stored, so it is skipped, unless an annotation says it holds data to classify.
    private static boolean isExcluded(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || field.isSynthetic() || field.getName().startsWith(GENERATED_FIELD_PREFIX)
                || Modifier.isTransient(modifiers) && FIELD_ANNOTATIONS.stream().noneMatch(field::isAnnotationPresent);
    }

    // A Hibernate proxy subclass (Entity$HibernateProxy$...) adds only the proxy's own state: classify the entity.
    private static Class<?> unproxied(Class<?> type) {
        Class<?> current = type;
        while (current.getName().contains(HIBERNATE_PROXY) && current.getSuperclass() != null
                && current.getSuperclass() != Object.class) {
            current = current.getSuperclass();
        }
        return current;
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
}
