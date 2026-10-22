# Architecture

## Current state — not yet migrated

One Android application module, namespace `com.example`. The inventory covers 66 production Kotlin files.
A full method-level audit remains necessary before refactoring. No application classes or resources
were changed at the preparation checkpoint.

Current opening flow:

1. `MainActivity` handles Android entry points and navigation.
2. `MainViewModel` handles intake, URI copies, recents, preferences, loading and saving.
3. `FormatDetector` obtains a `Detection` backed by `FormatRegistry` metadata.
4. The ViewModel switches on `FileKind`, invokes `FileManager` or parsers, and creates `FileContentState`.
5. `FilePreviewScreen` switches on state and owns many format composables and WebView bridges.
6. `UniversalViewers` implements SQLite, fonts, 3D, SVG and generated HTML views.
7. `FilePrintHelper` combines platform print adapters with format-specific preparation.

`FormatRegistry` currently registers metadata, **not document handlers**. There is no common
open/close handler contract yet. `FileContentState` is still in the ViewModel source.

Shared foundations include `AtomicFileWriter`, `TextIO`, `ArchiveSafety`, `PathGuard`,
Room DAOs/repository, cache helpers, and search utilities. Keep their safety guarantees intact.
PDF.js, DOCX/editor scripts, spreadsheet assets, fonts and licenses remain at existing asset URLs.

## Planned ownership and dependency direction

```
com.example/
  app/                composition root, navigation, lifecycle coordination
  core/               contracts, detection, I/O, security, cache, shared search
  data/               persistence, storage and repositories
  domain/             preview/edit/search use cases
  feature/            browser, home, preview chrome, editor, settings, file info
  formats/<family>/   existing parser, renderer, search/edit/save/print preparation
  ui/                 design tokens, reusable components, adaptive behavior
  platform/           Android permissions, URI/document, print/share adapters
```

These are **proposed** packages; do not create empty implementations. See the migration map for
actual current paths. Keep the application ID and manifest entry point identities unchanged.
Keep Room schema identity unchanged unless a separately tested migration requires otherwise.

Composition root wires format implementations into the existing registry. Core/domain depend
on contracts, not renderers. Formats consume shared services; they must not depend on application
ViewModels or unrelated formats. Shared OLE2 handling remains reusable for DOC and XLS. CSV/TSV
share delimited parsing; XLSX variants share a workbook engine; XLS is a separate engine.

## Contract design checkpoint (not implemented)

Refine existing Detection/FormatSpec/FileContentState rather than adding a competing registry.
A handler should accept source metadata and a cancellable opening context; return a session with
preview state, errors, capabilities and read-only reasons; and close owned resources idempotently.
Optional search/edit/save/print/export operations must be capability-gated. Metadata support labels
are not capabilities. Cancellation must propagate; resource closure belongs to the session/lifecycle,
not an unbounded global cache. Android preview composition belongs outside pure Kotlin contracts.

Before selecting signatures, audit all state consumers and ownership of file descriptors, WebViews,
media players and archive streams. Add routing and session cleanup tests before integrating handlers.

## Safe extraction order

1. Establish a green baseline and complete the implementation audit.
2. Move pure format helpers and their tests one family at a time.
3. Extract UniversalViewers and mixed document support helpers.
4. Introduce handlers via the existing registry, then delegate loading from the ViewModel.
5. Extract preview composables/bridges, save/search/print responsibilities incrementally.
6. Reduce activity/navigation responsibilities and audit remaining feature packages.
7. Run full regression/device gates and only then mark migration complete.

Every step requires compilation, relevant tests and UI/state verification before proceeding.
