package com.uniroot.app.engine

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.system.Os
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

enum class CompatibilityLevel {
    COMPATIBLE,
    CAUTION,
    UNSUPPORTED,
}

data class CompatibilityVerdict(
    val level: CompatibilityLevel,
    val badge: String,
    val headline: String,
    val reason: String,
    val suggestOldExploit: Boolean = false,
)

data class InstalledManagerInfo(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isNext: Boolean,
)

data class DeviceTelemetry(
    val marketingName: String,
    val modelCode: String,
    val socName: String,
    val abi: String,
    val cpuCores: Int,
    val is64Bit: Boolean,
    val androidVersion: String,
    val oneUiVersion: String?,
    val securityPatch: String,
    val kernelRelease: String,
    val kmi: String?,
    val knoxState: String?,
    val knoxTripped: Boolean?,
    val bootloaderState: String,
    val selinuxState: String,
    val rootState: String,
    val rootDetail: String,
    val rootedLive: Boolean,
    val moduleLoaded: Boolean,
    val installedManagers: List<InstalledManagerInfo>,
    val fingerprint: String,
    val ramUsedBytes: Long,
    val ramTotalBytes: Long,
    val ramAvailBytes: Long,
    val storageUsedBytes: Long,
    val storageTotalBytes: Long,
    val displaySummary: String,
    val batterySummary: String,
    val uptimeSummary: String,
    val loaded: Boolean = true,
) {
    val primaryManagerSummary: String
        get() {
            val mgr = installedManagers.firstOrNull { it.isNext } ?: installedManagers.firstOrNull()
            return if (mgr != null) "${mgr.label} ${mgr.versionName}" else "Not installed"
        }

    fun managerForFlavor(next: Boolean): InstalledManagerInfo? =
        installedManagers.firstOrNull { it.isNext == next }

    companion object {
        fun placeholder(): DeviceTelemetry = DeviceTelemetry(
            marketingName = Build.MODEL?.ifBlank { Build.DEVICE } ?: "Android Device",
            modelCode = Build.MODEL ?: "",
            socName = "Detecting…",
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            cpuCores = Runtime.getRuntime().availableProcessors(),
            is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty(),
            androidVersion = Build.VERSION.RELEASE ?: "?",
            oneUiVersion = null,
            securityPatch = Build.VERSION.SECURITY_PATCH ?: "",
            kernelRelease = runCatching { Os.uname().release }.getOrNull() ?: System.getProperty("os.version") ?: "?",
            kmi = null,
            knoxState = null,
            knoxTripped = null,
            bootloaderState = "Checking…",
            selinuxState = "Checking…",
            rootState = "Checking…",
            rootDetail = "",
            rootedLive = false,
            moduleLoaded = false,
            installedManagers = emptyList(),
            fingerprint = Build.FINGERPRINT ?: "",
            ramUsedBytes = 0L,
            ramTotalBytes = 0L,
            ramAvailBytes = 0L,
            storageUsedBytes = 0L,
            storageTotalBytes = 0L,
            displaySummary = "—",
            batterySummary = "—",
            uptimeSummary = "—",
            loaded = false,
        )
    }
}

/**
 * Rich, off-main-thread hardware, kernel, security & compatibility detection engine.
 * Fixes KMI extraction (`6.6.127-android15-...` -> `android15-6.6`), One UI integer math
 * (`80100` -> `8.1`), Samsung & OEM marketing names, SoC resolution, and per-method compatibility.
 */
object DeviceInfoPanel {

    /** The 8 Android GKI 2.0 KMIs bundled with DirtyFrag & Samsung KDP/DEFEX modules. */
    val BUNDLED_DF_KMIS: Set<String> = setOf(
        "android12-5.10",
        "android13-5.10",
        "android13-5.15",
        "android14-5.15",
        "android14-6.1",
        "android15-6.6",
        "android16-6.12",
        "android17-6.18",
    )

    /** Models explicitly known not to work on DirtyFrag currently (Galaxy S23 series incl. JP variants). */
    val DF_UNSUPPORTED_MODELS: Set<String> = setOf(
        "SM-S911",
        "SM-S916",
        "SM-S918",
        "SC-51D",
        "SCG19",
        "SC-52D",
        "SCG20",
    )

    private val SAMSUNG_NAMES: Map<String, String> = mapOf(
        // Galaxy S22
        "SM-S901" to "Galaxy S22",
        "SM-S906" to "Galaxy S22+",
        "SM-S908" to "Galaxy S22 Ultra",
        // Galaxy S23 (incl. Japan carrier models)
        "SM-S911" to "Galaxy S23",
        "SC-51D" to "Galaxy S23",
        "SCG19" to "Galaxy S23",
        "SM-S916" to "Galaxy S23+",
        "SM-S918" to "Galaxy S23 Ultra",
        "SC-52D" to "Galaxy S23 Ultra",
        "SCG20" to "Galaxy S23 Ultra",
        "SM-S711" to "Galaxy S23 FE",
        // Galaxy S24
        "SM-S921" to "Galaxy S24",
        "SM-S926" to "Galaxy S24+",
        "SM-S928" to "Galaxy S24 Ultra",
        "SM-S721" to "Galaxy S24 FE",
        // Galaxy S25
        "SM-S931" to "Galaxy S25",
        "SM-S936" to "Galaxy S25+",
        "SM-S937" to "Galaxy S25 Edge",
        "SM-S938" to "Galaxy S25 Ultra",
        "SM-S731" to "Galaxy S25 FE",
        // Galaxy S26
        "SM-S941" to "Galaxy S26",
        "SM-S942" to "Galaxy S26",
        "SM-S946" to "Galaxy S26+",
        "SM-S947" to "Galaxy S26 Edge",
        "SM-S948" to "Galaxy S26 Ultra",
        // Galaxy Z Fold
        "SM-F936" to "Galaxy Z Fold 4",
        "SM-F946" to "Galaxy Z Fold 5",
        "SM-F956" to "Galaxy Z Fold 6",
        "SM-F958" to "Galaxy Z Fold SE",
        "SM-F966" to "Galaxy Z Fold 7",
        "SM-F971" to "Galaxy Z Fold 8",
        "SM-F976" to "Galaxy Z Fold 8",
        // Galaxy Z Flip
        "SM-F721" to "Galaxy Z Flip 4",
        "SM-F731" to "Galaxy Z Flip 5",
        "SM-F741" to "Galaxy Z Flip 6",
        "SM-F751" to "Galaxy Z Flip 7",
        "SM-F766" to "Galaxy Z Flip 7",
        "SM-F776" to "Galaxy Z Flip 8",
        // Galaxy A / E series
        "SM-A346" to "Galaxy A34 5G",
        "SM-A356" to "Galaxy A35 5G",
        "SM-A366" to "Galaxy A36 5G",
        "SM-A546" to "Galaxy A54 5G",
        "SM-A556" to "Galaxy A55 5G",
        "SM-A566" to "Galaxy A56 5G",
        // Galaxy Tab S9 / S10
        "SM-X710" to "Galaxy Tab S9",
        "SM-X716" to "Galaxy Tab S9 5G",
        "SM-X810" to "Galaxy Tab S9+",
        "SM-X816" to "Galaxy Tab S9+ 5G",
        "SM-X910" to "Galaxy Tab S9 Ultra",
        "SM-X916" to "Galaxy Tab S9 Ultra 5G",
        "SM-X820" to "Galaxy Tab S10+",
        "SM-X826" to "Galaxy Tab S10+ 5G",
        "SM-X920" to "Galaxy Tab S10 Ultra",
        "SM-X926" to "Galaxy Tab S10 Ultra 5G",
    )

    private val SOC_FRIENDLY_NAMES: List<Pair<String, String>> = listOf(
        "SM8850" to "Snapdragon 8 Elite Gen 5",
        "SM8750" to "Snapdragon 8 Elite",
        "SM8650" to "Snapdragon 8 Gen 3",
        "SM8635" to "Snapdragon 8s Gen 3",
        "SM8550" to "Snapdragon 8 Gen 2",
        "SM8475" to "Snapdragon 8+ Gen 1",
        "SM8450" to "Snapdragon 8 Gen 1",
        "SM7675" to "Snapdragon 7+ Gen 3",
        "SM7550" to "Snapdragon 7 Gen 3",
        "SM6475" to "Snapdragon 6 Gen 3",
        "SM6450" to "Snapdragon 6 Gen 1",
        "SUN" to "Snapdragon 8 Elite",
        "PINEAPPLE" to "Snapdragon 8 Gen 3",
        "KALAMA" to "Snapdragon 8 Gen 2",
        "TARO" to "Snapdragon 8 Gen 1",
        "S5E9955" to "Exynos 2500",
        "S5E9945" to "Exynos 2400",
        "S5E9935" to "Exynos 2300",
        "S5E9925" to "Exynos 2200",
        "S5E8855" to "Exynos 1580",
        "S5E8845" to "Exynos 1480",
        "S5E8835" to "Exynos 1380",
        "MT6991" to "Dimensity 9400",
        "MT6989" to "Dimensity 9300",
        "MT6985" to "Dimensity 9200",
        "MT6983" to "Dimensity 9000",
        "MT6897" to "Dimensity 8300",
        "GS201" to "Google Tensor G2",
        "ZUMA" to "Google Tensor G3",
        "ZUMAPRO" to "Google Tensor G4",
    )

    private fun prop(name: String): String? = runCatching {
        val sp = Class.forName("android.os.SystemProperties")
        sp.getMethod("get", String::class.java).invoke(null, name) as? String
    }.getOrNull()?.trim()?.takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }

    fun marketingNameOnly(): String {
        val model = (Build.MODEL ?: "").trim()
        SAMSUNG_NAMES.entries.firstOrNull { model.startsWith(it.key, ignoreCase = true) }
            ?.let { return it.value }

        val marketProps = listOf(
            "ro.product.marketname",
            "ro.product.vendor.marketname",
            "ro.vendor.oplus.market.name",
            "ro.oppo.market.name",
            "ro.config.marketing_name",
            "ro.vivo.market.name",
            "ro.product.odm.marketname",
        )
        for (key in marketProps) {
            prop(key)?.let { return it }
        }

        val mfg = (Build.MANUFACTURER ?: "").trim()
        if (mfg.isNotEmpty() && model.isNotEmpty()) {
            return if (model.startsWith(mfg, ignoreCase = true)) {
                model
            } else {
                "${mfg.replaceFirstChar { it.uppercase() }} $model"
            }
        }
        return model.ifBlank { Build.DEVICE ?: "Android Device" }
    }

    fun marketingName(): String {
        val name = marketingNameOnly()
        val model = (Build.MODEL ?: "").trim()
        return if (model.isNotEmpty() && !name.contains(model, ignoreCase = true)) "$name ($model)" else name
    }

    fun kernelRelease(): String {
        val fromProc = runCatching {
            File("/proc/version").readText()
                .substringAfter("Linux version ", "")
                .substringBefore(" (")
                .trim()
        }.getOrDefault("")
        if (fromProc.isNotEmpty()) return fromProc
        val fromUname = runCatching { Os.uname().release }.getOrNull()?.trim().orEmpty()
        if (fromUname.isNotEmpty()) return fromUname
        return System.getProperty("os.version")?.trim()?.takeIf { it.isNotEmpty() } ?: "?"
    }

    /**
     * Parses Android GKI 2.0 KMI from real kernel release strings:
     * - `"6.6.127-android15-8-g12345"` -> `"android15-6.6"`
     * - `"6.12.69-android16-5-..."` -> `"android16-6.12"`
     * - `"5.15.149-android13-8-..."` -> `"android13-5.15"`
     * Also accepts already-formatted `"android15-6.6"`.
     */
    fun parseKmi(kernel: String): String? {
        if (kernel.isBlank()) return null
        Regex("""^(\d+\.\d+)(?:\.\d+)?[^\s]*?android(\d+)""").find(kernel)?.let { m ->
            return "android${m.groupValues[2]}-${m.groupValues[1]}"
        }
        Regex("""android(\d+)-(\d+\.\d+)""").find(kernel)?.let { m ->
            return "android${m.groupValues[1]}-${m.groupValues[2]}"
        }
        return null
    }

    fun kmi(): String? = parseKmi(kernelRelease())

    fun is64Bit(): Boolean = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

    /** Decodes Samsung `ro.build.version.oneui` (`M*10000 + m*100 + p`, e.g., `80100` -> `8.1`, `60101` -> `6.1.1`). */
    fun oneUiVersion(): String? {
        val raw = prop("ro.build.version.oneui") ?: return null
        val n = raw.toIntOrNull() ?: return raw
        if (n < 10000) return raw
        val major = n / 10000
        val minor = (n % 10000) / 100
        val patch = n % 100
        return if (patch > 0) "$major.$minor.$patch" else "$major.$minor"
    }

    fun resolveSocName(): String {
        val model = (Build.MODEL ?: "").trim()
        val rawCandidates = listOfNotNull(
            runCatching { Build.SOC_MODEL }.getOrNull()?.takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) },
            prop("ro.soc.model"),
            prop("ro.hardware.chipname"),
            prop("ro.board.platform"),
            Build.HARDWARE?.takeIf { it.isNotBlank() && !it.equals("qcom", ignoreCase = true) && !it.equals("unknown", ignoreCase = true) },
        )
        for (cand in rawCandidates) {
            val upper = cand.uppercase()
            SOC_FRIENDLY_NAMES.firstOrNull { upper.contains(it.first) }?.let { return it.second }
        }
        // Samsung fallback when SOC_MODEL is masked on certain builds
        when {
            model.startsWith("SM-S948", true) || model.startsWith("SM-F97", true) -> return "Snapdragon 8 Elite Gen 5"
            model.startsWith("SM-S93", true) || model.startsWith("SM-F966", true) -> return "Snapdragon 8 Elite"
            model.startsWith("SM-S928", true) || model.startsWith("SM-F956", true) -> return "Snapdragon 8 Gen 3"
            model.startsWith("SM-S91", true) -> return "Snapdragon 8 Gen 2"
        }
        val mfg = runCatching { Build.SOC_MANUFACTURER }.getOrNull()
            ?.takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
        val socModel = rawCandidates.firstOrNull()
        return listOfNotNull(mfg, socModel).distinct().joinToString(" ").ifBlank { Build.HARDWARE ?: "Unknown SoC" }
    }

    /** Returns Pair(displayText, trippedBoolean) on Samsung devices, or null on non-Samsung devices. */
    fun knoxInfo(): Pair<String, Boolean>? {
        val warranty = prop("ro.boot.warranty_bit")
            ?: prop("ro.warranty_bit")
            ?: prop("ro.boot.warrantybit")
            ?: return null
        val knox = prop("ro.boot.warrantybit") ?: warranty
        val tripped = warranty == "1" || knox == "1" ||
            warranty.equals("0x1", ignoreCase = true) || knox.equals("0x1", ignoreCase = true)
        return (if (tripped) "Tripped (0x1)" else "Intact (0x0)") to tripped
    }

    fun knoxState(): String? = knoxInfo()?.first

    fun bootloaderState(): String {
        val flashLocked = prop("ro.boot.flash.locked")
        val vbmeta = prop("ro.boot.vbmeta.device_state")
        val verified = prop("ro.boot.verifiedbootstate")
        val lockText = when {
            flashLocked == "1" || vbmeta.equals("locked", ignoreCase = true) -> "Locked"
            flashLocked == "0" || vbmeta.equals("unlocked", ignoreCase = true) -> "Unlocked"
            else -> null
        }
        val vbText = verified?.replaceFirstChar { it.uppercase() }
        return listOfNotNull(lockText, vbText).joinToString(" · ").ifBlank { "Unknown" }
    }

    fun installedManagers(context: Context): List<InstalledManagerInfo> {
        val pm = context.packageManager
        val specs = listOf(
            Triple("KernelSU Next", "com.rifsxd.ksunext", true),
            Triple("KernelSU Next (Debug)", "com.rifsxd.ksunext.debug", true),
            Triple("KernelSU", "me.weishu.kernelsu", false),
            Triple("KernelSU (PR)", "me.weishu.kernelsu.pr", false),
        )
        return buildList {
            for ((label, pkg, isNext) in specs) {
                val info = runCatching {
                    if (Build.VERSION.SDK_INT >= 33) {
                        pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(pkg, 0)
                    }
                }.getOrNull() ?: continue
                add(
                    InstalledManagerInfo(
                        label = label,
                        packageName = pkg,
                        versionName = info.versionName?.trim().orEmpty().ifEmpty { "?" },
                        versionCode = info.longVersionCode,
                        isNext = isNext,
                    )
                )
            }
        }
    }

    /** Evaluates compatibility for the active method + device telemetry. */
    fun evaluateCompatibility(
        telemetry: DeviceTelemetry,
        newMethod: Boolean,
        selectedProfile: DeviceProfile?,
        autoMatched: Boolean,
        shizukuActive: Boolean,
        shizukuPermitted: Boolean,
    ): CompatibilityVerdict {
        if (!telemetry.loaded) {
            return CompatibilityVerdict(
                level = CompatibilityLevel.CAUTION,
                badge = "Scanning…",
                headline = "Inspecting Device",
                reason = "Reading kernel KMI, architecture, and root environment…",
            )
        }
        if (newMethod) {
            if (!telemetry.is64Bit) {
                return CompatibilityVerdict(
                    level = CompatibilityLevel.UNSUPPORTED,
                    badge = "32-bit",
                    headline = "Unsupported Architecture",
                    reason = "DirtyFrag requires a 64-bit arm64-v8a Android device.",
                )
            }
            if (DF_UNSUPPORTED_MODELS.any { telemetry.modelCode.startsWith(it, ignoreCase = true) }) {
                return CompatibilityVerdict(
                    level = CompatibilityLevel.UNSUPPORTED,
                    badge = "S23 Unsupported",
                    headline = "Galaxy S23 Not Supported",
                    reason = "Galaxy S23 (${telemetry.kmi ?: "android13-5.15"}) is not currently supported by the DirtyFrag engine.",
                    suggestOldExploit = true,
                )
            }
            val kmi = telemetry.kmi
            if (kmi == null) {
                return CompatibilityVerdict(
                    level = CompatibilityLevel.UNSUPPORTED,
                    badge = "Non-GKI",
                    headline = "Non-GKI Kernel",
                    reason = "No GKI 2.0 KMI detected in ${telemetry.kernelRelease}. DirtyFrag requires GKI android12-5.10 through android17-6.18.",
                    suggestOldExploit = true,
                )
            }
            if (kmi !in BUNDLED_DF_KMIS) {
                return CompatibilityVerdict(
                    level = CompatibilityLevel.UNSUPPORTED,
                    badge = "KMI Unsupported",
                    headline = "KMI Not Bundled",
                    reason = "Detected KMI $kmi is not among the 8 bundled DirtyFrag + Samsung KDP/DEFEX modules.",
                    suggestOldExploit = true,
                )
            }
            return CompatibilityVerdict(
                level = CompatibilityLevel.COMPATIBLE,
                badge = "Compatible · $kmi",
                headline = "Ready to Root",
                reason = "arm64-v8a + bundled $kmi Samsung KDP/DEFEX module ready (requires unpatched CVE-2026-43284 firmware).",
            )
        } else {
            if (selectedProfile == null) {
                return CompatibilityVerdict(
                    level = CompatibilityLevel.UNSUPPORTED,
                    badge = "No Profile",
                    headline = "Profile Required",
                    reason = "No Old Exploit profile matched this build. Select or import a profile, or check Root My Galaxy in Settings.",
                )
            }
            val needsShizuku = RootEngine.profileNeedsShizuku(selectedProfile.name) || selectedProfile.useShizuku
            if (needsShizuku && (!shizukuActive || !shizukuPermitted)) {
                val why = if (!shizukuActive) "Start Shizuku (Wireless Debugging) first." else "Grant Shizuku permission to UniRoot Next."
                return CompatibilityVerdict(
                    level = CompatibilityLevel.CAUTION,
                    badge = "Shizuku Required",
                    headline = "Shizuku Required",
                    reason = "${selectedProfile.name} runs via Shizuku (UID 2000). $why",
                )
            }
            return if (autoMatched) {
                CompatibilityVerdict(
                    level = CompatibilityLevel.COMPATIBLE,
                    badge = "Profile Matched",
                    headline = "Ready to Root",
                    reason = "Auto-matched ${selectedProfile.name} for this device & kernel.",
                )
            } else {
                CompatibilityVerdict(
                    level = CompatibilityLevel.CAUTION,
                    badge = "Manual Profile",
                    headline = "Ready (Manual Profile)",
                    reason = "Using manually selected profile: ${selectedProfile.name}.",
                )
            }
        }
    }

    /** Full non-blocking snapshot executed on Dispatchers.IO. */
    suspend fun collectSnapshot(context: Context): DeviceTelemetry = withContext(Dispatchers.IO) {
        val model = (Build.MODEL ?: "").trim()
        val kernel = kernelRelease()
        val parsedKmi = parseKmi(kernel)
        val knox = knoxInfo()
        val managers = installedManagers(context)

        // Probe live root + kernel module off main thread.
        val suOut = runCatching {
            val p = ProcessBuilder("su", "-c", "id").start()
            val done = p.waitFor(4, TimeUnit.SECONDS)
            if (!done) { runCatching { p.destroyForcibly() }; "" }
            else p.inputStream.bufferedReader().use { it.readText() }
        }.getOrDefault("")
        val rootedLive = suOut.contains("uid=0")

        val modulesText = runCatching { File("/proc/modules").readText() }.getOrDefault("")
        val moduleLoaded = modulesText.contains("kernelsu", ignoreCase = true) ||
            modulesText.contains("ksunext", ignoreCase = true) ||
            runCatching { File("/dev/df").exists() || File("/dev/dfm3").exists() }.getOrDefault(false)

        val rootState = when {
            rootedLive -> "Rooted"
            moduleLoaded -> "Module active (grant su)"
            else -> "Not rooted"
        }
        val rootDetail = when {
            rootedLive -> "su uid=0 active"
            moduleLoaded -> "KernelSU / DF hook present — grant UniRoot Next in manager or re-run"
            else -> "No active KernelSU module"
        }

        // SELinux: non-privileged sysfs first, then `su -c getenforce` if rooted.
        val selinux = runCatching {
            val raw = File("/sys/fs/selinux/enforce").readText().trim()
            when (raw) {
                "1" -> "Enforcing"
                "0" -> "Permissive"
                else -> null
            }
        }.getOrNull() ?: if (rootedLive) {
            runCatching {
                val p = ProcessBuilder("su", "-c", "getenforce").start()
                if (p.waitFor(3, TimeUnit.SECONDS)) p.inputStream.bufferedReader().use { it.readText().trim() }
                else { runCatching { p.destroyForcibly() }; null }
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Enforcing"
        } else {
            "Restricted"
        }

        var ramTotal = 0L
        var ramAvail = 0L
        runCatching {
            val am = context.getSystemService(ActivityManager::class.java)
            val mi = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(mi)
            ramTotal = mi.totalMem
            ramAvail = mi.availMem
        }

        var storageTotal = 0L
        var storageUsed = 0L
        runCatching {
            val st = StatFs(Environment.getDataDirectory().absolutePath)
            storageTotal = st.totalBytes
            storageUsed = (st.totalBytes - st.availableBytes).coerceAtLeast(0L)
        }

        val displaySummary = runCatching {
            val wm = context.getSystemService(WindowManager::class.java)
            val b = wm?.maximumWindowMetrics?.bounds
            val hz = runCatching { context.display.refreshRate }.getOrDefault(60f)
            if (b != null) "${b.width()}×${b.height()} @ ${"%.0f".format(hz)} Hz" else "—"
        }.getOrDefault("—")

        val batterySummary = runCatching {
            val batt = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batt?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batt?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                val pct = level * 100 / scale
                val state = when (batt?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    else -> "On battery"
                }
                "$pct% · $state"
            } else "—"
        }.getOrDefault("—")

        DeviceTelemetry(
            marketingName = marketingNameOnly(),
            modelCode = model.ifBlank { Build.DEVICE ?: "?" },
            socName = resolveSocName(),
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "?",
            cpuCores = Runtime.getRuntime().availableProcessors(),
            is64Bit = is64Bit(),
            androidVersion = Build.VERSION.RELEASE ?: "?",
            oneUiVersion = oneUiVersion(),
            securityPatch = Build.VERSION.SECURITY_PATCH ?: "Unknown",
            kernelRelease = kernel,
            kmi = parsedKmi,
            knoxState = knox?.first,
            knoxTripped = knox?.second,
            bootloaderState = bootloaderState(),
            selinuxState = selinux,
            rootState = rootState,
            rootDetail = rootDetail,
            rootedLive = rootedLive,
            moduleLoaded = moduleLoaded,
            installedManagers = managers,
            fingerprint = Build.FINGERPRINT ?: Build.DISPLAY ?: "?",
            ramUsedBytes = (ramTotal - ramAvail).coerceAtLeast(0L),
            ramTotalBytes = ramTotal,
            ramAvailBytes = ramAvail,
            storageUsedBytes = storageUsed,
            storageTotalBytes = storageTotal,
            displaySummary = displaySummary,
            batterySummary = batterySummary,
            uptimeSummary = fmtUptime(SystemClock.elapsedRealtime()),
            loaded = true,
        )
    }

    fun formatDiagnosticsReport(
        t: DeviceTelemetry,
        verdict: CompatibilityVerdict,
        newMethod: Boolean,
        ksuNext: Boolean,
    ): String = buildString {
        appendLine("UniRoot Next Diagnostics (v${com.uniroot.app.BuildConfig.VERSION_NAME} / ${com.uniroot.app.BuildConfig.VERSION_CODE})")
        appendLine("Device:       ${t.marketingName} (${t.modelCode})")
        appendLine("SoC / ABI:    ${t.socName} · ${t.abi} (${t.cpuCores} cores, ${if (t.is64Bit) "64-bit" else "32-bit"})")
        appendLine("Android:      ${t.androidVersion}${t.oneUiVersion?.let { " · One UI $it" } ?: ""} · Patch ${t.securityPatch}")
        appendLine("Kernel / KMI: ${t.kernelRelease} (${t.kmi ?: "Non-GKI"})")
        appendLine("Security:     Bootloader ${t.bootloaderState} · SELinux ${t.selinuxState}${t.knoxState?.let { " · Knox $it" } ?: ""}")
        appendLine("Root Status:  ${t.rootState} (${t.rootDetail})")
        appendLine("Managers:     ${t.installedManagers.joinToString(", ") { "${it.label} ${it.versionName} (${it.versionCode})" }.ifEmpty { "None installed" }}")
        appendLine("Mode:         ${if (newMethod) "New Exploit (DirtyFrag)" else "Old Exploit (Profiles)"} · ${if (ksuNext) "KernelSU Next" else "KernelSU"}")
        appendLine("Verdict:      [${verdict.level}] ${verdict.badge} — ${verdict.reason}")
        appendLine("Fingerprint:  ${t.fingerprint}")
    }

    fun fmtBytesGb(b: Long): String {
        if (b <= 0L) return "—"
        val gb = b / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 10) String.format("%.0f GB", gb) else String.format("%.1f GB", gb)
    }

    private fun fmtUptime(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
