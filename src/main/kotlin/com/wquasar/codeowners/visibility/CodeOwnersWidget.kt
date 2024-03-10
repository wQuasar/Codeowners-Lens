package com.wquasar.codeowners.visibility

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.util.messages.MessageBusConnection
import com.wquasar.codeowners.visibility.utils.Utilities

class CodeOwnersWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.MultipleTextValuesPresentation,
    RefactoringEventListener, FileEditorManagerListener {

    companion object {
        const val ID = "com.wquasar.codeowners.visibility.CodeOwnersWidget"
    }

    private val connection: MessageBusConnection = project.messageBus.connect(this)

    init {
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                for (event in events) {
                    if (Utilities.isCodeOwnersFile(event.file)) {
                        codeOwners.refreshCodeOwnerRules(event.file)
                        break
                    }
                }
            }
        })
    }

    private var currentOrSelectedFile: VirtualFile? = null
    private var currentFilePath: String? = null
    private var currentFileRule: CodeOwnerRule? = null

    private val codeOwners = CodeOwners(project)

    override fun ID() = ID

    override fun getSelectedValue(): String {
        if (currentOrSelectedFile == null) return ""
        val owners = getCurrentCodeOwnerRule()?.owners ?: return "¯\\_(ツ)_/¯"

        return when {
            owners.size == 0 -> "¯\\_(ツ)_/¯"
            owners.size == 1 -> owners.first()
            else -> "${owners.first()} & ${owners.size - 1} more"
        }
    }

    override fun getTooltipText(): String {
        return "Hello, tip from the world!"
    }

    override fun getPresentation() = this

    private fun getCurrentCodeOwnerRule(): CodeOwnerRule? {
        val file = currentOrSelectedFile ?: return null
        if (file.path != currentFilePath) {
            currentFilePath = file.path
            currentFileRule = codeOwners.getCodeOwners(file)
        }
        return currentFileRule
    }

    private fun update(file: VirtualFile?) {
        currentOrSelectedFile = file ?: getSelectedFile() ?: currentOrSelectedFile
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
