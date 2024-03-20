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
    private var isCodeownerFileEdited = false

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
            createAndShowCodeownersEditedPopupIfNeeded(actionEvent)
        }
    }

    private fun createAndShowCodeownersEditedPopupIfNeeded(actionEvent: AnActionEvent) {
        if (isCodeownerFileEdited) {
            val component = actionEvent.inputEvent.component as? JComponent ?: return
            val displayPoint = RelativePoint(component, Point(component.width / 2, 0))
            val balloon = createBalloonPopup(
                message = "Codeowners file is edited. Codeowner info may be incorrect.",
                messageType = MessageType.WARNING,
                duration = 8000,
            )
            balloon.show(displayPoint, Balloon.Position.above)
        }
    }

    private fun createAndShowEmptyCodeownersPopup(actionEvent: AnActionEvent) {
        val component = actionEvent.inputEvent.component as? JComponent ?: return
        val displayPoint = RelativePoint(component, Point(component.width / 2, component.height))
        val balloon = createBalloonPopup(
            message = "No code owners found",
            messageType = MessageType.INFO,
            duration = 5000,
        )
        balloon.show(displayPoint, Balloon.Position.below)
    }

    private fun createBalloonPopup(message: String, messageType: MessageType, duration: Long) =
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, messageType, null)
            .setFadeoutTime(duration)
            .createBalloon()

    private fun isGitEnabled(): Boolean {
        return ProjectLevelVcsManager.getInstance(project).checkVcsIsActive("Git")
    }

    private fun populateCodeOwnersMap(): HashMap<String, MutableList<VirtualFile>> {
        val codeOwnerMap: HashMap<String, MutableList<VirtualFile>> = hashMapOf()
        isCodeownerFileEdited = false

        val fileChanges = ChangeListManager.getInstance(project).defaultChangeList.changes
        fileChanges.forEach { change ->
            change.virtualFile?.let { file ->
                if (isCodeownerFileEdited.not() && filesHelper.isCodeOwnersFile(file)) {
                    isCodeownerFileEdited = true
                }
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
