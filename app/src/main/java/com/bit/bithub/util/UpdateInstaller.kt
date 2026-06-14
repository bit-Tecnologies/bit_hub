package com.bit.bithub.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

object UpdateInstaller {
    private const val TAG = "bit_hub_updater"

    fun installApk(context: Context, file: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            Log.d(TAG, "[Installer] Intent sent for ${file.name}")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "[Installer] Failed to start install intent: ${e.message}")
        }
    }
}
