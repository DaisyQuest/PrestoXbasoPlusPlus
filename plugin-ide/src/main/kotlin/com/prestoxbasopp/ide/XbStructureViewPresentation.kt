package com.prestoxbasopp.ide

import com.intellij.icons.AllIcons
import com.prestoxbasopp.core.psi.XbPsiElementType
import javax.swing.Icon

internal object XbStructureViewPresentation {
    fun iconFor(item: XbStructureItem): Icon? {
        return when (item.elementType) {
            XbPsiElementType.FUNCTION_DECLARATION -> AllIcons.Nodes.Function
            XbPsiElementType.VARIABLE_DECLARATION -> if (item.isMutable == false) {
                AllIcons.Nodes.Constant
            } else {
                AllIcons.Nodes.Variable
            }
            else -> null
        }
    }
}
