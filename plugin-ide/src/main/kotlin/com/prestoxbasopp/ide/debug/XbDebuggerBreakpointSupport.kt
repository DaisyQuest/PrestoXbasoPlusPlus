package com.prestoxbasopp.ide.debug

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import java.io.File

data class XbDebuggerLineBreakpoint(
    val filePath: String,
    val lineOneBased: Int,
)

internal object XbDebuggerBreakpointCollector {
    fun collect(project: Project): List<XbDebuggerLineBreakpoint> {
        val allBreakpoints = XDebuggerManager.getInstance(project).breakpointManager.allBreakpoints
        return allBreakpoints
            .filterIsInstance<XLineBreakpoint<*>>()
            .mapNotNull { breakpoint ->
                val fileUrl = breakpoint.fileUrl ?: return@mapNotNull null
                val path = FileUtil.toSystemDependentName(fileUrl.removePrefix("file://"))
                val extension = File(path).extension.lowercase()
                if (extension != "prg" && extension != "xb") return@mapNotNull null
                XbDebuggerLineBreakpoint(path, breakpoint.line + 1)
            }
            .distinct()
            .sortedWith(compareBy({ it.filePath.lowercase() }, { it.lineOneBased }))
    }
}

internal data class XbSourceLocation(val filePath: String, val lineOneBased: Int)

internal object XbDebuggerOutputParser {
    private val patterns: List<Regex> = listOf(
        Regex("""BREAKPOINT\s+(.+):(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""Stopped\s+at\s+(.+)\((\d+)\)""", RegexOption.IGNORE_CASE),
        Regex("""at\s+(.+):(\d+)""", RegexOption.IGNORE_CASE),
    )

    fun parseLocation(line: String): XbSourceLocation? {
        val trimmed = line.trim()
        for (pattern in patterns) {
            val match = pattern.find(trimmed) ?: continue
            val path = match.groupValues[1].trim().removeSurrounding("\"")
            val lineNumber = match.groupValues[2].toIntOrNull() ?: continue
            if (lineNumber < 1 || path.isBlank()) continue
            return XbSourceLocation(path, lineNumber)
        }
        return null
    }
}

internal class XbDebuggerSourceResolver(
    private val project: Project,
    private val workingDirectory: String?,
    sourcePath: String,
) {
    private val sourceRoots = sourcePath
        .split(';')
        .map { it.trim() }
        .filter { it.isNotBlank() }

    fun resolve(path: String): VirtualFile? {
        val fileSystem = LocalFileSystem.getInstance()
        val normalized = FileUtil.toSystemIndependentName(path)

        if (File(path).isAbsolute) {
            return fileSystem.findFileByPath(normalized)
        }

        workingDirectory?.let { wd ->
            val candidate = FileUtil.toSystemIndependentName(File(wd, path).path)
            fileSystem.findFileByPath(candidate)?.let { return it }
        }

        for (root in sourceRoots) {
            val candidate = FileUtil.toSystemIndependentName(File(root, path).path)
            fileSystem.findFileByPath(candidate)?.let { return it }
        }

        project.basePath?.let { basePath ->
            val candidate = FileUtil.toSystemIndependentName(File(basePath, path).path)
            fileSystem.findFileByPath(candidate)?.let { return it }
        }

        return null
    }
}

internal class XbDebuggerSourceNavigationFilter(
    private val project: Project,
    private val resolver: XbDebuggerSourceResolver,
) : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val location = XbDebuggerOutputParser.parseLocation(line) ?: return null
        val file = resolver.resolve(location.filePath) ?: return null

        val lineStart = entireLength - line.length
        val highlightEnd = (lineStart + line.length).coerceAtLeast(lineStart)
        val hyperlink = HyperlinkInfo {
            OpenFileDescriptor(project, file, (location.lineOneBased - 1).coerceAtLeast(0), 0).navigate(true)
        }

        return Filter.Result(lineStart, highlightEnd, hyperlink)
    }
}
