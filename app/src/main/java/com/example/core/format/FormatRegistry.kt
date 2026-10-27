package com.example.core.format

/**
 * Routing category: which viewer family should handle the file.
 * Pure Kotlin (no Android imports) so it can be unit-tested on the JVM.
 */
enum class FileKind {
    TEXT, MARKDOWN, HTML, SVG, DELIMITED, SPREADSHEET, DOCX, PPTX, PDF,
    IMAGE, AUDIO, VIDEO, ARCHIVE, COMPRESSED_STREAM, SQLITE, FONT, MODEL_3D,
    RTF, ODF, EPUB, LEGACY_OFFICE, BINARY
}

/** How well the app can open a format today. */
enum class Support {
    /** A dedicated in-app viewer exists. */
    FULL,

    /** Best effort: extracted text only, or depends on device codecs. */
    PARTIAL,

    /** Recognised and labelled, but only the hex viewer is available. */
    IDENTIFY
}

data class FormatSpec(
    val id: String,
    val displayName: String,
    val kind: FileKind,
    val support: Support,
    val mime: String,
    val extensions: List<String>
)

/** Minimal routing identity shared with the app's single format-family handler contract. */
interface FormatRouteHandler {
    val formatIds: Set<String>
}

object FormatRegistry {

    private val specs = ArrayList<FormatSpec>()
    private val byId = HashMap<String, FormatSpec>()
    private val byExt = HashMap<String, FormatSpec>()

    /** Runtime family handlers are installed once by the application bootstrap. */
    @Volatile
    private var handlersById: Map<String, FormatRouteHandler> = emptyMap()

    private fun spec(
        id: String,
        name: String,
        kind: FileKind,
        support: Support,
        mime: String,
        vararg ext: String
    ) {
        val s = FormatSpec(id, name, kind, support, mime, ext.map { it.lowercase() })
        require(byId.put(id, s) == null) { "duplicate format id: $id" }
        specs.add(s)
        for (e in s.extensions) {
            require(byExt.put(e, s) == null) { "duplicate extension: $e" }
        }
    }

    init {
        val full = Support.FULL
        val part = Support.PARTIAL
        val ident = Support.IDENTIFY

        // ---- Text, code and data-as-text -------------------------------------------------
        spec("text", "Plain text", FileKind.TEXT, full, "text/plain", "txt", "text", "log", "nfo", "diz", "asc", "srt", "vtt", "sub", "lst", "out")
        spec("json", "JSON", FileKind.TEXT, full, "application/json", "json", "jsonl", "ndjson", "geojson", "jsonc", "json5", "ipynb", "webmanifest", "har")
        spec("xml", "XML", FileKind.TEXT, full, "application/xml", "xml", "xsl", "xslt", "xsd", "wsdl", "plist", "gpx", "kml", "rss", "atom", "opml", "dae", "resx", "csproj", "vcxproj", "pom", "iml", "fxml", "storyboard", "xaml")
        spec("yaml", "YAML", FileKind.TEXT, full, "application/yaml", "yaml", "yml")
        spec("config", "Config / INI / TOML", FileKind.TEXT, full, "text/plain", "toml", "ini", "cfg", "conf", "properties", "env", "editorconfig", "gitignore", "gitattributes", "npmrc", "reg", "desktop", "service", "lock")
        spec("code-jvm", "JVM source", FileKind.TEXT, full, "text/x-source", "kt", "kts", "java", "scala", "groovy", "gradle", "clj", "cljs")
        spec("code-web", "Web source", FileKind.TEXT, full, "text/x-source", "js", "mjs", "cjs", "ts", "tsx", "jsx", "css", "scss", "sass", "less", "vue", "svelte", "php", "graphql", "gql", "wxss", "wxml")
        spec("code-native", "Native source", FileKind.TEXT, full, "text/x-source", "c", "h", "cpp", "cc", "cxx", "hpp", "hh", "cs", "go", "rs", "swift", "m", "mm", "zig", "d", "nim", "asm", "s", "f90", "for", "pas", "vb", "cr")
        spec("code-script", "Script source", FileKind.TEXT, full, "text/x-source", "py", "rb", "pl", "pm", "lua", "sh", "bash", "zsh", "fish", "bat", "cmd", "ps1", "r", "jl", "dart", "ex", "exs", "erl", "hs", "ml", "fs", "lisp", "el", "vim", "tcl", "awk", "sed", "cmake", "mk")
        spec("code-data", "Query / schema / infra", FileKind.TEXT, full, "text/x-source", "sql", "proto", "tf", "hcl", "nix", "sol", "v", "sv", "vhd", "vhdl", "thrift", "avsc", "prisma")
        spec("markup-text", "Markup / docs source", FileKind.TEXT, full, "text/plain", "tex", "bib", "rst", "adoc", "org", "diff", "patch", "pem", "crt", "cer", "csr", "pub", "ics", "vcf", "eml", "m3u", "m3u8", "pls", "cue", "ass", "ssa")
        spec("markdown", "Markdown", FileKind.MARKDOWN, full, "text/markdown", "md", "markdown", "mdown", "mkd", "mdx", "rmd")
        spec("html", "HTML", FileKind.HTML, full, "text/html", "html", "htm", "xhtml")
        spec("svg", "SVG image", FileKind.SVG, full, "image/svg+xml", "svg")

        // ---- Tabular ---------------------------------------------------------------------
        spec("csv", "CSV / delimited text", FileKind.DELIMITED, full, "text/csv", "csv", "tsv", "tab", "psv")
        spec("xlsx", "Excel workbook (OOXML)", FileKind.SPREADSHEET, full, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", "xlsm", "xltx", "xltm")

        // ---- Office / documents ----------------------------------------------------------
        spec("docx", "Word document (OOXML)", FileKind.DOCX, full, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx", "docm", "dotx", "dotm")
        spec("pptx", "PowerPoint (OOXML)", FileKind.PPTX, full, "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx", "pptm", "ppsx", "potx", "potm")
        spec("pdf", "PDF document", FileKind.PDF, full, "application/pdf", "pdf")
        spec("rtf", "Rich Text Format", FileKind.RTF, full, "application/rtf", "rtf")
        spec("odt", "OpenDocument text", FileKind.ODF, part, "application/vnd.oasis.opendocument.text", "odt", "ott", "fodt")
        spec("ods", "OpenDocument spreadsheet", FileKind.ODF, part, "application/vnd.oasis.opendocument.spreadsheet", "ods", "ots")
        spec("odp", "OpenDocument presentation", FileKind.ODF, part, "application/vnd.oasis.opendocument.presentation", "odp", "otp")
        spec("epub", "EPUB e-book", FileKind.EPUB, full, "application/epub+zip", "epub")
        spec("ole2", "Legacy Microsoft Office (OLE2)", FileKind.LEGACY_OFFICE, part, "application/x-ole-storage", "doc", "xls", "ppt", "msg", "vsd", "xlsb", "wps", "mpp")

        // ---- Images ----------------------------------------------------------------------
        spec("png", "PNG image", FileKind.IMAGE, full, "image/png", "png", "apng")
        spec("jpeg", "JPEG image", FileKind.IMAGE, full, "image/jpeg", "jpg", "jpeg", "jfif", "jpe")
        spec("gif", "GIF image", FileKind.IMAGE, full, "image/gif", "gif")
        spec("webp", "WebP image", FileKind.IMAGE, full, "image/webp", "webp")
        spec("bmp", "Bitmap image", FileKind.IMAGE, full, "image/bmp", "bmp", "dib")
        spec("ico", "Icon", FileKind.IMAGE, full, "image/x-icon", "ico", "cur")
        spec("heif", "HEIC / HEIF / AVIF image", FileKind.IMAGE, part, "image/heif", "heic", "heif", "avif")
        spec("tiff", "TIFF image", FileKind.IMAGE, part, "image/tiff", "tif", "tiff")
        spec("raw", "Camera RAW (DNG)", FileKind.IMAGE, part, "image/x-adobe-dng", "dng")
        spec("psd", "Photoshop / layered image", FileKind.BINARY, ident, "image/vnd.adobe.photoshop", "psd", "xcf", "ai", "eps", "ps", "kra", "sketch", "fig")

        // ---- Audio / video ---------------------------------------------------------------
        spec("mp3", "MP3 audio", FileKind.AUDIO, full, "audio/mpeg", "mp3", "mp2")
        spec("wav", "WAV audio", FileKind.AUDIO, full, "audio/wav", "wav", "wave")
        spec("ogg", "Ogg audio", FileKind.AUDIO, full, "audio/ogg", "ogg", "oga", "opus", "spx")
        spec("m4a", "AAC / M4A audio", FileKind.AUDIO, full, "audio/mp4", "m4a", "aac", "m4b", "3ga")
        spec("flac", "FLAC audio", FileKind.AUDIO, full, "audio/flac", "flac")
        spec("midi", "MIDI", FileKind.AUDIO, full, "audio/midi", "mid", "midi", "kar")
        spec("audio-other", "Other audio", FileKind.AUDIO, part, "audio/x-other", "amr", "awb", "wma", "mka", "aiff", "aif", "ape", "wv")
        spec("mp4", "MP4 video", FileKind.VIDEO, full, "video/mp4", "mp4", "m4v")
        spec("mkv", "Matroska video", FileKind.VIDEO, full, "video/x-matroska", "mkv")
        spec("webm", "WebM video", FileKind.VIDEO, full, "video/webm", "webm")
        spec("3gp", "3GP video", FileKind.VIDEO, full, "video/3gpp", "3gp", "3g2")
        spec("video-other", "Other video", FileKind.VIDEO, part, "video/x-other", "avi", "mov", "mpg", "mpeg", "flv", "wmv", "asf", "ogv", "mts", "m2ts", "vob")

        // ---- Archives --------------------------------------------------------------------
        spec("zip", "ZIP archive", FileKind.ARCHIVE, full, "application/zip", "zip", "zipx")
        spec("zip-app", "ZIP-based package", FileKind.ARCHIVE, full, "application/zip", "jar", "war", "ear", "aar", "apk", "xapk", "apks", "xpi", "crx", "whl", "egg", "nupkg", "vsix", "ipa", "3mf", "kmz", "mrpack", "sb3", "love", "pk3")
        spec("7z", "7-Zip archive", FileKind.ARCHIVE, full, "application/x-7z-compressed", "7z")
        spec("tar", "TAR archive", FileKind.ARCHIVE, full, "application/x-tar", "tar")
        spec("tgz", "TAR + gzip", FileKind.ARCHIVE, full, "application/gzip", "tgz", "tar.gz")
        spec("tbz2", "TAR + bzip2", FileKind.ARCHIVE, full, "application/x-bzip2", "tbz2", "tbz", "tar.bz2")
        spec("txz", "TAR + xz", FileKind.ARCHIVE, full, "application/x-xz", "txz", "tar.xz")
        spec("ar", "ar / Debian package", FileKind.ARCHIVE, full, "application/x-archive", "ar", "deb", "a", "lib")
        spec("cpio", "cpio archive", FileKind.ARCHIVE, full, "application/x-cpio", "cpio")
        spec("gz", "gzip file", FileKind.COMPRESSED_STREAM, full, "application/gzip", "gz")
        spec("bz2", "bzip2 file", FileKind.COMPRESSED_STREAM, full, "application/x-bzip2", "bz2")
        spec("xz", "xz file", FileKind.COMPRESSED_STREAM, full, "application/x-xz", "xz", "lzma")
        spec("rar", "RAR archive", FileKind.ARCHIVE, ident, "application/vnd.rar", "rar", "r00")
        spec("archive-other", "Other archive / disk image", FileKind.BINARY, ident, "application/octet-stream", "iso", "cab", "dmg", "img", "wim", "zst", "lz4", "lz", "z", "arj", "lzh", "ace", "rpm", "vhdx", "vmdk", "qcow2")

        // ---- Databases, fonts, 3D, binaries ------------------------------------------------
        spec("sqlite", "SQLite database", FileKind.SQLITE, full, "application/vnd.sqlite3", "sqlite", "sqlite3", "db", "db3", "sdb", "s3db", "sqlitedb")
        spec("ttf", "TrueType / OpenType font", FileKind.FONT, full, "font/ttf", "ttf", "otf", "ttc", "otc")
        spec("woff", "Web font (WOFF)", FileKind.FONT, ident, "font/woff", "woff", "woff2", "eot")
        spec("stl", "STL 3D model", FileKind.MODEL_3D, full, "model/stl", "stl")
        spec("obj", "Wavefront OBJ", FileKind.MODEL_3D, full, "model/obj", "obj")
        spec("model-other", "Other 3D / CAD", FileKind.MODEL_3D, ident, "model/x-other", "ply", "glb", "gltf", "fbx", "blend", "dwg", "dxf", "step", "stp", "iges", "igs", "skp", "3ds", "usdz")
        spec("exe", "Executable / library", FileKind.BINARY, ident, "application/octet-stream", "exe", "dll", "so", "dylib", "elf", "o", "class", "dex", "wasm", "pyc", "msi", "sys", "ko")
        spec("data", "Binary data", FileKind.BINARY, ident, "application/octet-stream", "bin", "dat", "parquet", "avro", "orc", "pkl", "npy", "h5", "hdf5", "mat", "torrent", "lnk", "pak", "tflite", "onnx", "pb", "ckpt", "safetensors", "gguf")
    }

    val all: List<FormatSpec> get() = specs

    fun byId(id: String): FormatSpec? = byId[id]

    /** Installs the single application handler table, rejecting unknown and multiply-owned IDs. */
    @Synchronized
    fun registerHandlers(handlers: Collection<FormatRouteHandler>) {
        val next = linkedMapOf<String, FormatRouteHandler>()
        handlers.forEach { handler ->
            require(handler.formatIds.isNotEmpty()) { "A format handler must own at least one format ID." }
            handler.formatIds.forEach { id ->
                val normalized = id.lowercase()
                require(normalized.isNotBlank()) { "A format ID cannot be blank." }
                require(byId.containsKey(normalized)) { "No format metadata exists for '$normalized'." }
                require(next.putIfAbsent(normalized, handler) == null) {
                    "More than one handler is registered for format ID '$normalized'."
                }
            }
        }
        handlersById = next.toMap()
    }

    val registeredHandlerIds: Set<String> get() = handlersById.keys

    fun handlerFor(formatId: String): FormatRouteHandler? = handlersById[formatId.lowercase()]

    fun handlerFor(detection: Detection): FormatRouteHandler? = handlerFor(detection.spec.id)

    fun byExtension(ext: String): FormatSpec? = byExt[ext.lowercase().trimStart('.')]

    /** Fallback spec used when nothing else matches. */
    val unknownBinary: FormatSpec get() = byId.getValue("data")
    val plainText: FormatSpec get() = byId.getValue("text")

    val extensionCount: Int get() = byExt.size

    val fullSupportExtensionCount: Int
        get() = specs.filter { it.support == Support.FULL }.sumOf { it.extensions.size }

    /**
     * Lower-cased file extension with support for compound archive suffixes
     * (`.tar.gz`, `.tar.bz2`, `.tar.xz`). Returns "" when there is none.
     */
    fun extensionOf(fileName: String): String {
        val lower = fileName.lowercase().substringAfterLast('/')
        for (compound in COMPOUND) {
            if (lower.endsWith(".$compound") && lower.length > compound.length + 1) return compound
        }
        val dot = lower.lastIndexOf('.')
        return if (dot < 0 || dot == lower.length - 1) "" else lower.substring(dot + 1)
    }

    private val COMPOUND = listOf("tar.gz", "tar.bz2", "tar.xz")
}
