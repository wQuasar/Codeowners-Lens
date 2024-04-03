package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.actionSystem.*
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

    fun handleAction(actionEvent: AnActionEvent) {
        when (val codeOwnerState = getCodeOwnerActionState()) {
            is NoChangesInAnyChangelist -> createAndShowEmptyChangelistPopup(actionEvent)
            is NoCodeOwnerFileFound -> createAndShowEmptyCodeownersPopup(actionEvent)
            is FilesWithCodeOwnersEdited -> {
                createAndShowCodeownersPopup(codeOwnerState, actionEvent)
                createAndShowCodeownersEditedPopupIfNeeded(actionEvent)
            }
        }
    }

    private fun getCodeOwnerActionState(): CodeOwnersCommitActionState {
        val changeListManager = ChangeListManager.getInstance(project)
        if (changeListManager.allChanges.isEmpty()) {
            return NoChangesInAnyChangelist
        }

        val currentState = FilesWithCodeOwnersEdited(changeListWithOwnersList = mutableListOf())

        changeListManager.changeLists.forEach { changeList ->
            val fileChanges = changeList.changes
            if (fileChanges.isEmpty()) {
                return@forEach
            }

            val codeOwnerMap = getCodeOwnerMapForChangelist(fileChanges)
            if (codeOwnerMap.isNotEmpty()) {
                val changeListWithOwner = ChangeListWithOwners(
                    listLabel = changeList.name,
                    codeOwnersMap = codeOwnerMap,
                    isDefault = changeList.isDefault,
                )
                with(currentState.changeListWithOwnersList) {
                    if (changeList.isDefault) {
                        add(0, changeListWithOwner)
                    } else {
                        add(this.size, changeListWithOwner)
                    }
                }
            }
        }

        return if (currentState.changeListWithOwnersList.isEmpty()) {
            NoCodeOwnerFileFound
        } else {
            currentState
        }
    }

    private fun getCodeOwnerMapForChangelist(fileChanges: MutableCollection<Change>): HashMap<List<String>, MutableList<VirtualFile>> {
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
            message = "No files modified.",
        )
    }

    private fun createAndShowEmptyCodeownersPopup(actionEvent: AnActionEvent) {
        balloonPopupHelper.createAndShowBalloonPopupAboveComponent(
            actionEvent = actionEvent,
            message = "No CODEOWNERS file found for modified files.",
        )
    }

    private fun createAndShowCodeownersPopup(
        codeOwnerState: FilesWithCodeOwnersEdited,
        actionEvent: AnActionEvent,
    ) {
        val mouseEvent = actionEvent.inputEvent as? MouseEvent
        val point = mouseEvent?.point ?: return
        val component = actionEvent.inputEvent.component as? JComponent ?: return

        val changeLists = codeOwnerState.changeListWithOwnersList
        if (changeLists.isEmpty()) {
            return
        }

        val actionGroup = DefaultActionGroup().apply {
            if (changeLists.first().isDefault) {
                addSection("Codeowners in '${codeOwnerState.changeListWithOwnersList[0].listLabel}'")
                add(getCodeOwnerActionGroup(codeOwnerState.changeListWithOwnersList[0].codeOwnersMap))

                addOtherChangelists(changeLists, 1)
            } else {
                addOtherChangelists(changeLists, 0)
            }
        }

        val popup = ActionManager.getInstance().createActionPopupMenu("CodeOwners.Commit", actionGroup)
        popup.component.show(component, point.x, point.y)
    }

    private fun DefaultActionGroup.addOtherChangelists(
        changeLists: MutableList<ChangeListWithOwners>,
        firstIndex: Int
    ) {
        if (changeLists.size > 1) {
            addSection("Other Changelists", preSeparator = true)

            for (i in firstIndex until changeLists.size) {
                DefaultActionGroup("▹ ${changeLists[i].listLabel}", true).apply {
                    add(getCodeOwnerActionGroup(changeLists[i].codeOwnersMap))
                }.let { add(it) }
            }
        }
    }

    private fun getCodeOwnerActionGroup(codeOwnerMap: HashMap<List<String>, MutableList<VirtualFile>>): DefaultActionGroup {
        return DefaultActionGroup().apply {
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
    }

    private fun DefaultActionGroup.addSection(
        title: String,
        preSeparator: Boolean = false,
        postSeparator: Boolean = false
    ) {
        if (preSeparator) {
            add(Separator())
        }
        add(object : AnAction(title) {
            override fun actionPerformed(e: AnActionEvent) {
                // No action performed
            }

            override fun update(actionEvent: AnActionEvent) {
                actionEvent.presentation.isEnabled = false // makes it non-clickable
            }

            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.EDT
            }
        })
        if (postSeparator) {
            add(Separator())
        }
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
