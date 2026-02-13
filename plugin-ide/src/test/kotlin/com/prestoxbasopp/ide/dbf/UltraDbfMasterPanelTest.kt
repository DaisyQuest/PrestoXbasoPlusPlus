package com.prestoxbasopp.ide.dbf

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import javax.swing.JScrollPane
import javax.swing.JTextArea

class UltraDbfMasterPanelTest {
    private val project: Project =
        Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(Project::class.java),
        ) { _, _, _ -> null } as Project

    @Test
    fun `reverse engineering is a first class tab`() {
        val panel = UltraDbfMasterPanel(project)
        val tabs = panel.components.filterIsInstance<JBTabbedPane>().single()

        val titles = (0 until tabs.tabCount).map { tabs.getTitleAt(it) }

        assertThat(titles).contains("Table View", "Card View", "Filter View", "Reverse Engineering")
    }

    @Test
    fun `reverse engineering tab lists all workflow stages`() {
        val panel = UltraDbfMasterPanel(project)
        val tabs = panel.components.filterIsInstance<JBTabbedPane>().single()
        val reverseEngineeringIndex = (0 until tabs.tabCount).first { tabs.getTitleAt(it) == "Reverse Engineering" }

        val reverseEngineeringTab = tabs.getComponentAt(reverseEngineeringIndex) as java.awt.Container
        val reverseEngineeringScrollPane =
            reverseEngineeringTab.components.filterIsInstance<JScrollPane>().single()
        val reverseEngineeringText = (reverseEngineeringScrollPane.viewport.view as JTextArea).text

        assertThat(reverseEngineeringText).contains("Reverse Engineering Workflow")
        ReverseEngineeringWorkflow.defaultTabs().forEach { tab ->
            assertThat(reverseEngineeringText).contains(tab.title)
        }
    }
}
