package com.bit.bithub.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.bit.bithub.R
import com.bit.bithub.data.SettingsRepository
import com.bit.bithub.data.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class UpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "com.bit.bithub.worker.UpdateWorker"
        const val UPDATES_CHANNEL_ID = "UPDATES_CHANNEL"
        const val UPDATES_NOTIF_ID = 1001

        fun schedule(context: Context, intervalHours: Long, networkType: com.bit.bithub.data.NetworkType) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (networkType == com.bit.bithub.data.NetworkType.WIFI_ONLY) NetworkType.UNMETERED 
                    else NetworkType.CONNECTED
                )
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdateWorker>(
                intervalHours, TimeUnit.HOURS
            )
            .setConstraints(constraints)
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d("bit_hub_updater", "[UpdateWorker] Scheduled every $intervalHours hours")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d("bit_hub_updater", "[UpdateWorker] Cancelled")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settingsRepository = SettingsRepository(applicationContext)
        val isEnabled = settingsRepository.backgroundUpdateCheck.first()
        
        if (!isEnabled) {
            return@withContext Result.success()
        }

        try {
            val updateRepository = UpdateRepository(applicationContext)
            val includePreReleases = settingsRepository.downloadPreReleases.first()
            val updateInfo = updateRepository.checkUpdate(includePreReleases)

            if (updateInfo != null) {
                sendUpdateNotification(updateInfo.versionName)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("bit_hub_updater", "[UpdateWorker] Failed: ${e.message}")
            Result.retry()
        }
    }

    private fun sendUpdateNotification(versionName: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPDATES_CHANNEL_ID,
                "Обновления bit Hub",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, UPDATES_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Доступно обновление bit Hub")
            .setContentText("Новая версия $versionName готова к загрузке")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(UPDATES_NOTIF_ID, notification)
    }
}
