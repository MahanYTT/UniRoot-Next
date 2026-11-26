package com.uniroot.app.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.uniroot.app.R
import com.uniroot.app.engine.AppUpdateUiState
import com.uniroot.app.engine.AppUpdater
import com.uniroot.app.engine.DeviceInfoPanel
import com.uniroot.app.engine.DeviceProfile
import com.uniroot.app.engine.DeviceTelemetry
import com.uniroot.app.engine.InstantRoot
import com.uniroot.app.engine.RootEngine
import com.uniroot.app.engine.RunLogFile
import com.uniroot.app.newmethod.DfEngine
import com.uniroot.app.newmethod.DfKsudUpdater
import com.uniroot.app.newmethod.KsudClassicProfiles
import com.uniroot.app.newmethod.KsudNextProfile
import com.uniroot.app.newmethod.KsudNextProfiles
import com.uniroot.app.newmethod.KsudUpdateCheckState
import com.uniroot.app.newmethod.NewMethodBootReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File

class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {

    private lateinit var engine: RootEngine

    // Navigation & Defaults (New Exploit + KernelSU Next first and default)
    private var tab by mutableStateOf(UniTab.HOME)
    private var newMethod by mutableStateOf(true)
    private var ksuNextMode by mutableStateOf(true)
    private var settingsScrollTarget by mutableStateOf<String?>(null)

    // Device telemetry & compatibility
    private var telemetry by mutableStateOf(DeviceTelemetry.placeholder())
    private var telemetryRefreshing by mutableStateOf(false)
    private var autoMatched by mutableStateOf(false)
    private var shizukuActive by mutableStateOf(false)
    private var shizukuPermitted by mutableStateOf(false)

    // Old Method state
    private var shizukuEnabled by mutableStateOf(false)
    private var profiles by mutableStateOf(listOf<DeviceProfile>())
    private var selectedProfileName by mutableStateOf<String?>(null)
    private var useLatestKsu by mutableStateOf(false)
    private var latestKsuTag by mutableStateOf("")
    private var autoRootOnBoot by mutableStateOf(false)
    private var rmgStatus by mutableStateOf("")

    // Execution sheet & logs
    private var sheetVisible by mutableStateOf(false)
    private var sheetDismissible by mutableStateOf(false)
    private var permissionTick by mutableIntStateOf(0)
    private var runLogs by mutableStateOf(listOf<RunLogFile>())
    private var logViewerFile by mutableStateOf<RunLogFile?>(null)
    private var logViewerContent by mutableStateOf("")

    // Instant Root
    private var instantRootEnabled by mutableStateOf(false)
    private var instantRootInstalled by mutableStateOf(false)
    private var instantRootOfferVisible by mutableStateOf(false)
    private var keepInstantRootVisible by mutableStateOf(false)
    private var instantRootInstallVisible by mutableStateOf(false)
    private var instantRootLogVisible by mutableStateOf(false)
    private var instantRootLogContent by mutableStateOf("")
    private var superuserNeededVisible by mutableStateOf(false)
    private var injectConfirmVisible by mutableStateOf(false)
    private var pendingInstant: InstantRoot? = null
    private var pendingProfileName by mutableStateOf<String?>(null)

    // New Method state & smart ksud updater
    private var newMethodBootEnabled by mutableStateOf(false)
    private var newMethodSoftReboot by mutableStateOf(true)
    private var newMethodProgress by mutableIntStateOf(0)
    private var newMethodStage by mutableStateOf("")
    private var ksudNextProfiles by mutableStateOf<List<KsudNextProfile>>(emptyList())
    private var ksudNextProfileId by mutableStateOf("")
    private var ksudClassicProfiles by mutableStateOf<List<KsudClassicProfiles.ClassicProfile>>(emptyList())
    private var ksudClassicProfileId by mutableStateOf("")
    private var ksudCheckState by mutableStateOf<KsudUpdateCheckState>(KsudUpdateCheckState.Idle)
    private var ksudUpdateRunning by mutableStateOf(false)
    private var ksudUpdateLog by mutableStateOf("")
    private var ksudUpdateSheetVisible by mutableStateOf(false)
    private var ksudUpdateProgress by mutableIntStateOf(0)
    private var ksudUpdateStage by mutableStateOf("")

    // In-app GitHub self-updater
    private var appUpdateState by mutableStateOf<AppUpdateUiState>(AppUpdateUiState.Idle)
    private var appUpdateSheetVisible by mutableStateOf(false)
    private var appUpdateBannerDismissedTag by mutableStateOf("")
    private var autoCheckAppUpdates by mutableStateOf(true)
    private var appDownloadJob: Job? = null
    private var appUpdateGen: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = RootEngine(applicationContext)
        engine.initialize()
        Shizuku.addRequestPermissionResultListener(this)

        val cePrefs = getSharedPreferences("uniroot_prefs", MODE_PRIVATE)
        val dePrefs = createDeviceProtectedStorageContext()
            .getSharedPreferences(NewMethodBootReceiver.PREFS, MODE_PRIVATE)

        useLatestKsu = engine.useLatestKsu
        latestKsuTag = engine.latestKsuTag()
        autoRootOnBoot = engine.autoRootOnBoot
        ksuNextMode = engine.ksuFlavor == "kernelsu_next"

        // Default to New Exploit on fresh install; preserve saved preference for returning users.
        newMethod = cePrefs.getBoolean("root_method_new", true)

        // Ensure DE boot prefs mirror default/active flavor so DfBootService never silently uses Classic.
        if (!dePrefs.contains(NewMethodBootReceiver.PREF_NEXT)) {
            dePrefs.edit().putBoolean(NewMethodBootReceiver.PREF_NEXT, ksuNextMode).apply()
        }
        newMethodSoftReboot = dePrefs.getBoolean(NewMethodBootReceiver.PREF_AUTO_SOFT_REBOOT, true)

        // Track New Method boot toggle cleanly in CE prefs while falling back to component state on upgrade.
        val receiverEnabled = runCatching {
            packageManager.getComponentEnabledSetting(
                ComponentName(this, NewMethodBootReceiver::class.java),
            ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }.getOrDefault(false)
        newMethodBootEnabled = if (cePrefs.contains("new_method_boot_enabled")) {
            cePrefs.getBoolean("new_method_boot_enabled", false)
        } else {
            receiverEnabled && !engine.instantRootEnabled
        }

        ksudNextProfiles = KsudNextProfiles.list(this)
        ksudNextProfileId = KsudNextProfiles.selected(this)?.id ?: ""
        ksudClassicProfiles = KsudClassicProfiles.list(this)
        ksudClassicProfileId = KsudClassicProfiles.selected(this)?.id ?: ""

        autoCheckAppUpdates = AppUpdater.autoCheckEnabled(this)
        appUpdateBannerDismissedTag = AppUpdater.dismissedTag(this)
        runLogs = engine.listRunLogs()

        refreshInstantRootState()
        refreshProfiles()
        refreshTelemetryAndRoot()
        checkRootMyGalaxy(silent = true)

        // Silent background checks (6h cached + ETag rate-limit safe) + cleanup stale update APKs.
        lifecycleScope.launch(Dispatchers.IO) { AppUpdater.cleanupOldApks(this@MainActivity) }
        val resumePendingInstall = AppUpdater.pendingInstallTag(this).isNotEmpty()
        if (autoCheckAppUpdates || resumePendingInstall) {
            checkAppUpdates(forceRefresh = false, openSheet = resumePendingInstall)
        }
        checkKsudUpdates(forceRefresh = false)

        setContent {
            @Suppress("UNUSED_EXPRESSION") permissionTick
            val context = LocalContext.current
            val rooted by engine.rooted.collectAsState()
            val logs by engine.logLines.collectAsState()
            val running by engine.running.collectAsState()

            val activeProfile = profiles.firstOrNull { it.name == selectedProfileName }
            val verdict = DeviceInfoPanel.evaluateCompatibility(
                telemetry = telemetry,
                newMethod = newMethod,
                selectedProfile = activeProfile,
                autoMatched = autoMatched,
                shizukuActive = shizukuActive,
                shizukuPermitted = shizukuPermitted,
            )

            UniApp(
                state = UniUiState(
                    tab = tab,
                    newMethod = newMethod,
                    ksuNextMode = ksuNextMode,
                    rooted = rooted,
                    running = running,
                    telemetry = telemetry,
                    telemetryRefreshing = telemetryRefreshing,
                    compatibility = verdict,
                    shizukuEnabled = shizukuEnabled,
                    shizukuActive = shizukuActive,
                    shizukuPermitted = shizukuPermitted,
                    profiles = profiles,
                    selectedProfileName = selectedProfileName,
                    useLatestKsu = useLatestKsu,
                    latestKsuTag = latestKsuTag,
                    autoRootOnBoot = autoRootOnBoot,
                    rmgStatus = rmgStatus,
                    executionSheetVisible = sheetVisible,
                    executionSheetDismissible = sheetDismissible,
                    logLines = logs,
                    runLogs = runLogs,
                    logViewerFile = logViewerFile,
                    logViewerContent = logViewerContent,
                    instantRootEnabled = instantRootEnabled,
                    instantRootInstalled = instantRootInstalled,
                    instantRootOfferVisible = instantRootOfferVisible,
                    keepInstantRootVisible = keepInstantRootVisible,
                    instantRootInstallVisible = instantRootInstallVisible,
                    instantRootLogVisible = instantRootLogVisible,
                    instantRootLogContent = instantRootLogContent,
                    superuserNeededVisible = superuserNeededVisible,
                    injectConfirmVisible = injectConfirmVisible,
                    newMethodBootEnabled = newMethodBootEnabled,
                    newMethodAutoSoftReboot = newMethodSoftReboot,
                    newMethodProgress = newMethodProgress,
                    newMethodStage = newMethodStage,
                    ksudNextProfiles = ksudNextProfiles,
                    ksudNextProfileId = ksudNextProfileId,
                    ksudClassicProfiles = ksudClassicProfiles,
                    ksudClassicProfileId = ksudClassicProfileId,
                    ksudCheckState = ksudCheckState,
                    ksudUpdateRunning = ksudUpdateRunning,
                    ksudUpdateLog = ksudUpdateLog,
                    ksudUpdateSheetVisible = ksudUpdateSheetVisible,
                    ksudUpdateProgress = ksudUpdateProgress,
                    ksudUpdateStage = ksudUpdateStage,
                    appUpdateState = appUpdateState,
                    appUpdateSheetVisible = appUpdateSheetVisible,
                    appUpdateBannerDismissedTag = appUpdateBannerDismissedTag,
                    autoCheckAppUpdates = autoCheckAppUpdates,
                    settingsScrollTarget = settingsScrollTarget,
                ),
                actions = object : UniActions {
                    override fun onTabSelected(tab: UniTab) { this@MainActivity.tab = tab }
                    override fun onRun() { if (newMethod) launchNewMethod() else launchRun() }
                    override fun onReopenExecutionSheet() { sheetVisible = true }
                    override fun onDisableRoot() = disableRoot()
                    override fun onOpenActiveManager() = openActiveManager()
                    override fun onRefreshTelemetry() = refreshTelemetryAndRoot()
                    override fun onCopyDiagnostics() {
                        val activeProf = profiles.firstOrNull { it.name == selectedProfileName }
                        val v = DeviceInfoPanel.evaluateCompatibility(
                            telemetry, newMethod, activeProf, autoMatched, shizukuActive, shizukuPermitted,
                        )
                        val report = DeviceInfoPanel.formatDiagnosticsReport(telemetry, v, newMethod, ksuNextMode)
                        copyTextToClipboard("uniroot-diagnostics", report, R.string.diagnostics_copied)
                    }
                    override fun onCopyLogs() = copyLogs()
                    override fun onCopyText(text: String) = copyTextToClipboard("uniroot-log", text, R.string.action_copy)
                    override fun onClearLiveLogs() {
                        if (!engine.running.value) engine.clearLogs()
                    }
                    override fun onCloseExecutionSheet() { if (sheetDismissible) sheetVisible = false }

                    override fun onRootMethodChanged(newMethod: Boolean) {
                        this@MainActivity.newMethod = newMethod
                        getSharedPreferences("uniroot_prefs", MODE_PRIVATE)
                            .edit().putBoolean("root_method_new", newMethod).apply()
                        if (newMethod) {
                            engine.appendLog("[NewMethod] DirtyFrag engine selected (CVE-2026-43284)")
                            checkKsudUpdates(forceRefresh = false)
                        }
                    }

                    override fun onKsuFlavorChanged(next: Boolean) {
                        ksuNextMode = next
                        engine.ksuFlavor = if (next) "kernelsu_next" else "kernelsu"
                        createDeviceProtectedStorageContext()
                            .getSharedPreferences(NewMethodBootReceiver.PREFS, MODE_PRIVATE)
                            .edit()
                            .putBoolean(NewMethodBootReceiver.PREF_NEXT, next)
                            .apply()
                        refreshProfiles()
                        checkKsudUpdates(forceRefresh = false)
                    }

                    override fun onNewMethodBootChanged(enabled: Boolean) {
                        newMethodBootEnabled = enabled
                        getSharedPreferences("uniroot_prefs", MODE_PRIVATE)
                            .edit().putBoolean("new_method_boot_enabled", enabled).apply()
                        createDeviceProtectedStorageContext()
                            .getSharedPreferences(NewMethodBootReceiver.PREFS, MODE_PRIVATE)
                            .edit().putBoolean(NewMethodBootReceiver.PREF_NEXT, ksuNextMode).apply()
                        syncBootReceiverEnabled()
                    }

                    override fun onNewMethodSoftRebootChanged(enabled: Boolean) {
                        newMethodSoftReboot = enabled
                        createDeviceProtectedStorageContext()
                            .getSharedPreferences(NewMethodBootReceiver.PREFS, MODE_PRIVATE)
                            .edit().putBoolean(NewMethodBootReceiver.PREF_AUTO_SOFT_REBOOT, enabled).apply()
                    }

                    override fun onKsudNextProfileChanged(id: String) {
                        ksudNextProfileId = id
                        engine.ksudNextProfile = id
                        val label = ksudNextProfiles.firstOrNull { it.id == id }?.label ?: id
                        engine.appendLog("[NewMethod] ksud Next profile: $label")
                    }

                    override fun onKsudClassicProfileChanged(id: String) {
                        ksudClassicProfileId = id
                        KsudClassicProfiles.setSelected(this@MainActivity, id)
                        engine.appendLog("[NewMethod] ksud Classic profile: $id")
                    }

                    override fun onCheckKsudUpdates(forceRefresh: Boolean) = checkKsudUpdates(forceRefresh)

                    override fun onUpdateKsud(forceRepatch: Boolean) {
                        if (ksudUpdateRunning) return
                        ksudUpdateRunning = true
                        ksudUpdateLog = ""
                        ksudUpdateProgress = 5
                        ksudUpdateStage = "Querying latest release…"
                        ksudUpdateSheetVisible = true
                        val classic = !ksuNextMode
                        lifecycleScope.launch {
                            var active = true
                            var failed = false
                            val result = runCatching {
                                withContext(Dispatchers.IO) {
                                    DfKsudUpdater.run(
                                        context = this@MainActivity,
                                        classic = classic,
                                        forceRepatch = forceRepatch,
                                        onLog = { line ->
                                            runOnUiThread {
                                                if (active) ksudUpdateLog += line + "\n"
                                            }
                                            engine.appendLog("[KsudUpdate] $line")
                                        },
                                        onProgress = { pct, stage ->
                                            runOnUiThread {
                                                if (active) {
                                                    ksudUpdateProgress = pct
                                                    if (stage.isNotEmpty()) ksudUpdateStage = stage
                                                }
                                            }
                                        },
                                    )
                                }
                            }.getOrElse {
                                failed = true
                                "ERROR: ${it.message ?: it.javaClass.simpleName}"
                            }
                            active = false
                            ksudUpdateLog += "\n$result"
                            ksudUpdateStage = result
                            if (!failed) ksudUpdateProgress = 100
                            ksudNextProfiles = KsudNextProfiles.list(this@MainActivity)
                            ksudNextProfileId = KsudNextProfiles.selected(this@MainActivity)?.id ?: ""
                            engine.ksudNextProfile = ksudNextProfileId
                            ksudClassicProfiles = KsudClassicProfiles.list(this@MainActivity)
                            ksudClassicProfileId = KsudClassicProfiles.selected(this@MainActivity)?.id ?: ""
                            ksudUpdateRunning = false
                            checkKsudUpdates(forceRefresh = false)
                        }
                    }

                    override fun onCloseKsudUpdateSheet() {
                        if (!ksudUpdateRunning) ksudUpdateSheetVisible = false
                    }

                    override fun onDeleteKsudProfile(id: String, classic: Boolean) {
                        if (classic) {
                            if (!KsudClassicProfiles.deleteLatest(this@MainActivity)) return
                            engine.appendLog("[KsudUpdate] Classic profile deleted: $id")
                            ksudClassicProfiles = KsudClassicProfiles.list(this@MainActivity)
                            ksudClassicProfileId = KsudClassicProfiles.selected(this@MainActivity)?.id ?: ""
                        } else {
                            if (!KsudNextProfiles.deleteDynamic(this@MainActivity, id)) return
                            engine.appendLog("[KsudUpdate] Next profile deleted: $id")
                            ksudNextProfiles = KsudNextProfiles.list(this@MainActivity)
                            ksudNextProfileId = KsudNextProfiles.selected(this@MainActivity)?.id ?: ""
                            engine.ksudNextProfile = ksudNextProfileId
                        }
                        checkKsudUpdates(forceRefresh = false)
                    }

                    // Old Method
                    override fun onProfileSelected(index: Int) {
                        val newName = profiles.getOrNull(index)?.name
                        if (newName == null || newName == selectedProfileName) return
                        if (instantRootEnabled) {
                            pendingProfileName = newName
                            keepInstantRootVisible = true
                        } else {
                            selectedProfileName = newName
                            autoMatched = false
                        }
                    }

                    override fun onShizukuChanged(enabled: Boolean) { shizukuEnabled = enabled }
                    override fun onResetProfiles() = resetProfiles()
                    override fun onProfileSave(profile: DeviceProfile, originalName: String?) {
                        engine.addOrUpdateProfile(profile, originalName)
                        refreshProfiles()
                        selectedProfileName = profile.name
                        Toast.makeText(context, R.string.profile_saved, Toast.LENGTH_SHORT).show()
                    }

                    override fun onProfileDelete(name: String) {
                        engine.deleteProfile(name)
                        refreshProfiles()
                        if (selectedProfileName == name) {
                            selectedProfileName = matchProfileForActiveFlavor()
                        }
                    }

                    override fun onUseLatestKsuChanged(enabled: Boolean) {
                        useLatestKsu = enabled
                        engine.useLatestKsu = enabled
                        if (enabled) {
                            Toast.makeText(context, R.string.use_latest_enabled_toast, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onDownloadLatestKsu() = downloadLatestKsu()

                    override fun onAutoRootChanged(enabled: Boolean) {
                        if (enabled && instantRootEnabled) {
                            Toast.makeText(context, R.string.auto_root_exclusive, Toast.LENGTH_LONG).show()
                            return
                        }
                        autoRootOnBoot = enabled
                        engine.autoRootOnBoot = enabled
                        if (enabled && Build.VERSION.SDK_INT >= 33) {
                            runCatching {
                                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                            }
                        }
                        if (enabled && !Settings.canDrawOverlays(this@MainActivity)) {
                            runCatching {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName"),
                                    ),
                                )
                            }
                        }
                    }

                    override fun onCheckRmg() = checkRootMyGalaxy(silent = false)

                    // Instant Root
                    override fun onInstantRootEnable() { instantRootOfferVisible = false; enableInstantRoot() }
                    override fun onInstantRootNotNow() {
                        instantRootOfferVisible = false
                        engine.instantRootPrompted = true
                        lifecycleScope.launch { delay(12_000); restartKsuManager() }
                    }
                    override fun onKeepInstantRoot(keep: Boolean) {
                        keepInstantRootVisible = false
                        pendingProfileName?.let { selectedProfileName = it }
                        autoMatched = false
                        pendingProfileName = null
                        if (!keep) disableInstantRoot()
                    }
                    override fun onInstantRootEnabledChanged(enabled: Boolean) = applyInstantRootEnabled(enabled)
                    override fun onUninstallHelper() = uninstallHelper()
                    override fun onInstantRootInstallNow() {
                        instantRootInstallVisible = false
                        val intent = InstantRoot(this@MainActivity).helperInstallIntent()
                        if (intent != null) {
                            runCatching { startActivity(intent) }
                                .onFailure { Toast.makeText(this@MainActivity, R.string.instant_root_install_failed, Toast.LENGTH_LONG).show() }
                        } else {
                            Toast.makeText(this@MainActivity, R.string.instant_root_install_failed, Toast.LENGTH_LONG).show()
                        }
                    }
                    override fun onInstantRootInstallLater() { instantRootInstallVisible = false }
                    override fun onOpenInstantRootLog() {
                        instantRootLogVisible = true
                        instantRootLogContent = "Loading…"
                        lifecycleScope.launch {
                            val instant = InstantRoot(this@MainActivity)
                            val sb = StringBuilder(
                                instant.readLog().ifBlank { "No instant root log yet — press Enable first." },
                            )
                            val helperBootLog = withContext(Dispatchers.IO) {
                                instant.su(
                                    "cat /storage/emulated/0/Android/data/${InstantRoot.PKG_REROOT}/files/autoroot.log",
                                    timeoutSec = 20,
                                )?.second
                            }
                            if (!helperBootLog.isNullOrBlank()) {
                                sb.append("\n----- helper boot log (DFReroot) -----\n").append(helperBootLog)
                            }
                            val bootLog = withContext(Dispatchers.IO) {
                                instant.su("cat /data/system/dfreroot-boot.log", timeoutSec = 20)?.second
                            }
                            if (!bootLog.isNullOrBlank()) {
                                sb.append("\n----- /data/system/dfreroot-boot.log -----\n").append(bootLog)
                            }
                            instantRootLogContent = sb.toString()
                        }
                    }
                    override fun onCloseInstantRootLog() { instantRootLogVisible = false }
                    override fun onOpenKsuManager() {
                        superuserNeededVisible = false
                        restartKsuManager()
                    }
                    override fun onDismissSuperuserNeeded() { superuserNeededVisible = false }
                    override fun onInjectContinue() {
                        injectConfirmVisible = false
                        continueInstantRootSetup()
                    }
                    override fun onInjectCancel() {
                        injectConfirmVisible = false
                        pendingInstant?.record("[InstantRoot] Cancelled before injection.")
                        pendingInstant = null
                    }
                    override fun onOpenHelper() {
                        runCatching {
                            startActivity(
                                Intent().setClassName(
                                    InstantRoot.PKG_REROOT,
                                    InstantRoot.PKG_REROOT + ".MainActivity",
                                ).putExtra("manual", true)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }.onFailure {
                            Toast.makeText(this@MainActivity, R.string.instant_root_need_root, Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Run logs
                    override fun onRunLogOpen(file: RunLogFile) {
                        logViewerContent = runCatching { file.file.readText() }.getOrDefault("")
                        logViewerFile = file
                    }
                    override fun onRunLogShare(file: RunLogFile) = shareRunLog(file.file)
                    override fun onRunLogDelete(file: RunLogFile) {
                        engine.deleteRunLog(file.file)
                        runLogs = engine.listRunLogs()
                        if (logViewerFile == file) { logViewerFile = null; logViewerContent = "" }
                    }
                    override fun onRunLogViewerClose() { logViewerFile = null; logViewerContent = "" }

                    // App self-updater
                    override fun onCheckAppUpdates(forceRefresh: Boolean, openSheet: Boolean) =
                        checkAppUpdates(forceRefresh, openSheet)
                    override fun onOpenAppUpdateSheet() { appUpdateSheetVisible = true }
                    override fun onCloseAppUpdateSheet() {
                        val busy = appUpdateState is AppUpdateUiState.Downloading || appUpdateState is AppUpdateUiState.Verifying
                        if (!busy) appUpdateSheetVisible = false
                    }
                    override fun onDismissAppUpdateBanner(tag: String) {
                        appUpdateBannerDismissedTag = tag
                        AppUpdater.setDismissedTag(this@MainActivity, tag)
                    }
                    override fun onAutoCheckAppUpdatesChanged(enabled: Boolean) {
                        autoCheckAppUpdates = enabled
                        AppUpdater.setAutoCheckEnabled(this@MainActivity, enabled)
                    }
                    override fun onDownloadAppUpdate() = startAppUpdateDownload()
                    override fun onCancelAppUpdateDownload() {
                        appUpdateGen++
                        appDownloadJob?.cancel()
                        appDownloadJob = null
                        val rel = (appUpdateState as? AppUpdateUiState.Downloading)?.release
                        appUpdateState = if (rel != null) {
                            val current = com.uniroot.app.engine.SemVer.parse(com.uniroot.app.BuildConfig.VERSION_NAME)
                            val remote = com.uniroot.app.engine.SemVer.parse(rel.versionName)
                            if (remote != null && current != null && remote > current) AppUpdateUiState.Available(rel)
                            else AppUpdateUiState.UpToDate(rel, System.currentTimeMillis())
                        } else {
                            AppUpdateUiState.Idle
                        }
                    }
                    override fun onRequestInstallPermission() {
                        val tag = (appUpdateState as? AppUpdateUiState.PermissionRequired)?.release?.tag ?: ""
                        if (tag.isNotEmpty()) AppUpdater.setPendingInstallTag(this@MainActivity, tag)
                        runCatching { startActivity(AppUpdater.unknownSourcesIntent(this@MainActivity)) }
                            .onFailure { Toast.makeText(this@MainActivity, R.string.app_update_install_failed, Toast.LENGTH_LONG).show() }
                    }
                    override fun onInstallAppUpdate() {
                        val st = appUpdateState as? AppUpdateUiState.ReadyToInstall ?: return
                        if (!packageManager.canRequestPackageInstalls()) {
                            AppUpdater.setPendingInstallTag(this@MainActivity, st.release.tag)
                            appUpdateState = AppUpdateUiState.PermissionRequired(
                                st.release, st.apkPath, st.shaSummary, st.downgradeWarning,
                            )
                            return
                        }
                        AppUpdater.setPendingInstallTag(this@MainActivity, "")
                        val intent = AppUpdater.installIntent(this@MainActivity, st.apkPath)
                        if (intent != null) {
                            runCatching { startActivity(intent) }
                                .onFailure { Toast.makeText(this@MainActivity, R.string.app_update_install_failed, Toast.LENGTH_LONG).show() }
                        } else {
                            Toast.makeText(this@MainActivity, R.string.app_update_install_failed, Toast.LENGTH_LONG).show()
                        }
                    }

                    // Settings deep-link
                    override fun onJumpToLegacySettings() {
                        settingsScrollTarget = "legacy"
                        tab = UniTab.SETTINGS
                    }
                    override fun onConsumeSettingsScrollTarget() {
                        settingsScrollTarget = null
                    }
                },
            )
        }
        setupSystemBars()
    }

    override fun onDestroy() {
        appDownloadJob?.cancel()
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
    }

    override fun onResume() {
        super.onResume()
        checkInstantRootLanding()
        maybeWakeHelper()
        // Auto-advance AppUpdater when returning from "Allow install unknown apps" Settings
        val curUpd = appUpdateState
        if (curUpd is AppUpdateUiState.PermissionRequired && packageManager.canRequestPackageInstalls()) {
            appUpdateState = AppUpdateUiState.ReadyToInstall(
                release = curUpd.release,
                apkPath = curUpd.apkPath,
                shaSummary = curUpd.shaSummary,
                downgradeWarning = curUpd.downgradeWarning,
            )
        }
        // Refresh Shizuku & live device/root telemetry off the main thread
        refreshTelemetryAndRoot()
    }

    override fun onRequestPermissionResult(requestCode: Int, result: Int) {
        permissionTick++
        lifecycleScope.launch {
            val (active, perm) = withContext(Dispatchers.IO) {
                engine.shizukuBinderActive() to engine.shizukuPermissionGranted()
            }
            shizukuActive = active
            shizukuPermitted = perm
        }
    }

    // ------------------------------------------------------------------
    // Telemetry, App Updater & Smart ksud Check
    // ------------------------------------------------------------------

    private fun refreshTelemetryAndRoot() {
        if (telemetryRefreshing) return
        telemetryRefreshing = true
        lifecycleScope.launch {
            try {
                val snap = DeviceInfoPanel.collectSnapshot(this@MainActivity)
                val (sActive, sPerm) = withContext(Dispatchers.IO) {
                    engine.shizukuBinderActive() to engine.shizukuPermissionGranted()
                }
                telemetry = snap
                shizukuActive = sActive
                shizukuPermitted = sPerm
                engine.setRootedLive(snap.rootedLive)
            } finally {
                telemetryRefreshing = false
            }
        }
    }

    private fun checkAppUpdates(forceRefresh: Boolean, openSheet: Boolean) {
        if (openSheet) appUpdateSheetVisible = true
        val cur = appUpdateState
        if (cur is AppUpdateUiState.Downloading || cur is AppUpdateUiState.Verifying) return
        if (!forceRefresh && (cur is AppUpdateUiState.ReadyToInstall || cur is AppUpdateUiState.PermissionRequired)) return
        val gen = ++appUpdateGen
        if (forceRefresh || cur is AppUpdateUiState.Idle) {
            appUpdateState = AppUpdateUiState.Checking
        }
        lifecycleScope.launch {
            val result = AppUpdater.checkLatest(this@MainActivity, forceRefresh = forceRefresh)
            if (gen != appUpdateGen) return@launch
            val live = appUpdateState
            if (live is AppUpdateUiState.Downloading || live is AppUpdateUiState.Verifying) return@launch
            appUpdateState = result
            if (result is AppUpdateUiState.ReadyToInstall) {
                if (AppUpdater.pendingInstallTag(this@MainActivity) == result.release.tag) {
                    AppUpdater.setPendingInstallTag(this@MainActivity, "")
                    appUpdateSheetVisible = true
                }
            } else if (result is AppUpdateUiState.Available) {
                engine.appendLog("[AppUpdate] New UniRoot Next release available: ${result.release.tag}")
            }
        }
    }

    private fun startAppUpdateDownload() {
        val release = when (val st = appUpdateState) {
            is AppUpdateUiState.Available -> st.release
            is AppUpdateUiState.UpToDate -> st.release
            is AppUpdateUiState.Error -> st.release
            else -> null
        } ?: return

        val gen = ++appUpdateGen
        appDownloadJob?.cancel()
        appUpdateState = AppUpdateUiState.Downloading(release, 0L, release.apkSizeBytes, 0L)
        engine.appendLog("[AppUpdate] Downloading ${release.apkName} (${release.tag})…")

        appDownloadJob = lifecycleScope.launch {
            try {
                val finalState = AppUpdater.downloadAndVerify(
                    context = this@MainActivity,
                    release = release,
                    onProgress = { read, total, speed ->
                        runOnUiThread {
                            if (gen == appUpdateGen) {
                                appUpdateState = AppUpdateUiState.Downloading(release, read, total, speed)
                            }
                        }
                    },
                    onVerifying = { shaOk, sigOk ->
                        runOnUiThread {
                            if (gen == appUpdateGen) {
                                appUpdateState = AppUpdateUiState.Verifying(release, shaOk, sigOk)
                            }
                        }
                    },
                )
                if (gen != appUpdateGen) return@launch
                // Advance generation so any already-queued runOnUiThread callback cannot overwrite finalState
                appUpdateGen++
                appUpdateState = finalState
                if (finalState is AppUpdateUiState.ReadyToInstall) {
                    engine.appendLog("[AppUpdate] Verified ${release.tag} (${finalState.shaSummary})")
                } else if (finalState is AppUpdateUiState.Error) {
                    engine.appendLog("[AppUpdate] Error: ${finalState.message}")
                }
            } catch (_: CancellationException) {
                engine.appendLog("[AppUpdate] Download cancelled")
            }
        }
    }

    private fun checkKsudUpdates(forceRefresh: Boolean) {
        val classic = !ksuNextMode
        val localVer = if (classic) KsudClassicProfiles.highestVersion(this) else KsudNextProfiles.highestVersion(this)
        if (forceRefresh || ksudCheckState is KsudUpdateCheckState.Idle) {
            ksudCheckState = KsudUpdateCheckState.Checking(localVer)
        }
        lifecycleScope.launch {
            val result = DfKsudUpdater.checkLatest(this@MainActivity, classic = classic, forceRefresh = forceRefresh)
            // Ignore stale response if user switched flavor while request was in flight
            if (classic == !ksuNextMode) {
                ksudCheckState = result
            }
        }
    }

    // ------------------------------------------------------------------
    // Profiles, Manager & Logs Helpers
    // ------------------------------------------------------------------

    private fun matchProfileForActiveFlavor(): String? {
        val suffix = if (ksuNextMode) " Next" else ""
        val matched = engine.detectDevice().matchedProfileName
        return matched?.let { m ->
            engine.profileByName(m + suffix)?.name ?: engine.profileByName(m)?.name
        } ?: profiles.firstOrNull { it.name.startsWith("RMG · ") }?.name
    }

    private fun refreshProfiles() {
        val flavor = if (ksuNextMode) "kernelsu_next" else "kernelsu"
        profiles = engine.profiles.filter { it.flavor == flavor }
        if (selectedProfileName == null || profiles.none { it.name == selectedProfileName }) {
            val matched = matchProfileForActiveFlavor()
            selectedProfileName = matched
            autoMatched = matched != null && matched == engine.detectDevice().matchedProfileName?.let { m ->
                val suffix = if (ksuNextMode) " Next" else ""
                engine.profileByName(m + suffix)?.name ?: engine.profileByName(m)?.name
            }
        }
    }

    private fun checkRootMyGalaxy(silent: Boolean) {
        lifecycleScope.launch {
            rmgStatus = engine.checkRootMyGalaxy()
            refreshProfiles()
            if (!silent) Toast.makeText(this@MainActivity, rmgStatus, Toast.LENGTH_LONG).show()
        }
    }

    private fun openActiveManager() {
        val (installed, pkg) = engine.isKsuManagerInstalled(ksuNextMode)
        if (!installed) {
            Toast.makeText(this, R.string.manager_not_installed, Toast.LENGTH_LONG).show()
            return
        }
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(intent) }
        }
    }

    private fun restartKsuManager() {
        val (installed, pkg) = engine.isKsuManagerInstalled(ksuNextMode)
        if (!installed) {
            Toast.makeText(this, R.string.manager_not_installed, Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            val stopped = engine.forceStopKsuManager(pkg)
            delay(if (stopped) 800L else 200L)
            val activity = if (pkg.startsWith("com.rifsxd")) {
                "com.rifsxd.ksunext.ui.MainActivity"
            } else {
                "me.weishu.kernelsu.ui.MainActivity"
            }
            val started = engine.startManagerViaShizuku(pkg, activity)
            if (!started) {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { startActivity(intent) }
                }
            }
        }
    }

    private fun downloadLatestKsu() {
        val profile = engine.profileByName(selectedProfileName) ?: run {
            Toast.makeText(this, R.string.no_profile_selected, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val result = engine.fetchLatestKsuFor(profile)
            engine.appendLog("[KernelSU] $result")
            latestKsuTag = engine.latestKsuTag()
            Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
        }
    }

    private fun resetProfiles() {
        engine.resetProfiles()
        refreshProfiles()
        selectedProfileName = matchProfileForActiveFlavor()
        Toast.makeText(this, R.string.reset_done, Toast.LENGTH_SHORT).show()
    }

    private fun copyLogs() {
        val text = engine.logLines.value.joinToString("\n")
        copyTextToClipboard("uniroot-log", text, R.string.action_copy)
    }

    private fun copyTextToClipboard(label: String, text: String, toastRes: Int) {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, toastRes, Toast.LENGTH_SHORT).show()
    }

    private fun shareRunLog(file: File) {
        runCatching {
            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, file.name))
        }
    }

    // ------------------------------------------------------------------
    // Root Execution Pipelines (New Method & Old Method)
    // ------------------------------------------------------------------

    private fun launchNewMethod() {
        engine.clearLogs()
        val flavorLabel = if (ksuNextMode) "KernelSU Next" else "KernelSU"
        val profileLabel = "DirtyFrag ($flavorLabel)"
        engine.beginRunLog(profileLabel)
        sheetVisible = true
        sheetDismissible = false
        newMethodProgress = 5
        newMethodStage = "Loading DF engine…"
        engine.appendLog("[NewMethod] DirtyFrag engine (CVE-2026-43284) — $flavorLabel")
        lifecycleScope.launch {
            engine.setRunning(true)
            val t0 = android.os.SystemClock.elapsedRealtime()
            var rc = -1
            var active = true
            try {
                rc = withContext(Dispatchers.IO) {
                    DfEngine.run(
                        this@MainActivity,
                        ksuNextMode,
                        newMethodSoftReboot,
                        { line -> engine.appendLog(line.trimEnd()) },
                        { pct, stage ->
                            runOnUiThread {
                                if (active) {
                                    newMethodProgress = pct
                                    if (stage.isNotEmpty()) newMethodStage = stage
                                }
                            }
                        },
                    )
                }
                active = false
                engine.appendLog("[NewMethod] DF rc=$rc")
                if (rc == 0) {
                    newMethodProgress = 100
                    newMethodStage = "Phone rooted ✓"
                    engine.markRooted("DF", if (ksuNextMode) "kernelsu_next" else "kernelsu")
                    Toast.makeText(this@MainActivity, R.string.phone_rooted_toast, Toast.LENGTH_LONG).show()
                    refreshTelemetryAndRoot()
                    if (newMethodSoftReboot) {
                        engine.appendLog("[NewMethod] Auto soft reboot scheduled by ksud (Zygisk/LSPosed/Vector + global mounts)…")
                    } else {
                        delay(4_000)
                        restartKsuManager()
                    }
                } else {
                    newMethodStage = "Failed (rc=$rc)"
                    Toast.makeText(this@MainActivity, R.string.new_method_failed, Toast.LENGTH_LONG).show()
                }
            } finally {
                active = false
                engine.setRunning(false)
                sheetDismissible = true
                runCatching {
                    engine.endRunLog(
                        if (rc == 0) "Success" else "Failed",
                        profileLabel,
                        android.os.SystemClock.elapsedRealtime() - t0,
                    )
                }
                runLogs = engine.listRunLogs()
            }
        }
    }

    private fun launchRun() {
        val profile = engine.profileByName(selectedProfileName) ?: return
        val wantShizuku = RootEngine.profileNeedsShizuku(profile.name) || profile.useShizuku

        if (wantShizuku) {
            if (!engine.shizukuBinderActive()) {
                engine.clearLogs()
                sheetVisible = true
                sheetDismissible = true
                engine.appendLog("[!] Shizuku binder is NOT active.")
                engine.appendLog("[!] Start Shizuku (wireless debugging) first, then run again.")
                return
            }
            if (!engine.shizukuPermissionGranted()) {
                engine.appendLog("[!] Shizuku permission missing — accepting the prompt, run again after.")
                engine.requestShizukuPermission()
                sheetVisible = true
                sheetDismissible = true
                return
            }
            shizukuEnabled = true
        }

        engine.setLastRunProfile(profile.name)
        engine.clearLogs()
        sheetVisible = true
        sheetDismissible = false
        if (autoMatched) engine.appendLog("[Mode] Profile auto-detected for this device")
        engine.appendLog("[Mode] " + (if (shizukuEnabled || wantShizuku) "Shizuku (shell UID 2000)" else "Local (app context)"))

        lifecycleScope.launch {
            engine.setRunning(true)
            var status = "Crash"
            try {
                status = engine.runExecutionPipeline(profile, shizukuEnabled || wantShizuku)
                engine.appendLog("[Pipeline] Finished: $status")
                runLogs = engine.listRunLogs()
                if (status == "Success") {
                    withContext(Dispatchers.IO) { engine.refreshRootedLive() }
                    refreshTelemetryAndRoot()
                    delay(1200)
                    sheetVisible = false
                    refreshInstantRootState()
                    val offer = !engine.instantRootEnabled &&
                        !instantRootInstalled && InstantRoot(this@MainActivity).assetsAvailable()
                    if (offer) {
                        instantRootOfferVisible = true
                    } else {
                        delay(12_000)
                        restartKsuManager()
                    }
                }
            } catch (e: Exception) {
                engine.appendLog("[Error] Unexpected: ${e.javaClass.simpleName}: ${e.message}")
                android.util.Log.e("UniRoot", "pipeline failed", e)
                runCatching { engine.abandonActiveRunLog() }
                runLogs = engine.listRunLogs()
            } finally {
                engine.setRunning(false)
                sheetDismissible = true
            }
        }
    }

    // ------------------------------------------------------------------
    // Instant Root (DFReroot second stage — preserved 100%)
    // ------------------------------------------------------------------

    private fun refreshInstantRootState() {
        instantRootEnabled = engine.instantRootEnabled
        instantRootInstalled = InstantRoot(this).isHelperInstalled()
        syncBootReceiverEnabled()
    }

    private fun syncBootReceiverEnabled() {
        val enabled = newMethodBootEnabled || instantRootEnabled
        val cn = ComponentName(this, NewMethodBootReceiver::class.java)
        runCatching {
            packageManager.setComponentEnabledSetting(
                cn,
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    private fun checkInstantRootLanding() {
        val stage = engine.instantRootSetupStage
        if (stage != RootEngine.INSTANT_STAGE_INJECTED && stage != RootEngine.INSTANT_STAGE_QUIET) return
        val installed = InstantRoot(this).isHelperInstalled()
        if (installed) {
            engine.instantRootEnabled = true
            engine.instantRootSetupStage = ""
            engine.instantRootPrompted = true
            refreshInstantRootState()
            wakeHelperOnce()
            Toast.makeText(this, R.string.instant_root_install_ready, Toast.LENGTH_LONG).show()
            engine.appendLog("[InstantRoot] " + getString(R.string.instant_root_root_current_boot))
        } else if (!instantRootInstallVisible) {
            instantRootInstallVisible = true
        }
    }

    private fun maybeWakeHelper() {
        if (!engine.instantRootEnabled || !instantRootInstalled) return
        val prefs = getSharedPreferences("uniroot_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("helper_woken", false)) return
        wakeHelperOnce()
    }

    private fun wakeHelperOnce() {
        runCatching {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    runCatching { InstantRoot(this@MainActivity).unstopHelper() }
                }
            }
            val wake = Intent().setClassName(
                InstantRoot.PKG_REROOT,
                InstantRoot.PKG_REROOT + ".MainActivity",
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(wake)
            getSharedPreferences("uniroot_prefs", MODE_PRIVATE)
                .edit().putBoolean("helper_woken", true).apply()
            lifecycleScope.launch {
                delay(1_500)
                runCatching {
                    startActivity(
                        Intent(this@MainActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    )
                }
            }
        }
    }

    private fun enableInstantRoot() {
        val instant = InstantRoot(this) { line -> engine.appendLog(line) }
        if (Build.VERSION.SDK_INT >= 33) {
            runCatching { requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 102) }
        }
        if (!Settings.canDrawOverlays(this)) {
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                )
            }
        }
        lifecycleScope.launch {
            instant.record(getString(R.string.instant_root_setup_start))
            instant.record(getString(R.string.instant_root_setup_su_wait))
            var suOut = withContext(Dispatchers.IO) { instant.suId(timeoutSec = 8) }
            if (suOut == null) {
                superuserNeededVisible = true
                for (i in 1..48) {
                    delay(5_000)
                    suOut = withContext(Dispatchers.IO) { instant.suId(timeoutSec = 8) }
                    if (suOut != null) break
                }
                superuserNeededVisible = false
            }
            if (suOut == null) {
                instant.record("[InstantRoot] Superuser was not granted in time — allow UniRoot Next in KernelSU (Next) > Superuser, then press Enable again.")
                instant.record(getString(R.string.instant_root_setup_failed))
                Toast.makeText(this@MainActivity, R.string.instant_root_need_root, Toast.LENGTH_LONG).show()
                return@launch
            }
            instant.record("[InstantRoot] Superuser OK")
            Toast.makeText(this@MainActivity, R.string.instant_root_su_ok, Toast.LENGTH_SHORT).show()
            runCatching {
                startActivity(
                    Intent(this@MainActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
            }
            delay(1_000)
            if (instant.isHelperInstalled()) {
                withContext(Dispatchers.IO) { instant.setHelperEnabled(true) }
                if (newMethod) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            df.root.DFBridge.stageKsud(
                                this@MainActivity, ksuNextMode, newMethodSoftReboot,
                                object : df.root.IReporter { override fun report(msg: String?) {} },
                                null,
                            )
                            val rootFile = df.root.DFBridge.stagedBinaryFile(this@MainActivity, ksuNextMode)
                            if (rootFile.isFile) {
                                instant.stageProfileKsud(
                                    rootFile.absolutePath,
                                    if (ksuNextMode) "kernelsu_next" else "kernelsu",
                                )
                            }
                        }
                    }
                } else {
                    val usedProfile = engine.profileByName(selectedProfileName)
                    if (usedProfile != null && usedProfile.pathKsud.isNotBlank()) {
                        withContext(Dispatchers.IO) {
                            instant.stageProfileKsud(usedProfile.pathKsud, usedProfile.flavor)
                        }
                    }
                }
                engine.instantRootEnabled = true
                engine.instantRootPrompted = true
                refreshInstantRootState()
                wakeHelperOnce()
                instant.record("[InstantRoot] Helper already installed — instant root is ON (no reboot needed).")
                Toast.makeText(this@MainActivity, R.string.instant_root_switch_on_toast, Toast.LENGTH_LONG).show()
                return@launch
            }
            pendingInstant = instant
            injectConfirmVisible = true
        }
    }

    private fun continueInstantRootSetup() {
        val instant = pendingInstant ?: InstantRoot(this) { line -> engine.appendLog(line) }
        pendingInstant = null
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, R.string.instant_root_injecting, Toast.LENGTH_SHORT).show()
            val ok = withContext(Dispatchers.IO) { instant.stageApks() && instant.inject() }
            if (!ok) {
                instant.record(getString(R.string.instant_root_setup_failed))
                return@launch
            }
            if (newMethod) {
                val staged = withContext(Dispatchers.IO) {
                    runCatching {
                        df.root.DFBridge.stageKsud(
                            this@MainActivity, ksuNextMode, newMethodSoftReboot,
                            object : df.root.IReporter { override fun report(msg: String?) {} },
                            null,
                        )
                    }
                    val rootFile = df.root.DFBridge.stagedBinaryFile(this@MainActivity, ksuNextMode)
                    if (rootFile.isFile) instant.stageProfileKsud(rootFile.absolutePath, if (ksuNextMode) "kernelsu_next" else "kernelsu")
                    else false
                }
                instant.record(
                    if (staged) "[InstantRoot] new method ksud staged to /data/system/dfreroot-ksud"
                    else "[InstantRoot] new method ksud staging failed — helper will use its bundled fallback",
                )
            } else {
                val usedProfile = engine.profileByName(selectedProfileName)
                val ksudSrc = usedProfile?.pathKsud
                if (usedProfile != null && !ksudSrc.isNullOrBlank()) {
                    val staged = withContext(Dispatchers.IO) {
                        instant.stageProfileKsud(ksudSrc, usedProfile.flavor)
                    }
                    instant.record(
                        if (staged) "[InstantRoot] profile ksud staged to /data/system/dfreroot-ksud ($ksudSrc)"
                        else "[InstantRoot] profile ksud staging failed — helper will use its bundled fallback",
                    )
                }
            }
            engine.instantRootSetupStage = RootEngine.INSTANT_STAGE_INJECTED
            engine.instantRootEnabled = true
            engine.instantRootPrompted = true
            if (engine.autoRootOnBoot) {
                engine.autoRootOnBoot = false
                autoRootOnBoot = false
            }
            refreshInstantRootState()
            runCatching {
                val nm = getSystemService(NotificationManager::class.java)
                if (nm.getNotificationChannel("auto_root") == null) {
                    nm.createNotificationChannel(
                        NotificationChannel("auto_root", "Auto-root", NotificationManager.IMPORTANCE_DEFAULT),
                    )
                }
                val open = packageManager.getLaunchIntentForPackage(packageName)
                val n = Notification.Builder(this@MainActivity, "auto_root")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle("UniRoot Next — Instant Root")
                    .setContentText(getString(R.string.instant_root_soft_reboot_notif))
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .apply {
                        open?.let {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            setContentIntent(
                                PendingIntent.getActivity(
                                    this@MainActivity, 3, it,
                                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                                ),
                            )
                        }
                    }
                    .build()
                nm.notify(45, n)
            }
            instant.record(getString(R.string.instant_root_setup_reboot))
            delay(1_000)
            Toast.makeText(this@MainActivity, R.string.instant_root_restarting, Toast.LENGTH_LONG).show()
            delay(4_000)
            Toast.makeText(this@MainActivity, R.string.instant_root_launch_after, Toast.LENGTH_LONG).show()
            delay(4_000)
            Toast.makeText(this@MainActivity, R.string.instant_root_launch_after, Toast.LENGTH_LONG).show()
            delay(4_000)
            withContext(Dispatchers.IO) { instant.softReboot() }
        }
    }

    private fun uninstallHelper() {
        val instant = InstantRoot(this) { line -> engine.appendLog(line) }
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { instant.uninstallAll() }
            engine.instantRootEnabled = false
            engine.instantRootSetupStage = ""
            refreshInstantRootState()
            Toast.makeText(
                this@MainActivity,
                if (ok) R.string.instant_root_uninstalled else R.string.instant_root_need_root,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun disableInstantRoot() {
        engine.instantRootEnabled = false
        engine.instantRootSetupStage = ""
        refreshInstantRootState()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { InstantRoot(this@MainActivity).setHelperEnabled(false) }
            engine.appendLog(if (ok) "[InstantRoot] Helper disabled" else "[InstantRoot] Helper not disabled (no root right now)")
        }
        Toast.makeText(this, R.string.instant_root_disabled_toast, Toast.LENGTH_LONG).show()
    }

    private fun disableRoot() {
        newMethodBootEnabled = false
        getSharedPreferences("uniroot_prefs", MODE_PRIVATE)
            .edit().putBoolean("new_method_boot_enabled", false).apply()
        engine.instantRootEnabled = false
        engine.instantRootSetupStage = ""
        if (engine.autoRootOnBoot) {
            engine.autoRootOnBoot = false
            autoRootOnBoot = false
        }
        refreshInstantRootState()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { InstantRoot(this@MainActivity).setHelperEnabled(false) }
        }
        Toast.makeText(this, R.string.instant_root_disabled_toast, Toast.LENGTH_LONG).show()
    }

    private fun applyInstantRootEnabled(enabled: Boolean) {
        if (enabled && engine.autoRootOnBoot) {
            engine.autoRootOnBoot = false
            autoRootOnBoot = false
            Toast.makeText(this, R.string.autoroot_disabled_for_instant, Toast.LENGTH_LONG).show()
        }
        if (enabled && !instantRootInstalled) {
            enableInstantRoot()
            return
        }
        engine.instantRootEnabled = enabled
        refreshInstantRootState()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { InstantRoot(this@MainActivity).setHelperEnabled(enabled) }
        }
        Toast.makeText(
            this,
            if (enabled) R.string.instant_root_switch_on_toast else R.string.instant_root_disabled_toast,
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun setupSystemBars() {
        val controller = window.decorView.windowInsetsController ?: return
        val lightStatus = if (resources.getBoolean(R.bool.window_light_status_bar)) {
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        } else 0
        val lightNavigation = if (resources.getBoolean(R.bool.window_light_navigation_bar)) {
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        } else 0
        controller.setSystemBarsAppearance(
            lightStatus or lightNavigation,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
        )
    }
}
