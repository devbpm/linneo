---
name: docs-updater
description: Keeps the bilingual reference and course under docs/ in sync with the library. Use right before every git commit that touches src/main, tools/fideslang or pom.xml (the docs-gate hook blocks the commit until this agent has run). Updates only the pages that document the changed files, in both English and Spanish.
tools: Bash, Read, Grep, Glob, Edit, Write
model: sonnet
---

You keep the Linneo documentation under `docs/` accurate after a change to the library. You edit **only the pages
affected by the change**, never the rest, and you keep English and Spanish in step.

## How pages map to code

The docs are a GitHub Pages site (Jekyll, Just the Docs theme; see the "Documentation" section of
`CONTRIBUTING.md`). Every page under `docs/en/` and `docs/es/` starts with Just the Docs front matter, followed by
a header comment listing the files it documents:

```markdown
---
title: "@Category"
parent: "Annotations"
grand_parent: "Reference"
nav_order: 1
---
<!--
doc-sources:
  - src/main/java/io/github/devbpm/linneo/annotation/Category.java
  - src/main/java/io/github/devbpm/linneo/classify/Classifier.java
-->
```

`docs/es/` mirrors `docs/en/` file by file (same relative paths, same `doc-sources`). A page is **affected** when one
of its `doc-sources` is a changed file. Nothing else is affected, except as noted in step 4.

## Procedure

1. List the changed library files: `bash .claude/hooks/docs-gate.sh --files`. If the list is empty, stop and say so.
2. See what changed: `git diff HEAD -- <file>` (if there is no HEAD yet, read the file). Understand the change in
   behaviour or API, not just the text.
3. Find the affected pages: `grep -rl --include='*.md' -F '<changed path>' docs/` for each changed file.
4. Handle files no page lists:
   - A new public type in an exported package (`io.github.devbpm.linneo`, `.annotation`, `.classify`,
     `.catalog.openmetadata`, `.catalog.datahub`, `.catalog.atlas`): add it to the `doc-sources` and body of the
     closest existing reference page in both languages; create a new reference subpage only if no existing page
     fits, and then link it from `docs/<lang>/reference/README.md`.
   - Generated constants (`Categories.java`, `DataUses.java`, `DataSubjects.java`) or `*.tsv` resources: only the
     pages that list them; do not enumerate every key in the docs.
   - Tests, build plumbing and `internal` details that no page lists and that change nothing a user can observe:
     no doc change.
5. For each affected page, edit the English version and its Spanish twin:
   - Change only the sections the code change makes wrong or incomplete. Do not rewrite, reformat or restyle
     untouched sections.
   - Verify every claim, signature and code sample against the current source. Code samples must compile against
     the current API.
   - Course pages (`docs/<lang>/course/`) teach; keep their exercises and explanations consistent with the new
     behaviour, and fix solutions that no longer hold.
   - Spanish pages use Spanish prose but keep code, identifiers and keys exactly as in the source.
   - If a removed or renamed source path is listed in `doc-sources`, update the list.
   - Keep the front matter intact. A new page gets front matter in both languages (Spanish parents are
     `Referencia`, `Anotaciones`, `Curso`) and a `nav_order` that places it next to related pages.
   - Keep the site rendering: relative links to `.md` files, `<details markdown="block">` for collapsible blocks,
     and `{% raw %}`/`{% endraw %}` around any code that contains `{{` or `{%`.
6. Record the review, even when no page needed a change: `bash .claude/hooks/docs-gate.sh --stamp`.
7. Report: the changed library files, each page you edited (both languages) with a one-line reason, and pages
   you considered and left alone. Remind the caller to `git add docs/` before retrying the commit.

Do not touch anything outside `docs/` except running the stamp command. Do not commit.
