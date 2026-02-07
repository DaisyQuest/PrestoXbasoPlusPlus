package com.prestoxbasopp.core.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class XbTextRangeTest {
    @Test
    fun `accepts valid range`() {
        val range = XbTextRange(0, 5)

        assertThat(range.startOffset).isEqualTo(0)
        assertThat(range.endOffset).isEqualTo(5)
    }

    @Test
    fun `rejects negative start offset`() {
        assertThatThrownBy { XbTextRange(-1, 5) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("startOffset")
    }

    @Test
    fun `rejects end offset before start`() {
        assertThatThrownBy { XbTextRange(5, 4) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("endOffset")
    }
}
