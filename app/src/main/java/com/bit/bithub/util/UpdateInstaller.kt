package com.bit.bithub.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

object UpdateInstaller {
    private const val TAG = "bit_hub_updater"

    fun installApk(context: Context, file: File) {
        if (!file.exists()) {
            Log.e(TAG, "[Installer] File does not exist: ${file.absolutePath}")
            return
        }
        
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            installApk(context, contentUri)
        } catch (e: Exception) {
            Log.e(TAG, "[Installer] Failed to get URI for file: ${e.message}")
        }
    }

    fun installApk(context: Context, uri: Uri) {
        if (!checkInstallPermission(context)) {
            requestInstallPermission(context)
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            Log.d(TAG, "[Installer] Intent sent for URI: $uri")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "[Installer] Failed to start install intent: ${e.message}")
        }
    }

    private fun checkInstallPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    private fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
