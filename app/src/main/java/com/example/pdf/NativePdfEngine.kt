package com.example.pdf

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.LruCache
import com.example.data.AnnotationToolType
import com.example.data.NormalizedPoint
import com.example.data.PdfAnnotationEntity
import com.example.data.PdfDocumentEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object NativePdfEngine {

    enum class ScanFilterMode(val label: String) {
        MAGIC_COLOR("Magic Color"),
        HIGH_CONTRAST_BW("B&W Crisp"),
        GRAYSCALE("Grayscale"),
        ORIGINAL("Original")
    }

    private val renderMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val encryptedFilePaths = ConcurrentHashMap.newKeySet<String>()

    // Memory-conscious LRU cache for rendered PDF bitmaps (keyed by path + page + width + nightMode)
    private val bitmapCache = object : LruCache<String, Bitmap>(28 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun clearBitmapCache() {
        bitmapCache.evictAll()
    }

    fun getBitmapCacheStatsKb(): Pair<Int, Int> {
        return bitmapCache.size() to bitmapCache.maxSize()
    }

    fun isPdfPasswordProtected(filePath: String): Boolean {
        return encryptedFilePaths.contains(filePath)
    }

    fun getPdfsDirectory(context: Context): File {
        val dir = File(context.filesDir, "pdfs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getExportsDirectory(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun encodePoints(points: List<NormalizedPoint>): String {
        return json.encodeToString(points)
    }

    fun decodePoints(pointsJson: String): List<NormalizedPoint> {
        if (pointsJson.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<NormalizedPoint>>(pointsJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun checkIfPdfEncrypted(filePath: String): Boolean = withContext(Dispatchers.IO) {
        if (encryptedFilePaths.contains(filePath)) return@withContext true
        val file = File(filePath)
        if (!file.exists()) return@withContext false
        renderMutex.withLock {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use {
                        encryptedFilePaths.remove(filePath)
                        false
                    }
                }
            } catch (_: SecurityException) {
                encryptedFilePaths.add(filePath)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext 0
        renderMutex.withLock {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        encryptedFilePaths.remove(file.absolutePath)
                        renderer.pageCount
                    }
                }
            } catch (_: SecurityException) {
                encryptedFilePaths.add(file.absolutePath)
                -1
            } catch (_: Exception) {
                0
            }
        }
    }

    suspend fun renderPage(
        filePath: String,
        pageIndex: Int,
        targetWidthPx: Int = 1080,
        invertForOledNight: Boolean = false
    ): Bitmap? = withContext(Dispatchers.IO) {
        val safeWidth = targetWidthPx.coerceIn(240, 2400)
        val cacheKey = "$filePath|$pageIndex|$safeWidth|$invertForOledNight"
        bitmapCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        val file = File(filePath)
        if (!file.exists()) return@withContext null

        renderMutex.withLock {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        encryptedFilePaths.remove(filePath)
                        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock null
                        renderer.openPage(pageIndex).use { page ->
                            val aspect = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                            val bmpWidth = safeWidth
                            val bmpHeight = (bmpWidth * aspect).toInt().coerceAtLeast(1)

                            val rawBitmap = Bitmap.createBitmap(
                                bmpWidth,
                                bmpHeight,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = Canvas(rawBitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(
                                rawBitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )

                            val finalBitmap = if (invertForOledNight) {
                                val inverted = Bitmap.createBitmap(
                                    bmpWidth,
                                    bmpHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                                val invertCanvas = Canvas(inverted)
                                val invertMatrix = ColorMatrix(
                                    floatArrayOf(
                                        -1f, 0f, 0f, 0f, 255f,
                                        0f, -1f, 0f, 0f, 255f,
                                        0f, 0f, -1f, 0f, 255f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    colorFilter = ColorMatrixColorFilter(invertMatrix)
                                }
                                invertCanvas.drawBitmap(rawBitmap, 0f, 0f, paint)
                                rawBitmap.recycle()
                                inverted
                            } else {
                                rawBitmap
                            }

                            bitmapCache.put(cacheKey, finalBitmap)
                            finalBitmap
                        }
                    }
                }
            } catch (_: SecurityException) {
                encryptedFilePaths.add(filePath)
                null
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun importUriToInternalStorage(
        context: Context,
        uri: Uri,
        customTitle: String? = null,
        categoryTag: String = "Local"
    ): PdfDocumentEntity? = withContext(Dispatchers.IO) {
        try {
            var displayName = customTitle?.takeIf { it.isNotBlank() } ?: "Local Document.pdf"
            if (customTitle.isNullOrBlank()) {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                }
            }
            val cleanTitle = displayName.removeSuffix(".pdf").removeSuffix(".PDF")
            val prefix = if (categoryTag.equals("Scanned", ignoreCase = true)) "scan" else "local"
            val destFile = File(
                getPdfsDirectory(context),
                "${prefix}_${System.currentTimeMillis()}_${cleanTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.pdf"
            )

            if (uri.scheme == "file") {
                val src = File(uri.path ?: return@withContext null)
                if (!src.exists()) return@withContext null
                src.copyTo(destFile, overwrite = true)
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null
            }

            val pages = getPageCount(destFile)
            if (pages == -1) {
                // Password-protected / encrypted PDF: keep file so Reader can show the graceful encrypted dialog
                return@withContext PdfDocumentEntity(
                    title = cleanTitle,
                    filePath = destFile.absolutePath,
                    pageCount = 1,
                    fileSizeBytes = destFile.length(),
                    categoryTag = "Encrypted",
                    summaryPreview = "This PDF is password-protected and cannot be displayed."
                )
            }
            if (pages <= 0) {
                destFile.delete()
                return@withContext null
            }

            val summary = if (categoryTag.equals("Scanned", ignoreCase = true)) {
                "Physical document scanned via ML Kit Document Scanner ($pages pages) stored locally."
            } else {
                "Imported from local device storage ($pages pages) ready for viewing, vector annotation, and studio editing."
            }

            PdfDocumentEntity(
                title = cleanTitle,
                filePath = destFile.absolutePath,
                pageCount = pages,
                fileSizeBytes = destFile.length(),
                categoryTag = categoryTag,
                summaryPreview = summary
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun autoDetectDevicePdfs(
        context: Context,
        existingTitles: Set<String>,
        existingPaths: Set<String>
    ): List<PdfDocumentEntity> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<PdfDocumentEntity>()
        val seenTitles = existingTitles.map { it.lowercase() }.toMutableSet()
        val seenPaths = existingPaths.toMutableSet()

        // 1. Scan accessible device directories (Downloads, Documents, app external files)
        val candidateDirs = buildList {
            context.getExternalFilesDirs(null).filterNotNull().forEach { add(it) }
            runCatching {
                add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
            }
        }

        for (dir in candidateDirs) {
            val files = dir.listFiles()?.filter {
                it.isFile && it.extension.equals("pdf", ignoreCase = true)
            }.orEmpty()
            for (file in files) {
                val cleanTitle = file.nameWithoutExtension
                if (file.absolutePath in seenPaths || cleanTitle.lowercase() in seenTitles) continue
                val pages = getPageCount(file)
                if (pages > 0) {
                    val destFile = File(
                        getPdfsDirectory(context),
                        "device_${System.currentTimeMillis()}_${cleanTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.pdf"
                    )
                    runCatching {
                        file.copyTo(destFile, overwrite = true)
                        seenPaths.add(destFile.absolutePath)
                        seenTitles.add(cleanTitle.lowercase())
                        discovered.add(
                            PdfDocumentEntity(
                                title = cleanTitle,
                                filePath = destFile.absolutePath,
                                pageCount = pages,
                                fileSizeBytes = destFile.length(),
                                categoryTag = "Device",
                                summaryPreview = "Auto-detected on phone storage ($pages pages)."
                            )
                        )
                    }
                }
            }
        }

        // 2. Query MediaStore for any indexed PDF files accessible to the app
        runCatching {
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME
            )
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
            val selectionArgs = arrayOf("application/pdf")
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(nameCol) ?: continue
                    val cleanTitle = displayName.removeSuffix(".pdf").removeSuffix(".PDF")
                    if (cleanTitle.lowercase() in seenTitles) continue
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val destFile = File(
                        getPdfsDirectory(context),
                        "device_${System.currentTimeMillis()}_${cleanTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.pdf"
                    )
                    context.contentResolver.openInputStream(contentUri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val pages = getPageCount(destFile)
                    if (pages > 0) {
                        seenTitles.add(cleanTitle.lowercase())
                        seenPaths.add(destFile.absolutePath)
                        discovered.add(
                            PdfDocumentEntity(
                                title = cleanTitle,
                                filePath = destFile.absolutePath,
                                pageCount = pages,
                                fileSizeBytes = destFile.length(),
                                categoryTag = "Device",
                                summaryPreview = "Auto-detected on phone storage ($pages pages)."
                            )
                        )
                    } else {
                        destFile.delete()
                    }
                }
            }
        }

        discovered
    }

    private fun drawAnnotationsOnNativeCanvas(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        annotations: List<PdfAnnotationEntity>
    ) {
        annotations.forEach { ann ->
            val pts = decodePoints(ann.pointsJson)
            val rawColor = ann.colorHex.toInt()
            when (ann.toolType) {
                AnnotationToolType.INK.name -> {
                    if (pts.size >= 2) {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = rawColor
                            style = Paint.Style.STROKE
                            strokeWidth = ann.strokeWidth * (pageWidth / 400f)
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                        }
                        val path = Path()
                        path.moveTo(pts.first().x * pageWidth, pts.first().y * pageHeight)
                        for (idx in 1 until pts.size) {
                            path.lineTo(pts[idx].x * pageWidth, pts[idx].y * pageHeight)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                AnnotationToolType.HIGHLIGHT.name -> {
                    if (pts.size >= 2) {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = rawColor
                            alpha = 95
                            style = Paint.Style.STROKE
                            strokeWidth = (ann.strokeWidth * 2.4f) * (pageWidth / 400f)
                            strokeCap = Paint.Cap.SQUARE
                            strokeJoin = Paint.Join.BEVEL
                        }
                        val path = Path()
                        path.moveTo(pts.first().x * pageWidth, pts.first().y * pageHeight)
                        for (idx in 1 until pts.size) {
                            path.lineTo(pts[idx].x * pageWidth, pts[idx].y * pageHeight)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                AnnotationToolType.RECTANGLE.name -> {
                    if (pts.size >= 2) {
                        val p1 = pts.first()
                        val p2 = pts.last()
                        val rect = RectF(
                            min(p1.x, p2.x) * pageWidth,
                            min(p1.y, p2.y) * pageHeight,
                            max(p1.x, p2.x) * pageWidth,
                            max(p1.y, p2.y) * pageHeight
                        )
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = rawColor
                            style = Paint.Style.STROKE
                            strokeWidth = ann.strokeWidth * (pageWidth / 400f)
                        }
                        canvas.drawRoundRect(rect, 8f, 8f, paint)
                    }
                }
                AnnotationToolType.STAMP.name -> {
                    val anchor = pts.firstOrNull() ?: NormalizedPoint(0.5f, 0.5f)
                    val cx = anchor.x * pageWidth
                    val cy = anchor.y * pageHeight
                    val stampText = ann.textContent.ifBlank { "APPROVED" }
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = rawColor
                        style = Paint.Style.STROKE
                        strokeWidth = 3.5f
                    }
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = rawColor
                        textSize = 18f * (pageWidth / 595f)
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    val textWidth = textPaint.measureText(stampText)
                    val padH = 22f
                    val padV = 14f
                    val rect = RectF(
                        cx - textWidth / 2f - padH,
                        cy - padV * 1.4f,
                        cx + textWidth / 2f + padH,
                        cy + padV
                    )
                    canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
                    canvas.drawText(stampText, cx, cy, textPaint)
                }
                AnnotationToolType.TEXT_NOTE.name -> {
                    val anchor = pts.firstOrNull() ?: NormalizedPoint(0.2f, 0.2f)
                    val x = anchor.x * pageWidth
                    val y = anchor.y * pageHeight
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(230, 30, 30, 30)
                        style = Paint.Style.FILL
                    }
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = rawColor
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    }
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = 12f * (pageWidth / 595f)
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    }
                    val content = ann.textContent.ifBlank { "Note" }
                    val w = (textPaint.measureText(content) + 28f).coerceAtMost(pageWidth * 0.65f)
                    val rect = RectF(x, y, x + w, y + 36f)
                    canvas.drawRoundRect(rect, 8f, 8f, bgPaint)
                    canvas.drawRoundRect(rect, 8f, 8f, strokePaint)
                    canvas.drawText(content, x + 12f, y + 23f, textPaint)
                }
            }
        }
    }

    suspend fun mergePdfDocuments(
        context: Context,
        docs: List<PdfDocumentEntity>,
        mergedTitle: String
    ): PdfDocumentEntity? = withContext(Dispatchers.IO) {
        if (docs.isEmpty()) return@withContext null
        val outFile = File(
            getPdfsDirectory(context),
            "merged_${System.currentTimeMillis()}_${mergedTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.pdf"
        )
        var totalPages = 0

        renderMutex.withLock {
            try {
                val outPdf = PdfDocument()
                var globalPageNumber = 1
                for (doc in docs) {
                    val srcFile = File(doc.filePath)
                    if (!srcFile.exists()) continue
                    ParcelFileDescriptor.open(srcFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            for (i in 0 until renderer.pageCount) {
                                renderer.openPage(i).use { srcPage ->
                                    val w = srcPage.width.coerceAtLeast(595)
                                    val h = srcPage.height.coerceAtLeast(842)
                                    val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                                    val c = Canvas(bmp)
                                    c.drawColor(Color.WHITE)
                                    srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, globalPageNumber++).create()
                                    val destPage = outPdf.startPage(pageInfo)
                                    destPage.canvas.drawBitmap(
                                        bmp,
                                        null,
                                        Rect(0, 0, w, h),
                                        Paint(Paint.FILTER_BITMAP_FLAG)
                                    )
                                    bmp.recycle()
                                    outPdf.finishPage(destPage)
                                    totalPages++
                                }
                            }
                        }
                    }
                }
                FileOutputStream(outFile).use { fos -> outPdf.writeTo(fos) }
                outPdf.close()
            } catch (_: Exception) {
                return@withLock null
            }
        }

        if (totalPages == 0 || !outFile.exists()) return@withContext null
        PdfDocumentEntity(
            title = mergedTitle,
            filePath = outFile.absolutePath,
            pageCount = totalPages,
            fileSizeBytes = outFile.length(),
            categoryTag = "Merged",
            summaryPreview = "Merged ${docs.size} PDF documents (${docs.joinToString { it.title }}) totaling $totalPages pages."
        )
    }

    suspend fun splitPdfDocument(
        context: Context,
        sourceDoc: PdfDocumentEntity,
        startPageOneBased: Int,
        endPageOneBased: Int,
        outputTitle: String
    ): PdfDocumentEntity? = withContext(Dispatchers.IO) {
        val srcFile = File(sourceDoc.filePath)
        if (!srcFile.exists()) return@withContext null

        val outFile = File(
            getPdfsDirectory(context),
            "split_${System.currentTimeMillis()}_${outputTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.pdf"
        )
        var extractedCount = 0

        renderMutex.withLock {
            try {
                val outPdf = PdfDocument()
                ParcelFileDescriptor.open(srcFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val startIdx = (startPageOneBased - 1).coerceIn(0, renderer.pageCount - 1)
                        val endIdx = (endPageOneBased - 1).coerceIn(startIdx, renderer.pageCount - 1)
                        var outPageNum = 1
                        for (i in startIdx..endIdx) {
                            renderer.openPage(i).use { srcPage ->
                                val w = srcPage.width.coerceAtLeast(595)
                                val h = srcPage.height.coerceAtLeast(842)
                                val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                                val c = Canvas(bmp)
                                c.drawColor(Color.WHITE)
                                srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                val pageInfo = PdfDocument.PageInfo.Builder(w, h, outPageNum++).create()
                                val destPage = outPdf.startPage(pageInfo)
                                destPage.canvas.drawBitmap(
                                    bmp,
                                    null,
                                    Rect(0, 0, w, h),
                                    Paint(Paint.FILTER_BITMAP_FLAG)
                                )
                                bmp.recycle()
                                outPdf.finishPage(destPage)
                                extractedCount++
                            }
                        }
                    }
                }
                FileOutputStream(outFile).use { fos -> outPdf.writeTo(fos) }
                outPdf.close()
            } catch (_: Exception) {
                return@withLock null
            }
        }

        if (extractedCount == 0 || !outFile.exists()) return@withContext null
        PdfDocumentEntity(
            title = outputTitle,
            filePath = outFile.absolutePath,
            pageCount = extractedCount,
            fileSizeBytes = outFile.length(),
            categoryTag = "Extracted",
            summaryPreview = "Extracted pages $startPageOneBased–$endPageOneBased from '${sourceDoc.title}' ($extractedCount pages)."
        )
    }

    suspend fun reorderAndSavePdf(
        context: Context,
        sourceDoc: PdfDocumentEntity,
        pageOrderZeroBased: List<Int>,
        outputTitle: String
    ): PdfDocumentEntity? = withContext(Dispatchers.IO) {
        if (pageOrderZeroBased.isEmpty()) return@withContext null
        val srcFile = File(sourceDoc.filePath)
        if (!srcFile.exists()) return@withContext null

        val outFile = File(
            getPdfsDirectory(context),
            "organized_${System.currentTimeMillis()}_${outputTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.pdf"
        )
        var compiledPages = 0

        renderMutex.withLock {
            try {
                val outPdf = PdfDocument()
                ParcelFileDescriptor.open(srcFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        var outPageNum = 1
                        for (srcIndex in pageOrderZeroBased) {
                            if (srcIndex < 0 || srcIndex >= renderer.pageCount) continue
                            renderer.openPage(srcIndex).use { srcPage ->
                                val w = srcPage.width.coerceAtLeast(595)
                                val h = srcPage.height.coerceAtLeast(842)
                                val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                                val c = Canvas(bmp)
                                c.drawColor(Color.WHITE)
                                srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                val pageInfo = PdfDocument.PageInfo.Builder(w, h, outPageNum++).create()
                                val destPage = outPdf.startPage(pageInfo)
                                destPage.canvas.drawBitmap(
                                    bmp,
                                    null,
                                    Rect(0, 0, w, h),
                                    Paint(Paint.FILTER_BITMAP_FLAG)
                                )
                                bmp.recycle()
                                outPdf.finishPage(destPage)
                                compiledPages++
                            }
                        }
                    }
                }
                FileOutputStream(outFile).use { fos -> outPdf.writeTo(fos) }
                outPdf.close()
            } catch (_: Exception) {
                return@withLock null
            }
        }

        if (compiledPages == 0 || !outFile.exists()) return@withContext null
        val orderSummary = pageOrderZeroBased.joinToString(", ") { "P.${it + 1}" }
        PdfDocumentEntity(
            title = outputTitle,
            filePath = outFile.absolutePath,
            pageCount = compiledPages,
            fileSizeBytes = outFile.length(),
            categoryTag = "Organized",
            summaryPreview = "Reordered & compiled $compiledPages pages ($orderSummary) from '${sourceDoc.title}'."
        )
    }

    suspend fun applyWatermarkAndPageNumbers(
        context: Context,
        sourceDoc: PdfDocumentEntity,
        watermarkText: String,
        watermarkColorArgb: Int,
        opacityFraction: Float,
        isDiagonal: Boolean,
        includePageNumbers: Boolean,
        pageNumberPosition: String, // "BOTTOM_CENTER", "BOTTOM_RIGHT", "TOP_RIGHT"
        pageNumberFormat: String,   // "PAGE_X_OF_Y", "X_SLASH_Y"
        outputTitle: String
    ): PdfDocumentEntity? = withContext(Dispatchers.IO) {
        val srcFile = File(sourceDoc.filePath)
        if (!srcFile.exists()) return@withContext null

        val outFile = File(
            getPdfsDirectory(context),
            "watermarked_${System.currentTimeMillis()}_${outputTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.pdf"
        )
        var totalPages = 0

        renderMutex.withLock {
            try {
                val outPdf = PdfDocument()
                ParcelFileDescriptor.open(srcFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val pageCount = renderer.pageCount
                        for (i in 0 until pageCount) {
                            renderer.openPage(i).use { srcPage ->
                                val w = srcPage.width.coerceAtLeast(595)
                                val h = srcPage.height.coerceAtLeast(842)
                                val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                                val c = Canvas(bmp)
                                c.drawColor(Color.WHITE)
                                srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                                val destPage = outPdf.startPage(pageInfo)
                                val pdfCanvas = destPage.canvas
                                pdfCanvas.drawBitmap(
                                    bmp,
                                    null,
                                    Rect(0, 0, w, h),
                                    Paint(Paint.FILTER_BITMAP_FLAG)
                                )
                                bmp.recycle()

                                // 1. Draw Watermark if text is provided
                                val cleanWatermark = watermarkText.trim()
                                if (cleanWatermark.isNotEmpty()) {
                                    val alphaInt = (opacityFraction.coerceIn(0.10f, 0.50f) * 255f).toInt().coerceIn(25, 130)
                                    val r = Color.red(watermarkColorArgb)
                                    val g = Color.green(watermarkColorArgb)
                                    val b = Color.blue(watermarkColorArgb)
                                    val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = Color.argb(alphaInt, r, g, b)
                                        textSize = (w * 0.095f).coerceIn(36f, 84f)
                                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                                        textAlign = Paint.Align.CENTER
                                    }
                                    pdfCanvas.save()
                                    val cx = w / 2f
                                    val cy = h / 2f
                                    if (isDiagonal) {
                                        pdfCanvas.rotate(-45f, cx, cy)
                                    }
                                    pdfCanvas.drawText(cleanWatermark.uppercase(), cx, cy, wmPaint)
                                    pdfCanvas.restore()
                                }

                                // 2. Draw Page Numbering if enabled
                                if (includePageNumbers) {
                                    val pageNumLabel = if (pageNumberFormat == "X_SLASH_Y") {
                                        "${i + 1} / $pageCount"
                                    } else {
                                        "Page ${i + 1} of $pageCount"
                                    }
                                    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = Color.rgb(45, 45, 45)
                                        textSize = (w * 0.022f).coerceIn(11f, 18f)
                                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                                    }
                                    when (pageNumberPosition) {
                                        "BOTTOM_RIGHT" -> {
                                            numPaint.textAlign = Paint.Align.RIGHT
                                            pdfCanvas.drawText(pageNumLabel, w - 36f, h - 26f, numPaint)
                                        }
                                        "TOP_RIGHT" -> {
                                            numPaint.textAlign = Paint.Align.RIGHT
                                            pdfCanvas.drawText(pageNumLabel, w - 36f, 34f, numPaint)
                                        }
                                        else -> { // BOTTOM_CENTER
                                            numPaint.textAlign = Paint.Align.CENTER
                                            pdfCanvas.drawText(pageNumLabel, w / 2f, h - 26f, numPaint)
                                        }
                                    }
                                }

                                outPdf.finishPage(destPage)
                                totalPages++
                            }
                        }
                    }
                }
                FileOutputStream(outFile).use { fos -> outPdf.writeTo(fos) }
                outPdf.close()
            } catch (_: Exception) {
                return@withLock null
            }
        }

        if (totalPages == 0 || !outFile.exists()) return@withContext null
        PdfDocumentEntity(
            title = outputTitle,
            filePath = outFile.absolutePath,
            pageCount = totalPages,
            fileSizeBytes = outFile.length(),
            categoryTag = "Watermarked",
            summaryPreview = "Watermarked (${watermarkText.ifBlank { "Page Numbered" }}) across $totalPages pages from '${sourceDoc.title}'."
        )
    }

    suspend fun exportPageAsHighResImage(
        context: Context,
        sourceDoc: PdfDocumentEntity,
        pageIndexZeroBased: Int,
        annotations: List<PdfAnnotationEntity>,
        invertForOledNight: Boolean = false
    ): File? = withContext(Dispatchers.IO) {
        val srcFile = File(sourceDoc.filePath)
        if (!srcFile.exists()) return@withContext null

        val outFile = File(
            getExportsDirectory(context),
            "${sourceDoc.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")}_Page_${pageIndexZeroBased + 1}_HighRes.png"
        )

        renderMutex.withLock {
            try {
                ParcelFileDescriptor.open(srcFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val safePageIdx = pageIndexZeroBased.coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
                        renderer.openPage(safePageIdx).use { srcPage ->
                            val aspect = srcPage.height.toFloat() / srcPage.width.toFloat().coerceAtLeast(1f)
                            // High resolution: minimum 2000px width
                            val targetW = max(srcPage.width * 3, 2000).coerceAtMost(2600)
                            val targetH = (targetW * aspect).toInt().coerceAtLeast(1)

                            val rawBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                            val rawCanvas = Canvas(rawBitmap)
                            rawCanvas.drawColor(Color.WHITE)
                            srcPage.render(
                                rawBitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                            )

                            val compositeBitmap = if (invertForOledNight) {
                                val inverted = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                                val invertCanvas = Canvas(inverted)
                                val invertMatrix = ColorMatrix(
                                    floatArrayOf(
                                        -1f, 0f, 0f, 0f, 255f,
                                        0f, -1f, 0f, 0f, 255f,
                                        0f, 0f, -1f, 0f, 255f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    colorFilter = ColorMatrixColorFilter(invertMatrix)
                                }
                                invertCanvas.drawBitmap(rawBitmap, 0f, 0f, paint)
                                rawBitmap.recycle()
                                inverted
                            } else {
                                rawBitmap
                            }

                            val finalCanvas = Canvas(compositeBitmap)
                            val pageAnns = annotations.filter { it.pageIndex == safePageIdx }
                            drawAnnotationsOnNativeCanvas(
                                canvas = finalCanvas,
                                pageWidth = targetW.toFloat(),
                                pageHeight = targetH.toFloat(),
                                annotations = pageAnns
                            )

                            FileOutputStream(outFile).use { fos ->
                                compositeBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                            }
                            compositeBitmap.recycle()
                        }
                    }
                }
                if (outFile.exists() && outFile.length() > 0L) outFile else null
            } catch (_: Exception) {
                null
            }
        }
    }

    fun printPdfDocument(context: Context, doc: PdfDocumentEntity): Boolean {
        val srcFile = File(doc.filePath)
        if (!srcFile.exists() || isPdfPasswordProtected(doc.filePath)) return false
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: return false
            val jobName = "Infinity PDF — ${doc.title}"
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder("${doc.title}.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(doc.pageCount.coerceAtLeast(1))
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (destination == null) {
                        callback?.onWriteFailed("Print destination unavailable.")
                        return
                    }
                    try {
                        FileInputStream(srcFile).use { input ->
                            ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                                val buffer = ByteArray(16 * 1024)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } >= 0) {
                                    if (cancellationSignal?.isCanceled == true) {
                                        callback?.onWriteCancelled()
                                        return
                                    }
                                    output.write(buffer, 0, bytesRead)
                                }
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.localizedMessage ?: "Failed to spool PDF to printer.")
                    }
                }
            }
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun applyScannerFilterToBitmap(
        source: Bitmap,
        filterMode: ScanFilterMode,
        rotationDegrees: Int = 0
    ): Bitmap {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        val rotated = if (normalizedRotation != 0) {
            val matrix = Matrix().apply { postRotate(normalizedRotation.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        if (filterMode == ScanFilterMode.ORIGINAL) {
            return if (rotated === source) source.copy(Bitmap.Config.ARGB_8888, false) else rotated
        }

        val out = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val cm = when (filterMode) {
            ScanFilterMode.MAGIC_COLOR -> ColorMatrix(
                floatArrayOf(
                    1.28f, 0f, 0f, 0f, -18f,
                    0f, 1.28f, 0f, 0f, -18f,
                    0f, 0f, 1.28f, 0f, -18f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ScanFilterMode.HIGH_CONTRAST_BW -> ColorMatrix(
                floatArrayOf(
                    0.55f, 0.55f, 0.55f, 0f, -95f,
                    0.55f, 0.55f, 0.55f, 0f, -95f,
                    0.55f, 0.55f, 0.55f, 0f, -95f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ScanFilterMode.GRAYSCALE -> ColorMatrix().apply { setSaturation(0f) }
            ScanFilterMode.ORIGINAL -> ColorMatrix()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(rotated, 0f, 0f, paint)
        if (rotated !== source) rotated.recycle()
        return out
    }

    fun generatePhysicalScanPresetBitmap(
        presetIndex: Int,
        pageNumberOneBased: Int,
        filterMode: ScanFilterMode = ScanFilterMode.MAGIC_COLOR,
        rotationDegrees: Int = 0
    ): Bitmap {
        val w = 720
        val h = 1018
        val raw = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(raw)
        c.drawColor(Color.rgb(248, 247, 243))

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(212, 210, 204)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        c.drawRect(18f, 18f, w - 18f, h - 18f, borderPaint)

        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(24, 24, 24)
        }
        c.drawRect(36f, 36f, w - 36f, 108f, headerBg)

        val crimsonAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(237, 28, 36)
        }
        c.drawRect(36f, 102f, w - 36f, 108f, crimsonAccent)

        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 21f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val presetTitle = when (presetIndex % 3) {
            0 -> "ARCHITECTURAL FIELD CONTRACT"
            1 -> "ENGINEERING SPEC SHEET & RECEIPT"
            else -> "WHITEBOARD TOPOLOGY & MATH NOTES"
        }
        c.drawText("$presetTitle • P.$pageNumberOneBased", 54f, 78f, headerTitlePaint)

        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 20, 20)
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(42, 42, 42)
            textSize = 14.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        var y = 152f
        c.drawText("01 // PHYSICAL DOCUMENT CAPTURE TELEMETRY", 48f, y, sectionPaint)
        y += 32f
        val lines = listOf(
            "• Captured & perspective-corrected in Infinity Document Scanner.",
            "• Native Android PdfDocument compilation with zero cloud dependencies.",
            "• High-contrast text legibility verified for OLED Night Mode inversion.",
            "• Page $pageNumberOneBased verified with embedded vector checksum and stamp box."
        )
        for (line in lines) {
            c.drawText(line, 54f, y, bodyPaint)
            y += 28f
        }

        y += 20f
        c.drawText("02 // STRUCTURED TABLE & VERIFICATION MATRIX", 48f, y, sectionPaint)
        y += 22f

        val tableRect = RectF(48f, y, w - 48f, y + 260f)
        c.drawRoundRect(tableRect, 10f, 10f, borderPaint)
        val rows = listOf(
            "Document Ref" to "INF-SCAN-00$pageNumberOneBased",
            "Resolution" to "1190 x 1684 px (High-DPI)",
            "Storage Scope" to "100% Local App filesDir/pdfs",
            "Color Profile" to filterMode.label
        )
        var rowY = y + 44f
        for ((k, v) in rows) {
            c.drawText(k, 72f, rowY, sectionPaint)
            c.drawText(v, 330f, rowY, bodyPaint)
            c.drawLine(64f, rowY + 14f, w - 64f, rowY + 14f, borderPaint)
            rowY += 56f
        }

        // Stamp verification badge
        val stampRect = RectF(w - 260f, h - 165f, w - 56f, h - 85f)
        val stampStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(237, 28, 36)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val stampText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(237, 28, 36)
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        c.drawRoundRect(stampRect, 10f, 10f, stampStroke)
        c.drawText("SCANNED P.$pageNumberOneBased", stampRect.centerX(), stampRect.centerY() + 7f, stampText)

        val result = applyScannerFilterToBitmap(raw, filterMode, rotationDegrees)
        if (result !== raw) raw.recycle()
        return result
    }

    suspend fun compileScannedBitmapsToPdf(
        context: Context,
        pageBitmaps: List<Bitmap>,
        outputTitle: String,
        filterMode: ScanFilterMode
    ): PdfDocumentEntity? = withContext(Dispatchers.IO) {
        if (pageBitmaps.isEmpty()) return@withContext null
        val cleanTitle = outputTitle.trim().ifBlank { "Scanned Physical Document" }
        val outFile = File(
            getPdfsDirectory(context),
            "scan_${System.currentTimeMillis()}_${cleanTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.pdf"
        )
        val pageWidth = 595
        val pageHeight = 842

        renderMutex.withLock {
            val pdf = PdfDocument()
            try {
                for ((idx, srcBmp) in pageBitmaps.withIndex()) {
                    if (srcBmp.isRecycled) continue
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create()
                    val page = pdf.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)

                    val filteredBmp = applyScannerFilterToBitmap(srcBmp, filterMode, 0)
                    val margin = 20f
                    val availW = pageWidth - margin * 2f
                    val availH = pageHeight - margin * 2f
                    val scale = min(availW / filteredBmp.width.toFloat(), availH / filteredBmp.height.toFloat())
                    val drawW = filteredBmp.width * scale
                    val drawH = filteredBmp.height * scale
                    val left = (pageWidth - drawW) / 2f
                    val top = (pageHeight - drawH) / 2f

                    canvas.drawBitmap(
                        filteredBmp,
                        null,
                        RectF(left, top, left + drawW, top + drawH),
                        Paint(Paint.FILTER_BITMAP_FLAG)
                    )
                    if (filteredBmp !== srcBmp) filteredBmp.recycle()
                    pdf.finishPage(page)
                }
                FileOutputStream(outFile).use { fos -> pdf.writeTo(fos) }
            } catch (_: Exception) {
                pdf.close()
                return@withLock null
            }
            pdf.close()
        }

        if (!outFile.exists() || outFile.length() == 0L) return@withContext null
        val totalPages = getPageCount(outFile).coerceAtLeast(pageBitmaps.size)
        PdfDocumentEntity(
            title = cleanTitle,
            filePath = outFile.absolutePath,
            pageCount = totalPages,
            fileSizeBytes = outFile.length(),
            categoryTag = "Scanned",
            summaryPreview = "Scanned physical document ($totalPages pages, ${filterMode.label}) stored locally."
        )
    }
}

