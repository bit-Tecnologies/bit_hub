package com.bit.bithub.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val body: String = "",
    val assets: List<GitHubAsset> = emptyList(),
    val id: Long = -1L,
    val prerelease: Boolean = false
)

@Serializable
data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0L
)

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int?,
    val changelog: String,
    val downloadUrl: String,
    val fileName: String
)
