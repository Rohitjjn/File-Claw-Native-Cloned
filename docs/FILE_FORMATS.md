# Supported file formats

The source of truth is `app/src/main/java/com/example/core/format/FormatRegistry.kt`. This inventory lists registered format IDs, names, route categories, metadata support labels, MIME types, and extension aliases. It is an inventory, **not** proof that every real-world file works or that a format is editable.

`Full` means the current metadata marks the format as fully handled; it does not guarantee complete compatibility with every variant. `Partial` covers best-effort text extraction and/or support that depends on the Android device. `Identify only` means the app recognizes and labels the type but does not promise a full viewer. Check a family handler before claiming a specific operation. Detection can use file signatures and text heuristics, so the extension alone is not always decisive.

## Source-observed family limits

| Family | Current source behavior and limits |
|---|---|
| Plain text, code, Markdown, HTML | `TextFormatHandler` loads up to 250,000 characters for preview; a truncated view is flagged and cannot be safely saved as the whole original. HTML scripts start disabled. |
| CSV / TSV / PSV | `DelimitedTableLoader` reads at most 50,000 rows and 256 columns; it retains encoding/BOM/line-ending metadata for its existing save path. |
| XLSX and CSV/TSV | Shared bounded virtual grid. Workbook view is first-sheet, values-only: 20,000 rows, 100 columns, 4,000 characters/cell, 8,000,000 total characters. Light cell-value editing is offered only for a complete, single-sheet, non-macro, non-template XLSX workbook. |
| DOCX / PPTX | Existing local WebView renderers, now preceded by a shared OOXML package inspector and started only after the real WebView bounds are known. No arbitrary 25 MiB archive fallback; packages are bounded by compressed/expanded size, entry count, ratio and XML safety. Both are read-only previews. |
| PDF | Existing bundled PDF.js WebView reader with password/error callbacks, range serving, bounded canvas policy and a three-page resident render window. |
| RTF | Text extraction into a read-only text state; loader limit is 20,000,000 characters. Formatting fidelity is not promised. |
| OpenDocument | `content.xml` text extraction only; XML input is limited to 64 MiB and extracted output to 5,000,000 characters. |
| EPUB | Spine-order text/HTML preview through sanitization; per-chapter input is limited to 16 MiB and generated preview to about 3,000,000 characters. Images are not retained. |
| Archives and compressed streams | Listing caps the tree at 200,000 entries and flags unsafe paths/deep nesting (configured depth 64). Extraction caps one expanded entry at 2 GiB; the 2,000:1 ratio check above 4 MiB applies only when ZIP metadata provides compressed size. A declared 16 GiB aggregate limit is not enforced. RAR is identify-only. |
| Legacy Office | `.doc` and `.ppt` use bounded Unicode text recovery; `.xls` uses read-only first-sheet BIFF cell extraction. Encrypted packages and `.xlsb` are not decoded; other OLE2 variants use the identify-only hex view. |
| STL / OBJ | Existing parser accepts STL and OBJ with a 300,000-triangle input cap; other 3D/CAD metadata groups are identify-only. |
| Images, audio, video, fonts, SQLite | Existing family viewers use Android/device decoding or family-specific readers. Codec/decoder compatibility varies by device; inspect the family source before promising a particular variant. |
| Other binary types | Bounded hex/ASCII identification view; this does not decode the format. |

## Microsoft Office support matrix

The registry keeps one route per format family (for example `docx` covers several
OOXML Word variants). The matrix below is the verified behavior of the family
handlers, not an extension-only promise. “Read-only” means no in-app editing is
offered; the original file is never rewritten by a preview.

| Format family | Verified in-app behavior | Edit / save | Print / export | Important limitations |
|---|---|---|---|---|
| `.docx` | Formatted WebView preview with page tracking, search, zoom/pan, dark mode and text recovery on renderer failure. | None. | Dedicated print renderer / PDF export. | No full Word layout engine; unsupported fields, complex equations, ActiveX and remote content may be omitted. |
| `.docm`, `.dotx`, `.dotm` | Same read-only Word preview when the package is valid. A banner states macros/templates are not executed. | None. | Same as DOCX. | Macros, add-ins and template instantiation are unsupported. |
| `.xlsx` | Bounded first-sheet values grid with search, zoom, copy, safe cell edit prompt and dark mode. | Values-only light edit for a complete, single-sheet, non-macro workbook. Atomic replace; source URI/archive sync uses the existing ViewModel path. | Bounded HTML table print / PDF export. | Formulas are shown as cached/text values; styles, merged cells, charts, images, comments and additional sheets are not preserved by a save. Multi-sheet/truncated/macro/template workbooks are explicitly read-only. |
| `.xlsm`, `.xltx`, `.xltm` | Same read-only values grid. | Disabled: a values-only save would destroy macros/template metadata. | Same as XLSX. | Macros are never executed. |
| `.pptx` | Formatted slide preview with previous/next/jump navigation, cross-slide search, zoom/pan, dark mode, images, tables and text-only fallback. | None. | Dedicated all-slide print renderer / PDF export. | Animations, transitions, embedded video/audio, fonts not on the device and some SmartArt may be approximated or omitted. |
| `.pptm`, `.ppsx`, `.potx`, `.potm` | Same read-only presentation preview when the package is valid. | None. | Same as PPTX. | Macros, show settings and template instantiation are unsupported. |
| `.doc` | Bounded Unicode text recovery in the existing text viewer. | None. | Prints recovered text. | No binary formatting, tables, images or page layout. |
| `.xls` | Bounded first-sheet BIFF cell extraction in the grid. | None. | Bounded HTML table print. | No formulas evaluation, styling, charts or extra sheets. |
| `.ppt` | Bounded Unicode slide-text recovery in the existing text viewer. | None. | Prints recovered text. | No slide layout, images, tables, charts, animations or media. |
| `.xlsb`, other OLE2 Office | Identify-only hex/ASCII view. | None. | No meaningful document print. | Convert to OOXML for a preview. |
| Encrypted/password-protected Office | Explicit unsupported error state. | None. | None. | Remove the password outside the app and reopen. |

Content detection wins over a misleading extension, but the family handler
validates the package's actual main part before rendering. A Word package named
`.xlsx`, for example, is rejected with a mismatch error instead of being passed
to a spreadsheet parser. External relationships are counted and blocked from
network/file access; macros and document scripts are never executed.

## Registered format inventory

<!-- Inventory parsed from app/src/main/java/com/example/core/format/FormatRegistry.kt. Companion generator: tools/GenFormatDocs.kt; it is not wired to a Gradle task. -->

**399 registered extensions across 69 format groups:** 284 Full, 33 Partial, 82 Identify only.

| ID | Format name | File kind | Metadata support | MIME type | Extensions |
|---|---|---|---|---|---|
| `text` | Plain text | Text | Full | `text/plain` | `txt`, `text`, `log`, `nfo`, `diz`, `asc`, `srt`, `vtt`, `sub`, `lst`, `out` |
| `json` | JSON | Text | Full | `application/json` | `json`, `jsonl`, `ndjson`, `geojson`, `jsonc`, `json5`, `ipynb`, `webmanifest`, `har` |
| `xml` | XML | Text | Full | `application/xml` | `xml`, `xsl`, `xslt`, `xsd`, `wsdl`, `plist`, `gpx`, `kml`, `rss`, `atom`, `opml`, `dae`, `resx`, `csproj`, `vcxproj`, `pom`, `iml`, `fxml`, `storyboard`, `xaml` |
| `yaml` | YAML | Text | Full | `application/yaml` | `yaml`, `yml` |
| `config` | Config / INI / TOML | Text | Full | `text/plain` | `toml`, `ini`, `cfg`, `conf`, `properties`, `env`, `editorconfig`, `gitignore`, `gitattributes`, `npmrc`, `reg`, `desktop`, `service`, `lock` |
| `code-jvm` | JVM source | Text | Full | `text/x-source` | `kt`, `kts`, `java`, `scala`, `groovy`, `gradle`, `clj`, `cljs` |
| `code-web` | Web source | Text | Full | `text/x-source` | `js`, `mjs`, `cjs`, `ts`, `tsx`, `jsx`, `css`, `scss`, `sass`, `less`, `vue`, `svelte`, `php`, `graphql`, `gql`, `wxss`, `wxml` |
| `code-native` | Native source | Text | Full | `text/x-source` | `c`, `h`, `cpp`, `cc`, `cxx`, `hpp`, `hh`, `cs`, `go`, `rs`, `swift`, `m`, `mm`, `zig`, `d`, `nim`, `asm`, `s`, `f90`, `for`, `pas`, `vb`, `cr` |
| `code-script` | Script source | Text | Full | `text/x-source` | `py`, `rb`, `pl`, `pm`, `lua`, `sh`, `bash`, `zsh`, `fish`, `bat`, `cmd`, `ps1`, `r`, `jl`, `dart`, `ex`, `exs`, `erl`, `hs`, `ml`, `fs`, `lisp`, `el`, `vim`, `tcl`, `awk`, `sed`, `cmake`, `mk` |
| `code-data` | Query / schema / infra | Text | Full | `text/x-source` | `sql`, `proto`, `tf`, `hcl`, `nix`, `sol`, `v`, `sv`, `vhd`, `vhdl`, `thrift`, `avsc`, `prisma` |
| `markup-text` | Markup / docs source | Text | Full | `text/plain` | `tex`, `bib`, `rst`, `adoc`, `org`, `diff`, `patch`, `pem`, `crt`, `cer`, `csr`, `pub`, `ics`, `vcf`, `eml`, `m3u`, `m3u8`, `pls`, `cue`, `ass`, `ssa` |
| `markdown` | Markdown | Markdown | Full | `text/markdown` | `md`, `markdown`, `mdown`, `mkd`, `mdx`, `rmd` |
| `html` | HTML | Html | Full | `text/html` | `html`, `htm`, `xhtml` |
| `svg` | SVG image | Svg | Full | `image/svg+xml` | `svg` |
| `csv` | CSV / delimited text | Delimited | Full | `text/csv` | `csv`, `tsv`, `tab`, `psv` |
| `xlsx` | Excel workbook (OOXML) | Spreadsheet | Full | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | `xlsx`, `xlsm`, `xltx`, `xltm` |
| `docx` | Word document (OOXML) | Docx | Full | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | `docx`, `docm`, `dotx`, `dotm` |
| `pptx` | PowerPoint (OOXML) | Pptx | Full | `application/vnd.openxmlformats-officedocument.presentationml.presentation` | `pptx`, `pptm`, `ppsx`, `potx`, `potm` |
| `pdf` | PDF document | Pdf | Full | `application/pdf` | `pdf` |
| `rtf` | Rich Text Format | Rtf | Full | `application/rtf` | `rtf` |
| `odt` | OpenDocument text | Odf | Partial | `application/vnd.oasis.opendocument.text` | `odt`, `ott`, `fodt` |
| `ods` | OpenDocument spreadsheet | Odf | Partial | `application/vnd.oasis.opendocument.spreadsheet` | `ods`, `ots` |
| `odp` | OpenDocument presentation | Odf | Partial | `application/vnd.oasis.opendocument.presentation` | `odp`, `otp` |
| `epub` | EPUB e-book | Epub | Full | `application/epub+zip` | `epub` |
| `ole2` | Legacy Microsoft Office (OLE2) | Legacy office | Partial | `application/x-ole-storage` | `doc`, `xls`, `ppt`, `msg`, `vsd`, `xlsb`, `wps`, `mpp` |
| `png` | PNG image | Image | Full | `image/png` | `png`, `apng` |
| `jpeg` | JPEG image | Image | Full | `image/jpeg` | `jpg`, `jpeg`, `jfif`, `jpe` |
| `gif` | GIF image | Image | Full | `image/gif` | `gif` |
| `webp` | WebP image | Image | Full | `image/webp` | `webp` |
| `bmp` | Bitmap image | Image | Full | `image/bmp` | `bmp`, `dib` |
| `ico` | Icon | Image | Full | `image/x-icon` | `ico`, `cur` |
| `heif` | HEIC / HEIF / AVIF image | Image | Partial | `image/heif` | `heic`, `heif`, `avif` |
| `tiff` | TIFF image | Image | Partial | `image/tiff` | `tif`, `tiff` |
| `raw` | Camera RAW (DNG) | Image | Partial | `image/x-adobe-dng` | `dng` |
| `psd` | Photoshop / layered image | Binary | Identify only | `image/vnd.adobe.photoshop` | `psd`, `xcf`, `ai`, `eps`, `ps`, `kra`, `sketch`, `fig` |
| `mp3` | MP3 audio | Audio | Full | `audio/mpeg` | `mp3`, `mp2` |
| `wav` | WAV audio | Audio | Full | `audio/wav` | `wav`, `wave` |
| `ogg` | Ogg audio | Audio | Full | `audio/ogg` | `ogg`, `oga`, `opus`, `spx` |
| `m4a` | AAC / M4A audio | Audio | Full | `audio/mp4` | `m4a`, `aac`, `m4b`, `3ga` |
| `flac` | FLAC audio | Audio | Full | `audio/flac` | `flac` |
| `midi` | MIDI | Audio | Full | `audio/midi` | `mid`, `midi`, `kar` |
| `audio-other` | Other audio | Audio | Partial | `audio/x-other` | `amr`, `awb`, `wma`, `mka`, `aiff`, `aif`, `ape`, `wv` |
| `mp4` | MP4 video | Video | Full | `video/mp4` | `mp4`, `m4v` |
| `mkv` | Matroska video | Video | Full | `video/x-matroska` | `mkv` |
| `webm` | WebM video | Video | Full | `video/webm` | `webm` |
| `3gp` | 3GP video | Video | Full | `video/3gpp` | `3gp`, `3g2` |
| `video-other` | Other video | Video | Partial | `video/x-other` | `avi`, `mov`, `mpg`, `mpeg`, `flv`, `wmv`, `asf`, `ogv`, `mts`, `m2ts`, `vob` |
| `zip` | ZIP archive | Archive | Full | `application/zip` | `zip`, `zipx` |
| `zip-app` | ZIP-based package | Archive | Full | `application/zip` | `jar`, `war`, `ear`, `aar`, `apk`, `xapk`, `apks`, `xpi`, `crx`, `whl`, `egg`, `nupkg`, `vsix`, `ipa`, `3mf`, `kmz`, `mrpack`, `sb3`, `love`, `pk3` |
| `7z` | 7-Zip archive | Archive | Full | `application/x-7z-compressed` | `7z` |
| `tar` | TAR archive | Archive | Full | `application/x-tar` | `tar` |
| `tgz` | TAR + gzip | Archive | Full | `application/gzip` | `tgz`, `tar.gz` |
| `tbz2` | TAR + bzip2 | Archive | Full | `application/x-bzip2` | `tbz2`, `tbz`, `tar.bz2` |
| `txz` | TAR + xz | Archive | Full | `application/x-xz` | `txz`, `tar.xz` |
| `ar` | ar / Debian package | Archive | Full | `application/x-archive` | `ar`, `deb`, `a`, `lib` |
| `cpio` | cpio archive | Archive | Full | `application/x-cpio` | `cpio` |
| `gz` | gzip file | Compressed stream | Full | `application/gzip` | `gz` |
| `bz2` | bzip2 file | Compressed stream | Full | `application/x-bzip2` | `bz2` |
| `xz` | xz file | Compressed stream | Full | `application/x-xz` | `xz`, `lzma` |
| `rar` | RAR archive | Archive | Identify only | `application/vnd.rar` | `rar`, `r00` |
| `archive-other` | Other archive / disk image | Binary | Identify only | `application/octet-stream` | `iso`, `cab`, `dmg`, `img`, `wim`, `zst`, `lz4`, `lz`, `z`, `arj`, `lzh`, `ace`, `rpm`, `vhdx`, `vmdk`, `qcow2` |
| `sqlite` | SQLite database | Sqlite | Full | `application/vnd.sqlite3` | `sqlite`, `sqlite3`, `db`, `db3`, `sdb`, `s3db`, `sqlitedb` |
| `ttf` | TrueType / OpenType font | Font | Full | `font/ttf` | `ttf`, `otf`, `ttc`, `otc` |
| `woff` | Web font (WOFF) | Font | Identify only | `font/woff` | `woff`, `woff2`, `eot` |
| `stl` | STL 3D model | Model 3d | Full | `model/stl` | `stl` |
| `obj` | Wavefront OBJ | Model 3d | Full | `model/obj` | `obj` |
| `model-other` | Other 3D / CAD | Model 3d | Identify only | `model/x-other` | `ply`, `glb`, `gltf`, `fbx`, `blend`, `dwg`, `dxf`, `step`, `stp`, `iges`, `igs`, `skp`, `3ds`, `usdz` |
| `exe` | Executable / library | Binary | Identify only | `application/octet-stream` | `exe`, `dll`, `so`, `dylib`, `elf`, `o`, `class`, `dex`, `wasm`, `pyc`, `msi`, `sys`, `ko` |
| `data` | Binary data | Binary | Identify only | `application/octet-stream` | `bin`, `dat`, `parquet`, `avro`, `orc`, `pkl`, `npy`, `h5`, `hdf5`, `mat`, `torrent`, `lnk`, `pak`, `tflite`, `onnx`, `pb`, `ckpt`, `safetensors`, `gguf` |
