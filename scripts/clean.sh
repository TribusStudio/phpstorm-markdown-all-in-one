#!/usr/bin/env bash
# Clean build artifacts
set -euo pipefail

echo "=== Cleaning build artifacts ==="
/workspace/gradlew clean "$@"
echo "=== Clean complete ==="
