package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DocumentScannerModalSheet
import com.example.ui.screens.BookmarksWorkspaceScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioToolsScreen
import com.example.ui.theme.InfinityCrimsonRed
import com.example.ui.theme.InfinityCrispWhite
import com.example.ui.theme.InfinityMutedGray
import com.example.ui.theme.InfinityOledBlack
import com.example.ui.theme.InfinityPdfTheme
import com.example.ui.theme.InfinitySlateElevated
import com.example.ui.theme.InfinitySlateGray
import com.example.viewmodel.BottomNavTab
import com.example.viewmodel.InfinityPdfViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun isRunningOnEmulatorDevice(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val product = Build.PRODUCT.lowercase()
    val brand = Build.BRAND.lowercase()
    val hardware = Build.HARDWARE.lowercase()
    return fingerprint.startsWith("generic") ||
        fingerprint.contains("emulator") ||
        model.contains("sdk") ||
        model.contains("emulator") ||
        model.contains("android sdk built for") ||
        product.contains("sdk") ||
        product.contains("emulator") ||
        hardware.contains("goldfish") ||
        hardware.contains("ranchu") ||
        (brand.startsWith("generic") && Build.DEVICE.lowercase().startsWith("generic"))
}

class MainActivity : ComponentActivity() {
    private val mainViewModel: InfinityPdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#1E1E1E"))
        )
        if (savedInstanceState == null) {
            mainViewModel.handleIncomingViewIntent(intent)
        }
        setContent {
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            InfinityPdfTheme(themeMode = themeMode) {
                InfinityPdfApp(viewModel = mainViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainViewModel.handleIncomingViewIntent(intent)
    }
}

@Composable
fun InfinityPdfApp(
    viewModel: InfinityPdfViewModel = viewModel()
) {
    val context = LocalContext.current

    val allDocuments by viewModel.allDocuments.collectAsStateWithLifecycle()
    val filteredDocuments by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val allAnnotations by viewModel.allAnnotations.collectAsStateWithLifecycle()
    val allBookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()

    val activeDocument by viewModel.activeDocument.collectAsStateWithLifecycle()
    val currentPageIndex by viewModel.currentPageIndex.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isOledNightMode by viewModel.isOledNightMode.collectAsStateWithLifecycle()
    val isVerticalContinuous by viewModel.isVerticalContinuous.collectAsStateWithLifecycle()
    val pageRotations by viewModel.pageRotations.collectAsStateWithLifecycle()
    val isKeepScreenAwake by viewModel.isKeepScreenAwake.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val activeInkColorHex by viewModel.activeInkColorHex.collectAsStateWithLifecycle()
    val activeStrokeWidth by viewModel.activeStrokeWidth.collectAsStateWithLifecycle()
    val activeEraserSize by viewModel.activeEraserSize.collectAsStateWithLifecycle()
    val activeStampLabel by viewModel.activeStampLabel.collectAsStateWithLifecycle()

    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val encryptedPdfDialogTitle by viewModel.encryptedPdfDialogTitle.collectAsStateWithLifecycle()

    val isAutoUpdateEnabled by viewModel.isAutoUpdateEnabled.collectAsStateWithLifecycle()
    val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val isCheckingForUpdate by viewModel.isCheckingForUpdate.collectAsStateWithLifecycle()
    val isDownloadingUpdate by viewModel.isDownloadingUpdate.collectAsStateWithLifecycle()
    val updateDownloadProgress by viewModel.updateDownloadProgress.collectAsStateWithLifecycle()
    val updateStatusLabel by viewModel.updateStatusLabel.collectAsStateWithLifecycle()

    var showInAppScannerSheet by remember { mutableStateOf(false) }

    // Native Android Local Device PDF Picker
    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Copy immediately to internal storage
            }
            viewModel.importPdfFromUri(uri)
        }
    }

    // Native ML Kit Physical Document Scanner Launcher
    val documentScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val scannedPdfUri = scanResult?.pdf?.uri
            if (scannedPdfUri != null) {
                viewModel.importScannedPdfFromUri(scannedPdfUri)
            } else {
                viewModel.showStatusMessage("Scanner finished without generating a PDF.")
            }
        }
    }

    val launchDocumentScanner: () -> Unit = {
        val activity = context.findActivity()
        if (isRunningOnEmulatorDevice() || activity == null) {
            // On emulator images where GMS optional scanner module is not installed,
            // open the native in-app Document Scanner Studio directly without crashing GmsDocScanDelAct.
            showInAppScannerSheet = true
        } else {
            val scannerOptions = GmsDocumentScannerOptions.Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .setGalleryImportAllowed(true)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .build()
            val scanner = GmsDocumentScanning.getClient(scannerOptions)
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    try {
                        documentScannerLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    } catch (_: Exception) {
                        showInAppScannerSheet = true
                    }
                }
                .addOnFailureListener {
                    showInAppScannerSheet = true
                }
        }
    }

    // Handle back button when on non-home tabs
    BackHandler(enabled = activeDocument == null && selectedTab != BottomNavTab.LIBRARY) {
        viewModel.selectTab(BottomNavTab.LIBRARY)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(InfinityOledBlack),
        containerColor = InfinityOledBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (activeDocument == null) {
                NavigationBar(
                    containerColor = InfinitySlateGray,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("infinity_bottom_nav")
                ) {
                    BottomNavTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        val icon = when (tab) {
                            BottomNavTab.LIBRARY -> if (selected) Icons.Filled.LibraryBooks else Icons.Outlined.LibraryBooks
                            BottomNavTab.STUDIO_TOOLS -> if (selected) Icons.Filled.Construction else Icons.Outlined.Construction
                            BottomNavTab.BOOKMARKS -> if (selected) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks
                            BottomNavTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) InfinityCrispWhite else InfinityMutedGray
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.ui.graphics.Color.White,
                                unselectedIconColor = InfinityCrispWhite,
                                selectedTextColor = InfinityCrispWhite,
                                unselectedTextColor = InfinityMutedGray,
                                // Active bottom navigation pill strictly uses Crimson Red (#ED1C24)
                                indicatorColor = InfinityCrimsonRed
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(InfinityOledBlack)
                .padding(innerPadding)
        ) {
            val currentDoc = activeDocument
            if (currentDoc != null) {
                val docAnnotations = remember(allAnnotations, currentDoc.id) {
                    allAnnotations.filter { it.documentId == currentDoc.id }
                }
                val docBookmarks = remember(allBookmarks, currentDoc.id) {
                    allBookmarks.filter { it.documentId == currentDoc.id }
                }
                PdfReaderScreen(
                    document = currentDoc,
                    currentPageIndex = currentPageIndex,
                    isOledNightMode = isOledNightMode,
                    isVerticalContinuous = isVerticalContinuous,
                    pageRotations = pageRotations,
                    isKeepScreenAwake = isKeepScreenAwake,
                    activeTool = activeTool,
                    activeInkColorHex = activeInkColorHex,
                    activeStrokeWidth = activeStrokeWidth,
                    activeEraserSize = activeEraserSize,
                    activeStampLabel = activeStampLabel,
                    documentAnnotations = docAnnotations,
                    documentBookmarks = docBookmarks,
                    onCloseReader = { viewModel.closeReader() },
                    onSelectPage = { viewModel.setCurrentPage(it) },
                    onToggleNightMode = { viewModel.toggleOledNightMode() },
                    onToggleVerticalMode = { viewModel.toggleVerticalContinuousMode() },
                    onRotatePage = { pageIdx -> viewModel.rotatePage(pageIdx) },
                    onToggleKeepScreenAwake = { viewModel.toggleKeepScreenAwake() },
                    onSelectTool = { viewModel.selectAnnotationTool(it) },
                    onSelectInkColor = { viewModel.selectInkColor(it) },
                    onSelectStrokeWidth = { viewModel.selectStrokeWidth(it) },
                    onSelectEraserSize = { viewModel.selectEraserSize(it) },
                    onSelectStampLabel = { viewModel.selectStampLabel(it) },
                    onAddAnnotation = { pageIdx, tool, points, text ->
                        viewModel.addStrokeAnnotation(currentDoc.id, pageIdx, tool, points, text)
                    },
                    onEraseNearPoint = { pageIdx, normPt ->
                        viewModel.eraseAnnotationNearPoint(currentDoc.id, pageIdx, normPt)
                    },
                    onUndoAnnotation = { pageIdx ->
                        viewModel.undoLastPageAnnotation(currentDoc.id, pageIdx)
                    },
                    onClearPageAnnotations = { pageIdx ->
                        viewModel.clearAllAnnotationsOnPage(currentDoc.id, pageIdx)
                    },
                    onToggleBookmark = { pageIdx, label, note ->
                        viewModel.toggleBookmarkOnPage(currentDoc.id, pageIdx, label, note)
                    },
                    onSaveEdits = {
                        viewModel.saveDocumentEdits(currentDoc)
                    },
                    onPrintDocument = {
                        viewModel.printActiveDocument(context, currentDoc)
                    },
                    onShareDocument = {
                        viewModel.sharePdfDocument(context, currentDoc)
                    },
                    onExportPageAsImage = { pageIdx ->
                        viewModel.exportAndShareActivePageAsImage(context, currentDoc, pageIdx)
                    },
                    onReorderPages = { survivingOrder, outTitle ->
                        viewModel.reorderDocumentPages(currentDoc, survivingOrder, outTitle)
                    },
                    onEncryptedPdfDetected = {
                        viewModel.reportEncryptedPdf(currentDoc.title)
                    }
                )
            } else {
                when (selectedTab) {
                    BottomNavTab.LIBRARY -> {
                        LibraryScreen(
                            documents = filteredDocuments,
                            allAnnotations = allAnnotations,
                            allBookmarks = allBookmarks,
                            searchQuery = searchQuery,
                            selectedFilter = selectedFilter,
                            selectedSort = selectedSort,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onFilterSelect = viewModel::selectFilter,
                            onSortSelect = viewModel::selectSort,
                            onOpenDocument = { docId -> viewModel.openDocument(docId) },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onRenameDocument = viewModel::renameDocument,
                            onShareDocument = { doc ->
                                viewModel.sharePdfDocument(context, doc)
                            },
                            onDeleteDocument = viewModel::deleteDocument,
                            onScanPhysicalDocumentClick = launchDocumentScanner,
                            onImportDeviceFileClick = {
                                openPdfLauncher.launch(arrayOf("application/pdf"))
                            }
                        )
                    }
                    BottomNavTab.STUDIO_TOOLS -> {
                        StudioToolsScreen(
                            documents = allDocuments,
                            onScanDocumentClick = launchDocumentScanner,
                            onReorderPages = viewModel::reorderDocumentPages,
                            onApplyWatermarkAndNumbering = viewModel::applyWatermarkAndNumbering,
                            onMergeDocuments = viewModel::mergeSelectedDocuments,
                            onSplitDocument = viewModel::splitDocumentRange
                        )
                    }
                    BottomNavTab.BOOKMARKS -> {
                        BookmarksWorkspaceScreen(
                            documents = allDocuments,
                            bookmarks = allBookmarks,
                            annotations = allAnnotations,
                            onJumpToDocumentPage = { docId, pageIdx ->
                                viewModel.openDocument(docId, pageIdx)
                            },
                            onDeleteBookmark = viewModel::deleteBookmark,
                            onDeleteAnnotation = viewModel::deleteAnnotationById
                        )
                    }
                    BottomNavTab.SETTINGS -> {
                        SettingsScreen(
                            documentCount = allDocuments.size,
                            annotationCount = allAnnotations.size,
                            bookmarkCount = allBookmarks.size,
                            isVerticalContinuous = isVerticalContinuous,
                            isKeepScreenAwake = isKeepScreenAwake,
                            isOledNightMode = isOledNightMode,
                            onToggleVerticalContinuous = viewModel::toggleVerticalContinuousMode,
                            onToggleKeepScreenAwake = viewModel::toggleKeepScreenAwake,
                            onToggleOledNightMode = viewModel::toggleOledNightMode,
                            onClearRenderCache = viewModel::clearRenderCache,
                            themeMode = themeMode,
                            onSelectThemeMode = viewModel::selectThemeMode,
                            isAutoUpdateEnabled = isAutoUpdateEnabled,
                            onToggleAutoUpdate = viewModel::toggleAutoUpdateOnStartup,
                            installedVersionName = BuildConfig.VERSION_NAME,
                            updateStatusLabel = updateStatusLabel,
                            isCheckingForUpdate = isCheckingForUpdate,
                            isDownloadingUpdate = isDownloadingUpdate,
                            updateDownloadProgress = updateDownloadProgress,
                            availableUpdate = availableUpdate,
                            onCheckForUpdates = { viewModel.checkForGitHubUpdates(showToastWhenUpToDate = true) },
                            onUpdateNowClick = { viewModel.downloadAndInstallUpdate(context) },
                            onDismissUpdateDialog = viewModel::dismissUpdateDialog
                        )
                    }
                }
            }

            if (showInAppScannerSheet) {
                DocumentScannerModalSheet(
                    onDismiss = { showInAppScannerSheet = false },
                    onCompileScannedPages = { bitmaps, title, filterMode ->
                        showInAppScannerSheet = false
                        viewModel.compileScannedBitmapsAndOpen(bitmaps, title, filterMode)
                    }
                )
            }

            // Graceful Encrypted / Password-Protected PDF Dialog
            encryptedPdfDialogTitle?.let { lockedTitle ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissEncryptedPdfDialog() },
                    containerColor = InfinitySlateGray,
                    shape = RoundedCornerShape(16.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Encrypted PDF",
                            tint = InfinityCrimsonRed
                        )
                    },
                    title = {
                        Text(
                            text = "Encrypted PDF",
                            style = MaterialTheme.typography.titleLarge,
                            color = InfinityCrispWhite
                        )
                    },
                    text = {
                        Text(
                            text = "This PDF is password-protected and cannot be displayed ($lockedTitle).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InfinityMutedGray
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissEncryptedPdfDialog() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InfinityCrimsonRed,
                                contentColor = InfinityCrispWhite
                            ),
                            modifier = Modifier.testTag("encrypted_pdf_dialog_ok_button")
                        ) {
                            Text("OK")
                        }
                    }
                )
            }

            // Floating Status Toast Banner (#1E1E1E with 16dp corners)
            AnimatedVisibility(
                visible = statusMessage != null || isBusy,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = InfinitySlateElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = InfinityCrispWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = statusMessage ?: "Processing native PDF operation…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = InfinityCrispWhite,
                            modifier = Modifier.weight(1f)
                        )
                        if (statusMessage != null) {
                            IconButton(
                                onClick = { viewModel.dismissStatusMessage() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss notification",
                                    tint = InfinityMutedGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
