package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.ast.XbFunctionDeclaration
import com.prestoxbasopp.core.ast.XbExpressionStatement
import com.prestoxbasopp.core.ast.XbCallExpression
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbParserProductionCompatibilityTest {
    @Test
    fun `parses pass-by-reference arguments using at prefix in function calls`() {
        val source = """
            FUNCTION CenterScnPos( aSize, oRefDlg )
            LOCAL aRefPos, aRefSize
            v_Valtype(@oRefDlg, RootWindow(), @aSize, {0,0})
            RETURN {int(aRefPos[1]+(aRefSize[1]/2)-(aSize[1]/2)), int(aRefPos[2]+(aRefSize[2]/2)-(aSize[2]/2))}
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        val callStatement = function.body.statements[1] as XbExpressionStatement
        val call = callStatement.expression as XbCallExpression
        assertThat(call.arguments).hasSize(4)
    }

    @Test
    fun `parses if blocks without cascading endif errors when condition is valid`() {
        val source = """
            FUNCTION Ok2SaveDlgSize( lSaveSize )
               STATIC soSaveDlgSize
               IF valtype(soSaveDlgSize) # "L"
                 soSaveDlgSize := .T.
               ENDIF
               IF PCount() > 0 .AND. valtype(lSaveSize) == "L"
                  soSaveDlgSize := lSaveSize
               ENDIF
            RETURN soSaveDlgSize
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        assertThat(result.program!!.statements).hasSize(1)
    }

    @Test
    fun `parses object method dispatch with colon syntax`() {
        val source = """
            FUNCTION readCBStructFromIniAndStoreStatic(p_IniFile,p_typist)
              LOCAL aKeyList:={}, aCBstruct:={}
              p_IniFile:ReadSectionValues(p_typist +"-ClipBoard", aKeyList)
            RETURN aCBstruct
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        val callStatement = function.body.statements[1] as XbExpressionStatement
        val call = callStatement.expression as XbCallExpression
        assertThat(call.arguments).hasSize(3)
    }
}
