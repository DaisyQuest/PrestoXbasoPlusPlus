package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.ast.XbProgram
import com.prestoxbasopp.testframework.golden.AstDumpFormat
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
                    File
                      Expr.Binary.Add
                        Expr.Literal.Number[value=1]
                        Expr.Binary.Multiply
                          Expr.Literal.Number[value=2]
                          Expr.Literal.Number[value=3]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "precedence-associativity",
                source = "1 - 2 - 3;",
                expectedAst = """
                    File
                      Expr.Binary.Subtract
                        Expr.Binary.Subtract
                          Expr.Literal.Number[value=1]
                          Expr.Literal.Number[value=2]
                        Expr.Literal.Number[value=3]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "unary-and-grouping",
                source = "(1 + 2) * -3;",
                expectedAst = """
                    File
                      Expr.Binary.Multiply
                        Expr.Binary.Add
                          Expr.Literal.Number[value=1]
                          Expr.Literal.Number[value=2]
                        Expr.Unary.Negation
                          Expr.Literal.Number[value=3]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "if-else-return",
                source = "if x < 10 then return x; else return 0; endif",
                expectedAst = """
                    File
                      Stmt.If
                        Expr.Binary.LessThan
                          Expr.Identifier[name=x]
                          Expr.Literal.Number[value=10]
                        Block[branch=then]
                          Stmt.Return
                            Expr.Identifier[name=x]
                        Block[branch=else]
                          Stmt.Return
                            Expr.Literal.Number[value=0]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "string-literal",
                source = "\"hi\" + name;",
                expectedAst = """
                    File
                      Expr.Binary.Add
                        Expr.Literal.String[value=hi]
                        Expr.Identifier[name=name]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "while-loop",
                source = "while n > 0 do n - 1; enddo",
                expectedAst = """
                    File
                      Stmt.While
                        Expr.Binary.GreaterThan
                          Expr.Identifier[name=n]
                          Expr.Literal.Number[value=0]
                        Block
                          Stmt.Expression
                            Expr.Binary.Subtract
                              Expr.Identifier[name=n]
                              Expr.Literal.Number[value=1]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "function-and-procedure",
                source = """
                    FUNCTION LoadInventory()
                       LOCAL items := { "Mouse", "Keyboard" }
                       RETURN items
                    ENDFUNCTION

                    PROCEDURE InventoryReport()
                       LOCAL data := LoadInventory()
                       ? data[1]
                    ENDPROC
                """.trimIndent(),
                expectedAst = """
                    File
                      Decl.Function[name=LoadInventory]
                        Params
                        Block
                          Stmt.Local
                            Local.Binding[name=items]
                              Expr.ArrayLiteral
                                Expr.Literal.String[value=Mouse]
                                Expr.Literal.String[value=Keyboard]
                          Stmt.Return
                            Expr.Identifier[name=items]
                      Decl.Procedure[name=InventoryReport]
                        Params
                        Block
                          Stmt.Local
                            Local.Binding[name=data]
                              Expr.Call
                                Expr.Identifier[name=LoadInventory]
                          Stmt.Print
                            Expr.Index
                              Expr.Identifier[name=data]
                              Expr.Literal.Number[value=1]
                """.trimIndent(),
            ),
        )

        GoldenTestHarness.assertCases(cases, ::parseSource, ::dumpProgram)
    }

    @Test
    fun `parses inventory sample without spurious diagnostics`() {
        val cases = listOf<GoldenTestCase<XbProgram>>(
            GoldenTestCase(
                id = "inventory-sample",
                source = """
                    FUNCTION LoadInventory()
                       LOCAL items := { "Mouse", "Keyboard", "Monitor" }
                       LOCAL count := 0

                       WHILE count < Len(items)
                          ? "Loaded: " + items[count + 1]
                          count := count + 1
                       ENDDO

                    RETURN items
                    ENDFUNCTION


                    FUNCTION FindItem(items, query)
                       LOCAL index := 1

                       WHILE index <= Len(items)
                          IF items[index] == query
                             RETURN index
                          ENDIF
                          index := index + 1
                       ENDDO

                    RETURN 0
                    ENDFUNCTION


                    PROCEDURE InventoryReport()
                       LOCAL data := LoadInventory()
                       LOCAL target := "Keyboard"
                       LOCAL position := FindItem(data, target)

                       IF position == 0
                          ? "Item not found: " + target
                       ELSE
                          ? "Found " + target + " at position " + LTrim(Str(position))
                       ENDIF

                    RETURN
                    ENDPROC
                """.trimIndent(),
                expectedAst = """
                    File
                      Decl.Function[name=LoadInventory]
                        Params
                        Block
                          Stmt.Local
                            Local.Binding[name=items]
                              Expr.ArrayLiteral
                                Expr.Literal.String[value=Mouse]
                                Expr.Literal.String[value=Keyboard]
                                Expr.Literal.String[value=Monitor]
                          Stmt.Local
                            Local.Binding[name=count]
                              Expr.Literal.Number[value=0]
                          Stmt.While
                            Expr.Binary.LessThan
                              Expr.Identifier[name=count]
                              Expr.Call
                                Expr.Identifier[name=Len]
                                Expr.Identifier[name=items]
                            Block
                              Stmt.Print
                                Expr.Binary.Add
                                  Expr.Literal.String[value="Loaded: "]
                                  Expr.Index
                                    Expr.Identifier[name=items]
                                    Expr.Binary.Add
                                      Expr.Identifier[name=count]
                                      Expr.Literal.Number[value=1]
                              Stmt.Assignment
                                Expr.Identifier[name=count]
                                Expr.Binary.Add
                                  Expr.Identifier[name=count]
                                  Expr.Literal.Number[value=1]
                          Stmt.Return
                            Expr.Identifier[name=items]
                      Decl.Function[name=FindItem]
                        Params
                          Expr.Identifier[name=items]
                          Expr.Identifier[name=query]
                        Block
                          Stmt.Local
                            Local.Binding[name=index]
                              Expr.Literal.Number[value=1]
                          Stmt.While
                            Expr.Binary.LessThanOrEqual
                              Expr.Identifier[name=index]
                              Expr.Call
                                Expr.Identifier[name=Len]
                                Expr.Identifier[name=items]
                            Block
                              Stmt.If
                                Expr.Binary.Equal
                                  Expr.Index
                                    Expr.Identifier[name=items]
                                    Expr.Identifier[name=index]
                                  Expr.Identifier[name=query]
                                Block[branch=then]
                                  Stmt.Return
                                    Expr.Identifier[name=index]
                                Block[branch=else]
                              Stmt.Assignment
                                Expr.Identifier[name=index]
                                Expr.Binary.Add
                                  Expr.Identifier[name=index]
                                  Expr.Literal.Number[value=1]
                          Stmt.Return
                            Expr.Literal.Number[value=0]
                      Decl.Procedure[name=InventoryReport]
                        Params
                        Block
                          Stmt.Local
                            Local.Binding[name=data]
                              Expr.Call
                                Expr.Identifier[name=LoadInventory]
                          Stmt.Local
                            Local.Binding[name=target]
                              Expr.Literal.String[value=Keyboard]
                          Stmt.Local
                            Local.Binding[name=position]
                              Expr.Call
                                Expr.Identifier[name=FindItem]
                                Expr.Identifier[name=data]
                                Expr.Identifier[name=target]
                          Stmt.If
                            Expr.Binary.Equal
                              Expr.Identifier[name=position]
                              Expr.Literal.Number[value=0]
                            Block[branch=then]
                              Stmt.Print
                                Expr.Binary.Add
                                  Expr.Literal.String[value="Item not found: "]
                                  Expr.Identifier[name=target]
                            Block[branch=else]
                              Stmt.Print
                                Expr.Binary.Add
                                  Expr.Binary.Add
                                    Expr.Binary.Add
                                      Expr.Literal.String[value="Found "]
                                      Expr.Identifier[name=target]
                                    Expr.Literal.String[value=" at position "]
                                  Expr.Call
                                    Expr.Identifier[name=LTrim]
                                    Expr.Call
                                      Expr.Identifier[name=Str]
                                      Expr.Identifier[name=position]
                          Stmt.Return
                """.trimIndent(),
            ),
        )

        GoldenTestHarness.assertCases(cases, ::parseSource, ::dumpProgram)
    }

    @Test
    fun `parses comments and print continuations`() {
        val cases = listOf<GoldenTestCase<XbProgram>>(
            GoldenTestCase(
                id = "comments-and-print-continuation",
                source = """
                    /*
                    * File: Example.prg
                    */
                    PROCEDURE Main()
                       // Start
                       ? "A", ;
                         "B", ;
                         "C"
                       RETURN
                    ENDPROC
                """.trimIndent(),
                expectedAst = """
                    File
                      Decl.Procedure[name=Main]
                        Params
                        Block
                          Stmt.Print
                            Expr.Literal.String[value=A]
                            Expr.Literal.String[value=B]
                            Expr.Literal.String[value=C]
                          Stmt.Return
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
                    File
                      Stmt.Return
                        Expr.Literal.Number[value=1]
                      Stmt.Return
                        Expr.Literal.Number[value=2]
                """.trimIndent(),
                expectedErrors = listOf("Unexpected token '@' at 10"),
            ),
            GoldenTestCase(
                id = "missing-endif",
                source = "if 1 then return 2;",
                expectedAst = """
                    File
                      Stmt.If
                        Expr.Literal.Number[value=1]
                        Block[branch=then]
                          Stmt.Return
                            Expr.Literal.Number[value=2]
                        Block[branch=else]
                """.trimIndent(),
                expectedErrors = listOf("Expected ENDIF to close IF at 19"),
            ),
            GoldenTestCase(
                id = "optional-then",
                source = "if 1 return 2; endif",
                expectedAst = """
                    File
                      Stmt.If
                        Expr.Literal.Number[value=1]
                        Block[branch=then]
                          Stmt.Return
                            Expr.Literal.Number[value=2]
                        Block[branch=else]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "optional-do",
                source = "while 1 return 2; enddo",
                expectedAst = """
                    File
                      Stmt.While
                        Expr.Literal.Number[value=1]
                        Block
                          Stmt.Return
                            Expr.Literal.Number[value=2]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "missing-rparen",
                source = "(1 + 2;",
                expectedAst = """
                    File
                      Expr.Binary.Add
                        Expr.Literal.Number[value=1]
                        Expr.Literal.Number[value=2]
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
                    File
                      Stmt.Return
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "unary-missing-operand",
                source = "-;",
                expectedAst = """
                    File
                      Expr.Unary.Negation
                        Expr.Identifier[name="<error>"]
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
                    File
                      Expr.Binary.Add
                        Expr.Literal.Number[value=1]
                        Expr.Identifier[name="<error>"]
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
                    File
                      Stmt.If
                        Expr.Identifier[name="<error>"]
                        Block[branch=then]
                        Block[branch=else]
                """.trimIndent(),
                expectedErrors = listOf(
                    "Unexpected token THEN at 3",
                    "Expected condition after IF at 8",
                ),
            ),
            GoldenTestCase(
                id = "unexpected-terminator",
                source = "endif return 1;",
                expectedAst = """
                    File
                      Stmt.Return
                        Expr.Literal.Number[value=1]
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
                    File
                      Stmt.Return
                        Expr.Identifier[name=FOO]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "include-directive",
                source = "#include \"defs.ch\"\nif 1 then return 2; endif",
                expectedAst = """
                    File
                      Stmt.If
                        Expr.Literal.Number[value=1]
                        Block[branch=then]
                          Stmt.Return
                            Expr.Literal.Number[value=2]
                        Block[branch=else]
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
