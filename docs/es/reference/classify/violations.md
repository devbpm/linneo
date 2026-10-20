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
public record Violation(Class<?> type, String field, Severity severity, Code code, String message)
```

Un problema encontrado al clasificar una clase.

| Componente | Admite `null` | Descripción |
|---|---|---|
| `type` | no | La clase que se clasifica (no necesariamente la que declara el campo o la anotación). |
| `field` | sí | El campo implicado; `null` si el problema está en una anotación de clase (`@DefaultCategory`). |
| `severity` | no | Si hace fallar la verificación. |
| `code` | no | Qué tipo de problema es. |
| `message` | no | Una descripción que nombra la clave o la anotación implicada. |

El constructor canónico lanza `NullPointerException` si `type`, `severity`, `code` o `message` son `null`.

| Método | Descripción |
|---|---|
| `boolean isError()` | `true` para la gravedad `ERROR`. |
| `String toString()` | `<SEVERITY> <CODE> <nombre binario de type>[.<field>]: <message>`, por ejemplo `ERROR UNCLASSIFIED_FIELD com.example.Person.undecided: no @Category on the field and no @DefaultCategory on Person`. |

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
| `UNCLASSIFIED_FIELD` | error | Un campo no tiene `@Category`, `@CategoryByRow` ni `@Exempt`, y ni su clase declarante ni la clase clasificada tienen `@DefaultCategory`. | `no @Category on the field and no @DefaultCategory on <Declaring>[ or <Type>]` | no |
| `UNKNOWN_CATEGORY` | error | La clave de `@Category`, el `ceiling` de `@CategoryByRow` o el `value` de `@DefaultCategory` no está en la taxonomía. | `unknown data category '<key>'` | no (en un valor por defecto: faltan los campos que cubriría) |
| `DEPRECATED_CATEGORY` | **aviso** | Una clave de cualquier anotación está obsoleta. | `data category '<key>' is deprecated; use '<replacement>'` (sin la parte `; use` si no hay sustituta) | sí, con la sustituta vigente |
| `PERSONAL_DEFAULT` | error | La categoría (resuelta) de un `@DefaultCategory` es personal. `field` es `null`. | `@DefaultCategory of <Class> names '<key>', which is personal data: declare personal fields one by one with @Category` | no (faltan los campos que cubriría) |
| `MISSING_REASON` | error | `@Exempt` tiene el valor en blanco, `@CategoryByRow` el `declaredBy` en blanco o `@DefaultCategory` el `reason` en blanco (`field` es `null`). | `@Exempt needs a reason` / `@CategoryByRow needs declaredBy` / `@DefaultCategory of <Class> needs a reason` | exento: nunca; por fila y por defecto: sí |
| `CONFLICTING_ANNOTATIONS` | error | `@Exempt` con `@Category` o `@CategoryByRow`, o `@Category` con `@CategoryByRow`. | `@Exempt cannot be combined with @Category or @CategoryByRow` / `@Category and @CategoryByRow cannot be used together` | no |
| `LOWERED_LEVEL` | error | `@Category` declara una confidencialidad o integridad inferior a la de la categoría. Una violación por faceta. | `confidentiality <c> is lower than the <cc> of '<key>'` / `integrity <i> is lower than the <ci> of '<key>'` | no |
| `AMBIGUOUS_VALUE` | error | Un elemento opcional de `@Category` (`confidentiality`, `integrity`, o `identifiability` en una categoría personal) tiene más de un valor. | `<element> must have at most one value` | no |
| `IDENTIFIABILITY_NOT_APPLICABLE` | error | `@Category` declara una `identifiability` para una categoría que no es personal. | `'<key>' is not personal data and has no identifiability` | no |

Las violaciones sobre un `@DefaultCategory` se comprueban en la clase clasificada y en cada superclase que lo tenga,
y se informan en la `ClassClassification` de la clase que se clasifica (así, un valor por defecto erróneo en una
superclase aparece en el resultado de cada subclase).

```java
ClassClassification result = new Classifier(Taxonomy.standard()).classify(WrongFields.class);
for (Violation v : result.violations()) {
    System.out.println(v.severity() + " " + v.code() + " " + v.field() + ": " + v.message());
}
List<Violation.Code> codes = result.errors().stream().map(Violation::code).toList();
```

Véase también: [Classifier](classifier.md) · [Resultados](results.md) · [Anotaciones](../annotations/README.md) ·
curso: [Verificar en los tests](../../course/04-verifying-in-tests.md)
