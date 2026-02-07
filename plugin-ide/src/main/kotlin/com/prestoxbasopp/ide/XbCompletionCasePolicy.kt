package com.prestoxbasopp.ide

object XbCompletionCasePolicy {
    fun isCaseSensitive(prefix: String): Boolean {
        return prefix.any { it.isUpperCase() }
    }
}
