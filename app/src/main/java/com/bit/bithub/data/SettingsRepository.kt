package com.bit.bithub.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class UpdateInterval(val hours: Long) {
    SIX_HOURS(6),
    TWENTY_FOUR_HOURS(24)
}

enum class NetworkType {
    ANY,
    WIFI_ONLY
}

class SettingsRepository(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val BACKGROUND_UPDATE_CHECK = booleanPreferencesKey("background_update_check")
        val UPDATE_INTERVAL = stringPreferencesKey("update_interval")
        val NETWORK_TYPE = stringPreferencesKey("network_type")
        val DOWNLOAD_PRE_RELEASES = booleanPreferencesKey("download_pre_releases")
        val APP_DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("app_download_wifi_only")
    }

    val backgroundUpdateCheck: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[BACKGROUND_UPDATE_CHECK] ?: true }

    val updateInterval: Flow<UpdateInterval> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val name = preferences[UPDATE_INTERVAL] ?: UpdateInterval.TWENTY_FOUR_HOURS.name
            try { UpdateInterval.valueOf(name) } catch (e: Exception) { UpdateInterval.TWENTY_FOUR_HOURS }
        }

    val networkType: Flow<NetworkType> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val name = preferences[NETWORK_TYPE] ?: NetworkType.WIFI_ONLY.name
            try { NetworkType.valueOf(name) } catch (e: Exception) { NetworkType.WIFI_ONLY }
        }

    val downloadPreReleases: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[DOWNLOAD_PRE_RELEASES] ?: false }

    val appDownloadWifiOnly: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[APP_DOWNLOAD_WIFI_ONLY] ?: false }

    suspend fun setBackgroundUpdateCheck(enabled: Boolean) {
        dataStore.edit { it[BACKGROUND_UPDATE_CHECK] = enabled }
    }

    suspend fun setUpdateInterval(interval: UpdateInterval) {
        dataStore.edit { it[UPDATE_INTERVAL] = interval.name }
    }

    suspend fun setNetworkType(type: NetworkType) {
        dataStore.edit { it[NETWORK_TYPE] = type.name }
    }

    suspend fun setDownloadPreReleases(enabled: Boolean) {
        dataStore.edit { it[DOWNLOAD_PRE_RELEASES] = enabled }
    }

    suspend fun setAppDownloadWifiOnly(enabled: Boolean) {
        dataStore.edit { it[APP_DOWNLOAD_WIFI_ONLY] = enabled }
    }
}
