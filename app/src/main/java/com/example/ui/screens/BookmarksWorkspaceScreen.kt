package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PdfAnnotationEntity
import com.example.data.PdfBookmarkEntity
import com.example.data.PdfDocumentEntity
import com.example.ui.components.PdfThumbnailView
import com.example.ui.components.formatRelativeDate
import com.example.ui.theme.InfinityCrimsonRed
import com.example.ui.theme.InfinityCrispWhite
import com.example.ui.theme.InfinityMutedGray
import com.example.ui.theme.InfinityOledBlack
import com.example.ui.theme.InfinitySlateGray

@Composable
fun BookmarksWorkspaceScreen(
    documents: List<PdfDocumentEntity>,
    bookmarks: List<PdfBookmarkEntity>,
    annotations: List<PdfAnnotationEntity>,
    onJumpToDocumentPage: (Long, Int) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteAnnotation: (Long) -> Unit
) {
    var activeSection by remember { mutableIntStateOf(0) } // 0 = Bookmarks, 1 = Vector Annotations
    val docById = remember(documents) { documents.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InfinityOledBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ANNOTATION WORKSPACE",
            style = MaterialTheme.typography.headlineLarge,
            color = InfinityCrispWhite
        )
        Text(
            text = "${bookmarks.size} Saved Page Bookmarks • ${annotations.size} Vector & Stamp Annotations",
            style = MaterialTheme.typography.bodyMedium,
            color = InfinityMutedGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Toggle Pills (Active state uses Crimson Red #ED1C24 pill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = { activeSection = 0 },
                shape = RoundedCornerShape(16.dp),
                color = if (activeSection == 0) InfinityCrimsonRed else InfinitySlateGray,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("workspace_tab_bookmarks")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = "Bookmarks",
                        tint = InfinityCrispWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bookmarks (${bookmarks.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = InfinityCrispWhite
                    )
                }
            }

            Surface(
                onClick = { activeSection = 1 },
                shape = RoundedCornerShape(16.dp),
                color = if (activeSection == 1) InfinityCrimsonRed else InfinitySlateGray,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("workspace_tab_annotations")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Draw,
                        contentDescription = "Annotations",
                        tint = InfinityCrispWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Annotations (${annotations.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = InfinityCrispWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (activeSection == 0) {
            if (bookmarks.isEmpty()) {
                WorkspaceEmptyCard(
                    title = "No Page Bookmarks Yet",
                    subtitle = "Open any PDF document and tap the Bookmark icon in the top bar to save key pages."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(bookmarks, key = { it.id }) { bm ->
                        val doc = docById[bm.documentId]
                        if (doc != null) {
                            Surface(
                                onClick = { onJumpToDocumentPage(doc.id, bm.pageIndex) },
                                shape = RoundedCornerShape(16.dp),
                                color = InfinitySlateGray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bookmark_card_${bm.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PdfThumbnailView(
                                        filePath = doc.filePath,
                                        pageIndex = bm.pageIndex,
                                        targetWidthPx = 220,
                                        cornerRadius = 10.dp,
                                        modifier = Modifier
                                            .width(56.dp)
                                            .height(76.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bm.label,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = InfinityCrispWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${doc.title} • Page ${bm.pageIndex + 1} of ${doc.pageCount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = InfinityMutedGray
                                        )
                                        if (bm.note.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = bm.note,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = InfinityMutedGray
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteBookmark(bm.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete Bookmark",
                                            tint = InfinityMutedGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (annotations.isEmpty()) {
                WorkspaceEmptyCard(
                    title = "No Vector Annotations Yet",
                    subtitle = "Select the Ink Pen, Highlighter, Note, or Stamp tool inside the PDF Reader to annotate."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(annotations, key = { it.id }) { ann ->
                        val doc = docById[ann.documentId]
                        if (doc != null) {
                            Surface(
                                onClick = { onJumpToDocumentPage(doc.id, ann.pageIndex) },
                                shape = RoundedCornerShape(16.dp),
                                color = InfinitySlateGray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("annotation_card_${ann.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PdfThumbnailView(
                                        filePath = doc.filePath,
                                        pageIndex = ann.pageIndex,
                                        targetWidthPx = 220,
                                        cornerRadius = 10.dp,
                                        modifier = Modifier
                                            .width(56.dp)
                                            .height(76.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(ann.colorHex))
                                            )
                                            Text(
                                                text = if (ann.textContent.isNotBlank()) {
                                                    "${ann.toolType} — \"${ann.textContent}\""
                                                } else {
                                                    "${ann.toolType} Vector Stroke (${ann.strokeWidth.toInt()}pt)"
                                                },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = InfinityCrispWhite
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${doc.title} • Page ${ann.pageIndex + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = InfinityMutedGray
                                        )
                                        Text(
                                            text = formatRelativeDate(ann.createdAtEpochMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InfinityMutedGray
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteAnnotation(ann.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete Annotation",
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
    }
}

@Composable
private fun WorkspaceEmptyCard(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = InfinityCrispWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfinityMutedGray
                )
            }
        }
    }
}
