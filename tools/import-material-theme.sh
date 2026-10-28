#!/usr/bin/env bash
#
# Replaces the bundled sample theme with the full upstream Material Icon Theme.
#
#   tools/import-material-theme.sh            # latest release
#   tools/import-material-theme.sh 5.40.0     # pinned release
#
# Requirements: bash, curl, tar, python3, and Chrome or Chromium (used by
# tools/reskin_folders.py to measure the upstream folder artwork).
set -euo pipefail

version="${1:-latest}"
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
destination="$project_root/src/main/resources/themes/material"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

echo "Resolving material-icon-theme@${version}..."
tarball_url="$(curl -fsSL "https://registry.npmjs.org/material-icon-theme/${version}" \
    | python3 -c 'import json, sys; print(json.load(sys.stdin)["dist"]["tarball"])')"

echo "Downloading ${tarball_url}"
curl -fsSL "$tarball_url" -o "$work_dir/theme.tgz"

tar -xzf "$work_dir/theme.tgz" -C "$work_dir" \
    package/dist/material-icons.json \
    package/icons \
    package/LICENSE \
    package/package.json

resolved_version="$(python3 -c "import json; print(json.load(open('${work_dir}/package/package.json'))['version'])")"

rm -rf "$destination"
mkdir -p "$destination"
cp "$work_dir/package/dist/material-icons.json" "$destination/material-icons.json"
cp -R "$work_dir/package/icons" "$destination/icons"
cp "$work_dir/package/LICENSE" "$destination/LICENSE"

echo "Re-skinning folder icons with the IntelliJ folder silhouette..."
python3 "$project_root/tools/reskin_folders.py" \
    --theme-dir "$destination" \
    --geometry "$project_root/tools/folder-geometry.json"

echo "Imported material-icon-theme ${resolved_version} ($(find "$destination/icons" -name '*.svg' | wc -l) icons) into ${destination}"
echo "The plugin resolves icon paths by file name, so no manifest changes are required."
