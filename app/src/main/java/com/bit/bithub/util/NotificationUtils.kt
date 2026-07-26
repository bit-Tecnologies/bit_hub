package com.bit.bithub.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.bit.bithub.BitHubApplication
import com.bit.bithub.MainActivity
import com.bit.bithub.R

object NotificationUtils {

    fun showInstallReadyNotification(context: Context, appName: String, apkUri: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val installIntent = Intent(context, UpdateReceiver::class.java).apply {
            action = "com.bit.bithub.ACTION_INSTALL_APK"
            putExtra("apk_uri", apkUri)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            apkUri.hashCode(),
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BitHubApplication.INSTALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Приложение готово к установке")
            .setContentText("$appName успешно скачано. Нажмите, чтобы установить.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Установить", pendingIntent)
            .build()

        notificationManager.notify(apkUri.hashCode(), notification)
    }
}
