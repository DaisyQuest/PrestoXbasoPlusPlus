package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiElementType
import com.prestoxbasopp.core.psi.XbPsiSnapshot

private val SupportedLiteralTypes = setOf("number", "string", "boolean")

private data class XbTypeConversionResult(
    val updatedText: String? = null,
    val error: String? = null,
)

data class XbTypeMigrationEdit(
    val textRange: XbTextRange,
    val replacement: String,
    val fromType: String,
    val toType: String,
)

data class XbTypeMigrationTarget(
    val sourceId: String,
    val snapshot: XbPsiSnapshot,
)

data class XbProjectTypeMigrationEdit(
    val sourceId: String,
    val textRange: XbTextRange,
    val replacement: String,
    val fromType: String,
    val toType: String,
)

data class XbTypeMigrationResult(
    val updatedSnapshot: XbPsiSnapshot,
    val edits: List<XbTypeMigrationEdit>,
    val errors: List<String>,
)

data class XbProjectTypeMigrationResult(
    val updatedTargets: List<XbTypeMigrationTarget>,
    val edits: List<XbProjectTypeMigrationEdit>,
    val errors: List<String>,
)

class XbTypeMigrationRefactoring {
    fun migrate(snapshot: XbPsiSnapshot, fromType: String, toType: String): XbTypeMigrationResult {
        val validationError = validateTypes(fromType, toType)
        if (validationError != null) {
            return XbTypeMigrationResult(snapshot, emptyList(), listOf(validationError))
        }
        if (fromType == toType) {
            return XbTypeMigrationResult(snapshot, emptyList(), emptyList())
        }
        val edits = mutableListOf<XbTypeMigrationEdit>()
        val errors = mutableListOf<String>()
        val updated = migrateInSnapshot(snapshot, fromType, toType, edits, errors)
        return XbTypeMigrationResult(updated, edits, errors)
    }

    fun migrateProject(
        targets: List<XbTypeMigrationTarget>,
        fromType: String,
        toType: String,
    ): XbProjectTypeMigrationResult {
        val validationError = validateTypes(fromType, toType)
        if (validationError != null) {
            return XbProjectTypeMigrationResult(targets, emptyList(), listOf(validationError))
        }
        if (fromType == toType) {
            return XbProjectTypeMigrationResult(targets, emptyList(), emptyList())
        }
        val edits = mutableListOf<XbProjectTypeMigrationEdit>()
        val errors = mutableListOf<String>()
        val updatedTargets = targets.map { target ->
            val localEdits = mutableListOf<XbTypeMigrationEdit>()
            val localErrors = mutableListOf<String>()
            val updatedSnapshot = migrateInSnapshot(target.snapshot, fromType, toType, localEdits, localErrors)
            localEdits.forEach { edit ->
                edits += XbProjectTypeMigrationEdit(
                    sourceId = target.sourceId,
                    textRange = edit.textRange,
                    replacement = edit.replacement,
                    fromType = edit.fromType,
                    toType = edit.toType,
                )
            }
            localErrors.forEach { error ->
                errors += "${target.sourceId}: $error"
            }
            target.copy(snapshot = updatedSnapshot)
        }
        return XbProjectTypeMigrationResult(updatedTargets, edits, errors)
    }

    private fun migrateInSnapshot(
        snapshot: XbPsiSnapshot,
        fromType: String,
        toType: String,
        edits: MutableList<XbTypeMigrationEdit>,
        errors: MutableList<String>,
    ): XbPsiSnapshot {
        val migratedChildren = snapshot.children.map { migrateInSnapshot(it, fromType, toType, edits, errors) }
        val isLiteral = snapshot.elementType == XbPsiElementType.LITERAL
        val literalKind = snapshot.literalKind
        if (!isLiteral || literalKind != fromType) {
            return snapshot.copy(children = migratedChildren)
        }
        val conversion = convertLiteral(snapshot.text, fromType, toType)
        val updatedText = conversion.updatedText
        if (updatedText == null) {
            errors += conversion.error ?: "Unsupported type migration from $fromType to $toType."
            return snapshot.copy(children = migratedChildren)
        }
        edits += XbTypeMigrationEdit(snapshot.textRange, updatedText, fromType, toType)
        return snapshot.copy(
            text = updatedText,
            literalKind = toType,
            children = migratedChildren,
        )
    }

    private fun convertLiteral(text: String, fromType: String, toType: String): XbTypeConversionResult {
        if (fromType !in SupportedLiteralTypes || toType !in SupportedLiteralTypes) {
            return XbTypeConversionResult(error = "Unsupported type migration from $fromType to $toType.")
        }
        return when (fromType) {
            "number" -> convertNumber(text, toType)
            "string" -> convertString(text, toType)
            "boolean" -> convertBoolean(text, toType)
            else -> XbTypeConversionResult(error = "Unsupported type migration from $fromType to $toType.")
        }
    }

    private fun convertNumber(text: String, toType: String): XbTypeConversionResult {
        return when (toType) {
            "string" -> XbTypeConversionResult(updatedText = "\"$text\"")
            "boolean" -> when (text) {
                "0" -> XbTypeConversionResult(updatedText = "false")
                "1" -> XbTypeConversionResult(updatedText = "true")
                else -> XbTypeConversionResult(error = "Numeric literal '$text' cannot be converted to boolean.")
            }
            "number" -> XbTypeConversionResult(updatedText = text)
            else -> XbTypeConversionResult(error = "Unsupported type migration from number to $toType.")
        }
    }

    private fun convertString(text: String, toType: String): XbTypeConversionResult {
        val unwrapped = stripQuotes(text.trim()) ?: text.trim()
        return when (toType) {
            "number" -> if (isNumberLiteral(unwrapped)) {
                XbTypeConversionResult(updatedText = unwrapped)
            } else {
                XbTypeConversionResult(error = "Literal '$text' is not a valid number literal.")
            }
            "boolean" -> {
                val normalized = normalizeBoolean(unwrapped)
                if (normalized != null) {
                    XbTypeConversionResult(updatedText = normalized)
                } else {
                    XbTypeConversionResult(error = "Literal '$text' is not a valid boolean literal.")
                }
            }
            "string" -> XbTypeConversionResult(updatedText = "\"$unwrapped\"")
            else -> XbTypeConversionResult(error = "Unsupported type migration from string to $toType.")
        }
    }

    private fun convertBoolean(text: String, toType: String): XbTypeConversionResult {
        val normalized = normalizeBoolean(text.trim())
            ?: return XbTypeConversionResult(error = "Literal '$text' is not a valid boolean literal.")
        return when (toType) {
            "string" -> XbTypeConversionResult(updatedText = "\"$normalized\"")
            "number" -> XbTypeConversionResult(updatedText = if (normalized == "true") "1" else "0")
            "boolean" -> XbTypeConversionResult(updatedText = normalized)
            else -> XbTypeConversionResult(error = "Unsupported type migration from boolean to $toType.")
        }
    }

    private fun validateTypes(fromType: String, toType: String): String? {
        if (fromType.isBlank() || toType.isBlank()) {
            return "Type migration requires non-blank source and target types."
        }
        return null
    }

    private fun normalizeBoolean(text: String): String? {
        return when (text.lowercase()) {
            "true" -> "true"
            "false" -> "false"
            else -> null
        }
    }

    private fun stripQuotes(text: String): String? {
        if (text.length < 2) {
            return null
        }
        val first = text.first()
        val last = text.last()
        if (first != last || (first != '\'' && first != '"')) {
            return null
        }
        return text.substring(1, text.length - 1)
    }

    private fun isNumberLiteral(text: String): Boolean {
        return text.matches(Regex("^-?\\d+(\\.\\d+)?$"))
    }
}
