package com.prestoxbasopp.ide

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class XbStructureViewBuilderFactory(
    private val fileContentResolver: XbStructureViewFileContentResolver = XbStructureViewFileContentResolver(),
    private val rootBuilder: XbStructureViewRootBuilder = XbStructureViewRootBuilder(),
) : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is XbPsiFile) {
            return null
        }
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                val content = fileContentResolver.resolve(
                    fileName = psiFile.name,
                    psiText = psiFile.text,
                    editorText = editor?.document?.text,
                )
                val rootItem = rootBuilder.buildRoot(content)
                return XbStructureViewModel(psiFile, rootItem)
            }
        }
    }
}

private class XbStructureViewModel(
    psiFile: PsiFile,
    rootItem: XbStructureItem,
    private val delegate: StructureViewModel = StructureViewModelBase(
        psiFile,
        XbStructureViewElement(psiFile, rootItem),
    ),
) : StructureViewModel by delegate,
    StructureViewModel.ElementInfoProvider {
    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = false
}

private class XbStructureViewElement(
    private val psiFile: PsiFile,
    private val item: XbStructureItem,
) : StructureViewTreeElement, SortableTreeElement, ItemPresentation {
    override fun getValue(): Any = item

    override fun getChildren(): Array<TreeElement> = item.children
        .map { XbStructureViewElement(psiFile, it) }
        .toTypedArray()

    override fun getPresentation(): ItemPresentation = this

    override fun getPresentableText(): String = item.name

    override fun getAlphaSortKey(): String = item.name

    override fun getIcon(unused: Boolean) = null

    override fun navigate(requestFocus: Boolean) {
        val target = findPsiElement() as? NavigatablePsiElement ?: return
        target.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = findPsiElement() is NavigatablePsiElement

    override fun canNavigateToSource(): Boolean = canNavigate()

    private fun findPsiElement(): PsiElement? {
        val startOffset = item.textRange.startOffset
        val element = psiFile.findElementAt(startOffset)
        return PsiTreeUtil.getParentOfType(element, PsiElement::class.java, false) ?: element
    }
}
