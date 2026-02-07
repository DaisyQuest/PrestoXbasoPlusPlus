package com.prestoxbasopp.ide

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

class XbGotoDeclarationHandler(
    private val navigationAdapter: XbNavigationAdapter = XbNavigationAdapter(),
) : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?,
    ): Array<PsiElement>? {
        val file = sourceElement?.containingFile ?: return null
        if (file !is XbPsiFile) {
            return null
        }
        val targets = navigationAdapter.findTargets(file.text, offset)
        if (targets.isEmpty()) {
            return null
        }
        val elements = targets.mapNotNull { range -> file.findElementAt(range.startOffset) }
        return elements.takeIf { it.isNotEmpty() }?.toTypedArray()
    }
}
