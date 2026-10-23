package xyz.umarmohammad.hermit.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticVersionTest {

    @Test
    fun `parse valid standard semver`() {
        val version = SemanticVersion.parse("1.2.3")
        assertNotNull(version)
        assertEquals(1, version!!.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
        assertNull(version.preRelease)
        assertNull(version.buildMetadata)
    }

    @Test
    fun `parse with leading v prefix`() {
        val version = SemanticVersion.parse("v2.10.4")
        assertNotNull(version)
        assertEquals(2, version!!.major)
        assertEquals(10, version.minor)
        assertEquals(4, version.patch)
    }

    @Test
    fun `parse with pre-release and build metadata`() {
        val version = SemanticVersion.parse("v1.0.0-rc.1+build.123")
        assertNotNull(version)
        assertEquals("rc.1", version!!.preRelease)
        assertEquals("build.123", version.buildMetadata)
    }

    @Test
    fun `invalid semver returns null`() {
        assertNull(SemanticVersion.parse("invalid"))
        assertNull(SemanticVersion.parse("1.0"))
        assertNull(SemanticVersion.parse("1.a.3"))
        assertNull(SemanticVersion.parse(""))
    }

    @Test
    fun `numerical comparison overcomes lexicographical flaw`() {
        val v1_10_0 = SemanticVersion.parse("1.10.0")!!
        val v1_9_0 = SemanticVersion.parse("1.9.0")!!

        // In lexicographical comparison "1.10.0" < "1.9.0", but in SemVer 1.10.0 > 1.9.0
        assertTrue("1.10.0 must be greater than 1.9.0", v1_10_0 > v1_9_0)
        assertTrue(v1_10_0.isNewerThan(v1_9_0))
        assertFalse(v1_9_0.isNewerThan(v1_10_0))
    }

    @Test
    fun `patch comparison works properly`() {
        val v1_0_1 = SemanticVersion.parse("v1.0.1")!!
        val v1_0_0 = SemanticVersion.parse("v1.0.0")!!

        assertTrue(v1_0_1 > v1_0_0)
        assertFalse(v1_0_0 > v1_0_1)
    }

    @Test
    fun `pre-release has lower precedence than normal release`() {
        val release = SemanticVersion.parse("1.0.0")!!
        val rc = SemanticVersion.parse("1.0.0-rc.1")!!

        assertTrue("Normal release has higher precedence than pre-release", release > rc)
        assertFalse(rc > release)
    }

    @Test
    fun `build metadata is ignored in precedence`() {
        val v1 = SemanticVersion.parse("1.0.0+20261023")!!
        val v2 = SemanticVersion.parse("1.0.0+20261024")!!

        assertEquals(0, v1.compareTo(v2))
    }
}
