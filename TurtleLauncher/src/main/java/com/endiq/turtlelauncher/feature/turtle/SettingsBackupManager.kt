package com.endiq.turtlelauncher.feature.turtle

import com.endiq.turtlelauncher.feature.log.Logging
import com.endiq.turtlelauncher.setting.Settings
import com.endiq.turtlelauncher.utils.path.PathManager
import net.endiq.launcher.Tools
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * AeroLauncher settings backup/restore.
 *
 * Restore validates every record before touching the live settings file and stages the
 * replacement beside it, so malformed backups or a failed write cannot truncate the
 * user's current settings. Settings.refreshSettings() then applies the restored values.
 */
object SettingsBackupManager {
    private const val TAG = "SettingsBackupManager"

    /** Returns the current settings file's content, or null if it doesn't exist / isn't valid JSON. */
    @JvmStatic
    fun exportToJson(): String? {
        return runCatching {
            val file = PathManager.FILE_SETTINGS
            if (!file.exists()) return null
            val text = Tools.read(file)
            // Do not create backups that the restore path itself would reject.
            check(isValidBackupJson(text)) { "Settings file has an invalid record structure" }
            text
        }.onFailure { e -> Logging.e(TAG, "Settings export failed", e) }.getOrNull()
    }

    @JvmStatic
    fun exportToFile(destFile: File): Boolean {
        val json = exportToJson() ?: return false
        return runCatching {
            destFile.parentFile?.mkdirs()
            destFile.writeText(json)
            true
        }.onFailure { e -> Logging.e(TAG, "Settings export write failed", e) }.getOrDefault(false)
    }

    /**
     * Checks the actual persisted SettingAttribute shape: a JSON array of objects with a
     * non-blank string key and a string-or-null value. Duplicate keys are rejected because
     * Settings.refreshSettings() resolves duplicates by taking the last record silently.
     */
    @JvmStatic
    fun isValidBackupJson(json: String): Boolean {
        return runCatching {
            val parsed = com.google.gson.JsonParser.parseString(json)
            if (!parsed.isJsonArray) {
                false
            } else {
                val keys = HashSet<String>()
                parsed.asJsonArray.all { element ->
                    if (!element.isJsonObject) return@all false
                    val record = element.asJsonObject
                    val keyElement = record.get("key")
                    if (keyElement == null || !keyElement.isJsonPrimitive ||
                        !keyElement.asJsonPrimitive.isString
                    ) return@all false

                    val key = keyElement.asString
                    if (key.isBlank() || !keys.add(key)) return@all false

                    val value = record.get("value") ?: return@all false
                    value.isJsonNull || (value.isJsonPrimitive && value.asJsonPrimitive.isString)
                }
            }
        }.getOrDefault(false)
    }

    @JvmStatic
    fun importFromJson(json: String): Boolean {
        if (!isValidBackupJson(json)) {
            Logging.w(TAG, "Settings restore rejected: backup has an invalid record structure")
            return false
        }

        val target = PathManager.FILE_SETTINGS
        val parent = target.parentFile ?: return false
        return runCatching {
            if (!parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) {
                throw java.io.IOException("Unable to create settings directory")
            }

            // Same-directory rename avoids exposing a partially written settings file.
            val staged = File.createTempFile("settings-import-", ".json", parent)
            try {
                staged.writeText(json, Charsets.UTF_8)
                Files.move(
                    staged.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } finally {
                staged.delete()
            }

            Settings.refreshSettings()
            true
        }.onFailure { e -> Logging.e(TAG, "Settings restore failed; existing file was preserved when replacement did not complete", e) }
            .getOrDefault(false)
    }

    @JvmStatic
    fun importFromFile(srcFile: File): Boolean {
        if (!srcFile.isFile) return false
        return runCatching { importFromJson(srcFile.readText(Charsets.UTF_8)) }
            .onFailure { e -> Logging.e(TAG, "Settings backup read failed", e) }
            .getOrDefault(false)
    }
}
