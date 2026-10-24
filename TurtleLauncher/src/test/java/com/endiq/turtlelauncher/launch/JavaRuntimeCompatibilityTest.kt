package com.endiq.turtlelauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JavaRuntimeCompatibilityTest {
    @Test
    fun mapsMinecraftJavaRequirementsAtVersionBoundaries() {
        assertEquals(8, LaunchArgs.resolveRequiredJava("1.16.5", 21))
        // Minecraft 1.17 requires at least Java 16; the launcher runtime tiers include Java 17.
        assertEquals(17, LaunchArgs.resolveRequiredJava("1.17", 8))
        assertEquals(17, LaunchArgs.resolveRequiredJava("1.17.1", 8))
        assertEquals(17, LaunchArgs.resolveRequiredJava("1.20.4", 21))
        assertEquals(21, LaunchArgs.resolveRequiredJava("1.20.5", 17))
        assertEquals(21, LaunchArgs.resolveRequiredJava("1.21.11", 21))
        assertEquals(25, LaunchArgs.resolveRequiredJava("26.1", 21))
        assertEquals(25, LaunchArgs.resolveRequiredJava("26.1-snapshot-1", 21))
        // Early 26.0 snapshots defer to the version metadata until an explicit rule exists.
        assertEquals(21, LaunchArgs.resolveRequiredJava("26.0-snapshot-5", 21))
        // For future year-based branches, trust metadata; do not silently assume Java 25.
        assertEquals(25, LaunchArgs.resolveRequiredJava("27.0", 25))
        assertEquals(0, LaunchArgs.resolveRequiredJava("27.0", 0))
        // Unknown/custom labels must use explicit metadata; do not silently guess Java 8.
        assertEquals(17, LaunchArgs.resolveRequiredJava("custom-fabric-pack", 17))
        assertEquals(0, LaunchArgs.resolveRequiredJava("custom-fabric-pack", 0))
    }

    @Test
    fun rejectsUnknownOrTooOldRuntimeAndAcceptsRequiredOrNewer() {
        assertFalse(JavaRuntimePolicy.isCompatible(0, 25))
        assertFalse(JavaRuntimePolicy.isCompatible(21, 25))
        assertFalse(JavaRuntimePolicy.isCompatible(8, 17))
        assertFalse(JavaRuntimePolicy.isCompatible(25, 0))
        assertTrue(JavaRuntimePolicy.isCompatible(25, 25))
        assertTrue(JavaRuntimePolicy.isCompatible(26, 25))
    }
}
