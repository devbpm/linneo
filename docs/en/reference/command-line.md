---
title: "Command line"
parent: "Reference"
nav_order: 20
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/internal/Cli.java
  - pom.xml
-->

# Command line

The Linneo JAR runs as a program: `java -jar linneo.jar <command>`. It works on the text forms Linneo writes (a
[taxonomy](taxonomy.md#the-taxonomy-as-data), a [data inventory](classify/inventory.md#text-format)) and on the catalog
exports, so that a build pipeline or a data protection officer can use them without writing Java. It needs a Java 21
runtime and nothing else.

The command line is **not part of the public API**: it is a tool, with no Java entry point to call, and this page
documents the commands, not the code behind them.

```text
java -jar linneo.jar <command> [options]
```

| Command | What it does |
|---|---|
| [`taxonomy`](#taxonomy) | Writes the standard taxonomy, with the shipped extensions you name, as text. |
| [`taxonomy-diff`](#taxonomy-diff) | Lists the changes between two taxonomies and the version change they need. |
| [`inventory-diff`](#inventory-diff) | Lists the field changes between two data inventories. |
| [`catalog`](#catalog) | Writes the requests that create the taxonomy in OpenMetadata, DataHub or Apache Atlas, as JSON. |
| `help` | Shows the usage. `--help` and `-h` do the same. |

## Exit codes and input

| Code | Meaning |
|---|---|
| `0` | The command succeeded. Differences are listed and still exit `0` unless you asked for a check. |
| `1` | A check you asked for failed: `--require-compatible` found an incompatible change, or `--fail-on-weakening` found a change that weakens protection. |
| `2` | A usage or input error: no command, an unknown command, extension or option, the wrong number of arguments, a missing `--taxonomy`, a file that cannot be read or text that is not a taxonomy or an inventory. The message is on the standard error, as `Error: ...` for an input error, and after a usage error the usage follows it. |

Results go to the standard output as UTF-8. The files you pass are read as UTF-8, or in the encoding that their byte
order mark names: UTF-8 with a mark, as many Windows tools write it, or UTF-16 (either order), which is what the
redirection of Windows PowerShell 5 writes. A file without a mark is UTF-8.

## taxonomy

```text
java -jar linneo.jar taxonomy [--with EXTENSION,...]
```

Writes the standard taxonomy in its text form, the one `Taxonomy.fromTsv` reads. With `--with`, the shipped
[extensions](extensions/README.md) you name are applied on top of it, separated by commas: `university`, `finance`,
`supply_chain`, `project_operations` and `human_resources`. An unknown name is a usage error (exit `2`) that lists the
known ones.

```bash
java -jar linneo.jar taxonomy > taxonomy.tsv
java -jar linneo.jar taxonomy --with university,finance > taxonomy-with-extensions.tsv
```

The text is what the other commands take as a taxonomy: keep the one you applied, and compare it with a later one.

## taxonomy-diff

```text
java -jar linneo.jar taxonomy-diff BEFORE.tsv AFTER.tsv [--require-compatible]
```

Compares two taxonomies term by term, as [`TaxonomyDiff`](taxonomy-diff.md) does, and writes one line per changed term
and a summary line with the number of changes and the version change they need under Linneo's rules (`NONE`, `PATCH`,
`MINOR` or `MAJOR`). With `--require-compatible` it exits with `1` when a change is incompatible (a removed term, weaker
protection or a replacement that moved) and adds a line that says so. Nothing is a failure without the option.

```bash
java -jar linneo.jar taxonomy > current.tsv
java -jar linneo.jar taxonomy-diff applied.tsv current.tsv --require-compatible
```

```text
DATA_CATEGORY 'user.contact.address.state': [REMOVED]
1 change(s); required version change: MAJOR
Incompatible: 1 change(s) remove a term, weaken protection or move a replacement
```

(exit code `1`.) Adding the terms of the `university` extension to the standard taxonomy writes one `[ADDED]` line for
each and ends with `71 change(s); required version change: MINOR`, exit code `0`. The same two files give
`0 change(s); required version change: NONE`.

## inventory-diff

```text
java -jar linneo.jar inventory-diff BEFORE.tsv AFTER.tsv --taxonomy TAXONOMY.tsv [--fail-on-weakening]
```

Compares two [data inventories](classify/inventory.md), as [`InventoryDiff`](classify/inventory.md#inventorydiff)
does, under the taxonomy in `TAXONOMY.tsv`, which says how the protection of each field moved: `--taxonomy` is
required. It writes one line per changed field, marks the ones that weaken protection and ends with the number of
changes and of weakenings. With `--fail-on-weakening` it exits with `1` if any change weakens protection, including a
classified field that became exempt.

```bash
java -jar linneo.jar inventory-diff applied-inventory.tsv inventory.tsv --taxonomy taxonomy.tsv --fail-on-weakening
```

```text
CHANGED students.email LOOSENED: students.email exempt: always empty  <- weakens protection
ADDED students.phone: students.phone = user.contact.phone_number (DECLARED)
2 field change(s), 1 weakening
```

(exit code `1`.)

## catalog

```text
java -jar linneo.jar catalog openmetadata|datahub|atlas [--taxonomy TAXONOMY.tsv]
```

Writes the requests that create the taxonomy in a data catalog, as JSON on the standard output, from the standard
taxonomy or from the one in `TAXONOMY.tsv`. It makes no network calls: send the JSON with the HTTP client you use.

| Argument | Writes | See |
|---|---|---|
| `openmetadata` | An object `{"classifications": [...], "tags": [...]}`, with the bodies for `PUT /api/v1/classifications` and `PUT /api/v1/tags`, parents first. | [OpenMetadata catalog](openmetadata/catalog.md) |
| `datahub` | An array with the `ingestProposal` bodies of the glossary nodes and terms. | [DataHub catalog](datahub/catalog.md) |
| `atlas` | The body of the one `POST /api/atlas/v2/types/typedefs`, `{"classificationDefs": [...]}`. | [Apache Atlas catalog](atlas/catalog.md) |

```bash
java -jar linneo.jar catalog atlas --taxonomy taxonomy.tsv > atlas-typedefs.json
```

## In a build

The exit codes let a pipeline fail on what matters. Keep the taxonomy and the inventory that were applied last in the
repository and compare them with the current ones in a step of the build:

```bash
java -jar linneo.jar taxonomy-diff released-taxonomy.tsv taxonomy.tsv --require-compatible
java -jar linneo.jar inventory-diff released-inventory.tsv inventory.tsv --taxonomy taxonomy.tsv --fail-on-weakening
```

The first fails when the taxonomy lost a term or weakened a protection, the second when a field of the inventory is
less protected than it was. The inventory file is what [`DataInventory.toTsv()`](classify/inventory.md#text-format)
writes; the library writes it from the code and the command only reads it.

See also: [Taxonomy diff](taxonomy-diff.md) · [Data inventory](classify/inventory.md) ·
[Reference](README.md)
