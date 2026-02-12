package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.ast.XbFunctionDeclaration
import com.prestoxbasopp.core.ast.XbExpressionStatement
import com.prestoxbasopp.core.ast.XbCallExpression
import com.prestoxbasopp.core.ast.XbAssignmentStatement
import com.prestoxbasopp.core.ast.XbIdentifierExpression
import com.prestoxbasopp.core.ast.XbLocalDeclarationStatement
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

    @Test
    fun `parses db-alias arrow access in expressions and assignments`() {
        val source = """
            FUNCTION AliasRead(nameNo)
            LOCAL CivDef := .F.
            IF charges->orig_statu <> "CIV" .OR. !CivDef
              nameNo := charges->name_no
            ENDIF
            RETURN nameNo
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        val ifStatement = function.body.statements[1] as com.prestoxbasopp.core.ast.XbIfStatement
        val condition = ifStatement.condition as com.prestoxbasopp.core.ast.XbBinaryExpression
        val left = condition.left as com.prestoxbasopp.core.ast.XbBinaryExpression
        assertThat(left.left).isInstanceOf(XbIdentifierExpression::class.java)
        assertThat((left.left as XbIdentifierExpression).name).isEqualTo("charges->orig_statu")
        val assignment = ifStatement.thenBlock.statements.single() as XbAssignmentStatement
        assertThat((assignment.target as XbIdentifierExpression).name).isEqualTo("nameNo")
        assertThat((assignment.value as XbIdentifierExpression).name).isEqualTo("charges->name_no")
    }

    @Test
    fun `parses for loops that use equals assignment token`() {
        val source = """
            FUNCTION LoopEq()
            LOCAL total := 0
            FOR i = 1 TO 3
              total += i
            NEXT
            RETURN total
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        assertThat(function.body.statements[1]).isInstanceOf(com.prestoxbasopp.core.ast.XbForStatement::class.java)
    }

    @Test
    fun `parses static declarations through local declaration branch`() {
        val source = """
            FUNCTION UsesStatic()
            STATIC slDoBU := .F., scBUDays := "MON*"
            RETURN slDoBU
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        val declaration = function.body.statements[0] as XbLocalDeclarationStatement
        assertThat(declaration.bindings).hasSize(2)
        assertThat(declaration.bindings.map { it.name }).containsExactly("slDoBU", "scBUDays")
    }

    @Test
    fun `parses class banner asterisk comments without emitting star-token errors`() {
        val source = """
            FUNCTION BannerSafe()
            *******************************************************************************
            * Class "DataThread" used to start thread and open data files
            *******************************************************************************
            RETURN .T.
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        assertThat(function.body.statements).hasSize(1)
    }

    @Test
    fun `parses scoped instance variable assignments`() {
        val source = """
            FUNCTION ThreadStart()
            ::isOpened := .T.
            RETURN
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        val assignment = function.body.statements[0] as XbAssignmentStatement
        assertThat((assignment.target as XbIdentifierExpression).name).isEqualTo("::isOpened")
    }

    @Test
    fun `parses default command clauses with to assignments`() {
        val source = """
            FUNCTION Defaults(nameNo)
            DEFAULT nFormat TO 3, nLen TO 0, lIncAddr TO .F.
            RETURN nameNo
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        val statement = function.body.statements[0] as XbExpressionStatement
        assertThat((statement.expression as XbIdentifierExpression).name).isEqualTo("DEFAULT")
    }


    @Test
    fun `parses colon member property assignment and read`() {
        val source = """
            FUNCTION Paint(oLogo)
            oLogo:type := XBPSTATIC_TYPE_BITMAP
            RETURN oLogo:type
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        val assignment = function.body.statements[0] as XbAssignmentStatement
        assertThat((assignment.target as XbIdentifierExpression).name).isEqualTo("oLogo:type")
        val returnStmt = function.body.statements[1] as com.prestoxbasopp.core.ast.XbReturnStatement
        val call = returnStmt.expression as XbCallExpression
        assertThat((call.callee as XbIdentifierExpression).name).isEqualTo("type")
        assertThat((call.arguments.single() as XbIdentifierExpression).name).isEqualTo("oLogo")
    }

    @Test
    fun `parses alertbox command block with text to and buttons clauses`() {
        val source = """
            FUNCTION Warn(oAlertBox)
            AlertBox TEXT "The File is not available" TO oAlertBox BUTTONS {"OK"}
            RETURN NIL
            ENDFUNCTION
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val function = result.program!!.statements.single() as XbFunctionDeclaration
        val alertStmt = function.body.statements[0] as XbExpressionStatement
        assertThat((alertStmt.expression as XbIdentifierExpression).name).isEqualTo("AlertBox")
    }

}
