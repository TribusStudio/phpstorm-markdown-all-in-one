---
name: ship
description: Review changes, commit, push, and build locally. Use /ship to ship the current work.
user_invocable: true
---

# Ship Skill

Review all pending changes, commit with a conventional commit message, push to remote, and run a local build.

## Steps

1. **Review changes** — Run `git status` and `git diff` to see all pending changes. Summarize what changed and verify nothing unexpected is included (no secrets, no accidental files).

2. **Build & test** — Run `./dev build` to verify the code compiles and all tests pass. If the build fails, fix the issue before proceeding.

3. **Commit** — Stage all relevant files and create a commit using Conventional Commits format (`feat`, `fix`, `docs`, etc.). Include a concise description of the changes. End the commit message with `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>`.

4. **Push** — Push to the remote repository.

5. **Local package** — Run `./dev package` to build the distributable .zip for local installation.

6. **Report** — Summarize what was shipped: version, commit hash, artifact path.

## Important

- Always bump the version BEFORE shipping if it hasn't been bumped yet for this set of changes
- Never force push
- Never skip hooks
- If tests fail, fix and retry — do not ship broken code
