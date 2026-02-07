package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiElementType
import com.prestoxbasopp.core.psi.XbPsiSnapshot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbTypeMigrationRefactoringTest {
    @Test
    fun `migrates number literals to strings`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 3),
            text = "123",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.LITERAL,
                    name = null,
                    literalKind = "number",
                    textRange = XbTextRange(0, 3),
                    text = "123",
                ),
                XbPsiSnapshot(
                    elementType = XbPsiElementType.LITERAL,
                    name = null,
                    literalKind = "string",
                    textRange = XbTextRange(3, 8),
                    text = "\"stay\"",
                ),
            ),
        )

        val result = XbTypeMigrationRefactoring().migrate(snapshot, "number", "string")

        assertThat(result.errors).isEmpty()
        assertThat(result.edits).hasSize(1)
        assertThat(result.updatedSnapshot.children[0].text).isEqualTo("\"123\"")
        assertThat(result.updatedSnapshot.children[0].literalKind).isEqualTo("string")
        assertThat(result.updatedSnapshot.children[1].text).isEqualTo("\"stay\"")
        assertThat(result.updatedSnapshot.children[1].literalKind).isEqualTo("string")
    }

    @Test
    fun `migrates quoted string literals to numbers`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 4),
            text = "\"42\"",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.LITERAL,
                    name = null,
                    literalKind = "string",
                    textRange = XbTextRange(0, 4),
                    text = "\"42\"",
                ),
            ),
        )

        val result = XbTypeMigrationRefactoring().migrate(snapshot, "string", "number")

        assertThat(result.errors).isEmpty()
        assertThat(result.edits).hasSize(1)
        assertThat(result.updatedSnapshot.children[0].text).isEqualTo("42")
        assertThat(result.updatedSnapshot.children[0].literalKind).isEqualTo("number")
    }

    @Test
    fun `reports errors for invalid numeric conversion`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 5),
            text = "\"abc\"",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.LITERAL,
                    name = null,
                    literalKind = "string",
                    textRange = XbTextRange(0, 5),
                    text = "\"abc\"",
                ),
            ),
        )

        val result = XbTypeMigrationRefactoring().migrate(snapshot, "string", "number")

        assertThat(result.errors).containsExactly("Literal '\"abc\"' is not a valid number literal.")
        assertThat(result.edits).isEmpty()
        assertThat(result.updatedSnapshot.children[0].literalKind).isEqualTo("string")
    }

    @Test
    fun `migrates booleans to numbers`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 9),
            text = "truefalse",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.LITERAL,
                    name = null,
                    literalKind = "boolean",
                    textRange = XbTextRange(0, 4),
                    text = "true",
                ),
                XbPsiSnapshot(
                    elementType = XbPsiElementType.LITERAL,
                    name = null,
                    literalKind = "boolean",
                    textRange = XbTextRange(4, 9),
                    text = "false",
                ),
            ),
        )

        val result = XbTypeMigrationRefactoring().migrate(snapshot, "boolean", "number")

        assertThat(result.errors).isEmpty()
        assertThat(result.edits).hasSize(2)
        assertThat(result.updatedSnapshot.children.map { it.text }).containsExactly("1", "0")
    }

    @Test
    fun `rejects numeric to boolean conversion outside zero or one`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 1),
            text = "2",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.LITERAL,
                    name = null,
                    literalKind = "number",
                    textRange = XbTextRange(0, 1),
                    text = "2",
                ),
            ),
        )

        val result = XbTypeMigrationRefactoring().migrate(snapshot, "number", "boolean")

        assertThat(result.errors).containsExactly("Numeric literal '2' cannot be converted to boolean.")
        assertThat(result.edits).isEmpty()
        assertThat(result.updatedSnapshot.children[0].literalKind).isEqualTo("number")
    }

    @Test
    fun `project migration aggregates edits and errors`() {
        val first = XbTypeMigrationTarget(
            sourceId = "file-1",
            snapshot = XbPsiSnapshot(
                elementType = XbPsiElementType.FILE,
                name = "root",
                textRange = XbTextRange(0, 4),
                text = "\"1\"",
                children = listOf(
                    XbPsiSnapshot(
                        elementType = XbPsiElementType.LITERAL,
                        name = null,
                        literalKind = "string",
                        textRange = XbTextRange(0, 3),
                        text = "\"1\"",
                    ),
                ),
            ),
        )
        val second = XbTypeMigrationTarget(
            sourceId = "file-2",
            snapshot = XbPsiSnapshot(
                elementType = XbPsiElementType.FILE,
                name = "root",
                textRange = XbTextRange(0, 5),
                text = "\"bad\"",
                children = listOf(
                    XbPsiSnapshot(
                        elementType = XbPsiElementType.LITERAL,
                        name = null,
                        literalKind = "string",
                        textRange = XbTextRange(0, 5),
                        text = "\"bad\"",
                    ),
                ),
            ),
        )

        val result = XbTypeMigrationRefactoring().migrateProject(listOf(first, second), "string", "number")

        assertThat(result.edits).hasSize(1)
        assertThat(result.errors).containsExactly("file-2: Literal '\"bad\"' is not a valid number literal.")
        assertThat(result.updatedTargets[0].snapshot.children[0].text).isEqualTo("1")
        assertThat(result.updatedTargets[1].snapshot.children[0].text).isEqualTo("\"bad\"")
    }

    @Test
    fun `no ops when migrating to the same type`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 4),
            text = "true",
        )

        val result = XbTypeMigrationRefactoring().migrate(snapshot, "boolean", "boolean")

        assertThat(result.errors).isEmpty()
        assertThat(result.edits).isEmpty()
    }

    @Test
    fun `rejects blank migration types`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 4),
            text = "true",
        )

        val result = XbTypeMigrationRefactoring().migrate(snapshot, " ", "number")

        assertThat(result.errors).containsExactly("Type migration requires non-blank source and target types.")
        assertThat(result.edits).isEmpty()
    }
}
