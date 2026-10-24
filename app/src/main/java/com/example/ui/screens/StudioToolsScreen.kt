package com.example.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PdfDocumentEntity
import com.example.ui.components.PdfThumbnailView
import com.example.ui.theme.InfinityCrimsonRed
import com.example.ui.theme.InfinityCrispWhite
import com.example.ui.theme.InfinityDividerGray
import com.example.ui.theme.InfinityMutedGray
import com.example.ui.theme.InfinityOledBlack
import com.example.ui.theme.InfinitySlateElevated
import com.example.ui.theme.InfinitySlateGray

private enum class StudioDialogMode {
    NONE,
    PAGE_ORGANIZER,
    WATERMARK,
    MERGE,
    SPLIT
}

private data class StudioToolCardSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector
)

private data class WatermarkColorOption(
    val label: String,
    val argb: Int
)

private val WATERMARK_COLOR_OPTIONS = listOf(
    WatermarkColorOption("Crimson", AndroidColor.rgb(237, 28, 36)),
    WatermarkColorOption("White", AndroidColor.rgb(255, 255, 255)),
    WatermarkColorOption("Slate", AndroidColor.rgb(90, 90, 95))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioToolsScreen(
    documents: List<PdfDocumentEntity>,
    onScanDocumentClick: () -> Unit,
    onReorderPages: (PdfDocumentEntity, List<Int>, String) -> Unit,
    onApplyWatermarkAndNumbering: (
        sourceDoc: PdfDocumentEntity,
        watermarkText: String,
        watermarkColorArgb: Int,
        opacityFraction: Float,
        isDiagonal: Boolean,
        includePageNumbers: Boolean,
        pageNumberPosition: String,
        pageNumberFormat: String,
        outputTitle: String
    ) -> Unit,
    onMergeDocuments: (List<PdfDocumentEntity>, String) -> Unit,
    onSplitDocument: (PdfDocumentEntity, Int, Int, String) -> Unit
) {
    var activeModal by remember { mutableStateOf(StudioDialogMode.NONE) }

    // Page Organizer state
    var selectedOrganizerDoc by remember(documents) { mutableStateOf(documents.firstOrNull()) }
    val organizerPageOrder = remember { mutableStateListOf<Int>() }
    var organizerOutputTitle by remember { mutableStateOf("") }

    // Watermark & Numbering state
    var selectedWatermarkDoc by remember(documents) { mutableStateOf(documents.firstOrNull()) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var watermarkColorOption by remember { mutableStateOf(WATERMARK_COLOR_OPTIONS.first()) }
    var watermarkOpacityFraction by remember { mutableFloatStateOf(0.25f) }
    var watermarkIsDiagonal by remember { mutableStateOf(true) }
    var includePageNumbers by remember { mutableStateOf(true) }
    var pageNumberPosition by remember { mutableStateOf("BOTTOM_CENTER") } // BOTTOM_CENTER, BOTTOM_RIGHT, TOP_RIGHT
    var pageNumberFormat by remember { mutableStateOf("PAGE_X_OF_Y") }     // PAGE_X_OF_Y, X_SLASH_Y
    var watermarkOutputTitle by remember { mutableStateOf("") }

    // Merge state
    val selectedMergeDocIds = remember { mutableStateListOf<Long>() }
    var mergedTitleInput by remember { mutableStateOf("Combined Studio Packet") }

    // Split state
    var selectedSplitDoc by remember(documents) { mutableStateOf(documents.firstOrNull()) }
    var splitStartPage by remember { mutableIntStateOf(1) }
    var splitEndPage by remember { mutableIntStateOf(2) }
    var splitTitleInput by remember { mutableStateOf("") }

    val tools = remember {
        listOf(
            StudioToolCardSpec(
                id = "tool_card_scan_doc",
                title = "Scan Document",
                subtitle = "Scan physical pages via ML Kit Document Scanner into a local PDF.",
                badge = "ML KIT SCAN",
                icon = Icons.Filled.DocumentScanner
            ),
            StudioToolCardSpec(
                id = "tool_card_page_organizer",
                title = "Page Organizer",
                subtitle = "Visual thumbnail grid to reorder pages up/down or delete unwanted pages.",
                badge = "REORDER",
                icon = Icons.Filled.GridView
            ),
            StudioToolCardSpec(
                id = "tool_card_watermark",
                title = "Watermark & Numbering",
                subtitle = "Stamp diagonal/horizontal watermarks and formatted page numbers.",
                badge = "WATERMARK",
                icon = Icons.Filled.BrandingWatermark
            ),
            StudioToolCardSpec(
                id = "tool_card_merge",
                title = "Merge PDFs",
                subtitle = "Combine multiple library PDFs into one unified native document.",
                badge = "MULTI-DOC",
                icon = Icons.Filled.CallMerge
            ),
            StudioToolCardSpec(
                id = "tool_card_split",
                title = "Split & Extract",
                subtitle = "Extract custom page intervals into a new standalone PDF.",
                badge = "PAGE RANGE",
                icon = Icons.Filled.CallSplit
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InfinityOledBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "PDF STUDIO TOOLS",
            style = MaterialTheme.typography.headlineLarge,
            color = InfinityCrispWhite
        )
        Text(
            text = "100% Local Native PdfRenderer, PdfDocument & ML Kit Scanner Suite",
            style = MaterialTheme.typography.bodyMedium,
            color = InfinityMutedGray
        )

        Spacer(modifier = Modifier.height(18.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("studio_tools_grid")
        ) {
            items(tools, key = { it.id }) { tool ->
                Surface(
                    onClick = {
                        when (tool.id) {
                            "tool_card_scan_doc" -> {
                                onScanDocumentClick()
                            }
                            "tool_card_page_organizer" -> {
                                val firstDoc = documents.firstOrNull()
                                selectedOrganizerDoc = firstDoc
                                organizerPageOrder.clear()
                                if (firstDoc != null) {
                                    organizerPageOrder.addAll(0 until firstDoc.pageCount)
                                    organizerOutputTitle = "${firstDoc.title} (Reordered)"
                                }
                                activeModal = StudioDialogMode.PAGE_ORGANIZER
                            }
                            "tool_card_watermark" -> {
                                val firstDoc = documents.firstOrNull()
                                selectedWatermarkDoc = firstDoc
                                watermarkOutputTitle = "${firstDoc?.title ?: "Document"} (Watermarked)"
                                activeModal = StudioDialogMode.WATERMARK
                            }
                            "tool_card_merge" -> {
                                selectedMergeDocIds.clear()
                                documents.take(2).forEach { selectedMergeDocIds.add(it.id) }
                                activeModal = StudioDialogMode.MERGE
                            }
                            "tool_card_split" -> {
                                selectedSplitDoc = documents.firstOrNull()
                                splitStartPage = 1
                                splitEndPage = (selectedSplitDoc?.pageCount ?: 2).coerceAtLeast(1)
                                splitTitleInput = "${selectedSplitDoc?.title ?: "Document"} (Excerpt)"
                                activeModal = StudioDialogMode.SPLIT
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = InfinitySlateGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(tool.id)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Primary tool grid icon uses Crimson Red (#ED1C24) per Strict Color Law
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(InfinitySlateElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = tool.title,
                                    tint = InfinityCrimsonRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = InfinitySlateElevated
                            ) {
                                Text(
                                    text = tool.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InfinityMutedGray,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = InfinityCrispWhite
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = tool.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = InfinityMutedGray
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets for Studio Operations (#1E1E1E with 16dp rounded corners)
    if (activeModal != StudioDialogMode.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeModal = StudioDialogMode.NONE },
            sheetState = sheetState,
            containerColor = InfinitySlateGray,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            when (activeModal) {
                StudioDialogMode.PAGE_ORGANIZER -> {
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
                            text = "Select a document, reorder pages up/down, or delete unwanted pages.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InfinityMutedGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Document selector strip
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(documents, key = { it.id }) { doc ->
                                val isSelected = selectedOrganizerDoc?.id == doc.id
                                Surface(
                                    onClick = {
                                        selectedOrganizerDoc = doc
                                        organizerPageOrder.clear()
                                        organizerPageOrder.addAll(0 until doc.pageCount)
                                        organizerOutputTitle = "${doc.title} (Reordered)"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) InfinityCrimsonRed else InfinitySlateElevated
                                ) {
                                    Text(
                                        text = "${doc.title} (${doc.pageCount}P)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = InfinityCrispWhite,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
                                .testTag("studio_organizer_title_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val targetDoc = selectedOrganizerDoc
                        if (targetDoc != null) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .testTag("studio_page_organizer_grid")
                            ) {
                                gridItemsIndexed(
                                    items = organizerPageOrder,
                                    key = { idx, origPage -> "studio_org_${idx}_$origPage" }
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
                                                    text = "#${positionIdx + 1} (P.${origPageIdx + 1})",
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
                                                        .testTag("studio_organizer_delete_$positionIdx")
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
                                                filePath = targetDoc.filePath,
                                                pageIndex = origPageIdx,
                                                targetWidthPx = 280,
                                                cornerRadius = 10.dp,
                                                modifier = Modifier
                                                    .width(92.dp)
                                                    .height(122.dp)
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
                                                        .testTag("studio_organizer_up_$positionIdx")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Filled.ArrowUpward,
                                                            contentDescription = "Move Up",
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
                                                        .testTag("studio_organizer_down_$positionIdx")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Filled.ArrowDownward,
                                                            contentDescription = "Move Down",
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
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val doc = selectedOrganizerDoc ?: return@Button
                                val finalOrder = organizerPageOrder.toList()
                                activeModal = StudioDialogMode.NONE
                                onReorderPages(doc, finalOrder, organizerOutputTitle)
                            },
                            enabled = selectedOrganizerDoc != null && organizerPageOrder.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InfinityCrimsonRed,
                                contentColor = InfinityCrispWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_studio_organizer_button")
                        ) {
                            Text("Save Reordered PDF (${organizerPageOrder.size} Pages)")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                StudioDialogMode.WATERMARK -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Multi-Page Watermark & Numbering",
                            style = MaterialTheme.typography.titleLarge,
                            color = InfinityCrispWhite
                        )
                        Text(
                            text = "Overlay custom watermarks and page numbers across all pages natively.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InfinityMutedGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Document selector strip
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(documents, key = { it.id }) { doc ->
                                val isSelected = selectedWatermarkDoc?.id == doc.id
                                Surface(
                                    onClick = {
                                        selectedWatermarkDoc = doc
                                        watermarkOutputTitle = "${doc.title} (Watermarked)"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) InfinityCrimsonRed else InfinitySlateElevated
                                ) {
                                    Text(
                                        text = "${doc.title} (${doc.pageCount}P)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = InfinityCrispWhite,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Watermark Text + Quick Presets
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            label = { Text("Watermark Text (e.g. CONFIDENTIAL, DRAFT)", color = InfinityMutedGray) },
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
                                .testTag("watermark_text_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("CONFIDENTIAL", "DRAFT", "APPROVED", "DO NOT COPY").forEach { preset ->
                                Surface(
                                    onClick = { watermarkText = preset },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (watermarkText == preset) InfinityCrimsonRed else InfinitySlateElevated
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InfinityCrispWhite,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Color Selection (Crimson, White, Slate) + Alignment (Diagonal / Horizontal)
                        Text(
                            text = "WATERMARK COLOR & ORIENTATION",
                            style = MaterialTheme.typography.labelMedium,
                            color = InfinityMutedGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WATERMARK_COLOR_OPTIONS.forEach { option ->
                                val selected = watermarkColorOption.label == option.label
                                Surface(
                                    onClick = { watermarkColorOption = option },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) InfinityCrimsonRed else InfinitySlateElevated,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("watermark_color_${option.label.lowercase()}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = InfinityCrispWhite
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { watermarkIsDiagonal = true },
                                shape = RoundedCornerShape(12.dp),
                                color = if (watermarkIsDiagonal) InfinityCrimsonRed else InfinitySlateElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("watermark_align_diagonal")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Diagonal (-45°)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = InfinityCrispWhite
                                    )
                                }
                            }
                            Surface(
                                onClick = { watermarkIsDiagonal = false },
                                shape = RoundedCornerShape(12.dp),
                                color = if (!watermarkIsDiagonal) InfinityCrimsonRed else InfinitySlateElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("watermark_align_horizontal")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Horizontal (0°)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = InfinityCrispWhite
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Opacity Slider (10% to 50%)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Opacity ${(watermarkOpacityFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = InfinityCrispWhite
                            )
                            Slider(
                                value = watermarkOpacityFraction,
                                onValueChange = { watermarkOpacityFraction = it },
                                valueRange = 0.10f..0.50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = InfinityCrispWhite,
                                    activeTrackColor = InfinityCrispWhite,
                                    inactiveTrackColor = InfinityDividerGray
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("watermark_opacity_slider")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Page Numbering Suite
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Page Numbering",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InfinityCrispWhite
                                )
                                Text(
                                    text = "Stamp sequential numbers on every page",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InfinityMutedGray
                                )
                            }
                            Switch(
                                checked = includePageNumbers,
                                onCheckedChange = { includePageNumbers = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = InfinityCrispWhite,
                                    checkedTrackColor = InfinityCrimsonRed,
                                    uncheckedThumbColor = InfinityMutedGray,
                                    uncheckedTrackColor = InfinitySlateElevated
                                ),
                                modifier = Modifier.testTag("watermark_page_numbers_switch")
                            )
                        }

                        if (includePageNumbers) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Position Selector: Bottom Center, Bottom Right, Top Right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "BOTTOM_CENTER" to "Bottom Center",
                                    "BOTTOM_RIGHT" to "Bottom Right",
                                    "TOP_RIGHT" to "Top Right"
                                ).forEach { (posKey, posLabel) ->
                                    val selected = pageNumberPosition == posKey
                                    Surface(
                                        onClick = { pageNumberPosition = posKey },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) InfinityCrimsonRed else InfinitySlateElevated,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("page_num_pos_${posKey.lowercase()}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = posLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = InfinityCrispWhite
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Format Selector: Page X of Y vs X / Y
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "PAGE_X_OF_Y" to "Page X of Y",
                                    "X_SLASH_Y" to "X / Y"
                                ).forEach { (fmtKey, fmtLabel) ->
                                    val selected = pageNumberFormat == fmtKey
                                    Surface(
                                        onClick = { pageNumberFormat = fmtKey },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) InfinityCrimsonRed else InfinitySlateElevated,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("page_num_fmt_${fmtKey.lowercase()}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = fmtLabel,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = InfinityCrispWhite
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = watermarkOutputTitle,
                            onValueChange = { watermarkOutputTitle = it },
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
                                .testTag("watermark_output_title_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val doc = selectedWatermarkDoc ?: return@Button
                                activeModal = StudioDialogMode.NONE
                                onApplyWatermarkAndNumbering(
                                    doc,
                                    watermarkText,
                                    watermarkColorOption.argb,
                                    watermarkOpacityFraction,
                                    watermarkIsDiagonal,
                                    includePageNumbers,
                                    pageNumberPosition,
                                    pageNumberFormat,
                                    watermarkOutputTitle
                                )
                            },
                            enabled = selectedWatermarkDoc != null && (watermarkText.isNotBlank() || includePageNumbers),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InfinityCrimsonRed,
                                contentColor = InfinityCrispWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_watermark_button")
                        ) {
                            Text("Apply Watermark & Save Native PDF")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                StudioDialogMode.MERGE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Merge PDF Documents",
                            style = MaterialTheme.typography.titleLarge,
                            color = InfinityCrispWhite
                        )
                        Text(
                            text = "Select 2 or more PDFs to combine sequentially into a single native file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InfinityMutedGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mergedTitleInput,
                            onValueChange = { mergedTitleInput = it },
                            label = { Text("Output PDF Title", color = InfinityMutedGray) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InfinityCrispWhite,
                                unfocusedTextColor = InfinityCrispWhite,
                                focusedBorderColor = InfinityCrispWhite,
                                unfocusedBorderColor = InfinityDividerGray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("merge_title_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(220.dp)
                        ) {
                            items(documents, key = { it.id }) { doc ->
                                val isChecked = selectedMergeDocIds.contains(doc.id)
                                Surface(
                                    onClick = {
                                        if (isChecked) {
                                            selectedMergeDocIds.remove(doc.id)
                                        } else {
                                            selectedMergeDocIds.add(doc.id)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = InfinitySlateElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isChecked) {
                                                Icons.Filled.CheckBox
                                            } else {
                                                Icons.Filled.CheckBoxOutlineBlank
                                            },
                                            contentDescription = doc.title,
                                            tint = if (isChecked) InfinityCrimsonRed else InfinityMutedGray
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = doc.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = InfinityCrispWhite
                                            )
                                            Text(
                                                text = "${doc.pageCount} pages",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = InfinityMutedGray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val chosen = documents.filter { selectedMergeDocIds.contains(it.id) }
                                activeModal = StudioDialogMode.NONE
                                onMergeDocuments(chosen, mergedTitleInput)
                            },
                            enabled = selectedMergeDocIds.size >= 2,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InfinityCrimsonRed,
                                contentColor = InfinityCrispWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_merge_button")
                        ) {
                            Text("Merge ${selectedMergeDocIds.size} Selected PDFs")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                StudioDialogMode.SPLIT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Split & Extract Page Range",
                            style = MaterialTheme.typography.titleLarge,
                            color = InfinityCrispWhite
                        )
                        Text(
                            text = "Choose a source PDF and page interval to extract.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InfinityMutedGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(150.dp)
                        ) {
                            items(documents, key = { it.id }) { doc ->
                                val isSelected = selectedSplitDoc?.id == doc.id
                                Surface(
                                    onClick = {
                                        selectedSplitDoc = doc
                                        splitStartPage = 1
                                        splitEndPage = doc.pageCount.coerceAtLeast(1)
                                        splitTitleInput = "${doc.title} (Excerpt)"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) InfinityCrimsonRed else InfinitySlateElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = doc.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = InfinityCrispWhite
                                        )
                                        Text(
                                            text = "${doc.pageCount}P",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = InfinityCrispWhite
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val maxP = (selectedSplitDoc?.pageCount ?: 1).coerceAtLeast(1)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = splitStartPage.toString(),
                                onValueChange = { v ->
                                    splitStartPage = (v.toIntOrNull() ?: 1).coerceIn(1, maxP)
                                },
                                label = { Text("Start Page (1..$maxP)", color = InfinityMutedGray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = InfinityCrispWhite,
                                    unfocusedTextColor = InfinityCrispWhite,
                                    focusedBorderColor = InfinityCrispWhite,
                                    unfocusedBorderColor = InfinityDividerGray
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = splitEndPage.toString(),
                                onValueChange = { v ->
                                    splitEndPage = (v.toIntOrNull() ?: maxP).coerceIn(splitStartPage, maxP)
                                },
                                label = { Text("End Page (..$maxP)", color = InfinityMutedGray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = InfinityCrispWhite,
                                    unfocusedTextColor = InfinityCrispWhite,
                                    focusedBorderColor = InfinityCrispWhite,
                                    unfocusedBorderColor = InfinityDividerGray
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = splitTitleInput,
                            onValueChange = { splitTitleInput = it },
                            label = { Text("Extracted PDF Title", color = InfinityMutedGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InfinityCrispWhite,
                                unfocusedTextColor = InfinityCrispWhite,
                                focusedBorderColor = InfinityCrispWhite,
                                unfocusedBorderColor = InfinityDividerGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val doc = selectedSplitDoc ?: return@Button
                                activeModal = StudioDialogMode.NONE
                                onSplitDocument(doc, splitStartPage, splitEndPage, splitTitleInput)
                            },
                            enabled = selectedSplitDoc != null,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InfinityCrimsonRed,
                                contentColor = InfinityCrispWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_split_button")
                        ) {
                            Text("Extract Pages $splitStartPage–$splitEndPage to New PDF")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                StudioDialogMode.NONE -> {}
            }
        }
    }
}
