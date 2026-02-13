package com.prestoxbasopp.ide.dbf

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UltraDbfEditorModelTest {
    @Test
    fun `supports adding rows updating values and toggling deleted flag`() {
        val model = UltraDbfEditorModel(baseTable())

        model.addRecord(mapOf("FLAG" to "Y"))
        model.updateValue(0, "NAME", "CHANGED")
        model.toggleDeleted(1)

        assertThat(model.records(true)).hasSize(3)
        assertThat(model.records(true)[0].values["NAME"]).isEqualTo("CHANGED")
        assertThat(model.records(true)[2].values["FLAG"]).isEqualTo("Y")
        assertThat(model.records(true)[1].deleted).isFalse()
    }

    @Test
    fun `can hide deleted records`() {
        val model = UltraDbfEditorModel(baseTable())

        val visible = model.records(includeDeleted = false)

        assertThat(visible).hasSize(1)
        assertThat(visible.single().values["NAME"]).isEqualTo("ALICE")
    }

    @Test
    fun `completion returns static and learned suggestions by field type`() {
        val model = UltraDbfEditorModel(baseTable())
        val logicalField = model.fields().single { it.name == "FLAG" }
        val dateField = model.fields().single { it.name == "WHEN" }
        val numericField = model.fields().single { it.name == "TOTAL" }
        val memoField = model.fields().single { it.name == "MEMO" }

        assertThat(model.completionSuggestions(logicalField, "")).contains("Y", "N", "T", "F", "?")
        assertThat(model.completionSuggestions(logicalField, "n")).containsExactly("N")
        assertThat(model.completionSuggestions(dateField, "20")).contains("20240201")
        assertThat(model.completionSuggestions(numericField, "3")).containsExactly("3.14")
        assertThat(model.completionSuggestions(memoField, "000000000")).contains("0000000001", "0000000002")
    }

    @Test
    fun `snapshot is a defensive copy`() {
        val model = UltraDbfEditorModel(baseTable())

        val copy = model.snapshot()
        copy.records[0].values["NAME"] = "OVERRIDE"

        assertThat(model.records(true)[0].values["NAME"]).isEqualTo("ALICE")
    }

    private fun baseTable(): DbfTable = DbfTable(
        header = DbfHeader(3, 124, 1, 1, 2, 97, 20, 0, 0, false, 0),
        fields = listOf(
            DbfFieldDescriptor("NAME", DbfFieldType.Character, 10, 0, 0, false),
            DbfFieldDescriptor("FLAG", DbfFieldType.Logical, 1, 0, 0, false),
            DbfFieldDescriptor("WHEN", DbfFieldType.Date, 8, 0, 0, false),
            DbfFieldDescriptor("TOTAL", DbfFieldType.Numeric, 8, 2, 0, false),
            DbfFieldDescriptor("MEMO", DbfFieldType.Memo, 10, 0, 0, false),
        ),
        records = mutableListOf(
            DbfRecord(false, mutableMapOf("NAME" to "ALICE", "FLAG" to "Y", "WHEN" to "20240201", "TOTAL" to "42", "MEMO" to "123")),
            DbfRecord(true, mutableMapOf("NAME" to "BOB", "FLAG" to "N", "WHEN" to "20230105", "TOTAL" to "7", "MEMO" to "456")),
        ),
    )
}
