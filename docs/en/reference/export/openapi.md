---
title: "OpenAPI extension"
parent: "Export"
grand_parent: "Reference"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/export/openapi/OpenApiExtension.java
  - src/main/java/io/github/devbpm/linneo/export/openapi/package-info.java
  - src/main/java/io/github/devbpm/linneo/internal/Json.java
-->

# OpenAPI extension

`io.github.devbpm.linneo.export.openapi.OpenApiExtension` — `public final class`, `@since 0.1.0`

The classification of the fields of an API's data as an [OpenAPI](https://spec.openapis.org/oas/latest.html)
specification extension, so that the description of an API says what personal data each property carries and how much
protection it needs. The data an API returns is the data that leaves the system. Its transfer objects are classified
like any class, often with [`@DerivedFrom`](../annotations/derived-from.md), so that a view of an entity takes the
classification of the fields it copies.

The class has no public constructor and holds no state: every method is `static`. Linneo does not read or write the
API description. The application maps a field path to its property, which may be renamed or nested, and adds the
extension with the library it already uses.

## The extension

Each property gets one extension, named by the constant `OpenApiExtension.NAME`, `"x-linneo"`. Its value is an object:

```json
"x-linneo": {"category": "user.contact.email", "personal": true, "nature": "ORDINARY",
             "confidentiality": "LOW", "integrity": "MEDIUM", "identifiability": "IDENTIFIED", "secret": false}
```

| Member | Present | Value |
|---|---|---|
| `category` | always | The key of the data category of the field. |
| `personal` | always | `true` if the field holds personal data: a personal category not declared `ANONYMIZED` or `AGGREGATED`. |
| `nature` | only for personal data | The name of the [`LegalNature`](../protection-levels.md) of the category: `ORDINARY`, `SPECIAL_CATEGORY` or `CRIMINAL_OFFENCE`. |
| `confidentiality` | always | The name of the [`Confidentiality`](../protection-levels.md) the field needs, with the level raised on the field if any. |
| `integrity` | always | The name of the [`Integrity`](../protection-levels.md) the field needs, likewise. |
| `identifiability` | only for personal data | `IDENTIFIED` or `PSEUDONYMIZED`: the [`Identifiability`](../protection-levels.md) of the field. |
| `secret` | always | `true` if the field holds a secret. |

The values are strings (names of constants) and booleans. The extension is also valid in a JSON Schema, which ignores
unknown keywords.

## Members

| Member | Returns | Description |
|---|---|---|
| `static final String NAME` | `String` | `"x-linneo"`. |
| `static Map<String, Object> of(FieldClassification field)` | immutable map | The extension of a property that carries a classified field, in a stable order. |
| `static Map<String, Object> of(InventoryField field, Taxonomy taxonomy)` | immutable map | The extension of a stored field, resolved with the taxonomy; **empty** for an exempted field, which carries no data. `TaxonomyException` if the taxonomy does not contain the field's key. |
| `static Map<String, Map<String, Object>> of(ClassClassification classification)` | immutable map | The extensions of every classified field of a class by field path, in the order of the fields; exempted fields have none. `IllegalArgumentException` if the class has classification errors, because a field with an error would be described without its classification. |
| `static String toJson(ClassClassification classification)` | `String` | The same as a JSON object whose members are the field paths and whose values are the extensions, for tools that are not written in Java. `IllegalArgumentException` if the class has errors. |

A field derived with `@DerivedFrom` and no category of its own has the extension of its source, and the path of a part
of a [`@Composite`](../annotations/composite.md) is `field.part`, as in the classification.

## Using it

A transfer object for the API, and the extensions of its fields:

```java
@DefaultCategory(value = SYSTEM_OPERATIONS, reason = "paging data of the response")
public class StudentView {

    @Category(USER_NAME_FIRST)
    private String firstName;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    @DerivedFrom("email")
    private String emailDomain;

    private int page;

    @Exempt("hypermedia links")
    private String links;
}
```

```java
import io.github.devbpm.linneo.export.openapi.OpenApiExtension;

ClassClassification view = classifier.classify(StudentView.class);
Map<String, Map<String, Object>> extensions = OpenApiExtension.of(view);

extensions.keySet();                                   // [firstName, email, emailDomain, page]
extensions.get("email").get("confidentiality");        // "LOW"
extensions.get("email").equals(extensions.get("emailDomain"));   // true: derived from the email
OpenApiExtension.toJson(view);
// {"firstName":{"category":"user.name.first","personal":true,"nature":"ORDINARY",...},"email":{...},...}
```

To add them to the description of an API, hand each map to the `addExtension` method that swagger-core and springdoc
schemas have, with the property that carries the field. A springdoc customizer, for example:

```java
import io.github.devbpm.linneo.export.openapi.OpenApiExtension;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;

@Bean
OpenApiCustomizer linneo(Classifier classifier) {
    ClassClassification view = classifier.classify(StudentView.class);
    Map<String, Map<String, Object>> extensions = OpenApiExtension.of(view);
    return openApi -> {
        Schema<?> schema = openApi.getComponents().getSchemas().get("StudentView");
        extensions.forEach((path, extension) ->
                schema.getProperties().get(path).addExtension(OpenApiExtension.NAME, extension));
    };
}
```

springdoc and swagger-core are the application's dependencies, not Linneo's: the library has none. A tool that
reads the JSON takes it from `OpenApiExtension.toJson(...)`, written wherever the build can use it.

See also: [Export](README.md) · [Classifier](../classify/classifier.md) · [Data inventory](../classify/inventory.md) ·
[`@DerivedFrom`](../annotations/derived-from.md)
