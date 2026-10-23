package xyz.umarmohammad.hermit.updater

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseSelectionTest {

    @Test
    fun `selectLatestStableRelease ignores drafts and prereleases`() {
        val releases = listOf(
            createRelease(tag = "v2.0.0-beta.1", isDraft = false, isPrerelease = true),
            createRelease(tag = "v1.5.0", isDraft = true, isPrerelease = false),
            createRelease(tag = "v1.2.0", isDraft = false, isPrerelease = false),
            createRelease(tag = "v1.1.0", isDraft = false, isPrerelease = false)
        )

        val selected = GitHubRelease.selectLatestStableRelease(releases)
        assertNotNull(selected)
        assertEquals("v1.2.0", selected!!.tagName)
    }

    @Test
    fun `selectLatestStableRelease selects highest semver regardless of list order`() {
        val releases = listOf(
            createRelease(tag = "v1.0.5", isDraft = false, isPrerelease = false),
            createRelease(tag = "v1.10.0", isDraft = false, isPrerelease = false),
            createRelease(tag = "v1.2.0", isDraft = false, isPrerelease = false)
        )

        val selected = GitHubRelease.selectLatestStableRelease(releases)
        assertNotNull(selected)
        assertEquals("v1.10.0", selected!!.tagName)
    }

    @Test
    fun `finds primary apk and checksum assets correctly`() {
        val release = GitHubRelease(
            id = 1,
            tagName = "v1.0.0",
            name = "Hermit 1.0.0",
            body = "Release notes",
            htmlUrl = "https://github.com/mr-umar/Hermit/releases/tag/v1.0.0",
            publishedAt = "2026-10-23T00:00:00Z",
            isDraft = false,
            isPrerelease = false,
            assets = listOf(
                GitHubReleaseAsset(1, "Hermit-v1.0.0.apk", 15000000, "https://github.com/.../Hermit.apk", "application/vnd.android.package-archive"),
                GitHubReleaseAsset(2, "SHA256SUMS.txt", 128, "https://github.com/.../SHA256SUMS.txt", "text/plain"),
                GitHubReleaseAsset(3, "source.tar.gz", 200000, "https://github.com/.../source.tar.gz", "application/gzip")
            )
        )

        assertNotNull(release.apkAsset)
        assertEquals("Hermit-v1.0.0.apk", release.apkAsset!!.name)

        assertNotNull(release.checksumAsset)
        assertEquals("SHA256SUMS.txt", release.checksumAsset!!.name)
    }

    @Test
    fun `empty releases list returns null`() {
        assertNull(GitHubRelease.selectLatestStableRelease(emptyList()))
    }

    private fun createRelease(tag: String, isDraft: Boolean, isPrerelease: Boolean): GitHubRelease {
        return GitHubRelease(
            id = tag.hashCode().toLong(),
            tagName = tag,
            name = "Release $tag",
            body = "Notes for $tag",
            htmlUrl = "https://github.com/mr-umar/Hermit/releases/tag/$tag",
            publishedAt = "2026-10-23T00:00:00Z",
            isDraft = isDraft,
            isPrerelease = isPrerelease,
            assets = emptyList()
        )
    }
}
