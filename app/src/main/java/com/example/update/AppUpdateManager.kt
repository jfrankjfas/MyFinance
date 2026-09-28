package com.example.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val releaseDate: String
)

class AppUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateManager"
        // Configurable GitHub repo: user can change this in app settings if needed
        private const val PREFS_NAME = "finanzas_clara_prefs"
        private const val KEY_GITHUB_REPO = "github_repo_owner_name"
        const val DEFAULT_GITHUB_REPO = "jfranciscojfas/finanzas-clara"
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getRepositoryName(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GITHUB_REPO, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
    }

    fun setRepositoryName(repo: String) {
        val clean = repo.trim().removePrefix("https://github.com/").removeSuffix("/")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GITHUB_REPO, clean).apply()
    }

    suspend fun checkForUpdates(currentVersionName: String = "1.0"): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val repo = getRepositoryName()
            val url = "https://api.github.com/repos/$repo/releases/latest"
            Log.d(TAG, "Checking update at: $url")

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "FinanzasClara-App")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    return@withContext Result.failure(Exception("Aún no se ha publicado ningún Release en GitHub ($repo). Realiza un push con el workflow activo."))
                }
                return@withContext Result.failure(Exception("Error al consultar GitHub: Código ${response.code}"))
            }

            val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Respuesta vacía de GitHub"))
            val json = JSONObject(bodyString)

            val tagName = json.optString("tag_name", "").removePrefix("v")
            val releaseNotes = json.optString("body", "Mejoras de rendimiento y sincronización.")
            val publishedAt = json.optString("published_at", "").take(10)

            // Look for APK in release assets
            val assets = json.optJSONArray("assets")
            var downloadUrl = ""

            if (assets != null && assets.length() > 0) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            // Fallback to direct asset download URL if none found in array
            if (downloadUrl.isBlank()) {
                downloadUrl = "https://github.com/$repo/releases/latest/download/app-debug.apk"
            }

            val hasUpdate = isNewerVersion(currentVersionName, tagName)

            Result.success(
                UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = if (tagName.isNotBlank()) tagName else "1.0",
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = downloadUrl,
                    releaseDate = publishedAt
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking updates: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        if (latest.isBlank()) return false
        val currParts = current.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(currParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val c = currParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    suspend fun downloadAndInstallApk(
        activity: Activity,
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check if user allowed installation of unknown apps on Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    withContext(Dispatchers.Main) {
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                        activity.startActivity(intent)
                    }
                    return@withContext Result.failure(Exception("Por favor concede el permiso para instalar aplicaciones y vuelve a pulsar Actualizar."))
                }
            }

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "FinanzasClara-App")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Error al descargar archivo APK: Código ${response.code}"))
            }

            val responseBody = response.body ?: return@withContext Result.failure(Exception("Contenido de descarga vacío"))
            val totalBytes = responseBody.contentLength()

            val updatesDir = File(activity.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "update.apk")
            if (apkFile.exists()) apkFile.delete()

            val inputStream = responseBody.byteStream()
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var totalRead: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (totalBytes > 0) {
                    val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            withContext(Dispatchers.Main) {
                onProgress(1f)
                installApk(activity, apkFile)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Download/Install failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun installApk(activity: Activity, apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            activity.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
        }
    }
}
