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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val updateRepository = UpdateRepository(application)
    private val settingsRepository = SettingsRepository(application)
    
    var updateInfo by mutableStateOf<UpdateInfo?>(null)
        private set
    
    var isChecking by mutableStateOf(false)
        private set

    var showNoUpdateMessage by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            settingsRepository.backgroundUpdateCheck.collect { enabled ->
                if (enabled) {
                    val interval = settingsRepository.updateInterval.first()
                    val network = settingsRepository.networkType.first()
                    UpdateWorker.schedule(getApplication(), interval.hours, network)
                } else {
                    UpdateWorker.cancel(getApplication())
                }
            }
        }
        
        // Listen for interval and network changes too
        viewModelScope.launch {
            settingsRepository.updateInterval.collect { interval ->
                if (settingsRepository.backgroundUpdateCheck.first()) {
                    val network = settingsRepository.networkType.first()
                    UpdateWorker.schedule(getApplication(), interval.hours, network)
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.networkType.collect { network ->
                if (settingsRepository.backgroundUpdateCheck.first()) {
                    val interval = settingsRepository.updateInterval.first()
                    UpdateWorker.schedule(getApplication(), interval.hours, network)
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
        updateInfo = null
    }

    fun startUpdate(context: Context, info: UpdateInfo) {
        val cachedFile = updateRepository.getCachedUpdateFile(info.fileName)
        if (cachedFile != null) {
            Log.d("bit_hub_updater", "[Installer] Found cached APK: ${info.fileName}")
            UpdateInstaller.installApk(context, cachedFile)
            updateInfo = null
            return
        }

        updateRepository.clearOldUpdates()

        val destinationFile = File(context.externalCacheDir, info.fileName)
        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("bit Hub Update ${info.versionName}")
            .setDescription("Загрузка обновления...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destinationFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        
        Log.d("bit_hub_updater", "[Installer] Download started for ${info.versionName}")
        updateInfo = null
    }
}
