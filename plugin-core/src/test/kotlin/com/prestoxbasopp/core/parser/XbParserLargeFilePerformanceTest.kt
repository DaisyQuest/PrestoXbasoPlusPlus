package com.prestoxbasopp.core.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration

class XbParserLargeFilePerformanceTest {
    @Test
    fun `parses large generated source within timeout and without errors`() {
        val source = buildString {
            appendLine("FUNCTION HugeProgram()")
            repeat(10_000) { index ->
                appendLine("   LOCAL value$index := $index")
                appendLine("   value$index := value$index + 1")
                appendLine("   ? value$index")
            }
            appendLine("RETURN NIL")
            appendLine("ENDFUNCTION")
        }

        val result = assertTimeoutPreemptively(Duration.ofSeconds(8)) {
            XbParser.parse(source)
        }

        assertThat(result.errors).isEmpty()
        assertThat(result.program).isNotNull
        assertThat(result.program!!.statements).hasSize(1)
    }

    @Test
    fun `parses large expresspp style UI source within timeout`() {
        val source = buildString {
            appendLine("FUNCTION HugeUi(oDlg)")
            repeat(2_500) { index ->
                appendLine("   LOCAL oBtn$index := XbpPushButton():new( oDlg:drawingArea,, { 10, 10 }, { 80, 24 } )")
                appendLine("   oBtn$index:caption := \"Btn$index\"")
                appendLine("   oBtn$index:activate := { || NIL }")
            }
            appendLine("RETURN NIL")
            appendLine("ENDFUNCTION")
        }

        val result = assertTimeoutPreemptively(Duration.ofSeconds(8)) {
            XbParser.parse(source)
        }

        assertThat(result.errors).isEmpty()
        assertThat(result.program).isNotNull
        assertThat(result.program!!.statements).hasSize(1)
    }

}

