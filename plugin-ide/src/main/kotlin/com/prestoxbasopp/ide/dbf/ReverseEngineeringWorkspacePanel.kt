package com.prestoxbasopp.ide.dbf

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import java.awt.BorderLayout
import java.awt.GridLayout
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField

data class ReverseEngineeringInput(
    val tableName: String,
    val sourcePath: String,
    val table: DbfTable,
)

class ReverseEngineeringWorkspacePanel(
    private val inputProvider: () -> ReverseEngineeringInput? = { null },
    private val artifactWriter: (ReverseEngineeringInput, ReverseEngineerConfig, List<GeneratedClassArtifact>) -> List<String> = ::writeArtifacts,
) : JPanel(BorderLayout()) {
    private val workflowTabs = JBTabbedPane()
    private val overviewText = readOnlyArea("Reverse Engineering Workflow\nRun Analyze, then Generate.")
    private val metadataText = readOnlyArea("No metadata extracted yet.")
    private val mappingText = readOnlyArea("Model mapping will be generated after analysis.")
    private val relationsText = readOnlyArea("Relations will be inferred after analysis.")
    private val previewText = readOnlyArea("No generated output yet.")
    private val validationText = readOnlyArea("Validation status will appear after actions run.")
    private val logsText = readOnlyArea("Ready.\n")
    private val inputsText = readOnlyArea(inputsSummary())
    private val apiText = readOnlyArea(apiSummary())
    private val generationText = readOnlyArea(generationSummary())
    private val profileCombo = JComboBox(ApiProfile.entries.toTypedArray())
    private val aliasToggle = JCheckBox("Generate method aliases", true)
    private val outputDirField = JTextField("generated")
    private var metadataBundle: DbfMetadataBundle? = null
    private var lastInput: ReverseEngineeringInput? = null

    init {
        profileCombo.name = "reverse.profile"
        aliasToggle.name = "reverse.alias"
        outputDirField.name = "reverse.outputDir"

        val controls = JPanel(GridLayout(2, 1, 8, 8)).apply {
            add(
                JPanel().apply {
                    add(JButton("Analyze").apply {
                        name = "reverse.analyze"
                        addActionListener { analyze() }
                    })
                    add(JLabel("API Profile:"))
                    add(profileCombo)
                    add(aliasToggle)
                },
            )
            add(
                JPanel().apply {
                    add(JLabel("Output Dir:"))
                    add(outputDirField)
                    add(JButton("Generate").apply {
                        name = "reverse.generate"
                        addActionListener { generate() }
                    })
                },
            )
        }

        add(controls, BorderLayout.NORTH)
        add(workflowTabs, BorderLayout.CENTER)
        populateTabs()
    }

    private fun populateTabs() {
        workflowTabs.removeAll()
        workflowTabs.addTab(ReverseEngineeringTab.Overview.title, JBScrollPane(overviewText))
        workflowTabs.addTab(ReverseEngineeringTab.Inputs.title, JBScrollPane(inputsText))
        workflowTabs.addTab(ReverseEngineeringTab.Metadata.title, JBScrollPane(metadataText))
        workflowTabs.addTab(ReverseEngineeringTab.ModelMapping.title, JBScrollPane(mappingText))
        workflowTabs.addTab(ReverseEngineeringTab.Relations.title, JBScrollPane(relationsText))
        workflowTabs.addTab(ReverseEngineeringTab.ApiSurface.title, JBScrollPane(apiText))
        workflowTabs.addTab(ReverseEngineeringTab.GenerationOutput.title, JBScrollPane(generationText))
        workflowTabs.addTab(ReverseEngineeringTab.Preview.title, JBScrollPane(previewText))
        workflowTabs.addTab(ReverseEngineeringTab.Validation.title, JBScrollPane(validationText))
        workflowTabs.addTab(ReverseEngineeringTab.RunLogs.title, JBScrollPane(logsText))
    }

    private fun analyze() {
        val input = inputProvider()
        if (input == null) {
            validationText.text = "Blocking: import a DBF file before running Analyze."
            log("Analyze failed: no DBF input is currently loaded.")
            return
        }

        lastInput = input
        val metadata = ReverseEngineeringWorkflow.extractMetadata(input.tableName, input.sourcePath, input.table)
        metadataBundle = ReverseEngineeringWorkflow.toBundle(metadata)

        inputsText.text = buildInputText(input)
        metadataText.text = buildMetadataText(metadata)
        mappingText.text = buildMappingText(metadata)
        relationsText.text = buildRelationsText(metadata)
        apiText.text = buildApiText(profileCombo.selectedItem as ApiProfile, aliasToggle.isSelected)
        generationText.text = "Awaiting generation. Output directory: ${outputDirField.text.trim().ifBlank { "generated" }}"
        validationText.text = validationIssues(metadata)
        previewText.text = "Analysis complete. Use Generate to render class output."
        workflowTabs.setTitleAt(1, "${ReverseEngineeringTab.Inputs.title} (${input.tableName})")
        log("Analyze complete: ${metadata.tableName} (${metadata.fields.size} fields, ${input.table.records.size} records).")
    }

    private fun generate() {
        val metadata = metadataBundle
        val input = lastInput
        if (metadata == null) {
            validationText.text = "Blocking: run Analyze before Generate."
            log("Generate failed: metadata is missing. Run Analyze first.")
            return
        }
        if (input == null) {
            validationText.text = "Blocking: import a DBF file before Generate."
            log("Generate failed: DBF source context is unavailable.")
            return
        }

        val table = metadata.tables.single()
        val config = ReverseEngineerConfig(
            schemaVersion = metadata.schemaVersion,
            engineVersion = metadata.engineVersion,
            profile = profileCombo.selectedItem as ApiProfile,
            outputDir = outputDirField.text.trim().ifBlank { "generated" },
            generateMethodAliases = aliasToggle.isSelected,
            relations = table.candidateForeignKeys,
            tableConfigs = emptyList(),
        )

        val (artifacts, report) = ReverseEngineeringWorkflow.generate(metadata, config)
        val writtenFiles = artifactWriter(input, config, artifacts)
        previewText.text = artifacts.joinToString("\n\n") { it.source }
        apiText.text = buildApiText(config.profile, config.generateMethodAliases)
        generationText.text = buildGenerationText(config, artifacts, writtenFiles)
        validationText.text = if (report.warnings.isEmpty()) {
            "OK: generation completed without warnings."
        } else {
            "Non-blocking warnings:\n${report.warnings.joinToString("\n") { "- $it" }}"
        }
        log("Generate complete: ${report.generatedFileCount} artifacts into ${config.outputDir}.")
        if (writtenFiles.isNotEmpty()) {
            log("Generated files:\n${writtenFiles.joinToString("\n") { "- $it" }}")
        }
    }

    private fun buildInputText(input: ReverseEngineeringInput): String = buildString {
        appendLine("Loaded input table: ${input.tableName}")
        appendLine("Source path: ${input.sourcePath}")
        appendLine("Fields: ${input.table.fields.size}")
        appendLine("Records: ${input.table.records.size}")
    }

    private fun buildMetadataText(metadata: DbfTableMetadata): String = buildString {
        appendLine("Table: ${metadata.tableName}")
        appendLine("Source: ${metadata.sourcePath}")
        appendLine("Checksum: ${metadata.checksum}")
        appendLine("Primary Key: ${metadata.candidatePrimaryKey ?: "<none>"}")
        appendLine()
        appendLine("Fields:")
        metadata.fields.forEach {
            appendLine("- ${it.originalFieldName}: ${it.inferredType}(${it.width},${it.precision}) nullable=${it.nullableHint}")
        }
        if (metadata.warnings.isNotEmpty()) {
            appendLine()
            appendLine("Warnings:")
            metadata.warnings.forEach { appendLine("- $it") }
        }
    }

    private fun buildMappingText(metadata: DbfTableMetadata): String = buildString {
        appendLine("Class: ${metadata.tableName}")
        appendLine("Included fields: ${metadata.fields.size}")
        metadata.fields.forEach { appendLine("- ${it.originalFieldName} -> ${it.originalFieldName.lowercase()}") }
    }

    private fun buildRelationsText(metadata: DbfTableMetadata): String = buildString {
        if (metadata.candidateForeignKeys.isEmpty()) {
            append("No candidate relations inferred.")
            return@buildString
        }
        metadata.candidateForeignKeys.forEach {
            appendLine("${it.sourceTable}.${it.sourceFields.joinToString()} -> ${it.targetTable}.${it.targetFields.joinToString()} (${it.cardinality})")
        }
    }

    private fun validationIssues(metadata: DbfTableMetadata): String = buildString {
        val blocking = mutableListOf<String>()
        val nonBlocking = metadata.warnings.toMutableList()
        if (metadata.fields.isEmpty()) blocking += "Table must contain at least one field."
        if (metadata.candidatePrimaryKey == null) nonBlocking += "No candidate primary key detected."
        if (blocking.isEmpty()) {
            appendLine("Blocking: none")
        } else {
            appendLine("Blocking:")
            blocking.forEach { appendLine("- $it") }
        }
        if (nonBlocking.isEmpty()) {
            append("Non-blocking: none")
        } else {
            appendLine("Non-blocking:")
            nonBlocking.forEach { appendLine("- $it") }
        }
    }

    private fun inputsSummary(): String = "Select DBF input, then run Analyze."

    private fun apiSummary(): String = "Choose API profile and alias generation, then run Generate."

    private fun generationSummary(): String = "Set output directory and generate class artifacts."

    private fun buildApiText(profile: ApiProfile, aliasesEnabled: Boolean): String =
        "Profile: $profile\nMethod aliases: ${if (aliasesEnabled) "enabled" else "disabled"}"

    private fun buildGenerationText(
        config: ReverseEngineerConfig,
        artifacts: List<GeneratedClassArtifact>,
        writtenFiles: List<String>,
    ): String = buildString {
        appendLine("Output directory: ${config.outputDir}")
        appendLine("Artifacts generated: ${artifacts.size}")
        appendLine("Files written: ${writtenFiles.size}")
        if (writtenFiles.isNotEmpty()) {
            appendLine()
            appendLine("Written artifacts:")
            writtenFiles.forEach { appendLine("- $it") }
        }
    }

    private fun readOnlyArea(text: String): JTextArea = JTextArea(text).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    private fun log(message: String) {
        logsText.append("$message\n")
    }

    companion object {
        internal fun writeArtifacts(
            input: ReverseEngineeringInput,
            config: ReverseEngineerConfig,
            artifacts: List<GeneratedClassArtifact>,
        ): List<String> {
            if (artifacts.isEmpty()) return emptyList()
            val inputPath = Paths.get(input.sourcePath).toAbsolutePath().normalize()
            val baseDir = if (Files.isDirectory(inputPath)) inputPath else inputPath.parent ?: inputPath
            val outputDir = Paths.get(config.outputDir)
            val resolvedOutput = if (outputDir.isAbsolute) outputDir else baseDir.resolve(outputDir)
            val normalizedOutput = resolvedOutput.toAbsolutePath().normalize()
            Files.createDirectories(normalizedOutput)
            return artifacts.map { artifact ->
                val destination = normalizedOutput.resolve("${artifact.className}.prg")
                Files.createDirectories(destination.parent)
                Files.writeString(destination, artifact.source)
                destination.toString()
            }
        }
    }
}
