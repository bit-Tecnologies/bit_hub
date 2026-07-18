package com.bit.bithub.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class App(
    val id: Long? = null,
    val title: String = "",
    val developer: String = "",
    val description: String = "",
    @SerialName("icon_url") val iconUrl: String? = null,
    val rating: Double? = null,
    val category: String? = null,
    val platform: List<String>? = null,
    val screenshots: List<String>? = null,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("package_name") val packageName: String? = null,
    @SerialName("app_releases") val appReleases: List<AppRelease>? = null
) {
    val androidRelease: AppRelease?
        get() = appReleases
            ?.filter { it.platform == "android" }
            ?.maxByOrNull { it.versionCode ?: 0 }

    val downloadUrl: String?
        get() = androidRelease?.downloadUrl

    val versionName: String
        get() = androidRelease?.versionName ?: ""

    val versionCode: Int
        get() = androidRelease?.versionCode ?: 0

    val sizeBytes: Long?
        get() = androidRelease?.sizeBytes

    val formattedSize: String
        get() = formatSizeBytes(sizeBytes)
}

@Serializable
data class AppRelease(
    val id: Long? = null,
    @SerialName("app_id") val appId: Long? = null,
    val platform: String? = null,
    @SerialName("version_name") val versionName: String? = null,
    @SerialName("version_code") val versionCode: Int? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    @SerialName("min_os_version") val minOsVersion: String? = null,
    val changelog: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

fun formatSizeBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return ""
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1_048_576 -> "${bytes / 1024} KB"
        bytes < 1_073_741_824 -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
        else -> "${"%.1f".format(bytes / 1_073_741_824.0)} GB"
    }
}

val mockAppsData = listOf(
    App(id = 1, title = "bit Stream", developer = "Stream Inc.", rating = 4.5, description = "Лучшее приложение для стриминга вашего контента.", iconUrl = "https://picsum.photos/id/1/200/200", category = "Развлечения", packageName = "com.bit.stream", appReleases = listOf(AppRelease(id = 1, appId = 1, platform = "android", versionName = "1.2.0", versionCode = 12, downloadUrl = "https://example.com/stream.apk", sizeBytes = 47_185_920))),
    App(id = 2, title = "bit Pixel Art", developer = "Design Studio", rating = 4.8, description = "Рисуйте пиксель-арт с легкостью.", iconUrl = "https://picsum.photos/id/2/200/200", category = "Инструменты", packageName = "com.bit.pixelart", appReleases = listOf(AppRelease(id = 2, appId = 2, platform = "android", versionName = "1.0.1", versionCode = 2, downloadUrl = "https://example.com/pixelart.apk", sizeBytes = 12_582_912))),
    App(id = 3, title = "bit Code Runner", developer = "Dev Tools", rating = 4.2, description = "Компилируйте код прямо на вашем смартфоне.", iconUrl = "https://picsum.photos/id/3/200/200", category = "Инструменты", packageName = "com.bit.coderunner", appReleases = listOf(AppRelease(id = 3, appId = 3, platform = "android", versionName = "2.1.0", versionCode = 21, downloadUrl = "https://example.com/coderunner.apk", sizeBytes = 31_457_280))),
    App(id = 4, title = "bit Notes Plus", developer = "Productivity", rating = 4.7, description = "Умные заметки с поддержкой облачной синхронизации.", iconUrl = "https://picsum.photos/id/4/200/200", category = "Инструменты", packageName = "com.bit.notes", appReleases = listOf(AppRelease(id = 4, appId = 4, platform = "android", versionName = "1.5.4", versionCode = 15, downloadUrl = "https://example.com/notes.apk", sizeBytes = 15_728_640))),
    App(id = 5, title = "bit Monster Hunter", developer = "GameDev Hub", rating = 4.9, description = "Эпическая RPG игра про охоту на монстров.", iconUrl = "https://picsum.photos/id/5/300/200", category = "Игры", packageName = "com.bit.monsterhunter", appReleases = listOf(AppRelease(id = 5, appId = 5, platform = "android", versionName = "1.0.0", versionCode = 1, downloadUrl = "https://example.com/monsterhunter.apk", sizeBytes = 2_147_483_648))),
    App(id = 6, title = "bit Racing Pro", developer = "Speed Games", rating = 4.6, description = "Участвуйте в самых быстрых гонках на планете.", iconUrl = "https://picsum.photos/id/6/300/200", category = "Игры", packageName = "com.bit.racingpro", appReleases = listOf(AppRelease(id = 6, appId = 6, platform = "android", versionName = "3.2.1", versionCode = 32, downloadUrl = "https://example.com/racing.apk", sizeBytes = 1_288_490_188))),
    App(id = 7, title = "bit Space Puzzle", developer = "Logic Games", rating = 4.4, description = "Сложные головоломки в декорациях глубокого космоса.", iconUrl = "https://picsum.photos/id/7/300/200", category = "Игры", packageName = "com.bit.spacepuzzle", appReleases = listOf(AppRelease(id = 7, appId = 7, platform = "android", versionName = "1.1.0", versionCode = 11, downloadUrl = "https://example.com/spacepuzzle.apk", sizeBytes = 157_286_400)))
)
