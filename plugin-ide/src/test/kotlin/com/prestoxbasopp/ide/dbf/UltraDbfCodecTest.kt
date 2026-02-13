package com.prestoxbasopp.ide.dbf

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class UltraDbfCodecTest {
    @Test
    fun `round-trips level-5 dbf table including deleted records`() {
        val table = fixtureTable()

        val bytes = UltraDbfCodec.serialize(table)
        val parsed = UltraDbfCodec.parse(bytes)

        assertThat(parsed.header.recordCount).isEqualTo(2)
        assertThat(parsed.header.recordLength).isEqualTo(20)
        assertThat(parsed.fields).hasSize(4)
        assertThat(parsed.records[0].deleted).isFalse()
        assertThat(parsed.records[1].deleted).isTrue()
        assertThat(parsed.records[0].values).containsEntry("NAME", "ALICE")
        assertThat(parsed.records[0].values).containsEntry("ACTIVE", "Y")
        assertThat(parsed.records[1].values).containsEntry("BALANCE", "-7.5")
        assertThat(parsed.records[1].values).containsEntry("BIRTHDAY", "20240131")
        assertThat(bytes.last().toInt() and 0xFF).isEqualTo(0x1A)
    }

    @Test
    fun `parses gracefully when record count is larger than available bytes`() {
        val table = fixtureTable().copy(records = mutableListOf(fixtureTable().records.first()))
        val encoded = UltraDbfCodec.serialize(table)
        encoded[4] = 2 // lie: two rows

        val parsed = UltraDbfCodec.parse(encoded)

        assertThat(parsed.records).hasSize(1)
    }

    @Test
    fun `rejects files that are too small`() {
        assertThatThrownBy { UltraDbfCodec.parse(byteArrayOf(0x03, 0x01)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("too small")
    }

    @Test
    fun `rejects invalid header length`() {
        val raw = ByteArray(40)
        raw[8] = 1
        raw[9] = 0

        assertThatThrownBy { UltraDbfCodec.parse(raw) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Invalid DBF header length")
    }

    @Test
    fun `fails on unsupported field type`() {
        val raw = ByteArray(200)
        raw[8] = 65 // header len little endian
        raw[10] = 2 // record len little endian
        raw[32] = 'X'.code.toByte()
        raw[32 + 11] = 'Q'.code.toByte()
        raw[64] = 0x0D

        assertThatThrownBy { UltraDbfCodec.parse(raw) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Unsupported DBF field type")
    }

    @Test
    fun `fails on empty field name`() {
        val raw = ByteArray(200)
        raw[8] = 65
        raw[10] = 2
        raw[32 + 11] = 'C'.code.toByte()
        raw[64] = 0x0D

        assertThatThrownBy { UltraDbfCodec.parse(raw) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("field name")
    }

    private fun fixtureTable(): DbfTable {
        val fields = listOf(
            DbfFieldDescriptor("NAME", DbfFieldType.Character, length = 6, decimalCount = 0, workAreaId = 0, hasProductionMdxTag = false),
            DbfFieldDescriptor("ACTIVE", DbfFieldType.Logical, length = 1, decimalCount = 0, workAreaId = 0, hasProductionMdxTag = true),
            DbfFieldDescriptor("BALANCE", DbfFieldType.Numeric, length = 4, decimalCount = 1, workAreaId = 0, hasProductionMdxTag = false),
            DbfFieldDescriptor("BIRTHDAY", DbfFieldType.Date, length = 8, decimalCount = 0, workAreaId = 0, hasProductionMdxTag = false),
        )
        return DbfTable(
            header = DbfHeader(
                versionByte = 0x03,
                lastUpdateYearSince1900 = 124,
                lastUpdateMonth = 1,
                lastUpdateDay = 31,
                recordCount = 2,
                headerLength = 0,
                recordLength = 0,
                incompleteTransactionFlag = 0,
                encryptionFlag = 0,
                hasProductionMdx = true,
                languageDriverId = 0x57,
            ),
            fields = fields,
            records = mutableListOf(
                DbfRecord(false, mutableMapOf("NAME" to "ALICE", "ACTIVE" to "Y", "BALANCE" to "12.3", "BIRTHDAY" to "20240201")),
                DbfRecord(true, mutableMapOf("NAME" to "BOB", "ACTIVE" to "N", "BALANCE" to "-7.5", "BIRTHDAY" to "20240131")),
            ),
        )
    }
}
