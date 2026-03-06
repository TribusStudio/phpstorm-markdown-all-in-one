# 004 — Development Container Setup

**Date:** 2026-03-06
**Status:** Active

## Overview

The project uses a Docker devcontainer to provide a consistent, reproducible build environment. No JDK or Gradle installation is required on the host machine.

## Architecture

```
Host (macOS/Windows/Linux)
  |
  ├── ./dev              <- CLI entry point (bash script on host)
  |
  └── Docker Container (eclipse-temurin:21-jdk-jammy)
        ├── JDK 21       <- Build toolchain
        ├── Gradle 9.2.1 <- Build system (via wrapper)
        ├── Git          <- Version control
        ├── GitHub CLI   <- PR/release automation
        └── /workspace   <- Bind-mounted project directory
```

### Key Design Decisions

1. **Bind mount for source code** — Source files are mounted from the host, so edits in PHPStorm (or any editor) are immediately reflected in the container.

2. **Named volume for Gradle cache** — The `~/.gradle` directory is persisted in a Docker volume (`markdown-plugin-gradle-cache`) so dependencies survive container restarts. Only `./dev destroy` removes it.

3. **Non-root user** — The container runs as `dev` (UID 1000) to avoid file permission issues with bind mounts.

4. **Gradle wrapper generated at setup** — The wrapper script (`gradlew`) is generated inside the container during `./dev setup`, ensuring the correct Gradle version without requiring Gradle on the host.

## The `./dev` CLI

A single bash script at the project root provides all commands:

### Container Management

| Command | Description |
|---------|-------------|
| `./dev up` | Build image and start container |
| `./dev down` | Stop container |
| `./dev restart` | Restart container |
| `./dev status` | Show status, Java/Gradle versions |
| `./dev setup` | Generate Gradle wrapper, download dependencies |
| `./dev shell` | Open bash shell in container |
| `./dev logs` | Tail container logs |
| `./dev destroy` | Remove container AND Gradle cache volume |

### Build Commands

| Command | Description |
|---------|-------------|
| `./dev build` | Compile and build the plugin |
| `./dev test` | Run all tests |
| `./dev test --tests '*.SomeTest'` | Run specific test class |
| `./dev package` | Build distributable .zip |
| `./dev verify` | Verify IDE compatibility |
| `./dev clean` | Clean build artifacts |
| `./dev gradle <task>` | Run any Gradle task |

### Auto-start Behavior

All build commands (`build`, `test`, `package`, `verify`, `clean`, `gradle`) automatically start the container if it's not running. You can go straight to `./dev build` without running `./dev up` first.

## First-Time Setup

```bash
# 1. Start the container and run setup
./dev up
./dev setup

# 2. Verify everything works
./dev build
./dev test
```

## File Structure

```
.devcontainer/
  devcontainer.json      # Devcontainer spec (PHPStorm/VSCode/Codespaces)
  Dockerfile             # JDK 21 container image
docker-compose.yml       # Container orchestration
dev                      # Host-side CLI script
scripts/
  setup.sh               # First-run setup (Gradle wrapper, deps)
  build.sh               # Build script
  test.sh                # Test script
  package.sh             # Package script
  verify.sh              # Verification script
  clean.sh               # Clean script
```

## PHPStorm Integration

PHPStorm 2025.1+ has native devcontainer support:
1. Open the project in PHPStorm
2. PHPStorm detects `.devcontainer/devcontainer.json`
3. Click "Create Dev Container" in the notification
4. PHPStorm connects to the container for builds and indexing

Alternatively, use the `./dev` CLI from PHPStorm's built-in terminal.

## CI/CD

GitHub Actions does NOT use the devcontainer — it installs JDK 21 directly via `actions/setup-java` for faster CI builds. The devcontainer is for local development only.

## Troubleshooting

### Container won't start
```bash
./dev destroy    # Remove stale state
./dev up         # Rebuild from scratch
./dev setup      # Re-run setup
```

### Gradle cache issues
```bash
./dev destroy    # This removes the named volume
./dev up && ./dev setup   # Fresh cache
```

### Permission issues on Linux
The container user `dev` has UID 1000. If your host UID differs, rebuild with:
```bash
docker compose build --build-arg USER_UID=$(id -u) --build-arg USER_GID=$(id -g)
```
