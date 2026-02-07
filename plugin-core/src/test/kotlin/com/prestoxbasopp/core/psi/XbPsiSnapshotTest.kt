package com.prestoxbasopp.core.psi

import com.prestoxbasopp.core.api.XbTextRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbPsiSnapshotTest {
    @Test
    fun `snapshot round trip preserves structure`() {
        val element = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 3),
            text = "foo",
            children = listOf(
                XbPsiSymbolReference(
                    symbolName = "foo",
                    textRange = XbTextRange(0, 3),
                    text = "foo",
                ),
            ),
        )

        val snapshot = XbPsiSnapshot.fromElement(element)
        val reconstructed = XbPsiSnapshot.toElement(snapshot)
        val rebuiltSnapshot = XbPsiSnapshot.fromElement(reconstructed)

        assertThat(rebuiltSnapshot).isEqualTo(snapshot)
    }

    @Test
    fun `literal snapshot defaults are applied`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.LITERAL,
            name = null,
            textRange = XbTextRange(0, 1),
            text = "1",
            literalKind = null,
        )

        val element = XbPsiSnapshot.toElement(snapshot)
        val rebuilt = XbPsiSnapshot.fromElement(element)

        assertThat(rebuilt.literalKind).isEqualTo("literal")
    }

    @Test
    fun `symbol reference snapshots fall back to empty name`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.SYMBOL_REFERENCE,
            name = null,
            textRange = XbTextRange(0, 1),
            text = "x",
        )

        val element = XbPsiSnapshot.toElement(snapshot)

        assertThat(element).isInstanceOf(XbPsiSymbolReference::class.java)
        assertThat((element as XbPsiSymbolReference).symbolName).isEmpty()
    }
}
