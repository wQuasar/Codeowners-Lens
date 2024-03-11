package com.wquasar.codeowners.visibility

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.wquasar.codeowners.visibility.di.CodeOwnersComponentProvider
import com.wquasar.codeowners.visibility.di.ProjectProvider


internal class CodeOwnersWidgetFactory : StatusBarWidgetFactory {

    init {
        CodeOwnersComponentProvider.component.inject(this)
    }

    override fun getId() = CodeOwnersWidget.ID

    override fun getDisplayName() = "Code Owners"

    override fun createWidget(project: Project): CodeOwnersWidget {
        ProjectProvider.project = project
        return CodeOwnersWidget(project)
    }
}
