package com.prestoxbasopp.ide

import com.prestoxbasopp.core.index.XbSymbolIndex
import com.prestoxbasopp.core.index.XbSymbolSearchResult
import com.prestoxbasopp.core.psi.XbPsiElement
import com.prestoxbasopp.core.psi.XbPsiElementType
import com.prestoxbasopp.core.psi.XbPsiSnapshot
import com.prestoxbasopp.core.psi.XbPsiSymbol
import com.prestoxbasopp.core.stubs.XbStub
import com.prestoxbasopp.core.stubs.XbStubType

data class XbNavigationTargets(
    val declarations: List<XbStub>,
    val usages: List<XbPsiSymbol>,
)

class XbNavigationService {
    fun buildIndex(root: XbPsiElement): XbSymbolIndex {
        return XbSymbolIndex().also { it.index(root) }
    }

    fun findDeclarations(name: String, type: XbStubType, index: XbSymbolIndex): List<XbStub> {
        return index.findDeclarations(name, type)
    }

    fun findUsages(name: String, index: XbSymbolIndex): List<XbPsiSymbol> {
        return index.findUsages(name)
    }

    fun findFunctionTargets(name: String, index: XbSymbolIndex): XbNavigationTargets {
        val declarations = index.findDeclarations(name, XbStubType.FUNCTION)
        val usages = if (declarations.isEmpty()) emptyList() else index.findUsages(name)
        return XbNavigationTargets(
            declarations = declarations,
            usages = usages,
        )
    }

    fun jumpToFunctionDeclaration(name: String, index: XbSymbolIndex): XbStub? {
        return findFunctionTargets(name, index).declarations.firstOrNull()
    }

    fun findFunctionUsages(name: String, index: XbSymbolIndex): List<XbPsiSymbol> {
        return findFunctionTargets(name, index).usages
    }

    fun findUsagesFromDefinition(
        root: XbPsiElement,
        snapshot: XbPsiSnapshot,
        index: XbSymbolIndex,
    ): List<XbPsiSymbol> {
        val name = snapshot.name?.takeIf { it.isNotBlank() } ?: return emptyList()
        return when (snapshot.elementType) {
            XbPsiElementType.FUNCTION_DECLARATION -> index.findUsages(name)
            XbPsiElementType.VARIABLE_DECLARATION -> {
                val declaration = XbVariableScopeResolver.findDeclarationByRange(root, snapshot.textRange, name)
                    ?: return emptyList()
                XbVariableScopeResolver.findUsages(root, declaration)
            }
            else -> emptyList()
        }
    }

    fun jumpToDefinitionFromInvocation(snapshot: XbPsiSnapshot, index: XbSymbolIndex): XbStub? {
        if (snapshot.elementType != XbPsiElementType.SYMBOL_REFERENCE) {
            return null
        }
        val name = snapshot.name?.takeIf { it.isNotBlank() } ?: return null
        return index.findDeclarations(name, XbStubType.FUNCTION).firstOrNull()
    }

    fun findAll(name: String, type: XbStubType, index: XbSymbolIndex): XbNavigationTargets {
        val result: XbSymbolSearchResult = index.findAll(name, type)
        return XbNavigationTargets(
            declarations = result.declarations,
            usages = result.usages,
        )
    }
}
