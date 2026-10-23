# Roadmap

Full support matrix: `FILE_FORMATS.md` (generated from the code).

## Milestones
| Version | Theme | Status |
|---|---|---|
| 1.1.0 | Foundation & safety: tested core, universal detection, atomic writes, archive hardening, 13 new format families | ✅ code complete, **awaiting first real build** |
| 1.2.0 | Verified build + device pass; write-back to source URI; paged huge-text viewer; legacy `.xls`; editor fixes | ⏳ next |
| 1.3.0 | Root glass UI rollout; split `FilePreviewScreen`; typed navigation; Compose UI tests | ⏳ |
| 1.4.0 | Legacy `.doc`, RAR, GLB/PLY/DXF, CJK charsets | ⏳ |
| 2.0.0 | Streaming renderers (no base64 hand-off), Macrobenchmark-gated performance budget | ⏳ |

## Current preview work (Loop 6, pending Android verification)
- Scope: PDF, DOCX, TXT, and Markdown only. The working branch adds offline PDF.js reader/search/print, DOCX search/theme/zoom/print improvements, TXT/Markdown fenced-code copy, whole-file text search with bounded excerpts, and print/export wiring for all four formats.
- Static JavaScript and whitespace checks passed; Android compilation, unit tests, emulator/device checks, and actual print/export were not available in the authoring sandbox. See `TODO.md` → KNOWN ISSUES and the Unreleased changelog entry.

## Proposed next loop (Loop 7)
- Keep the same four-format scope. Priorities: true paged/virtualized TXT/Markdown reading beyond the initial window; Markdown syntax context across page boundaries; navigation beyond the 10,000 retained large-file search offsets; DOCX search across styled text runs; and device verification of PDF/DOCX/TXT/Markdown printing, dark mode, zoom, and immersive mode.
- Prompt: `docs/prompts/LOOP_07_PROMPT.md`.

## Format highlights
| Area | ✅ done | 🔄 partial | ⏳ planned |
|---|---|---|---|
| Documents | PDF, DOCX, PPTX, RTF, EPUB, Markdown, HTML, text/code | OpenDocument (text only) | `.doc`, `.ppt`, `.xls`, `.pages` |
| Data | CSV/TSV/PSV, XLSX (view), SQLite, JSON/XML/YAML | | Parquet, Avro, `.xlsb` |
| Archives | zip, 7z, tar(.gz/.bz2/.xz), gz/bz2/xz, ar/deb, cpio, apk/jar/… | | RAR, ISO, CAB, zstd |
| Media | image (png/jpg/gif/webp/bmp/ico), audio, video | HEIC/AVIF/TIFF/DNG, avi/mov (device codecs) | PSD preview, subtitles overlay |
| Fonts / 3D | TTF/OTF, STL, OBJ | | WOFF, GLB, PLY, DXF, 3MF, DWG |

## Performance budget (to be enforced once measurable on a device)
Cold start < 1.5 s · 60 fps scrolling · open 500 MB without OOM (core already supports it) · crash-free sessions ≥ 99.9 %.
