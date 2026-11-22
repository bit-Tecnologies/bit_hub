package com.bit.bithub.data

/**
 * GitHub API /releases не гарантирует порядок по дате создания — наблюдалось на практике
 * (v0.0.2.9 оказался раньше v0.0.2.11 и v0.0.2.10 в ответе). Поэтому релиз выбирается
 * по максимальному versionCode из имени APK-ассета, а не по позиции в списке.
 */
object ReleaseSelector {

    private val fileNameRegex = Regex("""bithub-(\d+)-v.*-release\.apk$""")

    fun extractVersionCode(fileName: String): Int? {
        return fileNameRegex.find(fileName)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun pickLatestRelease(releases: List<GitHubRelease>): GitHubRelease? {
        return releases
            .mapNotNull { release ->
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return@mapNotNull null
                val versionCode = extractVersionCode(apkAsset.name) ?: return@mapNotNull null
                release to versionCode
            }
            .maxByOrNull { it.second }
            ?.first
    }
}
