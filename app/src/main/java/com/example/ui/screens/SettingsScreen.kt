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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pdf.NativePdfEngine
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.InfinityCrimsonRed
import com.example.ui.theme.InfinityCrispWhite
import com.example.ui.theme.InfinityMutedGray
import com.example.ui.theme.InfinityOledBlack
import com.example.ui.theme.InfinitySlateElevated
import com.example.ui.theme.InfinitySlateGray
import com.example.update.GitHubReleaseInfo

@Composable
fun SettingsScreen(
    documentCount: Int,
    annotationCount: Int,
    bookmarkCount: Int,
    isVerticalContinuous: Boolean,
    isKeepScreenAwake: Boolean,
    isOledNightMode: Boolean,
    onToggleVerticalContinuous: () -> Unit,
    onToggleKeepScreenAwake: () -> Unit,
    onToggleOledNightMode: () -> Unit,
    onClearRenderCache: () -> Unit,
    themeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    isAutoUpdateEnabled: Boolean,
    onToggleAutoUpdate: () -> Unit,
    installedVersionName: String,
    updateStatusLabel: String,
    isCheckingForUpdate: Boolean,
    isDownloadingUpdate: Boolean,
    updateDownloadProgress: Int,
    availableUpdate: GitHubReleaseInfo?,
    onCheckForUpdates: () -> Unit,
    onUpdateNowClick: () -> Unit,
    onDismissUpdateDialog: () -> Unit
) {
    var cacheRefreshTick by remember { mutableIntStateOf(0) }
    val (usedCacheKb, maxCacheKb) = remember(cacheRefreshTick, documentCount) {
        NativePdfEngine.getBitmapCacheStatsKb()
    }
    val usedMbFormatted = remember(usedCacheKb) {
        String.format(java.util.Locale.US, "%.1f MB", usedCacheKb / 1024f)
    }
    val maxMbFormatted = remember(maxCacheKb) {
        String.format(java.util.Locale.US, "%.1f MB", maxCacheKb / 1024f)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(InfinityOledBlack)
            .padding(horizontal = 16.dp)
            .testTag("settings_screen_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Column {
                Text(
                    text = "SETTINGS & PREFERENCES",
                    style = MaterialTheme.typography.headlineLarge,
                    color = InfinityCrispWhite
                )
                Text(
                    text = "Offline Local-First PDF Utility",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfinityMutedGray
                )
            }
        }

        // 1. App Telemetry & Stats Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = InfinitySlateGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_telemetry_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingsSectionHeader(
                        icon = Icons.Filled.Settings,
                        title = "App Telemetry & Stats",
                        badge = "v$installedVersionName"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryStatPill(
                            label = "DOCUMENTS",
                            value = documentCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatPill(
                            label = "ANNOTATIONS",
                            value = annotationCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatPill(
                            label = "BOOKMARKS",
                            value = bookmarkCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TelemetryKeyValueRow(label = "Application", value = "Infinity PDF Viewer")
                    Spacer(modifier = Modifier.height(8.dp))
                    TelemetryKeyValueRow(label = "Render Engine", value = "android.graphics.pdf.PdfRenderer")
                    Spacer(modifier = Modifier.height(8.dp))
                    TelemetryKeyValueRow(label = "Storage Architecture", value = "100% Local Room SQLite")
                }
            }
        }

        // 2. Appearance & Theme Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = InfinitySlateGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_theme_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingsSectionHeader(
                        icon = Icons.Filled.Palette,
                        title = "Appearance & Theme",
                        badge = themeMode.label.uppercase()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val selected = themeMode == mode
                            val modeIcon = when (mode) {
                                AppThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                                AppThemeMode.LIGHT -> Icons.Filled.LightMode
                                AppThemeMode.DARK -> Icons.Filled.DarkMode
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) InfinityCrimsonRed else InfinitySlateElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectThemeMode(mode) }
                                    .testTag("theme_mode_${mode.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = mode.label,
                                        tint = if (selected) Color.White else InfinityCrispWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color.White else InfinityCrispWhite,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsToggleRow(
                        icon = Icons.Filled.DarkMode,
                        title = "OLED Night Inverter",
                        subtitle = "Invert white PDF pages to true OLED dark slate",
                        checked = isOledNightMode,
                        testTag = "settings_oled_night_switch",
                        onCheckedChange = { onToggleOledNightMode() }
                    )
                }
            }
        }

        // 3. Reader Preferences Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = InfinitySlateGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_reader_defaults_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingsSectionHeader(
                        icon = Icons.Filled.SwapVert,
                        title = "Reader Preferences",
                        badge = "VIEWPORT"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsToggleRow(
                        icon = Icons.Filled.SwapVert,
                        title = "Continuous Vertical Scroll",
                        subtitle = "Scroll vertically across PDF pages seamlessly",
                        checked = isVerticalContinuous,
                        testTag = "settings_continuous_scroll_switch",
                        onCheckedChange = { onToggleVerticalContinuous() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsToggleRow(
                        icon = Icons.Filled.Visibility,
                        title = "Study Mode (Keep Screen Awake)",
                        subtitle = "Prevent screen dimming while reading technical documents",
                        checked = isKeepScreenAwake,
                        testTag = "settings_study_mode_switch",
                        onCheckedChange = { onToggleKeepScreenAwake() }
                    )
                }
            }
        }

        // 4. GitHub Software Updates Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = InfinitySlateGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_github_updates_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingsSectionHeader(
                        icon = Icons.Filled.SystemUpdateAlt,
                        title = "GitHub Software Updates",
                        badge = "v$installedVersionName"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TelemetryKeyValueRow(label = "Installed Version", value = "v$installedVersionName")
                    Spacer(modifier = Modifier.height(8.dp))
                    TelemetryKeyValueRow(label = "Update Status", value = updateStatusLabel)

                    if (isDownloadingUpdate) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (updateDownloadProgress / 100f).coerceIn(0f, 1f) },
                            color = InfinityCrimsonRed,
                            trackColor = InfinitySlateElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsToggleRow(
                        icon = Icons.Filled.AutoMode,
                        title = "Auto-Update on Startup",
                        subtitle = "Search and install new GitHub APK releases on launch",
                        checked = isAutoUpdateEnabled,
                        testTag = "settings_auto_update_switch",
                        onCheckedChange = { onToggleAutoUpdate() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingForUpdate && !isDownloadingUpdate,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InfinityCrimsonRed,
                            contentColor = Color.White,
                            disabledContainerColor = InfinitySlateElevated,
                            disabledContentColor = InfinityMutedGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("check_for_updates_button")
                    ) {
                        if (isCheckingForUpdate || isDownloadingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDownloadingUpdate) {
                                    "Downloading Update ($updateDownloadProgress%)…"
                                } else {
                                    "Checking GitHub Releases…"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdateAlt,
                                contentDescription = "Check for Updates",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Check for Updates",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. Memory & Cache Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = InfinitySlateGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_cache_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingsSectionHeader(
                        icon = Icons.Filled.Memory,
                        title = "Memory & Cache",
                        badge = "$usedMbFormatted / $maxMbFormatted"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Rendered PDF page bitmaps are cached in an in-memory LRU buffer for zero-lag zooming and scrubbing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InfinityMutedGray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            NativePdfEngine.clearBitmapCache()
                            cacheRefreshTick++
                            onClearRenderCache()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InfinityCrimsonRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_render_cache_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CleaningServices,
                            contentDescription = "Clear Page Render Cache",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clear Page Render Cache",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (availableUpdate != null) {
        GitHubUpdateAlertDialog(
            releaseInfo = availableUpdate,
            installedVersionName = installedVersionName,
            isDownloading = isDownloadingUpdate,
            downloadProgress = updateDownloadProgress,
            onUpdateNow = onUpdateNowClick,
            onDismiss = onDismissUpdateDialog
        )
    }
}

@Composable
fun GitHubUpdateAlertDialog(
    releaseInfo: GitHubReleaseInfo,
    installedVersionName: String,
    isDownloading: Boolean,
    downloadProgress: Int,
    onUpdateNow: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        containerColor = InfinitySlateGray,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.SystemUpdateAlt,
                contentDescription = "New Update Available",
                tint = InfinityCrimsonRed,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Update Available (${releaseInfo.tagName})",
                    style = MaterialTheme.typography.titleLarge,
                    color = InfinityCrispWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Installed: v$installedVersionName → New: ${releaseInfo.tagName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = InfinityMutedGray
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = releaseInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = InfinityCrispWhite,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = InfinitySlateElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = releaseInfo.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = InfinityMutedGray
                        )
                    }
                }
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (downloadProgress / 100f).coerceIn(0f, 1f) },
                        color = InfinityCrimsonRed,
                        trackColor = InfinitySlateElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Downloading APK package ($downloadProgress%)…",
                        style = MaterialTheme.typography.labelSmall,
                        color = InfinityCrispWhite
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdateNow,
                enabled = !isDownloading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InfinityCrimsonRed,
                    contentColor = Color.White,
                    disabledContainerColor = InfinitySlateElevated,
                    disabledContentColor = InfinityMutedGray
                ),
                modifier = Modifier.testTag("update_dialog_update_now_button")
            ) {
                Text(
                    text = if (isDownloading) "Downloading…" else "Update Now",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDownloading,
                modifier = Modifier.testTag("update_dialog_later_button")
            ) {
                Text(
                    text = "Later",
                    color = InfinityMutedGray
                )
            }
        }
    )
}

@Composable
private fun TelemetryStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = InfinitySlateElevated,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = InfinityCrispWhite,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = InfinityMutedGray
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    badge: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InfinitySlateElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = InfinityCrimsonRed,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = InfinityCrispWhite
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = InfinitySlateElevated
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = InfinityMutedGray,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TelemetryKeyValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = InfinityMutedGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = InfinityCrispWhite
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = InfinitySlateElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (checked) InfinityCrimsonRed else InfinityCrispWhite,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = InfinityCrispWhite
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = InfinityMutedGray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = InfinityCrimsonRed,
                    uncheckedThumbColor = InfinityMutedGray,
                    uncheckedTrackColor = InfinitySlateGray
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
