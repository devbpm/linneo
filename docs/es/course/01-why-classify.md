---
title: "1. Por qué clasificar los datos"
parent: "Curso"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/LegalNature.java
  - src/main/java/io/github/devbpm/linneo/Confidentiality.java
  - src/main/java/io/github/devbpm/linneo/Integrity.java
  - src/main/java/io/github/devbpm/linneo/Identifiability.java
-->

# Módulo 1 — Por qué clasificar los datos

## Objetivos de aprendizaje

Al terminar este módulo serás capaz de:

- explicar por qué una organización necesita saber, campo a campo, qué contiene su modelo de datos;
- distinguir las cuatro naturalezas jurídicas que usa Linneo: datos personales ordinarios, categorías especiales
  de datos (art. 9 RGPD), datos relativos a condenas e infracciones penales (art. 10 RGPD) y datos no personales;
- describir la confidencialidad y la integridad como dos escalas independientes, y la identificabilidad como una
  propiedad de cómo se almacena un valor;
- reconocer los secretos y explicar por qué requieren un tratamiento especial;
- explicar qué significa «fallo seguro» (*fail closed*) en una clasificación y por qué Linneo está diseñado así.

## El problema

Nuestro ejemplo es una universidad. Su sistema de gestión académica tiene una tabla `Student`, matrículas,
calificaciones, fichas del personal, documentos subidos por los estudiantes y mucha configuración. Antes o después
alguien pregunta cosas como:

- *«¿Qué tablas contienen datos de salud?»* (una evaluación de impacto, la notificación de una brecha)
- *«¿Podemos enviar esta tabla al equipo de analítica?»* (limitación de la finalidad, minimización)
- *«¿Qué debe decir el registro de actividades de tratamiento sobre el proceso de calificación?»* (art. 30 RGPD)
- *«¿Qué columnas hay que cifrar, enmascarar en los logs o excluir del volcado de la base de datos de pruebas?»*
  (art. 32 RGPD)

Si la respuesta vive en una hoja de cálculo mantenida a mano, queda desfasada el día después de que alguien añada
una columna. Linneo pone la respuesta **junto al campo**, en el código fuente, y la comprueba en la compilación:

```java
@Category(USER_HEALTH_AND_MEDICAL_DISABILITY)
private Integer disabilityDegree;
```

La anotación dice *qué* contiene el campo. Todo lo demás —si es un dato personal, cuán sensible es, cómo debe
protegerse— se deriva de la categoría, como explica el resto del módulo.

## Naturaleza jurídica: qué es el dato según la normativa de protección de datos

Cada categoría de datos de Linneo tiene una `LegalNature`, un enum con cuatro valores:

| `LegalNature` | Significado | Ejemplos en la universidad |
|---|---|---|
| `NONE` | No es un dato personal: datos del sistema, de la organización o de referencia | configuración de menús, lista de países, nombres de edificios |
| `ORDINARY` | Dato personal (art. 4.1 RGPD): toda información sobre una persona física identificada o identificable | nombre y correo del estudiante, matrícula, calificación, IBAN de un empleado |
| `SPECIAL_CATEGORY` | Categorías especiales de datos personales (art. 9 RGPD): origen étnico o racial, opiniones políticas, convicciones religiosas o filosóficas, afiliación sindical, genéticos, biométricos, salud, vida sexual y orientación sexual | grado de discapacidad, un certificado médico, una plantilla de huella dactilar |
| `CRIMINAL_OFFENCE` | Datos personales relativos a condenas e infracciones penales (art. 10 RGPD) | el certificado negativo de delitos sexuales exigido al personal que trabaja con menores |

`LegalNature.isPersonal()` devuelve `false` solo para `NONE`. Las categorías especiales y los datos penales son
datos personales con protección *reforzada*: su tratamiento necesita una base jurídica adicional y una brecha se
considera más grave.

### Las naturalezas solo se endurecen al bajar por el árbol

Las categorías forman un árbol (módulo 2). Una categoría hija no puede ser *menos* personal que su madre. La regla
está codificada en `LegalNature.admits(child)`:

| Naturaleza de la madre | La hija puede ser |
|---|---|
| `NONE` | solo `NONE` |
| `ORDINARY` | `ORDINARY`, `SPECIAL_CATEGORY` o `CRIMINAL_OFFENCE` (cualquiera salvo `NONE`) |
| `SPECIAL_CATEGORY` | solo `SPECIAL_CATEGORY` |
| `CRIMINAL_OFFENCE` | solo `CRIMINAL_OFFENCE` |

```java
LegalNature.ORDINARY.admits(LegalNature.SPECIAL_CATEGORY);   // true: user -> user.health_and_medical
LegalNature.SPECIAL_CATEGORY.admits(LegalNature.ORDINARY);   // false: un dato de salud nunca pasa a ser ordinario
LegalNature.NONE.admits(LegalNature.ORDINARY);                // false: un subárbol system nunca contiene datos personales
```

Por eso bajo `user` (datos personales ordinarios) están `user.health_and_medical` (categoría especial) y
`user.criminal_history` (datos penales), y por eso nada por debajo de ellas puede «rebajarse».

## Confidencialidad: el daño de la revelación

`Confidentiality` mide cuánto daño causaría revelar el dato. Tiene cuatro niveles, declarados de menos a más
estricto, de modo que `compareTo` los ordena por exigencia:

| Nivel | Significado |
|---|---|
| `PUBLIC` | Destinado a ser público; revelarlo no causa daño |
| `LOW` | La revelación causaría un daño limitado |
| `MEDIUM` | La revelación causaría un daño grave |
| `HIGH` | La revelación causaría un daño muy grave |

`LOW`, `MEDIUM` y `HIGH` siguen las escalas de tres niveles habituales en los marcos de seguridad de la
información, como el ENS o FIPS 199. `PUBLIC` es para datos que están pensados para publicarse: el catálogo público
de asignaturas, la lista de países.

`Confidentiality.max(other)` devuelve el más estricto de dos niveles, útil cuando combinas varios campos:

```java
Confidentiality.LOW.max(Confidentiality.HIGH);   // HIGH
```

## Integridad: el daño de un cambio no deseado

`Integrity` mide cuánto daño causaría una modificación no autorizada o no detectada: `LOW`, `MEDIUM`, `HIGH`, de
nuevo en orden creciente y con un método `max`.

La integridad es **independiente** de la confidencialidad. Dos ejemplos de la universidad:

- Una **calificación** es moderadamente confidencial, pero su integridad es crítica: una nota alterada sin que nadie
  lo note cambia el expediente académico de alguien. En la política estándar de Linneo, `user.education.grade`
  tiene confidencialidad `MEDIUM` e integridad `HIGH`.
- Una **matriz de permisos** (`system.security`) puede ser inocua si se revela, pero un cambio no deseado da acceso a
  quien no debe: confidencialidad `LOW`, integridad `HIGH`.

Mantener las dos escalas permite que la clasificación guíe ambos tipos de control: restricciones de acceso y
cifrado para la confidencialidad; pistas de auditoría, firmas y aprobación de cambios para la integridad.

## Identificabilidad: una propiedad del valor almacenado

El mismo tipo de dato puede almacenarse de formas muy distintas. Una calificación guardada junto al identificador
del estudiante identifica al estudiante; la nota media de una asignatura con 120 matriculados no describe a ninguno
de ellos. `Identifiability` recoge esta diferencia y pertenece al **campo**, no a la categoría:

| `Identifiability` | Significado | ¿Dato personal? |
|---|---|---|
| `IDENTIFIED` | El valor identifica a la persona directamente o a través de otros datos que tiene el responsable | sí |
| `PSEUDONYMIZED` | El valor solo puede vincularse a la persona con información adicional conservada por separado (art. 4.5 RGPD) | **sí** (considerando 26 RGPD) |
| `ANONYMIZED` | El valor ya no puede vincularse a la persona | no |
| `AGGREGATED` | El valor resume a varias personas y no describe a ninguna en concreto | no |

Los valores siguen los rasgos de identificabilidad del Microsoft Common Data Model. Atención a un error frecuente:
**los datos seudonimizados siguen siendo datos personales**. Solo los valores anonimizados y agregados quedan fuera
del ámbito del RGPD, y solo mientras nadie pueda reidentificar a la persona.

En el módulo 3 declararás la identificabilidad en un campo:

```java
@Category(value = USER_UNIQUE_ID_PSEUDONYMOUS, identifiability = Identifiability.PSEUDONYMIZED)
private String analyticsId;
```

## Secretos

Algunos datos no solo *describen* a alguien: *dan acceso*. Contraseñas, hashes de contraseñas, claves de API,
claves de firma, tokens. Linneo marca estas categorías como **secretas**. Un secreto nunca debe leerse de vuelta,
exportarse ni publicarse; solo verificarse. En la taxonomía estándar son secretos las credenciales de una persona
(`user.authorization.credentials`, `.password`, `.biometric`), los identificadores de sesión
(`user.device.session_id`, que dan acceso mientras la sesión es válida) y `system.authentication.secret`.
`user.authorization` en sí, el alcance de los permisos de una persona, es confidencial pero no secreto. El módulo 2
muestra cómo se hereda esta marca y el módulo 5 la usa para mantener los secretos fuera de los logs.

## Fallo seguro

La decisión de diseño más importante de Linneo es que **falla de forma segura** (*fails closed*):

- un campo sin clasificación es un **error**, no un «seguramente no pasa nada»;
- una clave que no existe en la taxonomía es un error, no se ignora;
- un campo que declara una protección *inferior* a la de su categoría es un error;
- un valor por defecto para toda una clase solo puede indicar datos *no personales*; los datos personales siempre
  se declaran campo a campo.

El enfoque contrario —clasificar lo que uno se acuerda de clasificar y suponer que el resto es inocuo— es el camino
por el que los datos de salud acaban en un log de depuración. Con Linneo, quien añade una columna sin clasificarla
rompe la compilación, y la corrección es una anotación de una línea que se revisa junto con la columna (módulo 4).

## Errores frecuentes

- **«Seudonimizado significa anónimo».** No. Un identificador seudónimo es un dato personal; solo los valores
  `ANONYMIZED` y `AGGREGATED` no lo son.
- **Tratar confidencialidad e integridad como una única puntuación de «sensibilidad».** Una calificación y una
  matriz de permisos necesitan controles muy diferentes; mantén separadas las dos escalas.
- **Pensar que los datos penales son una categoría especial.** El art. 10 RGPD es un régimen distinto del art. 9; por
  eso `CRIMINAL_OFFENCE` es una naturaleza propia. (Al exportar a OpenMetadata ambas se corresponden con la misma
  etiqueta predefinida; véase el módulo 7.)
- **Clasificar por tabla en lugar de por campo.** Una tabla `Student` contiene a la vez datos ordinarios (nombre),
  de categoría especial (discapacidad) e identificadores seudónimos. La clasificación es por campo.
- **Suponer que «no personal» significa «no sensible».** Una clave de firma no es un dato personal y es un secreto
  con confidencialidad alta.

## Ejercicios

**Ejercicio 1.1 — Naturaleza.** Indica la `LegalNature` de cada dato que conserva la universidad:

1. La dirección de correo electrónico de un estudiante.
2. El texto de una opción de menú del portal del estudiante.
3. Un certificado médico que justifica la ausencia a un examen.
4. El certificado negativo de delitos sexuales exigido al personal que trabaja con menores.
5. El número de estudiantes que aprobaron *Álgebra I* el curso pasado.
6. La religión de un estudiante, registrada para organizar los servicios de capellanía.
7. El código ISO de un país.

**Ejercicio 1.2 — Dos escalas.** Para cada dato, indica si la preocupación principal es la *confidencialidad* o la
*integridad*, y por qué: (a) la nota final de un estudiante; (b) la cadena de hashes que protege el log de
auditoría; (c) el domicilio de un empleado; (d) el rol que da acceso al módulo de calificaciones.

**Ejercicio 1.3 — Identificabilidad.** Elige la `Identifiability` de cada valor almacenado e indica si es un dato
personal: (a) el identificador del estudiante en la tabla de matrículas; (b) un identificador aleatorio de
analítica cuya correspondencia con los estudiantes se guarda en otra tabla de acceso restringido; (c) la nota media
de una asignatura; (d) la respuesta a una encuesta cuyo vínculo con quien respondió se destruyó de forma
irreversible.

**Ejercicio 1.4 — Código.** Sin ejecutarlo, predice la salida; después ejecútalo para comprobarlo:

```java
System.out.println(LegalNature.CRIMINAL_OFFENCE.isPersonal());
System.out.println(LegalNature.ORDINARY.admits(LegalNature.CRIMINAL_OFFENCE));
System.out.println(LegalNature.CRIMINAL_OFFENCE.admits(LegalNature.SPECIAL_CATEGORY));
System.out.println(Confidentiality.PUBLIC.compareTo(Confidentiality.LOW) < 0);
System.out.println(Integrity.MEDIUM.max(Integrity.LOW));
```

## Cuestionario de autoevaluación

1. ¿Qué valores de `LegalNature` son datos personales?
2. ¿Puede ser `ORDINARY` una hija de una categoría `SPECIAL_CATEGORY`?
3. ¿Es un dato personal un valor seudonimizado?
4. ¿La identificabilidad pertenece a la categoría o al campo?
5. ¿Qué significa «fallo seguro» para un campo que nadie ha clasificado?

## Para saber más

- [Referencia — Niveles de protección](../reference/protection-levels.md): `LegalNature`, `Confidentiality`,
  `Integrity` e `Identifiability` en detalle.

---

← [Presentación del curso](README.md) · Siguiente: [Módulo 2 — La taxonomía](02-the-taxonomy.md) →

<details markdown="block">
<summary><strong>Soluciones</strong></summary>

**Ejercicio 1.1**

1. `ORDINARY`. 2. `NONE`. 3. `SPECIAL_CATEGORY` (salud). 4. `CRIMINAL_OFFENCE` (art. 10 RGPD). 5. La categoría trata
de calificaciones (personales), pero el valor almacenado está agregado, así que no es un dato personal; en el
módulo 3 lo clasificarás como calificación con `identifiability = AGGREGATED`. 6. `SPECIAL_CATEGORY` (convicciones
religiosas). 7. `NONE`.

**Ejercicio 1.2**

(a) Importan las dos, pero la integridad es crítica: una nota alterada cambia el expediente (la política estándar
da a las calificaciones confidencialidad `MEDIUM` e integridad `HIGH`). (b) Integridad: la cadena de hashes es la
prueba de que el log no se ha alterado; revelarla no causa daño. (c) Confidencialidad: revelarlo expone a la
persona. (d) Integridad: cambiar quién tiene el rol da acceso a quien no debe.

**Ejercicio 1.3**

(a) `IDENTIFIED`, personal. (b) `PSEUDONYMIZED`, personal (la correspondencia existe, aunque esté separada).
(c) `AGGREGATED`, no personal. (d) `ANONYMIZED`, no personal, mientras nadie pueda reidentificar a quien respondió.

**Ejercicio 1.4**

```
true
true
false
true
MEDIUM
```

**Cuestionario**

1. `ORDINARY`, `SPECIAL_CATEGORY` y `CRIMINAL_OFFENCE` (todos salvo `NONE`).
2. No. `SPECIAL_CATEGORY.admits(...)` solo acepta `SPECIAL_CATEGORY`.
3. Sí (art. 4.5 y considerando 26 RGPD).
4. Al campo: la misma categoría puede guardarse identificada en una tabla y agregada en otra.
5. Que es un error que hace fallar la verificación, no un «no personal» implícito.

</details>
