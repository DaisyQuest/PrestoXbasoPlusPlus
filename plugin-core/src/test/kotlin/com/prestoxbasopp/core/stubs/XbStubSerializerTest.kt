package com.prestoxbasopp.core.stubs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbStubSerializerTest {
    @Test
    fun `stub round trip via snapshot preserves data`() {
        val stub = XbStub(
            stubId = "FUNCTION:global.run",
            fqName = "global.run",
            name = "run",
            type = XbStubType.FUNCTION,
        )

        val snapshot = XbStubSerializer.toSnapshot(stub)
        val restored = XbStubSerializer.fromSnapshot(snapshot)

        assertThat(restored).isEqualTo(stub)
    }
}
