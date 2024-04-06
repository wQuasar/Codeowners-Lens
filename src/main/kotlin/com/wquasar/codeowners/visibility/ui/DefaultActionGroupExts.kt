package com.wquasar.codeowners.visibility.ui

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup

internal fun DefaultActionGroup.addPopupActionItem(
    label: String,
    operation: (AnActionEvent) -> Unit,
) {
    add(object : AnAction(label) {
        override fun actionPerformed(actionEvent: AnActionEvent) {
            operation(actionEvent)
        }
    })
}

internal fun DefaultActionGroup.addPopupSection(
    title: String,
    actionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT,
) {
    add(object : AnAction(title) {
        override fun actionPerformed(e: AnActionEvent) {
            // No action performed
        }

        override fun update(actionEvent: AnActionEvent) {
            actionEvent.presentation.isEnabled = false // makes it non-clickable
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return actionUpdateThread
        }
    })
}
