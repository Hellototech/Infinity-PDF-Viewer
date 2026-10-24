package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class GitHubReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val apkDownloadUrl: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val releaseInfo: GitHubReleaseInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object GitHubUpdateManager {

    const val GITHUB_OWNER = "Hellototech"
    const val GITHUB_REPO = "Infinity-PDF-Viewer"

    val LATEST_RELEASE_API_URL: String
        get() = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun getInstalledVersionName(): String = BuildConfig.VERSION_NAME

    suspend fun checkForUpdates(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val cleanOwner = GITHUB_OWNER.trim()
        val cleanRepo = GITHUB_REPO.trim()

        if (cleanOwner.isEmpty() ||
            cleanRepo.isEmpty() ||
            cleanOwner.equals("YOUR_GITHUB_USERNAME", ignoreCase = true)
        ) {
            return@withContext UpdateCheckResult.UpToDate(currentVersion)
        }

        var apiConnection: HttpURLConnection? = null
        try {
            val apiUrl = URL("https://api.github.com/repos/$cleanOwner/$cleanRepo/releases/latest")
            apiConnection = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "Infinity-PDF-Viewer-Android/${BuildConfig.VERSION_NAME}")
            }

            val responseCode = apiConnection.responseCode
            if (responseCode in 200..299) {
                val responseBody = apiConnection.inputStream.bufferedReader().use { it.readText() }
                return@withContext parseReleaseJson(
                    responseBody = responseBody,
                    currentVersion = currentVersion,
                    owner = cleanOwner,
                    repo = cleanRepo
                )
            }

            // Fallback to GitHub web release redirect if API is rate-limited (403/429) or 404
            return@withContext checkViaWebRedirectFallback(
                currentVersion = currentVersion,
                owner = cleanOwner,
                repo = cleanRepo
            )
        } catch (_: Exception) {
            return@withContext checkViaWebRedirectFallback(
                currentVersion = currentVersion,
                owner = cleanOwner,
                repo = cleanRepo
            )
        } finally {
            apiConnection?.disconnect()
        }
    }

    private fun parseReleaseJson(
        responseBody: String,
        currentVersion: String,
        owner: String,
        repo: String
    ): UpdateCheckResult {
        return try {
            val rootObject = jsonParser.parseToJsonElement(responseBody).jsonObject
            val rawTag = rootObject["tag_name"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (rawTag.isEmpty()) {
                return UpdateCheckResult.UpToDate(currentVersion)
            }

            val releaseName = rootObject["name"]?.jsonPrimitive?.content
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: rawTag

            val changelogBody = rootObject["body"]?.jsonPrimitive?.content
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "Performance optimizations, stability improvements, and native PDF engine updates."

            var apkUrl = ""
            val assetsArray = rootObject["assets"]?.jsonArray
            if (assetsArray != null) {
                for (element in assetsArray) {
                    val assetObj = element.jsonObject
                    val assetName = assetObj["name"]?.jsonPrimitive?.content.orEmpty()
                    val downloadUrl = assetObj["browser_download_url"]?.jsonPrimitive?.content.orEmpty()
                    if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                        apkUrl = downloadUrl
                        break
                    }
                }
            }
            if (apkUrl.isBlank()) {
                apkUrl = "https://github.com/$owner/$repo/releases/download/$rawTag/InfinityPDF-update.apk"
            }

            val remoteVersion = normalizeVersion(rawTag)
            val localVersion = normalizeVersion(currentVersion)

            if (isRemoteVersionNewer(remoteVersion, localVersion)) {
                val normalizedTag = if (rawTag.startsWith("v", ignoreCase = true)) rawTag else "v$rawTag"
                UpdateCheckResult.UpdateAvailable(
                    GitHubReleaseInfo(
                        tagName = normalizedTag,
                        name = releaseName,
                        body = changelogBody,
                        apkDownloadUrl = apkUrl
                    )
                )
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Failed to parse GitHub release metadata: ${e.localizedMessage}")
        }
    }

    private fun checkViaWebRedirectFallback(
        currentVersion: String,
        owner: String,
        repo: String
    ): UpdateCheckResult {
        var webConn: HttpURLConnection? = null
        return try {
            val webUrl = URL("https://github.com/$owner/$repo/releases/latest")
            webConn = (webUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
                setRequestProperty("User-Agent", "Infinity-PDF-Viewer-Android/${BuildConfig.VERSION_NAME}")
            }

            val code = webConn.responseCode
            val locationHeader = webConn.getHeaderField("Location").orEmpty()

            if (code in 300..399 && locationHeader.contains("/releases/tag/")) {
                val rawTag = locationHeader
                    .substringAfter("/releases/tag/")
                    .substringBefore("?")
                    .substringBefore("#")
                    .trim()

                if (rawTag.isNotEmpty()) {
                    val remoteVersion = normalizeVersion(rawTag)
                    val localVersion = normalizeVersion(currentVersion)
                    if (isRemoteVersionNewer(remoteVersion, localVersion)) {
                        val normalizedTag = if (rawTag.startsWith("v", ignoreCase = true)) rawTag else "v$rawTag"
                        return UpdateCheckResult.UpdateAvailable(
                            GitHubReleaseInfo(
                                tagName = normalizedTag,
                                name = "Infinity PDF Viewer $normalizedTag",
                                body = "New GitHub release ($normalizedTag) is ready to download and install.",
                                apkDownloadUrl = "https://github.com/$owner/$repo/releases/download/$rawTag/InfinityPDF-update.apk"
                            )
                        )
                    }
                }
                return UpdateCheckResult.UpToDate(currentVersion)
            }

            UpdateCheckResult.UpToDate(currentVersion)
        } catch (_: Exception) {
            UpdateCheckResult.Error("Unable to reach GitHub. Check your internet connection.")
        } finally {
            webConn?.disconnect()
        }
    }

    suspend fun downloadReleaseApk(
        context: Context,
        releaseInfo: GitHubReleaseInfo,
        onProgressPercent: (Int) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        val downloadUrl = releaseInfo.apkDownloadUrl.trim()
        if (downloadUrl.isEmpty()) return@withContext null

        var connection: HttpURLConnection? = null
        try {
            val updatesDir = File(context.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
            }
            val targetApkFile = File(context.cacheDir, "updates/InfinityPDF-update.apk")
            if (targetApkFile.exists()) {
                targetApkFile.delete()
            }

            val url = URL(downloadUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/octet-stream")
                setRequestProperty("User-Agent", "Infinity-PDF-Viewer-Android/${BuildConfig.VERSION_NAME}")
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                return@withContext null
            }

            val totalBytes = connection.contentLengthLong
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(targetApkFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var downloadedBytes = 0L
                    var bytesRead: Int
                    var lastReportedProgress = -1
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0L) {
                            val progress = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            if (progress != lastReportedProgress) {
                                lastReportedProgress = progress
                                onProgressPercent(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (targetApkFile.exists() && targetApkFile.length() > 0L) {
                onProgressPercent(100)
                targetApkFile
            } else {
                targetApkFile.delete()
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun triggerApkInstall(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return true
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun normalizeVersion(version: String): String {
        return version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
            .trim()
    }

    fun isRemoteVersionNewer(remoteVersion: String, localVersion: String): Boolean {
        val remoteParts = normalizeVersion(remoteVersion)
            .split(".")
            .map { it.toIntOrNull() ?: 0 }
        val localParts = normalizeVersion(localVersion)
            .split(".")
            .map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
