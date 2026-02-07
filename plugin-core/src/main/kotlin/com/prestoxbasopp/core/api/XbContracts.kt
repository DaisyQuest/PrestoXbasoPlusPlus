package com.prestoxbasopp.core.api

/**
 * Contract for PSI-backed elements. Implementations must map to IntelliJ PSI elements,
 * but this interface stays SDK-neutral to keep module boundaries stable.
 */
interface XbPsiElementContract {
    val name: String?
    val textRange: XbTextRange
}

/**
 * Contract for stub-backed elements used across indexing and PSI layers.
 */
interface XbStubElementContract {
    val stubId: String
    val fqName: String?
}

/**
 * Boundary for language services exposed to IDE/UI modules.
 */
interface XbLanguageService {
    fun languageId(): String
}

/**
 * Simple text range abstraction to keep module APIs IntelliJ-compatible.
 */
data class XbTextRange(
    val startOffset: Int,
    val endOffset: Int,
) {
    init {
        require(startOffset >= 0) { "startOffset must be >= 0" }
        require(endOffset >= startOffset) { "endOffset must be >= startOffset" }
    }
}
