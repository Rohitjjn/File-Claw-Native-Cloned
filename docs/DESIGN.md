# UI design source map

This page records the design tokens in the current app source. It is not a mandate to redesign screens as part of a bug fix, and it makes no claim that contrast, animation timing, or frame rate has been measured on a device.

## Theme and colors

The app theme is in `app/src/main/java/com/example/ui/theme/`:

| Source token | Value | Use indicated in source |
|---|---|---|
| `ClaudeTerracotta` | `#CC785C` | Main light/dark primary accent |
| `ClaudeSoftTomato` | `#A9583E` | Light secondary accent |
| `ClaudeAlabaster` | `#FAF9F5` | Light canvas/background |
| `ClaudeCharcoal` | `#141413` | Light foreground text |
| `ClaudeMutedText` | `#6C6A64` | Muted light text |
| `ClaudeSandBorder` | `#E6DFD8` | Light outline |
| `ClaudeOnyx` | `#181715` | Dark background |
| `ClaudeDarkSurface` | `#252320` | Dark elevated surface |
| `ClaudeDarkBorder` | `#2D2C2A` | Dark outline |
| `ClaudeDarkText` | `#FAF9F5` | Dark foreground text |
| `ClaudeOrangeAccent` | `#E8A55A` | Dark secondary accent |

The same source defines additional selected/background colors. `FilesClawTheme` selects Light, Dark, or System color schemes and synchronizes status/navigation bar icon contrast with the in-app theme.

## Glass surfaces

`Glass.kt` builds translucent surfaces from fills, a top sheen, a one-dp gradient border, and three ambient radial gradients. The tokens are `LightGlass` and `DarkGlass`; `Modifier.glass()` and `GlassBackdrop()` are shared implementation points. The backdrop uses gradients rather than a blur API.

## Ownership and safety

- Shared UI components belong in `ui/component/` and shared theme code in `ui/theme/`.
- Reading surfaces should remain legible; do not make a PDF/DOCX canvas transparent or change a viewer's behavior as a side effect of a format refactor.
- PDF is intentionally light-only in the current PDF viewer. DOCX and other supported text/document surfaces have their existing theme behavior; preserve it unless the user asks otherwise.
- Prefer using current tokens and components for a requested UI fix. This preview-repair task does not authorize unrelated visual changes.

No on-device contrast, animation, or performance measurement was recorded during this pass. The source values above should be checked against actual UI before making product claims.
