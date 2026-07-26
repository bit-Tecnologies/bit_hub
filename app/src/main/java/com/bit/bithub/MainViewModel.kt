package com.bit.bithub

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bit.bithub.data.App
import com.bit.bithub.data.AppRepository
import com.bit.bithub.data.SettingsRepository
import com.bit.bithub.util.isNetworkAvailable
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContainer = application as BitHubApplication
    private val dm = application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val settingsRepository = SettingsRepository(application)
    private val appRepository = AppRepository(appContainer)

    var appsFromCloud by mutableStateOf<List<App>>(emptyList())
        private set
    var isLoading by mutableStateOf(value = true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val installedApps = mutableStateMapOf<String, Int>()
    val appsWithApk = mutableStateListOf<Long>()
    val downloadingProgress = mutableStateMapOf<Long, Float>()
    val downloadIdToAppId = mutableStateMapOf<Long, Long>()

    var favorites by mutableStateOf<Set<String>>(emptySet())
        private set

    val appsWithUpdates: List<App> by derivedStateOf {
        appsFromCloud.filter { app ->
            val pkg = app.packageName ?: return@filter false
            val installedVersion = installedApps[pkg] ?: return@filter false
            app.versionCode > installedVersion
        }
    }

    val categories: List<String> by derivedStateOf {
        appsFromCloud.mapNotNull { it.category }.distinct()
    }

    init {
        loadData()
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            settingsRepository.favorites.collect {
                favorites = it
            }
        }
    }

    fun toggleFavorite(app: App) {
        val id = app.id?.toString() ?: return
        viewModelScope.launch {
            settingsRepository.toggleFavorite(id)
        }
    }

    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            if (!isNetworkAvailable(getApplication())) {
                errorMessage = "Нет интернет-соединения"
                isLoading = false
                return@launch
            }

            try {
                appsFromCloud = appRepository.fetchApps()
                if (appsFromCloud.isEmpty()) {
                    errorMessage = "Приложения не найдены"
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                Log.e("MainViewModel", "Fetch error: $msg")
                errorMessage = when {
                    !isNetworkAvailable(getApplication()) -> "Соединение разорвано"
                    msg.contains("Unable to resolve host", ignoreCase = true) ->
                        "Не удается найти сервер bit Hub. Возможно, база данных отключена."
                    msg.contains("500") || msg.contains("502") || msg.contains("503") ->
                        "Сервис Supabase временно недоступен (Ошибка сервера)"
                    else -> "Ошибка сервера: база данных недоступна"
                }
            } finally {
                isLoading = false
                refreshInstalledApps()
            }
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledPackages(0)
                val newInstalledApps = packages.associate { pkg ->
                    val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pkg.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pkg.versionCode
                    }
                    pkg.packageName to version
                }

                withContext(Dispatchers.Main) {
                    // Обновляем только если есть изменения
                    if (installedApps != newInstalledApps) {
                        installedApps.clear()
                        installedApps.putAll(newInstalledApps)
                        refreshApkStatus()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun refreshApkStatus() {
        viewModelScope.launch(Dispatchers.Default) {
            val currentApkIds = appsFromCloud.mapNotNull { app ->
                if (getApkFile(app.title).exists()) app.id else null
            }

            withContext(Dispatchers.Main) {
                if (appsWithApk.toList() != currentApkIds) {
                    appsWithApk.clear()
                    appsWithApk.addAll(currentApkIds)
                }
            }
        }
    }

    fun getApkFile(name: String): File {
        val downloadDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadDir, "$name.apk")
    }

    fun download(app: App, stateDownloadingText: String) {
        val url = app.downloadUrl ?: ""
        val name = app.title
        if (url.isEmpty()) return

        viewModelScope.launch {
            try {
                val wifiOnly = settingsRepository.appDownloadWifiOnly.first()
                val useMobile = settingsRepository.useMobileData.first()

                val request = DownloadManager.Request(url.toUri())
                    .setTitle(name)
                    .setDescription(stateDownloadingText)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalFilesDir(getApplication(), Environment.DIRECTORY_DOWNLOADS, "$name.apk")
                    .setAllowedOverMetered(useMobile && !wifiOnly)
                    .setAllowedOverRoaming(useMobile && !wifiOnly)
                    .setAllowedNetworkTypes(
                        if (wifiOnly) DownloadManager.Request.NETWORK_WIFI
                        else DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE,
                    )

                val id = dm.enqueue(request)
                app.id?.let { appId ->
                    downloadIdToAppId[id] = appId
                    downloadingProgress[appId] = 0.001f // Совсем чуть-чуть, чтобы показать начало
                }
                observeDownloads()
            } catch (_: Exception) { }
        }
    }

    private var observingDownloads = false
    private fun observeDownloads() {
        if (observingDownloads) return
        observingDownloads = true
        viewModelScope.launch {
            while (downloadIdToAppId.isNotEmpty()) {
                val ids = downloadIdToAppId.keys.toLongArray()
                if (ids.isEmpty()) break

                val query = DownloadManager.Query().setFilterById(*ids)
                val cursor = try { dm.query(query) } catch (_: Exception) { null }

                if ((cursor != null) && cursor.moveToFirst()) {
                    do {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val appId = downloadIdToAppId[id] ?: continue

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                val apkUri = dm.getUriForDownloadedFile(id)
                                if (apkUri != null) {
                                    com.bit.bithub.util.UpdateInstaller.installApk(getApplication(), apkUri)
                                } else {
                                    val file = getApkFile(appsFromCloud.find { it.id == appId }?.title ?: "")
                                    if (file.exists()) {
                                        com.bit.bithub.util.UpdateInstaller.installApk(getApplication(), file)
                                    }
                                }
                                downloadingProgress.remove(appId)
                                downloadIdToAppId.remove(id)
                                refreshApkStatus()
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloadingProgress.remove(appId)
                                downloadIdToAppId.remove(id)
                            }
                            DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                if (total > 0) {
                                    val newProgress = downloaded.toFloat() / total.toFloat()
                                    val currentProgress = downloadingProgress[appId] ?: 0f
                                    // Обновляем только если прогресс реально вырос, чтобы избежать скачков к 0%
                                    if (newProgress > currentProgress) {
                                        downloadingProgress[appId] = newProgress
                                    }
                                }
                            }
                        }
                    } while (cursor.moveToNext())
                    cursor.close()
                }
                delay(200.milliseconds)
            }
            observingDownloads = false
        }
    }

    fun cancelDownload(appId: Long) {
        val downloadId = downloadIdToAppId.entries.find { it.value == appId }?.key
        if (downloadId != null) {
            dm.remove(downloadId)
            downloadIdToAppId.remove(downloadId)
            downloadingProgress.remove(appId)
        }
    }

    fun onPackageInstalled(packageName: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val app = appsFromCloud.find { it.packageName == packageName }
            if (app != null) {
                withContext(Dispatchers.Main) {
                    deleteApk(app)
                }
            }
        }
    }

    fun openApp(context: Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to open app: ${e.message}")
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        val pkg = packageName.trim()
        if (pkg.isEmpty()) return
        
        try {
            Log.d("MainViewModel", "[Uninstall] Try 1: ACTION_DELETE for: $pkg")
            // 1. Самый надежный способ (проверено на Xiaomi пользователя)
            val intent = android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
                data = android.net.Uri.fromParts("package", pkg, null)
                if (context !is android.app.Activity) {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Log.e("MainViewModel", "[Uninstall] ACTION_DELETE failed, trying Try 2 (PackageInstaller): ${e.message}")
            try {
                // 2. Современный официальный API (Session-based)
                val packageInstaller = context.packageManager.packageInstaller
                val statusIntent = android.content.Intent(context, com.bit.bithub.util.UpdateReceiver::class.java).apply {
                    action = "com.bit.bithub.UNINSTALL_COMPLETE"
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context, 
                    0, 
                    statusIntent, 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) android.app.PendingIntent.FLAG_MUTABLE else 0
                )
                packageInstaller.uninstall(pkg, pendingIntent.intentSender)
                
            } catch (e2: Exception) {
                Log.e("MainViewModel", "[Uninstall] PackageInstaller failed, trying Try 3 (Legacy): ${e2.message}")
                try {
                    // 3. Старый добрый способ (крайний случай)
                    @Suppress("DEPRECATION")
                    val intent = android.content.Intent(android.content.Intent.ACTION_UNINSTALL_PACKAGE).apply {
                        data = android.net.Uri.parse("package:$pkg")
                        if (context !is android.app.Activity) {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                    context.startActivity(intent)
                } catch (e3: Exception) {
                    Log.e("MainViewModel", "[Uninstall] All uninstall methods failed: ${e3.message}")
                }
            }
        }
    }

    fun deleteApk(app: App) {
        val file = getApkFile(app.title)
        if (file.exists()) {
            file.delete()
            refreshApkStatus()
        }
    }
}
