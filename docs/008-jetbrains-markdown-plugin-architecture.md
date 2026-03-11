# 008 - JetBrains Bundled Markdown Plugin Architecture

Research date: 2026-03-10
Source: `JetBrains/intellij-community` master branch, `plugins/markdown/`

## Overview

The JetBrains bundled Markdown plugin (`org.intellij.plugins.markdown`) is a multi-module plugin using the IntelliJ Platform's module content system. It registers the `Markdown` language, provides syntax highlighting, a JCEF-based live preview, table editing, list handling, code fence injection, and export capabilities (HTML, DOCX, PDF via Pandoc).

**Plugin ID:** `org.intellij.plugins.markdown`
**Package:** `org.intellij.plugins.markdown`
**Category:** Languages
**Dependency:** `com.intellij.modules.lang` (only)
**Resource Bundle:** `messages.MarkdownBundle`

---

## Module Structure

The plugin is split into multiple modules, packaged into separate JARs:

### Main JAR (`lib/markdown.jar`)
Contains the core modules:

| Module Name | Location | Purpose |
|---|---|---|
| `intellij.markdown` | `core/` | Core plugin - language, PSI, editor, preview, settings, actions |
| `intellij.markdown.frontmatter` | `frontmatter/` | Front matter support (base) |
| `intellij.markdown.frontmatter.yaml` | `frontmatter/yaml/` | YAML front matter |
| `intellij.markdown.frontmatter.toml` | `frontmatter/toml/` | TOML front matter |
| `intellij.markdown.images` | `images/` | Image handling (registers `ProcessImagesExtension`) |
| `intellij.markdown.xml` | `xml/` | XML/HTML integration |
| `intellij.markdown.model` | `model/` | Data model |
| `intellij.markdown.spellchecker` | `spellchecker/` | Spell checking integration |
| `intellij.markdown.compose.preview` | `compose/` | Compose-based preview |

### Separate module JARs
| Module Name | JAR | Purpose |
|---|---|---|
| `intellij.markdown.backend` | `lib/modules/intellij.markdown.backend.jar` | Backend processing |
| `intellij.markdown.fenceInjection` | `lib/modules/intellij.markdown.fenceInjection.jar` | Code fence language injection |
| `intellij.markdown.frontend` | `lib/modules/intellij.markdown.frontend.jar` | Frontend editor support |
| `intellij.markdown.frontend.split` | `lib/modules/intellij.markdown.frontend.split.jar` | Split editor (editor + preview) |

### Plugin module (almost empty)
`plugin/` contains only `BUILD.bazel` and `intellij.markdown.plugin.main.iml` - the actual plugin descriptor lives in `core/resources/META-INF/plugin.xml`.

---

## Core Package Structure

All source lives under `core/src/org/intellij/plugins/markdown/`:

```
markdown/
  MarkdownBundle.java              # i18n resource bundle accessor
  MarkdownUsageCollector.kt        # Usage statistics
  braces/                          # Brace matching, quote handling
  breadcrumbs/                     # Breadcrumb navigation provider
  dto/                             # Data transfer objects
  editor/                          # Editor enhancements
    CodeFenceLanguageListCompletionProvider.kt
    EditorUtil.kt
    MarkdownBidiRegionsSeparator.java
    MarkdownCodeSpanExtendWordSelectionHandler.kt
    MarkdownCompletionContributor.kt
    MarkdownEnterHandler.kt
    MarkdownFocusModeProvider.kt
    MarkdownLineWrapPositionStrategy.kt
    MarkdownParagraphFillHandler.kt
    MarkdownStripTrailingSpacesFilterFactory.java
    MarkdownTypedHandler.java
    headers/                       # Header-level inference
    lists/                         # List editing (auto-numbering, indent, backspace)
    tables/                        # Table editing (formatting, handlers, inlays)
      actions/column/              # Column operations (swap, insert, remove, align, select)
      actions/row/                 # Row operations (swap, insert, remove, select)
      handlers/                    # Typed, backspace, enter, tab, shift-enter handlers
      inspections/                 # Table formatting inspections
      intentions/                  # Insert column, remove column/row, alignment
      ui/                          # Table inlay provider
    toc/                           # Table of contents generation + inspection
  extensions/                      # Plugin extension system
    common/                        # Base styles, inline styles, highlighter, PlantUML, math
    jcef/                          # JCEF-specific: process links, command runner, mermaid
  fileActions/                     # File-level actions
    export/                        # Export providers (HTML, DOCX, PDF)
    importFrom/docx/               # Import from DOCX
  folding/                         # Code folding builder
  highlighting/                    # Syntax highlighter, annotator, color settings, TODO indexer
  injection/                       # Code fence language injection
    aliases/                       # Language alias resolution
  lang/                            # Language definition
    MarkdownFileType.java          # File type registration (.md, .markdown)
    MarkdownLanguage.java          # Language singleton
    MarkdownCommenter.kt           # Comment support
    formatter/                     # Code formatter, block quote/table post-processors
    index/                         # Stub indexes (HeaderTextIndex, HeaderAnchorIndex)
    lexer/                         # Lexer
    parser/                        # Parser definition, flavour provider
    psi/                           # PSI tree (element types, implementations, manipulators)
    references/                    # Reference resolution (paths, GitHub wiki links, auto-links)
    stubs/                         # Stub element types
  mapper/                          # Source-to-preview mapping
  service/                         # Services
  settings/                        # All settings classes
    MarkdownSettings.kt            # Project-level settings
    MarkdownPreviewSettings.kt     # Preview settings (app-level)
    MarkdownCodeInsightSettings.kt # Code insight settings (app-level)
    MarkdownCodeFoldingSettings.kt # Folding settings (app-level)
    MarkdownExtensionsSettings.kt  # Extension settings (app-level)
    MarkdownSettingsConfigurable.kt # Settings UI
    MarkdownSmartKeysConfigurable.kt # Smart keys settings UI
    pandoc/                        # Pandoc configuration
  structureView/                   # Structure view (document outline)
  ui/
    MarkdownNotifications.kt       # Notification group
    actions/                       # UI actions
      styling/                     # Formatting actions (bold, italic, code, links, headers, lists)
      scrolling/                   # Auto-scroll synchronization
    floating/                      # Floating toolbar
    preview/                       # Live preview system
      MarkdownSplitEditorProvider.kt  # Split editor (text + preview)
      MarkdownPreviewFileEditor.kt    # Preview file editor
      MarkdownHtmlPanel.java          # HTML panel interface
      MarkdownHtmlPanelProvider.java  # Panel provider interface
      PreviewStaticServer.kt          # HTTP server for preview resources
      html/                           # HTML generation
      jcef/                           # JCEF (Chromium) preview implementation
      accessor/                       # Preview content accessors
    projectTree/                   # Project tree customization (rename handler)
  util/                            # Shared utilities
```

---

## Extension Points Defined

The plugin defines 11 custom extension points that other plugins can use:

| Extension Point | Interface | Purpose |
|---|---|---|
| `org.intellij.markdown.html.panel.provider` | `MarkdownHtmlPanelProvider` | Custom HTML preview panels |
| `org.intellij.markdown.fenceLanguageProvider` | `CodeFenceLanguageProvider` | Additional code fence languages |
| `org.intellij.markdown.fenceGeneratingProvider` | `CodeFenceGeneratingProvider` | Custom code fence rendering |
| `org.intellij.markdown.previewStylesProvider` | `MarkdownPreviewStylesProvider` | Custom preview CSS |
| `org.intellij.markdown.browserPreviewExtensionProvider` | `MarkdownBrowserPreviewExtension$Provider` | Browser preview extensions |
| `org.intellij.markdown.markdownExportProvider` | `MarkdownExportProvider` | Export format providers |
| `org.intellij.markdown.markdownRunner` | `MarkdownRunner` | Command runner in preview |
| `org.intellij.markdown.flavourProvider` | `MarkdownFlavourProvider` | Custom Markdown flavours/parsers |
| `org.intellij.markdown.additionalFenceLanguageSuggester` | `AdditionalFenceLanguageSuggester` | Code fence language suggestions |
| `org.intellij.markdown.markdownCompatibilityChecker` | `MarkdownCompatibilityChecker` | File compatibility checking |
| `org.intellij.markdown.actionPromoterExtension` | `MarkdownActionPromoterExtension` | Action promotion customization |

All extension points are marked `dynamic="true"`.

---

## Platform Extensions Registered

### Language Infrastructure
- **fileType:** `MarkdownFileType` (extensions: `markdown`, `md`)
- **lang.parserDefinition:** `MarkdownParserDefinition`
- **lang.ast.factory:** `MarkdownAstFactory`
- **lang.fileViewProviderFactory:** `MarkdownFileViewProviderFactory`
- **syntaxHighlighter:** `MarkdownSyntaxHighlighter`
- **colorSettingsPage:** `MarkdownColorSettingsPage`
- **lang.commenter:** `MarkdownCommenter`
- **lang.formatter:** `MarkdownFormattingModelBuilder`
- **lang.foldingBuilder:** `MarkdownFoldingBuilder`
- **lang.psiStructureViewFactory:** `MarkdownStructureViewFactory`
- **lang.lineWrapStrategy:** `MarkdownLineWrapPositionStrategy`
- **langCodeStyleSettingsProvider:** `MarkdownCodeStyleSettingsProvider`

### Editor Handlers
- **typedHandler:** `MarkdownTypedHandler`, `MarkdownTableTypedHandler`, `HeaderLevelInferenceTypedHandler`, `MarkdownListItemCreatingTypedHandlerDelegate`
- **enterHandlerDelegate:** `MarkdownTableEnterHandler`, `MarkdownEnterHandler`, `MarkdownListEnterHandlerDelegate`
- **backspaceHandlerDelegate:** `MarkdownTableBackspaceHandler`, `MarkdownListMarkerBackspaceHandlerDelegate`, `MarkdownListIndentBackspaceHandlerDelegate`
- **editorActionHandler (Tab):** `MarkdownTableTabHandler$Tab`, `MarkdownListItemTabHandler`
- **editorActionHandler (ShiftTab):** `MarkdownTableTabHandler$ShiftTab`, `MarkdownListItemUnindentHandler`
- **editorActionHandler (EditorStartNewLine):** `MarkdownTableShiftEnterHandler`
- **editorActionHandler (EditorDeleteToWordStart):** `MarkdownTableReformatAfterActionHook`
- **editorActionHandler (EditorIndentSelection):** `MarkdownListItemTabHandler`

### Completion & References
- **completion.contributor:** `MarkdownCompletionContributor`
- **psi.referenceContributor:** `GithubWikiLocalReferenceContributor`, `AutoLinkWebReferenceContributor`
- **pathReferenceProvider:** `RelativeFileWithoutExtensionReferenceProvider`

### Inspections (5 total)
| Short Name | Class | Level |
|---|---|---|
| `MarkdownIncorrectTableFormatting` | `MarkdownIncorrectTableFormattingInspection` | WEAK WARNING |
| `MarkdownNoTableBorders` | `MarkdownNoTableBordersInspection` | WARNING |
| `MarkdownUnresolvedFileReference` | `MarkdownUnresolvedFileReferenceInspection` | WARNING |
| `MarkdownIncorrectlyNumberedListItem` | `IncorrectListNumberingInspection` | (default) |
| `MarkdownOutdatedTableOfContents` | `OutdatedTableOfContentsInspection` | (default) |
| `MarkdownLinkDestinationWithSpaces` | `MarkdownLinkDestinationWithSpacesInspection` | (default) |

### Intentions (6 total)
- `MarkdownInsertTableColumnIntention$InsertBefore`
- `MarkdownInsertTableColumnIntention$InsertAfter`
- `MarkdownSetColumnAlignmentIntention`
- `MarkdownRemoveColumnIntention`
- `MarkdownRemoveRowIntention`
- `MarkdownCodeFenceErrorHighlightingIntention`

### Annotators
- **annotator:** `MarkdownHighlightingAnnotator`

### Editor Providers
- **fileEditorProvider:** `MarkdownSplitEditorProvider` (id: `markdown-preview-editor`)
- **textEditorCustomizer:** `AddFloatingToolbarTextEditorCustomizer`

### Inlay Providers
- **codeInsight.inlayProvider:** `MarkdownTableInlayProvider`

### Line Markers
- **runLineMarkerContributor:** `MarkdownRunLineMarkersProvider` (command runner)
- **codeInsight.lineMarkerProvider:** `PlantUMLCodeFenceDownloadLineMarkerProvider`, `MermaidLineMarkerAdvertisementProvider`

### Other
- **braceMatcher:** `MarkdownBraceMatcher`
- **quoteHandler:** `MarkdownQuoteHandler`
- **breadcrumbsInfoProvider:** `MarkdownBreadcrumbsProvider`
- **treeStructureProvider:** `MarkdownTreeStructureProvider`
- **renameHandler:** `MarkdownFileRenameHandler`
- **automaticRenamerFactory:** `MarkdownRenamerFactory`
- **stripTrailingSpacesFilterFactory:** `MarkdownStripTrailingSpacesFilterFactory`
- **httpRequestHandler:** `PreviewStaticServer`
- **fileDropHandler:** `MarkdownDocxFileDropHandler`
- **focusModeProvider:** `MarkdownFocusModeProvider`
- **extendWordSelectionHandler:** `MarkdownCodeSpanExtendWordSelectionHandler`
- **lineIndentProvider:** `MarkdownListIndentProvider`
- **todoIndexer:** `MarkdownTodoIndexer`
- **indexPatternBuilder:** `MarkdownIndexPatternBuilder`
- **stubIndex:** `HeaderTextIndex`, `HeaderAnchorIndex`
- **stubElementTypeHolder:** `MarkdownStubElementTypes`
- **daemon.highlightInfoFilter:** `CodeFenceHighlightInfoFilter`
- **actionPromoter:** `MarkdownActionPromoter`
- **postFormatProcessor:** `BlockQuotePostFormatProcessor`, `TablePostFormatProcessor`
- **additionalTextAttributes:** Default + Darcula color schemes
- **editorFactoryListener:** `MarkdownInlayUpdateOnSoftWrapListener`
- **codeInsight.fillParagraph:** `MarkdownParagraphFillHandler`
- **codeFoldingOptionsProvider:** `MarkdownCodeFoldingOptionsProvider`

---

## Services

### Application Services
| Service | Purpose |
|---|---|
| `ExtensionsExternalFilesPathManager` | Manages external files for extensions (PlantUML JAR, etc.) |
| `MarkdownCodeFenceHtmlCache` | Caches HTML output for code fences |
| `MarkdownHtmlExportSettings` | HTML export configuration |
| `MarkdownCodeFoldingSettings` | Code folding preferences |
| `MarkdownCodeInsightSettings` | Code insight preferences |
| `MarkdownExtensionsSettings` | Extension on/off settings |

### Application Settings (persisted)
- `MarkdownCodeInsightSettings`
- `MarkdownPreviewSettings`
- `MarkdownExtensionsSettings`

### Project Settings (persisted)
- `MarkdownSettings`

### Settings UI
- **projectConfigurable:** `MarkdownSettingsConfigurable` (under `language` group)
- **editorSmartKeysConfigurable:** `MarkdownSmartKeysConfigurable`

---

## Listeners

### Application Listeners
| Topic | Listener | Purpose |
|---|---|---|
| `LafManagerListener` | `SettingsChangeLafListener` | React to theme changes |
| `LafManagerListener` | `HtmlCacheManager$InvalidateHtmlCacheLafListener` | Invalidate HTML cache on theme change |

### Project Listeners
| Topic | Listener | Purpose |
|---|---|---|
| `MarkdownSettings$ChangeListener` | `CodeAnalyzerRestartListener` | Restart analysis when settings change |

---

## Actions and Keybindings

### Keybindings
| Action | Shortcut | Class |
|---|---|---|
| Toggle Bold | `Ctrl+B` | `ToggleBoldAction` |
| Toggle Italic | `Ctrl+I` | `ToggleItalicAction` |
| Toggle Strikethrough | `Ctrl+Shift+S` | `ToggleStrikethroughAction` |
| Toggle Code Span | `Ctrl+Shift+C` | `ToggleCodeSpanAction` |
| Create Link | `Ctrl+Shift+U` | `MarkdownCreateLinkAction` |
| Insert (Generate menu) | uses `Generate` shortcut | `InsertAction` |
| Preview Find | uses `Find` shortcut | `FindInPreviewAction` |
| Preview Increase Font | uses `ExpandAll` shortcut | `ChangeFontSizeAction$Increase` |
| Preview Decrease Font | uses `CollapseAll` shortcut | `ChangeFontSizeAction$Decrease` |

### Action Groups
- **`Markdown.Toolbar.Floating`** - Floating toolbar: Set Header Level, Bold, Italic, Strikethrough, Code Span, Create Link, Create/Change List
- **`Markdown.TableActions`** - Parent group for table operations
  - **`Markdown.TableColumnActions`** - Swap, insert, select, align, remove columns
  - **`Markdown.TableRowActions`** - Swap, insert, select, remove rows
  - **`Markdown.TableContextMenuGroup`** - Context menu for tables
- **`Markdown.InsertGroup`** - Added to `GenerateGroup`: Create Link, Insert Empty Table, Generate Table of Contents
- **`Markdown.EditorContextMenuGroup`** - Added to `EditorPopupMenu`: Table context menu, Insert action
- **`Markdown.Toolbar.Left/Right`** - Editor toolbar groups (Right has AutoScrollAction)
- **`Markdown.Tools`** - Added to `ToolsMenu`: Import from DOCX, Export, Configure Pandoc
- **`Markdown.PreviewGroup`** - Adjust Font Size, Find in Preview

### Standalone Actions
- `HeaderDownAction` / `HeaderUpAction` - Cycle header level
- `SetHeaderLevelAction` - Set specific header level
- `MarkdownIntroduceLinkReferenceAction` - Extract link reference
- `MarkdownOpenDevtoolsAction` - Open preview devtools
- `CleanupExtensionsExternalFilesAction` - Clean extension files
- `ResetFontSizeAction` - Reset preview font size
- `AutoScrollAction` - Toggle scroll sync

---

## Internal Extension Point Implementations

The plugin registers implementations of its own extension points:

- **html.panel.provider:** `JCEFHtmlPanelProvider`
- **fenceLanguageProvider:** `PlantUMLCodeFenceLanguageProvider`, `MermaidCodeFenceLanguageProvider`
- **fenceGeneratingProvider:** `PlantUMLCodeGeneratingProvider`, `MarkdownCodeFencePreviewHighlighter`
- **browserPreviewExtensionProvider:** `BaseStylesExtension`, `InlineStylesExtension`, `ProcessLinksExtension`, `CommandRunnerExtension`, `CodeFenceCopyButtonBrowserExtension`, `MathExtension`, `ProcessImagesExtension` (from images module)
- **markdownExportProvider:** `MarkdownDocxExportProvider`, `MarkdownPdfExportProvider`, `MarkdownHtmlExportProvider`
- **markdownCompatibilityChecker:** `DefaultMarkdownCompatibilityChecker`

---

## Registry Keys (Feature Flags)

| Key | Default | Purpose |
|---|---|---|
| `markdown.clear.cache.interval` | 600000 | Cache clear interval (ms) |
| `markdown.plantuml.download.link` | URL | PlantUML JAR download URL |
| `markdown.open.link.in.external.browser` | true | Open links externally vs IDE viewer |
| `markdown.formatter.apply.to.code.fence` | false | Format code inside fences |
| `markdown.structure.view.list.visibility` | false | Show lists in structure view |
| `markdown.export.html.enforce.csp` | true | CSP in exported HTML |
| `markdown.experimental.boundary.precise.scroll.enable` | true | Precise scroll sync |
| `markdown.experimental.header.level.inference.enable` | false | Auto-infer header level |
| `markdown.validate.short.links` | false | Validate short-style links |
| `markdown.experimental.show.frontmatter.in.preview` | false | Show front matter in preview |
| `markdown.experimental.allow.external.requests` | true | Allow external resource loading |

### Advanced Settings
- `markdown.hide.floating.toolbar` (default: false)
- `markdown.squash.multiple.dashes.in.header.anchors` (default: false)

---

## Key Architectural Patterns

1. **Modular JAR packaging:** The plugin uses `plugin-content.yaml` to define which modules go into which JARs, with lazy-loaded modules for fence injection, frontend, and backend.

2. **Extension point-driven preview:** The preview system is highly extensible - styles, link processing, image processing, code highlighting, math, Mermaid, PlantUML, and command running are all separate `browserPreviewExtensionProvider` implementations.

3. **JCEF-based preview:** The live preview uses Chromium Embedded Framework (JCEF) via `JCEFHtmlPanelProvider`, with a built-in HTTP server (`PreviewStaticServer`) serving resources.

4. **Split editor pattern:** `MarkdownSplitEditorProvider` creates a combined editor with text on one side and preview on the other, using `MarkdownEditorWithPreview`.

5. **Table editing via inlays:** Table column operations use `MarkdownTableInlayProvider` to render column controls as editor inlays, with dedicated handlers for typed, backspace, enter, and tab keys.

6. **List editing subsystem:** Lists have their own enter handler, backspace handlers (marker + indent), tab/untab handlers, and a line indent provider.

7. **Flavour/parser extensibility:** The `flavourProvider` extension point allows replacing the Markdown parser (e.g., GFM, CommonMark variants).

8. **No external preview dependencies in core:** PlantUML and Mermaid support are optional, with download-on-demand for PlantUML JAR and a separate plugin suggestion for Mermaid.

---

## Implications for Our Plugin

Since our plugin (`com.tribus.markdown-all-in-one`) is standalone and must NOT depend on the bundled Markdown plugin:

1. **Language conflict:** The bundled plugin registers `Markdown` as a language with file types `.md` and `.markdown`. We must either:
   - Target a different file type / language ID, OR
   - Require users to disable the bundled plugin, OR
   - Register our extensions for the same `Markdown` language (piggyback)

2. **Extension points we could implement:** If we choose to coexist, we can register implementations for the bundled plugin's extension points (e.g., custom `html.panel.provider`, `browserPreviewExtensionProvider`).

3. **Keybinding conflicts:** The bundled plugin uses `Ctrl+B` (Bold), `Ctrl+I` (Italic), `Ctrl+Shift+S` (Strikethrough), `Ctrl+Shift+C` (Code), `Ctrl+Shift+U` (Link). We should either match these or use different bindings.

4. **Feature parity targets:** The bundled plugin provides table editing (with inlays), list handling, TOC generation, export (HTML/DOCX/PDF), code fence injection, floating toolbar, and split preview. These are the features users expect.
