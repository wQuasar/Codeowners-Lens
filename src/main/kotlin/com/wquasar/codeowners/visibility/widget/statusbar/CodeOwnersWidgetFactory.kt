package com.wquasar.codeowners.visibility.widget.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.di.CodeOwnersComponentProvider
import com.wquasar.codeowners.visibility.file.FilesHelper
import javax.inject.Inject


internal class CodeOwnersWidgetFactory : StatusBarWidgetFactory {

    init {
        CodeOwnersComponentProvider.component.inject(this)
    }

    @Inject
    lateinit var codeOwnerService: CodeOwnerService

    @Inject
    lateinit var filesHelper: FilesHelper

    override fun getId() = CodeOwnersWidget.ID

    override fun getDisplayName() = "Code Owners"

    override fun createWidget(project: Project) = CodeOwnersWidget(project, codeOwnerService, filesHelper)
}
