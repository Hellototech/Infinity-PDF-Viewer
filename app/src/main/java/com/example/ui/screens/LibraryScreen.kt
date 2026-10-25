package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PdfAnnotationEntity
import com.example.data.PdfBookmarkEntity
import com.example.data.PdfDocumentEntity
import com.example.ui.components.PdfThumbnailView
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatRelativeDate
import com.example.ui.theme.InfinityCrimsonRed
import com.example.ui.theme.InfinityCrispWhite
import com.example.ui.theme.InfinityDividerGray
import com.example.ui.theme.InfinityMutedGray
import com.example.ui.theme.InfinityOledBlack
import com.example.ui.theme.InfinitySlateElevated
import com.example.ui.theme.InfinitySlateGray
import com.example.viewmodel.LibraryFilter
import com.example.viewmodel.LibrarySort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    documents: List<PdfDocumentEntity>,
    allAnnotations: List<PdfAnnotationEntity>,
    allBookmarks: List<PdfBookmarkEntity>,
    searchQuery: String,
    selectedFilter: LibraryFilter,
    selectedSort: LibrarySort,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (LibraryFilter) -> Unit,
    onSortSelect: (LibrarySort) -> Unit,
    onOpenDocument: (Long) -> Unit,
    onToggleFavorite: (PdfDocumentEntity) -> Unit,
    onRenameDocument: (PdfDocumentEntity, String) -> Unit,
    onShareDocument: (PdfDocumentEntity) -> Unit,
    onDeleteDocument: (PdfDocumentEntity) -> Unit,
    onScanPhysicalDocumentClick: () -> Unit,
    onImportDeviceFileClick: () -> Unit,
    hasStoragePermission: Boolean = true,
    onRequestStoragePermission: (() -> Unit)? = null
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var activeActionDoc by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var renameTargetDoc by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    var showLocalAddSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InfinityOledBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header Row: Title on Left, '+' Local Add/Scan Icon Button on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "INFINITY PDF",
                    style = MaterialTheme.typography.headlineLarge,
                    color = InfinityCrispWhite
                )
                Text(
                    text = "${documents.size} Documents • ${allAnnotations.size} Vector Annotations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfinityMutedGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                onClick = { showLocalAddSheet = true },
                shape = RoundedCornerShape(16.dp),
                color = InfinitySlateGray,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("import_pdf_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Scan or Import Local PDF",
                        tint = InfinityCrispWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar + Sort Trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search documents, categories, notes…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InfinityMutedGray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = InfinityCrispWhite
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = InfinityMutedGray
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InfinitySlateGray,
                    unfocusedContainerColor = InfinitySlateGray,
                    focusedBorderColor = InfinityDividerGray,
                    unfocusedBorderColor = InfinitySlateGray,
                    focusedTextColor = InfinityCrispWhite,
                    unfocusedTextColor = InfinityCrispWhite,
                    cursorColor = InfinityCrispWhite
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("library_search_input")
            )

            Box {
                Surface(
                    onClick = { sortMenuExpanded = true },
                    shape = RoundedCornerShape(16.dp),
                    color = InfinitySlateGray,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("sort_documents_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = "Sort Documents",
                            tint = InfinityCrispWhite
                        )
                    }
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(InfinitySlateGray)
                ) {
                    LibrarySort.entries.forEach { sortOption ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = sortOption.label,
                                    color = InfinityCrispWhite,
                                    fontWeight = if (sortOption == selectedSort) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSortSelect(sortOption)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Pills (Selected state uses Crimson Red #ED1C24 pill per Strict Color Law)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(LibraryFilter.entries) { filter ->
                val isSelected = filter == selectedFilter
                Surface(
                    onClick = { onFilterSelect(filter) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) InfinityCrimsonRed else InfinitySlateGray,
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("filter_chip_${filter.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = InfinityCrispWhite
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Document List or Empty State
        if (documents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = InfinitySlateGray,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = "No PDFs",
                            tint = InfinityCrispWhite,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isBlank() && selectedFilter == LibraryFilter.ALL) {
                                "No PDF files found"
                            } else {
                                "No Matching PDF Documents"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = InfinityCrispWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Device PDFs are auto-detected. Tap '+' in the top right to scan a physical document or import from device files.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InfinityMutedGray
                        )
                        if (!hasStoragePermission && onRequestStoragePermission != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRequestStoragePermission,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = InfinityCrimsonRed,
                                    contentColor = InfinityCrispWhite
                                ),
                                modifier = Modifier.testTag("empty_state_grant_storage_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FolderOpen,
                                    contentDescription = "Grant Storage Access",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grant Storage Access")
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("pdf_document_list")
            ) {
                items(documents, key = { it.id }) { doc ->
                    val docAnnotationsCount = allAnnotations.count { it.documentId == doc.id }
                    val docBookmarksCount = allBookmarks.count { it.documentId == doc.id }
                    PdfDocumentCard(
                        doc = doc,
                        annotationCount = docAnnotationsCount,
                        bookmarkCount = docBookmarksCount,
                        onOpen = { onOpenDocument(doc.id) },
                        onToggleFavorite = { onToggleFavorite(doc) },
                        onOpenActions = { activeActionDoc = doc }
                    )
                }
            }
        }
    }

    // Local Document Add Sheet ('+' button action: Scan Physical Document or Import from Device Files)
    if (showLocalAddSheet) {
        val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLocalAddSheet = false },
            sheetState = addSheetState,
            containerColor = InfinitySlateGray,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Add Local PDF Document",
                    style = MaterialTheme.typography.titleLarge,
                    color = InfinityCrispWhite
                )
                Text(
                    text = "100% Local-First Studio • Scan physical pages or pick a PDF from device storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfinityMutedGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Scan Physical Document (ML Kit Document Scanner)
                Surface(
                    onClick = {
                        showLocalAddSheet = false
                        onScanPhysicalDocumentClick()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = InfinitySlateElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_physical_document_button")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(InfinitySlateGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DocumentScanner,
                                contentDescription = "Scan Physical Document",
                                tint = InfinityCrimsonRed
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan Physical Document",
                                style = MaterialTheme.typography.titleMedium,
                                color = InfinityCrispWhite
                            )
                            Text(
                                text = "Capture & auto-crop pages into a local PDF via ML Kit Document Scanner",
                                style = MaterialTheme.typography.bodySmall,
                                color = InfinityMutedGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Import from Device Files (ActivityResultContracts.OpenDocument)
                Surface(
                    onClick = {
                        showLocalAddSheet = false
                        onImportDeviceFileClick()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = InfinitySlateElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_device_files_button")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(InfinitySlateGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = "Import from Device Files",
                                tint = InfinityCrispWhite
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Import from Device Files",
                                style = MaterialTheme.typography.titleMedium,
                                color = InfinityCrispWhite
                            )
                            Text(
                                text = "Pick any PDF stored on your phone's local storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = InfinityMutedGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Document Action Bottom Sheet (#1E1E1E with 16dp corners)
    activeActionDoc?.let { doc ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeActionDoc = null },
            sheetState = sheetState,
            containerColor = InfinitySlateGray,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = InfinityCrispWhite
                )
                Text(
                    text = "${doc.pageCount} Pages • ${formatFileSize(doc.fileSizeBytes)} • ${doc.categoryTag}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfinityMutedGray
                )
                Spacer(modifier = Modifier.height(16.dp))

                SheetActionRow(
                    icon = Icons.Filled.DriveFileRenameOutline,
                    title = "Rename Document",
                    subtitle = "Update display title in Infinity Library",
                    testTag = "sheet_rename_pdf",
                    onClick = {
                        renameTargetDoc = doc
                        renameText = doc.title
                        activeActionDoc = null
                    }
                )
                SheetActionRow(
                    icon = Icons.Filled.Share,
                    title = "Share / Export Native PDF",
                    subtitle = "Send file via system Share sheet",
                    testTag = "sheet_share_pdf",
                    onClick = {
                        activeActionDoc = null
                        onShareDocument(doc)
                    }
                )
                SheetActionRow(
                    icon = Icons.Filled.DeleteOutline,
                    title = "Delete from Library",
                    subtitle = "Permanently remove PDF and its annotations",
                    testTag = "sheet_delete_pdf",
                    onClick = {
                        activeActionDoc = null
                        onDeleteDocument(doc)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Rename Dialog
    renameTargetDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { renameTargetDoc = null },
            containerColor = InfinitySlateGray,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Rename PDF Document",
                    style = MaterialTheme.typography.titleLarge,
                    color = InfinityCrispWhite
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = InfinityCrispWhite,
                        unfocusedTextColor = InfinityCrispWhite,
                        focusedBorderColor = InfinityCrispWhite,
                        unfocusedBorderColor = InfinityMutedGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_dialog_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameDocument(doc, renameText)
                        renameTargetDoc = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InfinitySlateElevated,
                        contentColor = InfinityCrispWhite
                    ),
                    modifier = Modifier.testTag("rename_confirm_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetDoc = null }) {
                    Text("Cancel", color = InfinityMutedGray)
                }
            }
        )
    }
}

@Composable
private fun PdfDocumentCard(
    doc: PdfDocumentEntity,
    annotationCount: Int,
    bookmarkCount: Int,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenActions: () -> Unit
) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        color = InfinitySlateGray,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_card_${doc.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Native PdfRenderer Page 1 Thumbnail
            PdfThumbnailView(
                filePath = doc.filePath,
                pageIndex = 0,
                targetWidthPx = 280,
                cornerRadius = 12.dp,
                modifier = Modifier
                    .width(72.dp)
                    .height(98.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Top Badge Row: Category Tag + Read Progress + Annotation/Bookmark Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = InfinitySlateElevated
                    ) {
                        Text(
                            text = doc.categoryTag.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = InfinityMutedGray,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (doc.lastReadPage > 0) {
                        Text(
                            text = "P.${doc.lastReadPage + 1}/${doc.pageCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = InfinityMutedGray,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (annotationCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Draw,
                                contentDescription = "Annotations",
                                tint = InfinityCrispWhite,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$annotationCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = InfinityCrispWhite,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    if (bookmarkCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = "Bookmarks",
                                tint = InfinityCrispWhite,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$bookmarkCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = InfinityCrispWhite,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = InfinityCrispWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = doc.summaryPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfinityMutedGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Single-Line Metadata Row (Pages • Size • Date — guaranteed never to wrap vertically)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${doc.pageCount} pages • ${formatFileSize(doc.fileSizeBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = InfinityMutedGray,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = formatRelativeDate(doc.lastOpenedEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = InfinityMutedGray,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("favorite_button_${doc.id}")
                ) {
                    Icon(
                        imageVector = if (doc.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (doc.isFavorite) "Remove Favorite" else "Mark Favorite",
                        tint = InfinityCrispWhite
                    )
                }

                IconButton(
                    onClick = onOpenActions,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("more_actions_button_${doc.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Document Actions",
                        tint = InfinityMutedGray
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(InfinitySlateElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = InfinityCrispWhite,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = InfinityCrispWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = InfinityMutedGray
            )
        }
    }
}
