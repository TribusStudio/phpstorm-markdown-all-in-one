# /release — Prepare a release

Prepare a new release of the Markdown All-in-One plugin.

## Steps
1. Read the current version from `gradle.properties`
2. Ask the user for the new version number (or suggest one based on changes)
3. Update `gradle.properties` with the new version
4. Update `CHANGELOG.md` — move [Unreleased] items under the new version heading with today's date
5. Run `./gradlew build` and `./gradlew test` to verify everything passes
6. Run `./gradlew buildPlugin` to create the distribution zip
7. Create a git commit: `chore(release): vX.Y.Z`
8. Create a git tag: `vX.Y.Z`
9. Ask the user if they want to push the tag and trigger the CI/CD release pipeline
