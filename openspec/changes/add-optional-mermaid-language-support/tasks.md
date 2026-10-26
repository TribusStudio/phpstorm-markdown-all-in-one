# Tasks — Optional Mermaid injection into fences

## 1. Establish availability across the range

- [ ] 1.1 Determine whether `com.intellij.mermaid` is bundled in PhpStorm 2025.1, 2025.2, 2025.3, 2026.1, and 2026.2, and whether it is enabled by default in each. Confirmed bundled in 2026.2 (`PS-262.10315.130`); the rest are unverified.
- [ ] 1.2 Confirm the Mermaid language's registered ID string on the oldest and newest builds where it is present, since the injection resolves it by ID.
- [ ] 1.3 Record the findings, and the bundled plugin's architecture as inspected (core language descriptor vs. the `intellij.mermaid.markdown` module, and its `intellij.markdown` dependency), in a new numbered doc.

## 2. Optional descriptor wiring

- [ ] 2.1 Add an optional dependency on `com.intellij.mermaid` with its own configuration file, registering the injection contributor and the settings row and nothing else, with extension points marked `dynamic="true"`.
- [ ] 2.2 Add the persisted `var mermaidInjectionEnabled: Boolean = true` to `MarkdownSettings.State` in the **main** descriptor's code, so state deserialises identically whether or not the optional descriptor loads.
- [ ] 2.3 Add a test asserting the main `plugin.xml` contains no reference to Mermaid, so the optional guarantee cannot be eroded later.
- [ ] 2.4 Confirm the plugin loads cleanly with the Mermaid plugin disabled, with no error and nothing in the IDE log.

## 3. The injection

- [ ] 3.1 Implement the injection contributor: host-test via the converter's `isDiagramFence` predicate, target language resolved by ID through the platform's language registry, failing to no-injection when the lookup fails.
- [ ] 3.2 Derive the injected range from the fence's PSI — content only, excluding markers and info string — with a pass-through `LiteralTextEscaper`.
- [ ] 3.3 Gate injection on `mermaidInjectionEnabled`.
- [ ] 3.4 Add `BasePlatformTestCase` tests for the injected range: a simple fence, a fence with an attribute-bearing info string, a tilde fence, a four-backtick fence, an unterminated fence, a fence containing text that looks like a closing fence, and consecutive fences.
- [ ] 3.5 Test that no injection occurs for non-diagram fences, for an empty info string, or when the setting is off.
- [ ] 3.6 Test that the predicate shared with the converter is genuinely the same one, so editor and preview cannot diverge on what counts as a diagram fence.

## 4. Verify in a running IDE

- [ ] 4.1 With the Mermaid plugin enabled, confirm inside a fence: highlighting, completion, brace matching, commenting, folding, and at least one of its inspections firing at the right position.
- [ ] 4.2 Confirm fence markers, the info string, and surrounding Markdown are still treated as Markdown, and that other languages' fences are unchanged.
- [ ] 4.3 Disable the Mermaid plugin, restart, and confirm fences revert to plain text with no error and no log entry; re-enable and confirm support returns.
- [ ] 4.4 Confirm the injection setting is absent from settings when the Mermaid plugin is unavailable, and present when it is.
- [ ] 4.5 Confirm preview diagram rendering works with the Mermaid plugin disabled, and that injection works with diagram rendering disabled.
- [ ] 4.6 Measure typing latency against injection-disabled, in a document with twenty fences and in one with a several-hundred-line fence.
- [ ] 4.7 Confirm no prompt, notification, or nudge about installing or enabling anything appears at any point.

## 5. Ship

- [ ] 5.1 Run `./dev test` and fix any regressions.
- [ ] 5.2 Document the feature in `README.md` as conditional on the bundled JetBrains Mermaid plugin, stating plainly that preview rendering does **not** depend on it — the distinction users are most likely to get wrong.
- [ ] 5.3 Bump the minor version with `./scripts/bump-version.sh minor` and add the `CHANGELOG.md` entry.
- [ ] 5.4 Update `ROADMAP.md` per the project's phase conventions.
- [ ] 5.5 Commit on a `feature/optional-mermaid-language` branch using Conventional Commits, and open a PR whose test plan is section 4.
