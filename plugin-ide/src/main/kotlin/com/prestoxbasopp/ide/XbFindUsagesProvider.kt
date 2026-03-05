package com.prestoxbasopp.ide

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet

class XbFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        XbLexerAdapter(mode = XbLexerAdapter.Mode.PARSING),
        TokenSet.create(XbHighlighterTokenSet.forToken(com.prestoxbasopp.core.lexer.XbTokenType.IDENTIFIER)),
        TokenSet.create(XbHighlighterTokenSet.forToken(com.prestoxbasopp.core.lexer.XbTokenType.COMMENT)),
        TokenSet.create(XbHighlighterTokenSet.forToken(com.prestoxbasopp.core.lexer.XbTokenType.STRING), TokenType.WHITE_SPACE),
    )

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean = psiElement.text.isNotBlank()

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String = "symbol"

    override fun getDescriptiveName(element: PsiElement): String = element.text

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String = element.text
}
