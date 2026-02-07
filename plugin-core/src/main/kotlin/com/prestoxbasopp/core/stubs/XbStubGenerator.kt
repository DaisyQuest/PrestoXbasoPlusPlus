package com.prestoxbasopp.core.stubs

import com.prestoxbasopp.core.psi.XbPsiElement
import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration

object XbStubGenerator {
    fun from(element: XbPsiElement, namespace: List<String> = emptyList()): XbStub? {
        return when (element) {
            is XbPsiFunctionDeclaration -> createStub(
                type = XbStubType.FUNCTION,
                name = element.symbolName,
                namespace = namespace,
            )
            is XbPsiVariableDeclaration -> createStub(
                type = XbStubType.VARIABLE,
                name = element.symbolName,
                namespace = namespace,
            )
            else -> null
        }
    }

    private fun createStub(type: XbStubType, name: String?, namespace: List<String>): XbStub? {
        val normalizedName = name?.takeIf { it.isNotBlank() } ?: return null
        val fqName = buildList {
            addAll(namespace.filter { it.isNotBlank() })
            add(normalizedName)
        }.joinToString(".")
        val stubId = "${type.name}:${fqName.ifBlank { normalizedName }}"
        return XbStub(
            stubId = stubId,
            fqName = fqName.ifBlank { null },
            name = normalizedName,
            type = type,
        )
    }
}
