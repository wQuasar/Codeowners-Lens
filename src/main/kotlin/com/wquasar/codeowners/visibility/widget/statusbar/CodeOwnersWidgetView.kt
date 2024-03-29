package com.wquasar.codeowners.visibility.widget.statusbar

import com.intellij.openapi.vfs.VirtualFile

interface CodeOwnersWidgetView {
    fun updateWidget()
    fun getSelectedFile(): VirtualFile?
}
