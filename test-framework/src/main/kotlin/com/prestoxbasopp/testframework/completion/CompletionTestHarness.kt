package com.prestoxbasopp.testframework.completion

import com.prestoxbasopp.testframework.operations.OperationDefinition
import com.prestoxbasopp.testframework.operations.OperationsRegistry
import java.nio.file.Path

fun interface CompletionProvider {
    fun complete(source: String): CompletionCase
}

object CompletionTestHarness {
    fun assertOperation(
        operation: OperationDefinition,
        fixturesRoot: Path,
        provider: CompletionProvider,
    ) {
        val fixture = CompletionFixtureLoader.loadForOperation(operation, fixturesRoot)
        assertFixture(fixture, provider)
    }

    fun assertOperations(
        registry: OperationsRegistry,
        fixturesRoot: Path,
        provider: CompletionProvider,
    ) {
        registry.operations.forEach { operation ->
            if (operation.id.isNullOrBlank()) {
                return@forEach
            }
            assertOperation(operation, fixturesRoot, provider)
        }
    }

    fun assertFixture(
        fixture: CompletionFixture,
        provider: CompletionProvider,
    ) {
        fixture.cases().forEach { (caseId, completionCase) ->
            val actual = provider.complete(completionCase.source)
            CompletionSnapshotHarness.assertCase(caseId, completionCase, actual)
        }
    }
}
