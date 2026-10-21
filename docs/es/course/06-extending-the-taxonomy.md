---
title: "6. Extender la taxonomía"
parent: "Curso"
nav_order: 6
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/TaxonomyBuilder.java
  - src/main/java/io/github/devbpm/linneo/TaxonomyExtension.java
  - src/main/java/io/github/devbpm/linneo/CategoryDraft.java
  - src/main/java/io/github/devbpm/linneo/TermDraft.java
  - src/main/java/io/github/devbpm/linneo/extension/university/UniversityTaxonomy.java
-->

# Módulo 6 — Extender la taxonomía

## Objetivos de aprendizaje

Al terminar este módulo serás capaz de:

- decidir cuándo una extensión es la respuesta adecuada (y cuándo basta con una clave estándar);
- escribir una `TaxonomyExtension` que añada categorías, usos de datos e interesados;
- hacer más estrictas categorías existentes con `refineCategory`, añadir atributos y declarar obsoletos tus propios
  términos;
- registrar la extensión con `ServiceLoader`, en el class path o en el module path;
- aplicar la extensión universitaria que incluye Linneo y combinarla con la tuya;
- predecir qué extensiones rechaza el builder y leer sus mensajes de error.

## Cuándo extender

La taxonomía estándar cubre mucho, pero cada organización tiene sus propios datos. La universidad necesita:

- una clave para **cuadernos de laboratorio**: nada en `user.education.*` encaja;
- un atributo de **conservación** para los cuadernos de laboratorio, que deben guardarse de forma permanente;
- una protección más estricta para los **documentos de identidad** en toda la universidad: su política de seguridad
  considera muy grave (`HIGH`), y no grave (`MEDIUM`), revelar números de identificación nacional;
- un interesado para los **investigadores visitantes**.

Nada de esto exige cambiar Linneo. Una `TaxonomyExtension` lo aporta a la taxonomía al construirla.

Antes de extender, revisa la [taxonomía estándar](../reference/standard-taxonomy.md): una clave existente que sea
cierta para el dato siempre es mejor que una nueva, porque otras herramientas y organizaciones ya la entienden. En una
universidad, revisa también la [extensión universitaria](../reference/extensions/university.md) que incluye Linneo
(ver [más abajo](#la-extensión-universitaria-incluida-en-linneo)): ya tiene claves para tesis, asistencia,
expedientes disciplinarios y mucho más.

## Escribir la extensión

```java
package edu.example.university;

import io.github.devbpm.linneo.Categories;
import io.github.devbpm.linneo.Confidentiality;
import io.github.devbpm.linneo.TaxonomyBuilder;
import io.github.devbpm.linneo.TaxonomyExtension;

public final class CampusTaxonomy implements TaxonomyExtension {

    @Override
    public String name() {
        return "campus";
    }

    @Override
    public void contribute(TaxonomyBuilder builder) {
        builder.category("user.education.lab_notebook", "Cuaderno de laboratorio",
                        "Cuadernos de laboratorio del interesado: experimentos, mediciones y resultados.",
                        c -> c.attribute("retention", "permanent"))
               .category("user.education.lab_journal", "Diario de laboratorio",
                        "Nombre anterior de la clave del cuaderno de laboratorio.",
                        c -> c.deprecated("2.0", "user.education.lab_notebook"))
               .refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
               .dataSubject("visiting_researcher", "Investigador visitante", "Investigador de otra institución.");
    }
}
```

### `name()`

Un nombre corto y estable. Se registra como **origen** de todos los términos que añade la extensión, de modo que los
informes distinguen los términos de «campus» de los de «fideslang» y «linneo». Esos dos nombres están
reservados: una extensión llamada `"fideslang"` o `"linneo"` (o con el nombre vacío) se rechaza con
`Invalid extension name 'linneo' in edu.example.university.CampusTaxonomy`. Aplicar dos veces la misma clase de extensión
no tiene efecto la segunda vez, pero dos clases distintas con el mismo nombre se rechazan (véase más abajo). El nombre `"university"` lo usa la
[extensión incluida en Linneo](#la-extensión-universitaria-incluida-en-linneo); no lo reutilices.

### Añadir categorías

`category(key, name, description)` añade una categoría que **hereda toda su protección** de su madre.
`category(key, name, description, customizer)` permite declarar cosas en un `CategoryDraft`:

| Método de `CategoryDraft` | Declara |
|---|---|
| `nature(LegalNature)` | la naturaleza jurídica; debe admitirla la de la madre (`LegalNature.admits`) |
| `confidentiality(Confidentiality)` | la confidencialidad; nunca inferior a la de la madre |
| `integrity(Integrity)` | la integridad; nunca inferior a la de la madre |
| `secret()` | marca la categoría, y por tanto todas sus hijas, como secreta |
| `attribute(name, value)` | un atributo definido por la aplicación, que heredan las hijas que no fijan el suyo |
| `since(version)` | la versión de tu extensión que introdujo el término |
| `deprecated(sinceVersion, replacedBy)` | declara obsoleto el término; `replacedBy` puede ser `null` |

Con esta extensión, `user.education.lab_notebook` queda así:

```
nature=ORDINARY, confidentiality=MEDIUM, integrity=HIGH, secret=false,
attributes={retention=permanent}, origin=campus
```

Los niveles vienen de `user.education`; el atributo es de la extensión. Cualquier hija que añadas más adelante bajo
`user.education.lab_notebook` hereda `retention=permanent` salvo que fije su propio valor. En el inventario del módulo 5
puedes imprimirlo con `field.category().attribute("retention")`.

La clave debe estar formada por segmentos en minúsculas separados por puntos (`[a-z][a-z0-9_]*`) y su madre debe
existir. Añade los términos como **hijos de términos existentes**: así resultan comprensibles para cualquiera que
conozca el árbol estándar.

### Hacer más estrictas categorías existentes

`refineCategory(key, customizer)` modifica una categoría que ya está en el builder: estándar o añadida por una
extensión anterior. Puede subir niveles, convertir una naturaleza `ORDINARY` en otra más estricta, marcarla como
secreta, añadir atributos o declararla obsoleta. Nunca puede debilitarla:

```java
.refineCategory(Categories.USER_GOVERNMENT_ID, c -> c.confidentiality(Confidentiality.HIGH))
```

A partir de aquí, `user.government_id.passport_number`, `...national_identification_number`,
`...social_security_number` y todas las demás hijas heredan `HIGH`. Refinar no cambia el origen del término:
`user.government_id` sigue siendo `fideslang`.

### Usos de datos e interesados

`dataUse(key, name, description)` y `dataSubject(key, name, description)` (cada uno con un personalizador
`TermDraft` opcional para `since` y `deprecated`) añaden términos a las otras dos jerarquías. No tienen facetas de
protección.

### Declarar obsoletos tus propios términos

Supón que la universidad usó `user.education.lab_journal` en una primera versión y después lo renombró a
`user.education.lab_notebook`. Nunca borres ni reutilices una clave que las aplicaciones puedan haber guardado; declárala
obsoleta:

```java
.category("user.education.lab_journal", "Diario de laboratorio",
        "Nombre anterior de la clave del cuaderno de laboratorio.",
        c -> c.deprecated("2.0", "user.education.lab_notebook"))
```

Los campos que siguen anotados con la clave antigua continúan funcionando, se clasifican con
`user.education.lab_notebook` y producen el aviso que viste en el módulo 4:

```
WARNING DEPRECATED_CATEGORY edu.example.university.LabNotebook.file: data category 'user.education.lab_journal' is deprecated; use 'user.education.lab_notebook'
```

### Constantes para tus claves

`Categories` solo cubre la taxonomía estándar. Da también constantes de compilación a tus claves, para que funcionen
en las anotaciones y las erratas no compilen:

```java
public final class CampusCategories {
    public static final String USER_EDUCATION_LAB_NOTEBOOK = "user.education.lab_notebook";

    private CampusCategories() {
    }
}
```

```java
@Category(CampusCategories.USER_EDUCATION_LAB_NOTEBOOK)
private String notebookTitle;
```

## Registrar la extensión

`Taxonomy.load()` aplica todas las extensiones que encuentra `java.util.ServiceLoader`. Registra la tuya de una de
estas dos formas.

**Class path**: un fichero llamado `META-INF/services/io.github.devbpm.linneo.TaxonomyExtension` (en Maven, dentro de
`src/main/resources`) con el nombre completo de la clase:

```
edu.example.university.CampusTaxonomy
```

**Module path**: una cláusula `provides` en tu `module-info.java`:

```java
module edu.example.university {
    requires io.github.devbpm.linneo;

    provides io.github.devbpm.linneo.TaxonomyExtension with edu.example.university.CampusTaxonomy;
}
```

Después, en todas partes (aplicación y tests):

```java
Taxonomy taxonomy = Taxonomy.load();   // estándar + todas las extensiones registradas
```

`load()` busca las extensiones con el class loader de contexto del hilo (o con el de Linneo si no lo hay) y las aplica
**ordenadas por nombre**, de modo que el resultado no depende del orden del class path. Si necesitas otro class
loader, usa `Taxonomy.builder().withStandard().withExtensions(classLoader).build()`.

También puedes aplicar una extensión de forma explícita, sin `ServiceLoader`, algo práctico en un test unitario de la
propia extensión:

```java
Taxonomy taxonomy = Taxonomy.builder().withStandard().extend(new CampusTaxonomy()).build();
```

## La extensión universitaria incluida en Linneo

Linneo incluye una extensión opcional para la educación superior, `UniversityTaxonomy`, en el paquete
`io.github.devbpm.linneo.extension.university`. Añade 33 categorías de datos (asistencia, situación académica,
adaptaciones, solicitudes de admisión y su evaluación, notas finales, reconocimiento de créditos, títulos, prácticas,
valoraciones de renta, exenciones de tasas, donaciones…), 27 usos de datos (entre ellos dos raíces nuevas,
`advancement` y `research`) y 13 interesados (futuro estudiante, doctorando, estudiante de intercambio, donante, autor
de cartas de referencia…). La [referencia](../reference/extensions/university.md) enumera todos los
términos con su protección.

**No está registrada** en `ServiceLoader`, así que `Taxonomy.load()` nunca la aplica. Aplícala junto a tu propia
extensión:

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new UniversityTaxonomy())   // io.github.devbpm.linneo.extension.university
        .extend(new CampusTaxonomy())
        .build();
```

`taxonomy.toString()` imprime `Taxonomy[160 data categories, 90 data uses, 35 data subjects]`: las 125 categorías
estándar, 33 de `university` y 2 de `campus`. Sus claves tienen constantes en `UniversityCategories`,
`UniversityDataUses` y `UniversityDataSubjects`, así que funcionan en las anotaciones igual que las estándar:

```java
@Category(UniversityCategories.USER_EDUCATION_ATTENDANCE)
private int absences;
```

Dos extensiones se combinan sin problemas cuando:

- **sus nombres son distintos.** El nombre es el origen de cada término (`university`, `campus`) y la identidad de
  la extensión: `extend` lanza una `TaxonomyException` cuando llega una clase distinta con el nombre de una ya
  aplicada: `The extensions io.github.devbpm.linneo.extension.university.UniversityTaxonomy and
  edu.example.university.CampusTaxonomy are both named 'university'`. (Aplicar dos veces exactamente la misma clase es
  inocuo, y por eso una extensión puede aplicarse de forma explícita y estar además registrada como servicio.)
- **añaden claves distintas.** Añadir una clave que la otra extensión ya añadió falla con
  `Duplicate key 'user.education.thesis'`. Por eso `CampusTaxonomy` añade cuadernos de laboratorio y no tesis.
- **los refinamientos van después del término que refinan.** Tu extensión solo puede hacer más estricto un término de
  `UniversityTaxonomy` si se aplica después; si no, `refineCategory` falla con
  `Cannot refine unknown data category 'user.education.attendance'`. Con `extend` eliges tú el orden. `load()` ordena
  por nombre, así que para aplicar primero la extensión incluida y después todas las registradas, escribe
  `Taxonomy.builder().withStandard().extend(new UniversityTaxonomy()).withExtensions().build()`.

Para que `Taxonomy.load()` incluya la extensión de Linneo, regístrala en tu aplicación; la referencia de
[Extensiones](../reference/extensions/README.md#registrar-una-extensión-incluida) muestra cómo en el class path y en
el module path.

## Qué rechaza `build()`

El builder es donde se aplican las reglas del módulo 2. Algunos problemas se detectan en cuanto llamas a un método del
builder y el resto al construir la taxonomía; en cualquier caso, `Taxonomy.load()` lanza una `TaxonomyException`, de
modo que una extensión defectuosa detiene la aplicación (o el test) en lugar de producir una taxonomía más débil. Los
mensajes son:

| Problema | Mensaje de ejemplo | Cuándo |
|---|---|---|
| Clave no válida | `Invalid key 'user.education.LabNotebook': use lower-case segments separated by dots` | en `category(...)` |
| Clave duplicada | `Duplicate key 'user.education.grade'` | en `category(...)` |
| Refinar una clave que (aún) no existe | `Cannot refine unknown data category 'user.education.lab_notebook'` | en `refineCategory(...)` |
| El refinamiento baja un nivel declarado por la categoría | `Cannot lower the confidentiality of 'user.education' from MEDIUM to LOW` | en `refineCategory(...)` |
| El refinamiento cambia la naturaleza a una no admitida | `Cannot change the nature of 'user.health_and_medical' from SPECIAL_CATEGORY to ORDINARY` | en `refineCategory(...)` |
| Falta la madre | `The parent 'user.education.lab_notebook' of data category 'user.education.lab_notebook.entry' does not exist` | `build()` |
| Raíz sin todas las facetas | `Root data category 'research' must declare its nature, confidentiality and integrity` | `build()` |
| Naturaleza no admitida por la madre | `Data category 'user.education.stats' declares nature NONE, which its parent 'user.education' (ORDINARY) does not admit` | `build()` |
| Nivel inferior al de la madre | `Data category 'user.education.grade' declares confidentiality LOW, lower than its parent's MEDIUM` | `build()` |
| La sustituta no existe | `The replacement 'user.education.new' of data category 'user.education.old' does not exist` | `build()` |
| Las sustituciones forman un ciclo | `The replacements of data category 'user.education.a' form a cycle` | `build()` |

Dos casos merecen atención:

- **Refinar una categoría que solo hereda un nivel.** `user.education.grade` no declara confidencialidad propia, así
  que `refineCategory(USER_EDUCATION_GRADE, c -> c.confidentiality(LOW))` lo acepta `refineCategory`, pero después
  `build()` la compara con la madre y falla («declares confidentiality LOW, lower than its parent's MEDIUM»). De un
  modo u otro, no se puede bajar.
- **Subir una madre por encima de lo que declara una hija.** `refineCategory(USER_DEMOGRAPHIC, c ->
  c.confidentiality(HIGH))` falla al construir con `Data category 'user.demographic.date_of_birth' declares
  confidentiality MEDIUM, lower than its parent's HIGH`, porque varias hijas de `user.demographic` declaran `MEDIUM`
  de forma explícita. Refina también esas hijas, o refina solo las claves concretas que te interesan.

El orden importa en `refineCategory`: la clave ya debe estar en el builder. Los términos estándar siempre lo están; los
de otra extensión solo si el nombre de esa extensión va antes que el tuyo en orden alfabético.

## Errores frecuentes

- **Olvidar registrar la extensión** (o registrarla solo en los recursos de test). La aplicación falla entonces con
  `UNKNOWN_CATEGORY` o, peor aún, usa `Taxonomy.standard()` y nunca ve los niveles más estrictos.
- **Usar `Taxonomy.standard()`** donde se necesite la extensión: nunca incluye extensiones.
- **Intentar bajar un nivel** para toda la organización. No es posible por diseño; los niveles solo se endurecen.
- **Inventar raíces nuevas.** Añade hijos de términos existentes. (El builder acepta una raíz nueva que declare
  naturaleza, confidencialidad e integridad, pero nadie más la entenderá.)
- **Renombrar una clave** en lugar de declararla obsoleta con una sustituta.
- **Usar `"linneo"` o `"fideslang"` como nombre de la extensión**, o `"university"`, que usa la extensión incluida.
- **Añadir una clave que ya tiene la extensión universitaria incluida** en lugar de aplicarla.

## Ejercicios

**Ejercicio 6.1 — Préstamos de biblioteca.** Añade `user.education.library_loan` («Préstamo de biblioteca», «Libros y
otros materiales que el interesado ha tomado en préstamo de la biblioteca.») a `CampusTaxonomy` sin declarar ninguna
faceta. ¿Cuáles son su naturaleza, confidencialidad,
integridad y origen efectivos? Compruébalo con `requireCategory`.

**Ejercicio 6.2 — Direcciones postales.** La universidad decide que las direcciones postales de estudiantes y
personal tienen confidencialidad `MEDIUM`. Añade el refinamiento. ¿Funcionará `build()`? ¿Qué claves cambian?

**Ejercicio 6.3 — Predice.** Para cada cuerpo de extensión, predice si
`Taxonomy.builder().withStandard().extend(...).build()` funciona y, si no, el mensaje:

1. `builder.category("user.education.lab_notebook.entry", "Entrada", "Una entrada de un cuaderno de laboratorio.");` (solo)
2. `builder.category("user.education.special_needs", "Necesidades especiales", "Adaptaciones para estudiantes con discapacidad.", c -> c.nature(LegalNature.SPECIAL_CATEGORY));`
3. `builder.refineCategory(Categories.USER_HEALTH_AND_MEDICAL, c -> c.nature(LegalNature.ORDINARY));`
4. `builder.refineCategory(Categories.USER_EDUCATION_TEST_SCORE, CategoryDraft::secret);`

**Ejercicio 6.4 — Registrar y probar.** Registra `CampusTaxonomy` con `ServiceLoader` y escribe un test que
compruebe que `Taxonomy.load()` contiene `user.education.lab_notebook` con origen `"campus"` y que
`user.government_id.passport_number` tiene confidencialidad `HIGH`.

**Ejercicio 6.5 — Combinar.** Construye una taxonomía con los términos estándar, la `UniversityTaxonomy` incluida y
después `CampusTaxonomy`.

1. ¿Qué imprime `toString()`?
2. ¿Cuál es el origen de `user.education.thesis`, y el de `user.education.lab_notebook`?
3. ¿Qué ocurre si `CampusTaxonomy` también añade `user.education.thesis`?
4. ¿Y si `CampusTaxonomy.name()` devolviera `"university"`?

## Cuestionario de autoevaluación

1. ¿Qué se registra como origen de un término añadido por una extensión?
2. ¿Cambia `refineCategory` el origen de un término?
3. ¿En qué orden aplica `Taxonomy.load()` las extensiones?
4. ¿Puede una extensión hacer `PUBLIC` la clave `user.contact.email`?
5. ¿Dónde se pone el registro de `ServiceLoader` en el class path?
6. ¿Incluye `Taxonomy.load()` la extensión universitaria que trae Linneo?

## Para saber más

- [Referencia — Extender la taxonomía](../reference/extending.md)
- [Referencia — Extensiones](../reference/extensions/README.md) y la
  [extensión universitaria](../reference/extensions/university.md)
- [Referencia — Taxonomía](../reference/taxonomy.md)
- [Referencia — Taxonomía estándar](../reference/standard-taxonomy.md)

---

← Anterior: [Módulo 5 — Usar la clasificación](05-using-the-classification.md) · Siguiente:
[Módulo 7 — OpenMetadata](07-openmetadata.md) →

<details markdown="block">
<summary><strong>Soluciones</strong></summary>

**Ejercicio 6.1**

```java
builder.category("user.education.library_loan", "Préstamo de biblioteca",
        "Libros y otros materiales que el interesado ha tomado en préstamo de la biblioteca.");
```

Efectivos: `ORDINARY`, `MEDIUM`, `HIGH` (de `user.education`), no secreta, origen `"campus"`.

**Ejercicio 6.2**

```java
.refineCategory(Categories.USER_CONTACT_ADDRESS, c -> c.confidentiality(Confidentiality.MEDIUM))
```

Funciona: `user.contact.address` y sus hijas (`city`, `country`, `postal_code`, `state`, `street`) no declaran
confidencialidad, así que todas heredan `MEDIUM`. `user.contact.email` y las demás claves de contacto no cambian.

**Ejercicio 6.3**

1. Falla al construir: `The parent 'user.education.lab_notebook' of data category
   'user.education.lab_notebook.entry' does not exist` (salvo que también se añada `user.education.lab_notebook`).
2. Falla al construir: `ORDINARY` admite `SPECIAL_CATEGORY`, pero los datos de categoría especial deben tener la
   máxima protección y la clave hereda confidencialidad `MEDIUM` de `user.education`: `Data category
   'user.education.special_needs' is SPECIAL_CATEGORY data and must have HIGH confidentiality and integrity, not
   MEDIUM and HIGH`. Declarar `c.nature(LegalNature.SPECIAL_CATEGORY).confidentiality(Confidentiality.HIGH)` hace que
   funcione.
3. Falla en `refineCategory`: `Cannot change the nature of 'user.health_and_medical' from SPECIAL_CATEGORY to
   ORDINARY`.
4. Funciona: las puntuaciones de pruebas pasan a ser secretas (una elección extraña, pero más estricta, así que se
   permite).

**Ejercicio 6.4**

`src/main/resources/META-INF/services/io.github.devbpm.linneo.TaxonomyExtension`:

```
edu.example.university.CampusTaxonomy
```

(o `provides io.github.devbpm.linneo.TaxonomyExtension with edu.example.university.CampusTaxonomy;` en
`module-info.java`), y:

```java
@Test
void campusExtensionIsLoaded() {
    Taxonomy taxonomy = Taxonomy.load();
    assertEquals("campus", taxonomy.requireCategory("user.education.lab_notebook").info().origin());
    assertEquals(Confidentiality.HIGH,
            taxonomy.requireCategory(Categories.USER_GOVERNMENT_ID_PASSPORT_NUMBER).confidentiality());
}
```

**Ejercicio 6.5**

```java
Taxonomy taxonomy = Taxonomy.builder()
        .withStandard()
        .extend(new UniversityTaxonomy())
        .extend(new CampusTaxonomy())
        .build();
```

1. `Taxonomy[160 data categories, 90 data uses, 35 data subjects]`: 125 + 33 + 2 categorías, 63 + 27 usos y
   21 + 13 + 1 interesados. Aplicar las dos extensiones en el orden contrario da la misma taxonomía.
2. `university` y `campus`.
3. `extend(new CampusTaxonomy())` lanza `TaxonomyException` con `Duplicate key 'user.education.thesis'`.
4. `extend(new CampusTaxonomy())` lanza `TaxonomyException` con `The extensions
   io.github.devbpm.linneo.extension.university.UniversityTaxonomy and edu.example.university.CampusTaxonomy are both
   named 'university'`: dos clases distintas no pueden compartir nombre, y `CampusTaxonomy.contribute` no llega a
   ejecutarse.

**Cuestionario**

1. El `name()` de la extensión.
2. No; conserva el origen de quien añadió el término.
3. Ordenadas por nombre.
4. No. `user.contact.email` hereda `LOW` de `user`; `PUBLIC` es inferior, así que `build()` falla.
5. En `META-INF/services/io.github.devbpm.linneo.TaxonomyExtension` (en Maven, dentro de `src/main/resources`).
6. No. El módulo de Linneo no ofrece ninguna extensión como servicio; aplícala con
   `extend(new UniversityTaxonomy())` o regístrala en tu aplicación.

</details>
