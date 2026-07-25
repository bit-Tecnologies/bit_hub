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
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BACKGROUND_UPDATE_CHECK = booleanPreferencesKey("background_update_check")
        val UPDATE_INTERVAL = stringPreferencesKey("update_interval")
        val NETWORK_TYPE = stringPreferencesKey("network_type")
        val DOWNLOAD_PRE_RELEASES = booleanPreferencesKey("download_pre_releases")
        val APP_DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("app_download_wifi_only")
        val USE_MOBILE_DATA = booleanPreferencesKey("use_mobile_data")
        val LAST_IGNORED_VERSION = stringPreferencesKey("last_ignored_version")
        val FAVORITES = stringSetPreferencesKey("favorites")
    }

    val favorites: Flow<Set<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[FAVORITES] ?: emptySet() }

    val themeMode: Flow<com.bit.bithub.ui.theme.ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val name = preferences[THEME_MODE] ?: com.bit.bithub.ui.theme.ThemeMode.SYSTEM.name
            try { com.bit.bithub.ui.theme.ThemeMode.valueOf(name) } catch (e: Exception) { com.bit.bithub.ui.theme.ThemeMode.SYSTEM }
        }

    val backgroundUpdateCheck: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[BACKGROUND_UPDATE_CHECK] ?: true }

    val lastIgnoredVersion: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[LAST_IGNORED_VERSION] }

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

    val useMobileData: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[USE_MOBILE_DATA] ?: true }

    suspend fun setThemeMode(mode: com.bit.bithub.ui.theme.ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

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

    suspend fun setUseMobileData(enabled: Boolean) {
        dataStore.edit { it[USE_MOBILE_DATA] = enabled }
    }

    suspend fun setLastIgnoredVersion(version: String) {
        dataStore.edit { it[LAST_IGNORED_VERSION] = version }
    }

    suspend fun toggleFavorite(appId: String) {
        dataStore.edit { preferences ->
            val current = preferences[FAVORITES] ?: emptySet()
            if (current.contains(appId)) {
                preferences[FAVORITES] = current - appId
            } else {
                preferences[FAVORITES] = current + appId
            }
        }
    }
}
