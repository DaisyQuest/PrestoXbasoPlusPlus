package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiElement
import com.prestoxbasopp.core.psi.XbPsiElementType
import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiSnapshot
import com.prestoxbasopp.core.psi.XbPsiSymbolReference
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration
import com.prestoxbasopp.core.psi.XbVariableStorageClass
import kotlin.math.absoluteValue

object XbVariableScopeResolver {
    fun resolveDeclaration(root: XbPsiElement, reference: XbPsiSymbolReference): XbPsiVariableDeclaration? {
        val declarations = root.walk().filterIsInstance<XbPsiVariableDeclaration>().toList()
        val functions = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().toList()
        return resolveDeclaration(reference.symbolName, reference.textRange, declarations, functions)
    }

    fun findUsages(root: XbPsiElement, declaration: XbPsiVariableDeclaration): List<XbPsiSymbolReference> {
        val declarations = root.walk().filterIsInstance<XbPsiVariableDeclaration>().toList()
        val functions = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().toList()
        return root.walk()
            .filterIsInstance<XbPsiSymbolReference>()
            .filter { it.symbolName == declaration.symbolName }
            .filter { resolveDeclaration(it.symbolName, it.textRange, declarations, functions) == declaration }
            .toList()
    }

    fun findDeclarationByRange(root: XbPsiElement, range: XbTextRange, name: String): XbPsiVariableDeclaration? {
        return root.walk()
            .filterIsInstance<XbPsiVariableDeclaration>()
            .firstOrNull { it.symbolName == name && it.textRange == range }
    }

    fun collectRenameRanges(root: XbPsiSnapshot, declarationRange: XbTextRange, name: String): Set<XbTextRange> {
        val declaration = findDeclarationByRange(root, declarationRange, name) ?: return emptySet()
        val usages = findUsages(root, declaration)
        return (listOf(declaration) + usages).map { it.textRange }.toSet()
    }

    fun findDeclarationByRange(root: XbPsiSnapshot, range: XbTextRange, name: String): XbPsiSnapshot? {
        return walk(root)
            .filter { it.elementType == XbPsiElementType.VARIABLE_DECLARATION }
            .firstOrNull { it.name == name && it.textRange == range }
    }

    fun findUsages(root: XbPsiSnapshot, declaration: XbPsiSnapshot): List<XbPsiSnapshot> {
        val declarations = walk(root).filter { it.elementType == XbPsiElementType.VARIABLE_DECLARATION }.toList()
        val functions = walk(root).filter { it.elementType == XbPsiElementType.FUNCTION_DECLARATION }.toList()
        return walk(root)
            .filter { it.elementType == XbPsiElementType.SYMBOL_REFERENCE }
            .filter { it.name == declaration.name }
            .filter { resolveDeclaration(it.name.orEmpty(), it.textRange, declarations, functions) == declaration }
            .toList()
    }

    private fun resolveDeclaration(
        name: String,
        referenceRange: XbTextRange,
        declarations: List<XbPsiVariableDeclaration>,
        functions: List<XbPsiFunctionDeclaration>,
    ): XbPsiVariableDeclaration? {
        val referenceFunction = enclosingFunction(functions, referenceRange)
        val functionScopedDeclarations = declarations
            .filter { it.symbolName == name }
            .filter { it.storageClass in functionScopedStorageClasses }
            .filter { enclosingFunction(functions, it.textRange) == referenceFunction }
            .sortedBy { it.textRange.startOffset }
        val functionScopedMatch = selectDeclaration(functionScopedDeclarations, referenceRange)
        if (functionScopedMatch != null) {
            return functionScopedMatch
        }

        val globalDeclarations = declarations
            .filter { it.symbolName == name }
            .filter { it.storageClass in globalStorageClasses }
            .sortedBy { it.textRange.startOffset }
        return selectDeclaration(globalDeclarations, referenceRange)
    }

    private fun resolveDeclaration(
        name: String,
        referenceRange: XbTextRange,
        declarations: List<XbPsiSnapshot>,
        functions: List<XbPsiSnapshot>,
    ): XbPsiSnapshot? {
        val referenceFunction = enclosingFunction(functions, referenceRange)
        val functionScopedDeclarations = declarations
            .filter { it.name == name }
            .filter { (it.storageClass ?: XbVariableStorageClass.LOCAL) in functionScopedStorageClasses }
            .filter { enclosingFunction(functions, it.textRange) == referenceFunction }
            .sortedBy { it.textRange.startOffset }
        val functionScopedMatch = selectDeclaration(functionScopedDeclarations, referenceRange)
        if (functionScopedMatch != null) {
            return functionScopedMatch
        }

        val globalDeclarations = declarations
            .filter { it.name == name }
            .filter { (it.storageClass ?: XbVariableStorageClass.LOCAL) in globalStorageClasses }
            .sortedBy { it.textRange.startOffset }
        return selectDeclaration(globalDeclarations, referenceRange)
    }

    private fun selectDeclaration(
        declarations: List<XbPsiVariableDeclaration>,
        referenceRange: XbTextRange,
    ): XbPsiVariableDeclaration? {
        val referenceStart = referenceRange.startOffset
        val before = declarations.filter { it.textRange.startOffset <= referenceStart }
        if (before.isNotEmpty()) {
            return before.last()
        }
        return declarations.minByOrNull { (it.textRange.startOffset - referenceStart).absoluteValue }
    }

    private fun selectDeclaration(
        declarations: List<XbPsiSnapshot>,
        referenceRange: XbTextRange,
    ): XbPsiSnapshot? {
        val referenceStart = referenceRange.startOffset
        val before = declarations.filter { it.textRange.startOffset <= referenceStart }
        if (before.isNotEmpty()) {
            return before.last()
        }
        return declarations.minByOrNull { (it.textRange.startOffset - referenceStart).absoluteValue }
    }

    private fun enclosingFunction(
        functions: List<XbPsiFunctionDeclaration>,
        range: XbTextRange,
    ): XbPsiFunctionDeclaration? {
        return functions
            .filter { containsRange(it.textRange, range) }
            .minByOrNull { it.textRange.endOffset - it.textRange.startOffset }
    }

    private fun enclosingFunction(
        functions: List<XbPsiSnapshot>,
        range: XbTextRange,
    ): XbPsiSnapshot? {
        return functions
            .filter { containsRange(it.textRange, range) }
            .minByOrNull { it.textRange.endOffset - it.textRange.startOffset }
    }


    private val functionScopedStorageClasses = setOf(
        XbVariableStorageClass.LOCAL,
        XbVariableStorageClass.STATIC,
        XbVariableStorageClass.PRIVATE,
    )

    private val globalStorageClasses = setOf(
        XbVariableStorageClass.PUBLIC,
        XbVariableStorageClass.GLOBAL,
    )

    private fun containsRange(container: XbTextRange, range: XbTextRange): Boolean {
        return range.startOffset >= container.startOffset && range.endOffset <= container.endOffset
    }

    private fun walk(snapshot: XbPsiSnapshot): Sequence<XbPsiSnapshot> = sequence {
        yield(snapshot)
        snapshot.children.forEach { child ->
            yieldAll(walk(child))
        }
    }
}
