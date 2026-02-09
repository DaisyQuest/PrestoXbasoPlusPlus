package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiFile
import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbCompletionServiceTest {
    @Test
    fun `suggests completions with prefix filtering`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 30),
            text = "file",
            children = listOf(
                XbPsiFunctionDeclaration(
                    symbolName = "Alpha",
                    parameters = emptyList(),
                    textRange = XbTextRange(0, 10),
                    text = "function Alpha()",
                ),
                XbPsiVariableDeclaration(
                    symbolName = "Beta",
                    isMutable = false,
                    textRange = XbTextRange(11, 20),
                    text = "var Beta",
                ),
                XbPsiVariableDeclaration(
                    symbolName = "Alpha",
                    isMutable = true,
                    textRange = XbTextRange(21, 30),
                    text = "var Alpha",
                ),
            ),
        )

        val items = XbCompletionService().suggest(root, "Al", caseSensitive = true)
        assertThat(items).containsExactly(
            XbCompletionItem("Alpha", XbCompletionType.FUNCTION),
            XbCompletionItem("Alpha", XbCompletionType.VARIABLE),
        )
    }

    @Test
    fun `handles case insensitive completion`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 10),
            text = "file",
            children = listOf(
                XbPsiFunctionDeclaration(
                    symbolName = "Gamma",
                    parameters = emptyList(),
                    textRange = XbTextRange(0, 5),
                    text = "function Gamma()",
                ),
            ),
        )

        val items = XbCompletionService().suggest(root, "ga", caseSensitive = false)
        assertThat(items).containsExactly(XbCompletionItem("Gamma", XbCompletionType.FUNCTION))
    }

    @Test
    fun `includes keyword suggestions`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 5),
            text = "file",
            children = emptyList(),
        )

        val items = XbCompletionService().suggest(root, "lo", caseSensitive = false)

        assertThat(items).containsExactly(XbCompletionItem("LOCAL", XbCompletionType.KEYWORD))
    }

    @Test
    fun `suggests command attributes from metadata`() {
        val metadata = XbCompletionMetadata(
            commands = listOf(
                XbCommandMetadata(
                    name = "DCSAY",
                    attributes = listOf(
                        XbCommandAttributeMetadata(name = "SAYSIZE", type = "NUMERIC"),
                        XbCommandAttributeMetadata(name = "SAYCOLOR", type = "STRING"),
                    ),
                ),
            ),
        )
        val service = XbCompletionService(
            keywords = emptySet(),
            metadataService = XbCompletionMetadataService(provider = XbCompletionMetadataProvider { metadata }),
        )
        val contextText = "DCSAY SA"
        val context = XbCompletionContext(
            text = contextText,
            offset = contextText.length,
            projectBasePath = null,
        )

        val items = service.suggest(
            root = XbPsiFile("sample", XbTextRange(0, 0), "", emptyList()),
            prefix = "SA",
            caseSensitive = false,
            context = context,
        )

        assertThat(items).containsExactly(
            XbCompletionItem("SAYCOLOR", XbCompletionType.COMMAND_ATTRIBUTE, "STRING"),
            XbCompletionItem("SAYSIZE", XbCompletionType.COMMAND_ATTRIBUTE, "NUMERIC"),
        )
    }

    @Test
    fun `suggests table columns from metadata`() {
        val metadata = XbCompletionMetadata(
            tables = listOf(
                XbTableMetadata(
                    name = "charges",
                    columns = listOf(
                        XbTableColumnMetadata(name = "index_no", type = "NUMERIC", length = 10),
                        XbTableColumnMetadata(name = "account", type = "CHAR", length = 12),
                    ),
                ),
            ),
        )
        val service = XbCompletionService(
            keywords = emptySet(),
            metadataService = XbCompletionMetadataService(provider = XbCompletionMetadataProvider { metadata }),
        )
        val text = "SELE charges\ncharges->in"
        val context = XbCompletionContext(
            text = text,
            offset = text.length,
            projectBasePath = null,
        )

        val items = service.suggest(
            root = XbPsiFile("sample", XbTextRange(0, 0), "", emptyList()),
            prefix = "in",
            caseSensitive = false,
            context = context,
        )

        assertThat(items).containsExactly(
            XbCompletionItem("index_no", XbCompletionType.TABLE_COLUMN, "NUMERIC(10)"),
        )
    }

    @Test
    fun `suggests table columns from focused table when no alias is used`() {
        val metadata = XbCompletionMetadata(
            tables = listOf(
                XbTableMetadata(
                    name = "charges",
                    columns = listOf(
                        XbTableColumnMetadata(name = "account", type = "CHAR", length = 12),
                    ),
                ),
            ),
        )
        val service = XbCompletionService(
            keywords = emptySet(),
            metadataService = XbCompletionMetadataService(provider = XbCompletionMetadataProvider { metadata }),
        )
        val text = "SELE charges\n->ac"
        val context = XbCompletionContext(
            text = text,
            offset = text.length,
            projectBasePath = null,
        )

        val items = service.suggest(
            root = XbPsiFile("sample", XbTextRange(0, 0), "", emptyList()),
            prefix = "ac",
            caseSensitive = false,
            context = context,
        )

        assertThat(items).containsExactly(
            XbCompletionItem("account", XbCompletionType.TABLE_COLUMN, "CHAR(12)"),
        )
    }
}
