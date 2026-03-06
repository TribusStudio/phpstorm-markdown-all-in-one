#!/usr/bin/env bash
# Run the test suite
set -euo pipefail

echo "=== Running tests ==="
/workspace/gradlew test "$@"
echo "=== Tests complete ==="
