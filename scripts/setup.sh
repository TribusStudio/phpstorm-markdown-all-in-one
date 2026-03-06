#!/usr/bin/env bash
# Initial setup script — runs once after container creation
set -euo pipefail

echo "=== Markdown All-in-One Plugin — Setup ==="

# Generate Gradle wrapper if missing
if [ ! -f "/workspace/gradlew" ]; then
    echo "Generating Gradle wrapper..."
    GRADLE_VERSION=$(grep 'distributionUrl' /workspace/gradle/wrapper/gradle-wrapper.properties | sed 's/.*gradle-\(.*\)-bin.zip/\1/')

    # Download and run Gradle to generate the wrapper
    TEMP_DIR=$(mktemp -d)
    curl -sL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "${TEMP_DIR}/gradle.zip"
    unzip -q "${TEMP_DIR}/gradle.zip" -d "${TEMP_DIR}"
    "${TEMP_DIR}/gradle-${GRADLE_VERSION}/bin/gradle" wrapper --gradle-version "${GRADLE_VERSION}" --project-dir /workspace
    rm -rf "${TEMP_DIR}"

    echo "Gradle wrapper generated (v${GRADLE_VERSION})"
fi

# Pre-download dependencies
echo "Downloading dependencies (this may take a few minutes on first run)..."
/workspace/gradlew --project-dir /workspace dependencies --quiet || true

echo ""
echo "=== Setup complete ==="
echo "Available commands:"
echo "  ./dev build     — Build the plugin"
echo "  ./dev test      — Run tests"
echo "  ./dev run       — Launch PHPStorm sandbox with plugin"
echo "  ./dev package   — Build distributable .zip"
echo "  ./dev verify    — Verify plugin compatibility"
echo "  ./dev clean     — Clean build artifacts"
echo "  ./dev shell     — Open a shell in the container"
echo "  ./dev help      — Show all commands"
