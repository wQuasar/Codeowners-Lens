package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wquasar.codeowners.visibility.di.CodeOwnersComponentProvider
import javax.inject.Inject

internal class CodeOwnersCommitAction : AnAction() {

    init {
        CodeOwnersComponentProvider.component.inject(this)
    }

    @Inject
    lateinit var presenter: CodeOwnersCommitActionPresenter

    override fun update(actionEvent: AnActionEvent) {
        presenter.project = actionEvent.project ?: run {
            actionEvent.presentation.isVisible = false
            return
        }
        actionEvent.presentation.isVisible = presenter.isGitEnabled()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(actionEvent: AnActionEvent) {
        presenter.handleAction(actionEvent)
    }
}
