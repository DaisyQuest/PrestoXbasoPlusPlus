package com.prestoxbasopp.core.psi

import com.prestoxbasopp.core.api.XbTextRange

class XbPsiFile(
    name: String?,
    textRange: XbTextRange,
    text: String,
    children: List<XbPsiElement> = emptyList(),
) : XbBasePsiElement(name, textRange, text, children, XbPsiElementType.FILE)

class XbPsiBlock(
    textRange: XbTextRange,
    text: String,
    children: List<XbPsiElement> = emptyList(),
) : XbBasePsiElement(null, textRange, text, children, XbPsiElementType.BLOCK)

class XbPsiFunctionDeclaration(
    override val symbolName: String,
    val parameters: List<String>,
    textRange: XbTextRange,
    text: String,
    children: List<XbPsiElement> = emptyList(),
) : XbBasePsiElement(symbolName, textRange, text, children, XbPsiElementType.FUNCTION_DECLARATION), XbPsiSymbol {
    override val role: XbSymbolRole = XbSymbolRole.DECLARATION
}

class XbPsiVariableDeclaration(
    override val symbolName: String,
    val isMutable: Boolean,
    val storageClass: XbVariableStorageClass = XbVariableStorageClass.LOCAL,
    textRange: XbTextRange,
    text: String,
    children: List<XbPsiElement> = emptyList(),
) : XbBasePsiElement(symbolName, textRange, text, children, XbPsiElementType.VARIABLE_DECLARATION), XbPsiSymbol {
    override val role: XbSymbolRole = XbSymbolRole.DECLARATION
}

enum class XbVariableStorageClass {
    LOCAL,
    STATIC,
    PRIVATE,
    PUBLIC,
    GLOBAL,
}

class XbPsiSymbolReference(
    override val symbolName: String,
    textRange: XbTextRange,
    text: String,
    children: List<XbPsiElement> = emptyList(),
) : XbBasePsiElement(symbolName, textRange, text, children, XbPsiElementType.SYMBOL_REFERENCE), XbPsiSymbol {
    override val role: XbSymbolRole = XbSymbolRole.USAGE
}

class XbPsiLiteral(
    val literalKind: String,
    textRange: XbTextRange,
    text: String,
) : XbBasePsiElement(null, textRange, text, emptyList(), XbPsiElementType.LITERAL)
