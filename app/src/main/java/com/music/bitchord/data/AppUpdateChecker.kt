package com.music.bitchord.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.music.bitchord.BuildConfig
import com.music.bitchord.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import android.util.Log
import okhttp3.Request
import java.io.File

/**
 * BitChord ships as a sideloaded APK off GitHub Releases rather than through
 * a store, so there's nothing to push an update notice on its own — this
 * polls the repo's "latest release" once per launch and compares its tag
 * against the running build.
 *
 * The update itself is also handled here: the release's `.apk` asset is
 * downloaded into the app's cache and handed to the system package installer,
 * so the whole round trip stays inside the app instead of bouncing out to a
 * browser.
 */
object AppUpdateChecker {

    data class UpdateInfo(
        val version: String,
        val releaseUrl: String,
        val apkUrl: String?,
        /** The release's own Markdown body, shown as this update's "what's new". */
        val notes: String?,
    )

    private const val CACHE_SUBDIR = "updates"

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/MishrajiiCode/VibeWave/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available = _available.asStateFlow()

    /** Where this update's APK download currently stands, for the dialog's progress row. */
    sealed interface DownloadState {
        data object Idle : DownloadState
        data class Downloading(val fraction: Float) : DownloadState
        data class Ready(val file: File) : DownloadState
        data class Failed(val message: String) : DownloadState
    }

    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download = _download.asStateFlow()

    /** Set from the UI thread when the user cancels; polled between network reads. */
    @Volatile
    private var downloadCancelled = false

    suspend fun check(context: Context? = null) = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(LATEST_RELEASE_URL).build()
            val body = Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            } ?: return@runCatching
            val release = json.parseToJsonElement(body) as? JsonObject ?: return@runCatching
            val tag = release["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
            val url = release["html_url"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
            val apkUrl = apkAssetUrl(release)
            val rawNotes = release["body"]?.jsonPrimitive?.contentOrNull
            val latest = tag.removePrefix("v")
            val notes = formatReleaseNotes(rawNotes, latest)
            val current = BuildConfig.VERSION_NAME.removePrefix("v")
            if (isNewer(latest, current)) {
                _available.value = UpdateInfo(latest, url, apkUrl, notes)
                context?.let { showUpdateNotification(it, latest) }
            } else {
                _available.value = null
            }
        }
    }

    fun formatReleaseNotes(rawNotes: String?, version: String): String {
        val trimmed = rawNotes?.trim().orEmpty()
        if (trimmed.isBlank() || trimmed.startsWith("**Full Changelog**") || trimmed.length < 30) {
            return """
                ### 🚀 What's New in VibeWave v$version

                - 💬 **Live Community & WhatsApp-Style 1-on-1 Chat**: Post reviews, rate tracks, join global discussions, and message other users in private real-time chat.
                - ⏱️ **5.5-Second Enhanced Splash Screen**: 7-bar harmonic audio equalizer, smooth loading progress bar, and 4-stage system initialization status readout.
                - 🧠 **Advanced AI Neural Studio**: 13 curated acoustic modes, circadian rhythm harmony alignment, and real-time acoustic metrics breakdown (BPM, danceability, warmth).
                - 🌌 **Cosmic Mesh AI Interface**: Deep multi-stop gradient ambient canvas with glassmorphic cards replacing the plain dark theme.
                - 👑 **Architect Showcase & About Section**: Celebrating Raj Mishra's vision, with deep breakdowns of the Bit-Perfect Lossless Audio Pipeline and zero-API local AI engine.
                - ⚡ **Direct Seamless Installation**: Instant one-tap update download and automatic package installer launch.
            """.trimIndent()
        }
        return rawNotes ?: ""
    }

    fun showUpdateNotification(context: Context, version: String) {
        com.music.bitchord.data.firebase.AnnouncementManager.showRichNotification(
            context = context,
            title = "New VibeWave Update v$version Available!",
            message = "Version $version is ready to install! Tap to view the full changelog and update directly.",
            type = "APP_UPDATE",
            isUpdate = true,
        )
    }

    /**
     * Wipes any APK left over from a previous run. Called once at cold start
     * so a downloaded update is only ever "Install Now" for the session that
     * downloaded it — the next launch starts clean rather than trying to work
     * out whether a leftover file is still good.
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        File(context.cacheDir, CACHE_SUBDIR).listFiles()?.forEach { it.delete() }
    }

    /**
     * Finds the most compatible APK for the user's device architecture (e.g. arm64-v8a, universal).
     */
    private fun apkAssetUrl(release: JsonObject): String? = runCatching {
        val assets = release["assets"]?.jsonArray?.mapNotNull { it as? JsonObject } ?: emptyList()
        val abis = Build.SUPPORTED_ABIS ?: emptyArray()
        val preferredKeyword = when {
            abis.any { it.contains("arm64", ignoreCase = true) } -> "arm64-v8a"
            abis.any { it.contains("v7a", ignoreCase = true) } -> "armeabi-v7a"
            abis.any { it.contains("x86_64", ignoreCase = true) } -> "x86_64"
            else -> "universal"
        }

        val matchedAsset = assets.firstOrNull { asset ->
            val name = asset["name"]?.jsonPrimitive?.contentOrNull ?: ""
            name.endsWith(".apk", ignoreCase = true) &&
                name.contains(preferredKeyword, ignoreCase = true)
        } ?: assets.firstOrNull { asset ->
            val name = asset["name"]?.jsonPrimitive?.contentOrNull ?: ""
            name.endsWith(".apk", ignoreCase = true) &&
                name.contains("universal", ignoreCase = true)
        } ?: assets.firstOrNull { asset ->
            val name = asset["name"]?.jsonPrimitive?.contentOrNull ?: ""
            name.endsWith(".apk", ignoreCase = true)
        }

        matchedAsset?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    /**
     * Streams the current update's APK into the app cache, reporting progress
     * through [download]. Verifies byte completeness and APK package integrity
     * before declaring the download Ready for installation.
     */
    suspend fun downloadApk(context: Context): Unit = withContext(Dispatchers.IO) {
        val info = _available.value ?: return@withContext
        val url = info.apkUrl ?: return@withContext
        downloadCancelled = false
        _download.value = DownloadState.Downloading(0f)

        runCatching {
            val dir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
            // Drop anything left over from an earlier attempt.
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "vibewave-${info.version}.apk")

            val request = Request.Builder().url(url).build()
            Http.client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Download failed: HTTP ${response.code}" }
                val body = response.body ?: error("Empty download body")
                val total = body.contentLength().takeIf { it > 0 }

                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var readTotal = 0L
                        while (true) {
                            if (downloadCancelled) {
                                target.delete()
                                _download.value = DownloadState.Idle
                                return@withContext
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            readTotal += read
                            total?.let {
                                _download.value =
                                    DownloadState.Downloading((readTotal.toFloat() / it).coerceIn(0f, 1f))
                            }
                        }
                        output.flush()
                        if (total != null && total > 0 && readTotal < total) {
                            target.delete()
                            error("Download was interrupted: received $readTotal of $total bytes")
                        }
                    }
                }
            }

            // Verify file integrity: ensure file exists, non-empty, and parses as a valid Android package archive
            if (!target.exists() || target.length() == 0L) {
                error("Downloaded APK file is empty")
            }
            val pkgInfo = context.packageManager.getPackageArchiveInfo(target.absolutePath, 0)
            if (pkgInfo == null) {
                target.delete()
                error("Downloaded package is corrupted or incomplete. Please download again.")
            }

            _download.value = DownloadState.Ready(target)
            // Seamless flow: automatically launch package installer immediately upon download completion
            withContext(Dispatchers.Main) {
                installApk(context, target)
            }
        }.onFailure { error ->
            _download.value = if (downloadCancelled) {
                DownloadState.Idle
            } else {
                DownloadState.Failed(error.message ?: "Download failed")
            }
        }
    }

    /** Stops an in-flight download; the next read loop sees this and bails. */
    fun cancelDownload() {
        downloadCancelled = true
    }

    /** Back to square one after a failure, so the dialog offers Download again. */
    fun resetDownload() {
        _download.value = DownloadState.Idle
    }

    /**
     * Hands a downloaded APK to the system installer using standard ACTION_VIEW intent
     * and explicit URI permissions to guarantee compatibility across all modern Android versions
     * and OEM package managers.
     */
    fun installApk(context: Context, file: File) {
        if (!file.exists() || file.length() == 0L) {
            Log.e("AppUpdateChecker", "Cannot install: APK missing or empty")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }

        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Explicitly grant read URI permission to package installer handlers
            val resolveInfoList = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY,
            )
            for (resolveInfo in resolveInfoList) {
                val pkgName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkgName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdateChecker", "ACTION_VIEW install failed, attempting ACTION_INSTALL_PACKAGE fallback", e)
            runCatching {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val fallback = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
        }
    }

    /** Numeric, dot-separated comparison — "1.10" outranks "1.9". */
    fun isNewer(latest: String, current: String): Boolean {
        val l = latest.removePrefix("v").substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.removePrefix("v").substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val a = l.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
