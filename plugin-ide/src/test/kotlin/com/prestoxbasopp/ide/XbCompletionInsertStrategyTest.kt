package com.prestoxbasopp.ide

import com.prestoxbasopp.core.stubs.XbStubType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbCompletionInsertStrategyTest {
    @Test
    fun `plans function insertion with parentheses`() {
        val item = XbCompletionItem("Alpha", XbStubType.FUNCTION)

        val plan = XbCompletionInsertStrategy.plan(item)

        assertThat(plan.text).isEqualTo("Alpha()")
        assertThat(plan.caretOffsetDelta).isEqualTo(-1)
    }

    @Test
    fun `plans variable insertion without parentheses`() {
        val item = XbCompletionItem("Beta", XbStubType.VARIABLE)

        val plan = XbCompletionInsertStrategy.plan(item)

        assertThat(plan.text).isEqualTo("Beta")
        assertThat(plan.caretOffsetDelta).isNull()
    }
}
