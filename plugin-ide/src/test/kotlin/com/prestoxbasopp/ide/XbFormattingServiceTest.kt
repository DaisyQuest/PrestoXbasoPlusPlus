package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbFormattingServiceTest {
    @Test
    fun `formatting service applies formatter to text`() {
        val source = """
            if foo
            bar()
            endif
        """.trimIndent()

        val formatted = XbFormattingService().formatText(source, indentSize = 2)

        assertThat(formatted).isEqualTo(
            """
            if foo
              bar()
            endif
            """.trimIndent(),
        )
    }
}
