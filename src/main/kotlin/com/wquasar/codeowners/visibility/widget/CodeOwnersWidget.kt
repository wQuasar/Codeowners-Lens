package com.wquasar.codeowners.visibility.widget

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.util.messages.MessageBusConnection
import com.wquasar.codeowners.visibility.core.CodeOwnerRule
import com.wquasar.codeowners.visibility.core.CodeOwnerService
import com.wquasar.codeowners.visibility.file.FilesHelper
import javax.swing.SwingConstants

internal class CodeOwnersWidget(
    currentProject: Project,
    private val codeOwnerService: CodeOwnerService,
    private val filesHelper: FilesHelper,
) : EditorBasedWidget(currentProject), StatusBarWidget.MultipleTextValuesPresentation,
    RefactoringEventListener, FileEditorManagerListener {

    companion object {
        const val ID = "com.wquasar.codeowners.visibility.widget.CodeOwnersWidget"
        const val EMPTY_OWNER = "¯\\_(ツ)_/¯"
    }

    private val connection: MessageBusConnection = currentProject.messageBus.connect(this)

    init {
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                for (event in events) {
                    if (filesHelper.isCodeOwnersFile(event.file)) {
                        codeOwnerService.refreshCodeOwnerRules(ModuleManager.getInstance(project), event.file)
                        break
                    }
                }
            }
        })
    }

    private var currentOrSelectedFile: VirtualFile? = null
    private var currentFileCodeOwnerRule: CodeOwnerRule? = null

    override fun ID() = ID

    override fun getSelectedValue(): String {
        if (currentOrSelectedFile == null) return ""

        currentFileCodeOwnerRule = getCurrentCodeOwnerRule()
        val owners = currentFileCodeOwnerRule?.owners ?: return EMPTY_OWNER
        return when {
            owners.isEmpty() -> EMPTY_OWNER
            owners.size == 1 -> owners.first()
            else -> "${owners.first()} & ${owners.size - 1} more"
        }
    }

    override fun install(statusBar: StatusBar) {
        if (statusBar.project == project) {
            val baseDirPath = filesHelper.getBaseDir(ModuleManager.getInstance(project), currentOrSelectedFile)
            val codeOwnersFile = baseDirPath?.let { filesHelper.findCodeOwnersFile(it) }

            if (codeOwnersFile != null) {
                super.install(statusBar)
            }
        }
    }

    override fun getPopup(): JBPopup? {
        val codeOwnerRule = currentFileCodeOwnerRule ?: return null
        val owners = codeOwnerRule.owners
        if (owners.size == 1) {
            goToOwner(codeOwnerRule.lineNumber, owners.first())
            return null
        }

        val popup = JBPopupFactory.getInstance().createListPopup(
            object : BaseListPopupStep<String>("", owners) {
                override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                    goToOwner(codeOwnerRule.lineNumber, selectedValue)
                    return super.onChosen(selectedValue, finalChoice)
                }
            }
        )
        popup.setAdText("All codeowners", SwingConstants.CENTER)
        return popup
    }

    private fun goToOwner(lineNumber: Int, codeOwnerLabel: String) {
        val baseDirPath = filesHelper.getBaseDir(ModuleManager.getInstance(project), currentOrSelectedFile) ?: return
        val codeOwnerFile = filesHelper.findCodeOwnersFile(baseDirPath) ?: return

        val vf = codeOwnerFile.toPath().let { VirtualFileManager.getInstance().findFileByNioPath(it) } ?: return
        val columnIndex = filesHelper.getColumnIndexForCodeOwner(codeOwnerFile, lineNumber, codeOwnerLabel)
        filesHelper.openFile(project, vf, lineNumber, columnIndex)
    }

    override fun getTooltipText(): String {
        return when (currentFileCodeOwnerRule?.owners?.size) {
            0 -> "No codeowners found"
            else -> "Click to show in CODEOWNERS"
        }
    }

    override fun getPresentation() = this

    override fun getIcon() = IconLoader.getIcon("/icons/codeowner_icon.svg", CodeOwnersWidget::class.java)

    private fun getCurrentCodeOwnerRule(): CodeOwnerRule? {
        val file = currentOrSelectedFile ?: return null
        return codeOwnerService.getCodeOwners(ModuleManager.getInstance(project), file)
    }

    private fun update(file: VirtualFile?) {
        currentOrSelectedFile = file ?: getSelectedFile()
        myStatusBar?.updateWidget(ID())
    }

    override fun selectionChanged(event: FileEditorManagerEvent) = update(event.newFile)

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        update(file)
    }

    override fun refactoringStarted(p0: String, p1: RefactoringEventData?) {
        // no-ops
    }

    override fun refactoringDone(p0: String, p1: RefactoringEventData?) {
        update(null)
    }

    override fun conflictsDetected(p0: String, p1: RefactoringEventData) {
        // no-ops
    }

    override fun undoRefactoring(p0: String) {
        update(null)
    }

    override fun dispose() {
        super.dispose()
        connection.disconnect()
    }
}
