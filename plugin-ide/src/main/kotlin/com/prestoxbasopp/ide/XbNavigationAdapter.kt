package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiSymbolReference
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration

class XbNavigationAdapter(
    private val builder: XbPsiTextBuilder = XbPsiTextBuilder(),
    private val navigationService: XbNavigationService = XbNavigationService(),
) {
    fun findTargets(source: String, offset: Int): List<XbTextRange> {
        val root = builder.build(source)
        val symbol = XbPsiSymbolLocator.findSymbol(root, offset) ?: return emptyList()
        val name = symbol.symbolName.takeIf { it.isNotBlank() } ?: return emptyList()
        return when (symbol) {
            is XbPsiFunctionDeclaration -> usageTargets(root, name)
            is XbPsiVariableDeclaration -> variableUsageTargets(root, symbol)
            is XbPsiSymbolReference -> declarationTargets(root, symbol)
            else -> emptyList()
        }
    }

    private fun usageTargets(root: com.prestoxbasopp.core.psi.XbPsiElement, name: String): List<XbTextRange> {
        val index = navigationService.buildIndex(root)
        return navigationService.findUsages(name, index)
            .map { it.textRange }
            .sortedBy { it.startOffset }
    }

    private fun variableUsageTargets(
        root: com.prestoxbasopp.core.psi.XbPsiElement,
        declaration: XbPsiVariableDeclaration,
    ): List<XbTextRange> {
        return XbVariableScopeResolver.findUsages(root, declaration)
            .map { it.textRange }
            .sortedBy { it.startOffset }
    }

    private fun declarationTargets(
        root: com.prestoxbasopp.core.psi.XbPsiElement,
        reference: XbPsiSymbolReference,
    ): List<XbTextRange> {
        val variableDeclaration = XbVariableScopeResolver.resolveDeclaration(root, reference)
        if (variableDeclaration != null) {
            return listOf(variableDeclaration.textRange)
        }
        val name = reference.symbolName
        val functions = root.walk()
            .filterIsInstance<XbPsiFunctionDeclaration>()
            .filter { it.symbolName == name }
            .map { it.textRange }
            .toList()
        if (functions.isNotEmpty()) {
            return functions
        }
        return root.walk()
            .filterIsInstance<XbPsiVariableDeclaration>()
            .filter { it.symbolName == name }
            .map { it.textRange }
            .toList()
            .sortedBy { it.startOffset }
    }
}
