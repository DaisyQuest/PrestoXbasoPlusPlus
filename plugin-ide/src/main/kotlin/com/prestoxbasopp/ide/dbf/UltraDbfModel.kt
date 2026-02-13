package com.prestoxbasopp.ide.dbf

import java.nio.charset.StandardCharsets
import kotlin.math.min

enum class DbfFieldType(val code: Char) {
    Character('C'),
    Date('D'),
    FloatingPoint('F'),
    Logical('L'),
    Memo('M'),
    Numeric('N');

    companion object {
        fun fromCode(code: Byte): DbfFieldType? = entries.firstOrNull { it.code.code.toByte() == code }
    }
}

data class DbfHeader(
    val versionByte: Int,
    val lastUpdateYearSince1900: Int,
    val lastUpdateMonth: Int,
    val lastUpdateDay: Int,
    val recordCount: Int,
    val headerLength: Int,
    val recordLength: Int,
    val incompleteTransactionFlag: Int,
    val encryptionFlag: Int,
    val hasProductionMdx: Boolean,
    val languageDriverId: Int,
)

data class DbfFieldDescriptor(
    val name: String,
    val type: DbfFieldType,
    val length: Int,
    val decimalCount: Int,
    val workAreaId: Int,
    val hasProductionMdxTag: Boolean,
)

data class DbfRecord(
    val deleted: Boolean,
    val values: MutableMap<String, String>,
)

data class DbfTable(
    val header: DbfHeader,
    val fields: List<DbfFieldDescriptor>,
    val records: MutableList<DbfRecord>,
)

object UltraDbfCodec {
    fun parse(rawBytes: ByteArray): DbfTable {
        require(rawBytes.size >= 33) { "DBF file too small to contain a valid level-5 header." }
        val header = parseHeader(rawBytes)
        val fieldDescriptors = parseFieldDescriptors(rawBytes, header.headerLength)
        val records = parseRecords(rawBytes, header, fieldDescriptors)
        return DbfTable(header = header, fields = fieldDescriptors, records = records.toMutableList())
    }

    fun serialize(table: DbfTable): ByteArray {
        val fieldCount = table.fields.size
        val headerLength = 32 + (32 * fieldCount) + 1
        val recordLength = 1 + table.fields.sumOf { it.length }
        val recordCount = table.records.size
        val result = ByteArray(headerLength + (recordCount * recordLength) + 1)

        result[0] = table.header.versionByte.toByte()
        result[1] = table.header.lastUpdateYearSince1900.toByte()
        result[2] = table.header.lastUpdateMonth.toByte()
        result[3] = table.header.lastUpdateDay.toByte()
        writeInt32Le(result, 4, recordCount)
        writeInt16Le(result, 8, headerLength)
        writeInt16Le(result, 10, recordLength)
        result[14] = table.header.incompleteTransactionFlag.toByte()
        result[15] = table.header.encryptionFlag.toByte()
        result[28] = if (table.header.hasProductionMdx) 1 else 0
        result[29] = table.header.languageDriverId.toByte()

        var offset = 32
        table.fields.forEach { field ->
            writeFieldDescriptor(result, offset, field)
            offset += 32
        }
        result[offset] = 0x0D
        offset += 1

        table.records.forEach { record ->
            result[offset] = if (record.deleted) 0x2A else 0x20
            var fieldOffset = offset + 1
            table.fields.forEach { field ->
                val value = record.values[field.name].orEmpty()
                val encoded = formatFieldValue(value, field)
                encoded.copyInto(result, fieldOffset)
                fieldOffset += field.length
            }
            offset += recordLength
        }
        result[offset] = 0x1A
        return result
    }

    private fun formatFieldValue(value: String, field: DbfFieldDescriptor): ByteArray {
        val normalized = when (field.type) {
            DbfFieldType.Numeric, DbfFieldType.FloatingPoint, DbfFieldType.Memo -> value.trim().padStart(field.length, ' ')
            else -> value.padEnd(field.length, ' ')
        }
        return normalized.take(field.length).toByteArray(StandardCharsets.US_ASCII)
    }

    private fun parseHeader(rawBytes: ByteArray): DbfHeader {
        val headerLength = readInt16Le(rawBytes, 8)
        require(headerLength >= 33) { "Invalid DBF header length: $headerLength" }
        return DbfHeader(
            versionByte = rawBytes[0].toUByte().toInt(),
            lastUpdateYearSince1900 = rawBytes[1].toUByte().toInt(),
            lastUpdateMonth = rawBytes[2].toUByte().toInt(),
            lastUpdateDay = rawBytes[3].toUByte().toInt(),
            recordCount = readInt32Le(rawBytes, 4),
            headerLength = headerLength,
            recordLength = readInt16Le(rawBytes, 10),
            incompleteTransactionFlag = rawBytes[14].toUByte().toInt(),
            encryptionFlag = rawBytes[15].toUByte().toInt(),
            hasProductionMdx = rawBytes[28].toInt() == 1,
            languageDriverId = rawBytes[29].toUByte().toInt(),
        )
    }

    private fun parseFieldDescriptors(rawBytes: ByteArray, headerLength: Int): List<DbfFieldDescriptor> {
        val descriptors = mutableListOf<DbfFieldDescriptor>()
        var offset = 32
        while (offset < headerLength - 1) {
            val marker = rawBytes[offset].toUByte().toInt()
            if (marker == 0x0D) {
                break
            }
            val fieldType = DbfFieldType.fromCode(rawBytes[offset + 11])
                ?: error("Unsupported DBF field type '${rawBytes[offset + 11].toInt().toChar()}' at descriptor offset $offset")
            val nameBytes = rawBytes.copyOfRange(offset, offset + 11)
            val zeroIndex = nameBytes.indexOf(0)
            val effectiveLength = if (zeroIndex == -1) nameBytes.size else zeroIndex
            val name = String(nameBytes, 0, effectiveLength, StandardCharsets.US_ASCII).trim()
            require(name.isNotEmpty()) { "DBF field name at descriptor offset $offset is empty." }
            descriptors += DbfFieldDescriptor(
                name = name,
                type = fieldType,
                length = rawBytes[offset + 16].toUByte().toInt(),
                decimalCount = rawBytes[offset + 17].toUByte().toInt(),
                workAreaId = readInt16Le(rawBytes, offset + 18),
                hasProductionMdxTag = rawBytes[offset + 31].toInt() == 1,
            )
            offset += 32
        }
        return descriptors
    }

    private fun parseRecords(rawBytes: ByteArray, header: DbfHeader, fields: List<DbfFieldDescriptor>): List<DbfRecord> {
        if (fields.isEmpty()) return emptyList()
        val records = mutableListOf<DbfRecord>()
        var offset = header.headerLength
        repeat(header.recordCount) {
            if (offset + header.recordLength > rawBytes.size) {
                return@repeat
            }
            val deleted = rawBytes[offset].toInt() == 0x2A
            val values = linkedMapOf<String, String>()
            var fieldOffset = offset + 1
            fields.forEach { field ->
                val end = min(fieldOffset + field.length, rawBytes.size)
                val rawField = String(rawBytes, fieldOffset, end - fieldOffset, StandardCharsets.US_ASCII)
                values[field.name] = if (field.type == DbfFieldType.Character) rawField.trimEnd() else rawField.trim()
                fieldOffset += field.length
            }
            records += DbfRecord(deleted = deleted, values = values)
            offset += header.recordLength
        }
        return records
    }

    private fun writeFieldDescriptor(target: ByteArray, start: Int, field: DbfFieldDescriptor) {
        val bytes = field.name.take(11).toByteArray(StandardCharsets.US_ASCII)
        bytes.copyInto(target, destinationOffset = start)
        target[start + 11] = field.type.code.code.toByte()
        target[start + 16] = field.length.toByte()
        target[start + 17] = field.decimalCount.toByte()
        writeInt16Le(target, start + 18, field.workAreaId)
        target[start + 31] = if (field.hasProductionMdxTag) 1 else 0
    }

    private fun readInt16Le(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toUByte().toInt()) or (bytes[offset + 1].toUByte().toInt() shl 8)

    private fun readInt32Le(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toUByte().toInt()) or
            (bytes[offset + 1].toUByte().toInt() shl 8) or
            (bytes[offset + 2].toUByte().toInt() shl 16) or
            (bytes[offset + 3].toUByte().toInt() shl 24)

    private fun writeInt16Le(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }

    private fun writeInt32Le(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }
}
