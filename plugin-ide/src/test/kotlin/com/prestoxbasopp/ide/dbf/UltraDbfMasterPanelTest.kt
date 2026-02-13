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
        val workflowTabs = reverseEngineeringTab.components.filterIsInstance<JBTabbedPane>().single()
        val reverseEngineeringScrollPane = workflowTabs.getComponentAt(0) as JScrollPane
        val reverseEngineeringText = (reverseEngineeringScrollPane.viewport.view as JTextArea).text

        assertThat(reverseEngineeringText).contains("Reverse Engineering Workflow")
        val titles = (0 until workflowTabs.tabCount).map { workflowTabs.getTitleAt(it) }
        assertThat(titles).containsExactlyElementsOf(ReverseEngineeringWorkflow.defaultTabs().map { it.title })
    }

    @Test
    fun `panel can switch to reverse engineering tab programmatically`() {
        val panel = UltraDbfMasterPanel(project)
        val tabs = panel.components.filterIsInstance<JBTabbedPane>().single()

        panel.openReverseEngineeringTab()

        assertThat(tabs.getTitleAt(tabs.selectedIndex)).isEqualTo("Reverse Engineering")
    }

    @Test
    fun `reverse engineering open request store consumes one shot requests`() {
        val path = "/tmp/sample.dbf"

        ReverseEngineeringOpenRequestStore.request(path)

        assertThat(ReverseEngineeringOpenRequestStore.consume(path)).isTrue()
        assertThat(ReverseEngineeringOpenRequestStore.consume(path)).isFalse()
    }

}
