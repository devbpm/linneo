---
title: "Legal bases"
parent: "Processing"
grand_parent: "Reference"
nav_order: 2
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/processing/LegalBasis.java
  - src/main/java/io/github/devbpm/linneo/processing/SpecialCategoryCondition.java
  - src/main/java/io/github/devbpm/linneo/processing/CriminalDataBasis.java
  - src/main/java/io/github/devbpm/linneo/processing/TransferSafeguard.java
-->

# Legal bases

Four enums name what makes a processing activity, or a transfer, lawful. Each constant carries the GDPR provision
in `article()`. Three of them also carry, in `fideslangName()`, the value fideslang's privacy declarations use for
the same concept, so that an activity can be exported to a tool that speaks fideslang. `requiresLegalReference()`
says whether the basis rests on Union or Member State law, which the activity should then name in its
`legalReference`.

| Enum | GDPR | fideslang field | `fideslangName()` | `requiresLegalReference()` |
|---|---|---|---|---|
| [`LegalBasis`](#legalbasis) | art. 6.1 | `legal_basis_for_processing` | yes | yes |
| [`SpecialCategoryCondition`](#specialcategorycondition) | art. 9.2 | `special_category_legal_basis` | yes | yes |
| [`CriminalDataBasis`](#criminaldatabasis) | art. 10 | — | no | yes |
| [`TransferSafeguard`](#transfersafeguard) | chapter V | `legal_basis_for_transfers` | yes | no |

All four are `@since 0.1.0`. The descriptions below summarise the provisions to help pick a constant; they are not
legal advice.

## LegalBasis

`io.github.devbpm.linneo.processing.LegalBasis` — `public enum`

The lawful basis of a processing activity (GDPR article 6.1). Every activity that processes personal data needs one;
without it the checker reports the error `MISSING_LEGAL_BASIS`.

| Constant | `article()` | `fideslangName()` | `requiresLegalReference()` | Meaning |
|---|---|---|---|---|
| `CONSENT` | `6.1.a` | `Consent` | `false` | The data subject has given consent for one or more specific purposes. |
| `CONTRACT` | `6.1.b` | `Contract` | `false` | Necessary for a contract with the data subject, or for steps at their request before entering into one. |
| `LEGAL_OBLIGATION` | `6.1.c` | `Legal obligations` | `true` | Necessary to comply with a legal obligation of the controller, laid down by law. |
| `VITAL_INTERESTS` | `6.1.d` | `Vital interests` | `false` | Necessary to protect the vital interests of the data subject or of another natural person. |
| `PUBLIC_INTEREST` | `6.1.e` | `Public interest` | `true` | Necessary for a task in the public interest or in the exercise of official authority, laid down by law. |
| `LEGITIMATE_INTERESTS` | `6.1.f` | `Legitimate interests` | `false` | Necessary for the legitimate interests of the controller or a third party, balanced against the data subject's rights. State the interest with `legitimateInterest`. |

`requiresLegalReference()` is `true` for the two bases that article 6.3 requires to be laid down by law.

## SpecialCategoryCondition

`io.github.devbpm.linneo.processing.SpecialCategoryCondition` — `public enum`

The exception to the prohibition of processing special categories of personal data (GDPR article 9.2). An activity
that processes a category of nature `SPECIAL_CATEGORY` needs one, besides its lawful basis; without it the checker
reports the error `MISSING_SPECIAL_CATEGORY_CONDITION`. Declaring one for an activity without special category data
is the warning `UNNEEDED_CONDITION`.

| Constant | `article()` | `fideslangName()` | `requiresLegalReference()` |
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

What allows an activity to process personal data relating to criminal convictions and offences (GDPR article 10).
An activity that processes a category of nature `CRIMINAL_OFFENCE` needs one; without it the checker reports the
error `MISSING_CRIMINAL_DATA_BASIS`. Declaring one for an activity without criminal offence data is the warning
`UNNEEDED_CONDITION`. fideslang has no field for it, so this enum has no `fideslangName()`.

| Constant | `article()` | `requiresLegalReference()` | Meaning |
|---|---|---|---|
| `OFFICIAL_AUTHORITY` | `10` | `false` | The processing is carried out under the control of official authority. |
| `AUTHORISED_BY_LAW` | `10` | `true` | The processing is authorised by Union or Member State law that provides appropriate safeguards. |

## TransferSafeguard

`io.github.devbpm.linneo.processing.TransferSafeguard` — `public enum`

What allows a transfer of personal data to a third country or an international organisation (GDPR chapter V). It is
the `safeguard` of a [`Transfer`](activity.md#transfer). It has no `requiresLegalReference()`.

| Constant | `article()` | `fideslangName()` | Meaning |
|---|---|---|---|
| `ADEQUACY_DECISION` | `45` | `Adequacy Decision` | The Commission has decided that the destination ensures an adequate level of protection. |
| `STANDARD_CONTRACTUAL_CLAUSES` | `46.2.c` | `SCCs` | Standard data protection clauses adopted by the Commission. |
| `BINDING_CORPORATE_RULES` | `47` | `BCRs` | Binding corporate rules approved by a supervisory authority. |
| `DEROGATION` | `49` | `Other` | A derogation for a specific situation, such as explicit consent or the performance of a contract. |

## The legal reference

When an activity's bases rest on law, the checker collects the articles of every such basis that applies — the
lawful basis, the article 9.2 condition when the activity processes special category data, and the article 10 basis
when it processes criminal offence data — and, if `legalReference` is `null`, reports one `MISSING_LEGAL_REFERENCE`
warning naming them all:

```text
WARNING MISSING_LEGAL_REFERENCE screening: GDPR article(s) 6.1.c, 9.2.b, 10 rest on Union or Member State law; name it as the legal reference
```

`legalReference` is a single text for the whole activity, so name every law involved in it, for example
`Higher Education Act, arts. 12 and 31` (a fictional law).

See also: [ProcessingActivity](activity.md) · [ProcessingChecker](checker.md) · [Processing activities](README.md)
