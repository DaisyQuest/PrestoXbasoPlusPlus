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
                id = "not-equal-hash-operator",
                source = "if a # b then return 1; endif",
                expectedAst = """
                    File
                      Stmt.If
                        Expr.Binary.NotEqual
                          Expr.Identifier[name=a]
                          Expr.Identifier[name=b]
                        Block[branch=then]
                          Stmt.Return
                            Expr.Literal.Number[value=1]
                        Block[branch=else]
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
    fun `parses endprocedure and endfunc terminators without errors`() {
        val cases = listOf<GoldenTestCase<XbProgram>>(
            GoldenTestCase(
                id = "function-endfunc",
                source = """
                    FUNCTION BuildTitle(name)
                       RETURN "Hello " + name
                    ENDFUNC
                """.trimIndent(),
                expectedAst = """
                    File
                      Decl.Function[name=BuildTitle]
                        Params
                          Expr.Identifier[name=name]
                        Block
                          Stmt.Return
                            Expr.Binary.Add
                              Expr.Literal.String[value="Hello "]
                              Expr.Identifier[name=name]
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "procedure-endprocedure",
                source = """
                    PROCEDURE RenderStatusBar(userName)
                       ? "Status for " + userName
                    ENDPROCEDURE
                """.trimIndent(),
                expectedAst = """
                    File
                      Decl.Procedure[name=RenderStatusBar]
                        Params
                          Expr.Identifier[name=userName]
                        Block
                          Stmt.Print
                            Expr.Binary.Add
                              Expr.Literal.String[value="Status for "]
                              Expr.Identifier[name=userName]
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
    fun `parses indexed assignment statements`() {
        val cases = listOf<GoldenTestCase<XbProgram>>(
            GoldenTestCase(
                id = "indexed-assignments",
                source = """
                    items[1] := "Mouse"
                    matrix[1][2] := 3
                """.trimIndent(),
                expectedAst = """
                    File
                      Stmt.Assignment
                        Expr.Index
                          Expr.Identifier[name=items]
                          Expr.Literal.Number[value=1]
                        Expr.Literal.String[value=Mouse]
                      Stmt.Assignment
                        Expr.Index
                          Expr.Index
                            Expr.Identifier[name=matrix]
                            Expr.Literal.Number[value=1]
                          Expr.Literal.Number[value=2]
                        Expr.Literal.Number[value=3]
                """.trimIndent(),
            ),
        )

        GoldenTestHarness.assertCases(cases, ::parseSource, ::dumpProgram)
    }

    @Test
    fun `parses wait and exit statements`() {
        val cases = listOf<GoldenTestCase<XbProgram>>(
            GoldenTestCase(
                id = "wait-no-args-exit",
                source = "WAIT\nEXIT",
                expectedAst = """
                    File
                      Stmt.Wait
                      Stmt.Exit
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "wait-with-message",
                source = "WAIT \"Press any key\";",
                expectedAst = """
                    File
                      Stmt.Wait
                        Expr.Literal.String[value="Press any key"]
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
                expectedErrors = listOf("Expected ')' after expression at 7"),
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
                    "Unexpected token EOF at 2",
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
                    "Unexpected token EOF at 5",
                    "Expected expression after '+' at 5",
                ),
            ),
            GoldenTestCase(
                id = "line-continuation-blank-line",
                source = """
                    FUNCTION Main()
                       LOCAL cWeird := ;
                          "line1" + Chr(13)+Chr(10) + ;
                          "line2 ; looks like a terminator but isn't" + Chr(10) + ;

                       LOCAL h := {=>}
                       h["key"] := "value"
                    RETURN NIL
                """.trimIndent(),
                expectedAst = """
                    File
                      Decl.Function[name=Main]
                        Params
                        Block
                          Stmt.Local
                            Local.Binding[name=cWeird]
                              Expr.Binary.Add
                                Expr.Binary.Add
                                  Expr.Binary.Add
                                    Expr.Binary.Add
                                      Expr.Binary.Add
                                        Expr.Literal.String[value=line1]
                                        Expr.Call
                                          Expr.Identifier[name=Chr]
                                          Expr.Literal.Number[value=13]
                                      Expr.Call
                                        Expr.Identifier[name=Chr]
                                        Expr.Literal.Number[value=10]
                                    Expr.Literal.String[value="line2 ; looks like a terminator but isn't"]
                                  Expr.Call
                                    Expr.Identifier[name=Chr]
                                    Expr.Literal.Number[value=10]
                          Stmt.Local
                            Local.Binding[name=h]
                              Expr.HashLiteral
                          Stmt.Assignment
                            Expr.Index
                              Expr.Identifier[name=h]
                              Expr.Literal.String[value=key]
                            Expr.Literal.String[value=value]
                          Stmt.Return
                            Expr.Literal.Nil
                """.trimIndent(),
                expectedErrors = listOf(
                    "Unexpected token SEMICOLON at 135",
                    "Expected expression after '+' at 141",
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
            GoldenTestCase(
                id = "directive-with-at-say-get",
                source = "#define UI_ENABLED 1\n@ 1, 2 SAY \"Hi\" GET value",
                expectedAst = """
                    File
                      Stmt.AtSayGet
                        Expr.Literal.Number[value=1]
                        Expr.Literal.Number[value=2]
                        At.Say
                          Expr.Literal.String[value=Hi]
                        At.Get
                          Expr.Identifier[name=value]
                """.trimIndent(),
            ),
        )

        GoldenTestHarness.assertCases(cases, ::parseSource, ::dumpProgram)
    }

    @Test
    fun `parses macro stress sample with recovery`() {
        val cases = listOf<GoldenTestCase<XbProgram>>(
            GoldenTestCase(
                id = "macro-stress-sample",
                source = """
                    #include "appevent.ch"

                    #xtranslate STR(<x>) => '"' + <x> + '"'
                    #xtranslate DBG(<x>) => ( QOut( "DBG:" + ValToChar(<x>) ), <x> )

                    FUNCTION Main()
                       LOCAL cName := "Bossman"
                       LOCAL cMacro := "cName"
                       LOCAL a := { 1, 2, 3, {|| "nested block" }, { "a", "b", "c" } }
                       LOCAL h := {=>}
                       LOCAL i, j, k
                       LOCAL cWeird := ;
                          "line1" + Chr(13)+Chr(10) + ;
                          "line2 ; looks like a terminator but isn't" + Chr(10) + ;
                          "line3 with quotes "" inside"

                       h["key"] := "value"
                       h["num"] := 42
                       h["blk"] := {|x| IIF( x == NIL, "NIL", ValToChar(x) ) }

                       // Token salad: comments inside expressions and weird spacing
                       i := ( 1 /*c*/ + 2 ) * ( 3 + 4 ) - ( (5) ) + ( ( (6) ) )

                       BEGIN SEQUENCE
                          // Macro indirection on function name + params
                          &("QOut")( "Hello " + &cMacro + "!" )

                          // Macro that *builds* code, then executes it
                          cMacro := "QOut( 'macro says: ' + " + STR("x") + " )"
                          &("{|| " + cMacro + " }")()

                          // Deeply nested blocks + inline IF + IIF
                          FOR j := 1 TO 3
                             k := Eval( { |n| IIF( n % 2 == 0, ;
                                           ( {|m| m*m } )(n), ;
                                           ( {|m| m+m } )(n) ) }, j )

                             QOut( "j=" + LTrim(Str(j)) + " k=" + LTrim(Str(k)) )
                          NEXT

                          // @..SAY/GET with a block that includes macro + IIF
                          LOCAL cInput := Space(20)
                          LOCAL bValid := {|v| DBG( IIF( Empty(v), .F., .T. ) ) }

                          @ 5, 10 SAY "Enter:" GET cInput VALID Eval( bValid, cInput )

                          // Try a nasty do-while with macro and string escapes
                          DO WHILE ( &("Len")(cInput) < 1 ) .AND. ( "x""y" $ ("x" + Chr(34) + "y") )
                             cInput := "forced"
                          ENDDO

                          // Throw to RECOVER with an expression that looks like syntax trouble
                          BREAK ( {|| "BREAK payload: " + ;
                                     IIF( ( 1 + (2) ) == 3, "ok", "no" ) + ;
                                     " :: " + ValToChar( h["num"] ) } )()

                       RECOVER USING oErr
                          // oErr might be anything (including string), force weird checks
                          QOut( "RECOVER got: " + ValToChar(oErr) )

                          // Preprocessor + macro inside recover
                          QOut( STR("recover") + ":" + &("Upper")( "done" ) )
                       END SEQUENCE

                       // Final: macro that references array/hash and executes block from hash
                       cMacro := "h['blk']"
                       QOut( "h=" + Eval( &(cMacro), 123 ) )

                       // Parser poke: concatenation with line continuations and odd parentheses
                       QOut( ;
                          "weird=" + ;
                          ( ( ( "A" + "B" ) ) ) + ;
                          ":" + ;
                          ValToChar( ( {|| a[4]() } )() ) )

                    RETURN NIL
                """.trimIndent(),
                expectedAst = """
                    File
                      Decl.Function[name=Main]
                        Params
                        Block
                          Stmt.Local
                            Local.Binding[name=cName]
                              Expr.Literal.String[value=Bossman]
                          Stmt.Local
                            Local.Binding[name=cMacro]
                              Expr.Literal.String[value=cName]
                          Stmt.Local
                            Local.Binding[name=a]
                              Expr.ArrayLiteral
                                Expr.Literal.Number[value=1]
                                Expr.Literal.Number[value=2]
                                Expr.Literal.Number[value=3]
                                Expr.BlockLiteral
                                  Params
                                  Expr.Literal.String[value="nested block"]
                                Expr.ArrayLiteral
                                  Expr.Literal.String[value=a]
                                  Expr.Literal.String[value=b]
                                  Expr.Literal.String[value=c]
                          Stmt.Local
                            Local.Binding[name=h]
                              Expr.HashLiteral
                          Stmt.Local
                            Local.Binding[name=i]
                            Local.Binding[name=j]
                            Local.Binding[name=k]
                          Stmt.Local
                            Local.Binding[name=cWeird]
                              Expr.Binary.Add
                                Expr.Binary.Add
                                  Expr.Binary.Add
                                    Expr.Binary.Add
                                      Expr.Binary.Add
                                        Expr.Literal.String[value=line1]
                                        Expr.Call
                                          Expr.Identifier[name=Chr]
                                          Expr.Literal.Number[value=13]
                                      Expr.Call
                                        Expr.Identifier[name=Chr]
                                        Expr.Literal.Number[value=10]
                                    Expr.Literal.String[value="line2 ; looks like a terminator but isn't"]
                                  Expr.Call
                                    Expr.Identifier[name=Chr]
                                    Expr.Literal.Number[value=10]
                                Expr.Literal.String[value="line3 with quotes \" inside"]
                          Stmt.Assignment
                            Expr.Index
                              Expr.Identifier[name=h]
                              Expr.Literal.String[value=key]
                            Expr.Literal.String[value=value]
                          Stmt.Assignment
                            Expr.Index
                              Expr.Identifier[name=h]
                              Expr.Literal.String[value=num]
                            Expr.Literal.Number[value=42]
                          Stmt.Assignment
                            Expr.Index
                              Expr.Identifier[name=h]
                              Expr.Literal.String[value=blk]
                            Expr.BlockLiteral
                              Params
                                Expr.Identifier[name=x]
                              Expr.Call
                                Expr.Identifier[name=IIF]
                                Expr.Binary.Equal
                                  Expr.Identifier[name=x]
                                  Expr.Literal.Nil
                                Expr.Literal.String[value=NIL]
                                Expr.Call
                                  Expr.Identifier[name=ValToChar]
                                  Expr.Identifier[name=x]
                          Stmt.Assignment
                            Expr.Identifier[name=i]
                            Expr.Binary.Add
                              Expr.Binary.Subtract
                                Expr.Binary.Multiply
                                  Expr.Binary.Add
                                    Expr.Literal.Number[value=1]
                                    Expr.Literal.Number[value=2]
                                  Expr.Binary.Add
                                    Expr.Literal.Number[value=3]
                                    Expr.Literal.Number[value=4]
                                Expr.Literal.Number[value=5]
                              Expr.Literal.Number[value=6]
                          Stmt.Sequence[recoverVar=oErr]
                            Block[branch=sequence]
                              Stmt.Expression
                                Expr.Unary[op="&"]
                                  Expr.Call
                                    Expr.Literal.String[value=QOut]
                                    Expr.Binary.Add
                                      Expr.Binary.Add
                                        Expr.Literal.String[value="Hello "]
                                        Expr.Unary[op="&"]
                                          Expr.Identifier[name=cMacro]
                                      Expr.Literal.String[value="!"]
                              Stmt.Assignment
                                Expr.Identifier[name=cMacro]
                                Expr.Binary.Add
                                  Expr.Binary.Add
                                    Expr.Literal.String[value="QOut( 'macro says: ' + "]
                                    Expr.Call
                                      Expr.Identifier[name=STR]
                                      Expr.Literal.String[value=x]
                                  Expr.Literal.String[value=" )"]
                              Stmt.Expression
                                Expr.Unary[op="&"]
                                  Expr.Call
                                    Expr.Binary.Add
                                      Expr.Binary.Add
                                        Expr.Literal.String[value="{|| "]
                                        Expr.Identifier[name=cMacro]
                                      Expr.Literal.String[value=" }"]
                              Stmt.For
                                Expr.Identifier[name=j]
                                Expr.Literal.Number[value=1]
                                Expr.Literal.Number[value=3]
                                Expr.Literal.Number[value=1]
                                Block
                                  Stmt.Assignment
                                    Expr.Identifier[name=k]
                                    Expr.Call
                                      Expr.Identifier[name=Eval]
                                      Expr.BlockLiteral
                                        Params
                                          Expr.Identifier[name=n]
                                        Expr.Call
                                          Expr.Identifier[name=IIF]
                                          Expr.Binary.Equal
                                            Expr.Binary[op="%"]
                                              Expr.Identifier[name=n]
                                              Expr.Literal.Number[value=2]
                                            Expr.Literal.Number[value=0]
                                          Expr.Call
                                            Expr.BlockLiteral
                                              Params
                                                Expr.Identifier[name=m]
                                              Expr.Binary.Multiply
                                                Expr.Identifier[name=m]
                                                Expr.Identifier[name=m]
                                            Expr.Identifier[name=n]
                                          Expr.Call
                                            Expr.BlockLiteral
                                              Params
                                                Expr.Identifier[name=m]
                                              Expr.Binary.Add
                                                Expr.Identifier[name=m]
                                                Expr.Identifier[name=m]
                                            Expr.Identifier[name=n]
                                      Expr.Identifier[name=j]
                                  Stmt.Expression
                                    Expr.Call
                                      Expr.Identifier[name=QOut]
                                      Expr.Binary.Add
                                        Expr.Binary.Add
                                          Expr.Binary.Add
                                            Expr.Literal.String[value="j="]
                                            Expr.Call
                                              Expr.Identifier[name=LTrim]
                                              Expr.Call
                                                Expr.Identifier[name=Str]
                                                Expr.Identifier[name=j]
                                          Expr.Literal.String[value=" k="]
                                        Expr.Call
                                          Expr.Identifier[name=LTrim]
                                          Expr.Call
                                            Expr.Identifier[name=Str]
                                            Expr.Identifier[name=k]
                              Stmt.Local
                                Local.Binding[name=cInput]
                                  Expr.Call
                                    Expr.Identifier[name=Space]
                                    Expr.Literal.Number[value=20]
                              Stmt.Local
                                Local.Binding[name=bValid]
                                  Expr.BlockLiteral
                                    Params
                                      Expr.Identifier[name=v]
                                    Expr.Call
                                      Expr.Identifier[name=DBG]
                                      Expr.Call
                                        Expr.Identifier[name=IIF]
                                        Expr.Call
                                          Expr.Identifier[name=Empty]
                                          Expr.Identifier[name=v]
                                        Expr.Literal.Boolean[value=false]
                                        Expr.Literal.Boolean[value=true]
                              Stmt.AtSayGet
                                Expr.Literal.Number[value=5]
                                Expr.Literal.Number[value=10]
                                At.Say
                                  Expr.Literal.String[value="Enter:"]
                                At.Get
                                  Expr.Identifier[name=cInput]
                                At.Valid
                                  Expr.Call
                                    Expr.Identifier[name=Eval]
                                    Expr.Identifier[name=bValid]
                                    Expr.Identifier[name=cInput]
                              Stmt.While
                                Expr.Binary.And
                                  Expr.Binary.LessThan
                                    Expr.Unary[op="&"]
                                      Expr.Call
                                        Expr.Literal.String[value=Len]
                                        Expr.Identifier[name=cInput]
                                    Expr.Literal.Number[value=1]
                                  Expr.Binary[op="$"]
                                    Expr.Literal.String[value="x\"y"]
                                    Expr.Binary.Add
                                      Expr.Binary.Add
                                        Expr.Literal.String[value=x]
                                        Expr.Call
                                          Expr.Identifier[name=Chr]
                                          Expr.Literal.Number[value=34]
                                      Expr.Literal.String[value=y]
                                Block
                                  Stmt.Assignment
                                    Expr.Identifier[name=cInput]
                                    Expr.Literal.String[value=forced]
                              Stmt.Break
                                Expr.Call
                                  Expr.BlockLiteral
                                    Params
                                    Expr.Binary.Add
                                      Expr.Binary.Add
                                        Expr.Binary.Add
                                          Expr.Literal.String[value="BREAK payload: "]
                                          Expr.Call
                                            Expr.Identifier[name=IIF]
                                            Expr.Binary.Equal
                                              Expr.Binary.Add
                                                Expr.Literal.Number[value=1]
                                                Expr.Literal.Number[value=2]
                                              Expr.Literal.Number[value=3]
                                            Expr.Literal.String[value=ok]
                                            Expr.Literal.String[value=no]
                                        Expr.Literal.String[value=" :: "]
                                      Expr.Call
                                        Expr.Identifier[name=ValToChar]
                                        Expr.Index
                                          Expr.Identifier[name=h]
                                          Expr.Literal.String[value=num]
                            Block[branch=recover]
                              Stmt.Expression
                                Expr.Call
                                  Expr.Identifier[name=QOut]
                                  Expr.Binary.Add
                                    Expr.Literal.String[value="RECOVER got: "]
                                    Expr.Call
                                      Expr.Identifier[name=ValToChar]
                                      Expr.Identifier[name=oErr]
                              Stmt.Expression
                                Expr.Call
                                  Expr.Identifier[name=QOut]
                                  Expr.Binary.Add
                                    Expr.Binary.Add
                                      Expr.Call
                                        Expr.Identifier[name=STR]
                                        Expr.Literal.String[value=recover]
                                      Expr.Literal.String[value=":"]
                                    Expr.Unary[op="&"]
                                      Expr.Call
                                        Expr.Literal.String[value=Upper]
                                        Expr.Literal.String[value=done]
                          Stmt.Assignment
                            Expr.Identifier[name=cMacro]
                            Expr.Literal.String[value="h['blk']"]
                          Stmt.Expression
                            Expr.Call
                              Expr.Identifier[name=QOut]
                              Expr.Binary.Add
                                Expr.Literal.String[value="h="]
                                Expr.Call
                                  Expr.Identifier[name=Eval]
                                  Expr.Unary[op="&"]
                                    Expr.Identifier[name=cMacro]
                                  Expr.Literal.Number[value=123]
                          Stmt.Expression
                            Expr.Call
                              Expr.Identifier[name=QOut]
                              Expr.Binary.Add
                                Expr.Binary.Add
                                  Expr.Binary.Add
                                    Expr.Literal.String[value="weird="]
                                    Expr.Binary.Add
                                      Expr.Literal.String[value=A]
                                      Expr.Literal.String[value=B]
                                  Expr.Literal.String[value=":"]
                                Expr.Call
                                  Expr.Identifier[name=ValToChar]
                                  Expr.Call
                                    Expr.BlockLiteral
                                      Params
                                      Expr.Call
                                        Expr.Index
                                          Expr.Identifier[name=a]
                                          Expr.Literal.Number[value=4]
                          Stmt.Return
                            Expr.Literal.Nil
                """.trimIndent(),
            ),
            GoldenTestCase(
                id = "macro-stress-sample-user",
                source = """
                    #include "appevent.ch"

                    #xtranslate STR(<x>) => '"' + <x> + '"'
                    #xtranslate DBG(<x>) => ( QOut( "DBG:" + ValToChar(<x>) ), <x> )

                    FUNCTION Main()
                       LOCAL cName := "Bossman"
                       LOCAL cMacro := "cName"
                       LOCAL a := { 1, 2, 3, 4}
                       LOCAL h := {=>}
                       LOCAL i, j, k
                       LOCAL cWeird := ;
                          "line1" + Chr(13)+Chr(10) + ;
                          "line2 ; looks like a terminator but isn't" + Chr(10) + ;

                       h["key"] := "value"
                       h["num"] := 42
                       h["blk"] := {|x| IIF( x == NIL, "NIL", ValToChar(x) ) }

                       // Token salad: comments inside expressions and weird spacing

                       i := ( 1 /*c*/ + 2 ) * ( 3 + 4 ) - ( (5) ) + ( ( (6) ) )

                       BEGIN SEQUENCE
                          // Macro indirection on function name + params
                          &("QOut")( "Hello " + &cMacro + "!" )

                          // Macro that *builds* code, then executes it
                          cMacro := "QOut( 'macro says: ' + " + STR("x") + " )"
                          &("{|| " + cMacro + " }")()

                          // Deeply nested blocks + inline IF + IIF
                          FOR j := 1 TO 3
                             k := Eval( { |n| IIF( n % 2 == 0, ;
                                           ( {|m| m*m } )(n), ;
                                           ( {|m| m+m } )(n) ) }, j )

                             QOut( "j=" + LTrim(Str(j)) + " k=" + LTrim(Str(k)) )
                          NEXT

                          // @..SAY/GET with a block that includes macro + IIF
                          LOCAL cInput := Space(20)
                          LOCAL bValid := {|v| DBG( IIF( Empty(v), .F., .T. ) ) }

                          @ 5, 10 SAY "Enter:" GET cInput VALID Eval( bValid, cInput )

                          // Try a nasty do-while with macro and string escapes
                          DO WHILE ( &("Len")(cInput) < 1 ) .AND. ( "x""y" $ ("x" + Chr(34) + "y") )
                             cInput := "forced"
                          ENDDO

                          // Throw to RECOVER with an expression that looks like syntax trouble
                          BREAK ( {|| "BREAK payload: " + ;
                                     IIF( ( 1 + (2) ) == 3, "ok", "no" ) + ;
                                     " :: " + ValToChar( h["num"] ) } )()

                       RECOVER USING oErr
                          // oErr might be anything (including string), force weird checks
                          QOut( "RECOVER got: " + ValToChar(oErr) )

                          // Preprocessor + macro inside recover
                          QOut( STR("recover") + ":" + &("Upper")( "done" ) )
                       END SEQUENCE

                       // Final: macro that references array/hash and executes block from hash
                       cMacro := "h['blk']"
                       QOut( "h=" + Eval( &(cMacro), 123 ) )

                       // Parser poke: concatenation with line continuations and odd parentheses
                       QOut( ;
                          "weird=" + ;
                          ( ( ( "A" + "B" ) ) ) + ;
                          ":" + ;
                          ValToChar( ( {|| a[4]() } )() ) )

                    RETURN NIL
                """.trimIndent(),
                expectedAst = """
                    File
                      Decl.Function[name=Main]
                        Params
                        Block
                          Stmt.Local
                            Local.Binding[name=cName]
                              Expr.Literal.String[value=Bossman]
                          Stmt.Local
                            Local.Binding[name=cMacro]
                              Expr.Literal.String[value=cName]
                          Stmt.Local
                            Local.Binding[name=a]
                              Expr.ArrayLiteral
                                Expr.Literal.Number[value=1]
                                Expr.Literal.Number[value=2]
                                Expr.Literal.Number[value=3]
                                Expr.Literal.Number[value=4]
                          Stmt.Local
                            Local.Binding[name=h]
                              Expr.HashLiteral
                          Stmt.Local
                            Local.Binding[name=i]
                            Local.Binding[name=j]
                            Local.Binding[name=k]
                          Stmt.Local
                            Local.Binding[name=cWeird]
                              Expr.Binary.Add
                                Expr.Binary.Add
                                  Expr.Binary.Add
                                    Expr.Binary.Add
                                      Expr.Literal.String[value=line1]
                                      Expr.Call
                                        Expr.Identifier[name=Chr]
                                        Expr.Literal.Number[value=13]
                                    Expr.Call
                                      Expr.Identifier[name=Chr]
                                      Expr.Literal.Number[value=10]
                                  Expr.Literal.String[value="line2 ; looks like a terminator but isn't"]
                                Expr.Call
                                  Expr.Identifier[name=Chr]
                                  Expr.Literal.Number[value=10]
                          Stmt.Assignment
                            Expr.Index
                              Expr.Identifier[name=h]
                              Expr.Literal.String[value=key]
                            Expr.Literal.String[value=value]
                          Stmt.Assignment
                            Expr.Index
                              Expr.Identifier[name=h]
                              Expr.Literal.String[value=num]
                            Expr.Literal.Number[value=42]
                          Stmt.Assignment
                            Expr.Index
                              Expr.Identifier[name=h]
                              Expr.Literal.String[value=blk]
                            Expr.BlockLiteral
                              Params
                                Expr.Identifier[name=x]
                              Expr.Call
                                Expr.Identifier[name=IIF]
                                Expr.Binary.Equal
                                  Expr.Identifier[name=x]
                                  Expr.Literal.Nil
                                Expr.Literal.String[value=NIL]
                                Expr.Call
                                  Expr.Identifier[name=ValToChar]
                                  Expr.Identifier[name=x]
                          Stmt.Assignment
                            Expr.Identifier[name=i]
                            Expr.Binary.Add
                              Expr.Binary.Subtract
                                Expr.Binary.Multiply
                                  Expr.Binary.Add
                                    Expr.Literal.Number[value=1]
                                    Expr.Literal.Number[value=2]
                                  Expr.Binary.Add
                                    Expr.Literal.Number[value=3]
                                    Expr.Literal.Number[value=4]
                                Expr.Literal.Number[value=5]
                              Expr.Literal.Number[value=6]
                          Stmt.Sequence[recoverVar=oErr]
                            Block[branch=sequence]
                              Stmt.Expression
                                Expr.Unary[op="&"]
                                  Expr.Call
                                    Expr.Literal.String[value=QOut]
                                    Expr.Binary.Add
                                      Expr.Binary.Add
                                        Expr.Literal.String[value="Hello "]
                                        Expr.Unary[op="&"]
                                          Expr.Identifier[name=cMacro]
                                      Expr.Literal.String[value="!"]
                              Stmt.Assignment
                                Expr.Identifier[name=cMacro]
                                Expr.Binary.Add
                                  Expr.Binary.Add
                                    Expr.Literal.String[value="QOut( 'macro says: ' + "]
                                    Expr.Call
                                      Expr.Identifier[name=STR]
                                      Expr.Literal.String[value=x]
                                  Expr.Literal.String[value=" )"]
                              Stmt.Expression
                                Expr.Unary[op="&"]
                                  Expr.Call
                                    Expr.Binary.Add
                                      Expr.Binary.Add
                                        Expr.Literal.String[value="{|| "]
                                        Expr.Identifier[name=cMacro]
                                      Expr.Literal.String[value=" }"]
                              Stmt.For
                                Expr.Identifier[name=j]
                                Expr.Literal.Number[value=1]
                                Expr.Literal.Number[value=3]
                                Expr.Literal.Number[value=1]
                                Block
                                  Stmt.Assignment
                                    Expr.Identifier[name=k]
                                    Expr.Call
                                      Expr.Identifier[name=Eval]
                                      Expr.BlockLiteral
                                        Params
                                          Expr.Identifier[name=n]
                                        Expr.Call
                                          Expr.Identifier[name=IIF]
                                          Expr.Binary.Equal
                                            Expr.Binary[op="%"]
                                              Expr.Identifier[name=n]
                                              Expr.Literal.Number[value=2]
                                            Expr.Literal.Number[value=0]
                                          Expr.Call
                                            Expr.BlockLiteral
                                              Params
                                                Expr.Identifier[name=m]
                                              Expr.Binary.Multiply
                                                Expr.Identifier[name=m]
                                                Expr.Identifier[name=m]
                                            Expr.Identifier[name=n]
                                          Expr.Call
                                            Expr.BlockLiteral
                                              Params
                                                Expr.Identifier[name=m]
                                              Expr.Binary.Add
                                                Expr.Identifier[name=m]
                                                Expr.Identifier[name=m]
                                            Expr.Identifier[name=n]
                                      Expr.Identifier[name=j]
                                  Stmt.Expression
                                    Expr.Call
                                      Expr.Identifier[name=QOut]
                                      Expr.Binary.Add
                                        Expr.Binary.Add
                                          Expr.Binary.Add
                                            Expr.Literal.String[value="j="]
                                            Expr.Call
                                              Expr.Identifier[name=LTrim]
                                              Expr.Call
                                                Expr.Identifier[name=Str]
                                                Expr.Identifier[name=j]
                                          Expr.Literal.String[value=" k="]
                                        Expr.Call
                                          Expr.Identifier[name=LTrim]
                                          Expr.Call
                                            Expr.Identifier[name=Str]
                                            Expr.Identifier[name=k]
                              Stmt.Local
                                Local.Binding[name=cInput]
                                  Expr.Call
                                    Expr.Identifier[name=Space]
                                    Expr.Literal.Number[value=20]
                              Stmt.Local
                                Local.Binding[name=bValid]
                                  Expr.BlockLiteral
                                    Params
                                      Expr.Identifier[name=v]
                                    Expr.Call
                                      Expr.Identifier[name=DBG]
                                      Expr.Call
                                        Expr.Identifier[name=IIF]
                                        Expr.Call
                                          Expr.Identifier[name=Empty]
                                          Expr.Identifier[name=v]
                                        Expr.Literal.Boolean[value=false]
                                        Expr.Literal.Boolean[value=true]
                              Stmt.AtSayGet
                                Expr.Literal.Number[value=5]
                                Expr.Literal.Number[value=10]
                                At.Say
                                  Expr.Literal.String[value="Enter:"]
                                At.Get
                                  Expr.Identifier[name=cInput]
                                At.Valid
                                  Expr.Call
                                    Expr.Identifier[name=Eval]
                                    Expr.Identifier[name=bValid]
                                    Expr.Identifier[name=cInput]
                              Stmt.While
                                Expr.Binary.And
                                  Expr.Binary.LessThan
                                    Expr.Unary[op="&"]
                                      Expr.Call
                                        Expr.Literal.String[value=Len]
                                        Expr.Identifier[name=cInput]
                                    Expr.Literal.Number[value=1]
                                  Expr.Binary[op="$"]
                                    Expr.Literal.String[value="x\"y"]
                                    Expr.Binary.Add
                                      Expr.Binary.Add
                                        Expr.Literal.String[value=x]
                                        Expr.Call
                                          Expr.Identifier[name=Chr]
                                          Expr.Literal.Number[value=34]
                                      Expr.Literal.String[value=y]
                                Block
                                  Stmt.Assignment
                                    Expr.Identifier[name=cInput]
                                    Expr.Literal.String[value=forced]
                              Stmt.Break
                                Expr.Call
                                  Expr.BlockLiteral
                                    Params
                                    Expr.Binary.Add
                                      Expr.Binary.Add
                                        Expr.Binary.Add
                                          Expr.Literal.String[value="BREAK payload: "]
                                          Expr.Call
                                            Expr.Identifier[name=IIF]
                                            Expr.Binary.Equal
                                              Expr.Binary.Add
                                                Expr.Literal.Number[value=1]
                                                Expr.Literal.Number[value=2]
                                              Expr.Literal.Number[value=3]
                                            Expr.Literal.String[value=ok]
                                            Expr.Literal.String[value=no]
                                        Expr.Literal.String[value=" :: "]
                                      Expr.Call
                                        Expr.Identifier[name=ValToChar]
                                        Expr.Index
                                          Expr.Identifier[name=h]
                                          Expr.Literal.String[value=num]
                            Block[branch=recover]
                              Stmt.Expression
                                Expr.Call
                                  Expr.Identifier[name=QOut]
                                  Expr.Binary.Add
                                    Expr.Literal.String[value="RECOVER got: "]
                                    Expr.Call
                                      Expr.Identifier[name=ValToChar]
                                      Expr.Identifier[name=oErr]
                              Stmt.Expression
                                Expr.Call
                                  Expr.Identifier[name=QOut]
                                  Expr.Binary.Add
                                    Expr.Binary.Add
                                      Expr.Call
                                        Expr.Identifier[name=STR]
                                        Expr.Literal.String[value=recover]
                                      Expr.Literal.String[value=":"]
                                    Expr.Unary[op="&"]
                                      Expr.Call
                                        Expr.Literal.String[value=Upper]
                                        Expr.Literal.String[value=done]
                          Stmt.Assignment
                            Expr.Identifier[name=cMacro]
                            Expr.Literal.String[value="h['blk']"]
                          Stmt.Expression
                            Expr.Call
                              Expr.Identifier[name=QOut]
                              Expr.Binary.Add
                                Expr.Literal.String[value="h="]
                                Expr.Call
                                  Expr.Identifier[name=Eval]
                                  Expr.Unary[op="&"]
                                    Expr.Identifier[name=cMacro]
                                  Expr.Literal.Number[value=123]
                          Stmt.Expression
                            Expr.Call
                              Expr.Identifier[name=QOut]
                              Expr.Binary.Add
                                Expr.Binary.Add
                                  Expr.Binary.Add
                                    Expr.Literal.String[value="weird="]
                                    Expr.Binary.Add
                                      Expr.Literal.String[value=A]
                                      Expr.Literal.String[value=B]
                                  Expr.Literal.String[value=":"]
                                Expr.Call
                                  Expr.Identifier[name=ValToChar]
                                  Expr.Call
                                    Expr.BlockLiteral
                                      Params
                                      Expr.Call
                                        Expr.Index
                                          Expr.Identifier[name=a]
                                          Expr.Literal.Number[value=4]
                          Stmt.Return
                            Expr.Literal.Nil
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
