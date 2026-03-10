# JetBrains Bundled Markdown Plugin — Comparison & Lessons Learned

**Date:** 2026-03-10
**Reference:** https://github.com/JetBrains/intellij-community/tree/master/plugins/markdown
**Purpose:** Deep comparison of JetBrains' bundled Markdown plugin vs our plugin, identifying architectural lessons, feature gaps, and areas where we're stronger.

---

## Architecture Comparison

| Aspect | JetBrains Bundled | Our Plugin |
|--------|-------------------|------------|
| **Modules** | 13 modules across 5 JARs (core, fenceInjection, frontmatter, images, compose preview, etc.) | Single module, single JAR |
| **Parser** | Own `intellij-markdown` library → PSI bridge via `PsiBuilderFillingVisitor` | Hand-written `MarkdownLexer` + `MarkdownParserDefinition` with custom HTML converter |
| **Preview engine** | JCEF + IncrementalDOM (efficient DOM patching, no full page replacement) | JCEF + full HTML replacement via `loadHTML()` |
| **Preview resources** | Built-in HTTP server (`PreviewStaticServer`) serves local files | Direct `file://` URL resolution for images |
| **Extension points** | 11 custom dynamic extension points (flavours, fence providers, preview extensions, export providers) | Zero custom extension points |
| **Settings scope** | Project-level (`SimplePersistentStateComponent`) + application-level services | Application-level only (`PersistentStateComponent`) |
| **Scroll sync** | Multi-layer: line-based + boundary overflow (`PreciseVerticalScrollHelper`) + bidirectional `BrowserPipe` | Line-based (`data-source-line`) + bidirectional `JBCefJSQuery` |
| **Math support** | First-class: `INLINE_MATH`/`BLOCK_MATH` PSI elements + MathJax in preview | None |
| **Diagram support** | PlantUML built-in (JAR manager), Mermaid via plugin advertisement | None |
| **Table editing** | Full column/row operations (insert, delete, swap, align), inlay controls, inspections | Format, alignment detection, Tab navigation, width limiting |
| **TOC** | Minimal: generate + outdated inspection, `<!-- TOC -->` markers, no settings | Comprehensive: levels, slug modes, named TOCs, ranges, omit markers, auto-update, section numbers |
| **Code fence injection** | Full language injection (syntax highlighting, completion, inspections inside fences) | Language tag in `<code class="language-X">` for preview highlighting only |
| **Stub indexing** | Yes (`IStubFileElementType`) for fast heading navigation | No |
| **Export** | Extension point with HTML/PDF/DOCX providers | HTML only (single + batch), styled, base64 images, link validation |

---

## Feature-by-Feature Comparison

### Where JetBrains Is Stronger

| Feature | JetBrains | Us | Impact |
|---------|-----------|-----|--------|
| **IncrementalDOM preview** | Efficient DOM patching — only changed elements update | Full HTML replacement on every edit | Performance on large documents |
| **Language injection in fences** | Full IDE support inside code fences (highlighting, completion, errors) | Preview-only syntax highlighting via highlight.js | Major DX gap |
| **Math rendering** | `$...$` and `$$...$$` as first-class PSI elements + MathJax | Not supported | Notable for academic/technical users |
| **PlantUML** | Built-in with JAR manager and download gutter icon | Not supported | Niche but valued |
| **Table column/row operations** | Insert/delete/swap columns and rows, alignment actions, inlay UI | Format + Tab navigation only | Power user gap |
| **Boundary scroll** | `PreciseVerticalScrollHelper` — mouse wheel overflow flows into preview | Standard scroll sync only | Polish detail |
| **Structure view** | PSI-based heading hierarchy in Structure tool window | Not implemented | IDE integration gap |
| **Code folding** | Dedicated `MarkdownCodeFoldingSettings` | Not implemented | Editor convenience |
| **Spellchecking** | Dedicated spellchecker module | Relies on IDE default | Minor |
| **Project-level settings** | Settings per-project via `SimplePersistentStateComponent` | Global only | Multi-project workflows |
| **Extension point architecture** | 11 dynamic EPs — third parties can extend flavours, preview, export | Closed architecture | Ecosystem potential |
| **PDF/DOCX export** | Via `markdownExportProvider` extension point | HTML only | Export flexibility |
| **Front matter** | Dedicated module with YAML/TOML parsing | Skip in TOC only, no parsing | Minor |

### Where We Are Stronger

| Feature | Us | JetBrains | Impact |
|---------|-----|-----------|--------|
| **TOC system** | Comprehensive: 5 slug modes, named TOCs, content ranges, omit markers, auto-update on save, section numbering, multiple TOCs per doc, ordered/unordered, configurable levels & markers | Basic: generate + outdated inspection, no settings, no slug modes, no auto-update | **Major advantage** |
| **Selection wrapping** | Type `*`, `~`, `_`, `` ` ``, `\|`, `-` with selection to wrap contextually | Not available | Unique feature |
| **Floating selection toolbar** | Notion-style popup on text selection with quick formatting | Has floating toolbar but fewer options | UX advantage |
| **Context menu** | Context-aware right-click submenu (table, TOC, formatting awareness) | Standard actions only | Discoverability |
| **Generate menu** | 7 generators: Table, Code Block, Image, Footnote, Link Reference, Front Matter, TOC | Insert empty table + TOC only | Productivity |
| **File templates** | 6 templates: Blank, README, Document, Meeting Notes, Changelog, API Documentation | None | Onboarding |
| **Preview themes** | 4 themes (GitHub, GitHub Dark, GitLab, VSCode) + Auto + Custom CSS file | Custom CSS only, no theme chooser | User experience |
| **Preview zoom** | Zoom in/out/reset controls | Font size adjustment only | Convenience |
| **Smart table width** | Configurable max width with compact mode | Fixed formatting | Wide table handling |
| **Escaped pipe handling** | `\|` treated as literal in table parsing and preview rendering | Standard handling | Correctness |
| **Batch HTML export** | Folder-recursive batch export with styled output | Via extension point (implementation varies) | Out-of-box capability |
| **Editor toolbar** | Persistent toolbar with configurable display modes (icons/labels/both), tools menu | Action buttons (opt-in), minimal | Accessibility |
| **Smart paste** | URL over selection → markdown link, image URL detection | Not a dedicated feature | Quick linking |
| **Heading reference completion** | `[text](#` triggers heading slug completion | Available but less prominent | Linking convenience |
| **Backslash escape handling** | Full CommonMark escapable punctuation in converter and slugifier | Standard | Correctness |

### Feature Parity (Both Have)

- Toggle bold/italic/strikethrough/code span
- Heading level up/down
- Smart Enter (list continuation)
- Smart Backspace (list handling)
- List indent/outdent
- Task list toggle
- JCEF-based preview with split editor
- Bidirectional scroll sync
- Custom CSS support
- GFM table formatting
- Tab navigation in tables
- Auto-completion (file paths, heading refs, reference links)
- HTML export
- Code fence language detection

---

## Architectural Lessons

### 1. IncrementalDOM for Preview Updates

**Their approach:** Parse HTML with jsoup, convert to IncrementalDOM JS calls (`elementOpen`/`text`/`elementClose`), execute in JCEF. Only changed DOM nodes update.

**Our approach:** Full `loadHTML()` replacement on every document change.

**Lesson:** For large documents, our approach causes visible flicker and resets scroll position. IncrementalDOM is the correct solution for production-quality preview. However, this is a **significant refactor** — the entire HTML-to-JS conversion pipeline needs to be built.

**Recommendation:** Add to a future phase. For now, our approach works for typical document sizes. Could mitigate with debounced updates (already have 20ms debounce-like behavior via document change coalescing).

### 2. Language Injection in Code Fences

**Their approach:** Full IntelliJ language injection — code inside fences gets syntax highlighting, completion, inspections, and error checking from the target language's plugin.

**Our approach:** Language tag on the `<code>` element for highlight.js rendering in preview only. No editor-side intelligence.

**Lesson:** This is the single biggest DX gap. A developer writing SQL inside a markdown fence gets zero help from us but full SQL support from JetBrains. However, implementing proper language injection requires a robust PSI tree with correct range mapping — it's architecturally complex.

**Recommendation:** Track as a future phase. This would require significant parser work to produce proper PSI elements for code fence content ranges. Could potentially use `MultiHostInjector` with our existing lexer tokens if we add `CODE_FENCE_CONTENT` token ranges.

### 3. Extension Point Architecture

**Their approach:** 11 dynamic extension points let third-party plugins add preview renderers, export formats, parsing flavours, and fence language support.

**Our approach:** Closed architecture — all features are built-in.

**Lesson:** Extension points are critical for ecosystem growth. If a user wants Mermaid support, they shouldn't have to wait for us to build it — they should be able to install a companion plugin.

**Recommendation:** Define extension points in a future phase once the core feature set stabilizes. Priority EPs: `previewExtensionProvider` (inject JS/CSS into preview), `fenceLanguageProvider` (custom code fence handling), `exportProvider` (additional export formats).

### 4. Project-Level vs Application-Level Settings

**Their approach:** Core settings are project-level (different projects can have different markdown configurations).

**Our approach:** All settings are application-level (global).

**Lesson:** For monorepo users or people working on multiple projects with different conventions (e.g., one uses GitLab slugs, another uses GitHub), project-level settings matter.

**Recommendation:** Migrate to project-level settings in a future phase. The change is straightforward — swap `@Service(Service.Level.APP)` to `@Service(Service.Level.PROJECT)` and update all `getInstance()` calls to `getInstance(project)`.

### 5. Static Server for Preview Resources

**Their approach:** `PreviewStaticServer` (an `httpRequestHandler`) serves local files to JCEF over HTTP, with proper MIME types and security.

**Our approach:** Resolve image paths to `file://` URLs directly.

**Lesson:** `file://` URLs work but can have security restrictions in some JCEF configurations. An HTTP server is more robust and enables CSP headers, caching, and resource preprocessing.

**Recommendation:** Low priority. Our `file://` approach works. Consider if we encounter cross-origin or security issues.

### 6. Stub-Based Indexing

**Their approach:** `IStubFileElementType` enables fast heading access without full AST parsing — critical for Structure view and Go To Symbol.

**Our approach:** No stub indexing.

**Lesson:** Stub indexing is necessary for Structure view and project-wide heading search. Without it, every query requires full file parsing.

**Recommendation:** Required if we add Structure view support. Track alongside that feature.

---

## Quality Assessment: Are We Ready to Release?

### Strengths for Release

1. **Feature completeness for the target audience.** Our plugin covers the core markdown editing workflow thoroughly — formatting, lists, tables, TOC, preview, export. The VSCode-inspired feature set is well-implemented.

2. **TOC is best-in-class.** Our TOC system is significantly more capable than both VSCode's extension and JetBrains' bundled plugin. Named TOCs, content ranges, 5 slug modes, section numbering — this is a genuine differentiator.

3. **UX polish.** Selection wrapping, floating toolbar, context menu, generate menu, file templates, preview themes — these create a cohesive editing experience that neither competitor matches.

4. **Zero external dependencies.** Our converter, lexer, and renderer are all self-contained. No JAR downloads, no network requirements, no bundled libraries to maintain.

5. **Standalone design.** Users disable the bundled Markdown plugin and get a complete replacement. No conflicts, no overlap.

### Gaps That Are Acceptable for v1

1. **No language injection in fences.** This is JetBrains' marquee feature, but it requires deep PSI integration. Users still get preview-side highlighting. Acceptable for v1.

2. **No math/LaTeX.** Planned for Phase 10. Important for academic users but not blocking for general release.

3. **No IncrementalDOM.** Full HTML replacement works fine for typical documents (<1000 lines). Performance optimization can come later.

4. **No extension points.** Acceptable for a new plugin — define EPs once the API surface stabilizes after community feedback.

5. **No structure view.** Nice to have, not a blocker.

### Gaps That Should Be Addressed Before or Shortly After Release

1. **Preview-to-editor scroll accuracy** (tracked in Known Issues #1) — the experience should be polished.

2. **Mermaid rendering** (tracked in Known Issues #2) — increasingly expected by developers.

3. **Table column/row operations** — JetBrains has insert/delete/swap for columns and rows. We should add at least insert row/column as it's a common need.

### Verdict

**Yes, the plugin is ready for community release.** The feature set is comprehensive, the UX is polished, and our differentiators (TOC, toolbars, templates, selection wrapping) provide genuine value over the bundled plugin. The gaps (math, language injection, diagrams) are clearly scoped in the roadmap and don't block the core editing workflow.

The architectural choices we've made — single module, hand-written converter, application-level settings — are appropriate for the plugin's current maturity. They can evolve as the user base grows and requirements become clearer.
