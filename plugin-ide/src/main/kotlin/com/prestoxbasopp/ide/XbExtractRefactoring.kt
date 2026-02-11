package com.prestoxbasopp.ide

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringActionHandler

data class XbExtractResult(
    val updatedText: String,
    val errors: List<String> = emptyList(),
)

class XbExtractVariableService {
    fun extract(source: String, selectionStart: Int, selectionEnd: Int, variableName: String): XbExtractResult {
        val errors = mutableListOf<String>()
        if (variableName.isBlank()) {
            errors += "Variable name cannot be blank."
        }
        if (selectionStart < 0 || selectionEnd > source.length || selectionStart >= selectionEnd) {
            errors += "Select an expression to extract."
        }
        val selection = source.substring(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtMost(source.length))
        if (selection.isBlank()) {
            errors += "Selection must include an expression."
        }
        if (selection.contains('\n')) {
            errors += "Extract variable expects a single expression."
        }
        if (errors.isNotEmpty()) {
            return XbExtractResult(source, errors)
        }

        val trimmedSelection = selection.trim()
        val lineStart = lineStartOffset(source, selectionStart)
        val indent = lineIndent(source, lineStart)
        val declaration = "${indent}local $variableName := $trimmedSelection"
        val beforeLine = source.substring(0, lineStart)
        val lineAndRest = source.substring(lineStart)
        val replacedLine = lineAndRest.replaceRange(
            selectionStart - lineStart,
            selectionEnd - lineStart,
            variableName,
        )
        val updated = buildString {
            append(beforeLine)
            append(declaration)
            append('\n')
            append(replacedLine)
        }
        return XbExtractResult(updated)
    }
}

class XbExtractFunctionService {
    private val functionBodyIndent = "   "

    fun extract(source: String, selectionStart: Int, selectionEnd: Int, functionName: String): XbExtractResult {
        val errors = mutableListOf<String>()
        if (functionName.isBlank()) {
            errors += "Function name cannot be blank."
        }
        if (selectionStart < 0 || selectionEnd > source.length || selectionStart >= selectionEnd) {
            errors += "Select statements to extract."
        }
        val selection = source.substring(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtMost(source.length))
        if (selection.isBlank()) {
            errors += "Selection must include statements."
        }
        if (errors.isNotEmpty()) {
            return XbExtractResult(source, errors)
        }

        val normalizedSelection = selection.trimEnd().trimIndent()
        val body = normalizedSelection.lines().joinToString("\n") { line ->
            if (line.isBlank()) "" else "$functionBodyIndent$line"
        }
        val functionText = buildString {
            append("function ")
            append(functionName)
            append("()\n")
            append(body)
            append("\nendfunction")
        }
        val lineStart = lineStartOffset(source, selectionStart)
        val indent = lineIndent(source, lineStart)
        val replacement = buildFunctionCallReplacement(source, selectionEnd, indent, functionName)
        val replaced = source.replaceRange(selectionStart, selectionEnd, replacement)
        val trimmed = replaced.trimEnd()
        val updated = buildString {
            append(trimmed)
            append("\n\n")
            append(functionText)
        }
        return XbExtractResult(updated)
    }

    private fun buildFunctionCallReplacement(source: String, selectionEnd: Int, indent: String, functionName: String): String {
        val call = "$indent$functionName()"
        val nextChar = source.getOrNull(selectionEnd)
        if (selectionEnd >= source.length || nextChar == '\n') return "$call\n"
        val trailing = source.substring(selectionEnd)
        return if (trailing.startsWith(indent)) "$call\n" else call
    }
}

class XbExtractVariableHandler(
    private val service: XbExtractVariableService = XbExtractVariableService(),
) : RefactoringActionHandler {
    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext) {
        if (file !is XbPsiFile) {
            return
        }
        val selection = editor.selectionModel
        if (!selection.hasSelection()) {
            Messages.showErrorDialog(project, "Select an expression to extract.", "Extract Variable")
            return
        }
        val variableName = Messages.showInputDialog(
            project,
            "Extract expression to variable:",
            "Extract Variable",
            Messages.getQuestionIcon(),
            "value",
            null,
        ) ?: return
        val result = service.extract(
            editor.document.text,
            selection.selectionStart,
            selection.selectionEnd,
            variableName,
        )
        if (result.errors.isNotEmpty()) {
            Messages.showErrorDialog(project, result.errors.joinToString("\n"), "Extract Variable Failed")
            return
        }
        if (result.updatedText == editor.document.text) {
            return
        }
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.setText(result.updatedText)
        }
    }

    override fun invoke(project: Project, elements: Array<PsiElement>, dataContext: DataContext) {
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return
        invoke(project, editor, file, dataContext)
    }
}

class XbExtractFunctionHandler(
    private val service: XbExtractFunctionService = XbExtractFunctionService(),
) : RefactoringActionHandler {
    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext) {
        if (file !is XbPsiFile) {
            return
        }
        val selection = editor.selectionModel
        if (!selection.hasSelection()) {
            Messages.showErrorDialog(project, "Select statements to extract.", "Extract Function")
            return
        }
        val functionName = Messages.showInputDialog(
            project,
            "Extract statements into function:",
            "Extract Function",
            Messages.getQuestionIcon(),
            "NewFunction",
            null,
        ) ?: return
        val result = service.extract(
            editor.document.text,
            selection.selectionStart,
            selection.selectionEnd,
            functionName,
        )
        if (result.errors.isNotEmpty()) {
            Messages.showErrorDialog(project, result.errors.joinToString("\n"), "Extract Function Failed")
            return
        }
        if (result.updatedText == editor.document.text) {
            return
        }
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.setText(result.updatedText)
        }
    }

    override fun invoke(project: Project, elements: Array<PsiElement>, dataContext: DataContext) {
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return
        invoke(project, editor, file, dataContext)
    }
}

private fun lineStartOffset(source: String, offset: Int): Int {
    val index = source.lastIndexOf('\n', offset.coerceAtMost(source.length) - 1)
    return if (index == -1) 0 else index + 1
}

private fun lineIndent(source: String, lineStart: Int): String {
    val end = source.indexOf('\n', lineStart).let { if (it == -1) source.length else it }
    val line = source.substring(lineStart, end)
    return line.takeWhile { it == ' ' || it == '\t' }
}
