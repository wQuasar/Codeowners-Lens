package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.file.FilesHelper
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class CommitActionPresenterTest {

    private val filesHelper: FilesHelper = mock()
    private val codeOwnerService: CodeOwnerService = mock()
    private val view: CommitActionView = mock()
    private val project: Project = mock()
    private val actionEvent: AnActionEvent = mock()
    private val changeListManager: ChangeListManager = mock()
    private val changeListManagerProvider: ChangeListManagerProvider = mock()

    private var presenter: CommitActionPresenter = CommitActionPresenter(
        filesHelper,
        changeListManagerProvider,
    )

    @Before
    fun setUp() {
        presenter.view = view
        presenter.project = project
        presenter.codeOwnerService = codeOwnerService
        whenever(changeListManagerProvider.getChangeListManager(project)).thenReturn(changeListManager)
    }

    @Test
    fun `should show empty changelist popup when no changes in any changelist`() {
        // Arrange
        whenever(changeListManager.allChanges).thenReturn(emptyList())
        // Act
        presenter.handleActionEvent(actionEvent)
        // Assert
        verify(view).showEmptyChangelistPopup(actionEvent)
    }
}
