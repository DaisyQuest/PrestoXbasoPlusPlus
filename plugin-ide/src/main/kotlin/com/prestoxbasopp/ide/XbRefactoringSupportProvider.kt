package com.prestoxbasopp.ide

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringActionHandler

class XbRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        return element.containingFile is XbPsiFile
    }

    override fun getIntroduceVariableHandler(): RefactoringActionHandler = XbExtractVariableHandler()

    override fun getIntroduceMethodHandler(): RefactoringActionHandler = XbExtractFunctionHandler()
}
