package com.prestoxbasopp.ide.modules

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class XbModuleDetectionStrategyTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `marker detection uses marker filename and parent`() {
        val marker = tempDir.resolve("project.xpj").also { Files.createFile(it) }
        val strategy = XbModuleMarkerDetectionStrategy(
            markerFinder = FakeMarkerFinder(listOf(marker)),
            namingPolicy = FakeNamingPolicy("ModuleName"),
        )

        val candidates = strategy.detectCandidates(tempDir)

        assertThat(candidates).containsExactly(
            XbModuleCandidate(
                rootPath = tempDir,
                suggestedName = "ModuleName",
                reason = "Detected project.xpj",
            ),
        )
    }

    @Test
    fun `marker detection falls back to base dir when marker has no parent`() {
        val marker = Path.of("project.xpj")
        val strategy = XbModuleMarkerDetectionStrategy(
            markerFinder = FakeMarkerFinder(listOf(marker)),
            namingPolicy = FakeNamingPolicy("ModuleName"),
        )

        val candidates = strategy.detectCandidates(tempDir)

        assertThat(candidates.single().rootPath).isEqualTo(tempDir)
    }

    @Test
    fun `source finder collects unique roots and skips excluded directories`() {
        val srcDir = tempDir.resolve("src").createDirectories()
        val buildDir = tempDir.resolve("build").createDirectories()
        srcDir.resolve("main.PRG").writeText("return")
        srcDir.resolve("other.prg").writeText("return")
        buildDir.resolve("ignored.prg").writeText("return")
        val finder = XbModuleSourceFinder(setOf("prg"))

        val roots = finder.findSourceRoots(tempDir)

        assertThat(roots).containsExactly(srcDir)
    }

    @Test
    fun `source finder returns empty list when walk fails`() {
        val missingDir = tempDir.resolve("missing")
        val finder = XbModuleSourceFinder(setOf("prg"))

        val roots = finder.findSourceRoots(missingDir)

        assertThat(roots).isEmpty()
    }

    private class FakeMarkerFinder(private val markers: List<Path>) : XbModuleMarkerFinder {
        override fun findMarkers(baseDir: Path): List<Path> = markers
    }

    private class FakeNamingPolicy(private val name: String) : XbModuleNamingPolicy {
        override fun suggestName(moduleRoot: Path): String = name
    }
}
