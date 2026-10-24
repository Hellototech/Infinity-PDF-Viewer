package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.pdf.NativePdfEngine
import com.example.ui.theme.InfinityCrimsonRed
import com.example.ui.theme.InfinityCrispWhite
import com.example.ui.theme.InfinityDividerGray
import com.example.ui.theme.InfinityMutedGray
import com.example.ui.theme.InfinitySlateElevated
import com.example.ui.theme.InfinitySlateGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerModalSheet(
    onDismiss: () -> Unit,
    onCompileScannedPages: (List<Bitmap>, String, NativePdfEngine.ScanFilterMode) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultTimestamp = remember {
        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())
    }
    var scanTitle by remember {
        mutableStateOf("Scanned Document ($defaultTimestamp)")
    }
    var selectedFilter by remember {
        mutableStateOf(NativePdfEngine.ScanFilterMode.MAGIC_COLOR)
    }

    val capturedPages = remember { mutableStateListOf<Bitmap>() }
    var activePreviewIndex by remember { mutableIntStateOf(0) }
    var presetCounter by remember { mutableIntStateOf(0) }

    // Seed an initial physical document scan page so the studio is immediately ready
    LaunchedEffect(Unit) {
        if (capturedPages.isEmpty()) {
            val initialPage = withContext(Dispatchers.Default) {
                NativePdfEngine.generatePhysicalScanPresetBitmap(
                    presetIndex = 0,
                    pageNumberOneBased = 1,
                    filterMode = NativePdfEngine.ScanFilterMode.ORIGINAL,
                    rotationDegrees = 0
                )
            }
            capturedPages.add(initialPage)
            presetCounter = 1
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedPages.add(bitmap)
            activePreviewIndex = capturedPages.lastIndex
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        }.getOrNull()
                    }
                }
                if (decoded.isNotEmpty()) {
                    capturedPages.addAll(decoded)
                    activePreviewIndex = capturedPages.lastIndex
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = InfinitySlateGray,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("document_scanner_modal")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DocumentScanner,
                    contentDescription = "Physical Document Scanner",
                    tint = InfinityCrimsonRed
                )
                Column {
                    Text(
                        text = "Physical Document Scanner",
                        style = MaterialTheme.typography.titleLarge,
                        color = InfinityCrispWhite
                    )
                    Text(
                        text = "${capturedPages.size} Page(s) Queued • Auto-Crop & Contrast Filter",
                        style = MaterialTheme.typography.bodySmall,
                        color = InfinityMutedGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = scanTitle,
                onValueChange = { scanTitle = it },
                label = { Text("Output Scanned PDF Title", color = InfinityMutedGray) },
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
                    .testTag("scanner_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Capture / Import Action Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { cameraLauncher.launch(null) },
                    shape = RoundedCornerShape(12.dp),
                    color = InfinitySlateElevated,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("scanner_camera_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddAPhoto,
                            contentDescription = "Camera Capture",
                            tint = InfinityCrispWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Camera",
                            style = MaterialTheme.typography.labelMedium,
                            color = InfinityCrispWhite
                        )
                    }
                }

                Surface(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = InfinitySlateElevated,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("scanner_gallery_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = "Gallery Import",
                            tint = InfinityCrispWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gallery",
                            style = MaterialTheme.typography.labelMedium,
                            color = InfinityCrispWhite
                        )
                    }
                }

                Surface(
                    onClick = {
                        coroutineScope.launch {
                            val nextPageNum = capturedPages.size + 1
                            val nextPreset = presetCounter++
                            val pageBmp = withContext(Dispatchers.Default) {
                                NativePdfEngine.generatePhysicalScanPresetBitmap(
                                    presetIndex = nextPreset,
                                    pageNumberOneBased = nextPageNum,
                                    filterMode = NativePdfEngine.ScanFilterMode.ORIGINAL,
                                    rotationDegrees = 0
                                )
                            }
                            capturedPages.add(pageBmp)
                            activePreviewIndex = capturedPages.lastIndex
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = InfinitySlateElevated,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(44.dp)
                        .testTag("scanner_add_page_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DocumentScanner,
                            contentDescription = "Scan Next Page",
                            tint = InfinityCrimsonRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ Scan Page",
                            style = MaterialTheme.typography.labelMedium,
                            color = InfinityCrispWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Mode Strip
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NativePdfEngine.ScanFilterMode.entries) { mode ->
                    val selected = mode == selectedFilter
                    Surface(
                        onClick = { selectedFilter = mode },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) InfinityCrimsonRed else InfinitySlateElevated,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("scanner_filter_${mode.name.lowercase()}")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = InfinityCrispWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Page Preview & Page Strip
            val safeIdx = activePreviewIndex.coerceIn(0, (capturedPages.size - 1).coerceAtLeast(0))
            val activeRawBmp = capturedPages.getOrNull(safeIdx)
            val previewBmp = remember(activeRawBmp, selectedFilter) {
                if (activeRawBmp != null && !activeRawBmp.isRecycled) {
                    NativePdfEngine.applyScannerFilterToBitmap(activeRawBmp, selectedFilter, 0)
                } else {
                    null
                }
            }

            if (previewBmp != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = InfinitySlateElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            bitmap = previewBmp.asImageBitmap(),
                            contentDescription = "Scanned Page ${safeIdx + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(104.dp)
                                .height(144.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, InfinityDividerGray, RoundedCornerShape(10.dp))
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Page ${safeIdx + 1} of ${capturedPages.size}",
                                style = MaterialTheme.typography.titleMedium,
                                color = InfinityCrispWhite
                            )
                            Text(
                                text = "Filter: ${selectedFilter.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InfinityMutedGray
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    onClick = {
                                        val current = capturedPages.getOrNull(safeIdx) ?: return@Surface
                                        val rotated = NativePdfEngine.applyScannerFilterToBitmap(
                                            current,
                                            NativePdfEngine.ScanFilterMode.ORIGINAL,
                                            90
                                        )
                                        capturedPages[safeIdx] = rotated
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = InfinitySlateGray,
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("scanner_rotate_page_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                            contentDescription = "Rotate 90°",
                                            tint = InfinityCrispWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Rotate 90°",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InfinityCrispWhite
                                        )
                                    }
                                }

                                if (capturedPages.size > 1) {
                                    IconButton(
                                        onClick = {
                                            capturedPages.removeAt(safeIdx)
                                            activePreviewIndex = activePreviewIndex.coerceIn(
                                                0,
                                                (capturedPages.size - 1).coerceAtLeast(0)
                                            )
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Remove Scanned Page",
                                            tint = InfinityCrispWhite
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                itemsIndexed(capturedPages) { idx, _ ->
                                    val isCurrent = idx == safeIdx
                                    Surface(
                                        onClick = { activePreviewIndex = idx },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) InfinityCrimsonRed else InfinitySlateGray,
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "P.${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = InfinityCrispWhite
                                            )
                                        }
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
                    val pagesToCompile = capturedPages.toList()
                    onCompileScannedPages(pagesToCompile, scanTitle, selectedFilter)
                },
                enabled = capturedPages.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InfinityCrimsonRed,
                    contentColor = InfinityCrispWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_scan_pdf_button")
            ) {
                Text("Save Scanned PDF (${capturedPages.size} Page${if (capturedPages.size == 1) "" else "s"}) & Open")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
