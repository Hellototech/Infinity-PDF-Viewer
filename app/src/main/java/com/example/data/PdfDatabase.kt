package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_documents ORDER BY lastOpenedEpochMs DESC")
    fun getAllDocuments(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): PdfDocumentEntity?

    @Query("SELECT COUNT(*) FROM pdf_documents")
    suspend fun getDocumentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: PdfDocumentEntity): Long

    @Update
    suspend fun updateDocument(doc: PdfDocumentEntity)

    @Query("DELETE FROM pdf_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("SELECT * FROM pdf_annotations WHERE documentId = :documentId ORDER BY createdAtEpochMs ASC")
    fun getAnnotationsForDocument(documentId: Long): Flow<List<PdfAnnotationEntity>>

    @Query("SELECT * FROM pdf_annotations WHERE documentId = :documentId ORDER BY createdAtEpochMs ASC")
    suspend fun getAnnotationsForDocumentSync(documentId: Long): List<PdfAnnotationEntity>

    @Query("SELECT * FROM pdf_annotations ORDER BY createdAtEpochMs DESC")
    fun getAllAnnotations(): Flow<List<PdfAnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: PdfAnnotationEntity): Long

    @Query("DELETE FROM pdf_annotations WHERE id = :id")
    suspend fun deleteAnnotationById(id: Long)

    @Query("DELETE FROM pdf_annotations WHERE documentId = :documentId")
    suspend fun deleteAnnotationsForDocument(documentId: Long)

    @Query("DELETE FROM pdf_annotations WHERE documentId = :documentId AND pageIndex = :pageIndex")
    suspend fun clearPageAnnotations(documentId: Long, pageIndex: Int)

    @Query("SELECT * FROM pdf_bookmarks WHERE documentId = :documentId ORDER BY pageIndex ASC")
    fun getBookmarksForDocument(documentId: Long): Flow<List<PdfBookmarkEntity>>

    @Query("SELECT * FROM pdf_bookmarks ORDER BY createdAtEpochMs DESC")
    fun getAllBookmarks(): Flow<List<PdfBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: PdfBookmarkEntity): Long

    @Query("DELETE FROM pdf_bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM pdf_bookmarks WHERE documentId = :documentId")
    suspend fun deleteBookmarksForDocument(documentId: Long)
}

@Database(
    entities = [
        PdfDocumentEntity::class,
        PdfAnnotationEntity::class,
        PdfBookmarkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PdfDatabase : RoomDatabase() {
    abstract fun pdfDao(): PdfDao

    companion object {
        @Volatile
        private var INSTANCE: PdfDatabase? = null

        fun getInstance(context: Context): PdfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PdfDatabase::class.java,
                    "infinity_pdf_studio.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class PdfRepository(private val dao: PdfDao) {
    val allDocuments: Flow<List<PdfDocumentEntity>> = dao.getAllDocuments()
    val allAnnotations: Flow<List<PdfAnnotationEntity>> = dao.getAllAnnotations()
    val allBookmarks: Flow<List<PdfBookmarkEntity>> = dao.getAllBookmarks()

    fun getAnnotationsForDocument(documentId: Long): Flow<List<PdfAnnotationEntity>> =
        dao.getAnnotationsForDocument(documentId)

    suspend fun getAnnotationsForDocumentSync(documentId: Long): List<PdfAnnotationEntity> =
        dao.getAnnotationsForDocumentSync(documentId)

    fun getBookmarksForDocument(documentId: Long): Flow<List<PdfBookmarkEntity>> =
        dao.getBookmarksForDocument(documentId)

    suspend fun getDocumentById(id: Long): PdfDocumentEntity? = dao.getDocumentById(id)

    suspend fun getDocumentCount(): Int = dao.getDocumentCount()

    suspend fun insertDocument(doc: PdfDocumentEntity): Long = dao.insertDocument(doc)

    suspend fun updateDocument(doc: PdfDocumentEntity) = dao.updateDocument(doc)

    suspend fun deleteDocument(id: Long) {
        dao.deleteAnnotationsForDocument(id)
        dao.deleteBookmarksForDocument(id)
        dao.deleteDocumentById(id)
    }

    suspend fun insertAnnotation(annotation: PdfAnnotationEntity): Long =
        dao.insertAnnotation(annotation)

    suspend fun deleteAnnotationById(id: Long) = dao.deleteAnnotationById(id)

    suspend fun clearPageAnnotations(documentId: Long, pageIndex: Int) =
        dao.clearPageAnnotations(documentId, pageIndex)

    suspend fun insertBookmark(bookmark: PdfBookmarkEntity): Long = dao.insertBookmark(bookmark)

    suspend fun deleteBookmarkById(id: Long) = dao.deleteBookmarkById(id)
}
