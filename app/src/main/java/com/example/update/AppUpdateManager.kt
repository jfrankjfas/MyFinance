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
        const val DEFAULT_GITHUB_REPO = "jfrankjfas/MyFinance"
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getRepositoryName(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_GITHUB_REPO, null)
        // Automatically migrate if saved with the old incorrect repository
        if (saved == null || saved == "jfranciscojfas/finanzas-clara" || saved.contains("finanzas-clara")) {
            setRepositoryName(DEFAULT_GITHUB_REPO)
            return DEFAULT_GITHUB_REPO
        }
        return saved
    }

    fun setRepositoryName(repo: String) {
        val clean = repo.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .removeSuffix(".git")
            .removeSuffix("/")
        val finalRepo = if (clean.isBlank()) DEFAULT_GITHUB_REPO else clean
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GITHUB_REPO, finalRepo).apply()
    }

    fun getInstalledVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    suspend fun checkForUpdates(currentVersionName: String = getInstalledVersionName()): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        val currentVer = if (currentVersionName.isBlank()) getInstalledVersionName() else currentVersionName

        // 1. Try checking Firestore app_config/version first (Fast, cloud real-time update)
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val task = firestore.collection("app_config").document("version").get()
            val snap = com.google.android.gms.tasks.Tasks.await(task, 4, TimeUnit.SECONDS)
            if (snap != null && snap.exists()) {
                val ver = snap.getString("latestVersion") ?: ""
                val notes = snap.getString("releaseNotes") ?: "Nuevas mejoras de rendimiento y seguridad."
                val apkUrl = snap.getString("apkDownloadUrl") ?: ""
                val date = snap.getString("releaseDate") ?: ""
                if (ver.isNotBlank()) {
                    val hasUpdate = isNewerVersion(currentVer, ver)
                    return@withContext Result.success(
                        UpdateInfo(
                            hasUpdate = hasUpdate,
                            latestVersion = ver,
                            releaseNotes = notes,
                            apkDownloadUrl = apkUrl.ifBlank { "https://github.com/${getRepositoryName()}/releases/latest/download/app-debug.apk" },
                            releaseDate = date
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Firestore update check skipped/not configured: ${e.message}")
        }

        // 2. Fallback to GitHub Releases API
        val repo = getRepositoryName()
        try {
            val url = "https://api.github.com/repos/$repo/releases/latest"
            Log.d(TAG, "Checking update at GitHub: $url")

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "FinanzasClara-App")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
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

                val hasUpdate = isNewerVersion(currentVer, tagName)

                return@withContext Result.success(
                    UpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = if (tagName.isNotBlank()) tagName else currentVer,
                        releaseNotes = releaseNotes,
                        apkDownloadUrl = downloadUrl,
                        releaseDate = publishedAt
                    )
                )
            } else if (response.code == 404) {
                // Try GitHub Tags if Releases are not yet created
                try {
                    val tagsUrl = "https://api.github.com/repos/$repo/tags"
                    val tagsReq = Request.Builder()
                        .url(tagsUrl)
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "FinanzasClara-App")
                        .build()
                    val tagsResp = httpClient.newCall(tagsReq).execute()
                    if (tagsResp.isSuccessful) {
                        val tagsBody = tagsResp.body?.string() ?: "[]"
                        val tagsArr = org.json.JSONArray(tagsBody)
                        if (tagsArr.length() > 0) {
                            val latestTagObj = tagsArr.getJSONObject(0)
                            val latestTagName = latestTagObj.optString("name", "").removePrefix("v")
                            val hasUpdate = isNewerVersion(currentVer, latestTagName)
                            return@withContext Result.success(
                                UpdateInfo(
                                    hasUpdate = hasUpdate,
                                    latestVersion = latestTagName,
                                    releaseNotes = "Nueva versión etiquetada en GitHub: v$latestTagName",
                                    apkDownloadUrl = "https://github.com/$repo/releases/download/v$latestTagName/app-debug.apk",
                                    releaseDate = ""
                                )
                            )
                        }
                    }
                } catch (eTag: Exception) {
                    Log.d(TAG, "Tags check skipped: ${eTag.message}")
                }

                return@withContext Result.success(
                    UpdateInfo(
                        hasUpdate = false,
                        latestVersion = currentVer,
                        releaseNotes = "Repositorio configurado: $repo. No hay releases ni versiones superiores publicadas.",
                        apkDownloadUrl = "",
                        releaseDate = ""
                    )
                )
            } else {
                return@withContext Result.failure(Exception("GitHub respondió: Código ${response.code}"))
            }
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val resolveInfoList = activity.packageManager.queryIntentActivities(installIntent, 0)
            for (resolveInfo in resolveInfoList) {
                activity.grantUriPermission(
                    resolveInfo.activityInfo.packageName,
                    apkUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            activity.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
        }
    }
}
