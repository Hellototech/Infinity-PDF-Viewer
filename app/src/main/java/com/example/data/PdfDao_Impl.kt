package com.example.data

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Suppress("UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "RestrictedApi")
public class PdfDao_Impl(
    private val __db: RoomDatabase
) : PdfDao {

    private val __insertAdapterOfPdfDocumentEntity: EntityInsertAdapter<PdfDocumentEntity> =
        object : EntityInsertAdapter<PdfDocumentEntity>() {
            protected override fun createQuery(): String =
                "INSERT OR REPLACE INTO `pdf_documents` (`id`,`title`,`filePath`,`pageCount`,`fileSizeBytes`,`lastOpenedEpochMs`,`createdEpochMs`,`isFavorite`,`lastReadPage`,`categoryTag`,`summaryPreview`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)"

            protected override fun bind(statement: SQLiteStatement, entity: PdfDocumentEntity) {
                statement.bindLong(1, entity.id)
                statement.bindText(2, entity.title)
                statement.bindText(3, entity.filePath)
                statement.bindLong(4, entity.pageCount.toLong())
                statement.bindLong(5, entity.fileSizeBytes)
                statement.bindLong(6, entity.lastOpenedEpochMs)
                statement.bindLong(7, entity.createdEpochMs)
                val _tmp: Int = if (entity.isFavorite) 1 else 0
                statement.bindLong(8, _tmp.toLong())
                statement.bindLong(9, entity.lastReadPage.toLong())
                statement.bindText(10, entity.categoryTag)
                statement.bindText(11, entity.summaryPreview)
            }
        }

    private val __insertAdapterOfPdfAnnotationEntity: EntityInsertAdapter<PdfAnnotationEntity> =
        object : EntityInsertAdapter<PdfAnnotationEntity>() {
            protected override fun createQuery(): String =
                "INSERT OR REPLACE INTO `pdf_annotations` (`id`,`documentId`,`pageIndex`,`toolType`,`colorHex`,`strokeWidth`,`pointsJson`,`textContent`,`createdAtEpochMs`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

            protected override fun bind(statement: SQLiteStatement, entity: PdfAnnotationEntity) {
                statement.bindLong(1, entity.id)
                statement.bindLong(2, entity.documentId)
                statement.bindLong(3, entity.pageIndex.toLong())
                statement.bindText(4, entity.toolType)
                statement.bindLong(5, entity.colorHex)
                statement.bindDouble(6, entity.strokeWidth.toDouble())
                statement.bindText(7, entity.pointsJson)
                statement.bindText(8, entity.textContent)
                statement.bindLong(9, entity.createdAtEpochMs)
            }
        }

    private val __insertAdapterOfPdfBookmarkEntity: EntityInsertAdapter<PdfBookmarkEntity> =
        object : EntityInsertAdapter<PdfBookmarkEntity>() {
            protected override fun createQuery(): String =
                "INSERT OR REPLACE INTO `pdf_bookmarks` (`id`,`documentId`,`pageIndex`,`label`,`note`,`createdAtEpochMs`) VALUES (nullif(?, 0),?,?,?,?,?)"

            protected override fun bind(statement: SQLiteStatement, entity: PdfBookmarkEntity) {
                statement.bindLong(1, entity.id)
                statement.bindLong(2, entity.documentId)
                statement.bindLong(3, entity.pageIndex.toLong())
                statement.bindText(4, entity.label)
                statement.bindText(5, entity.note)
                statement.bindLong(6, entity.createdAtEpochMs)
            }
        }

    private val __updateAdapterOfPdfDocumentEntity: EntityDeleteOrUpdateAdapter<PdfDocumentEntity> =
        object : EntityDeleteOrUpdateAdapter<PdfDocumentEntity>() {
            protected override fun createQuery(): String =
                "UPDATE OR ABORT `pdf_documents` SET `id` = ?,`title` = ?,`filePath` = ?,`pageCount` = ?,`fileSizeBytes` = ?,`lastOpenedEpochMs` = ?,`createdEpochMs` = ?,`isFavorite` = ?,`lastReadPage` = ?,`categoryTag` = ?,`summaryPreview` = ? WHERE `id` = ?"

            protected override fun bind(statement: SQLiteStatement, entity: PdfDocumentEntity) {
                statement.bindLong(1, entity.id)
                statement.bindText(2, entity.title)
                statement.bindText(3, entity.filePath)
                statement.bindLong(4, entity.pageCount.toLong())
                statement.bindLong(5, entity.fileSizeBytes)
                statement.bindLong(6, entity.lastOpenedEpochMs)
                statement.bindLong(7, entity.createdEpochMs)
                val _tmp: Int = if (entity.isFavorite) 1 else 0
                statement.bindLong(8, _tmp.toLong())
                statement.bindLong(9, entity.lastReadPage.toLong())
                statement.bindText(10, entity.categoryTag)
                statement.bindText(11, entity.summaryPreview)
                statement.bindLong(12, entity.id)
            }
        }

    public override suspend fun insertDocument(doc: PdfDocumentEntity): Long =
        performSuspending(__db, false, true) { _connection ->
            __insertAdapterOfPdfDocumentEntity.insertAndReturnId(_connection, doc)
        }

    public override suspend fun insertAnnotation(annotation: PdfAnnotationEntity): Long =
        performSuspending(__db, false, true) { _connection ->
            __insertAdapterOfPdfAnnotationEntity.insertAndReturnId(_connection, annotation)
        }

    public override suspend fun insertBookmark(bookmark: PdfBookmarkEntity): Long =
        performSuspending(__db, false, true) { _connection ->
            __insertAdapterOfPdfBookmarkEntity.insertAndReturnId(_connection, bookmark)
        }

    public override suspend fun updateDocument(doc: PdfDocumentEntity): Unit =
        performSuspending(__db, false, true) { _connection ->
            __updateAdapterOfPdfDocumentEntity.handle(_connection, doc)
        }

    public override fun getAllDocuments(): Flow<List<PdfDocumentEntity>> {
        val _sql = "SELECT * FROM pdf_documents ORDER BY lastOpenedEpochMs DESC"
        return createFlow(__db, false, arrayOf("pdf_documents")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                val _columnIndexOfId = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfTitle = getColumnIndexOrThrow(_stmt, "title")
                val _columnIndexOfFilePath = getColumnIndexOrThrow(_stmt, "filePath")
                val _columnIndexOfPageCount = getColumnIndexOrThrow(_stmt, "pageCount")
                val _columnIndexOfFileSizeBytes = getColumnIndexOrThrow(_stmt, "fileSizeBytes")
                val _columnIndexOfLastOpenedEpochMs = getColumnIndexOrThrow(_stmt, "lastOpenedEpochMs")
                val _columnIndexOfCreatedEpochMs = getColumnIndexOrThrow(_stmt, "createdEpochMs")
                val _columnIndexOfIsFavorite = getColumnIndexOrThrow(_stmt, "isFavorite")
                val _columnIndexOfLastReadPage = getColumnIndexOrThrow(_stmt, "lastReadPage")
                val _columnIndexOfCategoryTag = getColumnIndexOrThrow(_stmt, "categoryTag")
                val _columnIndexOfSummaryPreview = getColumnIndexOrThrow(_stmt, "summaryPreview")
                val _result: MutableList<PdfDocumentEntity> = mutableListOf()
                while (_stmt.step()) {
                    val _tmpId = _stmt.getLong(_columnIndexOfId)
                    val _tmpTitle = _stmt.getText(_columnIndexOfTitle)
                    val _tmpFilePath = _stmt.getText(_columnIndexOfFilePath)
                    val _tmpPageCount = _stmt.getLong(_columnIndexOfPageCount).toInt()
                    val _tmpFileSizeBytes = _stmt.getLong(_columnIndexOfFileSizeBytes)
                    val _tmpLastOpenedEpochMs = _stmt.getLong(_columnIndexOfLastOpenedEpochMs)
                    val _tmpCreatedEpochMs = _stmt.getLong(_columnIndexOfCreatedEpochMs)
                    val _tmpIsFavorite = _stmt.getLong(_columnIndexOfIsFavorite).toInt() != 0
                    val _tmpLastReadPage = _stmt.getLong(_columnIndexOfLastReadPage).toInt()
                    val _tmpCategoryTag = _stmt.getText(_columnIndexOfCategoryTag)
                    val _tmpSummaryPreview = _stmt.getText(_columnIndexOfSummaryPreview)
                    _result.add(
                        PdfDocumentEntity(
                            _tmpId,
                            _tmpTitle,
                            _tmpFilePath,
                            _tmpPageCount,
                            _tmpFileSizeBytes,
                            _tmpLastOpenedEpochMs,
                            _tmpCreatedEpochMs,
                            _tmpIsFavorite,
                            _tmpLastReadPage,
                            _tmpCategoryTag,
                            _tmpSummaryPreview
                        )
                    )
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getDocumentById(id: Long): PdfDocumentEntity? {
        val _sql = "SELECT * FROM pdf_documents WHERE id = ? LIMIT 1"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, id)
                val _columnIndexOfId = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfTitle = getColumnIndexOrThrow(_stmt, "title")
                val _columnIndexOfFilePath = getColumnIndexOrThrow(_stmt, "filePath")
                val _columnIndexOfPageCount = getColumnIndexOrThrow(_stmt, "pageCount")
                val _columnIndexOfFileSizeBytes = getColumnIndexOrThrow(_stmt, "fileSizeBytes")
                val _columnIndexOfLastOpenedEpochMs = getColumnIndexOrThrow(_stmt, "lastOpenedEpochMs")
                val _columnIndexOfCreatedEpochMs = getColumnIndexOrThrow(_stmt, "createdEpochMs")
                val _columnIndexOfIsFavorite = getColumnIndexOrThrow(_stmt, "isFavorite")
                val _columnIndexOfLastReadPage = getColumnIndexOrThrow(_stmt, "lastReadPage")
                val _columnIndexOfCategoryTag = getColumnIndexOrThrow(_stmt, "categoryTag")
                val _columnIndexOfSummaryPreview = getColumnIndexOrThrow(_stmt, "summaryPreview")
                if (_stmt.step()) {
                    val _tmpId = _stmt.getLong(_columnIndexOfId)
                    val _tmpTitle = _stmt.getText(_columnIndexOfTitle)
                    val _tmpFilePath = _stmt.getText(_columnIndexOfFilePath)
                    val _tmpPageCount = _stmt.getLong(_columnIndexOfPageCount).toInt()
                    val _tmpFileSizeBytes = _stmt.getLong(_columnIndexOfFileSizeBytes)
                    val _tmpLastOpenedEpochMs = _stmt.getLong(_columnIndexOfLastOpenedEpochMs)
                    val _tmpCreatedEpochMs = _stmt.getLong(_columnIndexOfCreatedEpochMs)
                    val _tmpIsFavorite = _stmt.getLong(_columnIndexOfIsFavorite).toInt() != 0
                    val _tmpLastReadPage = _stmt.getLong(_columnIndexOfLastReadPage).toInt()
                    val _tmpCategoryTag = _stmt.getText(_columnIndexOfCategoryTag)
                    val _tmpSummaryPreview = _stmt.getText(_columnIndexOfSummaryPreview)
                    PdfDocumentEntity(
                        _tmpId,
                        _tmpTitle,
                        _tmpFilePath,
                        _tmpPageCount,
                        _tmpFileSizeBytes,
                        _tmpLastOpenedEpochMs,
                        _tmpCreatedEpochMs,
                        _tmpIsFavorite,
                        _tmpLastReadPage,
                        _tmpCategoryTag,
                        _tmpSummaryPreview
                    )
                } else {
                    null
                }
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getDocumentCount(): Int {
        val _sql = "SELECT COUNT(*) FROM pdf_documents"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                if (_stmt.step()) {
                    _stmt.getLong(0).toInt()
                } else {
                    0
                }
            } finally {
                _stmt.close()
            }
        }
    }

    public override fun getAnnotationsForDocument(documentId: Long): Flow<List<PdfAnnotationEntity>> {
        val _sql = "SELECT * FROM pdf_annotations WHERE documentId = ? ORDER BY createdAtEpochMs ASC"
        return createFlow(__db, false, arrayOf("pdf_annotations")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, documentId)
                val _columnIndexOfId = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfDocumentId = getColumnIndexOrThrow(_stmt, "documentId")
                val _columnIndexOfPageIndex = getColumnIndexOrThrow(_stmt, "pageIndex")
                val _columnIndexOfToolType = getColumnIndexOrThrow(_stmt, "toolType")
                val _columnIndexOfColorHex = getColumnIndexOrThrow(_stmt, "colorHex")
                val _columnIndexOfStrokeWidth = getColumnIndexOrThrow(_stmt, "strokeWidth")
                val _columnIndexOfPointsJson = getColumnIndexOrThrow(_stmt, "pointsJson")
                val _columnIndexOfTextContent = getColumnIndexOrThrow(_stmt, "textContent")
                val _columnIndexOfCreatedAtEpochMs = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
                val _result: MutableList<PdfAnnotationEntity> = mutableListOf()
                while (_stmt.step()) {
                    _result.add(
                        PdfAnnotationEntity(
                            _stmt.getLong(_columnIndexOfId),
                            _stmt.getLong(_columnIndexOfDocumentId),
                            _stmt.getLong(_columnIndexOfPageIndex).toInt(),
                            _stmt.getText(_columnIndexOfToolType),
                            _stmt.getLong(_columnIndexOfColorHex),
                            _stmt.getDouble(_columnIndexOfStrokeWidth).toFloat(),
                            _stmt.getText(_columnIndexOfPointsJson),
                            _stmt.getText(_columnIndexOfTextContent),
                            _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
                        )
                    )
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getAnnotationsForDocumentSync(documentId: Long): List<PdfAnnotationEntity> {
        val _sql = "SELECT * FROM pdf_annotations WHERE documentId = ? ORDER BY createdAtEpochMs ASC"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, documentId)
                val _columnIndexOfId = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfDocumentId = getColumnIndexOrThrow(_stmt, "documentId")
                val _columnIndexOfPageIndex = getColumnIndexOrThrow(_stmt, "pageIndex")
                val _columnIndexOfToolType = getColumnIndexOrThrow(_stmt, "toolType")
                val _columnIndexOfColorHex = getColumnIndexOrThrow(_stmt, "colorHex")
                val _columnIndexOfStrokeWidth = getColumnIndexOrThrow(_stmt, "strokeWidth")
                val _columnIndexOfPointsJson = getColumnIndexOrThrow(_stmt, "pointsJson")
                val _columnIndexOfTextContent = getColumnIndexOrThrow(_stmt, "textContent")
                val _columnIndexOfCreatedAtEpochMs = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
                val _result: MutableList<PdfAnnotationEntity> = mutableListOf()
                while (_stmt.step()) {
                    _result.add(
                        PdfAnnotationEntity(
                            _stmt.getLong(_columnIndexOfId),
                            _stmt.getLong(_columnIndexOfDocumentId),
                            _stmt.getLong(_columnIndexOfPageIndex).toInt(),
                            _stmt.getText(_columnIndexOfToolType),
                            _stmt.getLong(_columnIndexOfColorHex),
                            _stmt.getDouble(_columnIndexOfStrokeWidth).toFloat(),
                            _stmt.getText(_columnIndexOfPointsJson),
                            _stmt.getText(_columnIndexOfTextContent),
                            _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
                        )
                    )
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override fun getAllAnnotations(): Flow<List<PdfAnnotationEntity>> {
        val _sql = "SELECT * FROM pdf_annotations ORDER BY createdAtEpochMs DESC"
        return createFlow(__db, false, arrayOf("pdf_annotations")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                val _columnIndexOfId = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfDocumentId = getColumnIndexOrThrow(_stmt, "documentId")
                val _columnIndexOfPageIndex = getColumnIndexOrThrow(_stmt, "pageIndex")
                val _columnIndexOfToolType = getColumnIndexOrThrow(_stmt, "toolType")
                val _columnIndexOfColorHex = getColumnIndexOrThrow(_stmt, "colorHex")
                val _columnIndexOfStrokeWidth = getColumnIndexOrThrow(_stmt, "strokeWidth")
                val _columnIndexOfPointsJson = getColumnIndexOrThrow(_stmt, "pointsJson")
                val _columnIndexOfTextContent = getColumnIndexOrThrow(_stmt, "textContent")
                val _columnIndexOfCreatedAtEpochMs = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
                val _result: MutableList<PdfAnnotationEntity> = mutableListOf()
                while (_stmt.step()) {
                    _result.add(
                        PdfAnnotationEntity(
                            _stmt.getLong(_columnIndexOfId),
                            _stmt.getLong(_columnIndexOfDocumentId),
                            _stmt.getLong(_columnIndexOfPageIndex).toInt(),
                            _stmt.getText(_columnIndexOfToolType),
                            _stmt.getLong(_columnIndexOfColorHex),
                            _stmt.getDouble(_columnIndexOfStrokeWidth).toFloat(),
                            _stmt.getText(_columnIndexOfPointsJson),
                            _stmt.getText(_columnIndexOfTextContent),
                            _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
                        )
                    )
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override fun getBookmarksForDocument(documentId: Long): Flow<List<PdfBookmarkEntity>> {
        val _sql = "SELECT * FROM pdf_bookmarks WHERE documentId = ? ORDER BY pageIndex ASC"
        return createFlow(__db, false, arrayOf("pdf_bookmarks")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, documentId)
                val _columnIndexOfId = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfDocumentId = getColumnIndexOrThrow(_stmt, "documentId")
                val _columnIndexOfPageIndex = getColumnIndexOrThrow(_stmt, "pageIndex")
                val _columnIndexOfLabel = getColumnIndexOrThrow(_stmt, "label")
                val _columnIndexOfNote = getColumnIndexOrThrow(_stmt, "note")
                val _columnIndexOfCreatedAtEpochMs = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
                val _result: MutableList<PdfBookmarkEntity> = mutableListOf()
                while (_stmt.step()) {
                    _result.add(
                        PdfBookmarkEntity(
                            _stmt.getLong(_columnIndexOfId),
                            _stmt.getLong(_columnIndexOfDocumentId),
                            _stmt.getLong(_columnIndexOfPageIndex).toInt(),
                            _stmt.getText(_columnIndexOfLabel),
                            _stmt.getText(_columnIndexOfNote),
                            _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
                        )
                    )
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override fun getAllBookmarks(): Flow<List<PdfBookmarkEntity>> {
        val _sql = "SELECT * FROM pdf_bookmarks ORDER BY createdAtEpochMs DESC"
        return createFlow(__db, false, arrayOf("pdf_bookmarks")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                val _columnIndexOfId = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfDocumentId = getColumnIndexOrThrow(_stmt, "documentId")
                val _columnIndexOfPageIndex = getColumnIndexOrThrow(_stmt, "pageIndex")
                val _columnIndexOfLabel = getColumnIndexOrThrow(_stmt, "label")
                val _columnIndexOfNote = getColumnIndexOrThrow(_stmt, "note")
                val _columnIndexOfCreatedAtEpochMs = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
                val _result: MutableList<PdfBookmarkEntity> = mutableListOf()
                while (_stmt.step()) {
                    _result.add(
                        PdfBookmarkEntity(
                            _stmt.getLong(_columnIndexOfId),
                            _stmt.getLong(_columnIndexOfDocumentId),
                            _stmt.getLong(_columnIndexOfPageIndex).toInt(),
                            _stmt.getText(_columnIndexOfLabel),
                            _stmt.getText(_columnIndexOfNote),
                            _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
                        )
                    )
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun deleteDocumentById(id: Long) {
        val _sql = "DELETE FROM pdf_documents WHERE id = ?"
        return performSuspending(__db, false, true) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, id)
                _stmt.step()
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun deleteAnnotationById(id: Long) {
        val _sql = "DELETE FROM pdf_annotations WHERE id = ?"
        return performSuspending(__db, false, true) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, id)
                _stmt.step()
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun deleteAnnotationsForDocument(documentId: Long) {
        val _sql = "DELETE FROM pdf_annotations WHERE documentId = ?"
        return performSuspending(__db, false, true) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, documentId)
                _stmt.step()
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun clearPageAnnotations(documentId: Long, pageIndex: Int) {
        val _sql = "DELETE FROM pdf_annotations WHERE documentId = ? AND pageIndex = ?"
        return performSuspending(__db, false, true) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, documentId)
                _stmt.bindLong(2, pageIndex.toLong())
                _stmt.step()
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun deleteBookmarkById(id: Long) {
        val _sql = "DELETE FROM pdf_bookmarks WHERE id = ?"
        return performSuspending(__db, false, true) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, id)
                _stmt.step()
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun deleteBookmarksForDocument(documentId: Long) {
        val _sql = "DELETE FROM pdf_bookmarks WHERE documentId = ?"
        return performSuspending(__db, false, true) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, documentId)
                _stmt.step()
            } finally {
                _stmt.close()
            }
        }
    }

    public companion object {
        public fun getRequiredConverters(): List<KClass<*>> = emptyList()
    }
}
