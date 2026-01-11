package com.wquasar.codeowners.lens.widget.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.wquasar.codeowners.lens.core.CodeOwnerRule
import com.wquasar.codeowners.lens.core.CodeOwnerService
import com.wquasar.codeowners.lens.core.FileCodeOwnerState.NoCodeOwnerFileFound
import com.wquasar.codeowners.lens.core.FileCodeOwnerState.NoRuleFoundInCodeOwnerFile
import com.wquasar.codeowners.lens.core.FileCodeOwnerState.RuleFoundInCodeOwnerFile
import com.wquasar.codeowners.lens.file.FilesHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class CodeOwnerNameWidgetPresenterTest {

    private lateinit var presenter: CodeOwnerNameWidgetPresenter
    private val project: Project = mock()
    private val codeOwnerService: CodeOwnerService = mock()
    private val filesHelper: FilesHelper = mock()
    private val view: CodeOwnerNameWidgetView = mock()
    private val statusBar: StatusBar = mock()
    private val virtualFile: VirtualFile = mock {
        on { isInLocalFileSystem }.thenReturn(true)
    }

    @Before
    fun setup() {
        presenter = CodeOwnerNameWidgetPresenter(project, codeOwnerService, filesHelper)
        presenter.view = view
    }

    @Test
    fun `should Install returns true when statusBar project matches presenter project`() {
        whenever(statusBar.project).thenReturn(project)
        val result = presenter.shouldInstall(statusBar)
        assertEquals(true, result)
    }

    @Test
    fun `should Install returns false when statusBar project does not match presenter project`() {
        whenever(statusBar.project).thenReturn(mock())
        val result = presenter.shouldInstall(statusBar)
        assertEquals(false, result)
    }

    @Test
    fun `getSelectedValue returns empty string when no file is selected`() {
        val result = presenter.getSelectedValue()
        assertEquals("", result)
    }

    @Test
    fun `getSelectedValue returns NO CODEOWNER when no rule is found in code owner file`() {
        presenter.updateState(virtualFile)
        whenever(codeOwnerService.getFileCodeOwnerState(virtualFile, null)).thenReturn(NoRuleFoundInCodeOwnerFile)
        val result = presenter.getSelectedValue()
        assertEquals("¯\\_(ツ)_/¯", result)
    }

    @Test
    fun `getSelectedValue returns owner name when rule is found in code owner file`() {
        presenter.updateState(virtualFile)
        val codeOwnerRule = CodeOwnerRule(pattern = "path", owners = listOf("owner"), lineNumber = 1)
        whenever(codeOwnerService.getFileCodeOwnerState(virtualFile, null)).thenReturn(
            RuleFoundInCodeOwnerFile(
                codeOwnerRule
            )
        )
        val result = presenter.getSelectedValue()
        assertEquals("owner", result)
    }

    @Test
    fun `getSelectedValue returns formatted owner names when multiple owners are found`() {
        presenter.updateState(virtualFile)
        val codeOwnerRule =
            CodeOwnerRule(pattern = "path", owners = listOf("owner1", "owner2", "owner3"), lineNumber = 1)
        whenever(codeOwnerService.getFileCodeOwnerState(virtualFile, null)).thenReturn(
            RuleFoundInCodeOwnerFile(
                codeOwnerRule
            )
        )
        val result = presenter.getSelectedValue()
        assertEquals("owner1, owner2 & 1 more", result)
    }

    @Test
    fun `getTooltipText returns empty string when no code owner file is found`() {
        presenter.updateState(virtualFile)
        whenever(codeOwnerService.getFileCodeOwnerState(virtualFile, null)).thenReturn(NoCodeOwnerFileFound)
        val result = presenter.getTooltipText()
        assertEquals("", result)
    }

    @Test
    fun `getTooltipText returns empty string when the VirtualFile is not a physical file`() {
        presenter.updateState(virtualFile)
        whenever(virtualFile.isInLocalFileSystem).thenReturn(false)
        val result = presenter.getTooltipText()
        assertEquals("", result)

        verify(codeOwnerService, never()).getFileCodeOwnerState(virtualFile, null)
    }

    @Test
    fun `getTooltipText returns appropriate message when no rule is found in code owner file`() {
        whenever(codeOwnerService.getFileCodeOwnerState(virtualFile, null)).thenReturn(NoRuleFoundInCodeOwnerFile)
        presenter.updateState(virtualFile)
        presenter.getSelectedValue()
        val result = presenter.getTooltipText()
        assertEquals("No codeowners found", result)
    }

    @Test
    fun `getTooltipText returns appropriate message when rule is found in code owner file`() {
        val codeOwnerRule = CodeOwnerRule(pattern = "path", owners = listOf("owner"), lineNumber = 1)
        whenever(codeOwnerService.getFileCodeOwnerState(virtualFile, null)).thenReturn(
            RuleFoundInCodeOwnerFile(
                codeOwnerRule
            )
        )
        presenter.updateState(virtualFile)
        presenter.getSelectedValue()
        val result = presenter.getTooltipText()
        assertEquals("Click to show in CODEOWNERS", result)
    }
}
