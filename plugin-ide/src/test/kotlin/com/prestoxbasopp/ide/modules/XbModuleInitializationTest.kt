package com.prestoxbasopp.ide.modules

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

class XbModuleInitializationTest {
    @Test
    fun `module name resolver appends numeric suffix`() {
        val resolver = XbModuleNameResolver()
        val resolved = resolver.resolve("Core", setOf("Core", "Core 2"))

        assertThat(resolved).isEqualTo("Core 3")
    }

    @Test
    fun `candidate consolidator prefers marker based candidates`() {
        val consolidator = XbModuleCandidateConsolidator()
        val root = Paths.get("module")
        val markerCandidate = XbModuleCandidate(root, "module", "Detected project.xpj")
        val sourceCandidate = XbModuleCandidate(root, "module", "Contains Xbase++ sources")

        val consolidated = consolidator.consolidate(listOf(sourceCandidate, markerCandidate))

        assertThat(consolidated).containsExactly(markerCandidate)
    }

    @Test
    fun `prompt message includes detected modules`() {
        val builder = XbModulePromptMessageBuilder()
        val candidate = XbModuleCandidate(Paths.get("src"), "src", "Contains Xbase++ sources")

        val message = builder.buildMessage(listOf(candidate))

        assertThat(message).contains("XBase++ sources were detected")
        assertThat(message).contains("src")
        assertThat(message).contains("Contains Xbase++ sources")
    }

    @Test
    fun `decider blocks prompt when project already initialized`() {
        val state = FakeState(initialized = true)
        val registry = FakeRegistry(hasModules = false)
        val decider = XbModuleInitializationDecider(state, registry)

        val project = ProjectManager.getInstance().defaultProject
        assertThat(decider.shouldPrompt(project)).isFalse()
    }

    @Test
    fun `initializer respects prompt decision and marks initialized`() {
        val candidate = XbModuleCandidate(Paths.get("src"), "src", "Contains Xbase++ sources")
        val strategy = StaticStrategy(listOf(candidate))
        val discovery = XbModuleDiscoveryService(listOf(strategy), XbModuleCandidateConsolidator())
        val state = FakeState(initialized = false)
        val registry = FakeRegistry(hasModules = false)
        val decider = XbModuleInitializationDecider(state, registry)
        val prompt = FakePrompt(accept = true)
        val creator = FakeCreator()
        val initializer = XbModuleInitializer(
            discovery,
            decider,
            prompt,
            creator,
            state,
            FakePathResolver(Paths.get(".")),
        )

        val project = ProjectManager.getInstance().defaultProject
        val result = initializer.initializeIfNeeded(project)

        assertThat(result.prompted).isTrue()
        assertThat(result.accepted).isTrue()
        assertThat(state.initialized).isTrue()
        assertThat(prompt.prompted).isTrue()
        assertThat(creator.receivedCandidates).containsExactly(candidate)
    }

    private class StaticStrategy(private val candidates: List<XbModuleCandidate>) : XbModuleDetectionStrategy {
        override fun detectCandidates(baseDir: Path): List<XbModuleCandidate> = candidates
    }

    private class FakePrompt(private val accept: Boolean) : XbModuleDetectionPrompt {
        var prompted = false

        override fun confirmInitialization(project: Project, candidates: List<XbModuleCandidate>): Boolean {
            prompted = true
            return accept
        }
    }

    private class FakeCreator : XbModuleCreator {
        var receivedCandidates: List<XbModuleCandidate> = emptyList()

        override fun createModules(project: Project, candidates: List<XbModuleCandidate>) =
            emptyList<com.intellij.openapi.module.Module>().also {
                receivedCandidates = candidates
            }
    }

    private class FakeState(var initialized: Boolean) : XbProjectInitializationState {
        override fun isInitialized(project: Project): Boolean = initialized

        override fun markInitialized(project: Project) {
            initialized = true
        }
    }

    private class FakeRegistry(private val hasModules: Boolean) : XbModuleRegistry {
        override fun hasXbModules(project: Project): Boolean = hasModules
    }

    private class FakePathResolver(private val path: Path) : XbProjectPathResolver {
        override fun resolveBasePath(project: Project): Path? = path
    }

}
