package com.prestoxbasopp.testframework.completion

object CompletionSnapshotHarness {
    fun assertCase(caseId: String, expected: CompletionCase, actual: CompletionCase) {
        assertCompletions(caseId, expected.completions, actual.completions)
        assertRanking(caseId, expected.ranking, actual.ranking)
    }

    fun assertCompletions(caseId: String, expected: CompletionSnapshot, actual: CompletionSnapshot) {
        if (expected.entries.size != actual.entries.size) {
            throw AssertionError(
                "Completion count mismatch for $caseId: expected=${expected.entries.size}, actual=${actual.entries.size}",
            )
        }
        expected.entries.zip(actual.entries).forEachIndexed { index, (expectedEntry, actualEntry) ->
            assertEntryField(caseId, index, "label", expectedEntry.label, actualEntry.label)
            assertEntryField(caseId, index, "kind", expectedEntry.kind, actualEntry.kind)
            assertEntryField(caseId, index, "source", expectedEntry.source, actualEntry.source)
            assertEntryField(caseId, index, "insertText", expectedEntry.insertText, actualEntry.insertText)
            assertEntryField(caseId, index, "detail", expectedEntry.detail, actualEntry.detail)
            assertEntryField(caseId, index, "type", expectedEntry.type, actualEntry.type)
        }
    }

    fun assertRanking(caseId: String, expected: RankingSnapshot, actual: RankingSnapshot) {
        if (expected.ranked.size != actual.ranked.size) {
            throw AssertionError(
                "Ranking count mismatch for $caseId: expected=${expected.ranked.size}, actual=${actual.ranked.size}",
            )
        }
        expected.ranked.zip(actual.ranked).forEachIndexed { index, (expectedEntry, actualEntry) ->
            assertEntryField(caseId, index, "label", expectedEntry.label, actualEntry.label)
            assertEntryField(caseId, index, "score", expectedEntry.score, actualEntry.score)
            assertEntryField(caseId, index, "scope", expectedEntry.scope, actualEntry.scope)
            assertEntryField(caseId, index, "typeCompat", expectedEntry.typeCompat, actualEntry.typeCompat)
            assertEntryField(caseId, index, "tieBreak", expectedEntry.tieBreak, actualEntry.tieBreak)
        }
    }

    private fun assertEntryField(caseId: String, index: Int, field: String, expected: Any?, actual: Any?) {
        if (expected != actual) {
            throw AssertionError(
                "Completion mismatch for $caseId at index $index ($field): expected=$expected, actual=$actual",
            )
        }
    }
}
