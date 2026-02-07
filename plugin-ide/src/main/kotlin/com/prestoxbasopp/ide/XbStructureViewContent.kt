package com.prestoxbasopp.ide

data class XbStructureViewFileContent(
    val fileName: String,
    val text: String,
)

class XbStructureViewFileContentResolver {
    fun resolve(fileName: String, psiText: String, editorText: String?): XbStructureViewFileContent {
        val resolvedText = editorText ?: psiText
        return XbStructureViewFileContent(
            fileName = fileName,
            text = resolvedText,
        )
    }
}

class XbStructureViewRootBuilder(
    private val snapshotBuilder: XbPsiTextBuilder = XbPsiTextBuilder(),
    private val structureViewBuilder: XbStructureViewBuilder = XbStructureViewBuilder(),
) {
    fun buildRoot(content: XbStructureViewFileContent): XbStructureItem {
        val snapshot = snapshotBuilder.buildSnapshot(content.text, content.fileName)
        return structureViewBuilder.build(snapshot)
    }
}
