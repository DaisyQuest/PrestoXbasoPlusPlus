package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.ast.XbFunctionDeclaration
import com.prestoxbasopp.core.ast.XbExpressionStatement
import com.prestoxbasopp.core.ast.XbCallExpression
import com.prestoxbasopp.core.ast.XbUnaryExpression
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

    @Test
    fun `parses production snippet with index command bang-not and multi-dimensional index access`() {
        val source = """
            FUNCTION Repro(cAdd)
            STATIC cORIToUseForCAPCredValidation
            LOCAL oldArea, oldOrder, oldRec, nPointer, xText:="", aCodeList
            IF valtype(cORIToUseForCAPCredValidation)<>"C" .OR. len(cORIToUseForCAPCredValidation)==0
              IF fExists(DataPath()+"CDRCODES.DBF")
                oldArea:=SELE()
                oldOrder := OrdNumber()
                oldRec:=recno()
                dbSelectArea(0)
                net_use("CDRCODES",.F.,5)
                INDEX on KIND to CDRCODES
                SELECT "CDRCODES"
                FIND "L"
                IF found()
                  cORIToUseForCAPCredValidation:=CODE
                ENDIF
                SELE(oldArea)
                OrdSetfocus(oldOrder)
                dbGoTo(OldRec)
              ENDIF
            ENDIF

            IF valtype(aCodeList)<>"A" .OR. len(aCodeList)<2
              aCodeList := {}
              IF fExists(DataPath()+"CDRCODES.DBF")
                oldArea:=SELE()
                dbSelectArea(0)
                Net_Use('cdrcodes',.F.,5)
                DO WHILE !eof()
                  IF KIND=="C"
                    aAdd(aCodeList, {SubStr(CODE,1,1),SubStr(TEXT,1,25)})
                  ENDIF
                  Skip
                ENDDO
                USE
                SELE(oldArea)
              ENDIF
            ENDIF

            nPointer := aScan(aCodeList,{|x|x[1]==cCode})
            IF nPointer>0
              xText := alltrim(aCodeList[nPointer,2])
            ENDIF

            IF fExists(rootPath()+"PTDON.EXE") .AND. !fExists(dataPath()+"PTDON.XX")
               MakePTDON_xx()
            ENDIF
            IF !fExists(dataPath()+"PTDON.XX") .AND. !fExists(rootPath()+"PTDON.EXE")
               xText:="X"
            ENDIF
            RETURN xText
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        assertThat(function.body.statements).isNotEmpty()
    }

    @Test
    fun `parses unary bang not in do while condition as unary expression`() {
        val source = """
            FUNCTION ScanLoop()
            DO WHILE !eof()
              Skip
            ENDDO
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        val whileStatement = function.body.statements.single() as com.prestoxbasopp.core.ast.XbWhileStatement
        assertThat(whileStatement.condition).isInstanceOf(XbUnaryExpression::class.java)
        assertThat((whileStatement.condition as XbUnaryExpression).operator).isEqualTo("not")
    }

    @Test
    fun `parses multi-dimensional index target in assignment as nested index expression`() {
        val source = """
            FUNCTION ReadCode(aCodeList, nPointer)
            LOCAL xText:=""
            xText := alltrim(aCodeList[nPointer,2])
            RETURN xText
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        val assignment = function.body.statements[1] as com.prestoxbasopp.core.ast.XbAssignmentStatement
        val call = assignment.value as XbCallExpression
        val nestedIndex = call.arguments.single() as com.prestoxbasopp.core.ast.XbIndexExpression
        assertThat(nestedIndex.target).isInstanceOf(com.prestoxbasopp.core.ast.XbIndexExpression::class.java)
    }
}
