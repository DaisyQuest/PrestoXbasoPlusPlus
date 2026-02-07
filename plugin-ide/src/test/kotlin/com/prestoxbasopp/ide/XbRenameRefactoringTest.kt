package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiElementType
import com.prestoxbasopp.core.psi.XbPsiSnapshot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbRenameRefactoringTest {
    @Test
    fun `renames symbols and records edits`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 50),
            text = "file",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.FUNCTION_DECLARATION,
                    name = "foo",
                    textRange = XbTextRange(0, 10),
                    text = "function foo()",
                ),
                XbPsiSnapshot(
                    elementType = XbPsiElementType.SYMBOL_REFERENCE,
                    name = "foo",
                    textRange = XbTextRange(11, 14),
                    text = "foo",
                ),
            ),
        )

        val result = XbRenameRefactoring().rename(snapshot, "foo", "bar")

        assertThat(result.errors).isEmpty()
        assertThat(result.edits).hasSize(2)
        assertThat(result.updatedSnapshot.children.map { it.name }).containsExactly("bar", "bar")
    }

    @Test
    fun `rejects blank new name`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 10),
            text = "file",
        )

        val result = XbRenameRefactoring().rename(snapshot, "foo", " ")

        assertThat(result.errors).containsExactly("New name must not be blank.")
        assertThat(result.edits).isEmpty()
    }

    @Test
    fun `no ops when name is unchanged`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 10),
            text = "file",
        )

        val result = XbRenameRefactoring().rename(snapshot, "foo", "foo")

        assertThat(result.errors).isEmpty()
        assertThat(result.edits).isEmpty()
    }

    @Test
    fun `renames symbols across project scope`() {
        val fileOne = XbRenameTarget(
            sourceId = "file-1",
            snapshot = XbPsiSnapshot(
                elementType = XbPsiElementType.FILE,
                name = "fileOne",
                textRange = XbTextRange(0, 20),
                text = "file",
                children = listOf(
                    XbPsiSnapshot(
                        elementType = XbPsiElementType.FUNCTION_DECLARATION,
                        name = "foo",
                        textRange = XbTextRange(0, 10),
                        text = "function foo()",
                    ),
                ),
            ),
        )
        val fileTwo = XbRenameTarget(
            sourceId = "file-2",
            snapshot = XbPsiSnapshot(
                elementType = XbPsiElementType.FILE,
                name = "fileTwo",
                textRange = XbTextRange(0, 30),
                text = "file",
                children = listOf(
                    XbPsiSnapshot(
                        elementType = XbPsiElementType.SYMBOL_REFERENCE,
                        name = "foo",
                        textRange = XbTextRange(15, 18),
                        text = "foo",
                    ),
                ),
            ),
        )

        val result = XbRenameRefactoring().renameProject(listOf(fileOne, fileTwo), "foo", "bar")

        assertThat(result.errors).isEmpty()
        assertThat(result.edits).hasSize(2)
        assertThat(result.edits.map { it.sourceId }).containsExactly("file-1", "file-2")
        assertThat(result.updatedTargets.map { it.snapshot.children.first().name }).containsExactly("bar", "bar")
    }

    @Test
    fun `project rename rejects blank name`() {
        val target = XbRenameTarget(
            sourceId = "file-1",
            snapshot = XbPsiSnapshot(
                elementType = XbPsiElementType.FILE,
                name = "root",
                textRange = XbTextRange(0, 10),
                text = "file",
            ),
        )

        val result = XbRenameRefactoring().renameProject(listOf(target), "foo", " ")

        assertThat(result.errors).containsExactly("New name must not be blank.")
        assertThat(result.edits).isEmpty()
    }

    @Test
    fun `project rename no ops when name is unchanged`() {
        val target = XbRenameTarget(
            sourceId = "file-1",
            snapshot = XbPsiSnapshot(
                elementType = XbPsiElementType.FILE,
                name = "root",
                textRange = XbTextRange(0, 10),
                text = "file",
            ),
        )

        val result = XbRenameRefactoring().renameProject(listOf(target), "foo", "foo")

        assertThat(result.errors).isEmpty()
        assertThat(result.edits).isEmpty()
        assertThat(result.updatedTargets).containsExactly(target)
    }
}
