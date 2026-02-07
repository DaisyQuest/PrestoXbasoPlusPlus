package com.prestoxbasepp.ide.actions

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object XbaseActionDemos {
    val ActiveFilePath = ActionKey.of<String>("activeFilePath")
    val SelectedText = ActionKey.of<String>("selectedText")
    val ProjectName = ActionKey.of<String>("projectName")
    val IsTestEnvironment = ActionKey.of<Boolean>("isTestEnvironment")
    val LastRunDuration = ActionKey.of<Duration>("lastRunDuration")

    val demoActions: List<ActionDefinition> = listOf(
        xbaseAction("xbasepp.file.format") {
            presentation {
                text = "Format XBase++ File"
                description = "Format the currently active XBase++ file"
                category = "Editing"
                tag("format")
                tag("xbasepp")
            }
            enabledWhen(ActionCondition { it.get(ActiveFilePath) != null })
            handler(ActionHandler { ActionResult.Success("Formatted") })
        },
        xbaseAction("xbasepp.file.organizeImports") {
            presentation {
                text = "Organize Imports"
                description = "Reorder and clean import statements"
                category = "Editing"
                tag("imports")
            }
            enabledWhen(ActionCondition { it.get(ActiveFilePath) != null })
            handler(ActionHandler { ActionResult.Success("Imports organized") })
        },
        xbaseAction("xbasepp.file.goToDefinition") {
            presentation {
                text = "Go To Definition"
                description = "Navigate to the symbol definition"
                category = "Navigation"
                tag("navigation")
            }
            enabledWhen(ActionCondition { it.get(SelectedText)?.isNotBlank() == true })
            handler(ActionHandler { ActionResult.Success("Navigated") })
        },
        xbaseAction("xbasepp.file.findReferences") {
            presentation {
                text = "Find References"
                description = "Find references to the selected symbol"
                category = "Navigation"
                tag("references")
            }
            enabledWhen(ActionCondition { it.get(SelectedText)?.isNotBlank() == true })
            handler(ActionHandler { ActionResult.Success("References listed") })
        },
        xbaseAction("xbasepp.project.build") {
            presentation {
                text = "Build Project"
                description = "Compile the current project"
                category = "Build"
                tag("build")
            }
            enabledWhen(ActionCondition { it.get(ProjectName)?.isNotBlank() == true })
            handler(ActionHandler { ActionResult.Success("Build completed") })
        },
        xbaseAction("xbasepp.project.runTests") {
            presentation {
                text = "Run Tests"
                description = "Execute project tests"
                category = "Testing"
                tag("tests")
            }
            enabledWhen(ActionCondition { it.get(ProjectName)?.isNotBlank() == true })
            handler(ActionHandler { context ->
                if (context.get(IsTestEnvironment) == true) {
                    ActionResult.Success("Tests executed")
                } else {
                    ActionResult.Failure("Not in test environment")
                }
            })
        },
        xbaseAction("xbasepp.project.openDocumentation") {
            presentation {
                text = "Open Documentation"
                description = "Open XBase++ documentation"
                category = "Help"
                tag("docs")
            }
            handler(ActionHandler { ActionResult.Success("Documentation opened") })
        },
        xbaseAction("xbasepp.project.profileRun") {
            presentation {
                text = "Profile Last Run"
                description = "Profile the last execution"
                category = "Performance"
                tag("profile")
            }
            enabledWhen(ActionCondition { (it.get(LastRunDuration) ?: Duration.ZERO) > 5.seconds })
            handler(ActionHandler { ActionResult.Success("Profile started") })
        },
        xbaseAction("xbasepp.editor.toggleLogMarkers") {
            presentation {
                text = "Toggle Log Markers"
                description = "Show or hide log markers"
                category = "Diagnostics"
                tag("markers")
            }
            handler(ActionHandler { ActionResult.Success("Markers toggled") })
        },
        xbaseAction("xbasepp.editor.quickFixes") {
            presentation {
                text = "Show Quick Fixes"
                description = "List available quick fixes"
                category = "Diagnostics"
                tag("quickfix")
            }
            enabledWhen(ActionCondition { it.get(SelectedText)?.isNotBlank() == true })
            handler(ActionHandler { ActionResult.Success("Quick fixes available") })
        }
    )

    fun registerAll(registry: ActionRegistry) {
        demoActions.forEach(registry::register)
    }
}
