package com.prestoxbasopp.ide

data class XbCompletionLookup(
    val label: String,
    val typeText: String,
    val insertText: String,
    val caretOffsetDelta: Int?,
)

class XbCompletionLookupMapper(
    private val insertStrategy: (XbCompletionItem) -> XbCompletionInsertPlan = XbCompletionInsertStrategy::plan,
) {
    fun map(items: List<XbCompletionItem>): List<XbCompletionLookup> {
        return items.map { item ->
            val plan = insertStrategy(item)
            XbCompletionLookup(
                label = item.name,
                typeText = item.type.name,
                insertText = plan.text,
                caretOffsetDelta = plan.caretOffsetDelta,
            )
        }
    }
}
