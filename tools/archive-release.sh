#!/usr/bin/env bash
# Archive a superseded GitHub release:
#   - delete its downloadable assets
#   - mark it as a pre-release
#   - leave a notice pointing at the latest release
#
# Usage: tools/archive-release.sh v2.5.1-en.2
set -euo pipefail

repo="${REPO:-kpgc10kai/Shark}"
tag="${1:?usage: tools/archive-release.sh <tag>}"
tag="${tag#v}"

if ! gh release view "v$tag" --repo "$repo" >/dev/null 2>&1; then
    echo "release v$tag not found in $repo" >&2
    exit 1
fi

assets=$(gh release view "v$tag" --repo "$repo" --json assets --jq '.assets[].name')
while IFS= read -r asset; do
    [ -n "$asset" ] || continue
    echo "deleting $asset"
    gh release delete-asset "v$tag" "$asset" --repo "$repo" --yes
done <<< "$assets"

gh release edit "v$tag" \
    --repo "$repo" \
    --prerelease \
    --title "[ARCHIVED] Shark v$tag" \
    --notes "This build is archived and its APK has been withdrawn.

Download the latest release: https://github.com/$repo/releases/latest"

echo "archived v$tag"
