package com.wquasar.codeowners.visibility.commit

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.di.CodeOwnersComponentProvider
import com.wquasar.codeowners.visibility.file.FilesHelper
import java.awt.event.MouseEvent
import javax.inject.Inject

internal class CodeOwnersCommitAction : AnAction() {

    init {
        CodeOwnersComponentProvider.component.inject(this)
    }

    @Inject
    lateinit var codeOwnerService: CodeOwnerService

    @Inject
    lateinit var filesHelper: FilesHelper

    private lateinit var project: Project

    override fun update(actionEvent: AnActionEvent) {
        project = actionEvent.project ?: return
        actionEvent.presentation.isVisible = isGitEnabled()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(actionEvent: AnActionEvent) {
        val codeOwnersMap: HashMap<String, MutableList<VirtualFile>> = populateCodeOwnersMap()
        createAndShowPopup(codeOwnersMap, actionEvent)
    }

    private fun isGitEnabled(): Boolean {
        return ProjectLevelVcsManager.getInstance(project).checkVcsIsActive("Git")
    }

    private fun populateCodeOwnersMap(): HashMap<String, MutableList<VirtualFile>> {
        val codeOwnerMap: HashMap<String, MutableList<VirtualFile>> = hashMapOf()

        val fileChanges = ChangeListManager.getInstance(project).defaultChangeList.changes
        fileChanges.forEach { change ->
            change.virtualFile?.let { file ->
                val codeOwnerRule = codeOwnerService.getCodeOwners(ModuleManager.getInstance(project), file)

                codeOwnerRule?.owners?.forEach { owner ->
                    codeOwnerMap[owner] = codeOwnerMap.getOrPut(owner) { mutableListOf() }.apply {
                        add(file)
                    }
                }
            }
        }
        return codeOwnerMap
    }

    private fun createAndShowPopup(
        codeOwnerMap: HashMap<String, MutableList<VirtualFile>>,
        actionEvent: AnActionEvent,
    ) {
        val mouseEvent = actionEvent.inputEvent as? MouseEvent
        val point = mouseEvent?.point ?: return
        val component = actionEvent.getData(PlatformDataKeys.CONTEXT_COMPONENT) ?: return

        val actionGroup = DefaultActionGroup().apply {
            codeOwnerMap.keys.forEach { owner ->
                val modifiedOwnedFiles = codeOwnerMap[owner] ?: return@forEach

                // if one file, open it else, show files list
                when (modifiedOwnedFiles.size) {
                    1 -> addPopupItemAction(owner, modifiedOwnedFiles.first())
                    else -> {
                        val fileActionGroup = DefaultActionGroup(owner, true).apply {
                            modifiedOwnedFiles.forEach { file ->
                                addPopupItemAction(filesHelper.getTruncatedFileName(file), file)
                            }
                        }
                        add(fileActionGroup)
                    }
                }
            }
        }

        val popup = ActionManager.getInstance().createActionPopupMenu("CodeOwners.Commit", actionGroup)
        popup.component.show(component, point.x, point.y)
    }

    private fun DefaultActionGroup.addPopupItemAction(
        fileName: String,
        file: VirtualFile
    ) {
        add(object : AnAction(fileName) {
            override fun actionPerformed(e: AnActionEvent) {
                filesHelper.openFile(project, file)
            }
        })
    }
}
