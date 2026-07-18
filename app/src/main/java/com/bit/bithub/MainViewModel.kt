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
import com.bit.bithub.data.SettingsRepository
import com.bit.bithub.util.isNetworkAvailable
import com.bit.bithub.settings.SettingsManager
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
    private val appContainer = application as? BitHubApplication
    private val dm = application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val settingsRepository = SettingsRepository(application)

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

    val appsWithUpdates: List<App> by derivedStateOf {
        appsFromCloud.filter { app ->
            val pkg = app.packageName ?: return@filter false
            val installedVersion = installedApps[pkg] ?: return@filter false
            app.versionCode > installedVersion
        }
    }

    init {
        loadData()
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
                val supabase = appContainer?.supabase
                if (supabase != null) {
                    val results = supabase.from("apps").select(
                        columns = Columns.list("*", "app_releases(*)")
                    ).decodeList<App>()
                    appsFromCloud = results
                    if (results.isEmpty()) {
                        errorMessage = "Приложения не найдены"
                    }
                } else {
                    errorMessage = "Ошибка конфигурации"
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val internetOk = isNetworkAvailable(getApplication())

                if (!internetOk) {
                    errorMessage = "Соединение разорвано"
                } else {
                    errorMessage = when {
                        msg.contains("Unable to resolve host", ignoreCase = true) ->
                            "Не удается найти сервер bit Hub. Возможно, база данных отключена."
                        msg.contains("500") || msg.contains("502") || msg.contains("503") ->
                            "Сервис Supabase временно недоступен (Ошибка сервера)"
                        msg.contains("timeout", ignoreCase = true) ->
                            "Время ожидания истекло. Медленный ответ от сервера."
                        else -> "Ошибка сервера: база данных недоступна"
                    }
                    Log.e("MainViewModel", "Supabase error: $msg")
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
                val newInstalledApps = mutableMapOf<String, Int>()

                for (pkg in packages) {
                    val versionNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pkg.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pkg.versionCode
                    }
                    newInstalledApps[pkg.packageName] = versionNumber
                }

                withContext(Dispatchers.Main) {
                    installedApps.clear()
                    installedApps.putAll(newInstalledApps)
                    refreshApkStatus()
                }
            } catch (_: Exception) { }
        }
    }

    private fun refreshApkStatus() {
        viewModelScope.launch(Dispatchers.Default) {
            val statusUpdates = mutableListOf<Pair<Long, Boolean>>()
            appsFromCloud.forEach { app ->
                val file = getApkFile(app.title)
                app.id?.let { id ->
                    statusUpdates.add(id to file.exists())
                }
            }

            withContext(Dispatchers.Main) {
                statusUpdates.forEach { (id, exists) ->
                    if (exists && !appsWithApk.contains(id)) appsWithApk.add(id)
                    else if (!exists && appsWithApk.contains(id)) appsWithApk.remove(id)
                }
            }
        }
    }

    fun getApkFile(name: String): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "$name.apk")
    }

    fun download(app: App, stateDownloadingText: String) {
        val url = app.downloadUrl ?: ""
        val name = app.title
        if (url.isEmpty()) return

        viewModelScope.launch {
            try {
                val wifiOnly = settingsRepository.appDownloadWifiOnly.first()
                val useMobile = SettingsManager.useMobileData

                val request = DownloadManager.Request(url.toUri())
                    .setTitle(name)
                    .setDescription(stateDownloadingText)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$name.apk")
                    .setAllowedOverMetered(useMobile && !wifiOnly)
                    .setAllowedOverRoaming(useMobile && !wifiOnly)
                    .setAllowedNetworkTypes(
                        if (wifiOnly) DownloadManager.Request.NETWORK_WIFI
                        else DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE,
                    )

                val id = dm.enqueue(request)
                app.id?.let { appId ->
                    downloadIdToAppId[id] = appId
                    downloadingProgress[appId] = 0.01f
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
                                    downloadingProgress[appId] = downloaded.toFloat() / total.toFloat()
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
}
