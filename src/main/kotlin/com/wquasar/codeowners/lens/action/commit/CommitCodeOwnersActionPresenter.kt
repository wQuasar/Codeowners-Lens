package com.wquasar.codeowners.lens.action.commit

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.VirtualFile
import com.wquasar.codeowners.lens.action.commit.CommitCodeOwnersActionState.NoChangesInAnyChangelist
import com.wquasar.codeowners.lens.action.commit.CommitCodeOwnersActionState.NoCodeOwnerFileFound
import com.wquasar.codeowners.lens.action.commit.CommitCodeOwnersActionState.FilesWithCodeOwnersEdited
import com.wquasar.codeowners.lens.core.CodeOwnerService
import com.wquasar.codeowners.lens.core.FileCodeOwnerState
import com.wquasar.codeowners.lens.file.ChangeListManagerProvider
import com.wquasar.codeowners.lens.file.FilesHelper
import com.wquasar.codeowners.lens.ui.addPopupActionItem
import com.wquasar.codeowners.lens.ui.addPopupSection
import java.awt.event.MouseEvent
import javax.inject.Inject
import javax.swing.JComponent

private const val NO_CODEOWNER = "¯\\__(ツ)__/¯"

internal class CommitCodeOwnersActionPresenter @Inject constructor(
    private val filesHelper: FilesHelper,
    private val changeListManagerProvider: ChangeListManagerProvider,
) {

    lateinit var view: CommitCodeOwnersActionView
    lateinit var project: Project
    lateinit var codeOwnerService: CodeOwnerService

    fun isGitEnabled(): Boolean {
        return ProjectLevelVcsManager.getInstance(project).checkVcsIsActive("Git")
    }

    fun handleActionEvent(actionEvent: AnActionEvent) {
        when (val codeOwnerState = getCommitActionState()) {
            is NoChangesInAnyChangelist -> view.showEmptyChangelistPopup(actionEvent)
            is NoCodeOwnerFileFound -> {
                view.showNoCodeOwnersFilePopup(actionEvent)
                createAndShowOwnerPopup(codeOwnerState.changeListWithOwnersList, actionEvent)
            }

            is FilesWithCodeOwnersEdited -> {
                if (codeOwnerState.isCodeOwnerFileEdited) {
                    view.showCodeOwnersEditedPopup(actionEvent)
                }
                createAndShowOwnerPopup(codeOwnerState.changeListWithOwnersList, actionEvent)
            }
        }
    }

    private fun createAndShowOwnerPopup(
        changeListWithOwners: List<ChangeListWithOwners>,
        actionEvent: AnActionEvent
    ) {
        val actionGroup = createAndShowCodeownersPopup(changeListWithOwners, actionEvent)
        actionGroup?.let { view.showActionPopup(it) }
    }

    private fun isCodeownerFileEdited(changeListWithOwnersList: MutableList<ChangeListWithOwners>): Boolean {
        return changeListWithOwnersList.any {
            it.codeOwnersMap.values.any { files ->
                files.any { virtualFile -> filesHelper.isCodeOwnersFile(virtualFile) }
            }
        }
    }

    private fun getCommitActionState(): CommitCodeOwnersActionState {
        val changeListManager = changeListManagerProvider.getChangeListManager(project)
        if (changeListManager.allChanges.isEmpty()) {
            return NoChangesInAnyChangelist
        }

        val currentChangeListWithOwnersList = mutableListOf<ChangeListWithOwners>()
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
                currentChangeListWithOwnersList.add(
                    if (changeList.isDefault) 0 else currentChangeListWithOwnersList.size, changeListWithOwner
                )
            }
        }

        return if (isNoCodeOwnerFileFound(currentChangeListWithOwnersList)) {
            NoCodeOwnerFileFound(
                changeListWithOwnersList = currentChangeListWithOwnersList,
            )
        } else {
            FilesWithCodeOwnersEdited(
                isCodeOwnerFileEdited = isCodeownerFileEdited(currentChangeListWithOwnersList),
                changeListWithOwnersList = currentChangeListWithOwnersList,
            )
        }
    }

    private fun isNoCodeOwnerFileFound(changeLists: MutableList<ChangeListWithOwners>): Boolean {
        return changeLists.singleOrNull()?.codeOwnersMap?.keys?.first() == listOf(NO_CODEOWNER)
    }

    private fun getCodeOwnerMapForChangelist(fileChanges: MutableCollection<Change>):
            HashMap<List<String>, MutableList<VirtualFile>> {
        val codeOwnerMap: HashMap<List<String>, MutableList<VirtualFile>> = hashMapOf()

        fileChanges.mapNotNull { it.virtualFile }
            .forEach { file ->
                val codeOwnerState = codeOwnerService.getFileCodeOwnerState(project, file)

                if (codeOwnerState is FileCodeOwnerState.RuleFoundInCodeOwnerFile) {
                    val owners = codeOwnerState.codeOwnerRule.owners
                    codeOwnerMap.getOrPut(owners) { mutableListOf() }.apply {
                        add(file)
                    }
                } else if (codeOwnerState is FileCodeOwnerState.NoRuleFoundInCodeOwnerFile ||
                    codeOwnerState is FileCodeOwnerState.NoCodeOwnerFileFound
                ) {
                    codeOwnerMap
                        .getOrPut(listOf(NO_CODEOWNER)) { mutableListOf() }
                        .apply { add(file) }
                }
            }

        return codeOwnerMap
    }

    private fun createAndShowCodeownersPopup(
        changeListWithOwners: List<ChangeListWithOwners>,
        actionEvent: AnActionEvent,
    ): ActionPopupInfo? {
        if (changeListWithOwners.isEmpty()) return null

        (actionEvent.inputEvent as? MouseEvent)?.let { mouseEvent ->
            val point = mouseEvent.point
            (actionEvent.inputEvent.component as? JComponent)?.let { component ->
                val actionGroup = DefaultActionGroup().apply {
                    addDefaultChangeList(changeListWithOwners.firstOrNull { it.isDefault })
                    addOtherChangelists(changeListWithOwners.filter { it.isDefault.not() })
                }
                return ActionPopupInfo(
                    actionGroup = actionGroup,
                    component = component,
                    point = Pair(point.x, point.y),
                )
            }
        }

        return null
    }

    private fun DefaultActionGroup.addDefaultChangeList(
        changeList: ChangeListWithOwners?,
    ) {
        changeList ?: return
        addPopupSection("Codeowners for '${changeList.listLabel}'")
        add(getIndividualCodeOwnersActionGroup(changeList.codeOwnersMap))
    }

    private fun DefaultActionGroup.addOtherChangelists(
        changeLists: List<ChangeListWithOwners>,
    ) {
        if (changeLists.isEmpty()) return

        add(Separator())
        addPopupSection("Other Changelists")

        changeLists.forEach {
            add(DefaultActionGroup("▹ '${it.listLabel}'", true).apply {
                add(getIndividualCodeOwnersActionGroup(it.codeOwnersMap))
            })
        }
    }

    private fun getIndividualCodeOwnersActionGroup(
        codeOwnerMap: HashMap<List<String>, MutableList<VirtualFile>>,
    ): DefaultActionGroup {
        return DefaultActionGroup().apply {
            codeOwnerMap.entries.forEach { (owner, modifiedOwnedFiles) ->
                if (modifiedOwnedFiles.isEmpty()) return@forEach

                add(DefaultActionGroup().apply {
                    add(DefaultActionGroup("${owner.joinToString(", ")} \t[${modifiedOwnedFiles.size}]", true).apply {
                        modifiedOwnedFiles.forEach { file ->
                            addPopupActionItem(filesHelper.getTruncatedFileName(file)) {
                                filesHelper.openFile(project, file)
                            }
                        }
                    })
                })
            }
        }
    }
}
