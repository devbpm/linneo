---
title: "Violaciones"
parent: "Referencia"
nav_order: 11
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/classify/Violation.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# Violaciones

## Violation

`io.github.devbpm.linneo.classify.Violation` — `public record`, `@since 0.1.0`

```java
public record Violation(Class<?> type, String path, Severity severity, Code code, String message)
```

Un problema encontrado al clasificar una clase.

| Componente | Admite `null` | Descripción |
|---|---|---|
| `type` | no | La clase que se clasifica (no necesariamente la que declara el campo o la anotación). |
| `path` | sí | La ruta del campo implicado, que lo identifica dentro de `type`: su nombre, o `campo.parte` para un campo de un [`@Composite`](../annotations/composite.md); `null` si el problema está en una anotación de clase (`@DefaultCategory`). |
| `severity` | no | Si hace fallar la verificación. |
| `code` | no | Qué tipo de problema es. |
| `message` | no | Una descripción que nombra la clave o la anotación implicada. |

El constructor canónico lanza `NullPointerException` si `type`, `severity`, `code` o `message` son `null`.

| Método | Descripción |
|---|---|
| `boolean isError()` | `true` para la gravedad `ERROR`. |
| `String toString()` | `<SEVERITY> <CODE> <nombre binario de type>[.<path>]: <message>`, por ejemplo `ERROR UNCLASSIFIED_FIELD com.example.Person.undecided: no @Category on the field and no @DefaultCategory on Person`. |

## Severity

`Violation.Severity`

| Constante | Significado |
|---|---|
| `ERROR` | La clasificación es incorrecta o falta; `Classifier.verify` falla y `ClassClassification.isValid()` es `false`. |
| `WARNING` | La clasificación funciona pero conviene actualizarla; la verificación pasa. |

## Códigos

`Violation.Code` — todos los valores, con la gravedad que les asigna el [`Classifier`](classifier.md), cuándo se
producen, el mensaje y si el campo sigue apareciendo en `ClassClassification.fields()`.

| Código | Gravedad | Se produce cuando | Mensaje | ¿Se clasifica el campo? |
|---|---|---|---|---|
| `UNCLASSIFIED_FIELD` | error | Un campo no tiene `@Category`, `@CategoryByRow`, `@DerivedFrom` ni `@Exempt`, no es de un tipo [`@Composite`](../annotations/composite.md), y ni su clase declarante ni la clase clasificada tienen `@DefaultCategory`. Un `Map` de compuestos cuenta como un campo ordinario. | `no @Category on the field and no @DefaultCategory on <Declaring>[ or <Type>]` | no |
| `UNKNOWN_CATEGORY` | error | La clave de `@Category`, el `ceiling` de `@CategoryByRow` o el `value` de `@DefaultCategory` no está en la taxonomía. | `unknown data category '<key>'` | no (en un valor por defecto: faltan los campos que cubriría) |
| `DEPRECATED_CATEGORY` | **aviso** | Una clave de cualquier anotación está obsoleta. | `data category '<key>' is deprecated; use '<replacement>'` (sin la parte `; use` si no hay sustituta) | sí, con la sustituta vigente |
| `PERSONAL_DEFAULT` | error | La categoría (resuelta) de un `@DefaultCategory` es personal. `path` es `null`. | `@DefaultCategory of <Class> names '<key>', which is personal data: declare personal fields one by one with @Category` | no (faltan los campos que cubriría) |
| `MISSING_REASON` | error | `@Exempt` tiene el valor en blanco, `@CategoryByRow` el `declaredBy` en blanco o `@DefaultCategory` el `reason` en blanco (`path` es `null`). | `@Exempt needs a reason` / `@CategoryByRow needs declaredBy` / `@DefaultCategory of <Class> needs a reason` | exento: nunca; por fila y por defecto: sí |
| `CONFLICTING_ANNOTATIONS` | error | `@Exempt` con `@Category`, `@CategoryByRow` o `@DerivedFrom`, o `@CategoryByRow` con `@Category` o `@DerivedFrom`. (`@Category` con `@DerivedFrom` está permitido.) | `@Exempt cannot be combined with @Category, @CategoryByRow or @DerivedFrom` / `@CategoryByRow cannot be combined with @Category or @DerivedFrom` | no |
| `LOWERED_LEVEL` | error | `@Category` declara una confidencialidad o integridad inferior a la de la categoría. Una violación por faceta. | `confidentiality <c> is lower than the <cc> of '<key>'` / `integrity <i> is lower than the <ci> of '<key>'` | no |
| `AMBIGUOUS_VALUE` | error | Un elemento opcional de `@Category` (`confidentiality`, `integrity`, o `identifiability` en una categoría personal) tiene más de un valor. | `<element> must have at most one value` | no |
| `IDENTIFIABILITY_NOT_APPLICABLE` | error | `@Category` declara una `identifiability` para una categoría que no es personal. | `'<key>' is not personal data and has no identifiability` | no |
| `DUPLICATE_FIELD` | error | Un campo tiene el nombre de un campo de una superclase, así que su ruta no lo identificaría. Se produce para el campo que oculta, no para el de la superclase. | `hides the field of the same name declared by <Superclass>; rename one of them` | no (el de la superclase sí) |
| `UNKNOWN_SOURCE` | error | Un campo [`@DerivedFrom`](../annotations/derived-from.md) no nombra ninguna fuente, o nombra una que no es un campo clasificado de la clase indicada por `type` (o de la clase clasificada): no existe, está exenta o tiene un error, incluso un campo en un ciclo. | `@DerivedFrom needs at least one source` / `the source '<s>' is not a classified field of <Class>` / `the source '<s>' has an error` / `the source '<Class>.<s>' has an error` (un campo de otra clase) | no |
| `AMBIGUOUS_DERIVATION` | error | Un campo derivado sin `@Category` tiene fuentes cuyas clasificaciones difieren en categoría, confidencialidad, integridad o identificabilidad. | `its sources [<s>, ...] have different classifications; declare the category of the derived value with @Category` | no |
| `WEAKER_THAN_SOURCE` | error | Un campo derivado declara una categoría que protege menos que una de sus fuentes, sin declarar el valor anonimizado o agregado. | `'<key>' protects less than its source(s) [<s>, ...]; a value derived from data needs at least its protection, unless it is declared anonymized or aggregated` | no |
| `WEAKER_THAN_PARTS` | error | Un campo que guarda un [`@Composite`](../annotations/composite.md) como un solo valor declara, con `@Category` o `@CategoryByRow`, una categoría que protege menos que los campos del compuesto. | `'<key>' protects less than the fields of <Composite> need (<natures>, <confidentiality>, <integrity>[, secret])` | **sí** (el error se informa igualmente) |
| `CYCLE` | error | Un compuesto se contiene a sí mismo, o campos derivados dependen unos de otros, dentro de una clase o entre clases, también a través de las partes de un compuesto. | `the composite <Composite> contains itself; classify the field as one value with @Category or @CategoryByRow` / `is derived from itself through other derived fields` | no (en un compuesto, el campo que cierra el bucle) |

En `AMBIGUOUS_DERIVATION` y `WEAKER_THAN_SOURCE` las fuentes `[<s>, ...]` se escriben como en
`FieldClassification.derivedFrom()`: rutas de la misma clase (un compuesto expandido a sus campos), o
`ClassName#path`. En `UNKNOWN_SOURCE`, `<s>` es la ruta tal como está escrita en la anotación.

Las clases que derivan de los otros campos de la otra, y un compuesto que deriva de una clase que lo contiene, no son un
`CYCLE`: solo lo son los campos derivados que dependen unos de otros, y cada uno recibe el segundo mensaje. Un campo
que deriva de uno de ellos sin estar en el ciclo es un `UNKNOWN_SOURCE`. Un campo de un compuesto que está en un ciclo
se informa con el compuesto y con cada clase que lo contiene, en `campo.parte`.

Los mismos campos tienen errores, con los mismos códigos y mensajes, sea cual sea el orden en que se clasifiquen las
clases.

Las violaciones sobre un `@DefaultCategory` se comprueban en la clase clasificada y en cada superclase que lo tenga,
y se informan en la `ClassClassification` de la clase que se clasifica (así, un valor por defecto erróneo en una
superclase aparece en el resultado de cada subclase).

Las violaciones que se encuentran dentro de un [`@Composite`](../annotations/composite.md) se informan en la
`ClassClassification` de la clase que lo contiene, con esa clase como `type` y la ruta `campo.parte`; una que no tiene
ruta propia (un `@DefaultCategory` defectuoso del compuesto) recibe la ruta del campo que lo contiene. Un compuesto con
un error aparece, por tanto, en el resultado de cada clase que lo contiene.

```java
ClassClassification result = new Classifier(Taxonomy.standard()).classify(WrongFields.class);
for (Violation v : result.violations()) {
    System.out.println(v.severity() + " " + v.code() + " " + v.path() + ": " + v.message());
}
List<Violation.Code> codes = result.errors().stream().map(Violation::code).toList();
```

Véase también: [Classifier](classifier.md) · [Resultados](results.md) · [Anotaciones](../annotations/README.md) ·
curso: [Verificar en los tests](../../course/04-verifying-in-tests.md)
