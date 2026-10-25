package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AnnotationToolType
import com.example.data.NormalizedPoint
import com.example.data.PdfAnnotationEntity
import com.example.data.PdfBookmarkEntity
import com.example.data.PdfDatabase
import com.example.data.PdfDocumentEntity
import com.example.data.PdfRepository
import com.example.pdf.NativePdfEngine
import com.example.ui.theme.AppThemeMode
import com.example.update.GitHubReleaseInfo
import com.example.update.GitHubUpdateManager
import com.example.update.UpdateCheckResult
import java.io.File
import kotlin.math.hypot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryFilter(val label: String) {
    ALL("All PDFs"),
    FAVORITES("Favorites"),
    ANNOTATED("Annotated"),
    RECENT("Recent")
}

enum class LibrarySort(val label: String) {
    RECENT("Last Opened"),
    TITLE("Name A–Z"),
    PAGES("Page Count"),
    SIZE("File Size")
}

enum class BottomNavTab(val route: String, val label: String) {
    LIBRARY(route = "library", label = "Library"),
    STUDIO_TOOLS(route = "studio_tools", label = "Studio"),
    BOOKMARKS(route = "bookmarks", label = "Workspace"),
    SETTINGS(route = "settings", label = "Settings")
}

class InfinityPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("infinity_pdf_prefs", Context.MODE_PRIVATE)

    private val repository: PdfRepository = PdfRepository(
        PdfDatabase.getInstance(application).pdfDao()
    )

    val allDocuments: StateFlow<List<PdfDocumentEntity>> = repository.allDocuments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allAnnotations: StateFlow<List<PdfAnnotationEntity>> = repository.allAnnotations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allBookmarks: StateFlow<List<PdfBookmarkEntity>> = repository.allBookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedTab = MutableStateFlow(BottomNavTab.LIBRARY)
    val selectedTab: StateFlow<BottomNavTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    val selectedFilter: StateFlow<LibraryFilter> = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(LibrarySort.RECENT)
    val selectedSort: StateFlow<LibrarySort> = _selectedSort.asStateFlow()

    val filteredDocuments: StateFlow<List<PdfDocumentEntity>> = combine(
        allDocuments,
        allAnnotations,
        _searchQuery,
        _selectedFilter,
        _selectedSort
    ) { docs, annotations, query, filter, sort ->
        val annotatedDocIds = annotations.map { it.documentId }.toSet()
        val q = query.trim().lowercase()
        val filtered = docs.filter { doc ->
            val matchesQuery = q.isEmpty() ||
                doc.title.lowercase().contains(q) ||
                doc.categoryTag.lowercase().contains(q) ||
                doc.summaryPreview.lowercase().contains(q)

            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.FAVORITES -> doc.isFavorite
                LibraryFilter.ANNOTATED -> annotatedDocIds.contains(doc.id)
                LibraryFilter.RECENT -> (System.currentTimeMillis() - doc.lastOpenedEpochMs) < 7 * 24 * 3600 * 1000L
            }
            matchesQuery && matchesFilter
        }

        when (sort) {
            LibrarySort.RECENT -> filtered.sortedByDescending { it.lastOpenedEpochMs }
            LibrarySort.TITLE -> filtered.sortedBy { it.title.lowercase() }
            LibrarySort.PAGES -> filtered.sortedByDescending { it.pageCount }
            LibrarySort.SIZE -> filtered.sortedByDescending { it.fileSizeBytes }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active Reader & Annotation Studio State
    private val _activeDocumentId = MutableStateFlow<Long?>(null)
    val activeDocumentId: StateFlow<Long?> = _activeDocumentId.asStateFlow()

    val activeDocument: StateFlow<PdfDocumentEntity?> = combine(
        allDocuments,
        _activeDocumentId
    ) { docs, activeId ->
        docs.find { it.id == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    // App Theme Mode (System default, Light, Dark)
    private val _themeMode = MutableStateFlow(loadSavedThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _isOledNightMode = MutableStateFlow(false)
    val isOledNightMode: StateFlow<Boolean> = _isOledNightMode.asStateFlow()

    private val _isVerticalContinuous = MutableStateFlow(true)
    val isVerticalContinuous: StateFlow<Boolean> = _isVerticalContinuous.asStateFlow()

    // Granular Per-Page Rotation Map (pageIndex -> 0, 90, 180, 270 degrees)
    private val _pageRotations = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val pageRotations: StateFlow<Map<Int, Int>> = _pageRotations.asStateFlow()

    private val _isKeepScreenAwake = MutableStateFlow(false)
    val isKeepScreenAwake: StateFlow<Boolean> = _isKeepScreenAwake.asStateFlow()

    private val _encryptedPdfDialogTitle = MutableStateFlow<String?>(null)
    val encryptedPdfDialogTitle: StateFlow<String?> = _encryptedPdfDialogTitle.asStateFlow()

    private val _activeTool = MutableStateFlow(AnnotationToolType.VIEW)
    val activeTool: StateFlow<AnnotationToolType> = _activeTool.asStateFlow()

    private val _activeInkColorHex = MutableStateFlow(0xFFED1C24L)
    val activeInkColorHex: StateFlow<Long> = _activeInkColorHex.asStateFlow()

    private val _activeStrokeWidth = MutableStateFlow(4.5f)
    val activeStrokeWidth: StateFlow<Float> = _activeStrokeWidth.asStateFlow()

    private val _activeEraserSize = MutableStateFlow(24f)
    val activeEraserSize: StateFlow<Float> = _activeEraserSize.asStateFlow()

    private val _activeStampLabel = MutableStateFlow("APPROVED")
    val activeStampLabel: StateFlow<String> = _activeStampLabel.asStateFlow()

    // GitHub Auto-Updater State
    private val _isAutoUpdateEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_UPDATE_STARTUP, true)
    )
    val isAutoUpdateEnabled: StateFlow<Boolean> = _isAutoUpdateEnabled.asStateFlow()

    private val _availableUpdate = MutableStateFlow<GitHubReleaseInfo?>(null)
    val availableUpdate: StateFlow<GitHubReleaseInfo?> = _availableUpdate.asStateFlow()

    private val _isCheckingForUpdate = MutableStateFlow(false)
    val isCheckingForUpdate: StateFlow<Boolean> = _isCheckingForUpdate.asStateFlow()

    private val _isDownloadingUpdate = MutableStateFlow(false)
    val isDownloadingUpdate: StateFlow<Boolean> = _isDownloadingUpdate.asStateFlow()

    private val _updateDownloadProgress = MutableStateFlow(0)
    val updateDownloadProgress: StateFlow<Int> = _updateDownloadProgress.asStateFlow()

    private val _updateStatusLabel = MutableStateFlow("Up to date (v${BuildConfig.VERSION_NAME})")
    val updateStatusLabel: StateFlow<String> = _updateStatusLabel.asStateFlow()

    private val _downloadedUpdateApk = MutableStateFlow<File?>(null)
    val downloadedUpdateApk: StateFlow<File?> = _downloadedUpdateApk.asStateFlow()

    // Status Banner & Processing State
    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var lastHandledViewUri: String? = null

    init {
        scanPhoneStorageForPdfs()
        if (_isAutoUpdateEnabled.value) {
            checkForGitHubUpdates(
                showToastWhenUpToDate = false,
                autoDownloadAndInstall = true
            )
        }
    }

    private fun loadSavedThemeMode(): AppThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        return try {
            AppThemeMode.valueOf(raw)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }
    }

    fun selectThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _statusMessage.value = "Theme set to ${mode.label}"
    }

    fun toggleAutoUpdateOnStartup() {
        val next = !_isAutoUpdateEnabled.value
        _isAutoUpdateEnabled.value = next
        prefs.edit().putBoolean(KEY_AUTO_UPDATE_STARTUP, next).apply()
        if (next) {
            _statusMessage.value = "Auto-Update on Startup enabled • Searching for updates…"
            checkForGitHubUpdates(
                showToastWhenUpToDate = false,
                autoDownloadAndInstall = true
            )
        } else {
            _statusMessage.value = "Auto-Update on Startup disabled"
        }
    }

    private var isScanningStorage = false

    fun scanPhoneStorageForPdfs() {
        if (isScanningStorage) return
        isScanningStorage = true
        viewModelScope.launch {
            try {
                val currentDocs = repository.allDocuments.first()
                val existingTitles = currentDocs.map { it.title }.toSet()
                val existingPaths = currentDocs.map { it.filePath }.toSet()
                val detected = NativePdfEngine.autoDetectDevicePdfs(
                    context = getApplication(),
                    existingTitles = existingTitles,
                    existingPaths = existingPaths
                )
                for (doc in detected) {
                    repository.insertDocument(doc)
                }
                if (detected.isNotEmpty()) {
                    _statusMessage.value = "Auto-detected ${detected.size} PDF(s) on phone storage"
                }
            } finally {
                isScanningStorage = false
            }
        }
    }

    fun checkForGitHubUpdates(
        showToastWhenUpToDate: Boolean = true,
        autoDownloadAndInstall: Boolean = false
    ) {
        if (_isCheckingForUpdate.value || _isDownloadingUpdate.value) return
        viewModelScope.launch {
            _isCheckingForUpdate.value = true
            _updateStatusLabel.value = "Checking GitHub Releases…"
            when (val result = GitHubUpdateManager.checkForUpdates(BuildConfig.VERSION_NAME)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _isCheckingForUpdate.value = false
                    _availableUpdate.value = result.releaseInfo
                    _updateStatusLabel.value = "Update waiting (${result.releaseInfo.tagName})"
                    if (autoDownloadAndInstall) {
                        downloadAndInstallUpdate(getApplication())
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    _isCheckingForUpdate.value = false
                    _availableUpdate.value = null
                    _updateStatusLabel.value = "Up to date (v${result.currentVersion})"
                    if (showToastWhenUpToDate) {
                        _statusMessage.value = "Infinity PDF Viewer v${result.currentVersion} is up to date"
                    }
                }
                is UpdateCheckResult.Error -> {
                    _isCheckingForUpdate.value = false
                    _updateStatusLabel.value = "Up to date (v${BuildConfig.VERSION_NAME})"
                    if (showToastWhenUpToDate) {
                        _statusMessage.value = result.message
                    }
                }
            }
        }
    }

    fun downloadAndInstallUpdate(context: Context) {
        val release = _availableUpdate.value ?: return
        if (_isDownloadingUpdate.value) return

        // If already downloaded earlier in this session, launch installer immediately
        val cachedApk = _downloadedUpdateApk.value
        if (cachedApk != null && cachedApk.exists() && cachedApk.length() > 0L) {
            _availableUpdate.value = null
            val started = GitHubUpdateManager.triggerApkInstall(context, cachedApk)
            if (!started) {
                _statusMessage.value = "Unable to launch package installer for ${release.tagName}."
            }
            return
        }

        if (release.apkDownloadUrl.isBlank()) {
            _availableUpdate.value = null
            _statusMessage.value = "No APK asset attached to release ${release.tagName}."
            return
        }

        viewModelScope.launch {
            _isDownloadingUpdate.value = true
            _updateDownloadProgress.value = 0
            _updateStatusLabel.value = "Downloading ${release.tagName} (0%)…"

            val apkFile = GitHubUpdateManager.downloadReleaseApk(
                context = getApplication(),
                releaseInfo = release,
                onProgressPercent = { percent ->
                    _updateDownloadProgress.value = percent
                    _updateStatusLabel.value = "Downloading ${release.tagName} ($percent%)…"
                }
            )

            _isDownloadingUpdate.value = false
            if (apkFile != null && apkFile.exists()) {
                _downloadedUpdateApk.value = apkFile
                _availableUpdate.value = null
                _updateStatusLabel.value = "Update downloaded (${release.tagName}) • Ready to install"
                val launched = GitHubUpdateManager.triggerApkInstall(context, apkFile)
                if (!launched) {
                    _statusMessage.value = "Downloaded ${release.tagName}, but could not start package installer."
                }
            } else {
                _updateStatusLabel.value = "Update waiting (${release.tagName})"
                _statusMessage.value = "Failed to download ${release.tagName} APK from GitHub."
            }
        }
    }

    fun dismissUpdateDialog() {
        val release = _availableUpdate.value
        _availableUpdate.value = null
        if (release != null) {
            _updateStatusLabel.value = "Update waiting (${release.tagName})"
        }
    }

    fun handleIncomingViewIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        val uriKey = uri.toString()
        if (uriKey == lastHandledViewUri) return
        lastHandledViewUri = uriKey
        importPdfFromUri(uri)
    }

    fun selectTab(tab: BottomNavTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFilter(filter: LibraryFilter) {
        _selectedFilter.value = filter
    }

    fun selectSort(sort: LibrarySort) {
        _selectedSort.value = sort
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }

    fun dismissEncryptedPdfDialog() {
        _encryptedPdfDialogTitle.value = null
    }

    fun reportEncryptedPdf(title: String) {
        _encryptedPdfDialogTitle.value = title
    }

    fun openDocument(docId: Long, initialPage: Int? = null) {
        viewModelScope.launch {
            val doc = repository.getDocumentById(docId) ?: return@launch
            val isEncrypted = doc.categoryTag.equals("Encrypted", ignoreCase = true) ||
                NativePdfEngine.checkIfPdfEncrypted(doc.filePath)
            if (isEncrypted) {
                _encryptedPdfDialogTitle.value = doc.title
                return@launch
            }
            val pageToOpen = (initialPage ?: doc.lastReadPage).coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0))
            _currentPageIndex.value = pageToOpen
            _pageRotations.value = emptyMap()
            _activeTool.value = AnnotationToolType.VIEW
            _activeDocumentId.value = docId
            repository.updateDocument(
                doc.copy(
                    lastOpenedEpochMs = System.currentTimeMillis(),
                    lastReadPage = pageToOpen
                )
            )
        }
    }

    fun closeReader() {
        _activeDocumentId.value = null
        _pageRotations.value = emptyMap()
        _activeTool.value = AnnotationToolType.VIEW
    }

    fun setCurrentPage(pageIndex: Int) {
        val doc = activeDocument.value ?: return
        val clamped = pageIndex.coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0))
        _currentPageIndex.value = clamped
        viewModelScope.launch {
            repository.updateDocument(
                doc.copy(
                    lastReadPage = clamped,
                    lastOpenedEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleOledNightMode() {
        _isOledNightMode.value = !_isOledNightMode.value
    }

    fun toggleVerticalContinuousMode() {
        _isVerticalContinuous.value = !_isVerticalContinuous.value
    }

    fun rotatePage(pageIndex: Int) {
        val currentMap = _pageRotations.value
        val currentDeg = currentMap[pageIndex] ?: 0
        val nextDeg = (currentDeg + 90) % 360
        _pageRotations.value = currentMap.toMutableMap().apply {
            if (nextDeg == 0) {
                remove(pageIndex)
            } else {
                put(pageIndex, nextDeg)
            }
        }
    }

    fun rotateActivePage() {
        rotatePage(_currentPageIndex.value)
    }

    fun toggleKeepScreenAwake() {
        val next = !_isKeepScreenAwake.value
        _isKeepScreenAwake.value = next
        _statusMessage.value = if (next) {
            "Study Mode Active: Screen will stay awake while reading"
        } else {
            "Study Mode Disabled"
        }
    }

    fun clearRenderCache() {
        NativePdfEngine.clearBitmapCache()
        _statusMessage.value = "Cleared native PDF page render cache"
    }

    fun selectAnnotationTool(tool: AnnotationToolType) {
        _activeTool.value = tool
        if (tool == AnnotationToolType.HIGHLIGHT && _activeInkColorHex.value == 0xFFED1C24L) {
            _activeInkColorHex.value = 0xFFFFEA00L
        }
    }

    fun selectInkColor(colorHex: Long) {
        _activeInkColorHex.value = colorHex
    }

    fun selectStrokeWidth(width: Float) {
        _activeStrokeWidth.value = width.coerceIn(2f, 18f)
    }

    fun selectEraserSize(size: Float) {
        _activeEraserSize.value = size.coerceIn(8f, 64f)
    }

    fun selectStampLabel(label: String) {
        _activeStampLabel.value = label
    }

    fun toggleFavorite(doc: PdfDocumentEntity) {
        viewModelScope.launch {
            repository.updateDocument(doc.copy(isFavorite = !doc.isFavorite))
        }
    }

    fun renameDocument(doc: PdfDocumentEntity, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.updateDocument(doc.copy(title = trimmed))
            _statusMessage.value = "Renamed document to \"$trimmed\""
        }
    }

    fun deleteDocument(doc: PdfDocumentEntity) {
        viewModelScope.launch {
            if (_activeDocumentId.value == doc.id) {
                _activeDocumentId.value = null
            }
            File(doc.filePath).delete()
            repository.deleteDocument(doc.id)
            _statusMessage.value = "Deleted \"${doc.title}\""
        }
    }

    fun addStrokeAnnotation(
        documentId: Long,
        pageIndex: Int,
        tool: AnnotationToolType,
        points: List<NormalizedPoint>,
        textContent: String = ""
    ) {
        if (points.isEmpty()) return
        viewModelScope.launch {
            repository.insertAnnotation(
                PdfAnnotationEntity(
                    documentId = documentId,
                    pageIndex = pageIndex,
                    toolType = tool.name,
                    colorHex = _activeInkColorHex.value,
                    strokeWidth = _activeStrokeWidth.value,
                    pointsJson = NativePdfEngine.encodePoints(points),
                    textContent = textContent
                )
            )
        }
    }

    fun eraseAnnotationNearPoint(
        documentId: Long,
        pageIndex: Int,
        tapPoint: NormalizedPoint
    ) {
        val pageAnns = allAnnotations.value.filter {
            it.documentId == documentId && it.pageIndex == pageIndex
        }
        val normalizedRadius = (_activeEraserSize.value / 360f).coerceIn(0.025f, 0.22f).toDouble()
        val targets = pageAnns.filter { ann ->
            val pts = NativePdfEngine.decodePoints(ann.pointsJson)
            pts.any { pt ->
                hypot((pt.x - tapPoint.x).toDouble(), (pt.y - tapPoint.y).toDouble()) <= normalizedRadius
            }
        }
        if (targets.isNotEmpty()) {
            viewModelScope.launch {
                targets.forEach { target ->
                    repository.deleteAnnotationById(target.id)
                }
            }
        }
    }

    fun undoLastPageAnnotation(documentId: Long, pageIndex: Int) {
        val lastAnn = allAnnotations.value
            .filter { it.documentId == documentId && it.pageIndex == pageIndex }
            .maxByOrNull { it.createdAtEpochMs } ?: return
        viewModelScope.launch {
            repository.deleteAnnotationById(lastAnn.id)
            _statusMessage.value = "Undid last annotation on Page ${pageIndex + 1}"
        }
    }

    fun clearAllAnnotationsOnPage(documentId: Long, pageIndex: Int) {
        viewModelScope.launch {
            repository.clearPageAnnotations(documentId, pageIndex)
            _statusMessage.value = "Cleared annotations on Page ${pageIndex + 1}"
        }
    }

    fun deleteAnnotationById(annotationId: Long) {
        viewModelScope.launch {
            repository.deleteAnnotationById(annotationId)
        }
    }

    fun toggleBookmarkOnPage(
        documentId: Long,
        pageIndex: Int,
        customLabel: String = "Page ${pageIndex + 1} Bookmark",
        customNote: String = ""
    ) {
        viewModelScope.launch {
            val existing = allBookmarks.value.firstOrNull {
                it.documentId == documentId && it.pageIndex == pageIndex
            }
            if (existing != null) {
                repository.deleteBookmarkById(existing.id)
                _statusMessage.value = "Removed bookmark on Page ${pageIndex + 1}"
            } else {
                repository.insertBookmark(
                    PdfBookmarkEntity(
                        documentId = documentId,
                        pageIndex = pageIndex,
                        label = customLabel.ifBlank { "Page ${pageIndex + 1}" },
                        note = customNote
                    )
                )
                _statusMessage.value = "Bookmarked Page ${pageIndex + 1}"
            }
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            repository.deleteBookmarkById(bookmarkId)
        }
    }

    fun importPdfFromUri(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val imported = NativePdfEngine.importUriToInternalStorage(
                context = getApplication(),
                uri = uri,
                categoryTag = "Local"
            )
            _isBusy.value = false
            if (imported != null) {
                if (imported.categoryTag.equals("Encrypted", ignoreCase = true) ||
                    NativePdfEngine.isPdfPasswordProtected(imported.filePath)
                ) {
                    File(imported.filePath).delete()
                    _encryptedPdfDialogTitle.value = imported.title
                    return@launch
                }
                val id = repository.insertDocument(imported)
                _statusMessage.value = "Imported \"${imported.title}\" (${imported.pageCount} pages)"
                openDocument(id, 0)
            } else {
                _statusMessage.value = "Could not import selected local PDF file."
            }
        }
    }

    fun importScannedPdfFromUri(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val scanTimestamp = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.US)
                .format(java.util.Date())
            val scannedDoc = NativePdfEngine.importUriToInternalStorage(
                context = getApplication(),
                uri = uri,
                customTitle = "Scanned Document ($scanTimestamp)",
                categoryTag = "Scanned"
            )
            _isBusy.value = false
            if (scannedDoc != null) {
                val id = repository.insertDocument(scannedDoc)
                _statusMessage.value = "Scanned \"${scannedDoc.title}\" (${scannedDoc.pageCount} pages)"
                openDocument(id, 0)
            } else {
                _statusMessage.value = "Could not save scanned PDF document."
            }
        }
    }

    fun compileScannedBitmapsAndOpen(
        pageBitmaps: List<Bitmap>,
        outputTitle: String,
        filterMode: NativePdfEngine.ScanFilterMode
    ) {
        if (pageBitmaps.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            val scannedDoc = NativePdfEngine.compileScannedBitmapsToPdf(
                context = getApplication(),
                pageBitmaps = pageBitmaps,
                outputTitle = outputTitle,
                filterMode = filterMode
            )
            _isBusy.value = false
            if (scannedDoc != null) {
                val newId = repository.insertDocument(scannedDoc)
                _statusMessage.value = "Scanned \"${scannedDoc.title}\" (${scannedDoc.pageCount} pages)"
                openDocument(newId, 0)
            } else {
                _statusMessage.value = "Failed to compile scanned PDF document."
            }
        }
    }

    fun reorderDocumentPages(
        sourceDoc: PdfDocumentEntity,
        pageOrderZeroBased: List<Int>,
        outputTitle: String
    ) {
        if (pageOrderZeroBased.isEmpty()) {
            _statusMessage.value = "Keep at least 1 page when saving a reordered PDF."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val reordered = NativePdfEngine.reorderAndSavePdf(
                context = getApplication(),
                sourceDoc = sourceDoc,
                pageOrderZeroBased = pageOrderZeroBased,
                outputTitle = outputTitle.ifBlank { "${sourceDoc.title} (Reordered)" }
            )
            _isBusy.value = false
            if (reordered != null) {
                val newId = repository.insertDocument(reordered)
                _statusMessage.value = "Saved \"${reordered.title}\" (${reordered.pageCount} pages)"
                openDocument(newId, 0)
            } else {
                _statusMessage.value = "Failed to compile reordered PDF."
            }
        }
    }

    fun applyWatermarkAndNumbering(
        sourceDoc: PdfDocumentEntity,
        watermarkText: String,
        watermarkColorArgb: Int,
        opacityFraction: Float,
        isDiagonal: Boolean,
        includePageNumbers: Boolean,
        pageNumberPosition: String,
        pageNumberFormat: String,
        outputTitle: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val resultDoc = NativePdfEngine.applyWatermarkAndPageNumbers(
                context = getApplication(),
                sourceDoc = sourceDoc,
                watermarkText = watermarkText,
                watermarkColorArgb = watermarkColorArgb,
                opacityFraction = opacityFraction,
                isDiagonal = isDiagonal,
                includePageNumbers = includePageNumbers,
                pageNumberPosition = pageNumberPosition,
                pageNumberFormat = pageNumberFormat,
                outputTitle = outputTitle.ifBlank { "${sourceDoc.title} (Watermarked)" }
            )
            _isBusy.value = false
            if (resultDoc != null) {
                val newId = repository.insertDocument(resultDoc)
                _statusMessage.value = "Created \"${resultDoc.title}\" (${resultDoc.pageCount} pages)"
                openDocument(newId, 0)
            } else {
                _statusMessage.value = "Failed to apply watermark and page numbering."
            }
        }
    }

    fun printActiveDocument(context: Context, doc: PdfDocumentEntity) {
        val launched = NativePdfEngine.printPdfDocument(context, doc)
        if (!launched) {
            _statusMessage.value = "Unable to start system print service for \"${doc.title}\"."
        }
    }

    fun exportAndShareActivePageAsImage(
        context: Context,
        doc: PdfDocumentEntity,
        pageIndexZeroBased: Int
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val anns = repository.getAnnotationsForDocumentSync(doc.id)
            val exportedImageFile = NativePdfEngine.exportPageAsHighResImage(
                context = getApplication(),
                sourceDoc = doc,
                pageIndexZeroBased = pageIndexZeroBased,
                annotations = anns,
                invertForOledNight = _isOledNightMode.value
            )
            _isBusy.value = false
            if (exportedImageFile != null && exportedImageFile.exists()) {
                _statusMessage.value = "Exported Page ${pageIndexZeroBased + 1} as High-Res PNG"
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        exportedImageFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "${doc.title} — Page ${pageIndexZeroBased + 1}"
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            "Share Page ${pageIndexZeroBased + 1} High-Res Image"
                        )
                    )
                } catch (e: Exception) {
                    _statusMessage.value = "Saved PNG, but share failed: ${e.localizedMessage}"
                }
            } else {
                _statusMessage.value = "Failed to export page as high-resolution image."
            }
        }
    }

    fun saveDocumentEdits(doc: PdfDocumentEntity) {
        viewModelScope.launch {
            repository.updateDocument(
                doc.copy(lastOpenedEpochMs = System.currentTimeMillis())
            )
            _activeTool.value = AnnotationToolType.VIEW
            _statusMessage.value = "Saved edits to \"${doc.title}\""
        }
    }

    fun mergeSelectedDocuments(docs: List<PdfDocumentEntity>, mergedTitle: String) {
        if (docs.size < 2) {
            _statusMessage.value = "Select at least 2 PDFs to merge."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val merged = NativePdfEngine.mergePdfDocuments(
                context = getApplication(),
                docs = docs,
                mergedTitle = mergedTitle.ifBlank { "Merged Studio Document" }
            )
            _isBusy.value = false
            if (merged != null) {
                val newId = repository.insertDocument(merged)
                _statusMessage.value = "Merged ${docs.size} PDFs into \"${merged.title}\" (${merged.pageCount} pages)"
                openDocument(newId, 0)
            } else {
                _statusMessage.value = "Failed to merge selected PDFs."
            }
        }
    }

    fun splitDocumentRange(
        sourceDoc: PdfDocumentEntity,
        startPageOneBased: Int,
        endPageOneBased: Int,
        outputTitle: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val splitDoc = NativePdfEngine.splitPdfDocument(
                context = getApplication(),
                sourceDoc = sourceDoc,
                startPageOneBased = startPageOneBased,
                endPageOneBased = endPageOneBased,
                outputTitle = outputTitle.ifBlank { "${sourceDoc.title} (Pages $startPageOneBased-$endPageOneBased)" }
            )
            _isBusy.value = false
            if (splitDoc != null) {
                val newId = repository.insertDocument(splitDoc)
                _statusMessage.value = "Extracted ${splitDoc.pageCount} pages to \"${splitDoc.title}\""
                openDocument(newId, 0)
            } else {
                _statusMessage.value = "Failed to extract page range."
            }
        }
    }

    fun sharePdfDocument(context: Context, doc: PdfDocumentEntity) {
        try {
            val file = File(doc.filePath)
            if (!file.exists()) {
                _statusMessage.value = "PDF file not found on storage."
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, doc.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${doc.title}"))
        } catch (e: Exception) {
            _statusMessage.value = "Share failed: ${e.localizedMessage}"
        }
    }

    fun showStatusMessage(message: String) {
        _statusMessage.value = message
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AUTO_UPDATE_STARTUP = "auto_update_on_startup"
    }
}
