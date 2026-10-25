package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnnotationToolType
import com.example.data.NormalizedPoint
import com.example.data.PdfAnnotationEntity
import com.example.data.PdfBookmarkEntity
import com.example.data.PdfDocumentEntity
import com.example.pdf.NativePdfEngine
import com.example.ui.components.PdfThumbnailView
import com.example.ui.theme.InfinityCrimsonRed
import com.example.ui.theme.InfinityCrispWhite
import com.example.ui.theme.InfinityDividerGray
import com.example.ui.theme.InfinityMutedGray
import com.example.ui.theme.InfinityOledBlack
import com.example.ui.theme.InfinitySlateElevated
import com.example.ui.theme.InfinitySlateGray
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

private val INK_COLOR_PALETTE = listOf(
    0xFFED1C24L, // Crimson
    0xFFFFEA00L, // Highlighter Yellow
    0xFF00E5FFL, // Cyan
    0xFF00E676L, // Emerald
    0xFFD500F9L, // Violet
    0xFFFFFFFFL  // White
)

private val STAMP_PRESETS = listOf(
    "APPROVED",
    "CONFIDENTIAL",
    "REVIEWED",
    "VERIFIED",
    "DRAFT"
)

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    document: PdfDocumentEntity,
    currentPageIndex: Int,
    isOledNightMode: Boolean,
    isVerticalContinuous: Boolean,
    pageRotations: Map<Int, Int> = emptyMap(),
    isKeepScreenAwake: Boolean = false,
    activeTool: AnnotationToolType,
    activeInkColorHex: Long,
    activeStrokeWidth: Float,
    activeEraserSize: Float = 24f,
    activeStampLabel: String,
    documentAnnotations: List<PdfAnnotationEntity>,
    documentBookmarks: List<PdfBookmarkEntity>,
    onCloseReader: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleVerticalMode: () -> Unit,
    onRotatePage: (Int) -> Unit = {},
    onToggleKeepScreenAwake: () -> Unit = {},
    onSelectTool: (AnnotationToolType) -> Unit,
    onSelectInkColor: (Long) -> Unit,
    onSelectStrokeWidth: (Float) -> Unit,
    onSelectEraserSize: (Float) -> Unit = {},
    onSelectStampLabel: (String) -> Unit,
    onAddAnnotation: (Int, AnnotationToolType, List<NormalizedPoint>, String) -> Unit,
    onEraseNearPoint: (Int, NormalizedPoint) -> Unit,
    onUndoAnnotation: (Int) -> Unit,
    onClearPageAnnotations: (Int) -> Unit,
    onToggleBookmark: (Int, String, String) -> Unit,
    onSaveEdits: () -> Unit = {},
    onPrintDocument: () -> Unit = {},
    onShareDocument: () -> Unit,
    onExportPageAsImage: (Int) -> Unit = {},
    onReorderPages: (List<Int>, String) -> Unit = { _, _ -> },
    onEncryptedPdfDetected: () -> Unit = {}
) {
    BackHandler {
        onCloseReader()
    }

    val context = LocalContext.current
    DisposableEffect(isKeepScreenAwake) {
        val window = context.findHostActivity()?.window
        if (isKeepScreenAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val verticalListState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentPageIndex.coerceIn(0, (document.pageCount - 1).coerceAtLeast(0))
    )

    var zoomScale by remember(currentPageIndex) { mutableFloatStateOf(1f) }
    var panOffset by remember(currentPageIndex) { mutableStateOf(Offset.Zero) }

    var pendingNoteTapPoint by remember { mutableStateOf<NormalizedPoint?>(null) }
    var pendingNoteText by remember { mutableStateOf("") }

    var showPageOrganizerSheet by remember { mutableStateOf(false) }
    val organizerPageOrder = remember(document.id, document.pageCount) {
        mutableStateListOf<Int>().apply {
            addAll(0 until document.pageCount)
        }
    }
    var organizerOutputTitle by remember(document.id) {
        mutableStateOf("${document.title} (Reordered)")
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var isToolkitExpanded by remember { mutableStateOf(false) }
    var hasUnsavedEdits by remember(document.id) { mutableStateOf(false) }
    val animatedTitleSizeSp by animateFloatAsState(
        targetValue = if (hasUnsavedEdits) 13.0f else 15.5f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pdf_title_size"
    )

    val activePageRotation = pageRotations[currentPageIndex] ?: 0
    val isCurrentPageBookmarked = documentBookmarks.any { it.pageIndex == currentPageIndex }
    val currentPageAnnotations = remember(documentAnnotations, currentPageIndex) {
        documentAnnotations.filter { it.pageIndex == currentPageIndex }
    }

    // Keep currentPageIndex in sync when the user scrolls the continuous LazyColumn
    val mostVisiblePageInContinuous by remember {
        derivedStateOf {
            val layoutInfo = verticalListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                null
            } else {
                val viewportStart = layoutInfo.viewportStartOffset
                val viewportEnd = layoutInfo.viewportEndOffset
                visibleItems.maxByOrNull { item ->
                    val visibleTop = max(item.offset, viewportStart)
                    val visibleBottom = min(item.offset + item.size, viewportEnd)
                    (visibleBottom - visibleTop).coerceAtLeast(0)
                }?.index
            }
        }
    }

    LaunchedEffect(mostVisiblePageInContinuous, verticalListState.isScrollInProgress, isVerticalContinuous) {
        if (isVerticalContinuous && verticalListState.isScrollInProgress) {
            val targetPage = mostVisiblePageInContinuous
            if (targetPage != null && targetPage != currentPageIndex) {
                onSelectPage(targetPage)
            }
        }
    }

    // Helper to navigate to a page and scroll continuous LazyColumn if active
    val navigateToPage: (Int) -> Unit = { targetPage ->
        val safePage = targetPage.coerceIn(0, (document.pageCount - 1).coerceAtLeast(0))
        onSelectPage(safePage)
        if (isVerticalContinuous) {
            coroutineScope.launch {
                verticalListState.animateScrollToItem(safePage)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InfinityOledBlack)
    ) {
        // Compact Single-Line Top Studio Control Bar (#1E1E1E surface)
        Surface(
            color = InfinitySlateGray,
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCloseReader,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Library",
                        tint = InfinityCrispWhite
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .animateContentSize()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = animatedTitleSizeSp.sp
                        ),
                        color = InfinityCrispWhite,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Page ${currentPageIndex + 1} of ${document.pageCount} • ${(zoomScale * 100).toInt()}%" +
                            if (activePageRotation != 0) " • ${activePageRotation}°" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = InfinityMutedGray,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Smart Contrast Inverter (OLED Night Mode Reader Sun/Moon Toggle)
                IconButton(
                    onClick = onToggleNightMode,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("reader_night_mode_button")
                ) {
                    Icon(
                        imageVector = if (isOledNightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (isOledNightMode) "Disable OLED Contrast Inverter" else "Enable OLED Contrast Inverter",
                        tint = if (isOledNightMode) InfinityCrimsonRed else InfinityCrispWhite
                    )
                }

                // Granular Per-Page Rotation Toggle (0°, 90°, 180°, 270° for active page only)
                IconButton(
                    onClick = { onRotatePage(currentPageIndex) },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("reader_rotate_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Rotate Page ${currentPageIndex + 1} Clockwise ($activePageRotation°)",
                        tint = if (activePageRotation != 0) InfinityCrimsonRed else InfinityCrispWhite
                    )
                }

                // Bookmark Current Page Toggle
                IconButton(
                    onClick = {
                        onToggleBookmark(
                            currentPageIndex,
                            "${document.title} — Page ${currentPageIndex + 1}",
                            "Bookmarked in Reader"
                        )
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("reader_bookmark_button")
                ) {
                    Icon(
                        imageVector = if (isCurrentPageBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Bookmark Page",
                        tint = InfinityCrispWhite
                    )
                }

                // Smoothly appearing Save Button when PDF is edited
                androidx.compose.animation.AnimatedVisibility(
                    visible = hasUnsavedEdits,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.CenterHorizontally),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.CenterHorizontally)
                ) {
                    IconButton(
                        onClick = {
                            hasUnsavedEdits = false
                            isToolkitExpanded = false
                            onSaveEdits()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("reader_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save PDF Edits",
                            tint = InfinityCrispWhite
                        )
                    }
                }

                // Overflow Menu for Scroll Mode, Study Mode, Visual Page Organizer, Printing, High-Res Image Export & Share
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("reader_more_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More Reader Options",
                            tint = InfinityCrispWhite
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(InfinitySlateGray)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isVerticalContinuous) "Switch to Individual Page View" else "Switch to Continuous Scroll",
                                    color = InfinityCrispWhite
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.SwapVert,
                                    contentDescription = null,
                                    tint = InfinityCrispWhite
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onToggleVerticalMode()
                            },
                            modifier = Modifier.testTag("reader_scroll_mode_button")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isKeepScreenAwake) "Study Mode: Screen Awake (ON)" else "Study Mode: Keep Screen Awake",
                                    color = InfinityCrispWhite
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = if (isKeepScreenAwake) InfinityCrimsonRed else InfinityCrispWhite
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onToggleKeepScreenAwake()
                            },
                            modifier = Modifier.testTag("reader_keep_screen_awake_toggle")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Page Organizer (Reorder / Delete)",
                                    color = InfinityCrispWhite
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.GridView,
                                    contentDescription = null,
                                    tint = InfinityCrispWhite
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                organizerPageOrder.clear()
                                organizerPageOrder.addAll(0 until document.pageCount)
                                organizerOutputTitle = "${document.title} (Reordered)"
                                showPageOrganizerSheet = true
                            },
                            modifier = Modifier.testTag("reader_page_organizer_button")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Print PDF Document",
                                    color = InfinityCrispWhite
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Print,
                                    contentDescription = null,
                                    tint = InfinityCrispWhite
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onPrintDocument()
                            },
                            modifier = Modifier.testTag("reader_print_button")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Export Page as Image",
                                    color = InfinityCrispWhite
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = InfinityCrispWhite
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onExportPageAsImage(currentPageIndex)
                            },
                            modifier = Modifier.testTag("reader_export_page_image_button")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Share / Export Native PDF",
                                    color = InfinityCrispWhite
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null,
                                    tint = InfinityCrispWhite
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onShareDocument()
                            },
                            modifier = Modifier.testTag("reader_share_button")
                        )
                    }
                }
            }
        }

        // Main Full-Bleed PDF Viewport with Floating Editing Toolkit Overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(InfinityOledBlack),
            contentAlignment = Alignment.Center
        ) {
            if (isVerticalContinuous) {
                LazyColumn(
                    state = verticalListState,
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("vertical_pdf_pager")
                ) {
                    items(document.pageCount) { pageIdx ->
                        val pageAnns = documentAnnotations.filter { it.pageIndex == pageIdx }
                        val isCurrentZoomedPage = pageIdx == currentPageIndex
                        val pageRotation = (pageRotations[pageIdx] ?: 0).toFloat()
                        InteractivePdfPageCanvas(
                            filePath = document.filePath,
                            pageIndex = pageIdx,
                            invertForOledNight = isOledNightMode,
                            currentRotation = pageRotation,
                            activeTool = activeTool,
                            activeInkColorHex = activeInkColorHex,
                            activeStrokeWidth = activeStrokeWidth,
                            activeEraserSize = activeEraserSize,
                            activeStampLabel = activeStampLabel,
                            pageAnnotations = pageAnns,
                            zoomScale = if (isCurrentZoomedPage) zoomScale else 1f,
                            panOffset = if (isCurrentZoomedPage) panOffset else Offset.Zero,
                            fillViewport = false,
                            onTransformGesture = { panChange, zoomChange ->
                                isToolkitExpanded = false
                                if (currentPageIndex != pageIdx) {
                                    onSelectPage(pageIdx)
                                }
                                val newZoom = (zoomScale * zoomChange).coerceIn(1f, 4f)
                                zoomScale = newZoom
                                if (newZoom > 1.01f) {
                                    val maxOffset = 450f * (newZoom - 1f)
                                    panOffset = Offset(
                                        x = (panOffset.x + panChange.x).coerceIn(-maxOffset, maxOffset),
                                        y = (panOffset.y + panChange.y).coerceIn(-maxOffset, maxOffset)
                                    )
                                } else {
                                    panOffset = Offset.Zero
                                }
                            },
                            onDoubleTapZoom = { tapOffset, canvasSize ->
                                isToolkitExpanded = false
                                if (currentPageIndex != pageIdx) {
                                    onSelectPage(pageIdx)
                                }
                                if (zoomScale > 1.01f) {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    val targetZoom = 2.0f
                                    val centerX = canvasSize.width / 2f
                                    val centerY = canvasSize.height / 2f
                                    val maxOffset = 450f * (targetZoom - 1f)
                                    val targetPanX = ((centerX - tapOffset.x) * (targetZoom - 1f))
                                        .coerceIn(-maxOffset, maxOffset)
                                    val targetPanY = ((centerY - tapOffset.y) * (targetZoom - 1f))
                                        .coerceIn(-maxOffset, maxOffset)
                                    zoomScale = targetZoom
                                    panOffset = Offset(targetPanX, targetPanY)
                                }
                            },
                            onToolInteractionStart = {
                                isToolkitExpanded = false
                            },
                            onCommitAnnotation = { tool, pts, txt ->
                                isToolkitExpanded = false
                                hasUnsavedEdits = true
                                onSelectPage(pageIdx)
                                onAddAnnotation(pageIdx, tool, pts, txt)
                            },
                            onRequestTextNote = { normPt ->
                                isToolkitExpanded = false
                                onSelectPage(pageIdx)
                                pendingNoteTapPoint = normPt
                                pendingNoteText = ""
                            },
                            onEraseAt = { normPt ->
                                isToolkitExpanded = false
                                hasUnsavedEdits = true
                                onSelectPage(pageIdx)
                                onEraseNearPoint(pageIdx, normPt)
                            },
                            onEncryptedPdfDetected = onEncryptedPdfDetected
                        )
                    }
                }
            } else {
                val singlePageRotation = (pageRotations[currentPageIndex] ?: 0).toFloat()
                InteractivePdfPageCanvas(
                    filePath = document.filePath,
                    pageIndex = currentPageIndex,
                    invertForOledNight = isOledNightMode,
                    currentRotation = singlePageRotation,
                    activeTool = activeTool,
                    activeInkColorHex = activeInkColorHex,
                    activeStrokeWidth = activeStrokeWidth,
                    activeEraserSize = activeEraserSize,
                    activeStampLabel = activeStampLabel,
                    pageAnnotations = currentPageAnnotations,
                    zoomScale = zoomScale,
                    panOffset = panOffset,
                    fillViewport = true,
                    onTransformGesture = { panChange, zoomChange ->
                        isToolkitExpanded = false
                        val newZoom = (zoomScale * zoomChange).coerceIn(1f, 4f)
                        zoomScale = newZoom
                        if (newZoom > 1.01f) {
                            val maxOffset = 450f * (newZoom - 1f)
                            panOffset = Offset(
                                x = (panOffset.x + panChange.x).coerceIn(-maxOffset, maxOffset),
                                y = (panOffset.y + panChange.y).coerceIn(-maxOffset, maxOffset)
                            )
                        } else {
                            panOffset = Offset.Zero
                        }
                    },
                    onDoubleTapZoom = { tapOffset, canvasSize ->
                        isToolkitExpanded = false
                        if (zoomScale > 1.01f) {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        } else {
                            val targetZoom = 2.0f
                            val centerX = canvasSize.width / 2f
                            val centerY = canvasSize.height / 2f
                            val maxOffset = 450f * (targetZoom - 1f)
                            val targetPanX = ((centerX - tapOffset.x) * (targetZoom - 1f))
                                .coerceIn(-maxOffset, maxOffset)
                            val targetPanY = ((centerY - tapOffset.y) * (targetZoom - 1f))
                                .coerceIn(-maxOffset, maxOffset)
                            zoomScale = targetZoom
                            panOffset = Offset(targetPanX, targetPanY)
                        }
                    },
                    onToolInteractionStart = {
                        isToolkitExpanded = false
                    },
                    onCommitAnnotation = { tool, pts, txt ->
                        isToolkitExpanded = false
                        hasUnsavedEdits = true
                        onAddAnnotation(currentPageIndex, tool, pts, txt)
                    },
                    onRequestTextNote = { normPt ->
                        isToolkitExpanded = false
                        pendingNoteTapPoint = normPt
                        pendingNoteText = ""
                    },
                    onEraseAt = { normPt ->
                        isToolkitExpanded = false
                        hasUnsavedEdits = true
                        onEraseNearPoint(currentPageIndex, normPt)
                    },
                    onEncryptedPdfDetected = onEncryptedPdfDetected
                )
            }

            // Floating Editing & Annotation Toolkit Overlay
            // Stays open when selecting a tool; automatically hides itself when the user touches/clicks the PDF canvas
            androidx.compose.animation.AnimatedVisibility(
                visible = isToolkitExpanded,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Contextual Sub-Settings Card:
                    // - Hidden for Pan/Zoom (AnnotationToolType.VIEW)
                    // - Shows customizable Eraser Size Studio for Eraser (AnnotationToolType.ERASER)
                    // - Shows Ink / Stroke / Stamp / Note settings for drawing & markup tools
                    if (activeTool != AnnotationToolType.VIEW) {
                        Surface(
                            color = InfinitySlateGray.copy(alpha = 0.96f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                if (activeTool == AnnotationToolType.ERASER) {
                                    // Customizable Eraser Size Studio
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "ERASER SIZE ${activeEraserSize.toInt()}pt",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = InfinityCrispWhite
                                        )
                                        Slider(
                                            value = activeEraserSize,
                                            onValueChange = onSelectEraserSize,
                                            valueRange = 8f..64f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = InfinityCrispWhite,
                                                activeTrackColor = InfinityCrispWhite,
                                                inactiveTrackColor = InfinityDividerGray
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("eraser_size_slider")
                                        )
                                        Box(
                                            modifier = Modifier.size(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val previewDp = (activeEraserSize * 0.45f).coerceIn(6f, 28f).dp
                                            Box(
                                                modifier = Modifier
                                                    .size(previewDp)
                                                    .clip(CircleShape)
                                                    .background(InfinitySlateElevated)
                                                    .border(1.5.dp, InfinityCrispWhite, CircleShape)
                                            )
                                        }
                                    }
                                } else {
                                    // Color Palette Row for Ink, Highlight, Box, Stamp, Note
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = when (activeTool) {
                                                AnnotationToolType.STAMP -> "STAMP STUDIO"
                                                AnnotationToolType.TEXT_NOTE -> "NOTE COLOR"
                                                else -> "INK & STROKE STUDIO"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = InfinityMutedGray
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            INK_COLOR_PALETTE.forEach { hex ->
                                                val isSelected = hex == activeInkColorHex
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(hex))
                                                        .border(
                                                            width = if (isSelected) 2.5.dp else 1.dp,
                                                            color = if (isSelected) InfinityCrispWhite else InfinityDividerGray,
                                                            shape = CircleShape
                                                        )
                                                        .clickable { onSelectInkColor(hex) }
                                                        .testTag("ink_color_${hex.toString(16)}")
                                                )
                                            }
                                        }
                                    }

                                    if (activeTool == AnnotationToolType.STAMP) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(STAMP_PRESETS) { stamp ->
                                                val selected = stamp == activeStampLabel
                                                Surface(
                                                    onClick = { onSelectStampLabel(stamp) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (selected) InfinityCrimsonRed else InfinitySlateElevated,
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(horizontal = 12.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = stamp,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = InfinityCrispWhite
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else if (
                                        activeTool == AnnotationToolType.INK ||
                                        activeTool == AnnotationToolType.HIGHLIGHT ||
                                        activeTool == AnnotationToolType.RECTANGLE
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Width ${activeStrokeWidth.toInt()}pt",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = InfinityCrispWhite
                                            )
                                            Slider(
                                                value = activeStrokeWidth,
                                                onValueChange = onSelectStrokeWidth,
                                                valueRange = 2f..16f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = InfinityCrispWhite,
                                                    activeTrackColor = InfinityCrispWhite,
                                                    inactiveTrackColor = InfinityDividerGray
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Studio Annotation Tool Dock (#1E1E1E with 16dp rounded corners, Selected tool uses #ED1C24 pill)
                    Surface(
                        color = InfinitySlateGray.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                ToolPillButton(
                                    label = "Pan/Zoom",
                                    icon = Icons.Filled.PanTool,
                                    selected = activeTool == AnnotationToolType.VIEW,
                                    testTag = "tool_view",
                                    onClick = { onSelectTool(AnnotationToolType.VIEW) }
                                )
                            }
                            item {
                                ToolPillButton(
                                    label = "Ink Pen",
                                    icon = Icons.Filled.Draw,
                                    selected = activeTool == AnnotationToolType.INK,
                                    testTag = "tool_ink",
                                    onClick = { onSelectTool(AnnotationToolType.INK) }
                                )
                            }
                            item {
                                ToolPillButton(
                                    label = "Highlight",
                                    icon = Icons.Filled.BorderColor,
                                    selected = activeTool == AnnotationToolType.HIGHLIGHT,
                                    testTag = "tool_highlight",
                                    onClick = { onSelectTool(AnnotationToolType.HIGHLIGHT) }
                                )
                            }
                            item {
                                ToolPillButton(
                                    label = "Note",
                                    icon = Icons.Filled.StickyNote2,
                                    selected = activeTool == AnnotationToolType.TEXT_NOTE,
                                    testTag = "tool_note",
                                    onClick = { onSelectTool(AnnotationToolType.TEXT_NOTE) }
                                )
                            }
                            item {
                                ToolPillButton(
                                    label = "Stamp",
                                    icon = Icons.Filled.Verified,
                                    selected = activeTool == AnnotationToolType.STAMP,
                                    testTag = "tool_stamp",
                                    onClick = { onSelectTool(AnnotationToolType.STAMP) }
                                )
                            }
                            item {
                                ToolPillButton(
                                    label = "Box",
                                    icon = Icons.Filled.CropSquare,
                                    selected = activeTool == AnnotationToolType.RECTANGLE,
                                    testTag = "tool_rectangle",
                                    onClick = { onSelectTool(AnnotationToolType.RECTANGLE) }
                                )
                            }
                            item {
                                ToolPillButton(
                                    label = "Eraser",
                                    icon = Icons.Filled.CleaningServices,
                                    selected = activeTool == AnnotationToolType.ERASER,
                                    testTag = "tool_eraser",
                                    onClick = { onSelectTool(AnnotationToolType.ERASER) }
                                )
                            }
                            item {
                                IconButton(
                                    onClick = {
                                        hasUnsavedEdits = true
                                        onUndoAnnotation(currentPageIndex)
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("tool_undo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Undo Last Annotation",
                                        tint = InfinityCrispWhite
                                    )
                                }
                            }
                            item {
                                IconButton(
                                    onClick = {
                                        hasUnsavedEdits = true
                                        onClearPageAnnotations(currentPageIndex)
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("tool_clear_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteSweep,
                                        contentDescription = "Clear Page Annotations",
                                        tint = InfinityMutedGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Compact Bottom Page Scrubber & Floating Edit Toolkit Toggle (#1E1E1E)
        Surface(
            color = InfinitySlateGray,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentPageIndex > 0) navigateToPage(currentPageIndex - 1)
                    },
                    enabled = currentPageIndex > 0,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("reader_prev_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = "Previous Page",
                        tint = if (currentPageIndex > 0) InfinityCrispWhite else InfinityMutedGray
                    )
                }

                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(document.pageCount) { pageIdx ->
                        val isSelected = pageIdx == currentPageIndex
                        val scrubberPageRot = pageRotations[pageIdx] ?: 0
                        Surface(
                            onClick = { navigateToPage(pageIdx) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) InfinityCrimsonRed else InfinitySlateElevated,
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("page_scrubber_${pageIdx + 1}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                PdfThumbnailView(
                                    filePath = document.filePath,
                                    pageIndex = pageIdx,
                                    targetWidthPx = 120,
                                    invertForNight = isOledNightMode,
                                    cornerRadius = 3.dp,
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(28.dp)
                                        .graphicsLayer {
                                            rotationZ = scrubberPageRot.toFloat()
                                        }
                                )
                                Text(
                                    text = "P.${pageIdx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InfinityCrispWhite
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        if (currentPageIndex < document.pageCount - 1) {
                            navigateToPage(currentPageIndex + 1)
                        }
                    },
                    enabled = currentPageIndex < document.pageCount - 1,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("reader_next_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = "Next Page",
                        tint = if (currentPageIndex < document.pageCount - 1) InfinityCrispWhite else InfinityMutedGray
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Floating Editing Toolkit Toggle Pill in Page Scrubber Bar
                Surface(
                    onClick = {
                        isToolkitExpanded = !isToolkitExpanded
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isToolkitExpanded || activeTool != AnnotationToolType.VIEW) {
                        InfinityCrimsonRed
                    } else {
                        InfinitySlateElevated
                    },
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("toggle_toolkit_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isToolkitExpanded) {
                                Icons.Filled.KeyboardArrowDown
                            } else {
                                Icons.Filled.Draw
                            },
                            contentDescription = if (isToolkitExpanded) "Hide Edit Tools" else "Show Edit Tools",
                            tint = InfinityCrispWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isToolkitExpanded) "Hide" else "Tools",
                            style = MaterialTheme.typography.labelMedium,
                            color = InfinityCrispWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isToolkitExpanded) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = null,
                                tint = InfinityMutedGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Sticky Text Note Dialog
    pendingNoteTapPoint?.let { normPoint ->
        AlertDialog(
            onDismissRequest = { pendingNoteTapPoint = null },
            containerColor = InfinitySlateGray,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Add Page Callout Note",
                    style = MaterialTheme.typography.titleLarge,
                    color = InfinityCrispWhite
                )
            },
            text = {
                OutlinedTextField(
                    value = pendingNoteText,
                    onValueChange = { pendingNoteText = it },
                    placeholder = {
                        Text("Enter annotation note…", color = InfinityMutedGray)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = InfinityCrispWhite,
                        unfocusedTextColor = InfinityCrispWhite,
                        focusedBorderColor = InfinityCrispWhite,
                        unfocusedBorderColor = InfinityMutedGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reader_note_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pendingNoteText.isNotBlank()) {
                            hasUnsavedEdits = true
                            onAddAnnotation(
                                currentPageIndex,
                                AnnotationToolType.TEXT_NOTE,
                                listOf(normPoint),
                                pendingNoteText.trim()
                            )
                        }
                        pendingNoteTapPoint = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InfinitySlateElevated,
                        contentColor = InfinityCrispWhite
                    ),
                    modifier = Modifier.testTag("reader_note_confirm_button")
                ) {
                    Text("Place Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingNoteTapPoint = null }) {
                    Text("Cancel", color = InfinityMutedGray)
                }
            }
        )
    }

    // Visual Page Organizer Sheet (Reorder & Delete Pages via PdfThumbnailView Grid)
    if (showPageOrganizerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPageOrganizerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = InfinitySlateGray,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Visual Page Organizer",
                    style = MaterialTheme.typography.titleLarge,
                    color = InfinityCrispWhite
                )
                Text(
                    text = "${organizerPageOrder.size} surviving pages • Move pages up/down or delete unwanted pages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfinityMutedGray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = organizerOutputTitle,
                    onValueChange = { organizerOutputTitle = it },
                    label = { Text("Output PDF Title", color = InfinityMutedGray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = InfinityCrispWhite,
                        unfocusedTextColor = InfinityCrispWhite,
                        focusedBorderColor = InfinityCrispWhite,
                        unfocusedBorderColor = InfinityDividerGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reader_organizer_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .testTag("reader_page_organizer_grid")
                ) {
                    gridItemsIndexed(
                        items = organizerPageOrder,
                        key = { idx, origPage -> "org_${idx}_$origPage" }
                    ) { positionIdx, origPageIdx ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = InfinitySlateElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${positionIdx + 1} (Orig P.${origPageIdx + 1})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InfinityCrispWhite
                                    )
                                    IconButton(
                                        onClick = {
                                            if (organizerPageOrder.size > 1) {
                                                organizerPageOrder.removeAt(positionIdx)
                                            }
                                        },
                                        enabled = organizerPageOrder.size > 1,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("organizer_delete_page_$positionIdx")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete Page",
                                            tint = if (organizerPageOrder.size > 1) InfinityCrispWhite else InfinityMutedGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                PdfThumbnailView(
                                    filePath = document.filePath,
                                    pageIndex = origPageIdx,
                                    targetWidthPx = 280,
                                    invertForNight = isOledNightMode,
                                    cornerRadius = 10.dp,
                                    modifier = Modifier
                                        .width(96.dp)
                                        .height(128.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = {
                                            if (positionIdx > 0) {
                                                val item = organizerPageOrder.removeAt(positionIdx)
                                                organizerPageOrder.add(positionIdx - 1, item)
                                            }
                                        },
                                        enabled = positionIdx > 0,
                                        shape = RoundedCornerShape(10.dp),
                                        color = InfinitySlateGray,
                                        modifier = Modifier
                                            .height(34.dp)
                                            .weight(1f)
                                            .testTag("organizer_move_up_$positionIdx")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowUpward,
                                                contentDescription = "Move Page Up",
                                                tint = if (positionIdx > 0) InfinityCrispWhite else InfinityMutedGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        onClick = {
                                            if (positionIdx < organizerPageOrder.size - 1) {
                                                val item = organizerPageOrder.removeAt(positionIdx)
                                                organizerPageOrder.add(positionIdx + 1, item)
                                            }
                                        },
                                        enabled = positionIdx < organizerPageOrder.size - 1,
                                        shape = RoundedCornerShape(10.dp),
                                        color = InfinitySlateGray,
                                        modifier = Modifier
                                            .height(34.dp)
                                            .weight(1f)
                                            .testTag("organizer_move_down_$positionIdx")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowDownward,
                                                contentDescription = "Move Page Down",
                                                tint = if (positionIdx < organizerPageOrder.size - 1) InfinityCrispWhite else InfinityMutedGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val finalOrder = organizerPageOrder.toList()
                        showPageOrganizerSheet = false
                        onReorderPages(finalOrder, organizerOutputTitle)
                    },
                    enabled = organizerPageOrder.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InfinityCrimsonRed,
                        contentColor = InfinityCrispWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_page_organizer_button")
                ) {
                    Text("Save Reordered PDF (${organizerPageOrder.size} Pages)")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ToolPillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) InfinityCrimsonRed else InfinitySlateElevated,
        modifier = Modifier
            .height(44.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = InfinityCrispWhite,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = InfinityCrispWhite,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InteractivePdfPageCanvas(
    filePath: String,
    pageIndex: Int,
    invertForOledNight: Boolean,
    currentRotation: Float = 0f,
    activeTool: AnnotationToolType,
    activeInkColorHex: Long,
    activeStrokeWidth: Float,
    activeEraserSize: Float,
    activeStampLabel: String,
    pageAnnotations: List<PdfAnnotationEntity>,
    zoomScale: Float,
    panOffset: Offset,
    fillViewport: Boolean = true,
    onTransformGesture: (Offset, Float) -> Unit,
    onDoubleTapZoom: (Offset, Size) -> Unit,
    onToolInteractionStart: () -> Unit,
    onCommitAnnotation: (AnnotationToolType, List<NormalizedPoint>, String) -> Unit,
    onRequestTextNote: (NormalizedPoint) -> Unit,
    onEraseAt: (NormalizedPoint) -> Unit,
    onEncryptedPdfDetected: () -> Unit = {}
) {
    var pageBitmap by remember(filePath, pageIndex, invertForOledNight) {
        mutableStateOf<Bitmap?>(null)
    }
    var isLoading by remember(filePath, pageIndex, invertForOledNight) {
        mutableStateOf(true)
    }

    val currentOnTransformGesture by rememberUpdatedState(onTransformGesture)
    val currentOnDoubleTapZoom by rememberUpdatedState(onDoubleTapZoom)
    val currentOnToolInteractionStart by rememberUpdatedState(onToolInteractionStart)
    val currentOnCommitAnnotation by rememberUpdatedState(onCommitAnnotation)
    val currentOnRequestTextNote by rememberUpdatedState(onRequestTextNote)
    val currentOnEraseAt by rememberUpdatedState(onEraseAt)
    val currentOnEncryptedDetected by rememberUpdatedState(onEncryptedPdfDetected)
    val currentActiveTool by rememberUpdatedState(activeTool)
    val currentStampLabel by rememberUpdatedState(activeStampLabel)
    val currentZoomScale by rememberUpdatedState(zoomScale)

    val animatedZoom by animateFloatAsState(
        targetValue = zoomScale,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pdf_zoom_scale"
    )
    val animatedPan by animateOffsetAsState(
        targetValue = panOffset,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pdf_pan_offset"
    )
    val animatedRotation by animateFloatAsState(
        targetValue = currentRotation,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pdf_rotation_degrees_$pageIndex"
    )

    val liveDragPoints = remember { mutableStateListOf<NormalizedPoint>() }
    var liveEraserPoint by remember { mutableStateOf<NormalizedPoint?>(null) }

    LaunchedEffect(filePath, pageIndex, invertForOledNight) {
        isLoading = true
        val rendered = NativePdfEngine.renderPage(
            filePath = filePath,
            pageIndex = pageIndex,
            targetWidthPx = 1600,
            invertForOledNight = invertForOledNight
        )
        pageBitmap = rendered
        isLoading = false
        if (rendered == null && NativePdfEngine.isPdfPasswordProtected(filePath)) {
            currentOnEncryptedDetected()
        }
    }

    BoxWithConstraints(
        modifier = if (fillViewport) {
            Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 2.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp)
        },
        contentAlignment = Alignment.Center
    ) {
        val bmp = pageBitmap
        if (bmp == null || bmp.isRecycled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (fillViewport) maxHeight else 520.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(InfinitySlateGray),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = InfinityCrispWhite)
                } else {
                    Text(
                        text = "Unable to render Page ${pageIndex + 1}",
                        color = InfinityMutedGray
                    )
                }
            }
        } else {
            val aspectRatio = bmp.height.toFloat() / bmp.width.toFloat().coerceAtLeast(1f)
            val cardWidth = maxWidth
            val cardHeight = if (fillViewport) {
                maxHeight
            } else {
                cardWidth * aspectRatio
            }

            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .clip(RoundedCornerShape(6.dp))
                    .pointerInput(pageIndex, activeTool) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                currentOnToolInteractionStart()
                                currentOnDoubleTapZoom(
                                    tapOffset,
                                    Size(size.width.toFloat(), size.height.toFloat())
                                )
                            },
                            onTap = { tapOffset ->
                                // Any tap on the PDF screen hides the floating toolkit popup
                                currentOnToolInteractionStart()
                                val norm = NormalizedPoint(
                                    x = (tapOffset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f),
                                    y = (tapOffset.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                )
                                when (currentActiveTool) {
                                    AnnotationToolType.STAMP -> {
                                        currentOnCommitAnnotation(
                                            AnnotationToolType.STAMP,
                                            listOf(norm),
                                            currentStampLabel
                                        )
                                    }
                                    AnnotationToolType.TEXT_NOTE -> {
                                        currentOnRequestTextNote(norm)
                                    }
                                    AnnotationToolType.ERASER -> {
                                        currentOnEraseAt(norm)
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                    .pointerInput(pageIndex, activeTool) {
                        if (activeTool == AnnotationToolType.VIEW) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                currentOnToolInteractionStart()
                                do {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount >= 2 || currentZoomScale > 1.01f) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (abs(zoomChange - 1f) > 0.001f || panChange != Offset.Zero) {
                                            currentOnTransformGesture(panChange, zoomChange)
                                            event.changes.forEach { change ->
                                                if (change.positionChanged()) {
                                                    change.consume()
                                                }
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                    }
                    .pointerInput(activeTool, pageIndex, activeInkColorHex, activeStrokeWidth, activeEraserSize) {
                        if (
                            activeTool == AnnotationToolType.INK ||
                            activeTool == AnnotationToolType.HIGHLIGHT ||
                            activeTool == AnnotationToolType.RECTANGLE ||
                            activeTool == AnnotationToolType.ERASER
                        ) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    currentOnToolInteractionStart()
                                    liveDragPoints.clear()
                                    val pt = NormalizedPoint(
                                        x = (startOffset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f),
                                        y = (startOffset.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                    )
                                    if (currentActiveTool == AnnotationToolType.ERASER) {
                                        liveEraserPoint = pt
                                        currentOnEraseAt(pt)
                                    } else {
                                        liveDragPoints.add(pt)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val pt = NormalizedPoint(
                                        x = (change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f),
                                        y = (change.position.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                    )
                                    if (currentActiveTool == AnnotationToolType.ERASER) {
                                        liveEraserPoint = pt
                                        currentOnEraseAt(pt)
                                    } else {
                                        liveDragPoints.add(pt)
                                    }
                                },
                                onDragEnd = {
                                    if (liveDragPoints.size >= 2 && currentActiveTool != AnnotationToolType.ERASER) {
                                        currentOnCommitAnnotation(
                                            currentActiveTool,
                                            liveDragPoints.toList(),
                                            ""
                                        )
                                    }
                                    liveDragPoints.clear()
                                    liveEraserPoint = null
                                },
                                onDragCancel = {
                                    liveDragPoints.clear()
                                    liveEraserPoint = null
                                }
                            )
                        }
                    }
                    .graphicsLayer {
                        scaleX = animatedZoom
                        scaleY = animatedZoom
                        translationX = animatedPan.x
                        translationY = animatedPan.y
                        rotationZ = animatedRotation
                    }
                    .testTag("pdf_page_canvas_${pageIndex + 1}")
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Rendered PDF Page ${pageIndex + 1}",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // Vector Annotation Overlay Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw persisted annotations
                    pageAnnotations.forEach { ann ->
                        val pts = NativePdfEngine.decodePoints(ann.pointsJson)
                        drawAnnotationItem(
                            toolType = ann.toolType,
                            points = pts,
                            color = Color(ann.colorHex),
                            strokeWidth = ann.strokeWidth,
                            textContent = ann.textContent
                        )
                    }

                    // Draw live in-progress stroke
                    if (liveDragPoints.size >= 2) {
                        drawAnnotationItem(
                            toolType = activeTool.name,
                            points = liveDragPoints,
                            color = Color(activeInkColorHex),
                            strokeWidth = activeStrokeWidth,
                            textContent = ""
                        )
                    }

                    // Draw live customizable eraser radius indicator while erasing
                    liveEraserPoint?.let { ep ->
                        val eraserRadiusPx = (activeEraserSize / 360f).coerceIn(0.025f, 0.22f) * size.width
                        val center = Offset(ep.x * size.width, ep.y * size.height)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.22f),
                            radius = eraserRadiusPx,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = eraserRadiusPx,
                            center = center,
                            style = Stroke(width = 2.5f)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawAnnotationItem(
    toolType: String,
    points: List<NormalizedPoint>,
    color: Color,
    strokeWidth: Float,
    textContent: String
) {
    val w = size.width
    val h = size.height
    when (toolType) {
        AnnotationToolType.INK.name -> {
            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points.first().x * w, points.first().y * h)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x * w, points[i].y * h)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = strokeWidth * 1.6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        AnnotationToolType.HIGHLIGHT.name -> {
            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points.first().x * w, points.first().y * h)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x * w, points[i].y * h)
                    }
                }
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.36f),
                    style = Stroke(
                        width = strokeWidth * 4.2f,
                        cap = StrokeCap.Square,
                        join = StrokeJoin.Bevel
                    )
                )
            }
        }
        AnnotationToolType.RECTANGLE.name -> {
            if (points.size >= 2) {
                val p1 = points.first()
                val p2 = points.last()
                val left = min(p1.x, p2.x) * w
                val top = min(p1.y, p2.y) * h
                val right = max(p1.x, p2.x) * w
                val bottom = max(p1.y, p2.y) * h
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size((right - left).coerceAtLeast(4f), (bottom - top).coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = strokeWidth * 1.5f)
                )
            }
        }
        AnnotationToolType.STAMP.name -> {
            val anchor = points.firstOrNull() ?: return
            val cx = anchor.x * w
            val cy = anchor.y * h
            val label = textContent.ifBlank { "APPROVED" }
            val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                this.color = android.graphics.Color.argb(
                    255,
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                )
                textSize = 30f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = AndroidPaint.Align.CENTER
            }
            val textWidth = textPaint.measureText(label)
            val boxW = textWidth + 36f
            val boxH = 48f
            drawRoundRect(
                color = Color(0xCC1E1E1E),
                topLeft = Offset(cx - boxW / 2f, cy - boxH / 2f),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - boxW / 2f, cy - boxH / 2f),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 4f)
            )
            drawContext.canvas.nativeCanvas.drawText(label, cx, cy + 10f, textPaint)
        }
        AnnotationToolType.TEXT_NOTE.name -> {
            val anchor = points.firstOrNull() ?: return
            val x = (anchor.x * w).coerceIn(12f, (w - 220f).coerceAtLeast(12f))
            val y = (anchor.y * h).coerceIn(12f, (h - 60f).coerceAtLeast(12f))
            val noteText = textContent.ifBlank { "Note" }
            val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                this.color = android.graphics.Color.WHITE
                textSize = 24f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val measuredW = (textPaint.measureText(noteText) + 32f).coerceIn(100f, w * 0.7f)
            val boxH = 44f
            drawRoundRect(
                color = Color(0xEE1E1E1E),
                topLeft = Offset(x, y),
                size = Size(measuredW, boxH),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(measuredW, boxH),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 3f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                noteText.take(36),
                x + 14f,
                y + 30f,
                textPaint
            )
        }
    }
}
