package com.wquasar.codeowners.visibility.action.commit

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.visibility.action.commit.CommitActionState.*
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.core.FileCodeOwnerState
import com.wquasar.codeowners.visibility.file.FilesHelper
import com.wquasar.codeowners.visibility.ui.addPopupActionItem
import com.wquasar.codeowners.visibility.ui.addPopupSection
import java.awt.event.MouseEvent
import javax.inject.Inject
import javax.swing.JComponent

private const val NO_CODEOWNER = "¯\\__(ツ)__/¯"

internal class CommitActionPresenter @Inject constructor(
    private val filesHelper: FilesHelper,
) {

    lateinit var view: CommitActionView
    lateinit var project: Project
    lateinit var codeOwnerService: CodeOwnerService
    private var isCodeownerFileEdited = false

    fun isGitEnabled(): Boolean {
        return ProjectLevelVcsManager.getInstance(project).checkVcsIsActive("Git")
    }

    fun handleActionEvent(actionEvent: AnActionEvent) {
        when (val codeOwnerState = getCodeOwnerActionState()) {
            is NoChangesInAnyChangelist -> view.showEmptyChangelistPopup(actionEvent)
            is NoCodeOwnerFileFound -> view.showNoCodeOwnersFilePopup(actionEvent)
            is FilesWithCodeOwnersEdited -> {
                if (isCodeownerFileEdited) {
                    view.showCodeOwnersEditedPopup(actionEvent)
                }
                createAndShowCodeownersPopup(codeOwnerState, actionEvent)
            }
        }
    }

    private fun getCodeOwnerActionState(): CommitActionState {
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
                    codeOwnerMap.getOrPut(listOf(NO_CODEOWNER)) { mutableListOf() }.apply {
                        add(file)
                    }
                }
            }

        return codeOwnerMap
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
            addDefaultChangeList(changeLists.firstOrNull { it.isDefault })
            addOtherChangelists(changeLists.filter { it.isDefault.not() })
        }

        val popup = ActionManager.getInstance().createActionPopupMenu("CodeOwners.Commit", actionGroup)
        popup.component.show(component, point.x, point.y)
    }

    private fun DefaultActionGroup.addDefaultChangeList(
        changeList: ChangeListWithOwners?,
    ) {
        changeList ?: return
        addPopupSection("Codeowners for '${changeList.listLabel}'")
        add(getCodeOwnerActionGroup(changeList.codeOwnersMap))
    }

    private fun DefaultActionGroup.addOtherChangelists(
        changeLists: List<ChangeListWithOwners>,
    ) {
        if (changeLists.isEmpty()) return

        add(Separator())
        addPopupSection("Other Changelists")

        changeLists.forEach {
            add(DefaultActionGroup("▹ '${it.listLabel}'", true).apply {
                add(getCodeOwnerActionGroup(it.codeOwnersMap))
            })
        }
    }

    private fun getCodeOwnerActionGroup(codeOwnerMap: HashMap<List<String>, MutableList<VirtualFile>>): DefaultActionGroup {
        return DefaultActionGroup().apply {
            codeOwnerMap.entries.forEach { (owner, modifiedOwnedFiles) ->
                if (modifiedOwnedFiles.isEmpty()) return@forEach

                DefaultActionGroup().apply {
                    DefaultActionGroup("${owner.joinToString(", ")} \t[${modifiedOwnedFiles.size}]", true).apply {
                        modifiedOwnedFiles.forEach { file ->
                            addPopupActionItem(filesHelper.getTruncatedFileName(file)) {
                                filesHelper.openFile(project, file)
                            }
                        }
                    }.let { add(it) }
                }.let { add(it) }
            }
        }
    }
}