package com.prestoxbasopp.core.psi

import com.prestoxbasopp.core.api.XbTextRange

data class XbPsiSnapshot(
    val elementType: XbPsiElementType,
    val name: String?,
    val textRange: XbTextRange,
    val text: String,
    val parameters: List<String> = emptyList(),
    val isMutable: Boolean? = null,
    val literalKind: String? = null,
    val symbolRole: XbSymbolRole? = null,
    val children: List<XbPsiSnapshot> = emptyList(),
) {
    companion object {
        fun fromElement(element: XbPsiElement): XbPsiSnapshot {
            val parameters = if (element is XbPsiFunctionDeclaration) element.parameters else emptyList()
            val isMutable = if (element is XbPsiVariableDeclaration) element.isMutable else null
            val literalKind = if (element is XbPsiLiteral) element.literalKind else null
            val symbolRole = if (element is XbPsiSymbol) element.role else null
            return XbPsiSnapshot(
                elementType = element.elementType,
                name = element.name,
                textRange = element.textRange,
                text = element.text,
                parameters = parameters,
                isMutable = isMutable,
                literalKind = literalKind,
                symbolRole = symbolRole,
                children = element.children.map { fromElement(it) },
            )
        }

        fun toElement(snapshot: XbPsiSnapshot): XbPsiElement {
            val children = snapshot.children.map { toElement(it) }
            return when (snapshot.elementType) {
                XbPsiElementType.FILE -> XbPsiFile(
                    name = snapshot.name,
                    textRange = snapshot.textRange,
                    text = snapshot.text,
                    children = children,
                )
                XbPsiElementType.BLOCK -> XbPsiBlock(
                    textRange = snapshot.textRange,
                    text = snapshot.text,
                    children = children,
                )
                XbPsiElementType.FUNCTION_DECLARATION -> XbPsiFunctionDeclaration(
                    symbolName = snapshot.name.orEmpty(),
                    parameters = snapshot.parameters,
                    textRange = snapshot.textRange,
                    text = snapshot.text,
                    children = children,
                )
                XbPsiElementType.VARIABLE_DECLARATION -> XbPsiVariableDeclaration(
                    symbolName = snapshot.name.orEmpty(),
                    isMutable = snapshot.isMutable ?: false,
                    textRange = snapshot.textRange,
                    text = snapshot.text,
                    children = children,
                )
                XbPsiElementType.SYMBOL_REFERENCE -> XbPsiSymbolReference(
                    symbolName = snapshot.name.orEmpty(),
                    textRange = snapshot.textRange,
                    text = snapshot.text,
                    children = children,
                )
                XbPsiElementType.LITERAL -> XbPsiLiteral(
                    literalKind = snapshot.literalKind ?: "literal",
                    textRange = snapshot.textRange,
                    text = snapshot.text,
                )
            }
        }
    }
}
