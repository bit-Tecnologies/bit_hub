package com.bit.bithub.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import java.io.File

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == -1L) return

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            downloadManager.query(query)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if ((statusIndex != -1) && (cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL)) {
                        val uriStringIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        if (uriStringIndex != -1) {
                            val uriString = cursor.getString(uriStringIndex)
                            uriString?.let {
                                val uri = it.toUri()
                                val filePath = uri.path
                                if (filePath != null) {
                                    val file = File(filePath)
                                    if (file.exists() && file.name.endsWith(".apk")) {
                                        Log.d("bit_hub_updater", "[Receiver] Download complete, starting install: ${file.name}")
                                        UpdateInstaller.installApk(context, file)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
