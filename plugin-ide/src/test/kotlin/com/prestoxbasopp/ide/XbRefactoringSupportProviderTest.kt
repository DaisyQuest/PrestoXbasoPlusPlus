package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbRefactoringSupportProviderTest {
    @Test
    fun `extract method handler uses XbExtractFunctionHandler`() {
        val provider = XbRefactoringSupportProvider()

        val handler = provider.extractMethodHandler

        assertThat(handler).isInstanceOf(XbExtractFunctionHandler::class.java)
    }
}
