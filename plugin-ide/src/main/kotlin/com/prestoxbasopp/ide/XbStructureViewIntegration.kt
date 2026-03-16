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
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

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
                return XbStructureViewModel(psiFile, editor, rootItem)
            }
        }
    }
}

private class XbStructureViewModel(
    private val psiFile: PsiFile,
    private val editor: Editor?,
    private val rootItem: XbStructureItem,
    private val delegate: StructureViewModel = StructureViewModelBase(
        psiFile,
        XbStructureViewElement(psiFile, rootItem),
    ),
) : StructureViewModel by delegate,
    StructureViewModel.ElementInfoProvider,
    StructureViewModel.ExpandInfoProvider {
    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = when (element) {
        is XbStructureViewElement -> element.hasChildren()
        else -> false
    }



    override fun isAutoExpand(element: StructureViewTreeElement): Boolean = when (element) {
        is XbStructureViewElement -> element.isRoot()
        else -> false
    }

    override fun isSmartExpand(): Boolean = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = when (element) {
        is XbStructureViewElement -> !element.hasChildren()
        else -> false
    }

    override fun getCurrentEditorElement(): Any? {
        val offset = editor?.caretModel?.offset ?: return null
        return XbStructureViewSelectionResolver.findDeepestItemNearOffset(rootItem, offset)
    }
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

    override fun getIcon(unused: Boolean) = XbStructureViewPresentation.iconFor(item)

    override fun navigate(requestFocus: Boolean) {
        val virtualFile = psiFile.virtualFile ?: return
        OpenFileDescriptor(psiFile.project, virtualFile, item.textRange.startOffset).navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = psiFile.virtualFile != null

    override fun canNavigateToSource(): Boolean = canNavigate()

    fun hasChildren(): Boolean = item.children.isNotEmpty()

    fun isRoot(): Boolean = item.elementType == com.prestoxbasopp.core.psi.XbPsiElementType.FILE

    private fun findPsiElement(): PsiElement? = findPsiElement(psiFile, item)

    companion object {
        fun findPsiElement(psiFile: PsiFile, item: XbStructureItem): PsiElement? {
            if (item.elementType == com.prestoxbasopp.core.psi.XbPsiElementType.FILE) {
                return psiFile
            }
            val startOffset = item.textRange.startOffset
            val element = psiFile.findElementAt(startOffset) ?: return null
            val targetRange = TextRange(item.textRange.startOffset, item.textRange.endOffset)
            var current: PsiElement? = element
            var bestMatch: PsiElement? = null
            while (current != null && current != psiFile) {
                val range = current.textRange
                if (range == targetRange) {
                    return current
                }
                if (range.contains(targetRange)) {
                    bestMatch = current
                }
                current = current.parent
            }
            return bestMatch ?: element
        }
    }
}

internal object XbStructureViewSelectionResolver {
    fun findDeepestItemNearOffset(root: XbStructureItem, offset: Int): XbStructureItem? {
        val current = findDeepestItemAtOffset(root, offset)
        if (current == null) {
            return if (offset > 0) findDeepestItemAtOffset(root, offset - 1) else null
        }
        val isRootSelection = current == root
        if (!isRootSelection || offset <= root.textRange.startOffset) {
            return current
        }
        return findDeepestItemAtOffset(root, offset - 1) ?: current
    }

    fun findDeepestItemAtOffset(root: XbStructureItem, offset: Int): XbStructureItem? {
        if (!containsOffset(root, offset)) {
            return null
        }

        var current = root
        while (true) {
            val matchingChild = current.children
                .filter { containsOffset(it, offset) }
                .minByOrNull { it.textRange.endOffset - it.textRange.startOffset }
                ?: return current
            current = matchingChild
        }
    }

    private fun containsOffset(item: XbStructureItem, offset: Int): Boolean {
        val start = item.textRange.startOffset
        val end = item.textRange.endOffset
        return when {
            end == start -> offset == start
            else -> offset in start until end
        }
    }
}
