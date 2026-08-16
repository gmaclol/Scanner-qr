package com.apple.quickscan

import android.app.Activity
import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class MyApplication : Application(), Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null

    private val onDownloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                val prefs = getSharedPreferences("quickscan_updates", Context.MODE_PRIVATE)
                val pendingId = prefs.getLong("pending_download_id", -2L)
                val apkPath = prefs.getString("pending_apk_path", null)

                if (downloadId == pendingId && apkPath != null) {
                    val file = File(apkPath)
                    if (file.exists()) {
                        prefs.edit().remove("pending_download_id").apply()
                        Handler(Looper.getMainLooper()).post {
                            val activity = currentActivity
                            if (activity != null && !activity.isFinishing) {
                                MaterialAlertDialogBuilder(activity)
                                    .setTitle("Aggiornamento Scaricato 🎉")
                                    .setMessage("Il nuovo aggiornamento di QuickScan è pronto per l'installazione.")
                                    .setPositiveButton("Installa Ora") { _, _ ->
                                        UpdateManager(activity).installApk(file)
                                    }
                                    .setNegativeButton("Dopo", null)
                                    .show()
                            } else {
                                UpdateManager(this@MyApplication).installApk(file)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(
                this,
                onDownloadCompleteReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(onDownloadCompleteReceiver, filter)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
