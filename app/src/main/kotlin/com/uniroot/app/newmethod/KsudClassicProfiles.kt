package com.uniroot.app.newmethod

import android.content.Context
import com.uniroot.app.engine.SemVer
import java.io.File

/**
 * Classic KernelSU ksud profiles for New Method:
 * 1. Bundled: `assets/df/ksud-new-classic` (v3.3.0 universal Samsung KDP+DEFEX).
 * 2. Dynamic: `filesDir/ksud-classic-latest` downloaded & patched by `DfKsudUpdater`.
 */
object KsudClassicProfiles {

    private const val PREFS = "uniroot_prefs"
    private const val PREF_KEY = "ksud_classic_profile"
    private const val MODE = Context.MODE_PRIVATE

    /** Version of the bundled `assets/df/ksud-new-classic` binary (code 32653). */
    const val BUNDLED_VERSION = "3.3.0"

    /** Bumped whenever bundled `assets/df/samsung-ko-classic` modules change. */
    const val KO_PATCH_REV = "v5.2.0-classic-kdp"

    data class ClassicProfile(
        val id: String,       // "bundled-universal" or "<version>-<code>"
        val label: String,    // display label
        val note: String,     // description
        val dynamic: Boolean, // true = downloaded (ksud-classic-latest)
        val version: String = BUNDLED_VERSION,
    )

    fun latestFile(context: Context): File = File(context.filesDir, "ksud-classic-latest")

    private fun markerFile(context: Context): File = File(context.filesDir, "ksud-classic-latest.tag")

    private fun nameFile(context: Context): File = File(context.filesDir, "ksud-classic-latest.name")

    private fun revFile(context: Context): File = File(context.filesDir, "ksud-classic-latest.rev")

    private fun ensureFreshDynamic(context: Context) {
        runCatching {
            val rf = revFile(context)
            val current = if (rf.isFile) rf.readText().trim() else ""
            if (current != KO_PATCH_REV) {
                if (latestFile(context).isFile) {
                    latestFile(context).delete()
                    markerFile(context).delete()
                    nameFile(context).delete()
                    if (selectedId(context) != "bundled-universal") {
                        setSelected(context, "bundled-universal")
                    }
                }
                rf.writeText(KO_PATCH_REV)
            }
        }
    }

    private fun latestName(context: Context): String? =
        nameFile(context).takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }

    private fun latestTag(context: Context): String? =
        markerFile(context).takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }

    fun list(context: Context): List<ClassicProfile> {
        ensureFreshDynamic(context)
        val out = ArrayList<ClassicProfile>()
        val tag = latestTag(context)
        if (tag != null && latestFile(context).isFile) {
            val ver = tag.removePrefix("v").removePrefix("V")
            val name = latestName(context) ?: ver
            out.add(
                ClassicProfile(
                    id = name,
                    label = name,
                    note = "Official ksud $tag + Samsung KDP+DEFEX modules (patched on-device)",
                    dynamic = true,
                    version = ver,
                )
            )
        }
        out.add(
            ClassicProfile(
                id = "bundled-universal",
                label = "$BUNDLED_VERSION · bundled-universal",
                note = "Bundled universal KernelSU Classic binary with Samsung KDP+DEFEX modules",
                dynamic = false,
                version = BUNDLED_VERSION,
            )
        )
        return out
    }

    fun highestVersion(context: Context): String {
        val candidates = list(context).map { it.version }
        return candidates.maxWithOrNull { a, b ->
            val sa = SemVer.parse(a)
            val sb = SemVer.parse(b)
            if (sa != null && sb != null) sa.compareTo(sb) else a.compareTo(b)
        } ?: BUNDLED_VERSION
    }

    fun selectedId(context: Context): String =
        context.getSharedPreferences(PREFS, MODE).getString(PREF_KEY, "") ?: ""

    fun selected(context: Context): ClassicProfile? {
        val items = list(context)
        return items.firstOrNull { it.id == selectedId(context) } ?: items.firstOrNull()
    }

    fun setSelected(context: Context, id: String) {
        context.getSharedPreferences(PREFS, MODE).edit().putString(PREF_KEY, id).apply()
    }

    @JvmStatic
    fun selectedFile(context: Context): File? {
        val p = selected(context) ?: return null
        return if (p.dynamic && latestFile(context).isFile) latestFile(context) else null
    }

    fun selectLatest(context: Context, name: String, note: String) {
        revFile(context).writeText(KO_PATCH_REV)
        nameFile(context).writeText(name)
        setSelected(context, name)
    }

    fun deleteLatest(context: Context): Boolean {
        val f = latestFile(context)
        val existed = f.isFile
        f.delete()
        markerFile(context).delete()
        nameFile(context).delete()
        setSelected(context, "bundled-universal")
        return existed
    }
}
