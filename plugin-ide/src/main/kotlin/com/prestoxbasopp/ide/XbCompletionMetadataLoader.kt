package com.prestoxbasopp.ide

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.Reader

class XbCompletionMetadataLoader(
    private val gson: Gson = Gson(),
) {
    fun load(reader: Reader): XbCompletionMetadata {
        return try {
            gson.fromJson(reader, XbCompletionMetadata::class.java) ?: XbCompletionMetadata()
        } catch (_: JsonSyntaxException) {
            XbCompletionMetadata()
        }
    }
}
