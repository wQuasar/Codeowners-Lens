package com.wquasar.codeowners.lens.widget.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.wquasar.codeowners.lens.core.CodeOwnerService
import com.wquasar.codeowners.lens.di.CodeOwnersComponentProvider
import com.wquasar.codeowners.lens.file.FilesHelper
import com.wquasar.codeowners.lens.glob.RuleGlobMatcher
import javax.inject.Inject


internal class StatusBarWidgetFactory : StatusBarWidgetFactory {

    init {
        CodeOwnersComponentProvider.component.inject(this)
    }

    @Inject
    lateinit var filesHelper: FilesHelper

    @Inject
    lateinit var ruleGlobMatcher: RuleGlobMatcher

    private fun getCodeOwnerService(project: Project) = project.getService(CodeOwnerService::class.java).apply {
        init(ruleGlobMatcher, filesHelper)
    }

    private fun createPresenter(project: Project) =
        StatusBarWidgetPresenter(project, getCodeOwnerService(project), filesHelper)

    override fun getId() = StatusBarWidgetPresenter.ID

    override fun getDisplayName() = "Code Owners"

    override fun createWidget(project: Project) =
        StatusBarWidget(project, createPresenter(project))
}
