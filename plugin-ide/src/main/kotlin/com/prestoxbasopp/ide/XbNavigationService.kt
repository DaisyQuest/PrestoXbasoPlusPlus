package com.prestoxbasopp.ide

import com.prestoxbasopp.core.index.XbSymbolIndex
import com.prestoxbasopp.core.index.XbSymbolSearchResult
import com.prestoxbasopp.core.psi.XbPsiElement
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

    fun findAll(name: String, type: XbStubType, index: XbSymbolIndex): XbNavigationTargets {
        val result: XbSymbolSearchResult = index.findAll(name, type)
        return XbNavigationTargets(
            declarations = result.declarations,
            usages = result.usages,
        )
    }
}
