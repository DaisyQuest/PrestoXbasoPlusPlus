package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbColorSettingsPageTest {
    @Test
    fun `color settings page exposes descriptors for every category`() {
        val page = XbColorSettingsPage()

        val tags = page.additionalHighlightingTagToDescriptorMap
        val descriptors = page.attributeDescriptors

        assertThat(tags.keys).containsAll(
            com.prestoxbasopp.ui.XbHighlightCategory.entries.map { it.name },
        )
        assertThat(descriptors).hasSize(com.prestoxbasopp.ui.XbHighlightCategory.entries.size)
        assertThat(page.displayName).isEqualTo("Xbase++")
        assertThat(page.demoText).contains("defs.ch", "function", "BuildReport")
    }
}
