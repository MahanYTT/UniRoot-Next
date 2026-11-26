package com.uniroot.app.newmethod

import android.content.Context
import com.uniroot.app.BuildConfig
import com.uniroot.app.engine.SemVer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.zip.ZipException

sealed class KsudUpdateCheckState {
    data object Idle : KsudUpdateCheckState()
    data class Checking(val localVersion: String) : KsudUpdateCheckState()
    data class UpToDate(val localVersion: String, val remoteTag: String, val checkedAtMs: Long) : KsudUpdateCheckState()
    data class UpdateAvailable(
        val localVersion: String,
        val remoteTag: String,
        val niceName: String,
        val repo: String,
    ) : KsudUpdateCheckState()
    data class Error(val localVersion: String, val message: String) : KsudUpdateCheckState()
}

/**
 * Downloads the latest official `ksud` (KernelSU-Next or KernelSU Classic) from GitHub Releases
 * and injects the 8 bundled Samsung KDP+DEFEX `kernelsu.ko` modules in-place via raw DEFLATE
 * stream replacement + zero-padding (no ELF section shifted).
 */
object DfKsudUpdater {

    private val REPOS_NEXT = listOf("KernelSU-Next/KernelSU-Next", "rifsxd/KernelSU-Next")
    private val REPOS_CLASSIC = listOf("tiann/KernelSU", "KernelSU/KernelSU")
    private const val KO_ASSET_DIR = "df/samsung-ko"
    private const val KO_ASSET_DIR_CLASSIC = "df/samsung-ko-classic"
    private val KMIS = listOf(
        "android12-5.10", "android13-5.10", "android13-5.15", "android14-5.15",
        "android14-6.1", "android15-6.6", "android16-6.12", "android17-6.18",
    )

    private const val PREFS = "uniroot_prefs"
    private const val CACHE_TTL_MS = 6L * 60L * 60L * 1000L // 6 hours
    private const val MAX_ELF_INFLATE_BYTES = 48 shl 20

    private data class ReleaseMeta(
        val tag: String,
        val repo: String,
        val json: String,
        val niceName: String,
        val ksudDownloadUrl: String,
    )

    private fun localBestVersion(context: Context, classic: Boolean): String =
        if (classic) KsudClassicProfiles.highestVersion(context)
        else KsudNextProfiles.highestVersion(context)

    private fun isRemoteNewer(remoteTag: String, localVerStr: String): Boolean {
        val remote = SemVer.parse(remoteTag)
        val local = SemVer.parse(localVerStr)
        return if (remote != null && local != null) remote > local
        else remoteTag.removePrefix("v").removePrefix("V") != localVerStr.removePrefix("v").removePrefix("V")
    }

    // ------------------------------------------------------------------
    // Lightweight background release check (6h TTL + ETag)
    // ------------------------------------------------------------------

    suspend fun checkLatest(
        context: Context,
        classic: Boolean,
        forceRefresh: Boolean = false,
    ): KsudUpdateCheckState = withContext(Dispatchers.IO) {
        val localVer = localBestVersion(context, classic)
        val prefix = if (classic) "ksud_chk_classic_" else "ksud_chk_next_"
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastMs = prefs.getLong("${prefix}last_ms", 0L)
        val cachedTag = prefs.getString("${prefix}tag", null)
        val cachedCode = prefs.getString("${prefix}code", null)
        val cachedRepo = prefs.getString("${prefix}repo", null)
        val cachedEtag = prefs.getString("${prefix}etag", null)

        if (!forceRefresh && !cachedTag.isNullOrBlank() && (now - lastMs) in 0 until CACHE_TTL_MS) {
            return@withContext buildCheckState(localVer, cachedTag, cachedCode, cachedRepo ?: "", lastMs)
        }

        val repos = if (classic) REPOS_CLASSIC else REPOS_NEXT
        var lastError = "GitHub API unreachable"
        for (repo in repos) {
            val url = "https://api.github.com/repos/$repo/releases/latest"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 18_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "UniRoot-Next/${BuildConfig.VERSION_NAME}")
                if (!cachedEtag.isNullOrBlank() && !cachedTag.isNullOrBlank() && cachedRepo == repo) {
                    setRequestProperty("If-None-Match", cachedEtag)
                }
            }
            try {
                conn.connect()
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_NOT_MODIFIED && !cachedTag.isNullOrBlank()) {
                    prefs.edit().putLong("${prefix}last_ms", now).apply()
                    return@withContext buildCheckState(localVer, cachedTag, cachedCode, repo, now)
                }
                if (code == 403) {
                    val rem = conn.getHeaderField("X-RateLimit-Remaining")
                    lastError = if (rem == "0") "Rate limited (60/hr)" else "HTTP 403"
                    continue
                }
                if (code !in 200..299) {
                    lastError = "HTTP $code"
                    continue
                }
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val meta = parseReleaseMeta(json, repo)
                if (meta != null) {
                    val newEtag = conn.getHeaderField("ETag")
                    val apkCode = extractApkCode(json)
                    prefs.edit()
                        .putLong("${prefix}last_ms", now)
                        .putString("${prefix}tag", meta.tag)
                        .putString("${prefix}code", apkCode ?: "")
                        .putString("${prefix}repo", repo)
                        .apply { if (!newEtag.isNullOrBlank()) putString("${prefix}etag", newEtag) }
                        .apply()
                    return@withContext buildCheckState(localVer, meta.tag, apkCode, repo, now)
                }
            } catch (e: Exception) {
                lastError = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
            } finally {
                conn.disconnect()
            }
        }

        if (!forceRefresh && !cachedTag.isNullOrBlank()) {
            return@withContext buildCheckState(localVer, cachedTag, cachedCode, cachedRepo ?: "", lastMs)
        }
        KsudUpdateCheckState.Error(localVer, lastError)
    }

    private fun buildCheckState(
        localVer: String,
        remoteTag: String,
        apkCode: String?,
        repo: String,
        checkedAtMs: Long,
    ): KsudUpdateCheckState {
        val ver = remoteTag.removePrefix("v").removePrefix("V")
        val nice = if (!apkCode.isNullOrBlank()) "$ver-$apkCode" else "$ver-samsung"
        return if (isRemoteNewer(remoteTag, localVer)) {
            KsudUpdateCheckState.UpdateAvailable(localVer, remoteTag, nice, repo)
        } else {
            KsudUpdateCheckState.UpToDate(localVer, remoteTag, checkedAtMs)
        }
    }

    // ------------------------------------------------------------------
    // Deflate / inflate (reusable Inflater, zero native leaks, chunked RAM)
    // ------------------------------------------------------------------

    /** Inflates up to [out.size] header bytes using a reused [Inflater] without allocating per offset. */
    private fun probeHeader(inf: Inflater, data: ByteArray, off: Int, out: ByteArray): Int {
        val limit = minOf(262_144, data.size - off)
        if (limit <= 0) return -1
        return try {
            inf.reset()
            inf.setInput(data, off, limit)
            val n = inf.inflate(out)
            if (n > 0) n else -1
        } catch (_: ZipException) {
            -1
        } catch (_: Exception) {
            -1
        }
    }

    /**
     * Inflates a full raw DEFLATE stream in ONE pass into a chunked [ByteArrayOutputStream],
     * verifying `inf.finished()` and returning `Pair(payload, consumedCompressedBytes)`.
     * Avoids allocating 64 MB upfront and eliminates double inflation.
     */
    private fun inflateFull(data: ByteArray, off: Int): Pair<ByteArray, Int>? {
        val inputLen = data.size - off
        if (inputLen <= 0) return null
        val inf = Inflater(true)
        return try {
            inf.setInput(data, off, inputLen)
            val out = ByteArrayOutputStream(65_536)
            val chunk = ByteArray(65_536)
            while (!inf.finished()) {
                val n = inf.inflate(chunk)
                if (n > 0) {
                    if (out.size() + n > MAX_ELF_INFLATE_BYTES) return null
                    out.write(chunk, 0, n)
                } else {
                    if (inf.needsInput() || inf.needsDictionary()) break
                }
            }
            if (!inf.finished() || out.size() == 0) return null
            val consumed = inputLen - inf.remaining
            if (consumed <= 0) return null
            out.toByteArray() to consumed
        } catch (_: Exception) {
            null
        } finally {
            inf.end()
        }
    }

    private fun deflateRaw(raw: ByteArray, level: Int, strategy: Int): ByteArray {
        val def = Deflater(level, true)
        return try {
            def.setStrategy(strategy)
            def.setInput(raw)
            def.finish()
            val out = ByteArrayOutputStream(raw.size / 2 + 1024)
            val chunk = ByteArray(32_768)
            while (!def.finished()) {
                val n = def.deflate(chunk)
                if (n > 0) out.write(chunk, 0, n)
                else if (def.needsInput()) break
            }
            out.toByteArray()
        } finally {
            def.end()
        }
    }

    /** Returns the smallest raw DEFLATE stream that strictly fits within [maxSize], or null if impossible. */
    private fun compressBest(raw: ByteArray, maxSize: Int): ByteArray? {
        var best: ByteArray? = null
        for (level in intArrayOf(9, 6, 1)) {
            for (strategy in intArrayOf(Deflater.DEFAULT_STRATEGY, Deflater.FILTERED, Deflater.HUFFMAN_ONLY)) {
                val comp = deflateRaw(raw, level, strategy)
                if (best == null || comp.size < best.size) best = comp
                if (comp.size <= maxSize) return comp
            }
        }
        return best?.takeIf { it.size <= maxSize }
    }

    private fun containsBytes(data: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > data.size) return false
        outer@ for (i in 0..data.size - needle.size) {
            for (j in needle.indices) if (data[i + j] != needle[j]) continue@outer
            return true
        }
        return false
    }

    private fun containsBytes(data: ByteArray, needle: ByteArray, at: Int): Boolean {
        for (j in needle.indices) if (data[at + j] != needle[j]) return false
        return true
    }

    private fun byteIndexOfFrom(data: ByteArray, element: Byte, from: Int): Int {
        for (i in from until data.size) if (data[i] == element) return i
        return -1
    }

    // ------------------------------------------------------------------
    // Stream scan (reusable Inflater — zero native leak, full-file coverage)
    // ------------------------------------------------------------------

    private class Stream(val start: Int, val consumed: Int, val payload: ByteArray) {
        var vermagic: String = ""
        var sha: String = ""
        var kind: String = "unknown"
        var kmi: String = ""
        var how: String = ""
    }

    private fun scanStreams(data: ByteArray): List<Stream> {
        val out = ArrayList<Stream>()
        val probeInf = Inflater(true)
        val headBuf = ByteArray(4096)
        try {
            var off = 0
            val n = data.size - 16
            while (off < n) {
                val b = data[off].toInt() and 7
                if (b == 4 || b == 5) {
                    val hLen = probeHeader(probeInf, data, off, headBuf)
                    if (hLen >= 4 &&
                        headBuf[0] == 0x7f.toByte() &&
                        headBuf[1] == 'E'.code.toByte() &&
                        headBuf[2] == 'L'.code.toByte() &&
                        headBuf[3] == 'F'.code.toByte()
                    ) {
                        val inflated = inflateFull(data, off)
                        if (inflated != null) {
                            val (full, used) = inflated
                            out.add(Stream(off, used, full))
                            off += used
                            continue
                        }
                    }
                }
                off++
            }
        } finally {
            probeInf.end()
        }
        return out
    }

    // ------------------------------------------------------------------
    // Classification
    // ------------------------------------------------------------------

    private val MAJOR_TO_KMIS = mapOf(
        "5.10" to listOf("android12-5.10", "android13-5.10"),
        "5.15" to listOf("android13-5.15", "android14-5.15"),
        "6.1" to listOf("android14-6.1"),
        "6.6" to listOf("android15-6.6"),
        "6.12" to listOf("android16-6.12"),
        "6.18" to listOf("android17-6.18"),
    )

    private fun vermagicOf(payload: ByteArray): String {
        val needle = "vermagic=".toByteArray()
        var i = 0
        while (i + needle.size < payload.size) {
            if (payload[i] == needle[0] && containsBytes(payload, needle, i)) {
                val end = byteIndexOfFrom(payload, 0.toByte(), i + needle.size)
                    .let { if (it < 0) payload.size else it }
                val vm = payload.copyOfRange(i + needle.size, minOf(end, i + needle.size + 120))
                    .toString(Charsets.ISO_8859_1)
                if (Regex("^\\d+\\.\\d+\\.\\d+").containsMatchIn(vm) && vm.contains("mod_unload")) return vm
            }
            i++
        }
        return ""
    }

    private fun kmiFromVermagic(vm: String): String {
        val first = vm.split(" ").firstOrNull() ?: ""
        val major = Regex("^(\\d+\\.\\d+)").find(first)?.groupValues?.get(1) ?: ""
        val cands = MAJOR_TO_KMIS[major] ?: return ""
        if (cands.size == 1) return cands[0]
        for (c in cands) {
            val tag = Regex("^android(\\d+)-").find(c)?.groupValues?.get(1) ?: continue
            if (first.contains("android$tag")) return c
        }
        return ""
    }

    private fun classifyInitial(streams: MutableList<Stream>) {
        val susfsNeedle = "susfs".toByteArray()
        val busyboxNeedle = "BusyBox".toByteArray()
        val ksuinit1 = "Replaced module vermagic".toByteArray()
        val ksuinit2 = "Hello, KernelSU".toByteArray()
        val bootctlNeedle = "bootctl".toByteArray()

        for (s in streams) {
            s.vermagic = vermagicOf(s.payload)
            s.sha = sha256Hex(s.payload)
            if (s.vermagic.isNotEmpty()) {
                if (containsBytes(s.payload, susfsNeedle)) {
                    s.kind = "susfs_ko"
                    continue
                }
                s.kind = "ko"
                val k = kmiFromVermagic(s.vermagic)
                if (k.isNotEmpty()) {
                    s.kmi = k
                    s.how = "vermagic"
                }
            } else {
                s.kind = when {
                    containsBytes(s.payload, busyboxNeedle) -> "busybox"
                    containsBytes(s.payload, ksuinit1) -> "ksuinit"
                    containsBytes(s.payload, ksuinit2) -> "ksuinit"
                    containsBytes(s.payload, bootctlNeedle) -> "bootctl"
                    else -> "unknown"
                }
            }
        }
    }

    private fun sha256Hex(b: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    // ------------------------------------------------------------------
    // Downloads
    // ------------------------------------------------------------------

    private fun httpGet(url: String, isGithubApi: Boolean = false): ByteArray {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 45_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "UniRoot-Next/${BuildConfig.VERSION_NAME}")
            if (isGithubApi) setRequestProperty("Accept", "application/vnd.github+json")
        }
        return try {
            conn.connect()
            val code = conn.responseCode
            if (code == 403 && conn.getHeaderField("X-RateLimit-Remaining") == "0") {
                throw IllegalStateException("GitHub API rate limit reached (HTTP 403). Try again later.")
            }
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code for $url")
            }
            conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private fun extractApkCode(releaseJson: String): String? =
        Regex("""KernelSU(?:_Next)?_v[\d.]+_(\d+)-release\.apk""").find(releaseJson)?.groupValues?.get(1)

    private fun parseReleaseMeta(jsonText: String, repo: String): ReleaseMeta? = runCatching {
        val obj = JSONObject(jsonText)
        val tag = obj.optString("tag_name", "").trim()
        if (tag.isEmpty()) return null
        val version = tag.removePrefix("v").removePrefix("V")
        val code = extractApkCode(jsonText)
        val niceName = if (code != null) "$version-$code" else "$version-samsung"
        var ksudUrl: String? = null
        val assets = obj.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name", "")
                if (name == "ksud-aarch64-linux-android") {
                    ksudUrl = a.optString("browser_download_url", "").takeIf { it.isNotBlank() }
                    break
                }
            }
        }
        val finalUrl = ksudUrl ?: "https://github.com/$repo/releases/download/$tag/ksud-aarch64-linux-android"
        ReleaseMeta(tag = tag, repo = repo, json = jsonText, niceName = niceName, ksudDownloadUrl = finalUrl)
    }.getOrNull()

    private fun fetchLatest(classic: Boolean): ReleaseMeta {
        val repos = if (classic) REPOS_CLASSIC else REPOS_NEXT
        var lastError = "GitHub API unreachable"
        for (repo in repos) {
            try {
                val json = String(httpGet("https://api.github.com/repos/$repo/releases/latest", isGithubApi = true))
                parseReleaseMeta(json, repo)?.let { return it }
            } catch (e: Exception) {
                lastError = e.message ?: e.javaClass.simpleName
            }
        }
        throw IllegalStateException(lastError)
    }

    // ------------------------------------------------------------------
    // Main entry
    // ------------------------------------------------------------------

    fun run(
        context: Context,
        classic: Boolean = false,
        forceRepatch: Boolean = false,
        onLog: (String) -> Unit,
        onProgress: (Int, String) -> Unit = { _, _ -> },
    ): String {
        fun say(msg: String) {
            onLog(msg)
            android.util.Log.i("DfKsudUpd", msg)
        }

        val flavorName = if (classic) "KernelSU Classic" else "KernelSU Next"
        onProgress(5, "Querying latest $flavorName release…")
        say("[*] Querying latest $flavorName release…")
        val meta = fetchLatest(classic)
        say("[*] Latest upstream release: ${meta.tag} (${meta.repo})")

        val localBest = localBestVersion(context, classic)
        if (!forceRepatch && !isRemoteNewer(meta.tag, localBest)) {
            if (classic) {
                val targetId = KsudClassicProfiles.list(context).firstOrNull()?.id ?: "bundled-universal"
                KsudClassicProfiles.setSelected(context, targetId)
            } else {
                val bestProfile = KsudNextProfiles.list(context).firstOrNull()
                if (bestProfile != null) KsudNextProfiles.setSelected(context, bestProfile.id)
            }
            val msg = "Already up to date (v$localBest)"
            say("[✓] $msg (upstream ${meta.tag})")
            onProgress(100, msg)
            return msg
        }

        onProgress(20, "Downloading official ksud ${meta.tag}…")
        say("[*] Downloading official ksud from ${meta.ksudDownloadUrl}…")
        val ksud = httpGet(meta.ksudDownloadUrl)
        say("    Downloaded ${ksud.size} bytes")

        val patched = patchKsud(context, ksud, meta, classic, ::say, onProgress)

        return if (classic) {
            File(context.filesDir, "ksud-classic-latest").writeBytes(patched)
            File(context.filesDir, "ksud-classic-latest.tag").writeText(meta.tag)
            KsudClassicProfiles.selectLatest(
                context,
                meta.niceName,
                "Official ksud ${meta.tag} + Samsung KDP+DEFEX modules (patched on-device)",
            )
            say("[+] Classic profile '${meta.niceName}' saved and selected")
            onProgress(100, "Patched & selected · ${meta.niceName}")
            "Patched ${meta.tag} -> ${meta.niceName}"
        } else {
            KsudNextProfiles.addDynamic(
                context,
                meta.niceName,
                patched,
                "Official ksud ${meta.tag} + Samsung KDP+DEFEX modules (patched on-device)",
            )
            say("[+] Next profile '${meta.niceName}' created and selected")
            onProgress(100, "Patched & selected · ${meta.niceName}")
            "Patched ${meta.tag} -> ${meta.niceName}"
        }
    }

    // ------------------------------------------------------------------
    // Common patch pipeline (scan -> targeted reference fetch -> inject -> verify)
    // ------------------------------------------------------------------

    private fun patchKsud(
        context: Context,
        ksud: ByteArray,
        meta: ReleaseMeta,
        classic: Boolean,
        say: (String) -> Unit,
        progress: (Int, String) -> Unit,
    ): ByteArray {
        val koDir = if (classic) KO_ASSET_DIR_CLASSIC else KO_ASSET_DIR
        val koAssets = runCatching { context.assets.list(koDir)?.toList() ?: emptyList() }
            .getOrDefault(emptyList())
        val replacements = HashMap<String, ByteArray>()
        for (kmi in KMIS) {
            val name = koAssets.firstOrNull { it.startsWith("${kmi}_kernelsu") && it.endsWith(".ko") }
                ?: throw IllegalStateException("Samsung module missing from APK: $kmi")
            replacements[kmi] = context.assets.open("$koDir/$name").use { it.readBytes() }
        }

        progress(35, "Scanning embedded ELF/DEFLATE assets…")
        say("[*] Scanning raw DEFLATE streams in ksud…")
        val data = ksud.copyOf()
        val streams = scanStreams(data).toMutableList()
        classifyInitial(streams)
        say("    Found ${streams.size} embedded ELF assets")

        // Only fetch official reference .ko files for ambiguous KMIs not resolved by vermagic
        // (saves up to 5 HTTP requests and ~2.5 MB on every run).
        val resolvedByVermagic = streams.filter { it.kind == "ko" && it.kmi.isNotEmpty() }.map { it.kmi }.toSet()
        val unresolvedSlots = streams.filter { it.kind == "ko" && it.kmi.isEmpty() }
        val neededKmis = KMIS.filter { it !in resolvedByVermagic }

        if (unresolvedSlots.isNotEmpty() && neededKmis.isNotEmpty()) {
            progress(45, "Fetching ${neededKmis.size} reference .ko hashes…")
            say("[*] Downloading ${neededKmis.size} reference modules from ${meta.repo} ${meta.tag}…")
            val refBase = "https://github.com/${meta.repo}/releases/download/${meta.tag}"
            val refHashes = HashMap<String, String>()
            for (kmi in neededKmis) {
                val urls = if (classic) {
                    listOf(
                        "$refBase/lkm-aarch64-${kmi}_kernelsu.ko",
                        "$refBase/aarch64-${kmi}_kernelsu.ko",
                        "$refBase/${kmi}_kernelsu.ko",
                    )
                } else {
                    listOf(
                        "$refBase/aarch64_${kmi}_kernelsu.ko",
                        "$refBase/aarch64-${kmi}_kernelsu.ko",
                        "$refBase/${kmi}_kernelsu.ko",
                    )
                }
                var bytes: ByteArray? = null
                for (u in urls) {
                    bytes = runCatching { httpGet(u) }.getOrNull()
                    if (bytes != null) break
                }
                if (bytes != null) refHashes[sha256Hex(bytes)] = kmi
            }
            for (s in unresolvedSlots) {
                refHashes[s.sha]?.let { s.kmi = it; s.how = "reference" }
            }
            say("    Matched ${refHashes.size}/${neededKmis.size} ambiguous references")
        }

        val slots = HashMap<String, Stream>()
        for (s in streams) {
            if (s.kind == "ko" && s.kmi.isNotEmpty()) {
                check(s.kmi !in slots) { "Duplicate KMI slot ${s.kmi}" }
                slots[s.kmi] = s
            }
        }

        progress(60, "Compressing Samsung KDP+DEFEX modules…")
        class Plan(val kmi: String, val slot: Stream, val comp: ByteArray, val raw: ByteArray)
        val plan = ArrayList<Plan>()
        for (kmi in replacements.keys.sorted()) {
            val raw = replacements[kmi]!!
            check(raw.size >= 4 && raw[0] == 0x7f.toByte()) { "$kmi is not an ELF" }
            val slot = slots[kmi]
            if (slot == null) {
                say("[!] Slot $kmi not found/ambiguous — skipped")
                continue
            }
            val comp = compressBest(raw, slot.consumed)
                ?: throw IllegalStateException("$kmi compressed payload exceeds slot budget (${slot.consumed} bytes)")
            plan.add(Plan(kmi, slot, comp, raw))
        }
        check(plan.isNotEmpty()) { "No patchable KMI slot found in this ksud binary" }

        progress(75, "Injecting ${plan.size} Samsung modules…")
        for (p in plan) {
            val pad = p.slot.consumed - p.comp.size
            p.comp.copyInto(data, p.slot.start)
            java.util.Arrays.fill(data, p.slot.start + p.comp.size, p.slot.start + p.slot.consumed, 0)
            say("[+] ${p.kmi} @0x${Integer.toHexString(p.slot.start)}: ${p.slot.consumed} -> ${p.comp.size} B (+$pad pad)")
        }

        progress(90, "Verifying binary integrity…")
        say("[*] Verifying output streams…")
        val outStreams = scanStreams(data)
        val byStart = outStreams.associateBy { it.start }
        var ok = outStreams.size == streams.size
        for (p in plan) {
            val got = byStart[p.slot.start]
            if (got == null || !got.payload.contentEquals(p.raw)) {
                ok = false
                say("[!] ${p.kmi}: content verification mismatch")
            }
        }
        val patchedStarts = plan.map { it.slot.start }.toSet()
        for (a in streams) {
            if (a.start in patchedStarts) continue
            val got = byStart[a.start]
            if (got == null || !got.payload.contentEquals(a.payload)) {
                ok = false
                say("[!] Untouched asset modified @0x${Integer.toHexString(a.start)}")
            }
        }
        check(ok) { "Verification failed — patched binary rejected" }
        if (!classic) {
            patchNextLateLoadSoftReboot(data, say)
        }
        say("[OK] ${plan.size} modules injected, ${streams.size - plan.size} assets preserved")
        return data
    }

    private fun patchNextLateLoadSoftReboot(data: ByteArray, say: (String) -> Unit) {
        fun readLe32(off: Int): Int =
            (data[off].toInt() and 0xff) or
                ((data[off + 1].toInt() and 0xff) shl 8) or
                ((data[off + 2].toInt() and 0xff) shl 16) or
                ((data[off + 3].toInt() and 0xff) shl 24)

        fun writeLe32(off: Int, v: Int) {
            data[off] = (v and 0xff).toByte()
            data[off + 1] = ((v ushr 8) and 0xff).toByte()
            data[off + 2] = ((v ushr 16) and 0xff).toByte()
            data[off + 3] = ((v ushr 24) and 0xff).toByte()
        }

        // ksud Next 3.4.2 @ file 0x521238: bl #0x5180c8 -> b #0x51fce8
        if (data.size > 0x52123c && readLe32(0x521238) == 0x97ffd7a4.toInt()) {
            writeLe32(0x521238, 0x17fff6ac)
            say("[+] Patched Next 3.4.2 late-load -> emulated_soft_reboot @0x521238")
            return
        }
        // ksud Next 3.4.0 @ file 0x3af7c8: bl #0x3a6928 -> b #0x3ae3b4
        if (data.size > 0x3af7cc && readLe32(0x3af7c8) == 0x97ffd858.toInt()) {
            writeLe32(0x3af7c8, 0x17fff6fb)
            say("[+] Patched Next 3.4.0 late-load -> emulated_soft_reboot @0x3af7c8")
            return
        }
        say("[!] Warning: soft-reboot patch offset not matched for this ksud build; bundled 3.4.2-kdp-samsung is recommended if Zygisk/LSPosed does not auto-start.")
    }
}
