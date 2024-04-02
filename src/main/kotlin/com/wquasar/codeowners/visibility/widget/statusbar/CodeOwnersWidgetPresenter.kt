package com.wquasar.codeowners.visibility.widget.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.StatusBar
import com.wquasar.codeowners.visibility.core.CodeOwnerRule
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.core.CodeOwnerService.Companion.EMPTY_OWNER
import com.wquasar.codeowners.visibility.file.FilesHelper
import javax.swing.SwingConstants

internal class CodeOwnersWidgetPresenter(
    private val project: Project,
    private val codeOwnerService: CodeOwnerService,
    private val filesHelper: FilesHelper,
) {

    companion object {
        const val ID = "com.wquasar.codeowners.visibility.widget.statusbar.CodeOwnersWidget"
    }

    lateinit var view: CodeOwnersWidgetView

    private var currentOrSelectedFile: VirtualFile? = null
    private var currentFileCodeOwnerRule: CodeOwnerRule? = null

    fun getID(): String = ID

    fun shouldInstall(statusBar: StatusBar): Boolean {
        return statusBar.project == project
    }

    fun getSelectedValue(): String {
        if (currentOrSelectedFile == null) return "<no file>"

        currentFileCodeOwnerRule = getCurrentCodeOwnerRule() ?: return "<no rule>"
        val owners = currentFileCodeOwnerRule?.owners ?: return EMPTY_OWNER
        return when {
            owners.isEmpty() -> EMPTY_OWNER
            owners.size == 1 -> owners.first()
            owners.size == 2 -> "${owners.first()} & ${owners.last()}"
            else -> "${owners.first()}, ${owners[1]} & ${owners.size - 2} more"
        }
    }

    private fun getCurrentCodeOwnerRule(): CodeOwnerRule? {
        if (currentOrSelectedFile == null) {
            updateState(getSelectedFile())
            return null
        }

        val file = currentOrSelectedFile ?: return null
        return codeOwnerService.getCodeOwners(project, file)
    }

    fun getTooltipText(): String {
        val noCodeOwnersFoundMessage = "No codeowners found"
        return currentFileCodeOwnerRule?.owners?.size?.let { size ->
            when {
                size == 0 -> noCodeOwnersFoundMessage
                size > 1 -> "Click to show all codeowners"
                else -> "Click to show in CODEOWNERS"
            }
        } ?: noCodeOwnersFoundMessage
    }

    fun updateState(file: VirtualFile?) {
        currentOrSelectedFile = file
        currentFileCodeOwnerRule = null
        view.updateWidget()
    }

    private fun getSelectedFile(): VirtualFile? = view.getSelectedFile()

    fun getPopup(): JBPopup? {
        val codeOwnerRule = currentFileCodeOwnerRule ?: return null
        val owners = codeOwnerRule.owners
        if (owners.size == 1) {
            goToOwner(codeOwnerRule, owners.first())
            return null
        }

        val popup = JBPopupFactory.getInstance().createListPopup(
            object : BaseListPopupStep<String>("", owners) {
                override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                    goToOwner(codeOwnerRule, selectedValue)
                    return super.onChosen(selectedValue, finalChoice)
                }
            }
        )
        popup.setAdText("All codeowners", SwingConstants.CENTER)
        return popup
    }

    private fun goToOwner(codeOwnerRule: CodeOwnerRule, codeOwnerLabel: String) {
        val codeOwnerString = codeOwnerService.getTrueCodeOwner(codeOwnerLabel)
        val codeOwnerFile = codeOwnerService.getCodeOwnerFileForRule(codeOwnerRule) ?: return

        val vf = codeOwnerFile.toPath().let { VirtualFileManager.getInstance().findFileByNioPath(it) } ?: return
        val columnIndex =
            filesHelper.getColumnIndexForCodeOwner(codeOwnerFile, codeOwnerRule.lineNumber, codeOwnerString) +
                    (codeOwnerString.length - codeOwnerLabel.length)
        filesHelper.openFile(project, vf, codeOwnerRule.lineNumber, columnIndex)
    }

    fun updateCodeOwnerServiceIfNeeded(events: MutableList<out VFileEvent>) {
        for (event in events) {
            if (filesHelper.isCodeOwnersFile(event.file)) {
                codeOwnerService.refreshCodeOwnerRules(project)
                break
            }
        }
    }
}
