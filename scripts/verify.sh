#!/usr/bin/env bash
# Verify plugin compatibility with target IDEs
set -euo pipefail

echo "=== Verifying plugin compatibility ==="
/workspace/gradlew verifyPlugin "$@"
echo "=== Verification complete ==="
