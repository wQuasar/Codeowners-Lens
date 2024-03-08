package com.wquasar.codeowners.visibility

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidgetFactory


internal class CodeOwnersWidgetFactory : StatusBarWidgetFactory {
    override fun getId() = CodeOwnersWidget.ID

    override fun getDisplayName() = "Code Owners"

    override fun createWidget(project: Project) = CodeOwnersWidget(project)
}
