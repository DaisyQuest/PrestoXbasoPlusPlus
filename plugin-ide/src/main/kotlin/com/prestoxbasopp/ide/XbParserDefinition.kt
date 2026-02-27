package com.prestoxbasopp.ide

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.prestoxbasopp.core.lexer.XbTokenType as CoreTokenType

object XbElementTypes {
    val FILE: IFileElementType = IFileElementType(XbLanguage)
}

class XbPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, XbLanguage) {
    override fun getFileType() = XbFileType

    override fun toString(): String = "Xbase++ File"
}

class XbParserDefinition : ParserDefinition {
    override fun createLexer(project: Project): Lexer = XbLexerAdapter(mode = XbLexerAdapter.Mode.PARSING)

    override fun createParser(project: Project): PsiParser = XbPsiParser()

    override fun getFileNodeType(): IFileElementType = XbElementTypes.FILE

    override fun getWhitespaceTokens(): TokenSet = TokenSet.create(TokenType.WHITE_SPACE)

    override fun getCommentTokens(): TokenSet = TokenSet.create(
        XbHighlighterTokenSet.forToken(CoreTokenType.COMMENT),
    )

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(
        XbHighlighterTokenSet.forToken(CoreTokenType.STRING),
    )

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = XbPsiFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(
        left: ASTNode,
        right: ASTNode,
    ): ParserDefinition.SpaceRequirements = ParserDefinition.SpaceRequirements.MAY
}

private class XbPsiParser : PsiParser {
    override fun parse(root: IElementType, builder: com.intellij.lang.PsiBuilder): ASTNode {
        val marker = builder.mark()
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        marker.done(root)
        return builder.treeBuilt
    }
}
