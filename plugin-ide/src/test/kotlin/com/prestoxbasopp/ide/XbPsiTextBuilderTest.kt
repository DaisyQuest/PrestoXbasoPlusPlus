package com.prestoxbasopp.ide

import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiSymbolReference
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration
import com.prestoxbasopp.core.psi.XbPsiBlock
import com.prestoxbasopp.core.psi.XbVariableStorageClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbPsiTextBuilderTest {
    @Test
    fun `builds function and variable declarations`() {
        val source = """
            function Main(user, mode)
               local count, total
               total := count + 1
               Main(user, mode)
               return total
            endfunction
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source, "sample.xb")
        val functions = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().toList()
        val variables = root.walk().filterIsInstance<XbPsiVariableDeclaration>().toList()
        val references = root.walk().filterIsInstance<XbPsiSymbolReference>().toList()

        assertThat(functions.map { it.symbolName }).containsExactly("Main")
        assertThat(functions.first().parameters).containsExactly("user", "mode")
        assertThat(variables.map { it.symbolName }).containsExactlyInAnyOrder("user", "mode", "count", "total")
        assertThat(references.map { it.symbolName }).contains("total", "count", "Main", "user", "mode")
    }

    @Test
    fun `builds procedure declarations`() {
        val source = """
            procedure LogStatus()
               ? "ok"
            return
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val functions = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().toList()

        assertThat(functions).hasSize(1)
        assertThat(functions.first().symbolName).isEqualTo("LogStatus")
    }

    @Test
    fun `function ranges include end markers for nesting`() {
        val source = """
            function Main()
               local count
               return count
            endfunction
            local after
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val function = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().first()

        val expectedEnd = source.indexOf("endfunction") + "endfunction".length
        assertThat(function.textRange.endOffset).isEqualTo(expectedEnd)
    }

    @Test
    fun `function range is implicitly terminated by next declaration keyword`() {
        val source = """
            function AllFilesExist(aFiles)
               return .t.

            function CenterPos(aSize, aRefSize)
               return 1
            endfunction
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val functions = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().toList()

        assertThat(functions).hasSize(2)
        val firstFunctionEndExpected = source.indexOf("function CenterPos") - 2
        assertThat(functions[0].symbolName).isEqualTo("AllFilesExist")
        assertThat(functions[0].textRange.endOffset).isEqualTo(firstFunctionEndExpected)
        assertThat(functions[0].text.trimEnd()).endsWith("return .t.")
        assertThat(functions[1].symbolName).isEqualTo("CenterPos")
    }

    @Test
    fun `procedure range is implicitly terminated by next declaration keyword`() {
        val source = """
            procedure ChangePos(oXbp, aDistance)
               return

            function Ok2SaveDlgSize(lSaveSize)
               return .t.
            endfunction
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val declarations = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().toList()

        assertThat(declarations.map { it.symbolName }).containsExactly("ChangePos", "Ok2SaveDlgSize")
        val firstDeclarationEndExpected = source.indexOf("function Ok2SaveDlgSize") - 2
        assertThat(declarations[0].textRange.endOffset).isEqualTo(firstDeclarationEndExpected)
        assertThat(declarations[0].text.trimEnd()).endsWith("return")
    }

    @Test
    fun `declaration ranges close on explicit end markers even when mismatched`() {
        val source = """
            function Main()
               return 1
            endproc
            local afterMain
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val function = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().first()
        val afterMain = root.walk().filterIsInstance<XbPsiVariableDeclaration>().first { it.symbolName == "afterMain" }

        assertThat(function.symbolName).isEqualTo("Main")
        assertThat(function.textRange.endOffset).isLessThan(afterMain.textRange.startOffset)
        assertThat(function.text.trimEnd()).endsWith("endproc")
    }

    @Test
    fun `assigns storage classes for variable declarations`() {
        val source = """
            function Main(param)
               local localVar
               static staticVar
               private privateVar
               public publicVar
               global globalVar
               return param + localVar + staticVar + privateVar + publicVar + globalVar
            endfunction
        """.trimIndent()

        val declarations = XbPsiTextBuilder().build(source)
            .walk()
            .filterIsInstance<XbPsiVariableDeclaration>()
            .toList()
            .associateBy { it.symbolName }

        assertThat(declarations["param"]?.storageClass).isEqualTo(XbVariableStorageClass.LOCAL)
        assertThat(declarations["localVar"]?.storageClass).isEqualTo(XbVariableStorageClass.LOCAL)
        assertThat(declarations["staticVar"]?.storageClass).isEqualTo(XbVariableStorageClass.STATIC)
        assertThat(declarations["privateVar"]?.storageClass).isEqualTo(XbVariableStorageClass.PRIVATE)
        assertThat(declarations["publicVar"]?.storageClass).isEqualTo(XbVariableStorageClass.PUBLIC)
        assertThat(declarations["globalVar"]?.storageClass).isEqualTo(XbVariableStorageClass.GLOBAL)
    }

    @Test
    fun `method range is implicitly terminated by following class method declaration`() {
        val source = """
            CLASS DbaseF5
                CLASS METHOD load(...)
                CLASS METHOD findBy(...)
            ENDCLASS
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val declarations = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().toList()

        assertThat(declarations.map { it.symbolName }).containsExactly("load", "findBy")
        assertThat(declarations[0].text.trimEnd()).isEqualTo("METHOD load(...)")
        val secondMethodStart = source.indexOf("CLASS METHOD findBy")
        assertThat(declarations[0].textRange.endOffset)
            .isLessThanOrEqualTo(secondMethodStart)
    }

    @Test
    fun `method range is implicitly terminated by class terminator`() {
        val source = """
            CLASS DbaseF5
                CLASS METHOD load(...)
            ENDCLASS
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val declaration = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().single()

        assertThat(declaration.symbolName).isEqualTo("load")
        assertThat(declaration.text.trimEnd()).isEqualTo("METHOD load(...)")
        assertThat(declaration.textRange.endOffset)
            .isLessThan(source.indexOf("ENDCLASS"))
    }

    @Test
    fun `creates nested blocks for class and method declarations`() {
        val source = """
            CLASS Customer
                METHOD Init(name)
                    local copied
                    copied := name
                ENDMETHOD
            ENDCLASS
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val classBlock = root.children.filterIsInstance<XbPsiBlock>().single()
        val method = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().single()
        val methodBlock = method.children.filterIsInstance<XbPsiBlock>().single()

        assertThat(classBlock.text.trim()).startsWith("CLASS Customer")
        assertThat(classBlock.text.trim()).endsWith("ENDCLASS")
        assertThat(method.symbolName).isEqualTo("Init")
        assertThat(methodBlock.text.trim()).startsWith("local copied")
        assertThat(methodBlock.text.trim()).endsWith("ENDMETHOD")
        assertThat(classBlock.children).contains(method)
        assertThat(methodBlock.children.filterIsInstance<XbPsiVariableDeclaration>().map { it.symbolName })
            .containsExactly("copied")
    }

    @Test
    fun `creates nested blocks for procedures and keeps trailing globals at file level`() {
        val source = """
            procedure UpdateTotals(total)
                local value
                return value
            endproc
            global SharedTotal
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val procedure = root.walk().filterIsInstance<XbPsiFunctionDeclaration>().single()
        val procedureBlock = procedure.children.filterIsInstance<XbPsiBlock>().single()
        val trailingGlobal = root.children.filterIsInstance<XbPsiVariableDeclaration>().single()

        assertThat(procedure.symbolName).isEqualTo("UpdateTotals")
        assertThat(procedureBlock.children.filterIsInstance<XbPsiVariableDeclaration>().map { it.symbolName })
            .containsExactly("value")
        assertThat(trailingGlobal.symbolName).isEqualTo("SharedTotal")
        assertThat(trailingGlobal.parent).isEqualTo(root)
    }

    @Test
    fun `captures method declaration without end marker inside class block`() {
        val source = """
            CLASS Product
                METHOD Save()
                    return .t.
            ENDCLASS
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val classBlock = root.children.filterIsInstance<XbPsiBlock>().single()
        val method = classBlock.children.filterIsInstance<XbPsiFunctionDeclaration>().single()

        assertThat(method.text.trimEnd()).startsWith("METHOD Save()")
        assertThat(method.text).contains("return .t.")
        assertThat(method.children.filterIsInstance<XbPsiBlock>()).hasSize(1)
        assertThat(method.children.filterIsInstance<XbPsiBlock>().single().text.trim()).isEqualTo("return .t.")
    }

    @Test
    fun `keeps parent pointers when nesting declarations into blocks`() {
        val source = """
            function Main(arg)
               local count
               return count
            endfunction
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val function = root.children.filterIsInstance<XbPsiFunctionDeclaration>().single()
        val functionBlock = function.children.filterIsInstance<XbPsiBlock>().single()
        val localDeclaration = functionBlock.children.filterIsInstance<XbPsiVariableDeclaration>()
            .first { it.symbolName == "count" }

        assertThat(function.parent).isEqualTo(root)
        assertThat(functionBlock.parent).isEqualTo(function)
        assertThat(localDeclaration.parent).isEqualTo(functionBlock)
    }


}
