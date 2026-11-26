package com.uniroot.app.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniroot.app.BuildConfig
import com.uniroot.app.R
import com.uniroot.app.engine.AppReleaseInfo
import com.uniroot.app.engine.AppUpdateUiState
import com.uniroot.app.engine.AppUpdater
import com.uniroot.app.engine.DeviceProfile
import com.uniroot.app.engine.RunLogFile
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

// =========================================================================
// 1. IN-APP GITHUB SELF-UPDATER SHEET
// =========================================================================

@Composable
internal fun AppUpdateSheet(
    state: UniUiState,
    actions: UniActions,
) {
    val u = LocalUniColors.current
    val uriHandler = LocalUriHandler.current
    val upd = state.appUpdateState
    val locked = upd is AppUpdateUiState.Downloading || upd is AppUpdateUiState.Verifying

    OverlayBottomSheet(
        show = state.appUpdateSheetVisible,
        title = stringResource(R.string.app_update_title),
        allowDismiss = !locked,
        onDismissRequest = actions::onCloseAppUpdateSheet,
        endAction = {
            IconButton(enabled = !locked, onClick = actions::onCloseAppUpdateSheet) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.action_close),
                )
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val release: AppReleaseInfo? = when (upd) {
                    is AppUpdateUiState.Available -> upd.release
                    is AppUpdateUiState.UpToDate -> upd.release
                    is AppUpdateUiState.Downloading -> upd.release
                    is AppUpdateUiState.Verifying -> upd.release
                    is AppUpdateUiState.PermissionRequired -> upd.release
                    is AppUpdateUiState.ReadyToInstall -> upd.release
                    is AppUpdateUiState.Error -> upd.release
                    else -> null
                }

                // Header block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(colorResource(R.color.launcher_icon_background)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "UniRoot Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        val deltaText = when {
                            upd is AppUpdateUiState.UpToDate -> "v${BuildConfig.VERSION_NAME} · Up to date"
                            release != null -> "v${BuildConfig.VERSION_NAME} → ${release.tag}"
                            else -> "Installed v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            UniStatusBadge(
                                text = deltaText,
                                containerColor = if (upd is AppUpdateUiState.UpToDate) u.emeraldContainer else u.indigoContainer,
                                contentColor = if (upd is AppUpdateUiState.UpToDate) u.onEmeraldContainer else u.onIndigoContainer,
                                monospace = true,
                            )
                            if (release != null && release.apkSizeBytes > 0) {
                                UniStatusBadge(
                                    text = AppUpdater.formatBytes(release.apkSizeBytes),
                                    containerColor = MiuixTheme.colorScheme.surfaceVariant,
                                    contentColor = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    monospace = true,
                                )
                            }
                        }
                    }
                }

                // Release notes viewport
                if (release != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MiuixTheme.colorScheme.surfaceVariant)
                            .border(0.75.dp, u.cardBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Release ${release.tag}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = AppUpdater.formatPublishedDate(release.publishedAt),
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = u.textTertiary,
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp, max = 220.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                val notes = release.releaseNotes.ifBlank {
                                    "Signed release APK (${release.apkName}) published on GitHub Releases."
                                }
                                Text(
                                    text = formatReleaseNotesMarkdown(notes, MiuixTheme.colorScheme.onSurface, u.indigo),
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                )
                            }
                        }
                    }
                }

                // State-driven footer
                when (upd) {
                    AppUpdateUiState.Idle, AppUpdateUiState.Checking -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            LinearProgressIndicator(progress = null, modifier = Modifier.fillMaxWidth())
                            Text(
                                text = "Checking MahanYTT/UniRoot-Next releases on GitHub…",
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            )
                        }
                    }

                    is AppUpdateUiState.Available -> {
                        val sizeStr = if (upd.release.apkSizeBytes > 0) " (${AppUpdater.formatBytes(upd.release.apkSizeBytes)})" else ""
                        UniTactilePrimaryButton(
                            text = "${stringResource(R.string.app_update_download)}$sizeStr",
                            icon = Icons.Rounded.Download,
                            onClick = actions::onDownloadAppUpdate,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TextButton(
                                text = "View on GitHub",
                                onClick = { uriHandler.openUri(upd.release.htmlUrl) },
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                text = "Later",
                                onClick = actions::onCloseAppUpdateSheet,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    is AppUpdateUiState.UpToDate -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(u.emeraldContainer)
                                .border(0.75.dp, u.emeraldBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = u.emerald,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "You are on the latest release (v${BuildConfig.VERSION_NAME}).",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = u.onEmeraldContainer,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(
                                text = "Re-check",
                                onClick = { actions.onCheckAppUpdates(forceRefresh = true, openSheet = true) },
                                modifier = Modifier.weight(1f),
                            )
                            if (upd.release != null) {
                                TextButton(
                                    text = "Download ${upd.release.tag}",
                                    onClick = actions::onDownloadAppUpdate,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        if (upd.release != null) {
                            TextButton(
                                text = "Open on GitHub",
                                onClick = { uriHandler.openUri(upd.release.htmlUrl) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    is AppUpdateUiState.Downloading -> {
                        val fracRaw = if (upd.totalBytes > 0) (upd.bytesRead.toFloat() / upd.totalBytes.toFloat()).coerceIn(0f, 1f) else null
                        val fracAnim by animateFloatAsState(
                            targetValue = fracRaw ?: 0f,
                            animationSpec = tween(220),
                            label = "apk_dl_bar",
                        )
                        val pct = fracRaw?.let { (it * 100).toInt() }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${AppUpdater.formatBytes(upd.bytesRead)} / ${AppUpdater.formatBytes(upd.totalBytes)}",
                                    fontSize = 12.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                                val speedStr = if (upd.speedBytesPerSec > 0) "${AppUpdater.formatBytes(upd.speedBytesPerSec)}/s" else "…"
                                Text(
                                    text = if (pct != null) "$pct% · $speedStr" else speedStr,
                                    fontSize = 12.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = u.indigo,
                                )
                            }
                            LinearProgressIndicator(
                                progress = if (fracRaw != null) fracAnim else null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            TextButton(
                                text = stringResource(R.string.app_update_cancel),
                                onClick = actions::onCancelAppUpdateDownload,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    is AppUpdateUiState.Verifying -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            LinearProgressIndicator(progress = null, modifier = Modifier.fillMaxWidth())
                            VerificationStepRow("SHA-256 digest integrity", upd.shaPassed)
                            VerificationStepRow("APK package & signing certificate match", upd.sigPassed)
                        }
                    }

                    is AppUpdateUiState.PermissionRequired -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(u.amberContainer)
                                    .border(0.75.dp, u.amberBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = u.amber,
                                    modifier = Modifier.size(18.dp),
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Allow Install Unknown Apps",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = u.onAmberContainer,
                                    )
                                    Text(
                                        text = "${upd.shaSummary}. Enable \"Allow from this source\" in Android Settings and return here to finish installing.",
                                        fontSize = 12.sp,
                                        color = u.onAmberContainer.copy(alpha = 0.85f),
                                    )
                                }
                            }
                            UniTactilePrimaryButton(
                                text = stringResource(R.string.app_update_allow_unknown),
                                icon = Icons.Rounded.Security,
                                onClick = actions::onRequestInstallPermission,
                            )
                        }
                    }

                    is AppUpdateUiState.ReadyToInstall -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(u.emeraldContainer)
                                    .border(0.75.dp, u.emeraldBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = u.emerald,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = upd.shaSummary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = u.onEmeraldContainer,
                                )
                            }
                            if (upd.downgradeWarning) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(u.amberContainer)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = null,
                                        tint = u.amber,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Note: ${upd.release.tag} has an older versionCode than installed v${BuildConfig.VERSION_NAME}; Android may decline an in-place downgrade.",
                                        fontSize = 11.5.sp,
                                        color = u.onAmberContainer,
                                    )
                                }
                            }
                            UniTactilePrimaryButton(
                                text = "Install ${upd.release.tag}",
                                icon = Icons.Rounded.InstallMobile,
                                onClick = actions::onInstallAppUpdate,
                            )
                        }
                    }

                    is AppUpdateUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(u.roseContainer)
                                    .border(0.75.dp, u.roseBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = u.rose,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = upd.message,
                                    fontSize = 12.5.sp,
                                    color = u.onRoseContainer,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(
                                    text = "Retry",
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                    onClick = { actions.onCheckAppUpdates(forceRefresh = true, openSheet = true) },
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    text = "GitHub Releases",
                                    onClick = { uriHandler.openUri(upd.release?.htmlUrl ?: AppUpdater.RELEASES_URL) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun VerificationStepRow(label: String, passed: Boolean?) {
    val u = LocalUniColors.current
    val (icon, tint) = when (passed) {
        true -> Icons.Rounded.CheckCircle to u.emerald
        false -> Icons.Rounded.Cancel to u.rose
        null -> Icons.Rounded.RadioButtonUnchecked to u.sky
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text = label, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurface)
    }
}

private fun formatReleaseNotesMarkdown(raw: String, headingColor: Color, codeColor: Color): AnnotatedString =
    buildAnnotatedString {
        raw.lines().forEachIndexed { idx, line ->
            if (idx > 0) append('\n')
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") || trimmed.startsWith("## ") || trimmed.startsWith("# ") -> {
                    val clean = trimmed.trimStart('#').trim()
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = headingColor)) {
                        append(clean)
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    append("• ")
                    appendInlineMarkdown(trimmed.drop(2), headingColor, codeColor)
                }
                else -> appendInlineMarkdown(line, headingColor, codeColor)
            }
        }
    }

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String, boldColor: Color, codeColor: Color) {
    val tokenRe = Regex("""(\*\*[^*]+\*\*|`[^`]+`)""")
    var cursor = 0
    for (m in tokenRe.findAll(text)) {
        if (m.range.first > cursor) append(text.substring(cursor, m.range.first))
        val tok = m.value
        if (tok.startsWith("**") && tok.endsWith("**")) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = boldColor)) {
                append(tok.substring(2, tok.length - 2))
            }
        } else if (tok.startsWith("`") && tok.endsWith("`")) {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = codeColor)) {
                append(tok.substring(1, tok.length - 1))
            }
        }
        cursor = m.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

// =========================================================================
// 2. ROOT EXECUTION SHEET (4-step visual stepper + collapsible console)
// =========================================================================

@Composable
internal fun UniExecutionSheet(
    state: UniUiState,
    actions: UniActions,
) {
    val u = LocalUniColors.current
    val listState = rememberLazyListState()
    var showTechnicalLog by rememberSaveable(state.executionSheetVisible) { mutableStateOf(false) }

    LaunchedEffect(state.logLines.size) {
        if (state.logLines.isNotEmpty()) {
            listState.scrollToItem(state.logLines.lastIndex)
        }
    }

    OverlayBottomSheet(
        show = state.executionSheetVisible,
        title = if (state.newMethod) "DirtyFrag Root" else stringResource(R.string.log_title),
        allowDismiss = state.executionSheetDismissible,
        onDismissRequest = actions::onCloseExecutionSheet,
        startAction = {
            IconButton(onClick = actions::onCopyLogs, enabled = state.logLines.isNotEmpty()) {
                Icon(
                    imageVector = MiuixIcons.Copy,
                    contentDescription = stringResource(R.string.action_copy),
                )
            }
        },
        endAction = {
            IconButton(
                enabled = state.executionSheetDismissible,
                onClick = actions::onCloseExecutionSheet,
            ) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.action_close),
                )
            }
        },
        content = {
            if (state.newMethod) {
                val pct = state.newMethodProgress.coerceIn(0, 100)
                val failed = !state.running && state.newMethodStage.contains("Failed", ignoreCase = true)
                val succeeded = !state.running && pct >= 100 && !failed
                val barAnim by animateFloatAsState(
                    targetValue = pct / 100f,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    label = "exec_bar",
                )
                val steps = listOf(
                    0 to "Load DF native engine",
                    40 to "Allocate IPsec SA & SPI",
                    58 to "Stage ksud & arm exploit",
                    92 to if (state.newMethodAutoSoftReboot) "Root active & soft reboot" else "Root active",
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.newMethodStage.ifBlank { "Preparing…" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                failed -> u.rose
                                succeeded -> u.emerald
                                else -> MiuixTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$pct%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                failed -> u.rose
                                succeeded -> u.emerald
                                else -> u.indigo
                            },
                        )
                    }

                    LinearProgressIndicator(progress = barAnim, modifier = Modifier.fillMaxWidth())

                    // 4-step milestone checklist
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MiuixTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        steps.forEachIndexed { idx, (threshold, label) ->
                            val nextThreshold = steps.getOrNull(idx + 1)?.first ?: 101
                            val done = succeeded || (!failed && pct >= nextThreshold)
                            val active = state.running && pct >= threshold && pct < nextThreshold
                            val isErr = failed && pct >= threshold && pct < nextThreshold
                            PipelineStepRow(label = label, done = done, active = active, failed = isErr)
                        }
                    }

                    // Collapsible technical log
                    val chev by animateFloatAsState(if (showTechnicalLog) 180f else 0f, tween(200), label = "exec_log_chev")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                            .clickable { showTechnicalLog = !showTechnicalLog }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Live technical log (${state.logLines.size} lines)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                        )
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = chev },
                        )
                    }
                    AnimatedVisibility(
                        visible = showTechnicalLog || failed,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        UniLogConsole(
                            lines = state.logLines,
                            listState = listState,
                            modifier = Modifier.height(210.dp),
                        )
                    }

                    if (state.executionSheetDismissible) {
                        TextButton(
                            text = if (succeeded) "Done" else stringResource(R.string.action_close),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            onClick = actions::onCloseExecutionSheet,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    UniStatusBadge(
                        text = if (state.shizukuEnabled) "Shizuku · UID 2000" else "Local · App Context",
                        containerColor = u.indigoContainer,
                        contentColor = u.onIndigoContainer,
                    )
                    UniLogConsole(
                        lines = state.logLines,
                        listState = listState,
                        modifier = Modifier.height(340.dp),
                    )
                    if (state.executionSheetDismissible) {
                        TextButton(
                            text = stringResource(R.string.action_close),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            onClick = actions::onCloseExecutionSheet,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    )
}

// =========================================================================
// 3. KSUD PATCHER & UPDATER SHEET (5-step visual pipeline + scrollable log)
// =========================================================================

@Composable
internal fun UniKsudUpdateSheet(
    state: UniUiState,
    actions: UniActions,
) {
    val u = LocalUniColors.current
    var showLog by rememberSaveable(state.ksudUpdateSheetVisible) { mutableStateOf(false) }
    val pct = state.ksudUpdateProgress.coerceIn(0, 100)
    val failed = !state.ksudUpdateRunning && state.ksudUpdateStage.startsWith("ERROR:", ignoreCase = true)
    val doneAll = !state.ksudUpdateRunning && pct >= 100 && !failed
    val barAnim by animateFloatAsState(
        targetValue = pct / 100f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "ksud_bar",
    )

    val steps = listOf(
        0 to "Query latest GitHub release",
        18 to "Download official ksud (aarch64)",
        34 to "Scan ELF/DEFLATE & match KMI slots",
        58 to "Inject 8 Samsung KDP+DEFEX modules",
        88 to "Verify integrity & register profile",
    )

    OverlayBottomSheet(
        show = state.ksudUpdateSheetVisible,
        title = if (state.ksuNextMode) "Update ksud Next" else "Update ksud Classic",
        allowDismiss = !state.ksudUpdateRunning,
        onDismissRequest = actions::onCloseKsudUpdateSheet,
        endAction = {
            IconButton(
                enabled = !state.ksudUpdateRunning,
                onClick = actions::onCloseKsudUpdateSheet,
            ) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.action_close),
                )
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.ksudUpdateStage.ifBlank { "Preparing…" },
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            failed -> u.rose
                            doneAll -> u.emerald
                            else -> MiuixTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$pct%",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            failed -> u.rose
                            doneAll -> u.emerald
                            else -> u.indigo
                        },
                    )
                }

                LinearProgressIndicator(progress = barAnim, modifier = Modifier.fillMaxWidth())

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MiuixTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    steps.forEachIndexed { idx, (threshold, label) ->
                        val nextThreshold = steps.getOrNull(idx + 1)?.first ?: 101
                        val stepDone = doneAll || (!failed && pct >= nextThreshold)
                        val stepActive = state.ksudUpdateRunning && pct >= threshold && pct < nextThreshold
                        val stepErr = failed && pct >= threshold && pct < nextThreshold
                        PipelineStepRow(label = label, done = stepDone, active = stepActive, failed = stepErr)
                    }
                }

                // Collapsible, vertically scrollable technical patch log (fixes clipping bug!)
                val chev by animateFloatAsState(if (showLog || failed) 180f else 0f, tween(200), label = "ksud_log_chev")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                        .clickable { showLog = !showLog }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Technical patch log",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = chev },
                    )
                }
                AnimatedVisibility(
                    visible = showLog || failed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp, max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(u.consoleBg)
                                .border(1.dp, u.consoleBorder, RoundedCornerShape(12.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                        ) {
                            Text(
                                text = state.ksudUpdateLog.ifBlank { "Waiting for output…" },
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                color = u.consoleDefault,
                            )
                        }
                    }
                }

                if (!state.ksudUpdateRunning) {
                    TextButton(
                        text = if (doneAll) "Done" else stringResource(R.string.action_close),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = actions::onCloseKsudUpdateSheet,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    )
}

@Composable
private fun PipelineStepRow(
    label: String,
    done: Boolean,
    active: Boolean,
    failed: Boolean,
) {
    val u = LocalUniColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        when {
            failed -> Icon(Icons.Rounded.Cancel, null, tint = u.rose, modifier = Modifier.size(16.dp))
            done -> Icon(Icons.Rounded.CheckCircle, null, tint = u.emerald, modifier = Modifier.size(16.dp))
            active -> UniPulseBeacon(color = u.indigo, mode = PulseMode.RADAR, size = 16.dp)
            else -> Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = u.textTertiary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (active || done) FontWeight.Medium else FontWeight.Normal,
            color = when {
                failed -> u.rose
                active -> MiuixTheme.colorScheme.onSurface
                done -> MiuixTheme.colorScheme.onSurfaceSecondary
                else -> u.textTertiary
            },
        )
    }
}

// =========================================================================
// 4. RUN LOG VIEWER SHEET (replaces cramped dialog)
// =========================================================================

@Composable
internal fun RunLogViewerSheet(
    file: RunLogFile?,
    content: String,
    onShare: (RunLogFile) -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val u = LocalUniColors.current
    var query by rememberSaveable(file?.file?.name) { mutableStateOf("") }
    val lines = remember(content, query) {
        val all = content.lines()
        val q = query.trim()
        if (q.isEmpty()) all else all.filter { it.contains(q, ignoreCase = true) }
    }
    val listState = rememberLazyListState()

    OverlayBottomSheet(
        show = file != null,
        title = file?.file?.name ?: "Run Log",
        allowDismiss = true,
        onDismissRequest = onDismiss,
        endAction = {
            IconButton(onClick = onDismiss) {
                Icon(imageVector = MiuixIcons.Close, contentDescription = stringResource(R.string.action_close))
            }
        },
        content = {
            val entry = file ?: return@OverlayBottomSheet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "Search inside log…",
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.padding(start = 12.dp).size(18.dp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                UniLogConsole(
                    lines = lines,
                    listState = listState,
                    emptyText = "No matching lines.",
                    modifier = Modifier.height(280.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        text = stringResource(R.string.action_copy),
                        onClick = { onCopy(content) },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.log_share),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { onShare(entry) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

// =========================================================================
// 5. ABOUT DIALOG
// =========================================================================

@Composable
internal fun UniAboutDialog(
    show: Boolean,
    onCheckUpdates: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val u = LocalUniColors.current
    val uriHandler = LocalUriHandler.current
    OverlayDialog(
        show = show,
        title = stringResource(R.string.about),
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colorResource(R.color.launcher_icon_background)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(68.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                            fontSize = 12.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = u.textTertiary,
                        )
                        Text(
                            text = stringResource(R.string.made_by),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = u.indigo,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.opensource_info),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
                Text(
                    text = "https://github.com/MahanYTT/UniRoot-Next",
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = u.indigo,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/MahanYTT/UniRoot-Next")
                    },
                )
                Text(
                    text = stringResource(R.string.disclaimer),
                    fontSize = 11.5.sp,
                    color = u.textTertiary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        text = "Check Updates",
                        onClick = {
                            onDismissRequest()
                            onCheckUpdates()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.action_close),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

// =========================================================================
// 6. PROFILE MANAGER SHEET & PROFILE EDIT DIALOG
// =========================================================================

@Composable
internal fun ProfileManagerSheet(
    show: Boolean,
    profiles: List<DeviceProfile>,
    onEdit: (DeviceProfile) -> Unit,
    onNew: () -> Unit,
    onDelete: (DeviceProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    val u = LocalUniColors.current
    var pendingDelete by remember { mutableStateOf<DeviceProfile?>(null) }
    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.edit_profiles),
        allowDismiss = true,
        onDismissRequest = onDismiss,
        endAction = {
            IconButton(onClick = onDismiss) {
                Icon(imageVector = MiuixIcons.Close, contentDescription = stringResource(R.string.action_close))
            }
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(profiles, key = { it.name }) { profile ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, u.cardBorder, RoundedCornerShape(16.dp)),
                        cornerRadius = 16.dp,
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant),
                        insideMargin = PaddingValues(14.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = profile.name,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = fileSummary(profile),
                                modifier = Modifier.padding(top = 4.dp),
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(
                                    text = stringResource(R.string.action_edit),
                                    onClick = { onEdit(profile) },
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    text = stringResource(R.string.action_delete),
                                    onClick = { pendingDelete = profile },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                item(key = "new") {
                    TextButton(
                        text = stringResource(R.string.new_profile),
                        onClick = onNew,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    )
    pendingDelete?.let { victim ->
        OverlayDialog(
            show = true,
            title = stringResource(R.string.action_delete),
            onDismissRequest = { pendingDelete = null },
            content = {
                Text(text = stringResource(R.string.delete_profile_confirm, victim.name))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { pendingDelete = null },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.action_delete),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { onDelete(victim); pendingDelete = null },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }
}

private fun fileSummary(profile: DeviceProfile): String {
    fun n(path: String) = File(path).name.ifEmpty { "?" }
    val parts = mutableListOf("SO: ${n(profile.pathSo)}", "KO: ${n(profile.pathKo)}", "KSUD: ${n(profile.pathKsud)}")
    if (!profile.pathCveNormal.isNullOrEmpty()) parts.add("CVE: ${n(profile.pathCveNormal)}")
    val cveRoot = profile.pathCveRoot
    if (!cveRoot.isNullOrEmpty()) parts.add("ROOT: ${n(cveRoot)}")
    if (profile.useShizuku) parts.add("Shizuku")
    return parts.joinToString(" · ")
}

private enum class FileRole { SO, KO, KSUD, CVE_NORMAL, CVE_ROOT }

@Composable
internal fun ProfileEditDialog(
    profile: DeviceProfile?,
    defaultFlavor: String,
    onDismiss: () -> Unit,
    onSave: (DeviceProfile, String?) -> Unit,
) {
    val context = LocalContext.current
    val stateKey = profile?.name ?: "__new__"
    var name by remember(stateKey) { mutableStateOf(profile?.name ?: "") }
    var kaslr by remember(stateKey) { mutableStateOf(profile?.kaslrOffset ?: "") }
    var deviceType by remember(stateKey) { mutableStateOf(profile?.deviceType ?: "samsung") }
    var useShizuku by remember(stateKey) { mutableStateOf(profile?.useShizuku ?: false) }
    var soPath by remember(stateKey) { mutableStateOf(profile?.pathSo ?: "") }
    var koPath by remember(stateKey) { mutableStateOf(profile?.pathKo ?: "") }
    var ksudPath by remember(stateKey) { mutableStateOf(profile?.pathKsud ?: "") }
    var cveNormalPath by remember(stateKey) { mutableStateOf(profile?.pathCveNormal ?: "") }
    var cveRootPath by remember(stateKey) { mutableStateOf(profile?.pathCveRoot ?: "") }
    var pendingRole by remember { mutableStateOf<FileRole?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val role = pendingRole
        pendingRole = null
        if (uri != null && role != null) {
            val dest = copyPickedFile(context, uri)
            if (dest == null) {
                Toast.makeText(context, R.string.pick_failed, Toast.LENGTH_SHORT).show()
            } else {
                when (role) {
                    FileRole.SO -> soPath = dest.absolutePath
                    FileRole.KO -> koPath = dest.absolutePath
                    FileRole.KSUD -> ksudPath = dest.absolutePath
                    FileRole.CVE_NORMAL -> cveNormalPath = dest.absolutePath
                    FileRole.CVE_ROOT -> cveRootPath = dest.absolutePath
                }
            }
        }
    }

    OverlayDialog(
        show = true,
        title = stringResource(if (profile == null) R.string.new_profile else R.string.edit_profile),
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.field_name),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = kaslr,
                    onValueChange = { kaslr = it },
                    label = stringResource(R.string.field_kaslr),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        text = stringResource(R.string.field_device_samsung),
                        colors = if (deviceType == "samsung") ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors(),
                        onClick = { deviceType = "samsung" },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.field_device_other),
                        colors = if (deviceType == "oppo") ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors(),
                        onClick = { deviceType = "oppo" },
                        modifier = Modifier.weight(1f),
                    )
                }
                SwitchPreference(
                    checked = useShizuku,
                    onCheckedChange = { useShizuku = it },
                    title = stringResource(R.string.profile_use_shizuku),
                    summary = stringResource(R.string.profile_use_shizuku_summary),
                )
                FileField(stringResource(R.string.field_exploit_so), soPath) { pendingRole = FileRole.SO; picker.launch("*/*") }
                FileField(stringResource(R.string.field_ko), koPath) { pendingRole = FileRole.KO; picker.launch("*/*") }
                FileField(stringResource(R.string.field_ksud), ksudPath) { pendingRole = FileRole.KSUD; picker.launch("*/*") }
                FileField(stringResource(R.string.field_cve_normal), cveNormalPath) { pendingRole = FileRole.CVE_NORMAL; picker.launch("*/*") }
                FileField(stringResource(R.string.field_cve_root), cveRootPath) { pendingRole = FileRole.CVE_ROOT; picker.launch("*/*") }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.save),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            if (name.isBlank() || soPath.isBlank() || koPath.isBlank() || ksudPath.isBlank()) {
                                Toast.makeText(context, R.string.profile_missing_fields, Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            val updated = DeviceProfile(
                                name = name.trim(),
                                kaslrOffset = kaslr.trim(),
                                pathSo = soPath,
                                pathKo = koPath,
                                pathKsud = ksudPath,
                                deviceType = deviceType,
                                pathCveNormal = cveNormalPath.ifBlank { null },
                                pathCveRoot = cveRootPath.ifBlank { null },
                                flavor = profile?.flavor ?: defaultFlavor,
                                useShizuku = useShizuku,
                            )
                            onSave(updated, profile?.name)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

@Composable
private fun FileField(
    label: String,
    path: String,
    onPick: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = stringResource(R.string.choose_file),
                onClick = onPick,
                modifier = Modifier.heightIn(min = 32.dp),
            )
        }
        Text(
            text = path.substringAfterLast('/').ifEmpty { stringResource(R.string.no_file) },
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun copyPickedFile(context: Context, uri: Uri, prefix: String = ""): File? = runCatching {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: "file.bin"
    val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    val dir = File(context.getExternalFilesDir(null), "custom").apply { mkdirs() }
    val dest = File(dir, "${System.currentTimeMillis()}${if (prefix.isNotEmpty()) "-$prefix" else ""}-$safe")
    context.contentResolver.openInputStream(uri)!!.use { input ->
        dest.outputStream().use { output -> input.copyTo(output) }
    }
    dest.setReadable(true, false)
    dest.setExecutable(true, false)
    dest
}.getOrNull()

// =========================================================================
// 7. ALL 6 INSTANT ROOT DIALOGS (preserved 100% at root scope)
// =========================================================================

@Composable
internal fun InstantRootDialogsHost(
    state: UniUiState,
    actions: UniActions,
) {
    val u = LocalUniColors.current
    if (state.instantRootOfferVisible) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.instant_root_offer_title),
            onDismissRequest = actions::onInstantRootNotNow,
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.instant_root_offer_body), fontSize = 14.sp, lineHeight = 20.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextButton(
                            text = stringResource(R.string.instant_root_not_now),
                            onClick = actions::onInstantRootNotNow,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = stringResource(R.string.instant_root_enable),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            onClick = actions::onInstantRootEnable,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
        )
    }
    if (state.keepInstantRootVisible) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.instant_root_keep_title),
            onDismissRequest = { actions.onKeepInstantRoot(true) },
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.instant_root_keep_body), fontSize = 14.sp, lineHeight = 20.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextButton(
                            text = stringResource(R.string.instant_root_keep_no),
                            onClick = { actions.onKeepInstantRoot(false) },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = stringResource(R.string.instant_root_keep_yes),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            onClick = { actions.onKeepInstantRoot(true) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
        )
    }
    if (state.instantRootInstallVisible) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.instant_root_install_title),
            onDismissRequest = actions::onInstantRootInstallLater,
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.instant_root_install_body), fontSize = 14.sp, lineHeight = 20.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextButton(
                            text = stringResource(R.string.instant_root_install_later),
                            onClick = actions::onInstantRootInstallLater,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = stringResource(R.string.instant_root_install_now),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            onClick = actions::onInstantRootInstallNow,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
        )
    }
    if (state.instantRootLogVisible) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.instant_root_log),
            onDismissRequest = actions::onCloseInstantRootLog,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                        .clip(RoundedCornerShape(12.dp))
                        .background(u.consoleBg)
                        .border(1.dp, u.consoleBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    SelectionContainer {
                        Text(
                            text = state.instantRootLogContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = u.consoleDefault,
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    TextButton(
                        text = stringResource(R.string.action_close),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = actions::onCloseInstantRootLog,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }
    if (state.superuserNeededVisible) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.superuser_needed_title),
            onDismissRequest = actions::onDismissSuperuserNeeded,
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.superuser_needed_body), fontSize = 14.sp, lineHeight = 20.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextButton(
                            text = stringResource(R.string.cancel),
                            onClick = actions::onDismissSuperuserNeeded,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = stringResource(R.string.superuser_needed_open),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            onClick = actions::onOpenKsuManager,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
        )
    }
    if (state.injectConfirmVisible) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.instant_root_inject_title),
            onDismissRequest = actions::onInjectCancel,
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.instant_root_inject_body), fontSize = 14.sp, lineHeight = 20.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextButton(
                            text = stringResource(R.string.cancel),
                            onClick = actions::onInjectCancel,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = stringResource(R.string.instant_root_inject_continue),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            onClick = actions::onInjectContinue,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
        )
    }
}
