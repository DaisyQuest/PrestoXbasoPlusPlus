package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class XbCompletionMetadataRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `loads metadata from project file when available`() {
        val json = """
            {
              "commands": [
                {
                  "name": "DCSAY",
                  "attributes": [
                    { "name": "SAYSIZE", "type": "NUMERIC" }
                  ]
                }
              ]
            }
        """.trimIndent()
        val file = tempDir.resolve("xbasepp.completion.json")
        file.toFile().writeText(json)

        val repository = XbCompletionMetadataRepository()
        val metadata = repository.load(tempDir)

        assertThat(metadata.commands).hasSize(1)
        assertThat(metadata.commands.first().attributes.first().name).isEqualTo("SAYSIZE")
    }
}
