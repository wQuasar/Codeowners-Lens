package com.wquasar.codeowners.visibility.widget.statusbar

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
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

internal class CodeOwnersWidget(
    currentProject: Project,
    private val presenter: CodeOwnersWidgetPresenter
) : EditorBasedWidget(currentProject), StatusBarWidget.MultipleTextValuesPresentation,
    RefactoringEventListener, FileEditorManagerListener, CodeOwnersWidgetView {

    private val connection: MessageBusConnection = currentProject.messageBus.connect(this)

    init {
        presenter.view = this
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                presenter.updateCodeOwnerServiceIfNeeded(events)
            }
        })
    }

    override fun ID() = presenter.getID()

    override fun getSelectedValue(): String = presenter.getSelectedValue()

    override fun install(statusBar: StatusBar) {
        if (presenter.shouldInstall(statusBar)) {
            super.install(statusBar)
        }
    }

    override fun getPopup(): JBPopup? = presenter.getPopup()

    override fun getTooltipText(): String {
        return presenter.getTooltipText()
    }

    override fun getPresentation() = this

    private fun update(file: VirtualFile?) {
        presenter.updateState(file)
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

    override fun updateWidget() {
        myStatusBar?.updateWidget(ID())
    }

    override fun getSelectedFile(): VirtualFile? = getSelectedFile()

    override fun dispose() {
        super.dispose()
        connection.disconnect()
    }
}
