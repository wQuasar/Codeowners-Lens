package com.wquasar.codeowners.visibility.widget.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.wquasar.codeowners.visibility.core.CodeOwnerRule
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.core.FileCodeOwnerState.NoCodeOwnerFileFound
import com.wquasar.codeowners.visibility.core.FileCodeOwnerState.NoRuleFoundInCodeOwnerFile
import com.wquasar.codeowners.visibility.core.FileCodeOwnerState.RuleFoundInCodeOwnerFile
import com.wquasar.codeowners.visibility.file.FilesHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class StatusBarWidgetPresenterTest {

    private lateinit var presenter: StatusBarWidgetPresenter
    private val project: Project = mock()
    private val codeOwnerService: CodeOwnerService = mock()
    private val filesHelper: FilesHelper = mock()
    private val view: StatusBarWidgetView = mock()
    private val statusBar: StatusBar = mock()
    private val virtualFile: VirtualFile = mock()

    @Before
    fun setup() {
        presenter = StatusBarWidgetPresenter(project, codeOwnerService, filesHelper)
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
    fun `get Selected Value returns empty string when no file is selected`() {
        val result = presenter.getSelectedValue()
        assertEquals("", result)
    }

    @Test
    fun `get Selected Value returns NO CODEOWNER when no rule is found in code owner file`() {
        presenter.updateState(virtualFile)
        whenever(codeOwnerService.getFileCodeOwnerState(project, virtualFile)).thenReturn(NoRuleFoundInCodeOwnerFile)
        val result = presenter.getSelectedValue()
        assertEquals(StatusBarWidgetPresenter.NO_CODEOWNER, result)
    }

    @Test
    fun `get Selected Value returns owner name when rule is found in code owner file`() {
        presenter.updateState(virtualFile)
        val codeOwnerRule = CodeOwnerRule(pattern = "path", owners = listOf("owner"), lineNumber = 1)
        whenever(codeOwnerService.getFileCodeOwnerState(project, virtualFile)).thenReturn(
            RuleFoundInCodeOwnerFile(
                codeOwnerRule
            )
        )
        val result = presenter.getSelectedValue()
        assertEquals("owner", result)
    }

    @Test
    fun `get Selected Value returns formatted owner names when multiple owners are found`() {
        presenter.updateState(virtualFile)
        val codeOwnerRule =
            CodeOwnerRule(pattern = "path", owners = listOf("owner1", "owner2", "owner3"), lineNumber = 1)
        whenever(codeOwnerService.getFileCodeOwnerState(project, virtualFile)).thenReturn(
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
        whenever(codeOwnerService.getFileCodeOwnerState(project, virtualFile)).thenReturn(NoCodeOwnerFileFound)
        val result = presenter.getTooltipText()
        assertEquals("", result)
    }

    @Test
    fun `getTooltipText returns appropriate message when no rule is found in code owner file`() {
        whenever(codeOwnerService.getFileCodeOwnerState(project, virtualFile)).thenReturn(NoRuleFoundInCodeOwnerFile)
        presenter.updateState(virtualFile)
        presenter.getSelectedValue()
        val result = presenter.getTooltipText()
        assertEquals("No codeowners found", result)
    }

    @Test
    fun `getTooltipText returns appropriate message when rule is found in code owner file`() {
        val codeOwnerRule = CodeOwnerRule(pattern = "path", owners = listOf("owner"), lineNumber = 1)
        whenever(codeOwnerService.getFileCodeOwnerState(project, virtualFile)).thenReturn(
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
