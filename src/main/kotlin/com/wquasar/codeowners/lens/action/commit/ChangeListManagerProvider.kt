package com.wquasar.codeowners.lens.action.commit

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import javax.inject.Inject

internal class ChangeListManagerProvider @Inject constructor() {

    fun getChangeListManager(project: Project): ChangeListManager = ChangeListManager.getInstance(project)
}
