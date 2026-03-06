#!/usr/bin/env bash
# Build the distributable plugin .zip
set -euo pipefail

echo "=== Packaging plugin ==="
/workspace/gradlew buildPlugin "$@"
echo ""
echo "=== Package complete ==="
echo "Output: build/distributions/"
ls -lh /workspace/build/distributions/*.zip 2>/dev/null || echo "(no .zip found — build may have failed)"
