package com.prestoxbasopp.ide.modules

import java.nio.file.Path
import kotlin.io.path.name

open class XbModuleNamingPolicy {
    open fun suggestName(moduleRoot: Path): String {
        val name = moduleRoot.name
        return if (name.isBlank()) "XBase++ Module" else name
    }
}
