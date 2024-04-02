package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.di.CodeOwnersComponentProvider
import com.wquasar.codeowners.visibility.file.FilesHelper
import com.wquasar.codeowners.visibility.glob.RuleGlobMatcher
import javax.inject.Inject

internal class CodeOwnersCommitAction : AnAction() {

    init {
        CodeOwnersComponentProvider.component.inject(this)
    }

    @Inject
    lateinit var presenter: CodeOwnersCommitActionPresenter

    @Inject
    lateinit var ruleGlobMatcher: RuleGlobMatcher

    @Inject
    lateinit var filesHelper: FilesHelper

    override fun update(actionEvent: AnActionEvent) {
        actionEvent.project?.let {
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
        presenter.handleAction(actionEvent)
    }
}
