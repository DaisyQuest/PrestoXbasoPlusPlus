package com.prestoxbasopp.testframework.completion

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CompletionSnapshotHarnessTest {
    @Test
    fun `accepts matching completion and ranking snapshots`() {
        val expected = completionCase(label = "Alpha", score = 0.9)
        val actual = completionCase(label = "Alpha", score = 0.9)

        CompletionSnapshotHarness.assertCase("case-1", expected, actual)
    }

    @Test
    fun `rejects completion entry mismatch`() {
        val expected = completionCase(label = "Alpha", score = 0.9)
        val actual = completionCase(label = "Beta", score = 0.9)

        assertThatThrownBy { CompletionSnapshotHarness.assertCase("case-2", expected, actual) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("label")
    }

    @Test
    fun `rejects completion count mismatch`() {
        val expected = completionCase(label = "Alpha", score = 0.9)
        val actual = CompletionCase(
            source = "/*caret*/",
            completions = CompletionSnapshot(
                entries = listOf(
                    completionEntry(label = "Alpha"),
                    completionEntry(label = "Beta"),
                ),
            ),
            ranking = expected.ranking,
        )

        assertThatThrownBy { CompletionSnapshotHarness.assertCompletions("case-3", expected.completions, actual.completions) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("Completion count mismatch")
    }

    @Test
    fun `rejects ranking mismatch`() {
        val expected = completionCase(label = "Alpha", score = 0.9)
        val actual = completionCase(label = "Alpha", score = 0.1)

        assertThatThrownBy { CompletionSnapshotHarness.assertRanking("case-4", expected.ranking, actual.ranking) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("score")
    }

    private fun completionCase(label: String, score: Double): CompletionCase {
        return CompletionCase(
            source = "/*caret*/",
            completions = CompletionSnapshot(entries = listOf(completionEntry(label))),
            ranking = RankingSnapshot(
                ranked = listOf(
                    RankingEntry(
                        label = label,
                        score = score,
                        scope = "LOCAL",
                        typeCompat = "EXACT",
                        tieBreak = "ALPHA",
                    ),
                ),
            ),
        )
    }

    private fun completionEntry(label: String): CompletionEntry {
        return CompletionEntry(
            label = label,
            kind = "FUNCTION",
            source = "LOCAL",
            insertText = label,
            detail = null,
            type = null,
        )
    }
}
