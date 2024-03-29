package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.visibility.action.commit.CodeOwnersCommitActionState.*
import com.wquasar.codeowners.visibility.core.CodeOwnerRule
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.file.FilesHelper
import com.wquasar.codeowners.visibility.ui.BalloonPopupHelper
import java.awt.event.MouseEvent
import javax.inject.Inject
import javax.swing.JComponent

internal class CodeOwnersCommitActionPresenter @Inject constructor(
    private val codeOwnerService: CodeOwnerService,
    private val filesHelper: FilesHelper,
    private val balloonPopupHelper: BalloonPopupHelper,
) {

    lateinit var project: Project
    private var isCodeownerFileEdited = false

    fun isGitEnabled(): Boolean {
        return ProjectLevelVcsManager.getInstance(project).checkVcsIsActive("Git")
    }

    private fun getGitFileChanges(): MutableCollection<Change> =
        ChangeListManager.getInstance(project).defaultChangeList.changes

    private fun getCodeOwnerForFile(file: VirtualFile): CodeOwnerRule? {
        return codeOwnerService.getCodeOwners(ModuleManager.getInstance(project), file)
    }

    fun handleAction(actionEvent: AnActionEvent) {
        when (val codeOwnerState = getCodeOwnerActionState()) {
            is NoFilesInDefaultChangelist -> createAndShowEmptyChangelistPopup(actionEvent)
            is NoCodeownerFileFound -> createAndShowEmptyCodeownersPopup(actionEvent)
            is FilesWithCodeOwnersEdited -> {
                createAndShowCodeownersPopup(codeOwnerState.codeOwnersMap, actionEvent)
                createAndShowCodeownersEditedPopupIfNeeded(actionEvent)
            }
        }
    }

    private fun getCodeOwnerActionState(): CodeOwnersCommitActionState {
        val fileChanges = getGitFileChanges()
        if (fileChanges.isEmpty()) {
            return NoFilesInDefaultChangelist
        }

        val codeOwnerMap = populateCodeOwnersMap(fileChanges)
        if (codeOwnerMap.isEmpty()) {
            return NoCodeownerFileFound
        }

        return FilesWithCodeOwnersEdited(codeOwnerMap)
    }

    private fun populateCodeOwnersMap(fileChanges: MutableCollection<Change>): HashMap<String, MutableList<VirtualFile>> {
        val codeOwnerMap: HashMap<String, MutableList<VirtualFile>> = hashMapOf()
        isCodeownerFileEdited = false

        fileChanges.forEach { change ->
            change.virtualFile?.let { file ->
                if (isCodeownerFileEdited.not() && filesHelper.isCodeOwnersFile(file)) {
                    isCodeownerFileEdited = true
                }
                val codeOwnerRule = getCodeOwnerForFile(file)

                codeOwnerRule?.owners?.forEach { owner ->
                    codeOwnerMap[owner] = codeOwnerMap.getOrPut(owner) { mutableListOf() }.apply {
                        add(file)
                    }
                }
            }
        }
        return codeOwnerMap
    }

    private fun createAndShowCodeownersEditedPopupIfNeeded(actionEvent: AnActionEvent) {
        if (isCodeownerFileEdited) {
            balloonPopupHelper.createAndShowBalloonPopupAboveComponent(
                actionEvent = actionEvent,
                message = "Codeowners file is edited. Codeowner info may be incorrect.",
                messageType = MessageType.WARNING,
                duration = 8000,
            )
        }
    }

    private fun createAndShowEmptyChangelistPopup(actionEvent: AnActionEvent) {
        balloonPopupHelper.createAndShowBalloonPopupAboveComponent(
            actionEvent = actionEvent,
            message = "No files found in default changelist.",
        )
    }

    private fun createAndShowEmptyCodeownersPopup(actionEvent: AnActionEvent) {
        balloonPopupHelper.createAndShowBalloonPopupAboveComponent(
            actionEvent = actionEvent,
            message = "No codeowners file found for files in default changelist.",
        )
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
