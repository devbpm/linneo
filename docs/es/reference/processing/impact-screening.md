---
title: "ImpactScreening"
parent: "Tratamientos"
grand_parent: "Referencia"
nav_order: 6
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/ImpactScreening.java
  - src/main/java/io/github/devbpm/linneo/processing/Governance.java
  - src/main/java/io/github/devbpm/linneo/processing/CheckedActivity.java
  - src/main/java/io/github/devbpm/linneo/processing/ProcessingChecker.java
  - src/main/java/io/github/devbpm/linneo/Trait.java
-->

# ImpactScreening

`io.github.devbpm.linneo.processing.ImpactScreening` — `public record`, `@since 0.1.0`

```java
public record ImpactScreening(Outcome outcome, List<Finding> findings, String mandatoryCase)
```

Si una [actividad de tratamiento](activity.md) necesita una evaluación de impacto relativa a la protección de datos
(RGPD, artículo 35), contrastada con los nueve criterios de las directrices del Comité Europeo de Protección de Datos
(CEPD; WP248 rev.01) y con los casos que el artículo 35.3 hace obligatorios. Es un **cribado** (*screening*), **no la
evaluación**: dice qué criterios se cumplen y por qué, para que el delegado de protección de datos sepa dónde mirar.
Revisa el resultado frente a la lista de la autoridad de control (artículo 35.4).

| Componente | Descripción |
|---|---|
| `outcome` | Si hace falta una evaluación; véase [Outcome](#outcome). |
| `findings` | Los criterios que se cumplen, en el orden de las directrices, cada uno con lo que lo muestra. |
| `mandatoryCase` | La disposición del artículo 35.3 que hace obligatoria la evaluación, como `35.3.b`, o `null`. |

El constructor canónico lanza `NullPointerException` si `outcome` o `findings` es `null`, y copia la lista (inmutable).
Toda [`CheckedActivity`](checker.md#checkedactivity) tiene uno, con `screening()`; el [`ProcessingChecker`](checker.md)
lo usa para notificar la necesidad de una evaluación que la actividad no cita, y el [registro](record.md) lo muestra.

## Qué puede ver el código y qué solo sabe el delegado

Algunos criterios se ven en lo que declara la actividad. Los demás son hechos que solo conoce la función de protección
de datos, como la escala del tratamiento, y el delegado los declara como **factores de riesgo**.

Los criterios que el código puede ver se leen de los [rasgos](../terms.md#trait) de los términos de la actividad, no de
listas de claves, así que un término de una extensión o propio cuenta en cuanto declara sus rasgos.

| Criterio | Se encuentra a partir de la actividad | Se declara con |
|---|---|---|
| 1. `EVALUATION`: evaluación o puntuación, incluida la elaboración de perfiles | una finalidad con el rasgo `EVALUATION` | `Factor.EVALUATION` |
| 2. `AUTOMATED_DECISIONS`: decisiones automatizadas con efectos jurídicos o similares | no se ve | `Factor.AUTOMATED_DECISIONS` |
| 3. `SYSTEMATIC_MONITORING`: observación sistemática | una finalidad o una categoría de datos con el rasgo `MONITORING` | `Factor.SYSTEMATIC_MONITORING`, `Factor.PUBLICLY_ACCESSIBLE_AREA` |
| 4. `SENSITIVE_DATA`: datos sensibles o de naturaleza muy personal | una categoría de categorías especiales o de datos penales, o una categoría con el rasgo `FINANCIAL`, `BEHAVIOURAL` o `PRIVATE_LIFE` | — |
| 5. `LARGE_SCALE`: datos tratados a gran escala | no se ve | `Factor.LARGE_SCALE` |
| 6. `DATASET_MATCHING`: cotejo o combinación de conjuntos de datos | no se ve | `Factor.DATASET_MATCHING` |
| 7. `VULNERABLE_SUBJECTS`: datos de interesados vulnerables | un interesado o una categoría de datos con el rasgo `VULNERABLE` | `Factor.VULNERABLE_SUBJECTS` |
| 8. `INNOVATIVE_TECHNOLOGY`: uso innovador o nuevas soluciones tecnológicas u organizativas | no se ve | `Factor.INNOVATIVE_TECHNOLOGY` |
| 9. `PREVENTS_RIGHTS`: tratamiento que impide a las personas ejercer un derecho o usar un servicio o un contrato | no se ve | `Factor.PREVENTS_RIGHTS` |

Las finalidades, los interesados y las categorías de datos son los que resolvió el comprobador: las claves obsoletas
están sustituidas y solo cuentan los datos personales. Un criterio se cumple una sola vez, por muchas cosas que lo
muestren; los hallazgos las listan todas en su evidencia, **ordenadas**, de modo que el cribado no depende del orden en
que la actividad enumera sus términos.

La taxonomía estándar declara los rasgos en [`linneo-traits.tsv`](../standard-taxonomy.md#rasgos-de-los-términos-estándar):
las finalidades con `EVALUATION` son `marketing.advertising.profiling`, `marketing.advertising.first_party.targeted`,
`marketing.advertising.third_party.targeted`, `personalize.content.profiling`, `personalize.content.profiled` y la
obsoleta `personalize.profiling` (cribada como `personalize.content.profiling`); `employee`, `staff_member`,
`faculty_member`, `trainee`, `patient` y `job_applicant` son `VULNERABLE`, igual que la categoría `user.childrens`;
`user.financial` es `FINANCIAL`, `user.behavior` y `user.location` son `BEHAVIOURAL`, y `user.content.private` es
`PRIVATE_LIFE`. La taxonomía estándar no tiene ningún término `MONITORING`: vienen de las
[extensiones](../extensions/README.md#rasgos), como `employment.monitoring` y `user.employment.monitoring` de la
[extensión de recursos humanos](../extensions/human-resources.md) o `education.financial_aid.means_assessment`, una
finalidad `EVALUATION` de la [extensión de universidad](../extensions/university.md); con la taxonomía estándar sola,
esas filas no encuentran nada. Una extensión o una aplicación que declara los rasgos de sus propios términos se lee
igual que las estándar.

`Criterion.number()` es el número del criterio en las directrices, de 1 a 9. Un `Factor` tiene un `criterion()`: el que
cumple.

### Declarar factores de riesgo

Los factores de riesgo pertenecen a la [gobernanza](activity.md#governance) de la actividad, que mantiene el delegado:
son su componente `riskFactors`, un `Set<ImpactScreening.Factor>`, que se fija con el método `riskFactors(...)` del
builder de `Governance` y del de `ProcessingActivity`. El método **añade** los factores que recibe a los declarados
antes.

```java
Governance declared = Governance.builder()
        .legalBasis(LegalBasis.PUBLIC_INTEREST)
        .riskFactors(ImpactScreening.Factor.LARGE_SCALE, ImpactScreening.Factor.DATASET_MATCHING)
        .build();
declared.riskFactors();   // [LARGE_SCALE, DATASET_MATCHING]
```

Un cambio de los factores de riesgo entre dos registros es el aspecto de gobernanza `RISK_FACTORS` de un
[`ActivityChange`](record.md#aspect).

## Outcome

`ImpactScreening.Outcome` — `enum` anidado, `@since 0.1.0`: si hace falta una evaluación, de menos a más.

| Valor | Cuándo | Significado |
|---|---|---|
| `NOT_INDICATED` | no se cumple ningún criterio | Nada en la actividad apunta a una evaluación. |
| `POSSIBLE` | se cumple un criterio | Puede hacer falta una evaluación, según el caso. |
| `LIKELY` | se cumplen dos criterios o más | En la mayoría de los casos se exige una evaluación. |
| `REQUIRED` | se da un caso del artículo 35.3 | Se exige una evaluación. |

### Los casos del artículo 35.3

El resultado es `REQUIRED` y `mandatoryCase()` nombra la disposición cuando se da uno de estos, comprobados en este
orden:

| `mandatoryCase` | Artículo | Linneo lo encuentra cuando |
|---|---|---|
| `35.3.a` | Evaluación sistemática y exhaustiva de aspectos personales, con decisiones automatizadas que producen efectos jurídicos o similares | se cumple el criterio 1 (`EVALUATION`), por una finalidad o por un factor declarado, **y** se declara `Factor.AUTOMATED_DECISIONS` |
| `35.3.b` | Tratamiento a gran escala de categorías especiales de datos o de datos penales | la actividad trata datos de categorías especiales o datos penales **y** se declara `Factor.LARGE_SCALE` |
| `35.3.c` | Observación sistemática a gran escala de una zona de acceso público | se declaran `Factor.PUBLICLY_ACCESSIBLE_AREA` **y** `Factor.LARGE_SCALE` |

Los datos de naturaleza muy personal (financieros, de ubicación, de comportamiento, contenido privado) cumplen el
criterio 4, pero no son un caso del artículo 35.3.b: solo lo son los datos de categorías especiales y los penales.

## Finding

`ImpactScreening.Finding` — `record` anidado, `@since 0.1.0`

```java
public record Finding(Criterion criterion, String evidence)
```

Un criterio que se cumple, con lo que lo muestra. `evidence` está en inglés y une, con `; `, todo lo que cumple el
criterio: `purpose <key>`, `special category data <key>`, `criminal offence data <key>`, `highly personal data <key>`,
`data category <key>`, `data subjects <key>` y `declared <FACTOR>`. El constructor lanza `NullPointerException` si un
componente es `null`.

## Métodos

| Miembro | Devuelve | Descripción |
|---|---|---|
| `static ImpactScreening of(CheckedActivity activity)` | cribado | Criba una actividad comprobada, como hace [`CheckedActivity.screening()`](checker.md#checkedactivity). |
| `Set<Criterion> criteria()` | conjunto inmutable | Los criterios que se cumplen, en el orden de las directrices. |
| `String summary()` | `String` | Una explicación de una línea, en inglés: el resultado, la disposición o el número de criterios, y los hallazgos. |

`summary()` es la cabecera `Not indicated`, `Possible (1 criterion of WP248)`, `Likely required (<n> criteria of WP248)`
o `Required (GDPR art. <case>)`, seguida, cuando hay hallazgos, de `: ` y cada hallazgo como
`<número>. <criterio en minúsculas>: <evidencia>`, separados por `; `.

## En el comprobador y en el registro

Cuando una actividad trata datos personales y **no** cita ninguna evaluación de impacto
(`Governance.impactAssessment()`), el [`ProcessingChecker`](checker.md) notifica lo que encuentra el cribado:

| Resultado | Infracción | Mensaje |
|---|---|---|
| `REQUIRED` | **error** `IMPACT_ASSESSMENT_REQUIRED` | `GDPR article <case> requires an impact assessment; refer to it. <summary>` |
| `LIKELY` | **aviso** `IMPACT_ASSESSMENT_RECOMMENDED` | `GDPR article 35 likely requires an impact assessment. <summary>` |
| `POSSIBLE`, `NOT_INDICATED` | ninguna | |

Con `Jurisdiction.SPAIN` el aviso cita la lista de la autoridad de control española (artículo 35.4) y dice
`GDPR article 35 likely requires an impact assessment (the AEPD's list under article 35.4 requires one in most cases
that meet two criteria or more). <summary>`.

Ambas se notifican en el elemento `IMPACT_ASSESSMENT`, que pertenece a la gobernanza, y el error hace fallar a `verify`
como cualquier otro. Una actividad que cita una evaluación no recibe ninguna infracción, sea cual sea el resultado. El
[registro](record.md) muestra el cribado de cada actividad en cualquier caso, en una fila del Markdown y en un objeto
`screening` del JSON.

## Ejemplo

Una actividad que puntúa a los solicitantes con decisiones automatizadas es un caso del artículo 35.3.a, que se
encuentra por la finalidad y por el factor declarado:

```java
ProcessingActivity scoring = ProcessingActivity.builder("applicant-scoring", "Applicant scoring")
        .purposes(DataUses.PERSONALIZE_CONTENT_PROFILING)
        .dataSubjects(DataSubjects.APPLICANT)
        .dataCategories(USER_NAME, USER_CONTACT_EMAIL)
        .legalBasis(LegalBasis.CONSENT)
        .retention("Until the end of the admission cycle")
        .riskFactors(ImpactScreening.Factor.AUTOMATED_DECISIONS)
        .build();

CheckedActivity checked = checker.check(scoring);
ImpactScreening screening = checked.screening();
screening.outcome();         // REQUIRED
screening.mandatoryCase();   // "35.3.a"
screening.criteria();        // [EVALUATION, AUTOMATED_DECISIONS]
checked.isValid();           // false
```

```text
ERROR IMPACT_ASSESSMENT_REQUIRED applicant-scoring: GDPR article 35.3.a requires an impact assessment; refer to it. Required (GDPR art. 35.3.a): 1. evaluation: purpose personalize.content.profiling; 2. automated decisions: declared AUTOMATED_DECISIONS
```

Con `.impactAssessment("DPIA-2026-04")` no hay infracción, y el cribado es el mismo. Una actividad con un solo criterio
es `POSSIBLE` y no genera nada: los datos de categorías especiales por sí solos, como en una actividad de admisiones,
son un criterio, y las directrices piden una evaluación en la mayoría de los casos solo a partir de dos. Declara
`Factor.LARGE_SCALE` para ella y es un caso del artículo 35.3.b: el resultado es `REQUIRED`.

Véase también: [ProcessingChecker](checker.md) · [ProcessingActivity](activity.md#governance) ·
[ProcessingRecord](record.md) · [BreachAssessment](breach-assessment.md) · [Actividades de tratamiento](README.md)
