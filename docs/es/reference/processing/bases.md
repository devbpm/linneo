---
title: "Bases jurídicas"
parent: "Tratamientos"
grand_parent: "Referencia"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/LegalBasis.java
  - src/main/java/io/github/devbpm/linneo/processing/SpecialCategoryCondition.java
  - src/main/java/io/github/devbpm/linneo/processing/CriminalDataBasis.java
  - src/main/java/io/github/devbpm/linneo/processing/TransferSafeguard.java
-->

# Bases jurídicas

Cuatro enumerados nombran lo que hace lícita una actividad de tratamiento o una transferencia. Cada constante lleva la
disposición del RGPD en `article()`. Tres de ellos llevan además, en `fideslangName()`, el valor que usan las
declaraciones de privacidad de fideslang para el mismo concepto, de modo que una actividad pueda exportarse a una
herramienta que hable fideslang. `requiresLegalReference()` indica si la base descansa en el Derecho de la Unión o de
un Estado miembro, que la actividad debería entonces nombrar en su `legalReference`.

| Enumerado | RGPD | Campo de fideslang | `fideslangName()` | `requiresLegalReference()` |
|---|---|---|---|---|
| [`LegalBasis`](#legalbasis) | art. 6.1 | `legal_basis_for_processing` | sí | sí |
| [`SpecialCategoryCondition`](#specialcategorycondition) | art. 9.2 | `special_category_legal_basis` | sí | sí |
| [`CriminalDataBasis`](#criminaldatabasis) | art. 10 | — | no | sí |
| [`TransferSafeguard`](#transfersafeguard) | capítulo V | `legal_basis_for_transfers` | sí | no |

Los cuatro son `@since 0.1.0`. Las descripciones que siguen resumen las disposiciones para ayudar a elegir una
constante; no son asesoramiento jurídico. Los valores de `fideslangName()` son los de fideslang y están en inglés.

## LegalBasis

`io.github.devbpm.linneo.processing.LegalBasis` — `public enum`

La base jurídica de una actividad de tratamiento (RGPD, artículo 6.1). Toda actividad que trate datos personales
necesita una; sin ella el comprobador notifica el error `MISSING_LEGAL_BASIS`.

| Constante | `article()` | `fideslangName()` | `requiresLegalReference()` | Significado |
|---|---|---|---|---|
| `CONSENT` | `6.1.a` | `Consent` | `false` | El interesado ha dado su consentimiento para uno o varios fines específicos. |
| `CONTRACT` | `6.1.b` | `Contract` | `false` | Necesario para la ejecución de un contrato con el interesado, o para aplicar a petición suya medidas precontractuales. |
| `LEGAL_OBLIGATION` | `6.1.c` | `Legal obligations` | `true` | Necesario para cumplir una obligación legal del responsable, establecida por ley. |
| `VITAL_INTERESTS` | `6.1.d` | `Vital interests` | `false` | Necesario para proteger intereses vitales del interesado o de otra persona física. |
| `PUBLIC_INTEREST` | `6.1.e` | `Public interest` | `true` | Necesario para una misión realizada en interés público o en el ejercicio de poderes públicos, establecida por ley. |
| `LEGITIMATE_INTERESTS` | `6.1.f` | `Legitimate interests` | `false` | Necesario para los intereses legítimos del responsable o de un tercero, ponderados con los derechos del interesado. Indica el interés con `legitimateInterest`. |

`requiresLegalReference()` es `true` para las dos bases que el artículo 6.3 exige que se establezcan por ley.

## SpecialCategoryCondition

`io.github.devbpm.linneo.processing.SpecialCategoryCondition` — `public enum`

La excepción a la prohibición de tratar categorías especiales de datos personales (RGPD, artículo 9.2). Una actividad
que trata una categoría de naturaleza `SPECIAL_CATEGORY` necesita una, además de su base jurídica; sin ella el
comprobador notifica el error `MISSING_SPECIAL_CATEGORY_CONDITION`. Declararla en una actividad sin datos de categorías
especiales es el aviso `UNNEEDED_CONDITION`.

| Constante | `article()` | `fideslangName()` | `requiresLegalReference()` |
|---|---|---|---|
| `EXPLICIT_CONSENT` | `9.2.a` | `Explicit consent` | `false` |
| `EMPLOYMENT_AND_SOCIAL_SECURITY` | `9.2.b` | `Employment, social security and social protection` | `true` |
| `VITAL_INTERESTS` | `9.2.c` | `Vital interests` | `false` |
| `NOT_FOR_PROFIT_BODIES` | `9.2.d` | `Not-for-profit bodies` | `false` |
| `MADE_PUBLIC_BY_THE_DATA_SUBJECT` | `9.2.e` | `Made public by the data subject` | `false` |
| `LEGAL_CLAIMS` | `9.2.f` | `Legal claims or judicial acts` | `false` |
| `SUBSTANTIAL_PUBLIC_INTEREST` | `9.2.g` | `Reasons of substantial public interest (with a basis in law)` | `true` |
| `HEALTH_OR_SOCIAL_CARE` | `9.2.h` | `Health or social care (with a basis in law)` | `true` |
| `PUBLIC_HEALTH` | `9.2.i` | `Public health (with a basis in law)` | `true` |
| `ARCHIVING_RESEARCH_AND_STATISTICS` | `9.2.j` | `Archiving, research and statistics (with a basis in law)` | `true` |

## CriminalDataBasis

`io.github.devbpm.linneo.processing.CriminalDataBasis` — `public enum`

Lo que permite a una actividad tratar datos personales relativos a condenas e infracciones penales (RGPD, artículo
10). Una actividad que trata una categoría de naturaleza `CRIMINAL_OFFENCE` necesita una; sin ella el comprobador
notifica el error `MISSING_CRIMINAL_DATA_BASIS`. Declararla en una actividad sin datos penales es el aviso
`UNNEEDED_CONDITION`. fideslang no tiene un campo para esto, así que este enumerado no tiene `fideslangName()`.

| Constante | `article()` | `requiresLegalReference()` | Significado |
|---|---|---|---|
| `OFFICIAL_AUTHORITY` | `10` | `false` | El tratamiento se realiza bajo la supervisión de las autoridades públicas. |
| `AUTHORISED_BY_LAW` | `10` | `true` | El tratamiento está autorizado por el Derecho de la Unión o de un Estado miembro que establece garantías adecuadas. |

## TransferSafeguard

`io.github.devbpm.linneo.processing.TransferSafeguard` — `public enum`

Lo que permite una transferencia de datos personales a un tercer país o a una organización internacional (RGPD,
capítulo V). Es la `safeguard` de una [`Transfer`](activity.md#transfer). No tiene `requiresLegalReference()`.

| Constante | `article()` | `fideslangName()` | Significado |
|---|---|---|---|
| `ADEQUACY_DECISION` | `45` | `Adequacy Decision` | La Comisión ha decidido que el destino garantiza un nivel de protección adecuado. |
| `STANDARD_CONTRACTUAL_CLAUSES` | `46.2.c` | `SCCs` | Cláusulas tipo de protección de datos adoptadas por la Comisión. |
| `BINDING_CORPORATE_RULES` | `47` | `BCRs` | Normas corporativas vinculantes aprobadas por una autoridad de control. |
| `DEROGATION` | `49` | `Other` | Una excepción para situaciones específicas, como el consentimiento explícito o la ejecución de un contrato. |

## La referencia legal

Cuando las bases de una actividad descansan en una ley, el comprobador reúne los artículos de cada base de ese tipo
que se aplica —la base jurídica, la condición del artículo 9.2 cuando la actividad trata categorías especiales y la
base del artículo 10 cuando trata datos penales— y, si `legalReference` es `null`, notifica un único aviso
`MISSING_LEGAL_REFERENCE` que los nombra todos:

```text
WARNING MISSING_LEGAL_REFERENCE screening: GDPR article(s) 6.1.c, 9.2.b, 10 rest on Union or Member State law; name it as the legal reference
```

`legalReference` es un solo texto para toda la actividad, así que nombra en él todas las leyes implicadas, por
ejemplo `Higher Education Act, arts. 12 and 31` (una ley ficticia).

Véase también: [ProcessingActivity](activity.md) · [ProcessingChecker](checker.md) ·
[Actividades de tratamiento](README.md)
