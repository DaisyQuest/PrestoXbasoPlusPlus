package com.prestoxbasopp.core.index

import com.prestoxbasopp.core.psi.XbPsiElement
import com.prestoxbasopp.core.psi.XbPsiSymbol
import com.prestoxbasopp.core.psi.XbSymbolRole
import com.prestoxbasopp.core.stubs.XbStub
import com.prestoxbasopp.core.stubs.XbStubGenerator
import com.prestoxbasopp.core.stubs.XbStubType

data class XbIndexKey(
    val name: String,
    val type: XbStubType,
)

data class XbSymbolSearchResult(
    val declarations: List<XbStub>,
    val usages: List<XbPsiSymbol>,
)

class XbSymbolIndex {
    private val declarations: MutableMap<XbIndexKey, MutableList<XbStub>> = mutableMapOf()
    private val usages: MutableMap<String, MutableList<XbPsiSymbol>> = mutableMapOf()

    fun index(root: XbPsiElement, namespace: List<String> = emptyList()) {
        root.walk().forEach { element ->
            indexElement(element, namespace)
        }
    }

    fun indexElement(element: XbPsiElement, namespace: List<String> = emptyList()) {
        if (element is XbPsiSymbol) {
            if (element.role == XbSymbolRole.DECLARATION) {
                val stub = XbStubGenerator.from(element, namespace) ?: return
                val key = XbIndexKey(stub.name ?: return, stub.type)
                declarations.getOrPut(key) { mutableListOf() }.add(stub)
            } else {
                val name = element.symbolName.takeIf { it.isNotBlank() } ?: return
                usages.getOrPut(name) { mutableListOf() }.add(element)
            }
        }
    }

    fun findDeclarations(name: String, type: XbStubType): List<XbStub> {
        val key = XbIndexKey(name, type)
        return declarations[key].orEmpty()
    }

    fun findUsages(name: String): List<XbPsiSymbol> = usages[name].orEmpty()

    fun findAll(name: String, type: XbStubType): XbSymbolSearchResult = XbSymbolSearchResult(
        declarations = findDeclarations(name, type),
        usages = findUsages(name),
    )
}
