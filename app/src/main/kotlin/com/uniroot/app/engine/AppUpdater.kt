package com.uniroot.app.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import androidx.core.content.FileProvider
import com.uniroot.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.max

/**
 * Semantic version parser & comparator supporting `v5.2.2`, `5.3.0-rc1`, `3.4.2`, etc.
 * Stable releases sort higher than pre-releases of the same numeric tuple (`5.3.0 > 5.3.0-rc1`).
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String = "",
) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        if (preRelease.isEmpty() && other.preRelease.isEmpty()) return 0
        if (preRelease.isEmpty()) return 1
        if (other.preRelease.isEmpty()) return -1
        return comparePreRelease(preRelease, other.preRelease)
    }

    override fun toString(): String =
        if (preRelease.isEmpty()) "$major.$minor.$patch" else "$major.$minor.$patch-$preRelease"

    companion object {
        private val SEMVER_RE = Regex("""^[vV]?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:[-_]([A-Za-z0-9.]+))?""")

        fun parse(raw: String): SemVer? {
            val m = SEMVER_RE.find(raw.trim()) ?: return null
            val maj = m.groupValues[1].toIntOrNull() ?: return null
            val min = m.groupValues[2].toIntOrNull() ?: 0
            val pat = m.groupValues[3].toIntOrNull() ?: 0
            val pre = m.groupValues[4]
            return SemVer(maj, min, pat, pre)
        }

        private fun comparePreRelease(a: String, b: String): Int {
            val pa = a.split('.')
            val pb = b.split('.')
            for (i in 0 until max(pa.size, pb.size)) {
                val sa = pa.getOrNull(i) ?: return -1
                val sb = pb.getOrNull(i) ?: return 1
                val ia = sa.toIntOrNull()
                val ib = sb.toIntOrNull()
                val cmp = when {
                    ia != null && ib != null -> ia.compareTo(ib)
                    ia != null -> -1
                    ib != null -> 1
                    else -> sa.compareTo(sb, ignoreCase = true)
                }
                if (cmp != 0) return cmp
            }
            return 0
        }
    }
}

data class AppReleaseInfo(
    val tag: String,
    val versionName: String,
    val publishedAt: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkName: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val expectedSha256: String?,
)

sealed class AppUpdateUiState {
    data object Idle : AppUpdateUiState()
    data object Checking : AppUpdateUiState()
    data class UpToDate(val release: AppReleaseInfo?, val checkedAtMs: Long) : AppUpdateUiState()
    data class Available(val release: AppReleaseInfo) : AppUpdateUiState()
    data class Downloading(
        val release: AppReleaseInfo,
        val bytesRead: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long,
    ) : AppUpdateUiState()
    data class Verifying(
        val release: AppReleaseInfo,
        val shaPassed: Boolean?,
        val sigPassed: Boolean?,
    ) : AppUpdateUiState()
    data class PermissionRequired(
        val release: AppReleaseInfo,
        val apkPath: String,
        val shaSummary: String,
        val downgradeWarning: Boolean = false,
    ) : AppUpdateUiState()
    data class ReadyToInstall(
        val release: AppReleaseInfo,
        val apkPath: String,
        val shaSummary: String,
        val downgradeWarning: Boolean = false,
    ) : AppUpdateUiState()
    data class Error(val message: String, val release: AppReleaseInfo? = null) : AppUpdateUiState()
}

/**
 * In-app self-updater for UniRoot Next backed by GitHub Releases (`MahanYTT/UniRoot-Next`).
 *
 * - Rate-limit safe: 6-hour SharedPreferences cache + `If-None-Match` ETag (`304 Not Modified`).
 * - Streams large APKs (~140 MB) in 64 KiB chunks directly to `filesDir/updates/` with live speed & cancellation.
 * - Verifies SHA-256 (from GitHub asset `digest`, `.sha256` companion asset, or release body) AND
 *   verifies APK package name + signing certificate against the installed UniRoot Next package.
 * - Handles Android 12-17 `REQUEST_INSTALL_PACKAGES` permission flow + `FileProvider` `ACTION_VIEW` install.
 */
object AppUpdater {

    private const val REPO = "MahanYTT/UniRoot-Next"
    const val RELEASES_URL = "https://github.com/$REPO/releases/latest"
    private const val LATEST_API = "https://api.github.com/repos/$REPO/releases/latest"

    private const val PREFS = "uniroot_prefs"
    private const val KEY_AUTO_CHECK = "app_upd_auto_check"
    private const val KEY_LAST_CHECK_MS = "app_upd_last_check_ms"
    private const val KEY_ETAG = "app_upd_etag"
    private const val KEY_CACHED_JSON = "app_upd_cached_json"
    private const val KEY_DISMISSED_TAG = "app_upd_dismissed_tag"
    private const val KEY_PENDING_INSTALL_TAG = "app_upd_pending_install_tag"

    private const val CACHE_TTL_MS = 6L * 60L * 60L * 1000L // 6 hours
    private const val MAX_API_BYTES = 512 * 1024
    private const val SPACE_MARGIN_BYTES = 64L * 1024L * 1024L

    fun updatesDir(context: Context): File = File(context.filesDir, "updates").apply { mkdirs() }

    fun pendingInstallTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PENDING_INSTALL_TAG, "") ?: ""

    fun setPendingInstallTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PENDING_INSTALL_TAG, tag).apply()
    }

    fun autoCheckEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_CHECK, true)

    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_CHECK, enabled).apply()
    }

    fun dismissedTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DISMISSED_TAG, "") ?: ""

    fun setDismissedTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_DISMISSED_TAG, tag).apply()
    }

    /** Removes partial downloads and APKs for already-installed/older versions so ~140 MB isn't wasted. */
    fun cleanupOldApks(context: Context) {
        runCatching {
            val dir = updatesDir(context)
            val current = SemVer.parse(BuildConfig.VERSION_NAME)
            dir.listFiles()?.forEach { f ->
                if (f.name.endsWith(".part")) {
                    f.delete()
                } else if (f.name.endsWith(".apk")) {
                    val verStr = f.name.removePrefix("uniroot-").removeSuffix(".apk")
                    val ver = SemVer.parse(verStr)
                    if (ver == null || (current != null && ver <= current)) {
                        f.delete()
                    }
                }
            }
        }
    }

    /** Checks GitHub Releases for the latest UniRoot Next build with 6h TTL + ETag caching. */
    suspend fun checkLatest(context: Context, forceRefresh: Boolean = false): AppUpdateUiState =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val lastCheck = prefs.getLong(KEY_LAST_CHECK_MS, 0L)
            val cachedJson = prefs.getString(KEY_CACHED_JSON, null)
            val etag = prefs.getString(KEY_ETAG, null)

            if (!forceRefresh && !cachedJson.isNullOrBlank() && (now - lastCheck) in 0 until CACHE_TTL_MS) {
                parseRelease(cachedJson)?.let { release ->
                    return@withContext evaluateAgainstCurrent(context, release, lastCheck)
                }
            }

            try {
                val conn = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "UniRoot-Next/${BuildConfig.VERSION_NAME}")
                    if (!etag.isNullOrBlank() && !cachedJson.isNullOrBlank()) {
                        setRequestProperty("If-None-Match", etag)
                    }
                }
                try {
                    conn.connect()
                    val code = conn.responseCode
                    if (code == HttpURLConnection.HTTP_NOT_MODIFIED && !cachedJson.isNullOrBlank()) {
                        prefs.edit().putLong(KEY_LAST_CHECK_MS, now).apply()
                        val release = parseRelease(cachedJson)
                            ?: return@withContext AppUpdateUiState.Error("Cached release metadata invalid")
                        return@withContext evaluateAgainstCurrent(context, release, now)
                    }
                    if (code == 403) {
                        val remaining = conn.getHeaderField("X-RateLimit-Remaining")
                        val msg = if (remaining == "0") {
                            "GitHub API rate limit reached (60/hr). Try again later or open GitHub Releases."
                        } else {
                            "GitHub API refused request (HTTP 403)."
                        }
                        return@withContext fallbackOrError(context, cachedJson, lastCheck, msg, forceRefresh)
                    }
                    if (code !in 200..299) {
                        return@withContext fallbackOrError(context, cachedJson, lastCheck, "GitHub API returned HTTP $code.", forceRefresh)
                    }
                    val body = conn.inputStream.use { input ->
                        val out = ByteArrayOutputStream()
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            if (out.size() + n > MAX_API_BYTES) error("Release response too large")
                            out.write(buf, 0, n)
                        }
                        out.toString(Charsets.UTF_8.name())
                    }
                    val newEtag = conn.getHeaderField("ETag")
                    val release = parseRelease(body)
                        ?: return@withContext AppUpdateUiState.Error("No release APK found in latest GitHub release.")
                    prefs.edit()
                        .putLong(KEY_LAST_CHECK_MS, now)
                        .putString(KEY_CACHED_JSON, body)
                        .apply { if (!newEtag.isNullOrBlank()) putString(KEY_ETAG, newEtag) }
                        .apply()
                    evaluateAgainstCurrent(context, release, now)
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                val msg = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
                fallbackOrError(context, cachedJson, lastCheck, "Update check failed: $msg", forceRefresh)
            }
        }

    private fun fallbackOrError(
        context: Context,
        cachedJson: String?,
        lastCheck: Long,
        errorMsg: String,
        forceRefresh: Boolean,
    ): AppUpdateUiState {
        if (!forceRefresh && !cachedJson.isNullOrBlank()) {
            parseRelease(cachedJson)?.let { return evaluateAgainstCurrent(context, it, lastCheck) }
        }
        val cachedRelease = cachedJson?.let { parseRelease(it) }
        return AppUpdateUiState.Error(errorMsg, cachedRelease)
    }

    private fun evaluateAgainstCurrent(context: Context, release: AppReleaseInfo, checkedAtMs: Long): AppUpdateUiState {
        // If we already downloaded & verified this release before Android restarted the process
        // (e.g., when granting "Allow install unknown apps"), resume directly at ReadyToInstall!
        findVerifiedDownloadedApk(context, release)?.let { return it }

        val remoteVer = SemVer.parse(release.versionName)
        val localVer = SemVer.parse(BuildConfig.VERSION_NAME)
        val isNewer = if (remoteVer != null && localVer != null) {
            remoteVer > localVer
        } else {
            release.versionName != BuildConfig.VERSION_NAME
        }
        return if (isNewer) {
            AppUpdateUiState.Available(release)
        } else {
            AppUpdateUiState.UpToDate(release, checkedAtMs)
        }
    }

    /** Reuses a previously downloaded APK on disk if size, packageName, and release signature still verify. */
    private fun findVerifiedDownloadedApk(context: Context, release: AppReleaseInfo): AppUpdateUiState? {
        val safeTag = release.tag.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(updatesDir(context), "uniroot-$safeTag.apk")
        if (!file.isFile || file.length() == 0L) return null
        if (release.apkSizeBytes > 0L && file.length() != release.apkSizeBytes) return null
        val archiveInfo = getArchiveInfo(context.packageManager, file.absolutePath) ?: return null
        if (archiveInfo.packageName != context.packageName) return null
        if (!signaturesCompatible(context, archiveInfo)) return null
        val downgrade = archiveInfo.longVersionCode < BuildConfig.VERSION_CODE.toLong()
        val summary = "Cached APK verified · Release signature match"
        return if (context.packageManager.canRequestPackageInstalls()) {
            AppUpdateUiState.ReadyToInstall(release, file.absolutePath, summary, downgrade)
        } else {
            AppUpdateUiState.PermissionRequired(release, file.absolutePath, summary, downgrade)
        }
    }

    private fun parseRelease(jsonText: String): AppReleaseInfo? = runCatching {
        val obj = JSONObject(jsonText)
        val tag = obj.optString("tag_name", "").trim()
        if (tag.isEmpty()) return null
        val versionName = tag.removePrefix("v").removePrefix("V")
        val publishedAt = obj.optString("published_at", "").trim()
        val htmlUrl = obj.optString("html_url", RELEASES_URL).trim().ifEmpty { RELEASES_URL }
        val body = if (obj.isNull("body")) "" else obj.optString("body", "").trim()

        val assets = obj.optJSONArray("assets") ?: return null
        data class Candidate(val name: String, val url: String, val size: Long, val digest: String?)
        val apks = mutableListOf<Candidate>()
        var shaAssetUrl: String? = null

        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name", "")
            val url = a.optString("browser_download_url", "")
            val size = a.optLong("size", -1L)
            val digestRaw = if (a.isNull("digest")) "" else a.optString("digest", "")
            val digest = digestRaw.removePrefix("sha256:").trim()
                .takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }?.lowercase()
            if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                apks += Candidate(name, url, size, digest)
            } else if ((name.endsWith(".sha256", ignoreCase = true) || name.equals("SHA256SUMS", ignoreCase = true)) && url.isNotBlank()) {
                shaAssetUrl = url
            }
        }
        val chosen = apks.firstOrNull { it.name.equals("app-release.apk", ignoreCase = true) }
            ?: apks.firstOrNull { it.name.contains("release", ignoreCase = true) && !it.name.contains("debug", ignoreCase = true) }
            ?: apks.firstOrNull { !it.name.contains("debug", ignoreCase = true) }
            ?: apks.firstOrNull()
            ?: return null

        val expectedSha = chosen.digest
            ?: extractShaFromText(body, chosen.name)
            ?: shaAssetUrl?.let { fetchShaFromAsset(it, chosen.name) }

        AppReleaseInfo(
            tag = tag,
            versionName = versionName,
            publishedAt = publishedAt,
            releaseNotes = body,
            htmlUrl = htmlUrl,
            apkName = chosen.name,
            apkUrl = chosen.url,
            apkSizeBytes = chosen.size,
            expectedSha256 = expectedSha,
        )
    }.getOrNull()

    private fun extractShaFromText(text: String, apkName: String): String? {
        if (text.isBlank()) return null
        val escaped = Regex.escape(apkName)
        Regex("""([0-9a-fA-F]{64})\s+\*?.*$escaped""", RegexOption.IGNORE_CASE).find(text)
            ?.groupValues?.get(1)?.lowercase()?.let { return it }
        Regex("""sha-?256\s*[:=]\s*`?([0-9a-fA-F]{64})`?""", RegexOption.IGNORE_CASE).find(text)
            ?.groupValues?.get(1)?.lowercase()?.let { return it }
        return null
    }

    private fun fetchShaFromAsset(url: String, apkName: String): String? = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "UniRoot-Next/${BuildConfig.VERSION_NAME}")
        }
        try {
            conn.connect()
            if (conn.responseCode !in 200..299) return null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            extractShaFromText(text, apkName)
                ?: Regex("""\b([0-9a-fA-F]{64})\b""").find(text)?.groupValues?.get(1)?.lowercase()
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /**
     * Downloads the release APK with live progress/speed callbacks, verifies SHA-256 + package name +
     * signing certificate match, and returns ReadyToInstall, PermissionRequired, or Error.
     */
    suspend fun downloadAndVerify(
        context: Context,
        release: AppReleaseInfo,
        onProgress: (bytesRead: Long, totalBytes: Long, speedBytesPerSec: Long) -> Unit,
        onVerifying: (shaPassed: Boolean?, sigPassed: Boolean?) -> Unit,
    ): AppUpdateUiState = withContext(Dispatchers.IO) {
        val dir = updatesDir(context)
        val safeTag = release.tag.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val finalFile = File(dir, "uniroot-$safeTag.apk")
        val partFile = File(dir, "uniroot-$safeTag.apk.part")

        // Check available free disk space before streaming ~140 MB.
        val needed = (if (release.apkSizeBytes > 0) release.apkSizeBytes else 160L * 1024L * 1024L) + SPACE_MARGIN_BYTES
        val avail = runCatching { StatFs(dir.absolutePath).availableBytes }.getOrDefault(Long.MAX_VALUE)
        if (avail < needed) {
            return@withContext AppUpdateUiState.Error(
                "Not enough storage space (${formatBytes(avail)} free, need ~${formatBytes(needed)}).",
                release,
            )
        }

        // Remove older partial or stale APKs before downloading.
        dir.listFiles()?.forEach { f ->
            if (f != finalFile) runCatching { f.delete() }
        }

        val shaDigest = MessageDigest.getInstance("SHA-256")
        val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 45_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "UniRoot-Next/${BuildConfig.VERSION_NAME}")
        }

        var actualSha: String
        try {
            conn.connect()
            if (conn.responseCode !in 200..299) {
                return@withContext AppUpdateUiState.Error("Download failed (HTTP ${conn.responseCode}).", release)
            }
            val total = if (release.apkSizeBytes > 0) release.apkSizeBytes else conn.contentLengthLong
            var read = 0L
            var lastTickMs = System.nanoTime() / 1_000_000L
            var lastTickBytes = 0L
            var speed = 0L

            conn.inputStream.use { input ->
                FileOutputStream(partFile).use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        shaDigest.update(buf, 0, n)
                        read += n
                        val nowMs = System.nanoTime() / 1_000_000L
                        val dt = nowMs - lastTickMs
                        if (dt >= 250L) {
                            val dBytes = read - lastTickBytes
                            speed = if (dt > 0) (dBytes * 1000L) / dt else speed
                            lastTickMs = nowMs
                            lastTickBytes = read
                            onProgress(read, total, speed)
                        }
                    }
                    output.fd.sync()
                }
            }
            onProgress(read, if (total > 0) total else read, speed)
            if (release.apkSizeBytes > 0 && read != release.apkSizeBytes) {
                partFile.delete()
                return@withContext AppUpdateUiState.Error(
                    "Incomplete download (${formatBytes(read)} / ${formatBytes(release.apkSizeBytes)}).",
                    release,
                )
            }
            if (finalFile.exists()) finalFile.delete()
            if (!partFile.renameTo(finalFile)) {
                partFile.delete()
                return@withContext AppUpdateUiState.Error("Could not finalize downloaded APK.", release)
            }
            actualSha = shaDigest.digest().joinToString("") { "%02x".format(it) }
        } catch (ce: kotlinx.coroutines.CancellationException) {
            runCatching { partFile.delete() }
            throw ce
        } catch (e: Exception) {
            runCatching { partFile.delete() }
            return@withContext AppUpdateUiState.Error(
                "Download interrupted: ${e.message ?: e.javaClass.simpleName}",
                release,
            )
        } finally {
            conn.disconnect()
        }

        // Verification Step 1: SHA-256 integrity
        onVerifying(null, null)
        val expected = release.expectedSha256
        val shaSummary = if (expected != null) {
            if (!actualSha.equals(expected, ignoreCase = true)) {
                runCatching { finalFile.delete() }
                return@withContext AppUpdateUiState.Error(
                    "SHA-256 mismatch — file corrupted or tampered. Download discarded.",
                    release,
                )
            }
            onVerifying(true, null)
            "SHA-256 verified (${actualSha.take(12)}…)"
        } else {
            onVerifying(true, null)
            "SHA-256: ${actualSha.take(12)}…"
        }

        // Verification Step 2: APK archive package & signing certificate
        val archiveInfo = getArchiveInfo(context.packageManager, finalFile.absolutePath)
            ?: run {
                runCatching { finalFile.delete() }
                return@withContext AppUpdateUiState.Error("Downloaded file is not a valid Android APK.", release)
            }
        if (archiveInfo.packageName != context.packageName) {
            runCatching { finalFile.delete() }
            return@withContext AppUpdateUiState.Error(
                "Package ID mismatch (${archiveInfo.packageName} != ${context.packageName}).",
                release,
            )
        }

        if (!signaturesCompatible(context, archiveInfo)) {
            runCatching { finalFile.delete() }
            val msg = if (BuildConfig.DEBUG) {
                "Debug build signing key differs from GitHub release APK. Android cannot upgrade in-place across different signing keys — open GitHub Releases to install cleanly."
            } else {
                "APK signing certificate does not match the installed UniRoot Next key. Update rejected for safety."
            }
            return@withContext AppUpdateUiState.Error(msg, release)
        }
        onVerifying(true, true)

        val downgrade = archiveInfo.longVersionCode < BuildConfig.VERSION_CODE.toLong()
        val fullSummary = "$shaSummary · Release signature verified"

        return@withContext if (context.packageManager.canRequestPackageInstalls()) {
            AppUpdateUiState.ReadyToInstall(release, finalFile.absolutePath, fullSummary, downgrade)
        } else {
            AppUpdateUiState.PermissionRequired(release, finalFile.absolutePath, fullSummary, downgrade)
        }
    }

    fun installIntent(context: Context, apkPath: String): Intent? {
        val file = File(apkPath)
        if (!file.isFile || file.length() == 0L) return null
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun unknownSourcesIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    private fun getArchiveInfo(pm: PackageManager, apkPath: String): PackageInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageArchiveInfo(
                apkPath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNING_CERTIFICATES)
        }
    }.getOrNull()

    /** Enforces Android's real upgrade signing rule: exact signer-set match (or valid key-rotation history). */
    private fun signaturesCompatible(context: Context, archiveInfo: PackageInfo): Boolean {
        val installedInfo = runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            }
        }.getOrNull() ?: return false

        val installedSigners = digestList(installedInfo.signingInfo?.apkContentsSigners)
        val archiveSigners = digestList(archiveInfo.signingInfo?.apkContentsSigners)
        if (installedSigners.isEmpty() || archiveSigners.isEmpty()) return false
        if (installedSigners == archiveSigners) return true
        // Single-signer key rotation: new APK's certificate history must contain the installed signer.
        if (installedSigners.size == 1 && archiveSigners.size == 1) {
            val history = digestList(archiveInfo.signingInfo?.signingCertificateHistory)
            if (history.containsAll(installedSigners)) return true
        }
        return false
    }

    private fun digestList(sigs: Array<android.content.pm.Signature>?): Set<String> {
        if (sigs.isNullOrEmpty()) return emptySet()
        val md = MessageDigest.getInstance("SHA-256")
        return sigs.map { sig ->
            md.reset()
            md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 0L) return "—"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatPublishedDate(iso: String): String {
        val datePart = iso.substringBefore('T').trim()
        return datePart.ifEmpty { "Unknown date" }
    }
}
