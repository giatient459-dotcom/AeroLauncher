package com.endiq.turtlelauncher.core.resource

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ShaderInventoryTest {
    private val temporaryFolder = TemporaryFolder().apply { create() }

    @Test
    fun validShaderZipIsListed() = runTest {
        val root = temporaryFolder.newFolder("shaderpacks")
        writeZip(File(root, "good.zip"), "shaders/basic.vsh", "void main() {}")

        val result = ShaderInventory().scan(root)

        assertEquals(listOf("good"), result.map { it.name })
        assertEquals(ResourceType.SHADER_PACK, result.single().type)
    }

    @Test
    fun corruptShaderZipIsIgnored() = runTest {
        val root = temporaryFolder.newFolder("shaderpacks")
        File(root, "broken.zip").writeText("not a zip archive")

        assertTrue(ShaderInventory().scan(root).isEmpty())
    }

    @Test
    fun missingDirectoryReturnsEmptyList() = runTest {
        assertTrue(ShaderInventory().scan(File(temporaryFolder.root, "missing")).isEmpty())
    }

    private fun writeZip(file: File, entryName: String, content: String) {
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(entryName))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
}
