package com.prestoxbasopp.core.psi

data class XbPsiIntegrityViolation(
    val message: String,
    val element: XbPsiElement,
)

class XbPsiIntegrityValidator {
    fun validate(root: XbPsiElement): List<XbPsiIntegrityViolation> {
        val violations = mutableListOf<XbPsiIntegrityViolation>()
        validateElement(root, violations)
        return violations
    }

    private fun validateElement(element: XbPsiElement, violations: MutableList<XbPsiIntegrityViolation>) {
        val range = element.textRange
        if (range.endOffset - range.startOffset != element.text.length) {
            violations.add(
                XbPsiIntegrityViolation(
                    "Text length does not match text range length for ${element.elementType}",
                    element,
                ),
            )
        }

        if (element is XbPsiSymbol && element.symbolName.isBlank()) {
            violations.add(
                XbPsiIntegrityViolation(
                    "Symbol name must be non-blank for ${element.elementType}",
                    element,
                ),
            )
        }

        if (element is XbPsiFunctionDeclaration) {
            val duplicates = element.parameters.groupBy { it }.filterValues { it.size > 1 }
            if (duplicates.isNotEmpty()) {
                violations.add(
                    XbPsiIntegrityViolation(
                        "Function parameters must be unique for ${element.symbolName}",
                        element,
                    ),
                )
            }
        }

        if (element.children.isNotEmpty()) {
            val composedText = element.children.joinToString("") { it.text }
            if (composedText != element.text) {
                violations.add(
                    XbPsiIntegrityViolation(
                        "Parent text does not match concatenated child text for ${element.elementType}",
                        element,
                    ),
                )
            }
        }

        var lastEnd = range.startOffset
        element.children.forEach { child ->
            if (child.parent !== element) {
                violations.add(
                    XbPsiIntegrityViolation(
                        "Child parent pointer mismatch for ${element.elementType}",
                        element,
                    ),
                )
            }
            if (child.textRange.startOffset < range.startOffset || child.textRange.endOffset > range.endOffset) {
                violations.add(
                    XbPsiIntegrityViolation(
                        "Child range outside parent range for ${element.elementType}",
                        element,
                    ),
                )
            }
            if (child.textRange.startOffset < lastEnd) {
                violations.add(
                    XbPsiIntegrityViolation(
                        "Child ranges overlap or are out of order for ${element.elementType}",
                        element,
                    ),
                )
            }
            lastEnd = child.textRange.endOffset
            validateElement(child, violations)
        }
    }
}
