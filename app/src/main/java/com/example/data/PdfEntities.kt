package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "pdf_documents")
data class PdfDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val lastOpenedEpochMs: Long = System.currentTimeMillis(),
    val createdEpochMs: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val lastReadPage: Int = 0,
    val categoryTag: String = "Document",
    val summaryPreview: String = ""
)

enum class AnnotationToolType {
    VIEW,
    INK,
    HIGHLIGHT,
    TEXT_NOTE,
    STAMP,
    RECTANGLE,
    ERASER
}

@Serializable
data class NormalizedPoint(
    val x: Float,
    val y: Float
)

@Entity(tableName = "pdf_annotations")
data class PdfAnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val toolType: String, // AnnotationToolType.name
    val colorHex: Long,
    val strokeWidth: Float,
    val pointsJson: String, // JSON list of NormalizedPoint (0f..1f relative to page width/height)
    val textContent: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "pdf_bookmarks")
data class PdfBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val label: String,
    val note: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
