---
title: "Course"
nav_order: 3
permalink: "/en/course/"
---
<!--
doc-sources:
  - README.md
  - pom.xml
-->

# Linneo training course

**Classify the data model of your organisation, verify it in your tests and put the result to work.**

This course teaches you to use [Linneo](https://github.com/devbpm/linneo#readme), a small Java library that brings a data classification
taxonomy into your code. You will start from the legal and security concepts behind data classification, learn the
taxonomy Linneo ships with, annotate a realistic data model, make the build fail when something is left
unclassified, use the classification to build a data inventory, decide what can be logged and publish everything
to a data catalogue — OpenMetadata, DataHub or Apache Atlas — and finally declare the processing activities that use
the data and produce the GDPR record of processing activities.

Every module uses the same running example: the data model of a **small university**. It starts with a `Student`
and a `MenuGroup` and grows, module by module, into `Enrollment`, `Grade`, `Staff`, `Document` with its
`Attachment`s, a university-specific extension of the taxonomy, and the processing activities that use them:
enrollment, admissions, staff screening and the alumni newsletter.

> Looking for the exact contract of a type or annotation? That lives in the
> [reference](../reference/README.md). The course explains *why* and *how*; the reference states *what*.

## Who this course is for

- **Java developers** who maintain an application with a persistent data model (JPA entities, records, plain
  classes) holding data about people — students, staff, applicants — and must declare what each field holds.
- **Data protection officers, security officers and analysts** who need to understand which GDPR concepts the
  classification expresses and how to read its output. Module 1 and the "data protection" notes in later modules
  are written for you; you can skip the code-heavy parts.

## Goals

By the end of the course you will be able to:

1. Explain the four GDPR natures Linneo uses (ordinary personal data, special category, criminal offence data, not
   personal data) and the confidentiality, integrity and identifiability facets.
2. Navigate the taxonomy, pick the right key for a field and justify the choice.
3. Annotate a data model with `@Category`, `@DefaultCategory`, `@CategoryByRow` and `@Exempt`.
4. Write a test that fails the build when the model is not fully and correctly classified, and fix every kind of
   violation it reports.
5. Use the classification at run time: list personal fields, produce a data inventory, drive masking decisions.
6. Extend the taxonomy with your own terms and stricter protection through a `TaxonomyExtension`.
7. Publish the taxonomy and the tags of your columns to OpenMetadata.
8. Publish the same taxonomy as a DataHub business glossary or as Apache Atlas classifications, and compute the
   terms and classifications of your columns.
9. Declare processing activities with their lawful bases, check them in a test and produce the record of processing
   activities as Markdown and JSON.

## Prerequisites

- **Java 21 or later.** Linneo is a named JPMS module (`io.github.devbpm.linneo`) and has no runtime dependencies.
- **Maven or Gradle**, with the dependency:

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

- **JUnit 5** (or any test framework) for modules 4 and 9.
- Working knowledge of Java annotations, records and class inheritance. JPA knowledge helps but is not required:
  the classifier works on plain classes.
- For modules 7 and 8 only: access to an OpenMetadata, DataHub or Apache Atlas server is useful, but every exercise
  can be done offline.

## Modules

| # | Module | You will learn to… | Time |
|---|---|---|---|
| 1 | [Why classify data](01-why-classify.md) | Explain GDPR natures, confidentiality, integrity, identifiability and secrets; understand the fail-closed approach | 45 min |
| 2 | [The taxonomy](02-the-taxonomy.md) | Read dotted keys, understand inherited protection, explore the taxonomy with the `Taxonomy` API | 60 min |
| 3 | [Annotating models](03-annotating-models.md) | Annotate fields and classes, raise levels, declare identifiability, handle records, inheritance and per-row data | 90 min |
| 4 | [Verifying in tests](04-verifying-in-tests.md) | Fail the build on unclassified or wrongly classified fields; read and fix every violation | 60 min |
| 5 | [Using the classification](05-using-the-classification.md) | List personal fields, build a data inventory and a RoPA-style report, decide what may be logged | 60 min |
| 6 | [Extending the taxonomy](06-extending-the-taxonomy.md) | Add terms and stricter protection with a `TaxonomyExtension` registered through `ServiceLoader`; apply the university extension shipped with Linneo | 60 min |
| 7 | [OpenMetadata](07-openmetadata.md) | Publish classifications and tags, compute the tags of each column, customise the names | 45 min |
| 8 | [DataHub and Apache Atlas](08-datahub-and-atlas.md) | Publish a DataHub glossary and Atlas classification types, compute the terms and classifications of each column, compare the three catalogues | 60 min |
| 9 | [Processing activities and the record](09-processing-activities.md) | Declare processing activities with their lawful bases, tell errors from warnings, verify them in a test and produce the record of processing activities | 60 min |

Total: about 9 hours. Modules 1 and 2 are conceptual; modules 3 to 9 are hands-on. Data protection readers can
follow modules 1, 2, 5 and 9 on their own.

## How to do the exercises

1. Create an empty Maven or Gradle project on Java 21+, add the Linneo dependency and JUnit 5.
2. Create a package for the running example, for instance `edu.example.university`. Each module tells you which
   classes to add or change; the code in the modules compiles against Linneo 0.1.0.
3. Do the exercises before opening the solutions. Every module ends with a collapsible **Solutions** block.
4. Run things. Most exercises ask you to predict an output (a violation, a tag list, a protection level) and then
   check it by running the code: the library's messages are part of what you are learning to read.
5. Each module also has a short **self-check quiz**. If you miss more than one answer, re-read the section the
   question refers to before moving on.

Conventions used in the course:

- Code samples use `import static io.github.devbpm.linneo.Categories.*;` so that keys read as `USER_CONTACT_EMAIL`
  rather than `Categories.USER_CONTACT_EMAIL`.
- JPA annotations (`@Entity`, `@Id`, `@Column`...) are left out of most samples to keep the focus on Linneo; your
  real entities keep them. Linneo never looks at them.
- Output shown in the course is the real output of Linneo 0.1.0 for the code shown.

---

Start with [Module 1 — Why classify data](01-why-classify.md) →
