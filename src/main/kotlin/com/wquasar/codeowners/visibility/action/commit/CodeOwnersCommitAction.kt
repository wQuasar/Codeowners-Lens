package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.di.CodeOwnersComponentProvider
import com.wquasar.codeowners.visibility.file.FilesHelper
import com.wquasar.codeowners.visibility.ui.BalloonPopupHelper
import java.awt.event.MouseEvent
import javax.inject.Inject
import javax.swing.JComponent

internal class CodeOwnersCommitAction : AnAction() {

    init {
        CodeOwnersComponentProvider.component.inject(this)
    }

    internal sealed interface CodeOwnerState {
        data object NoFilesInDefaultChangelist : CodeOwnerState
        data object NoCodeownerFileFound : CodeOwnerState
        data class FilesWithCodeOwnersEdited(
            val codeOwnersMap: HashMap<String, MutableList<VirtualFile>>,
        ) : CodeOwnerState
    }

    @Inject
    lateinit var codeOwnerService: CodeOwnerService

    @Inject
    lateinit var filesHelper: FilesHelper

    @Inject
    lateinit var balloonPopupHelper: BalloonPopupHelper

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
        val codeOwnerState = getCodeOwnerActionState()
        when (codeOwnerState) {
            is CodeOwnerState.NoFilesInDefaultChangelist -> createAndShowEmptyChangelistPopup(actionEvent)
            is CodeOwnerState.NoCodeownerFileFound -> createAndShowEmptyCodeownersPopup(actionEvent)
            is CodeOwnerState.FilesWithCodeOwnersEdited -> {
                createAndShowCodeownersPopup(codeOwnerState.codeOwnersMap, actionEvent)
                createAndShowCodeownersEditedPopupIfNeeded(actionEvent)
            }
        }
    }

    private fun getCodeOwnerActionState(): CodeOwnerState {
        val fileChanges = ChangeListManager.getInstance(project).defaultChangeList.changes
        if (fileChanges.isEmpty()) {
            return CodeOwnerState.NoFilesInDefaultChangelist
        }

        val codeOwnerMap = populateCodeOwnersMap(fileChanges)
        if (codeOwnerMap.isEmpty()) {
            return CodeOwnerState.NoCodeownerFileFound
        }

        return CodeOwnerState.FilesWithCodeOwnersEdited(codeOwnerMap)
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

    private fun isGitEnabled(): Boolean {
        return ProjectLevelVcsManager.getInstance(project).checkVcsIsActive("Git")
    }

    private fun populateCodeOwnersMap(fileChanges: MutableCollection<Change>): HashMap<String, MutableList<VirtualFile>> {
        val codeOwnerMap: HashMap<String, MutableList<VirtualFile>> = hashMapOf()
        isCodeownerFileEdited = false

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
