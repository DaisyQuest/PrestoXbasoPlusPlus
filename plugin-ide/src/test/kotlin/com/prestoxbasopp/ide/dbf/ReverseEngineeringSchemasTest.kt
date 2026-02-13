package com.prestoxbasopp.ide.dbf

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReverseEngineeringSchemasTest {
    @Test
    fun `metadata schema exposes required top level fields`() {
        val schema = loadSchema("reverse-engineering/dbf-metadata.schema.json")
        assertThat(schema).contains("\"schemaVersion\"", "\"engineVersion\"", "\"tables\"")
        assertThat(schema).contains("\"candidatePrimaryKey\"", "\"candidateForeignKeys\"")
    }

    @Test
    fun `config schema exposes api profile enum`() {
        val schema = loadSchema("reverse-engineering/dbf-reverseengineer-config.schema.json")
        assertThat(schema).contains("READ_ONLY", "CRUD_BASIC", "CRUD_RELATIONAL", "FULL")
        assertThat(schema).contains("\"generateMethodAliases\"")
    }

    @Test
    fun `generation report schema contains deterministic counters`() {
        val schema = loadSchema("reverse-engineering/dbf-generation-report.schema.json")
        assertThat(schema).contains("\"generatedFileCount\"", "\"warnings\"")
        assertThat(schema).contains("\"minimum\": 0")
    }

    private fun loadSchema(path: String): String = checkNotNull(javaClass.classLoader.getResource(path))
        .readText()
}
