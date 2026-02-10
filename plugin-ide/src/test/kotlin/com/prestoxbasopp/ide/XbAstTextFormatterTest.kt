package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbAstTextFormatterTest {
    @Test
    fun `formats tree nodes with indentation for copy output`() {
        val root = XbAstTreeNode(
            label = "File",
            children = listOf(
                XbAstTreeNode(
                    label = "Function: Hello",
                    children = listOf(
                        XbAstTreeNode(label = "Params"),
                        XbAstTreeNode(
                            label = "Body",
                            children = listOf(
                                XbAstTreeNode(label = "Return"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val formatter = XbAstTextFormatter()

        val text = formatter.format(root)

        assertThat(text).isEqualTo(
            """
                File
                  Function: Hello
                    Params
                    Body
                      Return
            """.trimIndent(),
        )
    }
}
