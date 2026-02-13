package com.prestoxbasopp.ide.dbf

import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class DbfFileEditorProviderTest {
    private val project: Project =
        Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(Project::class.java),
        ) { _, _, _ -> null } as Project

    @Test
    fun `accept recognizes dbf extension case insensitively`() {
        val provider = DbfFileEditorProvider()

        assertThat(provider.accept(project, LightVirtualFile("customers.dbf"))).isTrue()
        assertThat(provider.accept(project, LightVirtualFile("customers.DBF"))).isTrue()
        assertThat(provider.accept(project, LightVirtualFile("customers.txt"))).isFalse()
    }

    @Test
    fun `provider metadata hides default editor for dbf files`() {
        val provider = DbfFileEditorProvider()

        assertThat(provider.editorTypeId).isEqualTo("dbf-ultra-master-editor")
        assertThat(provider.policy).isEqualTo(FileEditorPolicy.HIDE_DEFAULT_EDITOR)
    }
}
