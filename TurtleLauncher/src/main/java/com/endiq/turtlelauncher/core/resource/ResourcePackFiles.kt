package com.endiq.turtlelauncher.core.resource

import java.io.File
import java.util.zip.ZipFile

/**
 * Filesystem/ZIP inspection helpers shared by resource inventories.
 *
 * Corrupt archives and unreadable files are treated as invalid resources, not fatal
 * inventory errors. The JSON validator is intentionally small and strict, avoiding
 * another runtime dependency just to validate pack.mcmeta syntax.
 */
internal object ResourcePackFiles {
    fun listCandidates(directory: File): List<File> {
        if (!directory.isDirectory) return emptyList()
        return directory.listFiles()
            .orEmpty()
            .filter { it.isDirectory || (it.isFile && it.extension.equals("zip", ignoreCase = true)) }
            .sortedBy { it.name.lowercase() }
    }

    fun isValidShaderPack(file: File): Boolean = runCatching {
        if (file.isDirectory) {
            val shaders = File(file, "shaders")
            shaders.isDirectory && shaders.walkTopDown().any {
                it.isFile && (it.extension.equals("fsh", true) ||
                    it.extension.equals("vsh", true) ||
                    it.extension.equals("glsl", true))
            }
        } else if (file.isFile && file.extension.equals("zip", true)) {
            ZipFile(file).use { zip ->
                zip.entries().asSequence().any { entry ->
                    !entry.isDirectory &&
                        entry.name.replace('\\', '/').startsWith("shaders/") &&
                        (entry.name.endsWith(".fsh", true) ||
                            entry.name.endsWith(".vsh", true) ||
                            entry.name.endsWith(".glsl", true))
                }
            }
        } else {
            false
        }
    }.getOrDefault(false)

    fun readResourcePackMetadata(file: File): ResourcePackMetadata? = runCatching {
        val json = when {
            file.isDirectory -> File(file, "pack.mcmeta").takeIf { it.isFile }
                ?.readText(Charsets.UTF_8)
            file.isFile && file.extension.equals("zip", true) ->
                ZipFile(file).use { zip ->
                    val entry = zip.getEntry("pack.mcmeta") ?: return@use null
                    zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            else -> null
        } ?: return null

        JsonObjectReader(json).readPackMetadata()
    }.getOrNull()

    /** A strict JSON reader for the subset needed to validate pack.mcmeta. */
    private class JsonObjectReader(private val source: String) {
        private var index = 0

        fun readPackMetadata(): ResourcePackMetadata? {
            skipWhitespace()
            val root = readObject()
            skipWhitespace()
            if (index != source.length) return null
            val pack = root["pack"] as? Map<*, *> ?: return null
            val rawFormat = pack["pack_format"] as? Number ?: return null
            val format = rawFormat.toInt()
            if (format <= 0 || rawFormat.toDouble() != format.toDouble()) return null
            val description = pack["description"]?.toString()
            return ResourcePackMetadata(format, description)
        }

        private fun readValue(): Any? {
            skipWhitespace()
            if (index >= source.length) error("Unexpected end of JSON")
            return when (source[index]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> readLiteral("true", true)
                'f' -> readLiteral("false", false)
                'n' -> readLiteral("null", null)
                '-', in '0'..'9' -> readNumber()
                else -> error("Invalid JSON value")
            }
        }

        private fun readObject(): Map<String, Any?> {
            expect('{')
            skipWhitespace()
            val result = linkedMapOf<String, Any?>()
            if (consume('}')) return result
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                val value = readValue()
                if (result.containsKey(key)) error("Duplicate object key")
                result[key] = value
                skipWhitespace()
                if (consume('}')) return result
                expect(',')
            }
        }

        private fun readArray(): List<Any?> {
            expect('[')
            skipWhitespace()
            val result = mutableListOf<Any?>()
            if (consume(']')) return result
            while (true) {
                result += readValue()
                skipWhitespace()
                if (consume(']')) return result
                expect(',')
            }
        }

        private fun readString(): String {
            expect('"')
            val out = StringBuilder()
            while (index < source.length) {
                val c = source[index++]
                when {
                    c == '"' -> return out.toString()
                    c == '\\' -> {
                        if (index >= source.length) error("Incomplete escape")
                        when (val escaped = source[index++]) {
                            '"', '\\', '/' -> out.append(escaped)
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                if (index + 4 > source.length) error("Incomplete unicode escape")
                                val code = source.substring(index, index + 4).toIntOrNull(16)
                                    ?: error("Invalid unicode escape")
                                out.append(code.toChar())
                                index += 4
                            }
                            else -> error("Invalid escape")
                        }
                    }
                    c.code < 0x20 -> error("Unescaped control character")
                    else -> out.append(c)
                }
            }
            error("Unterminated string")
        }

        private fun readNumber(): Number {
            val start = index
            consume('-')
            if (!consume('0')) {
                if (index >= source.length || source[index] !in '1'..'9') error("Invalid number")
                while (index < source.length && source[index].isDigit()) index++
            }
            if (consume('.')) {
                if (index >= source.length || !source[index].isDigit()) error("Invalid fraction")
                while (index < source.length && source[index].isDigit()) index++
            }
            if (index < source.length && (source[index] == 'e' || source[index] == 'E')) {
                index++
                if (index < source.length && (source[index] == '+' || source[index] == '-')) index++
                if (index >= source.length || !source[index].isDigit()) error("Invalid exponent")
                while (index < source.length && source[index].isDigit()) index++
            }
            return source.substring(start, index).toDoubleOrNull() ?: error("Invalid number")
        }

        private fun readLiteral(token: String, value: Any?): Any? {
            if (!source.startsWith(token, index)) error("Invalid literal")
            index += token.length
            return value
        }

        private fun skipWhitespace() {
            while (index < source.length && source[index] in charArrayOf(' ', '\n', '\r', '\t')) index++
        }

        private fun expect(char: Char) {
            skipWhitespace()
            if (!consume(char)) error("Expected '$char'")
        }

        private fun consume(char: Char): Boolean {
            if (index < source.length && source[index] == char) {
                index++
                return true
            }
            return false
        }
    }
}
