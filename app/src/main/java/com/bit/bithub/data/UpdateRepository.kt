package com.bit.bithub.data

import android.content.Context
import android.util.Log
import com.bit.bithub.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class UpdateRepository(private val context: Context) {
    private val tag = "bit_hub_updater"
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 15000
        }
        install(DefaultRequest) {
            header("User-Agent", "bit-Hub-App")
        }
    }

    suspend fun checkUpdate(includePreReleases: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "[UpdateCheck] Started (includePreReleases: $includePreReleases)")
            
            val url = if (includePreReleases) {
                "https://api.github.com/repos/cybernattor/bit_hub/releases"
            } else {
                "https://api.github.com/repos/cybernattor/bit_hub/releases/latest"
            }
            
            val response: HttpResponse = client.get(url)
            
            if (!response.status.isSuccess()) {
                Log.e(tag, "[UpdateCheck] HTTP Error: ${response.status}")
                return@withContext null
            }

            val release = if (includePreReleases) {
                val releases = response.body<List<GitHubRelease>>()
                // Берем самый свежий релиз (первый в списке от GitHub API)
                releases.firstOrNull()
            } else {
                response.body<GitHubRelease>()
            }

            if (release == null || release.tagName.isEmpty()) {
                Log.e(tag, "[UpdateCheck] Release not found or tag is empty")
                return@withContext null
            }
            
            // Фильтрация: .apk
            val apkAsset = release.assets.find { it.name.endsWith(".apk") }

            if (apkAsset == null) {
                Log.e(tag, "[UpdateCheck] No APK asset found")
                return@withContext null
            }

            val remoteVersionName = parseTagName(release.tagName)
            val remoteVersionCode = extractVersionCode(apkAsset.name)

            Log.d(tag, "[UpdateCheck] Remote: $remoteVersionName (code: $remoteVersionCode), Local: ${BuildConfig.VERSION_NAME} (code: ${BuildConfig.VERSION_CODE})")

            if (isVersionHigher(remoteVersionCode, remoteVersionName)) {
                Log.d(tag, "[UpdateCheck] New version found!")
                return@withContext UpdateInfo(
                    versionName = remoteVersionName,
                    versionCode = remoteVersionCode,
                    changelog = release.body,
                    downloadUrl = apkAsset.downloadUrl,
                    fileName = apkAsset.name,
                )
            } else {
                Log.d(tag, "[UpdateCheck] App is up to date")
            }
        } catch (e: Exception) {
            Log.e(tag, "[UpdateCheck] Exception: ${e.message}")
            e.printStackTrace()
        }
        null
    }

    private fun parseTagName(tagName: String): String {
        return tagName.trim().removePrefix("v").trim()
    }

    private fun extractVersionCode(fileName: String): Int? {
        return try {
            // Формат: bithub-5-v0.0.2.3-release.apk
            val regex = Regex("""bithub-(\d+)-v.*-release\.apk$""")
            regex.find(fileName)?.groupValues?.get(1)?.toInt()
        } catch (_: Exception) {
            null
        }
    }

    private fun isVersionHigher(remoteCode: Int?, remoteName: String): Boolean {
        if (remoteCode != null) {
            if (remoteCode > BuildConfig.VERSION_CODE) return true
            if (remoteCode < BuildConfig.VERSION_CODE) return false
        }
        return compareVersions(remoteName, BuildConfig.VERSION_NAME) > 0
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val clean1 = v1.split("-")[0]
        val clean2 = v2.split("-")[0]
        
        val parts1 = clean1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = clean2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val length = maxOf(parts1.size, parts2.size)
        for (i in 0 until length) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        
        // Если основные части равны, но в одной есть суффикс (alpha/beta), 
        // а в другой нет — версия без суффикса считается выше (релиз)
        if (v1.contains("-") && !v2.contains("-")) return -1
        if (!v1.contains("-") && v2.contains("-")) return 1

        return 0
    }

    fun getCachedUpdateFile(fileName: String): File? {
        val file = File(context.externalCacheDir, fileName)
        return if (file.exists()) file else null
    }

    fun clearOldUpdates() {
        context.externalCacheDir?.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk")) {
                file.delete()
            }
        }
    }
}
