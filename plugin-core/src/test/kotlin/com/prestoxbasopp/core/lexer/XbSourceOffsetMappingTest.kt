package com.prestoxbasopp.core.lexer

import com.prestoxbasopp.core.api.XbTextRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbSourceOffsetMappingTest {
    @Test
    fun `maps range ends that align with segment boundaries`() {
        val mapping = XbSourceOffsetMapping(
            listOf(
                XbSourceOffsetMapping.Segment(logicalStart = 0, length = 3, sourceStart = 0),
                XbSourceOffsetMapping.Segment(logicalStart = 3, length = 2, sourceStart = 10),
            ),
        )

        val firstRange = mapping.toSourceRange(XbTextRange(0, 3))
        val secondRange = mapping.toSourceRange(XbTextRange(3, 5))

        assertThat(firstRange).isEqualTo(XbTextRange(0, 3))
        assertThat(secondRange).isEqualTo(XbTextRange(10, 12))
    }

    @Test
    fun `prefers previous segment when range end sits on a gap boundary`() {
        val mapping = XbSourceOffsetMapping(
            listOf(
                XbSourceOffsetMapping.Segment(logicalStart = 0, length = 2, sourceStart = 5),
                XbSourceOffsetMapping.Segment(logicalStart = 2, length = 3, sourceStart = 20),
            ),
        )

        val range = mapping.toSourceRange(XbTextRange(1, 2))

        assertThat(range).isEqualTo(XbTextRange(6, 7))
    }
}
