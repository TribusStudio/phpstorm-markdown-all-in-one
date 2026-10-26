# Design — Optional Mermaid injection into fences

## Context

See `proposal.md` — Why. What was verified in the installed IDE (PhpStorm 2026.2, `PS-262.10315.130`):

- `com.intellij.mermaid` registers a complete Mermaid language in its **core** descriptor: `fileType` (`mermaid;mmd`), `lang.parserDefinition`, `lang.syntaxHighlighter`, `completion.contributor`, `lang.commenter`, `lang.braceMatcher`, `lang.formatter`, folding builders, five `localInspection`s, `spellchecker.support`, and a `colorSettingsPage` with its own `additionalTextAttributes`. That is the whole payoff of this change, and it is all registered against `language="Mermaid"`.
- Its Markdown integration is a **separate** optional content module, `intellij.mermaid.markdown`, registering into `org.intellij.markdown.*` extension points. Those belong to the bundled Markdown plugin and are unreachable from our PSI — which is why injection has to be ours.
- Its core descriptor declares `<dependencies><module name="intellij.markdown"/></dependencies>`, and `intellij.markdown` is a content module of `org.intellij.plugins.markdown` (`loading="embedded"`, `visibility="public"`). So the Mermaid plugin cannot load without the bundled Markdown plugin. This is the fact that makes a hard dependency unacceptable and an optional one correct.
- Its own fence recognition is configurable through an `advancedSetting`, `mermaid.additional.code.fence.pattern.regex` — evidence that info-string variation in the wild is real enough that JetBrains made it user-extensible.

Our side: `add-mermaid-diagram-preview` introduces `isDiagramFence(info)` in the converter as the single recognition predicate. Our Markdown language and PSI are our own, so the injection host is ours to define.

## Goals / Non-Goals

**Goals**

- Borrow an entire language's editor support for the cost of an injection.
- Keep the optional dependency genuinely optional: our plugin's core descriptor must not reference it, and absence must be indistinguishable from today.
- One recognition predicate shared with the preview, so editor and preview cannot disagree.

**Non-Goals (design level)**

- A general fence-injection framework. Doing Mermaid only, with no extension seams, is deliberate: a generic version needs a language-mapping registry, per-language settings, and an answer for languages whose plugins are absent. That is a separate proposal, and building half of it here would prejudge it.
- Shadowing anything the Mermaid plugin provides — colours, inspections, settings.
- Detecting the plugin at runtime beyond what optional loading already does.

## Decisions

### 1. Optional dependency carries everything, in its own descriptor

The injection contributor and its settings row are registered **only** in an optional configuration file conditioned on `com.intellij.mermaid`. Our main `plugin.xml` gains no dependency and no reference to Mermaid classes.

*Why:* this is what makes absence free rather than merely handled. The platform does not load the optional descriptor when the plugin is missing or disabled, so there is no availability check to get wrong, no class-loading failure to catch, and nothing to log. The spec's "no log noise" and "no prompting" scenarios then hold by construction rather than by diligence.

*Consequence:* no code outside that optional descriptor may reference the Mermaid language ID, or we lose the guarantee. A test asserts our main descriptor contains no Mermaid reference.

### 2. Injection by language ID, not by class

The contributor names the language by its registered ID string and resolves it through the platform's language registry, rather than importing a class from the Mermaid plugin. If the plugin's internals change shape, the lookup fails and injection silently does not happen — the spec's degradation requirement — instead of our plugin failing to load.

*Alternative rejected:* compiling against the Mermaid plugin's API. It would be type-safe against today's build, but it pins us to a plugin whose `since-build` equals its `until-build`, i.e. one that is re-released with every IDE build. A string ID is the looser, more durable coupling here, and the thing it buys — surviving the plugin's churn — matters more than compile-time checking of a single identifier.

### 3. Reuse the converter's recognition predicate

The injection host test calls the same `isDiagramFence` the converter uses. The alternative — a second predicate in the editor — guarantees eventual divergence, where the preview renders a diagram the editor treats as plain text or the reverse. The spec states the no-divergence requirement explicitly so a test can hold it.

This creates a soft ordering preference: landing `add-mermaid-diagram-preview` first means the predicate already exists. If this change lands first it introduces the predicate and the other reuses it. Either order works; neither is a dependency.

### 4. Inject the content range only, via the fence's PSI

The injected range is the fence body: everything after the opening fence line's newline, up to the closing fence. Markers and info string stay Markdown. An unterminated fence injects to the end of its content and no further.

The range is derived from our own fence PSI rather than by re-scanning text, so it stays correct under incremental reparse. A `LiteralTextEscaper` maps offsets straight through, since fence content has no escaping.

### 5. Setting is conditionally registered, not conditionally hidden

The settings row lives in the optional descriptor too, so when the plugin is absent the row does not exist rather than existing-and-hidden. That satisfies the spec's "not presented as an available option" without conditional UI logic in the main settings page.

The persisted field itself lives in the main `MarkdownSettings.State`, since state must deserialise identically whether or not the optional descriptor loaded. A default of `true` means an existing persisted state picks up the feature on upgrade.

## Risks / Trade-offs

- **Coupling to another plugin's language ID** → Resolved by registry lookup, failing to no-injection rather than to an error (Decision 2); the degradation scenarios in the spec are the guard.
- **The Mermaid plugin's presence across 2025.1–2026.2 is unverified** → Confirmed bundled in 2026.2 only. Absence at the floor degrades to today's behaviour, so this is a question about how much of the range benefits, not about correctness. Task 1 establishes it.
- **A second language's inspections inside Markdown may be unwelcome** → That is what the setting is for, defaulting on because the highlighting alone is worth more than the inspections cost.
- **Injection can be a performance trap** on large or numerous hosts → Spec requires measured parity with injection disabled, on both many-fence and large-fence documents.
- **The optional-descriptor guarantee is easy to erode later** by adding a Mermaid reference to the main descriptor or to shared code → Pinned by a test over our main `plugin.xml` (Decision 1).
- **Users may believe the bundled plugin is what renders their diagrams** → It is not, and the preview change's documentation task says so; this change's documentation must not blur that, since it makes the plugin genuinely relevant to the editor while remaining irrelevant to the preview.

## Migration Plan

No migration. Additive, SemVer **minor**, and inert for anyone without the Mermaid plugin. Rollback is a revert, or the setting.

## Open Questions

- Whether to honour the Mermaid plugin's own `mermaid.additional.code.fence.pattern.regex` advanced setting, so a user who extended its recognition sees the same fences recognised in our editor. Deferred: it reads another plugin's advanced setting, which is a tighter coupling than Decision 2 allows, and the recognition set without it is already a superset of the common forms. Resolving it later changes neither the specs nor the approach.
