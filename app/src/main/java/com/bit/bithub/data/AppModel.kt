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

