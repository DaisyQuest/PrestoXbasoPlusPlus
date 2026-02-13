package com.prestoxbasopp.ide.xpj

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XpjProjectSerializerTest {
    private val serializer = XpjProjectSerializer()

    @Test
    fun `serializes sections with consistent indentation`() {
        val text = serializer.serialize(
            XpjProjectFile(
                listOf(
                    XpjSection(
                        "PROJECT",
                        listOf(XpjEntry.Definition("DEBUG", "YES"), XpjEntry.Reference("project.xpj")),
                    ),
                    XpjSection("project.xpj", listOf(XpjEntry.Reference("app.exe"))),
                ),
            ),
        )

        assertThat(text).isEqualTo(
            """
                [PROJECT]
                    DEBUG = YES
                    project.xpj

                [project.xpj]
                    app.exe
            """.trimIndent(),
        )
    }
}
