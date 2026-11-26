package com.uniroot.app.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniroot.app.BuildConfig
import com.uniroot.app.R
import com.uniroot.app.engine.AppUpdateUiState
import com.uniroot.app.newmethod.KsudUpdateCheckState
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
internal fun UniSettingsTab(
    state: UniUiState,
    actions: UniActions,
    scrollBehavior: ScrollBehavior,
    onOpenProfileSheet: () -> Unit,
    onAddProfile: () -> Unit,
    onShowAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val u = LocalUniColors.current
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    var confirmResetProfiles by remember { mutableStateOf(false) }
    var confirmUninstallHelper by remember { mutableStateOf(false) }
    var pendingDeleteKsud by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    LaunchedEffect(state.settingsScrollTarget) {
        if (state.settingsScrollTarget == "legacy") {
            listState.animateScrollToItem(4)
            actions.onConsumeSettingsScrollTarget()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 1. APP UPDATES
            item(key = "sec_app_updates_title") { SmallTitle(text = "APP UPDATES") }
            item(key = "sec_app_updates") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
                    cornerRadius = 20.dp,
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val (pillText, pillBg, pillFg, ctaLabel) = when (val st = state.appUpdateState) {
                            is AppUpdateUiState.Available -> Quad("${st.release.tag} available", u.indigoContainer, u.onIndigoContainer, "Update")
                            is AppUpdateUiState.Downloading -> Quad("Downloading…", u.skyContainer, u.onSkyContainer, "Progress")
                            is AppUpdateUiState.Verifying -> Quad("Verifying…", u.skyContainer, u.onSkyContainer, "Progress")
                            is AppUpdateUiState.ReadyToInstall -> Quad("Ready to install", u.emeraldContainer, u.onEmeraldContainer, "Install")
                            is AppUpdateUiState.PermissionRequired -> Quad("Permission needed", u.amberContainer, u.onAmberContainer, "Install")
                            is AppUpdateUiState.UpToDate -> Quad("Up to date", u.emeraldContainer, u.onEmeraldContainer, "Check")
                            is AppUpdateUiState.Checking -> Quad("Checking…", MiuixTheme.colorScheme.surfaceVariant, MiuixTheme.colorScheme.onSurfaceSecondary, "Checking")
                            is AppUpdateUiState.Error -> Quad("Check failed", u.roseContainer, u.onRoseContainer, "Retry")
                            AppUpdateUiState.Idle -> Quad("Not checked", MiuixTheme.colorScheme.surfaceVariant, MiuixTheme.colorScheme.onSurfaceSecondary, "Check")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(u.indigoContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdateAlt,
                                    contentDescription = null,
                                    tint = u.indigo,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "UniRoot Next v${BuildConfig.VERSION_NAME}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = "Build ${BuildConfig.VERSION_CODE}",
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = u.textTertiary,
                                    )
                                    UniStatusBadge(text = pillText, containerColor = pillBg, contentColor = pillFg)
                                }
                            }
                            val isBusy = state.appUpdateState is AppUpdateUiState.Checking
                            val openSheetDirectly = state.appUpdateState is AppUpdateUiState.Available ||
                                state.appUpdateState is AppUpdateUiState.Downloading ||
                                state.appUpdateState is AppUpdateUiState.Verifying ||
                                state.appUpdateState is AppUpdateUiState.ReadyToInstall ||
                                state.appUpdateState is AppUpdateUiState.PermissionRequired
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (openSheetDirectly) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh)
                                    .clickable(enabled = !isBusy) {
                                        if (openSheetDirectly) actions.onOpenAppUpdateSheet()
                                        else actions.onCheckAppUpdates(forceRefresh = true, openSheet = true)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = ctaLabel,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (openSheetDirectly) Color.White else MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                )
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                        ArrowPreference(
                            title = "Inspect latest GitHub release",
                            summary = "View changelog, SHA-256 verification & download options",
                            onClick = {
                                if (state.appUpdateState is AppUpdateUiState.Idle) {
                                    actions.onCheckAppUpdates(forceRefresh = true, openSheet = true)
                                } else {
                                    actions.onOpenAppUpdateSheet()
                                }
                            },
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                        SwitchPreference(
                            checked = state.autoCheckAppUpdates,
                            onCheckedChange = actions::onAutoCheckAppUpdatesChanged,
                            title = stringResource(R.string.app_update_auto_check),
                            summary = stringResource(R.string.app_update_auto_check_summary),
                        )
                    }
                }
            }

            // 2. NEW METHOD & KSUD BINARIES
            item(key = "sec_new_title") { SmallTitle(text = "NEW METHOD & KSUD BINARIES") }
            item(key = "sec_new") {
                val dynNext = state.ksudNextProfiles.filter { it.dynamic }
                val dynClassic = state.ksudClassicProfiles.filter { it.dynamic }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
                    cornerRadius = 20.dp,
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SwitchPreference(
                            checked = state.newMethodBootEnabled,
                            onCheckedChange = actions::onNewMethodBootChanged,
                            title = stringResource(R.string.new_method_boot_title),
                            summary = stringResource(R.string.new_method_boot_summary),
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                        SwitchPreference(
                            checked = state.newMethodAutoSoftReboot,
                            onCheckedChange = actions::onNewMethodSoftRebootChanged,
                            title = stringResource(R.string.new_method_soft_reboot_title),
                            summary = stringResource(R.string.new_method_soft_reboot_summary),
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val activeFlavor = if (state.ksuNextMode) "KernelSU Next" else "KernelSU Classic"
                            val ksudStatusText = when (val st = state.ksudCheckState) {
                                is KsudUpdateCheckState.UpToDate -> "$activeFlavor up to date (v${st.localVersion})"
                                is KsudUpdateCheckState.UpdateAvailable -> "$activeFlavor update: v${st.localVersion} → ${st.remoteTag}"
                                is KsudUpdateCheckState.Checking -> "Checking $activeFlavor releases…"
                                is KsudUpdateCheckState.Error -> "$activeFlavor v${st.localVersion} · ${st.message}"
                                KsudUpdateCheckState.Idle -> "$activeFlavor · tap Check to query GitHub"
                            }
                            Text(
                                text = ksudStatusText,
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                        .clickable { actions.onCheckKsudUpdates(forceRefresh = true) }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "Check ksud",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                        .clickable(enabled = !state.ksudUpdateRunning) { actions.onUpdateKsud(forceRepatch = true) }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Force Re-patch",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                    )
                                }
                            }
                        }

                        if (dynNext.isNotEmpty() || dynClassic.isNotEmpty()) {
                            HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                UniSectionEyebrow("Patched Dynamic Profiles")
                                dynNext.forEach { p ->
                                    DynamicKsudRow(
                                        label = p.label,
                                        tag = "Next",
                                        onDelete = { pendingDeleteKsud = p.id to false },
                                    )
                                }
                                dynClassic.forEach { p ->
                                    DynamicKsudRow(
                                        label = p.label,
                                        tag = "Classic",
                                        onDelete = { pendingDeleteKsud = p.id to true },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. LEGACY EXPLOIT & PROFILES (index 4 for scroll anchor)
            item(key = "sec_legacy_title") { SmallTitle(text = "LEGACY EXPLOIT & PROFILES") }
            item(key = "sec_legacy_profiles") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
                    cornerRadius = 20.dp,
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ArrowPreference(
                            title = stringResource(R.string.edit_profiles),
                            summary = "${state.profiles.size} profiles available for ${if (state.ksuNextMode) "KernelSU Next" else "KernelSU"}",
                            onClick = onOpenProfileSheet,
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                        ArrowPreference(
                            title = stringResource(R.string.action_add_profile),
                            summary = "Import custom exploit .so, kernelsu.ko & ksud binaries",
                            onClick = onAddProfile,
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                        ArrowPreference(
                            title = stringResource(R.string.reset_profiles),
                            summary = "Restore bundled default device profiles",
                            onClick = { confirmResetProfiles = true },
                        )
                    }
                }
            }

            item(key = "sec_legacy_rmg") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
                    cornerRadius = 20.dp,
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
                    insideMargin = PaddingValues(16.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Root My Galaxy Feed",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = state.rmgStatus.ifEmpty { stringResource(R.string.rmg_idle) },
                            fontSize = 12.5.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        )
                        TextButton(
                            text = stringResource(R.string.rmg_check),
                            onClick = actions::onCheckRmg,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item(key = "sec_legacy_latest_ksu") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
                    cornerRadius = 20.dp,
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SwitchPreference(
                            checked = state.useLatestKsu,
                            onCheckedChange = actions::onUseLatestKsuChanged,
                            title = stringResource(R.string.use_latest_ksu),
                            summary = "Experimental — overrides Old Method profile binaries with upstream GitHub builds",
                        )
                        if (state.useLatestKsu) {
                            HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(u.amberContainer)
                                        .border(0.75.dp, u.amberBorder, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = null,
                                        tint = u.amber,
                                        modifier = Modifier.size(16.dp).padding(top = 1.dp),
                                    )
                                    Text(
                                        text = if (state.latestKsuTag.isEmpty()) {
                                            stringResource(R.string.use_latest_not_downloaded)
                                        } else {
                                            stringResource(R.string.use_latest_downloaded, state.latestKsuTag)
                                        },
                                        fontSize = 12.sp,
                                        color = u.onAmberContainer,
                                    )
                                }
                                TextButton(
                                    text = stringResource(R.string.download_latest_ksu),
                                    onClick = actions::onDownloadLatestKsu,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "sec_legacy_boot") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
                    cornerRadius = 20.dp,
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SwitchPreference(
                            checked = state.autoRootOnBoot,
                            onCheckedChange = actions::onAutoRootChanged,
                            title = stringResource(R.string.auto_root_on_boot),
                            summary = stringResource(R.string.auto_root_on_boot_summary),
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                        SwitchPreference(
                            checked = state.instantRootEnabled,
                            onCheckedChange = actions::onInstantRootEnabledChanged,
                            title = stringResource(R.string.instant_root_title),
                            summary = when {
                                state.instantRootEnabled -> stringResource(R.string.instant_root_enabled_summary)
                                state.instantRootInstalled -> stringResource(R.string.instant_root_off_summary)
                                else -> stringResource(R.string.instant_root_not_installed)
                            },
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val (stText, stBg, stFg) = when {
                                state.instantRootEnabled -> Triple("Helper active", u.emeraldContainer, u.onEmeraldContainer)
                                state.instantRootInstalled -> Triple("Helper installed (Off)", MiuixTheme.colorScheme.surfaceVariant, MiuixTheme.colorScheme.onSurfaceSecondary)
                                else -> Triple(stringResource(R.string.instant_root_status_missing), u.amberContainer, u.onAmberContainer)
                            }
                            UniStatusBadge(text = stText, containerColor = stBg, contentColor = stFg)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(
                                    text = stringResource(R.string.instant_root_log),
                                    onClick = actions::onOpenInstantRootLog,
                                    modifier = Modifier.weight(1f),
                                )
                                if (state.instantRootInstalled) {
                                    TextButton(
                                        text = stringResource(R.string.instant_root_open_helper),
                                        onClick = actions::onOpenHelper,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            if (state.instantRootInstalled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    TextButton(
                                        text = stringResource(R.string.instant_root_update_helper),
                                        onClick = actions::onInstantRootInstallNow,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(
                                        text = stringResource(R.string.instant_root_uninstall_helper),
                                        onClick = { confirmUninstallHelper = true },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. DIAGNOSTICS & TOOLS
            item(key = "sec_diag_title") { SmallTitle(text = "DIAGNOSTICS & TOOLS") }
            item(key = "sec_diag") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
                    cornerRadius = 20.dp,
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ArrowPreference(
                            title = "Copy full device diagnostics",
                            summary = "Device, KMI, Knox, SELinux, manager & compatibility report",
                            onClick = actions::onCopyDiagnostics,
                        )
                        if (state.runLogs.isNotEmpty()) {
                            HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                            ArrowPreference(
                                title = "Open latest run log",
                                summary = state.runLogs.first().file.name,
                                onClick = { actions.onRunLogOpen(state.runLogs.first()) },
                            )
                        }
                    }
                }
            }

            // 5. ABOUT
            item(key = "sec_about_title") { SmallTitle(text = "ABOUT") }
            item(key = "sec_about") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
                    cornerRadius = 20.dp,
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
                    insideMargin = PaddingValues(16.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.clickable { onShowAbout() },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colorResource(R.color.launcher_icon_background)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_launcher_foreground),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = u.textTertiary,
                                    )
                                    UniStatusBadge(
                                        text = stringResource(R.string.made_by),
                                        containerColor = u.indigoContainer,
                                        contentColor = u.onIndigoContainer,
                                    )
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.opensource_info),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                .clickable { uriHandler.openUri("https://github.com/MahanYTT/UniRoot-Next") }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "GitHub Repository",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "github.com/MahanYTT/UniRoot-Next",
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = u.indigo,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = u.indigo,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmResetProfiles) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.reset_profiles),
            onDismissRequest = { confirmResetProfiles = false },
            content = {
                Text(
                    text = "Restore all bundled Old Method profiles? Custom profiles will be removed.",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { confirmResetProfiles = false },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = "Reset",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            confirmResetProfiles = false
                            actions.onResetProfiles()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    if (confirmUninstallHelper) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.instant_root_uninstall_helper),
            onDismissRequest = { confirmUninstallHelper = false },
            content = {
                Text(
                    text = stringResource(R.string.instant_root_uninstall_confirm),
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { confirmUninstallHelper = false },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.instant_root_uninstall),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            confirmUninstallHelper = false
                            actions.onUninstallHelper()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    pendingDeleteKsud?.let { (id, isClassic) ->
        OverlayDialog(
            show = true,
            title = "Delete patched ksud",
            onDismissRequest = { pendingDeleteKsud = null },
            content = {
                Text(
                    text = "Remove downloaded profile \"$id\"? Bundled profiles remain available.",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { pendingDeleteKsud = null },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.action_delete),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            actions.onDeleteKsudProfile(id, isClassic)
                            pendingDeleteKsud = null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }
}

@Composable
private fun DynamicKsudRow(
    label: String,
    tag: String,
    onDelete: () -> Unit,
) {
    val u = LocalUniColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        UniStatusBadge(
            text = "$tag · Patched",
            containerColor = u.indigoContainer,
            contentColor = u.onIndigoContainer,
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(u.roseContainer)
                .clickable { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Delete",
                tint = u.rose,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
