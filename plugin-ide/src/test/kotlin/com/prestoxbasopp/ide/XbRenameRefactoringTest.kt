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
}
