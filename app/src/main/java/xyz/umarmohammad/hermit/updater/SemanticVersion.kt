package xyz.umarmohammad.hermit.updater

/**
 * Strict Semantic Versioning (SemVer 2.0.0) representation and comparator.
 *
 * Compares versions numerically by major, minor, and patch components rather
 * than lexicographical string comparison (e.g. 1.10.0 > 1.9.0).
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val buildMetadata: String? = null
) : Comparable<SemanticVersion> {

    companion object {
        private val SEMVER_REGEX = Regex(
            """^[vV]?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$"""
        )

        /**
         * Parses a version string into a [SemanticVersion].
         * Supports optional leading 'v' or 'V' (e.g. "v1.2.3" -> 1.2.3).
         */
        fun parse(versionStr: String): SemanticVersion? {
            val trimmed = versionStr.trim()
            val match = SEMVER_REGEX.matchEntire(trimmed) ?: return null
            val (major, minor, patch, preRelease, build) = match.destructured

            return SemanticVersion(
                major = major.toIntOrNull() ?: return null,
                minor = minor.toIntOrNull() ?: return null,
                patch = patch.toIntOrNull() ?: return null,
                preRelease = preRelease.ifEmpty { null },
                buildMetadata = build.ifEmpty { null }
            )
        }
    }

    /**
     * Returns true if this version is strictly greater than [other].
     */
    fun isNewerThan(other: SemanticVersion): Boolean {
        return this > other
    }

    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)

        // When major, minor, and patch are equal, a pre-release version has lower precedence
        // than a normal version (e.g. 1.0.0-alpha < 1.0.0).
        if (preRelease == null && other.preRelease != null) return 1
        if (preRelease != null && other.preRelease == null) return -1
        if (preRelease != null && other.preRelease != null) {
            return comparePreRelease(preRelease, other.preRelease)
        }

        // Build metadata does not figure into precedence
        return 0
    }

    private fun comparePreRelease(pre1: String, pre2: String): Int {
        val parts1 = pre1.split(".")
        val parts2 = pre2.split(".")
        val minSize = minOf(parts1.size, parts2.size)

        for (i in 0 until minSize) {
            val p1 = parts1[i]
            val p2 = parts2[i]

            val num1 = p1.toIntOrNull()
            val num2 = p2.toIntOrNull()

            val cmp = when {
                num1 != null && num2 != null -> num1.compareTo(num2)
                num1 != null && num2 == null -> -1 // numeric identifiers have lower precedence than alphanumeric
                num1 == null && num2 != null -> 1
                else -> p1.compareTo(p2) // lexical comparison
            }
            if (cmp != 0) return cmp
        }

        return parts1.size.compareTo(parts2.size)
    }

    override fun toString(): String {
        val base = "$major.$minor.$patch"
        val pre = if (preRelease != null) "-$preRelease" else ""
        val build = if (buildMetadata != null) "+$buildMetadata" else ""
        return "$base$pre$build"
    }
}
