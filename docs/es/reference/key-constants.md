---
title: "Constantes de claves"
parent: "Referencia"
nav_order: 5
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/Categories.java
  - src/main/java/io/github/devbpm/linneo/DataUses.java
  - src/main/java/io/github/devbpm/linneo/DataSubjects.java
  - tools/fideslang/update.py
-->

# Constantes de claves

`io.github.devbpm.linneo.Categories`, `DataUses` y `DataSubjects` — clases `public final` con constructor privado,
`@since 0.1.0`.

Cada una contiene un `public static final String` por clave de la [taxonomía estándar](standard-taxonomy.md):

| Clase | Claves de | Número |
|---|---|---|
| `Categories` | categorías de datos | 123 |
| `DataUses` | usos de datos (finalidades del tratamiento) | 63 |
| `DataSubjects` | sujetos de datos | 21 |

## Por qué constantes

- Son **constantes de compilación**, así que pueden usarse como valores de anotaciones:
  `@Category(Categories.USER_CONTACT_EMAIL)`.
- Una errata no compila, mientras que un literal mal escrito solo lo detecta el
  [`Classifier`](classify/classifier.md) (`UNKNOWN_CATEGORY`).
- El IDE las autocompleta, y el Javadoc de cada constante muestra el nombre y la descripción del término.

Las cadenas literales son igual de válidas donde se espera una clave; úsalas para las claves que añaden tus propias
[extensiones](extending.md), que no tienen constante. Las extensiones incluidas en Linneo traen sus propias
constantes: la [extensión universitaria](extensions/university.md) tiene `UniversityCategories`,
`UniversityDataUses` y `UniversityDataSubjects`.

```java
import io.github.devbpm.linneo.annotation.Category;

import static io.github.devbpm.linneo.Categories.*;

public class Person {
    @Category(USER_NAME)
    private String fullName;

    @Category("user.education.lab_notebook")   // añadida por una extensión de la aplicación: sin constante
    private String notebookTitle;
}
```

## Regla de nombres

El nombre de la constante es la clave en mayúsculas con cada punto sustituido por un guion bajo:

| Clave | Constante |
|---|---|
| `user` | `Categories.USER` |
| `user.contact.email` | `Categories.USER_CONTACT_EMAIL` |
| `user.health_and_medical.disability` | `Categories.USER_HEALTH_AND_MEDICAL_DISABILITY` |
| `essential.service.authentication` | `DataUses.ESSENTIAL_SERVICE_AUTHENTICATION` |
| `third_party_sharing` | `DataUses.THIRD_PARTY_SHARING` |
| `job_applicant` | `DataSubjects.JOB_APPLICANT` |

Cada clase enumera primero las claves de fideslang (en el orden de la fuente de fideslang) y después las añadidas por
Linneo. Los términos obsoletos conservan su constante (por ejemplo, `DataUses.PERSONALIZE_PROFILING`).

## Código generado

Las tres clases las **genera** `tools/fideslang/update.py` a partir de los recursos `fideslang-*.tsv` y
`linneo-*.tsv`; no las edites a mano. Tras añadir un término a un fichero `linneo-*.tsv`, vuelve a ejecutar el
script:

```bash
python -I tools/fideslang/update.py 3.1.4
```

Un test de la biblioteca comprueba que cada clave de la taxonomía estándar tiene una constante y cada constante una
clave. Consulta [Taxonomía estándar](standard-taxonomy.md#regenerar-desde-fideslang).

Véase también: [Taxonomía estándar](standard-taxonomy.md) · [`@Category`](annotations/category.md) ·
[Taxonomía](taxonomy.md) · curso: [Anotar modelos](../course/03-annotating-models.md)
