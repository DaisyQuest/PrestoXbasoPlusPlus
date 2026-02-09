package com.prestoxbasopp.ide

import com.prestoxbasopp.core.psi.XbPsiSnapshot

data class XbRenameServiceResult(
    val updatedText: String,
    val edits: List<XbRenameEdit>,
    val errors: List<String>,
    val oldName: String?,
)

class XbRenameService(
    private val builder: XbPsiTextBuilder = XbPsiTextBuilder(),
    private val refactoring: XbRenameRefactoring = XbRenameRefactoring(),
) {
    fun canRename(source: String, offset: Int): Boolean {
        return findSymbolName(source, offset) != null
    }

    fun findSymbolName(source: String, offset: Int): String? {
        val root = builder.build(source)
        return XbPsiSymbolLocator.findSymbol(root, offset)?.symbolName
    }

    fun rename(source: String, offset: Int, newName: String): XbRenameServiceResult {
        val root = builder.build(source)
        val symbol = XbPsiSymbolLocator.findSymbol(root, offset)
        val oldName = symbol?.symbolName
            ?: return XbRenameServiceResult(source, emptyList(), listOf("No symbol found at the caret."), null)
        val anchor = when (symbol) {
            is com.prestoxbasopp.core.psi.XbPsiVariableDeclaration -> XbRenameAnchor(
                declarationRange = symbol.textRange,
                elementType = com.prestoxbasopp.core.psi.XbPsiElementType.VARIABLE_DECLARATION,
            )
            is com.prestoxbasopp.core.psi.XbPsiSymbolReference -> {
                val declaration = XbVariableScopeResolver.resolveDeclaration(root, symbol)
                declaration?.let {
                    XbRenameAnchor(
                        declarationRange = it.textRange,
                        elementType = com.prestoxbasopp.core.psi.XbPsiElementType.VARIABLE_DECLARATION,
                    )
                }
            }
            else -> null
        }
        val snapshot = XbPsiSnapshot.fromElement(root)
        val result = refactoring.rename(snapshot, oldName, newName, anchor)
        if (result.errors.isNotEmpty()) {
            return XbRenameServiceResult(source, result.edits, result.errors, oldName)
        }
        val updatedText = applyEdits(source, result.edits)
        return XbRenameServiceResult(updatedText, result.edits, emptyList(), oldName)
    }

    internal fun applyEdits(source: String, edits: List<XbRenameEdit>): String {
        var updated = source
        edits.sortedByDescending { it.textRange.startOffset }.forEach { edit ->
            updated = updated.replaceRange(
                edit.textRange.startOffset,
                edit.textRange.endOffset,
                edit.replacement,
            )
        }
        return updated
    }
}
