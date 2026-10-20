#!/usr/bin/env bash
# Blocks `git commit` while the library has changes that the docs-updater subagent has not reviewed.
#
#   docs-gate.sh            check mode (PreToolUse hook): exit 0 = allow, exit 2 = block
#   docs-gate.sh --stamp    record the current library diff as reviewed (run by docs-updater)
#   docs-gate.sh --files    list the changed library files the docs must be checked against
#
# The fingerprint covers every change to the library paths against HEAD (staged and unstaged),
# so editing the library again after a review requires a new review.
set -euo pipefail

root=$(git rev-parse --show-toplevel)
cd "$root"
stamp=.claude/docs-sync.stamp
paths=(src/main tools/fideslang pom.xml)

if git rev-parse --verify -q HEAD >/dev/null; then
    base=HEAD
else
    base=$(git hash-object -t tree /dev/null)   # no commits yet: compare against the empty tree
fi

changed=$(git diff --name-only "$base" -- "${paths[@]}"; git diff --cached --name-only "$base" -- "${paths[@]}")
changed=$(printf '%s\n' "$changed" | sed '/^$/d' | sort -u)
fingerprint=$( { printf '%s\n' "$changed"; git diff "$base" -- "${paths[@]}"; } | git hash-object --stdin)

case "${1:-}" in
    --stamp)
        printf '%s\n' "$fingerprint" > "$stamp"
        echo "docs-gate: recorded $fingerprint"
        exit 0 ;;
    --files)
        printf '%s\n' "$changed"
        exit 0 ;;
esac

[ -z "$changed" ] && exit 0
[ -f "$stamp" ] && [ "$(cat "$stamp")" = "$fingerprint" ] && exit 0

{
    echo "docs-gate: the library changed and the documentation has not been reviewed for these changes."
    echo "Before committing, run the docs-updater subagent (Agent tool, subagent_type \"docs-updater\")."
    echo "It updates only the affected pages under docs/ and then runs '.claude/hooks/docs-gate.sh --stamp'."
    echo "Stage the doc changes it reports, then retry the commit."
    echo "Changed library files:"
    printf '  %s\n' $changed
} >&2
exit 2
