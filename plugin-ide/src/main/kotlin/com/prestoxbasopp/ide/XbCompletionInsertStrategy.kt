package com.prestoxbasopp.ide

data class XbCompletionInsertPlan(
    val text: String,
    val caretOffsetDelta: Int? = null,
)

object XbCompletionInsertStrategy {
    fun plan(item: XbCompletionItem): XbCompletionInsertPlan {
        return when (item.type) {
            XbCompletionType.FUNCTION -> XbCompletionInsertPlan(
                text = "${item.name}()",
                caretOffsetDelta = -1,
            )
            else -> XbCompletionInsertPlan(text = item.name)
        }
    }
}
