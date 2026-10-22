# Contributing to Linneo

Thank you for your interest in Linneo.

## Before you start

Open an issue to discuss any change to the taxonomy or the public API before sending a pull request. Changes to
the taxonomy are reviewed with special care, because applications store and publish its keys.

## Building and testing

```bash
./mvnw verify
```

The build requires JDK 21 or later, compiles with every lint warning as an error and runs `javadoc` with full
doclint. Every pull request must keep the build green and add tests for new behaviour.

### Integration tests against catalog servers

`OpenMetadataIT`, `DataHubIT` and `AtlasIT` send the bodies built by the catalog exporters to a real server. They
are skipped unless a system property gives the server URL, so `./mvnw verify` never needs Docker. Start a server and
run one test class, for example:

```bash
./mvnw test -Dtest=OpenMetadataIT -Dsurefire.failIfNoSpecifiedTests=false -Dlinneo.it.openmetadata=http://localhost:8585
```

| Test | Property | Server |
|---|---|---|
| `OpenMetadataIT` | `-Dlinneo.it.openmetadata=http://localhost:8585` | The official release `docker-compose.yml` of OpenMetadata, `docker compose up -d openmetadata-server` |
| `DataHubIT` | `-Dlinneo.it.datahub=http://localhost:8080` | DataHub's `docker/quickstart/docker-compose.quickstart-profile.yml` with `--profile quickstart-backend`; set `DATAHUB_TOKEN_SERVICE_SIGNING_KEY` and `DATAHUB_TOKEN_SERVICE_SALT` to random values first |
| `AtlasIT` | `-Dlinneo.it.atlas=http://localhost:21000` | `docker run -d -p 21000:21000 sburn/apache-atlas:2.3.0` (Atlas takes a few minutes to start) |

They were written against OpenMetadata 2.0.6, DataHub v1.7.0.1 and Apache Atlas 2.3.0.

## Rules for the taxonomy

- Never change the meaning of an existing key and never reuse a removed one.
- Add terms as children of existing terms. New roots need a discussion first.
- Protection may only become stricter down the tree; the build enforces it.
- Deprecate instead of deleting: give `version_deprecated` and, where there is one, `replaced_by`.
- Do not edit the `fideslang-*.tsv` files by hand. They are regenerated from a fideslang release with
  `tools/fideslang/update.py`; Linneo's terms go in the `linneo-*.tsv` files.

## Code conventions

- No runtime dependencies.
- Public types are documented, with `@since` on new ones.
- Every source file starts with the Apache License header.
- Commit messages explain why a change is made.

## Documentation

The documentation in `docs/` is published with GitHub Pages at <https://devbpm.github.io/linneo/>, using Jekyll
and the [Just the Docs](https://just-the-docs.com/) theme. The `Pages` workflow builds it on every pull request that
touches `docs/` and deploys it from `main`.

- **Two languages, one structure.** `docs/es/` mirrors `docs/en/` file by file. A change to a page goes into both
  languages in the same pull request; code, identifiers and keys stay the same in both.
- **Front matter.** Every page starts with Just the Docs front matter: `title`, `parent` (and `grand_parent` for
  the annotation pages) and `nav_order`; section index pages (`README.md`) also set `permalink`. Spanish pages use
  the Spanish titles of their parents (`Referencia`, `Anotaciones`, `Curso`).
- **Source mapping.** Right after the front matter, a hidden comment lists the source files the page documents:

  ```markdown
  <!--
  doc-sources:
    - src/main/java/io/github/devbpm/linneo/annotation/Category.java
  -->
  ```

  When a change touches one of those files, update the pages that list it. Contributors using Claude Code get this
  done by the `docs-updater` subagent (`.claude/agents/docs-updater.md`): a `PreToolUse` hook
  (`.claude/hooks/docs-gate.sh`) blocks `git commit` from Claude Code until the subagent has reviewed the current
  library changes.
- **Links.** Link between pages with relative links to the `.md` files; Jekyll rewrites them to the published URLs.
  Heading anchors follow GitHub's rules, so the same links work on GitHub and on the site.
- **Collapsible blocks.** Write `<details markdown="block">` so that the Markdown inside is rendered on the site.
- **Liquid.** Jekyll treats `{{` and `{%` as template syntax. Wrap code samples that contain them in
  `{% raw %}` and `{% endraw %}`.
- **Code samples** must compile against the current API.

By contributing, you agree that your contributions are licensed under the Apache License, Version 2.0.
