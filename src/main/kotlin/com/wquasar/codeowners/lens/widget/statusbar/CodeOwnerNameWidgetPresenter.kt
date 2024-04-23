package com.wquasar.codeowners.lens.widget.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.StatusBar
import com.wquasar.codeowners.lens.core.CodeOwnerRule
import com.wquasar.codeowners.lens.core.CodeOwnerService
import com.wquasar.codeowners.lens.core.FileCodeOwnerState
import com.wquasar.codeowners.lens.core.FileCodeOwnerState.RuleFoundInCodeOwnerFile
import com.wquasar.codeowners.lens.core.FileCodeOwnerState.NoCodeOwnerFileFound
import com.wquasar.codeowners.lens.core.FileCodeOwnerState.NoRuleFoundInCodeOwnerFile
import com.wquasar.codeowners.lens.file.FilesHelper
import java.util.ResourceBundle
import java.util.Locale
import javax.swing.SwingConstants

internal class CodeOwnerNameWidgetPresenter(
    private val project: Project,
    private val codeOwnerService: CodeOwnerService,
    private val filesHelper: FilesHelper,
) {

    companion object {
        const val ID = "com.wquasar.codeowners.lens.CodeOwnerNameWidget"
    }

    lateinit var view: CodeOwnerNameWidgetView

    private var currentOrSelectedFile: VirtualFile? = null
    private var currentFileRuleOwnerState: FileCodeOwnerState? = null

    private val messages = ResourceBundle.getBundle("messages", Locale.getDefault())

    fun getID(): String = ID

    fun shouldInstall(statusBar: StatusBar): Boolean {
        return statusBar.project == project
    }

    fun getSelectedValue(): String {
        if (currentOrSelectedFile == null) return ""

        currentFileRuleOwnerState = getCurrentFileCodeOwnerState()
        return when (currentFileRuleOwnerState) {
            NoRuleFoundInCodeOwnerFile -> messages.getString("statusbar.unknown_codeowner_label")
            is RuleFoundInCodeOwnerFile -> {
                val owners = currentFileRuleOwnerState?.let {
                    (it as RuleFoundInCodeOwnerFile).codeOwnerRule.owners
                } ?: return ""
                when {
                    owners.isEmpty() -> messages.getString("statusbar.unknown_codeowner_label")
                    owners.size == 1 -> owners.first()
                    owners.size == 2 -> String.format(
                        messages.getString("statusbar.two_codeowners_display_label"),
                        owners.first(),
                        owners.last(),
                    )

                    else -> String.format(
                        messages.getString("statusbar.more_than_two_codeowners_display_label"),
                        owners.first(),
                        owners[1],
                        owners.size - 2,
                    )
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
        val currentFile = currentOrSelectedFile ?: return null
        if (currentFile.isInLocalFileSystem.not()) {
            return null
        }
        return codeOwnerService.getFileCodeOwnerState(currentFile, project.basePath)
    }

    fun getTooltipText(): String {
        return when (val fileCodeOwnerState = currentFileRuleOwnerState) {
            is NoCodeOwnerFileFound -> ""
            is NoRuleFoundInCodeOwnerFile -> messages.getString("statusbar.no_codeowner_label_msg")
            is RuleFoundInCodeOwnerFile -> {
                val owners = fileCodeOwnerState.codeOwnerRule.owners
                when {
                    owners.isEmpty() -> messages.getString("statusbar.no_codeowner_label_msg")
                    owners.size == 1 -> messages.getString("statusbar.click_to_show_codeowner_rule_msg")
                    else -> messages.getString("statusbar.click_to_show_all_codeowners_msg")
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
        popup.setAdText(messages.getString("statusbar.all_codeowners_popup_title"), SwingConstants.CENTER)
        return popup
    }

    private fun goToOwner(codeOwnerRule: CodeOwnerRule, codeOwnerLabel: String) {
        val codeOwnerString = codeOwnerService.getTrueCodeOwner(codeOwnerLabel)
        val codeOwnerFile = codeOwnerService.getCodeOwnerFile() ?: return

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
                view.updateWidget()
                break
            }
        }
    }
}
