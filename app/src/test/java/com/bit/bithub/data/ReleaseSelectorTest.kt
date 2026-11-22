package com.bit.bithub.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseSelectorTest {

    private fun releaseWithApk(tag: String, versionCode: Int): GitHubRelease {
        val versionName = tag.removePrefix("v")
        return GitHubRelease(
            tagName = tag,
            assets = listOf(
                GitHubAsset(
                    name = "bithub-$versionCode-v$versionName-release.apk",
                    downloadUrl = "https://example.com/$tag.apk",
                    size = 1L,
                )
            )
        )
    }

    private fun releaseWithoutApk(tag: String): GitHubRelease {
        return GitHubRelease(
            tagName = tag,
            assets = listOf(GitHubAsset(name = "notes.txt", downloadUrl = "https://example.com/notes.txt", size = 1L))
        )
    }

    @Test
    fun `picks highest versionCode regardless of API list order`() {
        // Реальный случай: GitHub отдал v0.0.2.9, v0.0.2.11, v0.0.2.10 в этом порядке,
        // хотя v0.0.2.11 — самый свежий (code 13).
        val releases = listOf(
            releaseWithApk("v0.0.2.9", 11),
            releaseWithApk("v0.0.2.11", 13),
            releaseWithApk("v0.0.2.10", 12),
        )

        val picked = ReleaseSelector.pickLatestRelease(releases)

        assertEquals("v0.0.2.11", picked?.tagName)
    }

    @Test
    fun `empty list returns null`() {
        assertNull(ReleaseSelector.pickLatestRelease(emptyList()))
    }

    @Test
    fun `single release is picked`() {
        val release = releaseWithApk("v1.0.0", 1)

        val picked = ReleaseSelector.pickLatestRelease(listOf(release))

        assertEquals(release, picked)
    }

    @Test
    fun `release without apk asset is skipped`() {
        val withApk = releaseWithApk("v1.0.0", 1)
        val withoutApk = releaseWithoutApk("v2.0.0")

        val picked = ReleaseSelector.pickLatestRelease(listOf(withoutApk, withApk))

        assertEquals("v1.0.0", picked?.tagName)
    }

    @Test
    fun `all releases without apk asset returns null`() {
        val releases = listOf(releaseWithoutApk("v1.0.0"), releaseWithoutApk("v2.0.0"))

        assertNull(ReleaseSelector.pickLatestRelease(releases))
    }

    @Test
    fun `extractVersionCode parses the apk file name format`() {
        assertEquals(12, ReleaseSelector.extractVersionCode("bithub-12-v0.0.2.10-release.apk"))
        assertNull(ReleaseSelector.extractVersionCode("not-an-apk.txt"))
    }
}
