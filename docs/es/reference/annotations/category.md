---
title: "@Category"
parent: "Anotaciones"
grand_parent: "Referencia"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/Category.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->

# @Category

`io.github.devbpm.linneo.annotation.Category` —
`@Documented @Retention(RUNTIME) @Target({FIELD, RECORD_COMPONENT})`, `@since 0.1.0`

Declara la categoría de datos que contiene un campo. En un componente de record, la anotación se aplica también al
campo del componente.

## Elementos

| Elemento | Tipo | Por defecto | Significado |
|---|---|---|---|
| `value` | `String` | obligatorio | Clave de la categoría, preferiblemente una constante de [`Categories`](../key-constants.md). |
| `identifiability` | `Identifiability[]` | `{}` | Hasta qué punto el valor almacenado apunta a la persona; solo para categorías personales. Vacío equivale a `IDENTIFIED`. |
| `confidentiality` | `Confidentiality[]` | `{}` | Una confidencialidad más estricta que la de la categoría, solo para este campo. Vacío equivale al nivel de la categoría. |
| `integrity` | `Integrity[]` | `{}` | Una integridad más estricta que la de la categoría, solo para este campo. Vacío equivale al nivel de la categoría. |

Los elementos opcionales son arrays para distinguir "sin fijar" de un valor: déjalos vacíos o da exactamente un
valor.

## Reglas que aplica el Classifier

| Regla | Violación | Gravedad | ¿Se clasifica el campo? |
|---|---|---|---|
| `value` debe ser una clave de la taxonomía | `UNKNOWN_CATEGORY` (`unknown data category '<key>'`) | error | no |
| Un `value` obsoleto se sustituye por su sustituta vigente (`Taxonomy.resolveCategory`) | `DEPRECATED_CATEGORY` (`data category '<key>' is deprecated; use '<replacement>'`) | aviso | sí, con la sustituta |
| Como mucho un valor por elemento opcional | `AMBIGUOUS_VALUE` (`<element> must have at most one value`) | error | no |
| `confidentiality` no inferior a la de la categoría | `LOWERED_LEVEL` (`confidentiality <c> is lower than the <cc> of '<key>'`) | error | no |
| `integrity` no inferior a la de la categoría | `LOWERED_LEVEL` (`integrity <i> is lower than the <ci> of '<key>'`) | error | no |
| `identifiability` solo en categorías personales | `IDENTIFIABILITY_NOT_APPLICABLE` (`'<key>' is not personal data and has no identifiability`) | error | no |
| No combinarla con `@CategoryByRow` ni con `@Exempt` | `CONFLICTING_ANNOTATIONS` | error | no |

Notas:

- Las comprobaciones de nivel comparan con los niveles de la categoría resuelta (vigente).
- Con varios valores se sigue comparando el primero, así que `integrity = {LOW, HIGH}` sobre una categoría `MEDIUM`
  produce `AMBIGUOUS_VALUE` y también `LOWERED_LEVEL`.
- La ambigüedad de `identifiability` solo se comprueba en categorías personales; en una no personal, cualquier valor
  produce `IDENTIFIABILITY_NOT_APPLICABLE`.

## FieldClassification resultante

| Componente | Valor |
|---|---|
| `category` | la categoría resuelta |
| `basis` | `DECLARED` |
| `reason` | `null` |
| `confidentiality` / `integrity` | el valor declarado o el de la categoría |
| `identifiability` | el valor declarado, o `IDENTIFIED`, en una categoría personal; `null` en una no personal |

## Ejemplos

```java
import io.github.devbpm.linneo.annotation.Category;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.Identifiability;
import static io.github.devbpm.linneo.Categories.*;

public class Person {

    @Category(USER_CONTACT_EMAIL)
    private String email;                 // user.contact.email, IDENTIFIED, LOW / MEDIUM

    @Category(value = USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
    private String analyticsId;           // sigue siendo dato personal

    @Category(value = USER_CONTENT_PRIVATE, confidentiality = Confidentiality.HIGH)
    private String notes;                 // elevada de MEDIUM a HIGH

    @Category(value = USER_CONTACT, identifiability = Identifiability.AGGREGATED)
    private Integer contactsCount;        // no es dato personal: isPersonal() es false
}

public record Contact(@Category(USER_CONTACT_EMAIL) String email) {
}
```

Declaraciones incorrectas:

```java
@Category("user.unknown")                                                   // UNKNOWN_CATEGORY
private String a;

@Category(value = USER_HEALTH_AND_MEDICAL, confidentiality = Confidentiality.LOW)   // LOWERED_LEVEL
private String b;

@Category(value = SYSTEM_CONFIGURATION, identifiability = Identifiability.IDENTIFIED) // IDENTIFIABILITY_NOT_APPLICABLE
private String c;

@Category(value = USER_NAME, integrity = {Integrity.LOW, Integrity.HIGH})            // AMBIGUOUS_VALUE (+ LOWERED_LEVEL)
private String d;
```

Véase también: [Visión general de las anotaciones](README.md) · [Niveles de protección](../protection-levels.md) ·
[Constantes de claves](../key-constants.md) · [Violaciones](../classify/violations.md) ·
curso: [Anotar modelos](../../course/03-annotating-models.md)
