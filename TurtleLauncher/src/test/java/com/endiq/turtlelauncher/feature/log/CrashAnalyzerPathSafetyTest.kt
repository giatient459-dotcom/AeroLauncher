package com.endiq.turtlelauncher.feature.log

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashAnalyzerPathSafetyTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun permitsTargetsInsideGameDirectory() {
        val gameRoot = temporaryFolder.newFolder("TurtleLauncher")
        val target = File(gameRoot, "mods/example.jar")
        assertTrue(target.parentFile!!.mkdirs())
        assertTrue(target.writeText("fixture").let { target.isFile })

        assertTrue(CrashAnalyzer.isSafeRepairTarget(gameRoot, target))
    }

    @Test
    fun rejectsSiblingDirectoryWithSameStringPrefix() {
        val parent = temporaryFolder.root
        val gameRoot = File(parent, "TurtleLauncher").apply { mkdirs() }
        val sibling = File(parent, "TurtleLauncher-old").apply { mkdirs() }
        val target = File(sibling, "important.dat").apply { writeText("keep") }

        assertFalse(CrashAnalyzer.isSafeRepairTarget(gameRoot, target))
        assertTrue("The outside file must remain untouched", target.readText() == "keep")
    }

    @Test
    fun rejectsTheGameRootItselfAndUnrelatedPaths() {
        val parent = temporaryFolder.root
        val gameRoot = File(parent, "TurtleLauncher").apply { mkdirs() }
        val unrelated = File(parent, "Downloads/important.dat").apply {
            parentFile!!.mkdirs()
            writeText("keep")
        }

        assertFalse(CrashAnalyzer.isSafeRepairTarget(gameRoot, gameRoot))
        assertFalse(CrashAnalyzer.isSafeRepairTarget(gameRoot, unrelated))
    }
}
