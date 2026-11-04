package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import kotlinx.coroutines.delay

// Pre-allocated file-level shapes & colors for zero-allocation 60/120fps rendering
private val LiquidGlassCapsuleShape = RoundedCornerShape(percent = 50)
private val LiquidGlassPillShape = RoundedCornerShape(percent = 50)
private val LiquidGlassBaseTint = Color(0xCC121216)

fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

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
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
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
    val lifecycleOwner = LocalLifecycleOwner.current

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

    // Auto-dismiss bottom status notification banner after 2800ms once any background operation completes
    LaunchedEffect(statusMessage, isBusy) {
        if (statusMessage != null && !isBusy) {
            delay(2800L)
            viewModel.dismissStatusMessage()
        }
    }

    val isAutoUpdateEnabled by viewModel.isAutoUpdateEnabled.collectAsStateWithLifecycle()
    val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val isCheckingForUpdate by viewModel.isCheckingForUpdate.collectAsStateWithLifecycle()
    val isDownloadingUpdate by viewModel.isDownloadingUpdate.collectAsStateWithLifecycle()
    val updateDownloadProgress by viewModel.updateDownloadProgress.collectAsStateWithLifecycle()
    val updateStatusLabel by viewModel.updateStatusLabel.collectAsStateWithLifecycle()

    var showInAppScannerSheet by remember { mutableStateOf(false) }

    // Storage Permission Check & Rationale Dialog State
    var storagePermissionGranted by remember {
        mutableStateOf(hasStoragePermission(context))
    }
    var showStoragePermissionDialog by remember {
        mutableStateOf(!hasStoragePermission(context))
    }

    // Android 11+ (API 30+) All Files Access Settings Launcher
    val manageAllFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = hasStoragePermission(context)
        storagePermissionGranted = granted
        if (granted) {
            showStoragePermissionDialog = false
            viewModel.scanPhoneStorageForPdfs()
        }
    }

    // Android 10 and below (API <= 29) Runtime Storage Permission Launcher
    val legacyStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        storagePermissionGranted = isGranted
        if (isGranted) {
            showStoragePermissionDialog = false
            viewModel.scanPhoneStorageForPdfs()
        }
    }

    val requestStoragePermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                manageAllFilesLauncher.launch(intent)
            } catch (_: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageAllFilesLauncher.launch(fallbackIntent)
                } catch (_: Exception) {
                    viewModel.showStatusMessage("Unable to open All Files Access settings.")
                }
            }
        } else {
            legacyStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Re-check storage permission when returning to the app from system Settings
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = hasStoragePermission(context)
                storagePermissionGranted = granted
                if (granted) {
                    showStoragePermissionDialog = false
                    viewModel.scanPhoneStorageForPdfs()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
        contentWindowInsets = WindowInsets.statusBars
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
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
                }
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
                            },
                            hasStoragePermission = storagePermissionGranted,
                            onRequestStoragePermission = requestStoragePermission
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

            // Floating 60/120fps Liquid Glass Capsule Dock overlaid at BottomCenter when reader is closed
            if (activeDocument == null) {
                LiquidGlassBottomDock(
                    selectedTab = selectedTab,
                    onSelectTab = viewModel::selectTab,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp)
                )
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

            // First-Launch Storage Permission Rationale Dialog (#1E1E1E surface with 16dp rounded corners)
            if (showStoragePermissionDialog && !storagePermissionGranted) {
                AlertDialog(
                    onDismissRequest = { showStoragePermissionDialog = false },
                    containerColor = InfinitySlateGray,
                    shape = RoundedCornerShape(16.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "Storage Access Required",
                            tint = InfinityCrimsonRed
                        )
                    },
                    title = {
                        Text(
                            text = "Storage Access Required",
                            style = MaterialTheme.typography.titleLarge,
                            color = InfinityCrispWhite
                        )
                    },
                    text = {
                        Text(
                            text = "Infinity PDF Viewer needs storage access to automatically scan, display, and organize your PDF files locally on your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InfinityMutedGray
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showStoragePermissionDialog = false
                                requestStoragePermission()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InfinityCrimsonRed,
                                contentColor = InfinityCrispWhite
                            ),
                            modifier = Modifier.testTag("storage_permission_grant_button")
                        ) {
                            Text("Grant Access")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showStoragePermissionDialog = false },
                            modifier = Modifier.testTag("storage_permission_later_button")
                        ) {
                            Text("Later", color = InfinityMutedGray)
                        }
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

            // Floating Status Toast Banner (#1E1E1E with 16dp corners, positioned above Liquid Glass Dock)
            AnimatedVisibility(
                visible = statusMessage != null || isBusy,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = if (activeDocument == null) 96.dp else 16.dp
                    )
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

/**
 * Zero-Lag 60/120fps Floating Liquid Glass Capsule Dock.
 *
 * Uses pre-allocated shaders inside [drawWithCache], hardware-accelerated translucent gradient layers,
 * a 1.2.dp 3D specular glass rim border, and GPU-phase [graphicsLayer] tab transitions without
 * expensive real-time RenderEffect blur passes.
 */
@Composable
fun LiquidGlassBottomDock(
    selectedTab: BottomNavTab,
    onSelectTab: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pre-allocate the 3D specular glass rim border brush once
    val specularRimBorderBrush = remember {
        Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = 0.55f),
                0.35f to Color.White.copy(alpha = 0.08f),
                0.68f to Color.White.copy(alpha = 0.08f),
                1.00f to Color.White.copy(alpha = 0.55f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .drawWithCache {
                val widthPx = size.width
                val heightPx = size.height
                val capsuleRadius = CornerRadius(heightPx / 2f, heightPx / 2f)

                // 1. Ambient Under-Glow Refraction Brushes (visible even over pure #000000 OLED black)
                val crimsonUnderGlowBrush = Brush.radialGradient(
                    colors = listOf(
                        InfinityCrimsonRed.copy(alpha = 0.18f),
                        InfinityCrimsonRed.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(widthPx * 0.50f, heightPx * 0.62f),
                    radius = (widthPx * 0.56f).coerceAtLeast(1f)
                )
                val coolSheenUnderGlowBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(widthPx * 0.24f, heightPx * 0.25f),
                    radius = (widthPx * 0.48f).coerceAtLeast(1f)
                )

                // 2. Frosted Liquid Glass Multi-Stop Linear Gradient Fill (top-left to bottom-right)
                val frostedGlassFillBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.14f),
                        0.52f to Color.White.copy(alpha = 0.05f),
                        1.00f to Color.White.copy(alpha = 0.11f)
                    ),
                    start = Offset.Zero,
                    end = Offset(widthPx, heightPx)
                )

                // 3. Internal Diagonal Specular Highlight Streak across the right side of the capsule
                val diagonalSpecularStreakBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.56f to Color.Transparent,
                        0.72f to Color.White.copy(alpha = 0.12f),
                        0.84f to Color.White.copy(alpha = 0.04f),
                        1.00f to Color.Transparent
                    ),
                    start = Offset(widthPx * 0.25f, 0f),
                    end = Offset(widthPx * 0.95f, heightPx)
                )

                // 4. Subtle top-edge curved meniscus sheen
                val topMeniscusBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = heightPx * 0.38f
                )

                onDrawBehind {
                    // Ambient under-glow halo extending softly around the curved stadium edges
                    val glowInflateX = 10.dp.toPx()
                    val glowInflateY = 8.dp.toPx()
                    val glowRadius = CornerRadius((heightPx / 2f) + glowInflateY, (heightPx / 2f) + glowInflateY)
                    drawRoundRect(
                        brush = crimsonUnderGlowBrush,
                        topLeft = Offset(-glowInflateX, -glowInflateY * 0.5f),
                        size = Size(widthPx + glowInflateX * 2f, heightPx + glowInflateY * 1.6f),
                        cornerRadius = glowRadius
                    )
                    drawRoundRect(
                        brush = coolSheenUnderGlowBrush,
                        topLeft = Offset(-glowInflateX * 0.5f, -glowInflateY * 0.8f),
                        size = Size(widthPx + glowInflateX, heightPx + glowInflateY * 1.2f),
                        cornerRadius = glowRadius
                    )

                    // Deep translucent smoky glass base so content scrolling behind stays legible
                    drawRoundRect(
                        color = LiquidGlassBaseTint,
                        cornerRadius = capsuleRadius
                    )

                    // Frosted liquid glass gradient fill
                    drawRoundRect(
                        brush = frostedGlassFillBrush,
                        cornerRadius = capsuleRadius
                    )

                    // Diagonal refraction highlight streak
                    drawRoundRect(
                        brush = diagonalSpecularStreakBrush,
                        cornerRadius = capsuleRadius
                    )

                    // Top meniscus specular reflection
                    drawRoundRect(
                        brush = topMeniscusBrush,
                        cornerRadius = capsuleRadius
                    )
                }
            }
            .clip(LiquidGlassCapsuleShape)
            .border(
                width = 1.2.dp,
                brush = specularRimBorderBrush,
                shape = LiquidGlassCapsuleShape
            )
            .testTag("infinity_bottom_nav")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                LiquidGlassDockTabItem(
                    tab = tab,
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassDockTabItem(
    tab: BottomNavTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquid_tab_scale_${tab.route}"
    )

    val animatedPillAlpha by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "liquid_tab_pill_alpha_${tab.route}"
    )

    val icon = when (tab) {
        BottomNavTab.LIBRARY -> if (selected) Icons.Filled.LibraryBooks else Icons.Outlined.LibraryBooks
        BottomNavTab.STUDIO_TOOLS -> if (selected) Icons.Filled.Construction else Icons.Outlined.Construction
        BottomNavTab.BOOKMARKS -> if (selected) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks
        BottomNavTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .drawWithCache {
                val pillRadius = CornerRadius(size.height / 2f, size.height / 2f)
                val borderStrokeWidthPx = 1.dp.toPx()

                // Pre-cached active liquid pill inner crimson glow & glass fill
                val crimsonInnerGlowBrush = Brush.radialGradient(
                    colors = listOf(
                        InfinityCrimsonRed.copy(alpha = 0.45f),
                        InfinityCrimsonRed.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.55f),
                    radius = (size.width * 0.65f).coerceAtLeast(1f)
                )

                val liquidPillGlassFill = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.10f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )

                val liquidPillBorderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.35f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )

                onDrawBehind {
                    val alpha = animatedPillAlpha
                    if (alpha > 0.005f) {
                        // Subtle Crimson Red inner glow inside the active liquid pill
                        drawRoundRect(
                            brush = crimsonInnerGlowBrush,
                            cornerRadius = pillRadius,
                            alpha = alpha
                        )
                        // Translucent liquid glass pill fill
                        drawRoundRect(
                            brush = liquidPillGlassFill,
                            cornerRadius = pillRadius,
                            alpha = alpha
                        )
                        // Specular 3D rim border around the active tab pill
                        drawRoundRect(
                            brush = liquidPillBorderBrush,
                            cornerRadius = pillRadius,
                            style = Stroke(width = borderStrokeWidthPx),
                            alpha = alpha
                        )
                    }
                }
            }
            .clip(LiquidGlassPillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                role = Role.Tab,
                onClick = onClick
            )
            .testTag("nav_tab_${tab.route}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tab.label,
                tint = if (selected) InfinityCrispWhite else InfinityCrispWhite.copy(alpha = 0.78f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tab.label,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) InfinityCrispWhite else InfinityMutedGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
