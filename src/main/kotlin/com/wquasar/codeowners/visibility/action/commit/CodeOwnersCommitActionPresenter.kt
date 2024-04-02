package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.visibility.action.commit.CodeOwnersCommitActionState.*
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.core.FileCodeOwnerState
import com.wquasar.codeowners.visibility.file.FilesHelper
import com.wquasar.codeowners.visibility.ui.BalloonPopupHelper
import java.awt.event.MouseEvent
import javax.inject.Inject
import javax.swing.JComponent

private const val EMPTY_OWNER = "¯\\__(ツ)__/¯"

internal class CodeOwnersCommitActionPresenter @Inject constructor(
    private val filesHelper: FilesHelper,
    private val balloonPopupHelper: BalloonPopupHelper,
) {

    lateinit var project: Project
    lateinit var codeOwnerService: CodeOwnerService
    private var isCodeownerFileEdited = false

    fun isGitEnabled(): Boolean {
        return ProjectLevelVcsManager.getInstance(project).checkVcsIsActive("Git")
    }

    private fun getGitFileChanges(): MutableCollection<Change> =
        ChangeListManager.getInstance(project).defaultChangeList.changes

    fun handleAction(actionEvent: AnActionEvent) {
        when (val codeOwnerState = getCodeOwnerActionState()) {
            is NoFilesInDefaultChangelist -> createAndShowEmptyChangelistPopup(actionEvent)
            is NoCodeOwnerFileFound -> createAndShowEmptyCodeownersPopup(actionEvent)
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

        return getActionStateForFileChanges(fileChanges)
    }

    private fun getActionStateForFileChanges(fileChanges: MutableCollection<Change>): CodeOwnersCommitActionState {
        val codeOwnerMap: HashMap<List<String>, MutableList<VirtualFile>> = hashMapOf()
        isCodeownerFileEdited = false

        fileChanges.mapNotNull { it.virtualFile }
            .forEach { file ->
                if (isCodeownerFileEdited.not() && filesHelper.isCodeOwnersFile(file)) {
                    isCodeownerFileEdited = true
                }
                val codeOwnerState = codeOwnerService.getFileCodeOwnerState(project, file)

                if (codeOwnerState is FileCodeOwnerState.RuleFoundInCodeOwnerFile) {
                    val owners = codeOwnerState.codeOwnerRule.owners
                    codeOwnerMap.getOrPut(owners) { mutableListOf() }.apply {
                        add(file)
                    }
                } else if (codeOwnerState is FileCodeOwnerState.NoRuleFoundInCodeOwnerFile) {
                    codeOwnerMap.getOrPut(listOf(EMPTY_OWNER)) { mutableListOf() }.apply {
                        add(file)
                    }
                }
            }

        return if (codeOwnerMap.isEmpty()) {
            NoCodeOwnerFileFound
        } else {
            FilesWithCodeOwnersEdited(codeOwnerMap)
        }
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
        codeOwnerMap: HashMap<List<String>, MutableList<VirtualFile>>,
        actionEvent: AnActionEvent,
    ) {
        val mouseEvent = actionEvent.inputEvent as? MouseEvent
        val point = mouseEvent?.point ?: return
        val component = actionEvent.inputEvent.component as? JComponent ?: return

        val actionGroup = DefaultActionGroup().apply {
            codeOwnerMap.keys.forEach { owner ->
                val modifiedOwnedFiles = codeOwnerMap[owner] ?: return@forEach

                val fileActionGroup =
                    DefaultActionGroup("${owner.joinToString(", ")} \t[${modifiedOwnedFiles.size}]", true).apply {
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
