package com.prestoxbasopp.core.stubs

data class XbStubSnapshot(
    val stubId: String,
    val fqName: String?,
    val name: String?,
    val type: XbStubType,
)

object XbStubSerializer {
    fun toSnapshot(stub: XbStub): XbStubSnapshot = XbStubSnapshot(
        stubId = stub.stubId,
        fqName = stub.fqName,
        name = stub.name,
        type = stub.type,
    )

    fun fromSnapshot(snapshot: XbStubSnapshot): XbStub = XbStub(
        stubId = snapshot.stubId,
        fqName = snapshot.fqName,
        name = snapshot.name,
        type = snapshot.type,
    )
}
