package com.apple.quickscan

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    val tag_name: String,
    val body: String?,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val browser_download_url: String,
    val name: String
)

class UpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val GITHUB_OWNER = "gmaclol"
    private val GITHUB_REPO = "Scanner-qr"

    suspend fun checkForUpdates(
        onUpdateAvailable: (versionName: String, downloadUrl: String, releaseNotes: String) -> Unit,
        onNoUpdate: (() -> Unit)? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "QuickScan-Android")
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val release = gson.fromJson(body, GitHubRelease::class.java)
                        val onlineVersionCode = extractVersionCode(release.tag_name, release.body ?: "")

                        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            context.packageManager.getPackageInfo(context.packageName, 0)
                        }
                        val currentVersionCode = PackageInfoCompat.getLongVersionCode(pInfo).toInt()

                        if (onlineVersionCode > currentVersionCode) {
                            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                            if (apkAsset != null) {
                                withContext(Dispatchers.Main) {
                                    onUpdateAvailable(release.tag_name, apkAsset.browser_download_url, release.body ?: "")
                                }
                                return@withContext
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    onNoUpdate?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onNoUpdate?.invoke()
                }
            }
        }
    }

    private fun extractVersionCode(tagName: String, body: String): Int {
        val regex = "versionCode:\\s*(\\d+)".toRegex()
        val match = regex.find(body)
        if (match != null) return match.groupValues[1].toInt()

        return tagName.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
    }

    fun downloadAndInstall(downloadUrl: String, onDownloadStarted: (() -> Unit)? = null) {
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "quickscan_update.apk")
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(downloadUrl.toUri())
            .setTitle(context.getString(R.string.app_name))
            .setDescription("Scaricamento aggiornamento QuickScan...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val prefs = context.getSharedPreferences("quickscan_updates", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("pending_download_id", downloadId)
            .putString("pending_apk_path", destination.absolutePath)
            .apply()

        onDownloadStarted?.invoke()
        Toast.makeText(context, "Scaricamento aggiornamento avviato...", Toast.LENGTH_SHORT).show()
    }

    fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
