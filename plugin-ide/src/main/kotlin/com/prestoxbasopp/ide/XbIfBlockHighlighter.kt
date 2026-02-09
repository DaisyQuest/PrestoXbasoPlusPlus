package com.prestoxbasopp.ide

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.prestoxbasopp.core.api.XbTextRange

class XbIfBlockHighlighter(
    private val rangeFinder: XbIfBlockRangeFinder = XbIfBlockRangeFinder(),
) : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val listener = XbIfBlockHoverListener(editor, rangeFinder)
        editor.addEditorMouseMotionListener(listener)
        editor.putUserData(HOVER_LISTENER_KEY, listener)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        val listener = editor.getUserData(HOVER_LISTENER_KEY)
        if (listener != null) {
            editor.removeEditorMouseMotionListener(listener)
            editor.putUserData(HOVER_LISTENER_KEY, null)
        }
    }

    private companion object {
        val HOVER_LISTENER_KEY: Key<XbIfBlockHoverListener> = Key.create("xb.if.block.hover.listener")
    }
}

private class XbIfBlockHoverListener(
    private val editor: Editor,
    private val rangeFinder: XbIfBlockRangeFinder,
) : EditorMouseMotionListener {
    private var highlighter: RangeHighlighter? = null
    private var lastRange: XbTextRange? = null
    private var lastStamp: Long = -1L
    private var lastIndex: XbIfBlockIndex? = null

    override fun mouseMoved(event: EditorMouseEvent) {
        val project = editor.project ?: return
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        if (psiFile !is XbPsiFile) {
            clearHighlight()
            return
        }
        val offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(event.mouseEvent.point))
        val index = ensureIndex()
        val range = index?.let { rangeFinder.findRange(it, offset) }
        if (range == lastRange) {
            return
        }
        if (range == null) {
            clearHighlight()
            return
        }
        highlight(range)
    }

    private fun ensureIndex(): XbIfBlockIndex? {
        val stamp = editor.document.modificationStamp
        if (stamp == lastStamp && lastIndex != null) {
            return lastIndex
        }
        val source = editor.document.text
        lastIndex = rangeFinder.buildIndex(source)
        lastStamp = stamp
        return lastIndex
    }

    private fun highlight(range: XbTextRange) {
        clearHighlight()
        val attributes = EditorColorsManager.getInstance().globalScheme
            .getAttributes(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES)
        val markupModel = editor.markupModel
        highlighter = markupModel.addRangeHighlighter(
            range.startOffset,
            range.endOffset,
            HighlighterLayer.SELECTION - 1,
            attributes,
            HighlighterTargetArea.EXACT_RANGE,
        )
        lastRange = range
    }

    private fun clearHighlight() {
        highlighter?.let { editor.markupModel.removeHighlighter(it) }
        highlighter = null
        lastRange = null
    }
}
