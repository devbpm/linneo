---
title: "Curso"
nav_order: 5
permalink: "/es/course/"
---
<!--
doc-sources:
  - README.md
  - pom.xml
-->

# Curso de formación de Linneo

**Clasifica el modelo de datos de tu organización, verifícalo en los tests y saca partido del resultado.**

Este curso enseña a usar [Linneo](https://github.com/devbpm/linneo#readme), una pequeña biblioteca Java que lleva una taxonomía de
clasificación de datos a tu código. Empezarás por los conceptos jurídicos y de seguridad que hay detrás de la
clasificación de datos, conocerás la taxonomía que incluye Linneo, anotarás un modelo de datos realista, harás que
la compilación falle cuando algo quede sin clasificar, usarás la clasificación para construir un inventario de
datos, decidir qué puede aparecer en los logs y publicarlo todo en un catálogo de datos —OpenMetadata, DataHub o
Apache Atlas— y, por último, declararás las actividades de tratamiento que usan los datos y generarás el registro de
actividades de tratamiento del RGPD.

Todos los módulos usan el mismo ejemplo: el modelo de datos de una **pequeña universidad**. Empieza con un
`Student` y un `MenuGroup` y crece, módulo a módulo, con `Enrollment`, `Grade`, `Staff`, `Document` con sus
`Attachment`, una extensión de la taxonomía propia de la universidad y las actividades de tratamiento que los usan:
matrícula, admisión, verificación de antecedentes del personal y boletín de antiguos alumnos.

> ¿Buscas el contrato exacto de un tipo o una anotación? Está en la [referencia](../reference/README.md). El curso
> explica el *porqué* y el *cómo*; la referencia establece el *qué*.

## A quién va dirigido

- **Desarrolladores Java** que mantienen una aplicación con un modelo de datos persistente (entidades JPA, records,
  clases normales) con datos de personas —estudiantes, personal, solicitantes— y deben declarar qué contiene cada
  campo.
- **Delegados de protección de datos, responsables de seguridad y analistas** que necesitan entender qué conceptos
  del RGPD expresa la clasificación y cómo leer sus resultados. El módulo 1 y las notas de protección de datos de
  los demás módulos están escritos para vosotros; podéis saltaros las partes con más código.

## Objetivos

Al terminar el curso serás capaz de:

1. Explicar las cuatro naturalezas jurídicas que usa Linneo (datos personales ordinarios, categorías especiales,
   datos penales y datos no personales) y las facetas de confidencialidad, integridad e identificabilidad.
2. Recorrer la taxonomía, elegir la clave adecuada para un campo y justificar la elección.
3. Anotar un modelo de datos con `@Category`, `@DefaultCategory`, `@CategoryByRow` y `@Exempt`.
4. Escribir un test que haga fallar la compilación cuando el modelo no esté clasificado por completo y
   correctamente, y corregir cada tipo de infracción que notifique.
5. Usar la clasificación en tiempo de ejecución: listar los campos personales, generar un inventario de datos y
   tomar decisiones de enmascaramiento.
6. Extender la taxonomía con términos propios y protección más estricta mediante una `TaxonomyExtension`.
7. Publicar la taxonomía y las etiquetas de tus columnas en OpenMetadata.
8. Publicar la misma taxonomía como glosario de negocio de DataHub o como clasificaciones de Apache Atlas, y calcular
   los términos y las clasificaciones de tus columnas.
9. Declarar actividades de tratamiento con sus bases jurídicas, comprobarlas en un test y generar el registro de
   actividades de tratamiento en Markdown y JSON.

## Requisitos previos

- **Java 21 o posterior.** Linneo es un módulo JPMS con nombre (`io.github.devbpm.linneo`) y no tiene dependencias
  en tiempo de ejecución.
- **Maven o Gradle**, con la dependencia:

  ```xml
  <dependency>
      <groupId>io.github.devbpm</groupId>
      <artifactId>linneo</artifactId>
      <version>0.1.0</version>
  </dependency>
  ```

  ```kotlin
  implementation("io.github.devbpm:linneo:0.1.0")
  ```

- **JUnit 5** (o cualquier framework de tests) para los módulos 4 y 9.
- Conocimientos prácticos de anotaciones Java, records y herencia de clases. Saber JPA ayuda, pero no es
  imprescindible: el clasificador trabaja con clases normales.
- Solo para los módulos 7 y 8: es útil tener acceso a un servidor OpenMetadata, DataHub o Apache Atlas, pero todos
  los ejercicios pueden hacerse sin conexión.

## Módulos

| # | Módulo | Aprenderás a… | Tiempo |
|---|---|---|---|
| 1 | [Por qué clasificar los datos](01-why-classify.md) | Explicar las naturalezas del RGPD, la confidencialidad, la integridad, la identificabilidad y los secretos; entender el enfoque de fallo seguro | 45 min |
| 2 | [La taxonomía](02-the-taxonomy.md) | Leer claves con puntos, entender la protección heredada y explorar la taxonomía con la API `Taxonomy` | 60 min |
| 3 | [Anotar los modelos](03-annotating-models.md) | Anotar campos y clases, elevar niveles, declarar la identificabilidad y tratar records, herencia y datos por fila | 90 min |
| 4 | [Verificar en los tests](04-verifying-in-tests.md) | Hacer fallar la compilación ante campos sin clasificar o mal clasificados; leer y corregir cada infracción | 60 min |
| 5 | [Usar la clasificación](05-using-the-classification.md) | Listar campos personales, generar un inventario de datos y un informe tipo RAT, decidir qué puede ir a los logs | 60 min |
| 6 | [Extender la taxonomía](06-extending-the-taxonomy.md) | Añadir términos y protección más estricta con una `TaxonomyExtension` registrada mediante `ServiceLoader`; aplicar la extensión universitaria que incluye Linneo | 60 min |
| 7 | [OpenMetadata](07-openmetadata.md) | Publicar clasificaciones y etiquetas, calcular las etiquetas de cada columna y personalizar los nombres | 45 min |
| 8 | [DataHub y Apache Atlas](08-datahub-and-atlas.md) | Publicar un glosario de DataHub y tipos de clasificación de Atlas, calcular los términos y clasificaciones de cada columna y comparar los tres catálogos | 60 min |
| 9 | [Actividades de tratamiento y el registro](09-processing-activities.md) | Declarar actividades de tratamiento con sus bases jurídicas, distinguir errores de avisos, verificarlas en un test y generar el registro de actividades de tratamiento | 60 min |

Total: unas 9 horas. Los módulos 1 y 2 son conceptuales; del 3 al 9 son prácticos. Los perfiles de protección de
datos pueden seguir por su cuenta los módulos 1, 2, 5 y 9.

## Cómo hacer los ejercicios

1. Crea un proyecto Maven o Gradle vacío con Java 21+, añade la dependencia de Linneo y JUnit 5.
2. Crea un paquete para el ejemplo, por ejemplo `edu.example.university`. Cada módulo indica qué clases añadir o
   cambiar; el código de los módulos compila con Linneo 0.1.0.
3. Haz los ejercicios antes de abrir las soluciones. Cada módulo termina con un bloque desplegable de
   **Soluciones**.
4. Ejecuta el código. La mayoría de los ejercicios piden predecir un resultado (una infracción, una lista de
   etiquetas, un nivel de protección) y comprobarlo después: los mensajes de la biblioteca forman parte de lo que
   aprendes a leer.
5. Cada módulo tiene también un breve **cuestionario de autoevaluación**. Si fallas más de una respuesta, vuelve a
   leer la sección correspondiente antes de seguir.

Convenciones del curso:

- Los ejemplos usan `import static io.github.devbpm.linneo.Categories.*;` para que las claves se lean como
  `USER_CONTACT_EMAIL` en lugar de `Categories.USER_CONTACT_EMAIL`.
- Las anotaciones JPA (`@Entity`, `@Id`, `@Column`...) se omiten en la mayoría de los ejemplos para centrarse en
  Linneo; tus entidades reales las mantienen. Linneo nunca las mira.
- Los identificadores de código y las claves de la taxonomía no se traducen. Los mensajes de error de Linneo están
  en inglés y se muestran tal cual.
- Las salidas que aparecen en el curso son las salidas reales de Linneo 0.1.0 para el código mostrado.

---

Empieza por el [Módulo 1 — Por qué clasificar los datos](01-why-classify.md) →
