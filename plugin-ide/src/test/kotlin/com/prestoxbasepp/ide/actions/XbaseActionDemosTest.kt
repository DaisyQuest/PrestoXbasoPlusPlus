package com.prestoxbasepp.ide.actions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class XbaseActionDemosTest {
    @Test
    fun `demo actions define ten unique ids`() {
        val ids = XbaseActionDemos.demoActions.map { it.id.value }

        assertThat(ids).hasSize(10)
        assertThat(ids).doesNotHaveDuplicates()
        assertThat(ids).contains(
            "xbasepp.file.format",
            "xbasepp.project.runTests",
            "xbasepp.editor.quickFixes"
        )
    }

    @Test
    fun `registering demo actions populates registry`() {
        val registry = InMemoryActionRegistry()

        XbaseActionDemos.registerAll(registry)

        assertThat(registry.all()).hasSize(10)
    }

    @Test
    fun `availability respects context for file and selection actions`() {
        val registry = InMemoryActionRegistry()
        XbaseActionDemos.registerAll(registry)

        val empty = ActionContext.empty()
        val withFile = ActionContext.build {
            put(XbaseActionDemos.ActiveFilePath, "/tmp/demo.prg")
        }
        val withSelection = ActionContext.build {
            put(XbaseActionDemos.SelectedText, "ALERT")
        }

        val availableEmpty = registry.available(empty).map { it.id.value }
        val availableFile = registry.available(withFile).map { it.id.value }
        val availableSelection = registry.available(withSelection).map { it.id.value }

        assertThat(availableEmpty).containsExactlyInAnyOrder(
            "xbasepp.project.openDocumentation",
            "xbasepp.editor.toggleLogMarkers"
        )
        assertThat(availableFile).contains(
            "xbasepp.file.format",
            "xbasepp.file.organizeImports"
        )
        assertThat(availableSelection).contains(
            "xbasepp.file.goToDefinition",
            "xbasepp.file.findReferences",
            "xbasepp.editor.quickFixes"
        )
    }

    @Test
    fun `run tests action enforces test environment`() {
        val action = XbaseActionDemos.demoActions.first { it.id.value == "xbasepp.project.runTests" }

        val notTestContext = ActionContext.build {
            put(XbaseActionDemos.ProjectName, "demo")
            put(XbaseActionDemos.IsTestEnvironment, false)
        }
        val testContext = ActionContext.build {
            put(XbaseActionDemos.ProjectName, "demo")
            put(XbaseActionDemos.IsTestEnvironment, true)
        }

        val notTestResult = action.execute(notTestContext)
        val testResult = action.execute(testContext)

        assertThat(notTestResult).isInstanceOf(ActionResult.Failure::class.java)
        assertThat(testResult).isEqualTo(ActionResult.Success("Tests executed"))
    }

    @Test
    fun `profile action requires long run duration`() {
        val action = XbaseActionDemos.demoActions.first { it.id.value == "xbasepp.project.profileRun" }
        val shortContext = ActionContext.build {
            put(XbaseActionDemos.LastRunDuration, 2.seconds)
        }
        val longContext = ActionContext.build {
            put(XbaseActionDemos.LastRunDuration, 12.seconds)
        }

        assertThat(action.isEnabled(shortContext)).isFalse()
        assertThat(action.isEnabled(longContext)).isTrue()
        assertThat(action.execute(longContext)).isEqualTo(ActionResult.Success("Profile started"))
    }

    @Test
    fun `enabled actions respond to presentation metadata`() {
        val action = XbaseActionDemos.demoActions.first { it.id.value == "xbasepp.file.format" }

        assertThat(action.presentation.category).isEqualTo("Editing")
        assertThat(action.presentation.tags).contains("format", "xbasepp")
    }

    @Test
    fun `action keys provide typed access`() {
        val context = ActionContext.build {
            put(XbaseActionDemos.ProjectName, "demo")
            put(XbaseActionDemos.LastRunDuration, Duration.ZERO)
        }

        assertThat(context.get(XbaseActionDemos.ProjectName)).isEqualTo("demo")
        assertThat(context.get(XbaseActionDemos.LastRunDuration)).isEqualTo(Duration.ZERO)
    }
}
