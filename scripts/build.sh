#!/usr/bin/env bash
# Build the plugin
set -euo pipefail

echo "=== Building Markdown All-in-One ==="
/workspace/gradlew build "$@"
echo "=== Build complete ==="
