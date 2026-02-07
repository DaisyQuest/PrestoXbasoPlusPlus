package com.prestoxbasopp.ide.modules

class XbModuleNameResolver {
    fun resolve(baseName: String, existing: Set<String>): String {
        if (baseName !in existing) return baseName
        var index = 2
        var candidate = "$baseName $index"
        while (candidate in existing) {
            index += 1
            candidate = "$baseName $index"
        }
        return candidate
    }
}
