package com.prestoxbasopp.core.stubs

import com.prestoxbasopp.core.api.XbStubElementContract

enum class XbStubType {
    FUNCTION,
    VARIABLE,
}

data class XbStub(
    override val stubId: String,
    override val fqName: String?,
    val name: String?,
    val type: XbStubType,
) : XbStubElementContract
