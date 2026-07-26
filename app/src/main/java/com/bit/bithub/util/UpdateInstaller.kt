package com.bit.bithub.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

object UpdateInstaller {
    private const val TAG = "bit_hub_updater"

    fun installApk(context: Context, file: File) {
        if (!file.exists()) {
            Log.e(TAG, "[Installer] File does not exist: ${file.absolutePath}")
            return
        }
        
        if (!checkInstallPermission(context)) {
            requestInstallPermission(context)
            return
        }

        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            
            val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            if (packageInfo != null) {
                params.setAppPackageName(packageInfo.packageName)
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            
            val out = session.openWrite("package_install_session", 0, file.length())
            val `in`: InputStream = FileInputStream(file)
            val buffer = ByteArray(65536)
            var c: Int
            while (`in`.read(buffer).also { c = it } != -1) {
                out.write(buffer, 0, c)
            }
            session.fsync(out)
            `in`.close()
            out.close()

            val intent = Intent(context, UpdateReceiver::class.java).apply {
                action = "com.bit.bithub.INSTALL_COMPLETE"
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, 
                sessionId, 
                intent, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) android.app.PendingIntent.FLAG_MUTABLE else 0
            )
            
            session.commit(pendingIntent.intentSender)
            session.close()
        } catch (e: Exception) {
            Log.e(TAG, "[PackageInstaller] Failed: ${e.message}. Falling back to Intent.")
            installApkLegacy(context, file)
        }
    }

    private fun installApkLegacy(context: Context, file: File) {
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
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "[Installer] Legacy failed: ${e.message}")
        }
    }

    fun installApk(context: Context, uri: Uri) {
        if (!checkInstallPermission(context)) {
            requestInstallPermission(context)
            return
        }

        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            
            val out = session.openWrite("package_install_session", 0, -1)
            val `in`: InputStream? = context.contentResolver.openInputStream(uri)
            if (`in` != null) {
                val buffer = ByteArray(65536)
                var c: Int
                while (`in`.read(buffer).also { c = it } != -1) {
                    out.write(buffer, 0, c)
                }
                session.fsync(out)
                `in`.close()
            }
            out.close()

            val intent = Intent(context, UpdateReceiver::class.java).apply {
                action = "com.bit.bithub.INSTALL_COMPLETE"
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, 
                sessionId, 
                intent, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) android.app.PendingIntent.FLAG_MUTABLE else 0
            )
            
            session.commit(pendingIntent.intentSender)
            session.close()
        } catch (e: Exception) {
            Log.e(TAG, "[PackageInstaller] Failed for URI: ${e.message}. Falling back to Intent.")
            installApkLegacy(context, uri)
        }
    }

    private fun installApkLegacy(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "[Installer] Legacy URI failed: ${e.message}")
        }
    }

    fun getApkVersionCode(context: Context, file: File): Int? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    file.absolutePath,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode?.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo?.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Installer] Failed to get APK version code: ${e.message}")
            null
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
