---
title: "Extensión OpenAPI"
parent: "Exportación"
grand_parent: "Referencia"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/export/openapi/OpenApiExtension.java
  - src/main/java/io/github/devbpm/linneo/export/openapi/package-info.java
  - src/main/java/io/github/devbpm/linneo/internal/Json.java
-->

# Extensión OpenAPI

`io.github.devbpm.linneo.export.openapi.OpenApiExtension` — `public final class`, `@since 0.1.0`

La clasificación de los campos de los datos de una API como una extensión de la especificación
[OpenAPI](https://spec.openapis.org/oas/latest.html), para que la descripción de una API diga qué datos personales
lleva cada propiedad y cuánta protección necesita. Los datos que devuelve una API son los datos que salen del sistema.
Sus objetos de transferencia se clasifican como cualquier clase, a menudo con
[`@DerivedFrom`](../annotations/derived-from.md), de modo que una vista de una entidad toma la clasificación de los
campos que copia.

La clase no tiene constructor público ni guarda estado: todos sus métodos son `static`. Linneo no lee ni escribe la
descripción de la API. La aplicación asigna la ruta de un campo a su propiedad, que puede tener otro nombre o estar
anidada, y añade la extensión con la biblioteca que ya usa.

## La extensión

Cada propiedad recibe una extensión, con el nombre de la constante `OpenApiExtension.NAME`, `"x-linneo"`. Su valor es
un objeto:

```json
"x-linneo": {"category": "user.contact.email", "personal": true, "nature": "ORDINARY",
             "confidentiality": "LOW", "integrity": "MEDIUM", "identifiability": "IDENTIFIED", "secret": false}
```

| Miembro | Presente | Valor |
|---|---|---|
| `category` | siempre | La clave de la categoría de datos del campo. |
| `personal` | siempre | `true` si el campo contiene datos personales: una categoría personal no declarada `ANONYMIZED` ni `AGGREGATED`. |
| `nature` | solo en datos personales | El nombre de la [`LegalNature`](../protection-levels.md) de la categoría: `ORDINARY`, `SPECIAL_CATEGORY` o `CRIMINAL_OFFENCE`. |
| `confidentiality` | siempre | El nombre de la [`Confidentiality`](../protection-levels.md) que necesita el campo, con el nivel elevado en el campo si lo hay. |
| `integrity` | siempre | El nombre de la [`Integrity`](../protection-levels.md) que necesita el campo, igualmente. |
| `identifiability` | solo en datos personales | `IDENTIFIED` o `PSEUDONYMIZED`: la [`Identifiability`](../protection-levels.md) del campo. |
| `secret` | siempre | `true` si el campo contiene un secreto. |

Los valores son cadenas (nombres de constantes) y booleanos. La extensión también es válida en un JSON Schema, que
ignora las palabras clave desconocidas.

## Miembros

| Miembro | Devuelve | Descripción |
|---|---|---|
| `static final String NAME` | `String` | `"x-linneo"`. |
| `static Map<String, Object> of(FieldClassification field)` | mapa inmutable | La extensión de una propiedad que lleva un campo clasificado, en un orden estable. |
| `static Map<String, Object> of(InventoryField field, Taxonomy taxonomy)` | mapa inmutable | La extensión de un campo guardado, resuelto con la taxonomía; **vacía** para un campo exento, que no lleva datos. `TaxonomyException` si la taxonomía no contiene la clave del campo. |
| `static Map<String, Map<String, Object>> of(ClassClassification classification)` | mapa inmutable | Las extensiones de todos los campos clasificados de una clase por ruta de campo, en el orden de los campos; los campos exentos no tienen. `IllegalArgumentException` si la clase tiene errores de clasificación, porque un campo con un error se describiría sin su clasificación. |
| `static String toJson(ClassClassification classification)` | `String` | Lo mismo como un objeto JSON cuyos miembros son las rutas de los campos y cuyos valores son las extensiones, para herramientas que no están escritas en Java. `IllegalArgumentException` si la clase tiene errores. |

Un campo derivado con `@DerivedFrom` y sin categoría propia tiene la extensión de su origen, y la ruta de una parte de
un [`@Composite`](../annotations/composite.md) es `campo.parte`, como en la clasificación.

## Uso

Un objeto de transferencia para la API y las extensiones de sus campos:

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
extensions.get("email").equals(extensions.get("emailDomain"));   // true: derivado del email
OpenApiExtension.toJson(view);
// {"firstName":{"category":"user.name.first","personal":true,"nature":"ORDINARY",...},"email":{...},...}
```

Para añadirlas a la descripción de una API, pasa cada mapa al método `addExtension` que tienen los esquemas de
swagger-core y springdoc, con la propiedad que lleva el campo. Por ejemplo, un customizer de springdoc:

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

springdoc y swagger-core son dependencias de la aplicación, no de Linneo: la biblioteca no tiene ninguna. Una
herramienta que lee el JSON lo toma de `OpenApiExtension.toJson(...)`, escrito donde pueda usarlo la compilación.

Véase también: [Exportación](README.md) · [Classifier](../classify/classifier.md) ·
[Inventario de datos](../classify/inventory.md) · [`@DerivedFrom`](../annotations/derived-from.md)
