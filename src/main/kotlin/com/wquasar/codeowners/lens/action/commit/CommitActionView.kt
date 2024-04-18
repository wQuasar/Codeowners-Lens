package com.wquasar.codeowners.lens.action.commit

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import javax.swing.JComponent

internal interface CommitActionView {

    fun showCodeOwnersEditedPopup(actionEvent: AnActionEvent)

    fun showEmptyChangelistPopup(actionEvent: AnActionEvent)

    fun showNoCodeOwnersFilePopup(actionEvent: AnActionEvent)

    fun showActionPopup(popupInfo: ActionPopupInfo)
}

internal data class ActionPopupInfo(
    val actionGroup: DefaultActionGroup,
    val component: JComponent,
    val point: Pair<Int, Int>,
)
