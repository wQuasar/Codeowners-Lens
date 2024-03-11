package com.wquasar.codeowners.visibility

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.wquasar.codeowners.visibility.di.CodeOwnersComponentProvider
import com.wquasar.codeowners.visibility.di.ProjectProvider
import dagger.Lazy
import javax.inject.Inject


internal class CodeOwnersWidgetFactory : StatusBarWidgetFactory {

    init {
        CodeOwnersComponentProvider.component.inject(this)
    }

    @Inject
    lateinit var codeOwnerWidgetProvider: Lazy<CodeOwnersWidget>

    override fun getId() = CodeOwnersWidget.ID

    override fun getDisplayName() = "Code Owners"

    override fun createWidget(project: Project): CodeOwnersWidget {
        ProjectProvider.project = project
        return codeOwnerWidgetProvider.get()
    }
}
