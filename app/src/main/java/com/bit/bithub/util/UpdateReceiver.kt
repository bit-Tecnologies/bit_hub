package com.bit.bithub.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.bit.bithub.BitHubApplication

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                handleDownloadComplete(context, intent)
            }
            "com.bit.bithub.ACTION_INSTALL_APK" -> {
                val uriString = intent.getStringExtra("apk_uri")
                uriString?.let {
                    Log.d("bit_hub_updater", "[Receiver] Manual install triggered from notification: $it")
                    UpdateInstaller.installApk(context, it.toUri())
                }
            }
            "com.bit.bithub.INSTALL_COMPLETE" -> {
                Log.d("bit_hub_updater", "[Receiver] Installation session finished")
            }
        }
    }

    private fun handleDownloadComplete(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1L) return

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if ((statusIndex != -1) && (cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL)) {
                    val apkUri = downloadManager.getUriForDownloadedFile(downloadId)
                    val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                    val appName = if (titleIndex != -1) cursor.getString(titleIndex) else "Приложение"
                    
                    if (apkUri != null) {
                        Log.d("bit_hub_updater", "[Receiver] Download complete for: $appName")
                        
                        if (BitHubApplication.isAppInForeground) {
                            UpdateInstaller.installApk(context, apkUri)
                        } else {
                            NotificationUtils.showInstallReadyNotification(context, appName, apkUri.toString())
                        }
                    } else {
                        val uriStringIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        if (uriStringIndex != -1) {
                            val uriString = cursor.getString(uriStringIndex)
                            uriString?.let {
                                if (BitHubApplication.isAppInForeground) {
                                    UpdateInstaller.installApk(context, it.toUri())
                                } else {
                                    NotificationUtils.showInstallReadyNotification(context, appName, it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
