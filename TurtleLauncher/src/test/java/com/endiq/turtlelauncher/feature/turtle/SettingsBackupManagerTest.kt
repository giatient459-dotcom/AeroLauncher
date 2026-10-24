package com.endiq.turtlelauncher.feature.turtle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupManagerTest {
    @Test
    fun acceptsPersistedSettingRecordShapes() {
        assertTrue(
            SettingsBackupManager.isValidBackupJson(
                """[{"key":"musicEnabled","value":"true"},{"key":"musicVolume","value":"55"},{"key":"optional","value":null}]"""
            )
        )
        // An empty array is structurally valid for a new install with no saved overrides.
        assertTrue(SettingsBackupManager.isValidBackupJson("[]"))
    }

    @Test
    fun rejectsMalformedOrUnexpectedJsonShapes() {
        assertFalse(SettingsBackupManager.isValidBackupJson("{"))
        assertFalse(SettingsBackupManager.isValidBackupJson("""{"key":"musicEnabled","value":"true"}"""))
        assertFalse(SettingsBackupManager.isValidBackupJson("""[null]"""))
        assertFalse(SettingsBackupManager.isValidBackupJson("""[{"value":"true"}]"""))
        assertFalse(SettingsBackupManager.isValidBackupJson("""[{"key":"","value":"true"}]"""))
        assertFalse(SettingsBackupManager.isValidBackupJson("""[{"key":"musicEnabled"}]"""))
        assertFalse(SettingsBackupManager.isValidBackupJson("""[{"key":"musicVolume","value":55}]"""))
        assertFalse(SettingsBackupManager.isValidBackupJson("""[{"key":"musicEnabled","value":"true"},{"key":"musicEnabled","value":"false"}]"""))
    }
}
