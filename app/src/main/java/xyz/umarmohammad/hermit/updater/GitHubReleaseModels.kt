package xyz.umarmohammad.hermit.updater

import org.json.JSONArray
import org.json.JSONObject

data class GitHubReleaseAsset(
    val id: Long,
    val name: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val contentType: String?
)

data class GitHubRelease(
    val id: Long,
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val publishedAt: String,
    val isDraft: Boolean,
    val isPrerelease: Boolean,
    val assets: List<GitHubReleaseAsset>,
    val parsedVersion: SemanticVersion? = SemanticVersion.parse(tagName)
) {
    /**
     * Finds the primary APK asset for this release.
     * Prefers assets named "Hermit-*.apk" or any file ending in ".apk".
     */
    val apkAsset: GitHubReleaseAsset?
        get() = assets.firstOrNull { it.name.startsWith("Hermit", ignoreCase = true) && it.name.endsWith(".apk", ignoreCase = true) }
            ?: assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

    /**
     * Finds any companion checksum asset (e.g. "SHA256SUMS.txt", "*.sha256", "checksums.txt").
     */
    val checksumAsset: GitHubReleaseAsset?
        get() = assets.firstOrNull {
            it.name.equals("SHA256SUMS.txt", ignoreCase = true) ||
                it.name.equals("checksums.txt", ignoreCase = true) ||
                it.name.endsWith(".sha256", ignoreCase = true)
        }

    companion object {
        fun fromJsonObject(json: JSONObject): GitHubRelease {
            val assetsList = mutableListOf<GitHubReleaseAsset>()
            val assetsJson = json.optJSONArray("assets")
            if (assetsJson != null) {
                for (i in 0 until assetsJson.length()) {
                    val assetObj = assetsJson.optJSONObject(i) ?: continue
                    assetsList.add(
                        GitHubReleaseAsset(
                            id = assetObj.optLong("id"),
                            name = assetObj.optString("name"),
                            sizeBytes = assetObj.optLong("size"),
                            downloadUrl = assetObj.optString("browser_download_url"),
                            contentType = if (assetObj.has("content_type") && !assetObj.isNull("content_type")) assetObj.optString("content_type") else null
                        )
                    )
                }
            }

            val tagName = json.optString("tag_name")

            return GitHubRelease(
                id = json.optLong("id"),
                tagName = tagName,
                name = json.optString("name").ifEmpty { tagName },
                body = json.optString("body"),
                htmlUrl = json.optString("html_url"),
                publishedAt = json.optString("published_at"),
                isDraft = json.optBoolean("draft", false),
                isPrerelease = json.optBoolean("prerelease", false),
                assets = assetsList,
                parsedVersion = SemanticVersion.parse(tagName)
            )
        }

        fun parseReleasesList(jsonString: String): List<GitHubRelease> {
            val list = mutableListOf<GitHubRelease>()
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(fromJsonObject(obj))
            }
            return list
        }

        /**
         * Selects the latest stable release from a list of releases.
         * Filters out drafts and pre-releases, and selects the release with the highest valid SemVer.
         */
        fun selectLatestStableRelease(releases: List<GitHubRelease>): GitHubRelease? {
            return releases
                .asSequence()
                .filter { !it.isDraft && !it.isPrerelease }
                .filter { it.parsedVersion != null }
                .maxByOrNull { it.parsedVersion!! }
        }
    }
}
