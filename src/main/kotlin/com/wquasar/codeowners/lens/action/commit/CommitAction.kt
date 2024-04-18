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
import javax.inject.Inject

internal class CommitAction : AnAction(), CommitActionView {

    init {
        CodeOwnersComponentProvider.component.inject(this)
    }

    @Inject
    lateinit var presenter: CommitActionPresenter

    @Inject
    lateinit var ruleGlobMatcher: RuleGlobMatcher

    @Inject
    lateinit var filesHelper: FilesHelper

    @Inject
    lateinit var balloonPopupHelper: BalloonPopupHelper

    override fun update(actionEvent: AnActionEvent) {
        actionEvent.project?.let {
            presenter.view = this
            presenter.project = it
            presenter.codeOwnerService = it.getService(CodeOwnerService::class.java).apply {
                init(ruleGlobMatcher, filesHelper)
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
            message = "Codeowners file is edited. Codeowner info may be incorrect.",
            messageType = MessageType.WARNING,
            duration = 8000,
        )
    }

    override fun showEmptyChangelistPopup(actionEvent: AnActionEvent) {
        balloonPopupHelper.createAndShowBalloonPopupAboveComponent(
            actionEvent = actionEvent,
            message = "No files modified.",
        )
    }

    override fun showNoCodeOwnersFilePopup(actionEvent: AnActionEvent) {
        balloonPopupHelper.createAndShowBalloonPopupAboveComponent(
            actionEvent = actionEvent,
            message = "No CODEOWNERS file found for modified files.",
        )
    }

    override fun showActionPopup(popupInfo: ActionPopupInfo) {
        val popup = ActionManager.getInstance().createActionPopupMenu("CodeOwners.Commit", popupInfo.actionGroup)
        popup.component.show(popupInfo.component, popupInfo.point.first, popupInfo.point.second)
    }
}
