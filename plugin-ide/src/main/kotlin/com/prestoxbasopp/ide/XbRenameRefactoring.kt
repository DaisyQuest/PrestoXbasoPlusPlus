package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiElementType
import com.prestoxbasopp.core.psi.XbPsiSnapshot

data class XbRenameEdit(
    val textRange: XbTextRange,
    val replacement: String,
)

data class XbRenameResult(
    val updatedSnapshot: XbPsiSnapshot,
    val edits: List<XbRenameEdit>,
    val errors: List<String>,
)

class XbRenameRefactoring {
    fun rename(snapshot: XbPsiSnapshot, oldName: String, newName: String): XbRenameResult {
        if (newName.isBlank()) {
            return XbRenameResult(snapshot, emptyList(), listOf("New name must not be blank."))
        }
        if (oldName == newName) {
            return XbRenameResult(snapshot, emptyList(), emptyList())
        }
        val edits = mutableListOf<XbRenameEdit>()
        val updated = renameInSnapshot(snapshot, oldName, newName, edits)
        return XbRenameResult(updated, edits, emptyList())
    }

    private fun renameInSnapshot(
        snapshot: XbPsiSnapshot,
        oldName: String,
        newName: String,
        edits: MutableList<XbRenameEdit>,
    ): XbPsiSnapshot {
        val renamedChildren = snapshot.children.map { renameInSnapshot(it, oldName, newName, edits) }
        val shouldRename = when (snapshot.elementType) {
            XbPsiElementType.FUNCTION_DECLARATION,
            XbPsiElementType.VARIABLE_DECLARATION,
            XbPsiElementType.SYMBOL_REFERENCE,
            -> snapshot.name == oldName
            else -> false
        }
        if (!shouldRename) {
            return snapshot.copy(children = renamedChildren)
        }
        val updatedText = replaceFirstToken(snapshot.text, oldName, newName)
        edits += XbRenameEdit(snapshot.textRange, updatedText)
        return snapshot.copy(
            name = newName,
            text = updatedText,
            children = renamedChildren,
        )
    }

    private fun replaceFirstToken(text: String, oldName: String, newName: String): String {
        val index = text.indexOf(oldName)
        if (index == -1) {
            return text
        }
        return buildString {
            append(text.substring(0, index))
            append(newName)
            append(text.substring(index + oldName.length))
        }
    }
}
