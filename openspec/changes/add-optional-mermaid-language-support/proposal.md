# Optional in-editor Mermaid language support inside fences

## Why

**Symptom:** the source inside a ```` ```mermaid ```` fence is dead text in our editor — one flat colour, no completion, no structural feedback. A user who has PhpStorm's bundled Mermaid plugin enabled gets all of that when editing a `.mermaid` file, and gets it inside Markdown fences too *if* they use JetBrains' Markdown plugin. With ours, they get nothing.

**Root cause:** the bundled Mermaid plugin's Markdown integration is registered against the bundled Markdown plugin's PSI and extension points (`org.intellij.markdown.fenceLanguageProvider` and friends). Our plugin has its own Markdown language and its own PSI, so none of that reaches our fences. Nothing is broken — the wiring simply has no path to us.

**Fix:** when `com.intellij.mermaid` is present and enabled, inject the Mermaid language into the content of our `mermaid` fences, so the platform's language-injection machinery gives us its highlighting, completion, brace matching, folding, formatting, and inspections inside our editor for free. When it is absent or disabled, nothing changes and nothing is lost.

This is worth doing because the cost is close to zero and the ceiling is high: the bundled plugin ships a full Mermaid language — parser, highlighter, completion contributor, formatter, folding, five inspections, spellchecking, and a colour settings page. Reimplementing any of that would be a large change for a payoff we can borrow.

**Why this is safe, when depending on that plugin generally is not.** A hard dependency on `com.intellij.mermaid` is out of the question: it declares `<module name="intellij.markdown"/>`, a content module owned by `org.intellij.plugins.markdown`, so requiring it would transitively require the bundled Markdown plugin — exactly what this project is built to work without. An *optional* dependency inverts that: the extra wiring loads only if the Mermaid plugin is loadable, and our plugin's core is untouched when it is not. Our standalone guarantee survives intact.

## What Changes

- An optional dependency on `com.intellij.mermaid`, in its own configuration file, carrying everything in this change. Our main descriptor gains no dependency.
- When that configuration loads, a language-injection contributor injects the Mermaid language into the content range of fences our converter already recognises as diagram fences, reusing the same recognition predicate so the editor and the preview can never disagree about what is a diagram.
- Injection covers the fence body only — never the fence markers or the info string.
- When the Mermaid plugin is absent, disabled, or fails to load, fences render exactly as they do today, with no error and no log noise.
- A setting controlling the injection, defaulting to enabled, shown only when the plugin is available — some users will not want a second language's inspections inside their Markdown.

### Non-goals

- **Any dependency on the bundled Markdown plugin**, directly or transitively through a non-optional path. This is the constraint the whole change is shaped around.
- **Preview rendering.** That is `add-mermaid-diagram-preview`, which bundles its own runtime and works whether or not this change ships. The two are independent in both directions.
- **Bundling or reimplementing a Mermaid language** of our own for users without the plugin. If the plugin is not there, fences stay plain text. Writing a Mermaid parser is a different order of work and a different proposal.
- **Prompting, nudging, or auto-enabling.** We will not ask the user to install or enable the Mermaid plugin, and we will not enable it programmatically — a user who disabled it may well have done so *because* they installed ours. Availability is detected, never solicited.
- **Our own Mermaid colour keys, inspections, or settings pages.** We borrow the plugin's; we do not shadow them. Note that it registers its own colour keys and ships `colorSchemes/MermaidDefault.xml`, so any key we ever add in this area must be `MDAIO_`-prefixed, for the reasons recorded in `recover-renamed-color-scheme-keys`.
- **Other fenced languages.** Generic fence-language injection for `kotlin`, `php`, `sql` and the rest is a much larger feature with its own design; this change is Mermaid only and deliberately does not lay groundwork for it.

## Capabilities

### New Capabilities

- `editor/fence-language-injection`: the observable contract for injecting a language into a Markdown fence's content — what range is injected, what the user gets from it, and how the editor behaves when the providing plugin is unavailable or the feature is switched off.

### Modified Capabilities

None — `openspec/specs/` holds no existing requirement for fence injection.

## Impact

**Code**

- A new optional configuration file under `META-INF/`, referenced from `plugin.xml` as an optional dependency on `com.intellij.mermaid`.
- A new injection contributor in `lang/` or `editor/`, registered only in that optional file.
- The diagram-fence recognition predicate introduced by `add-mermaid-diagram-preview` is reused rather than duplicated — which creates a soft ordering preference between the two changes, not a dependency.
- `settings/` — one persisted boolean and one conditionally-visible row.

**Platform APIs**

`com.intellij.lang.injection.MultiHostInjector` or the `languageInjectionContributor` extension point, `com.intellij.psi.LiteralTextEscaper` for the injected range, `PluginManagerCore`/`PluginId` only if availability needs checking beyond what optional loading already guarantees, and the optional-dependency mechanism in the plugin descriptor itself. Extension points declared `dynamic="true"`.

**Dependencies**

An optional dependency on a bundled plugin whose own versioning is pinned to the IDE build (`since-build` equal to `until-build`). We depend on its language ID and nothing else, which is the narrowest possible coupling, but it is still coupling to another plugin's internals and the spec requires it to degrade silently when the plugin changes shape.

**Versions in scope**

PhpStorm 2025.1 through 2026.2. The Mermaid plugin's presence across that whole range is unverified — it was confirmed bundled in 2026.2 (`PS-262.10315.130`) but not checked at the 2025.1 floor. Since the dependency is optional, absence at the floor degrades to today's behaviour rather than breaking anything, but it does determine how much of the range actually benefits.

**Users**

Purely additive, and invisible to anyone without the Mermaid plugin. For those with it, Markdown fences gain another language's inspections and completion inside them, which is why the setting exists.
