package com.prestoxbasopp.core.psi

import com.prestoxbasopp.core.api.XbTextRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbPsiIntegrityValidatorTest {
    private val validator = XbPsiIntegrityValidator()

    @Test
    fun `valid tree passes validation`() {
        val tree = sampleTree()

        val violations = validator.validate(tree)

        assertThat(violations).isEmpty()
    }

    @Test
    fun `text length mismatch is reported`() {
        val element = XbPsiLiteral(
            literalKind = "number",
            textRange = XbTextRange(0, 2),
            text = "1",
        )

        val violations = validator.validate(element)

        assertThat(violations).anySatisfy { violation ->
            assertThat(violation.message).contains("Text length does not match")
        }
    }

    @Test
    fun `blank symbol names are rejected`() {
        val element = XbPsiVariableDeclaration(
            symbolName = " ",
            isMutable = true,
            textRange = XbTextRange(0, 1),
            text = "x",
        )

        val violations = validator.validate(element)

        assertThat(violations).anySatisfy { violation ->
            assertThat(violation.message).contains("Symbol name must be non-blank")
        }
    }

    @Test
    fun `duplicate parameters are rejected`() {
        val element = XbPsiFunctionDeclaration(
            symbolName = "dup",
            parameters = listOf("a", "a"),
            textRange = XbTextRange(0, 0),
            text = "",
        )

        val violations = validator.validate(element)

        assertThat(violations).anySatisfy { violation ->
            assertThat(violation.message).contains("Function parameters must be unique")
        }
    }

    @Test
    fun `child text mismatch is reported`() {
        val child = XbPsiLiteral(
            literalKind = "number",
            textRange = XbTextRange(0, 1),
            text = "1",
        )
        val parent = XbPsiBlock(
            textRange = XbTextRange(0, 1),
            text = "2",
            children = listOf(child),
        )

        val violations = validator.validate(parent)

        assertThat(violations).anySatisfy { violation ->
            assertThat(violation.message).contains("Parent text does not match")
        }
    }

    @Test
    fun `parent pointer mismatches are reported`() {
        val child = XbPsiLiteral(
            literalKind = "number",
            textRange = XbTextRange(0, 1),
            text = "1",
        )
        val parent = XbPsiBlock(
            textRange = XbTextRange(0, 1),
            text = "1",
            children = listOf(child),
        )
        child.parent = null

        val violations = validator.validate(parent)

        assertThat(violations).anySatisfy { violation ->
            assertThat(violation.message).contains("Child parent pointer mismatch")
        }
    }

    @Test
    fun `child range outside parent is reported`() {
        val child = XbPsiLiteral(
            literalKind = "number",
            textRange = XbTextRange(0, 2),
            text = "10",
        )
        val parent = XbPsiBlock(
            textRange = XbTextRange(0, 1),
            text = "1",
            children = listOf(child),
        )

        val violations = validator.validate(parent)

        assertThat(violations).anySatisfy { violation ->
            assertThat(violation.message).contains("Child range outside parent range")
        }
    }

    @Test
    fun `child overlap is reported`() {
        val first = XbPsiLiteral(
            literalKind = "number",
            textRange = XbTextRange(0, 2),
            text = "10",
        )
        val second = XbPsiLiteral(
            literalKind = "number",
            textRange = XbTextRange(1, 2),
            text = "0",
        )
        val parent = XbPsiBlock(
            textRange = XbTextRange(0, 2),
            text = "10",
            children = listOf(first, second),
        )

        val violations = validator.validate(parent)

        assertThat(violations).anySatisfy { violation ->
            assertThat(violation.message).contains("Child ranges overlap")
        }
    }

    private fun sampleTree(): XbPsiElement {
        val literal = XbPsiLiteral(
            literalKind = "number",
            textRange = XbTextRange(0, 1),
            text = "1",
        )
        val reference = XbPsiSymbolReference(
            symbolName = "foo",
            textRange = XbTextRange(1, 4),
            text = "foo",
        )
        val block = XbPsiBlock(
            textRange = XbTextRange(0, 4),
            text = "1foo",
            children = listOf(literal, reference),
        )
        return XbPsiFunctionDeclaration(
            symbolName = "doWork",
            parameters = listOf("x"),
            textRange = XbTextRange(0, 4),
            text = "1foo",
            children = listOf(block),
        )
    }
}
