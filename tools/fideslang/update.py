#!/usr/bin/env python3
# Copyright 2026 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Regenerates the bundled fideslang taxonomy and the key-constant classes from a fideslang release.

Usage, from the repository root:

    python -I tools/fideslang/update.py 3.1.4

It downloads the release's source distribution from PyPI, checks it against the SHA-256 digest PyPI publishes,
reads src/fideslang/default_taxonomy/data_*.py without running them (the term definitions are parsed and only
their literal values are read) and writes:

  * src/main/resources/io/github/devbpm/linneo/internal/fideslang-data-{categories,uses,subjects}.tsv
  * src/main/java/io/github/devbpm/linneo/{Categories,DataUses,DataSubjects}.java (fideslang keys, then the
    keys of the linneo-*.tsv files)

Only the Python standard library is used. Update StandardTaxonomy.FIDESLANG_VERSION, NOTICE and the counts in
StandardTaxonomyTest when moving to another version.
"""
import ast
import hashlib
import io
import json
import os
import re
import sys
import tarfile
import urllib.request

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "src/main/resources/io/github/devbpm/linneo/internal")
JAVA = os.path.join(ROOT, "src/main/java/io/github/devbpm/linneo")
FIELDS = ["fides_key", "parent_key", "name", "description", "version_added", "version_deprecated", "replaced_by"]
MODULES = [("data_categories", "fideslang-data-categories.tsv"),
           ("data_uses", "fideslang-data-uses.tsv"),
           ("data_subjects", "fideslang-data-subjects.tsv")]

HEADER = """# Source: fideslang {version} default taxonomy, https://pypi.org/project/fideslang/{version}/
#   (source repository https://github.com/ethyca/fideslang, file src/fideslang/default_taxonomy/{module}.py)
# Copyright (c) Ethyca, Inc. Licensed under the Creative Commons Attribution 4.0 International licence
#   (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/); the package metadata declares Apache-2.0.
# Changes made by Linneo: converted from Python to tab-separated values; whitespace in descriptions
#   normalised; an empty version_added is written as 2.0.0, the value fideslang's default_factory assigns.
#   No key, parent, name or description was altered.
# Columns: key, parent_key, name, description, version_added, version_deprecated, replaced_by
key\tparent_key\tname\tdescription\tversion_added\tversion_deprecated\treplaced_by
"""

LICENSE_HEADER = """/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"""


def download_sources(version):
    with urllib.request.urlopen(f"https://pypi.org/pypi/fideslang/{version}/json") as response:
        release = json.load(response)
    sdist = next(u for u in release["urls"] if u["packagetype"] == "sdist")
    with urllib.request.urlopen(sdist["url"]) as response:
        data = response.read()
    digest = hashlib.sha256(data).hexdigest()
    if digest != sdist["digests"]["sha256"]:
        sys.exit(f"The download of fideslang {version} does not match PyPI's digest: {digest}")
    archive = tarfile.open(fileobj=io.BytesIO(data), mode="r:gz")
    sources = {}
    for member in archive.getmembers():
        for module, _ in MODULES:
            if member.name.endswith(f"src/fideslang/default_taxonomy/{module}.py"):
                if module in sources:
                    sys.exit(f"The release has two files for {module}")
                sources[module] = archive.extractfile(member).read().decode("utf-8")
    missing = [module for module, _ in MODULES if module not in sources]
    if missing:
        sys.exit(f"The release has no file for {missing}")
    return sources


def parse(source):
    """Reads the terms of a default_taxonomy module without running it: every call with a fides_key argument
    defines a term, in source order, and the fields Linneo keeps must be literals."""
    items = []
    for node in ast.walk(ast.parse(source)):
        if not isinstance(node, ast.Call):
            continue
        keywords = {keyword.arg: keyword.value for keyword in node.keywords if keyword.arg}
        if "fides_key" not in keywords:
            continue
        if node.args or any(keyword.arg is None for keyword in node.keywords):
            sys.exit(f"Line {node.lineno}: a term defined with positional or ** arguments cannot be read safely")
        item = {"_line": (node.lineno, node.col_offset)}
        for field in FIELDS:
            if field in keywords:
                try:
                    item[field] = ast.literal_eval(keywords[field])
                except ValueError:
                    sys.exit(f"Line {node.lineno}: {field} is not a literal value")
        items.append(item)
    items.sort(key=lambda item: item.pop("_line"))
    return items


def cell(value):
    return "" if value is None else " ".join(str(value).split())


def write_resource(name, module, version, items):
    with open(os.path.join(RES, name), "w", encoding="utf-8", newline="\n") as out:
        out.write(HEADER.format(version=version, module=module))
        for item in items:
            row = [cell(item.get(field)) for field in FIELDS]
            row[4] = row[4] or "2.0.0"
            out.write("\t".join(row) + "\n")


def rows(name):
    with open(os.path.join(RES, name), encoding="utf-8") as source:
        lines = [line for line in source.read().splitlines() if line.strip() and not line.startswith("#")]
    return [line.split("\t") for line in lines[1:]]


def javadoc(text):
    # A backslash first: javac reads unicode escapes such as \u002a\u002f anywhere, comments included, so text from
    # the release must never reach the source with one.
    return text.replace("&", "&amp;").replace("\\", "&#92;").replace("<", "&lt;").replace(">", "&gt;") \
        .replace("@", "&#64;").replace("*/", "*&#47;")


KEY = re.compile(r"[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*")


def checked_key(key):
    if not KEY.fullmatch(key):
        sys.exit(f"Not a valid key, refusing to write it into Java: {key!r}")
    return key


def write_constants(cls, what, version, fideslang_rows, linneo_rows):
    lines = [LICENSE_HEADER + "package io.github.devbpm.linneo;", "", "/**",
             f" * Keys of the {what} of the {{@linkplain Taxonomy#standard() standard taxonomy}}.",
             " * <p>",
             f" * The fideslang keys, names and descriptions come from the fideslang {version} default taxonomy,",
             " * &copy; Ethyca, Inc., licensed under CC BY 4.0 (see the {@code NOTICE} file). The keys added by",
             " * Linneo follow them.", " *", " * @since 0.1.0", " */", f"public final class {cls} {{", ""]
    for title, data in ((f"fideslang {version}", fideslang_rows), ("Linneo extensions", linneo_rows)):
        lines += [f"    // {title}", ""]
        for row in data:
            key, name, description = checked_key(row[0]), row[2], row[3]
            lines.append(f"    /** {javadoc(name)}: {javadoc(description)} */")
            lines.append(f"    public static final String {key.upper().replace('.', '_')} = \"{key}\";")
            lines.append("")
    lines += [f"    private {cls}() {{", "    }", "}", ""]
    with open(os.path.join(JAVA, cls + ".java"), "w", encoding="utf-8", newline="\n") as out:
        out.write("\n".join(lines))


def main(version):
    sources = download_sources(version)
    for module, resource in MODULES:
        items = parse(sources[module])
        write_resource(resource, module, version, items)
        print(f"{resource}: {len(items)} terms")
    write_constants("Categories", "data categories", version, rows("fideslang-data-categories.tsv"),
                    rows("linneo-data-categories.tsv"))
    write_constants("DataUses", "data uses (purposes of processing)", version, rows("fideslang-data-uses.tsv"),
                    rows("linneo-data-uses.tsv"))
    write_constants("DataSubjects", "data subjects", version, rows("fideslang-data-subjects.tsv"),
                    rows("linneo-data-subjects.tsv"))


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("usage: python -I tools/fideslang/update.py <fideslang-version>")
    main(sys.argv[1])
