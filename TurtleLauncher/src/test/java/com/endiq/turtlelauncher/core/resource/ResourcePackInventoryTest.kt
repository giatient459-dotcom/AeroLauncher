package com.endiq.turtlelauncher.core.resource

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ResourcePackInventoryTest {
    private val temporaryFolder = TemporaryFolder().apply { create() }

    @Test
    fun validPackMcmetaIsListed() = runTest {
        val root = temporaryFolder.newFolder("resourcepacks")
        val pack = File(root, "VanillaPlus")
        assertTrue(pack.mkdirs())
        File(pack, "pack.mcmeta").writeText(
            """{"pack":{"pack_format":15,"description":"test pack"}}"""
        )

        val result = ResourcePackInventory().scan(root)

        assertEquals(listOf("VanillaPlus"), result.map { it.name })
        assertEquals(ResourceType.RESOURCE_PACK, result.single().type)
    }

    @Test
    fun malformedJsonIsIgnored() = runTest {
        val root = temporaryFolder.newFolder("resourcepacks")
        val pack = File(root, "broken")
        assertTrue(pack.mkdirs())
        File(pack, "pack.mcmeta").writeText("""{"pack":""")

        assertTrue(ResourcePackInventory().scan(root).isEmpty())
    }

    @Test
    fun missingDirectoryReturnsEmptyList() = runTest {
        assertTrue(ResourcePackInventory().scan(File(temporaryFolder.root, "missing")).isEmpty())
    }

    @Test
    fun validPackMcmetaInsideZipIsListed() = runTest {
        val root = temporaryFolder.newFolder("resourcepacks")
        val archive = File(root, "pack.zip")
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("pack.mcmeta"))
            zip.write("""{"pack":{"pack_format":15,"description":"zip pack"}}""".toByteArray())
            zip.closeEntry()
        }

        assertEquals(listOf("pack"), ResourcePackInventory().scan(root).map { it.name })
    }
}
