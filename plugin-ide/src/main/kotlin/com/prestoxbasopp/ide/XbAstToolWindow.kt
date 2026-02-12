package com.prestoxbasopp.ide

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import com.prestoxbasopp.core.ast.XbArrayLiteralExpression
import com.prestoxbasopp.core.ast.XbAssignmentStatement
import com.prestoxbasopp.core.ast.XbBinaryExpression
import com.prestoxbasopp.core.ast.XbBlock
import com.prestoxbasopp.core.ast.XbCallExpression
import com.prestoxbasopp.core.ast.XbExpression
import com.prestoxbasopp.core.ast.XbExpressionStatement
import com.prestoxbasopp.core.ast.XbForStatement
import com.prestoxbasopp.core.ast.XbFunctionDeclaration
import com.prestoxbasopp.core.ast.XbIdentifierExpression
import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbIndexExpression
import com.prestoxbasopp.core.ast.XbLiteralExpression
import com.prestoxbasopp.core.ast.XbLiteralKind
import com.prestoxbasopp.core.ast.XbLocalDeclarationStatement
import com.prestoxbasopp.core.ast.XbPrintStatement
import com.prestoxbasopp.core.ast.XbProcedureDeclaration
import com.prestoxbasopp.core.ast.XbProgram
import com.prestoxbasopp.core.ast.XbReturnStatement
import com.prestoxbasopp.core.ast.XbStatement
import com.prestoxbasopp.core.ast.XbUnaryExpression
import com.prestoxbasopp.core.ast.XbWhileStatement
import com.prestoxbasopp.core.parser.XbParser
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.datatransfer.StringSelection
import com.google.gson.GsonBuilder
import java.util.regex.Pattern
import javax.swing.JButton
import javax.swing.JTabbedPane
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class XbAstToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = XbAstPanel()
        val controller = XbAstToolWindowController(project, panel)
        val content = com.intellij.ui.content.ContentFactory.getInstance().createContent(panel.component, "", false)
        content.setDisposer(controller)
        toolWindow.contentManager.addContent(content)
    }
}

data class XbAstTreeNode(
    val label: String,
    val children: List<XbAstTreeNode> = emptyList(),
)

data class XbAstPresentation(
    val root: XbAstTreeNode?,
    val message: String?,
    val parserErrors: List<XbParserErrorEntry> = emptyList(),
)

data class XbParserErrorEntry(
    val message: String,
    val offset: Int?,
    val line: Int?,
    val column: Int?,
    val evidence: XbParserErrorEvidence?,
)

data class XbParserErrorEvidence(
    val startLine: Int,
    val endLine: Int,
    val focusLine: Int,
    val lines: List<String>,
)

class XbParserErrorReportBuilder(
    private val contextLines: Int = 10,
) {
    private val offsetPattern = Pattern.compile("\\bat\\s+(\\d+)\\b")

    fun build(text: String, errors: List<String>): List<XbParserErrorEntry> {
        if (errors.isEmpty()) {
            return emptyList()
        }
        val locator = XbSourceLineLocator(text)
        return errors.map { error ->
            val offset = parseOffset(error)
            val location = offset?.let { locator.resolve(it) }
            val evidence = location?.let { lineInfo ->
                buildEvidence(locator.lines, lineInfo.line)
            }
            XbParserErrorEntry(
                message = error,
                offset = offset,
                line = location?.line,
                column = location?.column,
                evidence = evidence,
            )
        }
    }

    fun formatForDisplay(entries: List<XbParserErrorEntry>): String {
        if (entries.isEmpty()) {
            return "No parser errors."
        }
        return entries.joinToString("\n\n") { entry ->
            val location = when {
                entry.line != null && entry.column != null -> "line ${entry.line}, column ${entry.column}"
                entry.offset != null -> "offset ${entry.offset}"
                else -> "unknown location"
            }
            val header = "• ${entry.message} [$location]"
            val evidence = entry.evidence?.lines
                ?.mapIndexed { index, line ->
                    val lineNumber = entry.evidence.startLine + index
                    val marker = if (lineNumber == entry.evidence.focusLine) ">" else " "
                    "$marker ${lineNumber.toString().padStart(4, ' ')} | $line"
                }
                ?.joinToString("\n")
            if (evidence == null) {
                header
            } else {
                "$header\n$evidence"
            }
        }
    }

    fun toPrettyJson(entries: List<XbParserErrorEntry>): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(entries)
    }

    private fun parseOffset(error: String): Int? {
        val matcher = offsetPattern.matcher(error)
        if (!matcher.find()) {
            return null
        }
        return matcher.group(1)?.toIntOrNull()
    }

    private fun buildEvidence(lines: List<String>, focusLine: Int): XbParserErrorEvidence {
        val startLine = (focusLine - contextLines).coerceAtLeast(1)
        val endLine = (focusLine + contextLines).coerceAtMost(lines.size)
        return XbParserErrorEvidence(
            startLine = startLine,
            endLine = endLine,
            focusLine = focusLine,
            lines = lines.subList(startLine - 1, endLine),
        )
    }
}

private class XbSourceLineLocator(text: String) {
    val lines: List<String> = text.split("\n")
    private val lineStarts: IntArray = buildLineStarts(text)

    fun resolve(offset: Int): XbSourceLocation {
        if (lineStarts.isEmpty()) {
            return XbSourceLocation(line = 1, column = 1)
        }
        val clampedOffset = offset.coerceIn(0, (lineStarts.last() + lines.last().length).coerceAtLeast(0))
        var lineIndex = lineStarts.binarySearch(clampedOffset)
        if (lineIndex < 0) {
            lineIndex = -lineIndex - 2
        }
        if (lineIndex < 0) {
            lineIndex = 0
        }
        val lineStart = lineStarts[lineIndex]
        val column = (clampedOffset - lineStart) + 1
        return XbSourceLocation(line = lineIndex + 1, column = column)
    }

    private fun buildLineStarts(text: String): IntArray {
        if (text.isEmpty()) {
            return intArrayOf(0)
        }
        val starts = mutableListOf(0)
        text.forEachIndexed { index, c ->
            if (c == '\n' && index + 1 < text.length) {
                starts += (index + 1)
            }
        }
        return starts.toIntArray()
    }
}

private data class XbSourceLocation(
    val line: Int,
    val column: Int,
)

class XbAstPresenter {
    private val parserErrorReportBuilder = XbParserErrorReportBuilder()

    fun present(fileName: String?, text: String?): XbAstPresentation {
        if (text == null) {
            return XbAstPresentation(null, "No active editor.")
        }
        val parseResult = XbParser.parse(text)
        val program = parseResult.program
        val root = program?.let { buildProgramNode(it) }
        val message = buildMessage(fileName, parseResult.errors.size)
        val parserErrors = parserErrorReportBuilder.build(text, parseResult.errors)
        return XbAstPresentation(root, message, parserErrors)
    }

    private fun buildMessage(fileName: String?, errorCount: Int): String {
        val label = fileName?.let { "File: $it" } ?: "File: (unsaved)"
        return "$label — ${formatErrorSummary(errorCount)}"
    }

    private fun formatErrorSummary(errorCount: Int): String {
        return when (errorCount) {
            0 -> "Parser errors: none"
            1 -> "Parser error: 1"
            else -> "Parser errors: $errorCount"
        }
    }

    private fun buildProgramNode(program: XbProgram): XbAstTreeNode {
        return XbAstTreeNode(
            label = "File",
            children = program.statements.map { buildStatementNode(it) },
        )
    }

    private fun buildStatementNode(statement: XbStatement): XbAstTreeNode {
        return when (statement) {
            is XbExpressionStatement -> XbAstTreeNode(
                label = "Expression",
                children = listOf(buildExpressionNode(statement.expression)),
            )
            is XbAssignmentStatement -> XbAstTreeNode(
                label = "Assignment",
                children = listOf(
                    buildExpressionNode(statement.target),
                    buildExpressionNode(statement.value),
                ),
            )
            is XbPrintStatement -> XbAstTreeNode(
                label = "Print",
                children = statement.expressions.map { buildExpressionNode(it) },
            )
            is XbLocalDeclarationStatement -> XbAstTreeNode(
                label = "Local",
                children = statement.bindings.map { binding ->
                    XbAstTreeNode(
                        label = "Binding: ${binding.name}",
                        children = binding.initializer?.let { listOf(buildExpressionNode(it)) } ?: emptyList(),
                    )
                },
            )
            is XbReturnStatement -> XbAstTreeNode(
                label = "Return",
                children = statement.expression?.let { listOf(buildExpressionNode(it)) } ?: emptyList(),
            )
            is XbIfStatement -> XbAstTreeNode(
                label = "If",
                children = buildList {
                    add(buildExpressionNode(statement.condition))
                    add(buildBlockNode(statement.thenBlock, "Then"))
                    val elseBlock = statement.elseBlock ?: XbBlock(emptyList(), statement.thenBlock.range)
                    add(buildBlockNode(elseBlock, "Else"))
                },
            )
            is XbWhileStatement -> XbAstTreeNode(
                label = "While",
                children = listOf(
                    buildExpressionNode(statement.condition),
                    buildBlockNode(statement.body, "Body"),
                ),
            )
            is XbForStatement -> XbAstTreeNode(
                label = "For",
                children = buildList {
                    add(buildExpressionNode(statement.iterator))
                    add(buildExpressionNode(statement.start))
                    add(buildExpressionNode(statement.end))
                    add(buildExpressionNode(statement.step))
                    if (statement.body.statements.isNotEmpty()) {
                        add(buildBlockNode(statement.body, "Body"))
                    }
                },
            )
            is XbFunctionDeclaration -> XbAstTreeNode(
                label = "Function: ${statement.name}",
                children = listOf(
                    XbAstTreeNode(
                        label = "Params",
                        children = statement.parameters.map { param ->
                            XbAstTreeNode(label = "Param: $param")
                        },
                    ),
                    buildBlockNode(statement.body, "Body"),
                ),
            )
            is XbProcedureDeclaration -> XbAstTreeNode(
                label = "Procedure: ${statement.name}",
                children = listOf(
                    XbAstTreeNode(
                        label = "Params",
                        children = statement.parameters.map { param ->
                            XbAstTreeNode(label = "Param: $param")
                        },
                    ),
                    buildBlockNode(statement.body, "Body"),
                ),
            )
            is XbBlock -> buildBlockNode(statement, "Block")
            else -> XbAstTreeNode(label = "Unknown Statement")
        }
    }

    private fun buildBlockNode(block: XbBlock, label: String): XbAstTreeNode {
        return XbAstTreeNode(
            label = label,
            children = block.statements.map { buildStatementNode(it) },
        )
    }

    private fun buildExpressionNode(expression: XbExpression): XbAstTreeNode {
        return when (expression) {
            is XbIdentifierExpression -> XbAstTreeNode(label = "Identifier: ${expression.name}")
            is XbLiteralExpression -> XbAstTreeNode(label = formatLiteral(expression))
            is XbUnaryExpression -> XbAstTreeNode(
                label = "Unary (${expression.operator})",
                children = listOf(buildExpressionNode(expression.expression)),
            )
            is XbBinaryExpression -> XbAstTreeNode(
                label = "Binary (${expression.operator})",
                children = listOf(
                    buildExpressionNode(expression.left),
                    buildExpressionNode(expression.right),
                ),
            )
            is XbCallExpression -> XbAstTreeNode(
                label = "Call",
                children = listOf(buildExpressionNode(expression.callee)) +
                    expression.arguments.map { buildExpressionNode(it) },
            )
            is XbIndexExpression -> XbAstTreeNode(
                label = "Index",
                children = listOf(
                    buildExpressionNode(expression.target),
                    buildExpressionNode(expression.index),
                ),
            )
            is XbArrayLiteralExpression -> XbAstTreeNode(
                label = "Array",
                children = expression.elements.map { buildExpressionNode(it) },
            )
            else -> XbAstTreeNode(label = "Unknown Expression")
        }
    }

    private fun formatLiteral(expression: XbLiteralExpression): String {
        return when (expression.kind) {
            XbLiteralKind.NUMBER -> "Literal (Number): ${expression.value}"
            XbLiteralKind.STRING -> "Literal (String): ${expression.value}"
            XbLiteralKind.NIL -> "Literal (Nil)"
            XbLiteralKind.BOOLEAN -> "Literal (Boolean): ${expression.value}"
        }
    }
}

class XbAstPanel {
    private val messageLabel = JBLabel()
    private val astButtonPanel = JPanel(BorderLayout())
    private val copyAstButton = JButton("Copy AST").apply {
        isEnabled = false
    }
    private val copyErrorsJsonButton = JButton("Copy Errors JSON").apply {
        isEnabled = false
    }
    private val tree = Tree(DefaultMutableTreeNode("File")).apply {
        isRootVisible = true
        showsRootHandles = true
        emptyText.text = "No AST to display."
    }
    private val parserErrorsArea = JBTextArea().apply {
        isEditable = false
        lineWrap = false
    }
    private val tabs = JTabbedPane().apply {
        addTab("AST", ScrollPaneFactory.createScrollPane(tree))
        addTab("Parser Errors", ScrollPaneFactory.createScrollPane(parserErrorsArea))
    }
    private val buttonCards = JPanel(CardLayout()).apply {
        add(astButtonPanel, "AST")
        add(copyErrorsJsonButton, "Parser Errors")
    }
    val component: JPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(8)
        val header = JPanel(BorderLayout()).apply {
            add(messageLabel, BorderLayout.CENTER)
            add(buttonCards, BorderLayout.EAST)
        }
        add(header, BorderLayout.NORTH)
        add(tabs, BorderLayout.CENTER)
    }
    private val textFormatter = XbAstTextFormatter()
    private val parserErrorReportBuilder = XbParserErrorReportBuilder()
    private var latestPresentation: XbAstPresentation? = null

    init {
        astButtonPanel.add(copyAstButton, BorderLayout.EAST)
        TreeSpeedSearch(tree)
        copyAstButton.addActionListener { copyAstToClipboard() }
        copyErrorsJsonButton.addActionListener { copyErrorsJsonToClipboard() }
        tabs.addChangeListener {
            val selectedTitle = tabs.getTitleAt(tabs.selectedIndex)
            (buttonCards.layout as CardLayout).show(buttonCards, selectedTitle)
        }
    }

    fun render(presentation: XbAstPresentation) {
        latestPresentation = presentation
        messageLabel.text = presentation.message ?: ""
        messageLabel.isVisible = presentation.message != null
        copyAstButton.isEnabled = presentation.root != null
        copyErrorsJsonButton.isEnabled = presentation.parserErrors.isNotEmpty()
        val rootNode = presentation.root?.toSwingNode() ?: DefaultMutableTreeNode("File")
        tree.model = DefaultTreeModel(rootNode)
        TreeUtil.expandAll(tree)
        parserErrorsArea.text = parserErrorReportBuilder.formatForDisplay(presentation.parserErrors)
        tree.emptyText.text = if (presentation.root == null) {
            "No AST to display."
        } else {
            ""
        }
    }

    private fun copyAstToClipboard() {
        val root = latestPresentation?.root ?: return
        val text = textFormatter.format(root)
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    private fun copyErrorsJsonToClipboard() {
        val errors = latestPresentation?.parserErrors ?: return
        val json = parserErrorReportBuilder.toPrettyJson(errors)
        CopyPasteManager.getInstance().setContents(StringSelection(json))
    }

    private fun XbAstTreeNode.toSwingNode(): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode(label)
        children.forEach { child ->
            node.add(child.toSwingNode())
        }
        return node
    }
}

class XbAstTextFormatter {
    fun format(root: XbAstTreeNode): String {
        val lines = mutableListOf<String>()
        appendNode(root, "", lines)
        return lines.joinToString("\n")
    }

    private fun appendNode(node: XbAstTreeNode, indent: String, lines: MutableList<String>) {
        lines.add("$indent${node.label}")
        val nextIndent = "$indent  "
        node.children.forEach { child ->
            appendNode(child, nextIndent, lines)
        }
    }
}

private class XbAstToolWindowController(
    private val project: Project,
    private val panel: XbAstPanel,
    private val presenter: XbAstPresenter = XbAstPresenter(),
) : Disposable {
    private val connection = project.messageBus.connect(this)
    private var activeDocument: Document? = null
    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            refreshFromCurrentEditor()
        }
    }

    init {
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                refreshFromCurrentEditor()
            }
        })
        refreshFromCurrentEditor()
    }

    override fun dispose() {
        activeDocument?.removeDocumentListener(documentListener)
        activeDocument = null
    }

    private fun refreshFromCurrentEditor() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        updateDocumentListener(editor?.document)
        val presentation = buildPresentation(file, editor)
        panel.render(presentation)
    }

    private fun updateDocumentListener(document: Document?) {
        if (activeDocument == document) {
            return
        }
        activeDocument?.removeDocumentListener(documentListener)
        activeDocument = document
        activeDocument?.addDocumentListener(documentListener)
    }

    private fun buildPresentation(file: VirtualFile?, editor: Editor?): XbAstPresentation {
        return ApplicationManager.getApplication().runReadAction<XbAstPresentation> {
            val fileName = file?.name
            val text = editor?.document?.text
            presenter.present(fileName, text)
        }
    }
}
