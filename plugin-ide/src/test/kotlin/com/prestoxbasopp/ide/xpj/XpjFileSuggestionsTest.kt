package com.prestoxbasopp.ide.xpj

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class XpjFileSuggestionsTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun `suggests project files by prefix with relevant ranking`() {
        Files.createDirectories(root.resolve("src"))
        Files.writeString(root.resolve("src/myprogram.prg"), "")
        Files.writeString(root.resolve("src/myprogram2.prg"), "")
        Files.writeString(root.resolve("src/other.arc"), "")

        val suggestions = XpjFileSuggestions(root).suggest("myprogra")

        assertThat(suggestions).startsWith("src/myprogram.prg", "src/myprogram2.prg")
    }

    @Test
    fun `returns empty suggestions for blank input and unsupported extensions`() {
        Files.writeString(root.resolve("notes.txt"), "")
        val provider = XpjFileSuggestions(root)

        assertThat(provider.suggest(" ")).isEmpty()
        assertThat(provider.suggest("notes")).isEmpty()
    }
}
