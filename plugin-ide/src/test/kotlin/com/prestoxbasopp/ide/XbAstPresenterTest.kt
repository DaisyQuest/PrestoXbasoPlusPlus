package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbAstPresenterTest {
    @Test
    fun `returns empty presentation when no editor is active`() {
        val presenter = XbAstPresenter()

        val presentation = presenter.present(fileName = null, text = null)

        assertThat(presentation.root).isNull()
        assertThat(presentation.message).isEqualTo("No active editor.")
    }

    @Test
    fun `builds tree nodes for function declarations and statements`() {
        val presenter = XbAstPresenter()
        val source = """
            FUNCTION Foo(a)
               LOCAL x := 1
               RETURN x
            ENDFUNCTION
        """.trimIndent()

        val presentation = presenter.present(fileName = "sample.prg", text = source)

        val root = presentation.root
        assertThat(root?.label).isEqualTo("File")
        val functionNode = root?.children?.firstOrNull()
        assertThat(functionNode?.label).isEqualTo("Function: Foo")
        val paramsNode = functionNode?.children?.firstOrNull()
        assertThat(paramsNode?.label).isEqualTo("Params")
        assertThat(paramsNode?.children?.map { it.label }).containsExactly("Param: a")
        val bodyNode = functionNode?.children?.getOrNull(1)
        assertThat(bodyNode?.label).isEqualTo("Body")
        assertThat(bodyNode?.children?.map { it.label }).containsExactly("Local", "Return")
    }
}
