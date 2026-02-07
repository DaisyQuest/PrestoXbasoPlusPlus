package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.ast.XbProgram
import com.prestoxbasopp.testframework.golden.AstDumpFormat
import com.prestoxbasopp.testframework.golden.GoldenFixtureLoader
import com.prestoxbasopp.testframework.golden.GoldenTestHarness
import com.prestoxbasopp.testframework.golden.ParseResult
import com.prestoxbasopp.testframework.operations.OperationsRegistryLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class XbOperationsFixtureTest {
    @Test
    fun `parses operations fixtures with expected AST shapes`() {
        val root = repoRoot()
        val registry = OperationsRegistryLoader.load(root.resolve("spec/xbasepp/operations.yaml"))
        val fixturesRoot = root.resolve("spec/xbasepp/fixtures/operations")
        val cases = registry.operations.map { operation ->
            val fixture = GoldenFixtureLoader.loadForOperation(operation, fixturesRoot)
            fixture.toTestCases<XbProgram>().first()
        }
        GoldenTestHarness.assertCases(cases, ::parseSource, ::dumpProgram)

        registry.operations.forEach { operation ->
            val fixture = GoldenFixtureLoader.loadForOperation(operation, fixturesRoot)
            val result = parseSource(fixture.edgeSource)
            assertThat(result.errors).containsExactlyElementsOf(fixture.expectedErrors)
            val rootShape = result.ast?.toDumpNode()?.children?.firstOrNull()?.name
            assertThat(rootShape).isEqualTo(operation.expectedAstShape)
        }
    }

    private fun parseSource(source: String): ParseResult<XbProgram> {
        val result = XbParser.parse(source)
        return ParseResult(result.program, result.errors)
    }

    private fun dumpProgram(program: XbProgram): String {
        return AstDumpFormat.render(program.toDumpNode())
    }

    private fun repoRoot(): Path {
        var current = Paths.get("").toAbsolutePath()
        while (!Files.exists(current.resolve("spec/xbasepp/operations.yaml")) && current.parent != null) {
            current = current.parent
        }
        return current
    }
}
