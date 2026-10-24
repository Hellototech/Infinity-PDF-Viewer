package com.example.data

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "RestrictedApi")
public class PdfDatabase_Impl : PdfDatabase() {
    private val _pdfDao: Lazy<PdfDao> = lazy {
        PdfDao_Impl(this)
    }

    protected override fun createOpenDelegate(): RoomOpenDelegate {
        val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(
            1,
            "7b1a3c608199bfb122ac63dd849efaef",
            "2bf4f8f13a93821712f78dc6fc6340dd"
        ) {
            public override fun createAllTables(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `pdf_documents` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `filePath` TEXT NOT NULL, `pageCount` INTEGER NOT NULL, `fileSizeBytes` INTEGER NOT NULL, `lastOpenedEpochMs` INTEGER NOT NULL, `createdEpochMs` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `lastReadPage` INTEGER NOT NULL, `categoryTag` TEXT NOT NULL, `summaryPreview` TEXT NOT NULL)")
                connection.execSQL("CREATE TABLE IF NOT EXISTS `pdf_annotations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `documentId` INTEGER NOT NULL, `pageIndex` INTEGER NOT NULL, `toolType` TEXT NOT NULL, `colorHex` INTEGER NOT NULL, `strokeWidth` REAL NOT NULL, `pointsJson` TEXT NOT NULL, `textContent` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL)")
                connection.execSQL("CREATE TABLE IF NOT EXISTS `pdf_bookmarks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `documentId` INTEGER NOT NULL, `pageIndex` INTEGER NOT NULL, `label` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL)")
                connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7b1a3c608199bfb122ac63dd849efaef')")
            }

            public override fun dropAllTables(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE IF EXISTS `pdf_documents`")
                connection.execSQL("DROP TABLE IF EXISTS `pdf_annotations`")
                connection.execSQL("DROP TABLE IF EXISTS `pdf_bookmarks`")
            }

            public override fun onCreate(connection: SQLiteConnection) {
            }

            public override fun onOpen(connection: SQLiteConnection) {
                internalInitInvalidationTracker(connection)
            }

            public override fun onPreMigrate(connection: SQLiteConnection) {
                dropFtsSyncTriggers(connection)
            }

            public override fun onPostMigrate(connection: SQLiteConnection) {
            }

            public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
                val _columnsPdfDocuments: MutableMap<String, TableInfo.Column> = mutableMapOf()
                _columnsPdfDocuments["id"] = TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfDocuments["title"] = TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfDocuments["filePath"] = TableInfo.Column("filePath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfDocuments["pageCount"] = TableInfo.Column("pageCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfDocuments["fileSizeBytes"] = TableInfo.Column("fileSizeBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfDocuments["lastOpenedEpochMs"] = TableInfo.Column("lastOpenedEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfDocuments["createdEpochMs"] = TableInfo.Column("createdEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfDocuments["isFavorite"] = TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfDocuments["lastReadPage"] = TableInfo.Column("lastReadPage", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfDocuments["categoryTag"] = TableInfo.Column("categoryTag", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfDocuments["summaryPreview"] = TableInfo.Column("summaryPreview", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                val _foreignKeysPdfDocuments: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
                val _indicesPdfDocuments: MutableSet<TableInfo.Index> = mutableSetOf()
                val _infoPdfDocuments = TableInfo("pdf_documents", _columnsPdfDocuments, _foreignKeysPdfDocuments, _indicesPdfDocuments)
                val _existingPdfDocuments = read(connection, "pdf_documents")
                if (_infoPdfDocuments != _existingPdfDocuments) {
                    return RoomOpenDelegate.ValidationResult(
                        false,
                        """
                        |pdf_documents(com.example.data.PdfDocumentEntity).
                        | Expected:
                        |$_infoPdfDocuments
                        |
                        | Found:
                        |$_existingPdfDocuments
                        """.trimMargin()
                    )
                }

                val _columnsPdfAnnotations: MutableMap<String, TableInfo.Column> = mutableMapOf()
                _columnsPdfAnnotations["id"] = TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfAnnotations["documentId"] = TableInfo.Column("documentId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfAnnotations["pageIndex"] = TableInfo.Column("pageIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfAnnotations["toolType"] = TableInfo.Column("toolType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfAnnotations["colorHex"] = TableInfo.Column("colorHex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfAnnotations["strokeWidth"] = TableInfo.Column("strokeWidth", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfAnnotations["pointsJson"] = TableInfo.Column("pointsJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfAnnotations["textContent"] = TableInfo.Column("textContent", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfAnnotations["createdAtEpochMs"] = TableInfo.Column("createdAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                val _foreignKeysPdfAnnotations: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
                val _indicesPdfAnnotations: MutableSet<TableInfo.Index> = mutableSetOf()
                val _infoPdfAnnotations = TableInfo("pdf_annotations", _columnsPdfAnnotations, _foreignKeysPdfAnnotations, _indicesPdfAnnotations)
                val _existingPdfAnnotations = read(connection, "pdf_annotations")
                if (_infoPdfAnnotations != _existingPdfAnnotations) {
                    return RoomOpenDelegate.ValidationResult(
                        false,
                        """
                        |pdf_annotations(com.example.data.PdfAnnotationEntity).
                        | Expected:
                        |$_infoPdfAnnotations
                        |
                        | Found:
                        |$_existingPdfAnnotations
                        """.trimMargin()
                    )
                }

                val _columnsPdfBookmarks: MutableMap<String, TableInfo.Column> = mutableMapOf()
                _columnsPdfBookmarks["id"] = TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfBookmarks["documentId"] = TableInfo.Column("documentId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfBookmarks["pageIndex"] = TableInfo.Column("pageIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfBookmarks["label"] = TableInfo.Column("label", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfBookmarks["note"] = TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                _columnsPdfBookmarks["createdAtEpochMs"] = TableInfo.Column("createdAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                val _foreignKeysPdfBookmarks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
                val _indicesPdfBookmarks: MutableSet<TableInfo.Index> = mutableSetOf()
                val _infoPdfBookmarks = TableInfo("pdf_bookmarks", _columnsPdfBookmarks, _foreignKeysPdfBookmarks, _indicesPdfBookmarks)
                val _existingPdfBookmarks = read(connection, "pdf_bookmarks")
                if (_infoPdfBookmarks != _existingPdfBookmarks) {
                    return RoomOpenDelegate.ValidationResult(
                        false,
                        """
                        |pdf_bookmarks(com.example.data.PdfBookmarkEntity).
                        | Expected:
                        |$_infoPdfBookmarks
                        |
                        | Found:
                        |$_existingPdfBookmarks
                        """.trimMargin()
                    )
                }
                return RoomOpenDelegate.ValidationResult(true, null)
            }
        }
        return _openDelegate
    }

    protected override fun createInvalidationTracker(): InvalidationTracker {
        val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
        val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
        return InvalidationTracker(
            this,
            _shadowTablesMap,
            _viewTables,
            "pdf_documents",
            "pdf_annotations",
            "pdf_bookmarks"
        )
    }

    public override fun clearAllTables() {
        super.performClear(false, "pdf_documents", "pdf_annotations", "pdf_bookmarks")
    }

    protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
        val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
        _typeConvertersMap[PdfDao::class] = PdfDao_Impl.getRequiredConverters()
        return _typeConvertersMap
    }

    public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
        return mutableSetOf()
    }

    public override fun createAutoMigrations(
        autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration> {
        return mutableListOf()
    }

    public override fun pdfDao(): PdfDao = _pdfDao.value
}
