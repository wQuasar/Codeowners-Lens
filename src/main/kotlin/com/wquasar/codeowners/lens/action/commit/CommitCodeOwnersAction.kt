package com.wquasar.codeowners.lens.action.commit

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.MessageType
import com.wquasar.codeowners.lens.core.CodeOwnerService
import com.wquasar.codeowners.lens.di.CodeOwnersComponentProvider
import com.wquasar.codeowners.lens.file.FilesHelper
import com.wquasar.codeowners.lens.glob.RuleGlobMatcher
import com.wquasar.codeowners.lens.ui.BalloonPopupHelper
import java.util.ResourceBundle
import java.util.Locale
import javax.inject.Inject

internal class CommitCodeOwnersAction : AnAction(), CommitCodeOwnersActionView {

    init {
        CodeOwnersComponentProvider.component.inject(this)
    }

    @Inject
    lateinit var presenter: CommitCodeOwnersActionPresenter

    @Inject
    lateinit var ruleGlobMatcher: RuleGlobMatcher

    @Inject
    lateinit var filesHelper: FilesHelper

    @Inject
    lateinit var balloonPopupHelper: BalloonPopupHelper
    
    private val messages = ResourceBundle.getBundle("messages", Locale.getDefault())

    override fun update(actionEvent: AnActionEvent) {
        actionEvent.project?.let { project ->
            presenter.view = this
            presenter.project = project
            presenter.codeOwnerService = project.getService(CodeOwnerService::class.java).apply {
                init(project, ruleGlobMatcher, filesHelper)
            }
        } ?: run {
            actionEvent.presentation.isVisible = false
            return
        }
        actionEvent.presentation.isVisible = presenter.isGitEnabled()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(actionEvent: AnActionEvent) {
        presenter.handleActionEvent(actionEvent)
    }

    override fun showCodeOwnersEditedPopup(actionEvent: AnActionEvent) {
        balloonPopupHelper.createAndShowBalloonPopupAboveComponent(
            actionEvent = actionEvent,
            message = messages.getString("commit.codeowners_file_edited_msg"),
            messageType = MessageType.WARNING,
            duration = 8000,
        )
    }

    override fun showEmptyChangelistPopup(actionEvent: AnActionEvent) {
        balloonPopupHelper.createAndShowBalloonPopupAboveComponent(
            actionEvent = actionEvent,
            message = messages.getString("commit.no_modified_files_msg"),
        )
    }

    override fun showNoCodeOwnersFilePopup(actionEvent: AnActionEvent) {
        balloonPopupHelper.createAndShowBalloonPopupAboveComponent(
            actionEvent = actionEvent,
            message = messages.getString("commit.no_codeowner_file_msg"),
        )
    }

    override fun showActionPopup(popupInfo: ActionPopupInfo) {
        val popup = ActionManager.getInstance().createActionPopupMenu("CodeOwners.Commit", popupInfo.actionGroup)
        popup.component.show(popupInfo.component, popupInfo.point.first, popupInfo.point.second)
    }
}
