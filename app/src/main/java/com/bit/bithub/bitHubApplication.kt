package com.bit.bithub

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.annotations.SupabaseInternal
import io.ktor.client.plugins.HttpTimeout

class BitHubApplication : Application() {
    
    lateinit var supabase: SupabaseClient

    override fun onCreate() {
        super.onCreate()
        initSupabase()
        createNotificationChannels()
    }

    @OptIn(SupabaseInternal::class)
    private fun initSupabase() {
        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY,
        ) {
            install(Postgrest)
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 10000
                    connectTimeoutMillis = 5000
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            
            val installChannel = NotificationChannel(
                INSTALL_CHANNEL_ID,
                "Установка приложений",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            
            val updatesChannel = NotificationChannel(
                com.bit.bithub.worker.UpdateWorker.UPDATES_CHANNEL_ID,
                "Обновления bit Hub",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о доступных обновлениях bit Hub"
            }

            notificationManager.createNotificationChannels(listOf(installChannel, updatesChannel))
        }
    }

    companion object {
        const val INSTALL_CHANNEL_ID = "INSTALL_CHANNEL"
    }
}
