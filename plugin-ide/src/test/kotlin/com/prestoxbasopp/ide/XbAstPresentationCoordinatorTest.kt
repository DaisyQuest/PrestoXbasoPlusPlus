package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.Executor

class XbAstPresentationCoordinatorTest {
    @Test
    fun `emits no-active-editor loading presentation when snapshot text is null`() {
        val loadingStates = mutableListOf<XbAstPresentation>()
        val readyStates = mutableListOf<XbAstPresentation>()
        val uiQueue = mutableListOf<() -> Unit>()
        val executor = Executor { command -> command.run() }
        val presenter = XbAstPresenter()

        val coordinator = XbAstPresentationCoordinator(
            presenter = presenter,
            executor = executor,
            uiDispatcher = { runnable -> uiQueue.add(runnable) },
        )

        coordinator.schedule(
            snapshot = XbAstDocumentSnapshot(fileName = null, text = null),
            onLoading = { loadingStates += it },
            onReady = { readyStates += it },
        )

        assertThat(loadingStates).hasSize(1)
        assertThat(loadingStates.single().isLoading).isTrue()
        assertThat(loadingStates.single().message).isEqualTo("No active editor.")
        assertThat(readyStates).isEmpty()
        assertThat(uiQueue).hasSize(1)
        uiQueue.single().invoke()
        assertThat(readyStates).hasSize(1)
        assertThat(readyStates.single().isLoading).isFalse()
        assertThat(readyStates.single().message).isEqualTo("No active editor.")
    }

    @Test
    fun `uses large-file loading text when threshold is reached`() {
        val loadingStates = mutableListOf<XbAstPresentation>()
        val uiQueue = mutableListOf<() -> Unit>()
        val executor = Executor { command -> command.run() }
        val presenter = XbAstPresenter()

        val coordinator = XbAstPresentationCoordinator(
            presenter = presenter,
            executor = executor,
            uiDispatcher = { runnable -> uiQueue.add(runnable) },
            largeFileThreshold = 20,
        )

        coordinator.schedule(
            snapshot = XbAstDocumentSnapshot(fileName = "huge.prg", text = "x".repeat(20)),
            onLoading = { loadingStates += it },
            onReady = { },
        )

        assertThat(loadingStates).hasSize(1)
        assertThat(loadingStates.single().isLoading).isTrue()
        assertThat(loadingStates.single().message)
            .isEqualTo("File: huge.prg — Loading AST for large file (20 chars)...")
        assertThat(uiQueue).hasSize(1)
    }

    @Test
    fun `drops stale parse results when a newer revision is scheduled`() {
        val loadingStates = mutableListOf<XbAstPresentation>()
        val readyStates = mutableListOf<XbAstPresentation>()
        val uiQueue = mutableListOf<() -> Unit>()
        val backgroundQueue = mutableListOf<() -> Unit>()
        val executor = Executor { command -> backgroundQueue.add(command::run) }
        val presenter = XbAstPresenter()

        val coordinator = XbAstPresentationCoordinator(
            presenter = presenter,
            executor = executor,
            uiDispatcher = { runnable -> uiQueue.add(runnable) },
        )

        coordinator.schedule(
            snapshot = XbAstDocumentSnapshot(fileName = "first.prg", text = "return 1"),
            onLoading = { loadingStates += it },
            onReady = { readyStates += it },
        )
        coordinator.schedule(
            snapshot = XbAstDocumentSnapshot(fileName = "second.prg", text = "return 2"),
            onLoading = { loadingStates += it },
            onReady = { readyStates += it },
        )

        assertThat(loadingStates).hasSize(2)
        assertThat(backgroundQueue).hasSize(2)

        backgroundQueue.forEach { it.invoke() }
        assertThat(uiQueue).hasSize(2)

        uiQueue.first().invoke()
        assertThat(readyStates).isEmpty()

        uiQueue.last().invoke()
        assertThat(readyStates).hasSize(1)
        assertThat(readyStates.single().message).isEqualTo("File: second.prg — Parser errors: none")
    }
}
