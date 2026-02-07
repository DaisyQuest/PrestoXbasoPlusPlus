package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbPreprocessorDirectiveClassifierTest {
    @Test
    fun `detects macro definition directives`() {
        assertThat(isMacroDefinitionDirective("#define FOO 1")).isTrue
        assertThat(isMacroDefinitionDirective("# define BAR")).isTrue
        assertThat(isMacroDefinitionDirective("   #DEFINE BAZ 2")).isTrue
        assertThat(isMacroDefinitionDirective("#define")).isTrue
    }

    @Test
    fun `rejects non macro definition directives`() {
        assertThat(isMacroDefinitionDirective("#include \"file.ch\"")).isFalse
        assertThat(isMacroDefinitionDirective("#undef FOO")).isFalse
        assertThat(isMacroDefinitionDirective("#defineX FOO")).isFalse
        assertThat(isMacroDefinitionDirective("define FOO")).isFalse
    }
}
