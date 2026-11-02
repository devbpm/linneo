---
title: "PostgreSQL Anonymizer"
parent: "Exportación"
grand_parent: "Referencia"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/export/postgresql/PostgresAnonymizer.java
  - src/main/java/io/github/devbpm/linneo/export/postgresql/package-info.java
-->

# PostgreSQL Anonymizer

`io.github.devbpm.linneo.export.postgresql.PostgresAnonymizer` — `public final class`, `@since 0.1.0`

Las reglas de enmascaramiento de [PostgreSQL Anonymizer](https://postgresql-anonymizer.readthedocs.io/) para las
columnas de los campos clasificados: las sentencias `SECURITY LABEL FOR anon` que hacen que las vistas enmascaradas y
los volcados anonimizados de la base de datos sigan una [`DisclosurePolicy`](../classify/disclosure.md), normalmente
para las copias de datos de producción que usan los entornos de pruebas. Linneo solo escribe las sentencias: la
aplicación elige la columna de cada campo y las ejecuta, y Linneo no se conecta a ninguna base de datos. Las instancias
son inmutables y seguras entre hilos.

## La regla de cada tratamiento

La política decide qué ocurre con el valor de cada campo ([`Treatment`](../classify/disclosure.md#treatment)), y el
anonimizador lo convierte en una regla:

| Tratamiento | Regla | Sentencia |
|---|---|---|
| `OMIT` | `MASKED WITH VALUE NULL`, o `MASKED WITH VALUE <value>` cuando se declara un valor para la categoría | `SECURITY LABEL FOR anon ON COLUMN <column> IS 'MASKED WITH VALUE NULL';` |
| `MASK` | `MASKED WITH FUNCTION <function>` cuando Linneo conoce una función para la categoría o un ancestro; si no, como `OMIT` | `... IS 'MASKED WITH FUNCTION anon.dummy_safe_email()';` |
| `CLEAR` | ninguna | `SECURITY LABEL FOR anon ON COLUMN <column> IS NULL;` |

- Un campo enmascarado se sustituye por un valor falso del mismo tipo cuando se conoce una función para él. Si no, se
  omite como uno omitido, `NULL` salvo que hayas declarado un valor: Linneo no conoce el tipo de la columna, y omitir
  el valor es la opción más estricta. Declara los enmascaramientos de tus otras columnas con
  [`withMask`](#miembros).
- Un valor omitido es `NULL`, que una columna `NOT NULL`, como un hash de contraseña, rechaza. Declara el valor que lo
  sustituye con [`withValue`](#miembros), por ejemplo `'*'`: una constante no dice nada del original. Igual que un
  enmascaramiento, un valor se busca por la clave de la categoría y luego por las de sus ancestros.
- Un campo que la política muestra tal cual recibe `IS NULL`, que **elimina** cualquier regla que tuviera la columna.
  Ejecutar de nuevo las sentencias tras un cambio de la clasificación, o de la política, pone todas las columnas al día.
- Una función se busca por la clave de la categoría, luego por la de su padre, y así hacia arriba en el árbol: un
  enmascaramiento declarado para `user.education` sirve a `user.education.grade` salvo que una clave más específica
  tenga el suyo.

## Miembros

| Miembro | Devuelve | Descripción |
|---|---|---|
| `static PostgresAnonymizer of(DisclosurePolicy policy)` | anonimizador | Las reglas que siguen una política, con las funciones de enmascaramiento de Linneo. `NullPointerException` si `policy` es `null`. |
| `PostgresAnonymizer withMask(String categoryKey, String function)` | anonimizador nuevo | Las mismas reglas con una función de enmascaramiento para los campos de una categoría y de las categorías que cuelgan de ella, salvo que una clave más específica tenga la suya. `function` es el SQL que sigue a `MASKED WITH FUNCTION`, como `anon.random_int_between(0, 10)` o `anon.partial(iban, 4, '******', 2)`; se le quitan los espacios sobrantes. `IllegalArgumentException` si está en blanco. |
| `Map<String, String> masks()` | mapa inmutable | Las funciones de enmascaramiento por clave de categoría, ordenadas por clave. |
| `PostgresAnonymizer withValue(String categoryKey, String value)` | anonimizador nuevo | Las mismas reglas con el valor que sustituye a un valor omitido de una categoría y de las categorías que cuelgan de ella, salvo que una clave más específica tenga el suyo, en lugar de `NULL`: lo que necesita una columna `NOT NULL`. `value` es el SQL que sigue a `MASKED WITH VALUE`, como `'*'` o `0`; se le quitan los espacios sobrantes. `IllegalArgumentException` si está en blanco. |
| `Map<String, String> values()` | mapa inmutable | Los valores que sustituyen a los omitidos por clave de categoría, ordenados por clave; una categoría sin valor es `NULL`. |
| `String rule(FieldClassification field)` | `String` | El texto de la etiqueta de un campo: `MASKED WITH FUNCTION ...` o `MASKED WITH VALUE ...` (`NULL` salvo que se declare otro), o `null` para un valor que la política muestra tal cual. |
| `String rule(InventoryField field, Taxonomy taxonomy)` | `String` | Lo mismo para un campo guardado, resuelto con la taxonomía; `null` para un campo exento. `TaxonomyException` si la taxonomía no contiene la clave. |
| `String label(String column, FieldClassification field)` | `String` | La sentencia `SECURITY LABEL FOR anon ON COLUMN ...` de un campo. `IllegalArgumentException` si `column` no es un identificador SQL cualificado. |
| `String label(String column, InventoryField field, Taxonomy taxonomy)` | `String` | La sentencia de un campo guardado. |
| `List<String> labels(ClassClassification classification, Function<String, String> columns)` | lista inmutable | Las sentencias de todos los campos de una clase, también los exentos (`IS NULL`), **una por cada columna**: primero las que quitan una regla y después los enmascaramientos, cada grupo en el orden del primer campo de su columna. `columns` da la columna de la ruta de un campo; `null` omite el campo. `IllegalArgumentException` si la clase tiene errores de clasificación, porque algunos campos no tendrían regla, o si una columna no es un identificador cualificado. |
| `List<String> labels(DataInventory inventory, String object, Taxonomy taxonomy, Function<String, String> columns)` | lista inmutable | Las sentencias de todos los campos de un objeto de un inventario de datos, también los exentos, una por cada columna, primero las supresiones y después los enmascaramientos, como arriba. `IllegalArgumentException` si el inventario no tiene ese objeto (una clave mal escrita no escribiría ninguna sentencia), o si una columna no es un identificador cualificado. |

`toString()` es `PostgresAnonymizer[<política>, <n> masks, <n> values]`.

### La columna

Una columna se escribe `table.column` o `schema.table.column`, y cada parte es un identificador simple o uno entre
comillas dobles (`"Students"."e-mail"`). Cualquier otra cosa, como `email` solo o `students.email; DROP TABLE students`,
se rechaza con `IllegalArgumentException`, de modo que una sentencia no pueda llevar más que la etiqueta.

Las sentencias escriben siempre las partes **entre comillas**, y una parte sin comillas se pasa a minúsculas, como la
lee PostgreSQL: `students.firstName` pasa a `"students"."firstname"` y `"Students"."e-mail"` se queda como está. Así
funciona una columna con el nombre de una palabra reservada, como `user`, y la sentencia nombra la misma columna que la
base de datos.

Una parte de más de 63 bytes (en UTF-8, sin sus comillas) se rechaza con `IllegalArgumentException`: PostgreSQL conserva
solo los 63 primeros bytes de un nombre, así que dos nombres así pueden ser la misma columna.

La etiqueta es una constante de cadena que se lee igual sea cual sea `standard_conforming_strings`: las comillas simples
dentro de la regla se duplican, y una regla con una barra invertida se escribe como cadena de escape (`E'...'`) con la
barra duplicada.

### Varios campos en una columna

Cuando varios campos se guardan en una misma columna, como un compuesto guardado como JSON, la columna recibe **una**
sentencia: `MASKED WITH VALUE NULL` salvo que todos se muestren tal cual, aunque tengan el mismo enmascaramiento, porque
un valor falso del tipo de un campo no sirve en una columna que contiene varios. Una columna cuyos campos se muestran
todos tal cual recibe `IS NULL`.

Las sentencias que quitan una regla (`IS NULL`) van **primero** y los enmascaramientos después, cada grupo en el orden
del primer campo de cada columna. Si dos grafías nombran una misma columna, la supresión se ejecuta antes que el
enmascaramiento, así que la regla de enmascaramiento es la que queda.

### Enmascaramientos estándar

Las funciones que conoce Linneo son las funciones `dummy_*` que recomienda la versión 2 de PostgreSQL Anonymizer:

| Categoría | Función |
|---|---|
| `user.account.username` | `anon.dummy_username()` |
| `user.contact.address.city` | `anon.dummy_city_name()` |
| `user.contact.address.country` | `anon.dummy_country_name()` |
| `user.contact.address.postal_code` | `anon.dummy_post_code()` |
| `user.contact.address.state` | `anon.dummy_state_name()` |
| `user.contact.address.street` | `anon.dummy_street_name()` |
| `user.contact.email` | `anon.dummy_safe_email()` |
| `user.contact.phone_number` | `anon.dummy_phone_number()` |
| `user.device.ip_address` | `anon.dummy_ip()` |
| `user.name` | `anon.dummy_name()` |
| `user.name.first` | `anon.dummy_first_name()` |
| `user.name.last` | `anon.dummy_last_name()` |

## Uso

Los campos de una clase, con la política para logs: los datos personales se enmascaran, los de categorías especiales y
los secretos se omiten, el resto se muestra.

```java
@DefaultCategory(value = SYSTEM_OPERATIONS, reason = "technical")
public class Student {

    @Category(USER_NAME_FIRST)
    private String firstName;

    @Category(USER_CONTACT_EMAIL)
    private String email;

    @Category(USER_HEALTH_AND_MEDICAL_DISABILITY)
    private Integer disabilityDegree;

    @Category(USER_EDUCATION_GRADE)
    private BigDecimal mark;

    @Category(USER_AUTHORIZATION_PASSWORD)
    private String passwordHash;

    private long version;

    @Exempt("rendering cache")
    private String cache;
}
```

```java
import io.github.devbpm.linneo.export.postgresql.PostgresAnonymizer;

ClassClassification student = classifier.classify(Student.class);

PostgresAnonymizer anonymizer = PostgresAnonymizer.of(DisclosurePolicy.forLogs());
List<String> statements = anonymizer.labels(student, path -> "students." + path);
statements.forEach(System.out::println);
```

```text
SECURITY LABEL FOR anon ON COLUMN "students"."version" IS NULL;
SECURITY LABEL FOR anon ON COLUMN "students"."cache" IS NULL;
SECURITY LABEL FOR anon ON COLUMN "students"."firstname" IS 'MASKED WITH FUNCTION anon.dummy_first_name()';
SECURITY LABEL FOR anon ON COLUMN "students"."email" IS 'MASKED WITH FUNCTION anon.dummy_safe_email()';
SECURITY LABEL FOR anon ON COLUMN "students"."disabilitydegree" IS 'MASKED WITH VALUE NULL';
SECURITY LABEL FOR anon ON COLUMN "students"."mark" IS 'MASKED WITH VALUE NULL';
SECURITY LABEL FOR anon ON COLUMN "students"."passwordhash" IS 'MASKED WITH VALUE NULL';
```

`mark` es un dato personal, así que la política lo enmascara, pero Linneo no conoce ninguna función para
`user.education.grade` y lo enmascara con `NULL`. Declara una para tu columna y la regla cambia; un enmascaramiento de
una categoría sirve a sus descendientes. Una columna `NOT NULL`, como `passwordHash`, rechaza `NULL`: declara el valor
que lo sustituye con `withValue`:

```java
import io.github.devbpm.linneo.export.postgresql.PostgresAnonymizer;

PostgresAnonymizer custom = PostgresAnonymizer.of(DisclosurePolicy.forLogs())
        .withMask(USER_EDUCATION, "anon.random_int_between(0, 10)")
        .withMask(USER_CONTACT_EMAIL, "anon.partial(email, 1, '***', 4)")
        .withValue(USER_AUTHORIZATION_PASSWORD, "'*'");

ClassClassification student = classifier.classify(Student.class);
FieldClassification mark = student.field("mark").orElseThrow();
String statement = custom.label("public.students.mark", mark);
// SECURITY LABEL FOR anon ON COLUMN "public"."students"."mark" IS 'MASKED WITH FUNCTION anon.random_int_between(0, 10)';
custom.rule(student.field("passwordHash").orElseThrow());   // MASKED WITH VALUE '*'
```

Las sentencias de una clasificación guardada son las mismas, lo que permite que un back office sin las clases del modelo
las escriba a partir de un [inventario de datos](../classify/inventory.md):

```java
import io.github.devbpm.linneo.export.postgresql.PostgresAnonymizer;

DataInventory inventory = classifier.inventory(List.of(Student.class), type -> "students");

PostgresAnonymizer anonymizer = PostgresAnonymizer.of(DisclosurePolicy.forLogs());
List<String> statements = anonymizer.labels(inventory, "students", taxonomy, path -> "students." + path);
```

Ejecuta las sentencias con la herramienta de base de datos de la aplicación, por ejemplo en el script que prepara la
copia de los datos de producción. Una política para otro canal (una exportación, una API) da otras reglas: constrúyela
con [`DisclosurePolicy.builder()`](../classify/disclosure.md#builder).

Véase también: [Exportación](README.md) · [Política de divulgación](../classify/disclosure.md) ·
[Inventario de datos](../classify/inventory.md)
