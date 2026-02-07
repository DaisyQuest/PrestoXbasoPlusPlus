package com.prestoxbasopp.ide

import com.prestoxbasopp.core.stubs.XbStubType

data class XbCompletionInsertPlan(
    val text: String,
    val caretOffsetDelta: Int? = null,
)

object XbCompletionInsertStrategy {
    fun plan(item: XbCompletionItem): XbCompletionInsertPlan {
        return when (item.type) {
            XbStubType.FUNCTION -> XbCompletionInsertPlan(
                text = "${item.name}()",
                caretOffsetDelta = -1,
            )
            else -> XbCompletionInsertPlan(text = item.name)
        }
    }
}
