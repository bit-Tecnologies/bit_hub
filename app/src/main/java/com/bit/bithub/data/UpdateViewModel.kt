package com.bit.bithub.data

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bit.bithub.util.UpdateInstaller
import com.bit.bithub.worker.UpdateWorker
import androidx.core.net.toUri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val updateRepository = UpdateRepository(application)
    private val settingsRepository = SettingsRepository(application)
    
    var updateInfo by mutableStateOf<UpdateInfo?>(null)
        private set

    var isUpdatePromptVisible by mutableStateOf(false)
        private set
    
    var isChecking by mutableStateOf(value = false)
        private set

    var showNoUpdateMessage by mutableStateOf(value = false)
        private set

    var downloadProgress by mutableStateOf<Float?>(null)
        private set

    var isUpdateDownloaded by mutableStateOf(false)
        private set

    private var currentDownloadId: Long? = null

    init {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                settingsRepository.backgroundUpdateCheck,
                settingsRepository.updateInterval,
                settingsRepository.networkType
            ) { enabled, interval, network ->
                Triple(enabled, interval, network)
            }.collect { (enabled, interval, network) ->
                if (enabled) {
                    UpdateWorker.schedule(getApplication(), interval.hours, network)
                } else {
                    UpdateWorker.cancel(getApplication())
                }
            }
        }
    }

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            isChecking = true
            val includePreReleases = settingsRepository.downloadPreReleases.first()
            val info = updateRepository.checkUpdate(includePreReleases)
            
            if (info != null) {
                updateInfo = info
                // Проверка, скачано ли уже
                val cachedFile = updateRepository.getCachedUpdateFile(info.fileName)
                if (cachedFile != null) {
                    val fileVersion = UpdateInstaller.getApkVersionCode(getApplication(), cachedFile)
                    isUpdateDownloaded = fileVersion != null && fileVersion >= (info.versionCode ?: 0)
                } else {
                    isUpdateDownloaded = false
                }
                
                if (manual) {
                    isUpdatePromptVisible = true
                } else {
                    val ignored = settingsRepository.lastIgnoredVersion.first()
                    if (ignored != info.versionName) {
                        isUpdatePromptVisible = true
                    }
                }
            } else if (manual) {
                showNoUpdateMessage = true
            }
            isChecking = false
        }
    }

    fun resetNoUpdateMessage() {
        showNoUpdateMessage = false
    }

    fun dismissUpdate() {
        viewModelScope.launch {
            updateInfo?.let {
                settingsRepository.setLastIgnoredVersion(it.versionName)
            }
            isUpdatePromptVisible = false
        }
    }

    fun startUpdate(context: Context, info: UpdateInfo) {
        val cachedFile = updateRepository.getCachedUpdateFile(info.fileName)
        if (cachedFile != null) {
            val fileVersion = UpdateInstaller.getApkVersionCode(context, cachedFile)
            if (fileVersion != null && fileVersion >= (info.versionCode ?: 0)) {
                Log.d("bit_hub_updater", "[Installer] Found valid cached APK: ${info.fileName}")
                UpdateInstaller.installApk(context, cachedFile)
                // Не обнуляем инфо, чтобы показать статус "Установить" если прервано
                isUpdateDownloaded = true
                return
            } else {
                Log.d("bit_hub_updater", "[Installer] Cached APK is invalid or old, deleting: ${info.fileName}")
                cachedFile.delete()
            }
        }

        updateRepository.clearOldUpdates()

        val destinationFile = File(context.externalCacheDir, info.fileName)
        val request = DownloadManager.Request(info.downloadUrl.toUri())
            .setTitle("bit Hub Update ${info.versionName}")
            .setDescription("Загрузка обновления...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destinationFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = downloadManager.enqueue(request)
        currentDownloadId = id
        
        Log.d("bit_hub_updater", "[Installer] Download started for ${info.versionName}")
        observeDownloadProgress(context, id, info)
        isUpdatePromptVisible = false
    }

    private fun observeDownloadProgress(context: Context, downloadId: Long, info: UpdateInfo) {
        viewModelScope.launch {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloadProgress = null
                            downloading = false
                            isUpdateDownloaded = true
                            val destinationFile = File(context.externalCacheDir, info.fileName)
                            UpdateInstaller.installApk(context, destinationFile)
                            currentDownloadId = null
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloadProgress = null
                            downloading = false
                            currentDownloadId = null
                        }
                        DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            if (total > 0) {
                                downloadProgress = downloaded.toFloat() / total.toFloat()
                            }
                        }
                    }
                }
                cursor?.close()
                if (downloading) kotlinx.coroutines.delay(500)
            }
        }
    }
}
