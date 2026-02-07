package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.ast.XbBinaryExpression
import com.prestoxbasopp.core.ast.XbBlock
import com.prestoxbasopp.core.ast.XbExpression
import com.prestoxbasopp.core.ast.XbExpressionStatement
import com.prestoxbasopp.core.ast.XbIdentifierExpression
import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbLiteralExpression
import com.prestoxbasopp.core.ast.XbProgram
import com.prestoxbasopp.core.ast.XbReturnStatement
import com.prestoxbasopp.core.ast.XbStatement
import com.prestoxbasopp.core.ast.XbUnaryExpression
import com.prestoxbasopp.core.ast.XbWhileStatement
import com.prestoxbasopp.testframework.golden.AstDumpFormat
import com.prestoxbasopp.testframework.golden.AstDumpNode
import com.prestoxbasopp.testframework.golden.GoldenTestCase
import com.prestoxbasopp.testframework.golden.GoldenTestHarness
import com.prestoxbasopp.testframework.golden.ParseResult
import org.junit.jupiter.api.Test

class XbParserGoldenTest {
    @Test
    fun `parses expression precedence and control flow`() {
        val cases = listOf<GoldenTestCase<XbProgram>>(
            GoldenTestCase(
                id = "precedence-multiplication",
                source = "1 + 2 * 3;",
                expectedAst = """
                    Program
                      ExprStmt
                        Binary[op="+"]
                          Literal[kind=number, value=1]
                          Binary[op="*"]
                            Literal[kind=number, value=2]
                            Literal[kind=number, value=3]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "precedence-associativity",
                source = "1 - 2 - 3;",
                expectedAst = """
                    Program
                      ExprStmt
                        Binary[op=-]
                          Binary[op=-]
                            Literal[kind=number, value=1]
                            Literal[kind=number, value=2]
                          Literal[kind=number, value=3]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "unary-and-grouping",
                source = "(1 + 2) * -3;",
                expectedAst = """
                    Program
                      ExprStmt
                        Binary[op="*"]
                          Binary[op="+"]
                            Literal[kind=number, value=1]
                            Literal[kind=number, value=2]
                          Unary[op=-]
                            Literal[kind=number, value=3]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "if-else-return",
                source = "if x < 10 then return x; else return 0; endif",
                expectedAst = """
                    Program
                      IfStmt
                        Condition
                          Binary[op="<"]
                            Identifier[name=x]
                            Literal[kind=number, value=10]
                        Then
                          Block
                            ReturnStmt
                              Identifier[name=x]
                        Else
                          Block
                            ReturnStmt
                              Literal[kind=number, value=0]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "string-literal",
                source = "\"hi\" + name;",
                expectedAst = """
                    Program
                      ExprStmt
                        Binary[op="+"]
                          Literal[kind=string, value=hi]
                          Identifier[name=name]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "while-loop",
                source = "while n > 0 do n - 1; enddo",
                expectedAst = """
                    Program
                      WhileStmt
                        Condition
                          Binary[op=">"]
                            Identifier[name=n]
                            Literal[kind=number, value=0]
                        Body
                          Block
                            ExprStmt
                              Binary[op=-]
                                Identifier[name=n]
                                Literal[kind=number, value=1]
                """.trimIndent(),
            ),
        )

        GoldenTestHarness.assertCases(cases, ::parseSource, ::dumpProgram)
    }

    @Test
    fun `recovers from invalid tokens and missing terminators`() {
        val cases = listOf<GoldenTestCase<XbProgram>>(
            GoldenTestCase(
                id = "sync-on-semicolon",
                source = "return 1; @@@; return 2;",
                expectedAst = """
                    Program
                      ReturnStmt
                        Literal[kind=number, value=1]
                      ReturnStmt
                        Literal[kind=number, value=2]
                """.trimIndent(),
                expectedErrors = listOf("Unexpected token '@' at 10"),
            ),
            GoldenTestCase(
                id = "missing-endif",
                source = "if 1 then return 2;",
                expectedAst = """
                    Program
                      IfStmt
                        Condition
                          Literal[kind=number, value=1]
                        Then
                          Block
                            ReturnStmt
                              Literal[kind=number, value=2]
                """.trimIndent(),
                expectedErrors = listOf("Expected ENDIF to close IF at 19"),
            ),
            GoldenTestCase(
                id = "missing-then",
                source = "if 1 return 2; endif",
                expectedAst = """
                    Program
                      IfStmt
                        Condition
                          Literal[kind=number, value=1]
                        Then
                          Block
                            ReturnStmt
                              Literal[kind=number, value=2]
                """.trimIndent(),
                expectedErrors = listOf("Expected THEN after IF condition at 5"),
            ),
            GoldenTestCase(
                id = "missing-do",
                source = "while 1 return 2; enddo",
                expectedAst = """
                    Program
                      WhileStmt
                        Condition
                          Literal[kind=number, value=1]
                        Body
                          Block
                            ReturnStmt
                              Literal[kind=number, value=2]
                """.trimIndent(),
                expectedErrors = listOf("Expected DO after WHILE condition at 8"),
            ),
            GoldenTestCase(
                id = "missing-rparen",
                source = "(1 + 2;",
                expectedAst = """
                    Program
                      ExprStmt
                        Binary[op="+"]
                          Literal[kind=number, value=1]
                          Literal[kind=number, value=2]
                """.trimIndent(),
                expectedErrors = listOf("Expected ')' after expression at 6"),
            ),
        )

        GoldenTestHarness.assertCases(cases, ::parseSource, ::dumpProgram)
    }

    @Test
    fun `handles parser edge cases and recovery paths`() {
        val cases = listOf<GoldenTestCase<XbProgram>>(
            GoldenTestCase(
                id = "return-without-expression",
                source = "return;",
                expectedAst = """
                    Program
                      ReturnStmt
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "unary-missing-operand",
                source = "-;",
                expectedAst = """
                    Program
                      ExprStmt
                        Unary[op=-]
                          Identifier[name="<error>"]
                """.trimIndent(),
                expectedErrors = listOf(
                    "Unexpected token SEMICOLON at 1",
                    "Expected expression after unary '-' at 2",
                ),
            ),
            GoldenTestCase(
                id = "binary-missing-right-hand-side",
                source = "1 + ;",
                expectedAst = """
                    Program
                      ExprStmt
                        Binary[op="+"]
                          Literal[kind=number, value=1]
                          Identifier[name="<error>"]
                """.trimIndent(),
                expectedErrors = listOf(
                    "Unexpected token SEMICOLON at 4",
                    "Expected expression after '+' at 5",
                ),
            ),
            GoldenTestCase(
                id = "if-missing-condition",
                source = "if then return 1; endif",
                expectedAst = """
                    Program
                      IfStmt
                        Condition
                          Identifier[name="<error>"]
                        Then
                          Block
                """.trimIndent(),
                expectedErrors = listOf(
                    "Unexpected token THEN at 3",
                    "Expected condition after IF at 8",
                    "Expected THEN after IF condition at 18",
                ),
            ),
            GoldenTestCase(
                id = "unexpected-terminator",
                source = "endif return 1;",
                expectedAst = """
                    Program
                      ReturnStmt
                        Literal[kind=number, value=1]
                """.trimIndent(),
                expectedErrors = listOf("Unexpected ENDIF at 0"),
            ),
        )

        GoldenTestHarness.assertCases(cases, ::parseSource, ::dumpProgram)
    }

    @Test
    fun `ignores preprocessor directives when parsing`() {
        val cases = listOf<GoldenTestCase<XbProgram>>(
            GoldenTestCase(
                id = "define-directive",
                source = "#define FOO 1\nreturn FOO;",
                expectedAst = """
                    Program
                      ReturnStmt
                        Identifier[name=FOO]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "include-directive",
                source = "#include \"defs.ch\"\nif 1 then return 2; endif",
                expectedAst = """
                    Program
                      IfStmt
                        Condition
                          Literal[kind=number, value=1]
                        Then
                          Block
                            ReturnStmt
                              Literal[kind=number, value=2]
                """.trimIndent(),
            ),
        )

        GoldenTestHarness.assertCases(cases, ::parseSource, ::dumpProgram)
    }

    private fun parseSource(source: String): ParseResult<XbProgram> {
        val result = XbParser.parse(source)
        return ParseResult(result.program, result.errors)
    }

    private fun dumpProgram(program: XbProgram): String {
        return AstDumpFormat.render(program.toDumpNode())
    }
}

private fun XbProgram.toDumpNode(): AstDumpNode {
    return AstDumpNode(
        name = "Program",
        children = statements.map { it.toDumpNode() },
    )
}

private fun XbStatement.toDumpNode(): AstDumpNode {
    return when (this) {
        is XbExpressionStatement -> AstDumpNode(
            name = "ExprStmt",
            children = listOf(expression.toDumpNode()),
        )
        is XbReturnStatement -> AstDumpNode(
            name = "ReturnStmt",
            children = expression?.let { listOf(it.toDumpNode()) } ?: emptyList(),
        )
        is XbIfStatement -> AstDumpNode(
            name = "IfStmt",
            children = buildList {
                add(AstDumpNode(name = "Condition", children = listOf(condition.toDumpNode())))
                add(AstDumpNode(name = "Then", children = listOf(thenBlock.toDumpNode())))
                elseBlock?.let { elseNode ->
                    add(AstDumpNode(name = "Else", children = listOf(elseNode.toDumpNode())))
                }
            },
        )
        is XbWhileStatement -> AstDumpNode(
            name = "WhileStmt",
            children = listOf(
                AstDumpNode(name = "Condition", children = listOf(condition.toDumpNode())),
                AstDumpNode(name = "Body", children = listOf(body.toDumpNode())),
            ),
        )
        is XbBlock -> AstDumpNode(
            name = "Block",
            children = statements.map { it.toDumpNode() },
        )
        else -> AstDumpNode(name = "UnknownStatement")
    }
}

private fun XbExpression.toDumpNode(): AstDumpNode {
    return when (this) {
        is XbLiteralExpression -> AstDumpNode(
            name = "Literal",
            attributes = mapOf(
                "kind" to kind.name.lowercase(),
                "value" to value,
            ),
        )
        is XbIdentifierExpression -> AstDumpNode(
            name = "Identifier",
            attributes = mapOf("name" to name),
        )
        is XbUnaryExpression -> AstDumpNode(
            name = "Unary",
            attributes = mapOf("op" to operator),
            children = listOf(expression.toDumpNode()),
        )
        is XbBinaryExpression -> AstDumpNode(
            name = "Binary",
            attributes = mapOf("op" to operator),
            children = listOf(left.toDumpNode(), right.toDumpNode()),
        )
        else -> AstDumpNode(name = "UnknownExpression")
    }
}
