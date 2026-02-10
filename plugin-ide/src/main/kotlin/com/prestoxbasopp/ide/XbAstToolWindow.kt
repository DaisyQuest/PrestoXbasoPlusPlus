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
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLabel
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
)

class XbAstPresenter {
    fun present(fileName: String?, text: String?): XbAstPresentation {
        if (text == null) {
            return XbAstPresentation(null, "No active editor.")
        }
        val parseResult = XbParser.parse(text)
        val program = parseResult.program
        val root = program?.let { buildProgramNode(it) }
        val message = buildMessage(fileName, parseResult.errors.size)
        return XbAstPresentation(root, message)
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
    private val tree = Tree(DefaultMutableTreeNode("File")).apply {
        isRootVisible = true
        showsRootHandles = true
        emptyText.text = "No AST to display."
    }
    val component: JPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(8)
        add(messageLabel, BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(tree), BorderLayout.CENTER)
    }

    init {
        TreeSpeedSearch(tree)
    }

    fun render(presentation: XbAstPresentation) {
        messageLabel.text = presentation.message ?: ""
        messageLabel.isVisible = presentation.message != null
        val rootNode = presentation.root?.toSwingNode() ?: DefaultMutableTreeNode("File")
        tree.model = DefaultTreeModel(rootNode)
        TreeUtil.expandAll(tree)
        tree.emptyText.text = if (presentation.root == null) {
            "No AST to display."
        } else {
            ""
        }
    }

    private fun XbAstTreeNode.toSwingNode(): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode(label)
        children.forEach { child ->
            node.add(child.toSwingNode())
        }
        return node
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
