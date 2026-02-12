package com.prestoxbasopp.core.lexer

import com.prestoxbasopp.core.api.XbTextRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration

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

    @Test
    fun `maps offsets efficiently across many sparse segments`() {
        val segments = buildList {
            var logical = 0
            var source = 0
            repeat(25_000) {
                add(XbSourceOffsetMapping.Segment(logicalStart = logical, length = 3, sourceStart = source))
                logical += 3
                source += 11
            }
        }
        val mapping = XbSourceOffsetMapping(segments)

        assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            repeat(200_000) { iteration ->
                val logicalOffset = (iteration % 25_000) * 3 + (iteration % 3)
                val expectedSourceOffset = (iteration % 25_000) * 11 + (iteration % 3)
                assertThat(mapping.toSourceOffset(logicalOffset)).isEqualTo(expectedSourceOffset)
            }
        }
    }
}
