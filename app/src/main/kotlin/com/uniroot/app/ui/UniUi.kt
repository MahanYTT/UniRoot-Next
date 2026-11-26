package com.uniroot.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uniroot.app.BuildConfig
import com.uniroot.app.R
import com.uniroot.app.engine.AppUpdateUiState
import com.uniroot.app.engine.CompatibilityLevel
import com.uniroot.app.engine.CompatibilityVerdict
import com.uniroot.app.engine.DeviceProfile
import com.uniroot.app.engine.DeviceTelemetry
import com.uniroot.app.engine.RunLogFile
import com.uniroot.app.newmethod.KsudClassicProfiles
import com.uniroot.app.newmethod.KsudNextProfile
import com.uniroot.app.newmethod.KsudUpdateCheckState
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class UniTab(val labelRes: Int) {
    HOME(R.string.tab_home),
    LOGS(R.string.tab_logs),
    SETTINGS(R.string.tab_settings),
}

enum class LogTone {
    DEFAULT,
    SUCCESS,
    WARNING,
    ERROR,
    PROGRESS,
}

private val ANSI_RE = Regex("\u001B\\[[0-9;]*[A-Za-z]")

internal fun formatLogTone(raw: String): Pair<String, LogTone> {
    val clean = ANSI_RE.replace(raw, "").trimEnd()
    val lower = clean.lowercase()
    val tone = when {
        clean.contains("[Success]") || clean.contains("[OK]") || clean.contains("[✓]") ||
            clean.contains("rooted ✓") || clean.contains("loaded successfully") ||
            clean.startsWith("[+]") || lower.contains("uid=0") -> LogTone.SUCCESS
        clean.contains("[Error]") || clean.contains("ERROR:") || clean.contains("FAILED") ||
            clean.contains("exception") || lower.contains("mismatch") ||
            lower.contains("crashed") || clean.contains("rc=-") -> LogTone.ERROR
        clean.contains("[!]") || lower.contains("warning") || lower.contains("skipped") ||
            lower.contains("shizuku") && lower.contains("not") -> LogTone.WARNING
        clean.startsWith("[*]") || clean.startsWith("[NewMethod]") || clean.startsWith("[Pipeline]") ||
            clean.startsWith("[Daemon]") || clean.startsWith("[Mode]") ||
            clean.startsWith("[KsudUpdate]") || clean.startsWith("[AppUpdate]") -> LogTone.PROGRESS
        else -> LogTone.DEFAULT
    }
    return clean to tone
}

data class UniUiState(
    val tab: UniTab = UniTab.HOME,
    // Default = New Exploit + KernelSU Next
    val newMethod: Boolean = true,
    val ksuNextMode: Boolean = true,
    val rooted: Boolean = false,
    val running: Boolean = false,
    // Device detection & compatibility
    val telemetry: DeviceTelemetry = DeviceTelemetry.placeholder(),
    val telemetryRefreshing: Boolean = false,
    val compatibility: CompatibilityVerdict = CompatibilityVerdict(
        level = CompatibilityLevel.CAUTION,
        badge = "Scanning…",
        headline = "Inspecting Device",
        reason = "Reading kernel KMI, architecture, and root environment…",
    ),
    // Old Method state
    val shizukuEnabled: Boolean = false,
    val shizukuActive: Boolean = false,
    val shizukuPermitted: Boolean = false,
    val profiles: List<DeviceProfile> = emptyList(),
    val selectedProfileName: String? = null,
    val useLatestKsu: Boolean = false,
    val latestKsuTag: String = "",
    val autoRootOnBoot: Boolean = false,
    val rmgStatus: String = "",
    // Execution sheet & logs
    val executionSheetVisible: Boolean = false,
    val executionSheetDismissible: Boolean = false,
    val logLines: List<String> = emptyList(),
    val runLogs: List<RunLogFile> = emptyList(),
    val logViewerFile: RunLogFile? = null,
    val logViewerContent: String = "",
    // Instant root state
    val instantRootEnabled: Boolean = false,
    val instantRootInstalled: Boolean = false,
    val instantRootOfferVisible: Boolean = false,
    val keepInstantRootVisible: Boolean = false,
    val instantRootInstallVisible: Boolean = false,
    val instantRootLogVisible: Boolean = false,
    val instantRootLogContent: String = "",
    val superuserNeededVisible: Boolean = false,
    val injectConfirmVisible: Boolean = false,
    // New Method state & ksud updater
    val newMethodBootEnabled: Boolean = false,
    val newMethodAutoSoftReboot: Boolean = true,
    val newMethodProgress: Int = 0,
    val newMethodStage: String = "",
    val ksudNextProfiles: List<KsudNextProfile> = emptyList(),
    val ksudNextProfileId: String = "",
    val ksudClassicProfiles: List<KsudClassicProfiles.ClassicProfile> = emptyList(),
    val ksudClassicProfileId: String = "",
    val ksudCheckState: KsudUpdateCheckState = KsudUpdateCheckState.Idle,
    val ksudUpdateRunning: Boolean = false,
    val ksudUpdateLog: String = "",
    val ksudUpdateSheetVisible: Boolean = false,
    val ksudUpdateProgress: Int = 0,
    val ksudUpdateStage: String = "",
    // In-app GitHub release self-updater
    val appUpdateState: AppUpdateUiState = AppUpdateUiState.Idle,
    val appUpdateSheetVisible: Boolean = false,
    val appUpdateBannerDismissedTag: String = "",
    val autoCheckAppUpdates: Boolean = true,
    // Navigation helper
    val settingsScrollTarget: String? = null,
)

interface UniActions {
    fun onTabSelected(tab: UniTab)
    fun onRun()
    fun onReopenExecutionSheet()
    fun onDisableRoot()
    fun onOpenActiveManager()
    fun onRefreshTelemetry()
    fun onCopyDiagnostics()
    fun onCopyLogs()
    fun onCopyText(text: String)
    fun onClearLiveLogs()
    fun onCloseExecutionSheet()
    // Method & flavor
    fun onRootMethodChanged(newMethod: Boolean)
    fun onKsuFlavorChanged(next: Boolean)
    // New Method
    fun onNewMethodBootChanged(enabled: Boolean)
    fun onNewMethodSoftRebootChanged(enabled: Boolean)
    fun onKsudNextProfileChanged(id: String)
    fun onKsudClassicProfileChanged(id: String)
    fun onCheckKsudUpdates(forceRefresh: Boolean)
    fun onUpdateKsud(forceRepatch: Boolean = false)
    fun onCloseKsudUpdateSheet()
    fun onDeleteKsudProfile(id: String, classic: Boolean)
    // Old Method
    fun onProfileSelected(index: Int)
    fun onShizukuChanged(enabled: Boolean)
    fun onResetProfiles()
    fun onProfileSave(profile: DeviceProfile, originalName: String?)
    fun onProfileDelete(name: String)
    fun onUseLatestKsuChanged(enabled: Boolean)
    fun onDownloadLatestKsu()
    fun onAutoRootChanged(enabled: Boolean)
    fun onCheckRmg()
    // Instant Root
    fun onInstantRootEnable()
    fun onInstantRootNotNow()
    fun onKeepInstantRoot(keep: Boolean)
    fun onInstantRootEnabledChanged(enabled: Boolean)
    fun onUninstallHelper()
    fun onInstantRootInstallNow()
    fun onInstantRootInstallLater()
    fun onOpenInstantRootLog()
    fun onCloseInstantRootLog()
    fun onOpenKsuManager()
    fun onDismissSuperuserNeeded()
    fun onInjectContinue()
    fun onInjectCancel()
    fun onOpenHelper()
    // Run logs
    fun onRunLogOpen(file: RunLogFile)
    fun onRunLogShare(file: RunLogFile)
    fun onRunLogDelete(file: RunLogFile)
    fun onRunLogViewerClose()
    // App self-updater
    fun onCheckAppUpdates(forceRefresh: Boolean, openSheet: Boolean)
    fun onOpenAppUpdateSheet()
    fun onCloseAppUpdateSheet()
    fun onDismissAppUpdateBanner(tag: String)
    fun onAutoCheckAppUpdatesChanged(enabled: Boolean)
    fun onDownloadAppUpdate()
    fun onCancelAppUpdateDownload()
    fun onRequestInstallPermission()
    fun onInstallAppUpdate()
    // Settings deep-link
    fun onJumpToLegacySettings()
    fun onConsumeSettingsScrollTarget()
}

@Composable
internal fun UniApp(
    state: UniUiState,
    actions: UniActions,
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    var aboutVisible by rememberSaveable { mutableStateOf(false) }
    var profileSheetOpen by rememberSaveable { mutableStateOf(false) }
    var editingKey by rememberSaveable { mutableStateOf<String?>(null) }

    val hasAppUpdate = state.appUpdateState is AppUpdateUiState.Available ||
        state.appUpdateState is AppUpdateUiState.ReadyToInstall ||
        state.appUpdateState is AppUpdateUiState.PermissionRequired

    UniRootTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MiuixTheme.colorScheme.background,
            topBar = {
                val (barTitle, barSubtitle) = when (state.tab) {
                    UniTab.HOME -> stringResource(R.string.app_name) to "v${BuildConfig.VERSION_NAME}"
                    UniTab.LOGS -> stringResource(R.string.tab_logs) to "${state.logLines.size} lines · ${state.runLogs.size} runs"
                    UniTab.SETTINGS -> stringResource(R.string.tab_settings) to "UniRoot Next v${BuildConfig.VERSION_NAME}"
                }
                TopAppBar(
                    title = barTitle,
                    subtitle = barSubtitle,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        when (state.tab) {
                            UniTab.HOME -> {
                                val spin = rememberSpinAngle(state.telemetryRefreshing)
                                IconButton(
                                    enabled = !state.telemetryRefreshing,
                                    onClick = actions::onRefreshTelemetry,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh device info",
                                        tint = MiuixTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(21.dp).graphicsLayer { rotationZ = spin },
                                    )
                                }
                                IconButton(onClick = { aboutVisible = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = stringResource(R.string.about),
                                        tint = MiuixTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(21.dp),
                                    )
                                }
                            }
                            UniTab.LOGS -> {
                                IconButton(
                                    enabled = state.logLines.isNotEmpty(),
                                    onClick = actions::onCopyLogs,
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Copy,
                                        contentDescription = stringResource(R.string.action_copy),
                                        tint = MiuixTheme.colorScheme.onBackground,
                                    )
                                }
                                IconButton(
                                    enabled = !state.running && state.logLines.isNotEmpty(),
                                    onClick = actions::onClearLiveLogs,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteSweep,
                                        contentDescription = "Clear session logs",
                                        tint = MiuixTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(21.dp),
                                    )
                                }
                            }
                            UniTab.SETTINGS -> {
                                IconButton(onClick = { aboutVisible = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = stringResource(R.string.about),
                                        tint = MiuixTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(21.dp),
                                    )
                                }
                            }
                        }
                    },
                )
            },
            bottomBar = {
                UniBottomNavBar(
                    selectedTab = state.tab,
                    rooted = state.rooted || state.telemetry.rootedLive,
                    running = state.running,
                    hasAppUpdate = hasAppUpdate,
                    onSelect = actions::onTabSelected,
                )
            },
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                AnimatedContent(
                    targetState = state.tab,
                    transitionSpec = { fadeIn(tween(170)) togetherWith fadeOut(tween(120)) },
                    label = "root_tab",
                ) { activeTab ->
                    when (activeTab) {
                        UniTab.HOME -> UniHomeTab(
                            state = state,
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                            onOpenProfileSheet = { profileSheetOpen = true },
                            onAddProfile = { editingKey = "" },
                        )
                        UniTab.LOGS -> UniLogsTab(
                            state = state,
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                        )
                        UniTab.SETTINGS -> UniSettingsTab(
                            state = state,
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                            onOpenProfileSheet = { profileSheetOpen = true },
                            onAddProfile = { editingKey = "" },
                            onShowAbout = { aboutVisible = true },
                        )
                    }
                }
            }

            // Root-scoped overlays & dialogs (work seamlessly from any tab)
            UniExecutionSheet(state = state, actions = actions)
            UniKsudUpdateSheet(state = state, actions = actions)
            AppUpdateSheet(state = state, actions = actions)
            RunLogViewerSheet(
                file = state.logViewerFile,
                content = state.logViewerContent,
                onShare = actions::onRunLogShare,
                onCopy = actions::onCopyText,
                onDismiss = actions::onRunLogViewerClose,
            )
            UniAboutDialog(
                show = aboutVisible,
                onCheckUpdates = { actions.onCheckAppUpdates(forceRefresh = true, openSheet = true) },
                onDismissRequest = { aboutVisible = false },
            )
            ProfileManagerSheet(
                show = profileSheetOpen,
                profiles = state.profiles,
                onEdit = { profile -> editingKey = profile.name },
                onNew = { editingKey = "" },
                onDelete = { profile -> actions.onProfileDelete(profile.name) },
                onDismiss = { profileSheetOpen = false },
            )
            if (editingKey != null) {
                val editing = state.profiles.firstOrNull { it.name == editingKey }
                ProfileEditDialog(
                    profile = editing,
                    defaultFlavor = if (state.ksuNextMode) "kernelsu_next" else "kernelsu",
                    onDismiss = { editingKey = null },
                    onSave = { profile, originalName ->
                        actions.onProfileSave(profile, originalName)
                        editingKey = null
                    },
                )
            }
            InstantRootDialogsHost(state = state, actions = actions)
        }
    }
}
