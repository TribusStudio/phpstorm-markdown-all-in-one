#!/usr/bin/env bash
# bump-version.sh — Increment the plugin's SEMVER version
# Usage: ./scripts/bump-version.sh [major|minor|patch]
# Default: patch

set -euo pipefail

PROPERTIES_FILE="gradle.properties"
BUMP_TYPE="${1:-patch}"

if [[ ! -f "$PROPERTIES_FILE" ]]; then
    echo "Error: $PROPERTIES_FILE not found. Run from project root." >&2
    exit 1
fi

CURRENT_VERSION=$(grep '^pluginVersion' "$PROPERTIES_FILE" | cut -d'=' -f2 | tr -d ' ')
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

case "$BUMP_TYPE" in
    major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
    minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
    patch) PATCH=$((PATCH + 1)) ;;
    *)
        echo "Usage: $0 [major|minor|patch]" >&2
        exit 1
        ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"

if [[ "$(uname)" == "Darwin" ]]; then
    sed -i '' "s/^pluginVersion = .*/pluginVersion = ${NEW_VERSION}/" "$PROPERTIES_FILE"
else
    sed -i "s/^pluginVersion = .*/pluginVersion = ${NEW_VERSION}/" "$PROPERTIES_FILE"
fi

echo "${CURRENT_VERSION} -> ${NEW_VERSION}"
