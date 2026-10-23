package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.RecentFileEntity
import com.example.data.SettingEntity
import com.example.core.archive.ArchiveNode
import com.example.domain.preview.FileContentState
import com.example.domain.preview.FormatOpenRequest
import com.example.domain.preview.FormatSaveRequest
import com.example.domain.preview.FormatOperationResult
import com.example.core.format.FormatDetector
import com.example.core.format.FormatRegistry
import com.example.core.io.AtomicFileWriter
import com.example.core.io.BatchFileOperations
import com.example.core.io.TextEncoding
import com.example.core.security.PathGuard
import com.example.platform.storage.FileManager
import com.example.formats.registry.DefaultFormatHandlers
import com.example.formats.registry.open
import com.example.formats.registry.save
import com.example.utils.SampleFilesGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File
import java.io.FileOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val fileManager = FileManager()
    private val formatHandlers = DefaultFormatHandlers.create(fileManager)
    private val repository: AppRepository

    // Indexing for super-fast search
    private val allIndexedFiles = java.util.Collections.synchronizedList(mutableListOf<String>())

    private val _isIndexing = MutableStateFlow(false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _searchTermsHistory = MutableStateFlow<List<String>>(emptyList())
    val searchTermsHistory: StateFlow<List<String>> = _searchTermsHistory.asStateFlow()

    private val _favoritePaths = MutableStateFlow<Set<String>>(emptySet())
    val favoritePaths: StateFlow<Set<String>> = _favoritePaths.asStateFlow()

    private fun loadFavorites() {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val set = prefs.getStringSet("favorite_file_paths", emptySet()) ?: emptySet()
        _favoritePaths.value = set
    }

    fun toggleFavorite(path: String) {
        val current = _favoritePaths.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _favoritePaths.value = current
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("favorite_file_paths", current).apply()
    }

    fun isFavorite(path: String): Boolean {
        return _favoritePaths.value.contains(path)
    }

    private fun loadSearchTermsHistory() {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val historyStr = prefs.getString("search_terms_history", "") ?: ""
        if (historyStr.isEmpty()) return
        _searchTermsHistory.value = parseSearchTermsHistory(historyStr)
    }

    /** Reads the JSON-array format (v1.3.0+), falling back to the legacy "|*|"-joined format. */
    private fun parseSearchTermsHistory(raw: String): List<String> {
        return try {
            val json = org.json.JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val term = json.optString(index)
                    if (term.isNotEmpty()) add(term)
                }
            }
        } catch (_: org.json.JSONException) {
            raw.split("|*|").filter { it.isNotEmpty() }
        }
    }

    private fun persistSearchTermsHistory(current: List<String>) {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val json = org.json.JSONArray()
        current.forEach { json.put(it) }
        prefs.edit().putString("search_terms_history", json.toString()).apply()
    }

    fun clearSearchTermsHistory() {
        _searchTermsHistory.value = emptyList()
        persistSearchTermsHistory(emptyList())
    }

    fun addSearchTermToHistory(term: String) {
        if (term.trim().isEmpty()) return
        val current = _searchTermsHistory.value.toMutableList()
        current.remove(term.trim()) // Remove if exists to bring to front
        current.add(0, term.trim()) // Add to front
        if (current.size > 15) {
            current.removeAt(current.size - 1)
        }
        _searchTermsHistory.value = current
        persistSearchTermsHistory(current)
    }

    fun removeSearchTermFromHistory(term: String) {
        val current = _searchTermsHistory.value.toMutableList()
        current.remove(term)
        _searchTermsHistory.value = current
        persistSearchTermsHistory(current)
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.recentFileDao(), database.settingDao())
        FormatRegistry.registerHandlers(formatHandlers)

        loadSearchTermsHistory()
        loadFavorites()


        // Initialize settings, clean up sample files, and build search index from stored cache and background scanner
        viewModelScope.launch(Dispatchers.IO) {
            // Fix Settings DB initialization to inspect real Dao row
            val dbSettings = database.settingDao().getSettingsDirect()
            if (dbSettings == null) {
                repository.updateSettings(SettingEntity())
            }

            // Delete previously created sample files from database to ensure fresh start
            repository.deleteSampleFiles()

            // Legacy per-open copies are no longer made; reclaim any left behind by older versions.
            File(application.cacheDir, "file_cache").deleteRecursively()

            // 1. Immediately load previously saved index from device files cache if any
            try {
                val cacheFile = File(getApplication<Application>().cacheDir, "device_files_cache.txt")
                if (cacheFile.exists()) {
                    val lines = cacheFile.readLines()
                    allIndexedFiles.addAll(lines)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun buildStorageIndex() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_isIndexing.value) return@launch
            _isIndexing.value = true
            try {
                val rootDir = File("/storage/emulated/0")
                val appFilesDir = getApplication<Application>().filesDir
                val externalFilesDir = getApplication<Application>().getExternalFilesDir(null)
                val freshList = mutableListOf<String>()

                val dirsToScan = mutableListOf<File>()
                if (rootDir.exists()) {
                    dirsToScan.add(rootDir)
                    val defaultDirs = listOf("Download", "Documents", "DCIM", "Pictures", "Music")
                    for (dirName in defaultDirs) {
                        val sub = File(rootDir, dirName)
                        if (sub.exists() && sub.isDirectory) {
                            dirsToScan.add(sub)
                        }
                    }
                }
                if (appFilesDir.exists()) {
                    dirsToScan.add(appFilesDir)
                }
                if (externalFilesDir != null && externalFilesDir.exists()) {
                    dirsToScan.add(externalFilesDir)
                }

                for (dir in dirsToScan.distinctBy { it.absolutePath }) {
                    if (dir.exists()) {
                        dir.walkTopDown()
                            .maxDepth(INDEX_MAX_DEPTH)
                            .onEnter { d ->
                                val name = d.name
                                !name.startsWith(".") &&
                                !name.equals("Android", ignoreCase = true) &&
                                !name.contains("cache", ignoreCase = true) &&
                                !name.contains("tmp", ignoreCase = true) &&
                                !name.contains("com.", ignoreCase = true)
                            }
                            .filter { it.isFile }
                            .take(INDEX_MAX_FILES)
                            .forEach {
                                if (freshList.size < INDEX_MAX_FILES) {
                                    freshList.add(it.absolutePath)
                                }
                            }
                    }
                }

                val uniqueList = freshList.distinct()

                synchronized(allIndexedFiles) {
                    allIndexedFiles.clear()
                    allIndexedFiles.addAll(uniqueList)
                }

                // Persist the updated flat files cache
                try {
                    val cacheFile = File(getApplication<Application>().cacheDir, "device_files_cache.txt")
                    cacheFile.writeText(uniqueList.joinToString("\n"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isIndexing.value = false
            }
        }
    }

    // Call this whenever files are modified, saved, or imported, to dynamically update search results!
    private fun indexFile(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            synchronized(allIndexedFiles) {
                if (!allIndexedFiles.contains(filePath)) {
                    allIndexedFiles.add(filePath)
                    // Persist to cache file in background
                    try {
                        val cacheFile = File(getApplication<Application>().cacheDir, "device_files_cache.txt")
                        cacheFile.appendText(filePath + "\n")
                    } catch (e: java.io.IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // Settings State
    val settingsState: StateFlow<SettingEntity> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingEntity()
    )

    // Recent files state combined with search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun seedBenchmarkSampleFiles(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val entities = SampleFilesGenerator.generateAllSamples(getApplication())
            for (entity in entities) {
                repository.insertRecentFile(entity)
            }
            withContext(Dispatchers.Main) {
                onComplete?.invoke(entities.size)
            }
        }
    }

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()

    val recentFilesState: StateFlow<List<RecentFileEntity>> = repository.allRecentFiles
        .combine(_searchQuery) { files, query ->
            _isInitialized.value = true
            if (query.isEmpty()) {
                files
            } else {
                files.filter { it.name.contains(query, ignoreCase = true) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentFileState = MutableStateFlow<FileContentState>(FileContentState.Idle)
    val currentFileState: StateFlow<FileContentState> = _currentFileState.asStateFlow()

    /**
     * Inline edit mode for MD/TXT preview: the preview screen stays in place and morphs into an
     * editor instead of pushing a separate editor route. The draft text itself is intentionally
     * kept as transient Compose state inside [com.example.feature.preview.FilePreviewScreen] (same
     * pattern as EditorScreen) so no TextFieldValue ever leaks into the ViewModel.
     */
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    /** Enters inline edit mode for the currently open text file. Returns false when not allowed. */
    fun enterEditMode(): Boolean {
        val current = _currentFileState.value as? FileContentState.TextSuccess ?: return false
        if (!current.editable) return false
        _isEditMode.value = true
        return true
    }

    /** Leaves inline edit mode and drops any unconfirmed draft (call only after user confirmation). */
    fun exitEditMode() {
        _isEditMode.value = false
    }

    private val _expandedZipPaths = MutableStateFlow<Set<String>>(emptySet())
    val expandedZipPaths: StateFlow<Set<String>> = _expandedZipPaths.asStateFlow()

    fun toggleZipPathExpanded(path: String) {
        val current = _expandedZipPaths.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _expandedZipPaths.value = current
    }

    fun expandZipPath(path: String) {
        val current = _expandedZipPaths.value.toMutableSet()
        current.add(path)
        _expandedZipPaths.value = current
    }

    private val _loadingFilePath = MutableStateFlow<String?>(null)
    val loadingFilePath: StateFlow<String?> = _loadingFilePath.asStateFlow()

    // Navigation trigger event (prevents race conditions)
    sealed class NavigationEvent {
        data class NavigateToPreview(val fileState: FileContentState) : NavigationEvent()
        data class ShowError(val message: String) : NavigationEvent()
    }

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    // File operation events (e.g. notifications)
    private val _fileEvent = MutableStateFlow<String?>(null)
    val fileEvent = _fileEvent.asStateFlow()

    fun clearFileEvent() {
        _fileEvent.value = null
    }

    fun showNotification(message: String) {
        _fileEvent.value = message
    }

    fun resetFileState() {
        _currentFileState.value = FileContentState.Idle
    }

    fun openFile(fileEntity: RecentFileEntity, archivePassword: String = "") {
        _currentFileState.value = FileContentState.Loading
        _loadingFilePath.value = fileEntity.path

        viewModelScope.launch {
            _navigationEvent.send(NavigationEvent.NavigateToPreview(FileContentState.Loading))
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update last opened timestamp in database and retrieve stable DB ID
                val existing = repository.getRecentFileByPath(fileEntity.path)
                val updatedEntity = if (existing != null) {
                    fileEntity.copy(id = existing.id, lastOpened = System.currentTimeMillis())
                } else {
                    fileEntity.copy(lastOpened = System.currentTimeMillis())
                }
                val savedId = repository.insertRecentFile(updatedEntity)
                val finalEntity = updatedEntity.copy(id = if (existing != null) existing.id else savedId.toInt())

                // Add this path to search index dynamically if missing
                indexFile(fileEntity.path)

                val prefs = getApplication<android.app.Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("last_previewed_file_path", fileEntity.path).apply()

                val file = File(fileEntity.path)
                if (!file.exists()) {
                    _currentFileState.value = FileContentState.Error("File not found on disk. It might have been deleted.")
                    _navigationEvent.send(NavigationEvent.NavigateToPreview(_currentFileState.value))
                    return@launch
                }

                // Content-first routing: magic bytes decide, the extension is only a hint.
                val detection = FormatDetector.detect(file, file.name)
                _currentFileState.value = FormatRegistry.open(
                    FormatOpenRequest(
                        file = file,
                        fileEntity = finalEntity,
                        detection = detection,
                        archivePassword = archivePassword,
                        encoding = settingsState.value.defaultEncoding
                    )
                )
            } catch (e: OutOfMemoryError) {
                _currentFileState.value = FileContentState.Error("This file is too large to open in memory on this device.")
            } catch (e: Exception) {
                _currentFileState.value = FileContentState.Error("Failed to open file: ${e.message}")
            } finally {
                _loadingFilePath.value = null
                _navigationEvent.send(NavigationEvent.NavigateToPreview(_currentFileState.value))
            }
        }
    }

    suspend fun getMostRecentFile(): RecentFileEntity? = repository.getMostRecentFile()

    /** Replaces the current image-history path when the user swipes within its folder. */
    fun recordImageFolderPage(currentEntity: RecentFileEntity, imageFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getRecentFileByPath(currentEntity.path)
            val updated = (existing ?: currentEntity).copy(
                path = imageFile.absolutePath,
                name = imageFile.name,
                extension = imageFile.extension.lowercase(),
                size = imageFile.length(),
                lastOpened = System.currentTimeMillis()
            )
            repository.replaceRecentFilePath(currentEntity.path, updated)
        }
    }

    fun openZipEntry(parentZipEntity: RecentFileEntity, node: ArchiveNode, password: String = "") {
        _currentFileState.value = FileContentState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val tempZipExtractsDir = File(context.cacheDir, "zip_extracts")
                if (!tempZipExtractsDir.exists()) {
                    tempZipExtractsDir.mkdirs()
                }

                if (node.unsafe) {
                    _currentFileState.value = FileContentState.Error("This entry has an unsafe path and was not extracted.")
                    _navigationEvent.send(NavigationEvent.NavigateToPreview(_currentFileState.value))
                    return@launch
                }
                val uniqueSubName = "${parentZipEntity.id}_${PathGuard.cacheKey(node.path)}_${PathGuard.sanitizeFileName(node.name, "entry")}"
                val ext = node.name.substringAfterLast('.', "").lowercase()
                val localFile = File(tempZipExtractsDir, uniqueSubName)

                val pwdParam = if (password.isNotEmpty()) password else null
                val extracted = fileManager.extractZipEntry(parentZipEntity.path, node.path, localFile, pwdParam)
                if (extracted && localFile.exists()) {
                    val zipEntryEntity = RecentFileEntity(
                        path = localFile.absolutePath,
                        name = "${parentZipEntity.name} > ${node.name}",
                        size = localFile.length(),
                        extension = ext,
                        lastOpened = System.currentTimeMillis(),
                        isSample = false,
                        parentZipPath = parentZipEntity.path,
                        zipEntryPath = node.path
                    )

                    val insertedId = repository.insertRecentFile(zipEntryEntity)
                    val savedEntity = zipEntryEntity.copy(id = insertedId.toInt())

                    openFile(savedEntity)
                } else {
                    _currentFileState.value = FileContentState.Error("Could not extract entry: ${node.name}")
                    _navigationEvent.send(NavigationEvent.NavigateToPreview(_currentFileState.value))
                }
            } catch (e: Exception) {
                _currentFileState.value = FileContentState.Error("Failed to open ZIP entry: ${e.message}")
                _navigationEvent.send(NavigationEvent.NavigateToPreview(_currentFileState.value))
            }
        }
    }

    fun openParentZip(parentPath: String) {
        _currentFileState.value = FileContentState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parentEntity = repository.getRecentFileByPath(parentPath)
                if (parentEntity != null) {
                    openFile(parentEntity)
                } else {
                    val file = File(parentPath)
                    if (file.exists()) {
                        val recentFile = RecentFileEntity(
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            extension = file.name.substringAfterLast('.', "").lowercase(),
                            lastOpened = System.currentTimeMillis(),
                            isSample = false
                        )
                        val id = repository.insertRecentFile(recentFile)
                        openFile(recentFile.copy(id = id.toInt()))
                    } else {
                        _currentFileState.value = FileContentState.Error("Parent ZIP file not found on disk.")
                        _navigationEvent.send(NavigationEvent.NavigateToPreview(_currentFileState.value))
                    }
                }
            } catch (e: Exception) {
                _currentFileState.value = FileContentState.Error("Failed to reopen parent ZIP: ${e.message}")
                _navigationEvent.send(NavigationEvent.NavigateToPreview(_currentFileState.value))
            }
        }
    }

    /** Writes an edited file back into its parent archive (if any) and reports failure instead of hiding it. */
    private suspend fun syncBackToArchive(fileEntity: RecentFileEntity, diskFile: File) {
        val parentPath = fileEntity.parentZipPath ?: return
        val entryPath = fileEntity.zipEntryPath ?: return
        val updated = fileManager.updateZipEntry(parentPath, entryPath, diskFile)
        if (!updated) {
            _fileEvent.value = "Saved, but the change could not be written back into the archive."
            return
        }
        try {
            val parentEntity = repository.getRecentFileByPath(parentPath)
            parentEntity?.let {
                repository.insertRecentFile(it.copy(size = File(parentPath).length(), lastOpened = System.currentTimeMillis()))
            }
        } catch (e: Exception) {
            // Metadata refresh only; the archive itself was updated.
        }
    }

    private fun isInAppImportedCopy(path: String): Boolean =
        PathGuard.isInside(File(getApplication<Application>().filesDir, "imported"), File(path))

    fun saveSpreadsheetFile(fileEntity: RecentFileEntity, newRows: List<List<String>>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = _currentFileState.value as? FileContentState.CsvSuccess
                val table = current?.table
                if (current == null || current.file.path != fileEntity.path || !current.editable || table == null) {
                    _fileEvent.value = "This sheet is read-only, so nothing was saved."
                    return@launch
                }
                val diskFile = File(fileEntity.path)
                val detection = FormatDetector.detect(diskFile, diskFile.name)
                val saveResult = FormatRegistry.save(
                    detection,
                    FormatSaveRequest.Delimited(
                        file = diskFile,
                        rows = newRows,
                        charsetName = table.charset.name(),
                        hadBom = table.hadBom,
                        delimiter = table.delimiter,
                        lineEnd = table.lineEnd
                    )
                )
                if (saveResult != FormatOperationResult.Completed) {
                    _fileEvent.value = when (saveResult) {
                        is FormatOperationResult.Unsupported -> saveResult.reason
                        is FormatOperationResult.Failed -> "Error saving file: ${saveResult.message}"
                        FormatOperationResult.Completed -> ""
                    }
                    return@launch
                }
                val updatedEntity = fileEntity.copy(size = diskFile.length(), lastOpened = System.currentTimeMillis())
                syncBackToArchive(fileEntity, diskFile)
                repository.insertRecentFile(updatedEntity)
                withContext(Dispatchers.Main) {
                    _currentFileState.value = FileContentState.CsvSuccess(newRows, updatedEntity, false, null, table)
                }

                val writeBackError = if (fileEntity.sourceUri != null) {
                    tryWriteBackToSourceUri(fileEntity, diskFile)
                } else null

                if (fileEntity.sourceUri != null) {
                    if (writeBackError == null) {
                        _fileEvent.value = "Saved and synced back to source file."
                    } else {
                        _fileEvent.value = "Saved to in-app copy ($writeBackError)."
                    }
                } else if (isInAppImportedCopy(fileEntity.path)) {
                    _fileEvent.value = "Saved to the in-app copy. The original file was not changed."
                } else if (settingsState.value.notificationSaveComplete) {
                    _fileEvent.value = "Saved successfully!"
                }
            } catch (e: Exception) {
                _fileEvent.value = "Error saving file: ${e.message}"
            }
        }
    }

    private fun tryWriteBackToSourceUri(fileEntity: RecentFileEntity, diskFile: File): String? {
        val rawUri = fileEntity.sourceUri ?: return null
        return try {
            val uri = android.net.Uri.parse(rawUri)
            val context = getApplication<Application>()
            context.contentResolver.openOutputStream(uri, "wt")?.use { outStream ->
                diskFile.inputStream().use { inStream -> inStream.copyTo(outStream) }
            } ?: return "could not open source provider for writing"
            null
        } catch (e: Exception) {
            "could not sync to source: ${e.message ?: "permission denied"}"
        }
    }

    fun saveTextFile(fileEntity: RecentFileEntity, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = (_currentFileState.value as? FileContentState.TextSuccess)
                    ?.takeIf { it.file.path == fileEntity.path }
                if (current != null && !current.editable) {
                    _fileEvent.value = "This view is read-only (truncated or converted), so nothing was saved."
                    return@launch
                }

                // Safety backup before overwriting to ensure user data is never lost or corrupted
                val diskFile = File(fileEntity.path)
                if (diskFile.exists() && diskFile.length() > 0) {
                    try {
                        val backupDir = diskFile.parentFile ?: getApplication<Application>().cacheDir
                        val backupFile = File(backupDir, ".${diskFile.name}.bak")
                        diskFile.copyTo(backupFile, overwrite = true)
                    } catch (_: Exception) {}
                }

                val explicit = TextEncoding.resolve(settingsState.value.defaultEncoding)
                val charset = explicit
                    ?: current?.charsetName?.let { runCatching { java.nio.charset.Charset.forName(it) }.getOrNull() }
                    ?: Charsets.UTF_8
                val writeBom = current?.hadBom == true
                val detection = FormatDetector.detect(diskFile, diskFile.name)
                val saveResult = FormatRegistry.save(
                    detection,
                    FormatSaveRequest.Text(
                        file = diskFile,
                        content = newContent,
                        charsetName = charset.name(),
                        hadBom = writeBom
                    )
                )
                if (saveResult != FormatOperationResult.Completed) {
                    _fileEvent.value = when (saveResult) {
                        is FormatOperationResult.Unsupported -> saveResult.reason
                        is FormatOperationResult.Failed -> "Error saving file: ${saveResult.message}"
                        FormatOperationResult.Completed -> ""
                    }
                    return@launch
                }

                val updatedEntity = fileEntity.copy(size = diskFile.length(), lastOpened = System.currentTimeMillis())

                syncBackToArchive(fileEntity, diskFile)
                repository.insertRecentFile(updatedEntity)
                indexFile(fileEntity.path)

                val currentState = _currentFileState.value
                if (currentState is FileContentState.TextSuccess && currentState.file.path == fileEntity.path) {
                    _currentFileState.value = FileContentState.TextSuccess(
                        newContent, updatedEntity, false, diskFile.length(), charset.name(), writeBom
                    )
                }

                val writeBackError = if (fileEntity.sourceUri != null) {
                    tryWriteBackToSourceUri(fileEntity, diskFile)
                } else null

                if (fileEntity.sourceUri != null) {
                    if (writeBackError == null) {
                        _fileEvent.value = "Saved and synced back to source file."
                    } else {
                        _fileEvent.value = "Saved to in-app copy ($writeBackError)."
                    }
                } else if (isInAppImportedCopy(fileEntity.path)) {
                    _fileEvent.value = "Saved to the in-app copy. The original file was not changed."
                } else if (settingsState.value.notificationSaveComplete) {
                    _fileEvent.value = "Saved successfully!"
                }
            } catch (e: Exception) {
                _fileEvent.value = "Error saving file: ${e.message}"
            }
        }
    }

    /**
     * Atomically saves a copy of modified content under a new file name without touching the original.
     * Inserts the new copy into the recent files repository so it instantly updates in the UI.
     */
    fun exportCopy(
        originalEntity: RecentFileEntity,
        newFileName: String,
        content: String,
        onComplete: (File?) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val originalFile = File(originalEntity.path)
                val parentDir = originalFile.parentFile?.takeIf { it.exists() && it.canWrite() }
                    ?: getApplication<Application>().getExternalFilesDir(null)
                    ?: getApplication<Application>().filesDir

                val trimmedName = newFileName.trim()
                val targetName = if (trimmedName.contains(".")) {
                    trimmedName
                } else {
                    "$trimmedName.${originalEntity.extension}"
                }

                val targetFile = File(parentDir, targetName)
                val bytes = content.toByteArray(Charsets.UTF_8)

                // Atomic crash-safe write
                AtomicFileWriter.writeBytes(targetFile, bytes)

                val newEntity = RecentFileEntity(
                    path = targetFile.absolutePath,
                    name = targetFile.name,
                    size = targetFile.length(),
                    extension = targetFile.extension.lowercase(),
                    lastOpened = System.currentTimeMillis(),
                    id = 0
                )

                repository.insertRecentFile(newEntity)
                indexFile(targetFile.absolutePath)

                withContext(Dispatchers.Main) {
                    _fileEvent.value = "Exported copy: ${targetFile.name}"
                    onComplete(targetFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _fileEvent.value = "Error exporting copy: ${e.message}"
                    onComplete(null)
                }
            }
        }
    }

    fun deleteRecentFile(fileEntity: RecentFileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeRecentFileById(fileEntity.id)
            // If the deleted file is currently open, reset to Idle
            val currentState = _currentFileState.value
            if (currentState is FileContentState.TextSuccess && currentState.file.id == fileEntity.id ||
                currentState is FileContentState.CsvSuccess && currentState.file.id == fileEntity.id ||
                currentState is FileContentState.ZipSuccess && currentState.file.id == fileEntity.id) {
                _currentFileState.value = FileContentState.Idle
            }
        }
    }

    fun deleteBatchFiles(
        files: List<File>,
        onResult: ((BatchFileOperations.BatchOperationResult) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = BatchFileOperations.deleteFiles(files)
            for (file in result.affectedFiles) {
                repository.removeRecentFileByPath(file.absolutePath)
                synchronized(allIndexedFiles) {
                    allIndexedFiles.remove(file.absolutePath)
                }
            }
            val current = _currentFileState.value.fileOrNull
            if (current != null && result.affectedFiles.any { it.absolutePath == current.path }) {
                _currentFileState.value = FileContentState.Idle
            }
            if (result.isAllSuccessful) {
                _fileEvent.value = "Deleted ${result.successfulCount} item(s)"
            } else if (result.isPartialFailure) {
                _fileEvent.value = "Deleted ${result.successfulCount} item(s), ${result.failedCount} failed"
            } else if (result.failedCount > 0) {
                _fileEvent.value = "Delete failed: ${result.errors.firstOrNull() ?: "Unknown error"}"
            }
            withContext(Dispatchers.Main) {
                onResult?.invoke(result)
            }
        }
    }

    fun copyBatchFiles(
        files: List<File>,
        destinationDir: File,
        overwrite: Boolean = false,
        onResult: ((BatchFileOperations.BatchOperationResult) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = BatchFileOperations.copyFiles(files, destinationDir, overwrite)
            for (file in result.affectedFiles) {
                indexFile(file.absolutePath)
            }
            if (result.isAllSuccessful) {
                _fileEvent.value = "Copied ${result.successfulCount} item(s) to ${destinationDir.name}"
            } else if (result.isPartialFailure) {
                _fileEvent.value = "Copied ${result.successfulCount} item(s), ${result.failedCount} failed"
            } else if (result.failedCount > 0) {
                _fileEvent.value = "Copy failed: ${result.errors.firstOrNull() ?: "Unknown error"}"
            }
            withContext(Dispatchers.Main) {
                onResult?.invoke(result)
            }
        }
    }

    fun moveBatchFiles(
        files: List<File>,
        destinationDir: File,
        overwrite: Boolean = false,
        onResult: ((BatchFileOperations.BatchOperationResult) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = BatchFileOperations.moveFiles(files, destinationDir, overwrite)
            for (file in files) {
                repository.removeRecentFileByPath(file.absolutePath)
                synchronized(allIndexedFiles) {
                    allIndexedFiles.remove(file.absolutePath)
                }
            }
            for (destFile in result.affectedFiles) {
                indexFile(destFile.absolutePath)
            }
            if (result.isAllSuccessful) {
                _fileEvent.value = "Moved ${result.successfulCount} item(s) to ${destinationDir.name}"
            } else if (result.isPartialFailure) {
                _fileEvent.value = "Moved ${result.successfulCount} item(s), ${result.failedCount} failed"
            } else if (result.failedCount > 0) {
                _fileEvent.value = "Move failed: ${result.errors.firstOrNull() ?: "Unknown error"}"
            }
            withContext(Dispatchers.Main) {
                onResult?.invoke(result)
            }
        }
    }

    fun deleteRecentFilesBatch(entities: List<RecentFileEntity>, deleteFromDisk: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val openFileId = _currentFileState.value.fileOrNull?.id
            var resetOpen = false
            for (entity in entities) {
                repository.removeRecentFileById(entity.id)
                if (openFileId == entity.id) resetOpen = true
                if (deleteFromDisk) {
                    val f = File(entity.path)
                    if (f.exists()) f.delete()
                    synchronized(allIndexedFiles) {
                        allIndexedFiles.remove(entity.path)
                    }
                }
            }
            if (resetOpen) {
                _currentFileState.value = FileContentState.Idle
            }
            _fileEvent.value = if (deleteFromDisk) {
                "Deleted ${entities.size} file(s) from device"
            } else {
                "Removed ${entities.size} file(s) from history"
            }
        }
    }

    fun clearRecentFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
            _currentFileState.value = FileContentState.Idle
        }
    }

    fun closeCurrentFile() {
        _currentFileState.value = FileContentState.Idle
    }

    fun clearEditorCache() {
        // Since we write content directly to disk and do not keep complex draft caches other than database states,
        // clearing editor cache can reset the open state.
        _currentFileState.value = FileContentState.Idle
        // Comment out closing/clearing event to prevent Toast popup based on user request
        // _fileEvent.value = "Editor cache cleared"
    }

    fun updateSettings(newSettings: SettingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(newSettings)
        }
    }

    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver
                var fileName = "imported_file_${System.currentTimeMillis()}"
                var fileSize = 0L

                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) fileName = c.getString(nameIndex)
                        if (sizeIndex != -1) fileSize = c.getLong(sizeIndex)
                    }
                }

                // Untrusted display name: reduce to one safe path segment.
                val safeName = PathGuard.sanitizeFileName(fileName, "imported_file_${System.currentTimeMillis()}")

                val importedDir = File(context.filesDir, "imported")
                if (!importedDir.exists()) {
                    importedDir.mkdirs()
                }

                // Same name and size as an earlier import: reuse it (keeps local edits, skips a big copy).
                var destFile = File(importedDir, safeName)
                val reuse = destFile.isFile && fileSize > 0 && destFile.length() == fileSize
                if (destFile.exists() && !reuse) destFile = PathGuard.uniqueFile(importedDir, safeName)
                if (!reuse) {
                    try {
                        contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output -> input.copyTo(output) }
                        } ?: throw java.io.IOException("Could not open the selected file")
                    } catch (e: Exception) {
                        destFile.delete()
                        throw e
                    }
                }
                val sanitizedFileName = destFile.name

                if (fileSize == 0L) {
                    fileSize = destFile.length()
                }

                val extension = sanitizedFileName.substringAfterLast('.', "").lowercase()

                val existing = repository.getRecentFileByPath(destFile.absolutePath)
                val recent = RecentFileEntity(
                    id = existing?.id ?: 0,
                    path = destFile.absolutePath,
                    name = sanitizedFileName,
                    size = fileSize,
                    extension = extension,
                    lastOpened = System.currentTimeMillis(),
                    sourceUri = uri.toString()
                )

                val id = repository.insertRecentFile(recent)
                val finalRecent = recent.copy(id = if (existing != null) existing.id else id.toInt())

                // Index the newly imported file path
                indexFile(destFile.absolutePath)

                // Automatically trigger opening the imported file!
                openFile(finalRecent)
                // Comment out imported popup/toast based on user request
                // _fileEvent.value = "Imported: $sanitizedFileName"
            } catch (e: Exception) {
                _fileEvent.value = "Import failed: ${e.message}"
            }
        }
    }

    // Direct local device search states & methods
    private val _deviceSearchResults = MutableStateFlow<List<File>>(emptyList())
    val deviceSearchResults: StateFlow<List<File>> = _deviceSearchResults.asStateFlow()

    /** True when the query has more matches than the displayed cap ("showing first N"). */
    private val _deviceSearchTruncated = MutableStateFlow(false)
    val deviceSearchTruncated: StateFlow<Boolean> = _deviceSearchTruncated.asStateFlow()

    private val _isSearchingDevice = MutableStateFlow(false)
    val isSearchingDevice: StateFlow<Boolean> = _isSearchingDevice.asStateFlow()

    fun triggerStorageIndexRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            buildStorageIndex()
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun searchLocalFiles(query: String) {
        searchJob?.cancel()
        if (query.trim().isEmpty()) {
            _deviceSearchResults.value = emptyList()
            _deviceSearchTruncated.value = false
            _isSearchingDevice.value = false
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.Default) {
            kotlinx.coroutines.delay(150L) // 150ms debounce
            _isSearchingDevice.value = true
            try {
                val pattern = query.trim()
                val resultsList = mutableListOf<File>()

                // If the indexed list is completely empty, let's build it immediately on Dispatchers.IO
                val isEmpty = synchronized(allIndexedFiles) { allIndexedFiles.isEmpty() }
                if (isEmpty) {
                    buildStorageIndex()
                }

                // Gather matches from the indexed snapshot (name-based, no disk IO per file).
                val currentSnapshot = synchronized(allIndexedFiles) { ArrayList(allIndexedFiles) }
                var matchCount = 0
                val candidates = ArrayList<File>(256)
                for (path in currentSnapshot) {
                    val file = File(path)
                    if (file.name.contains(pattern, ignoreCase = true)) {
                        matchCount++
                        // Buffer a small overshoot so relevance ranking can fill the cap
                        // even after dropping files that no longer exist on disk.
                        if (candidates.size < 300) {
                            candidates.add(file)
                        }
                    }
                }

                // Rank by relevance (exact > prefix > word-boundary > substring), then drop
                // entries whose files were deleted from storage, then apply the display cap.
                val ranked = com.example.core.search.FileSearchRanker.rank(
                    items = candidates.distinctBy { it.absolutePath },
                    query = pattern,
                    nameOf = { it.name },
                    limit = 400
                )
                val existing = ranked.filter { it.isFile }
                resultsList.addAll(existing.take(150))

                // Removed live-scan to use only existing indexes (Room & memory cache)

                _deviceSearchResults.value = resultsList
                _deviceSearchTruncated.value = com.example.core.search.FileSearchRanker.isTruncated(matchCount, 150)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearchingDevice.value = false
            }
        }
    }

    fun importAndOpenFile(file: File) {
        _currentFileState.value = FileContentState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extension = file.name.substringAfterLast('.', "").lowercase()
                val existing = repository.getRecentFileByPath(file.absolutePath)
                val recentFile = RecentFileEntity(
                    id = existing?.id ?: 0,
                    path = file.absolutePath,
                    name = file.name,
                    size = file.length(),
                    extension = extension,
                    lastOpened = System.currentTimeMillis(),
                    isSample = false
                )
                val id = repository.insertRecentFile(recentFile)

                // Index this file dynamically
                indexFile(file.absolutePath)

                val savedEntity = recentFile.copy(id = if (existing != null) existing.id else id.toInt())
                openFile(savedEntity)
            } catch (e: Exception) {
                _currentFileState.value = FileContentState.Error("Failed to open local file: ${e.message}")
            }
        }
    }

    private companion object {
        /** Preview window for text. Beyond this the view is flagged truncated and made read-only. */

        /** Search index limits (the old values, depth 3 and 5,000 files, hid most of a real device). */
        const val INDEX_MAX_DEPTH = 8
        const val INDEX_MAX_FILES = 60_000

    }
}
