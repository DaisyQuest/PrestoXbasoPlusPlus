package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbCodeStyleTabSynchronizerTest {
    @Test
    fun `sync updates indent settings and notifies when tab size is positive`() {
        val target = FakeIndentOptionsTarget(indentSize = 4, tabSize = 4, continuationIndentSize = 8)
        var notifyCalls = 0
        val synchronizer = XbIdeCodeStyleTabSynchronizer(
            indentOptionsTargetProvider = { target },
            changeNotifier = { notifyCalls += 1 },
        )

        synchronizer.sync(6)

        assertThat(target.indentSize).isEqualTo(6)
        assertThat(target.tabSize).isEqualTo(6)
        assertThat(target.continuationIndentSize).isEqualTo(6)
        assertThat(notifyCalls).isEqualTo(1)
    }

    @Test
    fun `sync does nothing when tab size is zero or negative`() {
        val target = FakeIndentOptionsTarget(indentSize = 4, tabSize = 4, continuationIndentSize = 8)
        var notifyCalls = 0
        val synchronizer = XbIdeCodeStyleTabSynchronizer(
            indentOptionsTargetProvider = { target },
            changeNotifier = { notifyCalls += 1 },
        )

        synchronizer.sync(0)
        synchronizer.sync(-3)

        assertThat(target.indentSize).isEqualTo(4)
        assertThat(target.tabSize).isEqualTo(4)
        assertThat(target.continuationIndentSize).isEqualTo(8)
        assertThat(notifyCalls).isZero()
    }

    private data class FakeIndentOptionsTarget(
        override var indentSize: Int,
        override var tabSize: Int,
        override var continuationIndentSize: Int,
    ) : XbIndentOptionsTarget
}
