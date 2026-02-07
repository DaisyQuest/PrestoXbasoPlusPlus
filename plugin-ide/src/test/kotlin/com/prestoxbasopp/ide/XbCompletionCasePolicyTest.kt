package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbCompletionCasePolicyTest {
    @Test
    fun `uses case insensitive completion for lowercase prefix`() {
        assertThat(XbCompletionCasePolicy.isCaseSensitive("alpha")).isFalse()
    }

    @Test
    fun `uses case sensitive completion when uppercase is present`() {
        assertThat(XbCompletionCasePolicy.isCaseSensitive("Al")).isTrue()
        assertThat(XbCompletionCasePolicy.isCaseSensitive("aL")).isTrue()
    }
}
