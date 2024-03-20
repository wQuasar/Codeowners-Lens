package com.wquasar.codeowners.visibility.commit

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.di.CodeOwnersComponentProvider
import com.wquasar.codeowners.visibility.file.FilesHelper
import java.awt.Point
import java.awt.event.MouseEvent
import javax.inject.Inject
import javax.swing.JComponent

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
        val codeOwnersMap = populateCodeOwnersMap()
        if (codeOwnersMap.isEmpty()) {
            createAndShowEmptyCodeownersPopup(actionEvent)
        } else {
            createAndShowCodeownersPopup(codeOwnersMap, actionEvent)
        }
    }

    private fun createAndShowEmptyCodeownersPopup(actionEvent: AnActionEvent) {
        val displayPoint = getPopupDisplayPoint(actionEvent) ?: return
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("No code owners found", MessageType.INFO, null)
            .setFadeoutTime(5000)
            .createBalloon()
        balloon.show(displayPoint, Balloon.Position.below)
    }

    private fun getPopupDisplayPoint(actionEvent: AnActionEvent): RelativePoint? {
        val component = actionEvent.inputEvent.component as? JComponent ?: return null
        return RelativePoint(component, Point(component.width / 2, component.height))
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

    private fun createAndShowCodeownersPopup(
        codeOwnerMap: HashMap<String, MutableList<VirtualFile>>,
        actionEvent: AnActionEvent,
    ) {
        val mouseEvent = actionEvent.inputEvent as? MouseEvent
        val point = mouseEvent?.point ?: return
        val component = actionEvent.inputEvent.component as? JComponent ?: return

        val actionGroup = DefaultActionGroup().apply {
            codeOwnerMap.keys.forEach { owner ->
                val modifiedOwnedFiles = codeOwnerMap[owner] ?: return@forEach

                val fileActionGroup = DefaultActionGroup("$owner [${modifiedOwnedFiles.size}]", true).apply {
                    modifiedOwnedFiles.forEach { file ->
                        addPopupItemAction(filesHelper.getTruncatedFileName(file), file)
                    }
                }
                add(fileActionGroup)
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
