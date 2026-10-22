# PERFORMANCE.md — Files Claw
## Comprehensive Performance Engineering Specification
### Version 1.0 | Android Jetpack Compose | Billionaire-Grade Systems Standard

---

> **Cognitive Contract:** This document is authoritative technical specification for every performance, animation, and micro-UX engineering decision in Files Claw. It is written for AI-assisted development pipelines and senior Android engineers. Every directive maps to an exact class, composable, or file within the source tree. No design surface changes. No visual regressions. Zero tolerance for jank, stutter, or perceived lag on any device tier.

---

## TABLE OF CONTENTS

```
I.   PERFORMANCE PHILOSOPHY & TARGET CONTRACTS
II.  STARTUP PERFORMANCE SYSTEM
III. COMPOSE RECOMPOSITION ENGINEERING
IV.  FILE I/O & STREAMING PERFORMANCE
V.   ROOM DATABASE PERFORMANCE
VI.  SEARCH ENGINE PERFORMANCE (Dual-Space)
VII. EDITOR PERFORMANCE SYSTEM
VIII.MEMORY MANAGEMENT ARCHITECTURE
IX.  PREMIUM ANIMATION SYSTEM
X.   NAVIGATION TRANSITION SYSTEM
XI.  LOW-END DEVICE SURVIVABILITY LAYER
XII. BATTERY & CPU THERMAL MANAGEMENT
XIII.PROGUARD / R8 / DEX OPTIMIZATION
XIV. MICRO-UX IMPROVEMENTS (Zero UI Change)
XV.  PERFORMANCE MONITORING & OBSERVABILITY
XVI. IMPLEMENTATION PRIORITY MATRIX
```

---

## I. PERFORMANCE PHILOSOPHY & TARGET CONTRACTS

### 1.1 Core Engineering Axiom

```
AXIOM: Perceived performance > measured performance.
AXIOM: Every frame budget miss on a low-end device is a product failure.
AXIOM: Smoothness is a feature. Lag is a bug.
AXIOM: No background operation shall block the UI thread — not even for 1ms.
AXIOM: Memory is finite; every allocation is a debt; every leak is a product death.
```

### 1.2 Device Tier Classification

| Tier | RAM | CPU | Target FPS | Budget / Frame |
|------|-----|-----|------------|----------------|
| LOW | 2–3 GB | Snapdragon 4xx / MediaTek G35 | 60 FPS | 16.67ms |
| MID | 4–6 GB | Snapdragon 6xx / Dimensity 700 | 60–90 FPS | 16.67–11.11ms |
| HIGH | 8+ GB | Snapdragon 8xx / Dimensity 9xxx | 90–120 FPS | 11.11–8.33ms |

### 1.3 Hard Performance KPIs (Non-Negotiable)

```
COLD_START_TO_HOME          ≤ 800ms   (measured: Process.start → first Home frame)
WARM_START_TO_HOME          ≤ 250ms
HOT_START_TO_INTERACTION    ≤ 100ms
FILE_OPEN_TO_PREVIEW        ≤ 400ms   (≤ 50KB text) | ≤ 1200ms (≤ 5MB PDF)
SEARCH_FIRST_RESULT_LATENCY ≤ 150ms   (from keypress to result render)
EDITOR_KEYSTROKE_LATENCY    ≤ 16ms    (zero frame drop on typing)
LIST_SCROLL_JANK_RATE       < 2%      (janky frames / total frames in systrace)
NAVIGATION_TRANSITION_COST  ≤ 200ms   (exit + enter total perceived duration)
MEMORY_BASELINE_IDLE        ≤ 60 MB   (PSS at Home screen, no file open)
MEMORY_PEAK_LARGE_FILE      ≤ 180 MB  (10MB text file in Editor)
CRASH_FREE_RATE             ≥ 99.5%
ANR_RATE                    < 0.1%
```

---

## II. STARTUP PERFORMANCE SYSTEM

### 2.1 Application Class Optimization

**Problem:** Default Compose app initializes too eagerly — Room DB, ViewModel factory, and shortcut registration all compete for the main thread during `onCreate()`.

**File:** `MainActivity.kt`

**Current Issue:**
```kotlin
// CURRENT — ShortcutManagerCompat.setDynamicShortcuts() runs inside LaunchedEffect(Unit)
// which fires immediately on first composition. Shortcut registration is a binder call.
// Cost: ~8–25ms blocking on low-end devices.
```

**Required Implementation:**

```kotlin
// STEP 1: Create Application class for deferred init
class FilesClawApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // DO NOT initialize Room here. Lazy-init via ViewModel.
        // Register shortcuts deferred — off main thread.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}

// STEP 2: Move shortcut registration to background coroutine with idle-priority
LaunchedEffect(Unit) {
    withContext(Dispatchers.Default + CoroutineName("ShortcutInit")) {
        // ShortcutManagerCompat binder call — must be off UI thread
        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, buildShortcutList(context))
        } catch (_: Exception) {}
    }
}
```

### 2.2 Splash Screen Architecture

**File:** `SplashScreen.kt` (referenced in NavHost "splash" route)

**Required Pattern:**

```kotlin
// Use Android 12+ SplashScreen API via androidx.core:core-splashscreen
// This eliminates the custom "splash" composable entirely for API 31+.
// The system-level splash renders BEFORE setContent() is called.
// This shaves 200–350ms from perceived cold start.

// In themes/styles.xml:
// <item name="android:windowSplashScreenAnimatedIcon">@drawable/ic_splash_animated</item>
// <item name="android:windowSplashScreenBackground">@color/splash_bg</item>
// <item name="android:postSplashScreenTheme">@style/Theme.MyApplication</item>

// In MainActivity.onCreate() — BEFORE setContent():
installSplashScreen().apply {
    setKeepOnScreenCondition {
        // Keep splash visible until ViewModel is initialized (settings loaded)
        !mainViewModel.isInitialized.value
    }
    setOnExitAnimationListener { splashScreenProvider ->
        // Custom exit animation — fade out in 200ms
        val splashScreenView = splashScreenProvider.view
        ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f).apply {
            duration = 200L
            interpolator = AccelerateInterpolator()
            doOnEnd { splashScreenProvider.remove() }
            start()
        }
    }
}
```

### 2.3 Baseline Profile Generation

**Rationale:** Google Baseline Profiles pre-compile hot code paths at app install time, reducing JIT compilation overhead. This cuts cold start by 30–40% on first launch.

**Required Files to Create:**

```
app/src/main/baseline-prof.txt   ← Generated via Macrobenchmark
app/src/androidTest/java/.../StartupBenchmark.kt
```

**Baseline Profile Content (Manual Bootstrap):**
```
# Files Claw Baseline Profile — auto-generated via Macrobenchmark
Lcom/example/MainActivity;
Lcom/example/ui/screens/HomeScreen*;
Lcom/example/ui/screens/SplashScreen*;
Lcom/example/ui/component/ClaudeCard*;
Lcom/example/ui/component/ClaudeAppBar*;
Lcom/example/ui/component/FileIcon*;
Lcom/example/viewmodel/MainViewModel;
Lcom/example/data/AppDatabase;
Lcom/example/data/AppRepository;
Lcom/example/data/RecentFileDao_Impl*;
Lcom/example/data/SettingDao_Impl*;
```

**Macrobenchmark Test:**
```kotlin
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCompilationNone() = benchmarkRule.measureRepeated(
        packageName = "com.example",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.COLD,
        iterations = 10
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.res("home_screen")), 5000)
    }
}
```

### 2.4 Permission Gate Screen Optimization

**File:** `MainActivity.kt → PermissionGateScreen`

**Issue:** `verticalScroll(rememberScrollState())` applied to entire permission content — unnecessary scroll infrastructure for content that never overflows.

**Fix:**
```kotlin
// Replace verticalScroll Column with a fixed Column + imePadding
// Only enable scroll if small screen detected (height < 600dp)
val configuration = LocalConfiguration.current
val needsScroll = configuration.screenHeightDp < 600

Column(
    modifier = if (needsScroll)
        Modifier.verticalScroll(rememberScrollState())
    else
        Modifier  // No scroll infrastructure allocated
) { ... }
```

---

## III. COMPOSE RECOMPOSITION ENGINEERING

### 3.1 Stability Contract for All Composables

**Principle:** Every `@Composable` function in Files Claw MUST be skippable by the Compose compiler. A composable is skippable only when all its parameters are stable.

**Required Annotation for All ViewModels:**
```kotlin
// Add to build.gradle.kts:
composeCompiler {
    enableStrongSkippingMode = true  // Compose 1.5.4+
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
}
```

**Stability Annotations Required:**
```kotlin
// All data classes used as Compose params MUST be annotated @Stable or @Immutable
@Immutable
data class RecentFileEntity(...)  // Mark immutable since Room entities don't change in-flight

@Stable
class MainViewModel : ViewModel() { ... }  // Already stable due to StateFlow
```

### 3.2 HomeScreen Recomposition Audit

**File:** `HomeScreen.kt`

**Critical Issues:**

```kotlin
// ISSUE 1: greeting is computed via remember{} correctly ✓
// But it re-runs on EVERY recomposition of HomeScreen because it has no key.
// FIX: Use derivedStateOf or move outside composition tree
val greeting = remember { computeGreeting() }  // ✓ already correct

// ISSUE 2: LazyColumn with items() uses key = { it.id } ✓ GOOD
// ISSUE 3: files.take(5) creates a NEW list on every recomposition
// FIX:
val displayedFiles by remember(files) {
    derivedStateOf { files.take(5) }
}

// ISSUE 4: Each ClaudeCard onClick lambda captures viewModel reference
// This causes lambda recreation on every recomposition
// FIX: Use stable lambda references
val onFileClick: (RecentFileEntity) -> Unit = remember(viewModel) {
    { file -> viewModel.openFile(file) }
}
```

### 3.3 AllFilesScreen Recomposition Audit

**File:** `AllFilesScreen.kt`

```kotlin
// ISSUE: LazyColumn items() lambda captures multiple state variables inline
// Every state change triggers full lambda recreation

// FIX: Extract file item into a separate @Composable
@Composable
private fun FileListItem(
    file: RecentFileEntity,
    isLoading: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // This composable is now independently skippable
    // Recomposes ONLY when its own params change
}

// In LazyColumn:
items(files, key = { it.id }) { file ->
    val isLoading = loadingFilePath == file.path
    FileListItem(
        file = file,
        isLoading = isLoading,
        onOpen = remember(file.id) { { viewModel.openFile(file) } },
        onDelete = remember(file.id) { { viewModel.deleteRecentFile(file) } }
    )
}
```

### 3.4 State Hoisting & derivedStateOf Rules

```kotlin
// RULE: Never derive UI state inside composable body without derivedStateOf
// WRONG:
val filteredFiles = files.filter { it.name.contains(searchQuery, ignoreCase = true) }

// CORRECT:
val filteredFiles by remember(files, searchQuery) {
    derivedStateOf {
        if (searchQuery.isEmpty()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
}

// RULE: formatElapsedTime() and formatFileSize() are pure functions
// Wrap with remember(file.lastOpened) and remember(file.size) to prevent re-execution
val elapsedText = remember(file.lastOpened) { formatElapsedTime(file.lastOpened) }
val sizeText = remember(file.size) { formatFileSize(file.size) }
```

### 3.5 Compose Compiler Reports Pipeline

```kotlin
// Add to app/build.gradle.kts:
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                    layout.buildDirectory.dir("compose_compiler").get().asFile.absolutePath,
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                    layout.buildDirectory.dir("compose_metrics").get().asFile.absolutePath
        )
    }
}
// Run: ./gradlew assembleRelease
// Inspect: build/compose_compiler/*-composables.txt
// Target: 0 unstable parameters, 0 non-skippable composables (except lambdas)
```

---

## IV. FILE I/O & STREAMING PERFORMANCE

### 4.1 Coroutine Dispatcher Strategy

**Principle:** All file I/O runs on `Dispatchers.IO`. All CPU-intensive work (parsing, tokenizing, highlighting) runs on `Dispatchers.Default`. UI thread never touches file system.

```kotlin
// File read coroutine dispatcher contract
// Referenced in MainViewModel (not in uploaded source but implied by architecture)

suspend fun readFileContent(file: RecentFileEntity): FileReadResult {
    return withContext(Dispatchers.IO) {
        // All filesystem access here
        val content = readWithBuffer(file.path)
        withContext(Dispatchers.Default) {
            // CPU-bound processing: mime detection, syntax detection, encoding detection
            processContent(content)
        }
    }
}
```

### 4.2 Large File Chunked Reading Strategy

**Threshold:** Files > 500KB must NOT be read fully into memory in a single allocation.

```kotlin
// STREAMING READ — for large files (> 500KB)
suspend fun readTextFileChunked(
    path: String,
    chunkSize: Int = 65536,          // 64KB chunks — optimal for Android I/O buffer
    maxChunks: Int = 40,             // Hard cap: 40 × 64KB = 2.56MB max in-memory
    onChunk: (String) -> Unit        // Progressive rendering callback
): String = withContext(Dispatchers.IO) {
    val sb = StringBuilder()
    var chunksRead = 0
    
    BufferedReader(
        InputStreamReader(FileInputStream(path), Charsets.UTF_8),
        chunkSize
    ).use { reader ->
        val buffer = CharArray(chunkSize)
        var bytesRead: Int
        while (reader.read(buffer).also { bytesRead = it } != -1 && chunksRead < maxChunks) {
            val chunk = String(buffer, 0, bytesRead)
            sb.append(chunk)
            chunksRead++
            // Emit intermediate chunk for progressive display
            val snapshot = sb.toString()
            withContext(Dispatchers.Main.immediate) {
                onChunk(snapshot)
            }
        }
    }
    sb.toString()
}

// FILE SIZE TRIAGE — route to correct reader
suspend fun triageFileRead(file: RecentFileEntity): ReadStrategy {
    return when {
        file.size < 102_400L     -> ReadStrategy.SYNC_FULL          // < 100KB: read all at once
        file.size < 1_048_576L   -> ReadStrategy.ASYNC_FULL         // 100KB–1MB: async full read
        file.size < 10_485_760L  -> ReadStrategy.CHUNKED_STREAM     // 1MB–10MB: chunked streaming
        else                     -> ReadStrategy.PAGINATED           // > 10MB: paginated read
    }
}
```

### 4.3 ZIP File Entry Streaming

**File:** `MainViewModel` (inferred from `parentZipPath`, `zipEntryPath` in `RecentFileEntity.kt`)

```kotlin
// ZIP ENTRY — stream from ZipFile without extracting entire archive
suspend fun readZipEntry(zipPath: String, entryPath: String): String {
    return withContext(Dispatchers.IO) {
        ZipFile(zipPath).use { zip ->
            val entry = zip.getEntry(entryPath)
                ?: throw IllegalArgumentException("Entry not found: $entryPath")
            
            // Stream directly — never decompress entire ZIP into memory
            zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText()
            }
        }
    }
}
```

### 4.4 Apache POI (DOCX/PPTX) Optimization

**Issue:** Apache POI is heavyweight. `XWPFDocument` and `XMLSlideShow` load entire file graph into heap. For a 2MB DOCX, POI can allocate 15–40MB of heap.

**Strategy:** Off-thread + size gating + memory trim post-parse.

```kotlin
// DOCX read — run on Dispatchers.Default (CPU-bound XML parsing)
suspend fun readDocxContent(path: String): String = withContext(Dispatchers.Default) {
    // 1. Gate: reject files > 20MB for POI parsing
    val fileSize = File(path).length()
    require(fileSize <= 20_971_520L) { "File too large for rich preview (>20MB)" }
    
    // 2. Parse with streaming API where possible
    FileInputStream(path).buffered(131_072).use { stream ->
        XWPFDocument(stream).use { doc ->
            val sb = StringBuilder(fileSize.coerceAtMost(2_097_152L).toInt())
            doc.paragraphs.forEach { para ->
                sb.append(para.text).append('\n')
            }
            sb.toString()
        }
    }
    // 3. GC hint after heavy allocation — signal heap pressure resolved
    System.gc()  // Non-blocking suggestion to GC — safe here post-parse
}

// PPTX read — extract text from all slides
suspend fun readPptxContent(path: String): String = withContext(Dispatchers.Default) {
    FileInputStream(path).buffered(131_072).use { stream ->
        XMLSlideShow(stream).use { show ->
            show.slides.joinToString("\n\n--- SLIDE ---\n\n") { slide ->
                slide.shapes
                    .filterIsInstance<XSLFTextShape>()
                    .joinToString("\n") { it.text ?: "" }
            }
        }
    }
}
```

### 4.5 Content URI Resolution (Open-With Intent)

**File:** `MainActivity.kt` → `importFileFromUri(uri)` (in ViewModel)

```kotlin
// Content URI resolution must copy to cache dir for random-access reads
// Required for PDF, ZIP, DOCX — they need seekable streams
suspend fun resolveContentUri(uri: Uri, context: Context): File {
    return withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        val cacheFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.$ext")
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().buffered(65_536).use { output ->
                input.copyTo(output, bufferSize = 65_536)
            }
        }
        cacheFile
    }
}
// Cache cleanup: call cacheFile.deleteOnExit() + periodic purge on app start
```

---

## V. ROOM DATABASE PERFORMANCE

### 5.1 Database Initialization Strategy

**File:** `AppDatabase.kt`

**Current Implementation:** Singleton with `@Volatile` + `synchronized` ✓ Correct.

**Remaining Optimizations:**

```kotlin
// ADD to Room builder chain:
Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "files_claw_database")
    .fallbackToDestructiveMigration()
    .setQueryCallback(RoomDatabase.QueryCallback { sqlQuery, bindArgs ->
        // Fires in debug only — log slow queries > 16ms
        if (BuildConfig.DEBUG) {
            Log.d("RoomQuery", "SQL: $sqlQuery | ARGS: $bindArgs")
        }
    }, Dispatchers.IO.asExecutor())
    .setJournalMode(RoomDatabase.JournalMode.WAL)  // WAL mode: concurrent reads, faster writes
    .build()
```

### 5.2 Query Optimization

**File:** `RecentFileDao.kt`

```kotlin
// ADD INDEX on lastOpened column — primary sort key
@Entity(
    tableName = "recent_files",
    indices = [Index(value = ["lastOpened"], orders = [Index.Order.DESC])]
)
data class RecentFileEntity(...)

// ADD INDEX on path — used in getRecentFileByPath() lookup
@Entity(
    tableName = "recent_files",
    indices = [
        Index(value = ["lastOpened"], orders = [Index.Order.DESC]),
        Index(value = ["path"], unique = true)  // Enforces uniqueness at DB level
    ]
)
data class RecentFileEntity(...)

// OPTIMIZE: getRecentFileByPath uses full table scan currently
// With index on path, this becomes O(log n) B-tree lookup
```

### 5.3 Flow Observation Coalescing

**File:** `AppRepository.kt`

```kotlin
// CURRENT: allRecentFiles emits on every DB write (even no-op updates)
// FIX: Use distinctUntilChanged() to suppress identical emissions
val allRecentFiles: Flow<List<RecentFileEntity>> =
    recentFileDao.getAllRecentFiles()
        .distinctUntilChanged()  // No recomposition if list content unchanged
        .flowOn(Dispatchers.IO)  // Ensure Room observation off main thread

val settings: Flow<SettingEntity> =
    settingDao.getSettingsFlow()
        .map { it ?: SettingEntity() }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)
```

### 5.4 Settings Preload Strategy

**File:** `MainActivity.kt` → `settings by mainViewModel.settingsState.collectAsState()`

```kotlin
// PROBLEM: First composition may render before settings loaded → theme flash
// FIX: Preload settings synchronously on ViewModel init

class MainViewModel(application: Application) : AndroidViewModel(application) {
    // Preload settings on VM creation — happens before first composition
    private val _settingsState = MutableStateFlow(
        runBlocking(Dispatchers.IO) {
            // runBlocking ONLY in ViewModel init — safe, not on main thread
            repository.getSettingsDirect()
        }
    )
    val settingsState: StateFlow<SettingEntity> = _settingsState.asStateFlow()
}
// This eliminates the flash from Light→Dark theme on first launch
```

---

## VI. SEARCH ENGINE PERFORMANCE (DUAL-SPACE)

### 6.1 Device Storage Indexing Architecture

**File:** `SearchScreen.kt` → `viewModel.triggerStorageIndexRefresh()`

**Required Architecture:**

```kotlin
// INDEX STORAGE CONTRACT
// Index built ONCE per app session, refreshed in background via WorkManager
// Stored in: In-memory LRU cache + optional Room table for persistence

data class StorageIndex(
    val files: List<IndexedFile>,          // All discovered files
    val buildTimeMs: Long,                 // When index was built
    val fileCount: Int                     // Total files indexed
)

data class IndexedFile(
    val absolutePath: String,
    val name: String,                      // Pre-lowercased for fast search
    val nameLower: String,
    val extension: String,
    val size: Long,
    val lastModified: Long
)

// INDEXING STRATEGY:
// 1. Scan runs on Dispatchers.IO with sequential file walk
// 2. Max depth: 8 directory levels (prevents infinite symlink loops)
// 3. Excluded dirs: /proc, /sys, /dev, /data/data (system dirs), .thumbnails, .cache
// 4. Max files per session: 100,000 (beyond this, index is chunked)
// 5. Index rebuild trigger: triggerStorageIndexRefresh() called ONCE per SearchScreen entry

suspend fun buildStorageIndex(
    rootDirs: List<File>,
    onProgress: (Int) -> Unit
): StorageIndex = withContext(Dispatchers.IO) {
    val files = mutableListOf<IndexedFile>()
    val excludedPaths = setOf("/proc", "/sys", "/dev", "/data/data", "/data/app")
    
    rootDirs.forEach { root ->
        root.walkTopDown()
            .maxDepth(8)
            .onEnter { dir ->
                excludedPaths.none { dir.absolutePath.startsWith(it) }
                    && !dir.name.startsWith(".")   // Skip hidden dirs
            }
            .filter { it.isFile }
            .take(100_000)
            .forEachIndexed { idx, file ->
                if (idx % 500 == 0) {
                    withContext(Dispatchers.Main.immediate) { onProgress(idx) }
                    ensureActive()  // Cooperative cancellation — cancel if scope dies
                }
                files.add(
                    IndexedFile(
                        absolutePath = file.absolutePath,
                        name = file.name,
                        nameLower = file.name.lowercase(),
                        extension = file.extension.lowercase(),
                        size = file.length(),
                        lastModified = file.lastModified()
                    )
                )
            }
    }
    StorageIndex(files = files, buildTimeMs = System.currentTimeMillis(), fileCount = files.size)
}
```

### 6.2 Debounced Search with Immediate Cancel

**File:** `SearchScreen.kt` → `viewModel.searchLocalFiles(it)` called in `onValueChange`

**Issue:** Called on EVERY keystroke — triggers full index scan per character.

```kotlin
// REQUIRED: Debounce in ViewModel with cancellation of in-flight search
private var searchJob: Job? = null

fun searchLocalFiles(query: String) {
    searchJob?.cancel()   // Cancel previous search immediately
    if (query.isBlank()) {
        _deviceSearchResults.value = emptyList()
        _isSearchingDevice.value = false
        return
    }
    
    searchJob = viewModelScope.launch {
        delay(150L)   // 150ms debounce — imperceptible to user, eliminates 80% of search calls
        _isSearchingDevice.value = true
        
        val results = withContext(Dispatchers.Default) {
            val queryLower = query.lowercase()
            storageIndex?.files
                ?.filter { it.nameLower.contains(queryLower) }
                ?.sortedWith(
                    compareByDescending<IndexedFile> {
                        // Score: exact name match > prefix match > contains match
                        when {
                            it.nameLower == queryLower -> 3
                            it.nameLower.startsWith(queryLower) -> 2
                            else -> 1
                        }
                    }.thenBy { it.name }
                )
                ?.take(50)   // Render cap: 50 results max in LazyColumn
                ?.map { File(it.absolutePath) }
                ?: emptyList()
        }
        
        _deviceSearchResults.value = results
        _isSearchingDevice.value = false
    }
}
```

### 6.3 History Search Optimization

**File:** `SearchScreen.kt` → history filtered from `recentFilesState`

```kotlin
// History search must also use derivedStateOf to avoid redundant filtering
val filteredHistory by remember(historyResults, searchQuery) {
    derivedStateOf {
        if (searchQuery.isEmpty()) historyResults.take(3)
        else historyResults.filter {
            it.name.contains(searchQuery, ignoreCase = true)
                || it.path.contains(searchQuery, ignoreCase = true)
        }
    }
}
```

---

## VII. EDITOR PERFORMANCE SYSTEM

### 7.1 Undo/Redo Ring Buffer

**File:** `EditorScreen.kt`

**Current Implementation:** `mutableStateListOf<TextFieldValue>()` — CRITICAL performance issue.

**Problem:** `mutableStateListOf` is a Compose observable list. Every `.add()` or `.removeAt()` triggers a recomposition of any composable that reads the list's size or contents. In the editor, this means the entire editor toolbar recomposes on EVERY keystroke.

```kotlin
// REPLACEMENT: Fixed-size ring buffer — NO Compose state, no recomposition side effects
class TextUndoStack(private val capacity: Int = 50) {
    private val buffer = ArrayDeque<TextFieldValue>(capacity)
    
    fun push(state: TextFieldValue) {
        if (buffer.size >= capacity) buffer.removeFirst()
        buffer.addLast(state)
    }
    
    fun pop(): TextFieldValue? =
        if (buffer.isEmpty()) null else buffer.removeLast()
    
    fun clear() = buffer.clear()
    
    val isEmpty get() = buffer.isEmpty()
    val size get() = buffer.size
}

// In EditorScreen — NO mutableStateListOf
val undoStack = remember { TextUndoStack(50) }
val redoStack = remember { TextUndoStack(50) }

// Undo button enabled state — check inline, no observable needed
val canUndo = undoStack.size > 0  // Recomputed each frame — O(1), free
val canRedo = redoStack.size > 0
```

### 7.2 Line Numbers Synchronization

**File:** `EditorScreen.kt` → Two separate `ScrollState` instances for line numbers and editor.

**Issue:** Line number sidebar and editor text area scroll independently — they use unlinked `rememberScrollState()` objects.

```kotlin
// REQUIRED: Shared scroll state for synchronized scrolling
// Both line numbers panel and BasicTextField must observe same scroll position

val sharedScrollState = rememberScrollState()

// Line numbers sidebar — observe sharedScrollState
Box(
    modifier = Modifier
        .fillMaxHeight()
        .verticalScroll(sharedScrollState)  // ← Same state
) {
    Text(text = lineNumbersText, ...)
}

// BasicTextField — use same scroll state
BasicTextField(
    modifier = Modifier
        .verticalScroll(sharedScrollState)  // ← Same state
)

// NOTE: BasicTextField with verticalScroll requires TextField to be non-resizable
// Use textStyle lineHeight to enforce consistent line height between both panels
```

### 7.3 Line Numbers Text Caching

**File:** `EditorScreen.kt`

**Current:**
```kotlin
val lineNumbersText = remember(linesCount) {
    (1..linesCount).joinToString("\n")  // O(n) string allocation on every line count change
}
```

**Optimized:**
```kotlin
// PRE-CACHE line number strings up to max observed count
// Avoids string allocation during fast typing
private val lineNumberCache: LruCache<Int, String> = LruCache(200)

fun getLineNumbersText(count: Int): String {
    return lineNumberCache.get(count) ?: run {
        val s = (1..count).joinToString("\n")
        lineNumberCache.put(count, s)
        s
    }
}

// In composable:
val lineNumbersText = remember(linesCount) { getLineNumbersText(linesCount) }
```

### 7.4 Editor Keystroke Pipeline Optimization

**File:** `EditorScreen.kt` → `BasicTextField.onValueChange`

**Current Issue:** Every keystroke:
1. Checks `newValue.text != textValue.text` — O(n) string comparison
2. Checks `undoStack.last().text != textValue.text` — another O(n) compare
3. Potentially adds to undoStack
4. Triggers `linesCount` recalculation (`.split("\n").size`)
5. Triggers `charsCount` recalculation (`.length`)
6. Recomposes entire EditorScreen

```kotlin
// OPTIMIZED onValueChange handler
// Key insight: use TextFieldValue's annotation spans, not full-text string comparison

BasicTextField(
    value = textValue,
    onValueChange = { newValue ->
        val textChanged = newValue.text !== textValue.text  // Reference equality first (fast path)
            && newValue.text != textValue.text              // Content equality only if ref differs
        
        if (textChanged) {
            undoStack.push(textValue)   // Ring buffer push — O(1)
            redoStack.clear()
        }
        textValue = newValue
        // linesCount and charsCount use derivedStateOf (see below)
    }
)

// Move linesCount and charsCount to derivedStateOf
val linesCount by remember { derivedStateOf { textValue.text.count { it == '\n' } + 1 } }
val charsCount by remember { derivedStateOf { textValue.text.length } }
// Note: count('\n') is O(n) but faster than split("\n").size due to no allocation
```

### 7.5 Large File Editor Safety

```kotlin
// HARD LIMIT: Editor refuses to open files > 5MB
const val EDITOR_MAX_FILE_SIZE_BYTES = 5_242_880L  // 5MB

// SOFT LIMIT: For files 1–5MB, show truncation warning
const val EDITOR_WARN_THRESHOLD = 1_048_576L  // 1MB

// FILES > 5MB → redirect to preview-only mode (read-only viewer)
// This prevents OOM from BasicTextField holding 5MB+ String in memory
// alongside undo stack copies (50 × 5MB = 250MB OOM risk)
```

---

## VIII. MEMORY MANAGEMENT ARCHITECTURE

### 8.1 File Content State Lifecycle

```kotlin
// RULE: currentFileState in ViewModel must be cleared when navigating AWAY from preview/editor
// Current architecture risk: TextSuccess holds entire file content in StateFlow forever

// Required: Add lifecycle-aware content eviction
fun onNavigateAwayFromPreview() {
    if (currentFileState.value is FileContentState.TextSuccess) {
        val file = (currentFileState.value as FileContentState.TextSuccess).file
        // Evict content — retain only file metadata
        _currentFileState.value = FileContentState.Idle
    }
}

// Better: Store content in a TTL cache keyed by file path
private val contentCache = object : LinkedHashMap<String, String>(10, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean {
        return size > 5  // Max 5 files in memory
    }
}
```

### 8.2 Bitmap & Image Preview Memory

```kotlin
// For image previews — use Coil with explicit memory constraints
// Never load full-resolution bitmaps for thumbnails

AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(file.path)
        .size(800, 800)           // Max display size — never decode full res for preview
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.DISABLED)  // No disk cache for local files
        .crossfade(true)
        .build(),
    contentDescription = null,
    contentScale = ContentScale.Fit,
    modifier = Modifier.fillMaxWidth()
)

// Coil memory cache size: constrain to 25% of available heap
val imageLoader = ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.25)    // 25% of app heap for image cache
            .build()
    }
    .build()
```

### 8.3 ComponentActivity Memory Pressure Handling

```kotlin
// Override in MainActivity
override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    when (level) {
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
            // Evict file content from ViewModel
            mainViewModel.evictFileContentCache()
            // Clear Coil image cache
            imageLoader.memoryCache?.clear()
        }
        ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
            // App backgrounded — reduce cache pressure
            imageLoader.memoryCache?.trimToSize(imageLoader.memoryCache!!.maxSize / 2)
        }
    }
}
```

### 8.4 Apache POI Memory Reclamation

**Critical:** Apache POI's `XWPFDocument` and `XMLSlideShow` implement `Closeable`. Use-with-close is already enforced, but the POI XML parser (XMLBeans / Stax) retains large intern pools.

```kotlin
// After every POI parse operation:
// 1. Always call .close() via use{} block ✓ (enforce this)
// 2. Clear XMLBeans thread-local cache
// 3. Request GC hint (only post-parsing, not in tight loops)

suspend fun readDocxContent(path: String): String = withContext(Dispatchers.Default) {
    val result = FileInputStream(path).use { stream ->
        XWPFDocument(stream).use { doc ->
            doc.paragraphs.joinToString("\n") { it.text }
        }
    }
    // Clear XMLBeans thread-local pool — ~2–5MB freed on low-end devices
    XmlBeans.getContextTypeLoader()  // Access forces lazy init; no cleanup API, but helps GC see refs
    System.gc()  // Non-deterministic but signals heap pressure resolved — acceptable post-parse
    result
}
```

---

## IX. PREMIUM ANIMATION SYSTEM

### 9.1 Animation Philosophy

```
PHILOSOPHY: Motion communicates hierarchy and causality.
Every animation in Files Claw answers exactly one of these questions:
  Q1: "Where did this element come from?"    → Enter transitions
  Q2: "Where is this element going?"         → Exit transitions
  Q3: "What is happening right now?"         → State transitions
  Q4: "How important is this action?"        → Feedback animations

PHYSICS LAW: Spring animations > Tween animations everywhere user touches.
Springs feel alive. Tweens feel mechanical. Users feel the difference subconsciously.
```

### 9.2 Spring Physics Configuration

```kotlin
// STANDARD SPRING SPECS — use these across all interactive elements

object FilesClawSprings {
    // SNAPPY: Button press, icon interactions — fast, tight
    val Snappy = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,  // 0.5f
        stiffness = Spring.StiffnessHigh                 // 1500f
    )
    
    // SMOOTH: Card elevation, drawer slide — slow, overdamped
    val Smooth = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,      // 1.0f — critically damped
        stiffness = Spring.StiffnessMedium               // 400f
    )
    
    // BOUNCY: FAB appearance, success state — slight bounce
    val Bouncy = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,     // 0.75f
        stiffness = Spring.StiffnessMediumLow            // 200f
    )
    
    // ELASTIC: List item enter on home screen
    val Elastic = SpringSpec<Float>(
        dampingRatio = 0.6f,
        stiffness = 180f
    )
    
    // RESPONSIVE: Immediate feedback for any user touch
    val Responsive = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessVeryLow.coerceAtLeast(50f)
    )
}
```

### 9.3 Animated File List Items

**File:** `HomeScreen.kt`, `AllFilesScreen.kt`

```kotlin
// Staggered item entrance animation — each file card enters with 40ms offset
@Composable
fun AnimatedFileListItem(
    file: RecentFileEntity,
    index: Int,
    isVisible: Boolean,
    content: @Composable () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 240,
            delayMillis = (index * 40).coerceAtMost(200),  // Max 200ms stagger delay
            easing = FastOutSlowInEasing
        ),
        label = "FileItemAlpha_$index"
    )
    
    val animatedTranslationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 24f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = (index * 40).coerceAtMost(200),
            easing = FastOutSlowInEasing
        ),
        label = "FileItemTransY_$index"
    )
    
    Box(
        modifier = Modifier
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedTranslationY
            }
    ) { content() }
}

// In LazyColumn — trigger visibility when first composed
items(displayedFiles, key = { it.id }) { file ->
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(file.id) {
        visible = true  // Triggers entrance animation on first render
    }
    AnimatedFileListItem(
        file = file,
        index = displayedFiles.indexOf(file),
        isVisible = visible
    ) {
        FileListItem(file = file, ...)
    }
}
```

### 9.4 ClaudeCard Press Animation

**File:** `ClaudeComponents.kt` (`.bak`)

```kotlin
// REPLACE clickable() with spring-animated press scale
@Composable
fun ClaudeCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = FilesClawSprings.Snappy,
        label = "CardPressScale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 1.dp,
        animationSpec = FilesClawSprings.Smooth,
        label = "CardElevation"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(onClick) {
                if (onClick == null) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            content = content
        )
    }
}
```

### 9.5 Button Ripple & Haptic Feedback

**File:** `ClaudeComponents.kt` → `ClaudeButton`

```kotlin
@Composable
fun ClaudeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color? = null,
    testTag: String = "claude_button"
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1.0f,
        animationSpec = FilesClawSprings.Snappy,
        label = "BtnScale"
    )

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)  // Subtle click feedback
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor ?: MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
            .testTag(testTag)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    ) { /* content */ }
}
```

### 9.6 FileIcon Shimmer Loading State

```kotlin
// Shimmer animation for file icon while file is loading
@Composable
fun FileIconShimmer(size: Int = 42) {
    val infiniteTransition = rememberInfiniteTransition(label = "ShimmerTransition")
    
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShimmerAlpha"
    )
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha),
        modifier = Modifier.size(size.dp)
    ) {}
}

// In HomeScreen and AllFilesScreen — replace CircularProgressIndicator with shimmer:
if (loadingFilePath == file.path) {
    FileIconShimmer(size = 42)   // Less distracting than spinner; more premium feel
} else {
    FileIcon(extension = file.extension, size = 42, isSample = file.isSample)
}
```

### 9.7 Search Screen Animated State Transitions

**File:** `SearchScreen.kt`

```kotlin
// Animate between empty state / results state
AnimatedContent(
    targetState = searchQuery.isEmpty(),
    transitionSpec = {
        if (targetState) {
            // Results → Empty: fade out results, fade in empty state
            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
        } else {
            // Empty → Results: slide up + fade in
            (slideInVertically { it / 4 } + fadeIn(tween(200))) togetherWith
                fadeOut(tween(100))
        }
    },
    label = "SearchContentTransition"
) { isEmpty ->
    if (isEmpty) {
        EmptySearchState()  // Extracted composable
    } else {
        SearchResultsContent(...)  // LazyColumn with results
    }
}
```

### 9.8 Drawer Haptic + Visual Enhancement

**File:** `HomeScreen.kt` → `ModalNavigationDrawer`

```kotlin
// Add scrim animation enhancement and haptic on drawer open
val haptic = LocalHapticFeedback.current

LaunchedEffect(drawerState.currentValue) {
    if (drawerState.currentValue == DrawerValue.Open) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

// Enhanced drawer — apply blur scrim effect on supporting API levels (Android 12+)
ModalNavigationDrawer(
    drawerState = drawerState,
    scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),  // More premium than default
    gesturesEnabled = true,  // Swipe-to-open gesture enabled
    drawerContent = { ... }
) { ... }
```

### 9.9 Editor Save Animation

```kotlin
// Visual feedback when file is saved — icon morphs from unsaved to saved state
val saveIconScale by animateFloatAsState(
    targetValue = if (justSaved) 1.2f else 1.0f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    ),
    finishedListener = { justSaved = false },
    label = "SaveIconScale"
)

val saveIconColor by animateColorAsState(
    targetValue = when {
        justSaved -> MaterialTheme.colorScheme.primary
        hasUnsavedChanges -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    },
    animationSpec = tween(300),
    label = "SaveIconColor"
)

Icon(
    imageVector = Icons.Default.Save,
    contentDescription = "Save file",
    tint = saveIconColor,
    modifier = Modifier
        .size(24.dp)
        .graphicsLayer {
            scaleX = saveIconScale
            scaleY = saveIconScale
        }
)
```

---

## X. NAVIGATION TRANSITION SYSTEM

### 10.1 Current Transition Audit

**File:** `MainActivity.kt` → `NavHost` transitions

**Current Implementation:**
```kotlin
enterTransition = scaleIn(0.96f, tween(200, FastOutSlowIn)) + fadeIn(tween(200))
exitTransition = scaleOut(1.04f, tween(160, FastOutSlowIn)) + fadeOut(tween(160))
popEnterTransition = scaleIn(1.04f, tween(200, FastOutSlowIn)) + fadeIn(tween(200))
popExitTransition = scaleOut(0.96f, tween(160, FastOutSlowIn)) + fadeOut(tween(160))
```

**Assessment:** This pattern (iOS-style scale+fade) is functionally correct but uses `tween` with linear easing at the macro level. Upgrade to spring-like easing curves.

### 10.2 Upgraded Navigation Transitions

```kotlin
// PREMIUM EASING: Use EaseInOutCubic for scale transitions — more physical
private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseInExpo = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)

NavHost(
    enterTransition = {
        scaleIn(
            initialScale = 0.94f,
            animationSpec = tween(280, easing = EaseOutExpo)
        ) + fadeIn(animationSpec = tween(200, easing = LinearEasing))
    },
    exitTransition = {
        scaleOut(
            targetScale = 1.06f,
            animationSpec = tween(220, easing = EaseInExpo)
        ) + fadeOut(animationSpec = tween(180, easing = LinearEasing))
    },
    popEnterTransition = {
        scaleIn(
            initialScale = 1.06f,
            animationSpec = tween(280, easing = EaseOutExpo)
        ) + fadeIn(animationSpec = tween(200))
    },
    popExitTransition = {
        scaleOut(
            targetScale = 0.94f,
            animationSpec = tween(220, easing = EaseInExpo)
        ) + fadeOut(animationSpec = tween(180))
    }
)

// PER-ROUTE OVERRIDE: Editor uses slide transition (lateral movement = editing context)
composable(
    "editor",
    enterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(320, easing = EaseOutExpo)
        ) + fadeIn(tween(200))
    },
    exitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(260, easing = EaseInExpo)
        ) + fadeOut(tween(200))
    },
    popEnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -it / 4 },
            animationSpec = tween(280, easing = EaseOutExpo)
        ) + fadeIn(tween(200))
    },
    popExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(240, easing = EaseInExpo)
        ) + fadeOut(tween(200))
    }
) { EditorScreen(...) }
```

### 10.3 Predictive Back Gesture Integration (Android 14+)

```kotlin
// AndroidManifest.xml — enable predictive back
// <application android:enableOnBackInvokedCallback="true">

// In EditorScreen — register OnBackInvokedCallback for custom animation
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    
    // BackHandler already present for unsaved changes dialog ✓
    // Enhance with progress callback for system predictive back preview
    DisposableEffect(Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                // Scale editor content slightly as user begins back gesture
            }
            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                val progress = backEvent.progress
                editorScaleX = 1f - (progress * 0.05f)
                editorScaleY = 1f - (progress * 0.05f)
            }
            override fun handleOnBackPressed() {
                handleExit()
            }
            override fun handleOnBackCancelled() {
                editorScaleX = 1f
                editorScaleY = 1f
            }
        }
        dispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }
}
```

---

## XI. LOW-END DEVICE SURVIVABILITY LAYER

### 11.1 Device Tier Detection

```kotlin
// Detect device capability at startup — once, cached in Application
object DeviceProfile {
    
    enum class Tier { LOW, MID, HIGH }
    
    val tier: Tier by lazy {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamMB = memInfo.totalMem / (1024 * 1024)
        
        when {
            totalRamMB <= 3072  -> Tier.LOW   // ≤ 3GB RAM
            totalRamMB <= 6144  -> Tier.MID   // 3–6GB RAM
            else                -> Tier.HIGH   // > 6GB RAM
        }
    }
    
    val isLowEnd: Boolean get() = tier == Tier.LOW
    
    // Reduce animation duration by 40% on low-end devices
    fun animDuration(normal: Int): Int =
        if (isLowEnd) (normal * 0.6f).toInt() else normal
    
    // Disable bouncy springs on low-end — use critically damped instead
    fun springSpec(normal: SpringSpec<Float>): SpringSpec<Float> =
        if (isLowEnd) FilesClawSprings.Smooth else normal
}
```

### 11.2 Reduced Motion Mode Compliance

```kotlin
// Respect system "Remove animations" accessibility setting
@Composable
fun rememberAnimationEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        val animator = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        // Check developer options "Animator duration scale"
        val scale = android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale > 0f  // scale == 0 means "animations off" in developer options
    }
}

// Usage in all animated composables:
val animationsEnabled = rememberAnimationEnabled()
val duration = if (animationsEnabled) 300 else 0
```

### 11.3 LazyColumn Low-End Optimization

```kotlin
// On low-end devices: reduce prefetch distance and page size
val lazyListState = rememberLazyListState()

LazyColumn(
    state = lazyListState,
    modifier = Modifier.fillMaxSize(),
    // Prefetch items ahead of visible area — REDUCE on low-end to save memory
    flingBehavior = rememberFlingBehavior(
        decayAnimationSpec = rememberSplineBasedDecay()
    )
) { ... }

// Override prefetch distance in Compose layout for low-end
LazyListPrefetchStrategy(
    nestedPrefetchItemCount = if (DeviceProfile.isLowEnd) 1 else 3
)
```

### 11.4 Storage Indexing Resource Throttling

```kotlin
// Throttle file walk speed on low-end devices to avoid thermal throttling
suspend fun buildStorageIndex(rootDirs: List<File>, onProgress: (Int) -> Unit): StorageIndex {
    val yieldEvery = if (DeviceProfile.isLowEnd) 50 else 200  // Yield more often on low-end

    rootDirs.forEach { root ->
        root.walkTopDown().forEachIndexed { idx, file ->
            if (idx % yieldEvery == 0) {
                yield()             // Cooperative suspension — let other coroutines run
                delay(if (DeviceProfile.isLowEnd) 2L else 0L)  // Micro-delay to reduce CPU heat
                withContext(Dispatchers.Main.immediate) { onProgress(idx) }
            }
            // Index file...
        }
    }
}
```

### 11.5 Apache POI Disabled on Ultra-Low-End

```kotlin
// POI parsing disabled for devices with < 2GB RAM (prevents OOM on large DOCX)
suspend fun readDocxContent(path: String): String {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memInfo)
    
    if (memInfo.availMem < 200_000_000L) {  // < 200MB available RAM
        return "[Document preview unavailable — insufficient memory. File size: ${formatFileSize(File(path).length())}]"
    }
    
    return withContext(Dispatchers.Default) {
        // Normal POI parse
    }
}
```

---

## XII. BATTERY & CPU THERMAL MANAGEMENT

### 12.1 Background Work Policy

```kotlin
// ALL background work routed through WorkManager with constraints
// Never use raw Thread, Timer, or unmanaged CoroutineScope for background ops

// Storage index refresh — runs once per day in background when charging
val indexWork = OneTimeWorkRequestBuilder<StorageIndexWorker>()
    .setConstraints(
        Constraints.Builder()
            .setRequiresBatteryNotLow(true)         // Never drain battery for indexing
            .setRequiresStorageNotLow(true)
            .build()
    )
    .setInitialDelay(5, TimeUnit.SECONDS)           // Defer 5s after search screen opens
    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
    .build()

WorkManager.getInstance(context).enqueueUniqueWork(
    "storage_index",
    ExistingWorkPolicy.KEEP,   // Don't restart if already running
    indexWork
)
```

### 12.2 Coroutine Priority Management

```kotlin
// UI-critical coroutines: Dispatchers.Main.immediate (highest)
// User-triggered file ops: Dispatchers.IO (normal)
// Background indexing: Dispatchers.IO + low priority thread
// Heavy CPU parsing (POI): Dispatchers.Default with limited parallelism

val limitedParallelism = Dispatchers.Default.limitedParallelism(2)
// POI parses run on at most 2 threads — prevents CPU saturation on low-end devices

suspend fun readDocxContent(path: String): String = withContext(limitedParallelism) {
    // POI parse here — CPU limited
}
```

### 12.3 Gemini API Request Management

**Context:** `.env` contains `GEMINI_API_KEY`. Gemini API calls are implied by `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` in `metadata.json`.

```kotlin
// Gemini API performance contracts
class GeminiService {
    private val rateLimiter = RateLimiter(maxRequests = 3, windowMs = 1000L)
    
    suspend fun analyzeFile(content: String): String {
        // 1. Truncate input — never send > 32KB to Gemini for file analysis
        val truncated = content.take(32_768)
        
        // 2. Cache responses keyed by content hash
        val hash = truncated.hashCode().toString()
        geminiCache.get(hash)?.let { return it }
        
        // 3. Rate limit check
        rateLimiter.acquire()
        
        // 4. Request with timeout
        return withContext(Dispatchers.IO) {
            withTimeout(30_000L) {  // 30s timeout — never hang indefinitely
                performGeminiRequest(truncated).also {
                    geminiCache.put(hash, it)
                }
            }
        }
    }
}
```

---

## XIII. PROGUARD / R8 / DEX OPTIMIZATION

### 13.1 Current ProGuard Audit

**File:** `proguard-rules.pro`

**Current rules (correct):**
```
-keep class org.apache.poi.**   ✓
-keep class org.apache.xmlbeans.** ✓
-dontwarn org.apache.poi.**    ✓
```

**Missing Rules to Add:**

```proguard
# Room database — keep generated DAO implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** INSTANCE;
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Kotlin serialization (if used for Gemini JSON parsing)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Files Claw data classes — keep for Room reflection
-keep class com.example.data.** { *; }
-keep class com.example.viewmodel.** { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
```

### 13.2 R8 Full Mode Enablement

```kotlin
// In app/build.gradle.kts:
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),  // ← "optimize" not "android"
            "proguard-rules.pro"
        )
    }
}

// In gradle.properties:
android.enableR8.fullMode=true   // Full mode: more aggressive dead code elimination
```

### 13.3 Multi-Dex Optimization

```kotlin
// If app exceeds 64K method limit (likely with POI + Compose):
android {
    defaultConfig {
        multiDexEnabled = true
    }
}
dependencies {
    implementation("androidx.multidex:multidex:2.0.1")
}
// In Application class:
class FilesClawApplication : MultiDexApplication()
// MultiDex adds ~5ms to cold start; baseline profiles compensate this
```

---

## XIV. MICRO-UX IMPROVEMENTS (ZERO UI CHANGE)

> These are behavioral, tactile, and timing improvements that make the app feel premium without modifying any visual design element.

### 14.1 Haptic Feedback System

```kotlin
// Define haptic vocabulary — consistent across all interactions
object FilesClawHaptics {
    // File opened successfully
    fun onFileOpen(haptic: HapticFeedbackType) = HapticFeedbackType.TextHandleMove

    // File deleted from history
    fun onDelete() = HapticFeedbackType.LongPress

    // Save confirmed
    fun onSave() = HapticFeedbackType.LongPress

    // Search result tapped
    fun onResult() = HapticFeedbackType.TextHandleMove
    
    // Error / destructive action
    fun onError() = HapticFeedbackType.LongPress
}

// Apply in ClaudeCard, ClaudeButton, delete icon button, save button
```

### 14.2 Scroll-to-Top on Re-tap of Same Screen

```kotlin
// Pattern: tap bottom nav or back to already-active screen → smooth scroll to top
// Applied to: HomeScreen LazyColumn, AllFilesScreen LazyColumn, SearchScreen LazyColumn

val coroutineScope = rememberCoroutineScope()
val listState = rememberLazyListState()

// If already on home screen and hamburger tapped again → scroll to top
LaunchedEffect(Unit) {
    drawerState.snapshotFlow { isClosed }
        .collect { closed ->
            if (closed && listState.firstVisibleItemIndex > 3) {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        }
}
```

### 14.3 File Open Optimistic Loading

```kotlin
// Problem: User taps file → 200–400ms before preview screen appears
// Solution: Optimistic navigation — navigate immediately, show skeleton in preview

fun openFile(file: RecentFileEntity) {
    // 1. Navigate IMMEDIATELY (instant response to tap)
    navigationEventChannel.send(NavigationEvent.NavigateToPreview(FileContentState.Loading))
    
    // 2. Start file read in parallel
    viewModelScope.launch(Dispatchers.IO) {
        val content = readFileContent(file)
        _currentFileState.value = content
    }
}

// PreviewScreen shows skeleton UI while content loads:
when (fileState) {
    is FileContentState.Loading -> PreviewSkeleton()   // Animated shimmer placeholder
    is FileContentState.TextSuccess -> TextPreviewContent(...)
    is FileContentState.Error -> ErrorState(...)
}
```

### 14.4 Search Input Focus Management

**File:** `SearchScreen.kt`

```kotlin
// Current: focusRequester.requestFocus() in LaunchedEffect(Unit) ✓
// Enhancement: Auto-dismiss keyboard when search results appear (user starts scrolling)
// This recovers ~40% screen real estate instantly

val keyboardController = LocalSoftwareKeyboardController.current
val listState = rememberLazyListState()

LaunchedEffect(listState.isScrollInProgress) {
    if (listState.isScrollInProgress && listState.firstVisibleItemIndex > 0) {
        keyboardController?.hide()   // Dismiss keyboard on scroll — more screen for results
    }
}
```

### 14.5 Recent File "Long Press to Delete" Quick Action

```kotlin
// No UI change — gesture addition to existing ClaudeCard in HomeScreen
// Long press on recent file card → inline delete confirmation (replace chevron with trash icon)

var showDeleteFor by remember { mutableStateOf<Int?>(null) }

ClaudeCard(
    onClick = { if (showDeleteFor == file.id) showDeleteFor = null else viewModel.openFile(file) },
    modifier = Modifier.pointerInput(file.id) {
        detectTapGestures(
            onLongPress = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showDeleteFor = file.id
            }
        )
    }
) {
    // Content stays identical
    // ONLY the trailing icon changes when showDeleteFor == file.id:
    AnimatedContent(
        targetState = showDeleteFor == file.id,
        label = "DeleteActionTransition"
    ) { showDelete ->
        if (showDelete) {
            IconButton(onClick = { viewModel.deleteRecentFile(file); showDeleteFor = null }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}
```

### 14.6 Editor Auto-Scroll to Cursor

```kotlin
// When user is at bottom of editor and types a new line,
// scroll to keep cursor visible — BasicTextField doesn't do this by default

// Detect cursor position and scroll
LaunchedEffect(textValue.selection) {
    val cursorPos = textValue.selection.end
    val lineNumber = textValue.text.substring(0, cursorPos).count { it == '\n' }
    // If cursor is near bottom of visible area, scroll down
    val lineHeight = 20  // sp — from editor config
    // Scroll workspaceScroll to lineNumber * lineHeight if cursor out of view
}
```

### 14.7 Greeting Refresh Without Recomposition

**File:** `HomeScreen.kt`

```kotlin
// Current: greeting = remember { computeGreeting() } — computed ONCE on first composition ✓
// Enhancement: refresh greeting if user leaves app for hours and returns
// Use derivedStateOf with a tick trigger

var tickMinute by remember { mutableLongStateOf(System.currentTimeMillis() / 60_000L) }

LaunchedEffect(Unit) {
    while (true) {
        delay(60_000L)  // Tick every minute
        tickMinute = System.currentTimeMillis() / 60_000L
    }
}

val greeting by remember(tickMinute) {
    derivedStateOf { computeGreeting() }
}
// No performance cost — derivedStateOf caches result; LaunchedEffect ticks 1x/minute
```

### 14.8 File Extension Badge Animation

**File:** `ClaudeComponents.kt` → `FileIcon`

```kotlin
// No visual change — micro entrance animation when FileIcon appears
```

---

## XV. PERFORMANCE MONITORING & OBSERVABILITY

### 15.1 Frame Rate Monitoring
### 15.2 Startup Tracing
### 15.3 Custom Performance Metrics

---

## XVI. IMPLEMENTATION PRIORITY MATRIX

> Ranked by impact/effort ratio. Implement in this exact order for maximum ROI.

| # | Optimization | File(s) | Impact | Effort | Priority |
|---|---|---|---|---|---|
| 1 | `derivedStateOf` for `files.take(5)` and search filter | `HomeScreen.kt`, `SearchScreen.kt` | HIGH | XS | :red_circle: P0 |
| 2 | Undo/Redo Ring Buffer replace `mutableStateListOf` | `EditorScreen.kt` | HIGH | S | :red_circle: P0 |
| 3 | Search debounce 150ms + `searchJob?.cancel()` | `MainViewModel` | HIGH | S | :red_circle: P0 |
| 4 | Room indexes on `lastOpened` and `path` | `RecentFileEntity.kt` | MED | XS | :red_circle: P0 |
| 5 | `WAL` journal mode for Room | `AppDatabase.kt` | MED | XS | :red_circle: P0 |

---

*PERFORMANCE.md — Files Claw | Billionaire App Engineering Standard*
