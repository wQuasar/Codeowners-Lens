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
import com.wquasar.codeowners.visibility.core.FileCodeOwnerState
import com.wquasar.codeowners.visibility.core.FileCodeOwnerState.*
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
    private var currentFileRuleOwnerState: FileCodeOwnerState? = null

    fun getID(): String = ID

    fun shouldInstall(statusBar: StatusBar): Boolean {
        return statusBar.project == project
    }

    fun getSelectedValue(): String {
        if (currentOrSelectedFile == null) return ""

        currentFileRuleOwnerState = getCurrentFileCodeOwnerState()
        return when (currentFileRuleOwnerState) {
            NoRuleFoundInCodeOwnerFile -> EMPTY_OWNER
            is RuleFoundInCodeOwnerFile -> {
                val owners = currentFileRuleOwnerState?.let {
                    (it as RuleFoundInCodeOwnerFile).codeOwnerRule.owners
                } ?: return ""
                when {
                    owners.isEmpty() -> EMPTY_OWNER
                    owners.size == 1 -> owners.first()
                    owners.size == 2 -> "${owners.first()} & ${owners.last()}"
                    else -> "${owners.first()}, ${owners[1]} & ${owners.size - 2} more"
                }
            }

            NoCodeOwnerFileFound -> ""
            else -> ""
        }
    }

    private fun getCurrentFileCodeOwnerState(): FileCodeOwnerState? {
        if (currentOrSelectedFile == null) {
            updateState(getSelectedFile())
            return null
        }
        val file = currentOrSelectedFile ?: return null
        return codeOwnerService.getFileCodeOwnerState(project, file)
    }

    fun getTooltipText(): String {
        return when (val fileCodeOwnerState = currentFileRuleOwnerState) {
            is NoCodeOwnerFileFound -> ""
            is NoRuleFoundInCodeOwnerFile -> "No codeowners found"
            is RuleFoundInCodeOwnerFile -> {
                val owners = fileCodeOwnerState.codeOwnerRule.owners
                when {
                    owners.isEmpty() -> "No codeowners found"
                    owners.size == 1 -> "Click to show in CODEOWNERS"
                    else -> "Click to show all codeowners"
                }
            }

            else -> ""
        }
    }

    fun updateState(file: VirtualFile?) {
        currentOrSelectedFile = file
        currentFileRuleOwnerState = null
        view.updateWidget()
    }

    private fun getSelectedFile(): VirtualFile? = view.getSelectedFile()

    fun getPopup(): JBPopup? {
        val fileCodeOwnerState = currentFileRuleOwnerState
        if (fileCodeOwnerState !is RuleFoundInCodeOwnerFile) {
            return null
        }

        val codeOwnerRule = fileCodeOwnerState.codeOwnerRule
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
