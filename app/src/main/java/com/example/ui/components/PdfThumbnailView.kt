package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pdf.NativePdfEngine
import com.example.ui.theme.InfinityCrispWhite
import com.example.ui.theme.InfinityMutedGray
import com.example.ui.theme.InfinitySlateElevated
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfThumbnailView(
    filePath: String,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    targetWidthPx: Int = 320,
    invertForNight: Boolean = false,
    cornerRadius: Dp = 12.dp,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap by remember(filePath, pageIndex, targetWidthPx, invertForNight) {
        mutableStateOf<Bitmap?>(null)
    }
    var isLoading by remember(filePath, pageIndex, targetWidthPx, invertForNight) {
        mutableStateOf(true)
    }

    LaunchedEffect(filePath, pageIndex, targetWidthPx, invertForNight) {
        isLoading = true
        bitmap = NativePdfEngine.renderPage(
            filePath = filePath,
            pageIndex = pageIndex,
            targetWidthPx = targetWidthPx,
            invertForOledNight = invertForNight
        )
        isLoading = false
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(InfinitySlateElevated),
        contentAlignment = Alignment.Center
    ) {
        val currentBmp = bitmap
        if (currentBmp != null && !currentBmp.isRecycled) {
            Image(
                bitmap = currentBmp.asImageBitmap(),
                contentDescription = "PDF Page ${pageIndex + 1} Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = InfinityCrispWhite,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PictureAsPdf,
                contentDescription = "PDF Thumbnail Placeholder",
                tint = InfinityMutedGray,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}

fun formatRelativeDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.US)
    return sdf.format(Date(epochMs))
}
