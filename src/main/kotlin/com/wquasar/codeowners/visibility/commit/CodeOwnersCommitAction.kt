package com.wquasar.codeowners.visibility.commit

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.di.CodeOwnersComponentProvider
import com.wquasar.codeowners.visibility.file.FilesHelper
import java.awt.event.MouseEvent
import javax.inject.Inject
import javax.swing.SwingConstants

internal class CodeOwnersCommitAction : AnAction() {

    init {
        CodeOwnersComponentProvider.component.inject(this)
    }

    @Inject
    lateinit var codeOwnerService: CodeOwnerService

    @Inject
    lateinit var filesHelper: FilesHelper

    private lateinit var project: Project

    override fun actionPerformed(actionEvent: AnActionEvent) {
        project = actionEvent.project ?: return

        val codeOwnersMap: HashMap<String, MutableList<VirtualFile>> = populateCodeOwnersMap()
        createAndShowPopup(codeOwnersMap, actionEvent)
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

        val popupStep: ListPopupStep<String> = object : BaseListPopupStep<String>(
            "",
            codeOwnerMap.keys.toList()
        ) {
            override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<out Any>? {
                val modifiedOwnedFiles = codeOwnerMap[selectedValue] ?: return null

                // if one file, open it else, show files list
                when (modifiedOwnedFiles.size) {
                    1 -> filesHelper.openFile(project, modifiedOwnedFiles.first())
                    else -> return createSubListPopupStep(modifiedOwnedFiles)
                }

                return super.onChosen(selectedValue, finalChoice)
            }
        }

        val popup = JBPopupFactory.getInstance().createListPopup(popupStep)
        popup.setAdText("Affected codeowners", SwingConstants.LEADING)
        popup.show(RelativePoint(component, point))
    }

    private fun createSubListPopupStep(filePaths: MutableList<VirtualFile>): PopupStep<*> {
        val truncatedFilePaths = filePaths.map {
            filesHelper.getTruncatedFileName(it)
        }

        return object : BaseListPopupStep<String>("", truncatedFilePaths) {
            override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                filesHelper.openFile(project, filePaths[truncatedFilePaths.indexOf(selectedValue)])
                return super.onChosen(selectedValue, finalChoice)
            }
        }
    }
}
