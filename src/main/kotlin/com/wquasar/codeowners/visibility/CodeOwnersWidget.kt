package com.wquasar.codeowners.visibility

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget

class CodeOwnersWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.MultipleTextValuesPresentation {

    companion object {
        const val ID = "com.wquasar.codeowners.visibility.CodeOwnersWidget"
    }

    override fun ID() = ID

    override fun getSelectedValue(): String {
        return "Hello, world!"
    }

    override fun getTooltipText(): String {
        return "Hello, tip from the world!"
    }

    override fun getPresentation() = this

}
