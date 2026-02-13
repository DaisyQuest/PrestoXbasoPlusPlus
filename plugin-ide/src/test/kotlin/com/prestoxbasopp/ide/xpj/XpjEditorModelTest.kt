package com.prestoxbasopp.ide.xpj

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XpjEditorModelTest {
    @Test
    fun `adds sections entries and removes selected entry`() {
        val model = XpjEditorModel(XpjProjectFile.empty())

        model.addSection("PROJECT")
        model.addDefinition("PROJECT", "DEBUG", "YES")
        model.addReference("PROJECT", "project.xpj")
        model.removeEntry("PROJECT", 0)

        assertThat(model.sectionNames()).containsExactly("PROJECT")
        assertThat(model.section("PROJECT")?.entries).containsExactly(XpjEntry.Reference("project.xpj"))
    }

    @Test
    fun `ignores duplicate sections by name and missing section updates`() {
        val model = XpjEditorModel(XpjProjectFile.empty())

        model.addSection("PROJECT")
        model.addSection("project")
        model.addDefinition("missing", "DEBUG", "NO")

        assertThat(model.sectionNames()).containsExactly("PROJECT")
    }
}
