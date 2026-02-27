package com.prestoxbasopp.ui

class InMemoryKeyValueStore : XbKeyValueStore {
    private val booleans = mutableMapOf<String, Boolean>()
    private val ints = mutableMapOf<String, Int>()
    private val strings = mutableMapOf<String, String>()

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return booleans[key] ?: defaultValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return ints[key] ?: defaultValue
    }

    override fun putInt(key: String, value: Int) {
        ints[key] = value
    }

    override fun getString(key: String, defaultValue: String): String {
        return strings[key] ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        strings[key] = value
    }

    fun putRawInt(key: String, value: Int) {
        ints[key] = value
    }

    fun putRawString(key: String, value: String) {
        strings[key] = value
    }
}
