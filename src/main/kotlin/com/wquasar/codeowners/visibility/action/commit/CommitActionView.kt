package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.actionSystem.AnActionEvent

internal interface CommitActionView {

    fun showCodeOwnersEditedPopup(actionEvent: AnActionEvent)

    fun showEmptyChangelistPopup(actionEvent: AnActionEvent)

    fun showNoCodeOwnersFilePopup(actionEvent: AnActionEvent)
}
