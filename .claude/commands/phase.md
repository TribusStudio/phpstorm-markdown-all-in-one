---
name: phase
description: Launch development of a specific roadmap phase (e.g., /phase 3)
user_invocable: true
---

# Phase Development Skill

The user wants to start working on a specific development phase from ROADMAP.md.

## Instructions

1. Read `ROADMAP.md` to find the requested phase and its task list
2. Read relevant existing source files to understand the current codebase state
3. For each task in the phase:
   - Implement the feature following CLAUDE.md coding conventions
   - Add tests as required by Testing Requirements
   - Update plugin.xml if new extension points are needed
4. After all tasks are complete:
   - Update `ROADMAP.md` — mark completed items with `[x]`, move `← NEXT` to the next phase
   - Update `README.md` with new feature documentation
   - Update `CHANGELOG.md` with a new version section
   - Bump the version using `./scripts/bump-version.sh minor` (or `patch` for bug-fix-only phases)
5. Build and test: `./dev build && ./dev test`
6. Commit and push when ready

## Phase argument

The argument is the phase number, e.g., `/phase 3` starts Phase 3: Table of Contents.
